package com.osmb.script;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.scene.RSObject;

public enum Altar {
    BLOOD(new RectangleArea(1, 1, 1, 1, 0)),
    EARTH(new RectangleArea(1, 1, 1, 1, 0)),
    AIR(new RectangleArea(1, 1, 1, 1, 0)),
    WATER(new RectangleArea(1, 1, 1, 1, 0));

    private final Area area;

    Altar(Area area) {
        this.area = area;
    }

    public static RSObject getAltar(ScriptCore core) {
        // query altar object using object manager & return the object

        // Could also have a name variable in the enum for each altar name
        return core.getObjectManager().getClosestObject("Altar");
    }

    public Area getArea() {
        return area;
    }
}
