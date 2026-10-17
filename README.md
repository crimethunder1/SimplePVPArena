# SimplePVPArena

A Paper plugin that runs a two versus two single elimination tournament in one arena on the server. Admins register teams, mark the border of the fight area, set the spawns and start the matches from an inventory menu. The plugin handles rounds, the round time limit with sudden death, the start gate, the kit and the cleaning of the arena after every match.

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

Copy the jar into the `plugins` folder of the server and start it. The plugin creates its folder with `config.yml` on the first start. Teams are stored in `teams.yml`, border blocks in `borders.yml` and start gates in `startgates.yml`. Teams can be imported from an `event.json` file in the same folder.

## Commands and permissions

| Command | Permission | Default | Effect |
| --- | --- | --- | --- |
| `/tournament` (alias `/turnier`) | `tournament.admin` | op | Opens the menu that manages teams, the arena, border blocks, matches and the tournament |
| `/tournament spectate` | `tournament.spectate` | everyone | Sends the player back to the tribune of the arena, not possible while fighting |

## Configuration

`config.yml` has four sections, each value is described by a comment in the file:

- `match`: delay between rounds, round time limit, sudden death damage, whether the final runs without sudden death, start gate duration and team colours
- `kit`: armour and items every fighter receives at the start of a round
- `arena`: cleaning speed and particles, flood fill limit, safety zone radius, plus the fight area and spawns the plugin writes itself
- `border`: particle display of border blocks and the marker of a confirmed wall

Changes can be applied while the server runs with the "Reload config.yml" item in the main menu of `/tournament`.
