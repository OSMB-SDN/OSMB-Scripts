package com.osmb.script.wintertodt.ui;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.script.wintertodt.Brazier;
import com.osmb.script.wintertodt.FletchType;
import com.osmb.script.wintertodt.HealType;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ScriptOptions extends VBox {

    private final ComboBox<Brazier> focusedBrazierComboBox;
    private final RadioButton makePotionsRadio;
    private final RadioButton potionsBrewmaRadio;
    private final RadioButton fletchRootsNoRadio;
    private final RadioButton fletchRootsYesRadio;
    private final RadioButton fletchUntilMilestone;
    // private final CheckBox prioritiseSafeSpots;

    public ScriptOptions(ScriptCore core) {
        Label brazierLabel = new Label("Brazier to focus");
        brazierLabel.setStyle("-fx-font-size: 14");
        getChildren().add(brazierLabel);
        focusedBrazierComboBox = new ComboBox<>();
        focusedBrazierComboBox.getItems().addAll(Brazier.values());
        focusedBrazierComboBox.getSelectionModel().select(0);
        HBox focusedBrazierHBox = new HBox(focusedBrazierComboBox);
        focusedBrazierHBox.setStyle("-fx-spacing: 10; -fx-padding: 0 0 15 0");
        getChildren().add(focusedBrazierHBox);

        setStyle("-fx-background-color: #636E72; -fx-spacing: 10px; -fx-padding: 10");
        VBox foodVBox = new VBox();
        Label foodTitleLabel = new Label("Rejuvenation settings");
        foodTitleLabel.setStyle("-fx-font-size: 14");
        foodTitleLabel.setAlignment(Pos.CENTER);
        foodVBox.getChildren().add(foodTitleLabel);
        foodVBox.setStyle("-fx-spacing: 10; -fx-padding: 0 0 15 0");
        foodVBox.setSpacing(10);

        ToggleGroup potionsToggleGroup = new ToggleGroup();

        makePotionsRadio = new RadioButton("Make Potions");
        makePotionsRadio.setToggleGroup(potionsToggleGroup);
        makePotionsRadio.setSelected(true);
        potionsBrewmaRadio = new RadioButton("Use Brewma to make potions");
        potionsBrewmaRadio.setToggleGroup(potionsToggleGroup);
        HBox radioButtonHBox = new HBox(makePotionsRadio, potionsBrewmaRadio);
        radioButtonHBox.setStyle("-fx-spacing: 10; -fx-padding: 0 0 10 0");


        foodVBox.getChildren().addAll(radioButtonHBox);
        getChildren().add(foodVBox);

        Label fletchRootsLabel = new Label("Fletch Roots");
        fletchRootsLabel.setStyle("-fx-font-size: 14");
        getChildren().add(fletchRootsLabel);
        ToggleGroup fletchToggleGroup = new ToggleGroup();
        fletchRootsNoRadio = new RadioButton("No");
        fletchRootsNoRadio.setToggleGroup(fletchToggleGroup);
        fletchRootsYesRadio = new RadioButton("Yes");
        fletchRootsYesRadio.setToggleGroup(fletchToggleGroup);
        fletchRootsYesRadio.setSelected(true);
        fletchUntilMilestone = new RadioButton("Until first milestone (500 points)");
        fletchUntilMilestone.setToggleGroup(fletchToggleGroup);
        HBox fletchRootsHBox = new HBox(fletchRootsNoRadio, fletchRootsYesRadio, fletchUntilMilestone);
        fletchRootsHBox.setStyle("-fx-spacing: 10; -fx-padding: 0 0 15 0");

        getChildren().add(fletchRootsHBox);

//        prioritiseSafeSpots = new CheckBox("Prioritise Safe Spots");
//        prioritiseSafeSpots.setStyle("-fx-text-fill: white; -fx-padding: 0 0 10 0");
//        getChildren().add(prioritiseSafeSpots);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(actionEvent -> {
            if (!isSettingsValid()) {
                return;
            }
            ((Stage) confirmButton.getScene().getWindow()).close();
        });
        HBox buttonHBox = new HBox(confirmButton);
        buttonHBox.setStyle("-fx-alignment: center-right");
        getChildren().add(buttonHBox);

    }

    private boolean isSettingsValid() {

        return getSelectedFletchType() != null && getHealType() != null;
    }

    public Brazier getSelectedBrazier() {
        return focusedBrazierComboBox.getValue();
    }

    public HealType getHealType() {
        return potionsBrewmaRadio.isSelected() ? HealType.REJUVENATION_BREWMA : HealType.REJUVENATION;
    }

    public FletchType getSelectedFletchType() {
        if (fletchUntilMilestone.isSelected()) {
            return FletchType.UNTIL_MILESTONE;
        }
        if (fletchRootsNoRadio.isSelected()) {
            return FletchType.NO;
        }
        if (fletchRootsYesRadio.isSelected()) {
            return FletchType.YES;
        }
        return null;
    }
}
