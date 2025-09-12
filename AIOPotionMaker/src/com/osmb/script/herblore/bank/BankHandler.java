package com.osmb.script.herblore.bank;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.ui.bank.Bank;
import com.osmb.api.utils.RandomUtils;
import com.osmb.script.herblore.data.Ingredient;
import com.osmb.script.herblore.mixing.PotionMixer;
import com.osmb.script.herblore.utils.Utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.osmb.script.herblore.Config.selectedPotion;
import static com.osmb.script.herblore.Constants.BANK_ACTIONS;
import static com.osmb.script.herblore.Constants.BANK_QUERY;
import static com.osmb.script.herblore.State.inventorySnapshot;

public class BankHandler {

    private final ScriptCore core;
    private final Bank bank;

    public BankHandler(ScriptCore core) {
        this.core = core;
        this.bank = core.getWidgetManager().getBank();
    }

    public void handleBankInterface() {
        Ingredient[] ingredientsList = selectedPotion.getIngredients();
        // work out how many potions we can make
        int amountToProduce = Utilities.calculatePotionAmount(core, ingredientsList);
        core.log(BankHandler.class, "Amount of potions we can create: " + amountToProduce);
        Set<Integer> itemsToIgnore = getItemsToNotDeposit(inventorySnapshot, amountToProduce);

        // deposit unrelated items
        core.log(BankHandler.class, "Ignoring items" + itemsToIgnore);
        if (!bank.depositAll(itemsToIgnore)) {
            return;
        }

        ItemGroupResult bankSnapshot = bank.search(selectedPotion.getIngredientIds());
        inventorySnapshot = core.getWidgetManager().getInventory().search(selectedPotion.getIngredientIds());

        if (bankSnapshot == null || inventorySnapshot == null) {
            return;
        }
        amountToProduce = Utilities.calculatePotionAmount(core, ingredientsList);
        core.log(PotionMixer.class, "Amount of potions we can create: " + amountToProduce);


        List<BankEntry> bankEntries = getBankEntries(inventorySnapshot, ingredientsList, amountToProduce);

        // close if no entries
        if (bankEntries.isEmpty()) {
            bank.close();
            return;
        }

        BankEntry bankEntry = bankEntries.get(RandomUtils.uniformRandom(bankEntries.size()));
        core.log(PotionMixer.class, "Processing bank entry: " + bankEntry);
        if (bankEntry.amount > 0) {
            // withdraw
            boolean stackable = core.getItemManager().isStackable(bankEntry.itemID);
            boolean notEnough = bankSnapshot.getAmount(bankEntry.itemID) < bankEntry.amount;
            if (notEnough) {
                core.log(PotionMixer.class, "Insufficient supplies. Item ID: " + bankEntry.itemID);
                core.stop();
                return;
            }
            core.log(PotionMixer.class, "Withdrawing item id: " + bankEntry.itemID + " amount: " + bankEntry.amount + " stackable: " + stackable);
            bank.withdraw(bankEntry.itemID, stackable ? Integer.MAX_VALUE : amountToProduce);
        } else {
            // deposit
            bank.deposit(bankEntry.itemID, bankEntry.amount);
        }

    }

    private List<BankEntry> getBankEntries(ItemGroupResult inventorySnapshot, Ingredient[] ingredientsList, int amountToProduce) {
        List<BankEntry> bankEntries = new ArrayList<>();
        // go over and check if we have too many
        for (Ingredient ingredient : ingredientsList) {
            int inventoryAmount = inventorySnapshot.getAmount(ingredient.getItemID());
            core.log(PotionMixer.class, "Amount of itemid: " + ingredient.getItemID() + " amount: " + inventoryAmount);
            int amountNeeded = (ingredient.getAmount() * amountToProduce) - inventoryAmount;

            if (core.getItemManager().isStackable(ingredient.getItemID())) {
                ItemSearchResult stackableIngredient = inventorySnapshot.getItem(ingredient.getItemID());
                if (stackableIngredient == null || inventorySnapshot.getAmount(ingredient.getItemID()) < ingredient.getAmount()) {
                    bankEntries.add(new BankEntry(ingredient.getItemID(), amountNeeded));
                }
            } else {
                core.log(PotionMixer.class, "Amount needed: " + amountNeeded);
                if (amountNeeded == 0) continue;
                bankEntries.add(new BankEntry(ingredient.getItemID(), amountNeeded));
            }
        }
        return bankEntries;
    }

    private Set<Integer> getItemsToNotDeposit(ItemGroupResult inventorySnapshot, int amountToProduce) {

        Ingredient[] ingredients = selectedPotion.getIngredients();

        Set<Integer> itemsToIgnore = new HashSet<>();

        // make sure we deposit everything except the items we have the right amount of
        // we don't withdraw the difference as amulet of chemistry can be used saving supplies
        // which can cause issues changing x amounts each time, its easier to just deposit all of item with incorrect amount
        for (Ingredient ingredient : ingredients) {
            if (core.getItemManager().isStackable(ingredient.getItemID())) {
                itemsToIgnore.add(ingredient.getItemID());
                continue;
            }
            int inventoryAmount = inventorySnapshot.getAmount(ingredient.getItemID());
            int amountNeeded = (ingredient.getAmount() * amountToProduce) - inventoryAmount;
            String itemName = core.getItemManager().getItemName(ingredient.getItemID());
            core.log("Ingredient: " + itemName + " Inventory amount: " + inventoryAmount + " Amount needed: " + amountNeeded);
            if (amountNeeded == 0) {
                itemsToIgnore.add(ingredient.getItemID());
                core.log(PotionMixer.class, "Not depositing item id: " + itemName);
            }
        }
        return itemsToIgnore;
    }


    public void openBank() {
        core.log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        List<RSObject> banksFound = core.getObjectManager().getObjects(BANK_QUERY);
        //can't find a bank
        if (banksFound.isEmpty()) {
            core.log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }

        RSObject bank = (RSObject) core.getUtils().getClosest(banksFound);
        if (!bank.interact(BANK_ACTIONS)) {
            // failed to interact with the bank
            return;
        }
        waitForBankToOpen(bank);
    }

    private void waitForBankToOpen(RSObject object) {
        long positionChangeTimeout = RandomUtils.uniformRandom(1000, 2100);
        // wait for bank interface
        core.pollFramesHuman(() -> {
            WorldPosition worldPosition = core.getWorldPosition();
            if (worldPosition == null) return false;
            if (object.getTileDistance(worldPosition) > 1 && core.getLastPositionChangeMillis() > positionChangeTimeout) {
                // not moving for some reason
                return true;
            }
            return core.getWidgetManager().getBank().isVisible();
        }, RandomUtils.uniformRandom(6500, 10000));
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
