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
package fr.neatmonster.nocheatplus.utilities.map;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.collision.Axis;
import fr.neatmonster.nocheatplus.utilities.ds.map.BlockCoord;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

/**
 * Map-related static utility.
 * 
 * @author asofold
 *
 */
public class MapUtil {

    /**
     * Find the appropriate BlockFace.
     * @param x Exact increments.
     * @param y
     * @param z
     * @return
     */
    public static BlockFace matchBlockFace(int x, int y, int z) {
        for (BlockFace blockFace : BlockFace.values()) {
            if (blockFace.getModX() == x && blockFace.getModY() == y && blockFace.getModZ() == z) {
                return blockFace;
            }
        }
        return null;
    }

    /**
     * Convenience method to check if the bounds as returned by getBounds cover
     * a whole block.
     *
     * @param bounds
     *            Can be null, must have 6 fields.
     * @return true, if is full bounds
     */
    public static final boolean isFullBounds(final double[] bounds) {
        if (bounds == null) return false;
        for (int i = 0; i < 3; i ++) {
            if (bounds[i] > 0.0) return false;
            if (bounds[i + 3] < 1.0) return false;
        }
        return true;
    }

    /**
     * Check if chunks are loaded and load all not yet loaded chunks, using
     * normal world coordinates.<br>
     * NOTE: Not sure where to put this. Method does not use any caching.
     *
     * @param world
     *            the world
     * @param x
     *            the x
     * @param z
     *            the z
     * @param xzMargin
     *            the xz margin
     * @return Number of loaded chunks.
     */
    public static int ensureChunksLoaded(final World world, final double x, final double z, final double xzMargin) {
        int loaded = 0;
        if (world == null) {
            return 0;
        }
        final int minX = Location.locToBlock(x - xzMargin) / 16;
        final int maxX = Location.locToBlock(x + xzMargin) / 16;
        final int minZ = Location.locToBlock(z - xzMargin) / 16;
        final int maxZ = Location.locToBlock(z + xzMargin) / 16;
        for (int cx = minX; cx <= maxX; cx ++) {
            for (int cz = minZ; cz <= maxZ; cz ++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    try {
                        world.getChunkAt(cx, cz);
                        loaded ++;
                    } catch (Exception ex) {
                        // (Can't seem to catch more precisely: TileEntity with CB 1.7.10)
                        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().severe(Streams.STATUS, "Failed to load chunk at " + (cx * 16) + "," + (cz * 16) + " (real coordinates):\n" + StringUtil.throwableToString(ex));
                        // (Don't count as loaded.)
                    }
                }
            }
        }
        return loaded;
    }
    
    /**
     * A function to determine which neighboring blocks of a given block ({@code source}) can potentially be interacted with, 
     * based on the given direction Vector, representing the distance from the player to the block, and the player’s eye position. 
     * It considers priority axes and exclusions, provided through {@code axisData}.
     *
     * @param source The current block coordinate from which neighbors are determined.
     * @param direction A Vector indicating the distance from the player’s location to the block location.<br>
     *                  It essentially indicates which blocks around the current block are closer to the player, and thus more likely to be interacted with first.
     * @param eyeX Coordinates of the player’s eye position, used for prioritizing which axis to check first.
     * @param eyeY
     * @param eyeZ
     * @param axisData Additional information specifying priority axes and directions to exclude. 
     *                 This can be null. If not null, the given data is used (and consumed).
     * @return A list of BlockCoord objects representing the (coordinate of the) neighboring blocks that can possibly be interacted with from the current block’s position
     */
    public static List<BlockCoord> getNeighborsInDirection(BlockCoord source, Vector direction, double eyeX, double eyeY, double eyeZ, Axis.RichAxisData axisData) {
        // Create an empty list to store the coordinates of neighboring blocks.
        List<BlockCoord> neighbors = new ArrayList<>();
        // Determine the step values based on the direction. These steps determine which direction to move along each axis in order to get the neighbouring blocks.
        int stepY = direction.getY() > 0 ? 1 : (direction.getY() < 0 ? -1 : 0);
        int stepX = direction.getX() > 0 ? 1 : (direction.getX() < 0 ? -1 : 0);
        int stepZ = direction.getZ() > 0 ? 1 : (direction.getZ() < 0 ? -1 : 0);
        // Initialize the priority axis (priorityAxis) and exclusion direction (excludeDir) to their default values.
        Axis priorityAxis = Axis.NONE;
        BlockChangeTracker.Direction excludeDir = BlockChangeTracker.Direction.NONE;
        // By default, all directions are applicable.
        boolean allowX = true;
        boolean allowY = true;
        boolean allowZ = true;
        // If axisData is provided, update the variables above accordingly and determine whether movement is allowed in each direction (X, Y, Z) based on the exclusion direction.
        if (axisData != null) {
            priorityAxis = axisData.priority;
            excludeDir = axisData.dirExclusion;
            axisData.priority = Axis.NONE;
            axisData.dirExclusion = BlockChangeTracker.Direction.NONE;
            allowX = !(excludeDir == BlockChangeTracker.Direction.X_NEG && stepX < 0 || excludeDir == BlockChangeTracker.Direction.X_POS && stepX > 0);
            allowY = !(excludeDir == BlockChangeTracker.Direction.Y_NEG && stepY < 0 || excludeDir == BlockChangeTracker.Direction.Y_POS && stepY > 0);
            allowZ = !(excludeDir == BlockChangeTracker.Direction.Z_NEG && stepZ < 0 || excludeDir == BlockChangeTracker.Direction.Z_POS && stepZ > 0);
        }
        // If priorityAxis is specified, add the neighboring block along that axis first.
        switch (priorityAxis) {
            case X_AXIS:
                // Always add the blocks on currently handlded axis first. (x in this case)
                neighbors.add(new BlockCoord(source.getX() + stepX, source.getY(), source.getZ()));
                // Then, add neighboring blocks along the other axes, if movement in those directions is allowed.
                if (allowZ) {
                    neighbors.add(new BlockCoord(source.getX(), source.getY(), source.getZ() + stepZ));
                }
                if (allowY) {
                    neighbors.add(new BlockCoord(source.getX(), source.getY() + stepY, source.getZ()));
                }
                // Early return: the priority has been handled; blocks have been collected.
                return neighbors;
            case Y_AXIS:
                neighbors.add(new BlockCoord(source.getX(), source.getY() + stepY, source.getZ()));
                if (allowX) {
                    neighbors.add(new BlockCoord(source.getX() + stepX, source.getY(), source.getZ()));
                }
                if (allowZ) {
                    neighbors.add(new BlockCoord(source.getX(), source.getY(), source.getZ() + stepZ));
                }
                return neighbors;
            case Z_AXIS:
                neighbors.add(new BlockCoord(source.getX(), source.getY(), source.getZ() + stepZ));
                if (allowX) {
                    neighbors.add(new BlockCoord(source.getX() + stepX, source.getY(), source.getZ()));
                }
                if (allowY) {
                    neighbors.add(new BlockCoord(source.getX(), source.getY() + stepY, source.getZ()));
                }
                return neighbors;
            default:
                break;
        }
        
        // Calculate the manhattan distances (manhattanX, manhattanY, manhattanZ) from the source block to the eye position for each axis.
        final double manhattanY = TrigUtil.manhattan(source.getX(), source.getY() + stepY, source.getZ(), eyeX, eyeY, eyeZ);
        final double manhattanZ = TrigUtil.manhattan(source.getX(), source.getY(), source.getZ() + stepZ, eyeX, eyeY, eyeZ);
        final double manhattanX = TrigUtil.manhattan(source.getX() + stepX, source.getY(), source.getZ(), eyeX, eyeY, eyeZ);
        // Compare these distances to prioritize which neighboring block to add first.
        // Add neighbors based on the shortest Manhattan distance and whether movement in that direction is allowed.
        // TODO: Is this one correct?
        if (manhattanY <= manhattanX && manhattanY <= manhattanZ && Math.abs(direction.getY()) >= 0.5) {
            if (allowY) {
                neighbors.add(new BlockCoord(source.getX(), source.getY() + stepY, source.getZ()));
            }
            // Do sort priority of XZ in case Y not possible
            if (manhattanX < manhattanZ) {
                if (allowX) {
                    neighbors.add(new BlockCoord(source.getX() + stepX, source.getY(), source.getZ()));
                }
                if (allowZ) {
                    neighbors.add(new BlockCoord(source.getX(), source.getY(), source.getZ() + stepZ));
                }
            }
            else {
                if (allowZ) {
                    neighbors.add(new BlockCoord(source.getX(), source.getY(), source.getZ() + stepZ));
                }
                if (allowX) {
                    neighbors.add(new BlockCoord(source.getX() + stepX, source.getY(), source.getZ()));
                }
            }
            return neighbors;
        }

        if (manhattanX < manhattanZ) {
            if (allowX) {
                neighbors.add(new BlockCoord(source.getX() + stepX, source.getY(), source.getZ()));
            }
            if (allowZ) {
                neighbors.add(new BlockCoord(source.getX(), source.getY(), source.getZ() + stepZ));
            }
            if (allowY) {
                neighbors.add(new BlockCoord(source.getX(), source.getY() + stepY, source.getZ()));
            }
        }
        else {
            if (allowZ) {
                neighbors.add(new BlockCoord(source.getX(), source.getY(), source.getZ() + stepZ));
            }
            if (allowX) {
                neighbors.add(new BlockCoord(source.getX() + stepX, source.getY(), source.getZ()));
            }
            if (allowY) {
                neighbors.add(new BlockCoord(source.getX(), source.getY() + stepY, source.getZ()));
            }
        }
        return neighbors;
    }
}
