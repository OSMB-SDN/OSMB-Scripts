package com.osmb.script.woodcutting.data;

import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.PolyArea;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;

import java.util.List;

public class AreaManager {
    public static final Area GUILD_AREA = new PolyArea(List.of(new WorldPosition(1654, 3516, 0), new WorldPosition(1654, 3509, 0), new WorldPosition(1657, 3506, 0), new WorldPosition(1657, 3503, 0), new WorldPosition(1656, 3502, 0), new WorldPosition(1655, 3502, 0), new WorldPosition(1655, 3497, 0), new WorldPosition(1647, 3489, 0), new WorldPosition(1633, 3489, 0), new WorldPosition(1633, 3492, 0), new WorldPosition(1632, 3493, 0), new WorldPosition(1622, 3493, 0), new WorldPosition(1616, 3487, 0), new WorldPosition(1612, 3487, 0), new WorldPosition(1607, 3492, 0), new WorldPosition(1607, 3497, 0), new WorldPosition(1600, 3497, 0), new WorldPosition(1600, 3485, 0), new WorldPosition(1595, 3480, 0), new WorldPosition(1595, 3472, 0), new WorldPosition(1586, 3472, 0), new WorldPosition(1581, 3477, 0), new WorldPosition(1563, 3477, 0), new WorldPosition(1563, 3497, 0), new WorldPosition(1564, 3498, 0), new WorldPosition(1576, 3498, 0), new WorldPosition(1581, 3503, 0), new WorldPosition(1600, 3503, 0), new WorldPosition(1600, 3500, 0), new WorldPosition(1607, 3500, 0), new WorldPosition(1607, 3506, 0), new WorldPosition(1603, 3506, 0), new WorldPosition(1603, 3510, 0), new WorldPosition(1607, 3510, 0), new WorldPosition(1607, 3513, 0), new WorldPosition(1609, 3513, 0), new WorldPosition(1611, 3515, 0), new WorldPosition(1620, 3515, 0), new WorldPosition(1623, 3518, 0), new WorldPosition(1631, 3518, 0), new WorldPosition(1633, 3516, 0)));
    public static final Area YEW_TREE_AREA = new RectangleArea(1589, 3483, 10, 14, 0);
    public static final Area MAPLE_TREE_AREA = new RectangleArea(1608, 3488, 20, 11, 0);
    public static final Area WILLOW_TREE_AREA = new RectangleArea(1626, 3493, 22, 11, 0);
    public static final Area OAK_TREE_AREA = new RectangleArea(1613, 3506, 20, 9, 0);
    public static final Area TREE_AREA = new RectangleArea(1625, 3507, 29, 9, 0);
    public static final Area BANK_AREA = new RectangleArea(1589, 3475, 4, 5, 0);
    public static final Area REDWOOD_TREE_AREA =  new RectangleArea(1567, 3479, 7, 17, 1);
    public static final Area MAGIC_TREE_AREA = new RectangleArea(1576, 3481, 7, 13, 0);
    public static final Area LADDER_AREA = new RectangleArea(1575, 3482, 1, 2, 0);
}

