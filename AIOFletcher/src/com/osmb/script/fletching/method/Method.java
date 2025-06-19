package com.osmb.script.fletching.method;

import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.Result;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.fletching.AIOFletcher;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Method {

    public final AIOFletcher script;

    protected Method(AIOFletcher script) {
        this.script = script;
    }


    public abstract void poll();

    public abstract void handleBankInterface();

    public abstract String getMethodName();

    public abstract void provideUIOptions(VBox vBox);

    public abstract boolean uiOptionsSufficient();

    public void onGamestateChanged(GameState gameState) {
    }

    public boolean checkItemResult(Result uiResult) {
        if (uiResult.isNotVisible()) {
            return false;
        }
        if (uiResult.isNotFound()) {
            script.setBank(true);
            return false;
        }
        return true;
    }

    public boolean interactAndWaitForDialogue(ItemSearchResult item1, ItemSearchResult item2) {
        int random = script.random(2);
        ItemSearchResult interact1 = random == 0 ? item1 : item2;
        ItemSearchResult interact2 = random == 0 ? item2 : item1;

        if (interact1.interact() && interact2.interact()) {
            // sleep until dialogue is visible
            return script.submitHumanTask(() -> {
                DialogueType dialogueType1 = script.getWidgetManager().getDialogue().getDialogueType();
                if (dialogueType1 == null) return false;
                return dialogueType1 == DialogueType.ITEM_OPTION;
            }, 3000);
        }
        return false;
    }

    private int amountChangeTimeout = Utils.random(3500,6000);

    public void waitUntilFinishedProducing(int amountPer, int... resources) {
        AtomicReference<Map<Integer, Integer>> previousAmounts = new AtomicReference<>(new HashMap<>());
        for (int resource : resources) {
            previousAmounts.get().put(resource, -1);
        }
        Timer amountChangeTimer = new Timer();
        script.submitHumanTask(() -> {
            DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                // look out for level up dialogue etc.
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // sleep for a random time so we're not instantly reacting to the dialogue
                    // we do this in the task to continue updating the screen
                    script.submitTask(() -> false, script.random(1000, 4000));
                    return true;
                }
            }

            // If the amount of items in the inventory hasn't changed in the timeout amount, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                amountChangeTimeout = Utils.random(3500,6000);
                return true;
            }
            Set<Integer> itemIdsToRecognise = new HashSet<>();
            for(int resourceID : resources) {
                itemIdsToRecognise.add(resourceID);
            }

            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(itemIdsToRecognise);
            if(inventorySnapshot == null) {
                // inv not visible
                return false;
            }

            for (int resource : resources) {
                ItemDefinition def = script.getItemManager().getItemDefinition(resource);
                if (def == null) {
                    throw new RuntimeException("Definition is null for ID: " + resource);
                }
                int amount = inventorySnapshot.getAmount(resource);
                if (amount < amountPer) {
                    return true;
                }
                int previousAmount = previousAmounts.get().get(resource);
                if (amount < previousAmount || previousAmount == -1) {
                    previousAmounts.get().put(resource, amount);
                    amountChangeTimer.reset();
                }
            }
            return false;
        }, 60000, false, true);
    }
}

