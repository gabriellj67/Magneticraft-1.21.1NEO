package committee.nova.mods.magneticraft.content.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Electric axe for wood, foliage, vines and other soft plant blocks.
 */
public final class ElectricChainsawItem extends ElectricToolItem {
    private static final float DESTROY_SPEED = 10.0F;
    private static final Set<Block> EXPLICIT_PLANT_BLOCKS = Set.of(
            Blocks.VINE,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.WEEPING_VINES,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.CACTUS,
            Blocks.COBWEB,
            Blocks.GRASS,
            Blocks.TALL_GRASS,
            Blocks.FERN,
            Blocks.LARGE_FERN,
            Blocks.DEAD_BUSH,
            Blocks.SUGAR_CANE,
            Blocks.BAMBOO,
            Blocks.BAMBOO_SAPLING,
            Blocks.BROWN_MUSHROOM,
            Blocks.RED_MUSHROOM,
            Blocks.NETHER_SPROUTS,
            Blocks.CRIMSON_ROOTS,
            Blocks.WARPED_ROOTS,
            Blocks.HANGING_ROOTS,
            Blocks.KELP,
            Blocks.KELP_PLANT,
            Blocks.SEAGRASS,
            Blocks.TALL_SEAGRASS
    );

    public ElectricChainsawItem() {
        super(14.0F);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return canWork(stack) && isEffectiveBlock(state) ? DESTROY_SPEED : 1.0F;
    }

    @Override
    protected boolean isEffectiveBlock(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.CROPS)
                || state.is(BlockTags.CAVE_VINES)
                || EXPLICIT_PLANT_BLOCKS.contains(state.getBlock());
    }
}
