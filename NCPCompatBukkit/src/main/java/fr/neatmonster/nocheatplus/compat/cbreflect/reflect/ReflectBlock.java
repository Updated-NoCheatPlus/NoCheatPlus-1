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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Material;

import fr.neatmonster.nocheatplus.compat.cbreflect.reflect.ReflectHelper.ReflectFailureException;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.utilities.ReflectionUtil;

/**
 * Reflection for block shape getting (latest).
 * 
 * @author asofold
 *
 */
public class ReflectBlock implements IReflectBlock {

    /** Block class. */
    public final Class<?> nmsClass;

    // Reference.
    private final ReflectBlockPosition reflectBlockPosition;
    private final ReflectAxisAlignedBB reflectAxisAlignedBB;
    private final ReflectIBlockData reflectIBlockData;
    private final ReflectIBlockAccess reflectIBlockAccess;
    private final ReflectWorld reflectWorld;

    // Static.
    public final Method nmsGetById;

    // Instance.
    public final Method nmsGetBlockData;
    /** Not the original name. */
    public final Method nmsFetchAABB;

    /**
     * 
     * @param base
     * @param reflectBlockPosition
     * @throws ClassNotFoundException
     * @throws ReflectFailureException If not available.
     */
    public ReflectBlock(ReflectBase base, ReflectBlockPosition reflectBlockPosition, 
            ReflectMaterial reflectMaterial, ReflectWorld reflectWorld) throws ClassNotFoundException {
        // Reference.
        this.reflectBlockPosition = reflectBlockPosition;
        if (reflectBlockPosition.new_nmsBlockPosition == null) {
            fail();
        }
        this.reflectAxisAlignedBB = new ReflectAxisAlignedBB(base); // Fails if not available.
        this.reflectIBlockData = new ReflectIBlockData(base, reflectMaterial); // Fails if not available.
        this.reflectWorld = reflectWorld;
        if (reflectWorld.nmsClass == null || reflectWorld.nmsGetType == null) {
            fail();
        }
        this.reflectIBlockAccess = new ReflectIBlockAccess(base);
        // Block.
        nmsClass = Class.forName(base.nmsPackageName + ".Block");
        // byID (static)
        nmsGetById = ReflectionUtil.getMethod(nmsClass, "getById", int.class);
        if (nmsGetById == null) {
            fail();
        }
        // Instance.
        this.nmsGetBlockData = ReflectionUtil.getMethodNoArgs(nmsClass, "getBlockData", reflectIBlockData.nmsClass);
        if (this.nmsGetBlockData == null) {
            fail();
        }
        this.nmsFetchAABB = determine_fetchAABB();
        if (this.nmsFetchAABB == null) {
            fail();
        }
    }

    /**
     * Fail-safe (return null if not available).
     * 
     * @return
     */
    private Method determine_fetchAABB() {
        Method nmsFetchAABB = null;

        // At least since 1.11.
        if (ServerVersion.compareMinecraftVersion("1.11") >= 0) {
            nmsFetchAABB = ReflectionUtil.getMethod(nmsClass, "b", 
                    reflectIBlockData.nmsClass, reflectIBlockAccess.nmsClass, reflectBlockPosition.nmsClass);
            if (nmsFetchAABB != null 
                    && nmsFetchAABB.getReturnType() == reflectAxisAlignedBB.nmsClass
                    && nmsFetchAABB.isAnnotationPresent(Deprecated.class) // TODO: Why deprecated?
                    ) {
                return nmsFetchAABB;
            }
        }
        // Legacy (pre 1.11, possibly even before.)
        else if (nmsFetchAABB == null) {
            nmsFetchAABB = ReflectionUtil.getMethod(nmsClass, "a", 
                    reflectIBlockData.nmsClass, reflectIBlockAccess.nmsClass, reflectBlockPosition.nmsClass);
            if (nmsFetchAABB != null 
                    && nmsFetchAABB.getReturnType() == reflectAxisAlignedBB.nmsClass
                    && nmsFetchAABB.isAnnotationPresent(Deprecated.class) // TODO: Why deprecated?
                    ) {
                return nmsFetchAABB;
            }
        }

        // Nothing found.
        return null;
    }

    /**
     * Quick fail with exception.
     */
    private void fail() {
        throw new ReflectFailureException();
    }

    private Object nmsBlockPosition(final int x, final int y, final int z) {
        final Object blockPos = ReflectionUtil.newInstance(this.reflectBlockPosition.new_nmsBlockPosition, x, y, z);
        if (blockPos == null) {
            fail();
        }
        return blockPos;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Object nms_getByMaterial(final Material id) {
        if (this.nmsGetById == null) {
            fail();
        }
        return ReflectionUtil.invokeMethod(this.nmsGetById, null, id.getId());
    }

    /**
     * 
     * @param block
     *            Instance of Block.
     * @return
     */
    private Object nms_getBlockData(final Object block) {
        try {
            return nmsGetBlockData.invoke(block);
        } catch (IllegalAccessException e) {
            throw new ReflectFailureException();
        } catch (IllegalArgumentException e) {
            throw new ReflectFailureException();
        } catch (InvocationTargetException e) {
            throw new ReflectFailureException();
        }
    }

    @Override
    public Object nms_getMaterial(final Object block) {
        return reflectIBlockData.nms_getMaterial(nms_getBlockData(block));
    }

    @Override
    public double[] nms_fetchBounds(final Object nmsWorld, final Object nmsBlock, 
            final int x, final int y, final int z) {
        final Object pos = nmsBlockPosition(x, y, z);
        final Object blockData = reflectWorld.nms_getType(nmsWorld, pos);
        final Object bb = nms_fetchAABB(nmsBlock, blockData, nmsWorld, pos);
        if (bb == null) {
            return new double[] {0.0, 0.0, 0.0, 1.0, 1.0, 1.0}; // Special case.
            //return null;
        }
        return reflectAxisAlignedBB.fillInValues(bb, new double[6]);
    }

    private Object nms_fetchAABB(final Object nmsBlock, final Object iBlockData, final Object iBlockAccess, final Object blockPosition) {
        try {
            return nmsFetchAABB.invoke(nmsBlock, iBlockData, iBlockAccess, blockPosition);
        } catch (IllegalAccessException e) {
            throw new ReflectFailureException();
        } catch (IllegalArgumentException e) {
            throw new ReflectFailureException();
        } catch (InvocationTargetException e) {
            throw new ReflectFailureException();
        }
    }

    @Override
    public boolean isFetchBoundsAvailable() {
        // Available, if initialized at all.
        return true;
    }

}
