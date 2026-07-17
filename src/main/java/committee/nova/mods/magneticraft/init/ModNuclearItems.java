package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyItem;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.content.nuclear.material.NuclearMaterial;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearControllerUpgrade;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Registers the open fuel-cycle process items and the three stateful LEU assemblies. */
public final class ModNuclearItems {
    private static final Map<NuclearMaterial, RegistryObject<Item>> MATERIALS =
            new EnumMap<>(NuclearMaterial.class);
    private static final Map<NuclearFuelGrade, RegistryObject<FuelAssemblyItem>> FUEL_ASSEMBLIES =
            new EnumMap<>(NuclearFuelGrade.class);
    private static final Map<NuclearControllerUpgrade, RegistryObject<Item>> CONTROLLER_UPGRADES =
            new EnumMap<>(NuclearControllerUpgrade.class);
    private static final List<RegistryObject<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();

    static {
        for (NuclearMaterial material : NuclearMaterial.values()) {
            RegistryObject<Item> item = ModRegistries.ITEMS.register(
                    material.id(),
                    () -> new Item(new Item.Properties())
            );
            MATERIALS.put(material, item);
            CREATIVE_ITEMS.add(item);
        }
        for (NuclearFuelGrade grade : NuclearFuelGrade.values()) {
            RegistryObject<FuelAssemblyItem> item = ModRegistries.ITEMS.register(
                    grade.id(),
                    () -> new FuelAssemblyItem(grade)
            );
            FUEL_ASSEMBLIES.put(grade, item);
            CREATIVE_ITEMS.add(item);
        }
        for (NuclearControllerUpgrade upgrade : NuclearControllerUpgrade.values()) {
            RegistryObject<Item> item = ModRegistries.ITEMS.register(
                    upgrade.id(), () -> new Item(new Item.Properties().stacksTo(1)));
            CONTROLLER_UPGRADES.put(upgrade, item);
            CREATIVE_ITEMS.add(item);
        }
    }

    private ModNuclearItems() {
    }

    public static void bootstrap() {
    }

    public static RegistryObject<Item> material(NuclearMaterial material) {
        RegistryObject<Item> item = MATERIALS.get(material);
        if (item == null) {
            throw new IllegalArgumentException("No nuclear material registered for " + material);
        }
        return item;
    }

    public static RegistryObject<FuelAssemblyItem> fuelAssembly(NuclearFuelGrade grade) {
        RegistryObject<FuelAssemblyItem> item = FUEL_ASSEMBLIES.get(grade);
        if (item == null) {
            throw new IllegalArgumentException("No fuel assembly registered for " + grade);
        }
        return item;
    }

    public static Map<NuclearMaterial, RegistryObject<Item>> materials() {
        return Collections.unmodifiableMap(MATERIALS);
    }

    public static Map<NuclearFuelGrade, RegistryObject<FuelAssemblyItem>> fuelAssemblies() {
        return Collections.unmodifiableMap(FUEL_ASSEMBLIES);
    }

    public static RegistryObject<Item> controllerUpgrade(NuclearControllerUpgrade upgrade) {
        RegistryObject<Item> item = CONTROLLER_UPGRADES.get(upgrade);
        if (item == null) {
            throw new IllegalArgumentException("No controller upgrade registered for " + upgrade);
        }
        return item;
    }

    public static Map<NuclearControllerUpgrade, RegistryObject<Item>> controllerUpgrades() {
        return Collections.unmodifiableMap(CONTROLLER_UPGRADES);
    }

    public static List<RegistryObject<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }
}
