package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureDescriptor;
import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureValidator;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockTransform;
import committee.nova.mods.magneticraft.content.multiblock.StructureOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Independent two-shell variable-volume PWR validator. */
public final class NuclearReactorStructure {
    public static final VariableNuclearStructureDescriptor DESCRIPTOR = new VariableNuclearStructureDescriptor(
            Magneticraft.id("pressurized_water_reactor"),
            new VariableNuclearStructureDescriptor.IntRange(7, 13),
            new VariableNuclearStructureDescriptor.IntRange(7, 13),
            new VariableNuclearStructureDescriptor.IntRange(7, 15)
    );
    public static final VariableNuclearStructureValidator<NuclearReactorSnapshot, ObservedPart> VALIDATOR =
            new VariableNuclearStructureValidator<>() {
                @Override
                public VariableNuclearStructureDescriptor descriptor() {
                    return DESCRIPTOR;
                }

                @Override
                public ValidationResult<NuclearReactorSnapshot> validate(
                        BlockPos controller,
                        Direction facing,
                        VariableNuclearStructureValidator.PartLookup<ObservedPart> lookup
                ) {
                    Result result = NuclearReactorStructure.validate(
                            controller, facing, new NuclearReactorStructure.PartLookup() {
                        @Override
                        public ObservedPart partAt(BlockPos position) {
                            return lookup.partAt(position);
                        }

                        @Override
                        public boolean isLoaded(BlockPos position) {
                            return lookup.isLoaded(position);
                        }
                    });
                    return new ValidationResult<>(result.snapshot(), result.reason(), result.position());
                }
            };

    private NuclearReactorStructure() {
    }

    public static Result validate(BlockPos controller, Direction facing, PartLookup lookup) {
        Result last = Result.failure("no_matching_dimensions", controller);
        for (int width = DESCRIPTOR.width().minimum(); width <= DESCRIPTOR.width().maximum(); width++) {
            for (int length = DESCRIPTOR.length().minimum(); length <= DESCRIPTOR.length().maximum(); length++) {
                for (int height = DESCRIPTOR.height().minimum(); height <= DESCRIPTOR.height().maximum(); height++) {
                    last = validateExact(controller, facing, width, length, height, lookup);
                    if (last.snapshot().isPresent()) {
                        return last;
                    }
                }
            }
        }
        return last;
    }

    public static Result validateExact(
            BlockPos controller,
            Direction facing,
            int width,
            int length,
            int height,
            PartLookup lookup
    ) {
        Objects.requireNonNull(controller);
        Objects.requireNonNull(facing);
        Objects.requireNonNull(lookup);
        if (facing.getAxis().isVertical() || !DESCRIPTOR.accepts(width, length, height)) {
            return Result.failure("invalid_dimensions", controller);
        }

        Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns = new LinkedHashMap<>();
        int fuels = 0;
        int controls = 0;
        int coolants = 0;
        int instruments = 0;
        for (int z = 2; z <= length - 3; z++) {
            for (int x = 2; x <= width - 3; x++) {
                BlockPos position = worldPosition(controller, facing, width, x, 2, z);
                if (!lookup.isLoaded(position)) {
                    return Result.failure("unloaded", position);
                }
                ObservedPart observed = lookup.partAt(position);
                if (observed == null || observed.kind() != PartKind.COLUMN_BASE || observed.columnType() == null) {
                    return Result.failure("column_base", position);
                }
                NuclearReactorColumnType type = observed.columnType();
                columns.put(new ReactorColumnCoordinate(x - 2, z - 2), type);
                fuels += type.isFuel() ? 1 : 0;
                controls += type.isControlRod() ? 1 : 0;
                coolants += type == NuclearReactorColumnType.COOLANT_CHANNEL ? 1 : 0;
                instruments += type == NuclearReactorColumnType.INSTRUMENTATION ? 1 : 0;
            }
        }
        if (fuels == 0 || controls == 0 || coolants == 0 || instruments == 0) {
            return Result.failure("required_columns", controller);
        }

        Map<NuclearReactorPortType, BlockPos> ports = new EnumMap<>(NuclearReactorPortType.class);
        List<BlockPos> members = new ArrayList<>(width * length * height);
        BlockPos minimum = controller;
        BlockPos maximum = controller;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos position = worldPosition(controller, facing, width, x, y, z);
                    if (!lookup.isLoaded(position)) {
                        return Result.failure("unloaded", position);
                    }
                    ExpectedPart expected = expectedPart(width, length, height, x, y, z, columns, facing);
                    ObservedPart observed = lookup.partAt(position);
                    if (!expected.matches(observed)) {
                        return Result.failure("expected_" + expected.kind().name().toLowerCase(), position);
                    }
                    if (expected.portType() != null && isOuterShell(width, length, height, x, y, z)) {
                        ports.put(expected.portType(), position.immutable());
                    }
                    members.add(position.immutable());
                    minimum = min(minimum, position);
                    maximum = max(maximum, position);
                }
            }
        }
        return Result.success(new NuclearReactorSnapshot(
                DESCRIPTOR, controller, facing, width, length, height, minimum, maximum,
                columns, ports, members
        ));
    }

    public static BlockPos worldPosition(
            BlockPos controller, Direction facing, int width, int x, int y, int z
    ) {
        return MultiblockTransform.worldPosition(
                controller,
                new StructureOffset(x, y, z),
                new StructureOffset(width / 2, 1, 0),
                facing,
                false
        );
    }

    public static ExpectedPart expectedPart(
            int width,
            int length,
            int height,
            int x,
            int y,
            int z,
            Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns,
            Direction facing
    ) {
        NuclearReactorColumnType column = activeColumn(width, length, x, z, columns);
        if (y == 2 && column != null) {
            return new ExpectedPart(PartKind.COLUMN_BASE, column, null, null);
        }
        if (y >= 3 && y <= height - 3 && column != null) {
            return new ExpectedPart(PartKind.COLUMN_SEGMENT, null, null, null);
        }

        boolean actuator = column != null && column.isControlRod() && (y == height - 1 || y == height - 2);
        if (actuator) {
            return new ExpectedPart(PartKind.CONTROL_ROD_ACTUATOR, null, null, null);
        }
        int center = width / 2;
        if (x == center && y == 1 && z == 0) {
            return new ExpectedPart(PartKind.CONTROLLER, null, facing, null);
        }
        if ((x == 1 || x == width - 2) && y == 1 && (z == 0 || z == 1)) {
            NuclearReactorPortType type = x == 1
                    ? NuclearReactorPortType.COOLANT_INPUT
                    : NuclearReactorPortType.COOLANT_OUTPUT;
            return new ExpectedPart(PartKind.COOLANT_PORT, null, facing, type);
        }
        if (x == center && y == 1 && (z == length - 1 || z == length - 2)) {
            return new ExpectedPart(PartKind.ELECTRICAL_PORT, null, facing.getOpposite(),
                    NuclearReactorPortType.ELECTRICAL);
        }
        if (x == center + 1 && y == 1 && (z == length - 1 || z == length - 2)) {
            return new ExpectedPart(PartKind.INSTRUMENTATION_PORT, null, facing.getOpposite(),
                    NuclearReactorPortType.INSTRUMENTATION);
        }
        if (isOuterShell(width, length, height, x, y, z)) {
            return new ExpectedPart(PartKind.CONTAINMENT_CASING, null, null, null);
        }
        if (isInnerShell(width, length, height, x, y, z)) {
            return new ExpectedPart(PartKind.PRESSURE_VESSEL, null, null, null);
        }
        throw new IllegalArgumentException("Unclassified reactor cell " + x + "," + y + "," + z);
    }

    private static boolean isOuterShell(int width, int length, int height, int x, int y, int z) {
        return x == 0 || x == width - 1 || y == 0 || y == height - 1 || z == 0 || z == length - 1;
    }

    private static boolean isInnerShell(int width, int length, int height, int x, int y, int z) {
        return x == 1 || x == width - 2 || y == 1 || y == height - 2 || z == 1 || z == length - 2;
    }

    @Nullable
    private static NuclearReactorColumnType activeColumn(
            int width,
            int length,
            int x,
            int z,
            Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns
    ) {
        return x >= 2 && x <= width - 3 && z >= 2 && z <= length - 3
                ? columns.get(new ReactorColumnCoordinate(x - 2, z - 2))
                : null;
    }

    private static BlockPos min(BlockPos first, BlockPos second) {
        return new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ()));
    }

    private static BlockPos max(BlockPos first, BlockPos second) {
        return new BlockPos(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ()));
    }

    public enum PartKind {
        CONTROLLER,
        CONTAINMENT_CASING,
        PRESSURE_VESSEL,
        CONTROL_ROD_ACTUATOR,
        COOLANT_PORT,
        ELECTRICAL_PORT,
        INSTRUMENTATION_PORT,
        COLUMN_BASE,
        COLUMN_SEGMENT
    }

    public record ObservedPart(
            PartKind kind,
            @Nullable NuclearReactorColumnType columnType,
            @Nullable Direction facing
    ) {
    }

    public record ExpectedPart(
            PartKind kind,
            @Nullable NuclearReactorColumnType columnType,
            @Nullable Direction facing,
            @Nullable NuclearReactorPortType portType
    ) {
        boolean matches(@Nullable ObservedPart observed) {
            return observed != null && observed.kind() == kind
                    && (columnType == null || observed.columnType() == columnType)
                    && (facing == null || observed.facing() == facing);
        }
    }

    @FunctionalInterface
    public interface PartLookup {
        @Nullable ObservedPart partAt(BlockPos position);

        default boolean isLoaded(BlockPos position) {
            return true;
        }
    }

    public record Result(Optional<NuclearReactorSnapshot> snapshot, String reason, BlockPos position) {
        public Result {
            snapshot = Objects.requireNonNull(snapshot);
            reason = Objects.requireNonNull(reason);
            position = position.immutable();
        }

        static Result success(NuclearReactorSnapshot snapshot) {
            return new Result(Optional.of(snapshot), "ok", snapshot.controller());
        }

        static Result failure(String reason, BlockPos position) {
            return new Result(Optional.empty(), reason, position);
        }
    }
}
