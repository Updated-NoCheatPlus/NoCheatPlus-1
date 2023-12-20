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
package fr.neatmonster.nocheatplus.utilities.collision;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.utilities.ds.map.BlockCoord;


/**
 * Collision related static utility.
 * 
 * @author asofold
 *
 */
public class CollisionUtil {

    /** Temporary use, setWorld(null) once finished. */
    private static final Location useLoc = new Location(null, 0, 0, 0);

    /**
     * Check if a player looks at a target of a specific size, with a specific
     * precision value (roughly).
     *
     * @param player
     *            the player
     * @param targetX
     *            the target x
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            the target width
     * @param targetHeight
     *            the target height
     * @param precision
     *            the precision
     * @return the double
     */
    public static double directionCheck(final Player player, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision)
    {
        final Location loc = player.getLocation(useLoc);
        final Vector dir = loc.getDirection();
        final double res = directionCheck(loc.getX(), loc.getY() + MovingUtil.getEyeHeight(player), loc.getZ(), dir.getX(), dir.getY(), dir.getZ(), targetX, targetY, targetZ, targetWidth, targetHeight, precision);
        useLoc.setWorld(null);
        return res;
    }

    /**
     * Convenience method.
     *
     * @param sourceFoot
     *            the source foot
     * @param eyeHeight
     *            the eye height
     * @param dir
     *            the dir
     * @param target
     *            the target
     * @param precision
     *            (width/height are set to 1)
     * @return the double
     */
    public static double directionCheck(final Location sourceFoot, final double eyeHeight, final Vector dir, final Block target, final double precision)
    {
        return directionCheck(sourceFoot.getX(), sourceFoot.getY() + eyeHeight, sourceFoot.getZ(), dir.getX(), dir.getY(), dir.getZ(), target.getX(), target.getY(), target.getZ(), 1, 1, precision);
    }

    /**
     * Convenience method.
     *
     * @param sourceFoot
     *            the source foot
     * @param eyeHeight
     *            the eye height
     * @param dir
     *            the dir
     * @param targetX
     *            the target x
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            the target width
     * @param targetHeight
     *            the target height
     * @param precision
     *            the precision
     * @return the double
     */
    public static double directionCheck(final Location sourceFoot, final double eyeHeight, final Vector dir, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision)
    {
        return directionCheck(sourceFoot.getX(), sourceFoot.getY() + eyeHeight, sourceFoot.getZ(), dir.getX(), dir.getY(), dir.getZ(), targetX, targetY, targetZ, targetWidth, targetHeight, precision);                    
    }

    /**
     * Check how far the looking direction is off the target.
     *
     * @param sourceX
     *            Source location of looking direction.
     * @param sourceY
     *            the source y
     * @param sourceZ
     *            the source z
     * @param dirX
     *            Looking direction.
     * @param dirY
     *            the dir y
     * @param dirZ
     *            the dir z
     * @param targetX
     *            Location that should be looked towards.
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            xz extent
     * @param targetHeight
     *            y extent
     * @param precision
     *            the precision
     * @return Some offset.
     */
    public static double directionCheck(final double sourceX, final double sourceY, final double sourceZ, final double dirX, final double dirY, final double dirZ, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision)
    {

        //      // TODO: Here we have 0.x vs. 2.x, sometimes !
        //      NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, "COMBINED: " + combinedDirectionCheck(sourceX, sourceY, sourceZ, dirX, dirY, dirZ, targetX, targetY, targetZ, targetWidth, targetHeight, precision, 60));

        // TODO: rework / standardize.

        double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dirLength == 0.0) dirLength = 1.0; // ...

        final double dX = targetX - sourceX;
        final double dY = targetY - sourceY;
        final double dZ = targetZ - sourceZ;

        final double targetDist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        final double xPrediction = targetDist * dirX / dirLength;
        final double yPrediction = targetDist * dirY / dirLength;
        final double zPrediction = targetDist * dirZ / dirLength;

        double off = 0.0D;

        off += Math.max(Math.abs(dX - xPrediction) - (targetWidth / 2 + precision), 0.0D);
        off += Math.max(Math.abs(dZ - zPrediction) - (targetWidth / 2 + precision), 0.0D);
        off += Math.max(Math.abs(dY - yPrediction) - (targetHeight / 2 + precision), 0.0D);

        if (off > 1) off = Math.sqrt(off);

        return off;
    }

    /**
     * Combined direction check.
     *
     * @param sourceFoot
     *            the source foot
     * @param eyeHeight
     *            the eye height
     * @param dir
     *            the dir
     * @param targetX
     *            the target x
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            the target width
     * @param targetHeight
     *            the target height
     * @param precision
     *            the precision
     * @param anglePrecision
     *            the angle precision
     * @return the double
     */
    public static double combinedDirectionCheck(final Location sourceFoot, final double eyeHeight, final Vector dir, final double targetX, final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double precision, final double anglePrecision, boolean isPlayer)
    {
        return combinedDirectionCheck(sourceFoot.getX(), sourceFoot.getY() + eyeHeight, sourceFoot.getZ(), dir.getX(), dir.getY(), dir.getZ(), targetX, targetY, targetZ, targetWidth, targetHeight, precision, anglePrecision, isPlayer);                  
    }

    /**
     * Combined direction check.
     *
     * @param sourceFoot
     *            the source foot
     * @param eyeHeight
     *            the eye height
     * @param dir
     *            the dir
     * @param target
     *            the target
     * @param precision
     *            the precision
     * @param anglePrecision
     *            the angle precision
     * @return the double
     */
    public static double combinedDirectionCheck(final Location sourceFoot, final double eyeHeight, final Vector dir, final Block target, final double precision, final double anglePrecision)
    {
        return combinedDirectionCheck(sourceFoot.getX(), sourceFoot.getY() + eyeHeight, sourceFoot.getZ(), dir.getX(), dir.getY(), dir.getZ(), target.getX(), target.getY(), target.getZ(), 1, 1, precision, anglePrecision, true);
    }

    /**
     * Combine directionCheck with angle, in order to prevent low-distance
     * abuse.
     *
     * @param sourceX
     *            the source x
     * @param sourceY
     *            the source y
     * @param sourceZ
     *            the source z
     * @param dirX
     *            the dir x
     * @param dirY
     *            the dir y
     * @param dirZ
     *            the dir z
     * @param targetX
     *            the target x
     * @param targetY
     *            the target y
     * @param targetZ
     *            the target z
     * @param targetWidth
     *            the target width
     * @param targetHeight
     *            the target height
     * @param blockPrecision
     *            the block precision
     * @param anglePrecision
     *            Precision in grad.
     * @return the double
     */
    public static double combinedDirectionCheck(final double sourceX, final double sourceY, final double sourceZ, final double dirX, final double dirY, final double dirZ, final double targetX, 
                                                final double targetY, final double targetZ, final double targetWidth, final double targetHeight, final double blockPrecision, 
                                                final double anglePrecision, final boolean isPlayer) {
        double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dirLength == 0.0) dirLength = 1.0; // ...

        final double dX = targetX - sourceX;
        final double dY = targetY - sourceY;
        final double dZ = targetZ - sourceZ;

        final double targetDist = Math.sqrt(dX * dX + dY * dY + dZ * dZ);
        final double minDist = isPlayer ? Math.max(targetHeight, targetWidth) / 2.0 : Math.max(targetHeight, targetWidth);

        if (targetDist > minDist && TrigUtil.angle(sourceX, sourceY, sourceZ, dirX, dirY, dirZ, targetX, targetY, targetZ) * TrigUtil.fRadToGrad > anglePrecision){
            return targetDist - minDist;
        }

        final double xPrediction = targetDist * dirX / dirLength;
        final double yPrediction = targetDist * dirY / dirLength;
        final double zPrediction = targetDist * dirZ / dirLength;

        double off = 0.0D;

        off += Math.max(Math.abs(dX - xPrediction) - (targetWidth / 2 + blockPrecision), 0.0D);
        off += Math.max(Math.abs(dY - yPrediction) - (targetHeight / 2 + blockPrecision), 0.0D);
        off += Math.max(Math.abs(dZ - zPrediction) - (targetWidth / 2 + blockPrecision), 0.0D);

        if (off > 1) off = Math.sqrt(off);

        return off;
    }

    /**
     * Test if the block coordinate is intersecting with min+max bounds,
     * assuming the a full block. Excludes the case of only the edges
     * intersecting.
     *
     * @param min
     *            the min
     * @param max
     *            the max
     * @param block
     *            Block coordinate of the block.
     * @return true, if successful
     */
    public static boolean intersectsBlock(final double min, final double max, final int block) {
        final double db = (double) block;
        return db + 1.0 > min && db < max;
    }

    /**
     * Test if a point is inside an AABB, including the edges.
     * 
     * @param x
     *            Position of the point.
     * @param y
     * @param z
     * @param minX
     *            Minimum coordinates of the AABB.
     * @param minY
     * @param minZ
     * @param maxX
     *            Maximum coordinates of the AABB.
     * @param maxY
     * @param maxZ
     * @return
     */
    public static boolean isInsideAABBIncludeEdges(final double x, final double y, final double z,
                                                   final double minX, final double minY, final double minZ,
                                                   final double maxX, final double maxY, final double maxZ) {
        return !(x < minX || x > maxX || z < minZ || z > maxZ || y < minY || y > maxY);
    }

    /**
     * Get the earliest time a collision with the min-max coordinates can occur,
     * in multiples of dir, including edges.
     * 
     * @param pos
     * @param dir
     * @param minPos
     * @param maxPos
     * @return The multiple of dir to hit the min-max coordinates, or
     *         Double.POSITIVE_INFINITY if not possible to hit.
     */
    public static double getMinTimeIncludeEdges(final double pos, final double dir, final double minPos, final double maxPos) {
        if (pos >= minPos && pos <= maxPos) {
            return 0.0;
        }
        else if (dir == 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        else if (dir < 0.0) {
            return pos < minPos ? Double.POSITIVE_INFINITY : (Math.abs(pos - maxPos) / Math.abs(dir));
        }
        else {
            // dir > 0.0
            return pos > maxPos ? Double.POSITIVE_INFINITY : (Math.abs(pos - minPos) / dir);
        }
    }

    /**
     * Get the maximum time for which the min-max coordinates still are hit.
     * 
     * @param pos
     * @param dir
     * @param minPos
     * @param maxPos
     * @param minTime
     *            The earliest time of collision with the min-max coordinates,
     *            as returned by getMinTimeIncludeEdges.
     * @return The maximum time for which the min-max coordinates still are hit.
     *         If no hit is possible, Double.NaN is returned. If minTime is
     *         Double.POSITIVE_INFINITY, Double.NaN is returned directly.
     *         Double.POSITIVE_INFINITY may be returned, if coordinates are
     *         colliding always.
     */
    public static double getMaxTimeIncludeEdges(final double pos, final double dir, final double minPos, final double maxPos, final double minTime) {
        if (Double.isInfinite(minTime)) {
            return Double.NaN;
        }
        else if (dir == 0.0) {
            return (pos < minPos || pos > maxPos) ? Double.NaN : Double.POSITIVE_INFINITY;
        }
        else if (dir < 0.0) {
            return pos < minPos ? Double.NaN : (Math.abs(pos - minPos) / Math.abs(dir));
        }
        else {
            // dir > 0.0
            return pos > maxPos ? Double.NaN : (Math.abs(pos - maxPos) / dir);
        }
    }

    /**
     * Get the maximum (closest) distance from the given position towards the
     * AABB regarding axes independently.
     * 
     * @param x
     *            Position of the point.
     * @param y
     * @param z
     * @param minX
     *            Minimum coordinates of the AABB.
     * @param minY
     * @param minZ
     * @param maxX
     *            Maximum coordinates of the AABB.
     * @param maxY
     * @param maxZ
     * @return
     */
    public static double getMaxAxisDistAABB(final double x, final double y, final double z,
                                            final double minX, final double minY, final double minZ,
                                            final double maxX, final double maxY, final double maxZ) {
        return Math.max(axisDistance(x,  minX, maxX), Math.max(axisDistance(y, minY, maxY), axisDistance(z, minZ, maxZ)));
    }

    /**
     * Get the maximum (closest) 'Manhattan' distance from the given position
     * towards the AABB regarding axes independently.
     * 
     * @param x
     *            Position of the point.
     * @param y
     * @param z
     * @param minX
     *            Minimum coordinates of the AABB.
     * @param minY
     * @param minZ
     * @param maxX
     *            Maximum coordinates of the AABB.
     * @param maxY
     * @param maxZ
     * @return
     */
    public static double getManhattanDistAABB(final double x, final double y, final double z,
                                              final double minX, final double minY, final double minZ,
                                              final double maxX, final double maxY, final double maxZ) {
        return axisDistance(x,  minX, maxX)+ axisDistance(y, minY, maxY) + axisDistance(z, minZ, maxZ);
    }

    /**
     * Get the squared (closest) distance from the given position towards the
     * AABB regarding axes independently.
     * 
     * @param x
     *            Position of the point.
     * @param y
     * @param z
     * @param minX
     *            Minimum coordinates of the AABB.
     * @param minY
     * @param minZ
     * @param maxX
     *            Maximum coordinates of the AABB.
     * @param maxY
     * @param maxZ
     * @return
     */
    public static double getSquaredDistAABB(final double x, final double y, final double z, final double minX, final double minY, final double minZ,
                                            final double maxX, final double maxY, final double maxZ) {
        final double dX = axisDistance(x,  minX, maxX);
        final double dY = axisDistance(y, minY, maxY);
        final double dZ = axisDistance(z, minZ, maxZ);
        return dX * dX + dY * dY + dZ * dZ;
    }

    /**
     * Get the distance towards a min-max interval (inside and edge count as 0.0
     * distance).
     * 
     * @param pos
     * @param minPos
     * @param maxPos
     * @return Positive distance always.
     */
    public static double axisDistance(final double pos, final double minPos, final double maxPos) {
        return pos < minPos ? Math.abs(pos - minPos) : (pos > maxPos ? Math.abs(pos - maxPos) : 0.0);
    }

    /**
     * Test if the player is colliding with entities. <br>
     * 
     * @param player
     * @param shouldFilter Whether the check should filter out entities that cannot push players (boats, armor stands, dead and invalid entities)
     * @return True, if the player is colliding with entities.
     */
    public static boolean isCollidingWithEntities(final Player p, double xMargin, double yMargin, double zMargin, final boolean shouldFilter) {
        if (shouldFilter) {
            List<Entity> entities = p.getNearbyEntities(xMargin, yMargin, zMargin); 
            // Minecarts, boats, armor stands, dead entities cannot push the player
            entities.removeIf(e -> e.getType() == EntityType.MINECART || e.getType() == EntityType.ARMOR_STAND || !e.isValid() || MaterialUtil.isBoat(e.getType()) || !(e instanceof LivingEntity)); //|| !((LivingEntity) e).isCollidable() || ((LivingEntity) e).isSleeping());
            return !entities.isEmpty();
        }
        return !p.getNearbyEntities(xMargin, yMargin, zMargin).isEmpty();
    }
    
    /**
     * Test if the player is colliding with entities. <br>
     * Does not use any margin.
     * 
     * @param player
     * @param shouldFilter Whether the check should filter out entities that cannot push players (boats, armor stands, dead and invalid entities)
     * @return True, if the player is colliding with entities.
     */
    public static boolean isCollidingWithEntities(final Player p, final boolean shouldFilter) {
        return isCollidingWithEntities(p, 0.0, 0.0, 0.0, shouldFilter);
    }
    
    /**
     * Get a List of entities colliding with the player's AABB (within margins) that can actually push the player.<br>
     * (Not Minecarts, Boats, dead entities (...)).
     * Intended use is within checks.
     * 
     * @param p
     * @param xMargin
     * @param yMargin
     * @param zMargin
     * @return The List containing entities that can push the player.
     */
    public static List<Entity> getCollidingEntitiesThatCanPushThePlayer(final Player p, double xMargin, double yMargin, double zMargin) {
        List<Entity> entities = p.getNearbyEntities(xMargin, yMargin, zMargin);
        entities.removeIf(e -> e.getType() == EntityType.MINECART || e.getType() == EntityType.ARMOR_STAND || !e.isValid() || MaterialUtil.isBoat(e.getType()) || !(e instanceof LivingEntity) || !((LivingEntity) e).isCollidable() || ((LivingEntity) e).isSleeping());
        return entities;
    }
    
    /**
     * Get the number of entities colliding with the player's AABB (within margins) that can actually push the player.<br>
     * (Not Minecarts, Boats, dead entities (...)).
     * Intended use is within checks.
     * 
     * @param p
     * @param xMargin
     * @param yMargin
     * @param zMargin
     * @return The number of entities that can push the player.
     */
    public static int getNumberOfEntitiesThatCanPushThePlayer(final Player p, double xMargin, double yMargin, double zMargin) {
        return getCollidingEntitiesThatCanPushThePlayer(p, xMargin, yMargin, zMargin).size();
    }


    /**
     * Simple check to see if neighbor block is nearly same direction with block trying to interact.<br>
     * For example if block interacting below or equal eye block, neighbor must be below or equal eye block.<br>
     * 
     * @param neighbor coord to check
     * @param block coord that trying to interact
     * @param eyeBlock
     * @return true if correct.
     */
    public static boolean correctDir(int neighbor, int block, int eyeBlock) {
        int d = eyeBlock - block;
        if (d > 0) {
            if (neighbor > eyeBlock) return false;
        } else if (d < 0) {
            if (neighbor < eyeBlock) return false;
        } else {
            if (neighbor < eyeBlock || neighbor > eyeBlock) return false;
        }
        return true;
    }

    /**
     * Simple check to see if neighnor block is nearly same direction with block trying to interact.<br>
     * If the check don't satisfied but the coord to check is still within min and max, check still return true.<br>
     * Design for blocks currently colliding with a bounding box<br>
     * 
     * @param neighbor coord to check
     * @param block coord that trying to interact
     * @param eyeBlock
     * @param min Min value of one axis of bounding box
     * @param max Max value of one axis of bounding box
     * @return true if correct.
     */
    public static boolean correctDir(int neighbor, int block, int eyeBlock, int min, int max) {
        if (neighbor >= min && neighbor <= max) return true;
        int d = eyeBlock - block;
        if (d > 0) {
            if (neighbor > eyeBlock) return false;
        } else if (d < 0) {
            if (neighbor < eyeBlock) return false;
        } else {
            if (neighbor < eyeBlock || neighbor > eyeBlock) return false;
        }
        return true;
    }

    /**
     * Test if from last block, the next block can pass through
     * 
     * @param rayTracing
     * @param blockCache
     * @param lastBlock The last block
     * @param x The next block
     * @param y
     * @param z
     * @param direction Approximate normalized direction to block
     * @param eyeX Eye location
     * @param eyeY
     * @param eyeZ
     * @param eyeHeight
     * @param sCollidingBox Start of bounding box(min). Can be null
     * @param eCollidingBox End of bounding box(max). Can be null
     * @return true if can.
     */
    public static boolean canPassThrough(InteractAxisTracing rayTracing, BlockCache blockCache, BlockCoord lastBlock, int x, int y, int z, Vector direction, double eyeX, double eyeY, double eyeZ, double eyeHeight, BlockCoord sCollidingBox, BlockCoord eCollidingBox) {
        double[] nextBounds = blockCache.getBounds(x, y, z);
        final Material mat = blockCache.getType(x, y, z);
        final long flags = BlockFlags.getBlockFlags(mat);
        //double[] lastBounds = blockCache.getBounds(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ());
        // Ignore initially colliding block(block inside bounding box)
        if (sCollidingBox != null && eCollidingBox != null
                && isInsideAABBIncludeEdges(x,y,z, sCollidingBox.getX(), sCollidingBox.getY(), sCollidingBox.getZ(), eCollidingBox.getX(), eCollidingBox.getY(), eCollidingBox.getZ())) return true;
        // NOTE: Simply check next block is passable but doesn't know the start point will only correct for some cases
        // For the case oak_plank and next block is air, this one is correct
        // For the case door and next block is air, this one is incorrect. Because air is null bounds
        // Trap door is combined issue of slab and door
        if (nextBounds == null || canPassThroughWorkAround(blockCache, x, y, z, direction, eyeX, eyeY, eyeZ, eyeHeight)) return true;
        //if (lastBounds == null) return true;
        // NOTE: Only one of them will be 1 at a time
        int dy = y - lastBlock.getY();
        int dx = x - lastBlock.getX();
        int dz = z - lastBlock.getZ();
        // Move the end point to nearly end of block
        double stepX = dx * 0.99;
        double stepY = dy * 0.99;
        double stepZ = dz * 0.99;
        //rayTracing.setAxisOrder(Axis.AXIS_ORDER_XZY);
        rayTracing.set(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ(), x + stepX, y + stepY, z + stepZ);
        rayTracing.setIgnoreInitiallyColliding(true);
        rayTracing.loop();
        rayTracing.setIgnoreInitiallyColliding(false);
        if (!rayTracing.collides()) return true;
        // Too headache to think out a perfect algorithm
        boolean wallConnector = (flags & (BlockFlags.F_THICK_FENCE | BlockFlags.F_THIN_FENCE)) != 0;
        boolean door = BlockProperties.isDoor(mat);
        if ((flags & BlockFlags.F_STAIRS) != 0) {
            // Stair is being interacted from side!
            if (dy == 0) {
                int eyeBlockY = Location.locToBlock(eyeY);
                // nextBounds[4]: maxY of the slab of the stair
                // nextBounds[1]: minY of the slab of the stair
                if (eyeBlockY > y && nextBounds[4] == 1.0) return false;
                if (eyeBlockY < y && nextBounds[1] == 0.0) return false;
            }
            if (dx != 0) {
                // first bound is always a slab and will be handle below
                for (int i = 2; i <= (int)nextBounds.length / 6; i++) {
                    if (nextBounds[i*6-4] == 0.0 && nextBounds[i*6-1] == 1.0 && (dx < 0 ? nextBounds[i*6-3] == 1.0 : nextBounds[i*6-6] == 0.0)) return false;
                }
            }
            if (dz != 0) {
                // first bound is always a slab and will be handle below
                for (int i = 2; i <= (int)nextBounds.length / 6; i++) {
                    if (nextBounds[i*6-6] == 0.0 && nextBounds[i*6-3] == 1.0 && (dz < 0 ? nextBounds[i*6-1] == 1.0 : nextBounds[i*6-4] == 0.0)) return false;
                }
            }
        }
        //System.out.println(dx + " " + dy + " " + dz + " " + rayTracing.getCollidingAxis());
        // NOTE: Use rayTracing.getCollidingAxis() != Axis will false with slab(for example a block to interact surround with dirt and an upper slab above)
        // Using dy > 0 ? nextBounds[1] != 0.0 : nextBounds[4] != 1.0 and similar will only a temporally workaround. Will think a way to set ray-cast end-point correctly 
        if (dy != 0) {
            if (nextBounds[0] == 0.0 && nextBounds[3] == 1.0 && nextBounds[2] == 0.0 && nextBounds[5] == 1.0) return wallConnector || door ? rayTracing.getCollidingAxis() != Axis.Y_AXIS : dy > 0 ? nextBounds[1] != 0.0 : nextBounds[4] != 1.0;
            return true;
        }
        if (dx != 0) {
            if (nextBounds[1] == 0.0 && nextBounds[4] == 1.0 && nextBounds[2] == 0.0 && nextBounds[5] == 1.0) return wallConnector || door ? rayTracing.getCollidingAxis() != Axis.X_AXIS : dx > 0 ? nextBounds[0] != 0.0 : nextBounds[3] != 1.0;
            return true;
        }
        if (dz != 0) {
            if (nextBounds[0] == 0.0 && nextBounds[3] == 1.0 && nextBounds[1] == 0.0 && nextBounds[4] == 1.0) return wallConnector || door ? rayTracing.getCollidingAxis() != Axis.Z_AXIS : dz > 0 ? nextBounds[2] != 0.0 : nextBounds[5] != 1.0;
            return true;
        }
        return false;
    }

    private static boolean canPassThroughWorkAround(BlockCache blockCache, int blockX, int blockY, int blockZ, Vector direction, double eyeX, double eyeY, double eyeZ, double eyeHeight) {
        final Material mat = blockCache.getType(blockX, blockY, blockZ);
        final long flags = BlockFlags.getBlockFlags(mat);
        // TODO: (flags & BlockFlags.F_SOLID) == 0?
        //if ((flags & BlockFlags.F_SOLID) == 0) {
            // Ignore non solid blocks anyway.
        //    return true;
        //}
        // TODO: Passable in movement doesn't mean passable in interaction
        if ((flags & (BlockFlags.F_LIQUID | BlockFlags.F_IGN_PASSABLE)) != 0) {
            return true;
        }

        if ((flags & (BlockFlags.F_THICK_FENCE | BlockFlags.F_THIN_FENCE)) != 0) {
            // Restore the Y location of player trying to interact
            int entityBlockY = Location.locToBlock(eyeY - eyeHeight);
            // if player is close to the block and look up or look down
            return direction.getY() > 0.76 && entityBlockY > blockY || direction.getY() < -0.76 && entityBlockY < blockY;
        }
        return false;
    }

    /**
     * Function to return the list of blocks that can be interact from.<br>
     * As we can only see maximum 3 sides of a cube at a time
     * 
     * @param currentBlock Current block to move on
     * @param direction
     * @param eyeX Eye location just to prioritize with Axis will attempt to try first
     * @param eyeY
     * @param eyeZ
     * @return List of blocks that can possibly interact from
     */ 
    public static List<BlockCoord> getNeighborsInDirection(BlockCoord currentBlock, Vector direction, double eyeX, double eyeY, double eyeZ) {
        List<BlockCoord> neighbors = new ArrayList<>();
        int stepY = direction.getY() > 0 ? 1 : (direction.getY() < 0 ? -1 : 0);
        int stepX = direction.getX() > 0 ? 1 : (direction.getX() < 0 ? -1 : 0);
        int stepZ = direction.getZ() > 0 ? 1 : (direction.getZ() < 0 ? -1 : 0);
        
        final double dYM = TrigUtil.manhattan(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ(), eyeX, eyeY, eyeZ);
        final double dZM = TrigUtil.manhattan(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ, eyeX, eyeY, eyeZ);
        final double dXM = TrigUtil.manhattan(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ(), eyeX, eyeY, eyeZ);
        
        // Is this one correct?
        if (dYM <= dXM && dYM <= dZM && Math.abs(direction.getY()) >= 0.5) {
            neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
            neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
            neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
            return neighbors;
        }

        if (dXM < dZM) {
            neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
            neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
            neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
        } else {
            neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
            neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
            neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
        }
        return neighbors;
    }

}