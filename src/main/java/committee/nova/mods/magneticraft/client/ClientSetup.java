package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.guide.GuideRepository;
import committee.nova.mods.magneticraft.client.model.LegacyModelLoader;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.ModelEvent;
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
            ModMenus.singleBlockMachines().values().forEach(type ->
                    MenuScreens.register(type.get(), SingleBlockMachineScreen::new));
            ModMenus.advancedMultiblocks().values().forEach(type ->
                    MenuScreens.register(type.get(), AdvancedMultiblockScreen::new));
            MenuScreens.register(ModMenus.COMPUTER.get(), ProgrammableScreen::new);
            MenuScreens.register(ModMenus.MINING_ROBOT.get(), ProgrammableScreen::new);
            BlockEntityRenderers.register(ModBlockEntities.CRUSHING_TABLE.get(), CrushingTableRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CONVEYOR_BELT.get(), ConveyorBeltRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.PNEUMATIC_TUBE.get(), PneumaticTubeRenderer::new);
            BlockEntityRenderers.register(
                    ModBlockEntities.PNEUMATIC_RESTRICTION_TUBE.get(),
                    PneumaticTubeRenderer::new
            );
            BlockEntityRenderers.register(ModBlockEntities.ELECTRIC_CONNECTOR.get(), LongDistanceWireRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ELECTRIC_POLE.get(), LongDistanceWireRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.ELECTRIC_POLE_TRANSFORMER.get(), LongDistanceWireRenderer::new);
            ModBlockEntities.singleBlockMachines().values().forEach(type ->
                    BlockEntityRenderers.register(type.get(), SingleBlockMachineRenderer::new));
            ModBlockEntities.advancedMultiblocks().values().forEach(type ->
                    BlockEntityRenderers.register(type.get(), AdvancedMultiblockRenderer::new));
            BlockEntityRenderers.register(ModComputerContent.COMPUTER_BLOCK_ENTITY.get(), ComputerRenderer::new);
            BlockEntityRenderers.register(ModComputerContent.MINING_ROBOT_BLOCK_ENTITY.get(), MiningRobotRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.WIND_TURBINE.get(), WindTurbineRenderer::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("legacy_scene", LegacyModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(LegacyModelLoader.INSTANCE);
        event.registerReloadListener(GuideRepository.INSTANCE);
    }
}
