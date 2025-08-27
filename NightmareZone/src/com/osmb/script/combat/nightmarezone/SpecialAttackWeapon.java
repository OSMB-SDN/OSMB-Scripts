package com.osmb.script.combat.nightmarezone;

import com.osmb.api.item.ItemID;

public enum SpecialAttackWeapon {
    DRAGON_DAGGER(ItemID.DRAGON_DAGGER, 25),
    DRAGON_DAGGER_PP(ItemID.DRAGON_DAGGERP_5698, 25),
    DRAGON_SCIMITAR(ItemID.DRAGON_SCIMITAR, 55),
    DRAGON_LONGSWORD(ItemID.DRAGON_LONGSWORD, 25),
    DRAGON_MACE(ItemID.DRAGON_MACE, 25),
    DRAGON_WARHAMMER(ItemID.DRAGON_WARHAMMER, 50),
    ABYSSAL_DAGGER(ItemID.ABYSSAL_DAGGER, 50),
    ABYSSAL_TENTACLE(ItemID.ABYSSAL_TENTACLE, 50),
    ARMADYL_GODSWORD(ItemID.ARMADYL_GODSWORD, 50),
    BANDOS_GODSWORD(ItemID.BANDOS_GODSWORD, 50),
    SARADOMIN_GODSWORD(ItemID.SARADOMIN_GODSWORD, 50),
    ZAMORAK_GODSWORD(ItemID.ZAMORAK_GODSWORD, 50),
    GRANITE_HAMMER(ItemID.GRANITE_HAMMER, 60),
    CRYSTAL_HALBERD(ItemID.CRYSTAL_HALBERD, 30),
    DARK_BOW(ItemID.DARK_BOW, 55),
    DRAGON_THROWNAXE(ItemID.DRAGON_THROWNAXE, 50),
    DRAGON_KNIVES(ItemID.DRAGON_KNIFE, 25),
    MAGIC_SHORTBOW_I(ItemID.MAGIC_SHORTBOW_I, 55),
    LIGHT_BALLISTA(ItemID.LIGHT_BALLISTA, 65),
    HEAVY_BALLISTA(ItemID.HEAVY_BALLISTA, 65),
    TOXIC_BLOWPIPE(ItemID.TOXIC_BLOWPIPE, 50),
    SARADOMIN_SWORD(ItemID.SARADOMIN_SWORD, 100),
    SARADOMINS_BLESSED_SWORD(ItemID.SARADOMINS_BLESSED_SWORD, 65),
    DRAGON_CLAWS(ItemID.DRAGON_CLAWS, 50),
    DRAGON_BATTLEAXE(ItemID.DRAGON_BATTLEAXE, 100);

    private final int amount;
    private final int itemID;

    SpecialAttackWeapon(int itemID, int amount) {
        this.amount = amount;
        this.itemID = itemID;
    }

    public static int getAmountForID(int itemID) {
        for (SpecialAttackWeapon specialAttackWeapon : SpecialAttackWeapon.values()) {
            if (specialAttackWeapon.itemID == itemID) return specialAttackWeapon.amount;
        }
        return -1;
    }

    public static int[] getItemIDs() {
        int[] itemIDs = new int[SpecialAttackWeapon.values().length];
        for (int i = 0; i < itemIDs.length; i++) {
            itemIDs[i] = SpecialAttackWeapon.values()[i].itemID;
        }
        return itemIDs;
    }

    public static SpecialAttackWeapon forID(int itemID) {
        for(SpecialAttackWeapon specialAttackWeapon : values()) {
            if(specialAttackWeapon.itemID == itemID) {
                return specialAttackWeapon;
            }
        }
        return null;
    }

    public int getItemID() {
        return itemID;
    }

    public int getAmount() {
        return amount;
    }
}