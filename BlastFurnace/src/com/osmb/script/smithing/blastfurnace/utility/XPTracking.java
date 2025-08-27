package com.osmb.script.smithing.blastfurnace.utility;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.trackers.experiencetracker.XPTracker;
import com.osmb.api.ui.component.ComponentSearchResult;
import com.osmb.api.ui.component.minimap.xpcounter.XPDropsComponent;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.image.SearchableImage;

import java.awt.*;

public class XPTracking {

    private final ScriptCore core;
    private final SearchableImage smithingSprite;
    private final XPDropsComponent xpDropsComponent;
    private XPTracker xpTracker;

    public XPTracking(ScriptCore core) {
        this.core = core;
        this.xpDropsComponent = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);
        SearchableImage smithingSprite_ = new SearchableImage(210, core, new SingleThresholdComparator(15), ColorModel.RGB);
        smithingSprite = smithingSprite_.subImage(smithingSprite_.width / 2, 0, smithingSprite_.width / 2, smithingSprite_.height);
    }

    public XPTracker getXpTracker() {
        return xpTracker;
    }

    public void checkXP() {
        Integer currentXP = getXpCounter();
        if (currentXP != null) {
            if (xpTracker == null) {
                xpTracker = new XPTracker(core, currentXP);
            } else {
                double xp = xpTracker.getXp();
                double gainedXP = currentXP - xp;
                if (gainedXP > 0) {
                    xpTracker.incrementXp(gainedXP);
                }
            }
        }
    }

    private Integer getXpCounter() {
        Rectangle bounds = getXPDropsBounds();
        if (bounds == null) {
            core.log(XPTracking.class, "Failed to get XP drops component bounds");
            return null;
        }
        boolean isSmithing = core.getImageAnalyzer().findLocation(bounds, smithingSprite) != null;
        if (!isSmithing) {
            return null;
        }
        core.getScreen().getDrawableCanvas().drawRect(bounds, Color.RED.getRGB(), 1);
        String xpText = core.getOCR().getText(com.osmb.api.visual.ocr.fonts.Font.SMALL_FONT, bounds, -1).replaceAll("[^0-9]", "");
        if (xpText.isEmpty()) {
            return null;
        }
        return Integer.parseInt(xpText);
    }

    public boolean checkXPCounterActive() {
        Rectangle bounds = xpDropsComponent.getBounds();
        if (bounds == null) {
            core.log(XPTracking.class, "Failed to get XP drops component bounds");
            return true;
        }
        ComponentSearchResult<Integer> result = xpDropsComponent.getResult();
        if (result.getComponentImage().getGameFrameStatusType() != 1) {
            core.log(XPTracking.class, "XP drops component is not open, opening it");
            core.getFinger().tap(bounds);
            boolean succeed = core.pollFramesHuman(() -> {
                ComponentSearchResult<Integer> result_ = xpDropsComponent.getResult();
                return result_ != null && result_.getComponentImage().getGameFrameStatusType() == 1;
            }, RandomUtils.uniformRandom(1500, 3000));
            bounds = xpDropsComponent.getBounds();
            return succeed && bounds != null;
        } else {
            core.log(XPTracking.class, "XP drops component is open");
        }
        return true;
    }

    private Rectangle getXPDropsBounds() {
        XPDropsComponent xpDropsComponent = (XPDropsComponent) core.getWidgetManager().getComponent(XPDropsComponent.class);
        Rectangle bounds = xpDropsComponent.getBounds();
        if (bounds == null) {
            core.log(XPTracking.class, "Failed to get XP drops component bounds");
            return null;
        }
        ComponentSearchResult<Integer> result = xpDropsComponent.getResult();
        if (result.getComponentImage().getGameFrameStatusType() != 1) {
            return null;
        }
        return new Rectangle(bounds.x - 140, bounds.y - 1, 119, 38);
    }
}
