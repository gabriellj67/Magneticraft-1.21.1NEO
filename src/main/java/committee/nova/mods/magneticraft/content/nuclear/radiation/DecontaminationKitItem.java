package committee.nova.mods.magneticraft.content.nuclear.radiation;

import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.system.nuclear.radiation.RadiationExposureService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Consumable cleanup kit for player contamination and removable dispersed debris. */
public final class DecontaminationKitItem extends Item {
    public DecontaminationKitItem() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && RadiationExposureService.decontaminate(player)) {
            consume(player, stack);
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.decontamination.complete"), true);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(ModNuclearBlocks.RADIOACTIVE_DEBRIS.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.destroyBlock(context.getClickedPos(), false);
            if (context.getPlayer() != null) {
                consume(context.getPlayer(), context.getItemInHand());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }
}
