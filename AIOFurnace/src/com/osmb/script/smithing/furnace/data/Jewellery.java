package com.osmb.script.smithing.furnace.data;

import com.osmb.api.item.ItemID;
import com.osmb.script.smithing.furnace.Product;
import com.osmb.script.smithing.furnace.Resource;

import java.util.List;

public enum Jewellery implements Product {
    OPAL_RING(ItemID.OPAL_RING, "Opal ring", List.of(new Resource(ItemID.OPAL, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.RING),
    OPAL_NECKLACE(ItemID.OPAL_NECKLACE, "Opal necklace", List.of(new Resource(ItemID.OPAL, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.NECKLACE),
    OPAL_BRACELET(ItemID.OPAL_BRACELET, "Opal bracelet", List.of(new Resource(ItemID.OPAL, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.BRACELET),
    OPAL_AMULET(ItemID.OPAL_AMULET_U, "Opal amulet", List.of(new Resource(ItemID.OPAL, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.AMULET),

    JADE_RING(ItemID.JADE_RING, "Jade ring", List.of(new Resource(ItemID.JADE, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.RING),
    JADE_NECKLACE(ItemID.JADE_NECKLACE, "Jade necklace", List.of(new Resource(ItemID.JADE, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.NECKLACE),
    JADE_BRACELET(ItemID.JADE_BRACELET, "Jade bracelet", List.of(new Resource(ItemID.JADE, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.BRACELET),
    JADE_AMULET(ItemID.JADE_AMULET_U, "Jade amulet", List.of(new Resource(ItemID.JADE, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.AMULET),

    TOPAZ_RING(ItemID.TOPAZ_RING, "Topaz ring", List.of(new Resource(ItemID.RED_TOPAZ, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.RING),
    TOPAZ_NECKLACE(ItemID.TOPAZ_NECKLACE, "Topaz necklace", List.of(new Resource(ItemID.RED_TOPAZ, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.NECKLACE),
    TOPAZ_BRACELET(ItemID.TOPAZ_BRACELET, "Topaz bracelet", List.of(new Resource(ItemID.RED_TOPAZ, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.BRACELET),
    TOPAZ_AMULET(ItemID.TOPAZ_AMULET_U, "Topaz amulet", List.of(new Resource(ItemID.RED_TOPAZ, 1), new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.AMULET),

    TIARA(ItemID.TIARA, "Tiara", List.of(new Resource(ItemID.SILVER_BAR, 1)), JewelleryType.TIARA),
    GOLDEN_TIARA(ItemID.GOLD_TIARA, "Gold tiara", List.of(new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.TIARA),

    GOLD_RING(ItemID.GOLD_RING, "Gold ring", List.of(new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.RING),
    GOLD_NECKLACE(ItemID.GOLD_NECKLACE, "Gold necklace", List.of(new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.NECKLACE),
    GOLD_BRACELET(ItemID.GOLD_BRACELET, "Gold bracelet", List.of(new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.BRACELET),
    GOLD_AMULET(ItemID.GOLD_AMULET_U, "Gold amulet", List.of(new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.AMULET),

    SAPPHIRE_RING(ItemID.SAPPHIRE_RING, "Sapphire ring", List.of(new Resource(ItemID.SAPPHIRE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.RING),
    SAPPHIRE_NECKLACE(ItemID.SAPPHIRE_NECKLACE, "Sapphire necklace", List.of(new Resource(ItemID.SAPPHIRE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.NECKLACE),
    SAPPHIRE_BRACELET(ItemID.SAPPHIRE_BRACELET, "Sapphire bracelet", List.of(new Resource(ItemID.SAPPHIRE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.BRACELET),
    SAPPHIRE_AMULET(ItemID.SAPPHIRE_AMULET_U, "Sapphire amulet", List.of(new Resource(ItemID.SAPPHIRE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.AMULET),

    EMERALD_RING(ItemID.EMERALD_RING, "Emerald ring", List.of(new Resource(ItemID.EMERALD, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.RING),
    EMERALD_NECKLACE(ItemID.EMERALD_NECKLACE, "Emerald necklace", List.of(new Resource(ItemID.EMERALD, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.NECKLACE),
    EMERALD_BRACELET(ItemID.EMERALD_BRACELET, "Emerald bracelet", List.of(new Resource(ItemID.EMERALD, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.BRACELET),
    EMERALD_AMULET(ItemID.EMERALD_AMULET_U, "Emerald amulet", List.of(new Resource(ItemID.EMERALD, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.AMULET),

    RUBY_RING(ItemID.RUBY_RING, "Ruby ring", List.of(new Resource(ItemID.RUBY, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.RING),
    RUBY_NECKLACE(ItemID.RUBY_NECKLACE, "Ruby necklace", List.of(new Resource(ItemID.RUBY, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.NECKLACE),
    RUBY_BRACELET(ItemID.RUBY_BRACELET, "Ruby bracelet", List.of(new Resource(ItemID.RUBY, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.BRACELET),
    RUBY_AMULET(ItemID.RUBY_AMULET_U, "Ruby amulet", List.of(new Resource(ItemID.RUBY, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.AMULET),

    DIAMOND_RING(ItemID.DIAMOND_RING, "Diamond ring", List.of(new Resource(ItemID.DIAMOND, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.RING),
    DIAMOND_NECKLACE(ItemID.DIAMOND_NECKLACE, "Diamond necklace", List.of(new Resource(ItemID.DIAMOND, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.NECKLACE),
    DIAMONG_BRACELET(ItemID.DIAMOND_BRACELET, "Diamond bracelet", List.of(new Resource(ItemID.DIAMOND, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.BRACELET),
    DIAMOND_AMULET(ItemID.DIAMOND_AMULET_U, "Diamond amulet", List.of(new Resource(ItemID.DIAMOND, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.AMULET),

    DRAGONSTONE_RING(ItemID.DRAGONSTONE_RING, "Dragonstone ring", List.of(new Resource(ItemID.DRAGONSTONE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.RING),
    DRAGONSTONE_NECKLACE(ItemID.DRAGON_NECKLACE, "Dragonstone necklace", List.of(new Resource(ItemID.DRAGONSTONE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.NECKLACE),
    DRAGONSTONE_BRACELET(ItemID.DRAGONSTONE_BRACELET, "Dragonstone bracelet", List.of(new Resource(ItemID.DRAGONSTONE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.BRACELET),
    DRAGONSTONE_AMULET(ItemID.DRAGONSTONE_AMULET_U, "Dragonstone amulet", List.of(new Resource(ItemID.DRAGONSTONE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.AMULET),

    ONYX_RING(ItemID.ONYX_RING, "Onyx ring", List.of(new Resource(ItemID.ONYX, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.RING),
    ONYX_NECKLACE(ItemID.ONYX_NECKLACE, "Onyx necklace", List.of(new Resource(ItemID.ONYX, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.NECKLACE),
    ONYX_BRACELET(ItemID.ONYX_BRACELET, "Onyx bracelet", List.of(new Resource(ItemID.ONYX, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.BRACELET),
    ONYX_AMULET(ItemID.ONYX_AMULET_U, "Onyx amulet", List.of(new Resource(ItemID.ONYX, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.AMULET),

    ZENYTE_RING(ItemID.ZENYTE_RING, "Zenyte ring", List.of(new Resource(ItemID.ZENYTE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.RING),
    ZENYTE_NECKLACE(ItemID.ZENYTE_NECKLACE, "Zenyte necklace", List.of(new Resource(ItemID.ZENYTE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.NECKLACE),
    ZENYTE_BRACELET(ItemID.ZENYTE_BRACELET, "Zenyte bracelet", List.of(new Resource(ItemID.ZENYTE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.BRACELET),
    ZENYTE_AMULET(ItemID.ZENYTE_AMULET_U, "Zenyte amulet", List.of(new Resource(ItemID.ZENYTE, 1), new Resource(ItemID.GOLD_BAR, 1)), JewelleryType.AMULET);

    private final int itemID;
    private final JewelleryType type;
    private final String productName;
    private final List<Resource> resources;

    Jewellery(int itemID, String productName, List<Resource> resources, JewelleryType type) {
        this.itemID = itemID;
        this.productName = productName;
        this.resources = resources;
        this.type = type;
    }

    @Override
    public int getItemID() {
        return itemID;
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public List<Integer> getMouldIDs() {
        return List.of(getMouldItem());
    }

    public Integer getMouldItem() {
        return switch (type) {
            case RING -> ItemID.RING_MOULD;
            case NECKLACE -> ItemID.NECKLACE_MOULD;
            case BRACELET -> ItemID.BRACELET_MOULD;
            case AMULET -> ItemID.AMULET_MOULD;
            case TIARA -> ItemID.TIARA_MOULD;
        };
    }

    @Override
    public List<Resource> getResources() {
        return resources;
    }

    enum JewelleryType {
        RING,
        NECKLACE,
        BRACELET,
        AMULET,
        TIARA
    }
}
