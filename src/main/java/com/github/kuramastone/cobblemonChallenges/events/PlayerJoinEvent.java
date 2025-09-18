package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.player.Player;

public class PlayerJoinEvent {

    private Player player;

    public PlayerJoinEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public static void register() {
        // Register the block break callback
        ServerPlayConnectionEvents.JOIN.register(PlayerJoinEvent::trigger);
    }

    private static void trigger(ServerGamePacketListenerImpl serverGamePacketListener, PacketSender packetSender, MinecraftServer minecraftServer) {
        ChallengeListener.onPlayerJoin(new PlayerJoinEvent(serverGamePacketListener.getPlayer()));
    }

}
