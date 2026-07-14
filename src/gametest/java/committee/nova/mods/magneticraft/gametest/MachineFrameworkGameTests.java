package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.item.HammerItem;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingTableBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlock;
import committee.nova.mods.magneticraft.content.machine.framework.menu.GhostFilterMenuAccess;
import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.GhostSlot;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.GhostFilterModule;
import committee.nova.mods.magneticraft.init.ModItems;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.network.SetGhostFilterMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime contracts for the module host, three representative devices, menus and C2S validation.
 */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MachineFrameworkGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos TEST_POS = new BlockPos(1, 1, 1);

    private MachineFrameworkGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void capabilitiesAreDirectionalAndPersistByModuleId(GameTestHelper helper) {
        helper.setBlock(TEST_POS, ModMachineBlocks.BATTERY.get());
        BatteryBlockEntity battery = requireBlockEntity(helper, TEST_POS, BatteryBlockEntity.class);
        for (Direction direction : Direction.values()) {
            helper.assertFalse(
                    battery.getCapability(ForgeCapabilities.ENERGY, direction).isPresent(),
                    "Battery exposed external Forge Energy on " + direction
            );
        }
        helper.assertFalse(battery.getCapability(ForgeCapabilities.ENERGY, null).isPresent(),
                "Battery exposed an unsided Forge Energy capability");
        var oldItemCapability = battery.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP);
        helper.assertTrue(oldItemCapability.isPresent(), "Battery lost item automation capability");
        battery.invalidateCaps();
        helper.assertFalse(oldItemCapability.isPresent(), "Invalidated battery item capability remained live");
        battery.reviveCaps();
        helper.assertTrue(battery.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent(),
                "Battery item capability did not revive");
        battery.energy().setEnergyStored(12_345);
        battery.inventory().setStackInSlot(0, new ItemStack(ModMachineItems.LOW_BATTERY.get()));

        CompoundTag saved = battery.saveWithoutMetadata();
        BatteryBlockEntity restored = new BatteryBlockEntity(battery.getBlockPos(), battery.getBlockState());
        restored.load(saved);
        helper.assertTrue(restored.energy().getEnergyStored() == 12_345, "Battery energy did not survive reload");
        helper.assertTrue(restored.inventory().getStackInSlot(0).is(ModMachineItems.LOW_BATTERY.get()), "Battery inventory did not survive reload");
        helper.assertTrue(
                saved.getCompound("modules").contains("magneticraft:energy_storage"),
                "Stable energy module id missing from NBT"
        );

        helper.setBlock(TEST_POS, ModMachineBlocks.ELECTRIC_FURNACE.get());
        ElectricFurnaceBlockEntity furnace = requireBlockEntity(helper, TEST_POS, ElectricFurnaceBlockEntity.class);
        helper.assertFalse(
                furnace.getCapability(ForgeCapabilities.ENERGY, Direction.UP).isPresent(),
                "Electric furnace exposed direct Forge Energy capability"
        );
        var furnaceItems = furnace.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(AssertionError::new);
        helper.assertTrue(furnaceItems.insertItem(1, new ItemStack(Items.IRON_INGOT), true).getCount() == 1, "Output slot accepted automation input");

        helper.setBlock(TEST_POS, ModMachineBlocks.CRUSHING_TABLE.get());
        CrushingTableBlockEntity table = requireBlockEntity(helper, TEST_POS, CrushingTableBlockEntity.class);
        helper.assertFalse(table.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).isPresent(), "Crushing table exposed automation inventory");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void batteryRejectsMissingAndFutureRootSchemas(GameTestHelper helper) {
        helper.setBlock(TEST_POS, ModMachineBlocks.BATTERY.get());
        BatteryBlockEntity battery = requireBlockEntity(helper, TEST_POS, BatteryBlockEntity.class);

        seedBatteryPersistentState(battery);
        CompoundTag missingSchema = battery.saveWithoutMetadata();
        missingSchema.remove("schema_version");
        helper.assertFalse(missingSchema.getCompound("modules").isEmpty(), "Missing-schema fixture lost its module payload");
        battery.load(missingSchema);
        assertBatteryPersistentStateReset(helper, battery, "Missing root schema");

        seedBatteryPersistentState(battery);
        CompoundTag futureSchema = battery.saveWithoutMetadata();
        futureSchema.putInt("schema_version", Integer.MAX_VALUE);
        helper.assertFalse(futureSchema.getCompound("modules").isEmpty(), "Future-schema fixture lost its module payload");
        battery.load(futureSchema);
        assertBatteryPersistentStateReset(helper, battery, "Future root schema");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void batteryChargesPortableCellAtLegacyRate(GameTestHelper helper) {
        helper.setBlock(TEST_POS, ModMachineBlocks.BATTERY.get());
        BatteryBlockEntity battery = requireBlockEntity(helper, TEST_POS, BatteryBlockEntity.class);
        ItemStack cell = new ItemStack(ModMachineItems.LOW_BATTERY.get());
        battery.electricity().node().setVoltage(90.0D);
        battery.energy().setEnergyStored(2_000);
        battery.inventory().setStackInSlot(0, cell);

        helper.runAfterDelay(2, () -> {
            int stored = cell.getCapability(ForgeCapabilities.ENERGY)
                    .map(storage -> storage.getEnergyStored())
                    .orElse(-1);
            helper.assertTrue(stored == 1_000, "Portable cell did not receive 500 FE/t");
            helper.assertTrue(battery.energy().getEnergyStored() == 1_000, "Battery lost the wrong amount of energy");
            ItemStack restoredCell = ItemStack.of(cell.save(new CompoundTag()));
            int restoredEnergy = restoredCell.getCapability(ForgeCapabilities.ENERGY)
                    .map(storage -> storage.getEnergyStored())
                    .orElse(-1);
            helper.assertTrue(restoredEnergy == 1_000, "Portable-cell energy did not survive ItemStack serialization");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void fluidAndGhostModulesPersistWithoutLeakingSides(GameTestHelper helper) {
        TestModuleHost host = new TestModuleHost(helper.getLevel(), helper.absolutePos(TEST_POS));
        FluidTankModule fluid = new FluidTankModule(
                Magneticraft.id("fluid"),
                host,
                1_000,
                stack -> stack.getFluid() == Fluids.WATER,
                side -> side == Direction.UP
        );
        helper.assertTrue(fluid.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).isPresent(), "Fluid module lost configured side");
        helper.assertFalse(fluid.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).isPresent(), "Fluid module leaked an unconfigured side");
        IFluidHandler fluidHandler = fluid.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP)
                .orElseThrow(AssertionError::new);
        FluidStack water = new FluidStack(Fluids.WATER, 750);
        helper.assertTrue(fluidHandler.fill(water, IFluidHandler.FluidAction.SIMULATE) == 750, "Fluid simulation returned the wrong amount");
        helper.assertTrue(fluid.tank().isEmpty(), "Fluid simulation mutated the tank");
        fluidHandler.fill(water, IFluidHandler.FluidAction.EXECUTE);
        CompoundTag fluidTag = new CompoundTag();
        fluid.save(fluidTag);
        FluidTankModule restoredFluid = new FluidTankModule(
                Magneticraft.id("fluid"), host, 1_000, stack -> true, side -> true
        );
        restoredFluid.load(fluidTag);
        helper.assertTrue(restoredFluid.tank().getFluidAmount() == 750, "Fluid module did not survive reload");

        FluidTankModule emptyFluid = new FluidTankModule(
                Magneticraft.id("empty_fluid"), host, 1_000, stack -> true, side -> true
        );
        CompoundTag emptyClientSnapshot = new CompoundTag();
        emptyFluid.saveClientData(emptyClientSnapshot);
        helper.assertFalse(emptyClientSnapshot.isEmpty(), "Empty fluid client snapshot was omitted");
        restoredFluid.loadClientData(emptyClientSnapshot);
        helper.assertTrue(restoredFluid.tank().isEmpty(), "Empty client snapshot left stale rendered fluid");

        GhostFilterModule filters = new GhostFilterModule(Magneticraft.id("filters"), host, 1);
        filters.setFilter(0, new ItemStack(Items.IRON_INGOT, 32));
        CompoundTag filterTag = new CompoundTag();
        filters.save(filterTag);
        GhostFilterModule restoredFilters = new GhostFilterModule(Magneticraft.id("filters"), host, 1);
        restoredFilters.load(filterTag);
        helper.assertTrue(
                restoredFilters.getFilter(0).is(Items.IRON_INGOT) && restoredFilters.getFilter(0).getCount() == 1,
                "Ghost filter did not persist a normalized sample"
        );
        TestGhostClickMenu clickMenu = new TestGhostClickMenu(filters);
        clickMenu.setCarried(new ItemStack(Items.GOLD_INGOT, 32));
        clickMenu.clicked(0, 0, ClickType.PICKUP, helper.makeMockSurvivalPlayer());
        helper.assertTrue(clickMenu.getCarried().getCount() == 32, "Ghost slot consumed the carried stack");
        helper.assertTrue(
                filters.getFilter(0).is(Items.IRON_INGOT) && filters.getFilter(0).getCount() == 1,
                "Server-side generic slot click bypassed the validated filter message"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 220)
    public static void electricFurnaceConsumesEnergyAndSmelts(GameTestHelper helper) {
        helper.setBlock(TEST_POS, ModMachineBlocks.ELECTRIC_FURNACE.get());
        ElectricFurnaceBlockEntity furnace = requireBlockEntity(helper, TEST_POS, ElectricFurnaceBlockEntity.class);
        furnace.energy().setEnergyStored(ElectricFurnaceBlockEntity.ENERGY_CAPACITY);
        furnace.inventory().setStackInSlot(0, new ItemStack(Items.RAW_IRON));
        int[] pausedProgress = new int[1];

        helper.runAfterDelay(10, () -> {
            pausedProgress[0] = furnace.process().progressUnits();
            helper.assertTrue(pausedProgress[0] > 0, "Electric furnace did not start processing");
            furnace.inventory().setStackInSlot(1, new ItemStack(Items.DIAMOND));
        });
        helper.runAfterDelay(16, () -> {
            helper.assertTrue(
                    furnace.process().progressUnits() == pausedProgress[0],
                    "Blocked output reset or advanced electric-furnace progress"
            );
            helper.assertTrue(furnace.getBlockState().getValue(ElectricFurnaceBlock.LIT), "Working-state grace period was lost");
            furnace.inventory().setStackInSlot(1, ItemStack.EMPTY);
        });

        helper.runAfterDelay(190, () -> {
            helper.assertTrue(furnace.inventory().getStackInSlot(0).isEmpty(), "Electric furnace did not consume input");
            helper.assertTrue(furnace.inventory().getStackInSlot(1).is(Items.IRON_INGOT), "Electric furnace produced the wrong output");
            helper.assertTrue(
                    furnace.energy().getEnergyStored() < ElectricFurnaceBlockEntity.ENERGY_CAPACITY,
                    "Electric furnace did not consume energy"
            );
            helper.assertFalse(furnace.getBlockState().getValue(ElectricFurnaceBlock.LIT), "Idle electric furnace stayed lit");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void crushingTableAndShiftClickPreserveItems(GameTestHelper helper) {
        helper.setBlock(TEST_POS, ModMachineBlocks.CRUSHING_TABLE.get());
        CrushingTableBlockEntity table = requireBlockEntity(helper, TEST_POS, CrushingTableBlockEntity.class);
        Player player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        helper.assertTrue(table.interact(player, InteractionHand.MAIN_HAND), "Stone was not placed on crushing table");
        helper.assertTrue(table.storedItem().is(Items.STONE), "Crushing table stored the wrong item");

        ItemStack hammer = new ItemStack(ModItems.hammer(HammerType.STONE).get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        for (int hit = 0; hit < 5; hit++) {
            table.interact(player, InteractionHand.MAIN_HAND);
        }
        helper.assertTrue(table.storedItem().is(Items.COBBLESTONE), "Five stone-hammer hits did not finish crushing");
        helper.assertTrue(hammer.getDamageValue() == 5, "Crushing used the weapon-hit durability cost");

        helper.setBlock(TEST_POS, ModMachineBlocks.BATTERY.get());
        BatteryBlockEntity battery = requireBlockEntity(helper, TEST_POS, BatteryBlockEntity.class);
        ItemStack lowBattery = new ItemStack(ModMachineItems.LOW_BATTERY.get());
        player.getInventory().setItem(0, lowBattery);
        BatteryMenu menu = new BatteryMenu(1, player.getInventory(), battery);
        ItemStack moved = menu.quickMoveStack(player, 29);
        helper.assertFalse(moved.isEmpty(), "Shift-click did not move the portable battery");
        helper.assertTrue(battery.inventory().getStackInSlot(0).is(ModMachineItems.LOW_BATTERY.get()), "Shift-click chose the wrong machine slot");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void ghostPacketRejectsInvalidIntentAndNormalizesValidSample(GameTestHelper helper) {
        Player player = helper.makeMockSurvivalPlayer();
        BlockPos absolute = helper.absolutePos(TEST_POS);
        player.setPos(absolute.getX() + 0.5D, absolute.getY() + 0.5D, absolute.getZ() + 0.5D);
        TestGhostMenu menu = new TestGhostMenu(absolute);
        player.containerMenu = menu;

        boolean accepted = SetGhostFilterMessage.applyIfValid(
                player,
                new SetGhostFilterMessage(absolute, 0, new ItemStack(Items.IRON_INGOT, 32))
        );
        helper.assertTrue(accepted, "Valid ghost-filter intent was rejected");
        helper.assertTrue(menu.sample.is(Items.IRON_INGOT) && menu.sample.getCount() == 1, "Ghost sample was not normalized");

        ItemStack before = menu.sample.copy();
        boolean rejected = SetGhostFilterMessage.applyIfValid(
                player,
                new SetGhostFilterMessage(absolute, 1, new ItemStack(Items.GOLD_INGOT))
        );
        helper.assertFalse(rejected, "Out-of-range ghost-filter intent was accepted");
        helper.assertTrue(ItemStack.matches(before, menu.sample), "Rejected packet changed server state");

        rejected = SetGhostFilterMessage.applyIfValid(
                player,
                new SetGhostFilterMessage(absolute.offset(1, 0, 0), 0, new ItemStack(Items.GOLD_INGOT))
        );
        helper.assertFalse(rejected, "Mismatched target position was accepted");
        player.getAbilities().mayBuild = false;
        rejected = SetGhostFilterMessage.applyIfValid(
                player,
                new SetGhostFilterMessage(absolute, 0, new ItemStack(Items.GOLD_INGOT))
        );
        helper.assertFalse(rejected, "Player without build permission changed a ghost filter");
        helper.assertTrue(ItemStack.matches(before, menu.sample), "Rejected security paths changed server state");
        helper.succeed();
    }

    private static <T extends BlockEntity> T requireBlockEntity(
            GameTestHelper helper,
            BlockPos relativePosition,
            Class<T> type
    ) {
        BlockEntity blockEntity = helper.getBlockEntity(relativePosition);
        helper.assertTrue(type.isInstance(blockEntity), "Missing block entity " + type.getSimpleName());
        return type.cast(blockEntity);
    }

    private static void seedBatteryPersistentState(BatteryBlockEntity battery) {
        battery.energy().setEnergyStored(12_345);
        battery.electricity().node().setEnergyJoules(321.0D);
        battery.inventory().setStackInSlot(0, new ItemStack(ModMachineItems.LOW_BATTERY.get()));
    }

    private static void assertBatteryPersistentStateReset(
            GameTestHelper helper,
            BatteryBlockEntity battery,
            String scenario
    ) {
        helper.assertTrue(battery.energy().getEnergyStored() == 0, scenario + " retained Forge Energy");
        helper.assertTrue(
                battery.electricity().node().energyJoules() == 0.0D,
                scenario + " retained electrical-network energy"
        );
        helper.assertTrue(battery.inventory().getStackInSlot(0).isEmpty(), scenario + " retained inventory");
    }

    private static final class TestGhostMenu extends AbstractContainerMenu implements GhostFilterMenuAccess {
        private final BlockPos position;
        private ItemStack sample = ItemStack.EMPTY;

        private TestGhostMenu(BlockPos position) {
            super(null, 0);
            this.position = position;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return player.distanceToSqr(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D) <= 64.0D;
        }

        @Override
        public BlockPos machinePosition() {
            return position;
        }

        @Override
        public int ghostFilterCount() {
            return 1;
        }

        @Override
        public void setGhostFilter(int slot, ItemStack sample) {
            this.sample = sample.copy();
        }
    }

    private static final class TestModuleHost implements MachineModuleHost {
        private final Level level;
        private final BlockPos position;

        private TestModuleHost(Level level, BlockPos position) {
            this.level = level;
            this.position = position;
        }

        @Override
        public void markChanged() {
        }

        @Override
        public void markChangedAndSync() {
        }

        @Override
        public Level level() {
            return level;
        }

        @Override
        public BlockPos position() {
            return position;
        }
    }

    private static final class TestGhostClickMenu extends AbstractMachineMenu {
        private TestGhostClickMenu(GhostFilterModule filters) {
            super(null, 0);
            addSlot(new GhostSlot(filters, 0, 0, 0));
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        protected boolean movePlayerStackToMachine(ItemStack stack) {
            return false;
        }
    }
}
