package committee.nova.mods.magneticraft.content.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoltmeterItemTest {
    @Test
    void modeEncodingCyclesAndUnknownValuesFailSafeToPointMode() {
        assertEquals(VoltmeterItem.Mode.POINT, VoltmeterItem.Mode.fromId(""));
        assertEquals(VoltmeterItem.Mode.NETWORK_SUMMARY, VoltmeterItem.Mode.POINT.next());
        assertEquals(VoltmeterItem.Mode.FAULT_LOCATOR, VoltmeterItem.Mode.NETWORK_SUMMARY.next());
        assertEquals(VoltmeterItem.Mode.POINT, VoltmeterItem.Mode.FAULT_LOCATOR.next());
        assertEquals(VoltmeterItem.Mode.POINT, VoltmeterItem.Mode.fromId("future_mode"));
        assertEquals(VoltmeterItem.Mode.NETWORK_SUMMARY,
                VoltmeterItem.Mode.fromId(VoltmeterItem.Mode.NETWORK_SUMMARY.id()));
    }
}
