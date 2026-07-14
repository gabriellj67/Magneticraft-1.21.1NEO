package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.client.guide.GuideRepository;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * Resource-pack-driven guide with searchable structures, portable equipment and VM opcodes.
 */
public final class GuideScreen extends Screen {
    private static final int SIDEBAR_WIDTH = 150;

    private EditBox search;
    private Button modeButton;
    private Button previousEntry;
    private Button nextEntry;
    private Button previousLayer;
    private Button nextLayer;
    private Mode mode = Mode.STRUCTURES;
    private int selectedIndex;
    private int layer;
    private Component hoveredLegend = Component.empty();

    public GuideScreen() {
        super(Component.translatable("gui.magneticraft.guide.title"));
    }

    @Override
    protected void init() {
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
            layer = 0;
        });
        modeButton = addRenderableWidget(Button.builder(mode.title(), ignored -> toggleMode())
                .bounds(width - 116, 8, 104, 20)
                .build());
        previousEntry = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> select(-1))
                .bounds(12, height - 28, 24, 20)
                .build());
        nextEntry = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> select(1))
                .bounds(40, height - 28, 24, 20)
                .build());
        previousLayer = addRenderableWidget(Button.builder(Component.literal("<"), ignored -> selectLayer(-1))
                .bounds(SIDEBAR_WIDTH + 12, height - 28, 24, 20)
                .build());
        nextLayer = addRenderableWidget(Button.builder(Component.literal(">"), ignored -> selectLayer(1))
                .bounds(SIDEBAR_WIDTH + 40, height - 28, 24, 20)
                .build());
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.fill(4, 4, SIDEBAR_WIDTH, height - 4, 0xE61B2026);
        graphics.fill(SIDEBAR_WIDTH + 4, 34, width - 4, height - 34, 0xE61B2026);
        graphics.drawString(font, title, 12, 10, 0xFFE6EDF3, false);
        hoveredLegend = Component.empty();
        switch (mode) {
            case STRUCTURES -> renderStructures(graphics, mouseX, mouseY);
            case MACHINES -> renderMachines(graphics);
            case ITEMS -> renderItems(graphics);
            case LANGUAGES -> renderLanguages(graphics);
            case OPCODES -> renderOpcodes(graphics);
        }
        updateButtons();
        super.render(graphics, mouseX, mouseY, partialTick);
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
        renderEntryList(graphics, guides.stream()
                .map(guide -> Component.translatable(guide.translationKey()))
                .toList());

        int contentLeft = SIDEBAR_WIDTH + 12;
        graphics.drawString(font, Component.translatable(selected.translationKey()), contentLeft, 42, 0xFFE6EDF3, false);
        graphics.drawString(
                font,
                Component.translatable("gui.magneticraft.guide.category." + selected.category()),
                contentLeft,
                54,
                0xFF8B949E,
                false
        );
        int contentWidth = Math.max(80, width - contentLeft - 16);
        Component description = Component.translatable(selected.descriptionKey());
        graphics.drawWordWrap(font, description, contentLeft, 66, contentWidth, 0xFFB8C0C8);
        int descriptionBottom = 66 + Math.max(1, font.split(description, contentWidth).size()) * 9 + 3;
        int metadataY = renderStructureMetadata(graphics, selected, contentLeft, descriptionBottom);
        layer = Math.max(0, Math.min(layer, selected.layers().size() - 1));
        Component layerText = Component.translatable(
                "gui.magneticraft.guide.layer",
                layer + 1,
                selected.layers().size()
        );
        graphics.drawString(font, layerText, contentLeft, metadataY, 0xFFB8C0C8, false);
        renderLayer(graphics, selected, contentLeft, metadataY + 14, mouseX, mouseY);
    }

    private int renderStructureMetadata(
            GuiGraphics graphics,
            GuideRepository.MultiblockGuide guide,
            int x,
            int y
    ) {
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
        int lineWidth = Math.max(80, width - x - 16);
        int cursorY = y;
        for (Component line : lines) {
            graphics.drawWordWrap(font, line, x, cursorY, lineWidth, 0xFFB8C0C8);
            cursorY += Math.max(1, font.split(line, lineWidth).size()) * 9;
        }
        return cursorY + 3;
    }

    private void renderOpcodes(GuiGraphics graphics) {
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
        renderEntryList(graphics, opcodes.stream().map(opcode -> Component.literal(opcode.id())).toList());

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

    private void renderLanguages(GuiGraphics graphics) {
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
                .toList());

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

    private void renderItems(GuiGraphics graphics) {
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
                .toList());

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

    private void renderMachines(GuiGraphics graphics) {
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
                .toList());

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

    private void renderEntryList(GuiGraphics graphics, List<? extends Component> entries) {
        int visible = Math.max(1, (height - 90) / 11);
        int first = Math.max(0, Math.min(selectedIndex - visible / 2, entries.size() - visible));
        int last = Math.min(entries.size(), first + visible);
        for (int index = first; index < last; index++) {
            int y = 54 + (index - first) * 11;
            int color = index == selectedIndex ? 0xFF79C0FF : 0xFFB8C0C8;
            graphics.drawString(font, entries.get(index), 12, y, color, false);
        }
    }

    private void renderLayer(
            GuiGraphics graphics,
            GuideRepository.MultiblockGuide guide,
            int x,
            int y,
            int mouseX,
            int mouseY
    ) {
        List<String> rows = guide.layers().get(layer);
        int columns = rows.stream().mapToInt(String::length).max().orElse(1);
        int availableWidth = Math.max(1, width - x - 16);
        int availableHeight = Math.max(1, height - y - 44);
        int cell = Math.max(3, Math.min(12, Math.min(availableWidth / Math.max(1, columns), availableHeight / rows.size())));
        for (int row = 0; row < rows.size(); row++) {
            String rowText = rows.get(row);
            for (int column = 0; column < rowText.length(); column++) {
                char symbol = rowText.charAt(column);
                GuideRepository.LegendEntry legend = guide.legend().get(symbol);
                String rule = legend == null ? "unknown" : legend.rule();
                int cellX = x + column * cell;
                int cellY = y + row * cell;
                graphics.fill(cellX, cellY, cellX + cell - 1, cellY + cell - 1, ruleColor(rule));
                if (cell >= 8) {
                    graphics.drawString(font, Character.toString(symbol), cellX + 1, cellY, 0xFF101418, false);
                }
                if (mouseX >= cellX && mouseX < cellX + cell && mouseY >= cellY && mouseY < cellY + cell) {
                    hoveredLegend = Component.translatable("gui.magneticraft.guide.rule." + rule);
                }
            }
        }
    }

    private void toggleMode() {
        Mode[] modes = Mode.values();
        mode = modes[(mode.ordinal() + 1) % modes.length];
        selectedIndex = 0;
        layer = 0;
        modeButton.setMessage(mode.title());
        updateButtons();
    }

    private void select(int amount) {
        int size = switch (mode) {
            case STRUCTURES -> filteredStructures().size();
            case MACHINES -> filteredMachines().size();
            case ITEMS -> filteredItems().size();
            case LANGUAGES -> filteredLanguages().size();
            case OPCODES -> filteredOpcodes().size();
        };
        if (size == 0) {
            return;
        }
        selectedIndex = Math.floorMod(selectedIndex + amount, size);
        layer = 0;
    }

    private void selectLayer(int amount) {
        List<GuideRepository.MultiblockGuide> guides = filteredStructures();
        if (guides.isEmpty()) {
            return;
        }
        GuideRepository.MultiblockGuide selected = guides.get(Math.max(0, Math.min(selectedIndex, guides.size() - 1)));
        layer = Math.floorMod(layer + amount, selected.layers().size());
    }

    private void updateButtons() {
        if (previousLayer == null) {
            return;
        }
        boolean structureMode = mode == Mode.STRUCTURES;
        previousLayer.visible = structureMode;
        nextLayer.visible = structureMode;
        previousLayer.active = structureMode && !filteredStructures().isEmpty();
        nextLayer.active = previousLayer.active;
        boolean hasEntries = switch (mode) {
            case STRUCTURES -> !filteredStructures().isEmpty();
            case MACHINES -> !filteredMachines().isEmpty();
            case ITEMS -> !filteredItems().isEmpty();
            case LANGUAGES -> !filteredLanguages().isEmpty();
            case OPCODES -> !filteredOpcodes().isEmpty();
        };
        previousEntry.active = hasEntries;
        nextEntry.active = hasEntries;
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

    private static int ruleColor(String rule) {
        return switch (rule) {
            case "air" -> 0xFF9ECFF5;
            case "controller" -> 0xFFFFB454;
            case "base" -> 0xFF8B949E;
            case "grate" -> 0xFFA7B1BC;
            case "corrugated_iron" -> 0xFFC1CAD4;
            case "copper_coil" -> 0xFFD98245;
            case "bricks" -> 0xFFB04B4B;
            case "small_tank" -> 0xFF4FC3C8;
            case "striped" -> 0xFFE7C547;
            case "electric" -> 0xFF4DA3FF;
            case "column_x", "column_y", "column_z" -> 0xFF787F88;
            case "ignore" -> 0xFF30363D;
            default -> 0xFFFF00FF;
        };
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
