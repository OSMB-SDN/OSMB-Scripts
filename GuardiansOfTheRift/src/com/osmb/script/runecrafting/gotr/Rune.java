package com.osmb.script.runecrafting.gotr;

import com.osmb.api.item.ItemID;

public interface Rune {


    int getInterfaceSpriteId();

    String getName();

    int getRuneId();

    String getGuardianName();

    int getAltarRegionId();

    int getTier();

    int getTalismanId();

    int getLevelRequirement();

    String getQuestName();

    enum Cell {
        WEAK(ItemID.WEAK_CELL, 1),
        MEDIUM(ItemID.MEDIUM_CELL, 2),
        STRONG(ItemID.STRONG_CELL, 3),
        OVERCHARGED(ItemID.OVERCHARGED_CELL, 4);

        private final int itemID;
        private final int tier;

        Cell(int itemID, int tier) {
            this.itemID = itemID;
            this.tier = tier;
        }

        public int getItemID() {
            return itemID;
        }

        public int getTier() {
            return tier;
        }
    }
}
