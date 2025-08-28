package com.osmb.script.farming.tithefarm.javafx;

import com.osmb.script.farming.tithefarm.PatchManager;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ScriptOptions extends VBox {


    private final ToggleButton zigzagButton;
    private final ToggleButton serpentineButton;
    private final ToggleButton randomButton;
    private final Spinner<Integer> plantAmountSpinner;
    private final Slider depositFruitSlider;
    private final Slider depositFruitRandomnessSlider;

    public ScriptOptions() {
        setStyle("-fx-padding: 10; -fx-background-color: #636E72; -fx-spacing: 10");
        Label plantMethodLabel = new Label("Plant Method:");
        plantMethodLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        ToggleGroup plantMethodGroup = new ToggleGroup();
        zigzagButton = new ToggleButton("", new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream("zigzag.png"))));
        serpentineButton = new ToggleButton("", new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream("serpentine.png"))));
        randomButton = new ToggleButton("", new javafx.scene.image.ImageView(new javafx.scene.image.Image(getClass().getResourceAsStream("random.png"))));

        Tooltip serpentineToolTip = new Tooltip("Serpentine - Plants in a serpentine pattern");
        serpentineToolTip.setShowDelay(Duration.millis(100));
        Tooltip.install(serpentineButton, serpentineToolTip);

        Tooltip zigZagToolTip = new Tooltip("Zigzag - Plants in a zigzag pattern");
        zigZagToolTip.setShowDelay(Duration.millis(100));
        Tooltip.install(zigzagButton, zigZagToolTip);

        Tooltip randomToolTip = new Tooltip("Random - Plants in a random pattern every time");
        randomToolTip.setShowDelay(Duration.millis(100));
        Tooltip.install(randomButton, randomToolTip);
        randomButton.setSelected(true);

        zigzagButton.setToggleGroup(plantMethodGroup);
        serpentineButton.setToggleGroup(plantMethodGroup);
        randomButton.setToggleGroup(plantMethodGroup);
        ButtonBar buttonBar = new ButtonBar();
        buttonBar.setPrefWidth(0);
        buttonBar.setButtonOrder(ButtonBar.BUTTON_ORDER_NONE);
        buttonBar.setPadding(javafx.geometry.Insets.EMPTY);
        buttonBar.getButtons().addAll(randomButton, zigzagButton, serpentineButton);

        VBox plantMethodBox = new VBox(5, plantMethodLabel, buttonBar);

        Label plantAmountLabel = new Label("Plant Amount:");
        plantAmountLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        plantAmountSpinner = new Spinner<>(8, 16, 16);
        plantAmountSpinner.setPrefWidth(60);

        HBox plantAmountRow = new HBox(plantAmountSpinner);
        plantAmountRow.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        plantAmountRow.setAlignment(Pos.BOTTOM_RIGHT);
        VBox plantAmountBox = new VBox(5, plantAmountLabel, plantAmountRow);
        VBox.setVgrow(plantAmountRow, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(plantAmountBox, javafx.scene.layout.Priority.ALWAYS);
        HBox topBox = new HBox(10, plantMethodBox, plantAmountBox);
        HBox.setHgrow(plantAmountBox, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(topBox, new Separator());

        Label depositFruitLabel = new Label("Fruit depositing:");
        depositFruitLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        depositFruitSlider = new Slider(100, 10000, 300);
        depositFruitSlider.setBlockIncrement(100);
        depositFruitSlider.setMajorTickUnit(100);
        depositFruitSlider.setMinorTickCount(0);
        depositFruitSlider.setSnapToTicks(true);
        depositFruitSlider.setShowTickLabels(true);
        depositFruitSlider.setShowTickMarks(true);

        depositFruitRandomnessSlider = new Slider(100, 1000, 200);
        depositFruitRandomnessSlider.setBlockIncrement(100);
        depositFruitRandomnessSlider.setMajorTickUnit(100);
        depositFruitRandomnessSlider.setMinorTickCount(0);
        depositFruitRandomnessSlider.setShowTickMarks(true);
        depositFruitRandomnessSlider.setShowTickLabels(true);
        depositFruitRandomnessSlider.setSnapToTicks(true);
        double sliderValue = depositFruitSlider.getValue();
        Label depositFruitSummaryLabel = new Label("Deposit between " + (sliderValue + "-" + (sliderValue + depositFruitRandomnessSlider.getValue())) + " fruit.");
        depositFruitSummaryLabel.setStyle("-fx-font-size: 13px;");
        getChildren().addAll(depositFruitLabel, depositFruitSlider, depositFruitRandomnessSlider, depositFruitSummaryLabel);

        // Update summary label when sliders change
        depositFruitSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int maxValue = Math.min(10000, newVal.intValue() + (int) depositFruitRandomnessSlider.getValue());
            depositFruitSummaryLabel.setText("Deposit between " + newVal.intValue() + "-" + maxValue + " fruit.");
        });

        depositFruitRandomnessSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int maxValue = Math.min(10000, (int) depositFruitSlider.getValue() + newVal.intValue());
            depositFruitSummaryLabel.setText("Deposit between " + (int) depositFruitSlider.getValue() + "-" + maxValue + " fruit.");
        });

        Button confirmButton = new Button("Confirm");
        confirmButton.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-alignment: center-right");
        confirmButton.setOnAction(event -> {
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(ScriptOptions.class);
            // Example: Save plant method selection
            String plantMethod = zigzagButton.isSelected() ? "zigzag" : serpentineButton.isSelected() ? "serpentine" : "none";
            prefs.put("plant_method", plantMethod);

            // Example: Save plant amount
            prefs.putInt("plant_amount", plantAmountSpinner.getValue());

            // Example: Save fruit deposit options
            prefs.putInt("deposit_fruit_min", (int) depositFruitSlider.getValue());
            prefs.putInt("deposit_fruit_randomness", (int) depositFruitRandomnessSlider.getValue());

            ((Stage) confirmButton.getScene().getWindow()).close();
        });
        HBox buttonBox = new HBox(confirmButton);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        getChildren().add(buttonBox);
        loadOptionsFromPrefs(zigzagButton, serpentineButton, plantAmountSpinner, depositFruitSlider, depositFruitRandomnessSlider);
    }

    public void loadOptionsFromPrefs(ToggleButton zigzagButton, ToggleButton serpentineButton, Spinner<Integer> plantAmountSpinner, Slider depositFruitSlider, Slider depositFruitRandomnessSlider) {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(ScriptOptions.class);

        // Load plant method
        String plantMethod = prefs.get("plant_method", "zigzag");
        if (plantMethod.equals("zigzag")) {
            zigzagButton.setSelected(true);
        } else if (plantMethod.equals("serpentine")) {
            serpentineButton.setSelected(true);
        }

        // Load plant amount
        int plantAmount = prefs.getInt("plant_amount", 16);
        plantAmountSpinner.getValueFactory().setValue(plantAmount);

        // Load fruit deposit options
        int depositMin = prefs.getInt("deposit_fruit_min", 300);
        int depositRandomness = prefs.getInt("deposit_fruit_randomness", 200);
        depositFruitSlider.setValue(depositMin);
        depositFruitRandomnessSlider.setValue(depositRandomness);
    }

    public int getPlantAmount() {
        return plantAmountSpinner.getValue();
    }

    public PatchManager.PlantMethod getPlantMethod() {
        if (zigzagButton.isSelected()) {
            return PatchManager.PlantMethod.ZIG_ZAG;
        } else if (serpentineButton.isSelected()) {
            return PatchManager.PlantMethod.SERPENTINE;
        } else if (randomButton.isSelected()) {
            return PatchManager.PlantMethod.RANDOM;
        }
        return null; // or throw an exception if neither is selected
    }

    public int getDepositFruitMin() {
        return (int) depositFruitSlider.getValue();
    }

    public int getDepositFruitMax() {
        return Math.min(10000, (int) depositFruitSlider.getValue() + (int) depositFruitRandomnessSlider.getValue());
    }


}
