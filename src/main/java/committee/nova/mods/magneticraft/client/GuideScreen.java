package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.client.guide.GuideRepository;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout.Rect;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Locale;

/**
 * Resource-pack-driven guide with searchable structures, portable equipment and VM opcodes.
 */
public final class GuideScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 150;
    private static final int ENTRY_LIST_LEFT = 10;
    private static final int ENTRY_LIST_TOP = 51;
    private static final int ENTRY_LIST_RIGHT = SIDEBAR_WIDTH - 6;
    private static final int ENTRY_LIST_BOTTOM_MARGIN = 12;
    private static final int ENTRY_ROW_HEIGHT = 13;
    private static final int ENTRY_SCROLLBAR_WIDTH = 4;
    private static final int STRUCTURE_VIEW_TOP = 92;
    private static final int STRUCTURE_VIEW_BOTTOM_MARGIN = 34;
    private static final int STRUCTURE_STEP_MILLIS = 850;

    private EditBox search;
    private MachineIconButton modeButton;
    private MachineIconButton previousLayer;
    private MachineIconButton playLayers;
    private MachineIconButton resetStructure;
    private MachineIconButton nextLayer;
    private final GuideStructurePreview structurePreview = new GuideStructurePreview();
    private Mode mode = Mode.STRUCTURES;
    private int selectedIndex;
    private int entryScrollOffset;
    private int layer;
    private boolean playingLayers = true;
    private long lastLayerAdvanceMillis = Util.getMillis();
    private Component hoveredLegend = Component.empty();

    public GuideScreen() {
        super(Component.translatable("gui.magneticraft.guide.title"));
    }

    @Override
    protected void init() {
        super.init();
        MachineScreenLayout.validateInDevelopment(layout());
        search = addRenderableWidget(new EditBox(
                font,
                12,
                28,
                SIDEBAR_WIDTH - 24,
                18,
                Component.translatable("gui.magneticraft.guide.search")
        ));
        search.setHint(Component.translatable("gui.magneticraft.guide.search"));
        search.setMaxLength(64);
        search.setResponder(ignored -> {
            selectedIndex = 0;
            entryScrollOffset = 0;
            resetStructure(true);
        });
        modeButton = addRenderableWidget(guideButton(
                width - 36, 8, MachineIcon.MODE,
                "gui.magneticraft.guide.cycle_mode", ignored -> toggleMode()
        ));
        previousLayer = addRenderableWidget(guideButton(
                SIDEBAR_WIDTH + 12, height - 28, MachineIcon.DOWN,
                "gui.magneticraft.guide.previous_layer", ignored -> selectLayer(-1)
        ));
        playLayers = addRenderableWidget(guideButton(
                SIDEBAR_WIDTH + 40, height - 28, MachineIcon.PAUSE,
                "gui.magneticraft.guide.pause_layers", ignored -> toggleLayerPlayback()
        ));
        resetStructure = addRenderableWidget(guideButton(
                SIDEBAR_WIDTH + 68, height - 28, MachineIcon.RESET,
                "gui.magneticraft.guide.reset_structure_view", ignored -> resetStructure(false)
        ));
        nextLayer = addRenderableWidget(guideButton(
                SIDEBAR_WIDTH + 96, height - 28, MachineIcon.UP,
                "gui.magneticraft.guide.next_layer", ignored -> selectLayer(1)
        ));
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        MachineScreenLayout.drawPanel(graphics, 4, 4, width - 8, height - 8);
        MachineScreenLayout.drawSectionHeader(graphics, 4, 4, width - 8, 26);
        MachineScreenLayout.drawCard(graphics, 8, 24, SIDEBAR_WIDTH - 12, height - 32);
        MachineScreenLayout.drawCard(
                graphics,
                SIDEBAR_WIDTH + 4,
                34,
                width - SIDEBAR_WIDTH - 8,
                height - 38
        );
        graphics.drawString(font, title, 12, 10, MachineScreenLayout.TEXT_PRIMARY, false);
        hoveredLegend = Component.empty();
        switch (mode) {
            case STRUCTURES -> renderStructures(graphics, mouseX, mouseY);
            case MACHINES -> renderMachines(graphics, mouseX, mouseY);
            case ITEMS -> renderItems(graphics, mouseX, mouseY);
            case LANGUAGES -> renderLanguages(graphics, mouseX, mouseY);
            case OPCODES -> renderOpcodes(graphics, mouseX, mouseY);
        }
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (!hoveredLegend.getString().isEmpty()) {
            graphics.renderTooltip(font, hoveredLegend, mouseX, mouseY);
        }
    }

    private void renderStructures(GuiGraphics graphics, int mouseX, int mouseY) {
        List<GuideRepository.MultiblockGuide> guides = filteredStructures();
        if (guides.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.magneticraft.guide.no_results"),
                    12,
                    54,
                    0xFF8B949E,
                    false
            );
            return;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, guides.size() - 1));
        GuideRepository.MultiblockGuide selected = guides.get(selectedIndex);
        advanceLayerPlayback(selected.layers().size());
        renderEntryList(graphics, guides.stream()
                .map(guide -> Component.translatable(guide.translationKey()))
                .toList(), mouseX, mouseY);

        int contentLeft = SIDEBAR_WIDTH + 12;
        int contentRight = width - 16;
        int contentWidth = Math.max(80, contentRight - contentLeft);
        graphics.drawString(
                font,
                MachineScreenLayout.fitToWidth(
                        font,
                        Component.translatable(selected.translationKey()),
                        contentWidth
                ),
                contentLeft,
                42,
                MachineScreenLayout.TEXT_PRIMARY,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.magneticraft.guide.category." + selected.category()),
                contentLeft,
                54,
                MachineScreenLayout.TEXT_MUTED,
                false
        );
        Component description = Component.translatable(selected.descriptionKey());
        renderLimitedDescription(graphics, description, contentLeft, 66, contentWidth);

        layer = Math.max(0, Math.min(layer, selected.layers().size() - 1));
        int viewportBottom = height - STRUCTURE_VIEW_BOTTOM_MARGIN;
        int viewportWidth = Math.max(1, contentRight - contentLeft);
        int infoWidth = structureInfoWidth(viewportWidth);
        int sceneRight = infoWidth == 0 ? contentRight : contentRight - infoWidth - 4;
        structurePreview.render(
                graphics,
                selected,
                layer,
                contentLeft,
                STRUCTURE_VIEW_TOP,
                sceneRight,
                viewportBottom
        );
        renderStructureOverlay(graphics, selected, contentLeft, sceneRight);
        if (infoWidth > 0) {
            renderStructureInformation(
                    graphics,
                    selected,
                    sceneRight + 4,
                    STRUCTURE_VIEW_TOP,
                    contentRight,
                    viewportBottom,
                    mouseX,
                    mouseY
            );
        }
        renderLayerTimeline(graphics, selected.layers().size(), mouseX, mouseY);
    }

    private void renderLimitedDescription(
            GuiGraphics graphics,
            Component description,
            int x,
            int y,
            int width
    ) {
        List<FormattedCharSequence> lines = font.split(description, width);
        for (int index = 0; index < Math.min(2, lines.size()); index++) {
            graphics.drawString(
                    font,
                    lines.get(index),
                    x,
                    y + index * 9,
                    MachineScreenLayout.TEXT_MUTED,
                    false
            );
        }
    }

    private void renderStructureOverlay(
            GuiGraphics graphics,
            GuideRepository.MultiblockGuide guide,
            int left,
            int right
    ) {
        GuideStructurePreview.Dimensions dimensions = GuideStructurePreview.dimensions(guide);
        Component layerText = Component.translatable(
                "gui.magneticraft.guide.layer",
                layer + 1,
                guide.layers().size()
        ).append("  ·  ").append(Component.translatable(
                "gui.magneticraft.guide.structure_dimensions",
                dimensions.width(),
                dimensions.height(),
                dimensions.depth()
        ));
        Component fittedLayer = MachineScreenLayout.fitToWidth(font, layerText, Math.max(1, right - left - 12));
        int labelRight = left + 5 + font.width(fittedLayer) + 4;
        graphics.fill(left + 3, STRUCTURE_VIEW_TOP + 3, labelRight, STRUCTURE_VIEW_TOP + 15, 0xD0080E14);
        graphics.drawString(
                font,
                fittedLayer,
                left + 5,
                STRUCTURE_VIEW_TOP + 5,
                MachineScreenLayout.ACCENT,
                false
        );

        Component interaction = MachineScreenLayout.fitToWidth(
                font,
                Component.translatable("gui.magneticraft.guide.structure_interaction"),
                Math.max(1, right - left - 12)
        );
        int interactionY = height - STRUCTURE_VIEW_BOTTOM_MARGIN - 14;
        int interactionRight = left + 5 + font.width(interaction) + 4;
        graphics.fill(left + 3, interactionY - 2, interactionRight, interactionY + 10, 0xD0080E14);
        graphics.drawString(
                font,
                interaction,
                left + 5,
                interactionY,
                MachineScreenLayout.TEXT_MUTED,
                false
        );
    }

    private void renderStructureInformation(
            GuiGraphics graphics,
            GuideRepository.MultiblockGuide guide,
            int left,
            int top,
            int right,
            int bottom,
            int mouseX,
            int mouseY
    ) {
        MachineScreenLayout.drawCard(graphics, left, top, right - left, bottom - top);
        int textX = left + 5;
        int textWidth = Math.max(1, right - textX - 5);
        int cursorY = top + 5;
        graphics.drawString(
                font,
                Component.translatable("gui.magneticraft.guide.structure_data"),
                textX,
                cursorY,
                MachineScreenLayout.TEXT_PRIMARY,
                false
        );
        cursorY += 13;
        GuideRepository.PortSummary ports = guide.ports();
        Component yes = Component.translatable("gui.magneticraft.guide.yes");
        Component no = Component.translatable("gui.magneticraft.guide.no");
        String tankCapacities = ports.fluidTankCapacitiesMb().isEmpty()
                ? Component.translatable("gui.magneticraft.guide.none").getString()
                : ports.fluidTankCapacitiesMb().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
        List<Component> lines = List.of(
                Component.translatable(
                        "gui.magneticraft.guide.machine.recipe",
                        guide.recipeType().isEmpty()
                                ? Component.translatable("gui.magneticraft.guide.none")
                                : Component.literal(guide.recipeType())
                ),
                Component.translatable(
                        "gui.magneticraft.guide.mirroring",
                        guide.supportsMirroring() ? yes : no
                ),
                Component.translatable(
                        "gui.magneticraft.guide.ports.items",
                        ports.inventorySlots(),
                        ports.bulkItemCapacity()
                ),
                Component.translatable(
                        "gui.magneticraft.guide.ports.energy",
                        ports.electricity() ? yes : no,
                        ports.heat() ? yes : no
                ),
                Component.translatable("gui.magneticraft.guide.ports.tanks", tankCapacities)
        );
        for (Component line : lines) {
            if (cursorY + 9 >= bottom) {
                return;
            }
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, line, textWidth),
                    textX,
                    cursorY,
                    MachineScreenLayout.TEXT_MUTED,
                    false
            );
            cursorY += 10;
        }

        cursorY += 3;
        if (cursorY + 10 >= bottom) {
            return;
        }
        graphics.drawString(
                font,
                Component.translatable("gui.magneticraft.guide.structure_parts"),
                textX,
                cursorY,
                MachineScreenLayout.TEXT_PRIMARY,
                false
        );
        cursorY += 12;
        List<GuideRepository.LegendEntry> entries = guide.legend().values().stream()
                .filter(entry -> !entry.ignored())
                .sorted(java.util.Comparator.comparingInt(GuideRepository.LegendEntry::symbol))
                .toList();
        int rendered = 0;
        for (GuideRepository.LegendEntry entry : entries) {
            if (cursorY + 17 > bottom - 3) {
                break;
            }
            renderLegendIcon(graphics, guide, entry, textX, cursorY);
            Component label = MachineScreenLayout.fitToWidth(
                    font,
                    Component.translatable("gui.magneticraft.guide.rule." + entry.rule()),
                    Math.max(1, textWidth - 20)
            );
            graphics.drawString(
                    font,
                    label,
                    textX + 20,
                    cursorY + 4,
                    MachineScreenLayout.TEXT_MUTED,
                    false
            );
            if (mouseX >= textX && mouseX < right - 4 && mouseY >= cursorY && mouseY < cursorY + 17) {
                hoveredLegend = legendTooltip(guide, entry);
            }
            rendered++;
            cursorY += 17;
        }
        if (rendered < entries.size() && cursorY + 9 < bottom) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.magneticraft.guide.more_parts", entries.size() - rendered),
                    textX,
                    cursorY,
                    MachineScreenLayout.TEXT_DISABLED,
                    false
            );
        }
    }

    private void renderOpcodes(GuiGraphics graphics, int mouseX, int mouseY) {
        List<GuideRepository.OpcodeGuide> opcodes = filteredOpcodes();
        if (opcodes.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.magneticraft.guide.no_results"),
                    12,
                    54,
                    0xFF8B949E,
                    false
            );
            return;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, opcodes.size() - 1));
        GuideRepository.OpcodeGuide selected = opcodes.get(selectedIndex);
        renderEntryList(
                graphics,
                opcodes.stream().map(opcode -> Component.literal(opcode.id())).toList(),
                mouseX,
                mouseY
        );

        int contentLeft = SIDEBAR_WIDTH + 12;
        graphics.drawString(font, Component.literal(selected.id()), contentLeft, 44, 0xFFE6EDF3, false);
        graphics.drawString(
                font,
                Component.translatable(
                        "gui.magneticraft.guide.opcode_signature",
                        selected.code(),
                        selected.operandCount()
                ),
                contentLeft,
                60,
                0xFF79C0FF,
                false
        );
        graphics.drawWordWrap(
                font,
                Component.translatable(selected.descriptionKey()),
                contentLeft,
                80,
                Math.max(80, width - contentLeft - 16),
                0xFFB8C0C8
        );
    }

    private void renderLanguages(GuiGraphics graphics, int mouseX, int mouseY) {
        List<GuideRepository.ComputerLanguageGuide> languages = filteredLanguages();
        if (languages.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.magneticraft.guide.no_results"),
                    12,
                    54,
                    0xFF8B949E,
                    false
            );
            return;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, languages.size() - 1));
        GuideRepository.ComputerLanguageGuide selected = languages.get(selectedIndex);
        renderEntryList(graphics, languages.stream()
                .map(language -> Component.translatable(
                        "guide.magneticraft.computer.language." + language.id() + ".name"
                ))
                .toList(), mouseX, mouseY);

        int contentLeft = SIDEBAR_WIDTH + 12;
        int contentWidth = Math.max(80, width - contentLeft - 16);
        graphics.drawString(
                font,
                Component.translatable("guide.magneticraft.computer.language." + selected.id() + ".name"),
                contentLeft,
                44,
                0xFFE6EDF3,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.magneticraft.guide.computer.version", selected.historicalVersion()),
                contentLeft,
                58,
                0xFF79C0FF,
                false
        );
        Component description = Component.translatable(
                "guide.magneticraft.computer.language." + selected.id() + ".description"
        );
        graphics.drawWordWrap(font, description, contentLeft, 72, contentWidth, 0xFFB8C0C8);
        int cursorY = 72 + Math.max(1, font.split(description, contentWidth).size()) * 9 + 5;
        GuideRepository.ComputerLimits limits = selected.limits();
        List<Component> lines = List.of(
                Component.translatable(
                        "gui.magneticraft.guide.computer.source_limits",
                        limits.sourceBytes(), limits.outputCharacters()
                ),
                Component.translatable(
                        "gui.magneticraft.guide.computer.tick_limits",
                        limits.instructionsPerTick(), limits.deviceCallsPerTick()
                ),
                Component.translatable(
                        "gui.magneticraft.guide.computer.storage_limits",
                        limits.floppyBytes(), limits.floppyEntries(), limits.quarryMaxSize()
                ),
                Component.translatable(
                        "gui.magneticraft.guide.computer.examples",
                        String.join(" · ", selected.examples())
                ),
                Component.translatable(
                        "gui.magneticraft.guide.computer.commands",
                        String.join(", ", selected.commands())
                )
        );
        for (Component line : lines) {
            graphics.drawWordWrap(font, line, contentLeft, cursorY, contentWidth, 0xFF79C0FF);
            cursorY += Math.max(1, font.split(line, contentWidth).size()) * 9;
        }
        GuideRepository.ComputerSecurity security = selected.security();
        Component yes = Component.translatable("gui.magneticraft.guide.yes");
        Component no = Component.translatable("gui.magneticraft.guide.no");
        Component securityLine = Component.translatable(
                "gui.magneticraft.guide.computer.security",
                security.serverAuthoritative() ? yes : no,
                security.menuSessionReplayProtection() ? yes : no,
                security.hostFilesystemAccess() ? yes : no,
                security.outboundNetworkAccess() ? yes : no,
                security.forceLoadChunks() ? yes : no
        );
        graphics.drawWordWrap(font, securityLine, contentLeft, cursorY, contentWidth, 0xFFB8C0C8);
    }

    private void renderItems(GuiGraphics graphics, int mouseX, int mouseY) {
        List<GuideRepository.ItemGuide> items = filteredItems();
        if (items.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.magneticraft.guide.no_results"),
                    12,
                    54,
                    0xFF8B949E,
                    false
            );
            return;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, items.size() - 1));
        GuideRepository.ItemGuide selected = items.get(selectedIndex);
        renderEntryList(graphics, items.stream()
                .map(item -> Component.translatable(item.translationKey()))
                .toList(), mouseX, mouseY);

        int contentLeft = SIDEBAR_WIDTH + 12;
        int contentWidth = Math.max(80, width - contentLeft - 16);
        graphics.drawString(
                font,
                Component.translatable(selected.translationKey()),
                contentLeft,
                44,
                0xFFE6EDF3,
                false
        );
        Component description = Component.translatable(selected.descriptionKey());
        graphics.drawWordWrap(font, description, contentLeft, 62, contentWidth, 0xFFB8C0C8);
        int cursorY = 62 + Math.max(1, font.split(description, contentWidth).size()) * 9 + 8;
        cursorY = renderItemStat(graphics, "gui.magneticraft.guide.capacity", selected.capacityFe(), contentLeft, cursorY);
        cursorY = renderItemStat(graphics, "gui.magneticraft.guide.break_cost", selected.breakCostFe(), contentLeft, cursorY);
        cursorY = renderItemStat(graphics, "gui.magneticraft.guide.attack_cost", selected.attackCostFe(), contentLeft, cursorY);
        renderItemStat(graphics, "gui.magneticraft.guide.use_cost", selected.useCostFe(), contentLeft, cursorY);
    }

    private void renderMachines(GuiGraphics graphics, int mouseX, int mouseY) {
        List<GuideRepository.MachineGuide> machines = filteredMachines();
        if (machines.isEmpty()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.magneticraft.guide.no_results"),
                    12,
                    54,
                    0xFF8B949E,
                    false
            );
            return;
        }
        selectedIndex = Math.max(0, Math.min(selectedIndex, machines.size() - 1));
        GuideRepository.MachineGuide selected = machines.get(selectedIndex);
        renderEntryList(graphics, machines.stream()
                .map(machine -> Component.translatable(machine.translationKey()))
                .toList(), mouseX, mouseY);

        int contentLeft = SIDEBAR_WIDTH + 12;
        int contentWidth = Math.max(80, width - contentLeft - 16);
        graphics.drawString(
                font,
                Component.translatable(selected.translationKey()),
                contentLeft,
                42,
                0xFFE6EDF3,
                false
        );
        graphics.drawString(
                font,
                Component.translatable("gui.magneticraft.guide.category." + selected.category()),
                contentLeft,
                54,
                0xFF8B949E,
                false
        );
        Component description = Component.translatable(selected.descriptionKey());
        graphics.drawWordWrap(font, description, contentLeft, 68, contentWidth, 0xFFB8C0C8);
        int cursorY = 68 + Math.max(1, font.split(description, contentWidth).size()) * 9 + 6;
        Component yes = Component.translatable("gui.magneticraft.guide.yes");
        Component no = Component.translatable("gui.magneticraft.guide.no");
        List<Component> details = List.of(
                Component.translatable(
                        "gui.magneticraft.guide.machine.inventory",
                        selected.inventorySlots(),
                        selected.ghostSlots()
                ),
                Component.translatable(
                        "gui.magneticraft.guide.machine.menu",
                        selected.hasMenu() ? yes : no
                ),
                Component.translatable(
                        "gui.magneticraft.guide.machine.redstone",
                        Component.translatable("gui.magneticraft.guide.redstone." + selected.redstoneControl())
                ),
                Component.translatable(
                        "gui.magneticraft.guide.machine.processing",
                        Component.translatable("gui.magneticraft.guide.processing." + selected.processingKind())
                ),
                Component.translatable(
                        "gui.magneticraft.guide.machine.recipe",
                        selected.recipeType().isEmpty()
                                ? Component.translatable("gui.magneticraft.guide.none")
                                : Component.literal(selected.recipeType())
                ),
                Component.translatable(
                        "gui.magneticraft.guide.machine.automation",
                        Component.translatable("gui.magneticraft.guide.automation." + selected.automationProfile())
                ),
                Component.translatable(
                        "gui.magneticraft.guide.machine.slots",
                        translatedList("gui.magneticraft.guide.slot.", selected.slotRoles())
                ),
                Component.translatable(
                        "gui.magneticraft.guide.machine.ports",
                        translatedList("gui.magneticraft.guide.port.", selected.physicalPorts())
                )
        );
        for (Component detail : details) {
            graphics.drawWordWrap(font, detail, contentLeft, cursorY, contentWidth, 0xFF79C0FF);
            cursorY += Math.max(1, font.split(detail, contentWidth).size()) * 9;
        }
    }

    private static String translatedList(String prefix, List<String> values) {
        if (values.isEmpty()) {
            return Component.translatable("gui.magneticraft.guide.none").getString();
        }
        return values.stream()
                .map(value -> Component.translatable(prefix + value).getString())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private int renderItemStat(GuiGraphics graphics, String translationKey, int value, int x, int y) {
        if (value <= 0) {
            return y;
        }
        graphics.drawString(font, Component.translatable(translationKey, value), x, y, 0xFF79C0FF, false);
        return y + 12;
    }

    private void renderEntryList(
            GuiGraphics graphics,
            List<? extends Component> entries,
            int mouseX,
            int mouseY
    ) {
        int visible = visibleEntryCount(height);
        entryScrollOffset = clampEntryScroll(entryScrollOffset, entries.size(), height);
        int first = entryScrollOffset;
        int last = Math.min(entries.size(), first + visible);
        int hoveredIndex = entryIndexAt(mouseX, mouseY, first, entries.size(), height);
        boolean hasScrollbar = entries.size() > visible;
        int textWidth = ENTRY_LIST_RIGHT - ENTRY_LIST_LEFT - 7
                - (hasScrollbar ? ENTRY_SCROLLBAR_WIDTH + 3 : 0);
        for (int index = first; index < last; index++) {
            int y = ENTRY_LIST_TOP + (index - first) * ENTRY_ROW_HEIGHT;
            boolean selected = index == selectedIndex;
            boolean hovered = index == hoveredIndex;
            if (selected || hovered) {
                graphics.fill(
                        ENTRY_LIST_LEFT,
                        y,
                        ENTRY_LIST_RIGHT - (hasScrollbar ? ENTRY_SCROLLBAR_WIDTH + 2 : 0),
                        y + ENTRY_ROW_HEIGHT - 1,
                        selected ? MachineScreenLayout.BUTTON_SELECTED : MachineScreenLayout.BUTTON_HOVER
                );
            }
            if (selected) {
                graphics.fill(
                        ENTRY_LIST_LEFT,
                        y,
                        ENTRY_LIST_LEFT + 2,
                        y + ENTRY_ROW_HEIGHT - 1,
                        MachineScreenLayout.ACCENT
                );
            }
            Component entry = entries.get(index);
            graphics.drawString(
                    font,
                    MachineScreenLayout.fitToWidth(font, entry, textWidth),
                    ENTRY_LIST_LEFT + 4,
                    y + 2,
                    selected ? MachineScreenLayout.ACCENT : MachineScreenLayout.TEXT_PRIMARY,
                    false
            );
            if (hovered && font.width(entry) > textWidth) {
                hoveredLegend = entry;
            }
        }
        if (hasScrollbar) {
            renderEntryScrollbar(graphics, entries.size(), visible);
        }
    }

    private void renderEntryScrollbar(GuiGraphics graphics, int entryCount, int visibleCount) {
        int listBottom = entryListBottom(height);
        int trackHeight = listBottom - ENTRY_LIST_TOP;
        int trackLeft = ENTRY_LIST_RIGHT - ENTRY_SCROLLBAR_WIDTH;
        int thumbHeight = Math.max(10, Math.round(trackHeight * visibleCount / (float) entryCount));
        int maxOffset = Math.max(1, entryCount - visibleCount);
        int thumbTop = ENTRY_LIST_TOP + Math.round(
                (trackHeight - thumbHeight) * entryScrollOffset / (float) maxOffset
        );
        graphics.fill(
                trackLeft,
                ENTRY_LIST_TOP,
                ENTRY_LIST_RIGHT,
                listBottom,
                MachineScreenLayout.INSET_BACKGROUND
        );
        graphics.fill(
                trackLeft,
                thumbTop,
                ENTRY_LIST_RIGHT,
                thumbTop + thumbHeight,
                MachineScreenLayout.ACCENT
        );
    }

    private void renderLegendIcon(
            GuiGraphics graphics,
            GuideRepository.MultiblockGuide guide,
            GuideRepository.LegendEntry entry,
            int x,
            int y
    ) {
        BlockState state = structurePreview.stateFor(guide, entry);
        if (state == null || state.isAir()) {
            graphics.fill(x + 1, y + 1, x + 15, y + 2, MachineScreenLayout.FLUID);
            graphics.fill(x + 1, y + 14, x + 15, y + 15, MachineScreenLayout.FLUID);
            graphics.fill(x + 1, y + 2, x + 2, y + 14, MachineScreenLayout.FLUID);
            graphics.fill(x + 14, y + 2, x + 15, y + 14, MachineScreenLayout.FLUID);
            return;
        }
        graphics.renderItem(new ItemStack(state.getBlock()), x, y);
    }

    private Component legendTooltip(
            GuideRepository.MultiblockGuide guide,
            GuideRepository.LegendEntry entry
    ) {
        Component tooltip = Component.translatable("gui.magneticraft.guide.rule." + entry.rule());
        BlockState state = structurePreview.stateFor(guide, entry);
        if (state != null && !state.isAir()) {
            tooltip = tooltip.copy().append("\n").append(new ItemStack(state.getBlock()).getHoverName());
        }
        return tooltip;
    }

    private void renderLayerTimeline(GuiGraphics graphics, int layerCount, int mouseX, int mouseY) {
        int left = timelineLeft();
        int right = timelineRight();
        int top = timelineTop();
        if (right <= left) {
            return;
        }
        graphics.fill(left, top, right, top + 6, MachineScreenLayout.BUTTON_BORDER);
        graphics.fill(left + 1, top + 1, right - 1, top + 5, MachineScreenLayout.INSET_BACKGROUND);
        for (int index = 0; index < layerCount; index++) {
            int x = timelineLayerX(index, layerCount, left, right);
            int color = index <= layer ? MachineScreenLayout.ACCENT : MachineScreenLayout.TEXT_DISABLED;
            graphics.fill(x - 1, top, x + 1, top + 6, color);
        }
        if (insideTimeline(mouseX, mouseY)) {
            hoveredLegend = Component.translatable(
                    "gui.magneticraft.guide.timeline_hint",
                    layer + 1,
                    layerCount
            );
        }
    }

    private void toggleMode() {
        Mode[] modes = Mode.values();
        mode = modes[(mode.ordinal() + 1) % modes.length];
        selectedIndex = 0;
        entryScrollOffset = 0;
        resetStructure(mode == Mode.STRUCTURES);
        updateButtons();
    }

    private void selectEntry(int index) {
        if (index == selectedIndex) {
            return;
        }
        selectedIndex = index;
        resetStructure(mode == Mode.STRUCTURES);
    }

    private void selectLayer(int amount) {
        List<GuideRepository.MultiblockGuide> guides = filteredStructures();
        if (guides.isEmpty()) {
            return;
        }
        GuideRepository.MultiblockGuide selected = guides.get(Math.max(0, Math.min(selectedIndex, guides.size() - 1)));
        layer = Mth.clamp(layer + amount, 0, selected.layers().size() - 1);
        playingLayers = false;
        lastLayerAdvanceMillis = Util.getMillis();
        updateButtons();
    }

    private void toggleLayerPlayback() {
        int layers = layerCount();
        if (layers <= 1) {
            return;
        }
        if (!playingLayers && layer >= layers - 1) {
            layer = 0;
        }
        playingLayers = !playingLayers;
        lastLayerAdvanceMillis = Util.getMillis();
        updateButtons();
    }

    private void resetStructure(boolean autoplay) {
        layer = 0;
        playingLayers = autoplay;
        lastLayerAdvanceMillis = Util.getMillis();
        structurePreview.resetView();
        if (previousLayer != null) {
            updateButtons();
        }
    }

    private void advanceLayerPlayback(int layers) {
        if (!playingLayers || layers <= 1) {
            return;
        }
        long now = Util.getMillis();
        long elapsed = now - lastLayerAdvanceMillis;
        if (elapsed < STRUCTURE_STEP_MILLIS) {
            return;
        }
        int steps = Math.max(1, (int) (elapsed / STRUCTURE_STEP_MILLIS));
        layer = Math.min(layers - 1, layer + steps);
        lastLayerAdvanceMillis += (long) steps * STRUCTURE_STEP_MILLIS;
        if (layer >= layers - 1) {
            playingLayers = false;
        }
        updateButtons();
    }

    private void updateButtons() {
        if (previousLayer == null) {
            return;
        }
        boolean structureMode = mode == Mode.STRUCTURES;
        boolean hasStructures = structureMode && !filteredStructures().isEmpty();
        int layers = layerCount();
        previousLayer.visible = structureMode;
        playLayers.visible = structureMode;
        resetStructure.visible = structureMode;
        nextLayer.visible = structureMode;
        previousLayer.active = hasStructures && layer > 0;
        playLayers.active = hasStructures && layers > 1;
        resetStructure.active = hasStructures;
        nextLayer.active = hasStructures && layer < layers - 1;
        modeButton.updateExplanation(
                Component.translatable("gui.magneticraft.guide.cycle_mode"),
                controlExplanation("gui.magneticraft.guide.cycle_mode", mode.title())
        );
        Component layerState = layers == 0
                ? Component.translatable("gui.magneticraft.guide.none")
                : Component.translatable("gui.magneticraft.guide.layer", layer + 1, layers);
        previousLayer.updateExplanation(
                Component.translatable("gui.magneticraft.guide.previous_layer"),
                controlExplanation("gui.magneticraft.guide.previous_layer", layerState)
        );
        nextLayer.updateExplanation(
                Component.translatable("gui.magneticraft.guide.next_layer"),
                controlExplanation("gui.magneticraft.guide.next_layer", layerState)
        );
        String playbackKey = playingLayers
                ? "gui.magneticraft.guide.pause_layers"
                : "gui.magneticraft.guide.play_layers";
        playLayers.updateIcon(playingLayers ? MachineIcon.PAUSE : MachineIcon.START);
        playLayers.updateExplanation(
                Component.translatable(playbackKey),
                controlExplanation(playbackKey, layerState)
        );
        resetStructure.updateExplanation(
                Component.translatable("gui.magneticraft.guide.reset_structure_view"),
                controlExplanation("gui.magneticraft.guide.reset_structure_view", layerState)
        );
    }

    private int entryCount() {
        return switch (mode) {
            case STRUCTURES -> filteredStructures().size();
            case MACHINES -> filteredMachines().size();
            case ITEMS -> filteredItems().size();
            case LANGUAGES -> filteredLanguages().size();
            case OPCODES -> filteredOpcodes().size();
        };
    }

    private int layerCount() {
        if (mode != Mode.STRUCTURES) {
            return 0;
        }
        List<GuideRepository.MultiblockGuide> guides = filteredStructures();
        if (guides.isEmpty()) {
            return 0;
        }
        int index = Math.max(0, Math.min(selectedIndex, guides.size() - 1));
        return guides.get(index).layers().size();
    }

    private MachineIconButton guideButton(
            int x,
            int y,
            MachineIcon icon,
            String translationKey,
            MachineIconButton.OnPress onPress
    ) {
        Component action = Component.translatable(translationKey);
        return new MachineIconButton(x, y, 24, 20, icon, action, action, onPress);
    }

    private static Component controlExplanation(String actionKey, Component currentState) {
        return Component.translatable(actionKey)
                .append("\n")
                .append(Component.translatable("gui.magneticraft.control.current_state", currentState));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            int entryIndex = entryIndexAt(
                    mouseX,
                    mouseY,
                    entryScrollOffset,
                    entryCount(),
                    height
            );
            if (entryIndex >= 0) {
                selectEntry(entryIndex);
                return true;
            }
        }
        if (mode != Mode.STRUCTURES) {
            return false;
        }
        if (button == 0 && insideTimeline(mouseX, mouseY)) {
            int layers = layerCount();
            if (layers > 0) {
                int trackWidth = Math.max(1, timelineRight() - timelineLeft() - 1);
                double normalized = Mth.clamp((mouseX - timelineLeft()) / trackWidth, 0.0D, 1.0D);
                layer = Mth.clamp((int) Math.round(normalized * (layers - 1)), 0, layers - 1);
                playingLayers = false;
                lastLayerAdvanceMillis = Util.getMillis();
                updateButtons();
            }
            return true;
        }
        return structurePreview.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double dragX,
            double dragY
    ) {
        if (mode == Mode.STRUCTURES && structurePreview.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean previewReleased = mode == Mode.STRUCTURES && structurePreview.mouseReleased(button);
        return super.mouseReleased(mouseX, mouseY, button) || previewReleased;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (insideEntryList(mouseX, mouseY)) {
            int nextOffset = scrolledEntryOffset(entryScrollOffset, entryCount(), height, scrollY);
            if (nextOffset != entryScrollOffset) {
                entryScrollOffset = nextOffset;
                return true;
            }
        }
        if (mode == Mode.STRUCTURES && structurePreview.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean insideEntryList(double mouseX, double mouseY) {
        return mouseX >= ENTRY_LIST_LEFT
                && mouseX < ENTRY_LIST_RIGHT
                && mouseY >= ENTRY_LIST_TOP
                && mouseY < entryListBottom(height);
    }

    static int visibleEntryCount(int screenHeight) {
        return Math.max(1, (entryListBottom(screenHeight) - ENTRY_LIST_TOP) / ENTRY_ROW_HEIGHT);
    }

    static int clampEntryScroll(int offset, int entryCount, int screenHeight) {
        int maximum = Math.max(0, entryCount - visibleEntryCount(screenHeight));
        return Mth.clamp(offset, 0, maximum);
    }

    static int scrolledEntryOffset(int offset, int entryCount, int screenHeight, double delta) {
        int clamped = clampEntryScroll(offset, entryCount, screenHeight);
        if (delta == 0.0D) {
            return clamped;
        }
        int direction = delta > 0.0D ? -1 : 1;
        int steps = Math.max(1, (int) Math.floor(Math.abs(delta)));
        return clampEntryScroll(clamped + direction * steps, entryCount, screenHeight);
    }

    static int entryIndexAt(
            double mouseX,
            double mouseY,
            int firstEntry,
            int entryCount,
            int screenHeight
    ) {
        if (mouseX < ENTRY_LIST_LEFT
                || mouseX >= ENTRY_LIST_RIGHT - ENTRY_SCROLLBAR_WIDTH - 2
                || mouseY < ENTRY_LIST_TOP
                || mouseY >= entryListBottom(screenHeight)) {
            return -1;
        }
        int row = (int) ((mouseY - ENTRY_LIST_TOP) / ENTRY_ROW_HEIGHT);
        if (row < 0 || row >= visibleEntryCount(screenHeight)) {
            return -1;
        }
        int index = clampEntryScroll(firstEntry, entryCount, screenHeight) + row;
        return index < entryCount ? index : -1;
    }

    private static int entryListBottom(int screenHeight) {
        return Math.max(ENTRY_LIST_TOP + ENTRY_ROW_HEIGHT, screenHeight - ENTRY_LIST_BOTTOM_MARGIN);
    }

    private boolean insideTimeline(double mouseX, double mouseY) {
        return mouseX >= timelineLeft()
                && mouseX < timelineRight()
                && mouseY >= timelineTop() - 2
                && mouseY < timelineTop() + 8;
    }

    private int timelineLeft() {
        return SIDEBAR_WIDTH + 124;
    }

    private int timelineRight() {
        return width - 16;
    }

    private int timelineTop() {
        return height - 21;
    }

    private static int timelineLayerX(int index, int count, int left, int right) {
        if (count <= 1) {
            return (left + right) / 2;
        }
        return left + 2 + Math.round(index * (right - left - 5) / (float) (count - 1));
    }

    private static int structureInfoWidth(int viewportWidth) {
        if (viewportWidth < 390) {
            return 0;
        }
        return Mth.clamp(viewportWidth / 3, 140, 184);
    }

    MachineScreenBounds.Layout layout() {
        return layout(width, height);
    }

    static MachineScreenBounds.Layout layout(int width, int height) {
        int viewportLeft = SIDEBAR_WIDTH + 12;
        int viewportRight = width - 16;
        int viewportWidth = viewportRight - viewportLeft;
        int viewportHeight = height - STRUCTURE_VIEW_BOTTOM_MARGIN - STRUCTURE_VIEW_TOP;
        int infoWidth = structureInfoWidth(viewportWidth);
        int sceneWidth = infoWidth == 0 ? viewportWidth : viewportWidth - infoWidth - 4;
        MachineScreenBounds.Builder builder = MachineScreenBounds.builder("guide", width, height)
                .element("header", new Rect(4, 4, width - 8, 26))
                .element("sidebar", new Rect(8, 24, SIDEBAR_WIDTH - 12, height - 32))
                .element("content", new Rect(
                        SIDEBAR_WIDTH + 4, 34, width - SIDEBAR_WIDTH - 8, height - 38
                ))
                .child("search", new Rect(12, 28, SIDEBAR_WIDTH - 24, 18), "sidebar", null, 0)
                .child("mode", new Rect(width - 36, 8, 24, 20), "header", null, 0)
                .child("entry_list", new Rect(
                        ENTRY_LIST_LEFT,
                        ENTRY_LIST_TOP,
                        ENTRY_LIST_RIGHT - ENTRY_LIST_LEFT,
                        entryListBottom(height) - ENTRY_LIST_TOP
                ), "sidebar", null, 0)
                .child("structure_viewport", new Rect(
                        viewportLeft, STRUCTURE_VIEW_TOP, viewportWidth, viewportHeight
                ), "content", null, 0)
                .child("structure_scene", new Rect(
                        viewportLeft, STRUCTURE_VIEW_TOP, sceneWidth, viewportHeight
                ), "structure_viewport", "structure_columns", 4)
                .child("previous_layer", new Rect(
                        SIDEBAR_WIDTH + 12, height - 28, 24, 20
                ), "content", "navigation", 4)
                .child("play_layers", new Rect(
                        SIDEBAR_WIDTH + 40, height - 28, 24, 20
                ), "content", "navigation", 4)
                .child("reset_structure", new Rect(
                        SIDEBAR_WIDTH + 68, height - 28, 24, 20
                ), "content", "navigation", 4)
                .child("next_layer", new Rect(
                        SIDEBAR_WIDTH + 96, height - 28, 24, 20
                ), "content", "navigation", 4)
                .child("layer_timeline", new Rect(
                        SIDEBAR_WIDTH + 124, height - 21,
                        Math.max(1, width - SIDEBAR_WIDTH - 140), 6
                ), "content", "navigation", 4);
        if (infoWidth > 0) {
            builder.child("structure_information", new Rect(
                    viewportRight - infoWidth,
                    STRUCTURE_VIEW_TOP,
                    infoWidth,
                    viewportHeight
            ), "structure_viewport", "structure_columns", 4);
        }
        return builder.build();
    }

    private List<GuideRepository.MultiblockGuide> filteredStructures() {
        String query = query();
        return GuideRepository.INSTANCE.multiblocks().stream()
                .filter(guide -> query.isEmpty()
                        || guide.id().toString().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable(guide.translationKey()).getString().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable(guide.descriptionKey()).getString().toLowerCase(Locale.ROOT).contains(query)
                        || guide.recipeType().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<GuideRepository.OpcodeGuide> filteredOpcodes() {
        String query = query();
        return GuideRepository.INSTANCE.opcodes().stream()
                .filter(opcode -> query.isEmpty()
                        || opcode.id().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable(opcode.descriptionKey()).getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<GuideRepository.ComputerLanguageGuide> filteredLanguages() {
        String query = query();
        return GuideRepository.INSTANCE.languages().stream()
                .filter(language -> query.isEmpty()
                        || language.id().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable("guide.magneticraft.computer.language."
                                + language.id() + ".name").getString().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable("guide.magneticraft.computer.language."
                                + language.id() + ".description").getString().toLowerCase(Locale.ROOT).contains(query)
                        || language.commands().stream().anyMatch(command -> command.toLowerCase(Locale.ROOT).contains(query)))
                .toList();
    }

    private List<GuideRepository.MachineGuide> filteredMachines() {
        String query = query();
        return GuideRepository.INSTANCE.machines().stream()
                .filter(machine -> query.isEmpty()
                        || machine.id().toString().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable(machine.translationKey()).getString().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable(machine.descriptionKey()).getString().toLowerCase(Locale.ROOT).contains(query)
                        || machine.recipeType().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<GuideRepository.ItemGuide> filteredItems() {
        String query = query();
        return GuideRepository.INSTANCE.items().stream()
                .filter(item -> query.isEmpty()
                        || item.id().toString().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable(item.translationKey()).getString().toLowerCase(Locale.ROOT).contains(query)
                        || Component.translatable(item.descriptionKey()).getString().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private String query() {
        return search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private enum Mode {
        STRUCTURES("gui.magneticraft.guide.mode.structures"),
        MACHINES("gui.magneticraft.guide.mode.machines"),
        ITEMS("gui.magneticraft.guide.mode.items"),
        LANGUAGES("gui.magneticraft.guide.mode.languages"),
        OPCODES("gui.magneticraft.guide.mode.opcodes");

        private final String titleKey;

        Mode(String titleKey) {
            this.titleKey = titleKey;
        }

        private Component title() {
            return Component.translatable(titleKey);
        }
    }
}
