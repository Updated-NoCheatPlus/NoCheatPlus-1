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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;

import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * Utility class to provide compatibility with Paper's regionized multi-threaded server implementation (a.k.a.: Folia), using reflection.
 * If the server is not running Folia, use Bukkit's scheduler.
 */
public class SchedulerHelper {

    private static final boolean RegionizedServer = ReflectionUtil.getClass("io.papermc.paper.threadedregions.RegionizedServer") != null;
    // private static final Class<?> AsyncScheduler = ReflectionUtil.getClass("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
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
     * Run an asyncronous task, either with Bukkit's scheduler or Java's if the server is using Folia.
     * 
     * @param plugin Plugin to assign for
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @return An int, representing for task ID when running on Paper/Spigot, or Thread when on Folia (or null if unable to schedule)
     */
    public static Object runTaskAsync(Plugin plugin, Consumer<Object> run) {
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
            // Second attempt, should be happening during onDisable calling from BukkitLogNodeDispatcher
            Thread thread = Executors.defaultThreadFactory().newThread(() -> run.accept(null));
            if (thread == null) return null;
            thread.run();
            return thread;
        //}
    }
    
    /**
     * Schedule a once off task to occur as soon as possible, either with Bukkit's scheduler or Folia's.
     * 
     * @param plugin Plugin that owns the task
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @return An int, representing for task ID when running on Paper/Spigot, or ScheduledTask when on Folia (or null if unable to schedule)
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
     * Schedule a repeating task, either with Bukkit's scheduler or Folia's.
     * 
     * @param plugin Plugin that owns the task
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @param delay Delay in server ticks before executing the first repeat.
     * @param period Period in server ticks, for which the task will be repeated.
     * @return An int, representing for task ID when running on Paper/Spigot, or ScheduledTask when on Folia (or null if unable to schedule)
     */
    public static Object runSyncRepeatingTask(Plugin plugin, Consumer<Object> run, long delay, long period) {
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
     * Run a delayed task, either with Bukkit's scheduler or Folia's.
     * 
     * @param plugin Plugin that owns the task
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @param delay Delay in ticks
     * @return An int, representing a task ID when running on Paper/Spigot, or ScheduledTask when on Folia (or null if unable to schedule)
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
     * Run a delayed task for an entity on the next tick, either with Bukkit's scheduler or Folia's.
     * 
     * @param entity Entity that owns the task
     * @param plugin Plugin that owns the task
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @param retired The task to run if entity is retired before the task is run
     * @return An int, representing for task ID when running on Paper/Spigot, or ScheduledTask when on Folia (or null if unable to schedule)
     */
    public static Object runSyncTaskForEntity(Entity entity, Plugin plugin, Consumer<Object> run, Runnable retired) {
        return runSyncDelayedTaskForEntity(entity, plugin, run, retired, 1L);
    }
    
    /**
     * Run a delayed task for an entity, either with Bukkit's scheduler or Folia's.
     * 
     * @param entity Entity that owns the task
     * @param plugin Plugin that owns the task
     * @param run Consumer that accepts an object or null, for Folia or Paper/Spigot respectively
     * @param retired The task to run if entity is retired before the task is run
     * @param delay Delay in ticks
     * @return An int, representing for task ID when running on Paper/Spigot, or ScheduledTask when on Folia (or null if unable to schedule)
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
     * Cancel the given task
     * 
     * @param task The task to cancel. On Paper/Spigot this is an int, representing the task ID; on Folia, a ScheduledTask obj. 
     *             Won't do anything if the given object is null, or Thread.
     */
    public static void cancelTask(Object task) {
        if (task == null) {
            return;
        }
        if (task instanceof Thread) {
            return; // task = null ?
        }
        if (task instanceof Integer) {
            int taskId = (int)task;
            Bukkit.getScheduler().cancelTask(taskId);
        } 
        else {
            Method cancelMethod = ReflectionUtil.getMethodNoArgs(task.getClass(), "cancel");
            ReflectionUtil.invokeMethodNoArgs(cancelMethod, task);
        }
    }

    /**
     * Cancel all scheduled tasks for the given plugin
     * 
     * @param plugin Plugin to assign for
     */
    public static void cancelTasks(Plugin plugin) {
        if (!isFoliaServer) {
            Bukkit.getScheduler().cancelTasks(plugin);
        } 
        else {
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
    
    /**
     * Asyncronously teleports the given entity if the server is using Folia
     * 
     * @param entity The entity to teleport
     * @param loc Target location
     * @param cause the TeleportCause enum.
     */
    @SuppressWarnings("unchecked")
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

    /**
     * @param task The task ID (Paper/Spigot/Bukkit), ScheduledTask or Thread (Folia)
     *             May be null (would yield false)
     * @return Whether the given task is scheduled.
     */
    public static boolean isTaskScheduled(Object task) {
        if (task == null) {
            return false;
        }
        if (task instanceof Integer) {
            return (int)task != -1;
        }
        return true;
    } 
}
