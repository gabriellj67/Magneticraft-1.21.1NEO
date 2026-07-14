package committee.nova.mods.magneticraft.content.network.block;

import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Common server tick and wrench surface for network block entities.
 */
public abstract class NetworkComponentBlockEntity extends MachineBlockEntity {
    protected NetworkComponentBlockEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        Level level = getLevel();
        if (level != null && !level.isClientSide && getBlockState().getBlock() instanceof ConduitBlock) {
            ConduitBlock.refreshAround(level, getBlockPos());
        }
    }

    public final void serverTick() {
        tickModules();
        tickComponent();
        finishServerTick();
    }

    protected void tickComponent() {
    }

    public abstract Component configure(Direction side, boolean secondaryAction);

    public void dropContents(Level level) {
    }
}
