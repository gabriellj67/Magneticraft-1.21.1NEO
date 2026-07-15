# Electricity Transportation

Electric cables connect adjacent compatible faces of the same tier. A tier is fixed when the item is crafted and cannot be changed for free with a wrench. Breaking a cable or device removes its runtime edge; saved local state reconstructs the graph after reload without rebuilding a stable topology every tick.

Use a copper wire coil to select one endpoint while sneaking, then use it on a compatible second endpoint. The built-in ranges are:

| Tier | Connector | Pole |
|---|---:|---:|
| Low | 8 blocks | 16 blocks |
| Medium | 16 blocks | 32 blocks |
| High | 32 blocks | 64 blocks |

Different tiers, port types, and dimensions are rejected without consuming the wire coil. Long-distance resistance scales with the real geometric distance.

Standard and heavy protection devices are rated for 8 C/t and 16 C/t. Sustained overload accumulates thermal stress: fuses blow, breakers trip, and an unprotected cable eventually becomes a burnt, disconnected cable. Redstone forces a breaker open; removing the signal does not clear a manual trip. Repair faulted machines only after every terminal is below 95% of nominal voltage.

Unloaded remote endpoints pause transfer. Connections do not force-load chunks, and breaking either endpoint cleans the saved link before it can transfer again.
