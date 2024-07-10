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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.combined.CombinedData;
import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.envelope.PlayerEnvelopes;
import fr.neatmonster.nocheatplus.checks.moving.envelope.workaround.AirWorkarounds;
import fr.neatmonster.nocheatplus.checks.moving.envelope.workaround.LiquidWorkarounds;
import fr.neatmonster.nocheatplus.checks.moving.envelope.workaround.LostGround;
import fr.neatmonster.nocheatplus.checks.moving.model.InputDirection;
import fr.neatmonster.nocheatplus.checks.moving.model.InputDirection.ForwardDirection;
import fr.neatmonster.nocheatplus.checks.moving.model.InputDirection.StrafeDirection;
import fr.neatmonster.nocheatplus.checks.moving.model.LiftOffEnvelope;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.workaround.WRPT;
import fr.neatmonster.nocheatplus.compat.*;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;

/**
 * The counterpart to the CreativeFly check, designed for ordinary gameplay (Survival/Adventure)
 */
@SuppressWarnings("StatementWithEmptyBody")
public class SurvivalFly extends Check {

    /** To join some tags with moving check violations. */
    private final ArrayList<String> tags = new ArrayList<>(15);
    private final ArrayList<String> justUsedWorkarounds = new ArrayList<>();
    private final BlockChangeTracker blockChangeTracker;

    /**
     * Instantiates a new survival fly check.
     */
    public SurvivalFly() {
        super(CheckType.MOVING_SURVIVALFLY);
        blockChangeTracker = NCPAPIProvider.getNoCheatPlusAPI().getBlockChangeTracker();
    }


    /**
     * Checks a player
     *
     * @param multiMoveCount
     *            0: Ordinary, 1/2/(...): first/second/(...) part of a split move.
     * @param isNormalOrPacketSplitMove
     *           Flag to indicate if the packet-based split move mechanic is used instead of the Bukkit-based one (or the move was not split)
     *
     * @return The Location where to set back the player to. Null in case of no violation.
     */
    public Location check(final Player player, final PlayerLocation from, final PlayerLocation to,
                          final int multiMoveCount, final MovingData data, final MovingConfig cc,
                          final IPlayerData pData, final int tick, final long now,
                          final boolean useBlockChangeTracker, final boolean isNormalOrPacketSplitMove) {
        /*
          TODO: Ideally, all this data should really be set outside SurvivalFly (in the MovingListener), since they can be useful
          for other checks / stuff.
         */
        tags.clear();
        justUsedWorkarounds.clear();
        // Shortcuts:
        final boolean debug = pData.isDebugActive(type);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        /* Regular and past fromOnGround */
        final boolean fromOnGround = from.isOnGround() || useBlockChangeTracker && from.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, data.blockChangeRef, tick);
        /* Regular and past toOnGround */
        final boolean toOnGround = to.isOnGround() || useBlockChangeTracker && to.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, data.blockChangeRef, tick);  // TODO: Work in the past ground stuff differently (thisMove, touchedGround?, from/to ...)
        /* Moving onto/into everything that isn't in air (liquids, stuck-speed, ground, ALL) */
        final boolean resetTo = toOnGround || to.isResetCond();
        /* Moving off from anything that is not air (liquids, stuck-speed, ground, ALL). */
        final boolean resetFrom = fromOnGround || from.isResetCond();

        // Set workarounds for the registry
        data.ws.setJustUsedIds(justUsedWorkarounds);

        // Run lostground checks.
        if (!resetFrom) {
            LostGround.lostGround(player, from, to, thisMove.hDistance, thisMove.yDistance, pData.isSprinting(), lastMove, data, cc, useBlockChangeTracker ? blockChangeTracker : null, tags);
        }

        // Recover from data removal (somewhat random insertion point).
        if (data.liftOffEnvelope == LiftOffEnvelope.UNKNOWN) {
            data.adjustLiftOffEnvelope(from);
        }

        // Adjust block properties (friction, block speed etc...)
        data.adjustMediumProperties(player.getLocation(), cc, player, thisMove);

        if (thisMove.touchedGround) {
            // Lost ground workaround has just been applied, check resetting of the dirty flag.
            // TODO: Always/never reset with any ground touched?
            if (!thisMove.from.onGround && !thisMove.to.onGround) {
                data.resetVelocityJumpPhase(tags);
            }
            // Ground somehow appeared out of thin air (block place).
            else if (multiMoveCount == 0 && thisMove.from.onGround && Magic.inAir(lastMove)
                    && TrigUtil.isSamePosAndLook(thisMove.from, lastMove.to)) {
                data.setSetBack(from);
                if (debug) {
                    debug(player, "Ground appeared due to a block-place: adjust set-back location.");
                }
            }
        }

        // Renew the "dirty"-flag (in-air phase affected by velocity).
        // (Reset is done after checks run.) 
        if (data.isVelocityJumpPhase() || data.resetVelocityJumpPhase(tags)) {
            tags.add("dirty");
        }

        // Decrease bunnyhop delay counter
        if (data.jumpDelay > 0) {
            data.jumpDelay--;
        }


        /////////////////////////////////////
        // Horizontal move                ///
        /////////////////////////////////////
        double hAllowedDistance, hDistanceAboveLimit, hFreedom;
        double[] resGlide = processGliding(from, to, pData, data, player, isNormalOrPacketSplitMove, fromOnGround, toOnGround);
        // Set the allowed distance and determine the distance above limit
        double[] hRes = Bridge1_9.isGliding(player) ? resGlide : hDistRel(from, to, pData, player, data, thisMove, lastMove, fromOnGround, toOnGround, debug, isNormalOrPacketSplitMove, false, false);
        hAllowedDistance = hRes[0];
        hDistanceAboveLimit = hRes[1];
        // Beyond limit? Check if there may have been a reason for this (and try to re-estimate if needed)
        if (hDistanceAboveLimit > 0.0) {
            double[] res = hDistAfterFailure(player, from, to, hAllowedDistance, hDistanceAboveLimit, thisMove, lastMove, debug, data, cc, pData, tick, useBlockChangeTracker, fromOnGround, toOnGround, isNormalOrPacketSplitMove);
            hAllowedDistance = res[0];
            hDistanceAboveLimit = res[1];
            hFreedom = res[2];
        }
        else {
            // Clear active velocity if the distance is within limit (clearly not needed. :))
            data.clearActiveHorVel();
            hFreedom = 0.0;
        }


        /////////////////////////////////////
        // Vertical move                  ///
        /////////////////////////////////////
        // Order of checking in EntityLiving.java water -> lava -> gliding -> air
        double yAllowedDistance, yDistanceAboveLimit = 0.0;
        // Step wild-card: allow step height from ground to ground.
        if (thisMove.yDistance >= 0.0 && thisMove.yDistance <= cc.sfStepHeight
            && toOnGround && fromOnGround && !from.isOnClimbable()
            && !to.isOnClimbable() && !thisMove.hasLevitation
            && !Bridge1_9.isGliding(player) && !Bridge1_13.isRiptiding(player)) {
            yAllowedDistance = cc.sfStepHeight;
            thisMove.isStepUp = true;
            tags.add("groundstep");
        }
        else if (from.isOnClimbable()) {
            // They can technically be placed inside liquids.
            final double[] res = vDistClimbable(player, from, to, pData, thisMove, lastMove, thisMove.yDistance, data, cc);
            yAllowedDistance = res[0];
            yDistanceAboveLimit = res[1];
        }
        else if (from.isInLiquid()) {
            // Minecraft checks for liquids first, then for air.
            final double[] res = vDistLiquid(thisMove, from, to, toOnGround, thisMove.yDistance, lastMove, data, player, cc, pData, fromOnGround);
            yAllowedDistance = res[0];
            yDistanceAboveLimit = res[1];
            // The friction jump phase has to be set externally.
            if (yDistanceAboveLimit <= 0.0 && thisMove.yDistance > 0.0
                && Math.abs(thisMove.yDistance) > Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player))) {
                data.setFrictionJumpPhase();
            }
        }
        else if (Bridge1_9.isGliding(player)) {
            yAllowedDistance = resGlide[2];
            yDistanceAboveLimit = resGlide[3];
            // If failed, try testing for after-failure conditions.
            if (yDistanceAboveLimit > 0.0) {
                double[] res = vDistAfterFailure(player, pData, data, fromOnGround, toOnGround, from, to, isNormalOrPacketSplitMove, resetFrom, resetTo, thisMove, lastMove, yDistanceAboveLimit, yAllowedDistance, useBlockChangeTracker);
                yAllowedDistance = res[0];
                yDistanceAboveLimit = res[1];
            }
        }
        else {
            final double[] res = vDistRel(now, player, from, fromOnGround, resetFrom, to, toOnGround, resetTo, thisMove.yDistance, isNormalOrPacketSplitMove, lastMove, data, cc, pData, false);
            yAllowedDistance = res[0];
            yDistanceAboveLimit = res[1];
            // If failed, try testing for after-failure conditions.
            if (yDistanceAboveLimit > 0.0) {
                double[] vRes = vDistAfterFailure(player, pData, data, fromOnGround, toOnGround, from, to, isNormalOrPacketSplitMove, resetFrom, resetTo, thisMove, lastMove, yDistanceAboveLimit, yAllowedDistance, useBlockChangeTracker);
                yAllowedDistance = vRes[0];
                yDistanceAboveLimit = vRes[1];
            }
        }


        ////////////////////////////
        // Debug output.          //
        ////////////////////////////
        final int tagsLength;
        if (debug) {
            outputDebug(player, to, from, data, thisMove.hDistance, hAllowedDistance, hFreedom, thisMove.yDistance, yAllowedDistance, fromOnGround, resetFrom, toOnGround, resetTo, thisMove);
            tagsLength = tags.size();
            data.ws.setJustUsedIds(null);
        }
        else tagsLength = 0; // JIT vs. IDE.


        //////////////////////////////////////
        // Handle violations               ///
        //////////////////////////////////////
        final boolean inAir = Magic.inAir(thisMove);
        final double result = (Math.max(hDistanceAboveLimit, 0.0) + Math.max(yDistanceAboveLimit, 0.0)) * 100D;
        if (result > 0.0) {
            final Location vLoc = handleViolation(result, player, from, to, data, cc);
            if (inAir) {
                data.sfVLInAir = true;
            }
            // Request a new to-location
            if (vLoc != null) {
                return vLoc;
            }
        }
        else {
            // Slowly reduce the level with each event, if violations have not recently happened.
            if (data.getPlayerMoveCount() - data.sfVLMoveCount > cc.survivalFlyVLFreezeCount
                    && (!cc.survivalFlyVLFreezeInAir || !inAir
                    // Favor bunny-hopping slightly: clean descend.
                    || !data.sfVLInAir
                    && data.liftOffEnvelope == LiftOffEnvelope.NORMAL
                    && lastMove.toIsValid
                    && lastMove.yDistance < -Magic.GRAVITY_MIN
                    && thisMove.yDistance - lastMove.yDistance < -Magic.GRAVITY_MIN)) {
                // Relax VL.
                data.survivalFlyVL *= 0.95;
            }
        }


        //////////////////////////////////////////////////////////////////////////////////////////////
        //  Set data for normal move or violation without cancel (cancel would have returned above) //
        //////////////////////////////////////////////////////////////////////////////////////////////
        // Adjust lift off envelope to medium
        final LiftOffEnvelope oldLiftOffEnvelope = data.liftOffEnvelope;
        if (thisMove.to.inLiquid) {
            if (fromOnGround && !toOnGround && data.liftOffEnvelope == LiftOffEnvelope.NORMAL
                && data.sfJumpPhase <= 0 && !thisMove.from.inLiquid) {
                // KEEP
            }
            else data.liftOffEnvelope = LiftOffEnvelope.LIMIT_LIQUID;
        }
        else if (thisMove.to.inPowderSnow) {
            data.liftOffEnvelope = LiftOffEnvelope.LIMIT_POWDER_SNOW;
        }
        else if (thisMove.to.inWeb) {
            data.liftOffEnvelope = LiftOffEnvelope.LIMIT_WEBS;
        }
        else if (thisMove.to.inBerryBush) {
            data.liftOffEnvelope = LiftOffEnvelope.LIMIT_SWEET_BERRY;
        }
        else if (thisMove.to.onHoneyBlock) {
            data.liftOffEnvelope = LiftOffEnvelope.LIMIT_HONEY_BLOCK;
        }
        else if (resetTo) {
            data.liftOffEnvelope = LiftOffEnvelope.NORMAL;
        }
        else if (thisMove.from.inLiquid) {
            if (!resetTo && data.liftOffEnvelope == LiftOffEnvelope.NORMAL && data.sfJumpPhase <= 0) {
                // KEEP
            }
            else data.liftOffEnvelope = LiftOffEnvelope.LIMIT_LIQUID;

        }
        else if (thisMove.from.inPowderSnow) {
            data.liftOffEnvelope = LiftOffEnvelope.LIMIT_POWDER_SNOW;
        }
        else if (thisMove.from.inWeb) {
            data.liftOffEnvelope = LiftOffEnvelope.LIMIT_WEBS;
        }
        else if (thisMove.from.inBerryBush) {
            data.liftOffEnvelope = LiftOffEnvelope.LIMIT_SWEET_BERRY;
        }
        else if (thisMove.from.onHoneyBlock) {
            data.liftOffEnvelope = LiftOffEnvelope.LIMIT_HONEY_BLOCK;
        }
        else if (resetFrom || thisMove.touchedGround) {
            data.liftOffEnvelope = LiftOffEnvelope.NORMAL;
        }
        else {
            // Air, Keep medium.
        }

        // 2: Count how long one is moving inside a medium.
        if (oldLiftOffEnvelope != data.liftOffEnvelope) {
            data.insideMediumCount = 0;
        }
        else if (!resetFrom || !resetTo) {
            data.insideMediumCount = 0;
        }
        else data.insideMediumCount ++;

        // Apply reset conditions.
        if (resetTo) {
            // Reset data.
            data.setSetBack(to);
            data.sfJumpPhase = 0;
            if (hFreedom <= 0.0 && thisMove.verVelUsed == null) {
                data.resetVelocityJumpPhase(tags);
            }
        }
        // The player moved from ground.
        else if (resetFrom) {
            data.setSetBack(from);
            data.sfJumpPhase = 1; // This event is already in air.
        }
        else {
            data.sfJumpPhase ++;
            // TODO: Void-to-void: Rather handle unified somewhere else (!).
            if (to.getY() < 0.0 && cc.sfSetBackPolicyVoid 
                || thisMove.hasLevitation
                || Bridge1_13.isRiptiding(player)
                || Bridge1_9.isGliding(player)) {
                data.setSetBack(to);
            }
        }

        // Adjust not in-air stuff.
        if (!inAir) {
            data.ws.resetConditions(WRPT.G_RESET_NOTINAIR);
            data.sfVLInAir = false;
        }

        // Update unused velocity tracking.
        // TODO: Hide and seek with API.
        // TODO: Pull down tick / timing data (perhaps add an API object for millis + source + tick + sequence count (+ source of sequence count).
        if (debug) {
            // TODO: Only update, if velocity is queued at all.
            data.getVerticalVelocityTracker().updateBlockedState(tick,
                    // Assume blocked with being in web/water, despite not entirely correct.
                    thisMove.headObstructed || thisMove.from.resetCond,
                    // (Similar here.)
                    thisMove.touchedGround || thisMove.to.resetCond);
            // TODO: TEST: Check unused velocity here too. (Should have more efficient process, pre-conditions for checking.)
            UnusedVelocity.checkUnusedVelocity(player, type, data, cc);
        }

        // Adjust various speed/friction factors (both h/v).
        data.lastFrictionVertical = data.nextFrictionVertical;
        data.lastFrictionHorizontal = data.nextFrictionHorizontal;
        data.lastStuckInBlockVertical = data.nextStuckInBlockVertical;
        data.lastStuckInBlockHorizontal = data.nextStuckInBlockHorizontal;
        data.lastBlockSpeedMultiplier = data.nextBlockSpeedMultiplier;
        data.lastInertia = data.nextInertia;
        data.lastNonVanillaFrictionVertical = data.nextNonVanillaFrictionVertical;
        data.lastLevitationLevel = thisMove.hasLevitation ? Bridge1_9.getLevitationAmplifier(player) + 1 : 0.0;

        // Log tags added after violation handling.
        if (debug && tags.size() > tagsLength) {
            logPostViolationTags(player);
        }
        // Nothing to do, newTo (MovingListener) stays null
        return null;
    }






    /**
     * A check to prevent players from bed-flying.
     * To be called on PlayerBedLeaveEvent(s)
     * (This increases VL and sets tag only. Setback is done in MovingListener).
     *
     * @return If to prevent action (use the setback location of survivalfly).
     */
    public boolean checkBed(final Player player, final MovingConfig cc, final MovingData data) {
        boolean cancel = false;
        // Check if the player had been in bed at all.
        if (!data.wasInBed) {
            // Violation ...
            tags.add("bedfly");
            data.survivalFlyVL += 100D;
            Improbable.check(player, (float) 5.0, System.currentTimeMillis(), "moving.survivalfly.bedfly", DataManager.getPlayerData(player));
            final ViolationData vd = new ViolationData(this, player, data.survivalFlyVL, 100D, cc.survivalFlyActions);
            if (vd.needsParameters()) vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
            cancel = executeActions(vd).willCancel();
        }
        // Nothing detected.
        else data.wasInBed = false;
        return cancel;
    }


    /**
     * Check for push/pull by pistons, alter data appropriately (blockChangeId).
     */
    private double[] getVerticalBlockMoveResult(final double yDistance, final PlayerLocation from, final PlayerLocation to, final MovingData data) {
        /*
         * TODO: Pistons pushing horizontally allow similar/same upwards
         * (downwards?) moves (possibly all except downwards, which is hard to
         * test :p).
         */
        // TODO: Allow push up to 1.0 (or 0.65 something) even beyond block borders, IF COVERED [adapt PlayerLocation].
        // TODO: Other conditions/filters ... ?
        // Push (/pull) up.
        if (yDistance > 0.0) {
            if (yDistance <= 1.015) {
                /*
                 * (Full blocks: slightly more possible, ending up just above
                 * the block. Bounce allows other end positions.)
                 */
                // TODO: Is the air block wich the slime block is pushed onto really in? 
                if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, Direction.Y_POS, Math.min(yDistance, 1.0))) {
                    if (yDistance > 1.0) {
                        if (to.getY() - to.getBlockY() >= 0.015) {
                            // Exclude ordinary cases for this condition.
                            return null;
                        }
                    }
                    tags.add("blkmv_y_pos");
                    final double maxDistYPos = yDistance; //1.0 - (from.getY() - from.getBlockY()); // TODO: Margin ?
                    return new double[]{maxDistYPos, 0.0};
                }
            }
        }
        // Push (/pull) down.
        else if (yDistance < 0.0 && yDistance >= -1.0) {
            if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, Direction.Y_NEG, -yDistance)) {
                tags.add("blkmv_y_neg");
                final double maxDistYNeg = yDistance; // from.getY() - from.getBlockY(); // TODO: Margin ?
                return new double[]{maxDistYNeg, 0.0};
            }
        }
        // Nothing found.
        return null;
    }


    /**
     * Determine the allowed h / v distance for gliding.
     * Handled in its own method because of vertical and horizontal motion being too intertwined to separate (y-distance changes relate to h-distance changes). <br>
     * Consistency checks are done within the combined listener.<br>
     * <li> NOTE: this should be called with Bridge1_9#isGliding() not Bridge1_9#isGlidingWithElytra(), because the client does not check for elytra to apply the corresponding motion (EntityLiving, travel())</li>
     *
     * @return the allowed xyz distances + distances above limit.
     */
    private double[] processGliding(final PlayerLocation from, final PlayerLocation to, final IPlayerData pData, final MovingData data,
                                    final Player player, boolean isNormalOrPacketSplitMove, final boolean fromOnGround, final boolean toOnGround) {
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        double yDistanceAboveLimit = 0.0, hDistanceAboveLimit = 0.0;
        // TODO: What with gliding + rocket boosting + riptiding + bouncing on slimes/beds while riptiding? LMAO
        // Beats me why Mojang keeps letting players perform such ridiculous moves.
        if (!Bridge1_9.isGliding(player)) {
            // No Gliding, no deal
            return new double[] {thisMove.hDistance, 0.0, thisMove.yDistance, 0.0};
        }
        // WASD key presses, as well as sneaking and item-use are irrelevant when gliding.
        thisMove.hasImpulse = false;
        thisMove.forwardImpulse = InputDirection.ForwardDirection.NONE;
        thisMove.strafeImpulse = InputDirection.StrafeDirection.NONE;
        // Initialize speed.
        thisMove.xAllowedDistance = lastMove.toIsValid ? lastMove.xDistance : 0.0;
        thisMove.yAllowedDistance = lastMove.toIsValid ? lastMove.yDistance : 0.0;
        thisMove.zAllowedDistance = lastMove.toIsValid ? lastMove.zDistance : 0.0;
        // Throttle speed if stuck in a block.
        if (TrigUtil.lengthSquared(data.lastStuckInBlockHorizontal, data.lastStuckInBlockVertical, data.lastStuckInBlockHorizontal) > 1.0E-7) {
            if (data.lastStuckInBlockVertical != 1.0) {
                thisMove.yAllowedDistance = 0.0;
            }
            if (data.lastStuckInBlockHorizontal != 1.0) {
                thisMove.xAllowedDistance = thisMove.zAllowedDistance = 0.0;
            }
        }
        // Reset speed if judged to be negligible.
        checkNegligibleMomentum(pData, thisMove);
        
        // TODO: Reduce verbosity (at least, make it easier to look at)
        Vector viewVector = TrigUtil.getLookingDirection(to, player);
        float radianPitch = to.getPitch() * TrigUtil.toRadians;
        // Horizontal length of the look direction
        double viewVecHorizontalLength = MathUtil.dist(viewVector.getX(), viewVector.getZ());
        // Horizontal length of the movement
        double thisMoveHDistance = MathUtil.dist(thisMove.xAllowedDistance, thisMove.zAllowedDistance); // NOTE: MUST BE the ALLOWED distances.
        // Overall length of the look direction.
        double viewVectorLength = viewVector.length();
        // Mojang switched from their own cosine function to the standard Math.cos() one in 1.18.2
        double cosPitch = pData.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_18_2) ? TrigUtil.cos((double)radianPitch) : Math.cos((double)radianPitch);
        cosPitch = cosPitch * cosPitch * Math.min(1.0, viewVectorLength / 0.4);
        // Base gravity when gliding.
        thisMove.yAllowedDistance += (thisMove.hasSlowfall && lastMove.yDistance <= 0.0 ? Magic.SLOW_FALL_GRAVITY : Magic.DEFAULT_GRAVITY) * (-1.0 + cosPitch * 0.75);
        double baseSpeed;
        if (thisMove.yAllowedDistance < 0.0 && viewVecHorizontalLength > 0.0) {
            // Slow down.
            baseSpeed = thisMove.yAllowedDistance * -0.1 * cosPitch;
            thisMove.xAllowedDistance += viewVector.getX() * baseSpeed / viewVecHorizontalLength;
            thisMove.yAllowedDistance += baseSpeed;
            thisMove.zAllowedDistance += viewVector.getZ() * baseSpeed / viewVecHorizontalLength;
        }
        if (radianPitch < 0.0 && viewVecHorizontalLength > 0.0) {
            // Looking down speeds up the player.
            baseSpeed = thisMoveHDistance * (double) (-TrigUtil.sin(radianPitch)) * 0.04;
            thisMove.xAllowedDistance += -viewVector.getX() * baseSpeed / viewVecHorizontalLength;
            thisMove.yAllowedDistance += baseSpeed * 3.2;
            thisMove.zAllowedDistance += -viewVector.getZ() * baseSpeed / viewVecHorizontalLength;
        }
        if (viewVecHorizontalLength > 0.0) {
            // Accelerate
            thisMove.xAllowedDistance += (viewVector.getX() / viewVecHorizontalLength * thisMoveHDistance - thisMove.xAllowedDistance) * 0.1;
            thisMove.zAllowedDistance += (viewVector.getZ() / viewVecHorizontalLength * thisMoveHDistance - thisMove.zAllowedDistance) * 0.1;
        }
        // Boosted with a firework: propel the player.
        if (data.fireworksBoostDuration > 0) {
            // TODO: Firework netcode is horrible (a single firework can tick twice on the same tick, skipping the subsequent one), so simply applying the increase of speed won't cut it.
            // Not even sure if we can predict this at all without some kind of hacks / workarounds.
            thisMove.xAllowedDistance += viewVector.getX() * 0.1 + (viewVector.getX() * 1.5 - thisMove.xAllowedDistance) * 0.5;
            thisMove.yAllowedDistance += viewVector.getY() * 0.1 + (viewVector.getY() * 1.5 - thisMove.yAllowedDistance) * 0.5;
            thisMove.zAllowedDistance += viewVector.getZ() * 0.1 + (viewVector.getZ() * 1.5 - thisMove.zAllowedDistance) * 0.5;
        }
        // Friction here. (TEST)
        // Note about inertia: the game assigns the radian pitch to the "f" variable, which is the variable used to apply friction at the end of the tick, _normally_.
        // However, with gliding, the game does not use the f variable at the end of the tick, but instead applies the magic value of 0.99.
        thisMove.xAllowedDistance *= 0.99;
        thisMove.yAllowedDistance *= data.lastFrictionVertical;
        thisMove.zAllowedDistance *= 0.99;

        // Stuck-speed with the updated multiplier (both at the end)
        if (TrigUtil.lengthSquared(data.nextStuckInBlockHorizontal, data.nextStuckInBlockVertical, data.nextStuckInBlockHorizontal) > 1.0E-7) {
            thisMove.xAllowedDistance *= (double) data.nextStuckInBlockHorizontal;
            thisMove.yAllowedDistance *= (double) data.nextStuckInBlockVertical;
            thisMove.zAllowedDistance *= (double) data.nextStuckInBlockHorizontal;
        }
        // Yes, players can glide and riptide at the same time, increasing speed at a faster rate than chunks can load...
        // Surely a questionable decision on Mojang's part.
        if (lastMove.slowedByUsingAnItem && !thisMove.slowedByUsingAnItem && thisMove.isRiptiding) {
            Vector propellingForce = to.getTridentPropellingForce(player, fromOnGround);
            // Fortunately, we do not have to account for onGround push here, as gliding does not work on ground.
            thisMove.xAllowedDistance += propellingForce.getX();
            thisMove.yAllowedDistance += propellingForce.getY();
            thisMove.zAllowedDistance += propellingForce.getZ();
        }
        // Collisions last.
        Vector collisionVector = from.collide(new Vector(thisMove.xAllowedDistance, thisMove.yAllowedDistance, thisMove.zAllowedDistance), fromOnGround || thisMove.touchedGroundWorkaround, pData.getGenericInstance(MovingConfig.class), from.getAABBCopy());
        thisMove.xAllowedDistance = collisionVector.getX();
        thisMove.yAllowedDistance = collisionVector.getY();
        thisMove.zAllowedDistance = collisionVector.getZ();
        thisMove.collideX = collisionVector.getX() != thisMove.xAllowedDistance;
        thisMove.collideY = collisionVector.getY() != thisMove.yAllowedDistance;
        thisMove.collideZ = collisionVector.getZ() != thisMove.zAllowedDistance;

        // Can a vertical workaround apply? If so, override the prediction.
        if (AirWorkarounds.checkPostPredictWorkaround(data, fromOnGround, toOnGround, from, to, thisMove.yAllowedDistance, player, isNormalOrPacketSplitMove)) {
            thisMove.yAllowedDistance = thisMove.yDistance;
        }
        
        ////////////////////////////
        /// Calculate offests     //
        ////////////////////////////
        /* Expected difference from current to allowed */
        final double offsetV = thisMove.yDistance - thisMove.yAllowedDistance;
        if (Math.abs(offsetV) < Magic.PREDICTION_EPSILON) {
            // Accuracy margin.
        }
        else {
            // If velocity can be used for compensation, use it.
            if (data.getOrUseVerticalVelocity(thisMove.yDistance) == null) {
                yDistanceAboveLimit = Math.max(yDistanceAboveLimit, Math.abs(offsetV));
                tags.add("vdistrel");
            }
        }

        thisMove.hAllowedDistance = MathUtil.dist(thisMove.xAllowedDistance, thisMove.zAllowedDistance);
        final double offsetH = thisMove.hDistance - thisMove.hAllowedDistance;
        if (offsetH < Magic.PREDICTION_EPSILON) {
            // Accuracy margin.
        }
        else {
            hDistanceAboveLimit = Math.max(hDistanceAboveLimit, offsetH);
            tags.add("hdistrel");
        }
        player.sendMessage("hDistance/Predicted " + StringUtil.fdec6.format(thisMove.hDistance) + " / " + StringUtil.fdec6.format(thisMove.hAllowedDistance));
        player.sendMessage("vDistance/Predicted " + StringUtil.fdec6.format(thisMove.yDistance) + " / " + StringUtil.fdec6.format(thisMove.yAllowedDistance));
        return new double[]{thisMove.hAllowedDistance, hDistanceAboveLimit, thisMove.yAllowedDistance, yDistanceAboveLimit};
    }
    
    /**
     * Check if the allowed speed set in thisMove should be canceled due to it being lower than the negligible speed threshold.
     * 
     * @param pData
     * @param thisMove
     */
    private void checkNegligibleMomentum(IPlayerData pData, PlayerMoveData thisMove) {
        if (pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            if (Math.abs(thisMove.xAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD) {
                thisMove.xAllowedDistance = 0.0;
            }
            if (Math.abs(thisMove.yAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD) {
                thisMove.yAllowedDistance = 0.0;
            }
            if (Math.abs(thisMove.zAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD) {
                thisMove.zAllowedDistance = 0.0;
            }
        }
        else {
            // In 1.8 and lower, momentum is compared to 0.005 instead.
            if (Math.abs(thisMove.xAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY) {
                thisMove.xAllowedDistance = 0.0;
            }
            if (Math.abs(thisMove.yAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY) {
                thisMove.yAllowedDistance = 0.0;
            }
            if (Math.abs(thisMove.zAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY) {
                thisMove.zAllowedDistance = 0.0;
            }
        }
    }
    
    /**
     * Estimate the player's horizontal speed, according to the given data (inertia, ground status, etc...)
     */
    private void processNextSpeed(final Player player, float movementSpeed, final IPlayerData pData, final Collection<String> tags,
                                  final PlayerLocation to, final PlayerLocation from, final boolean debug,
                                  final boolean fromOnGround, final boolean toOnGround, final boolean onGround) {
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final CombinedData cData = pData.getGenericInstance(CombinedData.class);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        /*
         * Order of operations is essential. Do not shuffle things around unless you know what you're doing.
         *
         * Order of client-movement operations (MCP):
         * - LivingEntity.tick()
         * - Entity.tick()
         *    -> Entity.baseTick()
         *    -> Entity.updateInWaterStateAndDoFluidPushing() - 9
         * - LivingEntity.aiStep()
         *    -> Negligible speed(0.003) - 10
         *    -> LivingEntity.jumpFromGround() - 11
         * - Begin running LivingEntity.travel() - 12
         *    - Call Entity.moveRelative() (apply acceleration, getInputVector() [!]) - 13
         *    - Call Entity.move()
         *       -> Stuck speed multiplier 14
         *       -> maybeBackOffFromEdge - 15
         *       -> wall collision(Entity.collide()) - 16 [Speed is cut off and the collision flag is set.]
         *>>>>>>>Complete this move, prepare the next one.<<<<<<<
         *       -> horizontalCollision - next move - 1 [Speed is now reset to 0.0 on the colliding axis]
         *       -> checkFallDamage(do liquid pushing if was not previously in water) - next move - 2
         *       -> Block.updateEntityAfterFallOn() - next move - 3
         *       -> Block.stepOn() (for slime block) - next move - 4
         *       -> tryCheckInsideBlocks() (for honey block) - next move - 5
         *       -> Entity.getBlockSpeedFactor() - next move - 6
         * - Complete running the LivingEntity.travel() function. Apply friction/inertia - next move - 7
         * - Entity pushing - next move - complete running the aiStep() function - 8
         * - Complete running the LivingEntity.tick() function
         *>>>>>>> Send movement to the server<<<<<<<
         * - Repeat
         *
         * From the order above, we will start from horizontalCollision: first all calculations done by the client on the next tick, then the last
         */

        // Because Minecraft does not offer any way to listen to player's inputs, we brute force through all combinations and see which input combo matches the current speed of the player.
        ///////////////////////////////
        // Setup theoretical inputs  //
        ///////////////////////////////
        // The input's matrix is 9:  NONE, LEFT, RIGHT, FORWARD, FORWARD_LEFT, FORWARD_RIGHT, BACKWARD, BACKWARD_LEFT, BACKWARD_RIGHT.
        InputDirection[] inputs = new InputDirection[9]; 
        /** The index representing the input */
        int i = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                // Minecraft multiplies input values by 0.98 before passing them to the travel() function.
                inputs[i] = new InputDirection(x * 0.98f, z * 0.98f);
                i++;
            }
        }
        // From KeyboardInput.java (MC-Reborn tool)
        // Sneaking and item-use aren't directly applied to the player's motion. The game reduces the input force instead.
        // Note that this is determined by player poses, not shift key presses.
        if (pData.isCrouching()) {
            tags.add("crouching");
            float SwiftSneakIncrement = BridgeEnchant.getSwiftSneakIncrement(player);
            for (i = 0; i < 9; i++) {
                // Multiply all combinations
                inputs[i].runOperation(Magic.SNEAK_MULTIPLIER + SwiftSneakIncrement, Magic.SNEAK_MULTIPLIER + SwiftSneakIncrement, 1);
            }
        }
        // From LocalPlayer.java.aiStep()
        if (BridgeMisc.isUsingItem(player)) {
            tags.add("usingitem");
            for (i = 0; i < 9; i++) {
                inputs[i].runOperation(Magic.USING_ITEM_MULTIPLIER, Magic.USING_ITEM_MULTIPLIER, 1);
            }
        }


        ///////////////////////////////////////
        // Next client-tick/move            //
        //////////////////////////////////////
        // Initialize the allowed distance(s) with the previous speed. (Only if we have end-point coordinates)
        // This essentially represents the momentum of the player.
        thisMove.xAllowedDistance = lastMove.toIsValid ? lastMove.xDistance : 0.0;
        thisMove.zAllowedDistance = lastMove.toIsValid ? lastMove.zDistance : 0.0;
        // If the player collided with something on the previous tick, start with 0 momentum now.
        if (lastMove.collideX) {
            thisMove.xAllowedDistance = 0.0;
        }
        if (lastMove.collideZ) {
            thisMove.zAllowedDistance = 0.0;
        }
        // (The game calls a checkFallDamage() function, which, as you can imagine, handles fall damage. But also handles liquids' flow force, thus we need to apply this 2 times.)
        if (from.isInWater() && !lastMove.from.inWater) {
            Vector liquidFlowVector = from.getLiquidPushingVector(thisMove.xAllowedDistance, thisMove.zAllowedDistance, BlockFlags.F_WATER);
            thisMove.xAllowedDistance += liquidFlowVector.getX();
            thisMove.zAllowedDistance += liquidFlowVector.getZ();
        }
        // Slime speed
        if (from.isOnSlimeBlock() && onGround) {
            final double diff = Math.abs(0.0784000015258789 - lastMove.yDistance);
            boolean isGravity = MathUtil.inRange( 0.0784, diff,  0.0784000015258789); // diff <= 0.0784000015258789 && diff >= 0.0784;
            final double result = isGravity ? 0.0784000015258789 : lastMove.yDistance;
            if (Math.abs(result) < 0.1 && !player.isSneaking()) { // isSteppingCarefully -> isShiftKeyDown. Using isSneaking is correct.
                thisMove.xAllowedDistance *= 0.4 + Math.abs(result) * 0.2;
                thisMove.zAllowedDistance *= 0.4 + Math.abs(result) * 0.2;
            }
        }
        // Sliding speed (honey block)
        if (from.isSlidingDown()) {
            if (lastMove.yDistance < -Magic.SLIDE_START_AT_VERTICAL_MOTION_THRESHOLD) {
                thisMove.xAllowedDistance *= -Magic.SLIDE_SPEED_THROTTLE / lastMove.yDistance;
                thisMove.zAllowedDistance *= -Magic.SLIDE_SPEED_THROTTLE / lastMove.yDistance;
            }
        }
        // Stuck speed reset (the game resets momentum each tick the player is in a stuck-speed block)
        if (data.lastStuckInBlockHorizontal != 1.0) {
            if (TrigUtil.lengthSquared(data.lastStuckInBlockHorizontal, data.lastStuckInBlockVertical, data.lastStuckInBlockHorizontal) > 1.0E-7) { // (Vanilla check, don't ask)
                // Throttle speed if stuck in.
                thisMove.xAllowedDistance = thisMove.zAllowedDistance = 0.0;
            }
        }
        // Block speed
        thisMove.xAllowedDistance *= (double) data.nextBlockSpeedMultiplier;
        thisMove.zAllowedDistance *= (double) data.nextBlockSpeedMultiplier;
        // Friction next.
        thisMove.xAllowedDistance *= (double) data.lastInertia;
        thisMove.zAllowedDistance *= (double) data.lastInertia;
        // Apply entity-pushing speed
        // From Entity.java.push()
        // The entity's location is in the past.
        // TODO: Properly implement


        //////////////////////////////////
        // Last client-tick/move        //
        //////////////////////////////////
        // See CombinedListener.java for more details
        // This is done before liquid pushing...
        if (thisMove.hasAttackSlowDown) {
            thisMove.zAllowedDistance *= Magic.ATTACK_SLOWDOWN;
            thisMove.xAllowedDistance *= Magic.ATTACK_SLOWDOWN;
        }
        // Apply liquid pushing speed (2nd call).
        if (from.isInLiquid()) {
            Vector liquidFlowVector = from.getLiquidPushingVector(thisMove.xAllowedDistance, thisMove.zAllowedDistance, from.isInWater() ? BlockFlags.F_WATER : BlockFlags.F_LAVA);
            thisMove.xAllowedDistance += liquidFlowVector.getX();
            thisMove.zAllowedDistance += liquidFlowVector.getZ();
        }
        // Before calculating the acceleration, check if momentum is below the negligible speed threshold and cancel it.
        checkNegligibleMomentum(pData, thisMove);
        // Sprint-jumping...
        // NOTE: here you must use to.getYaw not from. To is the most recent rotation. Using from lags behind a few ticks, causing false positives when switching looking direction.
        // This does not apply for locations (from.(...) correctly reflects the player's current position)
        if (PlayerEnvelopes.isBunnyhop(from, to, pData, fromOnGround, toOnGround, player)) {
            thisMove.xAllowedDistance += (double) (-TrigUtil.sin(to.getYaw() * TrigUtil.toRadians) * Magic.BUNNYHOP_BOOST);
            thisMove.zAllowedDistance += (double) (TrigUtil.cos(to.getYaw() * TrigUtil.toRadians) * Magic.BUNNYHOP_BOOST);
            thisMove.bunnyHop = true;
            tags.add("bunnyhop");
        }
        // Transform theoretical inputs to acceleration vectors (getInputVector, entity.java)
        // NOTE: [See above]
        float sinYaw = TrigUtil.sin(to.getYaw() * TrigUtil.toRadians);
        float cosYaw = TrigUtil.cos(to.getYaw() * TrigUtil.toRadians);
        /* List of predicted X distances. Size is the number of possible inputs (left/right/backwards/forward etc...) */
        double[] xTheoreticalDistance = new double[9];
        /* To keep track which theoretical speed would result in a collision on the X axis */
        boolean[] collideX = new boolean[9];
        /* List of predicted Z distances. Size is the number of possible inputs (left/right/backwards/forward etc...) */
        double[] zTheoreticalDistance = new double[9];
        /* To keep track which theoretical speed would result in a collision on the Z axis */
        boolean[] collideZ = new boolean[9];
        for (i = 0; i < 9; i++) {
            // Each slot in the array is initialized with the same momentum first.
            xTheoreticalDistance[i] = thisMove.xAllowedDistance;
            zTheoreticalDistance[i] = thisMove.zAllowedDistance;
            // Then we proceed to compute all possible accelerations with all theoretical inputs.
            double inputSq = MathUtil.square((double)inputs[i].getStrafe()) + MathUtil.square((double)inputs[i].getForward()); // Cast to a double because the client does it
            if (inputSq >= 1.0E-7) {
                if (inputSq > 1.0) {
                    double inputForce = Math.sqrt(inputSq);
                    if (inputForce < 1.0E-4) {
                        // Not enough force, reset.
                        inputs[i].runOperation(0, 0, 0);
                    }
                    else {
                        // Normalize
                        inputs[i].runOperation(inputForce, inputForce, 2);
                    }
                }
                // Multiply all inputs by movement speed.
                inputs[i].runOperation(movementSpeed, movementSpeed, 1);
                // The acceleration vector is added to each momentum.
                xTheoreticalDistance[i] += inputs[i].getStrafe() * (double)cosYaw - inputs[i].getForward() * (double)sinYaw;
                zTheoreticalDistance[i] += inputs[i].getForward() * (double)cosYaw + inputs[i].getStrafe() * (double)sinYaw;
            }
        }
        // Minecraft caps horizontal speed if on climbable, for whatever reason.
        if (from.isOnClimbable() && !from.isInLiquid()) {
            for (i = 0; i < 9; i++) {
                xTheoreticalDistance[i] = MathUtil.clamp(xTheoreticalDistance[i], -Magic.CLIMBABLE_MAX_SPEED, Magic.CLIMBABLE_MAX_SPEED);
                zTheoreticalDistance[i] = MathUtil.clamp(zTheoreticalDistance[i], -Magic.CLIMBABLE_MAX_SPEED, Magic.CLIMBABLE_MAX_SPEED);
            }
        }
        // Stuck-speed multiplier.
        if (TrigUtil.lengthSquared(data.nextStuckInBlockHorizontal, data.nextStuckInBlockVertical, data.nextStuckInBlockHorizontal) > 1.0E-7) {
            for (i = 0; i < 9; i++) {
                xTheoreticalDistance[i] *= (double) data.nextStuckInBlockHorizontal;
                zTheoreticalDistance[i] *= (double) data.nextStuckInBlockHorizontal;
            }
        }
        // Try to back off players from edges, if sneaking.
        // NOTE: here the game uses isShiftKeyDown (so this is shifting not sneaking, using Bukkit's isSneaking is correct)
        if (!player.isFlying() && player.isSneaking() && from.isAboveGround() && thisMove.yDistance <= 0.0) {
            for (i = 0; i < 9; i++) {
                // TODO: Optimize. Brute forcing collisions with all 9 speed combinations will tank performance.
                Vector backOff = from.maybeBackOffFromEdge(new Vector(xTheoreticalDistance[i], thisMove.yDistance, zTheoreticalDistance[i]));
                xTheoreticalDistance[i] = backOff.getX();
                zTheoreticalDistance[i] = backOff.getZ();
            }
        }
        if (lastMove.slowedByUsingAnItem && !thisMove.slowedByUsingAnItem && thisMove.isRiptiding) {
            // Riptide works by propelling the player after releasing the trident (the effect only pushes the player, unless is on ground)
            Vector propellingForce = to.getTridentPropellingForce(player, lastMove.touchedGround);
            for (i = 0; i < 9; i++) {
                xTheoreticalDistance[i] += propellingForce.getX();
                zTheoreticalDistance[i] += propellingForce.getZ();
            }
        }
        // TODO: Optimize. Brute forcing collisions with all 9 speed combinations will tank performance.
        for (i = 0; i < 9; i++) {
            // NOTE: Passing the unchecked y-distance is fine in this case. Vertical collision is checked with vdistrel (just separately).
            Vector collisionVector = from.collide(new Vector(xTheoreticalDistance[i], thisMove.yDistance, zTheoreticalDistance[i]), onGround, pData.getGenericInstance(MovingConfig.class), from.getAABBCopy());
            if (xTheoreticalDistance[i] != collisionVector.getX()) {
                // This theoretical speed would result in a collision. Remember it.
                collideX[i] = true;
            }
            if (zTheoreticalDistance[i] != collisionVector.getZ()) {
                // This theoretical speed would result in a collision. Remember it.
                collideZ[i] = true;
            }
            xTheoreticalDistance[i] = collisionVector.getX();
            zTheoreticalDistance[i] = collisionVector.getZ();
        }
        // Check for block push.
        // TODO: Unoptimized insertion point... Waste of resources to just override everything at the end.
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        if (cc.trackBlockMove) {
            for (i = 0; i < 9; i++) {
                if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, xTheoreticalDistance[i] < 0.0 ? Direction.X_NEG : Direction.X_POS, 0.05)) {
                    xTheoreticalDistance[i] = thisMove.xDistance;
                }
            }
            for (i = 0; i < 9; i++) {
                if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, zTheoreticalDistance[i] < 0.0 ? Direction.Z_NEG : Direction.Z_POS, 0.05)) {
                    zTheoreticalDistance[i] = thisMove.zDistance;
                }
            }
        }
        

        /////////////////////////////////////////////////////////////////////////////
        // Determine which (and IF) theoretical speed should be set in this move   //
        /////////////////////////////////////////////////////////////////////////////
        /*
          True, if the offset between predicted and actual speed is smaller than the accuracy margin (0.0001).
         */
        boolean found = false;
        /*
          True will check if BOTH axis have an offset smaller than 0.0001 (against strafe-like cheats and anything of that sort that relies on the specific direction of the move).
          Will also perform some auxiliary checks.<br>
          Otherwise, only the combined horizontal distance will be checked against the offset.
         */
        boolean strict = cc.survivalFlyStrictHorizontal;
        for (i = 0; i < 9; i++) {
            if (strict) {
                if (MathUtil.almostEqual(thisMove.xDistance, xTheoreticalDistance[i], Magic.PREDICTION_EPSILON)
                    && MathUtil.almostEqual(thisMove.zDistance, zTheoreticalDistance[i], Magic.PREDICTION_EPSILON)) {
                    found = true;
                }
            }
            else {
                double theoreticalHDistance = MathUtil.dist(xTheoreticalDistance[i], zTheoreticalDistance[i]);
                if (MathUtil.almostEqual(theoreticalHDistance, thisMove.hDistance, Magic.PREDICTION_EPSILON)) {
                    found = true;
                }
            }
            
            if (found) {
                if (cData.isHackingRI) {
                    // Blatant cheat attempt, do not set speed.
                    tags.add("noslowpacket");
                    cData.isHackingRI = false;
                    Improbable.check(player, (float) thisMove.hDistance, System.currentTimeMillis(), "moving.survivalfly.noslow", pData);
                    // Keep looping
                    found = false;
                }
                else if ((inputs[i].getForwardDir().equals(ForwardDirection.BACKWARD) // Moving backwards
                        || inputs[i].getForward() < 0.8 // hasEnoughImpulseToStartSprinting, in LocalPlayer,java -> aiStep()
                        || inputs[i].getForwardDir().equals(ForwardDirection.NONE) && !inputs[i].getStrafeDir().equals(StrafeDirection.NONE)) // Moving sideways only.
                        && pData.isSprinting()) { 
                    // Stop omnisprinting.
                    tags.add("omnisprint");
                    pData.setSprintingState(false);
                    Improbable.check(player, (float) thisMove.hDistance, System.currentTimeMillis(), "moving.survivalfly.omnisprint", pData);
                    // Keep looping
                    found = false;
                }
                else {
                    // Found a candidate to set in this move; these collisions are valid.
                    thisMove.collideX = collideX[i];
                    thisMove.collideZ = collideZ[i];
                    thisMove.negligibleHorizontalCollision = (thisMove.collideX || thisMove.collideZ) && CollisionUtil.isHorizontalCollisionNegligible(new Vector(xTheoreticalDistance[i], thisMove.yDistance, zTheoreticalDistance[i]), to, inputs[i].getStrafe(), inputs[i].getForward());
                    break;
                }
            }
        }


        //////////////////////////////////////////////////////////////
        // Finish. Check if the move had been predictable at all    //
        //////////////////////////////////////////////////////////////
        int inputsIdx = i;
        final MovingConfig cc1 = pData.getGenericInstance(MovingConfig.class);
        /* All moves are assumed to be predictable, unless we explicitly state in here otherwise. */
        boolean isPredictable = true;
        int x_idx = -1;
        int z_idx = -1;
        if (inputsIdx >= 9) {
            // Among all 9 predicted speeds, we couldn't find one with an offset of 0.0001 from actual speed.
            // To prevent an IndexOutOfBounds, set the input index back to 4.
            // (This is the one that will be set IF the player is actually cheating)
            inputsIdx = 4;
        }
        // Finally, set in this move.
        thisMove.xAllowedDistance = xTheoreticalDistance[!isPredictable ? x_idx : inputsIdx];
        thisMove.zAllowedDistance = zTheoreticalDistance[!isPredictable ? z_idx : inputsIdx];
        // Set more edge data useful for other stuff.
        // TODO: How can we know the impulse if the move is uncertain? ...
        thisMove.hasImpulse = inputs[isPredictable ? inputsIdx : x_idx].getForwardDir() != ForwardDirection.NONE && inputs[isPredictable ? inputsIdx : z_idx].getStrafeDir() != StrafeDirection.NONE;
        thisMove.strafeImpulse = inputs[isPredictable ? inputsIdx : z_idx].getStrafeDir();
        thisMove.forwardImpulse = inputs[isPredictable ? inputsIdx : x_idx].getForwardDir();
        if (debug) {
            player.sendMessage("[SurvivalFly] (postPredict) " + (!isPredictable ? "Uncertain" : "Predicted") + " direction: " + inputs[isPredictable ? inputsIdx : x_idx].getForwardDir() +" | "+ inputs[isPredictable ? inputsIdx : x_idx].getStrafeDir());
        }
    }
    
    
    /**
     * Relative (to workarounds) horizontal distance checking.
     *
     * @param forceSetOnGround Set to true on specific occasions where we need to re-estimate speed with the ground status, in case the player fails the check.
     * @param forceSetOffGround Set to true on specific occasions where we want to run the prediction without taking the ground status into consideration.
     *
     * @return hAllowedDistance, hDistanceAboveLimit
     */
    // TODO: Naming convention. Doesn't make much sense currently as we're only using a single workaround for entity pushing.
    private double[] hDistRel(final PlayerLocation from, final PlayerLocation to, final IPlayerData pData, final Player player,
                              final MovingData data, final PlayerMoveData thisMove, final PlayerMoveData lastMove,
                              final boolean fromOnGround, final boolean toOnGround, final boolean debug,
                              final boolean isNormalOrPacketSplitMove, boolean forceSetOnGround, boolean forceSetOffGround) {
        boolean onGround = !forceSetOffGround && (from.isOnGround() || lastMove.toIsValid && lastMove.yDistance <= 0.0 && lastMove.from.onGround || forceSetOnGround);
        double hDistanceAboveLimit = 0.0;

        //////////////////////////////////////////////////////////////
        // Estimate the horizontal speed (per-move distance check)  //                      
        //////////////////////////////////////////////////////////////
        // Determine inertia and acceleration to calculate speed with.
        if (isNormalOrPacketSplitMove) {
            // Only check 'from' because it is the current location of the player (NMS-wise)
            if (from.isInWater()) {
                data.nextInertia = Bridge1_13.isSwimming(player) ? Magic.HORIZONTAL_SWIMMING_INERTIA : Magic.WATER_HORIZONTAL_INERTIA;
                /* Per-tick speed gain. */
                float acceleration = Magic.LIQUID_ACCELERATION;
                float StriderLevel = (float) BridgeEnchant.getDepthStriderLevel(player);
                if (!onGround) {
                    StriderLevel *= Magic.STRIDER_OFF_GROUND_MULTIPLIER;
                }
                if (StriderLevel > 0.0) {
                    // (Less speed conservation (or in other words, more friction))
                    data.nextInertia += (0.54600006f - data.nextInertia) * StriderLevel / 3.0f;
                    // (More per-tick speed gain)
                    acceleration += (data.walkSpeed - acceleration) * StriderLevel / 3.0f;
                }
                if (!Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(player))) {
                    // (Much more speed conservation (or in other words, much less friction))
                    // (Overrides swimming AND depth strider friction)
                    data.nextInertia = Magic.DOLPHIN_GRACE_INERTIA;
                }
                // Run through all operations
                processNextSpeed(player, acceleration, pData, tags, to, from, debug, fromOnGround, toOnGround, onGround);
            }
            else if (from.isInLava()) {
                data.nextInertia = Magic.LAVA_HORIZONTAL_INERTIA;
                processNextSpeed(player, Magic.LIQUID_ACCELERATION, pData, tags, to, from, debug, fromOnGround, toOnGround, onGround);
            }
            else {
                data.nextInertia = onGround ? data.nextFrictionHorizontal * Magic.HORIZONTAL_INERTIA
                                            : Magic.HORIZONTAL_INERTIA;
                // 1.12 (and below) clients will use cubed inertia, not cubed friction here. The difference isn't significant except for blocking speed and bunnyhopping on soul sand, which are both slower on 1.8
                float acceleration = onGround ? data.walkSpeed * ((pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) ? Magic.DEFAULT_FRICTION_CUBED : Magic.CUBED_INERTIA) / (data.nextFrictionHorizontal * data.nextFrictionHorizontal * data.nextFrictionHorizontal))
                                              : Magic.AIR_ACCELERATION;
                if (pData.isSprinting()) {
                    // (We don't use the attribute here due to desync issues, just detect when the player is sprinting and apply the multiplier manually)
                    acceleration += acceleration * 0.3f; // 0.3 is the effective sprinting speed (EntityLiving).
                }
                processNextSpeed(player, acceleration, pData, tags, to, from, debug, fromOnGround, toOnGround, onGround);
            }
        }
        else {
            // Bukkit-based split move: predicting the next speed is not possible due to coordinates not being reported correctly by Bukkit (and without ProtocolLib, it's nearly impossible to achieve precision here)
            // (Technically, one could attempt to interpolate missed positions given the from and to locations... Just like for the 0.03 threshold, but what's the point? We are not paid to deal with this bullshit).
            // Besides, there's no need in predicting speed when movement has been slowed down to the point of being considered micro by Bukkit.
            thisMove.xAllowedDistance = thisMove.xDistance;
            thisMove.zAllowedDistance = thisMove.zDistance;
        }

        thisMove.hAllowedDistance = MathUtil.dist(thisMove.xAllowedDistance, thisMove.zAllowedDistance);
        /* Expected difference from current to allowed */
        final double offset = thisMove.hDistance - thisMove.hAllowedDistance;
        if (offset < Magic.PREDICTION_EPSILON) {
            // Accuracy margin.
        }
        else {
            hDistanceAboveLimit = Math.max(hDistanceAboveLimit, offset);
            tags.add("hdistrel");
        }
        if (debug) player.sendMessage("c/e: " + StringUtil.fdec6.format(thisMove.hDistance) + " / " + StringUtil.fdec6.format(thisMove.hAllowedDistance));

        return new double[]{thisMove.hAllowedDistance, hDistanceAboveLimit};
    }


    /**
     * Relative (to workarounds) vertical distance checking.
     *
     * @param forceResetMomentum Whether the check should start with 0.0 speed on applying air friction.
     */
    private double[] vDistRel(final long now, final Player player, final PlayerLocation from,
                              final boolean fromOnGround, final boolean resetFrom, final PlayerLocation to,
                              final boolean toOnGround, final boolean resetTo,
                              final double yDistance, boolean isNormalOrPacketSplitMove,
                              final PlayerMoveData lastMove,
                              final MovingData data, final MovingConfig cc, final IPlayerData pData,
                              boolean forceResetMomentum) {
        double yDistanceAboveLimit = 0.0;
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final boolean yDirectionSwitch = lastMove.toIsValid && lastMove.yDistance != yDistance && (yDistance <= 0.0 && lastMove.yDistance >= 0.0 || yDistance >= 0.0 && lastMove.yDistance <= 0.0);
        /* Not on ground, not on climbable, not in liquids, not in stuck-speed, no lostground (...) */
        final boolean fullyInAir = !thisMove.touchedGroundWorkaround && !resetFrom && !resetTo;

        ///////////////////////////////////////////////////////////////////////////////////
        // Estimate the allowed yDistance (per-move distance check)                      //
        ///////////////////////////////////////////////////////////////////////////////////
        // Stepping and jumping have priority, due to both being a potential starting point for the move.
        if (PlayerEnvelopes.isStepUpByNCPDefinition(pData, fromOnGround, toOnGround)) {
            thisMove.yAllowedDistance = thisMove.yDistance;
            thisMove.isStepUp = true;
            tags.add("step_env");
        }
        else if (PlayerEnvelopes.isJump(from, to, player, fromOnGround, toOnGround)) {
            thisMove.yAllowedDistance = thisMove.yDistance;
            thisMove.isJump = true;
            data.jumpDelay = Magic.MAX_JUMP_DELAY;
            tags.add("jump_env");
        }
        else if (from.isInPowderSnow() && thisMove.yDistance > 0.0
                && BridgeMisc.hasLeatherBootsOn(player) && !thisMove.hasLevitation) {
            // Climbing inside powder snow. Set a limit because speed is throttled anyway (and I can't figure out how the game handles ascending speed here) 
            thisMove.yAllowedDistance = Magic.snowClimbSpeedAscend;
        }
        else {
            // Otherwise, a fully in-air move (friction).
            // Initialize with momentum (or lack thereof)
            thisMove.yAllowedDistance = forceResetMomentum ? 0.0 : (lastMove.toIsValid ? lastMove.yDistance : 0.0);
            
            //////////////////////////////////////
            // Next client-tick/move            //
            //////////////////////////////////////
            // NOTE: pressing space bar on a bouncy block will override the bounce (in that case, vdistrel will fall back to the jump check above).
            // updateEntityAfterFallOn(), this function is called on the next move
            if (!player.isSneaking() && lastMove.collideY) { // TODO: Cannot use flags from 1.20 and onwards, needs the mainSupportingBlock method.
                if (lastMove.yAllowedDistance < 0.0) { // NOTE: Must be the allowed distance, not the actual one (exploit)
                    if (lastMove.to.onBouncyBlock) {
                        // The effect works by inverting the distance.
                        // Beds have a weaker bounce effect (BedBlock.java).
                        thisMove.yAllowedDistance = lastMove.to.onSlimeBlock ? -lastMove.yAllowedDistance : -lastMove.yAllowedDistance * 0.6600000262260437;
                    }
                    tags.add("bounceup");
                }
            }
            if (from.isSlidingDown() && !thisMove.hasLevitation) {
                // Speed is static in this case
                thisMove.yAllowedDistance = -Magic.SLIDE_SPEED_THROTTLE;
            }
            if (TrigUtil.lengthSquared(data.lastStuckInBlockHorizontal, data.lastStuckInBlockVertical, data.lastStuckInBlockHorizontal) > 1.0E-7) {
                if (data.lastStuckInBlockVertical != 1.0) {
                    // Throttle speed when stuck in
                    thisMove.yAllowedDistance = 0.0;
                }
            }
            if (Math.abs(thisMove.yAllowedDistance) < (pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? Magic.NEGLIGIBLE_SPEED_THRESHOLD : Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY)) {
                // Negligible speed reset.
                thisMove.yAllowedDistance = 0.0;
            }
            if (lastMove.hasLevitation) { 
                // Levitation forces players to ascend and does not work in liquids, so thankfully we don't have to account for that, other than stuck-speed.
                thisMove.yAllowedDistance += (0.05 * data.lastLevitationLevel - lastMove.yAllowedDistance) * 0.2;
            }
            else if (lastMove.hasGravity) {
                // Slowfall simply reduces gravity. Introduced in 1.13
                thisMove.yAllowedDistance -= lastMove.hasSlowfall && lastMove.yDistance <= 0.0 ? Magic.SLOW_FALL_GRAVITY : Magic.DEFAULT_GRAVITY;
            }
            // Friction.
            // NOTE: Unlike horizontal friction, vertical friction is not assigned to the "f" variable (at the beginning of the travel() function in EntityLiving)
            thisMove.yAllowedDistance *= data.lastFrictionVertical;


            //////////////////////////////////
            // Last client-tick/move        //
            //////////////////////////////////
            // Stuck-speed with the updated multiplier
            if (TrigUtil.lengthSquared(data.nextStuckInBlockHorizontal, data.nextStuckInBlockVertical, data.nextStuckInBlockHorizontal) > 1.0E-7) {
                thisMove.yAllowedDistance *= data.nextStuckInBlockVertical;
            }
            // TODO: Needs to be adjusted for on ground pushing
            if (lastMove.slowedByUsingAnItem && !thisMove.slowedByUsingAnItem && thisMove.isRiptiding) {
                // Riptide works by propelling the player in air after releasing the trident (the effect only pushes the player, unless is on ground)
                thisMove.yAllowedDistance += to.getTridentPropellingForce(player, lastMove.collideY).getY();
                player.sendMessage("Trident propel(v): " + StringUtil.fdec6.format(thisMove.yDistance) + " / " + StringUtil.fdec6.format(thisMove.yAllowedDistance));
            }
            // Collision next.
            // Include horizontal motion to account for stepping: there are cases where NCP's isStep definition fails to catch it.
            // (In which case, isStep will return false and fall-back to friction here)
            // It is imperative that you pass yAllowedDistance as argument here (not the real yDistance), because if the player isn't on ground, the current motion will be used to determine it (collideY && motionY < 0.0). Passing an uncontrolled yDistance will be easily exploitable.
            Vector collisionVector = from.collide(new Vector(thisMove.xAllowedDistance, thisMove.yAllowedDistance, thisMove.zAllowedDistance), fromOnGround || thisMove.touchedGroundWorkaround, cc, from.getAABBCopy());
            thisMove.headObstructed = thisMove.yAllowedDistance != collisionVector.getY() && thisMove.yDistance >= 0.0 && from.seekHeadObstruction() && !fromOnGround;  // New definition of head obstruction: yDistance is checked because Minecraft considers players to be on ground when motion is explicitly negative
            // Switch to descending phase after colliding above.
            if (lastMove.headObstructed && !thisMove.headObstructed && yDirectionSwitch && thisMove.yDistance <= 0.0 && fullyInAir) {
                // Fix for clients not sending the "speed-reset move" to the server: player collides vertically with a ceiling, then proceeds to descend.
                // Normally, speed is set back to 0.0 and then gravity is applied. The former move however is never actually sent: what we see on the server-side is the player immediately descending, but with speed that is still based on a previous move of 0.0 speed.
                thisMove.yAllowedDistance = 0.0; // Simulate what the client should be doing and re-iterate gravity
                if (BridgeMisc.hasGravity(player)) {
                    thisMove.yAllowedDistance -= !Double.isInfinite(Bridge1_13.getSlowfallingAmplifier(player)) && lastMove.yDistance <= 0.0 ? Magic.SLOW_FALL_GRAVITY : Magic.DEFAULT_GRAVITY;
                }
                thisMove.yAllowedDistance *= data.lastFrictionVertical;
                tags.add("gravity_reiterate");
            }
            else thisMove.yAllowedDistance = collisionVector.getY();
            // If this vertical move resulted in a collision, remember it.
            thisMove.collideY = collisionVector.getY() != thisMove.yAllowedDistance;
        }
        // Check for workarounds at the end and override the prediction if needed (just allow the movement in this case.)
        if (AirWorkarounds.checkPostPredictWorkaround(data, fromOnGround, toOnGround, from, to, thisMove.yAllowedDistance, player, isNormalOrPacketSplitMove)) {
            thisMove.yAllowedDistance = thisMove.yDistance;
            if (pData.isDebugActive(type)) {
                player.sendMessage("Workaround ID: " + (!justUsedWorkarounds.isEmpty() ? StringUtil.join(justUsedWorkarounds, " , ") : ""));
            }
        }

        /* Expected difference from current to allowed */
        final double offset = thisMove.yDistance - thisMove.yAllowedDistance;
        if (Math.abs(offset) < Magic.PREDICTION_EPSILON) {
            // Accuracy margin.
        }
        else {
            // If velocity can be used for compensation, use it.
            if (data.getOrUseVerticalVelocity(yDistance) == null) {
                yDistanceAboveLimit = Math.max(yDistanceAboveLimit, Math.abs(offset));
                tags.add("vdistrel");
            }
        }


        // (All the checks below are to be considered a corollary to vDistRel. Virtually, the cheat attempt will always get caught by vDistRel first)
        ///////////////////////////////////////////
        // Check on change of Y direction        //
        ///////////////////////////////////////////
        if (fullyInAir && yDirectionSwitch && !(data.timeRiptiding + 2000 > now)) {
            if (yDistance > 0.0) {
                // TODO: Demand consuming queued velocity for valid change (!).
                if (lastMove.touchedGround || lastMove.to.extraPropertiesValid && lastMove.to.resetCond) {
                    // Change to increasing phase
                    tags.add("y_switch_inc");
                }
                else {
                    // Moving upwards after falling without having touched the ground.
                    if (data.jumpDelay < 9 && !((lastMove.touchedGround || lastMove.from.onGroundOrResetCond) && lastMove.yDistance == 0D)
                        && data.getOrUseVerticalVelocity(yDistance) == null && !thisMove.hasLevitation) {
                        Improbable.check(player, (float) Math.min(1.0, Math.abs(yDistance)), System.currentTimeMillis(), "moving.survivalfly.airjump", pData);
                        yDistanceAboveLimit = Math.max(yDistanceAboveLimit, Math.max(1.0, Math.abs(yDistance))); // Ensure that players cannot get a VL of 0.
                        tags.add("airjump");
                    }
                    else tags.add("y_inair_switch");
                }
            }
            else {
                // Change to decreasing phase.
                tags.add("y_switch_dec");
                // (Previously: a low-jump check would have been performed here, let vDistRel catch it instead.)
            }
        }
        return new double[]{thisMove.yAllowedDistance, yDistanceAboveLimit};
    }


    /**
     * After failure checks of vertical distance.
     *
     * @return yDistanceAboveLimit, yAllowedDistance.
     */
    private double[] vDistAfterFailure(final Player player, final IPlayerData pData, final MovingData data, boolean fromOnGround, boolean toOnGround,
                                       final PlayerLocation from, final PlayerLocation to, boolean isNormalOrPacketSplitMove,
                                       boolean resetFrom, boolean resetTo, final PlayerMoveData thisMove, final PlayerMoveData lastMove, double yDistanceAboveLimit, double yAllowedDistance,
                                       boolean useBlockChangeTracker) {
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        /*
         * 0: Vertical push/pull is put on top priority
         */
        if (useBlockChangeTracker) {
            double[] res = getVerticalBlockMoveResult(thisMove.yDistance, from, to, data);
            if (res != null) {
                yAllowedDistance = res[0];
                yDistanceAboveLimit = res[1];
                // Nothing else to do here. (skip all other after-failure stuff)
                return new double[]{yAllowedDistance, yDistanceAboveLimit};
            }
        }

        // Don't test if gliding or riptiding with the stuff below.
        if (Bridge1_9.isGliding(player) || Bridge1_13.isRiptiding(player) || from.isInLiquid() || from.isOnClimbable()) {
            return new double[]{yAllowedDistance, yDistanceAboveLimit};
        }

        /*
         * 1: Attempt to simulate the reset of speed that the client should have sent to the server.
         * [Client lands on the ground but does not come to a "rest" on top of the block (and thus, reset the vertical speed), instead they'll immediately descend right after, but with speed that is still based on a previous move of 0.0]
         * Can be noticed when stepping down stair of slabs or breaking blocks below.
         * See: https://gyazo.com/0f748030296aebc0484564629abe6864
         * After interpolating the ground status, notice how the player immediately proceeds to descend with speed as if they actually landed on the ground with the previous move (-0.0784)
         */
        if (!thisMove.couldStepUp && thisMove.yDistance < 0.0 && yDistanceAboveLimit > 0.0 && (lastMove.toLostGround || lastMove.to.onGround) && !thisMove.from.onGround) {
            // After completing a "touch-down", the next move should always come from ground
            // Thus, such cases can be generalised by checking for negative motion and last move landing on ground but this move not *starting back* from a ground position.
            double[] res = vDistRel(System.currentTimeMillis(), player, from, fromOnGround, resetFrom, to, toOnGround, resetTo, thisMove.yDistance, isNormalOrPacketSplitMove, lastMove, data, cc, pData, true);
            yAllowedDistance = res[0];
            yDistanceAboveLimit = res[1];
        }
        return new double[]{yAllowedDistance, yDistanceAboveLimit};
    }


    /**
     * After-horizontal-failure checks.
     *
     * @return hAllowedDistance, hDistanceAboveLimit, hFreedom
     */
    private double[] hDistAfterFailure(final Player player,
                                       final PlayerLocation from, final PlayerLocation to,
                                       double hAllowedDistance, double hDistanceAboveLimit,
                                       final PlayerMoveData thisMove, final PlayerMoveData lastMove, final boolean debug,
                                       final MovingData data, final MovingConfig cc, final IPlayerData pData, final int tick,
                                       boolean useBlockChangeTracker, final boolean fromOnGround, final boolean toOnGround,
                                       final boolean isNormalOrPacketSplitMove) {
        /*
         * 0: If we got a speed violation and the player is using an item, assume it to be a "noslowdown" violation.
         */
        if (cc.survivalFlyResetItem && BridgeMisc.isUsingItem(player) && !Bridge1_9.isGliding(player)) {
            // Forcibly release the item in use.
            pData.requestItemUseResync();
            tags.add("itemresync");
            if (!BridgeMisc.isUsingItem(player) && hDistanceAboveLimit > 0.0) {
                // Re-estimate with released item (if it still throws a VL, the player is actually cheating, if the item is still in use, then it wasn't desync'ed).
                double[] res = hDistRel(from, to, pData, player, data, thisMove, lastMove, fromOnGround, toOnGround, debug, isNormalOrPacketSplitMove, false, false);
                hAllowedDistance = res[0];
                hDistanceAboveLimit = res[1];
            }
        }
        /*
         * 1: Player failed with a concurrent lost ground case: force-set the ground status and re-estimate.
         * TODO: Not good. Performance will be affected, as we already brute force the player's input with horizontal motion.
         *  Confine lostground-use with horizontal motion further, so that we don't need to brute force this as well.
         */
        if ((thisMove.touchedGroundWorkaround || lastMove.toIsValid && lastMove.yDistance <= 0.0 && lastMove.touchedGroundWorkaround) && hDistanceAboveLimit > 0.0) {
            double[] res = hDistRel(from, to, pData, player, data, thisMove, lastMove, fromOnGround, toOnGround, debug, isNormalOrPacketSplitMove, true, false);
            hAllowedDistance = res[0];
            hDistanceAboveLimit = res[1];
        }
        /*
         * 2: Undetectable jump (must brute force here): player failed with the onGround flag, lets try with off-ground then.
         */
        if (PlayerEnvelopes.isVerticallyConstricted(from, to, pData) && hDistanceAboveLimit > 0.0) {
            double[] res = hDistRel(from, to, pData, player, data, thisMove, lastMove, fromOnGround, toOnGround, debug, isNormalOrPacketSplitMove, false, true);
            hAllowedDistance = res[0];
            hDistanceAboveLimit = res[1];
        }
        /*
         * 3: Specific issue with slime speed: the client tries to fall down with gravity -0.0784, and then bounce back up to 0 >=. Ground status is set to false then.
         *  However, we don't see this on server; we always see the player as being on ground with 0 dist.
         */
        if (from.isOnSlimeBlock() && hDistanceAboveLimit > 0.0) {
            double[] res = hDistRel(from, to, pData, player, data, thisMove, lastMove, fromOnGround, toOnGround, debug, isNormalOrPacketSplitMove, false, true);
            hAllowedDistance = res[0];
            hDistanceAboveLimit = res[1];
        }
        /*
         * 4: Above limit again? Check for past onGround states caused by block changes (i.e.: ground was pulled off from the player's feet)
         */
        if (useBlockChangeTracker && hDistanceAboveLimit > 0.0) {
            // Be sure to test this only if the player is seemingly off ground
            if (!thisMove.touchedGroundWorkaround && !from.isOnGround() && from.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, data.blockChangeRef, tick)) {
                tags.add("blockchange_h");
                double[] res = hDistRel(from, to, pData, player, data, thisMove, lastMove, fromOnGround, toOnGround, debug, isNormalOrPacketSplitMove, true, false);
                hAllowedDistance = res[0];
                hDistanceAboveLimit = res[1];
            }
        }
        /* 
         * 5: Distance is still above limit; last resort: check if the distance above limit can be covered with velocity
         */
        // TODO: Implement Asofold's fix to prevent too easy abuse:
        // See: https://github.com/NoCheatPlus/Issues/issues/374#issuecomment-296172316
        double hFreedom = 0.0; // Horizontal velocity used.
        if (hDistanceAboveLimit > 0.0) {
            hFreedom = data.getHorizontalFreedom();
            if (hFreedom < hDistanceAboveLimit) {
                // Distance above limit is still greater. Try using queued velocity if possible.
                hFreedom += data.useHorizontalVelocity(hDistanceAboveLimit - hFreedom);
            }
            if (hFreedom > 0.0) {
                tags.add("hvel");
                hDistanceAboveLimit = Math.max(0.0, hDistanceAboveLimit - hFreedom);
            }
        }
        return new double[]{hAllowedDistance, hDistanceAboveLimit, hFreedom};
    }


    /**
     * Inside liquids vertical speed checking.<br> setFrictionJumpPhase must be set
     * externally.
     * TODO: Unify with vDistRel, model after client behaviour like the rest
     *
     * @return yAllowedDistance, yDistanceAboveLimit
     */
    private double[] vDistLiquid(final PlayerMoveData thisMove, final PlayerLocation from, final PlayerLocation to,
                                 final boolean toOnGround, final double yDistance, final PlayerMoveData lastMove,
                                 final MovingData data, final Player player, final MovingConfig cc, final IPlayerData pData, final boolean fromOnGround) {
        final double yDistAbs = Math.abs(yDistance);
        /* If a server with version lower than 1.13 has ViaVer installed, allow swimming */
        final boolean swimmingInLegacyServer = player.isSprinting() && pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13);
        final double baseSpeed = thisMove.from.onGround ? Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player) || swimmingInLegacyServer) + 0.1 : Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player) || swimmingInLegacyServer);
        /* Slow fall gravity is applied only if the player is not sneaking (in that case, the player will descend in water with regular gravity) */
        final boolean Slowfall = !pData.isCrouching() && thisMove.hasSlowfall;

        //////////////////////////////////////////////////////
        // 0: Checks for no gravity when moving in a liquid.//
        //////////////////////////////////////////////////////
        if (thisMove.yDistance == 0.0 && lastMove.yDistance == 0.0 && lastMove.toIsValid
            && thisMove.hDistance >= 0.090 && lastMove.hDistance >= 0.090 // Do not check lower speeds. The cheat would be purely cosmetic at that point, it wouldn't offer any advantage.
            && BlockProperties.isLiquid(to.getTypeId())
            && BlockProperties.isLiquid(from.getTypeId())
            && !fromOnGround && !toOnGround
            && !from.seekHeadObstruction() && !to.seekHeadObstruction()
            && !Bridge1_13.isSwimming(player)) {
            Improbable.feed(player, (float) thisMove.hDistance, System.currentTimeMillis());
            tags.add("liquidwalk");
            return new double[]{0.0, yDistance};
        }

        ////////////////////////////
        // 1: Minimal speed.     //
        ///////////////////////////
        if (yDistAbs <= baseSpeed) {
            return new double[]{baseSpeed, 0.0};
        }

        /////////////////////////////////////////////////////////
        // 2: Vertical checking for waterlogged blocks 1.13+   //
        /////////////////////////////////////////////////////////
        if (from.isOnGround() && !BlockProperties.isLiquid(from.getTypeIdAbove())
            && from.isInWaterLogged()
            && !from.isInBubbleStream() && !thisMove.headObstructed
            && !from.isSubmerged(0.75)) {
            // (Envelope change shouldn't be done here but, eh.)
            data.liftOffEnvelope = LiftOffEnvelope.NORMAL;
            final double minJumpGain = data.liftOffEnvelope.getJumpGain(data.jumpAmplifier);
            // Allow stepping.
            final boolean step = (toOnGround || thisMove.to.resetCond) && yDistance > minJumpGain && yDistance <= cc.sfStepHeight;
            final double yAllowedDistance = step ? cc.sfStepHeight : minJumpGain;
            tags.add("liquidground");
            return new double[]{yAllowedDistance, yDistance - yAllowedDistance};
        }


        /////////////////////////////////////////////////////////
        // 3: Friction envelope (allow any kind of slow down). //
        ////////////////////////////////////////////////////////
        final double frictDist = lastMove.toIsValid ? Math.abs(lastMove.yDistance) * data.lastNonVanillaFrictionVertical : baseSpeed; // Bounds differ with sign.
        if (lastMove.toIsValid) {
            if (lastMove.yDistance < 0.0 && yDistance < 0.0
                && yDistAbs < frictDist + (Slowfall ? Magic.SLOW_FALL_GRAVITY : Magic.GRAVITY_MAX + Magic.GRAVITY_SPAN)) {
                // (Descend speed depends on how fast one dives in)
                tags.add("frictionenv(desc)");
                return new double[]{-frictDist - (Slowfall ? Magic.SLOW_FALL_GRAVITY : Magic.GRAVITY_MAX - Magic.GRAVITY_SPAN), 0.0};
            }
            if (lastMove.yDistance > 0.0 && yDistance > 0.0 && yDistance < frictDist - Magic.GRAVITY_SPAN) {
                tags.add("frictionenv(asc)");
                return new double[]{frictDist - Magic.GRAVITY_SPAN, 0.0};
            }
            // ("== 0.0" is covered by the minimal speed check above.)
        }


        ////////////////////////////////////
        // 4: Handle bubble columns 1.13+ // 
        ////////////////////////////////////
        // TODO: bubble columns adjacent to each other: push prevails on drag. So do check adjacent blocks and see if they are draggable as well.
        //[^ Problem: we only get told if the column can drag players]
        if (from.isInBubbleStream() || to.isInBubbleStream()) {
            // Minecraft distinguishes above VS inside, so we need to do it as well.
            // This is what it does to determine if the player is above the stream (check for air above)
            tags.add("bblstrm_asc");
            if (BlockProperties.isAir(from.getTypeIdAbove())) {
                double yAllowedDistance = Math.min(1.8, lastMove.yDistance + 0.2);
                return new double[]{yAllowedDistance, yDistAbs - yAllowedDistance};
            }
            // Inside.
            if (lastMove.yDistance < 0.0 && yDistance < 0.0 && !thisMove.headObstructed) {
                // == 0.0 is covered by waterwalk
                // If inside a bubble column, and the player is getting pushed, they cannot descend
                return new double[]{0.0, yDistAbs};
            }
            double yAllowedDistance = Math.min(Magic.bubbleStreamAscend, lastMove.yDistance + 0.2);
            return new double[]{yAllowedDistance, yDistance - yAllowedDistance};
        }
        // Check for drag
        // (Seems to work OK, unlike ascending)
        if (from.isDraggedByBubbleStream() || to.isDraggedByBubbleStream()) {
            tags.add("bblstrm_desc");
            // Above
            if (BlockProperties.isAir(from.getTypeIdAbove())) {
                // -0.03 is the effective acceleration, capped at -0.9
                double yAllowedDistance = Math.max(-0.9, lastMove.yDistance - 0.03);
                return new double[]{yAllowedDistance, yDistance < yAllowedDistance ? Math.abs(yDistance - yAllowedDistance) : 0.0};
            }
            // Inside
            if (lastMove.yDistance > 0.0 && yDistance > 0.0) {
                // If inside a bubble column, and the player is getting dragged, they cannot ascend
                return new double[]{0.0, yDistAbs};
            }
            double yAllowedDistance = Math.max(-0.3, lastMove.yDistance - 0.03);
            return new double[]{yAllowedDistance, yDistance < yAllowedDistance ? Math.abs(yDistance - yAllowedDistance) : 0.0};
        }


        ///////////////////////////////////////
        // 5: Workarounds for special cases. // 
        ///////////////////////////////////////
        final Double wRes = LiquidWorkarounds.liquidWorkarounds(player, from, to, baseSpeed, frictDist, lastMove, data);
        if (wRes != null) {
            return new double[]{wRes, 0.0};
        }


        ///////////////////////////////////////////////
        // 6: Try to use velocity for compensation.  //
        ///////////////////////////////////////////////
        if (data.getOrUseVerticalVelocity(yDistance) != null) {
            return new double[]{yDistance, 0.0};
        }


        ///////////////////////////////////
        // 7: At this point a violation. //
        //////////////////////////////////
        tags.add(yDistance < 0.0 ? "swimdown" : "swimup");
        // Can't ascend in liquid if sneaking.
        if (pData.isCrouching()
                // (Clearly ascending)
                && yDistance > 0.0 && lastMove.yDistance > 0.0 && yDistance >= lastMove.yDistance) {
            return new double[]{0.0, yDistance};
        }
        final double vl1 = yDistAbs - baseSpeed;
        final double vl2 = Math.abs(
                yDistAbs - frictDist -
                        (yDistance < 0.0 ? (Slowfall ? Magic.SLOW_FALL_GRAVITY : Magic.GRAVITY_MAX + Magic.GRAVITY_SPAN) : Magic.GRAVITY_MIN)
        );
        if (vl1 <= vl2) return new double[]{yDistance < 0.0 ? -baseSpeed : baseSpeed, vl1};
        return new double[]{yDistance < 0.0 ? -frictDist - (Slowfall ? Magic.SLOW_FALL_GRAVITY : Magic.GRAVITY_MAX - Magic.GRAVITY_SPAN)
                : frictDist - Magic.GRAVITY_SPAN, vl2};
    }


    /**
     * Simple (limit-based, non-predictive) on-climbable vertical distance checking.
     * Handled in a separate method to reduce the complexity of vDistRel workarounds.
     *
     * @return yAllowedDistance, yDistanceAboveLimit
     */
    private double[] vDistClimbable(final Player player, final PlayerLocation from, final PlayerLocation to,
                                    final IPlayerData pData,
                                    final PlayerMoveData thisMove, final PlayerMoveData lastMove,
                                    final double yDistance, final MovingData data, final MovingConfig cc) {
        data.clearActiveHorVel(); // Might want to clear ALL horizontal vel.
        double yDistanceAboveLimit = 0.0;
        double yDistAbs = Math.abs(yDistance);
        final double maxJumpGain = data.liftOffEnvelope.getJumpGain(data.jumpAmplifier) + 0.0001;
        /* Climbing a ladder in water and exiting water for whatever reason speeds up the player a lot in that one transition ... */
        final boolean waterStep = lastMove.from.inLiquid && yDistAbs < Magic.swimBaseSpeedV(Bridge1_13.hasIsSwimming());
        // Quick, temporary fix for scaffolding block
        final boolean scaffolding = from.isOnGround() && from.getBlockY() == Location.locToBlock(from.getY()) && yDistance > 0.0 && yDistance < maxJumpGain;
        double yAllowedDistance = (waterStep || scaffolding) ? yDistAbs : yDistance < 0.0 ? Magic.climbSpeedDescend : Magic.climbSpeedAscend;
        final double maxJumpHeight = LiftOffEnvelope.NORMAL.getMaxJumpHeight(0.0) + (data.jumpAmplifier > 0 ? (0.6 + data.jumpAmplifier - 1.0) : 0.0);

        // Workaround for ladders that have a much bigger collision box, but much smaller hitbox (We do not distinguish the two). 
        if (yDistAbs > yAllowedDistance) {
            if (from.isOnGround(BlockFlags.F_CLIMBABLE) && to.isOnGround(BlockFlags.F_CLIMBABLE)) {
                // Stepping up a block (i.e.: stepping up stairs with vines/ladders on the side), allow this movement.
                yDistanceAboveLimit = Math.max(yDistanceAboveLimit, yDistAbs - cc.sfStepHeight);
                tags.add("climbstep");
            }
            else if (from.isOnGround(maxJumpHeight, 0D, 0D, BlockFlags.F_CLIMBABLE)) {
                // If ground is found within the allowed jump height, we check speed against jumping motion not climbing motion, in order to still catch extremely fast / instant modules.
                // (We have to check for full height because otherwise the immediate move after jumping will yield a false positive, as air gravity will be applied, which is stll higher than climbing motion)
                // In other words, this means that as long as ground is found within the allowed height, players will be able to speed up to 0.42 on climbables.
                // The advantage is pretty insignificant while granting us some leeway with false positives and cross-versions compatibility issues.
                if (yDistance > maxJumpGain) {
                    // If the player managed to exceed the ordinary jumping motion while on a climbable, we know for sure they're cheating.
                    yDistanceAboveLimit = Math.max(yDistanceAboveLimit, yDistAbs - yAllowedDistance);
                    Improbable.check(player, (float)yDistAbs, System.currentTimeMillis(), "moving.survivalfly.instantclimb", pData);
                    tags.add("instantclimb");
                }
                // Ground is within reach and speed is lower than 0.42. Allow the movement.
                else tags.add("climbheight("+ StringUtil.fdec3.format(maxJumpHeight) +")");
            }
            else {
                // Ground is out reach and motion is still higher than legit. We can safely throw a VL at this point.
                yDistanceAboveLimit = Math.max(yDistanceAboveLimit, yDistAbs - yAllowedDistance);
                tags.add("climbspeed");
            }
        }

        // Can't climb up with vine not attached to a solid block (legacy).
        if (yDistance > 0.0 && !thisMove.touchedGround && !from.canClimbUp(maxJumpHeight)) {
            yDistanceAboveLimit = Math.max(yDistanceAboveLimit, yDistance);
            yAllowedDistance = 0.0;
            tags.add("climbdetached");
        }

        // Do allow friction with velocity.
        // TODO: Actual friction or limit by absolute y-distance?
        // TODO: Looks like it's only a problem when on ground?
        if (yDistanceAboveLimit > 0.0 && thisMove.yDistance > 0.0 
            && lastMove.yDistance - (Magic.GRAVITY_MAX + Magic.GRAVITY_MIN) / 2.0 > thisMove.yDistance) {
            yDistanceAboveLimit = 0.0;
            yAllowedDistance = yDistance;
            tags.add("vfrict_climb");
        }

        // Do allow vertical velocity.
        // TODO: Looks like less velocity is used here (normal hitting 0.361 of 0.462).
        if (yDistanceAboveLimit > 0.0 && data.getOrUseVerticalVelocity(yDistance) != null) {
            yDistanceAboveLimit = 0.0;
        }
        return new double[]{yAllowedDistance, yDistanceAboveLimit};
    }

    /**
     * Violation handling put here to have less code for the frequent processing of check.
     *
     * @return The Location where the player will be set backed to.
     */
    private Location handleViolation(final double result,
                                     final Player player, final PlayerLocation from, final PlayerLocation to,
                                     final MovingData data, final MovingConfig cc) {
        // Increment violation level.
        data.survivalFlyVL += result;
        final ViolationData vd = new ViolationData(this, player, data.survivalFlyVL, result, cc.survivalFlyActions);
        if (vd.needsParameters()) {
            vd.setParameter(ParameterName.LOCATION_FROM, String.format(Locale.US, "%.2f, %.2f, %.2f", from.getX(), from.getY(), from.getZ()));
            vd.setParameter(ParameterName.LOCATION_TO, String.format(Locale.US, "%.2f, %.2f, %.2f", to.getX(), to.getY(), to.getZ()));
            vd.setParameter(ParameterName.DISTANCE, String.format(Locale.US, "%.2f", TrigUtil.distance(from, to)));
            vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
        }
        // Some resetting is done in MovingListener.
        if (executeActions(vd).willCancel()) {
            data.sfVLMoveCount = data.getPlayerMoveCount();
            // Set back + view direction of to (more smooth).
            return MovingUtil.getApplicableSetBackLocation(player, to.getYaw(), to.getPitch(), to, data, cc);
        }
        else {
            data.sfJumpPhase = 0;
            data.sfVLMoveCount = data.getPlayerMoveCount();
            // Cancelled by other plugin, or no cancel set by configuration.
            return null;
        }
    }


    /**
     * Hover violations have to be handled in this check, because they are handled as SurvivalFly violations (needs executeActions).
     */
    public final void handleHoverViolation(final Player player, final PlayerLocation loc, final MovingConfig cc, final MovingData data) {
        data.survivalFlyVL += cc.sfHoverViolation;
        // TODO: Extra options for set back / kick, like vl?
        data.sfVLMoveCount = data.getPlayerMoveCount();
        data.sfVLInAir = true;
        final ViolationData vd = new ViolationData(this, player, data.survivalFlyVL, cc.sfHoverViolation, cc.survivalFlyActions);
        if (vd.needsParameters()) {
            vd.setParameter(ParameterName.LOCATION_FROM, String.format(Locale.US, "%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ()));
            vd.setParameter(ParameterName.LOCATION_TO, "(HOVER)");
            vd.setParameter(ParameterName.DISTANCE, "0.0(HOVER)");
            vd.setParameter(ParameterName.TAGS, "hover");
        }
        if (executeActions(vd).willCancel()) {
            // Set back or kick.
            final Location newTo = MovingUtil.getApplicableSetBackLocation(player, loc.getYaw(), loc.getPitch(), loc, data, cc);
            if (newTo != null) {
                data.prepareSetBack(newTo);
                SchedulerHelper.teleportEntity(player, newTo, BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION);
            }
            else {
                // Solve by extra actions ? Special case (probably never happens)?
                player.kickPlayer("Hovering?");
            }
        }
        else {
            // Ignore.
        }
    }


    /**
     * Debug output.
     */
    private void outputDebug(final Player player, final PlayerLocation to, final PlayerLocation from,
                             final MovingData data,
                             final double hDistance, final double hAllowedDistance, final double hFreedom,
                             final double yDistance, final double yAllowedDistance,
                             final boolean fromOnGround, final boolean resetFrom,
                             final boolean toOnGround, final boolean resetTo,
                             final PlayerMoveData thisMove) {

        // TODO: Show player name once (!)
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final double yDistDiffEx = yDistance - yAllowedDistance;
        final double hDistDiffEx = thisMove.hDistance - thisMove.hAllowedDistance;
        final StringBuilder builder = new StringBuilder(500);
        builder.append(CheckUtils.getLogMessagePrefix(player, type));
        final String hVelUsed = hFreedom > 0 ? " / hVelUsed: " + StringUtil.fdec3.format(hFreedom) : "";
        builder.append("\nOnGround: " + (thisMove.headObstructed ? "(head obstr.) " : from.isSlidingDown() ? "(sliding down) " : "") + (thisMove.touchedGroundWorkaround ? "(lost ground) " : "") + (fromOnGround ? "onground -> " : (resetFrom ? "resetcond -> " : "--- -> ")) + (toOnGround ? "onground" : (resetTo ? "resetcond" : "---")) + ", jumpPhase: " + data.sfJumpPhase + ", LiftOff: " + data.liftOffEnvelope.name() + "(" + data.insideMediumCount + ")");
        final String dHDist = lastMove.toIsValid ? "(" + StringUtil.formatDiff(hDistance, lastMove.hDistance) + ")" : "";
        final String dYDist = lastMove.toIsValid ? "(" + StringUtil.formatDiff(yDistance, lastMove.yDistance)+ ")" : "";
        builder.append("\n" + " hDist: " + StringUtil.fdec6.format(hDistance) + dHDist + " / offset: " + hDistDiffEx + " / predicted: " + StringUtil.fdec6.format(hAllowedDistance) + hVelUsed +
                "\n" + " vDist: " + StringUtil.fdec6.format(yDistance) + dYDist + " / offset: " + yDistDiffEx + " / predicted: " + StringUtil.fdec6.format(yAllowedDistance) + " , setBackY: " + (data.hasSetBack() ? (data.getSetBackY() + " (jump height: " + StringUtil.fdec3.format(to.getY() - data.getSetBackY()) + " / max jump height: " + data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier) + ")") : "?"));
        if (lastMove.toIsValid) {
            builder.append("\n fdsq: " + StringUtil.fdec3.format(thisMove.distanceSquared / lastMove.distanceSquared));
        }
        if (!lastMove.toIsValid) {
            builder.append("\n Invalid last move (data reset)");
        }
        if (!lastMove.valid) {
            builder.append("\n Invalid last move (missing data)");
        }
        if (thisMove.verVelUsed != null) {
            builder.append(" , vVelUsed: " + thisMove.verVelUsed + " ");
        }
        data.addVerticalVelocity(builder);
        data.addHorizontalVelocity(builder);
        if (player.isSleeping()) {
            tags.add("sleeping");
        }
        if (Bridge1_9.isWearingElytra(player)) {
            // Just wearing (not isGliding).
            tags.add("elytra_off");
        }
        if (!tags.isEmpty()) {
            builder.append("\n" + " Tags: " + StringUtil.join(tags, "+"));
        }
        if (!justUsedWorkarounds.isEmpty()) {
            builder.append("\n" + " Workaround ID: " + StringUtil.join(justUsedWorkarounds, " , "));
        }
        builder.append("\n");
        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
    }


    private void logPostViolationTags(final Player player) {
        debug(player, "SurvivalFly Post violation handling tag update:\n" + StringUtil.join(tags, "+"));
    }
}