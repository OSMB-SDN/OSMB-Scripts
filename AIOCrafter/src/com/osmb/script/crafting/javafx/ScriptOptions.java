package com.osmb.script.crafting.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.script.crafting.AIOCrafter;
import com.osmb.script.crafting.data.ItemIdentifier;
import com.osmb.script.crafting.method.Method;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class ScriptOptions extends VBox {

    private final VBox scriptContentBox;
    public static ComboBox<ItemIdentifier> createItemCombobox(ScriptCore core, ItemIdentifier[] values) {
        ComboBox<ItemIdentifier> productComboBox = new ComboBox<>();
        productComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ItemIdentifier item) {
                return item != null ? AIOCrafter.getItemName(core, item.getItemID()) : null; // Use the getName method
            }

            @Override
            public ItemIdentifier fromString(String string) {
                // Not needed in this context
                return null;
            }
        });
        productComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ItemIdentifier item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    int itemID = item.getItemID();
                    String name = AIOCrafter.getItemName(core, itemID);
                    ImageView itemImage = AIOCrafter.getUIImage(core, itemID);
                    setGraphic(itemImage);
                    setText(name);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });
        productComboBox.getItems().addAll(values);
        return productComboBox;
    }
    public ScriptOptions(AIOCrafter script, Method[] methods) {
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10");

        scriptContentBox = new VBox();
        scriptContentBox.setAlignment(Pos.TOP_CENTER);
        scriptContentBox.setSpacing(10);
        scriptContentBox.setPadding(new Insets(10));

        Label label = new Label("Choose your desired fletching method");
        ComboBox<Method> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(methods);
        comboBox.setOnAction(actionEvent -> {
            Method selectedMethod = comboBox.getSelectionModel().getSelectedItem();
            if (selectedMethod == null) return;
            //clear the current child nodes
            Platform.runLater(() -> {
                scriptContentBox.getChildren().clear();
                scriptContentBox.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: #495449;"); // Adjust background color as needed
                selectedMethod.provideUIOptions(scriptContentBox);
                scriptContentBox.setAlignment(Pos.TOP_CENTER); // Align items to the top left, change as needed
                Stage stage = null;

                if (getScene() != null && getScene().getWindow() instanceof Stage) {
                    stage = (Stage) getScene().getWindow();
                }
                stage.sizeToScene();
            });

        });

        Button button = new Button("Confirm");
        button.setOnAction(actionEvent -> {

            Method selectedMethod = comboBox.getValue();
            if (selectedMethod != null) {

                if (!selectedMethod.uiOptionsSufficient()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid script options", ButtonType.OK);
                    alert.initOwner(getScene().getWindow());
                    alert.showAndWait();
                    return;
                }
                script.setSelectedMethod(selectedMethod);
                //stage.close();
                ((Stage) button.getScene().getWindow()).close();
            }
        });
        getChildren().addAll(label, comboBox, scriptContentBox, button);
    }


}