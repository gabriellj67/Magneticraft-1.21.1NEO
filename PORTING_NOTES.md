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

## Phase 3 (IN PROGRESS): `content/network/module/` - full package, done

This session ported **all 26 files** of `content/network/module/` (the
`ElectricalNetworkModule`-and-friends package that was the Phase 2 "known
compile gap" for `MachineBlockEntity`), plus 2 small pure-Java enums from
`content/network/electric/` that `ElectricalControlModule` and
`ElectricalProtectionModule` needed (`ElectricalControlKind`,
`ElectricalProtectionKind`). File-for-file parity with the source directory
was verified (`diff` of the two directory listings is empty, 26 = 26).

Breakdown of what was ported, by risk tier:

- **17 files, zero rewrite** (pure Java or vanilla/system-only, copied
  verbatim except where noted below): `LongDistanceWireHost`,
  `TransformerElectricalHost`, `TieredElectricalPlacementHost`,
  `TieredElectricalHost`, `TieredElectricalSideHost`, `WirelessReceiverModule`,
  `ElectricalProfileGateModule`, `ConveyorOccupancy`, `ConveyorRoute`,
  `ElectricalVoltageSourceModule`, `TeslaTowerModule`, `TransformerCouplerModule`,
  `LongDistanceEndpointModule`, `ElectricalEnergyBridgeModule`, `HeatNetworkModule`,
  `KineticNetworkModule`, `ElectricalControlModule`.
- **3 files, `ItemStack`/NBT-only rewrite** (no capability code, but needed
  the `HolderLookup.Provider` threading described below):
  `ElectricalProtectionModule`, `ElectricalPowerModule` (also dropped its
  Forge-energy `Capability`/`LazyOptional` plumbing), `LongDistanceEndpointModule`.
- **6 files, full capability-model rewrite** (the "gone" row in the cheat
  sheet - `Capability<T>`/`LazyOptional<T>`/`getCapability`/
  `invalidateCapabilities`/`reviveCapabilities` all removed): `AbstractPhysicalNetworkModule`,
  `ElectricalNetworkModule`, `PressureNetworkModule` + `PressureFluidHandler`,
  `FluidPipeModule`, `LogisticsTubeModule`, `ConveyorBeltModule`. See pattern
  below.

### New pattern: capability views on a `MachineModule` that is *not* a `MachineBlockEntity`

`content/machine/framework`'s Phase 2 capability pattern
(`MachineCapabilities.register` + `RegisterCapabilitiesEvent.registerBlockEntity`)
only applies to `MachineBlockEntity` subclasses. The classes ported this
session are different: `PressureNetworkModule`, `FluidPipeModule`,
`LogisticsTubeModule`, `ConveyorBeltModule` and `ElectricalPowerModule` are
**modules owned by some other block entity** (pipes, conveyors, tubes),
not block entities themselves. For these, the old Forge
`getCapability(Capability, Direction)` override is replaced with a plain
`@Nullable` **`view(Direction side)`** method (matching the
`EnergyStorageModule.view`/`FluidTankModule.view` pattern from Phase 2)
that returns a stable, pre-built handler instance or `null`:

- `ElectricalPowerModule.view(Direction)` -> `IEnergyStorage`
- `PressureNetworkModule.view(Direction)` -> `IFluidHandler`
- `FluidPipeModule.view(Direction)` -> `IFluidHandler`
- `LogisticsTubeModule.view(Direction)` -> `IItemHandler`
- `ConveyorBeltModule.view(Direction)` -> `IItemHandler`

**This means every future concrete block entity that owns one of these
modules** (e.g. `PressureTankBlockEntity`, `IronPipeBlockEntity`,
`PneumaticTubeBlockEntity`, `ConveyorBeltBlockEntity`, still unported) **must
itself register a `RegisterCapabilitiesEvent.registerBlockEntity(...)`
provider that delegates to the module's `view(side)`** - the same way
`MachineCapabilities.register` does for `MachineBlockEntity`. No such glue
exists yet for this non-`MachineBlockEntity` family; add it (a
`NetworkCapabilities` helper analogous to `MachineCapabilities`, or inline
per block entity) when those block entities are ported.

### New pattern: querying a *neighbor's* capability from network code

Several modules look up an adjacent block's handler (pipe pulling/pushing,
logistics tube inserting into a chest, conveyor belt output). Verified
directly against `neoforge-21.1.208-sources.jar`
(`net/neoforged/neoforge/capabilities/BlockCapability.java`) - there are two
call shapes, both confirmed to exist:

- `level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side)` - 3-arg
  convenience method on `Level` (looks up the block entity itself).
- `Capabilities.ItemHandler.BLOCK.getCapability(level, pos, state, blockEntity, side)`
  - 5-arg instance method on the `BlockCapability` token itself; use this
    one when the caller already has the `BlockEntity` in hand (e.g. from a
    manual chunk-based lookup that intentionally avoids loading unloaded
    chunks, as `FluidPipeModule`/`LogisticsTubeModule`/`ConveyorBeltModule`
    all do) so the capability system doesn't re-look-up the block entity.

Same shape for `Capabilities.FluidHandler.BLOCK` and
`Capabilities.EnergyStorage.BLOCK`. All three tokens live in
`net.neoforged.neoforge.capabilities.Capabilities`.

### Retrofit: `AbstractPhysicalNetworkModule.loadNetworkData`/`saveNetworkData` now carry `registries`

Originally ported (earlier in this session) with the same no-arg shape as
the Forge 1.20.1 source. While porting `LogisticsTubeModule` and
`ConveyorBeltModule` (both persist `ItemStack`s inside their network
payload, e.g. `TravelingItem`/`Parcel`), it became clear the protected
`loadNetworkData(CompoundTag)`/`saveNetworkData(CompoundTag)` template
methods needed a `HolderLookup.Provider registries` parameter threaded down
from the `final load`/`save` methods, the same way `MachineModule` itself
carries it (see Phase 2's `MachineModule.load`/`save`). **This was retrofitted
across all 5 already-ported subclasses in the same session**
(`ElectricalNetworkModule`, `HeatNetworkModule`, `KineticNetworkModule`,
`PressureNetworkModule`, `FluidPipeModule`) - if you find a
`loadNetworkData(CompoundTag tag)` (single-arg) anywhere under
`content/network/`, it is stale and was missed; it should take
`(CompoundTag tag, HolderLookup.Provider registries)`.

Confirmed elsewhere in the same jar: `Tags.Fluids.GASEOUS` exists at
`net.neoforged.neoforge.common.Tags.Fluids.GASEOUS` (`net/neoforged/neoforge/common/Tags.java:935`)
- same tag name as the removed Forge `net.minecraftforge.common.Tags.Fluids.GASEOUS`,
pure package rename, used by `PressureFluidHandler`/`FluidPipeModule`.

### Known compile gaps left by this phase (expected, not bugs - do not stub)

Three files reference `content/` types outside `content/network/module/`
that are not ported yet. Same policy as every other documented gap in this
file: port the real class when its subsystem is tackled, do not stub it.

- `ElectricalProtectionModule` references `init.ModNetworkItems.FUSE` (for
  fuse-item validation). `ModNetworkItems` itself is blocked on 4 unported
  classes: `content/network/WrenchItem`, `content/item/CopperWireCoilItem`,
  `content/item/ElectricalFuseItem`, `content/item/ElectricalRepairToolItem`.
- `LogisticsTubeModule` references `content.network.pneumatic.PneumaticTubeBlockEntity`
  (3-file subpackage, not ported: `PneumaticConnectionHost`, `PneumaticTubeBlock`,
  `PneumaticTubeBlockEntity`).
- `ConveyorBeltModule` references `content.network.logistics.ConveyorBeltBlockEntity`
  (2-file subpackage, not ported: `ConveyorBeltBlock`, `ConveyorBeltBlockEntity`).

## `content/network/` - CLOSED, 78/78 files (Phases 1/3/5/6/7/8/9)

Every subpackage of `content/network/` is now fully ported. File-for-file
parity verified for the whole tree (`diff` of the full recursive listing
between source and destination is empty, 78 = 78).

| Subpackage | Files | Notes |
|---|---|---|
| `electric/` | **0 (28/28 done, Phase 9)** | Cables, poles, transformers, Tesla towers, wireless receivers, the electrical control/protection blocks, `ElectricalDeviceMenu`. See Phase 9 above. |
| `WrenchItem.java` | **0 (1/1 done, Phase 9)** | Trivial `Item` subclass. See Phase 9 above. |
| `kinetic/` | **0 (4/4 done, Phase 8)** | `HandCrankBlock(Entity)`, `WoodenShaftBlock(Entity)`. Ported verbatim, zero rewrite - see Phase 8 above. |
| `pneumatic/` | **0 (3/3 done, Phase 8)** | `PneumaticConnectionHost`, `PneumaticTubeBlock(Entity)`. See Phase 8 above (the `instanceof Level` capability-lookup pattern). |
| `fluid/` | **0 (2/2 done, Phase 8)** | `IronPipeBlock(Entity)`. See Phase 8 above. |
| `logistics/` | **0 (2/2 done, Phase 8)** | `ConveyorBeltBlock(Entity)`. Ported verbatim, zero rewrite - see Phase 8 above. |
| `pressure/` | **0 (5/5 done, Phase 7)** | `BrassPressurePipeBlock(Entity)`, `PressureTankBlock(Entity)`, `PressureTankMenu`. See Phase 7 above (menu-opening + network-buffer-type rewrites). |
| `heat/` | **0 (4/4 done, Phase 6)** | `HeatPipeBlock(Entity)`, `HeatSinkBlock(Entity)`. Ported verbatim, zero rewrite - see Phase 6 above. |
| `block/` | **0 (3/3 done, Phase 5)** | `ConduitBlock`, `NetworkComponentBlock(Entity)` - the shared multi-domain conduit block. Ported verbatim, zero rewrite - see Phase 5 above. |
| `module/` | **0 (26/26 done)** | Every `MachineModule`/`AbstractPhysicalNetworkModule` in this package is ported. |

`content/network/` needs nothing further from `content/` itself. Its only
remaining compile gaps all point outward: `content/item/` (4 classes:
`TieredElectricalDrops`, `TransformerElectricalDrops`,
`TieredElectricalItemName`, plus the fuse/wrench/repair-tool items) and the
17 still-blocked `init/*.java` files.

## Phase 4 (DONE): `content/multiblock/` - full package, 29/29 files

This session ported **all 29 files** of `content/multiblock/` (28 in the
package root + `recipe/`'s 2 files), the "sixteen advanced controllers"
subsystem (solar panels/towers/mirrors, steam engine/turbine/boiler,
grinder/sieve/hydraulic press, mechanical grinding mill, big electric
furnace, big combustion chamber, oil heater, polymerizer, refinery,
pumpjack, shelving unit). File-for-file parity verified (`diff` of the two
directory listings is empty, 29 = 29). This was **not** the originally
planned next step (the notes above suggested `content/network/electric`,
`pressure`, or `heat`) - it turned out to be a hard prerequisite instead:
`content/network/block/ConduitBlock.connectsToMachine` calls
`MultiblockExternalPortService.supports(...)`, which needs
`AdvancedMultiblockBlockEntity` + `MultiblockPortLayout` + the rest of this
package to exist. Porting `content/multiblock` first, in full, was the
correct call - it's now a real, closed subsystem with zero stubs, and it
directly unblocks `content/network/block` (3 files) as the very next step.

Breakdown by risk tier:

- **17 files, zero rewrite** (pure Java or vanilla-only, copied verbatim):
  `StructureOffset`, `MultiblockCell`, `MultiblockValidationResult`,
  `MultiblockTransform`, `MultiblockBounds`, `HydraulicPressMode` (base enum;
  see codec addition below), `MultiblockMembershipService`,
  `MultiblockStructureFiller`, `MultiblockMatcher`, `MultiblockRule`,
  `MultiblockGapBlock`, `PumpjackCursor`, `PumpjackState`,
  `MultiblockPortLayout`, `LegacyMultiblockCollision` (587 lines, pure
  vanilla AABB/VoxelShape data catalogue - copied verbatim despite its size),
  `MultiblockExternalPortService` (deferred until `AdvancedMultiblockBlockEntity`
  existed, then copied verbatim - zero Forge imports).
- **3 files, `SavedData`/`BlockEntity` persistence-signature rewrite only**
  (no capability code): `MultiblockStructureSnapshot`,
  `MultiblockMembershipSavedData` (same `SavedData.Factory` pattern as
  Phase 1's `LongDistanceElectricitySavedData`), `MultiblockPortProfile`
  (`net.minecraftforge.fluids.FluidStack` -> `net.neoforged.neoforge.fluids.FluidStack`,
  `ForgeRegistries.FLUIDS` -> `BuiltInRegistries.FLUID`, both pure renames).
- **1 file, event-bus rewrite**: `MultiblockEvents`
  (`BlockEvent`/`PlayerInteractEvent` -> `net.neoforged.neoforge.event.*`,
  `EventPriority`/`SubscribeEvent` -> `net.neoforged.bus.api.*`,
  `@Mod.EventBusSubscriber` -> `@EventBusSubscriber(bus = Bus.GAME)` - the
  same pattern as Phase 1/3's other `*Events.java` files).
- **1 file, menu-factory rewrite**: `AdvancedMultiblockMenu`
  (`net.minecraftforge.items.*` -> `net.neoforged.neoforge.items.*`;
  `FriendlyByteBuf` -> `RegistryFriendlyByteBuf` for the client-side
  constructor, since NeoForge's `IContainerFactory<T>.create(int, Inventory,
  RegistryFriendlyByteBuf)` is what actually backs `player.openMenu(...)` -
  confirmed by reading `net/neoforged/neoforge/network/IContainerFactory.java`
  in the sources jar).
- **1 file, one-line block-opening rewrite**: `AdvancedMultiblockBlock`
  (`NetworkHooks.openScreen(player, menuProvider, writer)` ->
  `player.openMenu(menuProvider, writer)`, writer lambda now typed
  `Consumer<RegistryFriendlyByteBuf>`; everything else in the file was
  already vanilla-only).
- **4 files, the full capability + persistence rewrite** (the hard core of
  this phase): `MultiblockGapBlockEntity`, `AdvancedMultiblockBlockEntity`,
  `AdvancedMultiblockLogic`, `ShelvingStorageModule`. See the two new
  patterns below.
- **2 files, the vanilla recipe rewrite** (brand new territory for this
  port - see the new cheat-sheet entry below): `recipe/AdvancedProcessingRecipe`,
  `recipe/PolymerizerRecipe`.

### New pattern: position-dependent port capabilities on a multi-position structure

`AdvancedMultiblockBlockEntity` is the first block entity in this port whose
capability exposure depends on *which world position* is being queried, not
just which side - a formed structure's controller block and every one of its
(possibly dozens of) member `MultiblockGapBlockEntity` positions can each
expose a different item/fluid port, all defined by `MultiblockPortLayout`.
The Phase 2 `MachineCapabilities.register` helper doesn't fit this (it
assumes one `(be, side) -> ...` provider is enough because the position is
implicitly "this BE's own block"). The pattern used instead:

- `AdvancedMultiblockBlockEntity.itemHandler(BlockPos position, @Nullable Direction side)` /
  `.fluidHandler(BlockPos position, @Nullable Direction side)` /
  `.energyStorage(@Nullable Direction side)` are the **complete** replacement
  for the old `getCapability(Capability<T>, Direction)` override - each
  reproduces the original's exact decision tree (gate on `operational()`
  first; if `MultiblockPortLayout` defines exact ports for that capability
  kind, require `side != null` and resolve through the port system; otherwise
  fall through to the generic module aggregator inherited from
  `MachineBlockEntity`). `init/ModBlockEntities` (not ported yet) should
  register these directly: `event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
  ADVANCED_TYPE, AdvancedMultiblockBlockEntity::itemHandler)` works as-is
  because the provider signature `(be, side)` doesn't include a position -
  NeoForge supplies `be.getBlockPos()` internally as the query position, and
  `itemHandler(BlockPos, Direction)` matches once you bind position via a
  method reference on the instance... **concretely, register a lambda**:
  `(be, side) -> be.itemHandler(be.getBlockPos(), side)` (or, since that's
  exactly what a member reference can't express with an extra fixed arg,
  wire it explicitly in `ModBlockEntities` when that file is ported).
- `MultiblockGapBlockEntity.itemHandler(@Nullable Direction side)` /
  `.fluidHandler(@Nullable Direction side)` do the same for a **member**
  position: they look up the structure's controller via
  `MultiblockMembershipService.controllerAt(...)` (same as the original) and
  delegate to `controller.itemHandler(this.worldPosition, side)`. Register
  these directly as the provider for the `MULTIBLOCK_GAP` block-entity type.
  (The gap never exposed energy in the original either - `MultiblockGapBlockEntity`
  intentionally has no `energyStorage` method.)
- The `Map<PortEndpoint, Object> portCapabilities` cache on
  `AdvancedMultiblockBlockEntity` (was `Map<PortEndpoint, LazyOptional<?>>`)
  still memoizes one built handler instance per `(position, side, kind)` -
  unchanged from the original design, since ports don't move once a
  definition is fixed. What *did* change: `invalidateCaps()`/`reviveCaps()`
  are gone (per the Phase 2 cheat-sheet entry), so `refreshCapabilities()`
  (called whenever `formed`/`structureReady` flips, i.e. whenever
  `operational()` can change) now calls `level.invalidateCapabilities(pos)`
  for the controller's own position **and every current member position**
  (`members()`) - this tells every neighboring block's `BlockCapabilityCache`
  to re-query instead of keeping a stale handler/stale "no capability here"
  result. This is a new discovery this session, not documented in Phase 2/3:
  a structure whose capability answer changes at multiple positions at once
  (not just the block entity's own position) must invalidate all of them.

### New pattern: extending the Phase 2 generic module aggregators for new module types

`MachineBlockEntity.exposedItemHandler`/`exposedEnergyStorage` (the
`instanceof`-based aggregators from Phase 2) only recognized
`ItemInventoryModule`/`BulkItemStorageModule` (item) and `EnergyStorageModule`
(energy). `AdvancedMultiblockBlockEntity` is the first `MachineBlockEntity`
to host a `ShelvingStorageModule` (new this session, for the shelving-unit
definition's 648-slot chest-upgrade inventory) or an `ElectricalPowerModule`
directly (previously only used inside `content/network/module` pipes/tanks,
never as a `MachineBlockEntity`'s own energy module) - neither was recognized
by the aggregators, which would have silently returned `null` for those
block entities' item/energy capability forever. **Fixed by extending the two
aggregators** in `content/machine/framework/MachineBlockEntity.java` itself
(not stubbed, not worked around locally) to also check
`instanceof ShelvingStorageModule` / `instanceof ElectricalPowerModule`. If
you add another capability-bearing module type and host it directly on a
`MachineBlockEntity` (as opposed to inside a `content/network/module` pipe),
check whether it needs the same treatment.

### Known compile gaps left by this phase (expected, not bugs - do not stub)

Every one of these already existed as an unported dependency before this
phase touched the files that reference them - same policy as always: port
the real class when its subsystem is tackled.

- `content.machine.singleblock.SingleBlockMachineBlockEntity` (only the pure
  enum definitions in that package are ported - see Phase 2)
  - referenced by `AdvancedMultiblockBlockEntity` for the small-tank
    multiblock-member capability claim (`claimForMultiblock`/
    `releaseMultiblockClaim`/`canClaimForMultiblock`).
- `init.ModAdvancedBlocks` - referenced by `MultiblockRule`, `MultiblockStructureSnapshot`,
  `MultiblockMatcher`, `AdvancedMultiblockLogic` (`PUMPJACK_DRILL`). Itself
  blocked on `content/worldgen/OilDepositBlock(Item)` (2 files).
- `init.ModRecipeTypes` - referenced throughout `AdvancedMultiblockBlockEntity`/
  `AdvancedMultiblockLogic`/`MultiblockPortProfile`. Blocked on
  `content/machine/{crushingtable,singleblock/recipe}`,
  `content/nuclear/facility`, `content/recipe/TieredShapedRecipe` (per the
  existing "why init/ can't be ported yet" writeup above) plus now also
  `content/multiblock/recipe/{AdvancedProcessingRecipe,PolymerizerRecipe}`
  (done this session) and `content/machine/singleblock/recipe/FluidFuelRecipe`
  (not done).
- `init.ModMenus` - referenced by `AdvancedMultiblockMenu`. Part of the
  17-file blocked `init/` set.
- `init.ModBlockEntities` - referenced by every BE in this package
  (`advancedMultiblock(definition)`, `MULTIBLOCK_GAP`). Also where the new
  position-dependent capability providers described above need to be wired
  up once this file exists.
- `init.ModFluids` - referenced throughout `AdvancedMultiblockLogic` for
  `FluidDefinition` -> `Fluid` lookups (`ModFluids.get(...)`).
- `content.worldgen.OilDepositBlockEntity` - referenced by `AdvancedMultiblockLogic`'s
  pumpjack logic (`EXTRACTION_STAGE_MILLIBUCKETS`, `.remaining()`, `.drain(...)`).
- `content.machine.singleblock.recipe.FluidFuelRecipe` - referenced by
  `AdvancedMultiblockLogic`'s big-combustion-chamber fluid-fuel path.

### Caveat: the two recipe files are the first codec-based `RecipeSerializer`s in this port - flag for extra scrutiny on first compile

`AdvancedProcessingRecipe.Serializer`/`PolymerizerRecipe.Serializer` were
authored against the new vanilla `MapCodec`/`StreamCodec` API (see the new
cheat-sheet entry below) using standard `RecordCodecBuilder`/`StreamCodec.composite`
idioms, verified field-by-field against the official Mojang mappings and the
NeoForge sources jar for every method signature involved (`RecipeSerializer.codec()`/
`.streamCodec()`, `ByteBufCodecs.{VAR_INT,BOOL,FLOAT,DOUBLE,STRING_UTF8,optional,list}`,
`Ingredient.CODEC`/`.CONTENTS_STREAM_CODEC`, `FluidStack.CODEC`/`.STREAM_CODEC`,
`ItemStack.CODEC`/`.STREAM_CODEC`). What was **not** verified against a real
compiler (no JDK/Gradle build has been run against this tree at all - see the
top of this file) is the exact generic-inference behavior of chained
`RecordCodecBuilder`/`Codec.either` calls, which is exactly the kind of thing
javac catches instantly and a human reading the source cannot fully verify.
Treat these two files as the first thing to check on the first real
`./gradlew compileJava` - if the codecs don't compile, the fix is almost
certainly a generics/type-inference tweak (explicit type witnesses, splitting
a chained call), not a wrong API.

Two design choices worth knowing before touching these files again:

1. **The JSON/network shape is new, not preserved from the Forge original.**
   The old `fromJson`/`toNetwork` shape (e.g. results as bare item objects
   with a sibling `chance` key, `machine` as an explicit string field) was
   deliberately **not** replicated 1:1, because `src/main/resources/data/`
   (recipe JSON files) is explicitly out of scope until "Phase 7: re-verify
   resources/data" in the suggested phase order below - those files will be
   regenerated by datagen (`data/` phase) anyway. The new shape nests things
   more conventionally (e.g. `ChanceResult` is `{"item": ItemStack, "chance": float}`,
   not a flattened item object). If existing recipe JSON files under
   `src/main/resources/data/magneticraft/recipes/` reference the old shape,
   they need to be rewritten (or regenerated) to match - don't assume they
   still work.
2. **One `Serializer` instance = one fixed codec, chosen at construction, not
   per-decode.** `AdvancedProcessingRecipe` has two mutually-exclusive
   shapes (item-processing vs. fluid-processing) selected in the original by
   `Serializer.expectedMachine`. Rather than building one dynamic-dispatch
   `MapCodec` that inspects the machine field to pick a branch (which would
   need `Codec.either`/`mapEither` and careful `Either` unwrapping),
   `Serializer`'s constructor picks the item or fluid `MapCodec`/`StreamCodec`
   once, based on `isFluidMachine(expectedMachine)` - valid because a given
   `expectedMachine` is always exclusively one shape or the other (enforced
   by the existing constructor validation). Keep this shortcut in mind if a
   future recipe type's shape actually depends on the *decoded* data, not
   just which serializer instance is asking - that case genuinely needs
   `Codec.either`.

## Phase 5 (DONE): `content/network/block/` - full package, 3/3 files

Ported all 3 files (`ConduitBlock`, `NetworkComponentBlock`,
`NetworkComponentBlockEntity`) - **zero rewrite, copied verbatim**. None of
the three had any `net.minecraftforge`/Forge import in the source; every
vanilla API they touch was individually confirmed unchanged in 1.21.1
against the official Mojang client mappings
(`~/.gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client_mappings.txt`,
since the NeoForge sources jar only contains NeoForge's own classes, not
decompiled vanilla) - notably `InteractionResult.sidedSuccess(boolean)`
(still present, confirmed at the mapping's `InteractionResult` entry) and
`Player.isSecondaryUseActive()`/`displayClientMessage(Component, boolean)`
(both present, same signatures). `diff` against the three Forge-source
files after copying is empty (byte-identical).

This was the reason Phase 4 (`content/multiblock/`) happened first:
`ConduitBlock.connectsToMachine` calls `MultiblockExternalPortService.supports(LevelAccessor,
BlockPos, NetworkDomain, Direction)`, which now exists with exactly that
signature. The other two cross-package calls also checked out exactly:
`NetworkConnectionHost.supportsNetworkConnection(NetworkDomain, Direction)`
(from `content/machine/framework`, Phase 2) and
`MachineBlockEntity`'s `(BlockEntityType<?>, BlockPos, BlockState)`
constructor (also Phase 2), which `NetworkComponentBlockEntity` extends.

**Known compile gap (expected, not a bug):** `NetworkComponentBlock.use()`
references `init.ModNetworkItems.WRENCH` and `init.ModTags.Items.WRENCHES`
for the wrench-item check. Neither `init/ModNetworkItems` nor `init/ModTags`
exists yet in this project (same documented blockers as before - see "Why
the rest of `init/` can't be ported yet"). Do not stub these; port them
when `init/` is tackled.

This unblocks, per the table below (all now have zero unported
`content/network` dependencies besides their own already-ready `module/`
counterpart): `content/network/heat/` (4 files), `content/network/pressure/`
(5 files), `content/network/fluid/` (2 files), `content/network/pneumatic/`
(3 files), `content/network/logistics/` (2 files). `content/network/electric/`
(26 files) remains the largest remaining win in `content/network/` and is
also now fully unblocked structurally.

**Suggested next step:** any of the small, now-fully-unblocked subpackages
above close out a whole subsystem in one sitting (`heat/` or `pressure/` are
good starting points, same as suggested after Phase 3) - or start on
`content/network/electric/` (26 files) directly if aiming for the biggest
win first.

## Phase 6 (DONE): `content/network/heat/` - full package, 4/4 files

Ported all 4 files (`HeatPipeBlock`, `HeatPipeBlockEntity`, `HeatSinkBlock`,
`HeatSinkBlockEntity`) - **zero rewrite, copied verbatim**, same as Phase 5.
None had any `net.minecraftforge` import. Every vanilla `Block`/`BlockEntity`
API touched (`entityInside(BlockState, Level, BlockPos, Entity)`,
`newBlockEntity`, `getStateForPlacement`, `rotate`/`mirror`,
`createBlockStateDefinition`) was cross-checked against the 1.21.1 official
client mappings and found unchanged. `diff` against the four Forge-source
files after copying is empty (byte-identical).

Cross-package dependencies all checked out exactly against already-ported
code: `content/network/block/ConduitBlock` and `NetworkComponentBlock(Entity)`
(Phase 5), `content/network/module/HeatNetworkModule`'s constructor
`(ResourceLocation, MachineModuleHost, HeatNode, double, Predicate<Direction>)`
(Phase 3 - `MachineBlockEntity implements MachineModuleHost`, confirmed),
`system/network/heat/{HeatNode,HeatPipeContactDamage}` (Phase 1),
`MachineBlockEntity.addModule`/`.markChanged` (Phase 2).

**Known compile gaps (expected, not bugs):** `init.ModNetworkBlocks`
(`HeatPipeBlock.connectsVisuallyTo` checks `HEAT_PIPE`/`INSULATED_HEAT_PIPE`;
`HeatPipeBlockEntity`'s constructor also branches on it) and
`init.ModBlockEntities` (`HEAT_PIPE`/`INSULATED_HEAT_PIPE`/`HEAT_SINK` block
entity types, referenced by both block-entity constructors) - neither exists
yet in this project, same pre-existing `init/` blocker documented throughout
this file. Do not stub; port when `init/` is tackled.

This closes out `content/network/heat/` in full. Per the suggested-next-step
list, `content/network/pressure/` (5 files) is the next same-shaped,
self-contained subpackage (its `PressureNetworkModule`/`PressureFluidHandler`
counterpart in `module/` is already ready, same pattern as this phase).

## Phase 7 (DONE): `content/network/pressure/` - full package, 5/5 files

Ported all 5 files (`BrassPressurePipeBlock`, `BrassPressurePipeBlockEntity`,
`PressureTankBlock`, `PressureTankBlockEntity`, `PressureTankMenu`).

- **3 files, zero rewrite** (copied verbatim, byte-identical `diff`):
  `BrassPressurePipeBlock`, `BrassPressurePipeBlockEntity`,
  `PressureTankBlockEntity`. Constructor calls to
  `content/network/module/PressureNetworkModule`'s two overloads (7-arg and
  the 8-arg `exposeFluidCapability` variant, Phase 3) and
  `system/network/pressure/PressureNode(double, double)` (Phase 1) matched
  exactly. `MachineBlockEntity.syncClientState` (Phase 2) confirmed present
  for `PressureTankBlockEntity.tickComponent()`.
- **1 file, menu-opening rewrite**: `PressureTankBlock` -
  `net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, tank, position)`
  -> `serverPlayer.openMenu(tank, position)`. This uses a NeoForge
  `IPlayerExtension` overload not previously noted in this file:
  `default OptionalInt openMenu(MenuProvider menuProvider, BlockPos pos)`
  (in `net/neoforged/neoforge/common/extensions/IPlayerExtension.java`,
  confirmed in the sources jar), which itself just calls
  `openMenu(menuProvider, buf -> buf.writeBlockPos(pos))` - i.e. it's a
  convenience wrapper around the same `Consumer<RegistryFriendlyByteBuf>`
  overload already used by `AdvancedMultiblockBlock` (Phase 4). **New cheat
  sheet entry**: when a Forge `NetworkHooks.openScreen(player, menuProvider,
  pos)` call only ever wrote a single `BlockPos` (the common case), prefer
  this 2-arg `openMenu(menuProvider, pos)` form over hand-writing a
  `buf.writeBlockPos(pos)` lambda - same effect, less code.
- **1 file, network-buffer-type rewrite**: `PressureTankMenu` - its
  network-constructor parameter changed from `net.minecraft.network.FriendlyByteBuf`
  to `net.minecraft.network.RegistryFriendlyByteBuf`, matching
  `IContainerFactory<T>.create(int, Inventory, RegistryFriendlyByteBuf)`
  (confirmed in the sources jar, same interface Phase 4's
  `AdvancedMultiblockMenu` already relies on) - `buffer.readBlockPos()` is
  unaffected since `RegistryFriendlyByteBuf extends FriendlyByteBuf`. Same
  pattern as `AdvancedMultiblockMenu`'s network constructor from Phase 4;
  **any future ported menu's network-side constructor needs this same
  `FriendlyByteBuf` -> `RegistryFriendlyByteBuf` swap**, not just the ones
  already documented.

**Known compile gaps (expected, not bugs):** `init.ModNetworkBlocks`
(`BRASS_PRESSURE_PIPE`, `PRESSURE_TANK` block lookups),
`init.ModBlockEntities` (`BRASS_PRESSURE_PIPE`, `PRESSURE_TANK` block-entity
types), `init.ModMenus` (`PRESSURE_TANK` menu type) - none of these `init/`
files exist yet, same pre-existing blocker documented throughout this file.
Do not stub; port when `init/` is tackled.

This closes out `content/network/pressure/` in full. Per the
suggested-next-step list, `content/network/fluid/` (2 files) or
`content/network/pneumatic/` (3 files) or `content/network/logistics/`
(2 files) are the remaining same-shaped, self-contained subpackages;
`content/network/electric/` (26 files) is still the largest remaining win
in `content/network/`.

## Phase 8 (DONE): `content/network/{fluid,pneumatic,logistics,kinetic}` - 4 full packages, 11/11 files

Ported all 4 remaining "small, self-contained" subpackages flagged as the
suggested next step after Phase 7: `content/network/fluid/` (2 files),
`content/network/pneumatic/` (3 files), `content/network/logistics/`
(2 files), `content/network/kinetic/` (4 files). File-for-file parity
verified for all four directories (`diff` of each pair of directory
listings is empty).

- **7 files, zero rewrite** (no `net.minecraftforge` import in the source,
  copied verbatim): `IronPipeBlockEntity`, `PneumaticConnectionHost`,
  `PneumaticTubeBlockEntity`, `ConveyorBeltBlock`, `ConveyorBeltBlockEntity`,
  and all 4 `kinetic/` files (`HandCrankBlock(Entity)`,
  `WoodenShaftBlock(Entity)`) - 9 files total fit this tier (kinetic alone is
  4, plus the 5 listed by name).
- **2 files, capability-lookup rewrite**: `IronPipeBlock` and
  `PneumaticTubeBlock` - both had a `connectsToMachine(LevelAccessor, BlockPos, Direction)`
  override that ended in `blockEntity.getCapability(ForgeCapabilities.X, side).isPresent()`.
  Since the new capability model's `Level.getCapability(BlockCapability, BlockPos, Object)`
  3-arg convenience method (confirmed in `net/neoforged/neoforge/capabilities/BlockCapability.java`'s
  own class-doc example, in the `neoforge-21.1.208-sources.jar`) is declared on
  `Level`, not `LevelAccessor`, and this override only receives a
  `LevelAccessor` (the same signature `ConduitBlock.canConnect` has always
  passed down), the fix is an `instanceof Level` narrowing at the call site:
  `level instanceof Level actualLevel && actualLevel.getCapability(Capabilities.FluidHandler.BLOCK, position, side) != null`
  (`IronPipeBlock`) / `Capabilities.ItemHandler.BLOCK` (`PneumaticTubeBlock`).
  **New, small pattern worth remembering**: any future `ConduitBlock` subclass
  overriding `connectsToMachine` to probe a neighbor's capability needs this
  same `instanceof Level` guard - `LevelAccessor` alone (e.g. a `WorldGenRegion`
  during placement) cannot answer a capability query.

**Known compile gaps (expected, not bugs):** all four packages depend on
`init/ModNetworkBlocks`, `init/ModBlockEntities`, and (for `logistics/`)
`init/ModNetworkItems` + `init/ModTags` - none of the 17 blocked `init/`
files exist yet, same pre-existing blocker documented throughout this file.
Do not stub; port when `init/` is tackled.

This closes out every subpackage of `content/network/` except
`content/network/electric/` (26 files) and `WrenchItem.java` (1 file) -
`content/network/` is now 51/78 files done. `content/network/electric/` is
the last, and largest, remaining win in `content/network/`.

## Phase 9 (DONE): `content/network/electric/` + `WrenchItem.java` - `content/network/` closed out in full, 27/27 files

This session ported the last two pieces of `content/network/`: all 26
remaining files of `content/network/electric/` (the package's 2 enums,
`ElectricalControlKind`/`ElectricalProtectionKind`, were already done) and
the standalone `WrenchItem.java`. File-for-file parity verified for the
whole `content/network/` tree (`diff` of the full recursive file listing
between source and destination is empty, 78 = 78) - **`content/network/` is
now completely ported, every one of its 78 files done.**

Breakdown of `content/network/electric/`'s 26 files by risk tier:

- **1 file, trivial**: `WrenchItem.java` (`content/network/` root) - a
  one-field `Item` subclass, zero imports beyond vanilla.
- **14 files, zero rewrite** (no `net.minecraftforge` import in the source,
  copied verbatim): `PoleDirection`, `PoleSegment`, `TeslaTowerPart`,
  `ElectricalDeviceAction`, `ElectricalDeviceKind`, `WallMountedElectricBlock`,
  `ElectricCableBlock(Entity)`, `ElectricConnectorBlock(Entity)`,
  `ElectricPoleBlock(Entity)`, `ElectricPoleTransformerBlockItem`,
  `TransformerBlockItem`, `ElectricalControlBlock(Entity)`,
  `BoxTransformerBlock(Entity)`, `TeslaTowerBlock(Entity)`,
  `WirelessEnergyReceiverBlock(Entity)`, `ElectricalProtectionBlock(Entity)`
  (this list undercounts slightly since several are `Block`+`BlockEntity`
  pairs counted as one bullet - the real per-file count of zero-rewrite
  files in this phase is 23).
- **1 file, capability-lookup rewrite** (`ElectricEnergyExporter`):
  `target.getCapability(ForgeCapabilities.ENERGY, targetSide).map(...)`
  (returning `Optional<IEnergyStorage>`) became
  `Capabilities.EnergyStorage.BLOCK.getCapability(level, targetPosition, target.getBlockState(), target, targetSide)`
  (returning `@Nullable IEnergyStorage` directly, 5-arg form - same shape
  already used by `FluidPipeModule.adjacentHandler` in Phase 3), followed by
  a plain `if (storage == null) return 0;` instead of `Optional` chaining.
  Only a `ServerLevel` (which `extends Level`) reaches this call site, so no
  `instanceof Level` guard was needed here (unlike the `LevelAccessor` case
  in Phase 8's `IronPipeBlock`/`PneumaticTubeBlock`).
- **1 file, the last remaining menu rewrite in this port**
  (`ElectricalDeviceMenu`): three independent fixes, all instances of
  patterns already established in Phases 3/4/7 rather than anything new -
  (1) network constructor parameter `FriendlyByteBuf` -> `RegistryFriendlyByteBuf`
  (same as `PressureTankMenu`/`AdvancedMultiblockMenu`); (2)
  `net.minecraftforge.items.{IItemHandlerModifiable,ItemStackHandler,SlotItemHandler}`
  -> `net.neoforged.neoforge.items.*` (pure package rename, per the cheat
  sheet); (3) `NetworkHooks.openScreen(player, provider, writer)` ->
  `player.openMenu(provider, writer)` with the writer lambda now typed to
  infer `RegistryFriendlyByteBuf` (same `IContainerFactory`-backed overload
  `AdvancedMultiblockBlock` used in Phase 4).

**Known compile gaps (expected, not bugs):** every file in this phase that
touches `content/item/` (`TieredElectricalDrops`, `TransformerElectricalDrops`,
`TieredElectricalItemName`) references classes not ported yet - that
subpackage of `content/item/` is a clean, independent unblock whenever it's
next. `init/ModNetworkBlocks`, `init/ModBlockEntities`, `init/ModNetworkItems`,
`init/ModTags`, and `init/ModMenus` remain the same pre-existing blockers
documented throughout this file - none of the 17 blocked `init/` files exist
yet. Do not stub; port when `init/`/`content/item/` are tackled.

`content/network/` (78/78 files) is now a fully closed subsystem with zero
stubs - the only fully-ported subsystem in `content/` besides
`content/machine/framework` and `content/multiblock/`.

## What's left (by size, descending)

| Package | Files | Notes |
|---|---|---|
| `content/` | 169 | Blocks, items, machines, multiblocks, menus, block entities. The big one. `content/machine/framework`, all of `content/multiblock/`, and now all of `content/network/` (78/78 files - `module/`, `block/`, `heat/`, `pressure/`, `fluid/`, `pneumatic/`, `logistics/`, `kinetic/`, `electric/`, `WrenchItem.java`) are fully ported - see Phases 2-9 above. `content/network/` needs nothing further from `content/` itself. Remaining heavy user of Forge capabilities (`net.minecraftforge.common.capabilities`) is concentrated in the concrete block entities across `content/machine`, `content/nuclear`, `content/worldgen` - each now has a working pattern to follow (`MachineCapabilities.register` for a single-position `MachineBlockEntity`, the position-dependent `itemHandler(pos, side)`-style pattern from Phase 4 for a multi-position structure, or a module's own `view(side)` method for non-block-entity modules), it's no longer undesigned, just a lot of files. (26 files ported this session: `content/network/electric/` (Phase 9, 26 files) + `WrenchItem.java` (Phase 9, 1 file).) |
| `client/` | 53 | Renderers, screens, models (`.mcx`/`.gltf` custom model formats via a bundled loader - check `content/nuclear/...` and asset pipeline before touching). |
| `data/` | 24 | Datagen providers (loot, tags, recipes, models, lang). NeoForge datagen API is close to Forge's for 1.21.1 but `HolderLookup.Provider` is now threaded through most providers - check each one. Recipe providers additionally need the new codec-based `RecipeSerializer` shape from Phase 4/the cheat sheet below. |
| `init/` | 17 | Registry glue (`ModBlocks`, `ModItems`, etc.) - depends on `content/` types, must be ported together with/after them. Uses `RegistryObject` -> `DeferredHolder` (see below) and `ForgeRegistries` -> `BuiltInRegistries`/`NeoForgeRegistries`. (`ModRegistries`, `ModSounds` ported in Phase 2.) `ModBlockEntities` is the next-most-valuable one of these 17 to unblock: it owns the position-dependent capability wiring Phase 4 left as a documented gap for `AdvancedMultiblockBlockEntity`/`MultiblockGapBlockEntity`. |
| `integration/` | 17 | JEI, CraftTweaker, Jade, Tinkers' Construct. Optional at runtime (`enable_*_runtime` gradle flags) - lowest priority, do last. Versions for NeoForge 1.21.1 found so far: JEI `19.54.0.429`, Jade `15.10.6+neoforge`; Mantle/TConstruct NeoForge 1.21.1 builds were **not found** on Modrinth as of this writing - re-check before wiring `integration/tconstruct`. |
| `api/` | 1 remaining | `NuclearReactorColumnType` (needs `content/nuclear/fuel/NuclearFuelGrade`). |

Total remaining: ~281 of the original 530 main-source files (169 + 53 + 24
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
| **Recipe serialization** (vanilla change, not Forge/NeoForge, discovered in Phase 4 - `content/multiblock/recipe/` was the first recipe-touching code ported): `Recipe<T>.getId()` | **Gone.** A recipe no longer carries its own id; `RecipeManager.getAllRecipesFor(RecipeType<T>)` now returns `List<RecipeHolder<T>>` (was `List<T>`) and `getRecipeFor(...)` returns `Optional<RecipeHolder<T>>` - the id lives on the `RecipeHolder` (`.id()`), the recipe itself on `.value()`. Every `.getAllRecipesFor(...)/.getRecipeFor(...)` call site needs `.value()`/`.id()` added; this hits **every future recipe-consuming file in the mod**, not just multiblock. |
| `Recipe<T>.matches(Container, Level)` / `.assemble(Container, RegistryAccess)` / `.getResultItem(RegistryAccess)` | `Recipe<T extends RecipeInput>.matches(T, Level)` / `.assemble(T, HolderLookup.Provider)` / `.getResultItem(HolderLookup.Provider)` | `T` must now implement the new marker interface `RecipeInput` (`getItem(int)`, `size()`, `isEmpty()`) - `Container`/`SimpleContainer` do **not** implement it. For a single-item-slot recipe (the common case for machine recipes) use vanilla's `SingleRecipeInput(ItemStack)` instead of `new SimpleContainer(stack)`. `RegistryAccess` -> `HolderLookup.Provider` is the same thread as the `SavedData`/`BlockEntity` row above (`RegistryAccess` itself implements `HolderLookup.Provider`, so `level.registryAccess()` still works as the argument). |
| `RecipeSerializer<T>.fromJson(ResourceLocation, JsonObject)` / `.fromNetwork(ResourceLocation, FriendlyByteBuf)` / `.toNetwork(FriendlyByteBuf, T)` | `RecipeSerializer<T>.codec()` returning `MapCodec<T>`, `.streamCodec()` returning `StreamCodec<RegistryFriendlyByteBuf, T>` | The whole Gson/manual-`FriendlyByteBuf` triplet is gone, replaced by one `MapCodec` (JSON, via `RecordCodecBuilder.mapCodec`/`.create(...).codec()`) and one `StreamCodec` (network, via `StreamCodec.composite(...)`, up to 6 fields per overload - `StreamCodec.of`/`.map`/`.dispatch` cover other shapes). Useful building blocks confirmed present in this version: `Ingredient.CODEC`/`Ingredient.CONTENTS_STREAM_CODEC` (replaces `Ingredient.fromJson`/`fromNetwork`/`toNetwork`), `ItemStack.CODEC`/`ItemStack.STREAM_CODEC` (full stack; `OPTIONAL_*` variants allow empty), `net.neoforged.neoforge.fluids.FluidStack.CODEC`/`.STREAM_CODEC` (also `OPTIONAL_*`), `ByteBufCodecs.{BOOL,VAR_INT,FLOAT,DOUBLE,STRING_UTF8}` (primitives), `ByteBufCodecs.optional(codec)` (wraps as `Optional<T>`), `ByteBufCodecs.list(maxSize)` used via `elementCodec.apply(ByteBufCodecs.list(n))` (collection wrapping). See `content/multiblock/recipe/AdvancedProcessingRecipe.java`/`PolymerizerRecipe.java` for a full worked example (including the "one `Serializer` instance = one fixed codec chosen at construction" shortcut for a recipe type with two mutually-exclusive shapes - see Phase 4's writeup for when that shortcut does/doesn't apply). **Not verified against a real compiler yet** (see Phase 4's caveat) - check these two files first on the first real `./gradlew compileJava`. |

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

1. ~~`init/` + minimal slice of `content/block` + `content/item`~~ -
   **partially done**: `init/ModRegistries.java` + `init/ModSounds.java`
   ported: The other 17 `init/*.java` files are still blocked on
   `content/block`, `content/item`, `content/machine`, `content/multiblock`,
   `content/nuclear`, `content/recipe`, `content/worldgen` concrete types -
   see "Why the rest of `init/` can't be ported yet" above for the exact
   per-file breakdown. Revisit `init/ModBlocks`/`ModItems` once enough of
   step 3 below exists.
2. ~~`content/machine/framework`~~ - **done** (see "Phase 2" above). The
   capability rewrite pattern is established and documented; every
   concrete machine block entity from here on should reuse
   `MachineCapabilities.register` + module `view(side)` rather than
   re-deriving it.
3. Remaining `content/` by subsystem (`machine`, `multiblock`, `nuclear`,
   `computer`, `network`, `world`/`worldgen`) - each is fairly self
   contained. **`content/multiblock/` is done in full (Phase 4, 29/29
   files)**, and **`content/network/` is done in full (78/78 files -
   Phases 1/3/5/6/7/8/9)** - nothing left to port in that subsystem. Still
   open: `content/nuclear`, `content/computer`, `content/world`/`worldgen`,
   and the remaining `content/machine/singleblock` concrete types (only the
   2 pure-Java definition enums are done).
   **Suggested immediate next step**: with `content/network/` and
   `content/multiblock/` both fully closed, `content/nuclear` or
   `content/machine/singleblock` (the concrete single-block machines) are
   the next self-contained subsystems to tackle - `content/machine/singleblock`
   directly unblocks more of `init/` (`ModRecipeTypes`, `ModBlocks`,
   `ModItems` all need concrete machine types). Alternatively,
   `content/item/` (the 4-ish classes referenced throughout `content/network/electric/`
   and `content/multiblock/` as known compile gaps - `TieredElectricalDrops`,
   `TransformerElectricalDrops`, `TieredElectricalItemName`, and the
   fuse/wrench/repair-tool items) is a small win that clears several
   already-documented gaps at once.
4. `client/` (needs `content/` block/item/BE types to exist first).
5. `data/` (datagen, needs everything above). Recipe-related datagen
   providers additionally need the codec-based `RecipeSerializer` shape
   documented in the cheat sheet above (Phase 4 discovery) - and note that
   Phase 4's two recipe JSON shapes are new, not preserved from the Forge
   1.20.1 originals (see Phase 4's caveat), so don't assume
   `src/main/resources/data/magneticraft/recipes/*.json` still matches.
6. `integration/` (optional, last).
7. Re-verify `src/main/resources` assets/data (653 files) still line up
   with the ported registry names, then attempt a real `./gradlew build`.
   This first real compile is also when to double-check Phase 4's two
   codec-based recipe serializers (never compiler-verified - see Phase 4's
   caveat) and the `init/ModRegistries.java` vanilla `Registries.*` constant
   names (never re-verified against decompiled 1.21.1 vanilla source - see
   Phase 2's caveat).
