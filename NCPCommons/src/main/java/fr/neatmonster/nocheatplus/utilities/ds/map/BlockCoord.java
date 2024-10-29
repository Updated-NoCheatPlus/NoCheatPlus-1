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
package fr.neatmonster.nocheatplus.utilities.ds.map;

import fr.neatmonster.nocheatplus.utilities.Misc;

public class BlockCoord {
    private final int x;
    private final int y;
    private final int z;
    
    /**
     * Constructs a BlockCoord with integer coordinates.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     */
    public BlockCoord(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * Constructs a BlockCoord from double coordinates by converting them
     * to block integer coordinates using Location.locToBlock().
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     */
    public BlockCoord(double x, double y, double z) {
        this.x = Misc.floor(x);
        this.y = Misc.floor(y);
        this.z = Misc.floor(z);
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getZ() {
        return z;
    }
    
    @Override
    public int hashCode() {
        return CoordHash.hashCode3DPrimes(x, y, z);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BlockCoord bc = (BlockCoord) obj;
        return bc.getX() == x && bc.getY() == y && bc.getZ() == z;
    }
}