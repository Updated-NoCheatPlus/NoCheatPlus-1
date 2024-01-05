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

import java.util.List;

import org.bukkit.craftbukkit.v1_20_R2.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.components.entity.IEntityAccessCollide;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EntityAccessCollide implements IEntityAccessCollide{

    @Override
    public Vector collide(Entity entity, Vector input, boolean onGround, MovingConfig cc, double[] ncpAABB) {
        net.minecraft.world.entity.Entity entityNMS = ((CraftEntity)entity).getHandle();
        // We must use the last bounding box here. Unless specified otherwise.
        AxisAlignedBB bb = ncpAABB == null ? entityNMS.cG() : new AxisAlignedBB(ncpAABB[0], ncpAABB[1], ncpAABB[2], ncpAABB[3], ncpAABB[4], ncpAABB[5]);
        Vec3D cInput = new Vec3D(input.getX(), input.getY(), input.getZ());
        List<VoxelShape> list = entityNMS.dL().c(entityNMS, bb.c(cInput));
        Vec3D vec3 = input.lengthSquared() == 0.0 ? cInput : net.minecraft.world.entity.Entity.a(entityNMS, cInput, bb, entityNMS.dL(), list);
        boolean collideX = cInput.c != vec3.c;
        boolean collideY = cInput.d != vec3.d;
        boolean collideZ = cInput.e != vec3.e;
        boolean touchGround = onGround || collideY && vec3.d < 0.0;
        //cc.sfStepHeight > 0.0
        if (entityNMS.dF() > 0.0 && touchGround && (collideX || collideZ)) {
            Vec3D vec31 = net.minecraft.world.entity.Entity.a(entityNMS, new Vec3D(input.getX(), entityNMS.dF(), input.getZ()), bb, entityNMS.dL(), list);
            Vec3D vec32 = net.minecraft.world.entity.Entity.a(entityNMS, new Vec3D(0.0, entityNMS.dF(), 0.0), bb.c(input.getX(), 0.0, input.getZ()), entityNMS.dL(), list);
            if (vec32.d < entityNMS.dF()) {
                Vec3D vec33 = net.minecraft.world.entity.Entity.a(entityNMS, new Vec3D(input.getX(), 0.0, input.getZ()), bb.c(vec32), entityNMS.dL(), list).e(vec32);
                if (vec33.i() > vec31.i()) vec31 = vec33;
            }
            
            if (vec31.i() > vec3.i()) {
                Vec3D tmp = vec31.e(net.minecraft.world.entity.Entity.a(entityNMS, new Vec3D(0.0, -vec31.d + cInput.d, 0.0), bb.c(vec31), entityNMS.dL(), list));
                return new Vector(tmp.c, tmp.d, tmp.e);
            }
        }
        return new Vector(vec3.c, vec3.d, vec3.e);
    }

    @Override
    public boolean getOnGround(Entity entity) {
        net.minecraft.world.entity.Entity entityNMS = ((CraftEntity)entity).getHandle();
        return entityNMS.aA();
    }
}
