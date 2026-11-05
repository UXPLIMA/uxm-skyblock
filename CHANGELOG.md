# Changelog

## 0.0.1 (2026-11-05)

The first release under UXPLIMA. The plugin is free and its source is public. It runs on Paper and Folia 26.2
and later; every other plugin it works with is optional.

### Added

- Classic Skyblock islands and eleven game modes: OneBlock, ChunkBlock, AcidIsland, CaveBlock, SkyGrid, Boxed,
  Poseidon, StrangerRealms, Brix, Parkour and TradeWinds. `presets.modes` in `config.conf` picks which a player
  is offered; Boxed and StrangerRealms need Paper.
- Island teams with invites, kicks, bans, trust and roles an owner shapes permission by permission.
- A shared island bank for money and experience, upgrades bought from it, an island shop, a shared vault,
  missions, boosters, seasons and island levels from block values.
- Leaderboards on the shared board engine, read through `%uxmskyblock_top_<board>_<period>_<n>_name%` and the
  other `%uxmskyblock_...%` placeholders.
- Homes, warps, visitors, a guestbook, ratings, alliances, an activity log and player trades.
- Every window is a file under `menus/`, every line a player reads is in `messages/`, in English, Turkish,
  German, Spanish, French, Portuguese and Russian. A player picks a language with `/is lang`; an operator sets
  or clears it for another player.
- SQLite, MySQL, MariaDB and PostgreSQL under `storage`. The SQLite file and logs are in `data/`, and dated copies
  of the config files are kept in `backups/`.
- `/is doctor` lists what the server is missing, such as an economy for the money bank or the world a mode
  needs.

### Fixed

- A player who joins an island can build, use `/is home` and leave the starter quarantine at once, without
  waiting for a restart.
- Console action lines in menus and rewards run on Folia.
- A refused command answers with the plugin's own line in the player's language.

