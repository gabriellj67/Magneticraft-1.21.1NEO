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
