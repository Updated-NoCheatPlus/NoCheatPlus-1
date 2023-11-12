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

import org.junit.Test;

import fr.neatmonster.nocheatplus.utilities.ds.count.acceptdeny.AcceptDenyCounter;
import fr.neatmonster.nocheatplus.utilities.ds.count.acceptdeny.IAcceptDenyCounter;

public class TestAcceptDenyCounters {

    /**
     * Test the default implementation.
     */
    @Test
    public void testAcceptDenyCounter() {
        AcceptDenyCounter leaf = new AcceptDenyCounter();
        AcceptDenyCounter parent = new AcceptDenyCounter();
        leaf.setParentCounter(parent);

        // Add to leaf.
        for (int i = 0; i < 73; i++) {
            leaf.accept();
        }
        checkCounts(leaf, 73, 0, "leaf");

        for (int i = 0; i < 65; i++) {
            leaf.deny();
        }
        checkCounts(leaf, 73, 65, "leaf");

        // Add only to parent.
        for (int i = 0; i < 52; i++) {
            parent.accept();
        }
        checkCounts(parent, 73 + 52, 65, "parent");
        checkCounts(leaf, 73, 65, "leaf");

        for (int i = 0; i < 97; i++) {
            parent.deny();
        }
        checkCounts(parent, 73 + 52, 65 + 97, "parent");
        checkCounts(leaf, 73, 65, "leaf");

        // Reset parent.
        parent.resetCounter();
        checkCounts(parent, 0, 0, "parent");
        checkCounts(leaf, 73, 65, "leaf");
    }

    public static void checkCounts(IAcceptDenyCounter counter, int acceptCount, int denyCount, String counterName) {
        if (counter.getAcceptCount() != acceptCount) {
            fail("Wrong accept count for counter '" + counterName + "': " + counter.getAcceptCount() + " instead of " + acceptCount + ".");
        }
        if (counter.getDenyCount() != denyCount) {
            fail("Wrong deny count for counter '" + counterName + "': " + counter.getDenyCount() + " instead of " + denyCount + ".");
        }
    }

    public static void checkSame(String testName, IAcceptDenyCounter... counters) {
        if (counters.length < 2) {
            return;
        }
        IAcceptDenyCounter first = counters[0];
        for (int i = 1; i < counters.length; i++) {
            if (first.getAcceptCount() != counters[i].getAcceptCount()) {
                fail("Accept count differs at index " + i + ": " + testName);
            }
            if (first.getDenyCount() != counters[i].getDenyCount()) {
                fail("Deny count differs at index " + i + ": " + testName);
            }
        }

    }

    public static void checkSame(int acceptCount, int denyCount, String testName, IAcceptDenyCounter... counters) {
        for (int i = 0; i < counters.length; i++) {
            checkCounts(counters[i], acceptCount, denyCount, "counter at index " + i + " / " + testName);
        }
        checkSame(testName, counters);
    }

}
