# uxmSkyblock

A Skyblock plugin for Paper and Folia 26.2 and later: islands, members and roles, bank, upgrades, missions,
leaderboards, warps and eleven game modes. Modules: `api`, `core`, `persistence-adapter`, `rest-adapter`,
`bukkit-adapter`. The Paper plugin is under `com.uxplima.uxmskyblock`, built on uxmLib and shaded into one jar.
The repository is public and the plugin is free.

## Commands

```sh
./gradlew build           # format check, Error Prone, NullAway, tests, verifyJar, verifyNamespace
./gradlew spotlessApply   # format
./gradlew runServer       # local Paper server on the catalog's Minecraft version
```

## Ways to hurt yourself

- **Killing by pattern.** Never kill `java` by name; stop `runServer` with its console or a PID you started.
- **Shading what the server owns.** Adventure, Brigadier, slf4j and netty come from Paper. `verifyJar` and
  `verifyNamespace` fail on a library at its own name; relocate it in `bukkit-adapter/build.gradle.kts`.
- **Blocking a server thread.** File and database IO goes through `SchedulerPort`'s async lane; island and
  player work runs on its region or entity thread. Nothing uses `BukkitScheduler`, so Folia keeps working.
- **Editing a shipped migration.** Schema migrations in `persistence-adapter` are numbered and run once per
  database. A change is a new migration, never an edit of one a server already ran.
- **Breaking an operator's files.** A renamed or removed config key, command name or permission is a breaking
  change: `!` in the title and a `BREAKING CHANGE:` footer, with a migration of the old key on first start.

## Invariants

- Dependencies point inward: `bukkit-adapter` and `persistence-adapter` call `core`; `core` has `domain` and
  `application` with its ports, and imports no Bukkit or Paper type. `api` is what other plugins compile against.
- Every package is `@NullMarked`.
- No text a player reads is written in Java: it is a message key with a line in every language file.
- `docs/internals/uxm-conventions.md`, `.editorconfig` and `.gitattributes` are synced from `UXPLIMA/.github`.

## Testing

New tests follow the repository standard: behavior through the public API, fakes for ports, storage on every
backend with `@EveryBackend`, no assertion on structure. In-game behavior is checked with Craftwire on a
development server, on Paper and on Folia.

## Pull requests

Titles are conventional commits with a scope: `api`, `core`, `island`, `world`, `command`, `config`, `menu`,
`message`, `storage`, `integration`, `rest`, `build`, `ci`, `docs`, `release`. Before merging a release pull
request, rewrite its `CHANGELOG.md` entry for operators: Added, Changed, Fixed, Removed.
