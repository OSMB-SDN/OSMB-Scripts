package com.osmb.script.fletching.data;

import com.osmb.api.item.ItemID;

public enum Dart implements ItemIdentifier, Combinable {
    BRONZE_DART(ItemID.BRONZE_DART, ItemID.BRONZE_DART_TIP),
    IRON_DART(ItemID.IRON_DART, ItemID.IRON_DART_TIP),
    STEEL_DART( ItemID.STEEL_DART, ItemID.STEEL_DART_TIP),
    MITHRIL_DART( ItemID.MITHRIL_DART, ItemID.MITHRIL_DART_TIP),
    ADAMANT_DART(ItemID.ADAMANT_DART, ItemID.ADAMANT_DART_TIP),
    RUNE_DART( ItemID.RUNE_DART, ItemID.RUNE_DART_TIP),
    AMETHYST_DART( ItemID.AMETHYST_DART, ItemID.AMETHYST_DART_TIP),
    DRAGON_DART( ItemID.DRAGON_DART, ItemID.DRAGON_DART_TIP);

    private final int tipID;
    private final int itemID;

    Dart(int itemID, int getUnfinishedID) {
        this.tipID = getUnfinishedID;
        this.itemID = itemID;
    }

    @Override
    public int getItemID() {
        return itemID;
    }


    @Override
    public int getUnfinishedID() {
        return tipID;
    }

}
