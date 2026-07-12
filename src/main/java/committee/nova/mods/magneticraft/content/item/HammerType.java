package committee.nova.mods.magneticraft.content.item;

/**
 * Legacy hammer durability and bonus-hit values.
 */
public enum HammerType {
    STONE("stone_hammer", "Stone Hammer", "石锤", 130, 2.0F),
    IRON("iron_hammer", "Iron Hammer", "铁锤", 250, 3.5F),
    STEEL("steel_hammer", "Steel Hammer", "钢锤", 750, 5.0F);

    public static final int DURABILITY_COST_PER_HIT = 2;

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final int durability;
    private final float bonusDamage;

    HammerType(String id, String englishName, String chineseName, int durability, float bonusDamage) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.durability = durability;
        this.bonusDamage = bonusDamage;
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

    public int durability() {
        return durability;
    }

    public float bonusDamage() {
        return bonusDamage;
    }
}
