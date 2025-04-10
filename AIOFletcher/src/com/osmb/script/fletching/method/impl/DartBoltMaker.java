package com.osmb.script.fletching.method.impl;

 import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.GameState;
import com.osmb.api.utils.UIResult;
import com.osmb.api.utils.Utils;
import com.osmb.api.utils.timing.Timer;
import com.osmb.script.fletching.AIOFletcher;
import com.osmb.script.fletching.data.*;
import com.osmb.script.fletching.method.Method;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.VBox;

import java.util.concurrent.TimeUnit;

import static com.osmb.script.fletching.AIOFletcher.FEATHERS;

public class DartBoltMaker extends Method {

    private final Object lock = new Object();
    private volatile Rectangle slot1, slot2;
    private volatile GameState gameState;
    private final Runnable tapThread = () -> {
        while (!script.stopped()) {
            GameState currentState;
            Rectangle currentSlot1, currentSlot2;

            synchronized (lock) {
                currentState = this.gameState;
                currentSlot1 = this.slot1;
                currentSlot2 = this.slot2;
            }

            // stop tapping if any profiles are due to execute
            if (currentSlot1 == null || currentSlot2 == null || currentState != GameState.LOGGED_IN || script.isDueToBreak() || script.isDueToHop() || script.isDueToAFK()) {
                sleep(500);
                continue;
            }

            if (currentSlot1 != null) {
                script.getFinger().tap(currentSlot1);
                sleep(Utils.random(50, 200));
            }
            if (currentSlot2 != null) {
                script.getFinger().tap(currentSlot2);
                sleep(Utils.random(50, 200));
            }
        }
    };
    private ComboBox<ItemIdentifier> itemComboBox = new ComboBox<>();
    private int selectedUnfinishedItemID;
    private Timer notFoundTimer = new Timer();
    private boolean setGameState = false;
    private boolean initialCheck = true;

    public DartBoltMaker(AIOFletcher script) {
        super(script);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGamestateChanged(GameState gameState) {
        synchronized (lock) {
            this.gameState = gameState;
        }
    }

    @Override
    public int poll() {
        if (!setGameState) {
            gameState = GameState.LOGGED_IN;
            setGameState = true;
        }

        if (!script.getWidgetManager().getInventory().open()) {
            synchronized (lock) {
                slot1 = null;
                slot2 = null;
            }
            return 0;
        }

        UIResult<ItemSearchResult> feathers = script.getItemManager().findItem(script.getWidgetManager().getInventory(), false, FEATHERS);
        UIResult<ItemSearchResult> dartTips = script.getItemManager().findItem(script.getWidgetManager().getInventory(), false, selectedUnfinishedItemID);

        if (!feathers.isFound() || !dartTips.isFound()) {
            if (initialCheck) {
                script.log(getClass().getSimpleName(), "Insufficient supplies in the inventory, stopping script...");
                script.stop();
                return 0;
            }
            // if we go 5-10 seconds not being able to detect the items, stop
            if (notFoundTimer.timeElapsed() < TimeUnit.SECONDS.toMillis(7)) {
                return 0;
            }
            script.stop();
            return 0;
        } else {
            notFoundTimer.reset();
            if (initialCheck) {
                initialCheck = false;
            }
        }

        UIResult<Rectangle> featherSlot = script.getItemManager().getBoundsForSlot(feathers.get().getItemSlot(), script.getWidgetManager().getInventory());
        UIResult<Rectangle> dartTipsSlot = script.getItemManager().getBoundsForSlot(dartTips.get().getItemSlot(), script.getWidgetManager().getInventory());

        synchronized (lock) {
            if (featherSlot.isFound() && dartTipsSlot.isFound()) {
                slot1 = featherSlot.get();
                slot2 = dartTipsSlot.get();
            }
        }
        return 0;
    }


    @Override
    public int handleBankInterface() {
        return 0;
    }

    @Override
    public void provideUIOptions(VBox vBox) {
        Label itemLabel = new Label("Choose item to create");
        vBox.getChildren().add(itemLabel);

        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("Darts", "Bolts");
        typeComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (type != null && !empty) {
                    itemComboBox.getItems().clear();
                    switch (type) {
                        case "Darts" -> itemComboBox.getItems().addAll(Dart.values());
                        case "Bolts" -> itemComboBox.getItems().addAll(Bolt.values());
                    }
                    setText(type);
                } else {
                    setText(null);
                }
            }
        });
        vBox.getChildren().add(typeComboBox);

        itemComboBox = new ComboBox<>();

        vBox.getChildren().add(itemComboBox);
    }

    @Override
    public boolean uiOptionsSufficient() {
        if (itemComboBox.getValue() != null) {
            selectedUnfinishedItemID = ((Combinable) itemComboBox.getValue()).getUnfinishedID();
            new Thread(tapThread).start();
            return true;
        }
        return false;
    }

    @Override
    public String getMethodName() {
        return "Darts & Bolts";
    }

    @Override
    public String toString() {
        return getMethodName();
    }
}
