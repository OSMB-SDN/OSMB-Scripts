package com.osmb.script.runecrafting.gotr;

import com.osmb.api.item.ItemID;

public enum CatalyticRune implements Rune {
    CHAOS(4360, "Chaos", ItemID.CHAOS_RUNE, ItemID.PORTAL_TALISMAN_CHAOS, "Guardian of Chaos", 9035, Rune.Cell.MEDIUM.getTier(), 35),
    DEATH(4363, "Death", ItemID.DEATH_RUNE, ItemID.PORTAL_TALISMAN_DEATH, "Guardian of Death", 8779, Rune.Cell.OVERCHARGED.getTier(), 65,"Mourning's End Part II"),
    BLOOD(4364, "Blood", ItemID.BLOOD_RUNE, ItemID.PORTAL_TALISMAN_BLOOD, "Guardian of Blood", 12875, Rune.Cell.OVERCHARGED.getTier(), 77, "Sins of the Father"),
    NATURE(4361, "Nature", ItemID.NATURE_RUNE, ItemID.PORTAL_TALISMAN_NATURE, "Guardian of Nature", 9547, Rune.Cell.STRONG.getTier(), 44),
    LAW(4362, "Law", ItemID.LAW_RUNE, ItemID.PORTAL_TALISMAN_LAW, "Guardian of Law", 9803, Rune.Cell.STRONG.getTier(), 54, "Troll Stronghold"),
    COSMIC(4359, "Cosmic", ItemID.COSMIC_RUNE, ItemID.PORTAL_TALISMAN_COSMIC, "Guardian of Cosmic", 8523, Rune.Cell.MEDIUM.getTier(), 27, "Lost City"),
    MIND(4354, "Mind", ItemID.MIND_RUNE, ItemID.PORTAL_TALISMAN_MIND, "Guardian of Mind", 11083, Rune.Cell.WEAK.getTier(), 2),
    BODY(4358, "Body", ItemID.BODY_RUNE, ItemID.PORTAL_TALISMAN_BODY, "Guardian of Body", 10059, Rune.Cell.WEAK.getTier(), 20);

    private final int interfaceSpriteId;
    private final String name;
    private final int runeId;
    private final String guardianName;
    private final int altarRegionId;
    private final int tier;
    private final int talismanId;
    private final int levelRequirement;

    public String getQuestName() {
        return questName;
    }

    private final String questName;

    CatalyticRune(int interfaceSpriteId, String name, int runeId, int talismanId, String guardianName, int altarRegionId, int tier, int levelRequirement, String questName) {
        this.interfaceSpriteId = interfaceSpriteId;
        this.name = name;
        this.runeId = runeId;
        this.guardianName = guardianName;
        this.altarRegionId = altarRegionId;
        this.tier = tier;
        this.talismanId = talismanId;
        this.levelRequirement = levelRequirement;
        this.questName = questName;
    }
    CatalyticRune(int interfaceSpriteId, String name, int runeId, int talismanId, String guardianName, int altarRegionId, int tier, int levelRequirement) {
        this.interfaceSpriteId = interfaceSpriteId;
        this.name = name;
        this.runeId = runeId;
        this.guardianName = guardianName;
        this.altarRegionId = altarRegionId;
        this.tier = tier;
        this.talismanId = talismanId;
        this.levelRequirement = levelRequirement;
        this.questName = null;
    }

    public static boolean isCatalytic(Rune rune) {
        for (CatalyticRune catalyticRune : CatalyticRune.values()) {
            if (catalyticRune == rune) {
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
    public int getInterfaceSpriteId() {
        return interfaceSpriteId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getRuneId() {
        return runeId;
    }

    @Override
    public String getGuardianName() {
        return guardianName;
    }

    @Override
    public int getAltarRegionId() {
        return altarRegionId;
    }

    @Override
    public int getTier() {
        return tier;
    }
}
