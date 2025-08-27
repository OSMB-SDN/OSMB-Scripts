package com.osmb.script.smithing.blastfurnace.data;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;

public enum Ore {
    TIN(ItemID.TIN_ORE),
    COPPER(ItemID.COPPER_ORE),
    IRON(ItemID.IRON_ORE),
    MITHRIL(ItemID.MITHRIL_ORE),
    ADAMANTITE(ItemID.ADAMANTITE_ORE),
    RUNITE(ItemID.RUNITE_ORE),
    SILVER(ItemID.SILVER_ORE),
    GOLD(ItemID.GOLD_ORE),
    COAL(ItemID.COAL);

    private final int itemID;

    Ore(int itemID) {
        this.itemID = itemID;
    }

    public String getOreName(ScriptCore core) {
        return core.getItemManager().getItemName(itemID);
    }

    public static Ore fromItemID(int itemID) {
        for (Ore ore : values()) {
            if (ore.itemID == itemID) {
                return ore;
            }
        }
        return null;
    }


    public int getItemID() {
        return itemID;
    }
}
