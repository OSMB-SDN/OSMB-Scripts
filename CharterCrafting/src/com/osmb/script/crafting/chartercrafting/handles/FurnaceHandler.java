package com.osmb.script.crafting.chartercrafting.handles;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.ui.chatbox.dialogue.Dialogue;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.walker.WalkConfig;
import com.osmb.script.crafting.chartercrafting.Method;

import static com.osmb.script.crafting.chartercrafting.Config.selectedDock;
import static com.osmb.script.crafting.chartercrafting.Config.selectedMethod;
import static com.osmb.script.crafting.chartercrafting.utils.Utilities.waitUntilFinishedProducing;

public class FurnaceHandler {

    private final ScriptCore core;
    private final Dialogue dialogue;
    public FurnaceHandler(ScriptCore core) {
        this.core = core;
        this.dialogue = core.getWidgetManager().getDialogue();
    }

    public void poll() {
        Area furnaceArea = selectedDock.getFurnaceArea();
        if (furnaceArea == null) {
            throw new RuntimeException("No furnace area for selected dock.");
        }

        RSObject furnace = core.getObjectManager().getClosestObject("Furnace");
        if (furnace == null) {
            // walk to furnace area if no furnace in our loaded scene
            walkToFurnace();
            return;
        }
        WorldPosition position = core.getWorldPosition();
        if (position == null) {
            return;
        }
        if (furnaceArea.contains(position)) {
            // check for dialogue
            DialogueType dialogueType = dialogue.getDialogueType();
            if (dialogueType == DialogueType.ITEM_OPTION) {
                boolean selectedOption = dialogue.selectItem(ItemID.MOLTEN_GLASS);
                if (!selectedOption) {
                    core.log(FurnaceHandler.class, "No option selected, can't find item in dialogue...");
                    return;
                }
                waitUntilFinishedProducing(core, Integer.MAX_VALUE, selectedMethod == Method.SUPER_GLASS_MAKE ? ItemID.SEAWEED : ItemID.SODA_ASH, ItemID.BUCKET_OF_SAND);
                return;
            }
        }

        if (furnace.interact("Smelt")) {
            // sleep until dialogue is visible
            core.pollFramesHuman(() -> dialogue.getDialogueType() == DialogueType.ITEM_OPTION, RandomUtils.uniformRandom(2000, 7000));
        }
    }

    private void walkToFurnace() {
        core.log(FurnaceHandler.class, "Walking to furnace");
        WalkConfig.Builder walkConfig = new WalkConfig.Builder();
        walkConfig.breakCondition(() -> {
            WorldPosition myPosition = core.getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            return selectedDock.getFurnaceArea().contains(myPosition);
        });
        core.getWalker().walkTo(selectedDock.getFurnaceArea().getRandomPosition(), walkConfig.build());
    }


}
