package com.osmb.script.chartercrafting;

import com.osmb.api.item.ItemID;

import java.util.Arrays;
import java.util.Map;

public enum GlassBlowingItem {
    BEER_GLASS("Beer glass", ItemID.BEER_GLASS, 17.5),
    CANDLE_LANTERN("Candle lantern", ItemID.EMPTY_CANDLE_LANTERN, 19),
    OIL_LAMP("Oil lamp", ItemID.EMPTY_OIL_LAMP, 25),
    VIAL("Vial", ItemID.VIAL, 35),
    FISHBOWL("Fish bowl", ItemID.EMPTY_FISHBOWL, 42.5),
    UNPOWERED_STAFF_ORB("Unpowered staff orb", ItemID.UNPOWERED_ORB, 52.5),
    LANTERN_LENS("Lantern lens", ItemID.LANTERN_LENS, 55);

    private final String name;
    private final int itemId;
    private final double xp;

    GlassBlowingItem(String name, int itemId, double xp) {
        this.name = name;
        this.itemId = itemId;
        this.xp = xp;
    }

    public static int[] getItemIds() {
        return Arrays.stream(values())
                .mapToInt(GlassBlowingItem::getItemId)
                .toArray();
    }

    public static GlassBlowingItem forItemId(int itemId) {
        for (GlassBlowingItem item : values()) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }

    public int getItemId() {
        return itemId;
    }

    public double getXp() {
        return xp;
    }

    @Override
    public String toString() {
        return name;
    }
}

