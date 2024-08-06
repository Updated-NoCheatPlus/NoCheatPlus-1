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
package fr.neatmonster.nocheatplus.utilities.collision.ray;

import org.bukkit.Location;

import fr.neatmonster.nocheatplus.utilities.collision.Axis;

/**
 * Ray tracing for block coordinates with entry point offsets.
 * @author mc_dev
 *
 */
public abstract class RayTracing implements ICollideBlocks {

    //	/** End point coordinates (from, to) */
    protected double x0, y0, z0; // x1, y1, z1;

    //	/** Total distance between end points. */
    //	protected double d;

    /** Distance per axis. */
    protected double dX, dY, dZ;

    /** Current block, step always has been or is called with these. */
    protected int blockX, blockY, blockZ;

    /** End block. */
    protected int endBlockX, endBlockY, endBlockZ;

    /** Offset within current block. */
    protected double oX, oY, oZ;

    /** Current "time" in [0..1]. */
    protected double t = Double.MIN_VALUE;

    /** Tolerance for time, for checking the abort condition: 1.0 - t <= tol . */
    protected double tol = 0.0;

    /** Force calling step at the end position, for the case it is reached with block transitions. */
    protected boolean forceStepEndPos = true;

    /**
     * Counting the number of steps along the primary line. Step is incremented
     * before calling step(), and is 0 after set(...). Checking this from within
     * step means to get the current step number, checking after loop gets the
     * number of steps done.
     */
    protected int step = 0;

    /** If to call stepSecondary at all (secondary transitions).*/
    protected boolean secondaryStep = true;

    /** Maximum steps that will be done. */
    private int maxSteps = Integer.MAX_VALUE;

    /** Just the flag, a sub-class must make use during handling step. */
    protected boolean ignoreInitiallyColliding = false;

    protected boolean collides = false;

    public RayTracing(double x0, double y0, double z0, double x1, double y1, double z1) {
        set(x0, y0, z0, x1, y1, z1);
    }

    public RayTracing() {
        set(0, 0, 0, 0, 0, 0);
    }

    @Override
    public void setIgnoreInitiallyColliding(boolean ignoreInitiallyColliding) {
        this.ignoreInitiallyColliding = ignoreInitiallyColliding;
    }

    @Override
    public boolean getIgnoreInitiallyColliding() {
        return ignoreInitiallyColliding;
    }

    @Override
    public void set(double x0, double y0, double z0, double x1, double y1, double z1) {
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        //		this.x1 = x1;
        //		this.y1 = y1;
        //		this.z1 = z1;
        //		d = CheckUtils.distance(x0, y0, z0, x1, y1, z1);
        dX = x1 - x0;
        dY = y1 - y0;
        dZ = z1 - z0;
        blockX = Location.locToBlock(x0);
        blockY = Location.locToBlock(y0);
        blockZ = Location.locToBlock(z0);
        endBlockX = Location.locToBlock(x1);
        endBlockY = Location.locToBlock(y1);
        endBlockZ = Location.locToBlock(z1);
        oX = x0 - (double) blockX;
        oY = y0 - (double) blockY;
        oZ = z0 - (double) blockZ;
        t = 0.0;
        step = 0;
        collides = false;
    }

    /**
     * 
     * @param dTotal
     * @param offset
     * @param isEndBlock If the end block coordinate is reached for this axis.
     * @return
     */
    private static final double tDiff(final double dTotal, final double offset, final boolean isEndBlock) {
        // TODO: endBlock check only for == not </> ?
        if (dTotal > 0.0) {
            if (offset >= 1.0) {
                // Static block change (e.g. diagonal move).
                return isEndBlock ? Double.MAX_VALUE : 0.0;
            } else {
                return (1.0 - offset) / dTotal; 
            }
        }
        else if (dTotal < 0.0) {
            if (offset <= 0.0) {
                // Static block change (e.g. diagonal move).
                return isEndBlock ? Double.MAX_VALUE : 0.0;
            } else {
                return offset / -dTotal;
            }
        }
        else {
            return Double.MAX_VALUE;
        }
    }

    @Override
    public void loop() {

        // Time to block edge.
        double tX, tY, tZ, tMin;
        // Number of axes to make a transition for.
        int transitions;
        // Transition direction per axis.
        boolean transX, transY, transZ;

        // Actual loop.
        /*
         * TODO: Fix last transition not taken sometimes (with
         * "off by x-th digit" or "t=0 transition"). Consider correcting t on
         * base of the block coordinates in use.
         */
        while (t + tol < 1.0) {
            // Determine smallest time to block edge, per axis.
            tX = tDiff(dX, oX, blockX == endBlockX);
            tY = tDiff(dY, oY, blockY == endBlockY);
            tZ = tDiff(dZ, oZ, blockZ == endBlockZ);
            // Adjust time.
            tMin = Math.max(0.0, Math.min(tX,  Math.min(tY, tZ)));
            if (tMin == Double.MAX_VALUE) {
                // All differences are 0 (no progress).
                if (step < 1) {
                    // Allow one step always.
                    tMin = 0.0;
                }
                else {
                    break;
                }
            }
            if (t + tMin > 1.0) {
                // Set to the remaining distance (does trigger).
                // TODO: Inaccurate t can mean iterating too short.
                tMin = 1.0 - t;
            }


            // Step for the primary line.
            step ++;
            if (!step(blockX, blockY, blockZ, oX, oY, oZ, tMin, true)) {
                break;
            }

            // Abort if arrived.
            if (t + tMin + tol >= 1.0 && isEndBlock()) {
                break;
            }

            // Determine transitions, per axis.
            transitions = 0;
            transX = transY = transZ = false;
            if (tX == tMin && blockX != endBlockX && dX != 0.0) {
                transX = true;
                transitions ++;
            }
            if (tY == tMin && blockY != endBlockY && dY != 0.0) {
                transY = true;
                transitions ++;
            }
            if (tZ == tMin && blockZ != endBlockZ && dZ != 0.0) {
                transZ = true;
                transitions ++;
            }

            // Advance on-block origin based on this move.
            // TODO: Calculate "directly" based on this/next block or and/t?
            oX = Math.min(1.0, Math.max(0.0, oX + tMin * dX));
            oY = Math.min(1.0, Math.max(0.0, oY + tMin * dY));
            oZ = Math.min(1.0, Math.max(0.0, oZ + tMin * dZ));

            // Advance time.
            t = Math.min(1.0, t + tMin);

            // Handle block transitions.
            if (transitions > 0) {
                if (!handleTransitions(transitions, transX, transY, transZ, tMin)) {
                    break;
                }
                // Check conditions for abort/end.
                if (forceStepEndPos && t + tol >= 1.0) {
                    // Reached the end with transitions, ensure we check the end block.
                    step(blockX, blockY, blockZ, oX, oY, oZ, 0.0, true);
                    break;
                }
            } else {
                // No transitions, finished.
                break;
            }
            // Ensure not to go beyond maxSteps.
            if (step >= maxSteps) {
                break;
            }
        }
    }

    /**
     * 
     * @param transitions
     * @param transX
     * @param transY
     * @param transZ
     * @param tMin
     * @return If to continue at all.
     */
    protected boolean handleTransitions(final int transitions, final boolean transX, final boolean transY, final boolean transZ, final double tMin) {
        // Secondary transitions.
        if (transitions > 1 && secondaryStep) {
            if (!handleSecondaryTransitions(transitions, transX, transY, transZ, tMin)) {
                return false; 
            }
        }

        // Apply all transitions to the primary line.
        double tcMin = 1.0; // Corrected absolute time to reach the resulting block position.
        if (transX) {
            if (dX > 0.0) {
                blockX ++;
                oX = 0.0;
                tcMin = Math.min(tcMin, ((double) blockX - x0) / dX);
            }
            else {
                blockX --;
                oX = 1.0;
                tcMin = Math.min(tcMin, (1.0 + (double) blockX - x0) / dX);
            }
        }
        if (transY) {
            if (dY > 0.0) {
                blockY ++;
                oY = 0.0;
                tcMin = Math.min(tcMin, ((double) blockY - y0) / dY);
            }
            else {
                blockY --;
                oY = 1.0;
                tcMin = Math.min(tcMin, (1.0 + (double) blockY - y0) / dY);
            }
        }
        if (transZ) {
            if (dZ > 0.0) {
                blockZ ++;
                oZ = 0.0;
                tcMin = Math.min(tcMin, ((double) blockZ - z0) / dZ);
            }
            else {
                blockZ --;
                oZ = 1.0;
                tcMin = Math.min(tcMin, (1.0 + (double) blockZ - z0) / dZ);
            }
        }
        // Correct time and offsets based on tcMin.
        oX = x0 + tcMin * dX - (double) blockX;
        oY = y0 + tcMin * dY - (double) blockY;
        oZ = z0 + tcMin * dZ - (double) blockZ;
        t = tcMin;
        return true; // Continue loop.
    }

    /**
     * Handle all secondary transitions (incomplete transitions).
     * @param transitions
     * @param transX
     * @param transY
     * @param transZ
     * @param tMin
     * @return If to continue at all.
     */
    protected boolean handleSecondaryTransitions(final int transitions, final boolean transX, final boolean transY, final boolean transZ, final double tMin) {
        // Handle one transition.
        if (transX) {
            if (!step(blockX + (dX > 0 ? 1 : -1), blockY, blockZ, dX > 0 ? 0.0 : 1.0, oY, oZ, 0.0, false)) {
                return false;
            }
        }
        if (transY) {
            if (!step(blockX, blockY + (dY > 0 ? 1 : -1), blockZ, oX, dY > 0 ? 0.0 : 1.0, oZ, 0.0, false)) {
                return false;
            }
        }
        if (transZ) {
            if (!step(blockX, blockY, blockZ + (dZ > 0 ? 1 : -1), oX, oY, dZ > 0 ? 0.0 : 1.0, 0.0, false)) {
                return false;
            }
        }

        // Handle double-transitions.
        if (transitions == 3) {
            if (!handleSecondaryDoubleTransitions(transitions, transX, transY, transZ, tMin)) {
                return false; 
            }
        }

        // All passed.
        return true;
    }

    /**
     * Handle secondary transitions with 2 axes at once (incomplete transitions).
     * @param transitions
     * @param transX
     * @param transY
     * @param transZ
     * @param tMin
     * @return
     */
    protected boolean handleSecondaryDoubleTransitions(final int transitions, final boolean transX, final boolean transY, final boolean transZ, final double tMin) {
        // Two transitions at once, thus step directly.
        // X and Y.
        if (!step(blockX + (dX > 0 ? 1 : -1), blockY + (dY > 0 ? 1 : -1), blockZ, dX > 0 ? 0.0 : 1.0, dY > 0 ? 0.0 : 1.0, oZ, 0.0, false)) {
            return false;
        }
        // X and Z.
        if (!step(blockX + (dX > 0 ? 1 : -1), blockY, blockZ + (dZ > 0 ? 1 : -1), dX > 0 ? 0.0 : 1.0, oY, dZ > 0 ? 0.0 : 1.0, 0.0, false)) {
            return false;
        }
        // Y and Z.
        if (!step(blockX, blockY + (dY > 0 ? 1 : -1), blockZ + (dZ > 0 ? 1 : -1), oX, dY > 0 ? 0.0 : 1.0, dZ > 0 ? 0.0 : 1.0, 0.0, false)) {
            return false;
        }
        // All passed.
        return true;
    }

    @Override
    public Axis[] getAxisOrder() {
        return new Axis[] {Axis.XYZ_AXES};
    }

    @Override
    public boolean collides() {
        return collides;
    }

    @Override
    public Axis getCollidingAxis() {
        return Axis.XYZ_AXES;
    }

    /**
     * Test if the primary line reached the end block.<br>
     * (Might later get changed to protected visibility.)
     * @return
     */
    public boolean isEndBlock() {
        return blockX == endBlockX && blockY == endBlockY && blockZ == endBlockZ;
    }

    /**
     * This is for external use. The field step will be incremented before
     * step(...) is called, thus checking it from within step means to get the
     * current step number, checking after loop gets the number of steps done.
     * 
     * @return
     */
    @Override
    public int getStepsDone() {
        return step;
    }

    @Override
    public int getMaxSteps() {
        return maxSteps;
    }

    @Override
    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    /**
     * Current block coordinate (main line).
     * @return
     */
    public int getBlockX() {
        return blockX;
    }

    /**
     * Current block coordinate (main line).
     * @return
     */
    public int getBlockY() {
        return blockY;
    }

    /**
     * Current block coordinate (main line).
     * @return
     */
    public int getBlockZ() {
        return blockZ;
    }

    /**
     * One step in the loop. Set the collides flag to indicate a specific
     * result.
     * 
     * @param blockX
     *            The block coordinates regarded in this step.
     * @param blockY
     * @param blockZ
     * @param oX
     *            Origin relative to the block coordinates.
     * @param oY
     * @param oZ
     * @param dT
     *            Amount of time regarded in this step (note that 0.0 is
     *            possible for transitions).
     * @param isPrimary
     *            If this is along the primary line, for which all transitions
     *            are done at once. The secondary line would cover all
     *            combinations of transitions off the primary line.
     * @return If to continue processing at all. Mind that the collides flag is
     *         not set based on the result, instead has to be set from within
     *         handling this method.
     */
    protected abstract boolean step(int blockX, int blockY, int blockZ, double oX, double oY, double oZ, double dT, boolean isPrimary);

}
