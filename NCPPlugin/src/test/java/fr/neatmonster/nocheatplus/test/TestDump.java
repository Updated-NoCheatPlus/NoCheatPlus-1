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

import org.junit.Test;

import fr.neatmonster.nocheatplus.PluginTests;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.CheckType.CheckTypeType;
import fr.neatmonster.nocheatplus.permissions.RegisteredPermission;
import fr.neatmonster.nocheatplus.MockServerBase;

/**
 * Uh oh.
 * @author asofold
 *
 */
public class TestDump extends MockServerBase {
    @Test
    public void dumpCheckSilentPermissionForPluginYML() {
        PluginTests.setUnitTestNoCheatPlusAPI(false);
        for (CheckType checkType : CheckType.values()) {
            final RegisteredPermission permission = checkType.getPermission();
            // Only add for actual checks at present.
            if (permission == null || checkType.getType() != CheckTypeType.CHECK) {
                continue;
            }
            // Print:
            System.out.print("    " + permission.getStringRepresentation() + ".silent:\n        default: false\n");
        }
    }
}
