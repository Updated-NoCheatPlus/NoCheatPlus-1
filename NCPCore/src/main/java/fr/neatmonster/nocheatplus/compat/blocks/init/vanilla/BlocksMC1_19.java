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

import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

public class BlocksMC1_19 implements BlockPropertiesSetup {
    public BlocksMC1_19() {
        BlockInit.assertMaterialExists("SCULK_CATALYST");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        BlockFlags.setFlagsAs("OCHRE_FROGLIGHT", Material.DIRT);
        BlockProperties.setBlockProps("OCHRE_FROGLIGHT", new BlockProperties.BlockProps(BlockProperties.noTool, 0.3f));

        BlockInit.setAs("VERDANT_FROGLIGHT", "OCHRE_FROGLIGHT");

        BlockInit.setAs("PEARLESCENT_FROGLIGHT", "OCHRE_FROGLIGHT");

        BlockProperties.setBlockProps("MUD", new BlockProperties.BlockProps(BlockProperties.woodSpade, 0.5f));
        BlockFlags.setBlockFlags("MUD", BlockFlags.SOLID_GROUND);

        BlockProperties.setBlockProps("PACKED_MUD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1f));
        BlockFlags.setFlagsAs("PACKED_MUD", Material.DIRT);

        BlockInit.setAs("MUD_BRICKS", Material.ANDESITE);
        BlockInit.setAs("MUD_BRICK_SLAB", Material.ANDESITE_SLAB);

        BlockInit.setAs("MUD_BRICK_STAIRS", Material.ANDESITE_STAIRS);
        BlockInit.setAs("MUD_BRICK_WALL", Material.ANDESITE_WALL);

        BlockProperties.setBlockProps("FROGSPAWN", BlockProperties.instantType);
        BlockFlags.setBlockFlags("FROGSPAWN", BlockFlags.F_IGN_PASSABLE);

        BlockFlags.setFlagsAs("MUDDY_MANGROVE_ROOTS", Material.DIRT);
        BlockProperties.setBlockProps("MUDDY_MANGROVE_ROOTS", new BlockProperties.BlockProps(BlockProperties.woodSpade, 0.7f));

        BlockFlags.setFlagsAs("MANGROVE_ROOTS", Material.DIRT);

        BlockProperties.setBlockProps("MANGROVE_ROOTS", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.7f));
        
        BlockFlags.setBlockFlags("SCULK_VEIN", BlockFlags.F_IGN_PASSABLE);

        BlockProperties.setBlockProps("SCULK_VEIN", new BlockProperties.BlockProps(BlockProperties.woodHoe, 0.2f));

        BlockInit.setPropsAs("SCULK", "SCULK_VEIN");
        BlockFlags.setFlagsAs("SCULK", Material.DIRT);

        BlockFlags.setFlagsAs("SCULK_CATALYST", Material.DIRT);
        BlockProperties.setBlockProps("SCULK_CATALYST", new BlockProperties.BlockProps(BlockProperties.woodHoe, 3f));

        BlockFlags.setFlagsAs("REINFORCED_DEEPSLATE", Material.OBSIDIAN);
        BlockProperties.setBlockProps("REINFORCED_DEEPSLATE", new BlockProperties.BlockProps(BlockProperties.noTool, 55f));

        BlockProperties.setBlockProps("SCULK_SHRIEKER", new BlockProperties.BlockProps(BlockProperties.woodHoe, 3f));
        BlockFlags.setBlockFlags("SCULK_SHRIEKER", BlockFlags.SOLID_GROUND);

        if (ServerVersion.compareMinecraftVersion("1.19.3") >= 0) {
            BlockInit.setAs("BAMBOO_MOSAIC", Material.OAK_PLANKS);
            BlockInit.setAs("BAMBOO_BLOCK", Material.OAK_PLANKS);
            BlockInit.setAs("STRIPPED_BAMBOO_BLOCK", Material.OAK_PLANKS);
            BlockInit.setAs("CHISELED_BOOKSHELF", Material.BOOKSHELF);
        }

        if (ServerVersion.compareMinecraftVersion("1.19.4") >= 0) {
            BlockInit.setPropsAs("SUSPICIOUS_SAND", Material.SAND);
            BlockProperties.setBlockProps("SUSPICIOUS_SAND", new BlockProperties.BlockProps(BlockProperties.woodSpade, 0.25f));
            
            BlockProperties.setBlockProps("DECORATED_POT", BlockProperties.instantType);
            BlockFlags.setBlockFlags("DECORATED_POT", BlockFlags.SOLID_GROUND);
        }

        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.19 blocks.");
    }
}
