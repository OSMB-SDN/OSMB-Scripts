package com.osmb.script.agility;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.Position;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.script.agility.courses.ardougne.Ardougne;
import com.osmb.script.agility.courses.pollnivneach.Pollnivneach;
import com.osmb.script.agility.ui.javafx.UI;
import javafx.scene.Scene;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "AIO Agility", author = "Joe", version = 1.0, description = "Provides support over a range of agility courses.", skillCategory = SkillCategory.AGILITY)
public class AIOAgility extends Script {
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open"};
    private static final ToleranceComparator MOG_TOLERANCE_COMPARATOR = new ChannelThresholdComparator(2, 2, 2);
    private static final SearchablePixel[] MOG_PIXELS_RED = new SearchablePixel[]{
            new SearchablePixel(-6741740, MOG_TOLERANCE_COMPARATOR, ColorModel.HSL),
            new SearchablePixel(-5826292, MOG_TOLERANCE_COMPARATOR, ColorModel.HSL),
            new SearchablePixel(-7004140, MOG_TOLERANCE_COMPARATOR, ColorModel.HSL),
    };
    private static final SearchablePixel[] MOG_PIXELS_GOLD = new SearchablePixel[]{
            new SearchablePixel(-4414953, MOG_TOLERANCE_COMPARATOR, ColorModel.HSL),
            new SearchablePixel(-5336052, MOG_TOLERANCE_COMPARATOR, ColorModel.HSL),
            new SearchablePixel(-3888359, MOG_TOLERANCE_COMPARATOR, ColorModel.HSL),
    };
    private static final int[] ITEMS_TO_IGNORE = new int[]{ItemID.MARK_OF_GRACE, ItemID.LAW_RUNE, ItemID.AIR_RUNE, ItemID.FIRE_RUNE};
    private static final WorldPosition ARDY_MOG_POS = new WorldPosition(2657, 3318, 3);
    private static final WorldPosition POLL_MOG_POS = new WorldPosition(3359, 2983, 2);
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.MARK_OF_GRACE));
    private static boolean handlingObstacle = false;
    private final Stopwatch eatBlockTimer = new Stopwatch();
    private Course selectedCourse;
    private int[] foodItemID = null;
    private int hitpointsToEat = -1;
    private int eatHigh;
    private int eatLow;
    private int nextRunActivate;
    private int noMovementTimeout = RandomUtils.weightedRandom(3000, 6000);
    private MultiConsumable multiConsumable = null;
    private ItemGroupResult inventorySnapshot;

    public AIOAgility(Object object) {
        super(object);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName The name of the obstacle
     * @param menuOption   The name of the menu option to select
     * @param end          The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param timeout      The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(AIOAgility core, String obstacleName, String
            menuOption, Object end, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, 1, timeout);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(AIOAgility core, String obstacleName, String
            menuOption, Object end, int interactDistance, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, interactDistance, true, timeout);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param canReach         If {@code false} then this method will avoid using {@link RSObject#canReach()} when querying objects for the obstacle.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(AIOAgility core, String obstacleName, String
            menuOption, Object end, int interactDistance, boolean canReach, int timeout) {
        return handleObstacle(core, obstacleName, menuOption, end, interactDistance, canReach, timeout, null);
    }

    /**
     * Handles an agility obstacle, will run to & interact using the specified {@param menuOption} then sleep until we reach then {@param endPosition}
     *
     * @param core
     * @param obstacleName     The name of the obstacle
     * @param menuOption       The name of the menu option to select
     * @param end              The finishing {@link WorldPosition} or {@link Area} of the obstacle interaction
     * @param interactDistance The tile distance away from the object which it can be interacted from.
     * @param canReach         If {@code false} then this method will avoid using {@link RSObject#canReach()} when querying objects for the obstacle.
     * @param timeout          The timeout when to the {@param endPosition}, method will return {@link ObstacleHandleResponse#TIMEOUT} if the specified timeout is surpassed
     * @param objectBaseTile   The base tile of the object. If null we avoid this check.
     * @return
     */
    public static ObstacleHandleResponse handleObstacle(AIOAgility core, String obstacleName, String
            menuOption, Object end, int interactDistance, boolean canReach, int timeout, WorldPosition objectBaseTile) {
        // cache hp, we determine if we failed the obstacle via hp decrementing
        UIResult<Integer> hitpoints = core.getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
        Optional<RSObject> result = core.getObjectManager().getObject(gameObject -> {

            if (gameObject.getName() == null || gameObject.getActions() == null) return false;

            if (!gameObject.getName().equalsIgnoreCase(obstacleName)) {
                return false;
            }

            if (objectBaseTile != null) {
                if (!objectBaseTile.equals(gameObject.getWorldPosition())) {
                    return false;
                }
            }
            if (!canReach) {
                return true;
            }

            return gameObject.canReach(interactDistance);
        });
        if (result.isEmpty()) {
            core.log(AIOAgility.class.getSimpleName(), "ERROR: Obstacle (" + obstacleName + ") does not exist with criteria.");
            return ObstacleHandleResponse.OBJECT_NOT_IN_SCENE;
        }
        RSObject object = result.get();
        if (object.interact(menuOption)) {
            AIOAgility.handlingObstacle = true;
            core.log(AIOAgility.class.getSimpleName(), "Interacted successfully, sleeping until conditions are met...");
            Timer noMovementTimer = new Timer();
            AtomicReference<WorldPosition> previousPosition = new AtomicReference<>();
            if (core.submitHumanTask(() -> {
                WorldPosition currentPos = core.getWorldPosition();
                if (currentPos == null) {
                    return false;
                }
                // check if we take damage
                if (hitpoints.isFound()) {
                    UIResult<Integer> newHitpointsResult = core.getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
                    if (newHitpointsResult.isFound()) {
                        if (hitpoints.get() > newHitpointsResult.get()) {
                            return true;
                        }
                    }
                }
                // check for being stood still
                if (previousPosition.get() != null) {
                    if (currentPos.equals(previousPosition.get())) {
                        if (noMovementTimer.timeElapsed() > core.noMovementTimeout) {
                            core.noMovementTimeout = RandomUtils.weightedRandom(2000, 6000);
                            return true;
                        }
                    } else {
                        noMovementTimer.reset();
                    }
                } else {
                    noMovementTimer.reset();
                }
                previousPosition.set(currentPos);

                RSTile tile = core.getSceneManager().getTile(core.getWorldPosition());
                Polygon poly = tile.getTileCube(120);
                if (core.getPixelAnalyzer().isAnimating(0.1, poly)) {
                    return false;
                }
                if (end instanceof Area area) {
                    return area.contains(currentPos);
                } else if (end instanceof Position pos) {
                    return currentPos.equals(pos);
                }
                return false;
            }, timeout)) {
                handlingObstacle = false;
                return ObstacleHandleResponse.SUCCESS;
            } else {
                handlingObstacle = false;
                return ObstacleHandleResponse.TIMEOUT;
            }
        } else {
            core.log(AIOAgility.class.getSimpleName(), "ERROR: Failed interacting with obstacle (" + obstacleName + ").");
            handlingObstacle = false;
            return ObstacleHandleResponse.FAILED_INTERACTION;
        }
    }

    public static boolean handleMOG(AIOAgility core) {
        UIResultList<WorldPosition> groundItems = core.getWidgetManager().getMinimap().getItemPositions();
        if (!groundItems.isFound()) {
            return false;
        }

        WorldPosition myPosition = core.getWorldPosition();
        for (WorldPosition groundItem : groundItems) {
            core.log("Ground item found");
            RSTile tile = core.getSceneManager().getTile(groundItem);
            if (tile == null) {
                core.log(AIOAgility.class.getSimpleName(), "Tile is null.");
                continue;
            }

            if (!tile.isOnGameScreen(ChatboxComponent.class)) {
                core.log(AIOAgility.class.getSimpleName(), "WARNING: Tile containing item is not on screen, reduce your zoom level.");
                continue;
            }

            // handle ardy mog tile as we can't reach this one
            if (tile.getWorldPosition().equals(ARDY_MOG_POS) || tile.getWorldPosition().equals(POLL_MOG_POS)) {
                if (!Ardougne.AREA_3.contains(myPosition) && !Pollnivneach.AREA_6.contains(myPosition)) {
                    continue;
                }
            } else if (!tile.canReach()) {
                continue;
            }

            Polygon tilePoly = tile.getTilePoly();
            if (tilePoly == null) {
                core.log(AIOAgility.class.getSimpleName(), "Tile poly is null.");
                continue;
            }
            core.log("Checking ground item for MOG");
            tilePoly = tilePoly.getResized(0.8);
            // if the tile contains all pixels

            if (core.getPixelAnalyzer().findPixel(tilePoly, MOG_PIXELS_GOLD) == null || core.getPixelAnalyzer().findPixel(tilePoly, MOG_PIXELS_RED) == null) {
                continue;
            }

            ItemGroupResult inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            // check if we have free spaces
            if (inventorySnapshot.isFull() && !inventorySnapshot.contains(ItemID.MARK_OF_GRACE)) {
                core.log(AIOAgility.class.getSimpleName(), "MOG Found but no inventory slots free in the inventory...");
                if (core.foodItemID != null) {
                    ItemSearchResult food = inventorySnapshot.getRandomItem(core.foodItemID);
                    if (food != null) {
                        core.log(AIOAgility.class.getSimpleName(), "Eating food to make space for MOG!");
                        if (!food.interact("eat", "drink")) {
                            return false;
                        }
                    } else {
                        core.log(AIOAgility.class.getSimpleName(), "No room to pick up MOG.");
                        core.stop();
                    }
                } else {
                    core.log(AIOAgility.class, "Inventory is full, cannot pick up MOG!");
                    return false;
                }
            }
            int previousAmount = inventorySnapshot.getAmount(ItemID.MARK_OF_GRACE);
            core.log(AIOAgility.class.getSimpleName(), "Attempting to interact with MOG");
            if (core.getFinger().tapGameScreen(tilePoly, "Take mark of grace")) {
                // sleep until we picked up the mark
                core.submitHumanTask(() -> {
                    ItemGroupResult inventorySnapshot1 = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                    if (inventorySnapshot1 == null) {
                        return false;
                    }
                    if (inventorySnapshot1.getAmount(ItemID.MARK_OF_GRACE) > previousAmount) {
                        core.log(AIOAgility.class.getSimpleName(), "Successfully picked up MOG!");
                        return true;
                    }
                    return false;
                }, 7000);
            }
            return true;
        }

        return false;
    }

    @Override
    public void onStart() {
        // fxml loading
//            FXMLLoader loader = new FXMLLoader(AIOAgility.class.getResource("/ui.fxml"));
//            // initializing the controller
//            popupController = new Controller();
//            loader.setController(popupController);
//            Parent layout = loader.load();
//
//            // initialise our fxml's components actions
//            popupController.init();

        UI ui = new UI();
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Settings", false);

        // set the selected course
        this.selectedCourse = ui.selectedCourse();
        this.eatHigh = ui.getEatHigh();
        this.eatLow = ui.getEatLow();
        this.hitpointsToEat = random(eatLow, eatHigh);
        this.nextRunActivate = random(30, 70);
        if (ui.shouldUseFood()) {
            int foodItemID = ui.getFoodID();

            this.multiConsumable = MultiConsumable.getMultiConsumable(foodItemID);
            if (multiConsumable != null) {
                this.foodItemID = multiConsumable.getItemIDs();
            } else if (foodItemID != -1) {
                this.foodItemID = new int[]{foodItemID};
            }

            for (Integer foodID : this.foodItemID) {
                ITEM_IDS_TO_RECOGNISE.add(foodID);
            }
        } else {
            this.foodItemID = null;
        }

    }

    @Override
    public int poll() {
        // handle eating food & banking
        if (getWidgetManager().getBank().isVisible()) {
            if (foodItemID != null) {
                handleBankInterface();
            } else {
                getWidgetManager().getBank().close();
            }
            return 0;
        }
        WorldPosition position = getWorldPosition();
        if (position == null) {
            log(getClass().getSimpleName(), "Position is null.");
            return 0;
        }

        if (foodItemID != null) {
            UIResult<Integer> hpOpt = getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
            if (!hpOpt.isFound()) {
                log(getClass().getSimpleName(), "Hitpoints orb not visible...");
                return 0;
            }
            // get a snapshot of the inventory
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return 0;
            }
            // walk to bank to restock if on the ground floor, stop script if course doesn't have a bank
            if (!inventorySnapshot.containsAny(foodItemID) && position.getPlane() == 0) {
                Area bankArea = selectedCourse.getBankArea();
                if (bankArea != null) {
                    //restock
                    return navigateToBank();
                } else {
                    stop();
                    log(getClass().getSimpleName(), "Ran out of food, stopping script...");
                    return 0;
                }
            } else if(hpOpt.get() <= hitpointsToEat && eatBlockTimer.hasFinished()) {
                eatFood();
                return 0;
            }
        }

        UIResult<Boolean> runEnabled = getWidgetManager().getMinimapOrbs().isRunEnabled();
        if (runEnabled.isFound()) {
            UIResult<Integer> runEnergyOpt = getWidgetManager().getMinimapOrbs().getRunEnergy();
            int runEnergy = runEnergyOpt.orElse(-1);
            if (!runEnabled.get() && runEnergy > nextRunActivate) {
                log(getClass().getSimpleName(), "Enabling run");
                if (!getWidgetManager().getMinimapOrbs().setRun(true)) {
                    return 0;
                }
                nextRunActivate = random(30, 70);
            }
        }

        if (position.getPlane() > 0 && handleMOG(this)) {
            return 0;
        }
        return selectedCourse.poll(this);
    }

    private void eatFood() {
            // eat food
            ItemSearchResult foodToEat;
            if (multiConsumable != null) {
                foodToEat = MultiConsumable.getSmallestConsumable(multiConsumable, inventorySnapshot.getAllOfItems(foodItemID));
            } else {
                foodToEat = inventorySnapshot.getRandomItem(foodItemID);
            }
            foodToEat.interact();
            eatBlockTimer.reset(3000);
            hitpointsToEat = random(eatLow, eatHigh);
    }

    private int navigateToBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");
        // find bank objects
        Predicate<RSObject> bankPredicate = gameObject -> {
            // if object has no name
            if (gameObject.getName() == null) {
                return false;
            }
            // has no interact options (eg. bank, open etc.)
            if (gameObject.getActions() == null) {
                return false;
            }

            if (!Arrays.stream(BANK_NAMES).anyMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
                return false;
            }

            // if no actions contain bank or open
            if (!Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
                return false;
            }
            // final check is if the object is reachable
            return gameObject.canReach();
        };

        // apply the predicate to find banks
        List<RSObject> banksFound = getObjectManager().getObjects(bankPredicate);

        // no banks found, walk to bank area
        if (banksFound.isEmpty()) {
            getWalker().walkTo(selectedCourse.getBankArea().getRandomPosition());
        } else {
            // get closest bank object
            RSObject object = (RSObject) getUtils().getClosest(banksFound);
            // interact with the bank object
            if (!object.interact(BANK_ACTIONS)) return 200;
            // wait for the bank interface to open
            submitHumanTask(() -> getWidgetManager().getBank().isVisible(), 10000);
        }
        return 0;
    }

    /**
     * Handles restocking food from the bank
     */
    private void handleBankInterface() {
        if (this.foodItemID == null) {
            getWidgetManager().getBank().close();
            return;
        }
        Set<Integer> itemsToIgnore = new HashSet<>();
        for (int itemToIgnore_ : ITEMS_TO_IGNORE) {
            itemsToIgnore.add(itemToIgnore_);
        }
        for (int foodItemID : foodItemID) {
            itemsToIgnore.add(foodItemID);
        }
        if (!getWidgetManager().getBank().depositAll(itemsToIgnore)) {
            return;
        }
        ItemGroupResult bankSnapshot = getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (bankSnapshot == null || inventorySnapshot == null) {
            return;
        }
        if (inventorySnapshot.isFull()) {
            getWidgetManager().getBank().close();
        } else {
            if (!bankSnapshot.containsAny(foodItemID)) {
                log(getClass().getSimpleName(), "No food left in the bank, stopping script...");
                stop();
            } else {
                getWidgetManager().getBank().withdraw(foodItemID[0], Integer.MAX_VALUE);
            }
        }

    }

    @Override
    public boolean canBreak() {
        return !handlingObstacle;
    }

    @Override
    public boolean canHopWorlds() {
        return !handlingObstacle;
    }

    @Override
    public int[] regionsToPrioritise() {
        if (selectedCourse == null) {
            return new int[0];
        }
        return selectedCourse.regions();
    }

    @Override
    public void onPaint(Canvas c) {
        if (selectedCourse == null) return;
        selectedCourse.onPaint(c);
    }
}
