ForgeMultipart
==============

An API for dynamically handling different functional parts in the one block space, for Minecraft 1.7.10 / Forge.
This is the GT New Horizons fork.

## Using the API

Depend on the dev artifact, for example `com.github.GTNewHorizons:ForgeMultipart:<version>:dev`, alongside GTNH
CodeChickenCore. Start at the [Java API index](docs/API.md): it lists the supported entry points with a guide and a
compiling Java example for each. You do not need Scala in your own mod to use them.

If you are rebuilding an existing integration against the Java port, read the [release notes](docs/RELEASE_NOTES.md)
first. Already compiled consumer jars keep working.

## Building

The codebase is being ported from Scala to Java. Most sources are Java now, but they still live under
`src/main/scala` so they can be joint-compiled with the Scala files that remain. Everything compiles for Java 8.

```
./gradlew build
```

Use `gradlew.bat` on Windows. The [maintainer guide](docs/migration/MAINTAINERS.md) covers the build boundaries,
the Forge functional tests and the full verification sequence. Port status and remaining work are in the
[migration status](docs/migration/README.md).
