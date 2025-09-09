package com.osmb.script.crafting.chartercrafting.utils;

import com.osmb.api.ScriptCore;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.crafting.chartercrafting.CharterCrafting;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.osmb.script.crafting.chartercrafting.Config.*;
import static com.osmb.script.crafting.chartercrafting.Constants.*;
import static com.osmb.script.crafting.chartercrafting.State.*;

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

    public static void waitUntilFinishedProducing(ScriptCore core, int timeout, int... resources) {
        core.log(Utilities.class, "Waiting until we've finished producing...");
        AtomicReference<Stopwatch> stopwatch = new AtomicReference<>(new Stopwatch(timeout));
        AtomicReference<Map<Integer, Integer>> previousAmounts = new AtomicReference<>(new HashMap<>());
        for (int resource : resources) {
            previousAmounts.get().put(resource, -1);
        }
        Timer amountChangeTimer = new Timer();
        core.pollFramesHuman(() -> {
            DialogueType dialogueType = core.getWidgetManager().getDialogue().getDialogueType();
            if (dialogueType != null) {
                // look out for level up dialogue etc.
                // we can check the dialogue text specifically if it is a level up dialogue,
                // no point though as if we're interrupted we want to break out the loop anyway
                if (dialogueType == DialogueType.TAP_HERE_TO_CONTINUE) {
                    // sleep for a random time so we're not instantly reacting to the dialogue
                    // we do this in the task to continue updating the screen
                    core.log(CharterCrafting.class, "Tap here to continue dialogue interrupted us, generating extra random time to react...");
                    // submitHumanTask already gives a delay afterward on completion,
                    // but we want a bit of extra time on top as the user won't always be expecting the dialogue
                    core.pollFramesUntil(() -> false, RandomUtils.uniformRandom(1000, 6000));
                    // return true and execute the shorter generated human delay by submitHumanTask
                    return true;
                }
            }

            WorldPosition myPosition = core.getWorldPosition();
            if (myPosition != null) {
                if (!selectedDock.getWanderArea().contains(myPosition) && stopwatch.get().hasFinished()) {
                    return true;
                }
            }
            inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
            if (inventorySnapshot == null) {
                return false;
            }

            for (int resource : resources) {
                int amount = inventorySnapshot.getAmount(resource);
                if (amount == 0) {
                    return true;
                }
                int previousAmount = previousAmounts.get().get(resource);
                if (amount < previousAmount || previousAmount == -1) {
                    previousAmounts.get().put(resource, amount);
                    amountChangeTimer.reset();
                }
            }

            // If the amount of resources in the inventory hasn't changed and the timeout is exceeded, then return true to break out of the sleep method
            if (amountChangeTimer.timeElapsed() > amountChangeTimeout) {
                resetAmountChangeTimeout();
                return true;
            }

            return false;
        }, 90000, true);
    }

}
