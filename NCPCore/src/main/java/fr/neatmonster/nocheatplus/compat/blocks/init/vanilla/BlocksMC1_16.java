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
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

public class BlocksMC1_16 implements BlockPropertiesSetup{
    public BlocksMC1_16() {
        BlockInit.assertMaterialExists("SOUL_CAMPFIRE");
        BlockInit.assertMaterialExists("CHAIN");
    }
    @SuppressWarnings("deprecation")
    @Override
    public void setupBlockProperties(WorldConfigProvider<?> worldConfigProvider) {
        BlockInit.setInstantPassable("NETHER_SPROUTS");
        // More Torch blocks
        BlockInit.setInstantPassable("SOUL_TORCH");
        BlockInit.setInstantPassable("SOUL_WALL_TORCH");
        // Soul Fire
        BlockInit.setInstantPassable("SOUL_FIRE");
        // Target
        BlockProperties.setBlockProps("TARGET", new BlockProperties.BlockProps(BlockProperties.woodHoe, 0.5f));
        BlockFlags.setFlagsAs("TARGET", Material.OAK_PLANKS);
        // Leaves
        for (Material mat : MaterialUtil.LEAVES) {
            BlockProperties.setBlockProps(mat, new BlockProperties.BlockProps(BlockProperties.woodHoe, 0.2f));
        }
        // Sponges
        BlockProperties.setBlockProps("WET_SPONGE", new BlockProperties.BlockProps(BlockProperties.woodHoe, 0.6f));
        BlockProperties.setBlockProps(Material.SPONGE, new BlockProperties.BlockProps(BlockProperties.woodHoe, 0.6f));
        // Hay block
        BlockInit.setPropsAs("HAY_BLOCK", "TARGET");
        // Dried kelp block
        BlockInit.setPropsAs("DRIED_KELP_BLOCK", "TARGET");
        // ShroomLight
        BlockFlags.setFlagsAs("SHROOMLIGHT", Material.OAK_PLANKS);
        BlockProperties.setBlockProps("SHROOMLIGHT", new BlockProperties.BlockProps(BlockProperties.woodHoe, 1f));
        // More Quartz block
        BlockInit.setPropsAs("QUARTZ_BRICKS", "QUARTZ_BLOCK");
        // Soul Soil
        BlockInit.setAs("SOUL_SOIL", Material.SAND);
        BlockFlags.addFlags("SOUL_SOIL", BlockFlags.F_SOULSAND);
        // More Obsidian block
        BlockInit.setAs("CRYING_OBSIDIAN", Material.OBSIDIAN);
        BlockInit.setAs("RESPAWN_ANCHOR", Material.OBSIDIAN);
        BlockInit.setAs("NETHERITE_BLOCK", Material.OBSIDIAN);
        // BlackStone
        BlockInit.setAs("BLACKSTONE", Material.STONE);
        BlockInit.setAs("GILDED_BLACKSTONE", Material.STONE);
        BlockInit.setPropsAs("BLACKSTONE_SLAB", "RED_NETHER_BRICK_STAIRS");
        BlockInit.setPropsAs("BLACKSTONE_STAIRS", Material.STONE);
        BlockInit.setAs("POLISHED_BLACKSTONE_BRICKS", Material.STONE);
        BlockInit.setAs("CRACKED_POLISHED_BLACKSTONE_BRICKS", Material.STONE);
        BlockInit.setPropsAs("POLISHED_BLACKSTONE_BRICK_STAIRS", Material.STONE);
        BlockInit.setAs("CHISELED_POLISHED_BLACKSTONE", Material.STONE);
        // BlackStone(higher hardness)
        BlockInit.setPropsAs("POLISHED_BLACKSTONE_BRICK_SLAB", Material.COBBLESTONE);
        BlockInit.setPropsAs("POLISHED_BLACKSTONE_SLAB", Material.COBBLESTONE);
        BlockInit.setPropsAs("POLISHED_BLACKSTONE", Material.COBBLESTONE);
        BlockInit.setPropsAs("POLISHED_BLACKSTONE_WALL", Material.COBBLESTONE);
        BlockInit.setPropsAs("POLISHED_BLACKSTONE_STAIRS", Material.COBBLESTONE);
        BlockInit.setPropsAs("POLISHED_BLACKSTONE_PRESSURE_PLATE", BridgeMaterial.STONE_PRESSURE_PLATE);
        BlockInit.setPropsAs("POLISHED_BLACKSTONE_BUTTON", Material.STONE_BUTTON);

        // Wart blocks
        BlockInit.setAs("NETHER_WART_BLOCK", "SHROOMLIGHT");
        BlockInit.setAs("WARPED_WART_BLOCK", "SHROOMLIGHT");
        // Lodestone
        BlockInit.setAs("LODESTONE", Material.FURNACE);
        // More Campfire blocks
        BlockInit.setAs("SOUL_CAMPFIRE", "CAMPFIRE");
        // More NetherBrick blocks
        BlockInit.setAs("CHISELED_NETHER_BRICKS",BridgeMaterial.NETHER_BRICKS);
        BlockInit.setAs("CRACKED_NETHER_BRICKS",BridgeMaterial.NETHER_BRICKS);
        // Basalt
        BlockInit.setAs("BASALT", BridgeMaterial.TERRACOTTA);
        BlockInit.setAs("POLISHED_BASALT", BridgeMaterial.TERRACOTTA);
        // Nether gold ore
        BlockInit.setAs("NETHER_GOLD_ORE", Material.COAL_ORE);
        // More Lantern blocks
        BlockInit.setAs("SOUL_LANTERN", "LANTERN");

        // Chain
        BlockFlags.addFlags("CHAIN", BlockFlags.SOLID_GROUND);
        BlockInit.setPropsAs("CHAIN", Material.IRON_BLOCK);

        // Ancient Debris
        BlockFlags.addFlags("ANCIENT_DEBRIS", BlockFlags.FULLY_SOLID_BOUNDS);
        BlockProperties.setBlockProps("ANCIENT_DEBRIS", new BlockProperties.BlockProps(BlockProperties.diamondPickaxe, 30f, true));

        // More Walls
        BlockInit.setPropsAs("BLACKSTONE_WALL",Material.STONE);
        BlockInit.setPropsAs("POLISHED_BLACKSTONE_BRICK_WALL",Material.STONE);

        // Climbable plants
        BlockFlags.addFlags("TWISTING_VINES", BlockFlags.F_CLIMBABLE);
        BlockFlags.addFlags("TWISTING_VINES_PLANT", BlockFlags.F_CLIMBABLE);
        BlockFlags.addFlags("WEEPING_VINES", BlockFlags.F_CLIMBABLE);
        BlockFlags.addFlags("WEEPING_VINES_PLANT", BlockFlags.F_CLIMBABLE);

        // Stem, Hyphae, Nylium
        BlockInit.setAs("CRIMSON_STEM", BridgeMaterial.OAK_LOG);
        BlockInit.setAs("CRIMSON_HYPHAE", BridgeMaterial.OAK_LOG);
        BlockInit.setAs("WARPED_HYPHAE", BridgeMaterial.OAK_LOG);
        BlockInit.setAs("WARPED_STEM", BridgeMaterial.OAK_LOG);
        BlockInit.setAs("STRIPPED_CRIMSON_STEM", BridgeMaterial.OAK_LOG);
        BlockInit.setAs("STRIPPED_CRIMSON_HYPHAE", BridgeMaterial.OAK_LOG);
        BlockInit.setAs("STRIPPED_WARPED_HYPHAE", BridgeMaterial.OAK_LOG);
        BlockInit.setAs("STRIPPED_WARPED_STEM", BridgeMaterial.OAK_LOG);
        BlockInit.setAs("CRIMSON_NYLIUM", Material.NETHERRACK);
        BlockInit.setAs("WARPED_NYLIUM", Material.NETHERRACK);

        //Piston
        for (Material mat: new Material[]{BridgeMaterial.PISTON, 
                BridgeMaterial.PISTON_HEAD, BridgeMaterial.STICKY_PISTON}) {
            BlockProperties.setBlockProps(mat, new BlockProps(BlockProperties.woodPickaxe, 1.5f));
        }
        
        // Re-add the decrease 8ism flag to lava. (See BlocksMC1_13)
        for (final Material mat : MaterialUtil.LAVA) {
            BlockFlags.addFlags(mat, BlockFlags.F_HEIGHT_8SIM_DEC);
        }

        ConfigFile config = ConfigManager.getConfigFile();
        if (config.getBoolean(ConfPaths.BLOCKBREAK_DEBUG, config.getBoolean(ConfPaths.CHECKS_DEBUG, false)))
        StaticLog.logInfo("Added block-info for Minecraft 1.16 blocks.");
    }
}
