package committee.nova.mods.magneticraft.content.nuclear.thermal;

import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureDescriptor;
import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureValidator;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockTransform;
import committee.nova.mods.magneticraft.content.multiblock.StructureOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Independent variable-volume validator for steam generators, condensers and wet cooling towers. */
public final class NuclearThermalStructureValidator implements
        VariableNuclearStructureValidator<NuclearThermalSnapshot, NuclearThermalPart> {
    private final NuclearThermalFacilityType type;

    public NuclearThermalStructureValidator(NuclearThermalFacilityType type) {
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public VariableNuclearStructureDescriptor descriptor() {
        return type.descriptor();
    }

    @Override
    public ValidationResult<NuclearThermalSnapshot> validate(
            BlockPos controller,
            Direction facing,
            PartLookup<NuclearThermalPart> lookup
    ) {
        if (facing.getAxis().isVertical()) {
            return failure("controller_facing_vertical", controller);
        }
        ValidationResult<NuclearThermalSnapshot> last = failure("no_valid_dimensions", controller);
        for (int height = descriptor().height().minimum(); height <= descriptor().height().maximum(); height++) {
            for (int length = descriptor().length().minimum(); length <= descriptor().length().maximum(); length++) {
                for (int width = descriptor().width().minimum(); width <= descriptor().width().maximum(); width++) {
                    last = validateDimensions(controller, facing, width, length, height, lookup);
                    if (last.snapshot().isPresent()) {
                        return last;
                    }
                }
            }
        }
        return last;
    }

    public ValidationResult<NuclearThermalSnapshot> validateDimensions(
            BlockPos controller,
            Direction facing,
            int width,
            int length,
            int height,
            PartLookup<NuclearThermalPart> lookup
    ) {
        if (!descriptor().accepts(width, length, height)) {
            return failure("dimensions_out_of_range", controller);
        }
        ArrayList<BlockPos> members = new ArrayList<>(width * length * height);
        EnumMap<NuclearThermalPortRole, BlockPos> ports = new EnumMap<>(NuclearThermalPortRole.class);
        BlockPos minimum = controller;
        BlockPos maximum = controller;
        int exchangers = 0;
        int fills = 0;
        int fans = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos position = worldPosition(controller, facing, width, x, y, z);
                    if (!lookup.isLoaded(position)) {
                        return failure("unloaded", position);
                    }
                    NuclearThermalPart part = lookup.partAt(position);
                    NuclearThermalPortRole portRole = portAt(type, width, length, height, x, y, z);
                    String mismatch = validateCell(type, width, length, height, x, y, z, part, portRole);
                    if (mismatch != null) {
                        return failure(mismatch, position);
                    }
                    if (portRole != null) {
                        if (part == null || part.facing() != portFacing(facing, width, length, x, z)) {
                            return failure("port_facing", position);
                        }
                        ports.put(portRole, position.immutable());
                    }
                    if (part != null) {
                        exchangers += part.kind() == NuclearThermalPart.Kind.HEAT_EXCHANGER ? 1 : 0;
                        fills += part.kind() == NuclearThermalPart.Kind.COOLING_FILL ? 1 : 0;
                        fans += part.kind() == NuclearThermalPart.Kind.COOLING_FAN ? 1 : 0;
                    }
                    members.add(position.immutable());
                    minimum = minimum(minimum, position);
                    maximum = maximum(maximum, position);
                }
            }
        }
        if ((type == NuclearThermalFacilityType.STEAM_GENERATOR
                || type == NuclearThermalFacilityType.CONDENSER) && exchangers <= 0) {
            return failure("missing_heat_exchanger", controller);
        }
        if (type == NuclearThermalFacilityType.COOLING_TOWER && (fills <= 0 || fans <= 0)) {
            return failure(fills <= 0 ? "missing_cooling_fill" : "missing_cooling_fan", controller);
        }
        return new ValidationResult<>(java.util.Optional.of(new NuclearThermalSnapshot(
                type, controller, facing, width, length, height, minimum, maximum,
                ports, members, exchangers, fills, fans
        )), "ok", controller);
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

    public static NuclearThermalPortRole portAt(
            NuclearThermalFacilityType type,
            int width,
            int length,
            int height,
            int x,
            int y,
            int z
    ) {
        int center = width / 2;
        int middle = length / 2;
        if (type == NuclearThermalFacilityType.STEAM_GENERATOR) {
            if (x == 0 && y == 1 && z == middle) return NuclearThermalPortRole.HOT_COOLANT_INPUT;
            if (x == width - 1 && y == 1 && z == middle) return NuclearThermalPortRole.COLD_COOLANT_OUTPUT;
            if (x == 0 && y == 2 && z == middle) return NuclearThermalPortRole.WATER_INPUT;
            if (x == width - 1 && y == 2 && z == middle) return NuclearThermalPortRole.STEAM_OUTPUT;
            if (x == center && y == 1 && z == length - 1) return NuclearThermalPortRole.ELECTRICAL;
        } else if (type == NuclearThermalFacilityType.CONDENSER) {
            if (x == 0 && y == 1 && z == middle) return NuclearThermalPortRole.EXHAUST_INPUT;
            if (x == width - 1 && y == 1 && z == middle) return NuclearThermalPortRole.WATER_OUTPUT;
            if (x == center && y == 1 && z == length - 1) return NuclearThermalPortRole.HEAT;
            if (x == Math.max(0, center - 1) && y == 1 && z == 0) return NuclearThermalPortRole.ELECTRICAL;
        } else {
            if (x == Math.max(0, center - 1) && y == 1 && z == 0) return NuclearThermalPortRole.MAKEUP_WATER_INPUT;
            if (x == Math.min(width - 1, center + 1) && y == 1 && z == 0) return NuclearThermalPortRole.ELECTRICAL;
            if (x == center && y == 1 && z == length - 1) return NuclearThermalPortRole.HEAT;
        }
        return null;
    }

    private static String validateCell(
            NuclearThermalFacilityType type,
            int width,
            int length,
            int height,
            int x,
            int y,
            int z,
            NuclearThermalPart part,
            NuclearThermalPortRole portRole
    ) {
        NuclearThermalPart.Kind actual = part == null ? null : part.kind();
        if (x == width / 2 && y == 1 && z == 0) {
            return actual == NuclearThermalPart.Kind.CONTROLLER ? null : "expected_controller";
        }
        if (portRole != null) {
            return actual == NuclearThermalPart.Kind.PORT ? null : "expected_port_" + portRole.name().toLowerCase();
        }
        boolean shell = x == 0 || x == width - 1 || y == 0 || y == height - 1 || z == 0 || z == length - 1;
        if (shell) {
            if (type == NuclearThermalFacilityType.COOLING_TOWER && y == height - 1
                    && actual == NuclearThermalPart.Kind.COOLING_FAN) {
                return null;
            }
            return actual == NuclearThermalPart.Kind.CASING ? null : "expected_casing";
        }
        if (type == NuclearThermalFacilityType.STEAM_GENERATOR && x == width / 2) {
            return actual == NuclearThermalPart.Kind.HEAT_EXCHANGER ? null : "expected_heat_exchanger";
        }
        if (type == NuclearThermalFacilityType.CONDENSER && y == height / 2) {
            return actual == NuclearThermalPart.Kind.HEAT_EXCHANGER ? null : "expected_heat_exchanger";
        }
        if (type == NuclearThermalFacilityType.COOLING_TOWER) {
            return actual == NuclearThermalPart.Kind.AIR || actual == NuclearThermalPart.Kind.COOLING_FILL
                    ? null : "expected_air_or_fill";
        }
        return actual == NuclearThermalPart.Kind.AIR ? null : "expected_air";
    }

    private static Direction portFacing(Direction facing, int width, int length, int x, int z) {
        if (z == 0) return facing;
        if (z == length - 1) return facing.getOpposite();
        if (x == 0) return facing.getCounterClockWise();
        if (x == width - 1) return facing.getClockWise();
        throw new IllegalArgumentException("Port is not on an outer wall");
    }

    private static BlockPos minimum(BlockPos left, BlockPos right) {
        return new BlockPos(Math.min(left.getX(), right.getX()), Math.min(left.getY(), right.getY()),
                Math.min(left.getZ(), right.getZ()));
    }

    private static BlockPos maximum(BlockPos left, BlockPos right) {
        return new BlockPos(Math.max(left.getX(), right.getX()), Math.max(left.getY(), right.getY()),
                Math.max(left.getZ(), right.getZ()));
    }

    private static ValidationResult<NuclearThermalSnapshot> failure(String reason, BlockPos position) {
        return new ValidationResult<>(java.util.Optional.empty(), reason, position);
    }
}
