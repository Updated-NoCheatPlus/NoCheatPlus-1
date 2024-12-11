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

import java.util.List;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.components.NoCheatPlusAPI;
import fr.neatmonster.nocheatplus.components.data.ICheckData;
import fr.neatmonster.nocheatplus.components.data.IData;
import fr.neatmonster.nocheatplus.components.registry.factory.IFactoryOne;
import fr.neatmonster.nocheatplus.players.PlayerFactoryArgument;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionFrequency;
import fr.neatmonster.nocheatplus.worlds.WorldFactoryArgument;

/**
 * Static method utility for networking related stuff.
 * <hr>
 * Not sure about final location and naming... and content :p.
 * @author dev1mc
 *
 */
public class NetStatic {

    /**
     * Packet-cheating check, for catching clients that send more packets than
     * allowed. Intention is to have a more accurate check than just preventing
     * "extreme spamming".
     * 
     * @param packetFreq
     *            Records the packets. This check will update packetFreq
     *            according to the given time and packets.
     * @param time
     *            Milliseconds time to update the ActionFrequency instance with.
     * @param packets
     *            Amount to add to packetFreq with time.
     * @param maxPackets
     *            The amount of packets per second (!), that is considered
     *            legitimate.
     * @param idealPackets
     *            The "ideal" amount of packets per second. Used for "burning"
     *            time frames by setting them to this amount.
     * @param burstFreq Counting burst events, should be covering a minute or so.
     * @param burstPackets Packets in the first time window to add to burst count.
     * @param burstEPM Events per minute to trigger a burst violation.
     * @param tags List to add tags to, for which parts of this check triggered a violation.
     * @return The violation amount, i.e. "count above limit", 0.0 if no violation.
     */
    public static double morePacketsCheck(final ActionFrequency packetFreq, final long time, final float packets, final float maxPackets, final float idealPackets, final ActionFrequency burstFreq, final float burstPackets, final double burstDirect, final double burstEPM, final List<String> tags) {
        // TODO: Push most stuff into a new class (e.g. PacketFrequency).
        // Pull down stuff.
        final long winDur = packetFreq.bucketDuration();
        final int winNum = packetFreq.numberOfBuckets();
        final long totalDur = winDur * winNum;

        // Redistribute excess packets in the first bucket to subsequent buckets to smooth out short-term spikes.
        // TODO: Configurability? Cleanup/optimize! Rename to smoothing or what not.
        final long tDiff = time - packetFreq.lastAccess();
        if (tDiff >= winDur && tDiff < totalDur) {
            // There will be some shift, so check if to relax, only if there could be some congestion. 
            float sc0 = packetFreq.bucketScore(0); // score of the most recent bucket (index 0).
            if (sc0 > maxPackets) { // TODO: Ideal vs. max. packets.
                // TODO: Keep in mind: potential exploits, a la burst to burst !?
                // Calculate the excess score to redistribute.
                sc0 -= maxPackets; // Count this down.
                // Iterate through subsequent buckets.
                for (int i = 1; i < winNum; i++) {
                    // Get the score of the current bucket.
                    final float sci = packetFreq.bucketScore(i);
                    // If the current bucket is not full, determine how much excess can be consumed.
                    if (sci < maxPackets) {
                        // Smoothen, using following empty spots including one occupied spot at most..
                        float consume = Math.min(sc0, maxPackets - sci);
                        // Reduce the excess score accordingly.
                        sc0 -= consume;
                        // Update the bucket with redistributed score.
                        packetFreq.setBucket(i, sci + consume);
                        // Stop redistribution at the first occupied bucket.
                        if (sci > 0f) {
                            break;
                        }
                    } else {
                        // Stop redistribution if no empty space is found.
                        break;
                    }
                }
                // Finally adjust the first bucket score.
                packetFreq.setBucket(0, maxPackets + sc0);
            }
        }

        // Add packet to frequency count.
        packetFreq.add(time, packets);

        // Fill up all "used" time windows (minimum we can do without other events).
        // These represent the "ideal" packet distribution in periods of no activity, to ensure that idle time (i.e.: lag, congestion etc...) is accounted for in calculations.
        final float burnScore = (float) idealPackets * (float) winDur / 1000f;
        // Find index.
        int burnStart;
        int empty = 0;
        boolean used = false;
        for (burnStart = 1; burnStart < winNum; burnStart ++) {
            if (packetFreq.bucketScore(burnStart) > 0f) {
                // TODO: burnStart ++; Fill up all ? ~ e.g. what with filled up half? 
                if (used) {
                    for (int j = burnStart; j < winNum; j ++) {
                        if (packetFreq.bucketScore(j) == 0f) {
                            empty += 1;
                        }
                    }
                    break;
                } else {
                    used = true;
                }
            }
        }

        // TODO: Burn time windows based on other activity counting [e.g. same resolution ActinFrequency with keep-alive].

        // Adjust empty based on server side lag, this makes the check more strict.
        if (empty > 0) {
            // TODO: Consider to add a config flag for skipping the lag adaption (e.g. strict).
            final float lag = TickTask.getLag(totalDur, true); // Full seconds range considered.
            // TODO: Consider increasing the allowed maximum, for extreme server-side lag.
            empty = Math.min(empty, (int) Math.round((lag - 1f) * winNum));
        }

        final double fullCount;
        if (burnStart < winNum) {
            // Assume all following time windows are burnt.
            // TODO: empty score + trailing score !? max with trailing buckets * ideal (!)
            final float trailing = Math.max(packetFreq.trailingScore(burnStart, 1f), burnScore * (winNum - burnStart - empty));
            final float leading = packetFreq.leadingScore(burnStart, 1f);
            fullCount = leading + trailing;
        } else {
            // All time windows are used.
            fullCount = packetFreq.score(1f);
        }

        double violation = 0.0; // Classic processing.
        final double vEPSAcc = (double) fullCount - (double) (maxPackets * winNum * winDur / 1000f);
        if (vEPSAcc > 0.0) {
            violation = Math.max(violation, vEPSAcc);
            tags.add("epsacc");
        }

        float burst = packetFreq.bucketScore(0); // 500ms
        if (burst > burstPackets) {
            // Account for server-side lag "minimally".
            burst /= TickTask.getLag(winDur, true); // First window lag.
            if (burst > burstPackets) {
                final double vBurstDirect = burst - burstDirect;
                if (vBurstDirect > 0.0) {
                    violation = Math.max(violation, vBurstDirect);
                    tags.add("burstdirect");
                }
                // TODO: Lag adaption for the burstFreq too [differing window durations]?
                burstFreq.add(time, 1f); // TODO: Remove float packets or do this properly.
                final double vBurstEPM = (double) burstFreq.score(0f) - burstEPM * (double) (burstFreq.bucketDuration() * burstFreq.numberOfBuckets()) / 60000.0;
                if (vBurstEPM > 0.0) {
                    violation = Math.max(violation, vBurstEPM);
                    tags.add("burstepm");
                }
            }
        }
        return Math.max(0.0, violation);
    }

    @SuppressWarnings("unchecked")
    public static void registerTypes() {
        final NoCheatPlusAPI api = NCPAPIProvider.getNoCheatPlusAPI();
        api.register(api.newRegistrationContext()
                // NetConfig
                .registerConfigWorld(NetConfig.class)
                .factory(new IFactoryOne<WorldFactoryArgument, NetConfig>() {
                    @Override
                    public NetConfig getNewInstance(WorldFactoryArgument arg) {
                        return new NetConfig(arg.worldData);
                    }
                })
                .registerConfigTypesPlayer()
                .context() //
                // NetData
                .registerDataPlayer(NetData.class)
                .factory(new IFactoryOne<PlayerFactoryArgument, NetData>() {
                    @Override
                    public NetData getNewInstance(PlayerFactoryArgument arg) {
                        return new NetData(arg.playerData.getGenericInstance(NetConfig.class));
                    }
                })
                .addToGroups(CheckType.NET, true, IData.class, ICheckData.class)
                .context() //
                );
    }

}
