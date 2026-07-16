package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlock;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockMenu;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockCell;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.LegacyMultiblockCollision;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockRule;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockPortLayout;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockTransform;
import committee.nova.mods.magneticraft.content.multiblock.StructureOffset;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockGapBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockEvents;
import committee.nova.mods.magneticraft.content.machine.observation.MachineObservationService;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.item.TieredElectricalBlockItem;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import committee.nova.mods.magneticraft.system.network.diagnostic.DiagnosticHost;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            for (BlockPos member : controller.members()) {
                var actual = helper.getLevel().getBlockState(member).getCollisionShape(
                        helper.getLevel(), member, CollisionContext.empty()
                );
                var expectedShape = LegacyMultiblockCollision.shapeAt(
                        member, controller.getBlockPos(), definition, controller.facing()
                );
                helper.assertFalse(Shapes.joinIsNotEmpty(actual, expectedShape, BooleanOp.NOT_SAME),
                        definition.id() + " collision diverged at " + member);
            }

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

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void formedMembersAllowBlockItemsToPlaceAgainstThem(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.GRINDER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Grinder did not form for member interaction test");

        Set<BlockPos> members = new HashSet<>(controller.members());
        BlockPos clicked = null;
        Direction face = null;
        search:
        for (BlockPos member : members) {
            if (member.equals(controller.getBlockPos())) {
                continue;
            }
            for (Direction candidate : Direction.values()) {
                BlockPos target = member.relative(candidate);
                if (!members.contains(target) && helper.getLevel().getBlockState(target).isAir()) {
                    clicked = member;
                    face = candidate;
                    break search;
                }
            }
        }
        helper.assertTrue(clicked != null && face != null, "Formed grinder had no exposed member face");

        ItemStack stone = new ItemStack(Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stone);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked), face, clicked, false);
        PlayerInteractEvent.RightClickBlock placementEvent = new PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, clicked, hit
        );
        MultiblockEvents.onMemberInteract(placementEvent);
        helper.assertFalse(placementEvent.isCanceled(), "Member interaction consumed a block placement");

        InteractionResult placement = stone.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        BlockPos placed = clicked.relative(face);
        helper.assertTrue(placement.consumesAction(), "Stone placement did not consume its normal item action");
        helper.assertTrue(helper.getLevel().getBlockState(placed).is(Blocks.STONE),
                "Block item did not place against the formed member");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        PlayerInteractEvent.RightClickBlock statusEvent = new PlayerInteractEvent.RightClickBlock(
                player, InteractionHand.MAIN_HAND, clicked, hit
        );
        MultiblockEvents.onMemberInteract(statusEvent);
        helper.assertTrue(statusEvent.isCanceled(), "Non-block member interaction was not safely consumed");

        helper.getLevel().removeBlock(placed, false);
        controller.unform();
        clear(helper, occupied);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void formedElectricalPortsProxyDiagnosticsFromTheirExactFace(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.GRINDER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Grinder did not form for port diagnostics");
        MultiblockPortLayout.Port port = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(candidate -> candidate.kind() == MultiblockPortLayout.Kind.ELECTRICITY)
                .filter(candidate -> !candidate.worldPosition(controller).equals(controller.getBlockPos()))
                .findFirst()
                .orElseThrow();

        helper.runAfterDelay(2, () -> {
            BlockPos portPosition = port.worldPosition(controller);
            Direction portSide = port.worldSide(controller.facing());
            var blockEntity = helper.getLevel().getBlockEntity(portPosition);
            helper.assertTrue(blockEntity instanceof MultiblockGapBlockEntity,
                    "Formed electrical port was not backed by a gap proxy");
            DiagnosticHost diagnostics = (DiagnosticHost) blockEntity;
            controller.electricity().node().setVoltage(360.0D);
            helper.assertTrue(diagnostics.electricalReading(portSide).isPresent(),
                    "Exact multiblock electrical port face did not expose point telemetry");
            helper.assertTrue(MachineObservationService.observe(blockEntity, portSide).electrical().isPresent(),
                    "Jade observation did not follow the multiblock electrical port proxy");
            helper.assertTrue(diagnostics.electricalNetworkSummary(portSide, 4_096).isPresent(),
                    "Network summary did not start at the physical multiblock port node");
            helper.assertTrue(diagnostics.nearestElectricalFault(portSide, 4_096).isPresent(),
                    "Fault search did not start at the physical multiblock port node");

            Direction unsupported = java.util.Arrays.stream(Direction.values())
                    .filter(side -> !MultiblockPortLayout.supports(
                            controller, portPosition, NetworkDomain.ELECTRICITY, side
                    ))
                    .findFirst()
                    .orElseThrow();
            helper.assertTrue(diagnostics.electricalReading(unsupported).isEmpty(),
                    "A non-port face leaked multiblock electrical telemetry");
            clear(helper, occupied);
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void formedElectricalPortsAcceptMatchingTierConnectors(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.GRINDER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Grinder did not form for connector placement");
        MultiblockPortLayout.Port port = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(candidate -> candidate.kind() == MultiblockPortLayout.Kind.ELECTRICITY)
                .filter(candidate -> !candidate.worldPosition(controller).equals(controller.getBlockPos()))
                .findFirst()
                .orElseThrow();
        BlockPos portPosition = port.worldPosition(controller);
        Direction portSide = port.worldSide(controller.facing());
        BlockPos connectorPosition = portPosition.relative(portSide);
        helper.assertTrue(helper.getLevel().getBlockState(connectorPosition).isAir(),
                "Grinder electrical port did not have room for a connector");

        ItemStack connector = TieredElectricalBlockItem.stackForTier(
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get(),
                VoltageTierIds.MEDIUM
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, connector);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(portPosition).add(
                        portSide.getStepX() * 0.5D,
                        portSide.getStepY() * 0.5D,
                        portSide.getStepZ() * 0.5D
                ),
                portSide,
                portPosition,
                false
        );
        InteractionResult placement = connector.useOn(new UseOnContext(
                player, InteractionHand.MAIN_HAND, hit
        ));
        helper.assertTrue(placement.consumesAction(),
                "Matching-tier connector could not be placed on the exact multiblock electrical port");
        helper.assertTrue(helper.getLevel().getBlockState(connectorPosition)
                        .is(ModNetworkBlocks.ELECTRIC_CONNECTOR.get()),
                "Connector item consumed its action without placing a connector");

        helper.runAfterDelay(2, () -> {
            helper.assertTrue(PhysicalNetworkService.manager(helper.getLevel())
                            .neighbors(NetworkDomain.ELECTRICITY, connectorPosition)
                            .contains(portPosition.asLong()),
                    "Placed connector did not join the exact multiblock electrical port");
            helper.getLevel().removeBlock(connectorPosition, false);
            controller.unform();
            clear(helper, occupied);
            player.discard();
            helper.succeed();
        });
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
        controller.energy().setStoredJoules(5_000);
        AdvancedMultiblockBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
        );
        helper.assertTrue(controller.working(), "Grinder did not enter working state before schema reset");
        helper.assertFalse(controller.inventory().getStackInSlot(0).isEmpty(), "Grinder lost its input before schema reset");
        helper.assertTrue(controller.energy().storedWholeJoules() > 0, "Grinder lost all energy before schema reset");
        helper.assertTrue(controller.progress() > 0, "Grinder did not advance before schema reset");
        helper.assertTrue(controller.totalProgress() > 0, "Grinder did not retain its active recipe before schema reset");
        helper.assertTrue(player.getUUID().equals(controller.owner()), "Grinder did not retain its owner before schema reset");

        CompoundTag incompatible = controller.saveWithoutMetadata();
        incompatible.putInt("schema_version", 2);
        controller.load(incompatible);

        helper.assertTrue(controller.inventory().getStackInSlot(0).isEmpty(), "Schema reset retained grinder inventory");
        helper.assertTrue(controller.energy().storedWholeJoules() == 0, "Schema reset retained grinder energy");
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
    public static void rotationsLegacyMirrorCompatibilityAndChunkBoundaryUseTheSameDefinition(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        MultiblockDefinition definition = MultiblockDefinition.OIL_HEATER;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> occupied = build(helper, definition, CONTROLLER, facing, false);
            AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
            helper.assertTrue(controller.tryForm(player),
                    "oil_heater did not form facing=" + facing);
            controller.unform();
            clear(helper, occupied);
        }

        List<BlockPos> normalPumpjack = build(
                helper, MultiblockDefinition.PUMPJACK, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity normalController = requireController(helper, CONTROLLER);
        helper.assertTrue(normalController.tryForm(player), "Normal pumpjack did not form");
        normalController.unform();
        clear(helper, normalPumpjack);

        List<BlockPos> mirroredPumpjack = build(
                helper, MultiblockDefinition.PUMPJACK, CONTROLLER, Direction.NORTH, true
        );
        AdvancedMultiblockBlockEntity mirroredController = requireController(helper, CONTROLLER);
        helper.assertFalse(mirroredController.tryForm(player),
                "Newly formed pumpjack accepted the removed mirror extension");

        CompoundTag legacyMirror = mirroredController.saveWithoutMetadata();
        legacyMirror.putBoolean("formed", true);
        legacyMirror.putBoolean("mirrored", true);
        mirroredController.load(legacyMirror);
        AdvancedMultiblockBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                mirroredController.getBlockState(),
                mirroredController
        );
        helper.assertTrue(mirroredController.operational(),
                "Already formed 0.2-0.5 mirrored pumpjack did not load compatibly");
        AdvancedMultiblockMenu mirroredMenu = new AdvancedMultiblockMenu(
                1, player.getInventory(), mirroredController
        );
        helper.assertTrue(mirroredMenu.mirrored(),
                "Advanced menu lost the mirrored structure state");
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

    @GameTest(template = TEMPLATE, timeoutTicks = 500)
    public static void unloadedCrossChunkMemberSuspendsWithoutForceLoadingAndRecovers(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerChunkCache chunks = level.getChunkSource();
        BlockPos absoluteOrigin = helper.absolutePos(BlockPos.ZERO);
        int boundaryX = ((absoluteOrigin.getX() >> 4) + 32) << 4;
        int remoteZ = (((absoluteOrigin.getZ() >> 4) + 32) << 4) + 8;
        BlockPos controllerPosition = new BlockPos(
                boundaryX - absoluteOrigin.getX(),
                2,
                remoteZ - absoluteOrigin.getZ()
        );
        BlockPos absoluteController = helper.absolutePos(controllerPosition);
        ChunkPos controllerChunk = new ChunkPos(absoluteController);

        chunks.addRegionTicket(TicketType.FORCED, controllerChunk, 0, controllerChunk);
        chunks.getChunk(controllerChunk.x, controllerChunk.z, ChunkStatus.FULL, true);
        List<BlockPos> occupied = build(
                helper,
                MultiblockDefinition.SOLAR_PANEL,
                controllerPosition,
                Direction.NORTH,
                false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, controllerPosition);
        Player player = helper.makeMockSurvivalPlayer();

        BlockPos unloadedMember = occupied.stream()
                .filter(position -> !new ChunkPos(helper.absolutePos(position)).equals(controllerChunk))
                .findFirst()
                .orElseThrow();
        ChunkPos memberChunk = new ChunkPos(helper.absolutePos(unloadedMember));
        boolean[] sawUnload = {false};
        boolean[] memberTicketAdded = {false};
        boolean[] finished = {false};
        int[] stableUnloadedTicks = {0};
        Runnable cleanup = () -> {
            if (finished[0]) {
                return;
            }
            finished[0] = true;
            if (!memberTicketAdded[0]) {
                chunks.addRegionTicket(TicketType.FORCED, memberChunk, 0, memberChunk);
                memberTicketAdded[0] = true;
            }
            chunks.getChunk(memberChunk.x, memberChunk.z, ChunkStatus.FULL, true);
            controller.unform();
            clear(helper, occupied);
            chunks.removeRegionTicket(TicketType.FORCED, memberChunk, 0, memberChunk);
            chunks.removeRegionTicket(TicketType.FORCED, controllerChunk, 0, controllerChunk);
        };

        if (!controller.tryForm(player)) {
            cleanup.run();
            helper.fail("Cross-chunk solar panel did not form before unload testing");
            return;
        }

        helper.onEachTick(() -> {
            if (finished[0]) {
                return;
            }
            if (helper.getTick() >= 460) {
                cleanup.run();
                helper.fail("Cross-chunk member did not complete the unload/reload lifecycle");
                return;
            }

            if (!sawUnload[0]) {
                if (chunks.hasChunk(memberChunk.x, memberChunk.z)) {
                    return;
                }
                sawUnload[0] = true;
                AdvancedMultiblockBlockEntity.serverTick(
                        level,
                        absoluteController,
                        controller.getBlockState(),
                        controller
                );
                if (!controller.formed()
                        || controller.operational()
                        || chunks.hasChunk(memberChunk.x, memberChunk.z)) {
                    cleanup.run();
                    helper.fail("Unloaded member forced a load, kept the machine operational, or unformed it");
                }
                return;
            }

            if (!memberTicketAdded[0]) {
                AdvancedMultiblockBlockEntity.serverTick(
                        level,
                        absoluteController,
                        controller.getBlockState(),
                        controller
                );
                if (!controller.formed() || chunks.hasChunk(memberChunk.x, memberChunk.z)) {
                    cleanup.run();
                    helper.fail("Repeated suspended ticks loaded the member chunk or unformed the controller");
                    return;
                }
                if (++stableUnloadedTicks[0] < 5) {
                    return;
                }
                chunks.addRegionTicket(TicketType.FORCED, memberChunk, 0, memberChunk);
                memberTicketAdded[0] = true;
                chunks.getChunk(memberChunk.x, memberChunk.z, ChunkStatus.FULL, true);
                return;
            }

            AdvancedMultiblockBlockEntity.serverTick(
                    level,
                    absoluteController,
                    controller.getBlockState(),
                    controller
            );
            if (!controller.operational()) {
                return;
            }
            boolean remainedFormed = controller.formed();
            cleanup.run();
            helper.assertTrue(remainedFormed, "Cross-chunk reload lost the durable formed state");
            helper.succeed();
        });
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
        helper.assertTrue(memberTank.claimedByMultiblock(), "Turbine tank was not claimed before projection");
        helper.assertTrue(
                helper.getBlockState(tankPosition).is(ModAdvancedBlocks.MULTIBLOCK_GAP.get()),
                "Formed turbine did not replace its tank with an invisible gap"
        );
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

        MultiblockPortLayout.Port steamPort = MultiblockPortLayout.ports(definition).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.FLUID)
                .findFirst()
                .orElseThrow();
        var cachedPortCapability = helper.getLevel().getBlockEntity(steamPort.worldPosition(controller))
                .getCapability(ForgeCapabilities.FLUID_HANDLER, steamPort.worldSide(controller.facing()));
        helper.assertTrue(cachedPortCapability.isPresent(), "Formed turbine exact port lost steam input");
        controller.unform();
        helper.assertFalse(cachedPortCapability.isPresent(), "Cached exact-port capability survived unform");
        helper.assertTrue(
                helper.getBlockState(tankPosition).is(
                        ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get()
                ),
                "Unformed turbine did not restore its member tank"
        );
        SingleBlockMachineBlockEntity restoredTank =
                (SingleBlockMachineBlockEntity) helper.getBlockEntity(tankPosition);
        IFluidHandler restored = restoredTank.getCapability(ForgeCapabilities.FLUID_HANDLER)
                .orElseThrow(AssertionError::new);
        helper.assertTrue(restored.getFluidInTank(0).getAmount() == 500,
                "Member tank contents changed while claimed");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void formedGapForwardsOnlyExactOilHeaterFluidPorts(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.OIL_HEATER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Oil heater did not form");

        MultiblockPortLayout.Port inputPort = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.FLUID && port.target() == 0)
                .findFirst()
                .orElseThrow();
        Direction inputSide = inputPort.worldSide(controller.facing());
        BlockPos inputPosition = inputPort.worldPosition(controller);
        IFluidHandler input = helper.getLevel().getBlockEntity(inputPosition)
                .getCapability(ForgeCapabilities.FLUID_HANDLER, inputSide)
                .orElseThrow(AssertionError::new);
        helper.assertFalse(helper.getLevel().getBlockEntity(inputPosition)
                        .getCapability(ForgeCapabilities.FLUID_HANDLER, inputSide.getOpposite()).isPresent(),
                "Oil input gap exposed a non-port face");
        int filled = input.fill(
                new FluidStack(ModFluids.get(FluidDefinition.OIL).source().get(), 250),
                IFluidHandler.FluidAction.EXECUTE
        );
        helper.assertTrue(filled == 250 && controller.tank(0).tank().getFluidAmount() == 250,
                "Oil input gap did not forward into controller tank 0");
        helper.assertTrue(input.drain(50, IFluidHandler.FluidAction.EXECUTE).getAmount() == 50,
                "Released oil-heater feed port did not preserve bidirectional tank access");

        MultiblockPortLayout.Port outputPort = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.FLUID && port.target() == 1)
                .findFirst()
                .orElseThrow();
        controller.tank(1).tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.HOT_CRUDE).source().get(), 180),
                IFluidHandler.FluidAction.EXECUTE
        );
        IFluidHandler output = helper.getLevel().getBlockEntity(outputPort.worldPosition(controller))
                .getCapability(ForgeCapabilities.FLUID_HANDLER, outputPort.worldSide(controller.facing()))
                .orElseThrow(AssertionError::new);
        helper.assertTrue(output.fill(
                new FluidStack(ModFluids.get(FluidDefinition.HOT_CRUDE).source().get(), 10),
                IFluidHandler.FluidAction.EXECUTE
        ) == 10, "Released oil-heater product port did not preserve bidirectional tank access");
        helper.assertTrue(output.drain(190, IFluidHandler.FluidAction.EXECUTE).getAmount() == 190,
                "Oil heater output gap did not drain controller tank 1");
        helper.assertFalse(controller.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent(),
                "Oil heater exposed an unsided controller fluid shortcut");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void steamBoilerFacesExposeSeparatedWaterAndSteamTanks(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.BIG_STEAM_BOILER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Big steam boiler did not form");

        MultiblockPortLayout.Port waterPort = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.FLUID && port.target() == 0)
                .findFirst()
                .orElseThrow();
        List<MultiblockPortLayout.Port> endpoint = MultiblockPortLayout.findAll(
                controller,
                waterPort.worldPosition(controller),
                waterPort.worldSide(controller.facing()),
                MultiblockPortLayout.Kind.FLUID
        );
        helper.assertTrue(endpoint.size() == 2,
                "Boiler face did not expose distinct water and steam routes");
        IFluidHandler face = helper.getLevel().getBlockEntity(waterPort.worldPosition(controller))
                .getCapability(ForgeCapabilities.FLUID_HANDLER, waterPort.worldSide(controller.facing()))
                .orElseThrow(AssertionError::new);
        helper.assertTrue(face.getTanks() == 2, "Boiler face merged the two fluids into one tank");
        helper.assertTrue(face.fill(
                new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 250),
                IFluidHandler.FluidAction.EXECUTE
        ) == 250, "Boiler face rejected water input");
        helper.assertTrue(face.fill(
                new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), 100),
                IFluidHandler.FluidAction.EXECUTE
        ) == 0, "Boiler steam output accepted input");
        controller.tank(1).tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), 180),
                IFluidHandler.FluidAction.EXECUTE
        );
        helper.assertTrue(face.drain(180, IFluidHandler.FluidAction.EXECUTE).getAmount() == 180,
                "Boiler face did not export steam");
        helper.assertFalse(controller.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent(),
                "Boiler exposed an unsided controller fluid shortcut");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void formedGapForwardsExactGrinderItemSlots(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.GRINDER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Grinder did not form");

        MultiblockPortLayout.Port inputPort = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.ITEM
                        && port.itemAccess().canInsert(0)
                        && !port.worldPosition(controller).equals(controller.getBlockPos()))
                .findFirst()
                .orElseThrow();
        IItemHandler input = helper.getLevel().getBlockEntity(inputPort.worldPosition(controller))
                .getCapability(ForgeCapabilities.ITEM_HANDLER, inputPort.worldSide(controller.facing()))
                .orElseThrow(AssertionError::new);
        helper.assertTrue(input.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(),
                "Grinder input gap rejected a valid recipe item");
        helper.assertTrue(controller.inventory().getStackInSlot(0).is(Items.COBBLESTONE),
                "Grinder input gap did not change controller slot 0");
        helper.assertTrue(input.extractItem(1, 1, false).isEmpty(),
                "Grinder input gap exposed an output slot");

        controller.inventory().setStackInSlot(1, new ItemStack(Items.GRAVEL, 2));
        MultiblockPortLayout.Port outputPort = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.ITEM
                        && port.itemAccess().canExtract(1))
                .findFirst()
                .orElseThrow();
        IItemHandler output = helper.getLevel().getBlockEntity(outputPort.worldPosition(controller))
                .getCapability(ForgeCapabilities.ITEM_HANDLER, outputPort.worldSide(controller.facing()))
                .orElseThrow(AssertionError::new);
        helper.assertTrue(output.insertItem(0, new ItemStack(Items.COBBLESTONE), false).getCount() == 1,
                "Grinder output gap accepted input");
        helper.assertTrue(output.extractItem(1, 2, false).getCount() == 2,
                "Grinder output gap did not extract controller slot 1");
        helper.assertFalse(controller.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),
                "Grinder exposed an unsided controller item shortcut");
        helper.assertFalse(controller.getCapability(ForgeCapabilities.ENERGY).isPresent(),
                "Grinder exposed Forge Energy outside its native electrical ports");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void exactGrinderElectricalPortConnectsAndTransfers(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.GRINDER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Grinder did not form");
        MultiblockPortLayout.Port port = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(candidate -> candidate.kind() == MultiblockPortLayout.Kind.ELECTRICITY)
                .findFirst()
                .orElseThrow();
        Direction outward = port.worldSide(controller.facing());
        BlockPos cablePosition = port.worldPosition(controller).relative(outward);
        helper.getLevel().setBlock(cablePosition, ModNetworkBlocks.ELECTRIC_CABLE.get().defaultBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        ElectricCableBlockEntity cable = (ElectricCableBlockEntity) helper.getLevel().getBlockEntity(cablePosition);
        cable.electricity().applyTierFromPlacementData(Magneticraft.id("medium_voltage"));
        cable.electricity().node().setEnergyJoules(2_000.0D);

        helper.runAfterDelay(20, () -> {
            var manager = PhysicalNetworkService.manager(helper.getLevel());
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, cablePosition).size() == 2,
                    "Grinder port proxy did not join the cable component");
            helper.assertTrue(controller.electricity().node().energyJoules() > 0.0D
                            || controller.energy().storedWholeJoules() > 0,
                    "Grinder port proxy did not transfer electrical energy");
            helper.getLevel().setBlock(cablePosition, Blocks.AIR.defaultBlockState(),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
            clear(helper, occupied);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unformedHologramTogglePersists(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        helper.setBlock(CONTROLLER, ModAdvancedBlocks.controller(MultiblockDefinition.SOLAR_PANEL)
                .get().defaultBlockState());
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.hologramEnabled(), "New controller did not default to hologram enabled");
        controller.toggleHologram(player);
        helper.assertFalse(controller.hologramEnabled(), "Hologram toggle did not disable projection");
        CompoundTag saved = controller.saveWithoutMetadata();
        AdvancedMultiblockBlockEntity restored = new AdvancedMultiblockBlockEntity(
                controller.getBlockPos(), controller.getBlockState()
        );
        restored.load(saved);
        helper.assertFalse(restored.hologramEnabled(), "Hologram toggle did not persist");
        helper.setBlock(CONTROLLER, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void frozenTankClaimCannotBeOverwritten(GameTestHelper helper) {
        BlockPos tankPosition = new BlockPos(4, 2, 4);
        helper.setBlock(
                tankPosition,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get().defaultBlockState()
        );
        SingleBlockMachineBlockEntity tank = (SingleBlockMachineBlockEntity) helper.getBlockEntity(tankPosition);
        BlockPos firstController = helper.absolutePos(new BlockPos(64, 2, 64));
        BlockPos secondController = helper.absolutePos(new BlockPos(80, 2, 64));

        helper.assertTrue(tank.claimForMultiblock(firstController), "Initial tank claim was rejected");
        helper.assertFalse(tank.canClaimForMultiblock(secondController),
                "A different controller was allowed to reserve a frozen tank claim");
        helper.assertFalse(tank.claimForMultiblock(secondController),
                "A different controller overwrote a frozen tank claim");
        tank.releaseMultiblockClaim(secondController);
        helper.assertTrue(tank.claimedByMultiblock(), "Wrong controller released the tank claim");
        tank.releaseMultiblockClaim(firstController);
        helper.assertFalse(tank.claimedByMultiblock(), "Recorded controller could not release the tank claim");
        helper.setBlock(tankPosition, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void completeAdvancedRecipeCatalogLoadsAtRuntime(GameTestHelper helper) {
        int processingRecipes = ModRecipeTypes.advancedProcessingTypes().values().stream()
                .mapToInt(type -> helper.getLevel().getRecipeManager().getAllRecipesFor(type.get()).size())
                .sum();
        int fluidFuels = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.FLUID_FUEL_TYPE.get()).size();
        int polymerizing = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.POLYMERIZING_TYPE.get()).size();
        helper.assertTrue(processingRecipes == 94,
                "Expected 94 advanced processing recipes, loaded " + processingRecipes);
        helper.assertTrue(fluidFuels == 10,
                "Expected 10 fluid fuel recipes, loaded " + fluidFuels);
        helper.assertTrue(polymerizing == 2,
                "Expected 2 polymerizing recipes, loaded " + polymerizing);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 180)
    public static void polymerizerHonorsFluidPortTemperatureAndPerTickHeatCost(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.POLYMERIZER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Polymerizer did not form");

        MultiblockPortLayout.Port fluidPort = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.FLUID)
                .findFirst()
                .orElseThrow();
        IFluidHandler input = helper.getLevel().getBlockEntity(fluidPort.worldPosition(controller))
                .getCapability(ForgeCapabilities.FLUID_HANDLER, fluidPort.worldSide(controller.facing()))
                .orElseThrow(AssertionError::new);
        helper.assertTrue(input.fill(
                new FluidStack(ModFluids.get(FluidDefinition.PLASTIC).source().get(), 250),
                IFluidHandler.FluidAction.EXECUTE
        ) == 250, "Polymerizer rejected liquid plastic through its top input");

        controller.heat().node().setTemperature(400.0D);
        AdvancedMultiblockBlockEntity.serverTick(
                helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
        );
        helper.assertTrue(controller.progress() == 0 && controller.tank(0).tank().getFluidAmount() == 250,
                "Polymerizer advanced below its recipe minimum temperature");

        controller.heat().node().setTemperature(500.0D);
        double initialHeat = controller.heat().node().internalEnergyJoules();
        for (int tick = 0; tick < 100; tick++) {
            AdvancedMultiblockBlockEntity.serverTick(
                    helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
            );
        }
        helper.assertTrue(controller.tank(0).tank().getFluidAmount() == 0,
                "Polymerizer did not consume exactly 250 mB liquid plastic");
        helper.assertTrue(controller.inventory().getStackInSlot(1).is(ModItems.PLASTIC_SHEET.get()),
                "Polymerizer did not produce one plastic sheet");
        helper.assertTrue(Math.abs(initialHeat - controller.heat().node().internalEnergyJoules() - 2_000.0D) < 0.001D,
                "Polymerizer heat accounting was not 100 x 20 J");
        clear(helper, occupied);
        player.discard();
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
        helper.assertTrue(controller.energy().storedWholeJoules() == 20,
                "Steam engine did not stage exactly 20 J in its generator cache");
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(Math.abs(controller.electricity().node().energyJoules() - 20.0D) < 0.0001D,
                    "Steam engine did not export exactly 20 J to its electrical terminal");
            clear(helper, occupied);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void grinderRejectsInvalidInputRecoversInputAndCompletesRecipe(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.GRINDER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Grinder did not form");
        MultiblockPortLayout.Port inputPort = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.ITEM
                        && port.itemAccess().canInsert(0)
                        && !port.worldPosition(controller).equals(controller.getBlockPos()))
                .findFirst()
                .orElseThrow();
        IItemHandler inputHandler = helper.getLevel().getBlockEntity(inputPort.worldPosition(controller))
                .getCapability(ForgeCapabilities.ITEM_HANDLER, inputPort.worldSide(controller.facing()))
                .orElseThrow(AssertionError::new);
        ItemStack invalid = inputHandler.insertItem(0, new ItemStack(Items.DIRT), false);
        helper.assertTrue(invalid.getCount() == 1, "Grinder accepted an item with no grinder recipe");
        helper.assertTrue(inputHandler.insertItem(0, new ItemStack(Items.COBBLESTONE), false).isEmpty(),
                "Grinder rejected its generated cobblestone recipe");
        helper.assertTrue(inputHandler.extractItem(0, 1, false).isEmpty(),
                "Grinder input port exposed extraction");
        helper.assertTrue(controller.inventory().extractInternal(0, 1, false).is(Items.COBBLESTONE),
                "Grinder internal input could not be reset for processing check");
        inputHandler.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
        controller.energy().setStoredJoules(8_000);

        for (int tick = 0; tick < 60; tick++) {
            AdvancedMultiblockBlockEntity.serverTick(
                    helper.getLevel(), helper.absolutePos(CONTROLLER), controller.getBlockState(), controller
            );
        }

        helper.assertTrue(controller.inventory().getStackInSlot(0).isEmpty(),
                "Grinder did not consume its input");
        helper.assertTrue(controller.inventory().getStackInSlot(1).is(Items.GRAVEL),
                "Grinder did not produce the guaranteed gravel output");
        helper.assertTrue(controller.energy().storedWholeJoules() == 5_600,
                "Grinder energy accounting was not 60 x 40 J");
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

    @GameTest(template = TEMPLATE, timeoutTicks = 1_200)
    public static void pumpjackFindsNearbyFiniteDepositsForEveryFacing(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        BlockPos pumpjackController = CONTROLLER.above(2);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            List<BlockPos> occupied = build(
                    helper, MultiblockDefinition.PUMPJACK, pumpjackController, facing, false
            );
            BlockPos drill = pumpjackController.relative(facing, 5);
            BlockPos depositPosition = drill.offset(2, -1, -1);
            helper.setBlock(depositPosition, ModAdvancedBlocks.OIL_DEPOSIT.get());
            OilDepositBlockEntity deposit = (OilDepositBlockEntity) helper.getBlockEntity(depositPosition);
            AdvancedMultiblockBlockEntity controller = requireController(helper, pumpjackController);
            helper.assertTrue(controller.tryForm(player), "Pumpjack did not form facing " + facing);
            controller.energy().setStoredJoules(12_000);

            for (int tick = 0; tick < 600 && controller.tank(0).tank().getFluidAmount() == 0; tick++) {
                AdvancedMultiblockBlockEntity.serverTick(
                        helper.getLevel(), helper.absolutePos(pumpjackController), controller.getBlockState(), controller
                );
                if (tick == 0) {
                    CompoundTag firstTickState = controller.saveWithoutMetadata().getCompound("pumpjack");
                    helper.assertTrue("searching_deposit".equals(firstTickState.getString("phase")),
                            "Pumpjack initial search did not find its radius-three source facing " + facing
                                    + "; cursor=" + firstTickState.getInt("cursor_index")
                                    + "; origin=" + controller.getBlockPos().relative(controller.facing(), 5).below()
                                    + "; deposit=" + helper.absolutePos(depositPosition)
                                    + "; block_entity=" + helper.getLevel().getBlockEntity(
                                    helper.absolutePos(depositPosition)));
                }
                if (tick == 1) {
                    CompoundTag secondTickState = controller.saveWithoutMetadata().getCompound("pumpjack");
                    helper.assertTrue(secondTickState.getInt("cursor_index") == 640,
                            "Pumpjack deposit scan did not advance on its first work tick facing " + facing
                                    + "; state=" + secondTickState
                                    + "; formed=" + controller.formed()
                                    + "; operational=" + controller.operational()
                                    + "; energy=" + controller.energy().storedWholeJoules());
                }
                if (tick == 60) {
                    CompoundTag persisted = controller.saveWithoutMetadata();
                    helper.assertTrue(persisted.contains("pumpjack"),
                            "Pumpjack search state was not persisted facing " + facing);
                    CompoundTag persistedPumpjack = persisted.getCompound("pumpjack");
                    BlockPos absoluteDeposit = helper.absolutePos(depositPosition);
                    helper.assertTrue(persistedPumpjack.getInt("deposit_size") == 1,
                            "Pumpjack deposit scan did not count its discovered source facing " + facing
                                    + "; deposit=" + absoluteDeposit
                                    + "; origin=" + BlockPos.of(persistedPumpjack.getLong("deposit_origin"))
                                    + "; expected_drill=" + helper.absolutePos(drill)
                                    + "; actual_drill=" + controller.getBlockPos().relative(controller.facing(), 5)
                                    + "; cursor=" + persistedPumpjack.getInt("cursor_index")
                                    + "; energy=" + controller.energy().storedWholeJoules()
                                    + "; phase=" + persistedPumpjack.getString("phase")
                                    + "; block_entity=" + helper.getLevel().getBlockEntity(absoluteDeposit)
                                    + "; chunk_loaded=" + (helper.getLevel().getChunkSource().getChunkNow(
                                    absoluteDeposit.getX() >> 4, absoluteDeposit.getZ() >> 4) != null));
                    ItemStack portable = new ItemStack(
                            ModAdvancedBlocks.controller(MultiblockDefinition.PUMPJACK).get()
                    );
                    controller.saveToItem(portable);
                    CompoundTag portableState = portable.getTagElement("BlockEntityTag");
                    helper.assertTrue(portableState != null && !portableState.contains("pumpjack"),
                            "Portable pumpjack retained world search coordinates facing " + facing);
                    controller.load(persisted);
                }
            }

            CompoundTag finalState = controller.saveWithoutMetadata();
            helper.assertTrue(controller.tank(0).tank().getFluidAmount() == 1_000,
                    "Pumpjack missed nearby deposit facing " + facing
                            + "; formed=" + controller.formed()
                            + "; energy=" + controller.energy().storedWholeJoules()
                            + "; reserve=" + deposit.remaining()
                            + "; drill_head=" + helper.getBlockState(drill)
                            + "; drill_below=" + helper.getBlockState(drill.below())
                            + "; state=" + finalState.getCompound("pumpjack"));
            helper.assertTrue(deposit.remaining() == OilDepositBlockEntity.DEFAULT_RESERVE_MILLIBUCKETS - 1_000,
                    "Pumpjack reserve accounting failed facing " + facing);
            helper.assertTrue(controller.energy().storedWholeJoules() == 3_840,
                    "Pumpjack did not preserve the 8,160 J legacy work budget facing " + facing);
            helper.assertTrue(helper.getBlockState(drill).is(ModAdvancedBlocks.PUMPJACK_DRILL.get())
                            && helper.getBlockState(drill.below()).is(ModAdvancedBlocks.PUMPJACK_DRILL.get()),
                    "Pumpjack did not leave a complete internal drill column facing " + facing);
            controller.unform();
            clear(helper, occupied);
            helper.setBlock(drill, Blocks.AIR);
            helper.setBlock(drill.below(), Blocks.AIR);
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

        List<ItemStack> drops = controllerDrops(helper, CONTROLLER, controller, player);
        List<ItemStack> controllerItems = drops.stream()
                .filter(stack -> stack.is(ModAdvancedBlocks.controller(MultiblockDefinition.CONTAINER).get().asItem()))
                .toList();
        helper.assertTrue(controllerItems.size() == 1, "Container loot did not contain exactly one controller");
        helper.assertTrue(drops.stream()
                        .filter(stack -> stack.is(Items.COBBLESTONE))
                        .mapToInt(ItemStack::getCount)
                        .sum() == 0,
                "Compressed container contents were duplicated as loose item drops");
        ItemStack droppedController = controllerItems.get(0);
        helper.assertTrue(droppedController.getMaxStackSize() == 1,
                "Portable controller contents were stackable");
        CompoundTag blockEntityTag = droppedController.getTagElement("BlockEntityTag");
        helper.assertTrue(blockEntityTag != null, "Controller drop has no BlockEntityTag");
        helper.assertFalse(blockEntityTag.getBoolean("formed"), "Dropped controller remained formed");
        helper.assertFalse(blockEntityTag.getBoolean("mirrored"), "Dropped controller retained mirror state");
        helper.assertFalse(blockEntityTag.contains("owner"), "Dropped controller retained its world owner");
        helper.assertFalse(blockEntityTag.contains("id")
                        || blockEntityTag.contains("x")
                        || blockEntityTag.contains("y")
                        || blockEntityTag.contains("z"),
                "Dropped controller retained world block-entity identity");
        CompoundTag bulk = blockEntityTag.getCompound(MachineBlockEntity.MODULES_TAG)
                .getCompound(Magneticraft.id("advanced_bulk_inventory").toString());
        helper.assertTrue(!bulk.isEmpty(),
                "Bulk inventory module was not stored on the controller item");
        helper.assertTrue(bulk.getInt("amount") == 65_536,
                "Bulk inventory count was not compactly preserved");
        helper.assertTrue(ItemStack.of(bulk.getCompound("type")).is(Items.COBBLESTONE),
                "Bulk inventory type was not compactly preserved");

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

    @GameTest(template = TEMPLATE, timeoutTicks = 240)
    public static void shelvingDropSeparatesStorageAndUpgradeChestsFromControllerNbt(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.SHELVING_UNIT, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Shelving unit did not form");
        helper.assertTrue(controller.shelvingStorage().installChest(new ItemStack(Items.CHEST)),
                "Shelving unit rejected its first chest upgrade");
        helper.assertTrue(controller.shelvingStorage().installChest(new ItemStack(Items.CHEST)),
                "Shelving unit rejected its second chest upgrade");
        helper.assertTrue(controller.shelvingStorage().insertItem(
                0, new ItemStack(Items.IRON_INGOT, 64), false
        ).isEmpty(), "Shelving unit rejected first-layer contents");
        helper.assertTrue(controller.shelvingStorage().insertItem(
                27, new ItemStack(Items.GOLD_INGOT, 7), false
        ).isEmpty(), "Shelving unit rejected second-layer contents");

        List<ItemStack> drops = controllerDrops(helper, CONTROLLER, controller, player);
        List<ItemStack> controllerItems = drops.stream()
                .filter(stack -> stack.is(ModAdvancedBlocks.controller(MultiblockDefinition.SHELVING_UNIT).get().asItem()))
                .toList();
        helper.assertTrue(controllerItems.size() == 1, "Shelving loot did not contain exactly one controller");
        CompoundTag blockEntityTag = controllerItems.get(0).getTagElement("BlockEntityTag");
        helper.assertTrue(blockEntityTag != null, "Shelving controller drop has no BlockEntityTag");
        helper.assertFalse(blockEntityTag.getCompound(MachineBlockEntity.MODULES_TAG)
                        .contains(Magneticraft.id("advanced_shelving_inventory").toString()),
                "Shelving contents were duplicated into the controller NBT");
        helper.assertTrue(countItem(drops, Items.CHEST) == 2,
                "Shelving chest upgrades were not returned exactly once");
        helper.assertTrue(countItem(drops, Items.IRON_INGOT) == 64,
                "Shelving first-layer contents were not returned exactly once");
        helper.assertTrue(countItem(drops, Items.GOLD_INGOT) == 7,
                "Shelving second-layer contents were not returned exactly once");

        BlockPos restoredPosition = CONTROLLER.offset(12, 0, 0);
        helper.setBlock(restoredPosition,
                ModAdvancedBlocks.controller(MultiblockDefinition.SHELVING_UNIT).get().defaultBlockState());
        AdvancedMultiblockBlockEntity restored = requireController(helper, restoredPosition);
        restored.load(blockEntityTag.copy());
        helper.assertTrue(restored.shelvingStorage().installedChests() == 0
                        && restored.shelvingStorage().unlockedSlots() == 0
                        && restored.shelvingStorage().dropContents().isEmpty(),
                "Restored shelving controller retained separated contents");
        clear(helper, occupied);
        helper.setBlock(restoredPosition, Blocks.AIR);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void combustionFluidPortPreservesReleasedBidirectionalTank(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.BIG_COMBUSTION_CHAMBER, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Big combustion chamber did not form");
        MultiblockPortLayout.Port inputPort = MultiblockPortLayout.ports(controller.definition()).stream()
                .filter(port -> port.kind() == MultiblockPortLayout.Kind.FLUID)
                .findFirst()
                .orElseThrow();
        IFluidHandler input = helper.getLevel().getBlockEntity(inputPort.worldPosition(controller))
                .getCapability(ForgeCapabilities.FLUID_HANDLER, inputPort.worldSide(controller.facing()))
                .orElseThrow(AssertionError::new);
        helper.assertTrue(input.fill(
                new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100),
                IFluidHandler.FluidAction.EXECUTE
        ) == 0, "Combustion chamber accepted a fluid with no fuel recipe");
        helper.assertTrue(input.fill(
                new FluidStack(ModFluids.get(FluidDefinition.FUEL).source().get(), 100),
                IFluidHandler.FluidAction.EXECUTE
        ) == 100, "Combustion chamber rejected a generated fluid fuel");
        helper.assertTrue(input.drain(100, IFluidHandler.FluidAction.EXECUTE).getAmount() == 100,
                "Combustion chamber did not preserve its released bidirectional fluid tank");
        clear(helper, occupied);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void stirlingGeneratorConservesHeatAndStopsWhenItsOutputIsFull(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        List<BlockPos> occupied = build(
                helper, MultiblockDefinition.STIRLING_GENERATOR, CONTROLLER, Direction.NORTH, false
        );
        AdvancedMultiblockBlockEntity controller = requireController(helper, CONTROLLER);
        helper.assertTrue(controller.tryForm(player), "Stirling generator did not form");
        helper.runAfterDelay(2, () -> {
            controller.energy().setStoredJoules(0.0D);
            controller.heat().node().setTemperature(HeatNode.AMBIENT_TEMPERATURE_KELVIN);
            controller.inventory().setStackInSlot(0, new ItemStack(Items.COAL));
            double ambientHeat = controller.heat().node().internalEnergyJoules();
            helper.assertTrue(controller.operational(), "Stirling generator was not operational after formation");
            helper.assertTrue(controller.energy().electricalControllerBound(),
                    "Stirling generator did not bind its electrical profile");
            helper.assertTrue(controller.energy().ratedCapacityWholeJoules() == 16_000,
                    "Stirling generator buffer was not 16 kJ; actual="
                            + controller.energy().ratedCapacityWholeJoules());
            helper.assertTrue(Math.abs(controller.energy().generateJoules(120.0D, true) - 120.0D) < 1.0E-6D,
                    "Stirling generator simulation rejected its 120 J output; accepted="
                            + controller.energy().generateJoules(120.0D, true));
            helper.assertTrue(!controller.inventory().getStackInSlot(0).isEmpty(),
                    "Stirling generator rejected coal from its fuel slot");

            AdvancedMultiblockBlockEntity.serverTick(
                    helper.getLevel(), controller.getBlockPos(), controller.getBlockState(), controller
            );

            helper.assertTrue(Math.abs(controller.energy().storedJoules() - 120.0D) < 1.0E-6D,
                    "Stirling generator did not cap electrical production at 120 J/t; actual="
                            + controller.energy().storedJoules());
            helper.assertTrue(Math.abs(controller.heat().node().internalEnergyJoules() - ambientHeat) < 1.0E-6D,
                    "Stirling generator did not convert 160 J heat into 120 J electricity at 75%");
            helper.assertTrue(controller.inventory().getStackInSlot(0).isEmpty(),
                    "Stirling generator did not consume exactly one solid fuel item");
            int remainingBurn = controller.burnTicks();

            controller.energy().setStoredJoules(controller.energy().ratedCapacityJoules());
            AdvancedMultiblockBlockEntity.serverTick(
                    helper.getLevel(), controller.getBlockPos(), controller.getBlockState(), controller
            );
            helper.assertTrue(controller.burnTicks() == remainingBurn,
                    "Blocked Stirling output continued consuming solid fuel work");
            helper.assertTrue(Math.abs(controller.heat().node().internalEnergyJoules() - ambientHeat) < 1.0E-6D,
                    "Blocked Stirling output continued extracting or producing heat");
            clear(helper, occupied);
            player.discard();
            helper.succeed();
        });
    }

    private static List<ItemStack> controllerDrops(
            GameTestHelper helper,
            BlockPos position,
            AdvancedMultiblockBlockEntity controller,
            Player player
    ) {
        LootParams.Builder loot = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(position)))
                .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                .withOptionalParameter(LootContextParams.BLOCK_ENTITY, controller)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player);
        return controller.getBlockState().getDrops(loot);
    }

    private static int countItem(List<ItemStack> drops, net.minecraft.world.item.Item item) {
        return drops.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
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
