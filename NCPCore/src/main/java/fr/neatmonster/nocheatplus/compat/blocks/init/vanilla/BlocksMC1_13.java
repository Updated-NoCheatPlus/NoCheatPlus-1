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
        // Add flag for anvil
        BlockFlags.addFlags("ANVIL", BlockProperties.F_GROUND_HEIGHT | BlockProperties.F_GROUND);
        // Change flag for walls
        BlockFlags.addFlags("COBBLESTONE_WALL", BlockProperties.F_THICK_FENCE2);
        BlockFlags.removeFlags("COBBLESTONE_WALL", BlockProperties.F_THICK_FENCE);
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
            BlockInit.setPropsAs(mat, Material.STONE);
        }

        // Kelp.

        // Fern.

        // Bubble column.
        // TODO: Drag down effect: probably not using velocity.
        BlockInit.setAs("BUBBLE_COLUMN", Material.WATER);

        // Further melon/pumpkin stems.

        // Wall torch
        BlockInit.setInstantPassable("WALL_TORCH");

        // Shulker boxes.
        for (Material mat : MaterialUtil.SHULKER_BOXES) {
            BlockFlags.addFlags(mat, BlockProperties.F_XZ100 | BlockFlags.SOLID_GROUND);
            BlockProperties.setBlockProps(mat, new BlockProps(BlockProperties.woodPickaxe, 2,
                    BlockProperties.secToMs(3.0, 1.45, 0.7, 0.45, 0.3, 0.25, 0.2)));
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

        // Wall heads.
        for (Material mat : MaterialUtil.HEADS_WALL) {
            BlockInit.setAs(mat, BridgeMaterial.SKELETON_SKULL); // TODO: Test...
        }

        // Blue ice.
        BlockInit.setAs("BLUE_ICE", Material.ICE);

        // Wet sponge.
        BlockInit.setAs("WET_SPONGE", Material.SPONGE);

        // Red sand.
        BlockInit.setAs("RED_SAND", Material.SAND);
        
        // Sea Grass.
        BlockInit.setAs("SEAGRASS", Material.SEAGRASS);
        BlockInit.setAs("TALL_SEAGRASS", Material.TALL_SEAGRASS);

        // Sandstone slabs.
        BlockInit.setPropsAs("SANDSTONE_SLAB", Material.SANDSTONE);
        BlockInit.setPropsAs("RED_SANDSTONE_SLAB", Material.SANDSTONE);

        // More sandstone.
        BlockInit.setAs("SMOOTH_SANDSTONE", Material.SANDSTONE);
        BlockInit.setAs("SMOOTH_RED_SANDSTONE", Material.SANDSTONE);
        BlockInit.setAs("CUT_SANDSTONE", Material.SANDSTONE);
        BlockInit.setAs("CUT_RED_SANDSTONE", Material.SANDSTONE);
        BlockInit.setAs("CHISELED_SANDSTONE", Material.SANDSTONE);
        BlockInit.setAs("CHISELED_RED_SANDSTONE", Material.SANDSTONE);

        // More brick slabs.
        BlockInit.setAs("COBBLESTONE_SLAB", BridgeMaterial.BRICK_SLAB);
        BlockInit.setAs("STONE_BRICK_SLAB", BridgeMaterial.BRICK_SLAB);
        BlockInit.setAs("NETHER_BRICK_SLAB", BridgeMaterial.BRICK_SLAB);
        BlockInit.setAs("PRISMARINE_BRICK_SLAB", BridgeMaterial.BRICK_SLAB);

        // More slabs.
        BlockInit.setAs("PRISMARINE_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setAs("DARK_PRISMARINE_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setAs("QUARTZ_SLAB", BridgeMaterial.STONE_SLAB); // TODO: Test.
        BlockInit.setAs("PETRIFIED_OAK_SLAB", BridgeMaterial.STONE_SLAB); // TODO: Test.

        // More bricks.
        BlockInit.setAs("PRISMARINE_BRICKS", BridgeMaterial.BRICKS);
        BlockInit.setAs("MOSSY_STONE_BRICKS", BridgeMaterial.BRICKS);
        BlockInit.setAs("CHISELED_STONE_BRICKS", BridgeMaterial.BRICKS);
        BlockInit.setAs("CRACKED_STONE_BRICKS", BridgeMaterial.BRICKS);

        // More brick stairs.
        BlockInit.setAs("PRISMARINE_BRICK_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);

        // More stairs.
        BlockInit.setAs("PRISMARINE_STAIRS", Material.COBBLESTONE_STAIRS);
        BlockInit.setAs("DARK_PRISMARINE_STAIRS", Material.COBBLESTONE_STAIRS);

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
        BlockInit.setAs("SMOOTH_QUARTZ", "QUARTZ_BLOCK");
        BlockInit.setAs("CHISELED_QUARTZ_BLOCK", "QUARTZ_BLOCK");

        // Quartz pillar.
        BlockInit.setPropsAs("QUARTZ_PILLAR", "QUARTZ_BLOCK");

        // Dried kelp block.
        BlockProperties.setBlockProps("DRIED_KELP_BLOCK", new BlockProps(
                BlockProperties.noTool, 0.5f, BlockProperties.secToMs(0.75)));

        // Conduit.
        BlockProperties.setBlockProps("CONDUIT", BlockProperties.woodDoorType);
        
        // Sea Pickle.
        BlockProperties.setBlockProps("SEA_PICKLE", BlockProperties.instantType);
        BlockFlags.addFlags("SEA_PICKLE", BlockProperties.F_GROUND | BlockProperties.F_GROUND_HEIGHT);

        // Turtle egg.
        BlockProperties.setBlockProps("TURTLE_EGG", new BlockProps(
                BlockProperties.noTool, 0.5f, BlockProperties.secToMs(0.7)));

        BlockProperties.setBlockFlags(Material.HOPPER,
                BlockProperties.F_GROUND | BlockProperties.F_SOLID | BlockProperties.F_XZ100 | BlockProperties.F_GROUND_HEIGHT | BlockProperties.F_IGN_PASSABLE);

        // Grass path. (Just in case not having multiversion plugin installed)
        BlockFlags.removeFlags(Material.GRASS_PATH, BlockProperties.F_HEIGHT100);
        BlockFlags.addFlags(Material.GRASS_PATH,
                BlockProperties.F_XZ100 | BlockProperties.F_MIN_HEIGHT16_15);
        // Farm land. (Just in case not having multiversion plugin installed)
        BlockFlags.removeFlags(BridgeMaterial.FARMLAND, BlockProperties.F_HEIGHT100);
        BlockFlags.addFlags(BridgeMaterial.FARMLAND,
                BlockProperties.F_XZ100 | BlockProperties.F_MIN_HEIGHT16_15);

        StaticLog.logInfo("Added block-info for Minecraft 1.13 blocks.");
    }

}
