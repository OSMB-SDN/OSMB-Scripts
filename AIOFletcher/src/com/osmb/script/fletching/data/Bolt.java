package com.osmb.script.fletching.data;

import com.osmb.api.item.ItemID;

public enum Bolt implements ItemIdentifier, Combinable {
    BRONZE_BOLTS(ItemID.BRONZE_BOLTS, ItemID.BRONZE_BOLTS_UNF),
    SILVER_BOLTS(ItemID.SILVER_BOLTS, ItemID.SILVER_BOLTS_UNF),
    IRON_BOLTS(ItemID.IRON_BOLTS, ItemID.IRON_BOLTS_UNF),
    STEEL_BOLTS(ItemID.STEEL_BOLTS, ItemID.STEEL_BOLTS_UNF),
    MITHRIL_BOLTS(ItemID.MITHRIL_BOLTS, ItemID.MITHRIL_BOLTS_UNF),
    ADAMANT_BOLTS(ItemID.ADAMANT_BOLTS, ItemID.ADAMANT_BOLTSUNF),
    RUNITE_BOLTS(ItemID.RUNITE_BOLTS, ItemID.RUNITE_BOLTS_UNF),
    DRAGON_BOLTS(ItemID.DRAGON_BOLTS, ItemID.DRAGON_BOLTS_UNF);

    private final int itemID;
    private final int unfID;

    Bolt(int itemID, int unfID) {
        this.itemID = itemID;
        this.unfID = unfID;
    }

    @Override
    public int getItemID() {
        return itemID;
    }

    @Override
    public int getUnfinishedID() {
        return unfID;
    }
}
