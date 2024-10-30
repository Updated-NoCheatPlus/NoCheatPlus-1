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

@SuppressWarnings("deprecation")
public class BlocksMC1_8 implements BlockPropertiesSetup {

    public BlocksMC1_8() {
        //BlockInit.assertMaterialExists("BARRIER");
        BlockInit.assertMaterialExists("SLIME_BLOCK");
        BlockInit.assertMaterialExists("ACACIA_FENCE_GATE");
        //BlockInit.assertMaterialExists("STANDING_BANNER");
        BlockInit.assertMaterialExists("SEA_LANTERN");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
         
        ////////////////////////////////////////////
        // ---- Changed block break timings ----
        //////////////////////////////////////////
        // Melon/pumpkin/like.
        BlockProps props = new BlockProps(BlockProperties.woodAxe, 1f);
        for (Material mat : new Material[] {
            BridgeMaterial.MELON,
            Material.PUMPKIN,
            Material.JACK_O_LANTERN,
            BridgeMaterial.SIGN,}) {
            BlockProperties.setBlockProps(mat, props);
        }
        
        // Ladder.
        props = new BlockProps(BlockProperties.woodAxe, 0.4f);
        BlockProperties.setBlockProps(Material.LADDER, props);
        
        
        ///////////////////////////////
        // ---- New blocks ----
        //////////////////////////////
        BlockInit.setAs("SLIME_BLOCK", Material.TNT); // Full block, instant break.
        BlockFlags.addFlags("SLIME_BLOCK", BlockFlags.F_BOUNCE25 | BlockFlags.F_SLIME);

        BlockInit.setAs("BARRIER", Material.BEDROCK); // Full block, unbreakable.

        BlockFlags.setFlagsAs("IRON_TRAPDOOR", BridgeMaterial.OAK_TRAPDOOR);
        BlockInit.setPropsAs("IRON_TRAPDOOR", BridgeMaterial.IRON_DOOR);

        BlockInit.setAs("PRISMARINE", Material.STONE);

        BlockInit.setAs("SEA_LANTERN", BridgeMaterial.getFirstNotNull("redstone_lamp", "redstone_lamp_off"));

        // 176(STANDING_BANNER
        // 177(WALL_BANNER

        BlockInit.setAsIfExists("DAYLIGHT_DETECTOR_INVERTED", Material.DAYLIGHT_DETECTOR);

        BlockInit.setAs("RED_SANDSTONE", Material.SANDSTONE);

        BlockInit.setAs("RED_SANDSTONE_STAIRS", Material.SANDSTONE_STAIRS);

        BlockInit.setAsIfExists("DOUBLE_STONE_SLAB2", BridgeMaterial.get("double_step")); // TODO: red sandstone / prismarine ?

        BlockInit.setAsIfExists("STONE_SLAB2", BridgeMaterial.STONE_SLAB); // TODO: red sandstone / prismarine ?

        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.8 blocks.");
    }

}
