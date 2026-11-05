# Development

The build comes from `uxm-gradle`: `uxm.paper-plugin` adds the Paper API, Error Prone, NullAway, Spotless,
the shaded jar with every bundled library relocated under `com.uxplima.uxmskyblock.libs`, and `verifyJar`,
which fails when the jar carries what the server owns or lacks the plugin's own classes. `verifyNamespace`
also fails on any class outside the plugin's namespace except the SQLite driver. Versions come from the `uxm`
catalog; `gradle/libs.versions.toml` holds only what it lacks.

## Build and run

```sh
./gradlew build       # everything CI runs
./gradlew runServer   # a local server with the plugin, in bukkit-adapter/run/
```

## Release

release-please keeps a release pull request open on `main`. Before merging it, rewrite its `CHANGELOG.md`
entry for operators and keep the pull request body's first and last lines, which release-please reads.
Merging it tags the release; the release workflow builds the jar and attaches it with a `SHA256SUMS` file to
the GitHub release.
