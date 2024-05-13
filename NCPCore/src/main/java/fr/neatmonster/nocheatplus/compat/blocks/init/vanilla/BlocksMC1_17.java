package fr.neatmonster.nocheatplus.compat.blocks.init.vanilla;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.compat.blocks.BlockPropertiesSetup;
import fr.neatmonster.nocheatplus.compat.blocks.init.BlockInit;
import fr.neatmonster.nocheatplus.config.*;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

public class BlocksMC1_17 implements BlockPropertiesSetup {
    public BlocksMC1_17() {
        BlockInit.assertMaterialExists("DEEPSLATE");
        BlockInit.assertMaterialExists("COBBLED_DEEPSLATE");
        BlockInit.assertMaterialExists("DEEPSLATE_COAL_ORE");
        BlockInit.assertMaterialExists("DEEPSLATE_GOLD_ORE");
        BlockInit.assertMaterialExists("COPPER_BLOCK");
    }

    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        BlockProperties.setBlockProps("LIGHT", BlockProperties.indestructibleType);
        BlockFlags.addFlags("LIGHT", BlockFlags.F_IGN_PASSABLE);

        for (Material mat : MaterialUtil.ALL_CANDLES) {
            BlockFlags.addFlags(mat, BlockFlags.SOLID_GROUND);
            BlockProperties.setBlockProps(mat, new BlockProperties.BlockProps(BlockProperties.noTool, 0.1f));
        }

        BlockProperties.setBlockProps(Material.DEEPSLATE_COAL_ORE, new BlockProperties.BlockProps(BlockProperties.stonePickaxe, 4.5f, true));
        BlockProperties.setBlockProps(Material.DEEPSLATE_GOLD_ORE, new BlockProperties.BlockProps(BlockProperties.ironPickaxe, 4.5f, true));

        // Deepslate ores + Copper ore
        BlockInit.setPropsAs("DEEPSLATE_IRON_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_COPPER_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_REDSTONE_ORE", Material.DEEPSLATE_GOLD_ORE);
        BlockInit.setPropsAs("DEEPSLATE_EMERALD_ORE", Material.DEEPSLATE_GOLD_ORE);
        BlockInit.setPropsAs("DEEPSLATE_LAPIS_ORE", Material.DEEPSLATE_COAL_ORE);
        BlockInit.setPropsAs("DEEPSLATE_DIAMOND_ORE", Material.DEEPSLATE_GOLD_ORE);
        BlockInit.setPropsAs("COPPER_ORE", Material.IRON_ORE);

        // Raw metal blocks
        BlockInit.setPropsAs("RAW_IRON_BLOCK", Material.IRON_BLOCK);
        BlockInit.setPropsAs("RAW_COPPER_BLOCK", Material.IRON_BLOCK);
        BlockInit.setPropsAs("RAW_GOLD_BLOCK", Material.DIAMOND_BLOCK);

        //Deepslate blocks
        BlockFlags.addFlags(Material.DEEPSLATE, BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps(Material.DEEPSLATE, new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3f, true));

        BlockFlags.addFlags(Material.COBBLED_DEEPSLATE, BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps(Material.COBBLED_DEEPSLATE, new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3.5f, true));

        BlockFlags.addFlags("POLISHED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE", Material.COBBLED_DEEPSLATE);

        BlockFlags.addFlags("DEEPSLATE_BRICKS", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("DEEPSLATE_BRICKS", Material.COBBLED_DEEPSLATE);

        BlockFlags.addFlags("CRACKED_DEEPSLATE_BRICKS", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("CRACKED_DEEPSLATE_BRICKS", Material.COBBLED_DEEPSLATE);

        BlockFlags.addFlags("DEEPSLATE_TILES", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("DEEPSLATE_TILES", Material.COBBLED_DEEPSLATE);

        BlockFlags.addFlags("CRACKED_DEEPSLATE_TILES", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("CRACKED_DEEPSLATE_TILES", Material.COBBLED_DEEPSLATE);

        BlockFlags.addFlags("CHISELED_DEEPSLATE", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockInit.setPropsAs("CHISELED_DEEPSLATE", Material.COBBLED_DEEPSLATE);

        //Deepslate walls
        BlockInit.setPropsAs("COBBLED_DEEPSLATE_WALL", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE_WALL", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_BRICK_WALL", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_TILE_WALL", Material.COBBLED_DEEPSLATE);

        //Deepslate stairs
        BlockInit.setPropsAs("COBBLED_DEEPSLATE_STAIRS", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE_STAIRS", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_BRICK_STAIRS", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_TILE_STAIRS", Material.COBBLED_DEEPSLATE);

        //Deepslate slabs
        BlockInit.setPropsAs("COBBLED_DEEPSLATE_SLAB", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("POLISHED_DEEPSLATE_SLAB", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_BRICK_SLAB", Material.COBBLED_DEEPSLATE);
        BlockInit.setPropsAs("DEEPSLATE_TILE_SLAB", Material.COBBLED_DEEPSLATE);

        // Copper Blocks
        for (Material mat : MaterialUtil.ALL_REFINED_COPPER_BLOCKS) {
            BlockInit.setPropsAs(mat, Material.IRON_ORE);
        }

        // Flower Azalea
        BlockProperties.setBlockProps("AZALEA", BlockProperties.instantType);
        BlockFlags.addFlags("AZALEA", BlockFlags.F_GROUND);
        BlockProperties.setBlockProps("FLOWERING_AZALEA", BlockProperties.instantType);
        BlockFlags.addFlags("FLOWERING_AZALEA", BlockFlags.F_GROUND);
        // This is temporary ground.
        BlockFlags.addFlags("BIG_DRIPLEAF", BlockFlags.F_GROUND);
        BlockProperties.setBlockProps("BIG_DRIPLEAF", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f));
        BlockProperties.setBlockProps("BIG_DRIPLEAF_STEM", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.1f));

        BlockFlags.addFlags("BIG_DRIPLEAF_STEM", BlockFlags.F_IGN_PASSABLE);
        BlockProperties.setBlockProps("GLOW_LICHEN", new BlockProperties.BlockProps(BlockProperties.woodAxe, 0.2f));
        BlockFlags.addFlags("GLOW_LICHEN", BlockFlags.F_IGN_PASSABLE);
        BlockFlags.addFlags("CAVE_VINES", BlockFlags.F_CLIMBABLE);
        BlockFlags.addFlags("CAVE_VINES_PLANT", BlockFlags.F_CLIMBABLE);
        
        BlockProperties.setBlockProps("HANGING_ROOTS", BlockProperties.instantType);
        BlockProperties.setBlockProps("ROOTED_DIRT", new BlockProperties.BlockProps(BlockProperties.woodSpade, 0.5f));

        //Dripstone
        BlockProperties.setBlockProps("DRIPSTONE_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, true));
        BlockFlags.addFlags("POINTED_DRIPSTONE", BlockFlags.SOLID_GROUND | BlockFlags.F_VARIABLE);
        BlockProperties.setBlockProps("POINTED_DRIPSTONE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f));

        //Amethyst buds
        BlockFlags.addFlags("SMALL_AMETHYST_BUD", BlockFlags.SOLID_GROUND);
        BlockFlags.addFlags("MEDIUM_AMETHYST_BUD", BlockFlags.SOLID_GROUND);
        BlockFlags.addFlags("LARGE_AMETHYST_BUD", BlockFlags.SOLID_GROUND);
        BlockFlags.addFlags("AMETHYST_CLUSTER", BlockFlags.SOLID_GROUND);
        BlockProperties.setBlockProps("SMALL_AMETHYST_BUD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f));
        BlockProperties.setBlockProps("MEDIUM_AMETHYST_BUD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f));
        BlockProperties.setBlockProps("LARGE_AMETHYST_BUD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f));
        BlockProperties.setBlockProps("AMETHYST_CLUSTER", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f));
        //Amethyst blocks
        BlockProperties.setBlockProps("BUDDING_AMETHYST", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, true));
        BlockProperties.setBlockProps("AMETHYST_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, true));

        // Not done
        //Candle cakes
        for (Material mat : MaterialUtil.ALL_CANDLE_CAKE) {
            BlockFlags.addFlags(mat, BlockFlags.F_GROUND);
            BlockProperties.setBlockProps(mat, new BlockProperties.BlockProps(BlockProperties.noTool, 0.5f));
        }

        //Other blocks
        BlockProperties.setBlockProps("CALCITE", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 0.75f, true));

        // Tuff
        for (Material mat : MaterialUtil.ALL_TUFF) {
            BlockProperties.setBlockProps(mat, new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 1.5f, true));
        }
        BlockFlags.setBlockFlags("POWDER_SNOW", BlockFlags.F_POWDERSNOW | BlockFlags.F_IGN_PASSABLE | BlockFlags.F_GROUND | BlockFlags.F_GROUND_HEIGHT);
        BlockProperties.setBlockProps("POWDER_SNOW", new BlockProperties.BlockProps(BlockProperties.noTool, 0.25f));
        BlockInit.setPropsAs("POWDER_SNOW_CAULDRON", Material.CAULDRON);
        BlockInit.setPropsAs("WATER_CAULDRON", Material.CAULDRON);
        BlockInit.setPropsAs("LAVA_CAULDRON", Material.CAULDRON);
        BlockProperties.setBlockProps("MOSS_BLOCK", new BlockProperties.BlockProps(BlockProperties.woodHoe, 0.1f));
        BlockFlags.addFlags("LIGHTNING_ROD", BlockFlags.SOLID_GROUND);
        BlockProperties.setBlockProps("LIGHTNING_ROD", new BlockProperties.BlockProps(BlockProperties.woodPickaxe, 3f, true));
        BlockInit.setAs("SMOOTH_BASALT", BridgeMaterial.TERRACOTTA);
        BlockFlags.addFlags("SCULK_SENSOR", BlockFlags.SOLID_GROUND);
        BlockProperties.setBlockProps("SCULK_SENSOR", new BlockProperties.BlockProps(BlockProperties.woodHoe, 1.5f));
        
        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.17 blocks.");
    }
}
