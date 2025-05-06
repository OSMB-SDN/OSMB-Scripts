package com.osmb.script.fletching.method.impl;

import com.osmb.api.item.ItemGroupResult;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class StringBows extends Method {

    private static final Set<Integer> ITEM_IDS_TO_RECOGNISE = Set.of(ItemID.BOW_STRING);
    private Bow selectedBow;
    private ComboBox<ItemIdentifier> itemComboBox;
    private ItemGroupResult inventorySnapshot;

    public StringBows(AIOFletcher script) {
        super(script);
    }

    @Override
    public void poll() {
        if (!script.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            return;
        }
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (inventorySnapshot == null) {
            return;
        }
        if (!inventorySnapshot.contains(ItemID.BOW_STRING) || !inventorySnapshot.contains(selectedBow.getUnfinishedID())) {
            // bank if either string or unf bows run out
            script.setBank(true);
            return;
        }

        // if item action dialogue is visible, select which item to craft
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean result = script.getWidgetManager().getDialogue().selectItem(selectedBow.getItemID(), selectedBow.getUnfinishedID());
            if (!result) {
                script.log(StringBows.class, "Failed selecting dialogue option...");
                return;
            }
            waitUntilFinishedProducing(selectedBow.getUnfinishedID());
            return;
        }

        interactAndWaitForDialogue(inventorySnapshot.getRandomItem(ItemID.BOW_STRING), inventorySnapshot.getRandomItem(selectedBow.getUnfinishedID()));
    }


    @Override
    public void handleBankInterface() {
        // bank everything, ignoring logs and knife
        script.log(getClass(), "Depositing unwanted items...");
        if (!script.getWidgetManager().getBank().depositAll(ITEM_IDS_TO_RECOGNISE)) {
            return;
        }
        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(ITEM_IDS_TO_RECOGNISE);
        inventorySnapshot = script.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
        if (bankSnapshot == null || inventorySnapshot == null) {
            return;
        }
        // work out how many slots per item
        int targetAmount = 28 / 2;

        // if we have the correct amount of items

        // if supplies not found in bank, then stop the script
        if (!bankSnapshot.contains(ItemID.BOW_STRING) || !bankSnapshot.contains(selectedBow.getUnfinishedID())) {
            script.log(getClass(), "Ran out of supplies, stopping script.");
            script.stop();
            return;
        }
        List<BankEntry> bankEntries = new ArrayList<>();

        int bowsStringNeeded = targetAmount - inventorySnapshot.getAmount(ItemID.BOW_STRING);
        if (bowsStringNeeded != 0)
            bankEntries.add(new BankEntry(ItemID.BOW_STRING, bowsStringNeeded));

        int bowsNeeded = targetAmount - inventorySnapshot.getAmount(selectedBow.getUnfinishedID());
        if (bowsNeeded != 0)
            bankEntries.add(new BankEntry(selectedBow.getUnfinishedID(), bowsNeeded));

        if (bankEntries.isEmpty()) {
            script.getWidgetManager().getBank().close();
        } else {
            // randomise order
            Collections.shuffle(bankEntries);

            BankEntry bankEntry = bankEntries.get(script.random(bankEntries.size()));
            script.log(StringBows.class, "Processing bank entry: " + bankEntry);
            if (bankEntry.amount > 0) {
                // withdraw
                script.getWidgetManager().getBank().withdraw(bankEntry.itemID, bankEntry.amount);
            } else {
                // deposit
                script.getWidgetManager().getBank().deposit(bankEntry.itemID, bankEntry.amount);
            }
        }
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
            ITEM_IDS_TO_RECOGNISE.add(selectedBow.getUnfinishedID());
            return true;
        }
        return false;
    }

    private static class BankEntry {
        int amount;
        int itemID;

        public BankEntry(int itemID, int amount) {
            this.itemID = itemID;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "BankEntry{" +
                    "amount=" + amount +
                    ", itemID=" + itemID +
                    '}';
        }
    }
}
