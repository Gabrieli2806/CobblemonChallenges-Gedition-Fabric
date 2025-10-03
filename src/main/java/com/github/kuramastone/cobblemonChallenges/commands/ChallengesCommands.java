package com.github.kuramastone.cobblemonChallenges.commands;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeListGUI;
import com.github.kuramastone.cobblemonChallenges.guis.ChallengeMenuGUI;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import com.github.kuramastone.cobblemonChallenges.utils.PermissionUtils;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import revxrsal.commands.annotation.*;
import revxrsal.commands.fabric.actor.FabricCommandActor;
import revxrsal.commands.fabric.annotation.CommandPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Command("challenges")
public class ChallengesCommands {

    private final CobbleChallengeAPI api;

    public ChallengesCommands() {
        api = CobbleChallengeMod.instance.getAPI();
    }

    @Subcommand("restart")
    @CommandPermission("challenges.commands.restart")
    public void handleRestartCommand(FabricCommandActor actor, ServerPlayer player) throws CommandSyntaxException {
        PlayerProfile profile = CobbleChallengeMod.instance.getAPI().getOrCreateProfile(player.getUUID());

        profile.resetChallenges();
        profile.addUnrestrictedChallenges();

        actor.sendRawMessage(FabricAdapter.adapt(api.getMessage("commands.restart")));
    }

    @Subcommand("reload")
    @CommandPermission(value = "challenges.commands.reload", vanilla = 2)
    public void handleReloadCommand(FabricCommandActor actor) {
        api.reloadConfig();
        actor.sendRawMessage(FabricAdapter.adapt(api.getMessage("commands.reload")));
    }

    @Subcommand("forcecomplete")
    @CommandPermission(value = "challenges.commands.forcecomplete", vanilla = 2)
    public void handleForceCompleteCommand(FabricCommandActor actor, @Optional ServerPlayer targetPlayer, @Optional String challengeName) {
        try {
            // If no target player specified, complete challenge for command sender
            ServerPlayer player = targetPlayer != null ? targetPlayer : actor.requirePlayer();

            PlayerProfile profile = api.getOrCreateProfile(player.getUUID());

            // If no challenge name provided, show list of active challenges with numbers
            if (challengeName == null || challengeName.isEmpty()) {
                List<com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress> activeChallenges = new ArrayList<>();

                for (var entry : profile.getActiveChallengesMap().entrySet()) {
                    activeChallenges.addAll(entry.getValue());
                }

                if (activeChallenges.isEmpty()) {
                    actor.sendRawMessage(Component.literal("Player " + player.getGameProfile().getName() + " has no active challenges")
                        .withStyle(ChatFormatting.YELLOW));
                } else {
                    actor.sendRawMessage(Component.literal("═══════════════════════════════════════")
                        .withStyle(ChatFormatting.GOLD));
                    actor.sendRawMessage(Component.literal("Active challenges for " + player.getGameProfile().getName() + ":")
                        .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD));
                    actor.sendRawMessage(Component.literal("═══════════════════════════════════════")
                        .withStyle(ChatFormatting.GOLD));

                    int index = 1;
                    for (var progress : activeChallenges) {
                        String challengeName2 = progress.getActiveChallenge().getName();
                        actor.sendRawMessage(Component.literal(String.format("[%d] %s", index, challengeName2))
                            .withStyle(ChatFormatting.GRAY));
                        index++;
                    }

                    actor.sendRawMessage(Component.literal("").withStyle(ChatFormatting.GRAY));
                    actor.sendRawMessage(Component.literal("Usage: /challenges forcecomplete [player] <challenge name or number>")
                        .withStyle(ChatFormatting.AQUA));
                }
                return;
            }

            // Try to parse as number first
            com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress foundProgress = null;
            try {
                int index = Integer.parseInt(challengeName);
                List<com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress> activeChallenges = new ArrayList<>();
                for (var entry : profile.getActiveChallengesMap().entrySet()) {
                    activeChallenges.addAll(entry.getValue());
                }

                if (index > 0 && index <= activeChallenges.size()) {
                    foundProgress = activeChallenges.get(index - 1);
                }
            } catch (NumberFormatException e) {
                // Not a number, search by name
                for (var entry : profile.getActiveChallengesMap().entrySet()) {
                    for (var progress : entry.getValue()) {
                        if (progress.getActiveChallenge().getName().equalsIgnoreCase(challengeName)) {
                            foundProgress = progress;
                            break;
                        }
                    }
                    if (foundProgress != null) break;
                }
            }

            if (foundProgress == null) {
                actor.sendRawMessage(Component.literal("Challenge '" + challengeName + "' not found in active challenges for player " + player.getGameProfile().getName())
                    .withStyle(ChatFormatting.RED));
                actor.sendRawMessage(Component.literal("Use /challenges forcecomplete to see the list of active challenges")
                    .withStyle(ChatFormatting.YELLOW));
                return;
            }

            // Force complete the challenge
            String completedChallengeName = foundProgress.getActiveChallenge().getName();
            foundProgress.completedActiveChallenge();

            actor.sendRawMessage(Component.literal("✓ Force completed challenge '" + completedChallengeName + "' for player " + player.getGameProfile().getName())
                .withStyle(ChatFormatting.GREEN));

        } catch (Exception e) {
            actor.sendRawMessage(Component.literal("Error: " + e.getMessage()).withStyle(ChatFormatting.RED));
            e.printStackTrace();
        }
    }

    @CommandPlaceholder
    @CommandPermission(value = "challenges.commands.challenge", vanilla = 2)
    public void handleChallengeListCommand(FabricCommandActor actor, @Optional ChallengeList challengeList) {
        try {
            ServerPlayer player = actor.requirePlayer();
            if(challengeList == null) {

                ChallengeMenuGUI gui = new ChallengeMenuGUI(api, api.getOrCreateProfile(player.getUUID()));
                gui.open();
                if (!player.hasContainerOpen())
                    player.displayClientMessage(FabricAdapter.adapt(api.getMessage("commands.opening-base-gui")), false);
            }
            else {
                new ChallengeListGUI(api, api.getOrCreateProfile(player.getUUID()), challengeList, api.getConfigOptions().getChallengeGuiConfig(challengeList.getName())).open();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
