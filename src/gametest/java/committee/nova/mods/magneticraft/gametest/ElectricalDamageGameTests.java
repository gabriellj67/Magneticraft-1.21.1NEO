package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionBlockEntity;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElectricalDamageGameTests {
    private static final String TEMPLATE = "base_content";

    private ElectricalDamageGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void overvoltageBurnsCableIntoDisconnectedBurntBlock(GameTestHelper helper) {
        BlockPos relative = new BlockPos(0, 2, 0);
        helper.setBlock(relative, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.runAfterDelay(1, () -> {
            ElectricCableBlockEntity cable = requireBlockEntity(
                    helper,
                    relative,
                    ElectricCableBlockEntity.class
            );
            cable.electricity().node().setVoltage(500.0D);
        });
        helper.runAfterDelay(12, () -> {
            helper.assertBlockPresent(ModNetworkBlocks.BURNT_ELECTRIC_CABLE.get(), relative);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void machineFaultPreservesStateAndRepairsOnlyAfterDeenergizing(GameTestHelper helper) {
        BlockPos relative = new BlockPos(0, 2, 0);
        helper.setBlock(relative, ModMachineBlocks.ELECTRIC_FURNACE.get());
        helper.runAfterDelay(1, () -> {
            ElectricFurnaceBlockEntity furnace = requireBlockEntity(
                    helper,
                    relative,
                    ElectricFurnaceBlockEntity.class
            );
            furnace.inventory().setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
            furnace.energy().setStoredJoules(3_000);
            CompoundTag process = new CompoundTag();
            process.putInt("progress_units", 123);
            furnace.process().load(process);
            furnace.electricity().node().setVoltage(500.0D);
        });
        helper.runAfterDelay(12, () -> {
            ElectricFurnaceBlockEntity furnace = requireBlockEntity(
                    helper,
                    relative,
                    ElectricFurnaceBlockEntity.class
            );
            helper.assertTrue(furnace.electricalFaulted(), "Overvolted machine did not enter permanent fault state");
            helper.assertTrue(furnace.inventory().getStackInSlot(0).getCount() == 3,
                    "Machine fault discarded inventory");
            helper.assertTrue(furnace.energy().storedWholeJoules() >= 3_000,
                    "Machine fault discarded the energy cache");
            helper.assertTrue(furnace.process().progressUnits() == 123,
                    "Machine fault changed recipe progress");
            helper.assertTrue(!furnace.tryRepairElectricalFault(),
                    "Energized machine repaired above the safe voltage threshold");
            furnace.electricity().node().setVoltage(0.0D);
            helper.assertTrue(furnace.tryRepairElectricalFault(), "Deenergized machine could not be repaired");
            helper.assertTrue(!furnace.electricalFaulted(), "Repair left the machine faulted");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void fuseAndBreakerOwnExplicitRedstoneControlledInternalEdges(GameTestHelper helper) {
        BlockPos fuseRelative = new BlockPos(0, 2, 0);
        BlockPos breakerRelative = new BlockPos(3, 2, 0);
        helper.setBlock(fuseRelative, ModNetworkBlocks.FUSE_BOX.get());
        helper.setBlock(breakerRelative, ModNetworkBlocks.CIRCUIT_BREAKER.get());
        helper.runAfterDelay(1, () -> {
            ElectricalProtectionBlockEntity fuseBox = requireBlockEntity(
                    helper,
                    fuseRelative,
                    ElectricalProtectionBlockEntity.class
            );
            ElectricalProtectionBlockEntity breaker = requireBlockEntity(
                    helper,
                    breakerRelative,
                    ElectricalProtectionBlockEntity.class
            );
            ItemStack fuse = ModNetworkItems.FUSE.get().stackFor(
                    VoltageTierIds.LOW,
                    ElectricalRatingIds.STANDARD
            );
            helper.assertTrue(fuseBox.protection().insertFuse(fuse), "Matching fuse was rejected");
            fuseBox.protection().refreshConnection();
            breaker.protection().refreshConnection();

            PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
            helper.assertTrue(manager.internalConnectionClosed(
                    fuseBox.input().nodeKey(), fuseBox.output().nodeKey()
            ), "Inserted fuse did not close its internal edge");
            helper.assertTrue(manager.internalConnectionClosed(
                    breaker.input().nodeKey(), breaker.output().nodeKey()
            ), "Reset breaker did not close its internal edge");
            helper.setBlock(breakerRelative.above(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(3, () -> {
            ElectricalProtectionBlockEntity breaker = requireBlockEntity(
                    helper,
                    breakerRelative,
                    ElectricalProtectionBlockEntity.class
            );
            PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
            helper.assertTrue(!manager.internalConnectionClosed(
                    breaker.input().nodeKey(), breaker.output().nodeKey()
            ), "Redstone signal did not force the breaker open");
            helper.succeed();
        });
    }

    private static <T> T requireBlockEntity(GameTestHelper helper, BlockPos relative, Class<T> type) {
        Object blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(relative));
        helper.assertTrue(type.isInstance(blockEntity), "Missing block entity " + type.getSimpleName());
        return type.cast(blockEntity);
    }
}
