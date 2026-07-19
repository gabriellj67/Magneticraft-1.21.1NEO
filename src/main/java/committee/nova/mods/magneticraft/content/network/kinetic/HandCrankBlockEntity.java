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

public final class HandCrankBlockEntity extends NetworkComponentBlockEntity {
    public static final int ACTIVE_TICKS = 20;
    public static final double GENERATED_JOULES_PER_TICK = 80.0D;

    private final KineticNetworkModule kinetic;
    private int activeTicks;

    public HandCrankBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.HAND_CRANK.get(), position, state);
        kinetic = addModule(new KineticNetworkModule(
                Magneticraft.id("kinetic"),
                this,
                new KineticNode(2.0D, 2_000.0D),
                200.0D,
                1.0D,
                side -> side == outputSide()
        ));
    }

    public KineticNetworkModule kinetic() {
        return kinetic;
    }

    public void activate() {
        activeTicks = ACTIVE_TICKS;
        markChangedAndSync();
    }

    @Override
    protected void tickComponent() {
        if (activeTicks > 0) {
            activeTicks--;
            kinetic.insertJoules(GENERATED_JOULES_PER_TICK, false);
        }
        syncClientState(31 * kinetic.clientStateHash() + activeTicks);
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        if (side == outputSide()) {
            kinetic.toggleSide(side);
        }
        return Component.translatable(
                kinetic.isSideEnabled(side)
                        ? "message.magneticraft.connection_enabled"
                        : "message.magneticraft.connection_disabled",
                Component.translatable("direction.minecraft." + side.getName())
        );
    }

    private Direction outputSide() {
        return getBlockState().getValue(HandCrankBlock.FACING).getOpposite();
    }
}
