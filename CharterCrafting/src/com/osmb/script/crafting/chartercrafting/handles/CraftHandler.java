package com.osmb.script.crafting.chartercrafting.handles;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.spellbook.LunarSpellbook;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.crafting.chartercrafting.Method;
import com.osmb.script.crafting.chartercrafting.utils.Utilities;

import static com.osmb.script.crafting.chartercrafting.Config.*;
import static com.osmb.script.crafting.chartercrafting.Constants.*;
import static com.osmb.script.crafting.chartercrafting.State.*;
import static com.osmb.script.crafting.chartercrafting.utils.Utilities.*;

public class CraftHandler {
    private final ScriptCore core;

    public CraftHandler(ScriptCore core) {
        this.core = core;
    }


    public void craft(ItemSearchResult glassblowingPipe, ItemSearchResult moltenGlass) {
        core.log(CraftHandler.class, "Crafting Molten glass...");
        WorldPosition myPosition = core.getWorldPosition();
        if (myPosition == null) {
            return;
        }
        Area wanderArea = selectedDock.getWanderArea();
        if (wanderArea.contains(myPosition)) {
            craft(glassblowingPipe, moltenGlass, Integer.MAX_VALUE);
        } else {
            // walk to wander area and craft
            WalkConfig.Builder walkConfig = new WalkConfig.Builder().disableWalkScreen(true).tileRandomisationRadius(2);
            walkConfig.doWhileWalking(() -> {
                core.log(CraftHandler.class, "Crafting while walking...");
                inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                if (inventorySnapshot == null) {
                    // inventory not visible
                    return null;
                }
                if (!inventorySnapshot.contains(ItemID.MOLTEN_GLASS)) {
                    // no molten glass to craft
                    return null;
                }
                if (!inventorySnapshot.contains(ItemID.GLASSBLOWING_PIPE)) {
                    core.log(CraftHandler.class, "No glassblowing pipe found.");
                    core.stop();
                    return null;
                }
                craft(inventorySnapshot.getItem(ItemID.GLASSBLOWING_PIPE), inventorySnapshot.getRandomItem(ItemID.MOLTEN_GLASS), RandomUtils.uniformRandom(4000, 12000));
                return null;
            });
            core.getWalker().walkTo(wanderArea.getRandomPosition(), walkConfig.build());
        }
    }

    private void craft(ItemSearchResult glassblowingPipe, ItemSearchResult moltenGlass, int timeout) {
        if (!core.getWidgetManager().getInventory().unSelectItemIfSelected()) {
            core.log(CraftHandler.class, "Failed to unselect item.");
            return;
        }
        if (Utilities.validDialogue(core)) {
            waitUntilFinishedProducing(core, timeout, ItemID.MOLTEN_GLASS);
            return;
        }
        core.log(CraftHandler.class, "Interacting...");
        combineAndWaitForDialogue(glassblowingPipe, moltenGlass);

        // only double call due to the walking method
        if (Utilities.validDialogue(core)) {
            waitUntilFinishedProducing(core, timeout, ItemID.MOLTEN_GLASS);
        }
    }


    public boolean combineAndWaitForDialogue(ItemSearchResult item1, ItemSearchResult item2) {
        int random = RandomUtils.uniformRandom(2);
        ItemSearchResult interact1 = random == 0 ? item1 : item2;
        ItemSearchResult interact2 = random == 0 ? item2 : item1;
        if (interact1.interact() && interact2.interact()) {
            return core.pollFramesHuman(() -> core.getWidgetManager().getDialogue().getDialogueType() == DialogueType.ITEM_OPTION, 3000);
        }
        return false;
    }


    public void superGlassMake() {
        try {
            if (core.getWidgetManager().getSpellbook().selectSpell(LunarSpellbook.SUPERGLASS_MAKE, null)) {
                // generate human response after selecting spell
                core.pollFramesHuman(() -> true, 100);
                // check inventory
                core.pollFramesHuman(() -> {
                    inventorySnapshot = core.getWidgetManager().getInventory().search(ITEM_IDS_TO_RECOGNISE);
                    if (inventorySnapshot == null) {
                        return false;
                    }
                    int combinationItem = selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH;
                    return !inventorySnapshot.contains(combinationItem) || !inventorySnapshot.contains(ItemID.BUCKET_OF_SAND);
                }, 5000);
                // randomise next amount
                nextCraftAmount(inventorySnapshot);
            }
        } catch (SpellNotFoundException e) {
            core.log(CraftHandler.class, "Spell sprite not found, stopping script...");
            core.stop();
        }
    }

}
