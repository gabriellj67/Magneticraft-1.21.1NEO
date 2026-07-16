package committee.nova.mods.magneticraft.content.network.pressure;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.PressureNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public final class BrassPressurePipeBlockEntity extends NetworkComponentBlockEntity {
    public static final double VOLUME_LITERS = 2.0D;
    public static final double MAX_PRESSURE_KPA = 20_000.0D;
    public static final double CONDUCTANCE = 4.0D;
    public static final double MAX_GAS_PER_TICK = 400.0D;

    private final PressureNetworkModule pressure;

    public BrassPressurePipeBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.BRASS_PRESSURE_PIPE.get(), position, state);
        pressure = addModule(new PressureNetworkModule(
                Magneticraft.id("pressure"),
                this,
                new PressureNode(VOLUME_LITERS, MAX_PRESSURE_KPA),
                CONDUCTANCE,
                MAX_GAS_PER_TICK,
                0.0D,
                side -> true
        ));
    }

    public PressureNetworkModule pressure() {
        return pressure;
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        if (secondaryAction) {
            pressure.cycleRedstoneMode();
            return Component.translatable(
                    "message.magneticraft.redstone_mode",
                    Component.translatable("message.magneticraft.redstone_mode."
                            + pressure.redstoneMode().name().toLowerCase())
            );
        }
        pressure.toggleSide(side);
        return Component.translatable(
                pressure.isSideEnabled(side)
                        ? "message.magneticraft.connection_enabled"
                        : "message.magneticraft.connection_disabled",
                Component.translatable("direction.minecraft." + side.getName())
        );
    }
}
