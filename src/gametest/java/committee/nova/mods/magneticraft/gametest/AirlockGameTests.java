package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.singleblock.AirBubbleBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.AirBubbleOwnershipSavedData;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Loaded-server coverage for the released 1.12 airlock boundary, decay and energy contracts.
 */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AirlockGameTests {
    private static final String TEMPLATE = "advanced_systems";
    private static final BlockPos CENTER = new BlockPos(24, 1, 24);
    private static final int SCAN_RANGE = 10;

    private AirlockGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 110)
    public static void poweredAirlockSeparatesBoundaryAndMaintainsTheBubble(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = isolatedOrigin(helper, 96);
        BlockPos boundary = origin.offset(9, 0, 0);
        BlockPos outsideWater = origin.offset(10, 0, 0);
        BlockPos interior = origin.above();
        List<BlockPos> touched = new ArrayList<>();
        loadScanChunks(level, origin);
        clearScanArea(level, origin);
        place(level, touched, boundary, Blocks.WATER.defaultBlockState());
        place(level, touched, outsideWater, Blocks.WATER.defaultBlockState());
        encloseWater(level, touched, outsideWater, boundary);
        encloseWater(level, touched, boundary, outsideWater);
        place(level, touched, interior, Blocks.WATER.defaultBlockState());
        encloseWater(level, touched, interior, origin);
        place(level, touched, origin, airlockState());

        SingleBlockMachineBlockEntity airlock = requireAirlock(level, origin);
        airlock.electricity().node().setVoltage(125.0D);
        double[] energyAfterBuild = new double[1];

        helper.startSequence()
                .thenWaitUntil(() -> {
                    BlockState boundaryState = level.getBlockState(boundary);
                    helper.assertTrue(
                            boundaryState.is(ModMachineBlocks.AIR_BUBBLE.get())
                                    && !boundaryState.getValue(AirBubbleBlock.DECAYING),
                            "Boundary water did not become a stable air bubble"
                    );
                })
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(interior).isAir(), "Interior water did not become air");
                    helper.assertTrue(
                            level.getBlockState(outsideWater).is(Blocks.WATER),
                            "Scan changed water outside radius nine"
                    );
                    helper.assertTrue(
                            AirBubbleOwnershipSavedData.get(level).ownerOf(boundary).filter(origin::equals).isPresent(),
                            "Powered airlock did not persist ownership of its boundary bubble"
                    );
                    energyAfterBuild[0] = airlock.electricity().node().energyJoules();
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        Math.abs(airlock.electricity().node().energyJoules() - energyAfterBuild[0]) > 0.000001D,
                        "Airlock has not completed its next maintenance cycle"
                ))
                .thenExecute(() -> {
                    BlockState boundaryState = level.getBlockState(boundary);
                    helper.assertTrue(
                            boundaryState.is(ModMachineBlocks.AIR_BUBBLE.get())
                                    && !boundaryState.getValue(AirBubbleBlock.DECAYING),
                            "Powered airlock did not maintain its boundary bubble"
                    );
                    double actualEnergy = airlock.electricity().node().energyJoules();
                    double consumedEnergy = energyAfterBuild[0] - actualEnergy;
                    helper.assertTrue(
                            Math.abs(consumedEnergy - 1.0D) < 0.000001D,
                            "Maintaining one stable bubble consumed " + consumedEnergy + " joules instead of one"
                    );
                    cleanup(level, touched);
                    clearScanArea(level, origin);
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void undervoltageDecaysOnlyBubblesInsideRadiusNine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = isolatedOrigin(helper, 128);
        BlockPos inside = origin.offset(9, 0, 0);
        BlockPos outside = origin.offset(10, 0, 0);
        List<BlockPos> touched = new ArrayList<>();
        loadScanChunks(level, origin);
        place(level, touched, inside, stableBubble());
        place(level, touched, outside, stableBubble());
        place(level, touched, origin, airlockState());
        requireAirlock(level, origin).electricity().node().setVoltage(0.0D);

        helper.runAfterDelay(65, () -> {
            helper.assertTrue(level.getBlockState(inside).isAir(), "Undervoltage bubble inside radius nine did not decay");
            BlockState outsideState = level.getBlockState(outside);
            helper.assertTrue(
                    outsideState.is(ModMachineBlocks.AIR_BUBBLE.get())
                            && !outsideState.getValue(AirBubbleBlock.DECAYING),
                    "Undervoltage decay crossed the radius-nine boundary"
            );
            cleanup(level, touched);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void breakingAirlockStartsBubbleDecay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = isolatedOrigin(helper, 160);
        BlockPos bubble = origin.offset(2, 0, 0);
        List<BlockPos> touched = new ArrayList<>();
        loadScanChunks(level, origin);
        place(level, touched, bubble, stableBubble());
        place(level, touched, origin, airlockState());
        AirBubbleOwnershipSavedData.get(level).bind(bubble, origin);

        level.removeBlock(origin, false);
        helper.assertFalse(
                AirBubbleOwnershipSavedData.get(level).ownerOf(bubble).isPresent(),
                "Breaking the airlock retained its durable bubble ownership"
        );
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(level.getBlockState(bubble).isAir(), "Breaking the airlock did not decay its bubble");
            cleanup(level, touched);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void bubbleValidationDoesNotLoadItsOwnerChunkAndDecaysAfterInvalidOwnerLoads(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos bubble = isolatedOrigin(helper, 224);
        BlockPos owner = bubble.offset(1_024, 0, 0);
        int ownerChunkX = owner.getX() >> 4;
        int ownerChunkZ = owner.getZ() >> 4;
        helper.assertTrue(
                level.getChunkSource().getChunkNow(ownerChunkX, ownerChunkZ) == null,
                "Owner fixture chunk was already loaded"
        );
        level.setBlock(bubble, stableBubble(), Block.UPDATE_CLIENTS);
        AirBubbleOwnershipSavedData.get(level).bind(bubble, owner);
        AirBubbleBlock block = (AirBubbleBlock) ModMachineBlocks.AIR_BUBBLE.get();

        block.tick(level.getBlockState(bubble), level, bubble, level.random);
        helper.assertTrue(
                level.getChunkSource().getChunkNow(ownerChunkX, ownerChunkZ) == null,
                "Bubble validation force-loaded its owner chunk"
        );
        helper.assertFalse(
                level.getBlockState(bubble).getValue(AirBubbleBlock.DECAYING),
                "Bubble decayed while its owner chunk was unavailable"
        );

        level.getChunk(ownerChunkX, ownerChunkZ);
        block.tick(level.getBlockState(bubble), level, bubble, level.random);
        helper.assertTrue(
                level.getBlockState(bubble).getValue(AirBubbleBlock.DECAYING),
                "Bubble did not begin decaying after its loaded owner proved invalid"
        );
        helper.assertFalse(
                AirBubbleOwnershipSavedData.get(level).ownerOf(bubble).isPresent(),
                "Invalid owner binding was not removed"
        );
        block.tick(level.getBlockState(bubble), level, bubble, level.random);
        helper.assertTrue(level.getBlockState(bubble).isAir(), "Decaying bubble did not clean itself up");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 70)
    public static void insufficientBudgetDoesNotApplyAPartialWaterPlan(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = isolatedOrigin(helper, 192);
        BlockPos outsideSentinel = origin.offset(10, 0, 0);
        List<BlockPos> touched = new ArrayList<>();
        List<BlockPos> water = new ArrayList<>();
        loadScanChunks(level, origin);

        for (int y = -9; y <= 9; y++) {
            for (int z = -9; z <= 9; z++) {
                for (int x = -9; x <= 9; x++) {
                    int distanceSquared = x * x + y * y + z * z;
                    BlockPos target = origin.offset(x, y, z);
                    if (distanceSquared > 64 && distanceSquared <= 81) {
                        place(level, touched, target, Blocks.STONE.defaultBlockState());
                    } else if (distanceSquared <= 64 && !target.equals(origin)) {
                        place(level, touched, target, Blocks.WATER.defaultBlockState());
                        water.add(target);
                    }
                }
            }
        }
        place(level, touched, outsideSentinel, Blocks.WATER.defaultBlockState());
        place(level, touched, origin, airlockState());
        SingleBlockMachineBlockEntity airlock = requireAirlock(level, origin);
        airlock.electricity().node().setEnergyJoules(3_600.0D);

        helper.runAfterDelay(45, () -> {
            helper.assertTrue(water.size() == 2_108, "Budget fixture does not exceed 3,600 joules");
            helper.assertTrue(
                    water.stream().allMatch(position -> level.getBlockState(position).is(Blocks.WATER)),
                    "Insufficient budget applied only part of the water-removal plan"
            );
            helper.assertTrue(
                    Math.abs(airlock.electricity().node().energyJoules() - 3_600.0D) < 0.000001D,
                    "Rejected airlock plan consumed energy"
            );
            helper.assertTrue(
                    level.getBlockState(outsideSentinel).is(Blocks.WATER),
                    "Rejected plan mutated a scan-only position outside radius nine"
            );
            cleanup(level, touched);
            helper.succeed();
        });
    }

    private static BlockPos isolatedOrigin(GameTestHelper helper, int y) {
        BlockPos base = helper.absolutePos(CENTER);
        return new BlockPos(base.getX(), y, base.getZ());
    }

    private static void loadScanChunks(ServerLevel level, BlockPos origin) {
        int minimumChunkX = (origin.getX() - SCAN_RANGE) >> 4;
        int maximumChunkX = (origin.getX() + SCAN_RANGE) >> 4;
        int minimumChunkZ = (origin.getZ() - SCAN_RANGE) >> 4;
        int maximumChunkZ = (origin.getZ() + SCAN_RANGE) >> 4;
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
    }

    private static void clearScanArea(ServerLevel level, BlockPos origin) {
        for (int y = -SCAN_RANGE; y <= SCAN_RANGE; y++) {
            for (int z = -SCAN_RANGE; z <= SCAN_RANGE; z++) {
                for (int x = -SCAN_RANGE; x <= SCAN_RANGE; x++) {
                    BlockPos target = origin.offset(x, y, z);
                    if (!level.getBlockState(target).isAir()) {
                        level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static void encloseWater(
            ServerLevel level,
            List<BlockPos> touched,
            BlockPos water,
            BlockPos openNeighbor
    ) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = water.relative(direction);
            if (!neighbor.equals(openNeighbor)) {
                place(level, touched, neighbor, Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static void place(ServerLevel level, List<BlockPos> touched, BlockPos position, BlockState state) {
        level.setBlock(position, state, Block.UPDATE_CLIENTS);
        touched.add(position.immutable());
    }

    private static void cleanup(ServerLevel level, List<BlockPos> touched) {
        for (BlockPos position : touched) {
            level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static SingleBlockMachineBlockEntity requireAirlock(ServerLevel level, BlockPos position) {
        if (!(level.getBlockEntity(position) instanceof SingleBlockMachineBlockEntity machine)
                || machine.definition() != SingleBlockMachineDefinition.AIRLOCK) {
            throw new AssertionError("Missing airlock block entity at " + position);
        }
        return machine;
    }

    private static BlockState airlockState() {
        return ModMachineBlocks.machine(SingleBlockMachineDefinition.AIRLOCK).get().defaultBlockState()
                .setValue(SingleBlockMachineBlock.FACING, Direction.NORTH)
                .setValue(SingleBlockMachineBlock.MASTER, true)
                .setValue(SingleBlockMachineBlock.LIT, false);
    }

    private static BlockState stableBubble() {
        return ModMachineBlocks.AIR_BUBBLE.get().defaultBlockState()
                .setValue(AirBubbleBlock.DECAYING, false);
    }
}
