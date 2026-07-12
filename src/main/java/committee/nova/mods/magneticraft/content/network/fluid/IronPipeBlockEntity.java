package committee.nova.mods.magneticraft.content.network.fluid;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.FluidPipeModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Legacy-rate iron fluid pipe with independently configured sides.
 */
public final class IronPipeBlockEntity extends NetworkComponentBlockEntity {
    public static final int CAPACITY = 160;
    public static final int MAX_RATE = 160;

    private final FluidPipeModule pipe;

    public IronPipeBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.IRON_PIPE.get(), position, state);
        pipe = addModule(new FluidPipeModule(
                Magneticraft.id("fluid_pipe"),
                this,
                CAPACITY,
                MAX_RATE
        ));
    }

    public FluidPipeModule pipe() {
        return pipe;
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        if (secondaryAction) {
            pipe.cycleRedstoneMode();
            return Component.translatable(
                    "message.magneticraft.redstone_mode",
                    Component.translatable("message.magneticraft.redstone_mode." + pipe.redstoneMode().name().toLowerCase())
            );
        }
        pipe.cycleSideMode(side);
        return Component.translatable(
                "message.magneticraft.fluid_side_mode",
                Component.translatable("direction.minecraft." + side.getName()),
                Component.translatable("message.magneticraft.fluid_side_mode." + pipe.sideMode(side).name().toLowerCase())
        );
    }
}
