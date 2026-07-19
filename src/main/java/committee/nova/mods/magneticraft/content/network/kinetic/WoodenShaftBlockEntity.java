package committee.nova.mods.magneticraft.content.network.kinetic;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.KineticNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.kinetic.KineticNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

public final class WoodenShaftBlockEntity extends NetworkComponentBlockEntity {
    private final KineticNetworkModule kinetic;

    public WoodenShaftBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.WOODEN_SHAFT.get(), position, state);
        kinetic = addModule(new KineticNetworkModule(
                Magneticraft.id("kinetic"),
                this,
                new KineticNode(0.5D, 1_000.0D),
                200.0D,
                0.05D,
                side -> side.getAxis() == axis()
        ));
    }

    public KineticNetworkModule kinetic() {
        return kinetic;
    }

    @Override
    protected void tickComponent() {
        syncClientState(kinetic.clientStateHash());
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        if (side.getAxis() == axis()) {
            kinetic.toggleSide(side);
        }
        return Component.translatable(
                kinetic.isSideEnabled(side)
                        ? "message.magneticraft.connection_enabled"
                        : "message.magneticraft.connection_disabled",
                Component.translatable("direction.minecraft." + side.getName())
        );
    }

    private Direction.Axis axis() {
        return getBlockState().getValue(WoodenShaftBlock.AXIS);
    }
}
