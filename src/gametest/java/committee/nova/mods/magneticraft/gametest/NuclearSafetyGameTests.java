package committee.nova.mods.magneticraft.gametest;

import com.mojang.authlib.GameProfile;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyItem;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyState;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolPortBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolStructureValidator;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.init.ModNuclearItems;
import committee.nova.mods.magneticraft.system.nuclear.radiation.RadiationExposureService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

import java.util.UUID;

/** Loaded-world proof for spent-fuel handling and removable, shieldable radiation sources. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearSafetyGameTests {
    private static final String TEMPLATE = "base_content";

    private NuclearSafetyGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void spentFuelPoolCoolsStatefulFuelAndOnlyExportsOnOutwardFace(GameTestHelper helper) {
        int width = 5;
        int length = 5;
        int height = 4;
        Direction facing = Direction.NORTH;
        BlockPos controllerPosition = helper.absolutePos(new BlockPos(8, 2, 8));
        buildPool(helper, controllerPosition, width, length, height, facing);
        SpentFuelPoolControllerBlockEntity controller =
                (SpentFuelPoolControllerBlockEntity) helper.getLevel().getBlockEntity(controllerPosition);
        ServerPlayer owner = operator(helper, controllerPosition, "spent-fuel-pool-operator");
        helper.assertTrue(controller.tryForm(owner), "Complete variable spent-fuel pool did not form");
        helper.assertTrue(controller.snapshot().orElseThrow().waterBlocks() == 18,
                "Pool snapshot did not retain its complete water inventory");

        BlockPos portPosition = controller.snapshot().orElseThrow().port();
        SpentFuelPoolPortBlockEntity port =
                (SpentFuelPoolPortBlockEntity) helper.getLevel().getBlockEntity(portPosition);
        Direction outward = facing.getOpposite();
        helper.assertTrue(port.getCapability(ForgeCapabilities.ITEM_HANDLER, outward).isPresent(),
                "Pool port did not expose item transfer on its exact outward face");
        helper.assertTrue(!port.getCapability(ForgeCapabilities.ITEM_HANDLER, facing).isPresent(),
                "Pool port exposed item transfer on an unsupported face");

        ItemStack spent = new ItemStack(ModNuclearItems.fuelAssembly(NuclearFuelGrade.STANDARD_ENRICHMENT).get());
        FuelAssemblyItem item = (FuelAssemblyItem) spent.getItem();
        item.writeState(spent, new FuelAssemblyState(
                NuclearFuelGrade.STANDARD_ENRICHMENT.definitionId(), 0.75D, 0.2D,
                5.00005D, 374.0D, 0.9D, helper.getLevel().getGameTime()));
        IItemHandler external = port.getCapability(ForgeCapabilities.ITEM_HANDLER, outward).orElseThrow(AssertionError::new);
        helper.assertTrue(external.insertItem(0, spent, false).isEmpty(),
                "Pool port rejected a valid stateful spent-fuel assembly");
        helper.assertTrue(external.extractItem(0, 1, false).isEmpty(),
                "Pool exported hot spent fuel before cooling completed");

        helper.runAfterDelay(45, () -> {
            ItemStack cooled = external.extractItem(0, 1, false);
            helper.assertTrue(!cooled.isEmpty(), "Pool did not export fuel after reaching both safety thresholds");
            FuelAssemblyState state = item.state(cooled).orElseThrow();
            helper.assertTrue(state.burnupFraction() == 0.75D,
                    "Pool cooling reset or changed durable fuel burnup");
            helper.assertTrue(state.temperatureKelvin() <= 373.15D && state.decayHeatJoules() <= 5.0D,
                    "Exported spent fuel was still above a configured safety threshold");
            helper.assertTrue(port.heat().node().temperatureKelvin() > 293.15D,
                    "Spent-fuel cooling discarded heat instead of forwarding it to the cold-side node");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void leadShieldAttenuatesVisibleSourceAndCleanupRemovesHazard(GameTestHelper helper) {
        BlockPos source = helper.absolutePos(new BlockPos(5, 2, 5));
        helper.getLevel().setBlock(source, ModNuclearBlocks.RADIOACTIVE_DEBRIS.get().defaultBlockState(), Block.UPDATE_ALL);
        ServerPlayer worker = operator(helper, source.east(4), "radiation-worker");
        worker.setPos(source.getX() + 4.5D, source.getY(), source.getZ() + 0.5D);

        helper.runAfterDelay(2, () -> {
            double unshielded = RadiationExposureService.reading(worker).doseRateMillisievertsPerHour();
            helper.assertTrue(unshielded > 0.0D, "Loaded radioactive debris was not indexed as a radiation source");
            BlockPos wall = source.east(2);
            for (int y = 0; y < 3; y++) {
                helper.getLevel().setBlock(wall.above(y),
                        ModNuclearBlocks.LEAD_RADIATION_SHIELD.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            double shielded = RadiationExposureService.reading(worker).doseRateMillisievertsPerHour();
            helper.assertTrue(shielded < unshielded * 0.1D,
                    "Lead shielding did not materially attenuate the source-to-player ray");

            worker.setItemInHand(InteractionHand.MAIN_HAND,
                    new ItemStack(ModNuclearItems.DECONTAMINATION_KIT.get()));
            UseOnContext context = new UseOnContext(worker, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(source), Direction.UP, source, false));
            ModNuclearItems.DECONTAMINATION_KIT.get().useOn(context);
            helper.assertTrue(helper.getLevel().getBlockState(source).isAir(),
                    "Decontamination kit did not remove the visible debris source");
            helper.assertTrue(RadiationExposureService.reading(worker).doseRateMillisievertsPerHour() == 0.0D,
                    "Removed debris continued to contribute invisible chunk radiation");
            helper.succeed();
        });
    }

    private static void buildPool(GameTestHelper helper, BlockPos controller, int width, int length,
                                  int height, Direction facing) {
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos position = SpentFuelPoolStructureValidator.world(
                            controller, facing, width, length, x, y, z);
                    if (x == width / 2 && y == 1 && z == length - 1) {
                        helper.getLevel().setBlock(position,
                                ModNuclearBlocks.SPENT_FUEL_POOL_CONTROLLER.get().defaultBlockState()
                                        .setValue(SpentFuelPoolControllerBlock.FACING, facing), Block.UPDATE_ALL);
                    } else if (x == width / 2 && y == 1 && z == 0) {
                        helper.getLevel().setBlock(position,
                                ModNuclearBlocks.SPENT_FUEL_POOL_PORT.get().defaultBlockState()
                                        .setValue(SpentFuelPoolPortBlock.FACING, facing.getOpposite()), Block.UPDATE_ALL);
                    } else if (y == 0 || x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                        helper.getLevel().setBlock(position,
                                ModNuclearBlocks.FACILITY_CASING.get().defaultBlockState(), Block.UPDATE_ALL);
                    } else if (y == height - 1) {
                        helper.getLevel().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    } else {
                        helper.getLevel().setBlock(position, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    private static ServerPlayer operator(GameTestHelper helper, BlockPos position, String name) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new GameProfile(UUID.randomUUID(), name));
        player.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 2.5D);
        return player;
    }
}
