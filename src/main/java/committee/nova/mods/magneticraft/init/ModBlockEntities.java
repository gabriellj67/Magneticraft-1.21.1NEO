package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingTableBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Machine block entity type registrations.
 */
public final class ModBlockEntities {
    public static final RegistryObject<BlockEntityType<CrushingTableBlockEntity>> CRUSHING_TABLE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "crushing_table",
                    () -> BlockEntityType.Builder.of(
                            CrushingTableBlockEntity::new,
                            ModMachineBlocks.CRUSHING_TABLE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<BatteryBlockEntity>> BATTERY =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "battery",
                    () -> BlockEntityType.Builder.of(
                            BatteryBlockEntity::new,
                            ModMachineBlocks.BATTERY.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_furnace",
                    () -> BlockEntityType.Builder.of(
                            ElectricFurnaceBlockEntity::new,
                            ModMachineBlocks.ELECTRIC_FURNACE.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }

    public static void bootstrap() {
    }
}
