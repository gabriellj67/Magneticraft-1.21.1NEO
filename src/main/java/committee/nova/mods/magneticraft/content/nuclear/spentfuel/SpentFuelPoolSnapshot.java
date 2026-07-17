package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Immutable formed-pool geometry and exact transfer/cooling penetration. */
public record SpentFuelPoolSnapshot(
        BlockPos controller,
        Direction facing,
        int width,
        int length,
        int height,
        BlockPos minimum,
        BlockPos maximum,
        BlockPos port,
        int waterBlocks
) {
    public SpentFuelPoolSnapshot {
        controller = controller.immutable();
        minimum = minimum.immutable();
        maximum = maximum.immutable();
        port = port.immutable();
    }
}
