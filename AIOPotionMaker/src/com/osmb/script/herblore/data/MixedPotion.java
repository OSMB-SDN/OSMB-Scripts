package com.osmb.script.herblore.data;

import com.osmb.api.item.ItemID;

public enum MixedPotion implements Potion {
    ATTACK_POTION(ItemID.ATTACK_POTION3, 25, new Ingredient(ItemID.GUAM_POTION_UNF), new Ingredient(ItemID.EYE_OF_NEWT)),
    ANTI_POISON(175, 37.5, new Ingredient(ItemID.MARRENTILL_POTION_UNF), new Ingredient(ItemID.UNICORN_HORN_DUST)),
    STRENGTH_POTION(115, 50, new Ingredient(ItemID.TARROMIN_POTION_UNF), new Ingredient(ItemID.LIMPWURT_ROOT)),
    SERUM_207(3410, 50, new Ingredient(ItemID.TARROMIN_POTION_UNF), new Ingredient(ItemID.ASHES)),
    COMPOST_POTION(6472, 60, new Ingredient(ItemID.HARRALANDER_POTION_UNF), new Ingredient(ItemID.VOLCANIC_ASH)),
    RESTORE_POTION(127, 62.5, new Ingredient(ItemID.HARRALANDER_POTION_UNF), new Ingredient(ItemID.RED_SPIDERS_EGGS)),
    ENERGY_POTION(3010, 67.5, new Ingredient(ItemID.HARRALANDER_POTION_UNF), new Ingredient(ItemID.CHOCOLATE_DUST)),
    DEFENCE_POTION(133, 75, new Ingredient(ItemID.RANARR_POTION_UNF), new Ingredient(ItemID.WHITE_BERRIES)),
    AGILITY_POTION(3034, 80, new Ingredient(ItemID.TOADFLAX_POTION_UNF), new Ingredient(ItemID.TOADS_LEGS)),
    COMBAT_POTION(9741, 84, new Ingredient(ItemID.HARRALANDER_POTION_UNF), new Ingredient(ItemID.GOAT_HORN_DUST)),
    PRAYER_POTION(139, 87.5, new Ingredient(ItemID.RANARR_POTION_UNF), new Ingredient(ItemID.SNAPE_GRASS)),
    SUPER_ATTACK(145, 100, new Ingredient(ItemID.IRIT_POTION_UNF), new Ingredient(ItemID.EYE_OF_NEWT)),
    SUPERANTIPOISON(181, 106.3, new Ingredient(ItemID.IRIT_POTION_UNF), new Ingredient(ItemID.UNICORN_HORN_DUST)),
    FISHING_POTION(151, 112.5, new Ingredient(ItemID.AVANTOE_POTION_UNF), new Ingredient(ItemID.SNAPE_GRASS)),
    SUPER_ENERGY(3018, 117.5, new Ingredient(ItemID.AVANTOE_POTION_UNF), new Ingredient(ItemID.MORT_MYRE_FUNGUS)),
    SUPER_STRENGTH(157, 125, new Ingredient(ItemID.KWUARM_POTION_UNF), new Ingredient(ItemID.LIMPWURT_ROOT)),
    SUPER_RESTORE(3026, 142.5, new Ingredient(ItemID.SNAPDRAGON_POTION_UNF), new Ingredient(ItemID.RED_SPIDERS_EGGS)),
    SANFEW_SERUM(ItemID.SANFEW_SERUM4, 160, new Ingredient(ItemID.SUPER_RESTORE4), new Ingredient(ItemID.SNAKE_WEED)),
    SUPER_DEFENCE(163, 150, new Ingredient(ItemID.CADANTINE_POTION_UNF), new Ingredient(ItemID.WHITE_BERRIES)),
    ANTIFIRE_POTION(2454, 157.5, new Ingredient(ItemID.LANTADYME_POTION_UNF), new Ingredient(ItemID.DRAGON_SCALE_DUST)),
    RANGING_POTION(169, 162.5, new Ingredient(ItemID.DWARF_WEED_POTION_UNF), new Ingredient(ItemID.WINE_OF_ZAMORAK)),
    MAGIC_POTION(3042, 172.5, new Ingredient(ItemID.LANTADYME_POTION_UNF), new Ingredient(ItemID.POTATO_CACTUS)),
    STAMINA_POTION(12625, 102, new Ingredient(ItemID.SUPER_ENERGY4), new Ingredient(ItemID.AMYLASE_CRYSTAL,1,true)),
    ZAMORAK_BREW(189, 175, new Ingredient(ItemID.TORSTOL_POTION_UNF), new Ingredient(ItemID.JANGERBERRIES)),
    BASTION_POTION(22464, 155, new Ingredient(ItemID.CADANTINE_BLOOD_POTION_UNF), new Ingredient(ItemID.WINE_OF_ZAMORAK)),
    BATTLEMAGE_POTION(22452, 155, new Ingredient(ItemID.CADANTINE_BLOOD_POTION_UNF), new Ingredient(ItemID.POTATO_CACTUS)),
    SARADOMIN_BREW(6687, 180, new Ingredient(ItemID.TOADFLAX_POTION_UNF), new Ingredient(ItemID.CRUSHED_NEST)),
    EXTENDED_ANTIFIRE(11951, 110, new Ingredient(ItemID.ANTIFIRE_POTION4), new Ingredient(ItemID.LAVA_SCALE_SHARD, 1, true)),
    ANTI_VENOM(12905, 120, new Ingredient(ItemID.ANTIDOTE4_5952), new Ingredient(ItemID.ZULRAHS_SCALES, 1, true)),
    ANTI_VENOM_PLUS(ItemID.ANTIVENOM4_12913, 125, new Ingredient(ItemID.ANTIVENOM4), new Ingredient(ItemID.TORSTOL, 1, true)),
    SUPER_ANTIFIRE_POTION(21978, 130, new Ingredient(ItemID.ANTIFIRE_POTION4), new Ingredient(ItemID.CRUSHED_SUPERIOR_DRAGON_BONES, 1, true)),
    SUPER_COMBAT_POTION(ItemID.SUPER_COMBAT_POTION4, 130, new Ingredient(ItemID.TORSTOL, 1, true), new Ingredient(ItemID.SUPER_ATTACK4), new Ingredient(ItemID.SUPER_STRENGTH4), new Ingredient(ItemID.SUPER_DEFENCE4)),
    SUPER_COMBAT_POTION_2(ItemID.SUPER_COMBAT_POTION4, 130, new Ingredient(ItemID.TORSTOL_POTION_UNF, 1, true), new Ingredient(ItemID.SUPER_ATTACK4), new Ingredient(ItemID.SUPER_STRENGTH4), new Ingredient(ItemID.SUPER_DEFENCE4));

    private final double xp;
    private final int itemId;
    private final Ingredient[] ingredients;

    MixedPotion(int itemId, double xp, Ingredient... ingredients) {
        this.xp = xp;
        this.itemId = itemId;
        this.ingredients = ingredients;
    }

    public static int[] getItemIds() {
        int[] itemIds = new int[MixedPotion.values().length];
        for (int i = 0; i < itemIds.length; i++) {
            itemIds[i] = MixedPotion.values()[i].itemId;
        }
        return itemIds;
    }
    public static MixedPotion getMixedPotionForId(int itemId) {
        for(MixedPotion mixedPotion : values()) {
            if(mixedPotion.itemId == itemId) {
                return mixedPotion;
            }
        }
        return null;
    }

    public Ingredient[] getIngredients() {
        return ingredients;
    }

    public int getItemId() {
        return itemId;
    }

    public double getXp() {
        return xp;
    }


    @Override
    public int getItemID() {
        return itemId;
    }
}
