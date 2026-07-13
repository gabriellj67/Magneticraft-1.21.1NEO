package committee.nova.mods.magneticraft.content.network.electric;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/** Legacy transformer item upgrades an existing pole instead of placing a second pole. */
public final class ElectricPoleTransformerBlockItem extends BlockItem {
    private final ElectricPoleBlock transformer;

    public ElectricPoleTransformerBlockItem(ElectricPoleBlock transformer, Properties properties) {
        super(transformer, properties);
        this.transformer = transformer;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof ElectricPoleBlock pole) || pole.isTransformer()) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel level) {
            BlockPosHolder.convert(level, context, state, pole, transformer);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static final class BlockPosHolder {
        private BlockPosHolder() {
        }

        private static void convert(
                ServerLevel level,
                UseOnContext context,
                BlockState state,
                ElectricPoleBlock pole,
                ElectricPoleBlock transformer
        ) {
            pole.convertStructure(level, ElectricPoleBlock.basePosition(context.getClickedPos(), state), transformer);
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
    }
}
