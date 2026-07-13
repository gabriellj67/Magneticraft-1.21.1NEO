# Directory Structure

> Package and resource layout for the Magneticraft Forge 1.20.1 port.

## Status and scope

This repository is a greenfield Java 17 port. At bootstrap time the only stable
source root is `src/main/java/committee/nova/mods/magneticraft/`, represented by
`Magneticraft.java`. The layout below is therefore the accepted project
contract for all migration tasks, not a description of the legacy 1.12 package
tree.

## Source roots

```text
src/main/java/committee/nova/mods/magneticraft/
|-- Magneticraft.java          # composition root only
|-- api/                       # deliberately public Java API
|-- init/                      # DeferredRegister declarations
|-- content/                   # player-visible blocks, items and machines
|   |-- block/
|   |-- item/
|   |-- fluid/
|   |-- machine/<machine>/     # machine-specific BE, menu and behavior
|   `-- multiblock/<name>/
|-- system/                    # reusable physical/network simulations
|   |-- electric/
|   |-- heat/
|   |-- pressure/
|   `-- logistics/
|-- network/                   # packet registration and payload handlers
|-- data/                      # datagen providers and data codecs
|-- integration/<modid>/       # optional integration, isolated by mod id
`-- client/                    # screens, BERs and client-only bootstrap

src/main/resources/            # handwritten metadata and source assets
src/generated/resources/       # Gradle datagen output
src/test/java/                 # pure Java unit and characterization tests
src/gametest/java/             # Forge GameTests when a dedicated source set exists
```

## Module organization

- Organize by gameplay domain first. A machine's block entity, menu and local
  behavior stay under `content.machine.<machine>` rather than being split into
  global `blocks`, `tiles` and `containers` packages.
- Put cross-machine algorithms in `system`. Electricity, heat, pressure and
  logistics remain separate models with small adapter interfaces.
- Put all Forge registry declarations in `init`; registration classes contain
  declarations only and do not implement gameplay behavior.
- Keep `Magneticraft` as the composition root: attach registries, configuration,
  networking and lifecycle listeners, but do not place gameplay logic there.
- Client-only types must live below `client` and be reached through client setup
  events. Common code must not import `net.minecraft.client.*`.
- Optional integrations must be isolated below `integration/<modid>` and loaded
  only after checking that the target mod is present.
- Add code to `api` only when external consumers need a stable contract. Internal
  extension points stay outside `api` until the 1.0 contract is intentionally
  frozen.

## Naming conventions

- Java packages are lowercase; classes and records use `UpperCamelCase`;
  methods, fields and locals use `lowerCamelCase`.
- Registry and data identifiers use `snake_case` and the `magneticraft`
  namespace. Never derive persistent IDs from translated display names.
- Registry holders use plural class names such as `ModBlocks`, `ModItems` and
  `ModBlockEntities`.
- Use modern Minecraft names: `BlockEntity`, `BlockEntityRenderer`, `Menu` and
  `Level`. Do not preserve `TileEntity`, `TESR`, `Container` or `World` in new
  class names merely to resemble 1.12.
- Tests end in `Test`; Forge game tests end in `GameTests`.

## Example

The current composition root is
`src/main/java/committee/nova/mods/magneticraft/Magneticraft.java`. New registry
classes follow this shape:

```java
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Magneticraft.MOD_ID);

    private ModBlocks() {
    }
}
```

The private constructor is required for static holder classes. The actual
registry entry belongs beside related content and must be wired from the
composition root.

## Forbidden layouts

- No Kotlin source roots or mixed Java/Kotlin APIs.
- No `common`, `util` or `helper` dumping ground. Name a package after its
  responsibility or keep a one-use helper beside its consumer.
- No proxy classes copied from Forge 1.12; use lifecycle events and explicit
  client setup.
- No source copied from `hypersmc/magneticraft2-1.20.x`; it is architecture-only
  reference material.
