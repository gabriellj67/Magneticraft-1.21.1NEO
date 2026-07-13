package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.system.network.logistics.ItemHandlerTransactions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
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
            SingleBlockMachineSupport.pushFluid(
                    machine,
                    Direction.DOWN,
                    machine.primaryTank(),
                    machine.primaryTank().tank().getFluidAmount()
            );
        } else if (machine.definition() == SingleBlockMachineDefinition.FILTER) {
            tickPneumaticFilter(level);
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
            ItemStack remainder = deliverInserterStack(level, carried);
            if (remainder.getCount() < carried.getCount()) {
                machine.inventory().setStackInSlot(0, remainder);
                state.cooldown = inserterDelay();
                state.working = true;
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
        if (level.getGameTime() % 5L != 0L
                || machine.inventory() == null
                || machine.pneumaticEndpoint() == null) {
            return;
        }
        ejectPneumaticOutput(level);
        PneumaticEndpointModule endpoint = machine.pneumaticEndpoint();
        if (!endpoint.canBufferOutput()) {
            return;
        }
        for (int slot = 0; slot < machine.inventory().slots(); slot++) {
            ItemStack candidate = machine.inventory().extractInternal(slot, 64, true);
            if (candidate.isEmpty()) {
                continue;
            }
            ItemStack extracted = machine.inventory().extractInternal(slot, candidate.getCount(), false);
            if (!endpoint.enqueueOutput(extracted)) {
                machine.inventory().menuHandler().insertItem(slot, extracted, false);
                return;
            }
            state.working = true;
            return;
        }
    }

    void tickTransposer(ServerLevel level) {
        if (level.getGameTime() % 5L != 0L) {
            return;
        }
        if (machine.pneumaticEndpoint() == null) {
            return;
        }
        ejectPneumaticOutput(level);
        PneumaticEndpointModule endpoint = machine.pneumaticEndpoint();
        if (!endpoint.canBufferOutput()) {
            return;
        }
        Direction input = SingleBlockMachineSupport.facing(machine).getOpposite();
        IItemHandler source = loadedBlockHandler(level, machine.getBlockPos().relative(input), input.getOpposite());
        if (source != null) {
            if (bufferFromInventory(source, endpoint)) {
                state.working = true;
            }
            return;
        }
        if (!isChunkLoaded(level, machine.getBlockPos().relative(input))) {
            return;
        }
        AABB area = new AABB(machine.getBlockPos().relative(input));
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
            ItemStack candidate = itemEntity.getItem();
            if (!SingleBlockMachineSupport.filterAllows(machine, state, candidate, true)) {
                continue;
            }
            int moved = Math.min(64, candidate.getCount());
            if (!endpoint.enqueueOutput(candidate.copyWithCount(moved))) {
                return;
            }
            candidate.shrink(moved);
            if (candidate.isEmpty()) {
                itemEntity.discard();
            }
            state.working = true;
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

    private boolean bufferFromInventory(IItemHandler source, PneumaticEndpointModule endpoint) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack candidate = source.extractItem(slot, 64, true);
            if (candidate.isEmpty()
                    || !SingleBlockMachineSupport.filterAllows(machine, state, candidate, true)) {
                continue;
            }
            ItemStack extracted = source.extractItem(slot, candidate.getCount(), false);
            if (!endpoint.enqueueOutput(extracted)) {
                source.insertItem(slot, extracted, false);
                return false;
            }
            machine.markChanged();
            return true;
        }
        return false;
    }

    private void tickPneumaticFilter(ServerLevel level) {
        PneumaticEndpointModule endpoint = machine.pneumaticEndpoint();
        if (endpoint == null) {
            return;
        }
        endpoint.moveInputsToOutput();
        // Forward-compatible migration of the pre-0.4 single-slot implementation.
        if (machine.inventory() != null && endpoint.canBufferOutput()) {
            ItemStack legacyBuffer = machine.inventory().getStackInSlot(0);
            if (!legacyBuffer.isEmpty()) {
                ItemStack extracted = machine.inventory().extractInternal(0, legacyBuffer.getCount(), false);
                if (!endpoint.enqueueOutput(extracted)) {
                    machine.inventory().menuHandler().insertItem(0, extracted, false);
                }
            }
        }
        if (level.getGameTime() % 5L == 0L) {
            ejectPneumaticOutput(level);
        }
    }

    private void ejectPneumaticOutput(ServerLevel level) {
        PneumaticEndpointModule endpoint = machine.pneumaticEndpoint();
        if (endpoint == null) {
            return;
        }
        for (int payload = 0; payload < PneumaticEndpointModule.MAX_PAYLOADS; payload++) {
            ItemStack stack = endpoint.outputHead();
            if (stack == null) {
                endpoint.clearOutputBlocked();
                return;
            }
            ItemStack remainder = insertOrDropPneumaticOutput(level, stack);
            if (remainder.getCount() >= stack.getCount()) {
                endpoint.markOutputBlocked();
                return;
            }
            endpoint.replaceOutputHead(remainder);
            state.working = true;
            if (!remainder.isEmpty()) {
                endpoint.markOutputBlocked();
                return;
            }
        }
    }

    private ItemStack insertOrDropPneumaticOutput(ServerLevel level, ItemStack stack) {
        Direction output = SingleBlockMachineSupport.facing(machine);
        BlockPos target = machine.getBlockPos().relative(output);
        if (!isChunkLoaded(level, target)) {
            return stack;
        }
        BlockEntity targetEntity = loadedBlockEntity(level, target);
        if (targetEntity != null) {
            IItemHandler handler = targetEntity
                    .getCapability(ForgeCapabilities.ITEM_HANDLER, output.getOpposite())
                    .orElse(null);
            if (handler == null) {
                return stack;
            }
            ItemStack simulated = ItemHandlerTransactions.insert(handler, stack.copy(), true);
            if (simulated.getCount() >= stack.getCount()) {
                return stack;
            }
            return ItemHandlerTransactions.insert(handler, stack.copy(), false);
        }

        BlockState targetState = level.getBlockState(target);
        if (!targetState.getCollisionShape(level, target).isEmpty()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            return stack;
        }
        ItemEntity entity = new ItemEntity(
                level,
                target.getX() + 0.5D,
                target.getY() + 0.5D,
                target.getZ() + 0.5D,
                stack.copy()
        );
        entity.setDeltaMovement(
                output.getStepX() * 0.15D,
                output.getStepY() * 0.15D,
                output.getStepZ() * 0.15D
        );
        return level.addFreshEntity(entity) ? ItemStack.EMPTY : stack;
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

    private ItemStack extractForInserter(ServerLevel level) {
        Direction sourceDirection = SingleBlockMachineSupport.facing(machine);
        int maximum = inserterStackSize();
        for (IItemHandler source : inserterHandlers(level, sourceDirection)) {
            for (int slot = 0; slot < source.getSlots(); slot++) {
                ItemStack candidate = source.extractItem(slot, maximum, true);
                if (candidate.isEmpty()
                        || !SingleBlockMachineSupport.filterAllows(
                        machine,
                        state,
                        candidate,
                        state.inserterWhitelist
                )) {
                    continue;
                }
                int accepted = acceptedByInserterDestination(level, candidate);
                if (accepted > 0) {
                    return source.extractItem(slot, Math.min(accepted, candidate.getCount()), false);
                }
            }
        }
        if (state.inserterGrabItems) {
            for (BlockPos sourcePosition : SingleBlockMachineSupport.inserterGroundPickupPositions(
                    machine.getBlockPos(), sourceDirection
            )) {
                if (!isChunkLoaded(level, sourcePosition)) {
                    continue;
                }
                AABB area = new AABB(sourcePosition);
                for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area, ItemEntity::isAlive)) {
                    ItemStack stack = entity.getItem();
                    if (!SingleBlockMachineSupport.filterAllows(
                            machine,
                            state,
                            stack,
                            state.inserterWhitelist
                    )) {
                        continue;
                    }
                    ItemStack offered = stack.copyWithCount(Math.min(maximum, stack.getCount()));
                    int moved = acceptedByInserterDestination(level, offered);
                    if (moved <= 0) {
                        continue;
                    }
                    ItemStack result = stack.copyWithCount(Math.min(moved, offered.getCount()));
                    stack.shrink(result.getCount());
                    if (stack.isEmpty()) {
                        entity.discard();
                    }
                    return result;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack deliverInserterStack(ServerLevel level, ItemStack stack) {
        Direction output = SingleBlockMachineSupport.facing(machine).getOpposite();
        for (IItemHandler target : inserterHandlers(level, output)) {
            ItemStack simulated = ItemHandlerTransactions.insert(target, stack.copy(), true);
            if (simulated.getCount() < stack.getCount()) {
                return ItemHandlerTransactions.insert(target, stack.copy(), false);
            }
        }
        BlockPos dropPosition = state.inserterDropItems ? inserterDropPosition(level, output) : null;
        if (dropPosition != null) {
            boolean spawned = level.addFreshEntity(new ItemEntity(
                    level,
                    dropPosition.getX() + 0.5D,
                    dropPosition.getY() + 0.5D,
                    dropPosition.getZ() + 0.5D,
                    stack.copy()
            ));
            return spawned ? ItemStack.EMPTY : stack;
        }
        return stack;
    }

    private int acceptedByInserterDestination(ServerLevel level, ItemStack stack) {
        Direction output = SingleBlockMachineSupport.facing(machine).getOpposite();
        for (IItemHandler target : inserterHandlers(level, output)) {
            ItemStack remainder = ItemHandlerTransactions.insert(target, stack.copy(), true);
            int accepted = stack.getCount() - remainder.getCount();
            if (accepted > 0) {
                return accepted;
            }
        }
        return state.inserterDropItems && inserterDropPosition(level, output) != null ? stack.getCount() : 0;
    }

    private List<IItemHandler> inserterHandlers(ServerLevel level, Direction direction) {
        java.util.ArrayList<IItemHandler> handlers = new java.util.ArrayList<>();
        for (SingleBlockMachineSupport.InserterAccess accessor
                : SingleBlockMachineSupport.inserterInventoryAccesses(machine.getBlockPos(), direction)) {
            BlockPos position = accessor.position();
            if (!isChunkLoaded(level, position)) {
                continue;
            }
            if (accessor.includeMinecarts()) {
                AABB area = new AABB(position);
                for (AbstractMinecartContainer minecart : level.getEntitiesOfClass(
                        AbstractMinecartContainer.class,
                        area,
                        AbstractMinecartContainer::isAlive
                )) {
                    IItemHandler handler = minecart
                            .getCapability(ForgeCapabilities.ITEM_HANDLER, accessor.side())
                            .orElse(null);
                    if (handler != null) {
                        handlers.add(handler);
                    }
                }
            }
            IItemHandler blockHandler = loadedBlockHandler(level, position, accessor.side());
            if (blockHandler != null) {
                handlers.add(blockHandler);
            }
        }
        return List.copyOf(handlers);
    }

    @javax.annotation.Nullable
    private BlockPos inserterDropPosition(ServerLevel level, Direction output) {
        for (BlockPos target : SingleBlockMachineSupport.inserterGroundDropPositions(
                machine.getBlockPos(), output
        )) {
            if (canInserterDropAt(level, target)) {
                return target;
            }
        }
        return null;
    }

    private boolean canInserterDropAt(ServerLevel level, BlockPos target) {
        if (!isChunkLoaded(level, target)
                || !level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)
                || !level.getBlockState(target).getCollisionShape(level, target).isEmpty()) {
            return false;
        }
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(target), ItemEntity::isAlive).isEmpty();
    }

    @javax.annotation.Nullable
    private static IItemHandler loadedBlockHandler(ServerLevel level, BlockPos position, Direction side) {
        BlockEntity blockEntity = loadedBlockEntity(level, position);
        return blockEntity == null
                ? null
                : blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }

    @javax.annotation.Nullable
    private static BlockEntity loadedBlockEntity(ServerLevel level, BlockPos position) {
        var chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(position);
    }

    private static boolean isChunkLoaded(ServerLevel level, BlockPos position) {
        return level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) != null;
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
