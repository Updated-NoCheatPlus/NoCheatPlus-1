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
package fr.neatmonster.nocheatplus.checks.net;

import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.net.model.DataPacketFlying;
import fr.neatmonster.nocheatplus.checks.net.model.TeleportQueue;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.SchedulerHelper;
import fr.neatmonster.nocheatplus.components.debug.IDebugPlayer;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;

/**
 * Data for net checks. Some data structures may not be thread-safe, intended
 * for thread-local use. Order of events should make use within packet handlers
 * safe.
 * 
 * @author asofold
 *
 */
public class NetData extends ACheckData {

    // Reentrant lock.
    private final Lock lock = new ReentrantLock();

    // AttackFrequency
    public ActionFrequency attackFrequencySeconds = new ActionFrequency(16, 500); //16 buckets each with 500ms duration = 8 seconds

    // FlyingFrequency
    /** All flying packets, use System.currentTimeMillis() for time. */
    public final ActionFrequency flyingFrequencyAll;
    public boolean flyingFrequencyOnGround = false;
    public long flyingFrequencyTimeOnGround = 0L;
    public long flyingFrequencyTimeNotOnGround = 0L;

    // Moving
    public double movingVL = 0;

    // KeepAliveFrequency
    /**
     * Last 20 seconds keep alive packets counting. Use lastUpdate() for the
     * time of the last event. System.currentTimeMillis() is used.
     */
    public ActionFrequency keepAliveFreq = new ActionFrequency(20, 1000);
	
    // Wrong Turn
    public double wrongTurnVL = 0;
    
    // ToggleFrequency
    public double toggleFrequencyVL = 0;
    public ActionFrequency playerActionFreq;
    
    // Shared.
    /**
     * Last time some action was received (keep alive/flying/interaction). Also
     * maintained for fight.godmode.
     */
    public long lastKeepAliveTime = 0L;

    /**
     * Detect teleport-ACK packets, consistency check to only use outgoing
     * position if there has been a PlayerTeleportEvent for it.
     */
    public final TeleportQueue teleportQueue = new TeleportQueue(); // TODO: Consider using one lock per data instance and pass here.

    /**
     * Store past flying packet locations for reference (lock for
     * synchronization). Mainly meant for access to flying packets from the
     * primary thread. Latest packet is first.
     */
    // TODO: Might extend to synchronize with moving events.
    private final LinkedList<DataPacketFlying> flyingQueue = new LinkedList<DataPacketFlying>();
    /** Maximum amount of packets to store. */
    private final int flyingQueueMaxSize = 20;
    /**
     * The maximum of so far already returned sequence values, altered under
     * lock.
     */
    private long maxSequence = 0;

    /** Overall packet frequency. */
    public final ActionFrequency packetFrequency;

    public NetData(final NetConfig config) {
        flyingFrequencyAll = new ActionFrequency(config.flyingFrequencySeconds, 1000L);
        if (config.packetFrequencySeconds <= 2) {
            packetFrequency = new ActionFrequency(config.packetFrequencySeconds * 3, 333);
        }
        else packetFrequency = new ActionFrequency(config.packetFrequencySeconds * 2, 500);
        playerActionFreq = new ActionFrequency(Math.max(1, config.toggleActionSeconds), 1000L);
    }

    public void onJoin(final Player player) {
        teleportQueue.clear();
        clearFlyingQueue();
    }

    public void onLeave(Player player) {
        teleportQueue.clear();
        clearFlyingQueue();
    }
    
    /**
     * Safely request a set back from MovingData.
     * 
     * @param player
     * @param idp
     * @param plugin
     * @param checkType
     */
    public void requestSetBack(final Player player, final IDebugPlayer idp, final Plugin plugin, final CheckType checkType) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        /** Last known location of the server that has been registered by Bukkit. */
        final Location knownLocation = player.getLocation();
        final MovingData mData = pData.getGenericInstance(MovingData.class);
        Object task = null;
        task = SchedulerHelper.runSyncTaskForEntity(player, plugin, (arg) -> {
            /** Get the first set-back location that might be available */
            final Location newTo = mData.hasSetBack() ? mData.getSetBack(knownLocation) :
                                   mData.hasMorePacketsSetBack() ? mData.getMorePacketsSetBack() :
                                   // Shouldn't happen. If it does, the location is likely to be null
                                   knownLocation;
            // Unsafe position. Location hasn't been updated yet.  
            if (newTo == null) {
                StaticLog.logSevere("Could not retrieve a safe (set back) location for " + player.getName() + " on packet-level, kicking them due to crash potential.");
                CheckUtils.kickIllegalMove(player, pData.getGenericInstance(MovingConfig.class));
            } 
            else {
                // Mask player teleport as a set back.
                mData.prepareSetBack(newTo);
                SchedulerHelper.teleportEntity(player, LocUtil.clone(newTo), BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION);
                if (pData.isDebugActive(checkType)) {
                    idp.debug(player, "Packet set back tasked for player: " + player.getName() + " at :" + LocUtil.simpleFormat(newTo));
                }
            }
        }, null);
        if (!SchedulerHelper.isTaskScheduled(task)) {
            StaticLog.logWarning("Failed to schedule packet set back task for player: " + player.getName());
        }
        mData.resetTeleported(); // Cleanup, just in case.
    }

    /**
     * Add a packet to the queue (under lock). The sequence number of the packet
     * will be set here, according to a count maintained per-data.
     * 
     * @param packetData
     * @return If a packet has been removed due to exceeding maximum size.
     */
    public boolean addFlyingQueue(final DataPacketFlying packetData) {
        boolean res = false;
        lock.lock();
        packetData.setSequence(++maxSequence);
        flyingQueue.addFirst(packetData);
        if (flyingQueue.size() > flyingQueueMaxSize) {
            flyingQueue.removeLast();
            res = true;
        }
        lock.unlock();
        return res;
    }

    /**
     * Clear the flying packet queue (under lock).
     */
    public void clearFlyingQueue() {
        lock.lock();
        flyingQueue.clear();
        lock.unlock();
    }

    /**
     * Copy the entire flying queue (under lock).
     * 
     * @return
     */
    public DataPacketFlying[] copyFlyingQueue() {
        lock.lock();
        /*
         * TODO: Add a method to synchronize with the current position at the
         * same time ? Packet inversion is acute on 1.11.2 (dig is processed
         * before flying).
         */
        final DataPacketFlying[] out = flyingQueue.toArray(new DataPacketFlying[flyingQueue.size()]);
        lock.unlock();
        return out;
    }

    /**
     * Fetch the latest packet (under lock).
     * 
     * @return
     */
    public DataPacketFlying getCurrentFlyingPacket() {
        lock.lock();
        final DataPacketFlying latest = flyingQueue.isEmpty() ? null : flyingQueue.getFirst();
        lock.unlock();
        return latest;
    }

    /**
     * Fetch a past packet in queue (under lock).
     * @param index 0 is current 1 is first past packet.
     * @return
     */
    public DataPacketFlying getPastFlyingPacketInQueue(final int index) {
        lock.lock();
        final DataPacketFlying packet = flyingQueue.isEmpty() ? null : flyingQueue.get(index);
        lock.unlock();
        return packet;
    }

    /**
     * (Not implementing the interface, to avoid confusion.)
     */
    public void handleSystemTimeRanBackwards() {
        final long now = System.currentTimeMillis();
        teleportQueue.clear(); // Can't handle timeouts. TODO: Might still keep.
        lastKeepAliveTime = Math.min(lastKeepAliveTime, now);
        flyingFrequencyTimeNotOnGround = Math.min(flyingFrequencyTimeNotOnGround, now);
        flyingFrequencyTimeOnGround = Math.min(flyingFrequencyTimeOnGround, now);
        // (Keep flyingQueue.)
        // (ActionFrequency can handle this.)
    }
}
