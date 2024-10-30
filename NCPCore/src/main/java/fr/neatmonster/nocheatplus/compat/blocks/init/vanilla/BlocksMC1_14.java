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
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

public class BlocksMC1_14 implements BlockPropertiesSetup{

    public BlocksMC1_14() {
        BlockInit.assertMaterialExists("LECTERN");
        BlockInit.assertMaterialExists("STONECUTTER");
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        // TODO: Clean up the mess

        final BlockProperties.BlockProps instant = BlockProperties.instantType;
        BlockInit.setPropsAs("END_STONE_BRICK_WALL", BridgeMaterial.END_STONE_BRICKS);
        BlockInit.setPropsAs("END_STONE_BRICK_STAIRS", BridgeMaterial.END_STONE_BRICKS);
        BlockInit.setPropsAs("END_STONE_BRICK_SLAB", BridgeMaterial.END_STONE_BRICKS);
        
        BlockInit.setPropsAs("SANDSTONE_WALL", Material.SANDSTONE);
        BlockInit.setPropsAs("SANDSTONE_STAIRS", Material.SANDSTONE);
        BlockInit.setPropsAs("SMOOTH_SANDSTONE_SLAB", "SANDSTONE_SLAB");
        BlockInit.setPropsAs("CUT_SANDSTONE_SLAB", "SANDSTONE_SLAB");
        BlockInit.setPropsAs("SMOOTH_SANDSTONE_STAIRS", "SANDSTONE_SLAB");
        BlockInit.setPropsAs("RED_SANDSTONE_WALL", Material.SANDSTONE);
        BlockInit.setPropsAs("RED_SANDSTONE_STAIRS", Material.SANDSTONE);
        BlockInit.setPropsAs("SMOOTH_RED_SANDSTONE_STAIRS", "SANDSTONE_SLAB");
        BlockInit.setPropsAs("SMOOTH_RED_SANDSTONE_SLAB", "SANDSTONE_SLAB");
        BlockInit.setPropsAs("CUT_RED_SANDSTONE_SLAB", "SANDSTONE_SLAB");

        BlockInit.setPropsAs("RED_NETHER_BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);

        BlockInit.setAs("RED_NETHER_BRICK_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockProperties.setBlockProps("RED_NETHER_BRICK_STAIRS", BlockProperties.stoneTypeII);

        BlockInit.setAs("RED_NETHER_BRICK_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setPropsAs("NETHER_BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
        BlockProperties.setBlockProps("MOSSY_STONE_BRICK_WALL", BlockProperties.stoneTypeI);
        BlockInit.setAs("MOSSY_STONE_BRICK_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);

        BlockInit.setAs("MOSSY_STONE_BRICK_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setPropsAs("MOSSY_STONE_BRICK_SLAB", "MOSSY_STONE_BRICK_WALL");

        BlockInit.setPropsAs("STONE_BRICK_WALL", "MOSSY_STONE_BRICK_WALL");

        BlockInit.setAs("MOSSY_COBBLESTONE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockInit.setPropsAs("MOSSY_COBBLESTONE_STAIRS", "RED_NETHER_BRICK_STAIRS");

        BlockInit.setAs("MOSSY_COBBLESTONE_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setPropsAs("PRISMARINE_WALL", "MOSSY_STONE_BRICK_WALL");
        BlockInit.setPropsAs("GRANITE_WALL", "MOSSY_STONE_BRICK_WALL");
        BlockInit.setAs("GRANITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockInit.setAs("POLISHED_GRANITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);

        BlockInit.setAs("GRANITE_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setPropsAs("GRANITE_SLAB", "MOSSY_STONE_BRICK_WALL");
        BlockInit.setAs("POLISHED_GRANITE_SLAB", "GRANITE_SLAB");
        BlockInit.setPropsAs("DIORITE_WALL", "MOSSY_STONE_BRICK_WALL");

        BlockInit.setAs("DIORITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockInit.setAs("POLISHED_DIORITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockInit.setAs("DIORITE_SLAB", "GRANITE_SLAB");

        BlockInit.setAs("POLISHED_DIORITE_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setPropsAs("POLISHED_DIORITE_SLAB", "MOSSY_STONE_BRICK_WALL");
        BlockInit.setPropsAs("ANDESITE_WALL", "MOSSY_STONE_BRICK_WALL");

        BlockInit.setAs("ANDESITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockInit.setAs("POLISHED_ANDESITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);

        BlockInit.setAs("ANDESITE_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setPropsAs("ANDESITE_SLAB", "MOSSY_STONE_BRICK_WALL");

        BlockInit.setAs("POLISHED_ANDESITE_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setPropsAs("POLISHED_ANDESITE_SLAB", "MOSSY_STONE_BRICK_WALL");

        BlockInit.setPropsAs("BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
        BlockInit.setAs("STONE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);

        BlockInit.setAs("SMOOTH_QUARTZ_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
        BlockInit.setPropsAs("SMOOTH_QUARTZ_STAIRS", "RED_NETHER_BRICK_STAIRS");

        BlockInit.setAs("SMOOTH_STONE_SLAB", BridgeMaterial.STONE_SLAB);
        BlockInit.setAs("SMOOTH_QUARTZ_SLAB", BridgeMaterial.STONE_SLAB);

        BlockInit.setAs("LOOM", BridgeMaterial.CRAFTING_TABLE);
        BlockInit.setAs("FLETCHING_TABLE", BridgeMaterial.CRAFTING_TABLE);
        BlockInit.setAs("SMITHING_TABLE", BridgeMaterial.CRAFTING_TABLE);
        BlockInit.setAs("CARTOGRAPHY_TABLE", BridgeMaterial.CRAFTING_TABLE);
        BlockInit.setAs("JIGSAW", BridgeMaterial.COMMAND_BLOCK);        
        BlockInit.setAs("BLAST_FURNACE", Material.FURNACE);
        BlockInit.setAs("SMOKER", Material.FURNACE);

        BlockProperties.setBlockProps("COMPOSTER", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.6f));
        BlockFlags.addFlags("COMPOSTER", BlockFlags.SOLID_GROUND);

        BlockProperties.setBlockProps("LECTERN", new BlockProperties.BlockProps(BlockProperties.woodAxe, 2.5f));
        BlockFlags.setBlockFlags("LECTERN", BlockFlags.SOLID_GROUND);

        BlockInit.setAs("BARREL", Material.OAK_PLANKS);
        BlockInit.setPropsAs("BARREL", "LECTERN");

        BlockProperties.setBlockProps("SCAFFOLDING", instant);
        BlockFlags.addFlags("SCAFFOLDING", 
        BlockFlags.F_IGN_PASSABLE |  BlockFlags.F_CLIMBABLE | BlockFlags.F_GROUND | BlockFlags.F_GROUND_HEIGHT | BlockFlags.F_XZ100);

        BlockProperties.setBlockProps("STONECUTTER", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, true));
        BlockFlags.addFlags("STONECUTTER", BlockFlags.SOLID_GROUND);

        BlockProperties.setBlockProps("BAMBOO", new BlockProperties.BlockProps(BlockProperties.woodAxe, 1f));
        BlockProperties.setBlockProps("BAMBOO_SAPLING", new BlockProperties.BlockProps(BlockProperties.woodSword, 1f));

        BlockFlags.addFlags("BAMBOO", BlockFlags.F_GROUND);
        BlockFlags.addFlags("BAMBOO_SAPLING", BlockFlags.F_IGN_PASSABLE);

        BlockFlags.addFlags("WITHER_ROSE", BlockFlags.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("WITHER_ROSE", instant);        
        BlockFlags.addFlags("CORNFLOWER", BlockFlags.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("CORNFLOWER", instant);
        BlockFlags.addFlags("LILY_OF_THE_VALLEY", BlockFlags.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("LILY_OF_THE_VALLEY", instant);

        // More signs
        for (Material mat : MaterialUtil.WOODEN_SIGNS) {
            BlockInit.setAs(mat, BridgeMaterial.SIGN);
        }

        BlockInit.setPropsAs("GRINDSTONE", Material.COBBLESTONE);
        BlockFlags.addFlags("GRINDSTONE", BlockFlags.SOLID_GROUND | BlockFlags.F_VARIABLE);

        BlockInit.setPropsAs("CAMPFIRE", Material.OAK_PLANKS);
        BlockFlags.addFlags("CAMPFIRE", BlockFlags.SOLID_GROUND);

        BlockFlags.addFlags("BELL", BlockFlags.SOLID_GROUND);
        BlockProperties.setBlockProps("BELL", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 5f, true));

        BlockProperties.setBlockProps("LANTERN", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, true));
        BlockFlags.addFlags("LANTERN", BlockFlags.F_GROUND);

        BlockFlags.addFlags("SWEET_BERRY_BUSH", BlockFlags.F_BERRY_BUSH);
        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.14 blocks.");
    }

}
