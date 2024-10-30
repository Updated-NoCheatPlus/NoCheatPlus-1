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
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigFile;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties.BlockProps;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;


@SuppressWarnings("deprecation")
public class BlocksMC1_13 implements BlockPropertiesSetup {

    public BlocksMC1_13() {
        BlockInit.assertMaterialExists("LILY_PAD");
        BlockInit.assertMaterialExists("CAVE_AIR");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        // Flag for anvil
        BlockFlags.setBlockFlags("ANVIL", BlockFlags.SOLID_GROUND);
        // Void air.
        BlockInit.setAs("VOID_AIR", Material.AIR);
        // Cave air.
        BlockInit.setAs("CAVE_AIR", Material.AIR);

        // Dirt like.
        BlockInit.setAs("PODZOL", Material.DIRT);
        BlockInit.setAs("COARSE_DIRT", Material.DIRT);

        // Coral blocks (dead or alive).
        for (Material mat : MaterialUtil.CORAL_BLOCKS) {
            BlockInit.setAs(mat, Material.STONE);
        }

        // Passable (alive) coral parts.

        // Dead coral parts (solid + ground already set).
        for (Material mat : MaterialUtil.DEAD_CORAL_PARTS) {
            // (Flags should be set correctly by default.)
            BlockProperties.setBlockProps(mat, BlockProperties.instantType);
            BlockFlags.setBlockFlags(mat, BlockFlags.F_IGN_PASSABLE);
        }

        // Water plants
        for (final Material mat : MaterialUtil.WATER_PLANTS) {
            BlockFlags.setFlag(mat, BlockFlags.F_XZ100 | BlockFlags.F_WATER_PLANT | BlockFlags.F_LIQUID | BlockFlags.F_WATER);
        }

        // Bubble column.
        BlockInit.setAs("BUBBLE_COLUMN", Material.WATER);
        BlockFlags.addFlags("BUBBLE_COLUMN", BlockFlags.F_BUBBLE_COLUMN);

        // Further melon/pumpkin stems.

        // Wall torch
        BlockInit.setInstantPassable("WALL_TORCH");

        // Shulker boxes.
        for (Material mat : MaterialUtil.SHULKER_BOXES) {
            BlockFlags.addFlags(mat, BlockFlags.F_XZ100 | BlockFlags.SOLID_GROUND);
        }
        
        for (Material mat : MaterialUtil.INFESTED_BLOCKS) {
            BlockProperties.setBlockProps(mat, BlockProperties.instantType);
        }

        // Stone types.
        for (Material mat : BridgeMaterial.getAllBlocks("andesite", "diorite", "granite", 
                "polished_andesite", "polished_diorite", "polished_granite",
                "smooth_stone")) {
            BlockInit.setAs(mat, Material.STONE);
        }
        BlockProperties.setBlockProps("SMOOTH_STONE", BlockProperties.stoneTypeII);

        // Wall heads.
        for (Material mat : MaterialUtil.HEADS_WALL) {
            BlockInit.setAs(mat, BridgeMaterial.SKELETON_SKULL); // TODO: Test...
        }

        // Blue ice.
        BlockFlags.setFlagsAs("BLUE_ICE", Material.ICE);
        BlockFlags.addFlags("BLUE_ICE",BlockFlags.F_BLUE_ICE);
        BlockProperties.setBlockProps("BLUE_ICE", new BlockProps(BlockProperties.woodPickaxe, 2.8f));

        // Wet sponge.
        BlockInit.setAs("WET_SPONGE", Material.SPONGE);

        // Red sand.
        BlockInit.setAs("RED_SAND", Material.SAND);
        
        // Sea Grass.
        BlockInit.setAs("SEAGRASS", Material.SEAGRASS);
        BlockInit.setAs("TALL_SEAGRASS", Material.TALL_SEAGRASS);

        // Sandstone slabs.
        BlockProperties.setBlockProps("SANDSTONE_SLAB", BlockProperties.stoneTypeII);
        BlockProperties.setBlockProps("RED_SANDSTONE_SLAB", BlockProperties.stoneTypeII);

        // More sandstone.
        BlockInit.setAs("SMOOTH_SANDSTONE", Material.SANDSTONE);
        BlockProperties.setBlockProps("SMOOTH_SANDSTONE", BlockProperties.stoneTypeII);
        BlockInit.setAs("SMOOTH_RED_SANDSTONE", Material.SANDSTONE);
        BlockProperties.setBlockProps("SMOOTH_RED_SANDSTONE", BlockProperties.stoneTypeII);
        BlockInit.setAs("CUT_SANDSTONE", Material.SANDSTONE);
        BlockInit.setAs("CUT_RED_SANDSTONE", Material.SANDSTONE);
        BlockInit.setAs("CHISELED_SANDSTONE", Material.SANDSTONE);
        BlockInit.setAs("CHISELED_RED_SANDSTONE", Material.SANDSTONE);

        // More brick slabs.
        BlockInit.setAs("COBBLESTONE_SLAB", BridgeMaterial.BRICK_SLAB);
        BlockInit.setAs("STONE_BRICK_SLAB", BridgeMaterial.BRICK_SLAB);
        BlockInit.setAs("NETHER_BRICK_SLAB", BridgeMaterial.BRICK_SLAB);
        BlockProperties.setBlockProps("PRISMARINE_BRICK_SLAB", BlockProperties.stoneTypeI);

        // More slabs.
        BlockProperties.setBlockProps("PRISMARINE_SLAB", BlockProperties.stoneTypeI);
        BlockInit.setAs("DARK_PRISMARINE_SLAB", "PRISMARINE_SLAB");
        BlockInit.setAs("QUARTZ_SLAB", BridgeMaterial.STONE_SLAB); // TODO: Test.
        BlockInit.setAs("PETRIFIED_OAK_SLAB", BridgeMaterial.STONE_SLAB); // TODO: Test.

        // More bricks.
        BlockInit.setAs("PRISMARINE_BRICKS", BridgeMaterial.BRICKS);
        BlockProperties.setBlockProps("PRISMARINE_BRICKS", BlockProperties.stoneTypeI);
        BlockInit.setAs("MOSSY_STONE_BRICKS", "PRISMARINE_BRICKS");
        BlockInit.setAs("CHISELED_STONE_BRICKS", "PRISMARINE_BRICKS");
        BlockInit.setAs("CRACKED_STONE_BRICKS", "PRISMARINE_BRICKS");

        // More brick stairs.
        BlockInit.setAs("PRISMARINE_BRICK_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockInit.setAs("PRISMARINE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockInit.setAs("DARK_PRISMARINE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);

        // More cobblestone walls.
        BlockInit.setAs("MOSSY_COBBLESTONE_WALL", BridgeMaterial.COBBLESTONE_WALL);

        // Dark prismarine.
        BlockInit.setAs("DARK_PRISMARINE", "PRISMARINE");

        // More anvil.
        BlockInit.setAs("DAMAGED_ANVIL", "ANVIL");
        BlockInit.setAs("CHIPPED_ANVIL", "ANVIL");

        // Carved pumpkin.
        BlockInit.setAs("CARVED_PUMPKIN", Material.PUMPKIN);

        // Mushroom stem: via MaterialUtil collection.

        // More quartz blocks.
        BlockInit.setAs("SMOOTH_QUARTZ", "SMOOTH_SANDSTONE");
        BlockInit.setAs("CHISELED_QUARTZ_BLOCK", "QUARTZ_BLOCK");

        // Quartz pillar.
        BlockInit.setPropsAs("QUARTZ_PILLAR", "QUARTZ_BLOCK");

        // Dried kelp block.
        BlockProperties.setBlockProps("DRIED_KELP_BLOCK", new BlockProps(BlockProperties.noTool, 0.5f));

        // Conduit.
        // 1.18 - conduit was assigned to the pickaxe in 1.18
        if (ServerVersion.compareMinecraftVersion("1.18") >= 0) {
            BlockProperties.setBlockProps(Material.CONDUIT, new BlockProperties.BlockProps(BlockProperties.stonePickaxe, 3.0f));        
        }
        // Pre 1.18
        else BlockProperties.setBlockProps("CONDUIT", new BlockProps(BlockProperties.noTool, 3f));
        
        // Sea Pickle.
        BlockProperties.setBlockProps("SEA_PICKLE", BlockProperties.instantType);
        BlockFlags.addFlags("SEA_PICKLE", BlockFlags.F_GROUND | BlockFlags.F_GROUND_HEIGHT);

        // Turtle egg.
        BlockProperties.setBlockProps("TURTLE_EGG", new BlockProps(BlockProperties.noTool, 0.5f));

        // Farm land. (Just in case not having multiversion plugin installed)
        BlockFlags.removeFlags(BridgeMaterial.FARMLAND, BlockFlags.F_HEIGHT100);
        BlockFlags.addFlags(BridgeMaterial.FARMLAND, BlockFlags.F_XZ100 | BlockFlags.F_MIN_HEIGHT16_15);
        
        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.13 blocks.");
    }

}
