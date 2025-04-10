package com.osmb.script.fletching.method.impl;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.fletching.AIOFletcher;
import com.osmb.script.fletching.data.Arrow;
import com.osmb.script.fletching.data.ItemIdentifier;
import com.osmb.script.fletching.javafx.ScriptOptions;
import com.osmb.script.fletching.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


public class Arrows extends Method {

    private int amountChangeTimeoutSeconds;
    private Arrow selectedArrow;
    private ComboBox<ItemIdentifier> itemComboBox;

    public Arrows(AIOFletcher script) {
        super(script);
    }

    @Override
    public int poll() {
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();

        if (dialogueType != null && dialogueType == DialogueType.ITEM_OPTION) {
            handleDialogue();
            return 0;
        }

        UIResult<ItemSearchResult> arrowUnf = script.getItemManager().findItem(script.getWidgetManager().getInventory(), selectedArrow.getUnfinishedID());
        int combinationID = selectedArrow == Arrow.HEADLESS_ARROW ? ItemID.FEATHER : ItemID.HEADLESS_ARROW;
        UIResult<ItemSearchResult> combination = script.getItemManager().findItem(script.getWidgetManager().getInventory(), false, combinationID);
        if (arrowUnf.isNotVisible() || combination.isNotVisible()) {
            return 0;
        }
        if (combination.get() == null || arrowUnf.get() == null) {
            script.log(getClass().getSimpleName(), "Ran out of supplies, stopping script...");
            script.stop();
            return 0;
        }

        interactAndWaitForDialogue(combination.get(), arrowUnf.get());
        return 0;
    }

    private void handleDialogue() {
        boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(selectedArrow.getItemID(), selectedArrow.getUnfinishedID());
        if (!selectedOption) {
            script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
            return;
        }
        Timer amountChangeTimer = new Timer();
        UIResult<ItemSearchResult> arrowsUnf = script.getItemManager().findItem(script.getWidgetManager().getInventory(), selectedArrow.getUnfinishedID());
        if (!arrowsUnf.isFound()) {
            return;
        }
        AtomicReference<Integer> arrowsUnfAmount = new AtomicReference<>(arrowsUnf.get().getStackAmount());
        amountChangeTimeoutSeconds = script.random(3, 7);
        script.submitTask(() -> {
            DialogueType dialogueType_ = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType_ != null) {
                if (dialogueType_ == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // sleep for a random time so we're not instantly reacting to the dialogue
                    // we do this as a task to continue updating the screen
                    script.submitTask(() -> false, script.random(1000, 4000));
                    return true;
                }
            }
            if (!script.getWidgetManager().getInventory().open()) {
                return false;
            }
            if (amountChangeTimer.timeElapsed() > TimeUnit.SECONDS.toMillis(amountChangeTimeoutSeconds)) {
                // If the amount of logs in the inventory hasn't changed in the timeout amount, then return true to break out of the sleep method
                return true;
            }
            UIResult<ItemSearchResult> arrowsUnf_ = script.getItemManager().findItem(script.getWidgetManager().getInventory(), selectedArrow.getUnfinishedID());
            int combinationID = selectedArrow == Arrow.HEADLESS_ARROW ? ItemID.FEATHER : ItemID.HEADLESS_ARROW;
            UIResult<ItemSearchResult> feathers = script.getItemManager().findItem(script.getWidgetManager().getInventory(), false, combinationID);
            if (arrowsUnf_.isNotVisible() || feathers.isNotVisible()) {
                return false;
            }
            if (arrowsUnf_.get() == null || feathers.get() == null) {
                script.log(getClass().getSimpleName(), "Insufficient supplies, stopping script...");
                script.stop();
                return true;
            }
            // if there are less logs, reset the timer & update the amount of logs
            int amount = arrowsUnf_.get().getStackAmount();
            if (amount < arrowsUnfAmount.get()) {
                arrowsUnfAmount.set(amount);
                amountChangeTimer.reset();
            }
            // if no logs left then return true, false otherwise...
            return false;
        }, 30000);
    }

    @Override
    public int handleBankInterface() {
        return 0;
    }

    @Override
    public void provideUIOptions(VBox vBox) {
        Label label = new Label("Choose arrows to make");
        vBox.getChildren().add(label);

        itemComboBox = ScriptOptions.createItemCombobox(script, Arrow.values());
        vBox.getChildren().add(itemComboBox);
    }

    @Override
    public boolean uiOptionsSufficient() {
        if (itemComboBox.getValue() != null) {
            selectedArrow = (Arrow) itemComboBox.getValue();
            return true;
        }
        return false;
    }

    @Override
    public String getMethodName() {
        return "Arrows";
    }

    @Override
    public String toString() {
        return getMethodName();
    }
}
