package com.osmb.script;

import com.osmb.api.location.position.types.WorldPosition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {

    public static Integer extractNumberFromBrackets(String text) {
        if (text == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("\\((\\d+)\\)");
        Matcher matcher = pattern.matcher(text);

        try {
            if (matcher.find()) {
                String numberStr = matcher.group(1);
                return Integer.parseInt(numberStr);
            }
            return null;
        } catch (NumberFormatException e) {
            // This theoretically shouldn't happen due to the regex,
            // but it's good practice to handle it
            return null;
        } catch (IllegalStateException | IndexOutOfBoundsException e) {
            // Handle potential matcher errors
            return null;
        }
    }
}
