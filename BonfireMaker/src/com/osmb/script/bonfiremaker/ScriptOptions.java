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

    private static final int[] LOGS = new int[]{ItemID.LOGS, ItemID.OAK_LOGS, ItemID.WILLOW_LOGS, ItemID.MAPLE_LOGS, ItemID.TEAK_LOGS, ItemID.ARCTIC_PINE_LOGS, ItemID.MAPLE_LOGS, ItemID.MAHOGANY_LOGS, ItemID.YEW_LOGS, ItemID.BLISTERWOOD_LOGS, ItemID.MAGIC_LOGS, ItemID.REDWOOD_LOGS};
    private ComboBox<Integer> logComboBox;

    private static final Preferences prefs = Preferences.userNodeForPackage(ScriptOptions.class);
    private static final String PREF_SELECTED_LOG_ID = "bonfiremaker_selected_log_id";

    public Scene buildScene(ScriptCore core) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10; -fx-alignment: center");

        Label logLabel = new Label("Choose log to burn");
        logComboBox = JavaFXUtils.createItemCombobox(core, LOGS);

        // Load saved selection
        int savedLogId = prefs.getInt(PREF_SELECTED_LOG_ID, ItemID.LOGS);
        for (Integer logId : LOGS) {
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
