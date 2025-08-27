package com.osmb.script.woodcutting.powerchopburn;

import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;

public class PixelProvider {

    /**
     * Pixels representing a cluster of tree colors in HSL format. Will cover normal trees, oaks.
     */
    public static final SearchablePixel[] TREE_CLUSTER_1 = new SearchablePixel[] {
            new SearchablePixel(-14012413, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-14209512, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12958706, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11316685, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13222121, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13486817, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11312366, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11972309, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13089777, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12103646, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-15329787, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-15131126, new SingleThresholdComparator(2), ColorModel.HSL),
    };
    public static final SearchablePixel[] TREE_CLUSTER_2 = new SearchablePixel[] {
            new SearchablePixel(-12168931, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11445719, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11773667, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13486558, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-14473700, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13946604, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13485041, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12367069, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11247078, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-9800139, new SingleThresholdComparator(2), ColorModel.HSL),
    };
    static final SearchablePixel[] AXE_PIXELS = new SearchablePixel[]{
            new SearchablePixel(-8758767, new SingleThresholdComparator(1), ColorModel.HSL)
    };
}
