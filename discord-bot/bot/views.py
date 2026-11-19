from collections.abc import Sequence
from typing import Protocol

import discord
from discord import ui
from discord.utils import escape_markdown

from bot.storage import Event, Signup

JOIN_BUTTON_ID = "simplepvparena:signup:join"
LEAVE_BUTTON_ID = "simplepvparena:signup:leave"
NAME_LIST_LIMIT = 2000

OPEN_COLOUR = discord.Colour.green()
CLOSED_COLOUR = discord.Colour.orange()
ARCHIVED_COLOUR = discord.Colour.dark_grey()


class SignupActions(Protocol):
    async def open_signup_form(self, interaction: discord.Interaction) -> None: ...

    async def submit_signup(
        self, interaction: discord.Interaction, event_id: int, name: str, partner: str | None
    ) -> None: ...

    async def withdraw_signup(self, interaction: discord.Interaction) -> None: ...


class JoinButton(ui.Button):
    def __init__(self, actions: SignupActions, disabled: bool = False):
        super().__init__(
            label="Anmelden",
            style=discord.ButtonStyle.success,
            custom_id=JOIN_BUTTON_ID,
            disabled=disabled,
        )
        self.actions = actions

    async def callback(self, interaction: discord.Interaction) -> None:
        await self.actions.open_signup_form(interaction)


class LeaveButton(ui.Button):
    def __init__(self, actions: SignupActions, disabled: bool = False):
        super().__init__(
            label="Abmelden",
            style=discord.ButtonStyle.danger,
            custom_id=LEAVE_BUTTON_ID,
            disabled=disabled,
        )
        self.actions = actions

    async def callback(self, interaction: discord.Interaction) -> None:
        await self.actions.withdraw_signup(interaction)


def status_text(event: Event) -> str:
    if not event.is_active:
        return "Event archiviert"
    if event.is_open:
        return "Anmeldung offen"
    return "Anmeldung geschlossen"


def accent_colour(event: Event) -> discord.Colour:
    if not event.is_active:
        return ARCHIVED_COLOUR
    return OPEN_COLOUR if event.is_open else CLOSED_COLOUR


def describe_signup(signup: Signup) -> str:
    text = escape_markdown(signup.minecraft_name)
    if signup.partner_name:
        text += f" (mit {escape_markdown(signup.partner_name)})"
    return text


def format_signup_list(signups: Sequence[Signup], limit: int = NAME_LIST_LIMIT) -> str:
    if not signups:
        return "Noch niemand angemeldet."
    lines: list[str] = []
    length = 0
    for position, signup in enumerate(signups, start=1):
        line = f"{position}. {describe_signup(signup)}"
        if length + len(line) + 1 > limit:
            lines.append(f"... und {len(signups) - position + 1} weitere")
            break
        lines.append(line)
        length += len(line) + 1
    return "\n".join(lines)


def format_details(event: Event, signup_count: int, team_size: int) -> str:
    details = []
    if event.event_date:
        details.append(f"**Datum:** {event.event_date}")
    details.append(f"**Modus:** {team_size}v{team_size}")
    details.append(f"**Anmeldungen:** {signup_count}")
    details.append(f"**Status:** {status_text(event)}")
    return "\n".join(details)


class EventMessageView(ui.LayoutView):
    def __init__(self, actions: SignupActions, event: Event, signups: Sequence[Signup], team_size: int):
        super().__init__(timeout=None)
        container = ui.Container(accent_colour=accent_colour(event))
        container.add_item(ui.TextDisplay(f"## {event.title}"))
        if event.description:
            container.add_item(ui.TextDisplay(event.description))
        container.add_item(ui.Separator())
        container.add_item(ui.TextDisplay(format_details(event, len(signups), team_size)))
        container.add_item(ui.Separator())
        container.add_item(ui.TextDisplay(f"### Angemeldet\n{format_signup_list(signups)}"))
        if event.is_active:
            closed = not event.is_open
            container.add_item(ui.Separator(spacing=discord.SeparatorSpacing.large))
            container.add_item(ui.ActionRow(JoinButton(actions, disabled=closed), LeaveButton(actions, disabled=closed)))
        self.add_item(container)


class SignupDispatchView(ui.LayoutView):
    def __init__(self, actions: SignupActions):
        super().__init__(timeout=None)
        self.add_item(ui.ActionRow(JoinButton(actions), LeaveButton(actions)))


class SignupModal(ui.Modal):
    def __init__(self, actions: SignupActions, event_id: int, current: Signup | None = None):
        super().__init__(title="Anmeldung zum Turnier")
        self.actions = actions
        self.event_id = event_id
        self.name_input = ui.TextInput(
            placeholder="z. B. Steve",
            min_length=3,
            max_length=16,
            default=current.minecraft_name if current else None,
        )
        self.partner_input = ui.TextInput(
            placeholder="z. B. Alex",
            required=False,
            max_length=16,
            default=current.partner_name if current and current.partner_name else None,
        )
        self.add_item(ui.Label(text="Dein Minecraft-Name", component=self.name_input))
        self.add_item(
            ui.Label(
                text="Minecraft-Name deines Partners",
                description="Leer lassen, wenn dir ein Partner zugelost werden soll.",
                component=self.partner_input,
            )
        )

    async def on_submit(self, interaction: discord.Interaction) -> None:
        await self.actions.submit_signup(
            interaction, self.event_id, self.name_input.value, self.partner_input.value
        )
