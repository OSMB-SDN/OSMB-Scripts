package com.osmb.script.chartercrafting;


import com.osmb.api.location.area.Area;
import com.osmb.api.location.area.impl.RectangleArea;

public enum Dock {
    BRIMHAVEN("Brimhaven",11058, new RectangleArea(1, 1, 1, 1, 0), null, null),
    CATHERBY("Catherby",11061, new RectangleArea(1, 1, 1, 1, 0), new RectangleArea(1, 1, 1, 1, 0), null),
    CORSAIR_COVE("Corsair Cove",10028, new RectangleArea(2583, 2850, 6, 1, 0), new RectangleArea(2567, 2858, 8, 6, 0), null),
  // not got acc to check
   // MOS_LE_HARMLESS(14638, new RectangleArea(1, 1, 1, 1, 0), null, null),
    MUSA_POINT("Musa Point",11569, new RectangleArea(1, 1, 1, 1, 0), null, null),
    PORT_KHAZARD("Port Khazard",10545, new RectangleArea(1, 1, 1, 1, 0), new RectangleArea(1, 1, 1, 1, 0), null),
    PORT_TYRAS("Port Tyras",8496, new RectangleArea(1, 1, 1, 1, 0), null, null),
    PORT_PHASMATYS("Port Phasmatys",14646, new RectangleArea(3701, 3497, 4, 8, 0), new RectangleArea(3683, 3466, 8, 9, 0), new RectangleArea(3679, 3474, 10, 9, 0)),
    PORT_PISCARILIUS("Port Piscarilius",7225, new RectangleArea(1, 1, 1, 1, 0), new RectangleArea(1, 1, 1, 1, 0), new RectangleArea(1, 1, 1, 1, 0)),
    PORT_SARIM("Port Sarim",12082, new RectangleArea(1, 1, 1, 1, 0), null, null),
    PRIFDDINAS("Prifddinas",8499, new RectangleArea(1, 1, 1, 1, 0), null, null),
    SHIPYARD("Shipyard",11823, new RectangleArea(1, 1, 1, 1, 0), null, null),
    LANDS_END("Lands End",11823, new RectangleArea(1, 1, 1, 1, 0), new RectangleArea(1,1,1,1,0), null);

    private final Area wanderArea;
    private final Area bankArea;
    private final Area furnaceArea;
    private final int regionID;
    private final String name;
    Dock(String name, int regionID, Area npcWanderArea, Area bankArea, Area furnaceArea) {
        this.name = name;
        this.wanderArea = npcWanderArea;
        this.regionID = regionID;
        this.bankArea = bankArea;
        this.furnaceArea = furnaceArea;
    }

    public Area getFurnaceArea() {
        return furnaceArea;
    }

    public Area getBankArea() {
        return bankArea;
    }

    public Area getWanderArea() {
        return wanderArea;
    }

    public int getRegionID() {
        return regionID;
    }

    @Override
    public String toString() {
        return name;
    }
}
