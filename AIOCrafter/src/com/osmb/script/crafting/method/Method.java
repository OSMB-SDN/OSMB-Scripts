package com.osmb.script.crafting.method;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.Result;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.crafting.AIOCrafter;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public abstract class Method {

    public final AIOCrafter script;

    public Method(AIOCrafter script) {
        this.script = script;
    }


    public abstract void poll();

    public abstract void handleBankInterface();

    public abstract String getMethodName();

    public abstract void provideUIOptions(VBox vBox);

    public abstract boolean uiOptionsSufficient();

    public void onGamestateChanged(GameState gameState) {
    }

    /**
     * Waits until the specified resources are finished producing.
     * This method will check the inventory for the specified resources and wait until their amounts stop changing.
     * If the amount of any resource goes to 0, it will also break out of the sleep.
     *
     * @param resources The resource IDs to wait for.
     */
    public void waitUntilFinishedProducing(int... resources) {
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

            // If the amount of gems in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > script.getAmountChangeTimeout()) {
                script.resetAmountChangeTimeout();
                return true;
            }
            Set<Integer> itemIdsToRecognise = new HashSet<>();
            for(int resource : resources) {
                itemIdsToRecognise.add(resource);
            }
            ItemGroupResult inventorySnapshot = script.getWidgetManager().getInventory().search(itemIdsToRecognise);
            if(inventorySnapshot == null) {
                // inventory not visible
                return false;
            }
            for (int resource : resources) {
                int amount = inventorySnapshot.getAmount(resource);

                if (amount == 0) {
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
    /**
     * Interacts with two items and waits for dialogue to appear.
     *
     * @param item1 First item to interact with.
     * @param item2 Second item to interact with.
     * @return true if dialogue appears, false otherwise.
     */
    public boolean interactAndWaitForDialogue(ItemSearchResult item1, ItemSearchResult item2) {
        int random = script.random(2);
        ItemSearchResult interact1 = random == 0 ? item1 : item2;
        ItemSearchResult interact2 = random == 0 ? item2 : item1;
        if (interact1.interact() && interact2.interact()) {
            return script.submitHumanTask(() -> script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION, 3000);
        }
        return false;
    }
}
