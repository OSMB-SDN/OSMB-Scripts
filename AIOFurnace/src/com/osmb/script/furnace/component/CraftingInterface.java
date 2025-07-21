package com.osmb.script.furnace.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.item.SearchableItem;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.utils.ImagePanel;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.Image;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraftingInterface extends ComponentCentered {
    public static final int NW_QUANTITY_SPRITE_ID = 921;
    private Map<ProductionQuantity, Rectangle> productionQuantityButtons;
    private ImageSearchResult lastFoundItem = null;

    public CraftingInterface(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(492, 300, ColorUtils.TRANSPARENT_PIXEL);
        // create steel border background
        canvas.createBackground(core, BorderPalette.STEEL_BORDER, null);
        // set middle to transparent
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        // display the canvas for debuggin
        Image image = canvas.toImage();
        ImagePanel imagePanel = new ImagePanel(image.toBufferedImage());
        imagePanel.showInFrame("background debug");

        return new ComponentImage<>(canvas.toSearchableImage(new SingleThresholdComparator(10), ColorModel.RGB), -1, 1);
    }

    @Override
    public Rectangle getBounds() {
        Rectangle bounds = super.getBounds();
        if(bounds == null) {
            return null;
        }
        this.productionQuantityButtons = findQuantityButtons(bounds);
        return bounds;
    }

    private Map<ProductionQuantity, Rectangle> findQuantityButtons(Rectangle bounds) {
        List<Rectangle> quantityButtons = core.getImageAnalyzer().findContainers(bounds, 929, 930, 931, 932);
        List<Rectangle> selectedQuantityButtons = core.getImageAnalyzer().findContainers(bounds, NW_QUANTITY_SPRITE_ID, 922, 923, 924);
        List<Rectangle> allButtons = new ArrayList<>(quantityButtons);
        allButtons.addAll(selectedQuantityButtons);

        Map<ProductionQuantity, Rectangle> productionQuantityButtons = new HashMap<>();
        for (Rectangle button : allButtons) {
            String buttonText = core.getOCR().getText(Font.SMALL_FONT, button, -1, -26593);
            if (buttonText.isEmpty()) {
                core.log(CraftingInterface.class, "Failed to read button text for: " + button);
            }
            ProductionQuantity quantity = ProductionQuantity.fromText(buttonText);
            if (quantity != null) {
                Rectangle interfaceBounds = new Rectangle(button.x - bounds.x, button.y - bounds.y, button.width, button.height);
                productionQuantityButtons.put(quantity, interfaceBounds);
                core.log(CraftingInterface.class, "Registered button for quantity: " + quantity.getText() + " at " + button);
            } else {
                core.log(CraftingInterface.class, "Unknown production quantity button text: " + buttonText);
            }
        }
        return productionQuantityButtons;
    }

    public ProductionQuantity getSelectedProductionQuantity() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }
        for (Map.Entry<ProductionQuantity, Rectangle> entry : productionQuantityButtons.entrySet()) {
            Rectangle buttonScreenBounds = bounds.getSubRectangle(entry.getValue());
            SearchableImage buttonCornerImage = new SearchableImage(NW_QUANTITY_SPRITE_ID, core, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
            if (core.getImageAnalyzer().isSubImageAt(buttonScreenBounds.x, buttonScreenBounds.y, buttonCornerImage) != null) {
                return entry.getKey();
            }
        }
        return null; // or throw an exception if preferred
    }

    public boolean selectProductionQuantity(ProductionQuantity quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Production quantity cannot be null.");
        }
        ProductionQuantity selectedQuantity = getSelectedProductionQuantity();
        if (selectedQuantity == quantity) {
            core.log(CraftingInterface.class, "Production quantity already selected: " + quantity.getText());
            return true; // already selected
        }
        Rectangle buttonBounds = productionQuantityButtons.get(quantity);
        if (buttonBounds == null) {
            core.log(CraftingInterface.class, "No button registered for quantity: " + quantity.getText());
            return false;
        }

        Rectangle bounds = getBounds();
        if (bounds == null) {
            return false;
        }
        Rectangle screenBounds = bounds.getSubRectangle(buttonBounds);
        Point point = RandomUtils.generateRandomPoint(screenBounds, 6.5);
        core.getFinger().tap(point);
        return core.submitTask(() -> getSelectedProductionQuantity() == quantity, 3000);
    }

    public boolean selectItem(int itemID) {
        ImageSearchResult item = findItem(itemID);

        if (item == null) {
            core.log(CraftingInterface.class, "Can't find item" + itemID + " inside interface");
            return false;
        }
        Rectangle itemBounds = item.getBounds();
        Point point = RandomUtils.generateRandomPoint(itemBounds, 6.5);

        if (core.getFinger().tap(point)) {
            return core.submitTask(() -> getBounds() == null, 3500);
        }
        return false;
    }

    private ImageSearchResult findItem(int itemID) {
        core.log(CraftingInterface.class, "Searching interface for item: " + itemID);
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }
        core.getScreen().getDrawableCanvas().drawRect(bounds, Color.RED.getRGB());
        SearchableItem[] searchableItems = core.getItemManager().getItem(itemID, false);

        // try searching at the last found location first
        if (lastFoundItem != null && bounds.contains(lastFoundItem.getBounds())) {
            Rectangle prevBounds = lastFoundItem.getBounds();
            for (SearchableItem searchableItem : searchableItems) {
                if (prevBounds.width >= searchableItem.width && prevBounds.height >= searchableItem.height) {
                    ImageSearchResult imageSearchResult = core.getItemManager().isItemAt(searchableItem, prevBounds.x, prevBounds.y);
                    if (imageSearchResult != null) {
                        lastFoundItem = imageSearchResult;
                        return imageSearchResult;
                    }
                }
            }
        }
        List<ItemSearchResult> imageSearchResults = core.getItemManager().findLocations(false, bounds, searchableItems);
        if(imageSearchResults.isEmpty()) {
            core.log(CraftingInterface.class, "No items found in the interface for item ID: " + itemID);
            return null;
        }
        // just get first as there only should be one item
        lastFoundItem = imageSearchResults.get(0);
        return lastFoundItem;
    }

    public enum ProductionQuantity {
        ONE("1"),
        FIVE("5"),
        TEN("10"),
        X("X"),
        ALL("All");

        private final String text;

        ProductionQuantity(String text) {
            this.text = text;
        }

        public static ProductionQuantity fromText(String text) {
            for (ProductionQuantity quantity : values()) {
                if (quantity.getText().equals(text)) {
                    return quantity;
                }
            }
            return null; // or throw an exception if preferred
        }

        public String getText() {
            return text;
        }
    }
}
