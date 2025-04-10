package com.osmb.script.chartercrafting;

import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;

import java.util.ArrayList;
import java.util.List;

public enum NPC {
    // Define enum constants without calling any methods that might need the enum to be fully initialized
    BLUE_MAN(Dock.PORT_PISCARILIUS, Dock.PORT_SARIM, Dock.CATHERBY, Dock.MUSA_POINT, Dock.CORSAIR_COVE, Dock.LANDS_END),
    BLUE_WOMAN(Dock.PORT_PISCARILIUS, Dock.PORT_SARIM, Dock.CATHERBY, Dock.BRIMHAVEN, Dock.CORSAIR_COVE, Dock.LANDS_END),
    WHITE_MAN(Dock.PORT_PHASMATYS, Dock.SHIPYARD, Dock.PORT_KHAZARD),
    BLACK_MAN(Dock.BRIMHAVEN, Dock.PORT_TYRAS),
    DARK_PINK_YELLOW_WOMAN(Dock.SHIPYARD, Dock.PORT_KHAZARD, Dock.PORT_TYRAS),
    BURGUNDY_WHITE_WOMAN(Dock.PORT_PHASMATYS, Dock.MUSA_POINT);

    private final Dock[] docks;
    private SearchablePixel[] searchablePixels; // Make this non-final and initialize lazily

    NPC(Dock... docks) {
        this.docks = docks;
        // Don't initialize searchablePixels here
    }

    public static List<NPC> getNpcsForDock(Dock dock) {
        List<NPC> npcsAtDock = new ArrayList<>();
        for (NPC npc : NPC.values()) {
            for (Dock npcDock : npc.docks) {
                if (npcDock == dock) {
                    npcsAtDock.add(npc);
                    break;
                }
            }
        }
        return npcsAtDock;
    }

    private synchronized SearchablePixel[] getOrCreatePixels() {
        if (searchablePixels == null) {
            searchablePixels = createPixels();
        }
        return searchablePixels;
    }

    private SearchablePixel[] createPixels() {
        switch (this) {
            case BLUE_MAN:
                return new SearchablePixel[]{
                        new SearchablePixel(-12695155, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-14407348, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-13946788, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-13222536, new SingleThresholdComparator(3), ColorModel.RGB),
                };
            case BLUE_WOMAN:
                return new SearchablePixel[]{
                        new SearchablePixel(-15448431, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-15848617, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-15850939, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-15314774, new SingleThresholdComparator(3), ColorModel.RGB),
                };
            case WHITE_MAN:
                return new SearchablePixel[]{
                        new SearchablePixel(-8555404, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-7569023, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-6516592, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-4279373, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-5003867, new SingleThresholdComparator(3), ColorModel.RGB),
                };
            case BURGUNDY_WHITE_WOMAN:
                return new SearchablePixel[]{
                        new SearchablePixel(-13890039, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-14808823, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-14284535, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-14808823, new SingleThresholdComparator(3), ColorModel.RGB),
                };
            case BLACK_MAN:
                return new SearchablePixel[]{
                        new SearchablePixel(-15327714, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-14997719, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-15129564, new SingleThresholdComparator(3), ColorModel.RGB),
                };
            case DARK_PINK_YELLOW_WOMAN:
                return new SearchablePixel[]{
                        new SearchablePixel(-8777391, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-11989455, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-3095995, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-11661772, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-9039537, new SingleThresholdComparator(3), ColorModel.RGB),
                        new SearchablePixel(-10744259, new SingleThresholdComparator(3), ColorModel.RGB),
                };
            default:
                throw new IllegalStateException("Unexpected NPC: " + this);
        }
    }

    public SearchablePixel[] getSearchablePixels() {
        return getOrCreatePixels();
    }
}