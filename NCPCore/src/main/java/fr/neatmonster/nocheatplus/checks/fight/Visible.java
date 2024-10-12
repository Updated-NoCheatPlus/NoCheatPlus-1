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
import fr.neatmonster.nocheatplus.utilities.map.MapUtil;
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
        final double[] AABB = AxisAlignedBBUtils.createAABBAtWidthResolution(damaged, dLoc);

        // Determine the attacker's eye position
        final double eyeX = loc.getX();
        final double eyeY = loc.getY() + MovingUtil.getEyeHeight(player);
        final double eyeZ = loc.getZ();

        // Convert the Location of the damaged entity to block coordinates.
        final int damagedToBlockX = Location.locToBlock(dLoc.getX());
        final int damagedToBlockY = Location.locToBlock(dLoc.getY());
        final int damagedToBlockZ = Location.locToBlock(dLoc.getZ());

        // Calculate the start and end coordinates for collision detection
        final BlockCoord sCollidingBox = new BlockCoord(AABB[0], AABB[1], AABB[2]);
        final BlockCoord eCollidingBox = new BlockCoord(AABB[3], AABB[4], AABB[5]);

        // Check if the player is inside the damaged entity's bounding box
        if (AxisAlignedBBUtils.isInsideAABBIncludeEdges(eyeX, eyeY, eyeZ, AABB)) {
            // Player is inside the damaged entity, return
            return cancel;
        }
        
        final BlockCache blockCache = this.wrapBlockCache.getBlockCache();
        blockCache.setAccess(loc.getWorld());
        rayTracing.setBlockCache(blockCache);
        // Perform a first ray trace check between the location of the damaged player and attacker's eye position.
        rayTracing.set(dLoc.getX(), dLoc.getY(), dLoc.getZ(), eyeX, eyeY, eyeZ);
        rayTracing.loop(); // Run it once.
        // Did the ray collide with something in between at all?
        if (rayTracing.collides()) {
            // An initial collision between attacker and target was detected. Check if the player might still have a legitimate line of sight to the entity
            cancel = true;
            // Block from which neighbours are determined.
            BlockCoord sourceBlock = new BlockCoord(damagedToBlockX, damagedToBlockY, damagedToBlockZ);
            // The direction is initially determined from attacker's eye to the block location of the target.
            Vector direction = new Vector(eyeX - damagedToBlockX, eyeY - damagedToBlockY, eyeZ - damagedToBlockZ).normalize();
            // Indicates that the process should continue because a possible alternative path for the ray to pass through has been found.
            boolean alternativePathExists;
            // To avoid redundant processing...
            Set<BlockCoord> visited = new HashSet<>();
            RichAxisData axisData = new RichAxisData(Axis.NONE, Direction.NONE);
            do {
                alternativePathExists = false;
                // Gather all neighbouring blocks that can be interacted with, starting from the origin block and the given direction.
                for (BlockCoord neighbor : MapUtil.getNeighborsInDirection(sourceBlock, direction, eyeX, eyeY, eyeZ, axisData)) {
                    // Can the ray pass through from source block to this neighbour in this direction?
                    if (CollisionUtil.canPassThrough(rayTracing, blockCache, sourceBlock, neighbor.getX(), neighbor.getY(), neighbor.getZ(), direction, eyeX, eyeY, eyeZ, MovingUtil.getEyeHeight(player), sCollidingBox, eCollidingBox, false, axisData) 
                        && CollisionUtil.correctDir(neighbor.getY(), damagedToBlockY, Location.locToBlock(eyeY), sCollidingBox.getY(), eCollidingBox.getY()) 
                        && !visited.contains(neighbor)) {
                        // It can.
                        if (TrigUtil.isSameBlock(neighbor.getX(), neighbor.getY(), neighbor.getZ(), eyeX, eyeY, eyeZ)) {
                            // Attacker is interacting with the block their head is in, allow the hit.
                            cancel = false;
                            break;
                        }
                        // This block has been processed, add it to the list so that we don't check it again.
                        visited.add(neighbor);
                        // Then, update the ray-trace from the attacker's eye to this neighbour.
                        rayTracing.set(neighbor.getX(), neighbor.getY(), neighbor.getZ(), eyeX, eyeY, eyeZ);
                        rayTracing.loop(); // Run the check once
                        // Signal that, although there was a collision with the direct line of sight of the player, an alternative path leading to a visible target exist.
                        alternativePathExists = true;
                        // If false, it means that this path allows the player to see the attacked entity, so no further looping is needed.
                        // If true, this updated path would lead to a collision, and the loop is allowed to continue to inspect further paths.
                        cancel = rayTracing.collides();
                        // Update the source block and the direction.
                        sourceBlock = new BlockCoord(neighbor.getX(), neighbor.getY(), neighbor.getZ());
                        // The direction is then updated from the attacker's eye to this neighbour block
                        direction = new Vector(eyeX - neighbor.getX(), eyeY - neighbor.getY(), eyeZ - neighbor.getZ()).normalize();
                        break;
                    }
                }
            } 
            // (Initial ray did not collide, the entity was fully visible then)
            /*
             * As long as a collision is detected (cancel = true) and there's a valid line of sight/path (alternativePathExists = true), the loop is allowed to continue, until all paths/line of sights options are exhausted.
             */
            while (cancel && alternativePathExists);
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
