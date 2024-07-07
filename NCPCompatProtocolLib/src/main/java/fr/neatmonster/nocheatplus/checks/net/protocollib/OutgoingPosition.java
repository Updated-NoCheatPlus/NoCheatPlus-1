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
package fr.neatmonster.nocheatplus.checks.net.protocollib;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.net.NetData;
import fr.neatmonster.nocheatplus.checks.net.model.CountableLocation;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;

public class OutgoingPosition extends BaseAdapter {

    public static final int indexX = 0;
    public static final int indexY = 1;
    public static final int indexZ = 2;
    public static final int indexYaw = 0;
    public static final int indexPitch = 1;

    private final Integer ID_OUTGOING_POSITION_UNTRACKED = counters.registerKey("packet.outgoing_position.untracked");

    private boolean hasTeleportId = true;

    public OutgoingPosition(Plugin plugin) {
        // PacketPlayInFlying[3, legacy: 10]
        super(plugin, ListenerPriority.HIGHEST, new PacketType[] {PacketType.Play.Server.POSITION});
        // TODO: POSITION_LOOK ??
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        try {
            if (event.isPlayerTemporary()) return;
        } 
        catch(NoSuchMethodError e) {}
    	if (event.isCancelled()) {
            return;
        }
        final long time = System.currentTimeMillis();
        final Player player = event.getPlayer();
        if (player == null) {
        	counters.add(ProtocolLibComponent.idNullPlayer, 1);
            return;
        }
        final IPlayerData pData = DataManager.getPlayerDataSafe(player);
        if (pData == null) {
            StaticLog.logWarning("Failed to fetch player data with " + event.getPacketType() + " for: " + player.toString());
            return;
        }
        interpretPacket(player, event.getPacket(), time, pData.getGenericInstance(NetData.class), pData.isDebugActive(CheckType.NET));
    }

    private void interpretPacket(final Player player, final PacketContainer packet, final long time, final NetData data, final boolean debug) {
        final StructureModifier<Double> doubles = packet.getDoubles();
        final StructureModifier<Float> floats = packet.getFloat();

        if (doubles.size() != 3 || floats.size() != 2) {
            packetMismatch(packet);
            return;
        }

        // TODO: Detect/skip data with relative coordinates.
        // TODO: Concept: force KeepAlive vs. set expected coordinates in Bukkit events.

        final double x = doubles.read(indexX);
        final double y = doubles.read(indexY);
        final double z = doubles.read(indexZ);
        final float yaw = floats.read(indexYaw);
        final float pitch = floats.read(indexPitch);
        Integer teleportId = Integer.MIN_VALUE;

        if (hasTeleportId) {
            try {
                final StructureModifier<Integer> integers = packet.getIntegers();
                if (integers.size() == 1) {
                    // Accept as id.
                    teleportId = integers.read(0);
                    if (teleportId == null) {
                        teleportId = Integer.MIN_VALUE;
                    }
                    if (teleportId != Integer.MIN_VALUE && debug) {
                        debug(player, "Outgoing confirm teleport id: " + teleportId);
                    }
                }
                else {
                    hasTeleportId = false;
                    NCPAPIProvider.getNoCheatPlusAPI().getLogManager().info(Streams.STATUS, "PacketPlayOutPosition: Teleport confirm id not available, field mismatch: " + integers.size());
                }
            }
            catch (Throwable t) {
                hasTeleportId = false;
                NCPAPIProvider.getNoCheatPlusAPI().getLogManager().info(Streams.STATUS, "PacketPlayOutPosition: Teleport confirm id not available.");
            }
        }

        final CountableLocation packetData = data.teleportQueue.onOutgoingTeleport(x, y, z, yaw, pitch, teleportId);
        if (packetData == null) {
            // Add counter for untracked (by Bukkit API) outgoing teleport.
            // TODO: There may be other cases which are indicated by Bukkit API events.
            counters.add(ID_OUTGOING_POSITION_UNTRACKED, 1);
            if (debug) {
                debug(player, "Untracked outgoing position: " + x + ", " + y + ", " + z + " (yaw=" + yaw + ", pitch=" + pitch + ").");
            }
        }
        else {
            if (debug) {
                debug(player, "Expect ACK on outgoing position: " + packetData);
            }
        }
    }

    private void packetMismatch(PacketContainer packet) {
        // TODO: What? Add to counters?
    }

}
