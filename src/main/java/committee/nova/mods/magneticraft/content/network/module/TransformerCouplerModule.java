package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalProfileController;
import committee.nova.mods.magneticraft.system.network.electric.TransformerCoupler;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfile;
import committee.nova.mods.magneticraft.system.network.runtime.RedstoneControlMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

/** Persistent profile, direction and automation state for one isolated two-terminal transformer. */
public final class TransformerCouplerModule implements MachineModule, ElectricalProfileController {
    public static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String PROFILE_ID_TAG = "transformer_profile_id";
    private static final String REVERSED_TAG = "reversed";
    private static final String REDSTONE_MODE_TAG = "redstone_mode";
    private static final String PROFILE_BOUND_TAG = "profile_bound";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final ElectricalNetworkModule first;
    private final ElectricalNetworkModule second;
    private final TransformerCoupler coupler;
    private ResourceLocation profileId;
    private RedstoneControlMode redstoneMode = RedstoneControlMode.IGNORED;
    private boolean persistenceValid = true;
    private boolean profileBound;

    public TransformerCouplerModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNetworkModule first,
            ElectricalNetworkModule second,
            ResourceLocation defaultProfileId
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.host = Objects.requireNonNull(host, "host");
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
        profileId = Objects.requireNonNull(defaultProfileId, "defaultProfileId");
        coupler = new TransformerCoupler(first.nodeKey(), second.nodeKey(), this::automationEnabled);
        first.attachProfileController(this);
        second.attachProfileController(this);
        ElectricalDataRegistry.INSTANCE.current().ifPresent(this::rebindElectricalProfile);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.isEmpty()) {
            return;
        }
        persistenceValid = tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                && tag.getInt(SCHEMA_VERSION_TAG) == SCHEMA_VERSION
                && tag.contains(PROFILE_ID_TAG, Tag.TAG_STRING)
                && tag.contains(REVERSED_TAG, Tag.TAG_BYTE)
                && tag.contains(REDSTONE_MODE_TAG, Tag.TAG_INT);
        if (!persistenceValid) {
            profileBound = false;
            coupler.clearProfile();
            return;
        }
        ResourceLocation savedProfile = ResourceLocation.tryParse(tag.getString(PROFILE_ID_TAG));
        int redstoneOrdinal = tag.getInt(REDSTONE_MODE_TAG);
        RedstoneControlMode[] modes = RedstoneControlMode.values();
        if (savedProfile == null || redstoneOrdinal < 0 || redstoneOrdinal >= modes.length) {
            persistenceValid = false;
            profileBound = false;
            coupler.clearProfile();
            return;
        }
        profileId = savedProfile;
        coupler.setReversed(tag.getBoolean(REVERSED_TAG));
        redstoneMode = modes[redstoneOrdinal];
        ElectricalDataRegistry.INSTANCE.current().ifPresent(this::rebindElectricalProfile);
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putString(PROFILE_ID_TAG, profileId.toString());
        tag.putBoolean(REVERSED_TAG, coupler.reversed());
        tag.putInt(REDSTONE_MODE_TAG, redstoneMode.ordinal());
    }

    @Override
    public void loadClientData(CompoundTag tag, HolderLookup.Provider registries) {
        ResourceLocation nextProfileId = ResourceLocation.tryParse(tag.getString(PROFILE_ID_TAG));
        int modeOrdinal = tag.getInt(REDSTONE_MODE_TAG);
        RedstoneControlMode[] modes = RedstoneControlMode.values();
        if (nextProfileId == null || modeOrdinal < 0 || modeOrdinal >= modes.length) {
            profileBound = false;
            return;
        }
        profileId = nextProfileId;
        coupler.setReversed(tag.getBoolean(REVERSED_TAG));
        redstoneMode = modes[modeOrdinal];
        profileBound = tag.getBoolean(PROFILE_BOUND_TAG);
    }

    @Override
    public void saveClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString(PROFILE_ID_TAG, profileId.toString());
        tag.putBoolean(REVERSED_TAG, coupler.reversed());
        tag.putInt(REDSTONE_MODE_TAG, redstoneMode.ordinal());
        tag.putBoolean(PROFILE_BOUND_TAG, profileBound);
    }

    @Override
    public void onLoad() {
        first.registerElectricalCoupler(coupler);
    }

    @Override
    public void onUnload() {
        first.unregisterElectricalCoupler(coupler);
    }

    @Override
    public void rebindElectricalProfile(ElectricalDataSnapshot snapshot) {
        Optional<TransformerProfile> next = persistenceValid
                ? snapshot.transformerProfile(profileId)
                : Optional.empty();
        profileBound = next.isPresent()
                && first.tierId().equals(next.get().inputTierId())
                && second.tierId().equals(next.get().outputTierId())
                && snapshot.voltageTier(next.get().inputTierId()).isPresent()
                && snapshot.voltageTier(next.get().outputTierId()).isPresent();
        if (!profileBound) {
            coupler.clearProfile();
            return;
        }
        TransformerProfile profile = next.orElseThrow();
        coupler.reconfigure(
                profile,
                snapshot.voltageTier(profile.inputTierId()).orElseThrow(),
                snapshot.voltageTier(profile.outputTierId()).orElseThrow()
        );
    }

    @Override
    public boolean electricalControllerBound() {
        return profileBound;
    }

    public boolean applyProfileFromPlacementData(ResourceLocation nextProfileId) {
        Optional<ElectricalDataSnapshot> snapshot = ElectricalDataRegistry.INSTANCE.current();
        Optional<TransformerProfile> profile = snapshot.flatMap(value -> value.transformerProfile(nextProfileId));
        if (profile.isEmpty()) {
            return false;
        }
        persistenceValid = true;
        profileId = nextProfileId;
        first.applyTierFromPlacementData(profile.get().inputTierId());
        second.applyTierFromPlacementData(profile.get().outputTierId());
        snapshot.ifPresent(this::rebindElectricalProfile);
        host.markChangedAndSync();
        return profileBound;
    }

    public boolean tryReverse() {
        if (!profileBound || !coupler.canReverse(first.node(), second.node())) {
            return false;
        }
        coupler.setReversed(!coupler.reversed());
        host.markChangedAndSync();
        return true;
    }

    public void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        host.markChangedAndSync();
    }

    public ResourceLocation profileId() {
        return profileId;
    }

    public boolean reversed() {
        return coupler.reversed();
    }

    public RedstoneControlMode redstoneMode() {
        return redstoneMode;
    }

    public ElectricalNetworkModule first() {
        return first;
    }

    public ElectricalNetworkModule second() {
        return second;
    }

    public TransformerCoupler coupler() {
        return coupler;
    }

    private boolean automationEnabled() {
        return host.level() == null || redstoneMode.allows(host.level().hasNeighborSignal(host.position()));
    }
}
