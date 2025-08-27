package com.osmb.script.runecrafting.gotr.overlay;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.overlay.OverlayBoundary;
import com.osmb.api.ui.overlay.OverlayPosition;
import com.osmb.api.ui.overlay.OverlayValueFinder;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.script.runecrafting.gotr.CatalyticRune;
import com.osmb.script.runecrafting.gotr.ElementalRune;
import com.osmb.script.runecrafting.gotr.Portal;
import com.osmb.script.runecrafting.gotr.Rune;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Overlay extends OverlayBoundary {

    public static final int GUARDIAN_SPRITE_ID = 4371;
    public static final String TIMER = "TIMER";
    public static final String ACTIVE_GAME = "ACTIVE_GAME";
    public static final String CATALYTIC_RUNE = "CATALYTIC";
    public static final String ELEMENTAL_RUNE = "ELEMENTAL";
    public static final String GUARDIAN_POWER = "ENERGY";
    public static final String PORTAL = "PORTAL";
    public static final int WHITE_TEXT = -1;
    public static final String CATALYTIC_POINTS = "CATALYTIC_POINTS";
    public static final String ELEMENTAL_POINTS = "ELEMENTAL_POINTS";
    private static final int PORTAL_SPRITE_ID = 4368;
    private static final Point ELEMENTAL_SPRITE_OFFSET = new Point(23, 31);
    private static final Point CATALYTIC_SPRITE_OFFSET = new Point(104, 31);
    private static final Point GUARDIAN_SPRITE_OFFSET = new Point(185, 1);
    private static final Map<Rune, SearchableImage> RUNE_SPRITES = new HashMap<>();
    private static final int BLACK_TEXT_COLOR = -16777215;
    private static final Rectangle GUARDIANS_POWER_BAR = new Rectangle(0, 0, 160, 17);
    private static final Rectangle TIMER_BOUNDS = new Rectangle(57, 35, 44, 27);
    private static final Rectangle ELEMENTAL_ENERGY_BOUNDS = new Rectangle(0, 85, 79, 19);
    private static final Rectangle CATALYTIC_ENERGY_BOUNDS = new Rectangle(86, 85, 70, 19);
    private static final Rectangle GUARDIAN_COUNT_BOUNDS = new Rectangle(181, 35, 40, 21);
    private static final Rectangle PORTAL_TIMER_BOUNDS = new Rectangle(169, 94, 62, 26);
    private final SearchableImage guardianSprite;

    public Overlay(ScriptCore core) {
        super(core);
        guardianSprite = new SearchableImage(core.getSpriteManager().getSprite(GUARDIAN_SPRITE_ID), ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
        // load sprites for elemental and catalytic runes
        for (Rune rune : CatalyticRune.values()) {
            RUNE_SPRITES.put(rune, new SearchableImage(core.getSpriteManager().getSprite(rune.getInterfaceSpriteId()), ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB));
        }
        for (Rune rune : ElementalRune.values()) {
            RUNE_SPRITES.put(rune, new SearchableImage(core.getSpriteManager().getSprite(rune.getInterfaceSpriteId()), ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB));
        }
    }

    private static Long parseTimeString(String text) {
        text = text.replaceAll("[^0-9:]", "");
        String[] parts = text.split(":");
        if (parts.length == 0 || parts.length > 3) return null;
        long seconds = 0;
        for (int i = 0; i < parts.length; i++) {
            int idx = parts.length - 1 - i;
            String part = parts[idx].trim();
            if (part.isEmpty() || !part.matches("\\d+")) return null;
            long val = Long.parseLong(part);
            seconds += (long) (val * Math.pow(60, i));
        }
        return seconds * 1000;
    }

    @Override
    public int getWidth() {
        return 230;
    }

    @Override
    public int getHeight() {
        return 110;
    }

    @Override
    protected boolean checkVisibility(Rectangle bounds) {
        return core.getImageAnalyzer().isSubImageAt(bounds.x + GUARDIAN_SPRITE_OFFSET.x, bounds.y + GUARDIAN_SPRITE_OFFSET.y, guardianSprite) != null;
    }

    @Override
    public OverlayPosition getOverlayPosition() {
        return OverlayPosition.TOP_LEFT;
    }

    @Override
    public Point getOverlayOffset() {
        return new Point(67, 8);
    }

    @Override
    public List<OverlayValueFinder> applyValueFinders() {
        return List.of(
                new OverlayValueFinder<>(TIMER, this::getTimerSeconds),
                new OverlayValueFinder<>(CATALYTIC_RUNE, this::getActiveCatalyticRune),
                new OverlayValueFinder<>(ELEMENTAL_RUNE, this::getActiveElementalRune),
                new OverlayValueFinder<>(PORTAL, this::getActivePortal),
                new OverlayValueFinder<>(GUARDIAN_POWER, this::getGuardianPower),
                new OverlayValueFinder<>(ELEMENTAL_POINTS, this::getElementalPoints),
                new OverlayValueFinder<>(CATALYTIC_POINTS, this::getCatalyticPoints)
        );
    }

    private PortalInfo getActivePortal(Rectangle overlayBounds) {
        Rectangle portalTextBounds = overlayBounds.getSubRectangle(PORTAL_TIMER_BOUNDS);
        Canvas c = core.getScreen().getDrawableCanvas();
        String text = core.getOCR().getText(Font.STANDARD_FONT, portalTextBounds, WHITE_TEXT);
        if (text.isEmpty()) {
            return null;
        }
        text = text.trim();
        String[] parts = text.split(" - ");
        if (parts.length != 2) {
            core.log(Overlay.class, "Portal text does not contain expected format: " + text);
            return null;
        }
        String portalText = parts[0].trim();
        Long timeLeft = parseTimeString(parts[1].trim());
        if (timeLeft == null) {
            core.log(Overlay.class, "Could not parse time left from portal text: " + parts[1].trim());
            return null;
        }

        for (Portal portal : Portal.values()) {
            if (portalText.equalsIgnoreCase(portal.getName())) {
                c.fillRect(portalTextBounds, Color.GREEN.getRGB(), 0.3);
                c.drawRect(portalTextBounds, Color.RED.getRGB());
                return new PortalInfo((int) TimeUnit.MILLISECONDS.toSeconds(timeLeft), portal);
            }
        }
        return null;
    }

    private Integer getGuardianPower(Rectangle overlayBounds) {
        Integer textValue = getBarValue(overlayBounds);
        if (textValue == null) {
            textValue = getBarValueFromFill(overlayBounds);
        }
        return textValue;
    }

    private Integer getBarValue(Rectangle overlayBounds) {
        Rectangle barBounds = overlayBounds.getSubRectangle(GUARDIANS_POWER_BAR).getPadding(2);
        core.getScreen().getDrawableCanvas().drawRect(barBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.SMALL_FONT, barBounds, BLACK_TEXT_COLOR);

        if (!text.contains("%")) {
            return null;
        }
        String healthString = text.replaceAll("[^0-9]", "");
        if (healthString.isEmpty()) {
            return null;
        }
        return Integer.parseInt(healthString);
    }
    private static final SearchablePixel[] ACTIVE_BAR_PIXELS = new SearchablePixel[]{new SearchablePixel(-338102, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB),
            new SearchablePixel(-400760, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB)};

    private Integer getBarValueFromFill(Rectangle overlayBounds) {
        Rectangle barBounds = overlayBounds.getSubRectangle(GUARDIANS_POWER_BAR).getPadding(1);
        for (int x = barBounds.x; x < barBounds.x + barBounds.width; x++) {
            boolean lineContains = false;
            for (int y = barBounds.y; y < barBounds.y + barBounds.height; y++) {
                int pixelColor = core.getPixelAnalyzer().getPixelAt(x, y);
                for(SearchablePixel pixel : ACTIVE_BAR_PIXELS) {
                    if (pixelColor == pixel.getRgb()) {
                        lineContains = true;
                        break;
                    }
                }
            }
            if(!lineContains) {
                return (int) Math.ceil((x - barBounds.x) / (double) barBounds.width * 100);
            }
        }
        return 100;
    }
    private CatalyticRune getActiveCatalyticRune(Rectangle overlayBounds) {
        for (CatalyticRune rune : CatalyticRune.values()) {
            SearchableImage runeSprite = RUNE_SPRITES.get(rune);
            if (core.getImageAnalyzer().isSubImageAt(overlayBounds.x + CATALYTIC_SPRITE_OFFSET.x, overlayBounds.y + CATALYTIC_SPRITE_OFFSET.y, runeSprite) != null) {
                return rune;
            }
        }
        return null;
    }

    private ElementalRune getActiveElementalRune(Rectangle overlayBounds) {
        for (ElementalRune rune : ElementalRune.values()) {
            SearchableImage runeSprite = RUNE_SPRITES.get(rune);
            if (core.getImageAnalyzer().isSubImageAt(overlayBounds.x + ELEMENTAL_SPRITE_OFFSET.x, overlayBounds.y + ELEMENTAL_SPRITE_OFFSET.y, runeSprite) != null) {
                return rune;
            }
        }
        return null;
    }

    private Integer getCatalyticPoints(Rectangle overlayBounds) {
        Rectangle catalyticEnergyBounds = overlayBounds.getSubRectangle(CATALYTIC_ENERGY_BOUNDS);
        core.getScreen().getDrawableCanvas().drawRect(catalyticEnergyBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.STANDARD_FONT, catalyticEnergyBounds, WHITE_TEXT);
        if (text == null || text.isEmpty()) {
            return null;
        }
        text = text.replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);

    }

    private Integer getElementalPoints(Rectangle overlayBounds) {
        Rectangle elementalEnergyBounds = overlayBounds.getSubRectangle(ELEMENTAL_ENERGY_BOUNDS);
        core.getScreen().getDrawableCanvas().drawRect(elementalEnergyBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.STANDARD_FONT, elementalEnergyBounds, WHITE_TEXT);
        if (text == null || text.isEmpty()) {
            return null;
        }
        text = text.replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);

    }

    private String getPortalText(Rectangle overlayBounds) {
        Rectangle portalTimerBounds = overlayBounds.getSubRectangle(PORTAL_TIMER_BOUNDS);
        core.getScreen().getDrawableCanvas().drawRect(portalTimerBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.STANDARD_FONT, portalTimerBounds, WHITE_TEXT);
        if (text == null) {
            return null;
        }
        if (text.isEmpty()) return null;
        return text;

    }

    private Long getTimerSeconds(Rectangle overlayBounds) {
        Rectangle timerBounds = overlayBounds.getSubRectangle(TIMER_BOUNDS);
        core.getScreen().getDrawableCanvas().drawRect(timerBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.STANDARD_FONT, timerBounds, WHITE_TEXT);
        if (text == null) {
            return null;
        }
        text = text.replaceAll("[^0-9:]", "");
        return parseTimeString(text);
    }

    @Override
    public void onOverlayFound(Rectangle overlayBounds) {

    }

    @Override
    public void onOverlayNotFound() {

    }

    public class PortalInfo {
        private final int secondsRemaining;
        private final Portal portal;

        public PortalInfo(int secondsRemaining, Portal portal) {
            this.secondsRemaining = secondsRemaining;
            this.portal = portal;
        }

        public int getSecondsRemaining() {
            return secondsRemaining;
        }

        public Portal getPortal() {
            return portal;
        }

    }
}
