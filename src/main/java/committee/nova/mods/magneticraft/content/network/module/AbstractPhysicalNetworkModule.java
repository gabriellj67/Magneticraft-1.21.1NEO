package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import committee.nova.mods.magneticraft.system.network.runtime.RedstoneControlMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Shared persisted side/lifecycle contract for one physical-domain module.
 */
public abstract class AbstractPhysicalNetworkModule implements MachineModule, PhysicalNetworkNode {
    private static final String SIDE_MASK_TAG = "side_mask";
    private static final String REDSTONE_MODE_TAG = "redstone_mode";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final NetworkDomain domain;
    private final EnumSet<Direction> enabledSides = EnumSet.allOf(Direction.class);
    private RedstoneControlMode redstoneMode = RedstoneControlMode.IGNORED;
    private boolean registered;
    private PhysicalNetworkManager runtimeManager;

    protected AbstractPhysicalNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            NetworkDomain domain
    ) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.domain = Objects.requireNonNull(domain);
    }

    @Override
    public final ResourceLocation id() {
        return id;
    }

    @Override
    public final NetworkDomain domain() {
        return domain;
    }

    @Override
    public final BlockPos position() {
        return host.position();
    }

    @Override
    public Set<Direction> connectionSides() {
        EnumSet<Direction> result = EnumSet.noneOf(Direction.class);
        for (Direction direction : enabledSides) {
            if (supportsSide(direction)) {
                result.add(direction);
            }
        }
        return Set.copyOf(result);
    }

    @Override
    public final void load(CompoundTag tag) {
        int mask = tag.contains(SIDE_MASK_TAG) ? tag.getInt(SIDE_MASK_TAG) : 0x3F;
        enabledSides.clear();
        for (Direction direction : Direction.values()) {
            if ((mask & (1 << direction.ordinal())) != 0) {
                enabledSides.add(direction);
            }
        }
        int redstoneOrdinal = tag.getInt(REDSTONE_MODE_TAG);
        RedstoneControlMode[] modes = RedstoneControlMode.values();
        redstoneMode = redstoneOrdinal >= 0 && redstoneOrdinal < modes.length
                ? modes[redstoneOrdinal]
                : RedstoneControlMode.IGNORED;
        loadNetworkData(tag);
    }

    @Override
    public final void save(CompoundTag tag) {
        int mask = 0;
        for (Direction direction : enabledSides) {
            mask |= 1 << direction.ordinal();
        }
        tag.putInt(SIDE_MASK_TAG, mask);
        tag.putInt(REDSTONE_MODE_TAG, redstoneMode.ordinal());
        saveNetworkData(tag);
    }

    @Override
    public void onLoad() {
        Level level = host.level();
        if (!registered && level instanceof ServerLevel serverLevel) {
            runtimeManager = PhysicalNetworkService.manager(serverLevel);
            runtimeManager.register(this);
            registered = true;
        }
    }

    @Override
    public void onUnload() {
        if (registered && runtimeManager != null) {
            runtimeManager.unregister(this);
        }
        registered = false;
        runtimeManager = null;
    }

    @Override
    public void serverTick() {
        if (runtimeManager != null) {
            runtimeManager.tick(runtimeManager.level().getGameTime());
        }
    }

    public final boolean isSideEnabled(Direction side) {
        return enabledSides.contains(side) && supportsSide(side);
    }

    public final void setSideEnabled(Direction side, boolean enabled) {
        boolean changed = enabled ? enabledSides.add(side) : enabledSides.remove(side);
        if (changed) {
            topologyChanged();
        }
    }

    public final void toggleSide(Direction side) {
        setSideEnabled(side, !enabledSides.contains(side));
    }

    public final RedstoneControlMode redstoneMode() {
        return redstoneMode;
    }

    public final void cycleRedstoneMode() {
        redstoneMode = redstoneMode.next();
        host.markChangedAndSync();
    }

    public final boolean automationEnabled() {
        Level level = host.level();
        return level == null || redstoneMode.allows(level.hasNeighborSignal(host.position()));
    }

    protected boolean supportsSide(Direction direction) {
        return true;
    }

    protected void loadNetworkData(CompoundTag tag) {
    }

    protected void saveNetworkData(CompoundTag tag) {
    }

    protected final MachineModuleHost host() {
        return host;
    }

    protected final void markStateChanged() {
        host.markChanged();
    }

    protected final void markStateChangedAndSync() {
        host.markChangedAndSync();
    }

    protected final PhysicalNetworkManager manager() {
        if (runtimeManager != null) {
            return runtimeManager;
        }
        Level level = host.level();
        return level instanceof ServerLevel serverLevel
                ? PhysicalNetworkService.manager(serverLevel)
                : null;
    }

    protected final boolean networkRegistered() {
        return registered;
    }

    protected final void topologyChanged() {
        host.markChangedAndSync();
        PhysicalNetworkManager manager = manager();
        if (manager != null && registered) {
            manager.update(this);
        }
    }
}
