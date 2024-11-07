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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.net.NetConfig;
import fr.neatmonster.nocheatplus.compat.bukkit.BridgeBukkitAPI;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

public class SoundDistance extends BaseAdapter {

    // TODO: Will not be effective with 512 radius, if they add the patch by @Amranth.
    // TODO: For lower distances more packets might need to be intercepted.

    /** Partly by debugging, partly from various sources, possibly including wrong spelling. */
    private static final Set<String> effectNames = new HashSet<String>(Arrays.asList(
            ////////////
            // PRE 1.9
            ////////////

            // Weather
            "ambient.weather.thunder",
            // Wither
            "wither-spawn-sound-radius",
            "mob.wither.spawn",
            "mob.wither.shoot",
            "mob.wither.idle",
            "mob.wither.hurt",
            "mob.wither.death",
            // Enderdragon
            "dragon-death-sound-radius",
            "mob.enderdragon.wings",
            "mob.enderdragon.grow",
            "mob.enderdragon.growl",
            "mob.enderdragon.hit",
            "mob.enderdragon.end",
            "game.neutral.die", // Enderdragon 1.8.7 (debug).

            //////////////////
            // 1.9 AND LATER
            //////////////////
            // Weather
            "ENTITY_LIGHTNING_IMPACT",
            "ENTITY_LIGHTNING_THUNDER",
            // Enderdragon
            "ENTITY_ENDERDRAGON_AMBIENT",
            "ENTITY_ENDERDRAGON_DEATH",
            "ENTITY_ENDERDRAGON_FIREBALL_EXPLODE",
            "ENTITY_ENDERDRAGON_FLAP",
            "ENTITY_ENDERDRAGON_GROWL",
            "ENTITY_ENDERDRAGON_HURT",
            "ENTITY_ENDERDRAGON_SHOOT",
            // Wither
            "ENTITY_WITHER_AMBIENT",
            "ENTITY_WITHER_BREAK_BLOCK",
            "ENTITY_WITHER_DEATH",
            "ENTITY_WITHER_HURT",
            "ENTITY_WITHER_SHOOT",
            "ENTITY_WITHER_SPAWN"


            ));

    private final Integer idSoundEffectCancel = counters.registerKey("packet.sound.cancel");
    private final Location useLoc = new Location(null, 0, 0, 0);
    /** Legacy check behavior. */
    private final boolean pre1_9;

    public SoundDistance(Plugin plugin) {
        super(plugin, ListenerPriority.LOW, PacketType.Play.Server.NAMED_SOUND_EFFECT);
        this.checkType = CheckType.NET_SOUNDDISTANCE;
        pre1_9 = ServerVersion.compareMinecraftVersion("1.9") < 0;
        inflateEffectNames();
    }

    /**
     * Ensure both lower and upper case are contained.
     */
    private void inflateEffectNames() {
        final List<String> names = new ArrayList<String>(effectNames);
        for (String name : names) {
            effectNames.add(name.toLowerCase());
            effectNames.add(name.toUpperCase());
        }
    }

    private boolean isSoundMonitoredPre1_9(final PacketContainer packetContainer) {
        //debug(null, packetContainer.getStrings().read(0));
        return effectNames.contains(packetContainer.getStrings().read(0));
    }

    private boolean isSoundMonitoredLatest(final PacketContainer packetContainer) {
        StructureModifier<Sound> sounds = packetContainer.getSoundEffects();
        for (final Sound sound : sounds.getValues()) {
            if (sound != null && effectNames.contains(BridgeBukkitAPI.ConvertSoundToString(sound))) {
                //debug(null, "MONITOR SOUND: " + sound);
                return true;
            }
        }
        return false;
    }

    private boolean isSoundMonitored(final PacketContainer packetContainer) {
        if (pre1_9) {
            return isSoundMonitoredPre1_9(packetContainer);
        }
        else {
            return isSoundMonitoredLatest(packetContainer);
        }
    }

    private void handleSoundPacket(final PacketEvent event) {
        try {
            if (event.isPlayerTemporary()) return;
        } catch(NoSuchMethodError e) {
            if (event.getPlayer() == null) return;
            if (DataManager.getPlayerDataSafe(event.getPlayer()) == null) return;
        }
        final PacketContainer packetContainer = event.getPacket();

        // Compare sound effect name.
        if (!isSoundMonitored(packetContainer)) {
            return;
        }

        final Player player = event.getPlayer();
        final IPlayerData pData = DataManager.getPlayerData(player);
        if (!pData.isCheckActive(CheckType.NET_SOUNDDISTANCE, player)) {
            return;
        }

        // Compare distance of player to the weather location.
        final Location loc = player.getLocation(useLoc);
        final StructureModifier<Integer> ints = packetContainer.getIntegers();
        final double dSq = TrigUtil.distanceSquared(ints.read(0) / 8, ints.read(2) / 8, loc.getX(), loc.getZ());
        //        if (data.debug) {
        //            debug(player, "SoundDistance(" + soundName + "): " + StringUtil.fdec1.format(Math.sqrt(dSq)));
        //        }
        final NetConfig cc = pData.getGenericInstance(NetConfig.class);
        if (dSq > cc.soundDistanceSq) {
            event.setCancelled(true);
            counters.add(idSoundEffectCancel, 1);
        }
        useLoc.setWorld(null);
    }
    
    @Override
    public void onPacketSending(final PacketEvent event) {
        try {
            handleSoundPacket(event);
        } catch(Throwable t) {
            t.printStackTrace();
            ProtocolLibComponent.unregister(this);
        }
    }

}
