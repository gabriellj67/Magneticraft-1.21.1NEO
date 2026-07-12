package committee.nova.mods.magneticraft.content.machine;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.module.BulkItemStorageModule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkItemStorageModuleTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void storesOneItemTypeUpToTheConfiguredCapacity() {
        BulkItemStorageModule storage = new BulkItemStorageModule(
                Magneticraft.id("bulk_test"), new TestHost(), 128
        );

        assertTrue(storage.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false).isEmpty());
        assertTrue(storage.insertItem(0, new ItemStack(Items.IRON_INGOT, 64), false).isEmpty());
        assertEquals(128, storage.amount());
        assertEquals(64, storage.getStackInSlot(0).getCount());
        assertEquals(16, storage.insertItem(0, new ItemStack(Items.IRON_INGOT, 16), false).getCount());
        assertEquals(16, storage.insertItem(0, new ItemStack(Items.GOLD_INGOT, 16), false).getCount());

        assertEquals(64, storage.extractItem(0, 100, false).getCount());
        assertEquals(64, storage.amount());
    }

    @Test
    void persistencePreservesTheAggregate() {
        TestHost host = new TestHost();
        BulkItemStorageModule storage = new BulkItemStorageModule(
                Magneticraft.id("bulk_test"), host, 65_536
        );
        storage.insertItem(0, new ItemStack(Items.COBBLESTONE, 37), false);
        CompoundTag saved = new CompoundTag();
        storage.save(saved);

        BulkItemStorageModule restored = new BulkItemStorageModule(
                Magneticraft.id("bulk_test"), host, 65_536
        );
        restored.load(saved);
        assertEquals(37, restored.amount());
        assertTrue(restored.getStackInSlot(0).is(Items.COBBLESTONE));
    }

    private static final class TestHost implements MachineModuleHost {
        @Override
        public void markChanged() {
        }

        @Override
        public void markChangedAndSync() {
        }

        @Override
        public Level level() {
            return null;
        }

        @Override
        public BlockPos position() {
            return BlockPos.ZERO;
        }
    }
}
