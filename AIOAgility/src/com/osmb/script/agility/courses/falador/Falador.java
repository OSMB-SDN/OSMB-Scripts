package com.osmb.script.agility.courses.falador;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;

public class Falador implements Course {

    private static final Area START_AREA = new RectangleArea(3035, 3340, 3, 1, 0);
    private static final Area END_AREA = new RectangleArea(3028, 3332, 3, 3, 0);
    private static final Area AREA_1 = new RectangleArea(3036, 3342, 4, 1, 3);
    private static final Area AREA_2 = new RectangleArea(3044, 3341, 7, 8, 3);
    private static final Area AREA_3 = new RectangleArea(3048, 3357, 2, 1, 3);
    private static final Area AREA_4 = new RectangleArea(3045, 3361, 3, 6, 3);
    private static final Area AREA_5 = new RectangleArea(3034, 3361, 7, 3, 3);
    private static final Area AREA_6 = new RectangleArea(3026, 3352, 7, 3, 3);
    private static final Area AREA_7 = new RectangleArea(3009, 3353, 12, 5, 3);
    private static final Area AREA_8 = new RectangleArea(3016, 3343, 6, 6, 3);
    private static final Area AREA_9 = new RectangleArea(3011, 3344, 3, 3, 3);
    private static final Area AREA_10 = new RectangleArea(3009, 3335, 4, 7, 3);
    private static final Area AREA_11 = new RectangleArea(3012, 3331, 5, 3, 3);
    private static final Area AREA_12 = new RectangleArea(3019, 3332, 5, 3, 3);
    private static final Area BANK_AREA = new RectangleArea(3019, 3332, 5, 3, 3);


    @Override
    public int poll(AIOAgility core) {

        WorldPosition pos = core.getWorldPosition();
        if (AREA_1.contains(pos)) {
            AIOAgility.handleObstacle(core,"Tightrope", "Cross", AREA_2, 25000);
        } else if (AREA_2.contains(pos)) {
            AIOAgility.handleObstacle(core,"Hand holds", "Cross", AREA_3, 35000);
        } else if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core,"Gap", "Jump", AREA_4, 20000);
        } else if (AREA_4.contains(pos)) {
            AIOAgility.handleObstacle(core,"Gap", "Jump", AREA_5, 20000);
        } else if (AREA_5.contains(pos)) {
            AIOAgility.handleObstacle(core,"Tightrope", "Cross", AREA_6, 20000);
        } else if (AREA_6.contains(pos)) {
            AIOAgility.handleObstacle(core,"Tightrope", "Cross", AREA_7, 20000);
        } else if (AREA_7.contains(pos)) {
            AIOAgility.handleObstacle(core,"Gap", "Jump", AREA_8, 20000);
        } else if (AREA_8.contains(pos)) {
            AIOAgility.handleObstacle(core,"Ledge", "Jump", AREA_9, 20000);
        } else if (AREA_9.contains(pos)) {
            AIOAgility.handleObstacle(core,"Ledge", "Jump", AREA_10, 20000);
        } else if (AREA_10.contains(pos)) {
            AIOAgility.handleObstacle(core,"Ledge", "Jump", AREA_11, 20000);
        } else if (AREA_11.contains(pos)) {
            AIOAgility.handleObstacle(core,"Ledge", "Jump", AREA_12, 20000);
        } else if (AREA_12.contains(pos)) {
            AIOAgility.handleObstacle(core,"Edge", "Jump", END_AREA, 20000);
        } else {
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "rough wall", "climb", AREA_1, 15000);
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
        return new int[]{12084};
    }

    @Override
    public String name() {
        return "Falador";
    }

    @Override
    public void onPaint(Canvas c) {

    }
}
