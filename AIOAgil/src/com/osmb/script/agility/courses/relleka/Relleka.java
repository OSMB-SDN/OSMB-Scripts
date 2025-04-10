package com.osmb.script.agility.courses.relleka;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;

public class Relleka implements Course {

    private static final Area BANK_AREA = new RectangleArea(2721, 3490, 8, 3, 0);
    private static final Area START_AREA = new RectangleArea(2625, 3677, 2, 1, 0);

    private static final Area AREA_1 = new RectangleArea(2622, 3672, 4, 4, 3);
    private static final Area AREA_2 = new RectangleArea(2615, 3658, 7, 10, 3);
    private static final Area AREA_3 = new RectangleArea(2626, 3651, 4, 4, 3);
    private static final Area AREA_4 = new RectangleArea(2639, 3649, 5, 4, 3);
    private static final Area AREA_5 = new RectangleArea(2643, 3656, 7, 6, 3);
    private static final Area AREA_6 = new RectangleArea(2654, 3664, 9, 22, 3);

    private static final Area END_AREA = new RectangleArea(2651, 3675, 2, 2, 0);

    @Override
    public int poll(AIOAgility core) {
        WorldPosition pos = core.getWorldPosition();

        if (AREA_1.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Leap", AREA_2, 15000);
            return 0;
        } else if (AREA_2.contains(pos)) {
            AIOAgility.handleObstacle(core, "Tightrope", "Cross", AREA_3, 15000);
            return 0;
        } else if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Leap", AREA_4, 15000);
            return 0;
        } else if (AREA_4.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Hurdle", AREA_5, 15000);
            return 0;
        } else if (AREA_5.contains(pos)) {
            AIOAgility.handleObstacle(core, "Tightrope", "Cross", AREA_6, 15000);
            return 0;
        } else if (AREA_6.contains(pos)) {
            AIOAgility.handleObstacle(core, "Pile of fish", "Jump-in", END_AREA, 15000);
            return 0;
        } else {
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "Rough wall", "Climb", AREA_1, 15000);
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
        return new int[]{10553};
    }

    @Override
    public String name() {
        return "Relleka";
    }

    @Override
    public void onPaint(Canvas gc) {

    }
}
