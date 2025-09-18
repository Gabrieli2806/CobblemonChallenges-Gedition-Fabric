package com.github.kuramastone.cobblemonChallenges.player;

import com.cobblemon.mod.common.Cobblemon;
import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.CompletedChallenge;
import com.github.kuramastone.cobblemonChallenges.challenges.reward.Reward;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.core.appender.rewrite.RewriteAppender;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlayerProfile {

    private CobbleChallengeAPI api;
    private MinecraftServer server;
    private UUID uuid;

    private @Nullable ServerPlayer playerEntity;
    private Map<String, List<ChallengeProgress>> activeChallenges; // active challenges per list
    private List<CompletedChallenge> completedChallenges;
    private List<Reward> rewardsToGive;

    public PlayerProfile(CobbleChallengeAPI api, UUID uuid) {
        this.api = api;
        this.uuid = uuid;

        activeChallenges = Collections.synchronizedMap(new HashMap<>());
        completedChallenges = Collections.synchronizedList(new ArrayList<>());
        rewardsToGive = Collections.synchronizedList(new ArrayList<>());

        server = CobbleChallengeMod.getMinecraftServer();
        syncPlayer(); // try syncing player object
    }

    public boolean isOnline() {
        syncPlayer();
        return playerEntity != null;
    }

    public void setCompletedChallenges(List<CompletedChallenge> completedChallenges) {
        this.completedChallenges = completedChallenges;
    }

    public void syncPlayer() {
        playerEntity = server.getPlayerList().getPlayer(uuid);
    }

    public UUID getUUID() {
        return uuid;
    }

    public Set<ChallengeProgress> getActiveChallenges() {
        Set<ChallengeProgress> set = new HashSet<>();

        for (List<ChallengeProgress> value : activeChallenges.values()) {
            set.addAll(value);
        }

        return set;
    }

    public Map<String, List<ChallengeProgress>> getActiveChallengesMap() {
        return activeChallenges;
    }

    /**
     * Challenges that dont require selection are added to player's progression if they dont exist
     */
    public void addUnrestrictedChallenges() {

        for (ChallengeList challengeList : new ArrayList<>(api.getChallengeLists())) {
            for (Challenge challenge : new ArrayList<>(challengeList.getChallengeMap())) {
                if (!challenge.doesNeedSelection()) {
                    // Always-active challenges should be added if not completed or if repeatable and cooldown expired
                    boolean shouldAdd = false;
                    
                    if (!isChallengeCompleted(challenge.getName()) && !isChallengeInProgress(challenge.getName())) {
                        shouldAdd = true;
                    } else if (challenge.isRepeatable() && !isChallengeInProgress(challenge.getName())) {
                        // Check if the challenge cooldown has expired for repeatable always-active challenges
                        CompletedChallenge completedChallenge = getCompletedChallenge(challenge.getName());
                        if (completedChallenge != null) {
                            long timeSinceCompleted = System.currentTimeMillis() - completedChallenge.timeCompleted();
                            if (timeSinceCompleted >= challenge.getRepeatableEveryMilliseconds()) {
                                shouldAdd = true;
                            }
                        }
                    }
                    
                    if (shouldAdd) {
                        // CobbleChallengeMod.logger.debug("Adding always-active challenge: {} to list: {}", challenge.getName(), challengeList.getName());
                        addActiveChallenge(challengeList, challenge);
                    } else {
                        // CobbleChallengeMod.logger.debug("Skipping always-active challenge: {} (completed: {}, inProgress: {})", 
// challenge.getName(), isChallengeCompleted(challenge.getName()), isChallengeInProgress(challenge.getName()));
                    }
                }
            }
            checkCompletion(challengeList);
        }

    }

    public void addActiveChallenge(ChallengeList list, Challenge challenge) {
        // CobbleChallengeMod.logger.info("Adding active challenge: {} to list: {} for player: {}", 
// challenge.getName(), list.getName(), getUUID());
        
        if (!list.getChallengeMap().contains(challenge)) {
            CobbleChallengeMod.logger.error("Challenge '{}' not found in challenge list '{}' - possibly due to rotation. Skipping addition.", challenge.getName(), list.getName());
            sendAdventureMessage(api.getMiniMessage("challenges.failure.challenge-no-longer-available", "{challenge}", challenge.getName()));
            return;
        }

        List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(list.getName(), (key) -> new ArrayList<>());
        
        // CobbleChallengeMod.logger.info("Current progress in list '{}': {} challenges, max allowed: {}", 
// list.getName(), progressInList.size(), list.getMaxChallengesPerPlayer());
        
        // Handle max limit logic - prioritize manually selected challenges over always-active ones
        if (progressInList.size() >= list.getMaxChallengesPerPlayer()) {
            if (challenge.doesNeedSelection()) {
                // If adding a manually selected challenge, remove always-active challenges first
                boolean removedAlwaysActive = false;
                for (int i = progressInList.size() - 1; i >= 0; i--) {
                    ChallengeProgress existing = progressInList.get(i);
                    if (!existing.getActiveChallenge().doesNeedSelection()) {
                        // CobbleChallengeMod.logger.info("Removing always-active challenge '{}' to make room for selected challenge '{}'", 
// existing.getActiveChallenge().getName(), challenge.getName());
                        progressInList.remove(i);
                        removedAlwaysActive = true;
                        break;
                    }
                }
                
                // If no always-active to remove, remove oldest selected challenge
                if (!removedAlwaysActive && progressInList.size() >= list.getMaxChallengesPerPlayer()) {
                    // CobbleChallengeMod.logger.info("Removing oldest selected challenge to make room for new selected challenge '{}'", challenge.getName());
                    progressInList.removeFirst();
                }
            } else {
                // If adding always-active, don't remove selected challenges - just skip
                if (hasSelectedChallengeInList(progressInList)) {
                    // CobbleChallengeMod.logger.info("Skipping always-active challenge '{}' - player has selected challenges in this category", challenge.getName());
                    return;
                } else {
                    // Only remove always-active if no selected challenges
                    // CobbleChallengeMod.logger.info("Removing always-active challenge to make room for new always-active '{}'", challenge.getName());
                    progressInList.removeFirst();
                }
            }
        }

        ChallengeProgress newProgress = list.buildNewProgressForQuest(challenge, this);
        progressInList.add(newProgress);
        
        // CobbleChallengeMod.logger.debug("Successfully added challenge '{}' to active challenges. Total active: {}", 
// challenge.getName(), getActiveChallenges().size());
    }

    private int getChallengesInProgress() {
        int count = 0;
        for (List<ChallengeProgress> cpList : this.activeChallenges.values()) {
            for (ChallengeProgress challengeProgress : cpList) {
                if (challengeProgress.getActiveChallenge().doesNeedSelection()) {
                    count++;
                }
            }
        }
        return count;
    }

    private boolean hasSelectedChallengeInList(List<ChallengeProgress> progressList) {
        for (ChallengeProgress progress : progressList) {
            if (progress.getActiveChallenge().doesNeedSelection()) {
                return true;
            }
        }
        return false;
    }

    public void addActiveChallenge(ChallengeProgress cp) {
        List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(cp.getParentList().getName(), (key) -> new ArrayList<>());
        // only add if they dont have it already
        if (progressInList.stream().noneMatch(it -> it.getActiveChallenge().getName().equalsIgnoreCase(cp.getActiveChallenge().getName())))
            progressInList.add(cp);
    }

    public ServerPlayer getPlayerEntity() {
        if (playerEntity == null || playerEntity.isRemoved()) {
            syncPlayer();
        }

        return playerEntity;
    }

    public List<Reward> getRewardsToGive() {
        return rewardsToGive;
    }

    public void completeChallenge(ChallengeList list, Challenge challenge) {
        //double check that it isnt already completed
        if (isChallengeCompleted(challenge.getName()))
            return;

        rewardsToGive.addAll(challenge.getRewards());

        dispenseRewards();
        sendAdventureMessage(api.getMiniMessage("challenges.completed", "{challenge}", challenge.getName(), "{challenge-description}", challenge.getDescription()));
        // CobbleChallengeMod.logger.info("{} has completed the {} challenge!",
                // isOnline() ? playerEntity.getName().getString() : uuid.toString(),
                // challenge.getName());

        addCompletedChallenge(list, challenge);
    }

    private void dispenseRewards() {
        syncPlayer();
        if (playerEntity != null) {
            for (Reward reward : rewardsToGive) {
                try {
                    if (reward != null)
                        reward.applyTo(playerEntity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            rewardsToGive.clear();
        }
    }

    public void sendMessage(Collection<String> text) {
        for (String s : text) {
            sendMessage(s);
        }
    }

    public void sendMessage(String text, Object... replacements) {
        if (isOnline()) {
            ComponentEditor editor = new ComponentEditor(text);
            for (int i = 0; i < replacements.length; i += 2) {
                editor.replace(replacements[i].toString(), replacements[i + 1].toString());
            }
            playerEntity.displayClientMessage(FabricAdapter.adapt(editor.build()), false);
        }
    }

    public void sendMessage(Component comp) {
        if (isOnline()) {
            playerEntity.displayClientMessage(FabricAdapter.adapt(comp), false);
        }
    }

    public void sendAdventureMessage(net.kyori.adventure.text.Component adventureComponent) {
        if (isOnline()) {
            // ServerPlayer implements Audience directly with Adventure Platform Fabric
            ((net.kyori.adventure.audience.Audience) playerEntity).sendMessage(adventureComponent);
        }
    }

    public void removeActiveChallenge(ChallengeProgress challengeProgress) {
        String challengeName = challengeProgress.getActiveChallenge().getName();
        String listName = challengeProgress.getParentList().getName();
        
        // CobbleChallengeMod.logger.warn("REMOVING ACTIVE CHALLENGE: {} from list: {} for player: {} - STACK TRACE:", 
// challengeName, listName, getUUID());
        
        List<ChallengeProgress> progressList = activeChallenges.get(listName);
        if (progressList != null) {
            boolean removed = progressList.remove(challengeProgress);
            // CobbleChallengeMod.logger.warn("Challenge removal result: {} - Remaining challenges in list: {}", 
// removed, progressList.size());
        } else {
            CobbleChallengeMod.logger.error("No progress list found for challenge list: {}", listName);
        }
    }

    public boolean isChallengeCompleted(String challengeID) {
        for (CompletedChallenge completedChallenge : this.completedChallenges) {
            if (completedChallenge.challengeID().equalsIgnoreCase(challengeID)) {
                return true;
            }
        }
        return false;
    }

    public CompletedChallenge getCompletedChallenge(String challengeID) {
        for (CompletedChallenge completedChallenge : this.completedChallenges) {
            if (completedChallenge.challengeID().equalsIgnoreCase(challengeID)) {
                return completedChallenge;
            }
        }
        return null;
    }

    public void addCompletedChallenge(ChallengeList list, Challenge challenge) {
        if (!isChallengeCompleted(challenge.getName())) {
            completedChallenges.add(new CompletedChallenge(list.getName(), challenge.getName(), System.currentTimeMillis()));
        }
    }

    public boolean isChallengeInProgress(String challengeName) {
        ChallengeProgress progress = getActiveChallengeProgress(challengeName);
        boolean inProgress = progress != null;
        
        // CobbleChallengeMod.logger.info("Challenge '{}' in progress check for player {}: {} (total active: {})", 
// challengeName, getUUID(), inProgress, getActiveChallenges().size());
        
        return inProgress;
    }

    public ChallengeProgress getActiveChallengeProgress(String challengeName) {
        for (ChallengeProgress challenge : getActiveChallenges()) {
            if (challenge.getActiveChallenge().getName().equals(challengeName)) {
                return challenge;
            }
        }
        return null;
    }

    public List<CompletedChallenge> getCompletedChallenges() {
        return completedChallenges;
    }

    private volatile boolean isCheckingCompletion = false; // Prevent infinite loops
    
    public void checkCompletion(ChallengeList challengeList) {
        // Prevent recursive calls that cause infinite loops
        if (isCheckingCompletion) {
            // CobbleChallengeMod.logger.debug("checkCompletion already in progress for list '{}' - skipping to prevent infinite loop", challengeList.getName());
            return;
        }
        
        try {
            isCheckingCompletion = true;
            
            List<ChallengeProgress> progressInList = this.activeChallenges.computeIfAbsent(challengeList.getName(), (key) -> new ArrayList<>());
            // CobbleChallengeMod.logger.debug("checkCompletion for list '{}' - Processing {} challenges for player {}", 
// challengeList.getName(), progressInList.size(), getUUID());
                
            // Only auto-progress always-active challenges (those that don't need selection)
            for (ChallengeProgress cp : new ArrayList<>(progressInList)) {
                Challenge challenge = cp.getActiveChallenge();
                // CobbleChallengeMod.logger.debug("Checking completion for challenge: '{}' - needsSelection: {}", 
// challenge.getName(), challenge.doesNeedSelection());
                    
                // Only auto-progress challenges that don't need selection (always-active challenges)
                if (!challenge.doesNeedSelection()) {
                    // CobbleChallengeMod.logger.debug("Auto-progressing always-active challenge: '{}'", challenge.getName());
                    
                    // Check if challenge is already completed to prevent unnecessary processing
                    if (cp.isCompleted()) {
                        // CobbleChallengeMod.logger.debug("Challenge '{}' already completed - triggering completion", challenge.getName());
                        cp.completedActiveChallenge();
                    } else {
                        cp.progress(null);
                    }
                } else {
                    // CobbleChallengeMod.logger.debug("Skipping auto-progress for selection-based challenge: '{}' - player must complete manually", challenge.getName());
                }
            }
            
            // CobbleChallengeMod.logger.debug("checkCompletion COMPLETE for list '{}' - Remaining challenges: {}", 
// challengeList.getName(), progressInList.size());
                
        } finally {
            isCheckingCompletion = false;
        }
    }

    /**
     * Remove {@link CompletedChallenge}s if they are repeatable and the repeat time has been reached
     */
    public void refreshRepeatableChallenges() {
        for (CompletedChallenge data : new ArrayList<>(completedChallenges)) {
            ChallengeList challengeList = api.getChallengeList(data.challengeListID());
            if (challengeList != null) {
                Challenge challenge = challengeList.getChallenge(data.challengeID());
                if (challenge != null) {
                    if (challenge.isRepeatable()) {
                        long timeSinceCompleted = System.currentTimeMillis() - data.timeCompleted();
                        if (timeSinceCompleted >= challenge.getRepeatableEveryMilliseconds()) {
                            completedChallenges.remove(data);
                        }
                    }
                }
            }

        }
    }

    public void resetChallenges() {
        resetProgress();
        completedChallenges.clear();
        rewardsToGive.clear();

        addUnrestrictedChallenges();
    }

    private void resetProgress() {
        activeChallenges.clear();
    }
}
