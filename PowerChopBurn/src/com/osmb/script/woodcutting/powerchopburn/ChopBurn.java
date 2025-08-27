package com.osmb.script.woodcutting.powerchopburn;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.woodcutting.powerchopburn.data.Tree;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.osmb.script.woodcutting.powerchopburn.Options.animationTimeout;
import static com.osmb.script.woodcutting.powerchopburn.Options.selectedArea;
import static com.osmb.script.woodcutting.powerchopburn.Status.BLACKLIST_TIMEOUT;
import static com.osmb.script.woodcutting.powerchopburn.Status.burning;


@ScriptDefinition(name = "Chop and Burn", description = "Chops trees and burns logs.", version = 1.0, author = "Joe", skillCategory = SkillCategory.WOODCUTTING)
public class ChopBurn extends Script {

    public static final Set<Integer> LOGS = Set.of(ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS);
    public static final Map<Integer, String> LOG_NAMES = new HashMap<>();
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(List.of(ItemID.TINDERBOX));
    private static final Map<WorldPosition, Long> TREE_BLACKLIST = new HashMap<>();
    private static final Stopwatch LEVEL_CHECK_TIMER = new Stopwatch();
    private static final Font ARIEL = new Font("Arial", Font.PLAIN, 14);
    private static String task = "None";
    private final BonfireHandler bonfireHandler;
    private int woodcuttingLevel = 1;
    private int firemakingLevel = 1;

    public ChopBurn(Object scriptCore) {
        super(scriptCore);
        this.bonfireHandler = new BonfireHandler(this);
    }

    @Override
    public void onStart() {
        for (int logID : LOGS) {
            String name = getItemManager().getItemName(logID);
            if (name != null) {
                LOG_NAMES.put(logID, name);
            }
        }
        ITEM_IDS_TO_RECOGNISE.addAll(LOGS);
        Options.amountChangeTimeout = random(6200, 9000);
        Options.animationTimeout = random(2000, 4000);
    }

    @Override
    public int poll() {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(ChopBurn.class, "World position is null");
            return 0;
        }
        if (!selectedArea.contains(worldPosition)) {
            task = "Walking to area";
            log(ChopBurn.class, "Outside of the area, walking back...");
            WalkConfig.Builder config = new WalkConfig.Builder();
            config.breakCondition(() -> {
                WorldPosition currentPosition = getWorldPosition();
                if (currentPosition == null) {
                    log(ChopBurn.class, "Current position is null");
                    return false;
                }
                return selectedArea.contains(currentPosition);
            });
            getWalker().walkTo(selectedArea.getRandomPosition(), config.build());
        }
        if (LEVEL_CHECK_TIMER.hasFinished()) {
            task = "Updating levels";
            updateLevels();
        }

        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(ChopBurn.class, "Failed to get inventory snapshot.");
            return 0;
        }
        if (!inventorySnapshot.contains(ItemID.TINDERBOX)) {
            log(ChopBurn.class, "Tinderbox not found, stopping script...");
            stop();
            return 0;
        }

        if (burning && !inventorySnapshot.containsAny(LOGS)) {
            burning = false;
        } else if (inventorySnapshot.isFull()) {
            burning = true;
        }

        if (burning) {
            task = "Burning logs";
            // burn logs
            bonfireHandler.burnLogs(inventorySnapshot);
        } else {
            task = "Chopping trees";
            // chop trees
            chopTrees();
        }

        return 0;
    }

    private void updateLevels() {
        SkillsTabComponent.SkillLevel firemakingskillLevel = getWidgetManager().getSkillTab().getSkillLevel(SkillType.FIREMAKING);
        SkillsTabComponent.SkillLevel woodcuttingSkillLevel = getWidgetManager().getSkillTab().getSkillLevel(SkillType.WOODCUTTING);
        if (firemakingskillLevel == null || woodcuttingSkillLevel == null) {
            log(ChopBurn.class, "Failed to get skill levels.");
            return;
        }
        firemakingLevel = firemakingskillLevel.getLevel();
        woodcuttingLevel = woodcuttingSkillLevel.getLevel();
        log(ChopBurn.class, "Woodcutting level: " + woodcuttingLevel + ", Firemaking level: " + firemakingLevel);
        LEVEL_CHECK_TIMER.reset(random(TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(30)));
    }

    private void chopTrees() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(ChopBurn.class, "Position is null");
            return;
        }
        Tree treeType = Tree.getTreeForLevel(Math.min(woodcuttingLevel, firemakingLevel));
        if (treeType == null) {
            log(ChopBurn.class, "No treeType found for level " + Math.min(woodcuttingLevel, firemakingLevel));
            return;
        }
//        Tree treeType = Tree.NORMAL; // For testing purposes, always use NORMAL tree
        // get the trees in the area
        List<RSObject> trees = getObjectManager().getObjects(rsObject -> {
            if (!selectedArea.contains(rsObject.getWorldPosition())) {
                return false;
            }
            String name = rsObject.getName();
            return name != null && name.equalsIgnoreCase(treeType.getObjectName());
        });
        if (trees.isEmpty()) {
            log("No trees found in the area, please run near an area of trees.");
            stop();
            return; // Wait for a second before checking again
        }
        // get treeType's on screen (list is already sorted by distance)
        List<RSObject> visibleTrees = getVisibleTrees(trees, myPosition);
        List<RSObject> activeTrees = getActiveTrees(treeType, visibleTrees);
        if (activeTrees.isEmpty()) {
            trees.removeAll(visibleTrees);
            trees.sort(Comparator.comparingDouble(value -> value.distance(myPosition)));
            // get closest treeType off screen
            if (!trees.isEmpty()) {
                RSObject closestOffScreenTree = trees.get(0);
                log("Closest treeType off screen: " + closestOffScreenTree.getName() + " at " + closestOffScreenTree.getWorldPosition());
                getWalker().walkTo(closestOffScreenTree);
            } else {
                log("No eligible nearby trees found");
                stop();
            }
            return;
        }
        RSObject closestTree = activeTrees.get(0);
        getScreen().queueCanvasDrawable("activeTrees", (canvas) -> {
            for (RSObject rsObject : activeTrees) {
                Polygon treePolygon = rsObject.getConvexHull();
                if (treePolygon != null) {
                    canvas.fillPolygon(treePolygon, Color.GREEN.getRGB(), 0.3);
                    int color = rsObject.equals(closestTree) ? Color.CYAN.getRGB() : Color.GREEN.getRGB();
                    canvas.drawPolygon(treePolygon, color, 1);
                }
            }
        });

        Polygon closestTreePolygon = closestTree.getConvexHull();
        if (getFinger().tapGameScreen(closestTreePolygon, "Chop down " + treeType.getObjectName())) {
            waitUntilFinishedChopping(treeType, closestTree);
        } else {
            // add to ignore
            TREE_BLACKLIST.put(closestTree.getWorldPosition(), System.currentTimeMillis());
        }
    }

    private List<RSObject> getVisibleTrees(List<RSObject> trees, WorldPosition myPosition) {
        return trees.stream()
                .filter(rsObject -> {
                    WorldPosition position = rsObject.getWorldPosition();
                    if (position == null) {
                        return false; // Skip if the position is null
                    }
                    Long time = TREE_BLACKLIST.get(position);
                    if (time != null) {
                        if ((System.currentTimeMillis() - time) < BLACKLIST_TIMEOUT) {
                            return false;
                        } else {
                            TREE_BLACKLIST.remove(position);
                        }
                    }
                    if (!selectedArea.contains(position)) {
                        return false;
                    }
                    Polygon treePolygon = rsObject.getConvexHull();
                    if (treePolygon == null || (treePolygon = treePolygon.getResized(0.5)) == null) {
                        return false; // Skip if the polygon is null
                    }
                    if (getWidgetManager().insideGameScreenFactor(treePolygon, List.of(ChatboxComponent.class)) < 0.5) {
                        return false;
                    }
                    if (!rsObject.canReach() || rsObject.getTileDistance(position) > 15) {
                        return false;
                    }
                    return true;
                })
                // sort by distance to player
                .sorted((a, b) -> {
                    double distA = a.getWorldPosition().distanceTo(myPosition);
                    double distB = b.getWorldPosition().distanceTo(myPosition);
                    return Double.compare(distA, distB);
                })
                .toList();
    }

    public List<RSObject> getActiveTrees(Tree treeType, List<RSObject> trees) {
        List<RSObject> activeTrees = new ArrayList<>();
        for (RSObject tree : trees) {
            Polygon treePolygon = tree.getConvexHull();
            if (treePolygon == null || (treePolygon = treePolygon.getResized(0.5)) == null) {
                continue;
            }
            if (getWidgetManager().insideGameScreenFactor(treePolygon, List.of(ChatboxComponent.class)) < 0.5) {
                continue;
            }
            if (getPixelAnalyzer().findPixels(treePolygon, treeType.getCluster()).size() < 20) {
                continue;
            }
            activeTrees.add(tree);
        }
        return activeTrees;
    }

    private void waitUntilFinishedChopping(Tree treeType, RSObject tree) {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(ChopBurn.class, "World position is null");
            return;
        }
        // wait until stopped moving
        if (tree.getTileDistance(worldPosition) > 1) {
            log(ChopBurn.class, "Waiting until we've started moving...");
            submitTask(() -> getLastPositionChangeMillis() < 600, Utils.random(1000, 3000));
            log(ChopBurn.class, "Waiting until we've stopped moving...");
            submitTask(() -> {
                WorldPosition worldPosition_ = getWorldPosition();
                long lastPositionChange = getLastPositionChangeMillis();
                return lastPositionChange > 800 && tree.getTileDistance(worldPosition_) == 1;
            }, Utils.random(5000, 12000));
        }
        worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(ChopBurn.class, "World position is null after waiting for movement");
            return;
        }
        if (tree.getTileDistance(worldPosition) > 1) {
            log(ChopBurn.class, "We didn't reach the tree, returning...");
            return;
        }
        log(ChopBurn.class, "Waiting until we're finished chopping...");
        Timer animatingTimer = new Timer();
        submitHumanTask(() -> {
                    WorldPosition myPosition = getWorldPosition();
                    if (myPosition == null) {
                        log(ChopBurn.class, "Position is null");
                        return false;
                    }
                    Polygon treePolygon = tree.getConvexHull();
                    if (treePolygon == null || (treePolygon = treePolygon.getResized(0.5)) == null) {
                        return false; // Skip if the polygon is null
                    }
                    if (getPixelAnalyzer().findPixels(treePolygon, treeType.getCluster()).size() < 20) {
                        log(ChopBurn.class, "No pixels found for treeType " + treeType.getObjectName() + ", returning...");
                        return true;
                    }
                    ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(Set.of(treeType.getLogID()));
                    if (inventorySnapshot == null) {
                        log(ChopBurn.class, "Failed to get inventory snapshot.");
                        return false;
                    }
                    if (inventorySnapshot.isFull()) {
                        log(ChopBurn.class, "Inventory is full");
                        return true; // Stop the script if inventory is full
                    }

                    if (animatingTimer.timeElapsed() > animationTimeout) {
                        log(ChopBurn.class, "Animation timeout");
                        animationTimeout = random(2000, 4000);
                        return true;
                    } else if (getPixelAnalyzer().isPlayerAnimating(0.25)) {
                        animatingTimer.reset();
                    }
                    return false;
                }, Utils.random(55000, 95000)
        );
        if (RandomUtils.uniformRandom(0, 3) == 0) {
            // Randomly wait before chopping again
            submitTask(() -> false, RandomUtils.exponentialRandom(1500, 700, 6000));
        }
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{9776};
    }

    @Override
    public void onPaint(Canvas c) {
        FontMetrics metrics = c.getFontMetrics(ARIEL);
        int padding = 5;

        List<String> lines = new ArrayList<>();
        lines.add("Task: " + (task == null ? "None" : task));

        lines.add("Woodcutting level: " + woodcuttingLevel + " Firemaking level: " + firemakingLevel);
        lines.add("Next level check in: " + LEVEL_CHECK_TIMER.getRemainingTimeFormatted());

        // Calculate max width and total height
        int maxWidth = 0;
        for (String line : lines) {
            int w = metrics.stringWidth(line);
            if (w > maxWidth) maxWidth = w;
        }
        int totalHeight = metrics.getHeight() * lines.size();
        int drawX = 10;
        // Draw background rectangle
        c.fillRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.BLACK.getRGB(), 0.8);
        c.drawRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.GREEN.getRGB());
        // Draw text lines
        int drawY = 40;
        for (String line : lines) {
            int color = Color.WHITE.getRGB();
            c.drawText(line, drawX, drawY += metrics.getHeight(), color, ARIEL);
        }
    }
}
