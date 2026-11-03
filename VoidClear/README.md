# VoidClear 1.0.2

Target: Paper 1.21.11 / Java 21.

## Build
`mvn -B clean package`

The build uses the official Paper API dependency:
`io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT` (provided scope).

## Install
Copy `target/VoidClear-1.0.2.jar` into the server's `plugins` directory.
The plugin creates `plugins/VoidClear/config.yml` on first startup.

## Commands
- `/voidclear help`
- `/voidclear clear items|mobs|all [force]`
- `/voidclear status`
- `/voidclear scan`
- `/voidclear pause`
- `/voidclear resume`
- `/voidclear reload`

Permission: `voidclear.admin` (default OP).

## Safety defaults
- Ordinary dropped items are only cleared after 180 seconds.
- Player death drops are marked for 30 minutes and protected from natural despawn.
- Hostile mob clusters are considered only above 15 mobs in a 12-block bounding area.
- At most 40% are removed, and at least 10 mobs remain.
- A cluster is skipped if a player is within 28 blocks of any mob in it.
- Named, persistent/non-despawning, targeted, recently damaged, and important boss mobs are protected.
- Cleanup uses Entity#remove(), so it does not award loot, XP, or kill credit.
- `force` only bypasses minimum age; it does not bypass death-drop protection, whitelist, player protection, or protected mob checks.

No ads, telemetry, or premium-version notices.
