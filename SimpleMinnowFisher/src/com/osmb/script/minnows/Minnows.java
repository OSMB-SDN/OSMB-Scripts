package com.osmb.script.minnows;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.walker.pathing.CollisionManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ScriptDefinition(name = "Minnows Fisher", skillCategory = SkillCategory.FISHING, version = 1.0, author = "Joe", description = "Fishes minnows for Sharks at the Fishing Guild.")
public class Minnows extends Script {
    private static final RectangleArea EAST_FISHING_SPOT_AREA = new RectangleArea(2617, 3443, 3, 1, 0);
    private static final RectangleArea WEST_FISHING_SPOT_AREA = new RectangleArea(2609, 3443, 3, 1, 0);
    private static final int FISH_TILE_HEIGHT = 15;
    /**
     * We have two images for the minnow tile, one for the top half and one for the bottom half.
     * This is because when zoomed out adjacent tile images can overlap
     */
    private SearchableImage minnowTileImageTop;
    private SearchableImage minnowTileImageBottom;

    public Minnows(Object scriptCore) {
        super(scriptCore);
    }

    private static WorldPosition getClosestFishingAreaPosition(WorldPosition myPosition) {
        List<WorldPosition> eastSidePositions = EAST_FISHING_SPOT_AREA.getSurroundingPositions(1);
        List<WorldPosition> westSidePositions = WEST_FISHING_SPOT_AREA.getSurroundingPositions(1);
        WorldPosition closestEastPosition = myPosition.getClosest(eastSidePositions);
        WorldPosition closestWestPosition = myPosition.getClosest(westSidePositions);
        // get closest out of the two sides
        return closestEastPosition.distanceTo(myPosition) < closestWestPosition.distanceTo(myPosition) ? closestEastPosition : closestWestPosition;
    }

    @Override
    public void onStart() {
        SearchableImage[] itemImages = getItemManager().getItem(ItemID.MINNOW, true);
        minnowTileImageTop = itemImages[itemImages.length - 1];
        minnowTileImageBottom = new SearchableImage(minnowTileImageTop.copy(), minnowTileImageTop.getToleranceComparator(), minnowTileImageTop.getColorModel());
        makeHalfTransparent(minnowTileImageTop, true);
        makeHalfTransparent(minnowTileImageBottom, false);
    }

    private void makeHalfTransparent(SearchableImage image, boolean topHalf) {
        int startY = topHalf ? 0 : image.getHeight() / 2;
        int endY = topHalf ? image.getHeight() / 2 : image.getHeight();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = startY; y < endY; y++) {
                image.setRGB(x, y, ColorUtils.TRANSPARENT_PIXEL);
            }
        }
    }

    @Override
    public int poll() {
        log(Minnows.class, "Getting fishing spots...");
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(Minnows.class, "Failed to get position.");
            return 0;
        }
        if (ensureWidgetsCollapsed()) {
            return 0;
        }

        List<WorldPosition> activeFishingSpots = getFishingSpots();
        if (activeFishingSpots.isEmpty()) {
            log(Minnows.class, "No active fishing spots found.");
            if (WEST_FISHING_SPOT_AREA.distanceTo(myPosition) > 1 && EAST_FISHING_SPOT_AREA.distanceTo(myPosition) > 1) {
                log(Minnows.class, "Walking to fishing area...");
                walkToFishingArea(myPosition);
            }
            return 0;
        }
        log(Minnows.class, "Found active fishing spots on screen: " + activeFishingSpots.size());
        WorldPosition closestFishingSpot = myPosition.getClosest(activeFishingSpots);
        Polygon tilePoly = getSceneProjector().getTilePoly(closestFishingSpot);
        if (tilePoly == null) {
            log(Minnows.class, "No tile polygon found for closest fishing spot: " + closestFishingSpot);
            return 0;
        }
        if (!getFinger().tap(tilePoly, "small net")) {
            log(Minnows.class, "Failed to tap on fishing spot: " + closestFishingSpot);
            return 0;
        }
        if (waitUntilAdjacentToFishingSpot()) {
            waitUntilFinishedFishing();
        }
        return 0;
    }

    private boolean waitUntilAdjacentToFishingSpot() {
        log(Minnows.class, "Waiting until we arrive at the fishing spot...");
        return submitHumanTask(() -> {
            WorldPosition myPosition = getWorldPosition();
            if (myPosition == null) {
                log(Minnows.class, "Failed to get position");
                return false;
            }
            List<WorldPosition> fishingSpots = getFishingSpots();
            if (fishingSpots.isEmpty()) {
                log(Minnows.class, "No fishing spots found...");
                return false;
            }
            return getAdjacentFishingSpot(fishingSpots, myPosition) != null;
        }, random(2500, 6000));
    }

    private void waitUntilFinishedFishing() {
        log(Minnows.class, "Waiting until finished fishing...");
        submitTask(() -> {
            WorldPosition myPosition = getWorldPosition();
            if (myPosition == null) {
                log(Minnows.class, "Failed to get position");
                return false;
            }
            List<WorldPosition> fishingSpots = getFishingSpots();
            if (fishingSpots.isEmpty()) {
                log(Minnows.class, "No fishing spots found...");
                return false;
            }
            boolean isAdjacent = getAdjacentFishingSpot(fishingSpots, myPosition) != null;
            if (!isAdjacent) {
                log(Minnows.class, "Not adjacent to fishing spot");
                return true;
            }
            return false;
        }, random(16000, 22000));
        // random delay before next fishing attempt
        int randomDelay = RandomUtils.gaussianRandom(300, 5000, 500, 1500);
        log(Minnows.class, "⏳ - Executing humanised delay before next fishing attempt: " + randomDelay + "ms");
        submitTask(() -> false, randomDelay);
    }

    private WorldPosition getAdjacentFishingSpot(List<WorldPosition> fishingSpots, WorldPosition myPosition) {
        if (fishingSpots.isEmpty()) {
            return null;
        }
        for (WorldPosition fishingSpotPosition : fishingSpots) {
            if (CollisionManager.isCardinallyAdjacent(myPosition, fishingSpotPosition)) {
                return fishingSpotPosition;
            }
        }
        return null;
    }

    private void walkToFishingArea(WorldPosition myPosition) {
        WorldPosition targetPosition = getClosestFishingAreaPosition(myPosition);
        getWalker().walkTo(targetPosition);
    }

    private boolean ensureWidgetsCollapsed() {
        if (getWidgetManager().getChatbox().isOpen()) {
            getWidgetManager().getChatbox().close();
            return true;
        }
        if (!getWidgetManager().getTabManager().closeContainer()) {
            log(Minnows.class, "Failed to close tab container.");
            return true;
        }
        return false;
    }

    private List<WorldPosition> getFishingSpots() {
        List<WorldPosition> fishingSpots = new ArrayList<>();
        fishingSpots.addAll(EAST_FISHING_SPOT_AREA.getAllWorldPositions());
        fishingSpots.addAll(WEST_FISHING_SPOT_AREA.getAllWorldPositions());
        List<WorldPosition> activeFishingSpots = new ArrayList<>();
        for (WorldPosition fishingSpot : fishingSpots) {
            if (checkForTileItem(fishingSpot)) {
                activeFishingSpots.add(fishingSpot);
                getScreen().queueCanvasDrawable("foundSpot=" + fishingSpot, canvas -> {
                    // draw fishing spot
                    Polygon tilePoly = getSceneProjector().getTilePoly(fishingSpot);
                    canvas.fillPolygon(tilePoly, Color.GREEN.getRGB(), 0.3);
                    canvas.drawPolygon(tilePoly, Color.RED.getRGB(), 1);
                });
            }
        }
        return activeFishingSpots;
    }

    private boolean checkForTileItem(WorldPosition tilePosition) {
        Point point = getSceneProjector().getTilePoint(tilePosition, null/*null means center point*/, FISH_TILE_HEIGHT);
        if (point == null) {
            log(Minnows.class, "No tile point found for position: " + tilePosition);
            return false;
        }
        Point tileItemPoint = new Point(point.x - (minnowTileImageTop.width / 2), point.y - (minnowTileImageTop.height / 2) - 20);
        int radius = 6;
        return searchForItemInRadius(tilePosition, tileItemPoint, radius, minnowTileImageTop, minnowTileImageBottom);
    }

    private boolean searchForItemInRadius(WorldPosition worldPosition, Point position, int radius, SearchableImage... itemImages) {
        for (int x = position.x - radius; x <= position.x + radius; x++) {
            for (int y = position.y - radius; y <= position.y + radius; y++) {
                for (SearchableImage itemImage : itemImages) {
                    if (getImageAnalyzer().isSubImageAt(x, y, itemImage) != null) {
                        getScreen().queueCanvasDrawable("harpoonFishTile-" + worldPosition, canvas -> {
                            // draw search area
                            com.osmb.api.shape.Rectangle tileItemArea = new Rectangle(position.x - radius, position.y - radius, itemImage.height + (radius * 2), itemImage.height + (radius * 2));
                            canvas.fillRect(tileItemArea, Color.GREEN.getRGB(), 0.3);
                            canvas.drawRect(tileItemArea, Color.RED.getRGB(), 1);
                        });
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{10293};
    }
}
