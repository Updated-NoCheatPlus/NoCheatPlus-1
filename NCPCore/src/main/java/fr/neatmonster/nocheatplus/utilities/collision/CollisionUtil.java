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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.model.InputDirection;
import fr.neatmonster.nocheatplus.compat.bukkit.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.utilities.collision.ray.InteractAxisTracing;
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
    
    /** Margin of error used by Minecraft/NMS within collision-specific contexts */
    public static final double COLLISION_EPSILON = 1.0E-7;
    private final static boolean ServerIsAtLeast1_8 = ServerVersion.isAtLeast("1.8");
    /** Temporary use, setWorld(null) once finished. */
    private static final Location useLoc = new Location(null, 0, 0, 0);


    /**
     * A function taken from NMS (LocalPlayer#aiStep()) to judge if the horizontal collision is to be considered as negligible. <br>
     * Used to determine whether the sprinting status needs to be reset on colliding with a wall (if this returns true, sprinting won't be reset then).
     *
     * @param collisionVector
     * @param to The location where the player has moved to. You must specifically use the "to" location, as it contains the most recent rotation. Using the "from" location would mean using the last rotation of the player.
     * @param strafeImpulse The player's sideways input force, represented as a double (see {@link InputDirection})
     * @param forwardImpulse The player's forward input force, represented as a double (see {@link InputDirection})
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
     * Ensure that the neighboring block is in the correct direction relative to the block being interacted with and the player’s eye position.<br>
     * Currently, this is rather meant for blocks colliding with a bounding box<br>
     *
     * @param neighbor The coordinate of the neighboring block (usually Y). 
     * @param block The coordinate of the block being interacted with (usually Y).
     * @param eyeBlock The coordinate of the player’s eye position to block coordinate (usually Y).
     * @return true if correct.
     */
    public static boolean correctDir(int neighbor, int block, int eyeBlock) {
        // The difference between the eye position and the block being interacted with. Determines the relative direction.
        int d = eyeBlock - block;
        // (eyeBlock is above block)
        if (d > 0) {
            if (neighbor > eyeBlock) {
                // Neighbour is above eyeBlock 
                return false;
            }
        }
        else if (d < 0) { // (eyeBlock is below block)
            if (neighbor < eyeBlock) {
                // Neighbour is below eyeBlock
                return false;
            }
        }
        else {
            // (eyeBlock is at the same level as block)
            if (neighbor < eyeBlock || neighbor > eyeBlock) {
                // Neighbor is at the same level as eyeBlock.
                return false;
            }
        }
        return true;
    }

    /**
     * Ensure that the neighboring block is either within the min and max boundaries, 
     * or in the correct direction relative to the block being interacted with and the player’s eye position.<br>
     * Currently, this is rather meant for blocks colliding with a bounding box<br>
     * 
     * @param neighbor The coordinate of the neighboring block (usually Y). 
     * @param block The coordinate of the block being interacted with (usually Y).
     * @param eyeBlock The coordinate of the player’s eye position to block coordinate (usually Y).
     * @param min The minimum value of one axis of the bounding box (minXYZ, usually Y).
     * @param max The maximum value of one axis of the bounding box (maxXYZ, usually Y).
     * @return True, if correct.
     */
    public static boolean correctDir(int neighbor, int block, int eyeBlock, int min, int max) {
        if (MathUtil.inRange(min, neighbor, max)) {
            return true;
        }
        return correctDir(neighbor, block, eyeBlock);
    }

    /**
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
     // TODO: Lots of cleanup pending for this function: add more comments; split into helper methods; readability improvements.
    public static boolean canPassThrough(InteractAxisTracing rayTracing, BlockCache blockCache, BlockCoord lastBlock, int x, int y, int z, Vector direction, double eyeX, double eyeY, double eyeZ, double eyeHeight,
                                         BlockCoord sCollidingBox, BlockCoord eCollidingBox, boolean mightEdgeInteraction, Axis.RichAxisData axisData) {
        double[] nextAABB = blockCache.getBounds(x, y, z);
        final Material mat = blockCache.getType(x, y, z);
        final long flags = BlockFlags.getBlockFlags(mat);
        //////////////////////////
        // 0: Early returns     //
        //////////////////////////
        if (nextAABB == null || canPassThroughWorkAround(blockCache, x, y, z, direction, eyeX, eyeY, eyeZ, eyeHeight)) {
            return true;
        }
        // NOTE: Only one of them will be 1 at a time
        int dy = y - lastBlock.getY();
        int dx = x - lastBlock.getX();
        int dz = z - lastBlock.getZ();
        // TODO: This is wrong, liguid should have no bound but still have height. But instead of messing up entire collision system, this hack work well
        mightEdgeInteraction |= (BlockFlags.getBlockFlags(blockCache.getType(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ())) & BlockFlags.F_LIQUID) != 0;
        ////////////////////////
        /// 1: Set axis data  //
        ////////////////////////
        double[] lastAABB = blockCache.getBounds(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ());
        if (lastAABB != null && nextAABB != null) {
            // Slab/door/trap door fix(3/3): Bypass : Can't interact through other side of block from one side 
            if (axisData != null) {
                if (dy != 0) {
                    // Condition: XZ of two block is full, Y is contain in other block
                    if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0
                        && nextAABB[2] == 0.0 && nextAABB[5] == 1.0
                        && lastAABB[1] == 0.0 && lastAABB[4] == 1.0
                        && lastAABB[2] == 0.0 && lastAABB[5] == 1.0
                        && MathUtil.rangeContains(nextAABB[0], lastAABB[0], nextAABB[3], lastAABB[3])) {
                        axisData.dirExclusion = nextAABB[0] == 0.0 ? Direction.X_NEG : nextAABB[3] == 1.0 ? Direction.X_POS : Direction.NONE;
                    }
                    if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0
                        && nextAABB[0] == 0.0 && nextAABB[3] == 1.0
                        && lastAABB[1] == 0.0 && lastAABB[4] == 1.0
                        && lastAABB[0] == 0.0 && lastAABB[3] == 1.0
                        && MathUtil.rangeContains(nextAABB[2], lastAABB[2], nextAABB[5], lastAABB[5])) {
                        axisData.dirExclusion = nextAABB[2] == 0.0 ? Direction.Z_NEG : nextAABB[5] == 1.0 ? Direction.Z_POS : Direction.NONE;
                    }
                }
                if (dx != 0) {
                    if (nextAABB[0] == 0.0 && nextAABB[3] == 1.0
                        && nextAABB[2] == 0.0 && nextAABB[5] == 1.0
                        && lastAABB[0] == 0.0 && lastAABB[3] == 1.0
                        && lastAABB[2] == 0.0 && lastAABB[5] == 1.0
                        && MathUtil.rangeContains(nextAABB[1], lastAABB[1], nextAABB[4], lastAABB[4])) {
                        axisData.dirExclusion = nextAABB[1] == 0.0 ? Direction.Y_NEG : nextAABB[4] == 1.0 ? Direction.Y_POS : Direction.NONE;
                    }
                    if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0
                        && nextAABB[0] == 0.0 && nextAABB[3] == 1.0
                        && lastAABB[1] == 0.0 && lastAABB[4] == 1.0
                        && lastAABB[0] == 0.0 && lastAABB[3] == 1.0
                        && MathUtil.rangeContains(nextAABB[2], lastAABB[2], nextAABB[5], lastAABB[5])) {
                        axisData.dirExclusion = nextAABB[2] == 0.0 ? Direction.Z_NEG : nextAABB[5] == 1.0 ? Direction.Z_POS : Direction.NONE;
                    }
                }
                if (dz != 0) {
                    if (nextAABB[0] == 0.0 && nextAABB[3] == 1.0
                        && nextAABB[2] == 0.0 && nextAABB[5] == 1.0
                        && lastAABB[0] == 0.0 && lastAABB[3] == 1.0
                        && lastAABB[2] == 0.0 && lastAABB[5] == 1.0
                        && MathUtil.rangeContains(nextAABB[1], lastAABB[1], nextAABB[4], lastAABB[4])) {
                        axisData.dirExclusion = nextAABB[1] == 0.0 ? Direction.Y_NEG : nextAABB[4] == 1.0 ? Direction.Y_POS : Direction.NONE;
                    }
                    if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0
                        && nextAABB[2] == 0.0 && nextAABB[5] == 1.0
                        && lastAABB[1] == 0.0 && lastAABB[4] == 1.0
                        && lastAABB[2] == 0.0 && lastAABB[5] == 1.0
                        && MathUtil.rangeContains(nextAABB[0], lastAABB[0], nextAABB[3], lastAABB[3])) {
                        axisData.dirExclusion = nextAABB[0] == 0.0 ? Direction.X_NEG : nextAABB[3] == 1.0 ? Direction.X_POS : Direction.NONE;
                    }
                }
            }
        }
        /////////////////////////////////////////////////////////////////////////
        // 2: Ignore initially colliding block(block inside bounding box)      //
        ////////////////////////////////////////////////////////////////////////
        if (sCollidingBox != null && eCollidingBox != null
            && AxisAlignedBBUtils.isInsideAABBIncludeEdges(x,y,z, sCollidingBox.getX(), sCollidingBox.getY(), sCollidingBox.getZ(), eCollidingBox.getX(), eCollidingBox.getY(), eCollidingBox.getZ())) {
            return true;
        }
        //////////////////////////////////////////////
        // 3: Test for raytracing collision         //
        //////////////////////////////////////////////
        // Move the end point to nearly end of block
        double stepX = dx * 0.99;
        double stepY = dy * 0.99;
        double stepZ = dz * 0.99;
        rayTracing.set(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ(), x + stepX, y + stepY, z + stepZ);
        rayTracing.setIgnoreInitiallyColliding(true);
        rayTracing.loop();
        rayTracing.setIgnoreInitiallyColliding(false);
        if (!rayTracing.collides()) {
            // Does not collide. The ray can pass through this direction
            return true;
        }
        // (Ray collided with something in the given direction. Check for special cases/workarounds.)
        // TODO: Document why this is needed
        //////////////////////////////////
        // 4: Handle stairs             //
        //////////////////////////////////
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
        // TODO: Document why this is needed
        ///////////////////////////////////////////
        // 5: Handle slabs on each axis          //
        ///////////////////////////////////////////
        // Y
        if (dy != 0) {
            if (nextAABB[0] == 0.0 && nextAABB[3] == 1.0 && nextAABB[2] == 0.0 && nextAABB[5] == 1.0) {
                // Slab fix(1/3): False positive: Moving on Y Axis but get obstructed by a slab like block, allow to pass, but not allow to move on Y Axis further
                if (axisData != null && (dy > 0 ? nextAABB[1] != 0.0 : nextAABB[4] != 1.0)) {
                    axisData.dirExclusion = dy > 0 ? Direction.Y_POS : Direction.Y_NEG;
                    return true;
                }
                return rayTracing.getCollidingAxis() != Axis.Y_AXIS;
            }
            // Slab fix(2/3): Bypass: lastBounds is bottom slab and nextBounds is upper slab _-, can't pass through
            // Condition: not the block trying to interact, Y axis of two block intersect, 
            if (!mightEdgeInteraction && lastAABB != null && (dy > 0 ? lastAABB[4] == 1.0 && nextAABB[1] == 0.0 : lastAABB[1] == 0.0 && nextAABB[4]==1.0)
                // Two block's X axis is full, Sum(exclude overlapping) of two block's Z axis is equal to 1.0
                && (
                    nextAABB[0] == 0.0 && lastAABB[0] == 0.0 && nextAABB[3] == 1.0 && lastAABB[3] == 1.0 
                    && MathUtil.equal(MathUtil.getCoveredSpace(lastAABB[2], lastAABB[5], nextAABB[2], nextAABB[5]), 1.0, 0.001)
                    // Or two block's Z axis is full, Sum(exclude overlapping) of two block's X axis is equal to 1.0
                    || nextAABB[2] == 0.0 && lastAABB[2] == 0.0 && nextAABB[5] == 1.0 && lastAABB[5] == 1.0 
                    && MathUtil.equal(MathUtil.getCoveredSpace(lastAABB[0], lastAABB[3], nextAABB[0], nextAABB[3]), 1.0, 0.001)
                )) {
                return false;
            }
            return true;
        }
        // X
        if (dx != 0) {
            if (nextAABB[1] == 0.0 && nextAABB[4] == 1.0 && nextAABB[2] == 0.0 && nextAABB[5] == 1.0) {
                if (axisData != null && (dx > 0 ? nextAABB[0] != 0.0 : nextAABB[3] != 1.0)) {
                    axisData.dirExclusion = dx > 0 ? Direction.X_POS : Direction.X_NEG;
                    return true;
                }
                return rayTracing.getCollidingAxis() != Axis.X_AXIS;
            }
            if (!mightEdgeInteraction && lastAABB != null && (dx > 0 ? lastAABB[3] == 1.0 && nextAABB[0] == 0.0 : lastAABB[0] == 0.0 && nextAABB[3]==1.0) 
                && (
                    nextAABB[1] == 0.0 && lastAABB[1] == 0.0 && nextAABB[4] == 1.0 && lastAABB[4] == 1.0 
                    && MathUtil.equal(MathUtil.getCoveredSpace(lastAABB[2], lastAABB[5], nextAABB[2], nextAABB[5]), 1.0, 0.001) 
                    || nextAABB[2] == 0.0 && lastAABB[2] == 0.0 && nextAABB[5] == 1.0 && lastAABB[5] == 1.0 
                    && MathUtil.equal(MathUtil.getCoveredSpace(lastAABB[1], lastAABB[4], nextAABB[1], nextAABB[4]), 1.0, 0.001)
                )) {
                return false;
            }
            return true;
        }
        // Z
        if (dz != 0) {
            if (nextAABB[0] == 0.0 && nextAABB[3] == 1.0 && nextAABB[1] == 0.0 && nextAABB[4] == 1.0) {
                if (axisData != null && (dz > 0 ? nextAABB[2] != 0.0 : nextAABB[5] != 1.0)) {
                    axisData.dirExclusion = dz > 0 ? Direction.Z_POS : Direction.Z_NEG;
                    return true;
                }
                return rayTracing.getCollidingAxis() != Axis.Z_AXIS;
            }
            if (!mightEdgeInteraction && lastAABB != null && (dz > 0 ? lastAABB[5] == 1.0 && nextAABB[2] == 0.0 : lastAABB[2] == 0.0 && nextAABB[5]==1.0)
                    && (nextAABB[1] == 0.0 && lastAABB[1] == 0.0 && nextAABB[4] == 1.0 && lastAABB[4] == 1.0 && MathUtil.equal(MathUtil.getCoveredSpace(lastAABB[0], lastAABB[3], nextAABB[0], nextAABB[3]), 1.0, 0.001)
                    || nextAABB[0] == 0.0 && lastAABB[0] == 0.0 && nextAABB[3] == 1.0 && lastAABB[3] == 1.0 && MathUtil.equal(MathUtil.getCoveredSpace(lastAABB[1], lastAABB[4], nextAABB[1], nextAABB[4]), 1.0, 0.001))) return false;
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
     * Resolves collisions between a bounding box and a list of other bounding boxes by adjusting its movement in 
     * the X, Y, and Z dimensions. The method computes how much the bounding box should move in each dimension, 
     * to avoid overlaps with the other bounding boxes.
     *
     * <p>The collision resolution is performed in a specific order:
     * 1. Adjustments are first made in the Y dimension if there is movement in that direction.
     * 2. Next, if the absolute value of movement in the X dimension is less than that of the Z one, 
     *    adjustments are made in the Z dimension before the X's.
     * 3. If necessary, adjustments in the Z dimension are recalculated after the X dimension adjustments.
     *
     * @param toCollide The Vector representing the movement to apply to the bounding box. This vector contains 
     *                  the initial displacement in the X, Y, and Z dimensions.
     * @param AABB The bounding box to be moved.
     * @param collisionBoxes A list of other bounding boxes. These are the boxes against which collisions are checked.
     *
     * @return A Vector representing the adjusted movement in the X, Y, and Z dimensions after resolving collisions.
     *         The vector's components reflect how much the original movement should be adjusted to avoid overlaps.
     */
    public static Vector collideBoundingBox(Vector toCollide, double[] AABB, List<double[]> collisionBoxes) {
        double[] tAABB = AABB.clone();
        double x = toCollide.getX();
        double y = toCollide.getY();
        double z = toCollide.getZ();
        if (y != 0.0) {
            for (double[] cb : collisionBoxes) {
                y = AxisAlignedBBUtils.collideY(cb, tAABB, y);
            }
            tAABB[1] += y;
            tAABB[4] += y;
        }
        boolean prioritizeZ = Math.abs(x) < Math.abs(z);
        if (prioritizeZ && z != 0.0) {
            for (double[] cb : collisionBoxes) {
                z = AxisAlignedBBUtils.collideZ(cb, tAABB, z);
            }
            tAABB[2] += z;
            tAABB[5] += z;
        }
        if (x != 0.0) {
            for (double[] cb : collisionBoxes) {
                x = AxisAlignedBBUtils.collideX(cb, tAABB, x);
            }
            tAABB[0] += x;
            tAABB[3] += x;
        }
        if (!prioritizeZ && z != 0.0) {
            for (double[] cb : collisionBoxes) {
                z = AxisAlignedBBUtils.collideZ(cb, tAABB, z);
            }
            tAABB[2] += z;
            tAABB[5] += z;
        }
        return new Vector(x, y, z);
    }
    
    /**
     * Checks for collisions between an entity and blocks within a specified region around the entity's bounding box. 
     * Optionally includes world border collisions if the entity is near the world border.
     *
     * <p>This method performs the following:
     * 1. Includes world border collision boxes if the entity is within a certain distance from the world border and the 
     *    world border is active.
     * 2. Iterates through blocks in the region defined by the entity's bounding box expanded by a small epsilon margin.
     * 3. For each block, determines if it should be considered for collision detection based on its material and exposure.
     * 4. Depending on the `onlyCheckCollide` flag:
     *    - If `onlyCheckCollide` is true, checks if the entity collides with any block and returns immediately if a collision is detected.
     *    - If `onlyCheckCollide` is false, collects all relevant collision boxes into the `collisionBoxes` list for further processing.
     *
     * @param blockCache The cache of block materials used to determine the type of blocks in the world.
     * @param entity The entity whose bounding box is being checked for collisions.
     * @param AABB The axis-aligned bounding box (AABB) of the entity.
     * @param collisionBoxes A list to which collision boxes will be added if `onlyCheckCollide` is false. 
     *                       Can be null if in `onlyCheckCollide` mode.
     * @param onlyCheckCollide If true, only checks for collisions and returns immediately if a collision is detected. 
     *                         If false, collects collision boxes for further processing.
     *
     * @return True if a collision is detected and `onlyCheckCollide` is true, or if a collision with the world border is detected. 
     *         Otherwise, returns false.
     */
    public static boolean getCollisionBoxes(BlockCache blockCache, Entity entity, double[] AABB, List<double[]> collisionBoxes, boolean onlyCheckCollide) {
        boolean collided = addWorldBorder(entity, AABB, collisionBoxes, onlyCheckCollide);
        if (onlyCheckCollide && collided) {
            // Already collided with the world border, return.
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
                        if (!onlyCheckCollide) {
                            double[] multiAABB = AxisAlignedBBUtils.move(blockCache.fetchBounds(x, y, z), x, y, z);
                            collisionBoxes.addAll(AxisAlignedBBUtils.splitIntoSingle(multiAABB));
                        } 
                        else if (AxisAlignedBBUtils.isCollided(blockCache.getBounds(x, y, z), x, y, z, AABB, true)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * Determines if the specified entity's bounding box is empty of any collisions within the world.
     * <p> If any collision is detected, the method returns `false`. If no collisions are detected, the method returns `true`.
     *
     * <p>The method is a wrapper around the {@link #getCollisionBoxes} method, invoking it in a mode where it only checks 
     * for collisions and returns immediately if a collision is found.
     *
     * @param blockCache 
     * @param entity The entity whose bounding box is being checked for collisions.
     * @param AABB The axis-aligned bounding box (AABB) of the entity.
     * @return `true` if no collisions are detected within the specified AABB, otherwise `false`.
     */
    public static boolean isEmpty(BlockCache blockCache, Entity entity, double[] AABB) {
        return !getCollisionBoxes(blockCache, entity, AABB, null, true);
    }
    
    /**
     * Adds the world border to the list of collision boxes if the entity is within a specified distance from the world border.
     * The method adjusts the bounding box to include the world border collision boxes if necessary.
     *
     * <p>This method performs the following:
     * 1. Checks if the world border is active and retrieves its size and center location.
     * 2. Determines the boundaries of the world border.
     * 3. If the entity is within 16 blocks of the world border, it adds the world border collision boxes to the provided list.
     *
     * @param entity The entity whose proximity to the world border is being checked.
     * @param AABB The axis-aligned bounding box (AABB) of the entity, used to check for proximity to the world border.
     * @param collisionBoxes A list to which world border collision boxes will be added if the entity is close to the border. 
     *                       If this is null, a new list will be created.
     * @param onlyCheckCollide If true, only checks for collisions with the world border and does not add collision boxes 
     *                         to the list.
     *
     * @return True if the entity is within 16 blocks of the world border and the world border collision boxes have been added.
     *         Otherwise, returns false.
     */
    public static boolean addWorldBorder(Entity entity, double[] AABB, List<double[]> collisionBoxes, boolean onlyCheckCollide) {
        if (ServerIsAtLeast1_8) {
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
    
    /**
     * From {@code Entity.java}.<br>
     * Collects possible step heights for a given bounding box by analyzing its interaction
     * with other bounding boxes in the environment. The method compares the Y-axis point
     * positions of the bounding boxes and returns a sorted array of possible step heights.
     *
     * @param AABB An array representing the bounding box as [minX, minY, minZ, maxX, maxY, maxZ].
     * @param collisionsBoxes A List of other bounding boxes, each represented as a double array.
     * @param stepHeight The maximum step height that can be traversed.
     * @param collideY The Y-coordinate of the collision point to avoid.
     * @return A sorted array of valid step heights.
     */
    public static float[] collectCandidateStepUpHeights(double[] AABB, List<double[]> collisionsBoxes, float stepHeight, float collideY) {
        // Using a set to store unique step heights
        Set<Float> stepHeights = new HashSet<>();
        // Extract the minimum Y-coordinate from the AABB
        double minY = AABB[1];
        
        // Iterate over all collision boxes
        for (double[] collision : collisionsBoxes) {
            // For each block, get possible Y-axis step points (minY and maxY)
            List<Double> yPoints = AxisAlignedBBUtils.getYPointPositions(collision);
            
            for (double possibleStepY : yPoints) {
                float yDiff = (float) (possibleStepY - minY); // Difference in height
                // Only consider steps above the current position but not equal to the collision point
                if (yDiff >= 0.0F && yDiff != collideY) {
                    if (yDiff > stepHeight) {
                        break; // Stop further processing for this block, as it exceeds max step height
                    }
                    // Add valid step heights to the set
                    stepHeights.add(yDiff);
                }
            }
        }
        
        // Convert the set of step heights to a list, then sort the list
        List<Float> sortedStepHeights = new ArrayList<>(stepHeights);
        Collections.sort(sortedStepHeights);
        
        // Convert the sorted list to an array of floats
        float[] stepHeightArray = new float[sortedStepHeights.size()];
        for (int i = 0; i < sortedStepHeights.size(); i++) {
            stepHeightArray[i] = sortedStepHeights.get(i);
        }
        // Return the sorted array of step heights
        return stepHeightArray;  
    }
    
    /**
     * Correct onto the block (from off-block), against the direction.
     * <p>This method adjusts the collision coordinate to be within the bounds of the block if it is found to be slightly outside due to calculation inaccuracies.</p>
     * 
     * @param blockC The coordinate of the block.
     * @param bdC The difference between the block's coordinate and the eye's block coordinate.
     * @param collideC The computed collision coordinate.
     * @return Adjusted coordinate.
     */
    public static double postCorrect(int blockC, int bdC, double collideC) {
        // If the computed collision coordinate is outside the block's coordinate, adjust it to be within the block.
        int ref = bdC < 0 ? blockC + 1 : blockC;
        if (Location.locToBlock(collideC) == ref) {
            return collideC;
        }
        return ref;
    }
    
    /**
     * Time until on the block (time = steps of dir).
     * <p>This method calculates the minimum time required for the player's line of sight to intersect the target block along a specific axis.</p>
     * 
     * @param eye The coordinate of the player's eye along the specific axis.
     * @param eyeBlock The block coordinate of the player's eye along the specific axis.
     * @param dir The direction vector component along the specific axis.
     * @param blockDiff The difference in block coordinates along the specific axis.
     * @return The min time.
     */
    public static double getMinTime(final double eye, final int eyeBlock, final double dir, final int blockDiff) {
        if (blockDiff == 0) {
            // If the block difference is zero, return 0.0 since the player's eye is already within the block.
            return 0.0;
        }
        // Otherwise, compute the time required to reach the block's edge from the current position.
        final double eyeOffset = Math.abs(eye - eyeBlock); // (abs not needed)
        return ((dir < 0.0 ? eyeOffset : 1.0 - eyeOffset) + (double) (Math.abs(blockDiff) - 1)) / Math.abs(dir);
    }
    
    /**
     * Time when not on the block anymore (after having hit it, time = steps of dir).
     * <p>This method calculates the maximum time the player's line of sight will remain within the target block along a specific axis.</p>
     * 
     * @param eye The coordinate of the player's eye along the specific axis.
     * @param eyeBlock The block coordinate of the player's eye along the specific axis.
     * @param dir The direction vector component along the specific axis.
     * @param tMin The minimum time to reach the block, calculated using {@code getMinTime}.
     * @return The max time.
     */
    public static double getMaxTime(final double eye, final int eyeBlock, final double dir, final double tMin) {
        if (dir == 0.0) {
            // If the direction vector component is zero, return Double.MAX_VALUE since the line of sight will always remain within the block.
            return Double.MAX_VALUE;
        }
        // Otherwise, compute the time required to exit the block from the current position.
        if (tMin == 0.0) {
            //  Already on the block, return "rest on block".
            final double eyeOffset = Math.abs(eye - eyeBlock); // (abs not needed)
            return (dir < 0.0 ? eyeOffset : 1.0 - eyeOffset) / Math.abs(dir);
        }
        // Just the time within range.
        return tMin + 1.0 /  Math.abs(dir);
    }
    
    /**
     * This method adjusts a coordinate to ensure it is within the bounds of a specific block.<br>
     * (only if outside, for correcting inside-block to edge tMin has to be checked)
     * 
     * @param coord The coordinate to be adjusted.
     * @param block The block coordinate that the adjusted coordinate should align with.
     * @return The adjusted coordinate.
     */
    public static double toBlock(final double coord, final int block) {
        final int blockDiff = block - Location.locToBlock(coord);
        if (blockDiff == 0) {
            return coord;
        }
        else {
            return Math.round(coord);
        }
    }
}