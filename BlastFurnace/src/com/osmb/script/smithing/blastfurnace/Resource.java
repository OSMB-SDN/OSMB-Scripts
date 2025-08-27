package com.osmb.script.smithing.blastfurnace;

public class Resource {
    private final int itemID;
    private final int amount;

    public Resource(int itemID, int amount) {
        this.itemID = itemID;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public int getItemID() {
        return itemID;
    }
}
