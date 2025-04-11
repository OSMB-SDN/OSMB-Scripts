package com.osmb.script.agility.courses.draynor;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;
import javafx.scene.canvas.GraphicsContext;


public class Draynor implements Course {

    private static final RectangleArea START_AREA = new RectangleArea(3104, 3279, 5, 4, 0);


    private static final Area AREA_1 = new RectangleArea(3097, 3277, 5, 4, 3);
    private static final Area AREA_2 = new RectangleArea(3088, 3273, 3, 4, 3);
    private static final Area AREA_3 = new RectangleArea(3089, 3265, 5, 3, 3);
    private static final Area AREA_4 = new RectangleArea(3087, 3257, 2, 4, 3);
    private static final Area AREA_5 = new RectangleArea(3087, 3255, 8, 0, 3);
    private static final Area AREA_6 = new RectangleArea(3096, 3256, 5, 5, 3);
    private static final Area BANK_AREA = new RectangleArea(3092, 3242, 2, 3, 0);

    private static final WorldPosition TIGHTROPE_END_POS = new WorldPosition(3090, 3276, 3);
    private static final WorldPosition TIGHTROPE_2_END_POS = new WorldPosition(3092, 3266, 3);
    private static final WorldPosition COURSE_END_POS = new WorldPosition(3103, 3261, 0);


    @Override
    public int poll(AIOAgility core) {
        WorldPosition position = core.getWorldPosition();

        if (AREA_1.contains(position)) {
            AIOAgility.handleObstacle(core, "tightrope", "cross", TIGHTROPE_END_POS, 15000);
        } else if (AREA_2.contains(position)) {
            AIOAgility.handleObstacle(core, "tightrope", "cross", TIGHTROPE_2_END_POS, 15000);
            return 0;
        } else if (AREA_3.contains(position)) {
            AIOAgility.handleObstacle(core, "narrow wall", "balance", AREA_4, 15000);
            return 0;
        } else if (AREA_4.contains(position)) {
            AIOAgility.handleObstacle(core, "wall", "jump-up", AREA_5, 15000);
            return 0;
        } else if (AREA_5.contains(position)) {
            AIOAgility.handleObstacle(core, "gap", "jump", AREA_6, 15000);
            return 0;
        } else if (AREA_6.contains(position)) {
            AIOAgility.handleObstacle(core, "crate", "climb-down", COURSE_END_POS, 15000);
            return 0;
        } else {
            // ground floor
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "rough wall", "climb", AREA_1, 15000);
            if (handleResponse == ObstacleHandleResponse.OBJECT_NOT_IN_SCENE) {
                // Walk to area
                core.getWalker().walkTo(START_AREA.getRandomPosition());
            }
            return 0;
        }
        return 0;
    }

    @Override
    public Area getBankArea() {
        return BANK_AREA;
    }


    @Override
    public int[] regions() {
        return new int[]{12338, 12339};
    }

    @Override
    public String name() {
        return "Draynor";
    }

    @Override
    public void onPaint(Canvas c) {
        // gc.setStroke(Color.BLUE);
        //gc.strokeText("Current area: " + currentArea, 10, 80);
    }
}
