package com.osmb.script.herblore.javafx;


import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.herblore.data.MixedPotion;
import com.osmb.script.herblore.data.Potion;
import com.osmb.script.herblore.data.UnfinishedPotion;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScriptOptions extends VBox {

    private final ListView<Potion> productList;
    private final HBox ingredientBox;
    private final Label ingredientsLabel;


    public ScriptOptions(ScriptCore core) {
        setStyle("-fx-padding: 10; -fx-background-color: #636E72;");

        ListView<Class<?>> typeList = new ListView<>();
        typeList.getItems().addAll(MixedPotion.class, UnfinishedPotion.class);
        typeList.setPrefWidth(170);
        typeList.setPrefHeight(250);

        productList = new ListView<>();
        productList.setPrefHeight(250);

        typeList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            productList.getItems().clear();
            if (newVal == MixedPotion.class) {
                productList.getItems().addAll(MixedPotion.values());
            } else if (newVal == UnfinishedPotion.class) {
                productList.getItems().addAll(UnfinishedPotion.values());
            }
        });
        ingredientBox = new HBox(5);
        ingredientBox.setPrefWidth(170);

        ingredientsLabel = new Label("Ingredients");
        ingredientsLabel.setManaged(false);
        ingredientsLabel.setVisible(false);
        VBox productsBox = new VBox(5, productList, ingredientsLabel, ingredientBox);


        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setOnAction(actionEvent -> {
            if (getSelectedProduct() == null) {
                return;
            }
            ((Stage) confirmButton.getScene().getWindow()).close();
        });

        HBox listsBox = new HBox(10, typeList, productsBox);
        this.setSpacing(10);
        this.getChildren().addAll(listsBox, confirmButton);
        productList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                ingredientsLabel.setVisible(false);
                ingredientsLabel.setManaged(false);
                ingredientBox.getChildren().clear();
            } else {
                ingredientsLabel.setVisible(true);
                ingredientsLabel.setManaged(true);
                ingredientBox.getChildren().clear();
                for (int ingredientId : newVal.getIngredientIds()) {
                    ImageView ingredientImageView = JavaFXUtils.getItemImageView(core, ingredientId);
                    ingredientImageView.setFitWidth(30);
                    ingredientImageView.setFitHeight(30);
                    ingredientBox.getChildren().add(ingredientImageView);

                    String itemName = core.getItemManager().getItemName(ingredientId);
                    Label desc = new Label(itemName);
                    desc.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");
                    ingredientBox.getChildren().add(desc);
                }
            }
        });
        productList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Potion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String name = core.getItemManager().getItemName(item.getItemID());
                    setText(name);
                    ImageView itemImageView = JavaFXUtils.getItemImageView(core, item.getItemID());
                    setGraphic(itemImageView);
                }
            }
        });
        typeList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Class<?> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getSimpleName());
                    try {
                        Object[] values = (Object[]) item.getMethod("values").invoke(null);
                        if (values.length > 0 && values[0] instanceof Potion first) {
                            setGraphic(JavaFXUtils.getItemImageView(core, first.getItemID()));
                        } else {
                            setGraphic(null);
                        }
                    } catch (Exception e) {
                        setGraphic(null);
                    }

                }
            }
        });
    }

    public static ScriptOptions show(ScriptCore core) {
        ScriptOptions scriptOptions = new ScriptOptions(core);
        Scene scene = new Scene(scriptOptions);
        scene.getStylesheets().add("style.css");
        core.getStageController().show(scene, "Settings", false);
        return scriptOptions;
    }

    public Potion getSelectedProduct() {
        return productList.getSelectionModel().getSelectedItem();
    }
}
