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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.magic.LostGround;
import fr.neatmonster.nocheatplus.checks.moving.magic.Magic;
import fr.neatmonster.nocheatplus.checks.moving.magic.MagicAir;
import fr.neatmonster.nocheatplus.checks.moving.magic.MagicLiquid;
import fr.neatmonster.nocheatplus.checks.moving.model.LiftOffEnvelope;
import fr.neatmonster.nocheatplus.checks.moving.model.LocationData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.util.AuxMoving;
import fr.neatmonster.nocheatplus.checks.moving.util.MovingUtil;
import fr.neatmonster.nocheatplus.checks.moving.velocity.VelocityFlags;
import fr.neatmonster.nocheatplus.checks.workaround.WRPT;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeEnchant;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.components.modifier.IAttributeAccess;
import fr.neatmonster.nocheatplus.components.registry.event.IGenericInstanceHandle;
import fr.neatmonster.nocheatplus.logging.Streams;
import fr.neatmonster.nocheatplus.permissions.Permissions;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;
import fr.neatmonster.nocheatplus.utilities.PotionUtil;
import fr.neatmonster.nocheatplus.utilities.StringUtil;
import fr.neatmonster.nocheatplus.utilities.ds.count.ActionAccumulator;
import fr.neatmonster.nocheatplus.utilities.collision.CollisionUtil;
import fr.neatmonster.nocheatplus.utilities.location.LocUtil;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.location.TrigUtil;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;

/**
 * The counterpart to the CreativeFly check. People that are not allowed to fly get checked by this. It will try to
 * identify when they are jumping, check if they aren't jumping too high or far, check if they aren't moving too fast on
 * normal ground, while sprinting, sneaking, swimming, etc.
 */
public class SurvivalFly extends Check {

    // Tags
    private static final String DOUBLE_BUNNY = "doublebunny";

    // Other.
    /** Bunny-hop delay. */
    private static final int   bunnyHopMax = 10;
    /** Divisor vs. last hDist for minimum slow down. */
    private static final double bunnyDivFriction = 160.0; // Rather in-air, blocks would differ by friction.
    
    public boolean snowFix;



    // TODO: Friction by block to walk on (horizontal only, possibly to be in BlockProperties rather).

    /** To join some tags with moving check violations. */
    private final ArrayList<String> tags = new ArrayList<String>(15);
    private final ArrayList<String> justUsedWorkarounds = new ArrayList<String>();

    private final Set<String> reallySneaking = new HashSet<String>(30);

    /** For temporary use: LocUtil.clone before passing deeply, call setWorld(null) after use. */
    private final Location useLoc = new Location(null, 0, 0, 0);

    private final BlockChangeTracker blockChangeTracker;

    // TODO: handle
    private final AuxMoving aux = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstance(AuxMoving.class);

    private IGenericInstanceHandle<IAttributeAccess> attributeAccess = NCPAPIProvider.getNoCheatPlusAPI().getGenericInstanceHandle(IAttributeAccess.class);


    /**
     * Instantiates a new survival fly check.
     */
    public SurvivalFly() {
        super(CheckType.MOVING_SURVIVALFLY);
        blockChangeTracker = NCPAPIProvider.getNoCheatPlusAPI().getBlockChangeTracker();
    }

    /**
     * 
     * @param player
     * @param from
     * @param to
     * @param multiMoveCount
     *            =: Ordinary, 1/2: first/second of a split move.
     * @param data
     * @param cc
     * @param tick
     * @param now
     * @param useBlockChangeTracker
     * @return
     */
    public Location check(final Player player, 
            final PlayerLocation from, final PlayerLocation to, 
            final int multiMoveCount, 
            final MovingData data, final MovingConfig cc, final IPlayerData pData,
            final int tick, final long now, final boolean useBlockChangeTracker) {
	    
	    
        tags.clear();
        final boolean debug = pData.isDebugActive(type);
        if (debug) {
            justUsedWorkarounds.clear();
            data.ws.setJustUsedIds(justUsedWorkarounds);
        }
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final boolean isSamePos = from.isSamePos(to);

        // Calculate some distances.
        final double xDistance, yDistance, zDistance, hDistance;
        final boolean hasHdist;
        if (isSamePos) {
            // TODO: Could run a completely different check here (roughly none :p).
            xDistance = yDistance = zDistance = hDistance = 0.0;
            hasHdist = false;
        }
        else {
            xDistance = to.getX() - from.getX();
            yDistance = thisMove.yDistance;
            zDistance = to.getZ() - from.getZ();
            if (xDistance == 0.0 && zDistance == 0.0) {
                hDistance = 0.0;
                hasHdist = false;
            }
            else {
                hasHdist = true;
                hDistance = thisMove.hDistance;
            }
        }

        // Recover from data removal (somewhat random insertion point).
        if (data.liftOffEnvelope == LiftOffEnvelope.UNKNOWN) {
            data.adjustMediumProperties(from);
        }

        // Set some flags.
        final boolean fromOnGround = thisMove.from.onGround;
        // TODO: Work in the past ground stuff differently (thisMove, touchedGround?, from/to ...)
        final boolean toOnGround = thisMove.to.onGround 
                || useBlockChangeTracker && toOnGroundPastStates(from, to, thisMove, tick, data, cc);
        final boolean resetTo = toOnGround || to.isResetCond();

        // Determine if the player is actually sprinting.
        final boolean sprinting;
        if (data.lostSprintCount > 0) {
            // Sprint got toggled off, though the client is still (legitimately) moving at sprinting speed.
            // NOTE: This could extend the "sprinting grace" period, theoretically, until on ground.
            if (resetTo && (fromOnGround || from.isResetCond()) || hDistance <= Magic.WALK_SPEED) {
                // Invalidate.
                data.lostSprintCount = 0;
                tags.add("invalidate_lostsprint");
                if (now <= data.timeSprinting + cc.sprintingGrace) {
                    sprinting = true;
                }
                else {
                    sprinting = false;
                }
            }
            else {
                tags.add("lostsprint");
                sprinting = true;
                if (data.lostSprintCount < 3 && toOnGround || to.isResetCond()) {
                    data.lostSprintCount = 0;
                }
                else {
                    data.lostSprintCount --;
                }
            }
        }
        else if (now <= data.timeSprinting + cc.sprintingGrace) {
            // Within grace period for hunger level being too low for sprinting on server side (latency).
            if (now != data.timeSprinting) {
                tags.add("sprintgrace");
            }
            sprinting = true;
        }
        else {
            sprinting = false;
        }

        // Use the player-specific walk speed.
        // TODO: Might get from listener.
        // TODO: Use in lostground?
        thisMove.walkSpeed = Magic.WALK_SPEED * ((double) data.walkSpeed / Magic.DEFAULT_WALKSPEED);

        setNextFriction(thisMove, data, cc);

        /////////////////////////////////
        // Mixed checks (lost ground).
        /////////////////////////////////


        final boolean resetFrom;
        if (fromOnGround || from.isResetCond()) {
            resetFrom = true;
        }
        // TODO: Extra workarounds for toOnGround (step-up is a case with to on ground)?
        else if (isSamePos) {
            // TODO: This isn't correct, needs redesign.
            if (useBlockChangeTracker && from.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, 
                    data.blockChangeRef, tick)) {
                // TODO: Quick addition. Reconsider entry points etc.
                resetFrom = true;
                tags.add("pastground_from");
            }
            else if (lastMove.toIsValid) {
                // Note that to is not on ground either.
                resetFrom = LostGround.lostGroundStill(player, from, to, hDistance, yDistance, sprinting, lastMove, data, cc, tags);
            }
            else {
                resetFrom = false;
            }
        }
        else {
            // "Lost ground" workaround.
            // TODO: More refined conditions possible ?
            // TODO: Consider if (!resetTo) ?
            // Check lost-ground workarounds.
            resetFrom = LostGround.lostGround(player, from, to, hDistance, yDistance, sprinting, lastMove, data, cc, 
                    useBlockChangeTracker ? blockChangeTracker : null, tags);
            // Note: if not setting resetFrom, other places have to check assumeGround...
        }

        if (thisMove.touchedGround ) {
            if (!thisMove.from.onGround && !thisMove.to.onGround) {
                // Lost ground workaround has just been applied, check resetting of the dirty flag.
                // TODO: Always/never reset with any ground touched?
                data.resetVelocityJumpPhase(tags);
            }
            else if (multiMoveCount == 0 && thisMove.from.onGround && !lastMove.touchedGround
                    && TrigUtil.isSamePosAndLook(thisMove.from, lastMove.to)) {
                // Ground somehow appeared out of thin air (block place).
                data.setSetBack(from);
                if (debug) {
                    debug(player, "Adjust set back on move: from is now on ground.");
                }
            }
        }

        // Renew the "dirty"-flag (in-air phase affected by velocity).
        if (data.isVelocityJumpPhase() || data.resetVelocityJumpPhase(tags)) {
            // (Reset is done after checks run.) 
            tags.add("dirty");
        }

        // Check if head is obstructed.
        //if (!resetFrom || !resetTo) {
        thisMove.headObstructed = (yDistance > 0.0 ? from.isHeadObstructed(yDistance) : from.isHeadObstructed()) 
                //                    || to.isHeadObstructed() // Best not have this one.
                ;
        //}

        // HACK: Force sfNoLowJump by a flag.
        // TODO: Might remove that flag, as the issue for trying this has been resolved differently (F_HEIGHT8_1).
        // TODO: Consider setting on ground_height always?
        if ((from.getBlockFlags() & BlockProperties.F_ALLOW_LOWJUMP) != 0) {
            // TODO: Specialize - test for foot region?
            data.sfNoLowJump = true;
        }
        // Moving half on farmland(or end_potal_frame) and half on water
        data.newHDist = BlockProperties.collides(from.getBlockCache(), from.getMinX(), from.getMinY(), from.getMinZ(), from.getMaxX(), from.getMaxY(), from.getMaxZ(), BlockProperties.F_HEIGHT16_15) && (from.isInWater() || to.isInWater());

	// Waterlogged 
        if (isWaterlogged(from)) {
            // thisMove.from.onGround = false; ?
            thisMove.from.inWater = true;
            thisMove.from.inLiquid = true;
        }
        if (isWaterlogged(to)) {
            // thisMove.to.onGround = false; ?
            thisMove.to.inWater = true;
            thisMove.to.inLiquid = true;
        }
	    
        snowFix = (from.getBlockFlags() & BlockProperties.F_HEIGHT_8_INC) != 0;

        //////////////////////
        // Horizontal move.
        //////////////////////

        // TODO: Account for lift-off medium / if in air [i.e. account for medium + friction]?

        // Alter some data / flags.
        data.bunnyhopDelay--; // TODO: Design to do the changing at the bottom? [if change: check limits in bunnyHop(...)]
        data.lastbunnyhopDelay -= data.lastbunnyhopDelay > 0 ? 1 : 0;

        // Set flag for swimming with the flowing direction of liquid.
        thisMove.downStream = from.isDownStream(xDistance, zDistance);
        // Pass downstream for later uses
        if (!data.isdownstream) data.isdownstream = thisMove.downStream;
        else if (from.isOnGround() && !from.isInLiquid()) data.isdownstream = false;

        // Handle ice.
        // TODO: Re-model ice stuff and other (e.g. general thing: ground-modifier + reset conditions).
        // 0: Jump with head obstructed and trap door on ice
        if (thisMove.headObstructed && from.isOnIce() && (from.getBlockFlags() & BlockProperties.F_ATTACHED_LOW2_SNEW) != 0) {
            // Actually no way to detect they are about to jump!
            data.sfOnIce = 24;
            data.bunnyhopTick = 4;
        }
        // 0: Jump
        if ((thisMove.from.onIce && !thisMove.to.onIce && !data.sfLowJump) 
           // 0: Jump with head obstructed
           || (thisMove.headObstructed && thisMove.yDistance > 0.01 && lastMove.from.onIce)
           ) {
            // TODO: 1. Test if this can simply be removed. 2. Ensure data.sfOnIce resets with a violation.
            data.sfOnIce = 20;
        }
        else if (data.sfOnIce > 0) {
            // TODO: Here some friction might apply, could become a general thing with bunny and other.
            // TODO: Other reset conditions.
            data.sfOnIce--;
        }

        // TODO: Remove these local variables ?
        double hAllowedDistance = 0.0, hDistanceAboveLimit = 0.0, hFreedom = 0.0;
        if (hasHdist) {
            final double attrMod = attributeAccess.getHandle().getSpeedAttributeMultiplier(player);
            // Check allowed vs. taken horizontal distance.
            // Get the allowed distance.
            hAllowedDistance = setAllowedhDist(player, sprinting, thisMove, data, cc, pData, from, false);
            // Judge if horizontal speed is above limit.
            hDistanceAboveLimit = hDistance - hAllowedDistance;

            // TODO: Remove this
	    if ((Bridge1_13.isRiptiding(player) || data.timeRiptiding + 4000 > now) && hDistanceAboveLimit < 3.0) {
            	hDistanceAboveLimit = 0.0;
            }
            // Velocity, buffers and after failure checks.
            if (hDistanceAboveLimit > 0) {
                // TODO: Move more of the workarounds (buffer, bunny, ...) into this method.
                final double[] res = hDistAfterFailure(player, from, to, 
                        hAllowedDistance, hDistanceAboveLimit, sprinting, thisMove, lastMove, 
                        data, cc, pData, false);
                hAllowedDistance = res[0];
                hDistanceAboveLimit = res[1];
                hFreedom = res[2];
            }
            else {
                data.clearActiveHorVel();
                hFreedom = 0.0;
                if (resetFrom && data.bunnyhopDelay <= 6) {
                    data.bunnyhopDelay = 0;
                }
            }

            // hacc (if enabled, always update)
            final double fcmhv = Math.max(1.0, Math.min(10.0, thisMove.hDistance / thisMove.hAllowedDistanceBase));
            data.combinedMediumHCount ++;
            data.combinedMediumHValue += fcmhv;
            // TODO: Balance, where to check / use (...).
            if (data.combinedMediumHCount > 30) {
                // TODO: Early trigger (> 0,1,2,5?), for way too high values. [in that case don't reset]
                final double fcmh = data.combinedMediumHValue / (double) data.combinedMediumHCount;
                final double limitFCMH;
                // TODO: with buffer use, might want to skip.
                if (data.liftOffEnvelope == LiftOffEnvelope.NORMAL) {
                    limitFCMH = 1.34;
                }
                else if (data.liftOffEnvelope == LiftOffEnvelope.LIMIT_LIQUID 
                        || data.liftOffEnvelope == LiftOffEnvelope.LIMIT_NEAR_GROUND) {
                    // limitFCMH = 1.05; // Seems to work on 1.10
                    limitFCMH = 1.1; // 1.8.8 in-water moves with jumping near/on surface. 1.2 is max factor for one move (!).
                    // TODO: Version+context dependent setting and/or confine by in-water moves, whatever.
                }
                else {
                    limitFCMH = 1.0;
                }
                // TODO: Configurable / adjust by medium type.
                // TODO: Instead of velocityJumpPhase account for friction directly?
                // TODO: Fly-NoFly + bunny-water transitions pose issues.
                if (fcmh > limitFCMH && !data.isVelocityJumpPhase()) {
                    hDistanceAboveLimit = hDistance * (fcmh - limitFCMH);
                    tags.add("hacc");
                    // Reset for now.
                    data.combinedMediumHCount = 0;
                    data.combinedMediumHValue = 0.0;
                }
                else {
                    // TODO: Other cases (1.0, between, ...)?
                    data.combinedMediumHCount = 1;
                    data.combinedMediumHValue = fcmhv;
                }
            }

            // Prevent players from walking on a liquid in a too simple way.
            // TODO: Find something more effective against more smart methods (limitjump helps already).
            // TODO: yDistance == 0D <- should there not be a tolerance +- or 0...x ?
            // TODO: Complete re-modeling.
            if (!pData.hasPermission(Permissions.MOVING_SURVIVALFLY_WATERWALK, player)) {
               if (hDistanceAboveLimit <= 0D && hDistance > 0.1D && yDistance == 0D 
		    && !toOnGround && !fromOnGround 
                    && lastMove.toIsValid && lastMove.yDistance == 0D 
                    && BlockProperties.isLiquid(to.getTypeId()) && BlockProperties.isLiquid(from.getTypeId())
                    && !from.isHeadObstructed() && !to.isHeadObstructed() && !Bridge1_13.isSwimming(player) // TODO: Might decrease margin here.
                    ) {
                // TODO: Relative hdistance.
                // TODO: Might check actual bounds (collidesBlock). Might implement + use BlockProperties.getCorrectedBounds or getSomeHeight.
                hDistanceAboveLimit = Math.max(hDistanceAboveLimit, hDistance);
                tags.add("waterwalk");
               }
            
	        // Detects walking directly above water
                Block blockUnder = player.getLocation().subtract(0, 0.3, 0).getBlock();
                Material blockAbove = player.getLocation().add(0, 0.10, 0).getBlock().getType();
                final boolean islowheigh = BlockProperties.collides(from.getBlockCache(), from.getMinX(), from.getMinY(), from.getMinZ(), from.getMaxX(), from.getMaxY(), from.getMaxZ(), BlockProperties.F_HEIGHT16_15);
                if (blockUnder != null && blockAbove != null && blockUnder.getType().toString().endsWith("WATER") && blockAbove.name().endsWith("AIR")) {
			
                   // hDist and vDist checks, simply checks for horizontal movement with little y distance
                   if (!islowheigh && hDistanceAboveLimit <= 0D && hDistance > 0.11D && yDistance <= 0.1D && !toOnGround && !fromOnGround
                      && lastMove.toIsValid && lastMove.yDistance == yDistance || lastMove.yDistance == yDistance * -1 && lastMove.yDistance != 0D
                      && !from.isHeadObstructed() && !to.isHeadObstructed() && !Bridge1_13.isSwimming(player)) {
			   
                       // Prevent being flagged if a player transitions from a block to water and the player falls into the water.
                       if (!(yDistance < 0 && yDistance != 0 && lastMove.yDistance < 0 && lastMove.yDistance != 0)) {
                           hDistanceAboveLimit = Math.max(hDistanceAboveLimit, hDistance);
                           tags.add("watermove");
                       }
                   }
                }
            }

            // TODO: Complete re-modeling
            // Prevent players from sprinting if they're moving backwards (allow buffers to cover up !?).
            //if (sprinting && data.lostSprintCount == 0 && hDistance > thisMove.walkSpeed * 1.2
            //&& !data.isVelocityJumpPhase() && !thisMove.touchedGroundWorkaround
            //&& !player.hasPotionEffect(PotionEffectType.SPEED) && (attrMod == Double.MAX_VALUE || attrMod <= 1.0)
            //&& !(Bridge1_13.isRiptiding(player) || data.timeRiptiding + 4000 > now) // Quick direction rotate
            //&& !(data.liftOffEnvelope.name().startsWith("LIMIT") || BlockProperties.isLiquid(to.getOrCreateBlockCacheNodeBelow().getType()) || isWaterlogged(from) || isWaterlogged(to))) {
            //    // (Ignore some cases, in order to prevent false positives.)
            //    if (TrigUtil.isMovingBackwards(xDistance, zDistance, LocUtil.correctYaw(from.getYaw())) 
            //    && !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_SPRINTING, player)) {
            //        boolean flag = false;               	
            //        if (data.bunnyhopTick > 0) {
            //            double newwalkSpeed = 0.0;
            //            if (data.bunnyhopTick > 3) newwalkSpeed = thisMove.walkSpeed * 1.4; else newwalkSpeed = thisMove.walkSpeed * 1.27;
            //            if (hDistance > newwalkSpeed) flag = true;
            //        } else flag = true;

            //        // TODO: Better modeling when on ice
            //        // TODO: Add Piston+Slime
            //        // Quick rotate back in-air
            //        if ((!thisMove.from.onGround && !thisMove.to.onGround && Math.abs(lastMove.from.getYaw() - thisMove.to.getYaw()) > 10.0) 
            //           || data.sfOnIce > 5 
            //           || (Bridge1_9.hasLevitation() && CollisionUtil.isCollidingWithEntities(player, true))
            //           || from.isAboveStairs()
            //        ) flag = false;

            //        if (flag) {
            //            // (Might have to account for speeding permissions.)
            //            // TODO: hDistance is too harsh?
            //            hDistanceAboveLimit = Math.max(hDistanceAboveLimit, hDistance) * 0.7;
            //            tags.add("sprintback"); // Might add it anyway.
            //        }
            //    }
            //}

           // Simple way to prevent players from sprinting if they have the blindness effect. 
           // Possibly just merge this with sprintback?
           if (player.isSprinting() && player.hasPotionEffect(PotionEffectType.BLINDNESS)
	       && data.lostSprintCount == 0
               && !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_SPRINTING, player)) {
               hDistanceAboveLimit = Math.max(hDistanceAboveLimit, hDistance);
               tags.add("badsprint");
           }

        }
        else {
            /*
             * TODO: Consider to log and/or remember when this was last time
             * cleared [add time distance to tags/log on violations].
             */
            data.clearActiveHorVel();
            thisMove.hAllowedDistanceBase = 0.0;
            thisMove.hAllowedDistance = 0.0;
            // TODO: Other properties should be set as well?
        }


        //////////////////////////
        // Vertical move.
        //////////////////////////

        
        // Calculate the vertical speed limit based on the current jump phase.
        double vAllowedDistance = 0, vDistanceAboveLimit = 0;
        // Distinguish certain media.
        if (yDistance >= 0.0 && yDistance <= cc.sfStepHeight && toOnGround && fromOnGround ) {
            // Wild-card allow step height from ground to ground.
            // TODO: Which of (fromOnGround || data.noFallAssumeGround || lastMove.toIsValid && lastMove.yDistance < 0.0)?
            vAllowedDistance = cc.sfStepHeight;
        }
        else if (ShouldApplyHBSpeed(from)) {
            final Double Amplifier = PotionUtil.getPotionEffectAmplifier(from.getPlayer(), PotionEffectType.JUMP);
            vAllowedDistance = 0.21 * (Double.isInfinite(Amplifier) ? 1.0 : 1.0 + 0.48*(Amplifier +1));
            if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) vAllowedDistance = 1.5;
            vDistanceAboveLimit = thisMove.yDistance - vAllowedDistance;
        }
        else if (from.isInWeb()) {
            // TODO: Further confine conditions.
            final double[] res = vDistWeb(player, thisMove, toOnGround, hDistanceAboveLimit, now,data,cc,from);
            vAllowedDistance = res[0];
            vDistanceAboveLimit = res[1];
            if (res[0] == Double.MIN_VALUE && res[1] == Double.MIN_VALUE) {
                // Silent set back.
                if (debug) {
                    tags.add("silentsbcobweb");
                    outputDebug(player, to, data, cc, hDistance, hAllowedDistance, hFreedom, 
                            yDistance, vAllowedDistance, fromOnGround, resetFrom, toOnGround, resetTo, thisMove);
                    data.ws.setJustUsedIds(null);
                }
                // (No need for MovingUtil.getApplicableSetBackLocation.)
                return data.getSetBack(to); // (OK)
            }
        }
        else if (from.isOnClimbable()) {
            // Ladder types.
            vDistanceAboveLimit = vDistClimbable(player, from, to, fromOnGround, toOnGround, 
                    thisMove, lastMove, yDistance, data);
        }
        else if (thisMove.from.inLiquid) { // && (Math.abs(yDistance) > 0.2 || to.isInLiquid())) {
            // Swimming...
            final double[] res = vDistLiquid(thisMove, from, to, toOnGround, yDistance, lastMove, data, player);
            vAllowedDistance = res[0];
            vDistanceAboveLimit = res[1];
            if (vDistanceAboveLimit <= 0.0 && yDistance > 0.0 && Math.abs(yDistance) > Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player))) {
                data.setFrictionJumpPhase();
            }
        }
        else {
            final double[] res = vDistAir(now, player, from, fromOnGround, resetFrom, 
                    to, toOnGround, resetTo, hDistanceAboveLimit, yDistance, multiMoveCount, lastMove, 
                    data, cc, pData);
            vAllowedDistance = res[0];
            vDistanceAboveLimit = res[1];
        }

        // Post-check recovery.
        if (useBlockChangeTracker && vDistanceAboveLimit > 0.0 
                // Skip for now: && Math.abs(yDistance) <= 1.55
                ) {
            // TODO: Better place for checking for moved blocks [redesign for intermediate result objects?].
            // Vertical push/pull.
            double[] blockMoveResult = getVerticalBlockMoveResult(yDistance, from, to, tick, data);
            if (blockMoveResult != null) {
                vAllowedDistance = blockMoveResult[0];
                vDistanceAboveLimit = blockMoveResult[1];
            }
        }

        // Push/pull sideways.
        // TODO: Slightly itchy: regard x and z separately (Better in another spot).

        // TODO: on ground -> on ground improvements.

        // Debug output.
        final int tagsLength;
        if (debug) {
            outputDebug(player, to, data, cc, hDistance, hAllowedDistance, hFreedom, 
                    yDistance, vAllowedDistance, fromOnGround, resetFrom, toOnGround, resetTo, thisMove);
            tagsLength = tags.size();
            data.ws.setJustUsedIds(null);
        }
        else {
            tagsLength = 0; // JIT vs. IDE.
        }

        ///////////////////////
        // Handle violations.
        ///////////////////////

        final boolean inAir = Magic.inAir(thisMove);
        final double result = (Math.max(hDistanceAboveLimit, 0D) + Math.max(vDistanceAboveLimit, 0D)) * 100D;
        if (result > 0D) {
            final Location vLoc = handleViolation(now, result, player, from, to, data, cc);
            if (inAir) {
                data.sfVLInAir = true;
            }
            if (vLoc != null) {
                return vLoc;
            }
        }
        else {
            // Slowly reduce the level with each event, if violations have not recently happened.
            // TODO: Switch to move count instead of time (!).
            if (data.getPlayerMoveCount() - data.sfVLTime > cc.survivalFlyVLFreezeCount 
                    && (!cc.survivalFlyVLFreezeInAir || !inAir
                            // Favor bunny-hopping slightly: clean descend.
                            || !data.sfVLInAir
                            && data.liftOffEnvelope == LiftOffEnvelope.NORMAL
                            && lastMove.toIsValid 
                            && lastMove.yDistance < -Magic.GRAVITY_MIN
                            && thisMove.yDistance - lastMove.yDistance < -Magic.GRAVITY_MIN)) {
                // Relax VL.
                data.survivalFlyVL *= 0.95;
                // Finally check horizontal buffer regain.
                if (hDistanceAboveLimit < 0.0  && result <= 0.0 
                        && !isSamePos && data.sfHorizontalBuffer < cc.hBufMax) {
                    // TODO: max min other conditions ?
                    hBufRegain(hDistance, Math.min(0.2, Math.abs(hDistanceAboveLimit)), data, cc);
                }
            }
        }

        //////////////////////////////////////////////////////////////////////////////////////////////
        //  Set data for normal move or violation without cancel (cancel would have returned above).
        //////////////////////////////////////////////////////////////////////////////////////////////

        // Check LiftOffEnvelope.
        // TODO: Web before liquid? Climbable?
        // TODO: isNextToGround(0.15, 0.4) allows a little much (yMargin), but reduces false positives.
        // TODO: nextToGround: Shortcut with block-flags ?
        final LiftOffEnvelope oldLiftOffEnvelope = data.liftOffEnvelope;
        if (to.isInLiquid()) {
            if (fromOnGround && !toOnGround 
                    && data.liftOffEnvelope == LiftOffEnvelope.NORMAL
                    && data.sfJumpPhase <= 0  && !thisMove.from.inLiquid) {
                // KEEP
            }
            else if (to.isNextToGround(0.15, 0.4)) {
                // Consent with ground.
                data.liftOffEnvelope = LiftOffEnvelope.LIMIT_NEAR_GROUND;
            } 
            else {
                // TODO: Distinguish strong limit from normal.
                data.liftOffEnvelope = LiftOffEnvelope.LIMIT_LIQUID;
            }
        }
        else if (thisMove.to.inWeb) {
            data.liftOffEnvelope = LiftOffEnvelope.NO_JUMP; // TODO: Test.
        }
        else if (resetTo) {
            // TODO: This might allow jumping on vines etc., but should do for the moment.
            data.liftOffEnvelope = LiftOffEnvelope.NORMAL;
        }
        else if (thisMove.from.inLiquid) {
            if (!resetTo 
                    && data.liftOffEnvelope == LiftOffEnvelope.NORMAL
                    && data.sfJumpPhase <= 0) {
                // KEEP
            }
            else if (to.isNextToGround(0.15, 0.4)) {
                // TODO: Problematic: y-distance slope can be low jump.
                data.liftOffEnvelope = LiftOffEnvelope.LIMIT_NEAR_GROUND;
            }
            else {
                // TODO: Distinguish strong limit.
                data.liftOffEnvelope = LiftOffEnvelope.LIMIT_LIQUID;
            }
        }
        else if (thisMove.from.inWeb) {
            data.liftOffEnvelope = LiftOffEnvelope.NO_JUMP; // TODO: Test.
        }
        else if (resetFrom || thisMove.touchedGround) {
            // TODO: Where exactly to put noFallAssumeGround ?
            data.liftOffEnvelope = LiftOffEnvelope.NORMAL;
        }
        else {
            // Keep medium.
            // TODO: Is above stairs ?
        }
        // Count how long one is moving inside of a medium.
        if (oldLiftOffEnvelope != data.liftOffEnvelope) {
            data.insideMediumCount = 0;
            data.combinedMediumHCount = 0;
            data.combinedMediumHValue = 0.0;
        }
        else if (!resetFrom || !resetTo) {
            data.insideMediumCount = 0;
        }
        else {
            data.insideMediumCount ++;
        }

        // Apply reset conditions.
        if (resetTo) {
            // The player has moved onto ground.
            if (toOnGround) {
                // Reset bunny-hop-delay.
                if (data.bunnyhopDelay > 0 && yDistance > 0.0 && to.getY() > data.getSetBackY() + 0.12 && !from.isResetCond() && !to.isResetCond()) {
                    if (data.bunnyhopDelay > 6) data.lastbunnyhopDelay = data.bunnyhopDelay;
                    data.bunnyhopDelay = 0;
                    tags.add("resetbunny");
                }
            }
            // Reset data.
            data.setSetBack(to);
            data.sfJumpPhase = 0;
            data.clearAccounting();
            data.sfNoLowJump = false;
            if (data.sfLowJump && resetFrom) {
                // Prevent reset if coming from air (purpose of the flag).
                data.sfLowJump = false;
            }
            if (hFreedom <= 0.0 && thisMove.verVelUsed == null) {
                data.resetVelocityJumpPhase(tags);
            }
        }
        else if (resetFrom) {
            // The player moved from ground.
            data.setSetBack(from);
            data.sfJumpPhase = 1; // This event is already in air.
            data.clearAccounting();
            data.sfLowJump = false;
            // not resetting nolowjump (?)...
            // Don't reset velocity phase unless moving into resetcond.
            //            if (hFreedom <= 0.0 && data.verVelUsed == null && (!data.noFallAssumeGround || fromOnGround)) {
            //                data.resetVelocityJumpPhase(tags);
            //            }
        }
        else {
            data.sfJumpPhase ++;
            // TODO: Void-to-void: Rather handle unified somewhere else (!).
            if (to.getY() < 0.0 && cc.sfSetBackPolicyVoid) {
                data.setSetBack(to);
            }
        }

        if (inAir) {
            // Adjust in-air counters.
            if (yDistance == 0.0) {
                data.sfZeroVdistRepeat ++;
            }
            else {
                data.sfZeroVdistRepeat = 0;
            }
        }
        else {
            data.sfZeroVdistRepeat = 0;
            data.ws.resetConditions(WRPT.G_RESET_NOTINAIR);
            data.sfVLInAir = false;
        }

        // Horizontal velocity invalidation.
        if (hDistance <= (cc.velocityStrictInvalidation ? thisMove.hAllowedDistanceBase : thisMove.hAllowedDistanceBase / 2.0)) {
            // TODO: Should there be other side conditions?
            // Invalidate used horizontal velocity.
            // debug(player, "*** INVALIDATE ON SPEED");
            data.clearActiveHorVel();
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

        // Adjust data.
        data.lastFrictionHorizontal = data.nextFrictionHorizontal;
        data.lastFrictionVertical = data.nextFrictionVertical;

        // Log tags added after violation handling.
        if (debug && tags.size() > tagsLength) {
            logPostViolationTags(player);
        }
        return null;
    }

    private boolean toOnGroundPastStates(
            final PlayerLocation from, final PlayerLocation to, 
            final PlayerMoveData thisMove, int tick, 
            final MovingData data, final MovingConfig cc) {
        // TODO: Heuristics / more / which? (too short move, typical step up moves, typical levels, ...)
        if (to.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, data.blockChangeRef, tick)) {
            tags.add("pastground_to");
            return true;
        }
        else {
            return false;
        }
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
    private double[] getVerticalBlockMoveResult(final double yDistance, 
            final PlayerLocation from, final PlayerLocation to, 
            final int tick, final MovingData data) {
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
                if (from.matchBlockChange(blockChangeTracker, data.blockChangeRef, Direction.Y_POS, 
                        Math.min(yDistance, 1.0))) {
                    if (yDistance > 1.0) {
                        //                        // TODO: Push of box off-center has the same effect.
                        //                        final BlockChangeEntry entry = blockChangeTracker.getBlockChangeEntryMatchFlags(data.blockChangeRef, 
                        //                                tick, from.getWorld().getUID(), from.getBlockX(), from.getBlockY() - 1, from.getBlockZ(),
                        //                                Direction.Y_POS, BlockProperties.F_BOUNCE25);
                        //                        if (entry != null) {
                        //                            data.blockChangeRef.updateSpan(entry);
                        //                            data.prependVerticalVelocity(new SimpleEntry(tick, 0.5015, 3)); // TODO: HACK
                        //                            tags.add("past_bounce");
                        //                        }
                        //                        else 
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
            // (No else.)
            //            if (yDistance <= 1.55) {
            //                // TODO: Edges ca. 0.5 (or 2x 0.5).
            //                // TODO: Center ca. 1.5. With falling height, values increase slightly.
            //                // Simplified: Always allow 1.5 or less with being pushed up by slime.
            //                // TODO: 
            //                if (from.matchBlockChangeMatchResultingFlags(
            //                        blockChangeTracker, data.blockChangeRef, Direction.Y_POS, 
            //                        Math.min(yDistance, 0.415), // Special limit.
            //                        BlockProperties.F_BOUNCE25)) {
            //                    tags.add("blkmv_y_pos_bounce");
            //                    final double maxDistYPos = yDistance; //1.0 - (from.getY() - from.getBlockY()); // TODO: Margin ?
            //                    // TODO: Set bounce effect or something !?
            //                    // TODO: Bounce effect instead ?
            //                    data.addVerticalVelocity(new SimpleEntry(tick, Math.max(0.515, yDistance - 0.5), 2));
            //                    return new double[]{maxDistYPos, 0.0};
            //                }
            //            }
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
     * Set data.nextFriction according to media.
     * @param from
     * @param to
     * @param data
     * @param cc
     */
    private void setNextFriction(final PlayerMoveData thisMove, 
            final MovingData data, final MovingConfig cc) {
        // NOTE: Other methods might still override nextFriction to 1.0 due to burst/lift-off envelope.
        // TODO: Other media / medium transitions / friction by block.
        final LocationData from = thisMove.from;
        final LocationData to = thisMove.to;
        if (from.inWeb || to.inWeb) {
            data.nextFrictionHorizontal = data.nextFrictionVertical = 0.0;
        }
        else if (from.onClimbable || to.onClimbable) {
            // TODO: Not sure about horizontal (!).
            data.nextFrictionHorizontal = data.nextFrictionVertical = 0.0;
        }
        else if (from.inLiquid) {
            // TODO: Exact conditions ?!
            if (from.inLava) {
                data.nextFrictionHorizontal = data.nextFrictionVertical = Magic.FRICTION_MEDIUM_LAVA;
            }
            else {
                data.nextFrictionHorizontal = data.nextFrictionVertical = Magic.FRICTION_MEDIUM_WATER;
            }
        }
        // TODO: consider setting minimum friction last (air), do add ground friction.
        else if (!from.onGround && !to.onGround) {
            data.nextFrictionHorizontal = data.nextFrictionVertical = Magic.FRICTION_MEDIUM_AIR;
        }
        else {
            data.nextFrictionHorizontal = 0.0; // TODO: Friction for walking on blocks (!).
            data.nextFrictionVertical = Magic.FRICTION_MEDIUM_AIR;
        }

    }

    /**
     * Set hAllowedDistanceBase and hAllowedDistance in thisMove. Not exact,
     * check permissions as far as necessary, if flag is set to check them.
     * 
     * @param player
     * @param sprinting
     * @param thisMove
     * @param data
     * @param cc
     * @param checkPermissions
     *            If to check permissions, allowing to speed up a little bit.
     *            Only set to true after having failed with it set to false.
     * @return Allowed distance.
     */
    private double setAllowedhDist(final Player player, final boolean sprinting, 
            final PlayerMoveData thisMove, 
            final MovingData data, final MovingConfig cc, final IPlayerData pData, final PlayerLocation from,
            final boolean checkPermissions)
    {
        // TODO: Optimize for double checking?
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
	final boolean sfDirty = data.isVelocityJumpPhase();  // TODO: sfDirty: Better friction/envelope-based.
        double hAllowedDistance = 0D;
	long now = System.currentTimeMillis();
        double friction = data.lastFrictionHorizontal; // Friction to use with this move.
        boolean useBaseModifiers = false;
        boolean useBaseModifiersSprint = true;
	    
	    
        // Reset noslow check if has velocity
        if (data.noslowhop != 0 && (sfDirty || (!data.isusingitem && !player.isBlocking()))) data.noslowhop = 0;
        if (!data.liftOffEnvelope.name().startsWith("LIMIT") || sfDirty) data.watermovect = 0;

	    
	// Webs
        if (thisMove.from.inWeb && (from.getBlockFlags() & BlockProperties.F_ALLOW_LOWJUMP) == 0) {
            data.sfOnIce = 0;
	    tags.add("hweb");
            // TODO: if (from.isOnIce()) <- makes it even slower !
            // Does include sprinting by now (would need other accounting methods).
            hAllowedDistance = Magic.modWeb * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            // Cobweb doesn't apply speed effect but BerryBush does
            useBaseModifiersSprint = false;
            from.collectBlockFlags(); // Just ensure.
            // Berry bush
            if ((from.getBlockFlags() & BlockProperties.F_COBWEB2) !=0) {
		tags.add("hbush");
                if (thisMove.yDistance > 0) {
                    hAllowedDistance = 1.1 * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
		}
                else {
                    hAllowedDistance = 0.362 * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
		}
                if (sprinting) hAllowedDistance += 0.0255;
            }
            friction = 0.0; // Ensure friction can't be used to speed.
            useBaseModifiers = true;
        }
	    
	// SoulSand   
	else if (thisMove.to.onSoulSand && thisMove.from.onSoulSand) {
            hAllowedDistance = Magic.modSoulSand * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            useBaseModifiers = true;
	    tags.add("hsoulsand");
	    // friction = 0.0; 
        }
	    
	// Honeyblock
        else if (ShouldApplyHBSpeed(from)) {
	   tags.add("hhoneyblock");
           if (!thisMove.to.onGround) 
               hAllowedDistance = Magic.modSoulSand * 1.75 * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D; 
	   else
               hAllowedDistance = Magic.modSoulSand * 0.8 * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
               useBaseModifiers = true;
	}
	    
	// In liquid
        else if (thisMove.from.inLiquid && thisMove.to.inLiquid) {
            // Check all liquids (lava might demand even slower speed though).
            // TODO: Test how to go with only checking from (less dolphins).
            // TODO: Sneaking and blocking applies to when in water !
	    tags.add("hliquid");
            hAllowedDistance = Bridge1_13.isSwimming(player) ? Magic.modSwim[1] : Magic.modSwim[0] * thisMove.walkSpeed * cc.survivalFlySwimmingSpeed / 100D;
            useBaseModifiers = false;
            if (sfDirty) friction = 0.0;
            if (thisMove.from.inWater || !thisMove.from.inLava) { // (We don't really have other liquids, though.)
                final int level = BridgeEnchant.getDepthStriderLevel(player);
                if (level > 0) {
                    // Speed effect, attribute will affect to water movement whenever you has DepthStrider enchant.
                    useBaseModifiers = true;
                    useBaseModifiersSprint = true;
                    // The hard way.
                    hAllowedDistance *= Magic.modDepthStrider[level];
                    // Modifiers: Most speed seems to be reached on ground, but couldn't nail down.
                }
                if (!Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(player))) {
                    // TODO: Allow for faster swimming above water with Dolhphins Grace
		    hAllowedDistance *= Magic.modDolphinsGrace;
                    if (level > 1) hAllowedDistance *= 1.0 + 0.07 * level;
                }
                if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
                   hAllowedDistance *= Magic.modRiptide[data.RiptideLevel];
                }
            }
            // Allow moving half on farmland and half on water to have higher speed
            if (data.newHDist && hAllowedDistance < 0.345D) {
                useBaseModifiers = true;
   	        hAllowedDistance = 0.445D;
            }
            // (Friction is used as is.)
        }
	    
	// Riptiding
        else if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
	     tags.add("hriptide");
             hAllowedDistance = Magic.modRiptide[data.RiptideLevel] * thisMove.walkSpeed * cc.survivalFlySpeedingSpeed / 100D;
        } 
	    
	// Temporary fix
        else if (data.newHDist && hAllowedDistance < 0.345D) {
             useBaseModifiers = true;
             hAllowedDistance = 0.445D;
        }
	    
	// Stairs
        // TODO: Should read hbufmax from config and adjust
        else if (from.isAboveStairs()) {
	    tags.add("hstairs");
            useBaseModifiers = true;
            hAllowedDistance = thisMove.yDistance == 0.5 ? 1.85 * thisMove.walkSpeed : 1.325 * thisMove.walkSpeed;
            if (TrigUtil.isMovingBackwards(thisMove.to.getX() - thisMove.from.getX(), thisMove.to.getZ() - thisMove.from.getZ(), LocUtil.correctYaw(from.getYaw())))
                hAllowedDistance = thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D;
            if (!Double.isInfinite(mcAccess.getHandle().getFasterMovementAmplifier(player))) hAllowedDistance *= 0.88;
        }
	    
	    
        //else if (snowFix && hAllowedDistance < 0.377D) {
        //     hAllowedDistance = 0.377D;
        //     useBaseModifiers = true;
        //}
	    
	    
	// Allows faster speed for player when swimming above water since from -> to does not seem to detect correctly
	else if (BlockProperties.isLiquid(from.getTypeIdBelow()) && !Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(player))) {
	     hAllowedDistance = Bridge1_13.isSwimming(player) ? Magic.modSwim[1] : Magic.modSwim[0] * thisMove.walkSpeed * cc.survivalFlySwimmingSpeed * Magic.modDolphinsGrace / 100D;
	     final int level = BridgeEnchant.getDepthStriderLevel(player);
	     if (level > 0) {
	        hAllowedDistance = Magic.modSwim[0] * thisMove.walkSpeed * cc.survivalFlySwimmingSpeed * Magic.modDolphinsGrace * Magic.modDepthStrider[level] / 100D;
	     }
        }
	    
	// Speed restrict when leaving water(mostly duplicate with normal liquid modeling above)
        // TODO: Still check with velocity?
        else if (!sfDirty && !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_WATERWALK, player) 
                && ((thisMove.from.inLiquid && !thisMove.to.inLiquid) || data.watermovect == 1) 
                && data.liftOffEnvelope.name().startsWith("LIMIT")) {
	    tags.add("hsurface");
	    hAllowedDistance = Bridge1_13.isSwimming(player) ? Magic.modSwim[1] : Magic.modSwim[0] * thisMove.walkSpeed * 1.06 * cc.survivalFlySwimmingSpeed / 100D;
            useBaseModifiersSprint = false;
            friction = 0.0;
            final int level = BridgeEnchant.getDepthStriderLevel(player);
            if (level > 0 && data.watermovect < 1) {
               // Speed effect, attribute will affect to water movement whenever you has DepthStrider enchant.
               useBaseModifiers = true;
               useBaseModifiersSprint = true;
               friction = data.lastFrictionHorizontal;
               hAllowedDistance *= Magic.modDepthStrider[level];
            }
            if (!Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(player))) {
		hAllowedDistance *= Magic.modDolphinsGrace;
                if (level > 1) hAllowedDistance *= 1.0 + 0.07 * level;
            }
            if (data.watermovect == 1) hAllowedDistance *= 1.35;
            data.watermovect = 1;
            final int blockdata = from.getData(from.getBlockX(), from.getBlockY(), from.getBlockZ());
            final int blockunderdata = from.getData(from.getBlockX(), from.getBlockY() -1, from.getBlockZ());
            if (blockdata > 3 || blockunderdata > 3 || data.isdownstream) {
                data.watermovect = 0;
                hAllowedDistance = thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D;
                data.isdownstream = false;
            }
	}
	    
	// Sneaking
        // TODO: !sfDirty is very coarse, should use friction instead.
        else if (!sfDirty && thisMove.from.onGround && player.isSneaking() 
		 && reallySneaking.contains(player.getName()) 
                 && (!checkPermissions || !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_SNEAKING, player))
		 ) {
	    tags.add("sneaking");
            hAllowedDistance = Magic.modSneak * thisMove.walkSpeed * cc.survivalFlySneakingSpeed / 100D;
            friction = 0.0; // Ensure friction can't be used to speed.
            useBaseModifiers = true;
            useBaseModifiersSprint = false;
            if (!Double.isInfinite(mcAccess.getHandle().getFasterMovementAmplifier(player))) {
                hAllowedDistance *= 0.88;
                useBaseModifiersSprint = true;
            }
            // TODO: Attribute modifiers can count in here, e.g. +0.5 (+ 50% doesn't seem to pose a problem, neither speed effect 2).
        }
	    
	// NoSlow
        else if (data.isHackingRI && !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_BLOCKING, player)) {
            data.isHackingRI = false;
            hAllowedDistance = 0.0;
            friction = 0.0;
            useBaseModifiers = false;
            tags.add("usingitem(cancel)");
        }
        
	// Using items
        // TODO: !sfDirty is very coarse, should use friction instead.
        else if (!sfDirty && (data.isusingitem || player.isBlocking()) 
		&& (thisMove.from.onGround || data.noslowhop > 0 || player.isBlocking())
                && (!checkPermissions || !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_BLOCKING, player)) 
		&& data.liftOffEnvelope == LiftOffEnvelope.NORMAL) {
            tags.add("usingitem");
            if (thisMove.from.onGround) {
                // Jump/left ground
                if (!thisMove.to.onGround) {
                    final double speedAmplifier = mcAccess.getHandle().getFasterMovementAmplifier(player);
                    hAllowedDistance = (lastMove.hDistance > 0.23 ? 0.4 : 0.2 + (Bridge1_13.hasIsSwimming() ? 0.15 : 0.0)) +
                    0.02 * (Double.isInfinite(speedAmplifier) ? 0 : speedAmplifier + 1.0);
                    hAllowedDistance *= cc.survivalFlyBlockingSpeed / 100D;
                    data.noslowhop = 1;
                // OnGround
                } 
		else {
                    // TODO: Need testing
                    if (lastMove.toIsValid && lastMove.hDistance > 0.0)
                       hAllowedDistance = data.noslowhop < 7 ?
                       // 0.6 for old vers, 0.621 for 1.13+
                       (lastMove.hAllowedDistance * (0.621 + 0.055 * ++data.noslowhop)) : lastMove.hAllowedDistance;
                       // Failed or no hDistance in last move, return to default speed
                    else hAllowedDistance = Magic.modBlock * thisMove.walkSpeed * cc.survivalFlyBlockingSpeed / 100D;
                }
            } 
	    else if (data.noslowhop > 0) {
                if (data.noslowhop == 1 && lastMove.toIsValid) {
                    // Second move after jump, high decay
                    hAllowedDistance = lastMove.hAllowedDistance * 0.6 * cc.survivalFlyBlockingSpeed / 100D;
                    // Fake data, prevent too much friction after slow - rejump
                    data.noslowhop = 4;
                } 
		else
                // Air friction
                hAllowedDistance = lastMove.hAllowedDistance * 0.96 * cc.survivalFlyBlockingSpeed / 100D;
            } 
	    else if (player.isBlocking() && lastMove.toIsValid) {
                // Air friction
                hAllowedDistance = lastMove.hAllowedDistance * 0.96 * cc.survivalFlyBlockingSpeed / 100D;
                // Fake data for air blocking
                data.noslowhop = 1;
            }
            // Check if too small horizontal last move allowed
            // 0.063 for old vers, 0.08 for 1.13+
            if (lastMove.hAllowedDistance < 0.03) hAllowedDistance = 0.08; 
            friction = 0.0; // Ensure friction can't be used to speed.
            useBaseModifiers = true;
            useBaseModifiersSprint = false;
        } 
	    
	// Collision with entities (1.9+)
	else if (Bridge1_9.hasLevitation() && CollisionUtil.isCollidingWithEntities(player, true) && hAllowedDistance < 0.35) {
        	hAllowedDistance = 0.35;
        	useBaseModifiers = true;
        	data.bunnyhopTick = 6;
        }
	    
	// Sprinting/Walking
        else {
            useBaseModifiers = true;
            if (sprinting) {
		tags.add("sprinting");
                if (!thisMove.from.onGround && thisMove.to.onGround) {
                    data.bunnyhopTick = Bridge1_13.hasIsSwimming() ? 6 : 3;
                    hAllowedDistance = thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D;
                } 
		else if (data.bunnyhopTick > 0) {
                	if (data.bunnyhopTick < 3) hAllowedDistance = thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D * 1.1; 
                	else hAllowedDistance = Magic.modSprint * thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D;
                	if (snowFix && data.bunnyhopTick > 5) hAllowedDistance *= 1.6;
                	data.bunnyhopTick--;
                }
		else                
            	  hAllowedDistance = thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D;
            }
            else {
		tags.add("walking");
                hAllowedDistance = thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            }
            // Ensure friction can't be used to speed.
            // TODO: Model bunny hop as a one time peak + friction. Allow medium based friction.
            friction = 0.0;
        }
        // Apply modifiers (sprinting, attributes, ...).
        if (useBaseModifiers) {
            if (useBaseModifiersSprint && sprinting) {
                hAllowedDistance *= data.multSprinting;
            }
            // Note: Attributes count in slowness potions, thus leaving out isn't possible.
            final double attrMod = attributeAccess.getHandle().getSpeedAttributeMultiplier(player);
            if (attrMod == Double.MAX_VALUE) {
                // TODO: Slowness potion.
                // Count in speed potions.
                final double speedAmplifier = mcAccess.getHandle().getFasterMovementAmplifier(player);
                if (!Double.isInfinite(speedAmplifier) && useBaseModifiersSprint) {
                    hAllowedDistance *= 1.0D + 0.2D * (speedAmplifier + 1);
                }
            }
            else {
                hAllowedDistance *= attrMod;
                // TODO: Consider getting modifiers from items, calculate with classic means (or iterate over all modifiers).
                // Hack for allow sprint-jumping with slowness.
                if (sprinting && hAllowedDistance < 0.29 && cc.sfSlownessSprintHack 
                        && (
                                // TODO: Test/balance thresholds (walkSpeed, attrMod).
                                player.hasPotionEffect(PotionEffectType.SLOW)
                                || data.walkSpeed < Magic.DEFAULT_WALKSPEED
                                || attrMod < 1.0
                                )
                        ) {
                    // TODO: Should restrict further by yDistance, ground and other (jumping only).
                    // TODO: Restrict to not in water (depth strider)?
                    hAllowedDistance = slownessSprintHack(player, hAllowedDistance);
                }
                //useBaseModifiersSprint = false mean not apply speed effect in it 
                if (!useBaseModifiersSprint) {
                	final double speedAmplifier = mcAccess.getHandle().getFasterMovementAmplifier(player);
                    if (!Double.isInfinite(speedAmplifier)) {
                        hAllowedDistance /= attrMod;
                        hAllowedDistance *= attrMod - 0.2D * (speedAmplifier + 1);
                    }
                }
            }
        }
        // TODO: Reset friction on too big change of direction?

        // Account for flowing liquids (only if needed).
        // Assume: If in liquids this would be placed right here.
        if (thisMove.downStream && thisMove.hDistance > thisMove.walkSpeed * Magic.modSwim[0] && thisMove.from.inLiquid) {
            hAllowedDistance *= Magic.modDownStream;
        }

        // If the player is on ice, give them a higher maximum speed.
        if (data.sfOnIce > 9) {
            hAllowedDistance *= Magic.modIce;
            if (data.bunnyhopTick > 3) hAllowedDistance *= 1.25;
            else if (data.bunnyhopTick > 0) hAllowedDistance *= 1.1;
        } else if (data.sfOnIce > 0) {
	    tags.add("hice");
            hAllowedDistance *= 1.0 + 0.025 * data.sfOnIce;
        }

        // Speeding bypass permission (can be combined with other bypasses).
        if (checkPermissions && pData.hasPermission(Permissions.MOVING_SURVIVALFLY_SPEEDING, player)) {
            hAllowedDistance *= cc.survivalFlySpeedingSpeed / 100D;
        }

        // Base speed is set.
        thisMove.hAllowedDistanceBase = hAllowedDistance;

        // Friction mechanics (next move).
        if (thisMove.hDistance <= hAllowedDistance) {
            // Move is within lift-off/burst envelope, allow next time.
            // TODO: This probably is the wrong place (+ bunny, + buffer)?
            data.nextFrictionHorizontal = 1.0;
        }

        // Friction or not (this move).
        if (lastMove.toIsValid && friction > 0.0) {
            // Consider friction.
            // TODO: Invalidation mechanics.
            // TODO: Friction model for high speeds?
	    tags.add("hfriction");
            hAllowedDistance = Math.max(hAllowedDistance, lastMove.hDistance * friction);
        }
        //        if (hAllowedDistance < thisMove.hDistance) {
        //            // After failure recovery.
        //            if (lastMove.toIsValid) {
        //                final double hDistDiff = thisMove.hDistance - lastMove.hDistance;
        //                // Elytra.
        //                if (hDistDiff < Magic.GLIDE_HORIZONTAL_GAIN_MAX
        //                        && Magic.inAir(thisMove) && Bridge1_9.isWearingElytra(player)) {
        //                    // (Abrupt hdist stops aren't covered yet anyway.)
        //                    hAllowedDistance = thisMove.hDistance;
        //                    data.nextFrictionHorizontal = Magic.FRICTION_MEDIUM_AIR;
        //                }
        //            }
        //        }
        thisMove.hAllowedDistance = hAllowedDistance;
        return thisMove.hAllowedDistance;
    }

    private boolean ShouldApplyHBSpeed(PlayerLocation from) {
        return (BlockProperties.getBlockFlags(from.getTypeId()) & BlockProperties.F_STICKY) !=0;
    }

	/**
     * Return a 'corrected' allowed horizontal speed. Call only if the player
     * has a SLOW effect.
     * 
     * @param player
     * @param hAllowedDistance
     * @return
     */
    private double slownessSprintHack(final Player player, final double hAllowedDistance) {
        // TODO: Certainly wrong for items with speed modifier (see above: calculate the classic way?).
        // Simple: up to high levels they can stay close, with a couple of hops until max base speed. 
        return 0.29;
    }

    /**
     * Access method from outside.
     * @param player
     * @return
     */
    public boolean isReallySneaking(final Player player) {
        return reallySneaking.contains(player.getName());
    }
    
    private boolean hackStep(final long now, final Player player, final PlayerLocation from, 
            final boolean fromOnGround, final boolean resetFrom, final PlayerLocation to, 
            final boolean resetTo, final double hDistance, final double yDistance, 
            final PlayerMoveData lastMove, 
            final MovingData data, final IPlayerData pData)  {
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        boolean reset = false;
        final double past = data.yDis;
        
        if (fromOnGround || from.isInLiquid() || from.isInWeb() || from.isOnClimbable() || (thisMove.touchedGround && resetTo)) reset = true;
        if (yDistance != 0.0 && !isWaterlogged(from)) {
            data.yDis += yDistance;
            if (data.yDis < 0.0 || data.yDis > 3.0) {
                data.yDis = 0.0;
            }
        }
        if (reset && fromOnGround && yDistance > 0.0) {
            reset = false;
            data.yDis = yDistance;
            final double roundY = Math.floor(from.getY());
            if (roundY > 0.0 && from.getY() < roundY + Magic.GRAVITY_ODD) data.yDis += from.getY() - roundY;
        }
        // "Noob" tower, jump_LostGroundEdgeAsc, velocity, recently left water, on stairs
        if (tags.contains("lostground_nbtwr")
            //TODO: How to merge all edgeasc cases in one?
            || tags.contains("lostground_edgeasc1") || tags.contains("lostground_edgeasc5") || tags.contains("lostground_edgeasc7")
            || (from.isOnGround(0.15) && data.yDis > 1.4995)
            || data.isVelocityJumpPhase()
            || data.liqtick != 0
            || from.isAboveStairs()) {
            data.yDis = 0.0;
        }

        if (data.yDis > 1.4995) {
            final double margin = 0.2;
            final Location loc = from.getLocation();
            if (BlockProperties.isCarpet(loc.clone().add(margin, 0.0, 0.0).getBlock().getType())) {
                final long flag = BlockProperties.getBlockFlags(loc.clone().add(margin, -1.0, 0.0).getBlock().getType());
                if ((flag & BlockProperties.F_THICK_FENCE) != 0 || (flag & BlockProperties.F_THICK_FENCE2) != 0) {
                    data.yDis = 0.0;
                    return false;
                }
            } else
            if (BlockProperties.isCarpet(loc.clone().add(-margin, 0.0, 0.0).getBlock().getType())) {
                final long flag = BlockProperties.getBlockFlags(loc.clone().add(-margin, -1.0, 0.0).getBlock().getType());
                if ((flag & BlockProperties.F_THICK_FENCE) != 0 || (flag & BlockProperties.F_THICK_FENCE2) != 0) {
                    data.yDis = 0.0;
                    return false;
                }
            } else
            if (BlockProperties.isCarpet(loc.clone().add(0.0, 0.0, margin).getBlock().getType())) {
                final long flag = BlockProperties.getBlockFlags(loc.clone().add(0.0, -1.0, margin).getBlock().getType());
                if ((flag & BlockProperties.F_THICK_FENCE) != 0 || (flag & BlockProperties.F_THICK_FENCE2) != 0) {
                    data.yDis = 0.0;
                    return false;
                }
            } else
            if (BlockProperties.isCarpet(loc.clone().add(0.0, 0.0, -margin).getBlock().getType())) {
                final long flag = BlockProperties.getBlockFlags(loc.clone().add(0.0, -1.0, -margin).getBlock().getType());
                if ((flag & BlockProperties.F_THICK_FENCE) != 0 || (flag & BlockProperties.F_THICK_FENCE2) != 0) {
                    data.yDis = 0.0;
                    return false;
                }
            }
        }

        //Will "feed" up to 1.4995 anyway
        if (data.yDis > 1.4995
            && data.timeRiptiding + 3500 < now
            && Double.isInfinite(PotionUtil.getPotionEffectAmplifier(player, PotionEffectType.JUMP))
            && !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_STEP, player)) {
            tags.add("step");
            data.yDis -= 1.0;
            if (reset && past != 0.0) data.yDis = 0.0;
            return true;
        }
        if (reset && past != 0.0) data.yDis = 0.0;
        return false;
    }

    /**
     * Core y-distance checks for in-air movement (may include air -> other).
     * @return
     */
    private double[] vDistAir(final long now, final Player player, final PlayerLocation from, 
            final boolean fromOnGround, final boolean resetFrom, final PlayerLocation to, 
            final boolean toOnGround, final boolean resetTo, 
            final double hDistance, final double yDistance, 
            final int multiMoveCount, final PlayerMoveData lastMove, 
            final MovingData data, final MovingConfig cc, final IPlayerData pData) {
	    
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        // Y-distance for normal jumping, like in air.
        double vAllowedDistance = 0.0;
        double vDistanceAboveLimit = 0.0;
        // Change seen from last yDistance.
        final double yDistChange = lastMove.toIsValid ? yDistance - lastMove.yDistance :  Double.MAX_VALUE;
        // Hacks.
        final boolean envelopeHack;
	    
	    
	    
        if (!resetFrom && !resetTo && MagicAir.venvHacks(from, to, yDistance, yDistChange, thisMove, lastMove, data)) {
            envelopeHack = true;
            tags.add("hack_venv");
        }
        else {
            envelopeHack = false;
        }

        // Relative distance (friction, lift-off).
        // Estimate expected yDistance.
        // TODO: Friction might need same treatment as with horizontal (medium transitions: data.lastFrictionVertical).
        // TODO: lostground_pyramid(yDist < 0.0) -> step up (yDist 0.5). Needs better last-move modeling.
        // TODO: lostground_edgedesc(yDist <0.0) -> Bunny (yDist > .72, e_jump). Needs better last-move modeling.
        // TODO: air->ground...small-range-tp...air-air+vDist==0.0 (might work around with fromWasReset?).
        // TODO: bunny after vDist<0.0... vdistsb. Might need set back detection. [solved with setFrictionJumpPhase?]
        // TODO: Other edge cases?
        // TODO: Cleanup pending.
        final boolean strictVdistRel;
        final double maxJumpGain = data.liftOffEnvelope.getMaxJumpGain(data.jumpAmplifier);
        final double jumpGainMargin = 0.005; // TODO: Model differently, workarounds where needed. 0.05 interferes with max height vs. velocity (<= 0.47 gain).
        // TODO: Add/set 'allow starting to fall' first (data reset / from ground on if no speed).
	    
	    
	// Strict vDistRel //    
        if (lastMove.toIsValid && Magic.fallingEnvelope(yDistance, lastMove.yDistance, data.lastFrictionVertical, 0.0)) {
            // Less headache: Always allow falling. 
            // TODO: Base should be data.lastFrictionVertical? Problem: "not set" detection?
            vAllowedDistance = lastMove.yDistance * data.lastFrictionVertical - Magic.GRAVITY_MIN; // Upper bound.
            strictVdistRel = true;
        }
        else if ((resetFrom || thisMove.touchedGroundWorkaround) && (from.isResetCond() || from.isOnGround(1.0) || tags.contains("lostground_edgeasc1"))) {

            // TODO: More concise conditions? Some workaround may allow more.
            if (toOnGround) {
                // Hack for boats (coarse: allows minecarts too).
                if (yDistance > cc.sfStepHeight && yDistance - cc.sfStepHeight < 0.00000003 && to.isOnGroundDueToStandingOnAnEntity()) {
                    vAllowedDistance = yDistance;

                }
                else  {
                    vAllowedDistance = Math.max(cc.sfStepHeight, maxJumpGain + jumpGainMargin);

                }
            }
            else {
                // Code duplication with the absolute limit below.
                if (yDistance < 0.0 || yDistance > cc.sfStepHeight || !tags.contains("lostground_couldstep")) {
                    vAllowedDistance = maxJumpGain + jumpGainMargin;

                }
                else {
                    // lostground_couldstep
                    // TODO: Other conditions / envelopes?
                    vAllowedDistance = yDistance;

                }
            }
            strictVdistRel = false;
        } 
        else if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
              vAllowedDistance = lastMove.yDistance * 5.0D;
              strictVdistRel = false;
        } 
        else if (data.bedLeaveTime + 500 > now && yDistance < 0.45) {
             strictVdistRel = false;
             vAllowedDistance = yDistance;			
        }
        else if (lastMove.toIsValid) {
            if (lastMove.yDistance >= -Math.max(Magic.GRAVITY_MAX / 2.0, 1.3 * Math.abs(yDistance)) && lastMove.yDistance <= 0.0 
                    && (lastMove.touchedGround || lastMove.to.extraPropertiesValid && lastMove.to.resetCond)) {
                if (resetTo) { // TODO: Might have to use max if resetto.
                    vAllowedDistance = cc.sfStepHeight;
                }
                else {
                    // TODO: Needs more precise confinement + setting set back or distance to ground or estYDist.
                    vAllowedDistance = maxJumpGain + jumpGainMargin;
                }
                strictVdistRel = false;
                
            }
            else {
                // Friction.
                // TODO: data.lastFrictionVertical (see above).
            	// Slime fix ?
                vAllowedDistance = lastMove.yDistance * data.lastFrictionVertical - Magic.GRAVITY_ODD; // Upper bound.
                strictVdistRel = true;
                
            }
        }
        else {
            // Teleport/join/respawn.
            vAllowedDistance = vAllowedDistanceNoData(thisMove, lastMove, maxJumpGain, jumpGainMargin, data, cc);
            strictVdistRel = false;
            
        }
	    
	    
	// vDistRel //   
        //System.out.println("=========: " + vAllowedDistance + " " + yDistance);
        // Compare yDistance to expected, use velocity on violation.
        // TODO: Quick detect valid envelope and move workaround code into a method.
        // TODO: data.noFallAssumeGround  needs more precise flags (refactor to per move data objects, store 123)
        boolean vDistRelVL = false;
        //System.out.println(data.noFallMaxY);
        // Difference from vAllowedDistance to yDistance.
        final double yDistDiffEx = yDistance - vAllowedDistance;
        //System.out.println("maxlimit: " + yDistDiffEx);
        // Quick invalidation for too much water envelope
        if (!from.isInLiquid() && strictVdistRel && data.liftOffEnvelope == LiftOffEnvelope.LIMIT_LIQUID
            && yDistance > 0.3 && yDistance > vAllowedDistance && data.getOrUseVerticalVelocity(yDistance) == null) {
            thisMove.invalidate();
        }
        if (envelopeHack || yDistDiffEx <= 0.0 && yDistDiffEx > -Magic.GRAVITY_SPAN) {
            // (Clearly accepted envelopes first.)
            vDistRelVL = false;
            //vAllowedDistance = yDistance;
        }
        else if (yDistDiffEx > 0.0) { // Upper bound violation.
            // && (yDistance > 0.0 || (!resetTo && !data.noFallAssumeGround))
            if (yDistance <= 0.0 && (resetTo || thisMove.touchedGround)) {
                // Allow falling shorter than expected, if onto ground.
                // Note resetFrom should usually mean that allowed dist is > 0 ?
            }
            else if (lastMove.toIsValid) {
                // TODO: Sort in workarounds to methods, unless extremely frequent.
                if (yDistance < 0.0 && lastMove.yDistance < 0.0 && yDistChange > -Magic.GRAVITY_MAX
                        && (from.isOnGround(Math.abs(yDistance) + 0.001) || BlockProperties.isLiquid(to.getTypeId(to.getBlockX(), Location.locToBlock(to.getY() - 0.5), to.getBlockZ())))) {
                    // Pretty coarse workaround, should instead do a proper modeling for from.getDistanceToGround.
                    // (OR loc... needs different model, distanceToGround, proper set back, moveHitGround)
                    // TODO: Slightly too short move onto the same level as snow (0.75), but into air (yDistance > -0.5).
                    // TODO: Better on-ground model (adapt to actual client code).
                }
                //                else if (yDistance < 0.0 && yDistChange > 0.0 && tags.contains("lostground_edgedesc")) {
                //                    // Falling less far than expected by hitting an edge.
                //                }
                else if (yDistDiffEx < Magic.GRAVITY_MIN / 2.0 && data.sfJumpPhase == 1 //&& data.fromWasReset
                        // TODO: Test with demanding && (data.noFallAssumeGround || data.liftOffEnvelope != LiftOffEnvelope.NORMAL)
                        && to.getY() - data.getSetBackY() <= data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier)
                        && lastMove.yDistance <= maxJumpGain && yDistance > -Magic.GRAVITY_MAX  && yDistance < lastMove.yDistance
                        && lastMove.yDistance - yDistance > Magic.GRAVITY_ODD / 3.0) {
                    // Special jump (water/edges/assume-ground), too small decrease.	
                }
                else if (yDistDiffEx < Magic.GRAVITY_MIN && data.sfJumpPhase == 1 
                        && data.liftOffEnvelope != LiftOffEnvelope.NORMAL 
                        && lastMove.from.extraPropertiesValid && lastMove.from.inLiquid
                        && lastMove.yDistance < -Magic.GRAVITY_ODD / 2.0 && lastMove.yDistance > -Magic.GRAVITY_MAX - Magic.GRAVITY_SPAN
                        && yDistance < lastMove.yDistance - 0.001) {
                    // Odd decrease with water.          	
                }
                else if (MagicAir.oddJunction(from, to, yDistance, yDistChange, yDistDiffEx, maxJumpGain, resetTo, thisMove, lastMove, data, cc)) {
                    // Gravity effects
                }
                else if (yDistDiffEx < 0.025 && Magic.noobJumpsOffTower(yDistance, maxJumpGain, thisMove, lastMove, data)) {
                    /*
                     * On (noob) tower up, the second move has a higher distance
                     * than expected, because the first had been starting
                     * slightly above the top.
                     */
                }
                else if (isLanternUpper(to) || isWaterlogged(from) || isWaterlogged(to)) {
		      // Ignore			
                }
                else if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
                    vDistRelVL = false;
                }
                else if (data.bedLeaveTime + 500 > now && yDistance < 0.45) {
                    vDistRelVL = false;
                }
                else if (lastMove.from.inLiquid && lastMove.from.onClimbable) {
		     // Ignore.	
                }
                else if (yDistance > 0.0 && lastMove.yDistance < 0.0
                        && data.ws.use(WRPT.W_M_SF_SLIME_JP_2X0)
                        && MagicAir.oddBounce(to, yDistance, lastMove, data)) {
                    //Slime
                    data.setFrictionJumpPhase();
                }
                else if (isCollideWithHB(from, to, data) && yDistance < -0.125 && yDistance > -0.128) {
                    // Side collision with a honey block thus slower movement than expected.
                }
                else if (Bridge1_13.hasIsSwimming() && 
                    (data.sfJumpPhase == 7 && yDistance < -0.02 && yDistance > -0.2
                    || data.sfJumpPhase == 3 && lastMove.yDistance < -0.139 && yDistance > -0.1 && yDistance < 0.005
                    || yDistance < -0.288 && yDistance > -0.32 && lastMove.yDistance > -0.1 && lastMove.yDistance < 0.005
                    )) {
                    // False positives when break block below too fast. Seem newly appeared in recent versions 
                }
                //else if (Bridge1_13.hasIsSwimming() && from.isOnGround(0.125) && (from.getBlockFlags() & BlockProperties.F_HEIGHT_8_INC) != 0 && yDistance > 0.0 && yDistance < 0.43) {
                //	thisMove.touchedGround = true;
                //}
                else {
                    // Violation.
                    vDistRelVL = true;
                }
            }
            else {
                // Violation.
                vDistRelVL = true;
                
            }
        } // else: yDistDiffEx <= 0.0
        else if (yDistance >= 0.0) { // Moved too short.
            if (!strictVdistRel || Math.abs(yDistDiffEx) <= Magic.GRAVITY_SPAN || vAllowedDistance <= 0.2) {
                // Allow jumping less high unless within "strict envelope".
                // TODO: Extreme anti-jump effects, perhaps.
            }
            else if (yDistance > 0.0 && lastMove.toIsValid && lastMove.yDistance > yDistance
                    && lastMove.yDistance - yDistance <= lastMove.yDistance / 4.0
                    && data.isVelocityJumpPhase()
                    ) {
                // Too strong decrease with velocity.
                // TODO: Observed when moving off water, might be confined by that.
            }
            else if (thisMove.headObstructed || lastMove.toIsValid && lastMove.headObstructed && lastMove.yDistance >= 0.0) {
                // Head is blocked, thus a shorter move.
            }
            else if (lastMove.toIsValid && MagicAir.oddJunction(from, to, yDistance, yDistChange, yDistDiffEx, maxJumpGain, resetTo, thisMove, lastMove, data, cc)) {
                // Several types of odd in-air moves, mostly with gravity near maximum, friction, medium change.
            }
            else if (thisMove.yDistance < 1.0 && thisMove.yDistance > 0.9 
                    && lastMove.yDistance >= 1.5 && data.sfJumpPhase <= 2
                    && lastMove.verVelUsed != null 
                    && (lastMove.verVelUsed.flags & (VelocityFlags.ORIGIN_BLOCK_MOVE | VelocityFlags.ORIGIN_BLOCK_BOUNCE)) != 0) {
                // Allow too strong decrease.
                // TODO: Another magic check here? Route most checks through methods anyway?
            }
	    else if (isLanternUpper(to)) {
                // Ignore		
	    }
            else {
                vDistRelVL = true;
            }
            // else: Allow moving up less. Note: possibility of low jump.
        }
        else { // if (yDistance < 0.0) // Rather too fast falling.
            if (yDistance < -3.0 && lastMove.yDistance < -3.0 && Math.abs(yDistDiffEx) < 5.0 * Magic.GRAVITY_MAX) {
                // Disregard not falling faster at some point (our constants don't match 100%).
            }
            else if (resetTo && (yDistDiffEx > -Magic.GRAVITY_SPAN || !fromOnGround && !thisMove.touchedGround && yDistChange >= 0.0)) {
                // Moving onto ground allows a shorter move.
                // TODO: Any lost-ground cases? 
            }
            else if (yDistance > lastMove.yDistance - Magic.GRAVITY_MAX - Magic.GRAVITY_SPAN && (resetTo || thisMove.touchedGround)) {
                // Mirrored case for yDistance > yAllowedDistance, hitting ground.
                // TODO: Needs more efficient structure.
            }
            else if (resetFrom && yDistance >= -0.5 && (yDistance > -0.31 || (resetTo || to.isAboveStairs()) && (lastMove.yDistance < 0.0))) {
                // Stairs and other cases moving off ground or ground-to-ground.
                // TODO: Margins !?
            }
            else if (data.liftOffEnvelope == LiftOffEnvelope.LIMIT_LIQUID 
                    && data.sfJumpPhase == 1 && lastMove.toIsValid
                    && lastMove.from.inLiquid && !(lastMove.to.extraPropertiesValid && lastMove.to.inLiquid)
                    && !resetFrom && resetTo // TODO: There might be other cases (possibly wrong bounding box).
                    && lastMove.yDistance > 0.0 && lastMove.yDistance < 0.5 * Magic.GRAVITY_ODD
                    && yDistance < 0.0 && Math.abs(Math.abs(yDistance) - lastMove.yDistance) < Magic.GRAVITY_SPAN / 2.0
                    ) {
                // LIMIT_LIQUID, vDist inversion (!).
            }
            else if (yDistance <= 0.0 && yDistance > -Magic.GRAVITY_MAX - Magic.GRAVITY_SPAN 
                    && (thisMove.headObstructed || lastMove.toIsValid && lastMove.headObstructed && lastMove.yDistance >= 0.0)) {
                // Head was blocked, thus faster decrease than expected.
            }
            else if (lastMove.toIsValid && MagicAir.oddJunction(from, to, yDistance, yDistChange, yDistDiffEx, maxJumpGain, resetTo, thisMove, lastMove, data, cc)) {
                // Several types of odd in-air moves, mostly with gravity near maximum, friction, medium change.
            }
            else if (isLanternUpper(to)) {
                // Ignore.
            }
            else if (isCollideWithHB(from, to, data) && yDistance < -0.125 && yDistance > -0.128) {
                // Side collision with a honey block thus slower movement than expected.
            }
            else if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
                vDistRelVL = false;
            }
            else if (Bridge1_13.hasIsSwimming() &&
                    (data.sfJumpPhase == 3 && lastMove.yDistance < -0.139 && yDistance > -0.1 && yDistance < 0.005
                    || yDistance < -0.288 && yDistance > -0.32 && lastMove.yDistance > -0.1 && lastMove.yDistance < 0.005
                    )) {
                // False positives when break block below too fast. Seem newly appeared in recent versions
            }
            else if (data.bedLeaveTime + 500 > now && yDistance < 0.45) {
                vDistRelVL = false;		
            }
            else {
                // Violation.
                vDistRelVL = true;
            }
            // else Accept small aberrations !?
        }
        
        if (hackStep(now, player, from, fromOnGround, resetFrom, to, resetTo, hDistance, yDistance, lastMove, data, pData)) {
            vDistRelVL = true;
            vDistanceAboveLimit = Math.max(vDistanceAboveLimit, 1.25);
        }

	    
	    
	    
        if (vDistRelVL) {
            if (data.getOrUseVerticalVelocity(yDistance) == null) {
                vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(yDistance - vAllowedDistance));
                tags.add("vdistrel");
            }
        }

	    
	    
	    
        // Absolute y-distance to set back.
        if (yDistance > 0.0 && !data.isVelocityJumpPhase()) {
            // TODO: Only allow higher violation when only in water (1.13 Swimming)
            // TODO: Maintain a value in data, adjusting to velocity?
            // TODO: LIMIT_JUMP 
            final double vAllowedAbsoluteDistance = data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier);
            final double totalVDistViolation =  to.getY() - data.getSetBackY() - vAllowedAbsoluteDistance;
            if (totalVDistViolation > 0.0) {
                // Skip actually stepping up.
                if ((fromOnGround || thisMove.touchedGroundWorkaround || lastMove.touchedGround) 
                        && toOnGround && yDistance <= cc.sfStepHeight) {
                    // Ignore: Legitimate step.
                }
                // Skip if the player could step up by lostground_couldstep.
                else if (yDistance <= cc.sfStepHeight && thisMove.touchedGroundWorkaround && tags.contains("lostground_couldstep")) {
                    // Ignore: Envelope already checked.
                }
                else if (Magic.skipPaper(thisMove, lastMove, data)) {
                    // Tag already set above.
		    // Teleport to in-air (PaperSpigot 1.7.10).
                }
		else if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
		    // Ignore riptiding for now
		}
		else if (isWaterlogged(from)) {
		     // Ignore water logged blocks 
		}
                // Attempt to use velocity.
                else if (data.getOrUseVerticalVelocity(yDistance) == null) {
                    // Violation.
                    vDistanceAboveLimit = Math.max(vDistanceAboveLimit, totalVDistViolation);
                    tags.add("vdistsb");
                }
            }
        }

	    
	    
        if (data.sfLowJump) {
            tags.add("lowjump");
        }

	    
	    
        // More in air checks.
        // TODO: move into the in air checking above !?
        if (!envelopeHack && !resetFrom && !resetTo) {
            // "On-air" checks (vertical, already use velocity if needed).
            vDistanceAboveLimit = Math.max(vDistanceAboveLimit, 
                    inAirChecks(now, from, to, hDistance, yDistance, thisMove, lastMove, data, cc));
        }

	    
	    
        // Block 'step' with yDistance between step height and minJumpGain (vdistrel and vdistsb should catch the rest).
        // TODO: Model other cases of unexpectedly low 'jumping', such as using too few velocity?
        // (Actual step cheats are probably better detected by generalized patterns.)
        if (vDistanceAboveLimit <= 0D 
                && yDistance > cc.sfStepHeight && yDistance < data.liftOffEnvelope.getMinJumpGain(data.jumpAmplifier) 
                && !thisMove.headObstructed && !thisMove.from.resetCond && !thisMove.to.resetCond
                && (thisMove.from.onGround || thisMove.touchedGroundWorkaround) && thisMove.to.onGround 
                ) {
            // Exclude a lost-ground case.
            if (thisMove.touchedGroundWorkaround && lastMove.toIsValid && lastMove.yDistance <= 0.0
                    && yDistance + Math.abs(lastMove.yDistance) <= 2.0 * (maxJumpGain + 0.1)) {
                // TODO: Review: still needed?
            }
            else {
                // Potential violation.
                if (!pData.hasPermission(Permissions.MOVING_SURVIVALFLY_STEP, player) 
                        && data.getOrUseVerticalVelocity(yDistance) == null) {
                    vDistanceAboveLimit = yDistance - cc.sfStepHeight;
                    tags.add("step");
                }
            }
        }

	    
	    
	    
        // Air-stay-time.
        // TODO: max-phase only when from is not reset !?
        // TODO: Only allow higher yDistance when in water (1.13 swimming)
        final int maxJumpPhase = data.liftOffEnvelope.getMaxJumpPhase(data.jumpAmplifier);
        if (!envelopeHack && data.sfJumpPhase > maxJumpPhase && !data.isVelocityJumpPhase()) {
            if (yDistance < 0.0) {
                // Ignore falling, and let accounting deal with it.
           }
           else if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
		// Ignore riptiding.		
	   }	
           else if (resetFrom) {
                // Ignore bunny etc.
            }
            else {
                // Violation (Too high jumping or step).
                if (data.getOrUseVerticalVelocity(yDistance) == null) {
                    vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.max(yDistance, 0.15));
                    tags.add("maxphase");
                }
            }
        }
        return new double[]{vAllowedDistance, vDistanceAboveLimit};
    }

    /**
     * vAllowedDistance with data having been reset after teleport/join/respawn.
     * 
     * @param thisMove
     * @param lastMove
     * @param maxJumpGain
     * @param jumpGainMargin
     * @param data
     * @param cc
     * @return
     */
    private double vAllowedDistanceNoData(
            final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
            final double maxJumpGain, final double jumpGainMargin, 
            final MovingData data, final MovingConfig cc) {
        if (lastMove.valid) {
            tags.add("data_reset");
        }
        else {
            tags.add("data_missing");
        }
        double vAllowedDistance;
        if (thisMove.yDistance > -(Magic.GRAVITY_MAX + Magic.GRAVITY_SPAN) && thisMove.yDistance < 0.0) {
            // Allow falling.
            vAllowedDistance = thisMove.yDistance;
        }
        else if (thisMove.from.onGround) {
            // Allow jumping.
            vAllowedDistance = maxJumpGain + jumpGainMargin;
            if (thisMove.to.onGround) {
                vAllowedDistance = Math.max(cc.sfStepHeight, vAllowedDistance);
            }
        }
        else if (Magic.skipPaper(thisMove, lastMove, data)) {
            // Double arithmetics, moving up after join/teleport/respawn.
            vAllowedDistance = Magic.PAPER_DIST;
            tags.add("skip_paper");
        }
        else {
            // Allow 0 y-distance once.
            vAllowedDistance = 0.0;
        }
        return vAllowedDistance;
    }

    /**
     * Extended in-air checks for vertical move: y-direction changes and accounting.
     * 
     * @param now
     * @param yDistance
     * @param data
     * @param cc
     * @return
     */
    private double inAirChecks(final long now, final PlayerLocation from, final PlayerLocation to, 
            final double hDistance, final double yDistance, 
            final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
            final MovingData data, final MovingConfig cc) {
        double vDistanceAboveLimit = 0;

        // y direction change detection.
        // TODO: Consider using accounting for y-change detection. <- Nope :).
        final boolean yDirChange = lastMove.toIsValid && lastMove.yDistance != yDistance && (yDistance <= 0.0 && lastMove.yDistance >= 0.0 || yDistance >= 0.0 && lastMove.yDistance <= 0.0 ); 
        if (yDirChange) {
            // yDirChange uses velocity if needed.
            vDistanceAboveLimit = yDirChange(from, to, yDistance, vDistanceAboveLimit, lastMove, data);
        }

        // Accounting support.
        if (cc.survivalFlyAccountingV) {
            // Currently only for "air" phases.
            // Vertical.
            if (isCollideWithHB(from, to, data) && thisMove.yDistance < 0.0 && thisMove.yDistance > -0.21) {
                data.vDistAcc.clear();
                data.vDistAcc.add((float)-0.2033);
            }
            else if (yDirChange && lastMove.yDistance > 0) { // lastMove.toIsValid is checked above. 
                // Change to descending phase.
                data.vDistAcc.clear();
                // Allow adding 0.
                data.vDistAcc.add((float) yDistance);
            }
            else if (thisMove.verVelUsed == null) { // Only skip if just used.
                // Here yDistance can be negative and positive.
                //                if (yDistance != 0.0) {
                if ((data.timeRiptiding + 500 > now) || (data.bedLeaveTime + 500 > now && yDistance < 0.45) || isLanternUpper(to) || 
                    isWaterlogged(from) || isWaterlogged(to) || (lastMove.from.inLiquid && Math.abs(yDistance) < 0.31)) {
                    // Ignore
                }
                else {
                data.vDistAcc.add((float) yDistance);
                final double accAboveLimit = verticalAccounting(yDistance, data.vDistAcc ,tags, "vacc" + (data.isVelocityJumpPhase() ? "dirty" : ""));
                if (accAboveLimit > vDistanceAboveLimit) {
                    if (data.getOrUseVerticalVelocity(yDistance) == null) {
                        vDistanceAboveLimit = accAboveLimit;
                    }
                    }
                }
                //                }
            }
            else {
                // TODO: Just to exclude source of error, might be redundant.
                data.vDistAcc.clear();
            }
        }
        return vDistanceAboveLimit;
    }	    

    /** Check if collide the side of HoneyBlock */
    private boolean isCollideWithHB(PlayerLocation from ,PlayerLocation to, MovingData data) {
        final boolean a = BlockProperties.collides(to.getBlockCache(), to.getMinX() - 0.1, to.getMinY(), to.getMinZ() - 0.1, to.getMaxX() + 0.1, to.getMaxY(), to.getMaxZ() + 0.1, BlockProperties.F_STICKY) ||
        BlockProperties.collides(from.getBlockCache(), from.getMinX() - 0.1, from.getMinY(), from.getMinZ() - 0.1, from.getMaxX() + 0.1, from.getMaxY(), from.getMaxZ() + 0.1, BlockProperties.F_STICKY);
        
        // Moving on side block, remove nofall data
        if (a) data.clearNoFallData();
        return a;
    }
    
    private boolean isLanternUpper(PlayerLocation from) {
    	World w = from.getWorld();
    	final int x = from.getBlockX();
    	final int y = from.getBlockY() + 2;
    	final int z = from.getBlockZ();
    	if (w.getBlockAt(x, y, z).getType().toString().equals("LANTERN")) return true;
    	return false;	
    }
	
    private boolean isWaterlogged(PlayerLocation from) {
        if (!Bridge1_13.hasIsSwimming()) return false;
        World w = from.getWorld();
        final int iMinX = Location.locToBlock(from.getMinX());
        final int iMaxX = Location.locToBlock(from.getMaxX());
        final int iMinY = Location.locToBlock(from.getMinY());
        final int iMaxY = Math.min(Location.locToBlock(from.getMaxY()), from.getBlockCache().getMaxBlockY());
        final int iMinZ = Location.locToBlock(from.getMinZ());
        final int iMaxZ = Location.locToBlock(from.getMaxZ());
        
        for (int x = iMinX; x <= iMaxX; x++) {
            for (int z = iMinZ; z <= iMaxZ; z++) {
                 for (int y = iMaxY; y >= iMinY; y--) {
                     BlockData bd = w.getBlockAt(x,y,z).getBlockData();
                     if (bd instanceof Waterlogged) {
                         return ((Waterlogged)bd).isWaterlogged();
                     }
                 }
            }
        }
        return false;
    }
    
    /**
     * Demand that with time the values decrease.<br>
     * The ActionAccumulator instance must have 3 buckets, bucket 1 is checked against
     * bucket 2, 0 is ignored. [Vertical accounting: applies to both falling and jumping]<br>
     * NOTE: This just checks and adds to tags, no change to acc.
     * 
     * @param yDistance
     * @param acc
     * @param tags
     * @param tag Tag to be added in case of a violation of this sub-check.
     * @return A violation value > 0.001, to be interpreted like a moving violation.
     */
    private static final double verticalAccounting(final double yDistance, 
            final ActionAccumulator acc, final ArrayList<String> tags, final String tag) {
        // TODO: Add air friction and do it per move anyway !?
        final int count0 = acc.bucketCount(0);
        if (count0 > 0) {
            final int count1 = acc.bucketCount(1);
            if (count1 > 0) {
                final int cap = acc.bucketCapacity();
                final float sc0;
                if (count0 == cap) {
                    sc0 = acc.bucketScore(0);
                }
                else {
                    // Catch extreme changes quick.
                    sc0 = acc.bucketScore(0) * (float) cap / (float) count0 - Magic.GRAVITY_VACC * (float) (cap - count0);
                }
                final float sc1 = acc.bucketScore(1);
                if (sc0 > sc1 - 3.0 * Magic.GRAVITY_VACC) {
                    // TODO: Velocity downwards fails here !!!
                    if (yDistance <= -1.05 && sc1 < -8.0 && sc0 < -8.0) { // (aDiff < Math.abs(yDistance) || sc2 < - 10.0f)) {
                        // High falling speeds may pass.
                        tags.add(tag + "grace");
                        return 0.0;
                    }
                    tags.add(tag);
                    return sc0 - (sc1 - 3.0 * Magic.GRAVITY_VACC);
                }
            }
        }
        return 0.0;
    }

    /**
     * Check on change of y direction. Needs last move data.
     * 
     * @param yDistance
     * @param vDistanceAboveLimit
     * @return vDistanceAboveLimit
     */
    private double yDirChange(final PlayerLocation from, final PlayerLocation to, 
            final double yDistance, double vDistanceAboveLimit, 
            final PlayerMoveData lastMove, final MovingData data) {
			long now = System.currentTimeMillis();
        // TODO: Does this account for velocity in a sufficient way?
        if (yDistance > 0) {
            // TODO: Clear active vertical velocity here ?
            // TODO: Demand consuming queued velocity for valid change (!).
            // Increase
            if (lastMove.touchedGround || lastMove.to.extraPropertiesValid && lastMove.to.resetCond) {
                tags.add("ychinc");
            }
            else {
                // Moving upwards after falling without having touched the ground.
                if (data.bunnyhopDelay < 9 && !((lastMove.touchedGround || lastMove.from.onGroundOrResetCond) && lastMove.yDistance == 0D) && data.getOrUseVerticalVelocity(yDistance) == null) {
                    // TODO: adjust limit for bunny-hop.
                    if ((data.timeRiptiding + 500 > now) 
			|| (data.bedLeaveTime + 500 > now && yDistance < 0.45) 
			|| isLanternUpper(to) || isWaterlogged(from)) {
			// Ignore.		
		   } 
		   else {
		     vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(yDistance));
                     tags.add("ychincfly");
	           }
                }
                else {
                    tags.add("ychincair");
                }
            }
        }
        else {
            // Decrease
            tags.add("ychdec");
            // Detect low jumping.
            // TODO: sfDirty: Account for actual velocity (demands consuming queued for dir-change(!))!
            if (!data.sfLowJump && !data.sfNoLowJump && data.liftOffEnvelope == LiftOffEnvelope.NORMAL &&
                    lastMove.toIsValid && lastMove.yDistance > 0.0 && !data.isVelocityJumpPhase()) {
                final double setBackYDistance = from.getY() - data.getSetBackY();
                if (setBackYDistance > 0.0) {
                    // Only count it if the player has actually been jumping (higher than setback).
                    final Player player = from.getPlayer();
                    // Estimate of minimal jump height.
                    double estimate = 1.15;
                    if (data.jumpAmplifier > 0) {
                        // TODO: Could skip this.
                        estimate += 0.5 * aux.getJumpAmplifier(player);
                    }
                    if (setBackYDistance < estimate) {
                        // Low jump, further check if there might have been a reason for low jumping.
                        if (data.playerMoves.getCurrentMove().headObstructed || yDistance <= 0.0 
                                && lastMove.headObstructed && lastMove.yDistance >= 0.0) {
                            // Exempt.
                            tags.add("nolowjump_ceil");
                        }
                        else {
                            tags.add("lowjump_set");
                            data.sfLowJump = true;
                        }
                    }
                }
            } // (Low jump.)
        }
        return vDistanceAboveLimit;
    }

    /**
     * After-failure checks for horizontal distance.
     * 
     * buffers and velocity, also re-check hDist with permissions, if needed.
     * 
     * 
     * @param player
     * @param from
     * @param to
     * @param hAllowedDistance
     * @param hDistanceAboveLimit
     * @param sprinting
     * @param thisMove
     * @param lastMove
     * @param data
     * @param cc
     * @param skipPermChecks
     * @return hAllowedDistance, hDistanceAboveLimit, hFreedom
     */
    private double[] hDistAfterFailure(final Player player, 
            final PlayerLocation from, final PlayerLocation to, 
            double hAllowedDistance, double hDistanceAboveLimit, final boolean sprinting, 
            final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
            final MovingData data, final MovingConfig cc, final IPlayerData pData, 
            final boolean skipPermChecks) {
				
	long now = System.currentTimeMillis();
        // TODO: Still not entirely sure about this checking order.
        // TODO: Would quick returns make sense for hDistanceAfterFailure == 0.0?

        // Test bunny early, because it applies often and destroys as little as possible.
        if (!(from.isAboveStairs() && to.isAboveStairs() && to.isOnGround())){
              hDistanceAboveLimit = bunnyHop(from, to, hAllowedDistance, hDistanceAboveLimit, sprinting, thisMove, lastMove, data, cc);
	}
        // After failure permission checks ( + speed modifier + sneaking + blocking + speeding) and velocity (!).
        // Noslow require no permission rechecks
        if (hDistanceAboveLimit > 0.12 
	    && !tags.contains("usingitem(cancel)") 
	    && !skipPermChecks 
	    && !thisMove.from.inLiquid && !thisMove.to.inLiquid 
	    && Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(player))
	   )  {
            // TODO: Most cases these will not apply. Consider redesign to do these last or checking right away and skip here on some conditions.
            hAllowedDistance = setAllowedhDist(player, sprinting, thisMove, data, cc, pData, from, true);
            hDistanceAboveLimit = thisMove.hDistance - hAllowedDistance;
            tags.add("permchecks-outliq");
        }
	if (hDistanceAboveLimit > 0.6 
	    && !skipPermChecks 
	    && thisMove.from.inLiquid && thisMove.to.inLiquid 
	    && !Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(player))
	   ) {
	    hAllowedDistance = setAllowedhDist(player, sprinting, thisMove, data, cc, pData, from, true);
            hDistanceAboveLimit = thisMove.hDistance - hAllowedDistance;
            tags.add("permchecks-inliq");
	 }

	    
        // Check being moved by blocks.
        if (cc.trackBlockMove && hDistanceAboveLimit > 0.0 
                && hDistanceAboveLimit < 1.025 // MAGIC
                ) {
            // Push by 0.49-0.51 in one direction. Also observed 1.02.
            // TODO: Better also test if the per axis distance is equal to or exceeds hDistanceAboveLimit?
            // TODO: The minimum push value can be misleading (blocked by a block?)
            final double xDistance = to.getX() - from.getX();
            final double zDistance = to.getZ() - from.getZ();
            if (Math.abs(xDistance) > 0.485 && Math.abs(xDistance) < 1.025
                    && from.matchBlockChange(blockChangeTracker, data.blockChangeRef, 
                            xDistance < 0 ? Direction.X_NEG : Direction.X_POS, 0.05)) {
                hAllowedDistance = thisMove.hDistance; // MAGIC
                hDistanceAboveLimit = 0.0;
            }
            else if (Math.abs(zDistance) > 0.485 && Math.abs(zDistance) < 1.025
                    && from.matchBlockChange(blockChangeTracker, data.blockChangeRef, 
                            zDistance < 0 ? Direction.Z_NEG : Direction.Z_POS, 0.05)) {
                hAllowedDistance = thisMove.hDistance; // MAGIC
                hDistanceAboveLimit = 0.0;
            }
        }

	    
        // Check velocity.
        double hFreedom = 0.0; // Horizontal velocity used.
        if (hDistanceAboveLimit > 0.0) {
            // (hDistanceAboveLimit > 0.0)
            hFreedom = data.getHorizontalFreedom();
            if (hFreedom < hDistanceAboveLimit) {
                // Use queued velocity if possible.
                hFreedom += data.useHorizontalVelocity(hDistanceAboveLimit - hFreedom);
            }
            if (hFreedom > 0.0) {
                tags.add("hvel");
                hDistanceAboveLimit = Math.max(0.0, hDistanceAboveLimit - hFreedom);
                if (hDistanceAboveLimit <= 0.0) {
                    data.combinedMediumHCount = 0;
                    data.combinedMediumHValue = 0.0;
                	tags.add("hvel_no_hacc");
                }
            }
        }

	    
        // After failure bunny (2nd).
        if (hDistanceAboveLimit > 0 && !(from.isAboveStairs() && to.isAboveStairs() && to.isOnGround())) {
            // (Could distinguish tags from above call).
            hDistanceAboveLimit = bunnyHop(from, to, hAllowedDistance, hDistanceAboveLimit, sprinting, thisMove, lastMove, data, cc);
        }

	    
        // Horizontal buffer.
        // TODO: Consider to confine use to "not in air" and similar.
        if (hDistanceAboveLimit > 0.0 && data.sfHorizontalBuffer > 0.0) {
        	//if (snowFix && data.sfHorizontalBuffer == 1) {
            // Ignore
        	//} else {
        	// Handle buffer only if moving too far.
            // Consume buffer.
            tags.add("hbufuse");
            final double amount = Math.min(data.sfHorizontalBuffer, hDistanceAboveLimit);
            hDistanceAboveLimit -= amount;
            // Ensure we never end up below zero.
            data.sfHorizontalBuffer = Math.max(0.0, data.sfHorizontalBuffer - amount);
        	//}
        }
		
	if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
	   if (Bridge1_9.isGliding(player) && hDistanceAboveLimit< 0.5) {
		hDistanceAboveLimit = 0.0;
	   }
	   tags.add("hriptide");
        }

        // Attempt to fix server-side blocking & bow drawn
        if (hDistanceAboveLimit > 0.0 && tags.contains("usingitem") && (!Bridge1_9.hasGetItemInOffHand() && player.isBlocking() 
	    || Bridge1_9.getItemInMainHand(player).getType() == Material.BOW)) {

            ItemStack stack = Bridge1_9.getItemInMainHand(player);
            if (stack != null) {
                tags.add("itemreset");
                Bridge1_9.setItemInMainHand(player, new ItemStack(Material.AIR));
                Bridge1_9.setItemInMainHand(player, stack);
            }
        }

        // Add the hspeed tag on violation.
        if (hDistanceAboveLimit > 0.0) {
            tags.add("hspeed");
        }
        return new double[]{hAllowedDistance, hDistanceAboveLimit, hFreedom};
    }

    /**
     * Test bunny hop / bunny fly. Does modify data only if 0.0 is returned.
     * @param from
     * @param to
     * @param hDistance
     * @param hAllowedDistance
     * @param hDistanceAboveLimit
     * @param yDistance
     * @param sprinting
     * @param data
     * @return hDistanceAboveLimit
     */
    private double bunnyHop(final PlayerLocation from, final PlayerLocation to, 
            final double hAllowedDistance, double hDistanceAboveLimit, final boolean sprinting, 
            final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
            final MovingData data, final MovingConfig cc) {
        // Check "bunny fly" here, to not fall over sprint resetting on the way.
        boolean allowHop = true;
        boolean double_bunny = false;

        // Pull down.
        final double hDistance = thisMove.hDistance;
        final double yDistance = thisMove.yDistance;
        // TODO: hDistanceBaseRef: Distinguish where to exclude sprint modifier?
        final double hDistanceBaseRef = thisMove.hAllowedDistanceBase;

        // TODO: A more state-machine like modeling (hop, slope, states, low-edge).

        // Fly phase.
        // TODO: Check which conditions might need resetting at lower speed (!).
        // Friction phase.
        if (lastMove.toIsValid && data.bunnyhopDelay > 0 && hDistance > hDistanceBaseRef) {
            // (lastHDist may be reset on commands.)
            allowHop = false; // Magic!
            final int hopTime = bunnyHopMax - data.bunnyhopDelay;

            // Increase buffer if hDistance is decreasing properly.
            if (lastMove.hDistance > hDistance) {
                final double hDistDiff = lastMove.hDistance - hDistance;

                // Bunny slope (downwards, directly after hop but before friction).
                if (data.bunnyhopDelay == bunnyHopMax - 1) {
                    // Ensure relative speed decrease vs. hop is met somehow.
                    if (hDistDiff >= 0.66 * (lastMove.hDistance - hDistanceBaseRef)) {
                        tags.add("bunnyslope");
                        hDistanceAboveLimit = 0.0;
                    }
                }
                else if (hDistDiff >= lastMove.hDistance / bunnyDivFriction || hDistDiff >= hDistanceAboveLimit / 33.3 || 
                        hDistDiff >= (hDistance - hDistanceBaseRef) * (1.0 - Magic.FRICTION_MEDIUM_AIR)
                        ) {
                    // TODO: Confine friction by medium ?
                    // TODO: Also calculate an absolute (minimal) speed decrease over the whole time, at least max - count?
                    tags.add("bunnyfriction");
                    // Speed must decrease by "a lot" at first, then by some minimal amount per event.
                    // TODO: Confine buffer to only be used during low jump phase !?
                    //if (!(data.toWasReset && thisMove.from.onGround && thisMove.to.onGround)) { // FISHY

                    // Allow the move.
                    if (data.isVelocityJumpPhase()) {
                        hDistanceAboveLimit = 0.0;
                    } else if (hDistDiff * 15 >= hDistanceAboveLimit && hDistance < hDistanceBaseRef * 1.25) hDistanceAboveLimit = 0.0;
                    if (data.bunnyhopDelay == 1 && !thisMove.to.onGround && !to.isResetCond()) {
                        // ... one move between toonground and liftoff remains for hbuf ...
                        data.bunnyhopDelay++;
                        tags.add("bunnyfly(keep)");
                    }
                    else {
                        tags.add("bunnyfly(" + data.bunnyhopDelay +")");
                    }
                    //}
                    //}
                }
            }

            // More leniency for jumping with head obstructed
            if (from.isHeadObstructed() && thisMove.to.onGround) {
                if (data.bunnyhopTick == 5 && data.bunnyhopDelay == 7 && thisMove.from.onGround && hDistance <= 0.47) {
                    hDistanceAboveLimit = 0.0;
                    data.sfHorizontalBuffer = cc.hBufMax;
                    tags.add("bunny_hbuf");
                }

                if (data.bunnyhopTick == 6 && data.bunnyhopDelay == 8 && hDistance <= 0.4) {
                    hDistanceAboveLimit = 0.0;
                    data.sfHorizontalBuffer = cc.hBufMax;
                    tags.add("bunny_hbuf");
                }
            }

            // 2x horizontal speed increase detection.
            if (!allowHop && hDistance - lastMove.hDistance >= hDistanceBaseRef * 0.5 && hopTime == 1) {
                if (lastMove.yDistance >= -Magic.GRAVITY_MAX / 2.0 && lastMove.yDistance <= 0.0 && yDistance >= 0.4 
                        && lastMove.touchedGround) {
                    // TODO: Confine to increasing set back y ?
                    tags.add(DOUBLE_BUNNY);
                    allowHop = double_bunny = true;
                }
            }

            // Allow hop for special cases.
            if (!allowHop && (thisMove.from.onGround || thisMove.touchedGroundWorkaround)) {
                // TODO: Better reset delay in this case ?
                if (data.bunnyhopDelay <= 6) {
                    // TODO: Confine further ?
                    tags.add("ediblebunny");
                    allowHop = true;
                }
                else if (lastMove.yDistance < 0.0 && thisMove.to.onGround && thisMove.yDistance == 0.0 
                        && Magic.fallAfterHeadObstructed(data, 2) 
                        && lastMove.hDistance > lastMove.hAllowedDistanceBase && lastMove.hDistance < 1.34 * lastMove.hAllowedDistanceBase
                        && thisMove.hDistance > lastMove.hDistance * 1.24
                        && thisMove.hDistance < lastMove.hDistance * 1.34
                        || yDistance >= 0.0 && thisMove.headObstructed // || to.isHeadObstructed()
                        ) {
                    // TODO: headObstructed: check always and set a flag in data + consider regain buffer?
                    tags.add("headbangbunny");
                    allowHop = true;
                    // TODO: Magic.
                    if (data.combinedMediumHValue / (double) data.combinedMediumHCount < 1.5) {
                        // TODO: Reset to 1 and min(allowed, actual) rather.
                        data.combinedMediumHCount = 0;
                        data.combinedMediumHValue = 0.0;
                        tags.add("bunny_no_hacc");
                    }
                }
            }

        }

        // Fix jumping on slime blocks
        if ((from.getBlockFlags() & BlockProperties.F_BOUNCE25) != 0
                && (data.bunnyhopDelay <= 4 && data.bunnyhopTick == 0 || data.bunnyhopDelay == 6 && data.bunnyhopTick == 1)
                && !thisMove.from.onGround && hDistance < 0.5) {
            hDistanceAboveLimit = 0.0;
            data.sfHorizontalBuffer = 0.5;
            tags.add("bounce_bunny");
        }

        // bunnyhop-> bunnyslope-> bunnyfriction-> ground-> microjump(still bunnyfriction)-> bunnyfriction
        //or bunnyhop-> ground-> slidedown-> bunnyfriction
        // Hit ground but slipped away by somehow and still remain bunny friction
        final double inc = Bridge1_13.hasIsSwimming() ? 0.03 : 0;
        if (lastMove.toIsValid && data.bunnyhopDelay <= 0 && data.lastbunnyhopDelay > 0 && lastMove.hDistance > hDistance 
            && hDistanceBaseRef > 0.0 && hDistance / hDistanceBaseRef < (data.bunnyhopTick > 0 ? (data.bunnyhopTick > 2 ? 1.0 + inc : 1.11 + inc) : 1.22 + inc)) {
            final double hDistDiff = lastMove.hDistance - hDistance;
            if (hDistDiff >= lastMove.hDistance / bunnyDivFriction || hDistDiff >= hDistanceAboveLimit / 33.3 || 
                hDistDiff >= (hDistance - hDistanceBaseRef) * (1.0 - Magic.FRICTION_MEDIUM_AIR)) {
                //if (data.lastbunnyhopDelay == 8 && thisMove.from.onGround && !thisMove.to.onGround) {
                //    data.lastbunnyhopDelay++;
                //    tags.add("bunnyfriction(keep)"); // TODO: Never happen?
                //} else 
                if (hDistDiff < 0.01) {
                    // Allow the move
                    hDistanceAboveLimit = 0.0;
                    tags.add("bunnyfriction");
                    // Remove lowjump in this hop, prevent false in next hop
                    if (data.sfLowJump) {
                        data.sfLowJump = false;
                        tags.add("lowjump_remove");
                        if (data.lastbunnyhopDelay > 3) data.lastbunnyhopDelay = 3;
                    }
                } else data.lastbunnyhopDelay = 0;
            } else data.lastbunnyhopDelay = 0;
        }

        // Check hop (singular peak up to roughly two times the allowed distance).
        // TODO: Needs better modeling.
        if (allowHop && hDistance >= hDistanceBaseRef
                && (hDistance > (((!lastMove.toIsValid || lastMove.hDistance == 0.0 && lastMove.yDistance == 0.0) ? 1.11 : 1.314)) * hDistanceBaseRef) 
                && hDistance < (data.bunnyhopTick > 0 ? (data.bunnyhopTick > 2 ? 1.76 : 1.96) : 2.15) * hDistanceBaseRef
                // TODO: Walk speed (static or not) is not a good reference, switch to need normal/base speed instead.
                || (yDistance > from.getyOnGround() || hDistance < (data.bunnyhopTick > 0 ? (data.bunnyhopTick > 2 ? 1.9 : 2.1) : 2.3) * hDistanceBaseRef) && lastMove.toIsValid && hDistance > 1.314 * lastMove.hDistance && hDistance < 2.15 * lastMove.hDistance
                ) { // if (sprinting) {
            // TODO: Test bunny spike over all sorts of speeds + attributes.
            // TODO: Allow slightly higher speed on lost ground?
            // TODO: LiftOffEnvelope.allowBunny ?
            if (data.liftOffEnvelope == LiftOffEnvelope.NORMAL
                    && (!data.sfLowJump || data.sfNoLowJump)
                    // 0: Y-distance envelope.
                    && yDistance >= 0.0
                    && (
                            // 1: Normal jumping.
                            yDistance > 0.0 
                            && yDistance > data.liftOffEnvelope.getMinJumpGain(data.jumpAmplifier) - Magic.GRAVITY_SPAN
                            // 1: Too short with head obstructed.
                            || thisMove.headObstructed || lastMove.toIsValid && lastMove.headObstructed && lastMove.yDistance <= 0.0
                            // 1: Hop without y distance increase at moderate h-speed.
                            // TODO: 2nd below: demand next move to jump. Relate to stored past moves. 
                            // TODO: Ensure the gain can only be used once per so and so.
                            || (cc.sfGroundHop || yDistance == 0.0 && !lastMove.touchedGroundWorkaround && !lastMove.from.onGround)
                            && hDistanceBaseRef > 0.0 && hDistance / hDistanceBaseRef < 1.5
                            && (hDistance / lastMove.hDistance < 1.35 
                                    || hDistance / hDistanceBaseRef < 1.35)
                            )
                    // 0: Ground + jump phase conditions.
                    && (
                            // 1: Ordinary/obvious lift-off.
                            data.sfJumpPhase == 0 && thisMove.from.onGround 
                            // 1: Touched ground somehow.
                            || data.sfJumpPhase <= 1 && (thisMove.touchedGroundWorkaround || 
                                    lastMove.touchedGround && !lastMove.bunnyHop) 
                            // 1: Double bunny.
                            || double_bunny)
                    // 0: Don't allow bunny to run out of liquid.
                    && !from.isResetCond() && !to.isResetCond() // TODO: !to.isResetCond() should be reviewed.
                    ) {
                // TODO: Jump effect might allow more strictness. 
                // TODO: Expected minimum gain depends on last speed (!).
                // TODO: Speed effect affects hDistanceAboveLimit?
                data.bunnyhopDelay = bunnyHopMax;
                hDistanceAboveLimit = 0D;
                thisMove.bunnyHop = true;
                tags.add("bunnyhop");
            }
            else {
                tags.add("bunnyenv");
            }
        }

        return hDistanceAboveLimit;
    }

    /**
     * Legitimate move: increase horizontal buffer somehow.
     * @param hDistance
     * @param amount Positive amount.
     * @param data
     */
    private void hBufRegain(final double hDistance, final double amount, 
            final MovingData data, final MovingConfig cc) {
        /*
         * TODO: Consider different concepts: 
         * 			- full resetting with harder conditions.
         * 			- maximum regain amount.
         * 			- reset or regain only every x blocks h distance.
         */
        // TODO: Confine general conditions for buffer regain further (regain in air, whatever)?
        data.sfHorizontalBuffer = Math.min(cc.hBufMax, data.sfHorizontalBuffer + amount);
    }

    /**
     * Inside liquids vertical speed checking. setFrictionJumpPhase must be set
     * externally.
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
            final MovingData data, final Player player) {
	    
        data.sfNoLowJump = true;
	long now = System.currentTimeMillis();

        // Expected envelopes.
        final double baseSpeed;
        if (player.isOnGround()) {
        baseSpeed = Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player)) + 0.1;   
        } else {
        baseSpeed = Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player)); } // TODO: Lava?
        final double yDistAbs = Math.abs(yDistance);

        // TODO: Later also cover things like a sudden stop.

        // Minimal speed.
        if (yDistAbs <= baseSpeed) {
            return new double[]{baseSpeed, 0.0};
        }

        // Friction envelope (allow any kind of slow down).
        final double frictDist = lastMove.toIsValid ? Math.abs(lastMove.yDistance) * data.lastFrictionVertical : baseSpeed; // Bounds differ with sign.
        if (lastMove.toIsValid) {
            if (lastMove.yDistance < 0.0 && yDistance < 0.0 && yDistAbs < frictDist + Magic.GRAVITY_MAX + Magic.GRAVITY_SPAN) {
                return new double[]{-frictDist - Magic.GRAVITY_MAX - Magic.GRAVITY_SPAN, 0.0};
            }
            if (lastMove.yDistance > 0.0 && yDistance > 0.0 && yDistance < frictDist - Magic.GRAVITY_SPAN) {
                return new double[]{frictDist - Magic.GRAVITY_SPAN, 0.0};
            }
            // Jump out water near edge ground
            if (lastMove.yDistance < -0.5 && yDistance > 0.4 && yDistance < frictDist - Magic.GRAVITY_MAX && from.isOnGround(0.6)) {
                return new double[]{frictDist - Magic.GRAVITY_MAX, 0.0};
            }
            // ("== 0.0" is covered by the minimal speed check above.)
        }

        // Workarounds for special cases.
        final Double wRes = MagicLiquid.liquidWorkarounds(from, to, baseSpeed, frictDist, lastMove, data);
        if (wRes != null) {
            return new double[]{wRes, 0.0};
        }

        // Try to use velocity for compensation.
        if (data.getOrUseVerticalVelocity(yDistance) != null) {
            return new double[]{yDistance, 0.0};
        }

        if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 1000 > now)) {
            return new double[]{yDistance, 0.0};
        }

	// TODO: Set magic speeds
	if (isBubbleColumn(from)) {
            return new double[]{yDistance, 0.0};
        }
        // At this point a violation.
        tags.add(yDistance < 0.0 ? "swimdown" : "swimup");
        final double vl1 = yDistAbs - baseSpeed;
        final double vl2 = Math.abs(yDistAbs - frictDist - (yDistance < 0.0 ? Magic.GRAVITY_MAX + Magic.GRAVITY_SPAN : Magic.GRAVITY_MIN));
        if (vl1 <= vl2) {
            return new double[]{yDistance < 0.0 ? -baseSpeed : baseSpeed, vl1};
        }
        else {
            return new double[]{yDistance < 0.0 ? -frictDist - Magic.GRAVITY_MAX - Magic.GRAVITY_SPAN : frictDist - Magic.GRAVITY_SPAN, vl2};
        }
    }

    private boolean isBubbleColumn(PlayerLocation from) {
        if (!Bridge1_13.hasIsSwimming()) return false;
        if (BlockProperties.collidesBlock(from.getBlockCache(), from.getMinX(), from.getMinY(), from.getMinZ(), from.getMaxX(), from.getMaxY(), from.getMaxZ(), Material.BUBBLE_COLUMN)) {
            return true;
        }
        return false;
    }
	
    /**
     * On-climbable vertical distance checking.
     * @param from
     * @param fromOnGround
     * @param toOnGround
     * @param lastMove 
     * @param thisMove 
     * @param yDistance
     * @param data
     * @return vDistanceAboveLimit
     */
    private double vDistClimbable(final Player player, 
            final PlayerLocation from, final PlayerLocation to,
            final boolean fromOnGround, final boolean toOnGround, 
            final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
            final double yDistance, final MovingData data) {
        double vDistanceAboveLimit = 0.0;
        data.sfNoLowJump = true;

        // Clear active horizontal velocity.
        data.clearActiveHorVel();
        // TODO: Might not be able to ignore vertical velocity if moving off climbable (!).

        // TODO: bring in in-medium accounting.
        //    	// TODO: make these extra checks to the jumpphase thing ?
        //    	if (fromOnGround) vAllowedDistance = climbSpeed + 0.3;
        //    	else vAllowedDistance = climbSpeed;
        //    	vDistanceAboveLimit = Math.abs(yDistance) - vAllowedDistance;
        //    	if (vDistanceAboveLimit > 0) tags.add("vclimb");
        final double jumpHeight = 1.35 + (data.jumpAmplifier > 0 ? (0.6 + data.jumpAmplifier - 1.0) : 0.0);
        // TODO: ladders are ground !
        // TODO: yDistance < 0.0 ?
        final double maxSpeed = yDistance < 0.0 ? Magic.climbSpeedDescend : Magic.climbSpeedAscend;
        if (Math.abs(yDistance) > maxSpeed) {
            if (from.isOnGround(jumpHeight, 0D, 0D, BlockProperties.F_CLIMBABLE)) {
                if (yDistance > data.liftOffEnvelope.getMaxJumpGain(data.jumpAmplifier)+ 0.1) {
                    tags.add("climbstep");
                    vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(yDistance) - maxSpeed);
		 }
            } 
	    else if (!isWaterlogged(from)) {
                  tags.add("climbspeed");
                  vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(yDistance) - maxSpeed);
            }
	    long now = System.currentTimeMillis();
            if ((Bridge1_13.isRiptiding(player) || data.timeRiptiding + 3000 > now) && vDistanceAboveLimit< 4.0) {
            	vDistanceAboveLimit = 0.0;
            } 
        }
        if (yDistance > 0) {
            if (!data.playerMoves.getCurrentMove().touchedGround) {
                // Check if player may climb up.
                // (This does exclude ladders.)
                if (!from.canClimbUp(jumpHeight)) {
                    tags.add("climbdetached");
                    vDistanceAboveLimit = Math.max(vDistanceAboveLimit, yDistance);
                }
            }
        }
        // Do allow friction with velocity.
        if (vDistanceAboveLimit > 0.0 && thisMove.yDistance > 0.0 
            && lastMove.yDistance - (Magic.GRAVITY_MAX + Magic.GRAVITY_MIN) / 2.0 > thisMove.yDistance) {
            // TODO: Actual friction or limit by absolute y-distance?
            // TODO: Looks like it's only a problem when on ground?
            vDistanceAboveLimit = 0.0;
            tags.add("vfrict_climb");
        }
        // Do allow vertical velocity.
        // TODO: Looks like less velocity is used here (normal hitting 0.361 of 0.462).
        if (vDistanceAboveLimit > 0.0 && data.getOrUseVerticalVelocity(yDistance) != null) {
            vDistanceAboveLimit = 0.0;
        }
        return vDistanceAboveLimit;
    }

    /**
     * In-web vertical distance checking.
     * @param player
     * @param from
     * @param to
     * @param toOnGround
     * @param hDistanceAboveLimit
     * @param yDistance
     * @param now
     * @param data
     * @param cc
     * @return vAllowedDistance, vDistanceAboveLimit
     */
    private double[] vDistWeb(final Player player, final PlayerMoveData thisMove, 
            final boolean toOnGround, final double hDistanceAboveLimit, final long now, 
            final MovingData data, final MovingConfig cc, final PlayerLocation from) {
	    
        final double yDistance = thisMove.yDistance;
        double vAllowedDistance = 0.0;
        double vDistanceAboveLimit = 0.0;
        data.sfNoLowJump = true;
        data.jumpAmplifier = 0; // TODO: later maybe fetch.
	    
        // Players cannot ascend in webs
        if (yDistance >= 0.0) {
            if (toOnGround && yDistance <= 0.5) {
                // Step up. Note: Does not take into account jump effect on purpose.
                vAllowedDistance = yDistance;
                if (yDistance > 0.0) {
                    tags.add("web_step");
                }
            }
            else {
                // TODO: Could prevent not moving down if not on ground (or on ladder or in liquid?).
		// Allow moving upwards a bit if the player is on ground
                vAllowedDistance = thisMove.from.onGround ? 0.1D : 0;
                from.collectBlockFlags();
		// Players can "halfjump" in berry bushes
                if ((from.getBlockFlags() & BlockProperties.F_COBWEB2) != 0) {
		    vAllowedDistance = 0.315;
		}
            }
	    // Compatibility with multiprotocol plugins
            if ((from.getBlockFlags() & BlockProperties.F_ALLOW_LOWJUMP) != 0) {
                vAllowedDistance = LiftOffEnvelope.NORMAL.getMaxJumpGain(data.jumpAmplifier) + 0.005;
            }
            vDistanceAboveLimit = yDistance - vAllowedDistance;
        }
	// Descending in web/sweet berry bushes.
        else {
            // Is checking resetCond necessary here?
            if (thisMove.from.resetCond && thisMove.to.resetCond && (from.getBlockFlags() & BlockProperties.F_COBWEB2) == 0
               && (from.getBlockFlags() & BlockProperties.F_ALLOW_LOWJUMP) == 0) {
        	// Players can descend faster if they're also moving horizontally 
                if (thisMove.hDistance > 0.018) {
                    vAllowedDistance = -0.062;
                    vDistanceAboveLimit = yDistance < -0.06 ? Math.abs(yDistance - 0.06) : 0;
                } 
		else {
                    vAllowedDistance = -0.032;
                    vDistanceAboveLimit = yDistance < -0.035 ? Math.abs(yDistance - 0.032) : 0;
                }
            }
	    else if ((from.getBlockFlags() & BlockProperties.F_COBWEB2) != 0) {
        	// Allow a faster descend speed when the player first enters berry (jumping on from the top)
        	if (data.insideMediumCount == 0) {
        	    vAllowedDistance = -0.227;
        	    vDistanceAboveLimit = yDistance < -0.227 ? Math.abs(yDistance - 0.227) : 0;
        	} 
		else {
        	    vAllowedDistance = -0.06;
            	    vDistanceAboveLimit = yDistance < -0.06 ? Math.abs(yDistance - 0.06) : 0;
        	}
            }
        }
	    
        /* Should flag as normal rather than doing silent setbacks. Also causes issues with descend check, meaning the player is never flagged.
        if (cc.survivalFlyCobwebHack && vDistanceAboveLimit > 0.0 && hDistanceAboveLimit <= 0.0) {
            // TODO: Seemed fixed at first by CB/MC, but still does occur due to jumping. 
            if (hackCobweb(player, data, thisMove, now, vDistanceAboveLimit)) {
                return new double[]{Double.MIN_VALUE, Double.MIN_VALUE};
            }
        } */
	    
        if (((from.getBlockFlags() & BlockProperties.F_COBWEB2) != 0) && vDistanceAboveLimit > 0.0){
            tags.add(yDistance > 0.0 ? "vbush" : "vbushdesc");
        }
        else if (vDistanceAboveLimit > 0.0) {
            tags.add(yDistance > 0.0 ? "vweb" : "vwebdesc");
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
            final MovingData data, final MovingConfig cc)
    {
        // Increment violation level.
        data.survivalFlyVL += result;
        //data.sfVLTime = data.getPlayerMoveCount();
        final ViolationData vd = new ViolationData(this, player, data.survivalFlyVL, result, cc.survivalFlyActions);
        if (vd.needsParameters()) {
            vd.setParameter(ParameterName.LOCATION_FROM, String.format(Locale.US, "%.2f, %.2f, %.2f", from.getX(), from.getY(), from.getZ()));
            vd.setParameter(ParameterName.LOCATION_TO, String.format(Locale.US, "%.2f, %.2f, %.2f", to.getX(), to.getY(), to.getZ()));
            vd.setParameter(ParameterName.DISTANCE, String.format(Locale.US, "%.2f", TrigUtil.distance(from, to)));
            vd.setParameter(ParameterName.TAGS, StringUtil.join(tags, "+"));
        }
        // Some resetting is done in MovingListener.
        if (executeActions(vd).willCancel()) {
            data.sfVLTime = data.getPlayerMoveCount();
            // Set back + view direction of to (more smooth).
            return MovingUtil.getApplicableSetBackLocation(player, 
                    to.getYaw(), to.getPitch(), to, 
                    data, cc);
        }
        else {
            data.sfVLTime = data.getPlayerMoveCount();
            // TODO: Evaluate how data resetting can be done minimal (skip certain things flags)?
            data.clearAccounting();
            data.sfJumpPhase = 0;
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
    public final void handleHoverViolation(final Player player, 
            final PlayerLocation loc, final MovingConfig cc, final MovingData data) {
        data.survivalFlyVL += cc.sfHoverViolation;

        // TODO: Extra options for set back / kick, like vl?
        data.sfVLTime = data.getPlayerMoveCount();
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
            final Location newTo = MovingUtil.getApplicableSetBackLocation(player, 
                    loc.getYaw(), loc.getPitch(), loc, data, cc);
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
     * Allow accumulating some vls and silently set the player back.
     * 
     * @param player
     * @param data
     * @param cc
     * @param to
     * @param now
     * @param vDistanceAboveLimit
     * @return If to silently set back.
     */
    private boolean hackCobweb(final Player player, final MovingData data, 
            final PlayerMoveData thisMove, final long now, final double vDistanceAboveLimit) {
        if (now - data.sfCobwebTime > 3000) {
            data.sfCobwebTime = now;
            data.sfCobwebVL = vDistanceAboveLimit * 100D;
        }
        else {
            data.sfCobwebVL += vDistanceAboveLimit * 100D;
        }
        if (data.sfCobwebVL < 550) { // Totally random !
            // Silently set back.
            if (!data.hasSetBack()) { // TODO: Assume redundant.
                data.setSetBack(player.getLocation(useLoc)); // ? check moment of call.
                useLoc.setWorld(null);
            }
            data.sfJumpPhase = 0;
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * This is set with PlayerToggleSneak, to be able to distinguish players that are really sneaking from players that are set sneaking by a plugin. 
     * @param player + ")"
     * @param sneaking
     */
    public void setReallySneaking(final Player player, final boolean sneaking) {
        if (sneaking) reallySneaking.add(player.getName());
        else reallySneaking.remove(player.getName());
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
    private void outputDebug(final Player player, final PlayerLocation to, 
            final MovingData data, final MovingConfig cc, 
            final double hDistance, final double hAllowedDistance, final double hFreedom, 
            final double yDistance, final double vAllowedDistance,
            final boolean fromOnGround, final boolean resetFrom, 
            final boolean toOnGround, final boolean resetTo,
            final PlayerMoveData thisMove) {
        // TODO: Show player name once (!)
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final StringBuilder builder = new StringBuilder(500);
        builder.append(CheckUtils.getLogMessagePrefix(player, type));
        final String hBuf = (data.sfHorizontalBuffer < 1.0 ? ((" hbuf=" + StringUtil.fdec3.format(data.sfHorizontalBuffer))) : "");
        final String lostSprint = (data.lostSprintCount > 0 ? (" lostSprint=" + data.lostSprintCount) : "");
        final String hVelUsed = hFreedom > 0 ? " hVelUsed=" + StringUtil.fdec3.format(hFreedom) : "";
        builder.append("\nonground: " + (thisMove.headObstructed ? "(head obstr.) " : "") + (thisMove.touchedGroundWorkaround ? "(touched ground) " : "") + (fromOnGround ? "onground -> " : (resetFrom ? "resetcond -> " : "--- -> ")) + (toOnGround ? "onground" : (resetTo ? "resetcond" : "---")) + ", jumpphase: " + data.sfJumpPhase + ", liftoff: " + data.liftOffEnvelope.name() + "(" + data.insideMediumCount + ")");
        final String dHDist = lastMove.toIsValid ? " (" + StringUtil.formatDiff(hDistance, lastMove.hDistance) + ")" : "";
        final String dYDist = lastMove.toIsValid ? " (" + StringUtil.formatDiff(yDistance, lastMove.yDistance)+ ")" : "";
        builder.append("\n" + " hDist: " + StringUtil.fdec3.format(hDistance) + dHDist + " / " +  StringUtil.fdec3.format(hAllowedDistance) + hBuf + lostSprint + hVelUsed + " , vDist: " + StringUtil.fdec3.format(yDistance) + dYDist + " / " + StringUtil.fdec3.format(vAllowedDistance) + " , sby=" + (data.hasSetBack() ? (data.getSetBackY() + " (" + StringUtil.fdec3.format(to.getY() - data.getSetBackY()) + " / " + data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier) + ")") : "?"));
        if (lastMove.toIsValid) {
            builder.append(" , fdsq: " + StringUtil.fdec3.format(thisMove.distanceSquared / lastMove.distanceSquared));
        }
        if (thisMove.verVelUsed != null) {
            builder.append(" , vVelUsed: " + thisMove.verVelUsed + " ");
        }
        data.addVerticalVelocity(builder);
        //		if (data.horizontalVelocityCounter > 0 || data.horizontalFreedom >= 0.001) {
        //			builder.append("\n" + player.getName() + " horizontal freedom: " +  StringUtil.fdec3.format(data.horizontalFreedom) + " (counter=" + data.horizontalVelocityCounter +"/used="+data.horizontalVelocityUsed);
        //		}
        data.addHorizontalVelocity(builder);
        if (!resetFrom && !resetTo) {
            if (cc.survivalFlyAccountingV && data.vDistAcc.count() > data.vDistAcc.bucketCapacity()) {
                builder.append("\n" + " vacc: " + data.vDistAcc.toInformalString());
            }
        }
        if (data.combinedMediumHCount > 0) {
            // TODO: if hacc activated:
            builder.append("\n hacc: " + StringUtil.fdec3.format(data.combinedMediumHValue / (double) data.combinedMediumHCount) + "(" + data.combinedMediumHCount + ")");
        }
        if (player.isSleeping()) {
            tags.add("sleeping");
        }
        if (player.getFoodLevel() <= 5 && player.isSprinting()) {
            // Exception: does not take into account latency.
            tags.add("lowfoodsprint");
        }
        if (Bridge1_9.isWearingElytra(player)) {
            // Just wearing (not isGliding).
            tags.add("elytra_off");
        }
        if (!tags.isEmpty()) {
            builder.append("\n" + " tags: " + StringUtil.join(tags, "+"));
        }
        if (!justUsedWorkarounds.isEmpty()) {
            builder.append("\n" + " workarounds: " + StringUtil.join(justUsedWorkarounds, "+"));
        }
        builder.append("\n");
        //		builder.append(data.stats.getStatsStr(false));
        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
    }

    private void logPostViolationTags(final Player player) {
        debug(player, "SurvivalFly Post violation handling tag update:\n" + StringUtil.join(tags, "+"));
    }

}
