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
 * Default implementation for access not being available.
 * 
 * @author asofold
 *
 */
public class DummyAttributeAccess implements IAttributeAccess {

    @Override
    public double getSpeedMultiplier(Player player) {
        return Double.MAX_VALUE;
    }

    @Override
    public double getSprintMultiplier(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public float getMovementSpeed(Player player) {
    	return Float.MAX_VALUE;
    }
    
    @Override
    public double getGravity(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getSafeFallDistance(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getFallDamageMultiplier(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getBreakingSpeedMultiplier(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getJumpGainMultiplier(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getPlayerSneakingFactor(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getPlayerMaxBlockReach(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getPlayerMaxAttackReach(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getMaxStepUp(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public float getMovementEfficiency(Player player) {
        return Float.MAX_VALUE;
    }
    
    @Override
    public float getWaterMovementEfficiency(Player player) {
         return Float.MAX_VALUE;
    }
    
    @Override
    public double getSubmergedMiningSpeedMultiplier(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getMiningEfficiency(Player player) {
        return Double.MAX_VALUE;
    }
    
    @Override
    public double getEntityScale(Player player) {
        return Double.MAX_VALUE;
    }
}
