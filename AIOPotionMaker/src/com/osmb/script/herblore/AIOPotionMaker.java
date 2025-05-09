package com.osmb.script.herblore;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.GameState;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.herblore.data.MixedPotion;
import com.osmb.script.herblore.data.UnfinishedPotion;
import com.osmb.script.herblore.javafx.ScriptOptions;
import com.osmb.script.herblore.method.PotionMixer;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "AIO Potion maker", author = "Joe", version = 1.0, description = "Makes potions and unfinished potions", skillCategory = SkillCategory.HERBLORE)
public class AIOPotionMaker extends Script {
    public static final int AMOUNT_CHANGE_TIMEOUT_SECONDS = 6;

    // names of possible banks
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    private PotionMixer selectedPotionMixer;
    private boolean bank = false;

    public AIOPotionMaker(Object o) {
        super(o);
    }

    public void setSelectedMethod(PotionMixer selectedPotionMixer) {
        this.selectedPotionMixer = selectedPotionMixer;
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public void onStart() {
        PotionMixer[] potionMixers = new PotionMixer[]{
                new PotionMixer(this, "Potion maker", MixedPotion.values()),
                new PotionMixer(this, "Unfinished potion maker", UnfinishedPotion.values())
        };
        ScriptOptions scriptOptions = new ScriptOptions(this, potionMixers);

        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);
        if (selectedPotionMixer == null) {
            throw new IllegalArgumentException("Selected method cannot be null!");
        }
    }

    @Override
    public void onGameStateChanged(GameState newGameState) {
        selectedPotionMixer.onGamestateChanged(newGameState);
    }

    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            log(getClass().getSimpleName(), "Handling bank");
            this.bank = false;
            selectedPotionMixer.handleBankInterface();
        } else if (bank) {
            openBank();
        } else {
            selectedPotionMixer.poll();
        }
        return 0;
    }

    private void openBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        Predicate<RSObject> bankQuery = gameObject -> {
            // if object has no name
            if (gameObject.getName() == null) {
                return false;
            }
            // has no interact options (eg. bank, open etc.)
            if (gameObject.getActions() == null) {
                return false;
            }

            if (!Arrays.stream(BANK_NAMES).anyMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
                return false;
            }

            // if no actions contain bank or open
            if (!Arrays.stream(gameObject.getActions()).anyMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
                return false;
            }
            // final check is if the object is reachable
            return gameObject.canReach();
        };
        List<RSObject> banksFound = getObjectManager().getObjects(bankQuery);
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact(BANK_ACTIONS))
            return;
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
        // wait for bank interface
        submitHumanTask(() -> {
            WorldPosition position = getWorldPosition();
            if (position == null) {
                return false;
            }
            // check position change, in case of a dud action
            if (pos.get() == null || !position.equals(pos.get())) {
                positionChangeTimer.get().reset();
                pos.set(position);
            }

            return getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000);
        return;
    }

    public boolean isBank() {
        return bank;
    }

    public void setBank(boolean bank) {
        this.bank = bank;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12598};
    }
}
