package com.osmb.script.runecrafting.gotr.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.runecrafting.gotr.CatalyticRune;
import com.osmb.script.runecrafting.gotr.PointStrategy;
import com.osmb.script.runecrafting.gotr.Pouch;
import com.osmb.script.runecrafting.gotr.Rune;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UI extends VBox {
    private final java.util.Map<CatalyticRune, CheckBox> catalyticRuneCheckBoxes = new java.util.HashMap<>();
    private final CheckBox useTalismans;
    private final ToggleButton[] cellButtons;
    // UI components for easier access in getters
    private ToggleGroup pointStrategyToggleGroup;
    private Spinner<Integer> fragmentsSpinner;
    private Spinner<Integer> fragmentsRandomSpinner;
    private ToggleGroup repairToggleGroup;

    public UI(ScriptCore core) {
        setStyle("-fx-padding: 10; -fx-background-color: #636E72; -fx-spacing: 10");

        // --- Guardian configuration (left column, top) ---
        Label label = new Label("Guardian configuration");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox questBox = new VBox(10);
        CatalyticRune[] runes = CatalyticRune.values();
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        int col = 0, rowIdx = 0;
        final int columns = 2;
        for (CatalyticRune r : runes) {
            String questRequired = r.getQuestName();
            if (questRequired == null) continue;
            CheckBox cb = new CheckBox(questRequired);
            ImageView itemImage = JavaFXUtils.getItemImageView(core, r.getRuneId());
            cb.setGraphic(itemImage);
            cb.setStyle("-fx-text-fill: white; ");
            catalyticRuneCheckBoxes.put(r, cb); // Store reference
            HBox wrapper = new HBox();
            wrapper.getChildren().add(cb);
            grid.add(wrapper, col, rowIdx);
            col++;
            if (col == columns) {
                col = 0;
                rowIdx++;
            }
        }
        questBox.getChildren().addAll(label, grid);
        // --- Strategy (left column, bottom) ---
        Label pointsLabel = new Label("Strategy");
        pointsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox pointStrategyBox = new VBox(10);
        pointStrategyToggleGroup = new ToggleGroup();
        for (PointStrategy strategy : PointStrategy.values()) {
            VBox radioWithDesc = new VBox(2);
            RadioButton radio = new RadioButton(strategy.getName());
            if (strategy == PointStrategy.BALANCED) {
                radio.setSelected(true);
            }
            radio.setToggleGroup(pointStrategyToggleGroup);
            Label desc = new Label(strategy.getDescription());
            desc.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");
            radioWithDesc.getChildren().addAll(radio, desc);
            pointStrategyBox.getChildren().add(radioWithDesc);
        }
        // --- Cell Drop Threshold ---
        Label cellDropLabel = new Label("Cell Drop Threshold:");
        cellDropLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        HBox cellDropBox = new HBox(6);
        cellButtons = java.util.Arrays.stream(Rune.Cell.values())
                .map(cell -> {
                    ImageView cellImage = JavaFXUtils.getItemImageView(core, cell.getItemID());
                    ToggleButton btn = new ToggleButton(cell.name());
                    btn.setUserData(cell);
                    btn.setStyle("-fx-font-size: 8px;");
                    btn.setSelected(cell == Rune.Cell.WEAK);
                    btn.setGraphic(cellImage);
                    return btn;
                })
                .toArray(ToggleButton[]::new);

        Label desc = new Label("The above selected cell tiers will be dropped.");
        desc.setStyle("-fx-font-size: 10px; -fx-text-fill: #b2bec3;");
        desc.setWrapText(true);
        desc.setMaxWidth(320);
        cellDropBox.getChildren().addAll(cellButtons);
        VBox cellDropBoxWrapper = new VBox(5, cellDropLabel, cellDropBox, desc);

        // Add selection logic: when a button is selected, select all with lower or equal ordinal
        for (int i = 0; i < cellButtons.length; i++) {
            final int idx = i;
            cellButtons[i].setOnAction(e -> {
                for (int j = 0; j < cellButtons.length; j++) {
                    cellButtons[j].setSelected(j <= idx);
                }
            });
        }

        VBox leftColumn = new VBox(20, questBox, new Separator(), pointsLabel, pointStrategyBox, cellDropBoxWrapper);

        // --- Pouch Repair (Right column, top) ---
        Label pouchRepairLabel = new Label("Pouch Repair");
        pouchRepairLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        VBox repairTypeBox = new VBox(15);
        repairToggleGroup = new ToggleGroup();
        RadioButton noneRadio = new RadioButton("None");
        noneRadio.setToggleGroup(repairToggleGroup);
        noneRadio.setSelected(true);
        repairTypeBox.getChildren().add(noneRadio);
        for (Pouch.RepairType repairType : Pouch.RepairType.values()) {
            RadioButton radio = new RadioButton(repairType.getName());
            radio.setToggleGroup(repairToggleGroup);
            repairTypeBox.getChildren().add(radio);
        }
        // --- Miscellaneous (right column, bottom) ---
        Label miscLabel = new Label("Miscellaneous");
        miscLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label targetFragmentsLabel = new Label("Target Fragments:");

        fragmentsSpinner = new Spinner<>(50, 200, 130);
        fragmentsSpinner.setPrefWidth(70);
        fragmentsSpinner.setEditable(true);
        Label fragmentRandomLabel = new Label("Randomise by:");

        fragmentsRandomSpinner = new Spinner<>(0, 20, 10);
        fragmentsRandomSpinner.setPrefWidth(70);


        VBox fragmentsVBox = new VBox(6);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox targetFragmentsBox = new HBox(targetFragmentsLabel, spacer, fragmentsSpinner);
        targetFragmentsBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        HBox fragmentRandomBox = new HBox(fragmentRandomLabel, spacer2, fragmentsRandomSpinner);
        fragmentRandomBox.setAlignment(Pos.CENTER_LEFT);

        fragmentsVBox.getChildren().addAll(targetFragmentsBox, fragmentRandomBox);

        useTalismans = new CheckBox("Use Talismans");
        useTalismans.setStyle("-fx-text-fill: white; ");
        useTalismans.setSelected(true);

        VBox rightColumn = new VBox(20, pouchRepairLabel, repairTypeBox, new Separator(), miscLabel, fragmentsVBox, useTalismans);

        // --- Layout: 2 columns side by side ---
        HBox mainBox = new HBox(30, leftColumn, rightColumn);
        getChildren().add(mainBox);

        getChildren().add(new Separator());
        // --- Confirm button ---
        Button confirmButton = new Button("Confirm");
        confirmButton.setStyle(" -fx-font-size: 14px; -fx-padding: 10px;-fx-alignment: center");
        confirmButton.setOnAction(event -> {
            // Save preferences
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(UI.class);
            // Save selected catalytic runes
            String selectedRunes = getSelectedCatalyticRunes().stream()
                    .map(CatalyticRune::name)
                    .collect(Collectors.joining(","));
            prefs.put("gotr_selected_catalytic_runes", selectedRunes);
            // Save useTalismans
            prefs.putBoolean("gotr_use_talismans", useTalismans.isSelected());
            // Save point strategy
            PointStrategy selectedStrategy = getSelectedPointStrategy();
            prefs.put("gotr_point_strategy", selectedStrategy != null ? selectedStrategy.name() : "");
            // Save pouch repair type
            Pouch.RepairType repairType = getPouchRepairType();
            prefs.put("gotr_pouch_repair_type", repairType != null ? repairType.name() : "");
            // Save fragments
            prefs.putInt("gotr_target_fragments", getTargetFragmentAmount());
            prefs.putInt("gotr_fragment_random", getFragmentRandomAmount());

            // Save cell buttons
            for (ToggleButton button : cellButtons) {
                Rune.Cell cell = (Rune.Cell) button.getUserData();
                prefs.putBoolean("gotr_cell_" + cell.name(), button.isSelected());
            }

            ((Stage) confirmButton.getScene().getWindow()).close();
        });
        HBox confirmBox = new HBox(confirmButton);
        confirmBox.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(confirmBox);
        loadPreferences();
    }

    public void loadPreferences() {
        java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(UI.class);

        // Load selected catalytic runes
        String selectedRunes = prefs.get("gotr_selected_catalytic_runes", "");
        List<String> selectedRuneNames = List.of(selectedRunes.split(","));
        catalyticRuneCheckBoxes.forEach((rune, cb) -> cb.setSelected(selectedRuneNames.contains(rune.name())));

        // Load useTalismans
        useTalismans.setSelected(prefs.getBoolean("gotr_use_talismans", true));

        // Load point strategy
        String strategyName = prefs.get("gotr_point_strategy", "");
        for (Toggle toggle : pointStrategyToggleGroup.getToggles()) {
            if (toggle instanceof RadioButton radio) {
                for (PointStrategy strategy : PointStrategy.values()) {
                    if (strategy.getName().equals(radio.getText()) && strategy.name().equals(strategyName)) {
                        radio.setSelected(true);
                    }
                }
            }
        }

        // Load pouch repair type
        String repairTypeName = prefs.get("gotr_pouch_repair_type", "");
        for (Toggle toggle : repairToggleGroup.getToggles()) {
            if (toggle instanceof RadioButton radio) {
                if (repairTypeName.isEmpty() && "None".equals(radio.getText())) {
                    radio.setSelected(true);
                } else {
                    for (Pouch.RepairType type : Pouch.RepairType.values()) {
                        if (type.getName().equals(radio.getText()) && type.name().equals(repairTypeName)) {
                            radio.setSelected(true);
                        }
                    }
                }
            }
        }

        // load cell buttons
        for (ToggleButton button : cellButtons) {
            Rune.Cell cell = (Rune.Cell) button.getUserData();
            boolean isSelected = prefs.getBoolean("gotr_cell_" + cell.name(), cell == Rune.Cell.WEAK);
            button.setSelected(isSelected);
        }

        // Load fragments
        fragmentsSpinner.getValueFactory().setValue(prefs.getInt("gotr_target_fragments", 130));
        fragmentsRandomSpinner.getValueFactory().setValue(prefs.getInt("gotr_fragment_random", 10));
    }

    public PointStrategy getSelectedPointStrategy() {
        Toggle selected = pointStrategyToggleGroup.getSelectedToggle();
        if (selected instanceof RadioButton radio) {
            String text = radio.getText();
            for (PointStrategy strategy : PointStrategy.values()) {
                if (strategy.getName().equals(text)) {
                    return strategy;
                }
            }
        }
        return null;
    }

    public int getCellDropThreshold() {
        int highestTier = Rune.Cell.WEAK.getTier(); // Default to WEAK
        for (ToggleButton button : cellButtons) {
            if (button.isSelected()) {
                Rune.Cell cell = (Rune.Cell) button.getUserData();
                if (cell.getTier() > highestTier) {
                    highestTier = cell.getTier();
                }
            }
        }
        return highestTier;
    }

    public boolean isUsingTalismans() {
        return useTalismans.isSelected();
    }

    public List<CatalyticRune> getSelectedCatalyticRunes() {
        return catalyticRuneCheckBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public int getTargetFragmentAmount() {
        return fragmentsSpinner.getValue();
    }

    public int getFragmentRandomAmount() {
        return fragmentsRandomSpinner.getValue();
    }

    public Pouch.RepairType getPouchRepairType() {
        Toggle selected = repairToggleGroup.getSelectedToggle();
        if (selected instanceof RadioButton radio) {
            String text = radio.getText();
            if (!"None".equals(text)) {
                for (Pouch.RepairType type : Pouch.RepairType.values()) {
                    if (type.getName().equals(text)) {
                        return type;
                    }
                }
            }
        }
        return null;
    }
}