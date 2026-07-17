package committee.nova.mods.magneticraft.content.nuclear.material;

/** Stable item IDs for the open nuclear fuel cycle and its supporting materials. */
public enum NuclearMaterial {
    URANIUM_DUST("uranium_dust", "Uranium Dust", "铀粉"),
    URANIUM_CONCENTRATE("uranium_concentrate", "Uranium Concentrate (Yellowcake)", "铀浓缩物（黄饼）"),
    EMPTY_URANIUM_HEXAFLUORIDE_CYLINDER(
            "empty_uranium_hexafluoride_cylinder",
            "Empty Uranium Hexafluoride Cylinder",
            "空六氟化铀容器"
    ),
    URANIUM_HEXAFLUORIDE_CYLINDER(
            "uranium_hexafluoride_cylinder",
            "Uranium Hexafluoride Cylinder",
            "六氟化铀容器"
    ),
    LOW_ENRICHED_URANIUM("low_enriched_uranium", "Low-Enriched Uranium", "低浓铀"),
    DEPLETED_URANIUM("depleted_uranium", "Depleted Uranium", "贫化铀"),
    URANIUM_DIOXIDE_PELLET("uranium_dioxide_pellet", "Uranium Dioxide Pellet", "二氧化铀芯块"),
    ZIRCON_SAND("zircon_sand", "Zircon Sand", "锆英砂"),
    ZIRCONIUM_DUST("zirconium_dust", "Zirconium Dust", "锆粉"),
    ZIRCONIUM_ALLOY_BLEND("zirconium_alloy_blend", "Zirconium Alloy Blend", "锆合金混合料"),
    ZIRCONIUM_ALLOY_INGOT("zirconium_alloy_ingot", "Zirconium Alloy Ingot", "锆合金锭"),
    ZIRCONIUM_ALLOY_CLADDING("zirconium_alloy_cladding", "Zirconium Alloy Cladding", "锆合金包壳"),
    BORAX("borax", "Borax", "硼砂"),
    BORON_DUST("boron_dust", "Boron Dust", "硼粉"),
    BORON_CARBIDE_BLEND("boron_carbide_blend", "Boron Carbide Blend", "碳化硼混合料"),
    BORON_CARBIDE("boron_carbide", "Boron Carbide", "碳化硼");

    private final String id;
    private final String englishName;
    private final String chineseName;

    NuclearMaterial(String id, String englishName, String chineseName) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
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
}
