package committee.nova.mods.magneticraft.content.item;

/**
 * Legacy hammer durability and bonus-hit values.
 */
public enum HammerType {
    STONE("stone_hammer", "Stone Hammer", "石锤", 130, 2.0F, 1, 8, 1),
    IRON("iron_hammer", "Iron Hammer", "铁锤", 250, 3.5F, 2, 10, 1),
    STEEL("steel_hammer", "Steel Hammer", "钢锤", 750, 5.0F, 4, 15, 1);

    public static final int DURABILITY_COST_PER_HIT = 2;

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final int durability;
    private final float bonusDamage;
    private final int crushingLevel;
    private final int crushingSpeed;
    private final int crushingDurabilityCost;

    HammerType(
            String id,
            String englishName,
            String chineseName,
            int durability,
            float bonusDamage,
            int crushingLevel,
            int crushingSpeed,
            int crushingDurabilityCost
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.durability = durability;
        this.bonusDamage = bonusDamage;
        this.crushingLevel = crushingLevel;
        this.crushingSpeed = crushingSpeed;
        this.crushingDurabilityCost = crushingDurabilityCost;
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

    public int crushingLevel() {
        return crushingLevel;
    }

    public int crushingSpeed() {
        return crushingSpeed;
    }

    public int crushingDurabilityCost() {
        return crushingDurabilityCost;
    }
}
