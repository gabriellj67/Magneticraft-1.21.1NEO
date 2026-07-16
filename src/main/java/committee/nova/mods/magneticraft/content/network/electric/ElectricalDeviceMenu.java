package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.content.network.module.ElectricalControlModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalProtectionModule;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.runtime.RedstoneControlMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;

import java.util.Objects;

/** Server-authoritative controls and telemetry for all isolated two-terminal electrical devices. */
public final class ElectricalDeviceMenu extends AbstractMachineMenu {
    public static final int FUSE_SLOT_X = 80;
    public static final int FUSE_SLOT_Y = 43;
    public static final int DATA_COUNT = 16;

    private static final int REVERSED = 0;
    private static final int REDSTONE_MODE = 1;
    private static final int PROFILE_BOUND = 2;
    private static final int TRIPPED = 3;
    private static final int BLOWN = 4;
    private static final int REDSTONE_FORCED_OPEN = 5;
    private static final int THERMAL_STRESS_MILLIS = 6;
    private static final int HEAVY_RATING = 7;
    private static final int FIRST_RING = 8;
    private static final int SECOND_RING = 9;
    private static final int MULTIPLIER_RING = 10;
    private static final int CONTROL_LOSS_MILLIJOULES = 11;
    private static final int CONTROL_CURRENT_MILLIAMPS = 12;
    private static final int CONTROL_DIRECTION = 13;
    private static final int CONTROL_ENABLED = 14;

    private final BlockPos position;
    private final ElectricalDeviceKind kind;
    private final ContainerData data;

    public ElectricalDeviceMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(
                containerId,
                inventory,
                buffer.readBlockPos(),
                ElectricalDeviceKind.decode(buffer.readVarInt()),
                null
        );
    }

    public ElectricalDeviceMenu(
            int containerId,
            Inventory inventory,
            BoxTransformerBlockEntity transformer
    ) {
        this(containerId, inventory, transformer.getBlockPos(), ElectricalDeviceKind.BOX_TRANSFORMER, transformer);
    }

    public ElectricalDeviceMenu(
            int containerId,
            Inventory inventory,
            ElectricalProtectionBlockEntity protection
    ) {
        this(containerId, inventory, protection.getBlockPos(), ElectricalDeviceKind.from(protection.kind()), protection);
    }

    public ElectricalDeviceMenu(
            int containerId,
            Inventory inventory,
            ElectricalControlBlockEntity control
    ) {
        this(containerId, inventory, control.getBlockPos(), ElectricalDeviceKind.from(control.kind()), control);
    }

    private ElectricalDeviceMenu(
            int containerId,
            Inventory inventory,
            BlockPos position,
            ElectricalDeviceKind kind,
            BlockEntity knownDevice
    ) {
        super(ModMenus.ELECTRICAL_DEVICE.get(), containerId);
        this.position = Objects.requireNonNull(position, "position").immutable();
        this.kind = Objects.requireNonNull(kind, "kind");
        data = knownDevice == null ? new SimpleContainerData(DATA_COUNT) : deviceData(knownDevice);
        addDataSlots(data);

        if (kind == ElectricalDeviceKind.FUSE_BOX) {
            IItemHandlerModifiable handler = knownDevice instanceof ElectricalProtectionBlockEntity protection
                    ? new FuseSlotHandler(protection.protection())
                    : new ItemStackHandler(1);
            addSlot(new SlotItemHandler(handler, 0, FUSE_SLOT_X, FUSE_SLOT_Y));
        }
        finishMachineSlots(
                inventory,
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                LegacyMachineGuiLayout.STANDARD_PLAYER_TOP
        );
    }

    public static void open(
            ServerPlayer player,
            MenuProvider provider,
            BlockPos position,
            ElectricalDeviceKind kind
    ) {
        NetworkHooks.openScreen(player, provider, buffer -> {
            buffer.writeBlockPos(position);
            buffer.writeVarInt(kind.ordinal());
        });
    }

    @Override
    public boolean stillValid(Player player) {
        if (!player.level().hasChunk(position.getX() >> 4, position.getZ() >> 4)
                || player.distanceToSqr(
                position.getX() + 0.5D,
                position.getY() + 0.5D,
                position.getZ() + 0.5D
        ) > 64.0D) {
            return false;
        }
        BlockEntity current = player.level().getBlockEntity(position);
        return current != null && !current.isRemoved() && kind.matches(current);
    }

    public boolean applyAction(ServerPlayer player, ElectricalDeviceAction action) {
        if (player == null
                || action == null
                || player.containerMenu != this
                || !player.getAbilities().mayBuild
                || !stillValid(player)) {
            return false;
        }
        BlockEntity current = player.level().getBlockEntity(position);
        return switch (action) {
            case REVERSE_TRANSFORMER -> kind == ElectricalDeviceKind.BOX_TRANSFORMER
                    && current instanceof BoxTransformerBlockEntity transformer
                    && transformer.transformerCoupler().tryReverse();
            case CYCLE_REDSTONE_MODE -> cycleTransformerRedstone(current);
            case RESET_BREAKER -> kind == ElectricalDeviceKind.CIRCUIT_BREAKER
                    && current instanceof ElectricalProtectionBlockEntity protection
                    && protection.kind() == ElectricalProtectionKind.CIRCUIT_BREAKER
                    && protection.protection().resetBreaker();
            case CYCLE_CONTROL_REDSTONE -> applyControlAction(current, ElectricalControlKind.SWITCH, -1);
            case CYCLE_RESISTOR_FIRST_RING -> applyControlAction(current, ElectricalControlKind.RESISTOR, 0);
            case CYCLE_RESISTOR_SECOND_RING -> applyControlAction(current, ElectricalControlKind.RESISTOR, 1);
            case CYCLE_RESISTOR_MULTIPLIER_RING -> applyControlAction(current, ElectricalControlKind.RESISTOR, 2);
        };
    }

    public BlockPos position() {
        return position;
    }

    public ElectricalDeviceKind kind() {
        return kind;
    }

    public boolean transformerReversed() {
        return data.get(REVERSED) != 0;
    }

    public RedstoneControlMode transformerRedstoneMode() {
        RedstoneControlMode[] modes = RedstoneControlMode.values();
        int ordinal = data.get(REDSTONE_MODE);
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : RedstoneControlMode.IGNORED;
    }

    public boolean transformerProfileBound() {
        return data.get(PROFILE_BOUND) != 0;
    }

    public boolean breakerTripped() {
        return data.get(TRIPPED) != 0;
    }

    public boolean fuseBlown() {
        return data.get(BLOWN) != 0;
    }

    public boolean redstoneForcedOpen() {
        return data.get(REDSTONE_FORCED_OPEN) != 0;
    }

    public double thermalStress() {
        return data.get(THERMAL_STRESS_MILLIS) / 1_000.0D;
    }

    public boolean heavyRating() {
        return data.get(HEAVY_RATING) != 0;
    }

    public RedstoneControlMode controlRedstoneMode() {
        return transformerRedstoneMode();
    }

    public int resistorFirstRing() {
        return data.get(FIRST_RING);
    }

    public int resistorSecondRing() {
        return data.get(SECOND_RING);
    }

    public int resistorMultiplierRing() {
        return data.get(MULTIPLIER_RING);
    }

    public double resistorOhms() {
        return Math.max(
                1.0D,
                (resistorFirstRing() * 10.0D + resistorSecondRing())
                        * Math.pow(10.0D, resistorMultiplierRing())
        );
    }

    public double controlLossJoules() {
        return data.get(CONTROL_LOSS_MILLIJOULES) / 1_000.0D;
    }

    public double controlCurrentAmps() {
        return data.get(CONTROL_CURRENT_MILLIAMPS) / 1_000.0D;
    }

    public TransferDirection controlDirection() {
        return TransferDirection.decode(data.get(CONTROL_DIRECTION));
    }

    public boolean controlEnabled() {
        return data.get(CONTROL_ENABLED) != 0;
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        return kind == ElectricalDeviceKind.FUSE_BOX
                && !slots.isEmpty()
                && slots.get(0).mayPlace(stack)
                && moveItemStackTo(stack, 0, 1, false);
    }

    private boolean cycleTransformerRedstone(BlockEntity current) {
        if (kind != ElectricalDeviceKind.BOX_TRANSFORMER
                || !(current instanceof BoxTransformerBlockEntity transformer)) {
            return false;
        }
        transformer.transformerCoupler().cycleRedstoneMode();
        return true;
    }

    private boolean applyControlAction(BlockEntity current, ElectricalControlKind expectedKind, int ring) {
        if (!(current instanceof ElectricalControlBlockEntity control) || control.kind() != expectedKind) {
            return false;
        }
        if (ring < 0) {
            control.control().cycleRedstoneMode();
        } else {
            control.control().cycleRing(ring);
        }
        return true;
    }

    private static ContainerData deviceData(BlockEntity device) {
        if (!(device instanceof BoxTransformerBlockEntity)
                && !(device instanceof ElectricalProtectionBlockEntity)
                && !(device instanceof ElectricalControlBlockEntity)) {
            throw new IllegalArgumentException("Unsupported electrical menu device " + device);
        }
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (device instanceof BoxTransformerBlockEntity transformer) {
                    return switch (index) {
                        case REVERSED -> transformer.transformerCoupler().reversed() ? 1 : 0;
                        case REDSTONE_MODE -> transformer.transformerCoupler().redstoneMode().ordinal();
                        case PROFILE_BOUND -> transformer.transformerCoupler().electricalControllerBound() ? 1 : 0;
                        default -> 0;
                    };
                }
                if (device instanceof ElectricalControlBlockEntity control) {
                    ElectricalControlModule module = control.control();
                    var transfer = module.lastTransfer();
                    return switch (index) {
                        case REDSTONE_MODE -> module.redstoneMode().ordinal();
                        case FIRST_RING -> module.firstRing();
                        case SECOND_RING -> module.secondRing();
                        case MULTIPLIER_RING -> module.multiplierRing();
                        case CONTROL_LOSS_MILLIJOULES -> scaled(module.lastTransfer().lostJoules());
                        case CONTROL_CURRENT_MILLIAMPS -> scaled(module.coupler().lastCurrentAmps());
                        case CONTROL_DIRECTION -> !transfer.moved() ? 0 : transfer.firstWasSource() ? 1 : 2;
                        case CONTROL_ENABLED -> module.enabled() ? 1 : 0;
                        default -> 0;
                    };
                }
                ElectricalProtectionModule protection =
                        ((ElectricalProtectionBlockEntity) device).protection();
                return switch (index) {
                    case TRIPPED -> protection.tripped() ? 1 : 0;
                    case BLOWN -> protection.blown() ? 1 : 0;
                    case REDSTONE_FORCED_OPEN -> protection.redstoneForcedOpen() ? 1 : 0;
                    case THERMAL_STRESS_MILLIS -> (int) Math.round(
                            Math.max(0.0D, Math.min(1.0D, protection.thermalStress())) * 1_000.0D
                    );
                    case HEAVY_RATING -> protection.ratingId().equals(ElectricalRatingIds.HEAVY) ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Read-only server projection. Actions use validated intent packets.
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private static int scaled(double value) {
        if (!Double.isFinite(value) || value <= 0.0D) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.round(value * 1_000.0D));
    }

    public enum TransferDirection {
        NONE("gui.magneticraft.electrical.direction.none"),
        BACK_TO_FRONT("gui.magneticraft.electrical.direction.back_to_front"),
        FRONT_TO_BACK("gui.magneticraft.electrical.direction.front_to_back");

        private final String translationKey;

        TransferDirection(String translationKey) {
            this.translationKey = translationKey;
        }

        public String translationKey() {
            return translationKey;
        }

        private static TransferDirection decode(int value) {
            return value == 1 ? BACK_TO_FRONT : value == 2 ? FRONT_TO_BACK : NONE;
        }
    }

    private static final class FuseSlotHandler implements IItemHandlerModifiable {
        private final ElectricalProtectionModule protection;

        private FuseSlotHandler(ElectricalProtectionModule protection) {
            this.protection = Objects.requireNonNull(protection, "protection");
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            checkSlot(slot);
            protection.setFuseFromMenu(stack);
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            checkSlot(slot);
            return protection.fuse();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            checkSlot(slot);
            if (stack.isEmpty() || !protection.canInsertFuse(stack)) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            if (!simulate) {
                protection.insertFuse(stack);
            }
            return remainder;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            checkSlot(slot);
            return protection.extractFuse(amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            checkSlot(slot);
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            checkSlot(slot);
            return protection.isCompatibleFuse(stack);
        }

        private static void checkSlot(int slot) {
            if (slot != 0) {
                throw new IndexOutOfBoundsException("Fuse slot " + slot);
            }
        }
    }
}
