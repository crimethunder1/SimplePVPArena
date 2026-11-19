from collections.abc import Iterable
from dataclasses import dataclass

from bot.names import name_key


@dataclass(frozen=True)
class Registration:
    name: str
    partner: str | None = None


def build_team_lines(registrations: Iterable[Registration]) -> list[tuple[str, ...]]:
    registered: dict[str, Registration] = {}
    for registration in registrations:
        registered.setdefault(name_key(registration.name), registration)

    taken: set[str] = set()
    lines: list[tuple[str, ...]] = []
    for key, registration in registered.items():
        if key in taken:
            continue
        taken.add(key)
        partner = find_partner(registration, registered, taken)
        if partner is None:
            lines.append((registration.name,))
        else:
            taken.add(name_key(partner))
            lines.append((registration.name, partner))
    return lines


def find_partner(
    registration: Registration,
    registered: dict[str, Registration],
    taken: set[str],
) -> str | None:
    if not registration.partner:
        return None
    partner_key = name_key(registration.partner)
    if partner_key == name_key(registration.name) or partner_key in taken:
        return None
    partner_registration = registered.get(partner_key)
    if partner_registration is None:
        return registration.partner
    if partner_registration.partner and name_key(partner_registration.partner) == name_key(registration.name):
        return partner_registration.name
    return None
