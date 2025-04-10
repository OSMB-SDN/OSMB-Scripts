package com.osmb.script;

public class PotionBuyEntry {
    private final int itemId;
    private final int requiredDoses;
    private final int initialStockedDoses;

    public PotionBuyEntry(int itemId, int requiredDoses, int initialStockedDoses) {
        this.itemId = itemId;
        this.requiredDoses = requiredDoses;
        this.initialStockedDoses = initialStockedDoses;
    }

    public int getRequiredDoses() {
        return requiredDoses;
    }

    public int getItemId() {
        return itemId;
    }

    public int getInitialStockedDoses() {
        return initialStockedDoses;
    }
}
