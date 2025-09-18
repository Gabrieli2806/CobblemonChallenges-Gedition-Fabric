package com.github.kuramastone.cobblemonChallenges.challenges.requirements;

import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.github.kuramastone.bUtilities.yaml.YamlConfig;
import com.github.kuramastone.bUtilities.yaml.YamlKey;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import com.github.kuramastone.cobblemonChallenges.utils.PixelmonUtils;
import com.github.kuramastone.cobblemonChallenges.utils.StringUtils;
// import dev.neovitalism.neodaycare.utils.DaycareUtils; // Commented out to avoid dependency issues
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

public class DrawPokemonRequirement implements Requirement {
    public static final String ID = "draw_pokemon";

    public record DrawEventData(ItemStack itemStack, Pokemon pokemon) {}

    @YamlKey("pokename")
    private String pokename = "any";
    @YamlKey("amount")
    private int amount = 1;

    @YamlKey("shiny")
    private boolean shiny = false;
    @YamlKey("pokemon_type")
    private String pokemon_type = "any";
    @YamlKey("ball")
    private String ball = "any";
    @YamlKey("time_of_day")
    private String time_of_day = "any";
    @YamlKey("is_legendary")
    private boolean is_legendary = false;
    @YamlKey("is_ultra_beast")
    private boolean is_ultra_beast = false;
    @YamlKey("is_mythical")
    private boolean is_mythical = false;
    @YamlKey("is_egg")
    private boolean is_egg = false;


    public DrawPokemonRequirement() {
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
        return new DrawPokemonProgression(profile, this);
    }

    // Static nested Progression class
    public static class DrawPokemonProgression implements Progression<DrawEventData> {
        private PlayerProfile profile;
        private DrawPokemonRequirement requirement;
        private int progressAmount;

        public DrawPokemonProgression(PlayerProfile profile, DrawPokemonRequirement requirement) {
            this.profile = profile;
            this.requirement = requirement;
            this.progressAmount = 0;
        }

        @Override
        public Class<DrawEventData> getType() {
            return DrawEventData.class;
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
        public boolean meetsCriteria(DrawEventData event) {
            Pokemon pokemon = event.pokemon();
            String pokename = pokemon.getSpecies().getName();
            boolean shiny = pokemon.getShiny();
            List<ElementalType> types = StreamSupport.stream(pokemon.getTypes().spliterator(), false).toList();
            String ballName = pokemon.getCaughtBall().getName().toString();
            long time_of_day = event.pokemon.getOwnerPlayer().level().getDayTime();
            boolean is_legendary = pokemon.isLegendary();
            boolean is_ultra_beast = pokemon.isUltraBeast();
            boolean is_mythical = pokemon.isMythical();

            // boolean is_egg = DaycareUtils.isEgg(pokemon); // NeoDaycare integration disabled
            boolean is_egg = false; // Default to false when NeoDaycare is not available

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

            if (!requirement.ball.toLowerCase().startsWith("any") &&
                    !ballName.toLowerCase().contains(requirement.ball.toLowerCase())) {
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

            if (requirement.is_egg && !is_egg) {
                return false;
            }

            if (requirement.is_mythical && !is_mythical) {
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
            return PlainTextComponentSerializer.plainText().serialize(CobbleChallengeMod.instance.getAPI().getMiniMessage("challenges.progression-string",
                    "{current}", String.valueOf(this.progressAmount),
                    "{target}", String.valueOf(this.requirement.amount)));
        }
    }
}