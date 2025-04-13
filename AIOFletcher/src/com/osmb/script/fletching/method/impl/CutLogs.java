package com.osmb.script.fletching.method.impl;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.script.fletching.AIOFletcher;
import com.osmb.script.fletching.data.Log;
import com.osmb.script.fletching.data.Product;
import com.osmb.script.fletching.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class CutLogs extends Method {


    private Log selectedLog;
    private Product itemToCreate;
    private ComboBox<Integer> logComboBox;
    private ComboBox<Integer> itemComboBox;

    public CutLogs(AIOFletcher script) {
        super(script);
    }


    @Override
    public int poll() {
        UIResult<ItemSearchResult> knife = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.KNIFE);
        UIResultList<ItemSearchResult> logs = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), selectedLog.getItemID());

        if (knife.isNotFound()) {
            script.log(getClass().getSimpleName(), "No knife found in the inventory, stopping script...");
            script.stop();
            return 0;
        }
        if (!checkItemResult(logs)) {
            return 0;
        }

        // if item action dialogue is visible, select which item to craft
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType != null && dialogueType == DialogueType.ITEM_OPTION) {
            boolean result = script.getWidgetManager().getDialogue().selectItem(itemToCreate.getUnfinishedID());
            if (!result) {
                return 0;
            }
            waitUntilFinishedProducing(selectedLog.getItemID());
            return 0;
        }

        if (!script.getItemManager().unSelectItemIfSelected()) {
            return 0;
        }
        interactAndWaitForDialogue(knife.get(), logs.getRandom());
        return 0;
    }


    @Override
    public int handleBankInterface() {
        // bank everything, ignoring logs and knife
        if (!script.getWidgetManager().getBank().depositAll(new int[]{selectedLog.getItemID(), ItemID.KNIFE})) {
            return 0;
        }
        UIResultList<ItemSearchResult> logsInInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), selectedLog.getItemID());
        Optional<Integer> freeItemSlots = script.getItemManager().getFreeSlotsInteger(script.getWidgetManager().getInventory());
        // if no free slots & logs are in the inventory, banking is finished

        if (freeItemSlots.isEmpty() || logsInInventory.isNotVisible()) {
            return 0;
        }
        if (freeItemSlots.get() == 0 && logsInInventory.isFound()) {
            script.getWidgetManager().getBank().close();
            return 0;
        }
        UIResultList<ItemSearchResult> logsInBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), selectedLog.getItemID());
        if (logsInBank.isNotVisible()) {
            return 0;
        }
        if (logsInBank.isNotFound()) {
            script.log(getClass().getSimpleName(), "No logs found in bank, stopping script.");
            script.stop();
            return 0;
        }
        // withdraw logs
        script.getWidgetManager().getBank().withdraw(selectedLog.getItemID(), Integer.MAX_VALUE);
        return 0;
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
