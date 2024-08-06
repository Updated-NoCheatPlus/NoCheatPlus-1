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

/**
 * Auxiliary static methods for dealing with mathematical operations.
 */
public class MathUtil {
   
    /**
     * Clamp double between a maximum and minimum value.
     * 
     * @param inputValue The value to clamp.
     * @param minParameter Exclusive
     * @param maxParameter Exclusive
     * @return the clamped value
     */
    public static double clamp(double inputValue, double minParameter, double maxParameter) {
       return inputValue < minParameter ? minParameter : (inputValue > maxParameter ? maxParameter : inputValue);
    }
    
    /**
     * Clamp float between a maximum and minimum value.
     * 
     * @param inputValue The value to clamp.
     * @param minParameter Exclusive
     * @param maxParameter Exclusive
     * @return the clamped value
     */
    public static float clamp(float inputValue, float minParameter, float maxParameter) {
       return inputValue < minParameter ? minParameter : (inputValue > maxParameter ? maxParameter : inputValue);
    }
    
    /**
     * Clamp int between a maximum and minimum value.
     * 
     * @param inputValue The value to clamp.
     * @param minParameter Exclusive
     * @param maxParameter Exclusive
     * @return the clamped value
     */
    public static int clamp(int inputValue, int minParameter, int maxParameter) {
       return inputValue < minParameter ? minParameter : (inputValue > maxParameter ? maxParameter : inputValue);
    }
    
    /**
     * Test if the input value is between a minimum and maximum threshold
     * 
     * @param minThreshold Exclusive
     * @param inputValue The value to test
     * @param maxThreshold Exclusive
     * @return True if the value is between the thresholds, false otherwise
     */
    public static boolean between(double minThreshold, double inputValue, double maxThreshold) {
       return inputValue > minThreshold && inputValue < maxThreshold;
    }
      
    /**
     * Test if the input value is between a minimum and maximum threshold
     * 
     * @param minThreshold Inclusive
     * @param inputValue The value to test
     * @param maxThreshold Inclusive
     * @return True if the value is between or equal the thresholds, false otherwise
     */
    public static boolean inRange(double minThreshold, double inputValue, double maxThreshold) {
       return inputValue >= minThreshold && inputValue <= maxThreshold;
    }
        
    /**
     * Test if the absolute difference between two values is small enough to be considered equal.
     * 
     * @param a The minuend
     * @param b The subtrahend
     * @param c Absolute(!) value to compare the difference with
     * @return True if the absolute difference is smaller or equals C.
     *         Returns false for negative C inputs.
     */
    public static boolean equal(double a, double b, double c) {
       if (c < 0.0) return false;
       return Math.abs(a-b) <= c;
    }
       
    /**
     * Convenience method to calculate horizontal distance
     * 
     * @param xDistance
     * @param zDistance
     * @return the 2d distance
     */
    public static double dist(double xDistance, double zDistance) {
       return Math.sqrt(square(xDistance) + square(zDistance));
    }
    
    /**
     * Convenience method
     * 
     * @param value the value to square.
     * @return squared value.
     */
    public static double square(double value) {
       return value * value;
    }
    
    /**
     * Maximum value of three numbers
     * 
     * @param a
     * @param b
     * @param c
     * @return The highest number
     */
    public static double max3(double a, double b, double c) {
       return Math.max(a, Math.max(b, c));
    }
    
    /**
     * Absolute non-zero value of the input number.
     * 
     * @param b
     * @return The first non-zero value.
     * @throws IllegalArgumentException if input is 0
     */
    public static double absNonZero(double input) {
       if (input > 0.0 || input < 0.0) {
          return Math.abs(input);
       } 
       else throw new IllegalArgumentException("Input cannot be 0.");
    }
     
     /**
      * Test if the difference between two values is small enough to be considered equal
      * 
      * @param a
      * @param b
      * @param c The difference (not inclusive)
      * @return true, if close enough.
      */
     public static boolean almostEqual(double a, double b, double c){
      return Math.abs(a-b) < c;
     }
    
    /**
     * Flooring method from NMS
     * 
     * @param var0
     * @return floored double 
     */
    public static int floor(double value) {
       int toInt = (int)value;
       return value < (double)toInt ? toInt - 1 : toInt;
    }

    /**
     * Ceiling method from NMS
     * 
     * @param var0
     * @return floored double
     */
    public static int ceil(double value) {
        int toInt = (int)value;
        return value > (double)toInt ? toInt + 1 : toInt;
    }
    
    /**
     * Square root method from NMS
     * 
     * @param var0
     * @return floored double
     */
    public static float sqrt(float f) {
        return (float) Math.sqrt((double) f);
    }

    /**
     * Given an array of doubles, get the index of the array slot containing the closest value to the target value.
     * 
     * @param arr
     * @return the index.
     */
    public static int closestIndex(double[] arr, double target) {
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
    public static double getFilledSpace(double sA, double eA, double sB, double eB) {
        return (eA - sA) + (eB - sB) - Math.max(0, Math.min(eA, eB) - Math.max(sA, sB));
    }
    
    /**
     * Checks if one range is completely contained within another range, or if the two ranges completely overlap.
     * <p>This method determines whether Range L (`[lBMin, lBMax]`) is fully within Range N (`[nBMin, nBMax]`),
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
}