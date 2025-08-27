package com.osmb.script.woodcutting.powerchopburn;

import com.osmb.api.location.position.types.WorldPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Status {
    static final long BLACKLIST_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    static int logsBurntOnFire = 0;
    static int tries = 0;
    static int logsBurnt = 0;
    static boolean burning = false;
    static WorldPosition bonfirePosition;
    static boolean forceNewPosition = false;
    static final List<String> PREVIOUS_CHATBOX_LINES = new ArrayList<>();

}
