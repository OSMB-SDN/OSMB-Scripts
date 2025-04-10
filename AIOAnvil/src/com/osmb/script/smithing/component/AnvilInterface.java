package com.osmb.script.smithing.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroup;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;


public class AnvilInterface extends ComponentCentered implements ItemGroup {

    public static final int ORANGE_UI_TEXT = -26593;
    private static final Rectangle TITLE_BOUNDS = new Rectangle(150, 6, 200, 22);

    public AnvilInterface(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(500, 320, ColorUtils.TRANSPARENT_PIXEL);
        canvas.createBackground(core, BorderPalette.STEEL_BORDER, null);
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        ComponentImage<Integer> image = new ComponentImage<>(canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB), -1, 1);
        return image;
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


    @Override
    public Point getStartPoint() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }
        return new Point(bounds.x + 16, bounds.y + 47);
    }

    @Override
    public int groupWidth() {
        return 6;
    }

    @Override
    public int groupHeight() {
        return 5;
    }

    @Override
    public int xIncrement() {
        return 80;
    }

    @Override
    public int yIncrement() {
        return 55;
    }

    @Override
    public Rectangle getGroupBounds() {
        return getBounds();
    }
}
