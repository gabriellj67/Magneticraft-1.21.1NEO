package committee.nova.mods.magneticraft.content.worldgen;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

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
        stack.addTagElement("BlockEntityTag", blockEntityTag);
        return stack;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundTag blockEntityTag = stack.getTagElement("BlockEntityTag");
        boolean empty = blockEntityTag != null && OilDepositPersistence.read(blockEntityTag) == 0;
        tooltip.add(Component.translatable(
                empty
                        ? "tooltip.magneticraft.oil_deposit.empty"
                        : "tooltip.magneticraft.oil_deposit.full"
        ).withStyle(ChatFormatting.GRAY));
    }
}
