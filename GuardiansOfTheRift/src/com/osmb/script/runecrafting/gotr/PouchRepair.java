package com.osmb.script.runecrafting.gotr;

import com.osmb.api.input.MenuEntry;
import com.osmb.api.input.MenuHook;
import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.shape.Polygon;
import com.osmb.api.ui.chatbox.dialogue.DialogueType;
import com.osmb.api.ui.component.chatbox.ChatboxComponent;
import com.osmb.api.ui.spellbook.LunarSpellbook;
import com.osmb.api.ui.spellbook.SpellNotFoundException;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.UIResultList;
import com.osmb.api.utils.Utils;
import com.osmb.api.walker.WalkConfig;

import java.util.List;

import static com.osmb.script.runecrafting.gotr.GuardiansOfTheRift.BOSS_AREA;


public class PouchRepair {
    private static final Area CORDELIA_AREA = new RectangleArea(3615, 9489, 2, 2, 0);
    private static final Area CORDELIA_GUARANTEED_AREA = new RectangleArea(3616, 9489, 1, 2, 0);
    private static final String[] CORDELIA_DIALOGUE = {
            "hi, it's me again",
            "hello, i know you can hear me",
            "ding dong, ding dong. you can't keep me waiting",
            "fine. what do you want?",
            "i got someone here in need of a pouch repair",
            "ok...it's done",
            "your pouches have been repaired",
            "It will cost you 1 abyssal pearl to repair pouches"
    };
    private static final String[] DIALOGUE_TITLES = {
            "apprentice cordelia",
            "dark mage",
    };
    private static final String FINISHED_DIALOGUE = "you don't seem to have any pouches in need of repair";
    private static final String[] DARK_MAGE_DIALOGUE = {
            "What do you want? Can't you see I'm busy?",
            "Can you repair my pouches?",
    };
    private static final String COMPLETED_DIALOGUE = "Your pouches have been repaired.";
    private static final String REPAIR_OPTION = "Can you repair my pouches?";
    private final GuardiansOfTheRift core;
    private final Pouch.RepairType repairType;

    public PouchRepair(GuardiansOfTheRift core, Pouch.RepairType repairType) {
        this.core = core;
        this.repairType = repairType;
    }

    public void poll() {
        switch (repairType) {
            case NPC_CONTACT -> castNPCContact();
            case APPRENTICE_CORDELIA -> repairNPC();
        }
    }

    private void repairNPC() {
        if (isCordeliaDialogue()) {
            handleDialogue();
            return;
        }
        WorldPosition worldPosition = core.getWorldPosition();
        if (worldPosition == null) {
            core.log(GuardiansOfTheRift.class, "My position is null, cannot repair pouches.");
            return;
        }
        if (BOSS_AREA.contains(worldPosition)) {
            UIResultList<WorldPosition> npcPositions = core.getWidgetManager().getMinimap().getNPCPositions();
            if (npcPositions.isNotVisible()) {
                core.log(PouchRepair.class, "Minimap not visible....");
                return;
            }
            if (npcPositions.isEmpty()) {
                // walk to area
                walkToArea();
                return;
            }
            List<WorldPosition> cordeliaPositions = getCordeliaPosition(npcPositions);
            if (cordeliaPositions.isEmpty()) {
                core.log(GuardiansOfTheRift.class, "Cordelia not found in the area, waiting...");
                walkToArea();
                return;
            }
            if (cordeliaPositions.size() > 1) {
                cordeliaPositions = cordeliaPositions.stream().filter(CORDELIA_GUARANTEED_AREA::contains).toList();
            }
            if (cordeliaPositions.isEmpty()) {
                core.log(GuardiansOfTheRift.class, "Waiting for cordelia to step into guaranteed bounds...");
                return;
            }
            if (cordeliaPositions.size() > 1) {
                core.log(GuardiansOfTheRift.class, "Multiple positions found");
                return;
            }
            core.log(GuardiansOfTheRift.class, "Cordelia found at: " + cordeliaPositions.get(0));
            interactWithCordelia(cordeliaPositions.get(0));
        } else {
            core.log(GuardiansOfTheRift.class, "Not in the boss area...");
        }
    }

    private void handleDialogue() {
        DialogueType dialogueType = core.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == null) {
            core.log(GuardiansOfTheRift.class, "Dialogue type is null, cannot handle dialogue.");
            return;
        }
        core.log(GuardiansOfTheRift.class, "Dialogue type: " + dialogueType);
        if (dialogueType == DialogueType.CHAT_DIALOGUE) {
            core.log(GuardiansOfTheRift.class, "Continuing dialogue...");
            core.getWidgetManager().getDialogue().continueChatDialogue();
        }
    }

    //
    private boolean isCordeliaDialogue() {
        UIResult<String> dialogueTitle = core.getWidgetManager().getDialogue().getDialogueTitle();
        UIResult<String> dialogueText = core.getWidgetManager().getDialogue().getText();

        if (dialogueTitle.isFound() && dialogueText.isFound()) {
            String title = dialogueTitle.get();
            String text = dialogueText.get();
            core.log("GuardiansOfTheRift", "Dialogue title: " + title);
            core.log("GuardiansOfTheRift", "Dialogue text: " + text);
            if (text.equals(FINISHED_DIALOGUE)) {
                core.log(GuardiansOfTheRift.class, "Finished repairing pouches.");
                return true;
            }
            for (String dialogue : CORDELIA_DIALOGUE) {
                if (text.toLowerCase().contains(dialogue.toLowerCase())) {
                    return true;
                }
            }
            return title.toLowerCase().contains("dark mage");
        }
        return false;
    }

    private void interactWithCordelia(WorldPosition cordeliaPosition) {
        Polygon tilePoly = core.getSceneProjector().getTileCube(cordeliaPosition, 80);
        if (tilePoly == null || (tilePoly = tilePoly.getResized(0.7)) == null || !core.getWidgetManager().insideGameScreen(tilePoly, List.of(ChatboxComponent.class))) {
            walkToArea();
            return;
        }
        MenuHook menuHook = menuEntries -> {
            boolean containsTalk = false;
            for (MenuEntry menuEntry : menuEntries) {
                String rawText = menuEntry.getRawText();
                if (rawText.startsWith("repair apprentice cordelia")) {
                    return menuEntry;
                } else if (rawText.startsWith("talk-to apprentice cordelia")) {
                    containsTalk = true;
                }
            }
            if (containsTalk) {
                // if not unlocked
                core.log(GuardiansOfTheRift.class, "Cordelia pouch repair is not unlocked, cannot repair pouches.");
                core.stop();
            }
            return null;
        };
        if (core.getFinger().tapGameScreen(tilePoly, menuHook)) {
            core.submitTask(() -> core.getWidgetManager().getDialogue().getDialogueType() != null, Utils.random(6000, 9000));
        }
    }

    private List<WorldPosition> getCordeliaPosition(UIResultList<WorldPosition> npcPositions) {
        return npcPositions.asList().stream().filter(CORDELIA_AREA::contains).toList();
    }

    private void walkToArea() {
        core.getWalker().walkTo(CORDELIA_AREA.getRandomPosition(), new WalkConfig.Builder().breakCondition(() -> {
            WorldPosition myPosition = core.getWorldPosition();
            if (myPosition == null) {
                return false;
            }
            return CORDELIA_AREA.contains(myPosition);
        }).build());
    }

    private void castNPCContact() {
        core.log(PouchRepair.class, "Casting NPC Contact to repair pouches...");
        if (handleNPCContactDialogue()) {
            return;
        }
        core.log(GuardiansOfTheRift.class, "Attempting to cast NPC Contact spell...");
        try {
            if (core.getWidgetManager().getSpellbook().selectSpell(LunarSpellbook.NPC_CONTACT, "dark mage", null)) {
                // generate human response after selecting spell
                core.submitHumanTask(() -> true, 100);
                core.submitHumanTask(() -> core.getWidgetManager().getDialogue().isVisible(), 5000);
                handleNPCContactDialogue();
            }
        } catch (SpellNotFoundException e) {
            core.log(GuardiansOfTheRift.class, "Spell sprite not found, stopping script...");
            core.stop();
        }
    }

    private boolean handleNPCContactDialogue() {
        if (isNPCContactDialogue()) {
            core.submitHumanTask(() -> {
                if (!isNPCContactDialogue()) {
                    core.log(GuardiansOfTheRift.class, "NPC Contact dialogue not found, breaking out of task..");
                    return true;
                }
                DialogueType dialogueType = core.getWidgetManager().getDialogue().getDialogueType();
                if (dialogueType == DialogueType.CHAT_DIALOGUE) {
                    core.log(GuardiansOfTheRift.class, "Continuing chat dialogue...");
                    core.getWidgetManager().getDialogue().continueChatDialogue();
                } else if (dialogueType == DialogueType.TEXT_OPTION) {
                    core.log(GuardiansOfTheRift.class, "Selecting repair option...");
                    core.getWidgetManager().getDialogue().selectOption(REPAIR_OPTION);
                }
                return false;
            }, core.random(15000, 20000), false, true);
            return true;
        }
        return false;
    }

    private boolean isNPCContactDialogue() {
        DialogueType dialogueType = core.getWidgetManager().getDialogue().getDialogueType();
        if (dialogueType == DialogueType.CHAT_DIALOGUE) {
            UIResult<String> dialogueText = core.getWidgetManager().getDialogue().getText();
            if (dialogueText.isFound()) {
                String text = dialogueText.get();
                for (String dialogue : DARK_MAGE_DIALOGUE) {
                    if (text.toLowerCase().contains(dialogue.toLowerCase())) {
                        return true;
                    }
                }
            }
        } else if (dialogueType == DialogueType.TEXT_OPTION) {
            List<String> options = core.getWidgetManager().getDialogue().getOptions();
            if (options != null && !options.isEmpty()) {
                for (String optionText : options) {
                    core.log("Checking option: " + optionText);
                    if (optionText.toLowerCase().contains(REPAIR_OPTION.toLowerCase())) {
                        return true;
                    }
                }
            }

        }
        return false;
    }
}
