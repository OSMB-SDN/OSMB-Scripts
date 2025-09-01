package com.osmb.script.crafting.chartercrafting;

import com.osmb.api.item.ItemID;
import com.osmb.api.scene.RSObject;
import com.osmb.script.crafting.chartercrafting.component.ShopInterface;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static com.osmb.script.crafting.chartercrafting.Config.selectedDock;

public class Constants {
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    public static final int[] SELL_OPTION_AMOUNTS = new int[]{1, 5, 10, 50};
    public static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.GLASSBLOWING_PIPE, ItemID.MOLTEN_GLASS, ItemID.BUCKET_OF_SAND, ItemID.SODA_ASH, ItemID.SEAWEED, ItemID.BUCKET));

    public static final Predicate<RSObject> BANK_QUERY = gameObject -> {
        // if object has no name
        String name = gameObject.getName();

        if (name == null) {
            return false;
        }


        if (selectedDock == Dock.CORSAIR_COVE) {
            // handle closed bank
            if (gameObject.getWorldX() != 2569 || gameObject.getWorldY() != 2865) {
                return false;
            }
            if (!name.equalsIgnoreCase("Closed booth")) {
                return false;
            }

        } else {
            // has no interact options (eg. bank, open etc.)
            if (gameObject.getActions() == null) {
                return false;
            }

            if (!Arrays.stream(BANK_NAMES).anyMatch(bankName -> bankName.equalsIgnoreCase(name))) {
                return false;
            }

            // if no actions contain bank or open
            if (!Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
                return false;
            }
        }
        // final check is if the object is reachable
        return gameObject.canReach();
    };
}
