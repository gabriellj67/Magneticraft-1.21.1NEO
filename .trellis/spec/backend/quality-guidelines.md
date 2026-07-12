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
