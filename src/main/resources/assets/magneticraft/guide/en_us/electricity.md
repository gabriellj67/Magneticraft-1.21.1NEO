# Electricity

Magneticraft uses a server-authoritative, physical RC network with stored joules as the source of truth. Voltage is derived from node energy and capacitance; charge moves through resistance without overshooting equilibrium, and resistive loss is measured rather than silently discarded.

Low, medium, and high voltage are separate tiers. Ordinary edges only connect terminals of the same tier. Transformers keep both terminals electrically isolated and transfer energy through an explicit, directional, efficiency-limited coupler.

Forge Energy does not replace native electricity. Native machines store energy only in their electrical node, measured in J, and derive voltage from that same state. Named adapters cross the boundary at `1 J = 1 FE`; recipes pause when the node is undervolted or cannot supply the required J/t.

Read [Fundamentals](electricity/fundamentals.md), [Conversion Rates](electricity/conversion_rates.md), and [Transportation](electricity/transportation.md).
