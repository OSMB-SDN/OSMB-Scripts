package com.osmb.script.combat.nightmarezone;

import com.osmb.api.location.area.impl.RectangleArea;

public enum AFKPosition {
    ENTRANCE("Entrance area", new RectangleArea(2273, 4680, 6, 3, 0)),
    RANDOM("Random area", null),
    CENTER("Center area", new RectangleArea(2264, 4694, 11, 8, 0)),
    SOUTH_WEST("South west corner", new RectangleArea(2255, 4680, 1, 5, 0)),
    SOUTH_EAST("South east corner", new RectangleArea(2287, 4680, 1, 6, 0));

    private final RectangleArea area;
    private final String name;

    AFKPosition(String name, RectangleArea area) {
        this.name = name;
        this.area = area;
    }

    public RectangleArea getArea() {
        return area;
    }

    @Override
    public String toString() {
        return name;
    }
}
