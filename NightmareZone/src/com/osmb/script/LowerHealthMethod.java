package com.osmb.script;

import com.osmb.api.item.ItemID;

public enum LowerHealthMethod {
    ROCK_CAKE(ItemID.DWARVEN_ROCK_CAKE_7510),
    LOCATOR_ORB(ItemID.LOCATOR_ORB);

    private final int itemID;

    LowerHealthMethod(int itemID) {
        this.itemID = itemID;
    }

    public int getItemID() {
        return itemID;
    }
}
