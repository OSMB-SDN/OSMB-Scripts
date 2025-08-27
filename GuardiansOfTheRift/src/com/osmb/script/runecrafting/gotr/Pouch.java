package com.osmb.script.runecrafting.gotr;

import com.osmb.api.item.ItemID;

public enum Pouch {
    SMALL(ItemID.SMALL_POUCH, -1, 3, 3),
    MEDIUM(ItemID.MEDIUM_POUCH, ItemID.MEDIUM_POUCH_5511, 6, 3),
    LARGE(ItemID.LARGE_POUCH, ItemID.LARGE_POUCH_5513, 9, 7),
    GIANT(ItemID.GIANT_POUCH, ItemID.GIANT_POUCH_5515, 12, 9),
    COLOSSAL(ItemID.COLOSSAL_POUCH, ItemID.COLOSSAL_POUCH_26786);

    private final int itemID;
    private final int degradedItemID;

    private int degradedCapacity = 0;
    private int maxCapacity;
    private int currentCapacity = 0;

    Pouch(int itemID, int degradedItemID, int maxCapacity, int degradedCapacity) {
        this.itemID = itemID;
        this.maxCapacity = maxCapacity;
        this.degradedItemID = degradedItemID;
        this.degradedCapacity = degradedCapacity;
    }

    Pouch(int itemID, int degradedItemID) {
        this.itemID = itemID;
        this.maxCapacity = -1;
        this.degradedItemID = degradedItemID;
    }

    public static void registerColossalCapacity(int runecraftingLevel) {
        if (runecraftingLevel >= 85) {
            COLOSSAL.setMaxCapacity(40);
            COLOSSAL.degradedCapacity = 35;
        } else if (runecraftingLevel >= 75) {
            COLOSSAL.setMaxCapacity(27);
            COLOSSAL.degradedCapacity = 23;
        } else if (runecraftingLevel >= 50) {
            COLOSSAL.setMaxCapacity(16);
            COLOSSAL.degradedCapacity = 13;
        } else if (runecraftingLevel >= 25) {
            COLOSSAL.setMaxCapacity(8);
            COLOSSAL.degradedCapacity = 5;
        } else {
            COLOSSAL.setMaxCapacity(1);
        }
    }

    public int getDegradedItemID() {
        return degradedItemID;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getItemID() {
        return itemID;
    }

    public int getCurrentCapacity() {
        return currentCapacity;
    }

    public void setCurrentCapacity(int currentCapacity) {
        this.currentCapacity = Math.min(currentCapacity, maxCapacity);
    }

    public void deductFromCurrentCapacity(int amount) {
        currentCapacity = Math.max(currentCapacity - amount, 0);
    }

    public int getSpaceLeft() {
        return maxCapacity - currentCapacity;
    }

    boolean isFull() {
        return currentCapacity >= maxCapacity;
    }

    public int getDegradedCapacity() {
        return degradedCapacity;
    }

    public enum RepairType {
        APPRENTICE_CORDELIA("Apprentice Cordelia"),
        NPC_CONTACT("NPC contact (Lunars required)");
        private final String name;

        RepairType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
