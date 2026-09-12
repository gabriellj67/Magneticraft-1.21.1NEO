package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Crushing-table sound events.
 */
public final class ModSounds {
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUSHING_HIT = register("crushing_table_hit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUSHING_FINAL = register("crushing_table_complete");

    private ModSounds() {
    }

    public static void bootstrap() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String id) {
        return ModRegistries.SOUND_EVENTS.register(
                id,
                () -> SoundEvent.createVariableRangeEvent(Magneticraft.id(id))
        );
    }
}
