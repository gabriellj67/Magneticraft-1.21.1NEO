package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.content.computer.ProgrammableMenu;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import committee.nova.mods.magneticraft.network.ModNetwork;
import committee.nova.mods.magneticraft.network.UploadComputerProgramMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

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
    static final int ROBOT_ENERGY_TOP = 96;
    static final int ROBOT_ENERGY_HEIGHT = 12;
    static final int STATE_TOP = 110;
    static final int STATE_LINE_HEIGHT = 9;
    static final int INVENTORY_PANEL_TOP = 128;
    private static final int STATE_LEFT = 166;
    private static final int STATE_WIDTH = 78;

    private final List<String> lines = new ArrayList<>(MAX_LINES);
    private final List<EditBox> editors = new ArrayList<>(LINES_PER_PAGE);
    private ScriptLanguage language;
    private Button languageButton;
    private int page;
    private Component validationMessage = Component.empty();

    public ProgrammableScreen(ProgrammableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 214;
        inventoryLabelX = 43;
        inventoryLabelY = 121;
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
                    leftPos + 26,
                    topPos + 18 + row * 12,
                    132,
                    11,
                    Component.translatable("gui.magneticraft.programmable.instruction", row + 1)
            );
            editor.setMaxLength(ScriptRuntime.MAX_SOURCE_BYTES);
            editors.add(addRenderableWidget(editor));
        }
        addRenderableWidget(Button.builder(Component.literal("<"), ignored -> setPage(page - 1))
                .bounds(leftPos + 8, topPos + 110, 20, 18)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), ignored -> setPage(page + 1))
                .bounds(leftPos + 32, topPos + 110, 20, 18)
                .build());
        languageButton = addRenderableWidget(Button.builder(languageLabel(), ignored -> cycleLanguage())
                .bounds(leftPos + 55, topPos + 110, 50, 18)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.magneticraft.programmable.upload"),
                        ignored -> uploadProgram()
                )
                .bounds(leftPos + 108, topPos + 110, 50, 18)
                .build());
        loadVisibleLines();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20242A);
        graphics.fill(leftPos + 5, topPos + 14, leftPos + 162, topPos + 108, 0xFF101316);
        if (menu.miningRobot()) {
            graphics.fill(leftPos + 166, topPos + 14, leftPos + 244, topPos + 94, 0xFF171B20);
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 4; column++) {
                    int x = leftPos + 169 + column * 18;
                    int y = topPos + 17 + row * 18;
                    graphics.fill(x, y, x + 18, y + 18, 0xFF353B43);
                }
            }
            graphics.fill(
                    leftPos + STATE_LEFT,
                    topPos + ROBOT_ENERGY_TOP,
                    leftPos + STATE_LEFT + STATE_WIDTH,
                    topPos + ROBOT_ENERGY_TOP + ROBOT_ENERGY_HEIGHT,
                    0xFF303740
            );
            int capacity = menu.energyCapacity();
            int width = capacity <= 0 ? 0 : (int) Math.min(76L, (long) menu.energyStored() * 76L / capacity);
            graphics.fill(
                    leftPos + STATE_LEFT + 1,
                    topPos + ROBOT_ENERGY_TOP + 1,
                    leftPos + STATE_LEFT + 1 + width,
                    topPos + ROBOT_ENERGY_TOP + ROBOT_ENERGY_HEIGHT - 1,
                    0xFF4DA3FF
            );
        }
        graphics.fill(leftPos + 39, topPos + INVENTORY_PANEL_TOP, leftPos + 209, topPos + 210, 0xFF171B20);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFE6EDF3, false);
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
                    101,
                    0xFFB8C0C8,
                    false
            );
        } else {
            graphics.drawString(font, validationMessage, 8, 101, 0xFFFF7B72, false);
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
