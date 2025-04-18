package com.osmb.script.fletching.method;

import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.Result;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.fletching.AIOFletcher;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.osmb.script.fletching.AIOFletcher.AMOUNT_CHANGE_TIMEOUT_SECONDS;

public abstract class Method {

    public final AIOFletcher script;

    protected Method(AIOFletcher script) {
        this.script = script;
    }


    public abstract int poll();

    public abstract int handleBankInterface();

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
        int random = script.random(1);
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

            // If the amount of gems in the inventory hasn't changed in the timeout amount, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > TimeUnit.SECONDS.toMillis(AMOUNT_CHANGE_TIMEOUT_SECONDS)) {
                return true;
            }
            if (!script.getWidgetManager().getInventory().open()) {
                return false;
            }

            for (int resource : resources) {
                ItemDefinition def = script.getItemManager().getItemDefinition(resource);
                if (def == null) {
                    throw new RuntimeException("Definition is null for ID: " + resource);
                }
                int amount;
                if (def.stackable == 0) {
                    UIResultList<ItemSearchResult> resourceResult = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), resource);
                    if (resourceResult.isNotVisible()) {
                        return false;
                    }
                    amount = resourceResult.size();
                } else {
                    UIResult<ItemSearchResult> resourceResult = script.getItemManager().findItem(script.getWidgetManager().getInventory(), resource);
                    if (resourceResult.isNotVisible()) {
                        return false;
                    }
                    amount = resourceResult.get().getStackAmount();
                }
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
        }, 60000, true, false, true);
    }
}

