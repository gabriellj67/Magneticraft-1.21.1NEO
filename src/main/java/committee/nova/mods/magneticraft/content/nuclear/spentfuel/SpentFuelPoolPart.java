package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public record SpentFuelPoolPart(Kind kind, @Nullable Direction facing) {
    public enum Kind { AIR, WATER, CASING, CONTROLLER, PORT }
}
