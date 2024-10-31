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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.PluginTests;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.DefaultConfig;
import fr.neatmonster.nocheatplus.config.RawConfigFile;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.MockServerBase;

/**
 * Auxiliary classes packed in here.
 * @author dev1mc
 *
 */
public class BlockTests extends MockServerBase {

    public static class SimpleWorldConfigProvider <C extends RawConfigFile> implements WorldConfigProvider <C>{

        private final C config;

        public SimpleWorldConfigProvider(C config) {
            this.config = config;
        }

        @Override
        public C getDefaultConfig() {
            return config;
        }

        @Override
        public C getConfig(String worldName) {
            return config;
        }

        @Override
        public Collection<C> getAllConfigs() {
            final List<C> list = new ArrayList<C>();
            list.add(config);
            return list;
        }

    }

    public static class DefaultConfigWorldConfigProvider extends SimpleWorldConfigProvider<ConfigFile> {
        public DefaultConfigWorldConfigProvider() {
            super(new DefaultConfig());
        }
    }

    /**
     * Initialize BlockProperties with default config and Bukkit-API compliance :p.
     */
    public static void initBlockProperties() {
        PluginTests.setUnitTestNoCheatPlusAPI(false);
        BlockProperties.init(NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(MCAccess.class), new DefaultConfigWorldConfigProvider());
    }

}
