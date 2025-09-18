package com.github.kuramastone.cobblemonChallenges.guis;

import com.github.kuramastone.bUtilities.ComponentEditor;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.Challenge;
import com.github.kuramastone.cobblemonChallenges.gui.ItemProvider;
import com.github.kuramastone.cobblemonChallenges.gui.SimpleWindow;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.FabricAdapter;
import com.github.kuramastone.cobblemonChallenges.utils.ItemUtils;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChallengeItem implements ItemProvider {

    private SimpleWindow window;
    private CobbleChallengeAPI api;
    private PlayerProfile profile;
    private Challenge challenge;

    public ChallengeItem(SimpleWindow window, PlayerProfile profile, Challenge challenge) {
        this.window = window;
        this.profile = profile;
        this.challenge = challenge;
        api = CobbleChallengeMod.instance.getAPI();
    }

    @Override
    public ItemStack build() {
        boolean inProgress = profile.isChallengeInProgress(challenge.getName());
        boolean completed = profile.isChallengeCompleted(challenge.getName());
        
        // Building challenge item

        ItemStack item = FabricAdapter.toItemStack(challenge.getDisplayConfig());

        //format lore
        List<net.minecraft.network.chat.Component> loreComponents = new ArrayList<>();

        // Keep description as MiniMessage text for proper processing
        String descriptionText = challenge.getDescription();

        for (String line : challenge.getDisplayConfig().getLore()) {
            String[] replacements = {
                    "{progression_status}", null,
                    "{description}", descriptionText,
                    "{tracking-tag}", null
            };

            // insert correct tracking tag
            if(challenge.doesNeedSelection()) {
                if (profile.isChallengeInProgress(challenge.getName())) {
                    long timeRemaining = profile.getActiveChallengeProgress(challenge.getName()).getTimeRemaining();
                    replacements[5] = PlainTextComponentSerializer.plainText().serialize(api.getMiniMessage("challenges.tracking-tag.after-starting", "{time-remaining}",
                            StringUtils.formatSecondsToString(timeRemaining / 1000)));
                }
                else {
                    long timeRemaining = challenge.getMaxTimeInMilliseconds();
                    replacements[5] = PlainTextComponentSerializer.plainText().serialize(api.getMiniMessage("challenges.tracking-tag.before-starting", "{time-remaining}",
                            StringUtils.formatSecondsToString(timeRemaining / 1000)));
                }
            }

            // insert correct progress tag
            if (profile.isChallengeCompleted(challenge.getName())) {
                replacements[1] = PlainTextComponentSerializer.plainText().serialize(api.getMiniMessage("challenges.progression_status.post-completion"));
                replacements[5] = ""; // remove tracking tag if completed
            }
            else if (profile.isChallengeInProgress(challenge.getName())) {
                String progressLines = profile.getActiveChallengeProgress(challenge.getName()).getProgressListAsString();
                replacements[1] = PlainTextComponentSerializer.plainText().serialize(api.getMiniMessage("challenges.progression_status.during-attempt")) + "\n" + progressLines;
            }
            else {
                replacements[1] = PlainTextComponentSerializer.plainText().serialize(api.getMiniMessage("challenges.progression_status.before-attempt"));
            }

            // remove tracking tag if no timer needed
            if(!challenge.doesNeedSelection()) {
                replacements[5] = "";
            }

            for (int i = 0; i < replacements.length; i += 2) {
                if (replacements[i] != null && replacements[i + 1] != null)
                    line = line.replace(replacements[i], replacements[i + 1]);
            }

            // Process MiniMessage and handle line breaks properly
            String[] splitLines = StringUtils.splitByLineBreak(line);
            for (String singleLine : splitLines) {
                if (!singleLine.trim().isEmpty()) {
                    net.kyori.adventure.text.Component adventureComponent = com.github.kuramastone.cobblemonChallenges.utils.MiniMessageUtils.parse(singleLine);
                    loreComponents.add(FabricAdapter.adapt(adventureComponent));
                }
            }
        }

        ItemUtils.setLoreComponents(item, loreComponents);

        if (profile.isChallengeCompleted(challenge.getName())) {
            item = ItemUtils.setItem(item, api.getConfigOptions().getCompletedChallengeItem().getItem());
        }
        else if (profile.isChallengeInProgress(challenge.getName()) && challenge.doesNeedSelection()) {
            item = ItemUtils.setItem(item, api.getConfigOptions().getActiveChallengeItem().getItem());
        }

        item.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);

        return item;
    }

    @Override
    public ItemProvider copy() {
        return new ChallengeItem(window, profile, challenge);
    }
}
