# Magneticraft for Minecraft 1.20.1

This repository is a pure Java 17 port of Magneticraft to Minecraft 1.20.1 and
Forge 47.4.20. The Nova/Magneticraft 1.12 codebase is the behavioral authority;
the implementation is being rebuilt around modern Forge registration, data
generation, persistence, networking, and rendering contracts.

The port targets new worlds. Importing worlds or NBT from older Magneticraft
versions is not supported.

## Current status

The repository currently contains the engineering baseline. Gameplay content
is added in dependency order and each large migration stage is verified before
its Chinese commit.

## Requirements

- 64-bit JDK 17
- Git
- Network access for the first Gradle dependency resolution

## Build and verification

```powershell
./gradlew.bat compileJava test --no-daemon
./gradlew.bat runData --no-daemon
./gradlew.bat build --no-daemon
./gradlew.bat runServer --no-daemon
./gradlew.bat runClient --no-daemon
```

Generated resources are written to `src/generated/resources` and are part of
the main resource source set.

## License and provenance

Magneticraft is distributed under GPL-2.0-only. See [LICENSE](LICENSE) for the
license text and [PORTING_SOURCES.md](PORTING_SOURCES.md) for the fixed upstream
commits, permitted reuse boundaries, and attribution policy.
