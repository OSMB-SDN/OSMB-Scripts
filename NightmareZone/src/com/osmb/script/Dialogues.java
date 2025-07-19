package com.osmb.script;

import java.util.Arrays;
import java.util.List;

public class Dialogues {
    static final List<String> DOMINIC_SUCCESS_CHAT_DIALOGUES = Arrays.asList(
            "I've already created a dream for you Do you want me to cancel it?",
            "I've prepared your dream. Step into the enclosure invite up to 4 players to join you, then drink from the vial to begin. Each of you will need to unlock the coffer and have 26,000 coins deposited first"
    );
    // Dialogue Strings
    static final List<String> DOMINIC_CHAT_DIALOGUES = Arrays.asList(
            "You haven't started that dream I created for you. Can I help you with something?",
            "Welcome to the Nightmare Zone! Would you like me to create a dream for you?"
    );
    static final NightmareZone.DialogueOption[] DOMINIC_DIALOGUE_OPTIONS = new NightmareZone.DialogueOption[]{
            new NightmareZone.DialogueOption("which dream would you like to experience?", "Previous:"),
            new NightmareZone.DialogueOption("Agree to pay", "Yes")
    };
}
