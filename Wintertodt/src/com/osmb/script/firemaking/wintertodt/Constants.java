package com.osmb.script.firemaking.wintertodt;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.utils.timing.Stopwatch;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Constants {
    public static final RectangleArea BOSS_AREA = new RectangleArea(1600, 3968, 63, 63, 0);
    public static final RectangleArea SAFE_AREA = new RectangleArea(1625, 3968, 10, 19, 0);
    public static final RectangleArea SOCIAL_SAFE_AREA = new RectangleArea(1626, 3980, 8, 7, 0);

    public static final WorldPosition[] REJUVENATION_CRATE_POSITIONS = new WorldPosition[]{new WorldPosition(1634, 3982, 0), new WorldPosition(1626, 3982, 0)};
    public static final WorldPosition BREWMA_POSITION = new WorldPosition(1635, 3986, 0);

    public static final List<Integer> REJUVENATION_POTION_IDS = List.of(ItemID.REJUVENATION_POTION_1, ItemID.REJUVENATION_POTION_2, ItemID.REJUVENATION_POTION_3, ItemID.REJUVENATION_POTION_4);
    public static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.BRUMA_HERB, ItemID.BRUMA_ROOT, ItemID.BRUMA_KINDLING, ItemID.REJUVENATION_POTION_UNF, ItemID.KNIFE));
    public static final Font ARIEL = new Font("Arial", Font.PLAIN, 14);

    public static final int FIRST_MILESTONE_POINTS = 500;
    public static final int FLETCHING_KNIFE_ID = 31043;

    public static final List<Equipment> MISSING_EQUIPMENT = new ArrayList<>();

    public static final Stopwatch POTION_DRINK_COOLDOWN = new Stopwatch();


}
