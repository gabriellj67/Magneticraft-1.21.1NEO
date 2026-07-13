package committee.nova.mods.magneticraft.content.machine.singleblock;

/**
 * Stable identifiers and player-facing metadata for the legacy single-block inventory.
 */
public enum SingleBlockMachineDefinition {
    BOX("wooden_crate", "Wooden Crate", "木箱", 27, 0, true, FacingMode.NONE, false),
    SLUICE_BOX("sluice_box", "Sluice Box", "淘洗槽", 1, 0, false, FacingMode.HORIZONTAL, true),
    FABRICATOR("fabricator", "Fabricator", "装配台", 9, 9, true, FacingMode.NONE, false),
    SMALL_TANK("small_tank", "Small Tank", "小型储罐", 0, 0, true, FacingMode.NONE, false),
    FEEDING_TROUGH("feeding_trough", "Feeding Trough", "喂食槽", 1, 0, false, FacingMode.HORIZONTAL, true),
    INSERTER("inserter", "Inserter", "机械臂", 3, 9, true, FacingMode.HORIZONTAL, false),
    WATER_GENERATOR("water_generator", "Water Generator", "供水器", 0, 0, true, FacingMode.NONE, false),
    RELAY("pneumatic_relay", "Pneumatic Relay", "气动中继器", 9, 0, true, FacingMode.ALL, false),
    FILTER("pneumatic_filter", "Pneumatic Filter", "气动过滤器", 1, 9, true, FacingMode.ALL, false),
    TRANSPOSER("pneumatic_transposer", "Pneumatic Transposer", "气动转置器", 0, 9, true, FacingMode.ALL, false),
    COMBUSTION_CHAMBER("combustion_chamber", "Combustion Chamber", "燃烧室", 1, 0, true, FacingMode.HORIZONTAL, false),
    STEAM_BOILER("steam_boiler", "Steam Boiler", "蒸汽锅炉", 0, 0, true, FacingMode.NONE, false),
    ELECTRIC_HEATER("electric_heater", "Electric Heater", "电加热器", 0, 0, true, FacingMode.NONE, false),
    RF_HEATER("forge_energy_heater", "Forge Energy Heater", "Forge 能量加热器", 0, 0, true, FacingMode.NONE, false),
    GASIFICATION_UNIT("gasification_unit", "Gasification Unit", "气化装置", 2, 0, true, FacingMode.NONE, false),
    BRICK_FURNACE("brick_furnace", "Brick Furnace", "砖炉", 2, 0, true, FacingMode.HORIZONTAL, false),
    INFINITE_ENERGY("infinite_energy_source", "Infinite Energy Source", "无限能源", 0, 0, false, FacingMode.NONE, false),
    AIRLOCK("airlock", "Airlock", "气闸", 0, 0, true, FacingMode.NONE, false),
    THERMOPILE("thermopile", "Thermopile", "热电堆", 0, 0, true, FacingMode.NONE, false),
    RF_TRANSFORMER("forge_energy_transformer", "Forge Energy Transformer", "Forge 能量变压器", 0, 0, true, FacingMode.NONE, false),
    ELECTRIC_ENGINE("electric_engine", "Electric Engine", "电动机", 0, 0, true, FacingMode.ALL, false);

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final int inventorySlots;
    private final int ghostSlots;
    private final boolean menu;
    private final FacingMode facingMode;
    private final boolean doubleLength;

    SingleBlockMachineDefinition(
            String id,
            String englishName,
            String chineseName,
            int inventorySlots,
            int ghostSlots,
            boolean menu,
            FacingMode facingMode,
            boolean doubleLength
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.inventorySlots = inventorySlots;
        this.ghostSlots = ghostSlots;
        this.menu = menu;
        this.facingMode = facingMode;
        this.doubleLength = doubleLength;
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

    public int inventorySlots() {
        return inventorySlots;
    }

    public int ghostSlots() {
        return ghostSlots;
    }

    public boolean hasMenu() {
        return menu;
    }

    public FacingMode facingMode() {
        return facingMode;
    }

    public boolean doubleLength() {
        return doubleLength;
    }

    public boolean hasLitState() {
        return this == BRICK_FURNACE || this == ELECTRIC_HEATER || this == RF_HEATER;
    }

    public boolean isWooden() {
        return this == BOX || this == SLUICE_BOX || this == FEEDING_TROUGH;
    }

    public enum FacingMode {
        NONE,
        HORIZONTAL,
        ALL
    }
}
