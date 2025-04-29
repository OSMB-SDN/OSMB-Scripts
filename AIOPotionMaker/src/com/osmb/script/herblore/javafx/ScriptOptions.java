package com.osmb.script.herblore.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.herblore.AIOHerblore;
import com.osmb.script.herblore.data.ItemIdentifier;
import com.osmb.script.herblore.data.MixedPotion;
import com.osmb.script.herblore.method.PotionMixer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.prefs.Preferences;

public class ScriptOptions extends VBox {

    private final VBox scriptContentBox;
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptOptions.class);
    private static final String PREF_SELECTED_MIXER = "aiopotionmaker_selected_mixer";

    public ScriptOptions(AIOHerblore script, PotionMixer[] potionMixers) {
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10");

        scriptContentBox = new VBox();
        scriptContentBox.setStyle("-fx-spacing: 10;-fx-alignment: center-left");

        Label label = new Label("Choose the type of potion to make");
        ComboBox<PotionMixer> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(200);
        comboBox.getItems().addAll(potionMixers);

        loadSavedMixer(comboBox, potionMixers);

        comboBox.setOnAction(actionEvent -> {
            PotionMixer selectedPotionMixer = comboBox.getSelectionModel().getSelectedItem();
            if (selectedPotionMixer == null) return;
            //clear the current child nodes
            Platform.runLater(() -> {
                scriptContentBox.getChildren().clear();
                selectedPotionMixer.provideUIOptions(scriptContentBox);
                scriptContentBox.setAlignment(Pos.CENTER_LEFT);
                Stage stage = null;
                if (getScene() != null && getScene().getWindow() instanceof Stage) {
                    stage = (Stage) getScene().getWindow();
                }
            });
        });

        Button button = new Button("Confirm");
        button.setOnAction(actionEvent -> {

            PotionMixer selectedPotionMixer = comboBox.getValue();
            if (selectedPotionMixer != null) {

                if (!selectedPotionMixer.uiOptionsSufficient()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid script options", ButtonType.OK);
                    alert.initOwner(getScene().getWindow());
                    alert.showAndWait();
                    return;
                }
                script.setSelectedMethod(selectedPotionMixer);
                //stage.close();
                saveSelectedMixer(selectedPotionMixer);
                ((Stage) button.getScene().getWindow()).close();
            }
        });
        HBox buttonHbox = new HBox(button);
        buttonHbox.setAlignment(Pos.CENTER_RIGHT);
        getChildren().addAll(label, comboBox, scriptContentBox, buttonHbox);
    }

    public static ComboBox<ItemIdentifier> createItemCombobox(ScriptCore core, ItemIdentifier[] values) {
        ComboBox<ItemIdentifier> productComboBox = new ComboBox<>();
        productComboBox.setPrefWidth(200);
        productComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ItemIdentifier item) {
                return item != null ? core.getItemManager().getItemName(item.getItemID()) : null;
            }

            @Override
            public ItemIdentifier fromString(String string) {
                // Not needed in this context
                return null;
            }
        });
        productComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ItemIdentifier item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    int itemID = item.getItemID();
                    String name = core.getItemManager().getItemName(itemID);
                    ImageView itemImage = JavaFXUtils.getItemImageView(core, itemID);
                    setGraphic(itemImage);
                    if (item instanceof MixedPotion mixedPotion) {
                        if (mixedPotion == MixedPotion.SUPER_COMBAT_POTION) {
                            name = "Super combat (Torstol)";
                        } else if (mixedPotion == MixedPotion.SUPER_COMBAT_POTION_2) {
                            name = "Super combat (Torstol Potion unf)";
                        }
                    }
                    setText(name);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });
        productComboBox.getItems().addAll(values);
        return productComboBox;
    }

    private void saveSelectedMixer(PotionMixer method) {
        if (method != null) {
            prefs.put(PREF_SELECTED_MIXER, method.name());
        }
    }

    private void loadSavedMixer(ComboBox<PotionMixer> comboBox, PotionMixer[] availableMixers) {
        String saved = prefs.get(PREF_SELECTED_MIXER, null);
        if (saved != null) {
            for (PotionMixer mixer : availableMixers) {
                if (mixer.name().equals(saved)) {
                    comboBox.getSelectionModel().select(mixer);
                    Platform.runLater(() -> {
                        scriptContentBox.getChildren().clear();
                        mixer.provideUIOptions(scriptContentBox);
                        scriptContentBox.setAlignment(Pos.CENTER_LEFT);
                    });
                    return;
                }
            }
        } else {
            comboBox.getSelectionModel().selectFirst();
            comboBox.fireEvent(new ActionEvent());
        }
    }
}
