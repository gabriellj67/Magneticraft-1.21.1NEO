# Physical Electrical Network Runtime

## 1. Scope / Trigger

Apply this contract whenever code changes native-electrical node math, physical
terminal identity, topology registration, ordinary/internal/long-distance
edges, isolated couplers, electrical telemetry or electrical-module NBT. Forge
Energy adapters remain outside this runtime and may exchange only through an
explicit conversion module.

## 2. Signatures

The stable internal entry points are:

```java
record PhysicalNodeKey(BlockPos position, ResourceLocation terminalId) {}

ElectricalLink.Transfer ElectricalLink.transfer(
    ElectricalNode first,
    ElectricalNode second,
    double distance,
    boolean simulate
);

void PhysicalNetworkManager.setInternalConnection(
    PhysicalNodeKey first,
    PhysicalNodeKey second,
    boolean closed
);

void PhysicalNetworkManager.registerElectricalCoupler(ElectricalCoupler coupler);

ElectricalLink.Transfer PhysicalNetworkManager.transferElectrical(
    PhysicalNodeKey first,
    PhysicalNodeKey second,
    double distance,
    ElectricalEdgeTelemetry.EdgeType edgeType
);
```

`PhysicalNodeKey.MAIN_TERMINAL` is `magneticraft:main`. Non-electrical domains
and compatibility position queries use that terminal unless an exact key is
provided.

## 3. Contracts

Node energy is authoritative in joules:

```text
E = 0.5 * C * V^2
V = sqrt(2E / C)
dt = 0.05 seconds
Ceq = C1*C2/(C1+C2)
Q = Ceq*(V1-V2)*(1-exp(-dt/(Rtotal*Ceq)))
I(A) = abs(Q(C/t))*20
W = abs(J/t)*20
```

Ordinary edges move equal signed charge and stop no later than the shared
voltage equilibrium. Source joules removed equal destination joules added plus
non-negative resistive loss. `maximumVoltage` is a rating/damage threshold;
new input is bounded by `4 * maximumVoltage`. Rebinding capacitance, resistance
or rating never rewrites stored joules, including energy already above the new
absolute boundary.

Every domain uses `IncrementalGraph<PhysicalNodeKey, PhysicalNetworkNode>` plus
an incremental position-to-terminal index. Same-position terminals do not join
implicitly. Ordinary adjacent, internal and long-distance electrical edges
require both endpoints to be loaded, profile-bound and on the same tier.
`ElectricalCoupler` executes separately and never becomes a graph edge.

The server tick order is fixed: reset telemetry, begin node telemetry, injection
hooks, couplers, each ordinary edge once, extraction hooks, complete telemetry,
then commit hooks. Long-distance exchange runs inside this window and never
loads an endpoint chunk. Unconnected proxy nodes are not resolved per tick.

Electrical module schema is `2` and saves terminal ID, tier ID and joules. Old,
unknown or terminal-mismatched payloads reset energy safely. Current payloads
may rebind after an already-loaded block entity changes position without
leaving a stale topology entry.

## 4. Validation & Error Matrix

| Condition | Required result |
|---|---|
| Non-finite/non-positive capacitance or voltage | Reject node configuration |
| Negative/non-finite resistance | Reject node configuration |
| Non-positive/non-finite edge distance | Reject transfer |
| Simulated node or edge operation | Return the exact prospective result; mutate nothing |
| Requested charge crosses equilibrium | Clamp to equilibrium charge |
| Target reaches the four-times voltage boundary | Reject the excess; leave it at the source |
| Existing energy exceeds a reloaded boundary | Preserve joules; reject additional input |
| Ordinary endpoint tier differs or profile is missing | Do not create or execute the edge |
| Two terminals share a position | Keep isolated unless an explicit closed internal edge exists |
| Coupler endpoints are unloaded | Skip exchange without merging components or loading chunks |
| Electrical NBT schema/terminal is invalid | Reset energy and rebind only the block's stable defaults |

## 5. Good / Base / Bad Cases

- Good: two terminals of one transformer remain in separate LV/MV components;
  an isolated coupler transfers bounded energy and reports its loss.
- Base: adjacent equal-tier cable nodes execute one exact RC response per tick,
  record C/t, A, J/t and loss, and retain topology across stable ticks.
- Bad: registering two terminals by `BlockPos.asLong()` overwrites one, or a
  diagnostic simulation advances telemetry/dirty state.

## 6. Tests Required

- Pure tests: `0.5CV^2`, four-times boundary, reconfiguration with joule
  preservation, exact RC known result, unlike capacitances, distance, loss,
  equilibrium and simulation purity.
- Runtime tests: same-position terminal index, internal open/close, tier
  mismatch, coupler isolation, deterministic phases and a stable 1024-node
  graph with no per-tick topology rebuild.
- Persistence tests: module-container round trip for schema/tier/terminal/joule
  data and safe reset for legacy, missing and mismatched payloads.
- GameTest: same-block terminal isolation, explicit internal edge, different
  tier rejection, existing non-electrical regression suite and chunk-unload
  lifecycle.
- Gates: `compileJava`, `compileGameTestJava`, `test`, `runGameTestServer`,
  `runData`, `build`, plus staged-path inspection.

## 7. Wrong vs Correct

Wrong: use rated voltage as a storage clamp and identify every terminal only by
its block position.

```java
energy = Math.min(energy, capacitance * maximumVoltage * maximumVoltage);
graph.addNode(position.asLong(), node);
```

Correct: preserve joules, derive voltage with the physical capacitor equation,
and register the stable terminal identity.

```java
double voltage = Math.sqrt(2.0D * energyJoules / capacitanceFarads);
graph.addNode(new PhysicalNodeKey(position, terminalId), node);
```
