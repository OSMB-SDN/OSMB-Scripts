package com.osmb.script.runecrafting.gotr;

import com.osmb.api.location.position.types.WorldPosition;

public enum Portal {
    E("E", new WorldPosition(3627, 9503, 0)),
    SE("SE", new WorldPosition(3627, 9493, 0)),
    S("S", new WorldPosition(3615, 9487, 0)),
    SW("SW", new WorldPosition(3603, 9493, 0)),;
    private final String name;
    private final WorldPosition position;

    Portal(String name, WorldPosition position) {
        this.name = name;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public WorldPosition getPosition() {
        return position;
    }

}
