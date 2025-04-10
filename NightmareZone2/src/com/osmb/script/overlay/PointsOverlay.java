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
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.List;

public class PointsOverlay extends OverlayBoundary {

    public static final SearchablePixel BLACK_PIXEL = new SearchablePixel(-16777215, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    public static final int ORANGE_TEXT = -26593;
    public static final String POINTS = "points";
    public static final Rectangle ABSORPTION_TEXT = new Rectangle(8, 4, 53, 11);
    public PointsOverlay(ScriptCore core) {
        super(core);
    }

    //private final SearchableImage bigAbsorption;

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
        if(bounds == null) {
            return false;
        }
        Rectangle textBounds = bounds.getSubRectangle(ABSORPTION_TEXT);
        String text = core.getOCR().getText(Font.SMALL_FONT, textBounds, ORANGE_TEXT);
        return text.equalsIgnoreCase("absorption");
    }

    @Override
    public OverlayPosition getOverlayPosition() {
        return OverlayPosition.TOP_LEFT;
    }

    @Override
    public Point getOverlayOffset() {
        return new Point(62, 50);
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
     * @param overlayBounds
     * @return
     */
    private Integer getTextColor(Rectangle overlayBounds) {
        List<Point> pixels = core.getPixelAnalyzer().findPixels(overlayBounds.getPadding(4), BLACK_PIXEL);
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
