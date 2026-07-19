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
    static final int ROBOT_ELECTRICAL_PANEL_WIDTH = 69;
    private static final int STATE_LEFT = 246;
    private static final int STATE_WIDTH = 96;

    private final List<String> lines = new ArrayList<>(MAX_LINES);
    private final List<EditBox> editors = new ArrayList<>(LINES_PER_PAGE);
    private ScriptLanguage language;
    private MachineIconButton previousButton;
    private MachineIconButton nextButton;
    private MachineIconButton languageButton;
    private int page;
    private Component validationMessage = Component.empty();
    private final int baseImageWidth;
    private final LegacyMachineGuiLayout.Rect electricalPanel;

    public ProgrammableScreen(ProgrammableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        LegacyMachineGuiLayout.Size baseSize = LegacyMachineGuiLayout.programmableSize(menu.miningRobot());
        baseImageWidth = baseSize.width();
        electricalPanel = menu.miningRobot()
                ? LegacyMachineGuiLayout.electricalPanel(baseImageWidth, ROBOT_ELECTRICAL_PANEL_WIDTH)
                : null;
        LegacyMachineGuiLayout.Size size = electricalPanel == null
                ? baseSize
                : LegacyMachineGuiLayout.withElectricalPanel(baseSize, ROBOT_ELECTRICAL_PANEL_WIDTH);
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
        MachineScreenLayout.validateInDevelopment(layout());
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
        previousButton = addRenderableWidget(new MachineIconButton(
                leftPos + previous.x(), topPos + previous.y(), previous.width(), previous.height(),
                MachineIcon.PREVIOUS,
                Component.translatable("gui.magneticraft.programmable.previous_page"),
                pageExplanation("gui.magneticraft.programmable.previous_page"),
                ignored -> setPage(page - 1)
        ));
        LegacyMachineGuiLayout.Rect next = buttons.get(1);
        nextButton = addRenderableWidget(new MachineIconButton(
                leftPos + next.x(), topPos + next.y(), next.width(), next.height(),
                MachineIcon.NEXT,
                Component.translatable("gui.magneticraft.programmable.next_page"),
                pageExplanation("gui.magneticraft.programmable.next_page"),
                ignored -> setPage(page + 1)
        ));
        LegacyMachineGuiLayout.Rect languageBounds = buttons.get(2);
        languageButton = addRenderableWidget(new MachineIconButton(
                leftPos + languageBounds.x(), topPos + languageBounds.y(),
                languageBounds.width(), languageBounds.height(), MachineIcon.LANGUAGE,
                Component.translatable("gui.magneticraft.programmable.language"),
                languageExplanation(),
                ignored -> cycleLanguage()
        ));
        LegacyMachineGuiLayout.Rect uploadBounds = buttons.get(3);
        addRenderableWidget(new MachineIconButton(
                leftPos + uploadBounds.x(), topPos + uploadBounds.y(),
                uploadBounds.width(), uploadBounds.height(), MachineIcon.UPLOAD,
                Component.translatable("gui.magneticraft.programmable.upload"),
                Component.translatable("gui.magneticraft.programmable.upload.tooltip"),
                ignored -> uploadProgram()
        ));
        loadVisibleLines();
        refreshNavigationButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        MachineScreenLayout.drawPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        MachineScreenLayout.drawHeader(graphics, leftPos, topPos, baseImageWidth);
        MachineScreenLayout.drawCard(graphics, leftPos + 5, topPos + 14, 233, EDITOR_PANEL_BOTTOM - 14);
        MachineScreenLayout.drawCard(graphics, leftPos + 242, topPos + 14, 104, EDITOR_PANEL_BOTTOM - 14);
        int playerTop = LegacyMachineGuiLayout.programmablePlayerTop(menu.miningRobot());
        MachineScreenLayout.drawCard(graphics, leftPos + 91, topPos + playerTop - 4, 167, 82);
        if (menu.miningRobot()) {
            MachineScreenLayout.drawCard(graphics, leftPos + 264, topPos + playerTop - 4, 82, 82);
            int capacity = menu.energyCapacity();
            int width = capacity <= 0 ? 0 : (int) Math.min(94L, (long) menu.energyStored() * 94L / capacity);
            MachineScreenLayout.drawStatusBar(
                    graphics,
                    leftPos + STATE_LEFT,
                    topPos + ROBOT_ENERGY_TOP,
                    STATE_WIDTH,
                    ROBOT_ENERGY_HEIGHT,
                    width,
                    MachineScreenLayout.ENERGY,
                    false
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
                MachineScreenLayout.TEXT_PRIMARY,
                false
        );
        for (int row = 0; row < LINES_PER_PAGE; row++) {
            graphics.drawString(
                    font,
                    Integer.toString(page * LINES_PER_PAGE + row),
                    8,
                    20 + row * 12,
                    MachineScreenLayout.TEXT_MUTED,
                    false
            );
        }
        graphics.drawString(
                font,
                MachineScreenLayout.fitToWidth(font, stateLabel(), STATE_WIDTH),
                STATE_LEFT,
                STATE_TOP,
                menu.fault() == VmFault.NONE ? MachineScreenLayout.SUCCESS : MachineScreenLayout.DANGER,
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
                MachineScreenLayout.TEXT_MUTED,
                false
        );
        if (validationMessage.getString().isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.magneticraft.programmable.page", page + 1, PAGE_COUNT),
                    8,
                    LegacyMachineGuiLayout.PROGRAMMABLE_PAGE_TOP,
                    MachineScreenLayout.TEXT_MUTED,
                    false
            );
        } else {
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, validationMessage, 230),
                    8,
                    LegacyMachineGuiLayout.PROGRAMMABLE_PAGE_TOP,
                    MachineScreenLayout.DANGER,
                    false
            );
        }
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY,
                MachineScreenLayout.TEXT_MUTED, false);
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
        refreshNavigationButtons();
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
        languageButton.updateExplanation(
                Component.translatable("gui.magneticraft.programmable.language"),
                languageExplanation()
        );
        validationMessage = Component.empty();
    }

    private Component languageLabel() {
        return Component.literal(language.serializedName().toUpperCase(Locale.ROOT));
    }

    private Component languageExplanation() {
        return Component.translatable("gui.magneticraft.programmable.language")
                .append("\n")
                .append(Component.translatable("gui.magneticraft.control.current_state", languageLabel()));
    }

    private Component pageExplanation(String actionKey) {
        return Component.translatable(actionKey)
                .append("\n")
                .append(Component.translatable(
                        "gui.magneticraft.control.current_state",
                        Component.translatable("gui.magneticraft.programmable.page", page + 1, PAGE_COUNT)
                ));
    }

    private void refreshNavigationButtons() {
        if (previousButton != null) {
            previousButton.active = page > 0;
            previousButton.updateExplanation(
                    Component.translatable("gui.magneticraft.programmable.previous_page"),
                    pageExplanation("gui.magneticraft.programmable.previous_page")
            );
        }
        if (nextButton != null) {
            nextButton.active = page + 1 < PAGE_COUNT;
            nextButton.updateExplanation(
                    Component.translatable("gui.magneticraft.programmable.next_page"),
                    pageExplanation("gui.magneticraft.programmable.next_page")
            );
        }
    }

    MachineScreenBounds.Layout layout() {
        return layout(menu.miningRobot(), imageWidth, imageHeight);
    }

    static MachineScreenBounds.Layout layout(boolean miningRobot, int imageWidth, int imageHeight) {
        int baseImageWidth = LegacyMachineGuiLayout.PROGRAMMABLE_WIDTH;
        int playerTop = LegacyMachineGuiLayout.programmablePlayerTop(miningRobot);
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder(
                miningRobot ? "programmable/mining_robot" : "programmable/computer",
                imageWidth,
                imageHeight
        ).element("header", new LegacyMachineGuiLayout.Rect(1, 1, baseImageWidth - 2, 15), "sections", 1)
                .element("editor_panel", new LegacyMachineGuiLayout.Rect(5, 14, 233, EDITOR_PANEL_BOTTOM - 14))
                .element("state_panel", new LegacyMachineGuiLayout.Rect(242, 14, 104, EDITOR_PANEL_BOTTOM - 14))
                .element("player_inventory", new LegacyMachineGuiLayout.Rect(91, playerTop - 4, 167, 82),
                        "sections", 2);
        for (int row = 0; row < LINES_PER_PAGE; row++) {
            builder.child("editor_" + row, new LegacyMachineGuiLayout.Rect(
                    LegacyMachineGuiLayout.PROGRAMMABLE_EDITOR_LEFT,
                    LegacyMachineGuiLayout.PROGRAMMABLE_EDITOR_TOP + row * 12,
                    LegacyMachineGuiLayout.PROGRAMMABLE_EDITOR_WIDTH,
                    LegacyMachineGuiLayout.PROGRAMMABLE_EDITOR_HEIGHT
            ), "editor_panel", "editors", 1);
        }
        List<LegacyMachineGuiLayout.Rect> buttons = LegacyMachineGuiLayout.programmableButtons();
        for (int index = 0; index < buttons.size(); index++) {
            builder.element("button_" + index, buttons.get(index), "buttons", 4);
        }
        List<LegacyMachineGuiLayout.Rect> playerSlots = LegacyMachineGuiLayout.playerInventorySlots(
                LegacyMachineGuiLayout.PROGRAMMABLE_PLAYER_LEFT, playerTop
        );
        for (int index = 0; index < playerSlots.size(); index++) {
            builder.child("player_slot_" + index, playerSlots.get(index),
                    "player_inventory", "player_slots", 0);
        }
        if (miningRobot) {
            builder.element("robot_inventory", new LegacyMachineGuiLayout.Rect(264, playerTop - 4, 82, 82),
                    "sections", 2)
                    .child("robot_energy", new LegacyMachineGuiLayout.Rect(
                            STATE_LEFT, ROBOT_ENERGY_TOP, STATE_WIDTH, ROBOT_ENERGY_HEIGHT
                    ), "state_panel", null, 0)
                    .element("electrical", LegacyMachineGuiLayout.electricalPanel(
                            baseImageWidth, ROBOT_ELECTRICAL_PANEL_WIDTH
                    ), "sections", 2);
            List<LegacyMachineGuiLayout.Point> robotSlots = LegacyMachineGuiLayout.miningRobotSlots();
            for (int index = 0; index < robotSlots.size(); index++) {
                builder.child("robot_slot_" + index, LegacyMachineGuiLayout.slotBounds(robotSlots.get(index)),
                        "robot_inventory", "robot_slots", 0);
            }
        }
        return builder.build();
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
