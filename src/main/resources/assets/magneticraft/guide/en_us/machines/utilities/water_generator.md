# Water Generator

The water generator provides an internal infinite water source and actively offers a bounded amount to each neighboring compatible tank every tick.

Every side is evaluated independently, and only the amount accepted by the destination is committed. Simulation and blocked outputs do not consume or create extra transfer state.

The machine checks loaded neighbors only and never force-loads a destination chunk.
