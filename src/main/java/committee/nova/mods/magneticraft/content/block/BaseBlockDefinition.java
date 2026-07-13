package committee.nova.mods.magneticraft.content.block;

/**
 * Stable IDs and display metadata for the stateless blocks in the base-content task.
 */
public enum BaseBlockDefinition {
    GALENA_ORE("galena_ore", "Galena Ore", "方铅矿石", Category.ORE, MiningTier.STONE),
    COBALT_ORE("cobalt_ore", "Cobaltite Ore", "钴矿石", Category.ORE, MiningTier.IRON),
    TUNGSTEN_ORE("tungsten_ore", "Wolframite Ore", "黑钨矿石", Category.ORE, MiningTier.IRON),
    PYRITE_ORE("pyrite_ore", "Pyrite Ore", "黄铁矿石", Category.ORE, MiningTier.STONE),
    LEAD_BLOCK("lead_block", "Lead Block", "铅块", Category.STORAGE, MiningTier.NONE),
    COBALT_BLOCK("cobalt_block", "Cobalt Block", "钴块", Category.STORAGE, MiningTier.NONE),
    TUNGSTEN_BLOCK("tungsten_block", "Tungsten Block", "钨块", Category.STORAGE, MiningTier.NONE),
    SULFUR_BLOCK("sulfur_block", "Sulfur Block", "硫磺块", Category.STORAGE, MiningTier.NONE),
    LIMESTONE("limestone", "Limestone", "石灰石", Category.DECORATION, MiningTier.NONE),
    LIMESTONE_BRICKS("limestone_bricks", "Limestone Bricks", "石灰石砖", Category.DECORATION, MiningTier.NONE),
    COBBLED_LIMESTONE("limestone_cobblestone", "Cobbled Limestone", "碎石灰石", Category.DECORATION, MiningTier.NONE),
    BURNT_LIMESTONE("burnt_limestone", "Burnt Limestone", "焦灰石灰石", Category.DECORATION, MiningTier.NONE),
    BURNT_LIMESTONE_BRICKS("burnt_limestone_bricks", "Burnt Limestone Bricks", "焦灰石灰石砖", Category.DECORATION, MiningTier.NONE),
    COBBLED_BURNT_LIMESTONE("burnt_limestone_cobblestone", "Cobbled Burnt Limestone", "焦灰碎石灰石", Category.DECORATION, MiningTier.NONE),
    LIMESTONE_TILES("limestone_tiles", "Limestone Tiles", "方石灰石", Category.DECORATION, MiningTier.NONE),
    INVERTED_LIMESTONE_TILES("inverted_limestone_tiles", "Inverted Limestone Tiles", "反向花纹石灰石砖", Category.DECORATION, MiningTier.NONE);

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final Category category;
    private final MiningTier miningTier;

    BaseBlockDefinition(String id, String englishName, String chineseName, Category category, MiningTier miningTier) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.category = category;
        this.miningTier = miningTier;
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

    public Category category() {
        return category;
    }

    public MiningTier miningTier() {
        return miningTier;
    }

    public enum Category {
        ORE,
        STORAGE,
        DECORATION
    }

    public enum MiningTier {
        NONE,
        STONE,
        IRON
    }
}
