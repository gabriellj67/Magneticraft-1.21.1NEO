package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalProfileController;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalRole;
import committee.nova.mods.magneticraft.system.network.electric.profile.MachineElectricalProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/** Fail-closed profile binding for devices whose specialized behavior has no buffer exchange. */
public final class ElectricalProfileGateModule implements MachineModule, ElectricalProfileController {
    private final ResourceLocation id;
    private final ResourceLocation profileId;
    private final ElectricalNetworkModule electricity;
    private final ElectricalRole expectedRole;
    private MachineElectricalProfile profile;
    private VoltageTier tier;
    private boolean profileBound;

    public ElectricalProfileGateModule(
            ResourceLocation id,
            ResourceLocation profileId,
            ElectricalNetworkModule electricity,
            ElectricalRole expectedRole
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.profileId = Objects.requireNonNull(profileId, "profileId");
        this.electricity = Objects.requireNonNull(electricity, "electricity");
        this.expectedRole = Objects.requireNonNull(expectedRole, "expectedRole");
        electricity.attachProfileController(this);
        ElectricalDataRegistry.INSTANCE.current().ifPresent(this::rebindElectricalProfile);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void rebindElectricalProfile(ElectricalDataSnapshot snapshot) {
        Optional<MachineElectricalProfile> nextProfile = snapshot.machineProfile(profileId)
                .filter(profile -> profile.role() == expectedRole)
                .filter(profile -> electricity.bindMachineProfile(snapshot, profile));
        Optional<VoltageTier> nextTier = nextProfile.flatMap(profile -> snapshot.voltageTier(profile.tierId()));
        profileBound = nextProfile.isPresent() && nextTier.isPresent();
        profile = profileBound ? nextProfile.orElseThrow() : null;
        tier = profileBound ? nextTier.orElseThrow() : null;
    }

    @Override
    public boolean electricalControllerBound() {
        return profileBound;
    }

    public Optional<MachineElectricalProfile> profile() {
        return Optional.ofNullable(profile);
    }

    public Optional<VoltageTier> tier() {
        return Optional.ofNullable(tier);
    }
}
