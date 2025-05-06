package com.osmb.script.fletching.method.impl;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.script.fletching.AIOFletcher;
import com.osmb.script.fletching.data.Log;
import com.osmb.script.fletching.data.Product;
import com.osmb.script.fletching.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Set;

public class CutLogs extends Method {


    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = Set.of(ItemID.KNIFE);
    private Log selectedLog;
    private Product itemToCreate;
    private ComboBox<Integer> logComboBox;
    private ComboBox<Integer> itemComboBox;
    private ItemGroupResult inventorySnapshot;

    public CutLogs(AIOFletcher script) {
        super(script);
    }

    @Override
    public void poll() {
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            // inventory is not visible - re-poll
            return;
        }
        if (!inventorySnapshot.contains(ItemID.KNIFE)) {
            script.log(getClass().getSimpleName(), "No knife found in the inventory, stopping script...");
            script.stop();
            return;
        }
        if (!inventorySnapshot.contains(selectedLog.getItemID())) {
            // bank if no logs inside the inventory
            script.setBank(true);
            return;
        }

        // if item action dialogue is visible, select which item to craft
        if (script.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION) {
            boolean result = script.getWidgetManager().getDialogue().selectItem(itemToCreate.getUnfinishedID());
            if (!result) {
                script.log(CutLogs.class, "Failed selecting item " + itemToCreate.getUnfinishedID() + " in dialogue...");
                return;
            }
            waitUntilFinishedProducing(selectedLog.getItemID());
            return;
        }
        interactAndWaitForDialogue(inventorySnapshot.getItem(ItemID.KNIFE), inventorySnapshot.getRandomItem(selectedLog.getItemID()));
    }


    @Override
    public void handleBankInterface() {
        // bank everything, ignoring logs and knife
        if (!script.getWidgetManager().getBank().depositAll(ITEM_IDS_TO_RECOGNISE)) {
            return;
        }
        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (bankSnapshot == null || inventorySnapshot == null) {
            return;
        }

        if (inventorySnapshot.isFull()) {
            script.getWidgetManager().getBank().close();
            return;
        }
        if (!bankSnapshot.contains(selectedLog.getItemID())) {
            script.log(getClass().getSimpleName(), "No logs found in bank, stopping script.");
            script.stop();
        } else {
            // withdraw logs
            script.getWidgetManager().getBank().withdraw(selectedLog.getItemID(), Integer.MAX_VALUE);
        }
    }

    @Override
    public String getMethodName() {
        return "Cut logs";
    }

    @Override
    public void provideUIOptions(VBox vBox) {
        Label logLabel = new Label("Choose log to cut");
        logComboBox = JavaFXUtils.createItemCombobox(script, Log.getItemIDs());
        logComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedLog = Log.getLog(newValue);
                itemComboBox.getItems().clear();
                itemComboBox.getItems().addAll(selectedLog.getProductIDs());
            }
        });

        Label itemLabel = new Label("Choose item to create");
        itemComboBox = JavaFXUtils.createItemCombobox(script, new int[]{});
        vBox.getChildren().addAll(logLabel, logComboBox, itemLabel, itemComboBox);
        vBox.requestLayout();
    }

    @Override
    public boolean uiOptionsSufficient() {
        if (itemComboBox.getValue() != null && logComboBox.getValue() != null) {
            selectedLog = Log.getLog(logComboBox.getValue());
            if (selectedLog != null) {
                itemToCreate = selectedLog.getProduct(itemComboBox.getValue());
                ITEM_IDS_TO_RECOGNISE.add(selectedLog.getItemID());
                return true;
            }
        }
        return false;
    }


    @Override
    public String toString() {
        return getMethodName();
    }
}
