package committee.nova.mods.magneticraft.content.nuclear.facility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;
import java.util.Map;

/** Immutable formed-structure state; runtime logic never rescans volume each tick. */
public record NuclearFacilitySnapshot(
        NuclearFacilityType type,
        BlockPos controller,
        Direction facing,
        int width,
        int height,
        int depth,
        BlockPos minimum,
        BlockPos maximum,
        Map<NuclearFacilityPartRole, BlockPos> ports,
        List<BlockPos> members
) {
    public NuclearFacilitySnapshot {
        controller = controller.immutable();
        ports = Map.copyOf(ports);
        members = members.stream().map(BlockPos::immutable).toList();
        minimum = minimum.immutable();
        maximum = maximum.immutable();
    }
}
