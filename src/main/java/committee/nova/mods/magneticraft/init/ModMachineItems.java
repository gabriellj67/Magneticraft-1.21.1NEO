package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.item.ElectricChainsawItem;
import committee.nova.mods.magneticraft.content.item.ElectricDrillItem;
import committee.nova.mods.magneticraft.content.item.ElectricPistonItem;
import committee.nova.mods.magneticraft.content.item.LowBatteryItem;
import committee.nova.mods.magneticraft.content.item.MediumBatteryItem;
import committee.nova.mods.magneticraft.content.item.OilProspectorItem;
import committee.nova.mods.magneticraft.content.item.PressureGaugeItem;
import committee.nova.mods.magneticraft.content.item.ThermometerItem;
import committee.nova.mods.magneticraft.content.item.VoltmeterItem;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineRotorItem;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineRotorTier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

/**
 * Registers portable items introduced by the machine framework.
 */
public final class ModMachineItems {
    public static final DeferredHolder<Item, Item> LOW_BATTERY =
            ModRegistries.ITEMS.register("low_voltage_battery", LowBatteryItem::new);
    public static final DeferredHolder<Item, Item> MEDIUM_BATTERY =
            ModRegistries.ITEMS.register("medium_voltage_battery", MediumBatteryItem::new);
    public static final DeferredHolder<Item, Item> ELECTRIC_DRILL =
            ModRegistries.ITEMS.register("electric_drill", ElectricDrillItem::new);
    public static final DeferredHolder<Item, Item> ELECTRIC_CHAINSAW =
            ModRegistries.ITEMS.register("electric_chainsaw", ElectricChainsawItem::new);
    public static final DeferredHolder<Item, Item> ELECTRIC_PISTON =
            ModRegistries.ITEMS.register("electric_piston", ElectricPistonItem::new);
    public static final DeferredHolder<Item, Item> VOLTMETER =
            ModRegistries.ITEMS.register("voltmeter", () -> new VoltmeterItem());
    public static final DeferredHolder<Item, Item> THERMOMETER =
            ModRegistries.ITEMS.register("thermometer", () -> new ThermometerItem());
    public static final DeferredHolder<Item, Item> OIL_PROSPECTOR =
            ModRegistries.ITEMS.register("oil_prospector", OilProspectorItem::new);
    public static final DeferredHolder<Item, Item> PRESSURE_GAUGE =
            ModRegistries.ITEMS.register("pressure_gauge", PressureGaugeItem::new);
    public static final DeferredHolder<Item, Item> INSERTER_SPEED_UPGRADE =
            ModRegistries.ITEMS.register("inserter_speed_upgrade", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> INSERTER_STACK_UPGRADE =
            ModRegistries.ITEMS.register("inserter_stack_upgrade", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> SMALL_WIND_TURBINE_ROTOR = ModRegistries.ITEMS.register(
            WindTurbineRotorTier.SMALL.id(),
            () -> new WindTurbineRotorItem(WindTurbineRotorTier.SMALL)
    );
    public static final DeferredHolder<Item, Item> WIND_TURBINE_ROTOR = ModRegistries.ITEMS.register(
            WindTurbineRotorTier.MEDIUM.id(),
            () -> new WindTurbineRotorItem(WindTurbineRotorTier.MEDIUM)
    );
    public static final DeferredHolder<Item, Item> LARGE_WIND_TURBINE_ROTOR = ModRegistries.ITEMS.register(
            WindTurbineRotorTier.LARGE.id(),
            () -> new WindTurbineRotorItem(WindTurbineRotorTier.LARGE)
    );

    private ModMachineItems() {
    }

    public static void bootstrap() {
    }

    public static List<DeferredHolder<Item, ? extends Item>> creativeItems() {
        return List.of(
                LOW_BATTERY,
                MEDIUM_BATTERY,
                ELECTRIC_DRILL,
                ELECTRIC_CHAINSAW,
                ELECTRIC_PISTON,
                VOLTMETER,
                THERMOMETER,
                OIL_PROSPECTOR,
                PRESSURE_GAUGE,
                INSERTER_SPEED_UPGRADE,
                INSERTER_STACK_UPGRADE,
                SMALL_WIND_TURBINE_ROTOR,
                WIND_TURBINE_ROTOR,
                LARGE_WIND_TURBINE_ROTOR
        );
    }
}
