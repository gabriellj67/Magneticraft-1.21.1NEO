package committee.nova.mods.magneticraft.content.multiblock;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockPortProfileTest {
    private static final Map<MultiblockDefinition, List<ExpectedTank>> EXPECTED_TANKS = Map.ofEntries(
            Map.entry(MultiblockDefinition.BIG_COMBUSTION_CHAMBER, List.of(tank(
                    0, 4_000, "fluid_fuel", List.of("@magneticraft:industrial_combustion_chamber")
            ))),
            Map.entry(MultiblockDefinition.BIG_STEAM_BOILER, List.of(
                    tank(0, 16_000, "water_input", List.of("#minecraft:water")),
                    tank(1, 128_000, "steam_output", List.of(fluid("steam")))
            )),
            Map.entry(MultiblockDefinition.OIL_HEATER, List.of(
                    tank(0, 16_000, "feed_input", List.of(fluid("crude_oil"), "#minecraft:water")),
                    tank(1, 16_000, "product_output", List.of(fluid("heated_crude_oil"), fluid("steam")))
            )),
            Map.entry(MultiblockDefinition.POLYMERIZER, List.of(tank(
                    0, 4_000, "feed_input", List.of(fluid("liquid_plastic"), fluid("natural_gas"))
            ))),
            Map.entry(MultiblockDefinition.PUMPJACK, List.of(tank(
                    0, 64_000, "oil_output", List.of(fluid("crude_oil"))
            ))),
            Map.entry(MultiblockDefinition.REFINERY, List.of(
                    tank(0, 16_000, "feed_input", List.of(
                            fluid("steam"), fluid("heated_crude_oil"), fluid("heavy_oil"),
                            fluid("light_oil"), fluid("lpg")
                    )),
                    tank(1, 64_000, "process_steam_input", List.of(fluid("steam"))),
                    tank(2, 16_000, "heavy_product_output", List.of(
                            "#minecraft:water", fluid("heavy_oil"), fluid("oil_residue"),
                            fluid("diesel"), fluid("liquid_plastic")
                    )),
                    tank(3, 16_000, "light_product_output", List.of(
                            fluid("light_oil"), fluid("fuel_oil"), fluid("kerosene"), fluid("naphtha")
                    )),
                    tank(4, 16_000, "gas_product_output", List.of(
                            fluid("lpg"), fluid("lubricant"), fluid("gasoline"), fluid("natural_gas")
                    ))
            )),
            Map.entry(MultiblockDefinition.STEAM_ENGINE, List.of(tank(
                    0, 16_000, "steam_input", List.of(fluid("steam"))
            ))),
            Map.entry(MultiblockDefinition.STEAM_TURBINE, List.of(tank(
                    0, 32_000, "steam_input", List.of(fluid("steam"))
            )))
    );

    @Test
    void catalogueLocksEveryTankIndexCapacityRoleAndFluidProfile() {
        Set<MultiblockDefinition> actualTankDefinitions = EnumSet.noneOf(MultiblockDefinition.class);
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            List<ExpectedTank> expected = EXPECTED_TANKS.getOrDefault(definition, List.of());
            List<MultiblockPortProfile.TankPort> actual = MultiblockPortProfile.tanks(definition);

            assertEquals(definition.tankCount(), actual.size(), definition.id());
            assertEquals(expected.size(), actual.size(), definition.id());
            if (!actual.isEmpty()) {
                actualTankDefinitions.add(definition);
            }

            Set<Integer> indices = new HashSet<>();
            for (int index = 0; index < actual.size(); index++) {
                MultiblockPortProfile.TankPort port = actual.get(index);
                ExpectedTank contract = expected.get(index);
                assertTrue(indices.add(port.index()), definition.id() + " duplicate tank " + port.index());
                assertEquals(index, port.index(), definition.id());
                assertEquals(definition.tankCapacity(index), port.capacity(), definition.id());
                assertEquals(contract.capacity(), port.capacity(), definition.id());
                assertEquals(contract.role(), port.role(), definition.id());
                assertEquals(contract.acceptedFluids(), port.acceptedFluids(), definition.id());
                assertFalse(port.acceptedFluids().isEmpty(), definition.id());
                assertTrue(port.acceptedFluids().stream().noneMatch(String::isBlank), definition.id());
                assertEquals(port.acceptedFluids().size(), new HashSet<>(port.acceptedFluids()).size(), definition.id());
                assertEquals(port, MultiblockPortProfile.tank(definition, index), definition.id());
            }

            assertThrows(IndexOutOfBoundsException.class,
                    () -> MultiblockPortProfile.tank(definition, definition.tankCount()), definition.id());
        }
        assertEquals(EXPECTED_TANKS.keySet(), actualTankDefinitions);
    }

    @Test
    void everyTankIsReachedOnlyThroughAnExactFluidConnection() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            Set<Integer> connectedTanks = new HashSet<>();
            for (MultiblockPortLayout.Port port : MultiblockPortLayout.ports(definition)) {
                if (port.kind() != MultiblockPortLayout.Kind.FLUID) {
                    continue;
                }
                assertTrue(port.target() >= 0 && port.target() < definition.tankCount(),
                        definition.id() + " invalid target " + port.target());
                connectedTanks.add(port.target());
            }
            Set<Integer> expected = new HashSet<>();
            for (int tank = 0; tank < definition.tankCount(); tank++) {
                expected.add(tank);
            }
            assertEquals(expected, connectedTanks, definition.id());
        }
    }

    private static ExpectedTank tank(int index, int capacity, String role, List<String> acceptedFluids) {
        return new ExpectedTank(index, capacity, role, List.copyOf(acceptedFluids));
    }

    private static String fluid(String id) {
        return "magneticraft:" + id;
    }

    private record ExpectedTank(int index, int capacity, String role, List<String> acceptedFluids) {
    }
}
