package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Functional, texture-independent screen shared by every Task 5 device.
 */
public final class SingleBlockMachineScreen extends AbstractContainerScreen<SingleBlockMachineMenu> {
    private static final String[] INSERTER_BUTTON_KEYS = {
            "gui.magneticraft.inserter.whitelist",
            "gui.magneticraft.inserter.match_damage",
            "gui.magneticraft.inserter.match_tags",
            "gui.magneticraft.inserter.match_nbt",
            "gui.magneticraft.inserter.allow_stacking",
            "gui.magneticraft.inserter.reverse"
    };
    private final List<Button> inserterButtons = new ArrayList<>();

    public SingleBlockMachineScreen(SingleBlockMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = menu.imageHeight();
    }

    @Override
    protected void init() {
        super.init();
        inserterButtons.clear();
        if (menu.definition() == SingleBlockMachineDefinition.INSERTER) {
            for (int id = 0; id < INSERTER_BUTTON_KEYS.length; id++) {
                int buttonId = id;
                Button button = Button.builder(
                                inserterButtonLabel(id, false),
                                ignored -> {
                                    if (minecraft != null && minecraft.gameMode != null) {
                                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                                    }
                                }
                        )
                        .bounds(leftPos + 7 + id * 27, topPos + 67, 24, 16)
                        .build();
                inserterButtons.add(addRenderableWidget(button));
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int id = 0; id < inserterButtons.size(); id++) {
            boolean enabled = (menu.flags() & (1 << id)) != 0;
            inserterButtons.get(id).setMessage(inserterButtonLabel(id, enabled));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderStatusTooltips(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
        drawStatusBars(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component status = statusLabel();
        if (status == null) {
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, title, imageWidth - titleLabelX - 8),
                    titleLabelX,
                    titleLabelY,
                    0x404040,
                    false
            );
        } else {
            Component fittedStatus = MachineScreenLayout.fitToWidth(font, status, 72);
            int statusWidth = font.width(fittedStatus);
            int statusX = imageWidth - 8 - statusWidth;
            int titleWidth = MachineScreenLayout.availableTitleWidth(imageWidth, titleLabelX, statusWidth);
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, title, titleWidth),
                    titleLabelX,
                    titleLabelY,
                    0x404040,
                    false
            );
            graphics.drawString(font, fittedStatus, statusX, titleLabelY, 0x404040, false);
        }
        graphics.drawString(
                font,
                playerInventoryTitle,
                inventoryLabelX,
                menu.playerInventoryTop() - 12,
                0x404040,
                false
        );
    }

    private void drawStatusBars(GuiGraphics graphics) {
        int barX = leftPos + 151;
        int barBottom = topPos + 68;
        if (menu.energyCapacity() > 0) {
            MachineScreenLayout.drawInset(graphics, barX, topPos + 18, 10, 52);
            int height = scaled(menu.energyStored(), menu.energyCapacity(), 48);
            graphics.fill(barX + 2, barBottom - height, barX + 8, barBottom, 0xFF43D96B);
            barX -= 13;
        }
        if (menu.primaryCapacity() > 0) {
            MachineScreenLayout.drawInset(graphics, barX, topPos + 18, 10, 52);
            int height = scaled(menu.primaryFluid(), menu.primaryCapacity(), 48);
            graphics.fill(barX + 2, barBottom - height, barX + 8, barBottom, 0xFF2F78D0);
            barX -= 13;
        }
        if (menu.secondaryCapacity() > 0) {
            MachineScreenLayout.drawInset(graphics, barX, topPos + 18, 10, 52);
            int height = scaled(menu.secondaryFluid(), menu.secondaryCapacity(), 48);
            graphics.fill(barX + 2, barBottom - height, barX + 8, barBottom, 0xFFE8E8E8);
        }
        int totalProgress = displayedProgressTotal();
        if (totalProgress > 0) {
            MachineScreenLayout.drawInset(graphics, leftPos + 72, topPos + 65, 34, 8);
            int width = scaled(displayedProgress(), totalProgress, 32);
            graphics.fill(leftPos + 73, topPos + 66, leftPos + 73 + width, topPos + 72, 0xFFE88A2A);
        }
    }

    private Component statusLabel() {
        if (menu.temperatureKelvin() > 0.0D) {
            return Component.translatable(
                    "gui.magneticraft.temperature_celsius",
                    String.format(Locale.ROOT, "%.1f", menu.temperatureKelvin() - 273.15D)
            );
        }
        if (menu.voltage() > 0.0D) {
            return Component.translatable(
                    "gui.magneticraft.voltage",
                    String.format(Locale.ROOT, "%.1f", menu.voltage())
            );
        }
        return menu.working() ? Component.translatable("gui.magneticraft.state.running") : null;
    }

    private Component inserterButtonLabel(int id, boolean enabled) {
        return Component.literal(enabled ? "✓ " : "· ")
                .append(Component.translatable(INSERTER_BUTTON_KEYS[id]));
    }

    private void renderStatusTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int barX = 151;
        if (menu.energyCapacity() > 0) {
            renderBarTooltip(graphics, mouseX, mouseY, barX, "gui.magneticraft.energy.tooltip", menu.energyStored(), menu.energyCapacity());
            barX -= 13;
        }
        if (menu.primaryCapacity() > 0) {
            renderBarTooltip(graphics, mouseX, mouseY, barX, "gui.magneticraft.fluid.tooltip", menu.primaryFluid(), menu.primaryCapacity());
            barX -= 13;
        }
        if (menu.secondaryCapacity() > 0) {
            renderBarTooltip(graphics, mouseX, mouseY, barX, "gui.magneticraft.fluid.tooltip", menu.secondaryFluid(), menu.secondaryCapacity());
        }
        int totalProgress = displayedProgressTotal();
        if (totalProgress > 0
                && mouseX >= leftPos + 72 && mouseX < leftPos + 106
                && mouseY >= topPos + 65 && mouseY < topPos + 73) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(
                    "gui.magneticraft.progress.tooltip",
                    displayedProgress(),
                    totalProgress
            ));
            if (menu.lastConsumption() > 0 || menu.lastProduction() > 0) {
                lines.add(Component.translatable(
                        "gui.magneticraft.machine.rate.tooltip",
                        menu.lastConsumption(),
                        menu.lastProduction()
                ));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    private int displayedProgress() {
        return menu.totalProgress() > 0 ? menu.progress() : menu.burnProgress();
    }

    private int displayedProgressTotal() {
        return menu.totalProgress() > 0 ? menu.totalProgress() : menu.burnTotal();
    }

    private void renderBarTooltip(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            int x,
            String key,
            int value,
            int capacity
    ) {
        if (mouseX >= leftPos + x && mouseX < leftPos + x + 10
                && mouseY >= topPos + 18 && mouseY < topPos + 70) {
            graphics.renderTooltip(font, Component.translatable(key, value, capacity), mouseX, mouseY);
        }
    }

    private static int scaled(int value, int capacity, int size) {
        if (capacity <= 0 || value <= 0) {
            return 0;
        }
        return (int) Math.min(size, (long) value * size / capacity);
    }
}
