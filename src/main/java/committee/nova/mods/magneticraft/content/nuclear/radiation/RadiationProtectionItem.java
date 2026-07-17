package committee.nova.mods.magneticraft.content.nuclear.radiation;

import net.minecraft.world.item.ArmorItem;

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
}
