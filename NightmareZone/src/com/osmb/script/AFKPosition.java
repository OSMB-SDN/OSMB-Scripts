package com.osmb.script;

import com.osmb.api.location.area.impl.RectangleArea;

public enum AFKPosition {
    RANDOM(null),
    ENTRANCE(new RectangleArea(2273, 4680, 6, 3, 0)),
    CENTER(new RectangleArea(2264, 4694, 11, 8, 0)),
    SOUTH_WEST(new RectangleArea(2255, 4680, 1, 5, 0)),
    SOUTH_EAST(new RectangleArea(2287, 4680, 1, 6, 0));

    public RectangleArea getArea() {
        return area;
    }

    private final RectangleArea area;

    AFKPosition(RectangleArea area) {
        this.area = area;
    }
}
