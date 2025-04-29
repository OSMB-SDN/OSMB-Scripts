package com.osmb.script.smithing.javafx;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ZoomType;
import com.osmb.script.smithing.AIOAnvil;
import com.osmb.script.smithing.data.Bar;
import com.osmb.script.smithing.data.Product;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.prefs.Preferences;

public class ScriptOptions extends VBox {

    private static final String PREF_SELECTED_BAR = "aioanvil_selected_bar";
    private static final String PREF_SELECTED_PRODUCT = "aioanvil_selected_product";

    public static final Color MENU_COLOR_BACKGROUND = new Color(58, 65, 66);
    private final Preferences prefs = Preferences.userNodeForPackage(ScriptOptions.class);

    private final ComboBox<Integer> productComboBox;
    private final ComboBox<Integer> barComboBox;

    public ScriptOptions(AIOAnvil script) {
        setAlignment(Pos.TOP_CENTER);
        setStyle("-fx-background-color: #636E72; -fx-padding: 10; -fx-spacing: 10");

        Label label = new Label("Select type of bar to use");
        barComboBox = createItemCombobox(script, Bar.getIDValues());
        productComboBox = createItemCombobox(script, new Integer[0]);

        // Load previous settings if they exist
        loadPreferences(script);

        barComboBox.setOnAction(actionEvent -> {
            Integer selectedBarID = barComboBox.getSelectionModel().getSelectedItem();
            if (selectedBarID == null) return;
            // get enum index
            int barIndex;
            for (barIndex = 0; barIndex < Bar.values().length; barIndex++) {
                Bar bar = Bar.values()[barIndex];
                if (bar.getItemID() == selectedBarID) {
                    break;
                }
            }
            // get items to populate second combobox, based on index of bar
            Integer[] itemsToPopulate = new Integer[Product.values().length];
            for (int index = 0; index < Product.values().length; index++) {
                Product product = Product.values()[index];
                itemsToPopulate[index] = product.getIds()[barIndex];
            }

            //clear the current child nodes
            Platform.runLater(() -> {
                productComboBox.getItems().clear();
                productComboBox.getItems().addAll(itemsToPopulate);
            });
        });
        Button button = new Button("Confirm");
        button.setOnAction(actionEvent -> {
            if (barComboBox.getValue() == null || productComboBox.getValue() == null) {
                return;
            }
            //stage.close();
            savePreferences();
            ((Stage) button.getScene().getWindow()).close();
        });
        getChildren().addAll(label, barComboBox, productComboBox, button);
    }

    private void loadPreferences(ScriptCore core) {
        int savedBar = prefs.getInt(PREF_SELECTED_BAR, -1);
        int savedProduct = prefs.getInt(PREF_SELECTED_PRODUCT, -1);

        if (savedBar != -1) {
            barComboBox.getSelectionModel().select(savedBar);

            // Now simulate bar selection to populate product combobox
            int barIndex = 0;
            for (int i = 0; i < Bar.values().length; i++) {
                if (Bar.values()[i].getItemID() == savedBar) {
                    barIndex = i;
                    break;
                }
            }

            Integer[] products = new Integer[Product.values().length];
            for (int i = 0; i < Product.values().length; i++) {
                products[i] = Product.values()[i].getIds()[barIndex];
            }

            productComboBox.getItems().clear();
            productComboBox.getItems().addAll(products);

            if (savedProduct != -1) {
                productComboBox.getSelectionModel().select(savedProduct);
            }
        }
    }

    private void savePreferences() {
        Integer selectedBar = barComboBox.getSelectionModel().getSelectedItem();
        Integer selectedProduct = productComboBox.getSelectionModel().getSelectedItem();
        if (selectedBar != null) {
            prefs.putInt(PREF_SELECTED_BAR, selectedBar);
        }
        if (selectedProduct != null) {
            prefs.putInt(PREF_SELECTED_PRODUCT, selectedProduct);
        }
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

    public static ComboBox<Integer> createItemCombobox(ScriptCore core, Integer[] values) {
        ComboBox<Integer> itemComboBox = new ComboBox<>();
        itemComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer item) {
                return item != null ? core.getItemManager().getItemName(item) : null; // Use the getName method
            }

            @Override
            public Integer fromString(String string) {
                // Not needed in this context
                return null;
            }
        });
        itemComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Integer itemID, boolean empty) {
                super.updateItem(itemID, empty);
                if (itemID != null && !empty) {
                    String name = core.getItemManager().getItemName(itemID);
                    ImageView itemImage = getUIImage(core, itemID);
                    setGraphic(itemImage);
                    setText(name);
                } else {
                    setText(null);
                    setGraphic(null);
                }
            }
        });
        itemComboBox.getItems().addAll(values);
        return itemComboBox;
    }

    public int getSelectedBar() {
        return barComboBox.getValue();
    }

    public Product getSelectedProduct() {
        int selectedID = productComboBox.getValue();
        for (Product product : Product.values()) {
            for (int id : product.getIds()) {
                if (id == selectedID) {
                    return product;
                }
            }
        }
        return null;
    }

    public int getSelectedProductID() {
        return productComboBox.getValue();
    }
}