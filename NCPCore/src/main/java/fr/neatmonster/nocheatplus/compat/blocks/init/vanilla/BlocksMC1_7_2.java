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
package fr.neatmonster.nocheatplus.compat.blocks.init.vanilla;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.bukkit.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;


@SuppressWarnings("deprecation")
public class BlocksMC1_7_2 implements BlockPropertiesSetup{

    public BlocksMC1_7_2() {
        BlockInit.assertMaterialExists("DARK_OAK_STAIRS");
        BlockInit.assertMaterialExists("PACKED_ICE");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        
        ////////////////////
        // Block shapes.
        ////////////////////
        BlockInit.setAsIfExists("STAINED_GLASS", Material.GLASS);
        // Collected otherwise: BlockInit.setAsIfExists("STAINED_GLASS_PANE", "THIN_GLASS");

        BlockInit.setAsIfExists("LEAVES_2", "LEAVES");

        BlockInit.setAsIfExists("LOG_2", "LOG");

        BlockInit.setAsIfExists("PACKED_ICE", Material.ICE);

        BlockInit.setAsIfExists("DOUBLE_PLANT", BridgeMaterial.DANDELION);

        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.7.2 blocks.");
    }

}
