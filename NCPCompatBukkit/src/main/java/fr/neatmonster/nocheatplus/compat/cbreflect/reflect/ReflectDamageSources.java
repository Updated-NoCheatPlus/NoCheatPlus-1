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

import java.lang.reflect.Method;

import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;

public class ReflectDamageSources {
    
    public Class<?> nmsClass;

    public Method nmsDamageSources;
    public Method nmsfall;

    public ReflectDamageSources(ReflectBase base, ReflectDamageSource reflectDamageSource) throws ClassNotFoundException {
        this(base, reflectDamageSource, Class.forName(base.nmsPackageName + ".Entity"));
    }

    public ReflectDamageSources(ReflectBase base, ReflectDamageSource reflectDamageSource, Class<?> nmsEntityClass) throws ClassNotFoundException {
        nmsClass = Class.forName("net.minecraft.world.damagesource.DamageSources");
        // 1.19 Entity.damageSources()
        nmsDamageSources = ReflectionUtil.getMethodNoArgs(nmsEntityClass, "dG", nmsClass);
        // 1.20
        if (nmsDamageSources == null) {
            nmsDamageSources = ReflectionUtil.getMethodNoArgs(nmsEntityClass, "dJ", nmsClass);
        }
        // 1.20.2
        if (nmsDamageSources == null) {
            nmsDamageSources = ReflectionUtil.getMethodNoArgs(nmsEntityClass, "dM", nmsClass);
        }
        nmsfall = ReflectionUtil.getMethodNoArgs(nmsClass, "k", reflectDamageSource.nmsClass);
    }

    public Object getDamageSource(Object handle) {
        if (nmsDamageSources == null || nmsfall == null) return null;
        Object damageSources = ReflectionUtil.invokeMethodNoArgs(nmsDamageSources, handle);
        return ReflectionUtil.invokeMethodNoArgs(nmsfall, damageSources);
    }
}
