package committee.nova.mods.magneticraft.content.computer.runtime;

import committee.nova.mods.magneticraft.content.computer.vm.VmFault;

import java.util.Objects;

/**
 * Value-only server boundary for script-visible devices. Implementations never
 * expose a level, block entity, capability or Java object to the script.
 */
@FunctionalInterface
public interface ComputerDeviceBus {
    ComputerDeviceBus NONE = (command, argument) -> DeviceResult.fault(VmFault.UNSUPPORTED_DEVICE_INSTRUCTION);

    DeviceResult execute(DeviceCommand command, int argument);

    enum DeviceCommand {
        SET_REDSTONE,
        MOVE_FRONT,
        MOVE_BACK,
        ROTATE_LEFT,
        ROTATE_RIGHT,
        ROTATE_UP,
        ROTATE_DOWN,
        MINE_FRONT,
        SCAN_FRONT,
        /** Extended direction-addressed command retained for the 1.20 candidate VM bridge. */
        MOVE,
        MINE,
        SCAN,
        INVENTORY_COUNT,
        ENERGY_STORED,
        QUARRY
    }

    enum Status {
        COMPLETE,
        WAIT,
        FAULT
    }

    record DeviceResult(Status status, int value, VmFault fault) {
        public DeviceResult {
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(fault, "fault");
            if (status == Status.FAULT && fault == VmFault.NONE) {
                throw new IllegalArgumentException("Fault result requires a concrete fault");
            }
            if (status != Status.FAULT) {
                fault = VmFault.NONE;
            }
        }

        public static DeviceResult complete(int value) {
            return new DeviceResult(Status.COMPLETE, value, VmFault.NONE);
        }

        public static DeviceResult waiting() {
            return new DeviceResult(Status.WAIT, 0, VmFault.NONE);
        }

        public static DeviceResult fault(VmFault fault) {
            return new DeviceResult(Status.FAULT, 0, fault);
        }
    }
}
