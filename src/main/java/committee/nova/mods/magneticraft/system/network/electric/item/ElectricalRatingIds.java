package committee.nova.mods.magneticraft.system.network.electric.item;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** Stable built-in protection ratings; the actual current is selected from the active voltage tier. */
public final class ElectricalRatingIds {
    public static final ResourceLocation STANDARD = Magneticraft.id("standard");
    public static final ResourceLocation HEAVY = Magneticraft.id("heavy");
    public static final Set<ResourceLocation> BUILT_IN = Set.of(STANDARD, HEAVY);

    private ElectricalRatingIds() {
    }

    public static boolean isKnown(ResourceLocation id) {
        return BUILT_IN.contains(id);
    }
}
