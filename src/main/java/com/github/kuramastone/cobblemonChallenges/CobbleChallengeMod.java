package com.github.kuramastone.cobblemonChallenges;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.github.kuramastone.cobblemonChallenges.challenges.ChallengeList;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.BreedPokemonRequirement;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.DrawPokemonRequirement;
import com.github.kuramastone.cobblemonChallenges.challenges.requirements.HatchPokemonRequirement;
import com.github.kuramastone.cobblemonChallenges.commands.ChallengeListArgument;
import com.github.kuramastone.cobblemonChallenges.commands.ChallengesCommands;
import com.github.kuramastone.cobblemonChallenges.commands.OldCommandHandler;
import com.github.kuramastone.cobblemonChallenges.events.BlockBreakEvent;
import com.github.kuramastone.cobblemonChallenges.events.BlockPlaceEvent;
import com.github.kuramastone.cobblemonChallenges.events.PlayTimeScheduler;
import com.github.kuramastone.cobblemonChallenges.events.PlayerJoinEvent;
import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import com.github.kuramastone.cobblemonChallenges.listeners.TickScheduler;
import com.github.kuramastone.cobblemonChallenges.player.PlayerProfile;
// import dev.neovitalism.neodaycare.api.NeoDaycareEvents; // Commented out to avoid dependency issues
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import revxrsal.commands.Lamp;
import revxrsal.commands.fabric.FabricLamp;
import revxrsal.commands.fabric.FabricLampConfig;
import revxrsal.commands.fabric.actor.FabricCommandActor;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class CobbleChallengeMod implements ModInitializer {

    public static String MODID = "cobblemonchallenges";

    public static CobbleChallengeMod instance;
    public static final Logger logger = LogManager.getLogger(MODID);
    private static MinecraftServer minecraftServer;
    private CobbleChallengeAPI api;
    private volatile FabricServerAudiences adventure;

    public static boolean rotationTestingMode = false; // Enable shorter intervals for testing
    public static boolean preventRotationOnReload = false; // Prevent rotation during reload/initialization


    @Override
    public void onInitialize() {
        instance = this;
        api = new CobbleChallengeAPI();

        ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted); // capture minecraftserver
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> onStopped());
        startSaveScheduler();
        startRepeatableScheduler();
        startRotationScheduler();
        OldCommandHandler.register();
//        registerCommands();
        registerTrackedEvents();

    }

    private void registerCommands() {
        Lamp<FabricCommandActor> lamp = FabricLamp.builder(FabricLampConfig.createDefault())
                .parameterTypes( it -> {
                    it.addParameterType(ChallengeList.class, new ChallengeListArgument(api));
                })
                .build();
        lamp.register(new ChallengesCommands());
    }

    private void onServerStarted(MinecraftServer server) {
        minecraftServer = server;

        // Initialize Adventure Platform Fabric
        this.adventure = FabricServerAudiences.of(server);

        // Prevent rotation during initialization/reload
        preventRotationOnReload = true;
        api.init();
        preventRotationOnReload = false;

        // NeoDaycare integration disabled - no event registration needed
    }

    private void onServerStopped(MinecraftServer server) {
        this.adventure = null;
    }

    public FabricServerAudiences adventure() {
        FabricServerAudiences ret = this.adventure;
        if (ret == null) {
            throw new IllegalStateException("Tried to access Adventure without a running server!");
        }
        return ret;
    }


    private void startSaveScheduler() {
        TickScheduler.scheduleRepeating(20 * 60 * 30, () -> {
            CompletableFuture.runAsync(() -> api.saveProfiles());
            return true;
        });
    }

    private void startRepeatableScheduler() {
        TickScheduler.scheduleRepeating(20, () -> {

            for (PlayerProfile profile : api.getProfiles()) {
                profile.refreshRepeatableChallenges();
            }

            return true;
        });
    }
    
    private void startRotationScheduler() {
        // Check for mission rotation every 10 minutes (or every 30 seconds in testing mode)
        TickScheduler.scheduleRepeating(20 * 30, () -> { // Check every 30 seconds for faster testing feedback
            if (api != null) {
                for (ChallengeList challengeList : api.getChallengeLists()) {
                    if (rotationTestingMode) {
                        challengeList.checkAndPerformRotationTesting();
                    } else {
                        challengeList.checkAndPerformRotation();
                    }
                }
            }
            return true;
        });
    }

    private void onStopped() {
        api.saveProfiles();
    }

    private void registerTrackedEvents() {
        ServerTickEvents.START_SERVER_TICK.register(TickScheduler::onServerTick);

        ChallengeListener.register();
        BlockBreakEvent.register();
        BlockPlaceEvent.register();
        PlayerJoinEvent.register();
        ServerTickEvents.START_SERVER_TICK.register(PlayTimeScheduler::onServerTick);
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonCaptured);
        CobblemonEvents.POKEMON_SCANNED.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonPokedexScanned);
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.HIGHEST, ChallengeListener::onBattleVictory);
        CobblemonEvents.BATTLE_FAINTED.subscribe(Priority.HIGHEST, ChallengeListener::onFainted);
        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe(Priority.HIGHEST, ChallengeListener::onBobberSpawnPokemon);
        CobblemonEvents.POKEMON_RELEASED_EVENT_POST.subscribe(Priority.HIGHEST, ChallengeListener::onReleasePokemon);
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(Priority.HIGHEST, ChallengeListener::onEvolution);
        CobblemonEvents.APRICORN_HARVESTED.subscribe(Priority.HIGHEST, ChallengeListener::onApricornHarvest);
        CobblemonEvents.BERRY_HARVEST.subscribe(Priority.HIGHEST, ChallengeListener::onBerryHarvest);
        CobblemonEvents.POKEMON_SEEN.subscribe(Priority.HIGHEST, ChallengeListener::onPokemonPokedexSeen);
        CobblemonEvents.EXPERIENCE_CANDY_USE_POST.subscribe(Priority.HIGHEST, ChallengeListener::onRareCandyUsed);
        CobblemonEvents.HATCH_EGG_POST.subscribe(Priority.HIGHEST, ChallengeListener::onEggHatch);
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_POST.subscribe(Priority.HIGHEST, ChallengeListener::onExpGained);
        CobblemonEvents.LEVEL_UP_EVENT.subscribe(Priority.HIGHEST, ChallengeListener::onLevelUp);
        CobblemonEvents.TRADE_COMPLETED.subscribe(Priority.HIGHEST, ChallengeListener::onTradeCompleted);
        CobblemonEvents.FOSSIL_REVIVED.subscribe(Priority.HIGHEST, ChallengeListener::onFossilRevived);

        // NeoDaycare integration disabled to avoid dependency issues
        /*
        if (FabricLoader.getInstance().isModLoaded("neodaycare")) {
            try {
                NeoDaycareEvents.EGG_HATCHED.register((a,b) -> ChallengeListener.onBreed(new HatchPokemonRequirement.EggHatchedEventData(ServerPlayer.class.cast(a), b)));
                NeoDaycareEvents.EGG_CREATE.register((a,b, c, d) -> ChallengeListener.onConversion(new BreedPokemonRequirement.BreedEventData(ServerPlayer.class.cast(a), b,c,d)));
                NeoDaycareEvents.DRAW_EGG.register((a,b) -> ChallengeListener.onDraw(new DrawPokemonRequirement.DrawEventData(ItemStack.class.cast(a), b)));
                logger.info("NeoDaycare integration enabled");
            } catch (Exception e) {
                logger.warn("NeoDaycare mod detected but integration failed: " + e.getMessage());
            }
        }
        */
    }

    public CobbleChallengeAPI getAPI() {
        return api;
    }

    public static MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public static File defaultDataFolder() {
        return new File(FabricLoader.getInstance().getConfigDir().toFile(), MODID);
    }
}
