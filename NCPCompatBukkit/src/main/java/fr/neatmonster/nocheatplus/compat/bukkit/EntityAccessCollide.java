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
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.components.entity.IEntityAccessCollide;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;

import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EntityAccessCollide implements IEntityAccessCollide {

    @Override
    public Vector collide(Entity entity, Vector input, boolean onGround, MovingConfig cc, double[] ncpAABB) {
        net.minecraft.world.entity.Entity entityNMS = ((CraftEntity)entity).getHandle();
        // We must use the last bounding box here. Unless specified otherwise.
        AxisAlignedBB AABB = ncpAABB == null ? entityNMS.cG() : new AxisAlignedBB(ncpAABB[0], ncpAABB[1], ncpAABB[2], ncpAABB[3], ncpAABB[4], ncpAABB[5]);
        Vec3D inputToVec3 = new Vec3D(input.getX(), input.getY(), input.getZ());
        List<VoxelShape> list = entityNMS.dL().c(entityNMS, AABB.b(inputToVec3));
        Vec3D collisionVector = input.lengthSquared() == 0.0 ? inputToVec3 : net.minecraft.world.entity.Entity.a(entityNMS, inputToVec3, AABB, entityNMS.dL(), list);
        boolean collideX = inputToVec3.c != collisionVector.c;
        boolean collideY = inputToVec3.d != collisionVector.d;
        boolean collideZ = inputToVec3.e != collisionVector.e;
        boolean touchGround = onGround || collideY && collisionVector.d < 0.0;
        // TODO: Not only cc.sfStepHeight (0.6), change on vehicle(boats:0.0, other vehicle 1.0)
        // How Minecraft handles stepping
        if (cc.sfStepHeight > 0.0 && touchGround && (collideX || collideZ)) {
            Vec3D vec31 = net.minecraft.world.entity.Entity.a(entityNMS, new Vec3D(input.getX(), cc.sfStepHeight, input.getZ()), AABB, entityNMS.dL(), list);
            Vec3D vec32 = net.minecraft.world.entity.Entity.a(entityNMS, new Vec3D(0.0, cc.sfStepHeight, 0.0), AABB.b(input.getX(), 0.0, input.getZ()), entityNMS.dL(), list);
            final IPlayerData pData = DataManager.getPlayerData((Player)(entity));
            // 1.7 and below don't have this "step up fix".
            // https://youtu.be/Awa9mZQwVi8?t=114
            if (vec32.d < cc.sfStepHeight && !pData.getClientVersion().isOlderThan(ClientVersion.V_1_8)) {
                Vec3D vec33 = net.minecraft.world.entity.Entity.a(entityNMS, new Vec3D(input.getX(), 0.0, input.getZ()), AABB.c(vec32), entityNMS.dL(), list).e(vec32);
                if (vec33.i() > vec31.i()) vec31 = vec33;
            }
            
            if (vec31.i() > collisionVector.i()) {
                Vec3D tmp = vec31.e(net.minecraft.world.entity.Entity.a(entityNMS, new Vec3D(0.0, -vec31.d + inputToVec3.d, 0.0), AABB.c(vec31), entityNMS.dL(), list));
                return new Vector(tmp.c, tmp.d, tmp.e);
            }
        }
        return new Vector(collisionVector.c, collisionVector.d, collisionVector.e);
    }

    @Override
    public boolean getOnGround(Entity entity) {
        net.minecraft.world.entity.Entity entityNMS = ((CraftEntity)entity).getHandle();
        return entityNMS.aA();
    }
}
