package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerItem;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.item.SulfurItem;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.material.Metal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registers material forms, crafting components and handheld hammers.
 */
public final class ModItems {
    private static final Map<MaterialForm, Map<Metal, RegistryObject<Item>>> MATERIALS =
            new EnumMap<>(MaterialForm.class);
    private static final Map<CraftingComponent, RegistryObject<Item>> COMPONENTS =
            new EnumMap<>(CraftingComponent.class);
    private static final Map<HammerType, RegistryObject<Item>> HAMMERS =
            new EnumMap<>(HammerType.class);
    private static final List<RegistryObject<? extends Item>> CREATIVE_ITEMS = new ArrayList<>();
    public static final RegistryObject<Item> GUIDE_BOOK = ModRegistries.ITEMS.register(
            "guide_book",
            () -> new Item(new Item.Properties().stacksTo(1))
    );

    static {
        CREATIVE_ITEMS.add(GUIDE_BOOK);
        registerMaterials();
        registerComponents();
        registerHammers();
    }

    private ModItems() {
    }

    public static void bootstrap() {
    }

    public static RegistryObject<Item> material(MaterialForm form, Metal metal) {
        RegistryObject<Item> item = MATERIALS.getOrDefault(form, Map.of()).get(metal);
        if (item == null) {
            throw new IllegalArgumentException("No material item registered for " + metal + " / " + form);
        }
        return item;
    }

    public static Item ingot(Metal metal) {
        return switch (metal) {
            case IRON -> Items.IRON_INGOT;
            case GOLD -> Items.GOLD_INGOT;
            case COPPER -> Items.COPPER_INGOT;
            default -> material(MaterialForm.INGOT, metal).get();
        };
    }

    public static Item nugget(Metal metal) {
        return switch (metal) {
            case IRON -> Items.IRON_NUGGET;
            case GOLD -> Items.GOLD_NUGGET;
            default -> material(MaterialForm.NUGGET, metal).get();
        };
    }

    public static RegistryObject<Item> component(CraftingComponent component) {
        return required(COMPONENTS, component, "crafting component");
    }

    public static RegistryObject<Item> hammer(HammerType type) {
        return required(HAMMERS, type, "hammer");
    }

    public static Map<MaterialForm, Map<Metal, RegistryObject<Item>>> materials() {
        EnumMap<MaterialForm, Map<Metal, RegistryObject<Item>>> copy = new EnumMap<>(MaterialForm.class);
        MATERIALS.forEach((form, entries) -> copy.put(form, Collections.unmodifiableMap(entries)));
        return Collections.unmodifiableMap(copy);
    }

    public static List<RegistryObject<? extends Item>> creativeItems() {
        return List.copyOf(CREATIVE_ITEMS);
    }

    private static void registerMaterials() {
        for (MaterialForm form : MaterialForm.values()) {
            EnumMap<Metal, RegistryObject<Item>> entries = new EnumMap<>(Metal.class);
            for (Metal metal : Metal.values()) {
                if (!form.appliesTo(metal)) {
                    continue;
                }
                RegistryObject<Item> item = ModRegistries.ITEMS.register(
                        form.id(metal),
                        () -> new Item(new Item.Properties())
                );
                entries.put(metal, item);
                CREATIVE_ITEMS.add(item);
            }
            MATERIALS.put(form, entries);
        }
    }

    private static void registerComponents() {
        for (CraftingComponent component : CraftingComponent.values()) {
            RegistryObject<Item> item = ModRegistries.ITEMS.register(
                    component.id(),
                    component == CraftingComponent.SULFUR
                            ? SulfurItem::new
                            : () -> new Item(new Item.Properties())
            );
            COMPONENTS.put(component, item);
            CREATIVE_ITEMS.add(item);
        }
    }

    private static void registerHammers() {
        for (HammerType type : HammerType.values()) {
            RegistryObject<Item> item = ModRegistries.ITEMS.register(type.id(), () -> new HammerItem(type));
            HAMMERS.put(type, item);
            CREATIVE_ITEMS.add(item);
        }
    }

    private static <K> RegistryObject<Item> required(Map<K, RegistryObject<Item>> entries, K key, String kind) {
        RegistryObject<Item> item = entries.get(key);
        if (item == null) {
            throw new IllegalArgumentException("No " + kind + " registered for " + key);
        }
        return item;
    }
}
