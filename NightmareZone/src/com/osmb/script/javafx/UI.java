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

import java.util.prefs.Preferences;

public class UI extends BorderPane {

    private static final Preferences prefs = Preferences.userNodeForPackage(UI.class);
    private static final String PREF_MAIN_WEAPON = "nmz_main_weapon";
    private static final String PREF_SHIELD = "nmz_shield";
    private static final String PREF_SPECIAL_WEAPON = "nmz_special_weapon";
    private static final String PREF_BOOST_POTION = "nmz_boost_potion";
    private static final String PREF_BOOST_AMOUNT = "nmz_boost_amount";
    private static final String PREF_SECONDARY_POTION = "nmz_secondary_potion";
    private static final String PREF_LOWER_HP_METHOD = "nmz_lower_hp_method";
    private static final String PREF_RAPID_HEAL = "nmz_flick_rapid_heal";
    private static final String PREF_SUICIDE_NO_BOOST = "nmz_suicide_no_boost";
    private static final String PREF_AFK_POSITION = "nmz_afk_position";

    private final ComboBox<Integer> lowerHPMethodComboBox;
    private final ComboBox<Integer> secondaryPotionComboBox;
    private final ComboBox<Integer> boostPotionComboBox;
    private final CheckBox flickRapidHeal;
    private final VBox absorptionSettingsVbox;
    private final CheckBox suicideNoBoost;
    private final ComboBox<Integer> specialItemComboBox;
    private final ComboBox<AFKPosition> afkPositionComboBox;
    private final Spinner<Integer> boostPotionAmountSpinner;
    private ImageView shieldImageView;
    private ImageView mainWeaponImageView;
    private int mainWeaponItemId = -1;
    private int shieldItemId = -1;

    public UI(ScriptCore core) {
        VBox leftContainer = new VBox();
        // main weapon
        Label mainWeaponLabel = new Label("Main weapon");
        mainWeaponItemId = prefs.getInt(PREF_MAIN_WEAPON, ItemID.BANK_FILLER);
        mainWeaponImageView = JavaFXUtils.getItemImageView(core, mainWeaponItemId);
        Button itemSearchButton = new Button("\uD83D\uDD0E");
        itemSearchButton.setOnAction(actionEvent -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) itemSearchButton.getScene().getWindow());
            if (itemID == -1) itemID = ItemID.BANK_FILLER;
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
        shieldItemId = prefs.getInt(PREF_SHIELD, ItemID.BANK_FILLER);
        shieldImageView = JavaFXUtils.getItemImageView(core, shieldItemId);
        Button itemSearchButton2 = new Button("\uD83D\uDD0E");
        itemSearchButton2.setOnAction(actionEvent -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) itemSearchButton2.getScene().getWindow());
            if (itemID == -1) itemID = ItemID.BANK_FILLER;
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
        Label specialAttackWeaponLabel = new Label("Special attack weapon");
        specialItemComboBox = JavaFXUtils.createItemCombobox(core, true, SpecialAttackWeapon.getItemIDs());
        specialItemComboBox.setPrefWidth(180);
        specialItemComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_SPECIAL_WEAPON, SpecialAttackWeapon.NONE.getItemID())));

        VBox weaponVBox = new VBox(weaponShieldHBox, specialAttackWeaponLabel, specialItemComboBox);
        weaponVBox.setStyle("-fx-spacing: 5; -fx-padding: 10; -fx-background-color: #636E72");

        TitledPane weaponTitledPane = new TitledPane("Weapon load out", weaponVBox);
        weaponTitledPane.setCollapsible(false);
        weaponTitledPane.getStyleClass().add("script-manager-titled-pane");
        VBox.setVgrow(weaponTitledPane, Priority.ALWAYS);
        weaponTitledPane.setMaxHeight(Double.MAX_VALUE);
        leftContainer.getChildren().add(weaponTitledPane);

        Label afkPositionLabel = new Label("AFK area");
        afkPositionComboBox = new ComboBox<>();
        afkPositionComboBox.getItems().addAll(AFKPosition.values());
        afkPositionComboBox.getSelectionModel().select(prefs.getInt(PREF_AFK_POSITION, 0));

        suicideNoBoost = new CheckBox("Suicide when out of stat boost potions (Better xp)");
        suicideNoBoost.setPrefWidth(180);
        suicideNoBoost.setStyle("-fx-text-fill: white");
        suicideNoBoost.setWrapText(true);
        suicideNoBoost.setSelected(prefs.getBoolean(PREF_SUICIDE_NO_BOOST, false));

        VBox miscBox = new VBox(afkPositionLabel, afkPositionComboBox, suicideNoBoost);
        miscBox.setSpacing(10);
        TitledPane misc = new TitledPane("Misc", miscBox);
        misc.setCollapsible(false);
        misc.getStyleClass().add("script-manager-titled-pane");
        leftContainer.getChildren().add(misc);

        setLeft(leftContainer);

        Label primaryPotionLabel = new Label("Stat boost potion");
        boostPotionComboBox = JavaFXUtils.createItemCombobox(core, true, new int[]{Potion.OVERLOAD.getFullID(), Potion.SUPER_COMBAT.getFullID(), Potion.SUPER_RANGING_POTION.getFullID(), Potion.SUPER_MAGIC_POTION.getFullID(), Potion.RANGING_POTION.getFullID()});
        boostPotionComboBox.setPrefWidth(180);
        boostPotionComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_BOOST_POTION, Potion.OVERLOAD.getFullID())));

        boostPotionAmountSpinner = new Spinner<>();
        boostPotionAmountSpinner.setPrefWidth(60);
        boostPotionAmountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 25, prefs.getInt(PREF_BOOST_AMOUNT, 4)));
        HBox boostPotionAmountBox = new HBox(new Label("Boost potion amount"), boostPotionAmountSpinner);
        boostPotionAmountBox.setStyle("-fx-spacing: 10; -fx-alignment: center");

        Label secondaryPotionLabel = new Label("Secondary potion");
        secondaryPotionComboBox = JavaFXUtils.createItemCombobox(core, false, new int[]{Potion.PRAYER_POTION.getFullID(), Potion.ABSORPTION_POTION.getFullID()});
        secondaryPotionComboBox.setPrefWidth(180);
        secondaryPotionComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_SECONDARY_POTION, Potion.PRAYER_POTION.getFullID())));

        Label lowerHPMethodLabel = new Label("Lower HP method");
        lowerHPMethodComboBox = JavaFXUtils.createItemCombobox(core, new int[]{LowerHealthMethod.ROCK_CAKE.getItemID(), LowerHealthMethod.LOCATOR_ORB.getItemID()});
        lowerHPMethodComboBox.setPrefWidth(180);
        lowerHPMethodComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_LOWER_HP_METHOD, LowerHealthMethod.ROCK_CAKE.getItemID())));

        flickRapidHeal = new CheckBox("Flick Rapid heal to keep 1HP (Quick prayers)");
        flickRapidHeal.setPrefWidth(180);
        flickRapidHeal.setWrapText(true);
        flickRapidHeal.setStyle("-fx-text-fill: white");
        flickRapidHeal.setSelected(prefs.getBoolean(PREF_RAPID_HEAL, false));

        absorptionSettingsVbox = new VBox(lowerHPMethodLabel, lowerHPMethodComboBox, flickRapidHeal);
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
        potionVBox.setPrefHeight(300);

        TitledPane potionTitledPane = new TitledPane("Potion load out", potionVBox);
        potionTitledPane.setCollapsible(false);
        potionTitledPane.getStyleClass().add("script-manager-titled-pane");

        // Create a container VBox for the titled pane
        VBox rightContainer = new VBox(potionTitledPane);
        rightContainer.setStyle("-fx-background-color: #636E72");
        VBox.setVgrow(potionTitledPane, Priority.ALWAYS);  // Make titled pane grow
        rightContainer.setMaxHeight(Double.MAX_VALUE);

        setRight(rightContainer);

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(actionEvent -> {
            // close stage
            prefs.putInt(PREF_MAIN_WEAPON, mainWeaponItemId);
            prefs.putInt(PREF_SHIELD, shieldItemId);
            prefs.putInt(PREF_SPECIAL_WEAPON, specialItemComboBox.getValue());
            prefs.putInt(PREF_BOOST_POTION, boostPotionComboBox.getValue());
            prefs.putInt(PREF_BOOST_AMOUNT, boostPotionAmountSpinner.getValue());
            prefs.putInt(PREF_SECONDARY_POTION, secondaryPotionComboBox.getValue());
            prefs.putInt(PREF_LOWER_HP_METHOD, lowerHPMethodComboBox.getValue());
            prefs.putBoolean(PREF_RAPID_HEAL, flickRapidHeal.isSelected());
            prefs.putBoolean(PREF_SUICIDE_NO_BOOST, suicideNoBoost.isSelected());
            prefs.putInt(PREF_AFK_POSITION, afkPositionComboBox.getSelectionModel().getSelectedIndex());
            ((Stage) confirmButton.getScene().getWindow()).close();
        });

        HBox buttonHBox = new HBox(confirmButton);
        buttonHBox.setStyle("-fx-alignment: center-right;");

        HBox bottomHBox = new HBox(miscBox, buttonHBox);
        bottomHBox.setMaxWidth(Double.MAX_VALUE);  // Allow it to expand horizontally
        bottomHBox.setStyle("-fx-alignment: center-right; -fx-padding: 10; -fx-background-color: #636E72");
        HBox.setHgrow(miscBox, Priority.ALWAYS); // Make afkPosBox grow to push buttonHBox to the right

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

    public int getBoostPotionAmount() {
        return boostPotionAmountSpinner.getValue();
    }

    public boolean suicideNoBoost() {
        return suicideNoBoost.isSelected();
    }

    public boolean flickRapidHeal() {
        return flickRapidHeal.isSelected();
    }
}
