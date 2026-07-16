package committee.nova.mods.magneticraft.content.network.electric;

import io.netty.handler.codec.DecoderException;

/** Bounded intents accepted by an open electrical-device menu. */
public enum ElectricalDeviceAction {
    REVERSE_TRANSFORMER,
    CYCLE_REDSTONE_MODE,
    RESET_BREAKER,
    CYCLE_CONTROL_REDSTONE,
    CYCLE_RESISTOR_FIRST_RING,
    CYCLE_RESISTOR_SECOND_RING,
    CYCLE_RESISTOR_MULTIPLIER_RING;

    public static ElectricalDeviceAction decode(int ordinal) {
        ElectricalDeviceAction[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException("Unknown electrical-device action " + ordinal);
        }
        return values[ordinal];
    }
}
