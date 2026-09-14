package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.init.ModComputerContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fixed computer host. World-facing behavior is deliberately limited to redstone output.
 */
public final class ComputerBlockEntity extends ProgrammableBlockEntity {
    public ComputerBlockEntity(BlockPos position, BlockState state) {
        super(ModComputerContent.COMPUTER_BLOCK_ENTITY.get(), position, state);
    }

    public static void serverTick(Level level, BlockPos position, BlockState state, ComputerBlockEntity computer) {
        computer.tickComputer();
        computer.finishServerTick();
    }
}
