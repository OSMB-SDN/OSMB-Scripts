package com.osmb.script.smithing.furnace.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.item.ItemSearchResult;
import com.osmb.api.item.SearchableItem;
import com.osmb.api.shape.Rectangle;
import com.osmb.api.ui.component.ComponentCentered;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.utils.RandomUtils;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.ToleranceComparator;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;
import com.osmb.api.visual.image.ImageSearchResult;
import com.osmb.api.visual.image.SearchableImage;
import com.osmb.api.visual.ocr.fonts.Font;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SilverCraftingInterface extends CraftingInterface {

    public SilverCraftingInterface(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(506, 324, ColorUtils.TRANSPARENT_PIXEL);
        // create steel border background
        canvas.createBackground(core, BorderPalette.STEEL_BORDER, null);
        // set middle to transparent
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        return new ComponentImage<>(canvas.toSearchableImage(new SingleThresholdComparator(10), ColorModel.RGB), -1, 1);
    }
}
