# Magneticraft: Forge 1.20.1 -> NeoForge 1.21.1 porting notes

Source: `../Magneticraft-forge-1.20.1` (530 main-source Java files, ~77.5k lines).
This directory is the destination project. **As of Phase 25, `./gradlew build`
succeeds** (first real compile happened that phase, 75 errors found and
fixed - see Phase 25 for the full discovery list and the cheat sheet above
for the reusable API-mapping entries). Everything in Phases 1-24 below was
written "verified by reading" only, before any compiler ever ran against
this tree; treat phase-by-phase narrative claims about untested code as
historical, superseded by Phase 25's actual green build.

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

## Phase 10 (DONE): `content/item/` - full package, 25/25 files

This session ported **all 25 files** of `content/item/` (the small-win
subsystem flagged as an alternative next step after Phase 9 - handheld
tools, batteries, diagnostic probes, the tiered-electrical block-item
family, and the fuse/wrench/repair-tool items that `content/network/electric/`
and `content/multiblock/` were still missing). File-for-file parity verified
(`diff` of the two directory listings is empty, 25 = 25), plus one **new**
file, `PortableEnergyComponents.java` (a data-component registration this
Forge version didn't need - see below, not a stub, real code).

This phase hit more genuine 1.21.1 vanilla-API breakage than any phase since
Phase 2/4 - **`ItemStack` no longer carries ad hoc NBT at all** (the
1.20.5+ item-component rework removes `getTag()`, `setTag()`,
`getOrCreateTag()`, `addTagElement()`, `removeTagElement()`, `hasTag()`
entirely - confirmed by their total absence from the 1.21.1 official Mojang
mappings). Every one of these needed a real redesign, not a rename. Breakdown
by risk tier:

- **17 files, zero rewrite** (pure Java or vanilla-only, no edits needed at
  all, copied verbatim): `CraftingComponent`, `HammerType`,
  `ElectricalFuseVisualVariant`, `SulfurItem`, `ElectricChainsawItem`,
  `ElectricDrillItem`, `ElectricPistonItem`, `ElectricalFuseItem`,
  `ThermometerItem`, `ProtectionBlockItem`, `TieredElectricalBlockItem`,
  `TieredElectricalItemName`, `TieredElectricalDrops`,
  `TransformerElectricalDrops`, `LowBatteryItem`, `MediumBatteryItem`,
  `OilProspectorItem` (the last four's only electrical-tier/energy plumbing
  lives in already-ported `system/`/`content/item` classes, not in these
  files themselves).
- **3 files, zero `net.minecraftforge` import but broken by pure
  vanilla-version drift** (see the new cheat-sheet entries below - this is
  not a Forge/NeoForge rename, the same code would also break on an
  unmodified 1.21.1 vanilla server): `HammerItem`, `ElectricalRepairToolItem`
  (both hit the `hurtAndBreak`/`broadcastBreakEvent` removal),
  `ElectricToolItem` (hit the `TierSortingRegistry` removal).
- **2 files, `ItemStack`-NBT-to-`CustomData` rewrite**: `CopperWireCoilItem`,
  `VoltmeterItem` - see the new cheat-sheet entry below.
- **1 file, Forge event-bus rewrite** (the last `*Events.java` shape already
  established in Phases 1/3/4): `DiagnosticInteractionEvents` -
  `PlayerInteractEvent.RightClickBlock` moved packages
  (`net.minecraftforge.event.entity.player` ->
  `net.neoforged.neoforge.event.entity.player`) and its cancellation API
  changed shape: `event.setUseItem(Event.Result.DENY)` is now
  `event.setUseItem(TriState.FALSE)` (`net.neoforged.neoforge.common.util.TriState`)
  - the boolean/tri-state `useBlock`/`useItem` fields replace the old
  `Event.Result` enum entirely, confirmed by reading
  `net/neoforged/neoforge/event/entity/player/PlayerInteractEvent.java` in
  the sources jar.
- **1 file, registry-rename + `DefaultedRegistry` nuance**: `PressureGaugeItem`
  - `net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(id)` (which
  can return `null`) became `net.minecraft.core.registries.BuiltInRegistries.FLUID`,
  a vanilla `DefaultedRegistry<Fluid>` - confirmed in the decompiled vanilla
  source that `DefaultedRegistry<T>.get(ResourceLocation)` is `@Nonnull` and
  falls back to the registry's default entry (`Fluids.EMPTY`) for an unknown
  id, it never returns `null`. **New, easy-to-miss pattern**: any future
  `ForgeRegistries.X.getValue(id) == null` null-check ported against a
  vanilla `DefaultedRegistry` (fluids, blocks, items, entity types, ...) must
  become `!BuiltInRegistries.X.containsKey(id)` instead - a literal
  `== null` translation compiles fine but is always false and silently
  breaks the fallback path.
- **1 file, first item-capability rewrite in this port**: `PortableEnergyItem`
  - see the two new patterns below (this is architecturally the biggest
  piece of this phase, comparable to Phase 2's block-capability rewrite but
  for items).

### New vanilla-version cheat-sheet entries this phase (not Forge/NeoForge - hits any future file touching these APIs)

- **`ItemStack` has no NBT accessors at all any more.** `getTag()`/
  `setTag(CompoundTag)`/`getOrCreateTag()`/`hasTag()`/`addTagElement(String,
  Tag)`/`removeTagElement(String)` are all gone (confirmed absent from the
  1.21.1 official Mojang mappings). The direct replacement for "get-or-create
  a tag and mutate it" is the static helper
  `net.minecraft.world.item.component.CustomData.update(DataComponentType<CustomData>,
  ItemStack, Consumer<CompoundTag>)` (typically called with
  `DataComponents.CUSTOM_DATA`); for a plain read, `stack.get(DataComponents.CUSTOM_DATA)`
  returns `@Nullable CustomData`, and `CustomData.copyTag()` gets the
  `CompoundTag` back out. To clear it: `stack.remove(DataComponents.CUSTOM_DATA)`
  if the tag is now empty, or `stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag))`
  otherwise. **This caught two live regressions in already-ported code**:
  `system/network/electric/item/TieredElectricalItemData.write/read` (Phase 1,
  used `addTagElement`/`getTag`) and
  `content/multiblock/AdvancedMultiblockBlockEntity.saveToItem` (Phase 4, used
  `addTagElement("BlockEntityTag", ...)` - the vanilla replacement for that
  specific well-known key is the dedicated `DataComponents.BLOCK_ENTITY_DATA`
  component, same `CustomData` wrapper, confirmed against vanilla's own
  `BlockItem.setBlockEntityData`/`BlockEntity.saveToItem` in the decompiled
  source). Both fixed this session. **If you find another
  `.getTag()`/`.setTag()`/`.addTagElement()`/`.hasTag()` call on an
  `ItemStack` anywhere in this codebase, it is the same bug, not a new one**
  (a repo-wide grep this session found only these two beyond the two new
  Phase 10 files that needed the same fix from scratch).
- **`net.minecraftforge.common.TierSortingRegistry` has no NeoForge
  replacement - it is simply gone** (confirmed absent from the entire
  `neoforge-21.1.208-sources.jar`, not just moved). Tool-tier gating moved
  fully into vanilla's `Tool` data component
  (`net.minecraft.world.item.component.Tool`, attached via
  `Tier.createToolProperties(TagKey<Block>)` and read by the *default*
  `Item.isCorrectToolForDrops(ItemStack, BlockState)` - confirmed in
  decompiled `Item.java`: `Tool tool = stack.get(DataComponents.TOOL); return
  tool != null && tool.isCorrectForDrops(state);`). A custom `Item` like
  `ElectricToolItem` that overrides `isCorrectToolForDrops(ItemStack,
  BlockState)` directly (bypassing the `Tool` component because it gates on
  dynamic energy instead of a static tag) no longer has anything to call for
  "is this at-most-diamond-tier" - the fix is to compare against the tier's
  own tag directly: `!state.is(Tiers.DIAMOND.getIncorrectBlocksForDrops())`
  (`Tier.getIncorrectBlocksForDrops()` is the same per-tier `TagKey<Block>`
  vanilla always had, e.g. `BlockTags.INCORRECT_FOR_DIAMOND_TOOL` for
  `Tiers.DIAMOND` - it doesn't need a sorting registry when you're hard-coding
  one specific vanilla tier rather than comparing arbitrary modded tiers
  against each other). Also note the *vanilla* `Item.isCorrectToolForDrops(BlockState)`
  1-arg overload Forge added no longer exists either - only the 2-arg
  `(ItemStack, BlockState)` form remains on `Item`.
- **`appendHoverText`'s signature changed** (vanilla, not Forge/NeoForge):
  `(ItemStack, @Nullable Level, List<Component>, TooltipFlag)` became
  `(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)` - confirmed
  in decompiled `Item.java`. `Item.TooltipContext` is a small interface
  (`registries()`, `tickRate()`, `mapData(MapId)`) with an `EMPTY` constant;
  a tooltip that doesn't need level/registry access (like
  `PortableEnergyItem`'s energy readout) just changes the parameter type and
  ignores it, same as before.
- **`LivingEntity.broadcastBreakEvent(...)` (both the `EquipmentSlot` and
  `InteractionHand` overloads) is gone entirely** - confirmed absent from
  decompiled `LivingEntity.java`/`Entity.java`. The old
  `stack.hurtAndBreak(int, LivingEntity, Consumer<LivingEntity>)` overload is
  also gone. The direct replacement for the common "break in this slot" case
  is the new 3-arg `ItemStack.hurtAndBreak(int amount, LivingEntity entity,
  EquipmentSlot slot)`, which internally calls the new
  `LivingEntity.onEquippedItemBroken(Item, EquipmentSlot)` for you (no lambda
  needed any more). To convert an `InteractionHand` (e.g. from
  `UseOnContext.getHand()`) to the `EquipmentSlot` this method wants, use the
  existing vanilla static helper `LivingEntity.getSlotForHand(InteractionHand)`
  rather than hand-rolling the ternary. Hit in `HammerItem.hurtEnemy` and
  `ElectricalRepairToolItem.useOn`, both already-copied "zero rewrite" files
  from earlier in this same session that turned out to need this fix -
  **re-check any other already-ported file that calls `hurtAndBreak` with a
  lambda third argument**, it has the same bug.

### New pattern: item capabilities (`ItemCapability`) - the first one in this port

`PortableEnergyItem` is the first `Item` in the port that exposed a Forge
capability (`ForgeCapabilities.ENERGY` via `initCapabilities`/
`ICapabilityProvider`/`ICapabilitySerializable`/`LazyOptional`, all gone, same
"gone" row as the block-capability cheat-sheet entry from Phase 2). The
replacement (`net.neoforged.neoforge.capabilities.ItemCapability<T, C>`,
confirmed in the sources jar) is a **structurally different, simpler** model
than the block-entity one:

1. **No per-stack cached provider object.** A block capability provider is
   registered once per `(capability, BlockEntityType)` pair and the returned
   handler instance is memoized by `BlockCapabilityCache` until explicitly
   invalidated (Phase 2). An item capability provider
   (`RegisterCapabilitiesEvent.registerItem(ItemCapability<T,C> capability,
   ICapabilityProvider<ItemStack, C, T> provider, ItemLike... items)`,
   confirmed in `RegisterCapabilitiesEvent.java`) is instead called fresh
   **every single query**, handed the exact `ItemStack` instance being
   queried - there is no stack-keyed cache to invalidate, because the
   provider is expected to be a cheap, stateless view constructed on demand.
   `Capabilities.EnergyStorage.ITEM` is `ItemCapability<IEnergyStorage,
   @Nullable Void>` (confirmed in `Capabilities.java`) - the context type is
   `Void`, items have no "side" concept.
2. **No serialize/deserialize step - the view reads/writes the stack
   directly.** Since `ItemStack.set()`/`.update()`/`.remove()` mutate the
   stack's component map in place (confirmed in decompiled `ItemStack.java` -
   `components` is a plain mutable field, not copy-on-write), a view object
   holding a reference to the queried `ItemStack` can safely call
   `stack.set(component, value)` inside `receiveEnergy`/`extractEnergy` and
   the mutation is visible immediately to whoever holds that same stack
   (inventory slot, hand, etc.) - exactly the effect the old
   `ICapabilitySerializable` achieved with an explicit NBT round trip, but
   with zero serialization code. Any per-stack mutable state a capability
   view needs to persist (here: the energy balance) has to live in a real
   `DataComponentType` on the stack, since there is no more generic
   "capability object's own NBT" to lean on - see
   `PortableEnergyComponents.java` (new file) for the minimal
   `DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build()`
   shape, registered through a new `ModRegistries.DATA_COMPONENT_TYPES`
   (`DeferredRegister<DataComponentType<?>>`, `Registries.DATA_COMPONENT_TYPE`)
   added to `init/ModRegistries.java` alongside the other root registries.
   `PortableEnergyItem` reuses the existing loader-independent
   `system/energy/PortableEnergyState` class to keep the receive/extract/
   consume clamping logic identical to the Forge version - only where the
   balance is read from/written to changed (component instead of NBT).
3. **Registration is deferred, same shape as `MachineCapabilities.register`.**
   `PortableEnergyItem.registerCapability(RegisterCapabilitiesEvent event,
   ItemLike... items)` is written and ready, but nothing calls it yet -
   exactly like Phase 2's `MachineCapabilities.register` before
   `init/ModBlockEntities` existed. **`init/ModItems`/`init/ModMachineItems`
   (still blocked, part of the 17 unported `init/*.java` files) should call
   `PortableEnergyItem.registerCapability(event, LOW_BATTERY.get(),
   MEDIUM_BATTERY.get(), MEDIUM_BATTERY.get(), ELECTRIC_DRILL.get(), ...)`
   once those `DeferredItem`s exist**, listing every concrete
   `PortableEnergyItem` subtype (`LowBatteryItem`, `MediumBatteryItem`, every
   `ElectricToolItem` subclass, `OilProspectorItem`) in one call - the
   provider lambda already dispatches on `stack.getItem() instanceof
   PortableEnergyItem` internally, so one registration call covers every
   subtype at once.

### Known compile gaps left by this phase (expected, not bugs - do not stub)

- `OilProspectorItem` references `content.worldgen.{OilDepositSavedData,
  OilFieldSurvey}` - `content/worldgen/` is not ported yet (0 files).
- `TieredElectricalItemName` references `client.electrical.ClientVoltageTierRegistry`
  - `client/` is not ported yet (0 files); this was already a documented
  future dependency, not new to this phase.
- `PortableEnergyItem.registerCapability` is written but uncalled - see
  the item-capability pattern above; wire it from `init/ModItems`/
  `init/ModMachineItems` once those exist.
- `ModRegistries.register()` still calls `ModBlocks.bootstrap()` and 14 other
  `init/*.bootstrap()` methods that don't exist yet (pre-existing, documented
  gap, unrelated to this phase - `init/ModRegistries.java`/`init/ModSounds.java`
  remain the only two of the 17 `init/*.java` files that are actually ported).

This closes out `content/item/` in full - it was the last of the three
"small win" subsystems flagged as alternatives after Phase 9
(`content/nuclear`, `content/machine/singleblock`, `content/item` - only the
latter is done now). **Suggested next step**: `content/machine/singleblock`
(the concrete single-block machines) is the highest-leverage remaining pick -
it directly unblocks more of `init/` (`ModRecipeTypes`, `ModBlocks`,
`ModItems` all need concrete machine types, and `ModItems` is now also what
`content/item`'s new `PortableEnergyItem.registerCapability` needs), or
`content/nuclear` if aiming to close another whole subsystem first.

## Phase 11 (DONE): `content/machine/singleblock` - full package, 25/25 files, plus `content/world/ProtectedWorldMutation` (new prerequisite)

This session ported the last of the three "content subsystem" alternatives flagged
after Phase 9/10: **all 25 files** of `content/machine/singleblock` (the concrete
Task-5 utility/generator machines - box, fabricator, sluice box, feeding trough,
inserter, filter/relay/transposer, block breaker, sprinkler, water generator,
small tank, steam boiler, gasification unit, brick furnace, combustion chamber,
electric/RF heater, RF transformer, electric engine, internal combustion engine,
airlock, thermopile, infinite energy, geothermal pump - the whole
`SingleBlockMachineDefinition` catalogue). File-for-file parity verified (`diff`
of the two directory listings, recursive, is empty, 25 = 25); 2 files
(`SingleBlockMachineDefinition`, `SingleBlockPortProfile`) were already ported in
Phase 2, so 23 files were new this session, plus one **new prerequisite file**,
`content/world/ProtectedWorldMutation.java` (a permission-aware FakePlayer-based
world-mutation helper used by the block breaker/sprinkler/geothermal pump - real
code ported from the Forge source's `content/world/` package, not a stub; this
package didn't exist in the destination tree before this session).

This phase hit the **first vanilla-recipe-API rewrite outside `content/multiblock/recipe`**
(all 4 files in `content/machine/singleblock/recipe/`) and the **first "hide capabilities
entirely based on runtime state" requirement** on a plain `MachineBlockEntity` (the
small-tank multiblock-claim gate), on top of routine Forge->NeoForge package renames.
Breakdown by risk tier:

- **7 files, zero rewrite** (pure Java or vanilla-only, no edits needed at all,
  copied verbatim): `AirBubbleBlock`, `AirBubbleOwnershipSavedData` (only needed
  the standard `SavedData.Factory`/`HolderLookup.Provider` signature already
  established in Phase 1/4, not a new pattern), `AirlockPlan`, `GeothermalPumpState`
  (once its `content/world/ProtectedWorldMutation` dependency existed),
  `SingleBlockMachineLogic`, `SingleBlockMachineMath`, `SingleBlockMachineState`,
  `TubeLightBlock`.
- **1 new prerequisite file**: `content/world/ProtectedWorldMutation.java` - see
  below.
- **1 file, `ItemStack`-NBT-to-`CustomData` + `appendHoverText` signature rewrite**:
  `SmallTankBlockItem` (both already-documented Phase 10 cheat-sheet patterns -
  `stack.getTagElement("BlockEntityTag")` -> `stack.get(DataComponents.BLOCK_ENTITY_DATA)`
  + `CustomData.copyTag()`, and `FluidStack.loadFluidStackFromNBT(CompoundTag)` ->
  `FluidStack.parseOptional(HolderLookup.Provider, CompoundTag)`, sourced from the
  tooltip's `Item.TooltipContext.registries()`, which is itself `@Nullable` - the
  tooltip line is silently skipped if it's null).
- **1 file, capability-model rewrite for a module that isn't a `MachineBlockEntity`
  or a `content/network` pipe**: `PneumaticEndpointModule` - see the new pattern
  below (had to become `public` so `MachineBlockEntity` could recognize it).
- **1 file, one-line menu-opening rewrite**: `SingleBlockMachineBlock`
  (`NetworkHooks.openScreen(serverPlayer, machine, machinePosition)` ->
  `serverPlayer.openMenu(machine, machinePosition)`, the same 2-arg
  `IPlayerExtension` overload Phase 7's `PressureTankBlock` used).
- **1 file, network-buffer-type rewrite**: `SingleBlockMachineMenu`
  (`FriendlyByteBuf` -> `RegistryFriendlyByteBuf` in the network constructor,
  same pattern as every other ported menu since Phase 4) plus the routine
  `net.minecraftforge.items.*` -> `net.neoforged.neoforge.items.*` rename.
- **4 files, capability-lookup-only rewrite** (no capability-model redesign,
  just call-site updates to the new `Capabilities.*`/`Level.getCapability`
  shapes from the Phase 2/3/8 cheat sheet, one new automation-entity capability
  token): `SingleBlockMachineSupport`, `SingleBlockMachineInteractions`,
  `SingleBlockThermalLogic`, `SingleBlockAutomationLogic` - the last of these
  is also where `net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(...)`
  (int-returning) became `net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(...)`
  (returns a `BlockEvent.BreakEvent`; check `.isCanceled()` instead of `== -1`) -
  see the new cheat-sheet entry below.
- **1 file, the position-dependent "hide entirely" capability pattern**:
  `SingleBlockMachineBlockEntity` - see the new pattern below. Also the file that
  needed the `saveTankToItem`/`visualStateHash` NBT-to-components fixes (same
  Phase 10 patterns as `SmallTankBlockItem`, but via vanilla's `BlockEntity.saveToItem`-equivalent
  `saveCustomOnly(HolderLookup.Provider)` instead of hand-rolling metadata
  stripping - see below).
- **1 file, the vanilla crafting-recipe-API rewrite**: `SingleBlockFabricator` -
  see the new cheat-sheet entry below (first `RecipeType.CRAFTING`-consuming file
  in this port; different from the Phase 4 `SingleRecipeInput` shape).
- **4 files, the non-crafting vanilla recipe rewrite** (`content/machine/singleblock/recipe/`):
  `FluidFuelRecipe`, `GasificationRecipe`, `SluiceRecipe`, `ThermopileRecipe` - same
  `Recipe<SingleRecipeInput>` + `codec()`/`streamCodec()` shape as Phase 4's
  `AdvancedProcessingRecipe`/`PolymerizerRecipe`, see the new codec-building-block
  entries below (`Registry.byNameCodec()`, `ByteBufCodecs.registry(ResourceKey)`,
  `ItemStack.OPTIONAL_CODEC`/`OPTIONAL_STREAM_CODEC`, `FluidStack.OPTIONAL_CODEC`/`OPTIONAL_STREAM_CODEC`,
  `ByteBufCodecs.map(...)` for `ThermopileRecipe`'s block-state-property map).

### New file: `content/world/ProtectedWorldMutation.java`

Forge's original used `ForgeHooks.onBlockBreakEvent`/`ForgeEventFactory.onBlockPlace`/
`BlockSnapshot`/`FakePlayer`/`FakePlayerFactory` to let an owner-bound `FakePlayer`
break/place blocks with full protection-mod compatibility and no item drops. All
five have NeoForge equivalents, confirmed in the `neoforge-21.1.208-sources.jar`:

- `net.minecraftforge.common.util.FakePlayer`/`FakePlayerFactory` -> pure package
  rename, `net.neoforged.neoforge.common.util.FakePlayer`/`FakePlayerFactory`
  (`FakePlayer extends ServerPlayer`, unchanged).
- `net.minecraftforge.common.util.BlockSnapshot` -> pure package rename for
  `create(ResourceKey<Level>, LevelAccessor, BlockPos)`, **but its restore method
  changed**: the old `restore(boolean force, boolean notify)` is gone, replaced by
  `restore(int flags)`/`restore()` (uses the flags the snapshot was created with -
  `Block.UPDATE_ALL` is the direct equivalent of the old `restore(true, true)`).
- `net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(Level, GameType, ServerPlayer, BlockPos)`
  (returned `int`, `-1` meant cancelled) -> `net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(Level, GameType, ServerPlayer, BlockPos, BlockState)`
  (takes the target `BlockState` too, returns a `BlockEvent.BreakEvent` - check
  `.isCanceled()` instead of comparing to `-1`). **This hits every future file
  that fake-breaks a block for drop-free protection-aware removal**, not just this
  one - `SingleBlockAutomationLogic.breakBlock` needed the identical fix.
- `net.minecraftforge.event.ForgeEventFactory.onBlockPlace(Entity, BlockSnapshot, Direction)` ->
  `net.neoforged.neoforge.event.EventHooks.onBlockPlace(Entity, BlockSnapshot, Direction)` -
  pure package+class rename, identical signature and return semantics (`true` means
  cancelled/blocked).

### New pattern: a non-`MachineBlockEntity`, non-`content/network`-module capability holder

`PneumaticEndpointModule` (the shared FIFO buffer behind the relay/filter/transposer)
exposes an insertion-only `IItemHandler` on its module host's back face - the same
shape as a `content/network` pipe module's `view(side)` (Phase 3), but the *host*
here is a plain `MachineBlockEntity` (`SingleBlockMachineBlockEntity`), which only
recognizes `ItemInventoryModule`/`BulkItemStorageModule`/`ShelvingStorageModule`/
`ElectricalPowerModule`/`EnergyStorageModule` in its `exposedItemHandler`/`exposedEnergyStorage`
aggregators (Phase 2, extended in Phase 4). Followed the same "extend the generic
aggregator" playbook as Phase 4's `ShelvingStorageModule`/`ElectricalPowerModule`
addition: added an `instanceof PneumaticEndpointModule` branch to
`MachineBlockEntity.exposedItemHandler` (`content/machine/framework/MachineBlockEntity.java`).
**This required making `PneumaticEndpointModule` `public`** (it was package-private
in the Forge original, and in every other single-block-package helper class in this
port) - `content/machine/framework` and `content/machine/singleblock` now have a
one-directional-in-code-but-bidirectional-in-packages import relationship (legal in
Java; package cycles between two packages of the same mod are not an error). If a
future module type needs the same treatment, check whether it needs to become
`public` too, not just get a `view(side)` method.

### New pattern: hiding every capability on a `MachineBlockEntity` based on runtime state

`SingleBlockMachineBlockEntity` needed two things the generic Phase 2
`MachineCapabilities.register(event, type)` can't express because it always calls
the `final` `MachineBlockEntity.exposedXxxHandler` methods directly: (1) hide
**all three** capability kinds while claimed by an `AdvancedMultiblockBlockEntity`
(the block became a multiblock port; the pre-Phase-4-port-system generic answer
must not shine through), and (2) the steam boiler's bespoke two-tank
`IFluidHandler` (fill routes to the water tank, drain routes to the steam tank -
not a simple per-side aggregate). Since `exposedItemHandler`/`exposedFluidHandler`/
`exposedEnergyStorage` are all `final`, they can't be overridden - the fix is the
same "instance-method wrapper + a dedicated static registration helper" shape
Phase 4 used for `AdvancedMultiblockBlockEntity.itemHandler(pos, side)`:

- `itemHandler(Direction)`/`fluidHandler(Direction)`/`energyStorage(Direction)`
  (package-private instance methods) each check `multiblockController != null`
  first (returning `null` if claimed), then either delegate to the matching
  `exposedXxxHandler` or - for `fluidHandler` on a steam boiler - return the
  `boilerFluidHandler` field, a **stable instance built once in the constructor**
  (no revive/invalidate bookkeeping needed at all, since `primaryTank`/`secondaryTank`
  are `final` and never rebuilt).
- `public static void registerCapabilities(RegisterCapabilitiesEvent event, BlockEntityType<SingleBlockMachineBlockEntity> type)`
  registers all three of the above instead of calling `MachineCapabilities.register`
  directly - **`init/ModBlockEntities` should call this for every single-block
  machine's `BlockEntityType` instead of the plain helper** once it exists.
- `claimForMultiblock`/`releaseMultiblockClaim` (which used to call the now-gone
  `invalidateCaps()`/`reviveCaps()`) now call `level.invalidateCapabilities(worldPosition)`
  directly - the same call `AdvancedMultiblockBlockEntity.refreshCapabilities()`
  makes in Phase 4, needed here because the claim toggle changes what every
  capability query for this position should answer.

`saveTankToItem` (used by the small-tank's `getDrops` override to preserve its
fluid across a break/pickup) was rewritten around vanilla's own
`BlockEntity.saveCustomOnly(HolderLookup.Provider)` (confirmed in decompiled
`BlockEntity.java` - it is what the vanilla convenience method `saveToItem`
itself calls) instead of hand-rolling `saveWithFullMetadata()` +
`tag.remove("id"/"x"/"y"/"z")`: `saveCustomOnly` never writes those metadata keys
in the first place, so the manual removal calls are simply gone, and the result is
attached via `stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag))` (the
Phase 10 pattern). `visualStateHash`'s old `displayItem.getTag()` (gone, see Phase
10) became `displayItem.getComponentsPatch().hashCode()` (`DataComponentPatch` is
a proper value type, never null).

### New cheat-sheet entries this phase (vanilla-version, not Forge/NeoForge - hit any future recipe- or capability-touching file)

- **`RecipeManager.getRecipeFor`/`getAllRecipesFor` need a `RecipeInput`, not a
  `Container`.** Confirmed in decompiled `RecipeManager.java`:
  `<I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T>, I, Level)`.
  For a single-item-slot recipe (the common case for machine recipes), use
  vanilla's `SingleRecipeInput(ItemStack)` instead of `new SimpleContainer(stack)`
  (this was already flagged as the intended replacement back in Phase 4's cheat
  sheet; this phase is the first to actually need it repeatedly -
  `SingleBlockMachineSupport.findSluiceRecipe`/`findGasificationRecipe`/`findSmeltingRecipe`,
  `SingleBlockThermalLogic.tickGasification`/`tickBrickFurnace`).
- **Vanilla crafting recipes (`RecipeType.CRAFTING`) use `CraftingInput`, a
  different `RecipeInput` shape from `SingleRecipeInput`.** `CraftingRecipe extends Recipe<CraftingInput>`
  (confirmed in decompiled `CraftingRecipe.java`). `CraftingContainer`/`TransientCraftingContainer`
  (the GUI-facing 3x3 grid types) still exist unchanged for slot management, but
  no longer implement `RecipeInput` themselves - bridge with the new default method
  `CraftingContainer.asCraftInput()` (confirmed in decompiled `CraftingContainer.java`)
  at every `matches`/`assemble`/`getRemainingItems` call site. See
  `SingleBlockFabricator` for the full worked example (`RecipeType.CRAFTING`,
  `grid.asCraftInput()`, `RecipeHolder<CraftingRecipe>`).
- **`Registry<T>.byNameCodec()`** (a default method, confirmed in decompiled
  `Registry.java`) is the standard `Codec<T>` for "this field is a registry entry
  written as its plain id string" (e.g. `BuiltInRegistries.FLUID.byNameCodec()`,
  `BuiltInRegistries.BLOCK.byNameCodec()`) - use it instead of hand-rolling a
  `ResourceLocation` codec + registry lookup for JSON. For the network side of the
  same field, `ByteBufCodecs.registry(ResourceKey<? extends Registry<T>>)`
  (confirmed in decompiled `ByteBufCodecs.java`, e.g. `ByteBufCodecs.registry(Registries.FLUID)`)
  gives the matching `StreamCodec<RegistryFriendlyByteBuf, T>` - the same building
  block `Recipe.STREAM_CODEC` itself uses for `RecipeSerializer` (`Registries.RECIPE_SERIALIZER`).
  See `FluidFuelRecipe`/`ThermopileRecipe`.
- **`ItemStack.OPTIONAL_CODEC`/`OPTIONAL_STREAM_CODEC` and `FluidStack.OPTIONAL_CODEC`/`OPTIONAL_STREAM_CODEC`**
  (confirmed in decompiled `ItemStack.java`/NeoForge's `FluidStack.java`) are the
  codec pair for "this field may legitimately be empty" (as opposed to the plain
  `CODEC`/`STREAM_CODEC` Phase 4 already used for a required stack) - use them for
  an optional item/fluid output instead of hand-rolling an `Optional<ItemStack>`
  wrapper. See `GasificationRecipe`'s `item_result`/`fluid_result` fields (either
  may be empty as long as not both are).
- **`ByteBufCodecs.map(IntFunction<? extends M> factory, StreamCodec<K>, StreamCodec<V>)`**
  (confirmed in decompiled `ByteBufCodecs.java`) builds a `StreamCodec` for an
  arbitrary `Map<K, V>` the same way `Codec.unboundedMap` does for JSON - used for
  `ThermopileRecipe`'s free-form block-state-property map (`HashMap::new`,
  `ByteBufCodecs.STRING_UTF8` for both key and value).
- **`ForgeHooks.onBlockBreakEvent(Level, GameType, ServerPlayer, BlockPos)` (returned
  `int`, `-1` meant cancelled)** -> `CommonHooks.fireBlockBreak(Level, GameType, ServerPlayer, BlockPos, BlockState)`
  (also takes the target `BlockState`; returns a `BlockEvent.BreakEvent`, check
  `.isCanceled()`). See the `ProtectedWorldMutation` writeup above for the full
  break/place/snapshot rewrite this phase needed.
- **Automation-facing entity inventories** (e.g. a minecart's item handler, probed
  by an inserter) use `Capabilities.ItemHandler.ENTITY_AUTOMATION`
  (`EntityCapability<IItemHandler, @Nullable Direction>`, queried as
  `capability.getCapability(entity, side)`) - **not** `Capabilities.ItemHandler.ENTITY`
  (`@Nullable Void` context, meant for "one combined inventory view", not a
  per-side automation query). Confirmed in decompiled `Capabilities.java`; this is
  the first time this port has queried an entity capability rather than a block or
  item one. See `SingleBlockAutomationLogic.inserterHandlers`.
- **Item fluid-handler capability**: `ForgeCapabilities.FLUID_HANDLER_ITEM`
  (`.getCapability(...).isPresent()`) -> `Capabilities.FluidHandler.ITEM`
  (`ItemCapability<IFluidHandlerItem, @Nullable Void>`, queried as
  `stack.getCapability(Capabilities.FluidHandler.ITEM) != null` - returns the
  handler directly, nullable, not an `Optional`, same shape as Phase 10's
  `Capabilities.EnergyStorage.ITEM`). See `SingleBlockMachineInteractions.interactFluidMachine`.

### Known compile gaps left by this phase (expected, not bugs - do not stub)

Every one of these already existed as an unported dependency before this phase
touched the files that reference them - same policy as always: port the real
class when its subsystem is tackled.

- `init.ModRecipeTypes` - referenced by all 4 `content/machine/singleblock/recipe/`
  files plus `SingleBlockMachineBlockEntity`/`SingleBlockMachineSupport`/
  `SingleBlockThermalLogic`/`SingleBlockElectricalLogic`. Per the existing
  "why `init/` can't be ported yet" writeup, this is now blocked on nothing new
  from this phase - all of its `content/machine/singleblock` dependency is done.
- `init.ModBlockEntities`, `init.ModMachineBlocks`, `init.ModMenus`,
  `init.ModMachineItems`, `init.ModNetworkItems`, `init.ModTags`,
  `init.ModAdvancedBlocks`, `init.ModFluids` - referenced throughout this
  package for concrete registry lookups (block/BE/menu/item types, the wrench
  tag, `MULTIBLOCK_GAP`, the steam/wood-gas fluids). None of the 17 blocked
  `init/*.java` files exist yet, same pre-existing blocker documented throughout
  this file. `init.ModBlockEntities` is also where
  `SingleBlockMachineBlockEntity.registerCapabilities` needs to be wired up per
  concrete `BlockEntityType`, per the new pattern above.

This closes out `content/machine/singleblock` in full - it was the last of the
"content subsystem" alternatives flagged after Phase 9 (`content/nuclear`,
`content/machine/singleblock`, `content/item` - all three are done now).
**Suggested next step**: `content/nuclear` is the highest-leverage remaining
whole-subsystem pick (closes another `content/` subsystem outright, the same way
`content/item` and `content/machine/singleblock` just did); `content/computer` or
`content/worldgen` are the other two still-fully-unported `content/` subsystems if
a change of pace is preferred. Whichever is picked, revisit `init/ModRecipeTypes`,
`init/ModBlocks`, `init/ModItems`, `init/ModMachineBlocks`, `init/ModBlockEntities`
once enough of `content/` exists to satisfy them - `content/machine/singleblock`
being done now means `ModMachineBlocks`/`ModBlockEntities`/`ModMenus` are each one
subsystem closer to portable.

## Phase 12 (DONE): `content/nuclear` - full package, 63/63 files

This session ported **all 63 files** of `content/nuclear` (the PWR reactor,
front-end processing facilities, spent-fuel pools, radiation/PPE, and the
variable-volume steam-generator/condenser/cooling-tower heat chain) - the
last of the three subsystems flagged after Phase 9
(`content/nuclear`/`content/machine/singleblock`/`content/item`, all three
now done). Also ported the one remaining `api/` gap,
`api/nuclear/reactor/NuclearReactorColumnType.java` (skipped in Phase 1
because it depended on `content/nuclear/fuel/NuclearFuelGrade`, which this
session ported) - **`api/` is now 8/8, fully closed**. File-for-file status
by subpackage (`diff` of the full recursive listing between source and
destination is empty, 63 = 63):

| Subpackage | Status | Notes |
|---|---|---|
| root (`NuclearMultiblockBounds`) | 1/1 done | Pure Java, copied verbatim. |
| `material/` | 1/1 done | `NuclearMaterial` - pure enum, copied verbatim. |
| `fuel/` | 3/3 done | `NuclearFuelGrade`, `FuelAssemblyState` copied verbatim; `FuelAssemblyItem` rewritten (see NBT->CustomData pattern below). |
| `facility/` | 10/10 done | Full subsystem: validator, snapshot, recipe (first *multi-slot* recipe input, see below), controller/port blocks+BEs, menu. |
| `radiation/` | 8/8 done | Full subsystem: PPE armor material (full rewrite, see below), protection/meter/decon/cask items, radioactive source block+BE. |
| `reactor/` | 17/17 done | Full subsystem: every enum, the PWR structure validator/snapshot codec/presets, column/port/controller blocks+BE, menu. This is the biggest and most complex file in the whole mod (`NuclearReactorControllerBlockEntity`, ~1300 lines) - ported with zero logic changes, only signature/API rewrites. |
| `spentfuel/` | 9/9 done | Full subsystem: status enum, part/snapshot records, validator, controller/port blocks+BEs, menu. |
| `structure/` | 2/2 done | Shared `FORMED` property holder + base structure-part block, both pure Java. |
| `thermal/` | 12/12 done | Full subsystem: facility type/part/port-role enums, snapshot, validator, main coolant pump block+BE, controller/port blocks+BEs, menu. Steam generator, condenser and wet cooling tower all covered by one shared controller/port design. |

### New pattern: vanilla's `ArmorMaterial` rework (first armor item in this port)

`NuclearProtectionMaterial` (radiation PPE) is the **first and only armor
material in the whole mod**, and vanilla's armor system changed completely
between 1.20.1 and 1.21.1 - confirmed by reading the real decompiled
`ArmorMaterial.java`/`ArmorMaterials.java`/`ArmorItem.java` in
`%TEMP%\vanilla_src\net\minecraft\world\item\`, not guessed:

- The old `IArmorMaterial` **interface** an enum could implement is **gone**.
  `ArmorMaterial` is now a plain `record(Map<ArmorItem.Type,Integer> defense,
  int enchantmentValue, Holder<SoundEvent> equipSound,
  Supplier<Ingredient> repairIngredient, List<ArmorMaterial.Layer> layers,
  float toughness, float knockbackResistance)` **registered into**
  `Registries.ARMOR_MATERIAL` (a normal moddable `BuiltInRegistry`, exactly
  like blocks/items) via a `DeferredRegister<ArmorMaterial>` - added as a new
  `ModRegistries.ARMOR_MATERIALS` field (same precedent as Phase 10's
  `DATA_COMPONENT_TYPES`: extend the already-finished `ModRegistries.java`
  when a content file needs a registry kind it doesn't have yet, and add the
  new bootstrap-forcing call - `NuclearProtectionMaterial.bootstrap()` - to
  `ModRegistries.register()`). `ArmorItem`'s constructor now takes a
  `Holder<ArmorMaterial>`, not the material object directly.
- **Durability moved out of the material entirely.** There is no more
  per-type durability array on the material; `ArmorItem.Type` itself now
  carries a fixed base durability per slot (`HELMET=11, CHESTPLATE=16,
  LEGGINGS=15, BOOTS=13, BODY=16`), and `Item.Properties.durability(...)` at
  *item* registration time is computed as `ArmorItem.Type.getDurability(int
  materialMultiplier)`. `NuclearProtectionMaterial.durabilityMultiplier()`
  exposes that multiplier for `init.ModNuclearItems` (still unported) to use
  once it registers the concrete armor items - this is a **documented,
  expected gap**, not a bug.
- **Texture resolution**: `ArmorMaterial.Layer` takes a `ResourceLocation`
  directly (no more `getName()`-derived path). Both PPE tiers deliberately
  reuse vanilla's own leather/iron texture layers (matching the Forge
  original's trick of returning `"minecraft:leather"`/`"minecraft:iron"`
  from `getName()`) via `new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace("leather"))`
  - no custom armor textures needed.
- See `content/nuclear/radiation/NuclearProtectionMaterial.java` for the full
  worked example. **Any future armor material added to this mod should copy
  this pattern**, not the old Forge one.

### New pattern: a recipe needing more than one input slot

`NuclearProcessRecipe` (nuclear facility processing, up to
`MAX_INGREDIENTS`=4 counted ingredients) is the **first recipe in this port
that needs more than one input slot** - Phase 4's `AdvancedProcessingRecipe`/
`PolymerizerRecipe` only ever needed vanilla's single-slot
`SingleRecipeInput`. Since vanilla ships no multi-slot generic
`RecipeInput`, this file defines its own minimal one as a nested type:
`public record Input(SimpleContainer container) implements RecipeInput`
(just `getItem(int)`/`size()` delegating to the wrapped `SimpleContainer` -
`RecipeInput`'s only two required members, confirmed in the decompiled
interface). Otherwise follows the established Phase 4 codec pattern exactly
(`MapCodec`/`StreamCodec` pair, `RecipeHolder<T>` instead of a
self-carried id). **Any future recipe type needing more than one input slot
should reuse this same `Input` wrapper shape** (or extract it to a shared
location) instead of re-deriving it.

### Retrofit: `RecipeManager.getAllRecipesFor`/`byKey` now return `RecipeHolder<T>`, not `T`

Not previously exercised by this port (Phase 4's two recipe types were never
looked up from a block entity's tick method - only registered). This phase's
`NuclearFacilityControllerBlockEntity.matchingRecipe()`/`activeDuration()`
are the **first live recipe-lookup call sites**, confirmed against the real
decompiled `RecipeManager.java`:
`getAllRecipesFor(RecipeType<T>)` returns `List<RecipeHolder<T>>` (was
`List<T>`), and `byKey(ResourceLocation)` returns `Optional<RecipeHolder<?>>`.
The recipe id used to persist "which recipe is in progress" across ticks
(`activeRecipe: ResourceLocation`) now comes from `RecipeHolder.id()`, not
`Recipe.getId()` (gone, per the existing cheat-sheet entry) - `.value()` gets
the actual `NuclearProcessRecipe`. **Any future block entity that looks up a
recipe by type and remembers which one is active hits this same pattern.**

### Retrofit: three more capability-bearing block entities converted off `LazyOptional`/`invalidateCaps`/`reviveCaps`

`NuclearFacilityPortBlockEntity`, `NuclearReactorPortBlockEntity`, and
`SpentFuelPoolPortBlockEntity` all previously (Forge original) overrode
`getCapability`/`invalidateCaps`/`reviveCaps` for a custom item-forwarding
handler and/or relied on the generic module aggregators for fluid/energy.
All three now follow the **exact same shape** established in Phases 2/4/11:

- A package-private/private `itemHandler(Direction side)` (or, for the
  reactor port, no item forwarding at all - only `exposedFluidHandler`/
  `exposedEnergyStorage`, both already-generic aggregators) returns a
  **stable, pre-built handler instance** built once (not per-query), gated
  by whatever runtime condition used to live in the capability override
  (claimed by a controller, correct outward side, etc.).
- A `public static void registerCapabilities(RegisterCapabilitiesEvent
  event, BlockEntityType<...> type)` helper registers the relevant
  `Capabilities.*.BLOCK` token(s) via `(be, side) -> be.xxxHandler(side)` -
  `init.ModBlockEntities` (still unported) should call these instead of the
  plain `MachineCapabilities.register` for these three BE types, exactly
  like Phase 11 documented for `SingleBlockMachineBlockEntity`.
- Whatever used to call `invalidateCaps()`/`reviveCaps()` (a `claim(...)`/
  `release(...)` pair, in all three cases) now calls
  `level.invalidateCapabilities(worldPosition)` once, guarded by
  `level != null`.

No new pattern here - just three more worked examples of the Phase 2/4
pattern applied to non-`MachineBlockEntity`-owned-module cases. If you find
another `LazyOptional`/`invalidateCaps`/`getCapability(Capability, ...)`
anywhere else in `content/`, it is stale Forge-era code following the same
already-fixed bug, not a new discovery.

### Known compile gaps left by this phase so far (expected, not bugs - do not stub)

Same policy as always - every one of these already existed as an unported
dependency before this phase touched the files that reference them:

- `init.ModNuclearItems`, `init.ModNuclearBlocks` - referenced throughout
  `facility/`, `radiation/`, `reactor/`, `spentfuel/` for concrete
  block/item lookups (`ModNuclearBlocks.controller(type)`,
  `ModNuclearItems.fuelAssemblies()`, `ModNuclearItems.controllerUpgrade(...)`,
  etc.). Both are named explicitly inside the already-ported
  `init/ModRegistries.java`'s `register()` method (forward-declared calls to
  `ModNuclearItems.bootstrap()`/`ModNuclearBlocks.bootstrap()` that don't
  compile yet) - confirming these are the expected names for when `init/` is
  tackled.
- `init.ModBlockEntities` - every concrete BE type in this phase
  (`NUCLEAR_FACILITY_CONTROLLER`, `NUCLEAR_FACILITY_PORT`,
  `NUCLEAR_REACTOR_CONTROLLER`, `NUCLEAR_REACTOR_PORT`,
  `SPENT_FUEL_POOL_CONTROLLER`, `SPENT_FUEL_POOL_PORT`, `RADIOACTIVE_SOURCE`).
  Also where the three new `registerCapabilities(event, type)` helpers above
  need to be wired up, and where `SingleBlockMachineBlockEntity`'s (Phase 11)
  and `AdvancedMultiblockBlockEntity`'s (Phase 4) equivalents are still
  waiting too.
- `init.ModMenus`, `init.ModRecipeTypes` - referenced by every menu class and
  by `NuclearProcessRecipe`/`NuclearFacilityControllerBlockEntity`.
- `content.worldgen.OilDepositBlock` family and `content/computer` - unrelated,
  pre-existing gaps, not touched this phase.

`content/nuclear/thermal/` (the last piece - steam generator / condenser /
cooling tower heat-chain facilities) was finished in the same session:
`MainCoolantPumpBlock` (copy-verbatim) + `MainCoolantPumpBlockEntity`
(fluid/energy imports rewritten, no capability override needed - both its
`FluidTankModule`s and its `ElectricalPowerModule` are already covered by
the generic `exposedFluidHandler`/`exposedEnergyStorage` aggregators, so
`init.ModBlockEntities` can use the plain `MachineCapabilities.register`
helper for this BE type, no custom one needed); `NuclearThermalControllerBlock`
(`NetworkHooks.openScreen` -> `player.openMenu`) + `NuclearThermalControllerBlockEntity`
(`saveMachineData`/`loadMachineData` gained `HolderLookup.Provider`);
`NuclearThermalMenu` (buffer constructor retyped to `RegistryFriendlyByteBuf`);
`NuclearThermalPortBlock` (copy-verbatim) + `NuclearThermalPortBlockEntity`
(same "no capability override existed, just replace
`invalidateCaps()`/`reviveCaps()` with `level.invalidateCapabilities(pos)`"
shape as `MainCoolantPumpBlockEntity` - the plain `MachineCapabilities.register`
helper works for this BE type too).

**`content/nuclear` is now closed in full (63/63)** - see the updated
subpackage table above. `content/computer` (28 files) and
`content/worldgen` (7 files) are the only two fully-unported `content/`
subsystems left; the small never-started machine families
(`content/machine/{windturbine 8, electricfurnace 4, battery 3,
crushingtable 3, observation 3}`) and two loose files
(`content/command`, `content/recipe`) round out the rest of `content/`.
**Suggested next step**: `content/worldgen` (7 files) is the smallest
remaining whole subsystem and also unblocks `init/ModAdvancedBlocks`
(`OilDepositBlock(Item)`, which Phase 4's `AdvancedMultiblockLogic`
pumpjack path has been waiting on since Phase 4); `content/computer` (28
files) is the last big self-contained subsystem if a larger chunk is
preferred instead.

## Phase 13 (DONE): `content/worldgen/` - full package, 7/7 files

Ported all 7 files (`OilDepositBlock`, `OilDepositBlockEntity`,
`OilDepositBlockItem`, `OilDepositPersistence`, `OilDepositSavedData`,
`OilFieldFeature`, `OilFieldSurvey`) - the finite underground oil-deposit
system that `content/multiblock`'s pumpjack logic (Phase 4) has been waiting
on since it was written. File-for-file parity verified (`diff` of the two
directory listings is empty, 7 = 7); no `net.minecraftforge` import existed
in any of the 7 source files to begin with.

Breakdown by risk tier:

- **3 files, zero rewrite** (copied verbatim, byte-identical `diff`):
  `OilDepositBlock` (vanilla `BaseEntityBlock`, `newBlockEntity`/
  `getRenderShape`/`onRemove` signatures cross-checked against the many
  other already-ported `BaseEntityBlock` subclasses in this tree - all
  unchanged), `OilDepositPersistence` (pure NBT int-codec logic, no MC/Forge
  API at all), `OilFieldSurvey` (pure record over `BlockPos`).
- **1 file, vanilla worldgen, zero rewrite**: `OilFieldFeature`. This is the
  **first `Feature<FC>` in this port** - confirmed against the real
  decompiled `Feature.java`/`FeaturePlaceContext.java` in the NeoForge
  sourcesWithNeoForge output zip (not guessed) that `Feature<FC>.place(FeaturePlaceContext<FC>)`,
  `FeaturePlaceContext.level()`/`.origin()`, and `WorldGenLevel.getSeed()`/
  `.getBlockState()`/`.setBlock()`/`.getBlockEntity()` are all unchanged
  from 1.20.1. Vanilla worldgen's `Feature`/`FeaturePlaceContext` API was
  untouched by the Forge->NeoForge transition and by the 1.20.1->1.21.1
  vanilla bump alike.
- **2 files, `HolderLookup.Provider`/`SavedData.Factory` rewrite** (the
  already-established Phase 1/4 pattern, no new discovery): `OilDepositBlockEntity`
  (`saveAdditional`/`load` -> `saveAdditional`/`loadAdditional`, both gaining
  a trailing `HolderLookup.Provider registries` parameter - unused by the
  body since this BE only persists plain ints, but required by the override
  signature) and `OilDepositSavedData` (`save(CompoundTag)` ->
  `save(CompoundTag, HolderLookup.Provider)`, registration switched from the
  old 3-arg `computeIfAbsent(Function, Supplier, String)` to a
  `SavedData.Factory<>(ctor, load, null)` + `computeIfAbsent(factory, key)` -
  copied the exact shape of Phase 1's `LongDistanceElectricitySavedData`).
- **1 file, `ItemStack` NBT-tag rewrite** (the Phase 10 cheat-sheet pattern,
  no new discovery): `OilDepositBlockItem`. The old `stack.addTagElement("BlockEntityTag", tag)`/
  `stack.getTagElement("BlockEntityTag")` calls are gone in this vanilla
  version range; rewritten to `stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag))`
  (write) and `stack.get(DataComponents.BLOCK_ENTITY_DATA)` + `.copyTag()`
  (read), matching the already-ported `content/machine/singleblock/SmallTankBlockItem`
  worked example. `appendHoverText`'s `@Nullable Level` parameter also
  retyped to `Item.TooltipContext` per the existing cheat-sheet row (this
  item never needed `HolderLookup.Provider` from the context, since its
  persisted state is plain ints, not an `ItemStack`/`FluidStack`).

### Known compile gaps left by this phase (expected, not bugs - do not stub)

Both pre-existing, already-documented blockers, not new discoveries:

- `OilDepositBlockEntity`/`OilFieldFeature` reference `init.ModBlockEntities.OIL_DEPOSIT`
  and `init.ModAdvancedBlocks.OIL_DEPOSIT`, neither of which exists yet -
  same `init/` blocker documented throughout this file.
- This phase is itself the dependency `init.ModAdvancedBlocks` (referenced by
  `content/multiblock/MultiblockRule`/`MultiblockStructureSnapshot`/`MultiblockMatcher`/
  `AdvancedMultiblockLogic` since Phase 4) and `content/multiblock/AdvancedMultiblockLogic`'s
  pumpjack path (`OilDepositBlockEntity.EXTRACTION_STAGE_MILLIBUCKETS`/`.remaining()`/`.drain(...)`)
  were waiting on - both are now unblocked structurally; wiring
  `init.ModAdvancedBlocks`'s concrete `OIL_DEPOSIT` block/item registration
  is what actually closes the loop, still pending as part of the 17-file
  blocked `init/` set.

**This closes `content/worldgen/` in full (7/7).** `content/computer` (28
files) is now the only fully-unported `content/` subsystem left of any real
size; the small never-started machine families
(`content/machine/{windturbine 8, electricfurnace 4, battery 3,
crushingtable 3, observation 3}`) and two loose files (`content/command`,
`content/recipe`, 1 each) round out the rest.

## Phase 14 (DONE): `content/computer/` - full package, 28/28 files

Ported all 28 files of `content/computer/` (the bounded-VM computer/mining-robot
subsystem: `vm/` 5, `runtime/` 10, package root 13) - the last sizeable
unstarted `content/` subsystem. File-for-file parity verified (`diff` of the
two directory listings is empty, 28 = 28).

Breakdown by risk tier:

- **23 files, zero rewrite** (pure Java or vanilla-only NBT logic, copied
  verbatim, byte-identical `diff`): all 5 of `vm/` (`BoundedComputerVm`,
  `ComputerDevice`, `ComputerInstruction`, `ComputerOpcode`, `VmFault` - a
  self-contained integer VM with no MC/Forge imports at all), all 10 of
  `runtime/` (`ComputerDeviceBus`, `ForthEngine`, `LispEngine`, `ScriptEngine`,
  `ScriptLanguage`, `ScriptOutput`, `ScriptProgram`, `ScriptRuntime`,
  `ShellEngine`, `VirtualDisk` - the Forth/Lisp/shell interpreters and their
  own `ScriptEngine.saveState()/restoreState(CompoundTag)` codec, unrelated to
  any vanilla persistence hook so it never needed `HolderLookup.Provider`),
  and 8 of the package root (`ComputerBlock`, `ComputerBlockEntity`,
  `ComputerProgramText`, `FloppyDiskPersistence`, `FloppyDiskVisualVariant`,
  `MiningRobotBlock`, `ProgramNbt`, `RobotQuarryTask`).
- **2 files, `ItemStack` NBT-tag rewrite** (the Phase 10/13 `CustomData`
  pattern, no new discovery): `FloppyDiskItem` (`stack.getOrCreateTag()`/
  `.getTag()` -> `CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> ...)`
  for writes and `stack.get(DataComponents.CUSTOM_DATA)` + `.copyTag()` for
  reads, matching `content/item/CopperWireCoilItem`'s worked example;
  `appendHoverText`'s `@Nullable Level` retyped to `Item.TooltipContext`).
- **1 file, menu-opening + items-package rewrite**: `ProgrammableBlock`
  (`NetworkHooks.openScreen(player, provider, writer)` ->
  `player.openMenu(provider, writer)`, per the established Phase 4 pattern).
- **1 file, `HolderLookup.Provider` + menu-opening-buffer rewrite**:
  `ProgrammableBlockEntity` - its `saveMachineData`/`loadMachineData`/
  `resetMachineData`/`saveClientData`/`loadClientData` overrides all gained
  the trailing `HolderLookup.Provider registries` parameter now required by
  the already-ported `MachineBlockEntity` (Phase 2); its own
  `saveProgrammableData(CompoundTag)`/`loadProgrammableData(CompoundTag)`
  extension points (not a vanilla override, just this class's own hook) were
  left without the parameter since neither `MiningRobotBlockEntity`'s
  override persists an `ItemStack`/`FluidStack`. `writeMenuOpeningData`'s
  buffer parameter retyped `FriendlyByteBuf` -> `RegistryFriendlyByteBuf`
  (Phase 4 pattern) to match `player.openMenu`'s `Consumer<RegistryFriendlyByteBuf>`.
- **1 file, `items` package rename + buffer retype**: `ProgrammableMenu`
  (`net.minecraftforge.items.*` -> `net.neoforged.neoforge.items.*`;
  its network-opening constructor's `FriendlyByteBuf` -> `RegistryFriendlyByteBuf`,
  matching `content/network/pressure/PressureTankMenu`'s worked example).
- **1 file, the full Forge-hooks + items-package rewrite** (the hard core of
  this phase): `MiningRobotBlockEntity`. Confirmed against the already-ported
  `content/world/ProtectedWorldMutation` (Phase 11), which independently
  solved the exact same three Forge-hook renames for its own owner-operated
  block-mutation logic:
  - `net.minecraftforge.common.ForgeHooks.onBlockBreakEvent(level, gameType, player, pos)`
    (returned `int`, `-1` meant cancelled) -> `net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(level, gameType, player, pos, state)`
    (returns a `BlockEvent.BreakEvent`; check `.isCanceled()`). The new call
    also requires the target `BlockState` as a fifth argument, which the
    caller already had in hand.
  - `net.minecraftforge.event.ForgeEventFactory.onBlockPlace(entity, snapshot, direction)`
    -> `net.neoforged.neoforge.event.EventHooks.onBlockPlace(entity, snapshot, direction)`
    - pure rename, same boolean return.
  - `net.minecraftforge.common.util.BlockSnapshot` -> `net.neoforged.neoforge.common.util.BlockSnapshot`
    - pure package rename, but `restore(boolean force, boolean notify)` is
    gone; the replacement is `restore(int flags)` (confirmed in the sources
    jar) - `replacedBlock.restore(true, true)` became
    `replacedBlock.restore(Block.UPDATE_ALL)`, the same flag
    `ProtectedWorldMutation` already uses for its own restore call.
  - `net.minecraftforge.common.util.FakePlayer` -> `net.neoforged.neoforge.common.util.FakePlayer`
    (already in the cheat sheet).
  - `net.minecraftforge.items.{IItemHandlerModifiable,ItemHandlerHelper,ItemStackHandler}`
    -> `net.neoforged.neoforge.items.*` (pure rename, per the cheat sheet).

  Separately (a vanilla-only change, not a Forge/NeoForge one): the
  robot-relocation code used to call the removed no-arg `BlockEntity.load(CompoundTag)`/
  `saveWithoutMetadata()` pair to clone one block entity's full durable state
  onto the freshly placed one at the target position. The vanilla replacement
  pair is `saveWithoutMetadata(HolderLookup.Provider)` (unchanged name, now
  takes the registries) and `loadWithComponents(CompoundTag, HolderLookup.Provider)`
  (the public entry point that calls `loadAdditional` *and* restores the
  component map - confirmed in the decompiled `BlockEntity.java`; the
  alternative `loadCustomOnly` skips components, which is not appropriate for
  a full clone). Both threaded through `level.registryAccess()`, which itself
  implements `HolderLookup.Provider`. **This is the first block-entity-to-
  block-entity full-state relocation in this port** - any future "teleport a
  block entity by copying its saved tag" logic should reuse
  `saveWithoutMetadata(registries)` / `loadWithComponents(tag, registries)`,
  not the old two-method vanilla pair.

### Known compile gaps left by this phase (expected, not bugs - do not stub)

Same policy as always - every one of these already existed as an unported
dependency before this phase touched the files that reference them:

- `init.ModComputerContent` - referenced by `ComputerBlock`, `ComputerBlockEntity`,
  `MiningRobotBlock`, `MiningRobotBlockEntity` for the two concrete
  `BlockEntityType`s. Not yet named in `init/ModRegistries.java`'s
  forward-declared bootstrap calls (unlike `ModNuclearItems`/`ModNuclearBlocks`
  in Phase 12) - add it there when `init/` is tackled.
- `init.ModMenus` - referenced by `ProgrammableMenu` (`ModMenus.programmable(boolean)`).
  Part of the 17-file blocked `init/` set.

**This closes `content/computer/` in full (28/28).** Every `content/`
subsystem that has ever been started is now fully closed. The only remaining
`content/` work is the small standalone machine families that were never
started at all: `content/machine/{windturbine 8, electricfurnace 4, battery 3,
crushingtable 3, observation 3}` and two loose files (`content/command`,
`content/recipe`, 1 each) - 24 files total, none of them large enough to need
their own phase.

## Phase 15 (DONE): the last 23 `content/` files - `content/` CLOSED IN FULL

Ported the small standalone machine families that were never started, closing
`content/` entirely (file-for-file parity verified: `diff` of the full
recursive `content/` listing between source and destination now shows only
the two already-documented *new* files this port added, `PortableEnergyComponents.java`
(Phase 10) and `MachineCapabilities.java` (Phase 2) - nothing from the
original 303-file source tree is missing).

- **`content/command/MagneticraftCommands.java`** (1 file) - event-bus rewrite:
  `net.minecraftforge.event.RegisterCommandsEvent` -> `net.neoforged.neoforge.event.RegisterCommandsEvent`
  (confirmed in the sources jar - `getDispatcher()` unchanged, fired on
  `NeoForge.EVENT_BUS`), `@Mod.EventBusSubscriber(bus = Bus.FORGE)` -> top-level
  `@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)` (established pattern).
- **`content/recipe/TieredShapedRecipe.java`** (1 file) - the second
  codec-based `RecipeSerializer` in this port (after Phase 4's
  `AdvancedProcessingRecipe`/`PolymerizerRecipe`). `CraftingRecipe` now
  extends `Recipe<CraftingInput>` (confirmed in decompiled vanilla source:
  `matches(CraftingInput, Level)`, `assemble(CraftingInput, HolderLookup.Provider)`,
  `getResultItem(HolderLookup.Provider)`, no more `getId()`). The nested nowrap
  pattern - embedding vanilla's own `ShapedRecipe.Serializer.CODEC`/`STREAM_CODEC`
  as one field of a wrapping `MapCodec`/`StreamCodec` via `.forGetter(...)` -
  mirrors vanilla `ShapedRecipe.Serializer.CODEC`'s own use of
  `ShapedRecipePattern.MAP_CODEC.forGetter(...)`, confirmed by reading vanilla's
  actual decompiled `ShapedRecipe.java`. Added a **new, separate** codec pair
  (`TieredElectricalItemData.CODEC`/`STREAM_CODEC`, `system/network/electric/item/TieredElectricalItemData.java`)
  alongside its existing NBT (`toTag`/`fromTag`, versioned via `SCHEMA_VERSION`)
  and old-Forge-JSON (`toJson`/`fromJson`) methods - deliberately not reusing
  those, since (per Phase 4's established policy) recipe JSON/network shapes
  are new for this port, not preserved from the Forge 1.20.1 originals.
- **`content/machine/battery/`** (3 files) - the first **item capability
  consumer** in this port (every earlier item-capability file, Phase 10's
  `PortableEnergyItem`, was only a *provider*). Confirmed by reading
  `net.neoforged.neoforge.capabilities.ItemCapability`'s own javadoc plus
  `IItemStackExtension.java` in the sources jar: unlike the Forge original's
  `stack.getCapability(ForgeCapabilities.ENERGY)` returning `LazyOptional<IEnergyStorage>`
  (used via `.map(...).orElse(...)`), the NeoForge replacement
  `stack.getCapability(Capabilities.EnergyStorage.ITEM)` returns a **plain
  `@Nullable IEnergyStorage` directly** - no `Optional`/`LazyOptional` wrapper
  at all. `BatteryBlockEntity.chargeItem`/`dischargeItem`/`isValidCell` were
  rewritten from `.map(...).orElse(...)` chains to plain null checks
  accordingly. **Any future file that queries an item's capability (as
  opposed to providing one) will hit this same nullable-not-Optional shape.**
  Also: `BatteryBlock`'s `NetworkHooks.openScreen(serverPlayer, battery, position)`
  became `serverPlayer.openMenu(battery, position)` - confirmed a
  `(MenuProvider, BlockPos)` overload exists as a default method on
  `net.neoforged.neoforge.common.extensions.IPlayerExtension` (which
  `ServerPlayer` implements) that auto-writes the position via
  `buf.writeBlockPos(pos)`, so this is a clean one-argument-shorter drop-in,
  not the 2-arg `(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` overload
  used elsewhere in this port. `BatteryMenu` was a plain `items` package
  rename.
- **`content/machine/crushingtable/`** (3 files) - `CrushingTableBlock` copied
  verbatim (zero Forge imports). `CrushingRecipe` rewritten like
  `content/machine/electricfurnace`'s process module below: `Recipe<SimpleContainer>`
  -> `Recipe<SingleRecipeInput>` (single-item-slot recipe, per the cheat
  sheet), `getId()` dropped, codec-based `RecipeSerializer` (`Ingredient.CODEC`,
  `ItemStack.STRICT_CODEC`, `Codec.INT.optionalFieldOf` for `required_level`).
  `CrushingTableBlockEntity` picked up the full `HolderLookup.Provider`
  persistence thread (`saveMachineData`/`loadMachineData`/`saveClientData`/`loadClientData`
  now all take a trailing `registries` param; `ItemStack.of(tag)` ->
  `ItemStack.parseOptional(registries, tag)`), `ItemStack.isSameItemSameTags`
  -> `isSameItemSameComponents`, and `held.hurtAndBreak(cost, player, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND))`
  -> `held.hurtAndBreak(cost, player, EquipmentSlot.MAINHAND)` (the removed-lambda
  form from the cheat sheet), plus `RecipeManager.getRecipeFor(...)` now
  returning `Optional<RecipeHolder<CrushingRecipe>>` (added `.value()` at both
  call sites).
- **`content/machine/electricfurnace/`** (4 files) - `ElectricFurnaceBlock`:
  same `NetworkHooks.openScreen` -> `serverPlayer.openMenu(furnace, position)`
  rewrite as battery above. `ElectricFurnaceBlockEntity` copied verbatim (zero
  Forge imports - it was already vanilla-clean in the Forge source).
  `ElectricFurnaceMenu`: plain `items` package rename.
  `ElectricFurnaceProcessModule` is the first file in this port to consume
  **vanilla's own** `RecipeType.SMELTING`/`AbstractCookingRecipe` (as opposed
  to a mod-defined recipe type): `SimpleContainer` -> `SingleRecipeInput`
  (confirmed `AbstractCookingRecipe implements Recipe<SingleRecipeInput>` in
  the decompiled vanilla source), `getRecipeFor(...)` result changed from
  `Optional<? extends AbstractCookingRecipe>` to
  `Optional<RecipeHolder<? extends AbstractCookingRecipe>>` (the wildcard
  covariance means no cast is needed despite `RecipeType.SMELTING` being
  concretely `RecipeType<SmeltingRecipe>`), `.value()` added at both use
  sites, `ItemStack.isSameItemSameTags` -> `isSameItemSameComponents`, and
  `MachineModule.load`/`save` gained the `registries` parameter (unused by
  this module's body, since it persists only primitives).
- **`content/machine/windturbine/`** (8 files) - the largest remaining
  family. `LegacyWindNoise`, `WindTurbineMath`, `WindTurbineRotorTier`,
  `WindTurbineRotorItem` are pure Java/vanilla-only, copied verbatim.
  `WindTurbineBlockEntity` copied verbatim (zero Forge imports - already
  vanilla-clean). `WindTurbineBlock`: same `openMenu(turbine, position)`
  rewrite. `WindTurbineMenu`: plain `items` package rename.
  `WindTurbineModule`: `MachineModule.load`/`save`/`saveClientData`/`loadClientData`
  all gained the `registries` parameter (unused by this module's body, same
  as the electric-furnace process module above) - `onLoad()` stays
  parameterless, confirmed against the current `MachineModule` interface.
- **`content/machine/observation/`** (3 files) - `MachineObservation`
  (records) and `MachineObservationCodec` (NBT-only) are pure vanilla, copied
  verbatim. `MachineObservationService` was the one file in this whole phase
  that depended on every other family above (`battery`, `electricfurnace`,
  `windturbine`, plus already-ported `singleblock`/`AdvancedMultiblockBlockEntity`/
  computer's `MiningRobotBlockEntity`) - ported last, once they all existed.
  Only rewrite needed: `net.minecraftforge.fluids.FluidStack` ->
  `net.neoforged.neoforge.fluids.FluidStack` and
  `ForgeRegistries.FLUIDS.getKey(fluid)` -> `BuiltInRegistries.FLUID.getKey(fluid)`
  (a plain package/class rename here - unlike the defaulted-registry `get(id)`
  direction flagged elsewhere in the cheat sheet, `getKey(value)` was always
  `@Nullable` on both sides, so no behavior change).

**`content/` is now CLOSED IN FULL - every one of the original 303 main-source
files under `content/` has been ported.** Combined with `system/` (98/98),
the root `network/` package (6/6), and `api/` (8/8) already being closed, the
only work left in the entire port is `client/` (53), `data/` (24), `init/`
(17 of 19 - now structurally unblocked for everything, since every
`content/` dependency any `init/` file will ever need now exists), and
`integration/` (17, optional/last). **Suggested next step**: `init/` -
specifically `ModBlockEntities` first (it owns the capability-registration
wiring that Phase 4/11/12 left as documented gaps for
`AdvancedMultiblockBlockEntity`/`MultiblockGapBlockEntity`/
`SingleBlockMachineBlockEntity`/the nuclear BEs/`ModComputerContent`'s BEs,
and now also battery/electric-furnace/crushing-table/wind-turbine's simple
BEs), then `ModBlocks`/`ModItems`/`ModMachineBlocks`/`ModNetworkBlocks`/
`ModAdvancedBlocks`/`ModAdvancedItems`/`ModAdvancedItemGroups` and the rest of
the 17-file set, then `ModRecipeTypes` (needed by nearly everything above -
every recipe type this file forward-declares now has a real class to point
at). Once `init/` compiles conceptually, `client/` becomes unblocked.

## Phase 16 (DONE): `init/` - full package, 17/17 remaining files - `init/` CLOSED IN FULL

Ported all 17 remaining `init/*.java` files (`ModRegistries`/`ModSounds` were
already done in Phase 2): `ModFeatures`, `ModTags`, `ModFluids`,
`ModComputerContent`, `ModNetworkItems`, `ModMachineItems`, `ModNuclearItems`,
`ModItems`, `ModBlocks`, `ModMachineBlocks`, `ModNetworkBlocks`,
`ModNuclearBlocks`, `ModAdvancedBlocks`, `ModBlockEntities`, `ModMenus`,
`ModRecipeTypes`, `ModCreativeTabs`. File-for-file parity verified (`diff` of
the two directory listings is empty, 19 = 19) and a full-tree grep for
`net.minecraftforge`/`RegistryObject`/`IForgeMenuType`/`ForgeCapabilities`/
`ForgeFlowingFluid`/`NetworkHooks` across the *entire* destination
`src/main/java` tree (not just `init/`) came back empty - every remaining
Forge reference in the whole port is gone.

Most of these 17 files were a mechanical, low-risk rename
(`net.minecraftforge.registries.RegistryObject<T>` -> `net.neoforged.neoforge.registries.DeferredHolder<T, T>`,
done with `sed` once the pattern was confirmed) since their logic is pure
registration wiring with no other Forge dependency. The real discoveries
were concentrated in a handful of files:

1. **`StairBlock`'s constructor lost its `Supplier<BlockState>` overload.**
   Confirmed in the decompiled vanilla source: 1.21.1's
   `StairBlock(BlockState, BlockBehaviour.Properties)` takes the base state
   directly, not `Supplier<BlockState>` as in 1.20.1. `ModBlocks.java`'s
   `new StairBlock(base.get()::defaultBlockState, ...)` became
   `new StairBlock(base.get().defaultBlockState(), ...)` - safe because this
   call already lives inside a lazy `Supplier<Block>` registration lambda, so
   evaluation timing (after `base` has registered) is unchanged.
2. **`net.minecraftforge.fluids.ForgeFlowingFluid` -> `net.neoforged.neoforge.fluids.BaseFlowingFluid`.**
   Confirmed in the sources jar: identical shape (`Flowing`/`Source` nested
   classes, `Properties(Supplier<FluidType>, Supplier<Fluid> still, Supplier<Fluid> flowing)`
   builder with `.block(...)`/`.bucket(...)`), just renamed and repackaged -
   a clean drop-in for `ModFluids.java`.
3. **NeoForge's common cross-loader tag convention moved from Forge's `forge:`
   namespace to `c:`.** Confirmed by reading `net.neoforged.neoforge.common.Tags`
   in the sources jar: e.g. `Tags.Items.INGOTS_COPPER` resolves to
   `c:ingots/copper`. Path segments are otherwise unchanged (`ores/x`,
   `storage_blocks/x`, `ingots/x`, ...) **except the wrench tool tag, which is
   now singular** (`c:tools/wrench`, matching `Tags.Items.TOOLS_WRENCH`) where
   the Forge original used plural `forge:tools/wrenches`. `ModTags.java`'s
   private `forge(path)` helpers were renamed `common(path)` and repointed at
   `c:`; the one public method that changed name (`ModTags.Fluids.forge(FluidDefinition)`
   -> `.common(FluidDefinition)`) has exactly one call site in the whole
   source tree, `data/ModFluidTagsProvider.java` (not ported yet) - update
   that call when `data/` is tackled.
4. **The first item-capability *consumer* in `init/`** (`ModCreativeTabs.acceptMachineItem`,
   charging a creative-tab preview stack): same nullable-not-`Optional` shape
   as Phase 15's `BatteryBlockEntity` discovery -
   `charged.getCapability(Capabilities.EnergyStorage.ITEM)` returns
   `@Nullable IEnergyStorage` directly, no `.ifPresent(...)` chain. Also hit
   here: `ItemStack.getOrCreateTagElement(String)` (vanilla, removed with the
   rest of the old NBT-tag API) -> `CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(key, subTag))`,
   the same pattern as `TieredElectricalItemData.write`.
5. **`net.minecraftforge.common.extensions.IForgeMenuType.create(factory)` ->
   `net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(factory)`.**
   Confirmed in the sources jar: identical static factory signature
   (`<T extends AbstractContainerMenu> MenuType<T> create(IContainerFactory<T> factory)`).
   Used throughout `ModMenus.java`.
6. **`RegisterCapabilitiesEvent` wiring finally lands.** Every
   `MachineCapabilities.register(event, TYPE.get())` call the last several
   phases deferred as a "documented gap" is now wired up for real: added
   `ModBlockEntities.registerCapabilities(RegisterCapabilitiesEvent)` (covers
   every simple `MachineBlockEntity` type registered in that file, plus the
   `SINGLE_BLOCK_MACHINES` map) and `ModComputerContent.registerCapabilities(...)`
   (covers `COMPUTER_BLOCK_ENTITY`/`MINING_ROBOT_BLOCK_ENTITY`), both
   subscribed via `modBus.addListener(...)` from `ModRegistries.register(modBus)`
   (confirmed `RegisterCapabilitiesEvent implements IModBusEvent`, so it's
   fired on the mod bus, same as every other registration event in this
   file). `AdvancedMultiblockBlockEntity`'s position-dependent
   `itemHandler(BlockPos, Direction)`/`fluidHandler(BlockPos, Direction)`/
   `energyStorage(Direction)` and `MultiblockGapBlockEntity`'s
   `itemHandler(Direction)`/`fluidHandler(Direction)` are registered with
   explicit lambdas instead of the generic helper, per the Phase 4 writeup.
7. **Extended `MachineBlockEntity`'s generic aggregators one more time** (the
   same treatment Phase 4 gave `ShelvingStorageModule`/`ElectricalPowerModule`):
   `exposedItemHandler` now also recognizes `LogisticsTubeModule`/`ConveyorBeltModule`,
   and `exposedFluidHandler` now also recognizes `PressureNetworkModule`/`FluidPipeModule`.
   Without this, registering `MachineCapabilities.register` for
   `PressureTankBlockEntity`/`BrassPressurePipeBlockEntity`/`PneumaticTubeBlockEntity`/
   `IronPipeBlockEntity`/`ConveyorBeltBlockEntity` (all real `MachineBlockEntity`
   subclasses via `NetworkComponentBlockEntity`, confirmed in Phase 5) would
   have silently exposed no capability at all despite each already having a
   working `view(side)` method on its owned module (Phase 3). **If a future
   module type needs block-capability exposure and isn't recognized by
   `exposedItemHandler`/`exposedFluidHandler`/`exposedEnergyStorage`, this is
   the same bug** - extend the aggregator, don't special-case the block
   entity.
8. **Bug caught and fixed in this session's own earlier `content/machine/{battery,electricfurnace,windturbine}`
   work (Phase 15)**: `BatteryMenu`/`ElectricFurnaceMenu`/`WindTurbineMenu`'s
   network-opening constructors were left typed `FriendlyByteBuf buffer`
   instead of `RegistryFriendlyByteBuf buffer`. Both compile against
   `IContainerFactory<T>.create(int, Inventory, RegistryFriendlyByteBuf)` by
   contravariance, but every other menu in this port (`PressureTankMenu`,
   `ProgrammableMenu`, ...) already committed to the precise
   `RegistryFriendlyByteBuf` type - fixed to match for consistency, caught by
   re-reading `ModMenus.java`'s `IContainerFactory` requirement while porting
   it. **If you spot a menu's buffer-constructor still typed plain
   `FriendlyByteBuf`, it's the same drift, not a new pattern.**

### Known compile gaps left by this phase (expected, not bugs - do not stub)

- `data/ModFluidTagsProvider.java`'s single call to `ModTags.Fluids.forge(...)`
  needs to become `ModTags.Fluids.common(...)` when `data/` is tackled (see
  discovery 3 above) - not a gap exactly, just a rename to remember.

**`init/` is now CLOSED IN FULL (19/19).** Combined with `content/` (Phase 15),
`system/`, `api/`, and the root `network/` package all already closed, the
only work left in the entire port is `client/` (53) and `data/` (24), plus
`integration/` (17, optional/last). **Suggested next step**: `data/`
(datagen) can now be tackled productively for the first time - every
registry object it will reference now exists. Recipe-related datagen
providers additionally need the codec-based `RecipeSerializer` JSON shape
established in Phase 4/11/15 (and note Phase 4's caveat: the JSON shapes for
`AdvancedProcessingRecipe`/`PolymerizerRecipe`/`TieredShapedRecipe`/
`CrushingRecipe` are new for this port, not preserved from the Forge 1.20.1
originals). Alternatively, `client/` can be started (renderers/screens/models)
since every `content/` block/item/block-entity type it needs now exists too -
it has no dependency on `data/` being done first.

## Phase 17 (DONE): `data/` - 21/24 files, 2 blocked on `client/model/`

This session ported `data/` (datagen), the suggested next step from Phase 16.
**This was the single biggest rewrite in the whole port so far, bigger than
the Phase 2 capability rewrite** - not because of Forge->NeoForge renames
(there were few), but because vanilla's own recipe-datagen API was
completely redesigned in the same 1.21.x window as the `Recipe`/
`RecipeSerializer` rewrite documented in Phase 4's cheat-sheet entry, and
nothing before this phase had touched recipe *datagen* (only recipe
*consumption*, e.g. `AdvancedMultiblockLogic`). Verified against the actual
vanilla 1.21.1 decompiled sources (`net/minecraft/data/recipes/*.java`) in
`sourcesWithNeoForge_e2d4a39d745d7ca198b2ada7812a435b5013a4f9_output.zip`
(the newest of six `sourcesWithNeoForge_*` zips in the local Gradle cache -
picked by timestamp), not guessed.

### The big discovery: `Consumer<FinishedRecipe>` is gone, replaced by `RecipeOutput`

`net.minecraft.data.recipes.FinishedRecipe` (the JSON-serializing interface
every recipe datagen builder used to implement/consume) **no longer
exists**. The whole shape changed to match the Phase 4 `Recipe`/
`RecipeSerializer` codec rewrite:

- Every vanilla recipe builder (`ShapedRecipeBuilder`, `ShapelessRecipeBuilder`,
  `SimpleCookingRecipeBuilder`, `SingleItemRecipeBuilder`, ...) now ends in
  `.save(RecipeOutput recipeOutput, ResourceLocation id)` instead of
  `.save(Consumer<FinishedRecipe> consumer, ResourceLocation id)`. Every
  other builder chain call (`.pattern(...)`, `.define(...)`, `.unlockedBy(...)`,
  `.requires(...)`) is **unchanged** - only the type fed into `.save(...)`
  changed, and it's fed through the exact same call sites, so **the fix for
  every mechanical `.save(consumer, id(...))` call site in a datagen provider
  is a pure type retype of the `consumer`/`output` parameter from
  `Consumer<FinishedRecipe>` to `RecipeOutput`, zero call-site rewrites
  needed** - this is what made porting the 3076-line `ModRecipeProvider.java`
  tractable instead of a line-by-line rewrite.
- `unlockedBy(String, CriterionTriggerInstance)` -> `unlockedBy(String,
  Criterion<?>)`. `RecipeProvider`'s static `has(ItemLike)`/`has(TagKey<Item>)`
  helpers already return `Criterion<InventoryChangeTrigger.TriggerInstance>`
  (a `Criterion<?>`), so **every `.unlockedBy("x", has(...))` call site is
  also unchanged** - only a bare `CriterionTriggerInstance` used as a
  *parameter type* in a private helper method needs retyping to `Criterion<?>`
  (two sites in `ModRecipeProvider`: `smelt(...)`/`tieredLine(...)`).
- `RecipeOutput.accept(ResourceLocation id, Recipe<?> recipe, @Nullable
  AdvancementHolder advancement)` (a default method, 4-arg
  `..., ICondition... conditions)` is the real abstract one, from
  `net.neoforged.neoforge.common.extensions.IRecipeOutputExtension` which
  `RecipeOutput` extends) is the new terminal call for **any custom recipe
  type's datagen** - it takes the actual `Recipe<?>` object directly, not a
  JSON-serializing wrapper. This is a much bigger change than a Forge/NeoForge
  rename: **every custom `FinishedRecipe`-implementing datagen record/class in
  this codebase had to be rewritten to construct the real ported `Recipe<T>`
  object** (from Phase 4/11/12/15's already-ported `content/` recipe classes)
  **and call `recipeOutput.accept(id, recipeObject, null)`** (null
  advancement - none of Magneticraft's own recipe types ever populated one).
  Hit for: `content/machine/crushingtable/CrushingRecipe`,
  `content/machine/singleblock/recipe/{SluiceRecipe,GasificationRecipe,
  ThermopileRecipe,FluidFuelRecipe}`, `content/nuclear/facility/NuclearProcessRecipe`,
  `content/multiblock/recipe/{AdvancedProcessingRecipe,PolymerizerRecipe}` -
  every one of these already had a real constructor from its Phase
  4/11/12/15 porting, so this was a mechanical "call the real constructor,
  drop the `id` argument (recipes don't carry ids anymore, per Phase 4)"
  change once each constructor's exact field order was re-read.
- **The `TieredShapedFinishedRecipe` "wrap a vanilla shaped recipe with extra
  JSON" pattern needed a different fix**, since there's no way to get the
  `ShapedRecipe` object out of a `ShapedRecipeBuilder` except by letting it
  call `.save(...)` itself (`ShapedRecipeBuilder.save` builds the `ShapedRecipe`
  internally and calls `recipeOutput.accept(...)` - there's no `.build()`
  escape hatch). Fixed by having `TieredShapedFinishedRecipe.save(builder,
  recipeOutput, id, itemData)` hand the builder a **wrapping `RecipeOutput`**
  (a small `record Intercept(RecipeOutput delegate, TieredElectricalItemData
  itemData) implements RecipeOutput`) whose `accept(id, recipe, advancement,
  conditions)` casts the incoming `recipe` to `ShapedRecipe`, wraps it in
  `new TieredShapedRecipe(shapedRecipe, itemData)` (the already-ported Phase
  15 wrapper class), and forwards to the real `recipeOutput`. **This
  intercepting-`RecipeOutput` pattern is the general fix for "I need to
  post-process the `Recipe` object a vanilla builder produces before it's
  written"** - reuse it if another wrapped-recipe datagen need comes up.
- `net.minecraft.advancements.CriterionTriggerInstance` type is gone (folded
  into `Criterion<T>`); `net.minecraft.data.recipes.FinishedRecipe` import
  simply deleted.

### Second discovery: vanilla's own cooking-recipe builder now supports a full `ItemStack` result (with count)

Forge 1.20.1's `data/recipe/CountedCookingRecipeBuilder.java` existed only to
work around vanilla's `SimpleCookingRecipeBuilder` not supporting a result
count > 1. **In 1.21.1 vanilla's own `SimpleCookingRecipeBuilder.smelting`/
`.blasting`/`.smoking`/`.campfireCooking` gained `ItemStack`-result overloads**
(alongside the original `ItemLike`-result ones) - confirmed in the decompiled
source. `CountedCookingRecipeBuilder` is now entirely redundant; **deleted,
not ported** - `ModRecipeProvider`'s two call sites (`smelt`/the one-off
brass-dust blasting recipe) now call
`SimpleCookingRecipeBuilder.smelting(ingredient, RecipeCategory.MISC, new
ItemStack(result, count), experience, cookingTime)` directly. If you spot
another Forge-original datagen helper class that exists purely to add a
missing vanilla feature, check whether vanilla 1.21.1 already grew that
feature natively before porting the helper.

### Third discovery: `PresetFloppyRecipe`'s NBT-tag result also has a clean modern replacement

The Forge original's `PresetFloppyRecipe` (custom `FinishedRecipe`) manually
wrote a shaped-recipe JSON with a `"result": {"item":..., "nbt": "..."}`
field - the old Forge/vanilla NBT-tag-on-result-item mechanism. Since
`ItemStack.getTag()`/NBT is gone (Phase 10 cheat-sheet entry -
`DataComponents.CUSTOM_DATA` replaces it) and `ShapedRecipeBuilder` now has a
`shaped(RecipeCategory, ItemStack result)` overload that carries the full
result stack (components included) through to the real `ShapedRecipe`, the
fix is much simpler than the original: build the floppy `ItemStack`, call
`FloppyDiskItem.configurePreset(stack, preset, language)` on it (already
ported in Phase 14 to use `CustomData.update(...)`, confirmed still works
here), then `ShapedRecipeBuilder.shaped(RecipeCategory.MISC, thatStack)...
.save(...)` - no custom `FinishedRecipe` needed at all anymore. **One new
constraint discovered**: the new `ShapedRecipeBuilder`/`SimpleCookingRecipeBuilder`
`.save()` always builds a real advancement and throws
`IllegalStateException` if zero `.unlockedBy(...)` criteria were added
(`ensureValid`) - the Forge original's `PresetFloppyRecipe` had **no**
unlock criterion at all (`serializeAdvancement()` returned `null`), which is
no longer legal; added a reasonable one (`has(ModComputerContent.FLOPPY_DISK.get())`).

### Fourth discovery: three more `net.neoforged.neoforge.common.data` datagen base classes gained a `HolderLookup.Provider` (or its `CompletableFuture` wrapper) constructor parameter

Beyond the already-documented `LootTableProvider`/tags-provider pattern:
`net.neoforged.neoforge.common.data.SpriteSourceProvider`'s constructor
gained a `CompletableFuture<HolderLookup.Provider> lookupProvider` parameter
(it now extends `JsonCodecProvider<List<SpriteSource>>`, which threads
registries through for its conditional-JSON encoding) - confirmed in the
sources jar. **Also: its extension point is no longer `protected void
addSources()` - that method doesn't exist anymore.** The abstract method to
override is `protected void gather()` (inherited from `JsonCodecProvider`),
with the exact same body (`atlas(...).addSource(...)` calls) - a pure rename
of the override point, not a behavior change.

### Fifth discovery: four more NeoForge `Tags.Items`/`Tags.Blocks` names use the plural convention, not just the `c:`-namespace rename from Phase 16

Beyond the `forge:` -> `c:` namespace change and the singular wrench-tag
rename already in the cheat sheet, **four more tag constants were renamed to
plurals** in the same NeoForge `Tags` class (confirmed in the sources jar,
`net/neoforged/neoforge/common/Tags.java`): `Tags.Items.STONE` ->
`Tags.Items.STONES`, `Tags.Items.GLASS` -> `Tags.Items.GLASS_BLOCKS` (not
`GLASS_PANES`, which already existed unchanged as its own tag),
`Tags.Items.STRING` -> `Tags.Items.STRINGS`, `Tags.Items.COBBLESTONE` ->
`Tags.Items.COBBLESTONES`. Hit 17 call sites across `ModRecipeProvider.java`
and `ModBlockTagsProvider`/`ModItemTagsProvider`. **If you spot another
singular Forge-original `Tags.Items`/`Tags.Blocks` constant that doesn't
resolve, check for a plural rename before assuming it's a `c:`-namespace-only
issue** - this cheat-sheet entry and Phase 16's are two independent rename
axes on the same class.

### Sixth discovery: `GatherDataEvent`'s registration API changed shape (not just a rename)

`net.neoforged.neoforge.data.event.GatherDataEvent` no longer exposes Forge's
`DataGenerator generator = event.getGenerator(); generator.addProvider(bool,
provider)` per-provider client/server gating flag. Instead:
`event.addProvider(T provider)` **always** enrolls the provider to run
(`dataGenerator.addProvider(true, provider)` internally, confirmed in the
sources jar) - **the caller must gate client-only vs. server-only providers
itself** with `if (event.includeClient()) { event.addProvider(...); }` /
`if (event.includeServer()) { ... }` blocks, rather than passing a boolean
flag per call. `ModDataGenerators.gatherData` was rewritten around this
shape. (NeoForge also offers `event.createProvider(BuilderFunctionalInterface)`
convenience overloads that thread `PackOutput`/`lookupProvider` automatically,
but plain `event.addProvider(new XxxProvider(...))` inside the gating blocks
was simpler here since most providers also need `ExistingFileHelper`, which
those convenience overloads don't thread through.)

### Known compile gaps left by this phase (expected, not bugs - do not stub)

Two `data/` files could not be ported this session - both depend on
`client/model/*` types (`GltfModelParser`, `McxModelParser`,
`ModelParseException`, `ModelScene`, `ModelSceneBounds`,
`ModelSceneSelection`, `ModelTransform`), which are part of the entirely
unported `client/` package:

- `data/LegacyInventoryTransform.java` - computes GUI inventory-icon fit
  transforms directly from MCX/glTF scene geometry via
  `ExistingFileHelper.getResource(...)`.
- `data/LegacySceneModelBuilder.java` - a `CustomLoaderBuilder<T>` datagen
  builder for the custom MCX/glTF scene-geometry model loader (would also
  need a one-line `net.minecraftforge.client.model.generators.{CustomLoaderBuilder,
  ModelBuilder}` -> `net.neoforged.neoforge...` rename once unblocked, per
  the already-established pattern - not a new discovery).

**This means `data/` and `client/` are not fully order-independent** as
Phase 16's "either works" suggestion assumed - these two files are the
exception. Port `client/model/` (or all of `client/`) first, then come back
for these two.

`data/recipe/CountedCookingRecipeBuilder.java` was **not ported and should
not be** - see the second discovery above, it's made obsolete by vanilla's
own `SimpleCookingRecipeBuilder` `ItemStack`-result overloads.

`ModDataGenerators.gatherData` does not register
`integration/tconstruct/TinkersConstructDataProvider` - `integration/` is
entirely unported (deferred to last, per every prior phase's notes).

**Suggested next step**: `client/` (53 files) is now the only remaining
non-optional package. Start with `client/model/` (13 files) both because it
unblocks the two `data/` files stranded above and because every renderer/
screen in the rest of `client/` likely depends on it.

## Phase 18 (DONE): `integration/` - 17/17 files, plus 1 `client/` exception

This session ported `integration/` in full (JEI 10 files, Jade 4 files,
CraftTweaker 2 files, Tinkers' Construct 1 file), ahead of `client/` in the
suggested order, at the user's explicit request. Also ported one `client/`
file out of order as a required, narrowly-scoped exception (see below).
Every one of these optional integrations is a **community NeoForge port that
kept the exact same Java package names as its Forge counterpart**
(`mezz.jei.api.*`, `snownee.jade.api.*`, `com.blamejared.crafttweaker.api.*`,
`org.openzen.zencode.*`, `slimeknights.mantle.*`/`slimeknights.tconstruct.*`)
- unlike Forge->NeoForge itself, these are not `net.minecraftforge` ->
`net.neoforged` renames. Verified against real artifacts, not guessed:
downloaded JEI's and CraftTweaker's actual `-sources.jar` for NeoForge
1.21.1 (`jei-1.21.1-{common,neoforge}-api:19.27.0.336`,
`CraftTweaker-neoforge-1.21.1:21.0.39`) from their real maven hosts, and
used `javap` against the real Jade jar (`maven.modrinth:jade:15.10.6+neoforge`
- no sources jar published) since decompiled source wasn't available for it.

### CraftTweaker and Jade: pure package-compatible ports, zero API drift

Both `integration/crafttweaker/*` and `integration/jade/*` compiled against
their real NeoForge 1.21.1 API jars with **zero code changes** beyond the
package-import level (which needed none - same packages). Confirmed by
reading the actual `IRecipeManager<T extends Recipe<?>>` interface
(CraftTweaker) and `IBlockComponentProvider`/`BlockAccessor`/`ITooltip`/
`IServerDataProvider`/`IWailaCommonRegistration`/`IWailaClientRegistration`
(Jade) - every method signature the Forge-source code already called
matches exactly. `@WailaPlugin`'s `value()` element has an empty-string
default (confirmed via `javap -v`'s `AnnotationDefault` bytecode attribute,
since `javap`'s normal output doesn't show annotation defaults) - the
Forge-original's bare `@WailaPlugin` with no argument is still legal.

**One real, required exception**: `integration/jade/JadeMachineComponentProvider`
depends on `client/electrical/ClientVoltageTierRegistry` - a `client/` file,
not yet ported as a package (see Phase 17's "not fully order-independent"
finding, now proven again for a second package pair). Rather than leave the
whole `jade/` integration blocked on all of `client/`, **ported this one
file out of order** as a narrow, justified exception: it is a pure,
zero-Forge-dependency data cache record (only depends on already-ported
`system/network/electric/profile/{VoltageTier,VoltageTierDisplay}`), so
porting it in isolation carries none of the real risk client rendering
code would (fonts, GUI, asset pipeline). This is now the first file
in `client/electrical/`, but it does **not** mean `client/electrical/` as a
whole is safe to consider started - only this one file.

### JEI: three real API breaks (dated to the same 1.21.x recipe rewrite Phase 4/17 already documented)

1. **`ISubtypeRegistration.useNbtForSubtypes(Item...)` is gone entirely.**
   Confirmed absent from the real `ISubtypeRegistration` interface - JEI now
   requires a per-base `registerSubtypeInterpreter(Item, ISubtypeInterpreter<ItemStack>)`
   (or the `IIngredientTypeWithSubtypes` overload), since NBT itself is
   largely gone from items (Phase 10's `DataComponents.CUSTOM_DATA` cheat-sheet
   entry). Fixed with one shared `ISubtypeInterpreter<ItemStack>` whose
   `getSubtypeData` returns `stack.getComponentsPatch()` - a direct, faithful
   generalization of "use full NBT to distinguish subtypes" to the
   components world, registered per-item in a loop replacing the old single
   varargs call. If a future JEI integration needs the old behavior again,
   reuse this same interpreter rather than writing per-item component logic
   unless an item actually needs a narrower comparison.
2. **`RecipeManager.getAllRecipesFor(RecipeType<T>)` returns `List<RecipeHolder<T>>`, not `List<T>`** -
   this is Phase 4's already-documented vanilla change, but Phase 4/17 never
   had to consume it from *JEI's* side before. Every
   `registration.addRecipes(SOME_TYPE, recipes.getAllRecipesFor(...))` call
   in `MagneticraftJeiPlugin.registerRecipes` needed
   `.stream().map(RecipeHolder::value).toList()` inserted (wrapped in a
   small `values(List<RecipeHolder<T>>)` helper) - **except** the one
   recipe type reworked per finding 3 below, which now matches
   `getAllRecipesFor`'s return shape directly with no mapping at all.
3. **A recipe category that needs the recipe's resource id can no longer get
   it from the recipe object** (`Recipe.getId()` is gone, per Phase 4).
   `NuclearProcessingRecipeCategory`'s "route" label depended on checking
   whether the id ends in `_direct` - fixed by keying that one JEI
   `RecipeType` on `RecipeHolder<NuclearProcessRecipe>` instead of the bare
   recipe class, built via `mezz.jei.api.recipe.RecipeType.createFromVanilla(vanillaRecipeType)`
   (confirmed present in the real API, purpose-built for exactly this case -
   "wrap a vanilla-recipe-manager type so JEI keeps the `RecipeHolder`
   through to `setRecipe`/`draw`"). The category class, `setRecipe`, and
   `draw` all now take `RecipeHolder<NuclearProcessRecipe>` and unwrap via
   `.value()`; the id check becomes `holder.id().getPath().endsWith("_direct")`.
   **If another JEI category's `draw()` ever needs a recipe's id, this is
   the reusable fix** - don't reach for `Recipe.getId()`, it doesn't exist;
   wrap that one recipe type with `RecipeHolder` via `createFromVanilla`/
   `createRecipeHolderType`/`createFromDeferredVanilla` instead.

Also two small, already-anticipated renames (same pattern as Phase 16/17,
not new discoveries): `net.minecraftforge.registries.ForgeRegistries.BLOCKS` ->
`net.minecraft.core.registries.BuiltInRegistries.BLOCK` in
`ThermopileRecipeCategory`, and `net.minecraftforge.fluids.FluidStack` ->
`net.neoforged.neoforge.fluids.FluidStack` in `AdvancedProcessingRecipeCategory`/
`PolymerizerRecipeCategory`.

### `FluidStack.getTag()` is gone - `getComponentsPatch()` replaces it for JEI's fluid-slot API too

`net.neoforged.neoforge.fluids.FluidStack.getTag()` (returning `CompoundTag`,
used for the old Forge JEI `IRecipeSlotBuilder.addFluidStack(Fluid, long,
CompoundTag)` overload) no longer exists - confirmed in the sources jar.
JEI's own 3-arg `addFluidStack` overload changed in lockstep: it now takes a
`DataComponentPatch` as its third argument, and `FluidStack.getComponentsPatch()`
(confirmed present, mirrors `ItemStack.getComponentsPatch()`) is the exact
drop-in replacement - same "NBT tag -> data component" pattern as every
other `.getTag()` call site this port has hit (Phase 10's cheat-sheet
entry), just for fluids instead of items. Hit in `GasificationRecipeCategory`
and `PolymerizerRecipeCategory`.

### Tinkers' Construct: API-free, only two datapack-JSON string literals needed fixing

`integration/tconstruct/TinkersConstructDataProvider` never imports
`slimeknights.*` at all (confirmed by grep across both the Forge source and
the ported file) - it hand-builds raw Gson JSON consumed by TConstruct's own
data-driven material/recipe loader, so it has **no compile-time dependency
on Mantle or Tinkers' Construct whatsoever** and needed no build.gradle
`compileOnly` entry for either. Two JSON string literals inside the
generated data needed fixing for the NeoForge-side consumers that actually
read this JSON at runtime: the mod-loaded condition type id
`"forge:mod_loaded"` -> `"neoforge:mod_loaded"` (confirmed via
`net.neoforged.neoforge.common.NeoForgeMod.MOD_LOADED_CONDITION`'s
registration, which registers under the `neoforge` DeferredRegister modid,
not `forge`) and the ingredient tag `"forge:ingots/tungsten"` ->
`"c:ingots/tungsten"` (the by-now-familiar `c:` namespace rename). **Also
fixed the wrapping key for the one JSON that's a real vanilla recipe**
(`"type": "tconstruct:material"`, loaded through `RecipeManager` like any
other data-driven recipe): the conditions-array field itself renamed from
Forge's `"conditions"` to `"neoforge:conditions"` - confirmed via
`net.neoforged.neoforge.common.conditions.ConditionalOps.DEFAULT_CONDITIONS_KEY = "neoforge:conditions"`,
which is exactly the key `Recipe.CONDITIONAL_CODEC` (used by every
vanilla-recipe-manager-loaded JSON, not just this mod's own recipes) reads.
The *other* JSON in this same file - the raw TConstruct material
*definition* (not a recipe, loaded by TConstruct's own bespoke loader, not
`RecipeManager`) - kept its own top-level `"condition"` (singular) field
name unchanged, since that key belongs to TConstruct's own schema, not
NeoForge's generic condition-wrapping convention; only the condition
object's inner `"type"` string needed the `neoforge:` rename, same as above.
**If you add another data-driven optional-integration JSON by hand**: a real
vanilla `Recipe`-typed JSON needs the `neoforge:conditions` key: anything
loaded through a mod's own bespoke datapack loader (like TConstruct
material definitions) keeps whatever field name that mod's Java loader
class expects - check the specific mod's own schema, don't assume one
convention covers both.

### Wired into the build for the first time this port

- **`build.gradle`**: added a `repositories` block for `maven.blamejared.com`
  (JEI + CraftTweaker + zencode) and Modrinth's maven (`maven.modrinth:jade`),
  `compileOnly` entries for JEI/CraftTweaker/Jade's API artifacts (never
  Mantle/Tinkers - see above), and `enable_{jei,crafttweaker,jade,tconstruct}_runtime`-gated
  `localRuntime` blocks mirroring the Forge build's opt-in runtime-testing
  pattern exactly (default `false` in `gradle.properties`, so a normal build
  never touches any of these mods at runtime). Mantle/Tinkers' `localRuntime`
  points at the two vendored jars in `libs/`
  (`NeoMantle-1.21.1-1.21.0-v1.25.jar`, `NeoTinkers-1.21.1-3.11.2-v1.29.jar`)
  plus `libs/modelloader-reborn-3.0.2.jar` (a Tinkers model-loader
  dependency already vendored there) - **no NeoForge 1.21.1 build of
  Mantle/Tinkers' Construct is published on any public maven as of this
  writing** (checked BlameJared, Modrinth, CurseMaven, DVS1's own maven),
  confirming the porting-notes caveat from before this port started.
- **`META-INF/neoforge.mods.toml`**: added the four `[[dependencies.${mod_id}]]`
  optional-dependency blocks (`jei`/`crafttweaker`/`tconstruct`/`jade`,
  `type = "optional"`) mirroring the Forge original's `mandatory = false`
  blocks - NeoForge's toml schema uses `type = "required"/"optional"/...`
  instead of Forge's `mandatory = true/false` boolean (a schema difference
  from the mod-loading system itself, not from any of the four integrations).
- **`ModDataGenerators.gatherData`**: now also registers
  `TinkersConstructDataProvider` (guarded by `includeClient() ||
  includeServer()`, since NeoForge's `event.addProvider` has no per-call
  side flag - see Phase 17's `GatherDataEvent` discovery) - this was the one
  `integration/` reference Phase 17 had explicitly left out pending this
  phase.
- JEI (`@JeiPlugin`), Jade (`@WailaPlugin`), and CraftTweaker (`@ZenRegister`)
  are all still pure annotation-driven self-registration, same as Forge -
  **no explicit wiring needed** in `Magneticraft.java` for those three.

### Known compile gaps left by this phase (expected, not bugs - do not stub)

None inside `integration/` itself - all 17 files are fully ported with no
stubs. The pre-existing `data/` gap from Phase 17
(`LegacyInventoryTransform.java`/`LegacySceneModelBuilder.java`, blocked on
`client/model/`) is unrelated to this phase and still open.

**`integration/` is now CLOSED IN FULL (17/17).** Combined with `content/`,
`init/`, `system/`, `api/`, the root `network/` package, and `data/`
(21/24) already closed or mostly closed, **`client/` (53 files, minus the
1 file this phase already took - 52 remain) is the only package with
substantial work left**, plus the 2 stranded `data/` files.

**Suggested next step**: `client/`, starting with `client/model/` (13
files) - same suggestion as Phase 17, now even more clearly the last
non-trivial package. `client/electrical/` (1 of 2 files already done this
phase - `ClientVoltageTierRegistry`) is a reasonable very next small step
after `client/model/` if aiming for another quick win.

## Phase 19 (DONE): `client/model/` - full package, 14/14 files

Ported all 14 files of `client/model/` (the MCX/glTF custom scene-model
loader and renderer that `client/electrical/ClientVoltageTierRegistry`'s
Jade integration sibling files, and most of the still-unported
`client/*Renderer`/`client/*Screen` classes, will need). File-for-file
parity verified against the source directory (14 = 14). This was the
package Phase 17/18 both flagged as the correct next step, since it also
unblocks the 2 stranded `data/` files left behind in Phase 17
(`LegacyInventoryTransform.java`/`LegacySceneModelBuilder.java`).

Breakdown by risk tier:

- **10 files, zero rewrite** (pure Java - zero `net.minecraft`/Forge
  imports at all, copied verbatim, byte-identical `diff`): `GltfModelParser`,
  `McxModelParser`, `ModelAnimationSampler`, `ModelParseException`,
  `ModelRenderManifest`, `ModelScene`, `ModelSceneBounds`,
  `ModelSceneSelection`, `ModelSelection`, `ModelTransform`.
- **1 file, zero rewrite despite vanilla imports** (`ModelRenderManifestRegistry`
  uses `Minecraft`/`ResourceLocation`/`Resource`/`ResourceManager`/
  `ResourceManagerReloadListener`, all unchanged vanilla APIs - copied
  verbatim, byte-identical `diff`).
- **1 file, pure package rename**: `LegacyModelLoader` -
  `net.minecraftforge.client.model.geometry.IGeometryLoader` ->
  `net.neoforged.neoforge.client.model.geometry.IGeometryLoader`, identical
  single-method shape (`T read(JsonObject, JsonDeserializationContext)`),
  confirmed in the sources jar. Everything else in the file is vanilla-only
  and unchanged.
- **1 file, vertex-consumer API rewrite** (no Forge/NeoForge import at all,
  but hit a vanilla 1.21 rendering-API break): `LegacySceneRenderer`. See
  the new cheat-sheet entry below - the old chained
  `vertex(matrix,x,y,z).color(...).uv(...).overlayCoords(...).uv2(...).normal(...).endVertex()`
  builder style is gone from vanilla's `VertexConsumer`, replaced by
  `addVertex(x,y,z)` + separate `setColor`/`setUv`/`setOverlay`/`setLight`/
  `setNormal` calls (no explicit "end vertex" call needed - `addVertex`
  itself starts the next vertex). `renderPrimitive`'s per-vertex loop was
  rewritten to the new call sequence; the math (position/UV/normal
  computation) is untouched.
- **1 file, the full geometry-loader API rewrite**: `LegacySceneGeometry`.
  See below - this was the one file in the package that needed real
  analysis, not just renames.

### New discovery: NeoForge's custom-geometry-loader API dropped `ResourceLocation modelLocation` and the "fast" render-type hint between 1.20.1 and 1.21.1

Confirmed by reading `net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry`/
`SimpleUnbakedGeometry`/`IGeometryBakingContext` and
`net.neoforged.neoforge.client.model.IModelBuilder` directly in the
`neoforge-21.1.250-sources.jar` (not guessed): the Forge 1.20.1 shapes
`IUnbakedGeometry.bake(context, baker, spriteGetter, modelState, overrides,
ResourceLocation modelLocation)` and `SimpleUnbakedGeometry.addQuads(owner,
modelBuilder, baker, spriteGetter, modelTransform, ResourceLocation
modelLocation)` both lost their trailing `ResourceLocation modelLocation`
parameter in this NeoForge version (now 5-arg, not 6-arg) - the "fast render
type" concept (`IGeometryBakingContext.getRenderTypeFastHint()` and the
matching 8-arg `IModelBuilder.of(..., RenderTypeGroup renderTypes,
RenderTypeGroup fastRenderTypes)` overload the Forge original called) is
**gone entirely** - only the plain `getRenderTypeHint()`/7-arg `IModelBuilder.of(...)`
remain. `LegacySceneGeometry.bake`/`addQuads` were rewritten to the 5-arg
shapes and the fast-render-type branch was dropped along with it (there is
no replacement to port to - the concept doesn't exist in this API version).
`RenderTypeGroup`, `IModelBuilder`, `IGeometryBakingContext`,
`SimpleUnbakedGeometry` are otherwise a pure `net.minecraftforge.client.*` ->
`net.neoforged.neoforge.client.*` package rename with identical remaining
shape. **If another custom model loader is ported later and still branches
on a "fast" render type or passes a bake-time model location, this is the
same finding - drop it, don't stub it.**

### New cheat-sheet-worthy discovery: `QuadBakingVertexConsumer` and vanilla's `VertexConsumer` both moved from a chained "builder" style to a flat setter style

`net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer`
(used by `LegacySceneGeometry` to bake a `BakedQuad` by hand) is a pure
package rename **in name only** - its internal shape changed completely
between the two versions, in lockstep with vanilla's own `VertexConsumer`
interface (confirmed by decompiling `com.mojang.blaze3d.vertex.VertexConsumer`
from the actual 1.21.1 NeoForge-patched vanilla sources, not docs). Old
Forge 1.20.1 shape: a nested `QuadBakingVertexConsumer.Buffered` helper with
`vertex(x,y,z).color(r,g,b,a).uv(u,v).uv2(packedLight).normal(x,y,z).endVertex()`
returning a `BakedQuad` via `.getQuad()`. New 1.21.1 shape: `QuadBakingVertexConsumer`
itself directly implements `VertexConsumer`'s new primary methods -
`addVertex(x,y,z)` (starts a new vertex slot), `setColor(int r,int g,int b,int a)`
(0-255 ints, not 0-1 floats - though the interface still has a
`setColor(float,float,float,float)` **default** method that does the
0-255 conversion for you, a clean drop-in for old float-based call sites),
`setUv(u,v)`, `setUv2(u,v)` (two ints, replacing the old single packed-light
`uv2(int)` - `VertexConsumer` still has a `setLight(int packedLight)`
default that does the same `u = packedLight & 0xFFFF, v = packedLight >> 16`
split internally, usable instead of splitting by hand), `setNormal(x,y,z)`
(also has a `setNormal(PoseStack.Pose, x,y,z)` default that pre-transforms
by the pose's normal matrix, replacing the old `.normal(pose.normal(), x,y,z)`
call shape exactly), then `bakeQuad()` (renamed from `.getQuad()`) to
retrieve the finished `BakedQuad` after exactly 4 `addVertex` calls. **No
explicit "end vertex" call exists or is needed** - each `addVertex` call
itself advances to the next vertex slot. `LegacySceneGeometry.bakePrimitive`
and `LegacySceneRenderer.renderPrimitive` were both rewritten to this new
call sequence; the underlying per-vertex math (transform application, UV
scaling, normal computation) is byte-for-byte unchanged from the Forge
original. **This is a vanilla rendering API change, not a NeoForge one** -
expect the same old-chained-builder -> new-flat-setter rewrite in every
still-unported `client/*Renderer.java` file that emits raw quads/vertices
by hand (the whole `client/` package was written against the old builder
style throughout, per a full-tree grep of the Forge source for `.endVertex()`/
`.uv2(`/`.overlayCoords(` - there are more hits outside `client/model/`).

### Known compile gaps left by this phase (expected, not bugs - do not stub)

None inside `client/model/` itself - all 14 files are fully ported with no
stubs and no unported dependencies (the package is entirely self-contained
plus `committee.nova.mods.magneticraft.Magneticraft` for the logger, already
ported in Phase 1).

Immediately after closing `client/model/`, this session also ported the 2
files `data/` had been stranded on since Phase 17:
`data/LegacyInventoryTransform.java` (datagen-time GUI-fit-transform
calculator, reads MCX/glTF geometry directly via `ExistingFileHelper` - only
needed `net.minecraftforge.common.data.ExistingFileHelper` ->
`net.neoforged.neoforge.common.data.ExistingFileHelper`, a pure package
rename, confirmed in the sources jar) and `data/LegacySceneModelBuilder.java`
(the `CustomLoaderBuilder<T>` subclass that emits the `"loader"`/`"model"`/
node-selection JSON for `LegacyModelLoader` to consume at runtime). **One
real API change found**: `net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder`'s
protected constructor gained a 4th parameter, `boolean allowInlineElements`
(confirmed in the sources jar - it's checked by `ModelBuilder.customLoader()`
against whether the model also defines inline vanilla `elements`, to decide
if a missing/optional loader can fall back to vanilla loading). Magneticraft's
loader has no vanilla-element fallback story, so `super(...)` now passes
`false`. `net.neoforged.neoforge.client.model.generators.ModelBuilder`
itself is otherwise an unchanged pure package rename (confirmed
`ModelBuilder.customLoader(BiFunction<T, ExistingFileHelper, L>)` still has
the exact shape `LegacySceneModelBuilder::begin` needs, so the existing
`ModBlockStateProvider.legacySceneModel` call site - ported earlier, already
referencing both classes as a known gap - needed no changes at all). This
closes `data/` to 23/24 (only the intentionally-skipped
`CountedCookingRecipeBuilder.java` remains absent, see Phase 17).

**Suggested next step**: `client/electrical/` (1 of 2 files already done,
see Phase 18) is a small next win, or dive into the remaining
renderer/screen files in `client/` directly - expect the `VertexConsumer`
builder-to-setter rewrite documented above to recur throughout, since the
whole package was written against the old chained-builder vertex API.

## Phase 20 (DONE): `client/electrical/ClientElectricalEvents.java` - closes `client/electrical/`, 2/2 files

Ported the last file in `client/electrical/` (`ClientVoltageTierRegistry.java` was
already done as a narrow Phase 18 exception). One file, full event-subscriber
rewrite - verified against the real `loader-4.0.43-sources.jar` (the
`net.neoforged.fancymodloader` module, a separate Gradle artifact from
`neoforge` itself - `net.neoforged.fml.*`/`net.neoforged.api.distmarker.*`
live there, not in the `neoforge-*-sources.jar`; confirmed by an empty grep of
the neoforge sources jar's file listing for both packages) and
`neoforge-21.1.250-sources.jar`, not guessed from docs.

Two real discoveries, both new to the cheat sheet:

1. **`@EventBusSubscriber`'s `bus()` parameter is now deprecated and ignored.**
   Reading the actual annotation source: the bus (mod bus vs. game/`NeoForge#EVENT_BUS`)
   is now auto-detected per-listener from whether the event type implements
   `IModBusEvent` - a single `@EventBusSubscriber`-annotated class can mix
   listeners for both buses. `bus = EventBusSubscriber.Bus.GAME` (the pattern
   every earlier phase's `*Events.java` file already uses) still compiles and
   is harmless, just inert - kept for consistency with the rest of the
   codebase, not because it's required. **`value()` is very much still live**
   and is the `Dist[]` restriction (default `{CLIENT, DEDICATED_SERVER}` -
   both sides) - this is the direct replacement for the Forge original's
   `@Mod.EventBusSubscriber(..., value = Dist.CLIENT)` third argument, same
   name, same purpose, confirmed in the annotation's own javadoc.
2. **`TickEvent.ClientTickEvent` (with a `.phase` field) is gone**, split into
   two standalone classes with no phase field: `net.neoforged.neoforge.client.event.ClientTickEvent.Pre`
   / `.Post` (confirmed in the sources jar - `Pre` fires before the client
   does per-tick work, `Post` fires after). A Forge original that gated on
   `event.phase == TickEvent.Phase.END` should subscribe to `ClientTickEvent.Post`
   instead of filtering inside the handler; `.Pre` is the `START`-phase
   equivalent. (The cheat sheet already had this same `Pre`/`.Post` split
   documented for `PlayerTickEvent`/`EntityTickEvent`/`LevelTickEvent`/`ServerTickEvent`
   under `event.tick` - `ClientTickEvent` is the same pattern, just under
   `client.event` instead, easy to miss since it's a different package.)

`ClientPlayerNetworkEvent.LoggingOut` needed no changes beyond the package
rename (`net.minecraftforge.client.event` -> `net.neoforged.neoforge.client.event`) -
confirmed identical shape (constructor args, nullable getters) in the sources
jar.

**No known compile gaps left in this file** - `ClientVoltageTierRegistry`
(sibling, Phase 18) and `content/machine/framework/{MachineBlockEntity,MachineModelRefreshQueue}`
(Phase 2) it depends on both already exist.

## Phase 21 (DONE): 7 foundational `client/` root files - `AnimationMath`, `MachineIcon`, `MachineScreenBounds`, `MachineIconButton`, `MachineScreenLayout`, `ElectricalTierColors`, `MachineRenderHelper`

Ported the small, self-contained utility/helper classes that the still-unported
renderer/screen files in `client/` depend on, working bottom-up so the next
screens/renderers ported will have everything they need already in place.
File-for-file, verified against the actual sources jars, not guessed:

- **3 files, zero rewrite** (pure Java or vanilla-only, copied verbatim,
  byte-identical `diff`): `AnimationMath` (pure Java, no imports at all),
  `MachineIcon` (only `GuiGraphics.fill`, unchanged), `MachineScreenBounds`
  (pure Java plus the already-ported `LegacyMachineGuiLayout.Rect`, confirmed
  its `right()`/`bottom()`/`contains()`/`overlaps()` helper methods already
  exist with matching shape).
- **1 file, zero rewrite despite depending on already-ported `content/`
  types**: `ElectricalTierColors` - `BlockColor`/`ItemColor` functional
  interfaces confirmed unchanged (`getColor(BlockState, @Nullable
  BlockAndTintGetter, @Nullable BlockPos, int)` / `getColor(ItemStack, int)`,
  both still `@OnlyIn(Dist.CLIENT)` single-method interfaces in the vanilla
  1.21.1 sources), and `ElectricalNetworkModule.tierId()`/
  `TieredElectricalItemData.read(ItemStack)`/`VoltageTierDisplay.colorRgb()`
  (all from earlier phases) matched exactly - a pure Java/vanilla file with
  zero Forge imports in the original, so no change needed at all.
- **1 file, one real API break**: `MachineIconButton` -
  `AbstractWidget.setTooltipDelay(int)` is gone; the vanilla signature is now
  `setTooltipDelay(java.time.Duration)` (confirmed in the decompiled 1.21.1
  vanilla source - `this.tooltip.setDelay(tooltipDelay)`). Fixed with
  `setTooltipDelay(Duration.ofMillis(150))`. Everything else in the file
  (`AbstractButton`'s constructor/`renderWidget`/`onPress` shape, `Tooltip.create(Component,
  Component)`, `updateWidgetNarration`/`defaultButtonNarrationText`/
  `isHoveredOrFocused`, the protected `width`/`height` fields) confirmed
  unchanged against the real `AbstractWidget.java`/`AbstractButton.java`/
  `Tooltip.java` in the decompiled vanilla sources - **new cheat-sheet
  entry**, watch for more `setTooltipDelay(int)` call sites in the remaining
  screen files.
- **1 file, pure package rename**: `MachineScreenLayout` -
  `net.minecraftforge.fml.loading.FMLEnvironment` -> `net.neoforged.fml.loading.FMLEnvironment`,
  confirmed identical shape (`public static final boolean production`) in the
  `net.neoforged.fancymodloader:loader` sources jar (same module as Phase
  20's `EventBusSubscriber`/`Dist` finding - `net.neoforged.fml.*` classes
  live there, not in `neoforge-*-sources.jar`).
- **1 file, the `VertexConsumer` builder-to-setter rewrite predicted since
  Phase 19/20**: `MachineRenderHelper` - its `renderFluidSurface`/
  `renderFluidCuboid` fluid-rendering helpers used the old chained
  `.vertex(matrix,x,y,z).color(...).uv(...).overlayCoords(...).uv2(...).normal(normal,x,y,z).endVertex()`
  builder. Rewritten using the same flat-setter API as Phase 19's
  `LegacySceneRenderer`, but this time passing the full `PoseStack.Pose`
  down to the vertex helpers instead of pre-extracting `Matrix4f`/`Matrix3f`
  locals - `VertexConsumer` has convenience defaults for exactly this shape:
  `addVertex(PoseStack.Pose, x, y, z)` (internally calls `pose.pose()` then
  `addVertex(Matrix4f, ...)`) and `setNormal(PoseStack.Pose, x, y, z)`
  (internally calls `pose.transformNormal(...)` then the raw 3-float
  `setNormal`) - confirmed both exist as default methods on the real
  `com.mojang.blaze3d.vertex.VertexConsumer` interface in the decompiled
  1.21.1 sources. This is cleaner than Phase 19's approach (which extracted
  `Matrix4f pose.pose()`/`Matrix3f pose.normal()` locals up front) and is
  the preferred pattern for any future renderer whose vertex helpers already
  receive a `PoseStack`/`PoseStack.Pose` - **prefer `addVertex(pose, ...)` +
  `setNormal(pose, ...)` over manually extracting the matrix/normal**, only
  fall back to the raw `Matrix4f`/`Matrix3f`-accepting overloads if the
  calling code truly only has the bare matrices in hand (as `LegacySceneGeometry`'s
  bake-time code did in Phase 19, which has no live `PoseStack` at all).
  Also renamed `net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions` ->
  `net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions`
  and `net.minecraftforge.fluids.FluidStack` -> `net.neoforged.neoforge.fluids.FluidStack`
  (both pure renames, confirmed `IClientFluidTypeExtensions.of(Fluid)`/
  `.getStillTexture(FluidStack)`/`.getTintColor(FluidStack)` unchanged in the
  sources jar - same finding as Phase 2's `MagneticraftFluidType`).

**No known compile gaps left in any of these 7 files** - every `content/`/
`system/` type they reference (`LegacyMachineGuiLayout.Rect`,
`BoxTransformerBlockEntity`, `TieredElectricalHost`, `ElectricalNetworkModule`,
`TieredElectricalItemData`, `ClientVoltageTierRegistry`) already exists from
earlier phases.

**Suggested next step**: the remaining 29 files in `client/` are all
renderers (`*Renderer.java`) and screens (`*Screen.java`), plus
`ElectricalStatePanel`/`GuideClientEvents`/`GuideStructurePreview`/
`LegacySceneModels`/`guide/GuideRepository`/`ClientSetup` (the big
registration/wiring file - leave `ClientSetup` for last since it references
essentially every renderer/screen by name for `BlockEntityRenderers.register`/
`MenuScreens.register` calls, most of which don't exist yet). Every renderer
should be expected to need the same `VertexConsumer` rewrite documented in
Phase 19/this phase if it emits raw quads by hand; every screen should be
expected to lean on `MachineScreenLayout`/`MachineScreenBounds`/`MachineIconButton`/
`MachineIcon` (all now ported) for its chrome.

## Phase 22 (DONE): 14 more `client/` files - almost every remaining renderer, plus `ElectricalStatePanel`, `guide/GuideRepository`, `LegacySceneModels`, `GuideClientEvents`, `GuideStructurePreview`

Ported: `ElectricalStatePanel`, `guide/GuideRepository`, `LegacySceneModels`,
`ComputerRenderer`, `WindTurbineRenderer`, `CrushingTableRenderer`,
`MiningRobotRenderer`, `ConveyorBeltRenderer`, `PneumaticTubeRenderer`,
`ElectricalDeviceRenderer`, `NuclearMultiblockRenderer`,
`LongDistanceWireRenderer`, `GuideClientEvents`, `GuideStructurePreview`.
Only `AdvancedMultiblockRenderer`/`SingleBlockMachineRenderer` (the two large
multiblock/framework renderers) and every `*Screen.java` remain in `client/`.

Breakdown by risk tier, each verified against the real sources jars/decompiled
vanilla, not guessed:

- **10 files, zero rewrite** (pure Java/vanilla-only or depending only on
  already-ported `content/`/`system/` types with unchanged shapes, copied
  verbatim, byte-identical `diff`): `ElectricalStatePanel` (confirmed
  `MachineBlockEntity.electricalTerminals()`, `ElectricalNetworkModule.displayReading()`,
  `GuiGraphics.renderComponentTooltip(Font, List<Component>, int, int)`
  unchanged), `guide/GuideRepository` (confirmed vanilla
  `SimplePreparableReloadListener<T>`'s abstract `prepare`/`apply` signatures
  and `ResourceManager.listResources`/`.getResource` unchanged despite now
  extending a new NeoForge `ContextAwareReloadListener` internal base -
  transparent to subclasses), `LegacySceneModels` (pure glue over Phase 19's
  `LegacySceneRenderer`/`ModelSceneSelection`/`ModelTransform`),
  `ComputerRenderer`/`WindTurbineRenderer`/`CrushingTableRenderer`/
  `MiningRobotRenderer`/`ConveyorBeltRenderer`/`PneumaticTubeRenderer` (all
  confirmed `BlockEntityRenderer<T>.render(...)` signature unchanged, plus
  each block entity's already-ported accessor methods matched exactly -
  `ItemRenderer.getModel(ItemStack, Level, LivingEntity, int)` also confirmed
  unchanged for `CrushingTableRenderer`'s flat-vs-3D item check).
- **2 files, event-subscriber pure rename** (reusing the Phase 20 pattern):
  `GuideClientEvents` - `net.minecraftforge.event.entity.player.PlayerInteractEvent` ->
  `net.neoforged.neoforge.event.entity.player.PlayerInteractEvent`, confirmed
  `RightClickItem implements ICancellableEvent` (so `setCanceled(boolean)`
  comes from the interface default, same as the already-ported
  `content/item/DiagnosticInteractionEvents` file's pattern) and
  `setCancellationResult(InteractionResult)`/`getItemStack()`/`getEntity()`
  all unchanged. **Known compile gap**: references `new GuideScreen()`, not
  ported yet - left as a documented gap, not stubbed, same as every other
  forward-reference in this port.
- **1 file, `RenderStyle.palette` reuse + already-verified accessors**:
  `ElectricalDeviceRenderer` - zero new API findings, every `content/network/{electric,module}`
  accessor it calls (`transformerCoupler()`, `protection()`, `tripped()`/
  `blown()`/`redstoneForcedOpen()`/`fuse()`, etc.) already existed from
  earlier phases with matching names.
- **1 file, position/render-accessor reuse**: `NuclearMultiblockRenderer` -
  zero new findings, `NuclearReactorControllerBlockEntity`/
  `SpentFuelPoolControllerBlockEntity`'s `facing()`/`renderWidth()`/
  `renderHeight()`/`renderLength()`/`FORMED` property all already existed.
- **1 file, the `VertexConsumer` builder-to-setter rewrite (same pattern as
  Phase 19/21)**: `LongDistanceWireRenderer`'s line-segment wire renderer.
  Its `vertex(...)` helper used the old
  `.vertex(matrix,x,y,z).color(...).normal(normal,x,y,z).endVertex()` chain
  for `RenderType.lines()` (position + color + normal only, no UV/light/
  overlay - confirmed that's a valid partial call sequence, `VertexConsumer`
  doesn't require every setter to be called before the next `addVertex`).
  Rewritten to `consumer.addVertex(pose, x, y, z).setColor(r,g,b,a).setNormal(pose, nx, ny, nz)`,
  same `PoseStack.Pose`-accepting convenience-overload preference documented
  in Phase 21's cheat-sheet entry. Also dropped the now-unused
  `org.joml.Matrix3f`/`Matrix4f` imports (no longer manually extracted).
- **1 file, two real API changes**: `GuideStructurePreview` -
  (1) `net.minecraftforge.client.model.data.ModelData` -> `net.neoforged.neoforge.client.model.data.ModelData`,
  confirmed `.EMPTY` constant and `BlockRenderDispatcher.renderSingleBlock(BlockState,
  PoseStack, MultiBufferSource, int, int, ModelData, RenderType)`'s exact
  7-arg shape (the vanilla class's own 5-arg convenience overload internally
  delegates to this exact NeoForge-typed 7-arg one, confirmed in the
  decompiled source) - pure rename, no shape change. (2)
  `net.minecraftforge.registries.ForgeRegistries.BLOCKS.containsKey(id)`/`.getValue(id)`
  -> `net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)`/`.get(id)`
  (same `DefaultedRegistry` rename as the Phase 10 `PressureGaugeItem`
  finding) - since the original code already guarded every `.getValue()` call
  with a preceding `.containsKey()` check, the redundant `if (block == null)`
  branch after `.get(...)` (which `BuiltInRegistries.BLOCK` never returns) was
  dropped as dead code, not left in as a silently-unreachable branch.
  `LevelRenderer.renderLineBox`'s 12-arg overload confirmed unchanged.

**No known compile gaps left in any of these 14 files** except
`GuideClientEvents`' forward reference to `GuideScreen` (documented above) -
every other `content/`/`system/`/`client/model/`/`client/guide` type they
reference already exists.

**Suggested next step**: `SingleBlockMachineRenderer.java` (the other large
renderer) or any `*Screen.java` directly - `GuideScreen.java` first would
close the `GuideClientEvents` forward-reference gap. Leave
`AdvancedMultiblockRenderer.java` (676 lines, likely the most involved
remaining renderer) and `ClientSetup.java` (the registration file - needs
essentially everything else in `client/` to exist first) for last.

## Phase 23 (DONE): 7 small/medium `*Screen.java` files - `PressureTankScreen`, `WindTurbineScreen`, `BatteryScreen`, `NuclearThermalScreen`, `SpentFuelPoolScreen`, `NuclearFacilityScreen`, `ElectricFurnaceScreen`

**The single most important discovery of this phase - affects every
remaining and already-ported-elsewhere container screen**: vanilla's
`AbstractContainerScreen.render(GuiGraphics, int, int, float)` changed
between 1.20.1 and 1.21.1 independent of Forge/NeoForge. Confirmed by
decompiling the real 1.21.1 `AbstractContainerScreen.java`/`Screen.java`:

- `Screen.renderBackground(GuiGraphics)` (the old 1-arg "darken/blur the
  background" call every Forge-era screen's `render()` override started
  with) **is gone entirely** - only `renderBackground(GuiGraphics, int
  mouseX, int mouseY, float partialTick)` (4-arg) exists now.
- `AbstractContainerScreen` overrides that 4-arg `renderBackground` to do
  `renderTransparentBackground(guiGraphics); renderBg(guiGraphics,
  partialTick, mouseX, mouseY);` - i.e. it now **absorbs the `renderBg` call
  that used to only happen inside `super.render(...)`**.
- Both `Screen.render(...)` and `AbstractContainerScreen.render(...)`
  (confirmed in the decompiled source) call `this.renderBackground(guiGraphics,
  mouseX, mouseY, partialTick)` as their very first statement - so by the
  time a subclass's `render()` override calls `super.render(...)`, the
  background/`renderBg` painting has **already happened**.

**The fix, mechanical and identical everywhere**: every screen's overridden
`render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)`
in the Forge original starts with `renderBackground(graphics);` immediately
before `super.render(...)`. That line must simply be **deleted** - `super.render(...)`
now does the equivalent work on its own. Everything after it (tooltips,
`renderTooltip(...)`) is unchanged. **Every remaining `*Screen.java` in this
port will have this exact same one-line deletion** - check for it first in
each one; do not leave the stale 1-arg call in, it will not compile.

Ported 7 screens with this fix applied, each also individually verified
against its already-ported `*Menu` class for accessor-name/shape matches
(zero mismatches found - every `Menu.value(index)`-backed accessor from
Phases 11/12 already existed with the exact name the screen calls):

- `PressureTankScreen`: also the one real registry-API rewrite in this batch -
  `net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(id)` (nullable)
  -> `net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id)`/`.containsKey(id)`,
  same `DefaultedRegistry` pattern as Phase 10's `PressureGaugeItem` and
  Phase 22's `GuideStructurePreview` findings - the ported code now guards
  `.get(id)` with an explicit `.containsKey(id)` check up front rather than
  null-checking the (never-null) result.
- `WindTurbineScreen`, `BatteryScreen`, `NuclearThermalScreen`,
  `SpentFuelPoolScreen`, `NuclearFacilityScreen`, `ElectricFurnaceScreen`:
  zero rewrite beyond the `renderBackground` deletion above - pure
  `MachineScreenLayout`/`MachineScreenBounds`/`ElectricalStatePanel` chrome
  over already-ported `*Menu` accessors, `Slot.x`/`Slot.y` fields (confirmed
  `public final int x, y` unchanged in 1.21.1), and `GuiGraphics.renderTooltip(Font,
  Component, int, int)` (confirmed unchanged, same signature check as Phase
  22).

**No known compile gaps left in any of these 7 files** - every `content/`
menu/accessor and `client/` helper type they reference already exists.

**Suggested next step**: the remaining screens (`GuideScreen.java` first,
closes the Phase 22 `GuideClientEvents` gap; then `ElectricalDeviceScreen`/
`ProgrammableScreen`/`SingleBlockMachineScreen`/`AdvancedMultiblockScreen`/
`NuclearReactorScreen`) or `SingleBlockMachineRenderer.java` - apply the
`renderBackground` deletion to every one of them first. Leave
`AdvancedMultiblockRenderer.java` and `ClientSetup.java` for last, same
reasoning as before.

## Phase 24 (DONE): `src/main/java` CLOSED - the last 9 files, port complete

Ported the last 9 files in one session, finishing the entire `src/main/java`
tree: `GuideScreen`, `ElectricalDeviceScreen`, `ProgrammableScreen`,
`SingleBlockMachineScreen`, `AdvancedMultiblockScreen`, `NuclearReactorScreen`,
`SingleBlockMachineRenderer`, `AdvancedMultiblockRenderer`, `ClientSetup`.
**`src/main/java` is now 530/530 files closed** (531 files physically present
- the 530 mirrored originals minus the one intentionally-skipped
`CountedCookingRecipeBuilder.java` plus the two legitimate new files added
along the way, `MachineCapabilities.java` (Phase 2) and
`PortableEnergyComponents.java` (Phase 10)). A full-tree grep confirms zero
`net.minecraftforge`/`RegistryObject` references anywhere in
`src/main/java`. `src/gametest`, `src/test`, `src/integrationTest`,
`src/generated`, and `src/main/resources` remain untouched, as they have
been out of scope throughout this port (see "What's left" below).

Five real discoveries this phase, each verified against the actual
decompiled/jar sources, not guessed:

1. **`GuideScreen` (a plain `Screen`, not `AbstractContainerScreen`) needed a
   different fix than every other screen.** Vanilla `Screen.render(...)`
   now calls `this.renderBackground(guiGraphics, mouseX, mouseY,
   partialTick)` (the 4-arg form) as its very first statement - confirmed by
   decompiling `Screen.java`. `GuideScreen`'s original `render()` override
   draws a large amount of custom content (panel/header/mode-specific data)
   *between* the old explicit `renderBackground(graphics)` call and the
   trailing `super.render(...)` call (which used to only render widgets).
   Simply deleting the old call and keeping `super.render(...)` at the end
   (the fix used everywhere else) would make the background paint *after*
   and *on top of* all that custom content. Fix: call
   `renderBackground(graphics, mouseX, mouseY, partialTick)` explicitly at
   the top (unchanged from the original position), draw the custom content
   as before, then replace the trailing `super.render(...)` with a manual
   loop over the public `renderables` field
   (`for (Renderable r : renderables) r.render(...)`) so the widgets still
   render last without re-triggering `renderBackground` a second time. This
   is a **new pattern**, distinct from the simple deletion fix documented in
   Phase 23 - use it for any future plain-`Screen` subclass that interleaves
   custom drawing between the old background call and `super.render()`.
2. **`PoseStack.mulPoseMatrix(Matrix4f)` is gone, renamed to
   `mulPose(Matrix4f)`** - confirmed in the decompiled `PoseStack.java` (a
   single overloaded `mulPose` now handles both `Quaternionf` and `Matrix4f`
   arguments). Pure rename, no shape change. Hit in
   `SingleBlockMachineRenderer.renderInserterItem`.
3. **A custom `VertexConsumer` implementation's required-override list
   shrank** with the Phase 19/20/21/22 flat-setter API change: the old
   interface required implementing `vertex`/`color`/`uv`/`overlayCoords`/
   `uv2`/`normal`/`endVertex` (7 methods) plus `defaultColor`/
   `unsetDefaultColor`; the new interface's only abstract methods are
   `addVertex(float,float,float)`, `setColor(int,int,int,int)`,
   `setUv(float,float)`, `setUv1(int,int)`, `setUv2(int,int)`,
   `setNormal(float,float,float)` (6 methods, `defaultColor`/
   `unsetDefaultColor` gone entirely) - confirmed by reading the real
   interface declaration in the sources jar.
   `AdvancedMultiblockRenderer.AlphaVertexConsumer` (an alpha-multiplying
   delegating wrapper used for ghost/hologram blocks) was rewritten to the
   new 6-method shape - simpler than the original, not just renamed.
4. **`MenuScreens.register(MenuType, ScreenConstructor)` is now `private`
   and `@Deprecated`**, replaced by a dedicated
   `net.neoforged.neoforge.client.event.RegisterMenuScreensEvent` (a mod-bus
   `IModBusEvent`, confirmed in the sources jar) with the same
   `event.register(menuType, screenConstructor)` call shape. This is an
   **architectural change, not a rename**: all 13 `MenuScreens.register(...)`
   calls (`ClientSetup.onClientSetup`'s body) had to move out of the
   `FMLClientSetupEvent.enqueueWork(...)` block entirely and into a new
   `onRegisterMenuScreens(RegisterMenuScreensEvent event)` listener method -
   they are no longer deferred work, they're direct calls on the event
   itself. `ItemProperties.register(...)` and `BlockEntityRenderers.register(...)`
   remain plain public static methods with no replacement event (confirmed
   by reading both classes in full) - those stayed inside
   `enqueueWork(...)` unchanged.
5. **`ModelEvent.RegisterGeometryLoaders.register` now takes a
   `ResourceLocation` key, not a `String`** - confirmed in the sources jar
   (`public void register(ResourceLocation key, IGeometryLoader<?> loader)`,
   no String overload). The Forge original's `event.register("legacy_scene",
   LegacyModelLoader.INSTANCE)` became `event.register(Magneticraft.id("legacy_scene"),
   LegacyModelLoader.INSTANCE)` - confirmed this exact ID
   (`magneticraft:legacy_scene`) is what `data/LegacySceneModelBuilder.java`
   (already ported, Phase 19) already emits into datagen'd model JSON via
   the same `Magneticraft.id("legacy_scene")` call, so the two sides agree.

**No known compile gaps left anywhere in `src/main/java`.**

## Phase 25 (DONE): first real `./gradlew compileJava` - 75 errors fixed, then `./gradlew build` green, then a real runtime crash fixed

This session ran `./gradlew compileJava` against the tree for the first time
ever (after 24 phases of "verified by reading only"). It failed with 75
errors across ~50 files. All were fixed; the tree now builds clean
(`./gradlew build` succeeds, 11 harmless pre-existing `[deprecation]`
warnings remain - `hasChunkAt`/`rotate(Rotation)`/`isFluidEqual`/etc., all
already documented or trivially safe). None of the fixes were guesses -
every one was checked against the real decompiled vanilla source or the
NeoForge sources jar (`sourcesWithNeoForge_e2d4a39d745d7ca198b2ada7812a435b5013a4f9_output.zip`,
the most-recently-modified of the 6 cached extracts, matching this
project's pinned `neo_version=21.1.250`). New discoveries, **all missing
from the cheat sheet above despite 24 prior phases** - add these to the
mental model for any future NeoForge 1.21.1 port:

1. **`BlockBehaviour.codec()` is a real abstract method that every concrete
   `Block` subclass must implement** (`protected abstract MapCodec<? extends
   Block> codec();`, confirmed in `BlockBehaviour.java`) - not mentioned
   anywhere in this file before because no compile had ever been attempted.
   Hit ~38 concrete block classes across every `content/` subpackage (every
   leaf `BaseEntityBlock`/`DirectionalBlock` subclass; intermediate abstract
   classes like `NetworkComponentBlock`/`ConduitBlock`/`ProgrammableBlock`/
   `WallMountedElectricBlock` correctly stay abstract and need nothing).
   Fix pattern, two shapes: (a) for a block whose constructor is exactly
   `(Properties)`, add `public static final MapCodec<X> CODEC =
   simpleCodec(X::new);` + `@Override protected MapCodec<? extends X>
   codec() { return CODEC; }` (mirrors vanilla's own convention, confirmed
   in `StairBlock`/`LiquidBlock`). (b) for a block with extra fixed
   constructor args that vary per registered instance (a tier/definition
   enum, a boolean flag) - `ElectricalControlBlock`, `ElectricPoleBlock`,
   `HeatPipeBlock`, `SingleBlockMachineBlock`, `AdvancedMultiblockBlock`,
   every nuclear controller/port block, etc. - use `MapCodec.unit(() ->
   this)` (an instance-scoped codec that never actually decodes; confirmed
   `MapCodec.unit(Supplier<A>)` exists in `datafixerupper-8.0.16`) since this
   mod never round-trips these blocks through the datapack-driven block
   registry this codec mechanism exists for. `LiquidBlock` (vanilla) already
   implements `codec()` itself - `ModFluids`'s direct `new LiquidBlock(...)`
   calls needed no change here.
2. **Vanilla split `Block.use(state, level, pos, player, hand, hit)` into
   two methods**, confirmed in `BlockBehaviour.java`: `protected
   InteractionResult useWithoutItem(BlockState, Level, BlockPos, Player,
   BlockHitResult)` (no hand/item) and `protected ItemInteractionResult
   useItemOn(ItemStack, BlockState, Level, BlockPos, Player,
   InteractionHand, BlockHitResult)` (item-aware, called first). Confirmed
   the dispatch order by reading `ServerPlayerGameMode.useItemOn`:
   `useItemOn` is **always** called first with the real held stack
   (possibly `ItemStack.EMPTY`); only if it returns
   `ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION` **and**
   `hand == InteractionHand.MAIN_HAND` does `useWithoutItem` fire as a
   fallback - so a block that only cared about `hand != MAIN_HAND ->
   PASS` under the old API needs *no* explicit hand check anymore, just
   move its body into `useWithoutItem` verbatim (the dispatcher already
   gates it to main-hand). Hit **19 files**, three shapes: (a) blocks with
   no item-specific logic (`WindTurbineBlock`, `HandCrankBlock`,
   `PressureTankBlock`, `BoxTransformerBlock`, `BatteryBlock`,
   `CrushingTableBlock`, `ElectricFurnaceBlock`, `ProgrammableBlock`, every
   nuclear controller block) - straight rename to `useWithoutItem`, drop the
   `InteractionHand` param, delete any `hand != MAIN_HAND` guard. (b)
   `NetworkComponentBlock`'s wrench check and its item-aware subclasses
   (`ElectricalControlBlock`'s wrench-rotate,`ElectricalProtectionBlock`'s
   fuse-box, `ConveyorBeltBlock`'s item pickup/drop) - override `useItemOn`
   instead, returning `ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION`
   for "not my item" so the **base class's own `useItemOn` and the
   subclass's `useWithoutItem` still run automatically** - the old manual
   `super.use(...); if (result != PASS) return result;` chaining pattern is
   gone, not needed. (c) `AdvancedMultiblockBlock`/`SingleBlockMachineBlock`,
   which inspect the held item AND need both hands - override `useItemOn`
   directly and never touch `useWithoutItem` at all (since the override
   always returns something other than `PASS_TO_DEFAULT_BLOCK_INTERACTION`,
   the fallback is never reached anyway). `InteractionResult.sidedSuccess`/
   `ItemInteractionResult.sidedSuccess` and a small hand-written switch
   (`SUCCESS/SUCCESS_NO_ITEM_USED -> SUCCESS, CONSUME -> CONSUME,
   CONSUME_PARTIAL -> CONSUME_PARTIAL, PASS -> PASS_TO_DEFAULT_BLOCK_INTERACTION,
   FAIL -> FAIL`) covers converting an inner `InteractionResult` (e.g. from
   a block-entity helper method) to the `ItemInteractionResult` an overridden
   `useItemOn` must return (see `SingleBlockMachineBlock.toItemInteractionResult`).
3. **`BlockEntity.getRenderBoundingBox()` is gone from `BlockEntity`
   entirely** - moved to `net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension`
   (a default method on the **renderer**, `AABB getRenderBoundingBox(T
   blockEntity)`, defaulting to a unit cube at the block's position; `BlockEntityRenderer<T>`
   already extends this interface, confirmed in the sources jar). Any
   block entity with a custom oversized culling box must move that logic to
   its `BlockEntityRenderer` and delete the block-entity-side override -
   hit 5 block entities (`AdvancedMultiblockBlockEntity`,
   `WindTurbineBlockEntity`, `ElectricPoleBlockEntity`/`ElectricConnectorBlockEntity`,
   `NuclearReactorControllerBlockEntity`/`SpentFuelPoolControllerBlockEntity`),
   fixed by adding `getRenderBoundingBox(BE)` overrides to
   `AdvancedMultiblockRenderer`/`WindTurbineRenderer`/`LongDistanceWireRenderer`
   (a generic renderer shared by pole/connector, dispatches via `instanceof`)/
   `NuclearMultiblockRenderer` (shared by reactor/pool controllers, same
   `instanceof` dispatch) respectively, calling through to each block
   entity's pre-existing package-private `static AABB renderBounds(...)`
   helper (widened to `public static` so the `client` package can call it).
4. **`Level.playSound`'s `SoundEvents.*` constants are a mix of plain
   `SoundEvent` and `Holder.Reference<SoundEvent>`**, confirmed in
   `SoundEvents.java`: most use `register(...)` (plain `SoundEvent`), but
   some - `GENERIC_EXPLODE`, `NOTE_BLOCK_BELL`, every `AMBIENT_*` loop, and
   others - use `registerForHolder(...)` instead, returning
   `Holder.Reference<SoundEvent>` and needing an explicit `.value()` before
   passing to any `playSound(..., SoundEvent, ...)` overload. Not a
   version-wide rename - each constant's type must be checked individually.
   Already-ported code calling `NOTE_BLOCK_BELL.value()` had this right;
   one `GENERIC_EXPLODE` call site (missed, not yet needing `.value()`
   under some earlier assumption) was the only bug.
5. **`RecipeManager.getRecipeFor(RecipeType<T>, I, Level)` infers its
   return type invariantly from the `RecipeType`'s own generic parameter**
   (confirmed: `RecipeType.SMELTING` is `RecipeType<SmeltingRecipe>`), so a
   declared receiving type of `Optional<RecipeHolder<AbstractCookingRecipe>>`
   or `Optional<RecipeHolder<? extends AbstractCookingRecipe>>` does **not**
   typecheck against the call's inferred `Optional<RecipeHolder<SmeltingRecipe>>`
   - `Optional<T>` is invariant, and Java's wildcard-containment subtyping
   only helps when the *caller's* declared type carries the wildcard at the
   exact same nesting depth the codec/API exposes it, which a bare
   `RecipeType.SMELTING` call site doesn't. Two fixes depending on whether
   the wildcard type is genuinely needed elsewhere in the file: retype the
   local variable to the concrete `RecipeHolder<SmeltingRecipe>` (no other
   recipe type used in that file - `SingleBlockThermalLogic`), or keep the
   wildcard-typed API and widen with `.map(Function.identity())` right at
   the call site (`ElectricFurnaceProcessModule`, whose `findRecipe` return
   type other callers rely on).
6. **`BonemealableBlock.isValidBonemealTarget` dropped its trailing
   `boolean` parameter** - now `(LevelReader, BlockPos, BlockState)`, 3-arg
   (confirmed in the interface). `performBonemeal`'s first parameter is
   `ServerLevel` (unchanged shape, just check the call site already has one
   in scope, as `SingleBlockAutomationLogic.serviceSprinklerColumn` does).
7. Misc one-off vanilla renames hit for the first time this phase, none
   previously in the cheat sheet: `Blocks.GRASS` -> `Blocks.SHORT_GRASS`
   (the short plant was renamed in 1.20.3+, freeing `grass` up); `Player`/
   `Entity.setSecondsOnFire(int)` -> `igniteForSeconds(float)`; `LiquidBlock`
   has no `getFluid()` method, the fluid is the **public field**
   `LiquidBlock.fluid` (confirmed in `LiquidBlock.java`); `PoseStack.mulPoseMatrix(Matrix4f)`
   -> `mulPose(Matrix4f)` (already known from Phase 24, hit again in
   `LegacySceneRenderer`); `ContainerEventHandler`/`GuiEventListener.mouseScrolled`
   gained horizontal-scroll support, now `(double mouseX, double mouseY,
   double scrollX, double scrollY)` (4-arg, was 3-arg) - `GuideScreen`
   needed its override retyped and its own scroll-consuming helpers fed
   `scrollY`. A `DeferredRegister<T>.register(String, ...)` call passing a
   constructor reference (`X::new`) is **ambiguous between the `Supplier`
   and `Function<ResourceLocation,...>` overloads whenever the target class
   has both a no-arg and a 1-arg constructor** (`VoltmeterItem`/`ThermometerItem`
   each have `()` and `(Properties)`) - javac's method-reference
   applicability check only compares arity, not parameter types, at the
   overload-selection phase, so it can't rule out the 1-arg `Function`
   overload even though no constructor actually takes a `ResourceLocation`;
   fix by writing an explicit `() -> new X()` lambda instead of `X::new`
   (a 0-arg lambda can't satisfy a 1-arg SAM at all, so the ambiguity
   disappears). `new LiquidBlock(fluidHolder, props)`/`new
   BucketItem(fluidHolder, props)` both want the resolved `Fluid`/`FlowingFluid`
   directly, not the `DeferredHolder` - call `.get()` inside the registration
   lambda (safe: NeoForge fires `RegisterEvent` for `FLUID` before `BLOCK`/
   `ITEM`, the same guarantee vanilla itself relies on for `Blocks.WATER`/
   `Blocks.LAVA`); confirmed this is literally NeoForge's own documented
   `LiquidBlock` registration idiom, not a workaround.
8. A private helper method literally named `invalidateCapabilities()` on a
   `BlockEntity` clashes with `IBlockEntityExtension.invalidateCapabilities()`
   (a public default method) - Java refuses to let a `private` method
   "override" a `public` one even though it isn't really an override (no
   `@Override` annotation, different visibility). Renamed the helper
   (`NuclearThermalPortBlockEntity.refreshCapabilities()`) - if this
   surfaces again, it's the same clash, not a new bug.

**Runtime crash after the first `./gradlew build` succeeded**: launching
the built jar and creating a world threw
`NuclearDataReloadException: Nuclear data is missing required built-in fuel
definitions` from `NuclearDataRegistry.apply` - not a code bug.
`NuclearDataReloadListener` (ported in Phase 1) loads fuel definitions from
datapack JSON at `data/magneticraft/magneticraft/nuclear_fuels/*.json`,
and `NuclearDataProvider` (already correctly ported datagen code, in
`data/`) generates exactly those files - but **`./gradlew runData` (the
NeoForge datagen run) had simply never been executed against this project**,
so `src/generated/resources` (wired as a resource `srcDir` in
`build.gradle`) was empty. Running `./gradlew runData` once generated all
2277 datagen outputs including the three required fuel JSONs; a following
`./gradlew build` packaged them into the jar (confirmed via `unzip -l` on
the built jar). **This is a standing gap, not a one-time fluke**: any
future code change to a `DataProvider` (recipes, tags, models, this nuclear
fuel data, the guide book data, etc.) needs `./gradlew runData` re-run
before the change is visible in a built/launched jar - `compileJava`/`build`
alone do not regenerate `src/generated/resources`. Confirmed by checking the
Forge source's own `src/generated/resources/data/magneticraft/magneticraft/nuclear_fuels/*.json`
(present there, byte-shape-equivalent field names) - this was always a
generated-not-authored resource, never something to hand-write or copy.

**Two more runtime-only bugs found after that, once world creation itself
succeeded** - neither is a compile error, so neither would ever surface
except by actually launching the game and looking:

1. **`AdvancedWorldgenProvider`'s hand-built `CLAMPED_NORMAL` count JSON
   nested the `IntProvider`'s fields one level too deep.** Vanilla's
   `ClampedNormalInt.CODEC` (confirmed in the decompiled source) puts
   `mean`/`deviation`/`min_inclusive`/`max_inclusive` as direct siblings of
   `"type"` - the provider code wrapped them in an extra `"value": {...}`
   object instead, which made `RegistryDataLoader` fail to parse
   `worldgen/placed_feature/limestone.json` at world-creation time with
   "Failed to parse either... No key mean/deviation/min_inclusive/max_inclusive".
   This is a **datagen-provider bug, not a code-vs-vanilla-API bug** - the
   generated JSON was simply hand-assembled wrong. Also caught in the same
   provider: **the biome-modifier type and datapack directory are still
   Forge's `forge:add_features`/`data/.../forge/biome_modifier/...`** -
   confirmed via `net.neoforged.neoforge.common.world.BiomeModifiers`'s own
   javadoc and `NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS`/`BIOME_MODIFIERS`
   (both keyed under the `neoforge:` namespace via `NeoForgeVersion.MOD_ID`)
   that both the JSON `"type"` value and the registry's datapack directory
   segment must be `neoforge:add_features` / `neoforge/biome_modifier`, not
   the Forge-era `forge:`. **New cheat-sheet entry**: `forge:` -> `c:` was
   already documented for cross-loader *tags* (Phase 16); this is the same
   rename but for a different thing - NeoForge's *own* registries/serializers
   (biome modifiers, and likely other NeoForge-registered dynamic-registry
   content) moved to the `neoforge:` namespace instead, not `c:`. Don't
   assume `forge:` always maps to `c:` - check which case it is.
2. **`TextureAtlasSprite.getU(float)`/`getV(float)` changed contract between
   1.20.1 and 1.21.1**: confirmed by decompiling both `TextureAtlasSprite.java`
   (now `float f = u1 - u0; return u0 + f * u;` - expects `u` as an
   already-normalized 0-1 fraction of the sprite's own width) and vanilla's
   own `FaceBakery.java` (now does `sprite.getU(blockFaceUV.getU(vertexIndex) / 16.0F)`
   at the *call site* - i.e. the `/16` that used to happen inside `getU`
   moved to the caller, because `BlockFaceUV`'s own values are still 0-16
   pixel-space). `LegacySceneGeometry.bakePrimitive`/`LegacySceneRenderer.renderPrimitive`
   (Phase 19/Phase 24) both still had the *old* 1.20.1-shaped call -
   `sprite.getU(textureCoordinates[...] * 16.0F)` - ported byte-for-byte
   unchanged from the Forge original, which was correct there because old
   `getU` divided by 16 internally. Since `textureCoordinates[]` (from both
   the glTF and MCX parsers) are already 0-1 normalized UV fractions, the
   old code was now multiplying by 16 into a method that no longer divides
   back down - sampling up to 16x past the intended sprite's own UV box,
   into whatever neighboring tiles happen to sit in the stitched atlas.
   Visually this looks like the model's faces are covered in a "quilt" of
   many unrelated textures at once - not a missing-texture checkerboard,
   not a solid wrong color, but a jumble, because the wildly out-of-range
   U/V wraps across many different sprites' regions depending on where they
   land in the atlas. **This is exactly the kind of bug the whole project's
   "verified by reading, not compiled" caveat warned about**: it compiled
   fine in every phase (both `getU(float)` signatures accept a plain
   `float`, so nothing type-checks differently), it matched the Forge
   original 1:1 (so a diff against the source tree shows nothing wrong), and
   it only manifests by actually rendering a legacy-scene model in game.
   Fix: drop the `* 16.0F` - pass `textureCoordinates[...]` straight to
   `getU`/`getV`. **If any other renderer in this codebase calls
   `TextureAtlasSprite.getU`/`getV` directly (grep confirmed only these two
   call sites exist as of this session), apply the same fix** - any future
   custom renderer added to this mod must pass a plain 0-1 fraction, never
   a pre-multiplied pixel-space value.
3. **`ItemStack.save(HolderLookup.Provider)` now throws
   `IllegalStateException("Cannot encode empty ItemStack")` instead of
   silently no-oping** (confirmed in the decompiled `ItemStack.java`; the
   1.20.1-era method tolerated an empty stack). `GhostFilterModule.save`
   called `.save(registries)` unconditionally over every filter slot,
   including empty ones (a ghost filter is *usually* empty - it's a
   template slot, not a real item) - this crashed the moment
   `BlockSnapshot.create` (fired by `IBlockExtension.onDestroyedByPlayer`,
   i.e. **breaking any block that hosts a `GhostFilterModule`**) tried to
   serialize the block entity's NBT for the drop event. Fix: use
   `ItemStack.saveOptional(HolderLookup.Provider)` instead - confirmed in
   `ItemStack.java`, it's the existing vanilla method for exactly this case
   (`this.isEmpty() ? new CompoundTag() : this.save(...)`), a drop-in
   replacement wherever a stack in the collection being serialized isn't
   already known to be non-empty. Audited every other `.save(registries)`
   call site on an `ItemStack` in `content/` (11 total): 6 were already
   guarded by an explicit `if (!stack.isEmpty())` (safe, left alone);
   `PneumaticEndpointModule.saveQueue`, `ConveyorBeltModule.Parcel.save`,
   and `LogisticsTubeModule.TravelingItem.save` serialize queues/lists of
   in-transit items that are never *supposed* to hold an empty stack by
   construction, but switched to `saveOptional` anyway as free defense in
   depth against the same crash class. **New cheat-sheet entry**: any
   `ItemStack` field/slot that can legitimately be empty (a filter, an
   optional display item, a "may or may not have a fuse installed" slot)
   must use `saveOptional`, not `save`, when persisting - `save` alone is
   now only safe directly after an `isEmpty()` guard.

## What's left (by size, descending)

| Package | Files | Notes |
|---|---|---|
| `content/` | **0 remaining** | **Fully closed as of Phase 15** (303/303) - `content/machine/{windturbine,electricfurnace,battery,crushingtable,observation}`, `content/command`, `content/recipe` were the last 23 files. Every `content/` subsystem is now closed. |
| `init/` | **0 remaining** | **Fully closed as of Phase 16** (19/19) - every `Mod*.java` registration file is ported, including the `RegisterCapabilitiesEvent` wiring the last several phases deferred. A full-tree grep confirms zero remaining `net.minecraftforge`/`RegistryObject` references anywhere in `src/main/java`. |
| `client/` | **0 remaining** | **Fully closed as of Phase 24** (53/53) - every renderer, screen, and the `ClientSetup` registration file are ported. See Phase 24 above for the `GuideScreen` background-ordering pattern, `PoseStack.mulPose(Matrix4f)` rename, the shrunk `VertexConsumer` override list, `RegisterMenuScreensEvent` replacing `MenuScreens.register`, and `RegisterGeometryLoaders.register` now taking a `ResourceLocation`. |
| `data/` | **1 remaining** | **23/24 done as of Phase 19.** `LegacyInventoryTransform.java`/`LegacySceneModelBuilder.java` were blocked on `client/model/` (see Phase 17's known-gap writeup) - both ported in Phase 19 once `client/model/` closed (one real API change: `CustomLoaderBuilder`'s constructor gained a `boolean allowInlineElements` param). `data/recipe/CountedCookingRecipeBuilder.java` was intentionally **not** ported (made obsolete by vanilla's own counted `SimpleCookingRecipeBuilder`, see Phase 17) - this is the last file in `data/`, and it should stay unported. |
| `integration/` | **0 remaining** | **Fully closed as of Phase 18** (17/17) - JEI, Jade, CraftTweaker, Tinkers' Construct all ported and wired into `build.gradle`/`neoforge.mods.toml`/`ModDataGenerators`. See Phase 18 above for the JEI `RecipeHolder`/subtype-interpreter rewrites and the `FluidStack.getComponentsPatch()` finding. |
| `api/` | 0 remaining | **Fully closed as of Phase 12** (8/8) - `NuclearReactorColumnType` was the last one, ported this session once `content/nuclear/fuel/NuclearFuelGrade` existed. |

`system/` (98/98) and the root `network/` packet-message package (6/6) were
already fully closed before Phase 12 and remain so.

**`src/main/java` is CLOSED - 0 files remaining** (530/530; the only
"missing" file is the intentionally-skipped `data/recipe/CountedCookingRecipeBuilder.java`,
which should stay unported - see `data/` above). `content/`, `init/`,
`client/`, `system/`, `api/`, `integration/`, and the root `network/`
package are all fully closed. What's left in the whole repo: all of
`src/gametest`, `src/test`, `src/integrationTest`, `src/generated`, and
`src/main/resources` (assets/data - 653 files, untouched) - none of these
were ever in scope for the "530 main-source files" this document tracks.
Run `find` diffs between the two `src/main/java/.../magneticraft` trees to
re-confirm parity if this claim is ever in doubt.

## Forge -> NeoForge API cheat sheet (verified against the actual
NeoForge `1.21.1` branch source, not docs summaries which drift)

| Forge (1.20.1) | NeoForge (1.21.1) | Notes |
|---|---|---|
| `net.minecraftforge.eventbus.api.IEventBus` | `net.neoforged.bus.api.IEventBus` | |
| `net.minecraftforge.fml.common.Mod` | `net.neoforged.fml.common.Mod` | |
| `FMLJavaModLoadingContext.get().getModEventBus()` | mod constructor param `IEventBus modBus` | constructor is `MyMod(IEventBus modBus, ModContainer container)`, both injected by FML |
| `ModLoadingContext.get().registerConfig(...)` | `container.registerConfig(...)` | `ModContainer` from `net.neoforged.fml.ModContainer` |
| `@Mod.EventBusSubscriber(modid=, bus=Bus.FORGE/MOD)` | `@net.neoforged.fml.common.EventBusSubscriber(modid=, bus=EventBusSubscriber.Bus.GAME/MOD)` | top-level annotation now, not nested; `FORGE` bus renamed `GAME`. **`bus()` is now deprecated and ignored** - the actual bus is auto-detected per listener method from whether the event type implements `IModBusEvent`; kept in this port for consistency with earlier phases, not because it's required. `value()` (a `Dist[]`, default both sides) is the live replacement for the Forge original's third `value = Dist.CLIENT` argument - `net.minecraftforge.api.distmarker.Dist` -> `net.neoforged.api.distmarker.Dist` (a pure rename, but note it and `EventBusSubscriber` itself live in the separate `net.neoforged.fancymodloader:loader` Gradle module, not in the `neoforge` artifact - confirmed by an empty grep of `neoforge-*-sources.jar`'s file listing for both packages). Phase 20, `client/electrical/ClientElectricalEvents.java`. |
| `net.minecraftforge.event.TickEvent.ClientTickEvent` (+ `.phase` field, gated on `TickEvent.Phase.END`) | `net.neoforged.neoforge.client.event.ClientTickEvent.Pre` / `.Post` (separate classes, no phase field) | same `Pre`/`.Post` split as the already-documented `PlayerTickEvent`/`EntityTickEvent`/`LevelTickEvent`/`ServerTickEvent`, just under `client.event` instead of `event.tick` - easy to miss since it's a different package. A Forge original gated on `Phase.END` should subscribe to `.Post`; `Phase.START` maps to `.Pre`. Phase 20, `client/electrical/ClientElectricalEvents.java`. |
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
| `ItemStack.getTag()`/`.setTag(CompoundTag)`/`.getOrCreateTag()`/`.hasTag()`/`.addTagElement(String,Tag)`/`.removeTagElement(String)` (vanilla, not Forge/NeoForge) | **Gone entirely**, no overload survives. Use `net.minecraft.world.item.component.CustomData` via `DataComponents.CUSTOM_DATA`: read with `stack.get(DataComponents.CUSTOM_DATA)` (`@Nullable`) + `.copyTag()`; write/mutate with the static helper `CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> ...)`; clear with `stack.remove(DataComponents.CUSTOM_DATA)`. For the specific well-known Forge key `"BlockEntityTag"`, the vanilla replacement is the dedicated `DataComponents.BLOCK_ENTITY_DATA` component instead (same `CustomData` wrapper) - see `BlockItem.setBlockEntityData`/`BlockEntity.saveToItem` in the decompiled vanilla source. **Caught two live regressions from this** (Phase 10): `system/network/electric/item/TieredElectricalItemData.write/read` (Phase 1) and `content/multiblock/AdvancedMultiblockBlockEntity.saveToItem` (Phase 4), both fixed. If you spot another `.getTag()`/`.setTag()`/`.getOrCreateTag()`/`.addTagElement()`/`.hasTag()` call on an `ItemStack` anywhere, it's the same bug. |
| `net.neoforged.neoforge.fluids.FluidStack.getTag()` (returning `CompoundTag`) | **Gone**, same "NBT -> data component" story as `ItemStack.getTag()` below, just for fluids. Use `FluidStack.getComponentsPatch()` (returns `DataComponentPatch`) instead - confirmed present, same shape/purpose as `ItemStack.getComponentsPatch()`. Hit in Phase 18's JEI `GasificationRecipeCategory`/`PolymerizerRecipeCategory` (JEI's own `IRecipeSlotBuilder.addFluidStack(Fluid, long, ...)` third-arg type changed from `CompoundTag` to `DataComponentPatch` in lockstep). If you spot another `.getTag()` call on a `FluidStack` anywhere (not yet seen in `content/`/`client/`, but check), it's the same bug. |
| `net.minecraftforge.common.TierSortingRegistry` | **Gone, no replacement class** (confirmed absent from the whole `neoforge-21.1.208-sources.jar`). Vanilla tool-tier gating moved into the `Tool` data component (`Tier.createToolProperties(TagKey<Block>)`, read by the default `Item.isCorrectToolForDrops(ItemStack, BlockState)`). A custom `Item` that overrides `isCorrectToolForDrops(ItemStack, BlockState)` directly (no `Tool` component attached) should compare against `Tier.getIncorrectBlocksForDrops()` directly instead, e.g. `!state.is(Tiers.DIAMOND.getIncorrectBlocksForDrops())` - see Phase 10's `ElectricToolItem`. The old Forge-added 1-arg `Item.isCorrectToolForDrops(BlockState)` override point is also gone; only the 2-arg form remains. |
| `Item.appendHoverText(ItemStack, @Nullable Level, List<Component>, TooltipFlag)` (vanilla) | `Item.appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)` | signature change only (`Item.TooltipContext` has `registries()`/`tickRate()`/`mapData(MapId)`, plus an `EMPTY` constant) - a tooltip that never used the `Level` parameter just retypes it and ignores it, see Phase 10's `PortableEnergyItem`. |
| `LivingEntity.broadcastBreakEvent(EquipmentSlot)` / `.broadcastBreakEvent(InteractionHand)`, `ItemStack.hurtAndBreak(int, LivingEntity, Consumer<LivingEntity>)` (vanilla) | **Gone.** Use `ItemStack.hurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot)` (calls the new `LivingEntity.onEquippedItemBroken(Item, EquipmentSlot)` internally - no lambda needed). To get an `EquipmentSlot` from an `InteractionHand` (e.g. `UseOnContext.getHand()`), use the existing vanilla static `LivingEntity.getSlotForHand(InteractionHand)`. Hit in Phase 10's `HammerItem`/`ElectricalRepairToolItem` - **re-check any other file calling `hurtAndBreak` with a lambda third argument**, same bug. |
| `net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(id)` (nullable) | `net.minecraft.core.registries.BuiltInRegistries.FLUID.get(id)` | `BuiltInRegistries.FLUID` is a vanilla `DefaultedRegistry<Fluid>` whose `get(ResourceLocation)` is `@Nonnull` and falls back to `Fluids.EMPTY` for an unknown id - it **never returns null**. A ported `== null` check must become `!BuiltInRegistries.FLUID.containsKey(id)` instead, or it silently never triggers. Same caveat applies to any other vanilla `DefaultedRegistry` (blocks, items, entity types, ...). See Phase 10's `PressureGaugeItem`. |
| Item capabilities: no Forge equivalent existed as cleanly as this - closest was `Item.initCapabilities(ItemStack, CompoundTag)` returning an `ICapabilityProvider`/`ICapabilitySerializable` | `net.neoforged.neoforge.capabilities.ItemCapability<T, C>` (e.g. `Capabilities.EnergyStorage.ITEM`, an `ItemCapability<IEnergyStorage, @Nullable Void>`), registered via `RegisterCapabilitiesEvent.registerItem(capability, (stack, ctx) -> ..., items...)`. **Structurally different from block capabilities**: no per-stack cached provider, no serialize/deserialize step - the provider is called fresh per query and is handed the live `ItemStack`, so it can read/write a `DataComponentType` on that exact stack directly (`ItemStack.set()`/`.get()`/`.remove()` mutate the stack's component map in place, confirmed in decompiled `ItemStack.java`). Any per-stack mutable state a capability view needs now has to be its own `DataComponentType`, since there's no more generic per-item NBT to lean on. See Phase 10's `PortableEnergyItem`/`PortableEnergyComponents` for the full worked example, including the `DeferredRegister<DataComponentType<?>>` (`Registries.DATA_COMPONENT_TYPE`) registration shape. |
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
| `net.minecraftforge.fluids.ForgeFlowingFluid` (+ nested `Flowing`/`Source`/`Properties`) | `net.neoforged.neoforge.fluids.BaseFlowingFluid` | pure rename - identical shape confirmed in the sources jar, same `Properties(Supplier<FluidType>, Supplier<Fluid> still, Supplier<Fluid> flowing).block(...).bucket(...)` builder. Phase 16, `init/ModFluids.java`. |
| `new StairBlock(Supplier<BlockState>, BlockBehaviour.Properties)` (vanilla, 1.20.1) | `StairBlock(BlockState, BlockBehaviour.Properties)` | vanilla change, not Forge/NeoForge - the supplier overload is gone; pass the resolved `BlockState` directly (e.g. `base.get().defaultBlockState()`). Confirmed in decompiled 1.21.1 vanilla source. Phase 16, `init/ModBlocks.java`. |
| `net.minecraftforge.common.Tags.{Items,Blocks,Fluids}` under the `forge:` namespace | `net.neoforged.neoforge.common.Tags` under the **`c:`** namespace (`neoforge:` for NeoForge-only tags) | NeoForge's cross-loader "common" tag convention moved off `forge:` entirely - confirmed in the sources jar (e.g. `Tags.Items.INGOTS_COPPER` = `c:ingots/copper`). Path segments are otherwise unchanged **except the wrench tag, now singular** (`c:tools/wrench`, not the Forge original's `forge:tools/wrenches`). Phase 16, `init/ModTags.java`. |
| `net.minecraftforge.common.extensions.IForgeMenuType.create(factory)` | `net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(factory)` | pure rename, identical static factory signature `<T extends AbstractContainerMenu> MenuType<T> create(IContainerFactory<T> factory)`, confirmed in the sources jar. Phase 16, `init/ModMenus.java`. |
| `stack.getCapability(ForgeCapabilities.ENERGY)` (or any other capability) returning `LazyOptional<T>`, consumed via `.map(...).orElse(...)`/`.ifPresent(...)` | `stack.getCapability(Capabilities.EnergyStorage.ITEM)` returns a plain **`@Nullable T` directly** | confirmed via `ItemCapability.getCapability`/`IItemStackExtension.getCapability` in the sources jar - there is no `Optional`/`LazyOptional` wrapper at all in the new model, for item capabilities same as block capabilities. Rewrite every `.map(...).orElse(...)`/`.ifPresent(...)` chain to a plain null check. First hit in Phase 15's `BatteryBlockEntity` (a capability *consumer*, not just a provider like every earlier item-capability file); hit again in Phase 16's `init/ModCreativeTabs.java`. |
| `RegisterCapabilitiesEvent` provider wiring | fired on the **mod event bus** (`RegisterCapabilitiesEvent implements IModBusEvent`) | register listeners the same way as any other registration event: `modBus.addListener(SomeClass::registerCapabilities)` from the composition root (`ModRegistries.register(IEventBus modBus)` in this project). Phase 16 finally wired up every `MachineCapabilities.register(...)` call the last several phases had deferred as a documented gap - see `init/ModBlockEntities.registerCapabilities`/`init/ModComputerContent.registerCapabilities`. |
| `AbstractWidget.setTooltipDelay(int millis)` (vanilla) | `AbstractWidget.setTooltipDelay(java.time.Duration)` | vanilla change, not Forge/NeoForge - confirmed in decompiled 1.21.1 source (`this.tooltip.setDelay(tooltipDelay)`). Fix: `setTooltipDelay(Duration.ofMillis(150))`. `Tooltip.create(Component, Component)`, `AbstractButton`'s constructor/`renderWidget`/`onPress`, and `AbstractWidget`'s `updateWidgetNarration`/`defaultButtonNarrationText`/`isHoveredOrFocused`/protected `width`/`height` fields are all otherwise unchanged. Phase 21, `client/MachineIconButton.java` - watch for more `setTooltipDelay(int)` call sites in the remaining screen files. |
| `VertexConsumer.vertex(Matrix4f,x,y,z)` chained onto a pre-extracted `Matrix4f`/`Matrix3f normal` pair (old builder style, see the Phase 19 entry above) | prefer `VertexConsumer.addVertex(PoseStack.Pose pose, x, y, z)` + `.setNormal(PoseStack.Pose pose, x, y, z)` over the raw `Matrix4f`/`Matrix3f` overloads whenever the calling code already has a live `PoseStack`/`PoseStack.Pose` in hand | both are confirmed default methods on `com.mojang.blaze3d.vertex.VertexConsumer` in the decompiled 1.21.1 sources - `addVertex(pose, ...)` internally calls `pose.pose()` then the `Matrix4f` overload, `setNormal(pose, ...)` internally calls `pose.transformNormal(...)` then the raw 3-float `setNormal`. Simpler than manually extracting `Matrix4f`/`Matrix3f` locals up front (Phase 19's approach) - only fall back to the raw matrix-accepting overloads when the code has no live `PoseStack` at all (e.g. bake-time geometry code building a `BakedQuad`, as in Phase 19's `LegacySceneGeometry`). Phase 21, `client/MachineRenderHelper.java`. |

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
   files)**, **`content/network/` is done in full (78/78 files -
   Phases 1/3/5/6/7/8/9)**, **`content/item/` is done in full (25/25
   files, Phase 10)**, **`content/machine/singleblock/` is done in full
   (25/25 files, Phase 11)**, **`content/world/` is done in full (1/1
   file, `ProtectedWorldMutation`, Phase 11)**, **`content/nuclear/` is
   done in full (63/63 files, Phase 12)**, **`content/worldgen/` is
   done in full (7/7 files, Phase 13)**, and **`content/computer/` is
   done in full (28/28 files, Phase 14)**, and **`content/` is now closed
   in full (303/303 files, Phase 15 - the last 23 files were the small
   standalone machine families `content/machine/{windturbine,
   electricfurnace, battery, crushingtable, observation}` plus
   `content/command`/`content/recipe`)**. `api/` also closed in full
   (8/8) once `NuclearReactorColumnType` unblocked (Phase 12).
1a. ~~`init/`~~ - **done in full (19/19 files, Phase 16)**, including the
   `RegisterCapabilitiesEvent` wiring (`ModBlockEntities.registerCapabilities`/
   `ModComputerContent.registerCapabilities`) that earlier phases deferred.
   See Phase 16 above for the `StairBlock`/`BaseFlowingFluid`/`c:`-tag-namespace/
   `IMenuTypeExtension` discoveries.
4. ~~`client/`~~ - **done in full (53/53 files, Phase 24)**. `client/model/`
   (14/14, Phase 19), `client/electrical/` (2/2, Phase 20), the 7
   foundational root-level helpers (Phase 21), almost every renderer plus
   the guide-book subsystem (Phase 22), 7 screens (Phase 23), and the final
   9 files - `GuideScreen`/`ElectricalDeviceScreen`/`ProgrammableScreen`/
   `SingleBlockMachineScreen`/`AdvancedMultiblockScreen`/`NuclearReactorScreen`/
   `SingleBlockMachineRenderer`/`AdvancedMultiblockRenderer`/`ClientSetup`
   (Phase 24) closed it out. See Phase 24 above for the `GuideScreen`
   background-ordering exception to the Phase 23 `renderBackground`-deletion
   rule, `PoseStack.mulPose(Matrix4f)`, the shrunk `VertexConsumer` override
   list, `RegisterMenuScreensEvent` replacing the now-private
   `MenuScreens.register`, and `RegisterGeometryLoaders.register` taking a
   `ResourceLocation`. **This closed `src/main/java` in full - see Phase 24's
   summary for the whole-tree confirmation.**
5. ~~`data/`~~ - **23/24 files done (Phase 17 + 19); only the intentionally-skipped
   file remains.** The recipe-datagen `Consumer<FinishedRecipe>` -> `RecipeOutput`
   rewrite (see Phase 17 above) was the biggest single discovery in the port
   so far. `LegacyInventoryTransform.java`/`LegacySceneModelBuilder.java` were
   blocked on `client/model/` - both ported in Phase 19 once that package
   closed (one real API change: `CustomLoaderBuilder`'s constructor gained a
   `boolean allowInlineElements` param). `CountedCookingRecipeBuilder.java`
   was correctly *not* ported (obsolete) and should stay that way.
6. ~~`integration/`~~ - **done in full (17/17 files, Phase 18)**, ported
   ahead of `client/` at the user's request. See Phase 18 above for the JEI
   `useNbtForSubtypes`/`RecipeHolder`/`FluidStack.getComponentsPatch()`
   discoveries and the `build.gradle`/`neoforge.mods.toml` wiring.
7. `src/main/resources/assets` and `/data` (653 files) - **copied over
   verbatim, byte-for-byte identical to the Forge source** (`diff -rq`
   against both directories is empty). This was a plain file transfer only,
   done at the user's explicit request ("just transfer, no need to modify")
   - **the content has NOT been re-verified to line up with the ported
   registry/recipe/model-loader names** (e.g. the Phase 4 note that the new
   `AdvancedProcessingRecipe`/`PolymerizerRecipe` JSON shape is deliberately
   **not** 1:1 with the old Forge JSON - existing recipe JSON under
   `data/magneticraft/recipes/` almost certainly still reflects the old
   shape and needs rewriting or datagen regeneration). `META-INF/neoforge.mods.toml`,
   `pack.mcmeta` (already bumped to `pack_format 34`), and `logo.png` were
   already correctly present in the destination from earlier phases and
   were **not** touched by this transfer. **Re-verifying this content
   against the ported code is still an open task** - do it before or during
   the first real `./gradlew build`, which is also when to double-check
   Phase 4's two codec-based recipe serializers (never compiler-verified -
   see Phase 4's caveat) and the `init/ModRegistries.java` vanilla
   `Registries.*` constant names (never re-verified against decompiled
   1.21.1 vanilla source - see Phase 2's caveat).
