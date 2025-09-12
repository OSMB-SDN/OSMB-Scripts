package com.osmb.script.crafting.chartercrafting;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.utils.RandomUtils;

import java.util.List;
import java.util.Set;

public class State {
    public static int craftAmount = -1;
    public static int amountChangeTimeout;
    public static ItemGroupResult inventorySnapshot;
    public static boolean hopFlag = false;
    public static List<NPC> npcs;
    public static boolean smelt = false;

    public static void resetAmountChangeTimeout() {
        amountChangeTimeout = RandomUtils.uniformRandom(4500, 7000);
    }

    /**
     * Add check to ensure we can work with UIM
     *
     * @param inventorySnapshot the current inventory snapshot
     * @return the next amount to craft
     */
    public static void nextCraftAmount(ItemGroupResult inventorySnapshot) {
        int freeSlotsExclItems = inventorySnapshot.getFreeSlots(Set.of(ItemID.BUCKET_OF_SAND, ItemID.SEAWEED, ItemID.SODA_ASH));
        int maxAmount = freeSlotsExclItems / 2;
        // randomise next amount
        craftAmount = Math.min(maxAmount, RandomUtils.uniformRandom(4, 8));
    }
}
