package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockDefinitionTest {
    private static final List<String> FROZEN_IDS = List.of(
            "industrial_combustion_chamber",
            "industrial_electric_furnace",
            "industrial_steam_boiler",
            "shipping_container",
            "grinder",
            "hydraulic_press",
            "oil_heater",
            "polymerizer",
            "pumpjack",
            "refinery",
            "shelving_unit",
            "sieve",
            "solar_mirror",
            "solar_panel",
            "solar_tower",
            "stirling_generator",
            "steam_engine",
            "steam_turbine"
    );
    private static final Map<String, String> FROZEN_SHAPES = Map.ofEntries(
            Map.entry("industrial_combustion_chamber", "3,2,4@1,0,0|bMb/bbb/bbb/.R.;bbb/bbb/bbb/.R."),
            Map.entry("industrial_electric_furnace", "3,2,3@1,0,0|YMY/GGG/YGY;YGY/GCG/YGY"),
            Map.entry("industrial_steam_boiler", "3,4,3@1,0,0|#M#/###/###;RRR/RRR/RRR;RRR/RRR/RRR;RRR/RRR/RRR"),
            Map.entry("shipping_container", "3,3,7@1,0,0|RMR/RRR/RRR/RRR/RRR/RRR/RRR;RRR/RAR/RAR/RAR/RAR/RAR/RRR;RRR/RRR/RRR/RRR/RRR/RRR/RRR"),
            Map.entry("grinder", "3,4,3@1,0,0|#M#/###/###;RGR/GCG/RGR;RGR/G#G/RGR;SSS/S#S/SSS"),
            Map.entry("hydraulic_press", "3,5,3@1,0,0|GMG/GGG/GGG;GGG/CRC/GGG;.../YSY/...;.../YRY/...;.../XXX/..."),
            Map.entry("oil_heater", "3,3,3@1,0,0|YMY/ZZZ/ZZZ;YYY/RRR/RRR;YYY/RRR/RRR"),
            Map.entry("polymerizer", "3,5,3@1,0,0|#M#/###/###;###/#A#/###;###/#A#/###;###/#A#/#C#;###/#T#/###"),
            Map.entry("pumpjack", "3,5,6@1,0,0|#MC/###/###/###/###/###;.../GRG/GGG/GYG/.G./.G.;.../GRG/GGG/GYG/.G./.R.;.../GZG/GZG/GZG/.Z./.R.;.../GGG/GGG/GGG/.G./.R."),
            Map.entry("refinery", "3,9,3@1,0,0|YMY/ZZZ/ZZZ;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR;YYY/RRR/RRR"),
            Map.entry("shelving_unit", "5,3,2@2,0,0|GGMGG/GGGGG;GGGGG/GGGGG;GGGGG/GGGGG"),
            Map.entry("sieve", "3,2,5@1,0,0|#M#/YGY/YGY/YGY/#G#;###/SRS/SRS/SRS/SRS"),
            Map.entry("solar_mirror", "3,3,3@1,0,0|AMA/###/A#A;AAA/ARA/AAA;AAA/ARA/AAA"),
            Map.entry("solar_panel", "3,1,3@1,0,0|EME/EEE/EEE"),
            Map.entry("solar_tower", "3,3,3@1,0,0|#M#/###/###;#R#/RCR/#R#;#R#/RCR/#R#"),
            Map.entry("stirling_generator", "2,3,3@1,0,1|##/#M/##;##/#C/##;##/#E/##"),
            Map.entry("steam_engine", "3,4,4@0,1,0|.../.../.G./.G.;M../b../bGC/bGY;R../Y../GG./.G.;#../#../#../..."),
            Map.entry("steam_turbine", "3,3,5@1,0,0|#M#/GGG/GGG/GGG/#G#;G#G/TRT/TRT/#R#/GGG;#G#/GCG/GGG/GGG/#G#")
    );
    private static final Map<String, CellCounts> FROZEN_CELL_COUNTS = Map.ofEntries(
            Map.entry("industrial_combustion_chamber", new CellCounts(20, 20)),
            Map.entry("industrial_electric_furnace", new CellCounts(18, 18)),
            Map.entry("industrial_steam_boiler", new CellCounts(36, 36)),
            Map.entry("shipping_container", new CellCounts(63, 58)),
            Map.entry("grinder", new CellCounts(36, 36)),
            Map.entry("hydraulic_press", new CellCounts(27, 27)),
            Map.entry("oil_heater", new CellCounts(27, 27)),
            Map.entry("polymerizer", new CellCounts(45, 42)),
            Map.entry("pumpjack", new CellCounts(62, 62)),
            Map.entry("refinery", new CellCounts(81, 81)),
            Map.entry("shelving_unit", new CellCounts(30, 30)),
            Map.entry("sieve", new CellCounts(30, 30)),
            Map.entry("solar_mirror", new CellCounts(27, 7)),
            Map.entry("solar_panel", new CellCounts(9, 9)),
            Map.entry("solar_tower", new CellCounts(27, 27)),
            Map.entry("stirling_generator", new CellCounts(18, 18)),
            Map.entry("steam_engine", new CellCounts(18, 18)),
            Map.entry("steam_turbine", new CellCounts(45, 45))
    );
    private static final Map<String, String> FROZEN_LAYER_FINGERPRINTS = Map.ofEntries(
            Map.entry("industrial_combustion_chamber", "e9bf06c88b9c080008ed3295a820377f576d93b98767b80aa3ef84bc241ddd49"),
            Map.entry("industrial_electric_furnace", "e3b01a5868563d149bd2919f0204cb2579795074939175f7a05489d570d3457b"),
            Map.entry("industrial_steam_boiler", "6c960fa7c9c0c3bfa7eecba1501ba996c4c729ccc97844fb3040acc7bd0ba738"),
            Map.entry("shipping_container", "673937a42492af8ebeef39370d355830228c00f2cb61822323c7053ad19e8975"),
            Map.entry("grinder", "f394ac6c76559edd3cdf0fd6c2fcb0b971a511f1cb53c6fca5ba7b8127dab2ce"),
            Map.entry("hydraulic_press", "3cb7ecaac403ba389b9762daa1079cfc9ed01119e388f269d237fd6c4904b75e"),
            Map.entry("oil_heater", "ae98254c905ade803b77bf3cca3b718891d9dcb9cc1d5a24eb9d4d753e5a5a4f"),
            Map.entry("polymerizer", "7c7a5cd8c5e068ed66dc50487d00507dc4cace31208aaa60917e9d553692a2c6"),
            Map.entry("pumpjack", "d794540dd69d146bd0db96e9eb1a029e60fe6ba9bec6493939d2a1bfbfb7ce22"),
            Map.entry("refinery", "72c25eae5efa57fa0a392254b41406e62435d00b50ada6fa93c6beb0eeb07409"),
            Map.entry("shelving_unit", "0d9c8aee52c20f585d3b53c898a32c9b712eaa2fa530d2e842d33c94fed91dcf"),
            Map.entry("sieve", "0ab80b88d6b90d0a8e83d37a3e4ac193731114d50e856b29aaefeb77be82a9b0"),
            Map.entry("solar_mirror", "a9987da89d62d924d23ffd875517b292c14216bba54dd3ec1f89308fae194cf9"),
            Map.entry("solar_panel", "36c254d3722cc5f3844ab6dbd88e848b62a65217755ad94efcc5350ae480d2ba"),
            Map.entry("solar_tower", "d539623ffcfb97c0b560d843c35a5cf9130676afb74330207b9f56012dbcf72e"),
            Map.entry("stirling_generator", "2d36660a441e3a9267f7f7548b8f68f4a7d6f0dab1eec1534d2648c489c89aaa"),
            Map.entry("steam_engine", "0c7ef1c1c8e0b4e17efb4df8990a180aaac6c9818891001423ed3d216fd433d1"),
            Map.entry("steam_turbine", "0c1f07230ad48ffa92c4279bef0136483a9b78634d6ee403644565d95f2b83a6")
    );
    /** Reader compatibility for already formed 0.2-0.5 mirrored structures. */
    private static final Map<MultiblockDefinition, String> FROZEN_MIRROR_FINGERPRINTS = Map.of(
            MultiblockDefinition.PUMPJACK,
            "ab112cb93dc2b39ccc1b93b66e1b2e306a053e7430ce97c5ec9873be248719da",
            MultiblockDefinition.STEAM_ENGINE,
            "bf1ef86e8609849f59eb065ede1e922d456c3aad294d007382ec6ec7234c7ad4"
    );

    @Test
    void catalogueContainsEveryLegacyStructureExactlyOnce() {
        List<String> actual = Arrays.stream(MultiblockDefinition.values())
                .map(MultiblockDefinition::id)
                .toList();

        assertEquals(FROZEN_IDS, actual);
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
        assertEquals(FROZEN_IDS.size(), FROZEN_SHAPES.size());
        assertEquals(FROZEN_IDS.size(), FROZEN_CELL_COUNTS.size());
        assertEquals(FROZEN_IDS.size(), FROZEN_LAYER_FINGERPRINTS.size());
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            String layers = definition.layers().stream()
                    .map(rows -> String.join("/", rows))
                    .collect(Collectors.joining(";"));
            String snapshot = definition.size().x() + "," + definition.size().y() + "," + definition.size().z()
                    + "@" + definition.center().x() + "," + definition.center().y() + "," + definition.center().z()
                    + "|" + layers;
            assertEquals(FROZEN_SHAPES.get(definition.id()), snapshot, definition.id());
            CellCounts counts = FROZEN_CELL_COUNTS.get(definition.id());
            assertEquals(counts.required(), definition.requiredCells().size(), definition.id());
            assertEquals(counts.members(), definition.memberCells().size(), definition.id());
            assertEquals(FROZEN_LAYER_FINGERPRINTS.get(definition.id()), sha256(layers), definition.id());
        }
    }

    @Test
    void allHorizontalTransformsAreBijectionsAroundTheController() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                for (boolean mirrored : List.of(false, true)) {
                    Set<StructureOffset> transformed = new HashSet<>();
                    for (MultiblockCell cell : definition.cells()) {
                        StructureOffset relative = MultiblockTransform.relative(
                                cell.offset(), definition.center(), facing, mirrored
                        );
                        transformed.add(relative);
                        assertEquals(
                                cell.offset(),
                                inverseRelative(relative, definition.center(), facing, mirrored),
                                definition.id() + " " + facing + " mirrored=" + mirrored + " " + cell.offset()
                        );
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
    void representativeAsymmetricMirrorsKeepTheirLegacyCompatibilityProjection() {
        for (Map.Entry<MultiblockDefinition, String> entry : FROZEN_MIRROR_FINGERPRINTS.entrySet()) {
            MultiblockDefinition definition = entry.getKey();
            String projection = definition.cells().stream()
                    .map(cell -> {
                        StructureOffset relative = MultiblockTransform.relative(
                                cell.offset(), definition.center(), Direction.EAST, true
                        );
                        return relative.x() + "," + relative.y() + "," + relative.z()
                                + "=" + cell.rule().symbol();
                    })
                    .sorted()
                    .collect(Collectors.joining(";"));
            assertEquals(entry.getValue(), sha256(projection), definition.id());
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

    private static StructureOffset inverseRelative(
            StructureOffset relative,
            StructureOffset center,
            Direction facing,
            boolean mirrored
    ) {
        StructureOffset unrotated = switch (facing) {
            case NORTH -> relative;
            case EAST -> new StructureOffset(relative.z(), relative.y(), -relative.x());
            case SOUTH -> new StructureOffset(-relative.x(), relative.y(), -relative.z());
            case WEST -> new StructureOffset(-relative.z(), relative.y(), relative.x());
            default -> throw new IllegalArgumentException("Multiblock facing must be horizontal: " + facing);
        };
        int localX = mirrored ? -unrotated.x() : unrotated.x();
        return new StructureOffset(
                center.x() + localX,
                center.y() + unrotated.y(),
                center.z() + unrotated.z()
        );
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 is unavailable", exception);
        }
    }

    private record CellCounts(int required, int members) {
    }
}
