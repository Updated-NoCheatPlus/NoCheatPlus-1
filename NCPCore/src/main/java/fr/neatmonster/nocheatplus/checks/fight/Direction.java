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

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace.ITraceEntry;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;

/**
 * The Direction check will find out if a player tried to interact with something that's not in their field of view.
 */
public class Direction extends Check {

    /**
     * Instantiates a new direction check.
     */
    public Direction() {
        super(CheckType.FIGHT_DIRECTION);
    }

    /**
     * "Classic" check.
     * 
     * @param player
     *            the player
     * @param damaged
     *            the damaged
     * @return true, if successful
     */
    public boolean check(final Player player, final Location loc, 
            final Entity damaged, final boolean damagedIsFake, final Location dLoc, 
            final FightData data, final FightConfig cc) {
        boolean cancel = false;

        final MCAccess mcAccess = this.mcAccess.getHandle();

        // Safeguard, if entity is complex, this check will fail due to giant and hard to define hitboxes.
        //        if (damaged instanceof EntityComplex || damaged instanceof EntityComplexPart)
        if (!damagedIsFake && mcAccess.isComplexPart(damaged)) {
            return false;
        }

        // Find out how wide the entity is.
        final double width = damagedIsFake ? 0.6 : mcAccess.getWidth(damaged);

        // entity.height is broken and will always be 0, therefore. Calculate height instead based on boundingBox.
        final double height = damagedIsFake ? (damaged instanceof LivingEntity ? ((LivingEntity) damaged).getEyeHeight() : 1.75) : mcAccess.getHeight(damaged);
        

        // TODO: allow any hit on the y axis (might just adapt interface to use foot position + height)!

        // How far "off" is the player with their aim. We calculate from the players eye location and view direction to
        // the center of the target entity. If the line of sight is more too far off, "off" will be bigger than 0.
        final Vector direction = loc.getDirection();

        final double off;
		off = CollisionUtil.directionCheck(loc, player.getEyeHeight(), direction, dLoc.getX(), dLoc.getY() + height / 2D, dLoc.getZ(), width, height, TrigUtil.DIRECTION_PRECISION);
        /*if (cc.directionStrict){
            off = CollisionUtil.combinedDirectionCheck(loc, player.getEyeHeight(), direction, dLoc.getX(), dLoc.getY() + height / 2D, dLoc.getZ(), width, height, TrigUtil.DIRECTION_PRECISION, 80.0, isPlayer);
        }
        else{
            // Also take into account the angle.
            off = CollisionUtil.directionCheck(loc, player.getEyeHeight(), direction, dLoc.getX(), dLoc.getY() + height / 2D, dLoc.getZ(), width, height, TrigUtil.DIRECTION_PRECISION);
        } */

        if (off > 0.1) {
            // Player failed the check. Let's try to guess how far they were from looking directly to the entity...
            final Vector blockEyes = new Vector(dLoc.getX() - loc.getX(),  dLoc.getY() + height / 2D - loc.getY() - player.getEyeHeight(), dLoc.getZ() - loc.getZ());
            final double distance = blockEyes.crossProduct(direction).length() / direction.length();

            // Add the overall violation level of the check.
            data.directionVL += distance;
            // Execute whatever actions are associated with this check and the violation level and find out if we should
            // cancel the event.
            cancel = executeActions(player, data.directionVL, distance, cc.directionActions).willCancel();

            if (cancel) {
                // Deal an attack penalty time.
                data.attackPenalty.applyPenalty(cc.directionPenalty);
            }
        } else {
            // Reward the player by lowering their violation level.
            data.directionVL *= 0.8D;
        }

        return cancel;
    }

    /**
     * Data context for iterating over TraceEntry instances.
     * @param player
     * @param loc
     * @param damaged
     * @param damagedLoc
     * @param data
     * @param cc
     * @return
     */
    public DirectionContext getContext(final Player player, final Location loc, 
            final Entity damaged, final boolean damagedIsFake, final Location damagedLoc, 
            final FightData data, final FightConfig cc) {
        final DirectionContext context = new DirectionContext();
        // Find out how wide the entity is.
        if (damagedIsFake) {
            // Assume player / default.
            context.damagedComplex = false; // Later prefer bukkit based provider.
        }
        else {
            final MCAccess mcAccess = this.mcAccess.getHandle();
            context.damagedComplex = mcAccess.isComplexPart(damaged);
        }
        context.direction = loc.getDirection();
        context.lengthDirection = context.direction.length();
        return context;
    }

    /**
     * Check if the player fails the direction check, no change of FightData.
     * @param player
     * @param loc
     * @param damaged
     * @param dLoc
     * @param context
     * @param data
     * @param cc
     * @return
     */
    public boolean loopCheck(final Player player, final Location loc, 
            final Entity damaged, final ITraceEntry dLoc, 
            final DirectionContext context, 
            final FightData data, final FightConfig cc) {

        // Ignore complex entities for the moment.
        if (context.damagedComplex) {
            // TODO: Revise :p
            return false;
        }
        boolean cancel = false;
        boolean isPlayer = damaged instanceof Player;

        // TODO: allow any hit on the y axis (might just adapt interface to use foot position + height)!

        // How far "off" is the player with their aim. We calculate from the players eye location and view direction to
        // the center of the target entity. If the line of sight is more too far off, "off" will be bigger than 0.
        final double damagedBoxMarginHorizontal = dLoc.getBoxMarginHorizontal();
        final double damagedBoxMarginVertical = dLoc.getBoxMarginVertical();
        final double off;
        if (cc.directionStrict){
            off = CollisionUtil.combinedDirectionCheck(loc, player.getEyeHeight(), context.direction, dLoc.getX(), dLoc.getY() + damagedBoxMarginVertical / 2D, dLoc.getZ(), damagedBoxMarginHorizontal * 2.0, damagedBoxMarginVertical, cc.directionloopprecision, cc.directionangleprecision, isPlayer);
        }
        else {
            // Also take into account the angle.
            off = CollisionUtil.directionCheck(loc, player.getEyeHeight(), 
                    context.direction, dLoc.getX(), 
                    dLoc.getY() + damagedBoxMarginVertical / 2D, dLoc.getZ(), 
                    damagedBoxMarginHorizontal * 2.0, damagedBoxMarginVertical, 
                    cc.directionloopprecision);
        }

        if (off > 0.0) {
            if (dLoc.isInside(loc.getX(), loc.getY() + player.getEyeHeight(), loc.getZ())) { // Inside box.
                context.minResult = 0.0;
            }
            else {
                if (off > 0.11) {
                    // Player failed the check. Let's try to guess how far they were from looking directly to the entity...
                    final Vector blockEyes = new Vector(dLoc.getX() - loc.getX(),  dLoc.getY() + damagedBoxMarginVertical / 2D - loc.getY() - player.getEyeHeight(), dLoc.getZ() - loc.getZ());
                    final double distance = blockEyes.crossProduct(context.direction).length() / context.lengthDirection;
                    context.minViolation = Math.min(context.minViolation, distance);
                    cancel = true;
                }
                context.minResult = Math.min(context.minResult, off);
            }
        } 
        else if (cc.directionFailAll) {
            context.minResult = 0.0;
        }

        return cancel;
    }

    /**
     * Apply changes to FightData according to check results (context), trigger violations.
     * @param player
     * @param loc
     * @param damaged
     * @param context
     * @param forceViolation
     * @param data
     * @param cc
     * @return
     */
    public boolean loopFinish(final Player player, final Location loc, final Entity damaged, final DirectionContext context, final boolean forceViolation, final FightData data, final FightConfig cc) {
        boolean cancel = false;
        final double off = forceViolation && context.minViolation != Double.MAX_VALUE ? context.minViolation : context.minResult;
        if (off == Double.MAX_VALUE) {
            return false;
        }
        else if (off > 0.1) {
            // Add the overall violation level of the check.
            data.directionVL += off;

            // Execute whatever actions are associated with this check and the violation level and find out if we should
            // cancel the event.
            cancel = executeActions(player, data.directionVL, off, cc.directionActions).willCancel();

            if (cancel) {
                // Deal an attack penalty time.
                data.attackPenalty.applyPenalty(cc.directionPenalty);
            }
        }
        else {
            // Reward the player by lowering their violation level.
            data.directionVL *= 0.8D;
        }
        return cancel;
    }
}