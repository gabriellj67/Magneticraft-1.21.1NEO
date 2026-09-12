package committee.nova.mods.magneticraft.system.network.electric.profile;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Stable IDs for the built-in tiers; data packs may add further IDs. */
public final class VoltageTierIds {
    public static final ResourceLocation LOW = Magneticraft.id("low_voltage");
    public static final ResourceLocation MEDIUM = Magneticraft.id("medium_voltage");
    public static final ResourceLocation HIGH = Magneticraft.id("high_voltage");
    public static final List<ResourceLocation> BUILT_IN = List.of(LOW, MEDIUM, HIGH);

    private VoltageTierIds() {
    }
}
