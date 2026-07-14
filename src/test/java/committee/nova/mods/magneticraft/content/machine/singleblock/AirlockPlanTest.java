package committee.nova.mods.magneticraft.content.machine.singleblock;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirlockPlanTest {
    @Test
    void legacyScanUsesPlusMinusTenAroundARadiusNineSphere() {
        assertEquals(10, AirlockPlan.SCAN_RANGE);
        assertEquals(81, AirlockPlan.ACTIVE_RADIUS_SQUARED);
        assertEquals(21 * 21 * 21, AirlockPlan.scanOffsets().size());
        assertEquals(3_071, AirlockPlan.activeOffsets().size());
        assertTrue(AirlockPlan.scanOffsets().contains(new AirlockPlan.Offset(10, 0, 0)));
        assertTrue(AirlockPlan.activeOffsets().contains(new AirlockPlan.Offset(9, 0, 0)));
        assertFalse(AirlockPlan.activeOffsets().contains(new AirlockPlan.Offset(9, 1, 0)));
    }

    @Test
    void boundaryWaterBecomesBubbleWhileInteriorWaterBecomesAir() {
        Map<AirlockPlan.Offset, AirlockPlan.Cell> cells = new HashMap<>();
        AirlockPlan.Offset boundary = new AirlockPlan.Offset(9, 0, 0);
        AirlockPlan.Offset outsideWater = new AirlockPlan.Offset(10, 0, 0);
        AirlockPlan.Offset interior = new AirlockPlan.Offset(0, 0, 0);
        cells.put(boundary, AirlockPlan.Cell.WATER);
        cells.put(outsideWater, AirlockPlan.Cell.WATER);
        cells.put(interior, AirlockPlan.Cell.WATER);

        AirlockPlan plan = plan(cells);

        assertEquals(
                new AirlockPlan.Operation(boundary, AirlockPlan.Target.STABLE_BUBBLE, 1),
                operationAt(plan, boundary)
        );
        assertEquals(
                new AirlockPlan.Operation(interior, AirlockPlan.Target.AIR, 2),
                operationAt(plan, interior)
        );
        assertFalse(plan.operations().stream().anyMatch(operation -> operation.offset().equals(outsideWater)));
        assertEquals(3, plan.totalCostJoules());
    }

    @Test
    void stableBoundaryBubbleIsMaintainedForOneJoule() {
        Map<AirlockPlan.Offset, AirlockPlan.Cell> cells = new HashMap<>();
        AirlockPlan.Offset boundary = new AirlockPlan.Offset(-9, 0, 0);
        cells.put(boundary, AirlockPlan.Cell.STABLE_BUBBLE);
        cells.put(new AirlockPlan.Offset(-10, 0, 0), AirlockPlan.Cell.WATER);

        AirlockPlan plan = plan(cells);

        assertEquals(
                new AirlockPlan.Operation(boundary, AirlockPlan.Target.STABLE_BUBBLE, 1),
                operationAt(plan, boundary)
        );
        assertEquals(1, plan.totalCostJoules());
    }

    @Test
    void insufficientBudgetRejectsTheWholeImmutablePlan() {
        Map<AirlockPlan.Offset, AirlockPlan.Cell> cells = new HashMap<>();
        cells.put(new AirlockPlan.Offset(0, 0, 0), AirlockPlan.Cell.WATER);
        cells.put(new AirlockPlan.Offset(1, 0, 0), AirlockPlan.Cell.WATER);
        AirlockPlan plan = plan(cells);

        assertEquals(4, plan.totalCostJoules());
        assertFalse(plan.canAfford(3.999D));
        assertTrue(plan.canAfford(4.0D));
        assertEquals(2, plan.operations().size());
    }

    private static AirlockPlan plan(Map<AirlockPlan.Offset, AirlockPlan.Cell> cells) {
        return AirlockPlan.build((x, y, z) -> cells.getOrDefault(
                new AirlockPlan.Offset(x, y, z),
                AirlockPlan.Cell.AIR
        ));
    }

    private static AirlockPlan.Operation operationAt(AirlockPlan plan, AirlockPlan.Offset offset) {
        return plan.operations().stream()
                .filter(operation -> operation.offset().equals(offset))
                .findFirst()
                .orElseThrow();
    }
}
