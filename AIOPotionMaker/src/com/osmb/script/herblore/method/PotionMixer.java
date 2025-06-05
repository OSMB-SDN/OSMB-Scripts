package com.osmb.script.herblore.method;

import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.herblore.AIOPotionMaker;
import com.osmb.script.herblore.data.Ingredient;
import com.osmb.script.herblore.data.ItemIdentifier;
import com.osmb.script.herblore.data.Potion;
import com.osmb.script.herblore.javafx.ScriptOptions;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.osmb.script.herblore.AIOPotionMaker.AMOUNT_CHANGE_TIMEOUT_SECONDS;

public class PotionMixer {
    public final AIOPotionMaker script;
    private final String name;
    private final Potion[] values;
    private Potion selectedPotion;
    private ComboBox<ItemIdentifier> itemComboBox;
    private ItemGroupResult inventorySnapshot;

    /**
     * @param script
     * @param name   - The name of the potion mixer
     * @param values - The potions that the method provides
     */
    public PotionMixer(AIOPotionMaker script, String name, Potion[] values) {
        this.script = script;
        this.name = name;
        this.values = values;
    }

    @Override
    public String toString() {
        return name;
    }

    public void poll() {
        // if item action dialogue is visible, select which item
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            interactWithItemDialogue();
            return;
        }
        inventorySnapshot = script.getWidgetManager().getInventory().search(selectedPotion.getIngredientIds());
        if (inventorySnapshot == null) {
            return;
        }

        Boolean hasIngredients = hasIngredients();
        if (hasIngredients) {
            script.log(PotionMixer.class, "We have ingredients...");
            mixPotions(selectedPotion.getIngredients());
        } else {
            script.log(PotionMixer.class, "We have no ingredients... banking!");
            script.setBank(true);
        }
    }

    private boolean interactWithItemDialogue() {
        script.log(PotionMixer.class, "Interacting with item dialogue");
        int[] dialogueIds = new int[selectedPotion.getIngredients().length + 1];
        for (int i = 0; i < selectedPotion.getIngredients().length; i++) {
            dialogueIds[i] = selectedPotion.getIngredients()[i].getItemID();
        }
        dialogueIds[dialogueIds.length - 1] = selectedPotion.getItemID();

        boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(dialogueIds);
        if (!selectedOption) {
            script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
        } else {
            waitUntilFinishedProducing(selectedPotion.getIngredients());
            return true;
        }
        return false;
    }


    private void mixPotions(Ingredient[] ingredients) {
        ItemSearchResult[] ingredientResults = new ItemSearchResult[ingredients.length];
        for (int i = 0; i < ingredients.length; i++) {
            Ingredient ingredient = ingredients[i];
            if (!script.getItemManager().isStackable(ingredient.getItemID())) {
                ingredientResults[i] = inventorySnapshot.getRandomItem(ingredient.getItemID());
            } else {
                ingredientResults[i] = inventorySnapshot.getItem(ingredient.getItemID());
            }
        }
        Ingredient mandatoryIngredient = null;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isMandatoryToCombine()) {
                mandatoryIngredient = ingredient;
                break;
            }
        }
        int index1 = -1;
        if (mandatoryIngredient != null) {
            for (int i = 0; i < ingredients.length; i++) {
                ItemSearchResult item = ingredientResults[i];
                if (item.getId() == mandatoryIngredient.getItemID()) {
                    index1 = i;
                    break;
                }
            }
        } else {
            index1 = script.random(ingredientResults.length);
        }
        if (index1 == -1) {
            script.log(PotionMixer.class, "Failed finding ingredient index. Mandatory ingredient: " + (mandatoryIngredient != null));
            return;
        }
        int index2;
        do {
            index2 = script.random(ingredientResults.length);
        } while (index2 == index1);
        interactAndWaitForDialogue(ingredientResults[index1], ingredientResults[index2]);
    }

    private boolean hasIngredients() {
        Ingredient[] ingredientsList = selectedPotion.getIngredients();
        for (Ingredient ingredient : ingredientsList) {
            if (script.getItemManager().isStackable(ingredient.getItemID())) {
                if (!inventorySnapshot.contains(ingredient.getItemID())) {
                    return false;
                }
            } else {
                if (inventorySnapshot.getAmount(ingredient.getItemID()) < ingredient.getAmount()) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean uiOptionsSufficient() {
        if (itemComboBox.getValue() != null) {
            selectedPotion = (Potion) itemComboBox.getValue();
            return true;
        }
        return false;
    }

    public void provideUIOptions(VBox vBox) {
        Label itemLabel = new Label("Choose potion");
        itemComboBox = ScriptOptions.createItemCombobox(script, values);
        vBox.getChildren().addAll(itemLabel, itemComboBox);
        vBox.requestLayout();
    }

    public void onGamestateChanged(GameState gameState) {
    }

    private Set<Integer> getItemsToNotDeposit() {
        Ingredient[] ingredients = selectedPotion.getIngredients();
        List<Ingredient> ingredientsList = Arrays.asList(ingredients);
        ingredients = ingredientsList.toArray(new Ingredient[0]);

        Set<Integer> itemsToIgnore = new HashSet<>();

        for (Ingredient ingredient : ingredients) {
            itemsToIgnore.add(ingredient.getItemID());
        }
        return itemsToIgnore;
    }

    public void handleBankInterface() {
        Set<Integer> itemsToIgnore = getItemsToNotDeposit();

        if (!script.getWidgetManager().getBank().depositAll(itemsToIgnore)) {
            return;
        }

        ItemGroupResult bankSnapshot = script.getWidgetManager().getBank().search(selectedPotion.getIngredientIds());
        inventorySnapshot = script.getWidgetManager().getInventory().search(selectedPotion.getIngredientIds());

        if (bankSnapshot == null || inventorySnapshot == null) {
            return;
        }


        int slotsToWorkWith = inventorySnapshot.getFreeSlots();
        // calculate how many slots it takes to make a single potion
        Ingredient[] ingredientsList = selectedPotion.getIngredients();
        int slotsPerPotion = 0;
        int stackableIngredients = 0;
        for (Ingredient ingredient : ingredientsList) {
            if (script.getItemManager().isStackable(ingredient.getItemID())) {
                stackableIngredients++;
                if (inventorySnapshot.contains(ingredient.getItemID())) {
                    slotsToWorkWith++;
                }
            } else {
                slotsPerPotion += ingredient.getAmount();
                slotsToWorkWith += inventorySnapshot.getAmount(ingredient.getItemID());
            }
        }
        // work out how many potions we can make
        int amountOfPotions = (slotsToWorkWith - stackableIngredients) / slotsPerPotion;

        script.log(PotionMixer.class, "Amount of potions we can create: " + amountOfPotions);
        List<BankEntry> bankEntries = new ArrayList<>();
        // go over and check if we have too many
        for (Ingredient ingredient : ingredientsList) {
            int inventoryAmount = inventorySnapshot.getAmount(ingredient.getItemID());
            script.log(PotionMixer.class, "Amount of itemid: " + ingredient.getItemID() + " amount: " + inventoryAmount);
            int amountNeeded = (ingredient.getAmount() * amountOfPotions) - inventoryAmount;

            if (script.getItemManager().isStackable(ingredient.getItemID())) {
                ItemSearchResult stackableIngredient = inventorySnapshot.getItem(ingredient.getItemID());
                if (stackableIngredient == null || inventorySnapshot.getAmount(ingredient.getItemID()) < ingredient.getAmount()) {
                    bankEntries.add(new BankEntry(ingredient.getItemID(), amountNeeded));
                }
            } else {
                script.log(PotionMixer.class, "Amount needed: " + amountNeeded);
                if (amountNeeded == 0) continue;
                bankEntries.add(new BankEntry(ingredient.getItemID(), amountNeeded));
            }
        }

        // close if no entries
        if (bankEntries.isEmpty()) {
            script.getWidgetManager().getBank().close();
        } else {
            BankEntry bankEntry = bankEntries.get(script.random(bankEntries.size()));
            script.log(PotionMixer.class, "Processing bank entry: " + bankEntry);
            if (bankEntry.amount > 0) {
                // withdraw
                boolean stackable = script.getItemManager().isStackable(bankEntry.itemID);
                boolean notEnough = bankSnapshot.getAmount(bankEntry.itemID) < bankEntry.amount;
                if (notEnough) {
                    script.log(PotionMixer.class, "Insufficient supplies. Item ID: " + bankEntry.itemID);
                    script.stop();
                    return;
                }
                script.getWidgetManager().getBank().withdraw(bankEntry.itemID, stackable ? Integer.MAX_VALUE : bankEntry.amount);
            } else {
                // deposit
                script.getWidgetManager().getBank().deposit(bankEntry.itemID, bankEntry.amount);
            }
        }
    }

    public boolean interactAndWaitForDialogue(ItemSearchResult item1, ItemSearchResult item2) {
        // use chisel on gems and wait for dialogue
        int random = script.random(2);
        ItemSearchResult interact1 = random == 0 ? item1 : item2;
        ItemSearchResult interact2 = random == 0 ? item2 : item1;
        if (interact1.interact() && interact2.interact()) {
            return script.submitHumanTask(() -> {
                DialogueType dialogueType1 = script.getWidgetManager().getDialogue().getDialogueType();
                if (dialogueType1 == null) return false;
                return dialogueType1 == DialogueType.ITEM_OPTION;
            }, 3000);
        }
        return false;
    }

    public void waitUntilFinishedProducing(Ingredient... resources) {
        AtomicReference<Map<Ingredient, Integer>> previousAmounts = new AtomicReference<>(new HashMap<>());
        for (Ingredient resource : resources) {
            previousAmounts.get().put(resource, -1);
        }
        Timer amountChangeTimer = new Timer();
        script.submitHumanTask(() -> {
            DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                // look out for level up dialogue etc.
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // sleep for a random time so we're not instantly reacting to the dialogue
                    // we do this in the task to continue updating the screen
                    script.submitTask(() -> false, script.random(1000, 4000));
                    return true;
                }
            }

            // If the amount of gems in the inventory hasn't changed in the timeout amount, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > TimeUnit.SECONDS.toMillis(AMOUNT_CHANGE_TIMEOUT_SECONDS)) {
                return true;
            }

            inventorySnapshot = script.getWidgetManager().getInventory().search(selectedPotion.getIngredientIds());
            if (inventorySnapshot == null) {
                return false;
            }
            for (Ingredient resource : resources) {
                int amount = 0;
                ItemSearchResult ingredientResult = inventorySnapshot.getItem(resource.getItemID());
                if (script.getItemManager().isStackable(resource.getItemID())) {
                    ItemSearchResult itemResult = inventorySnapshot.getItem(resource.getItemID());
                    if (itemResult != null) {
                        amount = ingredientResult.getStackAmount();
                    }
                } else {
                    amount = inventorySnapshot.getAmount(resource.getItemID());
                }
                if (amount == 0) {
                    return true;
                }
                int previousAmount = previousAmounts.get().get(resource);
                if (amount < previousAmount || previousAmount == -1) {
                    previousAmounts.get().put(resource, amount);
                    amountChangeTimer.reset();
                }
            }
            return false;
        }, 60000, false, true);
    }

    enum Task {
        MIX_POTIONS,
        HANDLE_DIALOGUE
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
