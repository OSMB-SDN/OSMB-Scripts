package com.osmb.script.agility.courses.canifis;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.AIOAgility;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.ObstacleHandleResponse;
import com.osmb.script.agility.courses.ardougne.Ardougne;

public class Canifis implements Course {
    private static final Area START_AREA = new RectangleArea(3504, 3486, 4, 2, 0);
    private static final Area AREA_1 = new RectangleArea(3505, 3492, 5, 5, 2);
    private static final Area AREA_2 = new RectangleArea(3497, 3504, 5, 2, 2);
    private static final Area AREA_3 = new RectangleArea(3486, 3499, 6, 5, 2);
    private static final Area AREA_4 = new RectangleArea(3475, 3492, 4, 7, 3);
    private static final Area AREA_5 = new RectangleArea(3477, 3481, 7, 6, 2);
    private static final Area AREA_6 = new RectangleArea(3489, 3469, 14, 9, 3);
    private static final Area AREA_7 = new RectangleArea(3509, 3475, 6, 7, 2);
    private static final Area BANK_AREA = new RectangleArea(3508, 3478, 4, 4, 0);
    private static final WorldPosition END_POS = new WorldPosition(3510, 3485, 0);

    @Override
    public int poll(AIOAgility core) {
        WorldPosition pos = core.getWorldPosition();
        if(pos == null) {
            core.log(Ardougne.class, "Position is null...");
            return 0;
        }
        if (AREA_1.contains(pos)) {
            AIOAgility.handleObstacle(core, "gap", "jump", AREA_2, 15000);
            return 0;
        } else if (AREA_2.contains(pos)) {
            AIOAgility.handleObstacle(core, "gap", "jump", AREA_3, 15000);
            return 0;
        } else if (AREA_3.contains(pos)) {
            AIOAgility.handleObstacle(core, "gap", "jump", AREA_4, 2,15000);
            return 0;
        } else if (AREA_4.contains(pos)) {
            AIOAgility.handleObstacle(core, "gap", "jump", AREA_5, 15000);
            return 0;
        } else if (AREA_5.contains(pos)) {
            AIOAgility.handleObstacle(core, "pole-vault", "vault", AREA_6, 15000);
            return 0;
        } else if (AREA_6.contains(pos)) {
            AIOAgility.handleObstacle(core, "gap", "jump", AREA_7, 25000);
            return 0;
        } else if (AREA_7.contains(pos)) {
            AIOAgility.handleObstacle(core, "gap", "jump", END_POS, 15000);
            return 0;
        } else {
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "tall tree", "climb", AREA_1, 15000);
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
        return new int[]{13878};
    }

    @Override
    public String name() {
        return "Canifis";
    }

    @Override
    public void onPaint(Canvas c) {

    }
}
