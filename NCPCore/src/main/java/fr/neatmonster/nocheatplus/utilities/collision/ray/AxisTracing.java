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

import java.util.List;

import org.bukkit.Location;

import fr.neatmonster.nocheatplus.utilities.collision.Axis;
import fr.neatmonster.nocheatplus.utilities.collision.BlockPositionContainer;

/**
 * Similar to ray-tracing, attempt to model how the client processes move vs.
 * block collision via y-x-z or similar. Rough orientation is the RayTracing
 * classes or a thinkable interface, to be able to use similar test cases later
 * on.
 * <hr>
 * This implementation is meant to provide some optimum of ease of
 * implementation and performance, not necessarily elegance.
 * 
 * @author asofold
 *
 */
public abstract class AxisTracing implements ICollideBlocks, ISetMargins {

    // TODO: Consider an extra loop(coordinates + margins...) for convenience.

    /** The order of axis to be checked. */
    private final Axis[] axisOrder = new Axis[3];

    /** Start coordinates (center). */
    private double x0, y0, z0;
    /** End coordinates (center). */
    private double x1, y1, z1;
    /**
     * Margins for the bounding box, seen from center / start coordinates.
     * Positive values.
     */
    private double marginXpos, marginXneg, marginYpos, marginYneg, marginZpos, marginZneg;

    /**
     * Indicate to cut margins that are opposite the moving direction (for
     * runAxis_).
     */
    private boolean cutOppositeDirectionMargin = false;

    private boolean ignoreInitiallyColliding = false;

    /** Blocks that are to be ignored. */
    private final BlockPositionContainer ignoredBlocks = new BlockPositionContainer();

    /** Result returned with collides() and reset to false on set/loop. */
    protected boolean collides;

    /** */
    protected Axis collidesAxis;

    /**
     * Number of steps, counting advancing on one axis for all axes. Does not
     * count the number of processed blocks. The step is increased before
     * calling step.
     */
    protected int step = 0;

    /**
     * Number of steps along one axis. Resets with each axis. Incremented before
     * calling step.
     */
    protected int axisStep = 0;

    private int maxSteps = 0;

    // TODO: Margin only for iteration, not for the bounds (for use-cases like fences).

    public AxisTracing() {
        setDefaultAxisOrder();
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public int getStepsDone() {
        return step;
    }

    public int getAxisStepsDone() {
        return axisStep;
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
    public Axis[] getAxisOrder() {
        final Axis[] out = new Axis[axisOrder.length];
        System.arraycopy(axisOrder, 0, out, 0, axisOrder.length);
        return out;
    }

    /**
     * Indicate if a collision appeared during loop().
     * @return 
     */
    public boolean collides() {
        return collides;
    }

    @Override
    public Axis getCollidingAxis() {
        return collidesAxis;
    }

    /**
     * Default order is AXIS_ORDER_YXZ.
     */
    public void setDefaultAxisOrder() {
        setAxisOrder(Axis.AXIS_ORDER_YXZ);
    }

    public void setAxisOrder(Axis first, Axis second, Axis third) {
        axisOrder[0] = first;
        axisOrder[1] = second;
        axisOrder[2] = third;
    }

    /**
     * 
     * @param axisOrder
     *            Size must be three. Use Axis.NONE for skipping.
     */
    public void setAxisOrder(final List<Axis> axisOrder) {
        if (axisOrder.size() != 3) {
            throw new IllegalArgumentException("Size must be three.");
        }
        setAxisOrder(axisOrder.get(0), axisOrder.get(1), axisOrder.get(2));
    }

    @Override
    public void setMargins(final double height, final double xzMargin) {
        this.marginXneg = this.marginXpos = this.marginZneg = this.marginZpos = xzMargin;
        this.marginYneg = 0.0;
        this.marginYpos = height;
    }

    @Override
    public void setCutOppositeDirectionMargin(final boolean cutOppositeDirectionMargin) {
        this.cutOppositeDirectionMargin = cutOppositeDirectionMargin;
    }

    public void set(double x0, double y0, double z0, double x1, double y1, double z1) {
        collides = false;
        step = 0;
        this.x0 = x0;
        this.y0 = y0;
        this.z0 = z0;
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
    }

    /**
     * Loop through blocks.
     */
    public void loop() {
        // Current start coordinates.
        double x = this.x0;
        double y = this.y0;
        double z = this.z0;
        if (ignoreInitiallyColliding) {
            collectInitiallyCollidingBlocks(x0 - marginXneg, y0 - marginYneg, z0 - marginZneg, 
                    x0 + marginXpos, y0 + marginYpos, z0 + marginZpos, ignoredBlocks);
        }
        for (int i = 0; i < 3; i++) {
            final Axis axis = axisOrder[i];
            collidesAxis = axis; // Ensure here, to get it on max steps.
            if (axis == Axis.Y_AXIS) {
                runAxisY(x, y, z);
                y = this.y1;
            }
            else if (axis == Axis.X_AXIS) {
                runAxisX(x, y, z);
                x = this.x1;
            }
            else if (axis == Axis.Z_AXIS) {
                runAxisZ(x, y, z);
                z = this.z1;
            }
            else if (axis != Axis.NONE) {
                // TODO: Might still just skip these.
                // TODO: Should throw IllegalArgumentException with setting to this.
                throw new IllegalStateException("Can not ");
            }
            // NONE = skip
            if (collides) {
                break;
            }
        }
        if (ignoreInitiallyColliding) {
            ignoredBlocks.clear();
        }
    }

    protected boolean shouldCheckForIgnoredBlocks() {
        return ignoreInitiallyColliding && !ignoredBlocks.isEmpty();
    }

    protected boolean isBlockIgnored(final int x, final int y, final int z) {
        return ignoredBlocks.containsBlockPosition(x, y, z);
    }

    private void runAxisY(final double xIn, final double yIn, final double zIn) {
        // Skip if there is nothing to iterate.
        if (yIn == this.y1) {
            return;
        }
        // Iterate over axis, applying margins.
        final int increment;
        final double xMin = xIn - marginXneg;
        final double xMax = xIn + marginXpos;
        final double zMin = zIn - marginZneg;
        final double zMax = zIn + marginZpos;
        final double yStart, yEnd;
        final int iEndY;
        if (yIn < this.y1) {
            increment = 1;
            yStart = cutOppositeDirectionMargin ? yIn : (yIn -marginYneg);
            yEnd = this.y1 + marginYpos;
            iEndY = Location.locToBlock(yEnd) + 1;
        }
        else {
            increment = -1;
            yStart = cutOppositeDirectionMargin ? yIn : (yIn + marginYpos);
            yEnd = this.y1 - marginYneg;
            iEndY = Location.locToBlock(yEnd) - 1;
        }
        final int iMinX = Location.locToBlock(xMin);
        final int iMaxX = Location.locToBlock(xMax);
        final int iMinZ = Location.locToBlock(zMin);
        final int iMaxZ = Location.locToBlock(zMax);
        final int iStartY = Location.locToBlock(yStart);
        axisStep = 0;
        for (int y = iStartY; y != iEndY; y += increment) {
            ++step;
            ++axisStep;
            if (step > maxSteps) {
                return;
            }
            final boolean checkInitiallyColliding = shouldCheckForIgnoredBlocks();
            for (int x = iMinX; x <= iMaxX; x++) {
                for (int z = iMinZ; z <= iMaxZ; z++) {
                    if (checkInitiallyColliding && isBlockIgnored(x, y, z)) {
                        // Ignore.
                    }
                    else if (!step(x, y, z, xMin, increment == 1 ? yStart : yEnd, zMin, xMax, increment == 1 ? yEnd : yStart, zMax, Axis.Y_AXIS, increment)) {
                        collides = true;
                        return;
                    }
                }
            }
        }
        // No collision.
        return;
    }

    private void runAxisX(final double xIn, final double yIn, final double zIn) {
        // Skip if there is nothing to iterate.
        if (xIn == this.x1) {
            return;
        }
        // Iterate over axis, applying margins.
        final int increment;
        final double yMin = yIn - marginYneg;
        final double yMax = yIn + marginYpos;
        final double zMin = zIn - marginZneg;
        final double zMax = zIn + marginZpos;
        final double xStart, xEnd;
        final int iEndX;
        if (xIn < this.x1) {
            increment = 1;
            xStart = cutOppositeDirectionMargin ? xIn : (xIn - marginXneg);
            xEnd = this.x1 + marginXpos;
            iEndX = Location.locToBlock(xEnd) + 1;
        }
        else {
            increment = -1;
            xStart = cutOppositeDirectionMargin ? xIn : (xIn + marginXpos);
            xEnd = this.x1 - marginXneg;
            iEndX = Location.locToBlock(xEnd) - 1;
        }
        final int iMinY = Location.locToBlock(yMin);
        final int iMaxY = Location.locToBlock(yMax);
        final int iMinZ = Location.locToBlock(zMin);
        final int iMaxZ = Location.locToBlock(zMax);
        final int iStartX = Location.locToBlock(xStart);
        axisStep = 0;
        for (int x = iStartX; x != iEndX; x += increment) {
            ++step;
            ++axisStep;
            if (step > maxSteps) {
                return;
            }
            final boolean checkInitiallyColliding = shouldCheckForIgnoredBlocks();
            for (int y = iMinY; y <= iMaxY; y++) {
                for (int z = iMinZ; z <= iMaxZ; z++) {
                    if (checkInitiallyColliding && isBlockIgnored(x, y, z)) {
                        // Ignore.
                    }
                    else if (!step(x, y, z, increment == 1 ? xStart : xEnd, yMin, zMin, increment == 1 ? xEnd : xStart, yMax, zMax, Axis.X_AXIS, increment)) {
                        collides = true;
                        return;
                    }
                }
            }
        }
        // No collision.
        return;
    }

    private void runAxisZ(final double xIn, final double yIn, final double zIn) {
        // Skip if there is nothing to iterate.
        if (zIn == this.z1) {
            return;
        }
        // Iterate over axis, applying margins.
        final int increment;
        final double yMin = yIn - marginYneg;
        final double yMax = yIn + marginYpos;
        final double xMin = xIn - marginXneg;
        final double xMax = xIn + marginXpos;
        final double zStart, zEnd;
        final int iEndZ;
        if (zIn < this.z1) {
            increment = 1;
            zStart = cutOppositeDirectionMargin ? zIn : (zIn - marginZneg);
            zEnd = this.z1 + marginZpos;
            iEndZ = Location.locToBlock(zEnd + 1);
        }
        else {
            increment = -1;
            zStart = cutOppositeDirectionMargin ? zIn : (zIn + marginZpos);
            zEnd = this.z1 - marginZneg;
            iEndZ = Location.locToBlock(zEnd - 1);
        }
        final int iMinY = Location.locToBlock(yMin);
        final int iMaxY = Location.locToBlock(yMax);
        final int iMinX = Location.locToBlock(xMin);
        final int iMaxX = Location.locToBlock(xMax);
        final int iStartZ = Location.locToBlock(zStart);
        axisStep = 0;
        final boolean checkInitiallyColliding = shouldCheckForIgnoredBlocks();
        for (int z = iStartZ; z != iEndZ; z += increment) {
            ++step;
            ++axisStep;
            if (step > maxSteps) {
                return;
            }
            for (int y = iMinY; y <= iMaxY; y++) {
                for (int x = iMinX; x <= iMaxX; x++) {
                    if (checkInitiallyColliding && isBlockIgnored(x, y, z)) {
                        // Ignore.
                    }
                    else if (!step(x, y, z, xMin, yMin, increment == 1 ? zStart : zEnd, xMax, yMax, increment == 1 ? zEnd : zStart, Axis.Z_AXIS, increment)) {
                        collides = true;
                        return;
                    }
                }
            }
        }
        // No collision.
        return;
    }

    /**
     * In case ignoreInitiallyColliding is set, this will be called to do the
     * actual collision checking with the given initial margins.
     * 
     * @param minX
     * @param minY
     * @param minZ
     * @param maxX
     * @param maxY
     * @param maxZ
     * @param results
     */
    protected abstract void collectInitiallyCollidingBlocks(
            final double minX, final double minY, final double minZ,
            final double maxX, final double maxY, final double maxZ,
            final BlockPositionContainer results);

    /**
     * Check a block position for collision with the ordered bounds of a move
     * along one axis.
     * 
     * @param blockX
     *            Position of the block that might be colliding.
     * @param blockY
     * @param blockZ
     * @param minX
     *            Minimum coordinates of the move along one axis, including
     *            bounds.
     * @param minY
     * @param minZ
     * @param maxX
     *            Maximum coordinates of the move along one axis, including
     *            bounds.
     * @param maxY
     * @param maxZ
     * @param axis
     *            The axis along which the bounds are stretched.
     * @param increment
     *            Direction of the move concerning the given axis (1 means from
     *            min to max, -1 means from max to min).
     * @return Return true to continue tracing. Return false to stop and trigger
     *         a collision.
     */
    protected abstract boolean step(int blockX, int blockY, int blockZ, 
            double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            Axis axis, int increment);

}