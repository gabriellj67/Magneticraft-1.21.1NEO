package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Displays the fluid preserved in a dropped small-tank item. */
public final class SmallTankBlockItem extends BlockItem {
    private static final String BLOCK_ENTITY_TAG = "BlockEntityTag";
    private static final String MODULES_TAG = "modules";
    private static final String FLUID_MODULE = Magneticraft.id("fluid").toString();

    public SmallTankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag blockEntityTag = stack.getTagElement(BLOCK_ENTITY_TAG);
        if (blockEntityTag == null) {
            return;
        }
        FluidStack fluid = FluidStack.loadFluidStackFromNBT(
                blockEntityTag.getCompound(MODULES_TAG).getCompound(FLUID_MODULE)
        );
        if (!fluid.isEmpty()) {
            tooltip.add(Component.translatable(
                    "tooltip.magneticraft.small_tank.line_0",
                    fluid.getAmount(),
                    fluid.getDisplayName()
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
