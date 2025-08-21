package com.osmb.script.agility.courses.gnome;

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

import java.util.List;
import java.util.Optional;

public class GnomeStronghold implements Course {

    private static final RectangleArea START_AREA = new RectangleArea(2473, 3436, 3, 2, 0);

    private static final RectangleArea OBSTACLE_NET_1_AREA = new RectangleArea(2470, 3426, 8, 3, 0);
    private static final RectangleArea OBSTACLE_NET_2_AREA = new RectangleArea(2481, 3417, 9, 8, 0);
    private static final RectangleArea TUNNEL_AREA = new RectangleArea(2481, 3427, 8, 4, 0);
    private static final RectangleArea TUNNEL_END_AREA = new RectangleArea(2481, 3437, 8, 3, 0);

    private static final RectangleArea TREE_BRANCH_AREA = new RectangleArea(2471, 3422, 5, 2, 1);

    private static final RectangleArea TOP_FLOOR_1_AREA = new RectangleArea(2472, 3418, 5, 3, 2);
    private static final RectangleArea TOP_FLOOR_2_AREA = new RectangleArea(2483, 3418, 5, 4, 2);


    private static final WorldPosition BRANCH_END_POS = new WorldPosition(2487, 3420, 0);

    //Maybe do this a better way
    private static final WorldPosition[] FIRST_NET_POSITIONS = new WorldPosition[]{new WorldPosition(2471, 3425, 0), new WorldPosition(2473, 3425, 0), new WorldPosition(2475, 3425, 0)};
    private static final WorldPosition[] SECOND_NET_POSITIONS = new WorldPosition[]{new WorldPosition(2483, 3426, 0), new WorldPosition(2485, 3426, 0), new WorldPosition(2487, 3426, 0)};

    @Override
    public int poll(AIOAgility core) {
        WorldPosition position = core.getWorldPosition();
        if (OBSTACLE_NET_1_AREA.contains(position)) {
            // Get a random net out of the 3
            List<RSObject> obstacleNets = core.getObjectManager().getObjects(gameObject -> gameObject.getName() != null && gameObject.getName().equalsIgnoreCase("obstacle net"));
            WorldPosition netToInteract = FIRST_NET_POSITIONS[core.random(FIRST_NET_POSITIONS.length)];
            Optional<RSObject> matchingNet = obstacleNets.stream().filter(net -> net.getWorldPosition().equals(netToInteract)).findFirst();

            if (!matchingNet.isPresent()) {
                // Shouldn't ever happen unless the scene is bugged... which should never hapen
                core.log(getClass().getSimpleName(), "Can't find net matching location");
                return 0;
            }
            core.log(getClass().getSimpleName(), "Interacting with Obstacle net");
            RSObject net = matchingNet.get();
            if (net.interact("climb-over")) {
                core.submitHumanTask(() -> core.getWorldPosition().getPlane() == 1, 10000);
                return 0;
            }
        } else if (OBSTACLE_NET_2_AREA.contains(position)) {
            // Get a random net out of the 3
            List<RSObject> obstacleNets = core.getObjectManager().getObjects(gameObject -> gameObject.getName() != null && gameObject.getName().equalsIgnoreCase("obstacle net"));
            WorldPosition netToInteract = SECOND_NET_POSITIONS[core.random(SECOND_NET_POSITIONS.length)];
            Optional<RSObject> matchingNet = obstacleNets.stream().filter(net -> net.getWorldPosition().equals(netToInteract)).findFirst();

            if (!matchingNet.isPresent()) {
                // Shouldn't ever happen unless the scene is bugged
                core.log(getClass().getSimpleName(), "Can't find net matching location");
                return 0;
            }
            core.log(getClass().getSimpleName(), "Interacting with Obstacle net");
            RSObject net = matchingNet.get();
            if (net.interact("climb-over")) {
                core.submitHumanTask(() -> TUNNEL_AREA.contains(core.getWorldPosition()), 10000);
                return 0;
            }
        } else if (TUNNEL_AREA.contains(position)) {
            AIOAgility.handleObstacle(core, "obstacle pipe", "squeeze-through", TUNNEL_END_AREA, 15000);
            return 0;
        } else if (TREE_BRANCH_AREA.contains(position)) {
            AIOAgility.handleObstacle(core, "tree branch", "climb", TOP_FLOOR_1_AREA, 10000);
            return 0;
        } else if (TOP_FLOOR_1_AREA.contains(position)) {
            AIOAgility.handleObstacle(core, "balancing rope", "walk-on", TOP_FLOOR_2_AREA, 15000);
        } else if (TOP_FLOOR_2_AREA.contains(position)) {
            AIOAgility.handleObstacle(core, "tree branch", "climb-down", BRANCH_END_POS, 15000);
            return 0;
        } else {
            ObstacleHandleResponse handleResponse = AIOAgility.handleObstacle(core, "log balance", "walk-across", OBSTACLE_NET_1_AREA, 15000);
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
        return null;
    }

    @Override
    public int[] regions() {
        return new int[]{9781};
    }

    @Override
    public String name() {
        return "Gnome stronghold";
    }

    @Override
    public void onPaint(Canvas c) {

    }

}
