package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.init.ModRegistries;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Data-component-backed replacement for the per-item NBT balance the Forge 1.20.1 port kept on
 * {@code ICapabilitySerializable}'s {@code serializeNBT()}/{@code deserializeNBT()}. NeoForge's item
 * capabilities are stateless views recomputed per query (see {@link PortableEnergyItem}) - the actual
 * balance has to live somewhere the stack itself owns, hence this component.
 *
 * <p>New file - the Forge source had no equivalent, because {@code ItemStack.getOrCreateTag()}/
 * {@code getTag()} do not exist in 1.21.1 (the whole NBT-on-ItemStack API was replaced by data
 * components in the 1.20.5+ item rework).
 */
public final class PortableEnergyComponents {
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PORTABLE_ENERGY =
            ModRegistries.DATA_COMPONENT_TYPES.register(
                    "portable_energy",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build()
            );

    private PortableEnergyComponents() {
    }

    public static void bootstrap() {
    }
}
