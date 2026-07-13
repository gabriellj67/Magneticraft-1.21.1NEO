package committee.nova.mods.magneticraft.content.item;

/**
 * Portable 2.5 MFE cell.
 */
public final class MediumBatteryItem extends PortableEnergyItem {
    public static final int CAPACITY = 2_500_000;

    public MediumBatteryItem() {
        super(CAPACITY);
    }
}
