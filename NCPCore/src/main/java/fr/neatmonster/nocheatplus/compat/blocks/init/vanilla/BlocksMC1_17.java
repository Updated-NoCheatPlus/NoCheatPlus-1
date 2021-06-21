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

        final long wall = BlockProperties.F_VARIABLE | BlockProperties.F_GROUND | BlockProperties.F_HEIGHT150 | BlockProperties.F_THICK_FENCE2;

        // Deepslate ores + Copper ore
        BlockInit.setPropsAs("DEEPSLATE_IRON_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_COPPER_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_GOLD_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_REDSTONE_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_EMERALD_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_LAPIS_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_DIAMOND_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockProperties.setBlockProps("COPPER_ORE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.0f, BlockProperties.secToMs(15, 7.5, 1.15, 0.75, 0.6, 0.5, 1.25)));

        // Raw metal blocks
        // These are the wrong timings
        BlockProperties.setBlockProps("RAW_IRON_ORE", BlockProperties.diamondBlockType);
        BlockProperties.setBlockProps("RAW_COPPER_ORE", BlockProperties.diamondBlockType);
        BlockProperties.setBlockProps("RAW_GOLD_ORE", BlockProperties.diamondBlockType);

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
        BlockInit.setAs("COBBLED_DEEPSLATE_WALL", Material.DEEPSLATE);
        BlockFlags.addFlags("COBBLED_DEEPSLATE_WALL", wall);
        BlockInit.setAs("POLISHED_DEEPSLATE_WALL", Material.DEEPSLATE);
        BlockFlags.addFlags("POLISHED_DEEPSLATE_WALL", wall);
        BlockInit.setAs("DEEPSLATE_BRICK_WALL", Material.DEEPSLATE);
        BlockFlags.addFlags("DEEPSLATE_BRICK_WALL", wall);
        BlockInit.setAs("DEEPSLATE_TILE_WALL", Material.DEEPSLATE);
        BlockFlags.addFlags("DEEPSLATE_TILE_WALL", wall);

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

        // Waxed Copper stairs
        BlockInit.setPropsAs("WAXED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXED_EXPOSED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXED_WEATHERED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXED_OXIDIZED_CUT_COPPER_STAIRS", Material.COPPER_BLOCK);
        // Waxed Copper slabs
        BlockInit.setPropsAs("WAXED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXED_EXPOSED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXED_WEATHERED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);
        BlockInit.setPropsAs("WAXED_OXIDIZED_CUT_COPPER_SLAB", Material.COPPER_BLOCK);

        // Flower stuffAzalea
        // (flowering_)azalea leaves should be automated in MaterialUtil
        //BlockFlags.addFlags("MOSS_CARPET", BlockProperties.F_CARPET); //This might be automated bc _carpet
        BlockProperties.setBlockProps("AZALEA", BlockProperties.instantType);
        BlockFlags.addFlags("AZALEA", BlockProperties.F_IGN_PASSABLE | BlockProperties.F_GROUND); //not figuring out shape
        BlockProperties.setBlockProps("FLOWERING_AZALEA", BlockProperties.instantType); //not figuring out shape
        BlockFlags.addFlags("FLOWERING_AZALEA", BlockProperties.F_IGN_PASSABLE | BlockProperties.F_GROUND); //not figuring out shape
        BlockFlags.addFlags("BIG_DRIPLEAF", BlockProperties.F_IGN_PASSABLE | BlockProperties.F_GROUND); //not figuring out shape
        BlockProperties.setBlockProps("BIG_DRIPLEAF", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
        BlockFlags.addFlags("SMALL_DRIPLEAF", BlockProperties.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("SMALL_DRIPLEAF", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
        BlockFlags.addFlags("BIG_DRIPLEAF_STEM", BlockProperties.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("BIG_DRIPLEAF_STEM", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
        BlockProperties.setBlockProps("GLOW_LICHEN", BlockProperties.instantType);
        // BlockFlags.addFlags("GLOW_LICHEN", BlockProperties.F_CLIMBABLE); Not climbable
        BlockProperties.setBlockProps("CAVE_VINES", BlockProperties.instantType);
        BlockFlags.addFlags("CAVE_VINES", BlockProperties.F_CLIMBABLE);
        BlockProperties.setBlockProps("CAVE_VINES_PLANT", BlockProperties.instantType);
        BlockFlags.addFlags("CAVE_VINES", BlockProperties.F_CLIMBABLE);
        BlockProperties.setBlockProps("SPORE_BLOSSOM", BlockProperties.instantType);
        BlockProperties.setBlockProps("HANGING_ROOTS", BlockProperties.instantType);
        BlockFlags.addFlags("ROOTED_DIRT", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("ROOTED_DIRT", new BlockProperties.BlockProps(BlockProperties.noTool, 0.5f, BlockProperties.secToMs(0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)));

        //Dripstone
        BlockFlags.addFlags("DRIPSTONE_BLOCK", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("DRIPSTONE_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        BlockFlags.addFlags("POINTED_DRIPSTONE", BlockProperties.F_GROUND | BlockProperties.F_IGN_PASSABLE); //not figuring out shape
        BlockProperties.setBlockProps("POINTED_DRIPSTONE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));

        //Amethyst buds - Need models because they are 0.5 of a block x size so you can stand at the edge of a block and not be on a bud
		BlockFlags.addFlags("SMALL_AMETHYST_BUD", BlockProperties.F_IGN_PASSABLE); // 0.1875
        BlockProperties.setBlockProps("SMALL_AMETHYST_BUD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(2.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        BlockFlags.addFlags("MEDIUM_AMETHYST_BUD", BlockProperties.F_IGN_PASSABLE); // 0.25
        BlockProperties.setBlockProps("MEDIUM_AMETHYST_BUD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(2.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        BlockFlags.addFlags("LARGE_AMETHYST_BUD", BlockProperties.F_IGN_PASSABLE); // 0.3125
        BlockProperties.setBlockProps("LARGE_AMETHYST_BUD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(2.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        BlockFlags.addFlags("AMETHYST_CLUSTER", BlockProperties.F_IGN_PASSABLE); // 0.4375
        BlockProperties.setBlockProps("AMETHYST_CLUSTER", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(2.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        //Amethyst blocks
        BlockFlags.addFlags("BUDDING_AMETHYST", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("BUDDING_AMETHYST", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 3.75, 1.9, 1.25, 0.95, 0.85, 0.65)));
        BlockFlags.addFlags("AMETHYST_BLOCK", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("AMETHYST_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(2.25, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));

        //Candles - So sea pickles with mining times of carpet. Needs model
        for (Material mat : MaterialUtil.CANDLES) {
            BlockFlags.addFlags(mat, BlockProperties.F_GROUND | BlockProperties.F_IGN_PASSABLE);
            BlockProperties.setBlockProps(mat, new BlockProps(BlockProperties.noTool, 0.1f, BlockProperties.secToMs(0.15, 0.15, 0.15, 0.15, 0.15, 0.15, 0.15)));
        }

        //Candle cakes - needs model
        for (Material mat : MaterialUtil.CANDLE_CAKES) {
            BlockFlags.addFlags(mat, BlockProperties.F_IGN_PASSABLE | BlockProperties.F_GROUND);
            BlockProperties.setBlockProps(mat, new BlockProps(BlockProperties.noTool, 0.5f, BlockProperties.secToMs(0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75)));
        }

        //Other blocks
        BlockFlags.addFlags("CALCITE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("CALCITE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 0.75f, BlockProperties.secToMs(3.75, 0.6, 0.3, 0.2, 0.15, 0.15, 0.1)));
        BlockFlags.addFlags("TUFF", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("TUFF", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, BlockProperties.secToMs(7.5, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));
        BlockFlags.addFlags("POWDER_SNOW", BlockProperties.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("POWDER_SNOW", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 0.25f, BlockProperties.secToMs(0.4)));
        BlockInit.setPropsAs("POWDER_SNOW_CAULDRON", Material.CAULDRON); //Need to apply cauldren model or flags when inside.
        BlockInit.setAs("TINTED_GLASS", "GLASS");
        BlockFlags.addFlags("MOSS_BLOCK", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("MOSS_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodHoe, 0.1f, BlockProperties.secToMs(0.15, 0.1, 0.05, 0.05, 0.05, 0.05, 0.05)));
        BlockProperties.setBlockProps("GLOW_ITEM_FRAME", BlockProperties.instantType);
        BlockFlags.addFlags("LIGHTNING_ROD", BlockProperties.F_GROUND | BlockProperties.F_IGN_PASSABLE); // APPLY END_ROD MODEL, these flags seem okay actually.
        BlockProperties.setBlockProps("LIGHTNING_ROD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.0f, BlockProperties.secToMs(15.0, 7.5, 1.15, 0.75, 0.6, 0.5, 1.25)));
        BlockInit.setAs("SMOOTH_BASALT", BridgeMaterial.TERRACOTTA); //This is a guess
        BlockFlags.addFlags("SCULK_SENSOR", BlockProperties.F_GROUND | BlockProperties.F_IGN_PASSABLE); // Need to find hight,if this is even in the game
        BlockProperties.setBlockProps("SCULK_SENSOR", new BlockProperties.BlockProps(BlockProperties.woodHoe, 1.5f, BlockProperties.secToMs(2.25, 1.15, 0.6, 0.4, 0.3, 0.25, 0.2)));

        //Chains can now be vertical!

        StaticLog.logInfo("Added block-info for Minecraft 1.17 blocks.");
	}
}
