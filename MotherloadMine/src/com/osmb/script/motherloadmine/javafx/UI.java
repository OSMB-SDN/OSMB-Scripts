package com.osmb.script.motherloadmine.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.script.motherloadmine.MotherloadMine;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class UI extends VBox {

    private final ComboBox<MotherloadMine.MineArea> areaComboBox;

    private static final Preferences prefs = Preferences.userNodeForPackage(UI.class);
    private static final String PREF_SELECTED_METHOD = "mlm_selected_area";

    public UI(ScriptCore core) {
        MotherloadMine.MineArea savedArea = getSavedArea();
        setStyle("-fx-spacing: 10; -fx-alignment: center; -fx-padding: 5; -fx-background-color: #636E72");
        Label methodLabel = new Label("Select your preferred area to mine");
        getChildren().add(methodLabel);

        areaComboBox = new ComboBox<>();
        areaComboBox.getItems().addAll(MotherloadMine.MineArea.values());
        areaComboBox.getSelectionModel().select(savedArea);
        getChildren().add(areaComboBox);

        Button confirmButton = new Button("Confirm");
        getChildren().add(confirmButton);
        confirmButton.setOnAction(actionEvent -> {
            MotherloadMine.MineArea selectedArea = areaComboBox.getSelectionModel().getSelectedItem();
            if (selectedArea == null) {
                return;
            }

            prefs.put(PREF_SELECTED_METHOD, selectedArea.name());

            ((Stage) confirmButton.getScene().getWindow()).close();
        });
    }

    public MotherloadMine.MineArea getSelectedArea() {
        return areaComboBox.getSelectionModel().getSelectedItem();
    }


    private MotherloadMine.MineArea getSavedArea() {
        try {
            return MotherloadMine.MineArea.valueOf(prefs.get(PREF_SELECTED_METHOD, MotherloadMine.MineArea.TOP.name()));
        } catch (IllegalArgumentException e) {
            return MotherloadMine.MineArea.TOP;
        }
    }
}
