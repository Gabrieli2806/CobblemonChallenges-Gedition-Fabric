package com.github.kuramastone.cobblemonChallenges.challenges.reward;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.mojang.brigadier.StringReader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;

public class CommandReward implements Reward {

    private String command;

    public CommandReward(String command) {
        this.command = command;
    }

    @Override
    public void applyTo(Player player) {
        if(command == null)
            return;

        String cmd = command.replace("{player}", player.getName().getString());

        MinecraftServer server = CobbleChallengeMod.getMinecraftServer();

        server.getCommands()
                .performCommand(
                        server.getCommands().getDispatcher()
                                .parse(new StringReader(cmd), server.createCommandSourceStack().withSuppressedOutput()), cmd);
    }

    public String getCommand() {
        return command;
    }
}
