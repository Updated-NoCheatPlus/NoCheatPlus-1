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
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;

import fr.neatmonster.nocheatplus.utilities.map.BlockCache;

public class BukkitSnow implements BukkitShapeModel {
    
    private static final double[][] SNOW_LAYERS = {
            null,
            {0.0, 0.0, 0.0, 1.0, 0.125, 1.0},
            {0.0, 0.0, 0.0, 1.0, 0.250, 1.0},
            {0.0, 0.0, 0.0, 1.0, 0.375, 1.0},
            {0.0, 0.0, 0.0, 1.0, 0.5, 1.0},
            {0.0, 0.0, 0.0, 1.0, 0.625, 1.0},
            {0.0, 0.0, 0.0, 1.0, 0.75, 1.0},
            {0.0, 0.0, 0.0, 1.0, 0.875, 1.0},
            {0.0, 0.0, 0.0, 1.0, 1.0, 1.0}
    };

    @Override
    public double[] getShape(BlockCache blockCache, World world, int x, int y, int z) {
        // TODO: Backward Handling
        final Block block = world.getBlockAt(x, y, z);
        final BlockState state = block.getState();
        final BlockData blockData = state.getBlockData();
        if (blockData instanceof Snow) {
            return SNOW_LAYERS[((Snow)blockData).getLayers() - 1];
        }
        return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
    }

    @Override
    public int getFakeData(BlockCache blockCache, World world, int x, int y, int z) {
        // final Block block = world.getBlockAt(x, y, z);
        // final BlockState state = block.getState();
        // final BlockData blockData = state.getBlockData();
        // if (blockData instanceof Snow) {
        //     return ((Snow)blockData).getLayers() - 1;
        // }
        return 0;
    }
}