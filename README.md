# Magneticraft for Minecraft 1.20.1

This repository is a pure Java 17 port of Magneticraft to Minecraft 1.20.1 and
Forge 47.4.20. The Nova/Magneticraft 1.12 codebase is the behavioral authority;
the implementation is being rebuilt around modern Forge registration, data
generation, persistence, networking, and rendering contracts.

The port targets new worlds. Importing worlds or NBT from older Magneticraft
versions is not supported.

## Current status

The repository is the `0.1.0` first-release candidate covering the registered 1.20.1
catalog: materials and fluids, physical electricity/heat/fluid/logistics
networks, single-block automation, sixteen advanced multiblocks, world
generation, programmable computers and mining robots. Client screens, guide
data, static OBJ assets and bounded dynamic block-entity renderers are included.

JEI, CraftTweaker and Tinkers' Construct support is optional. Magneticraft does
not embed their classes or require them in a base installation. Strut Your Stuff
was evaluated for future point-to-point electrical structures, but is not a
runtime dependency because the current release has no matching long-span wire
consumer.

See the [base-content migration map](docs/porting/base-content.md),
[machine framework notes](docs/porting/machine-framework.md) and
[legacy model inventory](docs/MODEL_CONVERSION.md) for stable IDs and explicit
conversion boundaries. The Chinese
[release-readiness report](docs/porting/release-readiness.md) records the
feasibility conclusion, test matrix, remaining risks and release advice.

## Runtime requirements

- Minecraft 1.20.1
- Forge 47.4.20 or a compatible Forge 47.x build
- 64-bit Java 17 runtime

The base installation has no mandatory content-mod dependency. The exact
optional versions exercised by the release matrix are:

- JEI `15.20.0.133` (client);
- CraftTweaker `14.0.60` (client and dedicated server);
- Tinkers' Construct `3.11.2.166` with Mantle `1.11.97` (client and dedicated
  server).

`mods.toml` declares version ranges for compatibility discovery; only the
fixed versions above are release-tested. Mantle is intentionally not declared
as a direct Magneticraft dependency because Tinkers' Construct owns that
runtime dependency.

## Development requirements

- 64-bit JDK 17
- Git
- Network access for the first Gradle dependency resolution

## Build and verification

```powershell
./gradlew.bat compileJava test --no-daemon
./gradlew.bat runData --no-daemon
./gradlew.bat runGameTestServer --no-daemon
./gradlew.bat build --no-daemon
./gradlew.bat runServer --no-daemon
./gradlew.bat runClient --no-daemon
```

Generated resources are written to `src/generated/resources` and are part of
the main resource source set. The release-readiness report contains the
recorded JUnit, GameTest, datagen, client/server and optional-mod matrix
results; the commands above remain the reproducible local quality gate.

Optional development runtimes are opt-in and may be combined:

```powershell
./gradlew.bat runClient -Penable_jei_runtime=true --no-daemon
./gradlew.bat runServer -Penable_crafttweaker_runtime=true --no-daemon
./gradlew.bat runClient -Penable_tconstruct_runtime=true --no-daemon
./gradlew.bat runClient -Penable_jei_runtime=true -Penable_crafttweaker_runtime=true -Penable_tconstruct_runtime=true --no-daemon
```

The target is new worlds. Legacy Magneticraft world/NBT import and currently
unregistered content such as electric poles, Tesla towers and sloped conveyors
remain outside this release.

## License and provenance

Magneticraft is distributed under GPL-2.0-only. See [LICENSE](LICENSE) for the
license text and [PORTING_SOURCES.md](PORTING_SOURCES.md) for the fixed upstream
commits, permitted reuse boundaries, and attribution policy.
