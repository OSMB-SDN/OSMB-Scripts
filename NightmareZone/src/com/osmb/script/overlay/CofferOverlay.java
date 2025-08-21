package com.osmb.script.overlay;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemGroup;
import com.osmb.api.item.ItemGroupResult;
import com.osmb.api.item.ItemID;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.overlay.OverlayBoundary;
import com.osmb.api.ui.overlay.OverlayPosition;
import com.osmb.api.ui.overlay.OverlayValueFinder;
import com.osmb.api.utils.UIResult;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.List;
import java.util.Set;

public class CofferOverlay extends OverlayBoundary implements ItemGroup {
    public static final Rectangle TITLE_BOUNDS = new Rectangle(5, 4, 36, 13);

    public CofferOverlay(ScriptCore core) {
        super(core);
    }

    @Override
    public int getWidth() {
        return 46;
    }

    @Override
    public int getHeight() {
        return 57;
    }

    @Override
    protected boolean checkVisibility(Rectangle bounds) {
        Rectangle titleBounds = bounds.getSubRectangle(TITLE_BOUNDS);
        String text = core.getOCR().getText(Font.STANDARD_FONT, titleBounds, AbsorptionPointsOverlay.ORANGE_TEXT);
        return text.equalsIgnoreCase("coffer");
    }

    @Override
    public OverlayPosition getOverlayPosition() {
        return OverlayPosition.TOP_RIGHT;
    }

    @Override
    public Point getOverlayOffset() {
        return new Point(-241, 4);
    }

    @Override
    public List<OverlayValueFinder> applyValueFinders() {
        return List.of();
    }

    @Override
    public void onOverlayFound(Rectangle overlayBounds) {

    }

    @Override
    public void onOverlayNotFound() {

    }

    public UIResult<Integer> getCofferValue() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return UIResult.notVisible();
        }
        ItemGroupResult cofferSnapshot = core.getItemManager().scanItemGroup(this, Set.of(ItemID.COINS_995));
        if (cofferSnapshot == null) {
            return UIResult.notVisible();
        }
        ItemSearchResult coins = cofferSnapshot.getItem(ItemID.COINS_995);
        if (coins == null) {
            return UIResult.notVisible();
        }
        return UIResult.of(coins.getStackAmount());
    }

    @Override
    public Point getStartPoint() {
        Rectangle bounds = getBounds();
        if (bounds == null) {
            return null;
        }
        return new Point(bounds.x + 4, bounds.y + 28);
    }

    @Override
    public int groupWidth() {
        return 1;
    }

    @Override
    public int groupHeight() {
        return 1;
    }

    @Override
    public int xIncrement() {
        return 0;
    }

    @Override
    public int yIncrement() {
        return 0;
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
