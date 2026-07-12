package committee.nova.mods.magneticraft.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenLayoutContractTest {
    @Test
    void programmableStateRowsDoNotOverlapEnergyOrInventory() {
        assertTrue(
                ProgrammableScreen.ROBOT_ENERGY_TOP + ProgrammableScreen.ROBOT_ENERGY_HEIGHT
                        <= ProgrammableScreen.STATE_TOP
        );
        assertTrue(
                ProgrammableScreen.STATE_TOP + 2 * ProgrammableScreen.STATE_LINE_HEIGHT
                        <= ProgrammableScreen.INVENTORY_PANEL_TOP
        );
    }

    @Test
    void singleBlockTitleAndStatusReceiveDisjointWidths() {
        int statusWidth = 54;
        int titleX = 8;
        int titleWidth = MachineScreenLayout.availableTitleWidth(176, titleX, statusWidth);
        int statusX = 176 - 8 - statusWidth;

        assertEquals(statusX - titleX - 6, titleWidth);
        assertTrue(titleX + titleWidth + 6 <= statusX);
    }
}
