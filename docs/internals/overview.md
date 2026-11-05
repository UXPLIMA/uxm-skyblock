# Overview

uxmSkyblock is split into five Gradle modules under `com.uxplima.uxmskyblock`:

- `api`: the types other plugins compile against: island views, events and service interfaces.
- `core`: `domain` holds values and rules, `application` holds the use cases and the ports they need. Nothing
  here imports Paper.
- `persistence-adapter`: the ports implemented on uxmLib storage (SQLite, MariaDB, MySQL, PostgreSQL), with
  numbered schema migrations per area.
- `rest-adapter`: the optional HTTP API on Javalin, off by default (`config.conf` `rest`).
- `bukkit-adapter`: commands, listeners, menus, the world generator, the game modes and integrations, and
  `bootstrap`, the composition root, where every object is built and receives its collaborators through its
  constructor. The wiring is split by area (`ConfigurationWiring`, `PersistenceWiring`, `IntegrationWiring`,
  `GameplayWiring` and one wiring per game mode).

## Islands and the grid

Islands stand on a square spiral in one world, `grid.spacing` blocks apart, so on Folia each island sits in
regions of its own. A game mode decides what an island is made of and how it grows; the classic island,
OneBlock, AcidIsland, Boxed, Brix, CaveBlock, ChunkBlock, Parkour, Poseidon, SkyGrid, StrangerRealms and
TradeWinds each have a module file and can be turned off.

## Threads

`SchedulerPort` (`FoliaSchedulerAdapter`) runs work on the global region, a region, an entity or the async
lane. Storage and backups run async; whatever touches a block or a player runs on its owner's thread.
Configuration is read into immutable snapshots and swapped whole on reload.

## Several servers

With `server-node.clustered = true`, nodes share one database and one Redis. A node holds an island through a
lease it renews (`authority-lease`); a visitor is routed to the node that holds the island, or to one a
placement strategy picks when that node stopped.

## Traps

- The island world needs the plugin's generator before it is first made; Folia makes no world while it runs,
  so on Folia the island world is the default world.
- Folia reports no server-wide tick time, so `mspt-aware` placement cannot tell Folia nodes apart.
- The SQLite driver is never relocated: it finds its native library by its own package name.
