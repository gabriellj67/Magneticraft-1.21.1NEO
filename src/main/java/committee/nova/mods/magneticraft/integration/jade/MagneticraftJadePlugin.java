package committee.nova.mods.magneticraft.integration.jade;

import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Optional Jade entrypoint. No Jade type is referenced outside this package. */
@WailaPlugin
public final class MagneticraftJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(JadeMachineDataProvider.INSTANCE, MachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(JadeMachineComponentProvider.INSTANCE, Block.class);
    }
}
