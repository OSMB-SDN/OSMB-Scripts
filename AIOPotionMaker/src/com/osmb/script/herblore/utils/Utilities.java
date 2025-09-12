package com.osmb.script.herblore.utils;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.script.herblore.data.Ingredient;
import com.osmb.script.herblore.data.Potion;

import static com.osmb.script.herblore.Config.selectedPotion;

public class Utilities {

    /**
     * Checks if the player has the ingredients required to make the selected potion.
     *
     * @param core the script core
     * @return true if the player has the ingredients, false otherwise
     */
    public static boolean hasIngredients(ScriptCore core, ItemGroupResult inventorySnapshot) {
        Ingredient[] ingredientsList = selectedPotion.getIngredients();
        for (Ingredient ingredient : ingredientsList) {
            if (core.getItemManager().isStackable(ingredient.getItemID())) {
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

    /**
     * Gets the item IDs of the ingredients and the potion itself for use in dialogues.
     * We have all the relating items to search the dialogue for, as sometimes jagex switches items in the dialogue.
     *
     * @param selectedPotion the potion to get the item IDs for
     * @return an array of item IDs
     */
    public static int[] getDialogueItemIds(Potion selectedPotion) {
        int[] dialogueIds = new int[selectedPotion.getIngredients().length + 1];
        // add ingredients to array
        for (int i = 0; i < selectedPotion.getIngredients().length; i++) {
            dialogueIds[i] = selectedPotion.getIngredients()[i].getItemID();
        }
        // add potion to array (this is because jagex is sneaky and sometimes switches items in dialogues)
        dialogueIds[dialogueIds.length - 1] = selectedPotion.getItemID();
        return dialogueIds;
    }

    /**
     * Calculates how many potions we can make based on the current inventory snapshot and the ingredients required.
     * This is done by calculating how many slots we have to work with, and how many slots are required per potion.
     * Stackable ingredients are treated differently as they only take up 1 slot for all potions.
     *
     * @param core              the script core
     * @param ingredients       the ingredients required to make the potion
     * @return the number of potions we can make based on the slots available in the inventory
     */
    public static int calculatePotionAmount(ScriptCore core, Ingredient[] ingredients) {
        int slotsToWorkWith = 28;
        // calculate how many slots it takes to make a single potion
        int slotsPerPotion = 0;
        // treat stackable ingredients differently as they only take up 1 slot for ALL potions
        int stackableIngredients = 0;

        for (Ingredient ingredient : ingredients) {
            if (core.getItemManager().isStackable(ingredient.getItemID())) {
                // if stackable
                stackableIngredients++;
            } else {
                // if not stackable
                slotsPerPotion += ingredient.getAmount();
            }
        }
        // work out how many potions we can make
        return (slotsToWorkWith - stackableIngredients) / slotsPerPotion;
    }

}
