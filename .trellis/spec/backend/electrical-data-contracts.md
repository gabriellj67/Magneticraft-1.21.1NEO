# Electrical Data Contracts

## 1. Scope / Trigger

Apply this contract whenever code changes native-electricity tiers, transformer
coupling, machine electrical roles, electrical reload behavior, tier display
packets or profile-bound nodes. Native electricity remains server-authoritative;
client display data and Forge Energy adapters are downstream projections only.

## 2. Signatures

The stable internal entry points are:

```java
ElectricalDataLoadResult ElectricalDataParser.parse(
    Map<ResourceLocation, JsonElement> tiers,
    Map<ResourceLocation, JsonElement> transformers,
    Map<ResourceLocation, JsonElement> machines
);

ElectricalDataRegistry.ApplyResult ElectricalDataRegistry.apply(
    ElectricalDataLoadResult candidate
);

void ElectricalProfileBinding.rebindElectricalProfile(
    ElectricalDataSnapshot snapshot
);

void PhysicalNetworkManager.onElectricalProfilesReloaded(
    ElectricalDataSnapshot snapshot,
    int graceTicks
);
```

Resource directories are fixed:

```text
data/<namespace>/magneticraft/voltage_tiers/*.json
data/<namespace>/magneticraft/transformer_profiles/*.json
data/<namespace>/magneticraft/machine_electrical_profiles/*.json
```

## 3. Contracts

Every JSON root is one object with `schema_version: 1`; its logical ID is the
outer data namespace plus the path below the registry directory. A voltage tier
owns voltage, capacitance, resistance, conductor/protection ratings, thermal
parameters, long-distance ranges, battery parameters, translation key and RGB
color. Transformer profiles reference two distinct tier IDs and define maximum
J/t plus efficiency. Machine profiles reference a tier and define one
`ElectricalRole`, buffer J, maximum J/t and terminal C/t.

`ElectricalDataSnapshot` contains three non-empty immutable maps. Reload builds
and validates all maps before `ElectricalDataRegistry.apply`; only a valid full
candidate receives a new monotonically increasing generation. Loaded nodes are
rebound without loading chunks. Electricity pauses for one tick and electrical
damage is suppressed for the configured `electricalReloadGraceTicks`.

S2C data contains at most 256 tier display entries. IDs and translation keys are
at most 128 characters. The client atomically accepts only non-stale generations
and must never feed its display cache into simulation.

Every item carrying `TieredElectricalItemData` decorates its normal display
name as `[%tier%] %base-name%`. On the client, the tier component comes from the
synced immutable display snapshot so data-pack tiers retain their declared
translation key. Server-side name generation may use the authoritative
snapshot; an unavailable display definition falls back to the tier ID. Item
name rendering never changes placement validation or simulation identity.

## 4. Validation & Error Matrix

| Condition | Required result |
|---|---|
| Missing/wrong JSON field type | Add a resource-anchored error; reject candidate |
| NaN, infinity, zero or negative physical value | Add an error; reject candidate |
| `minimum >= nominal` or `nominal >= maximum` | Add an error; reject candidate |
| Efficiency outside `(0, 1]` | Add an error; reject candidate |
| Missing tier reference | Add an error on the referring profile; reject candidate |
| Any registry empty or beyond its bound | Reject the whole candidate |
| Invalid reload with a current snapshot | Throw reload failure and retain the same live snapshot |
| Invalid first load | Fail resource loading; never publish an empty fallback |
| Missing profile after a valid reload | Bound device enters safe disconnected `MISSING_PROFILE` state |
| Malformed/stale client packet | Reject it and retain the prior client display snapshot |
| Tiered item with a synced custom tier | Render the synced tier translation in its display name |
| Item without valid tier payload | Preserve its ordinary base display name |
| Tier payload unavailable in both display snapshots | Render the stable tier ID; never crash |

All errors discovered in the same candidate are reported together. Do not log
per tick; one reload summary with resource-specific details is sufficient.

## 5. Good / Base / Bad Cases

- Good: a higher-priority pack adds a complete fourth tier plus referencing
  transformer, machine profiles and recipes; the snapshot swaps once.
- Base: built-in LV/MV/HV, LV-to-MV, MV-to-HV and machine profiles load as one
  generation before gameplay begins.
- Bad: one machine references a removed tier while another tier contains NaN;
  both errors are reported and no map from the candidate becomes visible.
- Good display: a data pack adds a fourth tier and its cable, connector,
  protection, fuse and transformer items all show that tier in the name without
  adding item-specific translation keys.

## 6. Tests Required

- Pure tests: every field/type/range, NaN/infinity, registry bounds, missing
  references, fourth-tier success, atomic failure and generation ordering.
- Packet tests: count, text, numeric and duplicate-ID bounds; round trip and
  stale-generation rejection.
- Item-name tests: built-in and custom synced tiers, all tier-payload item
  subclasses, missing payload and unknown-tier fallback.
- GameTest: built-in full snapshot exists on a dedicated server; successful
  rebind pauses only electricity for one tick and counts down grace.
- Cross-layer gates: `compileJava`, `compileGameTestJava`, `test`,
  `runGameTestServer`, `runData`, `build`, then inspect the JAR paths.

## 7. Wrong vs Correct

Wrong: mutate the live tier map while parsing files, or use the client cache to
choose machine voltage.

```java
liveTiers.put(id, parse(json));
double voltage = ClientVoltageTierRegistry.current().tier(id).orElseThrow().nominalVoltage();
```

Correct: parse all three registries into a candidate, validate cross references,
publish once, and resolve simulation data from the server snapshot.

```java
ElectricalDataLoadResult candidate = ElectricalDataParser.parse(tiers, transformers, machines);
ElectricalDataSnapshot snapshot = ElectricalDataRegistry.INSTANCE.apply(candidate).current();
double voltage = snapshot.voltageTier(id).orElseThrow().nominalVoltage();
```

Wrong: hard-code LV/MV/HV names in every item subclass. Correct: every payload
item delegates its name decoration to one display-only resolver backed by the
synced tier snapshot.
