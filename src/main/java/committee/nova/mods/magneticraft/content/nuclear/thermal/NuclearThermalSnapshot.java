package committee.nova.mods.magneticraft.content.nuclear.thermal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable orientation, bounds, port map and capacity contributors for one formed facility. */
public record NuclearThermalSnapshot(
        NuclearThermalFacilityType type,
        BlockPos controller,
        Direction facing,
        int width,
        int length,
        int height,
        BlockPos minimum,
        BlockPos maximum,
        Map<NuclearThermalPortRole, BlockPos> ports,
        List<BlockPos> members,
        int exchangerBlocks,
        int fillBlocks,
        int fanBlocks
) {
    public NuclearThermalSnapshot {
        Objects.requireNonNull(type);
        controller = controller.immutable();
        Objects.requireNonNull(facing);
        if (!type.descriptor().accepts(width, length, height)) {
            throw new IllegalArgumentException("Snapshot dimensions violate " + type.descriptor().id());
        }
        minimum = minimum.immutable();
        maximum = maximum.immutable();
        ports = Map.copyOf(ports);
        members = members.stream().map(BlockPos::immutable).toList();
        if (exchangerBlocks < 0 || fillBlocks < 0 || fanBlocks < 0) {
            throw new IllegalArgumentException("Structure contributors must be non-negative");
        }
    }
}
