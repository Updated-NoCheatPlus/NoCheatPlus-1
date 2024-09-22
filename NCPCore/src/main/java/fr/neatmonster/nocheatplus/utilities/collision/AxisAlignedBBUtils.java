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
import org.bukkit.entity.Entity;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;

/**
 * Utility methods for dealing with axis-aligned bounding boxes <br>
 * (A bounding box defined by its minimum and maximum corner coordinates in a 3D space)
 */
public class AxisAlignedBBUtils {
    
    private final static IGenericInstanceHandle<MCAccess> mcAccess = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(MCAccess.class);
    
    /**
     * Infers a new axis-aligned bounding box from the given Entity's NMS width and height parameters.
     * This method calculates the bounding box centered at its location and dimensions.
     *
     * @param entity The entity for which to calculate the AABB
     * @return A new array of doubles containing the entity's AABB coordinates in the following order:
     *         {minX, minY, minZ, maxX, maxY, maxZ}     
     */
    public static double[] createAABB(Entity entity) {
        final double halfWidth = mcAccess.getHandle().getWidth(entity) / 2f; // from the center position to add on each horizontal side. --.--
        final double height = mcAccess.getHandle().getHeight(entity);
        final Location loc = entity.getLocation();
        return new double[] {loc.getX() - halfWidth, // minX
                             loc.getY(),             // minY - feet
                             loc.getZ() - halfWidth, // minZ
                             loc.getX() + halfWidth, // maxX
                             loc.getY() + height,    // maxY - top of the box
                             loc.getZ() + halfWidth};// maxZ
    }
    
    /**
     * Infers a new axis-aligned bounding box from the given Entity's NMS width and height parameters.
     * This method calculates the bounding box centered at its location and dimensions.
     * <p>The width resolution is adjusted to 0.5 units by rounding to the nearest half unit.</p>
     *
     * @param entity The entity for which to calculate the AABB
     * @param loc The central location around which the AABB is to be created
     * @return A new array of doubles containing the entity's AABB coordinates in the following order:
     *         {minX, minY, minZ, maxX, maxY, maxZ}
     */
    public static double[] createAABBAtHorizontalResolution(Entity entity, Location loc) {
        final double widthRes = Math.round(mcAccess.getHandle().getWidth(entity) * 500.0) / 1000.0;
        final double height = mcAccess.getHandle().getHeight(entity);
        return new double[] {
                loc.getX() - widthRes, // minX
                loc.getY(),            // minY - feet
                loc.getZ() - widthRes, // minZ
                loc.getX() + widthRes, // maxX
                loc.getY() + height,   // maxY - top of the box
                loc.getZ() + widthRes  // maxZ
        };
    }
    
    /**
     * Moves am AABB by the specified translation vector.
     * <p>This method adjusts the positions of each bounding box by adding the given offsets in the X, Y, and Z directions. 
     * The input array is assumed to contain multiple AABBs, each defined by 6 consecutive values representing the 
     * minimum and maximum coordinates in the X, Y, and Z dimensions. 
     *
     * @param AABB An array of bounding boxes.
     * @param x The translation offset in the X direction.
     * @param y The translation offset in the Y direction.
     * @param z The translation offset in the Z direction.
     *
     * @return A new array of bounding boxes, each translated by the given offsets. Returns the original array if it does not 
     *         have a length that is a multiple of 6.
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
    
    /**
     * Expands an axis-aligned bounding box to ensure it includes a specified coordinate point.
     *
     * <p> If the point is outside the current bounds of the AABB, the method expands the bounding box to include this point. If the point is within the current bounds, 
     * the AABB remains unchanged. The adjustments are made only in the directions where necessary:
     * <ul>
     *   <li>If `x` is less than the current minimum X, the minimum X boundary is adjusted.</li>
     *   <li>If `x` is greater than the current maximum X, the maximum X boundary is adjusted.</li>
     *   <li>Similar adjustments are made for the Y and Z dimensions.</li>
     * </ul>
     *
     * @param AABB The AABB to expand or contract.
     * @param x The X coordinate to include in the bounding box. The bounding box will be expanded if this coordinate is outside 
     *          the current X bounds.
     * @param y The Y coordinate to include in the bounding box. The bounding box will be expanded if this coordinate is outside 
     *          the current Y bounds.
     * @param z The Z coordinate to include in the bounding box. The bounding box will be expanded if this coordinate is outside 
     *          the current Z bounds.
     *
     * @return A new bounding box where the dimensions have been adjusted to include the specified coordinate point.
     */
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
    
    /**
     * Splits a multi-bounding box array into individual bounding boxes.
     *
     * <p>The input array is expected to contain multiple bounding boxes. This method divides the input array into separate bounding boxes and 
     * collects them into a list.
     *
     * @param multiAABB An array of bounding boxes where each bounding box is represented by 6 consecutive doubles: 
     *                  minX, minY, minZ, maxX, maxY, maxZ. The length of this array must be a multiple of 6.
     *
     * @return A list of bounding boxes, where each bounding box is represented as a double array with 6 values: 
     *         minX, minY, minZ, maxX, maxY, maxZ. Returns an empty list if the input array length is not a multiple of 6.
     */
    public static List<double[]> splitIntoSingle(double[] multiAABB) {
        List<double[]> a = new ArrayList<>();
        if (multiAABB.length % 6 == 0) {
            for (int i = 1; i <= (int)multiAABB.length / 6; i++) {
                a.add(new double[] {multiAABB[i*6-6], multiAABB[i*6-5], multiAABB[i*6-4], multiAABB[i*6-3], multiAABB[i*6-2], multiAABB[i*6-1]});
            }
        }
        return a;
    }
    
    /**
     * Calculates the adjusted offset in the X direction for an axis-aligned bounding box (AABB) when colliding with another bounding box.
     *
     * <p>This method determines how much movement along the X axis is allowed before the bounding boxes intersect, given the
     * offset in the X direction. It ensures that the AABBs do not overlap in the Y and Z dimensions before computing the adjustment.
     *
     * @param AABB The bounding box that is being moved.
     * @param other The bounding box to check for collision with.
     * @param offsetX The proposed movement in the X direction.
     *
     * @return The adjusted offset in the X direction. Returns the original `offsetX` if there is no overlap in the Y and Z dimensions
     *         or if the movement is within acceptable bounds to prevent intersection.
     */
    public static double collideX(double[] AABB, double[] other, double offsetX) {
        if (offsetX != 0 && (other[1] - AABB[4]) < -CollisionUtil.COLLISION_EPSILON && (other[4] - AABB[1]) > CollisionUtil.COLLISION_EPSILON 
            && (other[2] - AABB[5]) < -CollisionUtil.COLLISION_EPSILON && (other[5] - AABB[2]) > CollisionUtil.COLLISION_EPSILON) {
            if (offsetX >= 0.0) {
                double max_move = AABB[0] - other[3];
                if (max_move < -CollisionUtil.COLLISION_EPSILON) {
                    return offsetX;
                }
                return Math.min(max_move, offsetX);
            } 
            else {
                double max_move = AABB[3] - other[0];
                if (max_move > CollisionUtil.COLLISION_EPSILON) {
                    return offsetX;
                }
                return Math.max(max_move, offsetX);
            }
        }
        return offsetX;
    }
    
    /**
     * Calculates the adjusted offset in the Y direction for an axis-aligned bounding box (AABB) when colliding with another bounding box.
     *
     * <p>This method determines how much movement along the Y axis is allowed before the bounding boxes intersect, given the
     * offset in the Y direction. It ensures that the AABBs do not overlap in the X and Z dimensions before computing the adjustment.
     *
     * @param AABB The bounding box that is being moved.
     * @param other The bounding box to check for collision with. 
     * @param offsetY The proposed movement in the Y direction.
     *
     * @return The adjusted offset in the Y direction. Returns the original `offsetY` if there is no overlap in the X and Z dimensions
     *         or if the movement is within acceptable bounds to prevent intersection.
     */
    public static double collideY(double[] AABB, double[] other, double offsetY) {
        if (offsetY != 0 && (other[0] - AABB[3]) < -CollisionUtil.COLLISION_EPSILON && (other[3] - AABB[0]) > CollisionUtil.COLLISION_EPSILON 
            && (other[2] - AABB[5]) < -CollisionUtil.COLLISION_EPSILON && (other[5] - AABB[2]) > CollisionUtil.COLLISION_EPSILON) {
            if (offsetY >= 0.0) {
                double max_move = AABB[1] - other[4];
                if (max_move < -CollisionUtil.COLLISION_EPSILON) {
                    return offsetY;
                }
                return Math.min(max_move, offsetY);
            } 
            else {
                double max_move = AABB[4] - other[1];
                if (max_move > CollisionUtil.COLLISION_EPSILON) {
                    return offsetY;
                }
                return Math.max(max_move, offsetY);
            }
        }
        return offsetY;
    }
    
    /**
     * Calculates the adjusted offset in the Z direction for an axis-aligned bounding box (AABB) when colliding with another bounding box.
     *
     * <p>This method determines how much movement along the Z axis is allowed before the bounding boxes intersect, given the
     * offset in the Z direction. It ensures that the AABBs do not overlap in the X and Y dimensions before computing the adjustment.
     *
     * @param AABB The bounding box that is being moved.
     * @param other The bounding box to check for collision with.
     * @param offsetZ The proposed movement in the Z direction.
     *
     * @return The adjusted offset in the Z direction. Returns the original `offsetZ` if there is no overlap in the X and Y dimensions
     *         or if the movement is within acceptable bounds to prevent intersection.
     */
    public static double collideZ(double[] AABB, double[] other, double offsetZ) {
        if (offsetZ != 0 && (other[0] - AABB[3]) < -CollisionUtil.COLLISION_EPSILON && (other[3] - AABB[0]) > CollisionUtil.COLLISION_EPSILON 
            && (other[1] - AABB[4]) < -CollisionUtil.COLLISION_EPSILON && (other[4] - AABB[1]) > CollisionUtil.COLLISION_EPSILON) {
            if (offsetZ >= 0.0) {
                double max_move = AABB[2] - other[5];
                if (max_move < -CollisionUtil.COLLISION_EPSILON) {
                    return offsetZ;
                }
                return Math.min(max_move, offsetZ);
            } 
            else {
                double max_move = AABB[5] - other[2];
                if (max_move > CollisionUtil.COLLISION_EPSILON) {
                    return offsetZ;
                }
                return Math.max(max_move, offsetZ);
            }
        }
        return offsetZ;
    }
    
    /**
     * Checks if a single axis-aligned bounding box (AABB) collides with any part of a block's bounding box.
     *
     * @param rawAABB An array representing the bounding box of a block. This can be a complex shape, 
     *                represented by multiple AABBs, with each AABB defined by six consecutive elements 
     *                in the array: [minX, minY, minZ, maxX, maxY, maxZ]. The length of the array must be 
     *                a multiple of 6. If a complex shape with multiple bounding boxes is passed, only the primary bounding box (first six elements) will be considered for collision detection.
     * @param x The coordinate of the block's position in the world.
     * @param y        
     * @param z        
     * @param sAABB The single AABB to check for collision. This is defined by an array of six elements: 
     *              [minX, minY, minZ, maxX, maxY, maxZ]. 
     * @param allowEdge A boolean indicating whether collisions at the edges of the AABBs should be considered 
     *                  as a valid collision. If true, an AABB touching the edge of the block's AABB will 
     *                  be considered a collision.
     * @return Returns {@code true} if the single AABB collides with any part of the block's AABB, taking into account the {@code allowEdge} flag. Otherwise, returns {@code false}.
     */
    public static boolean isCollided(final double[] rawAABB, final int x, final int y, final int z, final double[] sAABB, final boolean allowEdge) {
        if (rawAABB != null && sAABB != null && rawAABB.length % 6 == 0) {
            for (int i = 1; i <= (int)rawAABB.length / 6; i++) {

                // Clearly outside AABB.
                if (sAABB[0] > rawAABB[i*6-3] + x || sAABB[3] < rawAABB[i*6-6] + x || sAABB[1] > rawAABB[i*6-2] + y || sAABB[4] < rawAABB[i*6-5] + y
                    || sAABB[2] > rawAABB[i*6-1] + z || sAABB[5] < rawAABB[i*6-4] + z) {
                    continue;
                }
                // Hitting the max-edges (if allowed).
                if (sAABB[0] == rawAABB[i*6-3] + x && (rawAABB[i*6-3] < 1.0 || allowEdge)
                    || sAABB[1] == rawAABB[i*6-2] + y && (rawAABB[i*6-2] < 1.0 || allowEdge) 
                    || sAABB[2] == rawAABB[i*6-1] + z && (rawAABB[i*6-1] < 1.0 || allowEdge)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if the provided block bounds represent a full block, where all dimensions
     * are either 0 or 1.
     *
     * @param bounds An array representing the block bounds in the order: minX, minY, minZ, maxX, maxY, maxZ.
     * @return {@code true} if the bounds represent a full block (i.e., [0, 0, 0, 1, 1, 1]), 
     *         {@code false} otherwise. Returns {@code false} if the bounds array is {@code null}.
     */
    public static final boolean isFullBounds(final double[] bounds) {
        if (bounds == null) return false;
        for (int i = 0; i < 3; i++) {
            if (bounds[i] != 0.0 || bounds[i + 3] != 1.0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Compares two sets of block bounds to determine if they are identical.
     * This includes a null check to ensure both arrays are either non-null
     * and identical, or both are null.
     *
     * @param bounds1 The first block bounds array.
     * @param bounds2 The second block bounds array.
     * @return {@code true} if both bounds arrays are identical or both are {@code null}. 
     *         Returns {@code false} if one of the bounds is {@code null} or if the arrays 
     *         have differing values. Note: a full block in one array will not be considered
     *         equal to a {@code null} array.
     */
    public static final boolean isSameShape(final double[] bounds1, final double[] bounds2) {
        if (bounds1 == null || bounds2 == null) {
            return bounds1 == bounds2;
        }
        for (int i = 0; i < 6; i++) {
            if (bounds1[i] != bounds2[i]) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Checks if the given point(specified by its x, y, z coordinates) lies within or on the edges of the AABB.
     *
     * @param x Position of the point.
     * @param y
     * @param z
     * @param minX Minimum coordinates of the AABB.
     * @param minY
     * @param minZ
     * @param maxX Maximum coordinates of the AABB.
     * @param maxY
     * @param maxZ
     * @return {@code true} if the point lies inside or on the edges of the AABB; {@code false} otherwise.
     */
    public static boolean isInsideAABBIncludeEdges(final double x, final double y, final double z,
                                                   final double minX, final double minY, final double minZ,
                                                   final double maxX, final double maxY, final double maxZ) {
        return !(x < minX || x > maxX || z < minZ || z > maxZ || y < minY || y > maxY);
    }
    
    /**
     * Checks if the given point (specified by its x, y, z coordinates) lies within or on the edges of the AABB.
     *
     * @param x Position of the point.
     * @param y        
     * @param z     
     * @param AABB The axis-aligned bounding box represented as a double array with 6 elements:
     *             [minX, minY, minZ, maxX, maxY, maxZ].
     * @return {@code true} if the point lies inside or on the edges of the AABB; {@code false} otherwise.
     * @throws IllegalArgumentException if the bounds array does not contain exactly 6 elements.
     */
    public static boolean isInsideAABBIncludeEdges(final double x, final double y, final double z, final double[] AABB) {
        if (AABB == null || AABB.length != 6) {
            throw new IllegalArgumentException("Bounds array must contain exactly 6 elements.");
        }
        final double minX = AABB[0];
        final double minY = AABB[1];
        final double minZ = AABB[2];
        final double maxX = AABB[3];
        final double maxY = AABB[4];
        final double maxZ = AABB[5];
        return isInsideAABBIncludeEdges(x, y, z, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
