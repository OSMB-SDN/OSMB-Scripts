package com.osmb.script.fletching;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.GameState;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.utils.Utils;
import com.osmb.script.fletching.javafx.ScriptOptions;
import com.osmb.script.fletching.method.Method;
import com.osmb.script.fletching.method.impl.Arrows;
import com.osmb.script.fletching.method.impl.CutLogs;
import com.osmb.script.fletching.method.impl.StringBows;
import javafx.scene.Scene;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@ScriptDefinition(name = "AIO Fletcher", author = "Joe", version = 1.0, description = "Covers a variety of fletching methods!", skillCategory = SkillCategory.FLETCHING)
public class AIOFletcher extends Script {
    // little cheap fix as our Image class doesn't allow alpha channel
    public static final Color MENU_COLOR_BACKGROUND = new Color(58, 65, 66);
    public static final int[] FEATHERS = new int[]{ItemID.FEATHER, ItemID.BLUE_FEATHER, ItemID.ORANGE_FEATHER, ItemID.RED_FEATHER, ItemID.YELLOW_FEATHER, ItemID.STRIPY_FEATHER};
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


    public AIOFletcher(Object o) {
        super(o);
    }


    public void setSelectedMethod(Method selectedMethod) {
        this.selectedMethod = selectedMethod;
    }

    @Override
    public void onStart() {
        Method[] methods = new Method[]{new CutLogs(this), new StringBows(this), new Arrows(this)};
        ScriptOptions scriptOptions = new ScriptOptions(this, methods);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add(Objects.requireNonNull(ScriptCore.class.getResource("/style.css")).toExternalForm());
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
    public boolean promptBankTabDialogue() {
        return true;
    }

    @Override
    public int poll() {
        if (getWidgetManager().getBank().isVisible()) {
            log(getClass().getSimpleName(), "Handling bank");
            this.bank = false;
            selectedMethod.handleBankInterface();
        } else if (this.bank) {
            openBank();
        } else {
            selectedMethod.poll();
        }
        return 0;
    }

private void openBank() {
    List<RSObject> banksFound = getObjectManager().getObjects(BANK_QUERY);
    // can't find a bank in our loaded scene
    if (banksFound.isEmpty()) {
        log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
        return;
    }
    RSObject object = (RSObject) getUtils().getClosest(banksFound);
    if (!object.interact(BANK_ACTIONS)) {
        return;
    }
    waitForBankToOpen(object);
}

private void waitForBankToOpen(RSObject object) {
    long positionChangeTimeout = random(1000, 2500);
    pollFramesHuman(() -> {
        WorldPosition worldPosition = getWorldPosition();
        if (worldPosition == null) {
            return false;
        }
        int tileDistance = object.getTileDistance(worldPosition);
        // if we are 1 tile away, check if we aren't moving & break out to re-interact
        if (tileDistance > 1 && getLastPositionChangeMillis() >= positionChangeTimeout) {
            return true;
        }
        return getWidgetManager().getBank().isVisible();
    }, RandomUtils.uniformRandom(8000, 15000), true);
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
