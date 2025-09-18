package com.github.kuramastone.cobblemonChallenges.events;

import com.github.kuramastone.cobblemonChallenges.listeners.ChallengeListener;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BlockBreakEvent {

    private BlockState blockState;
    private BlockPos blockPos;
    private Level level;
    private Player player;

    public BlockBreakEvent(BlockState blockState, BlockPos blockPos, Level level, Player player) {
        this.blockState = blockState;
        this.blockPos = blockPos;
        this.level = level;
        this.player = player;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public Level getLevel() {
        return level;
    }

    public Player getPlayer() {
        return player;
    }

    public static void register() {
        // Register the block break callback
        PlayerBlockBreakEvents.AFTER.register(BlockBreakEvent::trigger);
    }

    private static void trigger(Level level, Player player, BlockPos blockPos, BlockState blockState, BlockEntity blockEntity) {
        ChallengeListener.onBlockBreak(new BlockBreakEvent(blockState, blockPos, level, player));
    }

}
