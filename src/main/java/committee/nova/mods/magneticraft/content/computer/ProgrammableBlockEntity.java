package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.vm.BoundedComputerVm;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerDevice;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
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
public abstract class ProgrammableBlockEntity extends MachineBlockEntity implements ComputerDevice {
    private static final int VM_FORMAT_VERSION = 1;
    private static final String VM_VERSION_TAG = "vm_version";
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

    private final BoundedComputerVm vm = new BoundedComputerVm();
    @Nullable
    private UUID owner;
    private long programRevision;
    private int redstoneOutput;

    protected ProgrammableBlockEntity(BlockEntityType<?> type, BlockPos position, BlockState state) {
        super(type, position, state);
    }

    protected final void tickComputer() {
        int executed = vm.executeTick(this);
        if (executed > 0) {
            markChanged();
        }
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
        programRevision = programRevision == Long.MAX_VALUE ? 0L : programRevision + 1L;
        markChangedAndSync();
        return true;
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

    public final int redstoneOutput() {
        return redstoneOutput;
    }

    public final Component statusComponent() {
        if (vm.fault() != VmFault.NONE) {
            return Component.translatable(
                    "message.magneticraft.computer.fault",
                    vm.fault().name().toLowerCase(java.util.Locale.ROOT)
            );
        }
        return Component.translatable(
                vm.running()
                        ? "message.magneticraft.computer.running"
                        : "message.magneticraft.computer.stopped",
                vm.programCounter(),
                programRevision
        );
    }

    @Override
    public final DeviceResult execute(ComputerOpcode opcode, int operand) {
        if (!opcode.isDeviceInstruction()) {
            return DeviceResult.fault(VmFault.UNSUPPORTED_DEVICE_INSTRUCTION);
        }
        if (opcode == ComputerOpcode.SET_REDSTONE) {
            setRedstoneOutput(operand);
            return DeviceResult.completeAndYield(redstoneOutput);
        }
        return executeWorldInstruction(opcode, operand);
    }

    protected DeviceResult executeWorldInstruction(ComputerOpcode opcode, int operand) {
        return DeviceResult.fault(VmFault.UNSUPPORTED_DEVICE_INSTRUCTION);
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
        tag.putInt(VM_VERSION_TAG, VM_FORMAT_VERSION);
        ProgramNbt.writeProgram(tag, PROGRAM_TAG, vm.program());
        tag.putInt(PROGRAM_COUNTER_TAG, vm.programCounter());
        tag.putIntArray(REGISTERS_TAG, vm.copyRegisters());
        tag.putIntArray(RAM_TAG, vm.copyRam());
        tag.putBoolean(RUNNING_TAG, vm.running());
        tag.putString(FAULT_TAG, vm.fault().name());
        tag.putInt(LAST_RESULT_TAG, vm.lastResult());
        tag.putLong(PROGRAM_REVISION_TAG, programRevision);
        tag.putInt(REDSTONE_OUTPUT_TAG, redstoneOutput);
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
        if (tag.getInt(VM_VERSION_TAG) != VM_FORMAT_VERSION) {
            vm.restore(List.of(), 0, new int[0], new int[0], false, VmFault.INVALID_SNAPSHOT, 0);
            loadProgrammableData(tag);
            return;
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

    protected void saveProgrammableData(CompoundTag tag) {
    }

    protected void loadProgrammableData(CompoundTag tag) {
    }

    @Override
    protected final void saveClientData(CompoundTag tag) {
        tag.putInt(REDSTONE_OUTPUT_TAG, redstoneOutput);
        tag.putLong(PROGRAM_REVISION_TAG, programRevision);
    }

    @Override
    protected final void loadClientData(CompoundTag tag) {
        redstoneOutput = Math.max(0, Math.min(15, tag.getInt(REDSTONE_OUTPUT_TAG)));
        programRevision = Math.max(0L, tag.getLong(PROGRAM_REVISION_TAG));
    }
}
