package committee.nova.mods.magneticraft.content.machine.electricfurnace;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Quantized legacy electric-furnace progress and energy consumption.
 */
public final class ElectricFurnaceProcessModule implements MachineModule {
    public static final int BASE_DURATION_TICKS = 100;
    public static final int SPEED_STEPS = 20;
    public static final int TOTAL_PROGRESS_UNITS = BASE_DURATION_TICKS * SPEED_STEPS;
    public static final int MAX_CONSUMPTION_PER_TICK = 20;

    private static final String PROGRESS_TAG = "progress_units";
    private static final String LAST_WORKING_TICK_TAG = "last_working_tick";
    private static final String WORKING_TAG = "working";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final ItemInventoryModule inventory;
    private final ElectricalPowerModule energy;
    private int progressUnits;
    private boolean working;
    private long lastWorkingTick = Long.MIN_VALUE;

    public ElectricFurnaceProcessModule(
            ResourceLocation id,
            MachineModuleHost host,
            ItemInventoryModule inventory,
            ElectricalPowerModule energy
    ) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.inventory = Objects.requireNonNull(inventory);
        this.energy = Objects.requireNonNull(energy);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        progressUnits = clampProgress(tag.getInt(PROGRESS_TAG));
        lastWorkingTick = tag.contains(LAST_WORKING_TICK_TAG)
                ? tag.getLong(LAST_WORKING_TICK_TAG)
                : Long.MIN_VALUE;
        working = tag.getBoolean(WORKING_TAG);
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(PROGRESS_TAG, progressUnits);
        tag.putLong(LAST_WORKING_TICK_TAG, lastWorkingTick);
        tag.putBoolean(WORKING_TAG, working);
    }

    @Override
    public void serverTick() {
        Level level = host.level();
        if (level == null || level.isClientSide) {
            return;
        }
        Optional<RecipeHolder<? extends AbstractCookingRecipe>> recipe = findRecipe(level);
        if (recipe.isEmpty() || !canAcceptOutput(recipe.get().value(), level.registryAccess())) {
            updateWorkingState(level);
            return;
        }

        int speedUnits = quantizedSpeedUnits();
        if (speedUnits <= 0 || energy.consumeJoules(speedUnits, true) != speedUnits) {
            updateWorkingState(level);
            return;
        }

        energy.consumeJoules(speedUnits, false);
        progressUnits += speedUnits;
        lastWorkingTick = level.getGameTime();
        working = true;
        if (progressUnits >= TOTAL_PROGRESS_UNITS) {
            craft(recipe.get().value(), level.registryAccess());
            progressUnits = 0;
        }
        host.markChanged();
    }

    public int progressUnits() {
        return progressUnits;
    }

    public boolean working() {
        return working;
    }

    private Optional<RecipeHolder<? extends AbstractCookingRecipe>> findRecipe(Level level) {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(
                RecipeType.SMELTING,
                new SingleRecipeInput(input.copyWithCount(1)),
                level
        ).map(Function.identity());
    }

    private boolean canAcceptOutput(AbstractCookingRecipe recipe, RegistryAccess registryAccess) {
        ItemStack result = recipe.getResultItem(registryAccess);
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void craft(AbstractCookingRecipe recipe, RegistryAccess registryAccess) {
        ItemStack result = recipe.getResultItem(registryAccess).copy();
        inventory.extractInternal(0, 1, false);
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            inventory.setStackInSlot(1, result);
        } else {
            output.grow(result.getCount());
            inventory.setStackInSlot(1, output);
        }
    }

    private int quantizedSpeedUnits() {
        if (energy.ratedCapacityJoules() <= 0.0D) {
            return 0;
        }
        return Math.min(
                MAX_CONSUMPTION_PER_TICK,
                (int) Math.floor(energy.storedJoules() * SPEED_STEPS / energy.ratedCapacityJoules())
        );
    }

    private void updateWorkingState(Level level) {
        boolean nowWorking = lastWorkingTick != Long.MIN_VALUE && level.getGameTime() - lastWorkingTick < 20;
        if (working != nowWorking) {
            working = nowWorking;
            host.markChanged();
        }
    }

    private static int clampProgress(int progress) {
        return Math.max(0, Math.min(TOTAL_PROGRESS_UNITS - 1, progress));
    }
}
