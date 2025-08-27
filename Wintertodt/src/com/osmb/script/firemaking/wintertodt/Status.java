package com.osmb.script.firemaking.wintertodt;

import com.osmb.api.utils.timing.Stopwatch;

public class Status {

    public static Task task;


    public static int nextDoseRestock = 8;
    public static int nextDrinkPercent;

    public static int fletchTimeout;
    public static int brazierTimeout;
    public static long chopRootsTimeout;
    public static Stopwatch breakDelay;
    public static int potionsToPrep;
    public static int idleTimeout;
    public static boolean checkedEquipment;
    public static int nextMilestone;

    public static Integer points;
    public static Integer warmth;
    public static Integer wintertodtEnergy;
    public static WintertodtOverlay.BrazierStatus brazierStatus;

}
