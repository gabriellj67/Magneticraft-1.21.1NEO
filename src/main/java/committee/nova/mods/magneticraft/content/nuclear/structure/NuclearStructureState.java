package committee.nova.mods.magneticraft.content.nuclear.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Shared visual projection state for formed nuclear structures. */
public final class NuclearStructureState {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private NuclearStructureState() {
    }

    public static void setFormed(
            ServerLevel level,
            Iterable<BlockPos> positions,
            boolean formed
    ) {
        for (BlockPos position : positions) {
            setFormed(level, position, formed);
        }
    }

    public static void setFormed(
            ServerLevel level,
            BlockPos minimum,
            BlockPos maximum,
            boolean formed
    ) {
        for (BlockPos position : BlockPos.betweenClosed(minimum, maximum)) {
            setFormed(level, position, formed);
        }
    }

    private static void setFormed(ServerLevel level, BlockPos position, boolean formed) {
        if (!level.hasChunkAt(position)) {
            return;
        }
        BlockState state = level.getBlockState(position);
        if (state.hasProperty(FORMED) && state.getValue(FORMED) != formed) {
            level.setBlock(position, state.setValue(FORMED, formed), Block.UPDATE_CLIENTS);
        }
    }
}
