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
package fr.neatmonster.nocheatplus.compat.cbreflect.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.compat.AttribUtil;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;

public class ReflectAttributeAccess implements IAttributeAccess {

    private static class ReflectGenericAttributes {
        public final Object nmsMOVEMENT_SPEED;

        public ReflectGenericAttributes(ReflectBase base) throws ClassNotFoundException {
            Class<?> clazz = Class.forName(base.nmsPackageName + ".GenericAttributes");
            Class<?> clazzIAttribute = Class.forName(base.nmsPackageName + ".IAttribute");
            Object nmsMOVEMENT_SPEED = null;
            nmsMOVEMENT_SPEED = get_nmsMOVEMENT_SPEED("MOVEMENT_SPEED", clazz, clazzIAttribute);
            if (nmsMOVEMENT_SPEED == null) {
                nmsMOVEMENT_SPEED = get_nmsMOVEMENT_SPEED("d", clazz, clazzIAttribute);
            }
            if (nmsMOVEMENT_SPEED == null) {
                throw new RuntimeException("Not available.");
            }
            else {
                this.nmsMOVEMENT_SPEED = nmsMOVEMENT_SPEED;
            }
        }

        private Object get_nmsMOVEMENT_SPEED(String fieldName, Class<?> clazz, Class<?> type) {
            Field field = ReflectionUtil.getField(clazz, fieldName, type);
            if (field != null) {
                return ReflectionUtil.get(field, clazz, null);
            }
            else {
                return null;
            }
        }

    }

    private static class ReflectAttributeInstance {
        /** (Custom naming.) */
        public final Method nmsGetBaseValue;
        public final Method nmsGetValue;

        /** (Custom naming.) */
        public final Method nmsGetAttributeModifier;

        public ReflectAttributeInstance(ReflectBase base) throws ClassNotFoundException {
            Class<?> clazz = Class.forName(base.nmsPackageName + ".AttributeInstance");
            // Base value.
            Method method = ReflectionUtil.getMethodNoArgs(clazz, "b", double.class);
            if (method == null) {
                // TODO: Consider to search (as long as only two exist).
                method = ReflectionUtil.getMethodNoArgs(clazz, "getBaseValue", double.class);
                if (method == null) {
                    method = ReflectionUtil.getMethodNoArgs(clazz, "getBase", double.class);
                }
            }
            nmsGetBaseValue = method;
            // Value (final value).
            method = ReflectionUtil.getMethodNoArgs(clazz, "getValue", double.class);
            if (method == null) {
                // TODO: Consider to search (as long as only two exist).
                method = ReflectionUtil.getMethodNoArgs(clazz, "e", double.class); // 1.6.1
            }
            nmsGetValue = method;
            // Get AttributeModifier.
            // TODO: If name changes: scan.
            method = ReflectionUtil.getMethod(clazz, "a", UUID.class);
            if (method == null) {
                method = ReflectionUtil.getMethod(clazz, "getAttributeModifier", UUID.class);
                if (method == null) {
                    method = ReflectionUtil.getMethod(clazz, "getModifier", UUID.class);
                }
            }
            nmsGetAttributeModifier = method;
            if (nmsGetAttributeModifier == null || nmsGetBaseValue == null || nmsGetValue == null) {
                throw new RuntimeException("Not available.");
            }
        }

    }

    private static class ReflectAttributeModifier {
        /** (Custom naming.) */
        public Method nmsGetOperation;
        /** (Custom naming.) */
        public Method nmsGetValue;

        public ReflectAttributeModifier(ReflectBase base) throws ClassNotFoundException {
            Class<?> clazz = Class.forName(base.nmsPackageName + ".AttributeModifier");
            // TODO: Scan in a more future proof way.
            nmsGetOperation = ReflectionUtil.getMethodNoArgs(clazz, "c", int.class);
            nmsGetValue = ReflectionUtil.getMethodNoArgs(clazz, "d", double.class);
            if (nmsGetOperation == null || nmsGetValue == null) {
                throw new RuntimeException("Not available.");
            }
        }

    }

    // TODO: Register each and every one of these as generic instances and fetch from there.
    private ReflectBase reflectBase;
    private ReflectGenericAttributes reflectGenericAttributes;
    private ReflectAttributeInstance reflectAttributeInstance;
    private ReflectAttributeModifier reflectAttributeModifier;
    private ReflectPlayer reflectPlayer;

    public ReflectAttributeAccess() {
        try {
            this.reflectBase = new ReflectBase();
            ReflectAxisAlignedBB reflectAxisAlignedBB = null;
            try {
                reflectAxisAlignedBB = new ReflectAxisAlignedBB(reflectBase);
            }
            catch (NullPointerException e) {}
            reflectGenericAttributes = new ReflectGenericAttributes(this.reflectBase);
            reflectAttributeInstance = new ReflectAttributeInstance(this.reflectBase);
            reflectAttributeModifier = new ReflectAttributeModifier(this.reflectBase);
            reflectPlayer = new ReflectPlayer(reflectBase, reflectAxisAlignedBB, new ReflectDamageSource(reflectBase));
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Not available.");
        }
        if (reflectBase.nmsPackageName == null || reflectBase.obcPackageName == null
                || reflectPlayer.obcGetHandle == null) {
            throw new RuntimeException("Not available.");
        }
    }
    
    /**
     * Get the speed attribute (MOVEMENT_SPEED) for a player.
     * @param handle EntityPlayer
     * @return AttributeInstance
     */
    private Object getMovementSpeedAttributeInstance(Player player) {
        return ReflectionUtil.invokeMethod(this.reflectPlayer.nmsGetAttributeInstance, getHandle(player), this.reflectGenericAttributes.nmsMOVEMENT_SPEED);
    }
    
    /**
     *
     * @param player
     * @param removeSprint If to calculate away the sprint boost modifier.
     * @return
     */
    private double getSpeedAttributeMultiplier(Player player, boolean removeSprint) {
        Object attributeInstance = getMovementSpeedAttributeInstance(player);
        double val = ((Double) ReflectionUtil.invokeMethodNoArgs(this.reflectAttributeInstance.nmsGetValue, attributeInstance)).doubleValue() / ((Double) ReflectionUtil.invokeMethodNoArgs(this.reflectAttributeInstance.nmsGetBaseValue, attributeInstance)).doubleValue();
        if (!removeSprint) {
            return val;
        }
        else {
            final double sprintModifier = nmsAttributeInstance_getSprintAttributeModifierMultiplier(attributeInstance);
            if (sprintModifier == 1.0) {
                return val;
            }
            else {
                return val / sprintModifier;
            }
        }
    }
    
    /**
     * (Not an existing method.)
     * @param attributeInstance
     */
    private double nmsAttributeInstance_getSprintAttributeModifierMultiplier(Object attributeInstance) {
        Object mod = ReflectionUtil.invokeMethod(this.reflectAttributeInstance.nmsGetAttributeModifier, attributeInstance, AttribUtil.ID_SPRINT_BOOST);
        if (mod == null) {
            return 1.0;
        }
        else {
            return AttribUtil.getMultiplier((Integer) ReflectionUtil.invokeMethodNoArgs(this.reflectAttributeModifier.nmsGetOperation, mod), (Double) ReflectionUtil.invokeMethodNoArgs(this.reflectAttributeModifier.nmsGetValue, mod));
        }
    }
    
    private Object getHandle(Player player) {
        Object handle = ReflectionUtil.invokeMethodNoArgs(this.reflectPlayer.obcGetHandle, player);
        return handle;
    }
    
    @Override
    public double getSpeedMultiplier(Player player) {
        return getSpeedAttributeMultiplier(player, true);
    }
    
    @Override
    public float getMovementSpeed(final Player player) {
        // / by 2 to get the base value 0.1f
        return (player.getWalkSpeed() / 2f) * (float) getSpeedMultiplier(player);
    }
    
    @Override
    public double getSprintMultiplier(Player player) {
        return nmsAttributeInstance_getSprintAttributeModifierMultiplier(getMovementSpeedAttributeInstance(player));
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
