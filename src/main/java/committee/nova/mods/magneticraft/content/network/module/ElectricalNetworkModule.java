package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultKind;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultSearchResult;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FlowDirection;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.NetworkSummary;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalEdgeTelemetry;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalFaultSource;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalLink;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeAccess;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalProfileController;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalStressState;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalTickParticipant;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalCoupler;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalProfileBinding;
import committee.nova.mods.magneticraft.system.network.electric.profile.MachineElectricalProfile;
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
    public static final int MODULE_SCHEMA_VERSION = 3;
    public static final ResourceLocation LOW_VOLTAGE = VoltageTierIds.LOW;

    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String TERMINAL_ID_TAG = "terminal_id";
    private static final String TIER_ID_TAG = "tier_id";
    private static final String ENERGY_TAG = "energy_joules";
    private static final String STRESS_TAG = "thermal_stress";
    private static final String FAULTED_TAG = "faulted";
    private static final String CLIENT_VOLTAGE_TAG = "voltage";
    private static final String CLIENT_CHARGE_TAG = "charge_per_tick";
    private static final String CLIENT_CURRENT_TAG = "current_amps";
    private static final String CLIENT_JOULES_TAG = "joules_per_tick";
    private static final String CLIENT_POWER_TAG = "power_watts";
    private static final String CLIENT_STORED_TAG = "stored_joules";
    private static final String CLIENT_CAPACITY_TAG = "capacity_joules";
    private static final String CLIENT_LOAD_TAG = "load_ratio";
    private static final String CLIENT_STRESS_TAG = "stress";
    private static final String CLIENT_FLOW_TAG = "flow";
    private static final String CLIENT_FAULT_TAG = "fault";

    private final ElectricalNode node;
    private final ElectricalNodeKind nodeKind;
    private final ResourceLocation terminalId;
    private final Predicate<Direction> sideFilter;
    private final ElectricalStressState stress = new ElectricalStressState();
    private ResourceLocation tierId;
    private boolean tierProfileBound;
    private boolean faulted;
    private IntrinsicDamageProfile intrinsicDamageProfile;
    private ElectricalProfileController profileController;
    private ElectricalTickParticipant tickParticipant;
    private ElectricalReading clientReading;

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
        intrinsicDamageProfile = nodeKind == ElectricalNodeKind.CONDUCTOR
                ? IntrinsicDamageProfile.CABLE
                : IntrinsicDamageProfile.MACHINE;
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

    /** Protection devices own a separate calibrated state machine for their internal edge. */
    public void disableIntrinsicDamage() {
        intrinsicDamageProfile = IntrinsicDamageProfile.NONE;
        stress.reset();
        faulted = false;
    }

    public void useOverheadDamageProfile() {
        intrinsicDamageProfile = IntrinsicDamageProfile.OVERHEAD;
    }

    public double thermalStress() {
        return stress.stress();
    }

    public boolean faulted() {
        return faulted;
    }

    public boolean safeToRepair() {
        if (!faulted) {
            return true;
        }
        return ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(tierId))
                .map(tier -> node.voltage() < tier.nominalVoltage() * 0.95D)
                .orElse(false);
    }

    /** Called only after the host has atomically validated every terminal. */
    public boolean repairFault() {
        if (!faulted || !safeToRepair()) {
            return false;
        }
        faulted = false;
        stress.reset();
        markStateChanged();
        topologyChanged();
        return true;
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

    public void setInternalConnection(ElectricalNetworkModule other, boolean closed) {
        Objects.requireNonNull(other, "other");
        PhysicalNetworkManager manager = manager();
        if (manager != null && manager == other.manager()) {
            manager.setInternalConnection(nodeKey(), other.nodeKey(), closed);
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
        Optional<VoltageTier> tier = snapshot.voltageTier(profileTierId);
        return tier.isPresent() && bindTierFromMachineProfile(
                snapshot,
                profileTierId,
                nodeKind.capacitance(tier.orElseThrow())
        );
    }

    boolean bindMachineProfile(ElectricalDataSnapshot snapshot, MachineElectricalProfile profile) {
        Objects.requireNonNull(profile, "profile");
        Optional<VoltageTier> tier = snapshot.voltageTier(profile.tierId());
        return tier.isPresent() && bindTierFromMachineProfile(
                snapshot,
                profile.tierId(),
                profile.nodeCapacitanceFarads(tier.orElseThrow())
        );
    }

    boolean bindTierFromMachineProfile(
            ElectricalDataSnapshot snapshot,
            ResourceLocation profileTierId,
            double capacitanceFarads
    ) {
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
                capacitanceFarads,
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
        ElectricalReading reading = liveReading();
        tag.putString(TIER_ID_TAG, tierId.toString());
        tag.putDouble(CLIENT_VOLTAGE_TAG, reading.voltageVolts());
        tag.putDouble(CLIENT_CHARGE_TAG, reading.chargeCoulombsPerTick());
        tag.putDouble(CLIENT_CURRENT_TAG, reading.currentAmps());
        tag.putDouble(CLIENT_JOULES_TAG, reading.joulesPerTick());
        tag.putDouble(CLIENT_POWER_TAG, reading.powerWatts());
        tag.putDouble(CLIENT_STORED_TAG, reading.storedJoules());
        tag.putDouble(CLIENT_CAPACITY_TAG, reading.capacityJoules());
        tag.putDouble(CLIENT_LOAD_TAG, reading.loadRatio());
        tag.putDouble(CLIENT_STRESS_TAG, reading.thermalStress());
        tag.putString(CLIENT_FLOW_TAG, reading.flowDirection().name());
        tag.putString(CLIENT_FAULT_TAG, reading.faultKind().name());
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        if (!tag.contains(TIER_ID_TAG, Tag.TAG_STRING)) {
            return;
        }
        ResourceLocation syncedTier = ResourceLocation.tryParse(tag.getString(TIER_ID_TAG));
        boolean tierChanged = syncedTier != null && !tierId.equals(syncedTier);
        if (syncedTier != null) {
            tierId = syncedTier;
        }
        if (tierChanged) {
            host().requestModelRefresh();
        }
        if (!tag.contains(CLIENT_VOLTAGE_TAG, Tag.TAG_DOUBLE)
                || !tag.contains(CLIENT_CHARGE_TAG, Tag.TAG_DOUBLE)
                || !tag.contains(CLIENT_CURRENT_TAG, Tag.TAG_DOUBLE)
                || !tag.contains(CLIENT_JOULES_TAG, Tag.TAG_DOUBLE)
                || !tag.contains(CLIENT_POWER_TAG, Tag.TAG_DOUBLE)
                || !tag.contains(CLIENT_STORED_TAG, Tag.TAG_DOUBLE)
                || !tag.contains(CLIENT_CAPACITY_TAG, Tag.TAG_DOUBLE)
                || !tag.contains(CLIENT_LOAD_TAG, Tag.TAG_DOUBLE)
                || !tag.contains(CLIENT_STRESS_TAG, Tag.TAG_DOUBLE)) {
            clientReading = null;
            return;
        }
        clientReading = new ElectricalReading(
                tierId,
                terminalId,
                nonNegativeFinite(tag.getDouble(CLIENT_VOLTAGE_TAG)),
                nonNegativeFinite(tag.getDouble(CLIENT_CHARGE_TAG)),
                nonNegativeFinite(tag.getDouble(CLIENT_CURRENT_TAG)),
                nonNegativeFinite(tag.getDouble(CLIENT_JOULES_TAG)),
                nonNegativeFinite(tag.getDouble(CLIENT_POWER_TAG)),
                nonNegativeFinite(tag.getDouble(CLIENT_STORED_TAG)),
                nonNegativeFinite(tag.getDouble(CLIENT_CAPACITY_TAG)),
                nonNegativeFinite(tag.getDouble(CLIENT_LOAD_TAG)),
                Math.max(0.0D, Math.min(1.0D, nonNegativeFinite(tag.getDouble(CLIENT_STRESS_TAG)))),
                enumValue(FlowDirection.class, tag.getString(CLIENT_FLOW_TAG), FlowDirection.IDLE),
                enumValue(FaultKind.class, tag.getString(CLIENT_FAULT_TAG), FaultKind.MISSING_PROFILE)
        );
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
        if (!isSideEnabled(side)) {
            return Optional.empty();
        }
        return Optional.of(displayReading());
    }

    /** Immutable display snapshot; on the logical client this never reads authoritative server state. */
    public ElectricalReading displayReading() {
        return host().level() != null && host().level().isClientSide && clientReading != null
                ? clientReading
                : liveReading();
    }

    Optional<ElectricalReading> syncedClientReading() {
        return Optional.ofNullable(clientReading);
    }

    /** Quantized hash used by the host's bounded block-entity update cadence. */
    public int clientStateHash() {
        ElectricalReading reading = liveReading();
        return Objects.hash(
                reading.tierId(),
                quantized(reading.voltageVolts(), 10.0D),
                quantized(reading.chargeCoulombsPerTick(), 100.0D),
                quantized(reading.joulesPerTick(), 10.0D),
                quantized(reading.storedJoules(), 1.0D),
                quantized(reading.loadRatio(), 1_000.0D),
                quantized(reading.thermalStress(), 1_000.0D),
                reading.flowDirection(),
                reading.faultKind()
        );
    }

    private ElectricalReading liveReading() {
        PhysicalNetworkManager manager = manager();
        double ratedCurrent = electricalRatedCurrentAmps();
        double terminalCurrent = manager == null
                ? node.lastCompletedTickCurrentAmps()
                : manager.maximumTerminalCurrentAmps(nodeKey());
        return new ElectricalReading(
                tierId,
                terminalId,
                node.voltage(),
                node.lastCompletedTickChargeCoulombs(),
                node.lastCompletedTickCurrentAmps(),
                node.lastCompletedTickJoules(),
                node.lastCompletedTickPowerWatts(),
                node.energyJoules(),
                node.ratedMaximumEnergyJoules(),
                ratedCurrent > 0.0D && Double.isFinite(ratedCurrent)
                        ? terminalCurrent / ratedCurrent
                        : 0.0D,
                electricalThermalStress(),
                completedFlowDirection(manager),
                electricalFaultKind()
        );
    }

    @Override
    public Optional<NetworkSummary> electricalNetworkSummary(Direction side, int maxVisitedNodes) {
        PhysicalNetworkManager manager = manager();
        return !isSideEnabled(side) || manager == null
                ? Optional.empty()
                : manager.electricalNetworkSummary(nodeKey(), maxVisitedNodes);
    }

    @Override
    public Optional<FaultSearchResult> nearestElectricalFault(Direction side, int maxVisitedNodes) {
        PhysicalNetworkManager manager = manager();
        return !isSideEnabled(side) || manager == null
                ? Optional.empty()
                : manager.nearestElectricalFault(nodeKey(), maxVisitedNodes);
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
        return !faulted
                && tierProfileBound
                && (profileController == null || profileController.electricalControllerBound());
    }

    @Override
    public double electricalRatedCurrentAmps() {
        ElectricalFaultSource source = deviceFaultSource();
        if (source != null && source.electricalRatedCurrentAmps() > 0.0D) {
            return source.electricalRatedCurrentAmps();
        }
        return ratedChargePerTick() * ElectricalNode.TICKS_PER_SECOND;
    }

    @Override
    public double electricalThermalStress() {
        ElectricalFaultSource source = deviceFaultSource();
        double deviceStress = source == null ? 0.0D : source.electricalThermalStress();
        return Math.max(0.0D, Math.min(1.0D, Math.max(stress.stress(), deviceStress)));
    }

    @Override
    public FaultKind electricalFaultKind() {
        if (faulted) {
            return FaultKind.MACHINE_FAULT;
        }
        if (!tierProfileBound || (profileController != null && !profileController.electricalControllerBound())) {
            return FaultKind.MISSING_PROFILE;
        }
        ElectricalFaultSource source = deviceFaultSource();
        return source == null ? FaultKind.NONE : source.electricalFaultKind();
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
        updateElectricalDamage(manager);
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
        tierProfileBound = false;
        if (tag.getInt(SCHEMA_VERSION_TAG) != MODULE_SCHEMA_VERSION) {
            node.setEnergyJoules(0.0D);
            stress.reset();
            faulted = false;
            finishLoadedBinding();
            return;
        }
        ResourceLocation savedTerminal = ResourceLocation.tryParse(tag.getString(TERMINAL_ID_TAG));
        ResourceLocation savedTier = ResourceLocation.tryParse(tag.getString(TIER_ID_TAG));
        if (!terminalId.equals(savedTerminal) || savedTier == null) {
            node.setEnergyJoules(0.0D);
            stress.reset();
            faulted = false;
            finishLoadedBinding();
            return;
        }
        tierId = savedTier;
        node.setEnergyJoules(tag.getDouble(ENERGY_TAG));
        stress.restore(tag.getDouble(STRESS_TAG));
        faulted = tag.getBoolean(FAULTED_TAG);
        finishLoadedBinding();
    }

    @Override
    protected void saveNetworkData(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION_TAG, MODULE_SCHEMA_VERSION);
        tag.putString(TERMINAL_ID_TAG, terminalId.toString());
        tag.putString(TIER_ID_TAG, tierId.toString());
        tag.putDouble(ENERGY_TAG, node.energyJoules());
        tag.putDouble(STRESS_TAG, stress.stress());
        tag.putBoolean(FAULTED_TAG, faulted);
    }

    private void updateElectricalDamage(PhysicalNetworkManager manager) {
        if (intrinsicDamageProfile == IntrinsicDamageProfile.NONE || faulted || !tierProfileBound) {
            return;
        }
        Optional<VoltageTier> tier = ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(tierId));
        if (tier.isEmpty()) {
            return;
        }
        VoltageTier value = tier.orElseThrow();
        double ratedChargePerTick = ratedChargePerTick(value);
        double thermalCapacity = intrinsicDamageProfile == IntrinsicDamageProfile.OVERHEAD
                ? value.overheadThermalCapacity()
                : value.cableThermalCapacity();
        double coolingPerTick = intrinsicDamageProfile == IntrinsicDamageProfile.OVERHEAD
                ? value.overheadCoolingPerTick()
                : value.cableCoolingPerTick();
        double before = stress.stress();
        boolean failedNow = stress.update(
                manager.maximumTerminalCurrentAmps(nodeKey()),
                ratedChargePerTick * ElectricalNode.TICKS_PER_SECOND,
                node.voltage(),
                value.maximumVoltage(),
                thermalCapacity,
                coolingPerTick,
                MagneticraftConfig.ENABLE_ELECTRICAL_DAMAGE.get() && !manager.electricalDamageSuppressed()
        );
        if (stress.stress() != before) {
            markStateChanged();
        }
        if (failedNow) {
            faulted = true;
            markStateChanged();
            topologyChanged();
        }
    }

    private enum IntrinsicDamageProfile {
        NONE,
        CABLE,
        OVERHEAD,
        MACHINE
    }

    private double ratedChargePerTick() {
        return ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(tierId))
                .map(this::ratedChargePerTick)
                .orElse(0.0D);
    }

    private double ratedChargePerTick(VoltageTier tier) {
        double controllerRating = profileController == null
                ? 0.0D
                : profileController.terminalRatedChargePerTick();
        if (controllerRating > 0.0D) {
            return controllerRating;
        }
        return switch (intrinsicDamageProfile) {
            case CABLE -> tier.cableRatedChargePerTick();
            case OVERHEAD -> tier.overheadRatedChargePerTick();
            case MACHINE, NONE -> tier.heavyProtectionChargePerTick();
        };
    }

    private ElectricalFaultSource deviceFaultSource() {
        if (tickParticipant instanceof ElectricalFaultSource source) {
            return source;
        }
        return profileController instanceof ElectricalFaultSource source ? source : null;
    }

    private FlowDirection completedFlowDirection(PhysicalNetworkManager manager) {
        if (manager == null) {
            return FlowDirection.IDLE;
        }
        boolean input = false;
        boolean output = false;
        for (ElectricalEdgeTelemetry telemetry : manager.electricalEdgeTelemetry()) {
            boolean first = telemetry.firstTerminal().equals(nodeKey());
            boolean second = telemetry.secondTerminal().equals(nodeKey());
            if ((!first && !second)
                    || telemetry.deliveredJoulesPerTick() + telemetry.lostJoulesPerTick() <= 0.0D) {
                continue;
            }
            boolean source = first ? telemetry.firstWasSource() : !telemetry.firstWasSource();
            output |= source;
            input |= !source;
        }
        if (input && output) {
            return FlowDirection.BIDIRECTIONAL;
        }
        if (input) {
            return FlowDirection.INPUT;
        }
        return output ? FlowDirection.OUTPUT : FlowDirection.IDLE;
    }

    private static double nonNegativeFinite(double value) {
        return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    private static long quantized(double value, double scale) {
        double scaled = value * scale;
        return Double.isFinite(scaled) ? Math.round(scaled) : Long.MAX_VALUE;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
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
