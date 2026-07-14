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

The repository is the `1.0.0` complete gameplay migration. Every published
Nova 1.12 gameplay entry is assigned exactly once in the migration matrix, and
every retained or rebuilt entry has executable evidence. Historical MCX/glTF
assets are converted offline to baked OBJ models; bounded renderers restore
moving parts without shipping the legacy runtime loader. The frozen registry
and persistence contracts remain unchanged from `0.2.0`.

JEI, CraftTweaker, Tinkers' Construct and Jade support is optional.
Magneticraft does not embed their classes or require them in a base
installation. Jade `11.13.1+forge` is isolated behind its plugin boundary and
receives only server-authoritative observation snapshots. Long-distance
electricity uses Magneticraft-owned endpoints and persisted connection graphs;
Strut Your Stuff is not a runtime dependency.

See the [authoritative migration matrix](docs/porting/migration-matrix.md),
[machine-readable registry map](docs/porting/registry-id-map.json),
[base-content migration map](docs/porting/base-content.md),
[machine framework notes](docs/porting/machine-framework.md) and
[legacy model contract](docs/MODEL_CONVERSION.md) for stable IDs and explicit
conversion boundaries. The Chinese
[1.0.0 visual and release report](docs/porting/1.0.0-visual-release.md) and
[release notes](docs/porting/1.0.0-release-notes.md) record the final scope and
verification evidence. The earlier
[release-readiness report](docs/porting/release-readiness.md) is retained only
as a historical feasibility snapshot.

## Runtime requirements

- Minecraft 1.20.1
- Forge 47.4.20 or a compatible Forge 47.x build
- 64-bit Java 17 runtime

The base installation has no mandatory content-mod dependency. The exact
optional versions exercised by the release matrix are:

- JEI `15.20.0.133` (client);
- CraftTweaker `14.0.60` (client and dedicated server);
- Tinkers' Construct `3.11.2.166` with Mantle `1.11.97` (client and dedicated
  server);
- Jade `11.13.1+forge` (client and dedicated server).

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
py -3.14 scripts/convert_legacy_models.py --check
./gradlew.bat compileJava test --no-daemon
./gradlew.bat runData --no-daemon
./gradlew.bat runGameTestServer --no-daemon
./gradlew.bat build --no-daemon
./gradlew.bat runServer --no-daemon
./gradlew.bat runClient --no-daemon
```

Generated data resources are written to `src/generated/resources`; converted
historical visual resources are owned by the manifest under
`src/main/resources/assets/magneticraft`. Both are part of the main resource
source set. The 1.0.0 report records JUnit, GameTest, deterministic conversion,
datagen, client/server and optional-mod matrix results.

Optional development runtimes are opt-in and may be combined:

```powershell
./gradlew.bat runClient -Penable_jei_runtime=true --no-daemon
./gradlew.bat runServer -Penable_crafttweaker_runtime=true --no-daemon
./gradlew.bat runClient -Penable_tconstruct_runtime=true --no-daemon
./gradlew.bat runClient -Penable_jade_runtime=true --no-daemon
./gradlew.bat runClient -Penable_jei_runtime=true -Penable_crafttweaker_runtime=true -Penable_tconstruct_runtime=true -Penable_jade_runtime=true --no-daemon
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
