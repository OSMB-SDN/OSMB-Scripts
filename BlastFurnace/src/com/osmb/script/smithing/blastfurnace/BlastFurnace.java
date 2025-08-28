package com.osmb.script.smithing.blastfurnace;

import com.osmb.api.ScriptCore;
import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.javafx.ColorPickerPanel;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.trackers.experiencetracker.XPTracker;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.tabs.SettingsTabComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.ChannelThresholdComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.smithing.blastfurnace.component.Overlay;
import com.osmb.script.smithing.blastfurnace.data.Bar;
import com.osmb.script.smithing.blastfurnace.data.Ore;
import com.osmb.script.smithing.blastfurnace.javafx.ScriptOptions;
import com.osmb.script.smithing.blastfurnace.utility.Utils;
import com.osmb.script.smithing.blastfurnace.utility.XPTracking;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.osmb.script.smithing.blastfurnace.Constants.*;
import static com.osmb.script.smithing.blastfurnace.Options.*;
import static com.osmb.script.smithing.blastfurnace.Status.*;
import static com.osmb.script.smithing.blastfurnace.component.Overlay.SECTIONS;

@ScriptDefinition(name = "Blast Furnace", description = "A script for the Blast Furnace minigame.", version = 1.0, author = "Joe", skillCategory = SkillCategory.SMITHING)
public class BlastFurnace extends Script {
    private final Overlay overlay;
    private final BankHandler bankHandler;
    private final XPTracking xpTracking;
    private Overlay.BlastFurnaceInfo blastFurnaceInfo;

    public BlastFurnace(Object scriptCore) {
        super(scriptCore);
        this.overlay = new Overlay(this);
        this.bankHandler = new BankHandler(this);
        this.xpTracking = new XPTracking(this);
    }

    public static void waitForConveyorBeltInteraction(ScriptCore core, ItemGroupResult inventorySnapshot, RSObject conveyorBelt, Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        WorldPosition worldPosition = core.getWorldPosition();
        if (worldPosition == null) {
            core.log(BlastFurnace.class, "Failed to get world position after interacting with conveyor belt.");
            return;
        }
        if (conveyorBelt.distance(worldPosition) > 1) {
            core.log(BlastFurnace.class, "Waiting until we start moving to conveyor belt...");
            // wait until we start moving
            boolean moving = core.pollFramesUntil(() -> core.getLastPositionChangeMillis() < 300, RandomUtils.uniformRandom(1000, 2500));
            if (!moving) {
                core.log(BlastFurnace.class, "Looks like we didn't start moving after interacting.");
                return;
            }
        }
        core.log(BlastFurnace.class, "Waiting for inventory change...");
        AtomicReference<ItemGroupResult> futureInventorySnapshot = new AtomicReference<>();
        AtomicBoolean inventoryChanged = new AtomicBoolean(false);
        core.pollFramesHuman(() -> {
            WorldPosition myPos = core.getWorldPosition();
            if (myPos == null) {
                core.log(BlastFurnace.class, "Failed to get world position.");
                return true;
            }
            futureInventorySnapshot.set(core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE));
            if (futureInventorySnapshot.get() == null) {
                core.log(BlastFurnace.class, "Unable to snapshot inventory after interacting with conveyor belt");
                return false;
            }

            // equip goldsmith gauntlets if we are smelting gold bars & have them in the inventory
            ItemSearchResult goldsmithGauntlets = futureInventorySnapshot.get().getItem(ItemID.GOLDSMITH_GAUNTLETS);
            if (selectedBar == Bar.GOLD && goldsmithGauntlets != null && (GOLD_GLOVE_EQUIP_DELAY.hasFinished() || conveyorBelt.getTileDistance(myPos) < 6)) {
                equipGoldsmithGuantlets(core, goldsmithGauntlets, false);
                return true;
            }

            // break out when the inventory snapshot has changed
            if (!futureInventorySnapshot.get().equals(inventorySnapshot)) {
                inventoryChanged.set(true);
                return true;
            }
            return false;
        }, RandomUtils.uniformRandom(10000, 18000));
        if (inventoryChanged.get()) {
            core.log(BlastFurnace.class, "Successfully put ores on conveyor belt.");
            // add ores to processing map
            int coalDeposited = blastFurnaceInfo.getOreAmount(Ore.COAL);
            for (Ore ore : Ore.values()) {
                if (ore == Ore.COAL) {
                    continue;
                }
                core.log(BlastFurnace.class, "Checking ore: " + ore.getOreName(core));
                int initialAmount = inventorySnapshot.getAmount(ore.getItemID());
                int newAmount = futureInventorySnapshot.get().getAmount(ore.getItemID());
                int oresDeposited = initialAmount - newAmount;
                if (oresDeposited > 0) {
                    Bar bar = Bar.getBarForOre(ore);
                    if (coalDeposited >= bar.getCoalAmount()) {
                        expectBarsToBeCollected = true;
                        break;
                    }
                }
            }
            // update melting pot map
            MELTING_POT_BARS.clear();
            MELTING_POT_BARS.putAll(Utils.getMeltingPotBars(blastFurnaceInfo));
        } else {
            core.log(BlastFurnace.class, "Failed to put ores on conveyor belt.");
        }
    }

    public static boolean equipGoldsmithGuantlets(ScriptCore core, ItemSearchResult gloves, boolean longTap) {
        boolean interactedSuccessfully = longTap ? gloves.interact("wear") : gloves.interact();
        if (interactedSuccessfully) {
            core.pollFramesHuman(() -> {
                ItemGroupResult futureInventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                return futureInventorySnapshot != null && futureInventorySnapshot.containsAny(ItemID.ICE_GLOVES, ItemID.SMITHS_GLOVES_I);
            }, RandomUtils.uniformRandom(1200, 1800));
            return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        // show ui
        ScriptOptions scriptOptions = new ScriptOptions(this);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Blast Furnace Configuration", false);
        // set configs
        drinkStaminas = scriptOptions.shouldUseStaminaPotion();
        selectedBar = scriptOptions.getSelectedProduct();
        payForeman = scriptOptions.shouldPayForeman();
        coolingMethod = scriptOptions.getCoolingMethod();

        if (selectedBar == Bar.GOLD) {
            ITEMS_TO_NOT_DEPOSIT.add(ItemID.GOLDSMITH_GAUNTLETS);
        }

        for (Bar bar : Bar.values()) {
            BAR_IDS.add(bar.getBarID());
            getItemManager().overrideDefaultComparator(bar.getBarID(), new ChannelThresholdComparator(9, 7, 7));
            for (Resource resource : bar.getOres()) {
                ORE_IDS.add(resource.getItemID());
            }
        }

        for (int itemID : ORE_IDS) {
            getItemManager().overrideDefaultColorModel(itemID, ColorModel.HSL);
            getItemManager().overrideDefaultComparator(itemID, new ChannelThresholdComparator(9, 7, 7));
        }

        ITEM_IDS_TO_RECOGNISE.addAll(ORE_IDS);
        ITEM_IDS_TO_RECOGNISE.addAll(BAR_IDS);
        ITEM_IDS_TO_RECOGNISE.addAll(STAMINA_POTION_IDS);
    }

    @Override
    public int poll() {
        if (defaultHighlightPixel == null) {
            int color = ColorPickerPanel.show(this, "Pick color for Foreman highlight");
            if (color == 0) {
                log(BlastFurnace.class, "No color selected for foreman, stopping script.");
                stop();
                return 0;
            }
            defaultHighlightPixel = new SearchablePixel(color, new SingleThresholdComparator(5), ColorModel.RGB);
        }

        // check zoom level is within range
        if (!setZoom) {
            setZoom();
            return 0;
        }
        // make sure xp drops is active
        if (!xpTracking.checkXPCounterActive()) {
            return 0;
        }

        if (getWidgetManager().getBank().isVisible()) {
            log(BlastFurnace.class, "Handling bank interface...");
            if (blastFurnaceInfo == null) {
                // close bank if we don't have the info
                getWidgetManager().getBank().close();
                return 0;
            }
            bankHandler.handleBank(blastFurnaceInfo);
        } else {
            WorldPosition worldPosition = getWorldPosition();
            if (worldPosition == null) {
                log(BlastFurnace.class, "Failed to get world position.");
                return 0;
            }
            ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                log(BlastFurnace.class, "Unable to snapshot inventory");
                return 0;
            }

            blastFurnaceInfo = (Overlay.BlastFurnaceInfo) overlay.getValue(SECTIONS);
            if (blastFurnaceInfo == null) {
                log(BlastFurnace.class, "Unable to get blast furnace info from overlay");
                return 0;
            }

            if (blastFurnaceInfo.getCofferValue() == 0) {
                log(BlastFurnace.class, "Coffer is empty, stopping script.");
                stop();
                return 0;
            }

            if (payForeman && FOREMAN_PAYMENT_TIMER.hasFinished()) {
                log(BlastFurnace.class, "Need to pay foreman.");
                // pay the foreman if we have enough coins
                int coins = inventorySnapshot.getAmount(ItemID.COINS_995);
                log(BlastFurnace.class, "Coins: " + coins);
                if (coins >= 2500) {
                    log(BlastFurnace.class, "Paying foreman...");
                    payForeman();
                } else {
                    log(BlastFurnace.class, "Open bank to withdraw coins for foreman payment.");
                    openBank();
                }
                return 0;
            }

            boolean hasBarsToCollect = Utils.hasBarsToCollect(blastFurnaceInfo);
            if (canEmptyCoalBag(worldPosition, inventorySnapshot, blastFurnaceInfo)) {
                log(BlastFurnace.class, "Emptying coal bag...");
                handleConveyorBelt(inventorySnapshot, blastFurnaceInfo);
            } else if (hasBarsToCollect && !inventorySnapshot.isFull()) {
                // collect bars
                log(BlastFurnace.class, "Collecting bars from dispenser...");
                handleBarDispenser(inventorySnapshot, blastFurnaceInfo);
                return 0;
            } else if (expectBarsToBeCollected) {
                log(BlastFurnace.class, "Waiting for processing ores...");
                waitForProcessingOres(blastFurnaceInfo, worldPosition);
            } else if (!hasBarsToCollect && shouldInteractConveyorBelt(worldPosition, selectedBar, inventorySnapshot, blastFurnaceInfo)) {
                log(BlastFurnace.class, "Handling conveyor belt interaction...");
                handleConveyorBelt(inventorySnapshot, blastFurnaceInfo);
            } else {
                log(BlastFurnace.class, "Opening the bank...");
                openBank();
            }

        }
        return 0;
    }

    private void setZoom() {
        log(BlastFurnace.class, "Checking zoom level...");
        // check if the settings tab + display sub-tab is open, if not, open it
        if (!getWidgetManager().getSettings().openSubTab(SettingsTabComponent.SettingsSubTabType.DISPLAY_TAB)) {
            return;
        }
        UIResult<Integer> zoomResult = getWidgetManager().getSettings().getZoomLevel();
        if (!zoomResult.isFound()) {
            // shouldn't happen unless we fail to change the sub tab in the settings tab
            log(BlastFurnace.class, "No zoom level found!");
            return;
        }
        int currentZoom = zoomResult.get();
        if (currentZoom > MAX_ZOOM) {
            // generate random zoom level between MIN_ZOOM and MAX_ZOOM
            int zoomLevel = random(MIN_ZOOM, MAX_ZOOM);
            if (getWidgetManager().getSettings().setZoomLevel(zoomLevel)) {
                log(BlastFurnace.class, "Zoom level set to: " + zoomLevel);
                // zoom level set, set flag to true
                Status.setZoom = true;
            }
        } else {
            // zoom level is already inside our desired range
            Status.setZoom = true;
        }
    }

    private void waitForProcessingOres(Overlay.BlastFurnaceInfo blastFurnaceInfo, WorldPosition worldPosition) {
        Overlay.BlastFurnaceInfo.CollectionStatus collectionStatus = blastFurnaceInfo.getCollectionStatus();
        log("Collection status: " + collectionStatus);
        if (collectionStatus != Overlay.BlastFurnaceInfo.CollectionStatus.NOT_READY) {
            log(BlastFurnace.class, "Collection is ready, collecting bars...");
            expectBarsToBeCollected = false;
        }
        if (meltingPotBarsChanged(blastFurnaceInfo)) {
            log(BlastFurnace.class, "Melting pot bars have changed, waiting for collection...");
            expectBarsToBeCollected = false;
            MELTING_POT_BARS.clear();
            ICE_GLOVE_EQUIP_DELAY.reset(RandomUtils.gaussianRandom(0, 1700, 500, 500));
        } else {
            RSObject barDispenser = getObjectManager().getClosestObject("Bar dispenser");
            if (barDispenser == null) {
                log(BlastFurnace.class, "No bar dispenser found, be sure to run the script inside the blast furnace area.");
                stop();
                return;
            }
            if (barDispenser.distance(worldPosition) > 2) {
                // walk to dispenser
                WalkConfig.Builder builder = new WalkConfig.Builder();
                builder.breakCondition(() -> {
                    ItemGroupResult inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                    if (inventorySnapshot == null) {
                        log(BlastFurnace.class, "Unable to snapshot inventory while waiting for processing ores.");
                        return true;
                    }
                    if (meltingPotBarsChanged(blastFurnaceInfo)) {
                        log(BlastFurnace.class, "Melting pot bars have changed, waiting for collection...");
                        expectBarsToBeCollected = false;
                        MELTING_POT_BARS.clear();
                        ICE_GLOVE_EQUIP_DELAY.reset(RandomUtils.gaussianRandom(0, 1700, 500, 500));
                        return true;
                    }
                    Overlay.BlastFurnaceInfo blastFurnaceInfo_ = (Overlay.BlastFurnaceInfo) overlay.getValue(SECTIONS);
                    log(BlastFurnace.class, "Collection status: " + blastFurnaceInfo_.getCollectionStatus());
                    if (blastFurnaceInfo_.getCollectionStatus() != Overlay.BlastFurnaceInfo.CollectionStatus.NOT_READY) {
                        // human delay (prevents breaking out of run method & immediately interacting with dispenser)
                        ICE_GLOVE_EQUIP_DELAY.reset(RandomUtils.gaussianRandom(0, 1700, 500, 500));
                        return true;
                    }
                    WorldPosition myPos = getWorldPosition();
                    return DISPENSER_AREA.contains(myPos);
                });
                getWalker().walkTo(DISPENSER_AREA.getRandomPosition(), builder.build());
            }
        }
    }

    private boolean meltingPotBarsChanged(Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        Map<Bar, Integer> barMap = Utils.getMeltingPotBars(blastFurnaceInfo);
        return !barMap.equals(MELTING_POT_BARS);
    }

    private boolean canEmptyCoalBag(WorldPosition worldPosition, ItemGroupResult inventorySnapshot, Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        RSObject conveyorBelt = Utils.getConveyorBelt(this);
        if (conveyorBelt == null) {
            log(BlastFurnace.class, "No conveyor belt found, be sure to run the script inside the blast furnace area.");
            stop();
            return false;
        }
        int coalDeposited = blastFurnaceInfo.getOreAmount(Ore.COAL);

        if (conveyorBelt.distance(worldPosition) > 2) {
            return false;
        }
        return !inventorySnapshot.containsAny(ORE_IDS) && coalBagFull || inventorySnapshot.contains(ItemID.COAL) && coalDeposited < MAX_COAL_AMOUNT;
    }

    private void payForeman() {
        DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.TEXT_OPTION) {
            if (handleForemanDialogue()) {
                return;
            }
        }
        UIResultList<WorldPosition> npcPositions = getWidgetManager().getMinimap().getNPCPositions();
        if (npcPositions.isNotVisible()) {
            log(BlastFurnace.class, "Minimap not visible.");
            return;
        }
        boolean containsNPC = false;
        for (WorldPosition npcPosition : npcPositions) {
            if (!FOREMAN_AREA.contains(npcPosition)) {
                continue;
            }
            containsNPC = true;
            Polygon tileCube = getSceneProjector().getTileCube(npcPosition, 130);
            if (tileCube == null || (tileCube = tileCube.getResized(0.6)) == null || getWidgetManager().insideGameScreenFactor(tileCube, Collections.emptyList()) < 0.5) {
                // cube not on screen
                continue;
            }
            Rectangle bounds = getPixelAnalyzer().getHighlightBounds(tileCube, YELLOW_SELECT_HIGHLIGHT_PIXEL, defaultHighlightPixel);
            if (bounds == null) {
                continue;
            }
            // found highlighted foreman
            log(BlastFurnace.class, "Found highlighted foreman at: " + npcPosition);
            if (getFinger().tap(tileCube, "pay")) {
                // wait for dialogue
                pollFramesHuman(() -> getWidgetManager().getDialogue().getDialogueType() == DialogueType.TEXT_OPTION, RandomUtils.uniformRandom(4000, 8000));
                return;
            }
        }
        if (containsNPC) {
            // can't find highlighted foreman but an npc is in the foremans area, so lets walk there to get a better view
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakCondition(() -> {
                WorldPosition worldPosition = getWorldPosition();
                if (worldPosition == null) {
                    log(BlastFurnace.class, "Failed to get world position while trying to walk to foreman area.");
                    return true;
                }
                return FOREMAN_AREA.contains(worldPosition);
            });
            getWalker().walkTo(FOREMAN_AREA.getRandomPosition());
        }
    }

    private boolean handleForemanDialogue() {
        UIResult<String> dialogueTitle = getWidgetManager().getDialogue().getDialogueTitle();
        if (dialogueTitle.isNotVisible()) {
            return true;
        }
        if (dialogueTitle.get().startsWith("Pay 2,500 coins")) {
            if (getWidgetManager().getDialogue().selectOption("Yes")) {
                boolean result = pollFramesHuman(() -> {
                    if (getWidgetManager().getDialogue().getDialogueType() != DialogueType.CHAT_DIALOGUE) {
                        return false;
                    }
                    UIResult<String> dialogueText = getWidgetManager().getDialogue().getText();
                    if (dialogueText.isNotVisible()) {
                        return false;
                    }
                    return dialogueText.get().startsWith("Okay, you can use the furnace");
                }, RandomUtils.uniformRandom(3000, 5000));
                if (result) {
                    // add random offset so we can allow to be prompted with the dialogue sometimes
                    long randomOffset = RandomUtils.uniformRandom(5000, 120000);
                    FOREMAN_PAYMENT_TIMER.reset(TimeUnit.MINUTES.toMillis(9) + randomOffset);
                }
            } else {
                log(BlastFurnace.class, "Failed to select 'yes' option in foreman payment dialogue.");
            }
            return true;
        }
        return false;
    }

    private void handleConveyorBelt(ItemGroupResult inventorySnapshot, Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        RSObject conveyorBelt = Utils.getConveyorBelt(this);
        if (conveyorBelt == null) {
            log(BlastFurnace.class, "No conveyor belt found, be sure to run the script inside the blast furnace area.");
            stop();
            return;
        }
        if (!inventorySnapshot.containsAny(ORE_IDS) && coalBagFull) {
            // empty coal bag if it's full and inventory doesn't contain any ores
            emptyCoalBag(inventorySnapshot);
            return;
        }

        if (selectedBar == Bar.GOLD) {
            int randomChance = RandomUtils.uniformRandom(0, 2);
            ItemSearchResult goldSmithGauntlets = inventorySnapshot.getItem(ItemID.GOLDSMITH_GAUNTLETS);
            if (randomChance == 0 && goldSmithGauntlets != null) {
                equipGoldsmithGuantlets(this, goldSmithGauntlets, false);
            }
        }
        if (conveyorBelt.interact("put-ore-on")) {
            waitForConveyorBeltInteraction(this, inventorySnapshot, conveyorBelt, blastFurnaceInfo);
        }
    }

    private void emptyCoalBag(ItemGroupResult inventorySnapshot) {
        log(BlastFurnace.class, "Emptying coal bag...");
        ItemSearchResult coalBag = inventorySnapshot.getItem(ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG);
        if (coalBag == null) {
            log(BlastFurnace.class, "Unable to find coal bag in inventory snapshot");
            return;
        }
        MenuHook coalBagHook = menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                if (entry.getRawText().startsWith("empty")) {
                    return entry;
                }
            }
            return null;
        };
        if (getFinger().tap(false, coalBag, coalBagHook)) {
            boolean emptied = pollFramesHuman(() -> {
                log(BlastFurnace.class, "Waiting for coal bag to be emptied...");
                ItemGroupResult currentInventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (currentInventorySnapshot == null) {
                    log(BlastFurnace.class, "Unable to snapshot inventory after emptying coal bag");
                    return false;
                }
                // return true if the inventory snapshot has changed
                return currentInventorySnapshot.contains(ItemID.COAL);
            }, RandomUtils.uniformRandom(1500, 2500));
            if (emptied) {
                log(BlastFurnace.class, "Coal bag has been emptied successfully.");
                coalBagFull = false;
            } else {
                log(BlastFurnace.class, "Failed to empty coal bag, looks like it was already empty.");
                coalBagFull = false;
            }
        }
    }


    private void handleBarDispenser(ItemGroupResult inventorySnapshot, Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            if (getWidgetManager().getDialogue().selectItem(BAR_IDS)) {
                pollFramesUntil(() -> {
                    // wait for bar amount to decrement in overlay
                    Overlay.BlastFurnaceInfo info = (Overlay.BlastFurnaceInfo) overlay.getValue(SECTIONS);
                    for (Bar bar : Bar.values()) {
                        if (info.getBarAmount(bar) < blastFurnaceInfo.getBarAmount(bar)) {
                            log(BlastFurnace.class, bar.getBarName(this) + " decremented");
                            return true;
                        }
                    }
                    return false;
                }, RandomUtils.uniformRandom(2000, 4000));

            } else {
                log(BlastFurnace.class, "Failed selecting bar from dialogue.");
            }
        } else {
            if (ICE_GLOVE_EQUIP_IGNORE_DELAY.hasFinished() && blastFurnaceInfo.getCollectionStatus() == Overlay.BlastFurnaceInfo.CollectionStatus.NEEDS_COOLING) {
                if (!ICE_GLOVE_EQUIP_DELAY.hasFinished()) {
                    return;
                }
                ItemSearchResult iceGloves = inventorySnapshot.getItem(ItemID.ICE_GLOVES, ItemID.SMITHS_GLOVES_I);
                if (iceGloves != null) {
                    log(BlastFurnace.class, "Equipping ice gloves...");
                    if (iceGloves.interact()) {
                        // this is so we simply tap & then
                        ICE_GLOVE_EQUIP_IGNORE_DELAY.reset(RandomUtils.uniformRandom(2000, 4000));
                        BAR_DISPENSER_INTERACTION_DELAY.reset(RandomUtils.gaussianRandom(100, 2000, 300, 600));
                        return;
                    }
                }
                log(BlastFurnace.class, "Waiting for dispenser to cool down...");
                return;
            }
            if (!BAR_DISPENSER_INTERACTION_DELAY.hasFinished()) {
                log(BlastFurnace.class, "Waiting for melting pot interaction delay...");
                return;
            }
            RSObject barDispenser = getObjectManager().getClosestObject("Bar dispenser");
            if (barDispenser == null) {
                log(BlastFurnace.class, "No bar dispenser found, be sure to run the script inside the blast furnace area.");
                stop();
                return;
            }
            if (barDispenser.interact("take")) {
                WorldPosition worldPosition = getWorldPosition();
                if (worldPosition == null) {
                    log(BlastFurnace.class, "Failed to get world position after interacting with bar dispenser.");
                    return;
                }
                if (barDispenser.distance(worldPosition) > 1) {
                    // wait until we start moving
                    boolean moving = pollFramesUntil(() -> getLastPositionChangeMillis() < 300, RandomUtils.uniformRandom(1000, 2500));
                    if (!moving) {
                        log(BlastFurnace.class, "Looks like we didn't start moving after interacting.");
                        return;
                    }
                }
                pollFramesHuman(() -> getWidgetManager().getDialogue().isVisible(), RandomUtils.uniformRandom(6000, 9000));
            }
        }
    }


    private void openBank() {
        RSObject bankChest = getObjectManager().getClosestObject("Bank chest");
        if (bankChest == null) {
            log(BlastFurnace.class, "No bank chest found, be sure to run the script inside the blast furnace area.");
            stop();
            return;
        }
        if (bankChest.interact(() -> {
            if (getWidgetManager().getBank().isVisible()) {
                return true;
            }
            Overlay.BlastFurnaceInfo blastFurnaceInfo = (Overlay.BlastFurnaceInfo) overlay.getValue(SECTIONS);
            if (blastFurnaceInfo != null) {
                this.blastFurnaceInfo = blastFurnaceInfo;
            } else {
                log(BlastFurnace.class, "Unable to get blast furnace info from overlay");
            }
            return false;
        }, "use")) {
            int positionChangeTimeout = RandomUtils.uniformRandom(1200, 2000);
            pollFramesHuman(() -> {
                WorldPosition worldPosition = getWorldPosition();
                if (worldPosition == null) {
                    log(BlastFurnace.class, "Failed to get world position after interacting with bank chest.");
                    return true;
                }
                if (getWidgetManager().getBank().isVisible()) {
                    return true;
                }
                if (bankChest.getTileDistance(worldPosition) > 2 && getLastPositionChangeMillis() > positionChangeTimeout) {
                    // break out if we stop moving
                    return true;
                } else if (bankChest.getTileDistance(worldPosition) > 1 && getLastPositionChangeMillis() > positionChangeTimeout + 1000) {
                    return true;
                }

                Overlay.BlastFurnaceInfo blastFurnaceInfo = (Overlay.BlastFurnaceInfo) overlay.getValue(SECTIONS);
                if (blastFurnaceInfo != null) {
                    this.blastFurnaceInfo = blastFurnaceInfo;
                } else {
                    log(BlastFurnace.class, "Unable to get blast furnace info from overlay");
                }
                return false;
            }, RandomUtils.uniformRandom(14000, 19000));
        }
    }

    private boolean shouldInteractConveyorBelt(WorldPosition worldPosition, Bar selectedBar, ItemGroupResult inventorySnapshot, Overlay.BlastFurnaceInfo blastFurnaceInfo) {
        Set<Integer> itemsToIgnore = new HashSet<>(ORE_IDS);
        itemsToIgnore.addAll(BAR_IDS);

        hasCoalBag = inventorySnapshot.getItem(ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG) != null;
        int oreCapacity = inventorySnapshot.getFreeSlots(itemsToIgnore);
        int coalGoal = selectedBar.getCoalAmount() * oreCapacity;
        int coalDeposited = blastFurnaceInfo.getOreAmount(Ore.COAL);

        if (hasCoalBag && !coalBagFull) {
            return false;
        }

        RSObject conveyorBelt = Utils.getConveyorBelt(this);
        if (conveyorBelt == null) {
            log(BlastFurnace.class, "No conveyor belt found, be sure to run the script inside the blast furnace area.");
            stop();
            return false;
        }
        // if we're near the conveyor belt and we have a full coal bag, we can put ores on it
        if (conveyorBelt.getTileDistance(worldPosition) < 3) {
            if (coalBagFull || inventorySnapshot.contains(ItemID.COAL) && coalDeposited < MAX_COAL_AMOUNT) {
                return true;
            }
        }
        if (coalGoal == 0 || coalDeposited >= coalGoal) {
            // check if we have enough primary ores
            List<Resource> resourceList = selectedBar.getOres();
            int amount = oreCapacity / resourceList.size();
            for (Resource resource : resourceList) {
                int resourceAmount = inventorySnapshot.getAmount(resource.getItemID());
                String itemName = getItemManager().getItemName(resource.getItemID());
                if (resourceAmount < amount) {
                    log(BlastFurnace.class, "Not enough " + itemName + " in inventory. Required: " + amount + ", Found: " + resourceAmount);
                    return false;
                } else if (resourceList.size() > 1 && resourceAmount > amount) {
                    log(BlastFurnace.class, "Too much " + itemName + " in inventory. Required: " + amount + ", Found: " + resourceAmount);
                    return false;
                }
            }
            log(BlastFurnace.class, "We have enough primary ores to put on conveyor belt.");
            return true;
        } else {
            // we need to deposit coal (coal bag if we have one also)
            log(BlastFurnace.class, "Coal amount = " + inventorySnapshot.getAmount(ItemID.COAL) + ", Ore capacity = " + oreCapacity);
            int coalAmount = inventorySnapshot.getAmount(ItemID.COAL);
            return coalAmount > 0 && coalAmount == oreCapacity;
        }
    }


    @Override
    public void onPaint(Canvas c) {
        FontMetrics metrics = c.getFontMetrics(ARIAL);
        int padding = 5;

        List<String> lines = new ArrayList<>();
        if (this.xpTracking != null) {
            lines.add("---- XP Info ----");
            XPTracker xpTracker = xpTracking.getXpTracker();
            if (xpTracker != null) {
                lines.add("XP Gained: " + xpTracker.getXpGained() + " | XP/hr: " + xpTracker.getXpPerHour());
                lines.add("Experience gained: " + xpTracker.getXpGained());
                lines.add("Time to next level: " + xpTracker.timeToNextLevelString());
            } else {
                lines.add("XP Tracker not initialized");
            }
            lines.add("");
        }
        if (selectedBar != null) {
            lines.add("Selected bar: " + selectedBar.getBarName(this));
        }
        lines.add("Has coal bag: " + hasCoalBag);
        lines.add("Has processing ores: " + expectBarsToBeCollected);
        if (hasCoalBag) {
            lines.add("Filled coal bag: " + coalBagFull);
        }
        if (payForeman) {
            lines.add("Paying foreman in: " + FOREMAN_PAYMENT_TIMER.getRemainingTimeFormatted());
        }

        if (this.blastFurnaceInfo != null) {
            lines.add("");
            lines.add("---- UI info ----");
            lines.add("Coffer: " + this.blastFurnaceInfo.getCofferValue());
            lines.add("Coal deposited: " + this.blastFurnaceInfo.getOreAmount(Ore.COAL));
            lines.add("Collection status: " + this.blastFurnaceInfo.getCollectionStatus());
            for (Bar bar : Bar.values()) {
                lines.add(bar.getBarName(this) + " deposited: " + this.blastFurnaceInfo.getBarAmount(bar));
            }
        }

        // calculate the maximum width and total height of the text
        int maxWidth = 0;
        for (String line : lines) {
            int w = metrics.stringWidth(line);
            if (w > maxWidth) maxWidth = w;
        }
        int totalHeight = metrics.getHeight() * lines.size();
        int drawX = 10;
        // draw the border
        c.fillRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.BLACK.getRGB(), 0.8);
        c.drawRect(drawX - padding, 40, maxWidth + padding * 2, totalHeight + padding * 2, Color.GRAY.getRGB());
        // draw text
        int drawY = 40;
        for (String line : lines) {
            int color = Color.WHITE.getRGB();
            c.drawText(line, drawX, drawY += metrics.getHeight(), color, ARIAL);
        }
    }

    @Override
    public void onNewFrame() {
        xpTracking.checkXP();
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{7757};
    }

    @Override
    public boolean canBreak() {
        return !expectBarsToBeCollected;
    }

    @Override
    public boolean canHopWorlds() {
        return !expectBarsToBeCollected;
    }

    @Override
    public boolean canAFK() {
        return true;
    }

    @Override
    public void onRelog() {
        expectBarsToBeCollected = false;
        FOREMAN_PAYMENT_TIMER.reset(0);
    }


    public enum CoolingMethod {
        ICE_GLOVES(ItemID.ICE_GLOVES),
        BUCKET_OF_WATER(ItemID.BUCKET_OF_WATER);

        private final int itemID;

        CoolingMethod(int itemID) {
            this.itemID = itemID;
        }

        public int getItemID() {
            return itemID;
        }
    }
}
