package committee.nova.mods.magneticraft.content.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

/** Legacy hydraulic-press force selector in stable serialized order. */
public enum HydraulicPressMode {
    LIGHT,
    MEDIUM,
    HEAVY;

    /** Recipe-JSON codec, added for {@code AdvancedProcessingRecipe}'s 1.21.1 {@code MapCodec}. */
    public static final Codec<HydraulicPressMode> CODEC = Codec.STRING.flatXmap(
            value -> {
                try {
                    return DataResult.success(parse(value));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(exception::getMessage);
                }
            },
            mode -> DataResult.success(mode.serializedName())
    );

    /** Packet codec, added for {@code AdvancedProcessingRecipe}'s 1.21.1 {@code StreamCodec}. */
    public static final StreamCodec<io.netty.buffer.ByteBuf, HydraulicPressMode> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(HydraulicPressMode::parse, HydraulicPressMode::serializedName);

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public HydraulicPressMode next() {
        HydraulicPressMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static HydraulicPressMode parse(String value) {
        for (HydraulicPressMode mode : values()) {
            if (mode.serializedName().equals(value.toLowerCase(Locale.ROOT))) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown hydraulic press mode: " + value);
    }
}
