package committee.nova.mods.magneticraft;

import net.minecraft.server.Bootstrap;
import net.minecraft.SharedConstants;

/** Test-only bootstrap for unit tests that touch vanilla registries. */
public final class MinecraftTestBootstrap {
    private MinecraftTestBootstrap() {
    }

    public static synchronized void ensureBootstrapped() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (ExceptionInInitializerError error) {
            Throwable root = error;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            if (!(root instanceof NoSuchMethodException)
                    || !root.getMessage().contains("net.minecraftforge.network.NetworkEvent.<init>()")) {
                throw error;
            }
            // Forge's plain-JUnit network hook fails after vanilla registries are
            // fully bootstrapped. GameTests cover networking; these unit tests only
            // need the now-initialized vanilla Item/Fluid registries.
        }
    }
}
