package com.osmb.script.furnace.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.furnace.Product;
import com.osmb.script.furnace.data.Bar;
import com.osmb.script.furnace.data.Jewellery;
import com.osmb.script.furnace.data.Misc;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScriptOptions extends VBox {

    private final ListView<Product> productList;

    public ScriptOptions(ScriptCore core) {
        setStyle("-fx-padding: 10; -fx-background-color: #636E72;");
        // ListView for product types (Jewellery, Bar)
        ListView<Class<?>> typeList = new ListView<>();
        typeList.getItems().addAll(Jewellery.class, Bar.class, Misc.class);
        typeList.setPrefWidth(120);
        typeList.setPrefHeight(250);

        productList = new ListView<>();
        productList.setPrefHeight(250);

        typeList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            productList.getItems().clear();
            if (newVal == Jewellery.class) {
                productList.getItems().addAll(Jewellery.values());
            } else if (newVal == Bar.class) {
                productList.getItems().addAll(Bar.values());
            } else if( newVal == Misc.class) {
                productList.getItems().addAll(Misc.values());
            }
        });

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setOnAction(actionEvent -> {
            if(getSelectedProduct() == null) {
                return;
            }
            ((Stage) confirmButton.getScene().getWindow()).close();
        });

        HBox listsBox = new HBox(10, typeList, productList);
        this.setSpacing(10);
        this.getChildren().addAll(listsBox, confirmButton);

        productList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getProductName());
                    setGraphic(JavaFXUtils.getItemImageView(core, item.getItemID()));
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
                        if (values.length > 0 && values[0] instanceof Product first) {
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

    public Product getSelectedProduct() {
        return productList.getSelectionModel().getSelectedItem();
    }
}
