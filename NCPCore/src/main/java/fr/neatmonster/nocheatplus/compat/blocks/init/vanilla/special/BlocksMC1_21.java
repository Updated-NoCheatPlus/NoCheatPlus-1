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
package fr.neatmonster.nocheatplus.compat.blocks.init.vanilla.special;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

public class BlocksMC1_21 implements BlockPropertiesSetup {
    public BlocksMC1_21() {
        BlockInit.assertMaterialExists("CREAKING_HEART");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        BlockProperties.setBlockProps("OPEN_EYEBLOSSOM", BlockProperties.instantType);
        BlockFlags.setBlockFlags("OPEN_EYEBLOSSOM", BlockFlags.F_IGN_PASSABLE);
        
        BlockProperties.setBlockProps("CLOSED_EYEBLOSSOM", BlockProperties.instantType);
        BlockFlags.setBlockFlags("CLOSED_EYEBLOSSOM", BlockFlags.F_IGN_PASSABLE);
        
        BlockProperties.setBlockProps("PALE_HANGING_MOSS", BlockProperties.instantType);
        BlockFlags.setBlockFlags("PALE_HANGING_MOSS", BlockFlags.F_IGN_PASSABLE);
        
        BlockProperties.setBlockProps("RESIN_CLUMP", BlockProperties.instantType);
        BlockFlags.setBlockFlags("RESIN_CLUMP", BlockFlags.F_IGN_PASSABLE);
        
        BlockInit.setAs("PALE_MOSS_BLOCK", Material.MOSS_BLOCK); // Uh oh, BridgeMaterial.MOSS here?
        
        BlockInit.setAs("RESIN_BLOCK", Material.TNT); 
        
        BlockProperties.setBlockProps("CREAKING_HEART", new BlockProperties.BlockProps(BlockProperties.woodAxe, 10f));
        BlockFlags.setBlockFlags("CREAKING_HEART", BlockFlags.FULLY_SOLID_BOUNDS);
        
        BlockInit.setAs("RESIN_BRICKS", Material.MUD_BRICKS); // From wiki, mud bricks and resin bricks have the same properties times.
        
        BlockInit.setAs("RESIN_BRICK_SLAB", Material.MUD_BRICK_SLAB);
        
        BlockInit.setAs("RESIN_BRICK_STAIRS", Material.MUD_BRICK_STAIRS);
        
        BlockInit.setAs("RESIN_BRICK_WALL", Material.MUD_BRICK_WALL);
        
        BlockInit.setAs("CHISELED_RESIN_BRICKS", Material.MUD_BRICKS);
               
        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.21 blocks.");
    }
}
