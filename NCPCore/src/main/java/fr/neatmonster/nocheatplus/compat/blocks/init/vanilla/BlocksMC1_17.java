package fr.neatmonster.nocheatplus.compat.blocks.init.vanilla;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.config.WorldConfigProvider;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties.BlockProps;

public class BlocksMC1_17 implements BlockPropertiesSetup{
	public BlocksMC1_17() {
        BlockInit.assertMaterialExists("FLOWERING_AZALEA");
        BlockInit.assertMaterialExists("CANDLE");
    }
	@SuppressWarnings("deprecation")
	@Override
	public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {

        // Deepslate ores + Copper ore
        BlockFlags.addFlags("DEEPSLATE_IRON_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE_IRON_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 4.5f, BlockProperties.secToMs(22.5, 11.25, 1.7, 1.15, 0.85, 0.75, 1.9)));

        BlockFlags.addFlags("DEEPSLATE_COPPER_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE_COPPER_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 4.5f, BlockProperties.secToMs(22.5, 11.25, 1.7, 1.15, 0.85, 0.75, 1.9)));

        BlockFlags.addFlags("DEEPSLATE_GOLD_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE_GOLD_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 4.5f, BlockProperties.secToMs(22.5, 11.25, 1.7, 1.15, 0.85, 0.75, 1.9)));

        BlockFlags.addFlags("DEEPSLATE_REDSTONE_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE_REDSTONE_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 4.5f, BlockProperties.secToMs(22.5, 11.25, 1.7, 1.15, 0.85, 0.75, 1.9)));

        BlockFlags.addFlags("DEEPSLATE_EMERALD_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE_EMERALD_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 4.5f, BlockProperties.secToMs(22.5, 11.25, 1.7, 1.15, 0.85, 0.75, 1.9)));

        BlockFlags.addFlags("DEEPSLATE_LAPIS_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE_LAPIS_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 4.5f, BlockProperties.secToMs(22.5, 11.25, 1.7, 1.15, 0.85, 0.75, 1.9)));

        BlockFlags.addFlags("DEEPSLATE_DIAMOND_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE_DIAMOND_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 4.5f, BlockProperties.secToMs(22.5, 11.25, 1.7, 1.15, 0.85, 0.75, 1.9)));

        BlockFlags.addFlags("COPPER_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("COPPER_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.0f, BlockProperties.secToMs(15, 7.5, 1.15, 0.75, 0.6, 0.5, 1.25)));

        // Raw metal blocks
        BlockFlags.addFlags("RAW_IRON_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("RAW_COPPER_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("RAW_GOLD_ORE", BlockFlags.FULLY_SOLID_BOUNDS);
        // none of these mining times were added to the wiki yet

        //Deepslate blocks
        BlockFlags.addFlags("DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.0f, BlockProperties.secToMs(15, 2.25, 1.15, 0.75, 0.6, 0.5, 0.4)));

        BlockFlags.addFlags("COBBLED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, BlockProperties.secToMs(17.5, 2.65, 1.35, 0.9, 0.7, 0.6, 0.45)));

        BlockFlags.addFlags("POLISHED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, BlockProperties.secToMs(17.5, 2.65, 1.35, 0.9, 0.7, 0.6, 0.45)));

        BlockFlags.addFlags("INFESTED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        //TODO: LIKE OTHER INFESTED BLOCKS PROP

        BlockFlags.addFlags("CRACKED_DEEPSLATE_BRICKS", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, BlockProperties.secToMs(17.5, 2.65, 1.35, 0.9, 0.7, 0.6, 0.45)));

        BlockFlags.addFlags("dEEPSLATE_TILES", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, BlockProperties.secToMs(17.5, 2.65, 1.35, 0.9, 0.7, 0.6, 0.45)));

        BlockFlags.addFlags("CRACKED_DEEPSLATE_TILES", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, BlockProperties.secToMs(17.5, 2.65, 1.35, 0.9, 0.7, 0.6, 0.45)));

        BlockFlags.addFlags("CHISELED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DEEPSLATE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, BlockProperties.secToMs(17.5, 2.65, 1.35, 0.9, 0.7, 0.6, 0.45)));

        //Deepslate walls
        BlockInit.setAs("COBBLED_DEEPSLATE_WALL", BridgeMaterial.COBBLESTONE_WALL);
        BlockInit.setAs("POLISHED_DEEPSLATE_WALL", BridgeMaterial.COBBLESTONE_WALL);
        BlockInit.setAs("DEEPSLATE_BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
        BlockInit.setAs("DEEPSLATE_TILE_WALL", BridgeMaterial.COBBLESTONE_WALL);

        //Deepslate stairs - This is a guess, prob not like stone
        BlockInit.setPropsAs("COBBLED_DEEPSLATE_STAIRS", Material.STONE);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE_STAIRS", Material.STONE);
        BlockInit.setPropsAs("DEEPSLATE_BRICK_STAIRS", Material.STONE);
        BlockInit.setPropsAs("DEEPSLATE_TILE_STAIRS", Material.STONE);

        //Deepslate slabs - This is a guess, prob not like stone
        BlockInit.setPropsAs("COBBLED_DEEPSLATE_SLAB", Material.STONE);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE_SLAB", Material.STONE);
        BlockInit.setPropsAs("DEEPSLATE_BRICK_SLAB", Material.STONE);
        BlockInit.setPropsAs("DEEPSLATE_TILE_SLAB", Material.STONE);

        // Copper Blocks - THESE ARE ALL THE SAME MINING TIMES, NOT FILLED OUT
        BlockProperties.setBlockProps("EXPOSED_COPPER", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.0f, BlockProperties.secToMs(15.0, 7.5, 1.15, 0.75, 0.6, 0.5, 1.25)));

        BlockFlags.addFlags("COPPER_BLOCK", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("EXPOSED_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("WEATHERED_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("OXIDIZED_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);

        BlockFlags.addFlags("CUT_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("EXPOSED_CUT_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("WEATHERED_CUT_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("OXIDIZED_CUT_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        // Copper stairs
        BlockInit.setPropsAs("CUT_COPPER_STAIRS", Material.STONE);
        BlockInit.setPropsAs("EXPOSED_CUT_COPPER_STAIRS",, Material.STONE);
        BlockInit.setPropsAs("WEATHERED_CUT_COPPER_STAIRS",, Material.STONE);
        BlockInit.setPropsAs("OXIDIZED_CUT_COPPER_STAIRS", Material.STONE);
        // Copper slabs
        BlockInit.setPropsAs("CUT_COPPER_SLAB", Material.STONE);
        BlockInit.setPropsAs("EXPOSED_CUT_COPPER_SLAB",, Material.STONE);
        BlockInit.setPropsAs("WEATHERED_CUT_COPPER_SLAB",, Material.STONE);
        BlockInit.setPropsAs("OXIDIZED_CUT_COPPER_SLAB", Material.STONE);

        //Waxxed Copper Blocks
        BlockFlags.addFlags("WAXXED_COPPER_BLOCK", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("WAXXED_EXPOSED_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("WWAXXED_EATHERED_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("WAXXED_OXIDIZED_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);

        BlockFlags.addFlags("WAXXED_CUT_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("WAXXED_EXPOSED_CUT_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("WAXXED_WEATHERED_CUT_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockFlags.addFlags("WAXXED_OXIDIZED_CUT_COPPER", BlockFlags.FULLY_SOLID_BOUNDS);
        // Waxxed Copper stairs
        BlockInit.setPropsAs("WAXXED_CUT_COPPER_STAIRS", Material.STONE);
        BlockInit.setPropsAs("WAXXED_EXPOSED_CUT_COPPER_STAIRS",, Material.STONE);
        BlockInit.setPropsAs("WAXXED_WEATHERED_CUT_COPPER_STAIRS",, Material.STONE);
        BlockInit.setPropsAs("WAXXED_OXIDIZED_CUT_COPPER_STAIRS", Material.STONE);
        // Waxxed Copper slabs
        BlockInit.setPropsAs("WAXXED_CUT_COPPER_SLAB", Material.STONE);
        BlockInit.setPropsAs("WAXXED_EXPOSED_CUT_COPPER_SLAB",, Material.STONE);
        BlockInit.setPropsAs("WAXXED_WEATHERED_CUT_COPPER_SLAB",, Material.STONE);
        BlockInit.setPropsAs("WAXXED_OXIDIZED_CUT_COPPER_SLAB", Material.STONE);

        // Flower stuff
        // Add AZALEA leaves to the other leaves somewhere
		BlockProperties.setBlockProps("FLOWERING_AZALEA", BlockProperties.instantType); //not figuring out shape
        BlockInit.setPropsAs("MOSS_CARPET", Material.CARPET);
        BlockFlags.addFlags("BIG_DRIPLEAF", BlockFlags.IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("BIG_DRIPLEAF", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
        BlockProperties.setBlockProps("GLOW_LICHEN", BlockProperties.instantType);
        BlockFlags.addFlags("GLOW_LICHEN", BlockProperties.F_CLIMBABLE);

        //Dripstone
        BlockFlags.addFlags("DRIPSTONE_BLOCK", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DRIPSTONE_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        BlockFlags.addFlags("POINTED_DRIPSTONE", BlockFlags.IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("POINTED_DRIPSTONE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));

        //Amethyst - this is basically like snow, sizes should be in block prop flags but too lazy to check them now
        BlockFlags.addFlags("SMALL_AMETHYST_BUD", BlockFlags.IGN_PASSABLE); //not figuring out shape
        BlockFlags.addFlags("MEDIUM_AMETHYST_BUD", BlockFlags.IGN_PASSABLE); //not figuring out shape
        BlockFlags.addFlags("LARGE_AMETHYST_BUD", BlockFlags.IGN_PASSABLE); //not figuring out shape
        BlockFlags.addFlags("AMETHYST_CLUSTER", BlockFlags.IGN_PASSABLE); //not figuring out shape

        //Candles - So sea pickles, someone else can do all of these with some big brain color variable
		//guessing thsi is right-ish
        BlockInit.setPropsAs("CANDLE", Material.SEA_PICKLE);

        //Other blocks
        BlockFlags.addFlags("CALCITE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("CALCITE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 0.75f, BlockProperties.secToMs(3.75, 0.6, 0.3, 0.2, 0.15, 0.15, 0.1)));
        BlockFlags.addFlags("TUFF", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("TUFF", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));

        StaticLog.logInfo("Added block-info for Minecraft 1.17 blocks.");
	}
}