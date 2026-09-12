package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalProfileController;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalTickParticipant;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalRole;
import committee.nova.mods.magneticraft.system.network.electric.profile.MachineElectricalProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * Data-pack-bound 1 J : 1 internal-buffer exchange owned by one native electrical terminal.
 * The physical manager invokes source/storage injection before edges and consumer/converter
 * extraction after edges, so a role can never accidentally perform the opposite operation.
 */
public final class ElectricalEnergyBridgeModule
        implements MachineModule, ElectricalProfileController, ElectricalTickParticipant {
    public static final double STORAGE_TARGET_FRACTION = 0.75D;
    public static final double STORAGE_DEADBAND_FRACTION = 0.02D;

    private final ResourceLocation id;
    private final ResourceLocation profileId;
    private final ElectricalNetworkModule electricity;
    private final EnergyStorageModule energy;
    private final ExchangePolicy exchangePolicy;
    private final boolean tierOverrideAllowed;

    private MachineElectricalProfile profile;
    private VoltageTier tier;
    private boolean profileBound;
    private int lastNetworkToBuffer;
    private int lastBufferToNetwork;

    public ElectricalEnergyBridgeModule(
            ResourceLocation id,
            ResourceLocation profileId,
            ElectricalNetworkModule electricity,
            EnergyStorageModule energy
    ) {
        this(id, profileId, electricity, energy, ExchangePolicy.PROFILE_ROLE, false);
    }

    public ElectricalEnergyBridgeModule(
            ResourceLocation id,
            ResourceLocation profileId,
            ElectricalNetworkModule electricity,
            EnergyStorageModule energy,
            ExchangePolicy exchangePolicy,
            boolean tierOverrideAllowed
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.profileId = Objects.requireNonNull(profileId, "profileId");
        this.electricity = Objects.requireNonNull(electricity, "electricity");
        this.energy = Objects.requireNonNull(energy, "energy");
        this.exchangePolicy = Objects.requireNonNull(exchangePolicy, "exchangePolicy");
        this.tierOverrideAllowed = tierOverrideAllowed;
        electricity.attachProfileController(this);
        electricity.attachTickParticipant(this);
        ElectricalDataRegistry.INSTANCE.current().ifPresent(this::rebindElectricalProfile);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void rebindElectricalProfile(ElectricalDataSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        Optional<MachineElectricalProfile> nextProfile = snapshot.machineProfile(profileId);
        boolean tierBound = nextProfile.isPresent() && (tierOverrideAllowed
                ? snapshot.voltageTier(electricity.tierId()).isPresent()
                : electricity.bindTierFromMachineProfile(snapshot, nextProfile.get().tierId()));
        Optional<VoltageTier> nextTier = tierBound
                ? snapshot.voltageTier(electricity.tierId())
                : Optional.empty();
        profileBound = nextProfile.isPresent()
                && tierBound
                && nextTier.isPresent()
                && exchangePolicy.accepts(nextProfile.get().role());
        if (!profileBound) {
            profile = null;
            tier = null;
            return;
        }

        profile = nextProfile.orElseThrow();
        tier = nextTier.orElseThrow();
        int capacity = tierOverrideAllowed && profile.role() == ElectricalRole.STORAGE
                ? boundedInt(tier.batteryCapacityJoules())
                : boundedInt(profile.bufferCapacityJoules());
        int rate = tierOverrideAllowed && profile.role() == ElectricalRole.STORAGE
                ? boundedInt(tier.batteryTransferJoulesPerTick())
                : boundedInt(profile.maximumTransferJoulesPerTick());
        energy.reconfigureForElectricalProfile(capacity, rate);
    }

    @Override
    public boolean electricalControllerBound() {
        return profileBound;
    }

    @Override
    public double terminalRatedChargePerTick() {
        return profileBound ? profile.terminalRatedChargePerTick() : 0.0D;
    }

    @Override
    public void injectElectricalEnergy(PhysicalNetworkManager manager) {
        lastNetworkToBuffer = 0;
        lastBufferToNetwork = 0;
        if (!profileBound) {
            return;
        }
        ElectricalRole role = profile.role();
        if (exchangePolicy == ExchangePolicy.FE_TO_NATIVE || role == ElectricalRole.GENERATOR) {
            lastBufferToNetwork = moveBufferToNode(tier.generatorVoltage());
        } else if (role == ElectricalRole.STORAGE
                && electricity.node().voltage() < storageLowVoltage()) {
            lastBufferToNetwork = moveBufferToNode(storageTargetVoltage());
        }
    }

    @Override
    public void extractElectricalEnergy(PhysicalNetworkManager manager) {
        if (!profileBound) {
            return;
        }
        ElectricalRole role = profile.role();
        if (exchangePolicy == ExchangePolicy.NATIVE_TO_FE || role == ElectricalRole.CONSUMER) {
            lastNetworkToBuffer = moveNodeToBuffer(voltageLimitedRate());
        } else if (role == ElectricalRole.STORAGE
                && electricity.node().voltage() > storageHighVoltage()) {
            lastNetworkToBuffer = moveNodeToBuffer(maxTransfer());
        }
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

    /** Number of joules moved from the native node into the internal/FE buffer this tick. */
    public int lastChargeTransfer() {
        return lastNetworkToBuffer;
    }

    public int lastDischargeTransfer() {
        return lastBufferToNetwork;
    }

    /** Data-pack authoritative converter/network boundary in joules per tick. */
    public int maximumTransferJoulesPerTick() {
        return profileBound ? maxTransfer() : 0;
    }

    private int voltageLimitedRate() {
        double voltage = electricity.node().voltage();
        if (voltage <= tier.minimumOperatingVoltage()) {
            return 0;
        }
        double fraction = Math.min(1.0D, (voltage - tier.minimumOperatingVoltage())
                / (tier.nominalVoltage() - tier.minimumOperatingVoltage()));
        return (int) Math.floor(maxTransfer() * fraction);
    }

    private int moveNodeToBuffer(int requested) {
        int room = energy.receiveEnergy(Math.min(maxTransfer(), Math.max(0, requested)), true);
        int transferable = wholeJoules(electricity.node().removeEnergy(room, true));
        if (transferable <= 0) {
            return 0;
        }
        double removed = electricity.node().removeEnergy(transferable, false);
        int removedWhole = wholeJoules(removed);
        int inserted = energy.receiveEnergy(removedWhole, false);
        if (inserted < removedWhole) {
            electricity.node().addEnergy(removedWhole - inserted, false);
        }
        return inserted;
    }

    private int moveBufferToNode(double targetVoltage) {
        double targetEnergy = 0.5D * electricity.node().capacitance() * targetVoltage * targetVoltage;
        int room = wholeJoules(Math.max(0.0D, targetEnergy - electricity.node().energyJoules()));
        int available = energy.extractEnergy(Math.min(maxTransfer(), room), true);
        int transferable = wholeJoules(electricity.node().addEnergy(available, true));
        if (transferable <= 0) {
            return 0;
        }
        int extracted = energy.extractEnergy(transferable, false);
        int inserted = wholeJoules(electricity.node().addEnergy(extracted, false));
        if (inserted < extracted) {
            energy.receiveEnergy(extracted - inserted, false);
        }
        return inserted;
    }

    private int maxTransfer() {
        return tierOverrideAllowed && profile.role() == ElectricalRole.STORAGE
                ? boundedInt(tier.batteryTransferJoulesPerTick())
                : boundedInt(profile.maximumTransferJoulesPerTick());
    }

    private double storageTargetVoltage() {
        return tier.nominalVoltage() * STORAGE_TARGET_FRACTION;
    }

    private double storageLowVoltage() {
        return tier.nominalVoltage() * (STORAGE_TARGET_FRACTION - STORAGE_DEADBAND_FRACTION);
    }

    private double storageHighVoltage() {
        return tier.nominalVoltage() * (STORAGE_TARGET_FRACTION + STORAGE_DEADBAND_FRACTION);
    }

    private static int wholeJoules(double joules) {
        return (int) Math.min(Integer.MAX_VALUE, Math.floor(Math.max(0.0D, joules)));
    }

    private static int boundedInt(double value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0D, Math.floor(value)));
    }

    private static int boundedInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, value));
    }

    public enum ExchangePolicy {
        PROFILE_ROLE {
            @Override
            boolean accepts(ElectricalRole role) {
                return role == ElectricalRole.CONSUMER
                        || role == ElectricalRole.GENERATOR
                        || role == ElectricalRole.STORAGE;
            }
        },
        NATIVE_TO_FE {
            @Override
            boolean accepts(ElectricalRole role) {
                return role == ElectricalRole.CONVERTER;
            }
        },
        FE_TO_NATIVE {
            @Override
            boolean accepts(ElectricalRole role) {
                return role == ElectricalRole.CONVERTER;
            }
        };

        abstract boolean accepts(ElectricalRole role);
    }
}
