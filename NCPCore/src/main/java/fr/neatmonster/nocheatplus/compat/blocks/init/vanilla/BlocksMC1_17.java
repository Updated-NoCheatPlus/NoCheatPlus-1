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
        BlockInit.assertMaterialExists("DEEPSLATE");
    }
	@SuppressWarnings("deprecation")
	@Override
	public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {

        // Deepslate ores + Copper ore
        BlockInit.setPropsAs("DEEPSLATE_IRON_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_COPPER_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_GOLD_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_REDSTONE_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_EMERALD_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_LAPIS_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_DIAMOND_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockProperties.setBlockProps("COPPER_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.0f, BlockProperties.secToMs(15, 7.5, 1.15, 0.75, 0.6, 0.5, 1.25)));

        // Raw metal blocks - They do not need BlockFlags.FULLY_SOLID_BOUNDS, it's in MaterialUtil
        // none of these mining times were added to the wiki yet so instant for now
        BlockProperties.setBlockProps("RAW_IRON_ORE", BlockProperties.instantType);
        BlockProperties.setBlockProps("RAW_COPPER_ORE", BlockProperties.instantType);
        BlockProperties.setBlockProps("RAW_GOLD_ORE", BlockProperties.instantType);

        //Deepslate blocks
        BlockFlags.addFlags("DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("DEEPSLATE", Material.DEEPSLATE);

        BlockFlags.addFlags("COBBLED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("COBBLED_DEEPSLATE", Material.DEEPSLATE);

        BlockFlags.addFlags("POLISHED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE", Material.DEEPSLATE);

        BlockFlags.addFlags("DEEPSLATE_BRICKS", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("DEEPSLATE_BRICK", Material.DEEPSLATE);

        BlockFlags.addFlags("CRACKED_DEEPSLATE_BRICKS", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("CRACKED_DEEPSLATE_BRICK", Material.DEEPSLATE);

        BlockFlags.addFlags("DEEPSLATE_TILES", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("DEEPSLATE_TILES", Material.DEEPSLATE);

        BlockFlags.addFlags("CRACKED_DEEPSLATE_TILES", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("CRACKED_DEEPSLATE_TILES", Material.DEEPSLATE);

        BlockFlags.addFlags("CHISELED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("CHISELED_DEEPSLATE", Material.DEEPSLATE);

        //Deepslate walls
        BlockInit.setAs("COBBLED_DEEPSLATE_WALL", BridgeMaterial.COBBLESTONE_WALL);
        BlockInit.setAs("POLISHED_DEEPSLATE_WALL", BridgeMaterial.COBBLESTONE_WALL);
        BlockInit.setAs("DEEPSLATE_BRICK_WALL", BridgeMaterial.COBBLESTONE_WALL);
        BlockInit.setAs("DEEPSLATE_TILE_WALL", BridgeMaterial.COBBLESTONE_WALL);

        //Deepslate stairs
        BlockInit.setPropsAs("COBBLED_DEEPSLATE_STAIRS", Material.DEEPSLATE);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE_STAIRS", Material.DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_BRICK_STAIRS", Material.DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_TILE_STAIRS", Material.DEEPSLATE);

        //Deepslate slabs
        BlockInit.setPropsAs("COBBLED_DEEPSLATE_SLAB", Material.DEEPSLATE);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE_SLAB", Material.DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_BRICK_SLAB", Material.DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_TILE_SLAB", Material.DEEPSLATE);

        // Copper Blocks
        for (Material mat : MaterialUtil.COPPER_BLOCKS) {
            BlockFlags.addFlags(mat, BlockFlags.FULLY_SOLID_BOUNDS);
            BlockInit.setAs(mat, Material.COPPER_BLOCK);
        }

        // Copper stairs
        BlockInit.setPropsAs("CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("EXPOSED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WEATHERED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("OXIDIZED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        // Copper slabs
        BlockInit.setPropsAs("CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("EXPOSED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WEATHERED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("OXIDIZED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);

        // Waxxed Copper stairs
        BlockInit.setPropsAs("WAXXED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXXED_EXPOSED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXXED_WEATHERED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXXED_OXIDIZED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        // Waxxed Copper slabs
        BlockInit.setPropsAs("WAXXED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXXED_EXPOSED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXXED_WEATHERED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXXED_OXIDIZED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);

        // Flower stuffAzalea
        // (flowering_)azalea leaves should be automated in MaterialUtil
        //BlockFlags.addFlags("MOSS_CARPET", BlockProperties.F_CARPET); //This might be automated bc _carpet
        BlockProperties.setBlockProps("AZALEA", BlockProperties.instantType);
        BlockProperties.setBlockProps("FLOWERING_AZALEA", BlockProperties.instantType);
		BlockProperties.setBlockProps("FLOWERING_AZALEA", BlockProperties.instantType); //not figuring out shape
        BlockFlags.addFlags("BIG_DRIPLEAF", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("BIG_DRIPLEAF", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
        BlockFlags.addFlags("SMALL_DRIPLEAF", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("SMALL_DRIPLEAF", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
        BlockFlags.addFlags("BIG_DRIPLEAF_STEM", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("BIG_DRIPLEAF_STEM", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
        BlockProperties.setBlockProps("GLOW_LICHEN", BlockProperties.instantType);
        BlockFlags.addFlags("GLOW_LICHEN", BlockProperties.F_CLIMBABLE);
        BlockProperties.setBlockProps("CAVE_VINES", BlockProperties.instantType);
        BlockProperties.setBlockProps("CAVE_VINES_PLANT", BlockProperties.instantType);

        //Dripstone
        BlockFlags.addFlags("DRIPSTONE_BLOCK", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DRIPSTONE_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        BlockFlags.addFlags("POINTED_DRIPSTONE", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("POINTED_DRIPSTONE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));

        //Amethyst - this is basically like snow, sizes should be in block prop flags but too lazy to check them now
        BlockFlags.addFlags("SMALL_AMETHYST_BUD", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockFlags.addFlags("MEDIUM_AMETHYST_BUD", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockFlags.addFlags("LARGE_AMETHYST_BUD", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockFlags.addFlags("AMETHYST_CLUSTER", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("AMETHYST_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(2.25, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));

        //Candles - So sea pickles with mining times of carpet
        for (Material mat : MaterialUtil.CANDELS) {
            BlockInit.setPropsAs("CANDLE", Material.SEA_PICKLE);
            BlockProperties.setBlockProps(mat, new BlockProps(BlockProperties.noTool, 0.1f, BlockProperties.secToMs(0.15, 0.15, 0.15, 0.15, 0.15, 0.15, 0.15)));
        }

        //Colored cakes
        for (Material mat : MaterialUtil.COLORED_CAKES) {
            BlockFlags.addFlags(mat, BlockProperties.F_IGN_PASSABLE | BlockProperties.F_GROUND_HEIGHT);
            BlockProperties.setBlockProps(mat, new BlockProps(BlockProperties.noTool, 0.5f, BlockProperties.secToMs(0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)));
        }

        //Other blocks
        BlockFlags.addFlags("CALCITE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("CALCITE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 0.75f, BlockProperties.secToMs(3.75, 0.6, 0.3, 0.2, 0.15, 0.15, 0.1)));
        BlockFlags.addFlags("TUFF", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("TUFF", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        BlockFlags.addFlags("POWDER_SNOW", BlockProperties.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("POWDER_SNOW", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 0.25f, BlockProperties.secToMs(0.4)));
        BlockFlags.addFlags("POWDER_SNOW_CAULDRON", BlockProperties.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("POWDER_SNOW_CAULDRON", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 0.25f, BlockProperties.secToMs(0.4)));
        BlockFlags.addFlags("CANDLE_CAKE", BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("TINTED_GLASS", new BlockProperties.BlockProps(BlockProperties.noTool, 0.3f, BlockProperties.secToMs(0.45)));
        BlockProperties.setBlockProps("MOSS_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodHoe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
		//Chains can now be vertical!

        StaticLog.logInfo("Added block-info for Minecraft 1.17 blocks.");
	}
}
