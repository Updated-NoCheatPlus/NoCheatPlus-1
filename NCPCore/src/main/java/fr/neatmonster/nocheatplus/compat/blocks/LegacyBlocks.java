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
package fr.neatmonster.nocheatplus.compat.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.bukkit.Material;
import org.bukkit.block.BlockFace;

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.utilities.collision.Axis;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;

public class LegacyBlocks {
    private static final BlockStairs STAIRS = new BlockStairs();
    private static final BlockTrapDoor TRAPDOOR = new BlockTrapDoor();
    private static final Map<Material, Block> blocks = init(); // new HashMap<>(); //private final Map<Material, Block> block;

    //public LegacyBlocks() {
    //    blocks = init();
    //}

    private static Map<Material, Block> init() {
        Map<Material, Block> blocks = new HashMap<>();
        for (Material mat : MaterialUtil.ALL_STAIRS) {
            blocks.put(mat, STAIRS);
        }
        for (Material mat : MaterialUtil.ALL_TRAP_DOORS) {
            blocks.put(mat, TRAPDOOR);
        }
        blocks.put(BridgeMaterial.END_PORTAL_FRAME, new BlockEndPortalFrame());
        blocks.put(BridgeMaterial.PISTON_HEAD, new BlockPistonHead());
        blocks.put(Material.BREWING_STAND, new BlockStatic(
                // Bottom rod
                0.0, 0.0, 0.0, 1.0, 0.125, 1.0,
                // Rod
                0.4375, 0.125, 0.4375, 0.5625, 0.875, 0.5625
                )
            );
        blocks.put(Material.SOUL_SAND, new BlockStatic(0.0, 0.0, 0.0, 1.0, 0.875, 1.0));
        blocks.put(Material.CACTUS, new BlockStatic(0.0625, 0.0, 0.0625, 0.9375, 0.9375, 0.9375));
        return blocks;
    }

    /**
     * Get block bounding boxes for legacy version
     * 
     * @param cache the BlockCache
     * @param mat Material of the block
     * @param x Block location
     * @param y Block location
     * @param z Block location
     * @param old if server is below 1.9
     * @return bounds, can be null if that block doesn't need.
     */
    //public double[] getShape(BlockCache cache, Material mat, int x, int y, int z, boolean old) {
    public static double[] getShape(BlockCache cache, Material mat, int x, int y, int z, boolean old) {
        final Block blockshape = blocks.get(mat);
        if (blockshape != null) {
            return blockshape.getShape(cache, mat, x, y, z, old);
        }
        return null;
    }

    public static interface Block {
        public double[] getShape(BlockCache cache, Material mat, int x, int y, int z, boolean old);
    }

    public static class BlockStatic implements Block{
        private final double[] bounds;

        public BlockStatic(double ...bounds) {
            if (bounds.length == 0) {
                this.bounds = null;
                return;
            }

            if (bounds.length % 6 != 0) {
                throw new IllegalArgumentException("The length must be a multiple of 6");
            }
            this.bounds = bounds;
        }

        @Override
        public double[] getShape(BlockCache cache, Material mat, int x, int y, int z, boolean old) {
            return bounds;
        }    
    }

    public static class BlockTrapDoor implements Block {

        private static final double closedHeight = 0.1875;
        private static final double openWidth = 0.1875;
        public BlockTrapDoor() {
            
        }

        @Override
        public double[] getShape(BlockCache cache, Material mat, int x, int y, int z, boolean old) {
            return getShapeLegacy(cache.getData(x, y, z));
        }

        public double[] getShapeLegacy(int data) {
            BlockFace face = dataToDirection(data);
            if (face == null) return null;
            return getShape(face, (data & 4) != 0, (data & 8) == 0);
        }

        public BlockFace dataToDirection(int data) {
            switch (data & 3) {
            case 0:
                return BlockFace.NORTH;
            case 1:
                return BlockFace.SOUTH;
            case 2:
                return BlockFace.WEST;
            case 3:
                return BlockFace.EAST;
            }
            return null;
        }

        private double[] getShape(BlockFace face, boolean open, boolean bottom) {
            if (open) {
                switch(face) {
                    case NORTH:
                        return new double[] {0.0, 0.0, 1.0 - openWidth, 1.0, 1.0, 1.0};
                    case SOUTH:
                        return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, openWidth};
                    case EAST:
                        return new double[] {0.0, 0.0, 0.0, openWidth, 1.0, 1.0};
                    case WEST:
                        return new double[] {1.0 - openWidth, 0.0, 0.0, 1.0, 1.0, 1.0};
                    default:
                        break;
                    }
                }
                else {
                    return bottom
                            ? new double[] {0.0, 0.0, 0.0, 1.0, closedHeight, 1.0}
                    : new double[] {0.0, 1.0 - closedHeight, 0.0, 1.0, 1.0, 1.0};

                }
            return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
        }
    }

    public static class BlockEndPortalFrame implements Block {

        public BlockEndPortalFrame() {
            
        }

        public double[] getShapeLegacy(boolean hasEye) {
            return hasEye ?
              new double[] {0.0, 0.0, 0.0, 1.0, 0.8125, 1.0,
                            0.3125, 0.8125, 0.3125, 0.6875, 1.0, 0.6875}
            : new double[] {0.0, 0.0, 0.0, 1.0, 0.8125, 1.0};
        }

        public double[] getShapeLegacy(int data) {
            return getShapeLegacy((data & 0x4) != 0);
        }

        @Override
        public double[] getShape(BlockCache cache, Material mat, int x, int y, int z, boolean old) {
            return getShapeLegacy(cache.getData(x, y, z));
        }
    }

    public static class BlockPistonHead implements Block {

        public BlockPistonHead() {
            
        }

        @Override
        public double[] getShape(BlockCache cache, Material mat, int x, int y, int z, boolean old) {
            return getShapeLegacy(cache.getData(x, y, z), old);
        }

        public double[] getShapeLegacy(int data, boolean bugged) {
            BlockFace face = dataToDirection(data);
            if (face == null) return null;
            return getShape(face, bugged);
        }

        private double[] getShape(BlockFace face, boolean bugged) {
            final double bug = bugged ? 0.125 : 0.0;
            switch (face) {
            case UP: return new double[] {
                    // Shaft
                    0.375, 0.0, 0.375, 0.625, 1.0, 0.625,
                    // Plank
                    0.0, 0.75, 0.0, 1.0, 1.0, 1.0
                    };
            case DOWN: return new double[] {
                    // Shaft
                    0.375, 0.0, 0.375, 0.625, 1.0, 0.625,
                    // Plank
                    0.0, 0.0, 0.0, 1.0, 0.25, 1.0
                    };
            case NORTH: return new double[] {
                    // Shaft
                    0.375 - bug, 0.375, 0.0, 0.625 + bug, 0.625, 1.0,
                    // Plank
                    0.0, 0.0, 0.0, 1.0, 1.0, 0.25
                    };
            case SOUTH: return new double[] {
                    // Shaft
                    0.375 - bug, 0.375, 0.0, 0.625 + bug, 0.625, 1.0,
                    // Plank
                    0.0, 0.0, 0.75, 1.0, 1.0, 1.0
                    };
            case WEST: 
                return bugged ? new double[] {
                       // Shaft ???
                       0.0, 0.375, 0.25, 0.625, 0.75, 1.0,
                       // Plank
                       0.0, 0.0, 0.0, 0.25, 1.0, 1.0
                       } : 
                    new double[] {
                    // Shaft
                    0.0, 0.375, 0.375, 1.0, 0.625, 0.625,
                    // Plank
                    0.0, 0.0, 0.0, 0.25, 1.0, 1.0
                    };
            case EAST: return new double[] {
                    // Shaft
                    0.0, 0.375, 0.375 - bug, 1.0, 0.625, 0.625 + bug,
                    // Plank
                    0.75, 0.0, 0.0, 1.0, 1.0, 1.0
                    };
            default:
                return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
            }
        }

        private BlockFace dataToDirection(int data) {
            switch (data & 7) {
            case 0:
                return BlockFace.DOWN;
            case 1:
                return BlockFace.UP;
            case 2:
                return BlockFace.NORTH;
            case 3:
                return BlockFace.SOUTH;
            case 4:
                return BlockFace.WEST;
            case 5:
                return BlockFace.EAST;
            }
            return null;
        }
    }

    // Mostly taken from NMS - StairBlock.java
    public static class BlockStairs implements Block {
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

        public BlockStairs() {
            
        }

        @Override
        public double[] getShape(BlockCache cache, Material mat, int x, int y, int z, boolean old) {
            return getShapeLegacy(cache, cache.getData(x, y, z), x, y, z);
        }

        public double[] getShapeLegacy(BlockCache cache, int data, int x, int y, int z) {
            final boolean isTop = (data & 4) !=0;
            final BlockFace face = dataToDirection(data);
            if (face == null) return null;
            final int shapeindex = getStairShapeIndexLegacy(cache, face, isTop, data, x, y, z);
            if (isTop) return top_stairs[shape_by_state[getShapeStateIndex(shapeindex, face)]];
            return bottom_stairs[shape_by_state[getShapeStateIndex(shapeindex, face)]]; 
        }

        private int getStairShapeIndexLegacy(BlockCache cache, BlockFace face, boolean isTop, int data, int x, int y, int z) {
            final BlockFace oppositeface = face.getOppositeFace();

            final Material testType1 = cache.getType(x + face.getModX(), y, z + face.getModZ());
            final int testData1 = cache.getData(x + face.getModX(), y, z + face.getModZ());

            final Material testType2 = cache.getType(x + oppositeface.getModX(), y, z + oppositeface.getModZ());
            final int testData2 = cache.getData(x + oppositeface.getModX(), y, z + oppositeface.getModZ());

            if (BlockProperties.isStairs(testType1) && isTop == ((testData1 & 4) !=0)) {
                final BlockFace testFace = dataToDirection(testData1);
                if (testFace != null && hasDifferentAscAxis(face, testFace) && 
                    canTakeShape(cache, isTop, face, x + testFace.getOppositeFace().getModX(), y, z + testFace.getOppositeFace().getModZ())) {
                    if (testFace == getCounterClockWise(face)) {
                        return 3; // OUTER_LEFT
                    }
                    return 4; // OUTER_RIGHT
                }
            }

            if (BlockProperties.isStairs(testType2) && isTop == ((testData2 & 4) !=0)) {
                final BlockFace testFace = dataToDirection(testData2);
                if (testFace != null && hasDifferentAscAxis(face, testFace) && 
                        canTakeShape(cache, isTop, face, x + testFace.getModX(), y, z + testFace.getModZ())) {
                        if (testFace == getCounterClockWise(face)) {
                            return 1; // INNER_LEFT
                        }
                        return 2; // INNER_RIGHT
                    }
            }
            return 0; // STRAIGHT
        }

        private boolean hasDifferentAscAxis(BlockFace testFace1, BlockFace testFace2) {
            return testFace1.getOppositeFace() != testFace2;
        }

        private boolean canTakeShape(BlockCache cache, boolean orginStairTop, BlockFace orginStairFace, int x, int y, int z) {
            final Material testType = cache.getType(x, y, z);
            final int testData = cache.getData(x, y, z);
            final boolean testTop = (testData & 4) !=0;
            final BlockFace testFace = dataToDirection(testData);
            return !BlockProperties.isStairs(testType) || orginStairFace != testFace || orginStairTop != testTop;
        }

        private BlockFace getCounterClockWise(BlockFace face) {
            switch (face) {
                case NORTH:
                    return BlockFace.WEST;
                case EAST:
                    return BlockFace.NORTH;
                case SOUTH:
                    return BlockFace.EAST;
                case WEST:
                    return BlockFace.SOUTH;
                default: return null;
            }
        }

        private double[][] makeshape(double[] slab, 
                double[] octet_nn, double[] octet_pn, double[] octet_np, double[] octet_pp) {
            return IntStream
                    .range(0, 16)
                    .mapToObj((flags) -> makeStairShape(flags, slab, octet_nn, octet_pn, octet_np, octet_pp))
                    .toArray(double[][]::new);
        }

        private double[] makeStairShape(int flags, double[] slab,
                double[] octet_nn, double[] octet_pn, double[] octet_np, double[] octet_pp) {
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

        private int getShapeStateIndex(int shapeIndex, BlockFace face) {
            return shapeIndex * 4 + directionToValue(face);
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

        private BlockFace dataToDirection(int data) {
            switch (data & 3) {
            case 0:
                return BlockFace.EAST;
            case 1:
                return BlockFace.WEST;
            case 2:
                return BlockFace.SOUTH;
            case 3:
                return BlockFace.NORTH;
            }
            return null;
        }

        private double[] add(final double[] array1, final double[] array2) {
            final double[] newArray = new double[array1.length + array2.length];
            System.arraycopy(array1, 0, newArray, 0, array1.length);
            System.arraycopy(array2, 0, newArray, array1.length, array2.length);
            return newArray;
        }

        private double[] merge(double[] bounds, double[] octet) {
            double[] res = bounds;
            final double minX = octet[0];
            final double minY = octet[1];
            final double minZ = octet[2];
            final double maxX = octet[3];
            final double maxY = octet[4];
            final double maxZ = octet[5];
            for (int i = 2; i <= (int)bounds.length / 6; i++) {
                    
                final double tminX = bounds[i*6-6];
                final double tminY = bounds[i*6-5];
                final double tminZ = bounds[i*6-4];
                final double tmaxX = bounds[i*6-3];
                final double tmaxY = bounds[i*6-2];
                final double tmaxZ = bounds[i*6-1];
                if (sameshape(minX, minY, minZ, maxX, maxY, maxZ, 
                         tminX, tminY, tminZ, tmaxX, tmaxY, tmaxZ)) {
                    final List<Axis> a = getRelative(minX, minY, minZ, maxX, maxY, maxZ, 
                            tminX, tminY, tminZ, tmaxX, tmaxY, tmaxZ);
                    if (a.size() == 1) {
                        Axis axis = a.get(0);
                        switch (axis) {
                        case X_AXIS:
                            res[i*6-6] = Math.min(tminX, minX);
                            res[i*6-3] = Math.max(tmaxX, maxX);
                            return res;
                        //case Y_AXIS:
                        //        break;
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
}
