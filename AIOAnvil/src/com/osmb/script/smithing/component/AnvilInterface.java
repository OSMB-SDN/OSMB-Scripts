package com.osmb.script.smithing.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.SearchableItem;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;


public class AnvilInterface extends ComponentCentered {

    public static final int ORANGE_UI_TEXT = -26593;
    private static final Rectangle TITLE_BOUNDS = new Rectangle(150, 6, 200, 22);
    private Rectangle itemPosition;

    public AnvilInterface(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(500, 320, ColorUtils.TRANSPARENT_PIXEL);
        canvas.createBackground(core, BorderPalette.STEEL_BORDER, null);
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        return new ComponentImage<>(canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB), -1, 1);
    }

    @Override
    public boolean isVisible() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return false;
        }
        Rectangle titleBounds = bounds.getSubRectangle(TITLE_BOUNDS);
        String title = core.getOCR().getText(Font.STANDARD_FONT_BOLD, titleBounds, ORANGE_UI_TEXT);
        return title.equalsIgnoreCase("what would you like to make?");
    }

    public boolean selectItem(int itemID) {
       // if (itemPosition == null) {
            ImageSearchResult item = findItem(itemID);
            if (item != null) {
                itemPosition = item.getBounds().getPadding(0, 0, -15, -15);
            } else {
                core.log(AnvilInterface.class, "Can't find item" + itemID + " inside interface");
                return false;
            }
   //     }

   //     core.getScreen().queueCanvasDrawable("r", canvas -> canvas.drawRect(itemPosition, Color.RED.getRGB()));
        if (core.getFinger().tap(itemPosition)) {
            return core.submitTask(() -> getBounds() == null, 3500);
        }
        return false;
    }

    public ImageSearchResult findItem(int itemID) {
        core.log(AnvilInterface.class,"Finding item");
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }
        core.getScreen().getDrawableCanvas().drawRect(bounds, Color.RED.getRGB());
        SearchableItem[] searchableItems = core.getItemManager().getItem(itemID, false);
        int startX = bounds.x;
        int startY = bounds.y;
        int endX = bounds.x + bounds.width;
        int endY = bounds.y + bounds.height;
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                for (SearchableItem searchableItem : searchableItems) {
                    if (x + searchableItem.width > endX || y + searchableItem.height > endY) {
                        continue;
                    }
                    ImageSearchResult imageSearchResult = core.getItemManager().isItemAt(searchableItem, x, y);
                    if(imageSearchResult != null) {
                        return imageSearchResult;
                    }
                }
            }
        }
        return null;
    }

}