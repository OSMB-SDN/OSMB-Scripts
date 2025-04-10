package com.osmb.script.crafting.method.impl;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.script.crafting.AIOCrafter;
import com.osmb.script.crafting.data.Hide;
import com.osmb.script.crafting.data.Product;
import com.osmb.script.crafting.data.ItemIdentifier;
import com.osmb.script.crafting.javafx.ScriptOptions;
import com.osmb.script.crafting.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Optional;


public class CraftHide extends Method {

    private Product itemToMake = null;
    private int hideID;
    private ComboBox<ItemIdentifier> hideComboBox;
    private ComboBox<ItemIdentifier> itemToMakeCombobox;
    private UIResult<ItemSearchResult> needle;
    private UIResult<ItemSearchResult> thread;
    private UIResultList<ItemSearchResult> hides;

    public CraftHide(AIOCrafter script) {
        super(script);
    }

    @Override
    public int poll() {
        needle = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.NEEDLE);
        thread = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.THREAD);
        hides = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), hideID);

        if (thread.isNotFound()) {
            script.log(getClass().getSimpleName(), "No thread found in the inventory, stopping script...");
            script.stop();
            return 0;
        }
        if (!checkItemResult(hides) || !checkItemResult(needle) || !checkItemResult(thread)) {
            return 0;
        }

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType != null) {
            if (dialogueType == DialogueType.ITEM_OPTION) {
                int itemToMakeItemID = itemToMake.getItemID();
                boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(itemToMakeItemID);
                if (!selectedOption) {
                    script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                    return 0;
                }
                waitUntilFinishedProducing(hideID);
                return 0;
            }
        }

        interactAndWaitForDialogue(needle.get(), hides.getRandom());
        return 0;
    }


    @Override
    public int handleBankInterface() {
        // bank everything, ignoring logs and knife
        if (!script.getWidgetManager().getBank().depositAll(new int[]{hideID, ItemID.THREAD, ItemID.NEEDLE})) {
            return 0;
        }
        UIResultList<ItemSearchResult> hidesInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), hideID);
        Optional<Integer> freeItemSlots = script.getItemManager().getFreeSlotsInteger(script.getWidgetManager().getInventory());
        // if no free slots & logs are in the inventory, banking is finished

        if (freeItemSlots.isEmpty() || hidesInventory.isNotVisible()) {
            return 0;
        }
        if (freeItemSlots.get() == 0 && hidesInventory.isFound()) {
            script.getWidgetManager().getBank().close();
            return 0;
        }
        UIResultList<ItemSearchResult> hidesBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), hideID);
        if (hidesBank.isNotVisible()) {
            return 0;
        }
        if (hidesBank.isNotFound()) {
            script.log(getClass().getSimpleName(), "No hides found in bank, stopping script.");
            script.stop();
            return 0;
        }
        // withdraw logs
        script.getWidgetManager().getBank().withdraw(hideID, Integer.MAX_VALUE);
        return 0;
    }

    @Override
    public String getMethodName() {
        return "Craft hides";
    }

    @Override
    public String toString() {
        return getMethodName();
    }

    @Override
    public void provideUIOptions(VBox vBox) {
        Label itemLabel = new Label("Choose hide to craft");
        hideComboBox = ScriptOptions.createItemCombobox(script, Hide.values());
        hideComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Hide hide = (Hide) newValue;
                itemToMakeCombobox.getItems().clear();
                itemToMakeCombobox.getItems().addAll(hide.getCraftables());
            }
        });

        Label itemToMakeLabel = new Label("Choose item to make");
        itemToMakeCombobox = ScriptOptions.createItemCombobox(script, new ItemIdentifier[0]);

        vBox.getChildren().addAll(itemLabel, hideComboBox, itemToMakeLabel, itemToMakeCombobox);
    }


    @Override
    public boolean uiOptionsSufficient() {
        if (hideComboBox.getValue() != null && itemToMakeCombobox.getValue() != null) {
            itemToMake = (Product) itemToMakeCombobox.getValue();
            hideID = hideComboBox.getValue().getItemID();
            return true;
        }
        return false;
    }
}
