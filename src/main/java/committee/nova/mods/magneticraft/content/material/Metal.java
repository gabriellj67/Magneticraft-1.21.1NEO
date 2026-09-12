package committee.nova.mods.magneticraft.content.material;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Stable material semantics carried over from the Nova 1.12 metal catalogue.
 *
 * <p>Every material declares its supported forms explicitly. Registration,
 * tags, recipes and data generation therefore share one source of truth and
 * do not need material-specific exceptions.</p>
 */
public enum Metal {
    IRON("iron", "Iron", "铁", true, true, false, true, true,
            MaterialForm.LIGHT_PLATE, MaterialForm.HEAVY_PLATE, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    GOLD("gold", "Gold", "金", true, true, false, true, true,
            MaterialForm.LIGHT_PLATE, MaterialForm.HEAVY_PLATE, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    COPPER("copper", "Copper", "铜", true, true, false, true, false,
            MaterialForm.NUGGET, MaterialForm.LIGHT_PLATE, MaterialForm.HEAVY_PLATE,
            MaterialForm.CHUNK, MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    LEAD("lead", "Lead", "铅", true, true, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.LIGHT_PLATE,
            MaterialForm.HEAVY_PLATE, MaterialForm.CHUNK, MaterialForm.DUST,
            MaterialForm.ROCKY_CHUNK),
    COBALT("cobalt", "Cobalt", "钴", true, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    TUNGSTEN("tungsten", "Tungsten", "钨", true, true, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.LIGHT_PLATE,
            MaterialForm.HEAVY_PLATE, MaterialForm.CHUNK, MaterialForm.DUST,
            MaterialForm.ROCKY_CHUNK),
    STEEL("steel", "Steel", "钢", false, true, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.LIGHT_PLATE,
            MaterialForm.HEAVY_PLATE, MaterialForm.DUST),
    ALUMINIUM("aluminium", "Aluminium", "铝", true, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    GALENA("galena", "Galena", "方铅矿", true, false, true, false, false,
            MaterialForm.ROCKY_CHUNK),
    MITHRIL("mithril", "Mithril", "秘银", true, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    NICKEL("nickel", "Nickel", "镍", true, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    OSMIUM("osmium", "Osmium", "锇", true, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    SILVER("silver", "Silver", "银", true, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    TIN("tin", "Tin", "锡", true, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    ZINC("zinc", "Zinc", "锌", true, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.NUGGET, MaterialForm.CHUNK,
            MaterialForm.DUST, MaterialForm.ROCKY_CHUNK),
    BRASS("brass", "Brass", "黄铜", false, false, false, false, false,
            MaterialForm.INGOT, MaterialForm.DUST),
    CARBIDE("carbide", "Carbide", "碳化物", false, false, false, false, false,
            MaterialForm.INGOT);

    private final String id;
    private final String englishName;
    private final String chineseName;
    private final boolean ore;
    private final boolean useful;
    private final boolean composite;
    private final boolean vanillaIngot;
    private final boolean vanillaNugget;
    private final Set<MaterialForm> supportedForms;

    Metal(
            String id,
            String englishName,
            String chineseName,
            boolean ore,
            boolean useful,
            boolean composite,
            boolean vanillaIngot,
            boolean vanillaNugget,
            MaterialForm... supportedForms
    ) {
        this.id = id;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.ore = ore;
        this.useful = useful;
        this.composite = composite;
        this.vanillaIngot = vanillaIngot;
        this.vanillaNugget = vanillaNugget;
        EnumSet<MaterialForm> forms = EnumSet.noneOf(MaterialForm.class);
        forms.addAll(Arrays.asList(supportedForms));
        this.supportedForms = Collections.unmodifiableSet(forms);
    }

    public String id() {
        return id;
    }

    public String englishName() {
        return englishName;
    }

    public String chineseName() {
        return chineseName;
    }

    public boolean isOre() {
        return ore;
    }

    public boolean isUseful() {
        return useful;
    }

    public boolean isComposite() {
        return composite;
    }

    public boolean hasVanillaIngot() {
        return vanillaIngot;
    }

    public boolean hasVanillaNugget() {
        return vanillaNugget;
    }

    public Set<MaterialForm> supportedForms() {
        return supportedForms;
    }

    public boolean supports(MaterialForm form) {
        return supportedForms.contains(form);
    }
}
