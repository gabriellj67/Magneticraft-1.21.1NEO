package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Native-electricity generation, conversion and airlock behavior.
 */
final class SingleBlockElectricalLogic {
    private static final double AIRLOCK_MIN_VOLTAGE = 60.0D;

    private final SingleBlockMachineBlockEntity machine;
    private final SingleBlockMachineState state;

    SingleBlockElectricalLogic(SingleBlockMachineBlockEntity machine, SingleBlockMachineState state) {
        this.machine = machine;
        this.state = state;
    }

    void tickInfiniteEnergy() {
        if (machine.electricity() != null) {
            clampInfiniteEnergyVoltage();
            state.working = true;
            machine.markChanged();
        }
    }

    void clampInfiniteEnergyVoltage() {
        if (machine.electricity() != null) {
            machine.electricity().node().setVoltage(125.0D);
        }
    }

    void tickAirlock(ServerLevel level) {
        if (level.getGameTime() % 40L != 0L || machine.electricity() == null) {
            return;
        }
        var node = machine.electricity().node();
        if (node.voltage() < AIRLOCK_MIN_VOLTAGE) {
            startAirBubbleDecay(level);
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
        double simulated = node.removeEnergy(plan.totalCostJoules(), true);
        if (!plan.canAfford(simulated)) {
            startAirBubbleDecay(level);
            return;
        }
        double removed = node.removeEnergy(plan.totalCostJoules(), false);
        if (!plan.canAfford(removed)) {
            node.addEnergy(removed, false);
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
        int converted = machine.electricalBridge() == null
                ? 0
                : machine.electricalBridge().lastChargeTransfer();
        state.lastProduction = converted;
        if (converted > 0) {
            state.working = true;
            machine.markChanged();
        }
        Direction output = SingleBlockMachineSupport.facing(machine).getOpposite();
        BlockPos targetPosition = machine.getBlockPos().relative(output);
        if (!level.hasChunk(targetPosition.getX() >> 4, targetPosition.getZ() >> 4)) {
            return;
        }
        BlockEntity target = level.getBlockEntity(targetPosition);
        if (target == null) {
            return;
        }
        target.getCapability(ForgeCapabilities.ENERGY, output.getOpposite()).ifPresent(storage -> {
            int offered = machine.energy().extractEnergy(machine.energy().getEnergyStored(), true);
            int accepted = storage.receiveEnergy(offered, true);
            if (accepted > 0) {
                int extracted = machine.energy().extractEnergy(accepted, false);
                int inserted = storage.receiveEnergy(extracted, false);
                if (inserted < extracted) {
                    machine.energy().receiveEnergy(extracted - inserted, false);
                }
            }
        });
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
