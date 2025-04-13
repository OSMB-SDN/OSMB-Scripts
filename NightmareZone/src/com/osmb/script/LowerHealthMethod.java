package com.osmb.script;

import com.osmb.api.item.ItemID;

public enum LowerHealthMethod {
    ROCK_CAKE(ItemID.DWARVEN_ROCK_CAKE_7510, "Rock cake"),
    LOCATOR_ORB(ItemID.LOCATOR_ORB, "Locator orb");

    private final int itemID;
    private final String name;

    LowerHealthMethod(int itemID, String name) {
        this.itemID = itemID;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static LowerHealthMethod forID(int itemID) {
        for(LowerHealthMethod lowerHealthMethod : values()) {
            if(lowerHealthMethod.itemID == itemID) {
                return lowerHealthMethod;
            }
        }
        return null;
    }

    public int getItemID() {
        return itemID;
    }
}
