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

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;

import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace.ITraceEntry;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace.TraceEntryPool;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace.TraceIterator;


public class TestLocationTrace {

    /**
     * +- radius around 0.0.
     * @param radius
     * @return
     */
    public static double rand(double radius) {
        return rand(0.0, radius);
    }

    /**
     * +- radius around center.
     * @param center
     * @param radius
     * @return
     */
    public static double rand(double center, double radius) {
        return center + 2.0 * radius * (ThreadLocalRandom.current().nextDouble() - 0.5);
    }

    /**
     * center +- step
     * @param center
     * @param step
     * @return
     */
    public static double randStep(double center, double step) {
        return center + (ThreadLocalRandom.current().nextBoolean() ? step : -step);
    }

    // TODO: Test pool as well.
    private TraceEntryPool pool = new TraceEntryPool(1000);

    @Test
    public void testSize() {
        int size = 80;
        LocationTrace trace = new LocationTrace(size, size, pool);
        // Empty
        if (!trace.isEmpty()) {
            fail("Trace must be empty on start.");
        }
        if (trace.size() != 0) {
            fail("Size must be 0 at start.");
        }
        // Adding up to size elements.
        for (int i = 0; i < size ; i++) {
            trace.addEntry(i, i, i, i, 0, 0);
            if (trace.size() != i + 1) {
                fail("Wrong size, expect " + (i + 1) + ", got instead: " + trace.size());
            }
        }
        // Adding a lot of elements.
        for (int i = 0; i < 1000; i ++) {
            trace.addEntry(i + size, i, i, i, 0, 0);
            if (trace.size() != size) {
                fail("Wrong size, expect " + size + ", got instead: " + trace.size());
            }
        }
    }

    @Test
    public void testMergeZeroDist() {
        int size = 80;
        LocationTrace trace = new LocationTrace(size, size, pool);
        for (int i = 0; i < 1000; i ++) {
            trace.addEntry(i + size, 0 , 0, 0, 0, 0);
            if (trace.size() != 1) {
                fail("Wrong size, expect 1, got instead: " + trace.size());
            }
        }
    }

    @Test
    public void testEmptyIterator() {
        // Expected to fail.
        int size = 80;
        LocationTrace trace = new LocationTrace(size, size, pool);
        try {
            trace.oldestIterator();
            fail("Expect an exception on trying to get an empty iterator (oldest).");
        } catch (IllegalArgumentException ex) {

        }
        try {
            trace.latestIterator();
            fail("Expect an exception on trying to get an empty iterator (latest).");
        } catch (IllegalArgumentException ex) {

        }
        try {
            trace.maxAgeIterator(0);
            fail("Expect an exception on trying to get an empty iterator (maxAge).");
        } catch (IllegalArgumentException ex) {

        }
    }

    @Test
    public void testIteratorSizeAndOrder() {
        int size = 80;
        LocationTrace trace = new LocationTrace(size, size, pool);
        // Adding up to size elements.
        for (int i = 0; i < size; i++) {
            trace.addEntry(i, i, i, i, 0, 0);
        }
        // Test size with one time filled up.
        testIteratorSizeAndOrder(trace, 80);
        // Add size / 2 elements, to test cross-boundary iteration.
        for (int i = 0; i < size / 2; i++) {
            trace.addEntry(i + size, i, i, i, 0, 0);
        }
        // Test size again.
        testIteratorSizeAndOrder(trace, 80);
    }

    private void testIteratorSizeAndOrder(LocationTrace trace, int expectedSize) {
        int size = trace.size();
        if (size != expectedSize) {
            fail("LocationTrace size differs from expected. Expect " + expectedSize +", got instead: " + size);
        }
        TraceIterator[] iterators = new TraceIterator[] {
                trace.oldestIterator(),
                trace.latestIterator(),
                trace.maxAgeIterator(0) // Asserts entries to start at time >= 0.
        };
        String[] iteratorNames = new String[]{
                "oldest",
                "latest",
                "maxAge"
        };
        int[] increments = new int[] {
                1,
                -1,
                1
        };
        for (int i = 0; i < iterators.length; i++) {
            int n = 0;
            TraceIterator it = iterators[i];
            ITraceEntry last = null;
            ITraceEntry current = null;
            while (it.hasNext()) {
                current = it.next();
                n ++;
                if (n > size) {
                    fail("Iterator (" + iteratorNames[i] + ") iterates too long.");
                }
                if (last != null) {
                    if (current.getTime() - last.getTime() != increments[i]) {
                        fail("Bad time increment (" + iteratorNames[i] + "). Expected " + increments[i] + ", got instead: " + (current.getTime() - last.getTime()));
                    }
                }
                last = current;
            }
            if (n != size) {
                fail("Iterator (" + iteratorNames[i] + ") should have " + size + " elements, found instead: " + n);
            }
        }
    }

    @Test
    public void testMaxAgeIterator() {
        int size = 80;
        LocationTrace trace = new LocationTrace(size, size, pool);
        // Adding up to size elements.
        for (int i = 0; i < size; i++) {
            trace.addEntry(i, i, i, i, 0, 0);
        }
        for (int i = 0; i < size; i++) {
            Iterator<ITraceEntry> it = trace.maxAgeIterator(i);
            long got = it.next().getTime();
            if (got != i) {
                fail("Bad entry point for iterator (maxAge), expected " + i + ", got instead: " + got);
            }
            int n = 1;
            while (it.hasNext()) {
                it.next();
                n ++;
            }
            if (n != size - i) {
                fail("Bad number of elements for iterator (maxAge), expected " + (size - i) + ", got instead: " + n);
            }
        }
    }

    @Test
    public void testMaxAgeFirstElementAnyway() {
        int size = 80;
        LocationTrace trace = new LocationTrace(size, size, pool);
        trace.addEntry(0, 0, 0, 0, 0, 0);
        if (!trace.maxAgeIterator(1000).hasNext()) {
            fail("Expect iterator (maxAge) to always contain the latest element.");
        }
        trace.addEntry(1, 0, 0, 0, 0, 0);
        final Iterator<ITraceEntry> it = trace.maxAgeIterator(2);
        if (!it.hasNext()) {
            fail("Expect iterator (maxAge) to always contain the latest element.");
        }
        it.next();
        if (it.hasNext()) {
            fail("Expect iterator (maxAge) to have only the latest element for all out of range entries.");
        }
    }

    // TODO: Tests with expiration of entries (size and iterators).

}
