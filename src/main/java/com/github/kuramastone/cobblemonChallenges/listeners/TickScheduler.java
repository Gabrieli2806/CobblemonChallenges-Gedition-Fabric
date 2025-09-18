package com.github.kuramastone.cobblemonChallenges.listeners;

import com.github.kuramastone.cobblemonChallenges.CobbleChallengeMod;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class TickScheduler {

    private static long tick = 0;
    private static final Map<Long, List<Runnable>> scheduledFutureTick = new HashMap<>();
    private static final Map<Callable<Boolean>, Long> repeatingTasks = new HashMap<>();

    // Method to queue the task for the next tick
    public static void scheduleNextTick(Runnable task) {
        scheduledFutureTick.computeIfAbsent(tick + 1, (t) -> new ArrayList<>()).add(task);
    }

    // Method to queue the task for the next tick
    public static void scheduleLater(long tickDelay, Runnable task) {
        scheduledFutureTick.computeIfAbsent(tick + tickDelay, (t) -> new ArrayList<>()).add(task);
    }

    // Method to schedule a repeating task every 'intervalTicks' ticks
    public static ForgeTask scheduleRepeating(long intervalTicks, Callable<Boolean> task) {

        ForgeTask forgeTask = new ForgeTask();
        Runnable loopRun = () -> {
            if(!forgeTask.isCancelled()) {

                boolean shouldRepeat = true; // scheduling continues even if an error occurs
                try {
                    if (!task.call())
                        shouldRepeat = false;
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if(shouldRepeat) {
                    rescheduleRepeatingTask(intervalTicks, task, forgeTask);
                }
            }
        };

        scheduleLater(intervalTicks, loopRun);
        return forgeTask;
    }

    // Reschedule the task after it's run
    private static void rescheduleRepeatingTask(long intervalTicks, Callable<Boolean> task, ForgeTask forgeTask) {
        repeatingTasks.put(task, intervalTicks);
        scheduleLater(intervalTicks, () -> {
            try {
                if(!forgeTask.isCancelled() && task.call())
                    rescheduleRepeatingTask(intervalTicks, task, forgeTask);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // This event is fired every server tick
    public static void onServerTick(MinecraftServer server) {
        // We only want to run this on the END phase to ensure everything is processed in one tick
            // Run all scheduled tasks
            List<Runnable> tasksToRun = scheduledFutureTick.get(tick);

            if (tasksToRun != null) {
                scheduledFutureTick.remove(tick);
                for (Runnable task : tasksToRun) {
                    try {
                        task.run();
                    }
                    catch (Exception e) {
                        CobbleChallengeMod.logger.error("Error during scheduled runnable's execution.");
                        e.printStackTrace();
                    }
                }
            }

            tick++;
    }

    public static class ForgeTask {
        private boolean isCancelled = false;

        public ForgeTask() {
        }

        public void setCancelled(boolean cancelled) {
            isCancelled = cancelled;
        }

        public boolean isCancelled() {
            return isCancelled;
        }
    }

}