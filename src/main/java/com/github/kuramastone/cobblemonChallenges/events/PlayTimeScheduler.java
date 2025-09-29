package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayTimeScheduler {

    private static long currentTick = 0;

    public static void onServerTick(MinecraftServer minecraftServer) {
        try {
            // We only want to run this on the END phase to ensure everything is processed in one tick
            currentTick++;

            // trigger every 30 seconds (600 ticks = 30 seconds at 20 ticks per second)
            if (currentTick % 600 == 0) {
                thirtySecondTick();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void thirtySecondTick() {
        for (ServerPlayer player : CobbleChallengeMod.getMinecraftServer().getPlayerList().getPlayers()) {
            PlayerProfile profile = CobbleChallengeMod.instance.getAPI().getOrCreateProfile(player.getUUID());
            ChallengeListener.on1SecondPlayed(new Played1SecondEvent(profile));
        }
    }

}
