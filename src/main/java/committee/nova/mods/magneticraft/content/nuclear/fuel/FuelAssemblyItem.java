package committee.nova.mods.magneticraft.content.nuclear.fuel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Optional;

/** Non-stackable fuel item whose complete in-core state survives removal and reinsertion. */
public final class FuelAssemblyItem extends Item {
    public static final String STATE_TAG = "magneticraft_fuel";

    private final NuclearFuelGrade grade;

    public FuelAssemblyItem(NuclearFuelGrade grade) {
        super(new Item.Properties().stacksTo(1));
        this.grade = grade;
    }

    public NuclearFuelGrade grade() {
        return grade;
    }

    public Optional<FuelAssemblyState> state(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.copyTag().contains(STATE_TAG)) {
            return Optional.of(FuelAssemblyState.fresh(grade, 0L));
        }
        return FuelAssemblyState.load(data.copyTag().getCompound(STATE_TAG))
                .filter(value -> value.fuelId().equals(grade.definitionId()));
    }

    public boolean writeState(ItemStack stack, FuelAssemblyState state) {
        if (state == null || !state.fuelId().equals(grade.definitionId())) {
            return false;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(STATE_TAG, state.save()));
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return state(stack).map(value -> value.burnupFraction() > 0.0D).orElse(true);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return state(stack)
                .map(value -> Math.max(0, Math.min(13, (int) Math.round(13.0D * (1.0D - value.burnupFraction())))))
                .orElse(0);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x5CCB5F;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        state(stack).ifPresentOrElse(value -> {
            tooltip.add(Component.translatable(
                    "item.magneticraft.fuel_assembly.burnup",
                    Math.round(value.burnupFraction() * 100.0D)
            ).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(
                    "item.magneticraft.fuel_assembly.cladding",
                    Math.round(value.claddingIntegrity() * 100.0D)
            ).withStyle(value.claddingIntegrity() < 0.5D ? ChatFormatting.RED : ChatFormatting.GRAY));
            tooltip.add(Component.translatable(
                    "item.magneticraft.fuel_assembly.temperature",
                    Math.round(value.temperatureKelvin())
            ).withStyle(ChatFormatting.GRAY));
        }, () -> tooltip.add(Component.translatable(
                "item.magneticraft.fuel_assembly.invalid_state"
        ).withStyle(ChatFormatting.RED)));
    }
}
