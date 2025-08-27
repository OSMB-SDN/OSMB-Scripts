package com.osmb.script.woodcutting.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.woodcutting.data.Tree;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScriptOptions extends VBox {

    private final ListView<Tree> treeListView;
    private final RadioButton dropButton;

    public ScriptOptions(ScriptCore scriptCore) {
        // Initialize the layout and components here
        setStyle("-fx-padding: 10; -fx-background-color: #636E72; -fx-spacing: 10");
        treeListView = new ListView<>();
        treeListView.setPrefHeight(230);
        treeListView.setCellFactory(listView -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Tree item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getObjectName());
                    ImageView itemImageView = JavaFXUtils.getItemImageView(scriptCore, item.getLogID());
                    setGraphic(itemImageView);
                }
            }
        });
        treeListView.getItems().addAll(Tree.values());
        getChildren().add(treeListView);

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setStyle("-fx-padding: 10; -fx-spacing: 10");

        RadioButton bankButton = new RadioButton("Bank logs");
        bankButton.setSelected(true);
        dropButton = new RadioButton("Drop logs");
        dropButton.setMinWidth(200); // Adjust width as needed
        getChildren().add(buttonBar);
        VBox fullOption = new VBox(10, bankButton, dropButton);
        buttonBar.getButtons().addAll(fullOption);

        ToggleGroup toggleGroup = new ToggleGroup();
        bankButton.setToggleGroup(toggleGroup);
        dropButton.setToggleGroup(toggleGroup);

        Button confirmButton = new Button("Confirm");
        confirmButton.setMaxWidth(80);
        ButtonBar.setButtonData(confirmButton, ButtonBar.ButtonData.RIGHT);
        confirmButton.setOnAction(event -> {
            Tree selectedTree = treeListView.getSelectionModel().getSelectedItem();
            if (selectedTree != null) {
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });

        buttonBar.getButtons().add(confirmButton);
    }

    public Tree getSelectedTree() {
        return treeListView.getSelectionModel().getSelectedItem();
    }

    public boolean isDropSelected() {
        return dropButton.isSelected();
    }
}
