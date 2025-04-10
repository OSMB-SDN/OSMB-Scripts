package com.osmb.script.fletching.data;

public class Product implements ItemIdentifier, Combinable {

    private final int itemID;
    private final int amountNeeded;
    private final int unfinishedID;

    public Product(int itemID, int unfinishedID, int amountNeeded) {
        this.itemID = itemID;
        this.amountNeeded = amountNeeded;
        this.unfinishedID = unfinishedID;
    }

    public Product(int unfinishedID, int amountNeeded) {
        this.itemID = -1;
        this.amountNeeded = amountNeeded;
        this.unfinishedID = unfinishedID;
    }

    @Override
    public int getUnfinishedID() {
        return unfinishedID;
    }

    @Override
    public int getItemID() {
        if (itemID == -1) {
            return unfinishedID;
        }
        return itemID;
    }

    public int getAmountNeeded() {
        return amountNeeded;
    }

    @Override
    public String toString() {
        return "Product{" + "itemID=" + itemID + ", amountNeeded=" + amountNeeded + ", unfinishedID=" + unfinishedID + '}';
    }
}
