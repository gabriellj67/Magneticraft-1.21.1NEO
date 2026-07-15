package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.network.module.TransformerElectricalHost;
import committee.nova.mods.magneticraft.content.item.TieredElectricalItemName;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfileIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Legacy transformer item upgrades an existing pole instead of placing a second pole. */
public final class ElectricPoleTransformerBlockItem extends BlockItem {
    private final ElectricPoleBlock transformer;

    public ElectricPoleTransformerBlockItem(ElectricPoleBlock transformer, Properties properties) {
        super(transformer, properties);
        this.transformer = transformer;
    }

    @Override
    public Component getName(ItemStack stack) {
        return TieredElectricalItemName.decorate(stack, super.getName(stack));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof ElectricPoleBlock pole) || pole.isTransformer()) {
            return InteractionResult.PASS;
        }
        Optional<TieredElectricalItemData> itemData = TieredElectricalItemData.read(context.getItemInHand());
        Optional<ResourceLocation> profileId = itemData.flatMap(TieredElectricalItemData::transformerProfileId);
        if (itemData.isEmpty() || profileId.isEmpty()) {
            reportInvalid(context, "missing");
            return InteractionResult.FAIL;
        }
        if (context.getLevel() instanceof ServerLevel level) {
            var profile = ElectricalDataRegistry.INSTANCE.current()
                    .flatMap(snapshot -> snapshot.transformerProfile(profileId.get()));
            if (profile.isEmpty() || !profile.get().inputTierId().equals(itemData.get().tierId())) {
                reportInvalid(context, profileId.get().toString());
                return InteractionResult.FAIL;
            }
            if (!BlockPosHolder.convert(level, context, state, pole, transformer, profileId.get())) {
                reportInvalid(context, profileId.get().toString());
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    public ItemStack stackForProfile(ResourceLocation profileId, ResourceLocation inputTierId) {
        ItemStack stack = new ItemStack(this);
        new TieredElectricalItemData(inputTierId, Optional.empty(), Optional.of(profileId)).write(stack);
        return stack;
    }

    public ItemStack defaultStack() {
        return stackForProfile(TransformerProfileIds.LV_TO_MV, committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds.LOW);
    }

    private static void reportInvalid(UseOnContext context, String profile) {
        if (context.getPlayer() != null && !context.getLevel().isClientSide) {
            context.getPlayer().displayClientMessage(
                    Component.translatable("message.magneticraft.invalid_transformer_profile", profile),
                    true
            );
        }
    }

    private static final class BlockPosHolder {
        private BlockPosHolder() {
        }

        private static boolean convert(
                ServerLevel level,
                UseOnContext context,
                BlockState state,
                ElectricPoleBlock pole,
                ElectricPoleBlock transformer,
                ResourceLocation profileId
        ) {
            var base = ElectricPoleBlock.basePosition(context.getClickedPos(), state);
            pole.convertStructure(level, base, transformer);
            if (!(level.getBlockEntity(base) instanceof TransformerElectricalHost host)
                    || !host.transformerCoupler().applyProfileFromPlacementData(profileId)) {
                return false;
            }
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            return true;
        }
    }
}
