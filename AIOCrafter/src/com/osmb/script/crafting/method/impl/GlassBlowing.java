package com.osmb.script.crafting.method.impl;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResultList;
import com.osmb.script.crafting.AIOCrafter;
import com.osmb.script.crafting.data.GlassBlowingItem;
import com.osmb.script.crafting.data.ItemIdentifier;
import com.osmb.script.crafting.javafx.ScriptOptions;
import com.osmb.script.crafting.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class GlassBlowing extends Method {

    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.GLASSBLOWING_PIPE, ItemID.MOLTEN_GLASS));
    private ItemIdentifier itemToMake;
    private ComboBox<ItemIdentifier> itemComboBox;
    private ItemGroupResult inventorySnapshot;

    public GlassBlowing(AIOCrafter script) {
        super(script);
    }

    @Override
    public void poll() {
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if(inventorySnapshot == null) {
            // inventory is not visible - re-poll
            return;
        }
        if (!inventorySnapshot.contains(ItemID.GLASSBLOWING_PIPE)) {
            script.log(getClass().getSimpleName(), "No Glassblowing pipe found in the inventory, stopping script...");
            script.stop();
            return;
        }
        if(!inventorySnapshot.contains(ItemID.MOLTEN_GLASS)) {
            script.setBank(true);
            return;
        }

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType != null) {
            if (dialogueType == DialogueType.ITEM_OPTION) {
                boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(itemToMake.getItemID());
                if (!selectedOption) {
                    script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                    return;
                }
                waitUntilFinishedProducing(ItemID.MOLTEN_GLASS);
                return;
            }
        }
        interactAndWaitForDialogue(inventorySnapshot.getItem(ItemID.GLASSBLOWING_PIPE), inventorySnapshot.getRandomItem(ItemID.MOLTEN_GLASS));
    }

    @Override
    public void handleBankInterface() {
        // bank everything, ignoring molten glass and pipe
        if (!script.getWidgetManager().getBank().depositAll(ITEM_IDS_TO_RECOGNISE)) {
            return;
        }
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        if (bankSnapshot == null || inventorySnapshot == null) {
            // either not visible - shouldn't happen but good practice to have this check
            return;
        }

        if (inventorySnapshot.isFull()) {
            script.getWidgetManager().getBank().close();
        } else {
            if(bankSnapshot.contains(ItemID.MOLTEN_GLASS)) {
                script.getWidgetManager().getBank().withdraw(ItemID.MOLTEN_GLASS, Integer.MAX_VALUE);
            } else {
                script.log(getClass().getSimpleName(), "No molten glass found in bank, stopping script.");
                script.stop();
            }
        }
    }

    @Override
    public String getMethodName() {
        return "Glassblowing";
    }

    @Override
    public String toString() {
        return getMethodName();
    }

    @Override
    public void provideUIOptions(VBox vBox) {
        Label itemLabel = new Label("Choose item to make");
        itemComboBox = ScriptOptions.createItemCombobox(script, GlassBlowingItem.values());
        vBox.getChildren().addAll(itemLabel, itemComboBox);
        vBox.requestLayout();
    }

    @Override
    public boolean uiOptionsSufficient() {
        if (itemComboBox.getValue() != null) {
            this.itemToMake = itemComboBox.getValue();
            return true;
        }
        return false;
    }
}
