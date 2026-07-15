package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlock;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceEndpoint;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceEndpointHost;
import committee.nova.mods.magneticraft.system.network.longdistance.WireSelectionPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Reusable endpoint-selection tool for player-created long-distance wires. */
public final class CopperWireCoilItem extends Item {
    private static final String UPDATED_POSITION_MESSAGE =
            "text.magneticraft.wire_connect.updated_position";
    private static final String SUCCESS_MESSAGE = "text.magneticraft.wire_connect.success";
    private static final String TOO_FAR_MESSAGE = "text.magneticraft.wire_connect.too_far";
    private static final String NOT_A_CONNECTOR_MESSAGE =
            "text.magneticraft.wire_connect.not_a_connector";
    private static final String INVALID_CONNECTOR_MESSAGE =
            "text.magneticraft.wire_connect.invalid_connector";
    private static final String SAME_CONNECTOR_MESSAGE =
            "text.magneticraft.wire_connect.same_connector";
    private static final String ALREADY_CONNECTED_MESSAGE =
            "text.magneticraft.wire_connect.already_connected";
    private static final String NO_OTHER_CONNECTOR_MESSAGE =
            "text.magneticraft.wire_connect.no_other_connector";

    public CopperWireCoilItem() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Optional<LongDistanceEndpoint> endpoint = resolveEndpoint(level, context.getClickedPos(), context.getClickedFace());
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return endpoint.isPresent() ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel serverLevel) || endpoint.isEmpty()) {
            return InteractionResult.PASS;
        }
        BlockPos position = endpoint.get().position();

        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) {
            WireSelectionPayload.write(
                    stack.getOrCreateTag(),
                    new WireSelectionPayload.Selection(serverLevel.dimension().location(), endpoint.get())
            );
            player.displayClientMessage(Component.translatable(
                    UPDATED_POSITION_MESSAGE,
                    position.getX(),
                    position.getY(),
                    position.getZ()
            ), false);
            return InteractionResult.CONSUME;
        }

        CompoundTag owner = stack.getTag();
        Optional<WireSelectionPayload.Selection> selection = owner == null ? Optional.empty()
                : WireSelectionPayload.read(owner);
        if (selection.isEmpty()) {
            player.displayClientMessage(Component.translatable(NO_OTHER_CONNECTOR_MESSAGE), false);
            return InteractionResult.PASS;
        }
        if (!selection.get().dimension().equals(serverLevel.dimension().location())) {
            player.displayClientMessage(Component.translatable(INVALID_CONNECTOR_MESSAGE), false);
            return InteractionResult.CONSUME;
        }

        var attempt = LongDistanceElectricityService.get(serverLevel)
                .connect(selection.get().endpoint(), endpoint.get());
        player.displayClientMessage(Component.translatable(messageKey(attempt.result())), false);
        if (attempt.result() == LongDistanceElectricityService.ConnectionResult.SUCCESS) {
            WireSelectionPayload.clear(stack.getOrCreateTag());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            CompoundTag owner = stack.getTag();
            if (owner != null) {
                WireSelectionPayload.clear(owner);
                if (owner.isEmpty()) {
                    stack.setTag(null);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static Optional<LongDistanceEndpoint> resolveEndpoint(
            Level level,
            BlockPos clickedPosition,
            net.minecraft.core.Direction clickedFace
    ) {
        if (!level.isLoaded(clickedPosition)) {
            return Optional.empty();
        }
        var state = level.getBlockState(clickedPosition);
        BlockPos endpointPosition = state.getBlock() instanceof ElectricPoleBlock
                ? ElectricPoleBlock.basePosition(clickedPosition, state)
                : clickedPosition;
        if (!level.isLoaded(endpointPosition)
                || !(level.getBlockEntity(endpointPosition) instanceof LongDistanceEndpointHost host)) {
            return Optional.empty();
        }
        return host.longDistanceEndpointForInteraction(clickedFace);
    }

    private static String messageKey(LongDistanceElectricityService.ConnectionResult result) {
        return switch (result) {
            case SUCCESS -> SUCCESS_MESSAGE;
            case ENDPOINT_UNLOADED -> NOT_A_CONNECTOR_MESSAGE;
            case SAME_ENDPOINT -> SAME_CONNECTOR_MESSAGE;
            case INCOMPATIBLE_PORT, INCOMPATIBLE_TIER, MISSING_PROFILE -> INVALID_CONNECTOR_MESSAGE;
            case TOO_FAR -> TOO_FAR_MESSAGE;
            case ALREADY_CONNECTED -> ALREADY_CONNECTED_MESSAGE;
        };
    }
}
