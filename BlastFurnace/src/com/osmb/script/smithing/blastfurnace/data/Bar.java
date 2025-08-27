package com.osmb.script.smithing.blastfurnace.data;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.script.smithing.blastfurnace.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum Bar {
    BRONZE(List.of(new Resource(ItemID.TIN_ORE, 1), new Resource(ItemID.COPPER_ORE, 1)), 0, ItemID.BRONZE_BAR),
    IRON(List.of(new Resource(ItemID.IRON_ORE, 1)), 0, ItemID.IRON_BAR),
    STEEL(List.of(new Resource(ItemID.IRON_ORE, 1)), 1, ItemID.STEEL_BAR),
    MITHRIL(List.of(new Resource(ItemID.MITHRIL_ORE, 1)), 2, ItemID.MITHRIL_BAR),
    ADAMANTITE(List.of(new Resource(ItemID.ADAMANTITE_ORE, 1)), 3, ItemID.ADAMANTITE_BAR),
    RUNITE(List.of(new Resource(ItemID.RUNITE_ORE, 1)), 4, ItemID.RUNITE_BAR),
    SILVER(List.of(new Resource(ItemID.SILVER_ORE, 1)), 0, ItemID.SILVER_BAR),
    GOLD(List.of(new Resource(ItemID.GOLD_ORE, 1)), 0, ItemID.GOLD_BAR);

    private static final Map<Bar, String> BAR_NAMES = new HashMap<>();
    private final List<Resource> resources;
    private final int barID;
    private final int coalAmount;

    Bar(List<Resource> resources, int coalAmount, int barID) {
        this.resources = resources;
        this.barID = barID;
        this.coalAmount = coalAmount;
    }

    public int getCoalAmount() {
        return coalAmount;
    }

    public int getBarID() {
        return barID;
    }

    public List<Resource> getOres() {
        return resources;
    }

    public String getBarName(ScriptCore core) {
        if (BAR_NAMES.containsKey(this)) {
            return BAR_NAMES.get(this);
        }
        String itemName = core.getItemManager().getItemName(barID);
        BAR_NAMES.put(this, itemName);
        return itemName;
    }

    public boolean hasResourcesForBar(Set<Ore> ores) {
        for (Resource resource : resources) {
            if (ores.stream().noneMatch(ore -> ore.getItemID() == resource.getItemID())) {
                return false;
            }
        }
        return true;
    }

    public static Bar getBarForOre(Ore ore) {
        for (Bar bar : Bar.values()) {
            if (bar.getOres().stream().anyMatch(resource -> resource.getItemID() == ore.getItemID())) {
                return bar;
            }
        }
        return null;
    }
}
