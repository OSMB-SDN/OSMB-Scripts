package com.osmb.script.farming.tithefarm;

import com.osmb.api.input.PhysicalKey;
import com.osmb.api.input.TouchType;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.tabs.SettingsTabComponent;
import com.osmb.api.ui.component.tabs.skill.SkillType;
import com.osmb.api.ui.component.tabs.skill.SkillsTabComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.visual.PixelCluster;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.farming.tithefarm.data.PixelProvider;
import com.osmb.script.farming.tithefarm.javafx.ScriptOptions;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.osmb.script.farming.tithefarm.Utils.getRepeatingDigitAmount;

@ScriptDefinition(name = "Tithe Farm", skillCategory = SkillCategory.FARMING, author = "Joe", version = 1.0, description = "Gains Farming XP by completing the Tithe Farm activity.")
public class TitheFarm extends Script {

    public static final int[] CLUSTER_COLORS = new int[]{
            Color.CYAN.getRGB(),
            Color.MAGENTA.getRGB(),
            Color.ORANGE.getRGB(),
            Color.PINK.getRGB(),
            Color.BLUE.getRGB(),
            Color.GREEN.getRGB(),
            Color.YELLOW.getRGB()
    };
    // Allow for spam watering
    private static final PolyArea TITHE_FARM_AREA = new PolyArea(List.of(new WorldPosition(1804, 3498, 0), new WorldPosition(1805, 3499, 0), new WorldPosition(1805, 3503, 0), new WorldPosition(1804, 3504, 0), new WorldPosition(1807, 3504, 0), new WorldPosition(1807, 3516, 0), new WorldPosition(1835, 3516, 0), new WorldPosition(1835, 3507, 0), new WorldPosition(1840, 3507, 0), new WorldPosition(1840, 3495, 0), new WorldPosition(1835, 3495, 0), new WorldPosition(1835, 3486, 0), new WorldPosition(1823, 3486, 0), new WorldPosition(1823, 3481, 0), new WorldPosition(1819, 3481, 0), new WorldPosition(1819, 3486, 0), new WorldPosition(1807, 3486, 0), new WorldPosition(1807, 3498, 0)));

    private static final Set<Integer> WATERING_CANS = Set.of(
            ItemID.WATERING_CAN8,
            ItemID.WATERING_CAN7,
            ItemID.WATERING_CAN6,
            ItemID.WATERING_CAN5,
            ItemID.WATERING_CAN4,
            ItemID.WATERING_CAN3,
            ItemID.WATERING_CAN2,
            ItemID.WATERING_CAN1,
            ItemID.WATERING_CAN
    );
    private static final Set<Integer> WATERING_CANS_TO_FILL = Set.of(
            ItemID.WATERING_CAN7,
            ItemID.WATERING_CAN6,
            ItemID.WATERING_CAN5,
            ItemID.WATERING_CAN4,
            ItemID.WATERING_CAN3,
            ItemID.WATERING_CAN2,
            ItemID.WATERING_CAN1,
            ItemID.WATERING_CAN
    );
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(
            ItemID.WATERING_CAN8, // Watering can
            ItemID.SEED_DIBBER, // Seed dibber
            ItemID.SPADE,  // Spade
            ItemID.GRICOLLERS_CAN
    ));
    private static final Font ARIAL = new Font("Arial", Font.PLAIN, 14);
    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();
    private static final Stopwatch LEVEL_CHECK_TIMER = new Stopwatch();
    private static final int MAX_ZOOM = 20;
    Stopwatch longTapDelay = new Stopwatch();
    private PatchManager patchManager;
    private ItemGroupResult inventorySnapshot;
    private Plant selectedPlant;
    private Task task;
    private int wateringCanDoses = -1;
    private int plantAmount;
    private Stopwatch breakDelay = null;
    private Stopwatch hopDelay = null;
    private int fruitAmount = -1;
    private int harvestedAmount = 0;
    private boolean needsToHop = false;
    private int farmingLevel = 0;
    private int depositMax;
    private int depositMin;
    private int depositAmount;
    private boolean setZoom = false;
    private boolean depositFlag = false;

    public TitheFarm(Object scriptCore) {
        super(scriptCore);
    }

    private static void drawPatchCluster(Polygon patchHull, Canvas c, PixelCluster.ClusterSearchResult result) {
        int count = 0;
        for (PixelCluster cluster : result.getClusters()) {
            int color = CLUSTER_COLORS[count++ % CLUSTER_COLORS.length];
            // Draw bounding rectangle
            c.drawRect(cluster.getBounds(), color);
            // Draw each point in the cluster
            for (Point p : cluster.getPoints()) {
                c.setRGB(p.x, p.y, color, 1);
            }
        }
        c.drawPolygon(patchHull, Color.GREEN.getRGB(), 1);
    }

    @Override
    public void onStart() {
        ScriptOptions ui = new ScriptOptions();
        Scene scene = new Scene(ui);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Tithe Farm Configuration", false);

        this.plantAmount = ui.getPlantAmount();
        this.depositMin = ui.getDepositFruitMin();
        this.depositMax = ui.getDepositFruitMax();
        this.depositAmount = Math.min(10000, Utils.random(depositMin / 100, depositMax / 100) * 100);
        PatchManager.PlantMethod plantMethod = ui.getPlantMethod();
        this.patchManager = new PatchManager(this, plantAmount, plantMethod);
        selectedPlant = Plant.BOLOGANO;
        for (Plant plant : Plant.values()) {
            ITEM_IDS_TO_RECOGNISE.add(plant.getSeedID());
            ITEM_IDS_TO_RECOGNISE.add(plant.getFruitID());
        }
        ITEM_IDS_TO_RECOGNISE.addAll(WATERING_CANS);

    }

    @Override
    public int poll() {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            return 0;
        }
        task = decideTask(worldPosition);
        if (task == null) {
            return 0;
        }
        executeTask(task);
        return 0;
    }

    private Task decideTask(WorldPosition worldPosition) {
        if (!setZoom) {
            return Task.SET_ZOOM;
        }
        if (LEVEL_CHECK_TIMER.hasFinished() || farmingLevel == 0) {
            return Task.CHECK_LEVEL;
        }

        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(TitheFarm.class, "Unable to snapshot inventory, returning null task");
            return null;
        }

        if (!inventorySnapshot.containsAll(Set.of(ItemID.SEED_DIBBER, ItemID.SPADE))) {
            log(TitheFarm.class, "Missing either a seed dibber or a spade, stopping script...");
            stop();
            return null;
        }

        selectedPlant = Plant.getPlantForLevel(farmingLevel);

        if (!TITHE_FARM_AREA.contains(worldPosition)) {
            // reset patch manager if we are outside the farm areaS
            if (patchManager.startedRun()) {
                log(TitheFarm.class, "Resetting patch manager...");
                patchManager.reset();
            }
            if (!inventorySnapshot.contains(selectedPlant.getSeedID())) {
                if (inventorySnapshot.isFull()) {
                    log(TitheFarm.class, "Inventory is full, no free space to take seeds, stopping script...");
                    stop();
                    return null;
                }
                return Task.TAKE_SEEDS;
            }
            return Task.ENTER_FARM;
        }

        int fruitAmount = inventorySnapshot.getAmount(selectedPlant.getFruitID());
        if (!depositFlag && fruitAmount >= depositAmount) {
            depositFlag = true;
        }

        if (depositFlag) {
            if (fruitAmount > 75) {
                return Task.DEPOSIT_FRUIT;
            } else {
                log(TitheFarm.class, "Fruit amount is below deposit threshold, not depositing...");
                depositFlag = false;
            }
        }

        if (!patchManager.startedRun()) {
            if (!inventorySnapshot.contains(selectedPlant.getSeedID())) {
                // ensure all fruit is deposited before leaving, even if it is below 75
                for (Plant plant : Plant.values()) {
                    if (inventorySnapshot.contains(plant.getFruitID())) {
                        return Task.DEPOSIT_FRUIT;
                    }
                }
                return Task.LEAVE_FARM;
            }
        }

        if (needsToHop && !patchManager.startedRun()) {
            log(TitheFarm.class, "Relogging to reset dead patches...");
            log(TitheFarm.class, "Random short delay...");
            submitTask(() -> false, Utils.random(500, 3000));
            getWidgetManager().getLogoutTab().logout();
            return null;
        }

        if (!inventorySnapshot.containsAny(WATERING_CANS) && !inventorySnapshot.contains(ItemID.GRICOLLERS_CAN)) {
            log(TitheFarm.class, "No watering can found, filling it up...");
            return Task.FILL_WATERING_CAN;
        }
        // calculate doses, only calc if we don't contain any below 3 as they all look the same (empty)
        if (!inventorySnapshot.containsAny(ItemID.WATERING_CAN, ItemID.WATERING_CAN1, ItemID.WATERING_CAN2, ItemID.WATERING_CAN3) && !inventorySnapshot.contains(ItemID.GRICOLLERS_CAN)) {
            log(TitheFarm.class, "Calculating watering can doses...");
            wateringCanDoses = 0;
            List<ItemSearchResult> wateringCans = inventorySnapshot.getAllOfItems(WATERING_CANS);
            for (ItemSearchResult can : wateringCans) {
                for (WateringCan wc : WateringCan.values()) {
                    if (wc.getItemId() == can.getId()) {
                        wateringCanDoses += wc.getDoses();
                    }
                }
            }
        }


        if (wateringCanDoses <= 0 || !patchManager.startedRun() && wateringCanDoses < plantAmount * 3) {
            if (inventorySnapshot.contains(ItemID.GRICOLLERS_CAN) && wateringCanDoses == -1) {
                getGricollerDoses();
                return null;
            }
            return Task.FILL_WATERING_CAN;
        }

        return Task.INTERACT_WITH_PATCH;
    }

    private void getGricollerDoses() {
        List<String> chatLines = listenChatbox();
        if (chatLines == null) {
            log(TitheFarm.class, "Failed to read chatbox");
            return;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(TitheFarm.class, "Unable to snapshot inventory");
            return;
        }
        ItemSearchResult gricollersCan = inventorySnapshot.getItem(ItemID.GRICOLLERS_CAN);
        if (gricollersCan == null) {
            log(TitheFarm.class, "No Gricoller's can found in inventory?");
            return;
        }
        if (gricollersCan.interact("check")) {
            submitTask(() -> {
                List<String> newChatLines = listenChatbox();
                if (newChatLines == null) {
                    return false;
                }
                for (String line : newChatLines) {
                    if (line.contains("charges remaining") && line.contains("%")) {
                        Matcher matcher = Pattern.compile("(\\d+)(\\.\\d+)?%").matcher(line);
                        if (matcher.find()) {
                            double percent = Double.parseDouble(matcher.group(1) + (matcher.group(2) != null ? matcher.group(2) : ""));
                            log(TitheFarm.class, "Charges remaining: " + percent + "%");
                            wateringCanDoses = (int) Math.round(percent * 10);
                            return true;
                        }
                    }
                }
                return false;
            }, Utils.random(4000, 8000));
        }

    }

    private List<String> listenChatbox() {
        if (getWidgetManager().getDialogue().getDialogueType() == null && getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            log(TitheFarm.class, "Switching to game chatbox filter tab...");
            if (!getWidgetManager().getChatbox().openFilterTab(ChatboxFilterTab.GAME)) {
                log(TitheFarm.class, "Failed to open chatbox filter tab");
            }
            return null;
        }
        UIResultList<String> textLines = getWidgetManager().getChatbox().getText();
        if (textLines.isNotVisible()) {
            log(TitheFarm.class, "Chatbox not visible");
            return null;
        }
        List<String> currentLines = textLines.asList();
        if (currentLines.isEmpty()) {
            return Collections.emptyList();
        }
        int firstDifference = 0;
        if (!PREVIOUS_CHATBOX_LINES.isEmpty()) {
            if (currentLines.equals(PREVIOUS_CHATBOX_LINES)) {
                return Collections.emptyList();
            }
            int currSize = currentLines.size();
            int prevSize = PREVIOUS_CHATBOX_LINES.size();
            for (int i = 0; i < currSize; i++) {
                int suffixLen = currSize - i;
                if (suffixLen > prevSize) continue;
                boolean match = true;
                for (int j = 0; j < suffixLen; j++) {
                    if (!currentLines.get(i + j).equals(PREVIOUS_CHATBOX_LINES.get(j))) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    firstDifference = i;
                    break;
                }
            }
        } else {
            PREVIOUS_CHATBOX_LINES.addAll(currentLines);
        }
        List<String> newLines = currentLines.subList(0, firstDifference);
        PREVIOUS_CHATBOX_LINES.clear();
        PREVIOUS_CHATBOX_LINES.addAll(currentLines);
        return newLines;
    }

    private void executeTask(Task task) {
        switch (task) {
            case ENTER_FARM -> enterFarm();
            case INTERACT_WITH_PATCH -> interactWithPatch();
            case FILL_WATERING_CAN -> fillWateringCan();
            case CHECK_LEVEL -> checkLevels();
            case DEPOSIT_FRUIT -> depositFruit();
            case TAKE_SEEDS -> takeSeeds();
            case LEAVE_FARM -> leaveFarm();
            case SET_ZOOM -> setZoom();
        }
    }

    private void leaveFarm() {
        handleDoor(false);
    }

    private void takeSeeds() {
        DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ENTER_AMOUNT) {
            log(TitheFarm.class, "Entering amount of seeds to take...");
            int amount = random(3) == 0 ? 10000 : getRepeatingDigitAmount();
            getKeyboard().type(String.valueOf(amount));
            sleep(100);
            getKeyboard().pressKey(TouchType.DOWN_AND_UP, PhysicalKey.ENTER);
            // wait until seeds are in the inventory
            submitHumanTask(() -> {
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    log(TitheFarm.class, "Unable to snapshot inventory");
                    return false;
                }
                return inventorySnapshot.contains(selectedPlant.getSeedID());
            }, Utils.random(3000, 6000));
            return;
        } else if (dialogueType == DialogueType.TEXT_OPTION) {
            UIResult<String> dialogueTitle = getWidgetManager().getDialogue().getDialogueTitle();
            if (dialogueTitle.isNotVisible()) {
                log(TitheFarm.class, "Dialogue title is not visible");
                return;
            }
            String title = dialogueTitle.get();
            if (title.equalsIgnoreCase("what kind of crop will you grow?")) {
                log(TitheFarm.class, "Selecting plant: " + selectedPlant.name());
                if (getWidgetManager().getDialogue().selectOption(selectedPlant.getDialogueOption())) {
                    // wait until enter amount dialogue appears
                    log(TitheFarm.class, "Selected plant: " + selectedPlant.name() + ", waiting for enter amount dialogue...");
                    submitTask(() -> {
                        DialogueType dialogueType1 = getWidgetManager().getDialogue().getDialogueType();
                        log(TitheFarm.class, "Dialogue type: " + dialogueType1);
                        return dialogueType1 == DialogueType.ENTER_AMOUNT;
                    }, Utils.random(3000, 5000));
                }
                return;
            } else {
                log(TitheFarm.class, "Unexpected dialogue title: " + title);
            }
        }

        RSObject seedTable = getObjectManager().getClosestObject("seed table");
        if (seedTable == null) {
            log(TitheFarm.class, "No seed table found... Make sure you start the script near the Tithe Farm area.");
            return;
        }
        if (seedTable.interact("search")) {
            // wait until the dialogue appears
            boolean result = submitHumanTask(() -> {
                DialogueType type = getWidgetManager().getDialogue().getDialogueType();
                return type == DialogueType.TEXT_OPTION || type == DialogueType.ENTER_AMOUNT;
            }, Utils.random(3000, 5000));
            if (!result) {
                log(TitheFarm.class, "Failed to open seed table dialogue.");
            }
        } else {
            log(TitheFarm.class, "Failed to interact with seed table.");
        }
    }

    private void depositFruit() {
        if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return;
        }
        int initialFruitAmount = inventorySnapshot.getAmount(selectedPlant.getFruitID());
        RSObject sack = getObjectManager().getClosestObject("sack");
        if (sack == null) {
            log(TitheFarm.class, "No sack found...");
            return;
        }
        if (sack.interact("deposit")) {
            long noMovementTimeout = random(3000, 5000);
            submitHumanTask(() -> {
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    log(TitheFarm.class, "Unable to snapshot inventory");
                    return false;
                }
                if (getLastPositionChangeMillis() > noMovementTimeout) {
                    log(TitheFarm.class, "No movement for " + noMovementTimeout + "ms.");
                    return true;
                }
                return inventorySnapshot.getAmount(selectedPlant.getFruitID()) < initialFruitAmount;
            }, Utils.random(8000, 14000));
        }
    }

    private void checkLevels() {
        SkillsTabComponent.SkillLevel farmingSkillLevel = getWidgetManager().getSkillTab().getSkillLevel(SkillType.FARMING);
        if (farmingSkillLevel == null) {
            log(TitheFarm.class, "Failed to get skill levels.");
            return;
        }
        farmingLevel = farmingSkillLevel.getLevel();
        log(TitheFarm.class, "Farming level: " + farmingLevel);
        LEVEL_CHECK_TIMER.reset(random(TimeUnit.MINUTES.toMillis(8), TimeUnit.MINUTES.toMillis(30)));
    }

    private void fillWateringCan() {
        log(TitheFarm.class, "Filling watering cans...");
        if (getWidgetManager().getDialogue().getDialogueType() == null && getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            log(TitheFarm.class, "Switching to game chatbox filter tab...");
            if (!getWidgetManager().getChatbox().openFilterTab(ChatboxFilterTab.GAME)) {
                log(TitheFarm.class, "Failed to open chatbox filter tab");
            }
            return;
        }
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            log(TitheFarm.class, "World position is null, cannot fill watering can.");
            return;
        }
        RSObject waterBarrel = getObjectManager().getClosestObject("water barrel");
        if (waterBarrel == null) {
            log(TitheFarm.class, "No water barrel found, stopping script...");
            return;
        }
        boolean walk = random(3) == 1 && waterBarrel.distance(worldPosition) > 3;
        if (!waterBarrel.isInteractableOnScreen() || walk) {
            int breakDistance = random(2, 5);
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> {
                WorldPosition worldPosition1 = getWorldPosition();
                if (worldPosition1 == null) {
                    log(TitheFarm.class, "World position is null.");
                    return false;
                }
                return waterBarrel.isInteractableOnScreen() && waterBarrel.distance(worldPosition1) <= breakDistance;
            });
            getWalker().walkTo(waterBarrel, builder.build());
            return;
        }
        submitHumanTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                log(TitheFarm.class, "Unable to snapshot inventory");
                return false;
            }
            int fullWateringCans = inventorySnapshot.getAmount(ItemID.WATERING_CAN8);
            boolean gricollers = inventorySnapshot.contains(ItemID.GRICOLLERS_CAN);
            if (gricollers && wateringCanDoses == 1000) {
                return true;
            }
            List<ItemSearchResult> cansToFill = gricollers ? inventorySnapshot.getAllOfItem(ItemID.GRICOLLERS_CAN) : inventorySnapshot.getAllOfItems(WATERING_CANS_TO_FILL);
            if (cansToFill.isEmpty()) {
                log(TitheFarm.class, "No watering cans to fill found in inventory");
                return true;
            }
            Integer selectedSlot = inventorySnapshot.getSelectedSlot();
            boolean selectedCan = true;
            if (selectedSlot != null) {
                for (ItemSearchResult can : cansToFill) {
                    if (can.getSlot() == selectedSlot) {
                        log(TitheFarm.class, "Already selected a watering can, not selecting another one");
                        selectedCan = false;
                        break;
                    }
                }
                if (selectedCan) {
                    log(TitheFarm.class, "Selected item in the inventory, but it is not a watering can, unselecting it...");
                    getWidgetManager().getInventory().unSelectItemIfSelected();
                    return false;
                }
            }
            if (selectedCan) {
                // get a random can to select
                ItemSearchResult randomWateringCan = cansToFill.get(random(cansToFill.size()));
                randomWateringCan.interact();
                // wait until the can is selected
                boolean result = submitTask(() -> {
                    inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                    if (inventorySnapshot == null) {
                        log(TitheFarm.class, "Unable to snapshot inventory");
                        return false;
                    }
                    Integer selectedSlot1 = inventorySnapshot.getSelectedSlot();
                    return selectedSlot1 != null && selectedSlot1 == randomWateringCan.getSlot();
                }, Utils.random(800, 1300));
                if (!result) {
                    log(TitheFarm.class, "Failed to select watering can...");
                    return false;
                }
            }
            Polygon barrelHull = waterBarrel.getConvexHull();
            if (barrelHull == null || (barrelHull = barrelHull.getResized(0.7)) == null || getWidgetManager().insideGameScreenFactor(barrelHull, Collections.emptyList()) < 0.1) {
                log(TitheFarm.class, "Water barrel is not on screen...");
                return true;
            }
            List<String> lines = listenChatbox();
            if (lines == null) {
                return false;
            }
            getFinger().tapGameScreen(barrelHull);
            // wait until a full watering can is in the inventory
            log(TitheFarm.class, "Waiting for watering can to be filled...");
            boolean filled = submitTask(() -> {
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    log(TitheFarm.class, "Unable to snapshot inventory");
                    return false;
                }
                if (gricollers) {
                    // wait for Gricoller's can to be filled
                    List<String> newLines = listenChatbox();
                    if (newLines == null) {
                        return false;
                    }
                    for (String line : newLines) {
                        if (line.toLowerCase().contains("you fill the watering can") || line.toLowerCase().contains("is already full")) {
                            log(TitheFarm.class, "Gricoller's can filled successfully.");
                            wateringCanDoses = 1000;
                            return true;
                        }
                    }
                    return false;
                } else {
                    // wait for all cans to be filled
                    return inventorySnapshot.getAllOfItems(WATERING_CANS_TO_FILL).isEmpty();
                }
            }, gricollers ? random(5000, 8000) : random(10000, 15000));
            if (filled) {
                // reaction delay after planting
                consistentHumanDelay();
            }
            return false;
        }, Utils.random(20000, 38000));
    }

    private void setZoom() {
        log(TitheFarm.class, "Checking zoom level...");
        // check if the settings tab + display sub-tab is open, if not, open it
        if (!getWidgetManager().getSettings().openSubTab(SettingsTabComponent.SettingsSubTabType.DISPLAY_TAB)) {
            return;
        }
        UIResult<Integer> zoomResult = getWidgetManager().getSettings().getZoomLevel();
        if (zoomResult.isFound()) {
            int currentZoom = zoomResult.get();
            if (currentZoom > MAX_ZOOM) {
                // generate random zoom level between 0 and MAX_ZOOM
                int zoomLevel = random(0, MAX_ZOOM);
                if (getWidgetManager().getSettings().setZoomLevel(zoomLevel)) {
                    log(TitheFarm.class, "Zoom level set to: " + zoomLevel);
                    // zoom level set, set flag to true
                    setZoom = true;
                }
            } else {
                setZoom = true;
            }
        }
    }

    private void interactWithPatch() {
        WorldPosition myPosition = getExpectedWorldPosition();
        if (myPosition == null) {
            log(TitheFarm.class, "Player position is null...");
            return;
        }
        log(TitheFarm.class, "Interacting with patch...");
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            log(TitheFarm.class, "Unable to snapshot inventory");
            return;
        }

        int initialSeedAmount = inventorySnapshot.getAmount(selectedPlant.getSeedID());
        int initialFruitAmount = inventorySnapshot.getAmount(selectedPlant.getFruitID());

        Patch focusedPatch = patchManager.getFocusedPatch();
        if (focusedPatch == null) {
            log(TitheFarm.class, "No next patch found...");
            return;
        }
        if (focusedPatch.isHarvested()) {
            log(TitheFarm.class, "Focused patch at " + focusedPatch.getPosition() + " is already harvested, moving to next patch...");
            patchManager.setNextPatch();
            return;
        }
        if (this.fruitAmount == -1) {
            this.fruitAmount = initialFruitAmount;
        }
        if (initialFruitAmount > this.fruitAmount) {
            this.fruitAmount = initialFruitAmount;
            log(TitheFarm.class, "Successfully harvested " + selectedPlant.name() + " from patch at " + focusedPatch.getPosition() + ".");
            Patch actualPatch = patchManager.getPatchByPlayerPosition(myPosition);
            if (actualPatch == null) {
                log(TitheFarm.class, "No actual patch found for player position...");
                return;
            }
            actualPatch.setHarvested(true);
            harvestedAmount++;
            patchManager.setNextPatch();
            return;
        } else {
            this.fruitAmount = initialFruitAmount;
        }

        if (focusedPatch.isDead()) {
            log(TitheFarm.class, "Next patch is dead, skipping...");
            patchManager.setNextPatch();
            needsToHop = true;
            return;
        }

        log(TitheFarm.class, "Patch: " + focusedPatch);
        RSObject patchObject = patchManager.getPatchObject(focusedPatch.getPosition());
        if (patchObject == null) {
            log(TitheFarm.class, "No patch object found...");
            return;
        }

        // if not inside the patch row, walk to it
        PatchManager.PatchRow row = PatchManager.PatchRow.getRowByPosition(myPosition);
        if (row == null) {
            log(TitheFarm.class, "Not inside a patch row, walking to the patch in the row...");
            walkToPatch(patchObject, focusedPatch);
            return;
        }
        double minGameScreenFactor = focusedPatch.getPlantTime() == -1 ? 0.2 : 0.8;

        Polygon patchHullFull = patchObject.getConvexHull();
        if (patchHullFull == null) {
            // walk to the patch if we can't find it
            log(TitheFarm.class, "Patch poly is null, walking to it...");
            walkToPatch(patchObject, focusedPatch);
            return;
        }
        // patch hull is not null, so it is on screen
        // now check if the resized version of the hull is on screen & also not covered by widgets
        Polygon patchHull = patchHullFull.getResized(0.7);
        boolean needsToWalk = patchHull == null;
        if (!needsToWalk) {
            // check if the patch hull is inside the game screen
            double insideGameScreenFactor = getWidgetManager().insideGameScreenFactor(patchHull, Collections.emptyList());
            log(TitheFarm.class, "Patch poly inside game screen factor: " + insideGameScreenFactor);
            if (insideGameScreenFactor < minGameScreenFactor) {
                log(TitheFarm.class, "Patch poly is not inside game screen, walking to it...");
                needsToWalk = true;
            }
        }
        if (needsToWalk) {
            log(TitheFarm.class, "Patch poly is not inside game screen, walking to it...");
            if (!longTapDelay.hasFinished()) {
                return;
            }
            if (inventorySnapshot.getSelectedSlot() != null) {
                log(TitheFarm.class, "Unselecting item in inventory before walking to patch...");
                getWidgetManager().getInventory().unSelectItemIfSelected();
            }
            tapWalkToPatch(myPosition, patchObject, focusedPatch, minGameScreenFactor);
            return;
        }
        // check if the patch is watered
        if (isWatered(patchHull)) {
            if (focusedPatch.isWatered()) {
                // wait until the patch is ready to be watered again
                waitForPatch(focusedPatch, patchObject, minGameScreenFactor);
            } else {
                log(TitheFarm.class, "Patch is already watered, moving to next patch...");
                // patch is already watered, move to next patch
                wateringCanDoses--;
                focusedPatch.setLastWateredTime(System.currentTimeMillis());
                patchManager.setNextPatch();
            }
            return;
        }
        // check if the patch is dead
        if (isDead(patchHull)) {
            log(TitheFarm.class, "Patch contains dead plant...");
            focusedPatch.setDead(true);
            patchManager.setNextPatch();
            return;
        }
        // patch is not watered, so we can water it or plant a seed
        if (isPlanted(patchHull)) {
            waterHarvestPatch(patchHull, focusedPatch, patchObject, initialFruitAmount);
        } else {
            if (focusedPatch.getPlantTime() == -1) {
                plantSeed(patchHull, patchObject, initialSeedAmount, focusedPatch);
            } else {
                log(TitheFarm.class, "[WARNING] Patch is not planted, but already has a plant time set, skipping...");
                focusedPatch.setHarvested(true);
                patchManager.setNextPatch();
            }
        }
    }

    private boolean isPlanted(Polygon patchHull) {
        return getPixelAnalyzer().findPixel(patchHull, selectedPlant.getPixels()) != null;
    }

    private void waitForPatch(Patch nextPatch, RSObject patchObject, double minGameScreenFactor) {
        log(TitheFarm.class, "Waiting for patch at " + nextPatch.getPosition() + " to be ready for watering...");
        boolean result = submitTask(() -> {
            WorldPosition worldPosition = getExpectedWorldPosition();
            if (worldPosition == null) {
                log(TitheFarm.class, "Player position is null...");
                return false;
            }
            if (patchObject.distance(worldPosition) > 1) {
                log(TitheFarm.class, "Walking to patch while waiting for it to be ready...");
                tapWalkToPatch(worldPosition, patchObject, nextPatch, minGameScreenFactor);
                return false;
            }
            Polygon patchHull_ = patchObject.getConvexHull();
            if (patchHull_ == null || (patchHull_ = patchHull_.getResized(0.7)) == null || getWidgetManager().insideGameScreenFactor(patchHull_, Collections.emptyList()) < minGameScreenFactor) {
                log(TitheFarm.class, "Patch hull is null... not on screen?");
                return false;
            }
            return !isWatered(patchHull_);
        }, Utils.random(1000, 8000));
        if (!result) {
            log(TitheFarm.class, "Patch not ready for watering after waiting...");
        }
    }

    private void plantSeed(Polygon patchHull, RSObject patchObject, int initialSeedAmount, Patch nextPatch) {
        log(TitheFarm.class, "Nothing planted in patch, planting seed...");
        // check planted seeds
        ItemSearchResult seed = inventorySnapshot.getItem(selectedPlant.getSeedID());
        if (seed == null) {
            log(TitheFarm.class, "Seed is null");
            return;
        }
        if (!seed.isSelected()) {
            // select seed
            if (seed.interact()) {
                // update hull
                patchHull = patchObject.getConvexHull();
                if (patchHull == null || (patchHull = patchHull.getResized(0.7)) == null || getWidgetManager().insideGameScreenFactor(patchHull, Collections.emptyList()) < 0.2) {
                    // walk to the patch if we can't find it
                    log(TitheFarm.class, "Patch hull is null... not on screen?");
                    return;
                }
            }
        }

        if (!getFinger().tapGameScreen(patchHull)) {
            log(TitheFarm.class, "Failed tapping patch, not on game screen");
            return;
        }
        long noMovementTimeout = random(1300, 1900);
        final MovementChecker[] movementChecker = {null};
        // wait until the seed is planted
        boolean planted = submitTask(() -> {
            log(TitheFarm.class, "Waiting until planted...");
            inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                log(TitheFarm.class, "Unable to snapshot inventory");
                return false;
            }
            int seedAmount = inventorySnapshot.getAmount(selectedPlant.getSeedID());
            log(TitheFarm.class, "Seed amount: " + seedAmount + ", initial seed amount: " + initialSeedAmount);
            if (seedAmount < initialSeedAmount) {
                log(TitheFarm.class, "Successfully planted " + selectedPlant.name() + " in patch at " + nextPatch.getPosition());
                nextPatch.setPlantTime(System.currentTimeMillis());
                return true;
            }
            WorldPosition worldPosition = getExpectedWorldPosition();
            if (worldPosition == null) {
                log(TitheFarm.class, "Player position is null...");
                return false;
            }
            if (!com.osmb.script.farming.tithefarm.Utils.isCardinallyAdjacent(worldPosition, patchObject.getObjectArea())) {
                // not at the patch yet
                WorldPosition position = getWorldPosition();
                if (position == null) {
                    log(TitheFarm.class, "World position is null...");
                    return false;
                }
                if (movementChecker[0] == null) {
                    movementChecker[0] = new MovementChecker(position, noMovementTimeout);
                } else {
                    if (movementChecker[0].hasTimedOut(position)) {
                        log(TitheFarm.class, "Movement checker timed out, no movement for " + noMovementTimeout + "ms.");
                        return true;
                    }
                }
            }
            return false;
        }, Utils.random(3500, 6000));

        if (planted) {
            consistentHumanDelay();
        }
    }

    private void consistentHumanDelay() {
        if (random(12) == 0) {
            // slightly longer delay for some randomness
            submitTask(() -> false, RandomUtils.gaussianRandom(100, 3000, 300, 150));
        } else if (random(30) == 0) {
            // longer delay for some randomness
            submitTask(() -> false, RandomUtils.gaussianRandom(100, 3000, 500, 650));
        } else {
            // normal delay
            submitTask(() -> false, RandomUtils.gaussianRandom(100, 3000, 100, 150));
        }
    }

    private void waterHarvestPatch(Polygon patchHull, Patch focusedPatch, RSObject patchObject, int initialFruitAmount) {
        log(TitheFarm.class, "Patch is planted, watering it...");
        // patch is planted - water it or listen for harvesting
        if (inventorySnapshot.getSelectedSlot() != null) {
            // if we have a selected item, unselect it
            log(TitheFarm.class, "Unselecting item in inventory before watering...");
            getWidgetManager().getInventory().unSelectItemIfSelected();
            return;
        }
        Stopwatch seedPreSelectDelay = new Stopwatch(random(0, 2500));
        long noMovementTimeout = random(800, 1300);
        final MovementChecker[] movementChecker = {null};
        if (getFinger().tapGameScreen(patchHull)) {
            log(TitheFarm.class, "Interacted with patch at " + focusedPatch.getPosition() + ". Waiting for result...");
            boolean result = submitTask(() -> {
                WorldPosition worldPosition = getExpectedWorldPosition();
                if (worldPosition == null) {
                    log(TitheFarm.class, "Player position is null...");
                    return false;
                }
                inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    log(TitheFarm.class, "Unable to snapshot inventory");
                    return false;
                }
                Polygon patchHull_ = patchObject.getConvexHull();
                if (patchHull_ == null || (patchHull_ = patchHull_.getResized(0.7)) == null || getWidgetManager().insideGameScreenFactor(patchHull_, Collections.emptyList()) < 0.8) {
                    // walk to the patch if we can't find it
                    log(TitheFarm.class, "Patch hull is null... not on screen?");
                    return false;
                }

                Patch nextPatch = patchManager.getNextPatch();
                if (nextPatch.plantTime == -1 && seedPreSelectDelay.hasFinished()) {
                    // pre-select seed
                    RSObject nextPatchObject = patchManager.getPatchObject(nextPatch.getPosition());
                    if (nextPatchObject != null) {
                        Polygon nextPatchHull = nextPatchObject.getConvexHull();
                        if (nextPatchHull != null && (nextPatchHull = nextPatchHull.getResized(0.7)) != null) {
                            double insideGameScreenFactor = getWidgetManager().insideGameScreenFactor(nextPatchHull, Collections.emptyList());
                            getScreen().getDrawableCanvas().drawPolygon(nextPatchHull, Color.CYAN.getRGB(), 1);
                            if (insideGameScreenFactor >= 0.3) {
                                ItemSearchResult seed = inventorySnapshot.getItem(selectedPlant.getSeedID());
                                if (seed != null && !seed.isSelected()) {
                                    log(TitheFarm.class, "Pre-selecting seed for next patch at " + nextPatch.getPosition() + "...");
                                    // select seed
                                    seed.interact();
                                }
                            }
                        }
                    }
                }

                fruitAmount = inventorySnapshot.getAmount(selectedPlant.getFruitID());
                // check harvested fruit
                if (fruitAmount > initialFruitAmount) {
                    Patch actualPatch = patchManager.getPatchByPlayerPosition(worldPosition);
                    if (actualPatch == null) {
                        log(TitheFarm.class, "No actual patch found for player position...");
                        return false;
                    }
                    log(TitheFarm.class, "Successfully harvested " + selectedPlant.name() + " from patch at " + focusedPatch.getPosition());
                    actualPatch.setHarvested(true);
                    harvestedAmount++;
                    if (actualPatch == focusedPatch) {
                        patchManager.setNextPatch();
                    }
                    return true;
                }
                if (isDead(patchHull_)) {
                    log(TitheFarm.class, "Patch is dead, skipping...");
                    focusedPatch.setDead(true);
                    patchManager.setNextPatch();
                    needsToHop = true;
                    return true;
                }
                if (!com.osmb.script.farming.tithefarm.Utils.isCardinallyAdjacent(worldPosition, patchObject.getObjectArea())) {
                    // not at the patch yet
                    WorldPosition position = getWorldPosition();
                    if (movementChecker[0] == null) {
                        movementChecker[0] = new MovementChecker(position, noMovementTimeout);
                    } else {
                        if (movementChecker[0].hasTimedOut(position)) {
                            log(TitheFarm.class, "Movement checker timed out, no movement for " + noMovementTimeout + "ms.");
                            return true;
                        }
                    }
                    return false;
                }
                // at this point we are at the patch

                // check if patch is watered
                if (isWatered(patchHull_)) {
                    log(TitheFarm.class, "Patch is watered, moving to next patch...");
                    focusedPatch.setLastWateredTime(System.currentTimeMillis());
                    wateringCanDoses--;
                    patchManager.setNextPatch();
                    return true;
                }
                return false;
            }, Utils.random(6000, 9000));
            if (result) {
                // reaction delay after watering
                consistentHumanDelay();
            }
        }
    }

    @Override
    public void onRelog() {
        needsToHop = false;
        patchManager.reset();
    }

    private boolean isDead(Polygon patchHull) {
        PixelCluster.ClusterSearchResult result = getPixelAnalyzer().findClusters(patchHull, PixelProvider.DEAD_PLANT_QUERY);
        if (result == null) {
            log(TitheFarm.class, "No water clusters found, patch is not watered");
            return false;
        }
        getScreen().queueCanvasDrawable("deadPixels", c -> {
            drawPatchCluster(patchHull, c, result);
        });
        return !result.getClusters().isEmpty();
    }

    private void tapWalkToPatch(WorldPosition worldPosition, RSObject patchObject, Patch nextPatch, double minGameScreenFactor) {
        log(TitheFarm.class, "Distance from patch: " + patchObject.distance(worldPosition) + ", walking to patch...");
        RectangleArea walkArea;
        if (patchObject.getWorldX() == nextPatch.getRow().getLeftXPosition()) {
            // patch is on the left side
            walkArea = new RectangleArea(patchObject.getWorldX() + 1, patchObject.getWorldY(), 1, 2, patchObject.getPlane());
        } else {
            // patch is on the right side
            walkArea = new RectangleArea(patchObject.getWorldX(), patchObject.getWorldY(), 1, 2, patchObject.getPlane());
        }

        WorldPosition randomPosition = walkArea.getRandomPosition();
        Polygon tilePoly = getSceneProjector().getTilePoly(randomPosition);
        if (tilePoly == null || (tilePoly = tilePoly.getResized(0.7)) == null || getWidgetManager().insideGameScreenFactor(tilePoly, Collections.emptyList()) < minGameScreenFactor || randomPosition.distanceTo(worldPosition) > 13) {
            log(TitheFarm.class, "Unable to get tile polygon for patch at " + randomPosition);
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.minimapTapDelay(300, 1500);
            builder.setWalkMethods(false, true);
            builder.tileRandomisationRadius(2);
            builder.setHandleContainerObstruction(false);
            builder.breakDistance(2);
            builder.breakCondition(() -> {
                Polygon patchHull = patchObject.getConvexHull();
                if (patchHull == null || (patchHull = patchHull.getResized(0.7)) == null) {
                    return false;
                }
                double minGameScreenFactor2 = nextPatch.getPlantTime() == -1 ? 0.2 : 0.8;
                return getWidgetManager().insideGameScreenFactor(patchHull, Collections.emptyList()) >= minGameScreenFactor2;
            });
            getWalker().walkTo(patchObject, builder.build());
            return;
        }
        RectangleArea patchArea = patchObject.getObjectArea();
        boolean longTap = patchArea.contains(randomPosition);

        boolean success = longTap ? getFinger().tapGameScreen(tilePoly, "walk here") : getFinger().tapGameScreen(tilePoly);
        if (success) {
            if (longTap) {
                longTapDelay.reset(random(700, 1100));
            }
            submitTask(() -> {
                WorldPosition worldPosition1 = getWorldPosition();
                if (worldPosition1 == null) {
                    log(TitheFarm.class, "Player position is null...");
                    return false;
                }
                Polygon patchHull = patchObject.getConvexHull();
                if (patchHull != null && (patchHull = patchHull.getResized(0.7)) != null && getWidgetManager().insideGameScreenFactor(patchHull, Collections.emptyList()) >= minGameScreenFactor) {
                    return true;
                }
                return patchObject.distance(worldPosition1) <= 1;
            }, Utils.random(4000, 6000));
        }
    }

    private boolean isWatered(Polygon patchHull) {
        PixelCluster.ClusterSearchResult result = getPixelAnalyzer().findClusters(patchHull, PixelProvider.WATER_QUERY);
        if (result == null) {
            log(TitheFarm.class, "Patch is not watered, no water clusters found");
            return false;
        }
        getScreen().queueCanvasDrawable("wateredPixels", c -> drawPatchCluster(patchHull, c, result));
        StringBuilder sb = new StringBuilder();
        for (PixelCluster cluster : result.getClusters()) {
            sb.append("Cluster size: ").append(cluster.getPoints().size()).append(", ");
        }
        log(TitheFarm.class, "Patch is watered, found " + result.getClusters().size() + " water clusters. " + sb);
        return result.getClusters().size() >= 6;
    }

    private void walkToPatch(RSObject patchObject, Patch nextPatch) {
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakCondition(() -> {
            WorldPosition worldPosition = getWorldPosition();
            if (worldPosition == null) {
                return false;
            }
            // TODO: Check if patch is at edge of middle row & if the player is in the middle row
            if (PatchManager.PatchRow.getRowByPosition(worldPosition) != nextPatch.getRow()) {
                return false;
            }
            Polygon hull = patchObject.getConvexHull();
            return hull != null && getWidgetManager().insideGameScreenFactor(hull, Collections.emptyList()) > 0.8;
        });
        builder.setHandleContainerObstruction(true);
        WorldPosition patchPosition = nextPatch.getPosition();
        PatchManager.PatchRow patchRow = nextPatch.getRow();
        WorldPosition destination = new WorldPosition(patchRow.getMiddleXPosition(), patchPosition.getY(), patchPosition.getPlane());
        getWalker().walkTo(destination, builder.build());
    }

    private void enterFarm() {
        // handle dialogue if it exists
        DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.CHAT_DIALOGUE) {
            UIResult<String> dialogueText = getWidgetManager().getDialogue().getText();
            if (dialogueText.isFound()) {
                String text = dialogueText.get();
                if (text.toLowerCase().contains("do you know what you")) {
                    getWidgetManager().getDialogue().continueChatDialogue();
                    return;
                } else if (text.toLowerCase().contains("don't ask me this again")) {
                    getWidgetManager().getDialogue().continueChatDialogue();
                    pollFramesHuman(() -> {
                        WorldPosition worldPosition = getWorldPosition();
                        if (worldPosition == null) {
                            log(TitheFarm.class, "Player position is null...");
                            return false;
                        }
                        return TITHE_FARM_AREA.contains(worldPosition);
                    }, RandomUtils.uniformRandom(7000, 9000));
                    return;
                }
            }
        } else if (dialogueType == DialogueType.TEXT_OPTION) {
            if (getWidgetManager().getDialogue().selectOption("I'm an expert - don't ask me this again")) {
                // wait for chat dialogue
                pollFramesHuman(() -> {
                    DialogueType dialogueType1 = getWidgetManager().getDialogue().getDialogueType();
                    return dialogueType1 == DialogueType.CHAT_DIALOGUE;
                }, RandomUtils.uniformRandom(7000, 9000));
                return;
            }
        }
        handleDoor(true);
    }

    private void handleDoor(boolean enter) {
        RSObject door = getObjectManager().getClosestObject("farm door");
        if (door == null) {
            log(TitheFarm.class, "No farm door found");
            return;
        }
        if (door.interact("open")) {
            log(TitheFarm.class, "Waiting until " + (enter ? "inside" : "outside") + " the Tithe Farm area...");
            submitHumanTask(() -> {
                WorldPosition worldPosition = getWorldPosition();
                if (worldPosition == null) {
                    log(TitheFarm.class, "Player position is null...");
                    return false;
                }
                DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
                return dialogueType == DialogueType.CHAT_DIALOGUE || (enter == TITHE_FARM_AREA.contains(worldPosition));
            }, Utils.random(7000, 9000));
        }
    }

    @Override
    public void onPaint(Canvas c) {
        FontMetrics metrics = c.getFontMetrics(ARIAL);
        int padding = 5;

        List<String> lines = new ArrayList<>();
        lines.add("Task: " + (task == null ? "None" : task));
        lines.add("Fruit harvested: " + harvestedAmount);
        lines.add("Focused patch index: " + patchManager.getFocusedPatchIndex());
        lines.add("Watering can doses: " + wateringCanDoses);
        lines.add("Farming level: " + farmingLevel);
        lines.add("Next level check: " + LEVEL_CHECK_TIMER.getRemainingTimeFormatted());

        if (breakDelay != null) {
            lines.add("Break delay: " + breakDelay.getRemainingTimeFormatted());
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

        patchManager.paintPatches(c);
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{7222};
    }

    @Override
    public boolean canBreak() {
        if (breakDelay != null && breakDelay.hasFinished()) {
            breakDelay = null;
            return true;
        }
        boolean canBreak = !patchManager.startedRun();
        if (getProfileManager().isDueToBreak() && canBreak) {
            if (breakDelay == null) {
                breakDelay = new Stopwatch(Utils.random(0, 15000));
            }
        }
        return canBreak;
    }

    @Override
    public boolean canHopWorlds() {
        if (hopDelay != null && hopDelay.hasFinished()) {
            hopDelay = null;
            return true;
        }
        boolean canHop = !patchManager.startedRun();
        if (getProfileManager().isDueToHop() && canHop) {
            if (hopDelay == null) {
                hopDelay = new Stopwatch(Utils.random(0, 15000));
            }
        }
        return canHop;
    }

    enum Task {
        ENTER_FARM,
        TAKE_SEEDS,
        DEPOSIT_FRUIT,
        LEAVE_FARM,
        INTERACT_WITH_PATCH,
        FILL_WATERING_CAN,
        GRAB_SEEDS,
        HOP_WORLDS,
        CHECK_LEVEL,
        SET_ZOOM;
    }

    enum Plant {
        GOLOVANOVA(PixelProvider.GOLOVANOVA_PIXELS, ItemID.GOLOVANOVA_SEED, ItemID.GOLOVANOVA_FRUIT, 34, "Golovanova seed (level 34)"),
        BOLOGANO(PixelProvider.BOLOGANO_PIXELS, ItemID.BOLOGANO_SEED, ItemID.BOLOGANO_FRUIT, 54, "Bologano seed (level 54)"),
        LOGAVANO_SEED(PixelProvider.LOGAVANO_PIXELS, ItemID.LOGAVANO_SEED, ItemID.LOGAVANO_FRUIT, 74, "Logavano seed (level 74)");

        private final SearchablePixel[] pixels;
        private final int level;
        private final int seedID;
        private final int fruitID;
        private final String dialogueOption;

        Plant(SearchablePixel[] pixels, int seedID, int fruitID, int level, String dialogueOption) {
            this.pixels = pixels;
            this.level = level;
            this.seedID = seedID;
            this.fruitID = fruitID;
            this.dialogueOption = dialogueOption;
        }

        public static Plant getPlantForLevel(int level) {
            Plant[] plants = Plant.values();
            for (int i = plants.length - 1; i >= 0; i--) {
                if (plants[i].getLevel() <= level) {
                    return plants[i];
                }
            }
            return null; // No tree found for the given level
        }

        public String getDialogueOption() {
            return dialogueOption;
        }

        public int getFruitID() {
            return fruitID;
        }

        public int getSeedID() {
            return seedID;
        }

        public SearchablePixel[] getPixels() {
            return pixels;
        }

        public int getLevel() {
            return level;
        }
    }

    enum WateringCan {
        CAN_1(ItemID.WATERING_CAN1, 1),
        CAN_2(ItemID.WATERING_CAN2, 2),
        CAN_3(ItemID.WATERING_CAN3, 3),
        CAN_4(ItemID.WATERING_CAN4, 4),
        CAN_5(ItemID.WATERING_CAN5, 5),
        CAN_6(ItemID.WATERING_CAN6, 6),
        CAN_7(ItemID.WATERING_CAN7, 7),
        CAN_8(ItemID.WATERING_CAN8, 8);

        private final int itemId;
        private final int doses;

        WateringCan(int itemId, int doses) {
            this.itemId = itemId;
            this.doses = doses;
        }

        public int getItemId() {
            return itemId;
        }

        public int getDoses() {
            return doses;
        }
    }

    public static class MovementChecker {
        private final long timeout;
        private WorldPosition initialPosition;
        private long lastMovementTime;

        public MovementChecker(WorldPosition initialPosition, long timeout) {
            this.initialPosition = initialPosition;
            this.timeout = timeout;
            this.lastMovementTime = System.currentTimeMillis();
        }

        public boolean hasTimedOut(WorldPosition currentPosition) {
            if (!currentPosition.equalsPrecisely(this.initialPosition)) {
                lastMovementTime = System.currentTimeMillis();
                initialPosition = currentPosition;
                return false;
            }
            return System.currentTimeMillis() - lastMovementTime > timeout;
        }

    }

    public static class Patch {
        public static final int MAX_STAGE = 3;
        private final WorldPosition position;
        private final PatchManager.PatchRow row;
        private long plantTime = -1;
        private boolean isFertilised;
        private boolean isHarvested;
        private boolean isDead;
        private long lastWateredTime = -1;

        public Patch(WorldPosition position, PatchManager.PatchRow row) {
            this.position = position;
            this.row = row;
        }

        public PatchManager.PatchRow getRow() {
            return row;
        }

        public boolean isDead() {
            return isDead;
        }

        public void setDead(boolean dead) {
            isDead = dead;
        }

        public boolean isHarvested() {
            return isHarvested;
        }

        public void setHarvested(boolean harvested) {
            isHarvested = harvested;
        }

        public WorldPosition getPosition() {
            return position;
        }

        public boolean isFertilised() {
            return isFertilised;
        }

        public void setFertilised(boolean fertilised) {
            isFertilised = fertilised;
        }

        public long getPlantTime() {
            return plantTime;
        }

        public void setPlantTime(long plantTime) {
            this.plantTime = plantTime;
        }

        public long getLastWateredTime() {
            return lastWateredTime;
        }

        public void setLastWateredTime(long lastWateredTime) {
            this.lastWateredTime = lastWateredTime;
        }

        public boolean isWatered() {
            if (plantTime == -1 || lastWateredTime == -1) {
                return false; // Not planted yet
            }
            // get stage for time, stages increment every 60 seconds from the initial plant time
            int currentStage = getCurrentStage();
            int wateredStage = getWaterStage();
            return wateredStage >= currentStage;
        }

        public int getWaterStage() {
            if (plantTime == -1 || lastWateredTime == -1) {
                return 0; // Not planted yet
            }
            // get stage for time, stages increment every 60 seconds from the initial plant time
            return (int) Math.min(MAX_STAGE, (getLastWateredTime() - plantTime) / TimeUnit.MINUTES.toMillis(1)) + 1;
        }

        public int getCurrentStage() {
            if (plantTime == -1) {
                return 0; // Not planted yet
            }
            // get stage for time, stages increment every 60 seconds from the initial plant time
            return (int) Math.min(MAX_STAGE, (System.currentTimeMillis() - plantTime) / TimeUnit.MINUTES.toMillis(1)) + 1;
        }

        @Override
        public String toString() {
            return "Patch{" +
                    "position=" + position +
                    ", row=" + row +
                    '}';
        }
    }
}
