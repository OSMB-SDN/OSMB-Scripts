package com.osmb.script.smithing.data;

import com.osmb.api.item.ItemID;

public enum Bar {
    BRONZE(ItemID.BRONZE_BAR),
    IRON(ItemID.IRON_BAR),
    STEEL(ItemID.STEEL_BAR),
    MITHRIL(ItemID.MITHRIL_BAR),
    ADAMANT(ItemID.ADAMANTITE_BAR),
    RUNE(ItemID.RUNITE_BAR);

    public int getItemID() {
        return itemID;
    }

    private final int itemID;

    Bar(int itemID) {
        this.itemID = itemID;
    }

    public static Integer[] getIDValues() {
        Bar[] bars = values();

        Integer[] ids = new Integer[bars.length];

        for (int i = 0; i < bars.length; i++) {
            ids[i] = bars[i].getItemID();
        }
        return ids;
    }
}
