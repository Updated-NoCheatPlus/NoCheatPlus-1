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

import org.bukkit.Material;
import org.junit.Test;

import fr.neatmonster.nocheatplus.compat.bukkit.BridgeMaterial;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.DefaultConfig;
import fr.neatmonster.nocheatplus.config.PathUtils;
import fr.neatmonster.nocheatplus.config.RawConfigFile;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.MockServerBase;

public class TestConfig extends MockServerBase {

    private void testReadMaterial(String input, Material expectedMat) {
        Material mat = RawConfigFile.parseMaterial(input);
        if (expectedMat != mat) {
            fail("Expected " + expectedMat + " for input '" + input + "', got instead: " + mat);
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testReadMaterial() {
        // Some really needed parts first.
        Material lily = BridgeMaterial.LILY_PAD;
        String lilys = lily.name();
        testReadMaterial(lilys.replaceAll("_", " "), lily);
        testReadMaterial(lilys.replaceAll("_", "-"), lily);
        testReadMaterial(lilys.replaceAll("e", "E"), lily);

        //testReadMaterial("flint and steel", Material.FLINT_AND_STEEL);
        //testReadMaterial("259", Material.FLINT_AND_STEEL);

        // Generic test.
        for (final Material mat : Material.values()) {
            if (mat.name().equalsIgnoreCase("LOCKED_CHEST")) {
                continue;
            }
            //testReadMaterial(mat.name(), mat);
            //testReadMaterial(Integer.toString(mat.getId()), mat);
        }
    }

    // TODO: More ConfigFile tests, once processing gets changed.

    @Test
    public void testMovePaths() {
        StaticLog.setUseLogManager(false);
        ConfigFile config = new ConfigFile();

        // Simple moved boolean.
        config.set(ConfPaths.LOGGING_FILE, false);
        config = PathUtils.processPaths(config, "test", false);
        if (config == null) {
            fail("Expect config to be changed at all.");
        }
        if (config.contains(ConfPaths.LOGGING_FILE)) {
            fail("Expect path be removed: " + ConfPaths.LOGGING_FILE);
        }
        Boolean val = config.getBoolean(ConfPaths.LOGGING_BACKEND_FILE_ACTIVE, true);
        if (val == null || val.booleanValue()) {
            fail("Expect new path to be set to false: " + ConfPaths.LOGGING_BACKEND_FILE_ACTIVE);
        }
    }

    @Test
    public void testDefaults() {
        ConfigFile defaults = new ConfigFile();
        defaults.set("all", 1.0);
        defaults.set("defaultsOnly", 1.0);
        ConfigFile config = new ConfigFile();
        config.setDefaults(defaults);
        config.set("all", 2.0);
        double val = config.getDouble("all", 3.0);
        if (val != 2.0) {
            fail("Expect 2.0 if set in config, got instead: " + val);
        }
        val = config.getDouble("defaultsOnly", 3.0);
        if (val != 3.0) { // Pitty.
            fail("Expect 3.0 (default argument), got instead: " + val);
        }
        val = config.getDouble("notset", 3.0);
        if (val != 3.0) {
            fail("Expect 3.0 (not set), got instead: " + val);
        }
    }

    @Test
    public void testActionLists() {
        ConfigFile config = new DefaultConfig();
        config.getOptimizedActionList(ConfPaths.MOVING_SURVIVALFLY_ACTIONS, null);
    }

}
