package com.osmb.script.smithing.blastfurnace;

import com.osmb.api.item.ItemID;
import com.osmb.api.location.area.impl.RectangleArea;
import com.osmb.api.location.position.types.WorldPosition;
import com.osmb.api.visual.SearchablePixel;
import com.osmb.api.visual.color.ColorModel;
import com.osmb.api.visual.color.tolerance.impl.SingleThresholdComparator;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Constants {
    public static final List<Integer> STAMINA_POTION_IDS = List.of(ItemID.STAMINA_POTION1, ItemID.STAMINA_POTION2, ItemID.STAMINA_POTION3, ItemID.STAMINA_POTION4);
    public static final Set<Integer> ITEM_IDS_TO_RECOGNISE = new HashSet<>(Set.of(ItemID.ICE_GLOVES, ItemID.BUCKET_OF_WATER, ItemID.COINS_995, ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG, ItemID.GOLDSMITH_GAUNTLETS, ItemID.SMITHS_GLOVES_I));
    public static final Set<Integer> ITEMS_TO_NOT_DEPOSIT = new HashSet<>(Set.of(ItemID.BUCKET_OF_WATER, ItemID.ICE_GLOVES, ItemID.COAL_BAG, ItemID.OPEN_COAL_BAG, ItemID.SMITHS_GLOVES_I));
    public static final Set<Integer> BAR_IDS = new HashSet<>();
    public static final Set<Integer> ORE_IDS = new HashSet<>(Set.of(ItemID.COAL));

    public static final WorldPosition CONVEYOR_BELT_POSITION = new WorldPosition(1943, 4967, 0);

    public static final SearchablePixel YELLOW_SELECT_HIGHLIGHT_PIXEL = new SearchablePixel(-2237670, new SingleThresholdComparator(5), ColorModel.RGB);

    public static final int MAX_COAL_AMOUNT = 254;

    public static final Font ARIAL = new Font("Arial", Font.PLAIN, 14);

    public static final RectangleArea FOREMAN_AREA = new RectangleArea(1942, 4956, 4, 4, 0);
    public static final RectangleArea DISPENSER_AREA = new RectangleArea(1938, 4962, 2, 3, 0);

    public static final int MAX_ZOOM = 28;
    public static final int MIN_ZOOM = 0;

}
