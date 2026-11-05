# uxmSkyblock

A Skyblock plugin for Paper and Folia 26.2 and later: islands with members, roles, a bank, upgrades, missions,
leaderboards and warps, and eleven game modes beside the classic island: OneBlock, AcidIsland, Boxed, Brix,
CaveBlock, ChunkBlock, Parkour, Poseidon, SkyGrid, StrangerRealms and TradeWinds. Free, under the GPL 3.0.

uxmLib and every library are shaded into the jar, so the plugin needs nothing else on the server. Vault,
PlaceholderAPI, Floodgate and the web maps are used when present.

## Install

Download `uxmSkyblock-<version>.jar` from the latest GitHub release, check it against `SHA256SUMS`, put it in
`plugins/` and give the island world the plugin's generator in `bukkit.yml` before the world is first made:

```yaml
worlds:
  world:
    generator: uxmSkyblock
```

The files it writes are listed in [docs/user/reference.md](docs/user/reference.md).

## Build

```sh
./gradlew build       # format check, static analysis, the shaded jar and its checks
./gradlew runServer   # a local Paper server with the plugin
```

## Documentation

[docs/README.md](docs/README.md)
