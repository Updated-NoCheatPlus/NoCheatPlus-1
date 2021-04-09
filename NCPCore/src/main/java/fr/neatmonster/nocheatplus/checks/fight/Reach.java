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


import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.moving.location.tracking.LocationTrace.ITraceEntry;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.location.TrigUtil;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.ViolationData;



/**
 * The Reach check will find out if a player interacts with something that's too far away.
 */
public class Reach extends Check {


    /** The maximum distance allowed to interact with an entity in creative mode. */
    public static final double CREATIVE_DISTANCE = 6D;


    /** Additum for distance, based on entity's hitbox. */
    private static double getDistMod(final Entity damaged) {
        if (damaged instanceof EnderDragon)
            return 6.5D;
        else if (damaged instanceof Giant)
            return 1.5D;
        else if (damaged instanceof Ghast)
            return 4.0D;
        else if (damaged instanceof EnderCrystal)
            return 2.0D;
        else if (damaged instanceof Slime) //Accounts for large and small slimes.
            return 2.08D;
        else return 0;
    }


    /**
     * Instantiates a new reach check.
     */
    public Reach() {
        super(CheckType.FIGHT_REACH);
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
    public boolean check(final Player player, final Location pLoc, 
                         final Entity damaged, final boolean damagedIsFake, final Location dRef, 
                         final FightData data, final FightConfig cc, final IPlayerData pData) {

        boolean cancel = false;
        // The maximum distance allowed to interact with an entity in survival mode.
        final double SURVIVAL_DISTANCE = cc.reachSurvivalDistance; 
        // Amount which can be reduced by reach adaption.
        final double DYNAMIC_RANGE = cc.reachReduceDistance; 
        // Adaption amount for dynamic range.
        final double DYNAMIC_STEP = cc.reachReduceStep / SURVIVAL_DISTANCE; 
        final double distanceLimit = player.getGameMode() == GameMode.CREATIVE ? CREATIVE_DISTANCE : SURVIVAL_DISTANCE + getDistMod(damaged);
        final double distanceMin = (distanceLimit - DYNAMIC_RANGE) / distanceLimit;
        final double height = damagedIsFake ? (damaged instanceof LivingEntity ? ((LivingEntity) damaged).getEyeHeight() : 1.75) : mcAccess.getHandle().getHeight(damaged);
        final double width = damagedIsFake ? 0.6 : mcAccess.getHandle().getWidth(damaged);

        double centertoedge = 0.0;
        if (cc.reachPrecision) centertoedge = getinset(pLoc, dRef, width / 2, 0.0);

        // Refine y position.
        // TODO: Make a little more accurate by counting in the actual bounding box.
        final double pY = pLoc.getY() + player.getEyeHeight();
        final double dY = dRef.getY();
        if (pY <= dY); // Keep the foot level y.
        else if (pY >= dY + height) dRef.setY(dY + height); // Highest ref y.
        else dRef.setY(pY); // Level with damaged.

        final Vector pRel = dRef.toVector().subtract(pLoc.toVector().setY(pY)); // TODO: Run calculations on numbers only :p.
        // Distance is calculated from eye location to center of targeted. If the player is further away from their target
        // than allowed, the difference will be assigned to "distance".
        final double lenpRel = pRel.length() - centertoedge;
        double violation = lenpRel - distanceLimit;
        final double reachMod = data.reachMod; 


        if (violation > 0) {
            if (TickTask.getLag(1000, true) < 1.5f){ // Do not increase the vl in case of server lag (1.5 is a magic value)
                data.reachVL += violation;
                final ViolationData vd = new ViolationData(this, player, data.reachVL, violation, cc.reachActions);
                vd.setParameter(ParameterName.REACH_DISTANCE, StringUtil.fdec3.format(lenpRel));
                // Execute whatever actions are associated with this check and the violation level and find out if we should
                // cancel the event.
                cancel = executeActions(vd).willCancel();
            }

            if (Improbable.check(player, (float) violation / 2f, System.currentTimeMillis(), "fight.reach", pData)){
                cancel = true;
            }

            if (cancel && cc.reachPenalty > 0){
                // Apply an attack penalty time.
                data.attackPenalty.applyPenalty(cc.reachPenalty);
            }
        }
        else if (lenpRel - distanceLimit * reachMod > 0){
            // Silent cancel.
            if (cc.reachPenalty > 0) {
                data.attackPenalty.applyPenalty(cc.reachPenalty / 2);
            }
            cancel = true;
            Improbable.feed(player, (float) (lenpRel - distanceLimit * reachMod) / 4f, System.currentTimeMillis());
        }
        else data.reachVL *= 0.8D; // Player passed the check, reward them.
            

        if (!cc.reachReduce){
            data.reachMod = 1d;
        }
        else if (lenpRel > distanceLimit - DYNAMIC_RANGE){
            data.reachMod = Math.max(distanceMin, data.reachMod - DYNAMIC_STEP);
        }
        else { 
            data.reachMod = Math.min(1.0, data.reachMod + DYNAMIC_STEP);
        }

        if (pData.isDebugActive(type) && pData.hasPermission(Permissions.ADMINISTRATION_DEBUG, player)){
            player.sendMessage("NC+: Attack/reach " + damaged.getType()+ " height="+ StringUtil.fdec3.format(height) + " dist=" + StringUtil.fdec3.format(lenpRel) +" @" + StringUtil.fdec3.format(reachMod));
        }

        return cancel;
    }

    /**
     * Data context for iterating over ITraceEntry instances.
     * @param player
     * @param pLoc
     * @param damaged
     * @param damagedLoc
     * @param data
     * @param cc
     * @return
     */
    public ReachContext getContext(final Player player, final Location pLoc, 
                                   final Entity damaged, final Location damagedLoc, 
                                   final FightData data, final FightConfig cc) {

        final ReachContext context = new ReachContext();
        context.distanceLimit = player.getGameMode() == GameMode.CREATIVE ? CREATIVE_DISTANCE : cc.reachSurvivalDistance + getDistMod(damaged);
        context.distanceMin = (context.distanceLimit - cc.reachReduceDistance) / context.distanceLimit;
        //context.eyeHeight = player.getEyeHeight();
        context.pY = pLoc.getY() + player.getEyeHeight();
        return context;
    }

    /**
     * Check if the player fails the reach check, no change of FightData.
     * @param player
     * @param pLoc
     * @param damaged
     * @param dRef
     * @param context
     * @param data
     * @param cc
     * @return
     */
    public boolean loopCheck(final Player player, final Location pLoc, final Entity damaged, 
                             final ITraceEntry dRef, final ReachContext context, 
                             final FightData data, final FightConfig cc) {

        boolean cancel = false;

        // Refine y position.
        final double dY = dRef.getY();
        double y = dRef.getY();

        if (context.pY <= dY) {
            // Keep the foot level y.
        }
        else if (context.pY >= dY + dRef.getBoxMarginVertical()) {
            y = dY + dRef.getBoxMarginVertical(); // Highest ref y.
        }
        else {
            y = context.pY; // Level with damaged.
        }

        double centertoedge = 0.0;
        if (cc.reachPrecision) centertoedge = getinset(pLoc, new Location(null, dRef.getX(), dRef.getY(), dRef.getZ()), dRef.getBoxMarginHorizontal(), y - context.pY);
        
        // Distance is calculated from eye location to center of targeted. If the player is further away from their target
        // than allowed, the difference will be assigned to "distance".
        // TODO: Run check on squared distances (quite easy to change to stored boundary-sq values).
        final double lenpRel = TrigUtil.distance(dRef.getX(), y, dRef.getZ(), pLoc.getX(), context.pY, pLoc.getZ()) - centertoedge;
        double violation = lenpRel - context.distanceLimit;

        if (violation > 0 || lenpRel - context.distanceLimit * data.reachMod > 0){
            // TODO: The silent cancel parts should be sen as "no violation" ?
            // Set minimum violation in context
            context.minViolation = Math.min(context.minViolation, lenpRel);
            cancel = true;
        }
        context.minResult = Math.min(context.minResult, lenpRel);

        return cancel;

    }

    /**
     * Apply changes to FightData according to check results (context), trigger violations.
     * @param player
     * @param pLoc
     * @param damaged
     * @param context
     * @param forceViolation
     * @param data
     * @param cc
     * @return
     */
    public boolean loopFinish(final Player player, final Location pLoc, final Entity damaged, 
                              final ReachContext context, final ITraceEntry traceEntry, final boolean forceViolation, 
                              final FightData data, final FightConfig cc, final IPlayerData pData) {

        final double lenpRel = forceViolation && context.minViolation != Double.MAX_VALUE ? context.minViolation : context.minResult;

        if (lenpRel == Double.MAX_VALUE) {
            return false;
        }

        double violation = lenpRel - context.distanceLimit;
        boolean cancel = false;

        if (violation > 0) {    
            if (TickTask.getLag(1000, true) < 1.5f){
                data.reachVL += violation;
                final ViolationData vd = new ViolationData(this, player, data.reachVL, violation, cc.reachActions);
                vd.setParameter(ParameterName.REACH_DISTANCE, StringUtil.fdec3.format(lenpRel));
                // Execute whatever actions are associated with this check and the violation level and find out if we should
                // cancel the event.
                cancel = executeActions(vd).willCancel();
            }
            
            //if (Improbable.check(player, (float) violation / 2f, System.currentTimeMillis(), 
            //        "fight.reach", pData)){
            //    cancel = true;
            //}
            // TODO: New improbable weight calculations so that weight is not inverse to config weight
            if (cc.reachImprobableWeight > 0.0f) {
                if (!cc.reachImprobableFeedOnly && Improbable.check(player, (float) violation / cc.reachImprobableWeight, System.currentTimeMillis(), "fight.reach", pData)) {
                    cancel = true;
                }
            }

            if (cancel && cc.reachPenalty > 0){
                // Apply an attack penalty time.
                data.attackPenalty.applyPenalty(cc.reachPenalty);
            }
        }
        else if (lenpRel - context.distanceLimit * data.reachMod > 0){
            // Silent cancel.
            if (cc.reachPenalty > 0) {
                data.attackPenalty.applyPenalty(cc.reachPenalty / 2);
            }

            cancel = true;

            if (cc.reachImprobableWeight > 0.0f) {
                Improbable.feed(player, (float) (lenpRel - context.distanceLimit * data.reachMod) / cc.reachImprobableWeight, System.currentTimeMillis());
            }
            // Improbable.feed(player, (float) (lenpRel - context.distanceLimit * data.reachMod) / 4f, System.currentTimeMillis());
        }
        else {
            // Player passed the check, reward them.
            data.reachVL *= 0.8D;

        }

        // Adaption amount for dynamic range.
        final double DYNAMIC_STEP = cc.reachReduceStep / cc.reachSurvivalDistance;

        if (!cc.reachReduce){
            data.reachMod = 1d;
        }
        else if (lenpRel > context.distanceLimit - cc.reachReduceDistance){
            data.reachMod = Math.max(context.distanceMin, data.reachMod - DYNAMIC_STEP);
        }
        else {
            data.reachMod = Math.min(1.0, data.reachMod + DYNAMIC_STEP);
        }

        if (pData.isDebugActive(type) && pData.hasPermission(Permissions.ADMINISTRATION_DEBUG, player)){
            // TODO: Height: remember successful ITraceEntry
            player.sendMessage("NC+: Attack/reach " + damaged.getType()+ (traceEntry == null ? "" : (" height=" + traceEntry.getBoxMarginVertical())) + " dist=" + StringUtil.fdec3.format(lenpRel) +" @" + StringUtil.fdec3.format(data.reachMod));
        }

        return cancel;
    }


    private boolean isSameXZ(final Location loc1, final Location loc2) {
        return loc1.getX() == loc2.getX() && loc1.getZ() == loc2.getZ();
    }


    /**
     *
     * @param pLoc
     *            the player location
     * @param dRef
     *            the target location
     * @param damagedBoxMarginHorizontal
     *            the target Width / 2
     * @param diffY the Y different
     * @return the double represent for the distance from target location to the edge of target hitbox
     */
    private double getinset(final Location pLoc, final Location dRef, final double damagedBoxMarginHorizontal, final double diffY) {

        if (!isSameXZ(pLoc, dRef)) {
            final Location dRefc = dRef.clone();
            final Vector vec1 = new Vector(pLoc.getX() - dRef.getX(), diffY , pLoc.getZ() - dRef.getZ());
            if (vec1.length() < damagedBoxMarginHorizontal * Math.sqrt(2)) return 0.0;
            if (vec1.getZ() > 0.0) {
                dRefc.setZ(dRefc.getZ() + damagedBoxMarginHorizontal);
            } 
            else if (vec1.getZ() < 0.0) {
                dRefc.setZ(dRefc.getZ() - damagedBoxMarginHorizontal);
            } 
            else if (vec1.getX() > 0.0) {
                dRefc.setX(dRefc.getX() + damagedBoxMarginHorizontal);
            } 
            else dRefc.setX(dRefc.getX() - damagedBoxMarginHorizontal);

            final Vector vec2 = new Vector(dRefc.getX() - dRef.getX(), 0.0 , dRefc.getZ() - dRef.getZ());
            double angle = TrigUtil.angle(vec1, vec2);
            // Require < 45deg, if not 90deg-angel
            if (angle > Math.PI / 4) angle = Math.PI / 2 - angle;
            if (angle >= 0.0 && angle <= Math.PI / 4) { // TODO: Dose this one necessary?
                return damagedBoxMarginHorizontal / Math.cos(angle);
            }
        }
        return 0.0;
    }
}
