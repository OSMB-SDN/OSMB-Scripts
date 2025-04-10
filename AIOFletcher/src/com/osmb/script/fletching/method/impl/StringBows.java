package com.osmb.script.fletching.method.impl;

import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.UIResultList;
import com.osmb.script.fletching.AIOFletcher;
import com.osmb.script.fletching.data.Bow;
import com.osmb.script.fletching.data.ItemIdentifier;
import com.osmb.script.fletching.javafx.ScriptOptions;
import com.osmb.script.fletching.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;


public class StringBows extends Method {

    private Bow selectedBow;
    private ComboBox<ItemIdentifier> itemComboBox;

    public StringBows(AIOFletcher script) {
        super(script);
    }

    @Override
    public int poll() {
        // find items
        UIResultList<ItemSearchResult> uBows = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), selectedBow.getUnfinishedID());
        UIResultList<ItemSearchResult> bowStrings = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ItemID.BOW_STRING);

        if (!checkItemResult(uBows) || !checkItemResult(bowStrings)) {
            return 0;
        }

        // if item action dialogue is visible, select which item to craft
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType != null && dialogueType == DialogueType.ITEM_OPTION) {
            boolean result = script.getWidgetManager().getDialogue().selectItem(selectedBow.getItemID(), selectedBow.getUnfinishedID());
            if (!result) {
                return 0;
            }
            waitUntilFinishedProducing(selectedBow.getUnfinishedID());
            return 0;
        }

        if (!script.getItemManager().unSelectItemIfSelected()) {
            return 0;
        }

        interactAndWaitForDialogue(bowStrings.getRandom(), uBows.getRandom());
        return 0;
    }


    @Override
    public int handleBankInterface() {
        // bank everything, ignoring logs and knife
        script.log(getClass(),"Depositing unwanted items...");
        if (!script.getWidgetManager().getBank().depositAll(new int[]{selectedBow.getUnfinishedID(), ItemID.BOW_STRING})) {
            return 0;
        }

        script.log(getClass(),"Finding items");
        // search for items in the inventory
        UIResultList<ItemSearchResult> uBowsInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), selectedBow.getUnfinishedID());
        UIResultList<ItemSearchResult> bowStringsInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ItemID.BOW_STRING);

        // search for items in the bank
        UIResultList<ItemSearchResult> uBowsBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), selectedBow.getUnfinishedID());
        UIResultList<ItemSearchResult> bowStringsBank = script.getItemManager().findAllOfItem(script.getWidgetManager().getBank(), ItemID.BOW_STRING);


        // If the inventory or bank for whatever reason is not visible
        // the bank results should never not be visible, only the inventory ones can, as this method is only executed if the bank is visible...
        if (uBowsInventory.isNotVisible() || bowStringsInventory.isNotVisible() || bowStringsBank.isNotVisible() || uBowsBank.isNotVisible()) {
            script.log(getClass(),"Inventory or bank isn't visible...");
            return 0;
        }
        // work out how many slots per item
        int maxAmountOfItem = 28 / 2;

        // if we have the correct amount of items
        if (uBowsInventory.size() == maxAmountOfItem && bowStringsInventory.size() == maxAmountOfItem ||
                // or if either supplies aren't in bank, BUT we still have some to craft in the inventory
                (uBowsBank.isNotFound() || bowStringsBank.isNotFound()) && uBowsInventory.isFound() && bowStringsInventory.isFound()) {
            script.getWidgetManager().getBank().close();
            return 0;
        }

        // check if we have supplies in the inventory with the wrong amount
        if (!depositIfNotEqualsAmount(script, uBowsInventory, maxAmountOfItem) || !depositIfNotEqualsAmount(script, bowStringsInventory, maxAmountOfItem)) {
            return 0;
        }

        // if supplies not found in bank, then stop the script
        if (bowStringsBank.isNotFound() || uBowsBank.isNotFound()) {
            script.log(getClass(), "Ran out of supplies, stopping script.");
            script.stop();
            return 0;
        }

        // withdraw
        boolean randomOrder = script.random(2) == 0;

        if (randomOrder) {
            if (bowStringsInventory.isEmpty()) {
                script.getWidgetManager().getBank().withdraw(ItemID.BOW_STRING, maxAmountOfItem);
            }
            if (uBowsInventory.isEmpty()) {
                script.getWidgetManager().getBank().withdraw(selectedBow.getUnfinishedID(), maxAmountOfItem);
            }
        } else {
            if (uBowsInventory.isEmpty()) {
                script.getWidgetManager().getBank().withdraw(selectedBow.getUnfinishedID(), maxAmountOfItem);
            }
            if (bowStringsInventory.isEmpty()) {
                script.getWidgetManager().getBank().withdraw(ItemID.BOW_STRING, maxAmountOfItem);
            }
        }
        return 0;
    }

    private boolean depositIfNotEqualsAmount(AIOFletcher script, UIResultList<ItemSearchResult> items, int amount) {
        if (items.size() <= amount) {
            return true;
        }
        int amountToDeposit = items.size() - amount;
        // just deposit all if too many as we want to use withdraw X anyways in the script
        return script.getWidgetManager().getBank().deposit(items.get(0).getId(), amountToDeposit);
    }

    @Override
    public String getMethodName() {
        return "String bows";
    }

    @Override
    public String toString() {
        return getMethodName();
    }


    @Override
    public void provideUIOptions(VBox parent) {
        Label itemLabel = new Label("Choose type of bow to string:");
        itemComboBox = ScriptOptions.createItemCombobox(script, Bow.values());
        parent.getChildren().addAll(itemLabel, itemComboBox);
    }

    @Override
    public boolean uiOptionsSufficient() {
        if (itemComboBox.getValue() != null) {
            selectedBow = (Bow) itemComboBox.getValue();
            return true;
        }
        return false;
    }
}
