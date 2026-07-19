package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.api.nuclear.radiation.RadiationSource;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyItem;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyState;
import committee.nova.mods.magneticraft.content.nuclear.structure.NuclearStructureState;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.init.ModNuclearItems;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearDataRegistry;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutEstimate;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutSimulator;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorRuntimeModel;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorRuntimeResult;
import committee.nova.mods.magneticraft.system.nuclear.thermal.NuclearThermalTransactions;
import committee.nova.mods.magneticraft.system.nuclear.safety.ReactorAccidentModel;
import committee.nova.mods.magneticraft.system.nuclear.safety.NuclearTerrainDamage;
import committee.nova.mods.magneticraft.system.nuclear.radiation.RadiationSourceRegistry;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Owns structure, permissions, controls and all durable per-column PWR runtime state. */
public final class NuclearReactorControllerBlockEntity extends MachineBlockEntity implements MenuProvider, RadiationSource {
    private static final int STRUCTURE_CHECK_INTERVAL = 40;
    private static final int RUNTIME_SCHEMA_VERSION = 2;
    private static final int OVERRIDE_CONFIRMATION_TICKS = 200;
    private static final String OWNER_TAG = "owner";
    private static final String SNAPSHOT_TAG = "reactor_snapshot";
    private static final String RUNTIME_TAG = "reactor_runtime";
    private static final String RENDER_WIDTH_TAG = "render_width";
    private static final String RENDER_LENGTH_TAG = "render_length";
    private static final String RENDER_HEIGHT_TAG = "render_height";

    @Nullable
    private UUID owner;
    @Nullable
    private NuclearReactorSnapshot snapshot;
    private ReactorLayoutEstimate estimate = ReactorLayoutEstimate.empty("unformed");
    private int renderWidth;
    private int renderLength;
    private int renderHeight;
    private long estimateGeneration = Long.MIN_VALUE;
    private final Map<ReactorColumnCoordinate, FuelAssemblyState> fuelStates = new LinkedHashMap<>();
    private final EnumMap<ReactorRodGroup, Double> rodInsertion = ReactorRuntimeModel.fullyInsertedRods();
    private ReactorRuntimeResult runtime = ReactorRuntimeResult.empty(Map.of());
    private ReactorOperatingState operatingState = ReactorOperatingState.SHUTDOWN;
    private ReactorControlMode controlMode = ReactorControlMode.POWER;
    private ReactorAutomationLevel automationLevel = ReactorAutomationLevel.PROTECTION;
    private double targetPowerFraction = 1.0D;
    private boolean engineeringOverride;
    @Nullable
    private UUID overrideRequester;
    private long overrideConfirmationDeadline;
    @Nullable
    private UUID lastOverrideOperator;
    private long lastOverrideGameTime;
    private int startupTicksRemaining;
    private long lastRuntimeGameTime;
    private double reportedCoolantFlow;
    private long coolantFlowReportTick = Long.MIN_VALUE;
    private boolean stationPowerAvailable;
    private int stationJoulesRequired;
    private int stationJoulesAvailable;
    private String scramReason = "none";
    private ReactorAccidentStage accidentStage = ReactorAccidentStage.NORMAL;
    private String accidentReason = "stable";
    private double corePressureMegapascals = 15.5D;
    private double vesselIntegrity = 1.0D;
    private double containmentIntegrity = 1.0D;
    private double accidentEnergyJoules;
    private long lastAccidentTransitionGameTime;
    private boolean terrainDamageApplied;
    @Nullable
    private CompoundTag rejectedRuntimeTag;

    public NuclearReactorControllerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.NUCLEAR_REACTOR_CONTROLLER.get(), position, state);
    }

    public static void serverTick(
            Level level, BlockPos position, BlockState state, NuclearReactorControllerBlockEntity controller
    ) {
        controller.tickModules();
        if (controller.snapshot != null && level.getGameTime() % STRUCTURE_CHECK_INTERVAL == 0L) {
            controller.revalidate();
            controller.refreshEstimateIfDataChanged();
        }
        controller.tickRuntime();
        controller.finishServerTick();
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(NuclearReactorControllerBlock.FACING)
                ? state.getValue(NuclearReactorControllerBlock.FACING) : Direction.NORTH;
    }

    public boolean formed() {
        return snapshot != null;
    }

    public int renderWidth() {
        return renderWidth;
    }

    public int renderLength() {
        return renderLength;
    }

    public int renderHeight() {
        return renderHeight;
    }

    public Optional<NuclearReactorSnapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public ReactorLayoutEstimate estimate() {
        return estimate;
    }

    public ReactorRuntimeResult runtime() {
        return runtime;
    }

    public ReactorOperatingState operatingState() {
        return operatingState;
    }

    public ReactorControlMode controlMode() {
        return controlMode;
    }

    public ReactorAutomationLevel automationLevel() {
        return automationLevel;
    }

    public double targetPowerFraction() {
        return targetPowerFraction;
    }

    public double rodInsertion(ReactorRodGroup group) {
        return rodInsertion.getOrDefault(group, 1.0D);
    }

    public Map<ReactorColumnCoordinate, FuelAssemblyState> fuelStates() {
        return Map.copyOf(fuelStates);
    }

    public Optional<FuelAssemblyState> fuelState(ReactorColumnCoordinate coordinate) {
        return Optional.ofNullable(fuelStates.get(coordinate));
    }

    public boolean engineeringOverride() {
        return engineeringOverride;
    }

    public boolean stationPowerAvailable() {
        return stationPowerAvailable;
    }

    public int stationJoulesRequired() {
        return stationJoulesRequired;
    }

    public int stationJoulesAvailable() {
        return stationJoulesAvailable;
    }

    public double actualCoolantFlow() {
        return level != null && level.getGameTime() - coolantFlowReportTick <= 2L
                ? reportedCoolantFlow : 0.0D;
    }

    public String scramReason() {
        return scramReason;
    }

    public ReactorAccidentStage accidentStage() { return accidentStage; }
    public String accidentReason() { return accidentReason; }
    public double corePressureMegapascals() { return corePressureMegapascals; }
    public double vesselIntegrity() { return vesselIntegrity; }
    public double containmentIntegrity() { return containmentIntegrity; }
    public double accidentEnergyJoules() { return accidentEnergyJoules; }

    public Optional<UUID> lastOverrideOperator() {
        return Optional.ofNullable(lastOverrideOperator);
    }

    public long lastOverrideGameTime() {
        return lastOverrideGameTime;
    }

    public EnumSet<ReactorInterlock> interlocks() {
        EnumSet<ReactorInterlock> result = EnumSet.noneOf(ReactorInterlock.class);
        if (snapshot == null) {
            result.add(ReactorInterlock.STRUCTURE);
        }
        if (fuelStates.isEmpty()) {
            result.add(ReactorInterlock.FUEL);
        }
        if (!stationPowerAvailable) {
            result.add(ReactorInterlock.STATION_POWER);
        }
        if (actualCoolantFlow() + 1.0E-9D < estimate.requiredCoolantFlowMilliBucketsPerTick()
                * parameters().minimumCoolantFraction()) {
            result.add(ReactorInterlock.COOLANT_FLOW);
        }
        if (automationLevel == ReactorAutomationLevel.NONE) {
            result.add(ReactorInterlock.INSTRUMENTATION);
        }
        if (runtime.minimumCladdingIntegrity() <= 0.2D) {
            result.add(ReactorInterlock.CLADDING);
        }
        if (runtime.hottestTemperatureKelvin() >= parameters().forcedScramTemperatureKelvin()) {
            result.add(ReactorInterlock.OVER_TEMPERATURE);
        }
        if (rejectedRuntimeTag != null) {
            result.add(ReactorInterlock.RUNTIME_DATA);
        }
        return result;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markChanged();
    }

    public boolean canManage(Player player) {
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D
        ) <= 64.0D && player.getAbilities().mayBuild
                && (owner == null || owner.equals(player.getUUID())
                || player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2));
    }

    public boolean tryForm(ServerPlayer player) {
        NuclearReactorStructure.Result result = validateAny();
        if (result.snapshot().isEmpty()) {
            invalidMessage(player, result.reason(), result.position());
            return false;
        }
        NuclearReactorSnapshot next = result.snapshot().orElseThrow();
        if (!loadedFuelMatches(next)) {
            invalidMessage(player, "loaded_fuel_layout_mismatch", worldPosition);
            return false;
        }
        if (owner == null) {
            owner = player.getUUID();
        }
        applySnapshot(next);
        return true;
    }

    public void unform() {
        NuclearReactorSnapshot current = snapshot;
        if (current != null) {
            releasePorts(current);
            if (level instanceof ServerLevel serverLevel) {
                NuclearStructureState.setFormed(serverLevel, current.members(), false);
            }
        }
        snapshot = null;
        setRenderDimensions(null);
        estimate = ReactorLayoutEstimate.empty("unformed");
        estimateGeneration = Long.MIN_VALUE;
        forcedScram("structure");
        updateFormedState(false);
        markChangedAndSync();
    }

    public void reportCoolantFlow(double milliBucketsPerTick) {
        if (level == null || level.isClientSide || !Double.isFinite(milliBucketsPerTick)) {
            return;
        }
        reportedCoolantFlow = Math.max(0.0D, milliBucketsPerTick);
        coolantFlowReportTick = level.getGameTime();
    }

    public boolean applyAction(ServerPlayer player, NuclearReactorAction action) {
        if (!canManage(player) || action == null || rejectedRuntimeTag != null) {
            return false;
        }
        boolean changed = switch (action.type()) {
            case SET_ROD_GROUP -> setRodGroup(action.group(), action.value() / 1000.0D);
            case SCRAM -> forcedScram("manual");
            case RESET -> resetScram();
            case START -> start();
            case STOP -> stop();
            case SET_MODE -> setControlMode(action.mode());
            case SET_TARGET_POWER -> setTargetPower(action.value() / 1000.0D);
            case SET_OVERRIDE -> setEngineeringOverride(player, action.value() != 0, action.confirmed());
            case LOAD_FUEL_FROM_HAND -> loadFuel(player, action.coordinate());
            case UNLOAD_FUEL -> unloadFuel(player, action.coordinate());
            case INSTALL_UPGRADE -> installUpgrade(player);
        };
        if (changed) {
            markChangedAndSync();
        }
        return changed;
    }

    public void writeMenuData(FriendlyByteBuf buffer) {
        NuclearReactorMenu.write(buffer, worldPosition, snapshot, estimate);
    }

    public void dropLoadedFuel() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (FuelAssemblyState state : fuelStates.values()) {
            ItemStack stack = itemFor(state);
            if (!stack.isEmpty()) {
                level.addFreshEntity(new ItemEntity(level,
                        worldPosition.getX() + 0.5D, worldPosition.getY() + 0.8D,
                        worldPosition.getZ() + 0.5D, stack));
            }
        }
        fuelStates.clear();
        runtime = ReactorRuntimeResult.empty(Map.of());
        markChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magneticraft.pressurized_water_reactor_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return canManage(player) && snapshot != null
                ? new NuclearReactorMenu(containerId, inventory, this) : null;
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
        }
        if (snapshot != null) {
            tag.put(SNAPSHOT_TAG, NuclearReactorSnapshotCodec.save(snapshot));
        }
        tag.put(RUNTIME_TAG, saveRuntime());
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        snapshot = tag.contains(SNAPSHOT_TAG, Tag.TAG_COMPOUND)
                ? NuclearReactorSnapshotCodec.load(tag.getCompound(SNAPSHOT_TAG), worldPosition).orElse(null)
                : null;
        estimate = ReactorLayoutEstimate.empty(snapshot == null ? "unformed" : "awaiting_nuclear_data");
        setRenderDimensions(snapshot);
        estimateGeneration = Long.MIN_VALUE;
        loadRuntime(tag.getCompound(RUNTIME_TAG));
    }

    @Override
    protected void saveClientData(CompoundTag tag) {
        super.saveClientData(tag);
        if (renderWidth > 0 && renderLength > 0 && renderHeight > 0) {
            tag.putInt(RENDER_WIDTH_TAG, renderWidth);
            tag.putInt(RENDER_LENGTH_TAG, renderLength);
            tag.putInt(RENDER_HEIGHT_TAG, renderHeight);
        }
    }

    @Override
    protected void loadClientData(CompoundTag tag) {
        super.loadClientData(tag);
        int width = tag.getInt(RENDER_WIDTH_TAG);
        int length = tag.getInt(RENDER_LENGTH_TAG);
        int height = tag.getInt(RENDER_HEIGHT_TAG);
        if (NuclearReactorStructure.DESCRIPTOR.accepts(width, length, height)) {
            renderWidth = width;
            renderLength = length;
            renderHeight = height;
        } else {
            setRenderDimensions(null);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }
        RadiationSourceRegistry.register((ServerLevel) level, worldPosition);
        if (snapshot != null) {
            NuclearStructureState.setFormed((ServerLevel) level, snapshot.members(), true);
            refreshEstimate();
            claimPorts(snapshot);
        }
        long now = level.getGameTime();
        if (lastRuntimeGameTime > 0L && now > lastRuntimeGameTime && !fuelStates.isEmpty()) {
            NuclearDataRegistry.INSTANCE.current().ifPresent(data -> {
                runtime = ReactorRuntimeModel.catchUpDecay(
                        fuelStates, data.fuelDefinitions(), now - lastRuntimeGameTime, now, parameters());
                replaceFuelStates(runtime.fuelStates());
            });
            if (operatingState.producesFissionHeat()) {
                forcedScram("chunk_unload");
            }
            catchUpAccident(now - lastRuntimeGameTime);
        }
        lastRuntimeGameTime = now;
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            RadiationSourceRegistry.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }

    private void tickRuntime() {
        if (level == null || level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        updatePrimaryCoolantFlow();
        stationJoulesRequired = snapshot == null ? 0 : parameters().stationInstrumentationJoulesPerTick()
                + (operatingState.producesFissionHeat() ? parameters().stationRunningJoulesPerTick() : 0);
        NuclearReactorPortBlockEntity electricalPort = electricalPort();
        stationJoulesAvailable = electricalPort == null ? 0 : electricalPort.storedJoules();
        stationPowerAvailable = stationJoulesRequired == 0 || electricalPort != null
                && electricalPort.consumeJoules(stationJoulesRequired, true) == stationJoulesRequired;
        if (stationPowerAvailable && stationJoulesRequired > 0) {
            electricalPort.consumeJoules(stationJoulesRequired, false);
        }
        if (operatingState.producesFissionHeat() && !stationPowerAvailable) {
            forcedScram("station_power");
        }
        if (operatingState.producesFissionHeat() && snapshot == null) {
            forcedScram("structure");
        }
        applyAutomaticControl();
        if (operatingState == ReactorOperatingState.STARTUP && startupTicksRemaining > 0) {
            startupTicksRemaining--;
            if (startupTicksRemaining == 0) {
                operatingState = ReactorOperatingState.RUNNING;
            }
        }
        updateFuelRuntime(now);
        updateAccidentState();
        EnumSet<ReactorInterlock> active = interlocks();
        if (operatingState.producesFissionHeat()) {
            if (active.contains(ReactorInterlock.OVER_TEMPERATURE)) {
                forcedScram("over_temperature");
            } else if (active.contains(ReactorInterlock.CLADDING)) {
                forcedScram("cladding");
            } else if (automationLevel.supports(ReactorAutomationLevel.PROTECTION)
                    && active.stream().anyMatch(interlock -> !engineeringOverride || !interlock.overrideAllowed())) {
                forcedScram("protection_interlock");
            }
        }
        if (operatingState == ReactorOperatingState.DECAY_HEAT
                && runtime.decayHeatJoulesPerTick() < 1.0D
                && runtime.hottestTemperatureKelvin() <= parameters().safeUnloadTemperatureKelvin()) {
            operatingState = ReactorOperatingState.SHUTDOWN;
        }
        lastRuntimeGameTime = now;
        if (!fuelStates.isEmpty() || operatingState != ReactorOperatingState.SHUTDOWN) {
            markChanged();
        }
    }

    private void updatePrimaryCoolantFlow() {
        NuclearReactorPortBlockEntity input = port(NuclearReactorPortType.COOLANT_INPUT);
        NuclearReactorPortBlockEntity output = port(NuclearReactorPortType.COOLANT_OUTPUT);
        if (input == null || output == null) {
            reportCoolantFlow(0.0D);
            return;
        }
        int requested = (int) Math.ceil(Math.max(0.0D,
                estimate.requiredCoolantFlowMilliBucketsPerTick() * 1.5D));
        var transfer = NuclearThermalTransactions.stateConversion(
                input.coolantAmount(FluidDefinition.COLD_REACTOR_COOLANT),
                output.coolantSpace(FluidDefinition.HOT_REACTOR_COOLANT),
                requested
        );
        reportCoolantFlow(transfer.consumed());
        if (transfer.consumed() <= 0 || !(operatingState.producesFissionHeat()
                || runtime.decayHeatJoulesPerTick() > 0.0D)) {
            return;
        }
        int drained = input.drainCoolant(FluidDefinition.COLD_REACTOR_COOLANT, transfer.consumed());
        int filled = output.fillCoolant(FluidDefinition.HOT_REACTOR_COOLANT, drained);
        if (filled != drained) {
            throw new IllegalStateException("Primary coolant transaction lost fluid after successful simulation");
        }
    }

    private void updateFuelRuntime(long now) {
        if (fuelStates.isEmpty()) {
            runtime = ReactorRuntimeResult.empty(Map.of());
            return;
        }
        NuclearReactorSnapshot current = snapshot;
        Optional<committee.nova.mods.magneticraft.system.nuclear.data.NuclearDataSnapshot> data =
                NuclearDataRegistry.INSTANCE.current();
        if (current == null || data.isEmpty()) {
            if (data.isPresent()) {
                runtime = ReactorRuntimeModel.catchUpDecay(
                        fuelStates, data.orElseThrow().fuelDefinitions(), 1L, now, parameters());
                replaceFuelStates(runtime.fuelStates());
            }
            return;
        }
        double command = operatingState == ReactorOperatingState.STARTUP
                ? targetPowerFraction * (1.0D - startupTicksRemaining
                / (double) Math.max(1, parameters().startupTicks())) : targetPowerFraction;
        runtime = ReactorRuntimeModel.step(
                current, estimate, fuelStates, data.orElseThrow().fuelDefinitions(), rodInsertion,
                command, actualCoolantFlow(), operatingState.producesFissionHeat(), now, parameters());
        replaceFuelStates(runtime.fuelStates());
    }

    private void updateAccidentState() {
        if (fuelStates.isEmpty()) {
            if (accidentStage.ordinal() < ReactorAccidentStage.CLADDING_DAMAGE.ordinal()) {
                resetAccidentState();
            }
            return;
        }
        ReactorAccidentStage previous = accidentStage;
        double required = Math.max(1.0D, estimate.requiredCoolantFlowMilliBucketsPerTick());
        ReactorAccidentModel.Result result = ReactorAccidentModel.step(new ReactorAccidentModel.Input(
                accidentStage, runtime.totalThermalPowerJoulesPerTick(), actualCoolantFlow() / required,
                runtime.hottestTemperatureKelvin(), runtime.minimumCladdingIntegrity(),
                corePressureMegapascals, vesselIntegrity, containmentIntegrity, accidentEnergyJoules), parameters());
        applyAccidentResult(result);
        if (accidentStage != previous) {
            lastAccidentTransitionGameTime = level == null ? 0L : level.getGameTime();
            emitAccidentFeedback(previous, accidentStage);
            markChangedAndSync();
        }
        if (accidentStage.ordinal() >= ReactorAccidentStage.LOCAL_BOILING.ordinal()
                && operatingState.producesFissionHeat()) {
            forcedScram("accident");
        }
        if (accidentStage == ReactorAccidentStage.CONTAINMENT_BREACH && !terrainDamageApplied
                && level instanceof ServerLevel serverLevel) {
            terrainDamageApplied = true;
            NuclearTerrainDamage.apply(serverLevel, worldPosition, accidentEnergyJoules,
                    containmentIntegrity, parameters());
            BlockPos coriumPosition = worldPosition.relative(facing().getOpposite()).below();
            if (serverLevel.hasChunkAt(coriumPosition)
                    && serverLevel.getBlockEntity(coriumPosition) == null
                    && serverLevel.getBlockState(coriumPosition).getDestroySpeed(serverLevel, coriumPosition) >= 0.0F) {
                serverLevel.setBlockAndUpdate(coriumPosition, ModNuclearBlocks.CORIUM.get().defaultBlockState());
            }
        }
    }

    private void catchUpAccident(long elapsedTicks) {
        long ticks = Math.min(Math.max(0L, elapsedTicks), parameters().maximumOfflineCatchupTicks());
        if (ticks == 0L || fuelStates.isEmpty()) return;
        double required = Math.max(1.0D, estimate.requiredCoolantFlowMilliBucketsPerTick());
        for (long tick = 0; tick < ticks; tick++) {
            ReactorAccidentModel.Result result = ReactorAccidentModel.step(new ReactorAccidentModel.Input(
                    accidentStage, runtime.decayHeatJoulesPerTick(), 0.0D / required,
                    runtime.hottestTemperatureKelvin(), runtime.minimumCladdingIntegrity(),
                    corePressureMegapascals, vesselIntegrity, containmentIntegrity, accidentEnergyJoules), parameters());
            applyAccidentResult(result);
            if (accidentStage == ReactorAccidentStage.CONTAINMENT_BREACH) break;
        }
    }

    private void applyAccidentResult(ReactorAccidentModel.Result result) {
        accidentStage = result.stage();
        accidentReason = result.reason();
        corePressureMegapascals = result.pressureMegapascals();
        vesselIntegrity = result.vesselIntegrity();
        containmentIntegrity = result.containmentIntegrity();
        accidentEnergyJoules = result.accidentEnergyJoules();
    }

    private void emitAccidentFeedback(ReactorAccidentStage previous, ReactorAccidentStage current) {
        if (!(level instanceof ServerLevel serverLevel) || current == ReactorAccidentStage.NORMAL
                || current.ordinal() <= previous.ordinal()) {
            return;
        }
        boolean severe = current.severe();
        serverLevel.sendParticles(
                severe ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 1.5D,
                worldPosition.getZ() + 0.5D,
                severe ? 40 : 12,
                severe ? 1.5D : 0.6D,
                severe ? 1.0D : 0.4D,
                severe ? 1.5D : 0.6D,
                0.02D
        );
        if (current.ordinal() >= ReactorAccidentStage.VESSEL_BREACH.ordinal()) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D,
                    worldPosition.getZ() + 0.5D, 30, 1.2D, 0.8D, 1.2D, 0.05D);
        }
        if (severe) {
            serverLevel.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS, 1.5F, 0.7F);
        } else {
            serverLevel.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(),
                    SoundSource.BLOCKS, 1.0F, 1.5F);
        }
    }

    private void resetAccidentState() {
        accidentStage = ReactorAccidentStage.NORMAL;
        accidentReason = "stable";
        corePressureMegapascals = 15.5D;
        vesselIntegrity = 1.0D;
        containmentIntegrity = 1.0D;
        accidentEnergyJoules = 0.0D;
        lastAccidentTransitionGameTime = 0L;
        terrainDamageApplied = false;
    }

    @Override
    public BlockPos radiationOrigin() {
        return worldPosition;
    }

    @Override
    public double doseRateMillisievertsPerHour() {
        ReactorParameters p = parameters();
        double dose = fuelStates.values().stream().mapToDouble(state ->
                p.freshFuelDoseRateMillisievertsPerHour()
                        + p.spentFuelDoseRateMillisievertsPerHour() * state.burnupFraction()
                        * (1.0D + state.decayHeatJoules() / 10.0D)).sum();
        if (accidentStage.severe()) dose += p.coriumDoseRateMillisievertsPerHour()
                * (0.25D + accidentStage.ordinal() * 0.1D);
        return dose;
    }

    @Override
    public boolean contaminationSource() {
        return accidentStage.releasesContamination();
    }

    private void applyAutomaticControl() {
        if (level == null || !operatingState.producesFissionHeat()
                || controlMode == ReactorControlMode.MANUAL
                || !automationLevel.supports(controlMode.requiredLevel())) {
            return;
        }
        double requested = switch (controlMode) {
            case MANUAL, POWER -> targetPowerFraction;
            case TEMPERATURE -> runtime.hottestTemperatureKelvin()
                    > parameters().forcedScramTemperatureKelvin() * 0.85D ? 0.25D : targetPowerFraction;
            case LOAD_FOLLOWING -> level.getBestNeighborSignal(worldPosition) / 15.0D;
        };
        double available = Math.max(1.0D, estimate.powerDensityJoulesPerTick()
                * Math.max(1, estimate.columns().size()));
        double actual = runtime.fissionPowerJoulesPerTick() / available;
        double direction = actual < requested ? -1.0D : 1.0D;
        double step = parameters().automaticRodStepPerTick() * direction;
        for (ReactorRodGroup group : ReactorRodGroup.values()) {
            rodInsertion.put(group, clamp(rodInsertion(group) + step, 0.0D, 1.0D));
        }
    }

    private boolean start() {
        if (operatingState != ReactorOperatingState.SHUTDOWN || snapshot == null || fuelStates.isEmpty()) {
            return false;
        }
        EnumSet<ReactorInterlock> active = interlocks();
        if (active.stream().anyMatch(interlock -> !engineeringOverride || !interlock.overrideAllowed())) {
            return false;
        }
        operatingState = ReactorOperatingState.STARTUP;
        startupTicksRemaining = parameters().startupTicks();
        scramReason = "none";
        return true;
    }

    private boolean stop() {
        if (!operatingState.producesFissionHeat()) {
            return false;
        }
        insertAllRods();
        operatingState = ReactorOperatingState.DECAY_HEAT;
        return true;
    }

    private boolean forcedScram(String reason) {
        boolean changed = operatingState != ReactorOperatingState.SCRAMMED || !reason.equals(scramReason);
        insertAllRods();
        operatingState = ReactorOperatingState.SCRAMMED;
        startupTicksRemaining = 0;
        scramReason = reason;
        engineeringOverride = false;
        overrideRequester = null;
        return changed;
    }

    private boolean resetScram() {
        if (operatingState != ReactorOperatingState.SCRAMMED) {
            return false;
        }
        EnumSet<ReactorInterlock> active = interlocks();
        if (active.contains(ReactorInterlock.STRUCTURE)
                || active.contains(ReactorInterlock.STATION_POWER)
                || active.contains(ReactorInterlock.CLADDING)
                || active.contains(ReactorInterlock.OVER_TEMPERATURE)) {
            return false;
        }
        operatingState = runtime.decayHeatJoulesPerTick() > 1.0D
                ? ReactorOperatingState.DECAY_HEAT : ReactorOperatingState.SHUTDOWN;
        scramReason = "none";
        engineeringOverride = false;
        return true;
    }

    private boolean setRodGroup(@Nullable ReactorRodGroup group, double insertion) {
        if (group == null || operatingState == ReactorOperatingState.SCRAMMED) {
            return false;
        }
        double next = clamp(insertion, 0.0D, 1.0D);
        if (rodInsertion(group) == next) {
            return false;
        }
        rodInsertion.put(group, next);
        return true;
    }

    private boolean setControlMode(@Nullable ReactorControlMode mode) {
        if (mode == null || !automationLevel.supports(mode.requiredLevel()) || mode == controlMode) {
            return false;
        }
        controlMode = mode;
        return true;
    }

    private boolean setTargetPower(double fraction) {
        double next = clamp(fraction, 0.0D, 1.0D);
        if (next == targetPowerFraction) {
            return false;
        }
        targetPowerFraction = next;
        return true;
    }

    private boolean setEngineeringOverride(ServerPlayer player, boolean enabled, boolean confirmed) {
        if (!enabled) {
            boolean changed = engineeringOverride || overrideRequester != null;
            engineeringOverride = false;
            overrideRequester = null;
            return changed;
        }
        long now = level == null ? 0L : level.getGameTime();
        if (!confirmed) {
            overrideRequester = player.getUUID();
            overrideConfirmationDeadline = now + OVERRIDE_CONFIRMATION_TICKS;
            return true;
        }
        if (!player.getUUID().equals(overrideRequester) || now > overrideConfirmationDeadline) {
            return false;
        }
        overrideRequester = null;
        engineeringOverride = true;
        lastOverrideOperator = player.getUUID();
        lastOverrideGameTime = now;
        return true;
    }

    private boolean loadFuel(ServerPlayer player, @Nullable ReactorColumnCoordinate coordinate) {
        NuclearReactorSnapshot current = snapshot;
        if (current == null || coordinate == null || fuelStates.containsKey(coordinate)) {
            return false;
        }
        NuclearReactorColumnType type = current.columns().get(coordinate);
        ItemStack held = player.getMainHandItem();
        if (type == null || !type.isFuel() || !(held.getItem() instanceof FuelAssemblyItem item)
                || item.grade() != type.fuelGrade()) {
            return false;
        }
        Optional<FuelAssemblyState> state = item.state(held);
        if (state.isEmpty()) {
            return false;
        }
        FuelAssemblyState value = state.orElseThrow();
        fuelStates.put(coordinate, new FuelAssemblyState(
                value.fuelId(), value.burnupFraction(), value.poisonFraction(), value.decayHeatJoules(),
                value.temperatureKelvin(), value.claddingIntegrity(),
                level == null ? value.lastUpdateGameTime() : level.getGameTime()));
        held.shrink(1);
        return true;
    }

    private boolean unloadFuel(ServerPlayer player, @Nullable ReactorColumnCoordinate coordinate) {
        if (coordinate == null || operatingState.producesFissionHeat()) {
            return false;
        }
        FuelAssemblyState state = fuelStates.get(coordinate);
        if (state == null || state.temperatureKelvin() > parameters().safeUnloadTemperatureKelvin()) {
            return false;
        }
        fuelStates.remove(coordinate);
        ItemStack stack = itemFor(state);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        return true;
    }

    private boolean installUpgrade(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        for (NuclearControllerUpgrade upgrade : NuclearControllerUpgrade.values()) {
            if (held.is(ModNuclearItems.controllerUpgrade(upgrade).get())
                    && upgrade.level().ordinal() > automationLevel.ordinal()) {
                automationLevel = upgrade.level();
                held.shrink(1);
                return true;
            }
        }
        return false;
    }

    private ItemStack itemFor(FuelAssemblyState state) {
        for (var entry : ModNuclearItems.fuelAssemblies().entrySet()) {
            if (entry.getKey().definitionId().equals(state.fuelId())) {
                ItemStack stack = new ItemStack(entry.getValue().get());
                ((FuelAssemblyItem) stack.getItem()).writeState(stack, state);
                return stack;
            }
        }
        Magneticraft.LOGGER.error("Cannot serialize unknown loaded fuel {}", state.fuelId());
        return ItemStack.EMPTY;
    }

    private void insertAllRods() {
        for (ReactorRodGroup group : ReactorRodGroup.values()) {
            rodInsertion.put(group, 1.0D);
        }
    }

    private boolean loadedFuelMatches(NuclearReactorSnapshot candidate) {
        for (Map.Entry<ReactorColumnCoordinate, FuelAssemblyState> entry : fuelStates.entrySet()) {
            NuclearReactorColumnType type = candidate.columns().get(entry.getKey());
            if (type == null || !type.isFuel()
                    || !type.fuelGrade().definitionId().equals(entry.getValue().fuelId())) {
                return false;
            }
        }
        return true;
    }

    private void replaceFuelStates(Map<ReactorColumnCoordinate, FuelAssemblyState> next) {
        fuelStates.clear();
        fuelStates.putAll(next);
    }

    private ReactorParameters parameters() {
        return ReactorParameterRegistry.INSTANCE.current().parameters();
    }

    private void revalidate() {
        NuclearReactorSnapshot current = snapshot;
        if (current == null) {
            return;
        }
        NuclearReactorStructure.Result result = NuclearReactorStructure.validateExact(
                worldPosition, facing(), current.width(), current.length(), current.height(), worldLookup());
        if (result.snapshot().isEmpty()) {
            unform();
            return;
        }
        NuclearReactorSnapshot valid = result.snapshot().orElseThrow();
        if (!valid.columns().equals(current.columns()) || valid.facing() != current.facing()) {
            if (loadedFuelMatches(valid)) {
                applySnapshot(valid);
            } else {
                unform();
            }
        }
    }

    private NuclearReactorStructure.Result validateAny() {
        return NuclearReactorStructure.validate(worldPosition, facing(), worldLookup());
    }

    private NuclearReactorStructure.PartLookup worldLookup() {
        return new NuclearReactorStructure.PartLookup() {
            @Nullable
            @Override
            public NuclearReactorStructure.ObservedPart partAt(BlockPos position) {
                return level == null ? null : observe(level.getBlockState(position));
            }

            @Override
            public boolean isLoaded(BlockPos position) {
                return level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(position);
            }
        };
    }

    private void applySnapshot(NuclearReactorSnapshot next) {
        NuclearReactorSnapshot previous = snapshot;
        if (previous != null) {
            releasePorts(previous);
            if (level instanceof ServerLevel serverLevel && !previous.members().equals(next.members())) {
                NuclearStructureState.setFormed(serverLevel, previous.members(), false);
            }
        }
        snapshot = next;
        setRenderDimensions(next);
        refreshEstimate();
        claimPorts(next);
        if (level instanceof ServerLevel serverLevel) {
            NuclearStructureState.setFormed(serverLevel, next.members(), true);
        }
        updateFormedState(true);
        markChangedAndSync();
    }

    private void setRenderDimensions(@Nullable NuclearReactorSnapshot value) {
        renderWidth = value == null ? 0 : value.width();
        renderLength = value == null ? 0 : value.length();
        renderHeight = value == null ? 0 : value.height();
    }

    private void claimPorts(NuclearReactorSnapshot value) {
        if (level == null) {
            return;
        }
        value.ports().forEach((role, position) -> {
            if (level.getBlockEntity(position) instanceof NuclearReactorPortBlockEntity port) {
                port.claim(worldPosition, role);
            }
        });
    }

    private void releasePorts(NuclearReactorSnapshot value) {
        if (level == null) {
            return;
        }
        value.ports().values().forEach(position -> {
            if (level.getBlockEntity(position) instanceof NuclearReactorPortBlockEntity port) {
                port.release(worldPosition);
            }
        });
    }

    @Nullable
    private NuclearReactorPortBlockEntity electricalPort() {
        NuclearReactorSnapshot current = snapshot;
        if (level == null || current == null) {
            return null;
        }
        BlockPos position = current.ports().get(NuclearReactorPortType.ELECTRICAL);
        return position != null && level.getBlockEntity(position) instanceof NuclearReactorPortBlockEntity port
                && port.claimedBy(worldPosition) ? port : null;
    }

    @Nullable
    private NuclearReactorPortBlockEntity port(NuclearReactorPortType role) {
        NuclearReactorSnapshot current = snapshot;
        if (level == null || current == null) {
            return null;
        }
        BlockPos position = current.ports().get(role);
        return position != null && level.getBlockEntity(position) instanceof NuclearReactorPortBlockEntity port
                && port.claimedBy(worldPosition) && port.claimedRole() == role ? port : null;
    }

    private void refreshEstimate() {
        NuclearReactorSnapshot current = snapshot;
        if (current == null) {
            estimate = ReactorLayoutEstimate.empty("unformed");
            estimateGeneration = Long.MIN_VALUE;
            return;
        }
        NuclearDataRegistry.INSTANCE.current().ifPresentOrElse(data -> {
            estimate = ReactorLayoutSimulator.estimate(current, data.fuelDefinitions());
            estimateGeneration = data.generation();
        }, () -> {
            estimate = ReactorLayoutEstimate.empty("nuclear_data_unavailable");
            estimateGeneration = Long.MIN_VALUE;
        });
    }

    private void refreshEstimateIfDataChanged() {
        long currentGeneration = NuclearDataRegistry.INSTANCE.current()
                .map(data -> data.generation()).orElse(Long.MIN_VALUE);
        if (currentGeneration != estimateGeneration) {
            refreshEstimate();
            markChangedAndSync();
        }
    }

    private CompoundTag saveRuntime() {
        if (rejectedRuntimeTag != null) {
            return rejectedRuntimeTag.copy();
        }
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema_version", RUNTIME_SCHEMA_VERSION);
        tag.putString("operating_state", operatingState.name());
        tag.putString("control_mode", controlMode.name());
        tag.putString("automation_level", automationLevel.name());
        tag.putDouble("target_power_fraction", targetPowerFraction);
        tag.putBoolean("engineering_override", engineeringOverride);
        if (lastOverrideOperator != null) {
            tag.putUUID("last_override_operator", lastOverrideOperator);
        }
        tag.putLong("last_override_game_time", lastOverrideGameTime);
        tag.putInt("startup_ticks_remaining", startupTicksRemaining);
        tag.putLong("last_runtime_game_time", lastRuntimeGameTime);
        tag.putString("scram_reason", scramReason);
        tag.putString("accident_stage", accidentStage.name());
        tag.putString("accident_reason", accidentReason);
        tag.putDouble("core_pressure_megapascals", corePressureMegapascals);
        tag.putDouble("vessel_integrity", vesselIntegrity);
        tag.putDouble("containment_integrity", containmentIntegrity);
        tag.putDouble("accident_energy_joules", accidentEnergyJoules);
        tag.putLong("last_accident_transition_game_time", lastAccidentTransitionGameTime);
        tag.putBoolean("terrain_damage_applied", terrainDamageApplied);
        CompoundTag rods = new CompoundTag();
        for (ReactorRodGroup group : ReactorRodGroup.values()) {
            rods.putDouble(group.name(), rodInsertion(group));
        }
        tag.put("rod_insertion", rods);
        ListTag fuels = new ListTag();
        fuelStates.forEach((coordinate, state) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("x", coordinate.x());
            entry.putInt("z", coordinate.z());
            entry.put("state", state.save());
            fuels.add(entry);
        });
        tag.put("fuel_columns", fuels);
        return tag;
    }

    private void loadRuntime(CompoundTag tag) {
        rejectedRuntimeTag = null;
        fuelStates.clear();
        insertAllRods();
        runtime = ReactorRuntimeResult.empty(Map.of());
        resetRuntimeDefaults();
        if (tag.isEmpty()) {
            return;
        }
        int schema = tag.getInt("schema_version");
        if (schema != 1 && schema != RUNTIME_SCHEMA_VERSION) {
            rejectRuntime(tag, "runtime_schema", null);
            return;
        }
        try {
            operatingState = ReactorOperatingState.valueOf(tag.getString("operating_state"));
            controlMode = ReactorControlMode.valueOf(tag.getString("control_mode"));
            automationLevel = ReactorAutomationLevel.valueOf(tag.getString("automation_level"));
            targetPowerFraction = clamp(tag.getDouble("target_power_fraction"), 0.0D, 1.0D);
            engineeringOverride = tag.getBoolean("engineering_override");
            lastOverrideOperator = tag.hasUUID("last_override_operator")
                    ? tag.getUUID("last_override_operator") : null;
            lastOverrideGameTime = Math.max(0L, tag.getLong("last_override_game_time"));
            startupTicksRemaining = Math.max(0, tag.getInt("startup_ticks_remaining"));
            lastRuntimeGameTime = Math.max(0L, tag.getLong("last_runtime_game_time"));
            scramReason = tag.getString("scram_reason");
            if (schema >= 2) {
                accidentStage = ReactorAccidentStage.valueOf(tag.getString("accident_stage"));
                accidentReason = tag.getString("accident_reason");
                corePressureMegapascals = clamp(tag.getDouble("core_pressure_megapascals"), 0.1D, 100.0D);
                vesselIntegrity = clamp(tag.getDouble("vessel_integrity"), 0.0D, 1.0D);
                containmentIntegrity = clamp(tag.getDouble("containment_integrity"), 0.0D, 1.0D);
                accidentEnergyJoules = Math.max(0.0D, tag.getDouble("accident_energy_joules"));
                lastAccidentTransitionGameTime = Math.max(0L, tag.getLong("last_accident_transition_game_time"));
                terrainDamageApplied = tag.getBoolean("terrain_damage_applied");
            } else {
                resetAccidentState();
                Magneticraft.LOGGER.info("Migrated reactor runtime schema 1 to 2 at {}", worldPosition);
            }
            CompoundTag rods = tag.getCompound("rod_insertion");
            for (ReactorRodGroup group : ReactorRodGroup.values()) {
                rodInsertion.put(group, clamp(rods.getDouble(group.name()), 0.0D, 1.0D));
            }
            for (Tag raw : tag.getList("fuel_columns", Tag.TAG_COMPOUND)) {
                CompoundTag entry = (CompoundTag) raw;
                ReactorColumnCoordinate coordinate = new ReactorColumnCoordinate(
                        entry.getInt("x"), entry.getInt("z"));
                FuelAssemblyState state = FuelAssemblyState.load(entry.getCompound("state")).orElseThrow();
                if (fuelStates.put(coordinate, state) != null) {
                    throw new IllegalArgumentException("duplicate fuel coordinate");
                }
            }
            runtime = summaryOfFuelStates();
        } catch (IllegalArgumentException exception) {
            rejectRuntime(tag, "runtime_data", exception);
        }
    }

    private void rejectRuntime(CompoundTag tag, String reason, @Nullable Exception exception) {
        if (exception == null) {
            Magneticraft.LOGGER.error("Rejected reactor runtime schema at {}; preserving raw data", worldPosition);
        } else {
            Magneticraft.LOGGER.error(
                    "Rejected malformed reactor runtime at {}; preserving raw data", worldPosition, exception);
        }
        rejectedRuntimeTag = tag.copy();
        fuelStates.clear();
        runtime = ReactorRuntimeResult.empty(Map.of());
        operatingState = ReactorOperatingState.SCRAMMED;
        controlMode = ReactorControlMode.MANUAL;
        automationLevel = ReactorAutomationLevel.NONE;
        targetPowerFraction = 0.0D;
        engineeringOverride = false;
        overrideRequester = null;
        lastOverrideOperator = null;
        lastOverrideGameTime = 0L;
        startupTicksRemaining = 0;
        lastRuntimeGameTime = 0L;
        scramReason = reason;
        resetAccidentState();
        insertAllRods();
    }

    private void resetRuntimeDefaults() {
        operatingState = ReactorOperatingState.SHUTDOWN;
        controlMode = ReactorControlMode.POWER;
        automationLevel = ReactorAutomationLevel.PROTECTION;
        targetPowerFraction = 1.0D;
        engineeringOverride = false;
        overrideRequester = null;
        overrideConfirmationDeadline = 0L;
        lastOverrideOperator = null;
        lastOverrideGameTime = 0L;
        startupTicksRemaining = 0;
        lastRuntimeGameTime = 0L;
        reportedCoolantFlow = 0.0D;
        coolantFlowReportTick = Long.MIN_VALUE;
        stationPowerAvailable = false;
        stationJoulesRequired = 0;
        stationJoulesAvailable = 0;
        scramReason = "none";
        resetAccidentState();
    }

    private ReactorRuntimeResult summaryOfFuelStates() {
        if (fuelStates.isEmpty()) {
            return ReactorRuntimeResult.empty(Map.of());
        }
        double decay = 0.0D;
        double temperature = 0.0D;
        double hottest = FuelAssemblyState.AMBIENT_TEMPERATURE_KELVIN;
        double cladding = 1.0D;
        double burnup = 0.0D;
        double poison = 0.0D;
        for (FuelAssemblyState state : fuelStates.values()) {
            decay += state.decayHeatJoules();
            temperature += state.temperatureKelvin();
            hottest = Math.max(hottest, state.temperatureKelvin());
            cladding = Math.min(cladding, state.claddingIntegrity());
            burnup += state.burnupFraction();
            poison += state.poisonFraction();
        }
        int count = fuelStates.size();
        return new ReactorRuntimeResult(fuelStates, 0.0D, decay, decay,
                temperature / count, hottest, cladding, burnup / count, poison / count);
    }

    private void invalidMessage(ServerPlayer player, String reason, BlockPos position) {
        player.displayClientMessage(Component.translatable(
                "message.magneticraft.reactor.invalid",
                Component.translatable("message.magneticraft.reactor.reason." + reason),
                position.toShortString()), true);
    }

    private void updateFormedState(boolean formed) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(NuclearReactorControllerBlock.FORMED)
                && state.getValue(NuclearReactorControllerBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(NuclearReactorControllerBlock.FORMED, formed),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    private NuclearReactorStructure.ObservedPart observe(BlockState state) {
        Block block = state.getBlock();
        if (block == ModNuclearBlocks.REACTOR_CONTROLLER.get()) {
            return new NuclearReactorStructure.ObservedPart(
                    NuclearReactorStructure.PartKind.CONTROLLER, null,
                    state.getValue(NuclearReactorControllerBlock.FACING));
        }
        if (block == ModNuclearBlocks.REACTOR_CONTAINMENT_CASING.get()) {
            return part(NuclearReactorStructure.PartKind.CONTAINMENT_CASING);
        }
        if (block == ModNuclearBlocks.REACTOR_PRESSURE_VESSEL.get()) {
            return part(NuclearReactorStructure.PartKind.PRESSURE_VESSEL);
        }
        if (block == ModNuclearBlocks.REACTOR_CONTROL_ROD_ACTUATOR.get()) {
            return part(NuclearReactorStructure.PartKind.CONTROL_ROD_ACTUATOR);
        }
        if (block == ModNuclearBlocks.REACTOR_COLUMN_SEGMENT.get()) {
            return part(NuclearReactorStructure.PartKind.COLUMN_SEGMENT);
        }
        if (block == ModNuclearBlocks.REACTOR_MAIN_COOLANT_PORT.get()) {
            return port(NuclearReactorStructure.PartKind.COOLANT_PORT, state);
        }
        if (block == ModNuclearBlocks.REACTOR_ELECTRICAL_PORT.get()) {
            return port(NuclearReactorStructure.PartKind.ELECTRICAL_PORT, state);
        }
        if (block == ModNuclearBlocks.REACTOR_INSTRUMENTATION_PORT.get()) {
            return port(NuclearReactorStructure.PartKind.INSTRUMENTATION_PORT, state);
        }
        if (block instanceof NuclearReactorColumnBlock column) {
            return new NuclearReactorStructure.ObservedPart(
                    NuclearReactorStructure.PartKind.COLUMN_BASE, column.columnType(), null);
        }
        return null;
    }

    private static NuclearReactorStructure.ObservedPart part(NuclearReactorStructure.PartKind kind) {
        return new NuclearReactorStructure.ObservedPart(kind, null, null);
    }

    private static NuclearReactorStructure.ObservedPart port(
            NuclearReactorStructure.PartKind kind, BlockState state
    ) {
        return new NuclearReactorStructure.ObservedPart(
                kind, null, state.getValue(NuclearReactorPortBlock.FACING));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
