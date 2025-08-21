package com.osmb.script.potion;

import com.osmb.api.item.ItemID;

import java.util.List;

public enum StandardPotion implements Potion {
    PRAYER_POTION("Prayer potion", List.of(ItemID.PRAYER_POTION4, ItemID.PRAYER_POTION3, ItemID.PRAYER_POTION2, ItemID.PRAYER_POTION1)),
    SUPER_COMBAT("Super combat potion", List.of(ItemID.SUPER_COMBAT_POTION4, ItemID.SUPER_COMBAT_POTION3, ItemID.SUPER_COMBAT_POTION2, ItemID.SUPER_COMBAT_POTION1)),
    SUPER_STRENGTH_POTION("Super strength poton", List.of(ItemID.SUPER_STRENGTH4, ItemID.SUPER_STRENGTH3, ItemID.SUPER_STRENGTH2, ItemID.SUPER_STRENGTH1)),
    RANGING_POTION("Ranging potion", List.of(ItemID.RANGING_POTION4, ItemID.RANGING_POTION3, ItemID.RANGING_POTION2, ItemID.RANGING_POTION1)),
    BASTION_POTION("Bastion potion", List.of(ItemID.BASTION_POTION4, ItemID.BASTION_POTION3, ItemID.BASTION_POTION2, ItemID.BASTION_POTION1)),
    BATTLEMAGE_POTION("Battlemage potion", List.of(ItemID.BATTLEMAGE_POTION4, ItemID.BATTLEMAGE_POTION3, ItemID.BATTLEMAGE_POTION2, ItemID.BATTLEMAGE_POTION1));

    private final String name;
    private final List<Integer> itemIDs;

    StandardPotion(String name, List<Integer> itemIDs) {
        this.name = name;
        this.itemIDs = itemIDs;
    }

    @Override
    public List<Integer> getItemIDs() {
        return itemIDs;
    }

    @Override
    public String getName() {
        return name;
    }
}
