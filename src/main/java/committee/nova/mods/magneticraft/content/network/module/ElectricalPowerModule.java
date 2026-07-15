package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.electric.ElectricEnergyExporter;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalProfileController;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalRole;
import committee.nova.mods.magneticraft.system.network.electric.profile.MachineElectricalProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Profile-bound native machine power backed exclusively by an {@link ElectricalNetworkModule} node.
 * This module owns no energy balance; its persistence surface only imports the removed legacy buffer.
 */
public final class ElectricalPowerModule implements MachineModule, ElectricalProfileController {
    private static final String LEGACY_ENERGY_TAG = "energy";

    private final ResourceLocation id;
    private final ResourceLocation profileId;
    private final MachineModuleHost host;
    private final ElectricalNetworkModule electricity;
    private final ForgeEnergyAccess forgeEnergyAccess;
    private final Predicate<Direction> forgeEnergySides;
    private final boolean tierOverrideAllowed;

    private LazyOptional<IEnergyStorage> forgeEnergyCapability = LazyOptional.empty();
    private MachineElectricalProfile profile;
    private VoltageTier tier;
    private boolean profileBound;
    private double ratedCapacityJoules;
    private double maximumTransferJoulesPerTick;
    private long forgeEnergyTransferTick = Long.MIN_VALUE;
    private int forgeEnergyTransferredThisTick;

    public ElectricalPowerModule(
            ResourceLocation id,
            ResourceLocation profileId,
            MachineModuleHost host,
            ElectricalNetworkModule electricity
    ) {
        this(id, profileId, host, electricity, ForgeEnergyAccess.NONE, side -> false, false);
    }

    public ElectricalPowerModule(
            ResourceLocation id,
            ResourceLocation profileId,
            MachineModuleHost host,
            ElectricalNetworkModule electricity,
            ForgeEnergyAccess forgeEnergyAccess,
            Predicate<Direction> forgeEnergySides,
            boolean tierOverrideAllowed
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.profileId = Objects.requireNonNull(profileId, "profileId");
        this.host = Objects.requireNonNull(host, "host");
        this.electricity = Objects.requireNonNull(electricity, "electricity");
        this.forgeEnergyAccess = Objects.requireNonNull(forgeEnergyAccess, "forgeEnergyAccess");
        this.forgeEnergySides = Objects.requireNonNull(forgeEnergySides, "forgeEnergySides");
        this.tierOverrideAllowed = tierOverrideAllowed;
        electricity.attachProfileController(this);
        reviveCapabilities();
        ElectricalDataRegistry.INSTANCE.current().ifPresent(this::rebindElectricalProfile);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag) {
        if (!tag.contains(LEGACY_ENERGY_TAG, Tag.TAG_INT)) {
            return;
        }
        int legacyJoules = Math.max(0, tag.getInt(LEGACY_ENERGY_TAG));
        if (legacyJoules <= 0) {
            return;
        }
        electricity.node().setEnergyJoules(electricity.node().energyJoules() + legacyJoules);
        host.markChanged();
    }

    @Override
    public void invalidateCapabilities() {
        forgeEnergyCapability.invalidate();
        forgeEnergyCapability = LazyOptional.empty();
    }

    @Override
    public void reviveCapabilities() {
        forgeEnergyCapability = forgeEnergyAccess == ForgeEnergyAccess.NONE
                ? LazyOptional.empty()
                : LazyOptional.of(ForgeEnergyView::new);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> requested, @Nullable Direction side) {
        if (requested == ForgeCapabilities.ENERGY
                && forgeEnergyAccess != ForgeEnergyAccess.NONE
                && forgeEnergySides.test(side)) {
            return forgeEnergyCapability.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public void rebindElectricalProfile(ElectricalDataSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Optional<MachineElectricalProfile> nextProfile = snapshot.machineProfile(profileId);
        ResourceLocation selectedTierId = nextProfile.isPresent() && !tierOverrideAllowed
                ? nextProfile.orElseThrow().tierId()
                : electricity.tierId();
        Optional<VoltageTier> nextTier = nextProfile.isPresent()
                ? snapshot.voltageTier(selectedTierId)
                : Optional.empty();
        double nextCapacity = nextProfile.isPresent() && nextTier.isPresent()
                ? ratedCapacity(nextProfile.orElseThrow(), nextTier.orElseThrow())
                : 0.0D;
        boolean tierBound = nextCapacity > 0.0D
                && electricity.bindTierFromMachineProfile(
                snapshot,
                selectedTierId,
                MachineElectricalProfile.capacitanceForRatedEnergy(nextCapacity, nextTier.orElseThrow())
        );
        profileBound = nextProfile.isPresent()
                && nextTier.isPresent()
                && tierBound
                && forgeEnergyAccess.accepts(nextProfile.orElseThrow().role());
        if (!profileBound) {
            profile = null;
            tier = null;
            ratedCapacityJoules = 0.0D;
            maximumTransferJoulesPerTick = 0.0D;
            return;
        }
        profile = nextProfile.orElseThrow();
        tier = nextTier.orElseThrow();
        ratedCapacityJoules = nextCapacity;
        maximumTransferJoulesPerTick = tierOverrideAllowed && profile.role() == ElectricalRole.STORAGE
                ? tier.batteryTransferJoulesPerTick()
                : profile.maximumTransferJoulesPerTick();
    }

    @Override
    public boolean electricalControllerBound() {
        return profileBound;
    }

    @Override
    public double terminalRatedChargePerTick() {
        return profileBound ? profile.terminalRatedChargePerTick() : 0.0D;
    }

    public ResourceLocation profileId() {
        return profileId;
    }

    public Optional<MachineElectricalProfile> profile() {
        return Optional.ofNullable(profile);
    }

    public Optional<VoltageTier> tier() {
        return Optional.ofNullable(tier);
    }

    public double storedJoules() {
        return electricity.node().energyJoules();
    }

    public int storedWholeJoules() {
        return wholeJoules(storedJoules());
    }

    public double ratedCapacityJoules() {
        return ratedCapacityJoules;
    }

    public int ratedCapacityWholeJoules() {
        return wholeJoules(ratedCapacityJoules);
    }

    public int maximumTransferJoulesPerTick() {
        return wholeJoules(maximumTransferJoulesPerTick);
    }

    /** Loader-independent FE -> J conversion entry point used by the capability view and tests. */
    public int receiveForgeEnergy(int maxReceive, boolean simulate) {
        if (forgeEnergyAccess != ForgeEnergyAccess.INPUT || !profileBound) {
            return 0;
        }
        int wholeRoom = wholeJoules(ratedCapacityJoules - electricity.node().energyJoules());
        int requested = Math.min(
                Math.max(0, maxReceive),
                Math.min(wholeRoom, remainingForgeEnergyTransfer(maximumTransferJoulesPerTick()))
        );
        int inserted = wholeJoules(storeJoules(requested, simulate));
        if (!simulate) {
            recordForgeEnergyTransfer(inserted);
        }
        return inserted;
    }

    /** Loader-independent J -> FE conversion entry point used by the capability view and tests. */
    public int extractForgeEnergy(int maxExtract, boolean simulate) {
        if (forgeEnergyAccess != ForgeEnergyAccess.OUTPUT || !profileBound) {
            return 0;
        }
        int requested = Math.min(
                Math.max(0, maxExtract),
                remainingForgeEnergyTransfer(voltageLimitedOutputRate())
        );
        int available = wholeJoules(electricity.node().removeEnergy(requested, true));
        int extracted = wholeJoules(withdrawJoules(available, simulate));
        if (!simulate) {
            recordForgeEnergyTransfer(extracted);
        }
        return extracted;
    }

    /** Active J -> FE output sharing the same per-tick budget as capability extraction. */
    public int exportForgeEnergy(ServerLevel level, BlockPos position, Direction outwardFacing) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(outwardFacing, "outwardFacing");
        if (forgeEnergyAccess != ForgeEnergyAccess.OUTPUT || !profileBound) {
            return 0;
        }
        int remaining = remainingForgeEnergyTransfer(voltageLimitedOutputRate());
        int exported = ElectricEnergyExporter.exportAtMost(
                level,
                position,
                outwardFacing,
                electricity.node(),
                remaining
        );
        recordForgeEnergyTransfer(exported);
        return exported;
    }

    public void setStoredJoules(double joules) {
        double bounded = Math.min(nonNegativeFinite(joules), ratedCapacityJoules);
        if (electricity.node().energyJoules() != bounded) {
            electricity.node().setEnergyJoules(bounded);
            host.markChanged();
        }
    }

    /** Restores a previously captured authoritative balance without applying a changed profile clamp. */
    public void restoreStoredJoules(double joules) {
        double restored = nonNegativeFinite(joules);
        if (electricity.node().energyJoules() != restored) {
            electricity.node().setEnergyJoules(restored);
            host.markChanged();
        }
    }

    /** Exact recipe/action withdrawal that fails closed below the profile's operating voltage. */
    public int consumeJoules(int requested, boolean simulate) {
        int amount = Math.max(0, requested);
        if (amount == 0
                || !profileBound
                || profile.role() != ElectricalRole.CONSUMER
                || !tier.meetsMinimumOperatingVoltage(electricity.node().voltage())
                || electricity.node().removeEnergy(amount, true) + 1.0E-9D < amount) {
            return 0;
        }
        if (!simulate) {
            electricity.node().removeEnergy(amount, false);
            host.markChanged();
        }
        return amount;
    }

    /** Bounded generator injection toward the configured generator voltage. */
    public double generateJoules(double requested, boolean simulate) {
        if (!profileBound || profile.role() != ElectricalRole.GENERATOR) {
            return 0.0D;
        }
        double targetEnergy = 0.5D * electricity.node().capacitance()
                * tier.generatorVoltage() * tier.generatorVoltage();
        double acceptedRequest = Math.min(
                Math.min(nonNegativeFinite(requested), maximumTransferJoulesPerTick),
                Math.max(0.0D, targetEnergy - electricity.node().energyJoules())
        );
        double inserted = electricity.node().addEnergy(acceptedRequest, simulate);
        if (inserted > 0.0D && !simulate) {
            host.markChanged();
        }
        return inserted;
    }

    /** Direct native storage insertion used by batteries and explicit conversion inputs. */
    public double storeJoules(double requested, boolean simulate) {
        if (!profileBound) {
            return 0.0D;
        }
        double acceptedRequest = Math.min(
                nonNegativeFinite(requested),
                Math.max(0.0D, ratedCapacityJoules - electricity.node().energyJoules())
        );
        double inserted = electricity.node().addEnergy(acceptedRequest, simulate);
        if (inserted > 0.0D && !simulate) {
            host.markChanged();
        }
        return inserted;
    }

    /** Direct native storage withdrawal used by batteries and explicit conversion boundaries. */
    public double withdrawJoules(double requested, boolean simulate) {
        if (!profileBound) {
            return 0.0D;
        }
        double removed = electricity.node().removeEnergy(nonNegativeFinite(requested), simulate);
        if (removed > 0.0D && !simulate) {
            host.markChanged();
        }
        return removed;
    }

    private double ratedCapacity(MachineElectricalProfile candidate, VoltageTier candidateTier) {
        return tierOverrideAllowed && candidate.role() == ElectricalRole.STORAGE
                ? candidateTier.batteryCapacityJoules()
                : candidate.bufferCapacityJoules();
    }

    private int voltageLimitedOutputRate() {
        if (!profileBound) {
            return 0;
        }
        double fraction = tier.operatingRateFraction(electricity.node().voltage());
        return wholeJoules(fraction * maximumTransferJoulesPerTick);
    }

    private int remainingForgeEnergyTransfer(int limit) {
        int used = currentGameTick() == forgeEnergyTransferTick
                ? forgeEnergyTransferredThisTick
                : 0;
        return Math.max(0, limit - used);
    }

    private void recordForgeEnergyTransfer(int transferred) {
        if (transferred <= 0) {
            return;
        }
        long currentTick = currentGameTick();
        if (currentTick != forgeEnergyTransferTick) {
            forgeEnergyTransferTick = currentTick;
            forgeEnergyTransferredThisTick = 0;
        }
        forgeEnergyTransferredThisTick = (int) Math.min(
                Integer.MAX_VALUE,
                (long) forgeEnergyTransferredThisTick + transferred
        );
    }

    private long currentGameTick() {
        return host.level() == null ? Long.MIN_VALUE : host.level().getGameTime();
    }

    private static int wholeJoules(double joules) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(nonNegativeFinite(joules)));
    }

    private static double nonNegativeFinite(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    public enum ForgeEnergyAccess {
        NONE {
            @Override
            boolean accepts(ElectricalRole role) {
                return role == ElectricalRole.CONSUMER
                        || role == ElectricalRole.GENERATOR
                        || role == ElectricalRole.STORAGE;
            }
        },
        INPUT {
            @Override
            boolean accepts(ElectricalRole role) {
                return role == ElectricalRole.CONVERTER;
            }
        },
        OUTPUT {
            @Override
            boolean accepts(ElectricalRole role) {
                return role == ElectricalRole.CONVERTER;
            }
        };

        abstract boolean accepts(ElectricalRole role);
    }

    private final class ForgeEnergyView implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return receiveForgeEnergy(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return extractForgeEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return storedWholeJoules();
        }

        @Override
        public int getMaxEnergyStored() {
            return ratedCapacityWholeJoules();
        }

        @Override
        public boolean canExtract() {
            return forgeEnergyAccess == ForgeEnergyAccess.OUTPUT;
        }

        @Override
        public boolean canReceive() {
            return forgeEnergyAccess == ForgeEnergyAccess.INPUT;
        }
    }
}
