# Quality Guidelines

> Definition of done for the Magneticraft Forge 1.20.1 Java port.

## Language and design

- Production code is Java 17 only. Do not add Kotlin sources, the Kotlin runtime
  or Kotlin-facing public contracts.
- Apply KISS and YAGNI: migrate confirmed 1.12 behavior with the smallest modern
  Forge design that supports it. Do not pre-build unrequested datapack or addon
  frameworks.
- Apply DRY after a pattern is demonstrated across multiple consumers. Avoid a
  global base class that accumulates unrelated machine behavior.
- Keep responsibilities narrow: registry declarations, simulation, persistence,
  menu synchronization and rendering are separate concerns.
- Depend on small Java interfaces at system boundaries. Forge Energy, item and
  fluid capabilities are adapters around Magneticraft's independent electricity,
  heat, pressure and logistics models.

## Required patterns

- Use `DeferredRegister` and registry holders for static registries.
- Use stable `ResourceLocation` IDs and `snake_case` resource paths.
- Keep all world mutations and authoritative simulation on the logical server.
- Use datagen for models, blockstates, tags, loot tables, language and recipes
  whenever a provider exists.
- Use static models for static geometry and `BlockEntityRenderer` only for
  genuinely dynamic machine state.
- Add provenance before adapting third-party code. Nova 1.12 is the behavioral
  authority; the official 1.14 branch is transition reference; MochiButter code
  may be reused only after GPL/provenance review; hypersmc Magneticraft2 is
  architecture-only and must not be copied.
- Public APIs remain internal until intentionally documented and frozen for 1.0.

## Forbidden patterns

- Mechanical translation of 1.12 APIs, metadata variants, proxies, `IHasModel`,
  `IGuiHandler`, TESR or `SimpleNetworkWrapper`.
- Client classes in common code, client-authoritative state or unrestricted
  client-to-server mutation packets.
- Gameplay state held only in static mutable collections.
- A static initializer that queries loader state or registries before the
  appropriate lifecycle phase.
- Raw registry lookups scattered through gameplay logic; inject or reference
  registry holders instead.
- Lombok for core domain state, wildcard dependency versions, unused optional
  integrations, or mixins without a documented API gap.
- Source or assets copied from `hypersmc/magneticraft2-1.20.x`.

## Scenario: Stable registry IDs and migration projection closure

### 1. Scope / trigger

Apply whenever registered content is added, renamed or removed, or when a
metadata variant from Nova 1.12 is assigned a Forge 1.20.1 target. The 0.2.0
reset is the only style-driven breaking rename; every ID is frozen after it.

### 2. Signatures

```java
BLOCKS.register("battery_box", () -> new BatteryBlock(...));
ITEMS.register("battery_box", () -> new BlockItem(BATTERY_BOX.get(), ...));
```

```json
{
  "legacy_id": "battery_item_low",
  "target_id": "low_voltage_battery",
  "disposition": "retain_rename"
}
```

### 3. Contracts

- Material forms use `<material>_<form>` and tiered devices use
  `<tier>_<device>`. Do not add `item`, `block`, `big` or `rf` when the domain
  meaning is clear without it.
- A block and its `BlockItem` share one semantic ID. Its block entity, menu and
  recipe type derive an equally specific content name instead of an unrelated
  alias.
- `docs/porting/migration-matrix.json` assigns every published legacy registry
  entry, metadata variant, recipe and guide to exactly one of
  `retain_rename`, `rebuild` or `exclude`. `registry-id-map.json` is its
  executable registry projection.
- Every retained or rebuilt row identifies its target data paths and acceptance
  cases. Exclusions have no target path and state the evidence and acceptance
  case that keep the prototype unavailable.
- Runtime aliases for pre-0.2.0 IDs are forbidden. Later releases may add
  entries and explicit forward data migrations, but must not rename a frozen
  ID for style.

### 4. Validation and error matrix

| Condition | Required result |
|---|---|
| One legacy leaf has zero or multiple dispositions | Fail the matrix contract |
| A retained target path is absent | Fail before runtime testing |
| A current registry entry is missing from the projection | Add it to the mapping or supporting-ID manifest |
| A forbidden pre-0.2.0 ID resolves at runtime | Fail; remove the alias |
| A block and its item use different semantic IDs | Fail; register both under the block ID |
| Second `runData` changes a path or content hash | Fail; repair provider determinism |

### 5. Good, base and bad cases

- Good: `battery_item_low` maps once to `low_voltage_battery`, every referenced
  resource exists, and only the new ID resolves in the item registry.
- Base: an internal structure gap is documented as `exclude`, has no generated
  resource and cannot be obtained through a registry entry.
- Bad: an old ID remains as a compatibility alias, or a generated recipe still
  points at a renamed path that no longer exists.

### 6. Tests required

- Plain JUnit validates leaf identity, projection hashes, disposition
  partitions, target-path closure, acceptance-case routing and forbidden-ID
  parity.
- GameTest compares definition catalogues with the live Forge registries,
  verifies every fluid family member, and proves all forbidden IDs are absent.
- Run datagen twice, compare non-cache path and content manifests, then inspect
  the built JAR for stale paths and `.cache` files.
- Scan base client and dedicated-server logs for missing mappings, duplicate
  registrations, missing models and optional-mod linkage.

### 7. Wrong vs correct

```java
// Wrong: ambiguous pre-freeze vocabulary and a second item alias.
BLOCKS.register("box", WoodenCrateBlock::new);
ITEMS.register("box_item", () -> new BlockItem(BOX.get(), properties));

// Correct: one stable semantic ID shared by block and BlockItem.
BLOCKS.register("wooden_crate", WoodenCrateBlock::new);
ITEMS.register("wooden_crate", () -> new BlockItem(WOODEN_CRATE.get(), properties));
```

## Scenario: Forge fluid client lifecycle and resource closure

### 1. Scope / trigger

Apply this contract whenever a `FluidType`, `LiquidBlock`, fluid bucket, or
custom fluid texture is added. It prevents constructor-time null access and
client-visible missing models or sprites.

### 2. Signatures

```java
@Override
public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer)
```

```java
atlas(BLOCKS_ATLAS).addSource(
        new SingleFile(Magneticraft.id("fluid/oil_still"), Optional.empty())
);
simpleBlock(fluidBlock, particleOnlyModel);
```

### 3. Contracts

- Forge 1.20.1 calls `FluidType#initializeClient` from the `FluidType`
  constructor. An override must use constants or state already initialized by
  the superclass, such as an explicit description ID; it must not read subclass
  fields assigned after `super(...)` returns.
- Every still and flowing texture returned by the client extension must be a
  source in Minecraft's `blocks` atlas. Keeping textures under
  `textures/fluid` therefore requires a `SpriteSourceProvider` entry.
- Every registered `LiquidBlock` needs a default blockstate and a valid model.
  A particle-only model is sufficient because the liquid renderer owns world
  geometry; the empty variant covers all `level=0..15` states.
- Common fluid code may use Forge's `IClientFluidTypeExtensions` callback, but
  must not reference `net.minecraft.client.*`. A dedicated server must load the
  complete registry without a client-class linkage failure.

### 4. Validation and error matrix

| Condition | Required result |
|---|---|
| `initializeClient` throws before the subclass constructor returns | Fail; remove access to subclass instance fields |
| Bucket model reports a missing still texture | Fail; add the texture to the block atlas |
| Model bakery reports missing `level` variants | Fail; generate a default blockstate and model |
| Dedicated server reports client-only linkage | Fail; move the client reference behind a physical-client boundary |

### 5. Good, base and bad cases

- Good: all fluid families register, buckets and world fluids resolve both
  sprites, and the client log contains no Magneticraft model/texture warning.
- Base: datagen skips `initializeClient`; generated atlas and blockstate
  contracts are still unit-tested from disk.
- Bad: the callback reads `this.definition`, which is still `null` while the
  `FluidType` superclass constructor is executing.

### 6. Tests required

- Unit-test description-ID to still/flow texture derivation for every fluid.
- Parse generated atlas, blockstate and model JSON and assert catalogue
  coverage.
- Run the client through sound-engine and block-atlas creation, then scan
  `latest.log` for missing textures, missing models and registry failures.
- Run GameTest and a dedicated server to prove common-side classloading.

### 7. Wrong vs correct

```java
// Wrong: definition is assigned only after super(...) returns.
ResourceLocation still = id("fluid/" + definition.id() + "_still");

// Correct: FluidType has already assigned the description ID before callback.
ResourceLocation still = texture(getDescriptionId(), "_still");
```

## Scenario: plain JUnit versus Forge loader tests

### 1. Scope / trigger

Apply whenever a test touches `ForgeCapabilities`, `ItemStack`, vanilla registry holders, block entities,
recipes, menus or any API whose implementation is injected by ModLauncher.

### 2. Signatures

```java
// src/test/java: loader-independent value logic only
@Test
void receiveSimulationDoesNotMutateEnergy() { }

// src/gametest/java: registry/capability/world behavior
@GameTest(template = "base_content")
public static void energyCapabilityInvalidatesAndRevives(GameTestHelper helper) { }
```

### 3. Contracts

- Plain JUnit may test pure Java algorithms, NBT routing that does not initialize item/block registries,
  generated files and other loader-independent codecs.
- Any test that resolves `ForgeCapabilities`, constructs registry-backed `ItemStack` values, queries
  `RecipeManager`, creates a real block entity/menu, or requires capability bytecode injection belongs in
  the Forge GameTest source set or another explicit ModLauncher harness.
- Do not call `Bootstrap.bootStrap()` in a plain test as a substitute for Forge lifecycle injection; it
  does not implement Forge's capability transformer contract.

### 4. Validation and error matrix

| Symptom | Cause | Required action |
|---|---|---|
| `CapabilityToken.getType(): This will be implemented by a transformer` | Plain JUnit touched an injected Forge capability | Move that assertion to GameTest |
| `IllegalArgumentException: Not bootstrapped` from `BuiltInRegistries` | Test initialized blocks/items before Minecraft bootstrap | Move registry-backed behavior to GameTest |
| Pure bounds/serialization helper needs a running game | Domain logic is coupled to the loader | Extract the smallest pure value object, then unit-test it |
| GameTest compiles but is not executed | Test is outside the configured namespace/source set | Verify `forge.enabledGameTestNamespaces` and `runGameTestServer` output |

### 5. Good, base and bad cases

- Good: `EnergyBuffer` bounds run in milliseconds under JUnit, while sided FE capabilities and BE reloads
  run in `runGameTestServer`.
- Base: an NBT module-key router using only `CompoundTag` and `ResourceLocation` remains a plain unit test.
- Bad: a JUnit test references `ForgeCapabilities.ENERGY` or `Items.IRON_INGOT` and tries to repair the
  resulting initialization failure with manual bootstrap calls.

### 6. Tests required

- Plain JUnit asserts simulation immutability, clamping, duplicate stable IDs and missing/unknown NBT nodes.
- GameTest asserts capability direction, invalidate/revive, item capability persistence, menu movement,
  recipe execution and block entity save/load in a loaded server.
- The final gate runs both `test` and `runGameTestServer`; compiling the GameTest source set alone is not
  runtime evidence.

### 7. Wrong vs correct

```java
// Wrong: ForgeCapabilities requires ModLauncher transformation.
@Test
void energyCapabilityExists() {
    assertTrue(machine.getCapability(ForgeCapabilities.ENERGY).isPresent());
}

// Correct: keep JUnit pure and exercise the adapter in GameTest.
@Test
void boundedEnergySimulationIsPure() {
    assertEquals(100, new EnergyBuffer(100, 100, 100).receive(200, true));
}
```

```java
@GameTest(template = "base_content")
public static void energyCapabilityExists(GameTestHelper helper) {
    // Place the registered block, query ForgeCapabilities and assert lifecycle behavior here.
}
```

## Rebuildable physical-network state

### Authority and lifecycle

- Electrical, thermal, pressure, fluid and logistics state belongs to the owning block entity module and
  is persisted below its stable module ID. A graph manager is a loaded-level cache, never the only copy of
  node values, side modes or in-flight payloads.
- Runtime managers must be keyed by `ServerLevel` identity (not a bare dimension integer or `BlockPos`) and
  must not connect nodes from different levels. Chunk unload unregisters nodes without draining or deleting
  their authoritative storage; `onLoad` reconstructs local topology.
- Every module that registers runtime state needs an idempotent unload hook. Capability invalidation does not
  replace topology unregistering, because it may also happen during block-state or loader lifecycle changes.

### Incremental topology and budgets

- Add/remove/update operations inspect the changed node and direct neighbors only. A removed edge may rebuild
  its former connected component, but no server tick may scan chunks or rebuild every component.
- Continuous-domain ticks traverse each loaded edge at most once. Item pathfinding has explicit visited-node
  and queue limits; full targets, redstone-disabled components and unloaded chunks must cause backpressure,
  not an unbounded retry queue.
- Expose cumulative topology metrics in the pure core so tests can prove that a 1024-node steady topology does
  no rebuild work and that a split only visits the affected component.

### Conservation and adapters

- Each domain declares its own unit and conservation equation. Losses (electrical resistance or configured
  pressure leakage) are returned as explicit transfer results; rounding remainders must be restored to the
  source rather than silently discarded.
- FE, `IFluidHandler` and `IItemHandler` are side-specific boundary adapters. Always simulate the target first,
  execute only the accepted amount, and keep cached views safe after a side is disabled.
- A simulation call may report capacity or route availability but must not alter storage, cursors, payloads,
  dirty flags or client sync state.
- Network behavior requires both pure Java conservation/budget tests and loaded GameTests for topology,
  capability injection, block-entity persistence and exactly-once item transfer.

## Scenario: constructor-safe sided capability views

### 1. Scope / trigger

Apply whenever a block entity passes a side-access callback into an inventory,
fluid, energy or other capability module whose constructor immediately creates
and caches sided `LazyOptional` views.

### 2. Signatures

```java
public ItemInventoryModule(
        int slots,
        Function<Direction, ItemInventoryModule.SideAccess> accessBySide
)

private ItemInventoryModule.SideAccess inventoryAccess(Direction side)
```

### 3. Contracts

- A callback evaluated from a module constructor may read only constructor
  arguments, immutable machine definitions or host state assigned before the
  module constructor starts.
- It must not read the host field that will receive the module instance, because
  Java assigns that field only after `new Module(...)` returns.
- If access policy requires live module state, defer creation of sided views
  until host construction completes and recreate them from `reviveCaps()`.
- Each cached view preserves its side policy through capability
  invalidate/revive and block-entity save/load cycles.

### 4. Validation and error matrix

| Condition | Required result |
|---|---|
| Callback observes an unassigned host module field | Fail; derive policy from immutable definition or defer view creation |
| Intended side returns an empty capability immediately after placement | Fail; inspect constructor-order dependency |
| Disabled side exposes a capability after revive | Fail; rebuild views with the same side policy |
| Reload changes the exposed side set | Fail; keep policy definition-driven and deterministic |

### 5. Good, base and bad cases

- Good: the host derives slot access from `definition.inventorySlots()` while
  constructing the inventory module; intended sides are available immediately.
- Base: a side-independent module returns one immutable access policy for every
  non-null direction and an explicit internal policy for `null`.
- Bad: `inventoryAccess()` reads `this.inventory`, which is still `null` while
  the `ItemInventoryModule` constructor caches its sided handlers.

### 6. Tests required

- Pure JUnit asserts deterministic access-policy mapping from the immutable
  machine definition, without loading Forge capabilities.
- GameTest places the block and asserts the intended side is present and a
  forbidden side is absent before the first tick.
- GameTest invalidates and revives capabilities, then saves/reloads the block
  entity and repeats both side assertions.

### 7. Wrong vs correct

```java
// Wrong: inventory is assigned only after the module constructor returns.
private SideAccess inventoryAccess(Direction side) {
    return inventory == null ? SideAccess.NONE : inventory.policyFor(side);
}

// Correct: definition was assigned before module construction begins.
private SideAccess inventoryAccess(Direction side) {
    return definition.inventorySlots() == 0
            ? SideAccess.NONE
            : definition.policyFor(side);
}
```

## Scenario: LegacyForge optional-mod remapping and publication isolation

### 1. Scope / trigger

Apply whenever a production-obfuscated Forge mod is used as a compile-only API
or opt-in development runtime under ModDevGradle LegacyForge. This includes JEI,
CraftTweaker, Tinkers' Construct and any future optional compatibility target.

### 2. Signatures

```groovy
obfuscation {
    createRemappingConfiguration(configurations.localRuntime)
}

dependencies {
    modCompileOnly("group:optional-api:version")
    modLocalRuntime("group:optional-mod:version")
    localRuntime("group:ordinary-java-library:version")
}
```

### 3. Contracts

- Production mod JARs must enter a `mod*` configuration so LegacyForge remaps
  SRG bytecode to the development namespace. Plain `runtimeOnly` or
  `localRuntime` is invalid for a mod JAR even when its dynamic Mixin refmap is
  present.
- `modCompileOnly` supplies public optional APIs without making the base mod
  require them at runtime. Opt-in test runtimes use an isolated remapped
  `modLocalRuntime`, created from `localRuntime`, so they do not become
  dependencies in the published POM.
- ModDevGradle's remapped dependency configurations are non-transitive. List
  every required mod module explicitly. Ordinary Java libraries required by an
  optional mod stay on `localRuntime`; they are not passed through mod remapping.
- Each optional runtime is controlled by an explicit boolean Gradle property.
  A tracked development default may enable integrations under active visual
  testing, but the base-installation matrix must pass every property as false,
  and published metadata/POMs must remain optional and dependency-free.

### 4. Validation and error matrix

| Symptom | Cause | Required action |
|---|---|---|
| Mixin shadow still names an SRG member such as `m_8895_` in development | Production mod JAR entered plain runtime configuration | Move the mod JAR to `modLocalRuntime` and verify the transformed artifact |
| Optional client works but all-in-one runtime misses a module/class | Remapped configurations are non-transitive | Enumerate every required mod module or Java library explicitly |
| Generated POM lists JEI, CraftTweaker, Mantle or TConstruct | Test runtime leaked into a published configuration | Keep opt-in dependencies on isolated `localRuntime`/`modLocalRuntime` and fail the artifact audit |
| Base installation throws `NoClassDefFoundError` for an optional API | Common registration linked integration classes eagerly | Gate integration entry points by loader presence and keep optional classes behind their mod event boundary |

### 5. Good, base and bad cases

- Good: the enabled runtime classpath contains a transformed optional-mod JAR,
  standalone and combined load matrices pass, and the generated POM contains no
  optional development dependency.
- Base: all optional properties are false; compile-only API references compile,
  while base client and dedicated server start without optional classes.
- Bad: `runtimeOnly("...:CraftTweaker-...jar")` starts with production SRG
  bytecode and fails after Mixin refmap remapping cannot repair method bodies.

### 6. Tests required

- Inspect the enabled runtime classpath and prove production mod JARs resolve
  through Gradle's transformed cache; use bytecode inspection when a mapping
  failure is suspected.
- Run each optional integration alone on its required physical sides, then run
  the complete combination on client and dedicated server.
- For stateful integrations, save, stop and restart the same world with the same
  mod set; scan logs for mapping, classloading, registry and Mixin failures.
- Generate the Maven POM with every optional runtime enabled and assert that it
  contains no optional development dependency.

### 7. Wrong vs correct

```groovy
// Wrong: a production-obfuscated mod JAR bypasses LegacyForge remapping.
runtimeOnly("com.example:optional-forge-mod:1.0.0")

// Correct: the optional mod is remapped and remains local to development.
modCompileOnly("com.example:optional-forge-mod:1.0.0")
if (optionalRuntimeEnabled) {
    modLocalRuntime("com.example:optional-forge-mod:1.0.0")
}
```

## Test requirements

Every large migration task must pass the applicable gates before its single
Chinese commit:

```text
git diff --check
./gradlew.bat compileJava --no-daemon
./gradlew.bat test --no-daemon
./gradlew.bat runData --no-daemon       # twice; second run is idempotent
./gradlew.bat build --no-daemon
```

- Add pure unit tests for algorithms, value bounds, codecs and serialization
  helpers.
- Add GameTests for block placement, automation sides, inventories, recipes,
  multiblock validation and server-side state transitions.
- Run a dedicated server for common-side classloading and lifecycle changes.
- Run a client for screens, models, BERs, item rendering and input behavior.
- For stateful systems, verify save, shutdown, restart and chunk reload.
- Record any gate that is not applicable or cannot run, with the reason; do not
  silently call the task complete.

## Review checklist

- Behavior matches the Nova 1.12 authority, including boundary and automation
  rules, unless a documented 1.20 decision supersedes it.
- Registry IDs, NBT keys, packet direction and physical units are explicit.
- Server/client boundaries and optional-mod classloading are safe.
- Generated resources are reproducible and contain no Avaritia leftovers.
- New abstractions have at least two real consumers or solve a demonstrated
  lifecycle contract.
- Licenses and attribution permit every reused source or asset.
- `git status` and the staged file list exclude `.trellis/tasks/**`,
  `.trellis/workspace/**`, `run/**` and `build/**` unless explicitly requested.
- Exactly one coherent Chinese commit is created after all required gates pass;
  no push is performed without a separate request.
