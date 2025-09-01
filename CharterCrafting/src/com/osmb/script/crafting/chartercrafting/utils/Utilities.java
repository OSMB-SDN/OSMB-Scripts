package com.osmb.script.crafting.chartercrafting.utils;

import com.osmb.api.ScriptCore;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;

import static com.osmb.script.crafting.chartercrafting.Config.selectedGlassBlowingItem;
import static com.osmb.script.crafting.chartercrafting.Constants.SELL_OPTION_AMOUNTS;

public class Utilities {
    public static int roundDownToNearestOption(int amount) {
        if (amount < SELL_OPTION_AMOUNTS[0]) {
            return 0;
        }

        for (int i = SELL_OPTION_AMOUNTS.length - 1; i >= 0; i--) {
            if (SELL_OPTION_AMOUNTS[i] <= amount) {
                return SELL_OPTION_AMOUNTS[i];
            }
        }

        // This line should theoretically never be reached because of the first check
        return SELL_OPTION_AMOUNTS[0];
    }

    public static boolean validDialogue(ScriptCore core) {
        DialogueType dialogueType = core.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.ITEM_OPTION) {
            boolean selectedOption = core.getWidgetManager().getDialogue().selectItem(selectedGlassBlowingItem.getItemId());
            if (!selectedOption) {
                core.log(Utilities.class, "No option selected, can't find item in dialogue...");
                return false;
            }
            return true;
        }
        return false;
    }


}
