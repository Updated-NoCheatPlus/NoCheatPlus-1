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
     * Retrieve the collision Vector of the entity from NMS.
     * 
     * @param input Meant to represent the desired speed to seek for collisions with.
     *              If no collision can't be found within the given speed, the method will return the unmodified input Vector as a result.
     *              Otherwise, a modified vector containing the "obstructed" speed is returned. 
     *              (Thus, if you wish to know if the player collided with something: desiredXYZ != collidedXYZ)       
     * @param onGround The "on ground" status of the player. Can be NCP's or Minecraft's. Do mind that if using NCP's, lost ground and mismatches must be taken into account.
     * @param cc
     * @return a Vector containing the collision components (collisionXYZ)
     */
    public Vector collide(Entity entity, Vector input, boolean onGround, MovingConfig cc);

    /**
     * The entity's "on ground" status.
     * This is controlled by the client, and thus not safe to use whatsoever.
     * Use only within specific contexts where NCP's ground logic fails.
     * 
     * @param entity
     */
    public boolean getOnGround(Entity entity);
}