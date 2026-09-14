package committee.nova.mods.magneticraft.content.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared server-authoritative energy accounting for handheld electric tools.
 */
public abstract class ElectricToolItem extends PortableEnergyItem {
    public static final int CAPACITY = 512_000;
    public static final int BLOCK_BREAK_COST = 1_000;
    public static final int ATTACK_COST = 2_000;

    private final float additionalAttackDamage;

    protected ElectricToolItem(float additionalAttackDamage) {
        super(CAPACITY);
        this.additionalAttackDamage = additionalAttackDamage;
    }

    protected abstract boolean isEffectiveBlock(BlockState state);

    protected final boolean canWork(ItemStack stack) {
        return hasEnergy(stack, BLOCK_BREAK_COST);
    }

    @Override
    public boolean mineBlock(
            ItemStack stack,
            Level level,
            BlockState state,
            BlockPos position,
            LivingEntity miner
    ) {
        if (!level.isClientSide
                && state.getDestroySpeed(level, position) != 0.0F) {
            consumeEnergy(stack, BLOCK_BREAK_COST);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level().isClientSide || !consumeEnergy(stack, ATTACK_COST)) {
            return false;
        }
        return target.hurt(attacker.damageSources().generic(), additionalAttackDamage);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return isEffectiveBlock(state) && !state.is(Tiers.DIAMOND.getIncorrectBlocksForDrops());
    }
}
