package com.github.kuramastone.cobblemonChallenges.guis;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChallengeReplacementGUI {
    
    private static final Map<UUID, PendingReplacement> pendingReplacements = new HashMap<>();

    private final CobbleChallengeAPI api;
    private final PlayerProfile profile;
    private final ChallengeList challengeList;
    private final Challenge newChallenge;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ChallengeReplacementGUI(CobbleChallengeAPI api, PlayerProfile profile, ChallengeList challengeList, 
                                   Challenge newChallenge, Runnable onConfirm, Runnable onCancel) {
        this.api = api;
        this.profile = profile;
        this.challengeList = challengeList;
        this.newChallenge = newChallenge;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public void open() {
        ChallengeProgress currentActive = getCurrentActiveForList();
        if (currentActive != null) {
            // Close the player's current GUI
            if (profile.getPlayerEntity().containerMenu != profile.getPlayerEntity().inventoryMenu) {
                profile.getPlayerEntity().closeContainer();
            }
            
            // Store the pending replacement
            String confirmationId = UUID.randomUUID().toString().substring(0, 8);
            pendingReplacements.put(profile.getUUID(), new PendingReplacement(
                challengeList, newChallenge, onConfirm, onCancel, System.currentTimeMillis()
            ));
            
            // Send warning message with clickable confirmation
            profile.sendMessage("");
            profile.sendAdventureMessage(api.getMiniMessage("challenges.replacement.separator"));
            profile.sendAdventureMessage(api.getMiniMessage("challenges.replacement.title"));
            profile.sendAdventureMessage(api.getMiniMessage("challenges.replacement.separator"));
            profile.sendMessage("");
            profile.getPlayerEntity().sendSystemMessage(
                FabricAdapter.adapt(api.getMiniMessage("challenges.replacement.current-mission")));
            profile.getPlayerEntity().sendSystemMessage(
                FabricAdapter.adapt(api.getMiniMessage("challenges.replacement.current-mission-name",
                "{challenge_name}", currentActive.getActiveChallenge().getName())));
            profile.sendMessage("");
            profile.getPlayerEntity().sendSystemMessage(
                FabricAdapter.adapt(api.getMiniMessage("challenges.replacement.new-mission")));
            profile.getPlayerEntity().sendSystemMessage(
                FabricAdapter.adapt(api.getMiniMessage("challenges.replacement.new-mission-name",
                "{challenge_name}", newChallenge.getName())));
            profile.sendMessage("");
            profile.sendAdventureMessage(api.getMiniMessage("challenges.replacement.warning"));
            profile.sendMessage("");
            
            // Create clickable confirmation buttons
            MutableComponent confirmButton = Component.literal(PlainTextComponentSerializer.plainText().serialize(
                    api.getMiniMessage("challenges.replacement.confirm-button-text")))
                .withStyle(ChatFormatting.GREEN)
                .withStyle(ChatFormatting.BOLD)
                .withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenges confirm-replacement " + confirmationId))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(PlainTextComponentSerializer.plainText().serialize(
                            api.getMiniMessage("challenges.replacement.confirm-button-hover")))))
                );
                
            MutableComponent cancelButton = Component.literal(PlainTextComponentSerializer.plainText().serialize(
                    api.getMiniMessage("challenges.replacement.cancel-button-text")))
                .withStyle(ChatFormatting.RED)
                .withStyle(ChatFormatting.BOLD)
                .withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenges cancel-replacement " + confirmationId))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(PlainTextComponentSerializer.plainText().serialize(
                            api.getMiniMessage("challenges.replacement.cancel-button-hover")))))
                );
            
            MutableComponent buttonRow = Component.literal("  ")
                .append(confirmButton)
                .append(Component.literal("    "))
                .append(cancelButton);
            
            profile.getPlayerEntity().sendSystemMessage(buttonRow);
            profile.sendMessage("");
            profile.sendAdventureMessage(api.getMiniMessage("challenges.replacement.separator"));
            
            // Player prompted for mission replacement
            
        } else {
            // Shouldn't happen, but fallback
            onCancel.run();
        }
    }

    private ChallengeProgress getCurrentActiveForList() {
        for (ChallengeProgress activeChallenge : profile.getActiveChallenges()) {
            if (activeChallenge.getActiveChallenge().doesNeedSelection() && 
                activeChallenge.getParentList().getName().equals(challengeList.getName())) {
                return activeChallenge;
            }
        }
        return null;
    }
    
    public static void handleConfirmation(UUID playerUUID, String confirmationId, boolean confirmed) {
        PendingReplacement replacement = pendingReplacements.remove(playerUUID);
        if (replacement == null) {
            return; // Already handled or expired
        }
        
        // Check if the confirmation hasn't expired (5 minutes timeout)
        if (System.currentTimeMillis() - replacement.timestamp > 5 * 60 * 1000) {
            return; // Expired
        }
        
        if (confirmed) {
            replacement.onConfirm.run();
            // Player confirmed mission replacement
        } else {
            replacement.onCancel.run();
            // Player cancelled mission replacement
        }
    }
    
    private static class PendingReplacement {
        final ChallengeList challengeList;
        final Challenge newChallenge;
        final Runnable onConfirm;
        final Runnable onCancel;
        final long timestamp;
        
        PendingReplacement(ChallengeList challengeList, Challenge newChallenge, 
                          Runnable onConfirm, Runnable onCancel, long timestamp) {
            this.challengeList = challengeList;
            this.newChallenge = newChallenge;
            this.onConfirm = onConfirm;
            this.onCancel = onCancel;
            this.timestamp = timestamp;
        }
    }
}