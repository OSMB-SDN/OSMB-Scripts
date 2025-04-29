package com.osmb.script.chartercrafting.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.chartercrafting.Dock;
import com.osmb.script.chartercrafting.GlassBlowingItem;
import com.osmb.script.chartercrafting.Method;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class UI extends VBox {

    private final ComboBox<Dock> dockComboBox = new ComboBox<>();
    private final ComboBox<Method> methodComboBox = new ComboBox<>();
    private final ComboBox<Integer> itemToMakeComboBox;
    private final Label itemLabel;
    private VBox itemToMakeBox = null;

    private static final Preferences prefs = Preferences.userNodeForPackage(UI.class);
    private static final String PREF_SELECTED_METHOD = "chartercrafting_selected_method";
    private static final String PREF_SELECTED_DOCK = "chartercrafting_selected_dock";
    private static final String PREF_SELECTED_ITEM = "chartercrafting_selected_item";

    public UI(ScriptCore core) {
        setStyle("-fx-spacing: 10; -fx-alignment: left; -fx-padding: 5; -fx-background-color: #636E72");
        Label methodLabel = new Label("Method");
        getChildren().add(methodLabel);
        methodComboBox.getItems().addAll(Method.values());
        methodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newMethod) -> {
            // update docks to match method requirements
            Platform.runLater(() -> {
                dockComboBox.getItems().setAll(
                        Arrays.stream(Dock.values())
                                .filter(dock -> isDockValidForMethod(dock, newMethod))
                                .collect(Collectors.toList())
                );
                Dock savedDock = getSavedDock(newMethod);
                if (savedDock != null) {
                    dockComboBox.getSelectionModel().select(savedDock);
                } else {
                    dockComboBox.getSelectionModel().select(0);
                }
                if (itemToMakeBox != null)
                    itemToMakeBox.setVisible(newMethod != Method.BUY_AND_BANK);
                Scene scene = methodComboBox.getScene();
                if (scene != null) {
                    ((Stage) scene.getWindow()).sizeToScene();
                }
            });
        });

        Method savedMethod = getSavedMethod();
        methodComboBox.getSelectionModel().select(savedMethod != null ? savedMethod : Method.BUY_AND_BANK);
        getChildren().add(methodComboBox);

        Label dockLabel = new Label("Dock");
        getChildren().add(dockLabel);
        dockComboBox.getItems().addAll(Dock.values());
        dockComboBox.getSelectionModel().select(getSavedDock(savedMethod));
        getChildren().add(dockComboBox);

        itemLabel = new Label("Item to make");
        getChildren().add(itemLabel);
        itemToMakeComboBox = JavaFXUtils.createItemCombobox(core, GlassBlowingItem.getItemIds());

        int savedItemId = prefs.getInt(PREF_SELECTED_ITEM, GlassBlowingItem.GLASS_ORB.getItemId());
        for (Integer id : GlassBlowingItem.getItemIds()) {
            if (id == savedItemId) {
                itemToMakeComboBox.getSelectionModel().select(id);
                break;
            }
        }

        itemToMakeBox = new VBox(itemLabel, itemToMakeComboBox);
        itemToMakeBox.setStyle("-fx-spacing: 10; -fx-padding: 0 0 20 0");
        getChildren().add(itemToMakeBox);

        Button confirmButton = new Button("Confirm");
        HBox confirmHbox = new HBox(confirmButton);
        confirmHbox.setStyle("-fx-alignment: center-right");
        getChildren().add(confirmHbox);
        confirmButton.setOnAction(actionEvent -> {
            if (getSelectedDock() == null || getSelectedMethod() == null || getSelectedGlassBlowingItem() == null) {
                return;
            }

            prefs.put(PREF_SELECTED_METHOD, getSelectedMethod().name());
            prefs.put(PREF_SELECTED_DOCK, getSelectedDock().name());
            prefs.putInt(PREF_SELECTED_ITEM, getSelectedGlassBlowingItem().getItemId());

            ((Stage) confirmButton.getScene().getWindow()).close();
            return;
        });
    }

    public Dock getSelectedDock() {
        return dockComboBox.getSelectionModel().getSelectedItem();
    }

    public Method getSelectedMethod() {
        return methodComboBox.getSelectionModel().getSelectedItem();
    }

    public GlassBlowingItem getSelectedGlassBlowingItem() {
        Integer itemToMake = itemToMakeComboBox.getSelectionModel().getSelectedItem();
        if (itemToMake == null) {
            return null;
        }

        return GlassBlowingItem.forItemId(itemToMake);
    }

    private boolean isDockValidForMethod(Dock dock, Method method) {
        return switch (method) {
            case BUY_AND_BANK -> dock.getBankArea() != null;
            case BUY_AND_FURNACE_CRAFT -> dock.getFurnaceArea() != null;
            default -> true;
        };
    }

    private Method getSavedMethod() {
        try {
            return Method.valueOf(prefs.get(PREF_SELECTED_METHOD, Method.BUY_AND_BANK.name()));
        } catch (IllegalArgumentException e) {
            return Method.BUY_AND_BANK;
        }
    }

    private Dock getSavedDock(Method method) {
        try {
            String saved = prefs.get(PREF_SELECTED_DOCK, "");
            if (!saved.isEmpty()) {
                Dock dock = Dock.valueOf(saved);
                if (isDockValidForMethod(dock, method)) {
                    return dock;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
