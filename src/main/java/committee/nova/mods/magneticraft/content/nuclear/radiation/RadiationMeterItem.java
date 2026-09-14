package committee.nova.mods.magneticraft.content.nuclear.radiation;

import committee.nova.mods.magneticraft.api.nuclear.radiation.RadiationSource;
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

/** Personal dosimeter or directional Geiger counter; all readings are server authoritative. */
public final class RadiationMeterItem extends Item {
    public enum Kind { DOSIMETER, GEIGER_COUNTER }

    private final Kind kind;

    public RadiationMeterItem(Kind kind) {
        super(new Properties().stacksTo(1));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var exposure = RadiationExposureService.exposure(player);
            player.displayClientMessage(Component.translatable(
                    kind == Kind.DOSIMETER
                            ? "message.magneticraft.dosimeter_reading"
                            : "message.magneticraft.geiger_ambient_reading",
                    format(exposure.lastDoseRateMillisievertsPerHour()),
                    format(exposure.cumulativeDoseMillisieverts()),
                    format(exposure.contaminationMillisieverts())), false);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (kind != Kind.GEIGER_COUNTER) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!level.isClientSide && player != null) {
            var entity = level.getBlockEntity(context.getClickedPos());
            if (entity instanceof RadiationSource source) {
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft.geiger_source_reading",
                        format(source.doseRateMillisievertsPerHour()),
                        Component.translatable(source.contaminationSource()
                                ? "message.magneticraft.contamination.present"
                                : "message.magneticraft.contamination.absent")), false);
            } else {
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft.geiger_no_source"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
