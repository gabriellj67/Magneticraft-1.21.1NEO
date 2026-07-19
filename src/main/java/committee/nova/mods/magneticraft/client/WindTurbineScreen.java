package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class WindTurbineScreen extends AbstractContainerScreen<WindTurbineMenu> {
    private static final int ENERGY_X = 142;
    private static final int ENERGY_Y = 22;
    private static final int ENERGY_HEIGHT = 46;

    public WindTurbineScreen(WindTurbineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = LegacyMachineGuiLayout.STANDARD_WIDTH;
        imageHeight = LegacyMachineGuiLayout.STANDARD_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout(imageWidth, imageHeight));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (inside(mouseX, mouseY, ENERGY_X, ENERGY_Y, 12, ENERGY_HEIGHT)) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.magneticraft.energy.tooltip",
                    menu.energyStored(),
                    menu.energyCapacity()
            ), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, 52, 24, 80, 42)) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.magneticraft.wind_turbine.environment.tooltip",
                    percent(menu.wind()),
                    percent(menu.openSpace())
            ), mouseX, mouseY);
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawHeader(graphics, leftPos, topPos, imageWidth);
        MachineScreenLayout.drawCard(graphics, leftPos + 5, topPos + 17, 166, 55);
        MachineScreenLayout.drawPlayerInventory(graphics, leftPos, topPos);
        MachineScreenLayout.drawSlot(
                graphics, leftPos, topPos,
                WindTurbineMenu.ROTOR_SLOT_X, WindTurbineMenu.ROTOR_SLOT_Y
        );
        int capacity = menu.energyCapacity();
        int fill = capacity <= 0 ? 0 : ENERGY_HEIGHT * menu.energyStored() / capacity;
        MachineScreenLayout.drawStatusBar(
                graphics, leftPos + ENERGY_X, topPos + ENERGY_Y, 12, ENERGY_HEIGHT,
                fill, MachineScreenLayout.ENERGY, true
        );
        Component rotor = menu.rotorTier() == 0
                ? Component.translatable("gui.magneticraft.wind_turbine.no_rotor")
                : Component.translatable("gui.magneticraft.wind_turbine.rotor_tier." + menu.rotorTier());
        graphics.drawString(font, rotor, leftPos + 52, topPos + 24, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, Component.translatable(
                        "gui.magneticraft.wind_turbine.output",
                        String.format(java.util.Locale.ROOT, "%.1f", menu.productionJoulesPerTick())
                ), leftPos + 52, topPos + 38, MachineScreenLayout.TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable(
                        "gui.magneticraft.wind_turbine.wind", percent(menu.wind())
                ), leftPos + 52, topPos + 50, MachineScreenLayout.TEXT_MUTED, false);
        graphics.drawString(font, Component.translatable(
                        "gui.magneticraft.wind_turbine.clearance", percent(menu.openSpace())
                ), leftPos + 52, topPos + 61, MachineScreenLayout.TEXT_MUTED, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, MachineScreenLayout.fitToWidth(font, title, imageWidth - 16),
                titleLabelX, titleLabelY, MachineScreenLayout.TEXT_PRIMARY, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
    }

    static MachineScreenBounds.Layout layout(int width, int height) {
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder("wind_turbine", width, height)
                .element("header", new LegacyMachineGuiLayout.Rect(1, 1, width - 2, 15), "sections", 1)
                .element("machine", new LegacyMachineGuiLayout.Rect(5, 17, 166, 55), "sections", 1)
                .child("rotor_slot", new LegacyMachineGuiLayout.Rect(
                        WindTurbineMenu.ROTOR_SLOT_X - 1, WindTurbineMenu.ROTOR_SLOT_Y - 1, 18, 18
                ), "machine", "machine_controls", 0)
                .child("status", new LegacyMachineGuiLayout.Rect(52, 24, 80, 42),
                        "machine", "machine_controls", 0)
                .child("energy", new LegacyMachineGuiLayout.Rect(ENERGY_X, ENERGY_Y, 12, ENERGY_HEIGHT),
                        "machine", "machine_controls", 0)
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(5, 78, 166, 86), "sections", 1);
        List<LegacyMachineGuiLayout.Rect> slots = LegacyMachineGuiLayout.playerInventorySlots(
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                LegacyMachineGuiLayout.STANDARD_PLAYER_TOP
        );
        for (int index = 0; index < slots.size(); index++) {
            builder.child("player_slot_" + index, slots.get(index), "player_inventory", "player_slots", 0);
        }
        return builder.build();
    }

    private boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static int percent(double value) {
        return (int) Math.round(Math.max(0.0D, Math.min(1.0D, value)) * 100.0D);
    }
}
