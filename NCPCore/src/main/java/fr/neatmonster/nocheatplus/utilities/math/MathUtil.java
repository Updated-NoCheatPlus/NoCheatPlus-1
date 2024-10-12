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
package fr.neatmonster.nocheatplus.utilities.math;

import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;

/**
 * Auxiliary static methods for dealing with mathematical operations.
 */
public class MathUtil {
    
    /**
     * Clamps a double value between a specified minimum and maximum value.
     *
     * @param inputValue The value to clamp.
     * @param minParameter The minimum allowable value (exclusive).
     * @param maxParameter The maximum allowable value (exclusive).
     * @return The clamped value.
     */
    public static double clamp(double inputValue, double minParameter, double maxParameter) {
        return inputValue < minParameter ? minParameter : (Math.min(inputValue, maxParameter));
    }
    
    /**
     * Clamps a float value between a specified minimum and maximum value.
     *
     * @param inputValue The value to clamp.
     * @param minParameter The minimum allowable value (exclusive).
     * @param maxParameter The maximum allowable value (exclusive).
     * @return The clamped value.
     */
    public static float clamp(float inputValue, float minParameter, float maxParameter) {
        return inputValue < minParameter ? minParameter : (Math.min(inputValue, maxParameter));
    }
    
    /**
     * Checks if a value lies strictly between a specified minimum and maximum threshold.
     *
     * @param minThreshold The minimum threshold (exclusive).
     * @param inputValue The value to check.
     * @param maxThreshold The maximum threshold (exclusive).
     * @return {@code true} if the value is between the thresholds, {@code false} otherwise.
     */
    public static boolean between(double minThreshold, double inputValue, double maxThreshold) {
        return inputValue > minThreshold && inputValue < maxThreshold;
    }
    
    /**
     * Checks if a value lies within or on the bounds of a specified range.
     *
     * @param minThreshold The minimum threshold (inclusive).
     * @param inputValue The value to check.
     * @param maxThreshold The maximum threshold (inclusive).
     * @return {@code true} if the value is within or equal to the thresholds, {@code false} otherwise.
     */
    public static boolean inRange(double minThreshold, double inputValue, double maxThreshold) {
        return inputValue >= minThreshold && inputValue <= maxThreshold;
    }
    
    /**
     * Determines if the absolute difference between two values is within a specified tolerance.
     *
     * @param a The first value.
     * @param b The second value.
     * @param c The tolerance (absolute value).
     * @return {@code true} if the absolute difference is less than or equal to the tolerance, {@code false} if the tolerance is negative.
     */
    public static boolean equal(double a, double b, double c) {
        if (c < 0.0) return false;
        return Math.abs(a - b) <= c;
    }
    
    /**
     * Computes the horizontal distance between two points.
     *
     * @param xDistance The distance along the x-axis.
     * @param zDistance The distance along the z-axis.
     * @return The 2D distance.
     */
    public static double dist(double xDistance, double zDistance) {
        return Math.sqrt(square(xDistance) + square(zDistance));
    }
    
    /**
     * Computes the square of a value.
     *
     * @param value The value to square.
     * @return The squared value.
     */
    public static double square(double value) {
        return value * value;
    }
    
    /**
     * Returns the maximum of three double values.
     *
     * @param a The first value.
     * @param b The second value.
     * @param c The third value.
     * @return The highest value.
     */
    public static double max3(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }
    
    /**
     * Checks if the difference between two values is within a specified tolerance.
     *
     * @param a The first value.
     * @param b The second value.
     * @param c The tolerance (exclusive).
     * @return {@code true} if the difference is less than the tolerance, {@code false} otherwise.
     */
    public static boolean almostEqual(double a, double b, double c) {
        return Math.abs(a - b) < c;
    }
    
    /**
     * Returns the largest integer less than or equal to the specified double value.
     *
     * @param value The value to floor.
     * @return The largest integer less than or equal to the value.
     */
    public static int floor(double value) {
        int toInt = (int) value;
        return value < toInt ? toInt - 1 : toInt;
    }
    
    /**
     * Returns the smallest integer greater than or equal to the specified double value.
     *
     * @param value The value to ceil.
     * @return The smallest integer greater than or equal to the value.
     */
    public static int ceil(double value) {
        int toInt = (int) value;
        return value > toInt ? toInt + 1 : toInt;
    }
    
    /**
     * Computes the square root of a float value.
     *
     * @param f The value to compute the square root of.
     * @return The square root of the value.
     */
    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    /**
     * Given an array of doubles, get the index of the array slot containing the closest value to the target value.
     * 
     * @param arr
     * @return the index.
     */
    public static int findClosestIndex(double[] arr, double target) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty or null.");
        }
        double lastMinOffset = Double.POSITIVE_INFINITY;
        int closestIdx = -1;
        for (int i = 0; i < arr.length; i++) {
            double thisOffSet = Math.abs(arr[i] - target);
            if (thisOffSet < lastMinOffset) {
                // Update closestIdx whenever a smaller offset is encountered (meaning, a closer value to target was found).
                lastMinOffset = thisOffSet;
                closestIdx = i;
            }
        }
        return closestIdx;
    }
    
    /**
     * Seconds to ms.
     *
     * @param s1
     *            the s1
     * @return the long[]
     */
    public static long[] secToMs(final double s1) {
        final long v = (long) (s1 * 1000d);
        return new long[]{v, v, v, v, v, v, v};
    }
    
    /**
     * Milliseconds to seconds
     * 
     * @return converted time.
     */
    public static double toSeconds(final long milliseconds) {
        return (double) milliseconds / 1000D;
    }

    /**
     * Calculates the total length of space covered("filled") by two line segments on a number line,
     * accounting for any overlap between them.
     * <p>This method computes the length of two segments and then subtracts the length
     * of their overlapping region to avoid counting it twice. The result is the total
     * length of the space that is covered by at least one of the segments.</p>
     *
     * @param sA The start of the first segment.
     * @param eA The end of the first segment.
     * @param sB The start of the second segment.
     * @param eB The end of the second segment.
     * @return The total length of the space covered by both segments, considering overlap.
     *
     * @example
     * For segments [1, 5] and [3, 7]:
     * <pre>
     *   Length of the first segment = 5 - 1 = 4
     *   Length of the second segment = 7 - 3 = 4
     *   Overlap length = min(5, 7) - max(1, 3) = 5 - 3 = 2
     *   Total covered length = 4 + 4 - 2 = 6
     * </pre>
     */
    public static double getCoveredSpace(double sA, double eA, double sB, double eB) {
        return (eA - sA) + (eB - sB) - Math.max(0, Math.min(eA, eB) - Math.max(sA, sB));
    }
    
    /**
     * <p>Determines whether Range L (`[lBMin, lBMax]`) is fully within Range N (`[nBMin, nBMax]`),
     * or whether Range N is fully within Range L.</p>
     *
     * @param lBMin The starting point of the first range (Range L).
     * @param nBMin The starting point of the second range (Range N).
     * @param lBMax The ending point of the first range (Range L).
     * @param nBMax The ending point of the second range (Range N).
     * @return {@code true} if Range L is within Range N, or Range N is within Range L; {@code false} otherwise.
     *
     * @example
     * For ranges [2, 5] and [1, 6]:
     * <pre>
     *   2 &gt;= 1 and 5 &lt;= 6 → true
     * </pre>
     * For ranges [1, 6] and [2, 5]:
     * <pre>
     *   2 &gt;= 1 and 5 &lt;= 6 → true
     * </pre>
     * For ranges [1, 5] and [4, 7]:
     * <pre>
     *   4 &gt;= 1 and 5 &lt;= 7 → false
     *   1 &gt;= 4 and 7 &lt;= 5 → false
     * </pre>
     */
    public static boolean rangeContains(double lBMin, double nBMin, double lBMax, double nBMax) {
        // Check if Range L is fully within Range N
        boolean isLWithinN = nBMin <= lBMin && lBMax <= nBMax;
        
        // Check if Range N is fully within Range L
        boolean isNWithinL = lBMin <= nBMin && nBMax <= lBMax;
        
        // Return true if either range is fully contained within the other
        return isLWithinN || isNWithinL;
    }
    
    /**
     * Linearly interpolates between two floats based on a given factor.
     *
     * <p>A factor of 0.0 returns {@code startValue}, while a factor of 1.0 returns {@code endValue}. Factors between 0.0 and 1.0
     * return values proportionally between the start and end values.</p>
     *
     * @param factor The interpolation factor, typically between 0.0 and 1.0.
     * @param startValue The starting value.
     * @param endValue The ending value.
     * @return The interpolated value.
     */
    public static float lerp(float factor, float startValue, float endValue) {
        return startValue + factor * (endValue - startValue);
    }
    
    /**
     * Linearly interpolates between two doubles based on a given factor.
     *
     * <p>A factor of 0.0 returns {@code startValue}, while a factor of 1.0 returns {@code endValue}. Factors between 0.0 and 1.0
     * return values proportionally between the start and end values.</p>
     *
     * @param factor The interpolation factor, typically between 0.0 and 1.0.
     * @param startValue The starting value.
     * @param endValue The ending value.
     * @return The interpolated value.
     */
    public static double lerp(double factor, double startValue, double endValue) {
        return startValue + factor * (endValue - startValue);
    }
    
    /**
     * Returns a normalized version of the given vector, ensuring that no NaN values 
     * are produced. If the length of the vector is less than 1.0E-4, 
     * an empty vector (0, 0, 0) is returned instead of attempting normalization.
     *
     * @param vector The input vector to be normalized
     * @return A normalized version of the input vector, or a zero vector if the 
     *         length of the input vector is smaller than 1.0E-4.
     */
    public static Vector normalizedVectorWithoutNaN(Vector vector) {
        double var0 = vector.length();
        return var0 < 1.0E-4 ? new Vector() : vector.multiply(1 / var0);
    }
    
    /**
     * From {@code VoxelShape.java}.<br>
     * Determines the bit precision needed to accurately subdivide the given range (min to max).
     * It scales the range to a power of two, and checks how many bits can be used to represent it.
     *
     * @param min The minimum value in the range
     * @param max The maximum value in the range
     * @return The bit precision as an integer (0 to 3), or -1 if the range cannot be subdivided
     */
    public static int findBits(double min, double max) {
        if (!(min < -CollisionUtil.COLLISION_EPSILON) && !(max > 1.0000001)) {
            for (int bitLevel = 0; bitLevel <= 3; bitLevel++) {
                // Calculate the scaling factor as a power of two based on the bit level
                int scaleFactor = 1 << bitLevel;
                double scaledMin = min * (double) scaleFactor;
                double scaledMax = max * (double) scaleFactor;
                // Check if the scaled values are sufficiently close to integers, within an epsilon margin
                boolean isMinCloseToInteger = Math.abs(scaledMin - (double) Math.round(scaledMin)) < CollisionUtil.COLLISION_EPSILON * (double) scaleFactor;
                boolean isMaxCloseToInteger = Math.abs(scaledMax - (double) Math.round(scaledMax)) < CollisionUtil.COLLISION_EPSILON * (double) scaleFactor;
                if (isMinCloseToInteger && isMaxCloseToInteger) {
                    return bitLevel;
                }
            }
        }
        return -1;
    }
}