package committee.nova.mods.magneticraft.content.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Energy-powered recoil and entity pushing tool.
 */
public final class ElectricPistonItem extends ElectricToolItem {
    public static final int PUSH_COST = 4_000;

    private static final double RAY_DISTANCE = 6.0D;
    private static final double NORMAL_FORCE = 1.75D;
    private static final double SNEAKING_FORCE = 0.25D;

    public ElectricPistonItem() {
        super(2.0F);
    }

    @Override
    protected boolean isEffectiveBlock(BlockState state) {
        return false;
    }

    @Override
    public boolean mineBlock(
            ItemStack stack,
            Level level,
            BlockState state,
            BlockPos position,
            LivingEntity miner
    ) {
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 start = player.position();
        BlockHitResult hit = level.clip(new ClipContext(
                start,
                start.add(look.scale(RAY_DISTANCE)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        if (!hasEnergy(stack, PUSH_COST)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            if (!consumeEnergy(stack, PUSH_COST)) {
                return InteractionResultHolder.pass(stack);
            }
            double force = player.isShiftKeyDown() ? SNEAKING_FORCE : NORMAL_FORCE;
            player.push(-look.x * force, -look.y * force, -look.z * force);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            playPistonSound(level, player);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack,
            Player player,
            LivingEntity target,
            InteractionHand hand
    ) {
        if (!hasEnergy(stack, PUSH_COST)) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (!level.isClientSide) {
            if (!consumeEnergy(stack, PUSH_COST)) {
                return InteractionResult.PASS;
            }
            Vec3 direction = target.position().subtract(player.position());
            if (direction.lengthSqr() < 1.0E-8D) {
                direction = player.getViewVector(1.0F);
            }
            direction = direction.normalize().scale(NORMAL_FORCE);
            target.push(direction.x, direction.y, direction.z);
            target.hurtMarked = true;
            playPistonSound(level, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void playPistonSound(Level level, LivingEntity source) {
        level.playSound(
                null,
                source.getX(),
                source.getY(),
                source.getZ(),
                SoundEvents.PISTON_EXTEND,
                SoundSource.BLOCKS,
                0.5F,
                level.random.nextFloat() * 0.25F + 0.6F
        );
    }
}
