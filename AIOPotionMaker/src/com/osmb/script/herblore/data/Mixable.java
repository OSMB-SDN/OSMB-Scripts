package com.osmb.script.herblore.data;

import java.util.HashSet;
import java.util.Set;

public interface Mixable {

    Ingredient[] getIngredients();

    default Set<Integer> getIngredientIds() {
        Set<Integer> ids = new HashSet<>();
        for (Ingredient ingredient : getIngredients()) {
            ids.add(ingredient.getItemID());
        }
        return ids;
    }
}
