package com.osmb.script;

import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.PhysicalKey;
import com.osmb.api.input.TouchType;
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
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.component.ChestInterface;
import com.osmb.script.component.PotionInterface;
import com.osmb.script.javafx.UI;
import com.osmb.script.overlay.CofferOverlay;
import com.osmb.script.overlay.PointsOverlay;
import javafx.scene.Scene;
import javafx.util.Pair;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(name = "Nightmare zone", description = "", author = "Joe", skillCategory = SkillCategory.COMBAT, version = 1.0)
public class NightmareZone extends Script {

    public static final String[] BANK_NAMES = {"Bank booth"};
    public static final String[] BANK_ACTIONS = {"bank"};
    public static final int BLACK_FONT_PIXEL = -16777215;
    public static final int MIN_COFFER_VALUE = 26000;
    public static final int ARENA_REGION = 9033;
    public static final int[] ABSORPTION_IDS = new int[]{ItemID.ABSORPTION_4, ItemID.ABSORPTION_3, ItemID.ABSORPTION_2, ItemID.ABSORPTION_1};

    // World Positions
    public static final WorldPosition REWARDS_CHEST_INTERACT_TILE = new WorldPosition(2609, 3118, 0);
    public static final WorldPosition POTION_TILE = new WorldPosition(2605, 3117, 0);
    public static final WorldPosition DOMINIC_POSITION = new WorldPosition(2608, 3116, 0);

    // Area Definitions
    private static final RectangleArea BANK_AREA = new RectangleArea(2609, 3088, 4, 9, 0);
    private static final RectangleArea NMZ_AREA = new RectangleArea(2601, 3113, 5, 5, 0);

    // Dialogue Strings
    private static final List<String> DOMINIC_CHAT_DIALOGUES = Arrays.asList(
            "You haven't started that dream I created for you. Can I help you with something?",
            "Welcome to the Nightmare Zone! Would you like me to create a dream for you?"
    );
    private static final List<String> DOMINIC_SUCCESS_CHAT_DIALOGUES = Arrays.asList(
            "I've already created a dream for you. Do you want me to cancel it?",
            "I've prepard your dream. Step ito te ecosure,"
    );
    private static final DialogueOption[] DOMINIC_DIALOGUE_OPTIONS = new DialogueOption[]{
            new DialogueOption("which dream would you like to experience?", "Previous:"),
            new DialogueOption("Agree to pay", "Yes")
    };
    // Misc
    private static final java.awt.Font ARIEL = java.awt.Font.getFont("Ariel");
    // Collections
    private final List<ItemSearchResult> itemsToEquip = new ArrayList<>();
    private final Map<Potion, Integer> barrelDoseCache = Collections.synchronizedMap(new HashMap<>());
    private final List<Task> dynamicTasks = new ArrayList<>();
    // Timers
    private final Stopwatch arenaDelayTimer = new Stopwatch();
    private final Stopwatch statBoostPotionDrinkTimer = new Stopwatch();
    private final Stopwatch rapidHealFlickTimer = new Stopwatch();
    private Stopwatch prayerDelayTimer;
    private Stopwatch lowerHPDelayTimer;
    // Configuration Settings
    private SpecialAttackWeapon specialAttackWeapon;
    private Potion statBoostPotion;
    private Potion secondaryPotion;
    private int statBoostPotionAmount;
    private int nextSecondaryDrink;
    private int idleTimeout;
    private LowerHealthMethod lowerHealthMethod;
    private boolean flickRapidHeal = false;
    private AFKPosition afkPosition;
    private boolean noBoostSuicide;
    private int shieldItemID;
    private int weaponItemID;
    // State Trackers
    private boolean setupDream = false;
    private boolean outOfPointsFlag = false;
    private boolean checkedChest = false;
    private Task previousTask = null;
    private Task task;
    // UI References
    private PointsOverlay pointsOverlay;
    private ChestInterface chestInterface;
    private PotionInterface potionInterface;
    private CofferOverlay cofferOverlay;
    // Current State
    private UIResultList<ItemSearchResult> boostPotions;
    private Optional<Integer> freeSlots;
    private UIResultList<ItemSearchResult> secondaryPotions;
    private Integer cachedRewardPoints = null;
    private Stopwatch overloadDrinkWindow = new Stopwatch();
    private boolean inArena;
    private Stopwatch specialDelayTimer;

    public NightmareZone(Object scriptCore) {
        super(scriptCore);
    }

    public static boolean isArena(WorldPosition worldPosition) {
        return worldPosition.getRegionID() == ARENA_REGION;
    }

    @Override
    public void onPaint(Canvas c) {
        c.fillRect(5, 40, 300, 200, Color.BLACK.getRGB(), 0.7);
        c.drawRect(5, 40, 300, 200, Color.BLACK.getRGB());
        int y = 60;
        c.drawText("Task: " + (task == null ? "None" : task), 10, y, Color.WHITE.getRGB(), ARIEL);
        y += 20;
        if (inArena) {
            if (lowerHPDelayTimer != null) {
                long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(lowerHPDelayTimer.timeLeft());
                c.drawText("Lower HP delay: " + Math.max(secondsLeft, 0), 10, y, Color.WHITE.getRGB(), ARIEL);
                y += 20;
            }
            if (statBoostPotion != null) {
                long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(statBoostPotionDrinkTimer.timeLeft());
                c.drawText("Next " + statBoostPotion + " drink: " + Math.max(secondsLeft, 0), 10, y, Color.WHITE.getRGB(), ARIEL);
                y += 20;
            }
            if (secondaryPotion != null) {
                String end = secondaryPotion == Potion.PRAYER_POTION ? "%" : "points";
                c.drawText("Next " + secondaryPotion + " drink @ " + nextSecondaryDrink + " " + end, 10, y, Color.WHITE.getRGB(), ARIEL);
                y += 20;
            }
        } else {
            c.drawText("Setup dream: " + setupDream, 10, y, Color.WHITE.getRGB(), ARIEL);
            y += 20;
            c.drawText("Reward points: " + (cachedRewardPoints == null ? "null" : cachedRewardPoints), 10, y, Color.WHITE.getRGB(), ARIEL);
            y += 20;
        }
        c.drawText("Dynamic tasks: ", 10, y, Color.WHITE.getRGB(), ARIEL);
        for (Task task : dynamicTasks) {
            y += 20;
            c.drawText(" - Task: " + task, 10, y, Color.WHITE.getRGB(), ARIEL);
        }
        for (Map.Entry<Potion, Integer> entry : barrelDoseCache.entrySet()) {
            y += 20;
            Potion potion = entry.getKey();
            Integer value = entry.getValue();
            c.drawText(potion.getName() + ": " + value, 10, y, Color.WHITE.getRGB(), ARIEL);
        }
    }

    @Override
    public void onStart() {
        UI ui = new UI(this);
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);
        this.lowerHealthMethod = ui.getLowerHPMethod();

        this.statBoostPotion = ui.getPrimaryPotion();
        // amount of boost potions to bring
        this.statBoostPotionAmount = 4;

        this.secondaryPotion = ui.getSecondaryPotion();

        this.afkPosition = ui.getAFKPosition();
        if (this.afkPosition == AFKPosition.RANDOM) {
            List<AFKPosition> positions = new ArrayList<>(EnumSet.allOf(AFKPosition.class));
            positions.remove(AFKPosition.RANDOM); // Remove RANDOM from possible choices
            this.afkPosition = positions.get(Utils.random(positions.size()));
        }
        // suicides when out of boost potions to be more effecient xp wise
        this.noBoostSuicide = true;
        // flicks rapid heal to keep 1hp (only if using absorption potions as secondary)
        this.flickRapidHeal = ui.flickRapidHeal();

        this.idleTimeout = random(2000, 4000);

        this.nextSecondaryDrink = secondaryPotion == Potion.PRAYER_POTION ? random(10, 60) : random(50, 200);

        cofferOverlay = new CofferOverlay(this);
        chestInterface = new ChestInterface(this);
        potionInterface = new PotionInterface(this);
        pointsOverlay = new PointsOverlay(this);
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

    private boolean isBarrelPotion(Potion potion) {
        if (potion == null) {
            return false;
        }
        return potion.getCostPerDose() != -1;
    }

    private Task decideTask(boolean walking) {
        WorldPosition worldPosition = getWorldPosition();

        // only check for bank if inside the bank area, this is because searching for center components takes 30ms or so.
        if (BANK_AREA.contains(worldPosition)) {
            if (getWidgetManager().getBank().isVisible()) {
                return Task.HANDLE_BANK;
            }
        }
        secondaryPotions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), secondaryPotion.getItemIDs());
        if (statBoostPotion != null)
            boostPotions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), statBoostPotion.getItemIDs());
        else {
            // if not selected just keep the list empty to avoid nullptr
            boostPotions = UIResultList.of(new ArrayList<>());
        }
        freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());

        if (worldPosition.equals(REWARDS_CHEST_INTERACT_TILE)) {
            if (chestInterface.isVisible()) {
                return Task.HANDLE_CHEST;
            }
        }

        if (freeSlots.isEmpty() || secondaryPotions.isNotVisible() || boostPotions.isNotVisible()) {
            log(NightmareZone.class, "Inventory not visible...");
            return null;
        }
        dynamicTasks.clear();


        inArena = isArena(worldPosition);
        if (inArena) {
            return getArenaTask(walking, worldPosition);
        } else {
            return getTask();
        }
    }

    private Task getTask() {
        // make sure boost potion are all full doses
        if ((isBarrelPotion(statBoostPotion) || isBarrelPotion(secondaryPotion)) && !checkedChest) {
            dynamicTasks.add(Task.HANDLE_CHEST);
        }
        // if amount doesn't match amount selected
        if (statBoostPotion != null && (boostPotions.size() != statBoostPotionAmount || !allFullDoses(boostPotions, statBoostPotion.getFullID()))) {
            log("Need boost potions");
            if (!isBarrelPotion(statBoostPotion)) {
                dynamicTasks.add(Task.OPEN_BANK);
            } else {
                if (boostPotions.size() > statBoostPotionAmount) {
                    return Task.RESTOCK_BOOST_POTIONS;
                }

                // ignore if we don't have enough to make a full inv & can buy more from the shop. (only if we know the dose stock & points)
                if (shouldInteractWithBarrel(statBoostPotion, boostPotions, statBoostPotionAmount)) {
                    dynamicTasks.add(Task.RESTOCK_BOOST_POTIONS);
                }
            }
        }

        // if we have free slots remaining, or if the absorptions aren't all (4) dose
        if (freeSlots.get() > 0 || !allFullDoses(secondaryPotions, secondaryPotion.getFullID())) {
            if (!isBarrelPotion(secondaryPotion)) {
                dynamicTasks.add(Task.OPEN_BANK);
            } else {
                // check ignore flags (used for when we don't have enough points to fully stock etc.)
                int requiredAmount = freeSlots.get() + secondaryPotions.size();
                if (statBoostPotion != null) {
                    requiredAmount += boostPotions.size();
                    // at this point required amount equals the free slots, excluding boost pots + secondary.
                    // we can now take the desired amount of stat pots needed away from this & we'll have the amount of secondaries needed.
                    requiredAmount -= statBoostPotionAmount;
                }

                if (shouldInteractWithBarrel(secondaryPotion, secondaryPotions, requiredAmount)) {
                    dynamicTasks.add(Task.RESTOCK_SECONDARY_POTIONS);
                }
            }
        }

        if (cofferOverlay.isVisible()) {
            UIResult<Integer> cofferValue = cofferOverlay.getCofferValue();
            if (cofferValue.isFound()) {
                if (cofferValue.get() <= MIN_COFFER_VALUE) {
                    log(NightmareZone.class, "Not enough coins in the coffer, stopping script!");
                    this.stop();
                    return null;
                }
            } else {
                log(NightmareZone.class, "Can't read coffer value, it might be locked...");
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

        // equip items if necessary
        if (hasItemsToEquip()) {
            return Task.EQUIP_ITEMS;
        }

        if (isDueToBreak()) {
            log(NightmareZone.class, "Due to break, suiciding...");
            return Task.SUICIDE;
        }
        if (statBoostPotion != null && noBoostSuicide) {
            int statBoostPotionDoses = getDoses(boostPotions, statBoostPotion);
            if (statBoostPotionDoses == 0) {
                // suicide and restock
                return Task.SUICIDE;
            }
        }

        if (!walking && !afkPosition.getArea().contains(worldPosition)) {
            return Task.WALK_TO_AFK_POS;
        }

        // drink absorptions
        if (secondaryPotion == Potion.ABSORPTION_POTION) {
            if (pointsOverlay.isVisible()) {
                Integer points = (Integer) pointsOverlay.getValue(PointsOverlay.POINTS);
                if (points != null && points <= nextSecondaryDrink) {
                    dynamicTasks.add(Task.DRINK_ABSORPTION);
                }
            } else {
                dynamicTasks.add(Task.DRINK_ABSORPTION);
            }
        } else if (secondaryPotion == Potion.PRAYER_POTION) {
            // prayer potion
            UIResult<Integer> prayerPoints = getWidgetManager().getMinimapOrbs().getPrayerPointsPercentage();
            UIResult<Boolean> quickPrayersActive = getWidgetManager().getMinimapOrbs().isQuickPrayersActivated();
            if (prayerPoints.isNotVisible() || quickPrayersActive.isNotVisible()) {
                log(NightmareZone.class, "Prayer points orb not visible, make sure regeneration indicators are disabled...");
                return null;
            }
            if (prayerPoints.isFound()) {
                log(NightmareZone.class, "Prayer points: " + prayerPoints + "% | Next drinking at: " + nextSecondaryDrink);
                if (prayerPoints.get() <= nextSecondaryDrink) {
                    dynamicTasks.add(Task.DRINK_PRAYER_POTION);
                }
            }
            if (quickPrayersActive.isFound()) {
                if (!quickPrayersActive.get()) {
                    if (prayerDelayTimer == null) {
                        prayerDelayTimer = new Stopwatch(random(1000, 20000));
                    } else if (prayerDelayTimer.hasFinished()) {
                        dynamicTasks.add(Task.ACTIVATE_PRAY);
                    }
                }
            }

        }

        // lower health if needs be
        UIResult<Integer> hitpoints = getWidgetManager().getMinimapOrbs().getHitpoints();
        if (hitpoints.isNotVisible()) {
            log(NightmareZone.class, "Hitpoints orb not visible, make sure regeneration indicators are disabled...");
            return null;
        }
        if (hitpoints.isNotFound()) {
            log(NightmareZone.class, "Hitpoints value not found...");
            return null;
        }
        // drink stat boost pot
        boolean canDrink = statBoostPotion != Potion.OVERLOAD || hitpoints.get() > 51;
        if (statBoostPotion != null && canDrink && statBoostPotionDrinkTimer.hasFinished()) {
            dynamicTasks.add(Task.DRINK_POTIONS);
        }


        if (flickRapidHeal && rapidHealFlickTimer.hasFinished()) {
            dynamicTasks.add(Task.FLICK_PRAYER);
        }


        if (secondaryPotion == Potion.ABSORPTION_POTION && hitpoints.isFound() && hitpoints.get() > 1) {
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
            if (previousTask != null && dynamicTasks.contains(previousTask)) {
                // if we still need to do the previous task
                return previousTask;
            } else {
                // update the previous task
                previousTask = dynamicTasks.get(random(dynamicTasks.size()));
                return previousTask;
            }
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
                    specialDelayTimer.reset(random(2000, 30000));
                } else if (specialDelayTimer.hasFinished()) {
                    return Task.USE_SPECIAL_ATTACK;
                }

            }
        }
        return null;
    }

    private boolean shouldInteractWithBarrel(Potion potionType, UIResultList<ItemSearchResult> potions, int requiredAmount) {
        // Early exit if not a barrel potion or not in cache
        if (!isBarrelPotion(potionType) || !barrelDoseCache.containsKey(potionType)) {
            return false;
        }

        int storedDoses = barrelDoseCache.get(potionType);
        // Don't interact if no doses available
        if (storedDoses <= 0) {
            return false;
        }

        int doses = getDoses(potions, potionType);
        int targetDoses = requiredAmount * 4;

        // If we have enough doses already, don't interact
        if (doses >= targetDoses) {
            return false;
        }

        // don't interact if we can afford to buy more as that will be a valid task. If we don't add this then we will potentially restock, buy more, then restock again
        if (cachedRewardPoints != null && cachedRewardPoints >= potionType.getCostPerDose() && storedDoses < targetDoses) {
            return false;
        }

        return true;
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
        ItemSearchResult potion = boostPotions.getRandom();
        String itemName = getItemManager().getItemName(potion.getId());
        int initialDoseAmount = getDoses(boostPotions, statBoostPotion);
        if (itemName != null)
            log(NightmareZone.class, "Drinking " + itemName + "...");
        if (!potion.interact()) {
            return;
        }
        // wait for potion to be non transparent again before searching
        submitTask(() -> false, 800);
        // search for items and calculate dose amount, then compare to the previous dose amount to confirm we drank the potion.
        boolean dosesDecremented = submitTask(() -> {
            boostPotions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), statBoostPotion.getItemIDs());
            if (boostPotions.isNotVisible()) {
                return false;
            }
            return getDoses(boostPotions, statBoostPotion) < initialDoseAmount;
        }, 3000);


        if (dosesDecremented) {
            resetBoostPotionDrinkTimer();
        }

    }

    private void resetBoostPotionDrinkTimer() {
        if (statBoostPotion == Potion.OVERLOAD) {
            // overload lasts 5 minutes exactly.
            long base = TimeUnit.MINUTES.toMillis(5);
            long extra = TimeUnit.SECONDS.toMillis(15);
            statBoostPotionDrinkTimer.reset(base + extra);
            overloadDrinkWindow.reset(random(TimeUnit.MINUTES.toMillis(3) + TimeUnit.SECONDS.toMillis(20), TimeUnit.MINUTES.toMillis(4) + TimeUnit.SECONDS.toMillis(30)));
            lowerHPDelayTimer = new Stopwatch(random(15000, 35000));
        } else {
            long min = TimeUnit.MINUTES.toMillis(3);
            long max = TimeUnit.HOURS.toMillis(5);
            statBoostPotionDrinkTimer.reset(random(min, max));
        }
    }

    private boolean canEatDownHP() {
        if (overloadDrinkWindow != null && statBoostPotion == Potion.OVERLOAD) {
            return !overloadDrinkWindow.hasFinished();
        }
        return true;
    }

    private void flickPrayer() {
        UIResult<Boolean> prayersActivated = getWidgetManager().getMinimapOrbs().isQuickPrayersActivated();
        if (prayersActivated.isNotVisible()) {
            log(NightmareZone.class, "Prayer orb not visible, make sure regeneration indicators are disabled...");
            return;
        }

        if (prayersActivated.isFound()) {
            if (getWidgetManager().getMinimapOrbs().setQuickPrayers(false)) {
                rapidHealFlickTimer.reset(random(20000, 50000));
            }
        } else if (getWidgetManager().getMinimapOrbs().setQuickPrayers(true)) {
            if (getWidgetManager().getMinimapOrbs().setQuickPrayers(false)) {
                rapidHealFlickTimer.reset(random(20000, 50000));
            }
        }
    }

    private void drinkPrayerPotion() {
        log(NightmareZone.class, "Drinking prayer potion...");
        if (secondaryPotions.size() <= 0) {
            return;
        }
        ItemSearchResult potion = secondaryPotions.getRandom();
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
            secondaryPotions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), secondaryPotion.getItemIDs());
            if (secondaryPotions.isNotVisible()) {
                return false;
            }
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
            Integer absorptionPoints = (Integer) pointsOverlay.getValue(PointsOverlay.POINTS);
            if (absorptionPoints == null) {
                absorptionPoints = 0;
            }
            if (absorptionPoints > nextSecondaryDrink) {
                return true;
            }
            UIResultList<ItemSearchResult> absorptions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), secondaryPotion.getItemIDs());
            ItemSearchResult absorption = absorptions.getRandom();
            getFinger().tap(false, absorption);
            submitTask(() -> false, RandomUtils.weightedRandom(150, 1200));
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
        UIResult<ItemSearchResult> lowerHealthItem = getItemManager().findItem(getWidgetManager().getInventory(), lowerHealthMethod.getItemID());
        if (lowerHealthItem.isNotVisible()) {
            return;
        }
        if (lowerHealthItem.isNotFound()) {
            log(NightmareZone.class, "Lower health item not found...");
            return;
        }

        submitTask(() -> {
            UIResult<Integer> healthResult = getWidgetManager().getMinimapOrbs().getHitpoints();
            if (healthResult.isNotVisible()) {
                log(NightmareZone.class, "Hitpoints orb not visible, make sure regeneration indicators are disabled...");
                return false;
            }
            if (healthResult.isFound()) {
                if (healthResult.get() <= 1) {
                    lowerHPDelayTimer = null;
                    return true;
                }
                getFinger().tap(false, lowerHealthItem.get());
                submitTask(() -> false, RandomUtils.weightedRandom(150, 600));
            }
            return false;
        }, timeout);
    }

    private void enterDream() {
        if (!potionInterface.isVisible()) {
            log(NightmareZone.class, "Interacting with potion...");
            interactWithPotion();
            return;
        }
        if (!potionInterface.accept()) {
            return;
        }
        // wait until inside arena
        submitHumanTask(() -> {
            WorldPosition worldPosition = getWorldPosition();
            if (worldPosition == null) {
                return false;
            }
            if (isArena(worldPosition)) {
                log(NightmareZone.class, "Executing delay...");
                submitTask(() -> {
                    // highlight screen blue to show we're executing this delay
                    Canvas canvas = getScreen().getDrawableCanvas();
                    canvas.fillRect(getScreen().getBounds(), Color.BLUE.getRGB(), 0.5);
                    return false;
                }, Utils.random(500, 7000));
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
        boolean isDialogue = !isDominicDialogue(dialogueType);
        if (setupDream) {
            return;
        }
        if (isDialogue) {
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
                if (DOMINIC_SUCCESS_CHAT_DIALOGUES.contains(text)) {
                    setupDream = true;
                    return true;
                }
                return DOMINIC_CHAT_DIALOGUES.contains(text.toLowerCase()) || text.toLowerCase().startsWith("for a customisable rumble dream,");
            }
            case TEXT_OPTION -> {
                UIResult<String> title = getWidgetManager().getDialogue().getDialogueTitle();
                if (title.isNotFound()) {
                    log(NightmareZone.class, "Title is null");
                    return false;
                }
                log(NightmareZone.class, "Text option dialogue title: " + title);
                for (DialogueOption domOption : DOMINIC_DIALOGUE_OPTIONS) {
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
                for (DialogueOption domOption : DOMINIC_DIALOGUE_OPTIONS) {
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
        RSTile dominicTile = getSceneManager().getTile(DOMINIC_POSITION);
        if (dominicTile == null) {
            log(NightmareZone.class, "Walking to Dominic...");
            getWalker().walkTo(DOMINIC_POSITION, new WalkConfig.Builder().breakDistance(10).build());
            return;
        }

        if (!dominicTile.isOnGameScreen()) {
            // walk to tile
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> {
                if (getWorldPosition() == null) {
                    return false;
                }
                RSTile dominicTile_ = getSceneManager().getTile(DOMINIC_POSITION);
                if (dominicTile_ == null) {
                    return false;
                }
                return dominicTile_.isOnGameScreen();
            });
            getWalker().walkTo(DOMINIC_POSITION, builder.build());
        }
        Polygon polygon = dominicTile.getTilePoly();
        if (polygon == null) {
            return;
        }
        if (getFinger().tap(polygon.getResized(0.6), "Dream")) {
            submitTask(() -> getWidgetManager().getDialogue().getDialogueType() != null, random(4000, 6000));
        }
    }


    private void interactWithPotion() {
        RSTile potionTile = getSceneManager().getTile(POTION_TILE);
        if (potionTile == null) {
            // shouldn't happen
            log(NightmareZone.class, "Dominic tile is null...");
            return;
        }

        if (!potionTile.isOnGameScreen()) {
            // walk to tile
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> {
                if (getWorldPosition() == null) {
                    return false;
                }
                RSTile potionTile_ = getSceneManager().getTile(POTION_TILE);
                if (potionTile_ == null) {
                    return false;
                }
                return potionTile_.isOnGameScreen();
            });
            getWalker().walkTo(POTION_TILE, builder.build());
        }
        Polygon polygon = potionTile.getTilePoly();
        if (polygon == null) {
            return;
        }
//        MenuHook hook = menuEntries -> {
//            return null;
//        };
        // fix
        if (!getFinger().tap(polygon.getResized(0.5), menuEntries -> {
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
        submitTask(() -> {
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
        for (Potion p : Potion.values()) {
            if (!isBarrelPotion(p)) {
                continue;
            }
            UIResult<Integer> storedDoses = chestInterface.getStoredDoses(p.getItemIDs()[3]);
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


        Map<ItemSearchResult, Integer> itemsToBuy = new HashMap<>();

        boolean isBoostPotion = statBoostPotion != null && isBarrelPotion(statBoostPotion);
        boolean isSecondaryPotion = secondaryPotion != null && isBarrelPotion(secondaryPotion);
        int boostDosesToBuy = 0;
        int secondaryDosesToBuy = 0;

        if (isBoostPotion) {
            UIResult<Integer> storedBoostDoses = chestInterface.getStoredDoses(statBoostPotion.getItemIDs()[3]);
            if (storedBoostDoses.isNotFound()) {
                return;
            }
            int boostDoses = storedBoostDoses.get();
            barrelDoseCache.put(statBoostPotion, boostDoses);
            int targetDoses = statBoostPotionAmount * 4;
            int doses = getDoses(boostPotions, statBoostPotion);

            int requiredDoses = targetDoses - doses;

            requiredDoses -= boostDoses;

            if (requiredDoses > 0) {
                boostDosesToBuy += requiredDoses;
            }
        }
        if (isSecondaryPotion) {
            UIResult<Integer> storedSecondaryDoses = chestInterface.getStoredDoses(secondaryPotion.getItemIDs()[3]);
            if (storedSecondaryDoses.isNotFound()) {
                return;
            }
            int secondaryDoses = storedSecondaryDoses.get();
            barrelDoseCache.put(secondaryPotion, secondaryDoses);

            int boostPots = isBoostPotion ? Math.max(boostPotions.size(), statBoostPotionAmount) : 0;
            List<Integer> itemsToIgnoreList = new ArrayList<>();
            if (isBoostPotion) {
                for (int boostId : statBoostPotion.getItemIDs()) {
                    itemsToIgnoreList.add(boostId);
                }
            }
            for (int secondaryId : secondaryPotion.getItemIDs()) {
                itemsToIgnoreList.add(secondaryId);
            }

            int[] itemsToIgnore = new int[itemsToIgnoreList.size()];
            for (int i = 0; i < itemsToIgnore.length; i++) {
                itemsToIgnore[i] = itemsToIgnoreList.get(i);
            }
            Optional<Integer> freeSlotsExcludingBoostPotions = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory(), itemsToIgnore);
            if (!freeSlotsExcludingBoostPotions.isPresent()) {
                // shouldn't happen
                return;
            }

            int amountRequired = freeSlotsExcludingBoostPotions.get();
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
        int primaryCost = !isBoostPotion ? 0 : boostDosesToBuy * statBoostPotion.getCostPerDose();
        int secondaryCost = secondaryDosesToBuy * secondaryPotion.getCostPerDose();
        int totalCost = primaryCost + secondaryCost;

        boolean prioritiseSecondary = secondaryDosesToBuy > 0 && totalCost > points.get();

        List<PotionBuyEntry> potionBuyEntries = new ArrayList<>();


        if (secondaryDosesToBuy > 0) {
            int affordableDoses = points.get() / secondaryPotion.getCostPerDose();
            potionBuyEntries.add(new PotionBuyEntry(secondaryPotion.getItemIDs()[3], balling ? 255 : affordableDoses, barrelDoseCache.getOrDefault(secondaryPotion, 0)));
        }

        if (isBoostPotion && !prioritiseSecondary && boostDosesToBuy > 0) {
            int affordableDoses = points.get() / statBoostPotion.getCostPerDose();
            potionBuyEntries.add(new PotionBuyEntry(statBoostPotion.getItemIDs()[3], balling ? 255 : affordableDoses, barrelDoseCache.getOrDefault(statBoostPotion, 0)));
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
            checkedChest = true;
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
        UIResult<ItemSearchResult> itemSearchResult = getItemManager().findItem(chestInterface, itemID);
        if (itemSearchResult.isNotFound()) {
            log(NightmareZone.class, "No item found.");
            return;
        }
        if (!itemSearchResult.get().interact("buy-x")) {
            log(NightmareZone.class, "Failed interacting with item.");
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
            return;
        }

    }

    private int[] getItemsToIgnore() {
        List<Integer> itemsToIgnore = new ArrayList<>();
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
            if (!isBarrelPotion(statBoostPotion)) {
                // if the potion is from the bank, then only keep full doses
                itemsToIgnore.add(statBoostPotion.getFullID());
            } else {
                for (int potionID : statBoostPotion.getItemIDs()) {
                    itemsToIgnore.add(potionID);
                }
            }
        }
        if (secondaryPotion != null) {
            if (!isBarrelPotion(secondaryPotion)) {
                // if the potion is from the bank, then only keep full doses
                itemsToIgnore.add(secondaryPotion.getFullID());
            } else {
                for (int potionID : secondaryPotion.getItemIDs()) {
                    itemsToIgnore.add(potionID);
                }
            }
        }
        if (lowerHealthMethod != null) {
            itemsToIgnore.add(lowerHealthMethod.getItemID());
        }

        // convert to int array
        int[] itemsToIgnoreArray = new int[itemsToIgnore.size()];
        for (int i = 0; i < itemsToIgnoreArray.length; i++) {
            itemsToIgnoreArray[i] = itemsToIgnore.get(i);
        }
        return itemsToIgnoreArray;
    }

    private void handleBank() {
        // deposit unwanted items
        int[] itemsToIgnore = getItemsToIgnore();
        if (!getWidgetManager().getBank().depositAll(itemsToIgnore)) {
            // if we fail to deposit all
            return;
        }
        // at this point all unwanted items are gone.
        boolean handleStatBoostPotion = statBoostPotion != null && !isBarrelPotion(statBoostPotion);
        boolean handleSecondaryPotion = !isBarrelPotion(secondaryPotion);

        // find secondary potions IF ENABLED
        if (secondaryPotion != null) {
            secondaryPotions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), secondaryPotion.getItemIDs());
        }

        // find boost potions IF ENABLED
        if (handleStatBoostPotion) {
            boostPotions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), statBoostPotion.getItemIDs());
        }

        freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory());
        Map<Integer, Integer> itemsToWithdraw = new HashMap<>();

        if (handleStatBoostPotion) {
            boostPotions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), statBoostPotion.getFullID());
            int amountToWithdraw = statBoostPotionAmount - boostPotions.size();
            log(NightmareZone.class, "Amount needed: " + statBoostPotionAmount + " Amount to withdraw: " + amountToWithdraw);
            if (amountToWithdraw != 0)
                itemsToWithdraw.put(statBoostPotion.getFullID(), amountToWithdraw);
        }
        if (handleSecondaryPotion) {
            int boostPots = handleStatBoostPotion ? Math.max(boostPotions.size(), statBoostPotionAmount) : 0;
            int[] potionsToIgnore = new int[0];
            if (handleStatBoostPotion) {
                potionsToIgnore = statBoostPotion.getItemIDs();
            }
            Optional<Integer> freeSlotsExcludingBoostPotions = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory(), potionsToIgnore);
            if (!freeSlotsExcludingBoostPotions.isPresent()) {
                // shouldn't happen
                return;
            }

            int amountToWithdraw = freeSlotsExcludingBoostPotions.get() - boostPots;
            if (amountToWithdraw != 0)
                itemsToWithdraw.put(secondaryPotion.getFullID(), amountToWithdraw);
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
            getWidgetManager().getBank().withdraw(itemId, amount);
        }
    }

    private boolean isBankingComplete(boolean boostPotion, UIResultList<ItemSearchResult> boostPotionsInventory, boolean secondaryPotion, UIResultList<ItemSearchResult> secondaryPotionsInventory) {
        // check if we have withdrawn the expected amount of potions
        boolean result = true;
        if (boostPotion) {
            result = boostPotionsInventory.size() == statBoostPotionAmount;
            if (!result) {
                return false;
            }
        }

        if (secondaryPotion) {
            Optional<Integer> freeSlots = getItemManager().getFreeSlotsInteger(getWidgetManager().getInventory(), this.secondaryPotion.getItemIDs());
            int amountNeeded = freeSlots.get();
            if (amountNeeded != secondaryPotionsInventory.size()) {
                return false;
            }
            return allFullDoses(secondaryPotionsInventory, this.secondaryPotion.getFullID());
        }

        return result;
    }

    private void restockSecondaryPotions() {
        if (!isBarrelPotion(secondaryPotion)) {
            // if prayer potion
            // open bank if potion is not available in barrels
            openBank();
            return;
        }
        // if absorption potion
        // work out amount needed
        int amountNeeded = freeSlots.get() + secondaryPotions.size();
        log(NightmareZone.class, "Free slots: " + amountNeeded);
        if (statBoostPotion != null) {
            int boostPotionsNeeded = statBoostPotionAmount;
            boostPotionsNeeded -= boostPotions.size();

            if (boostPotionsNeeded < 0) {
                log(NightmareZone.class, "Too many boost potions, handling that instead of secondaries.");
                // too many boost potions, redirect task
                restockBoostPotions();
                return;
            }
            int boostPotions = this.boostPotions.size();
            amountNeeded += boostPotions;
            amountNeeded -= boostPotionsNeeded;
        }
        restockFromBarrel(secondaryPotion, secondaryPotions, amountNeeded);
    }

    private void restockBoostPotions() {
        if (!isBarrelPotion(statBoostPotion)) {
            // open bank if potion is not available in barrels
            openBank();
            return;
        }
        // check here if we have room, deposit absorptions if needed

        restockFromBarrel(statBoostPotion, boostPotions, statBoostPotionAmount);
    }

    private boolean hasItemsToEquip() {
        if (shieldItemID != -1) {
            UIResult<ItemSearchResult> result = getItemManager().findItem(getWidgetManager().getInventory(), shieldItemID);
            if (result.isFound()) {
                itemsToEquip.add(result.get());
            }
        }
        if (weaponItemID != -1) {
            UIResult<ItemSearchResult> result = getItemManager().findItem(getWidgetManager().getInventory(), weaponItemID);
            if (result.isFound()) {
                itemsToEquip.add(result.get());
            }
        }
        return false;
    }

    public int getDoses(UIResultList<ItemSearchResult> potions, Potion type) {
        int totalDoses = 0;
        for (ItemSearchResult potion : potions) {
            totalDoses += type.getDose(potion.getId());
        }
        return totalDoses;
    }

    private boolean allFullDoses(UIResultList<ItemSearchResult> potions, int fullID) {
        for (ItemSearchResult potion : potions) {
            if (potion.getId() != fullID) {
                return false;
            }
        }
        return true;
    }

    private void restockFromBarrel(Potion potionType, UIResultList<ItemSearchResult> potions, int requiredAmount) {
        Optional<RSObject> barrelOpt = getObjectManager().getObject(rsObject -> {
            String name = rsObject.getName();
            if (name == null) {
                return false;
            }
            return name.equalsIgnoreCase(potionType.getName());
        });

        // if barrel is not found, then walk to nmz area
        if (!barrelOpt.isPresent()) {
            walkToNMZArea();
            return;
        }

        RSObject barrel = barrelOpt.get();
        int potionAmount = potions.size();
        // if too many pots then deposit
        if (potionAmount > requiredAmount) {
            log(NightmareZone.class, "Potions: " + potionAmount + " Required: " + requiredAmount);
            // use pot on barrel
            usePotionOnBarrel(potions, potionType, barrel);
            return;
        }

        DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ENTER_AMOUNT) {
            log(NightmareZone.class, "Handling enter amount dialogue...");
            int targetDoses = requiredAmount * 4;
            int doses = getDoses(potions, potionType);

            int requiredDoses = targetDoses - doses;
            handleBarrelDialogue(requiredDoses, potionType, potions);
            return;
        }

        // bring up dialogue
        if (barrel.interact("Take")) {
            log(NightmareZone.class, "Interacted with barrel successfully...");
            WorldPosition position = getWorldPosition();
            AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(position);
            Timer positionChangeTimer = new Timer();

            submitTask(() -> {
                WorldPosition myPos = getWorldPosition();
                if (myPos == null) {
                    return false;
                }

                if (previousPosition.get().equals(myPos)) {
                    if (positionChangeTimer.timeElapsed() > idleTimeout) {
                        idleTimeout = random(2000, 4000);

                        // if no interface pops up & we're next to the barrel, then assume no doses.
                        if (barrel.getTileDistance() <= 1) {
                            log(NightmareZone.class, "No dialogue, assuming there is no doses stored.");
                            barrelDoseCache.put(potionType, 0);
                        }
                        return true;
                    }
                } else {
                    previousPosition.set(myPos);
                    positionChangeTimer.reset();
                }
                DialogueType dialogueType_ = getWidgetManager().getDialogue().getDialogueType();
                return dialogueType_ == DialogueType.ENTER_AMOUNT;
            }, 8000);
        }
    }

    private void handleBarrelDialogue(int dosesToWithdraw, Potion potionType, UIResultList<ItemSearchResult> potions) {
        if (getWidgetManager().getDialogue().getDialogueType() != DialogueType.ENTER_AMOUNT) {
            throw new RuntimeException("Incorrect dialogue type");
        }
        Pair<Potion, Integer> info = getDialogueInfo();
        if (info == null) {
            log(NightmareZone.class, "Problem reading barrel dialogue info.");
            return;
        }
        if (info.getKey() != potionType) {
            // if incorrect barrel for some reason
            log(NightmareZone.class, "Incorrect dialogue info");
            return;
        }
        int prevDoses = getDoses(potions, potionType);
        // update dose cache
        barrelDoseCache.put(potionType, info.getValue());
        // type amount
        getKeyboard().type(String.valueOf(dosesToWithdraw));
        // little sleep to ensure client registers input fully
        submitTask(() -> false, 200);
        // press enter key
        getKeyboard().pressKey(TouchType.DOWN_AND_UP, PhysicalKey.ENTER);
        // wait for dose amount to change
        submitTask(() -> {
            UIResultList<ItemSearchResult> potions_ = getItemManager().findAllOfItem(getWidgetManager().getInventory(), potionType.getItemIDs());
            if (potions_.isNotFound()) {
                return false;
            }
            return prevDoses > getDoses(potions_, potionType);
        }, 3000);
    }

    /**
     * @return
     */
    private Pair<Potion, Integer> getDialogueInfo() {
        Rectangle dialogueBounds = getWidgetManager().getDialogue().getBounds();
        if (dialogueBounds == null) {
            return null;
        }
        Rectangle[] bounds = getUtils().getTextBounds(dialogueBounds.getPadding(10), 16, BLACK_FONT_PIXEL);
        if (bounds.length == 0) {
            return null;
        }
        String text = getOCR().getText(Font.STANDARD_FONT_BOLD, bounds[0], BLACK_FONT_PIXEL);
        Potion potionType = null;
        for (Potion potion : Potion.values()) {
            if (!isBarrelPotion(potion))
                continue;
            if (text.toLowerCase().contains(potion.getName().toLowerCase())) {
                potionType = potion;
                break;
            }
        }

        Integer doses = Utility.extractNumberFromBrackets(text);
        if (doses == null) {
            return null;
        }
        return new Pair<>(potionType, doses);
    }

    private void usePotionOnBarrel(UIResultList<ItemSearchResult> potions, Potion potionType, RSObject barrel) {
        if (barrel == null) {
            throw new RuntimeException("Barrel can not be null.");
        }
        if (!barrel.isInteractableOnScreen()) {
            // walk closer to the barrel
            log(NightmareZone.class, "Barrel is not on screen, lets walk to it.");
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> {
                if (barrel == null) {
                    return true;
                }
                return barrel.isInteractableOnScreen();
            });
            getWalker().walkTo(barrel, builder.build());
            return;
        }
        // use potion
        ItemSearchResult potion = potions.getRandom();
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
        if (getFinger().tap(polygon, "Use " + itemDefinition.name + " -> " + barrel.getName())) {

            // wait until potion amount decrements
            submitTask(() -> {
                UIResultList<ItemSearchResult> boostPotions = getItemManager().findAllOfItem(getWidgetManager().getInventory(), potionType.getItemIDs());
                if (boostPotions.isNotVisible()) {
                    return false;
                }
                return boostPotions.size() < this.boostPotions.size();
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
        List<RSObject> banksFound = getObjectManager().getObjects(gameObject -> {
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
        });
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact(BANK_ACTIONS)) return;
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
        submitTask(() -> {
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
        }, (int) TimeUnit.MINUTES.toMillis(6), true, false, true);
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
        boolean isPrimary = statBoostPotion != null && !statBoostPotion.isBarrel();
        boolean isSecondary = secondaryPotion != null && !secondaryPotion.isBarrel();
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

    public static class DialogueOption {
        final String title;
        final String option;

        public DialogueOption(String title, String option) {
            this.title = title;
            this.option = option;
        }
    }
}
