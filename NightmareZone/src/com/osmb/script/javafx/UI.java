package com.osmb.script.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.LowerHealthMethod;
import com.osmb.script.Potion;
import com.osmb.script.SpecialAttackWeapon;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UI extends BorderPane {

    private final ComboBox<Integer> lowerHPMethodComboBox;
    private final ComboBox<Integer> secondaryPotionComboBox;
    private final ComboBox<Integer> boostPotionComboBox;
    private final TextField mainWeaponTextField;
    private final TextField shieldTextField;
    private final CheckBox flickRapidHeal;

    public UI(ScriptCore core) {
        // main weapon
        Label mainWeaponLabel = new Label("Main weapon");
        mainWeaponTextField = new TextField();
        mainWeaponTextField.setPrefWidth(50);
        Button itemSearchButton = new Button("\uD83D\uDD0E");
        itemSearchButton.setOnAction(actionEvent -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) itemSearchButton.getScene().getWindow());
            mainWeaponTextField.setText(String.valueOf(itemID));
        });
        HBox mainWeaponHBox = new HBox(mainWeaponTextField, itemSearchButton);
        mainWeaponHBox.setSpacing(5);
        VBox mainWeaponVBox = new VBox(mainWeaponLabel, mainWeaponHBox);

        // shield
        Label shieldLabel = new Label("Shield");
        shieldTextField = new TextField();
        shieldTextField.setPrefWidth(50);
        Button itemSearchButton2 = new Button("\uD83D\uDD0E");
        itemSearchButton2.setOnAction(actionEvent -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) itemSearchButton2.getScene().getWindow());
            shieldTextField.setText(String.valueOf(itemID));
        });
        HBox shieldHBox = new HBox(shieldTextField, itemSearchButton2);
        shieldHBox.setSpacing(5);
        VBox shieldVBox = new VBox(shieldLabel, shieldHBox);

        HBox weaponShieldHBox = new HBox(mainWeaponVBox, shieldVBox);
        weaponShieldHBox.setStyle("-fx-spacing: 10; -fx-alignment: center");
        // special attack weapon
        Label specialAttackWeaponLabel = new Label("Special attack weapon");
        ComboBox<Integer> specialItemComboBox = JavaFXUtils.createItemCombobox(core, SpecialAttackWeapon.getItemIDs());

        Label specialAttackWeaponLabel2 = new Label("Leave blank, or -1 if none");


        VBox weaponVBox = new VBox(weaponShieldHBox, specialAttackWeaponLabel, specialItemComboBox, specialAttackWeaponLabel2);

        weaponVBox.setStyle("-fx-spacing: 5; -fx-padding: 10; -fx-background-color: #636E72");

        TitledPane weaponTitledPane = new TitledPane("Weapon load out", weaponVBox);
        weaponTitledPane.setCollapsible(false);
        weaponTitledPane.getStyleClass().add("script-manager-titled-pane");
        VBox.setVgrow(weaponTitledPane, Priority.ALWAYS);
        weaponTitledPane.setMaxHeight(Double.MAX_VALUE);

        setLeft(weaponTitledPane);

        Label primaryPotionLabel = new Label("Stat boost potion");

        boostPotionComboBox = JavaFXUtils.createItemCombobox(core, new int[]{Potion.OVERLOAD.getFullID(), Potion.SUPER_COMBAT.getFullID(), Potion.SUPER_RANGING_POTION.getFullID(), Potion.SUPER_MAGIC_POTION.getFullID(), Potion.RANGING_POTION.getFullID()});
        boostPotionComboBox.setPrefWidth(220);

        Label secondaryPotionLabel = new Label("Secondary potion");
        secondaryPotionComboBox = JavaFXUtils.createItemCombobox(core, new int[]{Potion.ABSORPTION_POTION.getFullID(), Potion.PRAYER_POTION.getFullID()});
        secondaryPotionComboBox.setPrefWidth(220);


        // ABSORPTION SETTINGS
        Label absorptionSettings = new Label("Absorption settings");

        Label lowerHPMethodLabel = new Label("Lower HP method");
        lowerHPMethodComboBox = JavaFXUtils.createItemCombobox(core, new int[]{LowerHealthMethod.ROCK_CAKE.getItemID(), LowerHealthMethod.LOCATOR_ORB.getItemID()});
        secondaryPotionComboBox.setPrefWidth(220);

        flickRapidHeal = new CheckBox("Flick Rapid heal (Make sure it is set as quick prayer)");
        flickRapidHeal.setStyle("-fx-text-fill: white");
        VBox absorptionSettingsVbox = new VBox(lowerHPMethodLabel, lowerHPMethodComboBox, flickRapidHeal);
        absorptionSettingsVbox.setSpacing(10);
        absorptionSettingsVbox.setVisible(false);

        secondaryPotionComboBox.getSelectionModel().selectedItemProperty().addListener((observableValue, integer, t1) -> {
            absorptionSettingsVbox.setVisible(t1 == Potion.ABSORPTION_POTION.getFullID());
        });
        VBox potionVBox = new VBox(primaryPotionLabel, boostPotionComboBox, secondaryPotionLabel, secondaryPotionComboBox, absorptionSettingsVbox);
        potionVBox.setStyle("-fx-spacing: 5; -fx-padding: 10; -fx-background-color: #636E72");
        TitledPane potionTitledPane = new TitledPane("Potion load out", potionVBox);
        potionTitledPane.setCollapsible(false);
        potionTitledPane.getStyleClass().add("script-manager-titled-pane");

        setRight(potionTitledPane);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(actionEvent -> {
            // close stage
            ((Stage) confirmButton.getScene().getWindow()).close();
        });
        HBox hBox = new HBox(confirmButton);
        hBox.setStyle("-fx-alignment: center-right; -fx-padding: 10; -fx-background-color: #636E72");
        setBottom(hBox);
    }

    public Potion getSelectedPrimaryPotion() {
        int selectedID = boostPotionComboBox.getValue();
        return Potion.getPotionForID(selectedID);
    }

    public Potion getSelectedSecondaryPotion() {
        int selectedID = secondaryPotionComboBox.getValue();
        return Potion.getPotionForID(selectedID);
    }
}
