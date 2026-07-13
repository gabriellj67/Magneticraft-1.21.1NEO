package committee.nova.mods.magneticraft.content.machine.singleblock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PneumaticEndpointModuleTest {
    @Test
    void payloadBudgetRemainsExplicitlyBounded() {
        assertEquals(64, PneumaticEndpointModule.MAX_PAYLOADS);
    }
}
