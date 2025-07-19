package com.osmb.script;

import com.osmb.api.item.ItemID;

public enum Ammo {
    IRON_ARROW(ItemID.IRON_ARROW),
    STEEL_ARROW(ItemID.STEEL_ARROW),
    MITHRIL_ARROW(ItemID.MITHRIL_ARROW),
    ADAMANT_ARROW(ItemID.ADAMANT_ARROW),
    RUNE_ARROW(ItemID.RUNE_ARROW),
    AMETHYST_ARROW(ItemID.AMETHYST_ARROW),
    DRAGON_ARROW(ItemID.DRAGON_ARROW),
    BRONZE_BOLT(ItemID.BRONZE_BOLTS),
    IRON_BOLT(ItemID.IRON_BOLTS),
    STEEL_BOLT(ItemID.STEEL_BOLTS),
    MITHRIL_BOLT(ItemID.MITHRIL_BOLTS),
    ADAMANT_BOLT(ItemID.ADAMANT_BOLTS),
    RUNE_BOLT(ItemID.RUNITE_BOLTS),
    DRAGON_BOLT(ItemID.DRAGON_BOLTS),
    IRON_KNIFE(ItemID.IRON_KNIFE),
    STEEL_KNIFE(ItemID.STEEL_KNIFE),
    MITHRIL_KNIFE(ItemID.MITHRIL_KNIFE),
    ADAMANT_KNIFE(ItemID.ADAMANT_KNIFE),
    RUNE_KNIFE(ItemID.RUNE_KNIFE),
    BRONZE_DART(ItemID.BRONZE_DART),
    IRON_DART(ItemID.IRON_DART),
    STEEL_DART(ItemID.STEEL_DART),
    MITHRIL_DART(ItemID.MITHRIL_DART),
    ADAMANT_DART(ItemID.ADAMANT_DART),
    RUNE_DART(ItemID.RUNE_DART);

    private final int itemID;

    Ammo(int ItemID) {
        this.itemID = ItemID;
    }

    public static int[] getItemIDs() {
        int[] itemIDs = new int[values().length];
        for (int i = 0; i < itemIDs.length; i++) {
            itemIDs[i] = values()[i].itemID;
        }
        return itemIDs;
    }
    public static Ammo fromItemID(int itemID) {
        for (Ammo ammo : values()) {
            if (ammo.itemID == itemID) {
                return ammo;
            }
        }
        return null; // or throw an exception if preferred
    }

    public int getItemID() {
        return itemID;
    }
}
