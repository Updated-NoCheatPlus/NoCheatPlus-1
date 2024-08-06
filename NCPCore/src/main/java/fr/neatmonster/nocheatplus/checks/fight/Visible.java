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
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.utilities.collision.Axis;
import fr.neatmonster.nocheatplus.utilities.collision.Axis.RichAxisData;
import fr.neatmonster.nocheatplus.utilities.collision.AxisAlignedBBUtils;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.collision.ray.InteractAxisTracing;
import fr.neatmonster.nocheatplus.utilities.ds.map.BlockCoord;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.WrapBlockCache;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;

/**
 * A check to verify that hits are legit in terms of target visibility (look-agnostic)
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
    
    /**
     * Perform a visibility check to determine whether the player can see the target entity, considering potential obstructions and ensuring the check is not dependent on the direction the player is looking. 
     * It uses a ray tracing mechanism combined with neighbor block analysis to ascertain visibility.
     * It involves the following steps:
     *
     * <ul>
     *   <li><b>Initialization and inside-AABB check:</b> Initializes ray tracing and computes the bounding box of the target entity. Checks if the player's eye position is inside the entity's bounding box, which would imply immediate visibility.</li>
     *   <li><b>Neighbor processing loop:</b> If the initial ray tracing detects a collision, the method iterates through neighboring blocks:
     *     <ul>
     *       <li>Computes neighboring blocks based on the player's eye position and the direction to the target entity.</li>
     *       <li>Checks if the ray can pass through each neighboring block and if the block is in the correct vertical alignment, relative to the player’s eye height.</li>
     *       <li>Maintains a set of visited blocks to avoid redundant processing.</li>
     *       <li>Finally, it updates the ray tracing to continue from the new block, provided it is a valid candidate.</li>
     *     </ul>
     *   </li>
     *   
     * @param player The attacking player.
     * @param loc The location of the player's eye position.
     * @param damaged The entity being attacked.
     * @param damagedIsFake Indicates whether the damaged entity is a fake entity.
     * @param dLoc The location of the damaged entity.
     * @param data Fight data.
     * @param cc Fight config.
     * @return True if the hit is considered invalid due to visibility obstructions, false otherwise.
     */
    public boolean check(final Player player, final Location loc, 
                         final Entity damaged, final boolean damagedIsFake, final Location dLoc, 
                         final FightData data, final FightConfig cc) {
        boolean cancel = false;
        final MCAccess mcAccess = this.mcAccess.getHandle();
        if (!damagedIsFake && mcAccess.isComplexPart(damaged)) {
            // Enderdragon... Too complex to define, skip.
            return cancel;
        }
        
        // Determine the entity's bounding box dimensions
        final double[] AABB = AxisAlignedBBUtils.createAABBAtHorizontalResolution(damaged, dLoc);

        // Determine the player's eye position
        final double eyeX = loc.getX();
        final double eyeY = loc.getY() + MovingUtil.getEyeHeight(player);
        final double eyeZ = loc.getZ();

        // Calculate the block coordinates for the bounding box
        final int dBX = Location.locToBlock(dLoc.getX());
        final int dBY = Location.locToBlock(dLoc.getY());
        final int dBZ = Location.locToBlock(dLoc.getZ());

        // Calculate the start and end coordinates for collision detection
        final BlockCoord sCollidingBox = new BlockCoord(AABB[0], AABB[1], AABB[2]);
        final BlockCoord eCollidingBox = new BlockCoord(AABB[3], AABB[4], AABB[5]);

        // Check if the player is inside the damaged entity's bounding box
        if (CollisionUtil.isInsideAABBIncludeEdges(eyeX, eyeY, eyeZ, AABB[0], AABB[1], AABB[2], AABB[3], AABB[4], AABB[5])) {
            // Player is inside the damaged entity, return
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
