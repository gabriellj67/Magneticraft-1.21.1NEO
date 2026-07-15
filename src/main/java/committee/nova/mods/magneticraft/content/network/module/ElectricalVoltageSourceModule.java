package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
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

/** Profile-bound ideal source that adds energy toward a tier setpoint without deleting overvoltage energy. */
public final class ElectricalVoltageSourceModule
        implements MachineModule, ElectricalProfileController, ElectricalTickParticipant {
    private final ResourceLocation id;
    private final ResourceLocation profileId;
    private final MachineModuleHost host;
    private final ElectricalNetworkModule electricity;
    private final boolean tierOverrideAllowed;
    private MachineElectricalProfile profile;
    private VoltageTier tier;
    private boolean profileBound;
    private double lastProductionJoules;

    public ElectricalVoltageSourceModule(
            ResourceLocation id,
            ResourceLocation profileId,
            MachineModuleHost host,
            ElectricalNetworkModule electricity,
            boolean tierOverrideAllowed
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.profileId = Objects.requireNonNull(profileId, "profileId");
        this.host = Objects.requireNonNull(host, "host");
        this.electricity = Objects.requireNonNull(electricity, "electricity");
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
        Optional<MachineElectricalProfile> nextProfile = snapshot.machineProfile(profileId);
        Optional<VoltageTier> selectedTier = nextProfile.isEmpty()
                ? Optional.empty()
                : snapshot.voltageTier(tierOverrideAllowed
                ? electricity.tierId()
                : nextProfile.orElseThrow().tierId());
        boolean tierBound = nextProfile.isPresent()
                && selectedTier.isPresent()
                && electricity.bindTierFromMachineProfile(
                snapshot,
                selectedTier.orElseThrow().id(),
                nextProfile.orElseThrow().nodeCapacitanceFarads(selectedTier.orElseThrow())
        );
        Optional<VoltageTier> nextTier = tierBound
                ? snapshot.voltageTier(electricity.tierId())
                : Optional.empty();
        profileBound = nextProfile.isPresent()
                && nextTier.isPresent()
                && nextProfile.get().role() == ElectricalRole.GENERATOR
                && tierBound;
        profile = profileBound ? nextProfile.orElseThrow() : null;
        tier = profileBound ? nextTier.orElseThrow() : null;
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
        lastProductionJoules = 0.0D;
        if (!profileBound) {
            return;
        }
        double targetEnergy = 0.5D * electricity.node().capacitance()
                * tier.generatorVoltage() * tier.generatorVoltage();
        double requested = Math.min(
                profile.maximumTransferJoulesPerTick(),
                Math.max(0.0D, targetEnergy - electricity.node().energyJoules())
        );
        lastProductionJoules = electricity.node().addEnergy(requested, false);
        if (lastProductionJoules > 0.0D) {
            host.markChanged();
        }
    }

    public double lastProductionJoules() {
        return lastProductionJoules;
    }
}
