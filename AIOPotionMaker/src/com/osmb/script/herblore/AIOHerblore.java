package com.osmb.script.herblore;

import com.osmb.api.ScriptCore;
import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.item.ZoomType;
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
import com.osmb.script.herblore.method.Method;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@ScriptDefinition(name = "AIO Potion maker", author = "Joe", version = 1.0, description = "Makes potions and unfinished potions", skillCategory = SkillCategory.HERBLORE)
public class AIOHerblore extends Script {
    public static final Color MENU_COLOR_BACKGROUND = new Color(58, 65, 66);
    public static final int AMOUNT_CHANGE_TIMEOUT_SECONDS = 6;

    // names of possible banks
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open"};
    private Method selectedMethod;
    private boolean bank = false;

    public AIOHerblore(Object o) {
        super(o);
    }


    public static String getItemName(ScriptCore core, int itemID) {
        ItemDefinition def = core.getItemManager().getItemDefinition(itemID);
        String name = null;
        if (def != null && def.name != null) {
            name = def.name;
        }
        return name;
    }

    public static ImageView getUIImage(ScriptCore core, int itemID) {
        BufferedImage itemImage = core.getItemManager().getItemImage(itemID, 1, ZoomType.SIZE_1, AIOHerblore.MENU_COLOR_BACKGROUND.getRGB()).toBufferedImage();
        Image fxImage = SwingFXUtils.toFXImage(itemImage, null);
        return new javafx.scene.image.ImageView(fxImage);
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
        Method[] methods = new Method[]{new Method(this, "Potion maker", MixedPotion.values()), new Method(this, "Unfinished potion maker", UnfinishedPotion.values())};
        ScriptOptions scriptOptions = new ScriptOptions(this, methods);

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
        log(getClass().getSimpleName(), "Searching for a bank...");
        // Find bank and open it
        List<RSObject> banksFound = getObjectManager().getObjects(gameObject -> {
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
        });
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact(BANK_ACTIONS)) return;
        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> pos = new AtomicReference<>(null);
        submitTask(() -> {
            WorldPosition position = getWorldPosition();
            if (position == null) {
                return false;
            }
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
