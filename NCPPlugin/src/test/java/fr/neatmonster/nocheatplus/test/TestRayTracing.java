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

import static org.junit.Assert.fail;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.Test;

import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.build.BuildParameters;
import fr.neatmonster.nocheatplus.utilities.collision.RayTracing;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

public class TestRayTracing {

    // TODO: Add a test that fails if going beyond target block coordinate.

    protected static double maxFactor = 9.0;

    protected static int maxSteps(double dX, double dY, double dZ) {
        // TODO: Better calculation.
        return (int) (maxFactor * (1 + Math.abs(dX) + Math.abs(dY) + Math.abs(dZ)));
    }

    public static class CountRayTracing extends RayTracing {

        public CountRayTracing(double x0, double y0, double z0, double x1, double y1, double z1) {
            super(x0, y0, z0, x1, y1, z1);
        }

        @Override
        protected boolean step(int blockX, int blockY, int blockZ, double oX,
                double oY, double oZ, double dT, boolean isPrimary) {
            if (step > maxSteps(dX, dY, dZ)) {
                System.out.println("[WARNING] Max steps exceeded: " + maxSteps(dX, dY, dZ));
                return false; 
            }
            return true;
        }

    }

    public static double[] randomCoords(double max) {
        double[] res = new double[6];
        for (int i = 0; i < 6 ; i++) {
            res[i] = (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * max;
        }
        return res;
    }

    public static double[] randomBlockCoords(int max) {
        double[] res = new double[6];
        for (int i = 0; i < 6 ; i++) {
            res[i] = ThreadLocalRandom.current().nextInt(max * 2 + 1) - max;
        }
        return res;
    }

    public static void doFail(String message, double[] coords) {
        System.out.println("---- Failure trace ----");
        System.out.println(message);
        if (coords != null) {
            System.out.println("{" + coords[0] + ", " + coords[1]+ ", " + coords[2] + "  ,  " + coords[3] + ", " + coords[4]+ ", " + coords[5] + "}");
            dumpRawRayTracing(coords);
        }
        fail(message);
    }

    /**
     * Mostly block-coordinate consistency checking.
     * @param coords
     * @return
     */
    public static RayTracing checkConsistency(final double[] coords) {
        RayTracing rt = new RayTracing(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]) {

            protected int lbx, lby, lbz;

            protected double ldt = 0;

            protected double lox, loy, loz;

            /* (non-Javadoc)
             * @see fr.neatmonster.nocheatplus.utilities.RayTracing#set(double, double, double, double, double, double)
             */
            @Override
            public void set(double x0, double y0, double z0, double x1, double y1, double z1) {
                super.set(x0, y0, z0, x1, y1, z1);
                lbx = blockX - 1;
                lby = blockY - 1;
                lbz = blockZ - 1;
                lox = oX;
                loy = oY;
                loz = oZ;
                ldt = 0;
            }

            //            private boolean ignEdge(double offset, double dTotal) {
            //                return offset == 1 && dTotal > 0 || offset == 0 && dTotal < 0;
            //            }

            @Override
            protected boolean step(int blockX, int blockY, int blockZ, double oX, double oY, double oZ, double dT, boolean isPrimary) {
                // TODO: This does not check last step for some occasions where it should.

                if (dT < 0.0) {
                    doFail("dT < 0 at t = " + StringUtil.fdec3.format(t), coords);
                }

                // TODO: Check if this check makes sense at all (dT=0 happens during multi-transitions).
                //                if (dT == 0.0 && 1.0 - (t + dT) > tol) {
                //                    if (!ignEdge(oX, dX) && !ignEdge(oY, dY) && !ignEdge(oZ, dZ)) {
                //                        doFail("Premature dT = 0 at t = " + StringUtil.fdec3.format(t), coords);
                //                    }
                //                }

//                checkOffset(oX, "x");
//                checkOffset(oY, "y");
//                checkOffset(oZ, "z");

                // TODO: check with last block coordinates
                if (lbx == blockX && lby == blockY && lbz == blockZ) {
                    if (1.0 - (t + dT) > tol) {
                        doFail("Expect block coordinates to change with each step (step=" + step + ", t=" + StringUtil.fdec3.format(t) +").", coords);
                    }
                }
                // TODO: check offsets
                // Set to current.
                lbx = blockX;
                lby = blockY;
                lbz = blockZ;
                lox = oX;
                loy = oY;
                loz = oZ;
                ldt = dT;
                if (step > maxSteps(dX, dY, dZ)) {
                    doFail("max steps exceeded: " + maxSteps(dX, dY, dZ), coords); 
                }
                return true;
            }

//            private void checkOffset(double offset, String name) {
//                if (offset < 0.0 || offset > 1.0) {
//                    doFail("Bad " + name + "-offset: " + offset, coords);
//                }
//            }

            @Override
            public void loop() {
                super.loop();
                checkBlockTarget(coords[3], lbx, lox, dX, ldt, "x");
                checkBlockTarget(coords[4], lby, loy, dY, ldt, "y");
                checkBlockTarget(coords[5], lbz, loz, dZ, ldt, "z");
            }

            private void checkBlockTarget(double target, int current, double offset, double dTotal, double dT, String name) {
                int b = Location.locToBlock(target);
                if (current != b) {
                    // TODO: Might do with or without these ?
                    //					if (current == b + 1 && dTotal > 0 && offset == 0) return;
                    //					if (current == b - 1 && dTotal < 0 && offset == 1) return;
                    double diff = Math.abs(dT * dTotal + offset + (double) current - target);
                    if (diff <= 0.001) {
                        // TODO: Test how far off this usually is...
                        // TODO: Narrow down by edge coordinates or so.
                        return;
                    }
                    System.out.println(target + "|" +  current + "|" + offset + "|" + dT * dTotal);
                    // Failure.
                    doFail("Bad target " + name + "-coordinate: " + current + " instead of " + b + " (" + diff + " off)", coords);
                }
            }		
        };
        rt.loop();
        if (!rt.isEndBlock()) {
            // TODO: Fix last transition not taken sometimes (with "off by x-th digit" or "t=0 transition").
//            doFail("Incorrect end block.", coords);
        }
        return rt;
    }

    public static RayTracing checkNumberOfSteps(double[] coords, int steps) {
        CountRayTracing crt = new CountRayTracing(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
        crt.loop();
        int done = crt.getStepsDone();
        if (done != steps) {
            doFail("Wrong number of steps: " + done + " instead of " + steps, coords);
        }
        return crt;
    }

    public static void dump(int blockX, int blockY, int blockZ, double oX, double oY, double oZ, double t, double dT, boolean isPrimary) {
        String sdt = StringUtil.fdec3.format(dT);
        if ("0".equals(sdt) && dT > 0) sdt = "0.X";
        System.out.println(StringUtil.fdec3.format(t) + " (+" + sdt + "): " + blockX + ", "+blockY + ", " + blockZ + " / " + StringUtil.fdec3.format(oX) + ", " + StringUtil.fdec3.format(oY)+ ", " + StringUtil.fdec3.format(oZ) + (isPrimary ? " (primary)" : ""));
    }

    public static RayTracing dumpRawRayTracing(final double[] coords) {
        RayTracing rt = new RayTracing(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]) {
            @Override
            protected boolean step(int blockX, int blockY, int blockZ, double oX, double oY, double oZ, double dT, boolean isPrimary) {
                dump(blockX, blockY, blockZ, oX, oY, oZ, t, dT, isPrimary);
                if (step > maxSteps(dX, dY, dZ)) {
                    System.out.println("[WARNING] Max steps exceeded: " + maxSteps(dX, dY, dZ));
                    return false;
                }
                return true;
            }
        };
        rt.loop();
        if (!rt.isEndBlock())  {
            System.out.println("[WARNING] Incorrect end block.");
        }
        return rt;
    }

    @Test
    public void testNumberOfSteps() {
        // Hand picked stuff.
        checkNumberOfSteps(new double[]{0.5, 0.5, 0.5, 1.5, -0.5, 1.5}, 2);
    }

    @Test
    public void testConsistency() {
        // Past failures / making a difference.		
        for (double[] coords : new double[][] {
                // Sort by x0.
                {-9.873, -4.773, -3.387, -0.161, -1.879, -7.079},
                {-3.0066423238842366, 0.8056808285866079, 5.359238045631369  ,  2.0000000356757375, -2.3002237817433757, -5.889349195033338},
                {2.5619753859456917, -5.010424935746547, -7.39326637860553  ,  -4.678643570182639, -2.0000000105642313, -4.634727842675916},
                {7.388348424961977, -8.000000029346532, -2.5365675909347507  ,  2.17126848312847, 3.236994108042559, -8.423292642985071},
                {7.525633617461991, 2.654408573114717, 3.5119744782127893  ,  9.99999995904821, 9.599753890871172, 6.721727939686946},
                {1.1, 1.1, 1.1  ,  1.3, 1.3, 1.3},
                {1.1, 1.1, 1.1  ,  1.1, 1.1, 1.1},
                {-6.0, -4.0, -3.0  ,  -4.0, -3.0, -2.0},
                {-3.0, 3.0, -6.0  ,  2.0, -3.0, 4.0},
        }) {
            checkConsistency(coords);
            // Test reversed.
            if (coords[0] != coords[3] || coords[1] != coords[4] || coords[2] != coords[5]) {
                checkConsistency(new double[] {coords[3], coords [4], coords[5], coords[0], coords[1], coords[2]});
            }
        }

        final boolean e = BuildParameters.testLevel > 0;

        // Random tests.
        for (int i = 0; i < (e ? 50000000 : 100000); i++) {
            checkConsistency(randomCoords(10.0));
        }

        for (int i = 0; i < (e ? 10000000 : 1000); i++) {
            checkConsistency(randomBlockCoords(6));
        }

        // TODO: Add tests for typical coordinates a with interact, passable.
    }

    /**
     * 
     * @param rt
     * @param setup
     * @param expectCollide
     * @param expectNotCollide
     * @param stepsManhattan
     * @param reverse If set to true, end points will be exchanged for this run (not in addition).
     * @param tag
     */
    public static void runCoordinates(RayTracing rt, double[] setup, boolean expectCollide, boolean expectNotCollide, double stepsManhattan, boolean reverse, String tag) {
        if (reverse) {
            rt.set(setup[3], setup [4], setup[5], setup[0], setup[1], setup[2]);
            tag += "/reversed";
        } else {
            rt.set(setup[0], setup[1], setup[2], setup[3], setup [4], setup[5]);
        }
        rt.loop();
        if (rt.collides()) {
            if (expectNotCollide) {
                doFail("Expect not to collide, "+ tag + ".", setup);
            }
        } else {
            if (expectCollide) {
                doFail("Expect to collide, "+ tag + ".", setup);
            }
        }
        if (stepsManhattan > 0.0) {
            final double maxSteps = stepsManhattan * Math.max(1.0, TrigUtil.manhattan(setup[0], setup[1], setup[2], setup[3], setup[4], setup[5]));
            if ((double) rt.getStepsDone() > maxSteps) {
                doFail("Expect less than " + maxSteps + " steps, "+ tag + ".", setup);
            }
        }
    }

    /**
     * 
     * @param rt
     * @param setups Array of Arrays of 6 doubles as argument for RayTracing.set(...).
     * @param expectCollide
     * @param expectNotCollide
     * @param stepsManhattan
     * @param testReversed If to test the each ray with reversed end points in addition.
     */
    public static void runCoordinates(RayTracing rt, double[][] setups, boolean expectCollide, boolean expectNotCollide, double stepsManhattan, boolean testReversed) {
        for (int i = 0; i < setups.length; i++) {
            double[] setup = setups[i];
            runCoordinates(rt, setup, expectCollide, expectNotCollide, stepsManhattan, false, "index=" + i);
            if (testReversed) {
                // Reverse.
                runCoordinates(rt, setup, expectCollide, expectNotCollide, stepsManhattan, true, "index=" + i);
            }
        }
    }

    /**
     * Run (some) standard directions towards the center.
     * @param rt
     * @param cX
     * @param cY
     * @param cZ
     * @param length Rough length of the rays (might be applied per-axis including an additum).
     * @param nRandom Test a number of random rays as well.
     * @param expectCollide
     * @param expectNotCollide
     * @param testReversed If to test the each ray with reversed end points in addition.
     */
    public static void runCenterRays(RayTracing rt, double cX, double cY, double cZ, double length, int nRandom, boolean expectCollide, boolean expectNotCollide, boolean testReversed) {
        double[] mult = new double[] {-1.0, 0.0, 1.0};
        for (int ix = 0; ix < 3; ix ++) {
            for (int iy = 0; iy < 3; iy++) {
                for (int iz = 0; iz < 3; iz++) {
                    if (ix == 1 && iy == 1 &&  iz == 1) {
                        // Skip the center itself.
                        continue;
                    }
                    double[] coords = new double[] {
                            cX + length * mult[ix], 
                            cY + length * mult[iy],
                            cZ + length * mult[iz],
                            cX,
                            cY,
                            cZ
                            };
                    // TODO: Generate differing target points on/near middle as well.
                    TestRayTracing.runCoordinates(rt, coords, true, false, 3.0, false, "");
                    if (testReversed) {
                        TestRayTracing.runCoordinates(rt, coords, true, false, 3.0, true, "");
                    }
                }
            }
        }
        // TODO: Consider running block coordinates with larger radius (potentially all within some radius?).
        for (int n = 0; n < nRandom; n ++) {
            // TODO: Check if normalize is necessary.
            // One totally random vector.
            Vector vec = Vector.getRandom().normalize().multiply(length);
            double[] coords = new double[] {
                    cX + vec.getX(), 
                    cY + vec.getY(),
                    cZ + vec.getZ(),
                    cX,
                    cY,
                    cZ
                    };
            TestRayTracing.runCoordinates(rt, coords, true, false, 3.0, false, "random");
            if (testReversed) {
                TestRayTracing.runCoordinates(rt, coords, true, false, 3.0, true, "random");
            }
        }
    }

}
