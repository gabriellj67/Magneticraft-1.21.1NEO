package committee.nova.mods.magneticraft.content.machine.framework.module;

import committee.nova.mods.magneticraft.content.machine.framework.EnergyBuffer;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * NeoForge energy adapter around the framework's pure Java energy buffer.
 */
public final class EnergyStorageModule implements MachineModule, IEnergyStorage {
    private static final String ENERGY_TAG = "energy";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final EnergyBuffer buffer;
    private final int internalMaxReceive;
    private final int internalMaxExtract;
    private final Predicate<Direction> exposedSides;
    private final boolean externalReceive;
    private final boolean externalExtract;
    private final IEnergyStorage externalView = new ExternalEnergyView();

    public EnergyStorageModule(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            int maxReceive,
            int maxExtract,
            Predicate<Direction> exposedSides
    ) {
        this(id, host, capacity, maxReceive, maxExtract, exposedSides, maxReceive > 0, maxExtract > 0);
    }

    public EnergyStorageModule(
            ResourceLocation id,
            MachineModuleHost host,
            int capacity,
            int maxReceive,
            int maxExtract,
            Predicate<Direction> exposedSides,
            boolean externalReceive,
            boolean externalExtract
    ) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.buffer = new EnergyBuffer(capacity, maxReceive, maxExtract);
        this.internalMaxReceive = maxReceive;
        this.internalMaxExtract = maxExtract;
        this.exposedSides = Objects.requireNonNull(exposedSides);
        this.externalReceive = externalReceive;
        this.externalExtract = externalExtract;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        buffer.setEnergy(tag.getInt(ENERGY_TAG));
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(ENERGY_TAG, buffer.energy());
    }

    /** Energy-storage view exposed for the given side, or {@code null} if this module hides it from that side. */
    @Nullable
    public IEnergyStorage view(@Nullable Direction side) {
        return exposedSides.test(side) ? externalView : null;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int accepted = buffer.receive(maxReceive, simulate);
        if (accepted > 0 && !simulate) {
            host.markChanged();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = buffer.extract(maxExtract, simulate);
        if (extracted > 0 && !simulate) {
            host.markChanged();
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return buffer.energy();
    }

    @Override
    public int getMaxEnergyStored() {
        return buffer.capacity();
    }

    @Override
    public boolean canExtract() {
        return buffer.maxExtract() > 0;
    }

    @Override
    public boolean canReceive() {
        return buffer.maxReceive() > 0;
    }

    public void setEnergyStored(int amount) {
        int oldEnergy = buffer.energy();
        buffer.setEnergy(amount);
        if (oldEnergy != buffer.energy()) {
            host.markChanged();
        }
    }

    /** Applies a validated data-pack profile while preserving the authoritative stored balance. */
    public void reconfigure(int capacity, int maxReceive, int maxExtract) {
        buffer.reconfigure(capacity, maxReceive, maxExtract);
        host.markChanged();
    }

    /**
     * Applies electrical cache/rate data without turning the network transfer limit into a
     * recipe or discrete-action withdrawal limit. The bridge enforces the electrical rate;
     * this buffer only needs to admit both its original internal operation and that bridge.
     */
    public void reconfigureForElectricalProfile(int capacity, int networkTransferRate) {
        buffer.reconfigure(
                capacity,
                Math.max(internalMaxReceive, networkTransferRate),
                Math.max(internalMaxExtract, networkTransferRate)
        );
        host.markChanged();
    }

    private final class ExternalEnergyView implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return externalReceive ? EnergyStorageModule.this.receiveEnergy(maxReceive, simulate) : 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return externalExtract ? EnergyStorageModule.this.extractEnergy(maxExtract, simulate) : 0;
        }

        @Override
        public int getEnergyStored() {
            return EnergyStorageModule.this.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return EnergyStorageModule.this.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return externalExtract && EnergyStorageModule.this.canExtract();
        }

        @Override
        public boolean canReceive() {
            return externalReceive && EnergyStorageModule.this.canReceive();
        }
    }
}
