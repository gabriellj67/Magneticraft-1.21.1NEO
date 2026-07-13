package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlock;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockCell;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockRule;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockTransform;
import committee.nova.mods.magneticraft.content.multiblock.StructureOffset;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdvancedSystemsGameTests {
    private static final String TEMPLATE = "advanced_systems";
    private static final BlockPos CONTROLLER = new BlockPos(16, 2, 16);

    private AdvancedSystemsGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void everyLegacyDefinitionFormsAndPersists(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            List<BlockPos> occupied = build(helper, definition, CONTROLLER, Direction.NORTH, false);
            AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
            controller.setOwner(player.getUUID());

            helper.assertTrue(controller.tryForm(player), definition.id() + " did not form");
            helper.assertTrue(controller.formed(), definition.id() + " lost formed state");
            helper.assertTrue(controller.operational(), definition.id() + " was not operational after validation");

            CompoundTag saved = controller.saveWithoutMetadata();
            AdvancedMultiblockBlockEntity restored = new AdvancedMultiblockBlockEntity(
                    controller.getBlockPos(), controller.getBlockState()
            );
            restored.load(saved);
            helper.assertTrue(restored.formed(), definition.id() + " formed state did not persist");
            helper.assertTrue(player.getUUID().equals(restored.owner()), definition.id() + " owner did not persist");

            controller.unform();
            clear(helper, occupied);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void incompatibleControllerSchemaResetsAndRevalidatesFormation(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.GRINDER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Grinder did not form before schema reset");
        controller.setOwner(player.getUUID());
        controller.inventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        controller.energy().setEnergyStored(5_000);
        AdvancedMultiblockBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
        );
        helper.assertTrue(controller.working(), "Grinder did not enter working state before schema reset");
        helper.assertFalse(controller.inventory().getStackInSlot(0).isEmpty(), "Grinder lost its input before schema reset");
        helper.assertTrue(controller.energy().getEnergyStored() > 0, "Grinder lost all energy before schema reset");
        helper.assertTrue(controller.progress() > 0, "Grinder did not advance before schema reset");
        helper.assertTrue(controller.totalProgress() > 0, "Grinder did not retain its active recipe before schema reset");
        helper.assertTrue(player.getUUID().equals(controller.owner()), "Grinder did not retain its owner before schema reset");

        CompoundTag incompatible = controller.saveWithoutMetadata();
        incompatible.putInt("schema_version", 2);
        controller.load(incompatible);

        helper.assertTrue(controller.inventory().getStackInSlot(0).isEmpty(), "Schema reset retained grinder inventory");
        helper.assertTrue(controller.energy().getEnergyStored() == 0, "Schema reset retained grinder energy");
        helper.assertTrue(controller.progress() == 0, "Schema reset retained grinder progress");
        helper.assertTrue(controller.totalProgress() == 0, "Schema reset retained grinder recipe duration");
        helper.assertTrue(controller.owner() == null, "Schema reset retained grinder ownership");
        helper.assertTrue(controller.formed(), "Schema reset lost the controller block's formation claim");
        helper.assertFalse(controller.operational(), "Schema reset trusted formation without revalidation");
        helper.assertFalse(controller.working(), "Schema reset retained stale working state");
        AdvancedMultiblockBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
        );
        helper.assertTrue(controller.operational(), "Schema reset did not revalidate the intact structure");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void rotationsMirrorsAndChunkBoundaryUseTheSameDefinition(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        MultiblockDefinition definition = MultiblockDefinition.OIL_HEATER;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (boolean mirrored : List.of(false, true)) {
                List<BlockPos> occupied = build(helper, definition, CONTROLLER, facing, mirrored);
                AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
                if (mirrored) {
                    controller.toggleMirrored(player);
                }
                helper.assertTrue(controller.tryForm(player),
                        "oil_heater did not form facing=" + facing + ", mirrored=" + mirrored);
                controller.unform();
                clear(helper, occupied);
            }
        }

        List<BlockPos> normalPumpjack = build(
                helper, MultiblockDefinition.PUMPJACK, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity normalController = requireController(helper, CONTROLLER);
        normalController.toggleMirrored(player);
        helper.assertFalse(normalController.tryForm(player),
                "Non-mirrored pumpjack formed with mirrored validation");
        clear(helper, normalPumpjack);

        List<BlockPos> mirroredPumpjack = build(
                helper, MultiblockDefinition.PUMPJACK, CONTROLLER, Direction.NORTH, true
        );
        AdvancedMultiblockBlockEntity mirroredController = requireController(helper, CONTROLLER);
        mirroredController.toggleMirrored(player);
        helper.assertTrue(mirroredController.tryForm(player), "Mirrored pumpjack did not form");
        mirroredController.unform();
        clear(helper, mirroredPumpjack);

        BlockPos absoluteOrigin = helper.absolutePos(BlockPos.ZERO);
        int edgeX = ((absoluteOrigin.getX() >> 4) + 2) << 4;
        BlockPos boundaryController = new BlockPos(edgeX - absoluteOrigin.getX(), 2, 1);
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.SOLAR_PANEL, boundaryController, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity boundary = requireController(helper, boundaryController);
        helper.assertTrue(boundary.tryForm(player), "Cross-chunk solar panel did not form");
        boundary.unform();
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void mismatchAndMemberMutationCannotLeaveAnOperationalMachine(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        MultiblockDefinition mirror = MultiblockDefinition.SOLAR_MIRROR;
        List<BlockPos> mirrorBlocks = build(helper, mirror, CONTROLLER, Direction.NORTH, false);
        BlockPos requiredAir = mirror.cells().stream()
                .filter(cell -> cell.rule() == MultiblockRule.AIR)
                .map(cell -> transformedPosition(mirror, CONTROLLER, Direction.NORTH, false, cell))
                .findFirst()
                .orElseThrow();
        helper.setBlock(requiredAir, Blocks.STONE);
        helper.assertFalse(requireController(helper, CONTROLLER).tryForm(player),
                "Solar mirror accepted an obstruction in required air");
        clear(helper, mirrorBlocks);

        MultiblockDefinition definition = MultiblockDefinition.BIG_STEAM_BOILER;
        List<BlockPos> occupied = build(helper, definition, CONTROLLER, Direction.NORTH, false);
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        BlockPos member = firstReplaceableMember(definition, CONTROLLER, Direction.NORTH, false);

        helper.setBlock(member, Blocks.DIAMOND_BLOCK);
        helper.assertFalse(controller.tryForm(player), "Mismatched structure formed");
        helper.setBlock(member, stateFor(definition.cells().stream()
                .filter(cell -> transformedPosition(definition, CONTROLLER, Direction.NORTH, false, cell).equals(member))
                .findFirst()
                .orElseThrow()
                .rule()));
        helper.assertTrue(controller.tryForm(player), "Repaired structure did not form");

        helper.setBlock(member, Blocks.AIR);
        helper.runAfterDelay(25, () -> {
            helper.assertFalse(controller.formed(), "Broken member left controller formed");
            helper.assertFalse(controller.operational(), "Broken member left controller operational");
            clear(helper, occupied);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void destroyingFormedControllerCannotRestoreTheRemovedBlock(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.SOLAR_PANEL, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Solar panel did not form before destruction");

        helper.setBlock(CONTROLLER, Blocks.AIR);
        helper.assertTrue(helper.getBlockState(CONTROLLER).isAir(),
                "Formed controller restored itself during onRemove");
        helper.assertTrue(helper.getBlockEntity(CONTROLLER) == null,
                "Removed controller left a ghost block entity");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void formationLocksMemberTankAndInvalidatesCachedCapabilities(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        MultiblockDefinition definition = MultiblockDefinition.STEAM_TURBINE;
        List<BlockPos> occupied = build(helper, definition, CONTROLLER, Direction.NORTH, false);
        BlockPos tankPosition = definition.cells().stream()
                .filter(cell -> cell.rule() == MultiblockRule.SMALL_TANK)
                .map(cell -> transformedPosition(definition, CONTROLLER, Direction.NORTH, false, cell))
                .findFirst()
                .orElseThrow();
        SingleBlockMachineBlockEntity memberTank = (SingleBlockMachineBlockEntity) helper.getBlockEntity(tankPosition);
        var cachedMemberCapability = memberTank.getCapability(ForgeCapabilities.FLUID_HANDLER);
        IFluidHandler initialTank = cachedMemberCapability.orElseThrow(AssertionError::new);
        initialTank.fill(
                new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), 500),
                IFluidHandler.FluidAction.EXECUTE
        );

        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Steam turbine did not form");
        helper.assertTrue(memberTank.claimedByMultiblock(), "Visible turbine tank was not claimed");
        helper.assertFalse(cachedMemberCapability.isPresent(), "Cached member-tank capability stayed live");
        helper.assertFalse(memberTank.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent(),
                "Formed turbine exposed the independent member tank");

        ItemStack memberDrop = new ItemStack(
                ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get()
        );
        memberTank.saveTankToItem(memberDrop);
        helper.assertFalse(memberDrop.getTagElement("BlockEntityTag")
                        .contains("multiblock_controller"),
                "Portable small tank retained a stale multiblock claim");

        var cachedControllerCapability = controller.getCapability(ForgeCapabilities.FLUID_HANDLER);
        helper.assertTrue(cachedControllerCapability.isPresent(), "Formed turbine controller lost steam input");
        controller.unform();
        helper.assertFalse(cachedControllerCapability.isPresent(), "Cached controller capability survived unform");
        IFluidHandler restored = memberTank.getCapability(ForgeCapabilities.FLUID_HANDLER)
                .orElseThrow(AssertionError::new);
        helper.assertTrue(restored.getFluidInTank(0).getAmount() == 500,
                "Member tank contents changed while claimed");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void completeAdvancedRecipeCatalogLoadsAtRuntime(GameTestHelper helper) {
        int processingRecipes = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.ADVANCED_PROCESSING_TYPE.get()).size();
        int fluidFuels = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.FLUID_FUEL_TYPE.get()).size();
        helper.assertTrue(processingRecipes == 86,
                "Expected 86 advanced processing recipes, loaded " + processingRecipes);
        helper.assertTrue(fluidFuels == 10,
                "Expected 10 fluid fuel recipes, loaded " + fluidFuels);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void steamGeneratorUsesTenMillibucketOperations(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.STEAM_ENGINE, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Steam engine did not form");
        helper.assertTrue(controller.tank(0).tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), 10),
                IFluidHandler.FluidAction.EXECUTE
        ) == 10, "Steam engine rejected a 10 mB operation");

        AdvancedMultiblockBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
        );

        helper.assertTrue(controller.tank(0).tank().getFluidAmount() == 0,
                "Steam engine did not consume exactly 10 mB");
        helper.assertTrue(Math.abs(controller.electricity().node().energyJoules() - 20.0D) < 0.0001D,
                "Steam engine did not produce exactly 20 J");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void grinderRejectsInvalidInputRecoversInputAndCompletesRecipe(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.GRINDER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Grinder did not form");
        IItemHandler recoveryPort = controller.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.SOUTH)
                .orElseThrow(AssertionError::new);
        ItemStack invalid = recoveryPort.insertItem(0, new ItemStack(Items.DIRT), false);
        helper.assertTrue(invalid.getCount() == 1, "Grinder accepted an item with no grinder recipe");
        helper.assertTrue(recoveryPort.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(),
                "Grinder rejected its generated cobblestone recipe");
        helper.assertTrue(recoveryPort.extractItem(0, 1, false).is(Items.COBBLESTONE),
                "Back service port could not recover grinder input");
        recoveryPort.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
        controller.energy().setEnergyStored(5_000);

        for (int tick = 0; tick < 60; tick++) {
            AdvancedMultiblockBlockEntity.serverTick(
                    helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
            );
        }

        helper.assertTrue(controller.inventory().getStackInSlot(0).isEmpty(),
                "Grinder did not consume its input");
        helper.assertTrue(controller.inventory().getStackInSlot(1).is(Items.GRAVEL),
                "Grinder did not produce the guaranteed gravel output");
        helper.assertTrue(controller.energy().getEnergyStored() == 2_600,
                "Grinder energy accounting was not 60 x 40 FE");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void thermalAndRefineryChainsPreserveLegacyRatios(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();

        List<BlockPos> boilerBlocks = build(
                helper, MultiblockDefinition.BIG_STEAM_BOILER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity boiler = requireController(helper, CONTROLLER);
        helper.assertTrue(boiler.tryForm(player), "Big steam boiler did not form");
        boiler.heat().node().setTemperature(500.0D);
        boiler.tank(0).tank().fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1),
                IFluidHandler.FluidAction.EXECUTE);
        AdvancedMultiblockBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(CONTROLLER), boiler.getBlockState(), boiler
        );
        helper.assertTrue(boiler.tank(0).tank().getFluidAmount() == 0,
                "Boiler did not consume one water mB");
        helper.assertTrue(boiler.tank(1).tank().getFluidAmount() == 10,
                "Boiler did not produce ten steam mB");
        boiler.unform();
        clear(helper, boilerBlocks);

        List<BlockPos> refineryBlocks = build(
                helper, MultiblockDefinition.REFINERY, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity refinery = requireController(helper, CONTROLLER);
        helper.assertTrue(refinery.tryForm(player), "Refinery did not form");
        refinery.tank(0).tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.HOT_CRUDE).source().get(), 100),
                IFluidHandler.FluidAction.EXECUTE
        );
        refinery.tank(1).tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), 64_000),
                IFluidHandler.FluidAction.EXECUTE
        );
        AdvancedMultiblockBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(CONTROLLER), refinery.getBlockState(), refinery
        );
        helper.assertTrue(refinery.tank(0).tank().getFluidAmount() == 0,
                "Refinery did not consume its 100 mB batch");
        helper.assertTrue(refinery.tank(1).tank().getFluidAmount() == 63_980,
                "Refinery process steam cost was not 20 mB");
        helper.assertTrue(refinery.tank(2).tank().getFluidAmount() == 4
                        && refinery.tank(3).tank().getFluidAmount() == 3
                        && refinery.tank(4).tank().getFluidAmount() == 3,
                "Refinery did not preserve the 4:3:3 output ratio");
        clear(helper, refineryBlocks);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void pumpjackFindsNearbyFiniteDepositsForEveryFacing(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> occupied = build(
                    helper, MultiblockDefinition.PUMPJACK, CONTROLLER, facing, false
            );
            BlockPos drill = CONTROLLER.relative(facing, 5);
            BlockPos depositPosition = drill.offset(2, -1, -1);
            helper.setBlock(depositPosition, ModAdvancedBlocks.OIL_DEPOSIT.get());
            OilDepositBlockEntity deposit = (OilDepositBlockEntity) helper.getBlockEntity(depositPosition);
            AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
            helper.assertTrue(controller.tryForm(player), "Pumpjack did not form facing " + facing);
            controller.energy().setEnergyStored(10_000);

            for (int tick = 0; tick < 100 && controller.tank(0).tank().getFluidAmount() == 0; tick++) {
                AdvancedMultiblockBlockEntity.serverTick(
                        helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
                );
            }

            helper.assertTrue(controller.tank(0).tank().getFluidAmount() == 20,
                    "Pumpjack missed nearby deposit facing " + facing);
            helper.assertTrue(deposit.remaining() == OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS - 20,
                    "Pumpjack reserve accounting failed facing " + facing);
            controller.unform();
            clear(helper, occupied);
            helper.setBlock(depositPosition, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 240)
    public static void controllerDropPreservesBulkStateAndClearsWorldIdentity(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.CONTAINER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        controller.setOwner(player.getUUID());
        helper.assertTrue(controller.tryForm(player), "Container did not form");
        for (int stack = 0; stack < 1_024; stack++) {
            helper.assertTrue(controller.bulkStorage().insertItem(
                    0, new ItemStack(Items.COBBLESTONE, 64), false
            ).isEmpty(), "Container rejected content before reaching 65,536 items");
        }

        LootParams.Builder loot = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(CONTROLLER)))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, controller)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player);
        ItemStack droppedController = controller.getBlockState().getDrops(loot).stream()
                .filter(stack -> stack.is(ModAdvancedBlocks.controller(MultiblockDefinition.CONTAINER).get().asItem()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Content-bearing controller was lost with the wrong tool"));
        helper.assertTrue(droppedController.getMaxStackSize() == 1,
                "Portable controller contents were stackable");
        CompoundTag blockEntityTag = droppedController.getTagElement("BlockEntityTag");
        helper.assertTrue(blockEntityTag != null, "Controller drop has no BlockEntityTag");
        helper.assertFalse(blockEntityTag.getBoolean("formed"), "Dropped controller remained formed");
        helper.assertFalse(blockEntityTag.contains("owner"), "Dropped controller retained its world owner");
        helper.assertTrue(blockEntityTag.getCompound(MachineBlockEntity.MODULES_TAG)
                        .contains("magneticraft:advanced_bulk_inventory"),
                "Bulk inventory module was not stored on the controller item");

        BlockPos restoredPosition = CONTROLLER.offset(12, 0, 0);
        helper.setBlock(restoredPosition,
                ModAdvancedBlocks.controller(MultiblockDefinition.CONTAINER).get().defaultBlockState());
        AdvancedMultiblockBlockEntity restored = requireController(helper, restoredPosition);
        restored.load(blockEntityTag.copy());
        helper.assertTrue(restored.bulkStorage().amount() == 65_536,
                "Controller item lost bulk item count");
        helper.assertTrue(restored.bulkStorage().getStackInSlot(0).is(Items.COBBLESTONE),
                "Controller item lost bulk item type");
        helper.assertFalse(restored.formed(), "Restored controller became formed without validation");
        helper.assertTrue(restored.owner() == null, "Restored controller inherited the previous owner");
        clear(helper, occupied);
        helper.setBlock(restoredPosition, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void combustionFluidPortRejectsInvalidFluidAndAllowsRecovery(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.BIG_COMBUSTION_CHAMBER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Big combustion chamber did not form");
        IFluidHandler input = controller.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        IFluidHandler recovery = controller.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN)
                .orElseThrow(AssertionError::new);
        helper.assertTrue(input.fill(
                new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100),
                IFluidHandler.FluidAction.EXECUTE
        ) == 0, "Combustion chamber accepted a fluid with no fuel recipe");
        helper.assertTrue(input.fill(
                new FluidStack(ModFluids.get(FluidDefinition.FUEL).source().get(), 100),
                IFluidHandler.FluidAction.EXECUTE
        ) == 100, "Combustion chamber rejected a generated fluid fuel");
        helper.assertTrue(recovery.drain(100, IFluidHandler.FluidAction.EXECUTE).getAmount() == 100,
                "Bottom service port could not recover fluid fuel");
        clear(helper, occupied);
        helper.succeed();
    }

    private static List<BlockPos> build(
            GameTestHelper helper,
            MultiblockDefinition definition,
            BlockPos controller,
            Direction facing,
            boolean mirrored
    ) {
        List<BlockPos> occupied = new ArrayList<>();
        for (MultiblockCell cell : definition.cells()) {
            if (cell.rule() == MultiblockRule.IGNORE) {
                continue;
            }
            BlockPos position = transformedPosition(definition, controller, facing, mirrored, cell);
            BlockState state = cell.rule() == MultiblockRule.CONTROLLER
                    ? ModAdvancedBlocks.controller(definition).get().defaultBlockState()
                    .setValue(AdvancedMultiblockBlock.FACING, facing)
                    : stateFor(cell.rule(), facing);
            helper.setBlock(position, state);
            occupied.add(position);
        }
        return occupied;
    }

    private static BlockPos transformedPosition(
            MultiblockDefinition definition,
            BlockPos controller,
            Direction facing,
            boolean mirrored,
            MultiblockCell cell
    ) {
        StructureOffset relative = MultiblockTransform.relative(
                cell.offset(), definition.center(), facing, mirrored
        );
        return controller.offset(relative.x(), relative.y(), relative.z());
    }

    private static BlockState stateFor(MultiblockRule rule) {
        return stateFor(rule, Direction.NORTH);
    }

    private static BlockState stateFor(MultiblockRule rule, Direction facing) {
        return switch (rule) {
            case AIR, IGNORE -> Blocks.AIR.defaultBlockState();
            case BASE -> ModAdvancedBlocks.MULTIBLOCK_BASE.get().defaultBlockState();
            case GRATE -> ModMachineBlocks.GRATE.get().defaultBlockState();
            case CORRUGATED_IRON -> ModAdvancedBlocks.CORRUGATED_IRON.get().defaultBlockState();
            case COPPER_COIL -> ModAdvancedBlocks.COPPER_COIL.get().defaultBlockState();
            case COLUMN_X -> column(rotatedAxis(Direction.Axis.X, facing));
            case COLUMN_Y -> column(Direction.Axis.Y);
            case COLUMN_Z -> column(rotatedAxis(Direction.Axis.Z, facing));
            case BRICKS -> Blocks.BRICKS.defaultBlockState();
            case SMALL_TANK -> ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK)
                    .get().defaultBlockState();
            case STRIPED -> ModAdvancedBlocks.STRIPED_MULTIBLOCK_PART.get().defaultBlockState();
            case ELECTRIC -> ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get().defaultBlockState();
            case CONTROLLER -> throw new IllegalArgumentException("Controller state requires a definition");
        };
    }

    private static BlockState column(Direction.Axis axis) {
        return ModAdvancedBlocks.MULTIBLOCK_COLUMN.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, axis);
    }

    private static Direction.Axis rotatedAxis(Direction.Axis localAxis, Direction facing) {
        if (localAxis == Direction.Axis.Y || facing.getAxis() == Direction.Axis.Z) {
            return localAxis;
        }
        return localAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    private static BlockPos firstReplaceableMember(
            MultiblockDefinition definition,
            BlockPos controller,
            Direction facing,
            boolean mirrored
    ) {
        return definition.memberCells().stream()
                .filter(cell -> cell.rule() != MultiblockRule.CONTROLLER)
                .map(cell -> transformedPosition(definition, controller, facing, mirrored, cell))
                .findFirst()
                .orElseThrow();
    }

    private static AdvancedMultiblockBlockEntity requireController(GameTestHelper helper, BlockPos position) {
        var blockEntity = helper.getBlockEntity(position);
        helper.assertTrue(blockEntity instanceof AdvancedMultiblockBlockEntity, "Missing multiblock controller");
        return (AdvancedMultiblockBlockEntity) blockEntity;
    }

    private static void clear(GameTestHelper helper, List<BlockPos> positions) {
        positions.forEach(position -> helper.setBlock(position, Blocks.AIR));
    }
}
