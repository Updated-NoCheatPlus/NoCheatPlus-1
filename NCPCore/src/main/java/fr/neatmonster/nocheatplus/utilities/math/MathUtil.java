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
     * Maximum of the absolute value of two numbers. NMS style.
     * 
     * @param d0
     * @param d1
     * @return Maximum absolute value between the two inputs.
     */
    public static double absMax(double d0, double d1) {
        if (d0 < 0.0D) {
            d0 = -d0;
        }
        if (d1 < 0.0D) {
            d1 = -d1;
        }
        return Math.max(d0, d1);
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
     * Convenience method
     * 
     * @param x 
     * @param y
     * @param z
     * @return Normalized vector
     */
    public static double[] normalize(double x, double y, double z) {
       double distanceSq = square(x) + square(y) + square(z);
       double magnitude = Math.sqrt(distanceSq);
       return new double[] {x / magnitude, y / magnitude, z / magnitude};
    }
    
    
    /**
     * "Round" a double.
     * Not for the most precise results.
     * Use for smaller values only.
     * 
     * @param value
     * @param decimalPlaces 
     * @return The rounded double
     * @throws IllegalArgumentException if decimal places are negative
     */
    public static double round(double value, int decimalPlaces) {
        if (decimalPlaces < 0) {
          	throw new IllegalArgumentException("Decimal places cannot be negative.");
        }
        long factor = (long) Math.pow(10, decimalPlaces);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
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
     * Calculate the standard deviation of this data.
     * https://en.wikipedia.org/wiki/Standard_deviation
     * 
     * @param data
     * @return the std dev
     */
    public static double stdDev(double[] data) {
        double variance = 0.0;
        for (double num : data) {
            variance += Math.pow(num - mean(data), 2);
        }
        return Math.sqrt(variance / data.length);
    }
    
    /**
     * Calculate the mean of this array
     * 
     * @param values
     * @return the mean
     */
    public static double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        double mean = sum / values.length;
        return mean;
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

    public static boolean equal(float var0, float var1) {
      return Math.abs(var1 - var0) < 1.0E-5F;
   }

   public static boolean equal(double var0, double var2) {
      return Math.abs(var2 - var0) < 9.999999747378752E-6D;
   }
}