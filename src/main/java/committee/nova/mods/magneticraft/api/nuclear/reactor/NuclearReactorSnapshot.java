package committee.nova.mods.magneticraft.api.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable authoritative structure snapshot, including the horizontal column map. */
public record NuclearReactorSnapshot(
        VariableNuclearStructureDescriptor descriptor,
        BlockPos controller,
        Direction facing,
        int width,
        int length,
        int height,
        BlockPos minimum,
        BlockPos maximum,
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns,
        Map<NuclearReactorPortType, BlockPos> ports,
        List<BlockPos> members
) {
    public NuclearReactorSnapshot {
        Objects.requireNonNull(descriptor);
        controller = controller.immutable();
        if (facing.getAxis().isVertical()) {
            throw new IllegalArgumentException("Reactor facing must be horizontal");
        }
        if (!descriptor.accepts(width, length, height)) {
            throw new IllegalArgumentException("Snapshot dimensions are outside descriptor bounds");
        }
        minimum = minimum.immutable();
        maximum = maximum.immutable();
        columns = Map.copyOf(columns);
        ports = Map.copyOf(ports);
        members = members.stream().map(BlockPos::immutable).toList();
    }

    public int activeWidth() {
        return width - 4;
    }

    public int activeLength() {
        return length - 4;
    }

    public int activeHeight() {
        return height - 4;
    }
}
