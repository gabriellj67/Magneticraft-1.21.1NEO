package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

/**
 * Base block entity that owns module identity, persistence and capability lifecycle.
 */
public abstract class MachineBlockEntity extends BlockEntity implements MachineModuleHost {
    public static final String MODULES_TAG = "modules";
    private static final int CLIENT_STATE_SYNC_INTERVAL = 4;

    private final MachineModuleContainer modules = new MachineModuleContainer();
    private int lastClientStateHash;
    private boolean hasClientStateHash;
    private boolean clientSyncRequested;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state);
    }

    protected final <T extends MachineModule> T addModule(T module) {
        return modules.add(module);
    }

    protected final void tickModules() {
        modules.values().forEach(MachineModule::serverTick);
    }

    /** Flushes module and renderer snapshot requests as one complete BE packet. */
    protected final void finishServerTick() {
        if (clientSyncRequested) {
            clientSyncRequested = false;
            markChangedAndSync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(MODULES_TAG, modules.save());
        saveMachineData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        modules.load(tag.getCompound(MODULES_TAG));
        loadMachineData(tag);
    }

    protected void saveMachineData(CompoundTag tag) {
    }

    protected void loadMachineData(CompoundTag tag) {
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
        if (currentLevel == null || currentLevel.isClientSide
                || currentLevel.getGameTime() % CLIENT_STATE_SYNC_INTERVAL != 0L) {
            return;
        }
        if (!hasClientStateHash || lastClientStateHash != stateHash) {
            lastClientStateHash = stateHash;
            hasClientStateHash = true;
            requestClientSync();
        }
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
