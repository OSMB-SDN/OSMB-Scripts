package com.osmb.script.runecrafting.gotr;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.profile.afk.AFKTime;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.api.walker.pathing.CollisionManager;
import com.osmb.api.walker.pathing.pathfinding.astar.AStarPathFinder;
import com.osmb.api.world.World;
import com.osmb.script.runecrafting.gotr.javafx.UI;
import com.osmb.script.runecrafting.gotr.overlay.Overlay;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@ScriptDefinition(name = "Guardians of the Rift", description = "A script for the Guardians of the Rift minigame.", version = 1.0, author = "Joe", skillCategory = SkillCategory.RUNECRAFTING)
public class GuardiansOfTheRift extends Script {

    public static final int GOTR_REGION_ID = 14484;
    public static final int MINIMUM_POINT_REQUIREMENT = 300;
    public static final Set<CatalyticRune> GUARDIANS_TO_IGNORE = new HashSet<>();
    public static final Area BOSS_AREA = new PolyArea(List.of(new WorldPosition(3597, 9505, 0), new WorldPosition(3597, 9502, 0), new WorldPosition(3595, 9501, 0), new WorldPosition(3594, 9499, 0), new WorldPosition(3596, 9497, 0), new WorldPosition(3596, 9495, 0), new WorldPosition(3596, 9492, 0), new WorldPosition(3596, 9490, 0), new WorldPosition(3599, 9487, 0), new WorldPosition(3603, 9487, 0), new WorldPosition(3606, 9485, 0), new WorldPosition(3607, 9483, 0), new WorldPosition(3622, 9483, 0), new WorldPosition(3623, 9484, 0), new WorldPosition(3623, 9486, 0), new WorldPosition(3625, 9487, 0), new WorldPosition(3630, 9487, 0), new WorldPosition(3634, 9491, 0), new WorldPosition(3635, 9492, 0), new WorldPosition(3635, 9496, 0), new WorldPosition(3636, 9497, 0), new WorldPosition(3636, 9499, 0), new WorldPosition(3635, 9500, 0), new WorldPosition(3634, 9501, 0), new WorldPosition(3633, 9502, 0), new WorldPosition(3633, 9504, 0), new WorldPosition(3635, 9506, 0), new WorldPosition(3636, 9506, 0), new WorldPosition(3636, 9507, 0), new WorldPosition(3635, 9508, 0), new WorldPosition(3635, 9511, 0), new WorldPosition(3634, 9512, 0), new WorldPosition(3634, 9522, 0), new WorldPosition(3596, 9521, 0), new WorldPosition(3594, 9518, 0), new WorldPosition(3594, 9515, 0), new WorldPosition(3596, 9513, 0), new WorldPosition(3596, 9508, 0)));
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(
            ItemID.GUARDIAN_FRAGMENTS,
            ItemID.GUARDIAN_ESSENCE,
            ItemID.UNCHARGED_CELL,
            ItemID.WEAK_CELL,
            ItemID.MEDIUM_CELL,
            ItemID.STRONG_CELL,
            ItemID.CHARGED_CELL,
            ItemID.OVERCHARGED_CELL,
            ItemID.CATALYTIC_GUARDIAN_STONE,
            ItemID.ELEMENTAL_GUARDIAN_STONE,
            ItemID.ABYSSAL_PEARLS
    ));
    private static final Font ARIEL = new Font("Arial", Font.PLAIN, 14);
    private static final WorldPosition GUARDIAN_POSITION = new WorldPosition(3615, 9503, 0);
    private static final WorldPosition LEAVE_PORTAL = new WorldPosition(3593, 9503, 0);
    private static final Area MINING_SHORTCUT_AREA = new PolyArea(List.of(new WorldPosition(3637, 9492, 0), new WorldPosition(3645, 9492, 0), new WorldPosition(3649, 9506, 0), new WorldPosition(3642, 9517, 0), new WorldPosition(3641, 9519, 0), new WorldPosition(3638, 9519, 0), new WorldPosition(3636, 9518, 0), new WorldPosition(3635, 9517, 0), new WorldPosition(3635, 9515, 0), new WorldPosition(3635, 9513, 0), new WorldPosition(3636, 9511, 0), new WorldPosition(3636, 9509, 0), new WorldPosition(3637, 9507, 0), new WorldPosition(3637, 9505, 0), new WorldPosition(3636, 9505, 0), new WorldPosition(3637, 9504, 0), new WorldPosition(3637, 9502, 0), new WorldPosition(3636, 9501, 0), new WorldPosition(3637, 9500, 0), new WorldPosition(3637, 9497, 0), new WorldPosition(3637, 9496, 0), new WorldPosition(3637, 9494, 0)));
    private static final Area MINING_AREA = new RectangleArea(3600, 9487, 6, 5, 0);
    private static final Area PORTAL_MINE_AREA = new PolyArea(List.of(new WorldPosition(3593, 9499, 0), new WorldPosition(3593, 9501, 0), new WorldPosition(3593, 9504, 0), new WorldPosition(3592, 9505, 0), new WorldPosition(3592, 9512, 0), new WorldPosition(3588, 9513, 0), new WorldPosition(3586, 9509, 0), new WorldPosition(3588, 9506, 0), new WorldPosition(3588, 9496, 0), new WorldPosition(3590, 9494, 0), new WorldPosition(3593, 9494, 0)));
    private static final Area GAME_AREA = new PolyArea(List.of(new WorldPosition(3619, 9484, 0), new WorldPosition(3605, 9484, 0), new WorldPosition(3599, 9485, 0), new WorldPosition(3595, 9488, 0), new WorldPosition(3588, 9496, 0), new WorldPosition(3586, 9498, 0), new WorldPosition(3586, 9509, 0), new WorldPosition(3588, 9518, 0), new WorldPosition(3601, 9528, 0), new WorldPosition(3615, 9527, 0), new WorldPosition(3631, 9525, 0), new WorldPosition(3636, 9519, 0), new WorldPosition(3646, 9512, 0), new WorldPosition(3648, 9500, 0), new WorldPosition(3640, 9486, 0), new WorldPosition(3628, 9486, 0), new WorldPosition(3624, 9486, 0), new WorldPosition(3622, 9484, 0)));
    private static final Area GUARDIAN_AREA = new RectangleArea(3604, 9494, 22, 19, 0);
    private static final Set<Pouch> POUCHES_ACTIVE = new HashSet<>();
    private static final Stopwatch START_MINING_REACTION_DELAY = new Stopwatch();
    private static final Stopwatch GUARDIAN_DELAY = new Stopwatch();
    private final Stopwatch barrierResetTimer = new Stopwatch();
    private final Stopwatch fillPouchDelay = new Stopwatch();
    private int prioritisedAltarRegion = 14484;
    private Overlay overlay;
    private ItemGroupResult inventorySnapshot;
    private Task task;
    private boolean builtFirstBarrier;
    private boolean useShortcut = false;
    private boolean useTalismans;
    private int animationTimeout;
    private int amountChangeTimeout;
    private int essenceChangeTimeout;
    private int targetFragments = 140;
    private PointStrategy selectedStrategy = PointStrategy.BALANCED;
    private int spaceForEssence;
    private AStarPathFinder aStarPathFinder;
    private int previousGuardianPower;
    private Pouch.RepairType repairType;
    private World gotrWorld;
    private boolean registeredWorld = false;
    private int forceGoalPercentage;
    private int selectedFragmentAmount;
    private int selectedFragmentRandomisation;
    private boolean forceCraftEssence;
    private PouchRepair pouchRepair;
    private int gamesPlayed;
    private int gamesQualified;
    private Integer runecraftingLevel;
    private Integer agilityLevel;
    private int totalPoints;
    private Integer guardianPower;
    private Timer overlayUpdateTimeout = new Timer();
    private int nextRuneDepositAmount;
    private int cellDropThreshold = 2;
    private Stopwatch pouchRepairDelay;
    private boolean hasDegradedPouches;
    private boolean insideBossArea;

    public GuardiansOfTheRift(Object scriptCore) {
        super(scriptCore);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(RandomUtils.weightedRandom(200, 3500, 0.0017));
        }
    }

    @Override
    public void onRelog() {
        guardianPower = 0;
        totalPoints = 0;
    }

    @Override
    public void onStart() {
        // show UI
        UI ui = new UI(this);
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Guardians of the Rift Configuration", false);

        selectedStrategy = ui.getSelectedPointStrategy();
        GUARDIANS_TO_IGNORE.addAll(List.of(CatalyticRune.values()));

        List<CatalyticRune> catalyticToNotIgnore = ui.getSelectedCatalyticRunes();

        List<CatalyticRune> toRemove = new ArrayList<>();
        for (CatalyticRune rune : GUARDIANS_TO_IGNORE) {
            if (catalyticToNotIgnore.contains(rune) || rune.getQuestName() == null) {
                toRemove.add(rune);
            }
        }
        toRemove.forEach(GUARDIANS_TO_IGNORE::remove);

        useTalismans = ui.isUsingTalismans();
        repairType = ui.getPouchRepairType();
        selectedFragmentAmount = ui.getTargetFragmentAmount();
        selectedFragmentRandomisation = ui.getFragmentRandomAmount();
        nextRuneDepositAmount = random(1, 3);
        this.cellDropThreshold = ui.getCellDropThreshold();

        targetFragments = selectedFragmentAmount + random(selectedFragmentRandomisation);

        aStarPathFinder = new AStarPathFinder(this);
        overlay = new Overlay(this);
        this.amountChangeTimeout = random(4000, 6000);
        this.essenceChangeTimeout = random(1300, 2500);
        this.animationTimeout = random(3000, 5000);
        this.forceGoalPercentage = random(87, 91);

        // register items
        for (Rune rune : ElementalRune.values()) {
            ITEM_IDS_TO_RECOGNISE.add(rune.getRuneId());
            ITEM_IDS_TO_RECOGNISE.add(rune.getTalismanId());
        }
        for (Rune rune : CatalyticRune.values()) {
            ITEM_IDS_TO_RECOGNISE.add(rune.getRuneId());
            ITEM_IDS_TO_RECOGNISE.add(rune.getTalismanId());
        }
        for (Pouch pouch : Pouch.values()) {
            ITEM_IDS_TO_RECOGNISE.add(pouch.getItemID());
            if (pouch != Pouch.SMALL)
                ITEM_IDS_TO_RECOGNISE.add(pouch.getDegradedItemID());
        }

        if (repairType != null) {
            pouchRepair = new PouchRepair(this, repairType);
        }
    }

    @Override
    public int poll() {
        if (agilityLevel == null || runecraftingLevel == null) {
            registerLevels();
            return 0;
        }
        task = decideTask();
        if (task == null) {
            return 0;
        }

        switch (task) {
            case ENTER_BARRIER -> enterBarrier();
            case BUILD_BARRIER -> buildBarrier();
            case MINE -> mine();
            case POWER_GUARDIAN -> powerGuardian();
            case ENTER_PORTAL -> enterPortal();
            case CREATE_ESSENCE -> createEssence();
            case ENTER_ALTAR -> enterAltar();
            case HANDLE_ALTAR -> handleAltar();
            case TAKE_UNCHARGED_CELL -> pickUpUnchargedCell();
            case DEPOSIT_RUNES -> depositRunes();
            case DROP_ESSENCE -> dropEssence();
            case DROP_TALISMANS -> dropItems();
        }
        return 0;
    }


    private void registerLevels() {
        log(GuardiansOfTheRift.class, "Registering levels");
        if (agilityLevel == null) {
            SkillsTabComponent.SkillLevel skillLevel = getWidgetManager().getSkillTab().getSkillLevel(SkillType.AGILITY);
            if (skillLevel == null) {
                log(GuardiansOfTheRift.class, "Failed to retrieve agility level");
                return;
            }
            agilityLevel = skillLevel.getLevel();
            useShortcut = agilityLevel >= 56;
            log(GuardiansOfTheRift.class, "Agility level: " + agilityLevel + ", using shortcut: " + useShortcut);
        } else if (runecraftingLevel == null) {
            SkillsTabComponent.SkillLevel skillLevel = getWidgetManager().getSkillTab().getSkillLevel(SkillType.RUNECRAFTING);
            if (skillLevel == null) {
                log(GuardiansOfTheRift.class, "Failed to retrieve Runecrafting level");
                return;
            }
            runecraftingLevel = skillLevel.getLevel();
            Pouch.registerColossalCapacity(runecraftingLevel);
            log(GuardiansOfTheRift.class, "Runecrafting level: " + runecraftingLevel);
        }
    }

    public void enterBarrier() {
        RSObject barrier = getObjectManager().getClosestObject("Barrier");
        if (barrier == null) {
            log(GuardiansOfTheRift.class, "Please start this script inside the Guardians of the Rift minigame area.");
            WorldPosition worldPosition = getWorldPosition();
            log(GuardiansOfTheRift.class, "Current position: " + worldPosition + " Region ID: " + worldPosition.getRegionID());
            stop();
            return;
        }
        if (barrier.interact("Pass")) {
            submitHumanTask(() -> {
                WorldPosition myPosition = getWorldPosition();
                if (myPosition == null) {
                    log(GuardiansOfTheRift.class, "My position is null.");
                    return false;
                }
                if (getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // random sleep
                    submitTask(() -> false, random(500, 8000));
                    return true;
                }
                insideBossArea = BOSS_AREA.contains(myPosition);
                return insideBossArea;
            }, random(50000));
        }
    }

    @Override
    public boolean canBreak() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return false;
        }
        if (GAME_AREA.contains(myPosition)) {
            return guardianPower != null && guardianPower == 0;
        }
        return false;
    }

    @Override
    public boolean canHopWorlds() {
        return canBreak();
    }

    private void dropEssence() {
        ItemSearchResult essence = inventorySnapshot.getItem(ItemID.GUARDIAN_ESSENCE);
        if (essence != null) {
            essence.interact("Drop");
        }
    }

    private int getSpaceForEssence() {
        int space = inventorySnapshot.getFreeSlots();
        for (Pouch pouch : Pouch.values()) {
            if (inventorySnapshot.contains(pouch.getItemID())) {
                space += pouch.getSpaceLeft();
            }
        }
        return space;
    }

    private void onNewGame() {
        targetFragments = selectedFragmentAmount + random(selectedFragmentRandomisation);
        builtFirstBarrier = false;
        gamesPlayed++;
        if (totalPoints >= 300) {
            gamesQualified++;
        }
        totalPoints = 0;
        // reset pouches
        forceGoalPercentage = random(83, 88);
        for (Pouch pouch : Pouch.values()) {
            pouch.setCurrentCapacity(0);
        }
    }

    private Integer getTileDistanceToWorldPosition(WorldPosition position) {
        LocalPosition myPosition = getLocalPosition();
        if (myPosition == null || position == null) {
            log(GuardiansOfTheRift.class, "My position or target position is null.");
            return null;
        }
        LocalPosition targetPosition = position.toLocalPosition(this);
        if (targetPosition == null) {
            log(GuardiansOfTheRift.class, "Target position is null after conversion to local position.");
            return null;
        }
        Deque<WorldPosition> path = aStarPathFinder.find(myPosition, targetPosition, false);
        if (path == null) {
            return null;
        }
        return path.size();
    }

    private Task decideTask() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            thinkLog("My position is null.");
            return null;
        }
        // prioritise updating oerlay values
        Integer guardianPower = (Integer) overlay.getValue(Overlay.GUARDIAN_POWER);
        Rune catalyticRune = (Rune) overlay.getValue(Overlay.CATALYTIC_RUNE);
        Rune elementalRune = (Rune) overlay.getValue(Overlay.ELEMENTAL_RUNE);
        Long runeChangeTimer = (Long) overlay.getValue(Overlay.TIMER);
        if (guardianPower != null) {
            this.guardianPower = guardianPower;
            if (guardianPower == 0 && previousGuardianPower > 0) {
                log(GuardiansOfTheRift.class, "New game started: Guardian power decreased from " + previousGuardianPower + " to " + guardianPower);
                onNewGame();
            }
            previousGuardianPower = guardianPower;
        }
        Integer totalPoints = getTotalPoints();
        if (totalPoints != null) {
            this.totalPoints = totalPoints;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            return null;
        }
        spaceForEssence = getSpaceForEssence();

//        Integer currentWorld = getCurrentWorld();
//        if (currentWorld != null && gotrWorld != null) {
//            if (checkNoneGOTRWorld(currentWorld, gotrWorld)) {
//                return null;
//            }
//        }

        hasDegradedPouches = hasDegradedPouches();
        insideBossArea = BOSS_AREA.contains(myPosition);
        if (this.guardianPower < 80 && (hasDegradedPouches && (insideBossArea && repairType == Pouch.RepairType.APPRENTICE_CORDELIA || repairType == Pouch.RepairType.NPC_CONTACT))) {
            thinkLog("Degraded pouches found, repairing...");
            if (pouchRepairDelay == null) {
                if (repairType == Pouch.RepairType.APPRENTICE_CORDELIA && guardianPower != null && guardianPower != 0) {
                    pouchRepairDelay = new Stopwatch(random(0, 30000));
                } else if (repairType == Pouch.RepairType.NPC_CONTACT) {
                    pouchRepairDelay = new Stopwatch(random(1000, 10000));
                } else {
                    pouchRepairDelay = new Stopwatch();
                }
            }
            if (pouchRepairDelay.hasFinished()) {
                if (repairType == Pouch.RepairType.APPRENTICE_CORDELIA && !inventorySnapshot.contains(ItemID.ABYSSAL_PEARLS)) {
                    log(GuardiansOfTheRift.class, "No pearls in the inventory to repair pouches, stopping script.");
                    stop();
                    return null;
                }
                pouchRepair.poll();
                return null;
            }
        }
        if (!hasDegradedPouches) {
            pouchRepairDelay = null;
        }
        for (Pouch pouch : Pouch.values()) {
            if (inventorySnapshot.contains(pouch.getItemID())) {
                POUCHES_ACTIVE.add(pouch);
            } else {
                POUCHES_ACTIVE.remove(pouch);
            }
        }
        if (insideAltar(myPosition)) {
            return Task.HANDLE_ALTAR;
        }


        if (!insideBossArea && !MINING_SHORTCUT_AREA.contains(myPosition) && !PORTAL_MINE_AREA.contains(myPosition)) {
            return Task.ENTER_BARRIER;
        }

        /**
         * No rifts open, but timer is running
         */
        if (catalyticRune == null && elementalRune == null && runeChangeTimer != null) {
            log(GuardiansOfTheRift.class, "No rifts open, but timer is running...");
            if (insideBossArea) {
                if (hasRunes(inventorySnapshot)) {
                    thinkLog("No rifts open, but we have runes, depositing...");
                    return Task.DEPOSIT_RUNES;
                }
                if (hasCell(inventorySnapshot)) {
                    return Task.BUILD_BARRIER;
                }
            }
            if (!PORTAL_MINE_AREA.contains(myPosition) && inventorySnapshot.isFull() && inventorySnapshot.contains(ItemID.GUARDIAN_ESSENCE) && !inventorySnapshot.contains(ItemID.GUARDIAN_FRAGMENTS)) {
                thinkLog("Inventory is full, but we have essence, dropping...");
                return Task.DROP_ESSENCE;
            }
            int totalEssence = inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE);
            for (Pouch pouch : POUCHES_ACTIVE) {
                totalEssence += pouch.getCurrentCapacity();
            }
            totalEssence += inventorySnapshot.getAmount(ItemID.GUARDIAN_FRAGMENTS);

            if (totalEssence >= targetFragments) {
                if (!inventorySnapshot.isFull() || canFillPouches()) {
                    thinkLog("We have enough fragments, lets create essence");
                    return Task.CREATE_ESSENCE;
                } else {
                    thinkLog("Inventory is full and we've reached our target fragments, waiting for the rifts to open...");
                    return null;
                }
            }

            if (PORTAL_MINE_AREA.contains(myPosition)) {
                boolean inventoryFull = inventorySnapshot.isFull();
                log(GuardiansOfTheRift.class, "Inside portal area - inventory full: " + inventoryFull + ", space for essence: " + spaceForEssence);
                if (inventorySnapshot.isFull() && inventorySnapshot.contains(ItemID.GUARDIAN_ESSENCE) && canFillPouches()) {
                    thinkLog("Inventory is full, fill pouches...");
                    fillPouches();
                    return null;
                }
                if (spaceForEssence == 0) {
                    thinkLog("No space for essence, leaving portal area...");
                    return Task.ENTER_PORTAL;
                }
            }
            return Task.MINE;
        }

        boolean gameStarted = gameStarted();
        log(GuardiansOfTheRift.class, "Game started: " + gameStarted);
        /**
         * If game is finished or not started
         */
        if (!gameStarted) {
            // handle when energy is at 100 and the game hasn't fully finished
            if (guardianPower != null && guardianPower == 100 && GUARDIAN_DELAY.hasFinished()) {
                if (inventorySnapshot.containsAny(ItemID.CATALYTIC_GUARDIAN_STONE, ItemID.ELEMENTAL_GUARDIAN_STONE)) {
                    thinkLog("Lets power guardian before game fully ends...");
                    return Task.POWER_GUARDIAN;
                }
                if (insideBossArea && hasCell(inventorySnapshot)) {
                    return Task.BUILD_BARRIER;
                }
            }
            if (insideBossArea) {
                if (hasRunes(inventorySnapshot)) {
                    thinkLog("Depositing runes before game starts...");
                    return Task.DEPOSIT_RUNES;
                }
                int amountOfUnchargedCells = inventorySnapshot.getAmount(ItemID.UNCHARGED_CELL);
                if (amountOfUnchargedCells < 10 && !inventorySnapshot.isFull()) {
                    log(GuardiansOfTheRift.class, "Taking uncharged cell before game starts. Current amount: " + amountOfUnchargedCells);
                    return Task.TAKE_UNCHARGED_CELL;
                }
//                if (createBarrierAtBeginning && !builtFirstBarrier) {
//                    return Task.BUILD_BARRIER;
//                }
            }
            log(GuardiansOfTheRift.class, "Game hasn't started yet, walking to mine area...");
            return Task.MINE;
        }


        forceCraftEssence = false;
        boolean forceCraftRunes = false;
        boolean forcePowerGuardian = false;

        if (guardianPower != null && totalPoints != null) {
            forceCraftEssence = shouldForceCraftEssence(totalPoints, guardianPower);
            forceCraftRunes = shouldForceCraftRunes(totalPoints, guardianPower);
            forcePowerGuardian = shouldForcePowerGuardian(totalPoints, guardianPower);
        }
        log(GuardiansOfTheRift.class, "Force craft essence: " + forceCraftEssence + ", Force craft runes: " + forceCraftRunes + ", Force power guardian: " + forcePowerGuardian);
        // Handle the portal area
        if (PORTAL_MINE_AREA.contains(myPosition)) {
            if (forcePowerGuardian || forceCraftRunes || spaceForEssence == 0) {
                // leave
                log(GuardiansOfTheRift.class, "Leaving portal area...");
                return Task.ENTER_PORTAL;
            }
            if (fillPouches()) {
                return null;
            }
            return Task.MINE;
        }

        // take uncharged cell if we are in the boss area and don't have one
        if (insideBossArea && !inventorySnapshot.contains(ItemID.UNCHARGED_CELL) && !inventorySnapshot.isFull()) {
            log(GuardiansOfTheRift.class, "Taking uncharged cell. Current amount: " + inventorySnapshot.getAmount(ItemID.UNCHARGED_CELL));
            return Task.TAKE_UNCHARGED_CELL;
        }


        // run to portal
        Overlay.PortalInfo portalInfo = (Overlay.PortalInfo) overlay.getValue(Overlay.PORTAL);
        if (portalInfo != null) {
            boolean ignorePortal = shouldIgnorePortal(portalInfo, spaceForEssence);

            // Don't enter portal if:
            // 1. We need to force power the guardian
            // 2. We need to force craft runes
            // 3. Ignore portal is false
            if (!forcePowerGuardian && !forceCraftRunes && !ignorePortal) {
                // portal is active, prioritize entering the portal
                log(GuardiansOfTheRift.class, "Portal found: " + portalInfo.getPortal().getName() + " with time left: " + portalInfo.getSecondsRemaining());
                return Task.ENTER_PORTAL;
            } else {
                log(GuardiansOfTheRift.class, "Ignoring portal. Force power guardian: " + forcePowerGuardian + ", Force craft runes: " + forceCraftRunes + ", Ignore portal: " + ignorePortal);
            }
        }

        if (inventorySnapshot.containsAny(ItemID.CATALYTIC_GUARDIAN_STONE, ItemID.ELEMENTAL_GUARDIAN_STONE) && GUARDIAN_DELAY.hasFinished()) {
            return Task.POWER_GUARDIAN;
        }

        if (hasCell(inventorySnapshot)) {
            return Task.BUILD_BARRIER;
        }

        if (totalPoints != null && guardianPower != null) {
            boolean hasEnoughForGoal = hasEnoughForGoal(totalPoints);
            boolean hasReachedGoal = hasReachedGoal(totalPoints);
            if (!hasReachedGoal && guardianPower >= forceGoalPercentage && hasEnoughForGoal) {
                thinkLog("Guardian power is high enough and we have reached our goal, lets enter the altar...");
                return Task.ENTER_ALTAR;
            }

        }

        if (forceCraftRunes) {
            thinkLog("Forcing crafting runes to reach goal, entering altar...");
            return Task.ENTER_ALTAR;
        }

        if (GUARDIAN_AREA.contains(myPosition) && inventorySnapshot.contains(ItemID.GUARDIAN_ESSENCE) && inventorySnapshot.getFreeSlots() < 3) {
            thinkLog("We are nearby the guardian, prioritizing entering altar to create essence...");
            return Task.ENTER_ALTAR;
        }

        if (amountOfRunesInInventory(inventorySnapshot) >= nextRuneDepositAmount && insideBossArea) {
            return Task.DEPOSIT_RUNES;
        }

        boolean riftsOpen = catalyticRune != null && elementalRune != null;

        if (riftsOpen && spaceForEssence == 0) {
            thinkLog("There is no space for essence, I'm going to enter the altar...");
            return Task.ENTER_ALTAR;
        }

        int freeSlots = inventorySnapshot.getFreeSlots();
        int fragments = inventorySnapshot.getAmount(ItemID.GUARDIAN_FRAGMENTS);
        log(GuardiansOfTheRift.class, "Fragments: " + fragments + ", Free slots: " + freeSlots + " | Force craft essence: " + forceCraftEssence);
        if (fragments > 0 && fragments >= spaceForEssence || forceCraftEssence) {
            return Task.CREATE_ESSENCE;
        }

        if (!PORTAL_MINE_AREA.contains(myPosition) && inventorySnapshot.isFull() && fragments <= 0) {
            // drop essence for shards
            return Task.DROP_ESSENCE;
        }
        return Task.MINE;
    }

    private void updateTotalPoints() {
        Integer totalPoints = getTotalPoints();
        if (totalPoints != null) {
            this.totalPoints = totalPoints;
        }
    }

    private boolean shouldForceCraftEssence(Integer totalPoints, Integer guardianPower) {
        boolean reachedGoal = hasReachedGoal(totalPoints);
        if (!reachedGoal && guardianPower >= forceGoalPercentage) {
            int essencePoints = getPointsForEssence();
            int fragmentsPoints = inventorySnapshot.getAmount(ItemID.GUARDIAN_FRAGMENTS) * 2;
            if (essencePoints + fragmentsPoints + totalPoints >= MINIMUM_POINT_REQUIREMENT) {
                log(GuardiansOfTheRift.class, "Inventory has enough for minimum point requirement & not long left! stopping mining.");
                return true;
            }
        }
        return false;
    }

    private boolean shouldForceCraftRunes(Integer totalPoints, Integer guardianPower) {
        boolean reachedGoal = hasReachedGoal(totalPoints);
        if (!reachedGoal && guardianPower >= forceGoalPercentage) {
            int essencePoints = getPointsForEssence();
            if (essencePoints + totalPoints >= MINIMUM_POINT_REQUIREMENT) {
                log(GuardiansOfTheRift.class, "Inventory has enough for minimum point requirement & not long left!.");
                return true;
            }
        }
        return false;
    }

    private boolean shouldForcePowerGuardian(Integer totalPoints, Integer guardianPower) {
        boolean reachedGoal = hasReachedGoal(totalPoints);
        if (!reachedGoal && guardianPower >= forceGoalPercentage) {
            int pointsForGuardianStones = getPointsForGuardianStones();
            if (pointsForGuardianStones + totalPoints >= MINIMUM_POINT_REQUIREMENT) {
                log(GuardiansOfTheRift.class, "Inventory has enough for minimum point requirement & not long left!");
                return true;
            }
        }
        return false;
    }

    private boolean shouldIgnorePortal(Overlay.PortalInfo portalInfo, int spaceForEssence) {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null, cannot determine if we should ignore portal.");
            return true;
        }
        Portal portal = portalInfo.getPortal();
        RSObject workbench = getObjectManager().getClosestObject("Workbench");
        Integer tileDistanceToWorkbench = null;
        if (workbench != null) {
            tileDistanceToWorkbench = workbench.getTileDistance(worldPosition);
        }
        Integer tileDistanceToPortal = getTileDistanceToWorldPosition(portal.getPosition());

        boolean ignorePortal = spaceForEssence == 0;
        if (!ignorePortal && tileDistanceToWorkbench != null) {
            if (tileDistanceToPortal != null) {
                boolean closerToWorkbench = tileDistanceToWorkbench < tileDistanceToPortal;
                ignorePortal = closerToWorkbench && spaceForEssence < 8;
            }
            boolean nextToWorkbench = tileDistanceToWorkbench <= 2;
            if (nextToWorkbench && spaceForEssence <= 14) {
                ignorePortal = true;
            }
        }
        return ignorePortal;
    }

    private void thinkLog(String message) {
        log(GuardiansOfTheRift.class, "[THINKING] " + message);
    }

    private Integer getTotalPoints() {
        Integer catalyticPoints = (Integer) overlay.getValue(Overlay.CATALYTIC_POINTS);
        Integer elementalPoints = (Integer) overlay.getValue(Overlay.ELEMENTAL_POINTS);
        if (catalyticPoints == null || elementalPoints == null) {
            return null;
        }
        return catalyticPoints + elementalPoints;
    }

    private int getPointsForEssence() {
        int points = inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE) * 2;
        for (Pouch pouch : Pouch.values()) {
            if (inventorySnapshot.contains(pouch.getItemID())) {
                points += pouch.getCurrentCapacity() * 2;
            }
        }
        return points;
    }

    private int getPointsForGuardianStones() {
        return inventorySnapshot.getAmount(ItemID.CATALYTIC_GUARDIAN_STONE, ItemID.ELEMENTAL_GUARDIAN_STONE) * 2;
    }

    private Boolean hasReachedGoal(int totalPoints) {
        return totalPoints >= MINIMUM_POINT_REQUIREMENT;
    }

    private Boolean hasEnoughForGoal(int totalPoints) {
        int pointsNeeded = MINIMUM_POINT_REQUIREMENT - totalPoints;
        int pointsForEssence = getPointsForEssence();
        return pointsForEssence >= pointsNeeded || pointsNeeded <= 0;
    }

    private boolean hasDegradedPouches() {
        for (Pouch pouch : Pouch.values()) {
            if (pouch == Pouch.SMALL) {
                continue; // Small pouch doesn't degrade
            }
            if (inventorySnapshot.contains(pouch.getDegradedItemID())) {
                return true;
            }
        }
        return false;
    }

    private boolean checkNoneGOTRWorld(Integer currentWorld, World gotrWorld) {
        if (gotrWorld.getId() != currentWorld) {
            // hop back to GOTR world
            log(GuardiansOfTheRift.class, "Current world is not a GOTR world: " + currentWorld + " Hopping back to world " + gotrWorld.getId());
            getProfileManager().forceHop(worlds -> {
                List<World> gotrWorlds = World.getWorlds().stream().filter(world -> {
                    String activity = world.getActivity();
                    return activity != null && activity.toLowerCase().contains("guardians of the rift");
                }).sorted(Comparator.comparingInt(World::getPlayerCount).reversed()).limit(2).toList();
                if (gotrWorlds.isEmpty()) {
                    return null;
                }
                return gotrWorlds.get(random(gotrWorlds.size()));
            });
        }
        return false;
    }

    private void depositRunes() {
        RSObject depositPool = getObjectManager().getClosestObject("Deposit Pool");
        if (depositPool == null) {
            log("No deposit pool found inside our loaded scene...");
            return;
        }
        BooleanSupplier breakCondition = () -> {
            WorldPosition myPosition = getWorldPosition();
            if (myPosition == null) {
                log(GuardiansOfTheRift.class, "My position is null.");
                return false;
            }
            insideBossArea = BOSS_AREA.contains(myPosition);
            return !insideBossArea;
        };
        if (!depositPool.interact(breakCondition, "deposit-runes")) {
            log("Failed to interact with weak deposit pool...");
            return;
        }
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition != null) {
            if (depositPool.getTileDistance(worldPosition) > 1) {
                // wait until we start moving towards the deposit pool
                submitTask(() -> getLastPositionChangeMillis() < 300, random(2000, 4000));
            }
        }
        int stopMovingTimeout = random(800, 1300);
        boolean result = submitTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            if (getLastPositionChangeMillis() > stopMovingTimeout) {
                log(GuardiansOfTheRift.class, "Stopped moving towards deposit pool.");
                return true;
            }
            return !hasRunes(inventorySnapshot);
        }, random(8000, 14000));
        if (result && guardianPower < 85) {
            nextRuneDepositAmount = random(1, 3);
            submitTask(() -> false, RandomUtils.gaussianRandom(400, 2000, 300, 500));
        }
    }

    private boolean hasRunes(ItemGroupResult inventorySnapshot) {
        for (Rune rune : ElementalRune.values()) {
            if (inventorySnapshot.contains(rune.getRuneId())) {
                return true;
            }
        }
        for (Rune rune : CatalyticRune.values()) {
            if (inventorySnapshot.contains(rune.getRuneId())) {
                return true;
            }
        }
        return false;
    }

    private int amountOfRunesInInventory(ItemGroupResult inventorySnapshot) {
        int amount = 0;
        for (Rune rune : ElementalRune.values()) {
            if (inventorySnapshot.contains(rune.getRuneId())) {
                amount++;
            }
        }
        for (Rune rune : CatalyticRune.values()) {
            if (inventorySnapshot.contains(rune.getRuneId())) {
                amount++;
            }
        }
        return amount;
    }

    private void createEssence() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null.");
            return;
        }
        if (MINING_SHORTCUT_AREA.contains(myPosition)) {
            handleShortcut();
            return;
        }
        if (fillPouches()) {
            return;
        }
        if (inventorySnapshot.isFull()) {
            log(GuardiansOfTheRift.class, "Inventory is full.");
            return;
        }
        RSObject workbench = getObjectManager().getClosestObject("Workbench");
        if (workbench == null) {
            log(GuardiansOfTheRift.class, "No workbench found to create essence.");
            return;
        }
        Stopwatch talismanDropDelay = new Stopwatch(random(1000, 4000));

        BooleanSupplier breakCondition = () -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            if (PORTAL_MINE_AREA.contains(myPosition)) {
                return true;
            }
            // place the cell before, as if not we create essence then place the cell, which leads to us crafting runes without filling pouches
            insideBossArea = BOSS_AREA.contains(myPosition);
            if (insideBossArea && hasCell(inventorySnapshot)) {
                return true;
            }
            if (talismanDropDelay.hasFinished() && dropItems()) {
                return true;
            }
            Overlay.PortalInfo portalInfo = (Overlay.PortalInfo) overlay.getValue(Overlay.PORTAL);
            if (portalInfo == null) {
                return false;
            }
            spaceForEssence = getSpaceForEssence();
            updateTotalPoints();
            return !shouldIgnorePortal(portalInfo, spaceForEssence);
        };
        if (!workbench.interact(breakCondition, "work-at")) {
            log(GuardiansOfTheRift.class, "Failed to interact with workbench.");
            return;
        }

        boolean fillWhenFull = random(3) < 2; // 66% chance to fill when full
        AtomicReference<Stopwatch> pouchFillDelay = new AtomicReference<>(null);
        if (!CollisionManager.isCardinallyAdjacent(myPosition, workbench.getWorldPosition())) {
            // wait until we start moving towards the workbench
            submitTask(() -> getLastPositionChangeMillis() < 1000, random(2000, 4000));
        }
        Timer amountChangeTimer = new Timer();
        AtomicInteger previousAmount = new AtomicInteger(-1);
        submitHumanTask(() -> {
            WorldPosition worldPosition = getWorldPosition();
            if (worldPosition == null) {
                log(GuardiansOfTheRift.class, "My position is null.");
                return false;
            }
            int tileDistance = workbench.getTileDistance(worldPosition);
            if (tileDistance > 1) {
                // still traversing to the rock
                amountChangeTimer.reset();
                log(GuardiansOfTheRift.class, "Still walking to rock. Tile distance: " + tileDistance);
                if (getLastPositionChangeMillis() > 2000) {
                    return true;
                } else {
                    return false;
                }
            }

            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            if (inventorySnapshot.isFull()) {
                return true;
            }
            if (talismanDropDelay.hasFinished() && dropItems()) {
                return true;
            }
            spaceForEssence = getSpaceForEssence();
            Overlay.PortalInfo portalInfo = (Overlay.PortalInfo) overlay.getValue(Overlay.PORTAL);
            if (portalInfo != null) {
                boolean ignorePortal = shouldIgnorePortal(portalInfo, spaceForEssence);
                if (!ignorePortal) {
                    log(GuardiansOfTheRift.class, "Prioritising entering portal...");
                    return true;
                }
            }
            updateOverlayValues();
            Integer guardianPower = (Integer) overlay.getValue(Overlay.GUARDIAN_POWER);
            Integer totalPoints = getTotalPoints();
            if (totalPoints != null) {
                this.totalPoints = totalPoints;
                if (guardianPower != null) {
                    boolean hasReachedGoal = hasReachedGoal(totalPoints);
                    boolean hasEnoughForGoal = hasEnoughForGoal(totalPoints);
                    if (!hasReachedGoal && guardianPower >= forceGoalPercentage && hasEnoughForGoal) {
                        log(GuardiansOfTheRift.class, "Need to enter altar before the game ends...");
                        return true;
                    }
                }
            }

            if (!fillWhenFull && canFillPouches()) {
                if (pouchFillDelay.get() == null) {
                    pouchFillDelay.set(new Stopwatch(random(200, 5000)));
                } else if (pouchFillDelay.get().hasFinished()) {
                    if (fillPouches()) {
                        return true;
                    }
                }
            }
            if (amountChangeTimer.timeElapsed() > essenceChangeTimeout) {
                log(GuardiansOfTheRift.class, "Amount change timeout");
                this.essenceChangeTimeout = random(1500, 3500);
                return true;
            }
            int essence = inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE);
            if (previousAmount.get() == -1) {
                previousAmount.set(essence);
            }
            if (essence > previousAmount.get()) {
                amountChangeTimer.reset();
                previousAmount.set(essence);
            }
            return false; // still creating essence
        }, random(16000, 25000));
        if (random(7) == 0 && guardianPower < 85) {
            // longer delay
            int delay = RandomUtils.exponentialRandom(2500, 500, 6000);
            log(GuardiansOfTheRift.class, "⏳ → human(" + delay + "ms)");
            submitTask(() -> {
                getScreen().getDrawableCanvas().drawRect(getScreen().getBounds(), Color.GREEN.getRGB(), 0.3);
                return false;
            }, delay);

        }

    }

    private boolean canFillPouches() {
        int essenceAmount = inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE);
        if (essenceAmount <= 0) {
            log(GuardiansOfTheRift.class, "No essence to fill pouches.");
            return false;
        }

        for (Pouch pouch : Pouch.values()) {
            if (!pouch.isFull() && inventorySnapshot.contains(pouch.getItemID()) && (pouch == Pouch.COLOSSAL || essenceAmount >= pouch.getSpaceLeft())) {
                return true;
            }
        }
        return false;
    }

    private boolean fillPouches() {
        if (!fillPouchDelay.hasFinished()) {
            return false;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(GuardiansOfTheRift.class, "Inventory snapshot is null, can't fill pouches.");
            return false;
        }
        final int initialEssenceAmount = inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE);
        AtomicInteger essenceAmount = new AtomicInteger(inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE));
        if (essenceAmount.get() <= 0) {
            log(GuardiansOfTheRift.class, "No essence to fill pouches.");
            return false;
        }
        log(GuardiansOfTheRift.class, "Initial essence amount: " + essenceAmount.get());
        Map<Pouch, ItemSearchResult> pouchesToFill = new HashMap<>();
        for (Pouch pouch : Pouch.values()) {
            ItemSearchResult pouchResult = inventorySnapshot.getItem(pouch.getItemID());
            if (pouchResult != null && pouch.getSpaceLeft() > 0) {
                pouchesToFill.put(pouch, pouchResult);
            }
        }

        if (pouchesToFill.isEmpty()) {
            log(GuardiansOfTheRift.class, "No pouches to fill.");
            return false;
        }
        log(GuardiansOfTheRift.class, "Pouches to fill: " + pouchesToFill.size() + " Essence amount: " + essenceAmount.get());
        List<Map.Entry<Pouch, ItemSearchResult>> entries = new ArrayList<>(pouchesToFill.entrySet());
        Collections.shuffle(entries);
        boolean filledPouches = false;
        boolean isColossal = entries.size() == 1 && entries.get(0).getKey() == Pouch.COLOSSAL;
        if (isColossal) {
            log(GuardiansOfTheRift.class, "Handling colossal");
            // handle colossal pouch separately
            Map.Entry<Pouch, ItemSearchResult> entry = entries.get(0);
            Pouch pouch = entry.getKey();
            ItemSearchResult pouchResult = entry.getValue();
            if (!pouch.isFull() && (inventorySnapshot.isFull() || essenceAmount.get() >= pouch.getSpaceLeft())) {
                if (getFinger().tap(false, pouchResult)) {
                    log(GuardiansOfTheRift.class, "Essence: " + essenceAmount.get() + " Pouch space left: " + pouch.getSpaceLeft() + " Inventory full: " + inventorySnapshot.isFull());
                    if (essenceAmount.get() >= pouch.getSpaceLeft()) {
                        // fill the pouch to max capacity
                        essenceAmount.set(essenceAmount.get() - pouch.getSpaceLeft());
                        pouch.setCurrentCapacity(pouch.getMaxCapacity());
                        log(GuardiansOfTheRift.class, "New capacity: " + pouch.getCurrentCapacity());
                    } else if (inventorySnapshot.isFull()) {
                        pouch.setCurrentCapacity(pouch.getCurrentCapacity() + essenceAmount.get());
                        essenceAmount.set(0);
                        log(GuardiansOfTheRift.class, "New capacity-: " + pouch.getCurrentCapacity());
                    }
                    // wait until pouches are filled
                    log(GuardiansOfTheRift.class, "Waiting for pouch to be filled...");
                    submitTask(() -> {
                        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                        if (inventorySnapshot == null) {
                            return false;
                        }
                        boolean hasDegraded = inventorySnapshot.contains(pouch.getDegradedItemID());
                        if (hasDegraded && pouch.getCurrentCapacity() > pouch.getDegradedCapacity()) {
                            log(GuardiansOfTheRift.class, "Pouch is degraded but has more capacity than degraded capacity, clamping to degraded capacity.");
                            pouch.setCurrentCapacity(pouch.getDegradedCapacity());
                        }
                        int currentAmount = inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE);
                        log(GuardiansOfTheRift.class, "Expected amount: " + essenceAmount.get() + " current amount: " + currentAmount);
                        return Math.abs(currentAmount - essenceAmount.get()) < 2; // allow for a small difference due to the way essence is filled
                    }, random(2000, 3000));
                    if (inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE) == initialEssenceAmount) {
                        Pouch.COLOSSAL.setCurrentCapacity(Pouch.COLOSSAL.getMaxCapacity());
                    }
                    int delay = RandomUtils.weightedRandom(100, 2500, 0.003);
                    log(GuardiansOfTheRift.class, "⏳ → human(" + delay + "ms)");
                    submitTask(() -> {
                        Canvas canvas = getScreen().getDrawableCanvas();
                        canvas.fillRect(getScreen().getBounds(), Color.GREEN.getRGB(), 0.2);
                        return false;
                    }, delay);
                    return true;
                }
            }
        } else {
            for (Map.Entry<Pouch, ItemSearchResult> entry : entries) {
                ItemSearchResult pouchResult = entry.getValue();
                Pouch pouch = entry.getKey();
                if (!pouch.isFull() && essenceAmount.get() >= pouch.getSpaceLeft()) {
                    if (getFinger().tap(false, pouchResult)) {
                        filledPouches = true;
                        submitTask(() -> {
                            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                            if (inventorySnapshot == null) {
                                return false;
                            }
                            boolean hasDegraded = pouch != Pouch.SMALL && inventorySnapshot.contains(pouch.getDegradedItemID());
                            log(GuardiansOfTheRift.class, "Pouch: " + pouch + " Degraded: " + hasDegraded);
                            int amountOfEssence = inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE);
                            if (amountOfEssence > essenceAmount.get()) {
                                essenceAmount.set(amountOfEssence);
                            } else if (amountOfEssence < essenceAmount.get()) {
                                log(GuardiansOfTheRift.class, "Essence amount decreased.");
                                pouch.setCurrentCapacity(hasDegraded ? pouch.getDegradedCapacity() : pouch.getMaxCapacity());
                                essenceAmount.set(amountOfEssence);
                                int delay = RandomUtils.weightedRandom(100, 2500, 0.003);
                                log(GuardiansOfTheRift.class, "⏳ → human(" + delay + "ms)");
                                submitTask(() -> {
                                    Canvas canvas = getScreen().getDrawableCanvas();
                                    canvas.fillRect(getScreen().getBounds(), Color.GREEN.getRGB(), 0.2);
                                    return false;
                                }, delay);
                                return true; // stop filling if essence amount decreased
                            }
                            return false;
                        }, random(3000, 4000));

                        if (inventorySnapshot.getAmount(ItemID.GUARDIAN_ESSENCE) == initialEssenceAmount) {
                            log(GuardiansOfTheRift.class, "Essence amount didn't change, setting pouch to max capacity.");
                            pouch.setCurrentCapacity(Pouch.COLOSSAL.getMaxCapacity());
                        }
                        // set full
                    }
                }
            }
            return filledPouches;
        }
        return false;
    }

    private boolean emptyPouches() {
        int freeSlots = inventorySnapshot.getFreeSlots();
        int initialFreeSlots = freeSlots;
        if (freeSlots <= 0) {
            log(GuardiansOfTheRift.class, "Inventory is full, can't empty pouches.");
            return false;
        }

        Map<Pouch, ItemSearchResult> pouchesToEmpty = new HashMap<>();
        for (Pouch pouch : Pouch.values()) {
            ItemSearchResult pouchResult;
            if (pouch == Pouch.SMALL) {
                pouchResult = inventorySnapshot.getItem(pouch.getItemID());
            } else {
                pouchResult = inventorySnapshot.getItem(pouch.getItemID(), pouch.getDegradedItemID());
            }
            if (pouchResult != null && pouch.getCurrentCapacity() > 0) {
                pouchesToEmpty.put(pouch, pouchResult);
            }
        }

        if (pouchesToEmpty.isEmpty()) {
            log(GuardiansOfTheRift.class, "No pouches to empty.");
            return false;
        }

        List<Map.Entry<Pouch, ItemSearchResult>> entries = new ArrayList<>(pouchesToEmpty.entrySet());
        Collections.shuffle(entries);
        for (Map.Entry<Pouch, ItemSearchResult> entry : entries) {
            if (freeSlots == 0) {
                log(GuardiansOfTheRift.class, "No free slots left to empty pouches.");
                break;
            }
            ItemSearchResult pouchResult = entry.getValue();
            Pouch pouch = entry.getKey();
            int pouchCapacity = pouch.getCurrentCapacity();
            if (pouchCapacity > 0) {
                // if inv has essence and the fill option isn't ni first index, pouch is full
                if (getFinger().tap(false, pouchResult)) {
                    if (freeSlots > pouchCapacity) {
                        freeSlots -= pouchCapacity;
                        pouch.setCurrentCapacity(0);
                    } else {
                        pouch.deductFromCurrentCapacity(freeSlots);
                        freeSlots = 0;
                    }
                    int delay = RandomUtils.weightedRandom(300, 4500, 0.0042);
                    log(GuardiansOfTheRift.class, "Executing delay after emptying pouch: " + delay + "ms");
                    submitTask(() -> {
                        Canvas canvas = getScreen().getDrawableCanvas();
                        canvas.fillRect(getScreen().getBounds(), Color.GREEN.getRGB(), 0.2);
                        return false;
                    }, delay);
                }
            }
        }
        int finalFreeSlots = freeSlots;
        submitTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            int freeSlots1 = inventorySnapshot.getFreeSlots();
            if (POUCHES_ACTIVE.contains(Pouch.COLOSSAL)) {
                if (freeSlots1 > 0 && Math.abs(freeSlots1 - initialFreeSlots) > 1) {
                    Pouch.COLOSSAL.setCurrentCapacity(0);
                    return true;
                }
            }
            return freeSlots1 == finalFreeSlots && inventorySnapshot.contains(ItemID.GUARDIAN_ESSENCE); // check if we have free slots left
        }, random(1500, 2500));
        return true;
    }

    private void enterAltar() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null.");
            return;
        }
        if (MINING_SHORTCUT_AREA.contains(myPosition)) {
            handleShortcut();
            return;
        }
        RuneToCraft runeToCraft = getRuneToCraft();
        if (runeToCraft == null) return;

        Rune rune = runeToCraft.getRune();

        RSObject runeGuardian = getObjectManager().getClosestObject(rune.getGuardianName());
        if (runeGuardian == null) {
            log(GuardiansOfTheRift.class, "No rune guardian found to craft runes.");
            return;
        }
        prioritisedAltarRegion = rune.getAltarRegionId();
        BooleanSupplier breakCondition = null;
        if (!runeToCraft.isTalisman) {
            breakCondition = () -> hasRuneChanged(rune);
        }
        if (!runeGuardian.interact(breakCondition, "Enter")) {
            log(GuardiansOfTheRift.class, "Failed to interact with rune guardian: " + runeGuardian.getName());
            return;
        }
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition != null) {
            if (runeGuardian.getTileDistance(worldPosition) > 1) {
                // if more than 1 tile away, wait until we start moving towards the rune guardian
                boolean moving = submitTask(() -> getLastPositionChangeMillis() < 800, random(1400, 2500));
                if (!moving) {
                    log(GuardiansOfTheRift.class, "Not moving, breaking out of interaction.");
                    return;
                }
            }
        }
        setExpectedRegionId(rune.getAltarRegionId());
        submitTask(() -> {
            // wait until we are inside the altar
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            if (insideAltar(myPosition_)) {
                return true;
            }
            return !runeToCraft.isTalisman && hasRuneChanged(rune);
        }, random(10000, 13000));
        if (guardianPower < 85) {
            submitHumanTask(() -> false, RandomUtils.weightedRandom(300, 4500, 0.0042));
        } else {
            submitHumanTask(() -> false, RandomUtils.weightedRandom(300, 1500, 0.0042));
        }
    }

    private List<Rune> getTalismans(ItemGroupResult inventorySnapshot) {
        if (inventorySnapshot == null) {
            log(GuardiansOfTheRift.class, "Inventory snapshot is null, can't get talismans.");
            return Collections.emptyList();
        }
        List<Rune> talismans = new ArrayList<>();
        for (Rune rune : ElementalRune.values()) {
            if (inventorySnapshot.contains(rune.getTalismanId())) {
                talismans.add(rune);
            }
        }
        for (Rune rune : CatalyticRune.values()) {
            if (inventorySnapshot.contains(rune.getTalismanId())) {
                talismans.add(rune);
            }
        }
        return talismans;
    }

    private RuneToCraft getRuneToCraft() {
        Rune runeToCraft;
        Integer catalyticPoints = (Integer) overlay.getValue(Overlay.CATALYTIC_POINTS);
        Integer elementalPoints = (Integer) overlay.getValue(Overlay.ELEMENTAL_POINTS);

        Rune catalyticRune = (Rune) overlay.getValue(Overlay.CATALYTIC_RUNE);
        Rune elementalRune = (Rune) overlay.getValue(Overlay.ELEMENTAL_RUNE);

        List<Rune> talismans = getTalismans(inventorySnapshot);

        if (catalyticPoints == null || elementalPoints == null || catalyticRune == null || elementalRune == null) {
            log(GuardiansOfTheRift.class, "Problem reading overlay values, catalyticPoints: " + catalyticPoints + ", elementalPoints: " + elementalPoints + ", catalyticRune: " + catalyticRune + ", elementalRune: " + elementalRune);
            return null;
        }
        log(GuardiansOfTheRift.class, "Deciding which rune to craft. Strategy: " + selectedStrategy + ", Catalytic points: " + catalyticPoints + ", Elemental points: " + elementalPoints);
        switch (selectedStrategy) {
            case CATALYTIC -> runeToCraft = catalyticRune;
            case ELEMENTAL -> runeToCraft = elementalRune;
            case BALANCED -> runeToCraft = catalyticPoints <= elementalPoints ? catalyticRune : elementalRune;
            case MAXIMUM_POINTS -> {
                if (catalyticRune.getTier() > elementalRune.getTier()) {
                    runeToCraft = catalyticRune;
                } else if (elementalRune.getTier() > catalyticRune.getTier()) {
                    runeToCraft = elementalRune;
                } else {
                    // same tier, choose the rune with the lowest points
                    runeToCraft = catalyticPoints <= elementalPoints ? catalyticRune : elementalRune;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + selectedStrategy);
        }
        log(GuardiansOfTheRift.class, "Rune to craft: " + runeToCraft);
        // if we have talismans, check if we can upgrade the rune to craft based on tier
        boolean isTalisman = false;
        if (!talismans.isEmpty()) {
            log(GuardiansOfTheRift.class, "We have talismans, checking if we can upgrade the rune to craft based on tier.");
            if (runeToCraft instanceof CatalyticRune) {
                for (Rune talisman : talismans) {
                    if (talisman instanceof CatalyticRune && talisman.getTier() > runeToCraft.getTier()) {
                        log(GuardiansOfTheRift.class, "Found a talisman with higher tier: " + talisman + ", upgrading rune to craft.");
                        isTalisman = true;
                        runeToCraft = talisman;
                        break;
                    }
                }
            } else if (runeToCraft instanceof ElementalRune) {
                for (Rune talisman : talismans) {
                    if (talisman instanceof ElementalRune && talisman.getTier() > runeToCraft.getTier()) {
                        log(GuardiansOfTheRift.class, "Found a talisman with higher tier: " + runeToCraft + ", upgrading rune to craft.");
                        isTalisman = true;
                        runeToCraft = talisman;
                        break;
                    }
                }
            }
            log(GuardiansOfTheRift.class, "Checking type focused talismans for upgrade.");
            if (selectedStrategy == PointStrategy.CATALYTIC) {
                for (Rune talisman : talismans) {
                    if (talisman instanceof CatalyticRune) {
                        if (runeToCraft instanceof ElementalRune || talisman.getTier() > runeToCraft.getTier()) {
                            log(GuardiansOfTheRift.class, "Found a catalytic talisman with higher tier: " + talisman + ", upgrading rune to craft.");
                            isTalisman = true;
                            runeToCraft = talisman;
                            break;
                        }
                    }
                }
            } else if (selectedStrategy == PointStrategy.ELEMENTAL) {
                for (Rune talisman : talismans) {
                    if (talisman instanceof ElementalRune) {
                        if (runeToCraft instanceof CatalyticRune || talisman.getTier() > runeToCraft.getTier()) {
                            log(GuardiansOfTheRift.class, "Found a elemental talisman with higher tier: " + talisman + ", upgrading rune to craft.");
                            isTalisman = true;
                            runeToCraft = talisman;
                            break;
                        }
                    }
                }
            }
        }

        if (!isTalisman) {
            // handle if we can't craft this rune, then select the opposite
            if (ElementalRune.isElemental(runeToCraft) && runecraftingLevel < runeToCraft.getLevelRequirement()) {
                log(GuardiansOfTheRift.class, "We can't craft: " + runeToCraft + ", switching to catalytic rune.");
                runeToCraft = catalyticRune;
            } else if (CatalyticRune.isCatalytic(runeToCraft) && (runecraftingLevel < runeToCraft.getLevelRequirement() || GUARDIANS_TO_IGNORE.contains((CatalyticRune) runeToCraft))) {
                log(GuardiansOfTheRift.class, "We can't craft: " + runeToCraft + ", switching to elemental rune.");
                runeToCraft = elementalRune;
            }
        }

        return new RuneToCraft(runeToCraft, isTalisman);
    }

    private boolean hasRuneChanged(Rune runeToCraft) {
        Rune catalyticRune_ = (Rune) overlay.getValue(Overlay.CATALYTIC_RUNE);
        Rune elementalRune_ = (Rune) overlay.getValue(Overlay.ELEMENTAL_RUNE);
        if (catalyticRune_ == null || elementalRune_ == null) {
            log(GuardiansOfTheRift.class, "Problem reading overlay values, catalyticPoints: " + "CatalyticRune: " + catalyticRune_ + "ElementalRune: " + elementalRune_);
            return false;
        }
        if (catalyticRune_ != runeToCraft && elementalRune_ != runeToCraft) {
            log(GuardiansOfTheRift.class, "Active runes changed, breaking out of interaction with rune guardian.");
            return true;
        }
        return false;
    }

    private void handleAltar() {
        if (inventorySnapshot.contains(ItemID.GUARDIAN_ESSENCE) || runesInPouches()) {
            craftRunes();
        } else {
            // leave
            leaveAltar();
        }
    }

    private List<Integer> getItemsToDrop(ItemGroupResult inventorySnapshot) {
        if (inventorySnapshot == null) {
            log(GuardiansOfTheRift.class, "Inventory snapshot is null, can't get items to drop.");
            return Collections.emptyList();
        }
        List<Integer> itemsToDrop = new ArrayList<>();
        itemsToDrop.addAll(getTalismansToDrop(inventorySnapshot));
        itemsToDrop.addAll(getCellsToDrop(inventorySnapshot));
        return itemsToDrop;
    }

    private List<Integer> getCellsToDrop(ItemGroupResult inventorySnapshot) {
        List<Integer> cellsToDrop = new ArrayList<>();
        // drop cells if tier is medium or below
        for (Rune.Cell cell : Rune.Cell.values()) {
            if (cell.getTier() <= cellDropThreshold && inventorySnapshot.contains(cell.getItemID())) {
                cellsToDrop.add(cell.getItemID());
            }
        }
        return cellsToDrop;
    }

    private List<Integer> getTalismansToDrop(ItemGroupResult inventorySnapshot) {
        List<Integer> talismansToDrop = new ArrayList<>();
        // drop talismans if tier is medium or below
        List<Rune> runes = new ArrayList<>();
        runes.addAll(Arrays.asList(CatalyticRune.values()));
        runes.addAll(Arrays.asList(ElementalRune.values()));
        for (Rune talisman : runes) {
            boolean containsTalisman = inventorySnapshot.contains(talisman.getTalismanId());
            if (!useTalismans && containsTalisman) {
                talismansToDrop.add(talisman.getTalismanId());
                continue;
            }
            if (talisman instanceof CatalyticRune && selectedStrategy == PointStrategy.CATALYTIC) {
                // ignore catalytic talismans if our strategy is catalytic
                continue;
            }
            if (containsTalisman && talisman.getTier() <= 2) {
                talismansToDrop.add(talisman.getTalismanId());
            }
        }
        return talismansToDrop;
    }

    private void leaveAltar() {
        log(GuardiansOfTheRift.class, "No guardian essence found, leaving altar.");
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null.");
            return;
        }
        RSObject portal = getPortalForRegion(myPosition);
        if (portal == null) {
            log(GuardiansOfTheRift.class, "No portal found to leave.");
            return;
        }
        /**
         * We have a 66% chance to drop talismans in the altar, we also add a delay to the drop action so that we aren't dropping them at the same time every time.
         */
        boolean dropTalismansInAltar = random(3) < 2; // 66% chance to drop talismans in altar
        final Stopwatch dropDelay = new Stopwatch(random(500, 3000));
        BooleanSupplier dropTalismanSupplier = dropTalismansInAltar ? () -> {
            // check if we have talismans to drop
            if (!dropDelay.hasFinished()) {
                return false;
            }
            return dropItems();
        } : null;

        if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return;
        }
        if (!portal.interact(dropTalismanSupplier, "Use")) {
            log(GuardiansOfTheRift.class, "Failed to interact with exit altar: " + portal.getName());
            return;
        }

        setExpectedRegionId(GOTR_REGION_ID);
        submitHumanTask(() -> {
            // wait until we are outside the altar
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            return !insideAltar(myPosition_);
        }, random(8000, 14000));
    }

    private boolean runesInPouches() {
        for (Pouch pouch : POUCHES_ACTIVE) {
            if (pouch.getCurrentCapacity() > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean dropItems() {
        ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(GuardiansOfTheRift.class, "Inventory snapshot is null, can't drop items.");
            return false;
        }
        List<Integer> itemsToDrop = getItemsToDrop(inventorySnapshot);
        if (itemsToDrop.isEmpty()) {
            //log(GuardiansOfTheRift.class, "No items to drop.");
            return false;
        }
        log(GuardiansOfTheRift.class, "Dropping items: " + itemsToDrop);
        Collections.shuffle(itemsToDrop);
        Integer itemToDrop = itemsToDrop.get(0);
        if (!getWidgetManager().getInventory().dropItem(itemToDrop, 1)) {
            log(GuardiansOfTheRift.class, "Failed to drop itemToDrop: " + itemToDrop);
            return false;
        }
        return true;
    }

    private RSObject getAltarForRegion(WorldPosition worldPosition) {
        Optional<RSObject> altarOpt = getObjectManager().getObject(obj -> {
            String objectName = obj.getName();
            if (objectName == null || !objectName.equalsIgnoreCase("altar")) {
                return false;
            }
            WorldPosition objectPosition = obj.getWorldPosition();
            return objectPosition != null && objectPosition.getRegionID() == worldPosition.getRegionID();
        });
        if (altarOpt.isEmpty()) {
            log(GuardiansOfTheRift.class, "No altar found in region: " + worldPosition.getRegionID());
            return null;
        }
        return altarOpt.get();
    }

    private RSObject getPortalForRegion(WorldPosition worldPosition) {
        Optional<RSObject> altarOpt = getObjectManager().getObject(obj -> {
            String objectName = obj.getName();
            if (objectName == null || !objectName.equalsIgnoreCase("portal")) {
                return false;
            }
            WorldPosition objectPosition = obj.getWorldPosition();
            return objectPosition != null && objectPosition.getRegionID() == worldPosition.getRegionID();
        });
        if (altarOpt.isEmpty()) {
            log(GuardiansOfTheRift.class, "No portal found in region: " + worldPosition.getRegionID());
            return null;
        }
        return altarOpt.get();
    }

    private void craftRunes() {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null.");
            return;
        }
        RSObject altar = getAltarForRegion(worldPosition);
        if (altar == null) {
            log(GuardiansOfTheRift.class, "No altar found to enter.");
            return;
        }
        if (altar.getTileDistance(worldPosition) <= 2) {
            if (emptyPouches()) {
                return;
            }
        }
        WorldPosition altarWorldPosition = altar.getWorldPosition();
        BooleanSupplier breakCondition = () -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                log(GuardiansOfTheRift.class, "My position is null.");
                return false;
            }
            return myPosition_.getRegionID() != altarWorldPosition.getRegionID();
        };
        log(GuardiansOfTheRift.class, "Interacting with Altar:" + altarWorldPosition + " In region: " + altarWorldPosition.getRegionID());
        if (!altar.interact(breakCondition, "craft-rune")) {
            log(GuardiansOfTheRift.class, "Failed to interact with altar: " + altar.getName());
            return;
        }

        submitTask(() -> {
            // wait until no ess in inventory
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            return !inventorySnapshot.contains(ItemID.GUARDIAN_ESSENCE);
        }, random(8000, 14000));
        if (guardianPower < 85) {
            submitTask(() -> false, RandomUtils.gaussianRandom(400, 2000, 400, 500));
        } else {
            submitTask(() -> false, RandomUtils.gaussianRandom(400, 1000, 400, 500));
        }
    }

    private void powerGuardian() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null.");
            return;
        }

        if (PORTAL_MINE_AREA.contains(myPosition)) {
            enterPortal();
            return;
        }
        if (MINING_SHORTCUT_AREA.contains(myPosition)) {
            handleShortcut();
            return;
        }
        Polygon guardianCube = getGuardianCube();
        if (guardianCube == null) {
            walkToGuardian();
            return;
        }
        if (random(2) == 0) {
            dropItems();
            return;
        }
        Stopwatch dropDelay = new Stopwatch(random(500, 2000));
        if (getFinger().tapGameScreen(guardianCube, "power-up")) {
            submitTask(() -> {
                // wait until we are inside the guardian cube
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    return false;
                }
                updateTotalPoints();
                Integer guardianPower = (Integer) overlay.getValue(Overlay.GUARDIAN_POWER);
                if (getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
                    if (guardianPower == null) {
                        this.guardianPower = 0;
                    }
                    GUARDIAN_DELAY.reset(10000);
                    return true;
                }
                if (guardianPower != null) {
                    if (guardianPower == 0) {
                        log(GuardiansOfTheRift.class, "Guardian power is 0.");
                        return true;
                    }
                    if (guardianPower < 95 && dropDelay.hasFinished()) {
                        if (dropItems()) {
                            log(GuardiansOfTheRift.class, "Dropped talisman.");
                            return true;
                        }
                    }
                }

                return !inventorySnapshot.containsAny(ItemID.CATALYTIC_GUARDIAN_STONE, ItemID.ELEMENTAL_GUARDIAN_STONE);
            }, random(8000, 14000));
        }
    }

    private void walkToGuardian() {
        log(GuardiansOfTheRift.class, "Walking to guardian...");
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> getGuardianCube() != null);
        List<WorldPosition> targetPosition = Utils.getPositionsWithinRadius(GUARDIAN_POSITION, 3);
        if (targetPosition.isEmpty()) {
            return;
        }
        getWalker().walkTo(targetPosition.get(random(targetPosition.size())), builder.build());
    }

    private boolean hasCell(ItemGroupResult inventorySnapshot) {
        if (inventorySnapshot == null) {
            ;
            log(GuardiansOfTheRift.class, "Inventory snapshot is null, can't check for cells.");
            return false;
        }
        for (Rune.Cell cell : Rune.Cell.values()) {
            if (cell.getTier() >= cellDropThreshold && inventorySnapshot.contains(cell.getItemID())) {
                return true;
            }
        }
        return false;
    }

    private void mine() {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(GuardiansOfTheRift.class, "Position is null.");
            return;
        }
        boolean shouldUseShortcut = false;
        // only use shortcut if start of the game, if mid game we don't bother
        if (useShortcut && !PORTAL_MINE_AREA.contains(worldPosition)) {
            Rune catalyticRune = (Rune) overlay.getValue(Overlay.CATALYTIC_RUNE);
            Rune elementalRune = (Rune) overlay.getValue(Overlay.ELEMENTAL_RUNE);
            Integer guardianPower = (Integer) overlay.getValue(Overlay.GUARDIAN_POWER);
            shouldUseShortcut = (catalyticRune == null && elementalRune == null) || MINING_SHORTCUT_AREA.contains(worldPosition) || (guardianPower != null && (guardianPower < 25 || guardianPower == 100));
        }
        // handle the shortcut if we should use it & we are not already in the shortcut area
        if (shouldUseShortcut && !MINING_SHORTCUT_AREA.contains(worldPosition)) {
            handleShortcut();
            return;
        }
        RockType rockType = MINING_SHORTCUT_AREA.contains(worldPosition) ? RockType.LARGE
                : PORTAL_MINE_AREA.contains(worldPosition) ? RockType.HUGE
                : RockType.NORMAL;

        // find a rock to mine
        RSObject rock = getRock(worldPosition, rockType);
        if (rock == null) {
            log(GuardiansOfTheRift.class, "No rocks found to mine of type: " + rockType);
            return;
        }
        int tileDistance = rock.getTileDistance(worldPosition);
        log(GuardiansOfTheRift.class, "Tile distance from rock: " + tileDistance);
        if (!gameStarted()) {
            if (tileDistance > 2) {
                log(GuardiansOfTheRift.class, "Walking to rock: " + rock.getName() + " at " + rock.getWorldPosition());
                if (walkToRock(rock, worldPosition))
                    return;
            }
            log(GuardiansOfTheRift.class, "Game hasn't started yet, waiting...");
            START_MINING_REACTION_DELAY.reset(RandomUtils.weightedRandom(300, 7500, 0.001));
        } else if (!START_MINING_REACTION_DELAY.hasFinished()) {
            log(GuardiansOfTheRift.class, "Executing start mining reaction delay...");
            submitTask(() -> {
                getScreen().getDrawableCanvas().drawRect(getScreen().getBounds(), Color.GREEN.getRGB(), 0.3);
                return START_MINING_REACTION_DELAY.hasFinished();
            }, Integer.MAX_VALUE);
        } else {
            log(GuardiansOfTheRift.class, "Interacting with rock: " + rock.getName() + " at " + rock.getWorldPosition());
            if (!rock.interact("Mine")) {
                log("Failed to interact with rock...");
                return;
            }
            worldPosition = getWorldPosition();
            if (worldPosition != null) {
                long positionChangeTime = getLastPositionChangeMillis();

                if (rock.getTileDistance(worldPosition) > 1) {
                    log(GuardiansOfTheRift.class, "Waiting until we've arrived at the rock, current tile distance: " + rock.getTileDistance(worldPosition));
                    // if not in interactable distance, wait a little so we start moving.
                    // This is just to detect a dud action (when you click a menu entry but nothing happens)
                    if (!submitTask(() -> {
                        WorldPosition myPosition = getWorldPosition();
                        if (myPosition == null) {
                            log(GuardiansOfTheRift.class, "My position is null, can't check tile distance.");
                            return false;
                        }
                        return rock.getTileDistance(myPosition) <= 1 || getLastPositionChangeMillis() < positionChangeTime;
                    }, random(2500, 4000))) {
                        // if we don't move after interacting and we aren't next to the object
                        log(GuardiansOfTheRift.class, "We're not moving... trying again.");
                        return;
                    }
                }
            }
            waitUntilFinishedMining(rock, rockType);
        }
    }

    private boolean walkToRock(RSObject rock, WorldPosition worldPosition) {
        LocalPosition myLocalPosition = getLocalPosition();
        // walk to rock and wait for game to start
        List<LocalPosition> targetTiles = CollisionManager.getObjectTargetTiles(this, rock, 1, getSceneManager().getLevelCollisionMap(worldPosition.getPlane()).flags)
                .stream()
                .sorted(Comparator.comparingDouble(lp -> lp.distanceTo(myLocalPosition)))
                .toList();
        for (LocalPosition targetTile : targetTiles) {
            Deque<WorldPosition> path = aStarPathFinder.find(myLocalPosition, targetTile, false);
            if (path.isEmpty()) {
                continue;
            }
            WalkConfig.Builder b = new WalkConfig.Builder();
            b.breakCondition(() -> {
                WorldPosition myPosition = getWorldPosition();
                if (myPosition == null) {
                    log(GuardiansOfTheRift.class, "My position is null, can't check tile distance.");
                    return false;
                }
                return rock.getTileDistance(myPosition) <= 2;
            });
            getWalker().walkPath(path.stream().toList(), b.build());
            return true;
        }
        getWalker().walkTo(rock.getWorldPosition());
        return false;
    }

    private void handleShortcut() {
        log(GuardiansOfTheRift.class, "Handling shortcut...");
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null, can't handle shortcut.");
            return;
        }
        Area targetArea = MINING_SHORTCUT_AREA.contains(myPosition) ? BOSS_AREA : MINING_SHORTCUT_AREA;
        RSObject shortcut = getObjectManager().getClosestObject("Rubble");
        if (shortcut == null) {
            log("No rubble found to use shortcut...");
            return;
        }
        BooleanSupplier breakCondition = () -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false; // can't continue if position is null
            }
            if (PORTAL_MINE_AREA.contains(myPosition_)) {
                return true;
            }
            if (!MINING_SHORTCUT_AREA.contains(myPosition_)) {
                return overlay.getValue(Overlay.PORTAL) != null;
            }
            return false;
        };
        if (!shortcut.interact(breakCondition, "Climb")) {
            log("Failed to interact with rubble...");
            return;
        }
        submitHumanTask(() -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            return targetArea.contains(myPosition_);
        }, random(9000, 12000));
    }

    private void waitUntilFinishedMining(RSObject rock, RockType rockType) {
        boolean essenceRock = rockType == RockType.HUGE;
        final int itemToTrack = essenceRock ? ItemID.GUARDIAN_ESSENCE : ItemID.GUARDIAN_FRAGMENTS;
        AtomicInteger previousAmount = new AtomicInteger(inventorySnapshot.getAmount(itemToTrack));
        Timer amountChangeTimer = new Timer();
        Timer animatingTimer = new Timer();
        log(GuardiansOfTheRift.class, "Entering waiting task...");
        submitTask(() -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            if (getWidgetManager().getDialogue().isVisible()) {
                log(GuardiansOfTheRift.class, "Dialogue is visible, breaking out of wait task.");
                return true;
            }
            int tileDistance = rock.getTileDistance(myPosition_);
            if (tileDistance > 1) {
                // still traversing to the rock
                amountChangeTimer.reset();
                log(GuardiansOfTheRift.class, "Still walking to rock. Tile distance: " + tileDistance);
                if (getLastPositionChangeMillis() > 2000) {
                    log(GuardiansOfTheRift.class, " Position not changed, breaking out of sleep...");
                    return true;
                } else {
                    return false;
                }
            }
            // If the amount of resources in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                log(GuardiansOfTheRift.class, "Amount change timeout");
                this.amountChangeTimeout = random(4000, 6000);
                return true;
            }

            if (animatingTimer.timeElapsed() > animationTimeout) {
                log(GuardiansOfTheRift.class, "Animation timeout");
                this.animationTimeout = random(4000, 6000);
                return true;
            }

            if (getPixelAnalyzer().isPlayerAnimating(0.2)) {
                animatingTimer.reset();
            }

            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            updateOverlayValues();
            spaceForEssence = getSpaceForEssence();
            if (overlay.isVisible()) {
                Integer guardianPower = (Integer) overlay.getValue(Overlay.GUARDIAN_POWER);
                Integer totalPoints = getTotalPoints();

                if (guardianPower != null && totalPoints != null) {
                    this.totalPoints = totalPoints;
                    forceCraftEssence = shouldForceCraftEssence(totalPoints, guardianPower);
                    if (rockType == RockType.HUGE && shouldForceCraftRunes(totalPoints, guardianPower)) {
                        return true;
                    }
                    // -3 so it gives us a bit of extra time to craft ess
                    else if (rockType != RockType.HUGE && shouldForceCraftEssence(totalPoints, guardianPower - 3)) {
                        return true;
                    }
                    if (guardianPower == 0 || guardianPower == 100) {
                        log(GuardiansOfTheRift.class, "Guardian power is null or zero, stopping mining.");
                        return true;
                    }
                    if (guardianPower > 25 && rockType == RockType.NORMAL) {
                        int fragments = inventorySnapshot.getAmount(ItemID.GUARDIAN_FRAGMENTS);
                        if (fragments >= spaceForEssence) {
                            log(GuardiansOfTheRift.class, "Enough fragments to do a run as we are mid-game & not focusing target amount.");
                            return true;
                        }
                    }
                }
                if (!essenceRock) {
                    // listen for portal
                    Overlay.PortalInfo portalInfo = (Overlay.PortalInfo) overlay.getValue(Overlay.PORTAL);
                    if (portalInfo != null) {
                        log(GuardiansOfTheRift.class, "Portal is active, prioritising..");
                        return true;
                    }
                }
            }

            if (essenceRock && inventorySnapshot.isFull()) {
                log(GuardiansOfTheRift.class, "Inventory is full.");
                return true;
            }
            int miningOutput = inventorySnapshot.getAmount(itemToTrack);
            if (!essenceRock && miningOutput > targetFragments) {
                log(GuardiansOfTheRift.class, "Reached target fragments: " + targetFragments + ", stopping mining.");
                return true;
            }
            if (miningOutput > previousAmount.get()) {
                log(GuardiansOfTheRift.class, "Gained fragments!");
                int amountGained = miningOutput - previousAmount.get();
                amountChangeTimer.reset();
                previousAmount.set(miningOutput);
            }
            return false;
        }, random(120000, 160000));


        // reaction delay
        int delay;
        if (random(4) == 0 && guardianPower < 85) {
            // longer delay
            delay = RandomUtils.exponentialRandom(2500, 500, 6000);
        } else if (guardianPower < 85) {
            delay = RandomUtils.weightedRandom(200, 3500, 0.0017);
        } else {
            delay = RandomUtils.weightedRandom(200, 1500, 0.0017);
        }
        log(GuardiansOfTheRift.class, "⏳ → human(" + delay + "ms)");
        submitTask(() -> {
            getScreen().getDrawableCanvas().drawRect(getScreen().getBounds(), Color.GREEN.getRGB(), 0.3);
            return false;
        }, delay);
    }

    private RSObject getRock(WorldPosition worldPosition, RockType rockType) {
        List<RSObject> rocks = getObjectManager().getObjects(object -> {
            String name = object.getName();
            if (name == null) {
                return false; // skip objects without a name
            }
            for (String rockName : rockType.getNames()) {
                if (name.equalsIgnoreCase(rockName.toLowerCase())) {
                    return object.canReach();
                }
            }
            return false;
        });

        if (rocks.isEmpty()) {
            return null;
        }
        if (rockType == RockType.NORMAL) {
            List<RSObject> rocksInArea = rocks.stream().filter(rsObject -> MINING_AREA.contains(rsObject.getWorldPosition())).toList();
            return rocksInArea.get(random(rocksInArea.size()));
        } else {
            // random rock in big rock areas as there is only 2
            for (RSObject rock : rocks) {
                if (rock.getTileDistance(worldPosition) <= 2) {
                    return rock;
                }
            }
            return rocks.get(random(rocks.size()));
        }
    }

    private void enterPortal() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null.");
            return;
        }
        boolean leave = PORTAL_MINE_AREA.contains(myPosition) && (inventorySnapshot.isFull() || forceCraftEssence);
        Overlay.PortalInfo portalInfo;
        if (!leave) {
            portalInfo = (Overlay.PortalInfo) overlay.getValue(Overlay.PORTAL);
            if (portalInfo == null) {
                log(GuardiansOfTheRift.class, "No active portal found.");
                return;
            }
        } else {
            portalInfo = null;
        }
        String portalName = leave ? "Exit portal" : portalInfo.getPortal().getName();
        if (MINING_SHORTCUT_AREA.contains(myPosition)) {
            log(GuardiansOfTheRift.class, "In mining shortcut area, handling shortcut to get to portal...");
            handleShortcut();
            return;
        }
        log(GuardiansOfTheRift.class, "Entering portal: " + portalName);
        Polygon portalPolygon = getPortalPolygon(leave ? LEAVE_PORTAL : portalInfo.getPortal().getPosition(), leave ? null : portalInfo.getPortal());
        if (portalPolygon == null) {
            log(GuardiansOfTheRift.class, "Portal polygon is null, walking to portal...");
            walkToPortal(leave ? null : portalInfo.getPortal(), leave ? null : new Stopwatch(portalInfo.getSecondsRemaining() * 1000L));
            return;
        }
        if (!getFinger().tapGameScreen(portalPolygon, "Enter")) {
            log(GuardiansOfTheRift.class, "Failed to interact with portal: " + portalName);
            return;
        }

        int noMovementTimeout = random(1500, 3000);
        int noMovementTimeoutAdjacent = random(2500, 3000);
        AtomicBoolean enteredPortal = new AtomicBoolean(false);
        WorldPosition[] previousPortalPosition = new WorldPosition[1];
        if (leave) {
            previousPortalPosition[0] = LEAVE_PORTAL;
        } else {
            previousPortalPosition[0] = portalInfo.getPortal().getPosition();
        }
        submitHumanTask(() -> {
            // wait until we are inside the portal
            WorldPosition myPosition_ = getExpectedWorldPosition();
            if (myPosition_ == null) {
                return false;
            }
            if (leave != PORTAL_MINE_AREA.contains(myPosition_)) {
                log(GuardiansOfTheRift.class, "We entered the other side.");
                fillPouchDelay.reset(random(2500, 4500));
                return true;
            }
            if (!leave) {
                Overlay.PortalInfo portalInfo_ = (Overlay.PortalInfo) overlay.getValue(Overlay.PORTAL);

                long lastPositionChangeMillis = getLastPositionChangeMillis();
                if (previousPortalPosition[0] != null && CollisionManager.isCardinallyAdjacent(myPosition_, previousPortalPosition[0])) {
                    if (!overlay.isVisible()) {
                        return false;
                    } else if (lastPositionChangeMillis > noMovementTimeoutAdjacent) {
                        log(GuardiansOfTheRift.class, "No movement detected while adjacent for " + noMovementTimeoutAdjacent + "ms, breaking out of interaction.");
                        fillPouchDelay.reset(random(2500, 4500));
                        return true;
                    }

                }

                if (portalInfo_ == null) {
                    if (!enteredPortal.get()) {
                        log(GuardiansOfTheRift.class, "Portal info is null & we've not entered the portal yet, breaking out of interaction.");
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    previousPortalPosition[0] = portalInfo_.getPortal().getPosition();
                    double distance = portalInfo.getPortal().getPosition().distanceTo(myPosition_);
                    if (portalInfo_.getSecondsRemaining() <= 1 && distance > 6) {
                        log(GuardiansOfTheRift.class, "Portal expiring & we are not close enough, breaking out of interaction.");
                        return true;
                    }
                }

                if (CollisionManager.isCardinallyAdjacent(portalInfo.getPortal().getPosition(), myPosition_)) {
                    log(GuardiansOfTheRift.class, "Cardianly adjacent to portal, last position change millis: " + lastPositionChangeMillis);
                    log(GuardiansOfTheRift.class, "Presuming we entered the portal, setting enteredPortal to true.");
                    enteredPortal.set(true);
                    return false;
                } else {
                    log(GuardiansOfTheRift.class, "Not cardianly adjacent to portal, checking for no movement timeout.");
                    if (lastPositionChangeMillis > noMovementTimeout) {
                        log(GuardiansOfTheRift.class, "No movement detected for " + noMovementTimeout + "ms, breaking out of interaction.");
                        return true;
                    }
                }
            }
            return false;
        }, random(8000, 14000));
        log(GuardiansOfTheRift.class, "End of portal interaction.");
    }

    private void walkToPortal(Portal portal, Stopwatch timeLeft) {
        log(GuardiansOfTheRift.class, "Walking to portal...");
        boolean leave = portal == null;
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> getPortalPolygon(leave ? LEAVE_PORTAL : portal.getPosition(), leave ? null : portal) != null);
        builder.timeout(leave ? 10000 : (int) timeLeft.timeLeft());
        getWalker().walkTo(leave ? LEAVE_PORTAL : portal.getPosition(), builder.build());
    }

    private Polygon getPortalPolygon(WorldPosition portalPosition, Portal portal) {
        LocalPosition localPosition = portalPosition.toLocalPosition(this);
        if (localPosition == null) {
            log(GuardiansOfTheRift.class, "Local position is null for portal: " + (portal == null ? "Exit portal" : portal.getName()));
            return null;
        }
        Polygon cube = getSceneProjector().getTileCube(localPosition.getX(), localPosition.getY(), 0, 50, 100);
        if (cube == null || (cube = cube.getResized(0.6)) == null) {
            return null;
        }
        if (!getWidgetManager().insideGameScreen(cube, List.of(ChatboxComponent.class))) {
            return null;
        }
        return cube;
    }

    private Polygon getGuardianCube() {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            return null;
        }
        if (GUARDIAN_POSITION.distanceTo(worldPosition) >= 15) {
            return null;
        }
        Polygon cube = getSceneProjector().getTileCube(GUARDIAN_POSITION, 100);
        if (cube == null || (cube = cube.getResized(1.5)) == null || !getWidgetManager().insideGameScreen(cube, List.of(ChatboxComponent.class))) {
            return null;
        }
        return cube;
    }

    private void buildBarrier() {
        if (hasCell(inventorySnapshot)) {
            // place weak cell
            placeCell();
        }
//        else {
//            if (!createBarrierAtBeginning || builtFirstBarrier || gameStarted()) {
//                return;
//            }
//            if (inventorySnapshot.isFull()) {
//                if (inventorySnapshot.contains(ItemID.GUARDIAN_ESSENCE)) {
//                    log(GuardiansOfTheRift.class, "Dropping Essence to make space for weak cell...");
//                    dropEssence();
//                } else {
//                    log(GuardiansOfTheRift.class, "No space left in inventory to pick up weak cell...");
//                }
//                return;
//            }
//            pickUpWeakCell();
//        }
    }

    private void pickUpWeakCell() {
        RSObject weakCellTable = getObjectManager().getClosestObject("Weak cells");
        if (weakCellTable == null) {
            log("No weak cell table found inside our loaded scene...");
            return;
        }
        if (!weakCellTable.interact("Take")) {
            log("Failed to interact with weak cell table...");
            return;
        }
        submitHumanTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            return inventorySnapshot.contains(ItemID.WEAK_CELL);
        }, random(8000, 14000));
    }

    private void pickUpUnchargedCell() {
        RSObject weakCellTable = getObjectManager().getClosestObject("Uncharged cells");
        if (weakCellTable == null) {
            log("No weak cell table found inside our loaded scene...");
            return;
        }
        if (!weakCellTable.interact("Take-10")) {
            log("Failed to interact with weak cell table...");
            return;
        }
        submitHumanTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }
            int amountOfCells = inventorySnapshot.getAmount(ItemID.UNCHARGED_CELL);
            boolean complete = amountOfCells >= 10;
            if (complete) {
                log(GuardiansOfTheRift.class, "Picked up uncharged cells, amount: " + amountOfCells);
            }
            return complete;
        }, random(8000, 14000));
    }

    private void updateOverlayValues() {
        Integer guardianPower = (Integer) overlay.getValue(Overlay.GUARDIAN_POWER);
        if (guardianPower != null) {
            overlayUpdateTimeout.reset();
            this.guardianPower = guardianPower;
        }
        Integer totalPoints = getTotalPoints();
        if (totalPoints != null) {
            this.totalPoints = totalPoints;
        }

    }

    private void placeCell() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            log(GuardiansOfTheRift.class, "My position is null, can't place cell.");
            return;
        }
        List<RSObject> cellTiles = getObjectManager().getObjects(object -> object.getName() != null && object.getName().equals("Inactive cell tile"));
        if (cellTiles.isEmpty()) {
            log("No weak cell tiles found inside our loaded scene...");
            return;
        }
        List<RSObject> closestCellTiles = cellTiles.stream()
                .sorted(Comparator.comparingDouble(tile -> tile.getWorldPosition().distanceTo(myPosition)))
                .limit(3)
                .toList();
        RSObject weakCellTile = closestCellTiles.get(random(closestCellTiles.size()));
        BooleanSupplier breakCondition = () -> {
            WorldPosition myPosition_ = getWorldPosition();
            if (myPosition_ == null) {
                return false; // can't continue if position is null
            }
            return PORTAL_MINE_AREA.contains(myPosition_) || MINING_SHORTCUT_AREA.contains(myPosition_);
        };
        if (!weakCellTile.interact(menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                if (entry.getAction().equalsIgnoreCase("Place-cell")) {
                    return entry; // return the entry that matches "Place weak cell"
                }
            }
            return null;
        }, breakCondition)) {
            log(GuardiansOfTheRift.class, "Failed to interact with weak cell tile...");
            return;
        }
        int notMovingTimeout = random(400, 1200);
        boolean moving = submitTask(() -> getLastPositionChangeMillis() < notMovingTimeout, random(2000, 3500));
        if (!moving) {
            log(GuardiansOfTheRift.class, "Not moving, breaking out of interaction.");
            return;
        }
        int movementTimeout = random(400, 1200);
        submitTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }

            if (getLastPositionChangeMillis() > movementTimeout) {
                log(GuardiansOfTheRift.class, "Movement timeout exceeded, breaking out of interaction.");
                return true; // break out of interaction if we haven't moved for a while
            }

            if (hasCell(inventorySnapshot)) {
                return false; // still have weak cell, wait for it to be used
            }
            builtFirstBarrier = true;
            return true; // barrier built
        }, random(9000, 14000));

        submitTask(() -> false, RandomUtils.gaussianRandom(400, 2000, 400, 500));
    }

    private boolean gameStarted() {
        updateOverlayValues();
        if (overlayUpdateTimeout.timeElapsed() > 10000 && guardianPower == null) {
            log(GuardiansOfTheRift.class, "Overlay update timeout exceeded, guardian power is still null.. presuming game hasn't started.");
            return false; // overlay hasn't updated in a while
        }
        if (guardianPower == null) {
            log(GuardiansOfTheRift.class, "Guardian power is null.");
            return false;
        }
        if (guardianPower > 0 && guardianPower < 100) {
            return true; // game has started
        } else if (guardianPower == 100 && builtFirstBarrier && barrierResetTimer.hasFinished()) {
            log(GuardiansOfTheRift.class, "Game has ended, resetting barrier building state.");
            builtFirstBarrier = false; // reset barrier building state
            barrierResetTimer.reset(120000);
        }
        return false;
    }

    @Override
    public int[] regionsToPrioritise() {
        int catalyticRunes = CatalyticRune.values().length;
        int elementalRunes = ElementalRune.values().length;

        List<Integer> regionList = new ArrayList<>();
        // prioritisedAltarRegion first
        regionList.add(prioritisedAltarRegion);
        // 14484 as the second index
        regionList.add(GOTR_REGION_ID);

        for (int i = 0; i < catalyticRunes; i++) {
            int regionId = CatalyticRune.values()[i].getAltarRegionId();
            if (regionId != prioritisedAltarRegion && regionId != 14484) {
                regionList.add(regionId);
            }
        }
        for (int i = 0; i < elementalRunes; i++) {
            int regionId = ElementalRune.values()[i].getAltarRegionId();
            if (regionId != prioritisedAltarRegion && regionId != 14484) {
                regionList.add(regionId);
            }
        }

        return regionList.stream().mapToInt(Integer::intValue).toArray();
    }

    private boolean insideAltar(WorldPosition worldPosition) {
        int regionId = worldPosition.getRegionID();
        for (Rune rune : CatalyticRune.values()) {
            if (rune.getAltarRegionId() == regionId) {
                return true; // inside a catalytic altar
            }
        }
        for (Rune rune : ElementalRune.values()) {
            if (rune.getAltarRegionId() == regionId) {
                return true; // inside an elemental altar
            }
        }
        return false;
    }

    @Override
    public boolean canAFK() {
        return guardianPower != null && guardianPower < 85;
    }

    @Override
    public List<AFKTime> afkTimers() {
        return List.of(
                new AFKTime(1, TimeUnit.SECONDS.toMillis(800), TimeUnit.SECONDS.toMillis(420), TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS.toMillis(300)),
                new AFKTime(1, TimeUnit.SECONDS.toMillis(200), TimeUnit.SECONDS.toMillis(220), TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS.toMillis(180)),
                new AFKTime(12, TimeUnit.SECONDS.toMillis(800), TimeUnit.SECONDS.toMillis(360), TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(15)),
                new AFKTime(12, TimeUnit.SECONDS.toMillis(200), TimeUnit.SECONDS.toMillis(200), TimeUnit.SECONDS.toMillis(5), TimeUnit.SECONDS.toMillis(15))
        );
    }

    @Override
    public void onPaint(Canvas c) {
        Rectangle usernameBounds = getWidgetManager().getChatbox().getUsernameBounds();
        if (usernameBounds != null) {
            c.fillRect(usernameBounds, Color.BLACK.getRGB());
        }
        FontMetrics metrics = c.getFontMetrics(ARIEL);
        int padding = 5;

        List<String> lines = new ArrayList<>();
        lines.add("Task: " + (task == null ? "None" : task));
        lines.add("");
        lines.add("Games played: " + gamesPlayed);
        lines.add("Games qualified: " + gamesQualified + "/" + gamesPlayed);
        lines.add("");
        lines.add("Point strategy: " + selectedStrategy);
        lines.add("");
        lines.add("Guardian's power: " + guardianPower);
        lines.add("Total points: " + totalPoints);
        lines.add("Space for essence: " + spaceForEssence);
        lines.add("Inside boss area: " + insideBossArea);
        lines.add("");
        lines.add("--- Pouch info -----");

        if (hasDegradedPouches) {
            lines.add("Degraded pouches: " + hasDegradedPouches);
        }
        if (pouchRepairDelay != null) {
            long millis = pouchRepairDelay.timeLeft();
            lines.add("Pouch repair delay: " + (millis < 0 ? "Due to execute" : formatMillisToHMS(millis)));
        }
        for (Pouch pouch : Pouch.values()) {
            boolean contains = POUCHES_ACTIVE.contains(pouch);
            lines.add((contains ? "[✔]" : "") + "Pouch: " + pouch.name() + " capacity: " + pouch.getCurrentCapacity() + "/" + pouch.getMaxCapacity());
        }
        lines.add("");

        for (Rune rune : GUARDIANS_TO_IGNORE) {
            lines.add("Ignoring guardian: " + rune.getName());
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
        c.drawRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.CYAN.getRGB());
        // Draw text lines
        int drawY = 40;
        for (int i = 0; i < lines.size(); i++) {
            int color = Color.WHITE.getRGB();
            String line = lines.get(i);
            if (line.startsWith("Ignoring guardian:")) {
                color = Color.RED.getRGB();
            } else if (line.startsWith("Pouch repair delay:")) {
                color = Color.GREEN.getRGB();
            }
            c.drawText(line, drawX, drawY += metrics.getHeight(), color, ARIEL);
        }
    }

    private String formatMillisToHMS(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }


    enum RockType {
        NORMAL("guardian parts", "guardian remains"),
        LARGE("large guardian remains"),
        HUGE("huge guardian remains");
        private final String[] names;

        RockType(String... names) {
            this.names = names;
        }

        public String[] getNames() {
            return names;
        }
    }

    enum Task {
        TAKE_UNCHARGED_CELL,
        MINE,
        ENTER_PORTAL,
        DROP_ESSENCE,
        ENTER_ALTAR,
        BUILD_BARRIER,
        CREATE_ESSENCE,
        POWER_GUARDIAN,
        DEPOSIT_RUNES,
        HANDLE_ALTAR,
        REPAIR_POUCHES,
        DROP_TALISMANS,
        ENTER_BARRIER;
    }

    class RuneToCraft {
        private final Rune rune;
        private final boolean isTalisman;

        public RuneToCraft(Rune rune, boolean isTalisman) {
            this.rune = rune;
            this.isTalisman = isTalisman;
        }

        public Rune getRune() {
            return rune;
        }

        public boolean isTalisman() {
            return isTalisman;
        }
    }
}
