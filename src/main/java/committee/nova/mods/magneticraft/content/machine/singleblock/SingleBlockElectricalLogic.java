package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Native-electricity generation and airlock behavior.
 */
final class SingleBlockElectricalLogic {
    private static final double AIRLOCK_MIN_VOLTAGE = 60.0D;
    private static final double INTERNAL_ENGINE_ELECTRIC_FRACTION = 0.70D;
    private static final double INTERNAL_ENGINE_HEAT_FRACTION = 0.30D;
    private static final double INTERNAL_ENGINE_MAX_ELECTRICAL_OUTPUT = 120.0D;

    private final SingleBlockMachineBlockEntity machine;
    private final SingleBlockMachineState state;

    SingleBlockElectricalLogic(SingleBlockMachineBlockEntity machine, SingleBlockMachineState state) {
        this.machine = machine;
        this.state = state;
    }

    void tickInfiniteEnergy() {
        if (machine.voltageSource() != null) {
            state.lastProduction = (int) Math.floor(machine.voltageSource().lastProductionJoules());
            state.working = state.lastProduction > 0;
            machine.markChanged();
        }
    }

    void tickAirlock(ServerLevel level) {
        if (level.getGameTime() % 40L != 0L || machine.energy() == null) {
            return;
        }
        BlockPos origin = machine.getBlockPos();
        if (!scanAreaLoaded(level, origin)) {
            return;
        }

        AirlockPlan plan = AirlockPlan.build((x, y, z) -> cellAt(level, origin.offset(x, y, z)));
        if (plan.operations().isEmpty()) {
            return;
        }
        int requested = (int) Math.ceil(plan.totalCostJoules());
        int simulated = machine.energy().consumeJoules(requested, true);
        if (!plan.canAfford(simulated)) {
            startAirBubbleDecay(level);
            return;
        }
        int removed = machine.energy().consumeJoules(requested, false);
        if (!plan.canAfford(removed)) {
            machine.energy().restoreStoredJoules(machine.energy().storedJoules() + removed);
            startAirBubbleDecay(level);
            return;
        }

        BlockState stableBubble = ModMachineBlocks.AIR_BUBBLE.get().defaultBlockState()
                .setValue(AirBubbleBlock.DECAYING, false);
        AirBubbleOwnershipSavedData ownership = AirBubbleOwnershipSavedData.get(level);
        for (AirlockPlan.Operation operation : plan.operations()) {
            BlockPos target = origin.offset(
                    operation.offset().x(),
                    operation.offset().y(),
                    operation.offset().z()
            );
            BlockState targetState = operation.target() == AirlockPlan.Target.STABLE_BUBBLE
                    ? stableBubble
                    : Blocks.AIR.defaultBlockState();
            if (operation.target() == AirlockPlan.Target.STABLE_BUBBLE) {
                ownership.bind(target, origin);
            } else {
                ownership.unbindBubble(target);
            }
            if (!level.getBlockState(target).equals(targetState)) {
                level.setBlock(target, targetState, Block.UPDATE_ALL);
            }
            if (operation.target() == AirlockPlan.Target.STABLE_BUBBLE) {
                AirBubbleBlock.scheduleOwnerValidation(level, target);
            }
        }
        state.working = !plan.operations().isEmpty();
        machine.markChanged();
    }

    void tickThermopile(ServerLevel level) {
        if (!thermopileNeighborsLoaded(level)) {
            return;
        }
        if (level.getGameTime() % 20L == 0L) {
            List<ThermalSource> sources = new ArrayList<>(Direction.values().length);
            for (Direction direction : Direction.values()) {
                sources.add(thermalSource(
                        level,
                        machine.getBlockPos().relative(direction),
                        direction.getOpposite()
                ));
            }
            double flux = 0.0D;
            for (int first = 0; first < sources.size(); first++) {
                for (int second = first + 1; second < sources.size(); second++) {
                    ThermalSource a = sources.get(first);
                    ThermalSource b = sources.get(second);
                    double conductivity = 1.0D / (1.0D / a.conductivity() + 1.0D / b.conductivity());
                    flux += conductivity * Math.abs(a.temperatureKelvin() - b.temperatureKelvin());
                }
            }
            state.thermopileFlux = flux;
            machine.markChanged();
        }
        if (machine.energy() != null) {
            double generated = state.thermopileFlux / 10_000.0D * 20.0D;
            if (machine.energy().generateJoules(generated, false) > 0.0D) {
                state.working = true;
                machine.markChanged();
            }
        }
    }

    void tickInternalCombustionEngine(ServerLevel level) {
        state.lastConsumption = 0;
        state.lastProduction = 0;
        if (machine.primaryTank() == null || machine.energy() == null || machine.heat() == null) {
            return;
        }

        double temperature = machine.heat().node().temperatureKelvin();
        double throttle = SingleBlockMachineMath.internalCombustionThrottle(temperature);
        if (throttle <= 0.0D) {
            return;
        }
        double maximumRawEnergy = INTERNAL_ENGINE_MAX_ELECTRICAL_OUTPUT
                / INTERNAL_ENGINE_ELECTRIC_FRACTION * throttle;
        double heatHeadroom = Math.max(
                0.0D,
                (SingleBlockMachineMath.INTERNAL_ENGINE_CUTOFF_TEMPERATURE_KELVIN - temperature)
                        * machine.heat().node().heatCapacityJoulesPerKelvin()
        );
        double electricityHeadroom = machine.energy().generateJoules(
                INTERNAL_ENGINE_MAX_ELECTRICAL_OUTPUT * throttle,
                true
        );
        if (electricityHeadroom <= 0.0D || heatHeadroom <= 0.0D) {
            return;
        }

        if (state.fuelEnergyJoules <= 1.0E-9D) {
            Optional<FluidFuelRecipe> recipe = currentFluidFuel(level);
            if (recipe.isEmpty()) {
                return;
            }
            var drained = machine.primaryTank().tank().drain(1, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() != 1) {
                return;
            }
            state.fuelEnergyJoules = recipe.get().totalEnergyPerMilliBucket();
        }

        double rawEnergy = Math.min(
                Math.min(maximumRawEnergy, state.fuelEnergyJoules),
                Math.min(
                        electricityHeadroom / INTERNAL_ENGINE_ELECTRIC_FRACTION,
                        heatHeadroom / INTERNAL_ENGINE_HEAT_FRACTION
                )
        );
        if (rawEnergy <= 1.0E-9D) {
            return;
        }
        double electricalEnergy = rawEnergy * INTERNAL_ENGINE_ELECTRIC_FRACTION;
        double thermalEnergy = rawEnergy * INTERNAL_ENGINE_HEAT_FRACTION;
        double inserted = machine.energy().generateJoules(electricalEnergy, false);
        if (inserted + 1.0E-9D < electricalEnergy) {
            return;
        }
        machine.heat().node().addHeat(thermalEnergy, false);
        state.fuelEnergyJoules = Math.max(0.0D, state.fuelEnergyJoules - rawEnergy);
        state.lastConsumption = (int) Math.round(rawEnergy);
        state.lastProduction = (int) Math.round(inserted);
        state.working = true;
        machine.markChanged();
    }

    private Optional<FluidFuelRecipe> currentFluidFuel(ServerLevel level) {
        if (machine.primaryTank() == null || machine.primaryTank().tank().getFluid().isEmpty()) {
            return Optional.empty();
        }
        var fluid = machine.primaryTank().tank().getFluid().getFluid();
        return level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.FLUID_FUEL_TYPE.get())
                .stream()
                .filter(recipe -> recipe.fluid() == fluid)
                .findFirst();
    }

    void startAirBubbleDecay(ServerLevel level) {
        BlockPos origin = machine.getBlockPos();
        AirBubbleOwnershipSavedData ownership = AirBubbleOwnershipSavedData.get(level);
        Set<BlockPos> targets = new LinkedHashSet<>(ownership.unbindAirlock(origin));
        for (AirlockPlan.Offset offset : AirlockPlan.activeOffsets()) {
            BlockPos target = origin.offset(offset.x(), offset.y(), offset.z());
            if (ownership.ownerOf(target).isEmpty()) {
                targets.add(target);
            }
        }
        for (BlockPos target : targets) {
            if (level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) == null) {
                continue;
            }
            BlockState targetState = level.getBlockState(target);
            if (targetState.is(ModMachineBlocks.AIR_BUBBLE.get())
                    && !targetState.getValue(AirBubbleBlock.DECAYING)) {
                AirBubbleBlock.beginDecay(level, target, targetState);
            }
        }
    }

    private static boolean scanAreaLoaded(ServerLevel level, BlockPos origin) {
        int minimumChunkX = (origin.getX() - AirlockPlan.SCAN_RANGE) >> 4;
        int maximumChunkX = (origin.getX() + AirlockPlan.SCAN_RANGE) >> 4;
        int minimumChunkZ = (origin.getZ() - AirlockPlan.SCAN_RANGE) >> 4;
        int maximumChunkZ = (origin.getZ() + AirlockPlan.SCAN_RANGE) >> 4;
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static AirlockPlan.Cell cellAt(ServerLevel level, BlockPos position) {
        BlockState blockState = level.getBlockState(position);
        if (blockState.is(Blocks.WATER)) {
            return AirlockPlan.Cell.WATER;
        }
        if (blockState.is(ModMachineBlocks.AIR_BUBBLE.get())) {
            return blockState.getValue(AirBubbleBlock.DECAYING)
                    ? AirlockPlan.Cell.DECAYING_BUBBLE
                    : AirlockPlan.Cell.STABLE_BUBBLE;
        }
        return blockState.isAir() ? AirlockPlan.Cell.AIR : AirlockPlan.Cell.OTHER;
    }

    private ThermalSource thermalSource(ServerLevel level, BlockPos position, Direction targetSide) {
        BlockState blockState = level.getBlockState(position);
        Optional<ThermopileRecipe> recipe = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.THERMOPILE_TYPE.get())
                .stream()
                .filter(candidate -> candidate.matches(blockState))
                .max(Comparator.comparingInt(ThermopileRecipe::specificity));
        if (recipe.isPresent()) {
            return new ThermalSource(recipe.get().temperatureKelvin(), recipe.get().conductivity());
        }
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof MachineBlockEntity thermalHost)
                || thermalHost.thermalReading(targetSide).isEmpty()) {
            return new ThermalSource(HeatNode.AMBIENT_TEMPERATURE_KELVIN, 1.0D);
        }
        if (blockEntity instanceof SingleBlockMachineBlockEntity otherMachine && otherMachine.heat() != null) {
            return source(otherMachine.heat().node());
        }
        if (blockEntity instanceof AdvancedMultiblockBlockEntity multiblock && multiblock.heat() != null) {
            return source(multiblock.heat().node());
        }
        if (blockEntity instanceof HeatPipeBlockEntity pipe) {
            return source(pipe.heat().node());
        }
        if (blockEntity instanceof HeatSinkBlockEntity sink) {
            return source(sink.heat().node());
        }
        return new ThermalSource(HeatNode.AMBIENT_TEMPERATURE_KELVIN, 1.0D);
    }

    private boolean thermopileNeighborsLoaded(ServerLevel level) {
        for (Direction direction : Direction.values()) {
            BlockPos position = machine.getBlockPos().relative(direction);
            if (level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4) == null) {
                return false;
            }
        }
        return true;
    }

    private static ThermalSource source(HeatNode node) {
        return new ThermalSource(
                node.temperatureKelvin(),
                Math.max(0.000001D, node.conductivity() * 0.01D)
        );
    }

    private record ThermalSource(double temperatureKelvin, double conductivity) {
    }
}
