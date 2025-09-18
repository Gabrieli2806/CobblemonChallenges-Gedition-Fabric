package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;

public class ChallengeCompletedEvent {

    private final PlayerProfile playerProfile;
    private final ChallengeList challengeList;
    private final Challenge challenge;

    public ChallengeCompletedEvent(PlayerProfile playerProfile, ChallengeList challengeList, Challenge challenge) {
        this.playerProfile = playerProfile;
        this.challengeList = challengeList;
        this.challenge = challenge;
    }

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    public ChallengeList getChallengeList() {
        return challengeList;
    }

    public Challenge getChallenge() {
        return challenge;
    }
}
