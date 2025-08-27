package com.osmb.script.smithing.furnace.data;

import com.osmb.api.item.ItemID;
import com.osmb.script.smithing.furnace.Product;
import com.osmb.script.smithing.furnace.Resource;

import java.util.Collections;
import java.util.List;

public enum Bar implements Product {
    GOLD(ItemID.GOLD_BAR, "Gold bar", List.of(new Resource(ItemID.GOLD_ORE, 1))),
    SILVER(ItemID.SILVER_BAR, "Silver bar", List.of(new Resource(ItemID.SILVER_ORE, 1))),
    BRONZE(ItemID.BRONZE_BAR, "Bronze bar", List.of(new Resource(ItemID.COPPER_ORE, 1), new Resource(ItemID.TIN_ORE, 1))),
    IRON(ItemID.IRON_BAR, "Iron bar", List.of(new Resource(ItemID.IRON_ORE, 1))),
    STEEL(ItemID.STEEL_BAR, "Steel bar", List.of(new Resource(ItemID.IRON_ORE, 1), new Resource(ItemID.COAL, 2))),
    MITHRIL(ItemID.MITHRIL_BAR, "Mithril bar", List.of(new Resource(ItemID.MITHRIL_ORE, 1), new Resource(ItemID.COAL, 4))),
    ADAMANT(ItemID.ADAMANTITE_BAR, "Adamant bar", List.of(new Resource(ItemID.ADAMANTITE_ORE, 1), new Resource(ItemID.COAL, 6))),
    RUNE(ItemID.RUNITE_BAR, "Rune bar", List.of(new Resource(ItemID.RUNITE_ORE, 1), new Resource(ItemID.COAL, 8)));

    private final int itemID;
    private final String productName;
    private final List<Resource> resources;

    Bar(int itemID, String productName, List<Resource> resources) {
        this.itemID = itemID;
        this.productName = productName;
        this.resources = resources;
    }

    public int getItemID() {
        return itemID;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public List<Integer> getMouldIDs() {
        return Collections.emptyList();
    }

    @Override
    public List<Resource> getResources() {
        return resources;
    }

}
