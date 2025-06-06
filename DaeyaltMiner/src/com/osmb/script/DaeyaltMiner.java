package com.osmb.script;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.component.tabs.SettingsTabComponent;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.walker.WalkConfig;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ScriptDefinition(name = "Daeyalt Miner", description = "Mines Daeyalt essence in the Daeyalt mines.", version = 1.0, author = "Joe", skillCategory = SkillCategory.MINING)
public class DaeyaltMiner extends Script {

    private static final Area CENTER_AREA = new RectangleArea(3673, 9755, 9, 5, 2);
    private static final Font ARIEL = Font.getFont("Ariel");
    private static final int MAX_ZOOM = 20;
    private final SearchablePixel[] ACTIVE_ROCK_PIXELS = {
            new SearchablePixel(-15646911, new SingleThresholdComparator(3), ColorModel.HSL),
            new SearchablePixel(-14978699, new SingleThresholdComparator(3), ColorModel.HSL),
            new SearchablePixel(-14712700, new SingleThresholdComparator(3), ColorModel.HSL)
    };
    private int animationTimeout;
    private boolean setZoom = false;

    public DaeyaltMiner(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        this.animationTimeout = random(4000, 6000);
    }

    @Override
    public int poll() {
        if (!setZoom) {
            log(DaeyaltMiner.class, "Checking zoom level...");
            // check if the settings tab + display sub-tab is open, if not, open it
            if (!getWidgetManager().getSettings().openSubTab(SettingsTabComponent.SettingsSubTabType.DISPLAY_TAB)) {
                return 0;
            }
            UIResult<Integer> zoomResult = getWidgetManager().getSettings().getZoomLevel();
            if (zoomResult.isFound()) {
                int currentZoom = zoomResult.get();
                if (currentZoom > MAX_ZOOM) {
                    // generate random zoom level between 0 and MAX_ZOOM
                    int zoomLevel = random(0, MAX_ZOOM);
                    if (getWidgetManager().getSettings().setZoomLevel(zoomLevel)) {
                        log(DaeyaltMiner.class, "Zoom level set to: " + zoomLevel);
                        // zoom level set, set flag to true
                        setZoom = true;
                    }
                } else {
                    setZoom = true;
                }
            }
            return 0;
        }
        // close widgets if they are open
        if (getWidgetManager().getChatbox().isOpen()) {
            getWidgetManager().getChatbox().close();
            return 0;
        }
        if (!getWidgetManager().getTabManager().closeContainer()) {
            log(DaeyaltMiner.class, "Failed to close tab container.");
            return 0;
        }

        // get all Daeyalt essence rocks
        List<RSObject> rocks = getDaeyaltEssenceRocks();
        if (rocks.isEmpty()) {
            log(DaeyaltMiner.class, "No Daeyalt essence rocks found, make sure you run this script inside the Daeyalt mines.");
            return 0;
        }
        // filter out rocks that aren't on screen
        log(DaeyaltMiner.class, "Found " + rocks.size() + " Daeyalt essence rocks.");
        Map<Rock, RSObject> rocksOnScreen = getRocksOnScreen(rocks);
        if (rocksOnScreen.isEmpty()) {
            log(DaeyaltMiner.class, "No Daeyalt rocks on screen, walking to center area.");
            // walk to center area if no rocks are on screen
            walkToCenter();
            return 0;
        }
        drawRocksOnScreen(rocksOnScreen);
        // search for an active rock
        Map.Entry<Rock, RSObject> activeRockEntry = findActiveRock(rocksOnScreen);
        if (activeRockEntry == null) {
            // if no active rock is found
            log(DaeyaltMiner.class, "No active rock found.");
            for(Map.Entry<Rock, RSObject> entry : rocksOnScreen.entrySet()) {
                log(DaeyaltMiner.class, "Rock on screen: " + entry.getKey() + " at " + entry.getValue().getWorldPosition());
            }
            List<Rock> rocksList = new ArrayList<>(List.of(Rock.values()));
            rocksList.removeAll(rocksOnScreen.keySet());
            log(DaeyaltMiner.class, "Rocks we can't see: " + rocksList);

            // if only 1 rock remains, then this must be the active rock
            if (rocksList.size() == 1) {
                log(DaeyaltMiner.class, "There is only one rock we can't see, assuming it is the active rock.");
                Rock rock = rocksList.get(0);
                RSObject targetRock = null;
                for(RSObject rsObject : rocks) {
                    if (rsObject.getWorldPosition().equals(rock.getRockPosition())) {
                        targetRock = rsObject;
                        break;
                    }
                }
                if( targetRock == null) {
                    log(DaeyaltMiner.class, "Target rock is null, cannot walk to it.");
                    return 0;
                }
                log(DaeyaltMiner.class, "Walking to rock: " + targetRock.getWorldPosition());
                walkToRock(targetRock);
            } else {
                log(DaeyaltMiner.class, "Can't see multiple rocks, walking to center area.");
                walkToCenter();
            }
            return 0;
        }

        // we have an active rock, interact with it
        WorldPosition pos = getWorldPosition();
        if (pos == null) {
            log(DaeyaltMiner.class, "Position is null.");
            return 0;
        }
        RSObject targetRock = rocks.stream().filter(rock -> rock.getWorldPosition().equals(activeRockEntry.getKey().getRockPosition())).findFirst().orElse(null);
        // if an active rock is found, interact
        if (targetRock.interact("Mine")) {
            log(DaeyaltMiner.class, "Interacted with rock: " + activeRockEntry.getKey());
            waitUntilFinishedMining(targetRock);
        }
        return 0;
    }

    private List<RSObject> getDaeyaltEssenceRocks() {
        return getObjectManager().getObjects(object -> {
            String name = object.getName();
            if (name == null) {
                return false;
            }
            if (!name.equalsIgnoreCase("daeyalt essence")) {
                return false;
            }
            WorldPosition position = object.getWorldPosition();
            log(DaeyaltMiner.class, "Found rock at: " + position);
            if (Rock.getRockByPosition(position) == null) {
                return false;
            }
            return true;
        });
    }

    private void waitUntilFinishedMining(RSObject targetRock) {
        // wait until we start moving
        if (targetRock.getTileDistance() > 1) {
            boolean moving = submitTask(() -> getLastPositionChangeMillis() < 500, random(1000, 2500));
            if (!moving) {
                log(DaeyaltMiner.class, "We aren't moving after clicking the rock, polling again.");
                return;
            }
        }
        log(DaeyaltMiner.class, "Waiting until finished mining rock");
        Timer animatingTimer = new Timer();
        AtomicInteger rockActiveCheckFails = new AtomicInteger(0);
        Stopwatch rockCheckTimer = new Stopwatch();
        submitHumanTask(() -> {
            if (animatingTimer.timeElapsed() > animationTimeout) {
                log(DaeyaltMiner.class, "Animation timeout");
                this.animationTimeout = random(2000, 4000);
                return true;
            }
            if (!isRockActive(targetRock)) {
                // use a timer as frames are updated super quick with OSMB
                rockCheckTimer.reset(200);
                rockActiveCheckFails.incrementAndGet();
                if (rockActiveCheckFails.get() > 3) {
                    log(DaeyaltMiner.class, "Rock is no longer active.");
                    return true;
                }
            }
            if (getPixelAnalyzer().isPlayerAnimating(0.2)) {
                animatingTimer.reset();
            }
            return false;
        }, random(70000, 90000));
    }

    private void drawRocksOnScreen(Map<Rock, RSObject> rocksOnScreen) {
        getScreen().queueCanvasDrawable("rocksOnScreen", (canvas) -> {
            for (Map.Entry<Rock, RSObject> entry : rocksOnScreen.entrySet()) {
                Rock rock = entry.getKey();
                Polygon polygon = entry.getValue().getConvexHull();
                if (polygon == null) {
                    continue;
                }
                canvas.fillPolygon(polygon, Color.RED.getRGB(), 0.2);
                canvas.drawPolygon(polygon, Color.RED.getRGB(), 1);
                Point center = polygon.getCenter();
                canvas.drawText(rock.name(), center.x, center.y, Color.WHITE.getRGB(), ARIEL);
            }
        });
    }

    private void walkToRock(RSObject rock) {
        WalkConfig.Builder walkConfigBuilder = new WalkConfig.Builder();
        walkConfigBuilder.breakCondition(() -> {
            Polygon rockPolygon = rock.getConvexHull();
            if (rockPolygon == null) {
                return false;
            }
            return getWidgetManager().insideGameScreenFactor(rockPolygon, Collections.emptyList()) >= 0.5;
        });
        getWalker().walkTo(rock.getWorldPosition(), walkConfigBuilder.build());
    }

    private void walkToCenter() {
        log(DaeyaltMiner.class, "Walking to center of the mine");
        WalkConfig.Builder walkConfigBuilder = new WalkConfig.Builder();
        walkConfigBuilder.breakCondition(() -> {
            WorldPosition playerPosition = getWorldPosition();
            if (playerPosition == null) {
                return false;
            }
            return CENTER_AREA.contains(playerPosition);
        });
        getWalker().walkTo(CENTER_AREA.getRandomPosition(), walkConfigBuilder.build());
    }

    private Map.Entry<Rock, RSObject> findActiveRock(Map<Rock, RSObject> rocksOnScreen) {
        for (Map.Entry<Rock, RSObject> entry : rocksOnScreen.entrySet()) {
            if (isRockActive(entry.getValue())) {
                log(DaeyaltMiner.class, "Found active rock: " + entry.getKey());
                return entry;
            }
        }
        return null;
    }

    private boolean isRockActive(RSObject rock) {
        Polygon polygon = rock.getConvexHull();
        if (polygon == null) {
            return false;
        }
        // check if the rock has active pixels
        return getPixelAnalyzer().findPixel(polygon, ACTIVE_ROCK_PIXELS) != null;
    }

    private Map<Rock, RSObject> getRocksOnScreen(List<RSObject> rocks) {
        Map<Rock, RSObject> rocksOnScreen = new HashMap<>();
        for (RSObject rock : rocks) {
            Polygon polygon = rock.getConvexHull();
            if (polygon == null) {
                continue;
            }
            if (getWidgetManager().insideGameScreenFactor(polygon, Collections.emptyList()) < 0.3) {
                continue;
            }
            Rock rockType = Rock.getRockByPosition(rock.getWorldPosition());
            rocksOnScreen.put(rockType, rock);
        }
        return rocksOnScreen;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{14744, 14484};
    }

    enum Rock {
        NORTH(new WorldPosition(3674, 9765, 2)),
        EAST(new WorldPosition(3687, 9755, 2)),
        WEST(new WorldPosition(3671, 9750, 2));

        private final WorldPosition rockPosition;

        Rock(WorldPosition rockPosition) {
            this.rockPosition = rockPosition;
        }

        public static Rock getRockByPosition(WorldPosition position) {
            for (Rock rock : Rock.values()) {
                if (rock.getRockPosition().equals(position)) {
                    return rock;
                }
            }
            return null;
        }

        public WorldPosition getRockPosition() {
            return rockPosition;
        }
    }
}
