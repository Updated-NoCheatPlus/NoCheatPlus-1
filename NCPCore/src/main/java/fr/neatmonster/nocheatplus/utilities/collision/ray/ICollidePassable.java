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

/**
 * Test for collision testing for passing through blocks.
 * 
 * @author asofold
 *
 */
public interface ICollidePassable extends ICollideBlocks, ISetMargins {
    // TODO: Add a super interface (+ asbtract impl.) for BlockCache based stuff including BlockChangeTracker.

    public void setBlockCache(BlockCache blockCache);
    public BlockCache getBlockCache();

    /**
     * Allows testing for past states. Should be reset in cleanup. Will do
     * nothing, if not supported.
     * <hr>
     * <b>Method signature subject to change (tick).</b>
     * 
     * @param blockChangeTracker
     * @param blockChangeReference
     * @param tick
     *            The tick right now (timing info).
     * @param worldId
     *            the UUID of the world this takes place in.
     */
    public void setBlockChangeTracker(BlockChangeTracker blockChangeTracker, 
            BlockChangeReference blockChangeReference, int tick, UUID worldId);

    /**
     * Set the axis checking order, length must be 3, use Axis.NONE for
     * skipping.
     * 
     * @param axisOrder
     * @throws UnsupportedOperationException
     *             If setting the order of axes is not supported.
     */
    public void setAxisOrder(List<Axis> axisOrder);

    /**
     * Convenience: Call set and setBlockCache with the data from the
     * PlayerLocation instances. Should use from.getBlockCache() as BlockCache
     * instance.
     * 
     * @param from
     * @param to
     */
    public void set(final PlayerLocation from, final PlayerLocation to);

    /**
     * Indicate if extra workarounds may be necessary, such as running y-axis
     * collision before xz-axes collision in case the default run yields a
     * collision.
     * 
     * @return Return false in order to prevent workarounds with split by axis
     *         checking. Typically should return true with ray-tracing and false
     *         with axis-tracing.
     */
    public boolean mightNeedSplitAxisHandling();

    /**
     * Remove reference to objects passed from outside (BlockCache,
     * BlockChangeTracker and similar, but not calling their cleanup methods).
     * Should not reset previously set collision information.
     */
    public void cleanup();
    
}
