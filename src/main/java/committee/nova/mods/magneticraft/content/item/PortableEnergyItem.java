package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.system.energy.PortableEnergyState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared per-stack Forge Energy storage for portable Magneticraft items.
 *
 * <p>Persists its balance in the {@link PortableEnergyComponents#PORTABLE_ENERGY} data
 * component instead of raw NBT (there is no more per-item capability provider object that
 * owns its own serialization - see {@link PortableEnergyComponents}).
 */
public class PortableEnergyItem extends Item {
    private static final int BAR_COLOR = 0x43D96B;
    private static final String ENERGY_TOOLTIP = "tooltip.magneticraft.energy";

    private final int capacity;

    protected PortableEnergyItem(int capacity) {
        super(new Item.Properties().stacksTo(1));
        if (capacity <= 0) {
            throw new IllegalArgumentException("Energy capacity must be positive");
        }
        this.capacity = capacity;
    }

    public final int capacity() {
        return capacity;
    }

    /**
     * Returns the current balance without mutating the stack.
     */
    public final int energyStored(ItemStack stack) {
        return stack.getOrDefault(PortableEnergyComponents.PORTABLE_ENERGY.get(), 0);
    }

    /**
     * Atomically consumes an internal action cost.
     */
    public final boolean consumeEnergy(ItemStack stack, int amount) {
        if (amount < 0) {
            return false;
        }
        if (amount == 0) {
            return true;
        }
        PortableEnergyState state = new PortableEnergyState(capacity);
        state.load(energyStored(stack));
        if (!state.consume(amount)) {
            return false;
        }
        stack.set(PortableEnergyComponents.PORTABLE_ENERGY.get(), state.energy());
        return true;
    }

    protected final boolean hasEnergy(ItemStack stack, int amount) {
        return amount >= 0 && energyStored(stack) >= amount;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return energyStored(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * energyStored(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable(ENERGY_TOOLTIP, energyStored(stack), capacity)
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * Registers the {@link Capabilities.EnergyStorage#ITEM} provider for one or more
     * portable-energy items. Call once from the {@code init/ModItems}/{@code ModMachineItems}
     * registry bootstrap for every concrete {@link PortableEnergyItem} subtype, once those
     * items exist (mirrors {@code content/machine/framework/MachineCapabilities.register}'s
     * role for block entities).
     */
    public static void registerCapability(RegisterCapabilitiesEvent event, ItemLike... items) {
        event.registerItem(Capabilities.EnergyStorage.ITEM, PortableEnergyItem::view, items);
    }

    @Nullable
    private static IEnergyStorage view(ItemStack stack, @Nullable Void context) {
        return stack.getItem() instanceof PortableEnergyItem item ? new EnergyView(stack, item.capacity) : null;
    }

    private static final class EnergyView implements IEnergyStorage {
        private final ItemStack stack;
        private final int capacity;

        private EnergyView(ItemStack stack, int capacity) {
            this.stack = stack;
            this.capacity = capacity;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            PortableEnergyState state = new PortableEnergyState(capacity);
            state.load(stack.getOrDefault(PortableEnergyComponents.PORTABLE_ENERGY.get(), 0));
            int accepted = state.receive(maxReceive, simulate);
            if (!simulate) {
                stack.set(PortableEnergyComponents.PORTABLE_ENERGY.get(), state.energy());
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            PortableEnergyState state = new PortableEnergyState(capacity);
            state.load(stack.getOrDefault(PortableEnergyComponents.PORTABLE_ENERGY.get(), 0));
            int extracted = state.extract(maxExtract, simulate);
            if (!simulate) {
                stack.set(PortableEnergyComponents.PORTABLE_ENERGY.get(), state.energy());
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return stack.getOrDefault(PortableEnergyComponents.PORTABLE_ENERGY.get(), 0);
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
