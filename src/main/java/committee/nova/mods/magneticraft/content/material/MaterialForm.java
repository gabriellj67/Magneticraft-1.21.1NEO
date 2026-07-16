package committee.nova.mods.magneticraft.content.material;

import java.util.function.Predicate;

/**
 * Visible material forms that replace the legacy metadata item containers.
 */
public enum MaterialForm {
    INGOT("ingot", metal -> !metal.isComposite() && !metal.hasVanillaIngot()),
    NUGGET("nugget", metal -> !metal.isComposite() && !metal.hasVanillaNugget()),
    LIGHT_PLATE("light_plate", metal -> metal.isUseful() && !metal.isComposite()),
    HEAVY_PLATE("heavy_plate", metal -> metal.isUseful() && !metal.isComposite()),
    CHUNK("chunk", metal -> metal.isOre() && !metal.isComposite()),
    DUST("dust", metal -> !metal.isComposite()),
    ROCKY_CHUNK("rocky_chunk", Metal::isOre);

    private final String suffix;
    private final Predicate<Metal> predicate;

    MaterialForm(String suffix, Predicate<Metal> predicate) {
        this.suffix = suffix;
        this.predicate = predicate;
    }

    public String suffix() {
        return suffix;
    }

    public boolean appliesTo(Metal metal) {
        return predicate.test(metal);
    }

    public String id(Metal metal) {
        return metal.id() + "_" + suffix;
    }

    public String englishName(Metal metal) {
        return switch (this) {
            case INGOT -> metal.englishName() + " Ingot";
            case NUGGET -> metal.englishName() + " Nugget";
            case LIGHT_PLATE -> "Light " + metal.englishName() + " Plate";
            case HEAVY_PLATE -> "Heavy " + metal.englishName() + " Plate";
            case CHUNK -> metal.englishName() + " Chunk";
            case DUST -> metal.englishName() + " Dust";
            case ROCKY_CHUNK -> metal.englishName() + " Rocky Chunk";
        };
    }

    public String chineseName(Metal metal) {
        return switch (this) {
            case INGOT -> metal.chineseName() + "锭";
            case NUGGET -> metal.chineseName() + "粒";
            case LIGHT_PLATE -> "轻型" + metal.chineseName() + "板";
            case HEAVY_PLATE -> "重型" + metal.chineseName() + "板";
            case CHUNK -> metal.chineseName() + "矿碎块";
            case DUST -> metal.chineseName() + "粉";
            case ROCKY_CHUNK -> "含石" + metal.chineseName() + "矿碎块";
        };
    }
}
