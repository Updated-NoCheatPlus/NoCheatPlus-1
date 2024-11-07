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
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.utilities.Validate;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;

/**
 * Utility methods for dealing with axis-aligned bounding boxes <br>
 * (A bounding box defined by its minimum and maximum corner coordinates in a 3D space)
 */
public class AxisAlignedBBUtils {
    
    /*
      TODO: Perhaps, instead of working with doubles (which has the disadvantage of having to remember each index for each coordinate (AABB[0-1-2-3-4-5]), we could make an instance class of some sort (AxisAlignedBB.java / CollisionBox.java / BoundingBox.java (...) (minXYZ,maxXYZ)
      Similar to Bukkit's BoundingBox.java 
     */
    private final static IGenericInstanceHandle<MCAccess> mcAccess = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(MCAccess.class);
    
    /**
     * Infers how many AABBs are supposed to be present in the given double array (represented as 6 consecutive doubles in the following order:
     * {minX, minY, minZ, maxX, maxY, maxZ}). <br>
     * To know how many AABBs are contained in a given array, you can divide its length by 6 (array.length / 6).
     * The length must therefore be a multiple of 6. <br>
     * 
     * @example 
     * <li>If the array contains 2 AABBs, they'll be represented as 12 consecutive doubles, thus -> 12 / 6 = 2. </li>
     * <li>If 3 AABBs are contained, 18 doubles -> 18 / 6 = 3. And so on...</li>
     *      
     * @param array The array to inspect. It is expected to contain multiple bounding boxes (more than 6 elements), but can also contain a single one.
     * 
     * @throws IllegalArgumentException if the given array's length isn't a multiple of 6, null or is empty.
     * 
     * @return An int indicating how many AABBs are supposed to be represented in the array.
     */
    public static int getNumberOfAABBs(double[] array) {
        Validate.validateAABB(array);
        return (int)array.length / 6;
    }
    
    /**
     * Converts a BoundingBox object (Bukkit) into a double array representing its min/max coordinates.
     *
     * @param box The BoundingBox to be converted.
     * @return A double array containing the minX, minY, minZ, maxX, maxY, maxZ coords of the bounding box.
     */
    public static double[] toArray(BoundingBox box) {
        return new double[] {
                box.getMinX(), box.getMinY(), box.getMinZ(), 
                box.getMaxX(), box.getMaxY(), box.getMaxZ()
        };
    }
    
    /**
     * Extracts the individual bounding box from a multi-bounding box double array, based on the given index (boxNumber).
     * Currently, this is rather meant to be used if you already know in advance how many AABBs are present in the array and which one you need.<br>
     * Otherwise, you should call {@link #splitIntoSingle(double[])} to get a list of individual bounding boxes.
     *
     * <p>The input array is expected to contain multiple bounding boxes, each represented by 6 consecutive doubles: 
     * minX, minY, minZ, maxX, maxY, maxZ. The length must therefore be a multiple of 6.
     *
     * @example
     * Given a multiAABB array representing two bounding boxes:
     * <pre>
     * double[] multiAABB = {0.0, 0.0, 0.0, 1.0, 1.0, 1.0,  // AABB 1
     *                       1.0, 1.0, 1.0, 2.0, 2.0, 2.0}; // AABB 2
     *
     * double[] AABB_1st = extractBoundingBox(multiAABB, 1); // Returns {0.0, 0.0, 0.0, 1.0, 1.0, 1.0}
     * double[] AABB_2nd = extractBoundingBox(multiAABB, 2); // Returns {1.0, 1.0, 1.0, 2.0, 2.0, 2.0}
     * </pre>
     * 
     * @param multiAABB An array containing multiple bounding boxes. If the array contains only a single bounding box, the method will just return it.
     * @param boxNumber The 1-based index of the bounding box to retrieve (starting from 1).
     *
     * @throws IllegalArgumentException If the multiAABB length is not a multiple of 6, or if boxNumber is out of bounds.
     *
     * @return A double array representing the individual bounding box.
     */
    public static double[] extractBoundingBox(double[] multiAABB, int boxNumber) {
        int numberOfAABBs = getNumberOfAABBs(multiAABB);
        if (boxNumber < 1 || boxNumber > numberOfAABBs) {
            throw new IllegalArgumentException("boxNumber is out of bounds. Valid indices are between 1 and " + numberOfAABBs);
        }
        if (numberOfAABBs == 1) {
            return multiAABB;
        }
        // Calculate the starting position for the requested bounding box
        int start = (boxNumber - 1) * 6;
        // Extract the individual bounding box
        return new double[] {
                multiAABB[start],//0  // minX
                multiAABB[start + 1], // minY
                multiAABB[start + 2], // minZ
                multiAABB[start + 3], // maxX
                multiAABB[start + 4], // maxY
                multiAABB[start + 5]  // maxZ
        };
    }
    
    /**
     * Splits a multi-bounding box array into individual bounding boxes and collects them into a list.
     *
     * @param multiAABB An array of bounding boxes where each box is represented by 6 consecutive doubles: 
     *                  minX, minY, minZ, maxX, maxY, maxZ. The length of this array must be a multiple of 6.
     *
     * @throws IllegalArgumentException If multiAABB length is not a multiple of 6.
     *
     * @return The bounding boxes List.
     */
    public static List<double[]> splitIntoSingle(double[] multiAABB) {
        // If there is only one bounding box, return it in a single-item list
        int numberOfAABBs = getNumberOfAABBs(multiAABB);
        if (numberOfAABBs == 1) {
            // Minor performance gain: avoid creating a list if the passed array only contains a single AABB.
            // (Also makes the method more semantically correct).
            return Collections.singletonList(multiAABB);
        }
        List<double[]> list = new ArrayList<>();
        for (int i = 1; i <= numberOfAABBs; i++) {
            list.add(extractBoundingBox(multiAABB, i));
        }
        return list;
    }
    
    /**
     * Infers a new axis-aligned bounding box from the given Entity's width and height.
     * This method calculates the bounding box centered at its location and dimensions.
     *
     * @param entity The entity for which to calculate the AABB.
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
     * Infers a new axis-aligned bounding box from the given Entity's width and height.
     * This method calculates the bounding box centered at its location and dimensions.
     * <p>The width resolution is adjusted to 0.5 units by rounding to the nearest half unit.</p>
     *
     * @param entity The entity for which to calculate the AABB.
     * @param loc The central location around which the AABB is to be created
     * @return A new array of doubles containing the entity's AABB coordinates in the following order:
     *         {minX, minY, minZ, maxX, maxY, maxZ}
     */
    public static double[] createAABBAtWidthResolution(Entity entity, Location loc) {
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
     * Offsets an AABB by the specified coordinate points.
     * <p>The method supports multi-bounding box double arrays. If a multi-AABB is passed, each bounding box in the array will be offset.</p>
     *
     * @param AABB A double array containing a single or multiple bounding boxes.
     * @param x The offsets in the x, y and z dimensions...
     * @param y 
     * @param z 
     * 
     * @throws IllegalArgumentException If the array length is not a multiple of 6.
     *
     * @return A new array of bounding boxes, each translated by the given offsets.<br> 
     *     
     */
    public static double[] move(double[] AABB, double x, double y, double z) {
        double[] tAABB = AABB.clone();
        // The loop starts at i = 1 and runs until i is equal to the number of AABBs in the array. (i.e.: if there are 2 AABB in the array, they'll be represented as 12 doubles -> 12 / 6 = 2, that why the array length must be a multiple of 6)
        for (int i = 1; i <= getNumberOfAABBs(AABB); i++) {
            // Indices are calculated using [i * 6 - X] expressions, where X ensures you target the specific coordinates for each AABB.
            tAABB[i*6-6] += x; // i.e.: For the first AABB in the array 1 * 6 - 6 = 0: This is the index for minX coordinate of the first AABB and so on...
            tAABB[i*6-3] += x;
            tAABB[i*6-5] += y;
            tAABB[i*6-2] += y;
            tAABB[i*6-4] += z;
            tAABB[i*6-1] += z;
        }
        return tAABB;
    }
    
    /**
     * Expands or contracts an axis-aligned bounding box to ensure it includes the given point (specified by its x, y, z coordinates).
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
    public static double[] expandTowards(double[] AABB, double x, double y, double z) {
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
     * Calculates the adjusted offset in the X direction for an axis-aligned bounding box when colliding with another bounding box.
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
     * Calculates the adjusted offset in the Y direction for an axis-aligned bounding box when colliding with another bounding box.
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
     * Calculates the adjusted offset in the Z direction for an axis-aligned bounding box when colliding with another bounding box.
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
     * Checks whether two axis-aligned bounding boxes intersect.
     * An epsilon value is used to ensure precise comparisons between the AABBs' boundaries.
     *
     * @param aabb1 The first AABB, represented by a double array [minX, minY, minZ, maxX, maxY, maxZ].
     * @param aabb2 The second AABB, represented by a double array [minX, minY, minZ, maxX, maxY, maxZ].
     * @return True if the two AABBs intersect, false otherwise.
     */
    public static boolean isIntersected(double[] aabb1, double[] aabb2) {
        return aabb2[3] - CollisionUtil.COLLISION_EPSILON > aabb1[0] && aabb2[0] + CollisionUtil.COLLISION_EPSILON < aabb1[3] // x
                && aabb2[4] - CollisionUtil.COLLISION_EPSILON > aabb1[1] && aabb2[1] + CollisionUtil.COLLISION_EPSILON < aabb1[4] // y
                && aabb2[5] - CollisionUtil.COLLISION_EPSILON > aabb1[2] && aabb2[2] + CollisionUtil.COLLISION_EPSILON < aabb1[5]; // z
    }
    
    /**
     * Checks if a single axis-aligned bounding box (sAABB) collides with any part of a block's bounding box.
     *
     * @param blockAABB A double array representing the bounding box of a block. This can be a complex shape, 
     *                meaning the array can contain multiple bounding boxes each defined by six <i>consecutive</i> elements 
     *                [minX, minY, minZ, maxX, maxY, maxZ / minX_1, minY_1 (...)], therefore, the length of the array must be a multiple of 6.<br>
     * @param x The coordinate of the block's position in the world.
     * @param y
     * @param z
     * @param sAABB The single double array containing the AABB to check for collision. <br>
     *              If an array with multiple AABBs is passed, only the primary bounding box (first six doubles) will be considered for collision detection.
     * @param allowEdge A boolean indicating whether collisions at the edges of the AABBs should be considered 
     *                  as a valid collision. If true, an AABB touching the edge of the block's AABB will 
     *                  be considered a collision.
     * @param startIndex The index (starting from 1) of the AABB in `blockAABB` from which to start checking for collisions.<br> Cannot be less than 1.
     *
     * @throws IllegalArgumentException If rawAABB length is not a multiple of 6 or is null, or the startIndex parameter is less than 1.
     *
     * @return Returns {@code true} if the single AABB collides with any part of the block's AABB, taking into account the {@code allowEdge} flag. Otherwise, returns {@code false}.
     */
    public static boolean isCollided(final double[] blockAABB, final int x, final int y, final int z, final double[] sAABB, final boolean allowEdge, int startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("startIndex cannot be less than 1. Index: " + startIndex);
        }
        if (blockAABB != null && sAABB != null) {
            for (int i = startIndex; i <= getNumberOfAABBs(blockAABB); i++) {
                if (sAABB[0] > blockAABB[i*6-3] + x 
                    || sAABB[3] < blockAABB[i*6-6] + x 
                    || sAABB[1] > blockAABB[i*6-2] + y 
                    || sAABB[4] < blockAABB[i*6-5] + y 
                    || sAABB[2] > blockAABB[i*6-1] + z 
                    || sAABB[5] < blockAABB[i*6-4] + z) {
                    // Outside AABB
                    continue;
                }
                // Hitting the max-edges (if allowed).
                if (sAABB[0] == blockAABB[i*6-3] + x && (blockAABB[i*6-3] < 1.0 || allowEdge) 
                    || sAABB[1] == blockAABB[i*6-2] + y && (blockAABB[i*6-2] < 1.0 || allowEdge) 
                    || sAABB[2] == blockAABB[i*6-1] + z && (blockAABB[i*6-1] < 1.0 || allowEdge)) {
                    // Outside AABB
                    continue;
                }
                // Collision.
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a single axis-aligned bounding box (sAABB) collides with any part of a block's bounding box.
     *
     * @param blockAABB A double array representing the bounding box of a block. This can be a complex shape, 
     *                meaning the array can contain multiple bounding boxes each defined by six <i>consecutive</i> elements 
     *                [minX, minY, minZ, maxX, maxY, maxZ / minX_1, minY_1 (...)], therefore, the length of the array must be a multiple of 6.<br>
     * @param x The coordinate of the block's position in the world.
     * @param y
     * @param z
     * @param sAABB The single double array containing the AABB to check for collision. <br>
     *              If an array with multiple AABBs is passed, only the primary bounding box (first six doubles) will be considered for collision detection.
     * @param allowEdge A boolean indicating whether collisions at the edges of the AABBs should be considered 
     *                  as a valid collision. If true, an AABB touching the edge of the block's AABB will 
     *                  be considered a collision.
     *
     * @throws IllegalArgumentException If rawAABB length is not a multiple of 6.
     *
     * @return Returns {@code true} if the single AABB collides with any part of the block's AABB, taking into account the {@code allowEdge} flag. Otherwise, returns {@code false}.
     */
    public static boolean isCollided(final double[] blockAABB, final int x, final int y, final int z, final double[] sAABB, final boolean allowEdge) {
        return isCollided(blockAABB, x, y, z, sAABB, allowEdge, 1);
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
     * Checks if the given AABB is degenerate, meaning at least one of its corner (min/max/XZ) is 0.0.<br>
     * Does not support multi-bounding box arrays. If a multi AABB is passed, only the primary box will be checked.
     * 
     * @param AABB The AABB to check
     * @return True, if at least one coordinate returns 0.0, false otherwise.
     */
    public static boolean isDegenerate(final double[] AABB) {
        Validate.validateAABB(AABB);
        return AABB[0] == 0.0 || AABB[2] == 0.0 || AABB[3] == 0.0 || AABB[5] == 0.0;
    }
    
    /**
     * Checks if the given array bounds is all 0.
     * 
     * @param bounds
     * @return
     */
    public static boolean isZero(final double[] bounds) {
        Validate.validateAABB(bounds);
        // Check if all elements are 0
        for (double bound : bounds) {
            if (bound != 0.0) {
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
     * Checks if the given point(specified by its x, y, z coordinates) lies within or on the edges of the AABB.<br>
     * (No validation of the AABB is performed)
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
     * @param x Position of the point...
     * @param y        
     * @param z     
     * @param AABB The axis-aligned bounding box represented as a double array with 6 elements:
     *             [minX, minY, minZ, maxX, maxY, maxZ].
     * @return {@code true} if the point lies inside or on the edges of the AABB; {@code false} otherwise.
     * @throws IllegalArgumentException if the bounds array does not contain exactly 6 elements.
     */
    public static boolean isInsideAABBIncludeEdges(final double x, final double y, final double z, final double[] AABB) {
        Validate.validateAABB(AABB);
        final double minX = AABB[0];
        final double minY = AABB[1];
        final double minZ = AABB[2];
        final double maxX = AABB[3];
        final double maxY = AABB[4];
        final double maxZ = AABB[5];
        return isInsideAABBIncludeEdges(x, y, z, minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    /**
     * Returns a list of Y-axis point positions within the bounding box.
     * The points are generated based on the precision determined by the 
     * bounding box dimensions.
     *
     * @param AABB The AABB on which the points are determined.
     * @return a list of Y-axis positions, either min and max Y values or 
     *         evenly spaced points between them.
     */
    public static List<Double> getYPointPositions(double[] AABB) {
        return getYPointPositions(AABB[0], AABB[1], AABB[2], AABB[3], AABB[4], AABB[5]);
    }
    
    /**
     * From {@code VoxelShape.java}.<br>
     * Generates a list of Y-axis positions based on the given AABB coordinates.
     * If the AABB is sufficiently large, it subdivides the Y-axis into evenly 
     * spaced points. If the bounding box is very small, it returns an empty list.
     *
     * @param minX Coordinates of the AABB...
     * @param minY 
     * @param minZ
     * @param maxX 
     * @param maxY 
     * @param maxZ 
     * @return A List of Y-axis points, either evenly spaced or just the min and max Y values
     */
    private static List<Double> getYPointPositions(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!(maxX - minX < CollisionUtil.COLLISION_EPSILON) && !(maxY - minY < CollisionUtil.COLLISION_EPSILON) && !(maxZ - minZ < CollisionUtil.COLLISION_EPSILON)) {
            int xBitPrecision = MathUtil.findBits(minX, maxX);
            int yBitPrecision = MathUtil.findBits(minY, maxY);
            int zBitPrecision = MathUtil.findBits(minZ, maxZ);
            
            if (xBitPrecision < 0 || yBitPrecision < 0 || zBitPrecision < 0) {
                // Return a list containing minY and maxY when bits cannot be determined
                List<Double> result = new ArrayList<>();
                result.add(minY);
                result.add(maxY);
                return result;
            }
            else if (xBitPrecision == 0 && yBitPrecision == 0 && zBitPrecision == 0) {
                // Return a list with 0 - 1 if no subdivisions are needed
                List<Double> result = new ArrayList<>();
                result.add(0.0);
                result.add(1.0);
                return result;
            }
            else {
                // Calculate the number of subdivisions based on Y precision
                int subdivisions = 1 << yBitPrecision;
                List<Double> result = new ArrayList<>();
                // Generate the list of evenly spaced Y points
                for (int index = 0; index <= subdivisions; index++) {
                    result.add((double) index / (double) subdivisions);
                }
                return result;
            }
        } 
        else {
            // Return an empty list if the bounding box is (way) too small
            return new ArrayList<>();
        }
    }
}
