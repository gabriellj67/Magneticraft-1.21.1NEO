package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.network.pressure.PressureTankMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;

import java.util.Locale;
import java.util.List;

public final class PressureTankScreen extends AbstractContainerScreen<PressureTankMenu> {
    public PressureTankScreen(PressureTankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = LegacyMachineGuiLayout.STANDARD_WIDTH;
        imageHeight = LegacyMachineGuiLayout.STANDARD_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawHeader(graphics, leftPos, topPos, imageWidth);
        MachineScreenLayout.drawCard(graphics, leftPos + 5, topPos + 17, imageWidth - 10, 56);
        MachineScreenLayout.drawPlayerInventory(graphics, leftPos, topPos);
        MachineScreenLayout.drawInset(graphics, leftPos + 10, topPos + 20, 156, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, imageWidth - 16),
                titleLabelX, titleLabelY, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, gasName(), 16, 25, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable(
                "gui.magneticraft.pressure.value",
                formatted(menu.pressureKpa()),
                formatted(menu.pressureKpa() / 100.0D),
                formatted(menu.pressureKpa() * 0.1450377377D)
        ), 16, 37, MachineScreenLayout.TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable(
                "gui.magneticraft.pressure.amount",
                formatted(menu.gasKpaLiters()),
                formatted(menu.capacityKpaLiters())
        ), 16, 49, MachineScreenLayout.TEXT_MUTED, false);
        if (menu.fillRatio() >= 0.9D) {
            graphics.drawString(font, Component.translatable("gui.magneticraft.pressure.warning"),
                    16, 61, MachineScreenLayout.WARNING, false);
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
    }

    private Component gasName() {
        ResourceLocation id = menu.gasId().orElse(null);
        if (id == null || !BuiltInRegistries.FLUID.containsKey(id)) {
            return Component.translatable("gui.magneticraft.pressure.empty");
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(id);
        return fluid.getFluidType().getDescription();
    }

    private static String formatted(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    MachineScreenBounds.Layout layout() {
        return layout(imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(int imageWidth, int imageHeight) {
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder("pressure_tank", imageWidth, imageHeight)
                .element("header", new LegacyMachineGuiLayout.Rect(1, 1, imageWidth - 2, 15), "sections", 1)
                .element("telemetry", new LegacyMachineGuiLayout.Rect(5, 17, imageWidth - 10, 56), "sections", 1)
                .child("pressure_readout", new LegacyMachineGuiLayout.Rect(10, 20, 156, 52),
                        "telemetry", null, 0)
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(5, 78, 166, 86), "sections", 1);
        List<LegacyMachineGuiLayout.Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT, LegacyMachineGuiLayout.STANDARD_PLAYER_TOP
        );
        for (int index = 0; index < playerSlots.size(); index++) {
            builder.child("player_slot_" + index, playerSlots.get(index),
                    "player_inventory", "player_slots", 0);
        }
        return builder.build();
    }
}
