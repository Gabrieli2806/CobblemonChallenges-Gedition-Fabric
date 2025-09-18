package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;

public class Played1SecondEvent {
    private final PlayerProfile playerProfile;

    public Played1SecondEvent(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }
}
