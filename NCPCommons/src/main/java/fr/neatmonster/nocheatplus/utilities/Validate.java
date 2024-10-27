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
package fr.neatmonster.nocheatplus.utilities;

/**
 * Simple parameter/thing for recurring validation.
 * 
 * @author asofold
 *
 */
public class Validate {

    /**
     * Throw a NullPointerException if any given object is null.
     * 
     * @param objects
     * @throws NullPointerException
     *             If any object is null.
     */
    public static void validateNotNull(final Object...objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] == null) {
                throw new NullPointerException("Object at index " + i + " is null.");
            }
        }
    }
    
    /**
     * Validates that the given double array's length is a multiple of 6.
     * 
     * <p>Currently, this is rather meant to validate double arrays containing multiple bounding boxes, each defined by six consecutive elements 
     * [minX, minY, minZ, maxX, maxY, maxZ].<br>
     *
     * @param array The double array to validate.
     * 
     * @throws IllegalArgumentException If the array length is not a multiple of 6, null or empty.
     */
    public static void validateAABB(final double[] array) {
        if (array == null) {
            throw new NullPointerException("The array is null.");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("The array is empty.");
        }
        if (array.length % 6 != 0) {
            throw new IllegalArgumentException("Array length is not a multiple of 6. Actual length: " + array.length);
        }
    }
}
