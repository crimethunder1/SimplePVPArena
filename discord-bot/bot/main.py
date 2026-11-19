import logging

import discord
from discord import app_commands

from bot.board import EventBoard
from bot.commands import EventCommands
from bot.config import ConfigError, Settings, load_settings
from bot.storage import Storage
from bot.views import SignupDispatchView

log = logging.getLogger("arena_bot")


class ArenaBot(discord.Client):
    def __init__(self, settings: Settings, storage: Storage):
        super().__init__(intents=discord.Intents.default())
        self.settings = settings
        self.storage = storage
        self.tree = app_commands.CommandTree(self)
        self.board = EventBoard(self, storage, settings.team_size)
        self.messages_restored = False

    async def setup_hook(self) -> None:
        self.add_view(SignupDispatchView(self.board))
        guild = discord.Object(id=self.settings.guild_id)
        self.tree.add_command(
            EventCommands(self.board, self.storage, self.settings.admin_role_id),
            guild=guild,
        )
        await self.tree.sync(guild=guild)

    async def on_ready(self) -> None:
        log.info("Logged in as %s", self.user)
        if not self.messages_restored:
            self.messages_restored = True
            await self.board.refresh_all_active()


def main() -> None:
    discord.utils.setup_logging()
    try:
        settings = load_settings()
    except ConfigError as error:
        log.error("Konfigurationsfehler: %s", error)
        raise SystemExit(1) from None
    storage = Storage(settings.db_path)
    try:
        ArenaBot(settings, storage).run(settings.token, log_handler=None)
    finally:
        storage.close()


if __name__ == "__main__":
    main()
