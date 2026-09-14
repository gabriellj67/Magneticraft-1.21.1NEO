package committee.nova.mods.magneticraft.content.nuclear.radiation;

import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyItem;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyState;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Shielded long-term container that only accepts spent fuel after pool cooling. */
public final class SealedSpentFuelCaskItem extends Item {
    public static final String FUEL_TAG = "sealed_spent_fuel";

    public SealedSpentFuelCaskItem() {
        super(new Properties().stacksTo(1));
    }

    public Optional<FuelAssemblyState> fuel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(FUEL_TAG)
                ? FuelAssemblyState.load(data.copyTag().getCompound(FUEL_TAG)) : Optional.empty();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack cask = player.getItemInHand(hand);
        if (fuel(cask).isPresent()) {
            return InteractionResultHolder.pass(cask);
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack assembly = player.getItemInHand(other);
        if (!(assembly.getItem() instanceof FuelAssemblyItem fuelItem)) {
            return InteractionResultHolder.pass(cask);
        }
        Optional<FuelAssemblyState> state = fuelItem.state(assembly).filter(this::safeForEncapsulation);
        if (state.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message.magneticraft.spent_fuel_cask.not_cooled"), true);
            }
            return InteractionResultHolder.fail(cask);
        }
        if (!level.isClientSide) {
            CustomData.update(DataComponents.CUSTOM_DATA, cask, tag -> tag.put(FUEL_TAG, state.orElseThrow().save()));
            if (!player.getAbilities().instabuild) {
                assembly.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(cask, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        fuel(stack).ifPresentOrElse(
                state -> tooltip.add(Component.translatable(
                        "item.magneticraft.sealed_spent_fuel_cask.sealed",
                        Math.round(state.burnupFraction() * 100.0D)).withStyle(ChatFormatting.GREEN)),
                () -> tooltip.add(Component.translatable(
                        "item.magneticraft.sealed_spent_fuel_cask.empty").withStyle(ChatFormatting.GRAY)));
    }

    private boolean safeForEncapsulation(FuelAssemblyState state) {
        var parameters = ReactorParameterRegistry.INSTANCE.current().parameters();
        return state.burnupFraction() > 0.0D
                && state.temperatureKelvin() <= parameters.safeUnloadTemperatureKelvin()
                && state.decayHeatJoules() <= parameters.spentFuelSafeDecayHeatJoules();
    }
}
