package com.osmb.script.crafting.data;

import com.osmb.api.item.ItemID;

public enum Gem implements ItemIdentifier {
    OPAL(ItemID.UNCUT_OPAL, ItemID.OPAL, "Opal"),
    JADE(ItemID.UNCUT_JADE, ItemID.JADE, "Jade"),
    TOPAZ(ItemID.UNCUT_RED_TOPAZ, ItemID.RED_TOPAZ, "Topaz"),
    SAPPHIRE(ItemID.UNCUT_SAPPHIRE, ItemID.SAPPHIRE, "Sapphire"),
    EMERALD(ItemID.UNCUT_EMERALD, ItemID.EMERALD, "Emerald"),
    RUBY(ItemID.UNCUT_RUBY, ItemID.RUBY, "Ruby"),
    DIAMOND(ItemID.UNCUT_DIAMOND, ItemID.DIAMOND, "Diamond"),
    DRAGONSTONE(ItemID.UNCUT_DRAGONSTONE, ItemID.DRAGONSTONE, "Dragonstone"),
    ONYX(ItemID.UNCUT_ONYX, ItemID.ONYX, "Onyx");

    private final int itemID;
    private final int cutID;
    private final String name;
    Gem(int itemID, int cutID, String name) {
        this.itemID = itemID;
        this.cutID = cutID;
        this.name = name;
    }

    public int getCutID() {
        return cutID;
    }

    public String getName() {
        return name;
    }

    @Override
    public int getItemID() {
        return itemID;
    }
}
