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
import org.bukkit.inventory.ItemStack;
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
import fr.neatmonster.nocheatplus.checks.moving.envelope.HorizontalUncertainty;
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
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeEnchant;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.math.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;

/**
 * The counterpart to the CreativeFly check. <br>People that are not allowed to fly get checked by this. <br>It will try to
 * identify when they are jumping, check if they aren't jumping too high or far, check if they aren't moving too fast on
 * normal ground, while sprinting, sneaking, swimming, etc.
 */
public class SurvivalFly extends Check {

    private final boolean ServerIsAtLeast1_13 = ServerVersion.compareMinecraftVersion("1.13") >= 0;
    /** To join some tags with moving check violations. */
    private final ArrayList<String> tags = new ArrayList<String>(15);
    private final ArrayList<String> justUsedWorkarounds = new ArrayList<String>();
    private final BlockChangeTracker blockChangeTracker;
    /** Location for temporary use with getLocation(useLoc). Always call setWorld(null) after use. Use LocUtil.clone before passing to other API. */
    private final Location useLoc = new Location(null, 0, 0, 0);    

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
     * @param player
     * @param from
     * @param to
     * @param multiMoveCount
     *            0: Ordinary, 1/2: first/second of a split move.
     * @param data
     * @param cc
     * @param pData
     * @param tick
     * @param now
     * @param useBlockChangeTracker
     * @param isNormalOrPacketSplitMove
     *           Flag to indicate if the packet-based split move mechanic is used instead of the Bukkit-based one (or the move was not split)
     * @return
     */
    public Location check(final Player player, final PlayerLocation from, final PlayerLocation to, 
                          final int multiMoveCount, final MovingData data, final MovingConfig cc, 
                          final IPlayerData pData, final int tick, final long now, 
                          final boolean useBlockChangeTracker, final boolean isNormalOrPacketSplitMove) {
        /**
         * TOOD: Ideally, all this data should really be set outside of SurvivalFly (in the MovingListener), since they can be useful
         * for other checks / stuff.
         */
        tags.clear();
        justUsedWorkarounds.clear();
        // Shortcuts:
        final boolean debug = pData.isDebugActive(type);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        /** Regular and past fromOnGround */
        final boolean fromOnGround = from.isOnGround() || useBlockChangeTracker && from.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, data.blockChangeRef, tick);
        /** Regular and past toOnGround */
        final boolean toOnGround = to.isOnGround() || useBlockChangeTracker && to.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, data.blockChangeRef, tick);  // TODO: Work in the past ground stuff differently (thisMove, touchedGround?, from/to ...)
        /** Moving onto/into everything that isn't in air (liquids, stuck-speed, ground, ALL) */
        final boolean resetTo = toOnGround || to.isResetCond();
        /** Moving off from anything that is not air (liquids, stuck-speed, ground, ALL), lostground is accounted for here. */
        final boolean resetFrom = fromOnGround || from.isResetCond() || LostGround.lostGround(player, from, to, thisMove.hDistance, thisMove.yDistance, pData.isSprinting(), lastMove, data, cc, useBlockChangeTracker ? blockChangeTracker : null, tags);
        data.ws.setJustUsedIds(justUsedWorkarounds);

        // Recover from data removal (somewhat random insertion point).
        if (data.liftOffEnvelope == LiftOffEnvelope.UNKNOWN) {
            data.adjustLiftOffEnvelope(from);
        }
  
        // NOTE: Should not use loc for adjusting as split moves can mess up
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

        if (player.getItemInUse() != null) {
            player.sendMessage("Item in use: " + player.getItemInUse().toString());
        }
        
        // Renew the "dirty"-flag (in-air phase affected by velocity).
        // (Reset is done after checks run.) 
        if (data.isVelocityJumpPhase() || data.resetVelocityJumpPhase(tags)) {
            tags.add("dirty");
        }

        // Decrease bunnyhop delay counter (bunnyfly)
        if (data.bunnyhopDelay > 0) {
            data.bunnyhopDelay--; 
        }
        

        /////////////////////////////////////
        // Horizontal move                ///
        /////////////////////////////////////
        double hAllowedDistance = 0.0, hDistanceAboveLimit = 0.0, hFreedom = 0.0;
        // Run through all hDistance checks if the player has actually some horizontal distance (saves some performance)
        // Set the allowed distance and determine the distance above limit
        double[] estimationRes = hDistRel(from, to, pData, player, data, thisMove, lastMove, cc, tick, useBlockChangeTracker, fromOnGround, toOnGround, debug, multiMoveCount, isNormalOrPacketSplitMove);
        hAllowedDistance = estimationRes[0];
        hDistanceAboveLimit = estimationRes[1];
        // The player went beyond the allowed limit, execute the after failure checks.
        if (hDistanceAboveLimit > 0.0) {
            double[] res = hDistAfterFailure(player, multiMoveCount, from, to, hAllowedDistance, hDistanceAboveLimit, thisMove, lastMove, debug, data, cc, pData, tick, useBlockChangeTracker, fromOnGround, toOnGround, isNormalOrPacketSplitMove);
            hAllowedDistance = res[0];
            hDistanceAboveLimit = res[1];
            hFreedom = res[2];
        }
        else {
            // Clear active velocity if the distance is within limit (clearly not needed. :))
            data.clearActiveHorVel();
            hFreedom = 0.0;
        }
        // Adjust some data after horizontal checking but before vertical
        // Count down for the soul speed enchant motion
        if (data.keepfrictiontick > 0) {
            data.keepfrictiontick-- ;
        }

        // A special(model) move from CreativeFly has been turned to a normal move again, count up for the incoming motion
        if (data.keepfrictiontick < 0) {
            data.keepfrictiontick++ ;
        }


        /////////////////////////////////////
        // Vertical move                  ///
        /////////////////////////////////////
        double vAllowedDistance = 0.0, vDistanceAboveLimit = 0.0;
        // Step wild-card: allow step height from ground to ground.
        if (thisMove.yDistance >= 0.0 && thisMove.yDistance <= cc.sfStepHeight 
            && toOnGround && fromOnGround && !from.isOnClimbable() 
            && !to.isOnClimbable() && !thisMove.hasLevitation) {
            vAllowedDistance = cc.sfStepHeight;
            thisMove.canStep = true;
            tags.add("groundstep");
        }
        else if (from.isOnClimbable()) {
            // They can technically be placed inside liquids.
            final double[] resultClimbable = vDistClimbable(player, from, to, fromOnGround, toOnGround, pData, thisMove, lastMove, thisMove.yDistance, data, cc);
            vAllowedDistance = resultClimbable[0];
            vDistanceAboveLimit = resultClimbable[1];
        }
        else if (from.isInLiquid()) { 
            // Minecraft checks for liquids first, then for air.
            final double[] resultLiquid = vDistLiquid(thisMove, from, to, toOnGround, thisMove.yDistance, lastMove, data, player, cc, pData, fromOnGround);
            vAllowedDistance = resultLiquid[0];
            vDistanceAboveLimit = resultLiquid[1];

            // The friction jump phase has to be set externally.
            if (vDistanceAboveLimit <= 0.0 && thisMove.yDistance > 0.0 
                && Math.abs(thisMove.yDistance) > Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player))) {
                data.setFrictionJumpPhase();
            }
        }
        else {
            final double[] resultAir = vDistRel(now, player, from, fromOnGround, resetFrom, 
                                                to, toOnGround, resetTo, hDistanceAboveLimit, thisMove.yDistance, isNormalOrPacketSplitMove,
                                                multiMoveCount, lastMove, data, cc, pData);
            vAllowedDistance = resultAir[0];
            vDistanceAboveLimit = resultAir[1];
        }
        // Vertical push/pull. (Horizontal is done in hDistanceAfterFailure)//
        // TODO: Better place for checking for moved blocks [redesign for intermediate result objects?].
        if (useBlockChangeTracker && vDistanceAboveLimit > 0.0) {
            double[] blockMoveResult = getVerticalBlockMoveResult(thisMove.yDistance, from, to, tick, data);
            if (blockMoveResult != null) {
                vAllowedDistance = blockMoveResult[0];
                vDistanceAboveLimit = blockMoveResult[1];
            }
        }


        ////////////////////////////
        // Debug output.          //
        ////////////////////////////
        final int tagsLength;
        if (debug) {
            outputDebug(player, to, from, data, cc, thisMove.hDistance, hAllowedDistance, hFreedom, 
                        thisMove.yDistance, vAllowedDistance, fromOnGround, resetFrom, toOnGround, 
                        resetTo, thisMove, vDistanceAboveLimit);
            tagsLength = tags.size();
            data.ws.setJustUsedIds(null);
        }
        else tagsLength = 0; // JIT vs. IDE.


        //////////////////////////////////////
        // Handle violations               ///
        //////////////////////////////////////
        final boolean inAir = Magic.inAir(thisMove);
        final double result = (Math.max(hDistanceAboveLimit, 0D) + Math.max(vDistanceAboveLimit, 0D)) * 100D;
        if (result > 0D) {

            final Location vLoc = handleViolation(now, result, player, from, to, data, cc);
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

        // 2: Count how long one is moving inside of a medium.
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
            if (to.getY() < 0.0 && cc.sfSetBackPolicyVoid || thisMove.hasLevitation || Bridge1_13.isRiptiding(player)) {
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
     * @param player
     *            the player
     * @param pData
     * @param cc
     * @param data
     * @return If to prevent action (use the set back location of survivalfly).
     */
    public boolean checkBed(final Player player, final IPlayerData pData, final MovingConfig cc, final MovingData data) {
        boolean cancel = false;
        // Check if the player had been in bed at all.
        if (!data.wasInBed) {
            // Violation ...
            tags.add("bedfly");
            data.survivalFlyVL += 100D;
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
     * 
     * @param yDistance
     * @param from
     * @param to
     * @param data
     * @return
     */
    private double[] getVerticalBlockMoveResult(final double yDistance, final PlayerLocation from, final PlayerLocation to, final int tick, final MovingData data) {
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
     * Currently unused.
     * 
     * @param from
     * @param to
     * @param pData
     * @param data
     * @param player
     * @param isNormalOrPacketSplitMove
     * @return the allowed xyz distances + distances above limit.
     */
    @SuppressWarnings("unused")
    private double[] elytraPredict(final PlayerLocation from, final PlayerLocation to, final IPlayerData pData, final MovingData data, 
                                   final Player player, boolean isNormalOrPacketSplitMove, final boolean fromOnGround, final boolean toOnGround) {
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        double vDistanceAboveLimit = 0.0, hDistanceAboveLimit = 0.0;
        // WASD key presses are irrelevant when gliding.
        thisMove.hasImpulse = false;

        // TODO: Glide -> no glide transition... Yeah.
        // TODO: What with gliding + rocket boosting + riptiding + bouncing on slimes/beds while riptiding? LMAO
        // Beats me why Mojang keeps letting players perform such ridiculous moves.

        if (!isNormalOrPacketSplitMove) {
            // Don't even attempt to predict if Bukkit skipped PlayerMoveEvents and we couldn't recover the movement's information via packets.
            // Would require too many hacks/workarounds.
            thisMove.xAllowedDistance = thisMove.xDistance;
            thisMove.zAllowedDistance = thisMove.zDistance;
            thisMove.vAllowedDistance = thisMove.yDistance;
            thisMove.hAllowedDistance = thisMove.hDistance;
            data.nextInertia = 0.99f;
            return new double[]{thisMove.hAllowedDistance, thisMove.vAllowedDistance, 0.0, 0.0};
        }
        // Initialize speed and horizontal friction.
        // Note about inertia: the game assigns the radian pitch to the "f" variable, which is the variable used to apply friction at the end of the tick, _normally_.
        // However, with gliding, the game does not use the f variable at the end of the tick, but instead applies the magic value of 0.99.
        data.nextInertia = 0.99f;
        thisMove.xAllowedDistance = lastMove.toIsValid ? lastMove.xDistance : 0.0;
        thisMove.vAllowedDistance = lastMove.toIsValid ? lastMove.yDistance : 0.0;
        thisMove.zAllowedDistance = lastMove.toIsValid ? lastMove.zDistance : 0.0;
        // Throttle speed if stuck in a block.
        if (data.lastStuckInBlockVertical != 1.0) {
            thisMove.vAllowedDistance = 0.0;
        }
        if (data.lastStuckInBlockHorizontal != 1.0) { 
           thisMove.xAllowedDistance = thisMove.zAllowedDistance = 0.0;
        }
        // Reset speed if judged to be negligible.
        if (Math.abs(thisMove.xAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY) {
            thisMove.xAllowedDistance = 0.0;
        }
        if (Math.abs(thisMove.vAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY) {
            thisMove.vAllowedDistance = 0.0;
        }
        if (Math.abs(thisMove.zAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY) {
            thisMove.zAllowedDistance = 0.0;
        }
        // Proceed to calculate...
        Vector viewVector = TrigUtil.getLookingDirection(to, player);
        float radianPitch = to.getPitch() * TrigUtil.toRadians;
        double directionHDist = MathUtil.dist(viewVector.getX(), viewVector.getZ());
        // Mojang switched from their own cosine function to the standard Math.cos() one in 1.18.2
        double cosPitch = pData.getClientVersion().isOlderThanOrEquals(ClientVersion.V_1_18_2) ? TrigUtil.cos((double)radianPitch) : Math.cos((double)radianPitch);
        cosPitch = MathUtil.square(cosPitch) * Math.min(1.0, viewVector.length() / 0.4);
        // Base gravity when gliding.
        thisMove.vAllowedDistance += (thisMove.hasSlowfall && thisMove.yDistance <= 0.0 ? Magic.DEFAULT_SLOW_FALL_GRAVITY : Magic.DEFAULT_GRAVITY) * (-1.0 + cosPitch * 0.75);
        double baseSpeed;
        if (thisMove.yDistance < 0.0 && directionHDist > 0.0) {
            // Slow down.
            baseSpeed = lastMove.yDistance * -0.1 * cosPitch;
            thisMove.vAllowedDistance += baseSpeed;
            thisMove.xAllowedDistance += viewVector.getX() * baseSpeed / directionHDist;
            thisMove.zAllowedDistance += viewVector.getZ() * baseSpeed / directionHDist;
        }
        if (radianPitch < 0.0 && directionHDist > 0.0) {
            // Looking down speeds up the player.
            baseSpeed = lastMove.hDistance * (double) (-TrigUtil.sin(radianPitch)) * 0.04;
            thisMove.vAllowedDistance += baseSpeed * 3.2;
            thisMove.xAllowedDistance += -viewVector.getX() * baseSpeed / directionHDist;
            thisMove.zAllowedDistance += -viewVector.getZ() * baseSpeed / directionHDist;
        }
        if (directionHDist > 0.0) {
            // Accellerate
            thisMove.xAllowedDistance += (viewVector.getX() / directionHDist * lastMove.hDistance - lastMove.xDistance) * 0.1;
            thisMove.zAllowedDistance += (viewVector.getZ() / directionHDist * lastMove.hDistance - lastMove.zDistance) * 0.1;
        }
        // Horizontal friction here. (TEST)
        thisMove.xAllowedDistance *= data.lastInertia;
        thisMove.vAllowedDistance *= data.lastFrictionVertical;
        thisMove.zAllowedDistance *= data.lastInertia;
        // Stuck-speed with the updated multiplier (both at the end)
        if (data.fireworksBoostDuration <= 0) {
            // Fireworks don't care about stuck-speed.
            thisMove.xAllowedDistance *= (double) data.nextStuckInBlockHorizontal;
            thisMove.vAllowedDistance *= (double) data.nextStuckInBlockVertical;
            thisMove.zAllowedDistance *= (double) data.nextStuckInBlockHorizontal;
        }

        if (data.fireworksBoostDuration > 0) {
            // TODO: Can this STACK WITH RIPTIDING!?
            // TODO: Firework netcode is horrible (a single firework can tick twice on the same tick, skipping the subsequent one), so simply applying the increase of speed won't cut it.
            // Not even sure if we can predict this at all without some kind of hacks / workarounds.
            thisMove.xAllowedDistance += viewVector.getX() * 0.1 + (viewVector.getX() * 1.5 - lastMove.xDistance) * 0.5;
            thisMove.vAllowedDistance += viewVector.getY() * 0.1 + (viewVector.getY() * 1.5 - lastMove.yDistance) * 0.5;
            thisMove.zAllowedDistance += viewVector.getZ() * 0.1 + (viewVector.getZ() * 1.5 - lastMove.zDistance) * 0.5;
            // The maximum reachable speed while boosting with rockets is 1.7
            //    thisMove.xAllowedDistance = MathUtil.clamp(thisMove.xAllowedDistance, -1.7, 1.7);
            //    thisMove.vAllowedDistance = MathUtil.clamp(thisMove.vAllowedDistance, -1.7, 1.7);
            //    thisMove.zAllowedDistance = MathUtil.clamp(thisMove.zAllowedDistance, -1.7, 1.7);
        }

        // Yes, players can glide and riptide at the same time, increasing speed at a faster rate than chunks can load...
        // Surely a questionable decision on Mojang's part.
        if (lastMove.slowedByUsingAnItem && !thisMove.slowedByUsingAnItem && thisMove.isRiptiding) {
            final double RiptideLevel = BridgeEnchant.getRiptideLevel(player);
            if (RiptideLevel > 0.0) {
                float x = -TrigUtil.sin(to.getYaw() * TrigUtil.toRadians) * TrigUtil.cos(to.getPitch() * TrigUtil.toRadians);
                float y = -TrigUtil.sin(to.getPitch() * TrigUtil.toRadians);
                float z = TrigUtil.cos(to.getYaw() * TrigUtil.toRadians) * TrigUtil.cos(to.getPitch() * TrigUtil.toRadians);
                float pushForce = MathUtil.sqrt(x*x + y*y + z*z);
                float riptideSpeed = 3.0f * ((1.0f + (float) RiptideLevel) / 4.0f);
                x *= riptideSpeed / pushForce;
                y *= riptideSpeed / pushForce;
                z *= riptideSpeed / pushForce;
                thisMove.xAllowedDistance += (double) x;
                thisMove.vAllowedDistance += (double) y;
                thisMove.zAllowedDistance += (double) z;
            } 
        }

        // Check for workarounds and ovveride the incorrect prediction if necessary.
        if (from.isNextToSolid(thisMove.xAllowedDistance, 0.0) || from.isNextToSolid(0.01, 0.0)) {
            // Add some leniency when moving near/against walls..
            thisMove.xAllowedDistance += 0.1;
            player.sendMessage("Wall collision (x)");
        }
        if (from.isNextToSolid(0.0, thisMove.zAllowedDistance) || from.isNextToSolid(0.0, 0.01)) {
            thisMove.zAllowedDistance =+ 0.1;
            player.sendMessage("Wall collision (z)");
        }
        if (!from.isOnGroundOrResetCond() && to.isOnGroundOrResetCond() && thisMove.yDistance < 0.0 
            || thisMove.headObstructed && lastMove.yDistance > 0.0) {
            // Allow landing back on ground
            player.sendMessage("Workaround (v)");
            thisMove.vAllowedDistance = thisMove.yDistance;
        }


        /** Expected difference from current to allowed */       
        final double offsetV = thisMove.yDistance - thisMove.vAllowedDistance; 
        if (Math.abs(offsetV) < Magic.PREDICTION_EPSILON) {
            // Accuracy margin.
        }
        else {
            // If velocity can be used for compensation, use it.
            if (data.getOrUseVerticalVelocity(thisMove.yDistance) == null) {
                vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(offsetV));
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
        player.sendMessage("vDistance/Predicted " + StringUtil.fdec6.format(thisMove.yDistance) + " / " + StringUtil.fdec6.format(thisMove.vAllowedDistance));
        return new double[]{thisMove.hAllowedDistance, thisMove.vAllowedDistance, hDistanceAboveLimit, vDistanceAboveLimit};
    }
    
    
   /** 
    * Estimate the player's horizontal speed, according the to given data (inertia, ground status, etc...)
    */ 
   private void processNextSpeed(final Player player, float movementSpeed, final IPlayerData pData, final Collection<String> tags, 
                                 final PlayerLocation to, final PlayerLocation from, final boolean debug,
                                 final boolean fromOnGround, final boolean toOnGround, final boolean onGround) {
        final MovingData data = pData.getGenericInstance(MovingData.class);      
        final CombinedData cData = pData.getGenericInstance(CombinedData.class); 
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);   
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        /*
         * Order of operations is essential. Do not shuffle things around unless you know what you're doing.
         * IMPORTANT: The 1.20 update fixes the 12 year old bug of walking/moving on the very edge blocks not applying the properties of said block
         *            - HoneyBlocks will now restrict jumping even if on the very edge
         *            - Same for slime and everything else
         *            
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
         * - Call Entity.moveRelative() from LivingEntity.travel() (apply acceleration [!]) - 13
         * - Call Entity.move() from LivingEntity.travel()
         *    -> Stuck speed multiplier 14
         *    -> maybeBackOffFromEdge - 15
         *    -> wall collision(Entity.collide()) - 16
         *
         *>>>>>>>Complete this move, prepare the next one.<<<<<<<
         *
         *    -> horizontalCollision - next move - 1
         *    -> checkFallDamage(do liquid puishing if was not previosly in water) - next move - 2
         *    -> Block.updateEntityAfterFallOn() - next move - 3
         *    -> Block.stepOn() (for slime block) - next move - 4
         *    -> tryCheckInsideBlocks() (for honey block) - next move - 5
         *    -> Entity.getBlockSpeedFactor() - next move - 6
         * - Complete the running the LivingEntity.travel() function. Apply friction/inertia - next move - 7
         * - Entity pushing - next move - complete running the aiStep() function - 8
         * - Complete running the LivingEntity.tick() function 
         *
         *>>>>>>> Send the movement to the server<<<<<<<
         *
         * - Repeat
         * 
         * From the order above, we will start from horizontalCollision, essentially reversing the calculations the client is doing: first all stuff that is done in the next tick by the client, then all the stuff done in the current tick.
         */

        // Because Minecraft does not offer any way to listen to player's inputs, we brute force through all combinations and see which input combo matches the current speed of the player.
        ///////////////////////////////
        // Setup theoretical inputs  //
        ///////////////////////////////
        InputDirection inputs[] = new InputDirection[9];
        int i = 0;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                // Minecraft multiplies the input values by 0.98 before passing them to the travel() function.
                inputs[i] = new InputDirection(x * 0.98f, z * 0.98f);
                i++;
            }
        }
        // From KeyboardInput.java (MC-Reborn tool)
        // Sneaking and item-use aren't directly applied to the player's motion. The game reduces the input force instead.
        if (pData.isSneaking()) {
            tags.add("sneaking");
            float SwiftSneakIncrement = BridgeEnchant.getSwiftSneakIncrement(player);
            for (i = 0; i < 9; i++) {
                // Multiply all combinations
                inputs[i].calculateDir(Magic.SNEAK_MULTIPLIER + SwiftSneakIncrement, Magic.SNEAK_MULTIPLIER + SwiftSneakIncrement, 1);
            }
        }
        // From LocalPlayer.java.aiStep()
        if (BridgeMisc.isUsingItem(player)) {
            tags.add("usingitem");
            for (i = 0; i < 9; i++) {
                inputs[i].calculateDir(Magic.USING_ITEM_MULTIPLIER, Magic.USING_ITEM_MULTIPLIER, 1);
            }
        }


        /////////////////////////////////////////////////////////////
        // Next tick / move (client) -  Last tick / move (server)  //
        /////////////////////////////////////////////////////////////
        // Initialize the allowed distance(s) with the previous speed. (Only if we have end-point coordinates)
        // This essentially represents the momentum of the player.
        thisMove.xAllowedDistance = lastMove.toIsValid ? lastMove.to.getX() - lastMove.from.getX() : 0.0;
        thisMove.zAllowedDistance = lastMove.toIsValid ? lastMove.to.getZ() - lastMove.from.getZ() : 0.0;
        // (The game calls a checkFallDamage() function, which, as you can imagine, handles fall damage. But also handles liquids' flow force, thus we need to apply this 2 times.)
        if (from.isInWater() && !lastMove.from.inWater) {
            Vector liquidFlowVector = from.getLiquidPushingVector(thisMove.xAllowedDistance, thisMove.zAllowedDistance, BlockFlags.F_WATER);
            thisMove.xAllowedDistance += liquidFlowVector.getX();
            thisMove.zAllowedDistance += liquidFlowVector.getZ();
        }
        // Slime speed
        // (Ground check is already included)
        if (from.isOnSlimeBlock()) {
            // Use pData#isSneaking() because swimming on slime blocks is a thing.
            if (Math.abs(thisMove.yDistance) < 0.1 && !pData.isSneaking()) { // -0.0784000015258789
                thisMove.xAllowedDistance *= 0.4 + Math.abs(thisMove.yDistance) * 0.2;
                thisMove.zAllowedDistance *= 0.4 + Math.abs(thisMove.yDistance) * 0.2;
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
        //if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7D) {
        //    p_19974_ = p_19974_.multiply(this.stuckSpeedMultiplier);
        //    this.stuckSpeedMultiplier = Vec3.ZERO;
        //    this.setDeltaMovement(Vec3.ZERO);
        //}
        if (data.lastStuckInBlockHorizontal != 1.0) { 
            if (TrigUtil.lengthSquared(data.nextStuckInBlockHorizontal, data.nextStuckInBlockVertical, data.nextStuckInBlockVertical) > 1.0E-7) {
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
        // TODO: This isn't correct: entity pushing is done on the next move (thus, this has already been applied with the move we receive on the server-side).
        // The entity's location is in the past.
        // TODO: Properly implement


        //////////////////////////////////////////////////////////////////////
        // Current tick / move (client) - Current tick / move (server)      //
        //////////////////////////////////////////////////////////////////////
        // See CombinedListener.java for more details
        // This is done before liquid pushing...
        if (thisMove.hasAttackSlowDown) {
            thisMove.zAllowedDistance *= Magic.ATTACK_SLOWDOWN;
            thisMove.xAllowedDistance *= Magic.ATTACK_SLOWDOWN;
        }
        if (from.isInLiquid()) {
            // Apply liquid pushing speed (2nd call).
            Vector liquidFlowVector = from.getLiquidPushingVector(thisMove.xAllowedDistance, thisMove.zAllowedDistance, from.isInWater() ? BlockFlags.F_WATER : BlockFlags.F_LAVA);
            thisMove.xAllowedDistance += liquidFlowVector.getX();
            thisMove.zAllowedDistance += liquidFlowVector.getZ();
        }
        // Before calculating the acceleration, check if momentum is below the negligible speed threshold and cancel it.
        if (pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9)) {
            if (Math.abs(thisMove.xAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD) {
                thisMove.xAllowedDistance = 0.0;
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
            if (Math.abs(thisMove.zAllowedDistance) < Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY) {
                thisMove.zAllowedDistance = 0.0;
            }
        }
        // Sprint-jumping...
        if (!from.isInLiquid()) {
            /* 
             * NOTE: here you need to use to.getYaw not from. To is the most recent rotation.
             *       Using From lags behind a few ticks, causing false positives when switching looking direction.
             *       This does not apply for locations (from.(...) correctly reflects the player's current position)
             */
            if (PlayerEnvelopes.isBunnyhop(pData, fromOnGround, toOnGround, player)) {
                thisMove.xAllowedDistance += (double) (-TrigUtil.sin(to.getYaw() * TrigUtil.toRadians) * Magic.BUNNYHOP_BOOST); 
                thisMove.zAllowedDistance += (double) (TrigUtil.cos(to.getYaw() * TrigUtil.toRadians) * Magic.BUNNYHOP_BOOST); 
                data.bunnyhopDelay = Magic.BUNNYHOP_MAX_DELAY;
                thisMove.bunnyHop = true;
                tags.add("bunnyhop");
                // Keep up the Improbable, but don't feed anything.
                Improbable.update(System.currentTimeMillis(), pData);
            }
        }
        // Transform theoretical inputs to acceleration vectors (getInputVector, entity.java)
        /* 
         * NOTE: here you need to use to.getYaw not from. To is the most recent rotation.
         *       Using from lags behind a few ticks, causing false positives when switching looking direction.
         *       This does not apply for locations (from.(...) correctly reflects the player's current position)
         */
        float sinYaw = TrigUtil.sin(to.getYaw() * TrigUtil.toRadians);
        float cosYaw = TrigUtil.cos(to.getYaw() * TrigUtil.toRadians);
        /** List of predicted X distances. Size is the number of possible inputs (left/right/backwards/forward etc...) */
        double xTheoreticalDistance[] = new double[9];
        /** List of predicted Z distances. Size is the number of possible inputs (left/right/backwards/forward etc...) */
        double zTheoreticalDistance[] = new double[9];
        for (i = 0; i < 9; i++) {
            // Each slot in the array is initialized with the same momentum.
            xTheoreticalDistance[i] = thisMove.xAllowedDistance;
            zTheoreticalDistance[i] = thisMove.zAllowedDistance;
            // Then we proceed to compute all possible accelerations with all theoretical inputs.
            double inputSq = MathUtil.square((double)inputs[i].getStrafe()) + MathUtil.square((double)inputs[i].getForward()); // Cast to a double because the client does it
            if (inputSq >= 1.0E-7) {
                if (inputSq > 1.0) {
                    double inputForce = Math.sqrt(inputSq);
                    if (inputForce < 1.0E-4) {
                        // Not enough force, reset.
                        inputs[i].calculateDir(0, 0, 0);
                    }
                    else {
                        // Normalize
                        inputs[i].calculateDir(inputForce, inputForce, 2);
                    }
                }
                // Multiply all inputs by movement speed.
                inputs[i].calculateDir(movementSpeed, movementSpeed, 1);
                // All acceleration vectors are added to each momentum.
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
        if (TrigUtil.lengthSquared(data.nextStuckInBlockHorizontal, data.nextStuckInBlockVertical, data.nextStuckInBlockVertical) > 1.0E-7) {
            // Apply stuck-speed
            for (i = 0; i < 9; i++) {
                xTheoreticalDistance[i] *= (double) data.nextStuckInBlockHorizontal;
                zTheoreticalDistance[i] *= (double) data.nextStuckInBlockHorizontal;
            }
        }
        // Try to back off players from edges, if sneaking.
        // NOTE: here the game uses isShiftKeyDown (so this is shifting not sneaking, using Bukkit's isSneaking is correct)
        if (!player.isFlying() && pData.isSneaking() && from.isAboveGround() && thisMove.yDistance <= 0.0) {
            for (i = 0; i < 9; i++) {
                // Not sure about performance here. We are looping 8 different directions with 3 different 'while' loops after all...
                Vector backOff = from.maybeBackOffFromEdge(new Vector(xTheoreticalDistance[i], 0.0, zTheoreticalDistance[i]));
                xTheoreticalDistance[i] = backOff.getX();
                zTheoreticalDistance[i] = backOff.getZ();
            }
        }
        if (lastMove.slowedByUsingAnItem && !thisMove.slowedByUsingAnItem && thisMove.isRiptiding) {
            // Riptide works by propelling the player after releasing the trident (the effect only pushes the player, unless is on ground)
            final double RiptideLevel = BridgeEnchant.getRiptideLevel(player);
            if (RiptideLevel > 0.0) {
                float x = -TrigUtil.sin(to.getYaw() * TrigUtil.toRadians) * TrigUtil.cos(to.getPitch() * TrigUtil.toRadians);
                float y = -TrigUtil.sin(to.getPitch() * TrigUtil.toRadians);
                float z = TrigUtil.cos(to.getYaw() * TrigUtil.toRadians) * TrigUtil.cos(to.getPitch() * TrigUtil.toRadians);
                float distance = MathUtil.sqrt(x*x + y*y + z*z);
                float force = 3.0f * ((1.0f + (float) RiptideLevel) / 4.0f);
                x *= force / distance;
                z *= force / distance;
                for (i = 0; i < 9; i++) {
                    xTheoreticalDistance[i] += x;
                    zTheoreticalDistance[i] += z;
                }
            } 
        }
        

        /////////////////////////////////////////////////////////////////////////////
        // Determine which (and IF) theoretical speed should be set in this move   //
        /////////////////////////////////////////////////////////////////////////////
        /** 
         * True, if the offset between predicted and actual speed is smaller than the accuracy margin (0.0001).
         */
        boolean found = false;
        /** 
         * True will check if BOTH axis have an offset smaller than 0.0001 (against strafe-like cheats and anything of that sort that relies on the specific direction of the move).
         * Will also perform some auxiliary checks.<br>
         * Otherwise, only the combined horizontal distance will be checked against the offset.
         */
        boolean strict = true; 
        /**
         * If true, it means that there could be a theoretical speed to set, but a subcheck has requested not to.
         */
        boolean forceViolation = false;
        for (i = 0; i < 9; i++) {
            // Calculate all possible hDistances
            double theoreticalHDistance = MathUtil.dist(xTheoreticalDistance[i], zTheoreticalDistance[i]);
            if (strict) {
                if (MathUtil.almostEqual(to.getX()-from.getX(), xTheoreticalDistance[i], Magic.PREDICTION_EPSILON) 
                    && MathUtil.almostEqual(to.getZ()-from.getZ(), zTheoreticalDistance[i], Magic.PREDICTION_EPSILON)) {

                    if (pData.isSprinting() && inputs[i].getForwardDir() != ForwardDirection.FORWARD && inputs[i].getStrafeDir() != StrafeDirection.NONE) {
                        // Assume cheating; if sprinting sideways or backwards, this speed is no candidate to set in thisMove.
                        tags.add("illegalsprint");
                        Improbable.check(player, (float) thisMove.hDistance, System.currentTimeMillis(), "moving.survivalfly.illegalsprint", pData);
                        forceViolation = true;
                    } 
                    else if (cData.isHackingRI) {
                        // Blatant cheat attempt, do not set speed.
                        tags.add("noslowpacket");
                        cData.isHackingRI = false;
                        Improbable.check(player, (float) thisMove.hDistance, System.currentTimeMillis(), "moving.survivalfly.noslow", pData);
                        forceViolation = true;
                    }
                    else {
                        found = true;
                    }
                }
            } 
            else {
                // Simply compare the combined speed otherwise.
                if (MathUtil.almostEqual(theoreticalHDistance, thisMove.hDistance, Magic.PREDICTION_EPSILON)) {
                    found = true;
                }
            }
            if (found) {
                // Found a candidate to set in this move.
                break;
            }
        }

        //////////////////////////////////////////////////////////////
        // Finish. Check if the move had been predictable at all    //
        //////////////////////////////////////////////////////////////
        postPredictionProcessing(data, pData, player, xTheoreticalDistance, zTheoreticalDistance, from, to, i, debug, inputs, forceViolation);
    }


    /**
     * Finalize this speed processing: check if we should apply a workaround. Otherwise, just throw a violation if needed.
     * (See AirWorkarounds class desc)
     * 
     * @param data
     * @param pData
     * @param player
     * @param xTheoreticalDistance The list of all 8 possible x distances.
     * @param zTheoreticalDistance The list of all 8 possible z distances.
     * @param from
     * @param to
     * @param inputIndex Index of the array containing all theoretical directions (8 - left/forward/backward/right/b.left/b.right/f.right/f.left). 
     */
    private void postPredictionProcessing(final MovingData data, final IPlayerData pData, final Player player, 
                                          double[] xTheoreticalDistance, double[] zTheoreticalDistance, final PlayerLocation from, final PlayerLocation to,
                                          int inputsIdx, final boolean debug, InputDirection[] inputs, final boolean forceViolation) {
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        /** All moves are assumed to be predictable, unless we explicitly state in here otherwise. */
        boolean isPredictable = true;
        int x_idx = -1;
        int z_idx = -1;
        if (inputsIdx > 8) {
            // Between all 8 predicted speeds, we couldn't find one with an offset of 0.0001 from actual speed.
            // To prevent an IndexOutOfBounds, set the input index back to 4, which corresponds to the index of the input of NONE/NONE.
            // (This is the one that will be set IF the player is actually cheating)
            inputsIdx = 4;   
            // Check for workarounds, unless the move has been judged to be illegal already.
            if (!forceViolation) {
                // Get the indeces of the theoretical x/z speeds that are closest to the actual speed.
                x_idx = MathUtil.closestIndex(xTheoreticalDistance, to.getX() - from.getX());
                z_idx = MathUtil.closestIndex(zTheoreticalDistance, to.getZ() - from.getZ());
                if (HorizontalUncertainty.tryDoWallCollisionX(from, to, xTheoreticalDistance, x_idx) ||
                    HorizontalUncertainty.tryDoWallCollisionZ(from, to, zTheoreticalDistance, z_idx) ||
                    HorizontalUncertainty.applyEntityPushingGrace(player, pData, xTheoreticalDistance, zTheoreticalDistance, x_idx, z_idx, to)) {
                    isPredictable = false;
                }
            } 
        }
        // Finally, set in this move.
        thisMove.xAllowedDistance = xTheoreticalDistance[!isPredictable ? x_idx : inputsIdx];
        thisMove.zAllowedDistance = zTheoreticalDistance[!isPredictable ? z_idx : inputsIdx];
        // TODO: How can we know the impulse if the move is uncertain? ...
        thisMove.hasImpulse = inputs[isPredictable ? inputsIdx : x_idx].getForwardDir() != ForwardDirection.NONE && inputs[isPredictable ? inputsIdx : z_idx].getStrafeDir() != StrafeDirection.NONE;
        // Debug and also set a flag, useful for other checks / contexts.
        if (debug) {
            player.sendMessage("[SurvivalFly] (postPredict) " + (!isPredictable ? "Uncertain" : "Predicted") + " direction: " + inputs[isPredictable ? inputsIdx : x_idx].getForwardDir() +" | "+ inputs[isPredictable ? inputsIdx : x_idx].getStrafeDir());
        }
    }


    /**
     * Core h-distance checks for media and all status
     * @return hAllowedDistance, hDistanceAboveLimit
     */
    private final double[] hDistRel(final PlayerLocation from, final PlayerLocation to, final IPlayerData pData, final Player player, 
                                    final MovingData data, final PlayerMoveData thisMove, final PlayerMoveData lastMove, final MovingConfig cc,
                                    final int tick, final boolean useBlockChangeTracker,
                                    final boolean fromOnGround, final boolean toOnGround, final boolean debug, final int multiMoveCount, 
                                    final boolean isNormalOrPacketSplitMove) {
        final boolean onGround = fromOnGround || thisMove.missedGroundCollision
                                 || lastMove.toIsValid && lastMove.yDistance <= 0.0  
                                 && (lastMove.from.onGround || lastMove.touchedGroundWorkaround && !thisMove.touchedGroundWorkaround /* See playerEnvelopes.isJump for touchedGroundWorkaround*/);
        double hDistanceAboveLimit = 0.0;
         
        // NOTE: Consider dropping bunnyfly once and for all. With vDistRel being reworked, this shouldn't be needed anymore.
        // Context: originally this was needed/intended to prevent players from _legitimately_ gaining more bunnyhop boosts than legit by simply: hopping-> descend faster than legit -> re-do hop -> rinse and repeat at a faster rate than legit.
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Determine if the bunnyhop delay should be reset earlier (bunnyfly). These checks need to run before the estimation         //                  
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // (NCP mechanic, not vanilla. We decide when players can bunnyhop because we use our own collision system for on ground judgement)
        if (data.bunnyhopDelay > 0) {
            if (data.bunnyhopDelay <= 9) {
                // (10 represents a bunnyhop)
                tags.add("bunnyfly(" + data.bunnyhopDelay + ")");
            }
            
            // Reset cases:
            if (from.isHeadObstructed(0.2, false) && thisMove.yDistance != 0.0) {
                // Head collision will make jumps last less.
                // Here (theoretically), a cheater could -after bumping head with a solid block- attempt to descend faster than legit to get a new bunnyhop boost. (thus increasing horizontal speed at faster rate than legit).
                // Catching irregular vertical motions is the job of vDistRel however. 
                // For more details, see AirWorkarounds.
                data.bunnyhopDelay = 0;
            }
            else if (from.isAboveStairs()) {
                // WORKAROUND: always reset the delay if moving on stairs due to the double-jump feature with autojump
                data.bunnyhopDelay = 0;
            }
            else if (onGround && !lastMove.bunnyHop) {
                // Jump delay lasts less on/in these blocks
                if (from.isOnHoneyBlock() && data.bunnyhopDelay <= 4
                    || from.isInBerryBush() && data.bunnyhopDelay <= 3
                    || from.isInWeb() && data.bunnyhopDelay <= 7) {
                    data.bunnyhopDelay = 0;
                }
                else if (data.bunnyhopDelay <= 7 && !thisMove.headObstructed 
                        // The player actually jumped up a slope: this movement has a higher altitude than the bunnyhop move.
                        && thisMove.from.getY() > data.playerMoves.getPastMove(Magic.BUNNYHOP_MAX_DELAY - data.bunnyhopDelay).to.getY()) {
                    // (Ground check is checked above)
                    data.bunnyhopDelay = 0;
                }
            }
        }


        //////////////////////////////////////////////////////////////
        // Estimate the horizontal speed (per-move distance check)  //                      
        //////////////////////////////////////////////////////////////
        // Determine inertia and acceleration to calculate speed with.
        if (isNormalOrPacketSplitMove) { 
            // Only check 'from' to spare some problematic transitions between media (i.e.: in 1.13+ with players being able to swim up to the surface and have 2 in-air moves)
            if (from.isInWater()) {
                data.nextInertia = Bridge1_13.isSwimming(player) ? Magic.HORIZONTAL_SWIMMING_INERTIA : Magic.WATER_HORIZONTAL_INERTIA;
                /** Per-tick speed gain. */      
                float acceleration = Magic.LIQUID_BASE_ACCELERATION;
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
                processNextSpeed(player, Magic.LIQUID_BASE_ACCELERATION, pData, tags, to, from, debug, fromOnGround, toOnGround, onGround);
            }
            else {
                data.nextInertia = onGround ? data.nextFrictionHorizontal * Magic.HORIZONTAL_INERTIA : Magic.HORIZONTAL_INERTIA;
                // 1.12 (and below) clients will use cubed inertia, not cubed friction here. The difference isn't significant except for blocking speed and bunnyhopping on soul sand, which are both slower on 1.8
                float acceleration = onGround ? data.walkSpeed * ((pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13) ? Magic.DEFAULT_FRICTION_CUBED : Magic.CUBED_INERTIA) / (data.nextFrictionHorizontal * data.nextFrictionHorizontal * data.nextFrictionHorizontal)) : Magic.AIR_ACCELERATION;
                if (pData.isSprinting()) {
                    // (We don't use the attribute here due to desync issues, just detect when the player is sprinting and apply the multiplier manually)
                    acceleration += acceleration * 0.3f; // 0.3 is the effective sprinting speed (EntityLiving).
                }
                processNextSpeed(player, acceleration, pData, tags, to, from, debug, fromOnGround, toOnGround, onGround);
            }
        }
        else {
            // Bukkit-based split move: predicting the next speed is not possible due to coordinates not being reported correctly by Bukkit (and without ProtocolLib, it's nearly impossible to achieve precision here)
            // Technically, one could attempt to interpolate the missed positions given the from and to locations... Just like for the 0.03 threshold, but what's the point? We are not paid to deal with this bullshit).
            // Besides, there's no need in predicting speed when movement has been slowed down to the point of being considered micro by Bukkit.
            thisMove.xAllowedDistance = thisMove.to.getX() - thisMove.from.getX();
            thisMove.zAllowedDistance = thisMove.to.getZ() - thisMove.from.getZ();
            if (debug) {
                debug(player, "(hDistRel): Missed PlayerMoveEvent(s) between 'from' and 'to' (Bukkit): skip prediction for this event.");
            }
        }
        
        thisMove.hAllowedDistance = MathUtil.dist(thisMove.xAllowedDistance, thisMove.zAllowedDistance);
        /** Expected difference from current to allowed */
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
     * Core y-distance checks for in-air movement.
     */
    private double[] vDistRel(final long now, final Player player, final PlayerLocation from, 
                              final boolean fromOnGround, final boolean resetFrom, final PlayerLocation to, 
                              final boolean toOnGround, final boolean resetTo, 
                              final double hDistance, final double yDistance, boolean isNormalOrPacketSplitMove,
                              final int multiMoveCount, final PlayerMoveData lastMove, 
                              final MovingData data, final MovingConfig cc, final IPlayerData pData) {
        double vDistanceAboveLimit = 0.0;
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        
        ///////////////////////////////////////////////////////////////////////////////////
        // Estimate the allowed yDistance (per-move distance check)                      //
        ///////////////////////////////////////////////////////////////////////////////////
        /**
         * Stuff to fix / take care of:
         *  - In water/lava -> in air transitions
         *  - Jumping in waterlogged blocks
         *  - Fix insidePowderSnow checking (see notes in RichBoundsLoc)
         *  - Wobbling up on slime blocks/beds (still need some adaptations to it. Getting sporadic false positives)
         *  - Breaking blocks below -> On the server side, we receive a move with negative motion, but the player will fall with friction based on a
         * After touching the ground, the player will f
         */
        // Stepping and jumping have priority, due to both being a potential starting point for the move.
        if (PlayerEnvelopes.isStep(pData, fromOnGround, toOnGround)) {
            thisMove.vAllowedDistance = thisMove.yDistance;
            thisMove.canStep = true;
            tags.add("step_env");
        }
        else if (PlayerEnvelopes.isJump(player, fromOnGround, toOnGround)) {
            thisMove.vAllowedDistance = thisMove.yDistance;
            thisMove.canJump = true;
            tags.add("jump_env");
        }
        else if (from.isInPowderSnow() && thisMove.yDistance > 0.0 
                && BridgeMisc.hasLeatherBootsOn(player) && !thisMove.hasLevitation) {
            // Climbing inside powder snow. Set a limit because speed is throttled anyway (and I can't figure out how the game handles ascending speed here) 
            thisMove.vAllowedDistance = Magic.snowClimbSpeedAscend;
        }
        else {
            // Otherwise, a fully in-air move (friction).
            // Initialize with momentum.
            thisMove.vAllowedDistance = lastMove.toIsValid ? lastMove.yDistance : 0.0;
            // Honey block sliding mechanic (With levitation, the player will just ascend)
            if (from.isSlidingDown() && !thisMove.hasLevitation) {
                // Speed is static in this case
                thisMove.vAllowedDistance = -Magic.SLIDE_SPEED_THROTTLE;
            }
            // Bounce effect (jumping has been ruled out already).
            // updateEntityAfterFallOn(), this function is called on the next move
            if (lastMove.yDistance < 0.0 && from.isOnBouncyBlock() /*&& to.isOnBouncyBlock()*/ && !pData.isSneaking()) {
                // NOTE: the touch-down moment is handled by AirWorkarounds, thus the move leading to a violation will be the very next one (first bouncing move, ascending)
                if (from.isOnSlimeBlock()) {
                    // Invert the _predicted_ distance (last one)
                    thisMove.vAllowedDistance = -lastMove.vAllowedDistance;
                }
                else {
                    // Beds have a weaker bounce effect (BedBlock.java).
                    thisMove.vAllowedDistance = -lastMove.vAllowedDistance * 0.6600000262260437;
                }
            }
            if (data.lastStuckInBlockVertical != 1.0) {
                // Throttle speed when stuck in
                thisMove.vAllowedDistance = 0.0;
            }
            if (Math.abs(thisMove.vAllowedDistance) < (pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_9) ? Magic.NEGLIGIBLE_SPEED_THRESHOLD : Magic.NEGLIGIBLE_SPEED_THRESHOLD_LEGACY)) {
                // Negligible speed reset.
                thisMove.vAllowedDistance = 0.0;
            }
            if (!Double.isInfinite(Bridge1_9.getLevitationAmplifier(player))) {
                // Levitation forces players to ascend and does not work in liquids, so thankfully we don't have to account for that, other than stuck-speed.
                thisMove.vAllowedDistance += (0.05 * (Bridge1_9.getLevitationAmplifier(player) + 1) - lastMove.yDistance) * 0.2;
            }
            else if (BridgeMisc.hasGravity(player)) {
                // Only apply gravity if the player can be affected by it (slowfall simply reduces gravity)
                thisMove.vAllowedDistance -= !Double.isInfinite(Bridge1_13.getSlowfallingAmplifier(player)) && lastMove.yDistance <= 0.0 ? Magic.DEFAULT_SLOW_FALL_GRAVITY : Magic.DEFAULT_GRAVITY;
            }
            // Friction.
            // NOTE: Unlike horizontal friction, vertical friction is not assigned to the "f" variable (at the beginning of the travel() function in EntityLiving)
            thisMove.vAllowedDistance *= data.lastFrictionVertical;
            // Stuck-speed with the updated multiplier
            thisMove.vAllowedDistance *= (double) data.nextStuckInBlockVertical;
            // TODO: Needs to be adjusted for on ground pushing
            if (lastMove.slowedByUsingAnItem && !thisMove.slowedByUsingAnItem && thisMove.isRiptiding) {
                // Riptide works by propelling the player in air after releasing the trident (the effect only pushes the player, unless is on ground)
                final double RiptideLevel = BridgeEnchant.getRiptideLevel(player);
                if (RiptideLevel > 0.0) {
                    // Compute the force of the push
                    float x = -TrigUtil.sin(to.getYaw() * TrigUtil.toRadians) * TrigUtil.cos(to.getPitch() * TrigUtil.toRadians);
                    float y = -TrigUtil.sin(to.getPitch() * TrigUtil.toRadians);
                    float z = TrigUtil.cos(to.getYaw() * TrigUtil.toRadians) * TrigUtil.cos(to.getPitch() * TrigUtil.toRadians);
                    float distance = MathUtil.sqrt(x*x + y*y + z*z);
                    float force = 3.0f * ((1.0f + (float) RiptideLevel) / 4.0f);
                    y *= force / distance;
                    // Set the new distances: the game calls the push method, which means the new distance is simply added to the previous delta
                    thisMove.vAllowedDistance += (double) y;
                    // here, the games calls the actual moving method instead.
                    // Lost ground won't be a problem, because item-use slows player down.
                    if (from.isOnGround()) {
                        thisMove.vAllowedDistance += 1.1999999f;
                    }
                    player.sendMessage("Trident propel(v): " + StringUtil.fdec6.format(thisMove.yDistance) + " / " + StringUtil.fdec6.format(thisMove.vAllowedDistance));
                } 
            }
            // Workarounds need to be tested at the very end.
            if (AirWorkarounds.checkPostPredictWorkaround(data, fromOnGround, toOnGround, from, to, thisMove.vAllowedDistance, player, isNormalOrPacketSplitMove)) {
                // Override the prediction (just allow the movement in this case.)
                thisMove.vAllowedDistance = thisMove.yDistance;
                player.sendMessage("ID: " + (!justUsedWorkarounds.isEmpty() ? StringUtil.join(justUsedWorkarounds, " , ") : ""));
            }
        }

        /** Expected difference from current to allowed */       
        final double offset = thisMove.yDistance - thisMove.vAllowedDistance; 
        if (Math.abs(offset) < Magic.PREDICTION_EPSILON) {
            // Accuracy margin.
        }
        else {
            // If velocity can be used for compensation, use it.
            if (data.getOrUseVerticalVelocity(yDistance) == null) {
                vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(offset));
                tags.add("vdistrel");
            }
        }
        
    
        // (All the checks below are to be considered a corollary to vDistRel. Virtually, the cheat attempt will always get caught by vDistRel first)
        ///////////////////////////////////////////
        // Check on change of Y direction        //
        ///////////////////////////////////////////
        /* Not on ground, not on climbables, not in liquids, not in stuck-speed, no lostground (...) */
        final boolean fullyInAir = !resetFrom && !resetTo && !(data.timeRiptiding + 2000 > now);
        final boolean yDirectionSwitch = lastMove.toIsValid && lastMove.yDistance != yDistance && (yDistance <= 0.0 && lastMove.yDistance >= 0.0 || yDistance >= 0.0 && lastMove.yDistance <= 0.0); 

        if (fullyInAir && yDirectionSwitch) {
            if (yDistance > 0.0) {
                // TODO: Demand consuming queued velocity for valid change (!).
                if (lastMove.touchedGround || lastMove.to.extraPropertiesValid && lastMove.to.resetCond) {
                    // Change to increasing phase
                    tags.add("y_switch_inc");
                }
                else {
                    // Moving upwards after falling without having touched the ground.
                    if (data.bunnyhopDelay < 9 && !((lastMove.touchedGround || lastMove.from.onGroundOrResetCond) && lastMove.yDistance == 0D) 
                        && data.getOrUseVerticalVelocity(yDistance) == null
                        && !Bridge1_13.isRiptiding(player) && !lastMove.isRiptiding) {
                        vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.max(Magic.GRAVITY_MIN, Math.abs(yDistance))); // Ensure that players cannot get a VL of 0.
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
        return new double[]{thisMove.vAllowedDistance, vDistanceAboveLimit};
    }


    /**
     * After-failure checks for horizontal distance.
     * Velocity, block move and reset-item.
     * 
     * @param player
     * @param multiMoveCount
     * @param from
     * @param to
     * @param hAllowedDistance
     * @param hDistanceAboveLimit
     * @param thisMove
     * @param lastMove
     * @param debug
     * @param data
     * @param cc
     * @param pData
     * @param tick
     * @param useBlockChangeTracker
     * @param fromOnGround
     * @param toOnGround
     * @param isNormalOrPacketSplitMove
     * @return hAllowedDistance, hDistanceAboveLimit, hFreedom
     */
    private double[] hDistAfterFailure(final Player player, final int multiMoveCount,
                                       final PlayerLocation from, final PlayerLocation to, 
                                       double hAllowedDistance, double hDistanceAboveLimit, 
                                       final PlayerMoveData thisMove, final PlayerMoveData lastMove, final boolean debug,
                                       final MovingData data, final MovingConfig cc, final IPlayerData pData, final int tick, 
                                       boolean useBlockChangeTracker, final boolean fromOnGround, final boolean toOnGround,
                                       final boolean isNormalOrPacketSplitMove) {
        final CombinedData cData = pData.getGenericInstance(CombinedData.class);
        // 1: Attempt to force-release the item upon a NoSlow Violation, if set so in the configuration.
        //    This is less invasive than a direct set back as item-use is handled quite badly in this game.
        if (cc.survivalFlyResetItem && hDistanceAboveLimit >= Magic.PREDICTION_EPSILON && BridgeMisc.isUsingItem(player)) {
            pData.requestItemUseResync();
            tags.add("itemreset");
            if (!BridgeMisc.isUsingItem(player)) {
                double[] estimationRes = hDistRel(from, to, pData, player, data, thisMove, lastMove, cc, tick, useBlockChangeTracker, 
                                                  fromOnGround, toOnGround, debug, multiMoveCount, isNormalOrPacketSplitMove);
                hAllowedDistance = estimationRes[0];
                hDistanceAboveLimit = estimationRes[1];
            }
        }

        // 2: Check being moved by blocks.
        // 1.025 is a Magic value
        // Why the hell would block pushing be limited to distances lower than 1.025?? 
        // Yet another case of undocumented Magic left by asofold, great... Comment it out for now.
        if (cc.trackBlockMove && hDistanceAboveLimit > 0.0) { // && hDistanceAboveLimit < 1.025) {
            // Push by 0.49-0.51 in one direction. Also observed 1.02.
            // TODO: Better also test if the per axis distance is equal to or exceeds hDistanceAboveLimit?
            // TODO: The minimum push value can be misleading (blocked by a block?)
            final double xDistance = to.getX() - from.getX();
            final double zDistance = to.getZ() - from.getZ();
            if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, xDistance < 0 ? Direction.X_NEG : Direction.X_POS, 0.05)) {
                hAllowedDistance = thisMove.hDistance; 
                hDistanceAboveLimit = 0.0;
            }
            else if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, zDistance < 0 ? Direction.Z_NEG : Direction.Z_POS, 0.05)) {
                hAllowedDistance = thisMove.hDistance; 
                hDistanceAboveLimit = 0.0;
            }
        }

        // 3: Check velocity.
        // TODO: Implement Asofold's fix to prevent too easy abuse:
        // See: https://github.com/NoCheatPlus/Issues/issues/374#issuecomment-296172316
        double hFreedom = 0.0; // Horizontal velocity used.
        if (hDistanceAboveLimit > 0.0) {
            hFreedom = data.getHorizontalFreedom();
            if (hFreedom < hDistanceAboveLimit) {
                // Distance above limit is still greater. Try using queued velocity if possible.
                hFreedom += data.useHorizontalVelocity(hDistanceAboveLimit - hFreedom);
                // If using queued horizontal velocity, update Improbable's entries/buckets without adding anything.
                // (We want to keep the check 'vigilant' for potential abuses)
                Improbable.update(System.currentTimeMillis(), pData);
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
     * TODO: Recode to a vDistRel-like implementation.
     * 
     * @param from
     * @param to
     * @param toOnGround
     * @param yDistance
     * @param data
     * @return vAllowedDistance, vDistanceAboveLimit
     */
    private double[] vDistLiquid(final PlayerMoveData thisMove, final PlayerLocation from, final PlayerLocation to, 
                                 final boolean toOnGround, final double yDistance, final PlayerMoveData lastMove, 
                                 final MovingData data, final Player player, final MovingConfig cc, final IPlayerData pData, final boolean fromOnGround) {
        final double yDistAbs = Math.abs(yDistance);
        /** If a server with version lower than 1.13 has ViaVer installed, allow swimming */
        final boolean swimmingInLegacyServer = !ServerIsAtLeast1_13 && player.isSprinting() && pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_13);
        final double baseSpeed = thisMove.from.onGround ? Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player) || swimmingInLegacyServer) + 0.1 : Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player) || swimmingInLegacyServer);
        /** Slow fall gravity is applied only if the player is not sneaking (in that case, the player will descend in water with regular gravity) */
        // TODO: Rough... Needs a better modeling.
        final boolean Slowfall = !pData.isSneaking() && thisMove.hasSlowfall;

        //////////////////////////////////////////////////////
        // 0: Checks for no gravity when moving in a liquid.//
        //////////////////////////////////////////////////////
        if (thisMove.yDistance == 0.0 && lastMove.yDistance == 0.0 && lastMove.toIsValid
            && thisMove.hDistance >= 0.090 && lastMove.hDistance >= 0.090 // Do not check lower speeds. The cheat would be purely cosmetic at that point, it wouldn't offer any advantage.
            && BlockProperties.isLiquid(to.getTypeId()) 
            && BlockProperties.isLiquid(from.getTypeId())
            && !fromOnGround && !toOnGround
            && !from.isHeadObstructed() && !to.isHeadObstructed() 
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
            final double vAllowedDistance = step ? cc.sfStepHeight : minJumpGain;
            tags.add("liquidground");
            return new double[]{vAllowedDistance, yDistance - vAllowedDistance};
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
        // TODO (Still quite rough: Minecraft acceleration(s) is different actually, but it doesn't seem to work at all with NCP...)
        if (from.isInBubbleStream() || to.isInBubbleStream()) {
            // Minecraft distinguishes above VS inside, so we need to do it as well.
            // This is what it does to determine if the player is above the stream (check for air above)
            tags.add("bblstrm_asc");
            if (BlockProperties.isAir(from.getTypeIdAbove())) {
                double vAllowedDistance = Math.min(1.8, lastMove.yDistance + 0.2);
                return new double[]{vAllowedDistance, yDistAbs - vAllowedDistance};
            }
            // Inside.
            if (lastMove.yDistance < 0.0 && yDistance < 0.0
                && !thisMove.headObstructed) {
                // == 0.0 is covered by waterwalk
                // If inside a bubble column, and the player is getting pushed, they cannot descend
                return new double[]{0.0, yDistAbs};
            }
            double vAllowedDistance = Math.min(Magic.bubbleStreamAscend, lastMove.yDistance + 0.2);
            return new double[]{vAllowedDistance, yDistance - vAllowedDistance}; 
        }
        // Check for drag
        // (Seems to work OK, unlike ascending)
        if (from.isDraggedByBubbleStream() || to.isDraggedByBubbleStream()) {
            tags.add("bblstrm_desc");
            // Above
            if (BlockProperties.isAir(from.getTypeIdAbove())) {
                // -0.03 is the effective acceleration, capped at -0.9
                double vAllowedDistance = Math.max(-0.9, lastMove.yDistance - 0.03);
                return new double[]{vAllowedDistance, yDistance < vAllowedDistance ? Math.abs(yDistance - vAllowedDistance) : 0.0}; 
            }
            // Inside
            if (lastMove.yDistance > 0.0 && yDistance > 0.0) {
                // If inside a bubble column, and the player is getting dragged, they cannot ascend
                return new double[]{0.0, yDistAbs};
            }
            double vAllowedDistance = Math.max(-0.3, lastMove.yDistance - 0.03);
            return new double[]{vAllowedDistance, yDistance < vAllowedDistance ? Math.abs(yDistance - vAllowedDistance) : 0.0}; 
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
        if (pData.isSneaking() 
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
     * @param player
     * @param from
     * @param to
     * @param fromOnGround 
     * @param toOnGround 
     * @param pData
     * @param thisMove
     * @param lastMove
     * @param yDistance
     * @param data
     * @param cc
     * @return vAllowedDistance, vDistanceAboveLimit
     */
    private double[] vDistClimbable(final Player player, final PlayerLocation from, final PlayerLocation to,
                                    final boolean fromOnGround, final boolean toOnGround, final IPlayerData pData,
                                    final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
                                    final double yDistance, final MovingData data, final MovingConfig cc) {
        data.clearActiveHorVel(); // Might want to clear ALL horizontal vel.
        double vDistanceAboveLimit = 0.0;
        double yDistAbs = Math.abs(yDistance);
        final double maxJumpGain = data.liftOffEnvelope.getJumpGain(data.jumpAmplifier) + 0.0001;
        /** Climbing a ladder in water and exiting water for whatever reason speeds up the player a lot in that one transition ... */
        final boolean waterStep = lastMove.from.inLiquid && yDistAbs < Magic.swimBaseSpeedV(Bridge1_13.hasIsSwimming());
        // Quick, temporary fix for scaffolding block
        final boolean scaffolding = from.isOnGround() && from.getBlockY() == Location.locToBlock(from.getY()) && yDistance > 0.0 && yDistance < maxJumpGain;
        double vAllowedDistance = (waterStep || scaffolding) ? yDistAbs : yDistance < 0.0 ? Magic.climbSpeedDescend : Magic.climbSpeedAscend;
        final double maxJumpHeight = LiftOffEnvelope.NORMAL.getMaxJumpHeight(0.0) + (data.jumpAmplifier > 0 ? (0.6 + data.jumpAmplifier - 1.0) : 0.0);
        
        // Workaround for ladders that have a much bigger collision box, but much smaller hitbox (We do not distinguish the two). 
        if (yDistAbs > vAllowedDistance) {
            if (from.isOnGround(BlockFlags.F_CLIMBABLE) && to.isOnGround(BlockFlags.F_CLIMBABLE)) {
                // Stepping a up a block (i.e.: stepping up stairs with vines/ladders on the side), allow this movement.
                vDistanceAboveLimit = Math.max(vDistanceAboveLimit, yDistAbs - cc.sfStepHeight);
                tags.add("climbstep");
            }
            else if (from.isOnGround(maxJumpHeight, 0D, 0D, BlockFlags.F_CLIMBABLE)) {
                // If ground is found within the allowed jump height, we check speed against jumping motion not climbing motion, in order to still catch extremely fast / instant modules.
                // (We have to check for full height because otherwise the immediate move after jumping will yield a false positive, as air gravity will be applied, which is stll higher than climbing motion)
                // In other words, this means that as long as ground is found within the allowed height, players will be able to speed up to 0.42 on climbables.
                // The advantage is pretty insignificant while granting us some leeway with false positives and cross-versions compatibility issues.
                if (yDistance > maxJumpGain) {
                    // If the player managed to exceed the ordinary jumping motion while on a climbable, we know for sure they're cheating.
                    vDistanceAboveLimit = Math.max(vDistanceAboveLimit, yDistAbs - vAllowedDistance);
                    Improbable.check(player, (float)yDistAbs, System.currentTimeMillis(), "moving.survivalfly.instantclimb", pData);
                    tags.add("instantclimb");
                }
                // Ground is within reach and speed is lower than 0.42. Allow the movement.
                else tags.add("climbheight("+ StringUtil.fdec3.format(maxJumpHeight) +")");
            }
            else {
                // Ground is out reach and motion is still higher than legit. We can safely throw a VL at this point.
                vDistanceAboveLimit = Math.max(vDistanceAboveLimit, yDistAbs - vAllowedDistance);
                tags.add("climbspeed");
            }
        }
        
        // Can't climb up with vine not attached to a solid block (legacy).
        if (yDistance > 0.0 && !thisMove.touchedGround && !from.canClimbUp(maxJumpHeight)) {
            vDistanceAboveLimit = Math.max(vDistanceAboveLimit, yDistance);
            vAllowedDistance = 0.0;
            tags.add("climbdetached");
        }

        // Do allow friction with velocity.
        // TODO: Actual friction or limit by absolute y-distance?
        // TODO: Looks like it's only a problem when on ground?
        if (vDistanceAboveLimit > 0.0 && thisMove.yDistance > 0.0 
            && lastMove.yDistance - (Magic.GRAVITY_MAX + Magic.GRAVITY_MIN) / 2.0 > thisMove.yDistance) {
            vDistanceAboveLimit = 0.0;
            vAllowedDistance = yDistance;
            tags.add("vfrict_climb");
        }

        // Do allow vertical velocity.
        // TODO: Looks like less velocity is used here (normal hitting 0.361 of 0.462).
        if (vDistanceAboveLimit > 0.0 && data.getOrUseVerticalVelocity(yDistance) != null) {
            vDistanceAboveLimit = 0.0;
        }
        return new double[]{vAllowedDistance, vDistanceAboveLimit};
    }

    /**
     * Violation handling put here to have less code for the frequent processing of check.
     * @param now
     * @param result
     * @param player
     * @param from
     * @param to
     * @param data
     * @param cc
     * @return
     */
    private Location handleViolation(final long now, final double result, 
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
     * @param player
     * @param loc
     * @param blockCache 
     * @param cc
     * @param data
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
                player.teleport(newTo, BridgeMisc.TELEPORT_CAUSE_CORRECTION_OF_POSITION);
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
     * @param player
     * @param to
     * @param data
     * @param cc
     * @param hDistance
     * @param hAllowedDistance
     * @param hFreedom
     * @param yDistance
     * @param vAllowedDistance
     * @param fromOnGround
     * @param resetFrom
     * @param toOnGround
     * @param resetTo
     */
    private void outputDebug(final Player player, final PlayerLocation to, final PlayerLocation from,
                             final MovingData data, final MovingConfig cc, 
                             final double hDistance, final double hAllowedDistance, final double hFreedom, 
                             final double yDistance, final double vAllowedDistance,
                             final boolean fromOnGround, final boolean resetFrom, 
                             final boolean toOnGround, final boolean resetTo,
                             final PlayerMoveData thisMove, double vDistanceAboveLimit) {

        // TODO: Show player name once (!)
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final double yDistDiffEx = yDistance - vAllowedDistance;
        final double hDistDiffEx = thisMove.hDistance - thisMove.hAllowedDistance;
        final StringBuilder builder = new StringBuilder(500);
        builder.append(CheckUtils.getLogMessagePrefix(player, type));
        final String hVelUsed = hFreedom > 0 ? " / hVelUsed: " + StringUtil.fdec3.format(hFreedom) : "";
        builder.append("\nOnGround: " + (from.isHeadObstructed(0.2, false) ? "(head obstr.) " : from.isSlidingDown() ? "(sliding down) " : "") + (thisMove.touchedGroundWorkaround ? "(lost ground) " : "") + (fromOnGround ? "onground -> " : (resetFrom ? "resetcond -> " : "--- -> ")) + (toOnGround ? "onground" : (resetTo ? "resetcond" : "---")) + ", jumpPhase: " + data.sfJumpPhase + ", LiftOff: " + data.liftOffEnvelope.name() + "(" + data.insideMediumCount + ")");
        final String dHDist = lastMove.toIsValid ? "(" + StringUtil.formatDiff(hDistance, lastMove.hDistance) + ")" : "";
        final String dYDist = lastMove.toIsValid ? "(" + StringUtil.formatDiff(yDistance, lastMove.yDistance)+ ")" : "";
        builder.append("\n" + " hDist: " + StringUtil.fdec6.format(hDistance) + dHDist + " / offset: " + hDistDiffEx + " / predicted: " + StringUtil.fdec6.format(hAllowedDistance) + hVelUsed +
                       "\n" + " vDist: " + StringUtil.fdec6.format(yDistance) + dYDist + " / offset: " + yDistDiffEx + " / predicted: " + StringUtil.fdec6.format(vAllowedDistance) + " , setBackY: " + (data.hasSetBack() ? (data.getSetBackY() + " (jump height: " + StringUtil.fdec3.format(to.getY() - data.getSetBackY()) + " / max jump height: " + data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier) + ")") : "?"));
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
            builder.append("\n" + " Workarounds: " + StringUtil.join(justUsedWorkarounds, " , "));
        }
        builder.append("\n");
        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
    }

    
    private void logPostViolationTags(final Player player) {
        debug(player, "SurvivalFly Post violation handling tag update:\n" + StringUtil.join(tags, "+"));
    }
}