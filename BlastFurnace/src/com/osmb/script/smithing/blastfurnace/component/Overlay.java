package com.osmb.script.smithing.blastfurnace.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.overlay.OverlayBoundary;
import com.osmb.api.ui.overlay.OverlayPosition;
import com.osmb.api.ui.overlay.OverlayValueFinder;
import com.osmb.api.visual.ocr.fonts.Font;
import com.osmb.script.smithing.blastfurnace.BlastFurnace;
import com.osmb.script.smithing.blastfurnace.data.Bar;
import com.osmb.script.smithing.blastfurnace.data.Ore;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Overlay extends OverlayBoundary {
    public static final Rectangle TITLE_BOUNDS = new Rectangle(6, 7, 119, 13);
    public static final String SECTIONS = "sections";
    private static final int LINE_START = 23;
    private static final int LINE_HEIGHT = 14;

    public Overlay(ScriptCore core) {
        super(core);
    }

    @Override
    public int getWidth() {
        return 162;
    }

    @Override
    public int getHeight() {
        return 172;
    }

    @Override
    protected boolean checkVisibility(Rectangle bounds) {
        Rectangle titleBounds = bounds.getSubRectangle(TITLE_BOUNDS);
        String text = core.getOCR().getText(Font.STANDARD_FONT_BOLD, titleBounds, -1);
        return text.equalsIgnoreCase("blast furnace info");
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
        return List.of(new OverlayValueFinder<>(SECTIONS, this::getBlastFurnaceInfo));
    }


    @Override
    public void onOverlayFound(Rectangle overlayBounds) {

    }

    @Override
    public void onOverlayNotFound() {

    }

    private BlastFurnaceInfo getBlastFurnaceInfo(Rectangle bounds) {
        Map<String, String> entries = getSections(bounds);
        return new BlastFurnaceInfo(core, entries);
    }

    private List<Rectangle> getLines(Rectangle bounds) {
        List<Rectangle> lines = new ArrayList<>();
        for (int y = LINE_START; y <= bounds.getHeight(); y += LINE_HEIGHT) {
            lines.add(new Rectangle(bounds.getX(), bounds.getY() + y, bounds.getWidth(), LINE_HEIGHT));
        }
        return lines;
    }


    private Map<String, String> getSections(Rectangle bounds) {
        List<Rectangle> lines = getLines(bounds);
        Map<String, String> sections = new HashMap<>();
        int startY = -1;
        StringBuilder sb = new StringBuilder();
        for (Rectangle line : lines) {
            Rectangle searchArea = new Rectangle(line.getX(), line.getY(), 80, line.getHeight());
            String text = core.getOCR().getText(Font.STANDARD_FONT, searchArea, -1);
            sb.append(text);
            if (startY == -1) {
                startY = line.getY();
            }

            if (text.endsWith(":")) {

                int height = line.getY() + line.getHeight() - startY;
                int width = 75;
                if (text.contains("Coffer")) {
                    width = 95;
                }
                Rectangle valueBounds = new Rectangle(line.getX() + line.width - width, startY, width, height);
                String sectionText = sb.toString();
                int[] color = sectionText.equalsIgnoreCase("Collection:") ? new int[]{/*green*/-16711936, /*red*/-65536} : new int[]{-1};
                core.getScreen().getDrawableCanvas().drawRect(valueBounds, Color.RED.getRGB());
                String value = core.getOCR().getText(Font.STANDARD_FONT, valueBounds, color);
                sections.put(sectionText.toLowerCase(), value.toLowerCase());
                sb.setLength(0);
                startY = -1;
            } else {
                sb.append(" ");
            }
        }
        return sections;
    }

    public static class BlastFurnaceInfo {
        private final Map<Ore, Integer> ores = new HashMap<>();
        private final Map<Bar, Integer> bars = new HashMap<>();
        private final CollectionStatus collectionReady;
        private Integer cofferValue = null;

        public BlastFurnaceInfo(ScriptCore core, Map<String, String> entries) {
            String coffer = entries.get("coffer:");
            if (coffer != null && !(coffer = coffer.replaceAll("\\D", "")).isEmpty()) {
                try {
                    this.cofferValue = Integer.parseInt(coffer);
                } catch (NumberFormatException e) {
                    core.log(Overlay.class, "Error parsing coffer value: " + coffer);
                }
            } else {
                core.log(Overlay.class, "Can't read Coffer value");
            }
            String collection = entries.get("collection:");
            if (collection != null) {
                this.collectionReady = CollectionStatus.fromString(collection);
            } else {
                core.log(Overlay.class, "Collection status not found in entries: " + entries);
                this.collectionReady = CollectionStatus.NOT_READY;
            }

            for (Map.Entry<String, String> entry : entries.entrySet()) {
                String key = entry.getKey().toLowerCase();
                String value = entry.getValue().toLowerCase();

                if (key.endsWith("coal:")) {
                    int amount = Integer.parseInt(value);
                    ores.put(Ore.COAL, amount);
                } else if (key.endsWith("ore:")) {
                    int amount = Integer.parseInt(value);
                    for (Ore ore : Ore.values()) {
                        String oreName = ore.getOreName(core);
                        if (key.toLowerCase().startsWith(oreName.toLowerCase())) {
                            ores.put(ore, amount);
                        }
                    }
                } else if (key.endsWith("bars:")) {
                    for (Bar bar : Bar.values()) {
                        String barName = bar.getBarName(core);
                        if (key.toLowerCase().startsWith(barName.toLowerCase())) {
                            int amount = Integer.parseInt(value);
                            bars.put(bar, amount);
                            break;
                        }
                    }
                }
            }
        }

        public CollectionStatus getCollectionStatus() {
            return collectionReady;
        }

        public Integer getCofferValue() {
            return cofferValue;
        }

        public int getOreAmount(Ore ore) {
            if (!ores.containsKey(ore)) {
                return 0;
            }
            return ores.get(ore);
        }

        public int getBarAmount(Bar bar) {
            if (!bars.containsKey(bar)) {
                return 0;
            }
            return bars.get(bar);
        }

        @Override
        public String toString() {
            return "BlastFurnaceInfo{" +
                    "cofferValue=" + cofferValue +
                    ", collectionReady=" + collectionReady +
                    ", ores=" + ores +
                    ", bars=" + bars +
                    '}';
        }

        public enum CollectionStatus {
            NOT_READY("ot ready"),
            READY("ready"),
            NEEDS_COOLING("cooling");

            private final String uiText;

            CollectionStatus(String uiText) {
                this.uiText = uiText;
            }

            public static CollectionStatus fromString(String text) {
                for (CollectionStatus status : CollectionStatus.values()) {
                    if (text.toLowerCase().contains(status.getUiText().toLowerCase())) {
                        return status;
                    }
                }
                throw new IllegalArgumentException("Unknown collection status: " + text);
            }

            public String getUiText() {
                return uiText;
            }
        }
    }
}
