package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlock;
import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalHost;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalPlacementHost;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Block item that applies only a validated electrical identity, never stored node energy. */
public class TieredElectricalBlockItem extends BlockItem {
    private static final String INVALID_TIER_MESSAGE = "message.magneticraft.invalid_voltage_tier";

    public TieredElectricalBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        Optional<TieredElectricalItemData> itemData = TieredElectricalItemData.read(stack);
        if (itemData.isEmpty()) {
            reportInvalidTier(context.getPlayer(), "missing");
            return InteractionResult.FAIL;
        }
        if (!context.getLevel().isClientSide && ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(itemData.get().tierId()))
                .isEmpty()) {
            reportInvalidTier(context.getPlayer(), itemData.get().tierId().toString());
            return InteractionResult.FAIL;
        }

        InteractionResult result = super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide) {
            applyToPlacedHost(context.getLevel(), context.getClickedPos(), itemData.get());
        }
        return result;
    }

    public static ItemStack stackForTier(Block block, net.minecraft.resources.ResourceLocation tierId) {
        ItemStack stack = new ItemStack(block);
        TieredElectricalItemData.forTier(tierId).write(stack);
        return stack;
    }

    private static void applyToPlacedHost(Level level, BlockPos placedPosition, TieredElectricalItemData data) {
        BlockState state = level.getBlockState(placedPosition);
        BlockPos hostPosition = state.getBlock() instanceof ElectricPoleBlock
                ? ElectricPoleBlock.basePosition(placedPosition, state)
                : placedPosition;
        BlockEntity blockEntity = level.getBlockEntity(hostPosition);
        if (blockEntity instanceof TieredElectricalPlacementHost placementHost) {
            placementHost.applyElectricalItemData(data);
            return;
        }
        if (!(blockEntity instanceof TieredElectricalHost host)) {
            return;
        }
        ElectricalNetworkModule electricity = host.tieredElectricalModule();
        if (electricity != null) {
            electricity.applyTierFromPlacementData(data.tierId());
            if (state.getBlock() instanceof ConduitBlock) {
                ConduitBlock.refreshAround(level, placedPosition);
            }
        }
    }

    private static void reportInvalidTier(Player player, String tier) {
        if (player != null && !player.level().isClientSide) {
            player.displayClientMessage(Component.translatable(INVALID_TIER_MESSAGE, tier), true);
        }
    }
}
