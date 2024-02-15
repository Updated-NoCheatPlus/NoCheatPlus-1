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
package fr.neatmonster.nocheatplus.components.entity;

import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;

public interface IEntityAccessCollide {

    /**
     * Make the entity collide with the given speed.
     *
     * @param entity   The entity to test collisions with.
     * @param input    Meant to represent the desired speed to seek for collisions with.<br>
     *                 If no collision can be found within the given speed, the method will return the unmodified input Vector as a result.
     *                 Otherwise, a modified vector containing the "obstructed" speed is returned. <br>
     *                 (Thus, if you wish to know if the player collided with something: desiredXYZ != collidedXYZ)
     * @param onGround The "on ground" status of the entity. Can be NCP's or Minecraft's. Used to determine whether the entity will be able to step up with the given motion;
     *                 if the given onGround status is false, the function will attempt to estimate the ground collision by using the given vertical motion. <br>
     *                 Do mind that if using NCP's, lost ground and mismatches must be taken into account.
     * @param cc
     * @param ncpAABB The AABB of the entity at the position they moved from (in other words, the last AABB of the entity); may be null.<br>
     *                Only makes sense if you intend to call this method during PlayerMoveEvents, because the NMS bounding box will already be moved to the event#getTo() Location, by the time this gets called by moving checks.
     *                If null, the default NMS bounding box will be used instead.
     *
     * @return A Vector containing the collision components (collisionXYZ)
     */
    public Vector collide(Entity entity, Vector input, boolean onGround, MovingConfig cc, double[] ncpAABB);

    /**
     * The entity's "on ground" status.
     * This is controlled by the client, and thus not safe to use whatsoever.
     * Use only within specific contexts where NCP's ground logic may fail.
     * 
     * @param entity
     *
     * @return True, if on ground by the client's definition (=collision below and negative motion(!)), false otherwise.
     */
    public boolean getOnGround(Entity entity);
}