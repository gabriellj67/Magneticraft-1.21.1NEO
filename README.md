# Magneticraft for Minecraft 1.20.1

This repository is a pure Java 17 port of Magneticraft to Minecraft 1.20.1 and
Forge 47.4.20. The Nova/Magneticraft 1.12 codebase is the behavioral authority;
the implementation is being rebuilt around modern Forge registration, data
generation, persistence, networking, and rendering contracts.

The `0.2.0` line is a one-time destructive contract reset. It does not load
worlds, item data, registry IDs or NBT written by Magneticraft 1.12 or any
earlier 1.20.1 build. Start a new world and do not install it over an existing
Magneticraft save. Registry IDs and versioned persistence envelopes are frozen
from `0.2.0`; later releases may only add forward migrations.

## Current status

The repository is the `0.2.0` governance and contract-freeze baseline for the
complete gameplay rebuild. Existing 1.20.1 implementations are candidates:
each later 0.x stage keeps code only after it passes the Nova 1.12 behavior
contract, otherwise it is selectively rebuilt.

JEI, CraftTweaker and Tinkers' Construct support is currently optional.
Magneticraft does not embed their classes or require them in a base
installation. Jade support is not implemented in `0.2.0`; it is planned as an
isolated optional integration for `0.8.0`. Strut Your Stuff was evaluated for
future point-to-point electrical structures, but is not a runtime dependency
because the current release has no matching long-span wire consumer.

See the [authoritative migration matrix](docs/porting/migration-matrix.md),
[machine-readable registry map](docs/porting/registry-id-map.json),
[base-content migration map](docs/porting/base-content.md),
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

The target is new worlds. The complete staged scope, future restoration targets
and intentional exclusions are fixed by the migration matrix. Sloped and
vertical conveyors, unused historical gears, unfinished kiln prototypes,
BuildCraft integration and unrestricted computer TCP/SSL access remain
intentionally excluded.

## License and provenance

Magneticraft is distributed under GPL-2.0-only. See [LICENSE](LICENSE) for the
license text and [PORTING_SOURCES.md](PORTING_SOURCES.md) for the fixed upstream
commits, permitted reuse boundaries, and attribution policy.
