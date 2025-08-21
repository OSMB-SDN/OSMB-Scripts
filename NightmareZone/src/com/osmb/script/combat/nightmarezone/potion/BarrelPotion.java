package com.osmb.script.combat.nightmarezone.potion;

import com.osmb.api.item.ItemID;

import java.util.List;

public enum BarrelPotion implements Potion {
    SUPER_RANGING_POTION(250, "Super ranging potion", List.of(ItemID.SUPER_RANGING_4, ItemID.SUPER_RANGING_3, ItemID.SUPER_RANGING_2, ItemID.SUPER_RANGING_1)),
    SUPER_MAGIC_POTION(250, "Super magic potion", List.of(ItemID.SUPER_MAGIC_POTION_4, ItemID.SUPER_MAGIC_POTION_3, ItemID.SUPER_MAGIC_POTION_2, ItemID.SUPER_MAGIC_POTION_1)),
    OVERLOAD(1500, "Overload potion", List.of(ItemID.OVERLOAD_4, ItemID.OVERLOAD_3, ItemID.OVERLOAD_2, ItemID.OVERLOAD_1)),
    ABSORPTION_POTION(1000, "Absorption potion", List.of(ItemID.ABSORPTION_4, ItemID.ABSORPTION_3, ItemID.ABSORPTION_2, ItemID.ABSORPTION_1));

    private final int costPerDose;
    private final String name;
    private final List<Integer> itemIDs;
    BarrelPotion(int costPerDose, String name, List<Integer> itemIDs) {
        this.costPerDose = costPerDose;
        this.name = name;
        this.itemIDs = itemIDs;
    }

    public int getCostPerDose() {
        return costPerDose;
    }

    @Override
    public List<Integer> getItemIDs() {
        return itemIDs;
    }

    @Override
    public String getName() {
        return name;
    }

    public static BarrelPotion getPotionFromDialogueText(String text) {
        for (BarrelPotion potion : values()) {
            if (text.toLowerCase().contains(potion.getName().toLowerCase())) {
                return potion;
            }
        }
        return null;
    }
}
