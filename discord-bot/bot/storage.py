import sqlite3
from dataclasses import dataclass
from datetime import datetime, timezone
from enum import Enum
from pathlib import Path

from bot.names import name_key

SCHEMA = """
CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id INTEGER NOT NULL,
    channel_id INTEGER NOT NULL,
    message_id INTEGER,
    title TEXT NOT NULL,
    description TEXT,
    event_date TEXT,
    is_open INTEGER NOT NULL DEFAULT 1,
    is_active INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS events_by_guild ON events (guild_id, is_active);

CREATE TABLE IF NOT EXISTS signups (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    event_id INTEGER NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    discord_user_id INTEGER NOT NULL,
    minecraft_name TEXT NOT NULL,
    name_key TEXT NOT NULL,
    partner_name TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    UNIQUE (event_id, name_key),
    UNIQUE (event_id, discord_user_id)
);
"""


@dataclass(frozen=True)
class Event:
    id: int
    guild_id: int
    channel_id: int
    message_id: int | None
    title: str
    description: str | None
    event_date: str | None
    is_open: bool
    is_active: bool


@dataclass(frozen=True)
class Signup:
    id: int
    event_id: int
    discord_user_id: int
    minecraft_name: str
    partner_name: str | None


class SignupOutcome(Enum):
    CREATED = "created"
    UPDATED = "updated"
    NAME_TAKEN = "name_taken"


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def event_from_row(row: sqlite3.Row) -> Event:
    return Event(
        id=row["id"],
        guild_id=row["guild_id"],
        channel_id=row["channel_id"],
        message_id=row["message_id"],
        title=row["title"],
        description=row["description"],
        event_date=row["event_date"],
        is_open=bool(row["is_open"]),
        is_active=bool(row["is_active"]),
    )


def signup_from_row(row: sqlite3.Row) -> Signup:
    return Signup(
        id=row["id"],
        event_id=row["event_id"],
        discord_user_id=row["discord_user_id"],
        minecraft_name=row["minecraft_name"],
        partner_name=row["partner_name"],
    )


class Storage:
    def __init__(self, path: Path | str):
        if str(path) != ":memory:":
            Path(path).parent.mkdir(parents=True, exist_ok=True)
        self.connection = sqlite3.connect(path)
        self.connection.row_factory = sqlite3.Row
        self.connection.execute("PRAGMA foreign_keys = ON")
        self.connection.executescript(SCHEMA)

    def close(self) -> None:
        self.connection.close()

    def create_event(
        self,
        guild_id: int,
        channel_id: int,
        title: str,
        description: str | None = None,
        event_date: str | None = None,
    ) -> Event:
        with self.connection:
            cursor = self.connection.execute(
                "INSERT INTO events (guild_id, channel_id, title, description, event_date, created_at) "
                "VALUES (?, ?, ?, ?, ?, ?)",
                (guild_id, channel_id, title, description, event_date, utc_now()),
            )
        return self.get_event(cursor.lastrowid)

    def activate_event(self, event_id: int, message_id: int) -> Event:
        event = self.get_event(event_id)
        with self.connection:
            self.connection.execute(
                "UPDATE events SET is_active = 0 WHERE guild_id = ? AND id != ?",
                (event.guild_id, event_id),
            )
            self.connection.execute(
                "UPDATE events SET is_active = 1, message_id = ? WHERE id = ?",
                (message_id, event_id),
            )
        return self.get_event(event_id)

    def delete_event(self, event_id: int) -> None:
        with self.connection:
            self.connection.execute("DELETE FROM events WHERE id = ?", (event_id,))

    def get_event(self, event_id: int) -> Event | None:
        row = self.connection.execute("SELECT * FROM events WHERE id = ?", (event_id,)).fetchone()
        return event_from_row(row) if row else None

    def active_event(self, guild_id: int) -> Event | None:
        row = self.connection.execute(
            "SELECT * FROM events WHERE guild_id = ? AND is_active = 1 ORDER BY id DESC LIMIT 1",
            (guild_id,),
        ).fetchone()
        return event_from_row(row) if row else None

    def active_events(self) -> list[Event]:
        rows = self.connection.execute("SELECT * FROM events WHERE is_active = 1 ORDER BY id").fetchall()
        return [event_from_row(row) for row in rows]

    def set_open(self, event_id: int, is_open: bool) -> Event:
        with self.connection:
            self.connection.execute("UPDATE events SET is_open = ? WHERE id = ?", (int(is_open), event_id))
        return self.get_event(event_id)

    def signups(self, event_id: int) -> list[Signup]:
        rows = self.connection.execute(
            "SELECT * FROM signups WHERE event_id = ? ORDER BY id", (event_id,)
        ).fetchall()
        return [signup_from_row(row) for row in rows]

    def signup_for_user(self, event_id: int, discord_user_id: int) -> Signup | None:
        row = self.connection.execute(
            "SELECT * FROM signups WHERE event_id = ? AND discord_user_id = ?",
            (event_id, discord_user_id),
        ).fetchone()
        return signup_from_row(row) if row else None

    def save_signup(
        self,
        event_id: int,
        discord_user_id: int,
        minecraft_name: str,
        partner_name: str | None,
    ) -> SignupOutcome:
        key = name_key(minecraft_name)
        now = utc_now()
        try:
            with self.connection:
                owner = self.connection.execute(
                    "SELECT discord_user_id FROM signups WHERE event_id = ? AND name_key = ?",
                    (event_id, key),
                ).fetchone()
                if owner is not None and owner["discord_user_id"] != discord_user_id:
                    return SignupOutcome.NAME_TAKEN
                updated = self.connection.execute(
                    "UPDATE signups SET minecraft_name = ?, name_key = ?, partner_name = ?, updated_at = ? "
                    "WHERE event_id = ? AND discord_user_id = ?",
                    (minecraft_name, key, partner_name, now, event_id, discord_user_id),
                )
                if updated.rowcount:
                    return SignupOutcome.UPDATED
                self.connection.execute(
                    "INSERT INTO signups "
                    "(event_id, discord_user_id, minecraft_name, name_key, partner_name, created_at, updated_at) "
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    (event_id, discord_user_id, minecraft_name, key, partner_name, now, now),
                )
                return SignupOutcome.CREATED
        except sqlite3.IntegrityError:
            return SignupOutcome.NAME_TAKEN

    def remove_signup_for_user(self, event_id: int, discord_user_id: int) -> Signup | None:
        signup = self.signup_for_user(event_id, discord_user_id)
        if signup is not None:
            self.delete_signup(signup.id)
        return signup

    def remove_signup_by_name(self, event_id: int, minecraft_name: str) -> Signup | None:
        row = self.connection.execute(
            "SELECT * FROM signups WHERE event_id = ? AND name_key = ?",
            (event_id, name_key(minecraft_name.strip())),
        ).fetchone()
        if row is None:
            return None
        signup = signup_from_row(row)
        self.delete_signup(signup.id)
        return signup

    def delete_signup(self, signup_id: int) -> None:
        with self.connection:
            self.connection.execute("DELETE FROM signups WHERE id = ?", (signup_id,))
