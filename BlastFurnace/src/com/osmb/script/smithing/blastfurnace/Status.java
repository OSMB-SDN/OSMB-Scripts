package com.osmb.script.smithing.blastfurnace;

import com.osmb.api.utils.timing.Stopwatch;
import com.osmb.script.smithing.blastfurnace.data.Bar;

import java.util.HashMap;
import java.util.Map;

public class Status {
    public static final Stopwatch FOREMAN_PAYMENT_TIMER = new Stopwatch();
    public static final Stopwatch GOLD_GLOVE_EQUIP_DELAY = new Stopwatch();
    public static final Stopwatch ICE_GLOVE_EQUIP_DELAY = new Stopwatch();
    public static final Stopwatch ICE_GLOVE_EQUIP_IGNORE_DELAY = new Stopwatch();
    public static final Stopwatch BAR_DISPENSER_INTERACTION_DELAY = new Stopwatch();
    public static boolean hasCoalBag = false;
    public static boolean coalBagFull = false;
    public static boolean expectBarsToBeCollected = false;
    public static boolean setZoom = false;

    public static final Map<Bar, Integer> MELTING_POT_BARS = new HashMap<>();
}
