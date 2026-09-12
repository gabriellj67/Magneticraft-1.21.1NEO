package committee.nova.mods.magneticraft.content.network.heat;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lossless thermal conduit; insulation is a block-level contact behavior.
 */
public final class HeatPipeBlockEntity extends NetworkComponentBlockEntity {
    private final HeatNetworkModule heat;

    public HeatPipeBlockEntity(BlockPos position, BlockState state) {
        super(state.is(ModNetworkBlocks.INSULATED_HEAT_PIPE.get())
                        ? ModBlockEntities.INSULATED_HEAT_PIPE.get()
                        : ModBlockEntities.HEAT_PIPE.get(),
                position,
                state);
        heat = addModule(new HeatNetworkModule(
                Magneticraft.id("heat"),
                this,
                new HeatNode(1.0D, 73.0D),
                Double.MAX_VALUE,
                side -> true
        ));
    }

    public HeatNetworkModule heat() {
        return heat;
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        heat.toggleSide(side);
        return Component.translatable(
                heat.isSideEnabled(side)
                        ? "message.magneticraft.connection_enabled"
                        : "message.magneticraft.connection_disabled",
                Component.translatable("direction.minecraft." + side.getName())
        );
    }
}
