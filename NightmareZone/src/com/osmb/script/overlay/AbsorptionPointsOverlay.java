package com.osmb.script.overlay;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.overlay.OverlayBoundary;
import com.osmb.api.ui.overlay.OverlayPosition;
import com.osmb.api.ui.overlay.OverlayValueFinder;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.image.Image;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.List;

public class AbsorptionPointsOverlay extends OverlayBoundary {

    public static final SearchablePixel BLACK_PIXEL = new SearchablePixel(-16777215, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    public static final int ORANGE_TEXT = -26593;
    public static final int BORDER_PIXEL = -9157096;
    public static final String POINTS = "points";

    //private final SearchableImage bigAbsorption;

    public AbsorptionPointsOverlay(ScriptCore core) {
        super(core);
    }

    @Override
    public int getWidth() {
        return 70;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    protected boolean checkVisibility(Rectangle bounds) {
        if (bounds == null) {
            return false;
        }
        Image screenImage = core.getScreen().getImage();

        boolean foundTop = false;
        boolean foundBottom = false;
        for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
            if (!foundTop && screenImage.getRGB(x, bounds.y) == BORDER_PIXEL) {
                foundTop = true;
            }
            if (!foundBottom && screenImage.getRGB(x, bounds.y) == BORDER_PIXEL) {
                foundBottom = true;
            }
            if (foundTop && foundBottom) {
                core.getScreen().getDrawableCanvas().fillRect(bounds, Color.GREEN.getRGB(), 0.5);
                core.getScreen().getDrawableCanvas().drawRect(bounds, Color.GREEN.getRGB());
                return true;
            }
        }
        return false;
    }

    @Override
    public OverlayPosition getOverlayPosition() {
        return OverlayPosition.TOP_LEFT;
    }

    @Override
    public Point getOverlayOffset() {
        return new Point(62, 6);
    }

    @Override
    public List<OverlayValueFinder> applyValueFinders() {
        OverlayValueFinder<Integer> pointsValueFinder = new OverlayValueFinder<>(POINTS, overlayBounds -> getAbsorptionPoints(overlayBounds));
        return List.of(pointsValueFinder);
    }

    private Integer getAbsorptionPoints(Rectangle overlayBounds) {
        Integer rgb = getTextColor(overlayBounds);
        if (rgb == null) {
            return null;
        }
        String text = core.getOCR().getText(Font.FANCY_BOLD_FONT_645, overlayBounds, rgb).replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }

    /**
     * Gets the color of the absorption points text. The text changes color, to work the color out we use the shadow pixels as they are always x+1, y+1.
     *
     * @param overlayBounds
     * @return
     */
    private Integer getTextColor(Rectangle overlayBounds) {
        //    Rectangle pointsBounds = overlayBounds.getSubRectangle()
        Rectangle textBounds = overlayBounds.getPadding(25,0,13,0);
        core.getScreen().getDrawableCanvas().drawRect(textBounds, Color.YELLOW.getRGB());
        List<Point> pixels = core.getPixelAnalyzer().findPixels(textBounds, BLACK_PIXEL);
        Image screenImage = core.getScreen().getImage();
        Integer rgb;
        for (Point p : pixels) {
            int color = screenImage.getRGB(p.x - 1, p.y - 1);
            if (color != ORANGE_TEXT) {
                rgb = color;
                return rgb;
            }
        }
        return null;
    }

    @Override
    public void onOverlayFound(Rectangle overlayBounds) {

    }

    @Override
    public void onOverlayNotFound() {

    }
}
