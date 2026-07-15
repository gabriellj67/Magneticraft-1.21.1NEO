package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.network.module.TransformerElectricalHost;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/** Places a transformer only when its complete profile identity is present and currently valid. */
public final class TransformerBlockItem extends BlockItem {
    public TransformerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Optional<TieredElectricalItemData> data = TieredElectricalItemData.read(context.getItemInHand());
        Optional<ResourceLocation> profileId = data.flatMap(TieredElectricalItemData::transformerProfileId);
        var profile = profileId.flatMap(id -> ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.transformerProfile(id)));
        if (data.isEmpty() || profile.isEmpty() || !profile.get().inputTierId().equals(data.get().tierId())) {
            reportInvalid(context.getPlayer(), profileId.map(ResourceLocation::toString).orElse("missing"));
            return InteractionResult.FAIL;
        }
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide) {
            var blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
            if (!(blockEntity instanceof TransformerElectricalHost host)
                    || !host.transformerCoupler().applyProfileFromPlacementData(profileId.orElseThrow())) {
                reportInvalid(context.getPlayer(), profileId.orElseThrow().toString());
                return InteractionResult.FAIL;
            }
        }
        return result;
    }

    public ItemStack stackForProfile(ResourceLocation profileId, ResourceLocation inputTierId) {
        ItemStack stack = new ItemStack(this);
        new TieredElectricalItemData(inputTierId, Optional.empty(), Optional.of(profileId)).write(stack);
        return stack;
    }

    private static void reportInvalid(Player player, String profile) {
        if (player != null && !player.level().isClientSide) {
            player.displayClientMessage(
                    Component.translatable("message.magneticraft.invalid_transformer_profile", profile),
                    true
            );
        }
    }
}
