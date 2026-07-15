# Electrical Data Contracts

## 1. Scope / Trigger

Apply this contract whenever code changes native-electricity tiers, transformer
coupling, connector conversion, machine electrical roles, electrical reload
behavior, tier display packets or profile-bound nodes. Native electricity
remains server-authoritative; client display data and Forge Energy adapters are
downstream projections only.

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

int ElectricalPowerModule.consumeJoules(int requested, boolean simulate);
double ElectricalPowerModule.generateJoules(double requested, boolean simulate);
int ElectricalPowerModule.receiveForgeEnergy(int maximum, boolean simulate);

int ElectricEnergyExporter.export(
    ServerLevel level,
    BlockPos position,
    Direction outwardFacing,
    ElectricalNode node,
    VoltageTier tier,
    int maximumTransfer
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
parameters, long-distance ranges, battery parameters, connector conversion
rate, translation key and RGB color. The optional schema-1 field
`connector_conversion_joules_per_tick` is a positive finite number; old packs
that omit it retain the compatibility default of `400.0`. Runtime conversion
floors it to whole J/t and FE/t. Transformer profiles reference two distinct
tier IDs and define maximum J/t plus efficiency. Machine profiles reference a tier and define one
`ElectricalRole`, rated node energy, maximum J/t and terminal C/t. The legacy
JSON key remains `buffer_capacity_joules` for data-pack compatibility, but it
does not authorize a second machine buffer. Profile-bound machine capacitance
is derived as `C = 2 * ratedEnergy / maximumVoltage^2`.

For every native machine, `ElectricalNode.energyJoules()` is the only
authoritative balance. `ElectricalPowerModule` binds the profile and performs
J operations without owning an energy field. Consumers atomically withdraw J
only at or above minimum operating voltage; generators write J directly up to
the profile rate and generator-voltage target. Normal native machines expose no
Forge Energy capability. The FE transformer is FE -> J, and the electric
engine remains a native medium-voltage J endpoint with no FE capability or
direct FE push. J -> FE is permitted only through a connector or wireless
receiver using `ElectricEnergyExporter`, always at `1 J = 1 FE`.
FE conversion operates on whole integer units. Capability simulation neither
consumes the machine-profile budget nor mutates either endpoint.

Operating thresholds use `VoltageTier.meetsMinimumOperatingVoltage` and
`VoltageTier.operatingRateFraction`. These helpers apply only a scale-relative
floating-point comparison tolerance; they do not round or mutate node energy.
This keeps voltages reconstructed from `E = 0.5CV^2` stable at the exact tier
minimum and nominal boundaries.

Connectors and wireless receivers expose their outward face to the native
physical network. `ElectricEnergyExporter` first checks the target's opposing
face for `NetworkDomain.ELECTRICITY`; if present, it performs no FE operation
and lets the physical network carry J. Only an FE-only target enters the
transactional J -> FE path. The low-voltage connector retains its 400 FE/t
compatibility limit, while every connector tier uses its own data-defined
`connector_conversion_joules_per_tick`. Built-in rates are LV 400, MV 1,600 and
HV 6,400 J/t (and therefore FE/t at the fixed ratio). The rate is scaled from
zero at minimum voltage to full rate at nominal voltage. Wireless output remains
independent and uses its machine-profile rate.

Machine-observation schema 3 includes `energy.unit` with the enum values
`JOULE` and `FORGE_ENERGY`. GUI and Jade translations must use this explicit
unit; native node storage is displayed in J and the pure FE heater in FE.
Observation preserves stored energy above the current rated capacity so a
profile shrink remains visible instead of silently clamping telemetry.

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
| Native machine is queried for a generic FE capability | Return no capability unless it is a named conversion boundary |
| Electric engine is queried for FE or placed beside an FE target | Return no FE capability and perform no direct export; route J through a connector |
| Adapter target supports both native electricity and FE | Skip FE conversion; allow only the native J path |
| Schema-1 voltage tier omits `connector_conversion_joules_per_tick` | Accept it and use `400.0` for backward compatibility |
| Connector conversion rate is NaN, infinite, zero or negative | Add an error on the voltage tier; reject the whole candidate |
| Simulated J or FE operation | Return the exact prospective integer/double amount and mutate nothing |
| Repeated FE conversion calls in one game tick | Share one cumulative profile J/t budget across every call |
| FE conversion has less than one whole unit available or free | Transfer zero; never create or delete fractional J |
| Legacy schema-1 module contains integer `energy` | Merge it with the loaded node balance; current saves omit that field |
| A machine action fails after withdrawing J | Restore the exact captured node balance, including energy above a reloaded rating |
| Missing/unknown machine-observation energy unit | Reject an unknown schema; within schema 3, use the bounded safe fallback |

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
- Good conversion: a wireless receiver adjacent to a hybrid native/FE target
  skips the FE capability, then transfers J through the ordinary electrical
  edge; an FE-only target receives only its simulated-and-accepted integer FE.
- Good connector: a custom voltage tier declares
  `connector_conversion_joules_per_tick: 12345.0`; its matching connector uses
  that full rate at nominal voltage without Java-side tier branching.
- Base connector: built-in LV/MV/HV connectors export 400/1,600/6,400 FE/t to
  FE-only targets, while an old schema-1 pack without the field stays at 400.
- Bad connector: hard-code only the three built-in tier IDs, or make the
  electric engine expose FE as a second conversion path.
- Base machine: recipe consumption and GUI storage both read the same node J;
  no synchronization bridge runs between two balances.
- Bad machine: keep `EnergyStorageModule` beside an electrical node and copy
  between them every tick, or choose FE merely because a hybrid target exposes
  that capability.

## 6. Tests Required

- Pure tests: every field/type/range, NaN/infinity, registry bounds, missing
  references, fourth-tier success, atomic failure and generation ordering.
- Packet tests: count, text, numeric and duplicate-ID bounds; round trip and
  stale-generation rejection.
- Item-name tests: built-in and custom synced tiers, all tier-payload item
  subclasses, missing payload and unknown-tier fallback.
- GameTest: built-in full snapshot exists on a dedicated server; successful
  rebind pauses only electricity for one tick and counts down grace.
- Power-module tests: profile energy-to-capacitance binding, undervoltage,
  generation/rate limits, pure simulation, FE-input limits, passive native-only
  binding and repeat-safe legacy `energy` migration.
- Parser tests: explicit connector rates, schema-1 fallback, non-finite and
  non-positive rejection, and custom-tier preservation.
- Boundary GameTests: every built-in connector tier covers its data-defined
  FE-only output; connector and wireless receiver both cover a hybrid target
  where native J wins and total J is conserved. The electric engine exposes no
  sided or unsided FE capability and never actively fills an adjacent FE target.
- Observation tests: schema-3 round trip includes `energy.unit`; unknown enum
  values use the documented safe fallback, over-rated stored J remains visible,
  and no value escapes the whitelist.
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

Wrong: maintain a node plus an integer FE buffer and select the FE capability
before checking whether the destination is a native electrical endpoint.

```java
bridge.tick(node, energyStorage);
target.getCapability(ForgeCapabilities.ENERGY, side).ifPresent(this::export);
```

Correct: operate on the node directly and make native endpoint detection the
first branch at an explicit conversion boundary. Resolve connector output from
the live voltage-tier snapshot instead of tier IDs.

```java
VoltageTier tier = snapshot.voltageTier(connector.electricity().tierId()).orElseThrow();
if (target instanceof NetworkConnectionHost host
        && host.supportsNetworkConnection(NetworkDomain.ELECTRICITY, side)) {
    return 0;
}
return ElectricEnergyExporter.export(
    level,
    position,
    facing,
    node,
    tier,
    (int) Math.floor(tier.connectorConversionJoulesPerTick())
);
```
