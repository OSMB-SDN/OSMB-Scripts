package com.osmb.script.chartercrafting;

public enum Method {
    SUPER_GLASS_MAKE("Super glass make"),
    BUY_AND_FURNACE_CRAFT("Buy & smelt"),
    BUY_AND_BANK("Buy & bank");

    private final String name;
    Method(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return name;
    }
}
