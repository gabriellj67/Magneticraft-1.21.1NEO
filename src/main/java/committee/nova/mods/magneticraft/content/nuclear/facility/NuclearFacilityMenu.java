package committee.nova.mods.magneticraft.content.nuclear.facility;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Read-only status plus explicit input/output inventories for front-end facilities. */
public final class NuclearFacilityMenu extends AbstractMachineMenu {
    public static final int IMAGE_WIDTH = 214;
    public static final int IMAGE_HEIGHT = 185;
    public static final int PLAYER_TOP = 103;

    private final BlockPos position;
    private final NuclearFacilityType facilityType;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public NuclearFacilityMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), buffer.readEnum(NuclearFacilityType.class), null);
    }

    public NuclearFacilityMenu(
            int containerId, Inventory inventory, NuclearFacilityControllerBlockEntity controller
    ) {
        this(containerId, inventory, controller.getBlockPos(), controller.facilityType(), controller);
    }

    private NuclearFacilityMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos position,
            NuclearFacilityType facilityType,
            NuclearFacilityControllerBlockEntity controller
    ) {
        super(ModMenus.NUCLEAR_FACILITY.get(), containerId);
        this.position = position.immutable();
        this.facilityType = facilityType;
        access = ContainerLevelAccess.create(playerInventory.player.level(), position);
        data = controller == null
                ? new SimpleContainerData(NuclearFacilityControllerBlockEntity.MENU_DATA_COUNT)
                : controller.menuData();
        addDataSlots(data);

        IItemHandler inventory = controller == null
                ? new ItemStackHandler(NuclearFacilityControllerBlockEntity.TOTAL_SLOTS)
                : controller.inventory().menuHandler();
        for (int slot = 0; slot < NuclearFacilityControllerBlockEntity.INPUT_SLOTS; slot++) {
            addSlot(new SlotItemHandler(inventory, slot, 26 + slot * 18, 44));
        }
        for (int slot = 0; slot < NuclearFacilityControllerBlockEntity.OUTPUT_SLOTS; slot++) {
            addSlot(new SlotItemHandler(
                    inventory,
                    NuclearFacilityControllerBlockEntity.INPUT_SLOTS + slot,
                    116 + slot * 18,
                    44
            ) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        finishMachineSlots(playerInventory, 26, PLAYER_TOP);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return true;
        }
        return access.evaluate((level, blockPos) ->
                level.getBlockEntity(blockPos) instanceof NuclearFacilityControllerBlockEntity controller
                        && controller.facilityType() == facilityType
                        && controller.formed()
                        && controller.canManage(player), false);
    }

    public BlockPos position() {
        return position;
    }

    public NuclearFacilityType facilityType() {
        return facilityType;
    }

    public boolean formed() {
        return value(0) != 0;
    }

    public Direction facing() {
        return Direction.from2DDataValue(value(1));
    }

    public int width() {
        return value(2);
    }

    public int height() {
        return value(3);
    }

    public int depth() {
        return value(4);
    }

    public int portFlags() {
        return value(5);
    }

    public int progress() {
        return value(6);
    }

    public int totalProgress() {
        return value(7);
    }

    public boolean running() {
        return value(8) != 0;
    }

    public int storedJoules() {
        return value(9);
    }

    public int capacityJoules() {
        return value(10);
    }

    public int activeColumns() {
        return value(11);
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        return moveItemStackTo(stack, 0, NuclearFacilityControllerBlockEntity.INPUT_SLOTS, false);
    }

    private int value(int logicalIndex) {
        return Int32ContainerData.read(data, logicalIndex);
    }
}
