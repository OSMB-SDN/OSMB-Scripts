package com.osmb.script.firemaking.wintertodt;

import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.scene.RSTile;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.world.World;
import com.osmb.script.firemaking.wintertodt.ui.ScriptOptions;
import com.osmb.script.firemaking.wintertodt.utilities.Utils;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.osmb.script.firemaking.wintertodt.Config.*;
import static com.osmb.script.firemaking.wintertodt.Constants.*;
import static com.osmb.script.firemaking.wintertodt.Status.*;
import static com.osmb.script.firemaking.wintertodt.utilities.Utils.getMenuOption;

@ScriptDefinition(name = "Wintertodt", author = "Joe", version = 1.0, skillCategory = SkillCategory.FIREMAKING, description = "")
public class Wintertodt extends Script {
    private final WintertodtOverlay overlay;

    public Wintertodt(Object scriptCore) {
        super(scriptCore);
        overlay = new WintertodtOverlay(this);
    }


    @Override
    public void onStart() {
        ScriptOptions scriptOptions = new ScriptOptions(this);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");

        getStageController().show(scene, "Wintertodt settings", false);

        focusedBrazier = scriptOptions.getSelectedBrazier();
        fletchType = scriptOptions.getSelectedFletchType();
        healType = scriptOptions.getHealType();
        minDrinkPercent = 60;
        maxDrinkPercent = 80;
        potionsToPrep = random(4, 6);
        idleTimeout = random(1500, 3000);

        nextDrinkPercent = random(minDrinkPercent, maxDrinkPercent);
        brazierTimeout = random(5000, 7000);
        fletchTimeout = random(5000, 7000);
        chopRootsTimeout = random(6000, 13000);
        checkedEquipment = false;

        ITEM_IDS_TO_RECOGNISE.addAll(REJUVENATION_POTION_IDS);

        for (Equipment equipment : Equipment.values()) {
            for (int item : equipment.getItemIds()) {
                ITEM_IDS_TO_RECOGNISE.add(item);
            }
        }
    }

    @Override
    public int poll() {
        if (checkWorld()) {
            return 0;
        }
        log(Wintertodt.class, "Deciding task...");
        task = decideTask(method);
        log(Wintertodt.class, "Executing task: " + task);
        if (task == null) {
            return 0;
        }
        executeTask(task);
        return 0;
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

        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(Wintertodt.class, "Inventory not visible");
            return null;
        }

        // ensure no items are selected
        if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
            log(Wintertodt.class, "failed to unselect item...");
            return null;
        }

        // check equipment if not done already
        if (!checkedEquipment) {
            return Task.CHECK_EQUIPMENT;
        }

        // if missing equipment, get it from crates
        if (!MISSING_EQUIPMENT.isEmpty()) {
            return Task.GET_EQUIPMENT;
        }

        // ensure we are in a safe area before taking a break
        if (getProfileManager().isDueToBreak()) {
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
            int doses = getRejuvenationDoses(inventorySnapshot);
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

        warmth = overlay.getWarmthPercent();
        wintertodtEnergy = overlay.getEnergyPercent();
        if (warmth == null) {
            log(Wintertodt.class, "Cannot figure out our warmth value, walking to safe area...");
            return Task.WALK_TO_SAFE_AREA;
        }

        if (warmth <= nextDrinkPercent) {
            return Task.DRINK_REJUVINATION;
        }


        switch (method) {
//            case SOLO -> {
//                return decideSoloTask();
//            }
            case GROUP -> {
                return decideGroupTask(worldPosition, inventorySnapshot);
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

    private Task decideGroupTask(WorldPosition worldPosition, ItemGroupResult inventorySnapshot) {
        points = overlay.getPoints();
        if (points == null) {
            log(Wintertodt.class, "Failed reading points value.");
            return null;
        }
        if (points < 100 && warmth < 30) {
            if (SOCIAL_SAFE_AREA.contains(worldPosition)) {
                return Task.WAIT_FOR_BOSS;
            } else {
                return Task.WALK_TO_SAFE_AREA;
            }
        }

        Boolean isIncapacitated = overlay.getIncapacitated(focusedBrazier);
        if (isIncapacitated != null && isIncapacitated) {
            if (worldPosition.distanceTo(focusedBrazier.getPyromancerPosition()) <= 2) {
                return Task.HEAL_PYROMANCER;
            }
        }

        RSObject brazier = getBrazier();
        WintertodtOverlay.BrazierStatus brazierStatus = overlay.getBrazierStatus(focusedBrazier);
        log(Wintertodt.class, "Brazier status: " + brazierStatus);
        if (brazierStatus != null) {
            // prioritise lighting brazier if close by
            if (brazier != null && brazier.getTileDistance(worldPosition) < 3) {
                if (brazierStatus != WintertodtOverlay.BrazierStatus.LIT) {
                    return Task.REPAIR_AND_LIGHT_BRAZIER;
                }
            }
        }
        // check if we reached next point milestone with our inventory contents
        Boolean reachedGoal = hasReachedGoal(inventorySnapshot);
        if (reachedGoal == null) {
            log(Wintertodt.class, "Failed to determine if goal reached.");
            return null;
        }

        boolean reachedFirstMilestone = points >= FIRST_MILESTONE_POINTS;
        boolean fletch = fletchType == ScriptOptions.FletchType.YES || !reachedFirstMilestone && fletchType == ScriptOptions.FletchType.UNTIL_MILESTONE;

        if (brazier != null && brazier.getTileDistance(worldPosition) <= 3) {
            // if close to brazier and have fuel
            log(Wintertodt.class, "Kindling found: " + inventorySnapshot.getAmount(ItemID.BRUMA_KINDLING) + ", Roots found: " + inventorySnapshot.getAmount(ItemID.BRUMA_ROOT));
            if (fletch && inventorySnapshot.contains(ItemID.BRUMA_KINDLING) && !inventorySnapshot.contains(ItemID.BRUMA_ROOT)
                    || !fletch && (inventorySnapshot.contains(ItemID.BRUMA_ROOT) || inventorySnapshot.contains(ItemID.BRUMA_KINDLING))) {
                return Task.FEED_BRAZIER;
            }

        }
        // or if goal reached
        if (inventorySnapshot.isFull() || reachedGoal) {
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

    private Boolean hasReachedGoal(ItemGroupResult inventorySnapshot) {
        points = overlay.getPoints();
        if (points == null) {
            return null;
        }
        nextMilestone = Utils.calculateNextMilestone(points);
        // get points in inventory
        UIResult<Integer> resourcePointsResult = Utils.calculateResourcePoints(inventorySnapshot, points);
        if (resourcePointsResult.isFound()) {
            int resourcePoints = resourcePointsResult.get();
            return nextMilestone - (points + resourcePoints) <= 0;
        } else {
            log(Wintertodt.class, "Resource points not found");
        }
        return false;
    }

//    //TODO
//    private Task decideSoloTask() {
//        if (wintertodtEnergy == null) {
//            return null;
//        }
//        if (wintertodtEnergy < 6) {
//            // wait for wintertodt to heal (avoid lighting braziers)
//        } else if (wintertodtEnergy >= 7 && wintertodtEnergy < 25) {
//            // focus points
//        } else {
//            // focus on reducing wintertodt health by lighting/repairing all braziers, also healing pyromancers
//
//        }
//        return null;
//    }

    private boolean checkWorld() {
        Integer currentWorld = getCurrentWorld();
        // filter worlds that have "Guardians of the Rift" in their activity
        List<World> wintertodtWorlds = World.getWorlds().stream()
                .filter(world -> {
                    String activity = world.getActivity();
                    return activity != null && activity.toLowerCase().contains("wintertodt");
                }).toList();
        boolean correctWorld = wintertodtWorlds.stream()
                .anyMatch(world -> world.getId() == currentWorld);
        if (!correctWorld) {
            log(Wintertodt.class, "Current world is not a Wintertodt world: " + currentWorld + " Hopping worlds.");
            // force hop to the wintertodt world with the highest player count
            getProfileManager().forceHop(worlds -> wintertodtWorlds.stream()
                    .max(Comparator.comparingInt(World::getPlayerCount))
                    .orElse(null));
            return true;
        }
        return false;
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
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(Wintertodt.class, "Unable to snapshot inventory.");
            return;
        }
        WorldPosition pyromancerPosition = focusedBrazier.getPyromancerPosition();
        // get the tile cube in the pyromancer position
        Polygon poly = getSceneProjector().getTileCube(pyromancerPosition, 100);
        if (poly == null) {
            // poly is null, will be off screen
            return;
        }
        // get the current frame uuid
        UUID frameUuid = getScreen().getUUID();
        // resize the poly so it fits to the pyromancer
        poly = poly.getResized(0.7);
        // calculate initial rejuvenation doses
        int doses = getRejuvenationDoses(inventorySnapshot);
        // print frame check
        log(Wintertodt.class, "Tapping pyromancer poly generated at frame: " + frameUuid + " current frame: " + getScreen().getUUID());
        // tap the pyromancer
        if (getFinger().tapGameScreen(poly, "Help pyromancer", "Heal pyromancer")) {
            // wait until rejuvenation doses decrement if we successfully tapped the pyromancer
            pollFramesHuman(() -> {
                ItemGroupResult inventorySnapshot_ = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot_ == null) {
                    return false;
                }
                Boolean incapacitated = overlay.getIncapacitated(focusedBrazier);
                if (incapacitated != null && !incapacitated) {
                    return true;
                }
                int currentDoses = getRejuvenationDoses(inventorySnapshot);
                return currentDoses < doses;
            }, 3000);
        }
    }

    private void getEquipment() {
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(Wintertodt.class, "Failed to get inventory snapshot.");
            return;
        }
        Equipment itemToRetrieve = MISSING_EQUIPMENT.iterator().next();
        ItemSearchResult item = inventorySnapshot.getItem(itemToRetrieve.getItemIds());
        if (item != null) {
            MISSING_EQUIPMENT.remove(itemToRetrieve);
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
            if (pollFramesHuman(() -> {
                ItemGroupResult inventorySnapshot_ = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot_ == null) {
                    log(Wintertodt.class, "Failed to retrieve inventory snapshot.");
                    return false;
                }
                return inventorySnapshot_.containsAny(itemToRetrieve.getItemIds());
            }, 10000)) {
                MISSING_EQUIPMENT.remove(itemToRetrieve);
            }
        }
    }

    private void chopRoots() {
        RSObject roots = getObjectManager().getRSObject(object -> {
            String name = object.getName();
            if (name == null || !name.equalsIgnoreCase("bruma roots")) {
                return false;
            }
            return object.getWorldPosition().equals(focusedBrazier.getRootsPosition());
        });

        if (roots == null) {
            log(Wintertodt.class, "Can't find Bruma roots in scene...");
            return;
        }
        if (roots.interact("Chop")) {
            // wait until inventory is full or stopped chopping
            AtomicInteger previousFreeSlots = new AtomicInteger(-1);
            Timer slotChangeTimer = new Timer();
            pollFramesUntil(() -> {
                ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    log(Wintertodt.class, "Failed to retrieve inventory snapshot.");
                    return false;
                }
                Boolean reachedGoal = hasReachedGoal(inventorySnapshot);
                log(Wintertodt.class, "Reached goal: " + reachedGoal);
                if (Boolean.TRUE.equals(reachedGoal)) {
                    log(Wintertodt.class, "Reached goal!");
                    pollFramesUntil(() -> false, RandomUtils.weightedRandom(200, 3500));
                    return true;
                }

                Integer currentWarmth = overlay.getWarmthPercent();
                if (currentWarmth != null) {
                    warmth = currentWarmth;
                    if (currentWarmth <= nextDrinkPercent) {
                        return true;
                    }
                }
                if (getWidgetManager().getDialogue().isVisible()) {
                    log(Wintertodt.class, "Dialogue visible");
                    pollFramesUntil(() -> false, RandomUtils.weightedRandom(200, 2500));
                    return true;
                }

                int freeSlots = inventorySnapshot.getFreeSlots();
                if (inventorySnapshot.isFull()) {
                    log(Wintertodt.class, "No free slots left");
                    sleep(RandomUtils.weightedRandom(200, 2500));
                    return true;
                } else if (previousFreeSlots.get() == -1) {
                    slotChangeTimer.reset();
                    previousFreeSlots.set(freeSlots);
                } else if (previousFreeSlots.get() != freeSlots) {
                    slotChangeTimer.reset();
                    previousFreeSlots.set(freeSlots);
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
        AtomicBoolean checkedEquipment = new AtomicBoolean(false);
        AtomicReference<ItemGroupResult> inventorySnapshot = new AtomicReference<>(null);
        pollFramesUntil(() -> {
            if (inventorySnapshot.get() != null && checkedEquipment.get()) {
                // if checked both inventory and equipment, we are done
                MISSING_EQUIPMENT.addAll(equipmentToCheck);
                log(Wintertodt.class, "Finished checking equipment. Missing items: " + MISSING_EQUIPMENT);
                Status.checkedEquipment = true;
                return true;
            }
            if (inventorySnapshot.get() == null) {
                // get inventory snapshot
                inventorySnapshot.set(getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE));
                if (inventorySnapshot.get() == null) {
                    log(Wintertodt.class, "Failed to retrieve inventory snapshot.");
                    return false;
                }
            }
            // if we have the inventory snapshot, check inventory & equipment
            for (Equipment equipment : Equipment.values()) {
                log(Wintertodt.class, "Searching inventory for: " + equipment);
                if (inventorySnapshot.get().containsAny(equipment.getItemIds())) {
                    equipmentToCheck.remove(equipment);
                    continue;
                }
                // check equipment tab if not found in inventory
                UIResult<ItemSearchResult> result = getWidgetManager().getEquipment().findItem(equipment.getItemIds());
                if (result.isNotVisible()) {
                    return false;
                }
                if (result.isFound()) {
                    log(Wintertodt.class, "Found: " + equipment);
                    equipmentToCheck.remove(equipment);
                }
            }
            checkedEquipment.set(true);
            return false;

        }, 10000);

        // shuffle so we retrieve in different orders
        if (!MISSING_EQUIPMENT.isEmpty()) {
            Collections.shuffle(MISSING_EQUIPMENT);
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
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(Wintertodt.class, "Failed to get inventory snapshot.");
            return;
        }

        ItemSearchResult knife = inventorySnapshot.getItem(ItemID.KNIFE, FLETCHING_KNIFE_ID);
        if (knife == null) {
            log(Wintertodt.class, "Missing knife...");
            MISSING_EQUIPMENT.add(Equipment.KNIFE);
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
            return;
        }
        if (SAFE_AREA.contains(myPosition)) {
            // cannot fletch in safe area
            // walk to brazier to fletch
            RSTile brazierTile = getBrazierTile(focusedBrazier, brazier);
            getWalker().walkTo(brazierTile.getWorldPosition());
            return;
        } else if (brazier.getTileDistance(myPosition) > 2) {
            // if not near brazier, tap the object or nearby tile to run towards before starting to fletch roots
            tapBrazierOrTileNearby(brazier, focusedBrazier);
        }

        warmth = overlay.getWarmthPercent();
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
        pollFramesUntil(() -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                log(Wintertodt.class, "Position is null");
                return false;
            }
            Integer warmthCurrent = overlay.getWarmthPercent();
            if (warmthCurrent != null) {
                if (warmthCurrent < warmth || warmthCurrent <= nextDrinkPercent) {
                    // if damage taken
                    warmth = warmthCurrent;
                    return true;
                }
            }
            //TODO interrupt if a few tiles away
            if (brazier.getTileDistance(myPosition_) > 2) {
                // if the brazier breaks, break out of the task to decide what to do
                WintertodtOverlay.BrazierStatus brazierStatus = overlay.getBrazierStatus(focusedBrazier);
                if (brazierStatus != null && brazierStatus != WintertodtOverlay.BrazierStatus.LIT) {
                    log(Wintertodt.class, "Brazier is not lit");
                    return true;
                }
            }
            if (getWidgetManager().getDialogue().isVisible()) {
                log(Wintertodt.class, "Dialogue visible");
                return true;
            }
            ItemGroupResult inventorySnapshot_ = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot_ == null) {
                return false;
            }
            // add item change listener
            int amountOfRoots = inventorySnapshot_.getAmount(ItemID.BRUMA_ROOT);
            if (amountOfRoots == 0) {
                log(Wintertodt.class, "Roots not found");
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
        return getObjectManager().getRSObject(object -> {
            String name = object.getName();
            if (name == null || !name.equalsIgnoreCase("brazier")) {
                return false;
            }
            return object.getWorldPosition().equals(focusedBrazier.getBrazierPosition());
        });
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
            builder.breakCondition(brazier::isInteractableOnScreen);
            getWalker().walkTo(brazier, builder.build());
            return;
        }
        Polygon brazierPoly = brazier.getConvexHull();
        if (brazierPoly == null) {
            return;
        }
        // cast menuHook to avoid ambigious call
        if (getFinger().tapGameScreen(brazierPoly)) {
            // sleep until brazier status changes
            submitTask(() -> {
                WintertodtOverlay.BrazierStatus brazierStatus = overlay.getBrazierStatus(focusedBrazier);
                if (brazierStatus != null) {
                    return brazierStatus != initialBrazierStatus;
                }
                return false;
            }, 7000);
            sleep(RandomUtils.weightedRandom(100, 800));
        }
    }

    private void feedBrazier() {
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(Wintertodt.class, "Failed to retrieve inventory snapshot.");
            return;
        }
        RSObject brazier = getBrazier();
        if (brazier == null) {
            log(Wintertodt.class, "Can't find Brazier object in the loaded scene, lets try walk towards it.");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> getBrazier() != null);
            return;
        }

        AtomicInteger previousAmountOfFeed = new AtomicInteger(inventorySnapshot.getAmount(ItemID.BRUMA_ROOT, ItemID.BRUMA_KINDLING));
        Timer itemAmountChangeTimer = new Timer();
        warmth = overlay.getWarmthPercent();
        if (warmth == null) {
            log(Wintertodt.class, "Can't read Warmth value...");
            return;
        }
        if (brazier.interact("Burning brazier", new String[]{"feed", "fix", "light"})) {
            // sleep until brazier status changes
            pollFramesUntil(() -> {
                ItemGroupResult inventorySnapshot_ = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot_ == null) {
                    log(Wintertodt.class, "Failed to retrieve inventory snapshot.");
                    return false;
                }
                if (!inventorySnapshot_.containsAny(ItemID.BRUMA_ROOT, ItemID.BRUMA_KINDLING)) {
                    sleep(RandomUtils.weightedRandom(400, 3500));
                    return true;
                }
                Integer warmthCurrent = overlay.getWarmthPercent();
                if (warmthCurrent != null) {
                    if (warmthCurrent < warmth || warmthCurrent <= nextDrinkPercent) {
                        // if damage taken
                        log(Wintertodt.class, "warmth decreased");
                        warmth = warmthCurrent;
                        return true;
                    }
                } else {
                    log(Wintertodt.class, "Failed to retrieve warmth value.");
                    return false;
                }
                // listen for brazier status
                brazierStatus = overlay.getBrazierStatus(focusedBrazier);
                if (brazierStatus != null && brazierStatus != WintertodtOverlay.BrazierStatus.LIT) {
                    log(Wintertodt.class, "Brazier is not lit");
                    return true;
                }

                int rootAmount = inventorySnapshot_.getAmount(ItemID.BRUMA_ROOT, ItemID.BRUMA_KINDLING);
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
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(Wintertodt.class, "Failed to retrieve inventory snapshot.");
            return;
        }
        // drink smallest dose first
        ItemSearchResult potionToDrink = null;
        List<ItemSearchResult> rejuvenationPotions = inventorySnapshot.getAllOfItems(new HashSet<>(REJUVENATION_POTION_IDS));
        for (Integer rejuvenationPotionId : REJUVENATION_POTION_IDS) {
            for (ItemSearchResult result : rejuvenationPotions) {
                if (result.getId() == rejuvenationPotionId) {
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
            POTION_DRINK_COOLDOWN.reset(random(2000, 2800));
        }
    }

    private int getRejuvenationDoses(ItemGroupResult inventorySnapshot) {
        List<ItemSearchResult> rejuvenationPotions = inventorySnapshot.getAllOfItems(new HashSet<>(REJUVENATION_POTION_IDS));
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
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB));
        if (inventorySnapshot == null) {
            return;
        }
        int currentDoses = getRejuvenationDoses(inventorySnapshot);

        // work out how many of each to get
        int rejuvenationPotions = inventorySnapshot.getAmount(ItemID.REJUVENATION_POTION_UNF);
        int brumaHerbs = inventorySnapshot.getAmount(ItemID.BRUMA_HERB);
        int maxResourceAmount = inventorySnapshot.getFreeSlots() + rejuvenationPotions + brumaHerbs / 2;

        if (maxResourceAmount > potionsToPrep) {
            maxResourceAmount = potionsToPrep;
        }
        int totalDoses = maxResourceAmount * 4;
        int dosesRemaining = totalDoses - currentDoses;
        int potionsNeeded = dosesRemaining / 4;
        // work out how much more to retrieve based on resources in inv
        int unfPotionsNeeded = potionsNeeded - rejuvenationPotions;
        int brumaHerbsNeeded = potionsNeeded - brumaHerbs;

        log(Wintertodt.class, "Unf potions needed: " + unfPotionsNeeded + " Bruma herbs needed: " + brumaHerbsNeeded);
        // drop if we have too many
        if (unfPotionsNeeded < 0) {
            log(Wintertodt.class, "Too many unf potions, dropping" + " " + Math.abs(unfPotionsNeeded) + " unf potions...");
            getWidgetManager().getInventory().dropItem(ItemID.REJUVENATION_POTION_UNF, Math.abs(unfPotionsNeeded));
        } else if (brumaHerbsNeeded < 0) {
            log(Wintertodt.class, "Too many herbs, dropping " + Math.abs(brumaHerbsNeeded) + " herbs...");
            getWidgetManager().getInventory().dropItem(ItemID.BRUMA_HERB, Math.abs(brumaHerbsNeeded));
        }
        // if we need more
        else if (unfPotionsNeeded > 0) {
            log(Wintertodt.class, "Need more unf potions");
            getUnfPotions(rejuvenationPotions, unfPotionsNeeded);
        } else if (brumaHerbsNeeded > 0) {
            log(Wintertodt.class, "Need more herbs");
            pickHerbs(brumaHerbs, brumaHerbsNeeded, inventorySnapshot.getFreeSlots());
        } else {
            if (!inventorySnapshot.contains(ItemID.BRUMA_HERB) || !inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF)) {
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
            // fail to interact
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
            pollFramesUntil(() -> {
                ItemGroupResult inventorySnapshot_ = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB));
                if (inventorySnapshot_ == null) {
                    return false;
                }
                return !inventorySnapshot_.contains(ItemID.REJUVENATION_POTION_UNF) || !inventorySnapshot_.contains(ItemID.BRUMA_HERB);
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
        if (combineSupplier == null) {
            return;
        }
        RectangleArea area = null;
        Integer warmth = overlay.getWarmthPercent();
        WorldPosition myPos = getWorldPosition();

        if (warmth != null && warmth > minDrinkPercent) {
            if (!focusedBrazier.getArea().contains(myPos)) {
                Boolean isBossActive = overlay.isBossActive();
                if (isBossActive != null && isBossActive) {
                    area = focusedBrazier.getArea();
                }
            }
        } else {
            if (!SOCIAL_SAFE_AREA.contains(myPos)) {
                // if we can't determine warmth, just combine in safe area
                area = SOCIAL_SAFE_AREA;
            }
        }

        if (area == null) {
            // if we're already in the designated area just combine on the spot
            combineSupplier.get();
            return;
        }
        final RectangleArea finalArea = area;
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(
                () -> {
                    ItemGroupResult inventorySnapshot_ = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF, ItemID.BRUMA_HERB));
                    if (inventorySnapshot_ == null) {
                        return false;
                    }
                    if (!inventorySnapshot_.contains(ItemID.REJUVENATION_POTION_UNF) || !inventorySnapshot_.contains(ItemID.BRUMA_HERB)) {
                        return true;
                    }
                    return getWorldPosition() != null && finalArea.contains(getWorldPosition());
                }
        );
        builder.doWhileWalking(combineSupplier);
        getWalker().walkTo(finalArea.getRandomPosition(), builder.build());
    }

    private Supplier<WalkConfig> getCombineSupplier(ItemGroupResult inventorySnapshot, boolean makeFast) {
        if (!inventorySnapshot.contains(ItemID.BRUMA_HERB) || !inventorySnapshot.contains(ItemID.REJUVENATION_POTION_UNF)) {
            return null;
        }
        Supplier<WalkConfig> combineSupplier;
        if (makeFast) {
            List<ItemSearchResult> brumaHerbs = new ArrayList<>(inventorySnapshot.getAllOfItem(ItemID.BRUMA_HERB));
            List<ItemSearchResult> unfPotions = new ArrayList<>(inventorySnapshot.getAllOfItem(ItemID.REJUVENATION_POTION_UNF));
            Collections.shuffle(brumaHerbs);
            Collections.shuffle(unfPotions);
            combineSupplier = Utils.getFastCombineSupplier(this, brumaHerbs, unfPotions);
        } else {
            combineSupplier = Utils.getCombineSupplier(this);
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
        if (pollFramesHuman(() -> {
            ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.REJUVENATION_POTION_UNF));
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
        if (pollFramesHuman(() -> {
            ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(Set.of(ItemID.BRUMA_HERB));
            if (inventorySnapshot == null) {
                return false;
            }

            int herbsGained = inventorySnapshot.getAmount(ItemID.BRUMA_HERB) - initialAmount;
            if (herbsGained >= herbsNeeded || inventorySnapshot.isFull()) {
                // if we have enough or inv is full
                LocalPosition localPosition = getLocalPosition();
                if (localPosition != null) {
                    submitTask(() -> false, RandomUtils.weightedRandom(0, 1000, 0.005));
                    // walk away to stop picking roots
                    List<LocalPosition> positions = getWalker().getCollisionManager().findReachableTiles(localPosition, 3);
                    List<LocalPosition> validPositions = positions.stream()
                            .filter(pos -> SOCIAL_SAFE_AREA.contains(pos.toWorldPosition(this)))
                            .toList();
                    LocalPosition randomPosition = validPositions.isEmpty() ? null : validPositions.get(random(validPositions.size()));
                    if (randomPosition != null) {
                        getWalker().walkTo(SOCIAL_SAFE_AREA.getRandomPosition());
                    }
                }
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
        return Collections.min(crate, Comparator.comparingDouble(obj -> myPos.distanceTo(obj.getWorldPosition())));
    }

    private void waitForBoss() {
        WorldPosition position = getWorldPosition();
        if (position == null) {
            log(Wintertodt.class, "World position is null");
            return;
        }
        RSObject brazier = getBrazier();
        if (brazier == null) {
            log(Wintertodt.class, "Can't find Brazier object in the loaded scene, lets try walk towards it.");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> getBrazier() != null);
            getWalker().walkTo(focusedBrazier.getBrazierPosition(), builder.build());
            return;
        }
        if (brazier.getTileDistance(position) > 2) {
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

    @Override
    public void onPaint(Canvas c) {
        FontMetrics metrics = c.getFontMetrics(ARIEL);
        int padding = 5;

        List<String> lines = new ArrayList<>();
        lines.add("Task: " + (task == null ? "None" : task));

        lines.add("Warmth: " + (warmth == null ? "Unknown" : warmth + "%") + " Next drink @ " + nextDrinkPercent + "%");
        lines.add("Points: " + (points == null ? "Unknown" : points) + " Next milestone: " + nextMilestone);

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
        c.drawRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.ORANGE.getRGB());
        // Draw text lines
        int drawY = 40;
        for (String line : lines) {
            int color = Color.WHITE.getRGB();
            c.drawText(line, drawX, drawY += metrics.getHeight(), color, ARIEL);
        }
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{6461, 12850};
    }


}
