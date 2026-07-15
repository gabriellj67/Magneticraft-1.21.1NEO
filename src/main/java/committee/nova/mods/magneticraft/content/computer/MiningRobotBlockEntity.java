package committee.nova.mods.magneticraft.content.computer;

import com.mojang.authlib.GameProfile;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.computer.runtime.ComputerDeviceBus;
import committee.nova.mods.magneticraft.content.computer.runtime.ComputerDeviceBus.DeviceCommand;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerDevice;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative robot host with atomic inventory/energy checks for every world action.
 */
public final class MiningRobotBlockEntity extends ProgrammableBlockEntity {
    public static final int INVENTORY_SLOTS = 16;
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int MOVE_ENERGY_COST = 500;
    public static final int MINE_ENERGY_COST = 200;
    public static final int MAX_QUARRY_SIZE = 32;
    private static final String MOVE_COMPLETION_TAG = "move_completion";
    private static final String ORIENTATION_PITCH_TAG = "orientation_pitch";
    private static final String PENDING_COMMAND_TAG = "pending_command";
    private static final String PENDING_ARGUMENT_TAG = "pending_argument";
    private static final String ACTION_COOLDOWN_TAG = "action_cooldown";
    private static final String QUARRY_TASK_TAG = "quarry_task";

    private final ItemInventoryModule inventory;
    private final ElectricalPowerModule energy;
    private final ElectricalNetworkModule electricity;
    @Nullable
    private Direction pendingMove;
    @Nullable
    private Boolean moveCompletion;
    @Nullable
    private DeviceCommand pendingScriptCommand;
    private int pendingScriptArgument;
    private int actionCooldown;
    private int orientationPitch;
    @Nullable
    private RobotQuarryTask quarryTask;
    private boolean relocating;

    public MiningRobotBlockEntity(BlockPos position, BlockState state) {
        super(ModComputerContent.MINING_ROBOT_BLOCK_ENTITY.get(), position, state);
        inventory = addModule(new ItemInventoryModule(
                Magneticraft.id("inventory"),
                this,
                INVENTORY_SLOTS,
                (slot, stack) -> true,
                side -> allSlotAccess()
        ));
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                ElectricalNodeKind.MACHINE,
                side -> true
        ));
        energy = addModule(new ElectricalPowerModule(
                Magneticraft.id("energy_storage"),
                Magneticraft.id("mining_robot"),
                this,
                electricity,
                ElectricalPowerModule.ForgeEnergyAccess.NONE,
                side -> false,
                false
        ));
    }

    public static void serverTick(Level level, BlockPos position, BlockState state, MiningRobotBlockEntity robot) {
        robot.tickModules();
        if (!robot.electricalFaulted()) {
            robot.tickActionCooldown();
            robot.tickComputer();
        }
        robot.finishServerTick();
        if (!robot.electricalFaulted()
                && robot.pendingMove != null
                && level instanceof ServerLevel serverLevel) {
            robot.performPendingMove(serverLevel);
        }
    }

    @Override
    protected ComputerDevice.DeviceResult executeWorldInstruction(ComputerOpcode opcode, int operand) {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return ComputerDevice.DeviceResult.completeAndYield(0);
        }
        Direction direction = resolveRelativeDirection(operand);
        if (direction == null) {
            return ComputerDevice.DeviceResult.completeAndYield(0);
        }
        return switch (opcode) {
            case MOVE -> completeOrScheduleMove(serverLevel, direction);
            case MINE -> ComputerDevice.DeviceResult.completeAndYield(mine(serverLevel, direction) ? 1 : 0);
            default -> super.executeWorldInstruction(opcode, operand);
        };
    }

    @Override
    protected ComputerDeviceBus.DeviceResult executeWorldCommand(DeviceCommand command, int argument) {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return ComputerDeviceBus.DeviceResult.fault(committee.nova.mods.magneticraft.content.computer.vm.VmFault.UNSUPPORTED_DEVICE_INSTRUCTION);
        }
        return switch (command) {
            case INVENTORY_COUNT -> ComputerDeviceBus.DeviceResult.complete(inventoryCount());
            case ENERGY_STORED -> ComputerDeviceBus.DeviceResult.complete(energy.storedWholeJoules());
            case QUARRY -> executeQuarry(serverLevel, argument);
            case MOVE_FRONT, MOVE_BACK, ROTATE_LEFT, ROTATE_RIGHT, ROTATE_UP, ROTATE_DOWN,
                    MINE_FRONT, SCAN_FRONT, MOVE, MINE, SCAN -> executeScheduledAction(serverLevel, command, argument);
            default -> super.executeWorldCommand(command, argument);
        };
    }

    private ComputerDeviceBus.DeviceResult executeScheduledAction(
            ServerLevel level,
            DeviceCommand command,
            int argument
    ) {
        if (quarryTask != null) {
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        if (pendingScriptCommand == null) {
            pendingScriptCommand = command;
            pendingScriptArgument = argument;
            actionCooldown = actionCooldown(command, argument);
            markChanged();
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        if (pendingScriptCommand != command || pendingScriptArgument != argument) {
            return ComputerDeviceBus.DeviceResult.fault(
                    committee.nova.mods.magneticraft.content.computer.vm.VmFault.INVALID_SOURCE
            );
        }
        if (actionCooldown > 0) {
            return ComputerDeviceBus.DeviceResult.waiting();
        }

        ComputerDeviceBus.DeviceResult result = performScriptAction(level, command, argument);
        if (result.status() != ComputerDeviceBus.Status.WAIT) {
            pendingScriptCommand = null;
            pendingScriptArgument = 0;
            markChanged();
        }
        return result;
    }

    private ComputerDeviceBus.DeviceResult performScriptAction(
            ServerLevel level,
            DeviceCommand command,
            int argument
    ) {
        return switch (command) {
            case MOVE_FRONT -> scriptMove(level, frontDirection());
            case MOVE_BACK -> scriptMove(level, frontDirection().getOpposite());
            case ROTATE_LEFT -> ComputerDeviceBus.DeviceResult.complete(rotateHorizontal(level, false));
            case ROTATE_RIGHT -> ComputerDeviceBus.DeviceResult.complete(rotateHorizontal(level, true));
            case ROTATE_UP -> ComputerDeviceBus.DeviceResult.complete(rotateVertical(1));
            case ROTATE_DOWN -> ComputerDeviceBus.DeviceResult.complete(rotateVertical(-1));
            case MINE_FRONT -> scriptMine(level, frontDirection());
            case SCAN_FRONT -> scriptScan(level, frontDirection());
            case MOVE -> scriptMove(level, resolveRelativeDirection(argument));
            case MINE -> scriptMine(level, resolveRelativeDirection(argument));
            case SCAN -> scriptScan(level, resolveRelativeDirection(argument));
            default -> ComputerDeviceBus.DeviceResult.fault(
                    committee.nova.mods.magneticraft.content.computer.vm.VmFault.UNSUPPORTED_DEVICE_INSTRUCTION
            );
        };
    }

    private ComputerDeviceBus.DeviceResult scriptMove(ServerLevel level, @Nullable Direction direction) {
        if (direction == null) {
            return ComputerDeviceBus.DeviceResult.complete(0);
        }
        return toScriptResult(completeOrScheduleMove(level, direction));
    }

    private ComputerDeviceBus.DeviceResult scriptMine(ServerLevel level, @Nullable Direction direction) {
        if (direction == null) {
            return ComputerDeviceBus.DeviceResult.complete(0);
        }
        BlockPos target = worldPosition.relative(direction);
        if (!isSafeLoadedTarget(level, target)) {
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        return ComputerDeviceBus.DeviceResult.complete(mine(level, direction) ? 1 : 0);
    }

    private ComputerDeviceBus.DeviceResult scriptScan(ServerLevel level, @Nullable Direction direction) {
        if (direction == null) {
            return ComputerDeviceBus.DeviceResult.complete(0);
        }
        BlockPos target = worldPosition.relative(direction);
        if (!isSafeLoadedTarget(level, target)) {
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        return ComputerDeviceBus.DeviceResult.complete(Block.getId(level.getBlockState(target)));
    }

    private static ComputerDeviceBus.DeviceResult toScriptResult(ComputerDevice.DeviceResult result) {
        return switch (result.status()) {
            case COMPLETE -> ComputerDeviceBus.DeviceResult.complete(result.value());
            case WAIT -> ComputerDeviceBus.DeviceResult.waiting();
            case FAULT -> ComputerDeviceBus.DeviceResult.fault(result.fault());
        };
    }

    private void tickActionCooldown() {
        if (actionCooldown > 0) {
            actionCooldown--;
            markChanged();
        }
    }

    private static int actionCooldown(DeviceCommand command, int argument) {
        return switch (command) {
            case MOVE_BACK -> 10;
            case MINE_FRONT, MINE -> 10;
            case SCAN_FRONT, SCAN -> 1;
            case MOVE -> argument == 2 ? 10 : 5;
            default -> 5;
        };
    }

    private ComputerDevice.DeviceResult completeOrScheduleMove(ServerLevel level, Direction direction) {
        if (moveCompletion != null) {
            boolean completed = moveCompletion;
            moveCompletion = null;
            return ComputerDevice.DeviceResult.completeAndYield(completed ? 1 : 0);
        }
        if (pendingMove != null) {
            return ComputerDevice.DeviceResult.waiting();
        }
        if (!canMove(level, direction)) {
            return ComputerDevice.DeviceResult.completeAndYield(0);
        }
        pendingMove = direction;
        return ComputerDevice.DeviceResult.waiting();
    }

    private ComputerDeviceBus.DeviceResult executeQuarry(ServerLevel level, int requestedSize) {
        if (pendingScriptCommand != null) {
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        if (quarryTask == null) {
            if (requestedSize < 1 || requestedSize > RobotQuarryTask.MAX_SIZE) {
                return ComputerDeviceBus.DeviceResult.complete(0);
            }
            quarryTask = new RobotQuarryTask(requestedSize);
            markChanged();
        } else if (quarryTask.size() != requestedSize) {
            return ComputerDeviceBus.DeviceResult.fault(
                    committee.nova.mods.magneticraft.content.computer.vm.VmFault.INVALID_SOURCE
            );
        }

        if (quarryTask.tickCooldown()) {
            markChanged();
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        if (!quarryTask.mined()) {
            BlockPos target = worldPosition.below();
            if (!isSafeLoadedTarget(level, target)) {
                return ComputerDeviceBus.DeviceResult.waiting();
            }
            boolean hadBlock = !level.getBlockState(target).isAir();
            if (hadBlock && !mine(level, Direction.DOWN)) {
                return finishQuarry(false);
            }
            quarryTask.markMined(hadBlock);
            markChanged();
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        if (quarryTask.complete()) {
            return finishQuarry(true);
        }
        if (moveCompletion != null) {
            boolean completed = moveCompletion;
            moveCompletion = null;
            if (!completed) {
                return finishQuarry(false);
            }
            quarryTask.advanceAfterMove();
            markChanged();
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        if (pendingMove != null) {
            return ComputerDeviceBus.DeviceResult.waiting();
        }

        Direction direction = resolveRelativeDirection(quarryTask.nextMovement());
        if (direction == null) {
            return finishQuarry(false);
        }
        BlockPos target = worldPosition.relative(direction);
        if (!isSafeLoadedTarget(level, target)) {
            return ComputerDeviceBus.DeviceResult.waiting();
        }
        if (!canMove(level, direction)) {
            return finishQuarry(false);
        }
        pendingMove = direction;
        markChanged();
        return ComputerDeviceBus.DeviceResult.waiting();
    }

    private ComputerDeviceBus.DeviceResult finishQuarry(boolean completed) {
        quarryTask = null;
        moveCompletion = null;
        markChanged();
        return ComputerDeviceBus.DeviceResult.complete(completed ? 1 : 0);
    }

    private int inventoryCount() {
        int count = 0;
        for (int slot = 0; slot < inventory.slots(); slot++) {
            count += inventory.getStackInSlot(slot).getCount();
        }
        return count;
    }

    private int rotateHorizontal(ServerLevel level, boolean clockwise) {
        BlockState state = getBlockState();
        if (!state.hasProperty(ProgrammableBlock.FACING)) {
            return 0;
        }
        Direction facing = state.getValue(ProgrammableBlock.FACING);
        Direction rotated = clockwise ? facing.getClockWise() : facing.getCounterClockWise();
        return level.setBlock(worldPosition, state.setValue(ProgrammableBlock.FACING, rotated), Block.UPDATE_ALL)
                ? 1
                : 0;
    }

    private int rotateVertical(int delta) {
        int updated = Math.max(-1, Math.min(1, orientationPitch + delta));
        if (updated == orientationPitch) {
            return 0;
        }
        orientationPitch = updated;
        markChangedAndSync();
        return 1;
    }

    private Direction frontDirection() {
        if (orientationPitch > 0) {
            return Direction.UP;
        }
        if (orientationPitch < 0) {
            return Direction.DOWN;
        }
        return getBlockState().hasProperty(ProgrammableBlock.FACING)
                ? getBlockState().getValue(ProgrammableBlock.FACING)
                : Direction.NORTH;
    }

    public int orientationFlag() {
        Direction facing = getBlockState().hasProperty(ProgrammableBlock.FACING)
                ? getBlockState().getValue(ProgrammableBlock.FACING)
                : Direction.NORTH;
        return orientationFlag(facing, orientationPitch);
    }

    static int orientationFlag(Direction facing, int pitch) {
        if (facing == null || facing.getAxis().isVertical() || pitch < -1 || pitch > 1) {
            throw new IllegalArgumentException("Robot orientation requires horizontal facing and pitch -1..1");
        }
        return (pitch + 1) * 4 + facing.get2DDataValue();
    }

    public int actionCooldown() {
        return actionCooldown;
    }

    private boolean canMove(ServerLevel level, Direction direction) {
        BlockPos target = worldPosition.relative(direction);
        if (!isSafeLoadedTarget(level, target)
                || level.getBlockEntity(target) != null
                || !level.getBlockState(target).canBeReplaced()
                || energy.consumeJoules(MOVE_ENERGY_COST, true) != MOVE_ENERGY_COST) {
            return false;
        }
        FakePlayer player = ownerPlayer(level);
        return player != null && mayModify(level, player, target, direction.getOpposite());
    }

    private void performPendingMove(ServerLevel level) {
        Direction direction = pendingMove;
        pendingMove = null;
        if (direction == null || !canMove(level, direction)) {
            queueMoveCompletion(false);
            return;
        }

        BlockPos source = worldPosition;
        BlockPos target = source.relative(direction);
        double energyBefore = energy.storedJoules();
        if (energy.consumeJoules(MOVE_ENERGY_COST, false) != MOVE_ENERGY_COST) {
            queueMoveCompletion(false);
            return;
        }
        CompoundTag durableState = saveWithoutMetadata();
        BlockState movedState = getBlockState();
        FakePlayer player = ownerPlayer(level);
        if (player == null) {
            energy.restoreStoredJoules(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        BlockSnapshot replacedBlock = BlockSnapshot.create(level.dimension(), level, target);
        relocating = true;
        if (!level.setBlock(target, movedState, Block.UPDATE_ALL)) {
            relocating = false;
            energy.restoreStoredJoules(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        if (ForgeEventFactory.onBlockPlace(player, replacedBlock, direction.getOpposite())) {
            replacedBlock.restore(true, true);
            relocating = false;
            energy.restoreStoredJoules(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        BlockEntity targetEntity = level.getBlockEntity(target);
        if (!(targetEntity instanceof MiningRobotBlockEntity movedRobot)) {
            level.removeBlock(target, false);
            relocating = false;
            energy.restoreStoredJoules(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        movedRobot.load(durableState);
        if (!level.removeBlock(source, false)) {
            movedRobot.relocating = true;
            level.removeBlock(target, false);
            relocating = false;
            energy.restoreStoredJoules(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        movedRobot.queueMoveCompletion(true);
        movedRobot.markChangedAndSync();
    }

    private void queueMoveCompletion(boolean completed) {
        moveCompletion = completed;
        markChanged();
    }

    @Override
    protected void saveProgrammableData(CompoundTag tag) {
        tag.putInt(MOVE_COMPLETION_TAG, moveCompletion == null ? -1 : moveCompletion ? 1 : 0);
        tag.putInt(ORIENTATION_PITCH_TAG, orientationPitch);
        if (pendingScriptCommand != null) {
            tag.putString(PENDING_COMMAND_TAG, pendingScriptCommand.name());
            tag.putInt(PENDING_ARGUMENT_TAG, pendingScriptArgument);
            tag.putInt(ACTION_COOLDOWN_TAG, actionCooldown);
        }
        if (quarryTask != null) {
            tag.put(QUARRY_TASK_TAG, quarryTask.save());
        }
    }

    @Override
    protected void loadProgrammableData(CompoundTag tag) {
        orientationPitch = Math.max(-1, Math.min(1, tag.getInt(ORIENTATION_PITCH_TAG)));
        pendingScriptCommand = readPendingCommand(tag);
        pendingScriptArgument = pendingScriptCommand == null ? 0 : tag.getInt(PENDING_ARGUMENT_TAG);
        actionCooldown = pendingScriptCommand == null
                ? 0
                : Math.max(0, Math.min(10, tag.getInt(ACTION_COOLDOWN_TAG)));
        quarryTask = tag.contains(QUARRY_TASK_TAG, Tag.TAG_COMPOUND)
                ? RobotQuarryTask.restore(tag.getCompound(QUARRY_TASK_TAG)).orElse(null)
                : null;
        int savedCompletion = tag.getInt(MOVE_COMPLETION_TAG);
        boolean awaitingLegacyMove = vm().running()
                && vm().programCounter() >= 0
                && vm().programCounter() < vm().program().size()
                && vm().program().get(vm().programCounter()).opcode() == ComputerOpcode.MOVE;
        boolean awaitingScriptMove = pendingScriptCommand == DeviceCommand.MOVE_FRONT
                || pendingScriptCommand == DeviceCommand.MOVE_BACK
                || pendingScriptCommand == DeviceCommand.MOVE;
        boolean awaitingQuarryMove = quarryTask != null && quarryTask.mined() && !quarryTask.complete();
        boolean awaitingMove = awaitingLegacyMove || awaitingScriptMove || awaitingQuarryMove;
        moveCompletion = awaitingMove && (savedCompletion == 0 || savedCompletion == 1)
                ? savedCompletion == 1
                : null;
    }

    @Nullable
    private static DeviceCommand readPendingCommand(CompoundTag tag) {
        if (!tag.contains(PENDING_COMMAND_TAG, Tag.TAG_STRING)) {
            return null;
        }
        try {
            DeviceCommand command = DeviceCommand.valueOf(tag.getString(PENDING_COMMAND_TAG));
            return switch (command) {
                case MOVE_FRONT, MOVE_BACK, ROTATE_LEFT, ROTATE_RIGHT, ROTATE_UP, ROTATE_DOWN,
                        MINE_FRONT, SCAN_FRONT, MOVE, MINE, SCAN -> command;
                default -> null;
            };
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean mine(ServerLevel level, Direction direction) {
        BlockPos target = worldPosition.relative(direction);
        if (!isSafeLoadedTarget(level, target) || level.getBlockEntity(target) != null) {
            return false;
        }
        BlockState targetState = level.getBlockState(target);
        if (targetState.isAir()
                || targetState.getDestroySpeed(level, target) < 0.0F
                || energy.consumeJoules(MINE_ENERGY_COST, true) != MINE_ENERGY_COST) {
            return false;
        }
        FakePlayer player = ownerPlayer(level);
        if (player == null || !mayModify(level, player, target, direction.getOpposite())) {
            return false;
        }

        ItemStackHandler stagedInventory = copyInventory();
        ItemStack builtInDrill = new ItemStack(Items.DIAMOND_PICKAXE);
        if (targetState.requiresCorrectToolForDrops() && !builtInDrill.isCorrectToolForDrops(targetState)) {
            return false;
        }
        List<ItemStack> drops = Block.getDrops(targetState, level, target, null, player, builtInDrill);
        for (ItemStack drop : drops) {
            if (!ItemHandlerHelper.insertItemStacked(stagedInventory, drop.copy(), false).isEmpty()) {
                return false;
            }
        }

        ItemStack previousTool = player.getMainHandItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, builtInDrill);
        try {
            if (ForgeHooks.onBlockBreakEvent(level, GameType.SURVIVAL, player, target) == -1
                    || !level.getBlockState(target).equals(targetState)) {
                return false;
            }
            double energyBefore = energy.storedJoules();
            if (energy.consumeJoules(MINE_ENERGY_COST, false) != MINE_ENERGY_COST) {
                return false;
            }
            if (!level.destroyBlock(target, false, player)) {
                energy.restoreStoredJoules(energyBefore);
                return false;
            }
            IItemHandlerModifiable targetInventory = inventory.menuHandler();
            for (int slot = 0; slot < INVENTORY_SLOTS; slot++) {
                targetInventory.setStackInSlot(slot, stagedInventory.getStackInSlot(slot).copy());
            }
            return true;
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previousTool);
        }
    }

    private ItemStackHandler copyInventory() {
        ItemStackHandler copy = new ItemStackHandler(INVENTORY_SLOTS);
        for (int slot = 0; slot < INVENTORY_SLOTS; slot++) {
            copy.setStackInSlot(slot, inventory.getStackInSlot(slot).copy());
        }
        return copy;
    }

    private boolean isSafeLoadedTarget(ServerLevel level, BlockPos target) {
        return !level.isOutsideBuildHeight(target)
                && level.getWorldBorder().isWithinBounds(target)
                && level.hasChunk(target.getX() >> 4, target.getZ() >> 4);
    }

    private boolean mayModify(ServerLevel level, FakePlayer player, BlockPos target, Direction face) {
        player.setPos(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
        return level.mayInteract(player, target) && player.mayUseItemAt(target, face, ItemStack.EMPTY);
    }

    @Nullable
    private FakePlayer ownerPlayer(ServerLevel level) {
        UUID ownerId = owner();
        if (ownerId == null) {
            return null;
        }
        return FakePlayerFactory.get(level, new GameProfile(ownerId, "[Magneticraft]"));
    }

    @Nullable
    private Direction resolveRelativeDirection(int encodedDirection) {
        Direction facing = getBlockState().hasProperty(ProgrammableBlock.FACING)
                ? getBlockState().getValue(ProgrammableBlock.FACING)
                : Direction.NORTH;
        return switch (encodedDirection) {
            case 0 -> facing;
            case 1 -> facing.getClockWise();
            case 2 -> facing.getOpposite();
            case 3 -> facing.getCounterClockWise();
            case 4 -> Direction.UP;
            case 5 -> Direction.DOWN;
            default -> null;
        };
    }

    public ItemInventoryModule inventory() {
        return inventory;
    }

    public ElectricalPowerModule energy() {
        return energy;
    }

    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    public boolean isRelocating() {
        return relocating;
    }

    public void dropContents(Level level) {
        for (int slot = 0; slot < inventory.slots(); slot++) {
            ItemStack stack = inventory.extractInternal(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    private static ItemInventoryModule.SlotAccess allSlotAccess() {
        int[] slots = new int[INVENTORY_SLOTS];
        for (int slot = 0; slot < INVENTORY_SLOTS; slot++) {
            slots[slot] = slot;
        }
        return new ItemInventoryModule.SlotAccess(slots, slots);
    }
}
