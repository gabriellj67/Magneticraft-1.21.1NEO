package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.content.machine.singleblock.AirBubbleBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
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
import committee.nova.mods.magneticraft.network.SetGhostFilterMessage;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
                helper.getLevel().getRecipeManager().getAllRecipesFor(ModRecipeTypes.GASIFICATION_TYPE.get()).size() == 28,
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
    public static void boxExposesAndPersistsItsTwentySevenStorageSlots(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.BOX, Direction.NORTH));
        SingleBlockMachineBlockEntity box = requireMachine(helper, CENTER);
        helper.assertTrue(box.inventory().slots() == 27, "Wooden box does not have 27 slots");
        for (Direction direction : Direction.values()) {
            IItemHandler handler = box.getCapability(ForgeCapabilities.ITEM_HANDLER, direction)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(handler.getSlots() == 27, "Wooden box side lost storage access: " + direction);
        }
        box.inventory().setStackInSlot(26, new ItemStack(Items.DIAMOND, 3));
        CompoundTag saved = box.saveWithoutMetadata();
        SingleBlockMachineBlockEntity restored = new SingleBlockMachineBlockEntity(
                box.getBlockPos(),
                box.getBlockState()
        );
        restored.load(saved);
        helper.assertTrue(
                restored.inventory().getStackInSlot(26).is(Items.DIAMOND)
                        && restored.inventory().getStackInSlot(26).getCount() == 3,
                "Wooden box did not persist its last slot"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void twoPlayersShareBoxWithoutDuplicatingItemsInOneTick(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.BOX, Direction.NORTH));
        SingleBlockMachineBlockEntity box = requireMachine(helper, CENTER);
        Player first = helper.makeMockSurvivalPlayer();
        Player second = helper.makeMockSurvivalPlayer();
        first.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 16));
        second.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 16));

        SingleBlockMachineMenu firstMenu = new SingleBlockMachineMenu(1, first.getInventory(), box);
        SingleBlockMachineMenu secondMenu = new SingleBlockMachineMenu(2, second.getInventory(), box);
        helper.assertTrue(firstMenu.quickMoveStack(first, 27).getCount() == 16,
                "First player could not insert its stack");
        helper.assertTrue(secondMenu.quickMoveStack(second, 27).getCount() == 16,
                "Second player could not insert its stack");
        helper.assertTrue(countIron(box) == 32, "Concurrent inserts duplicated or lost box items");
        helper.assertTrue(countIron(first) == 0 && countIron(second) == 0,
                "Concurrent inserts left a duplicate in a player inventory");

        ItemStack firstWithdrawal = firstMenu.quickMoveStack(first, 0);
        ItemStack secondWithdrawal = secondMenu.quickMoveStack(second, 0);
        helper.assertTrue(firstWithdrawal.getCount() == 32, "First withdrawal did not take the shared stack once");
        helper.assertTrue(secondWithdrawal.isEmpty(), "Second withdrawal duplicated the already removed stack");
        helper.assertTrue(countIron(box) + countIron(first) + countIron(second) == 32,
                "Two-player same-tick interaction violated item conservation");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void customRecipeSerializersLoadRepresentativeRuntimeRecipes(GameTestHelper helper) {
        var recipeManager = helper.getLevel().getRecipeManager();
        var sluiceRecipe = recipeManager.byKey(Magneticraft.id("sluice_box/gravel")).orElse(null);
        helper.assertTrue(sluiceRecipe instanceof SluiceRecipe, "Sluice serializer did not load its runtime recipe");
        helper.assertTrue(
                ((SluiceRecipe) sluiceRecipe).matches(new SimpleContainer(new ItemStack(Items.GRAVEL)), helper.getLevel()),
                "Sluice runtime recipe did not match gravel"
        );
        helper.assertFalse(((SluiceRecipe) sluiceRecipe).outputs().isEmpty(), "Sluice runtime recipe lost its outputs");

        var gasificationRecipe = recipeManager.byKey(Magneticraft.id("gasification_unit/00_logs")).orElse(null);
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
        helper.assertTrue(
                Math.abs(primary.getShape(helper.getLevel(), helper.absolutePos(CENTER)).bounds().maxY - 1.0D) < 1.0E-6D,
                "Sluice primary half lost its full-height shape"
        );
        helper.assertTrue(
                Math.abs(secondary.getShape(helper.getLevel(), helper.absolutePos(CENTER.east())).bounds().maxY - 0.5D) < 1.0E-6D,
                "Sluice secondary half is not half-height"
        );

        SingleBlockMachineBlock feedingTrough = (SingleBlockMachineBlock) ModMachineBlocks
                .machine(SingleBlockMachineDefinition.FEEDING_TROUGH).get();
        BlockState troughPrimary = feedingTrough.defaultBlockState()
                .setValue(SingleBlockMachineBlock.FACING, Direction.EAST)
                .setValue(SingleBlockMachineBlock.MASTER, true);
        BlockState troughSecondary = troughPrimary
                .setValue(SingleBlockMachineBlock.FACING, Direction.WEST)
                .setValue(SingleBlockMachineBlock.MASTER, false);
        helper.assertTrue(
                Math.abs(troughPrimary.getShape(helper.getLevel(), helper.absolutePos(CENTER)).bounds().maxY - 0.75D) < 1.0E-6D,
                "Feeding-trough primary half is not twelve pixels high"
        );
        helper.assertTrue(
                Math.abs(troughSecondary.getShape(helper.getLevel(), helper.absolutePos(CENTER.east())).bounds().maxY - 0.75D) < 1.0E-6D,
                "Feeding-trough secondary half is not twelve pixels high"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void waterGeneratorExportsTwentyMilliBucketsPerSidePerTick(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.WATER_GENERATOR, Direction.NORTH));
        SingleBlockMachineBlockEntity generator = requireMachine(helper, CENTER);
        SingleBlockMachineBlockEntity[] tanks = new SingleBlockMachineBlockEntity[Direction.values().length];
        for (Direction direction : Direction.values()) {
            BlockPos tankPosition = CENTER.relative(direction);
            helper.setBlock(tankPosition, machineState(SingleBlockMachineDefinition.SMALL_TANK, Direction.NORTH));
            tanks[direction.ordinal()] = requireMachine(helper, tankPosition);
        }
        helper.runAfterDelay(5, () -> {
            int rate = MagneticraftConfig.WATER_GENERATOR_PER_TICK_WATER.get();
            helper.assertTrue(rate == 20, "Water-generator default rate changed from 20 mB/t: " + rate);
            int total = 0;
            for (SingleBlockMachineBlockEntity tank : tanks) {
                int amount = tank.primaryTank().tank().getFluidAmount();
                helper.assertTrue(
                        amount >= rate * 4 && amount <= rate * 6,
                        "Water generator did not export its rate independently to one side: " + amount
                );
                helper.assertTrue(
                        tank.primaryTank().tank().getFluid().getFluid() == Fluids.WATER,
                        "Water generator exported another fluid"
                );
                total += amount;
            }
            helper.assertTrue(
                    total >= rate * 4 * Direction.values().length
                            && total <= rate * 6 * Direction.values().length,
                    "Six accepting sides did not receive 120 mB/t in total: " + total
            );
            FluidStack first = generator.primaryTank().tank().drain(32_000, IFluidHandler.FluidAction.EXECUTE);
            FluidStack second = generator.primaryTank().tank().drain(32_000, IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(first.getAmount() == 32_000 && second.getAmount() == 32_000,
                    "Water generator source did not refill in the same tick");
            helper.assertTrue(generator.primaryTank().tank().getFluidAmount() == 32_000,
                    "Water generator source tank was depleted");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void activeSluiceIsResetByUpstreamRunoff(GameTestHelper helper) {
        BlockPos upstreamPosition = new BlockPos(0, 2, 1);
        BlockPos downstreamPosition = new BlockPos(2, 1, 1);
        helper.setBlock(upstreamPosition, machineState(SingleBlockMachineDefinition.SLUICE_BOX, Direction.EAST));
        helper.setBlock(downstreamPosition, machineState(SingleBlockMachineDefinition.SLUICE_BOX, Direction.EAST));
        SingleBlockMachineBlockEntity upstream = requireMachine(helper, upstreamPosition);
        SingleBlockMachineBlockEntity downstream = requireMachine(helper, downstreamPosition);

        CompoundTag downstreamState = downstream.saveWithoutMetadata();
        downstreamState.putInt("progress", 20);
        downstreamState.putInt("total_progress", 80);
        downstream.load(downstreamState);
        CompoundTag upstreamState = upstream.saveWithoutMetadata();
        upstreamState.putInt("progress", 80);
        upstreamState.putInt("total_progress", 80);
        upstreamState.putInt("chain_delay", 1);
        upstream.load(upstreamState);

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(downstream.progress() > 20, "Active downstream sluice was not reset by runoff");
            helper.assertTrue(
                    downstream.saveWithoutMetadata().getInt("chain_delay") > 0,
                    "Reset downstream sluice did not continue the runoff chain"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void feedingTroughConsumesInvalidInteractionAndReturnsFoodToClickedHand(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FEEDING_TROUGH, Direction.EAST));
        SingleBlockMachineBlockEntity trough = requireMachine(helper, CENTER);
        Player player = helper.makeMockSurvivalPlayer();
        BlockPos absolute = helper.absolutePos(CENTER);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);

        trough.inventory().setStackInSlot(0, new ItemStack(Items.WHEAT, 3));
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        InteractionResult extraction = trough.interact(player, InteractionHand.OFF_HAND, hit);
        helper.assertTrue(extraction.consumesAction(), "Empty-hand trough extraction did not consume the interaction");
        helper.assertTrue(
                player.getItemInHand(InteractionHand.OFF_HAND).is(Items.WHEAT)
                        && player.getItemInHand(InteractionHand.OFF_HAND).getCount() == 3,
                "Trough extraction did not return food to the clicked hand"
        );

        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.POTATO));
        InteractionResult invalid = trough.interact(player, InteractionHand.OFF_HAND, hit);
        helper.assertTrue(invalid.consumesAction(), "Unsupported trough food did not consume the interaction");
        helper.assertTrue(trough.inventory().getStackInSlot(0).isEmpty(), "Unsupported trough food was inserted");
        helper.assertTrue(player.getItemInHand(InteractionHand.OFF_HAND).is(Items.POTATO), "Unsupported food was consumed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void combustionChamberDoorAloneControlsDirectFuelInteraction(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.COMBUSTION_CHAMBER, Direction.SOUTH));
        SingleBlockMachineBlockEntity chamber = requireMachine(helper, CENTER);
        Player player = helper.makeMockSurvivalPlayer();
        BlockPos absolute = helper.absolutePos(CENTER);

        InteractionResult body = chamber.interact(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );
        helper.assertTrue(body == InteractionResult.PASS, "Combustion-chamber body did not defer to its menu");
        helper.assertTrue(Int32ContainerData.read(chamber.menuData(), 9) == 0,
                "Combustion-chamber body toggled the door");

        Vec3 doorCenter = new Vec3(absolute.getX() + 0.5D, absolute.getY() + 0.375D, absolute.getZ() + 1.0D);
        BlockHitResult door = new BlockHitResult(doorCenter, Direction.SOUTH, absolute, false);
        InteractionResult opened = chamber.interact(player, InteractionHand.MAIN_HAND, door);
        helper.assertTrue(opened.consumesAction(), "Combustion-chamber door did not consume the interaction");
        helper.assertTrue(Int32ContainerData.read(chamber.menuData(), 9) == 1,
                "Combustion-chamber door did not open");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COAL, 2));
        chamber.interact(player, InteractionHand.MAIN_HAND, door);
        helper.assertTrue(chamber.inventory().getStackInSlot(0).is(Items.COAL),
                "Open combustion-chamber door did not accept fuel");
        helper.assertTrue(Int32ContainerData.read(chamber.menuData(), 9) == 1,
                "Fuel insertion unexpectedly closed the combustion-chamber door");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 430)
    public static void feedingTroughUsesPositionPhaseAndLegacyOneItemBoundary(GameTestHelper helper) {
        helper.killAllEntitiesOfClass(Animal.class);
        SingleBlockMachineBlock block = (SingleBlockMachineBlock) ModMachineBlocks
                .machine(SingleBlockMachineDefinition.FEEDING_TROUGH).get();
        BlockState primary = machineState(SingleBlockMachineDefinition.FEEDING_TROUGH, Direction.EAST);
        helper.setBlock(CENTER, primary);
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(CENTER), primary, null, ItemStack.EMPTY);
        SingleBlockMachineBlockEntity trough = requireMachine(helper, CENTER);
        trough.inventory().setStackInSlot(0, new ItemStack(Items.WHEAT));
        Cow cow = spawnAnimal(helper, EntityType.COW, CENTER.north());
        Sheep sheep = spawnAnimal(helper, EntityType.SHEEP, CENTER.south());

        BlockPos absolute = helper.absolutePos(CENTER);
        long phase = Math.floorMod(helper.getLevel().getGameTime() + absolute.hashCode(), 400L);
        int delay = (int) ((400L - phase) % 400L);
        if (delay == 0) {
            delay = 400;
        }
        helper.runAfterDelay(delay + 2, () -> {
            helper.assertTrue(cow.isInLove(), "Feeding trough did not feed the only eligible cow");
            helper.assertTrue(sheep.isInLove(), "Feeding trough did not feed the only eligible sheep");
            helper.assertTrue(trough.inventory().getStackInSlot(0).isEmpty(), "Feeding trough did not consume its one food item");
            cow.discard();
            sheep.discard();
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

    @GameTest(template = TEMPLATE)
    public static void incompatibleRootSchemaResetsSingleBlockModulesAndLocalState(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FABRICATOR, Direction.NORTH));
        SingleBlockMachineBlockEntity fabricator = requireMachine(helper, CENTER);
        fabricator.inventory().setStackInSlot(0, new ItemStack(Items.OAK_PLANKS, 8));
        fabricator.filters().setFilter(0, new ItemStack(Items.OAK_PLANKS));
        CompoundTag futureFabricator = fabricator.saveWithoutMetadata();
        futureFabricator.putInt("schema_version", Integer.MAX_VALUE);
        fabricator.load(futureFabricator);
        helper.assertTrue(fabricator.inventory().getStackInSlot(0).isEmpty(), "Future schema retained fabricator inventory");
        helper.assertTrue(fabricator.filters().getFilter(0).isEmpty(), "Future schema retained fabricator filter configuration");
        helper.assertTrue(fabricator.progress() == 0, "Future schema retained single-block progress");
        helper.assertTrue(fabricator.totalProgress() == 0, "Future schema retained single-block recipe duration");
        helper.assertFalse(fabricator.working(), "Future schema retained single-block working state");

        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.ELECTRIC_HEATER, Direction.NORTH));
        SingleBlockMachineBlockEntity heater = requireMachine(helper, CENTER);
        heater.energy().setStoredJoules(4_000);
        heater.electricity().node().setEnergyJoules(321.0D);
        heater.heat().node().setTemperature(650.0D);
        CompoundTag missingHeaterSchema = heater.saveWithoutMetadata();
        missingHeaterSchema.remove("schema_version");
        heater.load(missingHeaterSchema);
        helper.assertTrue(heater.energy().storedWholeJoules() == 0, "Missing schema retained heater native joules");
        helper.assertTrue(
                heater.electricity().node().energyJoules() == 0.0D,
                "Missing schema retained heater electrical energy"
        );
        helper.assertTrue(
                Math.abs(heater.heat().node().temperatureKelvin() - HeatNode.AMBIENT_TEMPERATURE_KELVIN) < 1.0E-6D,
                "Missing schema did not restore ambient heater temperature"
        );

        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.SMALL_TANK, Direction.NORTH));
        SingleBlockMachineBlockEntity tank = requireMachine(helper, CENTER);
        tank.primaryTank().tank().fill(new FluidStack(Fluids.WATER, 12_345), IFluidHandler.FluidAction.EXECUTE);
        tank.claimForMultiblock(helper.absolutePos(CENTER.above()));
        CompoundTag futureTank = tank.saveWithoutMetadata();
        futureTank.putInt("schema_version", Integer.MAX_VALUE);
        tank.load(futureTank);
        helper.assertTrue(tank.primaryTank().tank().isEmpty(), "Future schema retained small-tank fluid");
        helper.assertFalse(tank.claimedByMultiblock(), "Future schema retained a stale multiblock claim");

        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.INSERTER, Direction.NORTH));
        SingleBlockMachineBlockEntity inserter = requireMachine(helper, CENTER);
        inserter.toggleInserterFlag(0);
        helper.assertTrue(inserter.inserterFlags() != 12, "Inserter fixture did not change its configuration");
        CompoundTag missingInserterSchema = inserter.saveWithoutMetadata();
        missingInserterSchema.remove("schema_version");
        inserter.load(missingInserterSchema);
        helper.assertTrue(inserter.inserterFlags() == 12, "Missing schema retained inserter configuration");
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
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(
                fabricator.getBlockPos().getX() + 0.5D,
                fabricator.getBlockPos().getY() + 0.5D,
                fabricator.getBlockPos().getZ() + 0.5D
        );
        SingleBlockMachineMenu menu = new SingleBlockMachineMenu(1, player.getInventory(), fabricator);
        player.containerMenu = menu;
        int resultSlot = menu.ghostFilterCount();
        int energyBeforeRejectedCraft = menu.energyStored();
        int progressBeforeRejectedCraft = menu.progress();
        int consumptionBeforeRejectedCraft = menu.lastConsumption();
        boolean workingBeforeRejectedCraft = menu.working();
        player.getAbilities().mayBuild = false;
        menu.clicked(resultSlot, 0, ClickType.PICKUP, player);
        helper.assertTrue(
                fabricator.inventory().getStackInSlot(0).is(Items.OAK_PLANKS)
                        && fabricator.inventory().getStackInSlot(0).getCount() == 2,
                "Player without build permission consumed fabricator ingredients"
        );
        helper.assertTrue(fabricator.canCraftFabricator(), "Permission-rejected fabricator craft changed its recipe inputs");
        helper.assertTrue(
                menu.energyStored() == energyBeforeRejectedCraft
                        && menu.progress() == progressBeforeRejectedCraft
                        && menu.lastConsumption() == consumptionBeforeRejectedCraft
                        && menu.working() == workingBeforeRejectedCraft,
                "Permission-rejected fabricator craft changed machine processing state"
        );

        player.getAbilities().mayBuild = true;
        menu.clicked(resultSlot, 0, ClickType.PICKUP, player);
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

        helper.assertTrue(menu.ghostFilterCount() == 9, "Fabricator menu lost its ghost grid");
        player.getAbilities().mayBuild = false;
        menu.clicked(resultSlot, 1, ClickType.PICKUP, player);
        helper.assertTrue(
                fabricator.filters().getFilter(1).is(Items.OAK_PLANKS),
                "Player without build permission cleared the fabricator recipe"
        );
        player.getAbilities().mayBuild = true;
        menu.clicked(resultSlot, 1, ClickType.PICKUP, player);
        for (int slot = 0; slot < fabricator.filters().size(); slot++) {
            helper.assertTrue(fabricator.filters().getFilter(slot).isEmpty(), "Right-click did not clear fabricator ghost grid");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void inserterConfigurationRequiresBuildPermission(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.INSERTER, Direction.NORTH));
        SingleBlockMachineBlockEntity inserter = requireMachine(helper, CENTER);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(
                inserter.getBlockPos().getX() + 0.5D,
                inserter.getBlockPos().getY() + 0.5D,
                inserter.getBlockPos().getZ() + 0.5D
        );
        SingleBlockMachineMenu menu = new SingleBlockMachineMenu(2, player.getInventory(), inserter);
        player.containerMenu = menu;
        int initialFlags = inserter.inserterFlags();

        player.getAbilities().mayBuild = false;
        helper.assertFalse(menu.clickMenuButton(player, 0), "Permission-rejected inserter button reported success");
        helper.assertTrue(inserter.inserterFlags() == initialFlags, "Permission-rejected inserter button changed state");

        player.getAbilities().mayBuild = true;
        helper.assertTrue(menu.clickMenuButton(player, 0), "Valid inserter button was rejected");
        helper.assertTrue(inserter.inserterFlags() != initialFlags, "Valid inserter button did not change state");
        helper.assertFalse(menu.clickMenuButton(player, 6), "Out-of-range inserter button was accepted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ghostFilterPacketRejectsStaleDistantAndUnloadedRealMenus(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FABRICATOR, Direction.NORTH));
        SingleBlockMachineBlockEntity staleMachine = requireMachine(helper, CENTER);
        staleMachine.filters().setFilter(0, new ItemStack(Items.IRON_INGOT));
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(
                staleMachine.getBlockPos().getX() + 0.5D,
                staleMachine.getBlockPos().getY() + 0.5D,
                staleMachine.getBlockPos().getZ() + 0.5D
        );
        SingleBlockMachineMenu staleMenu = new SingleBlockMachineMenu(3, player.getInventory(), staleMachine);
        player.containerMenu = staleMenu;
        helper.setBlock(CENTER, Blocks.AIR);
        helper.assertFalse(
                SetGhostFilterMessage.applyIfValid(
                        player,
                        new SetGhostFilterMessage(staleMachine.getBlockPos(), 0, new ItemStack(Items.GOLD_INGOT))
                ),
                "Stale single-block menu accepted a ghost-filter packet"
        );
        helper.assertTrue(
                staleMachine.filters().getFilter(0).is(Items.IRON_INGOT),
                "Stale-menu rejection changed the ghost filter"
        );

        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FABRICATOR, Direction.NORTH));
        SingleBlockMachineBlockEntity distantMachine = requireMachine(helper, CENTER);
        distantMachine.filters().setFilter(0, new ItemStack(Items.IRON_INGOT));
        SingleBlockMachineMenu distantMenu = new SingleBlockMachineMenu(4, player.getInventory(), distantMachine);
        player.containerMenu = distantMenu;
        player.setPos(
                distantMachine.getBlockPos().getX() + 9.5D,
                distantMachine.getBlockPos().getY() + 0.5D,
                distantMachine.getBlockPos().getZ() + 0.5D
        );
        helper.assertFalse(
                SetGhostFilterMessage.applyIfValid(
                        player,
                        new SetGhostFilterMessage(distantMachine.getBlockPos(), 0, new ItemStack(Items.GOLD_INGOT))
                ),
                "Distant player changed a real machine ghost filter"
        );
        helper.assertTrue(
                distantMachine.filters().getFilter(0).is(Items.IRON_INGOT),
                "Distance rejection changed the ghost filter"
        );

        BlockPos unloadedPosition = null;
        BlockPos origin = helper.absolutePos(CENTER);
        for (int offset = 32; offset <= 2_048; offset += 16) {
            BlockPos candidate = origin.offset(offset, 0, 0);
            if (helper.getLevel().getWorldBorder().isWithinBounds(candidate)
                    && !helper.getLevel().hasChunk(candidate.getX() >> 4, candidate.getZ() >> 4)) {
                unloadedPosition = candidate;
                break;
            }
        }
        helper.assertTrue(unloadedPosition != null, "Could not find an unloaded chunk for packet validation");
        SingleBlockMachineBlockEntity unloadedMachine = new SingleBlockMachineBlockEntity(
                unloadedPosition,
                machineState(SingleBlockMachineDefinition.FABRICATOR, Direction.NORTH)
        );
        unloadedMachine.filters().setFilter(0, new ItemStack(Items.IRON_INGOT));
        SingleBlockMachineMenu unloadedMenu = new SingleBlockMachineMenu(5, player.getInventory(), unloadedMachine);
        player.containerMenu = unloadedMenu;
        player.setPos(
                unloadedPosition.getX() + 0.5D,
                unloadedPosition.getY() + 0.5D,
                unloadedPosition.getZ() + 0.5D
        );
        helper.assertFalse(
                helper.getLevel().hasChunk(unloadedPosition.getX() >> 4, unloadedPosition.getZ() >> 4),
                "Unloaded packet target became loaded during menu setup"
        );
        helper.assertFalse(
                SetGhostFilterMessage.applyIfValid(
                        player,
                        new SetGhostFilterMessage(unloadedPosition, 0, new ItemStack(Items.GOLD_INGOT))
                ),
                "Unloaded machine position accepted a ghost-filter packet"
        );
        helper.assertTrue(
                unloadedMachine.filters().getFilter(0).is(Items.IRON_INGOT),
                "Unloaded-position rejection changed the ghost filter"
        );
        helper.assertFalse(
                helper.getLevel().hasChunk(unloadedPosition.getX() >> 4, unloadedPosition.getZ() >> 4),
                "Ghost-filter packet force-loaded its unloaded target"
        );
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
        heater.forgeEnergy().setEnergyStored(160);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(heater.heat().node().temperatureKelvin() > before, "FE heater did not convert energy to heat");
            helper.assertTrue(heater.forgeEnergy().getEnergyStored() == 0, "FE heater consumed the wrong amount");

            helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.RF_TRANSFORMER, Direction.NORTH));
            SingleBlockMachineBlockEntity transformer = requireMachine(helper, CENTER);
            var transformerEnergy = transformer.getCapability(ForgeCapabilities.ENERGY, Direction.UP)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(transformerEnergy.canReceive(), "FE transformer cannot receive FE");
            helper.assertFalse(transformerEnergy.canExtract(), "FE transformer leaks FE externally");

            helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.ELECTRIC_ENGINE, Direction.NORTH));
            SingleBlockMachineBlockEntity engine = requireMachine(helper, CENTER);
            helper.assertFalse(
                    engine.getCapability(ForgeCapabilities.ENERGY, null).isPresent(),
                    "Electric engine exposed an unsided FE capability"
            );
            for (Direction direction : Direction.values()) {
                helper.assertFalse(
                        engine.getCapability(ForgeCapabilities.ENERGY, direction).isPresent(),
                        "Electric engine exposed FE on " + direction
                );
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 65)
    public static void airlockClearsInteriorWaterWithoutCreatingBoundaryBubble(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.AIRLOCK, Direction.NORTH));
        helper.setBlock(CENTER.east(), Blocks.WATER);
        SingleBlockMachineBlockEntity airlock = requireMachine(helper, CENTER);
        airlock.electricity().node().setVoltage(125.0D);

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(
                    helper.getBlockState(CENTER.east()).isAir(),
                    "Airlock treated isolated interior water as a boundary bubble"
            );
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
            helper.assertTrue(
                    Int32ContainerData.read(thermopile.menuData(), 11) > 0,
                    "Thermopile did not calculate heat flux"
            );
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

    private static int countIron(SingleBlockMachineBlockEntity machine) {
        int count = 0;
        for (int slot = 0; slot < machine.inventory().slots(); slot++) {
            ItemStack stack = machine.inventory().getStackInSlot(slot);
            if (stack.is(Items.IRON_INGOT)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countIron(Player player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.IRON_INGOT)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static <T extends Animal> T spawnAnimal(
            GameTestHelper helper,
            EntityType<T> type,
            BlockPos relativePosition
    ) {
        T animal = type.create(helper.getLevel());
        helper.assertTrue(animal != null, "Unable to create test animal " + type);
        BlockPos absolute = helper.absolutePos(relativePosition);
        animal.moveTo(absolute.getX() + 0.5D, absolute.getY(), absolute.getZ() + 0.5D, 0.0F, 0.0F);
        animal.setNoAi(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(animal), "Unable to spawn test animal " + type);
        return animal;
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
