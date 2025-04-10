package com.osmb.script.agility;

import com.osmb.api.location.area.Area;
import com.osmb.api.visual.drawing.Canvas;

public interface Course {

    int poll(AIOAgility core);

    Area getBankArea();

    int[] regions();

    String name();

    void onPaint(Canvas gc);
}
