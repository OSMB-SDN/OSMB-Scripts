package com.osmb.script.smithing.furnace.data;

import com.osmb.api.item.ItemID;
import com.osmb.script.smithing.furnace.Product;
import com.osmb.script.smithing.furnace.Resource;

import java.util.Collections;
import java.util.List;

public enum Misc implements Product {
    CANNONBALLS(ItemID.CANNONBALL, "Cannonballs (Normal+Double mould)", List.of(new Resource(ItemID.STEEL_BAR, 1)), List.of(ItemID.AMMO_MOULD, ItemID.DOUBLE_AMMO_MOULD)),
    MOLTEN_GLASS(ItemID.MOLTEN_GLASS, "Molten glass", List.of(new Resource(ItemID.BUCKET_OF_SAND, 1), new Resource(ItemID.SODA_ASH, 1)));

    private final int itemID;
    private final String productName;
    private final List<Resource> resources;
    private final List<Integer> mouldIDs;

    Misc(int itemID, String productName, List<Resource> resources) {
        this.itemID = itemID;
        this.productName = productName;
        this.resources = resources;
        this.mouldIDs = Collections.emptyList();
    }

    Misc(int itemID, String productName, List<Resource> resources, List<Integer> mouldIDs) {
        this.itemID = itemID;
        this.productName = productName;
        this.resources = resources;
        this.mouldIDs = mouldIDs;
    }

    @Override
    public int getItemID() {
        return itemID;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public List<Integer> getMouldIDs() {
        return mouldIDs;
    }

    @Override
    public List<Resource> getResources() {
        return resources;
    }
}
