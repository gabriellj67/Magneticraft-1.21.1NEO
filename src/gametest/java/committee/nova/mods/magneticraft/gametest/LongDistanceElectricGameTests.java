package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.item.CopperWireCoilItem;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlock;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricConnectorBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.PoleSegment;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.WirelessEnergyReceiverBlockEntity;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Runtime contracts for long-distance electricity, Tesla transfer and wind generation. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LongDistanceElectricGameTests {
    private static final String TEMPLATE = "base_content";
    private static final String ADVANCED_TEMPLATE = "advanced_systems";

    private LongDistanceElectricGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void windTurbinePreservesReleasedAllSideElectricalPorts(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 1, 1);
        helper.setBlock(position, ModNetworkBlocks.WIND_TURBINE.get().defaultBlockState()
                .setValue(WindTurbineBlock.FACING, Direction.NORTH));
        WindTurbineBlockEntity turbine = (WindTurbineBlockEntity) helper.getBlockEntity(position);
        for (Direction side : Direction.values()) {
            helper.assertTrue(turbine.electricity().connectionSides().contains(side),
                    "Wind turbine lost its released electrical port on " + side.getName());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void restoredBlocksAndBlockEntitiesAreRegistered(GameTestHelper helper) {
        assertBlockId(helper, "electric_connector", ModNetworkBlocks.ELECTRIC_CONNECTOR.get());
        assertBlockId(helper, "electric_pole", ModNetworkBlocks.ELECTRIC_POLE.get());
        assertBlockId(helper, "electric_pole_transformer", ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get());
        assertBlockId(helper, "tesla_tower", ModNetworkBlocks.TESLA_TOWER.get());
        assertBlockId(helper, "wireless_energy_receiver", ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get());
        assertBlockId(helper, "wind_turbine", ModNetworkBlocks.WIND_TURBINE.get());

        helper.assertTrue(
                "electric_connector".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.ELECTRIC_CONNECTOR.get()).getPath()),
                "Electric connector block entity has the wrong registry id"
        );
        helper.assertTrue(
                "electric_pole".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.ELECTRIC_POLE.get()).getPath()),
                "Electric pole block entity has the wrong registry id"
        );
        helper.assertTrue(
                "electric_pole_transformer".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.ELECTRIC_POLE_TRANSFORMER.get()).getPath()),
                "Transformer pole block entity has the wrong registry id"
        );
        helper.assertTrue(
                "tesla_tower".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.TESLA_TOWER.get()).getPath()),
                "Tesla tower block entity has the wrong registry id"
        );
        helper.assertTrue(
                "wireless_energy_receiver".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.WIRELESS_ENERGY_RECEIVER.get()).getPath()),
                "Wireless receiver block entity has the wrong registry id"
        );
        helper.assertTrue(
                "wind_turbine".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.WIND_TURBINE.get()).getPath()),
                "Wind turbine block entity has the wrong registry id"
        );

        require(helper, new BlockPos(1, 4, 1), ElectricConnectorBlockEntity.class,
                () -> placeConnector(helper, new BlockPos(1, 4, 1)));
        placePole(helper, new BlockPos(20, 8, 1), false);
        placePole(helper, new BlockPos(40, 8, 1), true);
        require(helper, new BlockPos(1, 4, 5), TeslaTowerBlockEntity.class,
                () -> helper.setBlock(new BlockPos(1, 4, 5), ModNetworkBlocks.TESLA_TOWER.get()));
        require(helper, new BlockPos(5, 4, 5), WirelessEnergyReceiverBlockEntity.class,
                () -> placeReceiver(helper, new BlockPos(5, 4, 5)));
        require(helper, new BlockPos(9, 12, 5), WindTurbineBlockEntity.class,
                () -> helper.setBlock(new BlockPos(9, 12, 5), ModNetworkBlocks.WIND_TURBINE.get()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void connectorMountsOnCableAndJoinsThroughBackFace(GameTestHelper helper) {
        BlockPos cablePosition = new BlockPos(4, 4, 4);
        BlockPos connectorPosition = cablePosition.relative(Direction.NORTH);
        BlockPos cableWorldPosition = helper.absolutePos(cablePosition);
        BlockPos connectorWorldPosition = helper.absolutePos(connectorPosition);
        helper.setBlock(cablePosition, ModNetworkBlocks.ELECTRIC_CABLE.get());

        Player player = helper.makeMockSurvivalPlayer();
        ItemStack connectorStack = new ItemStack(ModNetworkBlocks.ELECTRIC_CONNECTOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, connectorStack);
        InteractionResult placement = connectorStack.getItem().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(cableWorldPosition).add(0.0D, 0.0D, -0.5D),
                        Direction.NORTH,
                        cableWorldPosition,
                        false
                )
        ));
        helper.assertTrue(placement.consumesAction(), "Electric connector could not be placed on an electric cable");
        helper.assertBlockPresent(ModNetworkBlocks.ELECTRIC_CONNECTOR.get(), connectorPosition);

        ElectricCableBlockEntity cable = require(helper, cablePosition, ElectricCableBlockEntity.class);
        ElectricConnectorBlockEntity connector = require(helper, connectorPosition, ElectricConnectorBlockEntity.class);
        helper.assertTrue(connector.getBlockState().getValue(BlockStateProperties.FACING) == Direction.NORTH,
                "Cable-mounted connector did not face away from its support");
        helper.assertTrue(connector.electricity().isSideEnabled(Direction.SOUTH),
                "Cable-mounted connector did not expose its electrical back face");
        helper.assertFalse(connector.electricity().isSideEnabled(Direction.NORTH),
                "Cable-mounted connector exposed electricity on its outward face");

        double cableShapeMinZ = helper.getLevel().getBlockState(cableWorldPosition)
                .getShape(helper.getLevel(), cableWorldPosition, CollisionContext.empty())
                .bounds().minZ;
        helper.assertTrue(Math.abs(cableShapeMinZ) < 1.0E-9D,
                "Electric cable did not render an arm toward the connector");

        helper.runAfterDelay(2, () -> {
            var manager = PhysicalNetworkService.manager(helper.getLevel());
            helper.assertTrue(manager.neighbors(NetworkDomain.ELECTRICITY, connectorWorldPosition)
                            .contains(cableWorldPosition.asLong()),
                    "Electric connector did not join the adjacent cable through its back face");
            connector.electricity().node().setVoltage(120.0D);
            cable.electricity().node().setVoltage(0.0D);
            manager.tick(helper.getLevel().getGameTime() + 1_000L);
            helper.assertTrue(cable.electricity().node().energyJoules() > 0.0D,
                    "Electric connector did not transfer energy into its supporting cable");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void copperWireCoilConnectsAtEightBlocksAndRejectsNine(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 4, 2);
        BlockPos atLimit = new BlockPos(9, 4, 2);
        BlockPos tooFar = new BlockPos(10, 4, 2);
        placeConnector(helper, first);
        placeConnector(helper, atLimit);
        placeConnector(helper, tooFar);

        Player player = helper.makeMockSurvivalPlayer();
        ItemStack coilStack = new ItemStack(ModNetworkItems.COPPER_WIRE_COIL.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, coilStack);
        CopperWireCoilItem coil = (CopperWireCoilItem) coilStack.getItem();

        player.setShiftKeyDown(true);
        InteractionResult selected = coil.useOn(useContext(helper, player, first));
        helper.assertTrue(selected.consumesAction(), "Copper wire coil did not store its first endpoint");
        player.setShiftKeyDown(false);
        InteractionResult connected = coil.useOn(useContext(helper, player, atLimit));
        helper.assertTrue(connected.consumesAction(), "Copper wire coil did not handle its second endpoint");

        LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
        BlockPos firstWorld = helper.absolutePos(first);
        BlockPos limitWorld = helper.absolutePos(atLimit);
        helper.assertTrue(service.connectionsAt(firstWorld).stream()
                        .anyMatch(connection -> connection.containsPosition(limitWorld)),
                "Connector wire did not persist at the eight-block limit");
        helper.assertTrue(service.connect(firstWorld, helper.absolutePos(tooFar)).result()
                        == LongDistanceElectricityService.ConnectionResult.TOO_FAR,
                "Connector wire accepted a nine-block span");
        helper.assertTrue(coilStack.getCount() == 1, "Reusable copper wire coil was consumed");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void polesUseSixteenBlockRangeAndTransformerBridgesPortDomains(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 8, 1);
        BlockPos atLimit = new BlockPos(17, 8, 1);
        BlockPos tooFar = new BlockPos(18, 8, 1);
        LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
        service.removeConnectionsAt(helper.absolutePos(first));
        service.removeConnectionsAt(helper.absolutePos(atLimit));
        service.removeConnectionsAt(helper.absolutePos(tooFar));
        placePole(helper, first, false);
        placePole(helper, atLimit, false);

        BlockPos firstWorld = helper.absolutePos(first);
        BlockPos limitWorld = helper.absolutePos(atLimit);
        helper.assertTrue(service.connectionsAt(firstWorld).stream()
                        .anyMatch(connection -> connection.containsPosition(limitWorld)),
                "Electric poles did not auto-connect at sixteen blocks");
        helper.assertTrue(service.removeConnectionsAt(firstWorld) >= 1,
                "Pole fixture did not remove its automatic connection");
        helper.assertTrue(service.connect(firstWorld, limitWorld).result()
                        == LongDistanceElectricityService.ConnectionResult.SUCCESS,
                "Electric pole wire rejected a sixteen-block span");
        placePole(helper, tooFar, false);
        helper.assertTrue(service.connect(firstWorld, helper.absolutePos(tooFar)).result()
                        == LongDistanceElectricityService.ConnectionResult.TOO_FAR,
                "Electric pole wire accepted a seventeen-block span");

        BlockPos connector = new BlockPos(3, 8, 30);
        BlockPos transformer = new BlockPos(5, 8, 30);
        BlockPos pole = new BlockPos(7, 8, 30);
        service.removeConnectionsAt(helper.absolutePos(connector));
        service.removeConnectionsAt(helper.absolutePos(transformer));
        service.removeConnectionsAt(helper.absolutePos(pole));
        placeConnector(helper, connector);
        placePole(helper, pole, false);
        placePole(helper, transformer, true);
        helper.assertTrue(service.connect(helper.absolutePos(connector), helper.absolutePos(pole)).result()
                        == LongDistanceElectricityService.ConnectionResult.INCOMPATIBLE_PORT,
                "Connector wire directly connected to a three-wire pole port");
        helper.assertTrue(service.connect(helper.absolutePos(connector), helper.absolutePos(transformer)).result()
                        == LongDistanceElectricityService.ConnectionResult.SUCCESS,
                "Transformer pole did not accept the connector wire domain");
        BlockPos transformerWorld = helper.absolutePos(transformer);
        LongDistanceElectricityService.ConnectionResult poleBridge = service.connect(
                transformerWorld,
                helper.absolutePos(pole)
        ).result();
        helper.assertTrue(
                poleBridge == LongDistanceElectricityService.ConnectionResult.SUCCESS
                        || poleBridge == LongDistanceElectricityService.ConnectionResult.ALREADY_CONNECTED,
                "Transformer pole did not accept the pole wire domain"
        );
        helper.assertTrue(service.connectionsAt(transformerWorld).stream()
                        .anyMatch(connection -> connection.containsPosition(helper.absolutePos(connector))),
                "Transformer pole did not retain its connector edge");
        helper.assertTrue(service.connectionsAt(transformerWorld).stream()
                        .anyMatch(connection -> connection.containsPosition(helper.absolutePos(pole))),
                "Transformer pole did not retain its pole edge");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void repeatedTransformerConnectionPrefersExistingPoleEdgeOverConnectorDistance(GameTestHelper helper) {
        BlockPos first = new BlockPos(2, 8, 2);
        BlockPos second = new BlockPos(14, 8, 2);
        LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
        BlockPos firstWorld = helper.absolutePos(first);
        BlockPos secondWorld = helper.absolutePos(second);
        service.removeConnectionsAt(firstWorld);
        service.removeConnectionsAt(secondWorld);
        placePole(helper, first, true);
        placePole(helper, second, true);

        LongDistanceElectricityService.ConnectionResult initial = service.connect(firstWorld, secondWorld).result();
        helper.assertTrue(
                initial == LongDistanceElectricityService.ConnectionResult.SUCCESS
                        || initial == LongDistanceElectricityService.ConnectionResult.ALREADY_CONNECTED,
                "Transformer fixtures could not establish their twelve-block pole edge"
        );
        helper.assertTrue(
                service.connect(firstWorld, secondWorld).result()
                        == LongDistanceElectricityService.ConnectionResult.ALREADY_CONNECTED,
                "Existing pole edge did not take priority over the out-of-range connector port"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wireTransferPausesOnUnloadResumesAndCleansOnBreak(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 4, 2);
        BlockPos second = new BlockPos(9, 4, 2);
        ElectricConnectorBlockEntity source = placeConnector(helper, first);
        ElectricConnectorBlockEntity target = placeConnector(helper, second);
        LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
        helper.assertTrue(service.connect(helper.absolutePos(first), helper.absolutePos(second)).result()
                        == LongDistanceElectricityService.ConnectionResult.SUCCESS,
                "Connector fixture could not create its wire");

        source.electricity().node().setVoltage(120.0D);
        target.electricity().node().setVoltage(0.0D);
        double energyBefore = electricalEnergy(source, target);
        service.tick(helper.getLevel().getGameTime() + 1_000L);
        double energyAfter = electricalEnergy(source, target);
        helper.assertTrue(source.electricity().node().energyJoules() < energyBefore,
                "Long-distance source did not discharge");
        helper.assertTrue(target.electricity().node().energyJoules() > 0.0D,
                "Long-distance target did not receive energy");
        helper.assertTrue(energyAfter <= energyBefore + 1.0E-6D,
                "Long-distance transfer created energy");

        source.electricity().node().setVoltage(120.0D);
        target.electricity().node().setVoltage(0.0D);
        target.setRemoved();
        double pausedBefore = electricalEnergy(source, target);
        service.tick(helper.getLevel().getGameTime() + 1_001L);
        helper.assertTrue(Math.abs(electricalEnergy(source, target) - pausedBefore) < 1.0E-6D,
                "Unloaded long-distance endpoint did not pause its edge");
        helper.assertTrue(service.hasConnections(helper.absolutePos(first)),
                "Runtime endpoint unload deleted the durable edge");

        target.clearRemoved();
        target.onLoad();
        service.tick(helper.getLevel().getGameTime() + 1_002L);
        helper.assertTrue(target.electricity().node().energyJoules() > 0.0D,
                "Reloaded long-distance endpoint did not resume transfer");
        helper.destroyBlock(first);
        helper.assertFalse(service.hasConnections(helper.absolutePos(second)),
                "Breaking an endpoint left a durable wire edge");
        helper.succeed();
    }

    @GameTest(template = ADVANCED_TEMPLATE)
    public static void teslaTowerHonorsSixtyVoltThresholdAndFiveHundredRate(GameTestHelper helper) {
        BlockPos towerPosition = new BlockPos(20, 5, 20);
        BlockPos receiverPosition = new BlockPos(24, 5, 20);
        helper.setBlock(towerPosition, ModNetworkBlocks.TESLA_TOWER.get());
        TeslaTowerBlockEntity tower = require(helper, towerPosition, TeslaTowerBlockEntity.class);
        WirelessEnergyReceiverBlockEntity receiver = placeReceiver(helper, receiverPosition);

        helper.runAfterDelay(2, () -> {
            tower.electricity().node().setVoltage(59.0D);
            tower.serverTick();
            helper.assertTrue(receiver.electricity().node().energyJoules() == 0.0D,
                    "Tesla tower transferred below sixty volts");

            tower.electricity().node().setVoltage(60.0D);
            double sourceBefore = tower.electricity().node().energyJoules();
            tower.serverTick();
            helper.assertTrue(Math.abs(receiver.electricity().node().energyJoules() - 500.0D) < 1.0E-6D,
                    "Tesla receiver did not receive exactly 500 J in one tick");
            helper.assertTrue(
                    Math.abs(tower.electricity().node().energyJoules() - (sourceBefore - 500.0D)) < 1.0E-6D,
                    "Tesla transfer did not conserve its 1 J to 1 J boundary"
            );
            helper.succeed();
        });
    }

    @GameTest(template = ADVANCED_TEMPLATE, timeoutTicks = 40)
    public static void windTurbineStopsForBlockedRotorAndProducesBoundedPowerWhenOpen(GameTestHelper helper) {
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        BlockPos turbinePosition = new BlockPos(20, 128 - templateOrigin.getY(), 20);
        Direction facing = Direction.EAST;
        try {
            clearWindTurbineTestArea(helper, turbinePosition, facing);
            helper.setBlock(
                    turbinePosition,
                    ModNetworkBlocks.WIND_TURBINE.get().defaultBlockState().setValue(WindTurbineBlock.FACING, facing)
            );
            WindTurbineBlockEntity turbine = require(helper, turbinePosition, WindTurbineBlockEntity.class);
            Direction horizontal = facing.getClockWise();
            BlockPos planeCenter = turbinePosition.relative(facing);
            BlockPos obstruction = planeCenter.relative(horizontal, -5).below(3);
            helper.setBlock(obstruction, Blocks.STONE);

            turbine.wind().load(new CompoundTag());
            turbine.serverTick();
            helper.assertFalse(turbine.wind().operational(), "Blocked wind-turbine rotor remained operational");
            helper.assertTrue(turbine.wind().productionJoulesPerTick() == 0.0D,
                    "Blocked wind turbine generated energy");

            helper.setBlock(obstruction, Blocks.AIR);
            CompoundTag wind = new CompoundTag();
            wind.putDouble("current_wind", 1.0D);
            wind.putDouble("target_wind", 1.0D);
            turbine.wind().load(wind);
            turbine.serverTick();

            helper.assertTrue(turbine.wind().operational(), "Open wind-turbine rotor did not become operational");
            helper.assertTrue(turbine.wind().openSpace() >= 0.0D && turbine.wind().openSpace() <= 1.0D,
                    "Wind-turbine open-space factor escaped its bounded range");
            helper.assertTrue(turbine.wind().productionJoulesPerTick() > 0.0D,
                    "Open wind turbine did not generate energy");
            helper.assertTrue(turbine.wind().productionJoulesPerTick() <= 200.0D,
                    "Wind turbine exceeded its 200 J/t rating");
        } finally {
            clearWindTurbineTestArea(helper, turbinePosition, facing);
        }
        helper.succeed();
    }

    private static void clearWindTurbineTestArea(
            GameTestHelper helper,
            BlockPos turbinePosition,
            Direction facing
    ) {
        helper.setBlock(turbinePosition, Blocks.AIR);
        Direction horizontal = facing.getClockWise();
        BlockPos planeCenter = turbinePosition.relative(facing);
        for (int horizontalOffset = -5; horizontalOffset <= 5; horizontalOffset++) {
            for (int verticalOffset = -5; verticalOffset <= 5; verticalOffset++) {
                BlockPos bladePosition = planeCenter
                        .relative(horizontal, horizontalOffset)
                        .above(verticalOffset);
                for (int depth = 0; depth <= 16; depth++) {
                    helper.setBlock(bladePosition.relative(facing, depth), Blocks.AIR);
                }
            }
        }
    }

    private static UseOnContext useContext(GameTestHelper helper, Player player, BlockPos position) {
        BlockPos worldPosition = helper.absolutePos(position);
        return new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(worldPosition), Direction.UP, worldPosition, false)
        );
    }

    private static ElectricConnectorBlockEntity placeConnector(GameTestHelper helper, BlockPos position) {
        helper.setBlock(position.relative(Direction.SOUTH), Blocks.STONE);
        helper.setBlock(position, ModNetworkBlocks.ELECTRIC_CONNECTOR.get());
        return require(helper, position, ElectricConnectorBlockEntity.class);
    }

    private static ElectricPoleBlockEntity placePole(GameTestHelper helper, BlockPos position, boolean transformer) {
        var block = transformer
                ? ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get()
                : ModNetworkBlocks.ELECTRIC_POLE.get();
        helper.setBlock(
                position,
                block.defaultBlockState().setValue(ElectricPoleBlock.SEGMENT, PoleSegment.BASE)
        );
        return require(helper, position, ElectricPoleBlockEntity.class);
    }

    private static WirelessEnergyReceiverBlockEntity placeReceiver(GameTestHelper helper, BlockPos position) {
        helper.setBlock(position.relative(Direction.SOUTH), Blocks.STONE);
        helper.setBlock(position, ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get());
        return require(helper, position, WirelessEnergyReceiverBlockEntity.class);
    }

    private static double electricalEnergy(
            ElectricConnectorBlockEntity first,
            ElectricConnectorBlockEntity second
    ) {
        return first.electricity().node().energyJoules() + second.electricity().node().energyJoules();
    }

    private static void assertBlockId(
            GameTestHelper helper,
            String expected,
            net.minecraft.world.level.block.Block block
    ) {
        helper.assertTrue(expected.equals(ForgeRegistries.BLOCKS.getKey(block).getPath()),
                "Block has the wrong registry id: " + expected);
    }

    private static <T extends BlockEntity> T require(
            GameTestHelper helper,
            BlockPos position,
            Class<T> type,
            Runnable placement
    ) {
        placement.run();
        return require(helper, position, type);
    }

    private static <T extends BlockEntity> T require(GameTestHelper helper, BlockPos position, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!type.isInstance(blockEntity)) {
            throw new AssertionError("Expected " + type.getSimpleName() + " at " + position + ", got " + blockEntity);
        }
        return type.cast(blockEntity);
    }
}
