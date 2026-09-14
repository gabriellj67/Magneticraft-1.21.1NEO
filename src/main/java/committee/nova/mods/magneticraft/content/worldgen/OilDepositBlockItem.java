package committee.nova.mods.magneticraft.content.worldgen;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** Creative/admin placement surface for full and exhausted finite oil deposits. */
public final class OilDepositBlockItem extends BlockItem {
    public OilDepositBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public ItemStack emptyStack() {
        ItemStack stack = new ItemStack(this);
        CompoundTag blockEntityTag = new CompoundTag();
        OilDepositPersistence.write(blockEntityTag, 0);
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltip, flag);
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        boolean empty = blockEntityData != null && OilDepositPersistence.read(blockEntityData.copyTag()) == 0;
        tooltip.add(Component.translatable(
                empty
                        ? "tooltip.magneticraft.oil_deposit.empty"
                        : "tooltip.magneticraft.oil_deposit.full"
        ).withStyle(ChatFormatting.GRAY));
    }
}
