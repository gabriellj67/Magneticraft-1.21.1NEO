package committee.nova.mods.magneticraft.content.computer.vm;

/**
 * Narrow server-side boundary for world-facing VM instructions.
 */
@FunctionalInterface
public interface ComputerDevice {
    ComputerDevice NONE = (opcode, operand) -> DeviceResult.fault(VmFault.UNSUPPORTED_DEVICE_INSTRUCTION);

    DeviceResult execute(ComputerOpcode opcode, int operand);

    record DeviceResult(Status status, int value, VmFault fault, boolean yieldAfter) {
        public DeviceResult {
            if (status == Status.FAULT && (fault == null || fault == VmFault.NONE)) {
                throw new IllegalArgumentException("Fault result requires a concrete fault");
            }
            if (status != Status.FAULT) {
                fault = VmFault.NONE;
            }
        }

        public static DeviceResult complete(int value) {
            return new DeviceResult(Status.COMPLETE, value, VmFault.NONE, false);
        }

        public static DeviceResult completeAndYield(int value) {
            return new DeviceResult(Status.COMPLETE, value, VmFault.NONE, true);
        }

        public static DeviceResult waiting() {
            return new DeviceResult(Status.WAIT, 0, VmFault.NONE, true);
        }

        public static DeviceResult fault(VmFault fault) {
            return new DeviceResult(Status.FAULT, 0, fault, true);
        }
    }

    enum Status {
        COMPLETE,
        WAIT,
        FAULT
    }
}
