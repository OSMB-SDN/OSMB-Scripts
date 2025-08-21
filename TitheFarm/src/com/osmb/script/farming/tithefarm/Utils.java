package com.osmb.script.farming.tithefarm;

import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;

public class Utils {
    /**
     * Checks if the given world position is cardinally adjacent (N, S, E, W) to the rectangle area.
     */
    public static boolean isCardinallyAdjacent(WorldPosition pos, RectangleArea area) {
        int x = pos.getX();
        int y = pos.getY();
        int minX = area.getX();
        int maxX = area.getX() + area.getWidth();
        int minY = area.getY();
        int maxY = area.getY() + area.getHeight();

        boolean north = (y == maxY + 1) && (x >= minX && x <= maxX);
        boolean south = (y == minY - 1) && (x >= minX && x <= maxX);
        boolean east = (x == maxX + 1) && (y >= minY && y <= maxY);
        boolean west = (x == minX - 1) && (y >= minY && y <= maxY);

        return north || south || east || west;
    }

    public static int getRepeatingDigitAmount() {
        int digit = com.osmb.api.utils.Utils.random(1, 9); // Random digit 1-9
        int repeatCount = com.osmb.api.utils.Utils.random(5, 8); // Repeat 2-4 times
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < repeatCount; i++) {
            sb.append(digit);
        }
        return Integer.parseInt(sb.toString());
    }
}
