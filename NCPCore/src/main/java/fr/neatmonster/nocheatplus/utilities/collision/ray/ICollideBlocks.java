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

import fr.neatmonster.nocheatplus.utilities.collision.Axis;

/**
 * A collision checker much like RayTracing or AxisTracing.
 * 
 * @author asofold
 *
 */
public interface ICollideBlocks {

    // TODO: number of visited blocks?

    /**
     * Set the maximum steps to be done during loop() along the primary line.
     * Setting to 0 should disable the upper limit. (Integer.MAX_VALUE will be
     * on the safe side as well.) The primary line is meant to somehow reflect
     * run time complexity, not necessarily each visited block.
     * 
     * @param maxSteps
     */
    // TODO: Not sure :p.
    public void setMaxSteps(int maxSteps);
    public int getMaxSteps();

    /**
     * Call before loop, in order to skip checking blocks that are found to be
     * colliding at the start of loop. May or may not have any effect. This must
     * not be called during processing of loop.
     * 
     * @param ignoreInitiallyColliding
     */
    public void setIgnoreInitiallyColliding(boolean ignoreInitiallyColliding);
    public boolean getIgnoreInitiallyColliding();

    /**
     * Call before loop to set the coordinates of a move.
     * 
     * @param x0
     * @param y0
     * @param z0
     * @param x1
     * @param y1
     * @param z1
     */
    public void set(double x0, double y0, double z0, double x1, double y1, double z1);

    /**
     * Run the collision checking.
     */
    public void loop();

    /**
     * Get the (primary) steps done during loop(). In case of aborting due to
     * the max steps limit, the result should be greater or equal to
     * getMaxSteps().
     * 
     * @return
     */
    public int getStepsDone();

    /**
     * Test if the testing found a collision during loop(). This should reset
     * automatically with calling set.
     * 
     * @return
     */
    public boolean collides();

    /**
     * Optional information about which (sub-) type of checking lead to
     * collision. Should only be considered valid, if collides() returns true.
     * Implementation-specific.
     * 
     * @return
     */
    public Axis getCollidingAxis();

    /**
     * Get an array with the axes in checking order. Length may vary, depending
     * on the implementation.
     * 
     * @return
     */
    public Axis[] getAxisOrder();

}
