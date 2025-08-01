package com.osmb.script.firemaking.wintertodt;

import com.osmb.api.item.ItemID;

public enum Equipment {
    TINDERBOX("Tinderbox", new int[]{ItemID.TINDERBOX, ItemID.BRUMA_TORCH, ItemID.BRUMA_TORCH_OFFHAND}),
    KNIFE("Knife", new int[]{ItemID.KNIFE, Wintertodt.FLETCHING_KNIFE_ID}),
    HAMMER("Hammer", new int[]{ItemID.HAMMER}),
    AXE("Axe", new int[]{
            // normal axes
            ItemID.BRONZE_AXE, ItemID.IRON_AXE, ItemID.STEEL_AXE, ItemID.BLACK_AXE, ItemID.MITHRIL_AXE, ItemID.ADAMANT_AXE, ItemID.RUNE_AXE, ItemID.DRAGON_AXE, ItemID.DRAGON_AXE_OR, ItemID._3RD_AGE_AXE, ItemID.INFERNAL_AXE, ItemID.INFERNAL_AXE_OR, ItemID.CRYSTAL_AXE,
            // felling axes
            ItemID.BRONZE_FELLING_AXE, ItemID.IRON_FELLING_AXE, ItemID.STEEL_FELLING_AXE, ItemID.BLACK_FELLING_AXE, ItemID.MITHRIL_FELLING_AXE, ItemID.ADAMANT_FELLING_AXE, ItemID.RUNE_FELLING_AXE, ItemID.DRAGON_FELLING_AXE, ItemID.CRYSTAL_FELLING_AXE, ItemID._3RD_AGE_FELLING_AXE});

    private final int[] itemIds;
    private final String name;

    Equipment(String name, int[] itemIds) {
        this.name = name;
        this.itemIds = itemIds;
    }

    public String getName() {
        return name;
    }

    public int[] getItemIds() {
        return itemIds;
    }
}
