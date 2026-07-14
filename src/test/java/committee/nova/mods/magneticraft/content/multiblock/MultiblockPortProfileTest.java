package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule.TankAccess.BOTH;
import static committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule.TankAccess.INPUT;
import static committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule.TankAccess.NONE;
import static committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule.TankAccess.OUTPUT;
import static committee.nova.mods.magneticraft.content.multiblock.MultiblockPortProfile.RelativeSide.BACK;
import static committee.nova.mods.magneticraft.content.multiblock.MultiblockPortProfile.RelativeSide.DOWN;
import static committee.nova.mods.magneticraft.content.multiblock.MultiblockPortProfile.RelativeSide.FRONT;
import static committee.nova.mods.magneticraft.content.multiblock.MultiblockPortProfile.RelativeSide.LEFT;
import static committee.nova.mods.magneticraft.content.multiblock.MultiblockPortProfile.RelativeSide.RIGHT;
import static committee.nova.mods.magneticraft.content.multiblock.MultiblockPortProfile.RelativeSide.UP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockPortProfileTest {
    private static final Map<MultiblockDefinition, List<ExpectedPort>> EXPECTED_PORTS = Map.ofEntries(
            Map.entry(MultiblockDefinition.BIG_COMBUSTION_CHAMBER, List.of(port(
                    0, 4_000, "fluid_fuel", List.of("@magneticraft:fluid_fuel"), BOTH,
                    Map.of(FRONT, INPUT, BACK, INPUT, LEFT, INPUT, RIGHT, INPUT, UP, INPUT, DOWN, OUTPUT)
            ))),
            Map.entry(MultiblockDefinition.BIG_STEAM_BOILER, List.of(
                    port(0, 16_000, "water_input", List.of("#minecraft:water"), BOTH,
                            access(INPUT, FRONT, BACK, LEFT, RIGHT, DOWN)),
                    port(1, 128_000, "steam_output", List.of(fluid("steam")), NONE,
                            access(OUTPUT, UP))
            )),
            Map.entry(MultiblockDefinition.OIL_HEATER, List.of(
                    port(0, 16_000, "feed_input", List.of(fluid("crude_oil"), "#minecraft:water"), BOTH,
                            access(INPUT, FRONT)),
                    port(1, 16_000, "product_output", List.of(fluid("heated_crude_oil"), fluid("steam")),
                            NONE, access(OUTPUT, BACK, UP))
            )),
            Map.entry(MultiblockDefinition.PUMPJACK, List.of(port(
                    0, 64_000, "oil_output", List.of(fluid("crude_oil")), BOTH,
                    access(OUTPUT, MultiblockPortProfile.RelativeSide.values())
            ))),
            Map.entry(MultiblockDefinition.REFINERY, List.of(
                    port(0, 16_000, "feed_input", List.of(
                                    fluid("steam"), fluid("heated_crude_oil"), fluid("heavy_oil"),
                                    fluid("light_oil"), fluid("lpg")
                            ), BOTH, access(INPUT, FRONT)),
                    port(1, 64_000, "process_steam_input", List.of(fluid("steam")), NONE,
                            access(INPUT, BACK)),
                    port(2, 16_000, "heavy_product_output", List.of(
                                    "#minecraft:water", fluid("heavy_oil"), fluid("oil_residue"),
                                    fluid("diesel"), fluid("liquid_plastic")
                            ), NONE, access(OUTPUT, LEFT)),
                    port(3, 16_000, "light_product_output", List.of(
                                    fluid("light_oil"), fluid("fuel_oil"), fluid("kerosene"), fluid("naphtha")
                            ), NONE, access(OUTPUT, RIGHT)),
                    port(4, 16_000, "gas_product_output", List.of(
                                    fluid("lpg"), fluid("lubricant"), fluid("gasoline"), fluid("natural_gas")
                            ), NONE, access(OUTPUT, UP))
            )),
            Map.entry(MultiblockDefinition.STEAM_ENGINE, List.of(port(
                    0, 16_000, "steam_input", List.of(fluid("steam")), BOTH,
                    access(INPUT, MultiblockPortProfile.RelativeSide.values())
            ))),
            Map.entry(MultiblockDefinition.STEAM_TURBINE, List.of(port(
                    0, 32_000, "steam_input", List.of(fluid("steam")), BOTH,
                    access(INPUT, MultiblockPortProfile.RelativeSide.values())
            )))
    );

    @Test
    void catalogueLocksEveryTankIndexCapacityRoleFluidAndAccessProfile() {
        Set<MultiblockDefinition> actualTankDefinitions = EnumSet.noneOf(MultiblockDefinition.class);
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            List<ExpectedPort> expected = EXPECTED_PORTS.getOrDefault(definition, List.of());
            List<MultiblockPortProfile.TankPort> actual = MultiblockPortProfile.tanks(definition);

            assertEquals(definition.tankCount(), actual.size(), definition.id());
            assertEquals(expected.size(), actual.size(), definition.id());
            if (!actual.isEmpty()) {
                actualTankDefinitions.add(definition);
            }

            Set<Integer> indices = new HashSet<>();
            for (int index = 0; index < actual.size(); index++) {
                MultiblockPortProfile.TankPort port = actual.get(index);
                ExpectedPort contract = expected.get(index);
                assertTrue(indices.add(port.index()), definition.id() + " duplicate tank " + port.index());
                assertEquals(index, port.index(), definition.id());
                assertEquals(definition.tankCapacity(index), port.capacity(), definition.id());
                assertEquals(contract.capacity(), port.capacity(), definition.id());
                assertEquals(contract.role(), port.role(), definition.id());
                assertEquals(contract.acceptedFluids(), port.acceptedFluids(), definition.id());
                assertEquals(contract.unsidedAccess(), port.unsidedAccess(), definition.id());
                assertEquals(contract.sideAccess(), port.sideAccess(), definition.id());
                assertFalse(port.acceptedFluids().isEmpty(), definition.id());
                assertTrue(port.acceptedFluids().stream().noneMatch(String::isBlank), definition.id());
                assertEquals(port.acceptedFluids().size(), new HashSet<>(port.acceptedFluids()).size(), definition.id());
                assertEquals(port, MultiblockPortProfile.tank(definition, index), definition.id());
            }

            assertThrows(IndexOutOfBoundsException.class,
                    () -> MultiblockPortProfile.tank(definition, definition.tankCount()), definition.id());
        }

        assertEquals(EXPECTED_PORTS.keySet(), actualTankDefinitions);
    }

    @Test
    void everyWorldSideMapsBackToTheFrozenControllerRelativeAccess() {
        for (Map.Entry<MultiblockDefinition, List<ExpectedPort>> definitionEntry : EXPECTED_PORTS.entrySet()) {
            List<MultiblockPortProfile.TankPort> ports = MultiblockPortProfile.tanks(definitionEntry.getKey());
            for (int index = 0; index < ports.size(); index++) {
                MultiblockPortProfile.TankPort port = ports.get(index);
                ExpectedPort contract = definitionEntry.getValue().get(index);
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    assertEquals(contract.unsidedAccess(), port.access(null, facing),
                            definitionEntry.getKey().id() + " tank=" + index + " unsided");
                    for (Direction worldSide : Direction.values()) {
                        MultiblockPortProfile.RelativeSide relativeSide = expectedRelativeSide(worldSide, facing);
                        FluidTankModule.TankAccess expected = contract.sideAccess()
                                .getOrDefault(relativeSide, NONE);
                        assertEquals(relativeSide,
                                MultiblockPortProfile.RelativeSide.fromWorld(worldSide, facing));
                        assertEquals(expected, port.access(worldSide, facing),
                                definitionEntry.getKey().id() + " tank=" + index
                                        + " facing=" + facing + " side=" + worldSide);
                    }
                }
            }
        }
    }

    private static ExpectedPort port(
            int index,
            int capacity,
            String role,
            List<String> acceptedFluids,
            FluidTankModule.TankAccess unsidedAccess,
            Map<MultiblockPortProfile.RelativeSide, FluidTankModule.TankAccess> sideAccess
    ) {
        return new ExpectedPort(index, capacity, role, acceptedFluids, unsidedAccess, sideAccess);
    }

    private static Map<MultiblockPortProfile.RelativeSide, FluidTankModule.TankAccess> access(
            FluidTankModule.TankAccess access,
            MultiblockPortProfile.RelativeSide... sides
    ) {
        EnumMap<MultiblockPortProfile.RelativeSide, FluidTankModule.TankAccess> result =
                new EnumMap<>(MultiblockPortProfile.RelativeSide.class);
        for (MultiblockPortProfile.RelativeSide side : sides) {
            result.put(side, access);
        }
        return Map.copyOf(result);
    }

    private static MultiblockPortProfile.RelativeSide expectedRelativeSide(Direction side, Direction front) {
        if (side == Direction.UP) {
            return UP;
        }
        if (side == Direction.DOWN) {
            return DOWN;
        }
        if (side == front) {
            return FRONT;
        }
        if (side == front.getOpposite()) {
            return BACK;
        }
        return side == front.getCounterClockWise() ? LEFT : RIGHT;
    }

    private static String fluid(String id) {
        return "magneticraft:" + id;
    }

    private record ExpectedPort(
            int index,
            int capacity,
            String role,
            List<String> acceptedFluids,
            FluidTankModule.TankAccess unsidedAccess,
            Map<MultiblockPortProfile.RelativeSide, FluidTankModule.TankAccess> sideAccess
    ) {
        private ExpectedPort {
            acceptedFluids = List.copyOf(acceptedFluids);
            sideAccess = Map.copyOf(sideAccess);
        }
    }
}
