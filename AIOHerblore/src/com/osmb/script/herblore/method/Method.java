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

public class Method {
    public final AIOHerblore script;
    private final String name;
    private final Potion[] values;
    private Map<Ingredient, ItemDefinition> itemDefinition = new HashMap<>();
    private Potion selectedPotion;
    private ComboBox<ItemIdentifier> itemComboBox;

    public Method(AIOHerblore script, String name, Potion[] values) {
        this.script = script;
        this.name = name;
        this.values = values;
    }

    @Override
    public String toString() {
        return name;
    }

    public int poll() {
        // check ingredients
        Ingredient[] ingredients = selectedPotion.getIngredients();
        ItemSearchResult[] ingredientResults = new ItemSearchResult[ingredients.length];
        for (int i = 0; i < ingredients.length; i++) {
            Ingredient ingredient = ingredients[i];
            if (!script.getItemManager().isStackable(ingredient.getItemID())) {
                UIResultList<ItemSearchResult> items = script.getItemManager().findAllOfItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
                if (!checkItemResult(items)) {
                    return 0;
                }
                ingredientResults[i] = items.getRandom();
            } else {
                UIResult<ItemSearchResult> items = script.getItemManager().findItem(script.getWidgetManager().getInventory(), ingredient.getItemID());
                if (!checkItemResult(items)) {
                    return 0;
                }
                ingredientResults[i] = items.get();
            }
        }
        // if item action dialogue is visible, select which item
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selectedOption = script.getWidgetManager().getDialogue().selectItem(selectedPotion.getItemID());
            if (!selectedOption) {
                script.log(getClass().getSimpleName(), "No option selected, can't find item in dialogue...");
                return 0;
            }
            waitUntilFinishedProducing(ingredients);
            return 0;
        }
        if (!script.getItemManager().unSelectItemIfSelected()) {
            return 0;
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
            script.log(Method.class, "Failed finding ingredient index. Mandatory ingredient: " + (mandatoryIngredient != null));
            return 0;
        }
        int index2;
        do {
            index2 = script.random(ingredientResults.length);
        } while (index2 == index1);

        interactAndWaitForDialogue(ingredientResults[index1], ingredientResults[index2]);
        return 0;
    }

    public boolean uiOptionsSufficient() {
        if (itemComboBox.getValue() != null) {
            selectedPotion = (Potion) itemComboBox.getValue();
            return true;
        }
        return false;
    }

    public void provideUIOptions(VBox vBox) {
        Label itemLabel = new Label("Choose potion to make");
        itemComboBox = ScriptOptions.createItemCombobox(script, values);
        vBox.getChildren().addAll(itemLabel, itemComboBox);
        vBox.requestLayout();
    }

    public void onGamestateChanged(GameState gameState) {
    }


    protected ItemDefinition getItemDefinition(Ingredient ingredient) {
        if (itemDefinition.containsKey(ingredient)) {
            return itemDefinition.get(ingredient);
        }
        ItemDefinition itemDefinition1 = script.getItemManager().getItemDefinition(ingredient.getItemID());
        if (itemDefinition1 != null) {
            itemDefinition.put(ingredient, itemDefinition1);
            return itemDefinition1;
        }
        return null;
    }

    // final as we don't allow overriding this method
    public final int handleBankInterface() {
        Ingredient[] ingredients = selectedPotion.getIngredients();
        //shuffle array
        List<Ingredient> ingredientsList = Arrays.asList(ingredients);
        Collections.shuffle(ingredientsList);
        ingredients = ingredientsList.toArray(new Ingredient[0]);

        int[] itemsToIgnore = new int[ingredients.length];
        int slotsPerItem = 0;

        for (int i = 0; i < ingredients.length; i++) {
            itemsToIgnore[i] = ingredients[i].getItemID();
            slotsPerItem += ingredients[i].getAmount();
        }
        if (!script.getWidgetManager().getBank().depositAll(itemsToIgnore)) {
            return 0;
        }

        Optional<Integer> freeSlotsInventory = script.getItemManager().getFreeSlotsInteger(script.getWidgetManager().getInventory(), itemsToIgnore);
        if (!freeSlotsInventory.isPresent()) {
            return 0;
        }

        // work out how many potions we can make
        int amountOfPotions = freeSlotsInventory.get() / slotsPerItem;

        // go over and check if we have too many
        for (int i = 0; i < ingredients.length; i++) {
            Ingredient ingredient = ingredients[i];
            int amountNeeded = ingredient.getAmount() * amountOfPotions;
            Optional<Integer> ingredientAmount = getItemAmount(ingredient);
            if (!ingredientAmount.isPresent()) {
                return 0;
            }
            if (!script.getItemManager().isStackable(ingredient.getItemID())) {
                // deposit
                if (ingredientAmount.get() != amountNeeded) {
                    if (!script.getWidgetManager().getBank().deposit(ingredient.getItemID(), Integer.MAX_VALUE)) {
                        return 0;
                    }
                }
            }
        }
        // withdraw
        for (int i = 0; i < ingredients.length; i++) {
            Ingredient ingredient = ingredients[i];
            int amountNeeded = ingredient.getAmount() * amountOfPotions;
            Optional<Integer> ingredientAmount = getItemAmount(ingredient);
            if (!ingredientAmount.isPresent()) {
                return 0;
            }
            if (ingredientAmount.get() != amountNeeded) {
                // check if we have in the bank
                UIResult<ItemSearchResult> ingredientBank = script.getItemManager().findItem(script.getWidgetManager().getBank(), ingredient.getItemID());
                if (ingredientBank.isNotVisible()) {
                    return 0;
                }
                if (ingredientBank.isNotFound()) {
                    ItemDefinition def = getItemDefinition(ingredient);
                    script.log(def.name + " not found in bank, stopping script...");
                    script.stop();
                    return 0;
                }
                if (!script.getWidgetManager().getBank().withdraw(ingredient.getItemID(), amountNeeded)) {
                    return 0;
                }
            }
        }

        script.getWidgetManager().getBank().close();
        return 0;
    }

    private Optional<Integer> getItemAmount(Ingredient ingredient) {
        ItemDefinition def = getItemDefinition(ingredient);
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
}
