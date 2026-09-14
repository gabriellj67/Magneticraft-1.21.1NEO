package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.content.worldgen.OilDepositSavedData;
import committee.nova.mods.magneticraft.content.worldgen.OilFieldSurvey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Portable, read-only survey tool for already-generated oil-field records. */
public final class OilProspectorItem extends PortableEnergyItem {
    public static final int ENERGY_CAPACITY_JOULES = 25_000;
    public static final int SCAN_COST_JOULES = 500;
    public static final int SCAN_RADIUS_BLOCKS = 128;

    public OilProspectorItem() {
        super(ENERGY_CAPACITY_JOULES);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        if (!hasEnergy(stack, SCAN_COST_JOULES)) {
            player.displayClientMessage(
                    Component.translatable("message.magneticraft.oil_prospector.insufficient_energy", SCAN_COST_JOULES),
                    false
            );
            return InteractionResult.CONSUME;
        }
        if (!consumeEnergy(stack, SCAN_COST_JOULES)) {
            return InteractionResult.CONSUME;
        }

        BlockPos clicked = context.getClickedPos();
        var survey = OilDepositSavedData.get(level).survey(clicked, SCAN_RADIUS_BLOCKS);
        if (survey.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.magneticraft.oil_prospector.not_found"),
                    false
            );
            return InteractionResult.CONSUME;
        }
        report(player, clicked, survey.get());
        return InteractionResult.CONSUME;
    }

    private static void report(Player player, BlockPos clicked, OilFieldSurvey survey) {
        int deltaX = survey.origin().getX() - clicked.getX();
        int deltaZ = survey.origin().getZ() - clicked.getZ();
        Component direction = Component.translatable(
                "message.magneticraft.oil_prospector.direction." + directionId(deltaX, deltaZ)
        );
        int distance = roundedDistance(deltaX, deltaZ);
        String key = survey.depleted()
                ? "message.magneticraft.oil_prospector.depleted"
                : "message.magneticraft.oil_prospector.result";
        player.displayClientMessage(Component.translatable(
                key,
                direction,
                distance,
                survey.minimumY(),
                survey.maximumY(),
                survey.remainingMillibuckets(),
                survey.remainingPercent()
        ), false);
    }

    static int roundedDistance(int deltaX, int deltaZ) {
        double distance = Math.hypot(deltaX, deltaZ);
        return Math.max(0, (int) Math.round(distance / 8.0D) * 8);
    }

    static String directionId(int deltaX, int deltaZ) {
        if (deltaX == 0 && deltaZ == 0) {
            return "here";
        }
        double angle = Math.atan2(deltaZ, deltaX);
        int octant = Math.floorMod((int) Math.round(angle / (Math.PI / 4.0D)), 8);
        return switch (octant) {
            case 0 -> "east";
            case 1 -> "southeast";
            case 2 -> "south";
            case 3 -> "southwest";
            case 4 -> "west";
            case 5 -> "northwest";
            case 6 -> "north";
            case 7 -> "northeast";
            default -> throw new IllegalStateException("Unreachable octant " + octant);
        };
    }
}
