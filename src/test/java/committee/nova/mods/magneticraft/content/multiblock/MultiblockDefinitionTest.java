package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockDefinitionTest {
    private static final List<String> LEGACY_IDS = List.of(
            "big_combustion_chamber",
            "big_electric_furnace",
            "big_steam_boiler",
            "container",
            "grinder",
            "hydraulic_press",
            "oil_heater",
            "pumpjack",
            "refinery",
            "shelving_unit",
            "sieve",
            "solar_mirror",
            "solar_panel",
            "solar_tower",
            "steam_engine",
            "steam_turbine"
    );
    private static final Map<String, String> LEGACY_SHAPES = Map.ofEntries(
            Map.entry("big_combustion_chamber", "3,2,4@1,0,0|bMb/bbb/bbb/.R.;bbb/bbb/bbb/.R."),
            Map.entry("big_electric_furnace", "3,2,3@1,0,0|YMY/GGG/YGY;YGY/GCG/YGY"),
            Map.entry("big_steam_boiler", "3,4,3@1,0,0|#M#/###/###;RRR/RRR/RRR;RRR/RRR/RRR;RRR/RRR/RRR"),
            Map.entry("container", "3,3,7@1,0,0|RMR/RRR/RRR/RRR/RRR/RRR/RRR;RRR/RAR/RAR/RAR/RAR/RAR/RRR;RRR/RRR/RRR/RRR/RRR/RRR/RRR"),
            Map.entry("grinder", "3,4,3@1,0,0|#M#/###/###;RGR/GCG/RGR;RGR/G#G/RGR;SSS/S#S/SSS"),
            Map.entry("hydraulic_press", "3,5,3@1,0,0|GMG/GGG/GGG;GGG/CRC/GGG;.../YSY/...;.../YRY/...;.../XXX/..."),
            Map.entry("oil_heater", "3,3,3@1,0,0|YMY/ZZZ/ZZZ;YYY/RRR/RRR;YYY/RRR/RRR"),
            Map.entry("pumpjack", "3,5,6@1,0,0|#MC/###/###/###/###/###;.../GRG/GGG/GYG/.G./.G.;.../GRG/GGG/GYG/.G./.R.;.../GZG/GZG/GZG/.Z./.R.;.../GGG/GGG/GGG/.G./.R."),
            Map.entry("refinery", "3,9,3@1,0,0|YMY/ZZZ/ZZZ;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR"),
            Map.entry("shelving_unit", "5,3,2@2,0,0|GGMGG/GGGGG;GGGGG/GGGGG;GGGGG/GGGGG"),
            Map.entry("sieve", "3,2,5@1,0,0|#M#/YGY/YGY/YGY/#G#;###/SRS/SRS/SRS/SRS"),
            Map.entry("solar_mirror", "3,3,3@1,0,0|AMA/###/A#A;AAA/ARA/AAA;AAA/ARA/AAA"),
            Map.entry("solar_panel", "3,1,3@1,0,0|EME/EEE/EEE"),
            Map.entry("solar_tower", "3,3,3@1,0,0|#M#/###/###;#R#/RCR/#R#;#R#/RCR/#R#"),
            Map.entry("steam_engine", "3,4,4@0,1,0|.../.../.G./.G.;M../b../bGC/bGY;R../Y../GG./.G.;#../#../#../..."),
            Map.entry("steam_turbine", "3,3,5@1,0,0|#M#/GGG/GGG/GGG/#G#;G#G/TRT/TRT/#R#/GGG;#G#/GCG/GGG/GGG/#G#")
    );

    @Test
    void catalogueContainsEveryLegacyStructureExactlyOnce() {
        List<String> actual = Arrays.stream(MultiblockDefinition.values())
                .map(MultiblockDefinition::id)
                .toList();

        assertEquals(LEGACY_IDS, actual);
        assertEquals(actual.size(), new HashSet<>(actual).size());
    }

    @Test
    void everyShapeIsRectangularAndAnchoredByOneController() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            int expectedCells = definition.size().x() * definition.size().y() * definition.size().z();
            assertEquals(expectedCells, definition.cells().size(), definition.id());
            assertEquals(definition.size().y(), definition.layers().size(), definition.id());

            List<MultiblockCell> controllers = definition.cells().stream()
                    .filter(cell -> cell.rule() == MultiblockRule.CONTROLLER)
                    .toList();
            assertEquals(1, controllers.size(), definition.id());
            assertEquals(definition.center(), controllers.get(0).offset(), definition.id());
            assertFalse(definition.requiredCells().isEmpty(), definition.id());
            assertTrue(definition.memberCells().size() <= definition.requiredCells().size(), definition.id());
        }
    }

    @Test
    void catalogueMatchesTheIndependentlyReviewedLegacySnapshot() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            String layers = definition.layers().stream()
                    .map(rows -> String.join("/", rows))
                    .collect(Collectors.joining(";"));
            String snapshot = definition.size().x() + "," + definition.size().y() + "," + definition.size().z()
                    + "@" + definition.center().x() + "," + definition.center().y() + "," + definition.center().z()
                    + "|" + layers;
            assertEquals(LEGACY_SHAPES.get(definition.id()), snapshot, definition.id());
        }
    }

    @Test
    void allHorizontalTransformsAreBijectionsAroundTheController() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                for (boolean mirrored : List.of(false, true)) {
                    Set<StructureOffset> transformed = new HashSet<>();
                    for (MultiblockCell cell : definition.cells()) {
                        transformed.add(MultiblockTransform.relative(
                                cell.offset(), definition.center(), facing, mirrored
                        ));
                    }
                    assertEquals(definition.cells().size(), transformed.size(),
                            definition.id() + " " + facing + " mirrored=" + mirrored);
                    assertEquals(new StructureOffset(0, 0, 0), MultiblockTransform.relative(
                            definition.center(), definition.center(), facing, mirrored
                    ));
                }
            }
        }
    }

    @Test
    void mirrorIsAppliedBeforeHorizontalRotation() {
        StructureOffset cell = new StructureOffset(2, 3, 4);
        StructureOffset center = new StructureOffset(1, 1, 1);

        assertEquals(new StructureOffset(-3, 2, -1),
                MultiblockTransform.relative(cell, center, Direction.EAST, true));
        assertEquals(new StructureOffset(3, 2, 1),
                MultiblockTransform.relative(cell, center, Direction.WEST, true));
        assertThrows(IllegalArgumentException.class,
                () -> MultiblockTransform.relative(cell, center, Direction.UP, false));
    }

    @Test
    void machineProfilesHaveBoundedPositiveStorage() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            assertTrue(definition.inventorySlots() >= 0, definition.id());
            assertTrue(definition.tankCount() >= 0, definition.id());
            for (int tank = 0; tank < definition.tankCount(); tank++) {
                assertTrue(definition.tankCapacity(tank) > 0, definition.id() + " tank " + tank);
            }
            assertThrows(IndexOutOfBoundsException.class,
                    () -> definition.tankCapacity(definition.tankCount()), definition.id());
        }
    }

    @Test
    void ruleVocabularyRoundTripsEverySymbol() {
        Set<Character> symbols = new HashSet<>();
        for (MultiblockRule rule : MultiblockRule.values()) {
            assertTrue(symbols.add(rule.symbol()));
            assertEquals(rule, MultiblockRule.bySymbol(rule.symbol()));
        }
        assertThrows(IllegalArgumentException.class, () -> MultiblockRule.bySymbol('?'));
    }
}
