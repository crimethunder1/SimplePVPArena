import re
import unicodedata
from collections.abc import Sequence
from dataclasses import dataclass

GERMAN_LETTERS = str.maketrans({"ä": "ae", "ö": "oe", "ü": "ue", "ß": "ss"})
SLUG_MAX_LENGTH = 40


@dataclass(frozen=True)
class ExportSummary:
    duos: int
    solos: int
    players: int


def render_export(lines: Sequence[tuple[str, ...]]) -> str:
    return "".join(" ".join(line) + "\n" for line in lines)


def summarize(lines: Sequence[tuple[str, ...]]) -> ExportSummary:
    return ExportSummary(
        duos=sum(1 for line in lines if len(line) == 2),
        solos=sum(1 for line in lines if len(line) == 1),
        players=sum(len(line) for line in lines),
    )


def make_slug(title: str, fallback: str) -> str:
    text = unicodedata.normalize("NFKD", title.lower().translate(GERMAN_LETTERS))
    text = text.encode("ascii", "ignore").decode("ascii")
    slug = re.sub(r"[^a-z0-9]+", "-", text).strip("-")[:SLUG_MAX_LENGTH].strip("-")
    return slug or fallback


def export_filename(title: str, event_id: int) -> str:
    return f"teams-{make_slug(title, f'event-{event_id}')}.txt"
