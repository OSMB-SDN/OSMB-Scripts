package com.osmb.script.herblore.method;

import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.GameState;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.Result;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.herblore.AIOHerblore;
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

import static com.osmb.script.herblore.AIOHerblore.AMOUNT_CHANGE_TIMEOUT_SECONDS;

public class PotionMixer {
    public final AIOHerblore script;
    private final String name;
    private final Potion[] values;
    private Potion selectedPotion;
    private ComboBox<ItemIdentifier> itemComboBox;

    /**
     * @param script
     * @param name   - The name of the potion mixer
     * @param values - The potions that the method provides
     */
    public PotionMixer(AIOHerblore script, String name, Potion[] values) {
        this.script = script;
        this.name = name;
        this.values = values;
    }

    @Override
    public String toString() {
        return name;
    }

    public int poll() {
        // if item action dialogue is visible, select which item
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
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
                return 0;
            }
        }
        // calculate how many slots it takes to make a single potion
        Boolean hasIngredients = hasIngredients();
        if (hasIngredients == null) {
            script.log(PotionMixer.class, "Inventory isn't visible..");
            return 0;
        } else if (hasIngredients) {
            script.log(PotionMixer.class, "We have ingredients...");
            mixPotions(selectedPotion.getIngredients());
        } else {
            script.log(PotionMixer.class, "We have no ingredients... banking!");
            script.setBank(true);
        }
        return 0;
    }


    private void mixPotions(Ingredient[] ingredients) {
        ItemSearchResult[] ingredientResults = new ItemSearchResult[ingredients.length];
        for (int i = 0; i < ingredients.length; i++) {
            Ingredient ingredient = ingredients[i];
            if (!script.getItemManager().isStackable(ingredient.getItemID())) {
                UIResultList<ItemSearchResult> items = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
                if (!checkItemResult(items)) {
                    return;
                }
                ingredientResults[i] = items.getRandom();
            } else {
                UIResult<ItemSearchResult> items = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
                if (!checkItemResult(items)) {
                    return;
                }
                ingredientResults[i] = items.get();
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

    private Boolean hasIngredients() {
        Ingredient[] ingredientsList = selectedPotion.getIngredients();
        for (Ingredient ingredient : ingredientsList) {
            if (script.getItemManager().isStackable(ingredient.getItemID())) {
                UIResult<ItemSearchResult> ingredientsInventory = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
                if (ingredientsInventory.isNotVisible()) {
                    script.log(PotionMixer.class, "Inventory not visible");
                    return null;
                }
                if (ingredientsInventory.isNotFound()) {
                    return false;
                }
            } else {
                UIResultList<ItemSearchResult> ingredientsInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
                if (ingredientsInventory.isNotVisible()) {
                    script.log(PotionMixer.class, "Inventory not visible");
                    return null;
                }
                if (ingredientsInventory.size() < ingredient.getAmount()) {
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

    private int[] getItemsToNotDeposit() {
        Ingredient[] ingredients = selectedPotion.getIngredients();
        List<Ingredient> ingredientsList = Arrays.asList(ingredients);
        ingredients = ingredientsList.toArray(new Ingredient[0]);

        int[] itemsToIgnore = new int[ingredients.length];

        for (int i = 0; i < ingredients.length; i++) {
            itemsToIgnore[i] = ingredients[i].getItemID();
        }
        return itemsToIgnore;
    }

    public int handleBankInterface() {
        int[] itemsToIgnore = getItemsToNotDeposit();

        if (!script.getWidgetManager().getBank().depositAll(itemsToIgnore)) {
            return 0;
        }

        Optional<Integer> freeSlotsInventory = script.getItemManager().getFreeSlotsInteger(script.getWidgetManager().getInventory(), itemsToIgnore);
        if (!freeSlotsInventory.isPresent()) {
            return 0;
        }


        // calculate how many slots it takes to make a single potion
        Ingredient[] ingredientsList = selectedPotion.getIngredients();
        int slotsPerPotion = 0;
        int stackableIngredients = 0;
        for (Ingredient ingredient : ingredientsList) {
            if (script.getItemManager().isStackable(ingredient.getItemID())) {
                stackableIngredients++;
            } else {
                slotsPerPotion += ingredient.getAmount();
            }
        }

        // work out how many potions we can make
        int amountOfPotions = (freeSlotsInventory.get() - stackableIngredients) / slotsPerPotion;

        List<BankEntry> bankEntries = new ArrayList<>();
        // go over and check if we have too many
        for (Ingredient ingredient : ingredientsList) {
            UIResultList<ItemSearchResult> ingredientsInventory = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
            if (ingredientsInventory.isNotVisible()) {
                // inventory not visible
                return 0;
            }
            if (script.getItemManager().isStackable(ingredient.getItemID())) {
                if (ingredientsInventory.isEmpty() || ingredientsInventory.get(0).getStackAmount() < ingredient.getAmount()) {
                    bankEntries.add(new BankEntry(ingredient.getItemID(), Integer.MAX_VALUE));
                }
            } else {
                int inventoryAmount = ingredientsInventory.size();
                int amountNeeded = (ingredient.getAmount() * amountOfPotions) - inventoryAmount;
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
                script.getWidgetManager().getBank().withdraw(bankEntry.itemID, bankEntry.amount);
            } else {
                // deposit
                script.getWidgetManager().getBank().deposit(bankEntry.itemID, bankEntry.amount);
            }
        }
        return 0;
    }

    private Optional<Integer> getItemAmount(Ingredient ingredient) {
        ItemDefinition def = script.getItemManager().getItemDefinition(ingredient.getItemID());
        if (def.stackable == 0) {
            UIResultList<ItemSearchResult> items = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
            if (items.isNotVisible()) {
                return Optional.empty();
            }
            if (items.isNotFound()) {
                return Optional.of(0);
            }
            return Optional.of(items.size());
        } else {
            UIResult<ItemSearchResult> items = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
            if (items.isNotVisible()) {
                return Optional.empty();
            }
            if (items.isNotFound()) {
                return Optional.of(0);
            }
            return Optional.of(items.get().getStackAmount());
        }
    }

    public boolean interactAndWaitForDialogue(ItemSearchResult item1, ItemSearchResult item2) {
        // use chisel on gems and wait for dialogue
        int random = script.random(1);
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

    public boolean checkItemResult(Result uiResult) {
        if (uiResult.isNotVisible()) {
            return false;
        }
        if (uiResult.isNotFound()) {
            script.setBank(true);
            return false;
        }
        return true;
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
            if (!script.getWidgetManager().getInventory().open()) {
                return false;
            }

            for (Ingredient resource : resources) {
                ItemDefinition def = script.getItemManager().getItemDefinition(resource.getItemID());
                if (def == null) {
                    throw new RuntimeException("Definition is null for ID: " + resource);
                }
                int amount;
                if (def.stackable == 0) {
                    UIResultList<ItemSearchResult> resourceResult = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), resource.getItemID());
                    if (resourceResult.isNotFound()) {
                        return false;
                    }
                    amount = resourceResult.size();
                } else {
                    UIResult<ItemSearchResult> resourceResult = script.getItemManager().findItem(script.getWidgetManager().getInventory(), resource.getItemID());
                    if (!resourceResult.isFound()) {
                        return false;
                    }
                    amount = resourceResult.get().getStackAmount();
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
        }, 60000, true, false, true);
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
