package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.system.network.logistics.ItemHandlerTransactions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Optional;

/**
 * Item, fluid and animal automation behavior for the Task 5 utility machines.
 */
final class SingleBlockAutomationLogic {
    private final SingleBlockMachineBlockEntity machine;
    private final SingleBlockMachineState state;

    SingleBlockAutomationLogic(SingleBlockMachineBlockEntity machine, SingleBlockMachineState state) {
        this.machine = machine;
        this.state = state;
    }

    void tickPassive(ServerLevel level) {
        if (machine.definition() == SingleBlockMachineDefinition.SMALL_TANK
                && state.tankExportEnabled
                && machine.primaryTank() != null) {
            SingleBlockMachineSupport.pushFluid(machine, Direction.DOWN, machine.primaryTank(), 1_000);
        } else if (machine.definition() == SingleBlockMachineDefinition.FILTER
                && level.getGameTime() % 5L == 0L) {
            pushBufferedInventory(SingleBlockMachineSupport.facing(machine), 0);
        }
    }

    void tickSluice(ServerLevel level) {
        if (state.chainDelay > 0 && --state.chainDelay == 0) {
            BlockPos next = machine.getBlockPos().relative(SingleBlockMachineSupport.facing(machine), 2).below();
            if (level.getBlockEntity(next) instanceof SingleBlockMachineBlockEntity nextMachine
                    && nextMachine.definition() == SingleBlockMachineDefinition.SLUICE_BOX) {
                nextMachine.activateSluiceChain();
            }
        }
        if (state.progress <= 0) {
            return;
        }
        state.working = true;
        if (--state.progress == 0) {
            finishSluice(level);
        }
        machine.markChanged();
    }

    void tickFeedingTrough(ServerLevel level) {
        if (level.getGameTime() % 400L != 0L || machine.inventory() == null) {
            return;
        }
        ItemStack food = machine.inventory().getStackInSlot(0);
        if (food.getCount() < 2) {
            return;
        }
        List<Animal> animals = level.getEntitiesOfClass(
                Animal.class,
                new AABB(machine.getBlockPos()).inflate(4.0D, 1.5D, 4.0D),
                Animal::isAlive
        );
        if (animals.size() >= 30) {
            return;
        }
        for (int firstIndex = 0; firstIndex < animals.size(); firstIndex++) {
            Animal first = animals.get(firstIndex);
            if (!first.canFallInLove() || !first.isFood(food)) {
                continue;
            }
            for (int secondIndex = firstIndex + 1; secondIndex < animals.size(); secondIndex++) {
                Animal second = animals.get(secondIndex);
                if (second.getType() == first.getType() && second.canFallInLove() && second.isFood(food)) {
                    machine.inventory().extractInternal(0, 2, false);
                    first.setInLove(null);
                    second.setInLove(null);
                    machine.markChangedAndSync();
                    return;
                }
            }
        }
    }

    void tickInserter(ServerLevel level) {
        if (machine.inventory() == null) {
            return;
        }
        if (state.cooldown > 0) {
            state.cooldown--;
            return;
        }
        ItemStack carried = machine.inventory().getStackInSlot(0);
        if (!carried.isEmpty()) {
            if (deliverInserterStack(level, carried)) {
                machine.inventory().setStackInSlot(0, ItemStack.EMPTY);
                state.cooldown = inserterDelay();
                machine.markChangedAndSync();
            }
            return;
        }
        ItemStack extracted = extractForInserter(level);
        if (!extracted.isEmpty()) {
            machine.inventory().setStackInSlot(0, extracted);
            state.cooldown = inserterDelay();
            state.working = true;
            machine.markChangedAndSync();
        }
    }

    void tickWaterGenerator() {
        if (machine.primaryTank() == null) {
            return;
        }
        int room = machine.primaryTank().tank().getCapacity() - machine.primaryTank().tank().getFluidAmount();
        if (room > 0) {
            machine.primaryTank().tank().fill(
                    new FluidStack(net.minecraft.world.level.material.Fluids.WATER, room),
                    IFluidHandler.FluidAction.EXECUTE
            );
        }
        int remaining = 20;
        Direction[] directions = Direction.values();
        for (int offset = 0; offset < directions.length && remaining > 0; offset++) {
            int index = (state.fluidOutputCursor + offset) % directions.length;
            remaining -= SingleBlockMachineSupport.pushFluid(machine, directions[index], machine.primaryTank(), remaining);
            if (remaining < 20) {
                state.fluidOutputCursor = (index + 1) % directions.length;
            }
        }
    }

    void tickRelay(ServerLevel level) {
        if (level.getGameTime() % 5L == 0L && machine.inventory() != null) {
            for (int slot = 0; slot < machine.inventory().slots(); slot++) {
                if (pushBufferedInventory(SingleBlockMachineSupport.facing(machine), slot)) {
                    return;
                }
            }
        }
    }

    void tickTransposer(ServerLevel level) {
        if (level.getGameTime() % 5L != 0L) {
            return;
        }
        Direction output = SingleBlockMachineSupport.facing(machine);
        Direction input = output.getOpposite();
        IItemHandler target = SingleBlockMachineSupport.adjacentItemHandler(machine, output);
        if (target == null) {
            return;
        }
        IItemHandler source = SingleBlockMachineSupport.adjacentItemHandler(machine, input);
        if (source != null) {
            transferFromInventory(source, target);
            return;
        }
        AABB area = new AABB(machine.getBlockPos().relative(input));
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
            ItemStack candidate = itemEntity.getItem();
            if (!SingleBlockMachineSupport.filterAllows(machine, state, candidate, true)
                    || !ItemHandlerTransactions.acceptsAll(target, candidate)) {
                continue;
            }
            ItemStack remainder = ItemHandlerTransactions.insert(target, candidate.copy(), false);
            if (remainder.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remainder);
            }
            return;
        }
    }

    void activateSluiceChain() {
        if (state.progress == 0) {
            state.progress = SingleBlockMachineBlockEntity.SLUICE_DURATION;
            state.totalProgress = SingleBlockMachineBlockEntity.SLUICE_DURATION;
            state.chainDelay = 20;
            machine.markChangedAndSync();
        }
    }

    private void transferFromInventory(IItemHandler source, IItemHandler target) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack candidate = source.extractItem(slot, 64, true);
            if (candidate.isEmpty()
                    || !SingleBlockMachineSupport.filterAllows(machine, state, candidate, true)
                    || !ItemHandlerTransactions.acceptsAll(target, candidate)) {
                continue;
            }
            ItemStack extracted = source.extractItem(slot, candidate.getCount(), false);
            ItemStack remainder = ItemHandlerTransactions.insert(target, extracted, false);
            if (!remainder.isEmpty()) {
                source.insertItem(slot, remainder, false);
            }
            machine.markChanged();
            return;
        }
    }

    private void finishSluice(ServerLevel level) {
        if (machine.inventory() == null) {
            return;
        }
        ItemStack input = machine.inventory().getStackInSlot(0);
        Optional<SluiceRecipe> recipe = SingleBlockMachineSupport.findSluiceRecipe(machine, input);
        if (recipe.isEmpty()) {
            return;
        }
        Direction facing = SingleBlockMachineSupport.facing(machine);
        int count = input.getCount();
        for (int item = 0; item < count; item++) {
            for (SluiceRecipe.ChanceOutput output : recipe.get().outputs()) {
                if (!output.stack().isEmpty() && level.random.nextFloat() < output.chance()) {
                    net.minecraft.world.Containers.dropItemStack(
                            level,
                            machine.getBlockPos().getX() + facing.getStepX() + 0.5D,
                            machine.getBlockPos().getY() + 0.5D,
                            machine.getBlockPos().getZ() + facing.getStepZ() + 0.5D,
                            output.stack().copy()
                    );
                }
            }
        }
        machine.inventory().setStackInSlot(0, ItemStack.EMPTY);
        machine.markChangedAndSync();
    }

    private boolean pushBufferedInventory(Direction output, int slot) {
        if (machine.inventory() == null) {
            return false;
        }
        ItemStack stack = machine.inventory().getStackInSlot(slot);
        IItemHandler target = SingleBlockMachineSupport.adjacentItemHandler(machine, output);
        if (stack.isEmpty() || target == null || !ItemHandlerTransactions.acceptsAll(target, stack)) {
            return false;
        }
        ItemStack extracted = machine.inventory().extractInternal(slot, stack.getCount(), false);
        ItemStack remainder = ItemHandlerTransactions.insert(target, extracted, false);
        if (!remainder.isEmpty()) {
            machine.inventory().menuHandler().insertItem(slot, remainder, false);
        }
        machine.markChanged();
        return remainder.isEmpty();
    }

    private ItemStack extractForInserter(ServerLevel level) {
        Direction sourceDirection = SingleBlockMachineSupport.facing(machine);
        int maximum = inserterStackSize();
        for (BlockPos sourcePosition : List.of(
                machine.getBlockPos().relative(sourceDirection),
                machine.getBlockPos().relative(sourceDirection).below()
        )) {
            BlockEntity sourceEntity = level.getBlockEntity(sourcePosition);
            if (sourceEntity == null) {
                continue;
            }
            IItemHandler source = sourceEntity
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, sourceDirection.getOpposite())
                    .orElse(null);
            if (source == null) {
                continue;
            }
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack candidate = source.extractItem(slot, maximum, true);
                if (!candidate.isEmpty()
                        && SingleBlockMachineSupport.filterAllows(machine, state, candidate, state.inserterWhitelist)) {
                    return source.extractItem(slot, candidate.getCount(), false);
                }
            }
        }
        if (state.inserterGrabItems) {
            AABB area = new AABB(machine.getBlockPos().relative(sourceDirection));
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
                ItemStack stack = entity.getItem();
                if (!SingleBlockMachineSupport.filterAllows(machine, state, stack, state.inserterWhitelist)) {
                    continue;
                }
                int moved = Math.min(maximum, stack.getCount());
                ItemStack result = stack.copyWithCount(moved);
                stack.shrink(moved);
                if (stack.isEmpty()) {
                    entity.discard();
                }
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private boolean deliverInserterStack(ServerLevel level, ItemStack stack) {
        Direction output = SingleBlockMachineSupport.facing(machine).getOpposite();
        for (BlockPos targetPosition : List.of(
                machine.getBlockPos().relative(output),
                machine.getBlockPos().relative(output).below()
        )) {
            BlockEntity targetEntity = level.getBlockEntity(targetPosition);
            if (targetEntity == null) {
                continue;
            }
            IItemHandler target = targetEntity
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, output.getOpposite())
                    .orElse(null);
            if (target != null && ItemHandlerTransactions.acceptsAll(target, stack)) {
                return ItemHandlerTransactions.insert(target, stack, false).isEmpty();
            }
        }
        if (state.inserterDropItems) {
            BlockPos target = machine.getBlockPos().relative(output);
            level.addFreshEntity(new ItemEntity(
                    level,
                    target.getX() + 0.5D,
                    target.getY() + 0.5D,
                    target.getZ() + 0.5D,
                    stack.copy()
            ));
            return true;
        }
        return false;
    }

    private int inserterDelay() {
        return hasUpgrade(ModMachineItems.INSERTER_SPEED_UPGRADE.get()) ? 5 : 10;
    }

    private int inserterStackSize() {
        return hasUpgrade(ModMachineItems.INSERTER_STACK_UPGRADE.get()) ? 64 : 8;
    }

    private boolean hasUpgrade(net.minecraft.world.item.Item item) {
        return machine.inventory() != null
                && (machine.inventory().getStackInSlot(1).is(item)
                || machine.inventory().getStackInSlot(2).is(item));
    }
}
