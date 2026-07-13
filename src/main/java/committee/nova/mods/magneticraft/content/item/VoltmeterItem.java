package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.system.network.diagnostic.DiagnosticHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Locale;

/**
 * Server-authoritative read-only probe for Magneticraft electrical nodes.
 */
public final class VoltmeterItem extends Item {
    private static final String MESSAGE_KEY = "message.magneticraft.voltmeter";

    public VoltmeterItem() {
        this(new Properties().stacksTo(1));
    }

    public VoltmeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return inspect(
                context.getLevel(),
                context.getClickedPos(),
                context.getClickedFace(),
                context.getPlayer()
        );
    }

    static InteractionResult inspect(Level level, BlockPos position, Direction side, Player player) {
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof DiagnosticHost host)) {
            return InteractionResult.PASS;
        }
        var reading = host.electricalReading(side);
        if (reading.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ElectricalDiagnosticSource.ElectricalReading value = reading.get();
            player.displayClientMessage(
                    Component.translatable(
                            MESSAGE_KEY,
                            format(value.voltageVolts()),
                            format(value.currentAmps()),
                            format(value.powerWatts())
                    ),
                    true
            );
        }
        return InteractionResult.PASS;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
