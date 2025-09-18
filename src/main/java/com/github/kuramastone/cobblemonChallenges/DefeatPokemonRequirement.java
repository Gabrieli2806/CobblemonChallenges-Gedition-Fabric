package com.github.kuramastone.cobblemonChallenges;

import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Progression;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.Requirement;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.PixelmonUtils;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DefeatPokemonRequirement implements Requirement {
    public static final String ID = "Defeat_Pokemon";

    @YamlKey("pokename")
    public String pokename = "any";
    @YamlKey("amount")
    private int amount = 1;

    @YamlKey("shiny")
    private boolean shiny = false;
    @YamlKey("pokemon_type")
    private String pokemon_type = "any";
    @YamlKey("time_of_day")
    private String time_of_day = "any";
    @YamlKey("is_legendary")
    private boolean is_legendary = false;
    @YamlKey("is_ultra_beast")
    private boolean is_ultra_beast = false;

    @Override
    public Requirement load(YamlConfig section) {
        return YamlConfig.loadFromYaml(this, section);
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Progression<?> buildProgression(PlayerProfile profile) {
        return new DefeatPokemonProgression(profile, this);
    }

    public static class DefeatPokemonProgression implements Progression<BattleFaintedEvent> {
        private PlayerProfile profile;
        public DefeatPokemonRequirement requirement;
        private int progressAmount;

        public DefeatPokemonProgression(PlayerProfile profile, DefeatPokemonRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<BattleFaintedEvent> getType() {
            return BattleFaintedEvent.class;
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

        private UUID getPlayer(BattleFaintedEvent event) {
            var opponents = event.getKilled().getFacedOpponents();
            for (BattlePokemon battlePokemon : opponents) {
                for (UUID playerUUID : battlePokemon.actor.getPlayerUUIDs()) {
                    if (playerUUID == profile.getUUID()) {
                        return playerUUID;
                    }
                }
            }
            return null;
        }

        @Override
        public boolean meetsCriteria(BattleFaintedEvent event) {
            Pokemon pokemon = event.getKilled().getEffectedPokemon();
            String pokename = pokemon.getSpecies().getName();
            boolean shiny = pokemon.getShiny();
            List<ElementalType> types = StreamSupport.stream(pokemon.getTypes().spliterator(), false).collect(Collectors.toUnmodifiableList());
            long time_of_day = CobbleChallengeMod.getMinecraftServer().getPlayerList().getPlayer(getPlayer(event)).level().getDayTime();
            boolean is_legendary = pokemon.isLegendary();
            boolean is_ultra_beast = pokemon.isUltraBeast();

            if (!StringUtils.doesStringContainCategory(requirement.pokename.split("/"), pokename)) {
                return false;
            }

            if (requirement.shiny && !shiny) {
                return false;
            }

            if (!requirement.pokemon_type.toLowerCase().startsWith("any") &&
                    types.stream().map(ElementalType::getName).noneMatch(requirement.pokemon_type::equalsIgnoreCase)) {
                return false;
            }

            if (!requirement.time_of_day.toLowerCase().startsWith("any") &&
                    !PixelmonUtils.doesDaytimeMatch(time_of_day, requirement.time_of_day)) {
                return false;
            }

            if (requirement.is_legendary && !is_legendary) {
                return false;
            }

            if (requirement.is_ultra_beast && !is_ultra_beast) {
                return false;
            }

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
