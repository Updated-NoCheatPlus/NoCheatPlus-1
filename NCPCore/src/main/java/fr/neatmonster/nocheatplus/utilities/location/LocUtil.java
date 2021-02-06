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
package fr.neatmonster.nocheatplus.utilities.location;

import fr.neatmonster.nocheatplus.logging.StaticLog;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import fr.neatmonster.nocheatplus.components.location.IGetBlockPosition;
import fr.neatmonster.nocheatplus.components.location.IGetLocationWithLook;
import fr.neatmonster.nocheatplus.components.location.IGetPosition;
import fr.neatmonster.nocheatplus.components.location.IGetPositionWithLook;
import fr.neatmonster.nocheatplus.components.location.ISetPositionWithLook;

import java.util.Arrays;
import java.util.List;

/**
 * Auxiliary methods for Location handling, mainly intended for use with
 * set back locations.
 * 
 * @author asofold
 *
 */
public class LocUtil {

    public static final List<Material> GlitchBlocks = Arrays.asList(
            Material.AIR, Material.LAVA, Material.WATER, Material.GRASS,
            Material.VINE, Material.SEAGRASS, Material.TALL_SEAGRASS,
            Material.SNOW, Material.TALL_GRASS, Material.FIRE, Material.VOID_AIR,
            Material.BEDROCK, Material.END_PORTAL_FRAME, Material.POPPY,
            Material.LARGE_FERN, Material.SWEET_BERRY_BUSH, Material.DANDELION,
            Material.CAVE_AIR, Material.FERN);

    public static int hashCode(final Location location) {
        final World world = location.getWorld();
        return hashCode(world == null ? null : world.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static int hashCode(final IGetLocationWithLook location) {
        return hashCode(location.getWorldName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public static int hashCode(final IGetPositionWithLook location) {
        return hashCode(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /**
     * Internal standard for hashing locations like this. Hash of world name XOR
     * hashCode(x, y, ...).
     * 
     * @param worldName
     * @param x
     * @param y
     * @param z
     * @param yaw
     * @param pitch
     * @return
     */
    public static int hashCode(final String worldName, final double x, final double y, final double z, final float yaw, final float pitch) {
        return (worldName == null ? 0 : worldName.hashCode()) ^ hashCode(x, y, z, yaw, pitch);
    }

    /**
     * Implementation partly copied from the Bukkit API
     * (org.bukkit.bukkit.Location#hashCode).
     * 
     * @param x
     * @param y
     * @param z
     * @param yaw
     * @param pitch
     * @return
     */
    public static int hashCode(final double x, final double y, final double z, final float yaw, final float pitch) {
        int hash = 3;
        // Left out the world hash.
        hash = 19 * hash + (int) (Double.doubleToLongBits(x) ^ Double.doubleToLongBits(x) >>> 32);
        hash = 19 * hash + (int) (Double.doubleToLongBits(y) ^ Double.doubleToLongBits(y) >>> 32);
        hash = 19 * hash + (int) (Double.doubleToLongBits(z) ^ Double.doubleToLongBits(z) >>> 32);
        hash = 19 * hash + Float.floatToIntBits(pitch);
        hash = 19 * hash + Float.floatToIntBits(yaw);
        return hash;
    }

    /**
     * Get a Location instance for the given position in the given world.
     * 
     * @param world
     * @param loc
     * @return
     */
    public static Location getLocation(final World world, final IGetPositionWithLook loc) {
        return new Location(world, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    /**
     * Set loc to the given world and pos.
     * 
     * @param loc
     * @param world
     * @param pos
     * @return The given Location instance (loc), updated by the world and pos.
     * @throws NullPointerException
     *             if World is null.
     */
    public static Location set(final Location loc, final World world, final IGetPositionWithLook pos) {
        loc.setWorld(testWorld(world));
        loc.setX(pos.getX());
        loc.setY(pos.getY());
        loc.setZ(pos.getZ());
        loc.setYaw(pos.getYaw());
        loc.setPitch(pos.getPitch());
        return loc;
    }

    /**
     * Set pos to coordinates and looking direction contained in loc.
     * 
     * @param pos
     * @param loc
     * @return The given pos, updated by position and look from loc.
     */
    public static ISetPositionWithLook set(final ISetPositionWithLook pos, final Location loc) {
        pos.setX(loc.getX());
        pos.setY(loc.getY());
        pos.setZ(loc.getZ());
        pos.setYaw(loc.getYaw());
        pos.setPitch(loc.getPitch());
        return pos;
    }

    /**
     * Get a copy of a location (not actually using cloning).
     * 
     * @param loc
     * @return A new Location instance.
     * @throws NullPointerException
     *             if World is null.
     */
    public static final Location clone(final Location loc){
        return new Location(testWorld(loc.getWorld()), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    /**
     * Get a copy of a location (not actually using cloning), override yaw and
     * pitch with given values.
     * 
     * @param loc
     * @param yaw
     * @param pitch
     * @return A new Location instance.
     * @throws NullPointerException
     *             if the resulting world is null.
     */
    public static final Location clone(final Location loc, final float yaw, final float pitch){
        return new Location(testWorld(loc.getWorld()), loc.getX(), loc.getY(), loc.getZ(), yaw, pitch);
    }

    /**
     * Clone setBack, with yaw and pitch taken from ref, if setBack is null, ref
     * is cloned fully.
     * 
     * @param setBack
     *            Can be null.
     * @param ref
     *            Must not be null.
     * @return A new Location instance.
     * @throws NullPointerException
     *             if the resulting world is null.
     */
    public static final Location clone(final Location setBack, final Location ref) {
        if (setBack == null){
            return clone(ref);
        }
        else{
            return clone(setBack, ref.getYaw(), ref.getPitch());
        }
    }

    /**
     * Clone setBack, with yaw and pitch taken from ref, if setBack is null, ref
     * is cloned fully.
     * 
     * @param setBack
     * @param ref
     * @return
     */
    public static final Location clone(final Location setBack, final RichBoundsLocation ref) {
        if (setBack == null) {
            return ref.getLocation();
        }
        else{
            return clone(setBack, ref.getYaw(), ref.getPitch());
        }
    }

    /**
     * Update setBack by loc.
     * 
     * @param setBack
     * @param loc
     * @throws NullPointerException
     *             if loc.getWorld() is null.
     */
    public static final void set(final Location setBack, final Location loc) {
        setBack.setWorld(testWorld(loc.getWorld()));
        setBack.setX(loc.getX());
        setBack.setY(loc.getY());
        setBack.setZ(loc.getZ());
        setBack.setYaw(loc.getYaw());
        setBack.setPitch(loc.getPitch());
        Block block = setBack.getBlock();
        if (!GlitchBlocks.contains(block.getType())) {
            StaticLog.logInfo("Removing burrow block for player at "+block.getX()+" "+block.getY()+" "+block.getZ()+" TYPE: "+block.getType()+" WORLD: "+loc.getWorld().getName());
            setBack.getBlock().setType(Material.AIR);
        }
    }

    /**
     * Update setBack by loc.
     * 
     * @param setBack
     * @param loc
     * @throws NullPointerException
     *             if loc.getWorld() is null.
     */
    public static final void set(final Location setBack, final RichBoundsLocation loc) {
        setBack.setWorld(testWorld(loc.getWorld()));
        setBack.setX(loc.getX());
        setBack.setY(loc.getY());
        setBack.setZ(loc.getZ());
        setBack.setYaw(loc.getYaw());
        setBack.setPitch(loc.getPitch());
    }

    /**
     * Throw a NullPointerException if world is null.
     * 
     * @param world
     * @return
     */
    private static World testWorld(final World world) {
        if (world == null) {
            throw new NullPointerException("World must not be null.");
        } else {
            return world;
        }
    }

    /**
     * Quick out of bounds check for yaw (-360 < yaw < 360).
     * 
     * @param yaw
     * @return
     */
    public static final boolean needsYawCorrection2(final float yaw) {
        return Float.isNaN(yaw) || yaw <= -360f || yaw >= 360f;
    }

    /**
     * Quick out of bounds check for yaw.
     * 
     * @param yaw
     * @return
     */
    public static final boolean needsYawCorrection(final float yaw) {
        return Float.isNaN(yaw) || yaw < 0f || yaw >= 360f;
    }

    /**
     * Quick out of bounds check for pitch.
     * 
     * @param pitch
     * @return
     */
    public static final boolean needsPitchCorrection(final float pitch) {
        return Float.isNaN(pitch) || pitch < -90f || pitch > 90f;
    }

    /**
     * Check for NaN, infinity.
     *
     * @param floats
     *            the floats
     * @return true, if is bad coordinate
     */
    public static boolean isBadCoordinate(float ... floats) {
        for (int i = 0; i < floats.length; i++) {
            if (Float.isNaN(floats[i]) || Float.isInfinite(floats[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for NaN, infinity, some random maximum coordinate.
     *
     * @param doubles
     *            the doubles
     * @return true, if is bad coordinate
     */
    public static boolean isBadCoordinate(double ... doubles) {
        for (int i = 0; i < doubles.length; i++) {
            final double x = doubles[i];
            if (Double.isNaN(x) || Double.isInfinite(x) || Math.abs(x) > 3.2E7D) {
                return true;
            }
        }
        return false;
    }

    /**
     * Quick out of bounds check for yaw and pitch.
     * 
     * @param yaw
     * @param pitch
     * @return
     */
    public static final boolean needsDirectionCorrection(final float yaw, final float pitch) {
        return needsYawCorrection(yaw) || needsPitchCorrection(pitch);
    }

    /**
     * Quick out of bounds check for yaw and pitch.
     * 
     * @param yaw
     * @param pitch
     * @return
     */
    public static final boolean needsDirectionCorrection2(final float yaw, final float pitch) {
        return needsYawCorrection2(yaw) || needsPitchCorrection(pitch);
    }

    /**
     * Ensure 0 <= yaw < 360.
     * 
     * @param yaw
     * @return
     */
    public static final float correctYaw(float yaw) {
        if (Float.isNaN(yaw)) {
            return 0f;
        }
        if (yaw >= 360f) {
            if (yaw > 10000f) {
                yaw = 0f;
            } else {
                while (yaw > 360f) {
                    yaw -= 360f;
                }
            }
        }
        if (yaw < 0f) {
            if (yaw < -10000f) {
                yaw = 0f;
            } else {
                while (yaw < 0f) {
                    yaw += 360f;
                }
            }
        }
        return yaw;
    }

    /**
     * Ensure -360 < yaw < 360.
     * 
     * @param yaw
     * @return
     */
    public static final float correctYaw2(float yaw) {
        if (Float.isNaN(yaw) || yaw == Float.NEGATIVE_INFINITY || yaw == Float.POSITIVE_INFINITY) {
            return 0f;
        }
        if (yaw >= 360f) {
            yaw -= 360f * Math.floor(yaw / 360f);
        }
        if (yaw <= -360f) {
            yaw -= 360f * Math.ceil(yaw / 360f);
        }
        return yaw;
    }

    /**
     * Ensure -90 <= pitch <= 90.
     * 
     * @param pitch
     * @return
     */
    public static final float correctPitch(float pitch) {
        if (Float.isNaN(pitch)) {
            return 0f;
        } else if (pitch < -90f) {
            return -90f;
        } else if (pitch > 90f) {
            return 90f;
        } else {
            return pitch;
        }
    }

    /**
     * Get the world from the first location that is not null and has a non-null
     * world.
     * 
     * @param from
     * @param to
     * @return
     */
    public static World getFirstWorld(final Location... locs) {
        for (int i = 0; i < locs.length; i++) {
            final Location loc = locs[i];
            if (loc != null) {
                final World world = loc.getWorld();
                if (world != null) {
                    return world;
                }
            }
        }
        return null;
    }

    /**
     * Format like Location.toString, but without extras like the world name.
     * 
     * @param loc
     * @return
     */
    public static String simpleFormat(final Location loc) {
        return "x=" + loc.getX() + ",y=" + loc.getY() + ",z=" + loc.getZ() + ",pitch=" + loc.getPitch() + ",yaw=" + loc.getYaw();
    }

    /**
     * Format like Location.toString, but without extras like the world name.
     * 
     * @param loc
     * @return
     */
    public static String simpleFormat(final IGetPositionWithLook loc) {
        return "x=" + loc.getX() + ",y=" + loc.getY() + ",z=" + loc.getZ() + ",pitch=" + loc.getPitch() + ",yaw=" + loc.getYaw();
    }

    /**
     * Format like Location.toString, but without extras like the world name.
     * 
     * @param loc
     * @return
     */
    public static String simpleFormat(final IGetPosition loc) {
        return "x=" + loc.getX() + ",y=" + loc.getY() + ",z=" + loc.getZ();
    }

    /**
     * Just the coordinates, no world/yaw/pitch.
     * @param loc
     * @return
     */
    public static String simpleFormatPosition(Location loc) {
        return "x=" + loc.getX() + ",y=" + loc.getY() + ",z=" + loc.getZ();
    }

    /**
     * Just the coordinates.
     * @param block
     * @return
     */
    public static String simpleFormat(Block block) {
        return "x=" + block.getX() + ",y=" + block.getY() + ",z=" + block.getZ();
    }

    /**
     * Just the coordinates.
     * @param block
     * @return
     */
    public static String simpleFormatBlock(IGetBlockPosition block) {
        return "x=" + block.getBlockX() + ",y=" + block.getBlockY() + ",z=" + block.getBlockZ();
    }

}
