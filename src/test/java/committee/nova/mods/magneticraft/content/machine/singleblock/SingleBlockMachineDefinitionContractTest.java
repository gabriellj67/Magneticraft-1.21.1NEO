package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleBlockMachineDefinitionContractTest {
    @Test
    void everyDefinitionOwnsOneStableStructuralContract() {
        Set<String> ids = new HashSet<>();
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            assertTrue(ids.add(definition.id()), "Duplicate machine id: " + definition.id());
            assertEquals(definition.inventorySlots(), definition.slotRoles().size(), definition.id());
            assertEquals(SingleBlockMachineDefinition.RedstoneControl.IGNORED, definition.redstoneControl());
            assertFalse(definition.guideCategory().isBlank(), definition.id());
        }
    }

    @Test
    void processingAndAutomationProfilesMatchTheReleasedMachineRoles() {
        assertEquals(
                SingleBlockMachineDefinition.ProcessingKind.SLUICE_BOX,
                SingleBlockMachineDefinition.SLUICE_BOX.processingKind()
        );
        assertEquals(
                SingleBlockMachineDefinition.ProcessingKind.CRAFTING,
                SingleBlockMachineDefinition.FABRICATOR.processingKind()
        );
        assertEquals(
                SingleBlockMachineDefinition.AutomationProfile.NONE,
                SingleBlockMachineDefinition.FEEDING_TROUGH.automationProfile()
        );
        assertEquals(
                SingleBlockMachineDefinition.AutomationProfile.BOILER_ALL_SIDES,
                SingleBlockMachineDefinition.STEAM_BOILER.automationProfile()
        );
        assertEquals(
                SingleBlockMachineDefinition.AutomationProfile.ELECTRICITY_WITH_VERTICAL_HEAT,
                SingleBlockMachineDefinition.ELECTRIC_HEATER.automationProfile()
        );
        assertEquals(
                SingleBlockMachineDefinition.AutomationProfile.ALL_SIDES_FORGE_ENERGY_WITH_VERTICAL_HEAT,
                SingleBlockMachineDefinition.RF_HEATER.automationProfile()
        );
        assertEquals(
                SingleBlockMachineDefinition.AutomationProfile.ALL_SIDES,
                SingleBlockMachineDefinition.ELECTRIC_ENGINE.automationProfile()
        );
        assertFalse(SingleBlockMachineDefinition.WATER_GENERATOR.hasMenu());
    }

    @Test
    void physicalPortsKeepEnergyHeatFluidAndLogisticsAsSeparateDomains() {
        assertEquals(
                Set.of(
                        SingleBlockMachineDefinition.PhysicalPort.ELECTRICITY,
                        SingleBlockMachineDefinition.PhysicalPort.HEAT
                ),
                SingleBlockMachineDefinition.ELECTRIC_HEATER.physicalPorts()
        );
        assertEquals(
                Set.of(
                        SingleBlockMachineDefinition.PhysicalPort.FORGE_ENERGY,
                        SingleBlockMachineDefinition.PhysicalPort.HEAT
                ),
                SingleBlockMachineDefinition.RF_HEATER.physicalPorts()
        );
        assertEquals(
                Set.of(SingleBlockMachineDefinition.PhysicalPort.ELECTRICITY),
                SingleBlockMachineDefinition.ELECTRIC_ENGINE.physicalPorts()
        );
        assertEquals(
                Set.of(
                        SingleBlockMachineDefinition.PhysicalPort.GHOST_FILTER,
                        SingleBlockMachineDefinition.PhysicalPort.PNEUMATIC
                ),
                SingleBlockMachineDefinition.TRANSPOSER.physicalPorts()
        );
        assertTrue(Arrays.stream(SingleBlockMachineDefinition.values())
                .filter(definition -> definition == SingleBlockMachineDefinition.SLUICE_BOX
                        || definition == SingleBlockMachineDefinition.FEEDING_TROUGH)
                .allMatch(definition -> definition.physicalPorts().isEmpty()));
    }

    @Test
    void releasedAllAxisPlacementPointsTheModelBackIntoTheClickedBlock() {
        for (SingleBlockMachineDefinition definition : List.of(
                SingleBlockMachineDefinition.RELAY,
                SingleBlockMachineDefinition.FILTER,
                SingleBlockMachineDefinition.TRANSPOSER,
                SingleBlockMachineDefinition.ELECTRIC_ENGINE
        )) {
            assertEquals(Direction.DOWN,
                    SingleBlockMachineBlock.placementFacing(definition, Direction.UP, Direction.EAST),
                    definition.id());
            assertEquals(Direction.WEST,
                    SingleBlockMachineBlock.placementFacing(definition, Direction.EAST, Direction.NORTH),
                    definition.id());
        }
    }

    @Test
    void typedRuntimePortsUseTheSameFacingAsTheReleasedModels() {
        Direction facing = Direction.EAST;
        assertTrue(SingleBlockPortProfile.pneumatic(
                SingleBlockMachineDefinition.RELAY, Direction.EAST, facing));
        assertFalse(SingleBlockPortProfile.pneumatic(
                SingleBlockMachineDefinition.RELAY, Direction.WEST, facing));
        assertTrue(SingleBlockPortProfile.pneumatic(
                SingleBlockMachineDefinition.FILTER, Direction.EAST, facing));
        assertTrue(SingleBlockPortProfile.pneumatic(
                SingleBlockMachineDefinition.FILTER, Direction.WEST, facing));
        assertFalse(SingleBlockPortProfile.pneumatic(
                SingleBlockMachineDefinition.FILTER, Direction.UP, facing));

        assertEquals(0, SingleBlockPortProfile.item(
                SingleBlockMachineDefinition.RELAY, Direction.EAST, facing).insertSlots().length);
        assertEquals(9, SingleBlockPortProfile.item(
                SingleBlockMachineDefinition.RELAY, Direction.WEST, facing).insertSlots().length);
        assertEquals(FluidTankModule.TankAccess.INPUT, SingleBlockPortProfile.fluid(
                SingleBlockMachineDefinition.STEAM_BOILER, 0, Direction.NORTH));
        assertEquals(FluidTankModule.TankAccess.OUTPUT, SingleBlockPortProfile.fluid(
                SingleBlockMachineDefinition.STEAM_BOILER, 1, Direction.NORTH));
        assertTrue(SingleBlockPortProfile.heat(
                SingleBlockMachineDefinition.COMBUSTION_CHAMBER, Direction.UP, facing));
        assertFalse(SingleBlockPortProfile.heat(
                SingleBlockMachineDefinition.COMBUSTION_CHAMBER, Direction.NORTH, facing));
        assertTrue(SingleBlockPortProfile.electricity(
                SingleBlockMachineDefinition.ELECTRIC_ENGINE, Direction.NORTH, facing));
        assertFalse(SingleBlockPortProfile.forgeEnergy(
                SingleBlockMachineDefinition.ELECTRIC_ENGINE, Direction.NORTH, facing));
        assertFalse(SingleBlockPortProfile.forgeEnergy(
                SingleBlockMachineDefinition.ELECTRIC_HEATER, Direction.NORTH, facing));
    }
}
