# SimplePVPArena

A Paper plugin that runs a team versus team single elimination tournament in one arena on the server. Players register and choose their team, or admins add the teams and form them at random. Admins mark the border of the fight area, set the spawns and start the matches from an inventory menu. The plugin handles the bracket, the rounds, the round time limit with sudden death, the start gate and the countdown before the fight, the kits and the kit choice, the cleaning of the arena after every match, auto matches without a tournament and a live placement.

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

Copy the jar into the `plugins` folder of the server and start it. The plugin creates its folder with `config.yml` on the first start. Teams are stored in `teams.yml`, border blocks in `borders.yml`, start gates in `startgates.yml`, and the registrations for the next event with the registration stations in `registrations.yml`. An entry of `teams.yml` that cannot be read is skipped, and the file as it was read is kept as `teams.yml.broken` so the next save does not lose it. Every file the plugin writes is listed in "Files in the plugin folder".

## Updating from an older version

Stop the server, replace the jar and start it again. An existing `config.yml` is not extended: an entry it lacks uses its default, the comments of the shipped file are not added. To change such an entry, add it by hand with the name from "Configuration". Do not delete `config.yml` to get a fresh one, it also holds the calculated fight area, the spawns and the place of the standings board.

These defaults change the behaviour of a server that updates from a version without the entry:

- `match.reconnect-seconds: 60`: a fighter who loses the connection during a round has 60 seconds to come back instead of being eliminated at once. `0` brings the old behaviour back.
- `match.countdown-seconds: 3`: the fighters are blind and cannot be hurt for three seconds before every fight, the round time limit starts after them. `0` brings the old behaviour back.
- `arena.area-inventory.enabled: true`: every player, admins included, hands in the whole inventory inside the area of effect and gets it back on leaving. `false` keeps the inventory with the player.
- `match.unequal-teams.mode: vote`: when the two teams of a match have different numbers of players online, the larger team votes who fights. `all` lets every online player fight as before.
- `registration.team-mode: free-choice`: a player who registers gets the menu of beds to choose a team. `random` opens nothing, an admin forms the teams.

A `kit` section that an older version saved in `config.yml` is taken over into `kits.yml` once, see "Kits and the kit choice". The language files in the `lang` folder are never overwritten, new texts come from the shipped files until they are added by hand, see "Languages". A `teams.yml` written before has no team colours.

## Teams

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

## Tournament

The match menu of `/tournament` ("Match and tournament") starts, pauses, continues and cancels the tournament. "Start the tournament" needs at least two teams and an even number of them, "Force start" starts with an uneven number. The arena has to be ready, with a calculated fight area and the tribune spawn; "Dry run of the pipeline" also names a missing team spawn. The bracket is padded to the next power of two, the teams without a first round opponent advance at once and these byes are spread evenly over the bracket.

The matches are played one after another on the one arena. A match is won by the first team with two points, a team scores a point when the other team has no active fighter left. After every point both teams are sent back to their spawns and get the kit again. A round has a time limit (`match.round-timeout-seconds`); when it runs out, sudden death hurts every active fighter every second with a growing damage. The final has no time limit and no sudden death by default (`match.disable-sudden-death-in-final`). When a match is decided the arena is cleaned and the next open match follows, after its kit choice. The final never starts by itself: once both finalists are known, the online admins are told and "Start the final" starts it.

"Pause the tournament" starts no further match until "Continue the tournament", the round timer of a running round waits meanwhile. "Cancel the tournament" stops the running match and the bracket. "Force a result" decides the running match for one of its teams, "Decide a waiting match" decides a match that is not running, "Restart the match" stops the running tournament match and begins it again at 0 : 0 with the kit choice and the countdown, and "Take back a result" opens the decided matches whose following match has not started yet. "Tournament status" in the main menu writes the state, the rounds and the running match into the chat, "Dry run of the pipeline" checks the arena, the spawns, the kit, the start gate and the cleaning without starting anything.

The bracket and its results are written to `tournament.yml` after the start, after every decided match and after every pause and continue; cancelling the tournament and its end remove the file. After a restart of the server the tournament comes back with the same bracket and results, always paused, and "Continue the tournament" carries on. A match that was running when the server stopped starts again from the beginning. A `tournament.yml` that cannot be read is renamed to `tournament.yml.broken`, no tournament is restored and the reason is named.

## Arena setup

The arena has a fight area, the spawns of both teams and the tribune spawn, all set in the arena menu of `/tournament`.

- **Border blocks.** "Mark border blocks by clicking them" in the main menu switches a mode on in which every right click of the admin marks a block as a border block or takes the mark away. The marked blocks form the wall around the fight area and are stored in `borders.yml`. While the mode is on, chests, doors and other blocks stay shut.
- **Fight area.** "Calculate the fight area" fills the room inside the closed wall from the position of the admin, up to `arena.shape-maximum-cells` cells. The recognised wall is shown for `border.wall-preview-seconds`. When the fill escapes through a gap, the calculation fails and the admin gets a compass and a particle trail towards the gap. "Diagnose with a higher cell limit" runs one calculation with a typed limit and leaves `config.yml` untouched.
- **Height of the fight area.** "Mark the height of the fight area" hands out an item; a block placed with it marks the lowest point, the item then marks the highest point, and the placed blocks disappear again. The next calculation takes both as the floor and the ceiling of the fight area.
- **Spawns.** "Set the first team spawn" and "Set the second team spawn" take the position of the admin inside the fight area, "Set the tribune spawn" a position outside of it and inside the area of effect.
- **Start gate.** "Give start gate blocks" asks for a material and an amount from 1 to 64 in the chat and hands out that many start gate blocks, with a full inventory only what fits, and the message names the blocks that arrived. A box built around a team spawn and closed with them is the start gate, stored in `startgates.yml`. Every round start puts the whole gate back and takes it apart from the top over `match.start-gate-duration-seconds`. Without a placed gate block a round starts without a gate.
- **Cleaning.** The calculation reads the blocks of the fight area and the layers under its floor (`arena.baseline-depth-below-floor`) as the baseline. After every match the cleaning writes the baseline back, so everything built or broken during the match is undone; "Clean the arena" starts the same cleaning by hand. The baseline is kept in memory only: after a restart of the server, and for a fight area larger than a baseline covers, the arena is not cleaned between the matches and the admins are told, "Clean the arena" then sets every block of the fight area to air. A new calculation takes the baseline again.
- **Reset.** "Reset the arena" drops the fight area, the three spawns and the standings board after a confirmation, the blocks and the border blocks stay. It is refused while a match runs.
- **Clearing the marks.** "Clear the border marks" and "Clear the start gate marks" forget every marked border block or start gate block after a confirmation and write `borders.yml` or `startgates.yml` again; the blocks in the world stay as they are, and the calculated fight area stays. Both are refused while a match runs, like the reset.
- **Border particles.** The tile "Border particles" shows or hides the marker at the confirmed border wall.
- **Margin of the area of effect.** "Smaller area of effect" and "Larger area of effect", left and right of the info tile, show the margin around the fight area and, with a calculated fight area, the radius of the area of effect. A click changes `arena.effect-margin` by 8 blocks, a shift click by 1 block, from 0 to 512; at a limit the tile is grey. The new margin holds at once and is written to `config.yml`, players who are now outside get their inventory back at the next check every second and players who are now inside hand it in. Both are refused while a match runs, like the reset.

## Kits and the kit choice

The kits live in `kits.yml` and are created from the inventory of an admin, replaced, deleted and chosen as the default kit in the kit menu of `/tournament`. A kit name has 1 to 32 letters, digits, hyphens or underscores. The first kit becomes the default kit, the default kit and the last kit cannot be deleted. Every fighter receives the default kit at the start of a round. At the start of a match the inventory and the state of every fighter are saved in `player-snapshots/<uuid>.yml` and given back after the match. A `kit` section that an older version saved in `config.yml` is taken over into `kits.yml` once as the kit `default`. A `kits.yml` that cannot be read is renamed to `kits.yml.broken`, and there is no kit until one is created.

Before every tournament match, the final included, every online admin gets the popup "Choose the kit for <match>" and the chat line with the match and its teams. The first click on a kit counts for this match only, the default kit stays as it is. After 30 seconds without a choice the default kit is used, a reminder comes with 10 seconds left. With only one kit the match starts at once with it, without a kit with an empty one, and without an online admin at once with the default kit. The tile "Choose the kit for the next match" in the main menu opens the popup again while the choice is open. An auto match always uses the default kit.

## Reconnect

A fighter of the running round who loses the connection is not eliminated at once. The fighter stays in the round for `match.reconnect-seconds` (60 by default) and keeps kit, inventory, health and effects, and the team counts as alive. The players of the area read "Steve lost the connection, 60 seconds to come back". A fighter who is back in time fights on where they left, "Steve is back". When the time runs out, "Steve did not come back and is eliminated". Every fighter has one window per round: a fighter who comes back and leaves again in the same round is eliminated at once. A fighter who leaves during sudden death is eliminated at once. `0` eliminates at once.

## Start animation

Right before the fight of every round the fighters are blind and cannot be hurt for `match.countdown-seconds` (3 by default, 0 to 10). The players of the area read the titles 3, 2, 1 and "Fight!". With a start gate the countdown is the last seconds of the gate countdown. At "Fight!" the blindness ends, every fighter sees the totem animation, and the round time limit starts. `0` turns the animation off, the time limit then starts with the round. With ItemsAdder the titles can be images and the totem an own item, see "ItemsAdder (optional)".

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

`config.yml` has `language` at the top (see "Languages") and seven sections, each value is described by a comment in the file. The tables list every value with its default.

| Value | Default | Effect |
| --- | --- | --- |
| `language` | `en` | `en` or `de`, see "Languages" |

`match`:

| Value | Default | Effect |
| --- | --- | --- |
| `round-reset-delay-ticks` | `100` | Ticks between the end of a round and the start of the next one, 20 ticks are one second |
| `round-timeout-seconds` | `300` | Time limit of a round, sudden death starts when it runs out, at least 1 |
| `sudden-death-warning-seconds` | `45` | Seconds before the time limit in which the action bar counts down to sudden death |
| `sudden-death-damage-per-second` | `2.0` | Damage every active fighter takes in the first second of sudden death |
| `sudden-death-damage-increase-per-second` | `1.0` | Damage added for every further second of sudden death |
| `disable-sudden-death-in-final` | `true` | The rounds of the final have no time limit and no sudden death |
| `start-gate-duration-seconds` | `10` | Seconds a placed start gate takes to open layer by layer, at least 1 |
| `countdown-seconds` | `3` | Countdown right before the fight of every round, 0 to 10, 0 turns it off, see "Start animation" |
| `reconnect-seconds` | `60` | Seconds a fighter who loses the connection during a round has to come back, 0 to 600, 0 eliminates at once, see "Reconnect" |
| `unequal-teams.mode` | `vote` | `vote` or `all`, see "Teams with different numbers of players online", from the next match |
| `unequal-teams.vote-seconds` | `30` | Seconds the vote lasts, 5 to 120 |
| `team-size` | `2` | Players per team, 1 to 8, see "Teams" |
| `team-colours.first-team` | `red` | Scoreboard colour of the first team of a match, a Minecraft colour name |
| `team-colours.second-team` | `aqua` | Scoreboard colour of the second team, different from the first |

`auto-match`, see "Auto match mode":

| Value | Default | Effect |
| --- | --- | --- |
| `enabled` | `false` | Two teams that each have a player online get a match while no tournament is in progress, also switched by the tile "Auto match mode" in the match menu |
| `start-delay-seconds` | `10` | Seconds between the announcement of an auto match and its start, 3 to 120 |
| `pause-seconds` | `20` | Pause after an auto match and its cleaning before the next one is announced, 0 to 300 |

`registration`:

| Value | Default | Effect |
| --- | --- | --- |
| `team-mode` | `free-choice` | `free-choice` or `random`, also switched by the tile "Team mode" in the registrations menu, see "Choosing a team" |

`arena`:

| Value | Default | Effect |
| --- | --- | --- |
| `clean-blocks-per-tick` | `4096` | Blocks written per tick while the arena is cleaned |
| `baseline-blocks-per-tick` | `16384` | Blocks read per tick while the baseline of the fight area is taken |
| `baseline-depth-below-floor` | `11` | Layers the baseline covers and the cleaning restores, counted from the ground surface of the fight area down, 0 to 64, from the next calculation |
| `clean-particle.enabled` | `true` | Particles show a running cleaning, false cleans without them |
| `clean-particle.type` | `DUST` | Particle type, a colour carrying type such as `DUST` shows the red and the green phase |
| `clean-particle.total-duration-seconds` | `6.0` | Seconds of both phases together, at most 120, 0 skips both |
| `clean-particle.columns-per-tick` | `24` | Particle columns drawn per tick, at most 64, 0 draws none |
| `shape-maximum-cells` | `50000` | Most cells the flood fill may visit when it calculates the fight area |
| `safety-zone-radius` | `64` | Blocks around the arena outline in which players who do not fight take no environmental damage or hunger, 0 turns it off |
| `effect-margin` | `96` | Blocks added around the fight area for the area of effect, 0 to 512, see "Area of effect" |
| `teleport-on-join` | `true` | A player who joins is sent to the tribune once the arena is fully configured |
| `area-inventory.enabled` | `true` | Every player hands in the inventory inside the area of effect, see "Inventory in the area of effect" |
| `shape`, `spawns`, `standings-board` | `{}` | The fight area, the spawns and the place of the standings board, written by the plugin |

`border`:

| Value | Default | Effect |
| --- | --- | --- |
| `particle-period-ticks` | `30` | Ticks between two redraws of the border particles |
| `particle-view-radius` | `12` | Distance around a player in which border blocks are shown |
| `particle-maximum-blocks-per-player` | `24` | Most border blocks shown to one player per redraw |
| `particle-maximum-lines-per-player` | `48` | Most connection lines between border columns shown to one player, 0 hides them |
| `wall-preview-seconds` | `4` | Seconds the outline of a recognised wall is shown after a calculation |
| `confirmed-particle.enabled` | `true` | The marker on every block of a confirmed wall, also switched by the tile "Border particles" in the arena menu; false hides only the marker, the outline shown after a calculation stays |
| `confirmed-particle.type` | `END_ROD` | Particle type of the marker |
| `confirmed-particle.height` | `0.25` | Height in blocks of the particle stream above the block, 0 draws a single particle |
| `confirmed-particle.colour.red`, `.green`, `.blue` | `255` | RGB colour from 0 to 255, used only by colour carrying types such as `DUST` |

`items-adder`: whether ItemsAdder is used, the images of the countdown, the item of the totem and the item above every registration station, every value with its default in "ItemsAdder (optional)".

`placement`, see "Live placement":

| Value | Default | Effect |
| --- | --- | --- |
| `sidebar.enabled` | `true` | Every player inside the area of effect sees the placement as a sidebar |
| `board.enabled` | `true` | The standings board at the tribune is shown, false removes it and keeps its place |

Changes can be applied while the server runs with the "Reload config.yml" item in the main menu of `/tournament`. A changed `team-size` is taken over only under the rules of the team menu, otherwise the old size stays and the log and the online admins read why. At the start of the server the value of `config.yml` is always taken over, also during a restored tournament; a team with more players is then named in the log and to the online admins and keeps its players. The reload leaves the calculated fight area, the spawns, the border blocks, the teams and a running match untouched. A value that cannot be used falls back to its default, the log and the answer of the reload name it.

## Files in the plugin folder

| File | Content |
| --- | --- |
| `config.yml` | The settings, the calculated fight area, the spawns and the place of the standings board |
| `lang/en.yml`, `lang/de.yml` | The texts, copied on the first start and never overwritten, see "Languages" |
| `teams.yml` | The teams with their players and colours, an unreadable entry is skipped and the file kept as `teams.yml.broken` |
| `kits.yml` | The kits and the default kit, an unreadable file is renamed to `kits.yml.broken` |
| `tournament.yml` | The bracket and the results of a running or paused tournament, an unreadable file is renamed to `tournament.yml.broken` |
| `registrations.yml` | The registered players and the registration stations, an unreadable file is renamed to `registrations.yml.broken` |
| `startgates.yml` | The placed start gate blocks |
| `borders.yml` | The marked border blocks |
| `area-inventories/<uuid>.yml` | The inventory, the potion effects and the hunger a player handed in inside the area of effect, removed when they are given back, an unreadable file is renamed to `<uuid>.yml.broken` |
| `player-snapshots/<uuid>.yml` | The inventory and state of a fighter from before the kit, given back after the match, an unreadable file is renamed to `<uuid>.yml.broken` |

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

Every player who enters the area of effect hands in the whole inventory, armour and off hand included, together with the item on the cursor and the items in the 2x2 crafting grid. Before that the plugin closes every open inventory view of the player, so the server puts the items of an open crafting table, anvil, loom or similar view and the item on the cursor back into the inventory; what does not fit because the inventory is full drops at the feet of the player inside the area. Admins and operators are not exempt, in every game mode. The plugin writes the inventory to `plugins/SimplePVPArena/area-inventories/<uuid>.yml` and clears it only after the file is written. The player gets it back on leaving the area, on changing the world, when the arena is reset or when the switch is turned off, and everything the player got inside, from the creative mode, from the plugin or picked up, is removed at that moment. The items of the cursor and of the crafting grid go into free slots, what does not fit is dropped at the feet of the player.

The potion effects and the hunger are handed in as well: the file also holds every active effect with its remaining time and the food level, saturation and exhaustion. After the file is written every effect is removed and the hunger is full. On leaving the effects gained inside are removed, the saved effects come back with the time they had left on entering, an endless effect stays endless, and the saved hunger is set again. A file of an older version without effects and hunger leaves both as they are. Level, experience, health, the game mode and the ender chest stay untouched.

Every player who is no active fighter of the running round, admins included, gets the spectator item ("Spectator mode", a compass) in the last slot of the hotbar right after handing in the inventory. It is removed on leaving like every other item taken inside, and a player who already carries one gets no second one. Inside the area nobody can drop items or open the ender chest, and a death drops nothing. The messages on entering and leaving come into the chat at most once a minute, otherwise into the action bar.

When the file cannot be written the player keeps the inventory, the plugin tries again, and the log and the online admins read a warning at most once a minute. A file that cannot be read is renamed to `<uuid>.yml.broken`, nothing is cleared, the admins read the player and the file name, and the items can be restored from that file by hand. A stop of the server gives every inventory back, the next start takes them again. After a crash the inventory, the effects and the hunger come back at the next join outside of the area. The snapshot taken at the start of a match stays separate: after the match the fighter gets back what was carried inside together with the full hunger of the area, and the own inventory, effects and hunger on leaving the area.

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
