# SimplePVPArena Discord-Bot

Ein kleiner Discord-Bot für die Turnier-Anmeldung. Spieler melden sich per Button auf Discord an, ein Admin exportiert die Anmeldungen als `.txt` und lädt die Datei auf den Minecraft-Server hoch. SimplePVPArena baut daraus die Teams (Standard: 2v2).

## Einrichtung

Voraussetzung: Python 3.12 oder neuer.

1. Im [Discord Developer Portal](https://discord.com/developers/applications) eine Anwendung mit Bot anlegen und den Token kopieren.
2. Den Bot mit den Scopes `bot` und `applications.commands` auf den Server einladen. Er braucht im Event-Kanal die Rechte *Kanal ansehen*, *Nachrichten senden* und *Nachrichtenverlauf lesen*.
3. Umgebung einrichten:

   ```
   cd discord-bot
   py -3 -m venv .venv
   .venv\Scripts\activate
   pip install -r requirements.txt
   copy .env.example .env
   ```

   Unter Linux: `python3 -m venv .venv`, `source .venv/bin/activate`, `cp .env.example .env`.

4. `.env` ausfüllen:

   | Variable | Bedeutung |
   |---|---|
   | `DISCORD_TOKEN` | Bot-Token |
   | `GUILD_ID` | ID des Discord-Servers, auf dem die Befehle registriert werden |
   | `ADMIN_ROLE_ID` | Rolle, die die `/event`-Befehle nutzen darf (Mitglieder mit *Server verwalten* dürfen es immer) |
   | `DB_PATH` | optional, Pfad zur SQLite-Datei, Standard `data/signups.db` (relativ zum Ordner `discord-bot`) |
   | `TEAM_SIZE` | optional, Standard `2`, wird nur für die Anzeige („2v2“) verwendet |

5. Starten:

   ```
   python -m bot.main
   ```

## Befehle

Alle `/event`-Befehle sind nur für Admins (Admin-Rolle oder *Server verwalten*).

| Befehl | Beschreibung |
|---|---|
| `/event create title [description] [date]` | Postet die Anmeldung im aktuellen Kanal. Ein bereits aktives Event wird dabei archiviert. |
| `/event close` | Schließt die Anmeldung, die Buttons werden deaktiviert. |
| `/event open` | Öffnet die Anmeldung wieder. |
| `/event export` | Schickt dir die Datei `teams-<event>.txt` und eine kurze Übersicht (Duos, Solos, Spieler). |
| `/event list` | Zeigt alle Anmeldungen inklusive Discord-Nutzer. |
| `/event remove name` | Entfernt eine Anmeldung anhand des Minecraft-Namens. |

Spieler klicken auf **Anmelden**, tragen ihren Minecraft-Namen und optional den Namen ihres Partners ein. Wer erneut auf **Anmelden** klickt, ändert seine bestehende Anmeldung. **Abmelden** löscht die eigene Anmeldung. Die Nachricht aktualisiert sich dabei automatisch, und die Buttons funktionieren auch nach einem Neustart des Bots weiter.

## Export-Format

- UTF-8, eine Zeile pro Anmeldegruppe, Zeilenende `\n`, keine Kopfzeile, keine Leerzeilen.
- Zwei Namen in einer Zeile sind ein festes Duo, ein einzelner Name ist ein Solo-Spieler, der vom Plugin zufällig zugelost wird.
- Kein Name kommt doppelt vor. Groß-/Kleinschreibung wird beim Vergleich ignoriert, ausgegeben wird die Schreibweise des Spielers.

```
Steve Alex
Notch
Herobrine Jeb_
Dinnerbone
```

So werden Partner zugeordnet:

- A gibt B an und B gibt A an: eine Zeile `A B`.
- A gibt B an und B hat sich nicht selbst angemeldet: `A B`. Geben mehrere Spieler denselben B an, bekommt ihn nur die früheste Anmeldung, die anderen werden Solo.
- A gibt B an, aber B hat sich solo oder mit jemand anderem angemeldet: A wird Solo.
- Die Reihenfolge entspricht der Anmeldereihenfolge. Wer seine Anmeldung ändert, behält seinen Platz.

## Tests

```
pip install -r requirements-dev.txt
python -m pytest
```
