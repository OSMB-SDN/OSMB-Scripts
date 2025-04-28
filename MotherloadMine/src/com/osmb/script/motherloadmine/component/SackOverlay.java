package com.osmb.script.motherloadmine.component;

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

public class SackOverlay extends OverlayBoundary {

    public static final int BORDER_PIXEL = -13094877;
    public static final String SPACE_LEFT = "space";
    public static final String DEPOSITED = "deposited";
    public static final Rectangle SPACE_TEXT_BOUNDS = new Rectangle(20, 40, 62, 11);
    public static final Rectangle DEPOSITED_AMOUNT_TEXT = new Rectangle(1, 4, 101, 32);
    public static final SearchablePixel BLACK_PIXEL = new SearchablePixel(-16777215, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);

    public SackOverlay(ScriptCore core) {
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
        OverlayValueFinder<Integer> spaceValueFinder = new OverlayValueFinder<>(SPACE_LEFT, overlayBounds -> getSpaceLeft(overlayBounds));
        OverlayValueFinder<Integer> depositedValueFinder = new OverlayValueFinder<>(DEPOSITED, overlayBounds -> getDeposited(overlayBounds));
        return List.of(spaceValueFinder,depositedValueFinder);
    }

    private Integer getSpaceLeft(Rectangle overlayBounds) {
        Rectangle textBounds = overlayBounds.getSubRectangle(SPACE_TEXT_BOUNDS);
        String text = core.getOCR().getText(Font.SMALL_FONT, textBounds, -1).replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private Integer getDeposited(Rectangle overlayBounds) {
        Integer rgb = getTextColor(overlayBounds);
        String text = core.getOCR().getText(Font.FANCY_BOLD_FONT_645, overlayBounds, rgb).replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private Integer getTextColor(Rectangle overlayBounds) {
        //    Rectangle pointsBounds = overlayBounds.getSubRectangle()
        Rectangle textBounds = overlayBounds.getSubRectangle(DEPOSITED_AMOUNT_TEXT);
        core.getScreen().getDrawableCanvas().drawRect(textBounds, Color.YELLOW.getRGB());
        Point shadowPixel = core.getPixelAnalyzer().findPixel(textBounds, BLACK_PIXEL);
        if (shadowPixel == null) {
            return null;
        }
        Image screenImage = core.getScreen().getImage();
        return screenImage.getRGB(shadowPixel.x - 1, shadowPixel.y - 1);
    }

    @Override
    public void onOverlayFound(Rectangle overlayBounds) {

    }

    @Override
    public void onOverlayNotFound() {

    }
}
