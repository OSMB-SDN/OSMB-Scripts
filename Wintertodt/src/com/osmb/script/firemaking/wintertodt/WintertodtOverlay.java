package com.osmb.script.firemaking.wintertodt;

import com.osmb.api.ScriptCore;
import com.osmb.api.definition.SpriteDefinition;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.overlay.OverlayBoundary;
import com.osmb.api.ui.overlay.OverlayPosition;
import com.osmb.api.ui.overlay.OverlayValueFinder;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WintertodtOverlay extends OverlayBoundary {
    public static final String COUNTDOWN = "countdown";
    public static final String ENERGY = "energy";
    public static final String WARMTH = "warmth";
    public static final String POINTS = "points";
    public static final String BRAZIER_STATUS = "brazierStatus";
    public static final String INCAPACITATED = "incapacitated";
    public static final String BOSS_ACTIVE = "bossActive";
    private static final Rectangle WARMTH_BAR = new Rectangle(1, 1, 198, 13);
    private static final Rectangle WINTERTODTS_ENERGY_BAR = new Rectangle(1, 16, 198, 13);
    private static final Rectangle POINTS_AREA = new Rectangle(34, 93, 32, 10);
    private static final int BLACK_TEXT_COLOR = -16777215;
    private static final SearchablePixel YELLOW_UPDATE_TEXT = new SearchablePixel(-256, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    private static final SearchablePixel RED_PIXEL = new SearchablePixel(-65536, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    private static final SearchablePixel WHITE_PIXEL = new SearchablePixel(-1, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    private static final SearchablePixel RED_BAR_PIXEL = new SearchablePixel(-3407872, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    private static final SearchablePixel GREEN_PIXEL = new SearchablePixel(-16724992, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    private static final SearchablePixel ORANGE_PIXEL = new SearchablePixel(-761600, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    private static final SearchablePixel TURQUOISE_PIXEL = new SearchablePixel(-16745367, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    private static final SearchablePixel BLACK_OUTLINE = new SearchablePixel(-16777216, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);
    private static final SearchablePixel[] ACTIVE_BAR_PIXELS = new SearchablePixel[]{ORANGE_PIXEL, GREEN_PIXEL};
    private final Map<BrazierStatus, SearchableImage> statusImages;
    private final List<SearchableImage> incapacitatedImages;

    public WintertodtOverlay(ScriptCore core) {
        super(core);

        // build images
        this.statusImages = new HashMap<>();
        for (BrazierStatus status : BrazierStatus.values()) {
            SpriteDefinition spriteDefinition = core.getSpriteManager().getSprite(status.getSpriteID());
            statusImages.put(status, new SearchableImage(spriteDefinition, ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB));
        }
        SpriteDefinition incapacitatedSprite = core.getSpriteManager().getSprite(1400);

        // image for top part where status icon partially covers incapacitated icon
        Canvas canvas = new Canvas(incapacitatedSprite);
        // make the rest of the image transparent below 10 pixels
        canvas.fillRect(0, 10, canvas.canvasWidth, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        SearchableImage topImage = canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);

        // make bottom image
        canvas = new Canvas(incapacitatedSprite);
        // make top part transparent
        canvas.fillRect(0, 0, canvas.canvasWidth, 12, ColorUtils.TRANSPARENT_PIXEL);
        SearchableImage bottomImage = canvas.toSearchableImage(ToleranceComparator.ZERO_TOLERANCE, ColorModel.RGB);

        incapacitatedImages = List.of(topImage, bottomImage);
    }

    public Integer getWarmthPercent() {
        return (Integer) getValue(WARMTH);
    }

    public Integer getCountdown() {
        return (Integer) getValue(COUNTDOWN);
    }

    public Boolean isBossActive() {
        return (Boolean) getValue(BOSS_ACTIVE);
    }

    public Integer getEnergyPercent() {
        return (Integer) getValue(ENERGY);
    }

    public Integer getPoints() {
        return (Integer) getValue(POINTS);
    }

    @Override
    public boolean isVisible() {
        boolean b = super.isVisible();
        if (b) {
            Rectangle bounds = getBounds();
            if (bounds != null) {
                core.getScreen().getDrawableCanvas().drawRect(bounds, Color.RED.getRGB());
            }
        }
        return b;
    }

    public BrazierStatus getBrazierStatus(Brazier brazier) {
        Map<Brazier, BrazierStatus> statusMap = (Map<Brazier, BrazierStatus>) getValue(BRAZIER_STATUS);
        if (statusMap == null) {
            return null;
        }
        return statusMap.get(brazier);
    }

    public Boolean getIncapacitated(Brazier brazier) {
        Map<Brazier, Boolean> incapacitatedMap = (Map<Brazier, Boolean>) getValue(INCAPACITATED);
        if (incapacitatedMap == null) {
            return null;
        }
        return incapacitatedMap.get(brazier);
    }

    @Override
    public int getWidth() {
        return 200;
    }

    @Override
    public int getHeight() {
        return 140;
    }

    @Override
    public boolean checkVisibility(Rectangle bounds) {
        BrazierStatus brazierStatus = findBrazierIcon(Brazier.SOUTH_WEST, bounds);
        BrazierStatus brazierStatus2 = findBrazierIcon(Brazier.NORTH_WEST, bounds);

        return brazierStatus != null && brazierStatus2 != null;
    }

    @Override
    public OverlayPosition getOverlayPosition() {
        return OverlayPosition.TOP_LEFT;
    }

    @Override
    public Point getOverlayOffset() {
        return new Point(60, 4);
    }

    @Override
    public List<OverlayValueFinder> applyValueFinders() {
        OverlayValueFinder<Integer> countdownValueFinder = new OverlayValueFinder<>(COUNTDOWN, this::updateCountdown);
        OverlayValueFinder<Integer> energyValueFinder = new OverlayValueFinder<>(ENERGY, this::updateEnergyPercent);
        OverlayValueFinder<Integer> warmthValueFinder = new OverlayValueFinder<>(WARMTH, this::updateWarmthPercent);
        OverlayValueFinder<Integer> pointsValueFinder = new OverlayValueFinder<>(POINTS, this::updatePoints);
        OverlayValueFinder<Map<Brazier, BrazierStatus>> brazierStatusFinder = new OverlayValueFinder<>(BRAZIER_STATUS, this::updateBrazierStatusMap);
        OverlayValueFinder<Map<Brazier, Boolean>> incapacitatedStatusValueFinder = new OverlayValueFinder<>(INCAPACITATED, this::updateIncapacitatedMap);
        OverlayValueFinder<Boolean> bossActiveValueFinder = new OverlayValueFinder<>(BOSS_ACTIVE, this::updateBossActive);
        return List.of(countdownValueFinder, energyValueFinder, warmthValueFinder, pointsValueFinder, brazierStatusFinder, incapacitatedStatusValueFinder, bossActiveValueFinder);
    }

    @Override
    public void onOverlayFound(Rectangle overlayBounds) {

    }

    @Override
    public void onOverlayNotFound() {

    }

    public Integer updateCountdown(Rectangle overlayBounds) {
        Rectangle barBounds = overlayBounds.getSubRectangle(WINTERTODTS_ENERGY_BAR);
        core.getScreen().getDrawableCanvas().drawRect(barBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.SMALL_FONT, barBounds, BLACK_TEXT_COLOR);
        if (text == null) {
            return null;
        }
        if (text.toLowerCase().startsWith("the wintertodt returns")) {
            String countDownString = text.replaceAll("[^0-9]", "");
            if (countDownString.isEmpty()) {
                return null;
            }
            return Integer.parseInt(countDownString);
        }
        return null;
    }

    public Integer updateWarmthPercent(Rectangle overlayBounds) {
        Integer barPercentage = getBarPercentage(overlayBounds, WARMTH_BAR);
        if (barPercentage == null) {
            // Fallback solution, try to get the value from the fill
            barPercentage = getBarValueFromFill(overlayBounds, WARMTH_BAR);
        }
        return barPercentage;
    }

    public Integer updateEnergyPercent(Rectangle overlayBounds) {
        Integer barPercentage = getBarPercentage(overlayBounds, WINTERTODTS_ENERGY_BAR);
        if (barPercentage == null) {
            // Fallback solution, try to get the value from the fill
            barPercentage = getBarValueFromFill(overlayBounds, WINTERTODTS_ENERGY_BAR);
        }
        return barPercentage;
    }

    public Boolean updateBossActive(Rectangle overlayBounds) {
        Rectangle barBounds = overlayBounds.getSubRectangle(WINTERTODTS_ENERGY_BAR);
        core.getScreen().getDrawableCanvas().drawRect(barBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.SMALL_FONT, barBounds, BLACK_TEXT_COLOR);
        return !(text != null && text.toLowerCase().contains("wintertodt returns"));
    }

    private Integer getBarPercentage(Rectangle overlayBounds, Rectangle barBounds) {
        barBounds = overlayBounds.getSubRectangle(barBounds);
        core.getScreen().getDrawableCanvas().drawRect(barBounds, Color.RED.getRGB());
        String text = core.getOCR().getText(Font.SMALL_FONT, barBounds, BLACK_TEXT_COLOR);
        if (text != null && core.getPixelAnalyzer().findPixel(barBounds, YELLOW_UPDATE_TEXT) == null) {
            if (!text.contains("%")) {
                return null;
            }
            String healthString = text.replaceAll("[^0-9]", "");
            if (healthString.isEmpty()) {
                return null;
            }
            return Integer.parseInt(healthString);
        }
        return null;
    }

    private Integer getBarValueFromFill(Rectangle overlayBounds, Rectangle barBounds) {
        barBounds = overlayBounds.getSubRectangle(barBounds);
        for (int x = barBounds.x; x < barBounds.x + barBounds.width; x++) {
            boolean lineContains = false;
            for (int y = barBounds.y; y < barBounds.y + barBounds.height; y++) {
                int pixelColor = core.getPixelAnalyzer().getPixelAt(x, y);
                for (SearchablePixel pixel : ACTIVE_BAR_PIXELS) {
                    if (pixelColor == pixel.getRgb()) {
                        lineContains = true;
                        break;
                    }
                }
            }
            if (!lineContains) {
                return (int) Math.ceil((x - barBounds.x) / (double) barBounds.width * 100);
            }
        }
        return 100;
    }

    public Integer updatePoints(Rectangle overlayBounds) {
        Rectangle pointsBounds = overlayBounds.getSubRectangle(POINTS_AREA);
        core.getScreen().getDrawableCanvas().drawRect(pointsBounds, Color.RED.getRGB());

        String text = core.getOCR().getText(Font.SMALL_FONT, pointsBounds, RED_PIXEL.getRgb(), WHITE_PIXEL.getRgb());
        if (text == null) {
            return null;
        }
        text = text.replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private BrazierStatus findBrazierIcon(Brazier brazier, Rectangle overlayBounds) {
        int x = brazier.getStatusIconX();
        int y = brazier.getStatusIconY();
        for (BrazierStatus brazierStatus : BrazierStatus.values()) {
            SearchableImage statusImage = statusImages.get(brazierStatus);
            int xOffset = 24 - statusImage.width;
            int searchX = overlayBounds.x + x + xOffset;
            int searchY = overlayBounds.y + y;
            if (brazierStatus == BrazierStatus.UNLIT) {
                searchY += 8;
            }
            ImageSearchResult result = core.getImageAnalyzer().isSubImageAt(searchX, searchY, statusImage);
            if (result != null) {
                return brazierStatus;
            }
        }
        return null;
    }

    public Map<Brazier, BrazierStatus> updateBrazierStatusMap(Rectangle overlayBounds) {
        Map<Brazier, BrazierStatus> brazierStatusMap = new HashMap<>();
        for (Brazier brazier : Brazier.values()) {
            BrazierStatus brazierStatus = findBrazierIcon(brazier, overlayBounds);
            if (brazierStatus != null) {
                brazierStatusMap.put(brazier, brazierStatus);
            } else {
                brazierStatusMap.put(brazier, null);
            }
        }

        return brazierStatusMap;
    }

    public Map<Brazier, Boolean> updateIncapacitatedMap(Rectangle overlayBounds) {
        Map<Brazier, Boolean> incapacitatedMap = new HashMap<>();
        for (Brazier brazier : Brazier.values()) {
            int x = overlayBounds.x + brazier.getIncapacitatedX();
            int y = overlayBounds.y + brazier.getIncapacitatedY();
            for (SearchableImage incapacitatedImage : incapacitatedImages) {
                ImageSearchResult result = core.getImageAnalyzer().isSubImageAt(x, y, incapacitatedImage);
                boolean found = result != null;
                incapacitatedMap.put(brazier, found);
                if (found) {
                    break;
                }
            }
        }
        return incapacitatedMap;
    }

    public enum BrazierStatus {
        BROKEN(1397),
        LIT(1399),
        UNLIT(1398);

        private final int spriteID;

        BrazierStatus(int spriteID) {
            this.spriteID = spriteID;
        }

        public int getSpriteID() {
            return spriteID;
        }
    }
}

