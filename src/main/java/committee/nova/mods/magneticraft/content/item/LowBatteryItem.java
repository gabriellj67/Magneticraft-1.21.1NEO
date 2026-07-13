package committee.nova.mods.magneticraft.content.item;

/**
 * Portable 250 kFE cell used by the migrated battery block.
 */
public final class LowBatteryItem extends PortableEnergyItem {
    public static final int CAPACITY = 250_000;
    public static final int TRANSFER_RATE = PortableEnergyItem.TRANSFER_RATE;

    public LowBatteryItem() {
        super(CAPACITY);
    }
}
