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
package fr.neatmonster.nocheatplus.compat.spigotcb1_8_R2;

import org.bukkit.craftbukkit.v1_8_R2.entity.CraftLivingEntity;
import org.bukkit.entity.Player;

import net.minecraft.server.v1_8_R2.AttributeInstance;
import net.minecraft.server.v1_8_R2.AttributeModifier;
import net.minecraft.server.v1_8_R2.GenericAttributes;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.compat.AttribUtil;
import fr.neatmonster.nocheatplus.compat.bukkit.BridgeEnchant;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;

public class AttributeAccess implements IAttributeAccess {

    public AttributeAccess() {
        if (ReflectionUtil.getClass("net.minecraft.server.v1_8_R2.AttributeInstance") == null) {
            throw new RuntimeException("Service not available.");
        }
    }

    @Override
    public double getSpeedMultiplier(Player player) {
        final AttributeInstance attr = ((CraftLivingEntity) player).getHandle().getAttributeInstance(GenericAttributes.d);
        final double val = attr.getValue() / attr.b();
        final AttributeModifier mod = attr.a(AttribUtil.ID_SPRINT_BOOST);
        if (mod == null) {
            return val;
        } 
        else {
            return val / AttribUtil.getMultiplier(mod.c(), mod.d());
        }
    }
    
    @Override
    public float getMovementSpeed(final Player player) {
        // / by 2 to get the base value 0.1f
        return (player.getWalkSpeed() / 2f) * (float)getSpeedMultiplier(player);
    }

    @Override
    public double getSprintMultiplier(Player player) {
        final AttributeModifier mod = ((CraftLivingEntity) player).getHandle().getAttributeInstance(GenericAttributes.d).a(AttribUtil.ID_SPRINT_BOOST);
        if (mod == null) {
            return 1.0;
        } 
        else {
            return AttribUtil.getMultiplier(mod.c(), mod.d());
        }
    }
    
    ////////////////////////////////////////////////////////////////
    // Modern attributes. Not available for legacy versions.      //
    ////////////////////////////////////////////////////////////////
    @Override
    public double getGravity(Player player) {
        return Magic.DEFAULT_GRAVITY;
    }
    
    @Override
    public double getSafeFallDistance(Player player) {
        return Magic.FALL_DAMAGE_DIST;
    }
    
    @Override
    public double getFallDamageMultiplier(Player player) {
        return 1.0;
    }
    
    @Override
    public double getBreakingSpeedMultiplier(Player player) {
        return 1.0;
    }
    
    @Override
    public double getJumpGainMultiplier(Player player) {
        return 1.0;
    }
    
    @Override
    public double getPlayerSneakingFactor(Player player) {
        return Magic.SNEAK_MULTIPLIER;
    }
    
    @Override
    public double getPlayerMaxBlockReach(Player player) {
        return 4.5;
    }
    
    @Override
    public double getPlayerMaxAttackReach(Player player) {
        return 3.0;
    }
    
    @Override
    public double getMaxStepUp(Player player) {
        final MovingConfig cc = DataManager.getPlayerData(player).getGenericInstance(MovingConfig.class);
        return cc.sfStepHeight;
    }
    
    @Override
    public float getMovementEfficiency(Player player) {
        return 0.0f;
    }
    
    @Override
    public float getWaterMovementEfficiency(Player player) {
        return BridgeEnchant.getDepthStriderLevel(player);
    }
    
    @Override
    public double getSubmergedMiningSpeedMultiplier(Player player) {
        return 1.0;
    }
    
    @Override
    public double getMiningEfficiency(Player player) {
        return 0.0;
    }
    
    @Override
    public double getEntityScale(Player player) {
        return 1.0;
    }

}
