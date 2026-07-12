package committee.nova.mods.magneticraft.content.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Portable 250 kFE cell used by the migrated battery block.
 */
public final class LowBatteryItem extends Item {
    public static final int CAPACITY = 250_000;
    public static final int TRANSFER_RATE = 500;

    public LowBatteryItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EnergyProvider();
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage.getEnergyStored() < storage.getMaxEnergyStored())
                .orElse(false);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> Math.round(13.0F * storage.getEnergyStored() / storage.getMaxEnergyStored()))
                .orElse(0);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x43D96B;
    }

    private static final class EnergyProvider implements ICapabilitySerializable<CompoundTag>, IEnergyStorage {
        private static final String ENERGY_TAG = "energy";

        private final LazyOptional<IEnergyStorage> capability = LazyOptional.of(() -> this);
        private int energy;

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> requested, @Nullable net.minecraft.core.Direction side) {
            return requested == ForgeCapabilities.ENERGY ? capability.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt(ENERGY_TAG, energy);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            energy = Math.max(0, Math.min(CAPACITY, tag.getInt(ENERGY_TAG)));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.min(Math.max(maxReceive, 0), Math.min(TRANSFER_RATE, CAPACITY - energy));
            if (!simulate) {
                energy += accepted;
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.min(Math.max(maxExtract, 0), Math.min(TRANSFER_RATE, energy));
            if (!simulate) {
                energy -= extracted;
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return CAPACITY;
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
