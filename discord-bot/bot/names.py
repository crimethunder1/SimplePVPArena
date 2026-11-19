import re

MINECRAFT_NAME_PATTERN = re.compile(r"[A-Za-z0-9_]{3,16}")


class InvalidSignup(ValueError):
    pass


def is_valid_minecraft_name(name: str) -> bool:
    return MINECRAFT_NAME_PATTERN.fullmatch(name) is not None


def name_key(name: str) -> str:
    return name.lower()


def clean_signup_input(name: str, partner: str | None) -> tuple[str, str | None]:
    name = name.strip()
    partner = (partner or "").strip() or None
    if not is_valid_minecraft_name(name):
        raise InvalidSignup(
            "Dein Minecraft-Name ist ungültig. Erlaubt sind 3 bis 16 Zeichen aus Buchstaben, Zahlen und _."
        )
    if partner is not None and not is_valid_minecraft_name(partner):
        raise InvalidSignup(
            "Der Name deines Partners ist ungültig. Erlaubt sind 3 bis 16 Zeichen aus Buchstaben, Zahlen und _."
        )
    if partner is not None and name_key(partner) == name_key(name):
        raise InvalidSignup("Du kannst dich nicht selbst als Partner eintragen.")
    return name, partner
