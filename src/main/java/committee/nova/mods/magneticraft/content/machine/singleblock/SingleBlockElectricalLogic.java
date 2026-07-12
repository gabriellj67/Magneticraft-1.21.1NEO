package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlockEntity;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Native-electricity generation, conversion and airlock behavior.
 */
final class SingleBlockElectricalLogic {
    private final SingleBlockMachineBlockEntity machine;
    private final SingleBlockMachineState state;

    SingleBlockElectricalLogic(SingleBlockMachineBlockEntity machine, SingleBlockMachineState state) {
        this.machine = machine;
        this.state = state;
    }

    void tickInfiniteEnergy() {
        if (machine.electricity() != null) {
            machine.electricity().node().setVoltage(125.0D);
            state.working = true;
            machine.markChanged();
        }
    }

    void tickAirlock(ServerLevel level) {
        if (level.getGameTime() % 40L != 0L || machine.electricity() == null) {
            return;
        }
        if (machine.electricity().node().voltage() < 60.0D) {
            startAirBubbleDecay(level);
            return;
        }
        BlockState bubble = ModMachineBlocks.AIR_BUBBLE.get().defaultBlockState()
                .setValue(AirBubbleBlock.DECAYING, false);
        for (BlockPos target : BlockPos.betweenClosed(
                machine.getBlockPos().offset(-3, -3, -3),
                machine.getBlockPos().offset(3, 3, 3)
        )) {
            if (target.distSqr(machine.getBlockPos()) > 9.0D) {
                continue;
            }
            BlockState targetState = level.getBlockState(target);
            if (targetState.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                if (machine.electricity().node().removeEnergy(2.0D, true) < 2.0D) {
                    return;
                }
                machine.electricity().node().removeEnergy(2.0D, false);
                level.setBlock(target, bubble, Block.UPDATE_ALL);
                state.working = true;
            } else if (targetState.is(ModMachineBlocks.AIR_BUBBLE.get())
                    && targetState.getValue(AirBubbleBlock.DECAYING)) {
                level.setBlock(target, bubble, Block.UPDATE_ALL);
            }
        }
        machine.markChanged();
    }

    void tickThermopile(ServerLevel level) {
        if (level.getGameTime() % 20L == 0L) {
            List<ThermalSource> sources = new ArrayList<>(Direction.values().length);
            for (Direction direction : Direction.values()) {
                sources.add(thermalSource(level, machine.getBlockPos().relative(direction)));
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
        if (machine.electricity() != null) {
            double generated = state.thermopileFlux / 10_000.0D * 20.0D;
            if (machine.electricity().node().addEnergy(generated, false) > 0.0D) {
                state.working = true;
                machine.markChanged();
            }
        }
    }

    void tickElectricEngine(ServerLevel level) {
        if (machine.energy() == null) {
            return;
        }
        Direction output = SingleBlockMachineSupport.facing(machine).getOpposite();
        BlockEntity target = level.getBlockEntity(machine.getBlockPos().relative(output));
        if (target == null) {
            return;
        }
        target.getCapability(ForgeCapabilities.ENERGY, output.getOpposite()).ifPresent(storage -> {
            int offered = machine.energy().extractEnergy(1_000, true);
            int accepted = storage.receiveEnergy(offered, true);
            if (accepted > 0) {
                int extracted = machine.energy().extractEnergy(accepted, false);
                storage.receiveEnergy(extracted, false);
                state.working = true;
                machine.markChanged();
            }
        });
    }

    void startAirBubbleDecay(ServerLevel level) {
        for (BlockPos target : BlockPos.betweenClosed(
                machine.getBlockPos().offset(-3, -3, -3),
                machine.getBlockPos().offset(3, 3, 3)
        )) {
            if (target.distSqr(machine.getBlockPos()) <= 9.0D) {
                BlockState targetState = level.getBlockState(target);
                if (targetState.is(ModMachineBlocks.AIR_BUBBLE.get())
                        && !targetState.getValue(AirBubbleBlock.DECAYING)) {
                    level.setBlock(
                            target,
                            targetState.setValue(AirBubbleBlock.DECAYING, true),
                            Block.UPDATE_ALL
                    );
                    level.scheduleTick(target, ModMachineBlocks.AIR_BUBBLE.get(), 1 + level.random.nextInt(20));
                }
            }
        }
    }

    private ThermalSource thermalSource(ServerLevel level, BlockPos position) {
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
        if (blockEntity instanceof SingleBlockMachineBlockEntity otherMachine && otherMachine.heat() != null) {
            return source(otherMachine.heat().node());
        }
        if (blockEntity instanceof HeatPipeBlockEntity pipe) {
            return source(pipe.heat().node());
        }
        if (blockEntity instanceof HeatSinkBlockEntity sink) {
            return source(sink.heat().node());
        }
        return new ThermalSource(HeatNode.AMBIENT_TEMPERATURE_KELVIN, 1.0D);
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
