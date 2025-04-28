package com.osmb.script.motherloadmine;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.triangle.Triangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.motherloadmine.component.SackOverlay;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "Motherload mine", author = "Joe", version = 1.0, description = "", skillCategory = SkillCategory.MINING)
public class MotherloadMine extends Script {

    public static final int RESPAWN_CIRCLE_HEIGHT = 160;
    public static final int BLACKLIST_TIMEOUT = 15000;
    public static final int[] ITEM_IDS_TO_NOT_DEPOSIT = {
            ItemID.PAYDIRT, ItemID.BRONZE_PICKAXE, ItemID.IRON_PICKAXE,
            ItemID.STEEL_PICKAXE, ItemID.BLACK_PICKAXE, ItemID.MITHRIL_PICKAXE,
            ItemID.ADAMANT_PICKAXE, ItemID.RUNE_PICKAXE, ItemID.DRAGON_PICKAXE,
            ItemID.DRAGON_PICKAXE_OR, ItemID.CRYSTAL_PICKAXE, ItemID.INFERNAL_PICKAXE,
            ItemID.INFERNAL_PICKAXE_OR
    };
    public static final Predicate<RSObject> LADDER_QUERY = (rsObject) -> {
        String name = rsObject.getName();
        if (name == null) {
            return false;
        }
        if (!name.equalsIgnoreCase("ladder")) {
            return false;
        }
        return rsObject.canReach();
    };
    private static final RectangleArea SOUTH_AREA = new RectangleArea(3733, 5645, 27, 10, 0);
    private static final RectangleArea WEST_AREA = new RectangleArea(3728, 5655, 10, 17, 0);
    private static final java.awt.Font ARIEL = java.awt.Font.getFont("Ariel");
    private static final int[] ORES = new int[]{ItemID.COAL, ItemID.GOLD_ORE, ItemID.MITHRIL_ORE, ItemID.ADAMANTITE_ORE, ItemID.RUNITE_ORE};
    private static final SearchablePixel FLOWING_WATER_PIXEL = new SearchablePixel(-6707525, new SingleThresholdComparator(3), ColorModel.HSL);
    private static final PolyArea TOP_FLOOR_AREA = new PolyArea(List.of(
            new WorldPosition(3751, 5678, 0),
            new WorldPosition(3751, 5677, 0),
            new WorldPosition(3754, 5675, 0),
            new WorldPosition(3756, 5675, 0),
            new WorldPosition(3756, 5674, 0),
            new WorldPosition(3757, 5673, 0),
            new WorldPosition(3759, 5672, 0),
            new WorldPosition(3760, 5671, 0),
            new WorldPosition(3760, 5669, 0),
            new WorldPosition(3761, 5668, 0),
            new WorldPosition(3762, 5667, 0),
            new WorldPosition(3763, 5667, 0),
            new WorldPosition(3763, 5666, 0),
            new WorldPosition(3762, 5665, 0),
            new WorldPosition(3761, 5664, 0),
            new WorldPosition(3761, 5663, 0),
            new WorldPosition(3761, 5660, 0),
            new WorldPosition(3761, 5655, 0),
            new WorldPosition(3765, 5655, 0),
            new WorldPosition(3766, 5655, 0),
            new WorldPosition(3766, 5657, 0),
            new WorldPosition(3763, 5660, 0),
            new WorldPosition(3763, 5662, 0),
            new WorldPosition(3763, 5664, 0),
            new WorldPosition(3765, 5666, 0),
            new WorldPosition(3764, 5669, 0),
            new WorldPosition(3764, 5670, 0),
            new WorldPosition(3763, 5671, 0),
            new WorldPosition(3763, 5674, 0),
            new WorldPosition(3765, 5676, 0),
            new WorldPosition(3766, 5677, 0),
            new WorldPosition(3766, 5678, 0),
            new WorldPosition(3765, 5679, 0),
            new WorldPosition(3764, 5680, 0),
            new WorldPosition(3764, 5683, 0),
            new WorldPosition(3763, 5684, 0),
            new WorldPosition(3761, 5685, 0),
            new WorldPosition(3757, 5685, 0),
            new WorldPosition(3751, 5685, 0),
            new WorldPosition(3748, 5685, 0),
            new WorldPosition(3747, 5686, 0),
            new WorldPosition(3744, 5686, 0),
            new WorldPosition(3744, 5687, 0),
            new WorldPosition(3743, 5687, 0),
            new WorldPosition(3742, 5686, 0),
            new WorldPosition(3740, 5686, 0),
            new WorldPosition(3739, 5685, 0),
            new WorldPosition(3737, 5685, 0),
            new WorldPosition(3736, 5686, 0),
            new WorldPosition(3735, 5686, 0),
            new WorldPosition(3733, 5686, 0),
            new WorldPosition(3733, 5683, 0),
            new WorldPosition(3734, 5682, 0),
            new WorldPosition(3735, 5681, 0),
            new WorldPosition(3739, 5681, 0),
            new WorldPosition(3740, 5682, 0),
            new WorldPosition(3744, 5682, 0),
            new WorldPosition(3745, 5681, 0),
            new WorldPosition(3746, 5681, 0),
            new WorldPosition(3747, 5680, 0),
            new WorldPosition(3750, 5680, 0),
            new WorldPosition(3750, 5679, 0)
    ));
    private static final Area[] MINING_MINE_AREAS = new Area[]{SOUTH_AREA, WEST_AREA, TOP_FLOOR_AREA};
    private static final RectangleArea LADDER_AREA = new RectangleArea(3753, 5670, 2, 3, 0);
    /**
     * This is used as a failsafe to temporarily block interacting with a vein if the respawn circle isn't visible but the object is.
     * For example. The object is half on the game screen, but the respawn circle isn't (covered by a UI component etc.)
     */
    private final Map<WorldPosition, Long> objectPositionBlacklist = new HashMap<>();
    private boolean fixWaterWheelFlag = false;
    private boolean forceCollectFlag = false;
    private UIResultList<ItemSearchResult> payDirt;
    private int amountChangeTimeout;
    private int animationTimeout;
    private Optional<Integer> freeSlotsResult;
    private int nextSpaceLeftDeposit;
    private SackOverlay sackOverlay;
    private MineArea selectedMineArea;
    private Task task;
    private Integer spaceLeft;
    private Integer deposited;
    private boolean firstTimeBack = false;

    public MotherloadMine(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int poll() {
        task = decideTask();
        if (task == null) {
            return 0;
        }
        log(getClass().getSimpleName(), "Executing task: " + task);
        executeTask(task);
        return 0;
    }

    @Override
    public void onStart() {
        this.nextSpaceLeftDeposit = random(0, 20);
        this.amountChangeTimeout = random(9500, 16000);
        this.animationTimeout = random(3000, 5000);
        this.sackOverlay = new SackOverlay(this);
        this.selectedMineArea = MineArea.TOP;
    }

    private Task decideTask() {
        if (getWidgetManager().getBank().isVisible()) {
            return Task.HANDLE_BANK;
        }
        spaceLeft = (Integer) sackOverlay.getValue(SackOverlay.SPACE_LEFT);
        deposited = (Integer) sackOverlay.getValue(SackOverlay.DEPOSITED);
        if (spaceLeft == null || deposited == null) {
            log(MotherloadMine.class, "Problem reading sack overlay... (space left: " + spaceLeft + ") (deposited: " + deposited + ")");
            return null;
        }

        freeSlotsResult = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
        payDirt = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.PAYDIRT);
        UIResult<ItemSearchResult> ore = getItemManager().findItem(getWidgetManager().getInventory(), ORES);
        if (freeSlotsResult.isEmpty() || payDirt.isNotVisible() || ore.isNotVisible()) {
            // if inventory is not visible
            return null;
        }
        if (deposited == 0 && ore.isNotFound()) {
            forceCollectFlag = false;
        } else if (shouldCollect(spaceLeft) || forceCollectFlag) {
            forceCollectFlag = true;
            return Task.COLLECT;
        }

        int oresToMine = spaceLeft - payDirt.size();

        if (freeSlotsResult.get() <= 0 || oresToMine <= 0) {
            // If we have too much payDirt drop it
            if (oresToMine < 0) {
                // too many ores drop some
                return Task.DROP_PAYDIRT;
            }
            // If NO free slots AND we have paydirt in our inv deposit to mine cart
            else if (payDirt.isFound()) {
                return Task.DEPOSIT_PAY_DIRT;
            } else {
                // if no spaces & no paydirt, open the bank to deposit and make room...
                return Task.OPEN_BANK;
            }
        } else {
            if (ore.isFound()) {
                return Task.OPEN_BANK;
            }
            WorldPosition myPosition = getWorldPosition();
            if (myPosition == null) {
                return null;
            }
            if (needsToWalkToMineArea(myPosition)) {
                return Task.WALK_TO_VEIN_AREA;
            }
            return Task.MINE_VEIN;
        }
    }

    private void executeTask(Task task) {
        switch (task) {
            case MINE_VEIN -> mineVein();
            case DEPOSIT_PAY_DIRT -> depositPayDirt();
            case DROP_PAYDIRT -> dropPayDirt();
            case COLLECT -> collectPayDirt();
            case WALK_TO_VEIN_AREA -> walkToVeinArea();
            case HANDLE_BANK -> handleBank();
            case OPEN_BANK -> openBank();
        }
    }

    private void openBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        Predicate<RSObject> bankQuery = gameObject -> {
            if (gameObject.getName() == null || !gameObject.getName().equalsIgnoreCase("bank chest")) {
                return false;
            }
            return gameObject.canReach();
        };
        List<RSObject> banksFound = getObjectManager().getObjects(bankQuery);
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact("use")) {
            // if we fail to interact with the bank
            return;
        }
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
        submitTask(() -> {
            WorldPosition position = getWorldPosition();
            if (position == null) {
                return false;
            }
            if (pos.get() == null || !position.equals(pos.get())) {
                positionChangeTimer.get().reset();
                pos.set(position);
            }

            return getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000);
    }

    private void handleBank() {
        if (!getWidgetManager().getBank().depositAll(ITEM_IDS_TO_NOT_DEPOSIT)) {
            return;
        }
        getWidgetManager().getBank().close();
    }

    private void walkToVeinArea() {
        WalkConfig.Builder builder = new WalkConfig.Builder().tileRandomisationRadius(3);
        builder.breakCondition(() -> {
            WorldPosition myPosition = getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            return selectedMineArea.getArea().contains(myPosition);
        });
        if (selectedMineArea == MineArea.TOP) {
            Optional<RSObject> ladder = getObjectManager().getObject(LADDER_QUERY);
            if (ladder.isPresent()) {
                RSObject object = ladder.get();
                if (object.interact("Climb")) {
                    submitHumanTask(() -> {
                        WorldPosition worldPosition = getWorldPosition();
                        if (worldPosition == null) {
                            return false;
                        }
                        return TOP_FLOOR_AREA.contains(worldPosition);
                    }, 1000);
                }
            } else {
                // walk to ladder
                getWalker().walkTo(LADDER_AREA.getRandomPosition());
                return;
            }
        }
        getWalker().walkTo(selectedMineArea.getArea().getRandomPosition(), builder.build());
    }

    private void collectPayDirt() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(MotherloadMine.class, "Position is null...");
            return;
        }
        if (TOP_FLOOR_AREA.contains(myPosition)) {
            Optional<RSObject> ladder = getObjectManager().getObject(LADDER_QUERY);
            if (ladder.isPresent()) {
                RSObject object = ladder.get();
                if (object.interact("Climb")) {
                    submitHumanTask(() -> {
                        WorldPosition worldPosition = getWorldPosition();
                        if (worldPosition == null) {
                            return false;
                        }
                        return !TOP_FLOOR_AREA.contains(worldPosition);
                    }, 1000);
                    return;
                }
            } else {
                log(MotherloadMine.class, "Can't find ladder in scene...");
                return;
            }
        }
        if (freeSlotsResult.get() <= 0 || deposited == 0) {
            openBank();
            return;
        }
        Optional<RSObject> sack = getSack();
        if (sack.isEmpty()) {
            log(MotherloadMine.class, "Can't find object Sack inside our loaded scene.");
            return;
        }
        MenuHook menuHook = menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                if (entry.getRawText().equalsIgnoreCase("search sack")) {
                    return entry;
                }
            }
            return null;
        };
        if (sack.get().interact(null, menuHook)) {
            submitHumanTask(() -> {
                Optional<Integer> presentFreeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
                return presentFreeSlots.filter(integer -> integer < freeSlotsResult.get()).isPresent();
            }, random(15000, 25000));
        }
    }

    private void dropPayDirt() {
        payDirt.getRandom().interact("Drop");
    }

    private void depositPayDirt() {
        Optional<RSObject> hopper = getHopper();
        if (hopper.isEmpty()) {
            log(MotherloadMine.class, "Can't find the hopper in our loaded scene...");
            return;
        }

        if (!hopper.get().interact("deposit")) {
            // failed to interact with the hopper
            return;
        }
        // wait until paydirt is deposited
        int payDirtBefore = payDirt.size();
        submitHumanTask(() -> {
            if (getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
                UIResult<String> dialogueText = getWidgetManager().getDialogue().getText();
                if (dialogueText.isFound()) {
                    if (dialogueText.get().toLowerCase().startsWith("the machine will") || dialogueText.get().toLowerCase().startsWith("-")) {
                        fixWaterWheelFlag = true;
                    }
                }
            }

            Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
            boolean result = freeSlots.filter(integer -> integer > this.freeSlotsResult.get()).isPresent();
            if (result) {
                if (spaceLeft != null && spaceLeft - payDirtBefore <= 0) {
                    log(MotherloadMine.class, "Forcing collect.");
                    forceCollectFlag = true;
                }
            }
            return result;
        }, random(10000, 15000));
    }

    private Optional<RSObject> getHopper() {
        return getObjectManager().getObject(rsObject -> {
            if (rsObject.getName() == null || !rsObject.getName().equalsIgnoreCase("hopper")) {
                return false;
            }
            return rsObject.canReach();
        });
    }

    private Optional<RSObject> getSack() {
        return getObjectManager().getObject(rsObject -> {
            // name needs to be default name
            if (rsObject.getName() == null || !rsObject.getName().equalsIgnoreCase("empty sack")) {
                return false;
            }
            return rsObject.canReach();
        });
    }

    private void mineVein() {
        List<RSObject> veins = getObjectManager().getObjects(rsObject -> {
            WorldPosition position = rsObject.getWorldPosition();
            Long time = objectPositionBlacklist.get(position);
            if (time != null) {
                if ((System.currentTimeMillis() - time) < BLACKLIST_TIMEOUT) {
                    return false;
                } else {
                    objectPositionBlacklist.remove(position);
                }
            }
            return rsObject.getName() != null && rsObject.getName().equalsIgnoreCase("depleted vein") && rsObject.canReach();
        });
        List<RSObject> activeVeinsOnScreen = getActiveVeinsOnScreen(veins);
        if (activeVeinsOnScreen.isEmpty()) {
            // walk to some veins
            veins.removeAll(activeVeinsOnScreen);
            RSObject closestOffScreen = (RSObject) getUtils().getClosest(veins);
            if (closestOffScreen == null) {
                log(MotherloadMine.class, "Closest object off screen is null.");
                return;
            }
            getWalker().walkTo(closestOffScreen);
            return;
        }
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return;
        }
        log(MotherloadMine.class, "Active veins on screen: " + activeVeinsOnScreen.size());
        activeVeinsOnScreen.sort(Comparator.comparingDouble(
                vein -> vein.getWorldPosition().distanceTo(myPosition))
        );
        int index = 0;
        if (firstTimeBack) {
            index = Math.min(activeVeinsOnScreen.size() - 1, 3);
        }
        RSObject closestVein = activeVeinsOnScreen.get(index);
        // draw the active veins
        getScreen().queueCanvasDrawable("ActiveVeins", canvas -> {
            for (RSObject vein : activeVeinsOnScreen) {
                if (vein.getFaces() == null) {
                    continue;
                }
                Color color = Color.GREEN;
                if (vein.equals(closestVein)) {
                    Polygon polygon = vein.getConvexHull();
                    if (polygon != null) {
                        getScreen().getDrawableCanvas().fillPolygon(polygon, Color.GREEN.getRGB(), 0.3);
                    }
                    color = Color.CYAN;
                }
                for (Triangle triangle : vein.getFaces()) {
                    getScreen().getDrawableCanvas().drawPolygon(triangle.getXPoints(), triangle.getYPoints(), 3, color.getRGB());
                }
            }
        });

        // interact with the object
        // We aren't using RSObject#interact here because it tries multiple times to interact if the given menu entry options aren't visible.
        Polygon veinPolygon = closestVein.getConvexHull();
        if (veinPolygon == null) {
            return;
        }

        MenuHook veinMenuHook = menuEntries -> {
            boolean foundDepleted = false;
            for (MenuEntry entry : menuEntries) {
                String rawText = entry.getRawText();
                if (rawText.equalsIgnoreCase("mine ore vein")) {
                    return entry;
                } else if (rawText.equalsIgnoreCase("examine depleted vein")) {
                    log(MotherloadMine.class, "Depleted vein found");
                    foundDepleted = true;
                }
            }
            if (foundDepleted) {
                log(MotherloadMine.class, "Adding to blacklist");
                WorldPosition veinPosition = closestVein.getWorldPosition();
                objectPositionBlacklist.put(veinPosition, System.currentTimeMillis());
            }
            return null;
        };

        if (!getFinger().tapGameScreen(veinPolygon, veinMenuHook)) {
            // if we fail to interact with the object
            return;
        }

        long positionChangeTime = getLastPositionChangeMillis();
        if (closestVein.getTileDistance() > 1) {
            // if not in interactable distance, wait a little so we start moving.
            // This is just to detect a dud action (when you click a menu entry but nothing happens)
            if (!submitTask(() -> closestVein.getTileDistance() <= 1 || getLastPositionChangeMillis() < positionChangeTime, random(2000, 4000))) {
                // if we don't move after interacting and we aren't next to the object
                log(MotherloadMine.class, "We're not moving... trying again.");
                return;
            }
        }
        // wait until respawn circle appears in closestVein's position, or any other general conditions met
        AtomicInteger previousAmount = new AtomicInteger(payDirt.size());
        Timer amountChangeTimer = new Timer();
        Timer animatingTimer = new Timer();
        WorldPosition veinPosition = closestVein.getWorldPosition();
        log(MotherloadMine.class, "Entering waiting task...");
        submitHumanTask(() -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            int tileDistance = closestVein.getTileDistance();
            if (tileDistance > 1) {
                // still traversing to the rock
                log(MotherloadMine.class, "Still walking to rock. Tile distance: " + tileDistance);
                return getLastPositionChangeMillis() > 2000;
            }
            // If the amount of resources in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                log(MotherloadMine.class, "Amount change timeout");
                this.amountChangeTimeout = random(14000, 25000);
                return true;
            }
            if (animatingTimer.timeElapsed() > animationTimeout) {
                log(MotherloadMine.class, "Animation timeout");
                this.animationTimeout = random(3000, 5000);
            }

            Polygon polygon = getSceneProjector().getTileCube(myPosition_, 100);
            if (polygon == null) {
                return false;
            }
            if (getPixelAnalyzer().isAnimating(0.25, polygon)) {
                animatingTimer.reset();
            }

            List<WorldPosition> respawnCircles = getRespawnCirclePositions();
            if (respawnCircles.contains(veinPosition)) {
                log(MotherloadMine.class, "Respawn circle detected in the objects position.");
                return true;
            }

            Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
            if (!freeSlots.isPresent()) {
                // the component we are searching for the item is not visible (in this case, the inventory)
                return false;
            }
            if (freeSlots.get() == 0) {
                // full inventory
                return true;
            }
            // listen for inv items
            UIResultList<ItemSearchResult> payDirt = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.PAYDIRT);
            if (payDirt.isNotVisible()) {
                // the component we are searching for the item is not visible (in this case, the inventory)
                log(MotherloadMine.class, "Inventory is not visible");
                return false;
            }
            int payDirtAmount = payDirt.size();
            if (payDirtAmount > previousAmount.get()) {
                // gained paydirt, reset item amount change timer
                log(MotherloadMine.class, "Gained Paydirt!");
                amountChangeTimer.reset();
                previousAmount.set(payDirtAmount);
            }
            return false;
        }, random(60000, 90000));


    }

    private List<WorldPosition> getRespawnCirclePositions() {
        List<Rectangle> respawnCircles = getPixelAnalyzer().findRespawnCircles();
        return getUtils().getWorldPositionForRespawnCircles(respawnCircles, RESPAWN_CIRCLE_HEIGHT);
    }

    public List<RSObject> getActiveVeinsOnScreen(List<RSObject> veins) {
        List<RSObject> activeVeinsOnScreen = new ArrayList<>(veins);
        List<Rectangle> respawnCircles = getPixelAnalyzer().findRespawnCircles();
        List<WorldPosition> circlePositions = getUtils().getWorldPositionForRespawnCircles(respawnCircles, RESPAWN_CIRCLE_HEIGHT);
        // remove objects what aren't interactable on screen OR if there is a respawn circle in that position
        activeVeinsOnScreen.removeIf(rsObject -> !rsObject.isInteractableOnScreen() || circlePositions.contains(rsObject.getWorldPosition()));
        return activeVeinsOnScreen;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{14936};
    }

    @Override
    public void onPaint(Canvas c) {
        c.fillRect(5, 40, 200, 150, Color.BLACK.getRGB(), 0.7);
        c.drawRect(5, 40, 200, 150, Color.BLACK.getRGB());
        int y = 40;
        c.drawText("Task: " + (task == null ? "None" : task), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Space left: " + (spaceLeft == null ? 0 : spaceLeft), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        c.drawText("Amount deposited: " + (deposited == null ? 0 : deposited), 10, y += 20, Color.WHITE.getRGB(), ARIEL);
        for (Map.Entry<WorldPosition, Long> blackListedPosition : objectPositionBlacklist.entrySet()) {
            Polygon polygon = getSceneProjector().getTileCube(blackListedPosition.getKey(), 150);
            if (polygon == null) {
                continue;
            }
            c.fillPolygon(polygon, Color.red.getRGB(), 0.7);
        }
    }

    private boolean shouldCollect(int freeSackSpaces) {
        return payDirt.isEmpty() && freeSackSpaces <= (selectedMineArea != MineArea.TOP ? 14 : 0);
    }

    private boolean needsToWalkToMineArea(WorldPosition worldPosition) {
        for (Area area : MINING_MINE_AREAS) {
            if (area == null) {
                continue;
            }
            if (area.contains(worldPosition)) {
                return false;
            }
        }
        return true;
    }

    enum Task {
        MINE_VEIN,
        WALK_TO_VEIN_AREA,
        DEPOSIT_PAY_DIRT,
        COLLECT,
        DROP_PAYDIRT,
        HANDLE_BANK,
        OPEN_BANK
    }

    enum MineArea {
        TOP(TOP_FLOOR_AREA),
        BOTTOM_SOUTH(SOUTH_AREA),
        BOTTOM_WEST(WEST_AREA);

        private final Area area;

        MineArea(Area area) {
            this.area = area;
        }

        public Area getArea() {
            return area;
        }
    }
}
