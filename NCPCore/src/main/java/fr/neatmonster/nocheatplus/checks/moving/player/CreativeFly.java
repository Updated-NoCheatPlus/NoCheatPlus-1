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
package fr.neatmonster.nocheatplus.checks.moving.player;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.envelope.PlayerEnvelopes;
import fr.neatmonster.nocheatplus.checks.moving.envelope.workaround.LostGround;
import fr.neatmonster.nocheatplus.checks.moving.model.LiftOffEnvelope;
import fr.neatmonster.nocheatplus.checks.moving.model.ModelFlying;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.velocity.AccountEntry;
import fr.neatmonster.nocheatplus.checks.moving.velocity.SimpleEntry;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeEnchant;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;


/**
 * A check designed for people that are exposed to particular effects, flying
 * or gliding. The complement to the "SurvivalFly" check, which is for ordinary gameplay.
 */
public class CreativeFly extends Check {

   /*
    * TODO: This check is a fucking mess. 
    * Move slowfalling and levitation to SurvivalFly once the hSpeed rework is finished, reason being:
    * 1) It only changes vertical movement not horizontal, which still uses the NMS method
    * 2) Prevents possible bypasses with Fly checks switches.
    * ^ Done.
    * Leave here: Spectator, Flying.

    */

    private final List<String> tags = new LinkedList<String>();
    private final BlockChangeTracker blockChangeTracker;
    private IGenericInstanceHandle<IAttributeAccess> attributeAccess = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IAttributeAccess.class);


   /**
    * Instantiates a new creative fly check.
    */
    public CreativeFly() {
        super(CheckType.MOVING_CREATIVEFLY);
        blockChangeTracker = NCPAPIProvider.getNoCheatPlusAPI().getBlockChangeTracker();
    }


   /**
    * Checks a player
    *
    * @param player
    * @param from
    * @param to
    * @param data
    * @param cc
    * @param time Milliseconds.
    * @return
    */
    public Location check(final Player player, final PlayerLocation from, final PlayerLocation to, 
                          final MovingData data, final MovingConfig cc, final IPlayerData pData,
                          final long time, final int tick,
                          final boolean useBlockChangeTracker) {

        // Reset tags, just in case.
        tags.clear();
        final boolean debug = pData.isDebugActive(type);
        final GameMode gameMode = player.getGameMode();
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final ModelFlying model = thisMove.modelFlying;
        final double yDistance = thisMove.yDistance;
        final double hDistance = thisMove.hDistance;
        final boolean flying = gameMode == BridgeMisc.GAME_MODE_SPECTATOR || player.isFlying();
        final boolean sprinting = player.isSprinting();
        final long now = System.currentTimeMillis();
        boolean lostGround = false;

        // Lost ground, if set so.
        if (model.getGround()) {
            MovingUtil.prepareFullCheck(from, to, thisMove, Math.max(cc.yOnGround, cc.noFallyOnGround));
            if (!thisMove.from.onGroundOrResetCond) {
                if (LostGround.lostGround(player, from, to, hDistance, yDistance, sprinting, lastMove, data, cc, useBlockChangeTracker ? blockChangeTracker : null, tags)) {
                    lostGround = true;
                }
            }
        }


        //////////////////////////
        // Horizontal move.
        //////////////////////////

        double[] resH = hDist(player, from, to, hDistance, yDistance, sprinting, flying, thisMove, lastMove, time, model, data, cc);
        double limitH = resH[0];
        double resultH = resH[1];

        // Check velocity.
        if (resultH > 0) {
            double hFreedom = data.getHorizontalFreedom();
            if (hFreedom < resultH) {
                // Use queued velocity if possible.
                hFreedom += data.useHorizontalVelocity(resultH - hFreedom);
            }
            if (hFreedom > 0.0) {
                resultH = Math.max(0.0, resultH - hFreedom);
                if (resultH <= 0.0) {
                    limitH = hDistance;
                }
                tags.add("hvel");
            }
        }
        else {
            data.clearActiveHorVel(); // TODO: test/check !
        }

        resultH *= 100.0; // Normalize to % of a block.
        if (resultH > 0.0) {
            tags.add("hdist");
        }






        //////////////////////////
        // Vertical move.
        //////////////////////////

        double limitV = 0.0; // Limit. For debug only, violation handle on resultV
        double resultV = 0.0; // Violation (normalized to 100 * 1 block, applies if > 0.0).

        // Distinguish checking method by y-direction of the move:
        // Ascend.
        if (yDistance > 0.0) {
            double[] res = vDistAscend(from, to, yDistance, flying, thisMove, lastMove, model, data, cc, debug);
            resultV = Math.max(resultV, res[1]);
            limitV = res[0];
        }
        // Descend.
        else if (yDistance < 0.0) {
            double[] res = vDistDescend(from, to, yDistance, flying, thisMove, lastMove, model, data, cc);
            resultV = Math.max(resultV, res[1]);
            limitV = res[0];
        }
        // Keep altitude.
        else {
            double[] res = vDistZero(from, to, yDistance, flying, thisMove, lastMove, model, data, cc);
            resultV = Math.max(resultV, res[1]);
            limitV = res[0];
        }

        // Velocity.
        if (resultV > 0.0 && (thisMove.verVelUsed != null || data.getOrUseVerticalVelocity(yDistance) != null)) {
            resultV = 0.0;
            tags.add("vvel");
        }

        // Add tag for maximum height check (silent set back).
        // TODO: Allow use velocity there (would need a flag to signal the actual check below)?
        final double maximumHeight = model.getMaxHeight() + player.getWorld().getMaxHeight();
        if (to.getY() > maximumHeight) {
            tags.add("maxheight");
        }

        resultV *= 100.0; // Normalize to % of a block.
        if (resultV > 0.0) {
            tags.add("vdist");
        }

        final double result = Math.max(0.0, resultH) + Math.max(0.0, resultV);



        //////////////////////////
        // Output debug
        //////////////////////////
        if (debug) {
            outpuDebugMove(player, hDistance, limitH, yDistance, limitV, model, tags, data);
        }



        ///////////////////////
        // Violation handling
        ///////////////////////

        Location setBack = null; // Might get altered below.

        if (result > 0.0) {
            data.creativeFlyVL += result;
            // Execute whatever actions are associated with this check and the violation level and find out if we
            // should cancel the event.
            final ViolationData vd = new ViolationData(this, player, data.creativeFlyVL, result, cc.creativeFlyActions);
            if (vd.needsParameters()) {
                vd.setParameter(ParameterName.LOCATION_FROM, String.format(Locale.US, "%.2f, %.2f, %.2f", from.getX(), from.getY(), from.getZ()));
                vd.setParameter(ParameterName.LOCATION_TO, String.format(Locale.US, "%.2f, %.2f, %.2f", to.getX(), to.getY(), to.getZ()));
                vd.setParameter(ParameterName.DISTANCE, String.format(Locale.US, "%.2f", TrigUtil.distance(from,  to)));
                if (model != null) {
                    vd.setParameter(ParameterName.MODEL, model.getId().toString());
                }
                if (!tags.isEmpty()) {
                    vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
                }
            }
            if (executeActions(vd).willCancel()) {
                // Compose a new location based on coordinates of "newTo" and viewing direction of "event.getTo()"
                // to allow the player to look somewhere else despite getting pulled back by NoCheatPlus.
                setBack = data.getSetBack(to); // (OK)
            }
        }
        else {
            // Maximum height check (silent set back).
            if (to.getY() > maximumHeight) {
                setBack = data.getSetBack(to); // (OK)
                if (debug) {
                    debug(player, "Maximum height exceeded, silent set-back.");
                }
            }
            if (setBack == null) {
                // Slowly reduce the violation level with each event.
                data.creativeFlyVL *= 0.97;
            }
        }

        // Return setBack, if set.
        if (setBack != null) {
            // Check for max height of the set back.
            if (setBack.getY() > maximumHeight) {
                // Correct the y position.
                setBack.setY(getCorrectedHeight(maximumHeight, setBack.getWorld()));
                if (debug) {
                    debug(player, "Maximum height exceeded by set back, correct to: " + setBack.getY());
                }
            }
            data.sfJumpPhase = 0;
            return setBack;
        }
        else {
            // Adjust the set back and other last distances.
            data.setSetBack(to);
            // Adjust jump phase.
            if (!thisMove.from.onGroundOrResetCond && !thisMove.to.onGroundOrResetCond) {
                data.sfJumpPhase ++;
            }
            else if (thisMove.touchedGround && !thisMove.to.onGroundOrResetCond) {
                data.sfJumpPhase = 1;
            }
            else {
                data.sfJumpPhase = 0;
            }
            return null;
        }
    }


    /**
     * Horizontal distance checking.
     * @param player
     * @param from
     * @param to
     * @param hDistance
     * @param yDistance
     * @param flying
     * @param lastMove
     * @param time
     * @param model
     * @param data
     * @param cc
     * @return limitH, resultH (not normalized).
     */
    private double[] hDist(final Player player, final PlayerLocation from, final PlayerLocation to, final double hDistance, 
                           final double yDistance, final boolean sprinting, final boolean flying, final PlayerMoveData thisMove, 
                           final PlayerMoveData lastMove, final long time, final ModelFlying model, final MovingData data, final MovingConfig cc) {

        // Modifiers.
        double fSpeed;

        // TODO: Make this configurable ! [Speed effect should not affect flying if not on ground.]
        if (model.getApplyModifiers()) {
            final double speedModifier = mcAccess.getHandle().getFasterMovementAmplifier(player);
            if (Double.isInfinite(speedModifier)) fSpeed = 1.0;
            else fSpeed = 1.0 + 0.2 * (speedModifier + 1.0);
    
            if (flying) {
                // TODO: Consider mechanics for flying backwards.
                fSpeed *= data.flySpeed / Magic.DEFAULT_FLYSPEED;
                if (sprinting) {
                    // TODO: Prevent for pre-1.8?
                    fSpeed *= model.getHorizontalModSprint();
                    tags.add("sprint");
                }
                tags.add("flying");
            }
            //  else {
            //     // (Ignore sprinting here).
            //      final double attrMod = attributeAccess.getHandle().getSpeedAttributeMultiplier(player);
            //      if (attrMod != Double.MAX_VALUE) fSpeed *= attrMod;
            //      fSpeed *= data.walkSpeed / Magic.CB_DEFAULT_WALKSPEED;
            //  }
        }
        else fSpeed = 1.0;
        
        // The horizontal limit is now set. 
        double limitH = model.getHorizontalModSpeed() / 100.0 * ModelFlying.HORIZONTAL_SPEED * fSpeed;
        
        // Do apply dolphinsgrace modifier
        // if (from.isInWater() || to.isInWater()) {
        //     if (!Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(player))) {
        //         limitH *= Magic.modDolphinsGrace;
        //         tags.add("hdolphinsgrace");
        //     }
        // }

        // Ordinary friction
        // TODO: Use last friction (as well)?
        // TODO: Test/adjust more.
        // it doesn't really make much sense checking for friction as well...
        if (lastMove.toIsValid) {
            double frictionDist = lastMove.hDistance * Magic.FRICTION_MEDIUM_AIR;
            limitH = Math.max(frictionDist, limitH);
            tags.add("hfrict");
        }

        // Finally, determine how far the player went beyond the set limits.
        double resultH = Math.max(0.0, hDistance - limitH);

        if (model.getApplyModifiers()) {
            data.jumpDelay--;
            if (!flying && resultH > 0 && resultH < 0.3) {
                // 0: yDistance envelope
                if (yDistance >= 0.0 &&
                    (
                        // 1: Normal jumping.
                        yDistance > 0.0 
                        && yDistance > LiftOffEnvelope.NORMAL.getJumpGain(data.jumpAmplifier) - Magic.GRAVITY_SPAN
                        // 1: Too short with head obstructed.
                        || thisMove.headObstructed || lastMove.toIsValid && lastMove.headObstructed && lastMove.yDistance <= 0.0
                        // 1: Hop without y distance increase at moderate h-speed.
                        // TODO: 2nd below: demand next move to jump. Relate to stored past moves. 
                        // TODO: Ensure the gain can only be used once per so and so.
                        //|| (cc.sfGroundHop || yDistance == 0.0 && !lastMove.touchedGroundWorkaround && !lastMove.from.onGround)
                        //&& limitH > 0.0 && hDistance / limitH < 1.5
                        //&& (hDistance / lastMove.hDistance < 1.35 
                        //        || hDistance / limitH < 1.35)
                    )
                    // 0: Ground + jump phase conditions.
                    && (data.sfJumpPhase <= 1 && (thisMove.touchedGroundWorkaround || 
                        lastMove.touchedGround && !lastMove.bunnyHop))
                    // 0: Don't allow bunny to run out of liquid.
                    && (!from.isResetCond() && !to.isResetCond()) // TODO: !to.isResetCond() should be reviewed.
                    ) {

                    tags.add("bunnyhop");
                    data.jumpDelay = 9;
                    thisMove.bunnyHop = true;
                    resultH = 0.0;
                }
                else if (data.jumpDelay <= 0) {
                    resultH = 0.0;
                    tags.add("bunnyhop");
                }
            }
        }
        return new double[] {limitH, resultH};
    }


   /**
     * Ascending (yDistance > 0.0) check.
     * @param from
     * @param to
     * @param yDistance
     * @param flying
     * @param lastMove
     * @param model
     * @param data
     * @param cc
     * @return limitV, resultV (not normalized).
     */
    private double[] vDistAscend(final PlayerLocation from, final PlayerLocation to, final double yDistance, 
                                 final boolean flying, final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
                                 final ModelFlying model, final MovingData data, final MovingConfig cc, final boolean debug) {

        // Set the vertical limit.
        double limitV = model.getVerticalAscendModSpeed() / 100.0 * ModelFlying.VERTICAL_ASCEND_SPEED; 
        double resultV = 0.0;
        
        // Let fly speed apply with moving upwards.
        if (model.getApplyModifiers() && flying && yDistance > 0.0) {
            limitV *= data.flySpeed / Magic.DEFAULT_FLYSPEED;
        }
        
        // Friction with gravity.
        if (model.getGravity()) {
            if (yDistance > limitV && lastMove.toIsValid) { 
                // (Disregard gravity.)
                // TODO: Use last friction (as well)?
                double frictionDist = lastMove.yDistance * Magic.FRICTION_MEDIUM_AIR;
                if (!flying) {
                    frictionDist -= 0.019;
                }
                if (frictionDist > limitV) {
                    limitV = frictionDist;
                    tags.add("vfrict_g");
                }
            }
        }

        if (model.getGround()) {
            // Jump lift off gain.
            // NOTE: This assumes SurvivalFly busies about moves with from.onGroundOrResetCond.
            if (yDistance > limitV && !thisMove.to.onGroundOrResetCond && !thisMove.from.onGroundOrResetCond && (
                // Last move touched ground.
                lastMove.toIsValid && lastMove.touchedGround && 
                (lastMove.yDistance <= 0.0 || lastMove.to.extraPropertiesValid && lastMove.to.onGround)
                // This move touched ground by a workaround.
                || thisMove.touchedGroundWorkaround
                )) {
                // Allow normal jumping.
                final double maxGain = LiftOffEnvelope.NORMAL.getJumpGain(data.jumpAmplifier);
                if (maxGain > limitV) {
                    limitV = maxGain;
                    tags.add("jump_gain");
                }
            }
        }

        // Ordinary step up.
        // TODO: Might be within a 'if (model.ground)' block?
        // TODO: sfStepHeight should be a common modeling parameter?
        if (yDistance > limitV && yDistance <= cc.sfStepHeight 
            && (lastMove.toIsValid && lastMove.yDistance < 0.0 || from.isOnGroundOrResetCond() || thisMove.touchedGroundWorkaround)
            && to.isOnGround()) {
            // (Jump effect not checked yet.)
            limitV = cc.sfStepHeight;
            tags.add("step_up");
        }

        // Determine violation amount.
        resultV = Math.max(0.0, yDistance - limitV);
        // Post-violation recovery.
        return new double[] {limitV, resultV};
    }


    /**
     * Descending phase vDist check
     * @param from
     * @param to
     * @param yDistance
     * @param flying
     * @param thisMove
     * @param lastMove
     * @param model
     * @param data
     * @param cc
     * @return limitV, resultV
     */
    private double[] vDistDescend(final PlayerLocation from, final PlayerLocation to, final double yDistance, final boolean flying, 
                                  final PlayerMoveData thisMove, final PlayerMoveData lastMove, final ModelFlying model, 
                                  final MovingData data, final MovingConfig cc) {
        double limitV = 0.0;
        double resultV = 0.0;
        // Note that 'extreme moves' are covered by the extreme move check.
        // TODO: if gravity: friction + gravity.
        // TODO: ordinary flying (flying: enforce maximum speed at least)
        return new double[] {limitV, resultV};
    }


    /**
     * Keep the altitude
     * @param from
     * @param to
     * @param yDistance
     * @param flying
     * @param thisMove
     * @param lastMove
     * @param model
     * @param data
     * @param cc
     * @return limitV, resultV
     */
    private double[] vDistZero(final PlayerLocation from, final PlayerLocation to, final double yDistance, final boolean flying, 
                               final PlayerMoveData thisMove, final PlayerMoveData lastMove, final ModelFlying model, 
                               final MovingData data, final MovingConfig cc) {
        double limitV = 0.0;
        double resultV = 0.0;
        // TODO: Deny on enforcing mingain.
        return new double[] {limitV, resultV};
    }


   /**
    * 
    * @param maximumHeight
    * @param world
    * @return 
    */
    private double getCorrectedHeight(final double maximumHeight, final World world) {
        return Math.max(maximumHeight - 10.0, world.getMaxHeight());
    }


   /**
    * Output debug
    * @param player
    * @param hDistance
    * @param limitH
    * @param yDistance
    * @param limitV
    * @param model
    * @param tags
    * @param data
    * @return
    */
    private void outpuDebugMove(final Player player, final double hDistance, final double limitH, 
                                final double yDistance, final double limitV, final ModelFlying model, final List<String> tags, 
                                final MovingData data) {

        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        StringBuilder builder = new StringBuilder(350);
        final String dHDist = lastMove.toIsValid ? " (" + StringUtil.formatDiff(hDistance, lastMove.hDistance) + ")" : "";
        final String dYDist = lastMove.toIsValid ? " (" + StringUtil.formatDiff(yDistance, lastMove.yDistance)+ ")" : "";
        builder.append("hDist: " + hDistance + dHDist + " / " + limitH + " , vDist: " + yDistance + dYDist + " / " + limitV);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        if (lastMove.toIsValid) {
            builder.append(" , fdsq: " + StringUtil.fdec3.format(thisMove.distanceSquared / lastMove.distanceSquared));
        }
        if (thisMove.verVelUsed != null) {
            builder.append(" , vVelUsed: " + thisMove.verVelUsed);
        }
        builder.append(" , model: " + model.getId());
        if (!tags.isEmpty()) {
            builder.append(" , tags: ");
            builder.append(StringUtil.join(tags, "+"));
        }
        builder.append(" , jumpphase: " + data.sfJumpPhase);
        thisMove.addExtraProperties(builder, " , ");
        debug(player, builder.toString());
    }
}
