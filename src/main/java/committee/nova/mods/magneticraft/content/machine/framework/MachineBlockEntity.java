package committee.nova.mods.magneticraft.content.machine.framework;

import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.system.network.diagnostic.DiagnosticHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.diagnostic.ThermalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.List;

/**
 * Base block entity that owns module identity, persistence and capability lifecycle.
 */
public abstract class MachineBlockEntity extends BlockEntity
        implements MachineModuleHost, DiagnosticHost, NetworkConnectionHost {
    public static final String MODULES_TAG = "modules";
    static final String SCHEMA_VERSION_TAG = "schema_version";
    static final int INITIAL_SCHEMA_VERSION = 1;
    private static final int CLIENT_STATE_SYNC_INTERVAL = 4;

    private final MachineModuleContainer modules = new MachineModuleContainer();
    private int rendererClientStateHash;
    private boolean hasRendererClientStateHash;
    private int lastSentClientStateHash;
    private boolean hasSentClientStateHash;
    private boolean clientSyncRequested;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state);
    }

    protected final <T extends MachineModule> T addModule(T module) {
        return modules.add(module);
    }

    protected final void tickModules() {
        for (MachineModule module : modules.values()) {
            if (!electricalFaulted() || module instanceof ElectricalNetworkModule) {
                module.serverTick();
            }
        }
    }

    public final List<ElectricalNetworkModule> electricalTerminals() {
        return modules.values().stream()
                .filter(ElectricalNetworkModule.class::isInstance)
                .map(ElectricalNetworkModule.class::cast)
                .toList();
    }

    public final boolean electricalFaulted() {
        return modules.values().stream()
                .filter(ElectricalNetworkModule.class::isInstance)
                .map(ElectricalNetworkModule.class::cast)
                .anyMatch(ElectricalNetworkModule::faulted);
    }

    /** Repairs every terminal as one device transaction after validating all terminal voltages. */
    public final boolean tryRepairElectricalFault() {
        List<ElectricalNetworkModule> terminals = electricalTerminals();
        if (terminals.stream().noneMatch(ElectricalNetworkModule::faulted)
                || terminals.stream().anyMatch(terminal -> !terminal.safeToRepair())) {
            return false;
        }
        terminals.forEach(ElectricalNetworkModule::repairFault);
        markChangedAndSync();
        return true;
    }

    /** Returns whether one physical-network module exposes the requested face. */
    @Override
    public boolean supportsNetworkConnection(NetworkDomain domain, Direction side) {
        for (MachineModule module : modules.values()) {
            if (module instanceof PhysicalNetworkNode node
                    && node.domain() == domain
                    && node.connectionSides().contains(side)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final Optional<ElectricalDiagnosticSource.ElectricalReading> electricalReading(Direction side) {
        for (MachineModule module : modules.values()) {
            if (module instanceof ElectricalDiagnosticSource source) {
                Optional<ElectricalDiagnosticSource.ElectricalReading> reading = source.electricalReading(side);
                if (reading.isPresent()) {
                    return reading;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public final Optional<ElectricalDiagnosticSource.NetworkSummary> electricalNetworkSummary(
            Direction side,
            int maxVisitedNodes
    ) {
        for (MachineModule module : modules.values()) {
            if (module instanceof ElectricalDiagnosticSource source) {
                Optional<ElectricalDiagnosticSource.NetworkSummary> summary =
                        source.electricalNetworkSummary(side, maxVisitedNodes);
                if (summary.isPresent()) {
                    return summary;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public final Optional<ElectricalDiagnosticSource.FaultSearchResult> nearestElectricalFault(
            Direction side,
            int maxVisitedNodes
    ) {
        for (MachineModule module : modules.values()) {
            if (module instanceof ElectricalDiagnosticSource source) {
                Optional<ElectricalDiagnosticSource.FaultSearchResult> fault =
                        source.nearestElectricalFault(side, maxVisitedNodes);
                if (fault.isPresent()) {
                    return fault;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public final Optional<ThermalDiagnosticSource.ThermalReading> thermalReading(Direction side) {
        for (MachineModule module : modules.values()) {
            if (module instanceof ThermalDiagnosticSource source) {
                Optional<ThermalDiagnosticSource.ThermalReading> reading = source.thermalReading(side);
                if (reading.isPresent()) {
                    return reading;
                }
            }
        }
        return Optional.empty();
    }

    /** Flushes module and renderer snapshot requests as one complete BE packet. */
    protected final void finishServerTick() {
        Level currentLevel = getLevel();
        if (currentLevel != null && !currentLevel.isClientSide
                && currentLevel.getGameTime() % CLIENT_STATE_SYNC_INTERVAL == 0L) {
            List<ElectricalNetworkModule> terminals = electricalTerminals();
            if (hasRendererClientStateHash || !terminals.isEmpty()) {
                int combinedHash = hasRendererClientStateHash ? rendererClientStateHash : 1;
                for (ElectricalNetworkModule terminal : terminals) {
                    combinedHash = 31 * combinedHash + terminal.clientStateHash();
                }
                if (!hasSentClientStateHash || lastSentClientStateHash != combinedHash) {
                    lastSentClientStateHash = combinedHash;
                    hasSentClientStateHash = true;
                    requestClientSync();
                }
            }
        }
        if (clientSyncRequested) {
            clientSyncRequested = false;
            markChangedAndSync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeSchemaVersion(tag, persistenceSchemaVersion());
        tag.put(MODULES_TAG, modules.save());
        saveMachineData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (!hasSchema(tag, persistenceSchemaVersion())) {
            modules.resetPersistentState();
            resetMachineData();
            return;
        }
        modules.load(tag.getCompound(MODULES_TAG));
        loadMachineData(tag);
    }

    protected void saveMachineData(CompoundTag tag) {
    }

    protected void loadMachineData(CompoundTag tag) {
    }

    /** Restores local state without reading an incompatible persistence payload. */
    protected void resetMachineData() {
        loadMachineData(new CompoundTag());
    }

    /** Schema owned by the concrete block entity's root persistence payload. */
    protected int persistenceSchemaVersion() {
        return INITIAL_SCHEMA_VERSION;
    }

    static void writeSchemaVersion(CompoundTag tag, int schemaVersion) {
        tag.putInt(SCHEMA_VERSION_TAG, schemaVersion);
    }

    static boolean hasSchema(CompoundTag tag, int schemaVersion) {
        return tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                && tag.getInt(SCHEMA_VERSION_TAG) == schemaVersion;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag moduleRoot = new CompoundTag();
        modules.values().forEach(module -> {
            CompoundTag moduleTag = new CompoundTag();
            module.saveClientData(moduleTag);
            if (!moduleTag.isEmpty()) {
                moduleRoot.put(module.id().toString(), moduleTag);
            }
        });
        if (!moduleRoot.isEmpty()) {
            tag.put(MODULES_TAG, moduleRoot);
        }
        saveClientData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        CompoundTag moduleRoot = tag.getCompound(MODULES_TAG);
        modules.values().forEach(module -> {
            String key = module.id().toString();
            if (moduleRoot.contains(key, CompoundTag.TAG_COMPOUND)) {
                module.loadClientData(moduleRoot.getCompound(key));
            }
        });
        loadClientData(tag);
    }

    @Override
    public void onDataPacket(
            net.minecraft.network.Connection connection,
            ClientboundBlockEntityDataPacket packet
    ) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    protected void saveClientData(CompoundTag tag) {
    }

    protected void loadClientData(CompoundTag tag) {
    }

    /**
     * Sends changing visual state at a bounded cadence. Call after the machine
     * logic tick with a hash containing only fields consumed by world renderers.
     */
    protected final void syncClientState(int stateHash) {
        Level currentLevel = getLevel();
        if (currentLevel == null || currentLevel.isClientSide) {
            return;
        }
        rendererClientStateHash = stateHash;
        hasRendererClientStateHash = true;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        modules.values().forEach(MachineModule::onLoad);
    }

    @Override
    public void setRemoved() {
        modules.values().forEach(MachineModule::onUnload);
        super.setRemoved();
    }

    @Override
    public void invalidateCaps() {
        modules.values().forEach(MachineModule::invalidateCapabilities);
        super.invalidateCaps();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        modules.values().forEach(MachineModule::reviveCapabilities);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        for (MachineModule module : modules.values()) {
            LazyOptional<T> result = module.getCapability(capability, side);
            if (result.isPresent()) {
                return result;
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public final void markChanged() {
        setChanged();
    }

    @Override
    public final void markChangedAndSync() {
        setChanged();
        Level currentLevel = getLevel();
        if (currentLevel != null
                && !currentLevel.isClientSide
                && currentLevel.hasChunk(worldPosition.getX() >> 4, worldPosition.getZ() >> 4)) {
            currentLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public final void requestClientSync() {
        setChanged();
        clientSyncRequested = true;
    }

    @Override
    public final void requestModelRefresh() {
        Level currentLevel = getLevel();
        if (currentLevel == null || !currentLevel.isClientSide) {
            return;
        }
        MachineModelRefreshQueue.enqueue(currentLevel.dimension().location(), worldPosition);
    }

    @Nullable
    @Override
    public final Level level() {
        return getLevel();
    }

    @Override
    public final BlockPos position() {
        return getBlockPos();
    }
}
