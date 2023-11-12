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

import fr.neatmonster.nocheatplus.utilities.map.BlockCache;

public class BukkitBamboo implements BukkitShapeModel {

    @Override
    public double[] getShape(final BlockCache blockCache, final World world, final int x, final int y, final int z) {
        
        // Taken from NMS - Offset/Noise
        long i = (x * 3129871L) ^ z * 116129781L ^ 0;
        i = i * i * 42317861L + i * 11L;
        i = i >> 16;
        final double xOffset = (((i & 15L) / 15.0F) - 0.5D) * 0.5D;
        final double zOffset = (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D;
	    return new double[] {0.40625 + xOffset, 0.0, 0.40625 + zOffset, 0.59375 + xOffset, 1.0, 0.59375 + zOffset};
    }

    @Override
    public int getFakeData(final BlockCache blockCache, final World world, final int x, final int y, final int z) {
        return 0;
    }
}