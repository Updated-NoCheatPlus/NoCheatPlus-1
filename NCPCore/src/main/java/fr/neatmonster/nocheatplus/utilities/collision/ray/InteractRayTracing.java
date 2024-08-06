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
package fr.neatmonster.nocheatplus.utilities.collision.ray;

import org.bukkit.Location;
import org.bukkit.Material;

import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.FakeBlockCache;



/**
 * Rough ray-tracing for interaction with something. This does not do any smart end-point guessing.
 * @author mc_dev
 *
 */
public class InteractRayTracing extends RayTracing {

    //    private static final int[][] incr = new int[][]{
    //        {1, 0, 0},
    //        {0, 1, 0},
    //        {0, 0, 1},
    //        {-1, 0, 0},
    //        {0, -1, 0},
    //        {0, 0, -1},
    //    };

    protected BlockCache blockCache = null;

    protected final boolean strict;

    protected int lastBx, lastBy, lastBz;

    protected int targetX, targetY, targetZ;

    public InteractRayTracing() {
        this(false);
    }

    public InteractRayTracing(boolean strict) {
        super();
        this.strict = strict;
        this.forceStepEndPos = false; // Not needed here.
    }

    public BlockCache getBlockCache() {
        return blockCache;
    }

    public void setBlockCache(BlockCache blockCache) {
        this.blockCache = blockCache;
    }

    public void set(double x0, double y0, double z0, double x1, double y1, double z1) {
        set(x0, y0, z0, x1, y1, z1, Location.locToBlock(x1), Location.locToBlock(y1), Location.locToBlock(z1));
        // Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * 
     * @param x0
     * @param y0
     * @param z0
     * @param x1
     * @param y1
     * @param z1
     * @param targetX The block clicked/interacted with (can be different to the end point of ray-tracing, or ignored with Integer.MAX_VALUE).
     * @param targetY
     * @param targetZ
     */
    public void set(double x0, double y0, double z0, double x1, double y1, double z1, int targetX, int targetY, int targetZ) {
        super.set(x0, y0, z0, x1, y1, z1);
        collides = false;
        lastBx = blockX;
        lastBy = blockY;
        lastBz = blockZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    /**
     * Remove reference to BlockCache.
     */
    public void cleanup() {
        if (blockCache != null) {
            blockCache = null;
        }
    }

    /**
     * Simplistic collision check (can interact through this block).
     * @param blockX
     * @param blockY
     * @param blockZ
     * @return
     */
    private boolean doesCollide(final int blockX, final int blockY, final int blockZ) {
        final Material mat = blockCache.getType(blockX, blockY, blockZ);
        final long flags = BlockFlags.getBlockFlags(mat);
        if ((flags & BlockFlags.F_SOLID) == 0) {
            // Ignore non solid blocks anyway.
            return false;
        }
        if ((flags & (BlockFlags.F_LIQUID | BlockFlags.F_IGN_PASSABLE | BlockFlags.F_STAIRS | BlockFlags.F_VARIABLE)) != 0) {

            // Special cases.
            // TODO: F_VARIABLE: Bounding boxes are roughly right ?
            return false;
        }
        if (!blockCache.isFullBounds(blockX, blockY, blockZ)) {
            return false;
        }
        return true;
    }

    /**
     * Test if the primary line is on the block interacted with (may be a
     * different one that the end point of ray-tracing).
     * 
     * @return
     */
    public boolean isTargetBlock() {
        return targetX != Integer.MAX_VALUE && blockX == targetX && blockY == targetY && blockZ == targetZ;
    }

    //    /**
    //     * Check if the block may be interacted through by use of some workaround.
    //     * 
    //     * @param blockX
    //     * @param blockY
    //     * @param blockZ
    //     * @return
    //     */
    //    private boolean allowsWorkaround(final int blockX, final int blockY, final int blockZ) {
    //        
    //        // TODO: Recode this/other.
    //        
    //        // TODO: This could allow some bypasses for "strange" setups.
    //        // TODO: Consider using distance to target as heuristic ? [should not get smaller !?]
    //        // TODO: Consider (min/max) offset for distance.
    //        final int dX = blockX - lastBx;
    //        final int dY = blockY - lastBy;
    //        final int dZ = blockZ - lastBz;
    //        final double dSq = dX * dX + dY * dY + dZ * dZ;
    //        // TODO: Limit distance more here !? 
    //        for (int i = 0; i < 6; i++) {
    //            final int[] dir = incr[i];
    //            final int rX = blockX + dir[0];
    //            if (Math.abs(lastBx - rX) > 1) {
    //                continue;
    //            }
    //            final int rY = blockY + dir[1];
    //            if (Math.abs(lastBy - rY) > 1) {
    //                continue;
    //            }
    //            final int rZ = blockZ + dir[2];
    //            if (Math.abs(lastBz - rZ) > 1) {
    //                continue;
    //            }
    //            final int dRx = rX - lastBx;
    //            final int dRy = rY - lastBy;
    //            final int dRz = rZ - lastBz;
    //            if (dRx * dRx + dRy * dRy + dRz * dRz <= dSq) {
    //                continue;
    //            }
    //            if (!doesCollide(rX, rY, rZ)) {
    //                // NOTE: Don't check "rX == targetBx && rZ == targetBz && rY == targetBy".
    //                return true;
    //            }
    //        }
    //        return false;
    //    }

    @Override
    protected boolean step(final int blockX, final int blockY, final int blockZ, final double oX, final double oY, final double oZ, final double dT, final boolean isPrimary) {
        // TODO: Make an optional, more precise check (like passable) ?
        // TODO: isEndBlock -> blockInteractedWith, because the offset edge might be on the next block.
        // TODO: isTargetBlock checks the primary line (!, might be ok.).
        if (isTargetBlock() || !doesCollide(blockX, blockY, blockZ)) {
            if (isPrimary) {
                lastBx = blockX;
                lastBy = blockY;
                lastBz = blockZ;
            }
            return true;
        }
        //        if (strict || blockX == lastBx && blockZ == lastBz && blockY == lastBy) {
        //            collides = true;
        //            return false;
        //        }
        //        // Check workarounds...
        //        if (isPrimary && allowsWorkaround(blockX, blockY, blockZ)) {
        //            lastBx = blockX;
        //            lastBy = blockY;
        //            lastBz = blockZ;
        //            return true;
        //        }
        // No workaround found.
        collides = true;
        return false;
    }

    /**
     * Get a directly usable test case (in a closure), for copy and paste to TestInteractRayTracing.
     * @param captureMargin
     * @param expectCollide
     * @return
     */
    public String getTestCase(double captureMargin, boolean expectCollide) {
        FakeBlockCache recorder = new FakeBlockCache();
        recorder.set(this.blockCache, x0, y0, z0, x0 + dX, y0 + dY, z0 + dZ, captureMargin);
        StringBuilder builder = new StringBuilder(10000);
        // Add everything inside a closure for direct copy and paste.
        builder.append('{');
        // Set up the block cache.
        recorder.toJava(builder, "fbc", "");
        // Add the test case code.
        builder.append("InteractRayTracing rt = new CenteredInteractRayTracing(false, " + targetX + ", " + targetY + ", " + targetZ + "); rt.setBlockCache(fbc);");
        builder.append("TestRayTracing.runCoordinates(rt, new double[]{" + x0 + ", " + y0 + ", " + z0 + ", " + (x0 + dX) + ", " + (y0 + dY) + ", " + (z0 + dZ) + "}, " + expectCollide + ", " + !expectCollide + ", 0.0, false, \"ingame\");");
        builder.append("rt.cleanup(); fbc.cleanup();");
        builder.append('}');
        recorder.cleanup();
        return builder.toString();
    }

}
