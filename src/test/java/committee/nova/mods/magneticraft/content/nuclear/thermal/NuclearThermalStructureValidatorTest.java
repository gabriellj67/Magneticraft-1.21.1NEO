package committee.nova.mods.magneticraft.content.nuclear.thermal;

import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearThermalStructureValidatorTest {
    private static final BlockPos CONTROLLER = new BlockPos(20, 50, 20);

    @Test
    void steamGeneratorValidatesExactRotatedBoundsPortsAndExchangerPlane() {
        NuclearThermalSnapshot north = validateFixture(
                NuclearThermalFacilityType.STEAM_GENERATOR, Direction.NORTH, 5, 5, 5);
        NuclearThermalSnapshot east = validateFixture(
                NuclearThermalFacilityType.STEAM_GENERATOR, Direction.EAST, 5, 5, 5);

        assertEquals(9, north.exchangerBlocks());
        assertEquals(5, north.ports().size());
        assertEquals(CONTROLLER, north.controller());
        assertNotEquals(north.minimum(), east.minimum());
        assertNotEquals(north.ports().get(NuclearThermalPortRole.HOT_COOLANT_INPUT),
                east.ports().get(NuclearThermalPortRole.HOT_COOLANT_INPUT));
    }

    @Test
    void condenserAndCoolingTowerHonorTheirIndependentDimensionContracts() {
        NuclearThermalSnapshot condenser = validateFixture(
                NuclearThermalFacilityType.CONDENSER, Direction.SOUTH, 5, 7, 5);
        NuclearThermalSnapshot tower = validateFixture(
                NuclearThermalFacilityType.COOLING_TOWER, Direction.WEST, 7, 5, 9);

        assertEquals(15, condenser.exchangerBlocks());
        assertTrue(tower.fillBlocks() > 0);
        assertEquals(1, tower.fanBlocks());
        assertEquals(3, tower.ports().size());
        assertTrue(NuclearThermalFacilityType.COOLING_TOWER.descriptor().accepts(13, 13, 21));
        assertFalse(NuclearThermalFacilityType.CONDENSER.descriptor().accepts(5, 12, 5));
    }

    @Test
    void wrongPortFacingAndOutOfRangeDimensionsAreRejectedAtTheBoundary() {
        NuclearThermalFacilityType type = NuclearThermalFacilityType.STEAM_GENERATOR;
        Map<BlockPos, NuclearThermalPart> parts = fixture(type, Direction.NORTH, 5, 5, 5);
        BlockPos hotPort = NuclearThermalStructureValidator.worldPosition(CONTROLLER, Direction.NORTH, 5, 0, 1, 2);
        parts.put(hotPort, new NuclearThermalPart(NuclearThermalPart.Kind.PORT, Direction.EAST));
        var validator = new NuclearThermalStructureValidator(type);
        var wrongFacing = validator.validateDimensions(
                CONTROLLER, Direction.NORTH, 5, 5, 5, lookup(parts));
        assertTrue(wrongFacing.snapshot().isEmpty());
        assertEquals("port_facing", wrongFacing.reason());

        var outside = validator.validateDimensions(CONTROLLER, Direction.NORTH, 4, 5, 5, lookup(parts));
        assertTrue(outside.snapshot().isEmpty());
        assertEquals("dimensions_out_of_range", outside.reason());
    }

    private static NuclearThermalSnapshot validateFixture(
            NuclearThermalFacilityType type, Direction facing, int width, int length, int height
    ) {
        Map<BlockPos, NuclearThermalPart> parts = fixture(type, facing, width, length, height);
        return new NuclearThermalStructureValidator(type)
                .validateDimensions(CONTROLLER, facing, width, length, height, lookup(parts))
                .snapshot().orElseThrow();
    }

    private static Map<BlockPos, NuclearThermalPart> fixture(
            NuclearThermalFacilityType type, Direction facing, int width, int length, int height
    ) {
        Map<BlockPos, NuclearThermalPart> parts = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockPos position = NuclearThermalStructureValidator.worldPosition(
                            CONTROLLER, facing, width, x, y, z);
                    NuclearThermalPortRole port = NuclearThermalStructureValidator.portAt(
                            type, width, length, height, x, y, z);
                    NuclearThermalPart.Kind kind;
                    Direction partFacing = null;
                    if (x == width / 2 && y == 1 && z == 0) {
                        kind = NuclearThermalPart.Kind.CONTROLLER;
                        partFacing = facing;
                    } else if (port != null) {
                        kind = NuclearThermalPart.Kind.PORT;
                        partFacing = outward(facing, width, length, x, z);
                    } else {
                        boolean shell = x == 0 || x == width - 1 || y == 0 || y == height - 1
                                || z == 0 || z == length - 1;
                        if (type == NuclearThermalFacilityType.COOLING_TOWER
                                && y == height - 1 && x == width / 2 && z == length / 2) {
                            kind = NuclearThermalPart.Kind.COOLING_FAN;
                        } else if (shell) {
                            kind = NuclearThermalPart.Kind.CASING;
                        } else if (type == NuclearThermalFacilityType.STEAM_GENERATOR && x == width / 2
                                || type == NuclearThermalFacilityType.CONDENSER && y == height / 2) {
                            kind = NuclearThermalPart.Kind.HEAT_EXCHANGER;
                        } else if (type == NuclearThermalFacilityType.COOLING_TOWER) {
                            kind = NuclearThermalPart.Kind.COOLING_FILL;
                        } else {
                            kind = NuclearThermalPart.Kind.AIR;
                        }
                    }
                    parts.put(position, new NuclearThermalPart(kind, partFacing));
                }
            }
        }
        return parts;
    }

    private static Direction outward(Direction facing, int width, int length, int x, int z) {
        if (z == 0) return facing;
        if (z == length - 1) return facing.getOpposite();
        if (x == 0) return facing.getCounterClockWise();
        if (x == width - 1) return facing.getClockWise();
        throw new IllegalArgumentException();
    }

    private static VariableNuclearStructureValidator.PartLookup<NuclearThermalPart> lookup(
            Map<BlockPos, NuclearThermalPart> parts
    ) {
        return parts::get;
    }
}
