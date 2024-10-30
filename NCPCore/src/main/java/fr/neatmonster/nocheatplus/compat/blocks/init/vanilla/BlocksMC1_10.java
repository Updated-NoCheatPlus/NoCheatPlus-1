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
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties.BlockProps;

/**
 * Blocks for Minecraft 1.10.
 * 
 * @author asofold
 *
 */
public class BlocksMC1_10 implements BlockPropertiesSetup {

    public BlocksMC1_10() {
        if (BridgeMaterial.MAGMA_BLOCK == null) {
            throw new RuntimeException("Not suitable.");
        }
        BlockInit.assertMaterialExists("BONE_BLOCK");
        BlockInit.assertMaterialExists("STRUCTURE_VOID");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {

        BlockProperties.setBlockProps(BridgeMaterial.MAGMA_BLOCK, new BlockProps(BlockProperties.stonePickaxe, 0.5f, true));

        BlockInit.setPropsAs("NETHER_WART_BLOCK", BridgeMaterial.SKELETON_SKULL);

        BlockInit.setAs(BridgeMaterial.RED_NETHER_BRICKS, BridgeMaterial.NETHER_BRICKS);

        BlockInit.setAs("BONE_BLOCK", Material.COBBLESTONE);

        BlockInit.setInstantPassable("STRUCTURE_VOID");

        // Not sure when: structure block is solid.
        BlockFlags.setFullySolidFlags("STRUCTURE_BLOCK");

        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.10 blocks.");
    }
}
