package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.computer.ComputerBlockEntity;
import committee.nova.mods.magneticraft.content.computer.MiningRobotBlockEntity;
import committee.nova.mods.magneticraft.content.computer.ProgrammableBlock;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.network.UploadComputerProgramMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ComputerRobotGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos DEVICE_POSITION = new BlockPos(2, 2, 2);

    private ComputerRobotGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void computerVmSnapshotRestoresAllDurableState(GameTestHelper helper) {
        ComputerBlockEntity computer = placeComputer(helper);
        UUID owner = UUID.randomUUID();
        List<ComputerInstruction> program = List.of(
                instruction(ComputerOpcode.SET, 0, 41),
                instruction(ComputerOpcode.STORE, 7, 0),
                instruction(ComputerOpcode.SET_REDSTONE, 12, 1),
                instruction(ComputerOpcode.JUMP, 3, 0)
        );
        computer.setOwner(owner);

        helper.assertTrue(computer.tryReplaceProgram(0L, program), "Computer rejected its initial program");
        helper.assertTrue(computer.vm().executeTick(computer) == 3, "Computer did not yield after redstone output");

        CompoundTag saved = computer.saveWithoutMetadata();
        ComputerBlockEntity restored = new ComputerBlockEntity(computer.getBlockPos(), computer.getBlockState());
        restored.load(saved);

        helper.assertTrue(owner.equals(restored.owner()), "Computer owner did not survive reload");
        helper.assertTrue(restored.programRevision() == 1L, "Computer revision did not survive reload");
        helper.assertTrue(restored.program().equals(program), "Computer program did not survive reload");
        helper.assertTrue(restored.vm().programCounter() == 3, "Program counter did not survive reload");
        helper.assertTrue(restored.vm().register(0) == 41, "Register state did not survive reload");
        helper.assertTrue(restored.vm().register(1) == 12, "Device result register did not survive reload");
        helper.assertTrue(restored.vm().memory(7) == 41, "RAM state did not survive reload");
        helper.assertTrue(restored.vm().running(), "Running state did not survive reload");
        helper.assertTrue(restored.vm().fault() == VmFault.NONE, "Reload introduced a VM fault");
        helper.assertTrue(restored.vm().lastResult() == 12, "Last device result did not survive reload");
        helper.assertTrue(restored.redstoneOutput() == 12, "Redstone output did not survive reload");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void preResetComputerPayloadIsDiscardedWithoutFault(GameTestHelper helper) {
        ComputerBlockEntity computer = placeComputer(helper);
        BlockPos lampPosition = DEVICE_POSITION.relative(Direction.EAST);
        helper.setBlock(lampPosition, Blocks.REDSTONE_LAMP);
        computer.setOwner(UUID.randomUUID());
        helper.assertTrue(computer.tryReplaceProgram(0L, List.of(
                instruction(ComputerOpcode.SET_REDSTONE, 9, 0),
                instruction(ComputerOpcode.HALT, 0, 0)
        )), "Computer rejected its reset probe program");
        helper.assertTrue(computer.vm().executeTick(computer) == 1, "Reset probe did not execute");
        helper.assertTrue(computer.redstoneOutput() == 9, "Reset probe did not change redstone state");
        helper.assertTrue(helper.getBlockState(lampPosition).getValue(RedstoneLampBlock.LIT),
                "Reset probe did not power its neighboring lamp");

        CompoundTag incompatible = computer.saveWithoutMetadata();
        incompatible.remove("schema_version");
        computer.load(incompatible);

        helper.assertTrue(computer.owner() == null, "Incompatible payload retained its owner");
        helper.assertTrue(computer.programRevision() == 0L, "Incompatible payload retained its revision");
        helper.assertTrue(computer.program().isEmpty(), "Incompatible payload retained its program");
        helper.assertFalse(computer.vm().running(), "Incompatible payload kept the VM running");
        helper.assertTrue(computer.vm().fault() == VmFault.NONE, "Contract reset created a corruption fault");
        helper.assertTrue(computer.redstoneOutput() == 0, "Incompatible payload retained redstone output");
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(helper.getBlockState(lampPosition).getValue(RedstoneLampBlock.LIT),
                    "Schema reset left the neighboring lamp powered");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void uploadRejectsNonOwnerAndStaleRevision(GameTestHelper helper) {
        ComputerBlockEntity computer = placeComputer(helper);
        Player sender = helper.makeMockSurvivalPlayer();
        sender.setPos(
                computer.getBlockPos().getX() + 0.5D,
                computer.getBlockPos().getY() + 0.5D,
                computer.getBlockPos().getZ() + 0.5D
        );
        List<ComputerInstruction> firstProgram = List.of(
                instruction(ComputerOpcode.SET, 0, 1),
                instruction(ComputerOpcode.HALT, 0, 0)
        );
        List<ComputerInstruction> replacement = List.of(
                instruction(ComputerOpcode.SET, 0, 2),
                instruction(ComputerOpcode.HALT, 0, 0)
        );

        computer.setOwner(UUID.randomUUID());
        helper.assertFalse(computer.canManage(sender), "Mock sender unexpectedly bypassed ownership");
        boolean nonOwnerAccepted = UploadComputerProgramMessage.applyIfValid(
                sender,
                new UploadComputerProgramMessage(computer.getBlockPos(), 0L, firstProgram)
        );
        helper.assertFalse(nonOwnerAccepted, "Non-owner upload was accepted");
        helper.assertTrue(computer.programRevision() == 0L, "Rejected upload changed the revision");
        helper.assertTrue(computer.program().isEmpty(), "Rejected upload changed the program");

        computer.setOwner(sender.getUUID());
        boolean ownerAccepted = UploadComputerProgramMessage.applyIfValid(
                sender,
                new UploadComputerProgramMessage(computer.getBlockPos(), 0L, firstProgram)
        );
        helper.assertTrue(ownerAccepted, "Owner upload was rejected");
        helper.assertTrue(computer.programRevision() == 1L, "Accepted upload did not advance the revision");

        boolean staleAccepted = UploadComputerProgramMessage.applyIfValid(
                sender,
                new UploadComputerProgramMessage(computer.getBlockPos(), 0L, replacement)
        );
        helper.assertFalse(staleAccepted, "Stale revision upload was accepted");
        helper.assertTrue(computer.programRevision() == 1L, "Stale upload changed the revision");
        helper.assertTrue(computer.program().equals(firstProgram), "Stale upload replaced the program");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fullRobotInventoryPreventsMiningWithoutEnergyLoss(GameTestHelper helper) {
        MiningRobotBlockEntity robot = placeRobot(helper);
        BlockPos target = DEVICE_POSITION.relative(Direction.NORTH);
        helper.setBlock(target, Blocks.COBBLESTONE);
        robot.setOwner(UUID.randomUUID());
        robot.energy().setEnergyStored(2_000);
        for (int slot = 0; slot < robot.inventory().slots(); slot++) {
            robot.inventory().setStackInSlot(slot, new ItemStack(Items.DIRT, 64));
        }
        List<ComputerInstruction> program = List.of(
                instruction(ComputerOpcode.MINE, 0, 0),
                instruction(ComputerOpcode.HALT, 0, 0)
        );
        helper.assertTrue(robot.tryReplaceProgram(0L, program), "Robot rejected its mining program");

        int energyBefore = robot.energy().getEnergyStored();
        robot.vm().executeTick(robot);

        helper.assertTrue(helper.getBlockState(target).is(Blocks.COBBLESTONE), "Full robot destroyed the target");
        helper.assertTrue(robot.energy().getEnergyStored() == energyBefore, "Failed mining consumed energy");
        helper.assertTrue(robot.vm().lastResult() == 0, "Failed mining reported success");
        for (int slot = 0; slot < robot.inventory().slots(); slot++) {
            ItemStack stack = robot.inventory().getStackInSlot(slot);
            helper.assertTrue(stack.is(Items.DIRT) && stack.getCount() == 64,
                    "Failed mining changed inventory slot " + slot);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void validRobotMovePreservesDurableState(GameTestHelper helper) {
        MiningRobotBlockEntity robot = placeRobot(helper);
        BlockPos target = DEVICE_POSITION.relative(Direction.NORTH);
        UUID owner = UUID.randomUUID();
        List<ComputerInstruction> program = List.of(
                instruction(ComputerOpcode.SET, 3, 99),
                instruction(ComputerOpcode.STORE, 5, 3),
                instruction(ComputerOpcode.MOVE, 0, 0),
                instruction(ComputerOpcode.HALT, 0, 0)
        );
        helper.setBlock(target, Blocks.AIR);
        robot.setOwner(owner);
        robot.electricity().node().setVoltage(90.0D);
        robot.energy().setEnergyStored(5_000);
        robot.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 7));
        helper.assertTrue(robot.tryReplaceProgram(0L, program), "Robot rejected its movement program");
        helper.assertTrue(robot.vm().executeTick(robot) == 3, "Robot did not stop on the pending move");
        helper.assertTrue(robot.vm().programCounter() == 2, "Pending move advanced the program counter");

        MiningRobotBlockEntity.serverTick(
                helper.getLevel(),
                robot.getBlockPos(),
                robot.getBlockState(),
                robot
        );

        helper.assertTrue(helper.getBlockState(DEVICE_POSITION).isAir(), "Robot source block remained after move");
        MiningRobotBlockEntity moved = requireRobot(helper, target);
        helper.assertTrue(owner.equals(moved.owner()), "Robot owner changed during move");
        helper.assertTrue(moved.programRevision() == 1L, "Robot revision changed during move");
        helper.assertTrue(moved.program().equals(program), "Robot program changed during move");
        helper.assertTrue(moved.vm().programCounter() == 2, "Robot VM position changed during move");
        helper.assertTrue(moved.vm().register(3) == 99, "Robot register state changed during move");
        helper.assertTrue(moved.vm().memory(5) == 99, "Robot RAM state changed during move");
        helper.assertTrue(moved.energy().getEnergyStored() == 5_000 - MiningRobotBlockEntity.MOVE_ENERGY_COST,
                "Robot movement energy was not conserved");
        ItemStack movedStack = moved.inventory().getStackInSlot(0);
        helper.assertTrue(movedStack.is(Items.IRON_INGOT) && movedStack.getCount() == 7,
                "Robot inventory changed during move");

        helper.assertTrue(moved.vm().executeTick(moved) == 1, "Moved robot did not complete the waiting instruction");
        helper.assertTrue(moved.vm().register(0) == 1, "Successful move was not reported to the VM");
        helper.assertTrue(moved.vm().programCounter() == 3, "Completed move did not advance the VM");
        helper.assertTrue(moved.vm().executeTick(moved) == 1, "Moved robot did not execute its halt instruction");
        helper.assertFalse(moved.vm().running(), "Moved robot did not halt normally");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void canceledBreakEventPreventsRobotMiningWithoutEnergyLoss(GameTestHelper helper) {
        MiningRobotBlockEntity robot = placeRobot(helper);
        BlockPos target = DEVICE_POSITION.relative(Direction.NORTH);
        UUID owner = UUID.randomUUID();
        helper.setBlock(target, Blocks.COBBLESTONE);
        robot.setOwner(owner);
        robot.energy().setEnergyStored(2_000);
        helper.assertTrue(robot.tryReplaceProgram(0L, List.of(
                instruction(ComputerOpcode.MINE, 0, 0),
                instruction(ComputerOpcode.HALT, 0, 0)
        )), "Robot rejected its mining program");

        BreakCancellation listener = new BreakCancellation(robot.getBlockPos().relative(Direction.NORTH), owner);
        int energyBefore = robot.energy().getEnergyStored();
        MinecraftForge.EVENT_BUS.register(listener);
        try {
            robot.vm().executeTick(robot);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(listener);
        }

        helper.assertTrue(listener.called, "Robot mining did not reach the Forge break event");
        helper.assertTrue(helper.getBlockState(target).is(Blocks.COBBLESTONE), "Canceled mining destroyed the target");
        helper.assertTrue(robot.energy().getEnergyStored() == energyBefore, "Canceled mining consumed energy");
        helper.assertTrue(robot.inventory().getStackInSlot(0).isEmpty(), "Canceled mining inserted a drop");
        helper.assertTrue(robot.vm().lastResult() == 0, "Canceled mining reported success");
        helper.succeed();
    }

    private static ComputerBlockEntity placeComputer(GameTestHelper helper) {
        helper.setBlock(DEVICE_POSITION, ModComputerContent.COMPUTER.get().defaultBlockState());
        var blockEntity = helper.getBlockEntity(DEVICE_POSITION);
        helper.assertTrue(blockEntity instanceof ComputerBlockEntity, "Missing computer block entity");
        return (ComputerBlockEntity) blockEntity;
    }

    private static MiningRobotBlockEntity placeRobot(GameTestHelper helper) {
        helper.setBlock(
                DEVICE_POSITION,
                ModComputerContent.MINING_ROBOT.get().defaultBlockState()
                        .setValue(ProgrammableBlock.FACING, Direction.NORTH)
        );
        return requireRobot(helper, DEVICE_POSITION);
    }

    private static MiningRobotBlockEntity requireRobot(GameTestHelper helper, BlockPos position) {
        var blockEntity = helper.getBlockEntity(position);
        helper.assertTrue(blockEntity instanceof MiningRobotBlockEntity, "Missing mining robot block entity");
        return (MiningRobotBlockEntity) blockEntity;
    }

    private static ComputerInstruction instruction(ComputerOpcode opcode, int operandA, int operandB) {
        return new ComputerInstruction(opcode, operandA, operandB);
    }

    private static final class BreakCancellation {
        private final BlockPos target;
        private final UUID owner;
        private boolean called;

        private BreakCancellation(BlockPos target, UUID owner) {
            this.target = target;
            this.owner = owner;
        }

        @SubscribeEvent
        public void onBreak(BlockEvent.BreakEvent event) {
            if (event.getPos().equals(target) && event.getPlayer().getUUID().equals(owner)) {
                called = true;
                event.setCanceled(true);
            }
        }
    }
}
