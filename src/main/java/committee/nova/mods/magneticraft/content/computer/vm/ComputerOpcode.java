package committee.nova.mods.magneticraft.content.computer.vm;

import java.util.Optional;

/**
 * Stable instruction set for Magneticraft's bounded computer.
 */
public enum ComputerOpcode {
    NOP(0, 0, false),
    HALT(1, 0, false),
    SET(2, 2, false),
    ADD(3, 2, false),
    SUBTRACT(4, 2, false),
    MULTIPLY(5, 2, false),
    DIVIDE(6, 2, false),
    MODULO(7, 2, false),
    LOAD(8, 2, false),
    STORE(9, 2, false),
    JUMP(10, 1, false),
    JUMP_IF_ZERO(11, 2, false),
    MOVE(32, 2, true),
    MINE(33, 2, true),
    SET_REDSTONE(34, 2, true);

    private final int networkId;
    private final int operandCount;
    private final boolean deviceInstruction;

    ComputerOpcode(int networkId, int operandCount, boolean deviceInstruction) {
        this.networkId = networkId;
        this.operandCount = operandCount;
        this.deviceInstruction = deviceInstruction;
    }

    public int networkId() {
        return networkId;
    }

    public boolean isDeviceInstruction() {
        return deviceInstruction;
    }

    public int operandCount() {
        return operandCount;
    }

    public String serializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public String descriptionTranslationKey() {
        return "guide.magneticraft.computer.opcode." + serializedName();
    }

    public static Optional<ComputerOpcode> fromNetworkId(int networkId) {
        for (ComputerOpcode opcode : values()) {
            if (opcode.networkId == networkId) {
                return Optional.of(opcode);
            }
        }
        return Optional.empty();
    }
}
