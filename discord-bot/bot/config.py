import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DB_PATH = "data/signups.db"
DEFAULT_TEAM_SIZE = 2


class ConfigError(Exception):
    pass


@dataclass(frozen=True)
class Settings:
    token: str
    guild_id: int
    admin_role_id: int
    db_path: Path
    team_size: int


def load_settings(env_file: Path | None = None) -> Settings:
    load_dotenv(env_file or PROJECT_ROOT / ".env")
    return Settings(
        token=read_required("DISCORD_TOKEN"),
        guild_id=read_int("GUILD_ID"),
        admin_role_id=read_int("ADMIN_ROLE_ID"),
        db_path=resolve_db_path(os.getenv("DB_PATH", "").strip() or DEFAULT_DB_PATH),
        team_size=read_team_size(),
    )


def read_required(key: str) -> str:
    value = os.getenv(key, "").strip()
    if not value:
        raise ConfigError(f"{key} fehlt in der .env")
    return value


def read_int(key: str) -> int:
    value = read_required(key)
    try:
        return int(value)
    except ValueError:
        raise ConfigError(f"{key} muss eine Zahl sein, ist aber '{value}'") from None


def read_team_size() -> int:
    if not os.getenv("TEAM_SIZE", "").strip():
        return DEFAULT_TEAM_SIZE
    team_size = read_int("TEAM_SIZE")
    if team_size < 1:
        raise ConfigError("TEAM_SIZE muss mindestens 1 sein")
    return team_size


def resolve_db_path(raw_path: str) -> Path:
    path = Path(raw_path)
    if not path.is_absolute():
        path = PROJECT_ROOT / path
    return path
