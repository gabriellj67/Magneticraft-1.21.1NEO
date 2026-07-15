package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.system.network.diagnostic.DiagnosticHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Locale;

/** Server-authoritative, read-only three-mode probe for loaded Magneticraft electrical nodes. */
public final class VoltmeterItem extends Item {
    static final String MODE_TAG = "diagnostic_mode";

    public VoltmeterItem() {
        this(new Properties().stacksTo(1));
    }

    public VoltmeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            Mode next = mode(stack).next();
            setMode(stack, next);
            player.displayClientMessage(
                    Component.translatable(
                            "message.magneticraft.voltmeter.mode",
                            Component.translatable(next.translationKey())
                    ),
                    true
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return inspect(
                context.getLevel(),
                context.getClickedPos(),
                context.getClickedFace(),
                context.getPlayer(),
                context.getItemInHand()
        );
    }

    static InteractionResult inspect(
            Level level,
            BlockPos position,
            Direction side,
            Player player,
            ItemStack stack
    ) {
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (!(blockEntity instanceof DiagnosticHost host)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.PASS;
        }
        switch (mode(stack)) {
            case POINT -> showPoint(host, side, player);
            case NETWORK_SUMMARY -> showNetworkSummary(host, side, player);
            case FAULT_LOCATOR -> showFaultLocation(host, side, player);
        }
        return InteractionResult.PASS;
    }

    static Mode mode(ItemStack stack) {
        if (!stack.hasTag()) {
            return Mode.POINT;
        }
        return Mode.fromId(stack.getTag().getString(MODE_TAG));
    }

    static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateTag().putString(MODE_TAG, mode.id());
    }

    private static void showPoint(DiagnosticHost host, Direction side, Player player) {
        host.electricalReading(side).ifPresent(value -> player.displayClientMessage(
                Component.translatable(
                        "message.magneticraft.voltmeter.point",
                        value.terminalId(),
                        tierName(value.tierId()),
                        format(value.voltageVolts()),
                        format(value.chargeCoulombsPerTick()),
                        format(value.currentAmps()),
                        format(value.joulesPerTick()),
                        format(value.powerWatts()),
                        Component.translatable(value.flowDirection().translationKey()),
                        formatPercent(value.loadRatio()),
                        Component.translatable(value.faultKind().translationKey())
                ),
                false
        ));
    }

    private static void showNetworkSummary(DiagnosticHost host, Direction side, Player player) {
        host.electricalNetworkSummary(side, PhysicalNetworkManager.MAX_ELECTRICAL_DIAGNOSTIC_VISITS)
                .ifPresent(value -> player.displayClientMessage(
                        Component.translatable(
                                "message.magneticraft.voltmeter.network",
                                value.nodeCount(),
                                value.edgeCount(),
                                format(value.storedJoules()),
                                format(value.generatedJoulesPerTick()),
                                format(value.consumedJoulesPerTick()),
                                format(value.lostJoulesPerTick()),
                                formatPercent(value.maximumLoadRatio()),
                                value.faultCount(),
                                value.truncated()
                                        ? Component.translatable("message.magneticraft.voltmeter.truncated")
                                        : Component.empty()
                        ),
                        false
                ));
    }

    private static void showFaultLocation(DiagnosticHost host, Direction side, Player player) {
        host.nearestElectricalFault(side, PhysicalNetworkManager.MAX_ELECTRICAL_DIAGNOSTIC_VISITS)
                .ifPresent(result -> result.location().ifPresentOrElse(
                        location -> player.displayClientMessage(
                                Component.translatable(
                                        "message.magneticraft.voltmeter.fault",
                                        Component.translatable(location.faultKind().translationKey()),
                                        location.terminalId(),
                                        location.direction()
                                                .<Component>map(direction -> Component.translatable(
                                                        "direction.minecraft." + direction.getName()
                                                ))
                                                .orElseGet(() -> Component.translatable(
                                                        "message.magneticraft.voltmeter.here"
                                                )),
                                        format(location.distanceBlocks()),
                                        result.truncated()
                                                ? Component.translatable("message.magneticraft.voltmeter.truncated")
                                                : Component.empty()
                                ),
                                false
                        ),
                        () -> player.displayClientMessage(
                                Component.translatable(
                                        "message.magneticraft.voltmeter.no_fault",
                                        result.visitedNodes(),
                                        result.truncated()
                                                ? Component.translatable("message.magneticraft.voltmeter.truncated")
                                                : Component.empty()
                                ),
                                false
                        )
                ));
    }

    private static Component tierName(ResourceLocation tierId) {
        return ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(tierId))
                .<Component>map(tier -> Component.translatable(tier.translationKey()))
                .orElseGet(() -> Component.literal(tierId.toString()));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatPercent(double ratio) {
        return String.format(Locale.ROOT, "%.1f", ratio * 100.0D);
    }

    enum Mode {
        POINT("point"),
        NETWORK_SUMMARY("network_summary"),
        FAULT_LOCATOR("fault_locator");

        private final String id;

        Mode(String id) {
            this.id = id;
        }

        String id() {
            return id;
        }

        String translationKey() {
            return "mode.magneticraft.voltmeter." + id;
        }

        Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        static Mode fromId(String id) {
            for (Mode value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            return POINT;
        }
    }
}
