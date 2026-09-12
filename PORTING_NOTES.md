# Magneticraft: Forge 1.20.1 -> NeoForge 1.21.1 porting notes

Source: `../Magneticraft-forge-1.20.1` (530 main-source Java files, ~77.5k lines).
This directory is the destination project. Status below reflects what is
actually ported and verified-by-reading (NOT compiled yet - no JDK/Gradle
build has been run against this tree).

## Phase 1 (DONE): scaffold + núcleo (system/, api/, network/, config, main class)

- Full Gradle scaffold: `build.gradle`, `settings.gradle`, `gradle.properties`,
  `src/main/resources/META-INF/neoforge.mods.toml`, `pack.mcmeta`, gradle
  wrapper. Uses the NeoForge ModDevGradle plugin (`net.neoforged.moddev`,
  non-legacy flavor) targeting `neo_version=21.1.250` / `mc_version=1.21.1`.
- `Magneticraft.java` main mod class, rewritten for the NeoForge mod
  constructor (`(IEventBus, ModContainer)` instead of
  `FMLJavaModLoadingContext.get()` / `ModLoadingContext.get()`).
- `config/MagneticraftConfig.java`: `ForgeConfigSpec` -> `ModConfigSpec`
  (drop-in rename, builder API identical).
- `network/`: all 5 messages + `ModNetwork` rewritten from Forge's
  `SimpleChannel`/`NetworkRegistry` to NeoForge's `CustomPacketPayload` +
  `RegisterPayloadHandlersEvent`/`PayloadRegistrar` + `IPayloadContext`.
- `api/nuclear/**`: copied (7 of 8 files - `NuclearReactorColumnType` was
  skipped, it depends on `content/nuclear/fuel/NuclearFuelGrade` which isn't
  ported yet; add it back in the content phase).
- `system/**`: **all 98 files copied.** Of these:
  - 42 files are pure Java (zero `net.minecraft`/`net.minecraftforge`
    imports) - copied verbatim, zero risk.
  - ~50 files use only vanilla `net.minecraft.*` types - copied as-is;
    verified via Mojang's *official* mappings (not Yarn - see pitfall below)
    that the specific vanilla APIs they touch (`PreparableReloadListener`,
    `BlockPos`, `ResourceLocation`, `Codec`, etc.) are unchanged between
    1.20.1 and 1.21.1, **except** `SavedData`, which was fixed (see below).
  - 6 files used `net.minecraftforge.*` directly and were rewritten by hand:
    `system/network/electric/profile/ElectricalDataEvents.java`,
    `system/network/logistics/ItemHandlerTransactions.java`,
    `system/network/runtime/PhysicalNetworkEvents.java`,
    `system/nuclear/data/NuclearDataEvents.java`,
    `system/nuclear/radiation/RadiationExposureService.java`,
    `system/nuclear/safety/NuclearTerrainDamage.java`.
  - `system/network/longdistance/LongDistanceElectricitySavedData.java`:
    updated for the new `SavedData` contract - `save`/`load` now take a
    `HolderLookup.Provider`, and registration goes through a
    `SavedData.Factory<T>` instead of the old three-arg
    `computeIfAbsent(Function, Supplier, String)`.

### Known compile gaps in Phase 1 (expected, not bugs)

`network/*.java`, `system/nuclear/radiation/RadiationExposureService.java`
and `system/nuclear/safety/NuclearTerrainDamage.java` reference classes
under `content/`, `init/`, and `client/` that are **not ported yet**
(menus, script runtime, block/item registries, block-entity classes). This
whole tree will NOT compile stand-alone with `./gradlew compileJava` until
the content phase lands. Do not "fix" this by stubbing fake classes - port
the real ones instead.

## What's left (by size, descending)

| Package | Files | Notes |
|---|---|---|
| `content/` | 303 | Blocks, items, machines, multiblocks, menus, block entities. The big one. Heavy user of Forge capabilities (`net.minecraftforge.common.capabilities`, 47 hits) which is a *hard rewrite*, not a rename - see below. |
| `client/` | 53 | Renderers, screens, models (`.mcx`/`.gltf` custom model formats via a bundled loader - check `content/nuclear/...` and asset pipeline before touching). |
| `data/` | 24 | Datagen providers (loot, tags, recipes, models, lang). NeoForge datagen API is close to Forge's for 1.21.1 but `HolderLookup.Provider` is now threaded through most providers - check each one. |
| `init/` | 19 | Registry glue (`ModBlocks`, `ModItems`, etc.) - depends on `content/` types, must be ported together with/after them. Uses `RegistryObject` -> `DeferredHolder` (see below) and `ForgeRegistries` -> `BuiltInRegistries`/`NeoForgeRegistries`. |
| `integration/` | 17 | JEI, CraftTweaker, Jade, Tinkers' Construct. Optional at runtime (`enable_*_runtime` gradle flags) - lowest priority, do last. Versions for NeoForge 1.21.1 found so far: JEI `19.54.0.429`, Jade `15.10.6+neoforge`; Mantle/TConstruct NeoForge 1.21.1 builds were **not found** on Modrinth as of this writing - re-check before wiring `integration/tconstruct`. |
| `api/` | 1 remaining | `NuclearReactorColumnType` (needs `content/nuclear/fuel/NuclearFuelGrade`). |

Total remaining: ~417 of the original 530 main-source files, plus all of
`src/gametest`, `src/test`, `src/integrationTest`, `src/generated`, and
`src/main/resources` (assets/data - 653 files, untouched).

## Forge -> NeoForge API cheat sheet (verified against the actual
NeoForge `1.21.1` branch source, not docs summaries which drift)

| Forge (1.20.1) | NeoForge (1.21.1) | Notes |
|---|---|---|
| `net.minecraftforge.eventbus.api.IEventBus` | `net.neoforged.bus.api.IEventBus` | |
| `net.minecraftforge.fml.common.Mod` | `net.neoforged.fml.common.Mod` | |
| `FMLJavaModLoadingContext.get().getModEventBus()` | mod constructor param `IEventBus modBus` | constructor is `MyMod(IEventBus modBus, ModContainer container)`, both injected by FML |
| `ModLoadingContext.get().registerConfig(...)` | `container.registerConfig(...)` | `ModContainer` from `net.neoforged.fml.ModContainer` |
| `@Mod.EventBusSubscriber(modid=, bus=Bus.FORGE/MOD)` | `@net.neoforged.fml.common.EventBusSubscriber(modid=, bus=EventBusSubscriber.Bus.GAME/MOD)` | top-level annotation now, not nested; `FORGE` bus renamed `GAME` |
| `net.minecraftforge.common.ForgeConfigSpec` | `net.neoforged.neoforge.common.ModConfigSpec` | API-identical |
| `net.minecraftforge.registries.RegistryObject<T>` | `net.neoforged.neoforge.registries.DeferredHolder<T, T2 extends T>` (via `DeferredRegister`) | `DeferredRegister.create(ResourceKey<Registry<T>>, String)` - takes vanilla `ResourceKey`/`Registries.BLOCK` etc, not `IForgeRegistry` |
| `net.minecraftforge.registries.ForgeRegistries.BLOCKS` etc. | `net.minecraft.core.registries.Registries.BLOCK` (as the `ResourceKey` for `DeferredRegister.create`) or `BuiltInRegistries.BLOCK` (direct lookup) | |
| `net.minecraftforge.registries.ForgeRegistries.Keys.FLUID_TYPES` | `net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.FLUID_TYPES` | |
| `net.minecraftforge.fluids.FluidType` | `net.neoforged.neoforge.fluids.FluidType` | |
| `net.minecraftforge.items.IItemHandler` / `IItemHandlerModifiable` / `ItemStackHandler` / `SlotItemHandler` / `ItemHandlerHelper` | same simple names under `net.neoforged.neoforge.items.*` | pure package rename, methods unchanged |
| `net.minecraftforge.energy.IEnergyStorage` | `net.neoforged.neoforge.energy.IEnergyStorage` | pure package rename |
| **Capabilities**: `net.minecraftforge.common.capabilities.Capability<T>`, `LazyOptional<T>`, per-BE `getCapability(Capability, Direction)` override, `invalidateCaps()`/`reviveCaps()` | **Gone.** New model: `net.neoforged.neoforge.capabilities.*` - `BlockCapability<T, C>` / `ItemCapability<T, C>` / `EntityCapability<T,C>` tokens (e.g. `Capabilities.ItemHandler.BLOCK`, `Capabilities.EnergyStorage.BLOCK`), registered globally via a `RegisterCapabilitiesEvent` listener: `event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MY_BE_TYPE.get(), (be, side) -> be.exposedHandler(side))`. Lookup from calling code becomes `level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, side)`. **This is the single largest rewrite in `content/`** (47 direct `net.minecraftforge.common.capabilities` hits, likely 100+ affected files once every block entity/module that exposes energy or item handling is counted) - budget real time for it, it's architectural, not mechanical. |
| `net.minecraftforge.network.simple.SimpleChannel` + `NetworkRegistry.ChannelBuilder` + `NetworkEvent.Context` | `net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent` + `PayloadRegistrar` + records implementing `CustomPacketPayload` with a `StreamCodec` + `net.neoforged.neoforge.network.handling.IPayloadContext` | see `network/*.java` in this project for a full worked example of all 5 message shapes (simple composite codec, and manual `StreamCodec.of` for bespoke validation/DecoderException logic) |
| `net.minecraftforge.network.NetworkHooks` (e.g. `openScreen`) | `player.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf> extraDataWriter)` (vanilla) or NeoForge's `IContainerFactory`-based menu registration | check each menu-opening call site in `content/` |
| `net.minecraftforge.event.AddReloadListenerEvent` | `net.neoforged.neoforge.event.AddReloadListenerEvent` | same name/shape on the `1.21.1` branch (renamed to `AddServerReloadListenersEvent` only in *later* MC versions - do not pre-emptively rename) |
| `net.minecraftforge.event.OnDatapackSyncEvent` (`.getPlayers()`) | `net.neoforged.neoforge.event.OnDatapackSyncEvent` (`.getRelevantPlayers()` -> `Stream<ServerPlayer>`) | method renamed AND return type changed (List -> Stream) |
| `net.minecraftforge.event.server.ServerStoppedEvent` | `net.neoforged.neoforge.event.server.ServerStoppedEvent` | pure package rename |
| `net.minecraftforge.event.level.LevelEvent` | `net.neoforged.neoforge.event.level.LevelEvent` | pure package rename |
| `net.minecraftforge.event.TickEvent.PlayerTickEvent` (+ `.phase`, `.player` field) | `net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre` / `.Post` (separate classes, no phase field; player via inherited `getEntity()`) | also exists: `EntityTickEvent`, `LevelTickEvent`, `ServerTickEvent` in the same `event.tick` package for other old `TickEvent.*` usages you'll hit in `content/` |
| `net.minecraftforge.event.entity.player.PlayerEvent.Clone` | `net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone` | pure package rename, same `getOriginal()`/`getEntity()`/`isWasDeath()` |
| `net.minecraftforge.common.MinecraftForge.EVENT_BUS` | `net.neoforged.neoforge.common.NeoForge.EVENT_BUS` | posting a cancellable event: call `.post(event)` then check `event.isCanceled()` separately (don't rely on `.post()`'s return value) |
| `net.minecraftforge.common.util.FakePlayer` / `FakePlayerFactory` | `net.neoforged.neoforge.common.util.FakePlayer` / `FakePlayerFactory` | pure package rename |
| `net.minecraft.world.level.saveddata.SavedData#save(CompoundTag)` | `#save(CompoundTag, HolderLookup.Provider)` | vanilla change (not Forge/NeoForge), affects **every** `SavedData` subclass in `content/` too. Registration: `new SavedData.Factory<>(ctor, (tag, provider) -> load(tag, provider), null /* or a DataFixTypes */)` then `storage.computeIfAbsent(factory, key)` (old 3-arg overload is gone). |
| `new ResourceLocation(ns, path)` | `ResourceLocation.fromNamespaceAndPath(ns, path)` (constructor removed) | not hit in `system/`/`api/`/`network/` (already used the static factory throughout) but expect it in `content/`/`data/` |

### Pitfall worth remembering for later phases

**Yarn mappings are not Mojang's official mappings** - they're an
independent community naming project and can disagree with the official
Mojang names Forge/NeoForge/`loom.officialMojangMappings()` actually use,
even for stable-sounding classes (e.g. Yarn calls `ResourceLocation`
`Identifier`). When you need to check "does vanilla method X still exist in
1.21.1" for the *actual* symbol this codebase uses, pull Mojang's official
`client.txt` mappings from the version manifest
(`https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` ->
version entry -> `downloads.client_mappings`), not Yarn. Both the 1.20.1 and
1.21.1 official mappings were downloaded to a scratch temp dir during this
session for diffing; re-download if needed, they're not stored in this repo.

## Suggested phase order for the rest of the port

1. `init/` + minimal slice of `content/block` + `content/item` needed to
   make `init/ModBlocks`/`ModItems` compile - re-establishes the
   registration skeleton content plugs into.
2. `content/machine/framework` (the module/capability host base classes) -
   this is where the capability rewrite has to happen first, once, so every
   concrete machine built on top of it inherits the new pattern.
3. Remaining `content/` by subsystem (`machine`, `multiblock`, `nuclear`,
   `computer`, `network`, `world`/`worldgen`) - each is fairly self
   contained.
4. `client/` (needs `content/` block/item/BE types to exist first).
5. `data/` (datagen, needs everything above).
6. `integration/` (optional, last).
7. Re-verify `src/main/resources` assets/data (653 files) still line up
   with the ported registry names, then attempt a real `./gradlew build`.
