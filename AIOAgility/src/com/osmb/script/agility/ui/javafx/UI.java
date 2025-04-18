package com.osmb.script.agility.ui.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemID;
import com.osmb.api.javafx.ItemSearchDialogue;
import com.osmb.api.javafx.JavaFXUtils;
import com.osmb.script.agility.Course;
import com.osmb.script.agility.courses.alkharid.AlKharid;
import com.osmb.script.agility.courses.ardougne.Ardougne;
import com.osmb.script.agility.courses.barbarianoutpost.BarbarianOutpost;
import com.osmb.script.agility.courses.canafis.Canafis;
import com.osmb.script.agility.courses.draynor.Draynor;
import com.osmb.script.agility.courses.falador.Falador;
import com.osmb.script.agility.courses.gnome.GnomeStronghold;
import com.osmb.script.agility.courses.relleka.Relleka;
import com.osmb.script.agility.courses.seers.Seers;
import com.osmb.script.agility.courses.varrock.Varrock;
import com.osmb.script.agility.courses.pollnivneach.Pollnivneach;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class UI {

    private ComboBox<Course> selectCourseComboBox;
    private ImageView foodImageView;
    private TextField eatLow;
    private TextField eatHigh;
    private CheckBox foodCheckbox;

    public int getFoodID() {
        return foodID;
    }

    private int foodID = -1;

    public Scene buildScene(ScriptCore core) {

        VBox vBox = new VBox();
        vBox.setSpacing(20);
        vBox.setStyle("-fx-background-color: #636E72; -fx-padding: 10");
        HBox selectCourseBox = new HBox();
        selectCourseBox.setSpacing(10);
        selectCourseBox.setAlignment(Pos.CENTER_LEFT);

        Label selectCourseLabel = new Label("Select Course");
        selectCourseComboBox = new ComboBox<>();
        selectCourseComboBox.getItems().addAll(new GnomeStronghold(), new Draynor(), new AlKharid(), new Varrock(), new BarbarianOutpost(), new Canafis(), new Falador(), new Seers(), new Pollnivneach(), new Relleka(), new Ardougne());

        selectCourseComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Course course) {
                return course != null ? course.name() : "";
            }

            @Override
            public Course fromString(String string) {
                return selectCourseComboBox.getItems().stream().filter(course -> course.name().equals(string)).findFirst().orElse(null);
            }
        });

        selectCourseBox.getChildren().addAll(selectCourseLabel, selectCourseComboBox);

        vBox.getChildren().add(selectCourseBox);


        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setSpacing(10);
        foodImageView = JavaFXUtils.getItemImageView(core, ItemID.BANK_FILLER);
        Button button = new Button("\uD83D\uDD0E");
        button.setOnAction(actionEvent -> {
            int itemID = ItemSearchDialogue.show(core, (Stage) button.getScene().getWindow());
            if (itemID == -1) {
                itemID = ItemID.BANK_FILLER;
            }
            ImageView imageView = JavaFXUtils.getItemImageView(core, itemID);
            if (imageView != null) {
                foodID = itemID;
                foodImageView.setImage(imageView.getImage());
            }
        });
        hBox.getChildren().addAll(foodImageView,button);

        HBox hBox2 = new HBox();
        hBox2.setAlignment(Pos.CENTER_LEFT);
        hBox2.setSpacing(10);
        Label eatBetweenLabel = new Label("Eat between (%)");
        eatLow = new TextField();
        makeNumericOnly(eatLow);
        eatLow.setText("40");
        eatLow.setPrefWidth(40);
        Label eatBetweenLabel2 = new Label("and");
        eatHigh = new TextField();
        makeNumericOnly(eatHigh);
        eatHigh.setText("80");
        eatHigh.setPrefWidth(40);
        hBox2.getChildren().addAll(eatBetweenLabel, eatLow, eatBetweenLabel2, eatHigh);

        VBox foodBox = new VBox(hBox, hBox2);
        foodBox.setSpacing(10);
        foodCheckbox = new CheckBox("Use food");
        foodCheckbox.setStyle("-fx-text-fill: white");
        foodCheckbox.selectedProperty().addListener((observableValue, aBoolean, newValue) -> foodBox.setDisable(!newValue));
        VBox foodSelectionBox = new VBox(foodCheckbox, foodBox);
        foodSelectionBox.setSpacing(10);
        vBox.getChildren().addAll(foodSelectionBox);

        foodBox.setDisable(true);

        Button confirmButton = new Button("Confirm");


        vBox.getChildren().add(confirmButton);

        Scene scene = new Scene(vBox);
        confirmButton.setOnAction(actionEvent -> {
            if (selectedCourse() != null) ((Stage) confirmButton.getScene().getWindow()).close();
        });
        scene.getStylesheets().add("style.css");

        return scene;
    }

    public Course selectedCourse() {
        Course course = selectCourseComboBox.getSelectionModel().getSelectedItem();
        if (course == null) {
            return null;
        }
        return course;
    }

    public int getEatLow() {
        String eatLowString = eatLow.getText();
        if (eatLowString == null || eatLowString.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(eatLowString);
    }

    public int getEatHigh() {
        String eatHighString = eatHigh.getText();
        if (eatHighString == null || eatHighString.isEmpty()) {
            return -1;
        }
        return Integer.parseInt(eatHighString);
    }

    private void makeNumericOnly(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
}
