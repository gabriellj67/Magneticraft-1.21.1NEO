# Persistence and Data Guidelines

> Magneticraft has no external database. This file defines persistent game
> state, recipes, configuration and generated-data contracts.

## Persistence ownership

- A `BlockEntity` owns only its local durable state. Implement
  `saveAdditional(CompoundTag)` and `load(CompoundTag)` symmetrically and call
  the superclass implementation.
- Level-wide graph state belongs in `SavedData`; never keep authoritative
  networks only in static maps. Runtime caches must be reconstructible from
  saved state or loaded block entities.
- Item, fluid and energy inventories use their Forge handlers as the single
  source of truth. Do not mirror the same values in unrelated fields.
- Menus and screens are views of server-owned state. Client values are never
  persisted as authority.
- The first release supports new worlds only. This removes legacy-NBT migration,
  but it does not permit unstable keys or nondeterministic save formats.

## Stable identifiers and NBT

- Use namespaced `ResourceLocation` identifiers for modules, recipe types,
  multiblocks and network node kinds.
- Use explicit, stable NBT keys in `lower_snake_case`. Keep keys next to the
  owning serializer rather than in a global constants class.
- Store module state under its module ID so adding a module cannot reorder or
  reinterpret existing data.
- Validate ranges while loading. Missing optional keys use documented defaults;
  malformed required values produce a contextual warning and disable only the
  affected entry when safe.
- Call `setChanged()` after server-side durable state changes. Invalidate and
  revive Forge capabilities with the block entity lifecycle.

```java
private static final String ENERGY_KEY = "energy";

@Override
protected void saveAdditional(CompoundTag tag) {
    super.saveAdditional(tag);
    tag.putInt(ENERGY_KEY, energyStored);
}

@Override
public void load(CompoundTag tag) {
    super.load(tag);
    energyStored = Mth.clamp(tag.getInt(ENERGY_KEY), 0, capacity);
}
```

## Scenario: Versioned persistence envelopes and safe reset

### 1. Scope / trigger

Apply to every durable block entity, item payload, `SavedData` record and
independently owned machine module. The 0.2.0 reset intentionally rejects all
earlier workspace formats; after 0.2.0, schema changes require forward-compatible
handling.

### 2. Signatures

```java
private static final String SCHEMA_VERSION_KEY = "schema_version";
private static final int SCHEMA_VERSION = 1;

tag.putInt(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
tag.put("item_inventory", inventory.save());
```

```java
CompoundTag inventoryTag = tag.getCompound("item_inventory");
int version = inventoryTag.getInt(SCHEMA_VERSION_KEY);
```

### 3. Contracts

- Every persistence owner writes a local `schema_version`; a block entity root
  and each independently serialized module have separate versions. All keys and
  module names use `lower_snake_case`.
- The owner routes only its own subtag. It must not infer a module version from
  the block entity root or expose another module's raw keys at the root.
- In the 0.2.0 breaking reset, a missing, non-positive or future schema resets
  that owner to current neutral defaults: empty inventories and fluids, zero
  progress and energy, ambient heat, no owner/claim, and no executable program.
- From 0.2.0 onward, an older supported schema is handled by an explicit
  forward migrator. Never silently parse an old shape as the current schema;
  unknown future schemas still reset or disable only the owning component.
- Block entities persist local state. Level-wide long-distance connections and
  deposits belong to `SavedData`; runtime graphs and membership indexes remain
  rebuildable caches and never the only durable source.
- Load validation clamps bounded values, clears invalid registry references and
  leaves the object in a fully initialized state before capabilities or ticks
  become visible.

### 4. Validation and error matrix

| Input | Required result |
|---|---|
| Current root and current module schemas | Load validated values exactly |
| Missing root schema during 0.2.0 reset | Reset the complete owner to neutral defaults |
| Current root with missing/future module schema | Reset only that module |
| Older post-0.2.0 supported schema | Run the explicit forward migrator |
| Unknown future schema | Do not partially interpret; reset or safely disable the owner |
| Runtime graph absent after reload | Rebuild from loaded durable endpoints or `SavedData` |

### 5. Good, base and bad cases

- Good: a current machine reloads its validated modules; a future inventory
  module clears only inventory while the current heat module remains valid.
- Base: a new 0.2.0 world has no payload, so every owner starts at its documented
  neutral defaults and writes the current envelope on first save.
- Bad: `getInt` treats a missing version as current, or a future tag restores
  energy and items using an unknown field layout.

### 6. Tests required

- Pure JUnit covers current round trips, missing/root/module versions, future
  versions, malformed bounds and owner-local reset behavior for loader-free
  serializers.
- GameTest loads real block entities with missing and future envelopes and
  verifies inventory, fluid, energy, heat, progress, ownership, capabilities
  and structure revalidation.
- Dedicated-server restart tests cover `SavedData`, block entities and items;
  chunk unload/reload must not duplicate or erase durable values.
- Every new owner or schema bump must add current, missing, future and malformed
  fixtures before the implementation is accepted.

### 7. Wrong vs correct

```java
// Wrong: a missing or future envelope is interpreted as the current shape.
energy.setStored(tag.getInt("energy"));

// Correct: validate the owning envelope before loading any of its fields.
int version = tag.getInt(SCHEMA_VERSION_KEY);
if (version != SCHEMA_VERSION) {
    resetToDefaults();
    return;
}
energy.setStored(Mth.clamp(tag.getInt("energy"), 0, capacity));
```

## Recipes and generated resources

- Recipes use `RecipeType` plus `RecipeSerializer`; machine behavior must not be
  backed by hardcoded static recipe tables.
- Models, blockstates, loot tables, tags, language entries and recipes are
  datagen-first. Handwritten JSON is reserved for formats that the providers
  cannot express clearly.
- Run `runData` twice and require the second run to leave tracked generated
  resources unchanged.
- Prefer vanilla/Forge tags for copper and shared materials. Magneticraft-owned
  tags use the `magneticraft` namespace and `snake_case` paths.
- Multiblock definitions are immutable Java definitions in the first release;
  do not invent a custom datapack schema without a confirmed requirement.

## Scenario: Forge 1.20.1 datagen provider and cache boundary

### 1. Scope / trigger

Apply this contract whenever a `GatherDataEvent` adds a provider or a build
starts consuming `src/generated/resources`. It prevents an overloaded Java
call from failing compilation and prevents datagen's private hash cache from
being published as mod content.

### 2. Signatures

```java
DataGenerator generator = event.getGenerator();
PackOutput output = generator.getPackOutput();
generator.addProvider(event.includeClient(), new ModLanguageProvider(output));
```

```groovy
sourceSets.main.resources.exclude ".cache/**"
```

The repository `.gitignore` must also contain
`src/generated/resources/.cache/`.

### 3. Contracts

- Register a concrete `DataProvider` instance when a lambda could match both
  `addProvider(boolean, DataProvider.Factory<T>)` and
  `addProvider(boolean, T)` on Minecraft 1.20.1.
- Track generated assets/data that are build inputs; do not track or package
  the `.cache` directory used by `HashCache`.
- Run the exact same `runData` command twice. Hash every non-cache generated
  file before and after the second run; all paths and SHA-256 values must match.
- The binary and sources JARs must contain the generated resources but no
  `.cache` entry.

### 4. Validation and error matrix

| Condition | Required result |
|---|---|
| Java reports an ambiguous `addProvider` call | Replace the lambda with an explicit provider instance; do not cast blindly |
| First datagen run writes owned resources | Review and stage only intended resource files |
| Second datagen run changes a non-cache hash | Fail the task and fix nondeterminism |
| `.cache` appears in Git or either JAR | Fail packaging and correct both ignore and resource-exclude rules |
| Provider is disabled by `includeClient/includeServer` | No output is expected for that run mode |

### 5. Good, base and bad cases

- Good: two language files are generated, the second run writes zero files,
  hashes stay equal, and both JARs omit `.cache`.
- Base: a provider is excluded by the selected run mode; it produces no file
  and does not make the run fail.
- Bad: an untyped constructor lambda is ambiguous at compile time, or a broad
  `srcDir` causes `.cache` hashes to ship in the release JAR.

### 6. Tests required

- Run `compileJava` to prove every `addProvider` overload resolves.
- Run `runData` twice and assert non-cache path/SHA-256 equality.
- Inspect binary and sources JAR entry lists for required generated resources
  and the absence of `.cache`.
- Run `git diff --check` and inspect the staged path list for cache files.

### 7. Wrong vs correct

```java
// Wrong on this 1.20.1 toolchain: the lambda matches two overloads.
generator.addProvider(event.includeClient(), output -> new ModLanguageProvider(output));

// Correct: overload selection and provider ownership are explicit.
PackOutput output = generator.getPackOutput();
generator.addProvider(event.includeClient(), new ModLanguageProvider(output));
```

## Scenario: Counted Forge cooking results

### 1. Scope / trigger

Apply when legacy furnace behavior produces more than one item. Minecraft
1.20.1's vanilla cooking builder exposes only a single result, while Forge
47.4.20 extends the recipe JSON and serializer with a counted result object.

### 2. Signatures

```java
CountedCookingRecipeBuilder.smelting(
        ingredient, RecipeCategory.MISC, result, resultCount,
        experience, cookingTime
).save(consumer, recipeId);
```

### 3. Contracts

- `resultCount` is positive and is serialized as
  `"result": {"item": "namespace:id", "count": n}`.
- Use the counted builder only when `count > 1`; single-result recipes retain
  the normal cooking shape produced by the same validated builder.
- Do not add blasting recipes unless the behavioral authority defines them.
- A Forge runtime test, not JSON parsing alone, must prove the assembled output
  stack count because the object result is a Forge patch to the vanilla codec.

### 4. Validation and error matrix

| Condition | Required result |
|---|---|
| `resultCount <= 0` | Reject during builder creation |
| Counted result is serialized as a string | Fail generated-data validation |
| Runtime recipe output count differs from JSON | Fail GameTest |
| A migration invents a blasting counterpart | Remove it unless explicitly required |

### 5. Good, base and bad cases

- Good: a chunk smelts into two ingots in JSON and in a loaded server recipe.
- Base: ore or dust smelting produces one item through the same recipe family.
- Bad: two identical furnace recipes are emitted to imitate a count of two.

### 6. Tests required

- Unit-test generated JSON type, item ID and count for every counted recipe.
- GameTest `RecipeManager` lookup and assembled stack count for representative
  single- and double-result recipes.
- Run `runData` twice to prove stable output.

### 7. Wrong vs correct

```json
// Wrong: vanilla string form cannot preserve a count of two.
{"result": "magneticraft:lead_ingot"}

// Correct on Forge 47.4.20.
{"result": {"item": "magneticraft:lead_ingot", "count": 2}}
```

## Scenario: Multiblock authority, port exposure and portable state

### 1. Scope / trigger

Apply this contract when a formed multiblock claims another block entity,
exposes capabilities through directional ports, or preserves controller state
in its dropped item. These paths cross structure validation, runtime caches,
capabilities, NBT, loot and generated guide data.

### 2. Signatures

```java
public boolean operational()
public void claimForMultiblock(BlockPos controller)
public void releaseMultiblockClaim(BlockPos controller)
public void saveToItem(ItemStack stack)

MultiblockPortProfile.TankPort port =
        MultiblockPortProfile.tank(definition, tankIndex);
```

### 3. Contracts

- The controller modules own durable machine state. `MultiblockMembershipService`
  is only a loaded-level lookup cache and must be rebuildable after reload.
- `formed` records the durable structure decision; `operational()` additionally
  requires every structure chunk to be loaded and valid. Validation never
  force-loads chunks. An unloaded required chunk suspends processing and port
  exposure without immediately destroying the formed state.
- A claimed small tank invalidates its own capabilities. If the controller
  chunk is unloaded, the claim remains frozen; if that chunk is loaded and the
  controller is missing, unformed or no longer contains the tank, the tank
  releases the claim and revives its capabilities.
- An unform transition may update the controller block state only after reading
  the current world block and proving that it is the same controller definition.
  Removal callbacks must never recreate a controller that has already been
  replaced with air or another block.
- Portable controller NBT removes coordinates, owner, formed state and runtime
  links. Member items remove their controller claim. Large shelving contents
  are emitted as separate stacks rather than embedded into one oversized
  `BlockEntityTag`.
- `MultiblockPortProfile` is the single source of truth for tank capacity,
  accepted fluids, relative-side access and semantic role. Runtime handlers and
  generated guide JSON consume the same profile.

### 4. Validation and error matrix

| Condition | Required result |
|---|---|
| Required member chunk is unloaded | Suspend operation and capabilities; do not force-load or unform |
| Claimed tank can load its controller chunk and finds no valid owner | Release the claim and revive local capabilities |
| Controller block is removed while `unform()` runs | Do not write a formed-state block back into the world |
| A stateful controller has no normal loot result | Emit one controller fallback item containing portable state |
| Shelving contains items or upgrade chests | Drop contents separately and strip shelving storage from controller NBT |
| Guide port data differs from runtime access or validation | Fail generated-data tests; correct `MultiblockPortProfile` |

### 5. Good, base and bad cases

- Good: a refinery reloads with its controller state intact, exposes each tank
  on the profile-defined side, and the guide reports exactly those roles.
- Base: an unformed empty controller drops a normal stateless controller item
  and member tanks retain their independent capabilities.
- Bad: breaking a formed controller calls `setBlock` with its cached state and
  resurrects it, or a portable tank keeps a stale controller position.

### 6. Tests required

- Unit-test profile tank counts, capacities, roles, accepted-fluid rules and
  generated guide equality for every multiblock definition.
- GameTest all definitions, rotations and representative mirrors; invalid
  members, controller destruction, capability invalidate/revive, unloaded or
  stale claims, portable storage and runtime recipe execution.
- Run `runData` twice and compare every non-cache path and SHA-256 value.
- Run a dedicated server through save, clean shutdown and restart when changing
  durable multiblock or member state.

### 7. Wrong vs correct

```java
// Wrong: the cached block state may recreate a controller during onRemove.
level.setBlock(worldPosition, getBlockState().setValue(FORMED, formed), Block.UPDATE_ALL);

// Correct: mutate only a still-present controller of the same definition.
BlockState worldState = level.getBlockState(worldPosition);
if (worldState.getBlock() instanceof AdvancedMultiblockBlock controller
        && controller.definition() == definition) {
    level.setBlock(worldPosition, worldState.setValue(FORMED, formed), Block.UPDATE_ALL);
}
```

## Configuration

- Use Forge configuration specs for operator-tunable values. Keep gameplay
  defaults close to their validators and document units.
- Configuration is not a persistence substitute. Per-world or per-block state
  must not be written into the global config file.
- Physical quantities must state their unit in type, field or Javadoc when it is
  not obvious (for example, joules, watts, pascals or ticks).

## Round-trip verification

- Unit-test pure serializers and bounded value logic.
- GameTest or runtime-test block entity save/load, chunk unload/reload and menu
  synchronization for every new stateful framework.
- Dedicated-server tests must prove that client-only classes are absent from
  common persistence paths.

## Common mistakes

- Saving a runtime object reference, capability wrapper or `Level` instead of a
  stable identifier and primitive state.
- Forgetting `super.load`, `super.saveAdditional`, `setChanged`, capability
  invalidation or load-time range validation.
- Renaming registry IDs or NBT keys for style after worlds have been created.
- Maintaining a generated file by hand and allowing datagen to overwrite it.
