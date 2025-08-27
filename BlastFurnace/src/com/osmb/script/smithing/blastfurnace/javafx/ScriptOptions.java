package com.osmb.script.smithing.blastfurnace.javafx;


import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.smithing.blastfurnace.BlastFurnace;
import com.osmb.script.smithing.blastfurnace.Resource;
import com.osmb.script.smithing.blastfurnace.data.Bar;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScriptOptions extends VBox {

    private final ListView<Bar> productList;
    private final HBox ingredientBox;
    private final Label ingredientsLabel;
    private final CheckBox useStaminaPotionCheckBox;
    private final CheckBox payForeman;
//    private final RadioButton bucketOfWater;
//    private final RadioButton iceGloves;

    public ScriptOptions(ScriptCore core) {
        setStyle("-fx-padding: 10; -fx-background-color: #636E72;");

        productList = new ListView<>();
        productList.setPrefHeight(250);
        productList.getItems().addAll(Bar.values());

        ingredientBox = new HBox(5);
        ingredientBox.setPrefWidth(170);

        ingredientsLabel = new Label("Required resources per bar");
        ingredientsLabel.setManaged(false);
        ingredientsLabel.setVisible(false);
        VBox productsBox = new VBox(5, productList, ingredientsLabel, ingredientBox);

        productList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                ingredientsLabel.setVisible(false);
                ingredientsLabel.setManaged(false);
                ingredientBox.getChildren().clear();
            } else {
                ingredientsLabel.setVisible(true);
                ingredientsLabel.setManaged(true);
                ingredientBox.getChildren().clear();
                for (Resource resource : newVal.getOres()) {
                    ImageView ingredientImageView = JavaFXUtils.getItemImageView(core, resource.getItemID());
                    ingredientImageView.setFitWidth(30);
                    ingredientImageView.setFitHeight(30);
                    ingredientBox.getChildren().add(ingredientImageView);

                    String itemName = core.getItemManager().getItemName(resource.getItemID());
                    Label desc = new Label(itemName + " x " + resource.getAmount());
                    desc.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");
                    ingredientBox.getChildren().add(desc);
                }
                if (newVal.getCoalAmount() > 0) {
                    ImageView ingredientImageView = JavaFXUtils.getItemImageView(core, ItemID.COAL);
                    ingredientImageView.setFitWidth(30);
                    ingredientImageView.setFitHeight(30);
                    ingredientBox.getChildren().add(ingredientImageView);

                    String itemName = core.getItemManager().getItemName(ItemID.COAL);
                    Label desc = new Label(itemName + " x " + newVal.getCoalAmount());
                    desc.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");
                    ingredientBox.getChildren().add(desc);
                }
            }
        });
        productList.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Bar item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String name = core.getItemManager().getItemName(item.getBarID());
                    setText(name);
                    ImageView itemImageView = JavaFXUtils.getItemImageView(core, item.getBarID());
                    setGraphic(itemImageView);
                }
            }
        });

        payForeman = new CheckBox("Pay Foreman");
        payForeman.setStyle("-fx-text-fill: white");
        payForeman.setSelected(true);

        useStaminaPotionCheckBox = new CheckBox("Use stamina potion");
        useStaminaPotionCheckBox.setStyle("-fx-text-fill: white");
        useStaminaPotionCheckBox.setSelected(true);

        // Confirm button
        Button confirmButton = new Button("Confirm");
        confirmButton.setMaxWidth(Double.MAX_VALUE);
        confirmButton.setOnAction(actionEvent -> {
            if (getSelectedProduct() == null) {
                return;
            }
            ((Stage) confirmButton.getScene().getWindow()).close();
        });

//        Label coolingMethod = new Label("Cooling method");
//        coolingMethod.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
//        iceGloves = new RadioButton("Ice gloves");
//        bucketOfWater = new RadioButton("Bucket of water");
//        ToggleGroup coolingGroup = new ToggleGroup();
//        iceGloves.setToggleGroup(coolingGroup);
//        bucketOfWater.setToggleGroup(coolingGroup);
//        iceGloves.setSelected(true);
//        VBox coolingBox = new VBox(5, coolingMethod, iceGloves, bucketOfWater, new Region());

        Label miscLabel = new Label("Misc");
        miscLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
        VBox miscBox = new VBox(5, miscLabel, payForeman, useStaminaPotionCheckBox, new Region());
        this.setSpacing(10);
        this.getChildren().addAll(new HBox(10, productsBox), miscBox, confirmButton);
    }

    public Bar getSelectedProduct() {
        return productList.getSelectionModel().getSelectedItem();
    }

    public boolean shouldUseStaminaPotion() {
        return useStaminaPotionCheckBox.isSelected();
    }

    public boolean shouldPayForeman() {
        return payForeman.isSelected();
    }

    public BlastFurnace.CoolingMethod getCoolingMethod() {
//        if (iceGloves.isSelected()) {
//            return BlastFurnace.CoolingMethod.ICE_GLOVES;
//        } else {
//            return BlastFurnace.CoolingMethod.BUCKET_OF_WATER;
//        }
        return BlastFurnace.CoolingMethod.ICE_GLOVES;
    }
}
