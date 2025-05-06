package com.osmb.script.fletching.method.impl;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.fletching.AIOFletcher;
import com.osmb.script.fletching.data.Arrow;
import com.osmb.script.fletching.data.ItemIdentifier;
import com.osmb.script.fletching.javafx.ScriptOptions;
import com.osmb.script.fletching.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


public class Arrows extends Method {

    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of());
    private int amountChangeTimeoutSeconds;
    private Arrow selectedArrow;
    private ComboBox<ItemIdentifier> itemComboBox;
    private int combinationID;
    private ItemGroupResult inventorySnapshot;

    public Arrows(AIOFletcher script) {
        super(script);
    }

    @Override
    public void poll() {
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            handleDialogue();
            return;
        }
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            return;
        }
        if (!inventorySnapshot.contains(combinationID) || !inventorySnapshot.contains(selectedArrow.getUnfinishedID())) {
            script.log(getClass().getSimpleName(), "Ran out of supplies, stopping script...");
            script.stop();
            return;
        }
        interactAndWaitForDialogue(inventorySnapshot.getItem(combinationID), inventorySnapshot.getItem(selectedArrow.getUnfinishedID()));
    }

    private void handleDialogue() {
        boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(selectedArrow.getItemID(), selectedArrow.getUnfinishedID());
        if (!selectedOption) {
            script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
            return;
        }
        Timer amountChangeTimer = new Timer();
        AtomicReference<Integer> arrowsUnfAmount = new AtomicReference<>(null);
        amountChangeTimeoutSeconds = script.random(3, 7);
        script.submitTask(() -> {
            if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.TAP_HERE_TO_CONTINUE) {
                // sleep for a random time so we're not instantly reacting to the dialogue
                // we do this as a task to continue updating the screen
                script.submitTask(() -> false, script.random(1000, 4000));
                return true;
            }

            if (amountChangeTimer.timeElapsed() > TimeUnit.SECONDS.toMillis(amountChangeTimeoutSeconds)) {
                // If the amount of logs in the inventory hasn't changed in the timeout amount, then return true to break out of the sleep method
                return true;
            }
            inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                // inventory not visible
                return false;
            }
            if (!inventorySnapshot.contains(combinationID) || !inventorySnapshot.contains(selectedArrow.getUnfinishedID())) {
                script.log(getClass().getSimpleName(), "Ran out of supplies, stopping script...");
                script.stop();
            } else {
                // if there are less logs, reset the timer & update the amount of logs
                int amount = inventorySnapshot.getAmount(selectedArrow.getUnfinishedID());
                if (arrowsUnfAmount.get() == null) {
                    arrowsUnfAmount.set(amount);
                    amountChangeTimer.reset();
                } else if (amount < arrowsUnfAmount.get()) {
                    arrowsUnfAmount.set(amount);
                    amountChangeTimer.reset();
                }
            }
            // if no logs left then return true, false otherwise...
            return false;
        }, 30000);
    }

    @Override
    public void handleBankInterface() {
        return;
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
            combinationID = selectedArrow == Arrow.HEADLESS_ARROW ? ItemID.FEATHER : ItemID.HEADLESS_ARROW;
            ITEM_IDS_TO_RECOGNISE.add(selectedArrow.getItemID());
            ITEM_IDS_TO_RECOGNISE.add(selectedArrow.getUnfinishedID());
            ITEM_IDS_TO_RECOGNISE.add(combinationID);
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
