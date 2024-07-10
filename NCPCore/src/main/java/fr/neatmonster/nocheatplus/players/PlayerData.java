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
package fr.neatmonster.nocheatplus.players;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.config.value.OverrideType;
import fr.neatmonster.nocheatplus.components.data.*;
import fr.neatmonster.nocheatplus.hooks.ExemptionContext;
import fr.neatmonster.nocheatplus.hooks.NCPExemptionManager;
import fr.neatmonster.nocheatplus.permissions.PermissionInfo;
import fr.neatmonster.nocheatplus.permissions.PermissionNode;
import fr.neatmonster.nocheatplus.permissions.PermissionPolicy.FetchingPolicy;
import fr.neatmonster.nocheatplus.permissions.PermissionRegistry;
import fr.neatmonster.nocheatplus.permissions.RegisteredPermission;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.ds.corw.DualSet;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;
import fr.neatmonster.nocheatplus.utilities.ds.map.HashMapLOW;
import fr.neatmonster.nocheatplus.utilities.ds.map.InstanceMapLOW;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.worlds.IWorldData;
import fr.neatmonster.nocheatplus.worlds.WorldDataManager;
import fr.neatmonster.nocheatplus.worlds.WorldIdentifier;

/**
 * Central player-specific data object.
 * <ul>
 * <li>Access to on-tick request functionality.</li>
 * <li>TBD: Permission cache.</li>
 * <li>TBD: Check data.</li>
 * <li>TBD: Exemptions</li>
 * <li>...</li>
 * </ul>
 * <hr>
 * Creating PlayerData must always be thread-safe and fail-safe.
 * <hr>
 * OLD javadocs to be cleaned up (...):<br>
 * On the medium run this is intended to carry all data for the player...
 * <li>Checks data objects.</li>
 * <li>Time stamps for logged out players</li>
 * <li>Data to be persisted, like set backs, xray.</li> <br>
 * Might contain...
 * <li>References of configs.</li>
 * <li>Exemption entries.</li>
 * <li>Player references
 * <li>
 * <hr>
 * Main reasons are...
 * <li>Faster cross-check data access both for check and data management.</li>
 * <li>Have the data in one place, easy to control and manage.</li>
 * <li>Easier transition towards non-static access, if it should ever
 * happen.</li>
 * <hr>
 * (not complete)<br>
 * Might contain individual settings such as debug flags, exemption,
 * notification settings, task references.
 * 
 * @author asofold
 *
 */
public class PlayerData implements IPlayerData {

    // TODO: IPlayerData for the more official API.

    /** Monitor player task load across all players (nanoseconds per server tick). */
    private static ActionFrequency taskLoad = new ActionFrequency(6, 7);
    private static final int ticksMonitored = taskLoad.numberOfBuckets() * (int) taskLoad.bucketDuration();
    private static final long msMonitored = ticksMonitored * 50;
    /**
     * Some measure for heavy load, for skipping some of the (lazy) updating.
     * Assume 1% of a tick in average as heavy - just for this task. This still
     * is problematic, if System.nanoTime() just has milliseconds resolution,
     * because we'll be adding up zeros most of the time here. Perhaps just
     * relaying to TickTask.getLag is enough.
     */
    private static float heavyLoad = 500000f / (float) ticksMonitored;

    // Default tags.
    // TODO: Move elsewhere (API?)
    public static final String TAG_NOTIFY_OFF = "notify_off";
    /** Optimistic player data creation. */
    public static final String TAG_OPTIMISTIC_CREATE = "optimistic_create";

    private static final short frequentTaskLazyDefaultDelay = 10;
    private static final short frequentTaskUnregisterDefaultDelay = 2;

    //////////////
    // Instance //
    //////////////

    /** Per player lock. */
    /*
     * TODO: Impact of using this everywhere is uncertain. For exemptions and
     * permissions it'll be ok, because nodes get created once for most, but for
     * permission updates (merge primary thread) and the like, it'll not be as
     * certain.
     */
    private final Lock lock = new ReentrantLock();

    /** Not sure this is the future of extra properties. */
    private Set<String> tags = null;

    /*
     * TODO: Concept for updating names for UUIDs -> + OfflinePlayerData,
     * uncertain when/how to access.
     */
    /** Unique id of the player. */
    private final UUID playerId;

    // TODO: Names could/should get updated. (In which case?)
    /** Exact case name of the player. */
    private String playerName;
    /** Lower case name of the player. */
    private String playerNameLowerCase;

    private long lastJoinTime = 0;

    /** The IWorldData instance of the current world (at least while online). */
    private IWorldData currentWorldData = null;

    /*
     * TODO: Flags/counters for (async-login,) login, join, 'online', kick, quit
     * + shouldBeOnline(). 'online' means that some action has been recorded.
     * Same/deduce: isFake(), as opposed to ExemptionSettings.isRegardedAsNPC().
     */

    /** A reference for handling the permission cache with policies. */
    private final PermissionRegistry permissionRegistry;

    /** Permission cache. */
    private final HashMapLOW<Integer, PermissionNode> permissions = new HashMapLOW<Integer, PermissionNode>(lock, 35);
    // TODO: a per entry typed variant (key - value relation)?
    private final InstanceMapLOW dataCache = new InstanceMapLOW(lock, 24);

    /** If is Bedrock Player. This is set if CompatNoCheatPlus is present. */
    private boolean bedrockPlayer = false;
    private boolean requestUpdateInventory = false;
    private boolean requestItemUseResync = false;
    private boolean requestPlayerSetBack = false;
    private int versionID = -1;
    private ClientVersion clientVersion = ClientVersion.UNKNOWN;
    private boolean sneaking = false;
    private boolean sprinting = false;
    private Material itemInUse = null;

    private boolean frequentPlayerTaskShouldBeScheduled = false;
    /** Actually queried ones. */
    private final DualSet<RegisteredPermission> updatePermissions = new DualSet<RegisteredPermission>(lock);
    /** Possibly needed in future. */
    private final DualSet<RegisteredPermission> updatePermissionsLazy = new DualSet<RegisteredPermission>(lock);

    /** TODO: Soon to add minimized offline data, so these kind of things don't impact as much. */
    private final PlayerCheckTypeTree checkTypeTree = new PlayerCheckTypeTree(lock);

    /** Unregister the tasks once 0 count is reached. */
    private short frequentTaskDelayUnregister = 0;
    /** Run lazy stuff on reaching zero. */
    private short frequentTaskDelayLazy = 0;

    /**
     * Note that PlayerData might get instantiated optimistically, and then be
     * discarded, if the map already contains one - due to the implementation of
     * concurrency support.
     * 
     * @param playerName
     *            Should be accurate case - TBD: ensure it's used that way,
     *            and/or update with login/join.
     */
    public PlayerData(final UUID playerId, final String playerName, final PermissionRegistry permissionRegistry) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.playerNameLowerCase = playerName.toLowerCase();
        this.permissionRegistry = permissionRegistry;
    }

    void updatePlayerName(final String exactPlayerName) {
        this.playerName = exactPlayerName;
        this.playerNameLowerCase = exactPlayerName.toLowerCase();
    }

    /**
     * Run with DataManager frequent tasks (each tick).
     * 
     * @param tick
     * @param timeLast
     * @return True, of the listener is to be removed, false otherwise.
     */
    boolean processTickFrequent(final int tick, final long timeLast) {
        if (frequentTaskDelayUnregister == 0) {
            frequentTaskDelayUnregister = frequentTaskUnregisterDefaultDelay;
        }
        if (frequentTaskDelayLazy == 0) {
            frequentTaskDelayLazy = frequentTaskLazyDefaultDelay;
        }
        boolean busy = false;
        final Player player = DataManager.getPlayer(playerId);
        if (hasFrequentTasks()) {
            frequentTasks(tick, timeLast, player);
            busy = true;
        }
        if (--frequentTaskDelayLazy == 0) {
            if (!lazyTasks(tick, timeLast, player)) {
                busy = true;
            }
        }
        if (busy || frequentTaskDelayLazy > 0) {
            frequentTaskDelayUnregister = frequentTaskUnregisterDefaultDelay;
            return false;
        }
        else if (--frequentTaskDelayUnregister == 0) {
            // Check for lazy task.
            if (hasLazyTasks() || hasFrequentTasks()) {
                frequentTaskDelayLazy = frequentTaskLazyDefaultDelay;
                return false;
            }
            else {
                // Unregister.
                frequentPlayerTaskShouldBeScheduled = false; // uh oh.
                return true;
            }
        }
        else {
            // Stay a while.
            return false;
        }
    }

    private boolean hasFrequentTasks() {
        return !updatePermissions.isEmtpyAfterMergePrimaryThread() 
                // Should be primary thread:
                || requestPlayerSetBack || requestUpdateInventory || requestItemUseResync;
    }

    private void frequentTasks(final int tick, final long timeLast, final Player player) {
        if (player != null) { // Common criteria ...
            if (player.isOnline()) {
                long nanos = System.nanoTime();
                // Set back.
                if (requestPlayerSetBack) {
                    requestPlayerSetBack = false;
                    MovingUtil.processStoredSetBack(player, "Player set back on tick: ", this);
                }
                // Inventory update.
                if (requestUpdateInventory) {
                    requestUpdateInventory = false;
                    player.updateInventory();
                }
                if (requestItemUseResync) {
                    requestItemUseResync = false;
                    InventoryUtil.itemResyncTask(player, this);
                }
                // Permission updates (high priority).
                final Collection<RegisteredPermission> updatable = updatePermissions.getMergePrimaryThreadAndClear();
                if (updatable != null) {
                    for (final RegisteredPermission registeredPermission : updatable) {
                        // (Force update could be inefficient.)
                        hasPermission(registeredPermission, player);
                    }
                }
                nanos = System.nanoTime() - nanos;
                if (nanos > 0L) {
                    taskLoad.add(tick, nanos);
                }
            } // (The player is online.)
        } // (The player is not null.)
    }

    private boolean hasLazyTasks() {
        return !updatePermissionsLazy.isEmtpyAfterMergePrimaryThread();
    }

    /**
     * 
     * @param tick
     * @param timeLast
     * @return True, once all lazy stuff has been processed (no unregister delay
     *         here).
     */
    private boolean lazyTasks(final int tick, final long timeLast, final Player player) {
        if (player == null) {
            return true;
        }
        long nanos = System.nanoTime();
        taskLoad.update(tick);
        final boolean isHeavyLoad = taskLoad.score(1f) > heavyLoad || TickTask.getLag(msMonitored, true) > 1.1f;
        updatePermissionsLazy.mergePrimaryThread();
        final Iterator<RegisteredPermission> it = updatePermissionsLazy.iteratorPrimaryThread();
        // TODO: Load balancing with other tasks ?
        while (it.hasNext()) {
            hasPermission(it.next(), player);
            it.remove();
            if (isHeavyLoad) {
                break;
            }
        }
        boolean hasWrk = it.hasNext();
        nanos = System.nanoTime() - nanos;
        if (nanos > 0L) {
            taskLoad.add(tick, nanos);
        }
        return !hasWrk;
    }

    private void registerFrequentPlayerTask() {
        if (Bukkit.isPrimaryThread()) {
            registerFrequentPlayerTaskPrimaryThread();
        }
        else registerFrequentPlayerTaskAsynchronous();
    }

    private void registerFrequentPlayerTaskPrimaryThread() {
        frequentPlayerTaskShouldBeScheduled = true;
        DataManager.registerFrequentPlayerTaskPrimaryThread(playerId);
    }

    private void registerFrequentPlayerTaskAsynchronous() {
        frequentPlayerTaskShouldBeScheduled = true;
        DataManager.registerFrequentPlayerTaskAsynchronous(playerId);
    }

    private boolean isFrequentPlayerTaskScheduled() {
        return DataManager.isFrequentPlayerTaskScheduled(playerId);
    }

    private PermissionNode getOrCreatePermissionNode(final RegisteredPermission registeredPermission) {
        // Optimistic creation (concurrency).
        final PermissionNode node = new PermissionNode(permissionRegistry.getPermissionInfo(registeredPermission.getId()));
        final PermissionNode oldNode = permissions.putIfAbsent(registeredPermission.getId(), node);
        return oldNode == null ? node : oldNode;
    }

    /**
     * Fetch the permission hard, no putting to cache, just return the result.
     * For off-primary-server-thread access, this will wait for a
     * BukkitRunnable/TickTask task to finish, at an extraordinary performance
     * penalty.
     * 
     * @param registeredPermission
     * @param player
     * @return MAYBE in case permissions could not be fetched or 
     */
    private AlmostBoolean fetchPermission(final RegisteredPermission registeredPermission, Player player) {
        if (Bukkit.isPrimaryThread()) {
            if (player == null) {
                player = DataManager.getPlayer(this.playerId);
                if (player == null) {
                    return AlmostBoolean.MAYBE;
                }
            }
            // Minimal update within the primary thread.
            return player.hasPermission(registeredPermission.getBukkitPermission()) ? AlmostBoolean.YES : AlmostBoolean.NO;
        }
        else {
            requestPermissionUpdate(registeredPermission);
            /*
             * TODO: UNCERTAIN: request related permission right away ? Inefficient in case of exemption.
             * 
             */
            return AlmostBoolean.MAYBE;
        }
    }

    public void handleTimeRanBackwards(final Collection<Class<? extends IData>> dataTypes) {
        // Permissions.
        final Iterator<Entry<Integer, PermissionNode>> it = permissions.iterator();
        final long timeNow = System.currentTimeMillis();
        while (it.hasNext()) {
            final PermissionNode node = it.next().getValue();
            switch (node.getFetchingPolicy()) {
                case INTERVAL:
                    node.invalidate();
                    break;
                default:
                    if (node.getLastFetch() > timeNow) {
                        node.setState(node.getLastState(), timeNow);
                    }
                    break;
            }
        }
        // TODO: Register explicitly or not? (+ auto register?)...
        for (final Class<? extends IData> type : dataTypes) {
            final IData obj = dataCache.get(type);
            if (obj != null && obj instanceof ICanHandleTimeRunningBackwards) {
                ((ICanHandleTimeRunningBackwards) obj).handleTimeRanBackwards();
            }
        }
    }

    void requestPermissionUpdatePrimaryThread(final RegisteredPermission registeredPermission) {
        // Might throw something :p.
        updatePermissions.addPrimaryThread(registeredPermission);
        registerFrequentPlayerTaskPrimaryThread();
    }

    void requestPermissionUpdateAsynchronous(final RegisteredPermission registeredPermission) {
        updatePermissions.addAsynchronous(registeredPermission);
        registerFrequentPlayerTaskAsynchronous();
    }

    private void requestLazyPermissionsUpdateNonEmpty(final RegisteredPermission... registeredPermissions) {
        if (Bukkit.isPrimaryThread()) {
            requestLazyPermissionUpdatePrimaryThread(registeredPermissions);
        }
        else {
            requestLazyPermissionUpdateAsynchronous(registeredPermissions);
        }
    }

    void requestLazyPermissionUpdatePrimaryThread(final RegisteredPermission... registeredPermissions) {
        // Might throw something :p.
        updatePermissionsLazy.addAllPrimaryThread(Arrays.asList(registeredPermissions));
        registerFrequentPlayerTaskPrimaryThread();
    }

    void requestLazyPermissionUpdateAsynchronous(final RegisteredPermission... registeredPermissions) {
        updatePermissionsLazy.addAllAsynchronous(Arrays.asList(registeredPermissions));
        registerFrequentPlayerTaskAsynchronous();
    }

    void onPlayerLeave(final Player player, final long timeNow, Collection<Class<? extends IDataOnLeave>> types) {
        // (Might collect to be removed types first.)
        for (final Class<? extends IDataOnLeave> type : types) {
            final IDataOnLeave instance = dataCache.get(type);
            if (instance != null && instance.dataOnLeave(player, this)) {
                dataCache.remove(type);
            }
        }
        // (Somewhat reversed order of invalidation.)
        invalidateOffline();
        bedrockPlayer = false;
        sneaking = false;
        sprinting = false;
        versionID = -1;
        itemInUse = null;
        clientVersion = ClientVersion.UNKNOWN;
    }

    /**
     * Early adaption on player join.
     * 
     * @param world
     * @param timeNow
     * @param types 
     */
    void onPlayerJoin(final Player player, final World world, final long timeNow, final WorldDataManager worldDataManager, 
                      final Collection<Class<? extends IDataOnJoin>> types) {
        // Only update world if the data hasn't just been created.
        updateCurrentWorld(world, worldDataManager);
        invalidateOffline();
        for (final Class<? extends IDataOnJoin> type : types) {
            final IDataOnJoin instance = dataCache.get(type);
            if (instance != null && instance.dataOnJoin(player, this)) {
                dataCache.remove(type);
            }
        }
        requestLazyPermissionUpdate(permissionRegistry.getPreferKeepUpdatedOffline());
        lastJoinTime = timeNow;
    }

    private void updateCurrentWorld(final World world, final WorldDataManager worldDataManager) {
        updateCurrentWorld(worldDataManager.getWorldData(world));
    }

    /**
     * Allow direct call from DataManager after object creation.
     * 
     * @param worldData
     */
    void updateCurrentWorld(final IWorldData worldData) {
        // TODO: Consider storing last world too.
        if (currentWorldData != worldData) {
            currentWorldData = worldData;
            checkTypeTree.getNode(CheckType.ALL).updateDebug(worldData);
        }
    }

    private void invalidateOffline() {
        final Iterator<Entry<Integer, PermissionNode>> it = permissions.iterator();
        // TODO: More efficient: get unmodifiable collection from registry?
        while (it.hasNext()) {
            final PermissionNode node = it.next().getValue();
            final PermissionInfo info = node.getPermissionInfo();
            if (info.invalidationOffline()
                    /*
                     * TODO: world based should only be invalidated with world
                     * changing. Therefore store the last world info
                     * (UUID/name?) in PlayerData and use on login for
                     * comparison.
                     */
                    || info.invalidationWorld()) {
                // TODO: Really count leave as world change?
                node.invalidate();
            }
        }
    }

    /**
     * Early adaption on world change.
     * 
     * @param oldWorld
     * @param newWorld
     * @param types 
     */
    void onPlayerChangedWorld(final Player player, final World oldWorld, final World newWorld,
                              final WorldDataManager worldDataManager, final Collection<Class<? extends IDataOnWorldChange>> types) {
        updateCurrentWorld(newWorld, worldDataManager);
        // TODO: Double-invalidation (previous policy and target world policy)
        final Iterator<Entry<Integer, PermissionNode>> it = permissions.iterator();
        // TODO: More efficient: get unmodifiable collection from registry?
        while (it.hasNext()) {
            final PermissionNode node = it.next().getValue();
            final PermissionInfo info = node.getPermissionInfo();
            if (info.invalidationWorld()) {
                node.invalidate();
            }
        }
        requestLazyPermissionUpdate(permissionRegistry.getPreferKeepUpdatedWorld());
        for (final Class<? extends IDataOnWorldChange> type : types) {
            final IDataOnWorldChange instance = dataCache.get(type);
            if (instance != null && instance.dataOnWorldChange(player, this, oldWorld, newWorld)) {
                dataCache.remove(type);
            }
        }
    }

    @Override
    public boolean hasPermission(final RegisteredPermission registeredPermission, final Player player) {
        // Check cache and policy. 
        PermissionNode node = permissions.get(registeredPermission.getId());
        if (node == null) {
            node = getOrCreatePermissionNode(registeredPermission);
        }
        final FetchingPolicy fetchingPolicy = node.getFetchingPolicy();
        switch (fetchingPolicy) {
            case TRUE:
                return true;
            case FALSE:
                return false;
            default:
                // ONCE, TIMED, ALWAYS.
        }
        final AlmostBoolean lastState = node.getLastState();
        if (lastState != AlmostBoolean.MAYBE) {
            switch (fetchingPolicy) {
                case ONCE:
                    return lastState.decide();
                case INTERVAL:
                    if (System.currentTimeMillis() - node.getLastFetch() < node.getFetchInterval()) {
                        return lastState.decide();
                    }
                    // TODO: ALWAYS: Could still use cache within a check context.
                default:
                    // Must fetch.
                    break;
            }
        }
        // Permission not cached or needs to be updated with the PlayerTask.
        final AlmostBoolean fRes = fetchPermission(registeredPermission, player);
        if (fRes == AlmostBoolean.MAYBE) {
            // TODO: Get default state from info or Bukkit. Might need lastState for asynchronous ... redesign.
            return lastState.decide(); // TODO: Slight risk on left-over meant-temporary permissions.
        }
        else {
            node.setState(fRes, System.currentTimeMillis());
            return fRes.decide();
        }
    }

    /**
     * Remove extra stored data, keeping "essential" data if set so. "Essential"
     * data can't be recovered once deleted, like set-back locations for players
     * who leave in-air (once stored here at all).
     * 
     * @param keepEssentialData
     */
    public void removeData(boolean keepEssentialData) {
        // TODO: Interface / stages.
        permissions.clear(); // Might keep login-related permissions. Implement a 'retain-xy' or 'essential' flag?
        updatePermissions.clearPrimaryThread();
        updatePermissionsLazy.clearPrimaryThread();
        dataCache.clear();
    }

    /**
     * Bypass check including exemption and permission.
     * 
     * @param checkType
     * @param player
     * @param isPrimaryThread
     * @return
     */
    public boolean hasBypass(final CheckType checkType, final Player player, final IWorldData worldData) {
        // TODO: Expose or not.
        // Exemption check.
        // TODO: More efficient implementation, ExemptionSettings per world in worldData.
        if (NCPExemptionManager.isExempted(player, checkType)) {
            return true;
        }
        // Check permission policy/cache regardless of the thread context.
        final RegisteredPermission permission = checkType.getPermission();
        if (permission != null && hasPermission(permission, player)) {
            return true;
        }
        return false;
    }

    /**
     * Test if present.
     * 
     * @param tag
     * @return
     */
    public boolean hasTag(final String tag) {
        return tags != null && tags.contains(tag);
    }

    /**
     * Add the tag.
     * 
     * @param tag
     */
    public void addTag(final String tag) {
        if (tags == null) {
            tags = new HashSet<String>();
        }
        tags.add(tag);
    }

    /**
     * Remove the tag.
     * 
     * @param tag
     */
    public void removeTag(final String tag) {
        if (tags != null) {
            tags.remove(tag);
            if (tags.isEmpty()) {
                tags = null;
            }
        }
    }

    /**
     * Add tag or remove tag, based on arguments.
     * 
     * @param tag
     * @param add
     *            The tag will be added, if set to true. If set to false, the
     *            tag will be removed.
     */
    public void setTag(final String tag, final boolean add) {
        if (add) {
            addTag(tag);
        }
        else removeTag(tag);
    }

    private <T> T cacheMissGenericInstance(final Class<T> registeredFor) {
        // 2. Check for registered factory (local)
        // TODO: Might store PlayerDataManager here.
        T res = DataManager.getFromFactory(registeredFor, 
                new PlayerFactoryArgument(this, getCurrentWorldDataSafe()));
        if (res != null) {
            return  putDataCache(registeredFor, res);
        }
        // 3. Check proxy registry.
        res = getCurrentWorldDataSafe().getGenericInstance(registeredFor);
        return res == null ? null : putDataCache(registeredFor, res);
    }

    private <T> T putDataCache(final Class<T> registeredFor, final T instance) {
        final T previousInstance = (T) dataCache.putIfAbsent(registeredFor, instance); // Under lock.
        return previousInstance == null ? instance : previousInstance;
    }

    /**
     * Called with adjusting to the configuration (enable / config reload).
     * @param changedPermissions 
     */
    public void adjustSettings(final Set<RegisteredPermission> changedPermissions) {
        final Iterator<RegisteredPermission> it = changedPermissions.iterator();
        while (it.hasNext()) {
            final PermissionNode node = permissions.get(it.next().getId());
            if (node != null) {
                node.invalidate();
            }
        }
    }

    public void onWorldUnload(final World world, final Collection<Class<? extends IDataOnWorldUnload>> types) {
        for (final Class<? extends IDataOnWorldUnload> type : types) {
            final IDataOnWorldUnload instance = dataCache.get(type);
            if (instance != null && instance.dataOnWorldUnload(world, this)) {
                dataCache.remove(type);
            }
        }
    }

    public void onReload(final Collection<Class<? extends IDataOnReload>> types) {
        for (final Class<? extends IDataOnReload> type : types) {
            final IDataOnReload instance = dataCache.get(type);
            if (instance != null && instance.dataOnReload(this)) {
                dataCache.remove(type);
            }
        }
    }

    @Override
    public void requestPermissionUpdate(final RegisteredPermission registeredPermission) {
        if (Bukkit.isPrimaryThread()) {
            requestPermissionUpdatePrimaryThread(registeredPermission);
        }
        else requestPermissionUpdateAsynchronous(registeredPermission);
    }

    @Override
    public void requestLazyPermissionUpdate(final RegisteredPermission... registeredPermissions) {
        if (registeredPermissions == null || registeredPermissions.length == 0) {
            return;
        }
        else requestLazyPermissionsUpdateNonEmpty(registeredPermissions);
    }

    /**
     * Get a data/config instance (1.local cache, 2. player related factory, 3.
     * world registry).
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getGenericInstance(final Class<T> registeredFor) {
        // 1. Check for cache (local).
        final Object res = dataCache.get(registeredFor);
        if (res == null) {
            /*
             * TODO: Consider storing null and check containsKey(registeredFor)
             * here. On the other hand it's not intended to query non existent
             * data (just yet).
             */
            return cacheMissGenericInstance(registeredFor);
        }
        else {
            return (T) res;
        }
    }

    @Override
    public boolean getNotifyOff() {
        return hasTag(TAG_NOTIFY_OFF);
    }

    @Override
    public void setNotifyOff(final boolean notifyOff) {
        setTag(TAG_NOTIFY_OFF, notifyOff);
    }

    @Override
    public void setCrouchingState(final boolean sneaking) {
        this.sneaking = sneaking;
    }

    @Override
    public void setSprintingState(final boolean sprinting) {
        this.sprinting = sprinting;
    }

    @Override
    public void setItemInUseState(final Material itemInUse) {
        this.itemInUse = itemInUse;
    }
    
    @Override
    public void setBedrockPlayer(final boolean bedrockPlayer) {
        this.bedrockPlayer = bedrockPlayer;
    }
    
    @Override
    public boolean isBedrockPlayer() {
        return bedrockPlayer;
    }

    @Override
    public Material getItemInUse() {
        return itemInUse;
    }

    @Override
    public boolean isSprinting() {
        return sprinting;
    }

    @Override
    public boolean isCrouching() {
        return sneaking;
    }

    @Override
    public void requestUpdateInventory() {
        this.requestUpdateInventory = true;
        registerFrequentPlayerTask();
    }

    @Override
    public void requestItemUseResync() {
        this.requestItemUseResync = true;
        registerFrequentPlayerTask();
    }

    @Override
    public void requestPlayerSetBack() {
        this.requestPlayerSetBack = true;
        registerFrequentPlayerTask();
    }

    @Override
    public boolean isPlayerSetBackScheduled() {
        return this.requestPlayerSetBack && (frequentPlayerTaskShouldBeScheduled || isFrequentPlayerTaskScheduled());
    }

    @Override
    public int getClientVersionID() {
        return versionID;
    }

    @Override
    public ClientVersion getClientVersion() {
        return clientVersion;
    }

    @Override
    public void setClientVersionID(final int versionID) {
        this.versionID = versionID;
        this.clientVersion = ClientVersion.getById(versionID);
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public String getPlayerNameLowerCase() {
        return playerNameLowerCase;
    }

    @Override
    public UUID getPlayerId() {
        return playerId;
    }

    @Override
    public long getLastJoinTime() {
        return lastJoinTime;
    }

    @Override
    public void exempt(final CheckType checkType) {
        checkTypeTree.exempt(checkType, ExemptionContext.LEGACY_NON_NESTED);
        // TODO: Handlers?
    }

    @Override
    public void unexempt(final CheckType checkType) {
        checkTypeTree.unexemptAll(checkType, ExemptionContext.LEGACY_NON_NESTED);
        // TODO: Handlers?
    }

    @Override
    public void exempt(final CheckType checkType, final ExemptionContext context) {
        checkTypeTree.exempt(checkType, context);
    }

    @Override
    public void unexempt(final CheckType checkType, final ExemptionContext context) {
        checkTypeTree.unexempt(checkType, context);
    }


    @Override
    public void unexemptAll(final CheckType checkType, final ExemptionContext context) {
        checkTypeTree.unexemptAll(checkType, context);
    }

    @Override
    public boolean isExempted(final CheckType checkType) {
        return checkTypeTree.isExempted(checkType);
    }

    @Override
    public void clearAllExemptions() {
        checkTypeTree.clearAllExemptions();
    }

    @Override
    public void clearAllExemptions(final CheckType checkType) {
        checkTypeTree.clearAllExemptions(checkType);
    }

    @Override
    public WorldIdentifier getCurrentWorldIdentifier() {
        return currentWorldData.getWorldIdentifier();
    }

    @Override
    public IWorldData getCurrentWorldData() {
        return currentWorldData;
    }

    @Override
    public IWorldData getCurrentWorldDataSafe() {
        return currentWorldData == null ? NCPAPIProvider.getNoCheatPlusAPI().getWorldDataManager().getDefaultWorldData() : currentWorldData;
    }

    @Override
    public boolean isCheckActive(final CheckType checkType, final Player player) {
        // (No consistency checks for player id / world.)
        return isCheckActive(checkType, player, getCurrentWorldDataSafe());
    }

    @Override
    public boolean isCheckActive(final CheckType checkType, final Player player, final IWorldData worldData) {
        // TODO: Move the implementation of CheckUtils here (efficiency with exemption).
        return worldData.isCheckActive(checkType) && !hasBypass(checkType, player, worldData);
    }

    @Override
    public boolean hasBypass(final CheckType checkType, final Player player) {
        return hasBypass(checkType, player, getCurrentWorldDataSafe());
    }

    @Override
    public boolean isDebugActive(final CheckType checkType) {
        return checkTypeTree.getNode(checkType).isDebugActive();
    }

    @Override
    public void resetDebug() {
        resetDebug(CheckType.ALL);
    }

    @Override
    public void resetDebug(final CheckType checkType) {
        this.checkTypeTree.getNode(checkType).resetDebug(currentWorldData == null ? getCurrentWorldDataSafe() : currentWorldData);
    }

    @Override
    public void overrideDebug(final CheckType checkType, final AlmostBoolean active, final OverrideType overrideType, final boolean overrideChildren) {
        this.checkTypeTree.getNode(checkType).overrideDebug(checkType, active, overrideType, overrideChildren);
    }

    /** Remove from cache. */
    @Override
    public <T> void removeGenericInstance(final Class<T> type) {
        dataCache.remove(type);
    }

    @Override
    public void removeAllGenericInstances(final Collection<Class<?>> types) {
        if (dataCache.isEmpty()) {
            return;
        }
        dataCache.remove(types);
    }

    @Override
    public void removeSubCheckData(final Collection<Class<? extends IDataOnRemoveSubCheckData>> types,final Collection<CheckType> checkTypes) {
        final Collection<Class<?>> removeTypes = new LinkedList<Class<?>>();
        for (final Class<? extends IDataOnRemoveSubCheckData> type : types) {
            final IDataOnRemoveSubCheckData impl = (IDataOnRemoveSubCheckData) dataCache.get(type);
            if (impl != null) {
                if (impl.dataOnRemoveSubCheckData(checkTypes)) {
                    removeTypes.add(type);
                }
            }
        }
        if (!removeTypes.isEmpty()) {
            dataCache.remove(removeTypes);
        }
    }
}