package com.osmb.script.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroup;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.utils.UIResult;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.List;

import static com.osmb.api.visual.color.ColorUtils.ORANGE_UI_TEXT;

public class ChestInterface extends ComponentCentered implements ItemGroup {
    public static final int WHITE_PIXEL = -1;
    public static final Rectangle TITLE_BOUNDS = new Rectangle(117, 6, 476, 23);
    private static final Rectangle POINTS_BOUNDS = new Rectangle(276, 35, 206, 21);
    private static final Rectangle CLOSE_BUTTON_BOUNDS = new Rectangle(460, 7, 21, 21);
    private static final int UNSELECTED_BUTTON_ID = 812;
    private static final int SELECTED_BUTTON_ID = 813;
    private final SearchableImage selectedButtonImage;
    private final SearchableImage buttonImage;
    public ChestInterface(ScriptCore core) {
        super(core);
        this.selectedButtonImage = buildButtonImage(true);
        this.buttonImage = buildButtonImage(false);
    }

    private SearchableImage buildButtonImage(boolean selected) {
        Canvas canvas = new Canvas(selected ? SELECTED_BUTTON_ID : UNSELECTED_BUTTON_ID, core);
        canvas.fillRect(10, 3, canvas.canvasWidth - 20, canvas.canvasHeight - 5, ColorUtils.TRANSPARENT_PIXEL);
        return canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(488, 300, ColorUtils.TRANSPARENT_PIXEL);
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
        return title.equalsIgnoreCase("Dom Onion's Reward Shop");
    }

    public UIResult<Integer> getPoints() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return UIResult.notVisible();
        }
        Rectangle pointsBounds = bounds.getSubRectangle(POINTS_BOUNDS);

        String points = core.getOCR().getText(Font.STANDARD_FONT, pointsBounds, ORANGE_UI_TEXT).replaceAll("[^0-9]", "");

        if (points.isEmpty()) {
            return UIResult.of(null);
        }
        return UIResult.of(Integer.parseInt(points));
    }

    public UIResult<Integer> getStoredDoses(int itemID) {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return UIResult.notVisible();
        }
        UIResult<ItemSearchResult> result = core.getItemManager().findItem(this, itemID);
        if (result.isNotFound()) {
            return UIResult.of(null);
        }
        Rectangle resultBounds = result.get().getBounds();
        Rectangle doseBounds = new Rectangle(resultBounds.x, resultBounds.y + 40, resultBounds.width, 12);
        core.getScreen().getDrawableCanvas().drawRect(doseBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.STANDARD_FONT, doseBounds, WHITE_PIXEL).replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return UIResult.of(null);
        }
        return UIResult.of(Integer.parseInt(text));
    }

    public UIResult<Boolean> selectButton(String buttonText) {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return UIResult.notVisible();
        }

        // check if the button is already selected
        UIResult<String> selectedButton = getSelectedTab();
        if (selectedButton.get() != null && selectedButton.get().equals(buttonText)) {
            return UIResult.of(true);
        }

        // get unselected buttons
        List<ImageSearchResult> results = core.getImageAnalyzer().findLocations(bounds, buttonImage);
        if (results.isEmpty()) {
            core.log(ChestInterface.class, "No buttons found...");
            return UIResult.notVisible();
        }

        ImageSearchResult foundButton = null;

        for (ImageSearchResult result : results) {
            String buttonText_ = core.getOCR().getText(Font.SMALL_FONT, result.getBounds(), ORANGE_UI_TEXT);
            if (buttonText_.isEmpty()) {
                continue;
            }
            if (buttonText_.equalsIgnoreCase(buttonText)) {
                // found the button!
                foundButton = result;
                break;
            }
        }

        // if no button found with matching text
        if (foundButton == null) {
            return UIResult.of(false);
        }

        // tap the button
        core.getFinger().tap(foundButton.getBounds().getPadding(4));

        // wait for the tab to become active
        return UIResult.of(core.submitHumanTask(() -> {
            UIResult<String> selectedTab = getSelectedTab();
            return selectedTab.isFound() && selectedTab.get().equalsIgnoreCase(buttonText);
        }, 5000));
    }

    public UIResult<String> getSelectedTab() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return UIResult.notVisible();
        }

        ImageSearchResult result = core.getImageAnalyzer().findLocation(bounds, selectedButtonImage);
        if (result == null) {
            core.log(ChestInterface.class, "No selected button found...");
            return UIResult.notVisible();
        }

        String buttonText = core.getOCR().getText(Font.SMALL_FONT, result.getBounds(), ORANGE_UI_TEXT);
        if (buttonText.isEmpty()) {
            return UIResult.of(null);
        }
        return UIResult.of(buttonText);
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

    @Override
    public Point getStartPoint() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }
        UIResult<String> selectedTab = getSelectedTab();
        if (selectedTab.isNotVisible()) {
            return null;
        }
        String tabName = selectedTab.get();
        if (tabName == null || !tabName.equalsIgnoreCase("benefits")) {
            return null;
        }

        return new Point(bounds.x + 25, bounds.y + 155);
    }

    @Override
    public int groupWidth() {
        return 4;
    }

    @Override
    public int groupHeight() {
        return 1;
    }

    @Override
    public int xIncrement() {
        return 66;
    }

    @Override
    public int yIncrement() {
        return 0;
    }

    @Override
    public Rectangle getGroupBounds() {
        return getBounds();
    }
}
