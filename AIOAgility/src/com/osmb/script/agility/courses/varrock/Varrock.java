package com.osmb.script.agility.courses.varrock;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Varrock implements Course {

    private static final Area START_AREA = new RectangleArea(3221, 3413, 5, 5, 0);
    private static final Area AREA_1 = new RectangleArea(3213, 3409, 7, 10, 3);
    private static final Area AREA_2 = new RectangleArea(3201, 3413, 7, 7, 3);
    private static final Area AREA_3 = new RectangleArea(3194, 3416, 3, 0, 1);
    private static final Area AREA_4 = new RectangleArea(3192, 3402, 6, 4, 3);
    private static final PolyArea AREA_5 = new PolyArea(new int[][]{{3181, 3382}, {3181, 3399}, {3201, 3399}, {3201, 3404}, {3209, 3404}, {3209, 3395}, {3190, 3382}}, 3);
    private static final Area AREA_6 = new RectangleArea(3218, 3393, 14, 10, 3);
    private static final Area AREA_7 = new RectangleArea(3236, 3403, 4, 5, 3);
    private static final Area AREA_8 = new RectangleArea(3236, 3410, 4, 5, 3);
    private static final Area END_AREA = new RectangleArea(3236, 3417, 4, 2, 0);
    private static final Area BANK_AREA = new RectangleArea(3251, 3420, 5, 2, 0);

    private String area = null;

    @Override
    public int poll(AIOAgility core) {
        WorldPosition pos = core.getWorldPosition();

        if (AREA_1.contains(pos)) {
            area = "AREA_1";
            AIOAgility.handleObstacle(core, "clothes line", "cross", AREA_2, 15000);
            return 0;
        } else if (AREA_2.contains(pos)) {
            area = "AREA_2";
            AIOAgility.handleObstacle(core, "gap", "leap", AREA_3, 15000);
            return 0;
        } else if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core, "wall", "balance", AREA_4, 2, 20000);
            return 0;
        } else if (AREA_4.contains(pos)) {
            area = "AREA_4";
            AIOAgility.handleObstacle(core, "gap", "leap", AREA_5, 15000);
            return 0;
        } else if (AREA_5.contains(pos)) {
            area = "AREA_5";
            AIOAgility.handleObstacle(core, "gap", "leap", AREA_6, 15000);
            return 0;
        } else if (AREA_6.contains(pos)) {
            area = "AREA_6";
            AIOAgility.handleObstacle(core, "gap", "leap", AREA_7, 15000);
            return 0;
        } else if (AREA_7.contains(pos)) {
            area = "AREA_7";
            AIOAgility.handleObstacle(core, "ledge", "hurdle", AREA_8, 15000);
            return 0;
        } else if (AREA_8.contains(pos)) {
            area = "AREA_8";
            AIOAgility.handleObstacle(core, "edge", "jump-off", END_AREA, 15000);
            return 0;
        } else {
            area = "Ground floor";
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "rough wall", "climb", AREA_1, 15000);
            if (handleResponse == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                // Walk to area
                core.getWalker().walkTo(START_AREA.getRandomPosition());
            }
            return 0;
        }
    }

    @Override
    public Area getBankArea() {
        return BANK_AREA;
    }

    @Override
    public int[] regions() {
        return new int[]{12853};
    }

    @Override
    public String name() {
        return "Varrock";
    }

    @Override
    public void onPaint(Canvas c) {
    }
}
