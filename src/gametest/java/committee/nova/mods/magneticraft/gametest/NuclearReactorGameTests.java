package committee.nova.mods.magneticraft.gametest;

import com.mojang.authlib.GameProfile;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearControllerUpgrade;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorAction;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorColumnBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorPortBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorOperatingState;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorInterlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorControlMode;
import committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorAutomationLevel;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorStructure;
import committee.nova.mods.magneticraft.content.nuclear.structure.NuclearStructureState;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.init.ModNuclearItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Loaded-world proof for manual formation and periodic structural invalidation. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearReactorGameTests {
    private static final String TEMPLATE = "base_content";

    private NuclearReactorGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void variableReactorFormsThenUnformsAfterActuatorDamage(GameTestHelper helper) {
        int width = 7;
        int length = 7;
        int height = 7;
        Direction facing = Direction.NORTH;
        BlockPos controllerPosition = helper.absolutePos(new BlockPos(8, 2, 8));
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = columns(width, length);
        buildReactor(helper, controllerPosition, width, length, height, facing, columns);
        NuclearReactorControllerBlockEntity controller =
                (NuclearReactorControllerBlockEntity) helper.getLevel().getBlockEntity(controllerPosition);
        ServerPlayer operator = operator(helper, controllerPosition);
        helper.assertTrue(controller.tryForm(operator), "Complete variable reactor did not form");
        helper.assertTrue(controller.snapshot().orElseThrow().columns().size() == 9,
                "Reactor snapshot did not retain all logical columns");
        helper.assertTrue(controller.snapshot().orElseThrow().ports().size() == 4,
                "Reactor snapshot did not retain all public ports");
        helper.assertTrue(controller.estimate().powerDensityJoulesPerTick() > 0.0D,
                "Formed reactor did not calculate a static layout preview");

        BlockPos casing = NuclearReactorStructure.worldPosition(
                controllerPosition, facing, width, 0, 0, 0);
        helper.assertTrue(helper.getLevel().getBlockState(casing).getValue(NuclearStructureState.FORMED),
                "Formed reactor casing did not enter the projected-model state");
        BlockPos coolantPort = controller.snapshot().orElseThrow().ports().get(NuclearReactorPortType.COOLANT_INPUT);
        helper.assertTrue(helper.getLevel().getBlockState(coolantPort).getValue(NuclearStructureState.FORMED),
                "Formed reactor port did not enter the projected-model state");
        helper.assertTrue(helper.getLevel().getBlockEntity(coolantPort) instanceof NuclearReactorPortBlockEntity,
                "Projected reactor port lost its capability-providing block entity");

        ReactorColumnCoordinate control = new ReactorColumnCoordinate(1, 0);
        BlockPos actuator = NuclearReactorStructure.worldPosition(
                controllerPosition, facing, width, control.x() + 2, height - 1, control.z() + 2);
        helper.getLevel().destroyBlock(actuator, false);
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(!controller.formed(), "Damaged reactor remained formed after periodic validation");
            helper.assertTrue(!helper.getLevel().getBlockState(controllerPosition)
                            .getValue(NuclearReactorControllerBlock.FORMED),
                    "Controller block state remained formed after structure damage");
            helper.assertTrue(!helper.getLevel().getBlockState(casing).getValue(NuclearStructureState.FORMED),
                    "Reactor casing remained hidden after the structure unformed");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 160)
    public static void reactorControlsConsumeNativeJAndStationLossScramsWithoutErasingFuel(GameTestHelper helper) {
        int width = 7;
        int length = 7;
        int height = 7;
        Direction facing = Direction.NORTH;
        BlockPos controllerPosition = helper.absolutePos(new BlockPos(8, 2, 8));
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> layout = columns(width, length);
        buildReactor(helper, controllerPosition, width, length, height, facing, layout);
        NuclearReactorControllerBlockEntity controller =
                (NuclearReactorControllerBlockEntity) helper.getLevel().getBlockEntity(controllerPosition);
        ServerPlayer owner = operator(helper, controllerPosition);
        helper.assertTrue(controller.tryForm(owner), "Complete variable reactor did not form");
        helper.assertTrue(controller.automationLevel() == ReactorAutomationLevel.PROTECTION
                        && controller.controlMode() == ReactorControlMode.POWER,
                "Fresh reactor did not start with built-in protection and power regulation");

        BlockPos electricalPosition = controller.snapshot().orElseThrow().ports()
                .get(committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType.ELECTRICAL);
        helper.assertTrue(helper.getLevel().getBlockEntity(electricalPosition)
                        instanceof NuclearReactorPortBlockEntity,
                "Claimed electrical penetration has no reactor port entity");
        NuclearReactorPortBlockEntity electrical =
                (NuclearReactorPortBlockEntity) helper.getLevel().getBlockEntity(electricalPosition);
        helper.assertTrue(electrical.electricity() != null,
                "Electrical penetration did not expose the native-J module");
        electrical.electricity().node().setEnergyJoules(20_000.0D);

        ReactorColumnCoordinate fuelCoordinate = new ReactorColumnCoordinate(0, 0);
        owner.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModNuclearItems.fuelAssembly(NuclearFuelGrade.STANDARD_ENRICHMENT).get()));
        helper.assertTrue(controller.applyAction(owner, new NuclearReactorAction(
                        NuclearReactorAction.Type.LOAD_FUEL_FROM_HAND,
                        null, null, fuelCoordinate, 0, false)),
                "Owner could not load a matching stateful fuel assembly");
        owner.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModNuclearItems.controllerUpgrade(NuclearControllerUpgrade.REGULATION).get()));
        helper.assertTrue(controller.applyAction(owner,
                        NuclearReactorAction.simple(NuclearReactorAction.Type.INSTALL_UPGRADE)),
                "Closed-loop regulation upgrade was not installed");
        helper.assertTrue(controller.applyAction(owner, new NuclearReactorAction(
                        NuclearReactorAction.Type.SET_MODE,
                        null, ReactorControlMode.TEMPERATURE, null, 0, false)),
                "Regulation upgrade did not retain the engineering temperature mode");
        helper.assertTrue(controller.applyAction(owner, new NuclearReactorAction(
                        NuclearReactorAction.Type.SET_MODE,
                        null, ReactorControlMode.POWER, null, 0, false)),
                "Controller did not return to its basic power mode");

        double requiredFlow = controller.estimate().requiredCoolantFlowMilliBucketsPerTick();
        helper.onEachTick(() -> controller.reportCoolantFlow(requiredFlow));
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(controller.stationPowerAvailable(),
                    "Charged electrical port did not supply reactor station service");
            ServerPlayer intruder = operator(helper, controllerPosition);
            helper.assertTrue(!controller.applyAction(intruder,
                            NuclearReactorAction.simple(NuclearReactorAction.Type.START)),
                    "Non-owner bypassed reactor control permissions");
            helper.assertTrue(controller.applyAction(owner, new NuclearReactorAction(
                            NuclearReactorAction.Type.SET_OVERRIDE, null, null, null, 1, false)),
                    "Engineering override confirmation was not armed");
            helper.assertTrue(!controller.engineeringOverride(),
                    "Engineering override enabled without the second confirmation");
            helper.assertTrue(controller.applyAction(owner, new NuclearReactorAction(
                            NuclearReactorAction.Type.SET_OVERRIDE, null, null, null, 1, true)),
                    "Owner confirmation did not enable engineering override");
            helper.assertTrue(controller.lastOverrideOperator().orElseThrow().equals(owner.getUUID()),
                    "Confirmed engineering override did not retain its operator audit record");
            helper.assertTrue(controller.applyAction(owner, new NuclearReactorAction(
                            NuclearReactorAction.Type.SET_ROD_GROUP,
                            ReactorRodGroup.A, null, null, 500, false)),
                    "Rod group A did not accept its independent insertion command");
            helper.assertTrue(controller.applyAction(owner,
                            NuclearReactorAction.simple(NuclearReactorAction.Type.START)),
                    "Powered and cooled reactor did not enter startup");

            helper.runAfterDelay(10, () -> {
                double burnup = controller.fuelState(fuelCoordinate).orElseThrow().burnupFraction();
                helper.assertTrue(burnup > 0.0D, "Running reactor did not advance per-column burnup");
                helper.assertTrue(controller.runtime().decayHeatJoulesPerTick() > 0.0D,
                        "Running reactor did not accumulate durable decay heat");
                helper.assertTrue(controller.rodInsertion(ReactorRodGroup.A) < 0.5D,
                        "Closed-loop power control did not adjust the commanded rod group");
                electrical.electricity().node().setEnergyJoules(0.0D);
                helper.runAfterDelay(2, () -> {
                    helper.assertTrue(controller.operatingState() == ReactorOperatingState.SCRAMMED,
                            "Station-service loss did not cause an immediate SCRAM");
                    helper.assertTrue("station_power".equals(controller.scramReason()),
                            "Station-service SCRAM did not retain its deterministic reason");
                    helper.assertTrue(!controller.engineeringOverride(),
                            "SCRAM did not clear the visible engineering override");
                    helper.assertTrue(controller.rodInsertion(ReactorRodGroup.A) == 1.0D,
                            "SCRAM did not fully insert rod group A");
                    helper.assertTrue(controller.fuelState(fuelCoordinate).orElseThrow()
                                    .burnupFraction() >= burnup,
                            "SCRAM erased the loaded assembly burnup state");
                    helper.assertTrue(controller.runtime().decayHeatJoulesPerTick() > 0.0D,
                            "SCRAM incorrectly erased decay heat");
                    helper.succeed();
                });
            });
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void reactorRuntimeNbtRoundTripsAndFutureSchemaFailsSafeWithoutDataLoss(GameTestHelper helper) {
        int width = 7;
        int length = 7;
        int height = 7;
        Direction facing = Direction.NORTH;
        BlockPos controllerPosition = helper.absolutePos(new BlockPos(8, 2, 8));
        buildReactor(helper, controllerPosition, width, length, height, facing, columns(width, length));
        NuclearReactorControllerBlockEntity controller =
                (NuclearReactorControllerBlockEntity) helper.getLevel().getBlockEntity(controllerPosition);
        ServerPlayer owner = operator(helper, controllerPosition);
        helper.assertTrue(controller.tryForm(owner), "Complete variable reactor did not form");

        ReactorColumnCoordinate coordinate = new ReactorColumnCoordinate(0, 0);
        owner.setItemInHand(InteractionHand.MAIN_HAND,
                new ItemStack(ModNuclearItems.fuelAssembly(NuclearFuelGrade.STANDARD_ENRICHMENT).get()));
        helper.assertTrue(controller.applyAction(owner, new NuclearReactorAction(
                        NuclearReactorAction.Type.LOAD_FUEL_FROM_HAND,
                        null, null, coordinate, 0, false)),
                "Matching assembly did not load before NBT round-trip");
        helper.assertTrue(controller.applyAction(owner, new NuclearReactorAction(
                        NuclearReactorAction.Type.SET_ROD_GROUP,
                        ReactorRodGroup.A, null, null, 375, false)),
                "Rod group did not accept pre-save insertion state");
        CompoundTag saved = controller.saveWithFullMetadata();
        controller.load(saved);
        helper.assertTrue(controller.formed(), "Valid NBT round-trip lost the immutable structure snapshot");
        helper.assertTrue(controller.fuelState(coordinate).isPresent(),
                "Valid NBT round-trip lost the loaded fuel column");
        helper.assertTrue(Math.abs(controller.rodInsertion(ReactorRodGroup.A) - 0.375D) < 1.0E-9D,
                "Valid NBT round-trip changed rod group A insertion");
        helper.assertTrue(controller.controlMode() == ReactorControlMode.POWER
                        && controller.automationLevel() == ReactorAutomationLevel.PROTECTION,
                "Valid NBT round-trip changed the built-in basic control defaults");

        CompoundTag noRuntime = saved.copy();
        noRuntime.remove("reactor_runtime");
        controller.load(noRuntime);
        helper.assertTrue(controller.controlMode() == ReactorControlMode.POWER
                        && controller.automationLevel() == ReactorAutomationLevel.PROTECTION,
                "Missing runtime data did not restore the documented fresh-controller defaults");

        CompoundTag legacy = saved.copy();
        CompoundTag legacyRuntime = legacy.getCompound("reactor_runtime");
        legacyRuntime.putInt("schema_version", 1);
        legacyRuntime.remove("accident_stage");
        legacyRuntime.remove("accident_reason");
        legacyRuntime.remove("core_pressure_megapascals");
        legacyRuntime.remove("vessel_integrity");
        legacyRuntime.remove("containment_integrity");
        legacyRuntime.remove("accident_energy_joules");
        legacyRuntime.remove("last_accident_transition_game_time");
        legacyRuntime.remove("terrain_damage_applied");
        legacy.put("reactor_runtime", legacyRuntime);
        controller.load(legacy);
        CompoundTag migrated = controller.saveWithFullMetadata().getCompound("reactor_runtime");
        helper.assertTrue(migrated.getInt("schema_version") == 2
                        && controller.accidentStage() == committee.nova.mods.magneticraft.content.nuclear.reactor.ReactorAccidentStage.NORMAL,
                "Runtime schema 1 did not migrate once into the safe schema 2 defaults");

        CompoundTag future = saved.copy();
        CompoundTag futureRuntime = future.getCompound("reactor_runtime");
        futureRuntime.putInt("schema_version", 3);
        futureRuntime.putString("future_marker", "preserve-me");
        future.put("reactor_runtime", futureRuntime);
        controller.load(future);
        CompoundTag preserved = controller.saveWithFullMetadata().getCompound("reactor_runtime");
        helper.assertTrue(controller.operatingState() == ReactorOperatingState.SCRAMMED,
                "Unknown runtime schema did not fail into a safe SCRAM");
        helper.assertTrue(controller.interlocks().contains(ReactorInterlock.RUNTIME_DATA),
                "Unknown runtime schema did not expose its non-overridable data interlock");
        helper.assertTrue(preserved.getInt("schema_version") == 3
                        && "preserve-me".equals(preserved.getString("future_marker")),
                "Unknown runtime schema was overwritten instead of preserving raw data");
        helper.succeed();
    }

    private static void buildReactor(
            GameTestHelper helper,
            BlockPos controllerPosition,
            int width,
            int length,
            int height,
            Direction facing,
            Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns
    ) {
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    NuclearReactorStructure.ExpectedPart expected = NuclearReactorStructure.expectedPart(
                            width, length, height, x, y, z, columns, facing);
                    BlockPos position = NuclearReactorStructure.worldPosition(
                            controllerPosition, facing, width, x, y, z);
                    helper.getLevel().setBlock(position, state(expected), Block.UPDATE_ALL);
                }
            }
        }
        helper.assertTrue(helper.getLevel().getBlockEntity(controllerPosition)
                        instanceof NuclearReactorControllerBlockEntity,
                "Reactor controller block entity was not created");
    }

    private static Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns(int width, int length) {
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> result = new LinkedHashMap<>();
        NuclearReactorColumnType[] first = {
                NuclearReactorColumnType.FUEL_STANDARD,
                NuclearReactorColumnType.CONTROL_ROD_A,
                NuclearReactorColumnType.COOLANT_CHANNEL,
                NuclearReactorColumnType.INSTRUMENTATION
        };
        int index = 0;
        for (int z = 0; z < length - 4; z++) {
            for (int x = 0; x < width - 4; x++) {
                result.put(new ReactorColumnCoordinate(x, z), index < first.length
                        ? first[index]
                        : index % 2 == 0 ? NuclearReactorColumnType.FUEL_STANDARD
                        : NuclearReactorColumnType.REFLECTOR);
                index++;
            }
        }
        return result;
    }

    private static BlockState state(NuclearReactorStructure.ExpectedPart expected) {
        return switch (expected.kind()) {
            case CONTROLLER -> ModNuclearBlocks.REACTOR_CONTROLLER.get().defaultBlockState()
                    .setValue(NuclearReactorControllerBlock.FACING, expected.facing());
            case CONTAINMENT_CASING -> ModNuclearBlocks.REACTOR_CONTAINMENT_CASING.get().defaultBlockState();
            case PRESSURE_VESSEL -> ModNuclearBlocks.REACTOR_PRESSURE_VESSEL.get().defaultBlockState();
            case CONTROL_ROD_ACTUATOR -> ModNuclearBlocks.REACTOR_CONTROL_ROD_ACTUATOR.get().defaultBlockState();
            case COOLANT_PORT -> ModNuclearBlocks.REACTOR_MAIN_COOLANT_PORT.get().defaultBlockState()
                    .setValue(NuclearReactorPortBlock.FACING, expected.facing());
            case ELECTRICAL_PORT -> ModNuclearBlocks.REACTOR_ELECTRICAL_PORT.get().defaultBlockState()
                    .setValue(NuclearReactorPortBlock.FACING, expected.facing());
            case INSTRUMENTATION_PORT -> ModNuclearBlocks.REACTOR_INSTRUMENTATION_PORT.get().defaultBlockState()
                    .setValue(NuclearReactorPortBlock.FACING, expected.facing());
            case COLUMN_BASE -> ModNuclearBlocks.reactorColumn(expected.columnType()).get().defaultBlockState();
            case COLUMN_SEGMENT -> ModNuclearBlocks.REACTOR_COLUMN_SEGMENT.get().defaultBlockState();
        };
    }

    private static ServerPlayer operator(GameTestHelper helper, BlockPos controller) {
        ServerPlayer player = new ServerPlayer(
                helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "reactor-operator-test"));
        player.setPos(controller.getX() + 0.5D, controller.getY(), controller.getZ() + 2.5D);
        return player;
    }
}
