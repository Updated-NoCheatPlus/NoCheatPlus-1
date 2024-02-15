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
package fr.neatmonster.nocheatplus.components.modifier;

import org.bukkit.entity.Player;

/**
 * Encapsulate attribute access. Note that some of the methods may exclude
 * specific modifiers, or otherwise perform calculations, e.g. in order to
 * return a multiplier to be applied to typical walking speed.
 * 
 * @author asofold
 *
 */
public interface IAttributeAccess {

    /**
     * Generic speed modifier as a multiplier.
     * This is global, meaning it contains every single speed modifier (slowness, speed, sprint, powder snow, soul speed etc...), unless specified otherwise.
     *
     * @param player
     * @return A multiplier for the allowed speed, excluding the sprint boost
     *         modifier (!). If not possible to determine, it should
     *         Double.MAX_VALUE.
     */
    public double getSpeedAttributeMultiplier(Player player);

    /**
     * Sprint boost modifier as a multiplier.
     * 
     * @param player
     * @return The sprint boost modifier as a multiplier. If not possible to
     *         determine, it should be Double.MAX_VALUE.
     */
    public double getSprintAttributeMultiplier(Player player);
    
    /**
     * Retrieve the player's movement speed by multiplying walk-speed with getSpeedAttributeMultiplier. <br>
     * This takes into account Bukkit's /walkspeed command as well.
     * 
     * @param player
     * @return The player speed. If not possible to
     *         determine, it should be Float.MAX_VALUE.
     */
    public float getMovementSpeed(Player player);
}
