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
package fr.neatmonster.nocheatplus.utilities.map;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import fr.neatmonster.nocheatplus.compat.AlmostBoolean;
import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.InventoryUtil;
import fr.neatmonster.nocheatplus.utilities.StringUtil;

/**
 * Auxiliary collections and functionality for Material.
 * 
 * @author asofold
 *
 */
public class MaterialUtil {

    // TODO: Naming conventions for collections (_BLOCKS suffix?).


    ///////////////////////
    // Internal
    ///////////////////////

    private static final List<String> woodTypes = Arrays.asList(
            "acacia", "birch", "dark_oak", "jungle", "oak", "spruce", "warped", "crimson", "mangrove", "bamboo", "cherry",
            "wood" // Legacy
            );

    private static List<EntityType> collectTypesBySuffix(String prefix) {
        prefix = prefix.toLowerCase();
        final List<EntityType> res = new LinkedList<EntityType>();
        for (final EntityType type : EntityType.values()) {
            if (type.name().toLowerCase().endsWith(prefix)) {
                res.add(type);
            }
        }
        return res;
    }

    /**
     * Get a new set containing the given set, as well as all non-null results
     * from names.
     * 
     * @param set
     * @param names
     * @return
     */
    private static Set<Material> add(Set<Material> set, String... names) {
        final LinkedHashSet<Material> res = new LinkedHashSet<Material>(set);
        res.addAll(BridgeMaterial.getAll(names));
        return res;
    }

    /**
     * Get a new set containing the given set, as well as all non-null results
     * from names.
     * 
     * @param set
     * @param names
     * @return
     */
    public static Set<Material> addBlocks(Set<Material> set, String... names) {
        final LinkedHashSet<Material> res = new LinkedHashSet<Material>(set);
        res.addAll(BridgeMaterial.getAllBlocks(names));
        return res;
    }

    /**
     * Get a new set containing the given set, as well as all non-null Material entries.
     * 
     * @param set Set is not checked.
     * @param materials
     * @return
     */
    public static Set<Material> addBlocks(final Set<Material> set, final Material... materials) {
        final LinkedHashSet<Material> res = new LinkedHashSet<Material>(set);
        for (final Material mat : materials) {
            if (mat != null && mat.isBlock()) {
                res.add(mat);
            }
        }
        return res;
    }


    /**
     * Get a new set containing all elements of the given sets.
     * 
     * @param sets
     * @return
     */
    public static Set<Material> join(final Set<Material>...sets ) {
        final Set<Material> res = new LinkedHashSet<Material>();
        for (final Set<Material> set : sets) {
            res.addAll(set);
        }
        return res;
    }

    /**
     * Dump public static fields of type Set<?> to StaticLog, using
     * StringUtil.join (not recursive).
     */
    public static void dumpStaticSets(final Class<?> clazz, final Level level) {
        final StringBuilder builder = new StringBuilder(6000);
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers())) {
                try {
                    Object obj = field.get(clazz);
                    if (obj instanceof Set<?>) {
                        Set<?> set = (Set<?>) obj;
                        builder.append(clazz.getName());
                        builder.append('.');
                        builder.append(field.getName());
                        builder.append(": ");
                        StringUtil.join(set, ", ", builder);
                        builder.append('\n');
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {}
            }
        }
        StaticLog.log(level, builder.toString());
    }

    /////////////////////////////////////////////////
    // Material collections with common properties
    // (May not always have all aspects in common.)
    /////////////////////////////////////////////////
	public static final Set<Material> ALL_CAULDRONS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("cauldron", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> ALL_BUTTONS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("_button", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> ALL_DOORS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    null, Arrays.asList("_door"), AlmostBoolean.YES,
                    "trap", "legacy")
            );

    public static final Set<Material> COPPER_BLOCKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix("_copper", AlmostBoolean.YES), "copper_block", "waxed_copper_block"));

    public static final Set<Material> ALL_REFINED_COPPER_BLOCKS = Collections.unmodifiableSet(
            BridgeMaterial.getByContains(AlmostBoolean.YES, Arrays.asList("copper"), "ore", "raw"));

    public static final Set<Material> COPPER_BULBS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("copper_bulb", AlmostBoolean.YES));

    public static final Set<Material> COPPER_GRATES = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("copper_grate", AlmostBoolean.YES));

    public static final Set<Material> ALL_TUFF = Collections.unmodifiableSet(
            BridgeMaterial.getByContains(AlmostBoolean.YES, Arrays.asList("tuff")));

    public static final Set<Material> ALL_CANDLES = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("candle", AlmostBoolean.YES));

    public static final Set<Material> LANTERNS = Collections.unmodifiableSet(
            BridgeMaterial.getAllBlocks("lantern", "soul_lantern"));

    public static final Set<Material> ALL_CANDLE_CAKE = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("candle_cake", AlmostBoolean.YES));

    public static final Set<Material> ALL_WALLS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("_wall", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> ALL_STAIRS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("_stairs", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> ALL_SWORDS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("_sword", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> ALL_TRAP_DOORS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    null, Arrays.asList("trapdoor", "trap_door"), AlmostBoolean.YES,
                    "legacy")
            );

    public static final Set<Material> ALL_PRESSURE_PLATES = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("_pressure_plate", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> RODS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("_rod", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> AMETHYST = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    null, 
                    Arrays.asList("_amethyst_bud", "_amethyst"),
                    AlmostBoolean.YES
                    ), "amethyst_block", "amethyst_cluster"));

    public static final Set<Material> BANNERS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    null, 
                    Arrays.asList("_banner"),
                    AlmostBoolean.YES,
                    "legacy", "_wall"
                    ), "standing_banner"));

    public static final Set<Material> BEDS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix("_bed", AlmostBoolean.YES, 
                    "legacy"), "bed_block"));

    public static final Set<Material> BOATS;
    public static final Set<Material> ALL_SEEDS;
    static {
        HashSet<Material> temp = new HashSet<Material>();
        HashSet<Material> seedsTemp = new HashSet<Material>();
        if (BridgeMaterial.get("boat") != null) {
            temp.add(BridgeMaterial.get("boat"));
        }
        seedsTemp.addAll(InventoryUtil.collectItemsBySuffix("_SEEDS"));
        temp.addAll(InventoryUtil.collectItemsByPrefix("BOAT_"));
        temp.addAll(InventoryUtil.collectItemsBySuffix("_BOAT"));
        BOATS = Collections.unmodifiableSet(temp);
        ALL_SEEDS = Collections.unmodifiableSet(seedsTemp);
    }

    private static final List<EntityType> BOATSTYPE = collectTypesBySuffix("BOAT");

    public static final Set<Material> CARPETS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix("_carpet", AlmostBoolean.YES, "legacy"), 
            "carpet"));

    public static final Set<Material> CAULDRON = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("cauldron", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> CONCRETE_BLOCKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix("_concrete", AlmostBoolean.YES, "legacy"),
            "concrete"));

    public static final Set<Material> CONCRETE_POWDER_BLOCKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix("_concrete_powder", AlmostBoolean.YES, "legacy"),
            "concrete_powder"));

    /** Dead or alive. */
    public static final Set<Material> CORAL_BLOCKS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("coral_block", AlmostBoolean.YES, "legacy")
            );

    /**
     * Dead coral parts, that have been passable alive, but which are like stone
     * when dead.
     */
    public static final Set<Material> DEAD_CORAL_PARTS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    Arrays.asList("dead_"),
                    Arrays.asList("coral_fan", "coral_wall_fan", "coral"),
                    AlmostBoolean.YES, 
                    "block", "legacy"
                    ));

    /** Flower pot and potted plants / things. */
    public static final Set<Material> FLOWER_POTS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefix(
                    "potted_", AlmostBoolean.YES), "flower_pot"));

    /** Stained and other. */
    public static final Set<Material> GLASS_BLOCKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix("_glass", AlmostBoolean.YES, "legacy", "thin"),
            "glass"));

    public static final Set<Material> GLASS_PANES = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix("_glass_pane", AlmostBoolean.YES, "legacy"), 
            "glass_pane", "thin_glass"));

    public static final Set<Material> GLAZED_TERRACOTTA_BLOCKS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("glazed_terracotta", AlmostBoolean.YES, "legacy")
            );

    /** Heads placed on the ground. Includes skulls. */
    public static final Set<Material> HEADS_GROUND = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    null, 
                    Arrays.asList("_skull", "_head"),
                    AlmostBoolean.YES,
                    "legacy", "_wall_"
                    ), "skull"));

    /** Heads placed on the wall. Includes skulls. */
    public static final Set<Material> HEADS_WALL = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    null, 
                    Arrays.asList("_wall_skull", "_wall_head"),
                    AlmostBoolean.YES,
                    "legacy"
                    ));

    /** Blocks that are infested with silverfish. */
    public static final Set<Material> INFESTED_BLOCKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefix("infested_", AlmostBoolean.YES), "monster_eggs"));

    /** All lava blocks. */
    public static final Set<Material> LAVA = Collections.unmodifiableSet(
            BridgeMaterial.getAllBlocks("lava", "stationary_lava"));

    public static final Set<Material> LEAVES = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_leaves"),
                    AlmostBoolean.YES
                    // , ...
                    ), "leaves", "leaves_2", "azalea_leaves", "flowering_azalea_leaves"));

    /** LOGS. */
    public static final Set<Material> LOGS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_log"),
                    AlmostBoolean.YES,
                    "legacy"
                    ), "log", "log_2"));

    /** Mushroom blocks (huge ones). */ 
    public static final Set<Material> MUSHROOM_BLOCKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix(
                    "_mushroom_block",
                    AlmostBoolean.YES,
                    "legacy"
                    ), "mushroom_stem" , "huge_mushroom_1", "huge_mushroom_2"));

    public static final Set<Material> PLANKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix("_planks", AlmostBoolean.YES, "legacy"),
            "wood"));

    /** All rail types. */
    public static final Set<Material> RAILS = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix(Arrays.asList("rail", "rails"), AlmostBoolean.YES, "legacy"));

    public static final Set<Material> SHULKER_BOXES = Collections.unmodifiableSet(
            BridgeMaterial.getBySuffix("shulker_box", AlmostBoolean.YES, "legacy"));

    public static final Set<Material> SLABS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix(Arrays.asList("_slab", "_step"), 
                    AlmostBoolean.YES, "legacy"), "step"));

    public static final Set<Material> SPAWN_EGGS = Collections.unmodifiableSet(add(
            BridgeMaterial.getBySuffix("_spawn_egg", AlmostBoolean.YES, "legacy"),
            "monster_egg"
            ));

    public static final Set<Material> STRIPPED_LOGS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    Arrays.asList("stripped_"), 
                    Arrays.asList("_log"), AlmostBoolean.YES)
            );

    public static final Set<Material> STRIPPED_WOOD_BLOCKS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    Arrays.asList("stripped_"), 
                    Arrays.asList("_wood"), AlmostBoolean.YES)
            );

    /** All ordinary terracotta (hard clay) blocks. */
    public static final Set<Material> TERRACOTTA_BLOCKS = Collections.unmodifiableSet(addBlocks(
            // TODO: exclude GLAZED or not?
            BridgeMaterial.getByPrefixAndSuffix(
                    null, Arrays.asList("_terracotta"), 
                    AlmostBoolean.YES, 
                    "legacy", "glazed"),
            "terracotta", "hard_clay", "stained_clay"
            ));

    /**
     * Collect fully solid blocks, that are not contained in other collections
     * (of blocks that are fully solid too).
     */
    public static final Set<Material> VARIOUS_FULLY_SOLID = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix(
                    Arrays.asList(
                            "_bricks", "_ore", "prismarine", 
                            "andesite", "diorite", "granite",
                            "sandstone",
                            "command_block", "command"
                            ), AlmostBoolean.YES, "legacy"),
            "barrier", "structure_block",
            "note_block", "tnt", "jukebox", "jack_o_lantern",
            //"piston", "sticky_piston", "piston_base", "piston_sticky_base",
            "dispenser", "dropper", "furnace", "observer", "beacon",
            "pumpkin", "melon_block", "hay_block", "bone_block", "dried_kelp_block",
            "nether_wart_block", "netherrack", "warped_wart_block",
            "snow_block", "ice", "magma_block",
            "diamond_block", "gold_block", "iron_block", "coal_block", 
            "emerald_block", "lapis_block", "redstone_block", 
            "purpur_block", "smooth_stone", "smooth_quartz", "quartz_block",
            "quartz_pillar", "bookshelf",
            "sand", "stone", "gravel", "dirt", "grass_block", "grass", "obsidian", "clay",
            "cobblestone", "mossy_cobblestone",
            "sea_lantern", "redstone_lamp", "glowstone", "sponge", "wet_sponge",
            "budding_amethyst", "amethyst_block",
            "calcite", "tuff", "moss_block", "rooted_dirt", "dripstone_block"
            ));

    public static final Set<Material> WALL_BANNERS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    null, 
                    Arrays.asList("_wall_banner"),
                    AlmostBoolean.YES,
                    "legacy"
                    ), "wall_banner"));

    /** All water blocks. */
    public static final Set<Material> WATER = Collections.unmodifiableSet(
            BridgeMaterial.getAllBlocks("water", "stationary_water"));
     public static final Set<Material> WATER_PLANTS = Collections.unmodifiableSet(
            BridgeMaterial.getAllBlocks("TALL_SEAGRASS", "KELP_PLANT", "SEAGRASS", "KELP"));

    /** Wood types (1.13 rather). */
    public static final Set<Material> WOOD_BLOCKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_wood"),
                    AlmostBoolean.YES
                    // , ...
                    ), "wood"));

    public static final Set<Material> WOODEN_SIGNS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_sign", "_hanging_sign"),
                    AlmostBoolean.YES
                    // , ...
                    ), "sign", "wall_sign"));
    
    // These are SOLID
    public static final Set<Material> WALL_HANGING_SIGNS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_wall_hanging_sign"),
                    AlmostBoolean.YES
                    ));

    public static final Set<Material> WOODEN_BUTTONS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_button"),
                    AlmostBoolean.YES
                    ));

    public static final Set<Material> WOODEN_DOORS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_door"),
                    AlmostBoolean.YES,
                    "trap"
                    ));

    public static final Set<Material> WOODEN_FENCES = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_fence"),
                    AlmostBoolean.YES
                    // , ...
                    ), "fence"));

    public static final Set<Material> WOODEN_FENCE_GATES = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_fence_gate"),
                    AlmostBoolean.YES
                    // , ...
                    ), "fence_gate"));

    /** Wooden pressure plates. */
    public static final Set<Material> WOODEN_PRESSURE_PLATES = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_plate"), // Strictly _pressure_plate for 1.13.
                    AlmostBoolean.YES
                    // , ...
                    ));

    /** Wooden slabs. */
    public static final Set<Material> WOODEN_SLABS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_slab", "_step"),
                    AlmostBoolean.YES, 
                    "double" // Legacy
                    ));

    /** Wooden stairs. */
    public static final Set<Material> WOODEN_STAIRS = Collections.unmodifiableSet(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_stairs"),
                    AlmostBoolean.YES
                    // , ...
                    ));

    public static final Set<Material> WOODEN_TRAP_DOORS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getByPrefixAndSuffix(
                    woodTypes, 
                    Arrays.asList("_trap_door", "_trapdoor"),
                    AlmostBoolean.YES
                    // , ...
                    ), "trap_door"));

    public static final Set<Material> WOOL_BLOCKS = Collections.unmodifiableSet(addBlocks(
            BridgeMaterial.getBySuffix(
                    "_wool", 
                    AlmostBoolean.YES,
                    "legacy"
                    ), "wool"));

    ///////////////////////////////
    // Collections of collections
    ///////////////////////////////

    /** Instantly breakable, fully passable. */
    @SuppressWarnings("unchecked")
    public static final Set<Material> INSTANT_PLANTS = Collections.unmodifiableSet(join(
            BridgeMaterial.getBySuffix(Arrays.asList(
                    "bush", "sapling", "tulip", "orchid", "mushroom", "bluet", "fungus", "roots"), 
                    AlmostBoolean.YES, "legacy", "potted", "muddy", "mangrove"),
            BridgeMaterial.getByPrefixAndSuffix(
                    null,
                    Arrays.asList("coral_fan", "coral_wall_fan", "coral"),
                    AlmostBoolean.YES, "dead", "legacy"),
            BridgeMaterial.getAllBlocks("attached_melon_stem", "attached_pumpkin_stem",
                    "allium", "dandelion", "dandelion_yellow", 
                    "double_plant", "fern", "kelp", "kelp_plant", "large_fern", 
                    "lilac", "melon_stem", "nether_wart", "nether_warts", 
                    "oxeye_daisy", "peony", "poppy", "red_rose", "rose_red", 
                    "seagrass", "sunflower", "tall_seagrass", "yellow_flower",
                    "spore_blossom", "small_dripleaf", "cave_vines", "cave_vines_plant",
                    "weeping_vines", "weeping_vines_plant",
                    "twisting_vines", "twisting_vines_plant",
                    "mangrove_propagule", "pink_petals", "torchflower", "torchflower_crop",
                    "pitcher_plant"
                    ),
            new HashSet<Material>(Arrays.asList(BridgeMaterial.TALL_GRASS, 
                    BridgeMaterial.WHEAT_CROPS, BridgeMaterial.CARROTS, 
                    BridgeMaterial.POTATOES, BridgeMaterial.GRASS,
                    Material.PUMPKIN_STEM, Material.MELON_STEM,
                    BridgeMaterial.SUGAR_CANE, BridgeMaterial.BEETROOTS))
            ));

    @SuppressWarnings("unchecked")
	public static final Set<Material> FARMABLE = Collections.unmodifiableSet(join(new HashSet<Material>(Arrays.asList(BridgeMaterial.TALL_GRASS, 
                    BridgeMaterial.WHEAT_CROPS, BridgeMaterial.CARROTS, 
                    BridgeMaterial.POTATOES, BridgeMaterial.GRASS,
                    Material.PUMPKIN_STEM, Material.MELON_STEM,
                    BridgeMaterial.SUGAR_CANE, BridgeMaterial.BEETROOTS)),
                WATER_PLANTS,
                    BridgeMaterial.getAllBlocks("peony", "sunflower", "lilac", "rose_bush", "sweet_berry_bush", 
                                                "cocoa", "double_plant", "fern", "large_fern", "attached_melon_stem", 
                                                "attached_pumpkin_stem", "bamboo", "bamboo_sapling")


));

    /**
     * Sets of fully solid blocks (in terms of: can walk on, can't pass through,
     * full bounds - not necessarily 'solid' officially).
     */
    @SuppressWarnings("unchecked")
    public static final Set<Material> FULLY_SOLID_BLOCKS = Collections.unmodifiableSet(join(
            CONCRETE_BLOCKS,
            CONCRETE_POWDER_BLOCKS,
            CORAL_BLOCKS,
            COPPER_BLOCKS,
            COPPER_GRATES,
            COPPER_BULBS,
            GLASS_BLOCKS,
            GLAZED_TERRACOTTA_BLOCKS,
            INFESTED_BLOCKS,
            LEAVES,
            LOGS,
            MUSHROOM_BLOCKS,
            PLANKS,
            STRIPPED_LOGS,
            STRIPPED_WOOD_BLOCKS,
            TERRACOTTA_BLOCKS,
            VARIOUS_FULLY_SOLID,
            WOOD_BLOCKS,
            WOOL_BLOCKS,
            new HashSet<Material>(Arrays.asList(
                    BridgeMaterial.BRICKS,
                    BridgeMaterial.MYCELIUM,
                    BridgeMaterial.CRAFTING_TABLE,
                    BridgeMaterial.END_STONE
                    ))
            ));

    /**
     * Collections of blocks that are fully passable.
     */
    @SuppressWarnings("unchecked")
    public static final Set<Material> FULLY_PASSABLE_BLOCKS = Collections.unmodifiableSet(join(
            ALL_BUTTONS,
            ALL_PRESSURE_PLATES,
            BANNERS,
            RAILS,
            WALL_BANNERS,
            INSTANT_PLANTS,
            WATER_PLANTS,
            BridgeMaterial.getAllBlocks("structure_void", "end_gateway"),
            new HashSet<Material>(Arrays.asList(
                    Material.LEVER,
                    BridgeMaterial.END_PORTAL,
                    BridgeMaterial.NETHER_PORTAL))
            
            ));
    /**
     * Collections of blocks that are allowed all types of movement.
     */
    @SuppressWarnings("unchecked")
    public static final Set<Material> LIQUID_BLOCKS = Collections.unmodifiableSet(join(
            DEAD_CORAL_PARTS, WATER_PLANTS
            ));


    ////////////////////
    // Access methods.
    ////////////////////
    
    /**
     * Test if the material is a sword
     * 
     * @param mat
     * @return
     */
    public static boolean isSword(final Material mat) {
        return ALL_SWORDS.contains(mat);
    }

    /**
     * Test if the material is a boat.
     * 
     * @param mat
     * @return
     */
    public static boolean isBoat(final Material mat) {
        return BOATS.contains(mat);
    }

    /**
     * Test if the material is a spawn egg.
     * 
     * @param mat
     * @return
     */
    public static boolean isSpawnEgg(final Material mat) {
        return SPAWN_EGGS.contains(mat);
    }
    
    /**
     * Check if is log
     * 
     * @param mat
     * @return 
     */
    public static boolean isLog(final Material mat) {
        return LOGS.contains(mat);
    }
    
    /**
     * Check if is a sign or wall sign
     * 
     * @param mat
     * @return
     */
    public static boolean isSign(final Material mat) {
        return WOODEN_SIGNS.contains(mat);
    }
    
    /**
     * Check if the item can be planted in a dirt-like block
     * 
     * @param mat
     * @return
     */
    public static boolean isSeedable(final Material mat) {
        return mat == BridgeMaterial.CARROTS || ALL_SEEDS.contains(mat) 
               || mat == BridgeMaterial.POTATOES || mat == BridgeMaterial.BEETROOTS;
    }
    
    /**
     * Check if is boat type
     * 
     * @param entity
     * @return 
     */
    public static boolean isBoat(final EntityType entity) {
        return BOATSTYPE.contains(entity);
    }
    
    /**
     * Check if this block can be fertilized with bone meal
     * (Intended for player-farming, rather. I.e.: bone meal + seeds in both hands)
     * 
     * @param mat
     * @return
     */
    public static boolean isFarmable(final Material mat) {
        return isSeedable(mat) || FARMABLE.contains(mat);
    }
    
    /**
     * Check if is the material is a sign of any kind (normal(post, wall, hanging, wall hanging)
     * 
     * @param mat
     * @return 
     */
    public static boolean isAnySign(final Material mat) {
        return WOODEN_SIGNS.contains(mat) // Includes hanging_sign
              || WALL_HANGING_SIGNS.contains(mat);
    }
}
