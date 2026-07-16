package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * Durable, loader-independent runtime state shared by the single-block behavior strategies.
 */
final class SingleBlockMachineState {
    private static final int DEFAULT_INSERTER_FLAGS = (1 << 2) | (1 << 3);
    private static final String PROGRESS_TAG = "progress";
    private static final String TOTAL_PROGRESS_TAG = "total_progress";
    private static final String BURN_PROGRESS_TAG = "burn_progress";
    private static final String BURN_TOTAL_TAG = "burn_total";
    private static final String COOLDOWN_TAG = "cooldown";
    private static final String CHAIN_DELAY_TAG = "chain_delay";
    private static final String FLUID_OUTPUT_CURSOR_TAG = "fluid_output_cursor";
    private static final String WORKING_TAG = "working";
    private static final String DOOR_OPEN_TAG = "door_open";
    private static final String TANK_EXPORT_ENABLED_TAG = "tank_export_enabled";
    private static final String INSERTER_FLAGS_TAG = "inserter_flags";
    private static final String ACTIVE_RECIPE_TAG = "active_recipe";
    private static final String THERMOPILE_FLUX_TAG = "thermopile_flux";
    private static final String LAST_WORKING_TICK_TAG = "last_working_tick";
    private static final String FUEL_ENERGY_TAG = "fuel_energy_joules";

    int progress;
    int totalProgress;
    int burnProgress;
    int burnTotal;
    int cooldown;
    int chainDelay;
    int fluidOutputCursor;
    boolean working;
    boolean doorOpen;
    boolean tankExportEnabled;
    boolean inserterWhitelist;
    boolean inserterUseTags;
    boolean inserterUseDamage = true;
    boolean inserterUseNbt = true;
    boolean inserterDropItems;
    boolean inserterGrabItems;
    String activeRecipe = "";
    double thermopileFlux;
    double fuelEnergyJoules;
    long lastWorkingTick = -1L;
    int lastConsumption;
    int lastProduction;

    void recordWorking(long gameTime) {
        lastWorkingTick = Math.max(0L, gameTime);
        working = true;
    }

    boolean wasWorkingRecently(long gameTime, int graceTicks) {
        return graceTicks > 0
                && lastWorkingTick >= 0L
                && gameTime >= lastWorkingTick
                && gameTime - lastWorkingTick < graceTicks;
    }

    int inserterFlags() {
        int flags = 0;
        flags |= inserterWhitelist ? 1 : 0;
        flags |= inserterUseTags ? 1 << 1 : 0;
        flags |= inserterUseDamage ? 1 << 2 : 0;
        flags |= inserterUseNbt ? 1 << 3 : 0;
        flags |= inserterDropItems ? 1 << 4 : 0;
        flags |= inserterGrabItems ? 1 << 5 : 0;
        return flags;
    }

    boolean toggleInserterFlag(int flag) {
        switch (flag) {
            case 0 -> inserterWhitelist = !inserterWhitelist;
            case 1 -> inserterUseTags = !inserterUseTags;
            case 2 -> inserterUseDamage = !inserterUseDamage;
            case 3 -> inserterUseNbt = !inserterUseNbt;
            case 4 -> inserterDropItems = !inserterDropItems;
            case 5 -> inserterGrabItems = !inserterGrabItems;
            default -> {
                return false;
            }
        }
        return true;
    }

    void save(CompoundTag tag) {
        tag.putInt(PROGRESS_TAG, progress);
        tag.putInt(TOTAL_PROGRESS_TAG, totalProgress);
        tag.putInt(BURN_PROGRESS_TAG, burnProgress);
        tag.putInt(BURN_TOTAL_TAG, burnTotal);
        tag.putInt(COOLDOWN_TAG, cooldown);
        tag.putInt(CHAIN_DELAY_TAG, chainDelay);
        tag.putInt(FLUID_OUTPUT_CURSOR_TAG, fluidOutputCursor);
        tag.putBoolean(WORKING_TAG, working);
        tag.putBoolean(DOOR_OPEN_TAG, doorOpen);
        tag.putBoolean(TANK_EXPORT_ENABLED_TAG, tankExportEnabled);
        tag.putInt(INSERTER_FLAGS_TAG, inserterFlags());
        tag.putString(ACTIVE_RECIPE_TAG, activeRecipe);
        tag.putDouble(THERMOPILE_FLUX_TAG, thermopileFlux);
        tag.putLong(LAST_WORKING_TICK_TAG, lastWorkingTick);
        tag.putDouble(FUEL_ENERGY_TAG, fuelEnergyJoules);
    }

    void load(CompoundTag tag) {
        progress = nonNegative(tag.getInt(PROGRESS_TAG));
        totalProgress = nonNegative(tag.getInt(TOTAL_PROGRESS_TAG));
        burnProgress = nonNegative(tag.getInt(BURN_PROGRESS_TAG));
        burnTotal = nonNegative(tag.getInt(BURN_TOTAL_TAG));
        cooldown = nonNegative(tag.getInt(COOLDOWN_TAG));
        chainDelay = nonNegative(tag.getInt(CHAIN_DELAY_TAG));
        fluidOutputCursor = Math.floorMod(tag.getInt(FLUID_OUTPUT_CURSOR_TAG), Direction.values().length);
        working = tag.getBoolean(WORKING_TAG);
        doorOpen = tag.getBoolean(DOOR_OPEN_TAG);
        tankExportEnabled = tag.getBoolean(TANK_EXPORT_ENABLED_TAG);
        loadInserterFlags(tag.contains(INSERTER_FLAGS_TAG, Tag.TAG_INT)
                ? tag.getInt(INSERTER_FLAGS_TAG)
                : DEFAULT_INSERTER_FLAGS);
        activeRecipe = tag.getString(ACTIVE_RECIPE_TAG);
        double loadedFlux = tag.getDouble(THERMOPILE_FLUX_TAG);
        thermopileFlux = Double.isFinite(loadedFlux) ? Math.max(0.0D, loadedFlux) : 0.0D;
        lastWorkingTick = tag.contains(LAST_WORKING_TICK_TAG, Tag.TAG_LONG)
                ? Math.max(-1L, tag.getLong(LAST_WORKING_TICK_TAG))
                : -1L;
        double loadedFuelEnergy = tag.getDouble(FUEL_ENERGY_TAG);
        fuelEnergyJoules = Double.isFinite(loadedFuelEnergy) ? Math.max(0.0D, loadedFuelEnergy) : 0.0D;
    }

    private void loadInserterFlags(int flags) {
        inserterWhitelist = (flags & 1) != 0;
        inserterUseTags = (flags & (1 << 1)) != 0;
        inserterUseDamage = (flags & (1 << 2)) != 0;
        inserterUseNbt = (flags & (1 << 3)) != 0;
        inserterDropItems = (flags & (1 << 4)) != 0;
        inserterGrabItems = (flags & (1 << 5)) != 0;
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }
}
