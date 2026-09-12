package committee.nova.mods.magneticraft.content.machine.framework;

import committee.nova.mods.magneticraft.content.machine.framework.module.BulkItemStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.multiblock.ShelvingStorageModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.system.network.diagnostic.DiagnosticHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.diagnostic.ThermalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.diagnostic.PressureDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.List;

/**
 * Base block entity that owns module identity, persistence and the module-backed
 * capability views registered for it in {@link MachineCapabilities}.
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

    @Override
    public final Optional<PressureDiagnosticSource.PressureReading> pressureReading(Direction side) {
        for (MachineModule module : modules.values()) {
            if (module instanceof PressureDiagnosticSource source) {
                Optional<PressureDiagnosticSource.PressureReading> reading = source.pressureReading(side);
                if (reading.isPresent()) {
                    return reading;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Item-handler view aggregated across this block entity's modules, for the
     * capability provider registered by {@link MachineCapabilities#register}.
     */
    @Nullable
    public final IItemHandler exposedItemHandler(@Nullable Direction side) {
        for (MachineModule module : modules.values()) {
            IItemHandler view = null;
            if (module instanceof ItemInventoryModule itemModule) {
                view = itemModule.view(side);
            } else if (module instanceof BulkItemStorageModule bulkModule) {
                view = bulkModule.view();
            } else if (module instanceof ShelvingStorageModule shelvingModule) {
                view = shelvingModule.view();
            }
            if (view != null) {
                return view;
            }
        }
        return null;
    }

    /** Fluid-handler view aggregated across this block entity's modules. */
    @Nullable
    public final IFluidHandler exposedFluidHandler(@Nullable Direction side) {
        for (MachineModule module : modules.values()) {
            if (module instanceof FluidTankModule fluidModule) {
                IFluidHandler view = fluidModule.view(side);
                if (view != null) {
                    return view;
                }
            }
        }
        return null;
    }

    /** Energy-storage view aggregated across this block entity's modules. */
    @Nullable
    public final IEnergyStorage exposedEnergyStorage(@Nullable Direction side) {
        for (MachineModule module : modules.values()) {
            IEnergyStorage view = null;
            if (module instanceof EnergyStorageModule energyModule) {
                view = energyModule.view(side);
            } else if (module instanceof ElectricalPowerModule powerModule) {
                view = powerModule.view(side);
            }
            if (view != null) {
                return view;
            }
        }
        return null;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeSchemaVersion(tag, persistenceSchemaVersion());
        tag.put(MODULES_TAG, modules.save(registries));
        saveMachineData(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (!hasSchema(tag, persistenceSchemaVersion())) {
            modules.resetPersistentState(registries);
            resetMachineData(registries);
            return;
        }
        modules.load(tag.getCompound(MODULES_TAG), registries);
        loadMachineData(tag, registries);
    }

    protected void saveMachineData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    protected void loadMachineData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    /** Restores local state without reading an incompatible persistence payload. */
    protected void resetMachineData(HolderLookup.Provider registries) {
        loadMachineData(new CompoundTag(), registries);
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        CompoundTag moduleRoot = new CompoundTag();
        modules.values().forEach(module -> {
            CompoundTag moduleTag = new CompoundTag();
            module.saveClientData(moduleTag, registries);
            if (!moduleTag.isEmpty()) {
                moduleRoot.put(module.id().toString(), moduleTag);
            }
        });
        if (!moduleRoot.isEmpty()) {
            tag.put(MODULES_TAG, moduleRoot);
        }
        saveClientData(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag moduleRoot = tag.getCompound(MODULES_TAG);
        modules.values().forEach(module -> {
            String key = module.id().toString();
            if (moduleRoot.contains(key, CompoundTag.TAG_COMPOUND)) {
                module.loadClientData(moduleRoot.getCompound(key), registries);
            }
        });
        loadClientData(tag, registries);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            handleUpdateTag(tag, registries);
        }
    }

    protected void saveClientData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    protected void loadClientData(CompoundTag tag, HolderLookup.Provider registries) {
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
