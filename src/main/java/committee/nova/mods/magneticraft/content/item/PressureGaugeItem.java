package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.system.network.diagnostic.DiagnosticHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.PressureDiagnosticSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/**
 * Face-aware typed-gas pressure probe.
 */
public final class PressureGaugeItem extends Item {
    public PressureGaugeItem() {
        super(new Properties().stacksTo(1));
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
        var reading = host.pressureReading(side);
        if (reading.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            show(player, reading.get());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void show(Player player, PressureDiagnosticSource.PressureReading reading) {
        Component gas = reading.gasId()
                .<Component>map(PressureGaugeItem::gasName)
                .orElseGet(() -> Component.translatable("gui.magneticraft.pressure.empty"));
        player.displayClientMessage(Component.translatable(
                "message.magneticraft.pressure_gauge",
                gas,
                format(reading.pressureKpa()),
                format(reading.pressureKpa() / 100.0D),
                format(reading.pressureKpa() * 0.1450377377D),
                format(reading.gasKpaLiters()),
                format(reading.capacityKpaLiters()),
                reading.warning()
                        ? Component.translatable("message.magneticraft.pressure_gauge.warning")
                        : Component.empty()
        ), false);
    }

    private static Component gasName(ResourceLocation id) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
        return fluid == null
                ? Component.literal(id.toString())
                : fluid.getFluidType().getDescription();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
