package com.osmb.script.crafting.data;

import com.osmb.api.item.ItemID;

public enum GlassBlowingItem implements ItemIdentifier {
    BEER_GLASS(ItemID.BEER_GLASS),
    EMPTY_CANDLE_LANTERN(ItemID.EMPTY_CANDLE_LANTERN),
    EMPTY_OIL_LAMP(ItemID.EMPTY_OIL_LAMP),
    VIAL(ItemID.VIAL),
    EMPTY_FISHBOWL(ItemID.EMPTY_FISHBOWL),
    UNPOWERED_ORB(ItemID.UNPOWERED_ORB),
    LANTERN_LENS(ItemID.LANTERN_LENS),
    EMPTY_LIGHT_ORB(ItemID.EMPTY_LIGHT_ORB),;

    private final int itemID;

    GlassBlowingItem(int itemID) {
        this.itemID = itemID;
    }

    @Override
    public int getItemID() {
        return itemID;
    }
}
