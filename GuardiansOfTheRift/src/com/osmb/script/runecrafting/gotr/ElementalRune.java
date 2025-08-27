package com.osmb.script.runecrafting.gotr;

import com.osmb.api.item.ItemID;

public enum ElementalRune implements Rune {
    AIR(4353, "Air", ItemID.AIR_RUNE, ItemID.PORTAL_TALISMAN_AIR, "Guardian of Air", 11339, Rune.Cell.WEAK.getTier(),1),
    WATER(4355, "Water", ItemID.WATER_RUNE, ItemID.PORTAL_TALISMAN_WATER,"Guardian of Water", 10827, Rune.Cell.MEDIUM.getTier(),5),
    FIRE(4357, "Fire", ItemID.FIRE_RUNE,ItemID.PORTAL_TALISMAN_FIRE, "Guardian of Fire", 10315, Rune.Cell.OVERCHARGED.getTier(),14),
    EARTH(4356, "Earth", ItemID.EARTH_RUNE, ItemID.PORTAL_TALISMAN_EARTH,"Guardian of Earth", 10571, Rune.Cell.STRONG.getTier(),9);

    private final int interfaceSpriteId;
    private final String name;
    private final int runeId;
    private final String guardianName;
    private final int altarRegionId;
    private final int tier;
    private final int talismanId;
    private final int levelRequirement;

    ElementalRune(int interfaceSpriteId, String name, int runeId, int talismanId, String guardianName, int altarRegionId, int tier, int levelRequirement) {
        this.interfaceSpriteId = interfaceSpriteId;
        this.name = name;
        this.runeId = runeId;
        this.talismanId = talismanId;
        this.guardianName = guardianName;
        this.altarRegionId = altarRegionId;
        this.tier = tier;
        this.levelRequirement = levelRequirement;
    }

    public static boolean isElemental(Rune rune) {
        for (ElementalRune elementalRune : ElementalRune.values()) {
            if (elementalRune == rune) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getTalismanId() {
        return talismanId;
    }

    @Override
    public int getLevelRequirement() {
        return levelRequirement;
    }

    @Override
    public String getQuestName() {
        return null;
    }

    @Override
    public int getTier() {
        return tier;
    }

    public int getInterfaceSpriteId() {
        return interfaceSpriteId;
    }

    public String getName() {
        return name;
    }

    public int getRuneId() {
        return runeId;
    }

    public String getGuardianName() {
        return guardianName;
    }

    @Override
    public int getAltarRegionId() {
        return altarRegionId;
    }
}
