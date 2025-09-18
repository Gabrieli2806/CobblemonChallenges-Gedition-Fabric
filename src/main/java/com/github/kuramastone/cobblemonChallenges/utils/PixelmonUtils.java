package com.github.kuramastone.cobblemonChallenges.utils;

public class PixelmonUtils {

    public static boolean doesDaytimeMatch(long time, String phrase) {
        long normalizedTime = time % 24000;

        // Convert the phrase to lowercase to handle case-insensitive comparison
        phrase = phrase.toLowerCase();

        for(String sub : phrase.split("/")) {
            switch (sub) {
                case "any":
                    return true;  // If 'any', always return true
                case "dawn":
                    return normalizedTime >= 0 && normalizedTime < 1000;
                case "day":
                    return normalizedTime >= 1000 && normalizedTime < 12000;
                case "dusk":
                    return normalizedTime >= 12000 && normalizedTime < 13000;
                case "night":
                    return normalizedTime >= 13000 && normalizedTime < 24000;
            }
        }

        return false;
    }
}

