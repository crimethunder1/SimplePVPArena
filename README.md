# SimplePVPArena

A Paper plugin that runs a team versus team single elimination tournament in one arena on the server. Admins register teams, mark the border of the fight area, set the spawns and start the matches from an inventory menu. The plugin handles rounds, the round time limit with sudden death, the start gate, the kit and the cleaning of the arena after every match.

## Requirements

- Java 25
- A Paper server for Minecraft 26.2, built against `paper-api` 26.2.build.134-stable
- Maven to build

ViaVersion and ViaBackwards are optional soft dependencies.

## Building

```
mvn clean package
```

The jar is written to `target/SimplePVPArena-1.0.0-SNAPSHOT.jar`. `mvn test` runs the unit tests.

## Installation

Copy the jar into the `plugins` folder of the server and start it. The plugin creates its folder with `config.yml` on the first start. Teams are stored in `teams.yml`, border blocks in `borders.yml` and start gates in `startgates.yml`. An entry of `teams.yml` that cannot be read is skipped, and the file as it was read is kept as `teams.yml.broken` so the next save does not lose it.

A team has 1 to 8 players. Teams are added in the team menu of `/tournament` with one chat line, `<teamName> <player> [<player> ...]`, for example `Alpha Steve Alex`. The team name has 1 to 16 letters, digits, hyphens or underscores, and every player has to be online or known to the server.

A click on a team in the team menu opens its options. "Rename the team" asks for the new name in the chat, the team keeps its players and its place in the list. "Add a player" asks for a player name and accepts players up to the team size. "Remove a player" shows the players of the team, a click removes one; the last player stays, such a team is removed instead. Every change is written into `teams.yml`. While a tournament is running or paused no team can be renamed, the bracket keeps the names. A team that fights the running match or whose match waits for the kit choice can neither be changed nor removed until that match is over. Every other team can change its players or be removed during a tournament: a match keeps the players of its start, so a change counts from the next match of that team, and a removed team hands its next opponent a walkover.

The team size sets how many players a new team has at most, 2 by default (`match.team-size` in `config.yml`, 1 to 8). The tiles "Smaller teams" and "Larger teams" of the team menu change it by one and write it into `config.yml`, the comments of the file stay. The size cannot change while a tournament is running or paused, and it cannot become smaller than a team; such a team has to lose players or be removed first. Teams with fewer players stay as they are and take part, the start of a tournament names them to the admins, a missing player counts as absent. `teams.yml` keeps every team of 1 to 8 players whatever the size is.

## Commands and permissions

| Command | Permission | Default | Effect |
| --- | --- | --- | --- |
| `/tournament` (alias `/turnier`) | `tournament.admin` | op | Opens the menu that manages teams, the arena, border blocks, matches and the tournament |
| `/tournament spectate` | `tournament.spectate` | everyone | Sends the player back to the tribune of the arena, not possible while fighting |
| `/tournament bracket` | `tournament.view` | everyone | Opens the bracket as a menu with one page for every round, a player with `tournament.admin` may always open it |

The bracket menu shows a tile for every match of a round with its two teams and whether it waits for its teams, is ready to be played, runs or was won, and it names the champion once the tournament is over. It opens on the round of the running match, "Previous round" and "Next round" turn the pages. It only shows the bracket: a click on a match does nothing and nothing can be taken out. It reads the bracket when it opens and when the round is changed, an open menu does not follow a match that is decided meanwhile. The tile "Bracket" in the main menu of `/tournament` opens it as well, with a tile back to the main menu. Every other menu needs `tournament.admin`.

## Configuration

`config.yml` has four sections, each value is described by a comment in the file:

- `match`: delay between rounds, round time limit, sudden death damage, whether the final runs without sudden death, start gate duration, a countdown right before the fight of every round in which the fighters are blind and cannot be hurt and after which the round time limit starts (`countdown-seconds`, 3, 0 to 10, 0 turns it off), the seconds a fighter who loses the connection during a round has to come back before being eliminated (`reconnect-seconds`, 60, 0 to 600, 0 eliminates at once as before), the team size (`team-size`, 2 players) and team colours
- `auto-match`: whether two teams that each have a player online get a match without a tournament (`enabled`, false, also switched by the tile "Auto match mode" in the match menu), the seconds between the announcement of such a match and its start (`start-delay-seconds`, 10, 3 to 120) and the pause after it and its cleaning (`pause-seconds`, 20, 0 to 300)
- `arena`: cleaning speed and particles, flood fill limit, safety zone radius, the margin of the area of effect (`effect-margin`, 96 blocks), whether joining players are sent to the tribune (`teleport-on-join`, true), plus the fight area and spawns the plugin writes itself
- `border`: particle display of border blocks and the marker of a confirmed wall

Changes can be applied while the server runs with the "Reload config.yml" item in the main menu of `/tournament`. A changed `team-size` is taken over only under the rules of the team menu, otherwise the old size stays and the log and the online admins read why. At the start of the server the value of `config.yml` is always taken over, also during a restored tournament; a team with more players is then named in the log and to the online admins and keeps its players.

The kits live in `kits.yml` and are created from the inventory of an admin, replaced, deleted and chosen as the default kit in the kit menu of `/tournament`. Every fighter receives the default kit at the start of a round. A `kit` section that an older version saved in `config.yml` is taken over into `kits.yml` once as the kit `default`.

## Auto match mode

The tile "Auto match mode" in the match menu of `/tournament` switches the mode on and off and writes `auto-match.enabled` into `config.yml`. While it is on and no tournament is in progress, two teams that each have a player online get a match of their own: both teams and the players around the arena read "Auto match: Alpha against Bravo begins in 10 seconds", the match `auto-1` starts with the default kit and follows every rule of a normal match, the arena is cleaned afterwards and the next pair is announced after the pause. With more than two teams the two that have waited longest play next. A tournament has priority: it cannot be started while an auto match runs, it cancels an announced one, and the auto matches carry on once it is over. An auto match never enters the bracket or `tournament.yml`.

## Languages

The texts players and admins see come from language files. English (`en`) and German (`de`) are shipped. On the first start the plugin copies `lang/en.yml` and `lang/de.yml` into `plugins/SimplePVPArena/lang/`. `language` at the top of `config.yml` picks the language, `en` by default. Any other value counts as `en`, the log and the answer of the reload name it.

The files can be changed to adjust the wording. Every value stands in single quotes, a text of several lines is a list of lines, and words in braces such as `{team}` are filled in by the plugin and have to stay. A key that is missing in the file falls back to the shipped text of the same language, then to the shipped English text. A file that is no valid YAML is not used at all: the shipped texts of that language apply, and the log and the online admins name the file and the reason.

The plugin never overwrites or rewrites the files in the `lang` folder, also not after an update. When an update brings new texts, the log lists once after loading how many keys a file lacks and the first ten of them, their shipped texts are used until they are added by hand. To start over with the shipped texts, delete the file, the next start copies it again.

"Reload config.yml" in the main menu reads `language` and the language files again. Texts created afterwards use them, a menu that is open keeps its texts until it is opened again, and messages already sent stay as they were. The answer of the reload already uses the new language, its warnings about entries that cannot be used included.

A chat input of the menu is cancelled with `cancel` in every language, in German also with `abbrechen`.

Every text a player or an admin sees in the game follows the language: the chat prefix, the menus with their tiles, chat inputs and confirmations, the messages of the teams, the kits and the kit choice, the arena with its setup, baseline, cleaning, height marker and the way to a gap, the border wall, the start gate, the spectators, the listeners and the names and lore of the items, the tournament and the match with their announcements, titles and action bar, the notices to the admins, the tournament state and the bracket, the answers of `/tournament`, the dry run, the answer of "Reload config.yml", the warnings about entries of `config.yml` that cannot be used and the notice when the kits cannot be loaded at the start. Counts use the singular or the plural form of a text, lists put "and" ("und") before the last entry, and a tile named in a message carries the title it has in the menu of the same language. An item keeps the name of the language it was handed out in and works the same in every language.

A message that reaches the admins in the game and the log, such as a warning about an entry of `config.yml`, is written in the log in the same language. Some parts stay English on purpose: lines that only reach the server log, `plugin.yml`, the paths and comments of `config.yml`, the command `/tournament` and its permissions, the match ids, the names of the test teams of the dry run, and the technical reasons inside some notices, such as why a saved tournament, a saved player state or `kits.yml` could not be read. Numbers with a decimal point keep the point in every language.

## Area of effect

The protection of the world and the pvp lock only apply around the arena. The area of effect is a circle around the centre of the frame of the fight area, in the world of the arena and at every height. Its radius is the distance from that centre to the farthest corner of the frame plus `arena.effect-margin` from `config.yml`. The info tile of the arena menu shows the centre and the radius. Without a calculated fight area there is no area of effect.

Inside the area of effect, players who are no admins cannot build or break outside the fight area, explosions, fire, pistons and entities do not change blocks outside the fight area or under its floor, and a player standing in it is only hurt by another player when both fight the running round or when an admin hits an active fighter. Outside of it the plugin leaves building, explosions, fire, pistons and player against player alone. The rules of the fight area itself, the safety zone with its own radius and the item, mob and damage rules of fighters and spectators do not depend on it.

The tribune spawn has to lie inside the area of effect once the fight area is calculated. A calculation that leaves the stored tribune outside of the new area stays valid and warns the admin. With `arena.teleport-on-join: false` joining players stay where they are; players of the running match are still sent to the tribune.

The messages of a match reach the players inside the area of effect and the members of both teams wherever they stand: the announcement of the match, a walkover, the countdown of the start gate, the chat lines about rounds, points and the end of the match, the sudden death warning and title, a title with the score after every round that leaves the match open, a title with the winner after the match, and the next match in the chat and the action bar after every decided match, also after a walkover or a decision in the match menu ("Next: Charlie against Delta (round1-match2)", or the finalists, who wait for an admin to start the final). The start, pause, continuation, cancellation and end of the tournament and the teams advancing without a first round opponent are sent to the whole server.
