package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.guide.GuideRepository;
import committee.nova.mods.magneticraft.client.model.LegacyModelLoader;
import committee.nova.mods.magneticraft.client.model.ModelRenderManifestRegistry;
import committee.nova.mods.magneticraft.content.computer.FloppyDiskItem;
import committee.nova.mods.magneticraft.content.item.ElectricalFuseVisualVariant;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
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
            MenuScreens.register(ModMenus.ELECTRICAL_DEVICE.get(), ElectricalDeviceScreen::new);
            MenuScreens.register(ModMenus.PRESSURE_TANK.get(), PressureTankScreen::new);
            MenuScreens.register(ModMenus.NUCLEAR_FACILITY.get(), NuclearFacilityScreen::new);
            ItemProperties.register(
                    ModComputerContent.FLOPPY_DISK.get(),
                    Magneticraft.id("floppy_variant"),
                    (stack, level, entity, seed) -> FloppyDiskItem.visualVariant(stack).textureIndex()
            );
            ItemProperties.register(
                    ModNetworkItems.FUSE.get(),
                    Magneticraft.id("fuse_variant"),
                    (stack, level, entity, seed) -> ElectricalFuseVisualVariant.from(stack)
                            .map(ElectricalFuseVisualVariant::predicateValue)
                            .orElse(0)
            );
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
            BlockEntityRenderers.register(ModBlockEntities.BOX_TRANSFORMER.get(), ElectricalDeviceRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.FUSE_BOX.get(), ElectricalDeviceRenderer::new);
            BlockEntityRenderers.register(ModBlockEntities.CIRCUIT_BREAKER.get(), ElectricalDeviceRenderer::new);
        });
    }

    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register(
                ElectricalTierColors.blockColor(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.ELECTRIC_CABLE.get(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.BOX_TRANSFORMER.get(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.FUSE_BOX.get(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.CIRCUIT_BREAKER.get(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.ELECTRIC_SWITCH.get(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.DIODE.get(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.RESISTOR.get()
        );
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                ElectricalTierColors.itemColor(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.ELECTRIC_CABLE.get().asItem(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.BOX_TRANSFORMER.get().asItem(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.FUSE_BOX.get().asItem(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.CIRCUIT_BREAKER.get().asItem(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.ELECTRIC_SWITCH.get().asItem(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.DIODE.get().asItem(),
                committee.nova.mods.magneticraft.init.ModNetworkBlocks.RESISTOR.get().asItem()
        );
    }

    @SubscribeEvent
    public static void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("legacy_scene", LegacyModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(LegacyModelLoader.INSTANCE);
        event.registerReloadListener(ModelRenderManifestRegistry.INSTANCE);
        event.registerReloadListener(GuideRepository.INSTANCE);
    }
}
