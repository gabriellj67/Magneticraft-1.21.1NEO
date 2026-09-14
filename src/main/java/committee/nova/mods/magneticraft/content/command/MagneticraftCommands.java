package committee.nova.mods.magneticraft.content.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlock;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockStructureFiller;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Magneticraft operator commands. */
@EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class MagneticraftCommands {
    private static final double TARGET_DISTANCE = 16.0D;

    private MagneticraftCommands() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("magneticraft")
                .then(Commands.literal("multiblock")
                        .then(Commands.literal("fill")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> fillTargetedController(context.getSource())))));
    }

    private static int fillTargetedController(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(TARGET_DISTANCE, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(player.level().getBlockState(blockHit.getBlockPos()).getBlock()
                instanceof AdvancedMultiblockBlock)) {
            source.sendFailure(Component.translatable(
                    "commands.magneticraft.multiblock.fill.no_controller"
            ));
            return 0;
        }
        if (!(player.level().getBlockEntity(blockHit.getBlockPos())
                instanceof AdvancedMultiblockBlockEntity controller)) {
            source.sendFailure(Component.translatable(
                    "commands.magneticraft.multiblock.fill.no_controller"
            ));
            return 0;
        }
        if (controller.formed()) {
            source.sendFailure(Component.translatable(
                    "commands.magneticraft.multiblock.fill.already_formed"
            ));
            return 0;
        }

        MultiblockStructureFiller.Result result = MultiblockStructureFiller.fill(
                player.serverLevel(),
                controller
        );
        if (!result.success()) {
            source.sendFailure(Component.translatable(
                    "commands.magneticraft.multiblock.fill.unavailable",
                    result.unavailablePosition().toShortString()
            ));
            return 0;
        }

        Component machineName = Component.translatable(controller.getBlockState().getBlock().getDescriptionId());
        source.sendSuccess(() -> Component.translatable(
                "commands.magneticraft.multiblock.fill.success",
                machineName,
                result.changedBlocks()
        ), false);
        return 1;
    }
}
