package com.osmb.script.combat.nightmarezone.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.script.combat.nightmarezone.NightmareZone;

import static com.osmb.api.visual.color.ColorUtils.ORANGE_UI_TEXT;


public class PotionInterface extends ComponentCentered {

    public static final Rectangle ACCEPT_BUTTON_BOUNDS = new Rectangle(295, 180, 95, 30);
    public static final int GREEN_ACCEPT_TEXT_COLOR = -16711936;
    public PotionInterface(ScriptCore core) {
        super(core);
    }

    @Override
    public boolean isVisible() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return false;
        }
        Rectangle titleBounds = new Rectangle(bounds.x, bounds.y, bounds.width, 30);
        String title = core.getOCR().getText(Font.STANDARD_FONT_BOLD, titleBounds, ORANGE_UI_TEXT);
        return title.equalsIgnoreCase("Nightmare Zone");
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(400, 325, ColorUtils.TRANSPARENT_PIXEL);
        canvas.createBackground(core, BorderPalette.STEEL_BORDER, null);
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        ComponentImage<Integer> image = new ComponentImage<>(canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB), -1, 1);
        return image;
    }

    public boolean accept() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return false;
        }
        Rectangle acceptButton = bounds.getSubRectangle(ACCEPT_BUTTON_BOUNDS);
        String buttonText = core.getOCR().getText(Font.STANDARD_FONT, acceptButton, GREEN_ACCEPT_TEXT_COLOR);
        if(!buttonText.equalsIgnoreCase("accept")) {
            boolean b = core.submitTask(() -> {
                String buttonText_ = core.getOCR().getText(Font.STANDARD_FONT, acceptButton, GREEN_ACCEPT_TEXT_COLOR);
                return buttonText_.equalsIgnoreCase("accept");
            },3000);
            if(!b) {
                core.log(NightmareZone.class, "Unable to accept dream configuration...");
                core.stop();
                return false;
            }
        }
        core.getFinger().tap(acceptButton.getPadding(6));
        return core.submitHumanTask(() -> !isVisible(), 5000);
    }
}
