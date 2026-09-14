package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.content.world.ProtectedWorldMutation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Optional;
import java.util.Iterator;

/**
 * Heat production and heat-driven processing behavior.
 */
final class SingleBlockThermalLogic {
    private static final double CELSIUS_OFFSET = 273.15D;
    private static final double MACHINE_MAX_TEMPERATURE = 600.0D + CELSIUS_OFFSET;
    private static final double BOILING_TEMPERATURE = 100.0D + CELSIUS_OFFSET;
    private static final double FURNACE_MINIMUM_TEMPERATURE = 60.0D + CELSIUS_OFFSET;
    private static final int PROCESS_SCALE = 40;
    private static final int WORKING_GRACE_TICKS = 20;
    private static final double GEOTHERMAL_MAX_TEMPERATURE_KELVIN = 1_473.15D;
    private static final double GEOTHERMAL_SOURCE_ENERGY_JOULES = 200_000.0D;
    private static final double GEOTHERMAL_MAX_OUTPUT_JOULES_PER_TICK = 120.0D;

    private final SingleBlockMachineBlockEntity machine;
    private final SingleBlockMachineState state;

    SingleBlockThermalLogic(SingleBlockMachineBlockEntity machine, SingleBlockMachineState state) {
        this.machine = machine;
        this.state = state;
    }

    void tickCombustionChamber() {
        resetRates();
        if (machine.inventory() == null || machine.heat() == null) {
            return;
        }
        if (state.burnTotal <= 0) {
            ItemStack fuel = machine.inventory().getStackInSlot(0);
            if (SingleBlockMachineSupport.isCombustionFuel(fuel)) {
                int duration = fuel.getBurnTime(RecipeType.SMELTING);
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
            state.lastProduction = speed * 10;
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
        resetRates();
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
            state.lastConsumption = water * 20;
            state.lastProduction = water * 10;
            machine.markChanged();
        }
        pushFluidAbove(machine.secondaryTank(), Direction.DOWN);
    }

    void tickHeater() {
        resetRates();
        if ((machine.energy() == null && machine.forgeEnergy() == null) || machine.heat() == null) {
            return;
        }
        int available = machine.energy() != null
                ? machine.energy().consumeJoules(80, true)
                : machine.forgeEnergy().extractEnergy(80, true);
        if (machine.heat().node().temperatureKelvin() < MACHINE_MAX_TEMPERATURE && available == 80) {
            if (machine.energy() != null) {
                machine.energy().consumeJoules(80, false);
            } else {
                machine.forgeEnergy().extractEnergy(80, false);
            }
            machine.heat().node().addHeat(80.0D, false);
            state.working = true;
            state.lastConsumption = 80;
            state.lastProduction = 80;
            machine.markChanged();
        } else if ((machine.energy() != null
                ? machine.energy().storedWholeJoules()
                : machine.forgeEnergy().getEnergyStored()) < 80) {
            SingleBlockMachineSupport.dissipateHeat(machine, 10.0D);
        }
    }

    void tickGasification(ServerLevel level) {
        resetRates();
        if (machine.inventory() == null || machine.primaryTank() == null || machine.heat() == null) {
            return;
        }
        state.working = state.wasWorkingRecently(level.getGameTime(), WORKING_GRACE_TICKS);
        pushFluidAbove(machine.primaryTank(), Direction.UP);
        ItemStack input = machine.inventory().getStackInSlot(0);
        Optional<RecipeHolder<GasificationRecipe>> recipeHolder = level.getRecipeManager().getRecipeFor(
                ModRecipeTypes.GASIFICATION_TYPE.get(),
                new SingleRecipeInput(input.copyWithCount(1)),
                level
        );
        if (recipeHolder.isEmpty() || !canAcceptGasification(recipeHolder.get().value())) {
            return;
        }
        GasificationRecipe current = recipeHolder.get().value();
        selectRecipe(recipeHolder.get().id().toString());
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
        state.lastConsumption = speed;
        state.recordWorking(level.getGameTime());
        if (state.progress >= state.totalProgress) {
            machine.inventory().extractInternal(0, 1, false);
            SingleBlockMachineSupport.insertInventory(machine, 1, current.itemOutput());
            machine.primaryTank().tank().fill(current.fluidOutput(), IFluidHandler.FluidAction.EXECUTE);
            state.progress = 0;
        }
        machine.markChanged();
    }

    void tickBrickFurnace(ServerLevel level) {
        resetRates();
        if (machine.inventory() == null || machine.heat() == null) {
            return;
        }
        state.working = state.wasWorkingRecently(level.getGameTime(), WORKING_GRACE_TICKS);
        ItemStack input = machine.inventory().getStackInSlot(0);
        Optional<RecipeHolder<SmeltingRecipe>> recipeHolder = level.getRecipeManager().getRecipeFor(
                RecipeType.SMELTING,
                new SingleRecipeInput(input.copyWithCount(1)),
                level
        );
        if (recipeHolder.isEmpty()
                || !SingleBlockMachineSupport.canAcceptItemOutput(
                machine,
                1,
                recipeHolder.get().value().getResultItem(level.registryAccess())
        )) {
            return;
        }
        selectRecipe(recipeHolder.get().id().toString());
        state.totalProgress = 100 * 20;
        int speed = SingleBlockMachineSupport.thermalSpeed(machine, FURNACE_MINIMUM_TEMPERATURE, 20);
        if (speed <= 0 || machine.heat().node().removeHeat(speed, true) < speed) {
            return;
        }
        machine.heat().node().removeHeat(speed, false);
        state.progress += speed;
        state.lastConsumption = speed;
        state.recordWorking(level.getGameTime());
        if (state.progress >= state.totalProgress) {
            machine.inventory().extractInternal(0, 1, false);
            SingleBlockMachineSupport.insertInventory(
                    machine,
                    1,
                    recipeHolder.get().value().getResultItem(level.registryAccess()).copy()
            );
            state.progress = 0;
        }
        machine.markChanged();
    }

    void tickGeothermalPump(ServerLevel level) {
        resetRates();
        GeothermalPumpState geothermal = machine.geothermalPumpState();
        if (geothermal == null || geothermal.owner() == null || machine.heat() == null) {
            return;
        }

        BlockPos drillPosition = geothermal.drillPosition(machine.getBlockPos());
        if (ProtectedWorldMutation.isSafeLoadedTarget(level, drillPosition)) {
            if (level.getFluidState(drillPosition).is(net.minecraft.tags.FluidTags.LAVA)) {
                geothermal.startSearch(drillPosition);
                if (!geothermal.searchComplete() && geothermal.scanLoadedLava(level)) {
                    machine.markChanged();
                }
            } else if (level.getGameTime() % 20L == 0L) {
                if (level.getBlockState(drillPosition).is(ModMachineBlocks.GEOTHERMAL_DRILL_PIPE.get())) {
                    geothermal.advanceDrill();
                    machine.markChanged();
                } else if (ProtectedWorldMutation.replaceWithoutDrops(
                        level,
                        geothermal.owner(),
                        machine.getBlockPos(),
                        drillPosition,
                        Direction.UP,
                        ModMachineBlocks.GEOTHERMAL_DRILL_PIPE.get().defaultBlockState()
                )) {
                    geothermal.advanceDrill();
                    machine.markChanged();
                }
            }
        }

        produceGeothermalHeat(level, geothermal);
    }

    private void produceGeothermalHeat(ServerLevel level, GeothermalPumpState geothermal) {
        double temperature = machine.heat().node().temperatureKelvin();
        if (temperature >= GEOTHERMAL_MAX_TEMPERATURE_KELVIN) {
            return;
        }
        double headroom = (GEOTHERMAL_MAX_TEMPERATURE_KELVIN - temperature)
                * machine.heat().node().heatCapacityJoulesPerKelvin();
        if (headroom <= 1.0E-9D) {
            return;
        }
        if (geothermal.remainingEnergyJoules() <= 1.0E-9D && !consumeLavaSource(level, geothermal)) {
            return;
        }

        double produced = Math.min(
                Math.min(GEOTHERMAL_MAX_OUTPUT_JOULES_PER_TICK, geothermal.remainingEnergyJoules()),
                headroom
        );
        if (produced <= 1.0E-9D) {
            return;
        }
        machine.heat().node().addHeat(produced, false);
        geothermal.setRemainingEnergyJoules(geothermal.remainingEnergyJoules() - produced);
        state.lastProduction = (int) Math.round(produced);
        state.working = true;
        machine.markChanged();
    }

    private boolean consumeLavaSource(ServerLevel level, GeothermalPumpState geothermal) {
        Iterator<BlockPos> iterator = geothermal.sources().iterator();
        while (iterator.hasNext()) {
            BlockPos source = iterator.next();
            if (!ProtectedWorldMutation.isSafeLoadedTarget(level, source)) {
                continue;
            }
            if (!level.getFluidState(source).isSource()
                    || !level.getFluidState(source).is(net.minecraft.tags.FluidTags.LAVA)) {
                iterator.remove();
                machine.markChanged();
                continue;
            }
            if (ProtectedWorldMutation.replaceWithoutDrops(
                    level,
                    geothermal.owner(),
                    machine.getBlockPos(),
                    source,
                    Direction.UP,
                    Blocks.OBSIDIAN.defaultBlockState()
            )) {
                iterator.remove();
                geothermal.setRemainingEnergyJoules(GEOTHERMAL_SOURCE_ENERGY_JOULES);
                machine.markChanged();
                return true;
            }
        }
        return false;
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
        }
    }

    private void resetRates() {
        state.lastConsumption = 0;
        state.lastProduction = 0;
    }

    /** Legacy exporters above the machine used an explicit, machine-specific target face. */
    private int pushFluidAbove(FluidTankModule source, Direction targetSide) {
        if (!(machine.getLevel() instanceof ServerLevel level)) {
            return 0;
        }
        BlockPos targetPosition = machine.getBlockPos().above();
        var targetChunk = level.getChunkSource().getChunkNow(
                targetPosition.getX() >> 4,
                targetPosition.getZ() >> 4
        );
        if (targetChunk == null) {
            return 0;
        }
        BlockEntity targetEntity = targetChunk.getBlockEntity(targetPosition);
        if (targetEntity == null) {
            return 0;
        }
        IFluidHandler target = Capabilities.FluidHandler.BLOCK.getCapability(
                level, targetPosition, level.getBlockState(targetPosition), targetEntity, targetSide
        );
        if (target == null) {
            return 0;
        }
        FluidStack offered = source.tank().drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return 0;
        }
        int accepted = Math.min(offered.getAmount(), target.fill(offered, IFluidHandler.FluidAction.SIMULATE));
        if (accepted <= 0) {
            return 0;
        }
        FluidStack drained = source.tank().drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        int inserted = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (inserted < drained.getAmount()) {
            FluidStack remainder = drained.copy();
            remainder.setAmount(drained.getAmount() - inserted);
            source.tank().fill(remainder, IFluidHandler.FluidAction.EXECUTE);
        }
        return inserted;
    }
}
