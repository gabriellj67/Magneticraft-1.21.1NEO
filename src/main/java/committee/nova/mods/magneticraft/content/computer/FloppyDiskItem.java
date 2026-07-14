package committee.nova.mods.magneticraft.content.computer;

import committee.nova.mods.magneticraft.content.computer.runtime.ScriptLanguage;
import committee.nova.mods.magneticraft.content.computer.runtime.ScriptProgram;
import committee.nova.mods.magneticraft.content.computer.runtime.VirtualDisk;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerInstruction;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** One registry item whose versioned state represents user and historical preset media. */
public final class FloppyDiskItem extends Item {
    public FloppyDiskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean storeProgram(ItemStack stack, List<ComputerInstruction> program) {
        if (readState(stack).map(FloppyDiskPersistence.State::readOnly).orElse(true)) {
            return false;
        }
        FloppyDiskPersistence.writeLegacy(stack.getOrCreateTag(), program);
        return true;
    }

    public static boolean storeScript(ItemStack stack, ScriptProgram program, Optional<VirtualDisk> disk) {
        if (readState(stack).map(FloppyDiskPersistence.State::readOnly).orElse(true)) {
            return false;
        }
        FloppyDiskPersistence.writeScript(stack.getOrCreateTag(), program, disk);
        return true;
    }

    public static Optional<List<ComputerInstruction>> readProgram(ItemStack stack) {
        return readState(stack).flatMap(FloppyDiskPersistence.State::legacyProgram);
    }

    public static Optional<ScriptProgram> readScript(ItemStack stack) {
        return readState(stack).flatMap(FloppyDiskPersistence.State::scriptProgram);
    }

    public static Optional<VirtualDisk> readDisk(ItemStack stack) {
        return readState(stack).flatMap(FloppyDiskPersistence.State::disk).map(VirtualDisk::copy);
    }

    static Optional<FloppyDiskPersistence.State> readState(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? Optional.of(FloppyDiskPersistence.State.empty()) : FloppyDiskPersistence.read(tag);
    }

    public static void configurePreset(ItemStack stack, String preset, @Nullable ScriptLanguage language) {
        FloppyDiskPersistence.writePreset(stack.getOrCreateTag(), preset, language);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        Optional<FloppyDiskPersistence.State> state = readState(stack);
        if (state.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.magneticraft.floppy_disk.corrupt")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        FloppyDiskPersistence.State payload = state.get();
        tooltip.add(Component.translatable("tooltip.magneticraft.floppy_disk.preset", payload.preset())
                .withStyle(ChatFormatting.GRAY));
        payload.scriptProgram().ifPresent(program -> tooltip.add(Component.translatable(
                        "tooltip.magneticraft.floppy_disk.language",
                        program.language().serializedName()
                ).withStyle(ChatFormatting.GRAY)));
        payload.legacyProgram().ifPresent(program -> tooltip.add(Component.translatable(
                        "tooltip.magneticraft.floppy_disk.instructions",
                        program.size()
                ).withStyle(ChatFormatting.GRAY)));
        payload.disk().ifPresent(disk -> tooltip.add(Component.translatable(
                        "tooltip.magneticraft.floppy_disk.storage",
                        disk.usedBytes(),
                        VirtualDisk.CAPACITY_BYTES
                ).withStyle(ChatFormatting.GRAY)));
        if (payload.readOnly()) {
            tooltip.add(Component.translatable("tooltip.magneticraft.floppy_disk.read_only")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
