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

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.access.ACheckData;
import fr.neatmonster.nocheatplus.checks.net.model.DataPacketFlying;
import fr.neatmonster.nocheatplus.checks.net.model.TeleportQueue;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;

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
    public ActionFrequency attackFrequencySeconds = new ActionFrequency(16, 500);

    // FlyingFrequency
    /** All flying packets, use System.currentTimeMillis() for time. */
    public final ActionFrequency flyingFrequencyAll;
    public boolean flyingFrequencyOnGround = false;
    public long flyingFrequencyTimeOnGround = 0L;
    public long flyingFrequencyTimeNotOnGround = 0L;
    public double diffpacketVLs = 0;
    /**
     * Monitors redundant packets, when more than 20 packets per second are
     * sent. Use System.currentTimeMillis() for time.
     */
    public final ActionFrequency flyingFrequencyRedundantFreq;

    // KeepAliveFrequency
    /**
     * Last 20 seconds keep alive packets counting. Use lastUpdate() for the
     * time of the last event. System.currentTimeMillis() is used.
     */
    public ActionFrequency keepAliveFreq = new ActionFrequency(20, 1000);
	
	// Wrong Turn
    public double wrongTurnVL = 0;

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
    private final int flyingQueueMaxSize = 15;
    /**
     * The maximum of so far already returned sequence values, altered under
     * lock.
     */
    private long maxSequence = 0;

    /** Overall packet frequency. */
    public final ActionFrequency packetFrequency;

    public NetData(final NetConfig config) {
        flyingFrequencyAll = new ActionFrequency(config.flyingFrequencySeconds, 1000L);
        flyingFrequencyRedundantFreq = new ActionFrequency(config.flyingFrequencyRedundantSeconds, 1000L);
        if (config.packetFrequencySeconds <= 2) {
            packetFrequency = new ActionFrequency(config.packetFrequencySeconds * 3, 333);
        }
        else {
            packetFrequency = new ActionFrequency(config.packetFrequencySeconds * 2, 500);
        }
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
    public DataPacketFlying peekFlyingQueue() {
        lock.lock();
        final DataPacketFlying latest = flyingQueue.isEmpty() ? null : flyingQueue.getFirst();
        lock.unlock();
        return latest;
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
