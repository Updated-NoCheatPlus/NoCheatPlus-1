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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache.IBlockCacheNode;

/**
 * Flags and utilities for block-flags.
 * @author asofold
 *
 */
public class BlockFlags {

    /** The Constant blockFlags. */
    protected static final Map<Material, Long> blockFlags = new HashMap<Material, Long>();

    /** Map flag to names. */
    static final Map<Long, String> flagNameMap = new LinkedHashMap<Long, String>();

    /** Map flag name to flag, both names starting with F_... and the name without F_. */
    private static final Map<String, Long> nameFlagMap = new LinkedHashMap<String, Long>();

    private static long f_next = 1;
    private static long f_flag() {
        final long flag;
        synchronized(BlockFlags.class) {
            if (f_next <= 0L) {
                throw new IllegalStateException("No more flags available.");
            }
            flag = f_next;
            f_next *= 2L;
        }
        return flag;
    }

    /** Like stairs. */
    public static final long F_STAIRS                       = f_flag();

    /** Flag to distinguish beds from slimes. */
    public static final long F_BED                          = f_flag();
    
    /** Like liquids. */
    public static final long F_LIQUID                       = f_flag();
    
    /** Kelp-like 1.13+. */
    public static final long F_WATER_PLANT                  = f_flag();

    /** Minecraft isSolid result. Used for setting ground flag - Subject to change / rename. */
    public static final long F_SOLID                        = f_flag();

    /** Compatibility flag: regard this block as passable always. */
    public static final long F_IGN_PASSABLE                 = f_flag();

    /** Like water. */
    public static final long F_WATER                        = f_flag();

    /** Like lava. */
    public static final long F_LAVA                         = f_flag();

    /** 
     * Override bounding box: 1.5 blocks high, like fences.<br>
     *  NOTE: This might have relevance for passable later.
     */
    public static final long F_HEIGHT150                    = f_flag();

    /** The player can stand on these, sneaking or not. */
    public static final long F_GROUND                       = f_flag(); 

    /** 
     * Override bounding box: 1 block height.<br>
     * NOTE: This should later be ignored by passable, rather.
     */
    public static final long F_HEIGHT100                    = f_flag();

    /** Climbable like ladder and vine (allow to land on without taking damage). */
    public static final long F_CLIMBABLE                    = f_flag();

    /** Can actually climb up (like vines, which didn't use to). */
    public static final long F_CLIMBUPABLE                  = f_flag();

    /** 
     * The block can change shape. This is most likely not 100% accurate...<br>
     * More likely to indicate block change shape when stacking up
     */
    public static final long F_VARIABLE                     = f_flag();

    /** Block has full xz-bounds. */
    public static final long F_XZ100                        = f_flag();

    /**
     * This flag indicates that everything between the minimum ground height and
     * the height of the block can also be stood on. See
     * {@link BlockProperties#getGroundMinHeight(BlockCache, int, int, int, IBlockCacheNode, long)}
     * for minimum height.<br>
     * In addition this flag directly triggers a passable workaround for
     * otherwise colliding blocks
     * ({@link BlockProperties#isPassableWorkaround(BlockCache, int, int, int, double, double, double, IBlockCacheNode, double, double, double, double)}).
     */
    public static final long F_GROUND_HEIGHT                = f_flag();

    /** 
     * The height is assumed to decrease from 1.0 with increasing data value from 0 to 0x7, with 0x7 being the lowest.
     * (repeating till 0x15)). 0x8 means falling/full block. This is meant to model flowing water/lava. <br>
     * However the hit-box for collision checks  will be set to 0.5 height or 1.0 height only.
     */
    public static final long F_HEIGHT_8SIM_DEC              = f_flag();

    /**
     * The height increases with data value (8 heights).<br>
     * This is for MC 1.5 snow levels.
     */
    public static final long F_HEIGHT_8_INC                 = f_flag();

    /** All rail types a minecart can move on. */
    public static final long F_RAILS                        = f_flag();

    /** Like ice, slippery. Allows less air friction (which results in higher speed on bunnyhop) */
    public static final long F_ICE                          = f_flag();

    /** Like blue ice, more slippery. Allows less air friction (which results in higher speed on bunnyhop) */
    public static final long F_BLUE_ICE                     = f_flag();

    /** Indicator flag. */
    public static final long F_LEAVES                       = f_flag();

    /** Thin fences: iron bars and glass panes. */
    public static final long F_THIN_FENCE                   = f_flag();

    /** Meta-flag to indicate that the maxXZ edges of an AABB should mean a collision, can be passed to collidesBlock. */
    public static final long F_COLLIDE_EDGES                = f_flag();

    /** Thick fences: actual fences. */
    public static final long F_THICK_FENCE                  = f_flag();
    
    /** Fence gate style with 0x04 being fully passable. */
    public static final long F_PASSABLE_X4                  = f_flag();

    /** Like slime block: bounce back 25% of fall height without taking fall damage [TODO: Check/adjust]. */
    public static final long F_BOUNCE25                     = f_flag();
    
    /** Like the honey block: fall damage is / 5 when landing on this block. Also allows player to stick to its sides with slower falling speed. */
    public static final long F_STICKY                       = f_flag();

    /**
     * The facing direction is described by the lower 3 data bits in order of
     * NSWE, starting at and defaulting to 2, which includes invalid states.
     * Main purpose is ladders, no guarantees on defaults for other blocks yet.
     */
    public static final long F_FACING_LOW3D2_NSWE           = f_flag();

    /**
     * The direction the block is attached to is described by the lower 2 bits
     * in order of SNEW.
     */
    public static final long F_ATTACHED_LOW2_SNEW           = f_flag();

    /** One eighth block height (0.125). */
    public static final long F_HEIGHT8_1                    = f_flag();

    /**
     * Fall distance is divided by 2, if a move goes through this medium (currently only supports liquid).
     */
    public static final long F_FALLDIST_HALF                = f_flag();

    /**
     * Fall distance is set to zero, if a move goes through this medium (currently only supports liquid).
     */
    public static final long F_FALLDIST_ZERO                = f_flag();
    
    /**
     * Minimum height 15/16 (0.9375 = 1 - 0.0625). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT16_15              = f_flag();

    /**
     * Minimum height 14/16 (8750). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT16_14              = f_flag();
    
    /**
     * Minimum height 13/16 (8125). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT16_13              = f_flag();

    /**
     * Minimum height 11/16 (0.6875). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT16_11              = f_flag();

    /**
     * Minimum height 5/8 (0.625). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT8_5                = f_flag();

    /**
     * Minimum height 9/16 (0.5625). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT16_9               = f_flag();

    /**
     * Minimum height 7/16 (0.4375). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT16_7               = f_flag();
    
    /**
     * Minimum height 5/16 (0.3125). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT16_5               = f_flag();

    /**
     * Minimum height 1/4 (0.25). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT4_1                = f_flag();

    /**
     * Minimum height 1/8 (0.125). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT8_1                = f_flag();
    
    /**
     * Minimum height 1/16 (0.0625). <br>
     * Only applies with F_GROUND_HEIGHT set.
     */
    public static final long F_MIN_HEIGHT16_1               = f_flag();

    /** Indicator flag. */
    public static final long F_CARPET                       = f_flag();

    /** Cobweb like blocks (adhesive). */
    public static final long F_COBWEB                       = f_flag();

    /** Like berry bushes: similar to webs but with higher speed (adhesive). */
    public static final long F_BERRY_BUSH                   = f_flag();

    /** Like soul sand: slower speed when walking on this block */
    public static final long F_SOULSAND                     = f_flag();

    /** Indicator flag just to be able to distinguish from beds. Speed is slowed down when walking on this block. */
    public static final long F_SLIME                        = f_flag();

    /**
     * Block change tracking: ordinary right click interaction (use) can change the shape.
     */
    public static final long F_VARIABLE_USE                 = f_flag();

    /**
     * Block change tracking: block redstone events can change the shape.
     */
    public static final long F_VARIABLE_REDSTONE            = f_flag();

    /** Like bubble column. */
    public static final long F_BUBBLE_COLUMN                = f_flag();
    
    /** Flag used to workaround bugged block bounds in older servers for thin fences. */
    public static final long F_FAKEBOUNDS                   = f_flag();
    
    /** Like powder snow: climbable and ground with leather shoes on. */
    public static final long F_POWDER_SNOW = f_flag();

    /** Explicitly set full bounds. */
    public static final long FULL_BOUNDS                    = F_XZ100 | F_HEIGHT100;

    /** SOLID and GROUND set. Treatment of SOLID/GROUND may be changed later. */
    public static final long SOLID_GROUND                   = F_SOLID | F_GROUND;

    /** Full bounds and solid (+ground). */
    public static final long FULLY_SOLID_BOUNDS             = FULL_BOUNDS | SOLID_GROUND;

    // TODO: Convenience constants combining all height / minheight flags
    // TODO: When flags are out, switch to per-block classes :p.

    static {
        // Use reflection to get a flag -> name mapping and vice versa.
        synchronized (BlockFlags.class) {
            for (Field field : BlockFlags.class.getDeclaredFields()) {
                String name = field.getName();
                if (name.startsWith("F_")) {
                    try {
                        Long value = field.getLong(BlockFlags.class);
                        if (flagNameMap.containsKey(value)) {
                            throw new IllegalStateException("Same value for flag names: " + name + " + " + flagNameMap.get(value));
                        }
                        flagNameMap.put(value, name.substring(2));
                        nameFlagMap.put(name, value);
                        nameFlagMap.put(name.substring(2), value);
                    } 
                    catch (IllegalArgumentException | IllegalAccessException e) {}
                }
            }
        }
    }

    public static void setFlag(Material material, long addFlag) {
        try {
            if (!material.isBlock()) {
                // Let's not fail hard here.
                StaticLog.logWarning("Attempt to set flag for a non-block: " + material);
            }
        } 
        catch (Exception e) {}
        blockFlags.put(material, blockFlags.get(material) | addFlag);
    }

    public static void maskFlag(Material material, long addFlag) {
        try {
            if (!material.isBlock()) {
                // Let's not fail hard here.
                StaticLog.logWarning("Attempt to mask flag for a non-block: " + material);
            }
        } 
        catch (Exception e) {}
        blockFlags.put(material, blockFlags.get(material) & addFlag);
    }

    /**
     * Add all flag names for existing default flags to the list.
     *
     * @param flags
     *            the flags
     * @param tags
     *            Flag names will be added to this list (not with the
     *            "F_"-prefix).
     */
    public static void addFlagNames(final long flags, final Collection<String> tags) {
        String tag = flagNameMap.get(flags);
        if (tag != null) {
            tags.add(tag);
            return;
        }
        for (final Long flag : flagNameMap.keySet()) {
            if ((flags & flag) != 0) {
                tags.add(flagNameMap.get(flag));
            }
        }
    }

    /**
     * Return a collection containing all names of the flags.
     *
     * @param flags
     *            the flags
     * @return the flag names
     */
    public static Collection<String> getFlagNames(final Long flags) {
        final ArrayList<String> tags = new ArrayList<String>(flagNameMap.size());
        if (flags == null) {
            return tags;
        }
        addFlagNames(flags, tags);
        return tags;
    }

    /**
     * Get all flag names. Results don't start with the 'F_' prefix.
     * 
     * @return
     */
    public static Collection<String> getAllFlagNames() {
        final Set<String> res = new LinkedHashSet<String>();
        for (final String name : nameFlagMap.keySet()) {
            if (!name.startsWith("F_")) {
                res.add(name);
            }
        }
        return res;
    }

    /**
     * Convenience method to parse a flag.
     *
     * @param input
     *            the input
     * @return the long
     * @throws InputMismatchException
     *             if failed to parse.
     */
    public static long parseFlag(final String input) {
        final String ucInput = input.trim().toUpperCase();
        final Long flag = nameFlagMap.get(ucInput);
        if (flag != null) {
            return flag.longValue();
        }
        try {
            final Long altFlag = Long.parseLong(input);
            return altFlag;
        } catch (NumberFormatException e) {}
        // TODO: This very exception type?
        throw new InputMismatchException();
    }

    /**
     * Gets the block flags.
     *
     * @param blockType
     *            the material
     * @return the block flags
     */
    public static final long getBlockFlags(final Material mat) {
        return blockFlags.containsKey(mat) ? blockFlags.get(mat) : 0;
    }

    /**
     * Gets the block flags.
     *
     * @param id
     *            the id
     * @return the block flags
     */
    public static final long getBlockFlags(final String id) {
        return getBlockFlags(BlockProperties.getMaterial(id));
    }

    /**
     * Sets the block flags.
     *
     * @param material
     *            the material
     * @param flags
     *            the flags
     */
    public static final void setBlockFlags(final Material material, final long flags) {
        try {
            if (!material.isBlock()) {
                // Let's not fail hard here.
                StaticLog.logWarning("Attempt to set flags for a non-block: " + material);
            }
        } 
        catch (Exception e) {}
        blockFlags.put(material, flags);
    }

    /**
     * Sets the block flags.
     *
     * @param id
     *            the id
     * @param flags
     *            the flags
     */
    public static final void setBlockFlags(final String id, final long flags) {
        setBlockFlags(BlockProperties.getMaterial(id), flags);
    }

    /**
     * Set flags of id same as already set with flags for the given material.
     * (Uses BlockFlags.)
     *
     * @param id
     *            the id
     * @param mat
     *            the mat
     */
    public static void setFlagsAs(String id, Material mat) {
        BlockFlags.setBlockFlags(id, BlockFlags.getBlockFlags(mat));
    }

    /**
     * Set flags of id same as already set with flags for the given material.
     * (Uses BlockFlags.)
     *
     * @param id
     *            the id
     * @param otherId
     *            the other id
     */
    public static void setFlagsAs(String id, String otherId) {
        BlockFlags.setBlockFlags(id, BlockFlags.getBlockFlags(otherId));
    }

    /**
     * Set the same flags for newMat as are present for mat.
     * 
     * @param newMat
     * @param mat
     */
    public static void setFlagsAs(Material newMat, Material mat) {
        BlockFlags.setBlockFlags(newMat, BlockFlags.getBlockFlags(mat));
    }

    /**
     * Add flags to existent flags. (Uses BlockFlags.)
     *
     * @param id
     *            Id of the block.
     * @param flags
     *            Block flags.
     */
    public static void addFlags(String id, long flags) {
        BlockFlags.setBlockFlags(id, BlockFlags.getBlockFlags(id) | flags);
    }

    /**
     * Add flags to existent flags. (Uses BlockFlags.)
     *
     * @param mat
     *            Bukkit Material type.
     * @param flags
     *            Block flags.
     */
    public static void addFlags(Material mat, long flags) {
        BlockFlags.setBlockFlags(mat, BlockFlags.getBlockFlags(mat) | flags);
    }

    /**
     * Remove the given flags from existent flags. (Uses BlockFlags.)
     *
     * @param id
     *            the id
     * @param flags
     *            the flags
     */
    public static void removeFlags(String id, long flags) {
        BlockFlags.setBlockFlags(id, BlockFlags.getBlockFlags(id) & ~flags);
    }
    
    /**
     * Remove the given flags from existent flags. (Uses BlockFlags.)
     *
     * @param mat
     *            Bukkit Material type.
     * @param flags
     *            Block flags.
     */
    public static void removeFlags(Material mat, long flags) {
        BlockFlags.setBlockFlags(mat, BlockFlags.getBlockFlags(mat) & ~flags);
    }

    /**
     * Test if any flags within testFlags are contained.
     * 
     * @param flags
     * @param testFlags
     * @return
     */
    public static boolean hasAnyFlag(long flags, long testFlags) {
        return (flags & testFlags) != 0L;
    }

    /**
     * Test if all flags within testFlags are contained.
     * 
     * @param flags
     * @param testFlags
     * @return
     */
    public static boolean hasAllFlags(long flags, long testFlags) {
        return (flags & testFlags) == testFlags;
    }

    /**
     * Test if no flags within testFlags are contained.
     * 
     * @param flags
     * @param testFlags
     * @return
     */
    public static boolean hasNoFlags(long flags, long testFlags) {
        return (flags & testFlags) == 0L;
    }

    /** 
     * Override flags to a fully solid block (set explicitly). 
     * 
     * @param blockId
     * @return
     */
    public static void setFullySolidFlags(String blockId) {
        BlockFlags.setBlockFlags(blockId, FULL_BOUNDS | SOLID_GROUND);
    }
}