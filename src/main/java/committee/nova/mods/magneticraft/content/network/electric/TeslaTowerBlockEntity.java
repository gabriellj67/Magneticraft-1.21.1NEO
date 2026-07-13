package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.TeslaTowerModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public final class TeslaTowerBlockEntity extends NetworkComponentBlockEntity {
    private final ElectricalNetworkModule electricity;

    public TeslaTowerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.TESLA_TOWER.get(), position, state);
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                new ElectricalNode(1.0D, 125.0D, 0.001D),
                side -> side.getAxis() == Direction.Axis.Y
        ));
        addModule(new TeslaTowerModule(Magneticraft.id("tesla_tower"), this, electricity.node()));
    }

    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        return Component.translatable(
                "message.magneticraft.voltage",
                String.format(java.util.Locale.ROOT, "%.2f", electricity.node().voltage())
        );
    }
}
