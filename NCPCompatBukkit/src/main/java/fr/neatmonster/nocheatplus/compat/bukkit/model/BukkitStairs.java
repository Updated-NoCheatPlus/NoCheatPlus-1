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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;

import fr.neatmonster.nocheatplus.utilities.collision.Axis;
import fr.neatmonster.nocheatplus.utilities.collision.AxisAlignedBBUtils;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;

public class BukkitStairs implements BukkitShapeModel {

    // Taken from NMS - StairBlock.java
    private final double[] topslabs = new double[] {0.0, 0.5, 0.0, 1.0, 1.0, 1.0};
    private final double[] bottomslabs = new double[] {0.0, 0.0, 0.0, 1.0, 0.5, 1.0};

    private final double[] octet_nnn = new double[] {0.0, 0.0, 0.0, 0.5, 0.5, 0.5};
    private final double[] octet_nnp = new double[] {0.0, 0.0, 0.5, 0.5, 0.5, 1.0};
    private final double[] octet_pnn = new double[] {0.5, 0.0, 0.0, 1.0, 0.5, 0.5};
    private final double[] octet_pnp = new double[] {0.5, 0.0, 0.5, 1.0, 0.5, 1.0};

    private final double[] octet_npn = new double[] {0.0, 0.5, 0.0, 0.5, 1.0, 0.5};
    private final double[] octet_npp = new double[] {0.0, 0.5, 0.5, 0.5, 1.0, 1.0};
    private final double[] octet_ppn = new double[] {0.5, 0.5, 0.0, 1.0, 1.0, 0.5};
    private final double[] octet_ppp = new double[] {0.5, 0.5, 0.5, 1.0, 1.0, 1.0};

    private final double[][] top_stairs = makeshape(topslabs, octet_nnn, octet_pnn, octet_nnp, octet_pnp);
    private final double[][] bottom_stairs = makeshape(bottomslabs, octet_npn, octet_ppn, octet_npp, octet_ppp);
    private final int[] shape_by_state = new int[]{12, 5, 3, 10, 14, 13, 7, 11, 13, 7, 11, 14, 8, 4, 1, 2, 4, 1, 2, 8};

    @Override
    public double[] getShape(final BlockCache blockCache, final World world, final int x, final int y, final int z) {
        final Block block = world.getBlockAt(x, y, z);
        final BlockData blockData = block.getState().getBlockData();
        if (blockData instanceof Stairs) {
            final Stairs stairs = (Stairs) blockData;
            final Half half = stairs.getHalf();
            switch (half) {
                case BOTTOM:
                    return bottom_stairs[shape_by_state[getShapeStateIndex(stairs)]];
                case TOP:
                    return top_stairs[shape_by_state[getShapeStateIndex(stairs)]];
            }
        }
        return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
    }

    @Override
    public int getFakeData(final BlockCache blockCache, final World world, final int x, final int y, final int z) {
        //final Block block = world.getBlockAt(x, y, z);
        //final BlockState state = block.getState();
        //final BlockData blockData = state.getBlockData();

        //if (blockData instanceof Bisected) {
        //    final Bisected stairs = (Bisected) blockData;
        //    final Half half = stairs.getHalf();
        //    switch (half) {
        //        case TOP:
        //            return 0x4;
        //        default:
        //            break;
        //    }
        //}
        return 0;
    }

    private double[][] makeshape(double[] slab, double[] octet_nn, double[] octet_pn, double[] octet_np, double[] octet_pp) {
        return IntStream
                .range(0, 16)
                .mapToObj((flags) -> makeStairShape(flags, slab, octet_nn, octet_pn, octet_np, octet_pp))
                .toArray(double[][]::new);
    }

    private double[] makeStairShape(int flags, double[] slab, double[] octet_nn, double[] octet_pn, double[] octet_np, double[] octet_pp) {
        double[] res = slab;
        if ((flags & 1) != 0) {
            res = merge(res, octet_nn);
        }
        if ((flags & 2) != 0) {
            res = merge(res, octet_pn);
        }
        if ((flags & 4) != 0) {
            res = merge(res, octet_np);
        }
        if ((flags & 8) != 0) {
            res = merge(res, octet_pp);
        }
        return res;
    }

    private int getShapeStateIndex(Stairs stair) {
        return stair.getShape().ordinal() * 4 + directionToValue(stair.getFacing());
    }

    private int directionToValue(BlockFace face) {
        switch (face) {
            default:
            case UP:
            case DOWN:
                return -1;
            case NORTH:
                return 2;
            case SOUTH:
                return 0;
            case WEST:
                return 1;
            case EAST:
                return 3;
        }
    }

    // TODO: Poorly designed, Will recode better version later
    private double[] merge(double[] bounds, double[] octet) {
        double[] res = bounds;
        final double minX = octet[0];
        final double minY = octet[1];
        final double minZ = octet[2];
        final double maxX = octet[3];
        final double maxY = octet[4];
        final double maxZ = octet[5];
        for (int i = 2; i <= AxisAlignedBBUtils.getNumberOfAABBs(bounds); i++) {
                
            final double tminX = bounds[i*6-6];
            final double tminY = bounds[i*6-5];
            final double tminZ = bounds[i*6-4];
            final double tmaxX = bounds[i*6-3];
            final double tmaxY = bounds[i*6-2];
            final double tmaxZ = bounds[i*6-1];
            if (sameshape(minX, minY, minZ, maxX, maxY, maxZ, tminX, tminY, tminZ, tmaxX, tmaxY, tmaxZ)) {
                final List<Axis> a = getRelative(minX, minY, minZ, maxX, maxY, maxZ, tminX, tminY, tminZ, tmaxX, tmaxY, tmaxZ);
                if (a.size() == 1) {
                    Axis axis = a.get(0);
                    switch (axis) {
                        case X_AXIS:
                            res[i*6-6] = Math.min(tminX, minX);
                            res[i*6-3] = Math.max(tmaxX, maxX);
                            return res;
                        //case Y_AXIS:
                        //  break;
                        case Z_AXIS:
                            res[i*6-4] = Math.min(tminZ, minZ);
                            res[i*6-1] = Math.max(tmaxZ, maxZ);
                            return res;
                        default:
                            break;
                    }
                }  
            }
        }
        return add(res, octet);
    }

    private double[] add(final double[] array1, final double[] array2) {
        final double[] newArray = new double[array1.length + array2.length];
        System.arraycopy(array1, 0, newArray, 0, array1.length);
        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
        return newArray;
    }

    private List<Axis> getRelative(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                                   double tminX, double tminY, double tminZ, double tmaxX, double tmaxY, double tmaxZ) {
        final List<Axis> list = new ArrayList<Axis>();
        if (minX == tmaxX || maxX == tminX) {
            list.add(Axis.X_AXIS);
        }
        if (minY == tmaxY || maxY == tminY) {
            list.add(Axis.Y_AXIS);
        }
        if (minZ == tmaxZ || maxZ == tminZ) {
            list.add(Axis.Z_AXIS);
        }
        return list;
    }

    private boolean sameshape(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double tminX,
                              double tminY, double tminZ, double tmaxX, double tmaxY, double tmaxZ) {
        final double dx = maxX - minX;
        final double dy = maxY - minY;
        final double dz = maxZ - minZ;
        final double tdx = tmaxX - tminX;
        final double tdy = tmaxY - tminY;
        final double tdz = tmaxZ - tminZ;
        return dx == tdx && dy == tdy && dz == tdz;
    }
}
