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

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.utilities.collision.Axis;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil.RichAxisData;
import fr.neatmonster.nocheatplus.utilities.collision.InteractAxisTracing;
import fr.neatmonster.nocheatplus.utilities.ds.map.BlockCoord;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.WrapBlockCache;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;

/**
 * A check to verify that hits are legit in terms of target visibility (look-independent)
 * @author xaw3ep
 */
public class Visible extends Check {
    
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
        // Infer the bounding box of the damaged entity with the height and width parameters.
        final double boxMarginHorizontal = Math.round(width * 500.0) / 1000.0; // this.width / 2; // 0.3;
        final double minX = dLoc.getX() - boxMarginHorizontal;
        final double minY = dLoc.getY();
        final double minZ = dLoc.getZ() - boxMarginHorizontal;
        final double maxX = dLoc.getX() + boxMarginHorizontal;
        final double maxY = dLoc.getY() + height;
        final double maxZ = dLoc.getZ() + boxMarginHorizontal;
        // Damaged entity's position to block coordinates.
        final int dBX = Location.locToBlock(dLoc.getX());
        final int dBY = Location.locToBlock(dLoc.getY());
        final int dBZ = Location.locToBlock(dLoc.getZ());
        // Attacker's coordinates.
        final double eyeX = loc.getX();
        final double eyeY = loc.getY() + MovingUtil.getEyeHeight(player);
        final double eyeZ = loc.getZ();
        // Start of the collision box
        final BlockCoord sCollidingBox = new BlockCoord(Location.locToBlock(minX), Location.locToBlock(minY), Location.locToBlock(minZ));
        // End of the collision box
        final BlockCoord eCollidingBox = new BlockCoord(Location.locToBlock(maxX), Location.locToBlock(maxY), Location.locToBlock(maxZ));
        if (CollisionUtil.isInsideAABBIncludeEdges(eyeX, eyeY, eyeZ, minX, minY, minZ, maxX, maxY, maxZ)) {
            // Player is inside the damaged entity, return.
            return cancel;
        }
        
        final BlockCache blockCache = this.wrapBlockCache.getBlockCache();
        blockCache.setAccess(loc.getWorld());
        rayTracing.setBlockCache(blockCache);
        rayTracing.set(dLoc.getX(), dLoc.getY(), dLoc.getZ(), eyeX, eyeY, eyeZ);
        rayTracing.loop();
        if (rayTracing.collides()) {
            cancel = true;
            BlockCoord bc = new BlockCoord(dBX, dBY, dBZ);
            Vector direction = new Vector(eyeX - dBX, eyeY - dBY, eyeZ - dBZ).normalize();
            boolean canContinue;
            Set<BlockCoord> visited = new HashSet<>();
            RichAxisData axisData = new RichAxisData(Axis.NONE, Direction.NONE);
            do {
                canContinue = false;
                for (BlockCoord neighbor : CollisionUtil.getNeighborsInDirection(bc, direction, eyeX, eyeY, eyeZ, axisData)) {
                    if (CollisionUtil.canPassThrough(rayTracing, blockCache, bc, neighbor.getX(), neighbor.getY(), neighbor.getZ(), direction, eyeX, eyeY, eyeZ, MovingUtil.getEyeHeight(player), sCollidingBox, eCollidingBox, false, axisData) 
                        && CollisionUtil.correctDir(neighbor.getY(), dBY, Location.locToBlock(eyeY), sCollidingBox.getY(), eCollidingBox.getY()) 
                        && !visited.contains(neighbor)) {
                        if (TrigUtil.isSameBlock(neighbor.getX(), neighbor.getY(), neighbor.getZ(), eyeX, eyeY, eyeZ)) {
                            cancel = false;
                            break;
                        }
                        visited.add(neighbor);
                        rayTracing.set(neighbor.getX(), neighbor.getY(), neighbor.getZ(), eyeX, eyeY, eyeZ);
                        rayTracing.loop();
                        canContinue = true;
                        cancel = rayTracing.collides();
                        bc = new BlockCoord(neighbor.getX(), neighbor.getY(), neighbor.getZ());
                        direction = new Vector(eyeX - neighbor.getX(), eyeY - neighbor.getY(), eyeZ - neighbor.getZ()).normalize();
                        break;
                    }
                }
            } 
            while (cancel && canContinue);
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
