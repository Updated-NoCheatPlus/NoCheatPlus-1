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
import fr.neatmonster.nocheatplus.config.*;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

public class BlocksMC1_20 implements BlockPropertiesSetup {
    public BlocksMC1_20() {
        BlockInit.assertMaterialExists("CALIBRATED_SCULK_SENSOR");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        BlockInit.setAs("CALIBRATED_SCULK_SENSOR", "SCULK_SENSOR");
        BlockInit.setAs("SUSPICIOUS_GRAVEL", "SUSPICIOUS_SAND");
        
        BlockProperties.setBlockProps("PITCHER_CROP", BlockProperties.instantType);
        BlockFlags.setBlockFlags("PITCHER_CROP", BlockFlags.SOLID_GROUND);
        
        BlockFlags.setBlockFlags("SNIFFER_EGG", BlockFlags.SOLID_GROUND);
        BlockProperties.setBlockProps("SNIFFER_EGG", new BlockProperties.BlockProps(BlockProperties.noTool, 0.5f));
        
        for (Material mat : MaterialUtil.WALL_HANGING_SIGNS) {
            // These are solid
            BlockFlags.addFlags(mat, BlockFlags.SOLID_GROUND);
        }
        // (Hanging signs are treated as a normal sign and added in BlocksMC1_14)

        if (ServerVersion.compareMinecraftVersion("1.20.4") >= 0) {
            BlockProperties.setBlockProps("CRAFTER", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f));
            BlockFlags.setBlockFlags("CRAFTER", BlockFlags.FULLY_SOLID_BOUNDS);
            BlockProperties.setBlockProps("TRIAL_SPAWNER", new BlockProperties.BlockProps(BlockProperties.noTool, 50f));
            BlockFlags.setBlockFlags("TRIAL_SPAWNER", BlockFlags.FULLY_SOLID_BOUNDS);
        }
        if (ServerVersion.compareMinecraftVersion("1.20.5") >= 0) {
            BlockFlags.setBlockFlags("HEAVY_CORE", BlockFlags.SOLID_GROUND);
            BlockProperties.setBlockProps("HEAVY_CORE", new BlockProperties.BlockProps(BlockProperties.noTool, 50f));
            BlockProperties.setBlockProps("VAULT", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 10f));
            BlockFlags.setBlockFlags("VAULT", BlockFlags.FULLY_SOLID_BOUNDS);
        }
        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.20 blocks.");
    }
}
