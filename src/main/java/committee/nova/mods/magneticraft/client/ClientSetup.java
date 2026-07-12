package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.guide.GuideRepository;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only screen registration boundary.
 */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.BATTERY.get(), BatteryScreen::new);
            MenuScreens.register(ModMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
            MenuScreens.register(ModMenus.SINGLE_BLOCK_MACHINE.get(), SingleBlockMachineScreen::new);
            MenuScreens.register(ModMenus.ADVANCED_MULTIBLOCK.get(), AdvancedMultiblockScreen::new);
            MenuScreens.register(ModMenus.PROGRAMMABLE.get(), ProgrammableScreen::new);
            BlockEntityRenderers.register(ModBlockEntities.CRUSHING_TABLE.get(), CrushingTableRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CONVEYOR_BELT.get(), ConveyorBeltRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PNEUMATIC_TUBE.get(), PneumaticTubeRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.SINGLE_BLOCK_MACHINE.get(), SingleBlockMachineRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ADVANCED_MULTIBLOCK.get(), AdvancedMultiblockRenderer::new);
            BlockEntityRenderers.register(ModComputerContent.COMPUTER_BLOCK_ENTITY.get(), ComputerRenderer::new);
            BlockEntityRenderers.register(ModComputerContent.MINING_ROBOT_BLOCK_ENTITY.get(), MiningRobotRenderer::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(GuideRepository.INSTANCE);
    }
}
