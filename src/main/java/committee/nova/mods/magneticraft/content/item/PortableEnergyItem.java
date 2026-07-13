package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.system.energy.PortableEnergyState;
import committee.nova.mods.magneticraft.system.energy.PortableEnergyPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Shared Forge Energy storage for portable Magneticraft items.
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
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(IEnergyStorage::getEnergyStored)
                .orElse(0);
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
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage instanceof EnergyProvider provider && provider.consume(amount))
                .orElse(false);
    }

    protected final boolean hasEnergy(ItemStack stack, int amount) {
        return amount >= 0 && energyStored(stack) >= amount;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyProvider(capacity);
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
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable(ENERGY_TOOLTIP, energyStored(stack), capacity)
                .withStyle(ChatFormatting.GRAY));
    }

    private static final class EnergyProvider implements ICapabilitySerializable<CompoundTag>, IEnergyStorage {
        private final LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> this);
        private final int capacity;
        private final PortableEnergyState state;

        private EnergyProvider(int capacity) {
            this.capacity = capacity;
            state = new PortableEnergyState(capacity);
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> requested, @Nullable Direction side) {
            return requested == ForgeCapabilities.ENERGY ? capability.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return PortableEnergyPayload.write(state.energy());
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            state.load(PortableEnergyPayload.readEnergyOrDefault(tag, capacity));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return state.receive(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return state.extract(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return state.energy();
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

        private boolean consume(int amount) {
            return state.consume(amount);
        }
    }
}
