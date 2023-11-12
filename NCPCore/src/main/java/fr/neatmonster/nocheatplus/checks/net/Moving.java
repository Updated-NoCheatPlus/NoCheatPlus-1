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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.net.model.DataPacketFlying;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

/**
 * Misc. checks related to flying packets.
 * Currently only has a simplistic check for extreme moves.
 */
public class Moving extends Check {

    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
    final Location useLoc = new Location(null, 0, 0, 0);
    final List<String> tags = new ArrayList<String>();
    
    public Moving() {
        super(CheckType.NET_MOVING);
    }

    /**
     * Checks a player
     * 
     * @param player
     * @param packetData
     * @param data
     * @param cc
     * @param pData
     * @param cc
     * @return True, if to cancel the event
     */
    public boolean check(final Player player, final DataPacketFlying packetData, final NetData data, final NetConfig cc, 
                         final IPlayerData pData, final Plugin plugin) {
        // TODO: This will trigger if the client is waiting for chunks to load (Slow fall, on join or after a teleport)
        // TODO Replace this check with a packet-sync thing: Sync flying packets with a PlayerMoveEvent, flag incoming packets which don't fire any move event.
        boolean cancel = false;
        final long now = System.currentTimeMillis();
        final boolean debug = pData.isDebugActive(CheckType.NET_MOVING);
        tags.clear();
        if (now > pData.getLastJoinTime() && pData.getLastJoinTime() + 10000 > now) {
            tags.add("login_grace");
        	return false;
        }
        if (packetData != null && packetData.hasPos) {
            final MovingData mData = pData.getGenericInstance(MovingData.class);
            /** Actual Location on the server */
            final Location knownLocation = player.getLocation(useLoc);
            /** Claimed Location sent by the client */
            final Location packetLocation = new Location(null, packetData.getX(), packetData.getY(), packetData.getZ());
            //final double distanceSq = TrigUtil.distanceSquared(knownLocation, packetLocation);
            final double yDistance = Math.abs(knownLocation.getY() - packetLocation.getY());
            final double hDistance = TrigUtil.xzDistance(knownLocation, packetLocation);
            final double distance = TrigUtil.distance(knownLocation, packetLocation);

            // 100 it's the minimum [Math.max(100, config distance)]distance for the 'moved too quickly' check to fire
            // See PlayerConnection.java
            if (yDistance > 100.0 || distance > 100.0/*distanceSq > 100.0 || hDistance > 100.0*/) {
                data.movingVL++ ;
                tags.add("invalid_pos");
                final ViolationData vd = new ViolationData(this, player, data.movingVL, 1.0, cc.movingActions);
                if (vd.needsParameters()) vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
                cancel = executeActions(vd).willCancel();
            }
            else {
                data.movingVL *= 0.98;
            }
        }

        if (debug) {
            final Location packetLocation = new Location(null, packetData.getX(), packetData.getY(), packetData.getZ());
            final StringBuilder builder = new StringBuilder(500);
            if (packetData.hasPos) {
                builder.append(CheckUtils.getLogMessagePrefix(player, type));
                builder.append("\nPacket location: " + LocUtil.simpleFormat(packetLocation));
                builder.append("\nServer location: " + LocUtil.simpleFormat(player.getLocation(useLoc)));
                builder.append("\nDeltas: h= " + TrigUtil.distance(player.getLocation(useLoc), packetLocation) + ", y= " + Math.abs(player.getLocation(useLoc).getY() - packetLocation.getY()));
            }
            else {
            	builder.append("Empty packet (no position)");
            }
            NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
        }
        useLoc.setWorld(null);
        return cancel;
    }
}