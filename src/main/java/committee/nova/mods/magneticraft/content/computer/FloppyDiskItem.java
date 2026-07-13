package committee.nova.mods.magneticraft.content.computer;

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

/**
 * Portable bounded program storage. Execution state remains owned by the target machine.
 */
public final class FloppyDiskItem extends Item {
    public FloppyDiskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void storeProgram(ItemStack stack, List<ComputerInstruction> program) {
        FloppyDiskPersistence.write(stack.getOrCreateTag(), program);
    }

    public static Optional<List<ComputerInstruction>> readProgram(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? Optional.of(List.of()) : FloppyDiskPersistence.read(tag);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        int count = readProgram(stack).map(List::size).orElse(0);
        tooltip.add(Component.translatable("tooltip.magneticraft.floppy_disk.instructions", count)
                .withStyle(ChatFormatting.GRAY));
    }
}
