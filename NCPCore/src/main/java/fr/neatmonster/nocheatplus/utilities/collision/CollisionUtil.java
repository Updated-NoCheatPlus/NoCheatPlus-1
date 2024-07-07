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
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.utilities.ds.map.BlockCoord;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;


/**
 * Collision related static utility.
 *
 * @author asofold
 *
 */
public class CollisionUtil {

    public static final double COLLISION_EPSILON = 1.0E-7;
    private final static boolean serverHigherOEqual1_8 = ServerVersion.compareMinecraftVersion("1.8") >=0;
    /** Temporary use, setWorld(null) once finished. */
    private static final Location useLoc = new Location(null, 0, 0, 0);


    /**
     * A function taken from NMS to judge if the horizontal collision is to be considered as negligible. <br>
     * Used to determine whether the sprinting status needs to be reset on colliding with a wall (if this returns true, sprinting won't be reset then).
     *
     * @param collisionVector
     * @param to The location where the player has moved to. Used for rotations.
     * @param strafeImpulse The player's sideways input force represented as a double (see InputDirection.java)
     * @param forwardImpulse The player's forward input force represented as a double (see InputDirection.java)
     * @return True, if the collision's angle is less than 0.13962633907794952.
     */
    public static boolean isHorizontalCollisionNegligible(Vector collisionVector, final PlayerLocation to, double strafeImpulse, double forwardImpulse) {
        float radYaw = to.getYaw() * TrigUtil.toRadians;
        double sinYaw = (double)TrigUtil.sin(radYaw);
        double cosYaw = (double)TrigUtil.cos(radYaw);
        // NOTE: xxa = strafe, zza = forward
        double var7 = (double)strafeImpulse * cosYaw - (double)forwardImpulse * sinYaw;
        double var9 = (double)forwardImpulse * cosYaw + (double)strafeImpulse * sinYaw;
        double var11 = MathUtil.square(var7) + MathUtil.square(var9);
        double var13 = MathUtil.square(collisionVector.getX()) + MathUtil.square(collisionVector.getZ());
        if (!(var11 < 9.999999747378752E-6D) && !(var13 < 9.999999747378752E-6D)) {
            double var15 = var7 * collisionVector.getX() + var9 * collisionVector.getZ();
            double collisionAngle = Math.acos(var15 / Math.sqrt(var11 * var13));
            return collisionAngle < 0.13962633907794952D; // MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN
        }
        return false;
    }

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
    public static double directionCheck(final double sourceX, final double sourceY, final double sourceZ, final double dirX,
                                        final double dirY, final double dirZ, final double targetX, final double targetY, final double targetZ,
                                        final double targetWidth, final double targetHeight, final double precision) {
        //      // TODO: Here we have 0.x vs. 2.x, sometimes !
        //      NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, "COMBINED: " + combinedDirectionCheck(sourceX, sourceY, sourceZ, dirX, dirY, dirZ, targetX, targetY, targetZ, targetWidth, targetHeight, precision, 60));
        // TODO: rework / standardize.
        double dirLength = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (dirLength == 0.0) {
            dirLength = 1.0; // ...
        }

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
        if (off > 1) {
            off = Math.sqrt(off);
        }
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
     * @param shouldFilter Whether the check should filter out entities that cannot push players (boats, armor stands, dead and invalid entities)
     * @return True, if the player is colliding with entities, within the given margins.
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
     * @param shouldFilter Whether the check should filter out entities that cannot push players (boats, armor stands, dead and invalid entities)
     * @return True, if the player is colliding with entities.
     */
    public static boolean isCollidingWithEntities(final Player p, final boolean shouldFilter) {
        return isCollidingWithEntities(p, 0.0, 0.0, 0.0, shouldFilter);
    }
    
    /**
     * Get a List of entities colliding with the player's AABB (within margins) that can actually push the player.<br>
     * (Not Minecarts, Boats, dead entities (...)).
     * Intended use is within moving checks.
     *
     * @param p
     * @param xMargin
     * @param yMargin
     * @param zMargin
     * @return The List containing entities that can push the player.
     */
    public static List<Entity> getCollidingEntitiesThatCanPushThePlayer(final Player p, double xMargin, double yMargin, double zMargin) {
        List<Entity> entities = p.getNearbyEntities(xMargin, yMargin, zMargin);
        // isValid() somehow broken???
        entities.removeIf(e -> e.isDead() || !(e instanceof LivingEntity) || !((LivingEntity) e).isCollidable() || ((LivingEntity) e).isSleeping());
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
        if (neighbor >= min && neighbor <= max) {
            return true;
        }
        int d = eyeBlock - block;
        if (d > 0) {
            if (neighbor > eyeBlock) {
                return false;
            }
        }
        else if (d < 0) {
            if (neighbor < eyeBlock) {
                return false;
            }
        }
        else {
            if (neighbor < eyeBlock || neighbor > eyeBlock) {
                return false;
            }
        }
        return true;
    }

    /**
     * Test if from last block, the next block can pass through
     *
     * @param rayTracing
     * @param blockCache
     * @param lastBlock The last block
     * @param x The next block's coordinates.
     * @param y
     * @param z
     * @param direction Approximate normalized direction to block
     * @param eyeX Eye location
     * @param eyeY
     * @param eyeZ
     * @param eyeHeight
     * @param sCollidingBox Start of bounding box(min). Can be null
     * @param eCollidingBox End of bounding box(max). Can be null
     * @param mightEdgeInteraction
     * @param axisData Auxiliary stuff for specific usage can be null
     *
     * @return true if can.
     */
    public static boolean canPassThrough(InteractAxisTracing rayTracing, BlockCache blockCache, BlockCoord lastBlock, int x, int y, int z, Vector direction, double eyeX, double eyeY, double eyeZ, double eyeHeight,
                                         BlockCoord sCollidingBox, BlockCoord eCollidingBox, boolean mightEdgeInteraction, RichAxisData axisData) {
        double[] nextAABB = blockCache.getBounds(x, y, z);
        final Material mat = blockCache.getType(x, y, z);
        final long flags = BlockFlags.getBlockFlags(mat);
        if (nextAABB == null || canPassThroughWorkAround(blockCache, x, y, z, direction, eyeX, eyeY, eyeZ, eyeHeight)) {
            return true;
        }
        // NOTE: Only one of them will be 1 at a time
        int dy = y - lastBlock.getY();
        int dx = x - lastBlock.getX();
        int dz = z - lastBlock.getZ();
        // TODO: This is wrong, liguid should have no bound but still have height. But instead of messing up entire collision system, this hack work well
        mightEdgeInteraction |= (BlockFlags.getBlockFlags(blockCache.getType(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ())) & BlockFlags.F_LIQUID) != 0;
        // Door and trap door
        double[] lastAABB = blockCache.getBounds(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ());
        //final Material lastmat = blockCache.getType(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ());
        if (lastAABB != null && nextAABB != null) {
            // Slab/door/trap door fix(3/3): Bypass : Can't interact through other side of block from one side 
            if (axisData != null) {
                if (dy != 0) {
                    // Condition: XZ of two block is full, Y is contain in other block
                    if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0
                        && nextAABB[2] == 0.0 && nextAABB[5] == 1.0
                        && lastAABB[1] == 0.0 && lastAABB[4] == 1.0
                        && lastAABB[2] == 0.0 && lastAABB[5] == 1.0
                        && rangeContains(nextAABB[0], lastAABB[0], nextAABB[3], lastAABB[3])) {
                        axisData.exclude = nextAABB[0] == 0.0 ? Direction.X_NEG : nextAABB[3] == 1.0 ? Direction.X_POS : Direction.NONE;
                    }
                    if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0
                        && nextAABB[0] == 0.0 && nextAABB[3] == 1.0
                        && lastAABB[1] == 0.0 && lastAABB[4] == 1.0
                        && lastAABB[0] == 0.0 && lastAABB[3] == 1.0
                        && rangeContains(nextAABB[2], lastAABB[2], nextAABB[5], lastAABB[5])) {
                        axisData.exclude = nextAABB[2] == 0.0 ? Direction.Z_NEG : nextAABB[5] == 1.0 ? Direction.Z_POS : Direction.NONE;
                    }
                }
                if (dx != 0) {
                    if (nextAABB[0] == 0.0 && nextAABB[3] == 1.0
                        && nextAABB[2] == 0.0 && nextAABB[5] == 1.0
                        && lastAABB[0] == 0.0 && lastAABB[3] == 1.0
                        && lastAABB[2] == 0.0 && lastAABB[5] == 1.0
                        && rangeContains(nextAABB[1], lastAABB[1], nextAABB[4], lastAABB[4])) {
                        axisData.exclude = nextAABB[1] == 0.0 ? Direction.Y_NEG : nextAABB[4] == 1.0 ? Direction.Y_POS : Direction.NONE;
                    }
                    if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0
                        && nextAABB[0] == 0.0 && nextAABB[3] == 1.0
                        && lastAABB[1] == 0.0 && lastAABB[4] == 1.0
                        && lastAABB[0] == 0.0 && lastAABB[3] == 1.0
                        && rangeContains(nextAABB[2], lastAABB[2], nextAABB[5], lastAABB[5])) {
                        axisData.exclude = nextAABB[2] == 0.0 ? Direction.Z_NEG : nextAABB[5] == 1.0 ? Direction.Z_POS : Direction.NONE;
                    }
                }
                if (dz != 0) {
                    if (nextAABB[0] == 0.0 && nextAABB[3] == 1.0
                        && nextAABB[2] == 0.0 && nextAABB[5] == 1.0
                        && lastAABB[0] == 0.0 && lastAABB[3] == 1.0
                        && lastAABB[2] == 0.0 && lastAABB[5] == 1.0
                        && rangeContains(nextAABB[1], lastAABB[1], nextAABB[4], lastAABB[4])) {
                        axisData.exclude = nextAABB[1] == 0.0 ? Direction.Y_NEG : nextAABB[4] == 1.0 ? Direction.Y_POS : Direction.NONE;
                    }
                    if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0
                        && nextAABB[2] == 0.0 && nextAABB[5] == 1.0
                        && lastAABB[1] == 0.0 && lastAABB[4] == 1.0
                        && lastAABB[2] == 0.0 && lastAABB[5] == 1.0
                        && rangeContains(nextAABB[0], lastAABB[0], nextAABB[3], lastAABB[3])) {
                        axisData.exclude = nextAABB[0] == 0.0 ? Direction.X_NEG : nextAABB[3] == 1.0 ? Direction.X_POS : Direction.NONE;
                    }
                }
            }
        }
        // Ignore initially colliding block(block inside bounding box)
        if (sCollidingBox != null && eCollidingBox != null
            && isInsideAABBIncludeEdges(x,y,z, sCollidingBox.getX(), sCollidingBox.getY(), sCollidingBox.getZ(), eCollidingBox.getX(), eCollidingBox.getY(), eCollidingBox.getZ())) {
            return true;
        }
        // Move the end point to nearly end of block
        double stepX = dx * 0.99;
        double stepY = dy * 0.99;
        double stepZ = dz * 0.99;
        rayTracing.set(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ(), x + stepX, y + stepY, z + stepZ);
        rayTracing.setIgnoreInitiallyColliding(true);
        rayTracing.loop();
        rayTracing.setIgnoreInitiallyColliding(false);
        if (!rayTracing.collides()) {
            return true;
        }
        // Too headache to think out a perfect algorithm
        if ((flags & BlockFlags.F_STAIRS) != 0) {
            // Stair is being interacted from side!
            if (dy == 0) {
                int eyeBlockY = Location.locToBlock(eyeY);
                // nextBounds[4]: maxY of the slab of the stair
                // nextBounds[1]: minY of the slab of the stair
                if (eyeBlockY > y && nextAABB[4] == 1.0) {
                    return false;
                }
                if (eyeBlockY < y && nextAABB[1] == 0.0) {
                    return false;
                }
            }
            if (dx != 0) {
                // first bound is always a slab and will be handle below
                for (int i = 2; i <= (int)nextAABB.length / 6; i++) {
                    if (nextAABB[i*6-4] == 0.0 && nextAABB[i*6-1] == 1.0 && (dx < 0 ? nextAABB[i*6-3] == 1.0 : nextAABB[i*6-6] == 0.0)) {
                        return false;
                    }
                }
            }
            if (dz != 0) {
                // first bound is always a slab and will be handle below
                for (int i = 2; i <= (int)nextAABB.length / 6; i++) {
                    if (nextAABB[i*6-6] == 0.0 && nextAABB[i*6-3] == 1.0 && (dz < 0 ? nextAABB[i*6-1] == 1.0 : nextAABB[i*6-4] == 0.0)) {
                        return false;
                    }
                }
            }
        }
        if (dy != 0) {
            if (nextAABB[0] == 0.0 && nextAABB[3] == 1.0 && nextAABB[2] == 0.0 && nextAABB[5] == 1.0) {
                // Slab fix(1/3): False positive: Moving on Y Axis but get obstructed by a slab like block, allow to pass, but not allow to move on Y Axis further
                if (axisData != null && (dy > 0 ? nextAABB[1] != 0.0 : nextAABB[4] != 1.0)) {
                    axisData.exclude = dy > 0 ? Direction.Y_POS : Direction.Y_NEG;
                    return true;
                }
                return rayTracing.getCollidingAxis() != Axis.Y_AXIS;
            }
            // Slab fix(2/3): Bypass: lastBounds is bottom slab and nextBounds is upper slab _-, can't pass through
            // Condition: not the block trying to interact, Y axis of two block intersect, 
            if (!mightEdgeInteraction && lastAABB != null && (dy > 0 ? lastAABB[4] == 1.0 && nextAABB[1] == 0.0 : lastAABB[1] == 0.0 && nextAABB[4]==1.0)
                    // Two block's X axis is full, Sum(exclude overlapping) of two block's Z axis is equal to 1.0 
                    && (nextAABB[0] == 0.0 && lastAABB[0] == 0.0 && nextAABB[3] == 1.0 && lastAABB[3] == 1.0 && MathUtil.equal(getFilledSpace(lastAABB[2], lastAABB[5], nextAABB[2], nextAABB[5]), 1.0, 0.001)
                    // Or two block's Z axis is full, Sum(exclude overlapping) of two block's X axis is equal to 1.0 
                    || nextAABB[2] == 0.0 && lastAABB[2] == 0.0 && nextAABB[5] == 1.0 && lastAABB[5] == 1.0 && MathUtil.equal(getFilledSpace(lastAABB[0], lastAABB[3], nextAABB[0], nextAABB[3]), 1.0, 0.001))) return false;
            return true;
        }
        if (dx != 0) {
            if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0 && nextAABB[2] == 0.0 && nextAABB[5] == 1.0) {
                if (axisData != null && (dx > 0 ? nextAABB[0] != 0.0 : nextAABB[3] != 1.0)) {
                    axisData.exclude = dx > 0 ? Direction.X_POS : Direction.X_NEG;
                    return true;
                }
                return rayTracing.getCollidingAxis() != Axis.X_AXIS;
            }
            if (!mightEdgeInteraction && lastAABB != null && (dx > 0 ? lastAABB[3] == 1.0 && nextAABB[0] == 0.0 : lastAABB[0] == 0.0 && nextAABB[3]==1.0)
                    && (nextAABB[1] == 0.0 && lastAABB[1] == 0.0 && nextAABB[4] == 1.0 && lastAABB[4] == 1.0 && MathUtil.equal(getFilledSpace(lastAABB[2], lastAABB[5], nextAABB[2], nextAABB[5]), 1.0, 0.001)
                    || nextAABB[2] == 0.0 && lastAABB[2] == 0.0 && nextAABB[5] == 1.0 && lastAABB[5] == 1.0 && MathUtil.equal(getFilledSpace(lastAABB[1], lastAABB[4], nextAABB[1], nextAABB[4]), 1.0, 0.001))) return false;
            return true;
        }
        if (dz != 0) {
            if (nextAABB[0] == 0.0 && nextAABB[3] == 1.0 && nextAABB[1] == 0.0 && nextAABB[4] == 1.0) {
                if (axisData != null && (dz > 0 ? nextAABB[2] != 0.0 : nextAABB[5] != 1.0)) {
                    axisData.exclude = dz > 0 ? Direction.Z_POS : Direction.Z_NEG;
                    return true;
                }
                return rayTracing.getCollidingAxis() != Axis.Z_AXIS;
            }
            if (!mightEdgeInteraction && lastAABB != null && (dz > 0 ? lastAABB[5] == 1.0 && nextAABB[2] == 0.0 : lastAABB[2] == 0.0 && nextAABB[5]==1.0)
                    && (nextAABB[1] == 0.0 && lastAABB[1] == 0.0 && nextAABB[4] == 1.0 && lastAABB[4] == 1.0 && MathUtil.equal(getFilledSpace(lastAABB[0], lastAABB[3], nextAABB[0], nextAABB[3]), 1.0, 0.001)
                    || nextAABB[0] == 0.0 && lastAABB[0] == 0.0 && nextAABB[3] == 1.0 && lastAABB[3] == 1.0 && MathUtil.equal(getFilledSpace(lastAABB[1], lastAABB[4], nextAABB[1], nextAABB[4]), 1.0, 0.001))) return false;
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
        // TODO: Passable in movement doesn't mean passable in interaction(F_INTERACT_PASSABLE?)
        // To achive this, first, need to change collision system to flag passable block(complicated), 
        // second add flag F_INTERACT_PASSABLE to ignore block can truly passable, 
        // third add bounds to BlockCacheBukkit.java
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
     * Function to return the list of blocks that can be interacted from.<br>
     * As we can only see maximum 3 sides of a cube at a time
     *
     * @param currentBlock Current block to move on
     * @param direction
     * @param eyeX Eye location just to automatically prioritize with Axis will attempt to try first
     * @param eyeY
     * @param eyeZ
     * @param axisData Rich data for specific usage. Can be null. If not null will consume data
     * @return List of blocks that can possibly interact from
     */
    public static List<BlockCoord> getNeighborsInDirection(BlockCoord currentBlock, Vector direction, double eyeX, double eyeY, double eyeZ, RichAxisData axisData) {
        List<BlockCoord> neighbors = new ArrayList<>();
        int stepY = direction.getY() > 0 ? 1 : (direction.getY() < 0 ? -1 : 0);
        int stepX = direction.getX() > 0 ? 1 : (direction.getX() < 0 ? -1 : 0);
        int stepZ = direction.getZ() > 0 ? 1 : (direction.getZ() < 0 ? -1 : 0);
        Axis priorityAxis = Axis.NONE;
        Direction excludeDir = Direction.NONE;
        boolean allowX = true;
        boolean allowY = true;
        boolean allowZ = true;
        if (axisData != null) {
            priorityAxis = axisData.priority;
            excludeDir = axisData.exclude;
            axisData.priority = Axis.NONE;
            axisData.exclude = Direction.NONE;
            allowX = !(excludeDir == Direction.X_NEG && stepX < 0 || excludeDir == Direction.X_POS && stepX > 0);
            allowY = !(excludeDir == Direction.Y_NEG && stepY < 0 || excludeDir == Direction.Y_POS && stepY > 0);
            allowZ = !(excludeDir == Direction.Z_NEG && stepZ < 0 || excludeDir == Direction.Z_POS && stepZ > 0);
        }
        switch (priorityAxis) {
            case X_AXIS:
                neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
                if (allowZ) {
                    neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
                }
                if (allowY) {
                    neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
                }
                return neighbors;
            case Y_AXIS:
                neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
                if (allowX) {
                    neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
                }
                if (allowZ) {
                    neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
                }
                return neighbors;
            case Z_AXIS:
                neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
                if (allowX) {
                    neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
                }
                if (allowY) {
                    neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
                }
                return neighbors;
            default:
                break;
        }
        
        final double dYM = TrigUtil.manhattan(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ(), eyeX, eyeY, eyeZ);
        final double dZM = TrigUtil.manhattan(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ, eyeX, eyeY, eyeZ);
        final double dXM = TrigUtil.manhattan(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ(), eyeX, eyeY, eyeZ);
        // Is this one correct?
        if (dYM <= dXM && dYM <= dZM && Math.abs(direction.getY()) >= 0.5) {
            if (allowY) {
                neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
            }
            // Do sort priority of XZ in case Y not possible
            if (dXM < dZM) {
                if (allowX) {
                    neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
                }
                if (allowZ) {
                    neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
                }
            }
            else {
                if (allowZ) {
                    neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
                }
                if (allowX) {
                    neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
                }
            }
            return neighbors;
        }

        if (dXM < dZM) {
            if (allowX) {
                neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
            }
            if (allowZ) {
                neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
            }
            if (allowY) {
                neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
            }
        }
        else {
            if (allowZ) {
                neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY(), currentBlock.getZ() + stepZ));
            }
            if (allowX) {
                neighbors.add(new BlockCoord(currentBlock.getX() + stepX, currentBlock.getY(), currentBlock.getZ()));
            }
            if (allowY) {
                neighbors.add(new BlockCoord(currentBlock.getX(), currentBlock.getY() + stepY, currentBlock.getZ()));
            }
        }
        return neighbors;
    }

    // TODO: Move to another utility class
    private static double getFilledSpace(double sA, double eA, double sB, double eB) {
        return (eA-sA) + (eB-sB) - Math.max(0, Math.min(eA, eB) - Math.max(sA, sB));
    }

    // TODO: Move to another utility class
    private static boolean rangeContains(double lBMin, double nBMin, double lBMax, double nBMax) {
        return nBMin <= lBMin && lBMax <=nBMax || lBMin <= nBMin && nBMax <= lBMax;
    }

    public static class RichAxisData {
        public Axis priority;
        public Direction exclude;
        public RichAxisData(Axis priority, Direction exclude) {
            this.priority = priority;
            this.exclude = exclude;
        }
    }

    // TODO: Move to another utility class
    
    /**
     * Move the bounding box by the given coordinates.
     * 
     * @param AABB
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static double[] move(double[] AABB, double x, double y, double z) {
        if (AABB.length % 6 == 0) {
            double[] tAABB = AABB.clone();
            for (int i = 1; i <= (int)tAABB.length / 6; i++) {
                tAABB[i*6-6] += x;
                tAABB[i*6-3] += x;
                tAABB[i*6-5] += y;
                tAABB[i*6-2] += y;
                tAABB[i*6-4] += z;
                tAABB[i*6-1] += z;
            }
            return tAABB;
        }
        return AABB;
    }

    public static Vector collideBoundingBox(Vector toCollide, double[] AABB, List<double[]> collisionBoxes) {
        double[] tAABB = AABB.clone();
        double x = toCollide.getX();
        double y = toCollide.getY();
        double z = toCollide.getZ();
        if (y != 0.0) {
            for (double[] cb : collisionBoxes) {
                y = collideY(cb, tAABB, y);
            }
            tAABB[1] += y;
            tAABB[4] += y;
        }
        boolean flag = Math.abs(x) < Math.abs(z);
        if (flag && z != 0.0) {
            for (double[] cb : collisionBoxes) {
                z = collideZ(cb, tAABB, z);
            }
            tAABB[2] += z;
            tAABB[5] += z;
        }
        if (x != 0.0) {
            for (double[] cb : collisionBoxes) {
                x = collideX(cb, tAABB, x);
            }
            tAABB[0] += x;
            tAABB[3] += x;
        }
        if (!flag && z != 0.0) {
            for (double[] cb : collisionBoxes) {
                z = collideZ(cb, tAABB, z);
            }
            tAABB[2] += z;
            tAABB[5] += z;
        }
        return new Vector(x, y, z);
    }
    
    // TODO: Move to another utility class
    public static double[] expandToCoordinate(double[] AABB, double x, double y, double z) {
        double[] tAABB = AABB.clone();
        if (x < 0.0D) {
            tAABB[0] += x;
        } else {
            tAABB[3] += x;
        }

        if (y < 0.0D) {
            tAABB[1] += y;
        } else {
            tAABB[4] += y;
        }

        if (z < 0.0D) {
            tAABB[2] += z;
        } else {
            tAABB[5] += z;
        }
        return tAABB;
    }

    public static boolean getCollisionBoxes(BlockCache blockCache, Entity entity, double[] AABB, List<double[]> collisionBoxes, boolean onlyCheckCollide) {
        boolean collided = addWorldBorder(entity, AABB, collisionBoxes, onlyCheckCollide);
        if (onlyCheckCollide && collided) {
            return true;
        }
        int minBlockX = (int) Math.floor(AABB[0] - COLLISION_EPSILON) - 1;
        int maxBlockX = (int) Math.floor(AABB[3] + COLLISION_EPSILON) + 1;
        int minBlockY = (int) Math.floor(AABB[1] - COLLISION_EPSILON) - 1;
        int maxBlockY = (int) Math.floor(AABB[4] + COLLISION_EPSILON) + 1;
        int minBlockZ = (int) Math.floor(AABB[2] - COLLISION_EPSILON) - 1;
        int maxBlockZ = (int) Math.floor(AABB[5] + COLLISION_EPSILON) + 1;
        for (int y = minBlockY; y < maxBlockY; y++) {
            for (int x = minBlockX; x <= maxBlockX; x++) {
                for (int z = minBlockZ; z <= maxBlockZ; z++) {
                    Material mat = blockCache.getType(x, y, z);
                    if (BlockProperties.isAir(mat) || BlockProperties.isPassable(mat)) {
                        continue;
                    }
                    int edgeCount = ((x == minBlockX || x == maxBlockX) ? 1 : 0) +
                                    ((y == minBlockY || y == maxBlockY) ? 1 : 0) +
                                    ((z == minBlockZ || z == maxBlockZ) ? 1 : 0);
                    if (edgeCount != 3 && (edgeCount != 1 || (BlockFlags.getBlockFlags(mat) & BlockFlags.F_HEIGHT150) != 0) 
                            && (edgeCount != 2 || mat == BridgeMaterial.MOVING_PISTON)) {
                        // Don't add to a list if we only care if the player intersects with the block
                        //if (!onlyCheckCollide) {
                            double[] multiAABB = move(blockCache.fetchBounds(x, y, z), x, y, z);
                            collisionBoxes.addAll(splitIntoSingle(multiAABB));
                        //} else if (CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).isCollided(wantedBB)) {
                        //    return true;
                        //}
                    }
                }
            }
        }
        
//        final int minSection = blockCache.getMinBlockY() >> 4;
//        final int minBlock = minSection << 4;
//        final int maxBlock = blockCache.getMaxBlockY() - 1;
//
//        int minChunkX = minBlockX >> 4;
//        int maxChunkX = maxBlockX >> 4;
//
//        int minChunkZ = minBlockZ >> 4;
//        int maxChunkZ = maxBlockZ >> 4;
//
//        int minYIterate = Math.max(minBlock, minBlockY);
//        int maxYIterate = Math.min(maxBlock, maxBlockY);
//        for (int currChunkZ = minChunkZ; currChunkZ <= maxChunkZ; ++currChunkZ) {
//            int minZ = currChunkZ == minChunkZ ? minBlockZ & 15 : 0; // coordinate in chunk
//            int maxZ = currChunkZ == maxChunkZ ? maxBlockZ & 15 : 15; // coordinate in chunk
//
//            for (int currChunkX = minChunkX; currChunkX <= maxChunkX; ++currChunkX) {
//                int minX = currChunkX == minChunkX ? minBlockX & 15 : 0; // coordinate in chunk
//                int maxX = currChunkX == maxChunkX ? maxBlockX & 15 : 15; // coordinate in chunk
//
//                int chunkXGlobalPos = currChunkX << 4;
//                int chunkZGlobalPos = currChunkZ << 4;
//
//                if (!entity.getWorld().isChunkLoaded(currChunkX, currChunkZ)) continue;
//                for (int y = minYIterate; y <= maxYIterate; ++y) {
//                    for (int currZ = minZ; currZ <= maxZ; ++currZ) {
//                        for (int currX = minX; currX <= maxX; ++currX) {
//                            int x = currX | chunkXGlobalPos;
//                            int z = currZ | chunkZGlobalPos;
//                            Material mat = blockCache.getType(x, y, z);
//                            if (BlockProperties.isAir(mat) || BlockProperties.isPassable(mat)) continue;
//
//                            int edgeCount = ((x == minBlockX || x == maxBlockX) ? 1 : 0) +
//                                    ((y == minBlockY || y == maxBlockY) ? 1 : 0) +
//                                    ((z == minBlockZ || z == maxBlockZ) ? 1 : 0);
//
//                            if (edgeCount != 3 && (edgeCount != 1 || (BlockFlags.getBlockFlags(mat) & BlockFlags.F_HEIGHT150) != 0)
//                                    && (edgeCount != 2 || mat == BridgeMaterial.MOVING_PISTON)) {
//                                // Don't add to a list if we only care if the player intersects with the block
//                                //if (!onlyCheckCollide) {
//                                    double[] multiAABB = move(blockCache.fetchBounds(x, y, z), x, y, z);
//                                    collisionBoxes.addAll(splitIntoSingle(multiAABB));
//                                //} else if (CollisionData.getData(data.getType()).getMovementCollisionBox(player, player.getClientVersion(), data, x, y, z).isCollided(wantedBB)) {
//                                //    return true;
//                                //}
//                            }
//                        }
//                    }
//                }
//            }
//        }
        return false;
    }

    /**
     * Split the given block's multiple bounding boxes into single ones.
     * 
     * @param multiAABB
     * @return
     */
    // TODO: Move to another utility class.
    public static List<double[]> splitIntoSingle(double[] multiAABB) {
        List<double[]> a = new ArrayList<>();
        if (multiAABB.length % 6 == 0) {
            for (int i = 1; i <= (int)multiAABB.length / 6; i++) {
                a.add(new double[] {multiAABB[i*6-6], multiAABB[i*6-5], multiAABB[i*6-4], multiAABB[i*6-3], multiAABB[i*6-2], multiAABB[i*6-1]});
            }
        }
        return a;
    }

    public static boolean addWorldBorder(Entity entity, double[] AABB, List<double[]> collisionBoxes, boolean onlyCheckCollide) {
        if (serverHigherOEqual1_8) {
            WorldBorder border = entity.getWorld().getWorldBorder();
            Location tloc = border.getCenter();
            double centerX = tloc.getX();
            double centerZ = tloc.getZ();

            double size = border.getSize() / 2;
            double absoluteMaxSize = 29999984;

            double minX = Math.floor(MathUtil.clamp(centerX - size, -absoluteMaxSize, absoluteMaxSize));
            double minZ = Math.floor(MathUtil.clamp(centerZ - size, -absoluteMaxSize, absoluteMaxSize));
            double maxX = Math.ceil(MathUtil.clamp(centerX + size, -absoluteMaxSize, absoluteMaxSize));
            double maxZ = Math.ceil(MathUtil.clamp(centerZ + size, -absoluteMaxSize, absoluteMaxSize));

            // If the player is fully within the world border
            // TODO: Allow to input location?
            Location lastPlayerLoc = entity.getLocation(tloc);
            double toMinX = lastPlayerLoc.getX() - minX;
            double toMaxX = maxX - lastPlayerLoc.getX();
            double minimumInXDirection = Math.min(toMinX, toMaxX);

            double toMinZ = lastPlayerLoc.getZ() - minZ;
            double toMaxZ = maxZ - lastPlayerLoc.getZ();
            double minimumInZDirection = Math.min(toMinZ, toMaxZ);

            double distanceToBorder = Math.min(minimumInXDirection, minimumInZDirection);

            // If the player's is within 16 blocks of the world border, add the world border to the collisions (optimization)
            if (distanceToBorder < 16 && lastPlayerLoc.getX() > minX && lastPlayerLoc.getX() < maxX && lastPlayerLoc.getZ() > minZ && lastPlayerLoc.getZ() < maxZ) {
                if (collisionBoxes == null) collisionBoxes = new ArrayList<>();

                // South border
                collisionBoxes.add(new double[] {minX - 10, Double.NEGATIVE_INFINITY, maxZ, maxX + 10, Double.POSITIVE_INFINITY, maxZ});
                // North border
                collisionBoxes.add(new double[] {minX - 10, Double.NEGATIVE_INFINITY, minZ, maxX + 10, Double.POSITIVE_INFINITY, minZ});
                // East border
                collisionBoxes.add(new double[] {maxX, Double.NEGATIVE_INFINITY, minZ - 10, maxX, Double.POSITIVE_INFINITY, maxZ + 10});
                // West border
                collisionBoxes.add(new double[] {minX, Double.NEGATIVE_INFINITY, minZ - 10, minX, Double.POSITIVE_INFINITY, maxZ + 10});

                //if (onlyCheckCollide) {
                //    for (double[] box : collisionBoxes) {
                //        if (isIntersected(box, AABB)) return true;
                //    }
                //}
            }
        }
        
        return false;
    }

    public static double collideX(double[] AABB, double[] other, double offsetX) {
        if (offsetX != 0 && (other[1] - AABB[4]) < -COLLISION_EPSILON && (other[4] - AABB[1]) > COLLISION_EPSILON &&
                (other[2] - AABB[5]) < -COLLISION_EPSILON && (other[5] - AABB[2]) > COLLISION_EPSILON) {

            if (offsetX >= 0.0) {
                double max_move = AABB[0] - other[3];
                if (max_move < -COLLISION_EPSILON) {
                    return offsetX;
                }
                return Math.min(max_move, offsetX);
            } else {
                double max_move = AABB[3] - other[0];
                if (max_move > COLLISION_EPSILON) {
                    return offsetX;
                }
                return Math.max(max_move, offsetX);
            }
        }
        return offsetX;
    }

    public static double collideY(double[] AABB, double[] other, double offsetY) {
        if (offsetY != 0 && (other[0] - AABB[3]) < -COLLISION_EPSILON && (other[3] - AABB[0]) > COLLISION_EPSILON &&
                (other[2] - AABB[5]) < -COLLISION_EPSILON && (other[5] - AABB[2]) > COLLISION_EPSILON) {
            if (offsetY >= 0.0) {
                double max_move = AABB[1] - other[4];
                if (max_move < -COLLISION_EPSILON) {
                    return offsetY;
                }
                return Math.min(max_move, offsetY);
            } else {
                double max_move = AABB[4] - other[1];
                if (max_move > COLLISION_EPSILON) {
                    return offsetY;
                }
                return Math.max(max_move, offsetY);
            }
        }
        return offsetY;
    }

    public static double collideZ(double[] AABB, double[] other, double offsetZ) {
        if (offsetZ != 0 && (other[0] - AABB[3]) < -COLLISION_EPSILON && (other[3] - AABB[0]) > COLLISION_EPSILON &&
                (other[1] - AABB[4]) < -COLLISION_EPSILON && (other[4] - AABB[1]) > COLLISION_EPSILON) {
            if (offsetZ >= 0.0) {
                double max_move = AABB[2] - other[5];
                if (max_move < -COLLISION_EPSILON) {
                    return offsetZ;
                }
                return Math.min(max_move, offsetZ);
            } else {
                double max_move = AABB[5] - other[2];
                if (max_move > COLLISION_EPSILON) {
                    return offsetZ;
                }
                return Math.max(max_move, offsetZ);
            }
        }
        return offsetZ;
    }
}