package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpentFuelPoolStructureValidatorTest {
    private final SpentFuelPoolStructureValidator validator = new SpentFuelPoolStructureValidator();

    @Test
    void exactMinimumPoolValidatesAcrossAllHorizontalRotations() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos controller = new BlockPos(10, 20, 30);
            Map<BlockPos, SpentFuelPoolPart> parts = build(controller, facing, 5, 5, 4);
            var result = validator.validateExact(controller, facing, 5, 5, 4, parts::get);
            assertTrue(result.snapshot().isPresent(), facing + ": " + result.reason());
            assertEquals(18, result.snapshot().orElseThrow().waterBlocks());
            assertEquals(facing.getOpposite(), parts.get(result.snapshot().orElseThrow().port()).facing());
        }
    }

    @Test
    void boundsAndPortFacingAreStrict() {
        BlockPos controller = BlockPos.ZERO;
        Map<BlockPos, SpentFuelPoolPart> parts = build(controller, Direction.NORTH, 5, 5, 4);
        BlockPos port = SpentFuelPoolStructureValidator.world(controller, Direction.NORTH, 5, 5, 2, 1, 0);
        parts.put(port, new SpentFuelPoolPart(SpentFuelPoolPart.Kind.PORT, Direction.NORTH));
        assertTrue(validator.validateExact(controller, Direction.NORTH, 5, 5, 4, parts::get).snapshot().isEmpty());
        assertTrue(validator.validateExact(controller, Direction.NORTH, 4, 5, 4, parts::get).snapshot().isEmpty());
    }

    private static Map<BlockPos, SpentFuelPoolPart> build(
            BlockPos controller, Direction facing, int width, int length, int height
    ) {
        Map<BlockPos, SpentFuelPoolPart> result = new HashMap<>();
        for (int y = 0; y < height; y++) for (int z = 0; z < length; z++) for (int x = 0; x < width; x++) {
            BlockPos position = SpentFuelPoolStructureValidator.world(controller, facing, width, length, x, y, z);
            SpentFuelPoolPart part;
            if (x == width / 2 && y == 1 && z == length - 1) {
                part = new SpentFuelPoolPart(SpentFuelPoolPart.Kind.CONTROLLER, facing);
            } else if (x == width / 2 && y == 1 && z == 0) {
                part = new SpentFuelPoolPart(SpentFuelPoolPart.Kind.PORT, facing.getOpposite());
            } else if (y == 0 || x == 0 || x == width - 1 || z == 0 || z == length - 1) {
                part = new SpentFuelPoolPart(SpentFuelPoolPart.Kind.CASING, null);
            } else if (y == height - 1) {
                part = new SpentFuelPoolPart(SpentFuelPoolPart.Kind.AIR, null);
            } else {
                part = new SpentFuelPoolPart(SpentFuelPoolPart.Kind.WATER, null);
            }
            result.put(position, part);
        }
        return result;
    }
}
