package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalEdgeTelemetry;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalLink;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeAccess;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalProfileController;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalTickParticipant;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalCoupler;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalProfileBinding;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/** Persisted, tier-bound Magneticraft electrical terminal; Forge Energy is intentionally absent. */
public final class ElectricalNetworkModule extends AbstractPhysicalNetworkModule
        implements ElectricalDiagnosticSource, ElectricalNodeAccess, ElectricalProfileBinding, ElectricalTickParticipant {
    public static final int MODULE_SCHEMA_VERSION = 2;
    public static final ResourceLocation LOW_VOLTAGE = VoltageTierIds.LOW;

    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String TERMINAL_ID_TAG = "terminal_id";
    private static final String TIER_ID_TAG = "tier_id";
    private static final String ENERGY_TAG = "energy_joules";

    private final ElectricalNode node;
    private final ElectricalNodeKind nodeKind;
    private final ResourceLocation terminalId;
    private final Predicate<Direction> sideFilter;
    private ResourceLocation tierId;
    private boolean tierProfileBound;
    private ElectricalProfileController profileController;
    private ElectricalTickParticipant tickParticipant;

    /**
     * Compatibility constructor for isolated tests and old call sites. Production nodes should
     * select their node kind explicitly so a data reload can rebind capacitance correctly.
     */
    public ElectricalNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNode node,
            Predicate<Direction> sideFilter
    ) {
        this(id, host, node, LOW_VOLTAGE, PhysicalNodeKey.MAIN_TERMINAL, ElectricalNodeKind.MACHINE, sideFilter);
        tierProfileBound = true;
    }

    public ElectricalNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNodeKind nodeKind,
            Predicate<Direction> sideFilter
    ) {
        this(id, host, LOW_VOLTAGE, PhysicalNodeKey.MAIN_TERMINAL, nodeKind, sideFilter);
    }

    public ElectricalNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            ResourceLocation tierId,
            ElectricalNodeKind nodeKind,
            Predicate<Direction> sideFilter
    ) {
        this(id, host, tierId, PhysicalNodeKey.MAIN_TERMINAL, nodeKind, sideFilter);
    }

    public ElectricalNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            ResourceLocation tierId,
            ResourceLocation terminalId,
            ElectricalNodeKind nodeKind,
            Predicate<Direction> sideFilter
    ) {
        this(
                id,
                host,
                fallbackNode(nodeKind),
                tierId,
                terminalId,
                nodeKind,
                sideFilter
        );
    }

    private ElectricalNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNode node,
            ResourceLocation tierId,
            ResourceLocation terminalId,
            ElectricalNodeKind nodeKind,
            Predicate<Direction> sideFilter
    ) {
        super(id, host, NetworkDomain.ELECTRICITY);
        this.node = Objects.requireNonNull(node, "node");
        this.tierId = Objects.requireNonNull(tierId, "tierId");
        this.terminalId = Objects.requireNonNull(terminalId, "terminalId");
        this.nodeKind = Objects.requireNonNull(nodeKind, "nodeKind");
        this.sideFilter = Objects.requireNonNull(sideFilter, "sideFilter");
    }

    public ElectricalNode node() {
        return node;
    }

    @Override
    public int persistenceSchemaVersion() {
        return MODULE_SCHEMA_VERSION;
    }

    public ResourceLocation tierId() {
        return tierId;
    }

    public ResourceLocation terminalId() {
        return terminalId;
    }

    public ElectricalNodeKind nodeKind() {
        return nodeKind;
    }

    /** Attaches one device-level profile gate. Transformer controllers may attach to two terminals. */
    public void attachProfileController(ElectricalProfileController controller) {
        Objects.requireNonNull(controller, "controller");
        if (profileController != null && profileController != controller) {
            throw new IllegalStateException("Electrical terminal already owns a profile controller");
        }
        profileController = controller;
    }

    /** Attaches the deterministic source/sink participant executed by the physical manager. */
    public void attachTickParticipant(ElectricalTickParticipant participant) {
        Objects.requireNonNull(participant, "participant");
        if (tickParticipant != null && tickParticipant != participant) {
            throw new IllegalStateException("Electrical terminal already owns a tick participant");
        }
        tickParticipant = participant;
    }

    public void registerElectricalCoupler(ElectricalCoupler coupler) {
        PhysicalNetworkManager manager = manager();
        if (manager != null) {
            manager.registerElectricalCoupler(coupler);
        }
    }

    public void unregisterElectricalCoupler(ElectricalCoupler coupler) {
        PhysicalNetworkManager manager = manager();
        if (manager != null) {
            manager.unregisterElectricalCoupler(coupler);
        }
    }

    /** Applies already-validated placement/item data; no player interaction calls this method directly. */
    public void applyTierFromPlacementData(ResourceLocation nextTierId) {
        Objects.requireNonNull(nextTierId, "nextTierId");
        boolean changed = !tierId.equals(nextTierId);
        if (changed) {
            tierId = nextTierId;
        }
        ElectricalDataRegistry.INSTANCE.current().ifPresentOrElse(
                this::rebindElectricalProfile,
                () -> tierProfileBound = false
        );
        if (changed || networkRegistered()) {
            topologyChanged();
        }
    }

    /**
     * Binds a fixed machine terminal to the tier selected by its data-pack profile.
     * Tiered placeable devices bypass this path so their validated item identity remains authoritative.
     */
    boolean bindTierFromMachineProfile(ElectricalDataSnapshot snapshot, ResourceLocation profileTierId) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(profileTierId, "profileTierId");
        Optional<VoltageTier> tier = snapshot.voltageTier(profileTierId);
        if (tier.isEmpty()) {
            tierProfileBound = false;
            return false;
        }
        boolean changed = !tierId.equals(profileTierId);
        tierId = profileTierId;
        tierProfileBound = true;
        VoltageTier value = tier.orElseThrow();
        node.reconfigure(
                nodeKind.capacitance(value),
                value.maximumVoltage(),
                value.nodeResistanceOhms()
        );
        if (changed && networkRegistered()) {
            topologyChanged();
        }
        return true;
    }

    @Override
    public void saveClientData(CompoundTag tag) {
        tag.putString(TIER_ID_TAG, tierId.toString());
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        if (!tag.contains(TIER_ID_TAG, Tag.TAG_STRING)) {
            return;
        }
        ResourceLocation syncedTier = ResourceLocation.tryParse(tag.getString(TIER_ID_TAG));
        if (syncedTier != null) {
            tierId = syncedTier;
        }
    }

    @Override
    public PhysicalNodeKey nodeKey() {
        return new PhysicalNodeKey(position(), terminalId);
    }

    @Override
    public void onLoad() {
        ElectricalDataRegistry.INSTANCE.current().ifPresentOrElse(
                this::rebindElectricalProfile,
                () -> tierProfileBound = false
        );
        super.onLoad();
    }

    @Override
    public Optional<ElectricalReading> electricalReading(Direction side) {
        if (!electricalProfileBound() || !isSideEnabled(side)) {
            return Optional.empty();
        }
        return Optional.of(new ElectricalReading(
                node.voltage(),
                node.lastCompletedTickCurrentAmps(),
                node.lastCompletedTickPowerWatts()
        ));
    }

    @Override
    protected boolean supportsSide(Direction direction) {
        return sideFilter.test(direction);
    }

    @Override
    public boolean canConnect(Direction side, PhysicalNetworkNode other) {
        if (!electricalProfileBound() || !super.canConnect(side, other)) {
            return false;
        }
        PhysicalNetworkNode transferNode = other.transferNode();
        return transferNode instanceof ElectricalNodeAccess access
                && access.electricalProfileBound()
                && tierId.equals(access.electricalTierId());
    }

    /** Compatibility entry point; the manager owns execution and edge telemetry. */
    public ElectricalLink.Transfer exchangeLongDistance(ElectricalNetworkModule other, double distance) {
        Objects.requireNonNull(other, "other");
        PhysicalNetworkManager manager = manager();
        if (manager == null || manager != other.manager()) {
            return ElectricalLink.Transfer.ZERO;
        }
        return manager.transferElectrical(
                nodeKey(),
                other.nodeKey(),
                distance,
                ElectricalEdgeTelemetry.EdgeType.LONG_DISTANCE
        );
    }

    @Override
    public ElectricalNode electricalNode() {
        return node;
    }

    @Override
    public ResourceLocation electricalTierId() {
        return tierId;
    }

    @Override
    public boolean electricalProfileBound() {
        return tierProfileBound
                && (profileController == null || profileController.electricalControllerBound());
    }

    @Override
    public void markElectricalStateChanged() {
        markStateChanged();
    }

    @Override
    public void rebindElectricalProfile(ElectricalDataSnapshot snapshot) {
        Optional<VoltageTier> tier = snapshot.voltageTier(tierId);
        tierProfileBound = tier.isPresent();
        tier.ifPresent(value -> node.reconfigure(
                nodeKind.capacitance(value),
                value.maximumVoltage(),
                value.nodeResistanceOhms()
        ));
        if (profileController != null) {
            profileController.rebindElectricalProfile(snapshot);
        }
    }

    @Override
    public void injectElectricalEnergy(PhysicalNetworkManager manager) {
        if (electricalProfileBound() && tickParticipant != null) {
            tickParticipant.injectElectricalEnergy(manager);
        }
    }

    @Override
    public void extractElectricalEnergy(PhysicalNetworkManager manager) {
        if (electricalProfileBound() && tickParticipant != null) {
            tickParticipant.extractElectricalEnergy(manager);
        }
    }

    @Override
    public void commitElectricalState(PhysicalNetworkManager manager) {
        if (tickParticipant != null) {
            tickParticipant.commitElectricalState(manager);
        }
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
        tierProfileBound = false;
        if (tag.getInt(SCHEMA_VERSION_TAG) != MODULE_SCHEMA_VERSION) {
            node.setEnergyJoules(0.0D);
            finishLoadedBinding();
            return;
        }
        ResourceLocation savedTerminal = ResourceLocation.tryParse(tag.getString(TERMINAL_ID_TAG));
        ResourceLocation savedTier = ResourceLocation.tryParse(tag.getString(TIER_ID_TAG));
        if (!terminalId.equals(savedTerminal) || savedTier == null) {
            node.setEnergyJoules(0.0D);
            finishLoadedBinding();
            return;
        }
        tierId = savedTier;
        node.setEnergyJoules(tag.getDouble(ENERGY_TAG));
        finishLoadedBinding();
    }

    @Override
    protected void saveNetworkData(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION_TAG, MODULE_SCHEMA_VERSION);
        tag.putString(TERMINAL_ID_TAG, terminalId.toString());
        tag.putString(TIER_ID_TAG, tierId.toString());
        tag.putDouble(ENERGY_TAG, node.energyJoules());
    }

    private static ElectricalNode fallbackNode(ElectricalNodeKind nodeKind) {
        double capacitance = nodeKind == ElectricalNodeKind.CONDUCTOR ? 0.5D : 2.0D;
        return new ElectricalNode(capacitance, 125.0D, 0.005D);
    }

    private void finishLoadedBinding() {
        ElectricalDataRegistry.INSTANCE.current().ifPresentOrElse(
                this::rebindElectricalProfile,
                () -> tierProfileBound = false
        );
        PhysicalNetworkManager manager = manager();
        if (networkRegistered() && manager != null) {
            manager.update(this);
        }
    }
}
