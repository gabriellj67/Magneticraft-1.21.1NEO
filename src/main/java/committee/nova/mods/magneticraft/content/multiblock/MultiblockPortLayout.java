package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Exact port coordinates copied from the released 1.12 multiblock modules.
 * Coordinates and sides use the legacy controller-facing convention.
 */
public final class MultiblockPortLayout {
    private static final StructureOffset ORIGIN = new StructureOffset(0, 0, 0);
    private static final Map<MultiblockDefinition, List<Port>> PORTS = createLayouts();

    private MultiblockPortLayout() {
    }

    public static List<Port> ports(MultiblockDefinition definition) {
        return PORTS.getOrDefault(definition, List.of());
    }

    public static Optional<Port> find(
            AdvancedMultiblockBlockEntity controller,
            BlockPos position,
            Direction side,
            Kind kind
    ) {
        return ports(controller.definition()).stream()
                .filter(port -> port.kind() == kind)
                .filter(port -> port.worldPosition(controller).equals(position))
                .filter(port -> port.worldSide(controller.facing()) == side)
                .findFirst();
    }

    public static boolean supports(
            AdvancedMultiblockBlockEntity controller,
            BlockPos position,
            NetworkDomain domain,
            Direction side
    ) {
        Kind kind = switch (domain) {
            case ELECTRICITY -> Kind.ELECTRICITY;
            case HEAT -> Kind.HEAT;
            case FLUID -> Kind.FLUID;
            case LOGISTICS -> Kind.ITEM;
            default -> null;
        };
        return kind != null && find(controller, position, side, kind).isPresent();
    }

    private static Map<MultiblockDefinition, List<Port>> createLayouts() {
        EnumMap<MultiblockDefinition, List<Port>> layouts = new EnumMap<>(MultiblockDefinition.class);
        layouts.put(MultiblockDefinition.SOLAR_PANEL, List.of(
                network(0, 0, -5, Direction.NORTH, Kind.ELECTRICITY),
                network(0, 0, 0, Direction.SOUTH, Kind.ELECTRICITY)
        ));
        layouts.put(MultiblockDefinition.STEAM_ENGINE, steamEngine());
        layouts.put(MultiblockDefinition.STEAM_TURBINE, steamTurbine());
        layouts.put(MultiblockDefinition.GRINDER, grinder());
        layouts.put(MultiblockDefinition.SIEVE, sieve());
        layouts.put(MultiblockDefinition.HYDRAULIC_PRESS, hydraulicPress());
        layouts.put(MultiblockDefinition.PUMPJACK, List.of(
                network(-1, 0, 0, Direction.UP, Kind.ELECTRICITY),
                fluid(0, 0, 0, Direction.SOUTH, 0, FluidTankModule.TankAccess.OUTPUT)
        ));
        layouts.put(MultiblockDefinition.OIL_HEATER, oilHeater());
        layouts.put(MultiblockDefinition.REFINERY, refinery());
        layouts.put(MultiblockDefinition.SOLAR_TOWER, List.of(
                network(0, 0, -1, Direction.DOWN, Kind.HEAT)
        ));
        layouts.put(MultiblockDefinition.BIG_COMBUSTION_CHAMBER, combustionChamber());
        layouts.put(MultiblockDefinition.BIG_STEAM_BOILER, steamBoiler());
        layouts.put(MultiblockDefinition.BIG_ELECTRIC_FURNACE, electricFurnace());
        return Map.copyOf(layouts);
    }

    private static List<Port> steamEngine() {
        List<Port> ports = new ArrayList<>();
        ports.add(network(-2, 0, -2, Direction.UP, Kind.ELECTRICITY));
        ports.add(network(-2, 0, -2, Direction.SOUTH, Kind.ELECTRICITY));
        for (Direction side : Direction.values()) {
            ports.add(fluid(0, 0, 0, side, 0, FluidTankModule.TankAccess.INPUT));
        }
        return List.copyOf(ports);
    }

    private static List<Port> steamTurbine() {
        List<Port> ports = new ArrayList<>();
        ports.add(network(0, 2, -1, Direction.UP, Kind.ELECTRICITY));
        for (int x : new int[]{-1, 1}) {
            Direction side = x < 0 ? Direction.WEST : Direction.EAST;
            for (int y = 0; y <= 1; y++) {
                for (int z = -2; z <= -1; z++) {
                    ports.add(fluid(x, y, z, side, 0, FluidTankModule.TankAccess.INPUT));
                }
            }
        }
        return List.copyOf(ports);
    }

    private static List<Port> grinder() {
        List<Port> ports = new ArrayList<>();
        ports.add(network(1, 1, -1, Direction.EAST, Kind.ELECTRICITY));
        ports.add(item(0, 0, -2, Direction.NORTH, slots(), slots(1, 2)));
        ports.add(item(-1, 0, -1, Direction.WEST, slots(0), slots()));
        for (int x = -1; x <= 1; x++) {
            for (int z = -2; z <= 0; z++) {
                ports.add(item(x, 3, z, Direction.UP, slots(0), slots()));
            }
        }
        return List.copyOf(ports);
    }

    private static List<Port> sieve() {
        return List.of(
                network(-1, 1, 0, Direction.SOUTH, Kind.ELECTRICITY),
                network(1, 1, 0, Direction.SOUTH, Kind.ELECTRICITY),
                item(0, 1, 0, Direction.UP, slots(0), slots()),
                item(0, 0, 0, Direction.SOUTH, slots(0), slots()),
                item(0, 0, -1, Direction.DOWN, slots(), slots(1)),
                item(0, 0, -2, Direction.DOWN, slots(), slots(2)),
                item(0, 0, -3, Direction.DOWN, slots(), slots(3))
        );
    }

    private static List<Port> hydraulicPress() {
        return List.of(
                network(-1, 1, -1, Direction.WEST, Kind.ELECTRICITY),
                network(1, 1, -1, Direction.EAST, Kind.ELECTRICITY),
                item(0, 0, 0, Direction.SOUTH, slots(0), slots()),
                item(0, 0, -2, Direction.NORTH, slots(), slots(1))
        );
    }

    private static List<Port> oilHeater() {
        List<Port> ports = new ArrayList<>();
        ports.add(fluid(0, 1, -2, Direction.NORTH, 0, FluidTankModule.TankAccess.INPUT));
        ports.add(fluid(0, 2, -1, Direction.UP, 1, FluidTankModule.TankAccess.OUTPUT));
        addArea(ports, -1, 1, 0, -2, 0, Direction.DOWN, Kind.HEAT);
        return List.copyOf(ports);
    }

    private static List<Port> refinery() {
        List<Port> ports = new ArrayList<>();
        ports.add(fluid(0, 1, -2, Direction.NORTH, 0, FluidTankModule.TankAccess.INPUT));
        ports.add(fluid(-1, 1, -1, Direction.WEST, 1, FluidTankModule.TankAccess.INPUT));
        ports.add(fluid(1, 1, -1, Direction.EAST, 1, FluidTankModule.TankAccess.INPUT));
        addCross(ports, 3, 2);
        addCross(ports, 5, 3);
        addCross(ports, 7, 4);
        return List.copyOf(ports);
    }

    private static List<Port> combustionChamber() {
        List<Port> ports = new ArrayList<>();
        for (int x : new int[]{-1, 1}) {
            Direction side = x < 0 ? Direction.WEST : Direction.EAST;
            ports.add(item(x, 0, -1, side, slots(0), slots()));
            ports.add(fluid(x, 0, -1, side, 0, FluidTankModule.TankAccess.INPUT));
        }
        addArea(ports, -1, 1, 1, -2, 0, Direction.UP, Kind.HEAT);
        return List.copyOf(ports);
    }

    private static List<Port> steamBoiler() {
        List<Port> ports = new ArrayList<>();
        for (int y = 0; y <= 3; y++) {
            for (int z = -2; z <= 0; z++) {
                ports.add(fluid(-1, y, z, Direction.WEST, 0, FluidTankModule.TankAccess.INPUT));
                ports.add(fluid(1, y, z, Direction.EAST, 0, FluidTankModule.TankAccess.INPUT));
            }
            for (int x = -1; x <= 1; x++) {
                ports.add(fluid(x, y, -2, Direction.NORTH, 0, FluidTankModule.TankAccess.INPUT));
                ports.add(fluid(x, y, 0, Direction.SOUTH, 0, FluidTankModule.TankAccess.INPUT));
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -2; z <= 0; z++) {
                ports.add(fluid(x, 0, z, Direction.DOWN, 0, FluidTankModule.TankAccess.INPUT));
                ports.add(fluid(x, 3, z, Direction.UP, 1, FluidTankModule.TankAccess.OUTPUT));
            }
        }
        addArea(ports, -1, 1, 0, -2, 0, Direction.DOWN, Kind.HEAT);
        return List.copyOf(ports);
    }

    private static List<Port> electricFurnace() {
        return List.of(
                network(0, 1, -1, Direction.UP, Kind.ELECTRICITY),
                item(0, 0, 0, Direction.SOUTH, slots(0), slots()),
                item(0, 0, -1, Direction.SOUTH, slots(0), slots()),
                item(0, 0, -2, Direction.NORTH, slots(), slots(1))
        );
    }

    private static void addCross(List<Port> ports, int y, int tank) {
        ports.add(fluid(0, y, -2, Direction.NORTH, tank, FluidTankModule.TankAccess.OUTPUT));
        ports.add(fluid(0, y, 0, Direction.SOUTH, tank, FluidTankModule.TankAccess.OUTPUT));
        ports.add(fluid(-1, y, -1, Direction.WEST, tank, FluidTankModule.TankAccess.OUTPUT));
        ports.add(fluid(1, y, -1, Direction.EAST, tank, FluidTankModule.TankAccess.OUTPUT));
    }

    private static void addArea(
            List<Port> ports,
            int minX,
            int maxX,
            int y,
            int minZ,
            int maxZ,
            Direction side,
            Kind kind
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                ports.add(network(x, y, z, side, kind));
            }
        }
    }

    private static Port network(int x, int y, int z, Direction side, Kind kind) {
        return new Port(new StructureOffset(x, y, z), side, kind, -1,
                FluidTankModule.TankAccess.NONE, ItemInventoryModule.NONE);
    }

    private static Port fluid(
            int x, int y, int z, Direction side, int tank, FluidTankModule.TankAccess access
    ) {
        return new Port(new StructureOffset(x, y, z), side, Kind.FLUID, tank, access,
                ItemInventoryModule.NONE);
    }

    private static Port item(
            int x, int y, int z, Direction side, int[] insertSlots, int[] extractSlots
    ) {
        return new Port(new StructureOffset(x, y, z), side, Kind.ITEM, -1,
                FluidTankModule.TankAccess.NONE,
                new ItemInventoryModule.SlotAccess(insertSlots, extractSlots));
    }

    private static int[] slots(int... slots) {
        return slots;
    }

    public enum Kind {
        ITEM,
        FLUID,
        ELECTRICITY,
        HEAT
    }

    public record Port(
            StructureOffset offset,
            Direction side,
            Kind kind,
            int target,
            FluidTankModule.TankAccess fluidAccess,
            ItemInventoryModule.SlotAccess itemAccess
    ) {
        public BlockPos worldPosition(AdvancedMultiblockBlockEntity controller) {
            return worldPosition(controller.getBlockPos(), controller.facing());
        }

        public BlockPos worldPosition(BlockPos controller, Direction controllerFacing) {
            StructureOffset relative = MultiblockTransform.relative(
                    offset, ORIGIN, controllerFacing.getOpposite(), false
            );
            return controller.offset(relative.x(), relative.y(), relative.z());
        }

        public Direction worldSide(Direction controllerFacing) {
            StructureOffset direction = MultiblockTransform.relative(
                    new StructureOffset(side.getStepX(), side.getStepY(), side.getStepZ()),
                    ORIGIN,
                    controllerFacing.getOpposite(),
                    false
            );
            for (Direction candidate : Direction.values()) {
                if (candidate.getStepX() == direction.x()
                        && candidate.getStepY() == direction.y()
                        && candidate.getStepZ() == direction.z()) {
                    return candidate;
                }
            }
            throw new IllegalStateException("Invalid rotated multiblock port side: " + direction);
        }
    }
}
