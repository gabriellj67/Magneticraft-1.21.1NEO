package committee.nova.mods.magneticraft.content.network.logistics;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ConveyorBeltModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class ConveyorBeltBlockEntity extends NetworkComponentBlockEntity {
    private final ConveyorBeltModule belt;

    public ConveyorBeltBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.CONVEYOR_BELT.get(), position, state);
        belt = addModule(new ConveyorBeltModule(Magneticraft.id("conveyor"), this, this::facing));
    }

    public ConveyorBeltModule belt() {
        return belt;
    }

    public Direction facing() {
        return getBlockState().getValue(ConveyorBeltBlock.FACING);
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        belt.cycleRedstoneMode();
        return Component.translatable(
                "message.magneticraft.redstone_mode",
                Component.translatable("message.magneticraft.redstone_mode." + belt.redstoneMode().name().toLowerCase())
        );
    }

    @Override
    public void dropContents(Level level) {
        for (ItemStack stack : belt.removeAll()) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.25D,
                    worldPosition.getZ() + 0.5D,
                    stack
            );
        }
    }
}
