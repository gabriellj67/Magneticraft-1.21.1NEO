package committee.nova.mods.magneticraft.content.nuclear.thermal;

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

/** Read-only server-synchronized status for variable nuclear thermal facilities. */
public final class NuclearThermalMenu extends AbstractMachineMenu {
    public static final int IMAGE_WIDTH = 232;
    public static final int IMAGE_HEIGHT = 190;
    public static final int PLAYER_TOP = 108;

    private final BlockPos position;
    private final NuclearThermalFacilityType facilityType;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public NuclearThermalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), buffer.readEnum(NuclearThermalFacilityType.class), null);
    }

    public NuclearThermalMenu(
            int containerId, Inventory inventory, NuclearThermalControllerBlockEntity controller
    ) {
        this(containerId, inventory, controller.getBlockPos(), controller.facilityType(), controller);
    }

    private NuclearThermalMenu(
            int containerId,
            Inventory inventory,
            BlockPos position,
            NuclearThermalFacilityType facilityType,
            NuclearThermalControllerBlockEntity controller
    ) {
        super(ModMenus.NUCLEAR_THERMAL_FACILITY.get(), containerId);
        this.position = position.immutable();
        this.facilityType = facilityType;
        access = ContainerLevelAccess.create(inventory.player.level(), position);
        data = controller == null
                ? new SimpleContainerData(NuclearThermalControllerBlockEntity.MENU_DATA_COUNT)
                : controller.menuData();
        addDataSlots(data);
        finishMachineSlots(inventory, 35, PLAYER_TOP);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) return true;
        return access.evaluate((level, blockPos) ->
                level.getBlockEntity(blockPos) instanceof NuclearThermalControllerBlockEntity controller
                        && controller.facilityType() == facilityType && controller.formed()
                        && controller.canManage(player), false);
    }

    public BlockPos position() { return position; }
    public NuclearThermalFacilityType facilityType() { return facilityType; }
    public boolean formed() { return value(0) != 0; }
    public Direction facing() { return Direction.from2DDataValue(value(1)); }
    public int width() { return value(2); }
    public int length() { return value(3); }
    public int height() { return value(4); }
    public int portFlags() { return value(5); }
    public boolean working() { return value(6) != 0; }
    public boolean backpressured() { return value(7) != 0; }
    public int transferRate() { return value(8); }
    public int transferredHeat() { return value(9); }
    public int exchangerBlocks() { return value(10); }
    public int fillBlocks() { return value(11); }
    public int fanBlocks() { return value(12); }
    public int storedJoules() { return value(13); }
    public int capacityJoules() { return value(14); }
    public double heatTemperatureKelvin() { return value(15) / 10.0D; }
    public int primaryAmount() { return value(16); }
    public int secondaryAmount() { return value(17); }
    public int inputAmount() { return value(18); }
    public int outputAmount() { return value(19); }

    public boolean hasPort(NuclearThermalPortRole role) {
        return (portFlags() & 1 << role.ordinal()) != 0;
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        return false;
    }

    private int value(int logicalIndex) {
        return Int32ContainerData.read(data, logicalIndex);
    }
}
