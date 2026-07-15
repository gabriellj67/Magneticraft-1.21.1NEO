# Electricity Fundamentals

Node energy and voltage follow `E = 1/2 C V²`. A network tick is 0.05 seconds, so the displayed conversions are `A = C/t × 20` and `W = J/t × 20`. Maximum voltage is an insulation and damage threshold, not an energy clamp.

Built-in tiers are:

- Low voltage: 60 V minimum, 120 V nominal, 125 V maximum.
- Medium voltage: 240 V minimum, 480 V nominal, 500 V maximum.
- High voltage: 960 V minimum, 1920 V nominal, 2000 V maximum.

Machines expose only their declared electrical terminal faces. Fluid, gas, heat, item, and logistics ports remain separate. A visually adjacent cable does not create a connection when the tier or face is incompatible.

Use the voltmeter on a terminal for point readings. Sneak-use it in the air to cycle to network summary or bounded fault-location mode. The search only visits loaded nodes and stops after 4096 nodes.

The server owns all network mutation and conservation checks. Client displays are snapshots and cannot inject energy or create links.
