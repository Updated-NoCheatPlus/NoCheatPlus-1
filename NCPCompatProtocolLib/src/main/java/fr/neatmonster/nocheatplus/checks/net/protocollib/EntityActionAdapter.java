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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerAction;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.net.NetConfig;
import fr.neatmonster.nocheatplus.checks.net.NetData;
import fr.neatmonster.nocheatplus.checks.net.ToggleFrequency;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;

/**
 * Adapter for the EntityAction NMS packet.
 */
public class EntityActionAdapter extends BaseAdapter {
    
    private final ToggleFrequency toggleFrequency = new ToggleFrequency();
    
    private static PacketType[] initPacketTypes() {
        final List<PacketType> types = new LinkedList<PacketType>(Arrays.asList(PacketType.Play.Client.ENTITY_ACTION));
        return types.toArray(new PacketType[types.size()]);
    }

    public EntityActionAdapter(Plugin plugin) {
        super(plugin, ListenerPriority.LOW, initPacketTypes());
    }
    
    @Override
    public void onPacketReceiving(final PacketEvent event) {
        // Do some early return tests first.
        try {
            if (event.isPlayerTemporary()) return;
        } 
        catch (NoSuchMethodError e) {}
        
        // Not interested in any other packet.
        if (event.getPacketType() != PacketType.Play.Client.ENTITY_ACTION) {
            return;
        }
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
        final StructureModifier<PlayerAction> actions = event.getPacket().getPlayerActions();
        // if (actions.size() != 1) {
        //     StaticLog.logWarning("Unexpected packet size for EntityActionAdapter: " + actions.size());
        //     return;
        // }
       
        final NetData data = pData.getGenericInstance(NetData.class);
        // (This check might invalidate the current BedLeave implementation...)
        // Currently, we don't discern action types from one another.
        data.playerActionFreq.add(System.currentTimeMillis(), 1f);
        // Always set last received time.
        data.lastKeepAliveTime = System.currentTimeMillis();
        boolean cancel = false;
        if (!cancel && pData.isCheckActive(CheckType.NET_TOGGLEFREQUENCY, player)
            && toggleFrequency.check(player, data, pData.getGenericInstance(NetConfig.class), pData)
            && !pData.hasBypass(CheckType.NET_TOGGLEFREQUENCY, player)) {
            cancel = true;
        }
        
        // Request a setback
        if (cancel) {
        	data.requestSetBack(player, this, plugin, checkType);
        }
    }
}