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
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResultList;
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
    public static final int[] LOGS = new int[]{ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS, ItemID.MAPLE_LOGS, ItemID.TEAK_LOGS, ItemID.ARCTIC_PINE_LOGS, ItemID.MAPLE_LOGS, ItemID.MAHOGANY_LOGS, ItemID.YEW_LOGS, ItemID.BLISTERWOOD_LOGS, ItemID.MAGIC_LOGS, ItemID.REDWOOD_LOGS};
    private int amountChangeTimeout;
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.TINDERBOX));
    private static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();
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
    private boolean forceNewPosition = false;
    private ItemGroupResult inventorySnapshot;
    private int tries = 0;
    private Map<Integer, String> logNames = new HashMap<>();

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
        for (int logID : LOGS) {
            String name = getItemManager().getItemName(logID);
            if (name != null) {
                logNames.put(logID, name);
            }
        }
        amountChangeTimeout = random(6200, 9000);
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
            if (inventorySnapshot.getSelectedSlot().isPresent()) {
                // unselect item if selected
                if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
                    return 0;
                }
            }
            log(getClass().getSimpleName(), "No bonfire active, we need to light a bonfire");
            // walk to target position, if one is valid
            if (forceNewPosition) {
                log(getClass().getSimpleName(), "Moving to new light position...");
                moveToNewPosition();
            } else {
                log(getClass().getSimpleName(), "Lighting bonfire...");
                lightBonfire(inventorySnapshot.getItem(ItemID.TINDERBOX), inventorySnapshot.getRandomItem(selectedLogsID));
                listenChatbox();
            }
        } else {
            log(getClass().getSimpleName(), "Bonfire active");
            burnLogsOnBonfire(inventorySnapshot);
            listenChatbox();
        }
        return 0;
    }

    private void hopWorlds() {
        if (!getProfileManager().hasHopProfile()) {
            log(BonfireMaker.class, "No hop profile set, please make sure to select a hop profile when running this script.");
            stop();
            return;
        }
        getProfileManager().forceHop();
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
            List<LocalPosition> nearbyPositions = getWalker().getCollisionManager().findReachableTiles(myPos, 15);
            // remove tiles < 7 away, leaving tiles only 7-10 tiles away
            nearbyPositions.removeIf(localPosition -> myPos.distanceTo(localPosition) < 13);
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

    private void listenChatbox() {
        if (getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            getWidgetManager().getChatbox().openFilterTab(ChatboxFilterTab.GAME);
            return;
        }
        UIResultList<String> textLines = getWidgetManager().getChatbox().getText();
        if (textLines.isNotVisible()) {
            log("Chatbox not visible");
            return;
        }
        List<String> currentLines = textLines.asList();
        if (currentLines.isEmpty()) {
            return;
        }
        int firstDifference = 0;
        if (!PREVIOUS_CHATBOX_LINES.isEmpty()) {
            if (currentLines.equals(PREVIOUS_CHATBOX_LINES)) {
                return;
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
        onNewChatBoxMessage(newLines);
    }

    private void onNewChatBoxMessage(List<String> newLines) {
        for (String line : newLines) {
            line = line.toLowerCase();
            log("Chatbox listener", "New line: " + line);
            if (line.endsWith("further away.")) {
                hopWorlds();
                bonfirePosition = null;
            } else if (line.endsWith("light a fire here.")) {
                forceNewPosition = true;
                bonfirePosition = null;
            }
        }
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
        MenuHook menuHook = menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                String rawText = entry.getRawText();
                log(BonfireMaker.class, "Menu text: " + rawText);
                for (int logID : LOGS) {
                    String logName = logNames.get(logID);
                    if (logName == null) {
                        log(BonfireMaker.class, "Log name not found for ID: " + logID);
                        continue;
                    }
                    log("Log name: " + logName);
                    if (rawText.equalsIgnoreCase("Use " + logName + " -> fire") || rawText.equalsIgnoreCase("Use " + logName + " -> Forester's Campfire")) {
                        if (logID != selectedLogsID) {
                            log(BonfireMaker.class, "Selected log is not the same as the one we are trying to burn, stopping script.");
                            stop();
                            return null;
                        }
                        return entry;
                    }
                }
            }
            return null;
        };
        // resize the poly to minimise missclicks
        if (!getFinger().tapGameScreen(tilePoly, menuHook)) {
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
        return submitHumanTask(() -> {
            listenChatbox();
            return getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION;
        }, random(5000,8000));
    }

    private boolean failed() {
        if (tries > 2) {
            bonfirePosition = null;
            return true;
        }
        tries++;
        return false;
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

            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                amountChangeTimeout = random(6200, 9000);
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
        }, 160000, false, true);
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

    @Override
    public boolean canHopWorlds() {
        return false;
    }
}