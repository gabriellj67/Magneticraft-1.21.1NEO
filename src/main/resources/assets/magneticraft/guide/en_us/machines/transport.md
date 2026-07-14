# Transport

Conveyors move an internal carried stack, support right-click insertion and removal, and apply blockage backpressure. Items transfer exactly once at an accepting endpoint; unloaded destinations pause safely.

Inserters move between neighboring inventories with direction, filter, speed, and stack upgrades. Pneumatic tubes use a separate bounded logistics graph with relays, filters, transposers, and restriction tubes.

Fluid and heat pipes belong to their own physical domains. Simulation calls are read-only, and no transport network force-loads chunks.
