package com.osmb.script.motherloadmine;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.shape.triangle.Triangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.ImagePanel;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.motherloadmine.overlay.SackOverlay;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "Motherload mine", author = "Joe", version = 1.0, description = "", skillCategory = SkillCategory.MINING)
public class MotherloadMine extends Script {

    public static final int RESPAWN_CIRCLE_HEIGHT = 160;
    public static final int BLACKLIST_TIMEOUT = 15000;
    public static final Set<Integer> ITEM_IDS_TO_NOT_DEPOSIT = new HashSet<>(Set.of(
            ItemID.PAYDIRT, ItemID.BRONZE_PICKAXE, ItemID.IRON_PICKAXE,
            ItemID.STEEL_PICKAXE, ItemID.BLACK_PICKAXE, ItemID.MITHRIL_PICKAXE,
            ItemID.ADAMANT_PICKAXE, ItemID.RUNE_PICKAXE, ItemID.DRAGON_PICKAXE,
            ItemID.DRAGON_PICKAXE_OR, ItemID.CRYSTAL_PICKAXE, ItemID.INFERNAL_PICKAXE,
            ItemID.INFERNAL_PICKAXE_OR
    ));
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
    private static final RectangleArea WATER_INNER_AREA = new RectangleArea(3744, 5661, 3, 10, 0);
    private static final WorldPosition CRATE_POSITION = new WorldPosition(3752, 5674, 0);
    private static final PolyArea TOP_FLOOR_ACCESSIBLE_AREA = new PolyArea(List.of(
            new WorldPosition(3758, 5675, 0),
            new WorldPosition(3758, 5674, 0),
            new WorldPosition(3762, 5674, 0),
            new WorldPosition(3762, 5669, 0),
            new WorldPosition(3761, 5668, 0),
            new WorldPosition(3761, 5670, 0),
            new WorldPosition(3760, 5671, 0),
            new WorldPosition(3759, 5672, 0),
            new WorldPosition(3758, 5673, 0),
            new WorldPosition(3757, 5673, 0),
            new WorldPosition(3756, 5674, 0),
            new WorldPosition(3755, 5674, 0),
            new WorldPosition(3754, 5675, 0),
            new WorldPosition(3753, 5676, 0),
            new WorldPosition(3752, 5676, 0),
            new WorldPosition(3752, 5677, 0),
            new WorldPosition(3751, 5678, 0),
            new WorldPosition(3750, 5679, 0),
            new WorldPosition(3750, 5681, 0),
            new WorldPosition(3747, 5681, 0),
            new WorldPosition(3746, 5683, 0),
            new WorldPosition(3747, 5683, 0),
            new WorldPosition(3748, 5683, 0),
            new WorldPosition(3749, 5683, 0),
            new WorldPosition(3749, 5684, 0),
            new WorldPosition(3750, 5685, 0),
            new WorldPosition(3751, 5684, 0),
            new WorldPosition(3753, 5684, 0),
            new WorldPosition(3754, 5684, 0),
            new WorldPosition(3754, 5681, 0),
            new WorldPosition(3753, 5680, 0),
            new WorldPosition(3754, 5679, 0),
            new WorldPosition(3755, 5679, 0),
            new WorldPosition(3756, 5679, 0),
            new WorldPosition(3756, 5676, 0),
            new WorldPosition(3757, 5676, 0),
            new WorldPosition(3758, 5676, 0)));
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.PAYDIRT, ItemID.HAMMER));
    /**
     * This is used as a failsafe to temporarily block interacting with a vein if the respawn circle isn't visible but the object is.
     * For example. The object is half on the game screen, but the respawn circle isn't (covered by a UI component etc.)
     */
    private final Map<WorldPosition, Long> objectPositionBlacklist = new HashMap<>();
    private ItemGroupResult inventorySnapshot;

    private boolean fixWaterWheelFlag = false;
    private boolean forceCollectFlag = false;
    private boolean firstTimeBack = false;

    private int amountChangeTimeout;
    private int animationTimeout;
    private SackOverlay sackOverlay;
    private MineArea selectedMineArea;
    private Task task;
    private Integer spaceLeft;
    private Integer deposited;
    private int payDirtMined = 0;
    private List<LocalPosition> waterTiles;

    public MotherloadMine(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public int poll() {
        if (waterTiles == null) {
            waterTiles = WATER_INNER_AREA.getSurroundingPositions(this, 1);
        }

        task = decideTask();
        if (task == null) {
            return 0;
        }
        if ((task == Task.COLLECT || task == Task.HANDLE_BANK) && !firstTimeBack) {
            firstTimeBack = true;
        }
        log(getClass().getSimpleName(), "Executing task: " + task);
        executeTask(task);
        return 0;
    }

    @Override
    public void onStart() {
        this.amountChangeTimeout = random(9500, 16000);
        this.animationTimeout = random(3000, 5000);
        this.sackOverlay = new SackOverlay(this);
        this.selectedMineArea = MineArea.TOP;

        for (Integer ore : ORES) {
            ITEM_IDS_TO_RECOGNISE.add(ore);
        }

        for (int pickaxe : ITEM_IDS_TO_NOT_DEPOSIT) {
            if (ITEM_IDS_TO_RECOGNISE.contains(pickaxe)) {
                continue;
            }
            ITEM_IDS_TO_RECOGNISE.add(pickaxe);
        }
    }

    private Task decideTask() {
        if (getWidgetManager().getDepositBox().isVisible()) {
            return Task.HANDLE_BANK;
        }
        spaceLeft = (Integer) sackOverlay.getValue(SackOverlay.SPACE_LEFT);
        deposited = (Integer) sackOverlay.getValue(SackOverlay.DEPOSITED);
        if (spaceLeft == null || deposited == null) {
            log(MotherloadMine.class, "Problem reading sack overlay... (space left: " + spaceLeft + ") (deposited: " + deposited + ")");
            return null;
        }

        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            // inventory not visible
            log(MotherloadMine.class, "Inventory not visible...");
            return null;
        }

        if (fixWaterWheelFlag) {
            return Task.REPAIR_WHEEL;
        } else if (inventorySnapshot.contains(ItemID.HAMMER)) {
            return Task.DROP_HAMMER;
        }

        if (deposited == 0 && !inventorySnapshot.contains(ItemID.PAYDIRT)) {
            forceCollectFlag = false;
        } else if (shouldCollect(spaceLeft) || forceCollectFlag) {
            forceCollectFlag = true;
            return Task.COLLECT;
        }

        int oresToMine = spaceLeft - inventorySnapshot.getAmount(ItemID.PAYDIRT);

        if (inventorySnapshot.isFull() || oresToMine <= 0) {
            // If we have too much payDirt drop it
            if (oresToMine < 0) {
                // too many ores drop some
                return Task.DROP_PAYDIRT;
            }
            // If NO free slots AND we have paydirt in our inv deposit to mine cart
            else if (inventorySnapshot.contains(ItemID.PAYDIRT)) {
                return Task.DEPOSIT_PAY_DIRT;
            } else {
                // if no spaces & no paydirt, open the bank to deposit and make room...
                return Task.OPEN_BANK;
            }
        } else {
            if (inventorySnapshot.containsAny(ORES)) {
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
            case REPAIR_WHEEL -> repairWheel();
            case DROP_HAMMER -> dropHammer();
        }
    }

    private void dropHammer() {
        if (inventorySnapshot.contains(ItemID.HAMMER)) {
            inventorySnapshot.getItem(ItemID.HAMMER).interact("Drop");
        }
    }

    private void repairWheel() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return;
        }
        if (TOP_FLOOR_AREA.contains(myPosition)) {
            // climb down ladder if we are on the top floor
            climbDownLadder();
            return;
        }

        // scan the water to see if its flowing
        scanWater();

        if (!fixWaterWheelFlag) {
            return;
        }
        if (!inventorySnapshot.contains(ItemID.HAMMER)) {
            // grab a hammer
            takeHammer();
            return;
        }
        // find water wheel objects
        RSObject brokenStrut = getObjectManager().getClosestObject("broken strut");

        if (brokenStrut == null) {
            log(MotherloadMine.class, "Can't find Strut in scene...");
            return;
        }
        log(MotherloadMine.class, "Interact with water wheel...");
        boolean interactResult = brokenStrut.interact(null, menuEntries -> {
            log(MotherloadMine.class, menuEntries.toString());
            for (MenuEntry entry : menuEntries) {
                String entryText = entry.getRawText().toLowerCase();
                if (entryText.startsWith("hammer broken strut")) {
                    return entry;
                } else if (entryText.startsWith("examine strut")) {
                    fixWaterWheelFlag = false;
                }
            }
            return null;
        });

        if (interactResult) {
            submitHumanTask(() -> {
                // scan the water to check if repaired
                scanWater();
                // flag will switch to false inside scanWater method if water is flowing
                return !fixWaterWheelFlag;
            }, random(6000, 11000));
        }

    }

    private void takeHammer() {
        if (inventorySnapshot.isFull()) {
            // if no free slots, drop a paydirt
            dropPayDirt();
            return;
        }
        Optional<RSObject> crateOpt = getObjectManager().getObject(object -> object.getWorldPosition().equals(CRATE_POSITION));
        if (crateOpt.isEmpty()) {
            // walk to crate
            getWalker().walkTo(CRATE_POSITION, new WalkConfig.Builder().breakDistance(2).tileRandomisationRadius(2).build());
            return;
        }
        RSObject crate = crateOpt.get();
        if (!crate.interact("Search")) {
            return;
        }
        // wait for hammer to appear in the inventory
        submitHumanTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            return inventorySnapshot.contains(ItemID.HAMMER);
        }, random(5000, 8000));
    }

    private void openBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        Predicate<RSObject> bankQuery = gameObject -> {
            if (gameObject.getName() == null || !gameObject.getName().equalsIgnoreCase("bank deposit box")) {
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
        if (!object.interact("deposit")) {
            // if we fail to interact with the bank
            return;
        }
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
        submitHumanTask(() -> {
            WorldPosition position = getWorldPosition();
            if (position == null) {
                return false;
            }
            if (pos.get() == null || !position.equals(pos.get())) {
                positionChangeTimer.get().reset();
                pos.set(position);
            }

            return getWidgetManager().getDepositBox().isVisible() || positionChangeTimer.get().timeElapsed() > 3000;
        }, 15000);
    }

    private void handleBank() {
        getWidgetManager().getDepositBox().drawComponents(getScreen().getDrawableCanvas());
        if (!getWidgetManager().getDepositBox().depositAll(ITEM_IDS_TO_NOT_DEPOSIT)) {
            log(MotherloadMine.class, "Failed depositing items...");
            return;
        }
        log(MotherloadMine.class, "Closing deposit box...");
        getWidgetManager().getDepositBox().close();
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
            if (climbUpLadder()) {
                return;
            }
        }
        getWalker().walkTo(selectedMineArea.getArea().getRandomPosition(), builder.build());
    }

    private boolean climbUpLadder() {
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
                }, random(7000, 12000));
                return true;
            }
        } else {
            // walk to ladder
            getWalker().walkTo(LADDER_AREA.getRandomPosition());
            return true;
        }
        return false;
    }

    private void collectPayDirt() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(MotherloadMine.class, "Position is null...");
            return;
        }
        if (TOP_FLOOR_AREA.contains(myPosition)) {
            if (!TOP_FLOOR_ACCESSIBLE_AREA.contains(myPosition)) {
                log(MotherloadMine.class, "Outside accessible area, stopping script as rocks are not handled atm.");
                stop();
                return;
            }
            climbDownLadder();
            return;
        }
        if (inventorySnapshot.isFull() || deposited == 0) {
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
            int initialSlotsFree = inventorySnapshot.getFreeSlots();
            submitHumanTask(() -> {
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    // not visible
                    return false;
                }
                return inventorySnapshot.getFreeSlots() < initialSlotsFree;
            }, random(15000, 25000));
        }
    }

    private boolean climbDownLadder() {
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
                }, random(6000, 12000));
                return true;
            }
        } else {
            log(MotherloadMine.class, "Can't find ladder in scene...");
            return true;
        }
        return false;
    }

    private void dropPayDirt() {
        if (inventorySnapshot.contains(ItemID.PAYDIRT)) {
            inventorySnapshot.getRandomItem(ItemID.PAYDIRT).interact("Drop");
        }
    }

    private void depositPayDirt() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(MotherloadMine.class, "Position is null...");
            return;
        }
        if (TOP_FLOOR_AREA.contains(myPosition) && !TOP_FLOOR_ACCESSIBLE_AREA.contains(myPosition)) {
            log(MotherloadMine.class, "Outside accessible area, stopping script as rocks are not handled atm.");
            stop();
            return;
        }
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
        int payDirtBefore = inventorySnapshot.getAmount(ItemID.PAYDIRT);
        submitHumanTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                // inventory not visible
                return false;
            }
            int payDirtNow = inventorySnapshot.getAmount(ItemID.PAYDIRT);

            if (payDirtNow < payDirtBefore) {
                if (spaceLeft != null && spaceLeft - payDirtBefore <= 0) {
                    log(MotherloadMine.class, "Forcing collect.");
                    forceCollectFlag = true;
                }
                return true;
            }
            if (getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
                UIResult<String> dialogueText = getWidgetManager().getDialogue().getText();
                if (dialogueText.isFound()) {
                    if (dialogueText.get().toLowerCase().startsWith("you've already got some pay-dirt in the machine")) {
                        fixWaterWheelFlag = true;
                    }
                    // only fix in this case if our sack is full
                    else if (dialogueText.get().toLowerCase().startsWith("the machine will need to be repaired") && forceCollectFlag) {
                        fixWaterWheelFlag = true;
                    }
                    return true;
                }
            }
            return false;
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

    private void scanWater() {
        List<LocalPosition> waterTiles = WATER_INNER_AREA.getSurroundingPositions(this, 1);
        boolean found = false;
        for (LocalPosition worldPosition : waterTiles) {
            Polygon polygon = getSceneProjector().getTilePoly(worldPosition, true);
            if (polygon == null || getWidgetManager().insideGameScreenFactor(polygon, Collections.emptyList()) < 0.4) {
                // not on screen or blocked by ui
                continue;
            }

            List<Point> pixels = getPixelAnalyzer().findPixelsOnGameScreen(polygon, FLOWING_WATER_PIXEL);
            boolean contains = !pixels.isEmpty();
            log(MotherloadMine.class, "");
            int color = contains ? Color.GREEN.getRGB() : Color.RED.getRGB();
            getScreen().getDrawableCanvas().drawPolygon(polygon, color, 1);
            getScreen().getDrawableCanvas().fillPolygon(polygon, color, 0.5);
            if (contains && !found) {
                found = true;
                // we continue the loop just to draw the tiles as it looks fancy
                new ImagePanel(getScreen().getDrawableCanvas().toImage().toBufferedImage()).showInFrame("");
            }
        }
        if (found) {
            log("Water appears to be flowing... disabling fix water wheel flag");
            fixWaterWheelFlag = false;
        }
    }

    private void mineVein() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(MotherloadMine.class, "World position is null...");
            return;
        }
        List<RSObject> veins = getVeins();
        List<RSObject> activeVeinsOnScreen = getActiveVeinsOnScreen(veins, myPosition);
        log(MotherloadMine.class, "Active veins on screen: " + activeVeinsOnScreen.size());
        if (activeVeinsOnScreen.isEmpty()) {
            // walk to the closest vein which isn't on screen
            walkToClosestVeinOffScreen(veins, activeVeinsOnScreen);
            return;
        }

        int index = 0;
        if (firstTimeBack) {
            // first time running back to the mining area, we choose a random between the first 3 closest. this helps a lot with the top floor, as if we always get the closest first time around
            // it tends to go towards the south side, with this it makes it more random
            index = Math.min(activeVeinsOnScreen.size() - 1, 3);
        }
        RSObject closestVein = activeVeinsOnScreen.get(index);
        // draw the active veins
        drawActiveVeins(activeVeinsOnScreen, closestVein);

        // interact with the object
        // We aren't using RSObject#interact here because it tries multiple times to interact if the given menu entry options aren't visible.
        Polygon veinPolygon = closestVein.getConvexHull();
        if (veinPolygon == null) {
            return;
        }

        MenuHook veinMenuHook = getVeinMenuHook(closestVein);
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
        waitUntilFinishedMining(closestVein);
    }

    private void walkToClosestVeinOffScreen(List<RSObject> veins, List<RSObject> activeVeinsOnScreen) {
        veins.removeAll(activeVeinsOnScreen);
        RSObject closestOffScreen = (RSObject) getUtils().getClosest(veins);
        if (closestOffScreen == null) {
            log(MotherloadMine.class, "Closest object off screen is null.");
            return;
        }
        getWalker().walkTo(closestOffScreen);
    }

    private void waitUntilFinishedMining(RSObject closestVein) {
        // wait until respawn circle appears in closestVein's position, or any other general conditions met
        AtomicInteger previousAmount = new AtomicInteger(inventorySnapshot.getAmount(ItemID.PAYDIRT));
        Timer amountChangeTimer = new Timer();
        Timer animatingTimer = new Timer();
        WorldPosition veinPosition = closestVein.getWorldPosition();
        log(MotherloadMine.class, "Entering waiting task...");
        AtomicBoolean failed = new AtomicBoolean(false);
        submitHumanTask(() -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            int tileDistance = closestVein.getTileDistance();
            if (tileDistance > 1) {
                // still traversing to the rock
                amountChangeTimer.reset();
                log(MotherloadMine.class, "Still walking to rock. Tile distance: " + tileDistance);
                if (getLastPositionChangeMillis() > 2000) {
                    failed.set(true);
                } else {
                    return false;
                }
            }


            // If the amount of resources in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                log(MotherloadMine.class, "Amount change timeout");
                this.amountChangeTimeout = random(14000, 22000);
                failed.set(true);
                return true;
            }
            if (animatingTimer.timeElapsed() > animationTimeout) {
                log(MotherloadMine.class, "Animation timeout");
                this.animationTimeout = random(3000, 5000);
                failed.set(true);
                return true;
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

            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                // inv full
                return false;
            }

            if (inventorySnapshot.isFull()) {
                return true;
            }
            spaceLeft = (Integer) sackOverlay.getValue(SackOverlay.SPACE_LEFT);
            deposited = (Integer) sackOverlay.getValue(SackOverlay.DEPOSITED);
            if (spaceLeft == null || deposited == null) {
                log(MotherloadMine.class, "Problem reading sack overlay... (space left: " + spaceLeft + ") (deposited: " + deposited + ")");
                return false;
            }
            int payDirtAmount = inventorySnapshot.getAmount(ItemID.PAYDIRT);
            if (spaceLeft - payDirtAmount <= 0) {
                // sack full
                return true;
            }
            if (payDirtAmount > previousAmount.get()) {
                // gained paydirt, reset item amount change timer
                log(MotherloadMine.class, "Gained Paydirt!");
                int amountGained = payDirtAmount - previousAmount.get();
                payDirtMined += amountGained;
                amountChangeTimer.reset();
                previousAmount.set(payDirtAmount);
            }
            return false;
        }, random(60000, 90000));

        if (!failed.get()) {
            // extra response time so we aren't instantly reacting
            submitTask(() -> false, random(0, 4000));
        }
    }

    private MenuHook getVeinMenuHook(RSObject closestVein) {
        return menuEntries -> {
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
    }

    private void drawActiveVeins(List<RSObject> activeVeinsOnScreen, RSObject closestVein) {
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
    }

    private List<RSObject> getVeins() {
        return getObjectManager().getObjects(rsObject -> {
            WorldPosition position = rsObject.getWorldPosition();
            Long time = objectPositionBlacklist.get(position);
            if (time != null) {
                if ((System.currentTimeMillis() - time) < BLACKLIST_TIMEOUT) {
                    return false;
                } else {
                    objectPositionBlacklist.remove(position);
                }
            }
            if (selectedMineArea == MineArea.TOP) {
                if (!TOP_FLOOR_ACCESSIBLE_AREA.contains(rsObject.getWorldPosition())) {
                    return false;
                }
            }
            return rsObject.getName() != null && rsObject.getName().equalsIgnoreCase("depleted vein") && rsObject.canReach();
        });
    }

    private List<WorldPosition> getRespawnCirclePositions() {
        List<Rectangle> respawnCircles = getPixelAnalyzer().findRespawnCircles();
        return getUtils().getWorldPositionForRespawnCircles(respawnCircles, RESPAWN_CIRCLE_HEIGHT);
    }

    public List<RSObject> getActiveVeinsOnScreen(List<RSObject> veins, WorldPosition myPosition) {
        List<RSObject> activeVeinsOnScreen = new ArrayList<>(veins);
        List<Rectangle> respawnCircles = getPixelAnalyzer().findRespawnCircles();
        List<WorldPosition> circlePositions = getUtils().getWorldPositionForRespawnCircles(respawnCircles, RESPAWN_CIRCLE_HEIGHT);
        // remove objects what aren't interactable on screen OR if there is a respawn circle in that position
        activeVeinsOnScreen.removeIf(rsObject -> !rsObject.isInteractableOnScreen() || circlePositions.contains(rsObject.getWorldPosition()));
        // sort by distance
        activeVeinsOnScreen.sort(Comparator.comparingDouble(
                vein -> vein.getWorldPosition().distanceTo(myPosition))
        );
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
        c.drawText("Paydirt mined: " + payDirtMined, 10, y += 20, Color.WHITE.getRGB(), ARIEL);
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
        return !inventorySnapshot.contains(ItemID.PAYDIRT) && freeSackSpaces <= (selectedMineArea != MineArea.TOP ? 14 : 0);
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

    enum MineResult {
        FAILED,
        SUCCEEDED
    }

    enum Task {
        MINE_VEIN,
        WALK_TO_VEIN_AREA,
        DEPOSIT_PAY_DIRT,
        COLLECT,
        DROP_PAYDIRT,
        HANDLE_BANK,
        OPEN_BANK,
        REPAIR_WHEEL,
        DROP_HAMMER;
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
