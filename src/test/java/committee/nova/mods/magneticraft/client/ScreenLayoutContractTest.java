package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout.Rect;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceKind;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityMenu;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolMenu;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalMenu;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
                boolean primaryFluid = definition.physicalPorts().contains(
                        SingleBlockMachineDefinition.PhysicalPort.FLUID_INPUT
                ) || definition.physicalPorts().contains(
                        SingleBlockMachineDefinition.PhysicalPort.FLUID_OUTPUT
                );
                boolean secondaryFluid = definition == SingleBlockMachineDefinition.STEAM_BOILER;
                boolean progress = definition.processingKind()
                        != SingleBlockMachineDefinition.ProcessingKind.NONE;
                controls.addAll(LegacyMachineGuiLayout.singleBlockStatusBars(
                                definition.usesElectricity(),
                                definition == SingleBlockMachineDefinition.ELECTRIC_ENGINE,
                                primaryFluid,
                                secondaryFluid,
                                progress
                        ).stream()
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
                    definition.usesKinetics(),
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
            LegacyMachineGuiLayout.Size displayedSize = miningRobot
                    ? LegacyMachineGuiLayout.withElectricalPanel(
                            size, ProgrammableScreen.ROBOT_ELECTRICAL_PANEL_WIDTH
                    )
                    : size;
            assertTrue(displayedSize.width() <= 427 && displayedSize.height() <= 240,
                    () -> (miningRobot ? "mining_robot" : "computer")
                            + " must fit a common 427 x 240 GUI viewport");
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

    @Test
    void electricalPanelsStayInsideExpandedScreensAndOutsideLegacyContent() {
        assertElectricalPanel(
                new LegacyMachineGuiLayout.Size(
                        BatteryScreen.BASE_IMAGE_WIDTH,
                        LegacyMachineGuiLayout.STANDARD_HEIGHT
                ),
                BatteryScreen.ELECTRICAL_PANEL,
                "battery"
        );
        assertElectricalPanel(
                new LegacyMachineGuiLayout.Size(
                        ElectricFurnaceScreen.BASE_IMAGE_WIDTH,
                        LegacyMachineGuiLayout.STANDARD_HEIGHT
                ),
                ElectricFurnaceScreen.ELECTRICAL_PANEL,
                "electric_furnace"
        );
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            if (definition.hasMenu() && definition.usesElectricity()) {
                assertElectricalPanel(
                        LegacyMachineGuiLayout.singleBlockSize(definition),
                        SingleBlockMachineScreen.electricalPanelBounds(definition),
                        definition.id()
                );
            }
        }
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            if (definition.usesElectricity()) {
                assertElectricalPanel(
                        LegacyMachineGuiLayout.multiblockSize(definition),
                        AdvancedMultiblockScreen.electricalPanelBounds(definition),
                        definition.id()
                );
            }
        }
        LegacyMachineGuiLayout.Size robot = LegacyMachineGuiLayout.programmableSize(true);
        assertElectricalPanel(
                robot,
                LegacyMachineGuiLayout.electricalPanel(
                        robot.width(), ProgrammableScreen.ROBOT_ELECTRICAL_PANEL_WIDTH
                ),
                "mining_robot",
                64
        );
        assertElectricalPanel(
                new LegacyMachineGuiLayout.Size(
                        ElectricalDeviceScreen.BASE_IMAGE_WIDTH,
                        LegacyMachineGuiLayout.STANDARD_HEIGHT
                ),
                ElectricalDeviceScreen.ELECTRICAL_PANEL,
                "electrical_device"
        );
    }

    @Test
    void everyModernMagneticraftScreenLayoutIsBoundedAndDisjoint() {
        List<MachineScreenBounds.Layout> layouts = new ArrayList<>();
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            if (!definition.hasMenu()) {
                continue;
            }
            LegacyMachineGuiLayout.Size base = LegacyMachineGuiLayout.singleBlockSize(definition);
            LegacyMachineGuiLayout.Size size = definition.usesElectricity()
                    ? LegacyMachineGuiLayout.withElectricalPanel(base)
                    : base;
            layouts.add(SingleBlockMachineScreen.layout(definition, size.width(), size.height()));
        }
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            LegacyMachineGuiLayout.Size base = LegacyMachineGuiLayout.multiblockSize(definition);
            LegacyMachineGuiLayout.Size size = definition.usesElectricity()
                    ? LegacyMachineGuiLayout.withElectricalPanel(base)
                    : base;
            layouts.add(AdvancedMultiblockScreen.layout(definition, size.width(), size.height()));
        }

        LegacyMachineGuiLayout.Size standardElectrical = LegacyMachineGuiLayout.withElectricalPanel(
                new LegacyMachineGuiLayout.Size(
                        LegacyMachineGuiLayout.STANDARD_WIDTH,
                        LegacyMachineGuiLayout.STANDARD_HEIGHT
                )
        );
        layouts.add(BatteryScreen.layout(standardElectrical.width(), standardElectrical.height()));
        layouts.add(ElectricFurnaceScreen.layout(standardElectrical.width(), standardElectrical.height()));
        for (ElectricalDeviceKind kind : ElectricalDeviceKind.values()) {
            layouts.add(ElectricalDeviceScreen.layout(kind, standardElectrical.width(), standardElectrical.height()));
        }
        layouts.add(PressureTankScreen.layout(
                LegacyMachineGuiLayout.STANDARD_WIDTH,
                LegacyMachineGuiLayout.STANDARD_HEIGHT
        ));
        layouts.add(WindTurbineScreen.layout(
                LegacyMachineGuiLayout.STANDARD_WIDTH,
                LegacyMachineGuiLayout.STANDARD_HEIGHT
        ));

        LegacyMachineGuiLayout.Size computer = LegacyMachineGuiLayout.programmableSize(false);
        layouts.add(ProgrammableScreen.layout(false, computer.width(), computer.height()));
        LegacyMachineGuiLayout.Size robotBase = LegacyMachineGuiLayout.programmableSize(true);
        LegacyMachineGuiLayout.Size robot = LegacyMachineGuiLayout.withElectricalPanel(
                robotBase, ProgrammableScreen.ROBOT_ELECTRICAL_PANEL_WIDTH
        );
        layouts.add(ProgrammableScreen.layout(true, robot.width(), robot.height()));
        layouts.add(GuideScreen.layout(640, 360));
        layouts.add(NuclearFacilityScreen.layout(
                NuclearFacilityMenu.IMAGE_WIDTH, NuclearFacilityMenu.IMAGE_HEIGHT
        ));
        layouts.add(NuclearThermalScreen.layout(
                NuclearThermalMenu.IMAGE_WIDTH, NuclearThermalMenu.IMAGE_HEIGHT
        ));
        layouts.add(NuclearReactorScreen.layout(
                NuclearReactorMenu.IMAGE_WIDTH, NuclearReactorMenu.IMAGE_HEIGHT, 104, 104, false
        ));
        layouts.add(NuclearReactorScreen.layout(
                NuclearReactorMenu.IMAGE_WIDTH, NuclearReactorMenu.IMAGE_HEIGHT, 104, 104, true
        ));
        layouts.add(SpentFuelPoolScreen.layout(
                SpentFuelPoolMenu.IMAGE_WIDTH, SpentFuelPoolMenu.IMAGE_HEIGHT
        ));

        assertTrue(layouts.size() >= 12, "all Magneticraft-owned screen families must publish a layout");
        for (MachineScreenBounds.Layout layout : layouts) {
            assertDoesNotThrow(layout::validate, layout.screenName());
        }
    }

    @Test
    void boundsMonitorReportsOverflowOverlapClearanceAndMissingParents() {
        MachineScreenBounds.Layout layout = MachineScreenBounds.builder("broken", 100, 100)
                .element("outside", new Rect(95, 95, 10, 10))
                .element("first", new Rect(10, 10, 20, 20), "controls", 4)
                .element("overlap", new Rect(20, 20, 20, 20), "controls", 4)
                .element("too_close", new Rect(41, 20, 20, 20), "controls", 4)
                .child("orphan", new Rect(60, 60, 10, 10), "missing", null, 0)
                .build();

        List<String> problems = layout.problems();
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("exceeds screen")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("overlaps")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("clearance")));
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("missing parent")));
    }

    private static boolean hasProgress(MultiblockDefinition definition) {
        return switch (definition) {
            case GRINDER, MECHANICAL_GRINDING_MILL, SIEVE, HYDRAULIC_PRESS, PUMPJACK, REFINERY,
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

    private static void assertElectricalPanel(
            LegacyMachineGuiLayout.Size base,
            Rect panel,
            String name
    ) {
        assertElectricalPanel(base, panel, name, 96);
    }

    private static void assertElectricalPanel(
            LegacyMachineGuiLayout.Size base,
            Rect panel,
            String name,
            int minimumWidth
    ) {
        LegacyMachineGuiLayout.Size expanded = LegacyMachineGuiLayout.withElectricalPanel(base, panel.width());
        assertTrue(expanded.bounds().contains(panel), () -> name + " electrical panel exceeds its screen");
        assertFalse(base.bounds().overlaps(panel), () -> name + " electrical panel overlaps legacy content");
        assertTrue(panel.width() >= minimumWidth && panel.height() >= 120,
                () -> name + " electrical panel cannot fit two terminal summaries");
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
