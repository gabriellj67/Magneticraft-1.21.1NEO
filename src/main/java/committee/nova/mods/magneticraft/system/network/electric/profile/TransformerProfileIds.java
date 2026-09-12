package committee.nova.mods.magneticraft.system.network.electric.profile;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;

/** Built-in transformer profile identities used only as defaults for item variants. */
public final class TransformerProfileIds {
    public static final ResourceLocation LV_TO_MV = Magneticraft.id("lv_to_mv");
    public static final ResourceLocation MV_TO_HV = Magneticraft.id("mv_to_hv");

    private TransformerProfileIds() {
    }
}
