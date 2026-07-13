package committee.nova.mods.magneticraft.system.network.logistics;

import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemHandlerTransactionsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void rejectedSimulationHasNoSideEffects() {
        ChangingHandler handler = new ChangingHandler(2, 2);
        ItemStack source = new ItemStack(Items.IRON_INGOT, 5);

        ItemStack remainder = ItemHandlerTransactions.insertAfterFullSimulation(handler, source);

        assertEquals(5, remainder.getCount());
        assertEquals(0, handler.stored);
        assertEquals(5, source.getCount());
    }

    @Test
    void executionRemainderPreservesExactlyOnceAccounting() {
        ChangingHandler handler = new ChangingHandler(5, 3);
        ItemStack source = new ItemStack(Items.IRON_INGOT, 5);

        ItemStack remainder = ItemHandlerTransactions.insertAfterFullSimulation(handler, source);

        assertEquals(2, remainder.getCount());
        assertEquals(3, handler.stored);
        assertEquals(source.getCount(), remainder.getCount() + handler.stored);
    }

    @Test
    void fullCommitConsumesThePayloadExactlyOnce() {
        ChangingHandler handler = new ChangingHandler(5, 5);

        ItemStack remainder = ItemHandlerTransactions.insertAfterFullSimulation(
                handler,
                new ItemStack(Items.IRON_INGOT, 5)
        );

        assertTrue(remainder.isEmpty());
        assertEquals(5, handler.stored);
    }

    private static final class ChangingHandler implements IItemHandler {
        private final int simulatedCapacity;
        private final int executionCapacity;
        private int stored;

        private ChangingHandler(int simulatedCapacity, int executionCapacity) {
            this.simulatedCapacity = simulatedCapacity;
            this.executionCapacity = executionCapacity;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int capacity = simulate ? simulatedCapacity : executionCapacity - stored;
            int accepted = Math.min(stack.getCount(), Math.max(0, capacity));
            if (!simulate) {
                stored += accepted;
            }
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }
}
