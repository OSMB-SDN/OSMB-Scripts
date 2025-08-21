package com.osmb.script.potion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface Potion {

    List<Integer> getItemIDs();

    String getName();

    static Potion getPotionForID(int id) {
        for (Potion potion : StandardPotion.values()) {
            if (potion.getItemIDs().contains(id)) {
                return potion;
            }
        }
        for (Potion potion : BarrelPotion.values()) {
            if (potion.getItemIDs().contains(id)) {
                return potion;
            }
        }
        return null;
    }

    default int getDose(int id) {
        List<Integer> itemIDs = new ArrayList<>(getItemIDs());
        Collections.reverse(itemIDs);
        return itemIDs.indexOf(id) + 1;
    }

    default int getItemIDForDose(int dose) {
        if (dose < 0 || dose >= getItemIDs().size()) {
            throw new IllegalArgumentException("Invalid dose: " + dose);
        }
        List<Integer> itemIDs = new ArrayList<>(getItemIDs());
        Collections.reverse(itemIDs);
        return itemIDs.get(dose-1);
    }

    default int getFullID() {
        return getItemIDs().get(0);
    }
}
