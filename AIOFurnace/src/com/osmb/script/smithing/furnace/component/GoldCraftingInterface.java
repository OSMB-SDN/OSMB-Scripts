package com.osmb.script.smithing.furnace.component;

import com.osmb.api.ScriptCore;
import com.osmb.api.ui.component.ComponentImage;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.ColorUtils;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;
import com.osmb.api.visual.drawing.BorderPalette;
import com.osmb.api.visual.drawing.Canvas;

public class GoldCraftingInterface extends CraftingInterface {

    public GoldCraftingInterface(ScriptCore core) {
        super(core);
    }

    @Override
    protected ComponentImage buildBackgroundImage() {
        Canvas canvas = new Canvas(492, 300, ColorUtils.TRANSPARENT_PIXEL);
        // create steel border background
        canvas.createBackground(core, BorderPalette.STEEL_BORDER, null);
        // set middle to transparent
        canvas.fillRect(5, 5, canvas.canvasWidth - 10, canvas.canvasHeight - 10, ColorUtils.TRANSPARENT_PIXEL);
        return new ComponentImage<>(canvas.toSearchableImage(new SingleThresholdComparator(10), ColorModel.RGB), -1, 1);
    }
}
