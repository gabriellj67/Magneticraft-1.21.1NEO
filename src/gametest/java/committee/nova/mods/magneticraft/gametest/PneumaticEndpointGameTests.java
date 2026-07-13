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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        SingleBlockMachineMenu menu = new SingleBlockMachineMenu(1, player.getInventory(), filter);

        menu.setCarried(new ItemStack(Items.IRON_INGOT, 3));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 3, "Virtual filter sample consumed a held item");
        helper.assertTrue(filter.filters().getFilter(0).is(Items.IRON_INGOT)
                        && filter.filters().getFilter(0).getCount() == 1,
                "Filter sample was not normalized to one virtual item");

        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(0, 0, ClickType.PICKUP, player);
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
        SingleBlockMachineMenu menu = new SingleBlockMachineMenu(2, player.getInventory(), inserter);

        helper.assertTrue(inserter.filters() != null, "Inserter did not create its virtual filter samples");
        menu.setCarried(new ItemStack(Items.GOLD_INGOT, 3));
        menu.clicked(3, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 3, "Inserter virtual sample consumed the held item");
        helper.assertTrue(inserter.filters().getFilter(0).is(Items.GOLD_INGOT),
                "Inserter did not retain the virtual sample");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(3, 0, ClickType.PICKUP, player);
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
        input.insertItem(0, new ItemStack(Items.IRON_INGOT, 3), false);
        input.insertItem(0, new ItemStack(Items.GOLD_INGOT, 2), false);
        net.minecraft.nbt.CompoundTag saved = filter.saveWithoutMetadata();
        filter.load(saved);

        helper.runAfterDelay(8, () -> {
            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity)
                    helper.getBlockEntity(CENTER.east());
            helper.assertTrue(chest.getItem(0).is(Items.IRON_INGOT) && chest.getItem(0).getCount() == 3,
                    "Reloaded endpoint changed the FIFO head");
            helper.assertTrue(chest.getItem(1).is(Items.GOLD_INGOT) && chest.getItem(1).getCount() == 2,
                    "Reloaded endpoint changed the FIFO tail");
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

    @GameTest(template = TEMPLATE, timeoutTicks = 45)
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
        input.insertItem(0, new ItemStack(Items.IRON_INGOT), false);

        helper.runAfterDelay(8, () -> {
            helper.assertTrue(input.insertItem(0, new ItemStack(Items.IRON_INGOT), true).getCount() == 1,
                    "Full target did not backpressure filter input");
            net.minecraft.nbt.CompoundTag saved = filter.saveWithoutMetadata();
            filter.load(saved);
        });
        helper.runAfterDelay(16, () -> {
            var restoredInput = filter.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.WEST)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(restoredInput.insertItem(0, new ItemStack(Items.IRON_INGOT), true).getCount() == 1,
                    "Reloaded filter did not re-derive backpressure from its still-full target");
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

        helper.runAfterDelay(25, () -> {
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
        });
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

    private static PneumaticTubeBlockEntity tube(GameTestHelper helper, BlockPos position) {
        Object blockEntity = helper.getBlockEntity(position);
        helper.assertTrue(blockEntity instanceof PneumaticTubeBlockEntity, "Missing pneumatic tube");
        return (PneumaticTubeBlockEntity) blockEntity;
    }
}
