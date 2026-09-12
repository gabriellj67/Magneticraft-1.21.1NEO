package committee.nova.mods.magneticraft.content.block;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** Stable registration and display contract for every decorative block family. */
public enum DecorativeBlockFamily {
    LIMESTONE(BaseBlockDefinition.LIMESTONE),
    LIMESTONE_BRICKS(BaseBlockDefinition.LIMESTONE_BRICKS),
    COBBLED_LIMESTONE(BaseBlockDefinition.COBBLED_LIMESTONE),
    BURNT_LIMESTONE(BaseBlockDefinition.BURNT_LIMESTONE),
    BURNT_LIMESTONE_BRICKS(BaseBlockDefinition.BURNT_LIMESTONE_BRICKS),
    COBBLED_BURNT_LIMESTONE(BaseBlockDefinition.COBBLED_BURNT_LIMESTONE),
    LIMESTONE_TILES(BaseBlockDefinition.LIMESTONE_TILES),
    INVERTED_LIMESTONE_TILES(BaseBlockDefinition.INVERTED_LIMESTONE_TILES),
    TERRACOTTA_ROOF_TILE(null, "roof_tile", "Terracotta Roof Tile", "陶土屋瓦", true);

    @Nullable
    private final BaseBlockDefinition existingBase;
    private final String baseId;
    private final String englishName;
    private final String chineseName;
    private final boolean weightedTextures;

    DecorativeBlockFamily(BaseBlockDefinition existingBase) {
        this(
                Objects.requireNonNull(existingBase, "existingBase"),
                existingBase.id(),
                existingBase.englishName(),
                existingBase.chineseName(),
                false
        );
    }

    DecorativeBlockFamily(
            @Nullable BaseBlockDefinition existingBase,
            String baseId,
            String englishName,
            String chineseName,
            boolean weightedTextures
    ) {
        this.existingBase = existingBase;
        this.baseId = Objects.requireNonNull(baseId, "baseId");
        this.englishName = Objects.requireNonNull(englishName, "englishName");
        this.chineseName = Objects.requireNonNull(chineseName, "chineseName");
        this.weightedTextures = weightedTextures;
    }

    @Nullable
    public BaseBlockDefinition existingBase() {
        return existingBase;
    }

    public String baseId() {
        return baseId;
    }

    public String stairsId() {
        return baseId + "_stairs";
    }

    public String slabId() {
        return baseId + "_slab";
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public boolean registersBase() {
        return existingBase == null;
    }

    public boolean weightedTextures() {
        return weightedTextures;
    }
}
