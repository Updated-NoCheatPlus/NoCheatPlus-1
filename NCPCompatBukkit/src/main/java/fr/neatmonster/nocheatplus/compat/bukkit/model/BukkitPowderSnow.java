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

import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;

public class BukkitPowderSnow implements BukkitShapeModel {

    @Override
    public double[] getShape(BlockCache blockCache, World world, int x, int y, int z) {
        IPlayerData pData = blockCache.getPlayerData();
        if (pData != null) {
            MovingData data = pData.getGenericInstance(MovingData.class);
            // TODO: Make NoFall no dealing damage on this block
            if (data.lastY > (y + 1 - 1e-5) && data.hasLeatherBoots) {// && !pData.isSneaking()
                return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
            }
            // Give up, too hard to properly implement, workaround instead
            //if (data.noFallFallDistance > 2.5) {
            //    return new double[] {0.0, 0.0, 0.0, 1.0, 0.9, 1.0};
            //}
        }
        return new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    }

    @Override
    public int getFakeData(BlockCache blockCache, World world, int x, int y, int z) {
        return 0;
    }

}
