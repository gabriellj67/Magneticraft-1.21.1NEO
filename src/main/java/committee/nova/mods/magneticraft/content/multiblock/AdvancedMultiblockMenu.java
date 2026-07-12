package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
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
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final int machineSlots;

    public AdvancedMultiblockMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(
                containerId,
                playerInventory,
                buffer.readBlockPos(),
                buffer.readEnum(MultiblockDefinition.class),
                null
        );
    }

    public AdvancedMultiblockMenu(
            int containerId,
            Inventory playerInventory,
            AdvancedMultiblockBlockEntity controller
    ) {
        this(containerId, playerInventory, controller.getBlockPos(), controller.definition(), controller);
    }

    private AdvancedMultiblockMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos position,
            MultiblockDefinition definition,
            AdvancedMultiblockBlockEntity controller
    ) {
        super(ModMenus.ADVANCED_MULTIBLOCK.get(), containerId);
        this.position = position.immutable();
        this.definition = definition;
        access = ContainerLevelAccess.create(playerInventory.player.level(), position);
        data = controller == null
                ? new SimpleContainerData(AdvancedMultiblockBlockEntity.MENU_DATA_COUNT)
                : controller.menuData();
        addDataSlots(data);

        int displayedSlots = definition == MultiblockDefinition.SHELVING_UNIT
                ? 0
                : Math.min(definition.inventorySlots(), 4);
        IItemHandler inventory = controller != null && controller.inventory() != null
                ? controller.inventory().menuHandler()
                : new ItemStackHandler(displayedSlots);
        for (int slot = 0; slot < displayedSlots; slot++) {
            addSlot(new SlotItemHandler(inventory, slot, slotX(displayedSlots, slot), 35));
        }
        machineSlots = slots.size();
        finishMachineSlots(playerInventory, 8, 126);
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

    private static int slotX(int count, int index) {
        return 80 - (count - 1) * 9 + index * 18;
    }
}
