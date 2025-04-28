package com.osmb.script.fletching;

import com.osmb.api.ScriptCore;
import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ZoomType;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.scene.RSObject;
import com.osmb.api.script.Script;
import com.osmb.api.script.ScriptDefinition;
import com.osmb.api.script.SkillCategory;
import com.osmb.api.ui.GameState;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.fletching.javafx.ScriptOptions;
import com.osmb.script.fletching.method.Method;
import com.osmb.script.fletching.method.impl.Arrows;
import com.osmb.script.fletching.method.impl.CutLogs;
import com.osmb.script.fletching.method.impl.DartBoltMaker;
import com.osmb.script.fletching.method.impl.StringBows;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

@ScriptDefinition(name = "AIO Fletcher", author = "Joe", version = 1.0, description = "Covers a variety of fletching methods!", skillCategory = SkillCategory.FLETCHING)
public class AIOFletcher extends Script {
    // little cheap fix as our Image class doesn't allow alpha channel
    public static final Color MENU_COLOR_BACKGROUND = new Color(58, 65, 66);
    public static final int[] FEATHERS = new int[]{ItemID.FEATHER, ItemID.BLUE_FEATHER, ItemID.ORANGE_FEATHER, ItemID.RED_FEATHER, ItemID.YELLOW_FEATHER, ItemID.STRIPY_FEATHER};
    // names of possible banks
    public static final String[] BANK_NAMES = {"Bank", "Chest", "Bank booth", "Bank chest", "Grand Exchange booth"};
    public static final String[] BANK_ACTIONS = {"bank", "open"};
    public static final int AMOUNT_CHANGE_TIMEOUT_SECONDS = 6;
    private final Predicate<RSObject> bankQuery = gameObject -> {
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

    public static String getItemName(ScriptCore core, int itemID) {
        ItemDefinition def = core.getItemManager().getItemDefinition(itemID);
        String name = null;
        if (def != null && def.name != null) {
            name = def.name;
        }
        return name;
    }

    public static ImageView getUIImage(ScriptCore core, int itemID) {
        com.osmb.api.visual.image.Image itemImage = core.getItemManager().getItemImage(itemID, 1, ZoomType.SIZE_1, AIOFletcher.MENU_COLOR_BACKGROUND.getRGB());
        if (itemImage == null) {
            System.out.println("Item image is null for item: " + itemID);
            return null;
        }
        BufferedImage itemBufferedImage = itemImage.toBufferedImage();
        Image fxImage = SwingFXUtils.toFXImage(itemBufferedImage, null);
        return new javafx.scene.image.ImageView(fxImage);
    }

    public void setSelectedMethod(Method selectedMethod) {
        this.selectedMethod = selectedMethod;
    }

    @Override
    public void onStart() {
        Method[] methods = new Method[]{new CutLogs(this), new StringBows(this), new DartBoltMaker(this), new Arrows(this)};
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
        return !(selectedMethod instanceof DartBoltMaker);
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

        List<RSObject> banksFound = getObjectManager().getObjects(bankQuery);
        //can't find a bank
        if (banksFound.isEmpty()) {
            log(getClass().getSimpleName(), "Can't find any banks matching criteria...");
            return;
        }
        RSObject object = (RSObject) getUtils().getClosest(banksFound);
        if (!object.interact(BANK_ACTIONS)) {
            return;
        }

        AtomicReference<Timer> positionChangeTimer = new AtomicReference<>(new Timer());
        AtomicReference<WorldPosition> previousPosition = new AtomicReference<>(null);
        submitTask(() -> {
            WorldPosition position = getWorldPosition();
            if (position == null) {
                log(getClass(), "Position is null");
                return false;
            }

            if (!position.equals(previousPosition.get())) {
                log(getClass(), "Position changed");
                positionChangeTimer.get().reset();
                previousPosition.set(position);
            }

            return getWidgetManager().getBank().isVisible() || positionChangeTimer.get().timeElapsed() > 2000;
        }, 15000, true, false, true);
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
