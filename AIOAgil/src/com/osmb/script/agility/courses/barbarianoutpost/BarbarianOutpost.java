package com.osmb.script.agility.courses.barbarianoutpost;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;

public class BarbarianOutpost implements Course {
    private static final RectangleArea AREA_1 = new RectangleArea(2543, 3550, 10, 6, 0);
    private static final RectangleArea AREA_2 = new RectangleArea(2549, 3542, 4, 7, 0);
    private static final RectangleArea AREA_3 = new RectangleArea(2533, 3542, 9, 5, 0);
    private static final RectangleArea AREA_4 = new RectangleArea(2536, 3545, 2, 2, 1);

    private static final RectangleArea AREA_5 = new RectangleArea(2531, 3545, 1, 2, 1);
    private static final RectangleArea AREA_6 = new RectangleArea(2528, 3543, 4, 13, 0);
    private static final RectangleArea AREA_7 = new RectangleArea(2529, 3548, 7, 8, 0);
    private static final RectangleArea AREA_8 = new RectangleArea(2537, 3552, 2, 2, 0);
    private static final RectangleArea AREA_9 = new RectangleArea(2540, 3552, 2, 2, 0);
    private static final RectangleArea UNDERGROUND_AREA =new RectangleArea(2545, 9948, 11, 8, 0);

    @Override
    public int poll(AIOAgility core) {
        WorldPosition pos = core.getWorldPosition();
        if(UNDERGROUND_AREA.contains(pos)) {
            AIOAgility.handleObstacle(core, "Ladder", "Climb-up", AREA_1, 15000);
            return 0;
        }
        if (AREA_1.contains(pos)) {
            AIOAgility.handleObstacle(core, "Ropeswing", "Swing-on", AREA_2, 15000);
            return 0;
        }
        if (AREA_2.contains(pos)) {
            AIOAgility.handleObstacle(core, "Log balance", "Walk-across", AREA_3, 15000);
            return 0;
        }
        if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core, "Obstacle net", "Climb-over", AREA_4, 15000);
            return 0;
        }
        if(AREA_4.contains(pos)) {
            AIOAgility.handleObstacle(core, "Balancing ledge", "Walk-across", AREA_5, 15000);
            return 0;
        }
        if(AREA_5.contains(pos)) {
            AIOAgility.handleObstacle(core, "Ladder", "Climb-down", AREA_6, 15000);
            return 0;
        }
        if(AREA_6.contains(pos) || AREA_7.contains(pos)) {
            AIOAgility.handleObstacle(core, "Crumbling wall", "Climb-over", AREA_8, 15000);
            return 0;
        }
        if(AREA_8.contains(pos)) {
            AIOAgility.handleObstacle(core, "Crumbling wall", "Climb-over", AREA_9, 15000);
            return 0;
        }
        if(AREA_9.contains(pos)) {
            AIOAgility.handleObstacle(core, "Crumbling wall", "Climb-over", AREA_1, 15000);
            return 0;
        }
        return 0;
    }

    @Override
    public Area getBankArea() {
        return null;
    }

    @Override
    public int[] regions() {
        return new int[] {10139, 10039};
    }

    @Override
    public String name() {
        return "Barbarian outpost";
    }

    @Override
    public void onPaint(Canvas gc) {

    }
}
