package com.osmb.script.crafting.data;

import com.osmb.api.item.ItemID;

public enum Hide implements ItemIdentifier{
    LEATHER(ItemID.LEATHER, new Product[]{
            new Product(ItemID.LEATHER_GLOVES, 1),
            new Product(ItemID.LEATHER_BOOTS, 1),
            new Product(ItemID.LEATHER_COWL, 1),
            new Product(ItemID.LEATHER_VAMBRACES, 1),
            new Product(ItemID.LEATHER_BODY, 1),
            new Product(ItemID.LEATHER_CHAPS, 1),
            new Product(ItemID.COIF, 1)
    }),
    HARD_LEATHER(ItemID.HARD_LEATHER, new Product[]{
            new Product(ItemID.HARDLEATHER_BODY, 1)
    }),
    SNAKE_SKIN(ItemID.SNAKESKIN, new Product[]{
            new Product(ItemID.SNAKESKIN_BANDANA, 5),
            new Product(ItemID.SNAKESKIN_BODY,15),
            new Product(ItemID.SNAKESKIN_BOOTS, 6),
            new Product(ItemID.SNAKESKIN_CHAPS, 12),
            new Product(ItemID.SNAKESKIN_VAMBRACES, 8)
    }),
    GREEN_DHIDE(ItemID.GREEN_DRAGONHIDE, new Product[]{
            new Product(ItemID.GREEN_DHIDE_BODY, 3),
            new Product(ItemID.GREEN_DHIDE_CHAPS,2),
            new Product(ItemID.GREEN_DHIDE_VAMBRACES,1)
    }),
    BLUE_DHIDE(ItemID.BLUE_DRAGONHIDE, new Product[]{
            new Product(ItemID.BLUE_DHIDE_BODY,3),
            new Product(ItemID.BLUE_DHIDE_CHAPS, 2),
            new Product(ItemID.BLUE_DHIDE_VAMBRACES, 1)}),
    RED_DHIDE(ItemID.RED_DRAGONHIDE,new Product[]{
            new Product(ItemID.RED_DHIDE_BODY,3),
            new Product(ItemID.RED_DHIDE_CHAPS, 2),
            new Product(ItemID.RED_DHIDE_VAMBRACES, 1)}),
    BLACK_DHIDE(ItemID.BLACK_DRAGONHIDE,new Product[]{
            new Product(ItemID.BLACK_DHIDE_BODY,3),
            new Product(ItemID.BLACK_DHIDE_CHAPS, 2),
            new Product(ItemID.BLACK_DHIDE_VAMBRACES, 1)});

    private final int itemID;

    public Product[] getCraftables() {
        return craftables;
    }

    private final Product[] craftables;
    Hide(int itemID, Product[] craftables) {
        this.itemID = itemID;
        this.craftables = craftables;
    }

    @Override
    public int getItemID() {
        return itemID;
    }

}

