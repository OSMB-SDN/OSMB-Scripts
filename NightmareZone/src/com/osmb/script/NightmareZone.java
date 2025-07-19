package com.osmb.script;

import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.input.PhysicalKey;
import com.osmb.api.input.TouchType;
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
import com.osmb.api.script.task.exception.TaskInterruptedException;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.overlay.BuffOverlay;
import com.osmb.api.ui.tabs.Tab;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.component.ChestInterface;
import com.osmb.script.component.PotionInterface;
import com.osmb.script.javafx.UI;
import com.osmb.script.overlay.AbsorptionPointsOverlay;
import com.osmb.script.potion.BarrelPotion;
import com.osmb.script.potion.Potion;
import com.osmb.script.potion.StandardPotion;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.osmb.script.Options.*;
import static com.osmb.script.Status.*;

@ScriptDefinition(name = "Nightmare zone", description = "", author = "Joe", skillCategory = SkillCategory.COMBAT, version = 1.0)
public class NightmareZone extends Script {

    public static final String[] BANK_NAMES = {"Bank booth"};
    public static final String[] BANK_ACTIONS = {"bank"};
    public static final int BLACK_FONT_PIXEL = -16777215;
    public static final int ARENA_REGION = 9033;

    // World Positions
    public static final WorldPosition REWARDS_CHEST_INTERACT_TILE = new WorldPosition(2609, 3118, 0);
    public static final WorldPosition POTION_TILE = new WorldPosition(2605, 3117, 0);
    public static final WorldPosition DOMINIC_POSITION = new WorldPosition(2608, 3116, 0);
    public static final List<Integer> RUNES = List.of(ItemID.RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH, ItemID.AIR_RUNE, ItemID.WATER_RUNE, ItemID.EARTH_RUNE, ItemID.FIRE_RUNE, ItemID.ASTRAL_RUNE, ItemID.NATURE_RUNE, ItemID.CHAOS_RUNE, ItemID.DEATH_RUNE, ItemID.BLOOD_RUNE, ItemID.SOUL_RUNE, ItemID.BODY_RUNE, ItemID.MIND_RUNE, ItemID.LAW_RUNE, ItemID.COSMIC_RUNE, ItemID.STEAM_RUNE, ItemID.SMOKE_RUNE, ItemID.MIST_RUNE, ItemID.DUST_RUNE, ItemID.LAVA_RUNE, ItemID.MUD_RUNE, ItemID.WRATH_RUNE);
    // Area Definitions
    private static final RectangleArea BANK_AREA = new RectangleArea(2609, 3088, 4, 9, 0);
    private static final RectangleArea NMZ_AREA = new RectangleArea(2601, 3113, 5, 5, 0);
    // Misc
    private static final java.awt.Font ARIEL = new java.awt.Font("Arial", java.awt.Font.PLAIN, 14);
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>();
    private static boolean inArena = false;
    // Collections
    private final List<ItemSearchResult> itemsToEquip = new ArrayList<>();
    private final Map<BarrelPotion, Integer> barrelDoseCache = Collections.synchronizedMap(new HashMap<>());
    private final List<Task> dynamicTasks = new ArrayList<>();
    private final Stopwatch nextBoostPotionDrink = new Stopwatch();
    private final Stopwatch rapidHealFlickTimer = new Stopwatch();
    private final Stopwatch switchTabTimer = new Stopwatch();
    // UI References
    private final AbsorptionPointsOverlay absorptionPointsOverlay;
    private final ChestInterface chestInterface;

    // State Trackers
    private final PotionInterface potionInterface;
    private final BuffOverlay overloadBuffOverlay;
    // Timers
    private Stopwatch statBoostPotionDelay = null;
    private Stopwatch prayerDelayTimer;
    private Stopwatch lowerHPDelayTimer;
    private Task previousTask = null;
    private Task task;
    // Current State
    private Set<ItemSearchResult> boostPotions;
    private Set<ItemSearchResult> secondaryPotions;
    private Stopwatch specialDelayTimer;
    private ItemGroupResult inventorySnapshot;
    private ItemGroupResult bankSnapshot;
    private BuffOverlay ammoOverlay;
    private Integer ammoCount = null;

    public NightmareZone(Object scriptCore) {
        super(scriptCore);
        this.chestInterface = new ChestInterface(this);
        this.potionInterface = new PotionInterface(this);
        this.absorptionPointsOverlay = new AbsorptionPointsOverlay(this);
        this.overloadBuffOverlay = new BuffOverlay(this, ItemID.OVERLOAD_4);
    }

    public static boolean isArena(WorldPosition worldPosition) {
        return worldPosition.getRegionID() == ARENA_REGION;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 200; i++) {
            System.out.println(RandomUtils.gaussianRandom(300, 2500, 200, 700));
        }
    }

    @Override
    public void onPaint(Canvas c) {
        FontMetrics metrics = c.getFontMetrics(ARIEL);
        int padding = 5;

        List<String> lines = new ArrayList<>();
        lines.add("Task: " + (task == null ? "None" : task));
        lines.add("");
        if (ammoCount != null) {
            lines.add("Ammo count: " + ammoCount);
        }
        lines.add("Suicide when out of boost potions: " + noBoostSuicide);
        lines.add("Flick rapid heal: " + flickRapidHeal);

        if (inArena) {
            if (statBoostPotion == BarrelPotion.OVERLOAD) {
                lines.add("Overload active: " + overloadBuffOverlay.isVisible());
            }

            lines.add("");
            lines.add("--- Timers ---");
            if (flickRapidHeal) {
                long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(rapidHealFlickTimer.timeLeft());
                lines.add("Next flick: " + secondsLeft);
            }
            if (statBoostPotion != null) {
                lines.add("Next " + statBoostPotion.getName() + " drink: " + nextBoostPotionDrink.getRemainingTimeFormatted());
            }
            if (secondaryPotion != null) {
                String end = secondaryPotion == StandardPotion.PRAYER_POTION ? "%" : "points";
                lines.add("Next " + secondaryPotion.getName() + " drink: " + nextSecondaryDrink + " " + end);
            }
            if (lowerHealthMethod != null) {
                long secondsLeft = lowerHPDelayTimer == null ? -1 : TimeUnit.MILLISECONDS.toSeconds(lowerHPDelayTimer.timeLeft());
                lines.add("Lower HP delay: " + Math.max(secondsLeft, 0));
            }
        } else {
            lines.add("Setup dream: " + setupDream);
            lines.add("Reward points: " + (cachedRewardPoints == null ? "null" : cachedRewardPoints));
            boolean hasBarrelPotions = statBoostPotion instanceof BarrelPotion || secondaryPotion instanceof BarrelPotion;
            if (hasBarrelPotions) {
                lines.add("");
                lines.add("--- Barrel dose cache ---");
                for (Map.Entry<BarrelPotion, Integer> entry : barrelDoseCache.entrySet()) {
                    Potion potion = entry.getKey();
                    Integer value = entry.getValue();
                    lines.add(potion.getName() + ": " + value);
                }
            }
        }

        lines.add("");
        lines.add("--- Dynamic tasks ---");
        for (Task task : dynamicTasks) {
            lines.add(" - Task: " + task);
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
        c.drawRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.YELLOW.getRGB());
        // Draw text lines
        int drawY = 40;
        for (String line : lines) {
            int color = Color.WHITE.getRGB();
            c.drawText(line, drawX, drawY += metrics.getHeight(), color, ARIEL);
        }
    }

    @Override
    public void onStart() {
        UI ui = new UI(this);
        Scene scene = new Scene(ui);

        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);

        lowerHealthMethod = ui.getLowerHPMethod();
        statBoostPotion = ui.getPrimaryPotion();
        statBoostPotionAmount = ui.getBoostPotionAmount();
        secondaryPotion = ui.getSecondaryPotion();
        afkPosition = ui.getAFKPosition();
        shieldItemID = ui.getShieldItemId();
        weaponItemID = ui.getMainWeaponItemId();
        // suicides when out of boost potions to be more efficient xp wise
        noBoostSuicide = ui.suicideNoBoost();
        // flicks rapid heal to keep 1hp (only if using absorption potions as secondary)
        flickRapidHeal = ui.flickRapidHeal();

        if (afkPosition == AFKPosition.RANDOM) {
            List<AFKPosition> positions = new ArrayList<>(EnumSet.allOf(AFKPosition.class));
            positions.remove(AFKPosition.RANDOM); // Remove RANDOM from possible choices
            afkPosition = positions.get(Utils.random(positions.size()));
        }

        idleTimeout = random(2000, 4000);
        nextSecondaryDrink = secondaryPotion == StandardPotion.PRAYER_POTION ? random(10, 60) : random(50, 200);

        // add items to recognise

        if (weaponItemID != -1) {
            ITEM_IDS_TO_RECOGNISE.add(weaponItemID);
        }
        if (shieldItemID != -1) {
            ITEM_IDS_TO_RECOGNISE.add(shieldItemID);
        }
        if (statBoostPotion != null) {
            ITEM_IDS_TO_RECOGNISE.addAll(statBoostPotion.getItemIDs());
        }
        if (secondaryPotion != null) {
            ITEM_IDS_TO_RECOGNISE.addAll(secondaryPotion.getItemIDs());
        }
        for (LowerHealthMethod method : LowerHealthMethod.values()) {
            ITEM_IDS_TO_RECOGNISE.add(method.getItemID());
        }
        for (SpecialAttackWeapon specWeapon : SpecialAttackWeapon.values()) {
            ITEM_IDS_TO_RECOGNISE.add(specWeapon.getItemID());
        }
        Ammo ammo = ui.getAmmo();
        if (ammo != null) {
            ammoOverlay = new BuffOverlay(this, ammo.getItemID());
        }
        ITEM_IDS_TO_RECOGNISE.addAll(RUNES);
    }

    @Override
    public int poll() {
        task = decideTask(false);
        if (task == null) {
            return 0;
        }
        log(getClass().getSimpleName(), "Executing task: " + task);
        executeTask(task);
        return 0;
    }

    private Task decideTask(boolean walking) {
        WorldPosition worldPosition = getWorldPosition();

        // only check for bank if inside the bank area, this is because searching for center components takes 30ms or so.
        if (BANK_AREA.contains(worldPosition)) {
            if (getWidgetManager().getBank().isVisible()) {
                return Task.HANDLE_BANK;
            }
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(NightmareZone.class, "Failed opening inventory.");
            return null;
        }
        // get potion amounts
        secondaryPotions = inventorySnapshot.getAllOfItems(secondaryPotion.getItemIDs());
        boostPotions = statBoostPotion != null ? inventorySnapshot.getAllOfItems(statBoostPotion.getItemIDs()) : Collections.emptySet();

        // check for interfact if at the object, this is to save searching for it constantly
        if (worldPosition.equals(REWARDS_CHEST_INTERACT_TILE)) {
            if (chestInterface.isVisible()) {
                return Task.HANDLE_CHEST;
            }
        }

        // clear dynamic tasks
        dynamicTasks.clear();

        // check if we are in the arena
        inArena = isArena(worldPosition);

        if (ammoOverlay != null) {
            if (ammoOverlay.isVisible()) {
                String buffText = ammoOverlay.getBuffText();
                buffText = buffText.replaceAll("\\D", "");
                this.ammoCount = Integer.parseInt(buffText);
            } else {
                log(NightmareZone.class, "Cannot find ammo buff overlay, stopping script.");
                stop();
                return null;
            }
        }

        // decide tasks
        if (inArena) {
            return getArenaTask(walking, worldPosition);
        } else {
            return getTask(inventorySnapshot);
        }
    }

    private Task getTask(ItemGroupResult inventorySnapshot) {
        if (!noLowerHPDelay) {
            noLowerHPDelay = true;
        }

        boolean hasBarrelPotion = statBoostPotion instanceof BarrelPotion || secondaryPotion instanceof BarrelPotion;
        if (hasBarrelPotion && cachedRewardPoints == null) {
            dynamicTasks.add(Task.HANDLE_CHEST);
        }

        // make sure boost potion are all full doses
        // if amount doesn't match amount selected
        if (statBoostPotion != null && (boostPotions.size() != statBoostPotionAmount || !allFullDoses(boostPotions, statBoostPotion.getFullID()))) {
            log(NightmareZone.class, "Need boost potions");
            if (statBoostPotion instanceof BarrelPotion) {
                log(NightmareZone.class, "Is barrel potion");
                if (boostPotions.size() > statBoostPotionAmount) {
                    return Task.RESTOCK_BOOST_POTIONS;
                }

                // ignore if we don't have enough to make a full inv & can buy more from the shop. (only if we know the dose stock & points)
                if (shouldInteractWithBarrel(statBoostPotion, boostPotions, statBoostPotionAmount)) {
                    dynamicTasks.add(Task.RESTOCK_BOOST_POTIONS);
                }
            } else if (statBoostPotion instanceof StandardPotion) {
                if (bankSnapshot != null && !bankSnapshot.contains(statBoostPotion.getFullID())) {
                    if (!inventorySnapshot.containsAny(statBoostPotion.getItemIDs())) {
                        log(NightmareZone.class, "No boost potions in inventory or bank, stopping script!");
                        stop();
                    }
                } else {
                    log(NightmareZone.class, "Boost potion needs bank");
                    dynamicTasks.add(Task.OPEN_BANK);
                }
            }
        }

        // if we have free slots remaining, or if the absorptions aren't all (4) dose
        int secondariesNeeded = getSecondaryPotionsNeeded();
        if (secondariesNeeded > 0 || !allFullDoses(secondaryPotions, secondaryPotion.getFullID())) {
            if (secondaryPotion instanceof BarrelPotion) {
                // check ignore flags (used for when we don't have enough points to fully stock etc.)
                int requiredAmount = inventorySnapshot.getFreeSlots() + secondaryPotions.size();
                if (statBoostPotion != null) {
                    requiredAmount += boostPotions.size();
                    // at this point required amount equals the free slots, excluding boost pots + secondary.
                    // we can now take the desired amount of stat pots needed away from this & we'll have the amount of secondaries needed.
                    requiredAmount -= statBoostPotionAmount;
                }

                if (shouldInteractWithBarrel(secondaryPotion, secondaryPotions, requiredAmount)) {
                    dynamicTasks.add(Task.RESTOCK_SECONDARY_POTIONS);
                }
            } else if (secondaryPotion instanceof StandardPotion) {
                if (bankSnapshot != null && !bankSnapshot.contains(secondaryPotion.getFullID())) {
                    if (!inventorySnapshot.containsAny(secondaryPotion.getItemIDs())) {
                        log(NightmareZone.class, "No secondary potions in inventory or bank, stopping script!");
                        stop();
                    }
                } else {
                    log(NightmareZone.class, "Secondary needs bank");
                    if (!dynamicTasks.contains(Task.OPEN_BANK)) {
                        dynamicTasks.add(Task.OPEN_BANK);
                    }
                }
            }
        }

        if (!setupDream) {
            dynamicTasks.add(Task.SETUP_DREAM);
        }

        if (!dynamicTasks.isEmpty()) {
            log(NightmareZone.class, "Tasks: " + dynamicTasks);
            // if the previous task is still eligible to execute, then keep executing it until it doesn't contain anymore
            if (previousTask != null && dynamicTasks.contains(previousTask)) {
                return previousTask;
            } else {
                previousTask = dynamicTasks.get(random(dynamicTasks.size()));
                return previousTask;
            }
        }
        log(NightmareZone.class, "Tasks empty, entering dream.");
        return Task.ENTER_DREAM;
    }

    private Task getArenaTask(boolean walking, WorldPosition worldPosition) {
        // reset cached reward points
        if (cachedRewardPoints != null) {
            cachedRewardPoints = null;
            outOfPointsFlag = false;
        }

        // equip items if necessary
        if (hasItemsToEquip()) {
            return Task.EQUIP_ITEMS;
        }

        if (getProfileManager().isDueToBreak()) {
            log(NightmareZone.class, "Due to break, suiciding...");
            return Task.SUICIDE;
        }
        if (statBoostPotion != null && noBoostSuicide) {
            int statBoostPotionDoses = getDoses(boostPotions, statBoostPotion);
            if (statBoostPotionDoses == 0) {
                log("No boost doses left, suiciding...");
                // suicide and restock
                return Task.SUICIDE;
            }
        }

        if (!walking && !afkPosition.getArea().contains(worldPosition)) {
            return Task.WALK_TO_AFK_POS;
        }
        UIResult<Integer> prayerPoints = getWidgetManager().getMinimapOrbs().getPrayerPointsPercentage();
        UIResult<Boolean> quickPrayersActive = getWidgetManager().getMinimapOrbs().isQuickPrayersActivated();

        // Check prayers activated
        if (secondaryPotion == StandardPotion.PRAYER_POTION && quickPrayersActive.isFound()) {
            if (!quickPrayersActive.get()) {
                if (prayerDelayTimer == null) {
                    prayerDelayTimer = new Stopwatch(random(1000, 20000));
                } else if (prayerDelayTimer.hasFinished()) {
                    dynamicTasks.add(Task.ACTIVATE_PRAY);
                }
            }
        }

        // drink absorptions
        if (secondaryPotion == BarrelPotion.ABSORPTION_POTION) {
            if (absorptionPointsOverlay.isVisible()) {
                Integer points = (Integer) absorptionPointsOverlay.getValue(AbsorptionPointsOverlay.POINTS);
                // if points lower than nextSecondaryDrink, drink absorption potion
                if (points != null && points <= nextSecondaryDrink) {
                    dynamicTasks.add(Task.DRINK_ABSORPTION);
                }
            } else {
                // if interface is not visible, drink absorptions
                dynamicTasks.add(Task.DRINK_ABSORPTION);
            }
        } else if (secondaryPotion == StandardPotion.PRAYER_POTION) {
            // prayer potion
            if (prayerPoints.isNotVisible() || quickPrayersActive.isNotVisible()) {
                log(NightmareZone.class, "Prayer points orb not visible, make sure regeneration indicators are disabled...");
                return null;
            }
            if (prayerPoints.isFound()) {
                log(NightmareZone.class, "Prayer points: " + prayerPoints.get() + "% | Next drinking at: " + nextSecondaryDrink);
                if (prayerPoints.get() <= nextSecondaryDrink) {
                    dynamicTasks.add(Task.DRINK_PRAYER_POTION);
                }
            }
        }

        // lower health if needs be
        UIResult<Integer> hitpoints = getWidgetManager().getMinimapOrbs().getHitpoints();
        UIResult<Integer> hitpointsPercentage = getWidgetManager().getMinimapOrbs().getHitpointsPercentage();
        if (hitpoints.isNotVisible() || hitpointsPercentage.isNotVisible()) {
            log(NightmareZone.class, "Hitpoints orb not visible, make sure regeneration indicators are disabled...");
            return null;
        }
        if (hitpoints.isNotFound() || hitpointsPercentage.isNotVisible()) {
            log(NightmareZone.class, "Hitpoints value not found...");
            return null;
        }
        // drink stat boost pot
        boolean canDrink;
        if (statBoostPotion == BarrelPotion.OVERLOAD) {
            if (hitpointsPercentage.get() == 100 && hitpoints.get() < 51) {
                log(NightmareZone.class, "Overload potion not drinkable, hitpoints at 100% and above 51. You need at least 51 hitpoints to drink an overload potion.");
            }
            canDrink = hitpoints.get() >= 51 && !overloadBuffOverlay.isVisible();
        } else {
            canDrink = nextBoostPotionDrink.hasFinished();
        }
        if (statBoostPotion != null && canDrink && !boostPotions.isEmpty()) {
            if (statBoostPotionDelay == null) {
                statBoostPotionDelay = new Stopwatch(random(1000, 25000));
            } else if (statBoostPotionDelay.hasFinished()) {
                dynamicTasks.add(Task.DRINK_POTIONS);
            }
        }


        if (flickRapidHeal && rapidHealFlickTimer.hasFinished() && hitpoints.get() == 1) {
            dynamicTasks.add(Task.FLICK_PRAYER);
        }


        if (secondaryPotion == BarrelPotion.ABSORPTION_POTION && hitpoints.isFound() && hitpoints.get() > 1) {
            if (lowerHPDelayTimer != null && lowerHPDelayTimer.hasFinished()) {
                if (canEatDownHP()) {
                    dynamicTasks.add(Task.LOWER_HP);
                }
            } else {
                if (lowerHPDelayTimer == null) {
                    int min = (int) TimeUnit.SECONDS.toMillis(5);
                    // allow to reach ~3hp
                    int max = (int) TimeUnit.SECONDS.toMillis(120);
                    lowerHPDelayTimer = new Stopwatch(RandomUtils.weightedRandom(min, max, 0.0001));
                }
            }
        }
        if (!dynamicTasks.isEmpty()) {
//            if (previousTask != null && dynamicTasks.contains(previousTask)) {
//                // if we still need to do the previous task
//                return previousTask;
//            } else {
//                // update the previous task
//                previousTask = dynamicTasks.get(random(dynamicTasks.size()));
//                return previousTask;
//            }
            return dynamicTasks.get(random(dynamicTasks.size()));
        }
        // use special attack
        if (specialAttackWeapon != null && specialDelayTimer.hasFinished()) {
            UIResult<Integer> specialAttackAmount = getWidgetManager().getMinimapOrbs().getSpecialAttackPercentage();
            if (specialAttackAmount.isNotVisible()) {
                log(NightmareZone.class, "Special attack orb not visible...");
                return null;
            }
            if (specialAttackAmount.isFound() && specialAttackAmount.get() >= specialAttackWeapon.getAmount()) {
                if (specialDelayTimer == null) {
                    specialDelayTimer = new Stopwatch(random(2000, 30000));
                } else if (specialDelayTimer.hasFinished()) {
                    return Task.USE_SPECIAL_ATTACK;
                }

            }
        }

        // switch tabs randomly to prevent afk log
        if (switchTabTimer.hasFinished()) {
            log(NightmareZone.class, "Switching tabs...");
            getWidgetManager().getTabManager().openTab(Tab.Type.values()[random(Tab.Type.values().length)]);
            switchTabTimer.reset(random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
        }
        return null;
    }

    private boolean shouldInteractWithBarrel(Potion potionType, Set<ItemSearchResult> potions, int requiredAmount) {
        if (potionType instanceof BarrelPotion barrelPotion) {
            if (!barrelDoseCache.containsKey(barrelPotion)) {
                log(NightmareZone.class, "Cache does not contain barrel potion: " + barrelPotion.getName() + ".");
                return false;
            }
            int storedDoses = barrelDoseCache.get(barrelPotion);
            // Don't interact if no doses available
            if (storedDoses <= 0) {
                return false;
            }

            int doses = getDoses(potions, barrelPotion);
            int targetDoses = requiredAmount * 4;

            // If we have enough doses already, don't interact
            if (doses >= targetDoses) {
                return false;
            }

            // don't interact if we can afford to buy more as that will be a valid task. If we don't add this then we will potentially restock, buy more, then restock again
            return cachedRewardPoints == null || cachedRewardPoints < barrelPotion.getCostPerDose() || storedDoses >= targetDoses;
        }
        // if not a barrel potion, we don't need to interact with the barrel
        return false;
    }

    private void executeTask(Task task) {
        switch (task) {
            case HANDLE_BANK -> handleBank();
            case RESTOCK_BOOST_POTIONS -> restockBoostPotions();
            case RESTOCK_SECONDARY_POTIONS -> restockSecondaryPotions();
            case HANDLE_CHEST -> handleChest();
            case SETUP_DREAM -> setupDream();
            case ENTER_DREAM -> enterDream();
            case WALK_TO_AFK_POS -> walkToAFKPos();
            case LOWER_HP -> lowerHealth(random(2000, 8000));
            case EQUIP_ITEMS -> equipItems();
            case DRINK_ABSORPTION -> drinkAbsorption(random(2000, 8000));
            case ACTIVATE_PRAY -> activatePrayer();
            case DRINK_PRAYER_POTION -> drinkPrayerPotion();
            case SUICIDE -> suicide();
            case FLICK_PRAYER -> flickPrayer();
            case OPEN_BANK -> openBank();
            case DRINK_POTIONS -> drinkBoostPotion();
            case USE_SPECIAL_ATTACK -> activateSpecialAttack();
        }
    }

    private void activateSpecialAttack() {
        UIResult<Integer> previousPercentageResult = getWidgetManager().getMinimapOrbs().getSpecialAttackPercentage();
        if (!percentageValid(previousPercentageResult)) {
            return;
        }
        if (getWidgetManager().getMinimapOrbs().setSpecialAttack(true)) {
            boolean result = submitTask(() -> {
                UIResult<Boolean> activated = getWidgetManager().getMinimapOrbs().isSpecialAttackActivated();
                if (!percentageValid(activated)) {
                    return false;
                }
                return !activated.get();
            }, 6000);
            if (result) {
                specialDelayTimer = null;
            }
        }
    }

    private boolean percentageValid(UIResult result) {
        if (result.isNotVisible()) {
            log(NightmareZone.class, "Special attack percentage not visible.");
            return false;
        }
        if (result.isNotFound()) {
            log(NightmareZone.class, "Can't find special attack %");
            return false;
        }
        return true;
    }

    private void drinkBoostPotion() {
        ItemSearchResult potion = inventorySnapshot.getRandomItem(statBoostPotion.getItemIDs());
        String itemName = getItemManager().getItemName(potion.getId());
        int initialDoseAmount = getDoses(boostPotions, statBoostPotion);
        if (itemName != null)
            log(NightmareZone.class, "Drinking " + itemName + "... Initial dose amount: " + initialDoseAmount);
        if (!potion.interact()) {
            return;
        }
        // wait for potion to be non transparent again before searching
        submitTask(() -> false, 800);
        // search for items and calculate dose amount, then compare to the previous dose amount to confirm we drank the potion.
        boolean dosesDecremented = submitTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                log(NightmareZone.class, "Failed opening inventory.");
                return false;
            }
            boostPotions = inventorySnapshot.getAllOfItems(statBoostPotion.getItemIDs());
            int currentDoses = getDoses(boostPotions, statBoostPotion);
            return currentDoses < initialDoseAmount || (statBoostPotion == BarrelPotion.OVERLOAD && overloadBuffOverlay.isVisible());
        }, 3000);
        log(NightmareZone.class, "Dose amount decremented: " + dosesDecremented);
        if (dosesDecremented || (statBoostPotion == BarrelPotion.OVERLOAD && overloadBuffOverlay.isVisible())) {
            if (statBoostPotion == BarrelPotion.OVERLOAD) {
                int randomTime = noLowerHPDelay ? random(8000, 12000) : random(10000, 20000);
                lowerHPDelayTimer = new Stopwatch(randomTime);
                statBoostPotionDelay = null;
            } else {
                long min = TimeUnit.MINUTES.toMillis(3);
                long max = TimeUnit.MINUTES.toMillis(5);
                nextBoostPotionDrink.reset(random(min, max));
            }
        }

    }

    private boolean canEatDownHP() {
        if (statBoostPotion == BarrelPotion.OVERLOAD) {
            return overloadBuffOverlay.isVisible();
        }
        return true;
    }

    private void flickPrayer() {
        UIResult<Boolean> prayersActivated = getWidgetManager().getMinimapOrbs().isQuickPrayersActivated();
        if (prayersActivated.isNotVisible()) {
            log(NightmareZone.class, "Prayer orb not visible, make sure regeneration indicators are disabled...");
            return;
        }

        if (prayersActivated.isFound() && prayersActivated.get()) {
            log(NightmareZone.class, "Prayers activated already");
            if (getWidgetManager().getMinimapOrbs().setQuickPrayers(false)) {
                log(NightmareZone.class, "Disabled prayers, resetting");
                rapidHealFlickTimer.reset(random(20000, 50000));
            }
        } else if (getWidgetManager().getMinimapOrbs().setQuickPrayers(true)) {
            log(NightmareZone.class, "Prayers activated");
            boolean result = getWidgetManager().getMinimapOrbs().setQuickPrayers(false);
            if (result) {
                log(NightmareZone.class, "Activated & prayers, resetting");
                rapidHealFlickTimer.reset(random(20000, 50000));
            }
        }
    }

    private void drinkPrayerPotion() {
        log(NightmareZone.class, "Drinking prayer potion...");
        if (secondaryPotions.size() <= 0) {
            return;
        }
        ItemSearchResult potion = inventorySnapshot.getRandomItem(secondaryPotion.getItemIDs());
        String itemName = getItemManager().getItemName(potion.getId());
        int initialDoseAmount = getDoses(secondaryPotions, secondaryPotion);
        if (itemName != null)
            log(NightmareZone.class, "Drinking " + itemName + "...");
        if (!potion.interact()) {
            return;
        }
        // wait for potion to be non transparent again before searching
        submitTask(() -> false, 800);
        // search for items and calculate dose amount, then compare to the previous dose amount to confirm we drank the potion.
        boolean dosesDecremented = submitTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                log(NightmareZone.class, "Failed opening inventory.");
                return false;
            }
            secondaryPotions = inventorySnapshot.getAllOfItems(secondaryPotion.getItemIDs());
            return getDoses(secondaryPotions, secondaryPotion) < initialDoseAmount;
        }, 3000);

        if (dosesDecremented) {
            nextSecondaryDrink = random(20, 60);
        }
    }

    private void activatePrayer() {
        log(NightmareZone.class, "Activating prayer...");
        if (getWidgetManager().getMinimapOrbs().setQuickPrayers(true)) {
            prayerDelayTimer = null;
        }
    }

    private void drinkAbsorption(int timeout) {
        log(NightmareZone.class, "Drinking absorptions...");
        submitTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                log(NightmareZone.class, "Failed opening inventory.");
                return false;
            }
            Integer absorptionPoints = (Integer) absorptionPointsOverlay.getValue(AbsorptionPointsOverlay.POINTS);
            if (absorptionPoints == null) {
                absorptionPoints = 0;
            }
            if (absorptionPoints > nextSecondaryDrink) {
                return true;
            }
            ItemSearchResult absorption = inventorySnapshot.getRandomItem(secondaryPotion.getItemIDs());
            getFinger().tap(false, absorption);
            submitTask(() -> false, RandomUtils.gaussianRandom(350, 1200, 500, 500));
            return false;
        }, timeout);
        nextSecondaryDrink = random(50, 200);
    }

    private void equipItems() {
        ItemSearchResult item = itemsToEquip.get(random(itemsToEquip.size()));
        log(NightmareZone.class, "Equipping item: " + item.getId());
        item.interact();
    }

    private void walkToAFKPos() {
        log(NightmareZone.class, "Walking to AFK pos...");
        WorldPosition myPos = getWorldPosition();
        if (myPos == null) {
            log(NightmareZone.class, "Position is null...");
            return;
        }
        WalkConfig.Builder builder = new WalkConfig.Builder().breakDistance(0);
        builder.doWhileWalking(() -> {
            Task task = decideTask(true);
            if (task != null)
                executeTask(task);
            return null;
        });
        builder.breakCondition(() -> {
            // break condition
            WorldPosition myPos_ = getWorldPosition();
            if (myPos_ == null) {
                log(NightmareZone.class, "Position is null...");
                return false;
            }
            if (afkPosition.getArea().contains(myPos_)) {
                return true;
            }
            return false;
        });
        getWalker().walkTo(afkPosition.getArea().getRandomPosition(), builder.build());
    }

    private void lowerHealth(int timeout) {
        submitTask(() -> {
            UIResult<Integer> healthResult = getWidgetManager().getMinimapOrbs().getHitpoints();
            if (healthResult.isNotVisible()) {
                log(NightmareZone.class, "Hitpoints orb not visible, make sure regeneration indicators are disabled...");
                return false;
            }
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                log(NightmareZone.class, "Failed opening inventory.");
                return false;
            }
            ItemSearchResult lowerHealthItem = inventorySnapshot.getItem(lowerHealthMethod.getItemID());
            if (lowerHealthItem == null) {
                log(NightmareZone.class, "Lower health item not found in inventory: " + lowerHealthMethod.getName());
                return false;
            }
            if (healthResult.isFound()) {
                if (healthResult.get() <= 1) {
                    lowerHPDelayTimer = null;
                    return true;
                }
                if (lowerHealthMethod == LowerHealthMethod.ROCK_CAKE && !guzzleFirstOption) {
                    UIResult<Point> point = lowerHealthItem.getRandomPointInSlot();
                    if (point.isNotFound()) {
                        return false;
                    }
                    // check if "Guzzle" is first option
                    MenuHook menuHook = menuEntries -> {
                        for (int i = 0; i < menuEntries.size(); i++) {
                            MenuEntry menuEntry = menuEntries.get(i);
                            if (menuEntry.getRawText().toLowerCase().startsWith("guzzle")) {
                                guzzleFirstOption = i == 0;
                                return menuEntry;
                            }
                        }
                        return null;
                    };
                    getFinger().tap(false, point.get(), menuHook);
                    submitTask(() -> false, RandomUtils.gaussianRandom(300, 2500, 200, 700));
                } else {
                    getFinger().tap(false, lowerHealthItem);
                    submitTask(() -> false, RandomUtils.gaussianRandom(300, 2500, 200, 700));
                }
            }
            return false;
        }, timeout);
    }

    @Override
    public int onRelog() {
        guzzleFirstOption = false;
        return 0;
    }

    private void enterDream() {
        if (getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
            UIResult<String> dialogueText = getWidgetManager().getDialogue().getText();
            if (dialogueText.isFound()) {
                log(NightmareZone.class, "Dialogue text: " + dialogueText.get());
                if (dialogueText.get().contains("in the coffer before you can start this dream")) {
                    log(NightmareZone.class, "No coins left in the coffer, stopping script!");
                    this.stop();
                    return;
                }
            } else {
                log(NightmareZone.class, "Dialogue text not found.");
            }
        }
        if (!potionInterface.isVisible()) {
            log(NightmareZone.class, "Interacting with potion...");
            interactWithPotion();
            return;
        }
        if (!potionInterface.accept()) {
            return;
        }
        setExpectedRegionId(ARENA_REGION);
        // wait until inside arena
        submitHumanTask(() -> {
            WorldPosition worldPosition = getWorldPosition();
            if (worldPosition == null) {
                return false;
            }
            if (isArena(worldPosition)) {
                lowerHPDelayTimer = new Stopwatch(random(1000, 13000));
                prayerDelayTimer = new Stopwatch(random(1000, 13000));
                statBoostPotionDelay = new Stopwatch(random(1000, 13000));
                switchTabTimer.reset(random(TimeUnit.MINUTES.toMillis(3), TimeUnit.MINUTES.toMillis(5)));
                log(NightmareZone.class, "Executing delay...");
                submitTask(() -> {
                    // highlight screen blue to show we're executing this delay
                    Canvas canvas = getScreen().getDrawableCanvas();
                    canvas.fillRect(getScreen().getBounds(), Color.BLUE.getRGB(), 0.5);
                    return false;
                }, random(500, 7000));
                log(NightmareZone.class, "Finished.");
                return true;
            }
            return false;
        }, 5000);
    }

    private void setupDream() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return;
        }
        DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == null) {
            log(NightmareZone.class, "Interacting with Dominic");
            interactWithDominic();
            return;
        }
        boolean invalidDialogue = !isDominicDialogue(dialogueType);
        if (setupDream) {
            return;
        }
        if (invalidDialogue) {
            log(NightmareZone.class, "Interacting with Dominic");
            interactWithDominic();
            return;
        }
        log(NightmareZone.class, "Handling Dominic dialogue");
        handleDominicDialogue(dialogueType);
    }

    private boolean isDominicDialogue(DialogueType dialogueType) {
        log(NightmareZone.class, "Checking dialogue type: " + dialogueType);
        switch (dialogueType) {
            case CHAT_DIALOGUE -> {
                UIResult<String> dialogueTextResult = getWidgetManager().getDialogue().getText();
                if (dialogueTextResult.isNotVisible()) {
                    return false;
                }
                String text = dialogueTextResult.get();
                log(NightmareZone.class, "Dialogue: " + text);

                if (text.contains("You should put the money in my coffer")) {
                    log(NightmareZone.class, "No coins left in the coffer, stopping script!");
                    this.stop();
                    throw new TaskInterruptedException("Not enough coins in the coffer to start a dream.");
                }
                if (Dialogues.DOMINIC_SUCCESS_CHAT_DIALOGUES.contains(text)) {
                    setupDream = true;
                    return true;
                }
                return Dialogues.DOMINIC_CHAT_DIALOGUES.contains(text.toLowerCase()) || text.toLowerCase().startsWith("for a customisable rumble dream,");
            }
            case TEXT_OPTION -> {
                UIResult<String> title = getWidgetManager().getDialogue().getDialogueTitle();
                if (title.isNotFound()) {
                    log(NightmareZone.class, "Title is null");
                    return false;
                }
                log(NightmareZone.class, "Text option dialogue title: " + title);
                for (DialogueOption domOption : Dialogues.DOMINIC_DIALOGUE_OPTIONS) {
                    log(NightmareZone.class, "Title: " + domOption.title);
                    if (title.get().toLowerCase().startsWith(domOption.title.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean handleDominicDialogue(DialogueType dialogueType) {
        switch (dialogueType) {
            case CHAT_DIALOGUE -> {
                return getWidgetManager().getDialogue().continueChatDialogue();
            }
            case TEXT_OPTION -> {
                UIResult<String> title = getWidgetManager().getDialogue().getDialogueTitle();
                if (title.isNotFound()) {
                    log(NightmareZone.class, "Dialogue title not found...");
                    return false;
                }
                UIResult<String[]> options = getWidgetManager().getDialogue().getOptions();
                if (options.isNotFound()) {
                    log(NightmareZone.class, "No dialogue options found...");
                    return false;
                }
                for (DialogueOption domOption : Dialogues.DOMINIC_DIALOGUE_OPTIONS) {
                    log(NightmareZone.class, "Title: " + title.get() + " Dom Title: " + domOption.title);
                    if (!title.get().toLowerCase().startsWith(domOption.title.toLowerCase())) continue;
                    log(NightmareZone.class, "Checking options...");
                    for (String option : options.get()) {
                        log(NightmareZone.class, "Option: " + option + " Our option: " + domOption.option);
                        if (option.toLowerCase().startsWith(domOption.option.toLowerCase())) {
                            log(NightmareZone.class, "Option matches, selecting");
                            getWidgetManager().getDialogue().selectOption(option);
                            return true;
                        }
                    }
                }
                return false;
            }
        }
        return false;
    }

    private void interactWithDominic() {
        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return;
        }
        Polygon tilePoly = getSceneProjector().getTileCube(DOMINIC_POSITION, 80);
        if (tilePoly == null || (tilePoly = tilePoly.getResized(0.7)) == null ||
                !getWidgetManager().insideGameScreen(tilePoly, List.of(ChatboxComponent.class)) ||
                DOMINIC_POSITION.distanceTo(myPosition) > 13) {
            walkToDominic();
            return;
        }
        if (getFinger().tapGameScreen(tilePoly, "Dream")) {
            submitTask(() -> getWidgetManager().getDialogue().getDialogueType() != null, random(4000, 6000));
        }
    }

    private void walkToDominic() {
        log(NightmareZone.class, "Walking to Dominic...");
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> {
            WorldPosition worldPosition = getWorldPosition();
            if (worldPosition == null) {
                return false;
            }
            if (DOMINIC_POSITION.distanceTo(worldPosition) >= 13) {
                return false;
            }
            Polygon tilePoly2 = getSceneProjector().getTileCube(DOMINIC_POSITION, 80);
            if (tilePoly2 == null || (tilePoly2 = tilePoly2.getResized(0.7)) == null) {
                return false;
            }
            return getWidgetManager().insideGameScreen(tilePoly2, List.of(ChatboxComponent.class));
        });
        getWalker().walkTo(DOMINIC_POSITION, builder.build());
    }

    private void interactWithPotion() {
        RSTile potionTile = getSceneManager().getTile(POTION_TILE);
        if (potionTile == null) {
            walkToPotion();
            return;
        }
        boolean isGameScreen = false;
        Polygon tilePoly = potionTile.getTilePoly();
        if (tilePoly == null || (tilePoly = tilePoly.getResized(0.7)) == null) {
            log(NightmareZone.class, "Potion tile polygon is null.");
            getWalker().walkTo(POTION_TILE, new WalkConfig.Builder().breakDistance(1).build());
            return;
        }
        isGameScreen = getWidgetManager().insideGameScreen(tilePoly, List.of(ChatboxComponent.class));

        WorldPosition myPosition = getWorldPosition();
        if (myPosition == null) {
            return;
        }
        // walk to if distance is > 14 as the max render distance is 15
        if (!isGameScreen || DOMINIC_POSITION.distanceTo(myPosition) > 13) {
            walkToPotion();
            return;
        }
        if (!getFinger().tapGameScreen(tilePoly, menuEntries -> {
            // pre check for investigate option
            for (MenuEntry entry : menuEntries) {
                if (entry.getRawText().startsWith("investigate")) {
                    log(NightmareZone.class, "Investigate option found, it seems we are mistaken & the dream isn't setup!");
                    setupDream = false;
                    return null;
                }
            }

            for (MenuEntry entry : menuEntries) {
                if (entry.getRawText().startsWith("drink")) {
                    return entry;
                }
            }
            return null;
        })) {
            // failed to interact, just poll again from the top
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

            return potionInterface.isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000);
    }

    private void walkToPotion() {
        log(NightmareZone.class, "Walking to dominic tile");
        // walk to tile
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> {
            if (getWorldPosition() == null) {
                return false;
            }
            RSTile potionTile = getSceneManager().getTile(POTION_TILE);
            if (potionTile == null) {
                return false;
            }
            if (potionTile.distance() >= 13) {
                return false;
            }
            Polygon tilePoly2 = potionTile.getTilePoly();
            if (tilePoly2 == null || (tilePoly2 = tilePoly2.getResized(0.7)) == null) {
                return false;
            }
            return getWidgetManager().insideGameScreen(tilePoly2, List.of(ChatboxComponent.class));
        });
        getWalker().walkTo(POTION_TILE, builder.build());
    }

    private void handleChest() {
        if (!chestInterface.isVisible()) {
            // open chest
            log(NightmareZone.class, "Chest is not visible, opening...");
            openChest();
            return;
        }
        UIResult<String> selectedTab = chestInterface.getSelectedTab();
        if (selectedTab.isNotFound()) {
            log(NightmareZone.class, "No buttons found.");
            return;
        }

        if (!selectedTab.get().equalsIgnoreCase("benefits")) {
            // switch to tab
            chestInterface.selectButton("benefits");
            return;
        }

        UIResult<Integer> points = chestInterface.getPoints();
        if (points.isNotFound()) {
            return;
        }
        cachedRewardPoints = points.get();

        // update barrel dose cache
        for (BarrelPotion p : BarrelPotion.values()) {
            int oneDoseID = p.getItemIDForDose(1);
            log(NightmareZone.class, "one dose ID: " + oneDoseID + " for potion: " + p.getName());
            UIResult<Integer> storedDoses = chestInterface.getStoredDoses(oneDoseID);
            if (storedDoses.isNotVisible()) {
                return;
            }
            if (storedDoses.isNotFound()) {
                log(NightmareZone.class, "Can't find stored doses for potion: " + p.getName());
                return;
            }
        }
        // if we have enough points, fully stock up to save us running to the chest all of the time.
        boolean balling = points.get() >= 1000000;

        BarrelPotion boost = statBoostPotion instanceof BarrelPotion ? (BarrelPotion) statBoostPotion : null;
        BarrelPotion secondary = secondaryPotion instanceof BarrelPotion ? (BarrelPotion) secondaryPotion : null;

        int boostDosesToBuy = 0;
        int secondaryDosesToBuy = 0;

        if (boost != null) {
            UIResult<Integer> storedBoostDoses = chestInterface.getStoredDoses(statBoostPotion.getItemIDForDose(1));
            if (storedBoostDoses.isNotFound()) {
                return;
            }
            int boostDoses = storedBoostDoses.get();
            barrelDoseCache.put(boost, boostDoses);

            int targetDoses = statBoostPotionAmount * 4;
            int doses = getDoses(boostPotions, statBoostPotion);
            int requiredDoses = targetDoses - doses;
            requiredDoses -= boostDoses;

            if (requiredDoses > 0) {
                boostDosesToBuy += requiredDoses;
            }
        }
        if (secondary != null) {
            UIResult<Integer> storedSecondaryDoses = chestInterface.getStoredDoses(secondaryPotion.getItemIDForDose(1));
            if (storedSecondaryDoses.isNotFound()) {
                return;
            }
            int secondaryDoses = storedSecondaryDoses.get();
            barrelDoseCache.put(secondary, secondaryDoses);

            int amountRequired = inventorySnapshot.getFreeSlots();
            // log("Amount required: " + amountRequired);
            int targetDoses = amountRequired * 4;
            //   log("Target doses: " + targetDoses);
            int currentDoses = getDoses(secondaryPotions, secondaryPotion);
            //  log("Current doses: " + currentDoses);
            int requiredDoses = targetDoses - currentDoses;
            //  log("Required doses: " + requiredDoses);
            requiredDoses -= secondaryDoses;
            // log("Required doses minus stored doses: " + requiredDoses);

            if (requiredDoses > 0) {
                secondaryDosesToBuy += requiredDoses;
            }
        }

        // if we don't have enough for both then prioritise absorptions
        int primaryCost = boost == null ? 0 : boostDosesToBuy * boost.getCostPerDose();
        int secondaryCost = secondary == null ? 0 : secondaryDosesToBuy * secondary.getCostPerDose();

        int totalCost = primaryCost + secondaryCost;

        boolean prioritiseSecondary = secondaryDosesToBuy > 0 && totalCost > points.get();

        List<PotionBuyEntry> potionBuyEntries = new ArrayList<>();


        if (secondary != null && secondaryDosesToBuy > 0) {
            int affordableDoses = points.get() / secondary.getCostPerDose();
            potionBuyEntries.add(new PotionBuyEntry(secondary.getItemIDForDose(1), balling ? 255 : affordableDoses, barrelDoseCache.getOrDefault(secondary, 0)));
        }

        if (boost != null && !prioritiseSecondary && boostDosesToBuy > 0) {
            int affordableDoses = points.get() / boost.getCostPerDose();
            potionBuyEntries.add(new PotionBuyEntry(boost.getItemIDForDose(1), balling ? 255 : affordableDoses, barrelDoseCache.getOrDefault(boost, 0)));
        }

        log("Secondary doses to buy: " + secondaryDosesToBuy + " Boost doses to buy: " + boostDosesToBuy + " Prioritise secondary: " + prioritiseSecondary);
        for (PotionBuyEntry potionBuyEntry : potionBuyEntries) {
            if (potionBuyEntry.getRequiredDoses() == 0) {
                outOfPointsFlag = true;
                break;
            }
        }
        if (potionBuyEntries.isEmpty() || outOfPointsFlag) {
            log(NightmareZone.class, "Potion buy entries: " + potionBuyEntries.size() + " outOfPointsFlag: " + outOfPointsFlag);
            // close
            chestInterface.close();
        } else {
            PotionBuyEntry potionBuyEntry = potionBuyEntries.get(random(potionBuyEntries.size()));
            buyPotion(potionBuyEntry);
        }
    }

    private void buyPotion(PotionBuyEntry potionBuyEntry) {
        int itemID = potionBuyEntry.getItemId();
        int requiredDoses = potionBuyEntry.getRequiredDoses();
        int initialStoredDoses = potionBuyEntry.getInitialStockedDoses();
        log(NightmareZone.class, "Buying item ID: " + itemID + ".");
        ItemGroupResult chestSnapshot = getItemManager().scanItemGroup(chestInterface, Set.of(itemID));
        if (chestSnapshot == null) {
            log(NightmareZone.class, "Failed scanning chest interface for item ID: " + itemID + ".");
            return;
        }
        ItemSearchResult itemSearchResult = chestSnapshot.getItem(itemID);
        if (itemSearchResult == null) {
            log(NightmareZone.class, "Item ID: " + itemID + " not found in chest interface.");
            return;
        }
        if (!itemSearchResult.interact("buy-x")) {
            log(NightmareZone.class, "Failed interacting with item.");
            return;
        }
        if (submitTask(() -> getWidgetManager().getDialogue().getDialogueType() == DialogueType.ENTER_AMOUNT, 5000)) {
            // enter amount visible
            if (requiredDoses == 255) {
                // if we need max doses just enter a random number equal or higher to as some kind of anti-ban
                requiredDoses = random(255, 10000);
            }
            getKeyboard().type(String.valueOf(requiredDoses));
            sleep(100);
            getKeyboard().pressKey(TouchType.DOWN_AND_UP, PhysicalKey.ENTER);
            submitTask(() -> {
                UIResult<Integer> storedDoses_ = chestInterface.getStoredDoses(itemID);
                if (storedDoses_.isNotFound()) {
                    return false;
                }
                return storedDoses_.get() > initialStoredDoses;
            }, 4000);
        }

    }

    private Set<Integer> getItemsToIgnore() {
        Set<Integer> itemsToIgnore = new HashSet<>(RUNES);
        if (specialAttackWeapon != null) {
            itemsToIgnore.add(specialAttackWeapon.getItemID());
        }
        if (weaponItemID != -1) {
            itemsToIgnore.add(weaponItemID);
        }
        if (shieldItemID != -1) {
            itemsToIgnore.add(shieldItemID);
        }
        if (statBoostPotion != null) {
            if (statBoostPotion instanceof BarrelPotion) {
                itemsToIgnore.addAll(statBoostPotion.getItemIDs());
            } else {
                // if the potion is from the bank, then only keep full doses
                itemsToIgnore.add(statBoostPotion.getFullID());
            }
        }
        if (secondaryPotion != null) {
            if (secondaryPotion instanceof BarrelPotion) {
                itemsToIgnore.addAll(secondaryPotion.getItemIDs());
            } else {
                // if the potion is from the bank, then only keep full doses
                itemsToIgnore.add(secondaryPotion.getFullID());
            }
        }
        if (lowerHealthMethod != null) {
            itemsToIgnore.add(lowerHealthMethod.getItemID());
        }
        return itemsToIgnore;
    }

    private void handleBank() {
        // deposit unwanted items
        Set<Integer> itemsToIgnore = getItemsToIgnore();
        if (!getWidgetManager().getBank().depositAll(itemsToIgnore)) {
            // if we fail to deposit all
            return;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        bankSnapshot = getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null || bankSnapshot == null) {
            log(NightmareZone.class, "Can't get inventory or bank snapshot.");
            return;
        }
        // at this point all unwanted items are gone.
        boolean handleStatBoostPotion = statBoostPotion instanceof StandardPotion;
        boolean handleSecondaryPotion = secondaryPotion instanceof StandardPotion;

        // find secondary potions IF ENABLED
        if (secondaryPotion != null) {
            secondaryPotions = inventorySnapshot.getAllOfItem(secondaryPotion.getFullID());
        }

        // find boost potions IF ENABLED
        if (statBoostPotion != null) {
            boostPotions = inventorySnapshot.getAllOfItem(statBoostPotion.getFullID());
        }

        Map<Integer, Integer> itemsToWithdraw = new HashMap<>();

        if (handleStatBoostPotion) {
            if (bankSnapshot.contains(statBoostPotion.getFullID())) {
                int amountToWithdraw = statBoostPotionAmount - boostPotions.size();
                log(NightmareZone.class, "Amount needed: " + statBoostPotionAmount + " Amount to withdraw: " + amountToWithdraw);
                if (amountToWithdraw != 0)
                    itemsToWithdraw.put(statBoostPotion.getFullID(), amountToWithdraw);
            }
        }
        if (handleSecondaryPotion) {
            if (bankSnapshot.contains(secondaryPotion.getFullID())) {
                int amountNeeded = getSecondaryPotionsNeeded();
                log(NightmareZone.class, "Amount needed: " + amountNeeded);
                if (amountNeeded != 0)
                    itemsToWithdraw.put(secondaryPotion.getFullID(), amountNeeded);
            }
        }

        if (itemsToWithdraw.isEmpty()) {
            getWidgetManager().getBank().close();
            return;
        }
        // withdraw random item out of the map
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(itemsToWithdraw.entrySet());

        // get a random entry
        Map.Entry<Integer, Integer> randomEntry = entries.get(random(entries.size()));

        int itemId = randomEntry.getKey();
        int amount = randomEntry.getValue();
        if (amount < 0) {
            // deposit
            log(NightmareZone.class, "Depositing item ID: " + itemId + ", amount: " + amount);
            getWidgetManager().getBank().deposit(itemId, Math.abs(amount));
        } else {
            log(NightmareZone.class, "Withdrawing item ID: " + itemId + ", amount: " + amount);
            if (!bankSnapshot.contains(itemId)) {
                if (inventorySnapshot.contains(itemId)) {

                }
                log(NightmareZone.class, "Item ID: " + itemId + " not found in bank. Stopping script...");
                stop();
                return;
            }
            getWidgetManager().getBank().withdraw(itemId, amount);
        }
    }


    private int getSecondaryPotionsNeeded() {
        int boostPots = statBoostPotion != null ? statBoostPotionAmount : 0;
        List<Integer> potionsToIgnore = new ArrayList<>(secondaryPotion.getItemIDs());
        if (statBoostPotion != null) {
            potionsToIgnore.addAll(secondaryPotion.getItemIDs());
        }

        int freeSlotsExcludingPotions = inventorySnapshot.getFreeSlots(potionsToIgnore);
        return freeSlotsExcludingPotions - boostPots - secondaryPotions.size();
    }

    private void restockSecondaryPotions() {
        if (secondaryPotion instanceof BarrelPotion barrelPotion) {
            int amountNeeded = inventorySnapshot.getFreeSlots() + secondaryPotions.size();
            log(NightmareZone.class, "Free slots exl absorptions: " + amountNeeded);
            if (statBoostPotion != null) {
                int boostPotionsNeeded = statBoostPotionAmount;
                boostPotionsNeeded -= boostPotions.size();
                if (boostPotionsNeeded < 0) {
                    log(NightmareZone.class, "Too many boost potions, handling that instead of secondaries.");
                    // too many boost potions, redirect task
                    restockBoostPotions();
                    return;
                }
                amountNeeded -= boostPotionsNeeded;
            }
            restockFromBarrel(barrelPotion, secondaryPotions, amountNeeded);
        } else {
            // handle bank potion
            openBank();
        }
    }

    private void restockBoostPotions() {
        if (statBoostPotion instanceof BarrelPotion barrelPotion) {
            // check here if we have room, deposit absorptions if needed
            int slotsExclBoost = inventorySnapshot.getFreeSlots() + boostPotions.size();
            log(NightmareZone.class, "Free slots: " + inventorySnapshot.getFreeSlots() + " Boost potions: " + boostPotions.size());
            if (slotsExclBoost < statBoostPotionAmount) {
                // no room bank secondaries
                log(NightmareZone.class, "Not enough free slots for boost potions, restocking secondary potions instead.");
                restockSecondaryPotions();
                return;
            }
            restockFromBarrel(barrelPotion, boostPotions, statBoostPotionAmount);
        } else {
            // open bank if potion is not available in barrels
            log(NightmareZone.class, "Boost potion is not available in barrels, opening bank.");
            openBank();
        }
    }

    private boolean hasItemsToEquip() {
        if (shieldItemID != -1) {
            ItemSearchResult shield = inventorySnapshot.getItem(shieldItemID);
            if (shield != null) {
                itemsToEquip.add(shield);
            }
        }
        if (weaponItemID != -1) {
            ItemSearchResult weapon = inventorySnapshot.getItem(weaponItemID);
            if (weapon != null) {
                itemsToEquip.add(weapon);
            }
        }
        return false;
    }

    public int getDoses(Set<ItemSearchResult> potions, Potion type) {
        int totalDoses = 0;
        for (ItemSearchResult potion : potions) {
            totalDoses += type.getDose(potion.getId());
        }
        return totalDoses;
    }

    private boolean allFullDoses(Set<ItemSearchResult> potions, int fullID) {
        for (ItemSearchResult potion : potions) {
            if (potion.getId() != fullID) {
                return false;
            }
        }
        return true;
    }

    private void restockFromBarrel(BarrelPotion potionType, Set<ItemSearchResult> potions, int requiredAmount) {
        log("Restocking " + potionType.getName() + " from barrel. Amount needed: " + requiredAmount);
        RSObject barrel = getObjectManager().getRSObject(rsObject -> {
            String name = rsObject.getName();
            if (name == null) {
                return false;
            }
            return name.equalsIgnoreCase(potionType.getName());
        });
        // if barrel is not found, then walk to nmz area
        if (barrel == null) {
            walkToNMZArea();
            return;
        }
        int potionAmount = potions.size();
        // if too many pots then deposit
        if (potionAmount > requiredAmount) {
            // deposit excess potions
            usePotionOnBarrel(potions, potionType, barrel);
            return;
        }
        DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ENTER_AMOUNT) {
            // if enter amount dialogue is open, handle it
            log(NightmareZone.class, "Handling enter amount dialogue...");
            int targetDoses = requiredAmount * 4;
            int doses = getDoses(potions, potionType);

            int requiredDoses = targetDoses - doses;
            handleBarrelDialogue(requiredDoses, potionType, potions);
            return;
        }
        // bring up dialogue
        interactTakeBarrel(potionType, barrel);
    }

    private void interactTakeBarrel(BarrelPotion potionType, RSObject barrel) {
        if (!barrel.interact("Take")) {
            log(NightmareZone.class, "Failed to interact with barrel: " + barrel.getName());
            return;
        }
        log(NightmareZone.class, "Interacted with barrel successfully...");

        submitTask(() -> {
            WorldPosition myPos = getWorldPosition();
            if (myPos == null) {
                return false;
            }
            if (getLastPositionChangeMillis() > idleTimeout) {
                idleTimeout = random(2000, 4000);
                // if no interface pops up & we're next to the barrel, then assume no doses.
                if (barrel.getTileDistance() <= 1) {
                    log(NightmareZone.class, "No dialogue, assuming there is no doses stored.");
                    barrelDoseCache.put(potionType, 0);
                }
                return true;
            }
            return getWidgetManager().getDialogue().getDialogueType() == DialogueType.ENTER_AMOUNT;
        }, 8000);
    }

    private void handleBarrelDialogue(int dosesToWithdraw, BarrelPotion potionType, Set<ItemSearchResult> potions) {
        if (getWidgetManager().getDialogue().getDialogueType() != DialogueType.ENTER_AMOUNT) {
            throw new RuntimeException("Incorrect dialogue type");
        }
        BarrelPotionInfo info = getDialogueInfo();
        if (info == null) {
            log(NightmareZone.class, "Problem reading barrel dialogue info.");
            return;
        }
        Potion barrelPotionType = info.getPotionType();
        if (barrelPotionType != potionType) {
            // if incorrect barrel for some reason
            log(NightmareZone.class, "Incorrect barrel type, expected: " + potionType.getName() + ", got: " + barrelPotionType.getName());
            return;
        }
        log(NightmareZone.class, "Withdrawing " + dosesToWithdraw + " doses of " + potionType.getName() + ".");
        int prevDoses = getDoses(potions, potionType);
        // update dose cache
        barrelDoseCache.put(potionType, info.getDoses());
        // type amount
        log(NightmareZone.class, "Typing amount: " + dosesToWithdraw);
        getKeyboard().type(String.valueOf(dosesToWithdraw));
        // little sleep to ensure client registers input fully
        submitTask(() -> false, 200);
        // press enter key
        getKeyboard().pressKey(TouchType.DOWN_AND_UP, PhysicalKey.ENTER);
        // wait for dose amount to change
        boolean result = submitTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                log(NightmareZone.class, "Inventory snapshot is null.");
                return false;
            }
            Set<ItemSearchResult> potions_ = inventorySnapshot.getAllOfItems(potionType.getItemIDs());
            return prevDoses < getDoses(potions_, potionType);
        }, 3000);

        if (result) {
            log(NightmareZone.class, "Withdrawn doses successfully.");
            int cachedAmount = barrelDoseCache.get(potionType);
            int newAmount = Math.max(cachedAmount - dosesToWithdraw, 0);
            barrelDoseCache.put(potionType, newAmount);
        } else {
            log(NightmareZone.class, "Failed withdrawing doses");
        }
    }

    private BarrelPotionInfo getDialogueInfo() {
        Rectangle dialogueBounds = getWidgetManager().getDialogue().getBounds();
        if (dialogueBounds == null) {
            return null;
        }
        Rectangle[] bounds = getUtils().getTextBounds(dialogueBounds.getPadding(10), 16, BLACK_FONT_PIXEL);
        if (bounds.length == 0) {
            return null;
        }
        String text = getOCR().getText(Font.STANDARD_FONT_BOLD, bounds[0], BLACK_FONT_PIXEL);
        log(NightmareZone.class, "Dialogue text: " + text);
        Potion potionType = BarrelPotion.getPotionFromDialogueText(text);
        Integer doses = Utility.extractNumberFromBrackets(text);
        if (doses == null) {
            return null;
        }
        return new BarrelPotionInfo(potionType, doses);
    }

    private void usePotionOnBarrel(Set<ItemSearchResult> potions, BarrelPotion potionType, RSObject barrel) {
        log(NightmareZone.class, "Depositing potions into barrel: " + barrel.getName());
        if (!barrel.isInteractableOnScreen()) {
            // walk closer to the barrel
            log(NightmareZone.class, "Barrel is not on screen, lets walk to it.");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(barrel::isInteractableOnScreen);
            getWalker().walkTo(barrel, builder.build());
            return;
        }
        // use potion
        ArrayList<ItemSearchResult> potionList = new ArrayList<>(potions);
        ItemSearchResult potion = potionList.get(random(potionList.size()));
        if (!potion.interact("Use")) {
            return;
        }

        Polygon polygon = barrel.getConvexHull();
        if (polygon == null) {
            return;
        }
        ItemDefinition itemDefinition = getItemManager().getItemDefinition(potion.getId());

        if (itemDefinition.name == null) {
            log(NightmareZone.class, "Item definition is null for item: " + potion.getId());
            return;
        }
        if (getFinger().tapGameScreen(polygon, "Use " + itemDefinition.name + " -> " + barrel.getName())) {

            // wait until potion amount decrements
            submitTask(() -> {
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    log(NightmareZone.class, "Inventory snapshot is null.");
                    return false;
                }
                return inventorySnapshot.getAmount(potionType.getItemIDs()) < this.boostPotions.size();
            }, 5000);
        }
    }

    private void walkToNMZArea() {
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> {
            // break out when we're in the area
            WorldPosition pos = getWorldPosition();
            if (pos == null) {
                return false;
            }
            return NMZ_AREA.contains(pos);
        });
        getWalker().walkTo(NMZ_AREA.getRandomPosition(), builder.build());
    }

    private void openBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        Predicate<RSObject> bankQuery = gameObject -> {
            // if object has no name
            if (gameObject.getName() == null) {
                return false;
            }
            // has no interact options (eg. bank, open etc.)
            if (gameObject.getActions() == null) {
                return false;
            }

            if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
                return false;
            }

            // if no actions contain bank or open
            if (Arrays.stream(gameObject.getActions()).noneMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
                return false;
            }
            // final check is if the object is reachable
            return gameObject.canReach();
        };
        List<RSObject> banksFound = getObjectManager().getObjects(bankQuery);
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact(BANK_ACTIONS)) return;
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

            return getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000);
    }

    private void openChest() {
        RSObject rewardsChest = getObjectManager().getClosestObject("Rewards chest");
        if (rewardsChest == null) {
            log(NightmareZone.class, "Walking to Rewards chest...");
            getWalker().walkTo(REWARDS_CHEST_INTERACT_TILE, new WalkConfig.Builder().breakDistance(5).build());
            return;
        }
        if (!rewardsChest.interact("Search")) {
            // failed to interact, just poll again from the top
            log(NightmareZone.class, "Failed to interact with Rewards chest.");
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

            return chestInterface.isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000);
    }

    private void suicide() {
        log(NightmareZone.class, "Suiciding...");
        // sleep until we're dead
        submitTask(() -> {
            WorldPosition worldPosition_ = getWorldPosition();
            return worldPosition_ != null && !isArena(worldPosition_);
        }, (int) TimeUnit.MINUTES.toMillis(6), false, true);
    }

    @Override
    public boolean canBreak() {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition != null) {
            return worldPosition.getRegionID() != ARENA_REGION;
        }
        return false;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{10288, 9033};
    }

    @Override
    public boolean canHopWorlds() {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition != null) {
            return worldPosition.getRegionID() != ARENA_REGION;
        }
        return false;
    }

    @Override
    public boolean promptBankTabDialogue() {
        boolean isPrimary = statBoostPotion instanceof StandardPotion;
        boolean isSecondary = secondaryPotion instanceof StandardPotion;
        return isPrimary || isSecondary;
    }

    enum Task {
        HANDLE_BANK,
        OPEN_BANK,
        RESTOCK_BOOST_POTIONS,
        RESTOCK_SECONDARY_POTIONS,
        UNLOCK_COFFER,
        ENTER_DREAM,
        SETUP_DREAM,
        HANDLE_CHEST,


        // ARENA
        SUICIDE,
        LEAVE_ARENA,
        DRINK_ABSORPTION,
        DRINK_POTIONS,
        DRINK_PRAYER_POTION,
        FLICK_PRAYER,
        LOWER_HP,
        EQUIP_ITEMS,
        WALK_TO_AFK_POS,
        ACTIVATE_PRAY,
        USE_SPECIAL_ATTACK
    }

    public static class BarrelPotionInfo {
        private final Potion potionType;
        private final int doses;

        public BarrelPotionInfo(Potion potionType, int doses) {
            this.potionType = potionType;
            this.doses = doses;
        }

        public Potion getPotionType() {
            return potionType;
        }

        public int getDoses() {
            return doses;
        }
    }

    public static class DialogueOption {
        final String title;
        final String option;

        public DialogueOption(String title, String option) {
            this.title = title;
            this.option = option;
        }
    }
}
