package committee.nova.mods.magneticraft.content.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.Nullable;

/**
 * Sulfur retains the legacy 800-tick furnace fuel value.
 */
public final class SulfurItem extends Item {
    public static final int BURN_TIME_TICKS = 800;

    public SulfurItem() {
        super(new Item.Properties());
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        return BURN_TIME_TICKS;
    }
}
