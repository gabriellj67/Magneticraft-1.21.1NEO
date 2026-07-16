package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.block.DecorativeBlockFamily;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.SulfurItem;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Runtime contracts for the stateless content and fluid families migrated in task two.
 */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BaseContentGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);
    private static final Set<String> FORBIDDEN_LEGACY_IDS = Set.of(
            "ores",
            "storage_blocks",
            "tile_limestone",
            "cobbled_limestone",
            "cobbled_burnt_limestone",
            "ingots",
            "nuggets",
            "light_plates",
            "heavy_plates",
            "chunks",
            "dusts",
            "rocky_chunks",
            "crafting",
            "multiblock_parts",
            "battery_item_low",
            "battery_item_medium",
            "battery",
            "grate",
            "box",
            "relay",
            "filter",
            "transposer",
            "rf_heater",
            "rf_transformer",
            "iron_pipe",
            "connector",
            "energy_receiver",
            "multiblock_base",
            "electric_multiblock_part",
            "striped_multiblock_part",
            "multiblock_column",
            "big_combustion_chamber",
            "big_electric_furnace",
            "big_steam_boiler",
            "container",
            "infinite_energy",
            "oil",
            "hot_crude",
            "plastic",
            "fuel",
            "crushing_hit",
            "crushing_final",
            "water_flow",
            "water_flow_end",
            "crushing",
            "sluice",
            "gasification",
            "fluid_fuel",
            "single_block_machine",
            "advanced_multiblock",
            "programmable",
            "advanced_processing",
            "broken_gear",
            "iron_gear",
            "steel_gear",
            "tungsten_gear"
    );

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
    public static void ownedRegistriesUseTheirContentIds(GameTestHelper helper) {
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            ResourceLocation id = Magneticraft.id(definition.id());
            assertRegistryEntry(
                    helper,
                    "single-block block entity type",
                    ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ModBlockEntities.singleBlockMachine(definition),
                    id
            );
            if (definition.hasMenu()) {
                assertRegistryEntry(
                        helper,
                        "single-block menu",
                        ForgeRegistries.MENU_TYPES,
                        ModMenus.singleBlockMachine(definition),
                        id
                );
            }
        }
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            ResourceLocation id = Magneticraft.id(definition.id());
            assertRegistryEntry(
                    helper,
                    "multiblock block entity type",
                    ForgeRegistries.BLOCK_ENTITY_TYPES,
                    ModBlockEntities.advancedMultiblock(definition),
                    id
            );
            assertRegistryEntry(
                    helper,
                    "multiblock menu",
                    ForgeRegistries.MENU_TYPES,
                    ModMenus.advancedMultiblock(definition),
                    id
            );
        }
        ModRecipeTypes.advancedProcessingTypes().forEach((definition, type) -> {
            ResourceLocation id = Magneticraft.id(definition.id());
            assertRegistryEntry(helper, "processing recipe type", ForgeRegistries.RECIPE_TYPES, type, id);
            assertRegistryEntry(
                    helper,
                    "processing recipe serializer",
                    ForgeRegistries.RECIPE_SERIALIZERS,
                    ModRecipeTypes.advancedProcessingSerializer(definition),
                    id
            );
        });
        assertRegistryEntry(
                helper,
                "insulated heat pipe block entity type",
                ForgeRegistries.BLOCK_ENTITY_TYPES,
                ModBlockEntities.INSULATED_HEAT_PIPE,
                Magneticraft.id("insulated_heat_pipe")
        );
        assertRegistryEntry(
                helper,
                "pneumatic restriction tube block entity type",
                ForgeRegistries.BLOCK_ENTITY_TYPES,
                ModBlockEntities.PNEUMATIC_RESTRICTION_TUBE,
                Magneticraft.id("pneumatic_restriction_tube")
        );
        assertRegistryEntry(
                helper,
                "computer menu",
                ForgeRegistries.MENU_TYPES,
                ModMenus.COMPUTER,
                Magneticraft.id("computer")
        );
        assertRegistryEntry(
                helper,
                "mining robot menu",
                ForgeRegistries.MENU_TYPES,
                ModMenus.MINING_ROBOT,
                Magneticraft.id("mining_robot")
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void liquidAndLegacyGasBucketsRoundTrip(GameTestHelper helper) {
        assertBucketRoundTrip(helper, FluidDefinition.OIL, new BlockPos(1, 1, 1));
        assertBucketRoundTrip(helper, FluidDefinition.STEAM, new BlockPos(1, 1, 2));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fluidFamiliesUseFrozenRuntimeIds(GameTestHelper helper) {
        for (FluidDefinition definition : FluidDefinition.values()) {
            ModFluids.FluidFamily family = ModFluids.get(definition);
            ResourceLocation baseId = Magneticraft.id(definition.id());
            ResourceLocation flowingId = Magneticraft.id(definition.id() + "_flowing");
            ResourceLocation bucketId = Magneticraft.id(definition.id() + "_bucket");

            assertRegistryEntry(helper, "fluid type", ForgeRegistries.FLUID_TYPES.get(), family.type(), baseId);
            assertRegistryEntry(helper, "source fluid", ForgeRegistries.FLUIDS, family.source(), baseId);
            assertRegistryEntry(helper, "flowing fluid", ForgeRegistries.FLUIDS, family.flowing(), flowingId);
            assertRegistryEntry(helper, "fluid block", ForgeRegistries.BLOCKS, family.block(), baseId);
            assertRegistryEntry(helper, "fluid bucket", ForgeRegistries.ITEMS, family.bucket(), bucketId);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void definitionDrivenContentUsesFrozenRuntimeIds(GameTestHelper helper) {
        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            assertBlockItemId(helper, definition.id(), ModBlocks.get(definition));
        }
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            assertBlockItemId(helper, definition.id(), ModMachineBlocks.machine(definition));
        }
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            assertBlockItemId(helper, definition.id(), ModAdvancedBlocks.controller(definition));
        }
        ModItems.materials().forEach((form, entries) -> entries.forEach(
                (metal, item) -> assertItemId(helper, form.id(metal), item)
        ));
        for (CraftingComponent component : CraftingComponent.values()) {
            assertItemId(helper, component.id(), ModItems.component(component));
        }
        for (HammerType type : HammerType.values()) {
            assertItemId(helper, type.id(), ModItems.hammer(type));
        }

        assertItemId(helper, "guide_book", ModItems.GUIDE_BOOK);
        assertItemId(helper, "low_voltage_battery", ModMachineItems.LOW_BATTERY);
        assertItemId(helper, "medium_voltage_battery", ModMachineItems.MEDIUM_BATTERY);
        assertItemId(helper, "electric_drill", ModMachineItems.ELECTRIC_DRILL);
        assertItemId(helper, "electric_chainsaw", ModMachineItems.ELECTRIC_CHAINSAW);
        assertItemId(helper, "electric_piston", ModMachineItems.ELECTRIC_PISTON);
        assertItemId(helper, "voltmeter", ModMachineItems.VOLTMETER);
        assertItemId(helper, "thermometer", ModMachineItems.THERMOMETER);
        assertItemId(helper, "inserter_speed_upgrade", ModMachineItems.INSERTER_SPEED_UPGRADE);
        assertItemId(helper, "inserter_stack_upgrade", ModMachineItems.INSERTER_STACK_UPGRADE);
        assertItemId(helper, "wrench", ModNetworkItems.WRENCH);
        assertBlockItemId(helper, "computer", ModComputerContent.COMPUTER);
        assertBlockItemId(helper, "mining_robot", ModComputerContent.MINING_ROBOT);
        assertItemId(helper, "floppy_disk", ModComputerContent.FLOPPY_DISK);

        assertPublicContentRegistryObjectsAreBound(helper, ModMachineBlocks.class);
        assertPublicContentRegistryObjectsAreBound(helper, ModMachineItems.class);
        assertPublicContentRegistryObjectsAreBound(helper, ModNetworkBlocks.class);
        assertPublicContentRegistryObjectsAreBound(helper, ModNetworkItems.class);
        assertPublicContentRegistryObjectsAreBound(helper, ModAdvancedBlocks.class);
        assertPublicContentRegistryObjectsAreBound(helper, ModComputerContent.class);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void renamedLegacyIdsAreAbsentFromRuntimeRegistries(GameTestHelper helper) {
        assertNoForbiddenIds(helper, "blocks", ForgeRegistries.BLOCKS.getKeys());
        assertNoForbiddenIds(helper, "items", ForgeRegistries.ITEMS.getKeys());
        assertNoForbiddenIds(helper, "fluid types", ForgeRegistries.FLUID_TYPES.get().getKeys());
        assertNoForbiddenIds(helper, "fluids", ForgeRegistries.FLUIDS.getKeys());
        assertNoForbiddenIds(helper, "block entity types", ForgeRegistries.BLOCK_ENTITY_TYPES.getKeys());
        assertNoForbiddenIds(helper, "menus", ForgeRegistries.MENU_TYPES.getKeys());
        assertNoForbiddenIds(helper, "recipe types", ForgeRegistries.RECIPE_TYPES.getKeys());
        assertNoForbiddenIds(helper, "recipe serializers", ForgeRegistries.RECIPE_SERIALIZERS.getKeys());
        assertNoForbiddenIds(helper, "sounds", ForgeRegistries.SOUND_EVENTS.getKeys());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void internalAirBubbleNeverBecomesObtainableContent(GameTestHelper helper) {
        ResourceLocation airBubble = Magneticraft.id("air_bubble");
        ResourceLocation tubeLight = Magneticraft.id("tube_light");

        helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(airBubble), "air_bubble runtime block is missing");
        helper.assertTrue(!ForgeRegistries.ITEMS.containsKey(airBubble), "air_bubble must not have a BlockItem");
        helper.assertTrue(
                Objects.equals(ModMachineBlocks.AIR_BUBBLE.get().getLootTable(), BuiltInLootTables.EMPTY),
                "air_bubble must use the empty loot table"
        );
        helper.assertTrue(
                ModMachineBlocks.blockItems().stream()
                        .map(RegistryObject::get)
                        .noneMatch(item -> Objects.equals(ForgeRegistries.ITEMS.getKey(item), airBubble)),
                "air_bubble leaked into the creative item projection"
        );
        helper.assertTrue(
                helper.getLevel().getRecipeManager().getRecipes().stream()
                        .map(recipe -> recipe.getResultItem(helper.getLevel().registryAccess()))
                        .filter(stack -> !stack.isEmpty())
                        .noneMatch(stack -> Objects.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()), airBubble)),
                "air_bubble leaked into a recipe result"
        );

        helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(tubeLight), "tube_light public block is missing");
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(tubeLight), "tube_light public BlockItem is missing");
        helper.assertTrue(
                ModMachineBlocks.blockItems().stream()
                        .map(RegistryObject::get)
                        .anyMatch(item -> Objects.equals(ForgeRegistries.ITEMS.getKey(item), tubeLight)),
                "tube_light was incorrectly filtered as internal content"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void decorativeFamiliesUseStableIdsShapesAndMiningTags(GameTestHelper helper) {
        helper.assertTrue(ModBlocks.decorativeFamilies().size() == 9, "decorative family count changed");
        Set<Item> decorativeItems = new HashSet<>();
        for (DecorativeBlockFamily family : DecorativeBlockFamily.values()) {
            ModBlocks.DecorativeFamilyBlocks blocks = ModBlocks.decorativeFamily(family);
            assertBlockItemId(helper, family.stairsId(), blocks.stairs());
            assertBlockItemId(helper, family.slabId(), blocks.slab());
            helper.assertTrue(blocks.stairs().get() instanceof StairBlock,
                    family.stairsId() + " is not a stair block");
            helper.assertTrue(blocks.slab().get() instanceof SlabBlock,
                    family.slabId() + " is not a slab block");
            helper.assertTrue(blocks.stairs().get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                    family.stairsId() + " is missing the pickaxe mining tag");
            helper.assertTrue(blocks.slab().get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                    family.slabId() + " is missing the pickaxe mining tag");
            helper.assertTrue(decorativeItems.add(blocks.stairs().get().asItem()),
                    family.stairsId() + " appeared twice in the decorative catalogue");
            helper.assertTrue(decorativeItems.add(blocks.slab().get().asItem()),
                    family.slabId() + " appeared twice in the decorative catalogue");
            if (family.registersBase()) {
                assertBlockItemId(helper, family.baseId(), blocks.base());
                helper.assertTrue(blocks.base().get().defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE),
                        family.baseId() + " is missing the pickaxe mining tag");
                helper.assertTrue(decorativeItems.add(blocks.base().get().asItem()),
                        family.baseId() + " appeared twice in the decorative catalogue");
            } else {
                helper.assertTrue(blocks.base() == ModBlocks.get(Objects.requireNonNull(family.existingBase())),
                        family.baseId() + " did not reuse its existing base block");
            }
        }
        long creativeOccurrences = ModBlocks.blockItems().stream()
                .map(RegistryObject::get)
                .filter(decorativeItems::contains)
                .count();
        helper.assertTrue(decorativeItems.size() == 19, "decorative creative projection is incomplete");
        helper.assertTrue(creativeOccurrences == decorativeItems.size(),
                "a decorative item appears more than once in the creative projection");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void decorativeRecipesUsePlannedYields(GameTestHelper helper) {
        for (DecorativeBlockFamily family : DecorativeBlockFamily.values()) {
            ModBlocks.DecorativeFamilyBlocks blocks = ModBlocks.decorativeFamily(family);
            assertRecipeResult(helper, "crafting/" + family.stairsId(), blocks.stairs().get().asItem(), 4);
            assertRecipeResult(helper, "crafting/" + family.slabId(), blocks.slab().get().asItem(), 6);
            assertRecipeResult(helper, "stonecutting/" + family.stairsId(), blocks.stairs().get().asItem(), 1);
            assertRecipeResult(helper, "stonecutting/" + family.slabId(), blocks.slab().get().asItem(), 2);
        }
        ModBlocks.DecorativeFamilyBlocks roof = ModBlocks.decorativeFamily(
                DecorativeBlockFamily.TERRACOTTA_ROOF_TILE
        );
        assertRecipeResult(helper, "crafting/roof_tile", roof.base().get().asItem(), 2);
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

    private static void assertBlockItemId(
            GameTestHelper helper,
            String expectedPath,
            RegistryObject<? extends Block> blockObject
    ) {
        ResourceLocation expectedId = Magneticraft.id(expectedPath);
        assertRegistryEntry(helper, "block", ForgeRegistries.BLOCKS, blockObject, expectedId);
        Item blockItem = blockObject.get().asItem();
        helper.assertTrue(blockItem != Items.AIR, expectedId + " is missing its block item");
        helper.assertTrue(
                expectedId.equals(ForgeRegistries.ITEMS.getKey(blockItem)),
                expectedId + " block item has runtime ID " + ForgeRegistries.ITEMS.getKey(blockItem)
        );
        helper.assertTrue(
                ForgeRegistries.ITEMS.getValue(expectedId) == blockItem,
                expectedId + " does not resolve to its block item"
        );
    }

    private static void assertItemId(
            GameTestHelper helper,
            String expectedPath,
            RegistryObject<? extends Item> itemObject
    ) {
        assertRegistryEntry(
                helper,
                "item",
                ForgeRegistries.ITEMS,
                itemObject,
                Magneticraft.id(expectedPath)
        );
    }

    private static <T> void assertRegistryEntry(
            GameTestHelper helper,
            String kind,
            IForgeRegistry<T> registry,
            RegistryObject<? extends T> entry,
            ResourceLocation expectedId
    ) {
        T value = entry.get();
        helper.assertTrue(
                expectedId.equals(entry.getId()),
                kind + " registry object expected " + expectedId + " but declares " + entry.getId()
        );
        helper.assertTrue(
                expectedId.equals(registry.getKey(value)),
                kind + " " + expectedId + " has runtime ID " + registry.getKey(value)
        );
        helper.assertTrue(
                registry.getValue(expectedId) == value,
                kind + " " + expectedId + " does not resolve to its registered value"
        );
    }

    private static void assertPublicContentRegistryObjectsAreBound(GameTestHelper helper, Class<?> owner) {
        for (Field field : owner.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers())
                    || !Modifier.isStatic(field.getModifiers())
                    || !RegistryObject.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                RegistryObject<?> entry = (RegistryObject<?>) field.get(null);
                Object value = entry.get();
                ResourceLocation runtimeId = runtimeRegistryId(value);
                if (runtimeId == null) {
                    continue;
                }
                helper.assertTrue(
                        entry.getId().equals(runtimeId),
                        owner.getSimpleName() + "." + field.getName()
                                + " declares " + entry.getId() + " but is registered as " + runtimeId
                );
                if (value instanceof Block block && block.asItem() != Items.AIR) {
                    helper.assertTrue(
                            runtimeId.equals(ForgeRegistries.ITEMS.getKey(block.asItem())),
                            runtimeId + " block and block item IDs diverged"
                    );
                }
            } catch (IllegalAccessException exception) {
                throw new AssertionError("Cannot inspect " + owner.getName() + "." + field.getName(), exception);
            }
        }
    }

    private static ResourceLocation runtimeRegistryId(Object value) {
        if (value instanceof Block block) {
            return ForgeRegistries.BLOCKS.getKey(block);
        }
        if (value instanceof Item item) {
            return ForgeRegistries.ITEMS.getKey(item);
        }
        if (value instanceof net.minecraft.world.level.material.Fluid fluid) {
            return ForgeRegistries.FLUIDS.getKey(fluid);
        }
        if (value instanceof FluidType fluidType) {
            return ForgeRegistries.FLUID_TYPES.get().getKey(fluidType);
        }
        return null;
    }

    private static void assertNoForbiddenIds(
            GameTestHelper helper,
            String registryName,
            Iterable<ResourceLocation> ids
    ) {
        for (ResourceLocation id : ids) {
            if (Magneticraft.MOD_ID.equals(id.getNamespace())) {
                helper.assertTrue(
                        !FORBIDDEN_LEGACY_IDS.contains(id.getPath()),
                        registryName + " still contains forbidden legacy ID " + id
                );
            }
        }
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
