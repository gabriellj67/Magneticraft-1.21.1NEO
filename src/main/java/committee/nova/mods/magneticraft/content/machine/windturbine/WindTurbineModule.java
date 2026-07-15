package committee.nova.mods.magneticraft.content.machine.windturbine;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Server-authoritative wind sampling and electrical generation.
 */
public final class WindTurbineModule implements MachineModule {
    private static final String CURRENT_WIND_TAG = "current_wind";
    private static final String TARGET_WIND_TAG = "target_wind";
    private static final String ROTATION_SPEED_TAG = "rotation_speed";
    private static final String PRODUCTION_TAG = "production_joules_per_tick";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final ElectricalPowerModule energy;
    private final Supplier<Direction> facingSupplier;

    private double currentWind;
    private double targetWind;
    private double openSpace;
    private double lastProductionJoulesPerTick;
    private double clientRotationSpeed;
    private double clientProductionJoulesPerTick;
    private int ticksUntilScan;
    private boolean operational;

    public WindTurbineModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalPowerModule energy,
            Supplier<Direction> facingSupplier
    ) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.energy = Objects.requireNonNull(energy);
        this.facingSupplier = Objects.requireNonNull(facingSupplier);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag) {
        currentWind = WindTurbineMath.clamp01(tag.getDouble(CURRENT_WIND_TAG));
        targetWind = WindTurbineMath.clamp01(tag.getDouble(TARGET_WIND_TAG));
        resetEnvironmentView();
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putDouble(CURRENT_WIND_TAG, WindTurbineMath.clamp01(currentWind));
        tag.putDouble(TARGET_WIND_TAG, WindTurbineMath.clamp01(targetWind));
    }

    @Override
    public void onLoad() {
        resetEnvironmentView();
    }

    @Override
    public void saveClientData(CompoundTag tag) {
        tag.putDouble(ROTATION_SPEED_TAG, rotationSpeed());
        tag.putDouble(PRODUCTION_TAG, productionJoulesPerTick());
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        clientRotationSpeed = bound(
                tag.getDouble(ROTATION_SPEED_TAG),
                WindTurbineMath.MAX_ROTATION_SPEED
        );
        clientProductionJoulesPerTick = bound(
                tag.getDouble(PRODUCTION_TAG),
                WindTurbineMath.RATED_OUTPUT_JOULES_PER_TICK
        );
    }

    @Override
    public void serverTick() {
        Level level = host.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (ticksUntilScan > 0) {
            ticksUntilScan--;
        }
        if (ticksUntilScan == 0) {
            updateEnvironment(serverLevel);
            ticksUntilScan = WindTurbineMath.SCAN_INTERVAL_TICKS;
            host.markChanged();
        }

        currentWind = WindTurbineMath.smoothWind(currentWind, targetWind);
        lastProductionJoulesPerTick = 0.0D;
        if (operational && energy.storedJoules() < energy.ratedCapacityJoules()) {
            double requested = WindTurbineMath.productionJoulesPerTick(
                    openSpace,
                    currentWind,
                    host.position().getY()
            );
            double accepted = energy.generateJoules(requested, false);
            lastProductionJoulesPerTick = accepted;
            if (lastProductionJoulesPerTick > 0.0D) {
                host.markChanged();
            }
        }
    }

    public double currentWind() {
        return currentWind;
    }

    public double openSpace() {
        return openSpace;
    }

    public boolean operational() {
        return operational;
    }

    public double rotationSpeed() {
        Level level = host.level();
        return level != null && level.isClientSide
                ? clientRotationSpeed
                : WindTurbineMath.rotationSpeed(openSpace, currentWind, operational);
    }

    public double productionJoulesPerTick() {
        Level level = host.level();
        return level != null && level.isClientSide
                ? clientProductionJoulesPerTick
                : bound(lastProductionJoulesPerTick, WindTurbineMath.RATED_OUTPUT_JOULES_PER_TICK);
    }

    public int clientStateHash() {
        return Objects.hash(
                Double.doubleToLongBits(rotationSpeed()),
                Double.doubleToLongBits(productionJoulesPerTick())
        );
    }

    private void updateEnvironment(ServerLevel level) {
        Direction facing = facingSupplier.get();
        if (facing == null || !facing.getAxis().isHorizontal()) {
            facing = Direction.NORTH;
        }
        SpaceSample sample = sampleSpace(level, facing);
        operational = sample.operational();
        openSpace = sample.openSpace();
        BlockPos position = host.position();
        targetWind = WindTurbineMath.targetWind(
                position.getX(),
                position.getY(),
                position.getZ(),
                level.getGameTime()
        );
    }

    private SpaceSample sampleSpace(ServerLevel level, Direction facing) {
        Direction horizontal = facing.getClockWise();
        BlockPos planeCenter = host.position().relative(facing);
        int openSteps = 0;
        for (WindTurbineMath.BladeCell cell : WindTurbineMath.bladeCells()) {
            BlockPos bladePosition = planeCenter
                    .relative(horizontal, cell.horizontal())
                    .above(cell.vertical());
            if (!isLoadedWorldPosition(level, bladePosition)) {
                return SpaceSample.PAUSED;
            }
            BlockState bladeState = level.getBlockState(bladePosition);
            if (!bladeState.canBeReplaced()) {
                return SpaceSample.PAUSED;
            }
            for (int depth = 1; depth <= WindTurbineMath.CLEARANCE_DEPTH; depth++) {
                BlockPos clearancePosition = bladePosition.relative(facing, depth);
                if (!isLoadedWorldPosition(level, clearancePosition)) {
                    return SpaceSample.PAUSED;
                }
                BlockState clearanceState = level.getBlockState(clearancePosition);
                if (!clearanceState.getCollisionShape(level, clearancePosition).isEmpty()) {
                    break;
                }
                openSteps++;
            }
        }
        return new SpaceSample(true, WindTurbineMath.openSpace(openSteps));
    }

    private static boolean isLoadedWorldPosition(ServerLevel level, BlockPos position) {
        return !level.isOutsideBuildHeight(position)
                && level.hasChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    private void resetEnvironmentView() {
        openSpace = 0.0D;
        lastProductionJoulesPerTick = 0.0D;
        ticksUntilScan = 0;
        operational = false;
    }

    private static double bound(double value, double maximum) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(maximum, value));
    }

    private record SpaceSample(boolean operational, double openSpace) {
        private static final SpaceSample PAUSED = new SpaceSample(false, 0.0D);
    }
}
