package committee.nova.mods.magneticraft.content.nuclear.radiation;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.material.Metal;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** Two intentionally small PPE tiers: work clothing and heavy lead-lined protection. */
public enum NuclearProtectionMaterial implements ArmorMaterial {
    BASIC("leather", 12, new int[]{1, 3, 4, 2}, 9, 0.0F, 0.0F),
    HEAVY("iron", 28, new int[]{3, 6, 8, 3}, 6, 2.0F, 0.05F);

    private static final int[] DURABILITY = {13, 15, 16, 11};
    private final String name;
    private final int durabilityMultiplier;
    private final int[] defense;
    private final int enchantment;
    private final float toughness;
    private final float knockbackResistance;

    NuclearProtectionMaterial(String name, int durabilityMultiplier, int[] defense,
                              int enchantment, float toughness, float knockbackResistance) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.defense = defense;
        this.enchantment = enchantment;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return DURABILITY[type.getSlot().getIndex()] * durabilityMultiplier;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defense[type.getSlot().getIndex()];
    }

    @Override public int getEnchantmentValue() { return enchantment; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_IRON; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(ModTags.Items.ingot(Metal.LEAD)); }
    @Override public String getName() { return "minecraft:" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackResistance; }
}
