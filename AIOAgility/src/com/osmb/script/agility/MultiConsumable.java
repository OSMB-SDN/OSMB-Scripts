package com.osmb.script.agility;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum MultiConsumable {
    CAKE(ItemID.CAKE, ItemID._23_CAKE, ItemID.SLICE_OF_CAKE),
    CHOCOLATE_CAKE(ItemID.CHOCOLATE_CAKE, ItemID._23_CHOCOLATE_CAKE, ItemID.CHOCOLATE_SLICE),
    PLAIN_PIZZA(ItemID.PLAIN_PIZZA, ItemID._12_PLAIN_PIZZA),
    MEAT_PIZZA(ItemID.MEAT_PIZZA, ItemID._12_MEAT_PIZZA),
    ANCHOVY_PIZZA(ItemID.ANCHOVY_PIZZA, ItemID._12_ANCHOVY_PIZZA),
    PINEAPPLE_PIZZA(ItemID.PINEAPPLE_PIZZA, ItemID._12_PINEAPPLE_PIZZA),
    SARADOMIN_BREWS(ItemID.SARADOMIN_BREW4, ItemID.SARADOMIN_BREW3, ItemID.SARADOMIN_BREW2, ItemID.SARADOMIN_BREW1),
    SUMMER_PIES(ItemID.SUMMER_PIE, ItemID.HALF_A_SUMMER_PIE);

    private final int[] itemIDs;

    MultiConsumable(int... itemIDs) {
        this.itemIDs = itemIDs;
    }

    public int[] getItemIDs() {
        return itemIDs;
    }

    /**
     * Checks if the given itemID is a multi consumable.
     *
     * @param itemID the item ID to check
     * @return true if the itemID belongs to a multi consumable, false otherwise
     */
    public static MultiConsumable getMultiConsumable(int itemID) {
        for (MultiConsumable consumable : MultiConsumable.values()) {
            for (int id : consumable.getItemIDs()) {
                if (id == itemID) {
                    return consumable;
                }
            }
        }
        return null;
    }

    public static ItemSearchResult getSmallestConsumable(MultiConsumable multiConsumable, Set<ItemSearchResult> itemSearchResults) {
        List<ItemSearchResult> results = new ArrayList<>();
        int biggestIndex = 0;
        for (ItemSearchResult result : itemSearchResults) {
            int itemID = result.getId();
            int index = getArrayIndex(multiConsumable.itemIDs, itemID);
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
}
