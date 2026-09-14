package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.world.ProtectedWorldMutation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Durable geothermal drilling and lava-reservoir state. */
final class GeothermalPumpState {
    private static final String OWNER_TAG = "owner";
    private static final String DRILL_POSITION_TAG = "drill_position";
    private static final String SOURCES_TAG = "sources";
    private static final String SOURCE_POSITION_TAG = "position";
    private static final String REMAINING_ENERGY_TAG = "remaining_geothermal_energy";
    private static final int SEARCH_LIMIT = 4_096;
    private static final int VISITS_PER_TICK = 128;
    private static final int SOURCES_PER_TICK = 20;

    @Nullable
    private UUID owner;
    @Nullable
    private BlockPos drillPosition;
    private final LinkedHashSet<BlockPos> sources = new LinkedHashSet<>();
    private double remainingEnergyJoules;

    private final ArrayDeque<BlockPos> searchFrontier = new ArrayDeque<>();
    private final Set<BlockPos> searchVisited = new HashSet<>();
    private boolean searchComplete;

    @Nullable
    UUID owner() {
        return owner;
    }

    void setOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    BlockPos drillPosition(BlockPos controller) {
        if (drillPosition == null) {
            drillPosition = controller.below().immutable();
        }
        return drillPosition;
    }

    void advanceDrill() {
        if (drillPosition != null) {
            drillPosition = drillPosition.below().immutable();
        }
    }

    Set<BlockPos> sources() {
        return sources;
    }

    double remainingEnergyJoules() {
        return remainingEnergyJoules;
    }

    void setRemainingEnergyJoules(double value) {
        remainingEnergyJoules = Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
    }

    void startSearch(BlockPos origin) {
        if (searchComplete || !searchFrontier.isEmpty()) {
            return;
        }
        searchFrontier.add(origin.immutable());
    }

    boolean searchComplete() {
        return searchComplete;
    }

    boolean scanLoadedLava(ServerLevel level) {
        int visits = 0;
        int registered = 0;
        boolean changed = false;
        while (!searchFrontier.isEmpty()
                && searchVisited.size() < SEARCH_LIMIT
                && visits < VISITS_PER_TICK
                && registered < SOURCES_PER_TICK) {
            BlockPos current = searchFrontier.removeFirst();
            if (!searchVisited.add(current)) {
                continue;
            }
            visits++;
            if (!ProtectedWorldMutation.isSafeLoadedTarget(level, current)) {
                continue;
            }
            FluidState fluid = level.getFluidState(current);
            if (!fluid.is(FluidTags.LAVA)) {
                continue;
            }
            if (fluid.isSource() && sources.add(current.immutable())) {
                registered++;
                changed = true;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!searchVisited.contains(neighbor)
                        && ProtectedWorldMutation.isSafeLoadedTarget(level, neighbor)
                        && level.getFluidState(neighbor).is(FluidTags.LAVA)) {
                    searchFrontier.addLast(neighbor.immutable());
                }
            }
        }
        if (searchFrontier.isEmpty() || searchVisited.size() >= SEARCH_LIMIT) {
            searchComplete = true;
        }
        return changed;
    }

    void save(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
        }
        if (drillPosition != null) {
            tag.putLong(DRILL_POSITION_TAG, drillPosition.asLong());
        }
        ListTag savedSources = new ListTag();
        for (BlockPos source : sources) {
            CompoundTag savedSource = new CompoundTag();
            savedSource.putLong(SOURCE_POSITION_TAG, source.asLong());
            savedSources.add(savedSource);
        }
        tag.put(SOURCES_TAG, savedSources);
        tag.putDouble(REMAINING_ENERGY_TAG, remainingEnergyJoules);
    }

    void load(CompoundTag tag) {
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        drillPosition = tag.contains(DRILL_POSITION_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(DRILL_POSITION_TAG))
                : null;
        sources.clear();
        ListTag savedSources = tag.getList(SOURCES_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < savedSources.size(); index++) {
            sources.add(BlockPos.of(savedSources.getCompound(index).getLong(SOURCE_POSITION_TAG)));
        }
        setRemainingEnergyJoules(tag.getDouble(REMAINING_ENERGY_TAG));
        searchFrontier.clear();
        searchVisited.clear();
        searchComplete = false;
    }
}
