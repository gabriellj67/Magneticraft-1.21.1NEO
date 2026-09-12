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

## Phase 2 (IN PROGRESS): init/ + minimal content/ slice

Verified this session: every file count in the table below (as it stood
before this phase) checked out exactly against the source tree
(`content` 303, `client` 53, `data` 24, `init` 19, `integration` 17,
`api` 8, total 530), and the Phase 1 file-parity claim for `system/`
(98 = 98) and the capability-hit count (46, close to the noted 47) also
checked out. The cheat sheet and Phase 1 claims in this document are
accurate.

Ported so far in Phase 2 (9 files, all pure-Java or verified 1:1
package renames against the actual `neoforge-21.1.208-sources.jar` in
the local Gradle cache - not guessed from docs):

- `content/material/Metal.java`, `content/material/MaterialForm.java` -
  pure Java, zero risk, copied verbatim.
- `content/block/BaseBlockDefinition.java`, `OreBlockDefinition.java`,
  `DecorativeBlockFamily.java` - pure Java (one uses
  `org.jetbrains.annotations.Nullable`, not a MC/Forge type), copied
  verbatim.
- `content/fluid/FluidDefinition.java` - pure Java, copied verbatim.
- `content/fluid/MagneticraftFluidType.java` - rewritten:
  `net.minecraftforge.fluids.FluidType` -> `net.neoforged.neoforge.fluids.FluidType`,
  `net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions` ->
  `net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions`.
  Verified against the NeoForge sources jar: both classes exist at those
  paths, `FluidType.Properties` builder and
  `IClientFluidTypeExtensions.getStillTexture/getFlowingTexture/getTintColor`
  are unchanged. Pure package rename.
- `init/ModRegistries.java` - rewritten: `DeferredRegister.create(ForgeRegistries.X, ...)`
  -> `DeferredRegister.create(Registries.x, ...)` (vanilla `ResourceKey`s:
  `BLOCK`, `ITEM`, `FLUID`, `CREATIVE_MODE_TAB`, `BLOCK_ENTITY_TYPE`, `MENU`,
  `RECIPE_TYPE`, `RECIPE_SERIALIZER`, `SOUND_EVENT`, `FEATURE`), except
  `FLUID_TYPES` which stays on `NeoForgeRegistries.Keys.FLUID_TYPES`
  (verified directly in the sources jar). **Caveat**: the plain
  `Registries.*` constant names were not individually re-verified against
  decompiled 1.21.1 vanilla source this session (no joined/patched MC jar
  was found in the local Gradle cache - the project has never been built)
  - they're standard, version-stable vanilla registry keys, but confirm on
    the first real `./gradlew compileJava`.
- `init/ModSounds.java` - rewritten: `RegistryObject<T>` -> `DeferredHolder<T, T>`
  (per the cheat sheet's `RegistryObject` -> `DeferredHolder` rename). Only
  init file with zero `content/` dependencies, so it's the only one that's
  actually complete end-to-end right now.

### Why the rest of `init/` can't be ported yet (checked, not assumed)

Read every remaining `init/*.java` this session. All 17 remaining files
import from `content/` packages that don't exist in this project yet:
`ModFeatures` needs `content/worldgen/OilFieldFeature`; `ModRecipeTypes`
needs `content/machine/{crushingtable,singleblock/recipe}`,
`content/multiblock/{recipe,MultiblockDefinition}`,
`content/nuclear/facility`, `content/recipe/TieredShapedRecipe`;
`ModBlocks`/`ModItems`/`ModMachineBlocks`/etc. need the corresponding
`content/block`, `content/item`, `content/machine` concrete types. There
is no shortcut here - `ModRegistries.register()`'s `bootstrap()` calls
into all 17 will not compile until those `content/` subsystems exist,
same as the already-documented Phase 1 gap in `network/*.java`. Do not
stub fake classes to make it compile; port the real ones.

**This session ported `content/machine/framework` in full** (16 of 16
source files, plus one new file, see below) - the module/capability-host
base classes from the "Suggested phase order". Details:

### `content/machine/framework` - the capability rewrite, done

All 16 source files ported (`EnergyBuffer`, `MachineBlockEntity`,
`MachineModelRefreshQueue`, `MachineModule`, `MachineModuleContainer`,
`MachineModuleHost`, `NetworkConnectionHost`, `menu/*` x5, `module/*` x5),
plus one **new** file, `MachineCapabilities.java` (registration glue that
did not exist in the Forge version - not a stub, real code, see below).
Also ported the two pure-Java catalogue enums this package needed to stay
useful (`content/machine/singleblock/SingleBlockMachineDefinition.java` +
`SingleBlockPortProfile.java`, `content/multiblock/MultiblockDefinition.java`),
since `LegacyMachineGuiLayout` and the module port-access helpers reference
them and they turned out to be pure Java / vanilla-only with zero risk.

Everything here was verified against the actual NeoForge
`neoforge-21.1.208-sources.jar` in the local Gradle cache (found this
session at
`~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/21.1.208/...`),
not guessed from docs. Two important discoveries not yet in the cheat
sheet above:

1. **Capability query model, confirmed exact shape.** `Capabilities.ItemHandler.BLOCK`
   / `FluidHandler.BLOCK` / `EnergyStorage.BLOCK` are
   `BlockCapability<T, @Nullable Direction>` tokens (in
   `net.neoforged.neoforge.capabilities`). `RegisterCapabilitiesEvent.registerBlockEntity(capability, blockEntityType, (be, side) -> ...)`
   registers one provider per (capability kind, block-entity type) pair -
   there is no more single generic dispatcher, so `MachineBlockEntity`
   can no longer answer a generic `getCapability(Capability<T>, Direction)`
   the way it did under Forge (that override, and `invalidateCaps()`/
   `reviveCaps()`, are simply gone - confirmed by their absence from every
   current NeoForge BlockEntity doc/example found). Instead
   `MachineBlockEntity` now exposes three typed aggregator methods
   (`exposedItemHandler`/`exposedFluidHandler`/`exposedEnergyStorage`,
   each scanning `modules.values()` for the first module that has a view
   for that side - the same first-match-wins semantics as the old loop),
   and the new `MachineCapabilities.register(RegisterCapabilitiesEvent, BlockEntityType<BE>)`
   helper wires all three for one BE type in one call. Every future
   concrete `MachineBlockEntity` subclass's registry bootstrap should call
   `MachineCapabilities.register(event, MY_BE_TYPE.get())` once its
   `BlockEntityType` exists - do this instead of re-deriving the
   registration boilerplate per block entity.
   `BlockCapabilityCache` (the consumer-side cache) memoizes the *returned
   handler instance* until `Level.invalidateCapabilities(pos)` is called,
   exactly like Forge's `LazyOptional.of(supplier)` did - so each module's
   `view(side)` builds its wrapper views once (in the constructor) and
   returns the same stable instance every call; no per-module
   invalidate/revive bookkeeping is needed anymore, and none was ported.
2. **`HolderLookup.Provider` threading goes far beyond `SavedData`.**
   Not yet documented above: in the same vanilla version range, `BlockEntity`'s
   `saveAdditional`/`load` (`load` is now `protected loadAdditional`,
   `public load(CompoundTag)` is gone), `getUpdateTag`, `handleUpdateTag`,
   and `onDataPacket` all gained a `HolderLookup.Provider registries`
   parameter, and so did `ItemStackHandler.serializeNBT`/`deserializeNBT`,
   `FluidTank.readFromNBT`/`writeToNBT`, and `ItemStack.save`/the
   replacement for `ItemStack.of(CompoundTag)` (now
   `ItemStack.parseOptional(HolderLookup.Provider, CompoundTag)`, returning
   `ItemStack` directly - a clean drop-in). **This affects every
   `BlockEntity` subclass and every module that persists an `ItemStack` or
   `FluidStack` in all of `content/`**, not just `SavedData`. Confirmed via
   NeoForge's own 1.21.3 block-entity docs example (same API era as
   1.21.1) plus the class files actually in the sources jar - not guessed.
   `MachineModule`'s `load`/`save`/`resetPersistentState`/`loadClientData`/
   `saveClientData` all now take a trailing `HolderLookup.Provider registries`
   parameter accordingly; `MachineModuleContainer` and `MachineBlockEntity`
   thread it through.
3. **Regression caught and fixed in already-ported Phase 1 code**:
   `ItemStack.isSameItemSameTags` was renamed to
   `ItemStack.isSameItemSameComponents` in this vanilla version range.
   `system/network/runtime/PhysicalNetworkManager.java` (ported and marked
   "verified vanilla-only" in Phase 1) still had the old name and would not
   have compiled - fixed this session, along with the same call in the new
   `BulkItemStorageModule.java`.

`content/machine/framework/MachineBlockEntity.java` has one known,
expected compile gap: it references
`content/network/module/ElectricalNetworkModule`, which is not ported yet
(part of the 78-file `content/network` subsystem). Same pattern as the
already-documented Phase 1 gaps - do not stub it, port the real class when
`content/network` is tackled.

**Suggested next step:** `content/network/module/ElectricalNetworkModule`
plus enough of `content/network` for it to resolve, or alternatively start
on the `content/machine/singleblock` concrete blocks/block-entities now
that their framework and definition-enum dependencies exist - then come
back to `init/ModBlocks`+`ModItems`+the block/item slice they need.

## What's left (by size, descending)

| Package | Files | Notes |
|---|---|---|
| `content/` | 276 | Blocks, items, machines, multiblocks, menus, block entities. The big one. `content/machine/framework` (the capability-host base classes) is now fully ported - see above. Remaining heavy user of Forge capabilities (`net.minecraftforge.common.capabilities`) is concentrated in the concrete block entities across `content/machine`, `content/multiblock`, `content/network`, `content/nuclear` - each now has a working pattern to follow (`MachineCapabilities.register` + module `view(side)` methods), it's no longer undesigned, just a lot of files. (27 files ported this session on top of the 7 from before: full `content/machine/framework` (17, including 1 new), `content/machine/singleblock` definition x2, `content/multiblock/MultiblockDefinition`.) |
| `client/` | 53 | Renderers, screens, models (`.mcx`/`.gltf` custom model formats via a bundled loader - check `content/nuclear/...` and asset pipeline before touching). |
| `data/` | 24 | Datagen providers (loot, tags, recipes, models, lang). NeoForge datagen API is close to Forge's for 1.21.1 but `HolderLookup.Provider` is now threaded through most providers - check each one. |
| `init/` | 17 | Registry glue (`ModBlocks`, `ModItems`, etc.) - depends on `content/` types, must be ported together with/after them. Uses `RegistryObject` -> `DeferredHolder` (see below) and `ForgeRegistries` -> `BuiltInRegistries`/`NeoForgeRegistries`. (`ModRegistries`, `ModSounds` ported this session.) |
| `integration/` | 17 | JEI, CraftTweaker, Jade, Tinkers' Construct. Optional at runtime (`enable_*_runtime` gradle flags) - lowest priority, do last. Versions for NeoForge 1.21.1 found so far: JEI `19.54.0.429`, Jade `15.10.6+neoforge`; Mantle/TConstruct NeoForge 1.21.1 builds were **not found** on Modrinth as of this writing - re-check before wiring `integration/tconstruct`. |
| `api/` | 1 remaining | `NuclearReactorColumnType` (needs `content/nuclear/fuel/NuclearFuelGrade`). |

Total remaining: ~381 of the original 530 main-source files (276 + 53 + 24
+ 17 + 17 + 1 - api/init/content counts above already reflect this
session's progress), plus all of
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
| **Capabilities**: `net.minecraftforge.common.capabilities.Capability<T>`, `LazyOptional<T>`, per-BE `getCapability(Capability, Direction)` override, `invalidateCaps()`/`reviveCaps()` | **Gone.** New model: `net.neoforged.neoforge.capabilities.*` - `BlockCapability<T, C>` / `ItemCapability<T, C>` / `EntityCapability<T,C>` tokens (e.g. `Capabilities.ItemHandler.BLOCK`, `Capabilities.EnergyStorage.BLOCK`), registered globally via a `RegisterCapabilitiesEvent` listener: `event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MY_BE_TYPE.get(), (be, side) -> be.exposedHandler(side))`. Lookup from calling code becomes `level.getCapability(Capabilities.ItemHandler.BLOCK, pos, state, blockEntity, side)`. **This is the single largest rewrite in `content/`** (47 direct `net.minecraftforge.common.capabilities` hits, likely 100+ affected files once every block entity/module that exposes energy or item handling is counted) - budget real time for it, it's architectural, not mechanical. **Now actually done once, reusably, for the whole `content/machine` subsystem**: see `content/machine/framework/MachineBlockEntity.java` (the three `exposedXxxHandler(side)` aggregator methods) + `MachineCapabilities.register(event, blockEntityType)` (one-line registration per concrete BE type) + each module's `view(side)` method (`ItemInventoryModule`, `FluidTankModule`, `EnergyStorageModule`, `BulkItemStorageModule`) in the "Phase 2" section above for the concrete pattern to copy for every other capability-exposing block entity in `content/`. The provider function can return the *same* stable wrapper instance every call (built once, not per-query) because NeoForge's `BlockCapabilityCache` memoizes the returned instance until `Level.invalidateCapabilities(pos)` is called - exactly like `LazyOptional.of(supplier)` did - so no per-object invalidate/revive bookkeeping is needed in the new model. |
| **Block entity persistence/sync signatures** (vanilla change, not Forge/NeoForge, but hits every `BlockEntity` subclass in `content/`): `saveAdditional(CompoundTag)`, `load(CompoundTag)`, `getUpdateTag()`, `handleUpdateTag(CompoundTag)`, `onDataPacket(Connection, ClientboundBlockEntityDataPacket)` | `saveAdditional(CompoundTag, HolderLookup.Provider)`, `loadAdditional(CompoundTag, HolderLookup.Provider)` (renamed from `load`, now `protected`), `getUpdateTag(HolderLookup.Provider)`, `handleUpdateTag(CompoundTag, HolderLookup.Provider)`, `onDataPacket(Connection, ClientboundBlockEntityDataPacket, HolderLookup.Provider)` | Same `HolderLookup.Provider` thread as `SavedData` below, but far more pervasive - confirmed via NeoForge's 1.21.3 block-entity docs (same API era as 1.21.1). Also cascades into anything a `BlockEntity` or module persists: `ItemStackHandler.serializeNBT(HolderLookup.Provider)`/`deserializeNBT(HolderLookup.Provider, CompoundTag)`, `FluidTank.readFromNBT/writeToNBT(HolderLookup.Provider, CompoundTag)`, `ItemStack.save(HolderLookup.Provider)` (returns `Tag`, no zero-arg overload), and `ItemStack.of(CompoundTag)` is gone - use `ItemStack.parseOptional(HolderLookup.Provider, CompoundTag)` (returns `ItemStack` directly, `EMPTY` on failure - a clean drop-in replacement). `content/machine/framework/MachineModule.java`'s `load`/`save`/`resetPersistentState`/`loadClientData`/`saveClientData` all carry this `registries` parameter now; thread it through any new loader-independent persistence interface the same way. |
| `ItemStack.isSameItemSameTags(ItemStack, ItemStack)` | `ItemStack.isSameItemSameComponents(ItemStack, ItemStack)` | renamed (components rework). **Caught a live regression from this**: `system/network/runtime/PhysicalNetworkManager.java`, ported and marked "verified vanilla-only" in Phase 1, still had the old name - fixed in the Phase 2 session. If you spot another `system/`/`api/`/`network/` file using old item-stack-tag-comparison naming, it's the same bug, not a new one. |
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
