package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorColumnEstimate;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutEstimate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
    public static final int IMAGE_WIDTH = 310;
    public static final int IMAGE_HEIGHT = 190;
    private static final int MAX_COLUMNS = 81;
    private static final int MAX_WARNINGS = 16;

    private final BlockPos position;
    private final Direction facing;
    private final int width;
    private final int length;
    private final int height;
    private final Map<ReactorColumnCoordinate, ColumnView> columns;
    private final EnumSet<NuclearReactorPortType> ports;
    private final ReactorLayoutEstimate estimate;
    private final ContainerLevelAccess access;

    public NuclearReactorMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, read(buffer));
    }

    public NuclearReactorMenu(
            int containerId, Inventory inventory, NuclearReactorControllerBlockEntity controller
    ) {
        this(containerId, inventory, view(controller.getBlockPos(),
                controller.snapshot().orElseThrow(), controller.estimate()));
    }

    private NuclearReactorMenu(int containerId, Inventory inventory, MenuView view) {
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
        access = ContainerLevelAccess.create(inventory.player.level(), position);
    }

    public static void write(
            FriendlyByteBuf buffer,
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
        buffer.writeDouble(value.instrumentationCoverage());
        buffer.writeDouble(value.hotspotFactor());
    }

    private static ReactorColumnEstimate readColumn(FriendlyByteBuf buffer) {
        return new ReactorColumnEstimate(
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
            new ReactorColumnEstimate(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
}
