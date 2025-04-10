package com.osmb.script.fletching.data;

import com.osmb.api.item.ItemID;

public enum Arrow implements ItemIdentifier, Combinable {
    HEADLESS_ARROW(ItemID.HEADLESS_ARROW, ItemID.ARROW_SHAFT),
    BRONZE_ARROW(ItemID.BRONZE_ARROW, ItemID.BRONZE_ARROWTIPS),
    IRON_ARROW(ItemID.IRON_ARROW, ItemID.IRON_ARROWTIPS),
    STEEL_ARROW(ItemID.STEEL_ARROW, ItemID.STEEL_ARROWTIPS),
    MITHRIL_ARROW(ItemID.MITHRIL_ARROW, ItemID.MITHRIL_ARROWTIPS),
    ADAMANT_ARROW(ItemID.ADAMANT_ARROW, ItemID.ADAMANT_ARROWTIPS),
    RUNE_ARROW(ItemID.RUNE_ARROW, ItemID.RUNE_ARROWTIPS),
    BROAD_ARROW(ItemID.BROAD_ARROWS_4160, ItemID.BROAD_ARROWHEADS),
    AMETHYST_ARROW(ItemID.AMETHYST_ARROW, ItemID.AMETHYST_ARROWTIPS),
    DRAGON_ARROW(ItemID.DRAGON_ARROW, ItemID.DRAGON_ARROWTIPS),;

    private final int itemID;
    private final int unfinishedID;

    Arrow(int itemID, int unfinishedID) {
        this.itemID = itemID;
        this.unfinishedID = unfinishedID;
    }

    @Override
    public int getItemID() {
        return itemID;
    }

    @Override
    public int getUnfinishedID() {
        return unfinishedID;
    }

}
