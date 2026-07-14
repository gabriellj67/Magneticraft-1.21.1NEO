package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.runtime.ComputerDeviceBus;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptRuntime;
import committee.nova.mods.magneticraft.content.computer.runtime.VirtualDisk;
import committee.nova.mods.magneticraft.content.computer.vm.BoundedComputerVm;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerDevice;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Durable owner and VM state shared by computers and mining robots.
 */
public abstract class ProgrammableBlockEntity extends MachineBlockEntity
        implements ComputerDevice, ComputerDeviceBus, MenuProvider {
    private static final String PROGRAM_TAG = "program";
    private static final String PROGRAM_COUNTER_TAG = "program_counter";
    private static final String REGISTERS_TAG = "registers";
    private static final String RAM_TAG = "ram";
    private static final String RUNNING_TAG = "running";
    private static final String FAULT_TAG = "fault";
    private static final String LAST_RESULT_TAG = "last_result";
    private static final String PROGRAM_REVISION_TAG = "program_revision";
    private static final String OWNER_TAG = "owner";
    private static final String REDSTONE_OUTPUT_TAG = "redstone_output";
    private static final String SCRIPT_RUNTIME_TAG = "script_runtime";

    private final BoundedComputerVm vm = new BoundedComputerVm();
    private final ScriptRuntime scriptRuntime = new ScriptRuntime();
    @Nullable
    private UUID owner;
    private long programRevision;
    private int redstoneOutput;
    private boolean clientRunning;

    protected ProgrammableBlockEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state);
    }

    protected final void tickComputer() {
        int executed = usesScriptRuntime()
                ? scriptRuntime.executeTick(this)
                : vm.executeTick(this);
        if (executed > 0) {
            markChanged();
        }
        syncClientState(activeRunning() ? 1 : 0);
    }

    public final boolean tryReplaceProgram(long expectedRevision, List<ComputerInstruction> replacement) {
        Level currentLevel = getLevel();
        if (currentLevel == null || currentLevel.isClientSide || expectedRevision != programRevision) {
            return false;
        }
        try {
            vm.replaceProgram(replacement);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return false;
        }
        scriptRuntime.reset();
        incrementProgramRevision();
        markChangedAndSync();
        return true;
    }

    public final boolean tryReplaceScript(long expectedRevision, ScriptProgram replacement) {
        Level currentLevel = getLevel();
        if (currentLevel == null || currentLevel.isClientSide || expectedRevision != programRevision) {
            return false;
        }
        try {
            scriptRuntime.replaceProgram(replacement);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return false;
        }
        vm.replaceProgram(List.of());
        incrementProgramRevision();
        markChangedAndSync();
        return true;
    }

    private void incrementProgramRevision() {
        programRevision = programRevision == Long.MAX_VALUE ? 0L : programRevision + 1L;
    }

    public final boolean claim(Player player) {
        Level currentLevel = getLevel();
        if (currentLevel == null || currentLevel.isClientSide || owner != null || !player.getAbilities().mayBuild) {
            return false;
        }
        owner = player.getUUID();
        markChanged();
        return true;
    }

    public final boolean canManage(Player player) {
        return player.getAbilities().mayBuild
                && (owner != null && owner.equals(player.getUUID()) || player.hasPermissions(2));
    }

    public final void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        markChanged();
    }

    @Nullable
    public final UUID owner() {
        return owner;
    }

    public final long programRevision() {
        return programRevision;
    }

    public final List<ComputerInstruction> program() {
        return vm.program();
    }

    public final BoundedComputerVm vm() {
        return vm;
    }

    public final ScriptRuntime scriptRuntime() {
        return scriptRuntime;
    }

    public final Optional<ScriptProgram> scriptProgram() {
        return scriptRuntime.program();
    }

    public final Optional<VirtualDisk> virtualDisk() {
        return scriptRuntime.virtualDisk();
    }

    public final boolean replaceVirtualDisk(VirtualDisk disk) {
        boolean replaced = scriptRuntime.replaceVirtualDisk(disk);
        if (replaced) {
            markChanged();
        }
        return replaced;
    }

    public final int activeProgramCounter() {
        return usesScriptRuntime() ? scriptRuntime.programCounter() : vm.programCounter();
    }

    public final boolean activeRunning() {
        return usesScriptRuntime() ? scriptRuntime.running() : vm.running();
    }

    public final VmFault activeFault() {
        return usesScriptRuntime() ? scriptRuntime.fault() : vm.fault();
    }

    public final int activeLastResult() {
        return usesScriptRuntime() ? scriptRuntime.lastResult() : vm.lastResult();
    }

    public final String terminalOutput() {
        return usesScriptRuntime() ? scriptRuntime.output() : "";
    }

    private boolean usesScriptRuntime() {
        return scriptRuntime.hasProgram() || scriptRuntime.fault() != VmFault.NONE;
    }

    public final int redstoneOutput() {
        return redstoneOutput;
    }

    public final boolean visuallyRunning() {
        Level currentLevel = getLevel();
        return currentLevel != null && currentLevel.isClientSide ? clientRunning : activeRunning();
    }

    public final Component statusComponent() {
        if (activeFault() != VmFault.NONE) {
            return Component.translatable(
                    "message.magneticraft.computer.fault",
                    activeFault().name().toLowerCase(java.util.Locale.ROOT)
            );
        }
        return Component.translatable(
                activeRunning()
                        ? "message.magneticraft.computer.running"
                        : "message.magneticraft.computer.stopped",
                activeProgramCounter(),
                programRevision
        );
    }

    @Override
    public final Component getDisplayName() {
        return Component.translatable(
                this instanceof MiningRobotBlockEntity
                        ? "container.magneticraft.mining_robot"
                        : "container.magneticraft.computer"
        );
    }

    @Nullable
    @Override
    public final AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return createMenuForSession(containerId, playerInventory, player, UUID.randomUUID().getLeastSignificantBits());
    }

    public final AbstractContainerMenu createMenuForSession(
            int containerId,
            Inventory playerInventory,
            Player player,
            long sessionToken
    ) {
        return canManage(player) ? new ProgrammableMenu(containerId, playerInventory, this, sessionToken) : null;
    }

    public final void writeMenuOpeningData(FriendlyByteBuf buffer, long sessionToken) {
        ScriptProgram program = scriptProgram().orElseGet(() -> new ScriptProgram(ScriptLanguage.FORTH, ""));
        buffer.writeBoolean(this instanceof MiningRobotBlockEntity);
        buffer.writeBlockPos(worldPosition);
        buffer.writeVarLong(programRevision);
        buffer.writeLong(sessionToken);
        buffer.writeUtf(program.language().serializedName(), 16);
        buffer.writeUtf(program.source(), ScriptRuntime.MAX_SOURCE_BYTES);
        buffer.writeUtf(terminalOutput(), ScriptRuntime.MAX_OUTPUT_CHARACTERS);
    }

    @Override
    public final ComputerDevice.DeviceResult execute(ComputerOpcode opcode, int operand) {
        if (!opcode.isDeviceInstruction()) {
            return ComputerDevice.DeviceResult.fault(VmFault.UNSUPPORTED_DEVICE_INSTRUCTION);
        }
        if (opcode == ComputerOpcode.SET_REDSTONE) {
            setRedstoneOutput(operand);
            return ComputerDevice.DeviceResult.completeAndYield(redstoneOutput);
        }
        return executeWorldInstruction(opcode, operand);
    }

    protected ComputerDevice.DeviceResult executeWorldInstruction(ComputerOpcode opcode, int operand) {
        return ComputerDevice.DeviceResult.fault(VmFault.UNSUPPORTED_DEVICE_INSTRUCTION);
    }

    @Override
    public final ComputerDeviceBus.DeviceResult execute(DeviceCommand command, int argument) {
        if (command == DeviceCommand.SET_REDSTONE) {
            setRedstoneOutput(argument);
            return ComputerDeviceBus.DeviceResult.complete(redstoneOutput);
        }
        return executeWorldCommand(command, argument);
    }

    protected ComputerDeviceBus.DeviceResult executeWorldCommand(DeviceCommand command, int argument) {
        return ComputerDeviceBus.DeviceResult.fault(VmFault.UNSUPPORTED_DEVICE_INSTRUCTION);
    }

    private void setRedstoneOutput(int requestedOutput) {
        int boundedOutput = Math.max(0, Math.min(15, requestedOutput));
        if (redstoneOutput == boundedOutput) {
            return;
        }
        redstoneOutput = boundedOutput;
        markChangedAndSync();
        Level currentLevel = getLevel();
        if (currentLevel != null && !currentLevel.isClientSide) {
            currentLevel.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    protected final void saveMachineData(CompoundTag tag) {
        ProgramNbt.writeProgram(tag, PROGRAM_TAG, vm.program());
        tag.putInt(PROGRAM_COUNTER_TAG, vm.programCounter());
        tag.putIntArray(REGISTERS_TAG, vm.copyRegisters());
        tag.putIntArray(RAM_TAG, vm.copyRam());
        tag.putBoolean(RUNNING_TAG, vm.running());
        tag.putString(FAULT_TAG, vm.fault().name());
        tag.putInt(LAST_RESULT_TAG, vm.lastResult());
        tag.putLong(PROGRAM_REVISION_TAG, programRevision);
        tag.putInt(REDSTONE_OUTPUT_TAG, redstoneOutput);
        tag.put(SCRIPT_RUNTIME_TAG, scriptRuntime.save());
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
        }
        saveProgrammableData(tag);
    }

    @Override
    protected final void loadMachineData(CompoundTag tag) {
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        programRevision = Math.max(0L, tag.getLong(PROGRAM_REVISION_TAG));
        redstoneOutput = Math.max(0, Math.min(15, tag.getInt(REDSTONE_OUTPUT_TAG)));
        if (tag.contains(SCRIPT_RUNTIME_TAG, Tag.TAG_COMPOUND)) {
            scriptRuntime.restore(tag.getCompound(SCRIPT_RUNTIME_TAG));
        } else {
            scriptRuntime.reset();
        }
        Optional<List<ComputerInstruction>> restoredProgram = ProgramNbt.readProgram(tag, PROGRAM_TAG);
        if (restoredProgram.isEmpty()) {
            vm.restore(List.of(), 0, new int[0], new int[0], false, VmFault.INVALID_SNAPSHOT, 0);
            loadProgrammableData(tag);
            return;
        }
        vm.restore(
                restoredProgram.get(),
                tag.getInt(PROGRAM_COUNTER_TAG),
                tag.getIntArray(REGISTERS_TAG),
                tag.getIntArray(RAM_TAG),
                tag.getBoolean(RUNNING_TAG),
                VmFault.fromPersistentName(tag.getString(FAULT_TAG)),
                tag.getInt(LAST_RESULT_TAG)
        );
        loadProgrammableData(tag);
    }

    @Override
    protected final void resetMachineData() {
        owner = null;
        programRevision = 0L;
        setRedstoneOutput(0);
        clientRunning = false;
        vm.replaceProgram(List.of());
        scriptRuntime.reset();
        loadProgrammableData(new CompoundTag());
    }

    protected void saveProgrammableData(CompoundTag tag) {
    }

    protected void loadProgrammableData(CompoundTag tag) {
    }

    @Override
    protected final void saveClientData(CompoundTag tag) {
        tag.putInt(REDSTONE_OUTPUT_TAG, redstoneOutput);
        tag.putLong(PROGRAM_REVISION_TAG, programRevision);
        tag.putBoolean(RUNNING_TAG, activeRunning());
    }

    @Override
    protected final void loadClientData(CompoundTag tag) {
        redstoneOutput = Math.max(0, Math.min(15, tag.getInt(REDSTONE_OUTPUT_TAG)));
        programRevision = Math.max(0L, tag.getLong(PROGRAM_REVISION_TAG));
        clientRunning = tag.getBoolean(RUNNING_TAG);
    }
}
