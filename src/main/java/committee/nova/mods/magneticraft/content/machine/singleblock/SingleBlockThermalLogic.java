package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Optional;

/**
 * Heat production and heat-driven processing behavior.
 */
final class SingleBlockThermalLogic {
    private static final double CELSIUS_OFFSET = 273.15D;
    private static final double MACHINE_MAX_TEMPERATURE = 600.0D + CELSIUS_OFFSET;
    private static final double BOILING_TEMPERATURE = 100.0D + CELSIUS_OFFSET;
    private static final double FURNACE_MINIMUM_TEMPERATURE = 60.0D + CELSIUS_OFFSET;
    private static final int PROCESS_SCALE = 40;

    private final SingleBlockMachineBlockEntity machine;
    private final SingleBlockMachineState state;

    SingleBlockThermalLogic(SingleBlockMachineBlockEntity machine, SingleBlockMachineState state) {
        this.machine = machine;
        this.state = state;
    }

    void tickCombustionChamber() {
        if (machine.inventory() == null || machine.heat() == null) {
            return;
        }
        if (state.burnTotal <= 0) {
            ItemStack fuel = machine.inventory().getStackInSlot(0);
            if (SingleBlockMachineSupport.isCombustionFuel(fuel)) {
                int duration = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING);
                if (duration > 0) {
                    machine.inventory().extractInternal(0, 1, false);
                    state.burnTotal = duration;
                    state.burnProgress = 0;
                    machine.markChanged();
                }
            }
        }
        if (state.burnTotal > 0 && machine.heat().node().temperatureKelvin() < MACHINE_MAX_TEMPERATURE) {
            int speed = state.doorOpen ? 2 : 4;
            state.burnProgress += speed;
            machine.heat().node().addHeat(speed * 10.0D, false);
            state.working = true;
            if (state.burnProgress >= state.burnTotal) {
                state.burnProgress = 0;
                state.burnTotal = 0;
            }
            machine.markChanged();
        } else if (state.burnTotal <= 0) {
            SingleBlockMachineSupport.dissipateHeat(machine, 10.0D);
        }
    }

    void tickSteamBoiler() {
        if (machine.primaryTank() == null || machine.secondaryTank() == null || machine.heat() == null) {
            return;
        }
        double excessHeat = Math.max(
                0.0D,
                (machine.heat().node().temperatureKelvin() - BOILING_TEMPERATURE)
                        * machine.heat().node().heatCapacityJoulesPerKelvin()
        );
        int water = Math.min(2, machine.primaryTank().tank().getFluidAmount());
        water = Math.min(
                water,
                (machine.secondaryTank().tank().getCapacity() - machine.secondaryTank().tank().getFluidAmount()) / 10
        );
        water = Math.min(water, (int) Math.floor(excessHeat / 20.0D));
        if (water > 0) {
            machine.primaryTank().tank().drain(water, IFluidHandler.FluidAction.EXECUTE);
            machine.secondaryTank().tank().fill(
                    new FluidStack(ModFluids.get(FluidDefinition.STEAM).source().get(), water * 10),
                    IFluidHandler.FluidAction.EXECUTE
            );
            machine.heat().node().removeHeat(water * 20.0D, false);
            state.working = true;
            machine.markChanged();
        }
        SingleBlockMachineSupport.pushFluid(machine, Direction.UP, machine.secondaryTank(), 1_000);
    }

    void tickHeater() {
        if (machine.energy() == null || machine.heat() == null) {
            return;
        }
        if (machine.heat().node().temperatureKelvin() < MACHINE_MAX_TEMPERATURE
                && machine.energy().extractEnergy(80, true) == 80) {
            machine.energy().extractEnergy(80, false);
            machine.heat().node().addHeat(80.0D, false);
            state.working = true;
            machine.markChanged();
        } else if (machine.energy().getEnergyStored() == 0) {
            SingleBlockMachineSupport.dissipateHeat(machine, 10.0D);
        }
    }

    void tickGasification(ServerLevel level) {
        if (machine.inventory() == null || machine.primaryTank() == null || machine.heat() == null) {
            return;
        }
        ItemStack input = machine.inventory().getStackInSlot(0);
        Optional<GasificationRecipe> recipe = level.getRecipeManager().getRecipeFor(
                ModRecipeTypes.GASIFICATION_TYPE.get(),
                new SimpleContainer(input.copyWithCount(1)),
                level
        );
        if (recipe.isEmpty() || !canAcceptGasification(recipe.get())) {
            return;
        }
        GasificationRecipe current = recipe.get();
        selectRecipe(current.getId().toString());
        state.totalProgress = current.durationTicks() * PROCESS_SCALE;
        int speed = SingleBlockMachineSupport.thermalSpeed(
                machine,
                current.minimumTemperatureKelvin(),
                PROCESS_SCALE
        );
        if (speed <= 0 || machine.heat().node().removeHeat(speed, true) < speed) {
            return;
        }
        machine.heat().node().removeHeat(speed, false);
        state.progress += speed;
        state.working = true;
        if (state.progress >= state.totalProgress) {
            machine.inventory().extractInternal(0, 1, false);
            SingleBlockMachineSupport.insertInventory(machine, 1, current.itemOutput());
            machine.primaryTank().tank().fill(current.fluidOutput(), IFluidHandler.FluidAction.EXECUTE);
            state.progress = 0;
        }
        SingleBlockMachineSupport.pushFluid(machine, Direction.UP, machine.primaryTank(), 1_000);
        machine.markChanged();
    }

    void tickBrickFurnace(ServerLevel level) {
        if (machine.inventory() == null || machine.heat() == null) {
            return;
        }
        ItemStack input = machine.inventory().getStackInSlot(0);
        Optional<? extends AbstractCookingRecipe> recipe = level.getRecipeManager().getRecipeFor(
                RecipeType.SMELTING,
                new SimpleContainer(input.copyWithCount(1)),
                level
        );
        if (recipe.isEmpty()
                || !SingleBlockMachineSupport.canAcceptItemOutput(
                machine,
                1,
                recipe.get().getResultItem(level.registryAccess())
        )) {
            return;
        }
        selectRecipe(recipe.get().getId().toString());
        state.totalProgress = 100 * 20;
        int speed = SingleBlockMachineSupport.thermalSpeed(machine, FURNACE_MINIMUM_TEMPERATURE, 20);
        if (speed <= 0 || machine.heat().node().removeHeat(speed, true) < speed) {
            return;
        }
        machine.heat().node().removeHeat(speed, false);
        state.progress += speed;
        state.working = true;
        if (state.progress >= state.totalProgress) {
            machine.inventory().extractInternal(0, 1, false);
            SingleBlockMachineSupport.insertInventory(
                    machine,
                    1,
                    recipe.get().getResultItem(level.registryAccess()).copy()
            );
            state.progress = 0;
        }
        machine.markChanged();
    }

    private boolean canAcceptGasification(GasificationRecipe recipe) {
        return machine.primaryTank() != null
                && SingleBlockMachineSupport.canAcceptItemOutput(machine, 1, recipe.itemOutput())
                && machine.primaryTank().tank().fill(recipe.fluidOutput(), IFluidHandler.FluidAction.SIMULATE)
                == recipe.fluidOutput().getAmount();
    }

    private void selectRecipe(String recipeId) {
        if (!state.activeRecipe.equals(recipeId)) {
            state.activeRecipe = recipeId;
            state.progress = 0;
        }
    }
}
