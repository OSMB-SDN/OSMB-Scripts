package com.osmb.script.masterfarmerthiever.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.definition.ItemDefinition;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ZoomType;
import com.osmb.api.javafx.ColorPickerPanel;
import com.osmb.script.masterfarmerthiever.MasterFarmerTheiver;
import com.osmb.script.masterfarmerthiever.Seed;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Pair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ScriptOptions extends VBox {

    public static final Color MENU_COLOR_BACKGROUND = new Color(99, 110, 114);
    private final Button confirmButton;
    private final Label totalSlotsLabel;
    private CheckBox dodgyNecklaceCheckbox;
    private Spinner<Integer> dodgyNecklaceAmountSpinner;
    private HBox dodgyNecklaceAmountContainer;
    private int totalSlots = 0;
    private Map<CheckBox, Seed> seedCheckboxes = new HashMap<>();
    private CheckBox foodCheckbox;
    private VBox foodOptionsContainer;
    private Spinner<Integer> foodAmountSpinner;
    private TextField foodIdTextField;
    private Button colorPickerButton;
    private TextField rgbTextfield;
    private int highlightColor;
    private Canvas colorCanvas;

    public ScriptOptions(MasterFarmerTheiver script) {
        setStyle("-fx-background-color: rgba(58, 65, 66, 1); -fx-spacing: 10; -fx-padding: 10; -fx-alignment: center");

        Node colorSection = buildColorSection();
        HBox.setHgrow(colorSection, Priority.ALWAYS);

        Node foodSection = buildFoodSection();
        HBox.setHgrow(foodSection, Priority.ALWAYS);

        Node dodgyNecklaceSection = buildDodgyNecklaceSection(script);
        HBox.setHgrow(dodgyNecklaceSection, Priority.ALWAYS);

        HBox optionsGroup = new HBox(colorSection, foodSection, dodgyNecklaceSection);
        optionsGroup.setAlignment(Pos.CENTER);
        optionsGroup.setSpacing(10);
        VBox.setVgrow(optionsGroup, Priority.ALWAYS);
        getChildren().add(optionsGroup);

        Node seedSection = buildSeedCheckboxes(script);
        getChildren().add(seedSection);

        totalSlotsLabel = new Label("Total Slots: " + totalSlots);
        getChildren().add(totalSlotsLabel);

        // confirm button
        confirmButton = new Button("Confirm");
        getChildren().add(confirmButton);

        initNodeListeners(script);
    }

    public static void main(String[] args) {
        Color c = new Color(-26593);
    }

    public static ImageView getUIImage(ScriptCore core, int itemID) {
        com.osmb.api.visual.image.Image itemImage = core.getItemManager().getItemImage(itemID, 1, ZoomType.SIZE_1, MENU_COLOR_BACKGROUND.getRGB());
        if (itemImage == null) {
            System.out.println("Item image is null for item: " + itemID);
            return null;
        }
        BufferedImage itemBufferedImage = itemImage.toBufferedImage();
        Image fxImage = SwingFXUtils.toFXImage(itemBufferedImage, null);
        return new javafx.scene.image.ImageView(fxImage);
    }

    public static javafx.scene.paint.Color argbToColor(int rgb) {
        // Extract color components
        int red = (rgb >> 16) & 0xFF;   // Extract red (8 bits)
        int green = (rgb >> 8) & 0xFF;  // Extract green (8 bits)
        int blue = rgb & 0xFF;          // Extract blue (8 bits)

        return new javafx.scene.paint.Color(red / 255.0, green / 255.0, blue / 255.0, 1);
    }

    private void showAlert(Window parent, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStyleClass().add("dark-dialogue");
        alert.initOwner(parent);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void initNodeListeners(ScriptCore core) {
        confirmButton.setOnAction(actionEvent -> {
            Window window = confirmButton.getScene().getWindow();
            int selectedColor;
            if (rgbTextfield.getText().isEmpty()) {
                try {
                    selectedColor = Integer.parseInt(rgbTextfield.getText());
                } catch (Exception e) {
                    showAlert(window, "Invalid highlight color", "Please provide the highlighted color of the NPC.");
                    return;
                }
                this.highlightColor = selectedColor;
            }
            if (totalSlots >= 24) {
                showAlert(window, "Too many slots.", "Please leave at least 4 slots free.");
            } else {
                if (foodCheckbox.isSelected() && foodIdTextField.getText().isEmpty()) {
                    showAlert(window, "Food ID is empty.", "Please enter food item ID.");
                }
                ((Stage) window).close();
            }
        });

        dodgyNecklaceCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            dodgyNecklaceAmountContainer.setDisable(!newValue);
            updateTotalSlots();
        });

        foodCheckbox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            foodOptionsContainer.setDisable(!newValue);
            updateTotalSlots();
        });

        dodgyNecklaceAmountSpinner.valueProperty().addListener(observable -> updateTotalSlots());
        foodAmountSpinner.valueProperty().addListener(observable -> updateTotalSlots());

        colorPickerButton.setOnAction(actionEvent -> {
            Window window = confirmButton.getScene().getWindow();
            this.highlightColor = ColorPickerPanel.show(core, (Stage) window);
            this.rgbTextfield.setText(String.valueOf(this.highlightColor));
            javafx.scene.paint.Color color = argbToColor(this.highlightColor);
            updateColorBox(color);
        });
    }

    private void updateColorBox(javafx.scene.paint.Color color) {
        GraphicsContext gc = colorCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, colorCanvas.getWidth(), colorCanvas.getHeight());
        gc.setFill(color);
        gc.fillRect(0, 0, colorCanvas.getWidth(), colorCanvas.getHeight());
    }

    private Node buildDodgyNecklaceSection(ScriptCore core) {
        ImageView dodgyNecklaceGraphic = getUIImage(core, ItemID.DODGY_NECKLACE);
        dodgyNecklaceCheckbox = new CheckBox("Use Dodgy Necklaces");
        dodgyNecklaceCheckbox.setStyle("-fx-text-fill: white");
        dodgyNecklaceCheckbox.setGraphic(dodgyNecklaceGraphic);

        Label dodgyNecklaceLabel = new Label("Amount to withdraw");

        dodgyNecklaceAmountSpinner = new Spinner<>();
        dodgyNecklaceAmountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 8, 1));
        dodgyNecklaceAmountSpinner.setPrefWidth(70);

        dodgyNecklaceAmountContainer = new HBox(dodgyNecklaceLabel, dodgyNecklaceAmountSpinner);
        dodgyNecklaceAmountContainer.setStyle("-fx-spacing: 10; -fx-alignment: center-right");
        dodgyNecklaceAmountContainer.setDisable(true);

        VBox necklaceContainer = new VBox(dodgyNecklaceCheckbox, dodgyNecklaceAmountContainer);
        necklaceContainer.setStyle("-fx-spacing: 10;");

        TitledPane titledPane = getTitledPane("Dodgy Necklace", necklaceContainer);
        titledPane.setPrefHeight(140);
        return titledPane;
    }

    private Node buildFoodSection() {
        foodCheckbox = new CheckBox("Use food");
        foodCheckbox.setStyle("-fx-text-fill: white");

        Label foodIdLabel = new Label("Food Item ID");
        foodIdTextField = new TextField();
        // allow numbers
        foodIdTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                foodIdTextField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        foodIdTextField.setPrefWidth(70);
        foodIdTextField.setMaxWidth(70);
        HBox foodIDContainer = new HBox(foodIdLabel, foodIdTextField);
        foodIDContainer.setStyle("-fx-spacing: 10; -fx-alignment: center-right");

        Label foodAmountLabel = new Label("Amount to withdraw");
        foodAmountSpinner = new Spinner<>();
        foodAmountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 28, 1));
        foodAmountSpinner.setPrefWidth(70);
        HBox foodAmountContainer = new HBox(foodAmountLabel, foodAmountSpinner);
        foodAmountContainer.setStyle("-fx-spacing: 10; -fx-alignment: center-right");

        // wrap this in a container so the checkbox disables/enables all
        foodOptionsContainer = new VBox(foodIDContainer, foodAmountContainer);
        foodOptionsContainer.setStyle("-fx-spacing: 10; -fx-alignment: center");
        foodOptionsContainer.setDisable(true);

        VBox foodContainer = new VBox(foodCheckbox, foodOptionsContainer);
        foodContainer.setStyle("-fx-spacing: 10");
        TitledPane titledPane = getTitledPane("Food options", foodContainer);
        titledPane.setPrefHeight(140);
        return titledPane;
    }

    private Node buildColorSection() {
        VBox vBox = new VBox();
        vBox.setStyle("-fx-spacing: 10; -fx-alignment: center");
        vBox.setSpacing(10);
        rgbTextfield = new TextField();
        rgbTextfield.setText("0");
        rgbTextfield.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("-?\\d*")) {
                //update color box
                int colorInteger = 0;
                try {
                    colorInteger = Integer.parseInt(newText);
                } catch (Exception ignored) {
                }
                this.highlightColor = colorInteger;
                javafx.scene.paint.Color color = argbToColor(this.highlightColor);
                updateColorBox(color);
                return change;
            }
            return null;
        }));

        colorPickerButton = new Button();
        javafx.scene.image.Image searchImage = new javafx.scene.image.Image(ScriptCore.class.getResourceAsStream("/color-picker.png"));
        ImageView searchIcon = new ImageView(searchImage);
        searchIcon.setFitWidth(17);
        searchIcon.setFitHeight(17);
        colorPickerButton.setGraphic(searchIcon);
        HBox hBox = new HBox(rgbTextfield, colorPickerButton);
        hBox.setStyle("-fx-spacing: 10; -fx-alignment: center");
        vBox.getChildren().add(hBox);

        // color square
        colorCanvas = new Canvas(180, 40);
        colorCanvas.setStyle("-fx-border-color: black; -fx-border-width: 4;");
        updateColorBox(javafx.scene.paint.Color.BLACK);
        vBox.getChildren().add(colorCanvas);

        TitledPane titledPane = getTitledPane("NPC highlight color", vBox);
        titledPane.setPrefHeight(140);
        return titledPane;
    }

    private Node buildSeedCheckboxes(ScriptCore core) {
        GridPane gridPane = new GridPane();
        int x = 0;
        int y = 0;
        for (Seed seed : Seed.values()) {
            ItemDefinition itemDefinition = core.getItemManager().getItemDefinition(seed.getId());
            ImageView itemImageView = getUIImage(core, seed.getId());
            if (itemDefinition == null || itemImageView == null) {
                continue;
            }
            boolean herbSeedCheckBox = seed.getId() == ItemID.GUAM_SEED;
            String name = herbSeedCheckBox ? "Herb seeds" : itemDefinition.name;
            CheckBox checkBox = new CheckBox(name);
            checkBox.setPrefWidth(170);
            checkBox.setStyle("-fx-text-fill: white");
            checkBox.setGraphic(itemImageView);
            gridPane.add(checkBox, x, y);
            seedCheckboxes.put(checkBox, seed);
            // add change listener
            checkBox.selectedProperty().addListener((observableValue, aBoolean, newValue) -> {
                updateTotalSlots();
            });

            x++;
            if (x >= 4) {
                x = 0;
                y++;
            }
        }
        return getTitledPane("Select the seeds you don't want to drop", gridPane);
    }

    private void updateTotalSlots() {
        // update checkbox slot amounts
        totalSlots = 0;
        seedCheckboxes.entrySet().forEach(entry -> {
            CheckBox checkBox = entry.getKey();
            boolean herbSeedCheckBox = checkBox.getText().equals("Herb seeds");
            int amount = herbSeedCheckBox ? 14 : 1;
            if (checkBox.isSelected()) {
                totalSlots += amount;
            }
        });

        if (dodgyNecklaceCheckbox.isSelected()) {
            Integer dodgyNecklaces = dodgyNecklaceAmountSpinner.getValue();
            if (dodgyNecklaces != null)
                totalSlots += dodgyNecklaces;
        }
        if (foodCheckbox.isSelected()) {
            Integer foodAmount = foodAmountSpinner.getValue();
            if (foodAmount != null)
                totalSlots += foodAmount;
        }
        totalSlotsLabel.setText("Total Slots: " + totalSlots);
    }

    public TitledPane getTitledPane(String title, Node node) {
        TitledPane titledPane = new TitledPane(title, node);
        titledPane.setCollapsible(false);
        titledPane.getStyleClass().add("script-manager-titled-pane");
        return titledPane;
    }

    public Pair<Integer, Integer> getFoodOptions() {
        if (!foodCheckbox.isSelected()) {
            return null;
        }
        int itemID = Integer.parseInt(foodIdTextField.getText());
        Integer spinnerAmount = foodAmountSpinner.getValue();
        int amount = spinnerAmount == null ? 0 : spinnerAmount;
        return new Pair<>(itemID, amount);
    }

    public int getAmountOfNecklacesToWithdraw() {
        if (!dodgyNecklaceCheckbox.isSelected()) {
            return -1;
        }
        Integer value = dodgyNecklaceAmountSpinner.getValue();
        return value == null ? 0 : value;
    }

    public List<Seed> getSeedsToKeep() {
        List<Seed> seedsToKeep = new ArrayList<>();
        seedCheckboxes.entrySet().forEach(entry -> {
            CheckBox checkBox = entry.getKey();
            Seed seed = entry.getValue();
            if (checkBox.isSelected()) {
                seedsToKeep.add(seed);
            }
        });
        return seedsToKeep;
    }


    public int getHighlightColor() {
        return highlightColor;
    }
}