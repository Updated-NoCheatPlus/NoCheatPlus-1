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

import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import org.bukkit.World;

import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bamboo;

public class BukkitBamboo implements BukkitShapeModel {

    @Override
    public double[] getShape(final BlockCache blockCache, final World world, final int x, final int y, final int z) {
        if (blockCache.isBedrockCache()) {
            final Block block = world.getBlockAt(x, y, z);
            final BlockData blockData = block.getBlockData();
            if (blockData instanceof Bamboo) {
                Bamboo b = (Bamboo) blockData;
                double thickness = 0.125;
                if (b.getAge() > 0) thickness += 0.0625;
                long a = LocUtil.randomSeedBedrock(x, 0, z);
                long a1 = a * 2863311531L;
                long rdx = (a1 >> 32) & 0xFFFFFFFFL;
                //long rax = a1 & 0xFFFFFFFFL;
                rdx >>=3;rdx *=3;
                rdx = (rdx << 2) & 0xFFFFFFFFL;
                double xOffset = a - rdx + 1;

                a >>=8;
                long a2 = a;
                a *= 2863311531L;
                rdx = (a >> 32) & 0xFFFFFFFFL;
                rdx >>=3;rdx *=3;
                rdx = (rdx << 2) & 0xFFFFFFFFL;
                double zOffset = a2 - rdx +1;
                xOffset *= 0.0625;
                zOffset *= 0.0625;
                return new double[] {xOffset, 0, zOffset, xOffset + thickness, 1.0, zOffset + thickness};
            }
        }
        long i = LocUtil.randomSeedJava(x, 0, z);
        final double xOffset = (((i & 15L) / 15.0F) - 0.5D) * 0.5D;
        final double zOffset = (((i >> 8 & 15L) / 15.0F) - 0.5D) * 0.5D;
	return new double[] {0.40625 + xOffset, 0.0, 0.40625 + zOffset, 0.59375 + xOffset, 1.0, 0.59375 + zOffset};
    }

    @Override
    public int getFakeData(final BlockCache blockCache, final World world, final int x, final int y, final int z) {
        return 0;
    }
}