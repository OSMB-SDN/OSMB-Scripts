import com.osmb.api.input.MenuEntry;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.UIResultList;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlayGroundScript extends Script {

    public PlayGroundScript(Object scriptCore) {
        super(scriptCore);
    }
    private static final WorldPosition[] LOG_LOCATIONS = {
            new WorldPosition(3205, 3226, 2), // NW LOGS
            new WorldPosition(3208, 3225, 2), // NE LOGS
            new WorldPosition(3209, 3224, 2), // SE LOGS
            new WorldPosition(3205, 3224, 2)  // SW LOGS
    };
    private static final RectangleArea LOG_AREA = new RectangleArea(3205,3223,7,4, 2);


    private List<WorldPosition> checkedPositions = new ArrayList<>();
    @Override
    public int poll() {
        WorldPosition myPosition = getWorldPosition();
        if(myPosition == null) {
            return 0;
        }
        // check if we aren't in the log area
        if(!LOG_AREA.contains(getWorldPosition())) {
            // walk to the log area
            getWalker().walkTo(LOG_AREA.getRandomPosition());
            return 0;
        }
        // get the positions of the ground items around us
        UIResultList<WorldPosition> groundItems = getWidgetManager().getMinimap().getItemPositions();
        if(groundItems.isNotVisible()) {
            // minimap not visible for some reason
            return 0;
        }
        // if no ground items around us, hop worlds & also clear our list of checked positions
        if(groundItems.isEmpty()) {
            forceHop();
            checkedPositions.clear();
        }
        // get a random
        WorldPosition groundItemPosition = getUncheckedPosition();

        Polygon tilePoly = getSceneProjector().getTilePoly(groundItemPosition);
        if(tilePoly == null) {
            // position is off screen or not inside our scene
            return 0;
        }
        // if the tile poly is valid, resize it and interact to check if we can light some logs
        boolean interacted = getFinger().tap(tilePoly.getResized(0.6), menuEntries -> {
           for(MenuEntry menuEntry : menuEntries) {
               if(menuEntry.getRawText().equalsIgnoreCase("Light logs")) {
                   // entry is found interact
                   return menuEntry;
               }
           }
           // our desired option isn't found.. add to our checked positions
            checkedPositions.add(groundItemPosition);
           return null;
        });

        if(interacted) {
            // wait for the player to light the logs somehow & add checkedPositions.add(groundItemPosition); on succession
            submitHumanTask(() -> {

                return false;
            }, 5000);
        }
        return 0;
    }

    /**
     * @return Returns a random unchecked position, if all positions are checked return null.
     */
    private WorldPosition getUncheckedPosition() {
        List<WorldPosition> positions = new ArrayList<>(Arrays.asList(LOG_LOCATIONS));
        Collections.shuffle(positions);
        for(WorldPosition position : positions) {
            if(!checkedPositions.contains(position)) {
                return position;
            }
        }
        return null;
    }
}
