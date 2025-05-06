package com.osmb.script.chartercrafting.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroup;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.SpriteID;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.utils.UIResult;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.List;

public class ShopInterface extends ComponentCentered implements ItemGroup {
    public static final Rectangle TITLE_BOUNDS = new Rectangle(6, 6, 476, 23);
    private static final Rectangle CLOSE_BUTTON_BOUNDS = new Rectangle(460, 7, 21, 21);


    public ShopInterface(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(488, 300, ColorUtils.TRANSPARENT_PIXEL);
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
        String title = core.getOCR().getText(Font.STANDARD_FONT_BOLD, titleBounds, ColorUtils.ORANGE_UI_TEXT);
        return title.equalsIgnoreCase("Trader Stan's Trading Post");
    }

    public void close() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return;
        }
        Rectangle rectangle = bounds.getSubRectangle(CLOSE_BUTTON_BOUNDS);
        core.getFinger().tap(rectangle);
        core.submitHumanTask(() -> !isVisible(), 3000);
    }


    public UIResult<Integer> getSelectedAmount() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return UIResult.notVisible();
        }
        List<Rectangle> bottomTextButtonsRed = core.getImageAnalyzer().findContainers(bounds, SpriteID.BANK_TEXT_BUTTON_RED_NW, SpriteID.BANK_TEXT_BUTTON_RED_NE, SpriteID.BANK_TEXT_BUTTON_RED_SW, SpriteID.BANK_TEXT_BUTTON_RED_SE);
        if (bottomTextButtonsRed.isEmpty()) {
            return UIResult.notVisible();
        }
        String buttonText = core.getOCR().getText(Font.STANDARD_FONT, bottomTextButtonsRed.get(0), ColorUtils.ORANGE_UI_TEXT).replaceAll("[^0-9]", "");
        if (buttonText.isEmpty()) {
            return UIResult.of(null);
        }

        return UIResult.of(Integer.parseInt(buttonText));
    }

    public boolean setSelectedAmount(int amount) {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return false;
        }
        UIResult<Integer> selectedAmount = getSelectedAmount();
        if (selectedAmount.isNotVisible()) {
            return false;
        }
        if (selectedAmount.get() != null && selectedAmount.get() == amount) {
            // if already selected
            return true;
        }
        List<Rectangle> bottomTextButtons = core.getImageAnalyzer().findContainers(bounds, SpriteID.BANK_TEXT_BUTTON_NW, SpriteID.BANK_TEXT_BUTTON_NE, SpriteID.BANK_TEXT_BUTTON_SW, SpriteID.BANK_TEXT_BUTTON_SE);
        if (bottomTextButtons.isEmpty()) {
            return false;
        }
        for (Rectangle button : bottomTextButtons) {
            String buttonText = core.getOCR().getText(Font.STANDARD_FONT, button, ColorUtils.ORANGE_UI_TEXT).replaceAll("[^0-9]", "");
            if (buttonText.isEmpty()) {
                continue;
            }
            int btnAmount = Integer.parseInt(buttonText);
            if (btnAmount == amount) {
                core.getFinger().tap(button.getPadding(4));
                return core.submitTask(() -> {
                    UIResult<Integer> selectedAmount_ = getSelectedAmount();
                    if (selectedAmount_.isNotVisible()) {
                        return false;
                    }
                    if (selectedAmount_.get() == null) {
                        return false;
                    }
                    return selectedAmount_.get() == amount;
                }, core.random(1600, 3000));
            }
        }
        return false;
    }

    @Override
    public Point getStartPoint() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }

        return new Point(bounds.x + 61, bounds.y + 49);
    }

    @Override
    public int groupWidth() {
        return 8;
    }

    @Override
    public int groupHeight() {
        return 4;
    }

    @Override
    public int xIncrement() {
        return 47;
    }

    @Override
    public int yIncrement() {
        return 47;
    }

    @Override
    public Rectangle getGroupBounds() {
        return getBounds();
    }

    @Override
    public ScriptCore getCore() {
        return core;
    }
}
