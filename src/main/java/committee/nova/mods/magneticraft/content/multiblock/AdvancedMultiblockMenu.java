package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Shared bounded menu for all advanced multiblock controllers.
 */
public final class AdvancedMultiblockMenu extends AbstractMachineMenu {
    public static final int MAX_TANKS = 5;

    private final BlockPos position;
    private final MultiblockDefinition definition;
    private final boolean mirrored;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int machineSlots;

    public AdvancedMultiblockMenu(
            MultiblockDefinition definition,
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf buffer
    ) {
        this(
                containerId,
                playerInventory,
                buffer.readBlockPos(),
                validateDefinition(definition, buffer.readEnum(MultiblockDefinition.class)),
                buffer.readBoolean(),
                null
        );
    }

    public AdvancedMultiblockMenu(
            int containerId,
            Inventory playerInventory,
            AdvancedMultiblockBlockEntity controller
    ) {
        this(
                containerId,
                playerInventory,
                controller.getBlockPos(),
                controller.definition(),
                controller.mirrored(),
                controller
        );
    }

    private AdvancedMultiblockMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos position,
            MultiblockDefinition definition,
            boolean mirrored,
            AdvancedMultiblockBlockEntity controller
    ) {
        super(ModMenus.advancedMultiblock(definition).get(), containerId);
        this.position = position.immutable();
        this.definition = definition;
        this.mirrored = mirrored;
        access = ContainerLevelAccess.create(playerInventory.player.level(), position);
        data = controller == null
                ? new SimpleContainerData(AdvancedMultiblockBlockEntity.MENU_DATA_COUNT)
                : controller.menuData();
        addDataSlots(data);

        java.util.List<LegacyMachineGuiLayout.Point> slotLayout = LegacyMachineGuiLayout.multiblockSlots(definition);
        int displayedSlots = slotLayout.size();
        IItemHandler inventory = controller != null && controller.inventory() != null
                ? controller.inventory().menuHandler()
                : new ItemStackHandler(displayedSlots);
        for (int slot = 0; slot < displayedSlots; slot++) {
            LegacyMachineGuiLayout.Point slotPosition = slotLayout.get(slot);
            if (isOutputSlot(definition, slot)) {
                addSlot(new SlotItemHandler(inventory, slot, slotPosition.x(), slotPosition.y()) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            } else {
                addSlot(new SlotItemHandler(inventory, slot, slotPosition.x(), slotPosition.y()));
            }
        }
        machineSlots = slots.size();
        finishMachineSlots(
                playerInventory,
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                LegacyMachineGuiLayout.multiblockPlayerTop(definition)
        );
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return true;
        }
        return access.evaluate((level, blockPos) ->
                level.getBlockEntity(blockPos) instanceof AdvancedMultiblockBlockEntity controller
                        && controller.definition() == definition
                        && controller.formed()
                        && controller.canManage(player), false);
    }

    public BlockPos position() {
        return position;
    }

    public MultiblockDefinition definition() {
        return definition;
    }

    public boolean mirrored() {
        return mirrored;
    }

    public int imageWidth() {
        return LegacyMachineGuiLayout.multiblockSize(definition).width();
    }

    public int imageHeight() {
        return LegacyMachineGuiLayout.multiblockSize(definition).height();
    }

    public int playerInventoryTop() {
        return LegacyMachineGuiLayout.multiblockPlayerTop(definition);
    }

    public int energyStored() {
        return value(0);
    }

    public int energyCapacity() {
        return value(1);
    }

    public int progress() {
        return value(2);
    }

    public int totalProgress() {
        return value(3);
    }

    public boolean working() {
        return value(4) != 0;
    }

    public double temperatureKelvin() {
        return value(5) / 10.0D;
    }

    public double voltage() {
        return value(6) / 10.0D;
    }

    public int bulkAmount() {
        return value(7);
    }

    public int bulkCapacity() {
        return value(8);
    }

    public int installedChests() {
        return value(9);
    }

    public int shelvingSlots() {
        return value(10);
    }

    public int tankCount() {
        return Math.max(0, Math.min(MAX_TANKS, value(11)));
    }

    public int fluidAmount(int index) {
        checkTankIndex(index);
        return value(12 + index * 2);
    }

    public int fluidCapacity(int index) {
        checkTankIndex(index);
        return value(13 + index * 2);
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        return machineSlots > 0 && moveItemStackTo(stack, 0, machineSlots, false);
    }

    private int value(int logicalIndex) {
        return Int32ContainerData.read(data, logicalIndex);
    }

    private void checkTankIndex(int index) {
        if (index < 0 || index >= MAX_TANKS) {
            throw new IndexOutOfBoundsException("Tank " + index);
        }
    }

    private static boolean isOutputSlot(MultiblockDefinition definition, int slot) {
        return slot > 0 && switch (definition) {
            case GRINDER, SIEVE, HYDRAULIC_PRESS, BIG_ELECTRIC_FURNACE -> true;
            default -> false;
        };
    }

    private static MultiblockDefinition validateDefinition(
            MultiblockDefinition registered,
            MultiblockDefinition transmitted
    ) {
        if (registered != transmitted) {
            throw new IllegalArgumentException("Menu type " + registered.id()
                    + " does not match transmitted definition " + transmitted.id());
        }
        return registered;
    }
}
