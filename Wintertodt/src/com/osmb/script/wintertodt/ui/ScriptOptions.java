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

import java.util.prefs.Preferences;

public class ScriptOptions extends VBox {

    private final ComboBox<Brazier> focusedBrazierComboBox;
    private final RadioButton makePotionsRadio;
    private final RadioButton potionsBrewmaRadio;
    private final RadioButton fletchRootsNoRadio;
    private final RadioButton fletchRootsYesRadio;
    private final RadioButton fletchUntilMilestone;
    // private final CheckBox prioritiseSafeSpots;

    private static final Preferences prefs = Preferences.userNodeForPackage(ScriptOptions.class);
    private static final String PREF_BRAZIER = "wintertodt_brazier";
    private static final String PREF_HEAL_TYPE = "wintertodt_heal";
    private static final String PREF_FLETCH_TYPE = "wintertodt_fletch";

    public ScriptOptions(ScriptCore core) {
        Label brazierLabel = new Label("Brazier to focus");
        brazierLabel.setStyle("-fx-font-size: 14");
        getChildren().add(brazierLabel);
        focusedBrazierComboBox = new ComboBox<>();
        focusedBrazierComboBox.getItems().addAll(Brazier.values());
        focusedBrazierComboBox.getSelectionModel().select(prefs.getInt(PREF_BRAZIER, 0));
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
        potionsBrewmaRadio = new RadioButton("Use Brewma to make potions");
        potionsBrewmaRadio.setToggleGroup(potionsToggleGroup);
        int savedHeal = prefs.getInt(PREF_HEAL_TYPE, 0);
        (savedHeal == 1 ? potionsBrewmaRadio : makePotionsRadio).setSelected(true);

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
        fletchUntilMilestone = new RadioButton("Until first milestone (500 points)");
        fletchUntilMilestone.setToggleGroup(fletchToggleGroup);
        int savedFletch = prefs.getInt(PREF_FLETCH_TYPE, 1);
        switch (savedFletch) {
            case 0 -> fletchRootsNoRadio.setSelected(true);
            case 1 -> fletchRootsYesRadio.setSelected(true);
            case 2 -> fletchUntilMilestone.setSelected(true);
        }

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
            prefs.putInt(PREF_BRAZIER, focusedBrazierComboBox.getSelectionModel().getSelectedIndex());
            prefs.putInt(PREF_HEAL_TYPE, potionsBrewmaRadio.isSelected() ? 1 : 0);
            prefs.putInt(PREF_FLETCH_TYPE, getSelectedFletchType().ordinal());
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
