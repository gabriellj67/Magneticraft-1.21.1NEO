package committee.nova.mods.magneticraft.content.nuclear.facility;

import committee.nova.mods.magneticraft.content.multiblock.MultiblockTransform;
import committee.nova.mods.magneticraft.content.multiblock.StructureOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Variable-depth validator used only by nuclear facilities. */
public final class NuclearFacilityValidator {
    private NuclearFacilityValidator() {
    }

    public static Result validate(
            NuclearFacilityType type,
            BlockPos controller,
            Direction facing,
            CellLookup lookup
    ) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(controller);
        Objects.requireNonNull(facing);
        Objects.requireNonNull(lookup);
        if (facing.getAxis().isVertical()) {
            return Result.failure("controller_facing_vertical", controller);
        }

        Result last = Result.failure("invalid_depth", controller);
        for (int depth = type.minimumDepth(); depth <= type.maximumDepth(); depth++) {
            last = validateDepth(type, controller, facing, depth, lookup);
            if (last.snapshot().isPresent()) {
                return last;
            }
        }
        return last;
    }

    public static NuclearFacilityPartRole expectedRole(
            NuclearFacilityType type,
            int x,
            int y,
            int z,
            int depth
    ) {
        if (x == type.width() / 2 && y == 1 && z == 0) {
            return NuclearFacilityPartRole.CONTROLLER;
        }
        if (x == 0 && y == 1 && z == 0) {
            return NuclearFacilityPartRole.ITEM_INPUT;
        }
        if (x == type.width() - 1 && y == 1 && z == 0) {
            return NuclearFacilityPartRole.ITEM_OUTPUT;
        }
        if (x == type.width() / 2 && y == 1 && z == depth - 1) {
            return NuclearFacilityPartRole.ELECTRICAL;
        }
        boolean shell = x == 0 || x == type.width() - 1
                || y == 0 || y == type.height() - 1
                || z == 0 || z == depth - 1;
        if (shell) {
            return NuclearFacilityPartRole.CASING;
        }
        if (type == NuclearFacilityType.CENTRIFUGE_CASCADE) {
            return NuclearFacilityPartRole.CENTRIFUGE_STAGE;
        }
        return NuclearFacilityPartRole.PROCESS_CORE;
    }

    public static BlockPos worldPosition(
            NuclearFacilityType type,
            BlockPos controller,
            Direction facing,
            int x,
            int y,
            int z
    ) {
        return MultiblockTransform.worldPosition(
                controller,
                new StructureOffset(x, y, z),
                new StructureOffset(type.width() / 2, 1, 0),
                facing,
                false
        );
    }

    private static Result validateDepth(
            NuclearFacilityType type,
            BlockPos controller,
            Direction facing,
            int depth,
            CellLookup lookup
    ) {
        List<BlockPos> members = new ArrayList<>(type.width() * type.height() * depth);
        Map<NuclearFacilityPartRole, BlockPos> ports = new EnumMap<>(NuclearFacilityPartRole.class);
        BlockPos minimum = controller;
        BlockPos maximum = controller;

        for (int y = 0; y < type.height(); y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < type.width(); x++) {
                    BlockPos position = worldPosition(type, controller, facing, x, y, z);
                    if (!lookup.isLoaded(position)) {
                        return Result.failure("unloaded", position);
                    }
                    NuclearFacilityPartRole expected = expectedRole(type, x, y, z, depth);
                    ObservedCell observed = lookup.cellAt(position);
                    if (observed == null || observed.role() != expected) {
                        return Result.failure("expected_" + expected.name().toLowerCase(), position);
                    }
                    if (expected.isPort()) {
                        Direction expectedPortFacing = expected == NuclearFacilityPartRole.ELECTRICAL
                                ? facing.getOpposite()
                                : facing;
                        if (observed.facing() != expectedPortFacing) {
                            return Result.failure("port_facing", position);
                        }
                        ports.put(expected, position.immutable());
                    }
                    members.add(position.immutable());
                    minimum = new BlockPos(
                            Math.min(minimum.getX(), position.getX()),
                            Math.min(minimum.getY(), position.getY()),
                            Math.min(minimum.getZ(), position.getZ())
                    );
                    maximum = new BlockPos(
                            Math.max(maximum.getX(), position.getX()),
                            Math.max(maximum.getY(), position.getY()),
                            Math.max(maximum.getZ(), position.getZ())
                    );
                }
            }
        }
        return Result.success(new NuclearFacilitySnapshot(
                type, controller, facing, type.width(), type.height(), depth,
                minimum, maximum, ports, members
        ));
    }

    @FunctionalInterface
    public interface CellLookup {
        @Nullable ObservedCell cellAt(BlockPos position);

        default boolean isLoaded(BlockPos position) {
            return true;
        }
    }

    public record ObservedCell(NuclearFacilityPartRole role, @Nullable Direction facing) {
    }

    public record Result(Optional<NuclearFacilitySnapshot> snapshot, String reason, BlockPos position) {
        public Result {
            snapshot = Objects.requireNonNull(snapshot);
            reason = Objects.requireNonNull(reason);
            position = position.immutable();
        }

        public static Result success(NuclearFacilitySnapshot snapshot) {
            return new Result(Optional.of(snapshot), "ok", snapshot.controller());
        }

        public static Result failure(String reason, BlockPos position) {
            return new Result(Optional.empty(), reason, position);
        }
    }
}
