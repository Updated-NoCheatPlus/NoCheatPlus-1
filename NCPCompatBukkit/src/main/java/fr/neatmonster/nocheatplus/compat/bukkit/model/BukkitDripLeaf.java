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
package fr.neatmonster.nocheatplus.compat.bukkit.model;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.BigDripleaf;

import fr.neatmonster.nocheatplus.utilities.map.BlockCache;

public class BukkitDripLeaf implements BukkitShapeModel {

    @Override
    public double[] getShape(BlockCache blockCache, World world, int x, int y, int z) {
        final Block block = world.getBlockAt(x, y, z);
        final BlockData blockData = block.getState().getBlockData();
        double[] res = {0.0, 0.6875, 0.0, 1.0, 0.9375, 1.0};
        if (blockData instanceof BigDripleaf) {
            final BigDripleaf dripLeaf = (BigDripleaf) blockData;
            switch(dripLeaf.getTilt()) {
                case PARTIAL:
                    res[4] -= 0.125;
                    return res;
                case UNSTABLE:
                    // WORKAROUND: Zero this bounding box if the tilt is unstable [NCP should take care of ground judgement then]
                    return new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
                case NONE:
                    return res;
                default:
                    break;
            }
        }
        return null;
    }

    @Override
    public int getFakeData(BlockCache blockCache, World world, int x, int y, int z) {
        return 0;
    }
}