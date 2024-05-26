/*
 * This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package fr.neatmonster.nocheatplus.compat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;

import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.BoundingBox;

public class Folia {
    private static final boolean RegionizedServer = ReflectionUtil.getClass("io.papermc.paper.threadedregions.RegionizedServer") != null;
    //private static final Class<?> AsyncScheduler = ReflectionUtil.getClass("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
    private static final Class<?> GlobalRegionScheduler = ReflectionUtil.getClass("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
    private static final Class<?> EntityScheduler = ReflectionUtil.getClass("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
    private static final boolean isFoliaServer = RegionizedServer && GlobalRegionScheduler != null && EntityScheduler != null; // && AsyncScheduler != null
    
    /**
     * @return Whether the server is running Folia
     */
    public static boolean isFoliaServer() {
        return isFoliaServer;
    }

    /**
     * Run an async task, either with bukkit scheduler or Java
     * @param plugin Plugin to assign for
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @return An int represent for task id when running on Paper/Spigot or Thread when on Folia or null if can't schedule
     */
    public static Object runAsyncTask(Plugin plugin, Consumer<Object> run) {
        if (!isFoliaServer) {
            return Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run.accept(null)).getTaskId();
        }
        //try {
        //    Method getSchedulerMethod = ReflectionUtil.getMethodNoArgs(Server.class, "getAsyncScheduler", AsyncScheduler);
        //    Object asyncScheduler = getSchedulerMethod.invoke(Bukkit.getServer());

        //    Class<?> schedulerClass = asyncScheduler.getClass();
        //    Method executeMethod = schedulerClass.getMethod("runNow", Plugin.class, Consumer.class);

        //    Object taskInfo = executeMethod.invoke(asyncScheduler, plugin, run);
        //    return taskInfo;
        //}
        //catch (Exception e) {
            // Second attempt, should be happened during onDisable calling from BukkitLogNodeDispatcher
            Thread thread = Executors.defaultThreadFactory().newThread(() -> run.accept(null));
            if (thread == null) return null;
            thread.run();
            return thread;
        //}
    }

    /**
     * Run a repeating task, either with bukkit scheduler or folia scheduler
     * @param plugin Plugin to assign for
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @param delay Delay in ticks
     * @param period Period in ticks
     * @return An int represent for task id when running on Paper/Spigot or a ScheduledTask when running on Folia or null if can't schedule
     */
    public static Object runSyncRepatingTask(Plugin plugin, Consumer<Object> run, long delay, long period) {
        if (!isFoliaServer) {
            return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> run.accept(null), delay, period);
        }
        try {
            Method getSchedulerMethod = ReflectionUtil.getMethodNoArgs(Server.class, "getGlobalRegionScheduler", GlobalRegionScheduler);
            //ReflectionUtil.invokeMethod(getSchedulerMethod, Bukkit.getServer());
            Object syncScheduler = getSchedulerMethod.invoke(Bukkit.getServer());

            Class<?> schedulerClass = syncScheduler.getClass();
            //ReflectionUtil.getMethod(schedulerClass, "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            Method executeMethod = schedulerClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);

            //ReflectionUtil.invokeMethod(executeMethod, syncScheduler, plugin, run, delay, period);
            Object taskInfo = executeMethod.invoke(syncScheduler, plugin, run, delay, period);
            return taskInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Run a task, either with bukkit scheduler or folia scheduler
     * @param plugin Plugin to assign for
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @return An int represent for task id when running on Paper/Spigot or a ScheduledTask when running on Folia or null if can't schedule
     */
    public static Object runSyncTask(Plugin plugin, Consumer<Object> run) {
        if (!isFoliaServer) {
            return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> run.accept(null));
        }
        try {
            Method getSchedulerMethod = ReflectionUtil.getMethodNoArgs(Server.class, "getGlobalRegionScheduler", GlobalRegionScheduler);
            Object syncScheduler = getSchedulerMethod.invoke(Bukkit.getServer());

            Class<?> schedulerClass = syncScheduler.getClass();
            Method executeMethod = schedulerClass.getMethod("run", Plugin.class, Consumer.class);

            Object taskInfo = executeMethod.invoke(syncScheduler, plugin, run);
            return taskInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Run a delayed task, either with bukkit scheduler or folia scheduler
     * @param plugin Plugin to assign for
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @param delay Delay in ticks
     * @return An int represent for task id when running on Paper/Spigot or a ScheduledTask when running on Folia or null if can't schedule
     */
    public static Object runSyncDelayedTask(Plugin plugin, Consumer<Object> run, long delay) {
        if (!isFoliaServer) {
            return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> run.accept(null), delay);
        }
        try {
            Method getSchedulerMethod = ReflectionUtil.getMethodNoArgs(Server.class, "getGlobalRegionScheduler", GlobalRegionScheduler);
            Object syncScheduler = getSchedulerMethod.invoke(Bukkit.getServer());

            Class<?> schedulerClass = syncScheduler.getClass();
            Method executeMethod = schedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);

            Object taskInfo = executeMethod.invoke(syncScheduler, plugin, run, delay);
            return taskInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Run a delayed task for an entity on next tick, either with bukkit scheduler or folia scheduler
     * @param entity The Entity to assign a task
     * @param plugin Plugin to assign for
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @param retired The task to run if entity is retired before the task is run
     * @return An int represent for task id when running on Paper/Spigot or a ScheduledTask when running on Folia or null if can't schedule
     */
    public static Object runSyncTaskForEntity(Entity entity, Plugin plugin, Consumer<Object> run, Runnable retired) {
        return runSyncDelayedTaskForEntity(entity, plugin, run, retired, 1L);
    }
    
    /**
     * Run a delayed task for an entity on next tick, either with bukkit scheduler or folia scheduler
     * @param entity The Entity to assign a task
     * @param plugin Plugin to assign for
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @param retired The task to run if entity is retired before the task is run
     * @param delay Delay in ticks
     * @return An int represent for task id when running on Paper/Spigot or a ScheduledTask when running on Folia or null if can't schedule
     */
    public static Object runSyncDelayedTaskForEntity(Entity entity, Plugin plugin, Consumer<Object> run, Runnable retired, long delay) {
        if (!isFoliaServer) {
            return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> run.accept(null), delay);
        }
        try {
            Method getSchedulerMethod = ReflectionUtil.getMethodNoArgs(Entity.class, "getScheduler", EntityScheduler);
            Object syncEntityScheduler = getSchedulerMethod.invoke(entity);

            Class<?> schedulerClass = syncEntityScheduler.getClass();
            Method executeMethod = schedulerClass.getMethod("runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);

            Object taskInfo = executeMethod.invoke(syncEntityScheduler, plugin, run, retired, delay);
            return taskInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cancel a specific task
     * @param o The task to cancel (can be an int taskID on Paper/Spigot or a ScheduledTask on Folia. If null, does nothing!
     */
    public static void cancelTask(Object o) {
        if (o == null) return;
        if (o instanceof Thread) return;// o = null ?
        if (o instanceof Integer) {
            int taskId = (int)o;
            Bukkit.getScheduler().cancelTask(taskId);
        } else {
            Method cancelMethod = ReflectionUtil.getMethodNoArgs(o.getClass(), "cancel");
            ReflectionUtil.invokeMethodNoArgs(cancelMethod, o);
        }
    }

    /**
     * Cancel all tasks from given plugin
     * @param plugin Plugin to assign for
     */
    public static void cancelTasks(Plugin plugin) {
        if (!isFoliaServer) {
            Bukkit.getScheduler().cancelTasks(plugin);
        } else {
            try {
                Method getGlobalRegionSchedulerMethod = ReflectionUtil.getMethodNoArgs(Server.class, "getGlobalRegionScheduler", GlobalRegionScheduler);
                //Method getAsyncSchedulerMethod = ReflectionUtil.getMethodNoArgs(Server.class, "getAsyncScheduler", AsyncScheduler);
                
                Object syncScheduler = getGlobalRegionSchedulerMethod.invoke(Bukkit.getServer());
                //Object asyncScheduler = getAsyncSchedulerMethod.invoke(Bukkit.getServer());

                Class<?> schedulerClass = syncScheduler.getClass();
                Method executeMethod = schedulerClass.getMethod("cancelTasks", Plugin.class);
                executeMethod.invoke(syncScheduler, plugin);
                
                //schedulerClass = asyncScheduler.getClass();
                //executeMethod = schedulerClass.getMethod("cancelTasks", Plugin.class);
                //executeMethod.invoke(asyncScheduler, plugin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean teleportEntity(Entity entity, Location loc, TeleportCause cause) {
        if (!isFoliaServer) {
            return entity.teleport(loc, cause);
        }
        try {
            Method teleportAsyncMethod = ReflectionUtil.getMethod(Entity.class, "teleportAsync", Location.class, TeleportCause.class);
            Object result = ReflectionUtil.invokeMethod(teleportAsyncMethod, entity, loc, cause);
            CompletableFuture<Boolean> res = (CompletableFuture<Boolean>) result;
            return res.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Collection<Entity> getNearbyEntities(final Block block, final BoundingBox boundingBox, final Predicate<Entity> filter) {
        final World world = block.getWorld();
        if (isFoliaServer) {
            final int minChunkX = boundingBox.getMin().getBlockX() >> 4;
            final int maxChunkX = boundingBox.getMax().getBlockX() >> 4;
            final int minChunkZ = boundingBox.getMin().getBlockZ() >> 4;
            final int maxChunkZ = boundingBox.getMax().getBlockZ() >> 4;
            final List<Entity> nearbyEntities = new ArrayList<>();
            for (int chunkX = minChunkX; chunkX <= maxChunkX; ++chunkX) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; ++chunkZ) {
                    if (!world.isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }
                    final Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    if (!chunk.isEntitiesLoaded()) {
                        continue;
                    }
                    for (final Entity entity : chunk.getEntities()) {
                        if ((filter == null || filter.test(entity)) && boundingBox.overlaps(entity.getBoundingBox())) {
                            nearbyEntities.add(entity);
                        }
                    }
                }
            }
            return nearbyEntities;
        } else {
            return world.getNearbyEntities(boundingBox, filter);
        }
    }

    /**
     * @param object task number or ScheduledTask or Thread
     * @return Whether the task is scheduled
     */
    public static boolean isTaskScheduled(Object task) {
        if (task == null) return false;
        if (task instanceof Integer) return (int)task != -1;
        return true;
    } 
}
