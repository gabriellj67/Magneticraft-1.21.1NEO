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
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.Optional;

/** One-way, 1:1 joule-isolated MV-to-LV wireless transfer. */
public final class TeslaTowerModule
        implements MachineModule, ElectricalProfileController, ElectricalTickParticipant {
    public static final double RANGE_BLOCKS = 32.0D;

    private final ResourceLocation id;
    private final ResourceLocation profileId;
    private final MachineModuleHost host;
    private final ElectricalNetworkModule electricity;
    private MachineElectricalProfile profile;
    private VoltageTier inputTier;
    private VoltageTier outputTier;
    private boolean profileBound;
    private double lastTransferJoules;

    public TeslaTowerModule(
            ResourceLocation id,
            ResourceLocation profileId,
            MachineModuleHost host,
            ElectricalNetworkModule electricity
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.profileId = Objects.requireNonNull(profileId, "profileId");
        this.host = Objects.requireNonNull(host, "host");
        this.electricity = Objects.requireNonNull(electricity, "electricity");
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
        boolean inputBound = nextProfile.isPresent()
                && electricity.bindTierFromMachineProfile(snapshot, nextProfile.get().tierId());
        Optional<VoltageTier> nextInput = inputBound
                ? snapshot.voltageTier(electricity.tierId())
                : Optional.empty();
        Optional<VoltageTier> nextOutput = snapshot.voltageTier(VoltageTierIds.LOW);
        profileBound = nextProfile.isPresent()
                && nextInput.isPresent()
                && nextOutput.isPresent()
                && nextProfile.get().role() == ElectricalRole.CONVERTER
                && inputBound;
        profile = profileBound ? nextProfile.orElseThrow() : null;
        inputTier = profileBound ? nextInput.orElseThrow() : null;
        outputTier = profileBound ? nextOutput.orElseThrow() : null;
    }

    @Override
    public boolean electricalControllerBound() {
        return profileBound;
    }

    @Override
    public void extractElectricalEnergy(PhysicalNetworkManager manager) {
        lastTransferJoules = 0.0D;
        if (!profileBound
                || electricity.node().voltage() < inputTier.minimumOperatingVoltage()
                || !(host.level() instanceof ServerLevel level)) {
            return;
        }

        double remaining = profile.maximumTransferJoulesPerTick();
        for (var receiver : LongDistanceElectricityService.get(level).receiversWithin(host.position(), RANGE_BLOCKS)) {
            if (remaining <= 0.0D
                    || electricity.node().voltage() < inputTier.minimumOperatingVoltage()
                    || !receiver.electricity().electricalProfileBound()
                    || !receiver.electricity().tierId().equals(VoltageTierIds.LOW)) {
                continue;
            }
            double targetEnergy = 0.5D * receiver.electricity().node().capacitance()
                    * outputTier.generatorVoltage() * outputTier.generatorVoltage();
            double destinationRoom = Math.max(
                    0.0D,
                    targetEnergy - receiver.electricity().node().energyJoules()
            );
            double offered = Math.min(remaining, destinationRoom);
            double removable = electricity.node().removeEnergy(offered, true);
            double acceptable = receiver.electricity().node().addEnergy(removable, true);
            double transfer = Math.min(removable, acceptable);
            if (transfer <= 0.0D) {
                continue;
            }
            double removed = electricity.node().removeEnergy(transfer, false);
            double inserted = receiver.electricity().node().addEnergy(removed, false);
            if (inserted < removed) {
                electricity.node().addEnergy(removed - inserted, false);
            }
            if (inserted > 0.0D) {
                remaining -= inserted;
                lastTransferJoules += inserted;
                receiver.markChanged();
            }
        }
        if (lastTransferJoules > 0.0D) {
            host.markChanged();
        }
    }

    public double lastTransferJoules() {
        return lastTransferJoules;
    }
}
