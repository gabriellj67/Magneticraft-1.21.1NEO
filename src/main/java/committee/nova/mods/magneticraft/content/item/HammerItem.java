package committee.nova.mods.magneticraft.content.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A handheld crafting hammer with the Nova 1.12 bonus-hit behavior.
 */
public final class HammerItem extends Item {
    private final HammerType type;
    private final float bonusDamage;

    public HammerItem(HammerType type) {
        super(new Item.Properties().durability(type.durability()));
        this.type = type;
        this.bonusDamage = type.bonusDamage();
    }

    public HammerType type() {
        return type;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(
                HammerType.DURABILITY_COST_PER_HIT,
                attacker,
                entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND)
        );
        return target.hurt(attacker.damageSources().generic(), bonusDamage);
    }
}
