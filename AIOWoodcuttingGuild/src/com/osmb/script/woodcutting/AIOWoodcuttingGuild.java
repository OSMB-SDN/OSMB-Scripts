package com.osmb.script.woodcutting;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.trackers.experiencetracker.XPTracker;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.Utils;
import com.osmb.api.visual.PixelAnalyzer;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.woodcutting.data.AreaManager;
import com.osmb.script.woodcutting.data.Tree;
import com.osmb.script.woodcutting.javafx.ScriptOptions;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ScriptDefinition(name = "AIO Woodcutting Guild", description = "A script to chop trees in the Woodcutting Guild.", version = 1.0, author = "Joe", skillCategory = SkillCategory.WOODCUTTING)
public class AIOWoodcuttingGuild extends Script {

    static final long BLACKLIST_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.TINDERBOX, ItemID.KNIFE));
    private static final Set<Integer> LOGS = Set.of(ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS, ItemID.TEAK_LOGS, ItemID.MAPLE_LOGS, ItemID.MAHOGANY_LOGS, ItemID.YEW_LOGS, ItemID.MAGIC_LOGS, ItemID.REDWOOD_LOGS);
    private static final Map<WorldPosition, Long> TREE_BLACKLIST = new HashMap<>();
    private static final Set<Integer> ITEM_IDS_TO_NOT_DEPOSIT = Set.of(ItemID.TINDERBOX, ItemID.KNIFE, ItemID.BRONZE_AXE, ItemID.IRON_AXE, ItemID.STEEL_AXE, ItemID.BLACK_AXE, ItemID.MITHRIL_AXE, ItemID.ADAMANT_AXE,
            ItemID.RUNE_AXE, ItemID.DRAGON_AXE, ItemID.DRAGON_AXE_OR, ItemID.DRAGON_AXE_OR_30352, ItemID.CRYSTAL_AXE, ItemID.CRYSTAL_AXE_23862, ItemID.INFERNAL_AXE, ItemID.INFERNAL_AXE_OR, ItemID.INFERNAL_AXE_OR_30347,
            ItemID.BRONZE_FELLING_AXE, ItemID.IRON_FELLING_AXE, ItemID.STEEL_FELLING_AXE, ItemID.BLACK_FELLING_AXE, ItemID.MITHRIL_FELLING_AXE, ItemID.ADAMANT_FELLING_AXE, ItemID.RUNE_FELLING_AXE, ItemID.DRAGON_FELLING_AXE, ItemID.CRYSTAL_FELLING_AXE);
    private static final Font ARIAL = new Font("Arial", Font.PLAIN, 14);
    private final SearchableImage woodcuttingSprite;
    private Tree selectedTree = Tree.OAK;
    private boolean powerChop = false;
    private XPTracker xpTracker;
    private int logsChopped = 0;
    /**
     * Flag to indicate if this is the first time back from the bank. This is used to prevent the script being as repetitive by choosing one of the closest tree's instead of just the closest tree every time.
     */
    private boolean firstBack = false;

    public AIOWoodcuttingGuild(Object scriptCore) {
        super(scriptCore);
        woodcuttingSprite = new SearchableImage(214, this, new SingleThresholdComparator(15), ColorModel.RGB);
    }

    @Override
    public void onStart() {
        ScriptOptions ui = new ScriptOptions(this);
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        // show the ui
        getStageController().show(scene, "AIO Woodcutting guild Configuration", false);
        // apply ui settings
        selectedTree = ui.getSelectedTree();
        powerChop = ui.isDropSelected();

        ITEM_IDS_TO_RECOGNISE.addAll(LOGS);
    }

    @Override
    public int poll() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(AIOWoodcuttingGuild.class, "Position is null");
            return 0;
        }
        checkXP();
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(AIOWoodcuttingGuild.class, "Unable to snapshot inventory...");
            return 0;
        }
        if (getWidgetManager().getBank().isVisible()) {
            // handle bank interface
            handleBank();
        } else if (inventorySnapshot.isFull()) {
            if (powerChop) {
                // drop logs
                getWidgetManager().getInventory().dropItems(LOGS);
            } else {
                if (selectedTree == Tree.REDWOOD && AreaManager.REDWOOD_TREE_AREA.contains(myPosition)) {
                    // climb down the ladder if we are in the redwood tree area
                    climbDownLadder();
                    return 0;
                }
                // bank
                openBank();
            }
        } else {
            if (selectedTree == Tree.REDWOOD && !AreaManager.REDWOOD_TREE_AREA.contains(myPosition)) {
                // walk to redwood tree area
                log(AIOWoodcuttingGuild.class, "Walking to redwood tree area...");
                climbUpLadder();
                return 0;
            }
            // chop trees
            chopTrees();
        }
        return 0;
    }

    private void climbDownLadder() {
        RSObject ladder = getObjectManager().getClosestObject("rope ladder");
        if (ladder == null) {
            log(AIOWoodcuttingGuild.class, "Ladder is null");
            return;
        }
        if (ladder.interact("climb-down")) {
            // wait until we are no longer in the redwood tree area
            submitHumanTask(() -> {
                WorldPosition myPosition = getWorldPosition();
                if (myPosition == null) {
                    log(AIOWoodcuttingGuild.class, "Position is null");
                    return true;
                }
                return !AreaManager.REDWOOD_TREE_AREA.contains(myPosition);
            }, Utils.random(10000, 20000));
        } else {
            log(AIOWoodcuttingGuild.class, "Failed to interact with ladder");
        }

    }

    private void climbUpLadder() {
        RSObject ladder = getObjectManager().getClosestObject("rope ladder");
        if (ladder == null) {
            log(AIOWoodcuttingGuild.class, "Ladder is null, walking to ladder area...");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> getObjectManager().getClosestObject("rope ladder") != null);
            getWalker().walkTo(AreaManager.REDWOOD_TREE_AREA.getRandomPosition(), builder.build());
            return;
        }
        if (ladder.interact("climb-up")) {
            // wait until we are in the redwood tree area
            submitHumanTask(() -> {
                WorldPosition myPosition = getWorldPosition();
                if (myPosition == null) {
                    log(AIOWoodcuttingGuild.class, "Position is null");
                    return true;
                }
                return AreaManager.REDWOOD_TREE_AREA.contains(myPosition);
            }, Utils.random(10000, 20000));
        } else {
            log(AIOWoodcuttingGuild.class, "Failed to interact with ladder");
        }
    }

    private void handleBank() {
        // reset firstBack flag
        if (!firstBack) {
            firstBack = true;
        }
        // deposit all unwanted items
        log(AIOWoodcuttingGuild.class, "Depositing unwanted items...");
        if (!getWidgetManager().getBank().depositAll(ITEM_IDS_TO_NOT_DEPOSIT)) {
            // if we failed to deposit items, log and return
            log(AIOWoodcuttingGuild.class, "Failed depositing items...");
            return;
        }
        // at this point, we should only have items in the inventory that we want to keep, so close the bank
        log(AIOWoodcuttingGuild.class, "Closing bank...");
        getWidgetManager().getBank().close();
    }

    private void openBank() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(AIOWoodcuttingGuild.class, "Position is null");
            return;
        }
        RSObject bank = getBankChest();
        if (bank == null) {
            log(AIOWoodcuttingGuild.class, "Walking to bank area...");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> getBankChest() != null);
            getWalker().walkTo(AreaManager.BANK_AREA.getRandomPosition());
            return;
        }
        if (bank.interact("use")) {
            // sleep until bank is open
            long positionChangeTimeout = random(1000, 2500);
            submitHumanTask(() -> {
                WorldPosition myPosition_ = getWorldPosition();
                if (myPosition_ == null) {
                    log(AIOWoodcuttingGuild.class, "Position is null");
                    return false;
                }
                // add movement check as sometimes on rare occasions, actions seem to be consumed.
                // this could be seen as some form of bot detection to try and exploit static timeouts in scripts to measure the time it takes to re-interact
                // to avoid this, we just need to ensure the time to re-interact is random & not abnormally long every time.
                if (bank.distance(myPosition_) > 1 && getLastPositionChangeMillis() > positionChangeTimeout) {
                    log(AIOWoodcuttingGuild.class, "Not moving...");
                    return true;
                }
                return getWidgetManager().getBank().isVisible();
            }, Utils.random(10000, 20000));
        }
    }

    private RSObject getBankChest() {
        return getObjectManager().getRSObject(object -> {
            String name = object.getName();
            if (name == null || !name.equalsIgnoreCase("bank chest")) {
                return false;
            }
            return AreaManager.BANK_AREA.contains(object.getWorldPosition());
        });
    }

    private void chopTrees() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(AIOWoodcuttingGuild.class, "Position is null");
            return;
        }
        // get the all the tree's matching the selected tree type
        List<RSObject> trees = getObjectManager().getObjects(rsObject -> {
            String name = rsObject.getName();
            return name != null && name.equalsIgnoreCase(selectedTree.getObjectName()) && selectedTree.getTreeArea().contains(rsObject.getWorldPosition());
        });

        if (trees.isEmpty()) {
            // walk to tree area, this shouldn't really happen as the script should only run in the Woodcutting Guild
            log(AIOWoodcuttingGuild.class, "No tree's found in the area, walking to tree area...");
            getWalker().walkTo(selectedTree.getTreeArea().getRandomPosition());
            return;
        }

        // get all selected tree types visible on screen (list is already sorted by distance)
        List<RSObject> visibleTrees = getVisibleTrees(trees, myPosition);
        List<RSObject> activeTrees = getActiveTrees(selectedTree, visibleTrees);
        if (activeTrees.isEmpty()) {
            // if no trees are visible, walk to the closest tree off-screen
            // remove all active visible trees from the main tree list
            trees.removeAll(visibleTrees);
            // sort remaining trees by distance to player
            trees.sort(Comparator.comparingDouble(value -> value.distance(myPosition)));
            if (!trees.isEmpty()) {
                int index = 0;
                if (firstBack) {
                    index = Math.min(2, trees.size() - 1);
                }
                // if the tree list is not empty, walk to the closest tree off-screen, which will be the first element in the list
                RSObject closestOffScreenTree = trees.get(index);
                log("Closest treeType off screen: " + closestOffScreenTree.getName() + " at " + closestOffScreenTree.getWorldPosition());
                WalkConfig.Builder builder = new WalkConfig.Builder();
                builder.breakCondition(closestOffScreenTree::isInteractableOnScreen);
                builder.tileRandomisationRadius(3);
                getWalker().walkTo(closestOffScreenTree, builder.build());
            } else {
                // if the list is empty, walk to the tree area
                log(AIOWoodcuttingGuild.class, "Walking to tree area...");
                getWalker().walkTo(selectedTree.getTreeArea().getRandomPosition());
            }
            return;
        }
        int index = 0;
        if (firstBack) {
            index = Math.min(2, activeTrees.size() - 1);
            firstBack = false;
        }
        // if we have a tree on screen, lets chop it down
        RSObject closestTree = activeTrees.get(index);
        // draw active trees on the canvas & highlight the closest tree
        drawActiveTrees(activeTrees, closestTree);
        // get the convex hull of the closest tree
        Polygon closestTreePolygon = closestTree.getConvexHull();
        // interact with the closest tree
        String action = selectedTree == Tree.REDWOOD ? "cut" : "chop down";
        if (getFinger().tapGameScreen(closestTreePolygon, action + " " + selectedTree.getObjectName())) {
            // wait until we start chopping
            waitUntilFinishedChopping(selectedTree, closestTree);
        } else {
            // temporarily add to ignore list (which will be skipped when searching for trees next time polling) this is to handle a false positive
            TREE_BLACKLIST.put(closestTree.getWorldPosition(), System.currentTimeMillis());
        }
    }


    public List<RSObject> getActiveTrees(Tree treeType, List<RSObject> trees) {
        if (treeType.getCluster() == null) {
            // use respawn circle logic
            Map<RSObject, PixelAnalyzer.RespawnCircle> respawnCircleMap = getPixelAnalyzer().getRespawnCircleObjects(trees, PixelAnalyzer.RespawnCircleDrawType.TOP_CENTER, 0, 10);
            List<RSObject> treesCopy = new ArrayList<>(trees);
            treesCopy.removeIf(respawnCircleMap::containsKey);
            return treesCopy;
        } else {
            List<RSObject> activeTrees = new ArrayList<>();
            for (RSObject tree : trees) {
                Polygon treePolygon = tree.getConvexHull();
                if (treePolygon == null || (treePolygon = treePolygon.getResized(0.5)) == null) {
                    continue;
                }
                if (getWidgetManager().insideGameScreenFactor(treePolygon, List.of(ChatboxComponent.class)) < 0.5) {
                    continue;
                }
                if (getPixelAnalyzer().findPixels(treePolygon, treeType.getCluster()).size() < 10) {
                    continue;
                }
                activeTrees.add(tree);
            }
            return activeTrees;
        }
    }


    private void drawActiveTrees(List<RSObject> activeVisibleTrees, RSObject closestTree) {
        getScreen().queueCanvasDrawable("activeTrees", (canvas) -> {
            for (RSObject rsObject : activeVisibleTrees) {
                Polygon treePolygon = rsObject.getConvexHull();
                if (treePolygon != null) {
                    canvas.fillPolygon(treePolygon, Color.GREEN.getRGB(), 0.3);
                    int color = rsObject.equals(closestTree) ? Color.CYAN.getRGB() : Color.GREEN.getRGB();
                    canvas.drawPolygon(treePolygon, color, 1);
                }
            }
        });
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
                    if (!selectedTree.getTreeArea().contains(position)) {
                        return false;
                    }
                    Polygon treePolygon = rsObject.getConvexHull();
                    if (treePolygon == null || (treePolygon = treePolygon.getResized(0.5)) == null) {
                        return false; // Skip if the polygon is null
                    }
                    if (getWidgetManager().insideGameScreenFactor(treePolygon, List.of(ChatboxComponent.class)) < 0.5) {
                        return false;
                    }
                    return rsObject.canReach() && rsObject.getTileDistance(position) <= 15;
                })
                // sort by distance to player
                .sorted((a, b) -> {
                    double distA = a.getWorldPosition().distanceTo(myPosition);
                    double distB = b.getWorldPosition().distanceTo(myPosition);
                    return Double.compare(distA, distB);
                })
                .toList();
    }

    private void waitUntilFinishedChopping(Tree treeType, RSObject tree) {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(AIOWoodcuttingGuild.class, "Position is null");
            return;
        }
        // wait until stopped moving
        if (tree.getTileDistance(worldPosition) > 1) {
            log(AIOWoodcuttingGuild.class, "Waiting until we've started moving...");
            submitTask(() -> getLastPositionChangeMillis() < 600, Utils.random(1000, 3000));
            log(AIOWoodcuttingGuild.class, "Waiting until we've stopped moving...");
            submitTask(() -> {
                WorldPosition worldPosition_ = getWorldPosition();
                if (worldPosition_ == null) {
                    log(AIOWoodcuttingGuild.class, "Position is null");
                    return false;
                }
                long lastPositionChange = getLastPositionChangeMillis();
                return lastPositionChange > 800 && tree.getTileDistance(worldPosition_) == 1;
            }, Utils.random(5000, 12000));
        }
        if (tree.getTileDistance(worldPosition) > 1) {
            log(AIOWoodcuttingGuild.class, "We didn't reach the tree, returning...");
            return;
        }
        log(AIOWoodcuttingGuild.class, "Waiting until we're finished chopping...");
        submitHumanTask(() -> {
                    WorldPosition myPosition = getWorldPosition();
                    if (myPosition == null) {
                        log(AIOWoodcuttingGuild.class, "Position is null");
                        return false;
                    }
                    checkXP();
                    Polygon treePolygon = tree.getConvexHull();
                    if (treePolygon == null || (treePolygon = treePolygon.getResized(0.5)) == null) {
                        return false; // Skip if the polygon is null
                    }
                    if (treeType.getCluster() == null) {
                        Map<RSObject, PixelAnalyzer.RespawnCircle> results = getPixelAnalyzer().getRespawnCircleObjects(List.of(tree), PixelAnalyzer.RespawnCircleDrawType.TOP_CENTER, 0, 10);
                        if (!results.isEmpty()) {
                            // respawn circle found
                            log(AIOWoodcuttingGuild.class, "Found respawn circle!");
                            return true;
                        }
                    } else {
                        List<Point> pixels = getPixelAnalyzer().findPixels(treePolygon, treeType.getCluster());
                        if (pixels.size() < 10) {
                            log(AIOWoodcuttingGuild.class, "No pixels found for treeType " + treeType.getObjectName() + ", returning...");
                            return true;
                        }
                    }
                    ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(Set.of(treeType.getLogID()));
                    if (inventorySnapshot == null) {
                        log(AIOWoodcuttingGuild.class, "Failed to get inventory snapshot.");
                        return false;
                    }
                    if (inventorySnapshot.isFull()) {
                        log(AIOWoodcuttingGuild.class, "Inventory is full");
                        return true; // Stop the script if inventory is full
                    }
                    return false;
                }, selectedTree == Tree.REDWOOD ? Utils.random(220000, 380000) : Utils.random(55000, 95000), false, true
        );
        if (RandomUtils.uniformRandom(0, 3) == 0) {
            // Randomly wait before chopping again
            submitTask(() -> false, RandomUtils.exponentialRandom(1800, 700, 8000));
        }
    }

    private void checkXP() {
        Integer currentXP = getXpCounter();
        if (currentXP != null) {
            if (xpTracker == null) {
                xpTracker = new XPTracker(this, currentXP);
            } else {
                double xp = xpTracker.getXp();
                double gainedXP = currentXP - xp;
                if (gainedXP > 0) {
                    xpTracker.incrementXp(gainedXP);
                    logsChopped++;
                }
            }
        }
    }

    private Integer getXpCounter() {
        Rectangle bounds = getXPDropsBounds();
        if (bounds == null) {
            log(AIOWoodcuttingGuild.class, "Failed to get XP drops component bounds");
            return null;
        }
        boolean isWoodcutting = getImageAnalyzer().findLocation(bounds, woodcuttingSprite) != null;
        if (!isWoodcutting) {
            return null;
        }
        getScreen().getDrawableCanvas().drawRect(bounds, Color.RED.getRGB(), 1);
        String xpText = getOCR().getText(com.osmb.api.visual.ocr.fonts.Font.SMALL_FONT, bounds, -1).replaceAll("[^0-9]", "");
        if (xpText.isEmpty()) {
            return null;
        }
        return Integer.parseInt(xpText);
    }

    private Rectangle getXPDropsBounds() {
        XPDropsComponent xpDropsComponent = (XPDropsComponent) getWidgetManager().getComponent(XPDropsComponent.class);
        Rectangle bounds = xpDropsComponent.getBounds();
        if (bounds == null) {
            log(AIOWoodcuttingGuild.class, "Failed to get XP drops component bounds");
            return null;
        }
        ComponentSearchResult<Integer> result = xpDropsComponent.getResult();
        if (result.getComponentImage().getGameFrameStatusType() != 1) {
            log(AIOWoodcuttingGuild.class, "XP drops component is not open, opening it");
            getFinger().tap(bounds);
            boolean succeed = submitTask(() -> {
                ComponentSearchResult<Integer> result_ = xpDropsComponent.getResult();
                return result_ != null && result_.getComponentImage().getGameFrameStatusType() == 1;
            }, random(1500, 3000));
            bounds = xpDropsComponent.getBounds();
            if (!succeed || bounds == null) {
                return null;
            }
        }
        return new Rectangle(bounds.x - 140, bounds.y - 1, 119, 38);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{6198, 6454};
    }

    @Override
    public void onPaint(Canvas c) {
        FontMetrics metrics = c.getFontMetrics(ARIAL);
        int padding = 5;

        List<String> lines = new ArrayList<>();
        if (selectedTree != null) {
            lines.add("Selected tree: " + selectedTree.getObjectName());
        }
        if (xpTracker != null) {
            lines.add("Current XP: " + String.format("%,d", (long) xpTracker.getXp()));
            lines.add("XP Gained: " + String.format("%,d", (long) xpTracker.getXpGained()));
            lines.add("Xp per hour: " + String.format("%,d", (long) xpTracker.getXpPerHour(getStartTime())));
            lines.add("Logs Chopped: " + String.format("%,d", logsChopped));
        } else {
            lines.add("No XP tracker available.");
        }
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
            c.drawText(line, drawX, drawY += metrics.getHeight(), color, ARIAL);
        }
    }
}
