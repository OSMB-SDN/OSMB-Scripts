package com.osmb.script.herblore.data;

public class Ingredient {

    private final int itemID;
    private final int amount;

    public boolean isMandatoryToCombine() {
        return mandatoryToCombine;
    }

    private final boolean mandatoryToCombine;

    public Ingredient(int itemID, int amount) {
        this.itemID = itemID;
        this.amount = amount;
        this.mandatoryToCombine = false;
    }


    public Ingredient(int itemID) {
        this.itemID = itemID;
        this.amount = 1;
        this.mandatoryToCombine = false;
    }

    public Ingredient(int itemID, int amount, boolean mandatoryToCombine) {
        this.itemID = itemID;
        this.amount = amount;
        this.mandatoryToCombine = mandatoryToCombine;
    }

    public int getAmount() {
        return amount;
    }

    public int getItemID() {
        return itemID;
    }
}
