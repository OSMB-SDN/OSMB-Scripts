package com.osmb.script.combat.nightmarezone.overlay;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.overlay.OverlayBoundary;
import com.osmb.api.ui.overlay.OverlayPosition;
import com.osmb.api.ui.overlay.OverlayValueFinder;
import com.osmb.api.visual.image.Image;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.List;

public class BlastFurnaceOverlay extends OverlayBoundary {

    // The border pixel of the overlay (this would be the pixel where your red outline is)
    private static final int BORDER_PIXEL = -1;
    // the bounds of the coffer value text - RELATIVE to the overlay bounds (not the screen)
    private static final Rectangle COFFER_VALUE_BOUNDS = new Rectangle(0, 0, 1, 1);
    // the key for the coffer value
    private static final String COFFER = "Coffer";

    public BlastFurnaceOverlay(ScriptCore core) {
        super(core);
    }

    @Override
    public int getWidth() {
        return 146;
    }

    @Override
    public int getHeight() {
        return 5;
    }

    /**
     * This method is called when checking if visible. Overlays are stackable and stack in different directions based on {@link OverlayPosition}
     * For the TopLeft position, overlays stack vertically, so the overlay manager will loop through the y axis starting from the base position you provide - calling this method.
     * You need to provide a condition that guarantees that the overlay is visible in this position.
     * The easiest way to do this if the border isn't transparent is to just confirm it is visible in that position by checking for that.
     */
    @Override
    protected boolean checkVisibility(Rectangle bounds) {
        if (bounds == null) {
            return false;
        }
        Image screenImage = core.getScreen().getImage();

        boolean foundTop = false;
        boolean foundBottom = false;
        for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
            int topPixel = screenImage.getRGB(x, bounds.y);
            int bottomPixel = screenImage.getRGB(x, bounds.y + bounds.height - 1);

            if (!foundTop && topPixel == BORDER_PIXEL) {
                foundTop = true;
            }
            if (!foundBottom && bottomPixel == BORDER_PIXEL) {
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
        OverlayValueFinder<Integer> pointsValueFinder = new OverlayValueFinder<>(COFFER, overlayBounds -> getCofferValue(overlayBounds));
        return List.of(pointsValueFinder);
    }

    private Integer getCofferValue(Rectangle overlayBounds) {
        Rectangle cofferValueBounds = overlayBounds.getSubRectangle(COFFER_VALUE_BOUNDS);
        String text = core.getOCR().getText(Font.STANDARD_FONT, cofferValueBounds, -1).replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }


    // not needed will probably remove these methods in the future
    @Override
    public void onOverlayFound(Rectangle overlayBounds) {

    }

    // not needed will probably remove these methods in the future
    @Override
    public void onOverlayNotFound() {

    }
}
