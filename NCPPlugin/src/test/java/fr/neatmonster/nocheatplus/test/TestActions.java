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

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.PluginTests;
import fr.neatmonster.nocheatplus.actions.Action;
import fr.neatmonster.nocheatplus.actions.ActionList;
import fr.neatmonster.nocheatplus.actions.types.PenaltyAction;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.DefaultConfig;
import fr.neatmonster.nocheatplus.penalties.PenaltyNode;
import fr.neatmonster.nocheatplus.permissions.PermissionRegistry;
import fr.neatmonster.nocheatplus.MockServerBase;

public class TestActions extends MockServerBase {

    @Test
    public void testOptimizedLogActionEmpty() {
        PluginTests.setUnitTestNoCheatPlusAPI(false);
        PermissionRegistry pReg = NCPAPIProvider.getNoCheatPlusAPI().getPermissionRegistry();
        final ConfigFile config = new DefaultConfig();
        config.set("actions", "log:dummy:0:0:icf");
        config.set("strings.dummy", "dummy");
        config.set(ConfPaths.LOGGING_ACTIVE, false);
        ActionList actionList = config.getOptimizedActionList("actions", 
                pReg.getOrRegisterPermission("dummy")) ;
        Action<ViolationData, ActionList>[] actions = actionList.getActions(0.0);
        if (actions.length != 0) {
            fail("Wrong number of actions.");
        }
    }

    @Test
    public void testCancelWithProbability() {
        PluginTests.setUnitTestNoCheatPlusAPI(false);
        PermissionRegistry pReg = NCPAPIProvider.getNoCheatPlusAPI().getPermissionRegistry();
        final ConfigFile config = new DefaultConfig();
        config.set("actions", "25%cancel");
        ActionList actionList = config.getOptimizedActionList("actions", 
                pReg.getOrRegisterPermission("dummy")) ;
        Action<ViolationData, ActionList>[] actions = actionList.getActions(0.0);
        if (actions.length != 1) {
            fail("Wrong number of actions.");
        }
        Action<?, ?> action = actions[0];
        if (action instanceof PenaltyAction) {
            PenaltyAction<?, ?> penaltyAction = (PenaltyAction<?, ?>) action;
            PenaltyNode node = penaltyAction.getPenaltyNode();
            if (node.probability != (25.0 / 100.0)) {
                fail("Expect 0.25 probability, got instead: " + node.probability);
            }
        }
        else {
            fail("Expect a penalty action here.");
        }
    }

}
