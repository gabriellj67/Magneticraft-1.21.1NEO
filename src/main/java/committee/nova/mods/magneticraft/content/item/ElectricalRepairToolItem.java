package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class ElectricalRepairToolItem extends Item {
    public static final int DURABILITY = 16;

    public ElectricalRepairToolItem() {
        super(new Properties().durability(DURABILITY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockEntity blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof MachineBlockEntity machine) || !machine.electricalFaulted()) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (!machine.tryRepairElectricalFault()) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.magneticraft.repair_voltage_unsafe"), true);
            }
            return InteractionResult.FAIL;
        }
        if (player != null && !player.getAbilities().instabuild) {
            context.getItemInHand().hurtAndBreak(1, player, owner -> owner.broadcastBreakEvent(context.getHand()));
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.magneticraft.electrical_repaired"), true);
        }
        return InteractionResult.CONSUME;
    }
}
