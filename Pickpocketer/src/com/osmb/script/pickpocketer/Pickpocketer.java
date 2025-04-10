package com.osmb.script.pickpocketer;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@ScriptDefinition(name = "AIO Power pickpocketer", author = "Joe", version = 1.0, description = "Pickpockets any highlighted NPC & drops the items.", skillCategory = SkillCategory.THIEVING)
public class Pickpocketer extends Script {

    private static final ToleranceComparator TOLERANCE_COMPARATOR = new SingleThresholdComparator(3);
    private static final ToleranceComparator TOLERANCE_COMPARATOR_2 = new SingleThresholdComparator(5);
    private static final WorldPosition EDGE_LADDER_NORTH_TILE = new WorldPosition(3096, 3512, 0);
    private final Stopwatch eatBlockTimer = new Stopwatch();
    private SearchablePixel highlightColor = new SearchablePixel(-55297, TOLERANCE_COMPARATOR, ColorModel.RGB);
    private SearchablePixel highlightColor2 = new SearchablePixel(-2237670, TOLERANCE_COMPARATOR_2, ColorModel.RGB);
    private int nextOpenAmount;
    private int foodItemID = ItemID.LOBSTER;
    private int hitpointsToEat = -1;
    private int eatHigh = 6;
    private int eatLow = 4;
    private static final Area EDGEVILLE_UPSTAIRS = new RectangleArea(3091, 3507, 9, 6, 1);
    private WorldPosition previousPosition = null;

    public Pickpocketer(Object scriptCore) {
        super(scriptCore);
    }

    public static void main(String[] args) {
        int[] colors = new int[]{-2237413, -2171620, -2237670};
        for (int color : colors) {
            Color c = new Color(color);
            Color c1 = new Color(221, 219, 26);
            System.out.println(c1.getRGB());
            System.out.println(c.getRed() + "," + c.getGreen() + "," + c.getBlue());
        }
    }

    @Override
    public void onStart() {
        refreshOpenAmount();
        hitpointsToEat = random(eatLow, eatHigh);
    }

    private void refreshOpenAmount() {
        nextOpenAmount = random(10, 28);
    }

    private boolean handleEating() {
        UIResult<Integer> hpOpt = getWidgetManager().getMinimapOrbs().getHitpoints();
        if (!hpOpt.isFound()) {
            log(getClass().getSimpleName(), "Hitpoints orb not visible...");
            return false;
        }
        UIResultList<ItemSearchResult> food = getItemManager().findAllOfItem(getWidgetManager().getInventory(), foodItemID);
        if (food.isFound()) {
            int hitpoints = hpOpt.get();
            if (hitpoints <= hitpointsToEat && eatBlockTimer.hasFinished()) {
                // eat food
                ItemSearchResult foodToEat = food.getRandom();
                foodToEat.interact();
                sleep(random(300, 1600));
                eatBlockTimer.reset(3000);
                hitpointsToEat = random(eatLow, eatHigh);
                return true;
            }
            return false;
        }
        log("No food found");
        return false;
    }

    @Override
    public int poll() {
        // make sure inventory is open
        if (!getWidgetManager().getInventory().open()) {
            log(getClass().getSimpleName(), "Inventory not open.");
            return 0;
        }

        if (foodItemID != -1 && handleEating()) {
            log("Handling eating...");
            return 0;
        }
        WorldPosition myPosition = getWorldPosition();

        if (EDGEVILLE_UPSTAIRS.contains(myPosition)) {
            RSObject object = getObjectManager().getClosestObject("Ladder");
            if (object == null) {
                return 0;
            }
            if (object.interact("Climb-down")) {
                submitTask(() -> {
                    WorldPosition position = getWorldPosition();
                    if (position == null) {
                        return false;
                    }
                    return position.getPlane() == 0;
                }, 4000);
            }
            return 0;
        }
        UIResult<ItemSearchResult> coinPouch = getItemManager().findItem(getWidgetManager().getInventory(), ItemID.COIN_POUCH);
        if (coinPouch.isNotVisible()) {
            log(getClass().getSimpleName(), "Coin pouch not visible.");
            return 0;
        }

        int amountOfCoinPouches = coinPouch.isNotFound() ? 0 : coinPouch.get().getStackAmount();
        if (amountOfCoinPouches > nextOpenAmount || amountOfCoinPouches >= 28) {
            // tap coin pouches
            coinPouch.get().interact();
            refreshOpenAmount();
            // sleep
            submitTask(() -> false, random(600, 2000));
        }
        if (previousPosition != null) {
            LocalPosition previousLocalPosition = previousPosition.toLocalPosition(this);
            if (previousLocalPosition == null) {
                return 0;
            }
            Polygon poly = getSceneProjector().getTileCube(previousLocalPosition.getX(), previousLocalPosition.getY(), previousLocalPosition.getPlane(), 150, previousLocalPosition.getRemainderX(), previousLocalPosition.getRemainderY());
            Rectangle highlightBounds = getUtils().getHighlightBounds(poly, highlightColor, highlightColor2);
            if (highlightBounds != null) {
                getScreen().getDrawableCanvas().drawRect(highlightBounds, Color.RED.getRGB());
                if (getFinger().tap(highlightBounds)) {
                    submitTask(() -> false, random(250, 1500));
                }
            } else {
                previousPosition = null;
                return 0;
            }
        }
        Map<WorldPosition, Polygon> validPositions = getValidNPCPositions();
        if (validPositions == null || validPositions.isEmpty()) {
            log(getClass().getSimpleName(), "No valid positions...");
            return 0;
        }

        WorldPosition closestPosition = (WorldPosition) Utils.getClosestPosition(myPosition, validPositions.keySet().toArray(new WorldPosition[0]));
        previousPosition = closestPosition;
        Polygon closestPoly = validPositions.get(closestPosition);


        Rectangle highlightBounds = getUtils().getHighlightBounds(closestPoly, highlightColor, highlightColor2);
        if (highlightBounds == null) {
            return 0;
        }
        getScreen().getDrawableCanvas().drawRect(highlightBounds, Color.RED.getRGB());
        if (getFinger().tap(highlightBounds)) {
            submitTask(() -> false, random(250, 1500));
//            UIResult<ItemSearchResult> coinPouch_ = getItemManager().findItem(getWidgetManager().getInventory(), ItemID.COIN_POUCH);
//            if (!coinPouch.isFound()) {
//                return 0;
//            }
//            // wait until increment
//            if(coinPouch_.get().getStackAmount() > amountOfCoinPouches) {
//                return true;
//            }
        }
        return 0;
    }

    private Map<WorldPosition, Polygon> getValidNPCPositions() {
        UIResultList<WorldPosition> npcPositions = getWidgetManager().getMinimap().getNPCPositions();
        if (npcPositions.isNotVisible()) {
            log(getClass().getSimpleName(),"Not visible!");
            return null;
        }
        if (npcPositions.isNotFound()) {
            log(getClass().getSimpleName(), "No NPC's found nearby...");
            return null;
        }
        log(getClass().getSimpleName(),"NPC's on minimap: "+npcPositions.size());
        Map<WorldPosition, Polygon> validPositions = new HashMap<>();
        npcPositions.forEach(position -> {
            if (position.equals(EDGE_LADDER_NORTH_TILE)) {
                return;
            }
            // convert to local
            LocalPosition localPosition = position.toLocalPosition(this);
            // get poly for position
            Polygon poly = getSceneProjector().getTileCube(localPosition.getX(), localPosition.getY(), localPosition.getPlane(), 150, localPosition.getRemainderX(), localPosition.getRemainderY());
            if (poly == null) {
                return;
            }
            Polygon cubeResized = poly.getResized(1.3).convexHull();
            if (cubeResized == null) {
                return;
            }
            RSTile tile = getSceneManager().getTile(localPosition);
            if (!tile.canReach()) {
                return;
            }
            if (getPixelAnalyzer().findPixel(cubeResized, highlightColor) != null) {
                validPositions.put(position, cubeResized);
                getScreen().getDrawableCanvas().drawPolygon(cubeResized.getXPoints(), cubeResized.getYPoints(), cubeResized.numVertices(), Color.GREEN.getRGB(), 1);
            }
        });
        return validPositions;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12342, 10548};
    }
}
