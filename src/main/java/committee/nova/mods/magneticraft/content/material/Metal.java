package committee.nova.mods.magneticraft.content.material;

/**
 * Stable material semantics carried over from the Nova 1.12 metal catalogue.
 */
public enum Metal {
    IRON("iron", "Iron", "铁", true, true, false, true, true),
    GOLD("gold", "Gold", "金", true, true, false, true, true),
    COPPER("copper", "Copper", "铜", true, true, false, true, false),
    LEAD("lead", "Lead", "铅", true, true, false, false, false),
    COBALT("cobalt", "Cobalt", "钴", true, false, false, false, false),
    TUNGSTEN("tungsten", "Tungsten", "钨", true, true, false, false, false),
    STEEL("steel", "Steel", "钢", false, true, false, false, false),
    ALUMINIUM("aluminium", "Aluminium", "铝", true, false, false, false, false),
    GALENA("galena", "Galena", "方铅", true, false, true, false, false),
    MITHRIL("mithril", "Mithril", "秘银", true, false, false, false, false),
    NICKEL("nickel", "Nickel", "镍", true, false, false, false, false),
    OSMIUM("osmium", "Osmium", "锇", true, false, false, false, false),
    SILVER("silver", "Silver", "银", true, false, false, false, false),
    TIN("tin", "Tin", "锡", true, false, false, false, false),
    ZINC("zinc", "Zinc", "锌", true, false, false, false, false);

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final boolean ore;
    private final boolean useful;
    private final boolean composite;
    private final boolean vanillaIngot;
    private final boolean vanillaNugget;

    Metal(
            String id,
            String englishName,
            String chineseName,
            boolean ore,
            boolean useful,
            boolean composite,
            boolean vanillaIngot,
            boolean vanillaNugget
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.ore = ore;
        this.useful = useful;
        this.composite = composite;
        this.vanillaIngot = vanillaIngot;
        this.vanillaNugget = vanillaNugget;
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

    public boolean isOre() {
        return ore;
    }

    public boolean isUseful() {
        return useful;
    }

    public boolean isComposite() {
        return composite;
    }

    public boolean hasVanillaIngot() {
        return vanillaIngot;
    }

    public boolean hasVanillaNugget() {
        return vanillaNugget;
    }
}
