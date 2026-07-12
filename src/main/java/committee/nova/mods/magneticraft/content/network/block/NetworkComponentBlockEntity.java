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

    public final void serverTick() {
        tickModules();
        tickComponent();
    }

    protected void tickComponent() {
    }

    public abstract Component configure(Direction side, boolean secondaryAction);

    public void dropContents(Level level) {
    }
}
