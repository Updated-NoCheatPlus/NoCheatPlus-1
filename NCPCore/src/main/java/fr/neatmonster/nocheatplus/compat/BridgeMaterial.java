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
package fr.neatmonster.nocheatplus.compat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.ds.prefixtree.SimpleCharPrefixTree;

public class BridgeMaterial {

    // TODO: Should be non static, ideally.

    /** Legacy Material by lower case name without preceding 'legacy_' part. */
    private static final Map<String, Material> legacy = new HashMap<String, Material>();

    /** Actual lower case name to Material map for all existing materials. */
    private static final Map<String, Material> all = new HashMap<String, Material>();

    static {
        for (Material mat : Material.values()) {
            String name = mat.name().toLowerCase(Locale.ROOT);
            all.put(name, mat);
            if (name.startsWith("legacy_")) {
                legacy.put(name.substring(7), mat);
            }
        }
    }

    public static Material legacy(String name) {
        return legacy.get(name.toLowerCase());
    }

    /**
     * Get current Material by case-insensitive name.
     * 
     * @param name
     * @return
     */
    public static Material get(String name) {
        return all.get(name.toLowerCase());
    }

    /**
     * Like {@link BridgeMaterial#get(String)}, but return null for non-blocks.
     * 
     * @param name
     * @return
     */
    public static Material getBlock(String name) {
        Material mat = get(name);
        if (mat == null || !mat.isBlock()) {
            return null;
        }
        else {
            return mat;
        }
    }

    /**
     * Get current Material by case-insensitive name.
     * 
     * @param name
     * @return
     * @throws NullPointerException If the material is not present.
     */
    public static Material getNotNull(String name) {
        final Material mat = get(name);
        if (mat == null) {
            throw new NullPointerException("Material not present: " + name);
        }
        else {
            return mat;
        }
    }

    /**
     * Get current Material by case-insensitive names, return the first Material
     * instance that exists for a given name.
     * 
     * @param name
     * @return
     */
    public static Material getFirst(String... names) {
        for (String name : names) {
            final Material mat = get(name);
            if (mat != null) {
                return mat;
            }
        }
        return null;
    }

    /**
     * Get current Material by case-insensitive names, return the first Material
     * instance that exists for a given name.
     * 
     * @param name
     * @return
     * @throws NullPointerException
     *             If no material is present.
     */
    public static Material getFirstNotNull(String... names) {
        final Material mat = getFirst(names);
        if (mat == null) {
            throw new NullPointerException("Material not present: " + StringUtil.join(names, ", "));
        }
        else {
            return mat;
        }
    }

    public static Set<Material> getAll(String... names) {
        final LinkedHashSet<Material> res = new LinkedHashSet<Material>();
        for (final String name : names) {
            final Material mat = get(name);
            if (mat != null) {
                res.add(mat);
            }
        }
        return res;
    }

    public static Set<Material> getAllBlocks(String... names) {
        final LinkedHashSet<Material> res = new LinkedHashSet<Material>();
        for (final String name : names) {
            final Material mat = get(name);
            if (mat != null && mat.isBlock()) {
                res.add(mat);
            }
        }
        return res;
    }

    public static boolean has(String name) {
        return all.containsKey(name.toLowerCase());
    }

    /////////////////////////////////////////////////////////
    // Specific unique material instances for items (only).
    /////////////////////////////////////////////////////////

    public static final Material DIAMOND_SHOVEL = getFirstNotNull("diamond_shovel", "diamond_spade");

    public static final Material NETHERITE_AXE = getFirst("netherite_axe");
    public static final Material NETHERITE_HOE = getFirst("netherite_hoe");
    public static final Material NETHERITE_PICKAXE = getFirst("netherite_pickaxe");
    public static final Material NETHERITE_SHOVEL = getFirst("netherite_shovel");
    public static final Material NETHERITE_SWORD = getFirst("netherite_sword");

    public static final Material GOLDEN_AXE = getFirstNotNull("golden_axe", "gold_axe");
    public static final Material GOLDEN_HOE = getFirstNotNull("golden_hoe", "gold_hoe");
    public static final Material GOLDEN_PICKAXE = getFirstNotNull("golden_pickaxe", "gold_pickaxe");
    public static final Material GOLDEN_SHOVEL = getFirstNotNull("golden_shovel", "gold_spade");
    public static final Material GOLDEN_SWORD = getFirstNotNull("golden_sword", "gold_sword");

    public static final Material IRON_SHOVEL = getFirstNotNull("iron_shovel", "iron_spade");

    public static final Material STONE_SHOVEL = getFirstNotNull("stone_shovel", "stone_spade");

    public static final Material WOODEN_AXE = getFirstNotNull("wooden_axe", "wood_axe");
    public static final Material WOODEN_HOE = getFirstNotNull("wooden_hoe", "wood_hoe");
    public static final Material WOODEN_PICKAXE = getFirstNotNull("wooden_pickaxe", "wood_pickaxe");
    public static final Material WOODEN_SHOVEL = getFirstNotNull("wooden_shovel", "wood_spade");
    public static final Material WOODEN_SWORD = getFirstNotNull("wooden_sword", "wood_sword");


    ///////////////////////////////////////////////////
    // Specific unique material instances for blocks.
    ///////////////////////////////////////////////////
    public static final Material BARREL = get("barrel"); // null for legacy servers

    public static final Material BEETROOTS = getFirst("beetroots", "beetroot_block");

    public static final Material BRICKS = getFirst("bricks", "brick");
    
    public static final Material BRICK_SLAB = getFirst("brick_slab");

    public static final Material CAKE = getFirstNotNull("cake_block", "cake");

    public static final Material CARROTS = getFirst("carrots", "carrot");

    public static final Material CAVE_AIR = getFirst("cave_air");

    public static final Material CHAIN_COMMAND_BLOCK = getFirst("chain_command_block", "command_chain");

    public static final Material COBBLESTONE_WALL = getFirstNotNull("cobblestone_wall", "cobble_wall");

    public static final Material COBWEB = getFirstNotNull("cobweb", "web");

    public static final Material COMMAND_BLOCK = getFirstNotNull("command_block", "command");

    public static final Material CRAFTING_TABLE = getFirstNotNull("crafting_table", "workbench");

    public static final Material DANDELION = getFirstNotNull("dandelion", "yellow_flower");

    public static final Material ENCHANTING_TABLE = getFirstNotNull("enchanting_table", "enchantment_table");

    public static final Material END_PORTAL = getFirstNotNull("end_portal", "ender_portal");

    public static final Material END_PORTAL_FRAME = getFirstNotNull("end_portal_frame", "ender_portal_frame");

    public static final Material END_STONE = getFirstNotNull("end_stone", "ender_stone");
    
    public static final Material END_STONE_BRICKS = getFirst("end_stone_bricks", "end_bricks");

    public static final Material FARMLAND = getFirstNotNull("farmland", "soil");

    /** Could be null for very old Minecraft. */
    public static final Material FIREWORK_ROCKET = getFirst("firework_rocket", "firework");

    /** Passable (long) grass block. */
    public static final Material GRASS = getFirstNotNull("short_grass", "long_grass", "grass");

    /** Classic dirt-like grass block. */
    public static final Material GRASS_BLOCK = getFirstNotNull("grass_block", "grass");

    public static final Material GRASS_PATH = getFirst("grass_path", "dirt_path");

    public static final Material HEAVY_WEIGHTED_PRESSURE_PLATE = getFirstNotNull("heavy_weighted_pressure_plate", "iron_plate");

    public static final Material IRON_BARS = getFirstNotNull("iron_bars", "iron_fence");

    /** (Block.) */
    public static final Material IRON_DOOR = getFirstNotNull("iron_door_block", "iron_door");

    public static final Material LIGHT_WEIGHTED_PRESSURE_PLATE = getFirstNotNull("light_weighted_pressure_plate", "gold_plate");

    public static final Material LILY_PAD = getFirstNotNull("lily_pad", "water_lily");

    public static final Material MAGMA_BLOCK = getFirst("magma_block", "magma");

    /** (Block.) /*/
    public static final Material MELON = getFirstNotNull("melon_block", "melon");

    public static final Material MOVING_PISTON = getFirstNotNull("moving_piston", "piston_moving_piece");

    public static final Material MYCELIUM = getFirstNotNull("mycelium", "mycel");

    public static final Material NETHER_BRICKS = getFirstNotNull("nether_bricks", "nether_brick");

    public static final Material NETHER_BRICK_FENCE = getFirstNotNull("nether_brick_fence", "nether_fence");

    public static final Material NETHER_PORTAL = getFirstNotNull("nether_portal", "portal");

    public static final Material NETHER_QUARTZ_ORE = getFirstNotNull("nether_quartz_ore", "quartz_ore");

    public static final Material NETHER_WARTS = getFirstNotNull("nether_warts", "nether_wart");

    /** For reference: ordinary log. */
    public static final Material OAK_LOG = getFirstNotNull("oak_log", "log");

    /** For reference: ordinary wood. */
    public static final Material OAK_WOOD = getFirstNotNull("oak_wood", "wood");

    /** For reference: the ordinary wooden trap door. */
    public static final Material OAK_TRAPDOOR = getFirstNotNull("oak_trapdoor", "trap_door");

    /** (Block.) */
    public static final Material PISTON = getFirstNotNull("piston_base", "piston");

    public static final Material PISTON_HEAD = getFirstNotNull("piston_head", "piston_extension");

    public static final Material POTATOES = getFirst("potatoes", "potato");

    public static final Material RED_NETHER_BRICKS = getFirst("red_nether_bricks", "red_nether_brick");

    /** For reference to have the breaking/shape. */
    public static final Material REPEATER = getFirstNotNull("repeater", "diode_block_off");

    public static final Material REPEATING_COMMAND_BLOCK = getFirst("repeating_command_block", "command_repeating");
    
    public static final Material SCAFFOLDING = get("scaffolding");

    /** Sign block. */
    public static final Material SIGN = getFirstNotNull("bamboo_sign", "cherry_sign", "acacia_sign", "birch_sign", "dark_oak_sign", "jungle_sign", "oak_sign", "spruce_sign",  "bamboo_wall_sign", "cherry_wall_sign", "acacia_wall_sign", "birch_wall_sign", "dark_oak_wall_sign", "jungle_wall_sign", "oak_wall_sign", "spruce_wall_sign", "sign_post", "sign", "wall_sign");

    /** Some skull for reference. */
    public static final Material SKELETON_SKULL = getFirst("skeleton_skull", "skull");

    public static final Material SPAWNER = getFirstNotNull("spawner", "mob_spawner");

    /** (Block.) */
    public static final Material STICKY_PISTON = getFirstNotNull("piston_sticky_base", "sticky_piston");

    public static final Material STONE_BRICKS = getFirstNotNull("stone_bricks", "smooth_brick");

    public static final Material STONE_BRICK_STAIRS = getFirstNotNull("stone_brick_stairs", "smooth_stairs");

    public static final Material STONE_PRESSURE_PLATE = getFirstNotNull("stone_pressure_plate", "stone_plate");

    public static final Material WOODEN_PRESSURE_PLATE = getFirstNotNull("wooden_pressure_plate", "wood_plate", "acacia_pressure_plate", "birch_pressure_plate", "dark_oak_pressure_plate", "jungle_pressure_plate", "oak_pressure_plate", "spruce_pressure_plate");

    public static final Material STONE_SLAB = getFirstNotNull("stone_slab", "step");

    public static final Material SUGAR_CANE = getFirstNotNull("sugar_cane_block", "sugar_cane"); // Reversed

    // TODO: Which is the old long grass...
    public static final Material TALL_GRASS = getFirstNotNull("tall_grass", "long_grass");

    public static final Material TERRACOTTA = getFirst("terracotta", "hard_clay");

    public static final Material VOID_AIR = get("void_air"); // May be null on legacy spigot.

    public static final Material FROGSPAWN = get("frogspawn");

    public static final Material WHEAT_CROPS = getFirstNotNull("crops", "wheat"); // Reversed

    private static void getBySuffix(final String suffix, final AlmostBoolean isBlock, 
            final SimpleCharPrefixTree excludePrefixTree, final Set<Material> res) {
        for (final Entry<String, Material> entry : all.entrySet()) {
            final String key = entry.getKey();
            if (key.endsWith(suffix)) {
                if (excludePrefixTree.hasPrefix(key)) {
                    continue;
                }
                final Material value = entry.getValue();
                if (isBlock == AlmostBoolean.MAYBE || !(isBlock.decide() ^ value.isBlock())) {
                    res.add(value);
                }
            }
        }
    }

    /**
     * 
     * @param suffix
     * @param isBlock
     * @param excludePrefixes
     * @return
     */
    public static Set<Material> getBySuffix(final String suffix, 
            final AlmostBoolean isBlock, final String... excludePrefixes) {
        final Set<Material> res = new LinkedHashSet<Material>();
        final SimpleCharPrefixTree prefixTree = new SimpleCharPrefixTree();
        prefixTree.feedAll(Arrays.asList(excludePrefixes), false, true);
        getBySuffix(suffix.toLowerCase(), isBlock, prefixTree, res);
        return res;
    }

    /**
     * Collect materials for all suffices as
     * {@link #getBySuffix(String, AlmostBoolean, String...)} does for one. <br>
     * 
     * @param suffices
     * @param isBlock
     * @param excludePrefixes
     * @return
     */
    public static Set<Material> getBySuffix(final Collection<String> suffices, 
            final AlmostBoolean isBlock, final String... excludePrefixes) {
        final Set<Material> res = new LinkedHashSet<Material>();
        final SimpleCharPrefixTree prefixTree = new SimpleCharPrefixTree();
        prefixTree.feedAll(Arrays.asList(excludePrefixes), false, true);
        for (final String suffix : suffices) {
            getBySuffix(suffix.toLowerCase(), isBlock, prefixTree, res);
        }
        return res;
    }

    public static Set<Material> getByPrefix(final String prefix, final AlmostBoolean isBlock) {
        final Set<Material> res = new LinkedHashSet<Material>();
        for (final Entry<String, Material> entry : all.entrySet()) {
            final String key = entry.getKey();
            if (key.startsWith(prefix)) {
                final Material value = entry.getValue();
                if (isBlock == AlmostBoolean.MAYBE || !(isBlock.decide() ^ value.isBlock())) {
                    res.add(value);
                }
            }
        }
        return res;
    }

    public static Set<Material> getByContains(final AlmostBoolean isBlock, final Collection<String> contains, final String... excludeContains) {
        final Set<Material> res = new LinkedHashSet<Material>();
        final List<String> useExcludeContains = new LinkedList<String>();
        for (final String exclude : excludeContains) {
            useExcludeContains.add(exclude.toLowerCase());
        }
        for (final Entry<String, Material> entry : all.entrySet()) {
            final String key = entry.getKey();
            for (String s : contains) {
                if (key.contains(s)) {
                    final Material value = entry.getValue();
                    if (isBlock == AlmostBoolean.MAYBE || !(isBlock.decide() ^ value.isBlock())) {
                        boolean match = true;
                        for (final String exclude : useExcludeContains) {
                            if (key.contains(exclude)) {
                                match = false;
                                break;
                            }
                        }
                        if (match) res.add(value);
                    }
                }
            }
        }
        return res;
    }

    /**
     * Return materials for which all prefixes and suffices match but none of
     * excludeContains is contained, respecting the isBlock filter.
     * 
     * @param prefixes
     *            If prefixes is null, all prefixes will match.
     * @param suffices
     * @param isBlock
     * @param excludeContains
     * @return
     */
    public static Set<Material> getByPrefixAndSuffix(final Collection<String> prefixes,
            final Collection<String> suffices, final AlmostBoolean isBlock,
            final String... excludeContains) {
        final Set<Material> res = new LinkedHashSet<Material>();
        final List<String> useExcludeContains = new LinkedList<String>();
        for (final String exclude : excludeContains) {
            useExcludeContains.add(exclude.toLowerCase());
        }
        final SimpleCharPrefixTree prefixTree;
        if (prefixes == null) {
            prefixTree = null;
        }
        else {
            prefixTree = new SimpleCharPrefixTree();
            prefixTree.feedAll(prefixes, false, true);
        }
        final SimpleCharPrefixTree suffixTree = new SimpleCharPrefixTree(); // reversed inputs (!).
        for (final String suffix : suffices) {
            suffixTree.feed(StringUtil.reverse(suffix.toLowerCase()));
        }
        final boolean isBlockDecided = isBlock.decide();
        for (final Entry<String, Material> entry : all.entrySet()) {
            final String key = entry.getKey();
            if ((prefixTree == null || prefixTree.hasPrefix(key)) 
                    && suffixTree.hasPrefix(StringUtil.reverse(key))) {
                final Material value = entry.getValue();
                if (isBlock == AlmostBoolean.MAYBE || !(isBlockDecided ^ value.isBlock())) {
                    boolean match = true;
                    for (final String exclude : useExcludeContains) {
                        if (key.contains(exclude)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        res.add(entry.getValue());
                    }
                }
            }
        }
        return res;
    }

    //    public static Set<String> getKeySet() {
    //        return all.keySet();
    //    }

}
