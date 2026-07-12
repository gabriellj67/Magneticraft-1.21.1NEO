package committee.nova.mods.magneticraft.content.computer.vm;

/**
 * Durable VM fault state. A fault remains latched until a reset or program replacement.
 */
public enum VmFault {
    NONE,
    INVALID_PROGRAM_COUNTER,
    INVALID_REGISTER,
    INVALID_MEMORY_ADDRESS,
    DIVISION_BY_ZERO,
    UNSUPPORTED_DEVICE_INSTRUCTION,
    INVALID_SNAPSHOT;

    public static VmFault fromPersistentName(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return INVALID_SNAPSHOT;
        }
    }
}
