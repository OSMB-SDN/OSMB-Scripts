package com.osmb.script;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public enum Potion {
    PRAYER_POTION(ItemID.PRAYER_POTION4, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION1),
    ABSORPTION_POTION("Absorption potion", 1000, ItemID.ABSORPTION_4, ItemID.ABSORPTION_3, ItemID.ABSORPTION_2, ItemID.ABSORPTION_1),
    SUPER_COMBAT(ItemID.SUPER_COMBAT_POTION4, ItemID.SUPER_COMBAT_POTION3, ItemID.SUPER_COMBAT_POTION2, ItemID.SUPER_COMBAT_POTION1),
    OVERLOAD("Overload potion", 1500, ItemID.OVERLOAD_4, ItemID.OVERLOAD_3, ItemID.OVERLOAD_2, ItemID.OVERLOAD_1),
    SUPER_RANGING_POTION("Super ranging potion", 250, ItemID.SUPER_RANGING_4, ItemID.SUPER_RANGING_3, ItemID.SUPER_RANGING_2, ItemID.SUPER_RANGING_1),
    SUPER_MAGIC_POTION("Super magic potion", 250, ItemID.SUPER_MAGIC_POTION_4, ItemID.SUPER_MAGIC_POTION_3, ItemID.SUPER_MAGIC_POTION_2, ItemID.SUPER_MAGIC_POTION_1),
    SUPER_STRENGTH_POTION(ItemID.SUPER_STRENGTH4, ItemID.SUPER_STRENGTH3, ItemID.SUPER_STRENGTH2, ItemID.SUPER_STRENGTH1),
    RANGING_POTION(ItemID.RANGING_POTION4, ItemID.RANGING_POTION3, ItemID.RANGING_POTION2, ItemID.RANGING_POTION1);


    private final int[] itemIDs;
    private final String barrelName;
    private final int costPerDose;

    Potion(String barrelName, int costPerDose, int... itemIDs) {
        this.itemIDs = itemIDs;
        this.costPerDose = costPerDose;
        this.barrelName = barrelName;
    }

    Potion(int... itemIDs) {
        this.itemIDs = itemIDs;
        this.costPerDose = 0;
        this.barrelName = null;
    }

    /**
     * Checks if the given itemID is a multi consumable.
     *
     * @param itemID the item ID to check
     * @return true if the itemID belongs to a multi consumable, false otherwise
     */
    public static Potion getMultiConsumable(int itemID) {
        for (Potion consumable : Potion.values()) {
            for (int id : consumable.getItemIDs()) {
                if (id == itemID) {
                    return consumable;
                }
            }
        }
        return null;
    }

    public static ItemSearchResult getSmallestConsumable(Potion potion, UIResultList<ItemSearchResult> itemSearchResults) {
        List<ItemSearchResult> results = new ArrayList<>();
        int biggestIndex = 0;
        for (ItemSearchResult result : itemSearchResults) {
            int itemID = result.getId();
            int index = getArrayIndex(potion.itemIDs, itemID);
            if (index >= 0) {
                if (index > biggestIndex) {
                    biggestIndex = index;
                    results.clear();
                    results.add(result);
                } else if (index == biggestIndex) {
                    results.add(result);
                }
            }
        }
        return results.get(Utils.random(results.size()));
    }

    private static int getArrayIndex(int[] array, int index) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == index) {
                return i;
            }
        }
        return -1;
    }

    public int getCostPerDose() {
        return costPerDose;
    }

    public String getBarrelName() {
        return barrelName;
    }

    public int[] getItemIDs() {
        return itemIDs;
    }

    public int getDose(int id) {
        for (int i = 0; i < itemIDs.length; i++) {
            if (itemIDs[i] == id) {
                return 4 - i;
            }
        }
        throw new RuntimeException("Incorrect ID (" + id + ") for type: " + this);
    }

    public int getFullID() {
        return itemIDs[0];
    }
}
