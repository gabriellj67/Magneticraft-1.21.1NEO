package committee.nova.mods.magneticraft.content.network.pneumatic;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.LogisticsTubeModule;
import committee.nova.mods.magneticraft.content.network.module.PressureNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One pneumatic segment hosting independent pressure and item-routing nodes.
 */
public final class PneumaticTubeBlockEntity extends NetworkComponentBlockEntity {
    private final PressureNetworkModule pressure;
    private final LogisticsTubeModule logistics;

    public PneumaticTubeBlockEntity(BlockPos position, BlockState state) {
        super(state.is(ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get())
                        ? ModBlockEntities.PNEUMATIC_RESTRICTION_TUBE.get()
                        : ModBlockEntities.PNEUMATIC_TUBE.get(),
                position,
                state);
        boolean restriction = state.is(ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get());
        pressure = addModule(new PressureNetworkModule(
                Magneticraft.id("pressure"),
                this,
                new PressureNode(1.0D, 1_000.0D),
                restriction ? 0.25D : 1.0D,
                restriction ? 25.0D : 100.0D,
                0.0D,
                side -> true
        ));
        logistics = addModule(new LogisticsTubeModule(
                Magneticraft.id("logistics"),
                this,
                restriction ? 100 : 0
        ));
    }

    public PressureNetworkModule pressure() {
        return pressure;
    }

    public LogisticsTubeModule logistics() {
        return logistics;
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        if (secondaryAction) {
            logistics.cycleRedstoneMode();
            return Component.translatable(
                    "message.magneticraft.redstone_mode",
                    Component.translatable("message.magneticraft.redstone_mode." + logistics.redstoneMode().name().toLowerCase())
            );
        }
        boolean enabled = !logistics.isSideEnabled(side);
        logistics.setSideEnabled(side, enabled);
        pressure.setSideEnabled(side, enabled);
        return Component.translatable(
                enabled
                        ? "message.magneticraft.connection_enabled"
                        : "message.magneticraft.connection_disabled",
                Component.translatable("direction.minecraft." + side.getName())
        );
    }

    @Override
    public void dropContents(Level level) {
        for (ItemStack stack : logistics.removeAllItems()) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    stack
            );
        }
    }
}
