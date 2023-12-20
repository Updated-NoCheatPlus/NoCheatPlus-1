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
package fr.neatmonster.nocheatplus.utilities.collision;

import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

public class InteractAxisTracing extends AxisTracing {

    protected BlockCache blockCache = null;

    protected final boolean strict;

    public InteractAxisTracing() {
        this(false);
    }

    public InteractAxisTracing(boolean strict) {
        super();
        this.strict = strict;
    }

    public BlockCache getBlockCache() {
        return blockCache;
    }

    public void setBlockCache(BlockCache blockCache) {
        this.blockCache = blockCache;
    }

    /**
     * 
     * @param x0
     * @param y0
     * @param z0
     * @param x1
     * @param y1
     * @param z1
     */
    public void set(double x0, double y0, double z0, double x1, double y1, double z1) {
        super.set(x0, y0, z0, x1, y1, z1);
        collides = false;
    }

    /**
     * Remove reference to BlockCache.
     */
    public void cleanup() {
        if (blockCache != null) {
            blockCache = null;
        }
    }

    @Override
    protected boolean step(final int blockX, final int blockY, final int blockZ, 
            final double minX, final double minY, final double minZ, 
            final double maxX, final double maxY, final double maxZ, 
            final Axis axis, final int increment) {
        if (BlockProperties.isPassableBox(blockCache, blockX, blockY, blockZ, minX, minY, minZ, maxX, maxY, maxZ)) {
            return true;
        }
        // No condition for passing through found.
        collides = true;
        return false;
    }

    @Override
    protected void collectInitiallyCollidingBlocks(double minX, double minY, double minZ, double maxX, double maxY,
            double maxZ, BlockPositionContainer results) {
        BlockProperties.collectInitiallyCollidingBlocks(blockCache, minX, minY, minZ, maxX, maxY, maxZ, results);
    }
}
