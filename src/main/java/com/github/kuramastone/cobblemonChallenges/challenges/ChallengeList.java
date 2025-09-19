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
     * Check if rotation is needed and perform it if necessary
     */
    public void checkAndPerformRotation() {
        if ("disabled".equals(rotationInterval)) {
            // CobbleChallengeMod.logger.debug("Rotation disabled for challenge list: {}", name);
            return; // No rotation
        }

        // Prevent rotation during reload/initialization
        if (CobbleChallengeMod.preventRotationOnReload) {
            // CobbleChallengeMod.logger.debug("Rotation temporarily disabled during reload for challenge list: {}", name);
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceRotation = currentTime - lastRotationTime;
        
        // Convert to hours for easier reading
        long hoursSinceRotation = timeSinceRotation / (60 * 60 * 1000L);
        
        boolean shouldRotate = false;
        long requiredHours = 0;
        
        // Check if it's a custom interval (like "15d" for 15 days)
        if (rotationInterval.toLowerCase().endsWith("d")) {
            try {
                // Parse custom days (e.g., "15d" -> 15 days)
                String daysString = rotationInterval.toLowerCase().replace("d", "");
                int customDays = Integer.parseInt(daysString);
                requiredHours = customDays * 24;
                shouldRotate = timeSinceRotation >= customDays * 24L * 60 * 60 * 1000L;
                // CobbleChallengeMod.logger.debug("Custom rotation interval: {} days ({} hours)", customDays, requiredHours);
            } catch (NumberFormatException e) {
                CobbleChallengeMod.logger.error("Invalid custom rotation interval format: '{}'. Use format like '15d' for 15 days", rotationInterval);
                return; // Skip rotation if format is invalid
            }
        } else {
            // Standard intervals
            switch (rotationInterval.toLowerCase()) {
                case "daily":
                    requiredHours = 24;
                    shouldRotate = timeSinceRotation >= 24 * 60 * 60 * 1000L; // 24 hours
                    break;
                case "weekly":
                    requiredHours = 7 * 24;
                    shouldRotate = timeSinceRotation >= 7 * 24 * 60 * 60 * 1000L; // 7 days
                    break;
                case "monthly":
                    requiredHours = 30 * 24;
                    shouldRotate = timeSinceRotation >= 30L * 24 * 60 * 60 * 1000L; // 30 days
                    break;
                default:
                    CobbleChallengeMod.logger.error("Unknown rotation interval: '{}'. Valid options: daily, weekly, monthly, disabled, or custom format like '15d'", rotationInterval);
                    return;
            }
        }
        
        // CobbleChallengeMod.logger.debug("Rotation check for '{}': {} hours since last rotation, {} hours required for {} rotation. Should rotate: {}", 
//  // name, hoursSinceRotation, requiredHours, rotationInterval, shouldRotate);
        
        if (shouldRotate) {
            // Performing automatic rotation
            rotateVisibleChallenges();
            lastRotationTime = currentTime;
            // Rotation completed
        }
    }
    
    /**
     * Rotate the visible challenges, preserving active ones
     */
    private void rotateVisibleChallenges() {
        // Rotating visible challenges
        
        if (visibleMissions >= challengeMap.size()) {
            // Show all challenges if we have fewer challenges than visible missions
            visibleChallenges = new ArrayList<>(challengeMap);
            // Showing all challenges (no rotation needed)
            return;
        }
        
        // Get active challenges that should be preserved
        Set<Challenge> activeChallenges = getActiveChallengesForRotation();
        // Found active challenges to preserve
        
        // Create a list of challenges that can be rotated (exclude active ones)
        List<Challenge> availableForRotation = new ArrayList<>();
        for (Challenge challenge : challengeMap) {
            if (!activeChallenges.contains(challenge)) {
                availableForRotation.add(challenge);
            }
        }
        
        // Available challenges for rotation
        
        // Shuffle the available challenges for random selection
        Collections.shuffle(availableForRotation, new Random());
        
        // Start with active challenges
        visibleChallenges = new ArrayList<>(activeChallenges);
        
        // Add random challenges up to the visible limit
        int remainingSlots = visibleMissions - activeChallenges.size();
        for (int i = 0; i < remainingSlots && i < availableForRotation.size(); i++) {
            visibleChallenges.add(availableForRotation.get(i));
        }
        
        // Rotation complete
        
        // Rotation completed
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
