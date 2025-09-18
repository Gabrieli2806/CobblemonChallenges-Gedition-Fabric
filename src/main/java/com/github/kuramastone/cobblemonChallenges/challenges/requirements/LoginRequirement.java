package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.events.PlayerJoinEvent;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.UUID;

public class LoginRequirement implements Requirement {
    public static final String ID = "login";

    @YamlKey("amount")
    private int amount = 1;

    public LoginRequirement() {
    }

    public Requirement load(YamlConfig section) {
        return YamlConfig.loadFromYaml(this, section);
    }

    // The requirement name now returns the ID used to recognize it
    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Progression<?> buildProgression(PlayerProfile profile) {
        return new LoginProgression(profile, this);
    }

    // Static nested Progression class
    public static class LoginProgression implements Progression<PlayerJoinEvent> {

        private PlayerProfile profile;
        private LoginRequirement requirement;
        private int progressAmount;

        public LoginProgression(PlayerProfile profile, LoginRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<PlayerJoinEvent> getType() {
            return PlayerJoinEvent.class;
        }

        @Override
        public boolean isCompleted() {
            return progressAmount >= requirement.amount;
        }

        @Override
        public void progress(Object obj) {
            if (matchesMethod(obj)) {
                if (meetsCriteria(getType().cast(obj))) {
                    progressAmount++;
                }
            }
        }

        @Override
        public boolean meetsCriteria(PlayerJoinEvent event) {
            return true;
        }

        @Override
        public boolean matchesMethod(Object obj) {
            return getType().isInstance(obj);
        }

        @Override
        public double getPercentageComplete() {
            return (double) progressAmount / requirement.amount;
        }


        @Override
        public Progression loadFrom(UUID uuid, YamlConfig configurationSection) {
            this.progressAmount = configurationSection.getInt("progressAmount");
            return this;
        }

        @Override
        public void writeTo(YamlConfig configurationSection) {
            configurationSection.set("progressAmount", progressAmount);
        }

        @Override
        public String getProgressString() {
            return PlainTextComponentSerializer.plainText().serialize(
                CobbleChallengeMod.instance.getAPI().getMiniMessage("challenges.progression-string",
                "{current}", String.valueOf(this.progressAmount),
                "{target}", String.valueOf(this.requirement.amount)));
        }
    }
}