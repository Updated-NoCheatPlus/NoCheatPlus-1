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

/**
 * This is an attempt to add Minecraft 1.5 blocks information without actual 1.5 dependency.
 * @author mc_dev
 *
 */
public class BlocksMC1_5 implements BlockPropertiesSetup {

    public BlocksMC1_5(){
        // Test if materials exist.
        BlockInit.assertMaterialExists("REDSTONE_BLOCK");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {

        /////////////////////
        // New blocks
        ////////////////////
        BlockInit.setAs("TRAPPED_CHEST", Material.CHEST);

        BlockInit.setAs(BridgeMaterial.LIGHT_WEIGHTED_PRESSURE_PLATE,  BridgeMaterial.STONE_PRESSURE_PLATE);

        BlockInit.setAs(BridgeMaterial.HEAVY_WEIGHTED_PRESSURE_PLATE, BridgeMaterial.STONE_PRESSURE_PLATE);

        // HACK 1.13
        Material comparator = BridgeMaterial.get("comparator");
        if (comparator == null) {
            // LEGACY
            BlockInit.setAs("REDSTONE_COMPARATOR_OFF", "DIODE_BLOCK_OFF");
            BlockInit.setAs("REDSTONE_COMPARATOR_ON", "DIODE_BLOCK_ON");
        }
        // 1.13
        else BlockInit.setAs("COMPARATOR", BridgeMaterial.REPEATER);

        BlockInit.setPropsAs("DAYLIGHT_DETECTOR", Material.VINE);
        BlockFlags.setBlockFlags("DAYLIGHT_DETECTOR", BlockFlags.SOLID_GROUND | BlockFlags.F_XZ100);

        BlockInit.setPropsAs("REDSTONE_BLOCK", BridgeMaterial.ENCHANTING_TABLE);
        BlockFlags.setBlockFlags("REDSTONE_BLOCK", BlockFlags.FULLY_SOLID_BOUNDS);

        BlockInit.setAs(BridgeMaterial.NETHER_QUARTZ_ORE, Material.COAL_ORE);

        BlockInit.setAs("HOPPER", Material.COAL_ORE);
        BlockFlags.addFlags("HOPPER", BlockFlags.F_GROUND_HEIGHT | BlockFlags.F_MIN_HEIGHT8_5);

        BlockInit.setAs("QUARTZ_BLOCK", Material.SANDSTONE);

        BlockInit.setAs("QUARTZ_STAIRS", Material.SANDSTONE_STAIRS);

        BlockInit.setAs("ACTIVATOR_RAIL", Material.DETECTOR_RAIL);

        BlockInit.setAs("DROPPER", Material.DISPENSER);

        if (BridgeMaterial.getBlock("wall_sign") != null) BlockInit.setAs("WALL_SIGN", BridgeMaterial.SIGN);

        /////////////////////
        // Changed blocks
        ////////////////////
        // 78 Snow
        BlockFlags.addFlags("SNOW", BlockFlags.F_HEIGHT_8_INC | BlockFlags.F_XZ100 | BlockFlags.F_GROUND_HEIGHT | BlockFlags.F_GROUND);

        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.5 blocks.");
    }
}
