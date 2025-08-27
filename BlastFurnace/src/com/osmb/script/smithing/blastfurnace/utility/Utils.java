package com.osmb.script.smithing.blastfurnace.utility;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.script.smithing.blastfurnace.component.Overlay;
import com.osmb.script.smithing.blastfurnace.data.Bar;

import java.util.HashMap;
import java.util.Map;

import static com.osmb.script.smithing.blastfurnace.Constants.CONVEYOR_BELT_POSITION;


public class Utils {
    public static Map<Bar, Integer> getMeltingPotBars(Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        Map<Bar, Integer> bars = new HashMap<>();
        for (Bar bar : Bar.values()) {
            int barAmount = blastFurnaceInfo.getBarAmount(bar);
            if (barAmount > 0) {
                bars.put(bar, barAmount);
            }
        }
        return bars;
    }

    public static RSObject getConveyorBelt(ScriptCore core) {
        return core.getObjectManager().getRSObject(object -> {
            String name = object.getName();
            if (name == null || !name.equalsIgnoreCase("Conveyor belt")) {
                return false;
            }
            WorldPosition position = object.getWorldPosition();
            return position != null && position.equals(CONVEYOR_BELT_POSITION);
        });
    }

    public static boolean hasBarsToCollect(Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        for (Bar bar : Bar.values()) {
            if (blastFurnaceInfo.getBarAmount(bar) > 0) {
                return true;
            }
        }
        return false;
    }
}
