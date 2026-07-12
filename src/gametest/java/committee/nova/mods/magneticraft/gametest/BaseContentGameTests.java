package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.SulfurItem;
import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Runtime contracts for the stateless content and fluid families migrated in task two.
 */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseContentGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    private BaseContentGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void solidBlocksDropThemselvesAndCarryMiningTags(GameTestHelper helper) {
        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            Block block = ModBlocks.get(definition).get();
            helper.setBlock(TEST_POS, block);
            BlockState state = helper.getBlockState(TEST_POS);
            List<ItemStack> drops = Block.getDrops(
                    state,
                    helper.getLevel(),
                    helper.absolutePos(TEST_POS),
                    null
            );

            helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), definition.id() + " is not pickaxe-mineable");
            helper.assertTrue(
                    Math.abs(state.getDestroySpeed(helper.getLevel(), helper.absolutePos(TEST_POS)) - 1.5F) < 0.001F,
                    definition.id() + " has the wrong hardness"
            );
            helper.assertTrue(
                    Math.abs(block.getExplosionResistance(
                            state,
                            helper.getLevel(),
                            helper.absolutePos(TEST_POS),
                            null
                    ) - 10.0F) < 0.001F,
                    definition.id() + " has the wrong explosion resistance"
            );
            helper.assertTrue(
                    drops.size() == 1 && drops.get(0).is(block.asItem()),
                    definition.id() + " did not drop itself"
            );
            switch (definition.miningTier()) {
                case STONE -> helper.assertTrue(
                        state.is(BlockTags.NEEDS_STONE_TOOL),
                        definition.id() + " lost its stone-tool requirement"
                );
                case IRON -> helper.assertTrue(
                        state.is(BlockTags.NEEDS_IRON_TOOL),
                        definition.id() + " lost its iron-tool requirement"
                );
                case NONE -> {
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void liquidAndLegacyGasBucketsRoundTrip(GameTestHelper helper) {
        assertBucketRoundTrip(helper, FluidDefinition.OIL, new BlockPos(1, 1, 1));
        assertBucketRoundTrip(helper, FluidDefinition.STEAM, new BlockPos(1, 1, 2));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void countedSmeltingRecipesRetainLegacyOutputAmounts(GameTestHelper helper) {
        assertRecipeResult(helper, "smelting/iron_chunk", ModItems.ingot(Metal.IRON), 2);
        assertRecipeResult(helper, "smelting/galena_rocky_chunk", ModItems.ingot(Metal.LEAD), 2);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hammersRetainDurabilityAndBonusDamage(GameTestHelper helper) {
        for (HammerType type : HammerType.values()) {
            Zombie attacker = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
            Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 2);
            ItemStack hammer = new ItemStack(ModItems.hammer(type).get());
            float initialHealth = target.getHealth();

            ModItems.hammer(type).get().hurtEnemy(hammer, target, attacker);

            helper.assertTrue(hammer.getMaxDamage() == type.durability(), type.id() + " has the wrong durability");
            helper.assertTrue(
                    hammer.getDamageValue() == HammerType.DURABILITY_COST_PER_HIT,
                    type.id() + " consumed the wrong durability"
            );
            helper.assertTrue(
                    Math.abs(target.getHealth() - (initialHealth - type.bonusDamage())) < 0.001F,
                    type.id() + " dealt the wrong bonus damage"
            );
            attacker.discard();
            target.discard();
        }
        ItemStack sulfur = new ItemStack(ModItems.component(CraftingComponent.SULFUR).get());
        helper.assertTrue(
                sulfur.getItem().getBurnTime(sulfur, RecipeType.SMELTING) == SulfurItem.BURN_TIME_TICKS,
                "Sulfur lost its 800-tick fuel value"
        );
        helper.succeed();
    }

    private static void assertBucketRoundTrip(
            GameTestHelper helper,
            FluidDefinition definition,
            BlockPos relativePos
    ) {
        ModFluids.FluidFamily family = ModFluids.get(definition);
        BucketItem bucket = (BucketItem) family.bucket().get();
        BlockPos absolutePos = helper.absolutePos(relativePos);
        ItemStack bucketStack = new ItemStack(bucket);

        boolean placed = bucket.emptyContents(null, helper.getLevel(), absolutePos, null, bucketStack);
        helper.assertTrue(placed, definition.id() + " bucket could not place its fluid");

        FluidState fluidState = helper.getLevel().getFluidState(absolutePos);
        helper.assertTrue(
                family.type().get().getTemperature() == definition.temperatureKelvin(),
                definition.id() + " has the wrong temperature"
        );
        helper.assertTrue(
                family.type().get().getDensity() == definition.density(),
                definition.id() + " has the wrong density"
        );
        helper.assertTrue(
                family.type().get().getViscosity() == definition.viscosity(),
                definition.id() + " has the wrong viscosity"
        );
        helper.assertTrue(fluidState.getType() == family.source().get(), definition.id() + " placed the wrong source");
        helper.assertTrue(
                fluidState.is(ModTags.Fluids.magneticraft(definition)),
                definition.id() + " is missing its Magneticraft fluid tag"
        );
        helper.assertTrue(
                fluidState.is(ModTags.Fluids.forge(definition)),
                definition.id() + " is missing its Forge fluid tag"
        );
        if (definition.isGaseous()) {
            helper.assertTrue(fluidState.is(Tags.Fluids.GASEOUS), definition.id() + " is missing forge:gaseous");
        }

        FluidStack stack = new FluidStack(fluidState.getType(), FluidType.BUCKET_VOLUME);
        helper.assertTrue(stack.getFluid() == family.source().get(), definition.id() + " FluidStack lost its type");
        helper.assertTrue(
                helper.getLevel().getBlockState(absolutePos).getBlock() instanceof BucketPickup,
                definition.id() + " block cannot be picked up"
        );
        BucketPickup pickup = (BucketPickup) helper.getLevel().getBlockState(absolutePos).getBlock();
        ItemStack pickedUp = pickup.pickupBlock(
                helper.getLevel(),
                absolutePos,
                helper.getLevel().getBlockState(absolutePos)
        );
        helper.assertTrue(pickedUp.is(bucket), definition.id() + " returned the wrong bucket");
        helper.assertTrue(helper.getLevel().getFluidState(absolutePos).isEmpty(), definition.id() + " remained after pickup");
    }

    private static void assertRecipeResult(
            GameTestHelper helper,
            String recipePath,
            net.minecraft.world.item.Item expectedItem,
            int expectedCount
    ) {
        ResourceLocation recipeId = Magneticraft.id(recipePath);
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(recipeId)
                .orElseThrow(() -> new AssertionError("Missing recipe " + recipeId));
        ItemStack result = recipe.getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(expectedItem), recipeId + " returned the wrong item");
        helper.assertTrue(result.getCount() == expectedCount, recipeId + " returned count " + result.getCount());
    }
}
