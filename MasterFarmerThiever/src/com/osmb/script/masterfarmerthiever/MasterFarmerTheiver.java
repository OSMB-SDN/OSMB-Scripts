package com.osmb.script.masterfarmerthiever;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.CollisionMap;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Line;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.TileEdge;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.walker.pathing.CollisionFlags;
import com.osmb.script.masterfarmerthiever.javafx.ScriptOptions;
import javafx.scene.Scene;
import javafx.util.Pair;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(name = "Master farmer pickpocketer", author = "Joe", version = 1, description = "Pickpockets master farmers north of ardougne", skillCategory = SkillCategory.THIEVING)
public class MasterFarmerTheiver extends Script {
    public static final int[] SEED_IDS = new int[]{ItemID.POTATO_SEED, ItemID.ONION_SEED, ItemID.CABBAGE_SEED, ItemID.TOMATO_SEED, ItemID.SWEETCORN_SEED, ItemID.STRAWBERRY_SEED, ItemID.WATERMELON_SEED, ItemID.SNAPE_GRASS, ItemID.BARLEY_SEED, ItemID.HAMMERSTONE_SEED, ItemID.ASGARNIAN_SEED, ItemID.JUTE_SEED, ItemID.YANILLIAN_SEED, ItemID.KRANDORIAN_SEED, ItemID.WILDBLOOD_SEED, ItemID.MARIGOLD_SEED, ItemID.NASTURTIUM_SEED, ItemID.ROSEMARY_SEED, ItemID.WOAD_SEED, ItemID.LIMPWURT_SEED, ItemID.REDBERRY_SEED, ItemID.CADAVABERRY_SEED, ItemID.DWELLBERRY_SEED, ItemID.JANGERBERRY_SEED, ItemID.WHITEBERRY_SEED, ItemID.POISON_IVY_SEED, ItemID.MUSHROOM_SPORE, ItemID.BELLADONNA_SEED, ItemID.CACTUS_SEED, ItemID.SEAWEED_SPORE, ItemID.POTATO_CACTUS_SEED, ItemID.GUAM_SEED, ItemID.MARRENTILL_SEED, ItemID.TARROMIN_SEED, ItemID.HARRALANDER_SEED, ItemID.RANARR_SEED, ItemID.TOADFLAX_SEED, ItemID.IRIT_SEED, ItemID.AVANTOE_SEED, ItemID.KWUARM_SEED, ItemID.SNAPDRAGON_SEED, ItemID.CADANTINE_SEED, ItemID.LANTADYME_SEED, ItemID.DWARF_WEED_SEED, ItemID.TORSTOL_SEED};

    public static final RectangleArea BANK_AREA = new RectangleArea(2615, 3333, 4, 2, 0);
    public static final RectangleArea FARMER_WANDER_AREA = new RectangleArea(2631, 3359, 10, 10, 0);
    public static final RectangleArea FIELD_AREA = new RectangleArea(2631, 3359, 3, 10, 0);
    public static final WorldPosition GATE_NORTH_TILE = new WorldPosition(2634, 3361, 0);
    public static final WorldPosition GATE_SOUTH_TILE = new WorldPosition(2634, 3360, 0);
    private static final ToleranceComparator TOLERANCE_COMPARATOR = new SingleThresholdComparator(3);
    private static final ToleranceComparator TOLERANCE_COMPARATOR_2 = new SingleThresholdComparator(5);
    private static final SearchablePixel[] GATE_PIXELS = new SearchablePixel[]{new SearchablePixel(-10009586, new ChannelThresholdComparator(1, 1, 5), ColorModel.HSL), new SearchablePixel(-12571895, new ChannelThresholdComparator(1, 1, 3), ColorModel.HSL),};
    Map<Integer, Integer> slots = new HashMap<>();
    private int eatPercent = Utils.random(30, 80);
    private SearchablePixel highlightColor = new SearchablePixel(-14221313, TOLERANCE_COMPARATOR, ColorModel.RGB);
    private SearchablePixel highlightColor2 = new SearchablePixel(-2171877, TOLERANCE_COMPARATOR_2, ColorModel.RGB);
    private List<Seed> seedsToKeep = new ArrayList<>();
    private int foodItemID;
    private int foodAmount;
    private int amountOfNecklacesToWithdraw = -1;
    private boolean longTap = true;

    public MasterFarmerTheiver(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int poll() {
        // if the bank interface is visible handle it
        if (getWidgetManager().getBank().isVisible()) {
            log(getClass().getSimpleName(), "Handling bank interface");
            handleBankInterface();
            return 0;
        }

        // track seeds
        updateSlots();

        // when there is no free slots, handle dropping
        Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
        // if the itemgroup is not visible
        if (freeSlots.isEmpty()) {
            log(getClass().getSimpleName(), "Inventory is not visible");
            return 0;
        }
        if (freeSlots.get() == 0) {
            log(getClass().getSimpleName(), "Dropping seeds...");
            dropSeeds();
            return 0;
        }

        UIResult<Integer> hitpoints = getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
        // if we can't see the hitpoints orb for whatever reason
        if (!hitpoints.isFound()) {
            return 0;
        }

        // handle eating & restocking food
        if (hitpoints.get() <= eatPercent && foodItemID != -1) {
            log(getClass().getSimpleName(), "Need to eat food...");
            // check if we have food, if not walk to the bank & open it
            UIResultList<ItemSearchResult> food = getItemManager().findAllOfItem(getWidgetManager().getInventory(), foodItemID);
            if (food.isEmpty()) {
                log(getClass().getSimpleName(), "Walking to the bank");
                walkToBank();
                return 0;
            }
            log(getClass().getSimpleName(), "Eating food");
            // get a random food
            ItemSearchResult item = food.get(random(food.size()));
            // sleep until our hp changes
            if (item.interact("Eat")) {
                // randomise the health percentage for the next eat
                eatPercent = Utils.random(40, 80);
                // wait until hp increments
                submitTask(() -> {
                    UIResult<Integer> hitpointsRecent = getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
                    if (!hitpointsRecent.isFound()) {
                        return false;
                    }
                    return hitpointsRecent.get() > hitpoints.get();
                }, 4000);
            }
            return 0;
        }

        WorldPosition myPosition = getWorldPosition();
        // check if we are in the wonder area of the farmer, if not walk to it
        if (!FARMER_WANDER_AREA.contains(myPosition)) {
            log(getClass().getSimpleName(), "Walking to master farmer area");
            Point point = Utils.randomisePointByRadius(FARMER_WANDER_AREA.getCenter(), 3);
            getWalker().walkTo(point.x, point.y);
            return 0;
        }

        List<WorldPosition> validPositions = getValidNPCPositions();

        // no highlighted npc's found on game screen
        if (validPositions == null || validPositions.isEmpty()) {
            findFarmer(myPosition);
            return 0;
        }

        log(getClass().getSimpleName(), "Getting closest valid position");
        WorldPosition closestPosition = (WorldPosition) Utils.getClosestPosition(myPosition, validPositions.toArray(new WorldPosition[0]));

        // check if the gate is between the player and the target
        if (needsToHandleGate(myPosition, closestPosition)) {
            log(getClass().getSimpleName(), "Need to handle gate");
            log(getClass().getSimpleName(), "My pos: " + myPosition + " Closest pos: " + closestPosition);
            if (!handleGate()) {
                log(getClass().getSimpleName(), "Gate handle returned false");
                return 0;
            }
        }

        Polygon cubePoly = getSceneProjector().getTileCube(closestPosition, 130);
        Rectangle highlightBounds = getUtils().getHighlightBounds(cubePoly, highlightColor, highlightColor2);
        if (highlightBounds == null) {
            log(getClass().getSimpleName(), "Highlighted bounds are null");
            return 0;
        }

        if (longTap) {
            if (!getFinger().tap(highlightBounds, "Pickpocket Master Farmer")) {
                return 0;
            }
        } else {
            getFinger().tap(highlightBounds);
        }
        submitTask(() -> false, random(250, 1500));
        return 0;
    }

    private void findFarmer(WorldPosition myPosition) {
        log(getClass().getSimpleName(), "No valid positions, walking to furthest npc in farmers wander area...");

        // get the NPC furthest away in the farmers wander area
        WorldPosition furthestNPC = getFurthestNPC(myPosition);
        if (furthestNPC == null) {
            return;
        }
        List<WorldPosition> destPoints = Utils.getPositionsWithinRadius(furthestNPC, 2);
        WorldPosition destPoint = destPoints.get(random(destPoints.size()));
        // check if the gate is between the player and the target
        if (needsToHandleGate(myPosition, destPoint)) {
            log(getClass().getSimpleName(), "Need to handle gate");
            log(getClass().getSimpleName(), "My pos: " + myPosition + " Farmer pos: " + destPoint);
            if (!handleGate()) {
                log(getClass().getSimpleName(), "Gate handle returned false");
                return;
            }
        }
        log(getClass().getSimpleName(), "Walking to: " + destPoint);
        getWalker().walkTo(destPoint);
    }

    /**
     * Checks to see if we need to handle the gate by seeing if the farmers position is inside the field and ours isn't... vice versa
     *
     * @param myPosition     - Our {@link WorldPosition}
     * @param farmerPosition - The farmers {@link WorldPosition}
     * @return - Whether we need to handle the gate or not.
     */
    private boolean needsToHandleGate(WorldPosition myPosition, WorldPosition farmerPosition) {
        return FIELD_AREA.contains(myPosition) && !FIELD_AREA.contains(farmerPosition) || !FIELD_AREA.contains(myPosition) && FIELD_AREA.contains(farmerPosition);
    }

    /**
     * @return {@code true} if the gate is open, {@code false} otherwise.
     */
    private boolean handleGate() {
        boolean onScreen = gatePointsOnScreen();
        // not visible
        if (!onScreen) {
            log("Not on screen, walking to gate");
            // walk to gate until tiles are visible
            RSObject gate = getObjectManager().getClosestObject("gate");
            if (gate == null) {
                log(getClass().getSimpleName(), "Gate is null");
                return false;
            }
            log(getClass().getSimpleName(), "Executing walk method");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> gatePointsOnScreen());
            boolean result = getWalker().walkTo(gate.getWorldPosition(), builder.build());
            log(getClass().getSimpleName(), "Result: "+result);
            if (!result) {
                return false;
            }
        }
        Optional<Boolean> gateOpen = isGateOpen();
        if (!gateOpen.isPresent()) {
            log(getClass().getSimpleName(), "isGateOpen not present");
            return false;
        }
        // open gate if not open
        if (!gateOpen.get()) {
            log(getClass().getSimpleName(), "Opening gate");
            RSObject gate = getObjectManager().getClosestObject("gate");
            if (gate == null) {
                return false;
            }
            if (!gate.interact("open")) {
                return false;
            }
            log(getClass().getSimpleName(), "Interacted with gates");
            // wait for yellow/red tap circle to dissapear as it will cause false positive when checking if gate is open.
            sleep(800);
            // wait for gate to be open
            boolean result = submitTask(() -> {
                Optional<Boolean> gateOpen_ = isGateOpen();
                if (gateOpen_.isPresent() && gateOpen_.get()) {
                    return true;
                }
                return false;
            }, 10000);

            if (!result) {
                return false;
            }
        }
        // Make sure we remove the gate tile collision flag so the path finder can work its way to the other side.

        // We could also just remove the gate object in ObjectManager using ObjectManager#removeObject() and re-add the gate with its open rotation value.
        // I wouldn't say its necessary, but that would be the 'proper' way to go about this
        LocalPosition northTile = GATE_NORTH_TILE.toLocalPosition(this);
        CollisionMap collisionMap = getSceneManager().getLevelCollisionMap(northTile.getPlane());
        collisionMap.flags[northTile.getX()][northTile.getY()] = CollisionFlags.OPEN;
        return true;
    }

    /**
     * This is a pre-check to determine whether we need to walk to the gate or not, making sure all the points we analyse to determine the gates state are on screen.
     *
     * @return {@code true}: If all points of the tiles are on screen.
     * {@code false}: If we need to walk closer to the gate.
     */
    private boolean gatePointsOnScreen() {
        LocalPosition northLocal = GATE_NORTH_TILE.toLocalPosition(this);
        LocalPosition southLocal = GATE_SOUTH_TILE.toLocalPosition(this);

        // If either tile is not in the scene, return Optional.empty()
        if (northLocal == null || southLocal == null) {
            return false;
        }
        // Get the projected screen points for the north and south tiles
        Point neTilePoint = getSceneProjector().getTilePoint(northLocal.getX() + 1, northLocal.getY(), 0, TileEdge.N);
        Point seTilePoint = getSceneProjector().getTilePoint(southLocal.getX() + 1, southLocal.getY(), 0, TileEdge.S);

        Point nTilePoint = getSceneProjector().getTilePoint(northLocal.getX(), northLocal.getY(), 0, TileEdge.N);
        Point sTilePoint = getSceneProjector().getTilePoint(southLocal.getX(), southLocal.getY(), 0, TileEdge.S);

        // return true if all points are on screen
        return nTilePoint != null && sTilePoint != null && neTilePoint != null && seTilePoint != null;
    }

    /**
     * Determines whether a gate is open or closed by analyzing pixels within the gate's area.
     *
     * <p>This method makes 2 lines from either side of the gate (the center of the gate's tile & right tile adjacent the gate from north to south.
     * We then use these 2 lines to generate horizontal scan lines which we loop through and check for specific gate pixels.
     * If any horizontal scan line does not contain a gate pixel, the gate is assumed to be open.
     *
     * <p>See the visual explanation here:
     * <a href="https://imgur.com/a/V9lwbgt" target="_blank">Visual Reference</a>
     * <a href="https://imgur.com/bKx3K8J" target="_blank">Visual Reference</a>
     *
     * @return {@code Optional<Boolean>}:
     * - {@code Optional.of(true)}: The gate is open.
     * - {@code Optional.of(false)}: The gate is closed.
     * - {@code Optional.empty()}: The gate tiles are not in the scene or are not visible on the screen.
     */
    private Optional<Boolean> isGateOpen() {
        // ensure we are stood still and the screen isn't moving before performing the check
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(getWorldPosition());
        sleep(300);
        boolean result = submitTask(() -> {
            WorldPosition currentWorldPosition = getWorldPosition();
            if (currentWorldPosition == null) {
                return false;
            }
            if (previousPosition.get().equals(currentWorldPosition)) {
                return true;
            } else {
                previousPosition.set(currentWorldPosition);
            }
            return false;
        }, 7000);
        if (!result) return Optional.empty();
        // convert world positions to local positions relative to the current scene
        LocalPosition northLocal = GATE_NORTH_TILE.toLocalPosition(this);
        LocalPosition southLocal = GATE_SOUTH_TILE.toLocalPosition(this);

        // If either tile is not in the scene, return Optional.empty()
        if (northLocal == null || southLocal == null) {
            return Optional.empty();
        }

        // get the projected screen points for the north and south tiles
        Point neTilePoint = getSceneProjector().getTilePoint(northLocal.getX() + 1, northLocal.getY(), 0, TileEdge.N);
        Point seTilePoint = getSceneProjector().getTilePoint(southLocal.getX() + 1, southLocal.getY(), 0, TileEdge.S);

        Point nTilePoint = getSceneProjector().getTilePoint(northLocal.getX(), northLocal.getY(), 0, TileEdge.N);
        Point sTilePoint = getSceneProjector().getTilePoint(southLocal.getX(), southLocal.getY(), 0, TileEdge.S);

        // If any of the projected points are off-screen, return Optional.empty()
        if (nTilePoint == null || sTilePoint == null || neTilePoint == null || seTilePoint == null) {
            return Optional.empty();
        }

        // calculate the lengths of the right and left sides of the gate (in pixels)
        double rightLength = Math.sqrt(Math.pow(seTilePoint.getX() - neTilePoint.getX(), 2) + Math.pow(seTilePoint.getY() - neTilePoint.getY(), 2));
        double leftLength = Math.sqrt(Math.pow(sTilePoint.getX() - nTilePoint.getX(), 2) + Math.pow(sTilePoint.getY() - nTilePoint.getY(), 2));

        // determine the number of horizontal scan lines to generate
        int maxSteps = (int) Math.min(rightLength, leftLength);

        Line rightSideLine = new Line(neTilePoint, seTilePoint);
        Line centerLine = new Line(nTilePoint, sTilePoint);
        List<Rectangle> gameFrameBoundaries = getWidgetManager().getGameFrameBoundaries();

        // check if the tiles are covered by any interfaces
        for (Rectangle gameFrameBoundary : gameFrameBoundaries) {
            if (Utils.lineIntersectsRectangle(rightSideLine, gameFrameBoundary) || Utils.lineIntersectsRectangle(centerLine, gameFrameBoundary)) {
                return Optional.empty();
            }
        }

        // generate horizontal scan lines between the right and left sides of the gate
        List<Line> horizontalScanLines = new ArrayList<>();
        for (int i = 0; i <= maxSteps; i++) {
            double fraction = i / (double) maxSteps;

            // Interpolate points along the right-side line
            int rightX = (int) (neTilePoint.getX() + fraction * (seTilePoint.getX() - neTilePoint.getX()));
            int rightY = (int) (neTilePoint.getY() + fraction * (seTilePoint.getY() - neTilePoint.getY()));
            Point interpolatedRightPoint = new Point(rightX, rightY);

            // Interpolate points along the left-side line
            int leftX = (int) (nTilePoint.getX() + fraction * (sTilePoint.getX() - nTilePoint.getX()));
            int leftY = (int) (nTilePoint.getY() + fraction * (sTilePoint.getY() - nTilePoint.getY()));
            Point interpolatedLeftPoint = new Point(leftX, leftY);

            // Add a horizontal scan line between the interpolated points
            horizontalScanLines.add(new Line(interpolatedRightPoint, interpolatedLeftPoint));
        }

        // Analyze each horizontal scan line
        for (int i = 0; i < horizontalScanLines.size(); i++) {
            Line line = horizontalScanLines.get(i);
            Point start = line.getStart();
            Point end = line.getEnd();

            // Calculate the length of the current scan line
            int dx = end.x - start.x;
            int dy = end.y - start.y;
            int length = (int) Math.sqrt(dx * dx + dy * dy);

            boolean pixelInLine = false;
            // Check for gate pixels along the current scan line
            int j;
            for (j = 0; j <= length; j++) {
                double fraction = j / (double) length;
                int pixelX = (int) (start.getX() + fraction * dx);
                int pixelY = (int) (start.getY() + fraction * dy);

                // If a gate pixel is found, mark it as present
                if (getPixelAnalyzer().isPixelAt(pixelX, pixelY, GATE_PIXELS)) {
                    pixelInLine = true;
                    break;
                }
            }

            // If no gate pixel is found on this scan line, the gate must be open
            if (!pixelInLine) {
                getScreen().getDrawableCanvas().drawLine(start.x, start.y, end.x, end.y, Color.CYAN.getRGB());
                return Optional.of(true);
            }
        }

        // If all scan lines contain gate pixels, the gate is closed
        return Optional.of(false);
    }

    private WorldPosition getFurthestNPC(WorldPosition myPosition) {
        // Get the list of NPC positions from the minimap widget
        UIResultList<WorldPosition> npcPositions = getWidgetManager().getMinimap().getNPCPositions();

        // return null if minimap is not visible
        if (npcPositions.isNotVisible()) {
            return null;
        }
        if (npcPositions.isNotFound()) {
            log(getClass().getSimpleName(), "No NPC's found nearby...");
            return null;
        }
        // instantiate an arraylist as asList returns an unmodifiable list.
        List<WorldPosition> positionList = new ArrayList<>(npcPositions.asList());
        positionList.removeIf(position -> !FARMER_WANDER_AREA.contains(position));
        // get furthest npc
        return positionList.stream().max(Comparator.comparingDouble(npc -> npc.distanceTo(myPosition))).orElse(null);
    }

    private List<WorldPosition> getValidNPCPositions() {
        UIResultList<WorldPosition> npcPositions = getWidgetManager().getMinimap().getNPCPositions();
        if (npcPositions.isNotVisible()) {
            return null;
        }
        if (npcPositions.isNotFound()) {
            log(getClass().getSimpleName(), "No NPC's found nearby...");
            return null;
        }
        List<WorldPosition> validPositions = new ArrayList<>();
        npcPositions.forEach(position -> {
            // check if npc is in farmers wander area
            if (!FARMER_WANDER_AREA.contains(position)) {
                return;
            }
            // convert to local
            LocalPosition localPosition = position.toLocalPosition(this);
            // get poly for position
            Polygon poly = getSceneProjector().getTileCube(localPosition.getX(), localPosition.getY(), localPosition.getPlane(), 150);
            if (poly == null) {
                return;
            }
            Polygon cubeResized = poly.getResized(1.3).convexHull();
            if (cubeResized == null) {
                return;
            }
            if (getPixelAnalyzer().findPixel(cubeResized, highlightColor) != null) {
                validPositions.add(position);
                getScreen().getDrawableCanvas().drawPolygon(cubeResized.getXPoints(), cubeResized.getYPoints(), cubeResized.numVertices(), Color.GREEN.getRGB(), 1);
            }
        });
        return validPositions;
    }

    public void dropSeeds() {
        log(getClass().getSimpleName(), "Dropping unwanted seeds");
        List<Integer> itemsToDrop = new ArrayList<>();
        for (Seed seed : Seed.values()) {
            if (seedsToKeep.contains(seed)) {
                continue;
            }
            itemsToDrop.add(seed.getId());
        }
        // convert to primitive int[] array
        int[] itemsToDropArray = new int[itemsToDrop.size()];
        for (int i = 0; i < itemsToDropArray.length; i++) {
            itemsToDropArray[i] = itemsToDrop.get(i);
        }
        getItemManager().dropItems(getWidgetManager().getInventory(), itemsToDropArray);
    }

    public void handleBankInterface() {

    }

    private void walkToBank() {
        RSObject bank = getObjectManager().getClosestObject("Bank booth");

        // walk to bank area if bank isn't in our loaded scene
        if (bank == null) {
            getWalker().walkTo(BANK_AREA.getRandomPosition());
            return;
        }
        if (bank.interact("Bank")) {
            submitTask(() -> getWidgetManager().getBank().isVisible(), 10000);
        }
    }

    @Override
    public void onStart() {
        ScriptOptions scriptOptions = new ScriptOptions(this);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);
        highlightColor = new SearchablePixel(scriptOptions.getHighlightColor(), TOLERANCE_COMPARATOR, ColorModel.RGB);
        Pair<Integer, Integer> foodOptions = scriptOptions.getFoodOptions();
        if (foodOptions != null) {
            foodItemID = foodOptions.getKey();
            foodAmount = foodOptions.getValue();
        }

        int necksToWithdraw = scriptOptions.getAmountOfNecklacesToWithdraw();
        if (necksToWithdraw > 0) {
            this.amountOfNecklacesToWithdraw = necksToWithdraw;
        }

        seedsToKeep.addAll(scriptOptions.getSeedsToKeep());
    }


    /**
     * A custom item listener, this is because herb seeds have the same model so it makes it impossible for us to really tell them apart.
     * What this does is just loops through the slots & checks against the list on the previous loop
     */
    public boolean updateSlots() {
        UIResultList<ItemSearchResult> results = getItemManager().findAllOfItem(getWidgetManager().getInventory(), SEED_IDS);
        boolean updated = false;
        List<Integer> slotsToIgnore = new ArrayList<>();
        for (ItemSearchResult result : results) {
            slotsToIgnore.add(result.getItemSlot());
            boolean update = updateSlot(result.getItemSlot(), result.getStackAmount());
            if (update) {
                updated = true;
            }
        }
        for (int i = 0; i < 28; i++) {
            if (slotsToIgnore.contains(i)) {
                continue;
            }
            slots.remove(i);
        }
        return updated;
    }

    public boolean updateSlot(int slot, int amount) {
        if (!slots.containsKey(slot)) {
            slots.put(slot, amount);
            return true;
        } else {
            int slotAmount = slots.get(slot);
            if (slotAmount < amount) {
                slots.put(slot, amount);
                return true;
            }
        }
        return false;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{10548};
    }

}
