package committee.nova.mods.magneticraft.content.item;

/**
 * Stateless crafting components split from the legacy metadata item.
 */
public enum CraftingComponent {
    SULFUR("sulfur", "Sulfur Crystal", "硫磺"),
    ALTERNATOR("alternator", "Alternator", "交流发电机"),
    MOTOR("motor", "Motor", "马达"),
    FINE_COPPER_WIRE("fine_copper_wire", "Fine Copper Wire", "小型铜线圈"),
    MAGNET("magnet", "Magnet", "磁铁"),
    IRON_MESH("iron_mesh", "Iron Mesh", "铁网"),
    FABRIC_MESH("fabric_mesh", "Fabric Mesh", "线网");

    private final String id;
    private final String englishName;
    private final String chineseName;

    CraftingComponent(String id, String englishName, String chineseName) {
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
