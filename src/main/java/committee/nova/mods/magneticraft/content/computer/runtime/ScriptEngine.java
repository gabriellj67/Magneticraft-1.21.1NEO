package committee.nova.mods.magneticraft.content.computer.runtime;

import committee.nova.mods.magneticraft.content.computer.vm.VmFault;
import net.minecraft.nbt.CompoundTag;

interface ScriptEngine {
    int executeTick(ComputerDeviceBus bus, int instructionBudget);

    boolean running();

    int programCounter();

    int lastResult();

    VmFault fault();

    String output();

    CompoundTag saveState();

    boolean restoreState(CompoundTag state);
}
