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
package fr.neatmonster.nocheatplus.compat.bukkit;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import fr.neatmonster.nocheatplus.compat.bukkit.model.BukkitShapeModel;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;


/**
 * BlockCache for MCAccessBukkitModern.
 * 
 * @author asofold
 *
 */
public class BlockCacheBukkitModern extends BlockCacheBukkit {
    
    private Map<Material, BukkitShapeModel> shapeModels;
    
    public BlockCacheBukkitModern(Map<Material, BukkitShapeModel> shapeModels) {
        super(null);
        this.shapeModels = shapeModels;
    }
    
    public BlockCacheBukkitModern(World world) {
        super(world);
    }
    
    @Override
    public int fetchData(int x, int y, int z) {
        Material mat = getType(x, y, z);
        final BukkitShapeModel shapeModel = shapeModels.get(mat);
        if (shapeModel != null) {
            final int data = shapeModel.getFakeData(this, world, x, y, z);
            if (data != Integer.MAX_VALUE) {
                return data;
            }
        }
        return super.fetchData(x, y, z);
    }
    
    @Override
    public double[] fetchBounds(int x, int y, int z) {
        // TODO: Fetch what's possible to fetch/guess (...).
        // TODO: Consider to store the last used block/stuff within BlockCacheBukkit already.
        Material mat = getType(x, y, z);
        final BukkitShapeModel shapeModel = shapeModels.get(mat);
        if (shapeModel == null) {
            return super.fetchBounds(x, y, z);
        }
        return shapeModel.getShape(this, world, x, y, z);
    }
    
    @Override
    public boolean standsOnEntity(final Entity entity, final double minX, final double minY, final double minZ, final double maxX, final double maxY, final double maxZ) {
        try {
            // TODO: Probably check vehicle ids too before doing this ?
            for (final Entity vehicle : entity.getNearbyEntities(0.1, 2.0, 0.1)) {
                final EntityType type = vehicle.getType();
                if (!MaterialUtil.isBoat(type) && type != EntityType.SHULKER) { //  && !(vehicle instanceof Minecart)) 
                    continue;
                }
                final double vehicleY = vehicle.getLocation(useLoc).getY() + vehicle.getHeight();
                final double entityY = entity.getLocation(useLoc).getY();
                useLoc.setWorld(null);
                // TODO: A "better" estimate is possible, though some more tolerance would be good. 
                return vehicleY < entityY + 0.1 && Math.abs(vehicleY - entityY) < 0.7;
            }
        } catch (Throwable t) {
            // Ignore exceptions (Context: DisguiseCraft).
        }
        return false;
    }
}