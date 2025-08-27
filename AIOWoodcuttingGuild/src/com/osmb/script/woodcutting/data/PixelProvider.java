package com.osmb.script.woodcutting.data;

import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;

public class PixelProvider {

    /**
     * Pixels representing a cluster of tree colors in HSL format. Will cover normal trees, oaks.
     */
     static final SearchablePixel[] TREE_CLUSTER_1 = new SearchablePixel[] {
            new SearchablePixel(-14012413, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12958706, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11316685, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11312366, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13089777, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12103646, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-15329787, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-15131126, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-10917354, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-9274047, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11839205, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13422044, new SingleThresholdComparator(2), ColorModel.HSL),
    };
    /**
     * Pixels representing a cluster of tree colors in HSL format. Will cover maple trees.
     */
    static final SearchablePixel[] TREE_CLUSTER_2 = new SearchablePixel[] {
            new SearchablePixel(-7247872, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11395072, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11260160, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-9619968, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13495040, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-11518464, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-13821184, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-12900096, new SingleThresholdComparator(2), ColorModel.HSL),
            new SearchablePixel(-8893696, new SingleThresholdComparator(2), ColorModel.HSL),
    };

}
