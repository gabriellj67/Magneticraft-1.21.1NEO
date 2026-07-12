package committee.nova.mods.magneticraft.content.network.heat;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Directional ambient heat sink with no hidden network loss elsewhere.
 */
public final class HeatSinkBlockEntity extends NetworkComponentBlockEntity {
    private static final double DISSIPATION_WATTS_PER_KELVIN = 5.0D;

    private final HeatNetworkModule heat;

    public HeatSinkBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.HEAT_SINK.get(), position, state);
        heat = addModule(new HeatNetworkModule(
                Magneticraft.id("heat"),
                this,
                new HeatNode(1.0D, 73.0D),
                Double.MAX_VALUE,
                side -> !getBlockState().hasProperty(HeatSinkBlock.FACING)
                        || side != getBlockState().getValue(HeatSinkBlock.FACING).getOpposite()
        ));
    }

    public HeatNetworkModule heat() {
        return heat;
    }

    @Override
    protected void tickComponent() {
        double difference = heat.node().temperatureKelvin() - HeatNode.AMBIENT_TEMPERATURE_KELVIN;
        if (difference > 0.0D) {
            double removed = heat.node().removeHeat(difference * DISSIPATION_WATTS_PER_KELVIN, false);
            if (removed > 0.0D) {
                markChanged();
            }
        }
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
