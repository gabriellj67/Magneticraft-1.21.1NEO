package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlockEntity;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.network.SetGhostFilterMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** End-to-end ownership, backpressure and topology contracts for pneumatic automation. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PneumaticEndpointGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos CENTER = new BlockPos(1, 1, 1);

    private PneumaticEndpointGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void pneumaticFilterSamplesAreNonOwningAndNeverDrop(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FILTER, Direction.EAST));
        SingleBlockMachineBlockEntity filter = machine(helper, CENTER);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(
                filter.getBlockPos().getX() + 0.5D,
                filter.getBlockPos().getY() + 0.5D,
                filter.getBlockPos().getZ() + 0.5D
        );
        SingleBlockMachineMenu menu = new SingleBlockMachineMenu(1, player.getInventory(), filter);
        player.containerMenu = menu;

        menu.setCarried(new ItemStack(Items.IRON_INGOT, 3));
        helper.assertTrue(SetGhostFilterMessage.applyIfValid(
                player,
                new SetGhostFilterMessage(filter.getBlockPos(), 0, menu.getCarried())
        ), "Validated filter message was rejected");
        helper.assertTrue(menu.getCarried().getCount() == 3, "Virtual filter sample consumed a held item");
        helper.assertTrue(filter.filters().getFilter(0).is(Items.IRON_INGOT)
                        && filter.filters().getFilter(0).getCount() == 1,
                "Filter sample was not normalized to one virtual item");

        menu.setCarried(ItemStack.EMPTY);
        helper.assertTrue(SetGhostFilterMessage.applyIfValid(
                player,
                new SetGhostFilterMessage(filter.getBlockPos(), 0, ItemStack.EMPTY)
        ), "Validated filter clearing message was rejected");
        helper.assertTrue(menu.getCarried().isEmpty(), "Clearing a virtual filter returned a duplicated item");
        helper.assertTrue(filter.filters().getFilter(0).isEmpty(), "Virtual filter sample did not clear");

        filter.filters().setFilter(0, new ItemStack(Items.IRON_INGOT));
        filter.dropContents(helper.getLevel());
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(helper.absolutePos(CENTER)).inflate(1.0D),
                entity -> entity.getItem().is(Items.IRON_INGOT)
        ).isEmpty(), "Breaking a filter materialized its virtual sample");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void inserterFilterSamplesAreAlsoNonOwning(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.INSERTER, Direction.EAST));
        SingleBlockMachineBlockEntity inserter = machine(helper, CENTER);
        Player player = helper.makeMockSurvivalPlayer();
        player.setPos(
                inserter.getBlockPos().getX() + 0.5D,
                inserter.getBlockPos().getY() + 0.5D,
                inserter.getBlockPos().getZ() + 0.5D
        );
        SingleBlockMachineMenu menu = new SingleBlockMachineMenu(2, player.getInventory(), inserter);
        player.containerMenu = menu;

        helper.assertTrue(inserter.filters() != null, "Inserter did not create its virtual filter samples");
        menu.setCarried(new ItemStack(Items.GOLD_INGOT, 3));
        helper.assertTrue(SetGhostFilterMessage.applyIfValid(
                player,
                new SetGhostFilterMessage(inserter.getBlockPos(), 0, menu.getCarried())
        ), "Validated inserter filter message was rejected");
        helper.assertTrue(menu.getCarried().getCount() == 3, "Inserter virtual sample consumed the held item");
        helper.assertTrue(inserter.filters().getFilter(0).is(Items.GOLD_INGOT),
                "Inserter did not retain the virtual sample");
        menu.setCarried(ItemStack.EMPTY);
        helper.assertTrue(SetGhostFilterMessage.applyIfValid(
                player,
                new SetGhostFilterMessage(inserter.getBlockPos(), 0, ItemStack.EMPTY)
        ), "Validated inserter clearing message was rejected");
        helper.assertTrue(menu.getCarried().isEmpty() && inserter.filters().getFilter(0).isEmpty(),
                "Clearing an inserter sample returned a duplicated item");

        inserter.filters().setFilter(0, new ItemStack(Items.GOLD_INGOT));
        inserter.dropContents(helper.getLevel());
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(helper.absolutePos(CENTER)).inflate(1.0D),
                entity -> entity.getItem().is(Items.GOLD_INGOT)
        ).isEmpty(), "Breaking an inserter materialized its virtual sample");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void inserterCarrySlotRejectsUpgrades(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.INSERTER, Direction.EAST));
        SingleBlockMachineBlockEntity inserter = machine(helper, CENTER);
        ItemStack upgrade = new ItemStack(ModMachineItems.INSERTER_SPEED_UPGRADE.get());

        ItemStack remainder = inserter.inventory().menuHandler().insertItem(0, upgrade, false);
        helper.assertTrue(remainder.getCount() == 1 && inserter.inventory().getStackInSlot(0).isEmpty(),
                "Inserter carry slot accepted an upgrade item");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void pneumaticFilterInputSimulationIsPureAndBounded(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FILTER, Direction.EAST));
        SingleBlockMachineBlockEntity filter = machine(helper, CENTER);
        var input = filter.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                .orElseThrow(AssertionError::new);

        helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT), true).isEmpty(),
                "Filter rejected a simulated valid payload");
        for (int payload = 0; payload < 64; payload++) {
            helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty(),
                    "Simulation consumed queue capacity or the bounded queue filled early");
        }
        helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT), true).getCount() == 1,
                "Full endpoint queue did not apply simulated backpressure");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void pneumaticFilterFifoSurvivesPersistence(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FILTER, Direction.EAST));
        helper.setBlock(CENTER.east(), net.minecraft.world.level.block.Blocks.CHEST);
        SingleBlockMachineBlockEntity filter = machine(helper, CENTER);
        var input = filter.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                .orElseThrow(AssertionError::new);
        filter.filters().setFilter(0, new ItemStack(Items.IRON_INGOT));
        filter.filters().setFilter(1, new ItemStack(Items.GOLD_INGOT));
        helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT, 3), false).isEmpty(),
                "Filter rejected its first FIFO payload");
        helper.assertTrue(input.insertItem(0, new ItemStack(Items.GOLD_INGOT, 2), false).isEmpty(),
                "Filter rejected its second FIFO payload");
        CompoundTag saved = filter.saveWithoutMetadata();
        SingleBlockMachineBlockEntity restored = restoreMachine(helper, CENTER, filter, saved);
        helper.assertTrue(restored != filter, "Persistence fixture reused the original filter block entity");
        helper.assertTrue(
                restored.filters().getFilter(0).is(Items.IRON_INGOT)
                        && restored.filters().getFilter(1).is(Items.GOLD_INGOT),
                "Reloaded filter lost its persistent filter samples"
        );

        helper.runAfterDelay(8, () -> {
            ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(CENTER.east());
            helper.assertTrue(chest.getItem(0).is(Items.IRON_INGOT) && chest.getItem(0).getCount() == 3,
                    "Reloaded endpoint changed the FIFO head");
            helper.assertTrue(chest.getItem(1).is(Items.GOLD_INGOT) && chest.getItem(1).getCount() == 2,
                    "Reloaded endpoint changed the FIFO tail");
            helper.assertTrue(countItem(chest, Items.IRON_INGOT) == 3
                            && countItem(chest, Items.GOLD_INGOT) == 2
                            && countAllItems(chest) == 5,
                    "Reloaded filter duplicated or lost a FIFO payload");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void transposerDoesNotFallBackToEntitiesBehindAnInventory(GameTestHelper helper) {
        BlockPos input = CENTER.west();
        helper.setBlock(input, net.minecraft.world.level.block.Blocks.CHEST);
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.TRANSPOSER, Direction.EAST));
        BlockPos absoluteInput = helper.absolutePos(input);
        ItemEntity looseItem = new ItemEntity(
                helper.getLevel(),
                absoluteInput.getX() + 0.5D,
                absoluteInput.getY() + 0.5D,
                absoluteInput.getZ() + 0.5D,
                new ItemStack(Items.IRON_INGOT)
        );
        looseItem.setNoGravity(true);
        helper.getLevel().addFreshEntity(looseItem);

        helper.runAfterDelay(12, () -> {
            helper.assertTrue(looseItem.isAlive() && looseItem.getItem().getCount() == 1,
                    "Transposer picked up an entity despite an adjacent inventory handler");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 55)
    public static void blockedFilterRebuildsBackpressureAfterReload(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.FILTER, Direction.EAST));
        helper.setBlock(CENTER.east(), net.minecraft.world.level.block.Blocks.CHEST);
        SingleBlockMachineBlockEntity filter = machine(helper, CENTER);
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(CENTER.east());
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
        var input = filter.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                .orElseThrow(AssertionError::new);
        helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT), false).isEmpty(),
                "Filter rejected the payload used by the reload fixture");
        SingleBlockMachineBlockEntity[] restored = new SingleBlockMachineBlockEntity[1];

        helper.runAfterDelay(8, () -> {
            helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT), true).getCount() == 1,
                    "Full target did not backpressure filter input");
            CompoundTag saved = filter.saveWithoutMetadata();
            restored[0] = restoreMachine(helper, CENTER, filter, saved);
            helper.assertTrue(restored[0] != filter,
                    "Backpressure fixture reused the original filter block entity");
        });
        helper.runAfterDelay(16, () -> {
            var restoredInput = restored[0].getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(restoredInput.insertItem(0, new ItemStack(Items.IRON_INGOT), true).getCount() == 1,
                    "Reloaded filter did not re-derive backpressure from its still-full target");
            chest.setItem(0, ItemStack.EMPTY);
        });
        helper.runAfterDelay(24, () -> {
            var restoredInput = restored[0].getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(countItem(chest, Items.IRON_INGOT) == 1,
                    "Reloaded blocked filter duplicated or lost its queued payload");
            helper.assertTrue(restoredInput.insertItem(0, new ItemStack(Items.IRON_INGOT), true).isEmpty(),
                    "Reloaded filter did not clear backpressure after delivering its only payload");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 65)
    public static void relayAndTransposerQueuesSurviveNewBlockEntityRestoreExactlyOnce(GameTestHelper helper) {
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.RELAY, Direction.EAST));
        helper.setBlock(CENTER.east(), Blocks.STONE);
        SingleBlockMachineBlockEntity relay = machine(helper, CENTER);
        relay.inventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 3));
        relay.inventory().setStackInSlot(1, new ItemStack(Items.DIAMOND, 4));

        helper.runAfterDelay(8, () -> {
            helper.assertTrue(relay.inventory().getStackInSlot(0).isEmpty(),
                    "Relay did not move its first stack into the persistent output queue");
            CompoundTag saved = relay.saveWithoutMetadata();
            SingleBlockMachineBlockEntity restoredRelay = restoreMachine(helper, CENTER, relay, saved);
            helper.assertTrue(restoredRelay.inventory().getStackInSlot(1).is(Items.DIAMOND)
                            && restoredRelay.inventory().getStackInSlot(1).getCount() == 4,
                    "Reloaded relay lost its persistent inventory state");
            helper.setBlock(CENTER.east(), Blocks.CHEST);
        });
        helper.runAfterDelay(22, () -> {
            ChestBlockEntity target = (ChestBlockEntity) helper.getBlockEntity(CENTER.east());
            helper.assertTrue(countItem(target, Items.IRON_INGOT) == 3
                            && countItem(target, Items.DIAMOND) == 4
                            && countAllItems(target) == 7,
                    "Reloaded relay duplicated or lost inventory and queued payloads");
            target.clearContent();
            helper.setBlock(CENTER.east(), Blocks.STONE);
            helper.setBlock(CENTER.west(), Blocks.CHEST);
            ChestBlockEntity source = (ChestBlockEntity) helper.getBlockEntity(CENTER.west());
            source.setItem(0, new ItemStack(Items.GOLD_INGOT, 2));
            helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.TRANSPOSER, Direction.EAST));
            SingleBlockMachineBlockEntity transposer = machine(helper, CENTER);
            transposer.filters().setFilter(0, new ItemStack(Items.GOLD_INGOT));
        });
        helper.runAfterDelay(34, () -> {
            ChestBlockEntity source = (ChestBlockEntity) helper.getBlockEntity(CENTER.west());
            helper.assertTrue(source.getItem(0).isEmpty(),
                    "Transposer did not move its source stack into the persistent output queue");
            SingleBlockMachineBlockEntity transposer = machine(helper, CENTER);
            CompoundTag saved = transposer.saveWithoutMetadata();
            SingleBlockMachineBlockEntity restored = restoreMachine(helper, CENTER, transposer, saved);
            helper.assertTrue(restored.filters().getFilter(0).is(Items.GOLD_INGOT),
                    "Reloaded transposer lost its persistent filter state");
            helper.setBlock(CENTER.east(), Blocks.CHEST);
        });
        helper.runAfterDelay(48, () -> {
            ChestBlockEntity target = (ChestBlockEntity) helper.getBlockEntity(CENTER.east());
            helper.assertTrue(countItem(target, Items.GOLD_INGOT) == 2 && countAllItems(target) == 2,
                    "Reloaded transposer duplicated or lost its queued payload");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void unregisteredAdjacentTubeIsNeverAnExternalRoute(GameTestHelper helper) {
        helper.setBlock(CENTER, ModNetworkBlocks.PNEUMATIC_TUBE.get());
        helper.setBlock(CENTER.east(), ModNetworkBlocks.PNEUMATIC_TUBE.get());
        PneumaticTubeBlockEntity source = tube(helper, CENTER);
        PneumaticTubeBlockEntity target = tube(helper, CENTER.east());
        target.setRemoved();
        target.clearRemoved();
        var input = source.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                .orElseThrow(AssertionError::new);
        input.insertItem(0, new ItemStack(Items.IRON_INGOT), false);

        helper.runAfterDelay(20, () -> {
            helper.assertTrue(source.logistics().itemsSnapshot().size() == 1,
                    "Source treated an unregistered tube capability as an external destination");
            helper.assertTrue(target.logistics().itemsSnapshot().isEmpty(),
                    "Payload bypassed the runtime topology into an adjacent tube");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 65)
    public static void inserterBlankWhitelistRejectsAndBlankBlacklistAllows(GameTestHelper helper) {
        helper.setBlock(CENTER.east(), net.minecraft.world.level.block.Blocks.CHEST);
        helper.setBlock(CENTER.west(), net.minecraft.world.level.block.Blocks.CHEST);
        var source = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(CENTER.east());
        var target = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(CENTER.west());
        source.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.INSERTER, Direction.EAST));
        SingleBlockMachineBlockEntity inserter = machine(helper, CENTER);
        inserter.toggleInserterFlag(0);

        helper.runAfterDelay(25, () -> {
            helper.assertTrue(source.getItem(0).getCount() == 8 && target.getItem(0).isEmpty(),
                    "Blank inserter whitelist did not reject all items");
            inserter.toggleInserterFlag(0);
        });
        helper.runAfterDelay(52, () -> {
            helper.assertTrue(source.getItem(0).isEmpty() && target.getItem(0).getCount() == 8,
                    "Blank inserter blacklist did not allow all items");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void inserterPrefersLowerOpenDropPosition(GameTestHelper helper) {
        helper.setBlock(CENTER.east(), net.minecraft.world.level.block.Blocks.CHEST);
        var source = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getBlockEntity(CENTER.east());
        source.setItem(0, new ItemStack(Items.COPPER_INGOT, 8));
        helper.setBlock(CENTER, machineState(SingleBlockMachineDefinition.INSERTER, Direction.EAST));
        SingleBlockMachineBlockEntity inserter = machine(helper, CENTER);
        inserter.toggleInserterFlag(4);

        // Keep item physics from moving the entity away before its selected spawn position is observed.
        for (int tick = 0; tick < 25; tick++) {
            SingleBlockMachineBlockEntity.serverTick(
                    helper.getLevel(),
                    inserter.getBlockPos(),
                    inserter.getBlockState(),
                    inserter
            );
        }

        BlockPos lowerDrop = helper.absolutePos(CENTER.west().below());
        int dropped = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(lowerDrop),
                entity -> entity.getItem().is(Items.COPPER_INGOT)
        ).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        BlockPos sameLevelDrop = helper.absolutePos(CENTER.west());
        int droppedAtSameLevel = helper.getLevel().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(sameLevelDrop),
                entity -> entity.getItem().is(Items.COPPER_INGOT)
        ).stream().mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(source.getItem(0).isEmpty(), "Inserter did not extract for its lower drop position");
        helper.assertTrue(dropped == 8 && droppedAtSameLevel == 0,
                "Inserter did not prefer and conserve its stack at the lower drop position");
        helper.succeed();
    }

    private static BlockState machineState(SingleBlockMachineDefinition definition, Direction facing) {
        return ModMachineBlocks.machine(definition).get().defaultBlockState()
                .setValue(SingleBlockMachineBlock.FACING, facing)
                .setValue(SingleBlockMachineBlock.MASTER, true);
    }

    private static SingleBlockMachineBlockEntity machine(GameTestHelper helper, BlockPos position) {
        Object blockEntity = helper.getBlockEntity(position);
        helper.assertTrue(blockEntity instanceof SingleBlockMachineBlockEntity, "Missing single-block machine");
        return (SingleBlockMachineBlockEntity) blockEntity;
    }

    private static SingleBlockMachineBlockEntity restoreMachine(
            GameTestHelper helper,
            BlockPos position,
            SingleBlockMachineBlockEntity original,
            CompoundTag saved
    ) {
        BlockPos absolutePosition = helper.absolutePos(position);
        BlockState state = helper.getLevel().getBlockState(absolutePosition);
        helper.getLevel().removeBlockEntity(absolutePosition);
        SingleBlockMachineBlockEntity restored = new SingleBlockMachineBlockEntity(absolutePosition, state);
        restored.load(saved);
        helper.getLevel().setBlockEntity(restored);
        helper.assertTrue(original.isRemoved(), "Original block entity remained registered after replacement");
        helper.assertTrue(helper.getBlockEntity(position) == restored,
                "Restored block entity was not registered in the loaded level");
        return restored;
    }

    private static int countItem(ChestBlockEntity chest, Item item) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countAllItems(ChestBlockEntity chest) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            count += chest.getItem(slot).getCount();
        }
        return count;
    }

    private static PneumaticTubeBlockEntity tube(GameTestHelper helper, BlockPos position) {
        Object blockEntity = helper.getBlockEntity(position);
        helper.assertTrue(blockEntity instanceof PneumaticTubeBlockEntity, "Missing pneumatic tube");
        return (PneumaticTubeBlockEntity) blockEntity;
    }
}
