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

import fr.neatmonster.nocheatplus.components.location.IGetBox3D;
import fr.neatmonster.nocheatplus.components.location.IGetPosition;

/**
 * Collide a ray with an axis aligned bounding box (AABB). Allow fetching the
 * point of time, when the ray is closest to the AABB. The methods getX|Y|Z from
 * IGetPosition will fetch the coordinates of collision or the nearest point (if
 * set so). That nearest point may be an estimation, not necessarily consistent
 * for all cases/directions. By default colliding with the edges of the box
 * would count.
 * 
 * @author asofold
 *
 */
public interface ICollideRayVsAABB extends IGetPosition {

    // TODO: Control of counting in colliding with the edge?
    // TODO: Method to get the maximum per-axis distance in case of not colliding and findNearest... set.
    // TODO: Convenience methods to retrieve distances (especially in case of not colliding).
    // TODO: Convenience methods for other argument types (vector, IGet..., double[], setAABB(block/ints).
    // TODO: Implement fight.visible, use in BlockBreak.direction/visible.

    /**
     * Set the start and direction of the ray. This can be called independently
     * of other calls to setAABB and loop (etc.), in order to reiterate with an
     * altered ray to start with.
     * 
     * @param startX
     *            Starting point.
     * @param startY
     * @param startZ
     * @param dirX
     *            Direction of the ray. One time unit matches the length of this
     *            vector.
     * @param dirY
     * @param dirZ
     * @return The same instance for chaining.
     */
    public ICollideRayVsAABB setRay(double startX, double startY, double startZ,
                                    double dirX, double dirY, double dirZ);

    /**
     * Set the properties of the AABB, using foot-center positions and margins.
     * This can be called independently of setRay and loop, in order to
     * reiterate with a different AABB to check against.
     * 
     * @param targetX
     *            Bottom center coordinates of the AABB, e.g. the foot location
     *            of a player.
     * @param targetY
     * @param targetZ
     * @param boxMarginHorizontal
     *            Margin from the center to the x/z-sides.
     * @param boxMarginVertical
     *            Margin from targetY to the top of the box.
     * @return The same instance for chaining.
     */
    public ICollideRayVsAABB setAABB(double targetX, double targetY, double targetZ,
                                     double boxMarginHorizontal, double boxMarginVertical);

    /**
     * Set the properties of the AABB, using a given boxIGetBox3D instance.
     * 
     * @param box
     *            The 3D bounding box to set.
     * @return The same instance for chaining.
     */
    public ICollideRayVsAABB setAABB(IGetBox3D box);

    /**
     * Set the properties of the AABB, using block coordinates and a margin.
     * This can be called independently of setRay and loop, in order to
     * reiterate with a different AABB to check against.
     * 
     * @param targetX
     *            The block coordinates. This is the minimum coordinates, from
     *            which on 1.0 will be added for the maximum coordinates.
     * @param targetY
     * @param targetZ
     * @param margin
     *            A margin to apply towards all directions.
     * @return The same instance for chaining.
     */
    public ICollideRayVsAABB setAABB(int targetX, int targetY, int targetZ, double margin);

    /**
     * Set the properties of the AABB directly. This can be called independently
     * of setRay and loop, in order to reiterate with a different AABB to check
     * against.
     * 
     * @param minX
     * @param minY
     * @param minZ
     * @param maxX
     * @param maxY
     * @param maxZ
     * @return The same instance for chaining.
     */
    public ICollideRayVsAABB setAABB(double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ);

    /**
     * Set if the nearest point is to be estimated instead, in case the ray does
     * not collide.
     * 
     * @param findNearestPointIfNotCollide
     *            If set to true, getTime/getX/getY/getZ will return the point
     *            on the ray that is closest to the AABB (Implementation may
     *            allow some imprecision, as well as starting from 0 always).
     * @return The same instance for chaining.
     */
    public ICollideRayVsAABB setFindNearestPointIfNotCollide(boolean findNearestPointIfNotCollide);

    /**
     * Test if the nearest point is to be estimated instead, in case the ray
     * does not collide.
     * 
     * @return
     */
    public boolean getFindNearestPointIfNotCollide();

    /**
     * Run the actual checking once. This can be run multiple times with varying
     * arguments independently of setRay and setAABB (etc.).
     * 
     * @return The same instance for chaining.
     */
    public ICollideRayVsAABB loop();

    /**
     * Just fetch the result after calling loop.
     * 
     * @return If the ray collides the AABB
     */
    public boolean collides();

    /**
     * Get some kind of squared distance from the nearest point towards the
     * AABB, in case of not colliding and findNearestPointIfNotCollid being set.
     * 
     * @return 0.0 if colliding.
     */
    public double getClosestDistanceSquared();

    /**
     * Earliest time of collision if collides() returns true, or of the nearest
     * point, counted in times of applying the direction.
     * 
     * @return Time in multiples of the initial direction vector. In case it's
     *         not possible at all, 0.0 might be returned.
     */
    public double getTime();

}
