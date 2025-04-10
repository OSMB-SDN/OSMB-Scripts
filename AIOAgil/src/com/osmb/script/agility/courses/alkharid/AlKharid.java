package com.osmb.script.agility.courses.alkharid;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;
import javafx.scene.canvas.GraphicsContext;

public class AlKharid implements Course {

    private static final Area START_AREA = new RectangleArea(3270, 3196, 3, 5, 0);
    private static final Area AREA_1 = new RectangleArea(3270, 3180, 10, 12, 3);
    private static final Area AREA_2 = new RectangleArea(3263, 3161, 9, 12, 3);
    private static final Area AREA_3 = new RectangleArea(3282, 3159, 20, 17, 3);
    private static final Area AREA_4 = new RectangleArea(3312, 3159, 7, 7, 1);
    private static final Area AREA_5 = new RectangleArea(3313, 3174, 5, 5, 2);
    private static final Area AREA_6 = new RectangleArea(3311, 3179, 7, 7, 3);
    private static final Area AREA_7 = new RectangleArea(3295, 3184, 11, 10, 3);
    private static final Area BANK_AREA = new RectangleArea(3269, 3164, 3, 6, 0);

    private static final WorldPosition TIGHTROPE_END_POS = new WorldPosition(3272, 3172, 3);
    private static final WorldPosition CABLE_END_POS = new WorldPosition(3284, 3166, 3);
    private static final WorldPosition ZIP_LINE_END_POS = new WorldPosition(3315, 3163, 1);
    private static final WorldPosition TROPICAL_TREE_END_POS = new WorldPosition(3317, 3174, 2);
    private static final WorldPosition BEAMS_END_POS = new WorldPosition(3316, 3180, 3);
    private static final WorldPosition TIGHTROPE_2_END_POS = new WorldPosition(3302, 3187, 3);
    private static final WorldPosition COURSE_END_POS = new WorldPosition(3299, 3194, 0);

    @Override
    public int poll(AIOAgility core) {
        WorldPosition pos = core.getWorldPosition();

        if (AREA_1.contains(pos)) {
            AIOAgility.handleObstacle(core, "tightrope", "cross", TIGHTROPE_END_POS, 15000);
            return 0;
        } else if (AREA_2.contains(pos)) {
            AIOAgility.handleObstacle(core, "cable", "swing-across", CABLE_END_POS, 15000);
            return 0;
        } else if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core, "zip line", "teeth-grip", ZIP_LINE_END_POS, 15000);
            return 0;
        } else if (AREA_4.contains(pos)) {
            AIOAgility.handleObstacle(core, "tropical tree", "swing-across", TROPICAL_TREE_END_POS, 15000);
            return 0;
        } else if (AREA_5.contains(pos)) {
            AIOAgility.handleObstacle(core, "roof top beams", "climb", BEAMS_END_POS, 15000);
            return 0;
        } else if (AREA_6.contains(pos)) {
            AIOAgility.handleObstacle(core, "tightrope", "cross", TIGHTROPE_2_END_POS, 15000);
            return 0;
        } else if (AREA_7.contains(pos)) {
            AIOAgility.handleObstacle(core, "gap", "jump", COURSE_END_POS, 15000);
            return 0;
        } else {
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
        return new int[]{13105};
    }

    @Override
    public String name() {
        return "Al Kharid";
    }

    @Override
    public void onPaint(Canvas c) {

    }
}
