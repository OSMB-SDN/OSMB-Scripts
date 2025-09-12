package com.osmb.script.herblore;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.utils.RandomUtils;

public class State {
    public static int amountChangeTimeout;
    public static ItemGroupResult inventorySnapshot;

    static {
        resetAmountChangeTimeout();
    }

    public static void resetAmountChangeTimeout() {
        amountChangeTimeout = RandomUtils.uniformRandom(3500, 6000);
    }
}
