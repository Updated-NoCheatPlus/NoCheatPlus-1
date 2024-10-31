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
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.PluginTests;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.components.config.value.OverrideType;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.DefaultConfig;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.worlds.IWorldData;
import fr.neatmonster.nocheatplus.worlds.WorldDataManager;
import fr.neatmonster.nocheatplus.MockServerBase;

public class TestWorldDataManager extends MockServerBase {

    private WorldDataManager getWorldDataManager() {
        PluginTests.setUnitTestNoCheatPlusAPI(false);
        return (WorldDataManager) NCPAPIProvider.getNoCheatPlusAPI().getWorldDataManager();
    }

    /**
     * Set in the config taken from the map - create if not yet existent.
     * 
     * @param map
     * @param worldName
     * @param key
     * @param value
     */
    void set(Map<String, ConfigFile> map, String worldName, String key, Object value) {
        ConfigFile cfg = map.get(worldName);
        if (cfg == null) {
            cfg = new DefaultConfig();
            map.put(worldName, cfg);
        }
        cfg.set(key, value);
    }

    /**
     * Set for all.
     * 
     * @param map
     * @param key
     * @param value
     */
    void set(Map<String, ConfigFile> map, String key, Object value) {
        for (String worldName : map.keySet()) {
            set(map, worldName, key, value);
        }
    }

    void setup(Map<String, ConfigFile> map, String... worldNames) {
        for (String worldName : worldNames) {
            set(map, worldName, "dummy", true);
        }
    }

    @Test
    public void BasicTests() {

        //PluginTests.setUnitTestNoCheatPlusAPI(false);
        StaticLog.setUseLogManager(false);

        WorldDataManager worldMan = getWorldDataManager();

        Map<String, ConfigFile> rawWorldConfigs = new LinkedHashMap<String, ConfigFile>();

        // (Implicitly create configurations via set).
        // Default.
        set(rawWorldConfigs, null, ConfPaths.COMBINED + ConfPaths.SUB_ACTIVE, "yes");
        // All existing.
        setup(rawWorldConfigs, null, "Exist1", "Exist2");
        set(rawWorldConfigs, ConfPaths.COMBINED_MUNCHHAUSEN_CHECK, "default");

        // Exist1
        set(rawWorldConfigs, "Exist1", ConfPaths.COMBINED + ConfPaths.SUB_ACTIVE, "no");

        // Exist2
        set(rawWorldConfigs, "Exist2", ConfPaths.COMBINED_MUNCHHAUSEN_CHECK, false);

        // (Might set some particularly interesting values here.)


        /////////////////////////
        // Apply configuration
        /////////////////////////
        worldMan.applyConfiguration(rawWorldConfigs);

        for (String worldName : Arrays.asList("Exist1", "Exist2")) {
            if (rawWorldConfigs.get(worldName) != worldMan.getWorldData(worldName).getRawConfiguration()) {
                fail("Raw configuration set wrongly: " + worldName);
            }
        }

        if (!worldMan.getWorldData("notExist1").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Inherited from default: COMBINED_MUNCHHAUSEN should be active (-> COMBINED is)");
        }

        if (!worldMan.getDefaultWorldData().isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Default: COMBINED_MUNCHHAUSEN should be active (-> COMBINED is)");
        }

        if (worldMan.getWorldData("Exist1").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Specific: COMBINED_MUNCHHAUSEN should not be active (-> COMBINED is not)");
        }

        if (worldMan.getWorldData("Exist2").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Specific: COMBINED_MUNCHHAUSEN should not be active (directly set)");
        }

        if (!worldMan.getWorldData("notExist2").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Inherited from default: COMBINED_MUNCHHAUSEN should be active (-> COMBINED is)");
        }


        ////////////////
        // Override
        ////////////////

        // Override via config "reload":
        set(rawWorldConfigs, "Exist2", ConfPaths.COMBINED_MUNCHHAUSEN_CHECK, true);
        worldMan.applyConfiguration(rawWorldConfigs);
        if (!worldMan.getWorldData("Exist2").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Specific: COMBINED_MUNCHHAUSEN should be active (directly set)");
        }

        // Specific override (mild).
        worldMan.overrideCheckActivation(CheckType.COMBINED, AlmostBoolean.NO, 
                OverrideType.SPECIFIC, false);
        if (worldMan.getWorldData("notExist2").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Overridden (inherited from default): COMBINED_MUNCHHAUSEN should not be active (-> COMBINED is not)");
        }
        worldMan.overrideCheckActivation(CheckType.COMBINED, AlmostBoolean.NO, 
                OverrideType.SPECIFIC, true);
        if (worldMan.getWorldData("notExist2").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Overridden (inherited from default): COMBINED_MUNCHHAUSEN should not be active (overrideChildren from COMBINED should explicitly set this)");
        }


        ///////////////////
        // Fake reload 1
        ///////////////////
        /*
         * NOTE: With this testing scenario, ConfigFile instances within
         * rawWorldConfigs stay identical, which would not be the case with a
         * ConfigManager based reload.
         */
        worldMan.applyConfiguration(rawWorldConfigs);

        if (!worldMan.getWorldData("notExist2").isCheckActive(CheckType.COMBINED)) {
            fail("Inherited from default: COMBINED should be active after reload.");
        }
        if (!worldMan.getWorldData("notExist2").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Inherited from default: COMBINED_MUNCHHAUSEN should be active (-> COMBINED is)");
        }


        ////////////////
        // Override 2
        ////////////////

        worldMan.getWorldData("NotExist3").overrideCheckActivation(CheckType.COMBINED_MUNCHHAUSEN, 
                AlmostBoolean.NO, OverrideType.SPECIFIC, false);
        if (worldMan.getWorldData("notExist3").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Overridden (SPECIFIC): COMBINED_MUNCHHAUSEN should not be active (-directly set)");
        }


        ///////////////////
        // Fake reload 2
        ///////////////////
        worldMan.applyConfiguration(rawWorldConfigs);

        if (!worldMan.getWorldData("notExist3").isCheckActive(CheckType.COMBINED_MUNCHHAUSEN)) {
            fail("Overridden (SPECIFIC): COMBINED_MUNCHHAUSEN should be active after reload (COMBINED is)");
        }

        //////////////////////////////////////////////////////////////////////

        ///////////////////
        // Reset
        ///////////////////

        worldMan = getWorldDataManager();
        rawWorldConfigs = new LinkedHashMap<String, ConfigFile>();
        set(rawWorldConfigs, null, "dummy", "dummy");
        worldMan.applyConfiguration(rawWorldConfigs);

        IWorldData defaultWorldData = worldMan.getDefaultWorldData();

        // Set all to NO.
        defaultWorldData.overrideCheckActivation(CheckType.ALL, AlmostBoolean.NO, 
                OverrideType.VOLATILE, true);
        for (CheckType checkType : CheckType.values()) {
            if (defaultWorldData.isCheckActive(checkType)) {
                fail("Expect check not to be active: " + checkType);
            }
        }

        // Set all to YES.
        defaultWorldData.overrideCheckActivation(CheckType.ALL, AlmostBoolean.YES, 
                OverrideType.VOLATILE, true);
        for (CheckType checkType : CheckType.values()) {
            if (!defaultWorldData.isCheckActive(checkType)) {
                fail("Expect check to be active: " + checkType);
            }
        }

        // Set FastHeal to NO
        defaultWorldData.overrideCheckActivation(CheckType.FIGHT_FASTHEAL, AlmostBoolean.NO, 
                OverrideType.VOLATILE, true);
        if (defaultWorldData.isCheckActive(CheckType.FIGHT_FASTHEAL)) {
            fail("Expect FIGHT_FASTHEAL not to be active.");
        }

    }

}
