package committee.nova.mods.magneticraft.content.machine.windturbine;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class WindTurbineRotorItem extends Item {
    private final WindTurbineRotorTier tier;

    public WindTurbineRotorItem(WindTurbineRotorTier tier) {
        super(new Item.Properties().stacksTo(1));
        this.tier = Objects.requireNonNull(tier);
    }

    public WindTurbineRotorTier tier() {
        return tier;
    }

    @Nullable
    public static WindTurbineRotorTier tier(ItemStack stack) {
        return stack.getItem() instanceof WindTurbineRotorItem rotor ? rotor.tier : null;
    }
}
