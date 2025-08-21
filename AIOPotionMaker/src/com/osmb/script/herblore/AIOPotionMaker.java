package com.osmb.script.herblore;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.herblore.data.Potion;
import com.osmb.script.herblore.javafx.ScriptOptions;
import com.osmb.script.herblore.method.PotionMixer;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "AIO Potion maker", author = "Joe", version = 1.0, description = "Makes potions and unfinished potions", skillCategory = SkillCategory.HERBLORE)
public class AIOPotionMaker extends Script {
    // names of possible banks
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    public static final Predicate<RSObject> BANK_QUERY = gameObject -> {
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
    private PotionMixer potionMixer;
    private boolean bank = false;
    public AIOPotionMaker(Object o) {
        super(o);
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public void onStart() {
        ScriptOptions scriptOptions = new ScriptOptions(this);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);

        Potion selectedProduct = scriptOptions.getSelectedProduct();
        if (selectedProduct == null) {
            throw new IllegalArgumentException("Selected potion cannot be null!");
        }

        this.potionMixer = new PotionMixer(this, selectedProduct);

    }


    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            log(getClass().getSimpleName(), "Handling bank");
            this.bank = false;
            potionMixer.handleBankInterface();
        } else if (bank) {
            openBank();
        } else {
            potionMixer.poll();
        }
        return 0;
    }

    private void openBank() {
        log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        List<RSObject> banksFound = getObjectManager().getObjects(BANK_QUERY);
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }

        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact(BANK_ACTIONS)) {
            // failed to interact with the bank
            return;
        }
        waitForBankToOpen(object);
    }

    private void waitForBankToOpen(RSObject object) {
        long positionChangeTimeout = random(1000, 2500);
        submitHumanTask(() -> {
            int tileDistance = object.getTileDistance();
            if (tileDistance > 1 && getLastPositionChangeMillis() >= positionChangeTimeout) {
                return true;
            }
            return getWidgetManager().getBank().isVisible();
        }, Utils.random(8000, 13000), false, true);
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
