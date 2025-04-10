package com.osmb.script.crafting.data;

public class Product implements ItemIdentifier{
    private final int itemID;
    private final int hidesToMake;

    public Product(int itemID, int amountNeeded) {
        this.itemID = itemID;
        this.hidesToMake = amountNeeded;
    }

    public int getAmountNeeded() {
        return hidesToMake;
    }

    @Override
    public int getItemID() {
        return itemID;
    }
}
