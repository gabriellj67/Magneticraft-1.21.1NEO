package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** Displays the fluid preserved in a dropped small-tank item. */
public final class SmallTankBlockItem extends BlockItem {
    private static final String MODULES_TAG = "modules";
    private static final String FLUID_MODULE = Magneticraft.id("fluid").toString();

    public SmallTankBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        HolderLookup.Provider registries = context.registries();
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (registries == null || blockEntityData == null) {
            return;
        }
        CompoundTag blockEntityTag = blockEntityData.copyTag();
        FluidStack fluid = FluidStack.parseOptional(
                registries,
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
