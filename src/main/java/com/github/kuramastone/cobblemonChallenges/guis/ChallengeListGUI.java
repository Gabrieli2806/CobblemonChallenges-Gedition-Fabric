package com.github.kuramastone.cobblemonChallenges.guis;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import net.minecraft.world.item.ItemStack;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.MilestoneTimePlayedRequirement;
import com.github.kuramastone.cobblemonChallenges.gui.GuiConfig;
import com.github.kuramastone.cobblemonChallenges.gui.ItemProvider;
import com.github.kuramastone.cobblemonChallenges.gui.SimpleWindow;
import com.github.kuramastone.cobblemonChallenges.gui.WindowItem;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.PermissionUtils;
import net.minecraft.world.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ChallengeListGUI {

    private final CobbleChallengeAPI api;
    private final PlayerProfile profile;
    private final ChallengeList challengeList;

    private final SimpleWindow window;

    public ChallengeListGUI(CobbleChallengeAPI api, PlayerProfile profile, ChallengeList challengeList, GuiConfig config) {
        this.api = api;
        this.profile = profile;
        this.challengeList = challengeList;
        window = new SimpleWindow(config);
        build();
    }

    boolean hasActiveType(PlayerProfile profile, Challenge challenge) {
        int activeCount = 0;
        for (ChallengeProgress activeChallenge : profile.getActiveChallenges()) {
            if (activeChallenge.getActiveChallenge().doesNeedSelection() && 
                activeChallenge.getParentList().getName().equals(challengeList.getName())) {
                activeCount++;
            }
        }
        
        boolean hasMax = activeCount >= challengeList.getMaxChallengesPerPlayer();
        // CobbleChallengeMod.logger.info("hasActiveType check for {}: {} active challenges, max: {}, has max: {}", 
//  // challengeList.getName(), activeCount, challengeList.getMaxChallengesPerPlayer(), hasMax);
        
        return hasMax;
    }

    private void build() {
        List<WindowItem> contents = new ArrayList<>();

        //window is already built aesthetically, but now we need to insert each visible challenge
        for (Challenge challenge : challengeList.getVisibleChallenges()) {
            var perm = challenge.getPermission();
            if (perm != null && !PermissionUtils.hasPermission(profile.getPlayerEntity(), perm)) {
                // Create a more informative no-permission item
                WindowItem item = createNoPermissionItem(challenge);
                contents.add(item);
                continue;
            }

            WindowItem item = new WindowItem(window, new ChallengeItem(window, profile, challenge));
            if (challenge.doesNeedSelection() && profile.isChallengeInProgress(challenge.getName()))
                item.setAutoUpdate(15, () ->
                        // check if this challenge requirement should auto-update
                        challenge.getRequirements().stream().anyMatch(it -> it instanceof MilestoneTimePlayedRequirement)
                                // check if challenge has a timer that needs ticking
                                || profile.isChallengeInProgress(challenge.getName())
                );
            item.setRunnableOnClick(onChallengeClick(challenge, item));
            contents.add(item);
        }

        window.setContents(contents);
    }

    private BiConsumer<ClickType, Integer> onChallengeClick(Challenge challenge, WindowItem item) {
        return (type, dragType) -> {
            // CobbleChallengeMod.logger.debug("Challenge click: '{}' by player: {}", challenge.getName(), profile.getUUID());
//  // CobbleChallengeMod.logger.debug("Click Type: {}, DragType: {}, NeedsSelection: {}", type, dragType, challenge.doesNeedSelection());
            
            if (dragType == 0 && challenge.doesNeedSelection()) {
                // Check if challenge is already completed
                if (profile.isChallengeCompleted(challenge.getName())) {
                    profile.sendAdventureMessage(api.getMiniMessage("challenges.mission-already-completed"));
                    return;
                }
                
                // Check if challenge is already in progress
                if (profile.isChallengeInProgress(challenge.getName())) {
                    profile.sendAdventureMessage(api.getMiniMessage("challenges.mission-already-in-progress"));
                    return;
                }
                
                // Check if player has reached max challenges for this category
                if (hasActiveType(profile, challenge)) {
                    profile.sendMessage("§7Ya tienes una misión activa en esta categoría. Reemplazando...");
                    showReplacementConfirmation(challenge, item);
                } else {
                    // Can select directly - add some feedback
                    profile.sendMessage("§a¡Misión iniciada: §e" + challenge.getName());
                    profile.addActiveChallenge(challengeList, challenge);
                    
                    // Only check completion for always-active challenges
                    if (!challenge.doesNeedSelection()) {
                        profile.checkCompletion(challengeList);
                    }
                    
                    item.setAutoUpdate(10, () -> true);
                    item.notifyWindow();
                    // Challenge started successfully
                }
            } else if (dragType == 1 && challenge.doesNeedSelection()) {
                // Right click to remove challenge
                if (profile.isChallengeInProgress(challenge.getName())) {
                    profile.sendAdventureMessage(api.getMiniMessage("challenges.mission-cancelled",
                        "{challenge_name}", challenge.getName()));
                    profile.removeActiveChallenge(profile.getActiveChallengeProgress(challenge.getName()));
                    item.setAutoUpdate(10, () -> true);
                    item.notifyWindow();
                    // Challenge cancelled by player
                } else {
                    profile.sendAdventureMessage(api.getMiniMessage("challenges.mission-not-active"));
                }
            } else if (!challenge.doesNeedSelection()) {
                profile.sendAdventureMessage(api.getMiniMessage("challenges.automatic-mission"));
            } else {
                // CobbleChallengeMod.logger.debug("Click not processed - Type: {}, DragType: {}, NeedsSelection: {}", type, dragType, challenge.doesNeedSelection());
            }
        };
    }

    private void showReplacementConfirmation(Challenge newChallenge, WindowItem item) {
        // Create a simple confirmation dialog
        ChallengeReplacementGUI confirmationGUI = new ChallengeReplacementGUI(api, profile, challengeList, newChallenge, () -> {
            // On confirm: replace the active challenge
            ChallengeProgress currentActive = getCurrentActiveForList();
            if (currentActive != null) {
                profile.removeActiveChallenge(currentActive);
            }

            // Check if challenge still exists before adding
            if (!challengeList.getChallengeMap().contains(newChallenge)) {
                profile.sendMessage(api.getMessage("challenges.failure.challenge-no-longer-available", "{challenge}", newChallenge.getName()).build());
                return;
            }

            profile.addActiveChallenge(challengeList, newChallenge);
            
            // Only check completion for always-active challenges
            if (!newChallenge.doesNeedSelection()) {
                profile.checkCompletion(challengeList);
            }
            
            item.setAutoUpdate(10, () -> true);
            item.notifyWindow();
            
            // Reopen this GUI
            this.open();
        }, () -> {
            // On cancel: just reopen this GUI
            this.open();
        });
        confirmationGUI.open();
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

    private WindowItem createNoPermissionItem(Challenge challenge) {
        // Create a basic no-permission item
        WindowItem item = new WindowItem(window, new ItemProvider.ItemWrapper(
            CobbleChallengeMod.instance.getAPI().getConfigOptions().getNoPermChallengeItem()));
        
        // Add click handler to show detailed permission message
        item.setRunnableOnClick((type, dragType) -> {
            if (dragType == 0) {
                profile.sendMessage("");
                profile.sendMessage("§c§l🔒 ═════ ACCESO DENEGADO ═════ 🔒");
                profile.sendMessage("");
                profile.sendMessage("§7La misión §e§l" + challenge.getName() + " §7está bloqueada");
                profile.sendMessage("§7para tu rango actual.");
                profile.sendMessage("");
                profile.sendMessage("§cPermiso requerido: §f" + challenge.getPermission());
                profile.sendMessage("");
                profile.sendMessage("§6¿Quieres acceso a esta misión?");
                profile.sendMessage("§6¡Contacta a un administrador del servidor!");
                profile.sendMessage("");
                profile.sendMessage("§8Tip: Puedes conseguir permisos comprando");
                profile.sendMessage("§8rangos VIP o completando otras misiones.");
                profile.sendMessage("");
                profile.sendMessage("§c§l🔒 ═════════════════════════ 🔒");
                profile.sendMessage("");
                
                // CobbleChallengeMod.logger.info("Player {} attempted to access restricted mission: {} (requires: {})", 
// profile.getUUID(), challenge.getName(), challenge.getPermission());
            }
        });
        
        return item;
    }

    public void open() {
        window.show(profile.getPlayerEntity());
    }
}




















