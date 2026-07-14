package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.init.ModMenus;
import io.netty.handler.codec.DecoderException;
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
 * Server-authoritative editor snapshot and optional mining-robot inventory.
 */
public final class ProgrammableMenu extends AbstractMachineMenu {
    private static final int LOGICAL_DATA_COUNT = 7;
    private static final int PHYSICAL_DATA_COUNT = LOGICAL_DATA_COUNT * 2;

    private final BlockPos position;
    private final ContainerLevelAccess access;
    private final boolean miningRobot;
    private final long revision;
    private final long sessionToken;
    private final ScriptLanguage initialLanguage;
    private final String initialSource;
    private final String initialOutput;
    private final ContainerData data;
    private final int machineSlots;
    private int nextUploadSequence;

    public ProgrammableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(containerId, playerInventory, readOpeningData(buffer));
    }

    public ProgrammableMenu(
            int containerId,
            Inventory playerInventory,
            ProgrammableBlockEntity programmable,
            long sessionToken
    ) {
        this(
                containerId,
                playerInventory,
                new OpeningData(
                        programmable.getBlockPos(),
                        programmable instanceof MiningRobotBlockEntity,
                        programmable.programRevision(),
                        sessionToken,
                        programmable.scriptProgram().orElseGet(() -> new ScriptProgram(ScriptLanguage.FORTH, "")),
                        programmable.terminalOutput(),
                        programmable
                )
        );
    }

    private ProgrammableMenu(int containerId, Inventory playerInventory, OpeningData opening) {
        super(ModMenus.PROGRAMMABLE.get(), containerId);
        position = opening.position().immutable();
        miningRobot = opening.miningRobot();
        revision = opening.revision();
        sessionToken = opening.sessionToken();
        initialLanguage = opening.program().language();
        initialSource = opening.program().source();
        initialOutput = opening.output();
        access = ContainerLevelAccess.create(playerInventory.player.level(), position);

        ProgrammableBlockEntity programmable = opening.programmable();
        data = programmable == null
                ? new SimpleContainerData(PHYSICAL_DATA_COUNT)
                : Int32ContainerData.readOnly(
                        programmable::activeProgramCounter,
                        () -> programmable.activeRunning() ? 1 : 0,
                        () -> programmable.activeFault().ordinal(),
                        programmable::redstoneOutput,
                        programmable::activeLastResult,
                        () -> programmable instanceof MiningRobotBlockEntity robot
                                ? robot.energy().getEnergyStored()
                                : 0,
                        () -> programmable instanceof MiningRobotBlockEntity robot
                                ? robot.energy().getMaxEnergyStored()
                                : 0
                );
        addDataSlots(data);

        if (miningRobot) {
            IItemHandler inventory = programmable instanceof MiningRobotBlockEntity robot
                    ? robot.inventory().menuHandler()
                    : new ItemStackHandler(16);
            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 4; column++) {
                    addSlot(new SlotItemHandler(inventory, row * 4 + column, 170 + column * 18, 18 + row * 18));
                }
            }
        }
        machineSlots = slots.size();
        finishMachineSlots(playerInventory, 43, 132);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return true;
        }
        return access.evaluate((level, blockPos) ->
                level.getBlockEntity(blockPos) instanceof ProgrammableBlockEntity programmable
                        && programmable.canManage(player), false);
    }

    public BlockPos position() {
        return position;
    }

    public boolean miningRobot() {
        return miningRobot;
    }

    public long revision() {
        return revision;
    }

    public long sessionToken() {
        return sessionToken;
    }

    public int nextUploadSequence() {
        return nextUploadSequence;
    }

    public ScriptLanguage initialLanguage() {
        return initialLanguage;
    }

    public String initialSource() {
        return initialSource;
    }

    public String initialOutput() {
        return initialOutput;
    }

    public boolean applyUpload(
            Player sender,
            BlockPos requestedPosition,
            long requestedRevision,
            long requestedSession,
            int requestedSequence,
            ScriptProgram program
    ) {
        if (sender == null
                || program == null
                || sender.level().isClientSide
                || !sender.getAbilities().mayBuild
                || !position.equals(requestedPosition)
                || revision != requestedRevision
                || sessionToken != requestedSession
                || nextUploadSequence != requestedSequence
                || !sender.level().getWorldBorder().isWithinBounds(position)
                || !sender.level().hasChunk(position.getX() >> 4, position.getZ() >> 4)
                || sender.distanceToSqr(position.getX() + 0.5D, position.getY() + 0.5D, position.getZ() + 0.5D)
                > 64.0D
                || !stillValid(sender)) {
            return false;
        }
        if (!(sender.level().getBlockEntity(position) instanceof ProgrammableBlockEntity programmable)
                || !programmable.canManage(sender)
                || !programmable.tryReplaceScript(requestedRevision, program)) {
            return false;
        }
        nextUploadSequence++;
        return true;
    }

    public int programCounter() {
        return Int32ContainerData.read(data, 0);
    }

    public boolean running() {
        return Int32ContainerData.read(data, 1) != 0;
    }

    public VmFault fault() {
        int ordinal = Int32ContainerData.read(data, 2);
        VmFault[] faults = VmFault.values();
        return ordinal >= 0 && ordinal < faults.length ? faults[ordinal] : VmFault.INVALID_SNAPSHOT;
    }

    public int redstoneOutput() {
        return Int32ContainerData.read(data, 3);
    }

    public int lastResult() {
        return Int32ContainerData.read(data, 4);
    }

    public int energyStored() {
        return Int32ContainerData.read(data, 5);
    }

    public int energyCapacity() {
        return Int32ContainerData.read(data, 6);
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        return machineSlots > 0 && moveItemStackTo(stack, 0, machineSlots, false);
    }

    private static OpeningData readOpeningData(FriendlyByteBuf buffer) {
        boolean miningRobot = buffer.readBoolean();
        BlockPos position = buffer.readBlockPos();
        long revision = buffer.readVarLong();
        long sessionToken = buffer.readLong();
        ScriptLanguage language = ScriptLanguage.parse(buffer.readUtf(16))
                .orElseThrow(() -> new DecoderException("Unknown computer language"));
        try {
            ScriptProgram program = new ScriptProgram(
                    language,
                    buffer.readUtf(ScriptRuntime.MAX_SOURCE_BYTES)
            );
            return new OpeningData(
                    position,
                    miningRobot,
                    revision,
                    sessionToken,
                    program,
                    buffer.readUtf(ScriptRuntime.MAX_OUTPUT_CHARACTERS),
                    null
            );
        } catch (IllegalArgumentException exception) {
            throw new DecoderException("Invalid computer menu snapshot", exception);
        }
    }

    private record OpeningData(
            BlockPos position,
            boolean miningRobot,
            long revision,
            long sessionToken,
            ScriptProgram program,
            String output,
            ProgrammableBlockEntity programmable
    ) {
    }
}
