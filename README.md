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

The team size sets how many players a new team has at most, 2 by default (`match.team-size` in `config.yml`, 1 to 8). The tiles "Smaller teams" and "Larger teams" of the team menu change it by one and write it into `config.yml`, the comments of the file stay. The size cannot change while a tournament is running or paused, or while a team has more players than the new size; such a team has to be removed first. Teams with fewer players stay as they are and take part, the start of a tournament names them to the admins, a missing player counts as absent. `teams.yml` keeps every team of 1 to 8 players whatever the size is.

## Commands and permissions

| Command | Permission | Default | Effect |
| --- | --- | --- | --- |
| `/tournament` (alias `/turnier`) | `tournament.admin` | op | Opens the menu that manages teams, the arena, border blocks, matches and the tournament |
| `/tournament spectate` | `tournament.spectate` | everyone | Sends the player back to the tribune of the arena, not possible while fighting |

## Configuration

`config.yml` has three sections, each value is described by a comment in the file:

- `match`: delay between rounds, round time limit, sudden death damage, whether the final runs without sudden death, start gate duration, the team size (`team-size`, 2 players) and team colours
- `arena`: cleaning speed and particles, flood fill limit, safety zone radius, the margin of the area of effect (`effect-margin`, 96 blocks), whether joining players are sent to the tribune (`teleport-on-join`, true), plus the fight area and spawns the plugin writes itself
- `border`: particle display of border blocks and the marker of a confirmed wall

Changes can be applied while the server runs with the "Reload config.yml" item in the main menu of `/tournament`. A changed `team-size` is taken over only under the rules of the team menu, otherwise the old size stays and the log and the online admins read why.

The kits live in `kits.yml` and are created from the inventory of an admin, replaced, deleted and chosen as the default kit in the kit menu of `/tournament`. Every fighter receives the default kit at the start of a round. A `kit` section that an older version saved in `config.yml` is taken over into `kits.yml` once as the kit `default`.

## Area of effect

The protection of the world and the pvp lock only apply around the arena. The area of effect is a circle around the centre of the frame of the fight area, in the world of the arena and at every height. Its radius is the distance from that centre to the farthest corner of the frame plus `arena.effect-margin` from `config.yml`. The info tile of the arena menu shows the centre and the radius. Without a calculated fight area there is no area of effect.

Inside the area of effect, players who are no admins cannot build or break outside the fight area, explosions, fire, pistons and entities do not change blocks outside the fight area or under its floor, and a player standing in it is only hurt by another player when both fight the running round or when an admin hits an active fighter. Outside of it the plugin leaves building, explosions, fire, pistons and player against player alone. The rules of the fight area itself, the safety zone with its own radius and the item, mob and damage rules of fighters and spectators do not depend on it.

The tribune spawn has to lie inside the area of effect once the fight area is calculated. A calculation that leaves the stored tribune outside of the new area stays valid and warns the admin. With `arena.teleport-on-join: false` joining players stay where they are; players of the running match are still sent to the tribune.
