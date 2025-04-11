package com.osmb.script.agility.courses.seers;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;

public class Seers implements Course {

    private static final RectangleArea START_AREA = new RectangleArea(2728, 3485, 2, 4, 0);
    private static final RectangleArea AREA_1 = new RectangleArea(2721, 3490, 9, 7, 3);
    private static final RectangleArea AREA_2 = new RectangleArea(2704, 3487, 10, 11, 2);
    private static final RectangleArea AREA_3 = new RectangleArea(2709, 3476, 7, 6, 2);
    private static final RectangleArea AREA_4 = new RectangleArea(2699, 3469, 17, 7, 3);
    private static final RectangleArea AREA_5 = new RectangleArea(2689, 3459, 14, 7, 2);
    private static final RectangleArea END_AREA = new RectangleArea(2703, 3460, 2, 6, 0);
    private static final Area BANK_AREA = new RectangleArea(2721, 3490, 8, 3, 0);

    @Override
    public int poll(AIOAgility core) {
        WorldPosition pos = core.getWorldPosition();
        if (AREA_1.contains(pos)) {
            core.log(getClass().getSimpleName(), "Area 1");
            AIOAgility.handleObstacle(core, "Gap", "Jump", AREA_2, 15000);
            return 0;
        } else if (AREA_2.contains(pos)) {
            AIOAgility.handleObstacle(core, "Tightrope", "Cross", AREA_3, 15000);
            return 0;
        } else if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Jump", AREA_4, 15000);
            return 0;
        } else if (AREA_4.contains(pos)) {
            AIOAgility.handleObstacle(core, "Gap", "Jump", AREA_5, 15000);
            return 0;
        } else if (AREA_5.contains(pos)) {
            AIOAgility.handleObstacle(core, "Edge", "Jump", END_AREA, 15000);
            return 0;
        } else {
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "Wall", "Climb-up", AREA_1, 15000);
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
        return new int[]{10806};
    }

    @Override
    public String name() {
        return "Seers";
    }

    @Override
    public void onPaint(Canvas gc) {

    }
}
