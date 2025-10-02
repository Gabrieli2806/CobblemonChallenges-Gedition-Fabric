package com.github.kuramastone.cobblemonChallenges.listeners;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.events.berry.BerryHarvestEvent;
import com.cobblemon.mod.common.api.events.farming.ApricornHarvestEvent;
import com.cobblemon.mod.common.api.events.fishing.BobberSpawnPokemonEvent;
import com.cobblemon.mod.common.api.events.pokedex.scanning.PokemonScannedEvent;
import com.cobblemon.mod.common.api.events.pokemon.*;
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent;
import com.cobblemon.mod.common.api.events.pokemon.interaction.ExperienceCandyUseEvent;
import com.cobblemon.mod.common.api.events.storage.ReleasePokemonEvent;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeAPI;
import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.BreedPokemonRequirement;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.DrawPokemonRequirement;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.HatchPokemonRequirement;
import com.github.kuramastone.cobblemonChallenges.events.*;
import com.github.kuramastone.cobblemonChallenges.player.ChallengeProgress;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
import kotlin.Unit;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ChallengeListener {

    private static CobbleChallengeAPI api;

    public static void register() {
        api = CobbleChallengeMod.instance.getAPI();
    }

    public static void passEvent(Object event, UUID player) {

        PlayerProfile profile = api.getOrCreateProfile(player);
        for (ChallengeProgress activeChallenge : profile.getActiveChallenges()) {
            activeChallenge.progress(event);
        }

    }

    public static void passEvent(Object event, Player player) {
        try {
            if (player != null) {
                passEvent(event, player.getUUID());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onBlockBreak(BlockBreakEvent event) {
        passEvent(event, event.getPlayer());
    }

    public static void onChallengeCompleted(ChallengeCompletedEvent event) {
        passEvent(event, event.getPlayerProfile().getUUID());
    }

    public static void onBlockPlace(BlockPlaceEvent event) {
        passEvent(event, event.getPlayer());
    }

    public static Unit onPokemonCaptured(PokemonCapturedEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onPokemonPokedexScanned(PokemonScannedEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onBattleVictory(BattleVictoryEvent event) {
        for (BattleActor winner : event.getWinners()) {
            for (UUID playerUUID : winner.getPlayerUUIDs()) {
                passEvent(event, playerUUID);
            }
        }
        return null;
    }

    public static Unit onFainted(BattleFaintedEvent event) {
        var opponents = event.getKilled().getFacedOpponents();
        Set<UUID> ids = new HashSet<>();
        for (BattlePokemon battlePokemon : opponents) {
            for (UUID playerUUID : battlePokemon.actor.getPlayerUUIDs()) {
                ids.add(playerUUID);
            }
        }

        for (UUID playerUUID : ids) {
            passEvent(event, playerUUID);
        }

        return null;
    }

    public static Unit onEvolution(EvolutionCompleteEvent event) {
        passEvent(event, event.getPokemon().getOwnerPlayer());
        return null;
    }

    public static Unit onApricornHarvest(ApricornHarvestEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onBerryHarvest(BerryHarvestEvent event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onPokemonPokedexSeen(PokemonSeenEvent event) {
        passEvent(event, event.getPlayerId());
        return null;
    }

    public static Unit onRareCandyUsed(ExperienceCandyUseEvent.Post event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static void on1SecondPlayed(Played1SecondEvent event) {
        passEvent(event, event.getPlayerProfile().getPlayerEntity());
    }

    public static Unit onEggHatch(HatchEggEvent.Post event) {
        passEvent(event, event.getPlayer());
        return null;
    }

    public static Unit onExpGained(ExperienceGainedPostEvent event) {
        if (event.getPokemon().getOwnerPlayer() != null)
            passEvent(event, event.getPokemon().getOwnerPlayer());
        return null;
    }

    public static Unit onLevelUp(LevelUpEvent event) {
        if (event.getPokemon().getOwnerPlayer() != null)
            passEvent(event, event.getPokemon().getOwnerPlayer());
        return null;
    }

    public static Unit onTradeCompleted(TradeCompletedEvent event) {
        passEvent(event, event.getTradeParticipant1().getUuid());
        passEvent(event, event.getTradeParticipant2().getUuid());
        return null;
    }

    public static Unit onFossilRevived(FossilRevivedEvent event) {
        if (event.getPlayer() == null)
            return null;
        passEvent(event, event.getPlayer());
        return null;
    }

    public static void onPlayerJoin(PlayerJoinEvent event) {
        // delay this to allow player to fully join before triggering
        TickScheduler.scheduleLater(60L, () -> {
            // Force initialization of automatic challenges when player joins
            PlayerProfile profile = api.getOrCreateProfile(event.getPlayer().getUUID());
            profile.addUnrestrictedChallenges(); // Ensure automatic challenges are added
            passEvent(event, event.getPlayer());
        });
    }

    public static Unit onBobberSpawnPokemon(BobberSpawnPokemonEvent.Post post) {
        passEvent(post, Objects.requireNonNull(post.getBobber().getPlayerOwner()));
        return null;
    }

    public static Unit onReleasePokemon(ReleasePokemonEvent.Post post) {
        passEvent(post, post.getPlayer());
        return null;
    }

    public static Unit onBreed(HatchPokemonRequirement.EggHatchedEventData data) {
        passEvent(data, data.player());
        return null;
    }

    public static Unit onConversion(BreedPokemonRequirement.BreedEventData data) {
        passEvent(data, data.player());
        return null;
    }

    public static Unit onDraw(DrawPokemonRequirement.DrawEventData data) {
        passEvent(data, data.pokemon().getOwnerPlayer());
        return null;
    }
}
