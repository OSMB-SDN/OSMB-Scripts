package com.osmb.script.crafting.method.impl;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.script.crafting.AIOCrafter;
import com.osmb.script.crafting.data.Hide;
import com.osmb.script.crafting.data.ItemIdentifier;
import com.osmb.script.crafting.data.Product;
import com.osmb.script.crafting.javafx.ScriptOptions;
import com.osmb.script.crafting.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Set;


public class CraftHide extends Method {

    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.COSTUME_NEEDLE, ItemID.THREAD, ItemID.NEEDLE));
    private Product itemToMake = null;
    private int hideID;
    private ComboBox<ItemIdentifier> hideComboBox;
    private ComboBox<ItemIdentifier> itemToMakeCombobox;
    private ItemGroupResult inventorySnapshot;

    public CraftHide(AIOCrafter script) {
        super(script);
    }

    @Override
    public void poll() {
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            // inventory not visible
            return;
        }
        if (!inventorySnapshot.contains(ItemID.NEEDLE) && !inventorySnapshot.contains(ItemID.COSTUME_NEEDLE)) {
            script.log(getClass().getSimpleName(), "No needle found in the inventory, stopping script...");
            script.stop();
            return;
        }
        if (!inventorySnapshot.contains(ItemID.COSTUME_NEEDLE) && !inventorySnapshot.contains(ItemID.THREAD)) {
            script.log(getClass().getSimpleName(), "No thread found in the inventory, stopping script...");
            script.stop();
            return;
        }
        if (inventorySnapshot.getAmount(hideID) < itemToMake.getAmountNeeded()) {
            script.setBank(true);
            return;
        }

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType != null) {
            if (dialogueType == DialogueType.ITEM_OPTION) {
                int itemToMakeItemID = itemToMake.getItemID();
                boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(itemToMakeItemID);
                if (!selectedOption) {
                    script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                    return;
                }
                waitUntilFinishedProducing(hideID);
                return;
            }
        }
        interactAndWaitForDialogue(inventorySnapshot.getItem(ItemID.NEEDLE, ItemID.COSTUME_NEEDLE), inventorySnapshot.getRandomItem(hideID));
    }


    @Override
    public void handleBankInterface() {
        // bank everything, ignoring logs and knife
        if (!script.getWidgetManager().getBank().depositAll(ITEM_IDS_TO_RECOGNISE)) {
            return;
        }
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null || bankSnapshot == null) {
            // inventory or bank not visible
            return;
        }

        if (inventorySnapshot.isFull()) {
            script.getWidgetManager().getBank().close();
        } else {
            if (!bankSnapshot.contains(hideID)) {
                script.log(getClass().getSimpleName(), "No hides found in bank, stopping script.");
                script.stop();
            } else {
                // withdraw hides
                script.getWidgetManager().getBank().withdraw(hideID, Integer.MAX_VALUE);
            }
        }
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
            ITEM_IDS_TO_RECOGNISE.add(hideID);
            return true;
        }
        return false;
    }
}
