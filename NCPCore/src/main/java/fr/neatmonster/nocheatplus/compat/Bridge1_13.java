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
package fr.neatmonster.nocheatplus.compat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;
import fr.neatmonster.nocheatplus.utilities.entity.PotionUtil;

public class Bridge1_13 {
    private static final PotionEffectType SLOWFALLING = PotionEffectType.getByName("SLOW_FALLING");
    private static final PotionEffectType DOLPHINSGRACE = PotionEffectType.getByName("DOLPHINS_GRACE");
    private static final PotionEffectType CONDUIT_POWER = PotionEffectType.getByName("CONDUIT_POWER");
    private static final boolean hasIsRiptiding = ReflectionUtil.getMethodNoArgs(LivingEntity.class, "isRiptiding", boolean.class) != null;
    private static final boolean hasIsSwimming = ReflectionUtil.getMethodNoArgs(LivingEntity.class, "isSwimming", boolean.class) != null;
    private static final boolean hasBoundingBox = ReflectionUtil.getClass("org.bukkit.util.BoundingBox") != null;
    private static final boolean hasPlayerRiptideEvent = ReflectionUtil.getClass("org.bukkit.event.player.PlayerRiptideEvent") != null;
    // Bukkit introduced a built-in ray-tracing system in 1.13.2
    private static final boolean hasBuiltInRayTracing = ReflectionUtil.getClass("org.bukkit.util.RayTraceResult") != null;

    public static boolean hasSlowfalling() {
        return SLOWFALLING != null;
    }

    public static boolean hasDolphinGrace() {
        return DOLPHINSGRACE != null;
    }

    public static boolean hasConduitPower() {
        return CONDUIT_POWER != null;
    }

    public static boolean hasIsRiptiding() {
        return hasIsRiptiding;
    }

    public static boolean hasIsSwimming() {
        return hasIsSwimming;
    }

    public static boolean hasBoundingBox() {
        return hasBoundingBox;
    }

    public static boolean hasPlayerRiptideEvent() {
        return hasPlayerRiptideEvent;
    }

    public static boolean hasBuiltInRayTracing() {
        return hasBuiltInRayTracing;
    }

    /**
     * Test for the 'slowfalling' potion effect.
     * 
     * @param LivingEntity
     * @return Double.NEGATIVE_INFINITY if not present.
     */
    public static double getSlowfallingAmplifier(final LivingEntity entity) {
        if (SLOWFALLING == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return PotionUtil.getPotionEffectAmplifier(entity, SLOWFALLING);
    }

    public static double getDolphinGraceAmplifier(final LivingEntity entity) {
        if (DOLPHINSGRACE == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return PotionUtil.getPotionEffectAmplifier(entity, DOLPHINSGRACE);
    }

    public static double getConduitPowerAmplifier(final LivingEntity entity) {
        if (CONDUIT_POWER == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return PotionUtil.getPotionEffectAmplifier(entity, CONDUIT_POWER);
    }

    public static boolean isRiptiding(final LivingEntity entity) {
        return hasIsRiptiding ? entity.isRiptiding() : false;
    }

    public static boolean isSwimming(final LivingEntity entity) {
        return hasIsSwimming ? entity.isSwimming() : false;
    }
}
