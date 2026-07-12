package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShelvingStorageModuleTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void eachInstalledChestUnlocksExactlyTwentySevenSlots() {
        ShelvingStorageModule module = module();

        assertEquals(0, module.getSlots());
        assertThrows(IndexOutOfBoundsException.class,
                () -> module.insertItem(0, new ItemStack(Items.IRON_INGOT), false));

        assertTrue(module.installChest(new ItemStack(Items.CHEST)));
        assertEquals(1, module.installedChests());
        assertEquals(27, module.getSlots());
        assertTrue(module.insertItem(26, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty());
        assertThrows(IndexOutOfBoundsException.class,
                () -> module.insertItem(27, new ItemStack(Items.IRON_INGOT), false));
    }

    @Test
    void chestLimitAndContentsSurviveNbtRoundTrip() {
        ShelvingStorageModule original = module();
        for (int index = 0; index < ShelvingStorageModule.MAX_CHESTS; index++) {
            assertTrue(original.installChest(new ItemStack(Items.CHEST)));
        }
        assertFalse(original.installChest(new ItemStack(Items.CHEST)));
        assertEquals(ShelvingStorageModule.MAX_STORAGE_SLOTS, original.getSlots());
        original.insertItem(647, new ItemStack(Items.DIAMOND, 32), false);

        CompoundTag tag = new CompoundTag();
        original.save(tag);
        ShelvingStorageModule restored = module();
        restored.load(tag);

        assertEquals(ShelvingStorageModule.MAX_CHESTS, restored.installedChests());
        assertEquals(ShelvingStorageModule.MAX_STORAGE_SLOTS, restored.getSlots());
        assertEquals(32, restored.getStackInSlot(647).getCount());
        assertEquals(Items.DIAMOND, restored.getStackInSlot(647).getItem());
    }

    private static ShelvingStorageModule module() {
        return new ShelvingStorageModule(
                ResourceLocation.fromNamespaceAndPath("magneticraft", "test_shelving"),
                new TestHost()
        );
    }

    private static final class TestHost implements MachineModuleHost {
        @Override
        public void markChanged() {
        }

        @Override
        public void markChangedAndSync() {
        }

        @Nullable
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
