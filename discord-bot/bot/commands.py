import io
import logging

import discord
from discord import app_commands
from discord.utils import escape_markdown

from bot.board import EventBoard
from bot.export import export_filename, render_export, summarize
from bot.pairing import Registration, build_team_lines
from bot.storage import Event, Storage
from bot.views import describe_signup, status_text

log = logging.getLogger(__name__)

NO_ACTIVE_EVENT = "Es gibt gerade kein aktives Event. Lege eins mit `/event create` an."
MESSAGE_LIMIT = 2000


def is_event_admin(user: discord.abc.User, admin_role_id: int) -> bool:
    if not isinstance(user, discord.Member):
        return False
    if user.guild_permissions.manage_guild:
        return True
    return any(role.id == admin_role_id for role in user.roles)


def fit_message(lines: list[str], limit: int = MESSAGE_LIMIT) -> str:
    text = "\n".join(lines)
    if len(text) <= limit:
        return text
    kept: list[str] = []
    for index, line in enumerate(lines):
        rest = f"... und {len(lines) - index} weitere Zeilen"
        if len("\n".join([*kept, line, rest])) > limit:
            kept.append(rest)
            break
        kept.append(line)
    return "\n".join(kept)


class EventCommands(app_commands.Group):
    def __init__(self, board: EventBoard, storage: Storage, admin_role_id: int):
        super().__init__(name="event", description="Turnier-Anmeldungen verwalten", guild_only=True)
        self.board = board
        self.storage = storage
        self.admin_role_id = admin_role_id

    async def interaction_check(self, interaction: discord.Interaction) -> bool:
        return is_event_admin(interaction.user, self.admin_role_id)

    async def on_error(self, interaction: discord.Interaction, error: app_commands.AppCommandError) -> None:
        if isinstance(error, app_commands.CheckFailure):
            text = "Diesen Befehl dürfen nur Event-Admins benutzen."
        else:
            log.exception("Command /event failed", exc_info=error)
            text = "Da ist etwas schiefgelaufen. Bitte versuch es noch einmal."
        if interaction.response.is_done():
            await interaction.followup.send(text, ephemeral=True)
        else:
            await interaction.response.send_message(text, ephemeral=True)

    def current_event(self, interaction: discord.Interaction) -> Event | None:
        return self.storage.active_event(interaction.guild_id)

    @app_commands.command(name="create", description="Neues Event anlegen und die Anmeldung hier posten")
    @app_commands.describe(
        title="Titel des Events",
        description="Beschreibung (optional)",
        date="Datum und Uhrzeit als Text, z. B. Samstag 20:00 (optional)",
    )
    async def create_event(
        self,
        interaction: discord.Interaction,
        title: app_commands.Range[str, 1, 100],
        description: app_commands.Range[str, 1, 1500] | None = None,
        date: app_commands.Range[str, 1, 100] | None = None,
    ) -> None:
        channel = interaction.channel
        if not isinstance(channel, discord.abc.Messageable):
            await interaction.response.send_message("In diesem Kanal kann ich nichts posten.", ephemeral=True)
            return
        await interaction.response.defer(ephemeral=True, thinking=True)

        previous = self.current_event(interaction)
        event = self.storage.create_event(interaction.guild_id, channel.id, title, description, date)
        try:
            message = await channel.send(view=self.board.build_preview(event))
        except discord.HTTPException as error:
            self.storage.delete_event(event.id)
            log.warning("Could not post event message: %s", error)
            await interaction.followup.send(
                "Ich konnte die Anmeldung hier nicht posten. Fehlen mir Rechte in diesem Kanal?", ephemeral=True
            )
            return

        self.storage.activate_event(event.id, message.id)
        reply = f"Event **{escape_markdown(title)}** wurde angelegt."
        if previous is not None:
            await self.board.refresh_message(self.storage.get_event(previous.id))
            reply += f" Das vorherige Event **{escape_markdown(previous.title)}** wurde archiviert."
        await interaction.followup.send(reply, ephemeral=True)

    @app_commands.command(name="close", description="Anmeldung schließen")
    async def close_signups(self, interaction: discord.Interaction) -> None:
        await self.toggle_signups(interaction, is_open=False)

    @app_commands.command(name="open", description="Anmeldung wieder öffnen")
    async def open_signups(self, interaction: discord.Interaction) -> None:
        await self.toggle_signups(interaction, is_open=True)

    async def toggle_signups(self, interaction: discord.Interaction, is_open: bool) -> None:
        event = self.current_event(interaction)
        if event is None:
            await interaction.response.send_message(NO_ACTIVE_EVENT, ephemeral=True)
            return
        if event.is_open == is_open:
            state = "schon offen" if is_open else "schon geschlossen"
            await interaction.response.send_message(f"Die Anmeldung ist {state}.", ephemeral=True)
            return
        event = self.storage.set_open(event.id, is_open)
        await interaction.response.send_message(
            "Die Anmeldung ist wieder offen." if is_open else "Die Anmeldung ist jetzt geschlossen.",
            ephemeral=True,
        )
        await self.board.refresh_message(event)

    @app_commands.command(name="export", description="Anmeldungen als Team-Datei für den Server exportieren")
    async def export_signups(self, interaction: discord.Interaction) -> None:
        event = self.current_event(interaction)
        if event is None:
            await interaction.response.send_message(NO_ACTIVE_EVENT, ephemeral=True)
            return
        signups = self.storage.signups(event.id)
        if not signups:
            await interaction.response.send_message("Es gibt noch keine Anmeldungen zum Exportieren.", ephemeral=True)
            return
        lines = build_team_lines(Registration(signup.minecraft_name, signup.partner_name) for signup in signups)
        summary = summarize(lines)
        filename = export_filename(event.title, event.id)
        file = discord.File(io.BytesIO(render_export(lines).encode("utf-8")), filename=filename)
        await interaction.response.send_message(
            f"Export für **{escape_markdown(event.title)}**\n"
            f"Duos: {summary.duos} | Solos: {summary.solos} | Spieler gesamt: {summary.players}\n"
            f"Lade `{filename}` auf den Minecraft-Server hoch und importiere sie dort.",
            file=file,
            ephemeral=True,
        )

    @app_commands.command(name="list", description="Alle Anmeldungen anzeigen")
    async def list_signups(self, interaction: discord.Interaction) -> None:
        event = self.current_event(interaction)
        if event is None:
            await interaction.response.send_message(NO_ACTIVE_EVENT, ephemeral=True)
            return
        signups = self.storage.signups(event.id)
        lines = [
            f"**{escape_markdown(event.title)}** | {status_text(event)} | {len(signups)} Anmeldungen",
        ]
        if not signups:
            lines.append("Noch niemand angemeldet.")
        for position, signup in enumerate(signups, start=1):
            lines.append(f"{position}. {describe_signup(signup)} | <@{signup.discord_user_id}>")
        await interaction.response.send_message(
            fit_message(lines), ephemeral=True, allowed_mentions=discord.AllowedMentions.none()
        )

    @app_commands.command(name="remove", description="Eine Anmeldung entfernen")
    @app_commands.describe(name="Minecraft-Name der Anmeldung")
    async def remove_signup(self, interaction: discord.Interaction, name: app_commands.Range[str, 1, 32]) -> None:
        event = self.current_event(interaction)
        if event is None:
            await interaction.response.send_message(NO_ACTIVE_EVENT, ephemeral=True)
            return
        removed = self.storage.remove_signup_by_name(event.id, name)
        if removed is None:
            await interaction.response.send_message(
                f"Keine Anmeldung mit dem Namen **{escape_markdown(name.strip())}** gefunden.", ephemeral=True
            )
            return
        await interaction.response.send_message(
            f"**{escape_markdown(removed.minecraft_name)}** wurde aus der Anmeldung entfernt.", ephemeral=True
        )
        await self.board.refresh_message(event)

    @remove_signup.autocomplete("name")
    async def remove_name_autocomplete(
        self, interaction: discord.Interaction, current: str
    ) -> list[app_commands.Choice[str]]:
        event = self.current_event(interaction)
        if event is None:
            return []
        typed = current.lower()
        names = [signup.minecraft_name for signup in self.storage.signups(event.id)]
        return [app_commands.Choice(name=name, value=name) for name in names if typed in name.lower()][:25]
