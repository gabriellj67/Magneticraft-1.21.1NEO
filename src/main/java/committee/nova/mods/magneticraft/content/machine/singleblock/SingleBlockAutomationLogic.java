package committee.nova.mods.magneticraft.content.machine.singleblock;

import com.mojang.authlib.GameProfile;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.content.world.ProtectedWorldMutation;
import committee.nova.mods.magneticraft.system.network.logistics.ItemHandlerTransactions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Item, fluid and animal automation behavior for the Task 5 utility machines.
 */
final class SingleBlockAutomationLogic {
    private static final int FEEDING_TROUGH_INTERVAL = 400;
    private static final int BLOCK_BREAKER_INTERVAL = 20;
    private static final int BLOCK_BREAKER_RANGE = 16;
    private static final int BLOCK_BREAKER_ENERGY_COST = 500;
    private static final int SPRINKLER_INTERVAL = 20;
    private static final int SPRINKLER_RADIUS = 3;
    private static final int SPRINKLER_DEPTH = 7;
    private static final GameProfile FEEDING_TROUGH_PROFILE = new GameProfile(
            UUID.fromString("d0f15bc8-6eb3-4a1b-8b5d-d3fdf5140321"),
            "FeedingTrough"
    );

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
            if (loadedBlockEntity(level, next) instanceof SingleBlockMachineBlockEntity nextMachine
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
        if (Math.floorMod(
                level.getGameTime() + machine.getBlockPos().hashCode(),
                FEEDING_TROUGH_INTERVAL
        ) != 0 || machine.inventory() == null) {
            return;
        }
        ItemStack food = machine.inventory().getStackInSlot(0);
        if (food.isEmpty()) {
            return;
        }
        List<Animal> animals = level.getEntitiesOfClass(
                Animal.class,
                feedingTroughArea(),
                Animal::isAlive
        );
        if (animals.size() >= 30) {
            return;
        }
        List<Animal> eligible = new ArrayList<>();
        for (Animal animal : animals) {
            if (animal.canFallInLove() && animal.isFood(food)) {
                eligible.add(animal);
            }
        }
        if (eligible.size() < 2) {
            return;
        }
        for (int fed = 0; fed < 2; fed++) {
            Animal animal = eligible.remove(level.random.nextInt(eligible.size()));
            machine.inventory().extractInternal(0, 1, false);
            animal.setInLove(FakePlayerFactory.get(level, FEEDING_TROUGH_PROFILE));
        }
        machine.markChangedAndSync();
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

    void tickBlockBreaker(ServerLevel level) {
        if (Math.floorMod(level.getGameTime() + machine.getBlockPos().hashCode(), BLOCK_BREAKER_INTERVAL) != 0
                || level.hasNeighborSignal(machine.getBlockPos())
                || machine.inventory() == null
                || machine.energy() == null
                || machine.energy().consumeJoules(BLOCK_BREAKER_ENERGY_COST, true) != BLOCK_BREAKER_ENERGY_COST) {
            return;
        }
        Direction direction = SingleBlockMachineSupport.facing(machine);
        for (int distance = 1; distance <= BLOCK_BREAKER_RANGE; distance++) {
            BlockPos target = machine.getBlockPos().relative(direction, distance);
            if (!ProtectedWorldMutation.isSafeLoadedTarget(level, target)) {
                return;
            }
            BlockState targetState = level.getBlockState(target);
            if (targetState.is(ModMachineBlocks.PERMANENT_MAGNET.get())) {
                return;
            }
            if (!targetState.isAir()) {
                breakBlock(level, target, targetState, direction.getOpposite());
                return;
            }
        }
    }

    private void breakBlock(ServerLevel level, BlockPos target, BlockState targetState, Direction face) {
        if (level.getBlockEntity(target) != null || targetState.getDestroySpeed(level, target) < 0.0F) {
            return;
        }
        FakePlayer player = ProtectedWorldMutation.ownerPlayer(level, state.owner, machine.getBlockPos());
        if (player == null || !ProtectedWorldMutation.mayModify(level, player, machine.getBlockPos(), target, face)) {
            return;
        }
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        if (targetState.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(targetState)) {
            return;
        }
        ItemStackHandler staged = copyInventory();
        List<ItemStack> drops = Block.getDrops(targetState, level, target, null, player, tool);
        for (ItemStack drop : drops) {
            if (!SingleBlockMachineSupport.filterAllows(machine, state, drop, true)
                    || !ItemHandlerHelper.insertItemStacked(staged, drop.copy(), false).isEmpty()) {
                return;
            }
        }
        ItemStack previousTool = player.getMainHandItem();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
        try {
            if (ForgeHooks.onBlockBreakEvent(level, GameType.SURVIVAL, player, target) == -1
                    || !level.getBlockState(target).equals(targetState)) {
                return;
            }
            double energyBefore = machine.energy().storedJoules();
            if (machine.energy().consumeJoules(BLOCK_BREAKER_ENERGY_COST, false) != BLOCK_BREAKER_ENERGY_COST) {
                return;
            }
            if (!level.destroyBlock(target, false, player)) {
                machine.energy().restoreStoredJoules(energyBefore);
                return;
            }
            IItemHandlerModifiable inventory = machine.inventory().menuHandler();
            for (int slot = 0; slot < inventory.getSlots(); slot++) {
                inventory.setStackInSlot(slot, staged.getStackInSlot(slot).copy());
            }
            state.lastConsumption = BLOCK_BREAKER_ENERGY_COST;
            state.recordWorking(level.getGameTime());
            machine.markChangedAndSync();
        } finally {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, previousTool);
        }
    }

    private ItemStackHandler copyInventory() {
        ItemStackHandler copy = new ItemStackHandler(machine.inventory().slots());
        for (int slot = 0; slot < copy.getSlots(); slot++) {
            copy.setStackInSlot(slot, machine.inventory().getStackInSlot(slot).copy());
        }
        return copy;
    }

    void tickSprinkler(ServerLevel level) {
        if (Math.floorMod(level.getGameTime() + machine.getBlockPos().hashCode(), SPRINKLER_INTERVAL) != 0
                || machine.primaryTank() == null
                || machine.primaryTank().tank().getFluidAmount() <= 0) {
            return;
        }
        FakePlayer player = ProtectedWorldMutation.ownerPlayer(level, state.owner, machine.getBlockPos());
        if (player == null) {
            return;
        }
        int serviced = 0;
        for (int x = -SPRINKLER_RADIUS; x <= SPRINKLER_RADIUS; x++) {
            for (int z = -SPRINKLER_RADIUS; z <= SPRINKLER_RADIUS; z++) {
                if (serviced >= machine.primaryTank().tank().getFluidAmount()) {
                    break;
                }
                if (serviceSprinklerColumn(level, player, x, z)) {
                    serviced++;
                }
            }
        }
        if (serviced > 0) {
            machine.primaryTank().tank().drain(serviced, IFluidHandler.FluidAction.EXECUTE);
            state.lastConsumption = serviced;
            state.recordWorking(level.getGameTime());
            machine.markChangedAndSync();
        }
    }

    private boolean serviceSprinklerColumn(ServerLevel level, FakePlayer player, int x, int z) {
        for (int depth = 1; depth <= SPRINKLER_DEPTH; depth++) {
            BlockPos target = machine.getBlockPos().offset(x, -depth, z);
            if (!ProtectedWorldMutation.isSafeLoadedTarget(level, target)) {
                return false;
            }
            BlockState targetState = level.getBlockState(target);
            if (targetState.isAir()) {
                continue;
            }
            if (!ProtectedWorldMutation.mayModify(
                    level, player, machine.getBlockPos(), target, Direction.UP
            )) {
                return false;
            }
            boolean changed = false;
            if (targetState.getBlock() instanceof FarmBlock
                    && targetState.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
                changed = level.setBlock(target, targetState.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
            }
            BlockPos cropPosition = target.above();
            BlockState crop = level.getBlockState(cropPosition);
            if (crop.getBlock() instanceof BonemealableBlock growable
                    && level.random.nextFloat() < 0.002F
                    && ProtectedWorldMutation.isSafeLoadedTarget(level, cropPosition)
                    && ProtectedWorldMutation.mayModify(
                    level, player, machine.getBlockPos(), cropPosition, Direction.UP
            ) && growable.isValidBonemealTarget(level, cropPosition, crop, false)
                    && growable.isBonemealSuccess(level, level.random, cropPosition, crop)) {
                growable.performBonemeal(level, level.random, cropPosition, crop);
                changed = true;
            }
            return changed;
        }
        return false;
    }

    void tickWaterGenerator() {
        if (machine.primaryTank() == null) {
            return;
        }
        int perSide = MagneticraftConfig.WATER_GENERATOR_PER_TICK_WATER.get();
        for (Direction direction : Direction.values()) {
            SingleBlockMachineSupport.pushFluid(machine, direction, machine.primaryTank(), perSide);
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
        state.progress = SingleBlockMachineBlockEntity.SLUICE_DURATION;
        state.totalProgress = SingleBlockMachineBlockEntity.SLUICE_DURATION;
        state.chainDelay = 20;
        machine.markChangedAndSync();
    }

    private AABB feedingTroughArea() {
        BlockPos position = machine.getBlockPos();
        double minX = position.getX() - 3.5D;
        double minY = position.getY() - 1.0D;
        double minZ = position.getZ() - 3.5D;
        double maxX = position.getX() + 4.5D;
        double maxY = position.getY() + 2.0D;
        double maxZ = position.getZ() + 4.5D;
        switch (SingleBlockMachineSupport.facing(machine)) {
            case DOWN -> minY -= 1.0D;
            case UP -> maxY += 1.0D;
            case NORTH -> minZ -= 1.0D;
            case SOUTH -> maxZ += 1.0D;
            case WEST -> minX -= 1.0D;
            case EAST -> maxX += 1.0D;
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
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
