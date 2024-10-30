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

import fr.neatmonster.nocheatplus.compat.bukkit.BridgeMaterial;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;
import fr.neatmonster.nocheatplus.utilities.collision.ray.InteractRayTracing;
import fr.neatmonster.nocheatplus.utilities.map.FakeBlockCache;

public class TestInteractRayTracing {

    public final class CenteredInteractRayTracing extends InteractRayTracing {
        private int centerX, centerY, centerZ;
        public CenteredInteractRayTracing(boolean strict, int centerX, int centerY, int centerZ) {
            super(strict);
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
        }
        @Override
        public void set(double x0, double y0, double z0, double x1, double y1, double z1) {
            super.set(x0, y0, z0, x1, y1, z1, centerX, centerY, centerZ);
        }
    }

    // TODO: Blunt copy and paste from TestPassableRayTracing, add something that makes sense.

    public TestInteractRayTracing() {
        StaticLog.setUseLogManager(false);
        BlockTests.initBlockProperties();
        StaticLog.setUseLogManager(true);
    }

    @Test
    public void testAir() {
        FakeBlockCache bc = new FakeBlockCache();
        InteractRayTracing rt = new InteractRayTracing();
        rt.setBlockCache(bc);
        double[] coords = new double[]{0.5, 0.5, -0.5, 0.5, 0.5, 1.5};
        rt.set(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
        rt.loop();
        if (rt.collides()) {
            TestRayTracing.doFail("Expect not to collide with air.", coords);
        }
        if (rt.getStepsDone() > 4) {
            TestRayTracing.doFail("Expect less than 4 steps for moving straight through a block of air.", coords);
        }
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
        for (int y = 70 ; y < 73; y ++) {
            bc.set(10, y, 10, Material.STONE);
            bc.set(11, y, 11, Material.STONE);
        }
        // Ground.
        for (int x = 9; x < 13; x++) {
            for (int z = 9; z < 13; z++) {
                bc.set(x, 69, z, Material.STONE);
            }
        }
        // TODO: Make work with strict set to false.
        //InteractRayTracing rt = new InteractRayTracing(true);
        InteractRayTracing rt = new InteractRayTracing(false);
        rt.setBlockCache(bc);
        // TODO: More Directions, also just behind the corner.
        double[][] setups = new double[][] {
                // Slightly off the middle (11, y, 11)
                {11.4, 70.0, 10.4, 10.6, 70.0, 11.4},
                // Going exactly through the middle (11, y, 11)
                {11.4, 70.0, 10.6, 10.6, 70.0, 11.4},
                {11.5, 70.0, 10.5, 10.5, 70.0, 11.5},
        };
        TestRayTracing.runCoordinates(rt, setups, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }

    @Test
    public void testWall() {
        FakeBlockCache bc = new FakeBlockCache();
        // Wall using full blocks.
        bc.walls(0, 65, 0, 16, 67, 0, Material.STONE);
        // Ground using full blocks (roughly 16 margin to each side).
        bc.fill(-16, 64, -16, 32, 64, 16, Material.STONE);
        // TODO: Test chest like bounds for target blocks.
        InteractRayTracing rt = new InteractRayTracing(false);
        rt.setBlockCache(bc);
        // TODO: More cases, head inside block itself, angles, ...
        double[][] noCollision = new double[][] {
                {8.5, 66.75, 1.2  ,  8.5, 65.8, 1.0},
                {8.5, 66.75, 1.2  ,  8.5, 70.0, 0.9},
        };
        TestRayTracing.runCoordinates(rt, noCollision, false, true, 3.0, true);
        double[][] shouldCollide = new double[][] {
                {8.5, 66.75, 1.2  ,  8.5, 65.8, 0.0},
                {8.5, 66.75, 1.2  ,  8.5, 65.8, -0.2},
        };
        TestRayTracing.runCoordinates(rt, shouldCollide, true, false, 3.0, true);
        rt.cleanup();
        bc.cleanup();
    }

    @Test
    public void testRoom() {
        // TODO: Test for differing middle points (negative to positive range, random, selected rays).
        FakeBlockCache bc = new FakeBlockCache();
        bc.room(-1, 64, -1, 1, 66, 1, Material.STONE);
        // Note that reversed checks are slightly different with the centered version, but start + end blocks are air anyway.
        double[] middle = new double[] {0.5, 65.5, 0.5}; // Free spot.
        // TODO: Must work with strict set to false.
        CenteredInteractRayTracing rt = new CenteredInteractRayTracing(false, 0, 65, 0);
        //CenteredInteractRayTracing rt = new CenteredInteractRayTracing(true, 0, 65, 0);
        rt.setBlockCache(bc);
        double[][] pastFailures = new double[][] {
                {2.1393379885667643, 67.18197661625649, 1.7065201483677281  ,  0.0, 65.0, 0.0},
                {2.7915547712543676, 66.65545738305906, 1.310222428430474  ,  0.0, 65.0, 0.0},
                {0.0, 65.0, 4.5  ,  0.0, 65.0, 1.0}, // strict is false.
                {-3.5, 61.5, -3.5  ,  0.0, 65.0, 0.0} // strict is false.
        };
        TestRayTracing.runCoordinates(rt, pastFailures, true, false, 3, true);
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

    /**
     * Test cases taken from ingame logging.
     */
    @Test
    public void testIngame() {
        // Circle around the corners of 4 blocks with left button pressed down (random sample). 
        // Bad end coords (should fail): 
        {FakeBlockCache fbc = new FakeBlockCache(); 
        double[] _fb = new double[]{0.0, 0.0, 0.0, 1.0, 1.0, 1.0};
        fbc.set(142, 67, 221, Material.DIRT, 0, _fb);fbc.set(142, 67, 217, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(142, 69, 219, Material.AIR);fbc.set(142, 68, 218, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(142, 70, 220, Material.AIR);fbc.set(142, 71, 217, Material.AIR);fbc.set(142, 71, 221, Material.AIR);fbc.set(143, 67, 218, Material.DIRT, 0, _fb);fbc.set(143, 68, 217, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(143, 68, 221, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(143, 69, 220, Material.AIR);fbc.set(143, 70, 219, Material.AIR);fbc.set(143, 71, 218, Material.AIR);fbc.set(144, 67, 219, Material.DIRT, 0, _fb);fbc.set(144, 68, 220, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(144, 69, 217, Material.AIR);
        fbc.set(144, 69, 221, BridgeMaterial.TALL_GRASS, 1, new double[]{0.09999999403953552, 0.0, 0.09999999403953552, 0.8999999761581421, 0.800000011920929, 0.8999999761581421});fbc.set(144, 70, 218, Material.AIR);fbc.set(144, 71, 219, Material.AIR);fbc.set(145, 67, 220, Material.DIRT, 0, _fb);fbc.set(145, 68, 219, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(145, 69, 218, Material.AIR);fbc.set(145, 70, 217, Material.AIR);fbc.set(145, 70, 221, Material.AIR);fbc.set(145, 71, 220, Material.AIR);fbc.set(142, 68, 217, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(142, 68, 221, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(142, 67, 218, Material.DIRT, 0, _fb);fbc.set(142, 69, 220, Material.AIR);fbc.set(142, 70, 219, Material.AIR);fbc.set(142, 71, 218, Material.AIR);fbc.set(143, 67, 217, Material.DIRT, 0, _fb);fbc.set(143, 67, 221, Material.DIRT, 0, _fb);fbc.set(143, 68, 218, Material.OBSIDIAN, 0, _fb);fbc.set(143, 69, 219, Material.AIR);fbc.set(143, 70, 220, Material.AIR);fbc.set(143, 71, 217, Material.AIR);fbc.set(143, 71, 221, Material.AIR);fbc.set(144, 67, 220, Material.DIRT, 0, _fb);fbc.set(144, 68, 219, Material.OBSIDIAN, 0, _fb);fbc.set(144, 69, 218, Material.AIR);fbc.set(144, 70, 217, Material.AIR);fbc.set(144, 70, 221, Material.AIR);fbc.set(144, 71, 220, Material.AIR);fbc.set(145, 67, 219, Material.DIRT, 0, _fb);fbc.set(145, 68, 220, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(145, 69, 217, Material.AIR);fbc.set(145, 69, 221, Material.TORCH, 5, new double[]{0.4000000059604645, 0.0, 0.4000000059604645, 0.6000000238418579, 0.6000000238418579, 0.6000000238418579});fbc.set(145, 70, 218, Material.AIR);fbc.set(145, 71, 219, Material.AIR);fbc.set(142, 67, 219, Material.DIRT, 0, _fb);fbc.set(142, 70, 218, Material.AIR);fbc.set(142, 69, 221, BridgeMaterial.GRASS, 1, new double[]{0.09999999403953552, 0.0, 0.09999999403953552, 0.8999999761581421, 0.800000011920929, 0.8999999761581421});fbc.set(142, 69, 217, Material.AIR);fbc.set(142, 68, 220, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(142, 71, 219, Material.AIR);fbc.set(143, 67, 220, Material.DIRT, 0, _fb);fbc.set(143, 68, 219, Material.OBSIDIAN, 0, _fb);fbc.set(143, 69, 218, Material.AIR);fbc.set(143, 70, 217, Material.AIR);fbc.set(143, 70, 221, Material.AIR);fbc.set(143, 71, 220, Material.AIR);fbc.set(144, 67, 217, Material.DIRT, 0, _fb);fbc.set(144, 67, 221, Material.DIRT, 0, _fb);fbc.set(144, 68, 218, Material.OBSIDIAN, 0, _fb);fbc.set(144, 69, 219, Material.AIR);fbc.set(144, 70, 220, Material.AIR);fbc.set(144, 71, 217, Material.AIR);fbc.set(144, 71, 221, Material.AIR);fbc.set(145, 67, 218, Material.DIRT, 0, _fb);fbc.set(145, 68, 217, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(145, 68, 221, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(145, 69, 220, Material.AIR);fbc.set(145, 70, 219, Material.AIR);fbc.set(145, 71, 218, Material.AIR);fbc.set(142, 68, 219, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(142, 70, 217, Material.AIR);fbc.set(142, 67, 220, Material.DIRT, 0, _fb);fbc.set(142, 69, 218, BridgeMaterial.GRASS, 1, new double[]{0.09999999403953552, 0.0, 0.09999999403953552, 0.8999999761581421, 0.800000011920929, 0.8999999761581421});fbc.set(142, 70, 221, Material.AIR);fbc.set(142, 71, 220, Material.AIR);fbc.set(143, 67, 219, Material.DIRT, 0, _fb);fbc.set(143, 68, 220, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(143, 69, 217, Material.AIR);fbc.set(143, 69, 221, Material.AIR);fbc.set(143, 70, 218, Material.AIR);fbc.set(143, 71, 219, Material.AIR);fbc.set(144, 67, 218, Material.DIRT, 0, _fb);fbc.set(144, 68, 217, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(144, 68, 221, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(144, 69, 220, Material.AIR);fbc.set(144, 70, 219, Material.AIR);fbc.set(144, 71, 218, Material.AIR);fbc.set(145, 67, 217, Material.DIRT, 0, _fb);fbc.set(145, 67, 221, Material.DIRT, 0, _fb);fbc.set(145, 68, 218, BridgeMaterial.GRASS_BLOCK, 0, _fb);fbc.set(145, 69, 219, Material.AIR);fbc.set(145, 70, 220, Material.AIR);fbc.set(145, 71, 217, Material.AIR);fbc.set(145, 71, 221, Material.AIR);InteractRayTracing rt = new CenteredInteractRayTracing(false, 144, 68, 218);rt.setBlockCache(fbc);TestRayTracing.runCoordinates(rt, new double[]{144.01901074886095, 70.62, 220.1221052415879, 144.07776715103876, 68.99423513239826, 219.0}, true, false, 0.0, false, "ingame");rt.cleanup(); fbc.cleanup();}
    }

}
