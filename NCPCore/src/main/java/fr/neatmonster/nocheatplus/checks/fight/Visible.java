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
package fr.neatmonster.nocheatplus.checks.fight;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.moving.util.MovingUtil;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.collision.InteractAxisTracing;
import fr.neatmonster.nocheatplus.utilities.ds.map.BlockCoord;
import fr.neatmonster.nocheatplus.utilities.location.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.WrapBlockCache;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 *
 * @author xaw3ep
 */
public class Visible extends Check{
    private final InteractAxisTracing rayTracing = new InteractAxisTracing();
    private final WrapBlockCache wrapBlockCache;
    
    public Visible() {
        super(CheckType.FIGHT_VISIBLE);
        wrapBlockCache = new WrapBlockCache();
        rayTracing.setMaxSteps(30);
    }

    public boolean check(final Player player, final Location loc, 
            final Entity damaged, final boolean damagedIsFake, final Location dLoc, 
            final FightData data, final FightConfig cc) {
        boolean cancel = false;

        final MCAccess mcAccess = this.mcAccess.getHandle();

        if (!damagedIsFake && mcAccess.isComplexPart(damaged)) {
            return cancel;
        }
        
        // Find out how wide the entity is.
        final double width = damagedIsFake ? 0.6 : mcAccess.getWidth(damaged);

        // Find out how high the entity is.
        final double height = damagedIsFake ? (damaged instanceof LivingEntity ? ((LivingEntity) damaged).getEyeHeight() : 1.75) : mcAccess.getHeight(damaged);
        final double dxz = Math.round(width * 500.0) / 1000.0; // this.width / 2; // 0.3;
        final double dminX = dLoc.getX() - dxz;
        final double dminY = dLoc.getY();
        final double dminZ = dLoc.getZ() - dxz;
        final double dmaxX = dLoc.getX() + dxz;
        final double dmaxY = dLoc.getY() + height;
        final double dmaxZ = dLoc.getZ() + dxz;
        final int dBX = Location.locToBlock(dLoc.getX());
        final int dBY = Location.locToBlock(dLoc.getY());
        final int dBZ = Location.locToBlock(dLoc.getZ());
        
        final double eyeX = loc.getX();
        final double eyeY = loc.getY() + MovingUtil.getEyeHeight(player);
        final double eyeZ = loc.getZ();
        
        final BlockCoord sCollidingBox = new BlockCoord(Location.locToBlock(dminX), Location.locToBlock(dminY), Location.locToBlock(dminZ));
        final BlockCoord eCollidingBox = new BlockCoord(Location.locToBlock(dmaxX), Location.locToBlock(dmaxY), Location.locToBlock(dmaxZ));
        if (CollisionUtil.isInsideAABBIncludeEdges(eyeX, eyeY, eyeZ, dminX, dminY, dminZ, dmaxX, dmaxY, dmaxZ)) return cancel;
        
        final BlockCache blockCache = this.wrapBlockCache.getBlockCache();
        blockCache.setAccess(loc.getWorld());
        rayTracing.setBlockCache(blockCache);
        rayTracing.set(dLoc.getX(), dLoc.getY(), dLoc.getZ(), eyeX, eyeY, eyeZ);
        rayTracing.loop();
        //System.out.println("origin: " + eyeX + " " + eyeY+ " " + eyeZ + " | " + dLoc.getX() + " " + dLoc.getY() + " " + dLoc.getZ());
        if (rayTracing.collides()) {
            cancel = true;
            BlockCoord bc = new BlockCoord(dBX, dBY, dBZ);
            Vector direction = new Vector(eyeX - dBX, eyeY - dBY, eyeZ - dBZ).normalize();
            boolean canContinue;
            //System.out.println("dir:" + direction);
            Set<BlockCoord> visited = new HashSet<>();
            do {
                //System.out.println("dirl:" + direction);
                canContinue = false;
                for (BlockCoord neighbor : CollisionUtil.getNeighborsInDirection(bc, direction, eyeX, eyeY, eyeZ)) {
                    //System.out.println(CollisionUtil.canPassThrough(rayTracing, blockCache, bc, neighbor.getX(), neighbor.getY(), neighbor.getZ(), direction, eyeX, eyeY, eyeZ, MovingUtil.getEyeHeight(player), sCollidingBox, eCollidingBox) + " " + CollisionUtil.correctDir(neighbor.getY(), dBY, Location.locToBlock(eyeY), sCollidingBox.getY(), eCollidingBox.getY()) + " " + !visited.contains(neighbor)
                    //        + " " + blockCache.getType(neighbor.getX(), neighbor.getY(), neighbor.getZ()) + " " + neighbor.getX() + " " + neighbor.getY() + " " + neighbor.getZ()
                    //        + " " + blockCache.getType(bc.getX(), bc.getY(), bc.getZ()) + " " + bc.getX() + " " + bc.getY() + " " + bc.getZ());
                    if (CollisionUtil.canPassThrough(rayTracing, blockCache, bc, neighbor.getX(), neighbor.getY(), neighbor.getZ(), direction, eyeX, eyeY, eyeZ, MovingUtil.getEyeHeight(player), sCollidingBox, eCollidingBox) && CollisionUtil.correctDir(neighbor.getY(), dBY, Location.locToBlock(eyeY), sCollidingBox.getY(), eCollidingBox.getY()) && !visited.contains(neighbor)) {
                        if (TrigUtil.isSameBlock(neighbor.getX(), neighbor.getY(), neighbor.getZ(), eyeX, eyeY, eyeZ)) {
                            cancel = false;
                            break;
                        }
                        visited.add(neighbor);
                        rayTracing.set(neighbor.getX(), neighbor.getY(), neighbor.getZ(), eyeX, eyeY, eyeZ);
                        rayTracing.loop();
                        canContinue = true;
                        cancel = rayTracing.collides();
                        //if (!collides) break;
                        //bc = new BlockCoord(rayTracing.getBlockX(), rayTracing.getBlockY(), rayTracing.getBlockZ());
                        //direction = new Vector(eyeX - rayTracing.getBlockX(), eyeY - rayTracing.getBlockY(), eyeZ - rayTracing.getBlockZ()).normalize();
                        bc = new BlockCoord(neighbor.getX(), neighbor.getY(), neighbor.getZ());
                        direction = new Vector(eyeX - neighbor.getX(), eyeY - neighbor.getY(), eyeZ - neighbor.getZ()).normalize();
                        break;
                    }
                }
            } while (cancel && canContinue);
        }
        if (rayTracing.getStepsDone() > rayTracing.getMaxSteps()) {
            cancel = true;
        }
        if (cancel) {
            data.visibleVL += 1.0;
            final ViolationData vd = new ViolationData(this, player, data.visibleVL, 1.0, cc.visibleActions);
            //if (vd.needsParameters()) vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
            cancel = executeActions(vd).willCancel();
        }
        rayTracing.cleanup();
        blockCache.cleanup();
        return cancel;
    }
}
