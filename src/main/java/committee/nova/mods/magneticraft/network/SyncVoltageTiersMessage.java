package committee.nova.mods.magneticraft.network;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierDisplay;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Bounded S2C projection of voltage-tier fields needed for client display and tinting. */
public record SyncVoltageTiersMessage(long generation, List<VoltageTierDisplay> tiers) implements CustomPacketPayload {
    public static final Type<SyncVoltageTiersMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Magneticraft.MOD_ID, "sync_voltage_tiers"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncVoltageTiersMessage> STREAM_CODEC = StreamCodec.of(
            SyncVoltageTiersMessage::encode,
            SyncVoltageTiersMessage::decode
    );

    public SyncVoltageTiersMessage {
        if (generation <= 0L) {
            throw new IllegalArgumentException("generation must be positive");
        }
        tiers = List.copyOf(Objects.requireNonNull(tiers, "tiers"));
        validateTiers(tiers);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static SyncVoltageTiersMessage from(ElectricalDataSnapshot snapshot) {
        return new SyncVoltageTiersMessage(snapshot.generation(), snapshot.displayTiers());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SyncVoltageTiersMessage message) {
        buffer.writeVarLong(message.generation);
        buffer.writeVarInt(message.tiers.size());
        for (VoltageTierDisplay tier : message.tiers) {
            buffer.writeUtf(tier.id().toString(), VoltageTier.MAX_TEXT_LENGTH);
            buffer.writeUtf(tier.translationKey(), VoltageTier.MAX_TEXT_LENGTH);
            buffer.writeDouble(tier.minimumOperatingVoltage());
            buffer.writeDouble(tier.nominalVoltage());
            buffer.writeDouble(tier.maximumVoltage());
            buffer.writeVarInt(tier.colorRgb());
        }
    }

    private static SyncVoltageTiersMessage decode(RegistryFriendlyByteBuf buffer) {
        long generation = buffer.readVarLong();
        int count = buffer.readVarInt();
        if (generation <= 0L || count <= 0 || count > VoltageTier.MAX_SYNCED_TIERS) {
            throw new DecoderException("Invalid voltage-tier snapshot header");
        }
        ArrayList<VoltageTierDisplay> tiers = new ArrayList<>(count);
        try {
            for (int index = 0; index < count; index++) {
                String idText = buffer.readUtf(VoltageTier.MAX_TEXT_LENGTH);
                ResourceLocation id = ResourceLocation.tryParse(idText);
                if (id == null) {
                    throw new DecoderException("Invalid voltage-tier id " + idText);
                }
                tiers.add(new VoltageTierDisplay(
                        id,
                        buffer.readUtf(VoltageTier.MAX_TEXT_LENGTH),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readDouble(),
                        buffer.readVarInt()
                ));
            }
            return new SyncVoltageTiersMessage(generation, tiers);
        } catch (IllegalArgumentException exception) {
            throw new DecoderException("Invalid voltage-tier display snapshot", exception);
        }
    }

    public static void handle(
            SyncVoltageTiersMessage message,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> ClientVoltageTierRegistry.apply(message.generation, message.tiers));
    }

    private static void validateTiers(List<VoltageTierDisplay> tiers) {
        if (tiers.isEmpty() || tiers.size() > VoltageTier.MAX_SYNCED_TIERS) {
            throw new IllegalArgumentException("tier count must be in [1, " + VoltageTier.MAX_SYNCED_TIERS + "]");
        }
        HashSet<ResourceLocation> ids = new HashSet<>();
        for (VoltageTierDisplay tier : tiers) {
            if (!ids.add(tier.id())) {
                throw new IllegalArgumentException("duplicate voltage-tier display " + tier.id());
            }
        }
    }
}
