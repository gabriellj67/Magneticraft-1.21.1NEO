package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Released single-block port contract, split by transport domain.
 *
 * <p>The model-facing direction and the runtime capability side both consume
 * this contract so a machine cannot silently expose a second, generic port.</p>
 */
public final class SingleBlockPortProfile {
    private SingleBlockPortProfile() {
    }

    public static Set<SingleBlockMachineDefinition.PhysicalPort> physicalPorts(
            SingleBlockMachineDefinition definition
    ) {
        return switch (definition) {
            case BOX, FABRICATOR -> Set.of(SingleBlockMachineDefinition.PhysicalPort.ITEM);
            case SLUICE_BOX, FEEDING_TROUGH -> Set.of();
            case SMALL_TANK -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.FLUID_INPUT,
                    SingleBlockMachineDefinition.PhysicalPort.FLUID_OUTPUT
            );
            case INSERTER -> Set.of(SingleBlockMachineDefinition.PhysicalPort.ITEM_TRANSFER);
            case WATER_GENERATOR -> Set.of(SingleBlockMachineDefinition.PhysicalPort.FLUID_OUTPUT);
            case RELAY -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.ITEM,
                    SingleBlockMachineDefinition.PhysicalPort.PNEUMATIC
            );
            case FILTER, TRANSPOSER -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.GHOST_FILTER,
                    SingleBlockMachineDefinition.PhysicalPort.PNEUMATIC
            );
            case COMBUSTION_CHAMBER -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.ITEM,
                    SingleBlockMachineDefinition.PhysicalPort.HEAT
            );
            case STEAM_BOILER -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.FLUID_INPUT,
                    SingleBlockMachineDefinition.PhysicalPort.FLUID_OUTPUT,
                    SingleBlockMachineDefinition.PhysicalPort.HEAT
            );
            case ELECTRIC_HEATER -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.ELECTRICITY,
                    SingleBlockMachineDefinition.PhysicalPort.HEAT
            );
            case RF_HEATER -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.FORGE_ENERGY,
                    SingleBlockMachineDefinition.PhysicalPort.HEAT
            );
            case GASIFICATION_UNIT -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.ITEM,
                    SingleBlockMachineDefinition.PhysicalPort.FLUID_OUTPUT,
                    SingleBlockMachineDefinition.PhysicalPort.HEAT
            );
            case BRICK_FURNACE -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.ITEM,
                    SingleBlockMachineDefinition.PhysicalPort.HEAT
            );
            case INFINITE_ENERGY, AIRLOCK, THERMOPILE, ELECTRIC_ENGINE ->
                    Set.of(SingleBlockMachineDefinition.PhysicalPort.ELECTRICITY);
            case RF_TRANSFORMER -> Set.of(
                    SingleBlockMachineDefinition.PhysicalPort.ELECTRICITY,
                    SingleBlockMachineDefinition.PhysicalPort.FORGE_ENERGY
            );
        };
    }

    public static ItemInventoryModule.SlotAccess item(
            SingleBlockMachineDefinition definition,
            @Nullable Direction side,
            Direction facing
    ) {
        if (definition.inventorySlots() == 0) {
            return ItemInventoryModule.NONE;
        }
        int[] all = allSlots(definition.inventorySlots());
        return switch (definition) {
            case SLUICE_BOX, FEEDING_TROUGH, INSERTER, FILTER -> ItemInventoryModule.NONE;
            case RELAY -> side == facing
                    ? ItemInventoryModule.NONE
                    : new ItemInventoryModule.SlotAccess(all, all);
            case GASIFICATION_UNIT, BRICK_FURNACE ->
                    new ItemInventoryModule.SlotAccess(new int[]{0}, new int[]{1});
            default -> new ItemInventoryModule.SlotAccess(all, all);
        };
    }

    public static FluidTankModule.TankAccess fluid(
            SingleBlockMachineDefinition definition,
            int tank,
            @Nullable Direction side
    ) {
        return switch (definition) {
            case SMALL_TANK -> tank == 0
                    ? FluidTankModule.TankAccess.BOTH
                    : FluidTankModule.TankAccess.NONE;
            case WATER_GENERATOR -> tank == 0
                    ? FluidTankModule.TankAccess.OUTPUT
                    : FluidTankModule.TankAccess.NONE;
            case STEAM_BOILER -> tank == 0
                    ? FluidTankModule.TankAccess.INPUT
                    : tank == 1 ? FluidTankModule.TankAccess.OUTPUT : FluidTankModule.TankAccess.NONE;
            case GASIFICATION_UNIT -> tank == 0
                    ? FluidTankModule.TankAccess.OUTPUT
                    : FluidTankModule.TankAccess.NONE;
            default -> FluidTankModule.TankAccess.NONE;
        };
    }

    public static boolean forgeEnergy(
            SingleBlockMachineDefinition definition,
            @Nullable Direction side,
            Direction facing
    ) {
        return switch (definition) {
            case RF_HEATER, RF_TRANSFORMER -> true;
            default -> false;
        };
    }

    public static boolean electricity(
            SingleBlockMachineDefinition definition,
            Direction side,
            Direction facing
    ) {
        return switch (definition) {
            case ELECTRIC_HEATER, INFINITE_ENERGY, AIRLOCK, THERMOPILE, RF_TRANSFORMER, ELECTRIC_ENGINE -> true;
            default -> false;
        };
    }

    public static boolean heat(
            SingleBlockMachineDefinition definition,
            @Nullable Direction side,
            Direction facing
    ) {
        return switch (definition) {
            case COMBUSTION_CHAMBER -> side == Direction.UP;
            case ELECTRIC_HEATER, RF_HEATER -> side != null && side.getAxis() == Direction.Axis.Y;
            case STEAM_BOILER, GASIFICATION_UNIT, BRICK_FURNACE -> true;
            default -> false;
        };
    }

    public static boolean pneumatic(
            SingleBlockMachineDefinition definition,
            Direction side,
            Direction facing
    ) {
        return switch (definition) {
            case RELAY, TRANSPOSER -> side == facing;
            case FILTER -> side == facing || side == facing.getOpposite();
            default -> false;
        };
    }

    private static int[] allSlots(int slots) {
        int[] result = new int[slots];
        for (int slot = 0; slot < slots; slot++) {
            result[slot] = slot;
        }
        return result;
    }
}
