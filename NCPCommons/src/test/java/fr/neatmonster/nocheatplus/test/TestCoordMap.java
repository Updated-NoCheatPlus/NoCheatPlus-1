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
package fr.neatmonster.nocheatplus.test;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;
import fr.neatmonster.nocheatplus.utilities.ds.map.CoordHashMap;
import fr.neatmonster.nocheatplus.utilities.ds.map.CoordMap;
import fr.neatmonster.nocheatplus.utilities.ds.map.CoordMap.Entry;
import fr.neatmonster.nocheatplus.utilities.ds.map.LinkedCoordHashMap;
import fr.neatmonster.nocheatplus.utilities.ds.map.LinkedCoordHashMap.MoveOrder;

public class TestCoordMap {

    public static class Pos{
        private static final int p1 = 73856093;
        private static final int p2 = 19349663;
        private static final int p3 = 83492791;
        private static final int getHash(final int x, final int y, final int z) {
            return p1 * x ^ p2 * y ^ p3 * z;
        }
        public final int x;
        public final int y;
        public final int z;
        private final int hash;
        public Pos(int x, int y, int z){
            this.x = x;
            this.y = y;
            this.z = z;
            this.hash = getHash(x, y, z);
        }
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof Pos){
                Pos other = (Pos) obj;
                return other.hash == hash && other.x == x && other.y == y && other.z == z;
            }
            else {
                return false;
            }
        }
        @Override
        public int hashCode() {
            return hash;
        }
    }

    private final boolean extraTesting = BuildParameters.testLevel > 0;
    private final int suggestedSamples = extraTesting ? 40000 : 1250;

    public int[][] getRandomCoords(int n, int max, Random random) {
        final int [][] coords = new int[n][3];
        for (int i = 0; i < n; i++){
            for (int j = 0; j < 3 ; j++){
                coords[i][j] = random.nextInt(2*max) - max;
            }
        }
        return coords;
    }

    public int[][] getUniqueRandomCoords(int n, int max, Random random) {
        Set<Pos> present = new HashSet<Pos>();
        int failures = 0;
        final int [][] coords = new int[n][3];
        for (int i = 0; i < n; i++){
            boolean unique = false;
            Pos pos = null;
            while (!unique){
                pos = new Pos(random.nextInt(2*max) - max, random.nextInt(2*max) - max, random.nextInt(2*max) - max);
                if (!present.contains(pos)) break;
                failures ++;
                if (failures >= 2 * n){
                    throw new RuntimeException("Too many failed attempts to create a unique coordinate.");
                }
            }
            coords[i][0] = pos.x;
            coords[i][1] = pos.y;
            coords[i][2] = pos.z;
            present.add(pos);
        }
        present.clear();
        return coords;
    }

    public Map<Integer, int[]> getIndexMap(int[][] coords) {
        final Map<Integer, int[]> indexMap = new HashMap<Integer, int[]>(coords.length);
        for (int i = 0; i < coords.length; i ++){
            indexMap.put(i, coords[i]);
        }
        return indexMap;
    }

    /**
     * Fill map and check if filled in elements are inside (no size check).
     * @param map
     * @param coords
     */
    public void fillMap(CoordMap<Integer> map, int[][] coords) {
        for (int i = 0; i < coords.length ; i++){
            map.put(coords[i][0], coords[i][1], coords[i][2], i);
            Integer value = map.get(coords[i][0], coords[i][1], coords[i][2]);
            if (value == null) fail("Value is null, get after put: " + i);
            else if (value.intValue() != i) fail("get right after put");
            if (!map.contains(coords[i][0], coords[i][1], coords[i][2])) fail("Contains returns false: " + i);
        }
    }

    /**
     * Match map contents (must match exactly).
     * @param map
     * @param coords
     */
    public void matchAll(CoordMap<Integer> map, int[][] coords) {
        for (int i = 0; i < coords.length ; i++){
            Integer value = map.get(coords[i][0], coords[i][1], coords[i][2]);
            if (value == null) fail("Value is null instead of " + i);
            if (value.intValue() != i) fail("Wrong value: " + value + " vs. " + i);
            if (!map.contains(coords[i][0], coords[i][1], coords[i][2])) fail("Contains returns false.");
        }
        if (map.size() != coords.length) fail("Iterator wrong number of elements: " + map.size() + "/" + coords.length);

    }

    /**
     * Match map contents with an (must match exactly).
     * @param map
     * @param indexMap
     */
    public void matchAllIterator(CoordMap<Integer> map, Map<Integer, int[]> indexMap){
        Iterator<Entry<Integer>> it = map.iterator();
        Set<Integer> found = new HashSet<Integer>();
        while (it.hasNext()){
            Entry<Integer> entry = it.next();
            Integer value = entry.getValue();
            if (value == null) fail("Null value.");
            int[] pos = indexMap.get(value);
            //			if (pos == null) fail
            if (pos[0] != entry.getX() || pos[1] != entry.getY() || pos[2] != entry.getZ()) fail("Wrong coordinates.");
            if (map.get(pos[0], pos[1], pos[2]).intValue() != value.intValue()) fail("Wrong value.");
            if (found.contains(value)) fail("Already found: " + value);
            if (!map.contains(pos[0], pos[1], pos[2])) fail("Contains returns false");
            found.add(value);
        }
        if (found.size() != indexMap.size()) fail("Iterator wrong number of elements: " + found.size() + "/" + indexMap.size());

    }

    /**
     * Remove all coords (expect map to be only filled with those).
     * @param map
     * @param coords
     */
    public void removeAll(CoordMap<Integer> map, int[][] coords) {
        for (int i = 0; i < coords.length ; i++){
            if (map.remove(coords[i][0], coords[i][1], coords[i][2]).intValue() != i) fail("removed should be " + i );
            int expectedSize = coords.length - (i + 1);
            if (map.size() != expectedSize) fail("Bad size (" + map.size() + "), expect " + expectedSize);
            if (map.get(coords[i][0], coords[i][1], coords[i][2]) != null) fail("get right after remove not null");
            if (map.contains(coords[i][0], coords[i][1], coords[i][2])) fail ("Still contains");
        }
        if (map.size() != 0) fail("Map not emptied, left: " + map.size());
    }

    /**
     *  Remove all coords using an iterator (expect map to be only filled with those).
     * @param map
     * @param indexMap
     */
    public void removeAllIterator(CoordMap<Integer> map, Map<Integer, int[]> indexMap)
    {
        Iterator<Entry<Integer>> it = map.iterator();
        Set<Integer> removed = new HashSet<Integer>();
        while (it.hasNext()){
            Entry<Integer> entry = it.next();
            Integer value = entry.getValue();
            if (value == null) fail("Null value.");
            int[] pos = indexMap.get(value);
            //			if (pos == null) fail
            if (pos[0] != entry.getX() || pos[1] != entry.getY() || pos[2] != entry.getZ()) fail("Wrong coordinates.");
            if (removed.contains(value)) fail("Already removed: " + value);
            removed.add(value);
            it.remove();
            if (map.get(pos[0], pos[1], pos[2]) != null) fail("get right after remove not null");
            if (map.contains(pos[0], pos[1], pos[2])) fail("Still contains.");
        }
        if (map.size() != 0) fail("Map not emptied, left: " + map.size());
        if (removed.size() != indexMap.size()) fail("Iterator wrong number of elements: " + removed.size() + "/" + indexMap.size());
    }

    public void assertSize(CoordMap<?> map, int size){
        if (map.size() != size) fail("Map returns wrong size: " + map.size() + " instead of " + size);
        int found = 0;
        final Iterator<?> it = map.iterator();
        while (it.hasNext()){
            found ++;
            it.next();
        }
        if (found != size) fail("Iterator has wrong number of elements: " + found + " instead of " + size);
    }

    /**
     * One integrity test series with a map with given initial size.
     * @param coords
     * @param indexMap
     * @param initialSize
     */
    @SuppressWarnings("unchecked")
    public  void series(int[][] coords, Map<Integer, int[]> indexMap, int initialSize, float loadFactor) {

        // Fill and check
        for (CoordMap<Integer> map : Arrays.asList(
                new CoordHashMap<Integer>(initialSize, loadFactor),
                new LinkedCoordHashMap<Integer>(initialSize, loadFactor)
                )) {
            fillMap(map, coords);
            assertSize(map, indexMap.size());
            matchAll(map, coords);
        }

        // Fill and check with iterator.
        for (CoordMap<Integer> map : Arrays.asList(
                new CoordHashMap<Integer>(initialSize, loadFactor),
                new LinkedCoordHashMap<Integer>(initialSize, loadFactor)
                )) {
            fillMap(map, coords);
            assertSize(map, indexMap.size());
            matchAllIterator(map, indexMap);
        }

        // Normal removing
        for (CoordMap<Integer> map : Arrays.asList(
                new CoordHashMap<Integer>(initialSize, loadFactor),
                new LinkedCoordHashMap<Integer>(initialSize, loadFactor)
                )) {
            fillMap(map, coords);
            assertSize(map, indexMap.size());
            removeAll(map, coords);
            assertSize(map, 0);
        }

        // Removing with iterator.
        for (CoordMap<Integer> map : Arrays.asList(
                new CoordHashMap<Integer>(initialSize, loadFactor),
                new LinkedCoordHashMap<Integer>(initialSize, loadFactor)
                )) {
            fillMap(map, coords);
            assertSize(map, indexMap.size());
            removeAllIterator(map, indexMap);
            assertSize(map, 0);
        }

        // Fill twice.
        for (CoordMap<Integer> map : Arrays.asList(
                new CoordHashMap<Integer>(initialSize, loadFactor),
                new LinkedCoordHashMap<Integer>(initialSize, loadFactor)
                )) {
            fillMap(map, coords);
            assertSize(map, indexMap.size());
            fillMap(map, coords);
            assertSize(map, indexMap.size());
            matchAll(map, coords);
            removeAll(map, coords);
            assertSize(map, 0);
        }

        // Fill twice iterator.
        for (CoordMap<Integer> map : Arrays.asList(
                new CoordHashMap<Integer>(initialSize, loadFactor),
                new LinkedCoordHashMap<Integer>(initialSize, loadFactor)
                )) {
            fillMap(map, coords);
            assertSize(map, indexMap.size());
            fillMap(map, coords);
            assertSize(map, indexMap.size());
            matchAllIterator(map, indexMap);
            removeAllIterator(map, indexMap);
            assertSize(map, 0);
        }

        // TODO: test / account for identical keys.

        // ? random sequence of actions ?
    }

    @Test
    public void testIntegrity() {

        final Random random = new Random(System.nanoTime() - (System.currentTimeMillis() % 2 == 1 ? 37 : 137));

        final int n = suggestedSamples; // Number of coordinates.
        final int max = 800; // Coordinate maximum.

        int [][] coords = getUniqueRandomCoords(n, max, random);

        Map<Integer, int[]> indexMap = getIndexMap(coords);

        // No resize 
        series(coords, indexMap, 3 * n, 0.75f);

        // With some times resize.
        series(coords, indexMap, 1, 0.75f);

        // TODO: Also test with certain sets of coords that always are the same.
        // TODO: fill in multiple times + size, fill in new ones (careful random) + size
    }

    @Test
    public void testLinkedCoordHashMap() {

        final Random random = new Random(System.nanoTime() - (System.currentTimeMillis() % 2 == 1 ? 37 : 137));

        final int n = suggestedSamples; // Number of coordinates.
        final int max = 800; // Coordinate maximum.

        // Preparecoordinates.
        int [][] coords = getUniqueRandomCoords(n, max, random);
        LinkedCoordHashMap<Integer> map = new LinkedCoordHashMap<Integer>(1, 0.75f);

        // Use a map with these coordinates.
        fillMap(map, coords);

        // Initial iteration order.
        testIterationOrder(map, coords, 1);

        // Re-put, moving to end.
        for (int i = 0; i < coords.length; i++) {
            map.put(coords[i][0], coords[i][1], coords[i][2], i, MoveOrder.END);
            testLast(map, coords[i], i);
        }
        if (map.size() != coords.length) {
            fail("Map different size than coords.");
        }
        testIterationOrder(map, coords, 1);

        // Re-put, moving to front.
        for (int i = coords.length - 1; i >= 0; i--) {
            map.put(coords[i][0], coords[i][1], coords[i][2], i, MoveOrder.FRONT);
            testFirst(map, coords[i], i);
        }
        if (map.size() != coords.length) {
            fail("Map different size than coords.");
        }
        testIterationOrder(map, coords, 1);

        // Map.clear
        map.clear();
        if (map.size() != 0) {
            fail("Expect map size to be 0 after clear.");
        }
        if (map.iterator(false).hasNext()) {
            fail("Expect no first element on iteration after clear.");
        }
        if (map.iterator(true).hasNext()) {
            fail("Expect no last element on iteration after clear.");
        }

        // New map with all coordinates.
        fillMap(map, coords);
        // Half the coordinates.
        int[][] halfCoords = new int[n / 2][3];
        for (int i = 0; i < n / 2; i++) {
            for (int j = 0; j < 3; j++) {
                halfCoords[i][j] = coords[i * 2][j];
            }
        }
        // Test remove every second entry.
        for (int i = 0; i < n / 2; i++) {
            map.remove(coords[i * 2 + 1][0], coords[i * 2 + 1][1], coords[i * 2 + 1][2]);
            if (map.contains(coords[i * 2 + 1][0], coords[i * 2 + 1][1], coords[i * 2 + 1][2])) {
                fail("Expect removed entries not to be contained in the map.");
            }
        }
        if (map.size() != n / 2) {
            fail("Map size should be halfed after removing every second element (" + map.size() + " instead of " + n / 2 + ").");
        }
        testIterationOrder(map, halfCoords, 2);

        // Test iterator.remove every second entry.
        map.clear();
        fillMap(map, coords);
        int i = 0;
        Iterator<Entry<Integer>> it = map.iterator(false);
        while (it.hasNext()) {
            Entry<Integer> entry = it.next();
            if (i % 2 == 1) {
                it.remove();
                if (map.contains(entry.getX(), entry.getY(), entry.getZ())) {
                    fail("Expect entries removed by iterator not to be in the map.");
                }
            }
            i ++;
        }
        if (map.size() != n / 2) {
            fail("Map size should be halfed after removing every second element with an iterator (" + map.size() + " instead of " + n / 2 + ").");
        }
        testIterationOrder(map, halfCoords, 2);


        // TODO: Some random mixtures.

    }

    private void testIterationOrder(LinkedCoordHashMap<Integer> map, int[][] coords, int multiplyId) {
        // Test if the order of iteration is correct (!).
        int i = 0;
        Iterator<Entry<Integer>> it = map.iterator(false); // New entries are put to the end.
        while (it.hasNext()) {
            testNext(it, coords, i, multiplyId);
            i++;
        }
        if (i != coords.length) {
            fail("Iterator different size than coords.");
        }
        if (i != map.size()) {
            fail("Iterator different size than map.");
        }
        if (map.size() != coords.length) {
            fail("Map different size than coords.");
        }
        i = coords.length - 1;
        it = map.iterator(true);
        while (it.hasNext()) {
            testNext(it, coords, i, multiplyId);
            i--;
        }
        if (i != -1) {
            fail("Iterator wrong size.");
        }
        if (map.size() != coords.length) {
            fail("Map different size than coords.");
        }
    }

    /**
     * Test if the last element is the expected one.
     * @param map
     * @param is
     */
    private void testLast(LinkedCoordHashMap<Integer> map, int[] coords, int value) {
        if (map.get(coords[0], coords[1], coords[2]) != value) {
            fail("Not even in the map: " + value);
        }
        Entry<Integer> entry = map.iterator(true).next();
        if (entry.getValue() != value) {
            fail("Wrong id: " + entry.getValue() + " instead of " + value);
        }
        if (entry.getX() != coords[0] || entry.getY() != coords[1] || entry.getZ() != coords[2]) {
            fail("Coordinate mismatch on " + value);
        }
    }

    /**
     * Test if the first element is the expected one.
     * @param map
     * @param is
     */
    private void testFirst(LinkedCoordHashMap<Integer> map, int[] coords, int value) {
        if (map.get(coords[0], coords[1], coords[2]) != value) {
            fail("Not even in the map: " + value);
        }
        Entry<Integer> entry = map.iterator().next();
        if (entry.getValue() != value) {
            fail("Wrong id: " + entry.getValue() + " instead of " + value);
        }
        if (entry.getX() != coords[0] || entry.getY() != coords[1] || entry.getZ() != coords[2]) {
            fail("Coordinate mismatch on " + value);
        }
    }

    private void testNext(Iterator<Entry<Integer>> it, int[][] coords, int matchIndex, int multiplyId) {
        Entry<Integer> entry = it.next();
        if (entry.getValue().intValue() != matchIndex * multiplyId) {
            fail("Index vs. value mismatch, expect " + matchIndex * multiplyId + ", got instead: " + entry.getValue());
        }
        if (entry.getX() != coords[matchIndex][0] || entry.getY() != coords[matchIndex][1] || entry.getZ() != coords[matchIndex][2]) {
            // Very unlikely.
            fail("Coordinate mismatch at index: " + matchIndex);
        }
    }

}
