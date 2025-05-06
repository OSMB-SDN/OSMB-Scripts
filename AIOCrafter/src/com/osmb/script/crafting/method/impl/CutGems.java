package com.osmb.script.crafting.method.impl;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.script.crafting.AIOCrafter;
import com.osmb.script.crafting.data.Gem;
import com.osmb.script.crafting.data.ItemIdentifier;
import com.osmb.script.crafting.javafx.ScriptOptions;
import com.osmb.script.crafting.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Set;

public class CutGems extends Method {
    ComboBox<ItemIdentifier> itemComboBox;
    private Gem selectedGem;
    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = Set.of(ItemID.CHISEL);
    private ItemGroupResult inventorySnapshot;

    public CutGems(AIOCrafter script) {
        super(script);
    }

    @Override
    public void poll() {
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            // inventory not visible - repoll
            return;
        }

        if (!inventorySnapshot.contains(ItemID.CHISEL)) {
            script.log(getClass().getSimpleName(), "No chisel found in the inventory, stopping script...");
            script.stop();
            return;
        }
        if (!inventorySnapshot.contains(selectedGem.getItemID())) {
            script.setBank(true);
            return;
        }

        // if item action dialogue is visible, select which item to craft
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType != null) {
            if (dialogueType == DialogueType.ITEM_OPTION) {
                boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(selectedGem.getItemID(), selectedGem.getCutID());
                if (!selectedOption) {
                    script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                    return;
                }
                waitUntilFinishedProducing(selectedGem.getItemID());
                return;
            }
        }

        // use chisel on gems and wait for dialogue
        interactAndWaitForDialogue(inventorySnapshot.getItem(ItemID.CHISEL), inventorySnapshot.getRandomItem(selectedGem.getItemID()));
    }


    @Override
    public void handleBankInterface() {
        // bank everything, ignoring gems and chisel
        if (!script.getWidgetManager().getBank().depositAll(ITEM_IDS_TO_RECOGNISE)) {
            return;
        }
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        if (bankSnapshot == null || inventorySnapshot == null) {
            return;
        }
        if (inventorySnapshot.isFull()) {
            script.getWidgetManager().getBank().close();
        } else {
            if (bankSnapshot.contains(selectedGem.getItemID())) {
                script.getWidgetManager().getBank().withdraw(selectedGem.getItemID(), Integer.MAX_VALUE);
            } else {
                script.log(getClass().getSimpleName(), "No gems found in bank, stopping script.");
                script.stop();
            }
        }
    }

    @Override
    public String getMethodName() {
        return "Cut gems";
    }

    @Override
    public void provideUIOptions(VBox vBox) {
        Label itemLabel = new Label("Choose gem to cut");
        itemComboBox = ScriptOptions.createItemCombobox(script, Gem.values());
        vBox.getChildren().addAll(itemLabel, itemComboBox);
        vBox.requestLayout();
    }

    @Override
    public boolean uiOptionsSufficient() {
        if (itemComboBox.getValue() != null) {
            selectedGem = (Gem) itemComboBox.getValue();
            ITEM_IDS_TO_RECOGNISE.add(selectedGem.getItemID());
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getMethodName();
    }
}
