package com.osmb.script.crafting.chartercrafting;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.utils.RandomUtils;

import java.util.List;

public class Status {
    public static int amountChangeTimeout;
    public static ItemGroupResult inventorySnapshot;
    public static boolean hopFlag = false;
    public static List<NPC> npcs;
    public static void resetAmountChangeTimeout() {
        amountChangeTimeout = RandomUtils.uniformRandom(4500, 7000);
    }
}
