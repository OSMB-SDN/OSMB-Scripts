package com.osmb.script.bonfiremaker;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.walker.WalkConfig;
import javafx.scene.Scene;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "Bonfire Maker", description = "Makes bonfires and burns logs on them.", skillCategory = SkillCategory.FIREMAKING, version = 1.0, author = "Joe")
public class BonfireMaker extends Script {

    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    private static final int AMOUNT_CHANGE_TIMEOUT_SECONDS = 6;
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.TINDERBOX));
    private final Predicate<RSObject> bankQuery = gameObject -> {
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
    private int selectedLogsID = ItemID.LOGS;
    private WorldPosition bonfirePosition;
    private WorldPosition bonfireTargetCreationPos;
    private int logsBurnt;
    private int logsBurntOnFire;
    private String logName;
    private boolean forceNewPosition = false;
    private ItemGroupResult inventorySnapshot;
    private int tries = 0;
    private MenuHook burnMenuHook;

    public BonfireMaker(Object scriptCore) {
        super(scriptCore);
    }

    @Override
    public void onStart() {
        ScriptOptions ui = new ScriptOptions();
        Scene scene = ui.buildScene(this);
        getStageController().show(scene, "Options", false);
        selectedLogsID = ui.getSelectedLog();
        ITEM_IDS_TO_RECOGNISE.add(selectedLogsID);

        logName = getItemManager().getItemName(selectedLogsID);
        if (logName == null) {
            log(getClass().getSimpleName(), "Could not find name of selected logs...");
            stop();
        }
        burnMenuHook = createBurnMenuHook();
    }

    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            log(getClass().getSimpleName(), "Handling bank interface...");
            handleBank();
            return 0;
        }

        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            return 0;
        }

        // if no logs, open the bank
        if (!inventorySnapshot.contains(selectedLogsID)) {
            if (inventorySnapshot.getSelectedSlot().isPresent()) {
                if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
                    return 0;
                }
            }
            log(getClass().getSimpleName(), "Opening bank");
            openBank();
            return 0;
        }

        // handle dialogue if already visible
        if (isDialogueVisible()) {
            log(getClass().getSimpleName(), "Handling dialogue");
            handleDialogue();
            return 0;
        }

        if (!inventorySnapshot.contains(ItemID.TINDERBOX)) {
            log(getClass().getSimpleName(), "Tinderbox not found, stopping script...");
            stop();
            return 0;
        }

        if (bonfirePosition == null) {
            resetTries();
            if (inventorySnapshot.getSelectedSlot().isPresent()) {
                // unselect item if selected
                if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
                    return 0;
                }
            }
            log(getClass().getSimpleName(), "No bonfire active, we need to light a bonfire");
            // walk to target position, if one is valid
            if (bonfireTargetCreationPos != null) {
                escapeActiveFire();
            } else if (forceNewPosition) {
                log(getClass().getSimpleName(), "Moving to new light position...");
                moveToNewPosition();
            } else {
                log(getClass().getSimpleName(), "Lighting bonfire...");
//                if (!inventorySnapshot.containsAll(Set.of(ItemID.TINDERBOX, selectedLogsID))) {
//                    log(BonfireMaker.class,"Don't have everything to light a fire...");
//                    return 0;
//                }
                lightBonfire(inventorySnapshot.getItem(ItemID.TINDERBOX), inventorySnapshot.getRandomItem(selectedLogsID));
            }
        } else {
            log(getClass().getSimpleName(), "Bonfire active");
            burnLogsOnBonfire(inventorySnapshot);
        }
        return 0;
    }

    private void resetTries() {
        if (tries > 0) {
            tries = 0;
        }
    }

    private void escapeActiveFire() {
        log(getClass().getSimpleName(), "Running to target light position to escape a currently active bonfire...");
        WorldPosition myPos = getWorldPosition();
        if (!bonfireTargetCreationPos.equals(myPos)) {
            WalkConfig.Builder builder = new WalkConfig.Builder();
            builder.breakDistance(0).tileRandomisationRadius(0);
            getWalker().walkTo(bonfireTargetCreationPos, builder.build());
        } else {
            log(getClass().getSimpleName(), "Arrived at target position...");
            bonfireTargetCreationPos = null;
        }
    }

    private void handleBank() {
        if (!getWidgetManager().getBank().depositAll(ITEM_IDS_TO_RECOGNISE)) {
            return;
        }
        inventorySnapshot = getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);

        ItemGroupResult bankSnapshot = getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null || bankSnapshot == null) {
            return;
        }
        if (inventorySnapshot.isFull()) {
            if (bonfirePosition == null) {
                forceNewPosition = true;
            }
            getWidgetManager().getBank().close();
            return;
        }

        if (!bankSnapshot.contains(selectedLogsID)) {
            log(getClass().getSimpleName(), "Ran out of logs, stopping script...");
            stop();
            return;
        }
        getWidgetManager().getBank().withdraw(selectedLogsID, Integer.MAX_VALUE);
    }

    private void openBank() {
        // Implement banking logic here
        log("Opening bank to withdraw logs...");
        log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it

        List<RSObject> banksFound = getObjectManager().getObjects(bankQuery);
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact(BANK_ACTIONS)) return;
        log(BonfireMaker.class, "Waiting for bank to open...");
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

            return getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 3000;
        }, 15000);
    }

    private boolean isDialogueVisible() {
        return getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION;
    }

    private void handleDialogue() {
        boolean result = getWidgetManager().getDialogue().selectItem(selectedLogsID);
        if (!result) {
            log(getClass().getSimpleName(), "Failed to select item in dialogue.");
            return;
        }
        // Sleep until finished burning
        waitUntilFinishedBurning(selectedLogsID);
    }

    private void lightBonfire(ItemSearchResult tinderbox, ItemSearchResult logs) {
        if (!tinderbox.interact()) {
            return;
        }
        // select random log
        if (!logs.interact()) {
            return;
        }

        final int initialSlots = inventorySnapshot.getFreeSlots();
        boolean lightingFire = submitTask(() -> {
            inventorySnapshot = getWidgetManager().getInventory().search(Collections.emptySet());
            if (inventorySnapshot == null) {
                // inventory not visible, re-poll
                return false;
            }
            // wait until we have more free slots - this tells us the log has been used successfully
            return inventorySnapshot.getFreeSlots() > initialSlots;
        }, 3500);
        WorldPosition lightPosition = getWorldPosition();

        // if we failed to light the fire, usually means we're in a spot you can't make fires
        if (!lightingFire) {
            forceNewPosition = true;
        } else {
            waitForFireToLight(lightPosition);
        }
    }

    private void moveToNewPosition() {
        List<LocalPosition> reachableTiles = getWalker().getCollisionManager().findReachableTiles(getLocalPosition(), 6);
        if (reachableTiles.isEmpty()) {
            log(getClass().getSimpleName(), "No reachable tiles found.");
            return;
        }
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakDistance(1);
        builder.tileRandomisationRadius(1);
        LocalPosition randomPos = reachableTiles.get(random(reachableTiles.size()));
        getWalker().walkTo(randomPos);
        forceNewPosition = false;
    }

    private void waitForFireToLight(WorldPosition lightPosition) {
        log(getClass().getSimpleName(), "Waiting for fire to light...");
        boolean result = submitHumanTask(() -> {
            WorldPosition currentPos = getWorldPosition();
            return currentPos != null && !currentPos.equals(lightPosition);
        }, 14000);

        if (result) {
            log(BonfireMaker.class, "Fire successfully lit!");
            logsBurnt++;
            bonfirePosition = lightPosition;
        }
    }

    private Polygon getBonfireTile() {
        Polygon tilePoly = getSceneProjector().getTilePoly(bonfirePosition, true);
        if (tilePoly == null) {
            return null;
        }
        tilePoly = tilePoly.getResized(0.4);
        if (tilePoly == null || !getWidgetManager().insideGameScreen(tilePoly, Collections.emptyList())) {
            return null;
        }
        return tilePoly;
    }

    private void burnLogsOnBonfire(ItemGroupResult inventorySnapshot) {
        // check if bonfire is active
        Polygon tilePoly = getBonfireTile();
        if (tilePoly == null || !getWidgetManager().insideGameScreen(tilePoly, Collections.emptyList())) {
            log(getClass().getSimpleName(), "Walking to bonfire");
            walkToBonfire();
            return;
        }

        log(getClass().getSimpleName(), "Burning logs on the bonfire...");
        // use log on bonfire
        if (!interactAndWaitForDialogue(inventorySnapshot)) {
            // walk a few tiles away, probably another camp fire close by
            LocalPosition myPos = getLocalPosition();
            List<LocalPosition> nearbyPositions = getWalker().getCollisionManager().findReachableTiles(myPos, 10);
            // remove tiles < 7 away, leaving tiles only 7-10 tiles away
            nearbyPositions.removeIf(localPosition -> myPos.distanceTo(localPosition) < 7);
            LocalPosition posToWalk = nearbyPositions.get(random(nearbyPositions.size()));
            bonfireTargetCreationPos = posToWalk.toWorldPosition(this);
            bonfirePosition = null;
        }
    }

    private void walkToBonfire() {
        // walk to tile
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakDistance(2).tileRandomisationRadius(2);
        builder.breakCondition(() -> getBonfireTile() != null);
        getWalker().walkTo(bonfirePosition, builder.build());
    }

    public boolean interactAndWaitForDialogue(ItemGroupResult inventorySnapshot) {
        ItemSearchResult log = null;

        if (inventorySnapshot.getSelectedSlot().isPresent()) {
            log = getSelectedLog();

            // if item is selected & it isn't a log + if we fail to unselect it, re-poll
            if (log == null && !getWidgetManager().getInventory().unSelectItemIfSelected()) {
                return true;
            }
        }
        if (log == null) {
            log = inventorySnapshot.getRandomItem(selectedLogsID);
        }
        if (!log.interact()) {
            return true;
        }

        Polygon tilePoly = getBonfireTile();
        if (tilePoly == null) {
            return true;
        }

        getScreen().queueCanvasDrawable("highlightFireTile", canvas -> canvas.drawPolygon(tilePoly, Color.GREEN.getRGB(), 1));

        // resize the poly to minimise missclicks
        if (!getFinger().tapGameScreen(tilePoly, "Use " + logName + " -> fire",
                "Use " + logName + " -> Forester's Campfire")) {
            // remove the highlight after failing interacting
            getScreen().removeCanvasDrawable("highlightFireTile");
            // if we failed to interact with the fire, we need to reset the bonfire position
            if (failed()) {
                return false;
            }
            return true;
        }
        // reset tries, as we successfully interacted with the fire
        tries = 0;
        // remove the highlight after interacting
        getScreen().removeCanvasDrawable("highlightFireTile");

        log(getClass().getSimpleName(), "Waiting for dialogue");
        // sleep until dialogue is visible
        return submitHumanTask(() -> getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION, 7000);
    }

    private boolean failed() {
        if (tries > 2) {
            bonfirePosition = null;
            return true;
        }
        tries++;
        return false;
    }

    private MenuHook createBurnMenuHook() {
        return burnMenuHook = menuEntries -> {
            boolean foundAshes = false;
            Set<String> burnEntries = Set.of("Use " + logName + " -> fire", "Use " + logName + " -> Forester's Campfire");
            for (MenuEntry entry : menuEntries) {
                String rawText = entry.getRawText();
                if (burnEntries.contains(rawText)) {
                    return entry;
                }
                if (!foundAshes && rawText.toLowerCase().contains("-> ashes")) {
                    foundAshes = true;
                }
            }
            // if we don't find the burn entries, but we found ashes, then presume fire has gone out.
            if (foundAshes) {
                bonfirePosition = null;
                tries = 0;
            }
            return null;
        };
    }

    private ItemSearchResult getSelectedLog() {
        for (ItemSearchResult recognisedItem : inventorySnapshot.getRecognisedItems()) {
            if (recognisedItem.isSelected()) {
                int selectedItemID = recognisedItem.getId();
                if (selectedItemID == selectedLogsID) {
                    return recognisedItem;
                }
                return null;
            }
        }
        return null;
    }

    public void waitUntilFinishedBurning(int selectedLogsID) {
        Timer amountChangeTimer = new Timer();
        AtomicInteger previousAmount_ = new AtomicInteger(-1);
        submitHumanTask(() -> {
            DialogueType dialogueType = getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    submitTask(() -> false, random(1000, 4000));
                    return true;
                }
            }

            if (amountChangeTimer.timeElapsed() > TimeUnit.SECONDS.toMillis(AMOUNT_CHANGE_TIMEOUT_SECONDS)) {
                // usually happens when the bonfire extinguishes, so we clear our known bonfire position
                bonfirePosition = null;
                return true;
            }
            inventorySnapshot = getWidgetManager().getInventory().search(Set.of(selectedLogsID));
            if (inventorySnapshot == null) {
                return false;
            }
            if (!inventorySnapshot.contains(selectedLogsID)) {
                // no logs left
                return true;
            }

            int amount = inventorySnapshot.getAmount(selectedLogsID);

            int previousAmount = previousAmount_.get();
            if (amount < previousAmount || previousAmount == -1) {
                int diff = Math.abs(amount - previousAmount);
                logsBurntOnFire += diff;
                previousAmount_.set(amount);
                amountChangeTimer.reset();
            }
            return false;
        }, 80000, false, true);
    }

    @Override
    public void onPaint(Canvas c) {
        if (bonfirePosition != null) {
            Polygon polygon = getSceneProjector().getTilePoly(bonfirePosition);
            if (polygon != null) {
                c.fillPolygon(polygon, Color.ORANGE.getRGB(), 0.5);
                c.drawPolygon(polygon, Color.GREEN.getRGB(), 1);
            }
        }
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12598};
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    enum BurnResponse {

    }
}