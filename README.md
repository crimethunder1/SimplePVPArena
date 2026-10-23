# SimplePVPArena

A Paper plugin that runs a team versus team single elimination tournament in one arena on the server. Admins register teams, mark the border of the fight area, set the spawns and start the matches from an inventory menu. The plugin handles rounds, the round time limit with sudden death, the start gate, the kit and the cleaning of the arena after every match.

## Requirements

- Java 25
- A Paper server for Minecraft 26.2, built against `paper-api` 26.2.build.134-stable
- Maven to build

ViaVersion, ViaBackwards and ItemsAdder are optional soft dependencies, see "ItemsAdder (optional)" for the last one.

## Building

```
mvn clean package
```

The jar is written to `target/SimplePVPArena-1.0.0-SNAPSHOT.jar`. `mvn test` runs the unit tests.

## Installation

Copy the jar into the `plugins` folder of the server and start it. The plugin creates its folder with `config.yml` on the first start. Teams are stored in `teams.yml`, border blocks in `borders.yml`, start gates in `startgates.yml`, and the registrations for the next event with the registration stations in `registrations.yml`. An entry of `teams.yml` that cannot be read is skipped, and the file as it was read is kept as `teams.yml.broken` so the next save does not lose it.

A team has 1 to 8 players. Teams are added in the team menu of `/tournament` with one chat line, `<teamName> <player> [<player> ...]`, for example `Alpha Steve Alex`. The team name has 1 to 16 letters, digits, hyphens or underscores, and every player has to be online or known to the server.

A click on a team in the team menu opens its options. "Rename the team" asks for the new name in the chat, the team keeps its players and its place in the list. "Add a player" asks for a player name and accepts players up to the team size. "Remove a player" shows the players of the team, a click removes one; the last player stays, such a team is removed instead. Every change is written into `teams.yml`. While a tournament is running or paused no team can be renamed, the bracket keeps the names. A team that fights the running match or whose match waits for the kit choice can neither be changed nor removed until that match is over. Every other team can change its players or be removed during a tournament: a match keeps the players of its start, so a change counts from the next match of that team, and a removed team hands its next opponent a walkover.

The team size sets how many players a new team has at most, 2 by default (`match.team-size` in `config.yml`, 1 to 8). The tiles "Smaller teams" and "Larger teams" of the team menu change it by one and write it into `config.yml`, the comments of the file stay. The size cannot change while a tournament is running or paused, and it cannot become smaller than a team; such a team has to lose players or be removed first. Teams with fewer players stay as they are and take part, the start of a tournament names them to the admins, a missing player counts as absent. `teams.yml` keeps every team of 1 to 8 players whatever the size is.

## Commands and permissions

| Command | Permission | Default | Effect |
| --- | --- | --- | --- |
| `/tournament` (alias `/turnier`) | `tournament.admin` | op | Opens the menu that manages teams, the arena, border blocks, matches and the tournament |
| `/tournament spectate` | `tournament.spectate` | everyone | Sends the player back to the tribune of the arena, not possible while fighting |
| `/tournament bracket` | `tournament.view` | everyone | Opens the bracket as a menu with one page for every round, a player with `tournament.admin` may always open it |
| `/tournament join` | `tournament.view` | everyone | Registers the player for the next event, see "Registration and random teams" |
| `/tournament leave` | `tournament.view` | everyone | Takes the registration of the player back |
| `/tournament team` | `tournament.view` | everyone | Opens the team choice again for a registered player, see "Choosing a team" |
| `/tournament vote` | none | the players of the voting team | Opens the vote on who fights again while the larger team of a match votes, see "Teams with different numbers of players online" |

The bracket menu shows a tile for every match of a round with its two teams and whether it waits for its teams, is ready to be played, runs or was won, and it names the champion once the tournament is over. It opens on the round of the running match, "Previous round" and "Next round" turn the pages. It only shows the bracket: a click on a match does nothing and nothing can be taken out. It reads the bracket when it opens and when the round is changed, an open menu does not follow a match that is decided meanwhile. The tile "Bracket" in the main menu of `/tournament` opens it as well, with a tile back to the main menu. Every other menu needs `tournament.admin`.

## Registration and random teams

Players register for the next event themselves, at a registration station or with `/tournament join`, and take the registration back with `/tournament leave`. The list keeps the order of the registrations, holds a player at most once and has no limit. Registering and leaving are closed while a tournament is running or paused ("Registration is closed while a tournament is in progress"). An admin registers like every other player.

A registration station is any button of the world. The tile "Get the station tool" in the registrations menu hands out the registration station tool, a blaze rod that is recognised by a marker and never by its name. A right click with it on a button makes that button a station, a right click on a station makes it a normal button again, a click on anything else answers "Click a button". In the hand of a player without `tournament.admin` the tool does nothing and disappears from the inventory. Several stations are allowed, and a station can stand anywhere, also inside the area of effect, where the tool disappears on leaving like every item taken in there.

A right click on a station registers the player and presses the button as usual, the redstone signal is not held back. A second click answers "You are already registered, sneak and click the station or type /tournament leave to leave", a right click while sneaking takes the registration back. A player without `tournament.admin` cannot break a station ("This is a registration station"), an admin who breaks it removes the station.

The tile "Registrations" in the team menu opens the registrations menu. It shows a head for every registered player, a click on it removes that registration at once. The info tile names how many players are registered and how many teams that makes at the current team size. "Form random teams" asks for a confirmation and then replaces every existing team: the registered players are shuffled and split into teams of the team size, a smaller team takes the players that are left over (7 players at team size 2 make three teams of 2 and one team of 1). It is refused while a tournament is running or paused, while an auto match is announced or running, and when fewer than two teams would come out. The registrations stay, so the teams can be formed again. The admin reads how many teams were formed, every online player of a new team reads the team and the mates ("You play in team Red with Alex and Steve"). "Clear the registrations" removes every registration after a confirmation; it leaves the teams alone when the teams are formed at random, see "Choosing a team" for the free choice.

The first 16 random teams get a colour each and are named after it in the language of the server at that moment: Red, Blue, Green, Yellow, Orange, Purple, LightBlue, Pink, White, Gray, Black, Brown, Cyan, Lime, Magenta and LightGray (in German Rot, Blau, Grün, Gelb, Orange, Lila, Hellblau, Rosa, Weiß, Grau, Schwarz, Braun, Cyan, Lindgrün, Magenta and Hellgrau). Further teams are named `Team-17`, `Team-18` and so on and have no colour. The colour is stored as `colour` in `teams.yml` and kept when the team is renamed; teams added by hand and a `teams.yml` written before have no colour. During a match the names of the fighters show the colour of their team instead of the colour of their side (`match.team-colours`). A team without a colour, and both teams when they would show the same colour, keep the colours of the sides.

`registrations.yml` holds the registered players with their unique id and last known name, and the stations with their world and block coordinates. It is written at once after every change and read at the start of the server. A file that cannot be read is renamed to `registrations.yml.broken` (an older `.broken` file is kept, the new one then carries a time stamp in its name), the registrations and stations start empty, and the log and the online admins read why.

## Choosing a team

`registration.team-mode` in `config.yml` decides how the registered players get into teams. With `free-choice`, the default, the players choose their team themselves; with `random` an admin forms the teams with "Form random teams" as described above and nothing opens for the players. The tile "Team mode" in the registrations menu switches between both modes and writes the value into `config.yml`, a reload of `config.yml` applies it as well. "Form random teams" works in both modes.

With `free-choice` the menu "Choose your team" opens one tick after a player registers, at a station or with `/tournament join`, whether the auto match mode is on or a tournament is being prepared. `/tournament team` opens it again for a registered player. The menu shows a bed for every team colour that is needed for the registered players at the current team size (rounded up, at least two and at most 16, always in the order red, blue, green, yellow, orange and so on) and in addition the bed of every colour that already has a team. A bed lists the members of its team ("No one yet" while it is empty) and the places taken, for example "1 of 2 places". A click on a bed joins the team of that colour; the first player creates it, named after the colour in the language of the server, or with a number appended ("Red-2") when a team without that colour already holds the name. A player in another team leaves it first, a team left empty is removed. A full team is a grey tile, the own team reads "Your team", and "Leave your team" takes the player out of the team. Everyone who has the menu open sees the change at once, and the other members read who joined.

The team choice is closed while a tournament is running or paused. A team cannot be joined or left while it plays a match, while its players vote who fights, while it waits for the kit choice or while it is announced for an auto match. A player who takes the registration back, or whose registration an admin removes, leaves the coloured team as well; teams without a colour, which only an admin adds, are never touched, and with `random` every team stays as it is. "Clear the registrations" removes every team with a colour as well when the mode is `free-choice` and no tournament is in progress. The teams of the team choice are normal teams: they appear in the team menu, can be changed by an admin, play in the tournament and in auto matches, and show their colour during a match.

## Configuration

`config.yml` has `language` at the top (see "Languages") and seven sections, each value is described by a comment in the file:

- `match`: delay between rounds, round time limit, the seconds before the limit in which the action bar counts down to sudden death (`sudden-death-warning-seconds`, 45), the sudden death damage in its first second and the damage added for every further second (`sudden-death-damage-per-second`, 2.0, and `sudden-death-damage-increase-per-second`, 1.0), whether the final runs without sudden death, start gate duration, a countdown right before the fight of every round in which the fighters are blind and cannot be hurt and after which the round time limit starts (`countdown-seconds`, 3, 0 to 10, 0 turns it off), the seconds a fighter who loses the connection during a round has to come back before being eliminated (`reconnect-seconds`, 60, 0 to 600, 0 eliminates at once as before), what happens when the two teams of a match have different numbers of players online (`unequal-teams.mode`, `vote` or `all`, default `vote`, and `unequal-teams.vote-seconds`, 30, 5 to 120), the team size (`team-size`, 2 players) and team colours
- `auto-match`: whether two teams that each have a player online get a match without a tournament (`enabled`, false, also switched by the tile "Auto match mode" in the match menu), the seconds between the announcement of such a match and its start (`start-delay-seconds`, 10, 3 to 120) and the pause after it and its cleaning (`pause-seconds`, 20, 0 to 300)
- `registration`: whether the players choose their team in a menu of beds after registering or an admin forms the teams at random (`team-mode`, `free-choice` or `random`, default `free-choice`, also switched by the tile "Team mode" in the registrations menu), see "Choosing a team"
- `arena`: cleaning speed and particles, the blocks read per tick while the baseline of the fight area is taken (`baseline-blocks-per-tick`, 16384) and the layers the baseline covers and the cleaning restores, counted from the ground surface of the fight area down (`baseline-depth-below-floor`, 11, 0 to 64, from the next calculation), flood fill limit, safety zone radius, the margin of the area of effect (`effect-margin`, 96 blocks), whether joining players are sent to the tribune (`teleport-on-join`, true), whether every player hands in the inventory inside the area of effect (`area-inventory.enabled`, true, see "Inventory in the area of effect"), plus the fight area, the spawns and the place of the standings board the plugin writes itself
- `border`: particle display of border blocks (the ticks between two redraws, the view radius and the most blocks and connection lines shown to one player), the seconds the outline of a recognised wall is shown after a calculation (`wall-preview-seconds`, 4) and the marker of a confirmed wall, with whether that marker is shown (`confirmed-particle.enabled`, true, also switched by the tile "Border particles" in the arena menu; false hides only the marker, the outline shown after a calculation stays)
- `items-adder`: whether ItemsAdder is used (`enabled`, true), optional images for the countdown, an optional item for the totem at the start of the fight and an optional item floating above every registration station (`station-item`), taken from ItemsAdder, see "ItemsAdder (optional)"
- `placement`: whether the players inside the area of effect see the live placement as a sidebar (`sidebar.enabled`, true) and whether the standings board at the tribune is shown (`board.enabled`, true), see "Live placement"

Changes can be applied while the server runs with the "Reload config.yml" item in the main menu of `/tournament`. A changed `team-size` is taken over only under the rules of the team menu, otherwise the old size stays and the log and the online admins read why. At the start of the server the value of `config.yml` is always taken over, also during a restored tournament; a team with more players is then named in the log and to the online admins and keeps its players.

The kits live in `kits.yml` and are created from the inventory of an admin, replaced, deleted and chosen as the default kit in the kit menu of `/tournament`. Every fighter receives the default kit at the start of a round. A `kit` section that an older version saved in `config.yml` is taken over into `kits.yml` once as the kit `default`.

## Teams with different numbers of players online

When the two teams of a match have different numbers of players online right before the first round, for example 2 against 1, the larger team votes which of its players fight, as many as the smaller team has online. This happens after the kit choice of a tournament match and after the announcement of an auto match. Every online player of the larger team gets a menu with a head for every online player of the team, the own head included, and one vote that can be changed until the end; `/tournament vote` opens the menu again while the vote runs. The vote ends after `match.unequal-teams.vote-seconds` or once everyone has voted. The players with the most votes fight, a tie and the places nobody voted for are drawn by chance, and both teams and the players around the arena read "Alpha fights with Ben". The choice holds for every round of the match, a chosen player who is gone is not replaced, and the players who were not chosen can watch. While the vote runs the arena stays reserved for the match, its teams cannot be changed and the match cannot be restarted. With `match.unequal-teams.mode: all` every online player fights, as before this setting existed.

## Auto match mode

The tile "Auto match mode" in the match menu of `/tournament` switches the mode on and off and writes `auto-match.enabled` into `config.yml`. While it is on and no tournament is in progress, two teams that each have a player online get a match of their own: both teams and the players around the arena read "Auto match: Alpha against Bravo begins in 10 seconds", the match `auto-1` starts with the default kit and follows every rule of a normal match, the arena is cleaned afterwards and the next pair is announced after the pause. With more than two teams the two that have waited longest play next. A tournament has priority: it cannot be started while an auto match runs, it cancels an announced one, and the auto matches carry on once it is over. An auto match never enters the bracket or `tournament.yml`.

## Live placement

While a tournament runs, is paused or is over, and while the auto match mode is on and no tournament is in progress, the plugin shows the placement in two places. Without either of them both stay hidden.

- **Sidebar.** Every player inside the area of effect sees a sidebar "Tournament" or "Auto match" without numbers at the edge. During a tournament it shows "Round 2 of 3" ("Paused", "Over"), the running match as "Alpha 1 : 0 Bravo" with the team names in their colour (white without one) or "No match is running", "Still in: 4 teams" and up to eight of these teams with "and 2 more"; after the end the first places. In the auto match mode it shows the running auto match or "Waiting for the next auto match" and the ten teams with the most wins since the start of the server ("Alpha: 3 wins"). The sidebar lives on a scoreboard of its own for every such player, which mirrors every team of the main scoreboard (prefix, suffix, colour, options and members), so the names of the fighters keep their colour and the prefixes of other plugins stay. Objectives of other plugins are not mirrored, the player sees them again on leaving. On leaving the area the player gets the former scoreboard back.
- **Standings board.** The tile "Place the standings board here" in the arena menu saves the block the admin stands on; the board floats two blocks above it. A second click on the tile, now "Remove the standings board", removes it, and the reset of the arena removes it as well. The board shows the same as the sidebar, during a tournament also who is out in which round ("Out in round 1: Charlie and Delta"), and after the end the whole placement. The board is never saved in the world, it is put back after a restart.

The places of a finished tournament: 1 for the winner, 2 for the loser of the final, and the losers of an earlier round share the place "teams that survived that round plus 1": 3 for the semi final, 5 for the quarter final, 9 before that. A team that advanced without an opponent did not lose. The wins of the auto matches are kept in memory and start at zero with every start of the server.

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

### Inventory in the area of effect

Every player who enters the area of effect hands in the whole inventory, armour and off hand included, together with the item on the cursor and the items in the 2x2 crafting grid. Before that the plugin closes every open inventory view of the player, so the server puts the items of an open crafting table, anvil, loom or similar view and the item on the cursor back into the inventory; what does not fit because the inventory is full drops at the feet of the player inside the area. Admins and operators are not exempt, in every game mode. The plugin writes the inventory to `plugins/SimplePVPArena/area-inventories/<uuid>.yml` and clears it only after the file is written. The player gets it back on leaving the area, on changing the world, when the arena is reset or when the switch is turned off, and everything the player got inside, from the creative mode, from the plugin or picked up, is removed at that moment. The items of the cursor and of the crafting grid go into free slots, what does not fit is dropped at the feet of the player. Level, experience, health, hunger, effects, the game mode and the ender chest stay untouched. Inside the area nobody can drop items or open the ender chest, and a death drops nothing. The messages on entering and leaving come into the chat at most once a minute, otherwise into the action bar.

When the file cannot be written the player keeps the inventory, the plugin tries again, and the log and the online admins read a warning at most once a minute. A file that cannot be read is renamed to `<uuid>.yml.broken`, nothing is cleared, the admins read the player and the file name, and the items can be restored from that file by hand. A stop of the server gives every inventory back, the next start takes them again. After a crash the inventory comes back at the next join outside of the area. The snapshot taken at the start of a match stays separate: after the match the fighter gets back what was carried inside, and the own inventory on leaving the area.

Creative admins can put items into chests inside the area. Those chests belong to the arena, the cleaning resets the ones inside the fight area.

`arena.area-inventory.enabled: false` in `config.yml` turns it off. "Reload config.yml" then gives every inventory back at once.

## ItemsAdder (optional)

ItemsAdder is a soft dependency. When it runs on the server, the countdown before the fight can show images, the totem animation at the start of the fight can show an own item and an own item can float above every registration station. The plugin builds no resource pack of its own, the images and items come from the pack of ItemsAdder.

Without ItemsAdder the countdown shows 3, 2, 1 and "Fight!", every fighter sees the vanilla totem, and above every registration station floats the text "Register" with "Click to take part" below it, in the language of the server. The same holds with ItemsAdder while the IDs are empty or `items-adder.enabled` is false.

| Value in `config.yml` | Default | Effect | Suggested ID |
| --- | --- | --- | --- |
| `items-adder.enabled` | `true` | When true and ItemsAdder runs, the IDs below are looked up in ItemsAdder. false keeps the own titles and effects | |
| `items-adder.countdown-images.three` | `''` | Font image shown as the title instead of 3 | `simplepvparena:countdown_3` |
| `items-adder.countdown-images.two` | `''` | Font image shown as the title instead of 2 | `simplepvparena:countdown_2` |
| `items-adder.countdown-images.one` | `''` | Font image shown as the title instead of 1 | `simplepvparena:countdown_1` |
| `items-adder.countdown-images.fight` | `''` | Font image shown as the title instead of "Fight!" | `simplepvparena:countdown_fight` |
| `items-adder.totem-item` | `''` | Item every fighter sees in the totem animation at the start of the fight | `simplepvparena:totem` |
| `items-adder.station-item` | `''` | Item floating above every registration station instead of the text | `simplepvparena:station` |

The images are font images of ItemsAdder. They replace the title of the start animation and of the start gate countdown for the seconds 3, 2 and 1 and at the start of the fight; seconds above three keep their numbers, a second without an ID keeps its own title, the subtitle and the timing of the titles stay. For the totem the plugin asks ItemsAdder to play its totem animation with that item (`ItemsAdder.playTotemAnimation`) for every fighter; nothing in the inventory changes. When ItemsAdder does not play it, for example for an unknown ID or before ItemsAdder has loaded its content, the fighter sees the vanilla totem.

The display above a registration station is an entity that is never saved with the world. It hangs 0.7 blocks above the block of the button: an item display of `station-item` without rotation, or the text, which always faces the viewer. It appears when the server starts, when the chunk of the station loads and when a station is added, and goes away when the station is removed, when the chunk unloads and when the plugin stops. "Reload config.yml" rebuilds every display, so a new language or a new `station-item` shows at once, and when ItemsAdder finishes loading after the plugin the displays are rebuilt once with the item.

The IDs are looked up once ItemsAdder has loaded its content. An ID that ItemsAdder does not know, or a failure inside ItemsAdder, falls back to the own title or effect and writes one warning per ID into the log. A value that is no truth value or no text falls back to the default with a warning in the log and in the answer of "Reload config.yml".

"Reload config.yml" applies the IDs and the switch, false then ignores the IDs. Whether the plugin talks to ItemsAdder at all is decided when the server starts: installing or removing ItemsAdder, or starting with `items-adder.enabled: false` and switching it to true later, needs a restart of the server.
