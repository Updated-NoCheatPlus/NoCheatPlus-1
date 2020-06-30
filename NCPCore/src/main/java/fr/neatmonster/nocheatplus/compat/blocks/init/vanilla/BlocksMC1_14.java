package fr.neatmonster.nocheatplus.compat.blocks.init.vanilla;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

public class BlocksMC1_14 implements BlockPropertiesSetup{

	public BlocksMC1_14() {
        BlockInit.assertMaterialExists("LECTERN");
        BlockInit.assertMaterialExists("STONECUTTER");
    }
	
	@SuppressWarnings("deprecation")
	@Override
	public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
		// TODO Auto-generated method stub
		BlockFlags.addFlags("VINE", BlockProperties.F_CLIMBUPABLE);
		final long stepFlags = BlockFlags.SOLID_GROUND | BlockProperties.F_XZ100 | BlockProperties.F_GROUND_HEIGHT;
		final long wall = BlockProperties.F_VARIABLE | BlockProperties.F_GROUND | BlockProperties.F_HEIGHT150 | BlockProperties.F_THICK_FENCE2;
		final BlockProperties.BlockProps instant = BlockProperties.instantType;
		BlockInit.setPropsAs("END_STONE_BRICK_WALL", BridgeMaterial.END_STONE_BRICKS);
		BlockFlags.addFlags("END_STONE_BRICK_WALL", wall);
		BlockInit.setPropsAs("END_STONE_BRICK_STAIRS", BridgeMaterial.END_STONE_BRICKS);
		BlockFlags.setFlagsAs("END_STONE_BRICK_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setPropsAs("END_STONE_BRICK_SLAB", BridgeMaterial.END_STONE_BRICKS);
		BlockFlags.addFlags("END_STONE_BRICK_SLAB", stepFlags);
		
		BlockInit.setPropsAs("SANDSTONE_WALL", Material.SANDSTONE);
		BlockFlags.addFlags("SANDSTONE_WALL", wall);
		BlockInit.setPropsAs("SANDSTONE_STAIRS", Material.SANDSTONE);
		BlockFlags.setFlagsAs("SANDSTONE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setPropsAs("SMOOTH_SANDSTONE_SLAB", Material.SANDSTONE);
		BlockFlags.addFlags("SMOOTH_SANDSTONE_SLAB", stepFlags);
		BlockInit.setPropsAs("CUT_SANDSTONE_SLAB", Material.SANDSTONE);
		BlockFlags.addFlags("CUT_SANDSTONE_SLAB", stepFlags);
		BlockInit.setPropsAs("SMOOTH_SANDSTONE_STAIRS", Material.SANDSTONE);
		BlockFlags.setFlagsAs("SMOOTH_SANDSTONE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setPropsAs("RED_SANDSTONE_WALL", Material.SANDSTONE);
		BlockFlags.addFlags("RED_SANDSTONE_WALL", wall);
		BlockInit.setPropsAs("RED_SANDSTONE_STAIRS", Material.SANDSTONE);
		BlockFlags.setFlagsAs("RED_SANDSTONE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setPropsAs("SMOOTH_RED_SANDSTONE_STAIRS", Material.SANDSTONE);
		BlockFlags.setFlagsAs("SMOOTH_RED_SANDSTONE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setPropsAs("SMOOTH_RED_SANDSTONE_SLAB", Material.SANDSTONE);
		BlockFlags.addFlags("SMOOTH_RED_SANDSTONE_SLAB", stepFlags);
		BlockInit.setPropsAs("CUT_RED_SANDSTONE_SLAB", Material.SANDSTONE);
		BlockFlags.addFlags("CUT_RED_SANDSTONE_SLAB", stepFlags);
		
		BlockInit.setAs("RED_NETHER_BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("RED_NETHER_BRICK_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("RED_NETHER_BRICK_SLAB", BridgeMaterial.STONE_SLAB);
		BlockFlags.addFlags("RED_NETHER_BRICK_SLAB", stepFlags);
		BlockInit.setAs("NETHER_BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("MOSSY_STONE_BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("MOSSY_STONE_BRICK_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("MOSSY_STONE_BRICK_SLAB", BridgeMaterial.STONE_SLAB);
		BlockFlags.addFlags("MOSSY_STONE_BRICK_SLAB", stepFlags);
		BlockInit.setAs("STONE_BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("MOSSY_COBBLESTONE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("MOSSY_COBBLESTONE_SLAB", BridgeMaterial.STONE_SLAB);
		BlockFlags.addFlags("MOSSY_COBBLESTONE_SLAB", stepFlags);
		BlockInit.setAs("PRISMARINE_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("GRANITE_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("GRANITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("POLISHED_GRANITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("GRANITE_SLAB", BridgeMaterial.STONE_SLAB);
		BlockInit.setAs("POLISHED_GRANITE_SLAB", BridgeMaterial.STONE_SLAB);
		BlockFlags.addFlags("GRANITE_SLAB", stepFlags);
		BlockFlags.addFlags("POLISHED_GRANITE_SLAB", stepFlags);
		BlockInit.setAs("DIORITE_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("DIORITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("POLISHED_DIORITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("DIORITE_SLAB", BridgeMaterial.STONE_SLAB);
		BlockInit.setAs("POLISHED_DIORITE_SLAB", BridgeMaterial.STONE_SLAB);
		BlockFlags.addFlags("DIORITE_SLAB", stepFlags);
		BlockFlags.addFlags("POLISHED_DIORITE_SLAB", stepFlags);
		BlockInit.setAs("ANDESITE_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("ANDESITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("POLISHED_ANDESITE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("ANDESITE_SLAB", BridgeMaterial.STONE_SLAB);
		BlockInit.setAs("POLISHED_ANDESITE_SLAB", BridgeMaterial.STONE_SLAB);
		BlockFlags.addFlags("ANDESITE_SLAB", stepFlags);
		BlockFlags.addFlags("POLISHED_ANDESITE_SLAB", stepFlags);
		BlockInit.setAs("BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
		BlockInit.setAs("STONE_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("SMOOTH_QUARTZ_STAIRS", BridgeMaterial.STONE_BRICK_STAIRS);
		BlockInit.setAs("SMOOTH_STONE_SLAB", BridgeMaterial.STONE_SLAB);
		BlockInit.setAs("SMOOTH_QUARTZ_SLAB", BridgeMaterial.STONE_SLAB);
		BlockFlags.addFlags("SMOOTH_STONE_SLAB", stepFlags);
		BlockFlags.addFlags("SMOOTH_QUARTZ_SLAB", stepFlags);

		BlockInit.setAs("LOOM", BridgeMaterial.CRAFTING_TABLE);
		BlockInit.setAs("FLETCHING_TABLE", BridgeMaterial.CRAFTING_TABLE);
		BlockInit.setAs("SMITHING_TABLE", BridgeMaterial.CRAFTING_TABLE);
		BlockInit.setAs("CARTOGRAPHY_TABLE", BridgeMaterial.CRAFTING_TABLE);
		BlockInit.setAs("JIGSAW", BridgeMaterial.COMMAND_BLOCK);		
		BlockInit.setAs("BLAST_FURNACE", Material.FURNACE);
		BlockInit.setAs("SMOKER", Material.FURNACE);

		BlockProperties.setBlockProps("COMPOSTER", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.7f, BlockProperties.secToMs(0.9, 0.4, 0.2, 0.15, 0.1, 0.05, 0.05)));
		BlockFlags.addFlags("COMPOSTER", BlockFlags.SOLID_GROUND | BlockProperties.F_GROUND_HEIGHT | BlockProperties.F_MIN_HEIGHT8_1);

		BlockInit.setPropsAs("LECTERN", Material.OAK_PLANKS);
		BlockFlags.addFlags("LECTERN", BlockProperties.F_MIN_HEIGHT8_1 | BlockProperties.F_GROUND_HEIGHT | BlockFlags.SOLID_GROUND);

		BlockInit.setAs("BARREL", Material.OAK_PLANKS);

        BlockProperties.setBlockProps("SCAFFOLDING", instant);
        BlockFlags.addFlags("SCAFFOLDING", 
        BlockProperties.F_IGN_PASSABLE | BlockProperties.F_CLIMBABLE | BlockProperties.F_GROUND | BlockProperties.F_GROUND_HEIGHT | BlockProperties.F_XZ100);

		BlockProperties.setBlockProps("STONECUTTER", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 6.0f, BlockProperties.secToMs(17.0, 2.6, 1.3, 0.85, 0.65, 0.55, 0.4)));
		BlockFlags.addFlags("STONECUTTER", BlockProperties.F_MIN_HEIGHT16_9 | BlockProperties.F_GROUND_HEIGHT | BlockFlags.SOLID_GROUND);

		BlockProperties.setBlockProps("BAMBOO", new BlockProperties.BlockProps(BlockProperties.woodAxe, 1f, BlockProperties.secToMs(1.45, 0.7, 0.35, 0.2, 0.15, 0.1, 0.05)));
		BlockProperties.setBlockProps("BAMBOO_SAPLING", new BlockProperties.BlockProps(BlockProperties.noTool, 0.7f, BlockProperties.secToMs(1.25)));
		BlockFlags.addFlags("BAMBOO", BlockProperties.F_GROUND | BlockProperties.F_GROUND_HEIGHT);
		BlockFlags.addFlags("BAMBOO_SAPLING", BlockProperties.F_IGN_PASSABLE);

		BlockFlags.addFlags("WITHER_ROSE", BlockProperties.F_IGN_PASSABLE);
		BlockProperties.setBlockProps("WITHER_ROSE", instant);		
		BlockFlags.addFlags("CORNFLOWER", BlockProperties.F_IGN_PASSABLE);
		BlockProperties.setBlockProps("CORNFLOWER", instant);
		BlockFlags.addFlags("LILY_OF_THE_VALLEY", BlockProperties.F_IGN_PASSABLE);
		BlockProperties.setBlockProps("LILY_OF_THE_VALLEY", instant);

		BlockInit.setAs("ACACIA_WALL_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("BIRCH_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("BIRCH_WALL_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("DARK_OAK_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("DARK_OAK_WALL_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("JUNGLE_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("JUNGLE_WALL_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("OAK_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("OAK_WALL_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("SPRUCE_SIGN", BridgeMaterial.SIGN);
		BlockInit.setAs("SPRUCE_WALL_SIGN", BridgeMaterial.SIGN);

		BlockInit.setPropsAs("GRINDSTONE", Material.COBBLESTONE);
		BlockFlags.addFlags("GRINDSTONE", BlockFlags.SOLID_GROUND | BlockProperties.F_GROUND_HEIGHT);

		BlockInit.setPropsAs("CAMPFIRE", Material.OAK_PLANKS);
		BlockFlags.addFlags("CAMPFIRE", BlockFlags.SOLID_GROUND | BlockProperties.F_MIN_HEIGHT16_7 | BlockProperties.F_GROUND_HEIGHT | BlockProperties.F_XZ100);

		BlockFlags.addFlags("BELL", BlockProperties.F_GROUND_HEIGHT | BlockProperties.F_GROUND);
		BlockProperties.setBlockProps("BELL", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 5f, BlockProperties.secToMs(24.0, 3.7, 1.85, 1.2, 0.9, 0.8, 0.6)));

		BlockProperties.setBlockProps("LANTERN", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 6.0f, BlockProperties.secToMs(17.0, 2.6, 1.3, 0.85, 0.7, 0.55, 0.4)));
		BlockFlags.addFlags("LANTERN", BlockProperties.F_GROUND | BlockProperties.F_GROUND_HEIGHT);

		BlockFlags.addFlags("SWEET_BERRY_BUSH", BlockProperties.F_COBWEB | BlockProperties.F_COBWEB2);
		StaticLog.logInfo("Added block-info for Minecraft 1.14 blocks.");
	}

}
