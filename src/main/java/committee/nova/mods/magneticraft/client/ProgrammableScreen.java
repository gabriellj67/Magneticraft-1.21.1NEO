package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.computer.ProgrammableMenu;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.network.ModNetwork;
import committee.nova.mods.magneticraft.network.UploadComputerProgramMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bounded line editor for computers and mining robots.
 */
public final class ProgrammableScreen extends AbstractContainerScreen<ProgrammableMenu> {
    private static final int LINES_PER_PAGE = 8;
    private static final int MAX_LINES = ScriptRuntime.MAX_SOURCE_BYTES + 1;
    private static final int PAGE_COUNT = (MAX_LINES + LINES_PER_PAGE - 1) / LINES_PER_PAGE;
    static final int ROBOT_ENERGY_TOP = 48;
    static final int ROBOT_ENERGY_HEIGHT = 12;
    static final int STATE_TOP = 20;
    static final int STATE_LINE_HEIGHT = 9;
    static final int EDITOR_PANEL_BOTTOM = 114;
    private static final int STATE_LEFT = 246;
    private static final int STATE_WIDTH = 96;

    private final List<String> lines = new ArrayList<>(MAX_LINES);
    private final List<EditBox> editors = new ArrayList<>(LINES_PER_PAGE);
    private ScriptLanguage language;
    private Button languageButton;
    private int page;
    private Component validationMessage = Component.empty();
    private final int baseImageWidth;
    private final LegacyMachineGuiLayout.Rect electricalPanel;

    public ProgrammableScreen(ProgrammableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        LegacyMachineGuiLayout.Size baseSize = LegacyMachineGuiLayout.programmableSize(menu.miningRobot());
        baseImageWidth = baseSize.width();
        electricalPanel = menu.miningRobot()
                ? LegacyMachineGuiLayout.electricalPanel(baseImageWidth)
                : null;
        LegacyMachineGuiLayout.Size size = electricalPanel == null
                ? baseSize
                : LegacyMachineGuiLayout.withElectricalPanel(baseSize);
        imageWidth = size.width();
        imageHeight = size.height();
        inventoryLabelX = LegacyMachineGuiLayout.PROGRAMMABLE_PLAYER_LEFT;
        inventoryLabelY = LegacyMachineGuiLayout.programmablePlayerTop(menu.miningRobot()) - 12;
        language = menu.initialLanguage();
        String normalizedSource = menu.initialSource().replace("\r\n", "\n").replace('\r', '\n');
        lines.addAll(List.of(normalizedSource.split("\n", -1)));
        while (lines.size() < MAX_LINES) {
            lines.add("");
        }
    }

    @Override
    protected void init() {
        super.init();
        editors.clear();
        for (int row = 0; row < LINES_PER_PAGE; row++) {
            EditBox editor = new EditBox(
                    font,
                    leftPos + LegacyMachineGuiLayout.PROGRAMMABLE_EDITOR_LEFT,
                    topPos + LegacyMachineGuiLayout.PROGRAMMABLE_EDITOR_TOP + row * 12,
                    LegacyMachineGuiLayout.PROGRAMMABLE_EDITOR_WIDTH,
                    LegacyMachineGuiLayout.PROGRAMMABLE_EDITOR_HEIGHT,
                    Component.translatable("gui.magneticraft.programmable.instruction", row + 1)
            );
            editor.setMaxLength(ScriptRuntime.MAX_SOURCE_BYTES);
            editors.add(addRenderableWidget(editor));
        }
        List<LegacyMachineGuiLayout.Rect> buttons = LegacyMachineGuiLayout.programmableButtons();
        LegacyMachineGuiLayout.Rect previous = buttons.get(0);
        addRenderableWidget(Button.builder(Component.literal("<"), ignored -> setPage(page - 1))
                .bounds(leftPos + previous.x(), topPos + previous.y(), previous.width(), previous.height())
                .build());
        LegacyMachineGuiLayout.Rect next = buttons.get(1);
        addRenderableWidget(Button.builder(Component.literal(">"), ignored -> setPage(page + 1))
                .bounds(leftPos + next.x(), topPos + next.y(), next.width(), next.height())
                .build());
        LegacyMachineGuiLayout.Rect languageBounds = buttons.get(2);
        languageButton = addRenderableWidget(Button.builder(languageLabel(), ignored -> cycleLanguage())
                .bounds(
                        leftPos + languageBounds.x(),
                        topPos + languageBounds.y(),
                        languageBounds.width(),
                        languageBounds.height()
                )
                .build());
        LegacyMachineGuiLayout.Rect uploadBounds = buttons.get(3);
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.magneticraft.programmable.upload"),
                        ignored -> uploadProgram()
                )
                .bounds(
                        leftPos + uploadBounds.x(),
                        topPos + uploadBounds.y(),
                        uploadBounds.width(),
                        uploadBounds.height()
                )
                .build());
        loadVisibleLines();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20242A);
        graphics.fill(leftPos + 5, topPos + 14, leftPos + 238, topPos + EDITOR_PANEL_BOTTOM, 0xFF101316);
        graphics.fill(leftPos + 242, topPos + 14, leftPos + 346, topPos + EDITOR_PANEL_BOTTOM, 0xFF171B20);
        int playerTop = LegacyMachineGuiLayout.programmablePlayerTop(menu.miningRobot());
        graphics.fill(leftPos + 91, topPos + playerTop - 4, leftPos + 258, topPos + playerTop + 78, 0xFF171B20);
        if (menu.miningRobot()) {
            graphics.fill(leftPos + 264, topPos + playerTop - 4, leftPos + 346, topPos + playerTop + 78, 0xFF171B20);
            graphics.fill(
                    leftPos + STATE_LEFT,
                    topPos + ROBOT_ENERGY_TOP,
                    leftPos + STATE_LEFT + STATE_WIDTH,
                    topPos + ROBOT_ENERGY_TOP + ROBOT_ENERGY_HEIGHT,
                    0xFF303740
            );
            int capacity = menu.energyCapacity();
            int width = capacity <= 0 ? 0 : (int) Math.min(94L, (long) menu.energyStored() * 94L / capacity);
            graphics.fill(
                    leftPos + STATE_LEFT + 1,
                    topPos + ROBOT_ENERGY_TOP + 1,
                    leftPos + STATE_LEFT + 1 + width,
                    topPos + ROBOT_ENERGY_TOP + ROBOT_ENERGY_HEIGHT - 1,
                    0xFF4DA3FF
            );
        }
        if (electricalPanel != null) {
            ElectricalStatePanel.render(
                    graphics, font, leftPos, topPos, electricalPanel,
                    menu.position(), menu.energyStored(), menu.energyCapacity()
            );
        }
        for (Slot slot : menu.slots) {
            MachineScreenLayout.drawSlot(graphics, leftPos, topPos, slot.x, slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                font,
                MachineScreenLayout.fitToWidth(font, title, 230),
                titleLabelX,
                titleLabelY,
                0xFFE6EDF3,
                false
        );
        for (int row = 0; row < LINES_PER_PAGE; row++) {
            graphics.drawString(
                    font,
                    Integer.toString(page * LINES_PER_PAGE + row),
                    8,
                    20 + row * 12,
                    0xFF8B949E,
                    false
            );
        }
        graphics.drawString(
                font,
                MachineScreenLayout.fitToWidth(font, stateLabel(), STATE_WIDTH),
                STATE_LEFT,
                STATE_TOP,
                menu.fault() == VmFault.NONE ? 0xFF7EE787 : 0xFFFF7B72,
                false
        );
        graphics.drawString(
                font,
                MachineScreenLayout.fitToWidth(
                        font,
                        Component.translatable(
                                "gui.magneticraft.programmable.counters",
                                menu.programCounter(),
                                menu.redstoneOutput()
                        ),
                        STATE_WIDTH
                ),
                STATE_LEFT,
                STATE_TOP + STATE_LINE_HEIGHT,
                0xFFB8C0C8,
                false
        );
        if (validationMessage.getString().isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.magneticraft.programmable.page", page + 1, PAGE_COUNT),
                    8,
                    LegacyMachineGuiLayout.PROGRAMMABLE_PAGE_TOP,
                    0xFFB8C0C8,
                    false
            );
        } else {
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, validationMessage, 230),
                    8,
                    LegacyMachineGuiLayout.PROGRAMMABLE_PAGE_TOP,
                    0xFFFF7B72,
                    false
            );
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFB8C0C8, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (menu.miningRobot() && mouseX >= leftPos + STATE_LEFT && mouseX < leftPos + STATE_LEFT + STATE_WIDTH
                && mouseY >= topPos + ROBOT_ENERGY_TOP
                && mouseY < topPos + ROBOT_ENERGY_TOP + ROBOT_ENERGY_HEIGHT) {
            graphics.renderTooltip(
                    font,
                    Component.translatable(
                            "gui.magneticraft.energy.tooltip",
                            menu.energyStored(),
                            menu.energyCapacity()
                    ),
                    mouseX,
                    mouseY
            );
        }
        if (electricalPanel != null) {
            ElectricalStatePanel.renderTooltip(
                    graphics, font, mouseX, mouseY, leftPos, topPos, electricalPanel,
                    menu.position(), menu.energyStored(), menu.energyCapacity()
            );
        }
        renderTooltip(graphics, mouseX, mouseY);
    }

    private void setPage(int requestedPage) {
        saveVisibleLines();
        page = Math.max(0, Math.min(PAGE_COUNT - 1, requestedPage));
        validationMessage = Component.empty();
        loadVisibleLines();
    }

    private void saveVisibleLines() {
        for (int row = 0; row < editors.size(); row++) {
            lines.set(page * LINES_PER_PAGE + row, editors.get(row).getValue());
        }
    }

    private void loadVisibleLines() {
        for (int row = 0; row < editors.size(); row++) {
            editors.get(row).setValue(lines.get(page * LINES_PER_PAGE + row));
        }
    }

    private void uploadProgram() {
        saveVisibleLines();
        try {
            ScriptProgram program = new ScriptProgram(language, sourceFromEditor());
            ModNetwork.uploadComputerProgram(new UploadComputerProgramMessage(
                    menu.position(),
                    menu.revision(),
                    menu.sessionToken(),
                    menu.nextUploadSequence(),
                    program
            ));
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        } catch (IllegalArgumentException exception) {
            validationMessage = Component.translatable(
                    "gui.magneticraft.programmable.invalid_program",
                    exception.getMessage()
            );
        }
    }

    private String sourceFromEditor() {
        int lastLine = lines.size() - 1;
        while (lastLine >= 0 && lines.get(lastLine).isEmpty()) {
            lastLine--;
        }
        return lastLine < 0 ? "" : String.join("\n", lines.subList(0, lastLine + 1));
    }

    private void cycleLanguage() {
        ScriptLanguage[] languages = ScriptLanguage.values();
        language = languages[(language.ordinal() + 1) % languages.length];
        languageButton.setMessage(languageLabel());
        validationMessage = Component.empty();
    }

    private Component languageLabel() {
        return Component.literal(language.serializedName().toUpperCase(Locale.ROOT));
    }

    private Component stateLabel() {
        return menu.fault() == VmFault.NONE
                ? Component.translatable(menu.running()
                ? "gui.magneticraft.state.running"
                : "gui.magneticraft.state.stopped")
                : Component.translatable(
                        "gui.magneticraft.programmable.fault." + menu.fault().name().toLowerCase(Locale.ROOT)
                );
    }
}
