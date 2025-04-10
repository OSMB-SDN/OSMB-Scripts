package com.osmb.script.agility.courses.ardougne;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;

public class Ardougne implements Course {
    public static final Area AREA_3 = new RectangleArea(2653, 3317, 3, 4, 3);
    private static final Area START_AREA = new RectangleArea(2672, 3297, 2, 1, 0);
    private static final Area AREA_1 = new RectangleArea(2671, 3299, 3, 12, 3);
    private static final Area AREA_2 = new RectangleArea(2661, 3318, 4, 4, 3);
    private static final Area AREA_4 = new RectangleArea(2651, 3310, 2, 4, 3);
    private static final Area AREA_5 = new RectangleArea(2649, 3300, 5, 9, 3);
    private static final Area AREA_6 = new RectangleArea(2656, 3294, 2, 3, 3);
    private static final Area END_AREA = new RectangleArea(2668, 3296, 1, 2, 0);
    private static final Area BANK_AREA = new RectangleArea(2652, 3282, 3, 4, 0);

    private static final WorldPosition FIRST_GAP_POSITION = new WorldPosition(2670, 3310, 3);
    private static final WorldPosition LAST_GAP_POSITION = new WorldPosition(2656, 3296, 3);
    private boolean plankMOG = false;

    @Override
    public int poll(AIOAgility core) {
        WorldPosition pos = core.getWorldPosition();
        if (AREA_1.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Jump", AREA_2, 1, false, 15000, FIRST_GAP_POSITION);
            return 0;
        } else if (AREA_2.contains(pos)) {
            UIResultList<WorldPosition> groundItems = core.getWidgetManager().getMinimap().getItemPositions();
            plankMOG = groundItems.isFound();
            ObstacleHandleResponse obstacleHandleResponse = AIOAgility.handleObstacle(core, "Plank", "Walk-on", AREA_3, 15000);
            if (obstacleHandleResponse == ObstacleHandleResponse.SUCCESS && plankMOG) {
                core.log(Ardougne.class, "Waiting until item is visible...");
                core.submitTask(() -> {
                    UIResultList<WorldPosition> gItems = core.getWidgetManager().getMinimap().getItemPositions();
                    return gItems.isFound() && gItems.size() > 0;
                }, 4000);
                plankMOG = false;
            }
            return 0;
        } else if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Jump", AREA_4, 15000);
            return 0;
        } else if (AREA_4.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Jump", AREA_5, 15000);
            return 0;
        } else if (AREA_5.contains(pos)) {
            AIOAgility.handleObstacle(core, "Steep roof", "Balance-across", AREA_6, 15000);
            return 0;
        } else if (AREA_6.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Jump", END_AREA, 1, false, 18000, LAST_GAP_POSITION);
            return 0;
        } else {
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "Wooden Beams", "Climb-up", AREA_1, 15000);
            if (handleResponse == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                // Walk to area
                core.getWalker().walkTo(START_AREA.getRandomPosition());
            }
        }
        return 0;
    }

    @Override
    public Area getBankArea() {
        return BANK_AREA;
    }

    @Override
    public int[] regions() {
        return new int[]{10547};
    }

    @Override
    public String name() {
        return "Ardougne";
    }

    @Override
    public void onPaint(Canvas gc) {

    }
}
