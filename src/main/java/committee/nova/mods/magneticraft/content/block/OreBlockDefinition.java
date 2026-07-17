package committee.nova.mods.magneticraft.content.block;

import committee.nova.mods.magneticraft.content.material.Metal;

import java.util.List;

/** Shared ore-block semantics consumed by tags, processing and world generation. */
public enum OreBlockDefinition {
    GALENA(BaseBlockDefinition.GALENA_ORE, Metal.GALENA, 8, 10, 0, 2, 79, "galena", "lead"),
    COBALT(BaseBlockDefinition.COBALT_ORE, Metal.COBALT, 6, 4, 0, -32, 32, "cobalt"),
    TUNGSTEN(BaseBlockDefinition.TUNGSTEN_ORE, Metal.TUNGSTEN, 8, 8, 0, 20, 59, "tungsten"),
    PYRITE(BaseBlockDefinition.PYRITE_ORE, null, 9, 9, 0, 30, 99, "pyrite", "sulfur"),
    ZINC(BaseBlockDefinition.ZINC_ORE, Metal.ZINC, 7, 6, 0, 0, 64, "zinc"),
    BAUXITE(BaseBlockDefinition.BAUXITE_ORE, Metal.ALUMINIUM, 7, 5, 0, 32, 112,
            "bauxite", "aluminium", "aluminum"),
    SILVER(BaseBlockDefinition.SILVER_ORE, Metal.SILVER, 4, 0, 2, -48, 16, "silver"),
    NICKEL(BaseBlockDefinition.NICKEL_ORE, Metal.NICKEL, 5, 4, 0, -24, 40, "nickel"),
    TIN(BaseBlockDefinition.TIN_ORE, Metal.TIN, 6, 4, 0, -16, 48, "tin"),
    URANIUM(BaseBlockDefinition.URANIUM_ORE, null, 5, 3, 0, -48, 24, "uranium");

    private final BaseBlockDefinition block;
    private final Metal processedMetal;
    private final int veinSize;
    private final int count;
    private final int rarity;
    private final int minY;
    private final int maxY;
    private final List<String> forgeMaterials;

    OreBlockDefinition(
            BaseBlockDefinition block,
            Metal processedMetal,
            int veinSize,
            int count,
            int rarity,
            int minY,
            int maxY,
            String... forgeMaterials
    ) {
        this.block = block;
        this.processedMetal = processedMetal;
        this.veinSize = veinSize;
        this.count = count;
        this.rarity = rarity;
        this.minY = minY;
        this.maxY = maxY;
        this.forgeMaterials = List.of(forgeMaterials);
        if ((count > 0) == (rarity > 0)) {
            throw new IllegalArgumentException("Ore must define exactly one frequency mode: " + block.id());
        }
    }

    public BaseBlockDefinition block() {
        return block;
    }

    public Metal processedMetal() {
        return processedMetal;
    }

    public int veinSize() {
        return veinSize;
    }

    public int count() {
        return count;
    }

    public int rarity() {
        return rarity;
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    public List<String> forgeMaterials() {
        return forgeMaterials;
    }
}
