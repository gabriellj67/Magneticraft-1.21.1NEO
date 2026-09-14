package committee.nova.mods.magneticraft.content.nuclear.radiation;

import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.init.ModRegistries;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Two intentionally small PPE tiers: work clothing and heavy lead-lined protection.
 *
 * <p>Rewritten for the vanilla 1.20.5+ armor rework: {@code ArmorMaterial} is no longer an
 * interface an enum can implement - it is a plain record registered in
 * {@link net.minecraft.core.registries.Registries#ARMOR_MATERIAL}, and per-type durability moved
 * out of the material entirely (now supplied at item-registration time via
 * {@code ArmorItem.Type.getDurability(int)}, see {@link #durabilityMultiplier()}). Both tiers
 * intentionally reuse vanilla's own leather/iron texture layers instead of shipping custom armor
 * textures, matching the Forge original's {@code getName() -> "minecraft:leather"/"minecraft:iron"}
 * trick for the default texture-path resolution.
 */
public enum NuclearProtectionMaterial {
    BASIC("basic", 12, 9, 0.0F, 0.0F, defense(1, 3, 4, 2), "leather"),
    HEAVY("heavy", 28, 6, 2.0F, 0.05F, defense(3, 6, 8, 3), "iron");

    private final String id;
    private final int durabilityMultiplier;
    private final DeferredHolder<ArmorMaterial, ArmorMaterial> holder;

    NuclearProtectionMaterial(
            String id,
            int durabilityMultiplier,
            int enchantmentValue,
            float toughness,
            float knockbackResistance,
            Map<ArmorItem.Type, Integer> defense,
            String reusedVanillaTextureName
    ) {
        this.id = id;
        this.durabilityMultiplier = durabilityMultiplier;
        this.holder = ModRegistries.ARMOR_MATERIALS.register(id, () -> new ArmorMaterial(
                defense,
                enchantmentValue,
                SoundEvents.ARMOR_EQUIP_IRON,
                () -> Ingredient.of(ModTags.Items.ingot(Metal.LEAD)),
                List.of(new ArmorMaterial.Layer(ResourceLocation.withDefaultNamespace(reusedVanillaTextureName))),
                toughness,
                knockbackResistance
        ));
    }

    private static Map<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        Map<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.HELMET, helmet);
        return Map.copyOf(map);
    }

    public String id() {
        return id;
    }

    /** Passed to {@code ArmorItem.Type.getDurability(int)} when an item registers its max damage. */
    public int durabilityMultiplier() {
        return durabilityMultiplier;
    }

    public Holder<ArmorMaterial> holder() {
        return holder;
    }

    public static void bootstrap() {
    }
}
