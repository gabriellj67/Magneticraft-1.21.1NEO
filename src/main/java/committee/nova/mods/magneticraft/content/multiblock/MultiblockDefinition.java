package committee.nova.mods.magneticraft.content.multiblock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The complete immutable Nova 1.12 multiblock catalogue.
 *
 * <p>Layers are listed bottom-to-top. Rows inside a layer run controller-front
 * to back and characters run left-to-right in the local north-facing frame.</p>
 */
public enum MultiblockDefinition {
    BIG_COMBUSTION_CHAMBER(
            "industrial_combustion_chamber", "Industrial Combustion Chamber", "工业燃烧室",
            new StructureOffset(3, 2, 4), new StructureOffset(1, 0, 0),
            layers(
                    layer("bMb", "bbb", "bbb", ".R."),
                    layer("bbb", "bbb", "bbb", ".R.")
            )
    ),
    BIG_ELECTRIC_FURNACE(
            "industrial_electric_furnace", "Industrial Electric Furnace", "工业电炉",
            new StructureOffset(3, 2, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("YMY", "GGG", "YGY"),
                    layer("YGY", "GCG", "YGY")
            )
    ),
    BIG_STEAM_BOILER(
            "industrial_steam_boiler", "Industrial Steam Boiler", "工业蒸汽锅炉",
            new StructureOffset(3, 4, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("#M#", "###", "###"),
                    layer("RRR", "RRR", "RRR"),
                    layer("RRR", "RRR", "RRR"),
                    layer("RRR", "RRR", "RRR")
            )
    ),
    CONTAINER(
            "shipping_container", "Shipping Container", "货运集装箱",
            new StructureOffset(3, 3, 7), new StructureOffset(1, 0, 0),
            layers(
                    layer("RMR", "RRR", "RRR", "RRR", "RRR", "RRR", "RRR"),
                    layer("RRR", "RAR", "RAR", "RAR", "RAR", "RAR", "RRR"),
                    layer("RRR", "RRR", "RRR", "RRR", "RRR", "RRR", "RRR")
            )
    ),
    GRINDER(
            "grinder", "Grinder", "研磨机",
            new StructureOffset(3, 4, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("#M#", "###", "###"),
                    layer("RGR", "GCG", "RGR"),
                    layer("RGR", "G#G", "RGR"),
                    layer("SSS", "S#S", "SSS")
            )
    ),
    MECHANICAL_GRINDING_MILL(
            "mechanical_grinding_mill", "Mechanical Grinding Mill", "机械研磨机",
            new StructureOffset(3, 3, 3), new StructureOffset(1, 2, 0),
            layers(
                    layer("www", "wKw", "www"),
                    layer("AsA", "sAs", "AsA"),
                    layer(".M.", "...", "...")
            )
    ),
    HYDRAULIC_PRESS(
            "hydraulic_press", "Hydraulic Press", "液压机",
            new StructureOffset(3, 5, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("GMG", "GGG", "GGG"),
                    layer("GGG", "CRC", "GGG"),
                    layer("...", "YSY", "..."),
                    layer("...", "YRY", "..."),
                    layer("...", "XXX", "...")
            )
    ),
    OIL_HEATER(
            "oil_heater", "Oil Heater", "原油加热器",
            new StructureOffset(3, 3, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("YMY", "ZZZ", "ZZZ"),
                    layer("YYY", "RRR", "RRR"),
                    layer("YYY", "RRR", "RRR")
            )
    ),
    POLYMERIZER(
            "polymerizer", "Polymerizer", "聚合器",
            new StructureOffset(3, 5, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("#M#", "###", "###"),
                    layer("###", "#A#", "###"),
                    layer("###", "#A#", "###"),
                    layer("###", "#A#", "#C#"),
                    layer("###", "#T#", "###")
            )
    ),
    PUMPJACK(
            "pumpjack", "Pumpjack", "抽油机",
            new StructureOffset(3, 5, 6), new StructureOffset(1, 0, 0),
            layers(
                    layer("#MC", "###", "###", "###", "###", "###"),
                    layer("...", "GRG", "GGG", "GYG", ".G.", ".G."),
                    layer("...", "GRG", "GGG", "GYG", ".G.", ".R."),
                    layer("...", "GZG", "GZG", "GZG", ".Z.", ".R."),
                    layer("...", "GGG", "GGG", "GGG", ".G.", ".R.")
            )
    ),
    REFINERY(
            "refinery", "Refinery", "炼油塔",
            new StructureOffset(3, 9, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("YMY", "ZZZ", "ZZZ"),
                    layer("YYY", "RRR", "RRR"),
                    layer("YYY", "RRR", "RRR"),
                    layer("YYY", "RRR", "RRR"),
                    layer("YYY", "RRR", "RRR"),
                    layer("YYY", "RRR", "RRR"),
                    layer("YYY", "RRR", "RRR"),
                    layer("YYY", "RRR", "RRR"),
                    layer("YYY", "RRR", "RRR")
            )
    ),
    SHELVING_UNIT(
            "shelving_unit", "Shelving Unit", "货架单元",
            new StructureOffset(5, 3, 2), new StructureOffset(2, 0, 0),
            layers(
                    layer("GGMGG", "GGGGG"),
                    layer("GGGGG", "GGGGG"),
                    layer("GGGGG", "GGGGG")
            )
    ),
    SIEVE(
            "sieve", "Sieve", "筛分机",
            new StructureOffset(3, 2, 5), new StructureOffset(1, 0, 0),
            layers(
                    layer("#M#", "YGY", "YGY", "YGY", "#G#"),
                    layer("###", "SRS", "SRS", "SRS", "SRS")
            )
    ),
    SOLAR_MIRROR(
            "solar_mirror", "Solar Mirror", "太阳能反射镜",
            new StructureOffset(3, 3, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("AMA", "###", "A#A"),
                    layer("AAA", "ARA", "AAA"),
                    layer("AAA", "ARA", "AAA")
            )
    ),
    SOLAR_PANEL(
            "solar_panel", "Solar Panel", "太阳能板",
            new StructureOffset(3, 1, 3), new StructureOffset(1, 0, 0),
            layers(layer("EME", "EEE", "EEE"))
    ),
    SOLAR_TOWER(
            "solar_tower", "Solar Tower", "太阳能塔",
            new StructureOffset(3, 3, 3), new StructureOffset(1, 0, 0),
            layers(
                    layer("#M#", "###", "###"),
                    layer("#R#", "RCR", "#R#"),
                    layer("#R#", "RCR", "#R#")
            )
    ),
    STIRLING_GENERATOR(
            "stirling_generator", "Stirling Generator", "斯特林发电机",
            new StructureOffset(2, 3, 3), new StructureOffset(1, 0, 1),
            layers(
                    layer("##", "#M", "##"),
                    layer("##", "#C", "##"),
                    layer("##", "#E", "##")
            )
    ),
    STEAM_ENGINE(
            "steam_engine", "Steam Engine", "蒸汽机",
            new StructureOffset(3, 4, 4), new StructureOffset(0, 1, 0),
            layers(
                    layer("...", "...", ".G.", ".G."),
                    layer("M..", "b..", "bGC", "bGY"),
                    layer("R..", "Y..", "GG.", ".G."),
                    layer("#..", "#..", "#..", "...")
            )
    ),
    STEAM_TURBINE(
            "steam_turbine", "Steam Turbine", "蒸汽轮机",
            new StructureOffset(3, 3, 5), new StructureOffset(1, 0, 0),
            layers(
                    layer("#M#", "GGG", "GGG", "GGG", "#G#"),
                    layer("G#G", "TRT", "TRT", "#R#", "GGG"),
                    layer("#G#", "GCG", "GGG", "GGG", "#G#")
            )
    );

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final StructureOffset size;
    private final StructureOffset center;
    private final List<List<String>> layers;
    private final List<MultiblockCell> cells;

    MultiblockDefinition(
            String id,
            String englishName,
            String chineseName,
            StructureOffset size,
            StructureOffset center,
            String[][] layers
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.size = size;
        this.center = center;
        if (layers.length != size.y()) {
            throw new IllegalArgumentException(id + " layer count does not match size " + size.y());
        }

        List<List<String>> layerCopy = new ArrayList<>(layers.length);
        List<MultiblockCell> parsedCells = new ArrayList<>();
        int controllers = 0;
        for (int y = 0; y < layers.length; y++) {
            if (layers[y].length != size.z()) {
                throw new IllegalArgumentException(id + " row count does not match size at y=" + y);
            }
            List<String> rows = new ArrayList<>(layers[y].length);
            for (int z = 0; z < layers[y].length; z++) {
                String row = layers[y][z];
                if (row.length() != size.x()) {
                    throw new IllegalArgumentException(id + " width does not match size at y=" + y + ", z=" + z);
                }
                rows.add(row);
                for (int x = 0; x < row.length(); x++) {
                    MultiblockRule rule = MultiblockRule.bySymbol(row.charAt(x));
                    parsedCells.add(new MultiblockCell(new StructureOffset(x, y, z), rule));
                    if (rule == MultiblockRule.CONTROLLER) {
                        controllers++;
                        if (x != center.x() || y != center.y() || z != center.z()) {
                            throw new IllegalArgumentException(id + " controller does not match center " + center);
                        }
                    }
                }
            }
            layerCopy.add(Collections.unmodifiableList(rows));
        }
        if (controllers != 1) {
            throw new IllegalArgumentException(id + " must contain exactly one controller");
        }
        this.layers = Collections.unmodifiableList(layerCopy);
        this.cells = List.copyOf(parsedCells);
    }

    public String id() {
        return id;
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public StructureOffset size() {
        return size;
    }

    public StructureOffset center() {
        return center;
    }

    public List<List<String>> layers() {
        return layers;
    }

    public List<MultiblockCell> cells() {
        return cells;
    }

    public List<MultiblockCell> requiredCells() {
        return cells.stream().filter(cell -> cell.rule() != MultiblockRule.IGNORE).toList();
    }

    public List<MultiblockCell> memberCells() {
        return cells.stream()
                .filter(cell -> cell.rule() != MultiblockRule.IGNORE && cell.rule() != MultiblockRule.AIR)
                .toList();
    }

    public int inventorySlots() {
        return switch (this) {
            case SHELVING_UNIT -> 648;
            case GRINDER, MECHANICAL_GRINDING_MILL -> 3;
            case SIEVE -> 4;
            case HYDRAULIC_PRESS, BIG_ELECTRIC_FURNACE, POLYMERIZER -> 2;
            case BIG_COMBUSTION_CHAMBER, STIRLING_GENERATOR -> 1;
            default -> 0;
        };
    }

    public int bulkItemCapacity() {
        return this == CONTAINER ? 65_536 : 0;
    }

    public int tankCount() {
        return switch (this) {
            case STEAM_ENGINE, PUMPJACK, BIG_COMBUSTION_CHAMBER, POLYMERIZER -> 1;
            case STEAM_TURBINE, OIL_HEATER, BIG_STEAM_BOILER -> 2;
            case REFINERY -> 5;
            default -> 0;
        };
    }

    public int tankCapacity(int index) {
        if (index < 0 || index >= tankCount()) {
            throw new IndexOutOfBoundsException("Tank " + index + " for " + id);
        }
        return switch (this) {
            case STEAM_ENGINE -> 16_000;
            case STEAM_TURBINE -> 32_000;
            case PUMPJACK -> 64_000;
            case BIG_COMBUSTION_CHAMBER -> 4_000;
            case POLYMERIZER -> 4_000;
            case OIL_HEATER -> 16_000;
            case BIG_STEAM_BOILER -> index == 0 ? 16_000 : 128_000;
            case REFINERY -> index == 1 ? 64_000 : 16_000;
            default -> throw new IllegalStateException("No tanks for " + id);
        };
    }

    public boolean usesElectricity() {
        return switch (this) {
            case SOLAR_PANEL, STIRLING_GENERATOR, STEAM_ENGINE, STEAM_TURBINE, GRINDER, SIEVE,
                    HYDRAULIC_PRESS, PUMPJACK, BIG_ELECTRIC_FURNACE -> true;
            default -> false;
        };
    }

    public boolean usesHeat() {
        return switch (this) {
            case SOLAR_TOWER, STIRLING_GENERATOR, OIL_HEATER, POLYMERIZER,
                    BIG_COMBUSTION_CHAMBER, BIG_STEAM_BOILER -> true;
            default -> false;
        };
    }

    public boolean usesKinetics() {
        return this == MECHANICAL_GRINDING_MILL;
    }

    private static String[] layer(String... rows) {
        return Arrays.copyOf(rows, rows.length);
    }

    private static String[][] layers(String[]... layers) {
        return Arrays.copyOf(layers, layers.length);
    }
}
