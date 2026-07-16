package committee.nova.mods.magneticraft.content.material;

/** Visible material forms that replace the legacy metadata item containers. */
public enum MaterialForm {
    INGOT("ingot"),
    NUGGET("nugget"),
    LIGHT_PLATE("light_plate"),
    HEAVY_PLATE("heavy_plate"),
    CHUNK("chunk"),
    DUST("dust"),
    ROCKY_CHUNK("rocky_chunk");

    private final String suffix;

    MaterialForm(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }

    public boolean appliesTo(Metal metal) {
        return metal.supports(this);
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
