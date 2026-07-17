package committee.nova.mods.magneticraft.content.nuclear.radiation;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Armor item exposing its radiation attenuation independently from vanilla armor values. */
public final class RadiationProtectionItem extends ArmorItem {
    private final double protectionFraction;

    public RadiationProtectionItem(NuclearProtectionMaterial material, Type type, double protectionFraction) {
        super(material, type, new Properties());
        this.protectionFraction = protectionFraction;
    }

    public double protectionFraction() {
        return protectionFraction;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "item.magneticraft.radiation_protection.attenuation",
                Math.round(protectionFraction * 100.0D)
        ).withStyle(ChatFormatting.GRAY));
    }
}
