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
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.concurrent.CompletableFuture;

public class OldCommandHandler {

    private static CobbleChallengeAPI api;

    public static void register() {
        api = CobbleChallengeMod.instance.getAPI();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            // Register main command /challenges
            dispatcher.register(Commands.literal("challenges")
                    .requires(source -> hasPermission(source, "challenges.commands.challenge"))
                    .executes(OldCommandHandler::handleChallengeBaseCommand)
                    .then(Commands.argument("list", StringArgumentType.word())
                            .suggests(OldCommandHandler::handleListSuggestions)
                            .executes(OldCommandHandler::handleChallengeListCommand)
                    )
                    .then(Commands.literal("reload")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.reload"))
                            .executes(OldCommandHandler::handleReloadCommand))
                    .then(Commands.literal("reset")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.restart"))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(OldCommandHandler::handleRestartCommand)))
                    .then(Commands.literal("rotate")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.rotate"))
                            .executes(OldCommandHandler::handleRotateAllCommand)
                            .then(Commands.argument("list", StringArgumentType.word())
                                    .suggests(OldCommandHandler::handleListSuggestions)
                                    .executes(OldCommandHandler::handleRotateListCommand)))
                    .then(Commands.literal("confirm-replacement")
                            .then(Commands.argument("confirmationId", StringArgumentType.word())
                                    .executes(OldCommandHandler::handleConfirmReplacement)))
                    .then(Commands.literal("cancel-replacement")
                            .then(Commands.argument("confirmationId", StringArgumentType.word())
                                    .executes(OldCommandHandler::handleCancelReplacement)))
                    .then(Commands.literal("rotation-testing")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.rotate"))
                            .then(Commands.literal("enable")
                                    .executes(OldCommandHandler::handleEnableRotationTesting))
                            .then(Commands.literal("disable")
                                    .executes(OldCommandHandler::handleDisableRotationTesting))
                            .then(Commands.literal("status")
                                    .executes(OldCommandHandler::handleRotationTestingStatus)))
                    .then(Commands.literal("forcecomplete")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.forcecomplete"))
                            .executes(OldCommandHandler::handleForceCompleteList)
                            .then(Commands.argument("challenge", StringArgumentType.greedyString())
                                    .executes(OldCommandHandler::handleForceCompleteSelf))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(OldCommandHandler::handleForceCompleteOtherList)
                                    .then(Commands.argument("challenge", StringArgumentType.greedyString())
                                            .executes(OldCommandHandler::handleForceCompleteOther))))
            );

            // Register alias command /misiones
            dispatcher.register(Commands.literal("misiones")
                    .requires(source -> hasPermission(source, "challenges.commands.challenge"))
                    .executes(OldCommandHandler::handleChallengeBaseCommand)
                    .then(Commands.argument("list", StringArgumentType.word())
                            .suggests(OldCommandHandler::handleListSuggestions)
                            .executes(OldCommandHandler::handleChallengeListCommand)
                    )
                    .then(Commands.literal("reload")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.reload"))
                            .executes(OldCommandHandler::handleReloadCommand))
                    .then(Commands.literal("reset")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.restart"))
                            .then(Commands.argument("player", EntityArgument.player())
                                    .executes(OldCommandHandler::handleRestartCommand)))
                    .then(Commands.literal("rotate")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.rotate"))
                            .executes(OldCommandHandler::handleRotateAllCommand)
                            .then(Commands.argument("list", StringArgumentType.word())
                                    .suggests(OldCommandHandler::handleListSuggestions)
                                    .executes(OldCommandHandler::handleRotateListCommand)))
                    .then(Commands.literal("confirm-replacement")
                            .then(Commands.argument("confirmationId", StringArgumentType.word())
                                    .executes(OldCommandHandler::handleConfirmReplacement)))
                    .then(Commands.literal("cancel-replacement")
                            .then(Commands.argument("confirmationId", StringArgumentType.word())
                                    .executes(OldCommandHandler::handleCancelReplacement)))
                    .then(Commands.literal("rotation-testing")
                            .requires(source -> hasPermission(source, "challenges.commands.admin.rotate"))
                            .then(Commands.literal("enable")
                                    .executes(OldCommandHandler::handleEnableRotationTesting))
                            .then(Commands.literal("disable")
                                    .executes(OldCommandHandler::handleDisableRotationTesting))
                            .then(Commands.literal("status")
                                    .executes(OldCommandHandler::handleRotationTestingStatus)))
            );
        });

    }

    private static int handleRestartCommand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player player = EntityArgument.getPlayer(context, "player");
        PlayerProfile profile = CobbleChallengeMod.instance.getAPI().getOrCreateProfile(player.getUUID());

        profile.resetChallenges();
        profile.addUnrestrictedChallenges();

        context.getSource().sendSystemMessage(FabricAdapter.adapt(api.getMiniMessage("commands.restart")));
        return 1;
    }


    private static boolean hasPermission(CommandSourceStack source, String perm) {
        return source.hasPermission(2) || (source.isPlayer() && PermissionUtils.hasPermission(source.getPlayer(), perm));
    }

    private static CompletableFuture<Suggestions> handleListSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        // Define available options for completion
        for (ChallengeList cl : api.getChallengeLists()) {
            // Add each option to the suggestions
            builder.suggest(cl.getName());
        }

        return builder.buildFuture();
    }

    private static int handleReloadCommand(CommandContext<CommandSourceStack> context) {
        // The reloadConfig method now handles rotation prevention internally
        api.reloadConfig();

        context.getSource().sendSystemMessage(FabricAdapter.adapt(api.getMiniMessage("commands.reload")));
        return 1;
    }

    private static int handleChallengeBaseCommand(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();

            if (!source.isPlayer()) {
                source.sendSystemMessage(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
                return 1;
            }

            ServerPlayer player = (ServerPlayer) source.getEntity();

            ChallengeMenuGUI gui = new ChallengeMenuGUI(api, api.getOrCreateProfile(player.getUUID()));
            gui.open();
            if (!player.hasContainerOpen())
                player.displayClientMessage(FabricAdapter.adapt(api.getMiniMessage("commands.opening-base-gui")), false);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static int handleChallengeListCommand(CommandContext<CommandSourceStack> context) {
        try {
            String listName = StringArgumentType.getString(context, "list");

            CommandSourceStack source = context.getSource();

            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can use this command.").withStyle(ChatFormatting.RED));
                return 1;
            }

            ServerPlayer player = source.getPlayer();
            ChallengeList challengeList = api.getChallengeList(listName);

            if (challengeList == null) {
                source.sendFailure(FabricAdapter.adapt(api.getMiniMessage("issues.unknown_challenge_list", "{challenge_list}", listName)));
                return 1;
            }

            new ChallengeListGUI(api, api.getOrCreateProfile(player.getUUID()), challengeList, api.getConfigOptions().getChallengeGuiConfig(challengeList.getName())).open();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
    
    private static int handleRotateAllCommand(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            int rotatedCount = 0;
            for (ChallengeList challengeList : api.getChallengeLists()) {
                if (!"disabled".equals(challengeList.getRotationInterval())) {
                    challengeList.forceRotation();
                    rotatedCount++;
                }
            }
            
            final int finalRotatedCount = rotatedCount;
            source.sendSuccess(() -> Component.literal("Rotated " + finalRotatedCount + " challenge lists.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    private static int handleRotateListCommand(CommandContext<CommandSourceStack> context) {
        try {
            String listName = StringArgumentType.getString(context, "list");
            CommandSourceStack source = context.getSource();
            
            ChallengeList challengeList = api.getChallengeList(listName);
            if (challengeList == null) {
                source.sendFailure(Component.literal("Challenge list '" + listName + "' not found.").withStyle(ChatFormatting.RED));
                return 1;
            }
            
            if ("disabled".equals(challengeList.getRotationInterval())) {
                source.sendFailure(Component.literal("Challenge list '" + listName + "' has rotation disabled.").withStyle(ChatFormatting.RED));
                return 1;
            }
            
            challengeList.forceRotation();
            source.sendSuccess(() -> Component.literal("Rotated challenge list '" + listName + "'.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    private static int handleConfirmReplacement(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            if (!source.isPlayer()) {
                return 0;
            }
            
            String confirmationId = StringArgumentType.getString(context, "confirmationId");
            ServerPlayer player = source.getPlayer();
            
            com.github.kuramastone.cobblemonChallenges.guis.ChallengeReplacementGUI.handleConfirmation(
                player.getUUID(), confirmationId, true);
            
            player.sendSystemMessage(FabricAdapter.adapt(api.getMiniMessage("challenges.replacement-success")));
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    private static int handleCancelReplacement(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            if (!source.isPlayer()) {
                return 0;
            }
            
            String confirmationId = StringArgumentType.getString(context, "confirmationId");
            ServerPlayer player = source.getPlayer();
            
            com.github.kuramastone.cobblemonChallenges.guis.ChallengeReplacementGUI.handleConfirmation(
                player.getUUID(), confirmationId, false);
            
            player.sendSystemMessage(FabricAdapter.adapt(api.getMiniMessage("challenges.replacement.cancel-success")));
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return 0;
    }
    
    private static int handleEnableRotationTesting(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            CobbleChallengeMod.rotationTestingMode = true;
            
            Component message = Component.literal("Modo de testing de rotación HABILITADO. Intervalos: Daily=2min, Weekly=5min, Monthly=10min")
                .withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> message, true);
            
            // CobbleChallengeMod.logger.info("Rotation testing mode enabled by {}", 
//  // source.isPlayer() ? source.getPlayer().getName().getString() : "Console");
            
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private static int handleDisableRotationTesting(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            CobbleChallengeMod.rotationTestingMode = false;
            
            Component message = Component.literal("Modo de testing de rotación DESHABILITADO. Intervalos normales restaurados.")
                .withStyle(ChatFormatting.YELLOW);
            source.sendSuccess(() -> message, true);
            
            // CobbleChallengeMod.logger.info("Rotation testing mode disabled by {}", 
//  // source.isPlayer() ? source.getPlayer().getName().getString() : "Console");
            
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private static int handleRotationTestingStatus(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            String status = CobbleChallengeMod.rotationTestingMode ? "HABILITADO" : "DESHABILITADO";
            ChatFormatting color = CobbleChallengeMod.rotationTestingMode ? ChatFormatting.GREEN : ChatFormatting.RED;

            Component message = Component.literal("Estado del modo de testing de rotación: " + status)
                .withStyle(color);
            source.sendSuccess(() -> message, false);

            if (CobbleChallengeMod.rotationTestingMode) {
                Component intervalInfo = Component.literal("Intervalos de testing: Daily=2min, Weekly=5min, Monthly=10min")
                    .withStyle(ChatFormatting.GRAY);
                source.sendSuccess(() -> intervalInfo, false);
            }

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int handleForceCompleteList(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can use this command").withStyle(ChatFormatting.RED));
                return 0;
            }

            ServerPlayer player = source.getPlayer();
            return showActiveChallengesList(source, player);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int handleForceCompleteOtherList(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            return showActiveChallengesList(source, targetPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int handleForceCompleteSelf(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can use this command").withStyle(ChatFormatting.RED));
                return 0;
            }

            String challengeInput = StringArgumentType.getString(context, "challenge");
            ServerPlayer player = source.getPlayer();
            return forceCompleteChallenge(source, player, challengeInput);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int handleForceCompleteOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
            String challengeInput = StringArgumentType.getString(context, "challenge");
            return forceCompleteChallenge(source, targetPlayer, challengeInput);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int showActiveChallengesList(CommandSourceStack source, ServerPlayer player) {
        PlayerProfile profile = api.getOrCreateProfile(player.getUUID());
        java.util.List<com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress> activeChallenges = new java.util.ArrayList<>();

        for (var entry : profile.getActiveChallengesMap().entrySet()) {
            activeChallenges.addAll(entry.getValue());
        }

        if (activeChallenges.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Player " + player.getGameProfile().getName() + " has no active challenges")
                .withStyle(ChatFormatting.YELLOW), false);
        } else {
            source.sendSuccess(() -> Component.literal("═══════════════════════════════════════").withStyle(ChatFormatting.GOLD), false);
            source.sendSuccess(() -> Component.literal("Active challenges for " + player.getGameProfile().getName() + ":")
                .withStyle(ChatFormatting.GOLD).withStyle(ChatFormatting.BOLD), false);
            source.sendSuccess(() -> Component.literal("═══════════════════════════════════════").withStyle(ChatFormatting.GOLD), false);

            int index = 1;
            for (var progress : activeChallenges) {
                String challengeName = progress.getActiveChallenge().getName();
                int finalIndex = index;
                source.sendSuccess(() -> Component.literal(String.format("[%d] %s", finalIndex, challengeName))
                    .withStyle(ChatFormatting.GRAY), false);
                index++;
            }

            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("Usage: /challenges forcecomplete [player] <challenge name or number>")
                .withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    private static int forceCompleteChallenge(CommandSourceStack source, ServerPlayer player, String challengeInput) {
        PlayerProfile profile = api.getOrCreateProfile(player.getUUID());
        com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress foundProgress = null;

        // Try to parse as number first
        try {
            int index = Integer.parseInt(challengeInput);
            java.util.List<com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress> activeChallenges = new java.util.ArrayList<>();
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
                    if (progress.getActiveChallenge().getName().equalsIgnoreCase(challengeInput)) {
                        foundProgress = progress;
                        break;
                    }
                }
                if (foundProgress != null) break;
            }
        }

        if (foundProgress == null) {
            source.sendFailure(Component.literal("Challenge '" + challengeInput + "' not found in active challenges for player " + player.getGameProfile().getName())
                .withStyle(ChatFormatting.RED));
            source.sendSuccess(() -> Component.literal("Use /challenges forcecomplete to see the list of active challenges")
                .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        // Force complete the challenge
        String completedChallengeName = foundProgress.getActiveChallenge().getName();
        foundProgress.completedActiveChallenge();

        source.sendSuccess(() -> Component.literal("✓ Force completed challenge '" + completedChallengeName + "' for player " + player.getGameProfile().getName())
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}