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
package fr.neatmonster.nocheatplus.test;

import org.bukkit.Material;
import org.junit.Test;

import fr.neatmonster.nocheatplus.compat.BridgeMaterial;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;
import fr.neatmonster.nocheatplus.utilities.collision.ray.PassableRayTracing;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.FakeBlockCache;

public class TestPassableRayTracing {
    
    // TODO: Which tests to run on ICollidePassable with PassableAxisRayTracing as well...

    // TODO: Moving into a block, 
    // TODO: Moving out of a block
    // TODO: Moving horizontally on various kinds of ground (normal, half blocks)
    // TODO: Moving up stairs etc ?
    // TODO: From ground and onto ground moves, onto-edge moves (block before edge, into block, etc).
    // TODO: Randomized tests (Collide with inner sphere, not collide with outer sphere).

    public TestPassableRayTracing() {
        StaticLog.setUseLogManager(false);
        BlockTests.initBlockProperties();
        StaticLog.setUseLogManager(true);
    }

    @Test
    public void testAir() {
        FakeBlockCache bc = new FakeBlockCache();
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        double[] coords = new double[]{0.5, 0.5, -0.5, 0.5, 0.5, 1.5};
        rt.set(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
        rt.loop();
        if (rt.collides()) {
            TestRayTracing.doFail("Expect not to collide when moving through a block.", coords);
        }
        if (rt.getStepsDone() > 4) {
            TestRayTracing.doFail("Expect less than 4 steps for moving straight through a block of air.", coords);
        }
        rt.cleanup();
        bc.cleanup();
    }

    @Test
    public void testThroughOneBlock() {
        FakeBlockCache bc = new FakeBlockCache();
        bc.set(0, 0, 0, Material.STONE);
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        double[][] setups = new double[][] {
                // Through the middle of the block.
                {0.5, 0.5, -0.5, 0.5, 0.5, 1.5},
                {-0.5, 0.5, 0.5, 1.5, 0.5, 0.5},
                {0.5, -0.5, 0.5, 0.5, 1.5, 0.5},
                // Along the edges.
                {0.5, 0.0, -0.5, 0.5, 0.0, 1.5},
                {-0.5, 0.0, 0.5, 1.5, 0.0, 0.5},
                // Exactly diagonal.
                {-0.5, -0.5, -0.5, 1.5, 1.5, 1.5}, // 3d
                {-0.5, 0.0, -0.5, 1.5, 0.0, 1.5}, // 2d
                // Through a corner.
                {1.2, 0.5, 0.5, 0.5, 0.5, 1.2},

                // TODO: More of each and other... + generic set-ups?
        };
        TestRayTracing.runCoordinates(rt, setups, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }

    /**
     * Moving diagonally through an "empty corner", seen from above:<br>
     * ox<br>
     * xo
     */
    @Test
    public void testEmptyCorner() {
        FakeBlockCache bc = new FakeBlockCache();
        // The "empty corner" setup.
        bc.set(10, 70, 10, Material.STONE);
        bc.set(11, 70, 11, Material.STONE);
        // Ground.
        for (int x = 9; x < 13; x++) {
            for (int z = 9; z < 13; z++) {
                bc.set(x, 69, z, Material.STONE);
            }
        }
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        // TODO: More Directions, over a corner, sides, etc.
        double[][] setups = new double[][] {
                // Slightly off the middle (11, y, 11)
                {11.4, 70.0, 10.4, 10.6, 70.0, 11.4},
                // Going exactly through the middle (11, y, 11)
                {11.4, 70.0, 10.6, 10.6, 70.0, 11.4},
                {11.5, 70.0, 10.5, 10.5, 70.0, 11.5},
                //{11.5, 70.0, 10.5, 10.99999999999, 70.0, 11.00000000001}, // TODO: Craft something here
        };
        TestRayTracing.runCoordinates(rt, setups, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }

    @Test
    public void testGround() {
        FakeBlockCache bc = new FakeBlockCache();
        // Ground using full blocks.
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                bc.set(x, 65, z, Material.STONE);
            }
        }
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        // TODO: More Directions, also from air underneath to ground).
        double[][] noCollision = new double[][] {
                {1.3, 66.0, 2.43, 5.25, 66.0, 7.12},
        };
        TestRayTracing.runCoordinates(rt, noCollision, false, true, 3.0, true);
        double[][] shouldCollide = new double[][] {
                {1.3, 65.1, 2.43, 2.3, 65.1, 4.43},
                {1.3, 65.0, 2.43, 2.3, 65.0, 4.43},
                {1.3, 66.0, 2.43, 1.3, 65.9, 2.43},
                {1.3, 66.0, 2.43, 5.25, 65.9, 7.12},
                {1.3, 65.4, 2.43, 1.3, 65.4, 2.43}, // No distance.
        };
        TestRayTracing.runCoordinates(rt, shouldCollide, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }

    @Test
    public void testGroundSteps() {
        FakeBlockCache bc = new FakeBlockCache();
        // Ground using 0.5 high step blocks.
        final double[] stepBounds = new double[]{0.0, 0.0, 0.0, 1.0, 0.5, 1.0};
        BlockFlags.setBlockFlags(BridgeMaterial.STONE_SLAB, BlockFlags.F_SOLID | BlockFlags.F_GROUND);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                bc.set(x, 65, z, BridgeMaterial.STONE_SLAB, stepBounds);
            }
        }
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        // TODO: More Directions, also from air underneath to ground).
        double[][] noCollision = new double[][] {
                {1.3, 65.5, 2.43, 5.25, 65.5, 7.12},
        };
        TestRayTracing.runCoordinates(rt, noCollision, false, true, 3.0, true);
        double[][] shouldCollide = new double[][] {
                {1.3, 65.1, 2.43, 2.3, 65.1, 7.43},
                {1.3, 65.0, 2.43, 2.3, 65.0, 7.43},
                {1.3, 65.5, 2.43, 1.3, 65.4, 2.43},
                {1.3, 65.5, 2.43, 5.25, 65.4, 7.12},
                {1.3, 65.4, 2.43, 1.3, 65.4, 2.43}, // No distance.
        };
        TestRayTracing.runCoordinates(rt, shouldCollide, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }

    @Test
    public void testRoom() {
        FakeBlockCache bc = new FakeBlockCache();
        bc.room(-1, 64, -1, 1, 66, 1, Material.STONE);
        double[] middle = new double[] {0.5, 65.5, 0.5}; // Free spot.
        PassableRayTracing rt = new PassableRayTracing();
        rt.setBlockCache(bc);
        boolean intense = BuildParameters.testLevel > 1;
        for (double x = -0.5; x < 1.0; x += 0.5) {
            for (double y = -0.5; y < 1.0; y += 0.5) {
                for (double z = -0.5; z < 1.0; z += 0.5) {
                    double add = Math.abs(x) + Math.abs(y) + Math.abs(z);
                    TestRayTracing.runCenterRays(rt, middle[0] + x, middle[1] + y, middle[2] + z, 2.0 + add, intense ? 10000 : 1000, true, false, true);
                }
            }
        }
        rt.cleanup();
        bc.cleanup();
    }

}
