package com.osmb.script.bonfiremaker;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.JavaFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class ScriptOptions {

    private ComboBox<Integer> logComboBox;

    private static final Preferences prefs = Preferences.userNodeForPackage(ScriptOptions.class);
    private static final String PREF_SELECTED_LOG_ID = "bonfiremaker_selected_log_id";

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        Label logLabel = new Label("Choose log to burn");
        logComboBox = JavaFXUtils.createItemCombobox(core, BonfireMaker.LOGS);

        // Load saved selection
        int savedLogId = prefs.getInt(PREF_SELECTED_LOG_ID, ItemID.LOGS);
        for (Integer logId : BonfireMaker.LOGS) {
            if (logId == savedLogId) {
                logComboBox.getSelectionModel().select(logId);
                break;
            }
        }

        root.getChildren().addAll(logLabel, logComboBox);

        Button confirmButton = new Button("Confirm");
        root.getChildren().add(confirmButton);
        Scene scene = new Scene(root);
        confirmButton.setOnAction(actionEvent -> {
            if (logComboBox.getSelectionModel().getSelectedIndex() >= 0) {
                prefs.putInt(PREF_SELECTED_LOG_ID, logComboBox.getSelectionModel().getSelectedItem());
                ((Stage) confirmButton.getScene().getWindow()).close();
            }
        });
        scene.getStylesheets().add("style.css");
        return scene;
    }

    public int getSelectedLog() {
        return logComboBox.getSelectionModel().getSelectedItem();
    }
}
