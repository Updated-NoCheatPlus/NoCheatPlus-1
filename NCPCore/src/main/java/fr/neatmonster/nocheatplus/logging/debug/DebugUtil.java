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
package fr.neatmonster.nocheatplus.logging.debug;

import org.bukkit.Location;
import org.bukkit.Material;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache.IBlockCacheNode;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

/**
 * Some auxiliary static-access methods.
 * 
 * @author asofold
 *
 */
public class DebugUtil {

    /**
     * Coordinates and pitch/yaw, no extras.
     * 
     * @param from
     * @param to
     * @return
     */
    public static String formatMove(Location from, Location to) {
        StringBuilder builder = new StringBuilder(128);
        DebugUtil.addMove(from, to, null, builder);
        return builder.toString();
    }

    public static void addLocation(final Location loc, final StringBuilder builder){
        builder.append(LocUtil.simpleFormat(loc));
    }

    public static void addLocation(final PlayerLocation loc, final StringBuilder builder){
        builder.append(LocUtil.simpleFormat(loc));
    }

    /**
     * Add exact coordinates and pitch/yaw, multiple lines. No leading/trailing
     * new lines.
     * 
     * @param from
     * @param to
     * @param loc
     *            Reference location for from, usually Player.getLocation().
     * @param builder
     */
    public static void addMove(final PlayerLocation from, final PlayerLocation to, final Location loc, final StringBuilder builder){
        if (loc != null && !from.isSamePos(loc)){
            builder.append("Location: ");
            addLocation(loc, builder);
            builder.append("\n");
        }
        addMove(from, to, builder);	
    }

    /**
     * Add exact coordinates and pitch/yaw, multiple lines. No leading/trailing
     * new lines.
     * 
     * @param from
     * @param to
     * @param loc
     *            Reference location for from, usually Player.getLocation().
     * @param builder
     */
    public static void addMove(final Location from, final Location to, final Location loc, final StringBuilder builder){
        if (loc != null && !TrigUtil.isSamePos(from, loc)){
            builder.append("Location: ");
            addLocation(loc, builder);
            builder.append("\n");
        }
        addMove(from, to, builder);
    }

    /**
     * Add exact coordinates and pitch/yaw, multiple lines. No leading/trailing
     * newlines.
     * 
     * @param from
     * @param to
     * @param builder
     */
    public static void addMove(final PlayerLocation from, final PlayerLocation to, final StringBuilder builder) {
        builder.append("From loc: ");
        addLocation(from, builder);
        builder.append("\n");
        builder.append("To loc: ");
        addLocation(to, builder);
    }

    /**
     * Add exact coordinates and pitch/yaw, multiple lines. No leading/trailing
     * newlines.
     * 
     * @param from
     * @param to
     * @param builder
     */
    public static void addMove(final Location from, final Location to, final StringBuilder builder) {
        builder.append("From: ");
        addLocation(from, builder);
        builder.append("\n");
        builder.append("To: ");
        addLocation(to, builder);
    }

    public static void addBlockBelowInfo(final StringBuilder builder, final PlayerLocation loc, final String tag) {
        addBlockInfo(builder, loc.getBlockCache(), loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ(), tag + " below");
    }

    public static void addBlockInfo(final StringBuilder builder, final PlayerLocation loc, final String tag) {
        addBlockInfo(builder, loc.getBlockCache(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), tag);
    }

    /**
     * Add information about id, data, shape to the builder. Starts with a space
     * always.
     * 
     * @param builder
     * @param blockCache
     * @param x
     *            Block coordinates.
     * @param y
     * @param z
     * @param messagePrefix
     *            May be null.
     */
    public static void addBlockInfo(final StringBuilder builder, final BlockCache blockCache, final int x, final int y, final int z, final String messagePrefix) {
        if (messagePrefix != null && ! messagePrefix.isEmpty()) {
            builder.append(messagePrefix);
        }
        builder.append(" id= ");
        final IBlockCacheNode node = blockCache.getOrCreateBlockCacheNode(x, y, z, true);
        final Material id = node.getType();
        builder.append(id);
        builder.append(" | data= ");
        builder.append(node.getData());
        final double[] bounds = node.getBounds();
        if (bounds != null) {
            final double minHeight = BlockProperties.getGroundMinHeight(blockCache, x, y, z, node, BlockFlags.getBlockFlags(id));
            builder.append(" | shape= [");
            builder.append(bounds[0]);
            builder.append(", ");
            builder.append(bounds[1]);
            builder.append(", ");
            builder.append(bounds[2]);
            builder.append(", ");
            builder.append(bounds[3]);
            builder.append(", ");
            builder.append(minHeight == bounds[4] ? minHeight : (minHeight + ".." + bounds[4]));
            builder.append(", ");
            builder.append(bounds[5]);
            if (bounds.length > 6 && bounds.length % 6 == 0) {
                for (int i = 2; i <= (int)bounds.length / 6; i++) {
                    builder.append(", ");
                    builder.append(bounds[i*6-6]);
                    builder.append(", ");
                    builder.append(bounds[i*6-5]);
                    builder.append(", ");
                    builder.append(bounds[i*6-4]);
                    builder.append(", ");
                    builder.append(bounds[i*6-3]);
                    builder.append(", ");
                    builder.append(bounds[i*6-2]);
                    builder.append(", ");
                    builder.append(bounds[i*6-1]);
                }
            }
            builder.append("]");
        }
    }

    /**
     * Debug to TRACE_FILE. Meant for temporary use, use CheckUtils.debug for
     * permanent inserts.
     * 
     * @return The given returnValue.
     */
    public static boolean debug(String message, boolean returnValue) {
        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, message);
        return returnValue;
    }

    /**
     * Debug to TRACE_FILE. Meant for temporary use, use CheckUtils.debug for
     * permanent inserts.
     * 
     * @param message
     * @return
     */
    public static void debug(String message) {
        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, message);
    }

    public static String toJava(final double[] doubles) {
        final StringBuilder builder = new StringBuilder(20 + doubles.length * 20);
        toJava(doubles, builder);
        return builder.toString();
    }

    /**
     * 
     * @param doubles
     * @param builder
     */
    public static void toJava(final double[] doubles, final StringBuilder builder) {
        if (doubles == null) {
            builder.append("null");
            return;
        }
        builder.append("new double[] {");
        if (doubles.length > 0) {
            builder.append(doubles[0]);
        }
        for (int i = 1; i < doubles.length; i++) {
            builder.append(", " + doubles[i]);
        }
        builder.append("}");
    }

    // TODO: method to log raytracing/recorder directly with prefix message.

}
