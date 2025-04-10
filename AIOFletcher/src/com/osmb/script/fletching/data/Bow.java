package com.osmb.script.fletching.data;

import com.osmb.api.item.ItemID;

public enum Bow implements ItemIdentifier, Combinable{
    SHORTBOWS(ItemID.SHORTBOW_U, ItemID.SHORTBOW),
    LONGBOWS(ItemID.LONGBOW_U, ItemID.LONGBOW),
    OAK_SHORTBOWS(ItemID.OAK_SHORTBOW_U,ItemID.OAK_SHORTBOW),
    OAK_LONGBOWS(ItemID.OAK_LONGBOW_U, ItemID.OAK_LONGBOW),
    WILLOW_SHORTBOWS(ItemID.WILLOW_SHORTBOW_U, ItemID.WILLOW_SHORTBOW),
    WILLOW_LONGBOWS(ItemID.WILLOW_LONGBOW_U, ItemID.WILLOW_LONGBOW),
    MAPLE_SHORTBOWS(ItemID.MAPLE_SHORTBOW_U, ItemID.MAPLE_SHORTBOW),
    MAPLE_LONGBOWS(ItemID.MAPLE_LONGBOW_U, ItemID.MAPLE_LONGBOW),
    YEW_SHORTBOWS(ItemID.YEW_SHORTBOW_U, ItemID.YEW_SHORTBOW),
    YEW_LONGBOWS(ItemID.YEW_LONGBOW_U, ItemID.YEW_LONGBOW),
    MAGIC_SHORTBOWS(ItemID.MAGIC_SHORTBOW_U, ItemID.MAGIC_SHORTBOW),
    MAGIC_LONGBOWS(ItemID.MAGIC_LONGBOW_U, ItemID.MAGIC_LONGBOW);

    private final int unstrungID;
    private final int strungID;

    Bow(int unstrungID, int strungID) {
        this.unstrungID = unstrungID;
        this.strungID = strungID;
    }

    @Override
    public int getItemID() {
        return strungID;
    }


    @Override
    public int getUnfinishedID() {
        return unstrungID;
    }
}
