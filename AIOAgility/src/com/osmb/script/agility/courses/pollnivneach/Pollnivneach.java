package com.osmb.script.agility.courses.pollnivneach;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;

import java.util.Optional;

public class Pollnivneach implements Course {
    private static final Area START_AREA = new RectangleArea(3350, 2961, 2, 1, 0);
    private static final Area AREA_1 = new RectangleArea(3346, 2964, 5, 4, 1);
    private static final Area AREA_2 = new RectangleArea(3352, 2973, 3, 3, 1);
    private static final Area AREA_3 = new RectangleArea(3360, 2977, 2, 2, 1);
    private static final Area AREA_4 = new RectangleArea(3366, 2974, 4, 2, 1);
    private static final Area AREA_5 = new RectangleArea(3365, 2982, 4, 4, 1);
    public static final Area AREA_6 = new RectangleArea(3355, 2980, 10, 5, 2);
    private static final Area AREA_7 = new RectangleArea(3357, 2990, 13, 5, 2);
    private static final Area AREA_8 = new RectangleArea(3356, 3000, 6, 4, 2);
    private static final Area END_AREA = new RectangleArea(3362, 2997, 2, 2, 0);

    @Override
    public int poll(AIOAgility core) {
        WorldPosition pos = core.getWorldPosition();

        if (AREA_1.contains(pos)) {
            AIOAgility.handleObstacle(core, "Market stall", "Jump-on", AREA_2, 2,false,15000);
            return 0;
        } else if (AREA_2.contains(pos)) {
            AIOAgility.handleObstacle(core, "Banner", "Grab", AREA_3, 2,false,15000);
            return 0;
        } else if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Leap", AREA_4, 2,false,15000);
            return 0;
        } else if (AREA_4.contains(pos)) {
            AIOAgility.handleObstacle(core, "Tree", "Jump-to", AREA_5, 2,false,15000);
            return 0;
        } else if (AREA_5.contains(pos)) {
            AIOAgility.handleObstacle(core, "Rough wall", "Climb", AREA_6, 15000);
            return 0;
        } else if (AREA_6.contains(pos)) {
            AIOAgility.handleObstacle(core, "Monkeybars", "Cross", AREA_7, 20000);
            return 0;
        } else if (AREA_7.contains(pos)) {
            AIOAgility.handleObstacle(core, "Tree", "Jump-on", AREA_8, 2,false,20000);
            return 0;
        } else if (AREA_8.contains(pos)) {
            AIOAgility.handleObstacle(core, "Drying line", "Jump-to", END_AREA, 2, false, 20000);
            return 0;
        } else {
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "Basket", "Climb-on", AREA_1, 15000);
            if (handleResponse == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                // Walk to area
                core.getWalker().walkTo(START_AREA.getRandomPosition());
            }
        }
        return 0;
    }

    @Override
    public Area getBankArea() {
        return null;
    }


    @Override
    public int[] regions() {
        return new int[]{13358};
    }

    @Override
    public String name() {
        return "Pollnivneach";
    }

    @Override
    public void onPaint(Canvas gc) {

    }
}
