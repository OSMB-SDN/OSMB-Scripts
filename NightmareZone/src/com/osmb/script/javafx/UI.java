package com.osmb.script.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.AFKPosition;
import com.osmb.script.Ammo;
import com.osmb.script.LowerHealthMethod;
import com.osmb.script.SpecialAttackWeapon;
import com.osmb.script.potion.BarrelPotion;
import com.osmb.script.potion.Potion;
import com.osmb.script.potion.StandardPotion;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class UI extends BorderPane {

    private static final Preferences prefs = Preferences.userNodeForPackage(UI.class);
    private static final String PREF_MAIN_WEAPON = "nmz_main_weapon";
    private static final String PREF_SHIELD = "nmz_shield";
    private static final String PREF_SPECIAL_WEAPON = "nmz_special_weapon";
    private static final String PREF_AMMO = "nmz_ammo";
    private static final String PREF_BOOST_POTION = "nmz_boost_potion";
    private static final String PREF_BOOST_AMOUNT = "nmz_boost_amount";
    private static final String PREF_SECONDARY_POTION = "nmz_secondary_potion";
    private static final String PREF_LOWER_HP_METHOD = "nmz_lower_hp_method";
    private static final String PREF_RAPID_HEAL = "nmz_flick_rapid_heal";
    private static final String PREF_SUICIDE_NO_BOOST = "nmz_suicide_no_boost";
    private static final String PREF_AFK_POSITION = "nmz_afk_position";

    private ComboBox<Integer> lowerHPMethodComboBox;
    private ComboBox<Integer> secondaryPotionComboBox;
    private ComboBox<Integer> boostPotionComboBox;
    private CheckBox flickRapidHeal;
    private CheckBox suicideNoBoost;
    private ComboBox<Integer> specialItemComboBox;
    private ComboBox<AFKPosition> afkPositionComboBox;
    private Spinner<Integer> boostPotionAmountSpinner;
    private ImageView shieldImageView;
    private ImageView mainWeaponImageView;
    private int mainWeaponItemId = -1;
    private int shieldItemId = -1;
    private ComboBox<Integer> ammoComboBox;

    public UI(ScriptCore core) {
        setStyle("-fx-background-color: #636E72");
        HBox topHBox = new HBox(
                buildWeaponLoadOutCard(core),
                new Separator(Orientation.VERTICAL),
                buildPotionCard(core),
                new Separator(Orientation.VERTICAL),
                buildMiscCard()
        );
        setCenter(topHBox);
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(actionEvent -> {
            // close stage
            prefs.putInt(PREF_MAIN_WEAPON, mainWeaponItemId);
            prefs.putInt(PREF_SHIELD, shieldItemId);
            prefs.putInt(PREF_SPECIAL_WEAPON, specialItemComboBox.getValue());
            prefs.putInt(PREF_AMMO, ammoComboBox.getValue());
            prefs.putInt(PREF_BOOST_POTION, boostPotionComboBox.getValue());
            prefs.putInt(PREF_BOOST_AMOUNT, boostPotionAmountSpinner.getValue());
            prefs.putInt(PREF_SECONDARY_POTION, secondaryPotionComboBox.getValue());
            prefs.putInt(PREF_LOWER_HP_METHOD, lowerHPMethodComboBox.getValue());
            prefs.putBoolean(PREF_RAPID_HEAL, flickRapidHeal.isSelected());
            prefs.putBoolean(PREF_SUICIDE_NO_BOOST, suicideNoBoost.isSelected());
            prefs.putInt(PREF_AFK_POSITION, afkPositionComboBox.getSelectionModel().getSelectedIndex());
            ((Stage) confirmButton.getScene().getWindow()).close();
        });


        HBox bottomHBox = new HBox(confirmButton);
        bottomHBox.setMaxWidth(Double.MAX_VALUE);  // Allow it to expand horizontally
        bottomHBox.setStyle("-fx-alignment: center-right; -fx-padding: 5 5 5 0; -fx-background-color: #374146");

        setBottom(bottomHBox);
        Stage stage;
        if (getScene() != null && getScene().getWindow() instanceof Stage) {
            stage = (Stage) getScene().getWindow();
            stage.sizeToScene();
        }
    }

    private VBox buildPotionCard(ScriptCore core) {
        Label titleLabel = new Label("Potion loadout");
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #374146");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setAlignment(Pos.CENTER);
        Label primaryPotionLabel = new Label("Stat boost potion");
        int[] potions = new int[BarrelPotion.values().length - 1 + StandardPotion.values().length - 1];
        int index = 0;
        for (BarrelPotion potion : BarrelPotion.values()) {
            if (potion == BarrelPotion.ABSORPTION_POTION) {
                continue;
            }
            potions[index++] = potion.getFullID();
        }
        for (StandardPotion potion : StandardPotion.values()) {
            if (potion == StandardPotion.PRAYER_POTION) {
                continue;
            }
            potions[index++] = potion.getFullID();
        }
        boostPotionComboBox = JavaFXUtils.createItemCombobox(core, true, potions);
        boostPotionComboBox.setPrefWidth(180);
        boostPotionComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_BOOST_POTION, BarrelPotion.OVERLOAD.getFullID())));

        boostPotionAmountSpinner = new Spinner<>();
        boostPotionAmountSpinner.setPrefWidth(60);
        boostPotionAmountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 25, prefs.getInt(PREF_BOOST_AMOUNT, 4)));
        HBox boostPotionAmountBox = new HBox(new Label("Boost potion amount"), boostPotionAmountSpinner);
        boostPotionAmountBox.setStyle("-fx-spacing: 10; -fx-alignment: center");

        Label secondaryPotionLabel = new Label("Secondary potion");
        secondaryPotionComboBox = JavaFXUtils.createItemCombobox(core, false, new int[]{StandardPotion.PRAYER_POTION.getFullID(), BarrelPotion.ABSORPTION_POTION.getFullID()});
        secondaryPotionComboBox.setPrefWidth(180);

        Label lowerHPMethodLabel = new Label("Lower HP method");
        lowerHPMethodComboBox = JavaFXUtils.createItemCombobox(core, new int[]{LowerHealthMethod.ROCK_CAKE.getItemID(), LowerHealthMethod.LOCATOR_ORB.getItemID()});
        lowerHPMethodComboBox.setPrefWidth(180);
        lowerHPMethodComboBox.getSelectionModel().select(Integer.valueOf(prefs.getInt(PREF_LOWER_HP_METHOD, LowerHealthMethod.ROCK_CAKE.getItemID())));

        VBox potionVBox = new VBox(titleLabel, primaryPotionLabel, boostPotionComboBox, boostPotionAmountBox, secondaryPotionLabel, secondaryPotionComboBox, lowerHPMethodLabel, lowerHPMethodComboBox);

        secondaryPotionComboBox.getSelectionModel().selectedItemProperty().addListener((observableValue, integer, t1) -> {
            boolean absorptionSelected_ = t1 != null && t1 == BarrelPotion.ABSORPTION_POTION.getFullID();
            lowerHPMethodLabel.setDisable(!absorptionSelected_);
            lowerHPMethodComboBox.setDisable(!absorptionSelected_);
            if(flickRapidHeal != null) {
                flickRapidHeal.setDisable(!absorptionSelected_);
            }

            // resize the stage to fit the content
            Stage stage;
            if (getScene() != null && getScene().getWindow() instanceof Stage) {
                stage = (Stage) getScene().getWindow();
                stage.sizeToScene();
            }
        });
        int savedSecondary = prefs.getInt(PREF_SECONDARY_POTION, BarrelPotion.ABSORPTION_POTION.getFullID());
        secondaryPotionComboBox.getSelectionModel().select(Integer.valueOf(savedSecondary));
        boolean absorptionSelected = savedSecondary == BarrelPotion.ABSORPTION_POTION.getFullID();
        lowerHPMethodLabel.setDisable(!absorptionSelected);
        lowerHPMethodComboBox.setDisable(!absorptionSelected);

        potionVBox.setStyle("-fx-spacing: 5; -fx-padding: 10; -fx-background-color: #636E72");
        potionVBox.setPrefHeight(300);

        return potionVBox;
    }

    private VBox buildMiscCard() {
        Label miscLabel = new Label("Miscellaneous");
        miscLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #374146");
        miscLabel.setMaxWidth(Double.MAX_VALUE);
        miscLabel.setAlignment(Pos.CENTER);
        Label afkPositionLabel = new Label("AFK area");
        afkPositionComboBox = new ComboBox<>();
        afkPositionComboBox.getItems().addAll(AFKPosition.values());
        afkPositionComboBox.getSelectionModel().select(prefs.getInt(PREF_AFK_POSITION, 0));

        suicideNoBoost = new CheckBox("Suicide when out of stat boost potions (Better xp)");
        suicideNoBoost.setPrefWidth(180);
        suicideNoBoost.setStyle("-fx-text-fill: white");
        suicideNoBoost.setWrapText(true);
        suicideNoBoost.setSelected(prefs.getBoolean(PREF_SUICIDE_NO_BOOST, false));

        flickRapidHeal = new CheckBox("Flick Rapid heal to keep 1HP (Quick prayers)");
        flickRapidHeal.setPrefWidth(180);
        flickRapidHeal.setWrapText(true);
        flickRapidHeal.setStyle("-fx-text-fill: white");
        flickRapidHeal.setSelected(prefs.getBoolean(PREF_RAPID_HEAL, false));
        boolean absorptionSelected = secondaryPotionComboBox.getSelectionModel().getSelectedItem() == BarrelPotion.ABSORPTION_POTION.getFullID();
        flickRapidHeal.setDisable(!absorptionSelected);

        VBox miscCard = new VBox(miscLabel, afkPositionLabel, afkPositionComboBox, suicideNoBoost, flickRapidHeal);
        miscCard.setMaxHeight(Integer.MAX_VALUE);
        miscCard.setStyle("-fx-spacing: 10; -fx-padding: 10; -fx-background-color: #636E72");
        return miscCard;
    }

    private VBox buildWeaponLoadOutCard(ScriptCore core) {
        Label gearLoadoutLabel = new Label("Gear loadout");
        gearLoadoutLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-color: #374146");
        gearLoadoutLabel.setMaxWidth(Double.MAX_VALUE);
        gearLoadoutLabel.setAlignment(Pos.CENTER);

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
        int savedItemID = prefs.getInt(PREF_SPECIAL_WEAPON, -1);
        if (savedItemID != -1) {
            specialItemComboBox.getSelectionModel().select(Integer.valueOf(savedItemID));
        }

        Label ammoLabel = new Label("Ammo type");
        savedItemID = prefs.getInt(PREF_AMMO, -1);
        ammoComboBox = JavaFXUtils.createItemCombobox(core, true, Ammo.getItemIDs());
        ammoComboBox.setPrefWidth(180);
        if (savedItemID != -1) {
            ammoComboBox.getSelectionModel().select(Integer.valueOf(savedItemID));
        }

        VBox weaponVBox = new VBox(gearLoadoutLabel, weaponShieldHBox, specialAttackWeaponLabel, specialItemComboBox, ammoLabel, ammoComboBox);
        weaponVBox.setStyle("-fx-spacing: 10; -fx-padding: 10; -fx-background-color: #636E72");
        return weaponVBox;
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

    public Ammo getAmmo() {
        Integer selectedID = ammoComboBox.getValue();
        if (selectedID != null) {
            return Ammo.fromItemID(selectedID);
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
