package com.osmb.script.crafting;

import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.GameState;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.crafting.javafx.ScriptOptions;
import com.osmb.script.crafting.method.Method;
import com.osmb.script.crafting.method.impl.CraftHide;
import com.osmb.script.crafting.method.impl.CutGems;
import com.osmb.script.crafting.method.impl.GlassBlowing;
import javafx.scene.Scene;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "AIO Crafter", author = "Joe", version = 1.0, description = "Covers a variety of crafting methods!", skillCategory = SkillCategory.CRAFTING)
public class AIOCrafter extends Script {
    public static final Color MENU_COLOR_BACKGROUND = new Color(58, 65, 66);

    // names of possible banks
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth", "Bank counter", "Bank table"};
    public static final String[] BANK_ACTIONS = {"bank", "open", "use"};
    private static final Predicate<RSObject> BANK_QUERY = gameObject -> {
        // if object has no name
        if (gameObject.getName() == null) {
            return false;
        }
        // has no interact options (eg. bank, open etc.)
        if (gameObject.getActions() == null) {
            return false;
        }

        if (Arrays.stream(BANK_NAMES).noneMatch(name -> name.equalsIgnoreCase(gameObject.getName()))) {
            return false;
        }

        // if no actions contain bank or open
        if (Arrays.stream(gameObject.getActions()).noneMatch(action -> Arrays.stream(BANK_ACTIONS).anyMatch(bankAction -> bankAction.equalsIgnoreCase(action)))) {
            return false;
        }
        // final check is if the object is reachable
        return gameObject.canReach();
    };
    private Method selectedMethod;
    private boolean bank = false;
    private int amountChangeTimeout;

    public AIOCrafter(Object o) {
        super(o);
    }

    public int getAmountChangeTimeout() {
        return amountChangeTimeout;
    }

    public void resetAmountChangeTimeout() {
        amountChangeTimeout = random(4500, 8000);
    }

    public void setSelectedMethod(Method selectedMethod) {
        this.selectedMethod = selectedMethod;
    }

    @Override
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public void onStart() {
        Method[] methods = new Method[]{new CutGems(this), new CraftHide(this), new GlassBlowing(this)};
        ScriptOptions scriptOptions = new ScriptOptions(this, methods);

        resetAmountChangeTimeout();


        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");
        getStageController().show(scene, "Settings", false);
        if (selectedMethod == null) {
            throw new IllegalArgumentException("Selected method cannot be null!");
        }
    }

    @Override
    public void onGameStateChanged(GameState newGameState) {
        selectedMethod.onGamestateChanged(newGameState);
    }

    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            log(getClass().getSimpleName(), "Handling bank");
            // if bank interface is visible, handle it
            // set bank flag to false now we have the bank open
            if (this.bank) this.bank = false;
            selectedMethod.handleBankInterface();
        } else if (this.bank) {
            openBank();
        } else {
            if (!getWidgetManager().getInventory().unSelectItemIfSelected()) {
                return 0;
            }
            selectedMethod.poll();
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
        if (!object.interact(BANK_ACTIONS)) return;
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

    public void setBank(boolean bank) {
        this.bank = bank;
    }

    @Override
    public int[] regionsToPrioritise() {
        return new int[]{12598};
    }
}
