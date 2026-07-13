package committee.nova.mods.magneticraft.content.computer;

import com.mojang.authlib.GameProfile;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalEnergyBridgeModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
    private static final String MOVE_COMPLETION_TAG = "move_completion";

    private final ItemInventoryModule inventory;
    private final EnergyStorageModule energy;
    private final ElectricalNetworkModule electricity;
    @Nullable
    private Direction pendingMove;
    @Nullable
    private Boolean moveCompletion;
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
        energy = addModule(new EnergyStorageModule(
                Magneticraft.id("energy_storage"),
                this,
                ENERGY_CAPACITY,
                1_000,
                1_000,
                side -> true,
                true,
                false
        ));
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                new ElectricalNode(0.5D, 125.0D, 0.001D),
                side -> true
        ));
        addModule(new ElectricalEnergyBridgeModule(
                Magneticraft.id("electricity_bridge"),
                electricity,
                energy,
                90.0D,
                90.0D,
                1_000
        ));
    }

    public static void serverTick(Level level, BlockPos position, BlockState state, MiningRobotBlockEntity robot) {
        robot.tickModules();
        robot.tickComputer();
        robot.finishServerTick();
        if (robot.pendingMove != null && level instanceof ServerLevel serverLevel) {
            robot.performPendingMove(serverLevel);
        }
    }

    @Override
    protected DeviceResult executeWorldInstruction(ComputerOpcode opcode, int operand) {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return DeviceResult.completeAndYield(0);
        }
        Direction direction = resolveRelativeDirection(operand);
        if (direction == null) {
            return DeviceResult.completeAndYield(0);
        }
        return switch (opcode) {
            case MOVE -> completeOrScheduleMove(serverLevel, direction);
            case MINE -> DeviceResult.completeAndYield(mine(serverLevel, direction) ? 1 : 0);
            default -> super.executeWorldInstruction(opcode, operand);
        };
    }

    private DeviceResult completeOrScheduleMove(ServerLevel level, Direction direction) {
        if (moveCompletion != null) {
            boolean completed = moveCompletion;
            moveCompletion = null;
            return DeviceResult.completeAndYield(completed ? 1 : 0);
        }
        if (pendingMove != null) {
            return DeviceResult.waiting();
        }
        if (!canMove(level, direction)) {
            return DeviceResult.completeAndYield(0);
        }
        pendingMove = direction;
        return DeviceResult.waiting();
    }

    private boolean canMove(ServerLevel level, Direction direction) {
        BlockPos target = worldPosition.relative(direction);
        if (!isSafeLoadedTarget(level, target)
                || level.getBlockEntity(target) != null
                || !level.getBlockState(target).canBeReplaced()
                || energy.extractEnergy(MOVE_ENERGY_COST, true) != MOVE_ENERGY_COST) {
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
        int energyBefore = energy.getEnergyStored();
        if (energy.extractEnergy(MOVE_ENERGY_COST, false) != MOVE_ENERGY_COST) {
            queueMoveCompletion(false);
            return;
        }
        CompoundTag durableState = saveWithoutMetadata();
        BlockState movedState = getBlockState();
        FakePlayer player = ownerPlayer(level);
        if (player == null) {
            energy.setEnergyStored(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        BlockSnapshot replacedBlock = BlockSnapshot.create(level.dimension(), level, target);
        relocating = true;
        if (!level.setBlock(target, movedState, Block.UPDATE_ALL)) {
            relocating = false;
            energy.setEnergyStored(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        if (ForgeEventFactory.onBlockPlace(player, replacedBlock, direction.getOpposite())) {
            replacedBlock.restore(true, true);
            relocating = false;
            energy.setEnergyStored(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        BlockEntity targetEntity = level.getBlockEntity(target);
        if (!(targetEntity instanceof MiningRobotBlockEntity movedRobot)) {
            level.removeBlock(target, false);
            relocating = false;
            energy.setEnergyStored(energyBefore);
            queueMoveCompletion(false);
            return;
        }
        movedRobot.load(durableState);
        if (!level.removeBlock(source, false)) {
            movedRobot.relocating = true;
            level.removeBlock(target, false);
            relocating = false;
            energy.setEnergyStored(energyBefore);
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
    }

    @Override
    protected void loadProgrammableData(CompoundTag tag) {
        int savedCompletion = tag.getInt(MOVE_COMPLETION_TAG);
        boolean awaitingMove = vm().running()
                && vm().programCounter() >= 0
                && vm().programCounter() < vm().program().size()
                && vm().program().get(vm().programCounter()).opcode() == ComputerOpcode.MOVE;
        moveCompletion = awaitingMove && (savedCompletion == 0 || savedCompletion == 1)
                ? savedCompletion == 1
                : null;
    }

    private boolean mine(ServerLevel level, Direction direction) {
        BlockPos target = worldPosition.relative(direction);
        if (!isSafeLoadedTarget(level, target) || level.getBlockEntity(target) != null) {
            return false;
        }
        BlockState targetState = level.getBlockState(target);
        if (targetState.isAir()
                || targetState.getDestroySpeed(level, target) < 0.0F
                || energy.extractEnergy(MINE_ENERGY_COST, true) != MINE_ENERGY_COST) {
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
            int energyBefore = energy.getEnergyStored();
            if (energy.extractEnergy(MINE_ENERGY_COST, false) != MINE_ENERGY_COST) {
                return false;
            }
            if (!level.destroyBlock(target, false, player)) {
                energy.setEnergyStored(energyBefore);
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

    public EnergyStorageModule energy() {
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
