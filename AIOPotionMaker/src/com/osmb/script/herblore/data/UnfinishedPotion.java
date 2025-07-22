package com.osmb.script.herblore.data;


import com.osmb.api.item.ItemID;

public enum UnfinishedPotion implements Potion{
    AVANTOE(ItemID.AVANTOE_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.AVANTOE), new Ingredient(ItemID.VIAL_OF_WATER)}),
    CADANTINE_BLOOD(ItemID.CADANTINE_BLOOD_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.CADANTINE), new Ingredient(ItemID.VIAL_OF_BLOOD_22446)}),
    CADANTINE(ItemID.CADANTINE_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.CADANTINE), new Ingredient(ItemID.VIAL_OF_WATER)}),
    DWARF_WEED(ItemID.DWARF_WEED_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.DWARF_WEED), new Ingredient(ItemID.VIAL_OF_WATER)}),
    GUAM(ItemID.GUAM_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.GUAM_LEAF), new Ingredient(ItemID.VIAL_OF_WATER)}),
    HARRALANDER(ItemID.HARRALANDER_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.HARRALANDER), new Ingredient(ItemID.VIAL_OF_WATER)}),
    IRIT(ItemID.IRIT_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.IRIT_LEAF), new Ingredient(ItemID.VIAL_OF_WATER)}),
    KWUARM(ItemID.KWUARM_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.KWUARM), new Ingredient(ItemID.VIAL_OF_WATER)}),
    LANTADYME(ItemID.LANTADYME_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.LANTADYME), new Ingredient(ItemID.VIAL_OF_WATER)}),
    MARRENTILL(ItemID.MARRENTILL_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.MARRENTILL), new Ingredient(ItemID.VIAL_OF_WATER)}),
    RANARR(ItemID.RANARR_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.RANARR_WEED), new Ingredient(ItemID.VIAL_OF_WATER)}),
    SNAPDRAGON(ItemID.SNAPDRAGON_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.SNAPDRAGON), new Ingredient(ItemID.VIAL_OF_WATER)}),
    TARROMIN(ItemID.TARROMIN_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.TARROMIN), new Ingredient(ItemID.VIAL_OF_WATER)}),
    TOADFLAX(ItemID.TOADFLAX_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.TOADFLAX), new Ingredient(ItemID.VIAL_OF_WATER)}),
    TORSTOL(ItemID.TORSTOL_POTION_UNF, new Ingredient[]{new Ingredient(ItemID.TORSTOL), new Ingredient(ItemID.VIAL_OF_WATER)});

    private final int itemId;
    private final Ingredient[] ingredients;

    UnfinishedPotion(int itemId, Ingredient[] ingredients) {
        this.itemId = itemId;
        this.ingredients = ingredients;
    }


    @Override
    public Ingredient[] getIngredients() {
        return ingredients;
    }

    @Override
    public int getItemID() {
        return itemId;
    }
}
