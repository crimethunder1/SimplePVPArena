import logging
from dataclasses import replace

import discord
from discord.utils import escape_markdown

from bot.names import InvalidSignup, clean_signup_input
from bot.storage import Event, SignupOutcome, Storage
from bot.views import EventMessageView, SignupModal

log = logging.getLogger(__name__)


class EventBoard:
    def __init__(self, client: discord.Client, storage: Storage, team_size: int):
        self.client = client
        self.storage = storage
        self.team_size = team_size

    def build_view(self, event: Event) -> EventMessageView:
        return EventMessageView(self, event, self.storage.signups(event.id), self.team_size)

    def build_preview(self, event: Event) -> EventMessageView:
        return self.build_view(replace(event, is_active=True))

    async def refresh_message(self, event: Event) -> None:
        if event.message_id is None:
            return
        try:
            channel = self.client.get_channel(event.channel_id) or await self.client.fetch_channel(event.channel_id)
            await channel.get_partial_message(event.message_id).edit(view=self.build_view(event))
        except discord.NotFound:
            log.warning("Message for event %s no longer exists", event.id)
        except discord.HTTPException as error:
            log.warning("Could not update message for event %s: %s", event.id, error)

    async def refresh_all_active(self) -> None:
        for event in self.storage.active_events():
            await self.refresh_message(event)

    def event_for_interaction(self, interaction: discord.Interaction) -> Event | None:
        if interaction.guild_id is None or interaction.message is None:
            return None
        event = self.storage.active_event(interaction.guild_id)
        if event is None or event.message_id != interaction.message.id:
            return None
        return event

    async def open_signup_form(self, interaction: discord.Interaction) -> None:
        event = self.event_for_interaction(interaction)
        if event is None:
            await interaction.response.send_message("Diese Anmeldung ist nicht mehr aktiv.", ephemeral=True)
            return
        if not event.is_open:
            await interaction.response.send_message("Die Anmeldung ist gerade geschlossen.", ephemeral=True)
            return
        current = self.storage.signup_for_user(event.id, interaction.user.id)
        await interaction.response.send_modal(SignupModal(self, event.id, current))

    async def submit_signup(
        self, interaction: discord.Interaction, event_id: int, name: str, partner: str | None
    ) -> None:
        try:
            name, partner = clean_signup_input(name, partner)
        except InvalidSignup as error:
            await interaction.response.send_message(str(error), ephemeral=True)
            return

        event = self.storage.get_event(event_id)
        if event is None or not event.is_active:
            await interaction.response.send_message("Diese Anmeldung ist nicht mehr aktiv.", ephemeral=True)
            return
        if not event.is_open:
            await interaction.response.send_message("Die Anmeldung wurde inzwischen geschlossen.", ephemeral=True)
            return

        outcome = self.storage.save_signup(event.id, interaction.user.id, name, partner)
        if outcome is SignupOutcome.NAME_TAKEN:
            await interaction.response.send_message(
                f"**{escape_markdown(name)}** wurde bereits von jemand anderem angemeldet.", ephemeral=True
            )
            return

        await interaction.response.send_message(confirmation_text(outcome, name, partner), ephemeral=True)
        await self.refresh_message(event)

    async def withdraw_signup(self, interaction: discord.Interaction) -> None:
        event = self.event_for_interaction(interaction)
        if event is None:
            await interaction.response.send_message("Diese Anmeldung ist nicht mehr aktiv.", ephemeral=True)
            return
        if not event.is_open:
            await interaction.response.send_message("Die Anmeldung ist gerade geschlossen.", ephemeral=True)
            return
        removed = self.storage.remove_signup_for_user(event.id, interaction.user.id)
        if removed is None:
            await interaction.response.send_message("Du bist für dieses Event nicht angemeldet.", ephemeral=True)
            return
        await interaction.response.send_message(
            f"Du hast **{escape_markdown(removed.minecraft_name)}** abgemeldet.", ephemeral=True
        )
        await self.refresh_message(event)


def confirmation_text(outcome: SignupOutcome, name: str, partner: str | None) -> str:
    if outcome is SignupOutcome.UPDATED:
        text = f"Deine Anmeldung wurde aktualisiert: **{escape_markdown(name)}**"
    else:
        text = f"Du bist angemeldet als **{escape_markdown(name)}**"
    if partner is None:
        return text + ". Dir wird ein Partner zugelost."
    return (
        text
        + f" mit **{escape_markdown(partner)}**.\n"
        + "Damit ihr sicher zusammen spielt, sollte sich dein Partner entweder gar nicht selbst anmelden "
        + "oder dich bei seiner Anmeldung ebenfalls als Partner angeben."
    )
