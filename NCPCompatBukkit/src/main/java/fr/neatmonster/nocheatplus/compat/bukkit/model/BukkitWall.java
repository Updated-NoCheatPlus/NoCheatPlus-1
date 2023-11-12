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

import java.util.Set;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.type.Wall;

import fr.neatmonster.nocheatplus.utilities.map.BlockCache;

public class BukkitWall implements BukkitShapeModel {

    private final double minXZ;
    private final double maxXZ;
    private final double height;
    private final double sideInset;
    private final double[] east;
    private final double[] north;
    private final double[] west;
    private final double[] south;
    private final double[] eastwest;
    private final double[] southnorth;

    public BukkitWall(double inset, double height) {
        this(inset, 1.0 - inset, height, 0.0);
    }

    public BukkitWall(double inset, double height, double sideInset) {
        this(inset, 1.0 - inset, height, sideInset);
    }

    public BukkitWall(double minXZ, double maxXZ, double height, double sideInset) {
        this.minXZ = minXZ;
        this.maxXZ = maxXZ;
        this.height = height;
        this.sideInset = sideInset;
        east = new double[] {maxXZ, 0.0, sideInset, 1.0, height, 1.0 - sideInset};
        north = new double[] {sideInset, 0.0, 0.0, 1.0 - sideInset, height, minXZ};
        west = new double[] {0.0, 0.0, sideInset, minXZ, height, 1.0 - sideInset};
        south = new double[] {sideInset, 0.0, maxXZ, 1.0 - sideInset, height, 1.0};
        eastwest = new double[] {0.0, 0.0, sideInset, 1.0, height, 1.0 - sideInset};
        southnorth = new double[] {sideInset, 0.0, 0.0, 1.0 - sideInset, height, 1.0};
    }

    @Override
    public double[] getShape(final BlockCache blockCache, final World world, final int x, final int y, final int z) {
        // Relevant: https://bugs.mojang.com/browse/MC-9565
        final Block block = world.getBlockAt(x, y, z);
        final BlockState state = block.getState();
        final BlockData blockData = state.getBlockData();

        if (blockData instanceof MultipleFacing) {
            MultipleFacing fence = (MultipleFacing) blockData;
            double[] res = new double[] {minXZ, 0.0, minXZ, maxXZ, height, maxXZ};
            Set<BlockFace> faces = fence.getFaces();
            if (!faces.contains(BlockFace.UP) && faces.size() == 2) {
                if (faces.contains(BlockFace.SOUTH)) {
                    return new double[] {sideInset, 0.0, 0.0, 1.0 - sideInset, height, 1.0};
                }
                if (faces.contains(BlockFace.WEST)) {
                    return new double[] {0.0, 0.0, sideInset, 1.0, height, 1.0 - sideInset};
                }
            }
            for (final BlockFace face : fence.getFaces()) {
                switch (face) {
                    case EAST:
                        res = add(res, east);
                        break;
                    case NORTH:
                        res = add(res, north);
                        break;
                    case WEST:
                        res = add(res, west);
                        break;
                    case SOUTH:
                        res = add(res, south);
                        break;
                    default:
                        break;
                }
            }
            return res;
        } 
        else if (blockData instanceof Wall) {
            final Wall wall = (Wall) blockData;
            if (wall.isUp()) {
                double[] res = new double[] {minXZ, 0.0, minXZ, maxXZ, height, maxXZ};
                if (!wall.getHeight(BlockFace.WEST).equals(Wall.Height.NONE)) {
                    res = add(res, west);
                }
                if (!wall.getHeight(BlockFace.EAST).equals(Wall.Height.NONE)) {
                    res = add(res, east);
                }
                if (!wall.getHeight(BlockFace.NORTH).equals(Wall.Height.NONE)) {
                    res = add(res, north);
                }
                if (!wall.getHeight(BlockFace.SOUTH).equals(Wall.Height.NONE)) {
                    res = add(res, south);
                }
                return res;
            } 
            else {
                double[] res = null;
                if (!wall.getHeight(BlockFace.WEST).equals(Wall.Height.NONE) 
                    && !wall.getHeight(BlockFace.EAST).equals(Wall.Height.NONE)) {
                    if (res == null) {
                        res = eastwest;
                    }
                }
                if (!wall.getHeight(BlockFace.NORTH).equals(Wall.Height.NONE) 
                    && !wall.getHeight(BlockFace.SOUTH).equals(Wall.Height.NONE)) {
                    if (res == null) {
                        res = southnorth; 
                    }
                    else res = add(res, southnorth);
                }
                if (res != null) {
                    return res;
                }
            }
        }
        return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
    }

    @Override
    public int getFakeData(final BlockCache blockCache, final World world, final int x, final int y, final int z) {
        return 0;
    }

    private double[] add(final double[] array1, final double[] array2) {
        final double[] newArray = new double[array1.length + array2.length];
        System.arraycopy(array1, 0, newArray, 0, array1.length);
        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
        return newArray;
    }
}