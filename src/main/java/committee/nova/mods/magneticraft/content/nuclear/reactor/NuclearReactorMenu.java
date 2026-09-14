package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorColumnEstimate;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutEstimate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable menu snapshot for structure preview and the per-column heat map. */
public final class NuclearReactorMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 420;
    public static final int IMAGE_HEIGHT = 230;
    private static final int MAX_COLUMNS = 81;
    private static final int MAX_WARNINGS = 16;
    private static final int BASE_DATA_SIZE = 31;
    private static final int FUEL_DATA_STRIDE = 5;
    private static final int DATA_SIZE = BASE_DATA_SIZE + MAX_COLUMNS * FUEL_DATA_STRIDE;

    private final BlockPos position;
    private final Direction facing;
    private final int width;
    private final int length;
    private final int height;
    private final Map<ReactorColumnCoordinate, ColumnView> columns;
    private final EnumSet<NuclearReactorPortType> ports;
    private final ReactorLayoutEstimate estimate;
    private final ContainerLevelAccess access;
    private final ContainerData liveData;
    @Nullable
    private final NuclearReactorControllerBlockEntity controller;

    public NuclearReactorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, read(buffer), new SimpleContainerData(DATA_SIZE), null);
    }

    public NuclearReactorMenu(
            int containerId, Inventory inventory, NuclearReactorControllerBlockEntity controller
    ) {
        this(containerId, inventory, view(controller.getBlockPos(),
                controller.snapshot().orElseThrow(), controller.estimate()),
                new ReactorControllerData(controller), controller);
    }

    private NuclearReactorMenu(
            int containerId,
            Inventory inventory,
            MenuView view,
            ContainerData liveData,
            @Nullable NuclearReactorControllerBlockEntity controller
    ) {
        super(ModMenus.NUCLEAR_REACTOR.get(), containerId);
        position = view.position();
        facing = view.facing();
        width = view.width();
        length = view.length();
        height = view.height();
        columns = view.columns();
        ports = view.ports().isEmpty()
                ? EnumSet.noneOf(NuclearReactorPortType.class)
                : EnumSet.copyOf(view.ports());
        estimate = view.estimate();
        this.liveData = liveData;
        this.controller = controller;
        checkContainerDataCount(liveData, DATA_SIZE);
        addDataSlots(liveData);
        access = ContainerLevelAccess.create(inventory.player.level(), position);
    }

    public static void write(
            RegistryFriendlyByteBuf buffer,
            BlockPos position,
            @Nullable NuclearReactorSnapshot snapshot,
            ReactorLayoutEstimate estimate
    ) {
        if (snapshot == null) {
            throw new IllegalStateException("Cannot open an unformed reactor menu");
        }
        MenuView view = view(position, snapshot, estimate);
        buffer.writeBlockPos(view.position());
        buffer.writeEnum(view.facing());
        buffer.writeVarInt(view.width());
        buffer.writeVarInt(view.length());
        buffer.writeVarInt(view.height());
        buffer.writeVarInt(view.columns().size());
        view.columns().entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<ReactorColumnCoordinate, ColumnView> entry) -> entry.getKey().z())
                        .thenComparingInt(entry -> entry.getKey().x()))
                .forEach(entry -> {
                    buffer.writeVarInt(entry.getKey().x());
                    buffer.writeVarInt(entry.getKey().z());
                    buffer.writeEnum(entry.getValue().type());
                    writeColumn(buffer, entry.getValue().estimate());
                });
        buffer.writeVarInt(view.ports().size());
        view.ports().forEach(buffer::writeEnum);
        writeEstimate(buffer, estimate);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return true;
        }
        return access.evaluate((level, blockPos) ->
                level.getBlockEntity(blockPos) instanceof NuclearReactorControllerBlockEntity controller
                        && controller.formed() && controller.canManage(player), false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    public BlockPos position() {
        return position;
    }

    public Direction facing() {
        return facing;
    }

    public int width() {
        return width;
    }

    public int length() {
        return length;
    }

    public int height() {
        return height;
    }

    public int activeWidth() {
        return width - 4;
    }

    public int activeLength() {
        return length - 4;
    }

    public Map<ReactorColumnCoordinate, ColumnView> columns() {
        return columns;
    }

    public EnumSet<NuclearReactorPortType> ports() {
        return EnumSet.copyOf(ports);
    }

    public ReactorLayoutEstimate estimate() {
        return estimate;
    }

    public ReactorOperatingState operatingState() {
        return enumValue(ReactorOperatingState.values(), liveData.get(0), ReactorOperatingState.SHUTDOWN);
    }

    public ReactorControlMode controlMode() {
        return enumValue(ReactorControlMode.values(), liveData.get(1), ReactorControlMode.MANUAL);
    }

    public ReactorAutomationLevel automationLevel() {
        return enumValue(ReactorAutomationLevel.values(), liveData.get(2), ReactorAutomationLevel.NONE);
    }

    public boolean engineeringOverride() {
        return liveData.get(3) != 0;
    }

    public boolean stationPowerAvailable() {
        return liveData.get(4) != 0;
    }

    public int stationJoulesRequired() {
        return liveData.get(5);
    }

    public int stationJoulesAvailable() {
        return liveData.get(6);
    }

    public double targetPowerFraction() {
        return liveData.get(7) / 1000.0D;
    }

    public double rodInsertion(ReactorRodGroup group) {
        return liveData.get(8 + group.ordinal()) / 1000.0D;
    }

    public double fissionPowerJoulesPerTick() {
        return liveData.get(12) * 10.0D;
    }

    public double decayHeatJoulesPerTick() {
        return liveData.get(13) * 10.0D;
    }

    public double totalThermalPowerJoulesPerTick() {
        return liveData.get(14) * 10.0D;
    }

    public double averageTemperatureKelvin() {
        return liveData.get(15) / 10.0D;
    }

    public double hottestTemperatureKelvin() {
        return liveData.get(16) / 10.0D;
    }

    public double minimumCladdingIntegrity() {
        return liveData.get(17) / 10_000.0D;
    }

    public double averageBurnupFraction() {
        return liveData.get(18) / 10_000.0D;
    }

    public double averagePoisonFraction() {
        return liveData.get(19) / 10_000.0D;
    }

    public double requiredCoolantFlow() {
        return liveData.get(20) / 10.0D;
    }

    public double actualCoolantFlow() {
        return liveData.get(21) / 10.0D;
    }

    public int loadedFuelCount() {
        return liveData.get(22);
    }

    public String scramReason() {
        return switch (liveData.get(23)) {
            case 1 -> "manual";
            case 2 -> "station_power";
            case 3 -> "structure";
            case 4 -> "chunk_unload";
            case 5 -> "over_temperature";
            case 6 -> "cladding";
            case 7 -> "protection_interlock";
            case 8 -> "runtime_data";
            case 9 -> "accident";
            default -> "none";
        };
    }

    public EnumSet<ReactorInterlock> interlocks() {
        int mask = liveData.get(24);
        EnumSet<ReactorInterlock> result = EnumSet.noneOf(ReactorInterlock.class);
        for (ReactorInterlock interlock : ReactorInterlock.values()) {
            if ((mask & 1 << interlock.ordinal()) != 0) {
                result.add(interlock);
            }
        }
        return result;
    }

    public ReactorAccidentStage accidentStage() {
        return enumValue(ReactorAccidentStage.values(), liveData.get(25), ReactorAccidentStage.NORMAL);
    }

    public double corePressureMegapascals() { return liveData.get(26) / 100.0D; }
    public double vesselIntegrity() { return liveData.get(27) / 10_000.0D; }
    public double containmentIntegrity() { return liveData.get(28) / 10_000.0D; }
    public double accidentEnergyJoules() { return liveData.get(29) * 1_000.0D; }
    public double doseRateMillisievertsPerHour() { return liveData.get(30) / 1000.0D; }

    public FuelView fuelView(ReactorColumnCoordinate coordinate) {
        int index = coordinate.z() * activeWidth() + coordinate.x();
        if (index < 0 || index >= MAX_COLUMNS) {
            return FuelView.EMPTY;
        }
        int base = BASE_DATA_SIZE + index * FUEL_DATA_STRIDE;
        return new FuelView(
                liveData.get(base) != 0,
                liveData.get(base + 1) / 10_000.0D,
                liveData.get(base + 2) / 10_000.0D,
                liveData.get(base + 3) / 10.0D,
                liveData.get(base + 4) / 10_000.0D
        );
    }

    public boolean applyAction(ServerPlayer player, NuclearReactorAction action) {
        return controller != null && controller.applyAction(player, action);
    }

    private static MenuView view(
            BlockPos position, NuclearReactorSnapshot snapshot, ReactorLayoutEstimate estimate
    ) {
        Map<ReactorColumnCoordinate, ColumnView> columns = new LinkedHashMap<>();
        snapshot.columns().forEach((coordinate, type) -> columns.put(
                coordinate,
                new ColumnView(type, estimate.columns().getOrDefault(coordinate, ZERO_COLUMN))
        ));
        return new MenuView(
                position.immutable(), snapshot.facing(), snapshot.width(), snapshot.length(), snapshot.height(),
                Map.copyOf(columns), EnumSet.copyOf(snapshot.ports().keySet()), estimate
        );
    }

    private static MenuView read(FriendlyByteBuf buffer) {
        BlockPos position = buffer.readBlockPos();
        Direction facing = buffer.readEnum(Direction.class);
        int width = buffer.readVarInt();
        int length = buffer.readVarInt();
        int height = buffer.readVarInt();
        if (facing.getAxis().isVertical()
                || !NuclearReactorStructure.DESCRIPTOR.accepts(width, length, height)) {
            throw new IllegalArgumentException("Invalid reactor menu dimensions");
        }
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_COLUMNS || count != (width - 4) * (length - 4)) {
            throw new IllegalArgumentException("Invalid reactor menu column count: " + count);
        }
        Map<ReactorColumnCoordinate, ColumnView> columns = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            ReactorColumnCoordinate coordinate = new ReactorColumnCoordinate(buffer.readVarInt(), buffer.readVarInt());
            ColumnView previous = columns.put(coordinate, new ColumnView(
                    buffer.readEnum(NuclearReactorColumnType.class), readColumn(buffer)));
            if (coordinate.x() >= width - 4 || coordinate.z() >= length - 4 || previous != null) {
                throw new IllegalArgumentException("Invalid reactor menu column coordinate");
            }
        }
        int portCount = buffer.readVarInt();
        if (portCount < 0 || portCount > NuclearReactorPortType.values().length) {
            throw new IllegalArgumentException("Invalid reactor menu port count");
        }
        EnumSet<NuclearReactorPortType> ports = EnumSet.noneOf(NuclearReactorPortType.class);
        for (int index = 0; index < portCount; index++) {
            if (!ports.add(buffer.readEnum(NuclearReactorPortType.class))) {
                throw new IllegalArgumentException("Duplicate reactor menu port");
            }
        }
        ReactorLayoutEstimate estimate = readEstimate(buffer, columns);
        return new MenuView(position, facing, width, length, height, Map.copyOf(columns), ports, estimate);
    }

    private static void writeColumn(FriendlyByteBuf buffer, ReactorColumnEstimate value) {
        buffer.writeDouble(value.thermalPowerJoulesPerTick());
        buffer.writeDouble(value.heatFraction());
        buffer.writeDouble(value.fuelCoupling());
        buffer.writeDouble(value.moderation());
        buffer.writeDouble(value.reflection());
        buffer.writeDouble(value.cooling());
        buffer.writeDouble(value.shutdownWorth());
        buffer.writeDouble(value.controlWorthA());
        buffer.writeDouble(value.controlWorthB());
        buffer.writeDouble(value.controlWorthC());
        buffer.writeDouble(value.controlWorthD());
        buffer.writeDouble(value.instrumentationCoverage());
        buffer.writeDouble(value.hotspotFactor());
    }

    private static ReactorColumnEstimate readColumn(FriendlyByteBuf buffer) {
        return new ReactorColumnEstimate(
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static void writeEstimate(FriendlyByteBuf buffer, ReactorLayoutEstimate value) {
        buffer.writeDouble(value.powerDensityJoulesPerTick());
        buffer.writeDouble(value.fuelEnergyJoulesPerColumn());
        buffer.writeDouble(value.safetyMarginPercent());
        buffer.writeDouble(value.loadFollowingPercent());
        buffer.writeDouble(value.shutdownMarginPercent());
        buffer.writeDouble(value.requiredCoolantFlowMilliBucketsPerTick());
        int warnings = Math.min(MAX_WARNINGS, value.warnings().size());
        buffer.writeVarInt(warnings);
        value.warnings().stream().limit(warnings).forEach(buffer::writeUtf);
    }

    private static ReactorLayoutEstimate readEstimate(
            FriendlyByteBuf buffer, Map<ReactorColumnCoordinate, ColumnView> columns
    ) {
        double power = buffer.readDouble();
        double energy = buffer.readDouble();
        double safety = buffer.readDouble();
        double following = buffer.readDouble();
        double shutdown = buffer.readDouble();
        double coolant = buffer.readDouble();
        int warningCount = buffer.readVarInt();
        if (warningCount < 0 || warningCount > MAX_WARNINGS) {
            throw new IllegalArgumentException("Invalid reactor menu warning count");
        }
        List<String> warnings = new ArrayList<>();
        for (int index = 0; index < warningCount; index++) {
            warnings.add(buffer.readUtf(256));
        }
        Map<ReactorColumnCoordinate, ReactorColumnEstimate> estimates = new LinkedHashMap<>();
        columns.forEach((coordinate, view) -> estimates.put(coordinate, view.estimate()));
        return new ReactorLayoutEstimate(estimates, power, energy, safety, following, shutdown, coolant, warnings);
    }

    public record ColumnView(NuclearReactorColumnType type, ReactorColumnEstimate estimate) {
    }

    public record FuelView(
            boolean loaded,
            double burnupFraction,
            double poisonFraction,
            double temperatureKelvin,
            double claddingIntegrity
    ) {
        private static final FuelView EMPTY = new FuelView(
                false, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private record MenuView(
            BlockPos position,
            Direction facing,
            int width,
            int length,
            int height,
            Map<ReactorColumnCoordinate, ColumnView> columns,
            EnumSet<NuclearReactorPortType> ports,
            ReactorLayoutEstimate estimate
    ) {
    }

    private static final ReactorColumnEstimate ZERO_COLUMN =
            new ReactorColumnEstimate(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                    0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    private static <T> T enumValue(T[] values, int ordinal, T fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }

    private static int scaled(double value, double scale) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(value * scale)));
    }

    private static final class ReactorControllerData implements ContainerData {
        private final NuclearReactorControllerBlockEntity controller;

        private ReactorControllerData(NuclearReactorControllerBlockEntity controller) {
            this.controller = controller;
        }

        @Override
        public int get(int index) {
            if (index == 0) return controller.operatingState().ordinal();
            if (index == 1) return controller.controlMode().ordinal();
            if (index == 2) return controller.automationLevel().ordinal();
            if (index == 3) return controller.engineeringOverride() ? 1 : 0;
            if (index == 4) return controller.stationPowerAvailable() ? 1 : 0;
            if (index == 5) return controller.stationJoulesRequired();
            if (index == 6) return controller.stationJoulesAvailable();
            if (index == 7) return scaled(controller.targetPowerFraction(), 1000.0D);
            if (index >= 8 && index <= 11) {
                return scaled(controller.rodInsertion(ReactorRodGroup.values()[index - 8]), 1000.0D);
            }
            if (index == 12) return scaled(controller.runtime().fissionPowerJoulesPerTick(), 0.1D);
            if (index == 13) return scaled(controller.runtime().decayHeatJoulesPerTick(), 0.1D);
            if (index == 14) return scaled(controller.runtime().totalThermalPowerJoulesPerTick(), 0.1D);
            if (index == 15) return scaled(controller.runtime().averageTemperatureKelvin(), 10.0D);
            if (index == 16) return scaled(controller.runtime().hottestTemperatureKelvin(), 10.0D);
            if (index == 17) return scaled(controller.runtime().minimumCladdingIntegrity(), 10_000.0D);
            if (index == 18) return scaled(controller.runtime().averageBurnupFraction(), 10_000.0D);
            if (index == 19) return scaled(controller.runtime().averagePoisonFraction(), 10_000.0D);
            if (index == 20) return scaled(controller.estimate().requiredCoolantFlowMilliBucketsPerTick(), 10.0D);
            if (index == 21) return scaled(controller.actualCoolantFlow(), 10.0D);
            if (index == 22) return controller.fuelStates().size();
            if (index == 23) return scramReasonCode(controller.scramReason());
            if (index == 24) {
                int mask = 0;
                for (ReactorInterlock interlock : controller.interlocks()) {
                    mask |= 1 << interlock.ordinal();
                }
                return mask;
            }
            if (index == 25) return controller.accidentStage().ordinal();
            if (index == 26) return scaled(controller.corePressureMegapascals(), 100.0D);
            if (index == 27) return scaled(controller.vesselIntegrity(), 10_000.0D);
            if (index == 28) return scaled(controller.containmentIntegrity(), 10_000.0D);
            if (index == 29) return scaled(controller.accidentEnergyJoules(), 0.001D);
            if (index == 30) return scaled(controller.doseRateMillisievertsPerHour(), 1000.0D);
            if (index < BASE_DATA_SIZE || index >= DATA_SIZE) return 0;
            int relative = index - BASE_DATA_SIZE;
            int columnIndex = relative / FUEL_DATA_STRIDE;
            int field = relative % FUEL_DATA_STRIDE;
            NuclearReactorSnapshot snapshot = controller.snapshot().orElse(null);
            if (snapshot == null) return 0;
            int activeWidth = snapshot.activeWidth();
            ReactorColumnCoordinate coordinate = new ReactorColumnCoordinate(
                    columnIndex % activeWidth, columnIndex / activeWidth);
            var state = controller.fuelState(coordinate).orElse(null);
            if (state == null) return 0;
            return switch (field) {
                case 0 -> 1;
                case 1 -> scaled(state.burnupFraction(), 10_000.0D);
                case 2 -> scaled(state.poisonFraction(), 10_000.0D);
                case 3 -> scaled(state.temperatureKelvin(), 10.0D);
                case 4 -> scaled(state.claddingIntegrity(), 10_000.0D);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_SIZE;
        }

        private static int scramReasonCode(String reason) {
            return switch (reason) {
                case "manual" -> 1;
                case "station_power" -> 2;
                case "structure" -> 3;
                case "chunk_unload" -> 4;
                case "over_temperature" -> 5;
                case "cladding" -> 6;
                case "protection_interlock" -> 7;
                case "runtime_schema", "runtime_data" -> 8;
                case "accident" -> 9;
                default -> 0;
            };
        }
    }
}
