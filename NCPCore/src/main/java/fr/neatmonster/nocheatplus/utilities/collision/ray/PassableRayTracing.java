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

import java.util.List;
import java.util.UUID;

import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeReference;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.utilities.collision.Axis;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

public class PassableRayTracing extends RayTracing implements ICollidePassable {

    protected BlockCache blockCache = null;

    @Override
    public BlockCache getBlockCache() {
        return blockCache;
    }

    @Override
    public void setBlockCache(BlockCache blockCache) {
        this.blockCache = blockCache;
    }

    @Override
    public void setBlockChangeTracker(BlockChangeTracker blockChangeTracker,
            BlockChangeReference blockChangeReference, int tick, UUID worldId) {
        // (Not supported.)
    }

    @Override
    public void set(final PlayerLocation from, final PlayerLocation to){
        set(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());
        setBlockCache(from.getBlockCache()); // TODO: This might better be done extra.
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.utilities.RayTracing#set(double, double, double, double, double, double)
     */
    @Override
    public void set(double x0, double y0, double z0, double x1, double y1, double z1) {
        super.set(x0, y0, z0, x1, y1, z1);
        collides = false;
    }

    @Override
    public void cleanup(){
        blockCache = null;
    }

    @Override
    protected boolean step(final int blockX, final int blockY, final int blockZ, final double oX, final double oY, final double oZ, final double dT, final boolean isPrimary) {
        // Check if initially colliding blocks are meant to be skipped.
        if (isPrimary && step == 1 && ignoreInitiallyColliding 
                && !BlockProperties.isPassable(blockCache, 
                        oX + blockX, oY + blockY, oZ + blockZ, 
                        blockCache.getOrCreateBlockCacheNode(blockX, blockY, blockZ, false), null)){
            return true;
        }
        // Actual collision check for this block vs. the move.
        if (BlockProperties.isPassableRay(blockCache, blockX, blockY, blockZ, oX, oY, oZ, dX, dY, dZ, dT)){
            return true;
        }
        else{
            collides = true;
            return false;
        }
    }

    @Override
    public boolean mightNeedSplitAxisHandling() {
        return true;
    }

    @Override
    public void setMargins(double height, double xzMargin) {
        // (No effect.)
    }

    @Override
    public void setCutOppositeDirectionMargin(boolean cutOppositeDirectionMargin) {
        // (No effect.)
    }

    @Override
    public void setAxisOrder(List<Axis> axisOrder) {
        throw new UnsupportedOperationException();
    }

}
