package com.osmb.script.woodcutting.powerchopburn;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.LocalPosition;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.ChatboxFilterTab;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.api.walker.WalkConfig;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.osmb.script.woodcutting.powerchopburn.ChopBurn.LOGS;
import static com.osmb.script.woodcutting.powerchopburn.ChopBurn.LOG_NAMES;
import static com.osmb.script.woodcutting.powerchopburn.Options.amountChangeTimeout;
import static com.osmb.script.woodcutting.powerchopburn.Status.*;


public class BonfireHandler {

    private final ChopBurn script;


    public BonfireHandler(ChopBurn script) {
        this.script = script;
    }

    public void burnLogs(ItemGroupResult inventorySnapshot) {
        // handle dialogue if already visible
        if (isDialogueVisible()) {
            script.log(BonfireHandler.class, "Handling dialogue");
            handleDialogue();
            return;
        }
        if (bonfirePosition == null) {
            if (inventorySnapshot.getSelectedSlot() != null) {
                // unselect item if selected
                if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
                    return;
                }
            }
            script.log(BonfireHandler.class, "No bonfire active, we need to light a bonfire");
            // walk to target position, if one is valid
            if (forceNewPosition) {
                script.log(BonfireHandler.class, "Moving to new light position...");
                moveToNewPosition();
            } else {
                script.log(BonfireHandler.class, "Lighting bonfire...");
                List<ItemSearchResult> logs = inventorySnapshot.getAllOfItems(LOGS);
                if (logs.isEmpty()) {
                    script.log(BonfireHandler.class, "No logs in inventory found to burn.");
                    return;
                }
                ItemSearchResult randomLog = logs.stream().skip(Utils.random(logs.size())).findFirst().orElse(null);
                lightBonfire(inventorySnapshot.getFreeSlots(), inventorySnapshot.getItem(ItemID.TINDERBOX), randomLog);
                listenChatbox();
            }
        } else {
            script.log(BonfireHandler.class, "Bonfire active");
            burnLogsOnBonfire(inventorySnapshot);
            listenChatbox();
        }
    }

    private void listenChatbox() {
        if (script.getWidgetManager().getDialogue().getDialogueType() == null && script.getWidgetManager().getChatbox().getActiveFilterTab() != ChatboxFilterTab.GAME) {
            script.getWidgetManager().getChatbox().openFilterTab(ChatboxFilterTab.GAME);
            return;
        }
        UIResultList<String> textLines = script.getWidgetManager().getChatbox().getText();
        if (textLines.isNotVisible()) {
            script.log(BonfireHandler.class, "Chatbox not visible");
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
            script.log(BonfireHandler.class, "New line: " + line);
            if (line.endsWith("further away.")) {
                hopWorlds();
                bonfirePosition = null;
            } else if (line.endsWith("light a fire here.")) {
                forceNewPosition = true;
                bonfirePosition = null;
            } else if (line.endsWith("fire has burned out.")) {
                bonfirePosition = null;
            }
        }
    }

    private void hopWorlds() {
        if (!script.getProfileManager().hasHopProfile()) {
            script.log(BonfireHandler.class, "No hop profile set, please make sure to select a hop profile when running this script.");
            script.stop();
            return;
        }
        script.getProfileManager().forceHop();
    }

    private boolean isDialogueVisible() {
        return script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION;
    }

    private void handleDialogue() {
        int[] logIds = LOGS.stream().mapToInt(Integer::intValue).toArray();
        boolean result = script.getWidgetManager().getDialogue().selectItem(logIds);
        if (!result) {
            script.log(BonfireHandler.class, "Failed to select item in dialogue.");
            return;
        }
        // Sleep until finished burning
        waitUntilFinishedBurning();
    }

    public void waitUntilFinishedBurning() {
        com.osmb.api.utils.timing.Timer amountChangeTimer = new Timer();
        AtomicInteger previousAmount_ = new AtomicInteger(-1);
        script.submitHumanTask(() -> {
            DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    script.submitTask(() -> false, Utils.random(1000, 4000));
                    return true;
                }
            }

            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                amountChangeTimeout = Utils.random(6200, 9000);
                // usually happens when the bonfire extinguishes, so we clear our known bonfire position
                bonfirePosition = null;
                return true;
            }
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(LOGS);
            if (inventorySnapshot == null) {
                return false;
            }
            if (!inventorySnapshot.containsAny(LOGS)) {
                // no logs left
                bonfirePosition = null;
                return true;
            }

            int amount = inventorySnapshot.getAmount(LOGS);

            int previousAmount = previousAmount_.get();
            if (amount < previousAmount || previousAmount == -1) {
                int diff = Math.abs(amount - previousAmount);
                logsBurntOnFire += diff;
                previousAmount_.set(amount);
                amountChangeTimer.reset();
            }
            return false;
        }, Utils.random(150000, 300000), false, true);
    }

    private void lightBonfire(int initialFreeSlots, ItemSearchResult tinderbox, ItemSearchResult logs) {
        if (!tinderbox.interact()) {
            return;
        }
        // select random log
        if (!logs.interact()) {
            return;
        }

        boolean lightingFire = script.submitTask(() -> {
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(Collections.emptySet());
            if (inventorySnapshot == null) {
                // inventory not visible, re-poll
                return false;
            }
            // wait until we have more free slots - this tells us the log has been used successfully
            return inventorySnapshot.getFreeSlots() > initialFreeSlots;
        }, 3500);
        WorldPosition lightPosition = script.getWorldPosition();

        // if we failed to light the fire, usually means we're in a spot you can't make fires
        if (!lightingFire) {
            forceNewPosition = true;
        } else {
            waitForFireToLight(lightPosition);
        }
    }

    private void moveToNewPosition() {
        LocalPosition localPosition = script.getLocalPosition();
        if (localPosition == null) {
            script.log(BonfireHandler.class, "Local position is null, cannot find reachable tiles.");
            return;
        }
        List<LocalPosition> reachableTiles = script.getWalker().getCollisionManager().findReachableTiles(localPosition, 6);
        if (reachableTiles.isEmpty()) {
            script.log(BonfireHandler.class, "No reachable tiles found.");
            return;
        }
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakDistance(1);
        builder.tileRandomisationRadius(1);
        LocalPosition randomPos = reachableTiles.get(Utils.random(reachableTiles.size()));
        script.getWalker().walkTo(randomPos);
        forceNewPosition = false;
    }

    private void waitForFireToLight(WorldPosition lightPosition) {
        script.log(BonfireHandler.class, "Waiting for fire to light...");
        boolean result = script.submitHumanTask(() -> {
            WorldPosition currentPos = script.getWorldPosition();
            return currentPos != null && !currentPos.equals(lightPosition);
        }, 14000);

        if (result) {
            script.log(BonfireHandler.class, "Fire successfully lit!");
            logsBurnt++;
            bonfirePosition = lightPosition;
        }
    }

    private Polygon getBonfireTile() {
        Polygon tilePoly = script.getSceneProjector().getTilePoly(bonfirePosition, true);
        if (tilePoly == null) {
            return null;
        }
        tilePoly = tilePoly.getResized(0.4);
        if (tilePoly == null || !script.getWidgetManager().insideGameScreen(tilePoly, Collections.emptyList())) {
            return null;
        }
        return tilePoly;
    }

    public boolean interactAndWaitForDialogue(ItemGroupResult inventorySnapshot) {
        ItemSearchResult log = null;

        if (inventorySnapshot.getSelectedSlot() != null) {
            log = getSelectedLog(inventorySnapshot);

            // if item is selected & it isn't a log + if we fail to unselect it, re-poll
            if (log == null && !script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
                return true;
            }
        }
        if (log == null) {
            List<ItemSearchResult> itemSearchResults = inventorySnapshot.getAllOfItems(LOGS);
            if (itemSearchResults.isEmpty()) {
                script.log(BonfireHandler.class, "No logs found in inventory to burn.");
                return false;
            }
            // select a random log
            log = itemSearchResults.stream().skip(Utils.random(itemSearchResults.size())).findFirst().orElse(null);
        }
        if (!log.interact()) {
            return true;
        }

        Polygon tilePoly = getBonfireTile();
        if (tilePoly == null) {
            return true;
        }

        script.getScreen().queueCanvasDrawable("highlightFireTile", canvas -> canvas.drawPolygon(tilePoly, Color.GREEN.getRGB(), 1));
        MenuHook menuHook = menuEntries -> {
            for (MenuEntry entry : menuEntries) {
                String rawText = entry.getRawText();
                script.log(BonfireHandler.class, "Menu text: " + rawText);
                for (int logID : LOGS) {
                    String logName = LOG_NAMES.get(logID);
                    if (logName == null) {
                        script.log(BonfireHandler.class, "Log name not found for ID: " + logID);
                        continue;
                    }
                    script.log("Log name: " + logName);
                    if (rawText.equalsIgnoreCase("Use " + logName + " -> fire") || rawText.equalsIgnoreCase("Use " + logName + " -> Forester's Campfire")) {
                        return entry;
                    }
                }
            }
            return null;
        };
        // resize the poly to minimise missclicks
        if (!script.getFinger().tapGameScreen(tilePoly, menuHook)) {
            // remove the highlight after failing interacting
            script.getScreen().removeCanvasDrawable("highlightFireTile");
            // if we failed to interact with the fire, we need to reset the bonfire position
            if (failed()) {
                return false;
            }
            return true;
        }
        // reset tries, as we successfully interacted with the fire
        tries = 0;
        // remove the highlight after interacting
        script.getScreen().removeCanvasDrawable("highlightFireTile");

        script.log(BonfireHandler.class, "Waiting for dialogue");
        // sleep until dialogue is visible
        return script.submitHumanTask(() -> {
            listenChatbox();
            return script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION;
        }, Utils.random(5000, 8000));
    }

    private void burnLogsOnBonfire(ItemGroupResult inventorySnapshot) {
        // check if bonfire is active
        Polygon tilePoly = getBonfireTile();
        if (tilePoly == null || !script.getWidgetManager().insideGameScreen(tilePoly, Collections.emptyList())) {
            script.log(BonfireHandler.class, "Walking to bonfire");
            walkToBonfire();
            return;
        }

        script.log(BonfireHandler.class, "Burning logs on the bonfire...");
        // use log on bonfire
        if (!interactAndWaitForDialogue(inventorySnapshot)) {
            // walk a few tiles away, probably another camp fire close by
            LocalPosition myPos = script.getLocalPosition();
            if (myPos == null) {
                script.log(BonfireHandler.class, "Local position is null, cannot find nearby positions.");
                return;
            }
            List<LocalPosition> nearbyPositions = script.getWalker().getCollisionManager().findReachableTiles(myPos, 15);
            // remove tiles < 7 away, leaving tiles only 7-10 tiles away
            nearbyPositions.removeIf(localPosition -> myPos.distanceTo(localPosition) < 13);
            bonfirePosition = null;
        }
    }

    private void walkToBonfire() {
        // walk to tile
        WalkConfig.Builder builder = new WalkConfig.Builder();
        builder.breakDistance(2).tileRandomisationRadius(2);
        builder.breakCondition(() -> getBonfireTile() != null);
        script.getWalker().walkTo(bonfirePosition, builder.build());
    }

    private ItemSearchResult getSelectedLog(ItemGroupResult inventorySnapshot) {
        for (ItemSearchResult recognisedItem : inventorySnapshot.getRecognisedItems()) {
            if (recognisedItem.isSelected()) {
                if (!LOGS.contains(recognisedItem.getId())) {
                    script.log(BonfireHandler.class, "Selected item is not a log, unselecting it.");
                    script.getWidgetManager().getInventory().unSelectItemIfSelected();
                    return null;
                }
                return recognisedItem;
            }
        }
        return null;
    }

    private boolean failed() {
        if (tries > 1) {
            bonfirePosition = null;
            return true;
        }
        tries++;
        return false;
    }

    private static class Bonfire {
        private final WorldPosition position;
        private final long createdAt;

        private Bonfire(WorldPosition position) {
            this.position = position;
            this.createdAt = System.currentTimeMillis();
        }


        public void burnLog() {
            // add time to the bonfire based on log type
        }

        public WorldPosition getPosition() {
            return position;
        }

    }
}
