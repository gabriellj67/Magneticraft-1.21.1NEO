package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureDescriptor;
import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.Optional;

/** Open-top, water-filled variable pool: 5-11 footprint and 4-8 height. */
public final class SpentFuelPoolStructureValidator implements
        VariableNuclearStructureValidator<SpentFuelPoolSnapshot, SpentFuelPoolPart> {
    public static final VariableNuclearStructureDescriptor DESCRIPTOR = new VariableNuclearStructureDescriptor(
            Magneticraft.id("spent_fuel_pool"),
            new VariableNuclearStructureDescriptor.IntRange(5, 11),
            new VariableNuclearStructureDescriptor.IntRange(5, 11),
            new VariableNuclearStructureDescriptor.IntRange(4, 8));

    @Override public VariableNuclearStructureDescriptor descriptor() { return DESCRIPTOR; }

    @Override
    public ValidationResult<SpentFuelPoolSnapshot> validate(
            BlockPos controller, Direction facing, PartLookup<SpentFuelPoolPart> lookup
    ) {
        for (int width = DESCRIPTOR.width().minimum(); width <= DESCRIPTOR.width().maximum(); width++) {
            for (int length = DESCRIPTOR.length().minimum(); length <= DESCRIPTOR.length().maximum(); length++) {
                for (int height = DESCRIPTOR.height().minimum(); height <= DESCRIPTOR.height().maximum(); height++) {
                    ValidationResult<SpentFuelPoolSnapshot> result = validateExact(
                            controller, facing, width, length, height, lookup);
                    if (result.snapshot().isPresent()) {
                        return result;
                    }
                }
            }
        }
        return failure("no_matching_dimensions", controller);
    }

    public ValidationResult<SpentFuelPoolSnapshot> validateExact(
            BlockPos controller, Direction facing, int width, int length, int height,
            PartLookup<SpentFuelPoolPart> lookup
    ) {
        if (!DESCRIPTOR.accepts(width, length, height)) {
            return failure("dimensions", controller);
        }
        BlockPos port = world(controller, facing, width, length, width / 2, 1, 0);
        BlockPos minimum = controller;
        BlockPos maximum = controller;
        int water = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos position = world(controller, facing, width, length, x, y, z);
                    minimum = new BlockPos(Math.min(minimum.getX(), position.getX()),
                            Math.min(minimum.getY(), position.getY()), Math.min(minimum.getZ(), position.getZ()));
                    maximum = new BlockPos(Math.max(maximum.getX(), position.getX()),
                            Math.max(maximum.getY(), position.getY()), Math.max(maximum.getZ(), position.getZ()));
                    if (!lookup.isLoaded(position)) {
                        return failure("unloaded", position);
                    }
                    SpentFuelPoolPart expected = expected(width, length, height, x, y, z, facing);
                    SpentFuelPoolPart actual = lookup.partAt(position);
                    if (!matches(expected, actual)) {
                        return failure("invalid_" + expected.kind().name().toLowerCase(java.util.Locale.ROOT), position);
                    }
                    if (expected.kind() == SpentFuelPoolPart.Kind.WATER) {
                        water++;
                    }
                }
            }
        }
        return new ValidationResult<>(Optional.of(new SpentFuelPoolSnapshot(
                controller, facing, width, length, height, minimum, maximum, port, water)), "ok", controller);
    }

    public static BlockPos world(
            BlockPos controller, Direction facing, int width, int length, int x, int y, int z
    ) {
        Direction right = facing.getClockWise();
        Direction inward = facing.getOpposite();
        return controller.relative(right, x - width / 2)
                .relative(inward, length - 1 - z)
                .above(y - 1);
    }

    private static SpentFuelPoolPart expected(
            int width, int length, int height, int x, int y, int z, Direction facing
    ) {
        if (x == width / 2 && y == 1 && z == length - 1) {
            return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.CONTROLLER, facing);
        }
        if (x == width / 2 && y == 1 && z == 0) {
            return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.PORT, facing.getOpposite());
        }
        boolean wall = x == 0 || x == width - 1 || z == 0 || z == length - 1;
        if (y == 0 || wall) {
            return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.CASING, null);
        }
        if (y == height - 1) {
            return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.AIR, null);
        }
        return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.WATER, null);
    }

    private static boolean matches(SpentFuelPoolPart expected, SpentFuelPoolPart actual) {
        return actual != null && actual.kind() == expected.kind()
                && (expected.facing() == null || expected.facing() == actual.facing());
    }

    private static ValidationResult<SpentFuelPoolSnapshot> failure(String reason, BlockPos position) {
        return new ValidationResult<>(Optional.empty(), reason, position);
    }
}
