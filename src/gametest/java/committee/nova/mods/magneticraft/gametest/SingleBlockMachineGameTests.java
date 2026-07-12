package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.singleblock.AirBubbleBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlockEntity;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * End-to-end contracts for the complete Task 5 machine set.
 */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SingleBlockMachineGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    private SingleBlockMachineGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void inventoryAndRecipeRegistrationsAreComplete(GameTestHelper helper) {
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            Block block = ModMachineBlocks.machine(definition).get();
            helper.assertTrue(
                    ForgeRegistries.BLOCKS.getKey(block).equals(Magneticraft.id(definition.id())),
                    "Missing machine registration: " + definition.id()
            );
        }
        helper.assertTrue(
                helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.SLUICE_TYPE.get()).size() == 16,
                "Sluice recipe inventory is incomplete"
        );
        helper.assertTrue(
                helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.GASIFICATION_TYPE.get()).size() == 19,
                "Gasification recipe inventory is incomplete"
        );
        helper.assertTrue(
                helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.THERMOPILE_TYPE.get()).size() == 33,
                "Thermopile recipe inventory is incomplete"
        );
        helper.assertTrue(
                helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.FLUID_FUEL_TYPE.get()).size() == 10,
                "Fluid-fuel recipe inventory is incomplete"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void customRecipeSerializersLoadRepresentativeRuntimeRecipes(GameTestHelper helper) {
        var recipeManager = helper.getLevel().getRecipeManager();
        var sluiceRecipe = recipeManager.byKey(Magneticraft.id("sluice/gravel")).orElse(null);
        helper.assertTrue(sluiceRecipe instanceof SluiceRecipe, "Sluice serializer did not load its runtime recipe");
        helper.assertTrue(
                ((SluiceRecipe) sluiceRecipe).matches(new SimpleContainer(new ItemStack(Items.GRAVEL)), helper.getLevel()),
                "Sluice runtime recipe did not match gravel"
        );
        helper.assertFalse(((SluiceRecipe) sluiceRecipe).outputs().isEmpty(), "Sluice runtime recipe lost its outputs");

        var gasificationRecipe = recipeManager.byKey(Magneticraft.id("gasification/00_logs")).orElse(null);
        helper.assertTrue(
                gasificationRecipe instanceof GasificationRecipe,
                "Gasification serializer did not load its runtime recipe"
        );
        helper.assertTrue(
                ((GasificationRecipe) gasificationRecipe).matches(
                        new SimpleContainer(new ItemStack(Items.OAK_LOG)),
                        helper.getLevel()
                ),
                "Gasification runtime recipe did not match a log"
        );
        helper.assertTrue(
                ((GasificationRecipe) gasificationRecipe).itemOutput().is(Items.CHARCOAL),
                "Gasification runtime recipe lost its charcoal output"
        );

        var thermopileRecipe = recipeManager.byKey(Magneticraft.id("thermopile/magma_block")).orElse(null);
        helper.assertTrue(
                thermopileRecipe instanceof ThermopileRecipe,
                "Thermopile serializer did not load its runtime recipe"
        );
        helper.assertTrue(
                ((ThermopileRecipe) thermopileRecipe).matches(Blocks.MAGMA_BLOCK.defaultBlockState()),
                "Thermopile runtime recipe did not match magma"
        );

        var fuelRecipe = recipeManager.byKey(Magneticraft.id("fluid_fuel/diesel")).orElse(null);
        helper.assertTrue(fuelRecipe instanceof FluidFuelRecipe, "Fluid-fuel serializer did not load its runtime recipe");
        helper.assertTrue(
                ((FluidFuelRecipe) fuelRecipe).fluid() == ModFluids.get(FluidDefinition.DIESEL).source().get(),
                "Fluid-fuel runtime recipe resolved the wrong fluid"
        );
        helper.assertTrue(
                ((FluidFuelRecipe) fuelRecipe).totalEnergyPerMilliBucket() > 0.0D,
                "Fluid-fuel runtime recipe lost its energy value"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pairedMachinePlacementCreatesOneAuthoritativeBlockEntity(GameTestHelper helper) {
        SingleBlockMachineBlock block = (SingleBlockMachineBlock) ModMachineBlocks
                .machine(SingleBlockMachineDefinition.SLUICE_BOX).get();
        BlockState primary = block.defaultBlockState()
                .setValue(SingleBlockMachineBlock.FACING, Direction.EAST)
                .setValue(SingleBlockMachineBlock.MASTER, true);
        helper.setBlock(CENTER, primary);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(CENTER), primary, null, ItemStack.EMPTY);

        BlockState secondary = helper.getBlockState(CENTER.east());
        helper.assertTrue(secondary.is(block), "Sluice secondary half was not placed");
        helper.assertFalse(secondary.getValue(SingleBlockMachineBlock.MASTER), "Sluice secondary half became authoritative");
        helper.assertTrue(helper.getBlockEntity(CENTER) instanceof SingleBlockMachineBlockEntity, "Primary half lost block entity");
        helper.assertTrue(helper.getBlockEntity(CENTER.east()) == null, "Secondary half created duplicate block entity");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void waterGeneratorExportsExactlyTwentyMilliBucketsPerTick(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.WATER_GENERATOR, Direction.NORTH));
        helper.setBlock(CENTER.east(), machineState(SingleBlockMachineDefinition.SMALL_TANK, Direction.NORTH));
        SingleBlockMachineBlockEntity tank = requireMachine(helper, CENTER.east());
        helper.runAfterDelay(5, () -> {
            int amount = tank.primaryTank().tank().getFluidAmount();
            helper.assertTrue(amount >= 80 && amount <= 120, "Water generator violated its 20 mB/t transfer rate: " + amount);
            helper.assertTrue(tank.primaryTank().tank().getFluid().getFluid() == Fluids.WATER, "Water generator exported another fluid");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void smallTankFluidSurvivesItemAndBlockEntityRoundTrip(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.SMALL_TANK, Direction.NORTH));
        SingleBlockMachineBlockEntity tank = requireMachine(helper, CENTER);
        tank.primaryTank().tank().fill(new FluidStack(Fluids.WATER, 12_345), IFluidHandler.FluidAction.EXECUTE);

        ItemStack droppedTank = new ItemStack(ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
        tank.saveTankToItem(droppedTank);
        CompoundTag blockEntityTag = droppedTank.getTagElement("BlockEntityTag");
        helper.assertTrue(blockEntityTag != null, "Small tank item lost its block-entity data");

        SingleBlockMachineBlockEntity restored = new SingleBlockMachineBlockEntity(
                tank.getBlockPos(),
                tank.getBlockState()
        );
        restored.load(blockEntityTag);
        helper.assertTrue(restored.primaryTank().tank().getFluidAmount() == 12_345, "Small tank fluid amount did not round-trip");
        helper.assertTrue(
                restored.primaryTank().tank().getFluid().getFluid() == Fluids.WATER,
                "Small tank fluid type did not round-trip"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void relayAndFilterUseTransactionalPneumaticEndpoints(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.RELAY, Direction.EAST));
        helper.setBlock(CENTER.east(), ModNetworkBlocks.PNEUMATIC_TUBE.get());
        BlockPos filterPosition = CENTER.above();
        helper.setBlock(filterPosition, machineState(SingleBlockMachineDefinition.FILTER, Direction.EAST));
        SingleBlockMachineBlockEntity relay = requireMachine(helper, CENTER);
        relay.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
        helper.runAfterDelay(10, () -> {
            PneumaticTubeBlockEntity tube = requireBlockEntity(helper, CENTER.east(), PneumaticTubeBlockEntity.class);
            helper.assertTrue(relay.inventory().getStackInSlot(0).isEmpty(), "Relay did not empty its selected stack");
            helper.assertFalse(tube.logistics().itemsSnapshot().isEmpty(), "Relay did not create a pneumatic payload");

            SingleBlockMachineBlockEntity filter = requireMachine(helper, filterPosition);
            filter.filters().setFilter(0, new ItemStack(Items.IRON_INGOT));
            Direction outputSide = filter.getBlockState().getValue(SingleBlockMachineBlock.FACING);
            IItemHandler input = filter.getCapability(ForgeCapabilities.ITEM_HANDLER, outputSide.getOpposite())
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(
                    input.insertItem(0, new ItemStack(Items.DIAMOND), true).getCount() == 1,
                    "Filter accepted a nonmatching stack"
            );
            helper.assertTrue(
                    input.insertItem(0, new ItemStack(Items.IRON_INGOT), true).isEmpty(),
                    "Filter rejected a matching stack"
            );
            helper.assertFalse(
                    filter.getCapability(ForgeCapabilities.ITEM_HANDLER, outputSide).isPresent(),
                    "Filter exposed input capability on its output face"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 45)
    public static void inserterConservesItemsAcrossTwoInventories(GameTestHelper helper) {
        helper.setBlock(CENTER.east(), Blocks.CHEST);
        helper.setBlock(CENTER.west(), Blocks.CHEST);
        ChestBlockEntity source = requireBlockEntity(helper, CENTER.east(), ChestBlockEntity.class);
        ChestBlockEntity target = requireBlockEntity(helper, CENTER.west(), ChestBlockEntity.class);
        source.setItem(0, new ItemStack(Items.COPPER_INGOT, 8));
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.INSERTER, Direction.EAST));
        SingleBlockMachineBlockEntity inserter = requireMachine(helper, CENTER);

        helper.runAfterDelay(25, () -> {
            int sourceCount = source.getItem(0).getCount();
            int targetCount = target.getItem(0).getCount();
            int carried = inserter.inventory().getStackInSlot(0).getCount();
            helper.assertTrue(sourceCount + targetCount + carried == 8, "Inserter duplicated or lost items");
            helper.assertTrue(targetCount == 8, "Inserter did not deliver its legacy default stack size");

            CompoundTag saved = inserter.saveWithoutMetadata();
            SingleBlockMachineBlockEntity restored = new SingleBlockMachineBlockEntity(
                    inserter.getBlockPos(),
                    inserter.getBlockState()
            );
            restored.load(saved);
            helper.assertTrue(restored.inserterFlags() == inserter.inserterFlags(), "Inserter flags did not survive reload");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void fabricatorConsumesReservedIngredientsAtomically(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FABRICATOR, Direction.NORTH));
        SingleBlockMachineBlockEntity fabricator = requireMachine(helper, CENTER);
        fabricator.filters().setFilter(1, new ItemStack(Items.OAK_PLANKS));
        fabricator.filters().setFilter(4, new ItemStack(Items.OAK_PLANKS));
        fabricator.inventory().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS));

        helper.assertTrue(fabricator.fabricatorResult().is(Items.STICK), "Fabricator did not resolve the ghost recipe");
        helper.assertFalse(fabricator.canCraftFabricator(), "Fabricator reserved one ingredient twice");
        helper.assertFalse(fabricator.craftFabricator(), "Fabricator consumed an incomplete reservation");
        helper.assertTrue(
                fabricator.inventory().getStackInSlot(0).getCount() == 1,
                "Failed fabricator transaction mutated its ingredient"
        );
        fabricator.inventory().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 2));
        helper.assertTrue(fabricator.canCraftFabricator(), "Fabricator failed to reserve matching ingredients");
        helper.assertTrue(fabricator.craftFabricator(), "Fabricator rejected a valid atomic craft");
        int sticks = 0;
        int planks = 0;
        for (int slot = 0; slot < fabricator.inventory().slots(); slot++) {
            ItemStack stack = fabricator.inventory().getStackInSlot(slot);
            if (stack.is(Items.STICK)) {
                sticks += stack.getCount();
            } else if (stack.is(Items.OAK_PLANKS)) {
                planks += stack.getCount();
            }
        }
        helper.assertTrue(sticks == 4 && planks == 0, "Fabricator output or ingredient count is wrong");

        Player player = helper.makeMockSurvivalPlayer();
        SingleBlockMachineMenu menu = new SingleBlockMachineMenu(1, player.getInventory(), fabricator);
        helper.assertTrue(menu.ghostFilterCount() == 9, "Fabricator menu lost its ghost grid");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 150)
    public static void heatMachinesPauseSafelyAndProduceExpectedOutputs(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.STEAM_BOILER, Direction.NORTH));
        SingleBlockMachineBlockEntity boiler = requireMachine(helper, CENTER);
        boiler.heat().node().setTemperature(500.0D);
        boiler.primaryTank().tank().fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE);

        helper.runAfterDelay(4, () -> {
            helper.assertTrue(boiler.secondaryTank().tank().getFluidAmount() == 80, "Steam boiler broke the 1:10 conversion ratio");
            helper.assertTrue(
                    boiler.secondaryTank().tank().getFluid().getFluid()
                            == ModFluids.get(FluidDefinition.STEAM).source().get(),
                    "Steam boiler produced another fluid"
            );

            helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.GASIFICATION_UNIT, Direction.NORTH));
            SingleBlockMachineBlockEntity gasifier = requireMachine(helper, CENTER);
            gasifier.heat().node().setTemperature(600.0D);
            gasifier.inventory().setStackInSlot(0, new ItemStack(Items.OAK_LOG));
        });
        helper.runAfterDelay(40, () -> {
            SingleBlockMachineBlockEntity gasifier = requireMachine(helper, CENTER);
            helper.assertTrue(gasifier.inventory().getStackInSlot(1).is(Items.CHARCOAL), "Gasifier lost its charcoal byproduct");
            helper.assertTrue(gasifier.primaryTank().tank().getFluidAmount() == 150, "Gasifier produced the wrong wood-gas amount");

            helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.BRICK_FURNACE, Direction.NORTH));
            SingleBlockMachineBlockEntity furnace = requireMachine(helper, CENTER);
            furnace.heat().node().setTemperature(600.0D);
            furnace.inventory().setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        });
        helper.runAfterDelay(145, () -> {
            SingleBlockMachineBlockEntity furnace = requireMachine(helper, CENTER);
            helper.assertTrue(furnace.inventory().getStackInSlot(0).isEmpty(), "Brick furnace did not consume input");
            helper.assertTrue(furnace.inventory().getStackInSlot(1).is(Items.IRON_INGOT), "Brick furnace output is wrong");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 45)
    public static void brickFurnacePausesOnFullOutputAndPersistsProgress(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.BRICK_FURNACE, Direction.NORTH));
        SingleBlockMachineBlockEntity furnace = requireMachine(helper, CENTER);
        furnace.heat().node().setTemperature(600.0D);
        furnace.inventory().setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        int[] pausedProgress = new int[1];

        helper.runAfterDelay(8, () -> {
            helper.assertTrue(furnace.progress() > 0, "Brick furnace did not start before the output became full");
            furnace.inventory().setStackInSlot(1, new ItemStack(Items.DIRT, 64));
            pausedProgress[0] = furnace.progress();
        });
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(furnace.progress() == pausedProgress[0], "Full output did not pause brick-furnace progress");
            CompoundTag saved = furnace.saveWithoutMetadata();
            SingleBlockMachineBlockEntity restored = new SingleBlockMachineBlockEntity(
                    furnace.getBlockPos(),
                    furnace.getBlockState()
            );
            restored.load(saved);
            helper.assertTrue(restored.progress() == pausedProgress[0], "Brick-furnace progress did not survive reload");
            furnace.inventory().setStackInSlot(1, ItemStack.EMPTY);
        });
        helper.runAfterDelay(28, () -> {
            helper.assertTrue(furnace.progress() > pausedProgress[0], "Brick furnace did not resume after output cleared");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void heatersAndConvertersExposeOnlyTheirIntendedEnergyBoundary(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.RF_HEATER, Direction.NORTH));
        SingleBlockMachineBlockEntity heater = requireMachine(helper, CENTER);
        double before = heater.heat().node().temperatureKelvin();
        heater.energy().setEnergyStored(160);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(heater.heat().node().temperatureKelvin() > before, "FE heater did not convert energy to heat");
            helper.assertTrue(heater.energy().getEnergyStored() == 0, "FE heater consumed the wrong amount");

            helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.RF_TRANSFORMER, Direction.NORTH));
            SingleBlockMachineBlockEntity transformer = requireMachine(helper, CENTER);
            var transformerEnergy = transformer.getCapability(ForgeCapabilities.ENERGY, Direction.UP)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(transformerEnergy.canReceive(), "FE transformer cannot receive FE");
            helper.assertFalse(transformerEnergy.canExtract(), "FE transformer leaks FE externally");

            helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.ELECTRIC_ENGINE, Direction.NORTH));
            SingleBlockMachineBlockEntity engine = requireMachine(helper, CENTER);
            helper.assertTrue(
                    engine.getCapability(ForgeCapabilities.ENERGY, Direction.SOUTH).isPresent(),
                    "Electric engine lost its output face"
            );
            helper.assertFalse(
                    engine.getCapability(ForgeCapabilities.ENERGY, Direction.NORTH).isPresent(),
                    "Electric engine exposed FE on its input face"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 125)
    public static void airlockBubblesDecayAfterPowerLoss(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.AIRLOCK, Direction.NORTH));
        helper.setBlock(CENTER.east(), Blocks.WATER);
        SingleBlockMachineBlockEntity airlock = requireMachine(helper, CENTER);
        airlock.electricity().node().setVoltage(125.0D);

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(helper.getBlockState(CENTER.east()).is(ModMachineBlocks.AIR_BUBBLE.get()), "Powered airlock did not replace water");
            airlock.electricity().node().setVoltage(0.0D);
        });
        helper.runAfterDelay(100, () -> {
            helper.assertFalse(helper.getBlockState(CENTER.east()).is(ModMachineBlocks.AIR_BUBBLE.get()), "Unpowered air bubble did not decay");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 35)
    public static void thermopileConvertsOpposingHeatSourcesWithoutFreeEnergy(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.THERMOPILE, Direction.NORTH));
        helper.setBlock(CENTER.east(), Blocks.ICE);
        helper.setBlock(CENTER.west(), Blocks.MAGMA_BLOCK);
        SingleBlockMachineBlockEntity thermopile = requireMachine(helper, CENTER);

        helper.runAfterDelay(25, () -> {
            helper.assertTrue(thermopile.menuData().get(11) > 0, "Thermopile did not calculate heat flux");
            helper.assertTrue(thermopile.electricity().node().energyJoules() > 0.0D, "Thermopile did not generate electricity");
            helper.succeed();
        });
    }

    private static BlockState machineState(SingleBlockMachineDefinition definition, Direction facing) {
        return ModMachineBlocks.machine(definition).get().defaultBlockState()
                .setValue(SingleBlockMachineBlock.FACING, facing)
                .setValue(SingleBlockMachineBlock.MASTER, true);
    }

    private static SingleBlockMachineBlockEntity requireMachine(GameTestHelper helper, BlockPos position) {
        return requireBlockEntity(helper, position, SingleBlockMachineBlockEntity.class);
    }

    private static <T extends BlockEntity> T requireBlockEntity(
            GameTestHelper helper,
            BlockPos position,
            Class<T> type
    ) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        helper.assertTrue(type.isInstance(blockEntity), "Missing block entity " + type.getSimpleName());
        return type.cast(blockEntity);
    }
}
