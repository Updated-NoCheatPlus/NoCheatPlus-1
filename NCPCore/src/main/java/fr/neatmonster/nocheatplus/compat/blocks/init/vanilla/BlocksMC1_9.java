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
public class BlocksMC1_9 implements BlockPropertiesSetup {

    public BlocksMC1_9() {
        BlockInit.assertMaterialExists("END_ROD");
        //BlockInit.assertMaterialExists("GRASS_PATH");
        BlockInit.assertMaterialExists("END_GATEWAY");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {

        final long ground = BlockFlags.SOLID_GROUND; // BlockFlags.F_GROUND;
        final BlockProps instant = BlockProperties.instantType;

        BlockFlags.addFlags("END_ROD", ground);
        BlockProperties.setBlockProps("END_ROD", instant);

        BlockFlags.addFlags("CHORUS_PLANT", ground);
        BlockProperties.setBlockProps("CHORUS_PLANT", new BlockProps(BlockProperties.woodAxe, 0.4f));

        BlockFlags.addFlags("CHORUS_FLOWER", ground);
        BlockProperties.setBlockProps("CHORUS_FLOWER", new BlockProps(BlockProperties.woodAxe, 0.4f));

        BlockInit.setAs("PURPUR_BLOCK", BridgeMaterial.STONE_BRICKS);

        BlockInit.setAs("PURPUR_PILLAR", BridgeMaterial.STONE_BRICKS); // Rough.

        BlockInit.setAs("PURPUR_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS); // Rough.
        
        // Uhm, why the double if?
        if (BridgeMaterial.has("PURPUR_DOUBLE_SLAB")) {
            if (BridgeMaterial.has("PURPUR_DOUBLE_SLAB")) {
                BlockInit.setAs("PURPUR_DOUBLE_SLAB", "DOUBLE_STEP");
            }
        }

        BlockInit.setAs("PURPUR_SLAB", BridgeMaterial.STONE_SLAB);

        BlockInit.setAs(BridgeMaterial.END_STONE_BRICKS, Material.SANDSTONE);
        BlockProperties.setBlockProps(BridgeMaterial.END_STONE_BRICKS, new BlockProps(BlockProperties.woodPickaxe, 3f));

        BlockInit.setInstantPassable(BridgeMaterial.BEETROOTS);

        BlockProperties.setBlockProps(BridgeMaterial.GRASS_PATH, new BlockProps(BlockProperties.woodSpade, 0.65f));
        BlockFlags.addFlags(BridgeMaterial.GRASS_PATH, BlockFlags.F_MIN_HEIGHT16_15 | BlockFlags.F_XZ100 | BlockFlags.SOLID_GROUND | BlockFlags.F_GROUND_HEIGHT);

        // -> Leave flags as is (like air).
        BlockProperties.setBlockProps("END_GATEWAY", BlockProperties.indestructibleType);

        BlockInit.setAs(BridgeMaterial.REPEATING_COMMAND_BLOCK, BridgeMaterial.COMMAND_BLOCK);

        BlockInit.setAs(BridgeMaterial.CHAIN_COMMAND_BLOCK, BridgeMaterial.COMMAND_BLOCK);

        BlockInit.setAs("FROSTED_ICE", Material.ICE);

        BlockInit.setInstantPassable("STRUCTURE_BLOCK");

        // Special case activation.
        BlockProperties.setSpecialCaseTrapDoorAboveLadder(true);

        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.9 blocks.");
    }
}
