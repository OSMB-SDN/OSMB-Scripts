package com.osmb.script.crafting.method.impl;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.script.crafting.AIOCrafter;
import com.osmb.script.crafting.data.Gem;
import com.osmb.script.crafting.data.ItemIdentifier;
import com.osmb.script.crafting.javafx.ScriptOptions;
import com.osmb.script.crafting.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class CutGems extends Method {
    ComboBox<ItemIdentifier> itemComboBox;
    private Gem selectedGem;

    public CutGems(AIOCrafter script) {
        super(script);
    }

    @Override
    public int poll() {
        UIResult<ItemSearchResult> chisel = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.CHISEL);
        UIResultList<ItemSearchResult> gems = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), selectedGem.getItemID());

        if (chisel.isNotFound()) {
            script.log(getClass().getSimpleName(), "No chisel found in the inventory, stopping script...");
            script.stop();
            return 0;
        }
        if (!checkItemResult(gems) || !checkItemResult(chisel)) {
            return 0;
        }

        // if item action dialogue is visible, select which item to craft
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType != null) {
            if (dialogueType == DialogueType.ITEM_OPTION) {
                boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(selectedGem.getItemID(), selectedGem.getCutID());
                if (!selectedOption) {
                    script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                    return 0;
                }
                waitUntilFinishedProducing(selectedGem.getItemID());
                return 0;
            }
        }
        if (!script.getItemManager().unSelectItemIfSelected()) {
            return 0;
        }

        // use chisel on gems and wait for dialogue
        interactAndWaitForDialogue(chisel.get(), gems.getRandom());
        return 0;
    }


    @Override
    public int handleBankInterface() {
        // bank everything, ignoring gems and chisel
        if (!script.getWidgetManager().getBank().depositAll(new int[]{selectedGem.getItemID(), ItemID.CHISEL})) {
            return 0;
        }
        UIResultList<ItemSearchResult> gemsInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), selectedGem.getItemID());
        Optional<Integer> freeItemSlots = script.getItemManager().getFreeSlotsInteger(script.getWidgetManager().getInventory());
        // if no free slots & gems are in the inventory, banking is finished

        if (freeItemSlots.isEmpty() || gemsInventory.isNotVisible()) {
            return 0;
        }
        if (freeItemSlots.get() == 0 && gemsInventory.isFound()) {
            script.getWidgetManager().getBank().close();
            return 0;
        }
        UIResultList<ItemSearchResult> gemsBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), selectedGem.getItemID());
        if (gemsBank.isNotVisible()) {
            return 0;
        }
        if (gemsBank.isNotFound()) {
            script.log(getClass().getSimpleName(), "No gems found in bank, stopping script.");
            script.stop();
            return 0;
        }
        // withdraw gems
        script.getWidgetManager().getBank().withdraw(selectedGem.getItemID(), Integer.MAX_VALUE);
        return 0;
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
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getMethodName();
    }
}
