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

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties.BlockProps;


@SuppressWarnings("deprecation")
public class BlocksMC1_7_2 implements BlockPropertiesSetup{

    public BlocksMC1_7_2() {
        BlockInit.assertMaterialExists("DARK_OAK_STAIRS");
        BlockInit.assertMaterialExists("PACKED_ICE");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        // Block shapes.

        // Stained glass
        BlockInit.setAsIfExists("STAINED_GLASS", Material.GLASS);
        // Stained glass pane
        // Collected otherwise: BlockInit.setAsIfExists("STAINED_GLASS_PANE", "THIN_GLASS");
        // Leaves 2
        BlockInit.setAsIfExists("LEAVES_2", "LEAVES");
        // Log 2
        BlockInit.setAsIfExists("LOG_2", "LOG");
        // Acacia wood stairs
        //        BlockInit.setAsIfExists("ACACIA_STAIRS", "WOOD_STAIRS");
        //        // Oak wood stairs
        //        BlockInit.setAsIfExists("DARK_OAK_STAIRS", "WOOD_STAIRS");
        // Packed ice
        BlockInit.setAsIfExists("PACKED_ICE", Material.ICE);
        // Large flowers
        BlockInit.setAsIfExists("DOUBLE_PLANT", BridgeMaterial.DANDELION);

        // Block breaking.
        final long[] ironTimes = BlockProperties.secToMs(15, 7.5, 1.15, 0.75, 0.56, 0.5, 1.25);
        final BlockProps ironType = new BlockProps(BlockProperties.woodPickaxe, 3, ironTimes);
        for (Material mat : new Material[]{
                Material.LAPIS_ORE, Material.LAPIS_BLOCK, Material.IRON_ORE,
        }) {
            BlockProperties.setBlockProps(mat, ironType);
        }
        final long[] diamondTimes = BlockProperties.secToMs(15, 7.5, 3.75, 0.75, 0.56, 0.5, 1.25);
        final BlockProps diamondType = new BlockProps(BlockProperties.woodPickaxe, 3, diamondTimes);
        for (Material mat : new Material[]{
                Material.REDSTONE_ORE, BridgeMaterial.get("glowing_redstone_ore"),
                Material.EMERALD_ORE, Material.GOLD_ORE, Material.DIAMOND_ORE,
        }) {
            if (mat != null) {
                BlockProperties.setBlockProps(mat, diamondType);
            }
        }

        StaticLog.logInfo("Added block-info for Minecraft 1.7.2 blocks.");
    }

}
