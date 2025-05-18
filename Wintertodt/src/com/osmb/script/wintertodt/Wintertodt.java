package com.osmb.script.wintertodt;

import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.ImageImport;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.wintertodt.ui.ScriptOptions;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@ScriptDefinition(name = "Wintertodt", author = "Joe", version = 1.0, skillCategory = SkillCategory.FIREMAKING, description = "")
public class Wintertodt extends Script {

    public static final int FIRST_MILESTONE_POINTS = 500;
    private static final List<Integer> REJUVENATION_POTION_IDS = List.of(ItemID.REJUVENATION_POTION_1, ItemID.REJUVENATION_POTION_2, ItemID.REJUVENATION_POTION_3, ItemID.REJUVENATION_POTION_4);
    private static final RectangleArea BOSS_AREA = new RectangleArea(1600, 3968, 63, 63, 0);
    private static final Font FONT = new Font("Serif", Font.BOLD, 24);
    private static final RectangleArea SAFE_AREA = new RectangleArea(1625, 3968, 10, 19, 0);
    private static final RectangleArea SOCIAL_SAFE_AREA = new RectangleArea(1626, 3980, 8, 7, 0);
    private static final WorldPosition[] REJUVENATION_CRATE_POSITIONS = new WorldPosition[]{new WorldPosition(1634, 3982, 0), new WorldPosition(1626, 3982, 0)};
    private static final WorldPosition BREWMA_POSITION = new WorldPosition(1635, 3986, 0);
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.BRUMA_HERB, ItemID.BRUMA_ROOT, ItemID.BRUMA_KINDLING, ItemID.REJUVENATION_POTION_UNF));
    private WintertodtOverlay overlay;
    private Brazier focusedBrazier;
    private Method method;
    private boolean checkedEquipment = false;
    private Task task;
    private Set<Equipment> missingEquipment = new HashSet<>();
    private Stopwatch potionDrinkCooldown = new Stopwatch();
    private Integer warmth;
    private Integer wintertodtEnergy;
    private int nextDrinkPercent;
    private int minDrinkPercent;
    private int maxDrinkPercent;
    private int fletchTimeout;
    private int brazierTimeout;
    private long chopRootsTimeout;
    private Stopwatch breakDelay;
    private int nextDoseRestock = 8;
    private int potionsToPrep;
    private int idleTimeout;
    private int foodItemID;
    private HealType healType;
    private FletchType fletchType;
    private ItemGroupResult inventorySnapshot;

    public Wintertodt(Object scriptCore) {
        super(scriptCore);
    }

    public static int calculateNextMilestone(int currentPoints) {
        if (currentPoints < 500) {
            return 500; // First milestone is always 500
        } else {
            // Calculate the next milestone as the next multiple of 250
            return ((currentPoints / 250) + 1) * 250;
        }
    }

    @Override
    public void onStart() {
        //UI
        ScriptOptions scriptOptions = new ScriptOptions(this);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");

        getStageController().show(scene, "Wintertodt settings", false);


        focusedBrazier = scriptOptions.getSelectedBrazier();
        fletchType = scriptOptions.getSelectedFletchType();
        healType = scriptOptions.getHealType();

        overlay = new WintertodtOverlay(this);
        method = Method.GROUP;
        minDrinkPercent = 60;
        maxDrinkPercent = 80;
        potionsToPrep = random(4, 6);
        idleTimeout = random(1500, 3000);

        nextDrinkPercent = random(minDrinkPercent, maxDrinkPercent);
        brazierTimeout = random(5000, 7000);
        fletchTimeout = random(5000, 7000);
        chopRootsTimeout = random(6000, 13000);

        ITEM_IDS_TO_RECOGNISE.addAll(REJUVENATION_POTION_IDS);

        for (Equipment equipment : Equipment.values()) {
            for (int item : equipment.getItemIds()) {
                ITEM_IDS_TO_RECOGNISE.add(item);
            }
        }
    }

    @Override
    public int poll() {
        log(Wintertodt.class, "Deciding task...");
        this.task = decideTask(method);
        log(Wintertodt.class, "Executing task: " + task);
        if (task == null) {
            return 0;
        }
        executeTask(task);
        return 0;
    }

    @Override
    public void onPaint(Canvas c) {
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{6461, 12850};
    }

    private Task decideTask(Method method) {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(Wintertodt.class, "Position is null.");
            return null;
        }

        // walk to the boss area if we aren't inside
        if (!BOSS_AREA.contains(worldPosition)) {
            return Task.WALK_TO_BOSS_AREA;
        }

        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(Wintertodt.class, "Inventory not visible");
            return null;
        }
        if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
            log(Wintertodt.class, "failed to unselect item...");
            return null;
        }
        if (!checkedEquipment) {
            return Task.CHECK_EQUIPMENT;
        }
        if (!missingEquipment.isEmpty()) {
            return Task.GET_EQUIPMENT;
        }
        if (isDueToBreak()) {
            if (breakDelay != null && breakDelay.hasFinished()) {
                if (!SAFE_AREA.contains(worldPosition)) {
                    return Task.WALK_TO_SAFE_AREA;
                }
            }
        }

        if (!inventorySnapshot.containsAny(new HashSet<>(REJUVENATION_POTION_IDS)) || inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF) || inventorySnapshot.contains(ItemID.BRUMA_HERB)) {
            return Task.RESTOCK_REJUVINATION_POTIONS;
        }

        if (!overlay.isVisible()) {
            log(Wintertodt.class, "Overlay is not visible for some reason, walking to safe area...");
            return Task.WALK_TO_SAFE_AREA;
        }

        if (!overlay.isBossActive()) {
            int doses = getRejuvenationDoses(false);
            if (doses <= nextDoseRestock) {
                return Task.RESTOCK_REJUVINATION_POTIONS;
            }
            return Task.WAIT_FOR_BOSS;
        }

        UIResult<Boolean> tapToDrop = getWidgetManager().getHotkeys().isTapToDropEnabled();
        if (tapToDrop.isFound() && tapToDrop.get()) {
            if (!getWidgetManager().getHotkeys().setTapToDropEnabled(false)) {
                log(Wintertodt.class, "Failed to deactivate tap to drop...");
                return null;
            }
        }

        this.warmth = overlay.getWarmthPercent();
        this.wintertodtEnergy = overlay.getEnergyPercent();
        if (warmth == null) {
            log(Wintertodt.class, "Cannot figure out our warmth value, walking to safe area...");
            return Task.WALK_TO_SAFE_AREA;
        }

        if (warmth <= nextDrinkPercent) {
            return Task.DRINK_REJUVINATION;
        }


        switch (method) {
            case SOLO -> {
                return decideSoloTask();
            }
            case GROUP -> {
                return decideGroupTask();
            }
        }
        return null;
    }

    @Override
    public boolean canBreak() {
        Boolean bossActive = overlay.isBossActive();

        boolean result = bossActive != null && !bossActive;
        if (!result) {
            breakDelay = null;
            return false;
        }
        if (breakDelay == null) {
            breakDelay = new Stopwatch(random(2000, 15000));
        }
        if (!breakDelay.hasFinished()) {
            return false;
        }
        WorldPosition myPos = getWorldPosition();
        if (myPos == null) {
            return false;
        }
        return SOCIAL_SAFE_AREA.contains(myPos);
    }

    private Task decideGroupTask() {
        WorldPosition myPosition = getWorldPosition();

        Integer points = overlay.getPoints();
        if (points == null) {
            log(Wintertodt.class, "Failed reading points value.");
            return null;
        }
        if (points < 100 && warmth < 30) {
            if (SOCIAL_SAFE_AREA.contains(myPosition)) {
                return Task.WAIT_FOR_BOSS;
            } else {
                return Task.WALK_TO_SAFE_AREA;
            }
        }

        Boolean isIncapacitated = overlay.getIncapacitated(focusedBrazier);
        if (isIncapacitated != null && isIncapacitated) {
            if (myPosition.distanceTo(focusedBrazier.getPyromancerPosition()) <= 2) {
                return Task.HEAL_PYROMANCER;
            }
        }

        RSObject brazier = getBrazier();
        WintertodtOverlay.BrazierStatus brazierStatus = overlay.getBrazierStatus(focusedBrazier);
        log(Wintertodt.class, "Brazier status: " + brazierStatus);
        if (brazierStatus != null) {
            // prioritise lighting brazier if close by
            if (brazier != null && brazier.getTileDistance() < 3) {
                if (brazierStatus != WintertodtOverlay.BrazierStatus.LIT) {
                    return Task.REPAIR_AND_LIGHT_BRAZIER;
                }
            }
        }
        // check if we reached next point milestone with our inventory contents
        boolean reachedGoal = hasReachedGoal();
        boolean reachedFirstMilestone = points >= FIRST_MILESTONE_POINTS;
        boolean fletch = fletchType == FletchType.YES || !reachedFirstMilestone && fletchType == FletchType.UNTIL_MILESTONE;

        if (brazier != null && brazier.getTileDistance() <= 3) {
            // if close to brazier and have fuel
            log(Wintertodt.class, "Kindling found: " + inventorySnapshot.getAmount(ItemID.BRUMA_KINDLING) + ", Roots found: " + inventorySnapshot.getAmount(ItemID.BRUMA_ROOT));
            if (fletch && inventorySnapshot.contains(ItemID.BRUMA_KINDLING) && !inventorySnapshot.contains(ItemID.BRUMA_ROOT)
                    || !fletch && (inventorySnapshot.contains(ItemID.BRUMA_ROOT) || inventorySnapshot.contains(ItemID.BRUMA_KINDLING))) {
                return Task.FEED_BRAZIER;
            }

        }
        // or if goal reached
        if (inventorySnapshot.isFull() || reachedGoal) {
            log(Wintertodt.class, "Reached goal: " + reachedGoal + " Free slots: " + inventorySnapshot.getFreeSlots());
            if (fletch && inventorySnapshot.containsAny(ItemID.BRUMA_ROOT)) {
                return Task.FLETCH_ROOTS;
            } else {
                if (brazierStatus != null && brazierStatus != WintertodtOverlay.BrazierStatus.LIT) {
                    return Task.REPAIR_AND_LIGHT_BRAZIER;
                }
                log(Wintertodt.class, "Feeding brazier");
                return Task.FEED_BRAZIER;
            }
        }

        return Task.CHOP_ROOTS;
    }

    private Boolean hasReachedGoal() {
        Integer currentPoints = overlay.getPoints();
        if (currentPoints == null) {
            return null;
        }
        int nextMilestone = calculateNextMilestone(currentPoints);
// get points in inventory
        UIResult<Integer> resourcePointsResult = calculateResourcePoints(currentPoints);
        if (resourcePointsResult.isFound()) {
            log(Wintertodt.class, "Resource points: " + resourcePointsResult.get() + " Current points: " + currentPoints + " nextMilestone: " + nextMilestone);
            int resourcePoints = resourcePointsResult.get();
            return nextMilestone - (currentPoints + resourcePoints) <= 0;
        } else {
            log(Wintertodt.class, "Resource points not found");
        }
        return false;
    }

    private UIResult<Integer> calculateResourcePoints(int currentPoints) {
        UIResultList<ItemSearchResult> kindling = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.BRUMA_KINDLING);
        UIResultList<ItemSearchResult> roots = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.BRUMA_ROOT);
        if (kindling.isNotVisible() || roots.isNotVisible()) {
            return UIResult.notVisible();
        }
        int kindlingPoints = 25 * kindling.size();
        boolean reachedFirstMilestone = currentPoints >= 500;

        boolean fletch = fletchType == FletchType.YES || !reachedFirstMilestone && fletchType == FletchType.UNTIL_MILESTONE;

        int rootXp = fletch ? 25 : 10;
        int rootsPoints = roots.size() * rootXp;
        return UIResult.of(kindlingPoints + rootsPoints);
    }

    //TODO
    private Task decideSoloTask() {
        if (wintertodtEnergy == null) {
            return null;
        }
        if (wintertodtEnergy < 6) {
            // wait for wintertodt to heal (avoid lighting braziers)
        } else if (wintertodtEnergy >= 7 && wintertodtEnergy < 25) {
            // focus points
        } else {
            // focus on reducing wintertodt health by lighting/repairing all braziers, also healing pyromancers

        }
        return null;
    }

    public void executeTask(Task task) {
        switch (task) {
            // done
            case CHOP_ROOTS -> chopRoots();
            // done
            case CHECK_EQUIPMENT -> checkEquipment();
            // done
            case FLETCH_ROOTS -> fletchRoots();
            // done
            case FEED_BRAZIER -> feedBrazier();
            // done
            case DRINK_REJUVINATION -> drinkRejuvenation();
            // done
            case RESTOCK_REJUVINATION_POTIONS -> restockRejuvenation();
            //done
            case WAIT_FOR_BOSS -> waitForBoss();
            //done
            case WALK_TO_BOSS_AREA -> walkToBossArea();
            // done
            case WALK_TO_SAFE_AREA -> walkToSafeArea();
            // done
            case GET_EQUIPMENT -> getEquipment();
            // done
            case REPAIR_AND_LIGHT_BRAZIER -> repairLightBrazier();
            //todo
            case HEAL_PYROMANCER -> healPyromancer();
        }
    }

    private void healPyromancer() {
        WorldPosition pyromancerPosition = focusedBrazier.getPyromancerPosition();

        Polygon poly = getSceneProjector().getTileCube(pyromancerPosition, 100);
        if (poly == null) {
            return;
        }
        poly = poly.getResized(0.7);
        int doses = getRejuvenationDoses(false);
        if (getFinger().tap(poly, "Help pyromancer", "Heal pyromancer")) {
            submitTask(() -> {
                Boolean incapacitated = overlay.getIncapacitated(focusedBrazier);
                if (incapacitated != null && !incapacitated) {
                    return true;
                }
                int currentDoses = getRejuvenationDoses(true);
                return currentDoses < doses;
            }, 3000);
        }

    }

    private void getEquipment() {
        if (missingEquipment.isEmpty()) {
            return;
        }

        Equipment itemToRetrieve = missingEquipment.iterator().next();
        UIResult<ItemSearchResult> item = getItemManager().findItem(getWidgetManager().getInventory(), itemToRetrieve.getItemIds());

        if (item.isFound()) {
            missingEquipment.remove(itemToRetrieve);
            return;
        }

        log(Wintertodt.class, "Getting crate with menu option: " + itemToRetrieve.getName().toLowerCase());
        RSObject crate = getCrate("take-" + itemToRetrieve.getName().toLowerCase(), null);

        if (crate == null) {
            log(Wintertodt.class, "Can't find crate for " + itemToRetrieve.getName());
            return;
        }
        if (crate.interact("take-" + itemToRetrieve.getName().toLowerCase())) {
            log(Wintertodt.class, "Interacted with crate for " + itemToRetrieve.getName());
            // wait for item to be in the inventory and remove from missing equipment list
            if (submitHumanTask(() -> {
                UIResult<ItemSearchResult> item_ = getItemManager().findItem(getWidgetManager().getInventory(), itemToRetrieve.getItemIds());
                return item_.isFound();
            }, 10000)) {
                missingEquipment.remove(itemToRetrieve);
            }
        }
    }

    //TODO break out when fletching and we have enough to reach goal
    private void chopRoots() {
        Optional<RSObject> roots = getObjectManager().getObject(object -> {
            String name = object.getName();
            if (name == null || !name.equalsIgnoreCase("bruma roots")) {
                return false;
            }
            return object.getWorldPosition().equals(focusedBrazier.getRootsPosition());
        });

        if ((roots.isEmpty())) {
            log(Wintertodt.class, "Can't find Bruma roots in scene...");
            return;
        }
        if (roots.get().interact("Chop")) {
            // wait until inventory is full
            AtomicInteger previousFreeSlots = new AtomicInteger(-1);
            Timer slotChangeTimer = new Timer();
            submitTask(() -> {
                Boolean reachedGoal = hasReachedGoal();
                log(Wintertodt.class, "Reached goal: " + reachedGoal);
                if (Boolean.TRUE.equals(reachedGoal)) {
                    log(Wintertodt.class, "Reached goal!");
                    return true;
                }

                Integer warmth = overlay.getWarmthPercent();
                if (warmth != null) {
                    log(Wintertodt.class, "Current warmth: " + warmth + " Next drinking @ " + nextDrinkPercent);
                    if (warmth <= nextDrinkPercent) {
                        return true;
                    }
                }
                if (getWidgetManager().getDialogue().isVisible()) {
                    log(Wintertodt.class, "Dialogue visible");
                    sleep(RandomUtils.weightedRandom(200, 1500));
                    return true;
                }
                Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
                if (!freeSlots.isPresent()) {
                    return false;
                }
                if (freeSlots.get() == 0) {
                    log(Wintertodt.class, "No free slots left");
                    sleep(RandomUtils.weightedRandom(200, 1000));
                    return true;
                } else if (previousFreeSlots.get() == -1) {
                    slotChangeTimer.reset();
                    previousFreeSlots.set(freeSlots.get());
                } else if (previousFreeSlots.get() != freeSlots.get()) {
                    slotChangeTimer.reset();
                    previousFreeSlots.set(freeSlots.get());
                } else if (slotChangeTimer.timeElapsed() > chopRootsTimeout) {
                    log(Wintertodt.class, "Slot change timeout");
                    // change the timeout so we don't keep interacting with the roots after a set time
                    chopRootsTimeout = random(6000, 13000);
                    return true;
                }
                return false;
            }, 70000);
        } else {
            log(Wintertodt.class, "Failed interacting with roots.");
        }
    }

    private void checkEquipment() {
        log(Wintertodt.class, "Checking equipment...");
        List<Equipment> equipmentToCheck = new ArrayList<>(List.of(Equipment.values()));
        AtomicBoolean checkedInventory = new AtomicBoolean(false);
        AtomicBoolean checkedEquipment = new AtomicBoolean(false);
        this.checkedEquipment = submitTask(() -> {
            log(equipmentToCheck.toString());
            if (checkedInventory.get() && checkedEquipment.get()) {
                missingEquipment.addAll(equipmentToCheck);
                log(Wintertodt.class, "Finished checking equipment. Missing items: " + missingEquipment);
                this.checkedEquipment = true;
                return true;
            }
            for (Equipment equipment : Equipment.values()) {
                UIResult<ItemSearchResult> result;
                if (!checkedInventory.get()) {
                    log(Wintertodt.class, "Searching inventory for: " + equipment);
                    result = getItemManager().findItem(getWidgetManager().getInventory(), equipment.getItemIds());
                } else {
                    log(Wintertodt.class, "Searching equipment for: " + equipment);
                    result = getWidgetManager().getEquipment().findItem(equipment.getItemIds());
                }

                if (result.isNotVisible()) {
                    return false;
                }
                if (result.isFound()) {
                    log(Wintertodt.class, "Found: " + equipment);
                    equipmentToCheck.remove(equipment);
                }
            }
            if (!checkedInventory.get()) {
                checkedInventory.set(true);
            } else if (!checkedEquipment.get()) {
                checkedEquipment.set(true);
            }
            return false;

        }, 10000);


        // shuffle so we retrieve in different orders
        if (!missingEquipment.isEmpty()) {
            // Convert to List
            List<Equipment> list = new ArrayList<>(missingEquipment);
            Collections.shuffle(list);
            missingEquipment = new LinkedHashSet<>(list);
        }

    }

    private RSTile getBrazierTile(Brazier focusedBrazier, RSObject brazier) {
        boolean northBrazier = focusedBrazier == Brazier.NORTH_WEST || focusedBrazier == Brazier.NORTH_EAST;

        int minX = brazier.getWorldX();
        int maxX = minX + brazier.getTileWidth();

        int y = brazier.getWorldY() + (northBrazier ? 2 : -1);
        int x = random(minX, maxX);

        int localX = x - getSceneManager().getSceneBaseTileX();
        int localY = y - getSceneManager().getSceneBaseTileY();
        return getSceneManager().getTiles()[brazier.getPlane()][localX][localY];
    }

    private void fletchRoots() {
        ItemSearchResult knife = inventorySnapshot.getItem(ItemID.KNIFE);
        if (knife == null) {
            log(Wintertodt.class, "Missing knife...");
            missingEquipment.add(Equipment.KNIFE);
            return;
        }
        ItemSearchResult root = inventorySnapshot.getRandomItem(ItemID.BRUMA_ROOT);
        if (root == null) {
            return;
        }
        WorldPosition myPosition = getWorldPosition();
        RSObject brazier = getBrazier();
        if (brazier == null) {
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> getBrazier() != null);
            getWalker().walkTo(focusedBrazier.getBrazierPosition(), builder.build());
        }
        if (SAFE_AREA.contains(myPosition)) {
            // cannot fletch in safe area
            // walk to brazier to fletch
            RSTile brazierTile = getBrazierTile(focusedBrazier, brazier);
            getWalker().walkTo(brazierTile.getWorldPosition());
            return;
        } else if (brazier.getTileDistance() > 2) {
            // if not near brazier, tap the object or nearby tile to run towards before starting to fletch roots
            tapBrazierOrTileNearby(brazier, focusedBrazier);
        }

        Integer warmth = overlay.getWarmthPercent();
        if (warmth == null) {
            log(Wintertodt.class, "Can't retrieve warmth value.");
            return;
        }
        log(Wintertodt.class, "Using knife on roots...");
        if (!knife.interact() || !root.interact()) {
            log(Wintertodt.class, "Failed combining items");
            return;
        }
        log(Wintertodt.class, "Entering wait task...");
        AtomicInteger previousAmountOfRoots = new AtomicInteger(inventorySnapshot.getAmount(ItemID.BRUMA_ROOT));
        Timer itemAmountChangeTimer = new Timer();
        submitTask(() -> {
            Integer warmthCurrent = overlay.getWarmthPercent();
            if (warmthCurrent != null) {
                log(Wintertodt.class, "Current warmth: " + warmthCurrent + " Next drinking @ " + nextDrinkPercent);
                if (warmthCurrent < warmth || warmthCurrent <= nextDrinkPercent) {
                    // if damage taken
                    return true;
                }
            }
            //TODO interrupt if a few tiles away
            if (brazier.getTileDistance() > 2) {
                // if the brazier breaks, break out of the task to decide what to do
                WintertodtOverlay.BrazierStatus brazierStatus = overlay.getBrazierStatus(focusedBrazier);
                if (brazierStatus != null && brazierStatus != WintertodtOverlay.BrazierStatus.LIT) {
                    log(Wintertodt.class, "Brazier is not lit");
                    return true;
                }
            }
            if (getWidgetManager().getDialogue().isVisible()) {
                log(Wintertodt.class, "Dialogue visible");
                sleep(RandomUtils.weightedRandom(200, 1500));
                return true;
            }
            this.inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (this.inventorySnapshot == null) {
                return false;
            }
            // add item change listener
            int amountOfRoots = inventorySnapshot.getAmount(ItemID.BRUMA_ROOT);
            if (amountOfRoots == 0) {
                log(Wintertodt.class, "Roots not found");
                sleep(RandomUtils.weightedRandom(200, 1000));
                return true;
            }
            if (amountOfRoots < previousAmountOfRoots.get()) {
                previousAmountOfRoots.set(amountOfRoots);
                itemAmountChangeTimer.reset();
            } else if (itemAmountChangeTimer.timeElapsed() > fletchTimeout) {
                log(Wintertodt.class, "Slot change timeout...");
                fletchTimeout = random(5000, 7000);
                return true;
            }
            return false;
        }, 50000);
    }

    private boolean tapBrazierOrTileNearby(RSObject brazier, Brazier focusedBrazier) {
        int randomNumber = random(3);
        boolean tapBrazier = randomNumber == 0;
        if (tapBrazier && brazier.isInteractableOnScreen()) {
            Polygon polygon = brazier.getConvexHull();
            if (polygon != null) {
                if (getFinger().tapGameScreen(polygon, (MenuHook) null)) {
                    return true;
                }
            }
        }
        RSTile brazierTile = getBrazierTile(focusedBrazier, brazier);
        if (brazierTile != null) {
            randomNumber = random(3);
            boolean walkScreen = randomNumber != 0;
            if (brazierTile.isOnGameScreen() && walkScreen) {
                brazierTile.interact("Walk here");
            } else {
                WorldPosition myPos = getWorldPosition();
                Point tile2dMapCoords = getWidgetManager().getMinimap().toMinimapCoordinates(myPos, brazierTile);
                if (!getFinger().tap(tile2dMapCoords)) {
                    return false;
                }
                sleep(RandomUtils.weightedRandom(100, 1000));
                return true;
            }
        }
        return false;
    }

    private RSObject getBrazier() {
        Optional<RSObject> brazier = getObjectManager().getObject(object -> {
            String name = object.getName();
            if (name == null || !name.equalsIgnoreCase("brazier")) {
                return false;
            }
            return object.getWorldPosition().equals(focusedBrazier.getBrazierPosition());
        });
        if (!brazier.isPresent()) {
            return null;
        }
        return brazier.get();
    }

    private void repairLightBrazier() {
        RSObject brazier = getBrazier();
        if (brazier == null) {
            log(Wintertodt.class, "Can't find Brazier object in the loaded scene.");
            return;
        }

        WintertodtOverlay.BrazierStatus initialBrazierStatus = overlay.getBrazierStatus(focusedBrazier);
        if (initialBrazierStatus == null) {
            log(Wintertodt.class, "Can't read Brazier status.");
            return;
        }

        if (!brazier.isInteractableOnScreen()) {
            log(Wintertodt.class, "Brazier is not interactable on screen, walking to it to repair/light.");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> brazier.isInteractableOnScreen());
            getWalker().walkTo(brazier, builder.build());
            return;
        }
        Polygon brazierPoly = brazier.getConvexHull();
        if (brazierPoly == null) {
            return;
        }
        // cast menuHook to avoid ambigious call
        if (getFinger().tapGameScreen(brazierPoly, (MenuHook) null)) {
            // sleep until brazier status changes
            submitTask(() -> {
                WintertodtOverlay.BrazierStatus brazierStatus = overlay.getBrazierStatus(focusedBrazier);
                if (brazierStatus != null) {
                    return brazierStatus != initialBrazierStatus;
                }
                return false;
            }, 7000);
        }
    }

    private void feedBrazier() {
        RSObject brazier = getBrazier();
        if (brazier == null) {
            log(Wintertodt.class, "Can't find Brazier object in the loaded scene, lets try walk towards it.");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> getBrazier() != null);
            return;
        }
        UIResultList<ItemSearchResult> initialBrazierFeed = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.BRUMA_ROOT, ItemID.BRUMA_KINDLING);
        if (initialBrazierFeed.isNotVisible()) {
            return;
        }
        AtomicInteger previousAmountOfFeed = new AtomicInteger(initialBrazierFeed.size());
        Timer itemAmountChangeTimer = new Timer();
        Integer warmth = overlay.getWarmthPercent();
        if (warmth == null) {
            log(Wintertodt.class, "Can't read Warmth value...");
            return;
        }
        if (brazier.interact("Burning brazier", new String[]{"feed", "fix", "light"})) {
            // sleep until brazier status changes
            submitTask(() -> {
                //listen hitpoints
                Integer warmthCurrent = overlay.getWarmthPercent();
                if (warmthCurrent != null) {
                    log(Wintertodt.class, "Current warmth: " + warmthCurrent + " Next drinking @ " + nextDrinkPercent);
                    if (warmthCurrent < warmth || warmthCurrent <= nextDrinkPercent) {
                        // if damage taken
                        log(Wintertodt.class, "warmth decreased");
                        return true;
                    }
                }
                if (getWidgetManager().getDialogue().isVisible()) {
                    sleep(RandomUtils.weightedRandom(200, 1500));
                    return true;
                }
                // listen for brazier status
                WintertodtOverlay.BrazierStatus brazierStatus = overlay.getBrazierStatus(focusedBrazier);
                if (brazierStatus != null && brazierStatus != WintertodtOverlay.BrazierStatus.LIT) {
                    log(Wintertodt.class, "Brazier is not lit");
                    return true;
                }

                UIResultList<ItemSearchResult> brazierFeed = getItemManager().findAllOfItem(getWidgetManager().getInventory(), ItemID.BRUMA_ROOT, ItemID.BRUMA_KINDLING);
                if (brazierFeed.isNotVisible()) {
                    return false;
                }
                if (brazierFeed.isNotFound()) {
                    log(Wintertodt.class, "Ran out of brazier feed...");
                    return true;
                }
                //TODO doesn't work
                int rootAmount = brazierFeed.size();
                if (rootAmount < previousAmountOfFeed.get()) {
                    itemAmountChangeTimer.reset();
                } else if (itemAmountChangeTimer.timeElapsed() > brazierTimeout) {
                    log(Wintertodt.class, "amount change timer, timed out");
                    brazierTimeout = random(5000, 7000);
                    return true;
                }
                return false;
            }, 50000);

        }
    }

    private void drinkRejuvenation() {
        // drink smallest dose first
        ItemSearchResult potionToDrink = null;
        Set<ItemSearchResult> rejuvenationPotions = inventorySnapshot.getAllOfItems(new HashSet<>(REJUVENATION_POTION_IDS));
        for (int i = 0; i < REJUVENATION_POTION_IDS.size(); i++) {
            for (ItemSearchResult result : rejuvenationPotions) {
                if (result.getId() == REJUVENATION_POTION_IDS.get(i)) {
                    potionToDrink = result;
                    break;
                }
            }
            if (potionToDrink != null) {
                break;
            }
        }
        if (potionToDrink == null) {
            return;
        }
        if (potionToDrink.interact()) {
            nextDrinkPercent = random(minDrinkPercent, maxDrinkPercent);
            potionDrinkCooldown.reset(random(2000, 2800));
        }
    }

    private int getRejuvenationDoses(boolean update) {
        Set<ItemSearchResult> rejuvenationPotions = inventorySnapshot.getAllOfItems(new HashSet<>(REJUVENATION_POTION_IDS));
        AtomicInteger currentDosesAtomic = new AtomicInteger();
        rejuvenationPotions.forEach(itemSearchResult -> {
            switch (itemSearchResult.getId()) {
                case ItemID.REJUVENATION_POTION_1 -> currentDosesAtomic.getAndAdd(1);
                case ItemID.REJUVENATION_POTION_2 -> currentDosesAtomic.getAndAdd(2);
                case ItemID.REJUVENATION_POTION_3 -> currentDosesAtomic.getAndAdd(3);
                case ItemID.REJUVENATION_POTION_4 -> currentDosesAtomic.getAndAdd(4);
            }
        });
        return currentDosesAtomic.get();
    }

    private void restockRejuvenation() {
        log(Wintertodt.class, "Restocking Rejuvenation");
        int currentDoses = getRejuvenationDoses(false);
        inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB));
        if (inventorySnapshot == null) {
            return;
        }

        // work out how many of each to get
        int maxResourceAmount = inventorySnapshot.getFreeSlots() / 2;

        if (maxResourceAmount > potionsToPrep) {
            maxResourceAmount = potionsToPrep;
        }
        int totalDoses = maxResourceAmount * 4;
        int dosesRemaining = totalDoses - currentDoses;
        int potionsNeeded = dosesRemaining / 4;
        // work out how much more to retrieve based on resources in inv
        int unfPotionsNeeded = potionsNeeded - inventorySnapshot.getAmount(ItemID.REJUVENATION_POTION_UNF);
        int brumaHerbsNeeded = potionsNeeded - inventorySnapshot.getAmount(ItemID.BRUMA_HERB);

        log(Wintertodt.class, "Unf potions needed: " + unfPotionsNeeded + " Bruma herbs needed: " + brumaHerbsNeeded);
        // drop if we have too many
        if (unfPotionsNeeded < 0) {
            log(Wintertodt.class, "Too many unf potions, dropping...");
            getWidgetManager().getInventory().dropItem(ItemID.REJUVENATION_POTION_UNF, Math.abs(unfPotionsNeeded));
        } else if (brumaHerbsNeeded < 0) {
            log(Wintertodt.class, "Too many herbs, dropping...");
            getWidgetManager().getInventory().dropItem(ItemID.BRUMA_HERB, Math.abs(brumaHerbsNeeded));
        }
        // if we need more
        else if (unfPotionsNeeded > 0) {
            log(Wintertodt.class, "Need more unf potions");
            getUnfPotions(inventorySnapshot.getAmount(ItemID.REJUVENATION_POTION_UNF), unfPotionsNeeded);
        } else if (brumaHerbsNeeded > 0) {
            log(Wintertodt.class, "Need more herbs");
            pickHerbs(inventorySnapshot.getAmount(ItemID.BRUMA_HERB), brumaHerbsNeeded, inventorySnapshot.getFreeSlots());
        } else {
            if(!inventorySnapshot.contains(ItemID.BRUMA_HERB) || !inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF)) {
                return;
            }
            // ensure tap to drop is disabled
            UIResult<Boolean> tapToDrop = getWidgetManager().getHotkeys().isTapToDropEnabled();
            if (tapToDrop.isFound() && tapToDrop.get()) {
                if (!getWidgetManager().getHotkeys().setTapToDropEnabled(false)) {
                    log(Wintertodt.class, "Failed to deactivate tap to drop...");
                    return;
                }
            }
            // make potions
            switch (healType) {
                case REJUVENATION_BREWMA -> useIngredientsOnBrewMa(inventorySnapshot);
                case REJUVENATION -> combineIngredients(inventorySnapshot);
            }
        }
    }

    private void useIngredientsOnBrewMa(ItemGroupResult inventorySnapshot) {
        Polygon tilePoly = getBrewmaTileCube(0.6);

        if (tilePoly == null) {
            // walk to tile
            walkToBrewma();
            return;
        }
        // select "Use" on a random ingredient
        ItemSearchResult randomItem = inventorySnapshot.getRandomItem((random(2) == 0 ? ItemID.BRUMA_HERB : ItemID.REJUVENATION_POTION_UNF));
        if (randomItem == null) {
            return;
        }
        // check result as it will return false if we have a purpose miss click
        if (!randomItem.interact()) {
            return;
        }
        ItemDefinition itemDefinition = getItemManager().getItemDefinition(randomItem.getId());

        if (itemDefinition.name == null) {
            log(Wintertodt.class, "Item definition is null for item: " + randomItem.getId());
            return;
        }
        tilePoly = getBrewmaTileCube(0.6);
        if (tilePoly == null) {
            return;
        }
        if (getFinger().tapGameScreen(tilePoly, "Use " + itemDefinition.name + " -> " + "Brew'ma")) {
            submitTask(() -> {
                this.inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB));
                if (this.inventorySnapshot == null) {
                    return false;
                }
                return !this.inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF) || !inventorySnapshot.contains(ItemID.BRUMA_HERB);
            }, 7000);
        }
    }

    private Polygon getBrewmaTileCube(double scale) {
        Polygon tilePoly;
        tilePoly = getSceneProjector().getTileCube(BREWMA_POSITION, 80, true);
        if (tilePoly == null) {
            return null;
        }
        if (scale < 1.0) {
            tilePoly = tilePoly.getResized(scale);
            if (tilePoly == null) {
                return null;
            }
        }

        if (!getWidgetManager().insideGameScreen(tilePoly, Collections.emptyList())) {
            return null;
        }
        return tilePoly;
    }

    private void walkToBrewma() {
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakDistance(2);
        builder.breakCondition(() -> {
            if (getWorldPosition() == null) {
                return false;
            }
            Polygon tilePoly = getBrewmaTileCube(0.6);
            return tilePoly != null && getWidgetManager().insideGameScreen(tilePoly, Collections.emptyList());
        });
        getWalker().walkTo(BREWMA_POSITION, builder.build());
    }

    private void combineIngredients(ItemGroupResult inventorySnapshot) {
        boolean makeFast = random(5) != 0;
        Supplier<WalkConfig> combineSupplier = getCombineSupplier(inventorySnapshot, makeFast);
        RectangleArea area = null;
        Integer warmth = overlay.getWarmthPercent();
        WorldPosition myPos = getWorldPosition();

        if (overlay.isVisible() && warmth != null && warmth > minDrinkPercent) {
            if (!focusedBrazier.getArea().contains(myPos)) {
                Boolean isBossActive = overlay.isBossActive();
                if (isBossActive != null && isBossActive) {
                    area = focusedBrazier.getArea();
                }
            }
        } else {
            if (!SOCIAL_SAFE_AREA.contains(myPos)) {
                area = SOCIAL_SAFE_AREA;
            }
        }

        if (area == null) {
            combineSupplier.get();
            return;
        }
        final RectangleArea finalArea = area;
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(
                () -> {
                    this.inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB));
                    if (this.inventorySnapshot == null) {
                        return false;
                    }
                    if (!inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF) || !inventorySnapshot.contains(ItemID.BRUMA_HERB)) {
                        return true;
                    }
                    return getWorldPosition() != null && finalArea.contains(getWorldPosition());
                }
        );
        builder.doWhileWalking(combineSupplier);
        getWalker().walkTo(finalArea.getRandomPosition(), builder.build());
    }

    private Supplier<WalkConfig> getCombineSupplier(ItemGroupResult inventorySnapshot, boolean makeFast) {
        if(!inventorySnapshot.contains(ItemID.BRUMA_HERB) || !inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF)) {
            return null;
        }
        Supplier<WalkConfig> combineSupplier;
        if (makeFast) {
            AtomicInteger count = new AtomicInteger(0);
            final int amountToMake = Math.min(inventorySnapshot.getAmount(ItemID.BRUMA_HERB), inventorySnapshot.getAmount(ItemID.REJUVENATION_POTION_UNF));
            List<ItemSearchResult> brumaHerbs = new ArrayList<>(inventorySnapshot.getAllOfItem(ItemID.BRUMA_HERB));
            List<ItemSearchResult> unfPotions = new ArrayList<>(inventorySnapshot.getAllOfItem(ItemID.REJUVENATION_POTION_UNF));
            Collections.shuffle(brumaHerbs);
            Collections.shuffle(unfPotions);
            combineSupplier = () -> {
                submitTask(() -> {
                    if (count.get() >= amountToMake) {
                        return true;
                    }
                    if (!getFinger().tap(false, unfPotions.get(count.get())) || !getFinger().tap(false, brumaHerbs.get(count.getAndIncrement()))) {
                        return true;
                    }
                    submitTask(() -> false, random(RandomUtils.weightedRandom(200, 800)));
                    return false;
                }, 8000);
                return null;
            };
        } else {
            combineSupplier = () -> {
                this.inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB));
                if(this.inventorySnapshot == null) {
                    return null;
                }
                int rand = random(2);
                ItemSearchResult item1 = this.inventorySnapshot.getRandomItem((rand == 0 ? ItemID.REJUVENATION_POTION_UNF : ItemID.BRUMA_HERB));
                ItemSearchResult item2 = this.inventorySnapshot.getRandomItem((rand == 0 ? ItemID.BRUMA_HERB : ItemID.REJUVENATION_POTION_UNF));
                if(item1 == null && item2 == null) {
                    return null;
                }
                if (item1.interact() && item2.interact()) {
                    // sleep until potions are made
                    submitTask(() -> {
                        this.inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF));
                        if (this.inventorySnapshot == null) {
                            return false;
                        }
                        return !inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF);
                    }, random(3000, 8000));
                }
                return null;
            };
        }
        return combineSupplier;
    }

    private void getUnfPotions(final int initialAmount, int unfPotionsNeeded) {
        RSObject crate = getCrate("take-concoction", REJUVENATION_CRATE_POSITIONS);
        if (crate == null) {
            log(Wintertodt.class, "Can't find Crate object...");
            return;
        }
        String menuOption = getMenuOption(unfPotionsNeeded);
        log(Wintertodt.class, "Option: " + menuOption);
        if (!crate.interact(menuOption)) {
            // failed to interact
            return;
        }
        if (submitHumanTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF));
            if (inventorySnapshot == null) {
                return false;
            }
            return inventorySnapshot.getAmount(ItemID.REJUVENATION_POTION_UNF) > initialAmount;
        }, 10000)) ;

    }

    private void pickHerbs(final int initialAmount, int herbsNeeded, int initialFreeSlots) {
        RSObject sproutingRoots = getObjectManager().getClosestObject("Sprouting roots");
        if (sproutingRoots == null) {
            log(Wintertodt.class, "Can't find Sprouting roots object");
            return;
        }
        if (!sproutingRoots.interact("Pick")) {
            //failed to interact
            return;
        }
        AtomicInteger previousFreeSlots = new AtomicInteger(initialFreeSlots);
        Timer positionChangeTimer = new Timer();
        Timer slotChangeTimer = new Timer();
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);
        if (submitHumanTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.BRUMA_HERB));
            if (inventorySnapshot == null) {
                return false;
            }

            int herbsGained = inventorySnapshot.getAmount(ItemID.BRUMA_HERB) - initialAmount;
            if (herbsGained >= herbsNeeded || inventorySnapshot.isFull()) {
                // if we have enough or inv is full
                return true;
            }

            WorldPosition myPos = getWorldPosition();
            if (myPos == null) {
                return false;
            }


            if (myPos.distanceTo(sproutingRoots.getWorldPosition()) <= 1) {
                // if at the roots listen for item change
                if (inventorySnapshot.getFreeSlots() < previousFreeSlots.get()) {
                    slotChangeTimer.reset();
                } else if (slotChangeTimer.timeElapsed() > 6000) {
                    return true;
                }
            } else {
                // stop slot change timer from finishing if we aren't at the roots
                slotChangeTimer.reset();
                // if we don't move
                if (previousPosition.get() == null) {
                    previousPosition.set(myPos);
                    positionChangeTimer.reset();
                } else if (previousPosition.get().equals(myPos)) {
                    previousPosition.set(myPos);
                    if (positionChangeTimer.timeElapsed() > idleTimeout) {
                        idleTimeout = random(1500, 3000);
                        return true;
                    }
                } else {
                    positionChangeTimer.reset();
                }
            }
            return false;
        }, 20000)) ;

    }

    private String getMenuOption(int amount) {
        int menuAmount = -1;

        if (amount >= 8) {
            menuAmount = 10;
        } else if (amount >= 3) {
            menuAmount = 5;
        }

        return "take-" + (menuAmount != -1 ? menuAmount + " " : "") + "concoction" + (menuAmount != -1 ? "s" : "");
    }

    private RSObject getCrate(String firstMenuOption, WorldPosition... positions) {
        List<RSObject> crate = getObjectManager().getObjects(object -> {
            String objectName = object.getName();
            if (objectName == null || !objectName.equalsIgnoreCase("crate")) {
                return false;
            }
            String[] actions = object.getActions();
            if (actions == null || actions.length == 0) {
                return false;
            }
            boolean contains = false;
            for (String action : actions) {
                if (action == null) continue;
                contains = contains || action.equalsIgnoreCase(firstMenuOption);
            }
            if (positions != null) {
                WorldPosition objectPos = object.getWorldPosition();
                for (WorldPosition pos : positions) {
                    if (pos.equals(objectPos)) {
                        return true;
                    }
                }
                return false;
            }
            return contains;
        });
        if (crate.isEmpty()) {
            log(Wintertodt.class, "Can't find Crate...");
            return null;
        }
        WorldPosition myPos = getWorldPosition();
        // get closest
        return Collections.min(crate, Comparator.comparingInt(obj -> myPos.distanceTo(obj.getWorldPosition())));
    }

    private void waitForBoss() {
        RSObject brazier = getBrazier();
        if (brazier == null) {
            log(Wintertodt.class, "Can't find Brazier object in the loaded scene, lets try walk towards it.");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> getBrazier() != null);
            getWalker().walkTo(focusedBrazier.getBrazierPosition(), builder.build());
            return;
        }
        if (brazier.getTileDistance() > 2) {
            RSTile tile = getBrazierTile(focusedBrazier, brazier);
            getWalker().walkTo(tile.getWorldPosition());
        }
    }

    private void walkToBossArea() {
        RSObject doors = getObjectManager().getClosestObject("Doors of dinh");
        if (doors == null) {
            log(Wintertodt.class, "Can't find Doors of dinh, please run the script in the Wintertodt area.");
            stop();
        } else {
            if (doors.interact("Enter")) {
                submitTask(() -> {
                    WorldPosition position = getWorldPosition();
                    return position != null && BOSS_AREA.contains(position);
                }, 10000);
            }
        }
    }

    private void walkToSafeArea() {
        WorldPosition position = getWorldPosition();
        if (!SAFE_AREA.contains(position)) {
            getWalker().walkTo(SOCIAL_SAFE_AREA.getRandomPosition());
        }
    }

    enum Task {
        WAIT_FOR_BOSS,
        RESTOCK_REJUVINATION_POTIONS,
        CHOP_ROOTS,
        FLETCH_ROOTS,
        WALK_TO_BOSS_AREA,
        WALK_TO_SAFE_AREA,
        CHECK_EQUIPMENT,
        DRINK_REJUVINATION,
        REPAIR_AND_LIGHT_BRAZIER,
        HEAL_PYROMANCER,
        FEED_BRAZIER,
        GET_EQUIPMENT
    }
}
