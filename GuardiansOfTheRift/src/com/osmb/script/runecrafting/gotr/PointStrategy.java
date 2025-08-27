package com.osmb.script.runecrafting.gotr;

public enum PointStrategy {
    BALANCED("Balanced", "The script will try to maintain a balance between points."),
    MAXIMUM_POINTS("Maximum Points", "The script will gain maximum points by focusing the highest tier rune available."),
    CATALYTIC("Catalytic", "The script will focus Catalytic runes where possible."),
    ELEMENTAL("Elemental", "The script will focus Elemental runes where possible.");

    private final String name;
    private final String description;

    PointStrategy(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

}