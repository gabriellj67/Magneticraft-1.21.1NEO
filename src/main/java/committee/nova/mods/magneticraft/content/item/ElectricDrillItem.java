package committee.nova.mods.magneticraft.content.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Diamond-tier electric pickaxe and shovel replacement.
 */
public final class ElectricDrillItem extends ElectricToolItem {
    private static final float PICKAXE_SPEED = 44.0F;
    private static final float SHOVEL_SPEED = 15.0F;

    public ElectricDrillItem() {
        super(5.0F);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!canWork(stack)) {
            return 1.0F;
        }
        if (state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            return PICKAXE_SPEED;
        }
        return state.is(BlockTags.MINEABLE_WITH_SHOVEL) || state.is(Blocks.CAKE) ? SHOVEL_SPEED : 1.0F;
    }

    @Override
    protected boolean isEffectiveBlock(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) || state.is(BlockTags.MINEABLE_WITH_SHOVEL);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return canWork(stack);
    }
}
