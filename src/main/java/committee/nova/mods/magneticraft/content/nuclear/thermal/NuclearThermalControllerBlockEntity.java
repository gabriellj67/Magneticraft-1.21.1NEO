package committee.nova.mods.magneticraft.content.nuclear.thermal;

import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import committee.nova.mods.magneticraft.system.nuclear.thermal.NuclearThermalTransactions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

/** Server-authoritative lifecycle and lossless processing for all variable nuclear heat-chain facilities. */
public final class NuclearThermalControllerBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int MENU_LOGICAL_DATA_COUNT = 20;
    public static final int MENU_DATA_COUNT = MENU_LOGICAL_DATA_COUNT * 2;
    private static final String FORMED_TAG = "formed";
    private static final String OWNER_TAG = "owner";
    private static final String WIDTH_TAG = "width";
    private static final String LENGTH_TAG = "length";
    private static final String HEIGHT_TAG = "height";

    private final NuclearThermalFacilityType facilityType;
    private final NuclearThermalStructureValidator validator;
    private final ContainerData menuData;
    @Nullable
    private NuclearThermalSnapshot snapshot;
    @Nullable
    private UUID owner;
    private boolean formed;
    private boolean working;
    private boolean backpressured;
    private int transferRate;
    private long transferredHeat;

    public NuclearThermalControllerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.NUCLEAR_THERMAL_CONTROLLER.get(), position, state);
        if (!(state.getBlock() instanceof NuclearThermalControllerBlock block)) {
            throw new IllegalArgumentException("Thermal controller entity requires a thermal controller block");
        }
        facilityType = block.facilityType();
        validator = new NuclearThermalStructureValidator(facilityType);
        formed = state.getValue(NuclearThermalControllerBlock.FORMED);
        menuData = Int32ContainerData.readOnly(
                () -> formed ? 1 : 0,
                () -> facing().get2DDataValue(),
                () -> snapshot == null ? 0 : snapshot.width(),
                () -> snapshot == null ? 0 : snapshot.length(),
                () -> snapshot == null ? 0 : snapshot.height(),
                this::portFlags,
                () -> working ? 1 : 0,
                () -> backpressured ? 1 : 0,
                () -> transferRate,
                () -> (int) Math.min(Integer.MAX_VALUE, transferredHeat),
                () -> snapshot == null ? 0 : snapshot.exchangerBlocks(),
                () -> snapshot == null ? 0 : snapshot.fillBlocks(),
                () -> snapshot == null ? 0 : snapshot.fanBlocks(),
                this::storedJoules,
                this::capacityJoules,
                () -> (int) Math.round(heatTemperature() * 10.0D),
                () -> fluidAmount(NuclearThermalPortRole.HOT_COOLANT_INPUT),
                () -> fluidAmount(NuclearThermalPortRole.COLD_COOLANT_OUTPUT),
                () -> fluidAmount(primaryInputRole()),
                () -> fluidAmount(primaryOutputRole())
        );
    }

    public static void serverTick(
            Level level, BlockPos position, BlockState state, NuclearThermalControllerBlockEntity controller
    ) {
        controller.serverTick((ServerLevel) level);
    }

    public NuclearThermalFacilityType facilityType() {
        return facilityType;
    }

    public boolean formed() {
        return formed;
    }

    public Optional<NuclearThermalSnapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public ContainerData menuData() {
        return menuData;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markChanged();
    }

    public boolean canManage(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D && player.getAbilities().mayBuild
                && (owner == null || owner.equals(player.getUUID())
                || player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2));
    }

    public boolean tryForm(ServerPlayer player) {
        var result = validator.validate(worldPosition, facing(), partLookup());
        if (result.snapshot().isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.nuclear_thermal.invalid", result.reason(), coordinates(result.position())), false);
            return false;
        }
        if (owner == null) owner = player.getUUID();
        applySnapshot(result.snapshot().orElseThrow());
        return true;
    }

    public void unform() {
        NuclearThermalSnapshot previous = snapshot;
        if (previous != null) releasePorts(previous);
        snapshot = null;
        working = false;
        backpressured = false;
        transferRate = 0;
        transferredHeat = 0L;
        setFormed(false);
        markChangedAndSync();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magneticraft." + facilityType.id());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new NuclearThermalMenu(containerId, inventory, this);
    }

    private void serverTick(ServerLevel level) {
        if (!formed) return;
        if (snapshot == null) {
            var result = validator.validate(worldPosition, facing(), partLookup());
            if (result.snapshot().isEmpty()) {
                unform();
                return;
            }
            applySnapshot(result.snapshot().orElseThrow());
        } else if (level.getGameTime() % 20L == 0L) {
            var result = validator.validateDimensions(
                    worldPosition, facing(), snapshot.width(), snapshot.length(), snapshot.height(), partLookup());
            if (result.snapshot().isEmpty()) {
                unform();
                return;
            }
        }
        tickModules();
        working = false;
        backpressured = false;
        transferRate = 0;
        transferredHeat = 0L;
        switch (facilityType) {
            case STEAM_GENERATOR -> tickSteamGenerator();
            case CONDENSER -> tickCondenser();
            case COOLING_TOWER -> tickCoolingTower(level);
        }
        finishServerTick();
    }

    private void tickSteamGenerator() {
        ReactorParameters parameters = parameters();
        NuclearThermalPortBlockEntity hot = port(NuclearThermalPortRole.HOT_COOLANT_INPUT);
        NuclearThermalPortBlockEntity cold = port(NuclearThermalPortRole.COLD_COOLANT_OUTPUT);
        NuclearThermalPortBlockEntity water = port(NuclearThermalPortRole.WATER_INPUT);
        NuclearThermalPortBlockEntity steam = port(NuclearThermalPortRole.STEAM_OUTPUT);
        NuclearThermalPortBlockEntity electrical = port(NuclearThermalPortRole.ELECTRICAL);
        if (hot == null || cold == null || water == null || steam == null || electrical == null || snapshot == null) return;
        int availableJoules = electrical.storedJoules();
        int requested = Math.min(snapshot.exchangerBlocks() * 4, Math.max(0, availableJoules - 20));
        Fluid waterFluid = water.storedFluid();
        int waterAvailable = waterFluid != null && waterFluid.defaultFluidState().is(FluidTags.WATER)
                ? water.storedFluidAmount() : 0;
        var transfer = NuclearThermalTransactions.steamGenerator(
                hot.fluidAmount(fluid(FluidDefinition.HOT_REACTOR_COOLANT)),
                cold.fluidSpace(fluid(FluidDefinition.COLD_REACTOR_COOLANT)),
                waterAvailable,
                steam.fluidSpace(fluid(FluidDefinition.STEAM)),
                requested,
                parameters
        );
        if (transfer.hotCoolantConsumed() <= 0) {
            backpressured = hot.storedFluidAmount() > 0 && (cold.fluidSpace(fluid(FluidDefinition.COLD_REACTOR_COOLANT)) <= 0
                    || steam.fluidSpace(fluid(FluidDefinition.STEAM)) < parameters.steamGeneratorSteamPerWaterMilliBucket());
            return;
        }
        commitDrain(hot, fluid(FluidDefinition.HOT_REACTOR_COOLANT), transfer.hotCoolantConsumed());
        commitDrain(water, waterFluid, transfer.waterConsumed());
        commitFill(cold, fluid(FluidDefinition.COLD_REACTOR_COOLANT), transfer.coldCoolantProduced());
        commitFill(steam, fluid(FluidDefinition.STEAM), transfer.steamProduced());
        electrical.consumeJoules(20 + transfer.hotCoolantConsumed(), false);
        working = true;
        transferRate = transfer.hotCoolantConsumed();
        transferredHeat = transfer.heatTransferredJoules();
    }

    private void tickCondenser() {
        NuclearThermalPortBlockEntity exhaust = port(NuclearThermalPortRole.EXHAUST_INPUT);
        NuclearThermalPortBlockEntity water = port(NuclearThermalPortRole.WATER_OUTPUT);
        NuclearThermalPortBlockEntity heat = port(NuclearThermalPortRole.HEAT);
        NuclearThermalPortBlockEntity electrical = port(NuclearThermalPortRole.ELECTRICAL);
        if (exhaust == null || water == null || heat == null || electrical == null || snapshot == null) return;
        ReactorParameters parameters = parameters();
        HeatNode node = heat.heat().node();
        long heatRoom = (long) Math.floor(Math.max(0.0D,
                (353.15D - node.temperatureKelvin()) * node.heatCapacityJoulesPerKelvin()));
        int energyLimited = Math.max(0, electrical.storedJoules() - 20) * 10;
        int requested = Math.min(snapshot.exchangerBlocks() * 40, energyLimited);
        var transfer = NuclearThermalTransactions.condenser(
                exhaust.fluidAmount(fluid(FluidDefinition.LOW_PRESSURE_EXHAUST_STEAM)),
                water.fluidSpace(Fluids.WATER), heatRoom, requested, parameters);
        if (transfer.exhaustConsumed() <= 0) {
            backpressured = exhaust.storedFluidAmount() > 0;
            return;
        }
        commitDrain(exhaust, fluid(FluidDefinition.LOW_PRESSURE_EXHAUST_STEAM), transfer.exhaustConsumed());
        commitFill(water, Fluids.WATER, transfer.waterProduced());
        node.addHeat(transfer.heatRejectedJoules(), false);
        electrical.consumeJoules(20 + Math.max(1, transfer.exhaustConsumed() / 10), false);
        working = true;
        transferRate = transfer.exhaustConsumed();
        transferredHeat = transfer.heatRejectedJoules();
    }

    private void tickCoolingTower(ServerLevel level) {
        NuclearThermalPortBlockEntity makeup = port(NuclearThermalPortRole.MAKEUP_WATER_INPUT);
        NuclearThermalPortBlockEntity heat = port(NuclearThermalPortRole.HEAT);
        NuclearThermalPortBlockEntity electrical = port(NuclearThermalPortRole.ELECTRICAL);
        if (makeup == null || heat == null || electrical == null || snapshot == null) return;
        Fluid waterFluid = makeup.storedFluid();
        int water = waterFluid != null && waterFluid.defaultFluidState().is(FluidTags.WATER)
                ? makeup.storedFluidAmount() : 0;
        int fanEnergy = snapshot.fanBlocks() * 10;
        if (electrical.consumeJoules(fanEnergy, true) < fanEnergy) return;
        double environment = 288.15D + level.getBiome(worldPosition).value().getBaseTemperature() * 10.0D;
        long capacity = NuclearThermalTransactions.coolingTowerCapacity(
                snapshot.fillBlocks(), snapshot.fanBlocks(), environment, water, parameters());
        HeatNode node = heat.heat().node();
        double removable = Math.max(0.0D,
                node.internalEnergyJoules() - environment * node.heatCapacityJoulesPerKelvin());
        long removed = (long) Math.floor(Math.min(removable, capacity));
        if (removed <= 0L) return;
        int consumedWater = (int) Math.ceil(removed / 100.0D);
        commitDrain(makeup, waterFluid, consumedWater);
        node.removeHeat(removed, false);
        electrical.consumeJoules(fanEnergy, false);
        working = true;
        transferRate = consumedWater;
        transferredHeat = removed;
    }

    private void commitDrain(NuclearThermalPortBlockEntity port, Fluid fluid, int amount) {
        if (fluid == null || port.drain(fluid, amount) != amount) {
            throw new IllegalStateException("Thermal transaction input changed after simulation");
        }
    }

    private void commitFill(NuclearThermalPortBlockEntity port, Fluid fluid, int amount) {
        if (port.fill(fluid, amount) != amount) {
            throw new IllegalStateException("Thermal transaction output changed after simulation");
        }
    }

    private void applySnapshot(NuclearThermalSnapshot next) {
        if (snapshot != null) releasePorts(snapshot);
        snapshot = next;
        next.ports().forEach((role, position) -> {
            if (level != null && level.getBlockEntity(position) instanceof NuclearThermalPortBlockEntity port) {
                port.claim(worldPosition, role);
            }
        });
        setFormed(true);
        markChangedAndSync();
    }

    private void releasePorts(NuclearThermalSnapshot value) {
        if (level == null) return;
        value.ports().values().forEach(position -> {
            if (level.getBlockEntity(position) instanceof NuclearThermalPortBlockEntity port) {
                port.release(worldPosition);
            }
        });
    }

    @Nullable
    private NuclearThermalPortBlockEntity port(NuclearThermalPortRole role) {
        if (level == null || snapshot == null) return null;
        BlockPos position = snapshot.ports().get(role);
        return position != null && level.getBlockEntity(position) instanceof NuclearThermalPortBlockEntity port
                && port.claimedBy(worldPosition, role) ? port : null;
    }

    private int fluidAmount(@Nullable NuclearThermalPortRole role) {
        NuclearThermalPortBlockEntity port = role == null ? null : port(role);
        return port == null ? 0 : port.storedFluidAmount();
    }

    @Nullable
    private NuclearThermalPortRole primaryInputRole() {
        return switch (facilityType) {
            case STEAM_GENERATOR -> NuclearThermalPortRole.WATER_INPUT;
            case CONDENSER -> NuclearThermalPortRole.EXHAUST_INPUT;
            case COOLING_TOWER -> NuclearThermalPortRole.MAKEUP_WATER_INPUT;
        };
    }

    @Nullable
    private NuclearThermalPortRole primaryOutputRole() {
        return switch (facilityType) {
            case STEAM_GENERATOR -> NuclearThermalPortRole.STEAM_OUTPUT;
            case CONDENSER -> NuclearThermalPortRole.WATER_OUTPUT;
            case COOLING_TOWER -> null;
        };
    }

    private int portFlags() {
        if (snapshot == null) return 0;
        int flags = 0;
        for (NuclearThermalPortRole role : snapshot.ports().keySet()) flags |= 1 << role.ordinal();
        return flags;
    }

    private int storedJoules() {
        NuclearThermalPortBlockEntity port = port(NuclearThermalPortRole.ELECTRICAL);
        return port == null ? 0 : port.storedJoules();
    }

    private int capacityJoules() {
        NuclearThermalPortBlockEntity port = port(NuclearThermalPortRole.ELECTRICAL);
        return port == null ? 0 : port.capacityJoules();
    }

    private double heatTemperature() {
        NuclearThermalPortBlockEntity port = port(NuclearThermalPortRole.HEAT);
        return port == null ? HeatNode.AMBIENT_TEMPERATURE_KELVIN : port.heat().node().temperatureKelvin();
    }

    private Direction facing() {
        return getBlockState().getValue(NuclearThermalControllerBlock.FACING);
    }

    private void setFormed(boolean next) {
        formed = next;
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            if (state.getBlock() instanceof NuclearThermalControllerBlock
                    && state.getValue(NuclearThermalControllerBlock.FORMED) != next) {
                level.setBlock(worldPosition, state.setValue(NuclearThermalControllerBlock.FORMED, next), Block.UPDATE_ALL);
            }
        }
    }

    private VariablePartLookup partLookup() {
        return new VariablePartLookup();
    }

    private final class VariablePartLookup implements
            committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureValidator.PartLookup<NuclearThermalPart> {
        @Nullable
        @Override
        public NuclearThermalPart partAt(BlockPos position) {
            if (level == null) return null;
            BlockState state = level.getBlockState(position);
            if (state.isAir()) return new NuclearThermalPart(NuclearThermalPart.Kind.AIR, null);
            if (position.equals(worldPosition) && state.getBlock() instanceof NuclearThermalControllerBlock controller
                    && controller.facilityType() == facilityType) {
                return new NuclearThermalPart(NuclearThermalPart.Kind.CONTROLLER, facing());
            }
            if (state.is(ModNuclearBlocks.FACILITY_CASING.get()))
                return new NuclearThermalPart(NuclearThermalPart.Kind.CASING, null);
            if (state.is(ModNuclearBlocks.NUCLEAR_HEAT_EXCHANGER.get()))
                return new NuclearThermalPart(NuclearThermalPart.Kind.HEAT_EXCHANGER, null);
            if (state.is(ModNuclearBlocks.COOLING_TOWER_FILL.get()))
                return new NuclearThermalPart(NuclearThermalPart.Kind.COOLING_FILL, null);
            if (state.is(ModNuclearBlocks.COOLING_TOWER_FAN.get()))
                return new NuclearThermalPart(NuclearThermalPart.Kind.COOLING_FAN, null);
            if (state.getBlock() instanceof NuclearThermalPortBlock)
                return new NuclearThermalPart(NuclearThermalPart.Kind.PORT,
                        state.getValue(NuclearThermalPortBlock.FACING));
            return null;
        }

        @Override
        public boolean isLoaded(BlockPos position) {
            return level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(position);
        }
    }

    private static Fluid fluid(FluidDefinition definition) {
        return ModFluids.get(definition).source().get();
    }

    private ReactorParameters parameters() {
        return ReactorParameterRegistry.INSTANCE.current().parameters();
    }

    private static String coordinates(BlockPos position) {
        return position.getX() + ", " + position.getY() + ", " + position.getZ();
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        tag.putBoolean(FORMED_TAG, formed);
        if (owner != null) tag.putUUID(OWNER_TAG, owner);
        if (snapshot != null) {
            tag.putInt(WIDTH_TAG, snapshot.width());
            tag.putInt(LENGTH_TAG, snapshot.length());
            tag.putInt(HEIGHT_TAG, snapshot.height());
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        formed = tag.getBoolean(FORMED_TAG);
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        snapshot = null;
        working = false;
        backpressured = false;
    }
}
