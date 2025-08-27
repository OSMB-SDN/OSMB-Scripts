package com.osmb.script.firemaking.wintertodt.data;

import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;

public enum Brazier {
    SOUTH_EAST("South east", 66, 98, 76, 108, new WorldPosition(1638, 3997, 0), new WorldPosition(1639, 3988, 0), new WorldPosition(1641, 3996, 0), new RectangleArea(1638, 3989, 3, 8, 0)),
    SOUTH_WEST("South west", 10, 98, 0, 108, new WorldPosition(1620, 3997, 0), new WorldPosition(1620, 3988, 0), new WorldPosition(1619, 3996, 0), new RectangleArea(1619, 3989, 3, 7, 0)),
    NORTH_EAST("North east", 66, 42, 76, 32, new WorldPosition(1638, 4015, 0), new WorldPosition(1638, 4025, 0), new WorldPosition(1641, 4018, 0), new RectangleArea(1638, 4018, 3, 7, 0)),
    NORTH_WEST("North west", 10, 42, 0, 32, new WorldPosition(1620, 4015, 0), new WorldPosition(1621, 4025, 0), new WorldPosition(1619, 4018, 0), new RectangleArea(1619, 4018, 3, 7, 0));


    private final int statusIconX;
    private final int statusIconY;
    private final int incapacitatedX;
    private final int incapacitatedY;
    private final WorldPosition brazierPosition;
    private final WorldPosition rootsPosition;
    private final WorldPosition pyromancerPosition;
    private final RectangleArea area;
    private final String name;

    Brazier(String name, int statusIconX, int statusIconY, int incapacitatedX, int incapacitatedY, WorldPosition brazierPosition, WorldPosition rootsPosition, WorldPosition pyromancerPosition, RectangleArea area) {
        this.name = name;
        this.statusIconX = statusIconX;
        this.statusIconY = statusIconY;
        this.incapacitatedX = incapacitatedX;
        this.incapacitatedY = incapacitatedY;
        this.brazierPosition = brazierPosition;
        this.rootsPosition = rootsPosition;
        this.pyromancerPosition = pyromancerPosition;
        this.area = area;
    }

    public RectangleArea getArea() {
        return area;
    }

    public String getName() {
        return name;
    }

    public WorldPosition getPyromancerPosition() {
        return pyromancerPosition;
    }

    public int getStatusIconX() {
        return statusIconX;
    }

    public int getStatusIconY() {
        return statusIconY;
    }

    public WorldPosition getRootsPosition() {
        return rootsPosition;
    }

    public WorldPosition getBrazierPosition() {
        return brazierPosition;
    }

    public int getIncapacitatedY() {
        return incapacitatedY;
    }

    public int getIncapacitatedX() {
        return incapacitatedX;
    }

    @Override
    public String toString() {
        return name;
    }
}

