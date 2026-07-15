package committee.nova.mods.magneticraft.content.machine.singleblock;

import java.util.List;
import java.util.Set;

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
    WATER_GENERATOR("water_generator", "Water Generator", "供水器", 0, 0, false, FacingMode.NONE, false),
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

    /**
     * Immutable slot ownership contract. Menu layout and capability views may present
     * the slots differently, but they must not change their durable role.
     */
    public List<SlotRole> slotRoles() {
        return switch (this) {
            case BOX -> repeated(SlotRole.STORAGE, 27);
            case SLUICE_BOX, FEEDING_TROUGH -> List.of(SlotRole.INPUT);
            case FABRICATOR, RELAY -> repeated(SlotRole.STORAGE, inventorySlots);
            case FILTER -> List.of(SlotRole.INTERNAL_BUFFER);
            case INSERTER -> List.of(SlotRole.CARRIED, SlotRole.UPGRADE, SlotRole.UPGRADE);
            case COMBUSTION_CHAMBER -> List.of(SlotRole.FUEL);
            case GASIFICATION_UNIT, BRICK_FURNACE -> List.of(SlotRole.INPUT, SlotRole.OUTPUT);
            default -> List.of();
        };
    }

    /** Data recipe family consumed by this machine; crafting its block is intentionally separate. */
    public ProcessingKind processingKind() {
        return switch (this) {
            case SLUICE_BOX -> ProcessingKind.SLUICE_BOX;
            case FABRICATOR -> ProcessingKind.CRAFTING;
            case GASIFICATION_UNIT -> ProcessingKind.GASIFICATION;
            case BRICK_FURNACE -> ProcessingKind.SMELTING;
            case THERMOPILE -> ProcessingKind.THERMOPILE;
            default -> ProcessingKind.NONE;
        };
    }

    /** Released Nova single-block machines did not gate their work on redstone. */
    public RedstoneControl redstoneControl() {
        return RedstoneControl.IGNORED;
    }

    /** Stable player-facing description of the capability side contract. */
    public AutomationProfile automationProfile() {
        return switch (this) {
            case SLUICE_BOX, FEEDING_TROUGH, INSERTER -> AutomationProfile.NONE;
            case RELAY -> AutomationProfile.ALL_EXCEPT_OUTPUT_FACE;
            case FILTER, TRANSPOSER -> AutomationProfile.PNEUMATIC_ONLY;
            case STEAM_BOILER -> AutomationProfile.BOILER_ALL_SIDES;
            case ELECTRIC_HEATER -> AutomationProfile.ELECTRICITY_WITH_VERTICAL_HEAT;
            case RF_HEATER -> AutomationProfile.ALL_SIDES_FORGE_ENERGY_WITH_VERTICAL_HEAT;
            case ELECTRIC_ENGINE ->
                    AutomationProfile.ALL_SIDES_BIDIRECTIONAL_FORGE_ENERGY_WITH_DIRECTIONAL_OUTPUT;
            case GASIFICATION_UNIT, BRICK_FURNACE -> AutomationProfile.ITEM_INPUT_OUTPUT;
            case WATER_GENERATOR -> AutomationProfile.FLUID_OUTPUT_ALL_SIDES;
            default -> AutomationProfile.ALL_SIDES;
        };
    }

    /** Physical domains actually owned by this block entity. */
    public Set<PhysicalPort> physicalPorts() {
        return SingleBlockPortProfile.physicalPorts(this);
    }

    public boolean usesElectricity() {
        return physicalPorts().contains(PhysicalPort.ELECTRICITY);
    }

    public String guideCategory() {
        return switch (this) {
            case BOX, SMALL_TANK -> "storage";
            case SLUICE_BOX, FABRICATOR, GASIFICATION_UNIT, BRICK_FURNACE -> "processing";
            case FEEDING_TROUGH, INSERTER, RELAY, FILTER, TRANSPOSER, AIRLOCK -> "utility";
            default -> "energy";
        };
    }

    private static List<SlotRole> repeated(SlotRole role, int count) {
        return java.util.Collections.nCopies(count, role);
    }

    public enum FacingMode {
        NONE,
        HORIZONTAL,
        ALL
    }

    public enum SlotRole {
        STORAGE,
        INPUT,
        OUTPUT,
        FUEL,
        CARRIED,
        UPGRADE,
        INTERNAL_BUFFER
    }

    public enum ProcessingKind {
        NONE,
        CRAFTING,
        SLUICE_BOX,
        SMELTING,
        GASIFICATION,
        THERMOPILE
    }

    public enum RedstoneControl {
        IGNORED
    }

    public enum AutomationProfile {
        NONE,
        ALL_SIDES,
        ALL_EXCEPT_OUTPUT_FACE,
        ITEM_INPUT_OUTPUT,
        FLUID_OUTPUT_ALL_SIDES,
        BOILER_ALL_SIDES,
        ELECTRICITY_WITH_VERTICAL_HEAT,
        ALL_SIDES_FORGE_ENERGY_WITH_VERTICAL_HEAT,
        ALL_SIDES_BIDIRECTIONAL_FORGE_ENERGY_WITH_DIRECTIONAL_OUTPUT,
        PNEUMATIC_ONLY
    }

    public enum PhysicalPort {
        ITEM,
        ITEM_TRANSFER,
        GHOST_FILTER,
        FLUID_INPUT,
        FLUID_OUTPUT,
        FORGE_ENERGY,
        ELECTRICITY,
        HEAT,
        PNEUMATIC
    }
}
