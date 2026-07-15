package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout.StatusBar;
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
        imageWidth = LegacyMachineGuiLayout.singleBlockSize(menu.definition()).width();
        imageHeight = menu.imageHeight();
    }

    @Override
    protected void init() {
        super.init();
        inserterButtons.clear();
        if (menu.definition() == SingleBlockMachineDefinition.INSERTER) {
            for (int id = 0; id < INSERTER_BUTTON_KEYS.length; id++) {
                int buttonId = id;
                LegacyMachineGuiLayout.Rect bounds = LegacyMachineGuiLayout.inserterButtons().get(id);
                Button button = Button.builder(
                                inserterButtonLabel(id, false),
                                ignored -> {
                                    if (minecraft != null && minecraft.gameMode != null) {
                                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
                                    }
                                }
                        )
                        .bounds(
                                leftPos + bounds.x(),
                                topPos + bounds.y(),
                                bounds.width(),
                                bounds.height()
                        )
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
        for (int id = 0; id < inserterButtons.size(); id++) {
            if (inserterButtons.get(id).isHoveredOrFocused()) {
                graphics.renderTooltip(
                        font,
                        Component.translatable(INSERTER_BUTTON_KEYS[id]),
                        mouseX,
                        mouseY
                );
                break;
            }
        }
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
        if (menu.definition() == SingleBlockMachineDefinition.BOX) {
            return;
        }
        Component status = statusLabel();
        if (status == null) {
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, title, imageWidth - titleLabelX - 8),
                    titleLabelX,
                    LegacyMachineGuiLayout.CONTENT_TITLE_TOP,
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
                    LegacyMachineGuiLayout.CONTENT_TITLE_TOP,
                    0x404040,
                    false
            );
            graphics.drawString(
                    font,
                    fittedStatus,
                    statusX,
                    LegacyMachineGuiLayout.CONTENT_TITLE_TOP,
                    0x404040,
                    false
            );
        }
    }

    private void drawStatusBars(GuiGraphics graphics) {
        for (StatusBar bar : statusBars()) {
            LegacyMachineGuiLayout.Rect bounds = bar.bounds();
            MachineScreenLayout.drawInset(
                    graphics,
                    leftPos + bounds.x(),
                    topPos + bounds.y(),
                    bounds.width(),
                    bounds.height()
            );
            int height = scaled(value(bar), capacity(bar), bounds.height() - 4);
            graphics.fill(
                    leftPos + bounds.x() + 2,
                    topPos + bounds.bottom() - 2 - height,
                    leftPos + bounds.right() - 2,
                    topPos + bounds.bottom() - 2,
                    color(bar)
            );
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
        return Component.literal((enabled ? "✓" : "·") + (id + 1));
    }

    private void renderStatusTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        for (StatusBar bar : statusBars()) {
            LegacyMachineGuiLayout.Rect bounds = bar.bounds();
            if (mouseX < leftPos + bounds.x() || mouseX >= leftPos + bounds.right()
                    || mouseY < topPos + bounds.y() || mouseY >= topPos + bounds.bottom()) {
                continue;
            }
            String key = switch (bar.kind()) {
                case ENERGY -> "gui.magneticraft.energy.tooltip";
                case PROGRESS -> "gui.magneticraft.progress.tooltip";
                case PRIMARY_FLUID, SECONDARY_FLUID, TANK -> "gui.magneticraft.fluid.tooltip";
                case BULK -> "gui.magneticraft.items.tooltip";
            };
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(
                    key,
                    value(bar),
                    capacity(bar)
            ));
            if (bar.kind() == LegacyMachineGuiLayout.StatusKind.PROGRESS
                    && (menu.lastConsumption() > 0 || menu.lastProduction() > 0)) {
                lines.add(Component.translatable(
                        "gui.magneticraft.machine.rate.tooltip",
                        menu.lastConsumption(),
                        menu.lastProduction()
                ));
            }
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
    }

    private List<StatusBar> statusBars() {
        return LegacyMachineGuiLayout.singleBlockStatusBars(
                menu.energyCapacity() > 0,
                menu.primaryCapacity() > 0,
                menu.secondaryCapacity() > 0,
                displayedProgressTotal() > 0
        );
    }

    private int value(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> menu.energyStored();
            case PRIMARY_FLUID -> menu.primaryFluid();
            case SECONDARY_FLUID -> menu.secondaryFluid();
            case PROGRESS -> displayedProgress();
            case BULK, TANK -> 0;
        };
    }

    private int capacity(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> menu.energyCapacity();
            case PRIMARY_FLUID -> menu.primaryCapacity();
            case SECONDARY_FLUID -> menu.secondaryCapacity();
            case PROGRESS -> displayedProgressTotal();
            case BULK, TANK -> 0;
        };
    }

    private int color(StatusBar bar) {
        return switch (bar.kind()) {
            case ENERGY -> 0xFF43D96B;
            case PRIMARY_FLUID -> 0xFF2F78D0;
            case SECONDARY_FLUID -> 0xFFE8E8E8;
            case PROGRESS -> 0xFFE88A2A;
            case BULK, TANK -> 0xFF4FC3C8;
        };
    }

    private int displayedProgress() {
        return menu.totalProgress() > 0 ? menu.progress() : menu.burnProgress();
    }

    private int displayedProgressTotal() {
        return menu.totalProgress() > 0 ? menu.totalProgress() : menu.burnTotal();
    }

    private static int scaled(int value, int capacity, int size) {
        if (capacity <= 0 || value <= 0) {
            return 0;
        }
        return (int) Math.min(size, (long) value * size / capacity);
    }
}
