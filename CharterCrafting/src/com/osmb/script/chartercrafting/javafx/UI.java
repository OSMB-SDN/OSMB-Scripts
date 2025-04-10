package com.osmb.script.chartercrafting.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.chartercrafting.Dock;
import com.osmb.script.chartercrafting.GlassBlowingItem;
import com.osmb.script.chartercrafting.Method;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.stream.Collectors;

public class UI extends VBox {

    private final ComboBox<Dock> dockComboBox = new ComboBox<>();
    ;
    private final ComboBox<Method> methodComboBox = new ComboBox<>();
    private final ComboBox<Integer> itemToMakeComboBox;
    private final Label itemLabel;
    private VBox itemToMakeBox = null;

    public UI(ScriptCore core) {
        setStyle("-fx-spacing: 10; -fx-alignment: center; -fx-padding: 15; -fx-background-color: #636E72");
        Label methodLabel = new Label("Method");
        getChildren().add(methodLabel);
        methodComboBox.getItems().addAll(Method.values());
        methodComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newMethod) -> {
            Platform.runLater(() -> {
                dockComboBox.getItems().setAll(
                        Arrays.stream(Dock.values())
                                .filter(dock -> isDockValidForMethod(dock, newMethod))
                                .collect(Collectors.toList())
                );
                dockComboBox.getSelectionModel().select(0);
                if (itemToMakeBox != null)
                    itemToMakeBox.setVisible(newMethod != Method.BUY_AND_BANK);
                ((Stage) methodComboBox.getScene().getWindow()).sizeToScene();
            });
        });
        methodComboBox.getSelectionModel().select(0);
        getChildren().add(methodComboBox);

        Label dockLabel = new Label("Dock");
        getChildren().add(dockLabel);
        dockComboBox.getItems().addAll(Dock.values());
        dockComboBox.getSelectionModel().select(0);
        getChildren().add(dockComboBox);

        itemLabel = new Label("Item to make");
        getChildren().add(itemLabel);
        itemToMakeComboBox = JavaFXUtils.createItemCombobox(core, GlassBlowingItem.getItemIds());
        itemToMakeComboBox.getSelectionModel().select(0);

        itemToMakeBox = new VBox(itemLabel, itemToMakeComboBox);
        itemToMakeBox.setStyle("-fx-spacing: 10; -fx-alignment: center; -fx-padding: 15; ");
        getChildren().add(itemToMakeBox);

        Button confirmButton = new Button("Confirm");
        getChildren().add(confirmButton);
        confirmButton.setOnAction(actionEvent -> {
            if (getSelectedDock() == null || getSelectedMethod() == null || getSelectedGlassBlowingItem() == null) {
                return;
            }
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
}
