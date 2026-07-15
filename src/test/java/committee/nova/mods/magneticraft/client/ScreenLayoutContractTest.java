package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout.Rect;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenLayoutContractTest {
    @Test
    void everySingleBlockMachineUsesBoundedDisjointLegacyLayout() {
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            if (!definition.hasMenu()) {
                continue;
            }
            LegacyMachineGuiLayout.Size size = LegacyMachineGuiLayout.singleBlockSize(definition);
            Rect screen = size.bounds();
            List<Rect> machineSlots = LegacyMachineGuiLayout.singleBlockSlots(definition).stream()
                    .map(LegacyMachineGuiLayout::slotBounds)
                    .toList();
            List<Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                    LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                    LegacyMachineGuiLayout.singleBlockPlayerTop(definition)
            );
            List<Rect> controls = new ArrayList<>();
            if (definition != SingleBlockMachineDefinition.BOX
                    && definition != SingleBlockMachineDefinition.FABRICATOR
                    && definition != SingleBlockMachineDefinition.INSERTER) {
                controls.addAll(LegacyMachineGuiLayout.singleBlockStatusBars(true, true, true, true).stream()
                        .map(LegacyMachineGuiLayout.StatusBar::bounds)
                        .toList());
            }
            if (definition == SingleBlockMachineDefinition.INSERTER) {
                controls.addAll(LegacyMachineGuiLayout.inserterButtons());
            }
            if (definition != SingleBlockMachineDefinition.BOX) {
                controls.add(new Rect(8, LegacyMachineGuiLayout.CONTENT_TITLE_TOP, size.width() - 16, 9));
            }

            List<Rect> all = new ArrayList<>();
            all.addAll(machineSlots);
            all.addAll(playerSlots);
            all.addAll(controls);
            assertContained(screen, all, definition.id());
            assertDisjoint(all, definition.id());
        }
    }

    @Test
    void everyMultiblockMachineUsesBoundedDisjointLegacyLayout() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            LegacyMachineGuiLayout.Size size = LegacyMachineGuiLayout.multiblockSize(definition);
            Rect screen = size.bounds();
            List<Rect> machineSlots = LegacyMachineGuiLayout.multiblockSlots(definition).stream()
                    .map(LegacyMachineGuiLayout::slotBounds)
                    .toList();
            List<Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                    LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                    LegacyMachineGuiLayout.multiblockPlayerTop(definition)
            );
            List<Rect> controls = new ArrayList<>();
            controls.add(new Rect(8, 6, size.width() - 16, 9));
            controls.add(new Rect(
                    LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                    LegacyMachineGuiLayout.multiblockPlayerTop(definition) - 12,
                    size.width() - 16,
                    9
            ));
            if (definition == MultiblockDefinition.SHELVING_UNIT) {
                controls.add(new Rect(8, 24, size.width() - 16, 9));
            }
            controls.addAll(LegacyMachineGuiLayout.multiblockStatusBars(
                    definition.usesElectricity(),
                    hasProgress(definition),
                    definition.bulkItemCapacity() > 0,
                    definition.tankCount()
            ).stream().map(LegacyMachineGuiLayout.StatusBar::bounds).toList());

            List<Rect> all = new ArrayList<>();
            all.addAll(machineSlots);
            all.addAll(playerSlots);
            all.addAll(controls);
            assertContained(screen, all, definition.id());
            assertDisjoint(all, definition.id());
        }
    }

    @Test
    void programmableLayoutsKeepEditorStateButtonsAndInventoriesSeparated() {
        for (boolean miningRobot : List.of(false, true)) {
            LegacyMachineGuiLayout.Size size = LegacyMachineGuiLayout.programmableSize(miningRobot);
            List<Rect> all = new ArrayList<>();
            all.add(new Rect(8, 6, 230, 9));
            all.addAll(LegacyMachineGuiLayout.programmableEditorRows());
            all.add(new Rect(8, LegacyMachineGuiLayout.PROGRAMMABLE_PAGE_TOP, 230, 9));
            all.addAll(LegacyMachineGuiLayout.programmableButtons());
            all.add(new Rect(246, ProgrammableScreen.STATE_TOP, 96, 2 * ProgrammableScreen.STATE_LINE_HEIGHT));
            if (miningRobot) {
                all.add(new Rect(
                        246,
                        ProgrammableScreen.ROBOT_ENERGY_TOP,
                        96,
                        ProgrammableScreen.ROBOT_ENERGY_HEIGHT
                ));
                all.addAll(LegacyMachineGuiLayout.miningRobotSlots().stream()
                        .map(LegacyMachineGuiLayout::slotBounds)
                        .toList());
            }
            int playerTop = LegacyMachineGuiLayout.programmablePlayerTop(miningRobot);
            all.add(new Rect(LegacyMachineGuiLayout.PROGRAMMABLE_PLAYER_LEFT, playerTop - 12, 160, 9));
            all.addAll(LegacyMachineGuiLayout.playerInventorySlots(
                    LegacyMachineGuiLayout.PROGRAMMABLE_PLAYER_LEFT,
                    playerTop
            ));

            String name = miningRobot ? "mining_robot" : "computer";
            assertContained(size.bounds(), all, name);
            assertDisjoint(all, name);
        }
    }

    @Test
    void dedicatedMachineSlotsMatchLegacyConfiguration() {
        assertEquals(new LegacyMachineGuiLayout.Point(103, 16), LegacyMachineGuiLayout.BATTERY_INPUT);
        assertEquals(new LegacyMachineGuiLayout.Point(103, 48), LegacyMachineGuiLayout.BATTERY_OUTPUT);
        assertEquals(new LegacyMachineGuiLayout.Point(108, 17), LegacyMachineGuiLayout.ELECTRIC_FURNACE_INPUT);
        assertEquals(new LegacyMachineGuiLayout.Point(108, 49), LegacyMachineGuiLayout.ELECTRIC_FURNACE_OUTPUT);
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

    private static boolean hasProgress(MultiblockDefinition definition) {
        return switch (definition) {
            case GRINDER, SIEVE, HYDRAULIC_PRESS, PUMPJACK, REFINERY,
                    BIG_ELECTRIC_FURNACE, BIG_COMBUSTION_CHAMBER,
                    BIG_STEAM_BOILER, OIL_HEATER -> true;
            default -> false;
        };
    }

    private static void assertContained(Rect screen, List<Rect> elements, String name) {
        for (Rect element : elements) {
            assertTrue(screen.contains(element), () -> name + " contains out-of-bounds element " + element);
        }
    }

    private static void assertDisjoint(List<Rect> elements, String name) {
        for (int first = 0; first < elements.size(); first++) {
            for (int second = first + 1; second < elements.size(); second++) {
                Rect left = elements.get(first);
                Rect right = elements.get(second);
                assertFalse(left.overlaps(right), () -> name + " overlaps " + left + " and " + right);
            }
        }
    }
}
