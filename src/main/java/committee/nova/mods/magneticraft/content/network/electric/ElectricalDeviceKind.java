package committee.nova.mods.magneticraft.content.network.electric;

import io.netty.handler.codec.DecoderException;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Stable menu discriminator for electrical devices with server-side controls. */
public enum ElectricalDeviceKind {
    BOX_TRANSFORMER,
    FUSE_BOX,
    CIRCUIT_BREAKER;

    public static ElectricalDeviceKind from(ElectricalProtectionKind kind) {
        return kind == ElectricalProtectionKind.FUSE_BOX ? FUSE_BOX : CIRCUIT_BREAKER;
    }

    public static ElectricalDeviceKind decode(int ordinal) {
        ElectricalDeviceKind[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new DecoderException("Unknown electrical-device kind " + ordinal);
        }
        return values[ordinal];
    }

    public boolean matches(BlockEntity blockEntity) {
        return switch (this) {
            case BOX_TRANSFORMER -> blockEntity instanceof BoxTransformerBlockEntity;
            case FUSE_BOX -> blockEntity instanceof ElectricalProtectionBlockEntity protection
                    && protection.kind() == ElectricalProtectionKind.FUSE_BOX;
            case CIRCUIT_BREAKER -> blockEntity instanceof ElectricalProtectionBlockEntity protection
                    && protection.kind() == ElectricalProtectionKind.CIRCUIT_BREAKER;
        };
    }
}
