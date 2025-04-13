package com.osmb.script.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.AFKPosition;
import com.osmb.script.LowerHealthMethod;
import com.osmb.script.Potion;
import com.osmb.script.SpecialAttackWeapon;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UI extends BorderPane {

    private final ComboBox<Integer> lowerHPMethodComboBox;
    private final ComboBox<Integer> secondaryPotionComboBox;
    private final ComboBox<Integer> boostPotionComboBox;
    private final CheckBox flickRapidHeal;
    private final VBox absorptionSettingsVbox;
    private final CheckBox suicideNoBoost;
    private final ComboBox<Integer> specialItemComboBox;
    private final ComboBox<AFKPosition> afkPositionComboBox;
    private ImageView shieldImageView;
    private ImageView mainWeaponImageView;
    private int mainWeaponItemId = -1;
    private int shieldItemId = -1;

    public UI(ScriptCore core) {
        // main weapon
        Label mainWeaponLabel = new Label("Main weapon");
        mainWeaponImageView = JavaFXUtils.getItemImageView(core, ItemID.BANK_FILLER);
        Button itemSearchButton = new Button("\uD83D\uDD0E");
        itemSearchButton.setOnAction(actionEvent -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) itemSearchButton.getScene().getWindow());
            if (itemID == -1) {
                itemID = ItemID.BANK_FILLER;
            }
            ImageView imageView = JavaFXUtils.getItemImageView(core, itemID);
            if (imageView != null) {
                mainWeaponItemId = itemID;
                mainWeaponImageView.setImage(imageView.getImage());
            }
        });
        HBox mainWeaponHBox = new HBox(mainWeaponImageView, itemSearchButton);
        mainWeaponHBox.setSpacing(5);
        VBox mainWeaponVBox = new VBox(mainWeaponLabel, mainWeaponHBox);
        mainWeaponVBox.setStyle("-fx-spacing: 10; -fx-alignment: center");

        // shield
        Label shieldLabel = new Label("Shield");
        shieldImageView = JavaFXUtils.getItemImageView(core, ItemID.BANK_FILLER);

        Button itemSearchButton2 = new Button("\uD83D\uDD0E");
        itemSearchButton2.setOnAction(actionEvent -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) itemSearchButton2.getScene().getWindow());
            if (itemID == -1) {
                itemID = ItemID.BANK_FILLER;
            }
            ImageView imageView = JavaFXUtils.getItemImageView(core, itemID);
            if (imageView != null) {
                shieldItemId = itemID;
                shieldImageView.setImage(imageView.getImage());
            }
        });
        HBox shieldHBox = new HBox(shieldImageView, itemSearchButton2);
        shieldHBox.setSpacing(5);
        VBox shieldVBox = new VBox(shieldLabel, shieldHBox);
        shieldVBox.setStyle("-fx-spacing: 10; -fx-alignment: center");

        HBox weaponShieldHBox = new HBox(mainWeaponVBox, shieldVBox);
        weaponShieldHBox.setStyle("-fx-spacing: 10; -fx-alignment: center");
        // special attack weapon
        Label specialAttackWeaponLabel = new Label("Special attack weapon");
        specialItemComboBox = JavaFXUtils.createItemCombobox(core, true, SpecialAttackWeapon.getItemIDs());
        specialItemComboBox.setPrefWidth(180);


        VBox weaponVBox = new VBox(weaponShieldHBox, specialAttackWeaponLabel, specialItemComboBox);

        weaponVBox.setStyle("-fx-spacing: 5; -fx-padding: 10; -fx-background-color: #636E72");

        TitledPane weaponTitledPane = new TitledPane("Weapon load out", weaponVBox);
        weaponTitledPane.setCollapsible(false);
        weaponTitledPane.getStyleClass().add("script-manager-titled-pane");
        VBox.setVgrow(weaponTitledPane, Priority.ALWAYS);
        weaponTitledPane.setMaxHeight(Double.MAX_VALUE);

        setLeft(weaponTitledPane);

        Label primaryPotionLabel = new Label("Stat boost potion");

        boostPotionComboBox = JavaFXUtils.createItemCombobox(core, true, new int[]{Potion.OVERLOAD.getFullID(), Potion.SUPER_COMBAT.getFullID(), Potion.SUPER_RANGING_POTION.getFullID(), Potion.SUPER_MAGIC_POTION.getFullID(), Potion.RANGING_POTION.getFullID()});
        boostPotionComboBox.setPrefWidth(180);

        Spinner<Integer> boostPotionAmountSpinner = new Spinner<>();
        boostPotionAmountSpinner.setPrefWidth(60);
        boostPotionAmountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 25));
        HBox boostPotionAmountBox = new HBox(new Label("Boost potion amount"), boostPotionAmountSpinner);
        boostPotionAmountBox.setStyle("-fx-spacing: 10; -fx-alignment: center");

        Label secondaryPotionLabel = new Label("Secondary potion");
        secondaryPotionComboBox = JavaFXUtils.createItemCombobox(core, false, new int[]{Potion.PRAYER_POTION.getFullID(), Potion.ABSORPTION_POTION.getFullID()});
        secondaryPotionComboBox.setPrefWidth(180);


        // ABSORPTION SETTINGS
        Label absorptionSettings = new Label("Absorption settings");

        Label lowerHPMethodLabel = new Label("Lower HP method");
        lowerHPMethodComboBox = JavaFXUtils.createItemCombobox(core, new int[]{LowerHealthMethod.ROCK_CAKE.getItemID(), LowerHealthMethod.LOCATOR_ORB.getItemID()});
        lowerHPMethodComboBox.setPrefWidth(180);

        suicideNoBoost = new CheckBox("Suicide when out of stat boost potions (Better xp)");
        suicideNoBoost.setPrefWidth(180);
        suicideNoBoost.setStyle("-fx-text-fill: white");
        suicideNoBoost.setWrapText(true);

        flickRapidHeal = new CheckBox("Flick Rapid heal to keep 1HP (Quick prayers)");
        flickRapidHeal.setPrefWidth(180);
        flickRapidHeal.setWrapText(true);
        flickRapidHeal.setStyle("-fx-text-fill: white");


        absorptionSettingsVbox = new VBox(lowerHPMethodLabel, lowerHPMethodComboBox, suicideNoBoost, flickRapidHeal);
        absorptionSettingsVbox.setSpacing(10);


        VBox potionVBox = new VBox(primaryPotionLabel, boostPotionComboBox, boostPotionAmountBox, secondaryPotionLabel, secondaryPotionComboBox);
        secondaryPotionComboBox.getSelectionModel().selectedItemProperty().addListener((observableValue, integer, t1) -> {
            if (t1 != null && t1 == Potion.ABSORPTION_POTION.getFullID()) {
                potionVBox.getChildren().add(absorptionSettingsVbox);
            } else {
                potionVBox.getChildren().remove(absorptionSettingsVbox);
            }
            Stage stage = null;
            if (getScene() != null && getScene().getWindow() instanceof Stage) {
                stage = (Stage) getScene().getWindow();
                stage.sizeToScene();
            }
        });
        potionVBox.setStyle("-fx-spacing: 5; -fx-padding: 10; -fx-background-color: #636E72");
        TitledPane potionTitledPane = new TitledPane("Potion load out", potionVBox);
        potionTitledPane.setCollapsible(false);
        potionTitledPane.getStyleClass().add("script-manager-titled-pane");

        setRight(potionTitledPane);


        Label afkPositionLabel = new Label("AFK area");
        afkPositionComboBox = new ComboBox<>();
        afkPositionComboBox.getSelectionModel().select(0);
        afkPositionComboBox.getItems().addAll(AFKPosition.values());
        VBox afkPosBox = new VBox(afkPositionLabel, afkPositionComboBox);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(actionEvent -> {
            // close stage
            ((Stage) confirmButton.getScene().getWindow()).close();
        });
        HBox buttonHBox = new HBox(confirmButton);
        buttonHBox.setStyle("-fx-alignment: center-right;");

        HBox bottomHBox = new HBox(afkPosBox, buttonHBox);
        bottomHBox.setMaxWidth(Double.MAX_VALUE);  // Allow it to expand horizontally
        bottomHBox.setStyle("-fx-alignment: center; -fx-padding: 10; -fx-background-color: #636E72");
        HBox.setHgrow(afkPosBox, Priority.ALWAYS); // Make afkPosBox grow to push buttonHBox to the right

        setBottom(bottomHBox);
    }


    public AFKPosition getAFKPosition() {
        return afkPositionComboBox.getValue();
    }

    public Potion getSelectedPrimaryPotion() {
        int selectedID = boostPotionComboBox.getValue();
        return Potion.getPotionForID(selectedID);
    }

    public Potion getSelectedSecondaryPotion() {
        int selectedID = secondaryPotionComboBox.getValue();
        return Potion.getPotionForID(selectedID);
    }

    public int getMainWeaponItemId() {
        if (mainWeaponItemId == ItemID.BANK_FILLER) {
            return -1;
        }
        return mainWeaponItemId;
    }

    public int getShieldItemId() {
        if (shieldItemId == ItemID.BANK_FILLER) {
            return -1;
        }
        return shieldItemId;
    }

    public SpecialAttackWeapon getSpecialAttackWeapon() {
        Integer selectedID = specialItemComboBox.getValue();
        if (selectedID != null) {
            return SpecialAttackWeapon.forID(selectedID);
        }
        return null;
    }

    public Potion getPrimaryPotion() {
        int selectedID = boostPotionComboBox.getValue();
        if (selectedID != -1) {
            return Potion.getPotionForID(selectedID);
        }
        return null;
    }

    public Potion getSecondaryPotion() {
        int selectedID = secondaryPotionComboBox.getValue();
        if (selectedID != -1) {
            return Potion.getPotionForID(selectedID);
        }
        return null;
    }

    public LowerHealthMethod getLowerHPMethod() {
        int selectedID = lowerHPMethodComboBox.getValue();
        if (selectedID != -1) {
            return LowerHealthMethod.forID(selectedID);
        }
        return null;
    }

    public boolean suicideNoBoost() {
        return suicideNoBoost.isSelected();
    }

    public boolean flickRapidHeal() {
        return flickRapidHeal.isSelected();
    }
}
