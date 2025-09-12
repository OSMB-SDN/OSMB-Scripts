package com.osmb.script.herblore.mixing;

import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.herblore.AIOPotionMaker;
import com.osmb.script.herblore.State;
import com.osmb.script.herblore.data.Ingredient;
import com.osmb.script.herblore.utils.Utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.osmb.script.herblore.Config.selectedPotion;
import static com.osmb.script.herblore.State.amountChangeTimeout;
import static com.osmb.script.herblore.State.inventorySnapshot;


public class PotionMixer {
    public final AIOPotionMaker script;

    public PotionMixer(AIOPotionMaker script) {
        this.script = script;
    }

    public void poll() {
        DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();

        if (dialogueType == DialogueType.ITEM_OPTION) {
            // if dialogue is visible, interact with it
            if (interactWithItemDialogue()) {
                waitUntilFinishedProducing(selectedPotion.getIngredients());
            } else {
                script.log(PotionMixer.class, "Can't find item in dialogue...");
            }
        } else {
            // combine ingredients & wait for dialogue
            combineIngredients(selectedPotion.getIngredients());
        }
    }

    private boolean interactWithItemDialogue() {
        script.log(PotionMixer.class, "Interacting with item dialogue");

        int[] dialogueIds = Utilities.getDialogueItemIds(selectedPotion);
        // select the item in the dialogue
        return script.getWidgetManager().getDialogue().selectItem(dialogueIds);
    }

    private void combineIngredients(Ingredient[] ingredients) {
        CombinableItems itemsToCombine = getCombinableItems(ingredients);
        if (itemsToCombine == null) {
            script.log(PotionMixer.class, "Failed getting items to combine.");
            return;
        }
        interactAndWaitForDialogue(itemsToCombine);
    }

    private CombinableItems getCombinableItems(Ingredient[] ingredients) {
        ItemSearchResult[] ingredientResults = new ItemSearchResult[ingredients.length];
        for (int i = 0; i < ingredients.length; i++) {
            Ingredient ingredient = ingredients[i];
            if (!script.getItemManager().isStackable(ingredient.getItemID())) {
                ingredientResults[i] = inventorySnapshot.getRandomItem(ingredient.getItemID());
            } else {
                ingredientResults[i] = inventorySnapshot.getItem(ingredient.getItemID());
            }
        }
        int index1 = -1;
        // find mandatory ingredient if any
        for (int i = 0; i < ingredients.length; i++) {
            if (ingredients[i].isMandatoryToCombine()) {
                index1 = i;
                script.log(PotionMixer.class, "Mandatory ingredient found: " + ingredients[i].getItemID());
                break;
            }
        }
        // if no mandatory ingredient, pick a random one
        if (index1 == -1) {
            index1 = script.random(ingredientResults.length);
        }

        if (index1 == -1) {
            script.log(PotionMixer.class, "Failed finding ingredient index.");
            return null;
        }

        // pick a different random ingredient which is not the same as index1
        int index2;
        do {
            index2 = script.random(ingredientResults.length);
        } while (index2 == index1);
        return new CombinableItems(ingredientResults[index1], ingredientResults[index2]);
    }

    private void interactAndWaitForDialogue(CombinableItems combinableItems) {
        // use chisel on gems and wait for dialogue
        if (combinableItems.item1.interact() && combinableItems.item2.interact()) {
            script.pollFramesHuman(() -> {
                DialogueType dialogueType1 = script.getWidgetManager().getDialogue().getDialogueType();
                if (dialogueType1 == null) return false;
                return dialogueType1 == DialogueType.ITEM_OPTION;
            }, 3000);
        }
    }

    public void waitUntilFinishedProducing(Ingredient... resources) {
        AtomicReference<Map<Ingredient, Integer>> previousAmounts = new AtomicReference<>(new HashMap<>());
        for (Ingredient resource : resources) {
            previousAmounts.get().put(resource, -1);
        }
        Timer amountChangeTimer = new Timer();
        script.pollFramesHuman(() -> {
            DialogueType dialogueType = script.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                // look out for level up dialogue etc.
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // sleep for a random time so we're not instantly reacting to the dialogue
                    // we do this in the task to continue updating the screen
                    script.pollFramesUntil(() -> false, script.random(1000, 4000));
                    return true;
                }
            }
            ItemListenResult itemListenResult = itemsChanged(previousAmounts, resources);
            if (itemListenResult == ItemListenResult.FAILED) {
                return false;
            }
            if (itemListenResult == ItemListenResult.NO_ITEM) {
                return true;
            }
            if (itemListenResult == ItemListenResult.AMOUNT_CHANGED) {
                State.resetAmountChangeTimeout();
                amountChangeTimer.reset();
            }
            // check if items haven't changed within timeout
            return amountChangeTimer.timeElapsed() > amountChangeTimeout;
        }, 60000, true);
    }

    private ItemListenResult itemsChanged(AtomicReference<Map<Ingredient, Integer>> previousAmounts, Ingredient... ingredients) {
        inventorySnapshot = script.getWidgetManager().getInventory().search(selectedPotion.getIngredientIds());
        if (inventorySnapshot == null) {
            return ItemListenResult.FAILED;
        }
        for (Ingredient resource : ingredients) {
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
                return ItemListenResult.NO_ITEM;
            }
            int previousAmount = previousAmounts.get().get(resource);
            if (amount < previousAmount || previousAmount == -1) {
                previousAmounts.get().put(resource, amount);
                return ItemListenResult.AMOUNT_CHANGED;
            }
        }
        return ItemListenResult.NO_CHANGE;
    }

    enum ItemListenResult {
        AMOUNT_CHANGED,
        NO_CHANGE,
        NO_ITEM,
        FAILED
    }

    private static class CombinableItems {
        ItemSearchResult item1;
        ItemSearchResult item2;

        public CombinableItems(ItemSearchResult item1, ItemSearchResult item2) {
            this.item1 = item1;
            this.item2 = item2;
        }
    }
}
