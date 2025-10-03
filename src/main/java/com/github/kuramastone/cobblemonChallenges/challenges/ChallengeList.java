package com.github.kuramastone.cobblemonChallenges.challenges;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Requirement;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ChallengeList {

    private CobbleChallengeAPI api;

    private String name;
    private List<Challenge> challengeMap; // All available challenges
    private List<Challenge> visibleChallenges; // Currently visible challenges (rotated)
    private int maxChallengesPerPlayer;
    private int visibleMissions;
    private String rotationInterval; // daily, weekly, monthly, disabled
    private long lastRotationTime;

    public ChallengeList(CobbleChallengeAPI api, String name, List<Challenge> challengeMap, int maxChallengesPerPlayer, 
                         int visibleMissions, String rotationInterval) {
        this.api = api;
        this.name = name;
        this.challengeMap = challengeMap;
        this.maxChallengesPerPlayer = maxChallengesPerPlayer;
        this.visibleMissions = visibleMissions;
        this.rotationInterval = rotationInterval;
        this.lastRotationTime = System.currentTimeMillis();
        
        // Initialize visible challenges
        this.visibleChallenges = new ArrayList<>();

        // Only rotate if not during reload/initialization
        if (!CobbleChallengeMod.preventRotationOnReload) {
            rotateVisibleChallenges();
        } else {
            // During reload, initialize with all challenges or preserve existing rotation data
            initializeVisibleChallengesDuringReload();
        }
    }

    private void initializeVisibleChallengesDuringReload() {
        // Try to load existing rotation data to preserve current missions
        if (api.loadRotationData(this)) {
            // Successfully loaded existing rotation data
            return;
        }

        // Fallback: show all challenges or first few challenges if rotation data is not available
        if (visibleMissions >= challengeMap.size()) {
            visibleChallenges = new ArrayList<>(challengeMap);
        } else {
            visibleChallenges = new ArrayList<>(challengeMap.subList(0, Math.min(visibleMissions, challengeMap.size())));
        }
    }

    public static ChallengeList load(CobbleChallengeAPI api, String challengeListID, YamlConfig section) {
        Objects.requireNonNull(section, "Cannot load data from a null section.");

        List<Challenge> challengeList = new ArrayList<>();
        for (String challengeID : section.getKeys("challenges", false)) {
            Challenge challenge = Challenge.load(challengeID, section.getSection("challenges." + challengeID));
            if(challenge == null) continue; // something went wrong, skip
            boolean valid = api.registerChallenge(challenge);
            if (valid) {
                challengeList.add(challenge);
            }
            else {
                CobbleChallengeMod.logger.error("Unable to load duplicate Challenge name '{}'. Try renaming it! Ignoring this challenge.", challengeID);
            }
        }
        int maxChallengesPerPlayer = section.get("maxChallengesPerPlayer", 1);
        int visibleMissions = section.get("visible-missions", challengeList.size()); // Default to show all if not specified
        String rotationInterval = section.get("rotation-interval", "disabled");

        return new ChallengeList(api, challengeListID, challengeList, maxChallengesPerPlayer, visibleMissions, rotationInterval);
    }

    public String getName() {
        return name;
    }

    public List<Challenge> getChallengeMap() {
        return challengeMap; // Return all challenges for internal operations
    }
    
    public List<Challenge> getVisibleChallenges() {
        checkAndPerformRotation();
        return new ArrayList<>(visibleChallenges); // Return copy to prevent modification
    }

    public int getMaxChallengesPerPlayer() {
        return maxChallengesPerPlayer;
    }

    public Challenge getChallengeAt(Integer level) {
        return challengeMap.get(level);
    }

    /**
     * Create a progression for this challenge. This can be used to keep track of a player's progress
     *
     * @param challenge
     * @param profile
     * @return
     */
    public ChallengeProgress buildNewProgressForQuest(Challenge challenge, PlayerProfile profile) {

        List<Pair<String, Progression<?>>> progs = new ArrayList<>();
        for (Requirement requirement : challenge.getRequirements()) {
            progs.add(Pair.of(requirement.getName(), requirement.buildProgression(profile)));
        }

        return new ChallengeProgress(api, profile, this, challenge, progs, System.currentTimeMillis());
    }

    public Challenge getChallenge(String challengeName) {
        for(Challenge challenge : this.challengeMap) {
            if (challenge.getName().equals(challengeName)) {
                return challenge;
            }
        }

        return null;
    }
    
    public int getVisibleMissions() {
        return visibleMissions;
    }
    
    public String getRotationInterval() {
        return rotationInterval;
    }
    
    public long getLastRotationTime() {
        return lastRotationTime;
    }

    /**
     * Get time remaining until next rotation in milliseconds
     * Returns -1 if rotation is disabled
     */
    public long getTimeUntilNextRotation() {
        if ("disabled".equals(rotationInterval)) {
            return -1;
        }

        long requiredMillis = parseRotationInterval(rotationInterval);
        if (requiredMillis == -1) {
            return -1;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceRotation = currentTime - lastRotationTime;
        long timeRemaining = requiredMillis - timeSinceRotation;

        return Math.max(0, timeRemaining);
    }

    /**
     * Get time remaining until next rotation formatted as "Xd Xh Xm"
     * Returns "Disabled" if rotation is disabled
     */
    public String getTimeUntilNextRotationFormatted() {
        long remaining = getTimeUntilNextRotation();

        if (remaining == -1) {
            return "Disabled";
        }

        if (remaining == 0) {
            return "Ready to rotate";
        }

        long days = remaining / (24 * 60 * 60 * 1000L);
        remaining %= (24 * 60 * 60 * 1000L);

        long hours = remaining / (60 * 60 * 1000L);
        remaining %= (60 * 60 * 1000L);

        long minutes = remaining / (60 * 1000L);

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            result.append(hours).append("h ");
        }
        result.append(minutes).append("m");

        return result.toString().trim();
    }
    
    /**
     * Check if rotation is needed and perform it if necessary
     */
    public void checkAndPerformRotation() {
        if ("disabled".equals(rotationInterval)) {
            return; // No rotation
        }

        // Prevent rotation during reload/initialization
        if (CobbleChallengeMod.preventRotationOnReload) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceRotation = currentTime - lastRotationTime;

        // Parse the rotation interval to milliseconds
        long requiredMillis = parseRotationInterval(rotationInterval);
        if (requiredMillis == -1) {
            return; // Invalid format, already logged error
        }

        boolean shouldRotate = timeSinceRotation >= requiredMillis;

        if (shouldRotate) {
            // Performing automatic rotation
            rotateVisibleChallenges();
            lastRotationTime = currentTime;
            // Rotation completed
            CobbleChallengeMod.logger.info("Automatic rotation performed for '{}' (interval: {})", name, rotationInterval);
        }
    }

    /**
     * Parse rotation interval string to milliseconds
     * Supports formats: "daily", "weekly", "monthly", "15d", "1d12h", "7d12h", "2h30m", etc.
     */
    private long parseRotationInterval(String interval) {
        interval = interval.toLowerCase().trim();

        // Standard intervals
        switch (interval) {
            case "daily":
                return 24 * 60 * 60 * 1000L; // 24 hours
            case "weekly":
                return 7 * 24 * 60 * 60 * 1000L; // 7 days
            case "monthly":
                return 30L * 24 * 60 * 60 * 1000L; // 30 days
        }

        // Custom interval parsing (e.g., "1d12h", "7d", "12h30m", "2h", "30m")
        try {
            long totalMillis = 0;

            // Extract days
            if (interval.contains("d")) {
                int dIndex = interval.indexOf('d');
                String daysStr = interval.substring(0, dIndex);
                totalMillis += Long.parseLong(daysStr) * 24 * 60 * 60 * 1000L;
                interval = interval.substring(dIndex + 1);
            }

            // Extract hours
            if (interval.contains("h")) {
                int hIndex = interval.indexOf('h');
                String hoursStr = interval.substring(0, hIndex);
                totalMillis += Long.parseLong(hoursStr) * 60 * 60 * 1000L;
                interval = interval.substring(hIndex + 1);
            }

            // Extract minutes
            if (interval.contains("m")) {
                int mIndex = interval.indexOf('m');
                String minutesStr = interval.substring(0, mIndex);
                totalMillis += Long.parseLong(minutesStr) * 60 * 1000L;
            }

            if (totalMillis > 0) {
                return totalMillis;
            }
        } catch (Exception e) {
            CobbleChallengeMod.logger.error("Invalid rotation interval format: '{}'. Use formats like: 'daily', 'weekly', 'monthly', '15d', '1d12h', '7d12h', '2h30m'", rotationInterval);
            return -1;
        }

        CobbleChallengeMod.logger.error("Unknown rotation interval: '{}'. Valid formats: 'daily', 'weekly', 'monthly', '15d', '1d12h', '7d12h', '2h30m'", rotationInterval);
        return -1;
    }
    
    /**
     * Rotate the visible challenges, preserving active ones
     */
    private void rotateVisibleChallenges() {
        // Rotating visible challenges

        // CANCEL ALL ACTIVE CHALLENGES FOR THIS LIST WHEN ROTATION HAPPENS
        cancelAllActiveChallengesForThisList();

        if (visibleMissions >= challengeMap.size()) {
            // Show all challenges if we have fewer challenges than visible missions
            visibleChallenges = new ArrayList<>(challengeMap);
            // Showing all challenges (no rotation needed)
            return;
        }

        // Create a list of all challenges available for rotation
        List<Challenge> availableForRotation = new ArrayList<>(challengeMap);

        // Shuffle the available challenges for random selection
        Collections.shuffle(availableForRotation, new Random());

        // Select random challenges up to the visible limit
        visibleChallenges = new ArrayList<>();
        for (int i = 0; i < visibleMissions && i < availableForRotation.size(); i++) {
            visibleChallenges.add(availableForRotation.get(i));
        }

        // Rotation completed
    }

    /**
     * Cancel ALL active challenges for ALL players in this challenge list
     */
    private void cancelAllActiveChallengesForThisList() {
        int canceledCount = 0;

        for (PlayerProfile profile : api.getProfiles()) {
            List<ChallengeProgress> toRemove = new ArrayList<>();

            for (ChallengeProgress progress : profile.getActiveChallenges()) {
                if (progress.getParentList().getName().equals(this.name)) {
                    toRemove.add(progress);
                }
            }

            // Remove all active challenges from this list for this player
            for (ChallengeProgress progress : toRemove) {
                profile.removeActiveChallenge(progress);
                canceledCount++;
            }
        }

        if (canceledCount > 0) {
            CobbleChallengeMod.logger.info("Rotation: Canceled {} active challenge(s) from challenge list '{}'", canceledCount, name);
        }
    }
    
    /**
     * Get challenges that are currently active for any player (should be preserved during rotation)
     */
    private Set<Challenge> getActiveChallengesForRotation() {
        Set<Challenge> activeChallenges = new HashSet<>();
        
        // Check all player profiles for active challenges in this list
        for (PlayerProfile profile : api.getProfiles()) {
            for (ChallengeProgress progress : profile.getActiveChallenges()) {
                if (progress.getParentList().getName().equals(this.name)) {
                    activeChallenges.add(progress.getActiveChallenge());
                }
            }
        }
        
        return activeChallenges;
    }
    
    /**
     * Force a rotation of visible challenges
     */
    public void forceRotation() {
        // Force rotating challenge list
        rotateVisibleChallenges();
        lastRotationTime = System.currentTimeMillis();
        // Force rotation completed
    }
    
    /**
     * Check with testing intervals (shorter times for debugging)
     * Daily = 2 minutes, Weekly = 5 minutes, Monthly = 10 minutes
     */
    public void checkAndPerformRotationTesting() {
        if ("disabled".equals(rotationInterval)) {
            // CobbleChallengeMod.logger.debug("Rotation disabled for challenge list: {}", name);
            return;
        }

        // Prevent rotation during reload/initialization
        if (CobbleChallengeMod.preventRotationOnReload) {
            // CobbleChallengeMod.logger.debug("Rotation temporarily disabled during reload for challenge list: {}", name);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceRotation = currentTime - lastRotationTime;
        
        boolean shouldRotate = false;
        long requiredMillis = 0;
        
        // Check if it's a custom interval (like "15d" for 15 days)
        if (rotationInterval.toLowerCase().endsWith("d")) {
            try {
                // Parse custom days (e.g., "15d" -> 15 days)
                String daysString = rotationInterval.toLowerCase().replace("d", "");
                int customDays = Integer.parseInt(daysString);
                // For testing: 1 second per day (so 15d = 15 seconds)
                requiredMillis = customDays * 1000L;
                shouldRotate = timeSinceRotation >= requiredMillis;
                // CobbleChallengeMod.logger.debug("TESTING Custom rotation interval: {} days ({} seconds)", customDays, customDays);
            } catch (NumberFormatException e) {
                CobbleChallengeMod.logger.error("Invalid custom rotation interval format: '{}'. Use format like '15d' for 15 days", rotationInterval);
                return; // Skip rotation if format is invalid
            }
        } else {
            // Standard intervals
            switch (rotationInterval.toLowerCase()) {
                case "daily":
                    requiredMillis = 2 * 60 * 1000L; // 2 minutes for testing
                    shouldRotate = timeSinceRotation >= requiredMillis;
                    break;
                case "weekly":
                    requiredMillis = 5 * 60 * 1000L; // 5 minutes for testing
                    shouldRotate = timeSinceRotation >= requiredMillis;
                    break;
                case "monthly":
                    requiredMillis = 10 * 60 * 1000L; // 10 minutes for testing
                    shouldRotate = timeSinceRotation >= requiredMillis;
                    break;
                default:
                    CobbleChallengeMod.logger.error("Unknown rotation interval: '{}'. Valid options: daily, weekly, monthly, disabled, or custom format like '15d'", rotationInterval);
                    return;
            }
        }
        
        long secondsSinceRotation = timeSinceRotation / 1000L;
        long requiredSeconds = requiredMillis / 1000L;
        
        // CobbleChallengeMod.logger.debug("TESTING Rotation check for '{}': {} seconds since last rotation, {} seconds required for {} rotation. Should rotate: {}", 
//  // name, secondsSinceRotation, requiredSeconds, rotationInterval, shouldRotate);
        
        if (shouldRotate) {
            // Performing testing rotation
            rotateVisibleChallenges();
            lastRotationTime = currentTime;
            // Testing rotation completed
        }
    }
    
    /**
     * Set the last rotation time (used when loading from config)
     */
    public void setLastRotationTime(long lastRotationTime) {
        this.lastRotationTime = lastRotationTime;
    }
    
    /**
     * Load visible challenges from saved data
     */
    public boolean loadVisibleChallenges(List<String> visibleChallengeNames) {
        visibleChallenges.clear();

        for (String challengeName : visibleChallengeNames) {
            Challenge challenge = getChallenge(challengeName);
            if (challenge != null) {
                visibleChallenges.add(challenge);
            }
        }

        // If we couldn't load any visible challenges, perform a rotation (only if not during reload)
        if (visibleChallenges.isEmpty()) {
            if (!CobbleChallengeMod.preventRotationOnReload) {
                rotateVisibleChallenges();
            }
            return false; // Couldn't load saved challenges
        }
        return true; // Successfully loaded saved challenges
    }
}
