package com.osmb.script.crafting.method.impl;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.script.crafting.AIOCrafter;
import com.osmb.script.crafting.data.GlassBlowingItem;
import com.osmb.script.crafting.data.ItemIdentifier;
import com.osmb.script.crafting.javafx.ScriptOptions;
import com.osmb.script.crafting.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Optional;

public class GlassBlowing extends Method {

    private ItemIdentifier itemToMake;

    private ComboBox<ItemIdentifier> itemComboBox;

    public GlassBlowing(AIOCrafter script) {
        super(script);
    }

    @Override
    public int poll() {
        UIResult<ItemSearchResult> pipe = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ItemID.GLASSBLOWING_PIPE);
        UIResultList<ItemSearchResult> moltenGlass = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ItemID.MOLTEN_GLASS);
        if (pipe.isNotFound()) {
            script.log(getClass().getSimpleName(), "No Glassblowing pipe found in the inventory, stopping script...");
            script.stop();
            return 0;
        }
        if (!checkItemResult(pipe) || !checkItemResult(moltenGlass)) {
            return 0;
        }

        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType != null) {
            if (dialogueType == DialogueType.ITEM_OPTION) {
                boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(itemToMake.getItemID());
                if (!selectedOption) {
                    script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                    return 0;
                }
                waitUntilFinishedProducing(ItemID.MOLTEN_GLASS);
                return 0;
            }
        }
        interactAndWaitForDialogue(pipe.get(), moltenGlass.getRandom());
        return 0;
    }

    @Override
    public int handleBankInterface() {
        // bank everything, ignoring gems and chisel
        if (!script.getWidgetManager().getBank().depositAll(new int[]{ItemID.MOLTEN_GLASS, ItemID.GLASSBLOWING_PIPE})) {
            return 0;
        }
        UIResultList<ItemSearchResult> moltenGlassInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ItemID.MOLTEN_GLASS);
        Optional<Integer> freeItemSlots = script.getItemManager().getFreeSlotsInteger(script.getWidgetManager().getInventory());
        // if no free slots & gems are in the inventory, banking is finished

        if (freeItemSlots.isEmpty() || moltenGlassInventory.isNotVisible()) {
            return 0;
        }
        if (freeItemSlots.get() == 0 && moltenGlassInventory.isFound()) {
            script.getWidgetManager().getBank().close();
            return 0;
        }
        UIResultList<ItemSearchResult> moltenGlassBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), ItemID.MOLTEN_GLASS);
        if (moltenGlassBank.isNotVisible()) {
            return 0;
        }
        if (moltenGlassBank.isNotFound()) {
            script.log(getClass().getSimpleName(), "No gems found in bank, stopping script.");
            script.stop();
            return 0;
        }
        // withdraw gems
        script.getWidgetManager().getBank().withdraw(ItemID.MOLTEN_GLASS, Integer.MAX_VALUE);
        return 0;
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
