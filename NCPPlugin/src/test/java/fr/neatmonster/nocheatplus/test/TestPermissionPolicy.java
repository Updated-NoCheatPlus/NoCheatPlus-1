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

import fr.neatmonster.nocheatplus.permissions.PermissionPolicy;
import fr.neatmonster.nocheatplus.MockServerBase;

public class TestPermissionPolicy extends MockServerBase {

    private void testToConfigString(PermissionPolicy policy, String expected) {
        if (!policy.policyToConfigLine().equals(expected)) {
            fail("Expect toConfigString() to result in '" + expected + "', got instead: '" + policy.policyToConfigLine() + "'");
        }
    }

    @Test
    public void testConfigString() {
        PermissionPolicy policy1 = new PermissionPolicy();
        testToConfigString(policy1, "ALWAYS");
        policy1.fetchingPolicyInterval(100);
        testToConfigString(policy1, "INTERVAL:0.1");
        policy1.invalidationOffline(false);
        testToConfigString(policy1, "INTERVAL:0.1,-offline");
        policy1.invalidationWorld(false);
        testToConfigString(policy1, "INTERVAL:0.1,-offline,-world");
    }

    private void testFromConfigString(PermissionPolicy policy) {
        String to = policy.policyToConfigLine();
        PermissionPolicy policy2 = new PermissionPolicy();
        policy2.setPolicyFromConfigLine(to);
        if (!policy.isPolicyEquivalent(policy2)) {
            fail("to+from config string yields non equivalent policy.");
        }
    }

    private void testFromConfigString(PermissionPolicy equivalent, String... inputs) {
        for (String input : inputs) {
            PermissionPolicy policy2 = new PermissionPolicy();
            policy2.setPolicyFromConfigLine(input);
            if (!equivalent.isPolicyEquivalent(policy2)) {
                fail("Expect equivalent policy to '" + equivalent.policyToConfigLine() + "', got instead: " + policy2.policyToConfigLine());
            }
        }
    }

    @Test
    public void testFromConfigString() {
        PermissionPolicy policy1 = new PermissionPolicy();
        testFromConfigString(policy1, "ALWAYS", "ALWAYS +offline", "ALWAYS +WORLD", "ALWAYS +world +offline", "ALWAYS:+offline +world");
        testFromConfigString(policy1);
        policy1.fetchingPolicyInterval(100);
        testFromConfigString(policy1, "INTERVAL0.1", "INTERVAL 0.1", "Interval 0.1 +ofFline");
        testFromConfigString(policy1);
        policy1.invalidationOffline(false);
        testFromConfigString(policy1, "INTERVAL 0.1 -offline");
        testFromConfigString(policy1);
        policy1.invalidationWorld(false);
        testFromConfigString(policy1, "INTERVAL 0.1 -world -offline", "INTERVAL 0.1 -offline -world");
        testFromConfigString(policy1);
    }

}
