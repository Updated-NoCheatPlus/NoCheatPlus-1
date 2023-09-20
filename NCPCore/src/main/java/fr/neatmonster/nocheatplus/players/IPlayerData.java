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

import java.util.Collection;
import java.util.UUID;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.config.value.OverrideType;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.data.IDataOnRemoveSubCheckData;
import fr.neatmonster.nocheatplus.components.data.checktype.IBaseDataAccess;
import fr.neatmonster.nocheatplus.components.registry.IGetGenericInstance;
import fr.neatmonster.nocheatplus.hooks.ExemptionContext;
import fr.neatmonster.nocheatplus.permissions.RegisteredPermission;
import fr.neatmonster.nocheatplus.worlds.IWorldData;
import fr.neatmonster.nocheatplus.worlds.WorldIdentifier;

public interface IPlayerData extends IData, IBaseDataAccess, IGetGenericInstance {

    /**
     * Get the (supposedly exact) player name, as had been passed at the time of
     * object creation.
     * 
     * @return
     */
    public String getPlayerName();

    /**
     * Get the player name in lower case, as had been passed at the time of
     * object creation.
     * 
     * @return
     */
    public String getPlayerNameLowerCase();

    /**
     * Get the unique id for the player.
     * 
     * @return
     */
    public UUID getPlayerId();

    /**
     * Get the System.currentTimeMillis() value from player join handling.
     * 
     * @return
     */
    public long getLastJoinTime();

    /**
     * Permission check (thread-safe, results and impact of asynchronous queries depends on
     * settings +- TBD).
     * 
     * @param registeredPermission Must not be null, must be registered.
     * @param player
     *            May be null (if lucky the permission is set to static/timed
     *            and/or has already been fetched).
     * @return
     */
    public boolean hasPermission(final RegisteredPermission registeredPermission, final Player player);

    /**
     * Request a permission cache update.
     * @param registeredPermission
     */
    public void requestPermissionUpdate(final RegisteredPermission registeredPermission);

    /**
     * Low priority permission update for check type specific permissions.
     * 
     * @param registeredPermissions
     *            May be null.
     */
    public void requestLazyPermissionUpdate(final RegisteredPermission... registeredPermissions);

    /**
     * Mimic legacy behavior (non-nested) - exempt including descendants
     * recursively. Note that contexts other than
     * ExemptionContext.LEGACY_NON_NESTED will not be touched.
     * 
     * @param checkType
     */
    public void exempt(final CheckType checkType);

    /**
     * Mimic legacy behavior (non-nested) - unexempt including descendants
     * recursively. Note that contexts other than
     * ExemptionContext.LEGACY_NON_NESTED will not be touched.
     * <hr>
     * Primary thread and asynchronous access are separated and yield different
     * results, it's imperative to always unexempt properly for asyncrhonous
     * thread contexts, as isExempted reflects a mixture of both.
     * 
     * @param checkType
     */
    public void unexempt(final CheckType checkType);

    /**
     * Exempt with reference to the given context with descendants recursively.
     * <br>
     * Note that multiple calls to exempt demand multiple calls to
     * unexempt(CheckType, ExemptionContext).
     * <hr>
     * Primary thread and asynchronous access are separated and yield different
     * results, it's imperative to always unexempt properly for asyncrhonous
     * thread contexts, as isExempted reflects a mixture of both.
     * 
     * @param checkType
     * @param context
     */
    public void exempt(final CheckType checkType, final ExemptionContext context);

    /**
     * Unexempt once, including descendants recursively. <br>
     * Note that for multiple calls to exempt with one context, multiple calls
     * to unexempt with that context may be necessary to fully unexempt, or call
     * unexemptAll for the context.
     * <hr>
     * ExemptionContext.LEGACY_NON_NESTED is not automatically calling
     * unexemptAll as is done with the legacy signature unexempt(CheckType).
     * <hr>
     * Primary thread and asynchronous access are separated and yield different
     * results, it's imperative to always unexempt properly for asyncrhonous
     * thread contexts, as isExempted reflects a mixture of both.
     * 
     * @param checkType
     * @param context
     */
    public void unexempt(final CheckType checkType, final ExemptionContext context);

    /**
     * Remove all (potentially nested) entries context for the given checkType
     * and descendants recursively.
     * <hr>
     * Primary thread and asynchronous access are separated and yield different
     * results, it's imperative to always unexempt properly for asyncrhonous
     * thread contexts, as isExempted reflects a mixture of both.
     * 
     * @param checkType
     * @param context
     */
    public void unexemptAll(final CheckType checkType, final ExemptionContext context);

    /**
     * Test for exemption.
     * <hr>
     * Thread-safe read (not synchronized).
     * 
     * @param checkType
     * @return
     */
    public boolean isExempted(final CheckType checkType);

    /**
     * Clear all exemptions, for all thread contexts.
     * <hr>
     * Call from the primary thread only.
     */
    public void clearAllExemptions();

    /**
     * Clear all exemptions for the given checkType and descendants recursively,
     * for all thread contexts.
     * <hr>
     * Call from the primary thread only.
     * 
     * @param checkType
     */
    public void clearAllExemptions(final CheckType checkType);

    /**
     * Will get set on player join and world change. Currently NOT on login.
     * 
     * @return
     */
    public WorldIdentifier getCurrentWorldIdentifier();

    /**
     * Get the currently stored IWorldData instance.
     * 
     * @return Might return null, if not initialized - to get the default world
     *         data in that case, use instead:
     *         {@link #getCurrentWorldDataSafe()}
     */
    public IWorldData getCurrentWorldData();

    /**
     * Convenience method: Return the default world data, if currentWorldData is
     * null.
     * 
     * @return
     */
    public IWorldData getCurrentWorldDataSafe();

    /**
     * Full activation check (configuration, exemption, permission).
     * 
     * @param checkType
     * @param player
     * @return
     */
    public boolean isCheckActive(final CheckType checkType, final Player player);

    /**
     * Full activation check (configuration, exemption, permission).
     * 
     * @param checkType
     * @param player
     * @param worldData
     * @return
     */
    public boolean isCheckActive(final CheckType checkType, final Player player,
            final IWorldData worldData);

    /**
     * Bypass check including exemption and permission.
     * 
     * @param checkType
     * @param player
     * @return
     */
    public boolean hasBypass(final CheckType checkType, final Player player);

    /**
     * Reset to the current world data (or the default one).
     */
    public void resetDebug();

    /**
     * Reset to the current world data (or the default one).
     * 
     * @param checkType
     */
    public void resetDebug(final CheckType checkType);

    /**
     * Override debug flags.
     * 
     * @param checkType
     * @param active
     * @param overrideType
     * @param overrideChildren
     */
    public void overrideDebug(final CheckType checkType, final AlmostBoolean active, 
            final OverrideType overrideType, final boolean overrideChildren);

    @Override
    public <T> T getGenericInstance(Class<T> registeredFor);

    /**
     * Remove data from the cache (not from underlying factories, nor from per
     * world storage.
     * 
     * @param registeredFor
     */
    public <T> void removeGenericInstance(Class<T> registeredFor);

    /**
     * Remove all generic instances from cache, which are contained in the given
     * collection.
     * 
     * @param types
     */
    public void removeAllGenericInstances(Collection<Class<?>> types);

    /**
     * Call dataOnRemoveSubCheckData(...).
     * 
     * @param subCheckRemoval
     */
    public void removeSubCheckData(
            Collection<Class<? extends IDataOnRemoveSubCheckData>> subCheckRemoval,
            Collection<CheckType> checkTypes);

    /**
     * Check if notifications are turned off, this does not bypass permission
     * checks.
     * 
     * @return
     */
    public boolean getNotifyOff();

    /**
     * Allow or turn off notifications. A player must have the admin.notify
     * permission to receive notifications.
     * 
     * @param notifyOff
     *            set to true to turn off notifications.
     */
    public void setNotifyOff(final boolean notifyOff);
    
    /**
     * Check if player join via geysermc
     * 
     * @return
     */
    public boolean isBedrockPlayer();

    /**
     * Set the state player connect through geysermc
     * 
     * @param bedrockPlayer
     */
    public void setBedrockPlayer(final boolean bedrockPlayer);

    /**
     * Let the inventory be updated (run in TickTask).
     */
    public void requestUpdateInventory();

    /**
     * Let the player be set back to the location stored in moving data (run in
     * TickTask). Only applies if it's set there.
     */
    public void requestPlayerSetBack();

    /**
     * Test if it's set to process a player set back on tick. This does not
     * check MovingData.hasTeleported().
     * 
     * @return
     */
    public boolean isPlayerSetBackScheduled();

    /**
     * Get the client's version protocol ID through ViaVersion or ProtocolSupport. <br>
     * Requires CompatNoCheatPlus (subject to change)
     * @see https://wiki.vg/Protocol_version_numbers
     * 
     * @return -1, if it cannot be determined.
     */
    public int getClientVersionID();

    /**
     * Get the client's version (translated from the protocol ID) through ViaVersion or ProtocolSupport. <br>
     * Requires CompatNoCheatPlus (subject to change)
     * 
     * @return ClientVersion.UNKNOWN, if it cannot be determined.
     */
    public ClientVersion getClientVersion();

    /**
     * Set the client's version protocol ID as given by ProtocolSupport or ViaVersion.
     * Currently done externally, through CompatNoCheatPlus (subject to change)
     * 
     * @param ID
     */
    public void setClientVersionID(final int ID);

    /**
     * Set the sneaking state of the client.<br>
     * There are cases where the information sent to the server can be incosistent, so we need to estimate
     * ourselves if the player could be sneaking.
     * 
     * @param sneaking
     */
    public void setSneaking(final boolean sneaking);

    /**
     * Gets the sneaking state of the client, as set by us.
     * @return True, if sneaking.
     */
    public boolean isSneaking();

    /**
     * Set the sprinting state of the client.<br>
     * There are cases where the information sent to the server can be incosistent, so we need to estimate
     * ourselves if the player could be sprinting.
     * 
     * @param sprinting
     */
    public void setSprinting(final boolean sprinting);
    
    /**
     * This ensures that sprinting is actually possible (i.e.: not with low food level)
     * @return True, if sprinting
     */
    public boolean isSprinting();

    /**
     * Set the using item state of the client.<br>
     * Needed because Minecraft nor Bukkit provides us with a method (except for player#isBlocking())
     * 
     * @param usingItem
     */
    public void setUsingItem(final boolean usingItem);
    
    /**
     * Gets the using item state of the client, as set by us.
     * @return True, if using an item
     */
    public boolean isUsingItem();
}