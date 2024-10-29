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
package fr.neatmonster.nocheatplus.compat.bukkit;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import fr.neatmonster.nocheatplus.config.ConfPaths;
import fr.neatmonster.nocheatplus.config.ConfigManager;
import fr.neatmonster.nocheatplus.logging.StaticLog;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;

/**
 * Health/Fight utility class with static access methods to bridge compatibility
 * issues, such as arising from changes in Bukkit from MC 1.5.2 to 1.6.1. <br>
 * NOTES:
 * <li>To simplify code IncompatibleClassChangeError is caught instead of
 * AbstractMethodError and NoSuchMethodError etc.</li>
 * <li>TODO: Since API dependency is now 1.6.1+, some things can be simplified
 * to just call the "int-methods".</li>
 * 
 * @author asofold
 *
 */
@SuppressWarnings("deprecation")
public class BridgeHealth {

    // TODO: Move to (smaller?) IGenericInstanceHandle instances.

    /** For debugging purposes. TODO: Reset on shutdown !? */
    private static Set<String> failures = new HashSet<String>();

    private static DamageCause getDamageCause(String name) {
        try {
            return DamageCause.valueOf(name);
        } catch (Throwable t) {
            // ouch.
            return null;
        }
    }

    public static final DamageCause DAMAGE_THORNS = getDamageCause("THORNS");
    public static final DamageCause DAMAGE_SWEEP = getDamageCause("ENTITY_SWEEP_ATTACK");

    /**
     * This method is meant to be called on API that changed from int to
     * double.<br>
     * NOTE: Might get changed to return a Number instance.
     * 
     * @param obj
     *            Object to call the method on.
     * @param methodName
     * @param RuntimeException
     *            with reason Will be thrown if no recovery from present methods
     *            is possible. If not null the first call gets logged as "API
     *            incompatibility".
     * @return
     */
    public static final double getDoubleOrInt(final Object obj, 
            final String methodName, final Throwable reason) {
        if (reason != null) {
            final String tag = obj.getClass().getName() + "." + methodName;
            if (failures.add(tag)) {
                // New entry.
                checkLogEntry(tag);
            }
        }
        final Object o1 = ReflectionUtil.invokeMethodNoArgs(obj, methodName, 
                double.class, int.class);
        if (o1 instanceof Number) {
            return ((Number) o1).doubleValue();
        }
        else{
            String message = "Expect method " + methodName + " in " + obj.getClass() + " with return type double or int, returned instead: " + ((o1 == null ? "null" : o1.getClass().getName()));
            if (reason == null) {
                throw new RuntimeException(message);
            }
            else{
                throw new RuntimeException(message, reason);
            }
        }
    }

    /**
     * Get the amount of health added with the event.
     * 
     * @param event
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static double getAmount(final EntityRegainHealthEvent event) {
        try{
            return event.getAmount();
        }
        catch(IncompatibleClassChangeError e) {
            return getDoubleOrInt(event, "getAmount", e);
        }
    }

    /**
     * Get the original/raw damage from an EntityDamageEvent (damage before
     * applying modifiers).
     * 
     * @param event
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static double getRawDamage(final EntityDamageEvent event) {
        try{
            return event.getDamage();
        }
        catch(IncompatibleClassChangeError e) {
            return getDoubleOrInt(event, "getDamage", e);
        }
    }

    /**
     * Damage amount after applying modifiers (if/as available).
     * 
     * @param event
     * @return
     */
    public static double getFinalDamage(final EntityDamageEvent event) {
        try {
            return event.getFinalDamage();
        }
        catch (Throwable t) {
            return getRawDamage(event);
        }
    }

    /**
     * Original/raw damage amount before applying modifiers. This might return
     * the final damage, in case getting the raw damage isn't implemented.
     * 
     * @param event
     * @return
     */
    public static double getOriginalDamage(final EntityDamageEvent event) {
        try {
            return event.getDamage();
        }
        catch (Throwable t) {
            return getRawDamage(event);
        }
    }

    /**
     * Set the final damage for an EntityDamageEvent. This means damage after
     * applying modifiers.
     * 
     * @param event
     * @param damage
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static void setFinalDamage(final EntityDamageEvent event, 
            final double damage) {
        try{
            event.setDamage(damage);
        }
        catch(IncompatibleClassChangeError e) {
            invokeVoid(event, "setDamage", (int) Math.round(damage), e);
        }
    }

    /**
     * Set the original/raw damage for an EntityDamageEvent. This means damage before
     * applying modifiers.
     * 
     * @param event
     * @param damage
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static void setRawDamage(final EntityDamageEvent event, 
            final double damage) {
        try {
            event.setDamage(DamageModifier.BASE, damage);
        }
        catch (Throwable t) {
            setFinalDamage(event, damage);
        }
    }

    /**
     * Multiply the final damage for an EntityDamageEvent. This might alter the
     * raw damage and/or alter existing modifiers.
     * 
     * @param event
     * @param multiplier
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static void multiplyFinalDamage(final EntityDamageEvent event, 
            final double multiplier) {
        try {
            // TODO: Better recalculate modifiers, as this scales them.
            setFinalDamage(event, event.getFinalDamage() * multiplier);
        }
        catch (Throwable e) {
            setFinalDamage(event, getRawDamage(event) * multiplier);
        }
    }

    /**
     * Get the health for an entity (LivingEntity).
     * 
     * @param entity
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static double getHealth(final LivingEntity entity) {
        try{
            return entity.getHealth();
        }
        catch(IncompatibleClassChangeError e) {
            return getDoubleOrInt(entity, "getHealth", e);
        }
    }

    /**
     * Get the maximum health for an entity (LivingEntity).
     * 
     * @param entity
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static double getMaxHealth(final LivingEntity entity) {
        try{
            // TODO: Attribute.GENERIC_MAX_HEALTH for latest.
            return entity.getMaxHealth();
        }
        catch(IncompatibleClassChangeError e) {
            return getDoubleOrInt(entity, "getMaxHealth", e);
        }
    }

    /**
     * Get the last damage for an entity (LivingEntity).
     * 
     * @param entity
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static double getLastDamage(final LivingEntity entity) {
        try{
            return entity.getLastDamage();
        }
        catch(IncompatibleClassChangeError e) {
            return getDoubleOrInt(entity, "getLastDamage", e);
        }
    }

    /**
     * Set the health for an entity (LivingEntity).
     * 
     * @param entity
     * @param health
     * @return
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static void setHealth(final LivingEntity entity, 
            final double health) {
        try{
            entity.setHealth(health);
        }
        catch(IncompatibleClassChangeError e) {
            invokeVoid(entity, "setHealth", (int) Math.round(health), e);
        }
    }

    /**
     * Damage an entity (LivingEntity).
     * 
     * @param entity
     * @param damage
     * @throws RuntimeException,
     *             in case of an IncompatibleClassChangeError without success on
     *             recovery attempts.
     */
    public static void damage(final LivingEntity entity, 
            final double damage) {
        try{
            entity.damage(damage);
        }
        catch(IncompatibleClassChangeError e) {
            invokeVoid(entity, "damage", (int) Math.round(damage), e);
        }
    }

    public static EntityDamageEvent getEntityDamageEvent(final Entity entity, 
            final DamageCause damageCause, final double damage) {
        try{
            return new EntityDamageEvent(entity, damageCause, damage);
        }
        catch(IncompatibleClassChangeError e) {
            Object o = ReflectionUtil.newInstance(
                    ReflectionUtil.getConstructor(EntityDamageEvent.class, Entity.class, DamageCause.class, int.class),
                    entity, damageCause, (int) Math.round(damage)
                );
            if (o instanceof EntityDamageEvent) {
                return (EntityDamageEvent)o;
            }
            return null;
        }
    }

    /**
     * Intended for faster compatibility methods for defined scenarios.
     * Transforms any exception to a RuntimeException.
     * 
     * @param obj
     * @param methodName
     * @param value
     */
    public static void invokeVoid(final Object obj, final String methodName, 
            final int value, final Throwable reason) {
        if (reason != null) {
            final String tag = obj.getClass().getName() + "." + methodName;
            if (failures.add(tag)) {
                checkLogEntry(tag);
            }
        }
        try {
            obj.getClass().getMethod(methodName, int.class).invoke(obj, value);
        } catch (Throwable t) {
            throw new RuntimeException("Could not invoke " + methodName + " with one argument (int) on: " + obj.getClass().getName(), reason);
        }
    }

    private static void checkLogEntry(final String tag) {
        // New entry.
        if (ConfigManager.getConfigFile().getBoolean(ConfPaths.LOGGING_EXTENDED_STATUS)) {
            StaticLog.logInfo("Try old health API: " + tag);
        }
    }

}
