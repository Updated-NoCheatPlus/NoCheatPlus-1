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
import fr.neatmonster.nocheatplus.permissions.PermissionSettings;
import fr.neatmonster.nocheatplus.permissions.PermissionSettings.PermissionRule;
import fr.neatmonster.nocheatplus.MockServerBase;

public class TestPermissionSettings extends MockServerBase {

    @Test
    public void testRegex() {
        PermissionPolicy dummy = new PermissionPolicy();
        String regex = "^nocheatplus\\.checks\\..*\\.silent$";
        String permissionName = "nocheatplus.checks.moving.survivalfly.silent";
        // Also/rather a config test. 
        if (!permissionName.matches(regex)) {
            fail("Expect regex to match.");
        }
        PermissionRule rule = PermissionSettings.getMatchingRule("regex:" + regex, dummy);
        if (rule == null) {
            fail("Expect factory to return a regex rule.");
        }
        if (!rule.matches(permissionName)) {
            fail("Expect rule to match permissions name.");
        }
        if (rule.matches("xy" + permissionName) || rule.matches(permissionName + "yx")) {
            fail("Rule matches wrong start/end.");
        }
    }

}
