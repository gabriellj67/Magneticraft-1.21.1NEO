package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Six-way 125V cable segment.
 */
public final class ElectricCableBlockEntity extends NetworkComponentBlockEntity {
    private final ElectricalNetworkModule electricity;

    public ElectricCableBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.ELECTRIC_CABLE.get(), position, state);
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                new ElectricalNode(0.25D, 125.0D, 0.001D),
                0.001D,
                8.0D,
                side -> true
        ));
    }

    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        electricity.toggleSide(side);
        return Component.translatable(
                electricity.isSideEnabled(side)
                        ? "message.magneticraft.connection_enabled"
                        : "message.magneticraft.connection_disabled",
                Component.translatable("direction.minecraft." + side.getName())
        );
    }
}
