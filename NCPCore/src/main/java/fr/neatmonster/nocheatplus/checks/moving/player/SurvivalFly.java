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

import fr.neatmonster.nocheatplus.NCPAPIProvider;
import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.ViolationData;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.magic.LostGround;
import fr.neatmonster.nocheatplus.checks.moving.magic.Magic;
import fr.neatmonster.nocheatplus.checks.moving.magic.InAirVerticalRules;
import fr.neatmonster.nocheatplus.checks.moving.magic.VerticalLiquidRules;
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
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
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
    // Server versions ugly but gets the job done...
    private final boolean ServerIsAtLeast1_9 = ServerVersion.compareMinecraftVersion("1.9") >= 0;
    private final boolean ServerIsAtLeast1_10 = ServerVersion.compareMinecraftVersion("1.10") >= 0;
    private final boolean ServerIsAtLeast1_13 = ServerVersion.compareMinecraftVersion("1.13") >= 0;
    private final boolean ServerIsAtLeast1_16 = ServerVersion.compareMinecraftVersion("1.16") >= 0;
    /** Maximum hop delay. */
    private static final int bunnyHopMax = 10;
    /** Divisor vs. last hDist for minimum slow down. */
    private static final double bunnyDivFriction = 160.0; // Rather in-air, blocks would differ by friction.
    private boolean snowFix;
    /** Flag to indicate whether the buffer should be used for this move (only work inside setAllowedhDist). */
    private boolean bufferUse;
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
    //private final Plugin plugin = Bukkit.getPluginManager().getPlugin("NoCheatPlus");

    /**
     * Some note for mcbe compatibility:
     * - New step pattern 0.42-0.58-0.001 ?
     * - Maximum step height 0.75 ?
     * - Ladder descends speed 0.2
     * - Jump on grass_path blocks will result in jump height 0.42 + 0.0625
     *   but next move friction still base on 0.42 ( not sure this does happen
     *   on others )
     * - honey block: yDistance < -0.118 && yDistance > -0.128 ?
     */

    /**
     * Instantiates a new survival fly check.
     */
    public SurvivalFly() {
        super(CheckType.MOVING_SURVIVALFLY);
        blockChangeTracker = NCPAPIProvider.getNoCheatPlusAPI().getBlockChangeTracker();
    }


    /**
     * Checks a player
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
    public Location check(final Player player, final PlayerLocation from, final PlayerLocation to, 
                          final int multiMoveCount, 
                          final MovingData data, final MovingConfig cc, final IPlayerData pData,
                          final int tick, final long now, final boolean useBlockChangeTracker) {

        tags.clear();
        final boolean debug = pData.isDebugActive(type);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final boolean isSamePos = from.isSamePos(to);

        if (debug) {
            justUsedWorkarounds.clear();
            data.ws.setJustUsedIds(justUsedWorkarounds);
        }

        // Calculate some distances.
        final double xDistance, yDistance, zDistance, hDistance;
        final boolean HasHorizontalDistance;
        if (isSamePos) {
            // TODO: Could run a completely different check here (roughly none :p).
            xDistance = yDistance = zDistance = hDistance = 0.0;
            HasHorizontalDistance = false;
        }
        else {
            xDistance = to.getX() - from.getX();
            yDistance = thisMove.yDistance;
            zDistance = to.getZ() - from.getZ();
            if (xDistance == 0.0 && zDistance == 0.0) {
                hDistance = 0.0;
                HasHorizontalDistance = false;
            }
            else {
                HasHorizontalDistance = true;
                hDistance = thisMove.hDistance;
            }
        }

        // Recover from data removal (somewhat random insertion point).
        if (data.liftOffEnvelope == LiftOffEnvelope.UNKNOWN) {
            data.adjustMediumProperties(from);
        }

        // Shortcuts:
        final boolean fromOnGround = thisMove.from.onGround;
        final boolean toOnGround = thisMove.to.onGround || useBlockChangeTracker && toOnGroundPastStates(from, to, thisMove, tick, data, cc);  // TODO: Work in the past ground stuff differently (thisMove, touchedGround?, from/to ...)
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
                else sprinting = false;
            }
            else {
                tags.add("lostsprint");
                sprinting = true;
                if (data.lostSprintCount < 3 && toOnGround || to.isResetCond()) {
                    data.lostSprintCount = 0;
                }
                else data.lostSprintCount --;
            }
        }
        else if (now <= data.timeSprinting + cc.sprintingGrace) {
            // Within grace period for hunger level being too low for sprinting on server side (latency).
            if (now != data.timeSprinting) {
                tags.add("sprintgrace");
            }
            sprinting = true;
        }
        else sprinting = false;

        // Use the player-specific walk speed.
        // TODO: Might get from listener.
        // TODO: Use in lostground?
        thisMove.walkSpeed = Magic.WALK_SPEED * ((double) data.walkSpeed / Magic.DEFAULT_WALKSPEED);
        setNextFriction(thisMove, data, cc);







        /////////////////////////////////
        // Mixed checks (lost ground).
        /////////////////////////////////

        final boolean resetFrom;
        if (fromOnGround || from.isResetCond()) resetFrom = true;

        // TODO: Extra workarounds for toOnGround (step-up is a case with to on ground)?
        // TODO: This isn't correct, needs redesign.
        // TODO: Quick addition. Reconsider entry points etc.
        else if (isSamePos) {

            if (useBlockChangeTracker && from.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, data.blockChangeRef, tick)) {
                resetFrom = true;
                tags.add("pastground_from");
            }
            else if (lastMove.toIsValid) {
                // Note that to is not on ground either.
                resetFrom = LostGround.lostGroundStill(player, from, to, hDistance, yDistance, sprinting, lastMove, data, cc, tags);
            }
            else resetFrom = false;
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

        if (thisMove.touchedGround) {
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
        thisMove.headObstructed = (yDistance > 0.0 ? from.isHeadObstructed(yDistance) : from.isHeadObstructed());

        // HACK: Force sfNoLowJump by a flag.
        // TODO: Might remove that flag, as the issue for trying this has been resolved differently (F_HEIGHT8_1).
        // TODO: Consider setting on ground_height always?
        // TODO: Specialize - test for foot region?
        if ((from.getBlockFlags() & BlockProperties.F_ALLOW_LOWJUMP) != 0) {
            data.sfNoLowJump = true;
        }

        // Moving half on farmland(or end_potal_frame) and half on water
        data.isHalfGroundHalfWater = (from.getBlockFlags() & BlockProperties.F_MIN_HEIGHT16_15) != 0 && from.isInWater()
                                     && !BlockProperties.isLiquid(from.getTypeId(from.getBlockX(), Location.locToBlock(from.getY() + 0.3), 
                                     from.getBlockZ()));

        snowFix = (from.getBlockFlags() & BlockProperties.F_HEIGHT_8_INC) != 0;
        bufferUse = true;






        //////////////////////
        // Horizontal move.
        //////////////////////

        // TODO: Account for lift-off medium / if in air [i.e. account for medium + friction]?
        // Alter some data / flags.
        data.bunnyhopDelay--; // TODO: Design to do the changing at the bottom? [if change: check limits in bunnyHop(...)]
        data.lastbunnyhopDelay -= data.lastbunnyhopDelay > 0 ? 1 : 0;
        thisMove.downStream = from.isDownStream(xDistance, zDistance); // Set flag for swimming with the flowing direction of liquid.
        double hAllowedDistance = 0.0, hDistanceAboveLimit = 0.0, hFreedom = 0.0;

        // Pass downstream for later uses
        if (!data.isdownstream) data.isdownstream = thisMove.downStream;
        else if (from.isOnGround() && !from.isInLiquid()) data.isdownstream = false;

        // Jump with head obstructed and trap door on ice
        // Actually no way to detect they are about to jump!
        if (thisMove.headObstructed && from.isOnIce() && (from.getBlockFlags() & BlockProperties.F_ATTACHED_LOW2_SNEW) != 0) {
            data.sfOnIce = 24;
            data.bunnyhopTick = 4;
        }

        // Ice jump
        if ((thisMove.from.onIce && !thisMove.to.onIce && !data.sfLowJump) 
           // Jump with head obstructed
           || (thisMove.headObstructed && thisMove.yDistance > 0.01 && lastMove.from.onIce)
           ) {
            data.sfOnIce = 20;
        }
        else if (data.sfOnIce > 0) data.sfOnIce--;

        // Run through all hDistance checks if the player has actually some horizontal distance
        if (HasHorizontalDistance) {

            final double attrMod = attributeAccess.getHandle().getSpeedAttributeMultiplier(player);
            // Set the allowed distance and determine the distance above limit
            hAllowedDistance = setAllowedhDist(player, sprinting, thisMove, data, cc, pData, from, true);
            hDistanceAboveLimit = hDistance - hAllowedDistance;

            // Ugly temporary workaround for riptiding.
            if ((Bridge1_13.isRiptiding(player) || data.timeRiptiding + 4000 > now) && hDistanceAboveLimit < 3.0) {
                hDistanceAboveLimit = 0.0;
            }

            // The player went beyond the allowed limit, check if there might have been a reason for this
            // TODO: Move more of the workarounds (buffer, bunny, ...) into ths method.
            if (hDistanceAboveLimit > 0) {
                final double[] resultH = hDistAfterFailure(player, from, to, hAllowedDistance, hDistanceAboveLimit, 
                                                           sprinting, thisMove, lastMove, data, cc, pData, false);
                hAllowedDistance = resultH[0];
                hDistanceAboveLimit = resultH[1];
                hFreedom = resultH[2];
            }
            else {
                data.clearActiveHorVel();
                hFreedom = 0.0;
                if (resetFrom && data.bunnyhopDelay <= 6) data.bunnyhopDelay = 0;
            }

            // The hacc subcheck (if enabled, always update)
            if (cc.survivalFlyAccountingH) {
                hDistanceAboveLimit = horizontalAccounting(data, hDistance, hDistanceAboveLimit, thisMove);
            }

            // Prevent players from walking on a liquid in a too simple way.
            if (!pData.hasPermission(Permissions.MOVING_SURVIVALFLY_WATERWALK, player)) {
                hDistanceAboveLimit = waterWalkChecks(data, player, hDistance, yDistance, thisMove, lastMove, fromOnGround, hDistanceAboveLimit,
                                                      toOnGround, from, to);
            }
            
            // Prevent players from illegally sprinting.
            //if (!pData.hasPermission(Permissions.MOVING_SURVIVALFLY_SPRINTING, player)){
            //    hDistanceAboveLimit = sprintingChecks(sprinting, data, player, hDistance, hDistanceAboveLimit, thisMove,
            //                                          xDistance, zDistance, from);
            //}

            // Decrease bhop tick after checking
            if (data.bunnyhopTick > 0) {
                data.bunnyhopTick-- ;
            }

            // Count down for the soul speed enchant motion
            if (data.keepfrictiontick > 0) {
                data.keepfrictiontick-- ;
            }

            // A special(model) move from CreativeFly has been turned to a normal move again, count up for the incoming motion
            if (data.keepfrictiontick < 0) {
                data.keepfrictiontick++ ;
            }

        }
        // No horizontal distance present
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

        double vAllowedDistance = 0, vDistanceAboveLimit = 0;
        
        // Wild-card allow step height from ground to ground.
        if (yDistance >= 0.0 && yDistance <= cc.sfStepHeight && toOnGround && fromOnGround && !from.isOnClimbable()) {
            vAllowedDistance = cc.sfStepHeight;
            thisMove.allowstep = true;
        }

        // HoneyBlock
        else if ((BlockProperties.getBlockFlags(from.getTypeId()) & BlockProperties.F_STICKY) != 0) {
            final Double Amplifier = PotionUtil.getPotionEffectAmplifier(from.getPlayer(), PotionEffectType.JUMP);
            vAllowedDistance = 0.21 * (Double.isInfinite(Amplifier) ? 1.0 : 1.0 + 0.48  * (Amplifier + 1));
            if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) vAllowedDistance = 1.5;
            vDistanceAboveLimit = thisMove.yDistance - vAllowedDistance;
        }

        // Webs 
        else if (from.isInWeb()) {
            final double[] resultWeb = vDistWeb(player, thisMove, toOnGround, hDistanceAboveLimit, now, data, cc, from);
            vAllowedDistance = resultWeb[0];
            vDistanceAboveLimit = resultWeb[1];
        }
        
        // Berry bush
        // TODO: One more move outside block still affect, need add vdistrel workarounds
        else if (from.isInBerryBush()){
            final double[] resultBush = vDistBush(player, thisMove, toOnGround, hDistanceAboveLimit, now, data, cc, from, fromOnGround);
            vAllowedDistance = resultBush[0];
            vDistanceAboveLimit = resultBush[1];
        }

        // Climbable blocks
        else if (from.isOnClimbable()) {
            vDistanceAboveLimit = vDistClimbable(player, from, to, fromOnGround, toOnGround, thisMove, lastMove, yDistance, data);
        }

        // In liquid
        else if (thisMove.from.inLiquid) { 
            final double[] resultLiquid = vDistLiquid(thisMove, from, to, toOnGround, yDistance, lastMove, data, player);
            vAllowedDistance = resultLiquid[0];
            vDistanceAboveLimit = resultLiquid[1];

            // The frition jump phase has to be set externally.
            if (vDistanceAboveLimit <= 0.0 && yDistance > 0.0 && Math.abs(yDistance) > Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player))) {
                data.setFrictionJumpPhase();
            }
        }

        // Fallback to in-air checks
        else {
            final double[] resultAir = vDistAir(now, player, from, fromOnGround, resetFrom, 
                                               to, toOnGround, resetTo, hDistanceAboveLimit, yDistance, 
                                               multiMoveCount, lastMove, data, cc, pData);
            vAllowedDistance = resultAir[0];
            vDistanceAboveLimit = resultAir[1];
        }

        // Post-check recovery.
        // TODO: Better place for checking for moved blocks [redesign for intermediate result objects?].
        // Vertical push/pull.
        if (useBlockChangeTracker && vDistanceAboveLimit > 0.0) {
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
                        yDistance, vAllowedDistance, fromOnGround, resetFrom, toOnGround, 
                        resetTo, thisMove);
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

            final Location vLoc = handleViolation(now, Double.isInfinite(result) ? 30.0 : result, player, from, to, data, cc);
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
                && data.sfJumpPhase <= 0 && !thisMove.from.inLiquid) {
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
            else data.sfZeroVdistRepeat = 0;
        }
        else {
            data.sfZeroVdistRepeat = 0;
            data.ws.resetConditions(WRPT.G_RESET_NOTINAIR);
            data.sfVLInAir = false;
        }

        // Horizontal velocity invalidation.
        if (hDistance <= (cc.velocityStrictInvalidation ? thisMove.hAllowedDistanceBase : thisMove.hAllowedDistanceBase / 2.0)) {
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
    





   /**
    * The horizontal accounting subcheck, it monitors average combined-medium (e.g. air+ground or air+water) speed, 
    * with a rather simple bucket(s)-overflow mechanism.
    * We feed 1.0 whenever we're below the allowed BASE speed, and (actual / base) if we're above. 
    *
    * (hAllowedDistanceBase is about what a player can run at without using special techniques like extra jumping, 
    * not necessarily the finally allowed speed).
    * 
    * @return hDistanceAboveLimit
    */
    private double horizontalAccounting(final MovingData data, double hDistance, double hDistanceAboveLimit, final PlayerMoveData thisMove){
        
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
                // 1.8.8 in-water moves with jumping near/on surface. 1.2 is max factor for one move (!).
                limitFCMH =  ServerIsAtLeast1_10 ? 1.05 : 1.1; 
            }
            else {
                limitFCMH = 1.0;
            }
            // TODO: Configurable / adjust by medium type.
            // TODO: Instead of velocityJumpPhase account for friction directly?
            // TODO: Fly-NoFly + bunny-water transitions pose issues.
            if (fcmh > limitFCMH && !data.isVelocityJumpPhase()) {
                hDistanceAboveLimit = Math.max(hDistanceAboveLimit, (hDistance * (fcmh - limitFCMH)));
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
        return hDistanceAboveLimit;
    }
    

   /**
    * Directly checks for cheat implementation types that aim at walking on liquids
    *
    * @return hDistanceAboveLimit
    *
    */
    private double waterWalkChecks(final MovingData data, final Player player, double hDistance, double yDistance, 
                                   final PlayerMoveData thisMove, final PlayerMoveData lastMove,
                                   final boolean fromOnGround, double hDistanceAboveLimit,
                                   final boolean toOnGround, final PlayerLocation from, final PlayerLocation to){

        Material blockUnder = from.getTypeId(from.getBlockX(), Location.locToBlock(from.getY() - 0.3), from.getBlockZ());
        Material blockAbove = from.getTypeId(from.getBlockX(), Location.locToBlock(from.getY() + 0.1), from.getBlockZ());

        // Checks for 0 y deltas when on/in water
        if (hDistanceAboveLimit <= 0D && hDistance > 0.1D && yDistance == 0D && lastMove.toIsValid && lastMove.yDistance == 0D 
            && BlockProperties.isLiquid(to.getTypeId()) 
            && BlockProperties.isLiquid(from.getTypeId())
            && !toOnGround && !fromOnGround
            && !from.isHeadObstructed() && !to.isHeadObstructed() 
            && !Bridge1_13.isSwimming(player)
            ) {
            hDistanceAboveLimit = Math.max(hDistanceAboveLimit, hDistance);
            tags.add("liquidwalk");
        }

        // Checks for micro y deltas when moving above liquid.
        if (blockUnder != null && BlockProperties.isLiquid(blockUnder) && BlockProperties.isAir(blockAbove)) {
            
            if (!data.isHalfGroundHalfWater && hDistanceAboveLimit <= 0D && hDistance > 0.11D && yDistance <= 0.1D 
                && !toOnGround && !fromOnGround
                && lastMove.toIsValid && lastMove.yDistance == yDistance 
                || lastMove.yDistance == yDistance * -1 && lastMove.yDistance != 0D
                && !from.isHeadObstructed() && !to.isHeadObstructed() 
                && !Bridge1_13.isSwimming(player)
                ) {

                // Prevent being flagged if a player transitions from a block to water and the player falls into the water.
                if (!(yDistance < 0.0 && yDistance != 0.0 && lastMove.yDistance < 0.0 && lastMove.yDistance != 0.0)) {
                    hDistanceAboveLimit = Math.max(hDistanceAboveLimit, hDistance);
                    tags.add("liquidmove");
                }
            }
        }
        return hDistanceAboveLimit;
    }

   
    ///**
    //* Checks for illegal sprinting modifications, such as sprinting backwards, sideways and on impossible conditions
    //* 
    // * @param sprinting
    //* @param hDistance
    //* @param hDistanceAboveLimit
    //* @return hDistanceAboveLimit
    //*/
    //private double sprintingChecks(final boolean sprinting, final MovingData data, final Player player,
    //                               double hDistance, double hDistanceAboveLimit, final PlayerMoveData thisMove,
    //                               double xDistance, double zDistance, final PlayerLocation from){
    //
    //    // TODO: Add sideways sprinting module (rather important for PVP)
    //    // TODO: Move the lowfoodsprint check here?
    //
    //    // Note: Using NCP's sprinting mechanics here could yield false positives, due to the current assumeSprint workaround which assumes players to be
    //    // sprinting all the time (when possible), even if they are just walking. 
    //    // Potential fixes: 
    //    // 1) Review if the workaround is still nedeed. (-> Seems like it's crucial for PVP since players will still get setbacked without this...);
    //    // 2) Confine assumeSprint more (-> How?);
    //    // 3) Simply use player#isSprinting() (But events could be missing, out of order or not sent for whatever reason, in a lostGround fashion...).
    //    
    //    // Vanilla MC disallows players from sprinting with blindness
    //    //if (player.isSprinting() && hDistance > thisMove.walkSpeed && player.hasPotionEffect(PotionEffectType.BLINDNESS)
    //    //    && data.lostSprintCount == 0 && !data.isVelocityJumpPhase()) {
    //    //    hDistanceAboveLimit = Math.max(hDistanceAboveLimit, (hDistance - thisMove.walkSpeed)); // Allow players to walk at walking pace, rather than invalidating all hDist
    //    //    tags.add("blindsprint");
    //    //    bufferUse = false;
    //    //}
    //    
    //    // Prevent players from sprinting backwards
    //    //if (player.isSprinting() && hDistance > thisMove.walkSpeed && data.lostSprintCount == 0 && !data.isVelocityJumpPhase()){
    //    //    if (TrigUtil.isMovingBackwards(xDistance, zDistance, LocUtil.correctYaw(from.getYaw()))){
    //    //        hDistanceAboveLimit = Math.max(hDistanceAboveLimit, (hDistance - thisMove.walkSpeed)); // Allow players to walk at walking pace, rather than invalidating all hDist
    //    //        tags.add("backsprint");
    //    //        bufferUse = false; // Mnh, too harsh?
    //    //    }
    //    //}
    //    return hDistanceAboveLimit;
    //}
    


   /**
    * Check for toOnGround past states
    * 
    * @param from
    * @param to
    * @param thisMove
    * @param tick
    * @param data
    * @param cc
    * @return
    */
    private boolean toOnGroundPastStates(final PlayerLocation from, final PlayerLocation to, 
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
    private void setNextFriction(final PlayerMoveData thisMove, final MovingData data, final MovingConfig cc) {

        // NOTE: Other methods might still override nextFriction to 1.0 due to burst/lift-off envelope.
        // TODO: Other media / medium transitions / friction by block.
        final LocationData from = thisMove.from;
        final LocationData to = thisMove.to;

        if (from.inWeb || to.inWeb) {
            data.nextFrictionHorizontal = data.nextFrictionVertical = 0.0;
        }
        // No from#onClimbable check to fix vines fps casue by medium counts, probably wrong place! 
        else if (to.onClimbable) {
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
                                   final PlayerMoveData thisMove, final MovingData data,
                                   final MovingConfig cc, final IPlayerData pData, final PlayerLocation from,
                                   final boolean checkPermissions) {

        // TODO: Optimize for double checking?
        // TODO: sfDirty: Better friction/envelope-based.
        final boolean isMovingBackwards   = TrigUtil.isMovingBackwards(thisMove.to.getX()-thisMove.from.getX(), thisMove.to.getZ()-thisMove.from.getZ(), LocUtil.correctYaw(from.getYaw()));  
        final boolean actuallySneaking    = player.isSneaking() && reallySneaking.contains(player.getName());
        final boolean isBlockingOrUsing   = data.isusingitem || player.isBlocking();
        final PlayerMoveData lastMove     = data.playerMoves.getFirstPastMove();
        final long now                    = System.currentTimeMillis(); 
        final double modHoneyBlock        = Magic.modSoulSand * (thisMove.to.onGround ? 0.8 : 1.75);
        final double modStairs            = isMovingBackwards ? 1.0 : thisMove.yDistance == 0.5 ? 1.85 : 1.325;
        final double modHopSprint         = (data.bunnyhopTick < 3 ? 1.15 : Magic.modSprint);
        final double webJumpAccell        = (thisMove.yDistance > 0.0 ? 0.26 : 0.0);
        final boolean sfDirty             = data.isVelocityJumpPhase(); 
        double hAllowedDistance           = 0D;
        double friction                   = data.lastFrictionHorizontal; // Friction to use with this move.
        boolean useBaseModifiers          = false;
        boolean useBaseModifiersSprint    = true;
        boolean useBlockAndSneakModifier  = false;
        boolean useBlockOrSneakModifier   = false;

  
        // Preliminary resets
        if (data.noslowhop != 0 && (sfDirty || (!data.isusingitem && !player.isBlocking()))) data.noslowhop = 0;
        if (!data.liftOffEnvelope.name().startsWith("LIMIT") || sfDirty) data.watermovect = 0;


        /////////////////////////////////////////////////////////////
        // Set the allowed horizontal distance according to medium //
        /////////////////////////////////////////////////////////////
        // Webs
        if (thisMove.from.inWeb) {
            tags.add("hweb");
            data.sfOnIce = 0;
            hAllowedDistance = Magic.modWeb * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            // Walking through webs with slime/ice underneath slows down even more.
            // TODO: Accurate Magic.(get rid of the accell thingy)
            if (thisMove.from.onSlimeBlock || thisMove.from.onIce) hAllowedDistance *= (Magic.modSlime - Magic.modWeb) + webJumpAccell; 
            useBaseModifiersSprint = false; 
            useBaseModifiers = true;
            friction = 0.0; 
        }
        
        // Soulsand
        else if (thisMove.from.onSoulSand) {
            tags.add("hsoulsand");
            final boolean hasEnchant = BridgeEnchant.hasSoulSpeed(player);
            final boolean inSoulBlock = (BlockProperties.getBlockFlags(from.getTypeId()) & BlockProperties.F_SOULSAND) != 0;
            hAllowedDistance = (inSoulBlock ? Magic.modSoulSand : (sprinting ? Magic.modSprint : 1.0)) * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            if (hasEnchant && inSoulBlock) hAllowedDistance *= Magic.modSoulSpeed;
            if (hasEnchant) data.keepfrictiontick = 60;
            useBlockOrSneakModifier = true;
            useBlockAndSneakModifier = true; // (OK)
            useBaseModifiers = true;
            friction = 0.0;
        }
        
        // Slimeblock
        else if (thisMove.from.onSlimeBlock) {
            tags.add("hslimeblock");
            hAllowedDistance = Magic.modSlime * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            useBlockOrSneakModifier = true; 
            useBaseModifiers = true;
            useBaseModifiersSprint = false;
            // Friction is used as is.
        }
        
        // Berry bush
        else if (thisMove.from.inBerryBush) {
            tags.add("hbush");
            hAllowedDistance = Magic.modBush * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            if (thisMove.to.inBerryBush) hAllowedDistance *= 0.8;
            if (thisMove.yDistance > 0.0 && thisMove.from.onGround && !thisMove.to.onGround) hAllowedDistance *= 2.0;
            useBlockOrSneakModifier = true;
            useBaseModifiers = true;
            friction = 0.0;
            
            // Multiprotocol plugins: allow normal walking.
            if ((from.getBlockFlags() & BlockProperties.F_ALLOW_LOWJUMP) != 0) {
               hAllowedDistance = thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            }
        }

        // Honeyblock
        else if (thisMove.from.onHoneyBlock) {
            tags.add("hhoneyblock");
            hAllowedDistance = modHoneyBlock * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            useBlockOrSneakModifier = true;
            useBaseModifiers = true;
            friction = 0.0; 
        }

        // Stairs
        else if (from.isAboveStairs()) {
            tags.add("hstairs");
            useBaseModifiers = true;
            hAllowedDistance = modStairs * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            friction = 0.0;
            if (!Double.isInfinite(mcAccess.getHandle().getFasterMovementAmplifier(player))) hAllowedDistance *= 0.88;
        }

        // NoSlow
        else if (data.isHackingRI && !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_BLOCKING, player)) {
            tags.add("usingitem(cancel)");
            data.isHackingRI = false;
            hAllowedDistance = 0.0;
            friction = 0.0;
            useBaseModifiers = false;
        }

        // Collision with entities (1.9+)
        else if (ServerIsAtLeast1_9 && CollisionUtil.isCollidingWithEntities(player, true) && hAllowedDistance < 0.35) {
            tags.add("hcollision");
            hAllowedDistance = Magic.modCollision * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            useBaseModifiers = true;
            data.bunnyhopTick = 20;
            friction = 0.0;
        }

        // Riptiding
        // TODO: Move to Creativefly
        else if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
            tags.add("hriptide");
            hAllowedDistance = Magic.modRiptide[data.RiptideLevel] * thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
        }

        // In liquid
        // Check all liquids (lava might demand even slower speed though).
        // TODO: Test how to go with only checking from (less dolphins).
        // TODO: TEST: Isn't walkSpeed static here? /walspeed shouldn't work in liquids...
        else if (thisMove.from.inLiquid && thisMove.to.inLiquid && !data.isHalfGroundHalfWater) {
            tags.add("hliquid");
            hAllowedDistance = Bridge1_13.isSwimming(player) ? Magic.modSwim[1] : Magic.modSwim[0] * thisMove.walkSpeed * cc.survivalFlySwimmingSpeed / 100D;
            useBaseModifiers = false;
            useBlockOrSneakModifier = true; 
            useBlockAndSneakModifier = true; // (OK)
            if (sfDirty) friction = 0.0;
            
            // Account for all water-related enchants
            if (thisMove.from.inWater || !thisMove.from.inLava) { 
                final int level = BridgeEnchant.getDepthStriderLevel(player);
                if (level > 0) {
                    // Speed effect, attribute will affect to water movement whenever you has DepthStrider enchant.
                    useBaseModifiers = true;
                    useBaseModifiersSprint = true;
                    hAllowedDistance *= Magic.modDepthStrider[level];
                    // Modifiers: Most speed seems to be reached on ground, but couldn't nail down.
                }

                if (!Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(player))) {
                    // TODO: Allow for faster swimming above water with Dolhphins Grace
                    hAllowedDistance *= Magic.modDolphinsGrace;
                    if (level > 1) {
                        hAllowedDistance *= 1.0 + 0.07 * level;
                    }
                }

                if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
                   hAllowedDistance *= Magic.modRiptide[data.RiptideLevel];
                }

                if (data.liqtick < 5 && lastMove.toIsValid) {
                    if (!lastMove.from.inLiquid) {
                        if (lastMove.hDistance * 0.92 > thisMove.hDistance) {
                            hAllowedDistance = lastMove.hDistance * 0.92;
                        }
                    } 
                    else if (lastMove.hAllowedDistance * 0.92 > thisMove.hDistance) {
                        hAllowedDistance = lastMove.hAllowedDistance * 0.92;
                    }
                }
            }
        }


        // Speed limit for players moving above surface
        // TODO: Still check with velocity?
        else if (!data.isHalfGroundHalfWater && !sfDirty && !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_WATERWALK, player) 
                && ((thisMove.from.inLiquid && !thisMove.to.inLiquid) || data.watermovect == 1) 
                && data.liftOffEnvelope.name().startsWith("LIMIT")
                ) {
            tags.add("hsurface");
            hAllowedDistance = Bridge1_13.isSwimming(player) ? Magic.modSwim[1] : Magic.modSwim[0] * thisMove.walkSpeed * Magic.modSurface[0] * cc.survivalFlySwimmingSpeed / 100D;
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
                if (level > 1) {
                    hAllowedDistance *= 1.0 + 0.07 * level;
                }
            }

            if (data.watermovect == 1) hAllowedDistance *= Magic.modSurface[1];
            data.watermovect = 1;
            final int blockData = from.getData(from.getBlockX(), from.getBlockY(), from.getBlockZ());
            final int blockUnderData = from.getData(from.getBlockX(), from.getBlockY() -1, from.getBlockZ());
            if (blockData > 3 || blockUnderData > 3 || data.isdownstream) {
                data.watermovect = 0;
                hAllowedDistance = thisMove.walkSpeed * cc.survivalFlySwimmingSpeed / 100D;
                data.isdownstream = false;
            }
        }

        // Sneaking
        // TODO: !sfDirty is very coarse, should use friction instead.
        // TODO: Attribute modifiers can count in here, e.g. +0.5 (+ 50% doesn't seem to pose a problem, neither speed effect 2).
        else if (!sfDirty && thisMove.from.onGround && actuallySneaking 
                && (!checkPermissions || !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_SNEAKING, player))
                && cc.sfCheckNoSlow) {
            tags.add("sneaking");
            hAllowedDistance = Magic.modSneak * thisMove.walkSpeed * cc.survivalFlySneakingSpeed / 100D;
            useBaseModifiers = true;
            if (isBlockingOrUsing) hAllowedDistance *= Magic.modBlock;
            friction = 0.0; // Ensure friction can't be used to speed.

            if (!Double.isInfinite(mcAccess.getHandle().getFasterMovementAmplifier(player))) {
                hAllowedDistance *= 0.88;
                useBaseModifiersSprint = true;
            }
        }


        // Using items
        // TODO: !sfDirty is very coarse, should use friction instead.
        else if (!sfDirty && (data.isusingitem || player.isBlocking()) 
                && (thisMove.from.onGround || data.noslowhop > 0 || player.isBlocking())
                && (!checkPermissions || !pData.hasPermission(Permissions.MOVING_SURVIVALFLY_BLOCKING, player)) 
                && data.liftOffEnvelope == LiftOffEnvelope.NORMAL && cc.sfCheckNoSlow) {
            tags.add("usingitem");
            if (thisMove.from.onGround) {
                // Jump/left ground
                if (!thisMove.to.onGround) {
                    final double speedAmplifier = mcAccess.getHandle().getFasterMovementAmplifier(player);
                    hAllowedDistance = (lastMove.hDistance > 0.23 ? 0.4 : 0.23 + (ServerIsAtLeast1_13 ? 0.155 : 0.0)) +
                                        0.02 * (Double.isInfinite(speedAmplifier) ? 0 : speedAmplifier + 1.0);
                    hAllowedDistance *= cc.survivalFlyBlockingSpeed / 100D;
                    data.noslowhop = 1;
                }
                // OnGround
                else {
                    // TODO: Need testing
                    if (lastMove.toIsValid && lastMove.hDistance > 0.0)
                       hAllowedDistance = data.noslowhop < 7 ?
                       // 0.6 for old vers, 0.621 for 1.13+
                       (lastMove.hAllowedDistance * (0.63 + 0.052 * ++data.noslowhop)) : lastMove.hAllowedDistance;
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
                // Air friction
                else hAllowedDistance = lastMove.hAllowedDistance * 0.96 * cc.survivalFlyBlockingSpeed / 100D;
            }
            else if (player.isBlocking() && lastMove.toIsValid) {
                // Air friction
                hAllowedDistance = lastMove.hAllowedDistance * 0.96 * cc.survivalFlyBlockingSpeed / 100D;
                // Fake data for air blocking
                data.noslowhop = 2;
            }
            // Check if too small horizontal last move allowed
            // 0.063 for old vers, 0.08 for 1.13+
            hAllowedDistance = Math.max(hAllowedDistance, 0.08);
            friction = 0.0; // Ensure friction can't be used to speed.
            useBaseModifiers = true;
            useBaseModifiersSprint = false;
        }

        // Fallback to Sprinting/Walking
        else {
            useBaseModifiers = true;
            if (sprinting) {
                if (!thisMove.from.onGround && thisMove.to.onGround) {
                    data.bunnyhopTick = ServerIsAtLeast1_13 ? 6 : 3;
                    hAllowedDistance = 1.14 * thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D;
                    tags.add("sprintTo");
                }
                else if (data.bunnyhopTick > 0) {
                    hAllowedDistance = modHopSprint * thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D;
                    if (snowFix && data.bunnyhopTick > 5) hAllowedDistance *= 1.6;
                    tags.add("sprinthop");
                }
                else {
                    hAllowedDistance = thisMove.walkSpeed * cc.survivalFlySprintingSpeed / 100D; 
                    tags.add("sprintwalk");              
                }
            }
            else {
                tags.add("walking");
                hAllowedDistance = thisMove.walkSpeed * cc.survivalFlyWalkingSpeed / 100D;
            }
            // Ensure friction can't be used to speed.
            // TODO: Model bunny hop as a one time peak + friction. Allow medium based friction.
            friction = 0.0;
        }



        /////////////////////////////////////////////////
        // Apply modifiers (sprinting, attributes, ...)//
        /////////////////////////////////////////////////
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
                if (sprinting && hAllowedDistance < 0.29 && cc.sfSlownessSprintHack && 
                    (
                        // TODO: Test/balance thresholds (walkSpeed, attrMod).
                        player.hasPotionEffect(PotionEffectType.SLOW)
                        || data.walkSpeed < Magic.DEFAULT_WALKSPEED
                        || attrMod < 1.0
                    )) {
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
        
        // Player is blocking and sneaking at the same time
        // TODO: Our constants don't match 100%, so blocking and sneaking at the same time slows down too much when on certain blocks (Observed: honeyblock, berry bush, webs)
        // Henche the distinguishing between sneaking AND/OR blocking (Seems OK for water and soulsand)
        // NOTE: Blocking and sneaking on a slime block has the same speed as either blocking or sneaking.
        // TEST: Collision, stairs
        if (useBlockAndSneakModifier && (isBlockingOrUsing && actuallySneaking)) {
        	hAllowedDistance *= Magic.modBlockSneak;
        }
        else if (useBlockOrSneakModifier && (isBlockingOrUsing || actuallySneaking)) {
        	hAllowedDistance *= isBlockingOrUsing ? Magic.modBlock : Magic.modSneak;
        }
        


        ///////////////////////
        // Other properties. //
        ///////////////////////
        // TODO: Reset friction on too big change of direction?

        // Account for flowing liquids (only if needed).
        // Assume: If in liquids this would be placed right here.
        if (thisMove.downStream && thisMove.hDistance > thisMove.walkSpeed * Magic.modSwim[0] && thisMove.from.inLiquid) {
            hAllowedDistance *= Magic.modDownStream;
        }

        // If the player is on ice, give them a higher maximum speed.
        if (data.sfOnIce > 9) {
            tags.add("hice");
            hAllowedDistance *= Magic.modIce;
            if (data.bunnyhopTick > 3) hAllowedDistance *= 1.25;
            else if (data.bunnyhopTick > 0) hAllowedDistance *= 1.1;
        }
        else if (data.sfOnIce > 0) {
            tags.add("hice");
            hAllowedDistance *= 1.0 + 0.025 * data.sfOnIce;
        }
        
        // Soul speed workaround
        if (data.keepfrictiontick > 0) {
            if (!BridgeEnchant.hasSoulSpeed(player)) {
                data.keepfrictiontick = 0;
            } 
            else if (lastMove.toIsValid) {
                hAllowedDistance = Math.max(hAllowedDistance, lastMove.hAllowedDistance * 0.96);
            }
        }

        // Speeding bypass permission (can be combined with other bypasses).
        if (checkPermissions && pData.hasPermission(Permissions.MOVING_SURVIVALFLY_SPEEDING, player)) {
            hAllowedDistance *= cc.survivalFlySpeedingSpeed / 100D;
        }

        // Base speed is set.
        thisMove.hAllowedDistanceBase = hAllowedDistance;

        // Friction mechanics (next move).
        // Move is within lift-off/burst envelope, allow next time.
        // TODO: This probably is the wrong place (+ bunny, + buffer)?
        if (thisMove.hDistance <= hAllowedDistance) {
            data.nextFrictionHorizontal = 1.0;
        }

        // Friction or not (this move).
        // Consider friction.
        // TODO: Invalidation mechanics.
        // TODO: Friction model for high speeds?
        if (lastMove.toIsValid && friction > 0.0) {
            tags.add("hfrict");
            hAllowedDistance = Math.max(hAllowedDistance, lastMove.hDistance * friction);
        }

        thisMove.hAllowedDistance = hAllowedDistance;
        return thisMove.hAllowedDistance;
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


    /**
     * Core y-distance checks for in-air movement (may include air -> other).
     * Also see the InAirVerticalRules to check the general regulation.
     *
     * @return
     */
    private double[] vDistAir(final long now, final Player player, final PlayerLocation from, 
                              final boolean fromOnGround, final boolean resetFrom, final PlayerLocation to, 
                              final boolean toOnGround, final boolean resetTo, 
                              final double hDistance, final double yDistance, 
                              final int multiMoveCount, final PlayerMoveData lastMove, 
                              final MovingData data, final MovingConfig cc, final IPlayerData pData) {
        
        // TODO: Friction might need same treatment as with horizontal (medium transitions: data.lastFrictionVertical).
        // TODO: lostground_pyramid(yDist < 0.0) -> step up (yDist 0.5). Needs better last-move modeling.
        // TODO: lostground_edgedesc(yDist <0.0) -> Bunny (yDist > .72, e_jump). Needs better last-move modeling.
        // TODO: air->ground...small-range-tp...air-air+vDist==0.0 (might work around with fromWasReset?).
        // TODO: bunny after vDist<0.0... vdistsb. Might need set back detection. [solved with setFrictionJumpPhase?]
        // TODO: Other edge cases?
        // TODO: Add/set 'allow starting to fall' first (data reset / from ground on if no speed).
        // TODO: Quick detect valid envelope and move workaround code into a method.
        // TODO: data.noFallAssumeGround  needs more precise flags (refactor to per move data objects, store 123)
        double vAllowedDistance          = 0.0;
        double vDistanceAboveLimit       = 0.0;
        final PlayerMoveData thisMove    = data.playerMoves.getCurrentMove();
        final double yDistChange         = lastMove.toIsValid ? yDistance - lastMove.yDistance : Double.MAX_VALUE; // Change seen from last yDistance.
        final double maxJumpGain         = data.liftOffEnvelope.getMaxJumpGain(data.jumpAmplifier);
        final int maxJumpPhase           = data.liftOffEnvelope.getMaxJumpPhase(data.jumpAmplifier);
        final double jumpGainMargin      = 0.005; // TODO: Model differently, workarounds where needed. 0.05 interferes with max height vs. velocity (<= 0.47 gain).
        final boolean envelopeHack       = InAirVerticalRules.venvHacks(from, to, yDistance, yDistChange, thisMove, lastMove, data) && !resetFrom && !resetTo;
        final boolean strictVdistRel;


        /////////////////////////////////////
        // Determine the allowed yDistance //
        /////////////////////////////////////

        // Less headache: Always allow falling. 
        // TODO: Base should be data.lastFrictionVertical? Problem: "not set" detection?
        if (lastMove.toIsValid && Magic.fallingEnvelope(yDistance, lastMove.yDistance, data.lastFrictionVertical, 0.0)) {
            vAllowedDistance = lastMove.yDistance * data.lastFrictionVertical - Magic.GRAVITY_MIN; // Upper bound.
            strictVdistRel = true;
        }
        else if (resetFrom || thisMove.touchedGroundWorkaround) {

            // TODO: More concise conditions? Some workaround may allow more.
            if (toOnGround) {

                // Hack for boats (coarse: allows minecarts too): allow staying on an entity
                if (yDistance > cc.sfStepHeight && yDistance - cc.sfStepHeight < 0.00000003 && to.isOnGroundDueToStandingOnAnEntity()) {
                    vAllowedDistance = yDistance;
                }
                // Allow jumping height
                else {
                    vAllowedDistance = Math.max(cc.sfStepHeight, maxJumpGain + jumpGainMargin);
                    thisMove.allowstep = true;
                    thisMove.allowjump = true;
                }
            }
            else {

                // Code duplication with the absolute limit below.
                if (yDistance < 0.0 || yDistance > cc.sfStepHeight || !tags.contains("lostground_couldstep")) {
                    vAllowedDistance = maxJumpGain + jumpGainMargin;
                    thisMove.allowjump = true;
                }
                else vAllowedDistance = yDistance;
            }
            strictVdistRel = false;
        }
        else if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
              vAllowedDistance = lastMove.yDistance * 5.0D;
              strictVdistRel = false;
        }
        else if (lastMove.toIsValid) {

            if (lastMove.yDistance >= -Math.max(Magic.GRAVITY_MAX / 2.0, 1.3 * Math.abs(yDistance)) && lastMove.yDistance <= 0.0 
                && (lastMove.touchedGround || lastMove.to.extraPropertiesValid && lastMove.to.resetCond)) {

                if (resetTo) {
                    vAllowedDistance = cc.sfStepHeight;
                    thisMove.allowstep = true;
                }
                else {
                    vAllowedDistance = maxJumpGain + jumpGainMargin;
                    thisMove.allowjump = true;
                } // TODO: Needs more precise confinement + setting set back or distance to ground or estYDist.
                strictVdistRel = false;
            }
            else {
                // Friction.
                // TODO: data.lastFrictionVertical (see above).
                vAllowedDistance = lastMove.yDistance * data.lastFrictionVertical - Magic.GRAVITY_ODD; // Upper bound.
                strictVdistRel = true;
            }
        }
        else {
            // Teleport/join/respawn.
            vAllowedDistance = vAllowedDistanceNoData(thisMove, lastMove, maxJumpGain, jumpGainMargin, data, cc);
            strictVdistRel = false;
            
        }


        ////////////////////////////////////////////////////////////////
        // Compare yDistance to expected, use velocity on violation. //
        ///////////////////////////////////////////////////////////////
        boolean vDistRelVL = false;
        final double yDistDiffEx          = yDistance - vAllowedDistance; 
        final boolean honeyBlockCollision = isCollideWithHB(from, to, data) && yDistance < -0.125 && yDistance > -0.128;
        final boolean gravityEffects      = InAirVerticalRules.oddJunction(from, to, yDistance, yDistChange, yDistDiffEx, maxJumpGain, resetTo, thisMove, lastMove, data, cc);
        final boolean outOfEnvelope       = InAirVerticalRules.outOfEnvelopeExemptions(yDistance, yDistDiffEx, lastMove, data, from, to, now, yDistChange, maxJumpGain, player, thisMove, resetTo);
        final boolean shortMoveExemptions = InAirVerticalRules.shortMoveExemptions(yDistance, yDistDiffEx, lastMove, data, from, to, now, strictVdistRel, maxJumpGain, vAllowedDistance, player, thisMove);
        final boolean fastFallExemptions  = InAirVerticalRules.fastFallExemptions(yDistance, yDistDiffEx, lastMove, data, from, to, now, strictVdistRel, yDistChange, resetTo, fromOnGround, toOnGround, maxJumpGain, player, thisMove, resetFrom);

  
        // Quick invalidation for too much water envelope
        if (!from.isInLiquid() && strictVdistRel && data.liftOffEnvelope == LiftOffEnvelope.LIMIT_LIQUID
            && yDistance > 0.3 && yDistance > vAllowedDistance && data.getOrUseVerticalVelocity(yDistance) == null){
            thisMove.invalidate();
        }


        // Accepted envelopes first
        if (envelopeHack || yDistDiffEx <= 0.0 && yDistDiffEx > -Magic.GRAVITY_SPAN){
            vDistRelVL = false;
        }
        // Upper bound violation: bigger move than expected/allowed
        else if (yDistDiffEx > 0.0) { 
            
            if (yDistance <= 0.0 && (resetTo || thisMove.touchedGround)) {
                // Allow falling shorter than expected, if onto ground.
                // Note resetFrom should usually mean that allowed dist is > 0 ?
                // NOTE: Does not require lastMove.toIsValid
            }
            else if (lastMove.toIsValid && (outOfEnvelope || gravityEffects || honeyBlockCollision || isLanternUpper(to))) {
                // Determine if there could have been a reason for the player to have moved unexepctedly, om match, ignore.
            }
            else vDistRelVL = true;
        } 
        // Upper bound violation: smaller move than expected/allowed (yDistDiffEx <= 0.0)
        else if (yDistance >= 0.0) { 

            if (shortMoveExemptions) {
                // Moved too short, determine if there could be a reason for it.
            }
            else if (lastMove.toIsValid && gravityEffects) {
                // Several types of odd in-air moves, mostly with gravity near maximum, friction, medium change.
            }
            else if (isLanternUpper(to)) {
                // Ignore.
            }
            else vDistRelVL = true;
        }
        // Upper bound violation: too fast fall (yDistance < 0.0)
        else { 

            if (fastFallExemptions) {
                // Falling too fast: Determine if there could have been a reason for the player to have moved unexepctedly, om match, ignore.
            }
            else if (lastMove.toIsValid && gravityEffects) {
               // Several types of odd in-air moves, mostly with gravity near maximum, friction, medium change.
            }
            else if (isLanternUpper(to) || honeyBlockCollision) {
                // Ignore.
            }
            else vDistRelVL = true;
        }

        
        // At this point, a violation
        if (vDistRelVL) {
            if (data.getOrUseVerticalVelocity(yDistance) == null) {
                vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(yDistance - vAllowedDistance));
                tags.add("vdistrel");
            }
        }



        //////////////////////////////////////////////////////////////////////////////
        // Prevent players from moving further than the (absolute) setback distance.//
        //////////////////////////////////////////////////////////////////////////////
        // TODO: Maintain a value in data, adjusting to velocity?
        // TODO: LIMIT_JUMP 
        if (!pData.hasPermission(Permissions.MOVING_SURVIVALFLY_STEP, player) && yDistance > 0.0 
            && !data.isVelocityJumpPhase() && data.hasSetBack()) {
            
            final double vAllowedAbsoluteDistance = data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier);
            final double totalVDistViolation      =  to.getY() - data.getSetBackY() - vAllowedAbsoluteDistance;
            if (totalVDistViolation > 0.0) {

                // Check for exemptions
                if (InAirVerticalRules.vDistSBExemptions(toOnGround, thisMove, lastMove, data, cc, now, player, totalVDistViolation, yDistance, fromOnGround)){
                    // Skip
                }
                else if (yDistance <= cc.sfStepHeight && thisMove.touchedGroundWorkaround && tags.contains("lostground_couldstep")) {
                    // Skip if the player could step up by lostground_couldstep.
                }
                // Attempt to use velocity.
                else if (data.getOrUseVerticalVelocity(yDistance) == null) {
                    vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.max(totalVDistViolation, 0.4));
                    tags.add("vdistsb");
                }
            }
        }


        ////////////////////////////////////////////////////////////////////////////////////
        // Air-stay-time: prevent players from moving further than the maximum jump phase.//
        ////////////////////////////////////////////////////////////////////////////////////
        // TODO: max-phase only when from is not reset !?
        if (!envelopeHack && data.sfJumpPhase > maxJumpPhase && !data.isVelocityJumpPhase()) {
            if (yDistance < 0.5) {
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


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Block 'step' with yDistance between step height and minJumpGain (vdistrel and vdistsb should catch the rest).//
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Model other cases of unexpectedly low 'jumping', such as using too few velocity?
        // (Actual step cheats are probably better detected by generalized patterns.)
        //if (vDistanceAboveLimit <= 0D 
        //    && yDistance > cc.sfStepHeight && yDistance < data.liftOffEnvelope.getMinJumpGain(data.jumpAmplifier) 
        //    && !thisMove.headObstructed && !thisMove.from.resetCond && !thisMove.to.resetCond
        //    && (thisMove.from.onGround || thisMove.touchedGroundWorkaround) && thisMove.to.onGround 
        //    ) {
        //    // Exclude a lost-ground case.
        //    if (thisMove.touchedGroundWorkaround && lastMove.toIsValid && lastMove.yDistance <= 0.0
        //        && yDistance + Math.abs(lastMove.yDistance) <= 2.0 * (maxJumpGain + 0.1)) {
        //        // TODO: Review: still needed?
        //    }
        //    else {
        //        // Potential violation.
        //        if (!pData.hasPermission(Permissions.MOVING_SURVIVALFLY_STEP, player) 
        //                && data.getOrUseVerticalVelocity(yDistance) == null) {
        //            vDistanceAboveLimit = yDistance - cc.sfStepHeight;
        //            tags.add("step");
        //        }
        //    }
        //}
        

        ////////////////////////
        // More in air checks.//
        ////////////////////////
        // TODO: move into the in air checking above !?
        if (!envelopeHack && !resetFrom && !resetTo) {
            vDistanceAboveLimit = Math.max(vDistanceAboveLimit, inAirChecks(now, from, to, hDistance, yDistance, thisMove, lastMove, data, cc));
        }


        // Join the lowjump tag
        if (data.sfLowJump) {
            tags.add("lowjump");
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
    private double vAllowedDistanceNoData(final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
                                          final double maxJumpGain, final double jumpGainMargin, 
                                          final MovingData data, final MovingConfig cc) {
        double vAllowedDistance;
        tags.add(lastMove.valid ? "data_reset" : "data_missing");

        if (thisMove.yDistance > -(Magic.GRAVITY_MAX + Magic.GRAVITY_SPAN) && thisMove.yDistance < 0.0) {
            // Allow falling.
            vAllowedDistance = thisMove.yDistance;
        }
        else if (thisMove.from.onGround || (lastMove.valid && lastMove.to.onGround)) {
            // Allow jumping.
            // TODO: Is (lastMove.valid && lastMove.to.onGround) safe?
            vAllowedDistance = maxJumpGain + jumpGainMargin;
            if (lastMove.to.onGround && vAllowedDistance < 0.1) {
                vAllowedDistance = 0.42 + jumpGainMargin;
            }

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
        final boolean yDirChange = lastMove.toIsValid && lastMove.yDistance != yDistance && (yDistance <= 0.0 && lastMove.yDistance >= 0.0 || yDistance >= 0.0 && lastMove.yDistance <= 0.0 ); 
        if (yDirChange) vDistanceAboveLimit = yDirChange(from, to, yDistance, vDistanceAboveLimit, lastMove, data);

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
                if ((data.timeRiptiding + 500 > now) 
                    || isLanternUpper(to) 
                    || (lastMove.from.inLiquid && Math.abs(yDistance) < 0.31)) {
                    // Ignore
                }
                else {
                    data.vDistAcc.add((float) yDistance);
                    final double accAboveLimit = verticalAccounting(yDistance, data.vDistAcc, tags, "vacc" + (data.isVelocityJumpPhase() ? "dirty" : ""));
                    if (accAboveLimit > vDistanceAboveLimit) {
                        if (data.getOrUseVerticalVelocity(yDistance) == null) {
                            vDistanceAboveLimit = accAboveLimit;
                        }
                    }
                }
            }
            else {
                // TODO: Just to exclude source of error, might be redundant.
                data.vDistAcc.clear();
            }
        }
        return vDistanceAboveLimit;
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
                                                   final ActionAccumulator acc, final ArrayList<String> tags, 
                                                   final String tag) {
        // TODO: Add air friction and do it per move anyway !?
        final int count0 = acc.bucketCount(0);
        if (count0 > 0) {
            final int count1 = acc.bucketCount(1);
            if (count1 > 0) {
                final int cap = acc.bucketCapacity();
                final float sc0;
                sc0 = (count0 == cap) ? acc.bucketScore(0) : 
                                        // Catch extreme changes quick.
                                        acc.bucketScore(0) * (float) cap / (float) count0 - Magic.GRAVITY_VACC * (float) (cap - count0);
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

        final long now = System.currentTimeMillis();
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
                        || isLanternUpper(to)) {
                        // Ignore.
                    }
                    else {
                        vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(yDistance));
                        tags.add("ychincfly");
                    }
                }
                else tags.add("ychincair");
            }
        }
        else {
            // Decrease
            tags.add("ychdec");
            // Detect low jumping.
            // TODO: sfDirty: Account for actual velocity (demands consuming queued for dir-change(!))!
            if (!data.sfLowJump && !data.sfNoLowJump && data.liftOffEnvelope == LiftOffEnvelope.NORMAL &&
                lastMove.toIsValid && lastMove.yDistance > 0.0 && !data.isVelocityJumpPhase()
                ) {

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
     * buffer, velocity, bunnyhop, block move and reset-item, also re-check hDist with permissions, if needed.
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

        // TODO: Still not entirely sure about this checking order.
        // TODO: Would quick returns make sense for hDistanceAfterFailure == 0.0?
        final long now = System.currentTimeMillis();

        // 1: Attempt to reset item on NoSlow Violation
        if (cc.survivalFlyResetItem && hDistanceAboveLimit > 0.0 && data.sfHorizontalBuffer <= 0.5 && tags.contains("usingitem")) {
            tags.add("itemreset");
            // Handle through nms
            if (mcAccess.getHandle().resetActiveItem(player)) {
                data.isusingitem = false;
                pData.requestUpdateInventory();
            }
            // Off hand (non nms)
            else if (Bridge1_9.hasGetItemInOffHand() && data.offhanduse) {
                ItemStack stack = Bridge1_9.getItemInOffHand(player);
                if (stack != null) {
                    if (ServerIsAtLeast1_13) {
                        if (player.isHandRaised()) {
                            // Does nothing
                        }
                        // False positive
                        else data.isusingitem = false;
                    } 
                    else {
                        player.getInventory().setItemInOffHand(stack);
                        data.isusingitem = false;
                    }
                }
            }
            // Main hand (non nms)
            else if (!data.offhanduse) {
                ItemStack stack = Bridge1_9.getItemInMainHand(player);
                if (ServerIsAtLeast1_13) {
                    if (player.isHandRaised()) {
                        data.olditemslot = player.getInventory().getHeldItemSlot();
                        if (stack != null) player.setCooldown(stack.getType(), 10);
                        player.getInventory().setHeldItemSlot((data.olditemslot + 1) % 9);
                        data.changeslot = true;
                    }
                    // False positive
                    else data.isusingitem = false;
                } 
                else {
                    if (stack != null) {
                        Bridge1_9.setItemInMainHand(player, stack);
                    }
                }
                data.isusingitem = false;
            }
            if (!data.isusingitem) {
                hAllowedDistance = setAllowedhDist(player, sprinting, thisMove, data, cc, pData, from, true);
                hDistanceAboveLimit = thisMove.hDistance - hAllowedDistance;
            }
        }

        // 2: Test bunny early, because it applies often and destroys as little as possible.
        if (!(from.isAboveStairs() && to.isAboveStairs() && to.isOnGround())){
            hDistanceAboveLimit = bunnyHop(from, to, hAllowedDistance, hDistanceAboveLimit, sprinting, thisMove, lastMove, data, cc);
        }

        // 3: After failure permission checks ( + speed modifier + sneaking + blocking + speeding) and velocity (!).
        // Noslow require no permission rechecks
        // TODO: Most cases these will not apply. Consider redesign to do these last or checking right away and skip here on some conditions.
        //if (hDistanceAboveLimit > 0.12 && !tags.contains("usingitem(cancel)") && !skipPermChecks)  {
        //    hAllowedDistance = setAllowedhDist(player, sprinting, thisMove, data, cc, pData, from, true);
        //    hDistanceAboveLimit = thisMove.hDistance - hAllowedDistance;
        //    tags.add("permchecks");
        //}
       
        // 4: Check being moved by blocks.
        // 1.025 is a Magic value
        if (cc.trackBlockMove && hDistanceAboveLimit > 0.0 && hDistanceAboveLimit < 1.025) {
            // Push by 0.49-0.51 in one direction. Also observed 1.02.
            // TODO: Better also test if the per axis distance is equal to or exceeds hDistanceAboveLimit?
            // TODO: The minimum push value can be misleading (blocked by a block?)
            final double xDistance = to.getX() - from.getX();
            final double zDistance = to.getZ() - from.getZ();
            if (Math.abs(xDistance) > 0.485 && Math.abs(xDistance) < 1.025
                && from.matchBlockChange(blockChangeTracker, data.blockChangeRef, 
                                         xDistance < 0 ? Direction.X_NEG : Direction.X_POS, 0.05)
                ) {
                hAllowedDistance = thisMove.hDistance; // MAGIC
                hDistanceAboveLimit = 0.0;
            }
            else if (Math.abs(zDistance) > 0.485 && Math.abs(zDistance) < 1.025
                    && from.matchBlockChange(blockChangeTracker, data.blockChangeRef, 
                                             zDistance < 0 ? Direction.Z_NEG : Direction.Z_POS, 0.05)
                    ) {
                hAllowedDistance = thisMove.hDistance; // MAGIC
                hDistanceAboveLimit = 0.0;
            }
        }

        // 5: Check velocity.
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
                bufferUse = false;
                hDistanceAboveLimit = Math.max(0.0, hDistanceAboveLimit - hFreedom);
                if (hDistanceAboveLimit <= 0.0) {
                    data.combinedMediumHCount = 0;
                    data.combinedMediumHValue = 0.0;
                    tags.add("hvel_no_hacc");
                }
            }
        }

        // 6: Re-check for bunnyhopping if the hDistance is still above limit (2nd).
        if (hDistanceAboveLimit > 0 && !(from.isAboveStairs() && to.isAboveStairs() && to.isOnGround())) {
            hDistanceAboveLimit = bunnyHop(from, to, hAllowedDistance, hDistanceAboveLimit, sprinting, thisMove, lastMove, data, cc);
        }

        // 7: Horizontal buffer.
        if (hDistanceAboveLimit > 0.0 && data.sfHorizontalBuffer > 0.0 && bufferUse) { // && !Magic.inAir(thisMove)) {
            tags.add("hbufuse");
            final double amount = Math.min(data.sfHorizontalBuffer, hDistanceAboveLimit);
            hDistanceAboveLimit -= amount;
            data.sfHorizontalBuffer = Math.max(0.0, data.sfHorizontalBuffer - amount); // Ensure we never end up below zero.
        }
        
        // TODO: Remove this...
        if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 3000 > now)) {
            if (Bridge1_9.isGliding(player) && hDistanceAboveLimit < 0.5) {
                hDistanceAboveLimit = 0.0;
            }
            tags.add("hriptide");
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
        // TODO: A more state-machine like modeling (hop, slope, states, low-edge).
        boolean allowHop = true;
        boolean double_bunny = false;
        final double hDistance = thisMove.hDistance;
        final double yDistance = thisMove.yDistance;
        final double baseSpeed = thisMove.hAllowedDistanceBase;
        final double lastBaseSpeed = lastMove.hAllowedDistanceBase;

        // Fly phase.
        // TODO: Check which conditions might need resetting at lower speed (!).
        // Friction phase.
        if (lastMove.toIsValid && data.bunnyhopDelay > 0 && hDistance > baseSpeed) {
            allowHop = false;
            final int hopTime = bunnyHopMax - data.bunnyhopDelay;

            // Increase buffer if hDistance is decreasing properly.
            if (lastMove.hDistance > hDistance) {

                final double hDistDiff = lastMove.hDistance - hDistance;
                // Bunny slope (downwards, directly after hop but before friction).
                if (data.bunnyhopDelay == bunnyHopMax - 1) {

                    // Ensure relative speed decrease vs. hop is met somehow.
                    if (hDistDiff >= 0.66 * (lastMove.hDistance - baseSpeed)) {
                        tags.add("bunnyslope");
                        hDistanceAboveLimit = 0.0;
                    }
                }
                else if (
                        hDistDiff >= lastMove.hDistance / bunnyDivFriction 
                        || hDistDiff >= hDistanceAboveLimit / 33.3 
                        || hDistDiff >= (hDistance - baseSpeed) * (1.0 - Magic.FRICTION_MEDIUM_AIR)
                        ) {

                    // TODO: Confine friction by medium ?
                    // TODO: Also calculate an absolute (minimal) speed decrease over the whole time, at least max - count?
                    final double maxSpeed = baseSpeed * (data.bunnyhopTick > 0 ? 1.09 : 1.255);
                    final double allowedSpeed = maxSpeed * Math.pow(0.99, bunnyHopMax - data.bunnyhopDelay);
                    tags.add("bunnyfriction");
                    // Speed must decrease by "a lot" at first, then by some minimal amount per event.
                    // TODO: Confine buffer to only be used during low jump phase !?
                    // Normal speed
                    if (hDistance <= allowedSpeed
                        // Exemption
                        || data.bunnyhopTick > 6 || data.isVelocityJumpPhase() 
                        || thisMove.headObstructed && hDistance < 0.39 
                        || data.keepfrictiontick > 0) {
                        tags.add("allowbunny");
                        hDistanceAboveLimit = 0.0;
                    }
                     
                    if (data.bunnyhopDelay == 1 && !thisMove.to.onGround && !to.isResetCond()) {
                        // ... one move between toonground and liftoff remains for hbuf ...
                        data.bunnyhopDelay++;
                        tags.add("bunnyfly(keep)");
                    }
                    else tags.add("bunnyfly(" + data.bunnyhopDelay +")");
                }
            }

            // 2x horizontal speed increase detection.
            // TODO: Confine to increasing set back y ?
            if (!allowHop && hDistance - lastMove.hDistance >= baseSpeed * 0.5 && hopTime == 1) {
                if (lastMove.yDistance >= -Magic.GRAVITY_MAX / 2.0 && lastMove.yDistance <= 0.0 && yDistance >= 0.4 
                    && lastMove.touchedGround) {
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
                        && lastMove.hDistance > lastBaseSpeed 
                        && lastMove.hDistance < 1.34 * lastBaseSpeed
                        && thisMove.hDistance > lastMove.hDistance * 1.24
                        && thisMove.hDistance < lastMove.hDistance * 1.34
                        || yDistance >= 0.0 && thisMove.headObstructed 
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
        } // (friction phase)


        // Allow sprintjumping on slime blocks
        // TODO: Better model, using the buffer here is rough.
        if ((from.getBlockFlags() & BlockProperties.F_BOUNCE25) != 0
            && data.bunnyhopDelay == 9
            && !thisMove.from.onGround && hDistance < 0.45){
            hDistanceAboveLimit = 0.0;
            data.bunnyhopTick = 15;
            data.sfHorizontalBuffer = Math.max(data.sfHorizontalBuffer, 0.5); 
            tags.add("bouncebunny");
        }


        // bunnyhop-> bunnyslope-> bunnyfriction-> ground-> microjump(still bunnyfriction)-> bunnyfriction
        //or bunnyhop-> ground-> slidedown-> bunnyfriction
        // Hit ground but slipped away by somehow and still remain bunny friction
        final double inc = ServerIsAtLeast1_13 ? 0.03 : 0;
        final double hopMargin = (data.bunnyhopTick > 0 ? (data.bunnyhopTick > 2 ? 1.0 + inc : 1.11 + inc) : 1.22 + inc);

        if (lastMove.toIsValid && data.bunnyhopDelay <= 0 && data.lastbunnyhopDelay > 0 && lastMove.hDistance > hDistance 
            && baseSpeed > 0.0 && hDistance / baseSpeed < hopMargin){

            final double hDistDiff = lastMove.hDistance - hDistance;
            if (
                hDistDiff >= lastMove.hDistance / bunnyDivFriction 
                || hDistDiff >= hDistanceAboveLimit / 33.3 
                || hDistDiff >= (hDistance - baseSpeed) * (1.0 - Magic.FRICTION_MEDIUM_AIR)
                ) {
                //if (data.lastbunnyhopDelay == 8 && thisMove.from.onGround && !thisMove.to.onGround) {
                //    data.lastbunnyhopDelay++;
                //    tags.add("bunnyfriction(keep)"); // TODO: Never happen?
                //} else 

                if (hDistDiff < 0.01) {
                    // Allow the move
                    hDistanceAboveLimit = 0.0;
                    tags.add("bunnyfriction(2)");
                    
                    // Remove lowjump in this hop, prevent false in next hop
                    if (data.sfLowJump) {
                        data.sfLowJump = false;
                        tags.add("bunny_no_low");
                        data.bunnyhopDelay = data.lastbunnyhopDelay - 1;
                        data.lastbunnyhopDelay = 0;
                    }
                } 
                else data.lastbunnyhopDelay = 0;
            } 
            else data.lastbunnyhopDelay = 0;
        }


        // Check hop (singular peak up to roughly two times the allowed distance).
        // TODO: Needs better modeling.
        // TODO: Test bunny spike over all sorts of speeds + attributes.
        // TODO: Allow slightly higher speed on lost ground?
        // TODO: LiftOffEnvelope.allowBunny ?
        final double hopTickMultiplier = ((!lastMove.toIsValid || lastMove.hDistance == 0.0 && lastMove.yDistance == 0.0) ? 1.11 : 1.314);
        final double hopTickMultiplier2 = (data.bunnyhopTick > 0 ? (data.bunnyhopTick > 2 ? 1.76 : 1.96) : 2.15);
        final double hopTickMultiplier3 = (data.bunnyhopTick > 0 ? (data.bunnyhopTick > 2 ? 1.9 : 2.1) : 2.3);

        if (allowHop && hDistance >= baseSpeed
            // 0: Accelleration envelope
            && (hDistance > hopTickMultiplier * baseSpeed || data.keepfrictiontick > 0) && hDistance < hopTickMultiplier2 * baseSpeed
            // 0: Horizontal distance envelope
            // TODO: Walk speed (static or not) is not a good reference, switch to need normal/base speed instead.
            || (yDistance > from.getyOnGround() || hDistance < hopTickMultiplier3 * baseSpeed) 
            && lastMove.toIsValid && hDistance > 1.314 * lastMove.hDistance && hDistance < 2.15 * lastMove.hDistance
            ) { 
            
            // Pre-condition: normal jumping envelope, not a lowjump or a noLowJump flag is set for thisMove
            if (data.liftOffEnvelope == LiftOffEnvelope.NORMAL && (!data.sfLowJump || data.sfNoLowJump)
                // 0: Y-distance envelope.
                && yDistance >= 0.0 && 
                (
                    // 1: Normal jumping.
                    yDistance > 0.0 
                    && yDistance > data.liftOffEnvelope.getMinJumpGain(data.jumpAmplifier) - Magic.GRAVITY_SPAN
                    // 1: Too short with head obstructed.
                    || thisMove.headObstructed || lastMove.toIsValid && lastMove.headObstructed && lastMove.yDistance <= 0.0
                    // 1: Hop without y distance increase at moderate h-speed.
                    // TODO: 2nd below: demand next move to jump. Relate to stored past moves. 
                    // TODO: Ensure the gain can only be used once per so and so.
                    || (cc.sfGroundHop || yDistance == 0.0 && !lastMove.touchedGroundWorkaround && !lastMove.from.onGround)
                    && baseSpeed > 0.0 && hDistance / baseSpeed < 1.5 && (hDistance / lastMove.hDistance < 1.35 || hDistance / baseSpeed < 1.35)
                )
                // 0: Ground + jump phase conditions.
                && (
                    // 1: Ordinary/obvious lift-off.
                    data.sfJumpPhase == 0 && thisMove.from.onGround 
                    // 1: Touched ground somehow.
                    || data.sfJumpPhase <= 1 && (thisMove.touchedGroundWorkaround || lastMove.touchedGround && !lastMove.bunnyHop) 
                    // 1: Double bunny.
                    || double_bunny
                )
                // 0: Don't allow bunny to run out of liquid.
                && (!from.isResetCond() && !to.isResetCond() || data.isHalfGroundHalfWater) // TODO: !to.isResetCond() should be reviewed.
                ) {

                // TODO: Jump effect might allow more strictness. 
                // TODO: Expected minimum gain depends on last speed (!).
                // TODO: Speed effect affects hDistanceAboveLimit?
                data.bunnyhopDelay = bunnyHopMax;
                hDistanceAboveLimit = 0D;
                thisMove.bunnyHop = true;
                tags.add("bunnyhop");
            }
            else tags.add("bunnyenv");
        }

        return hDistanceAboveLimit;
    }


    /**
     * Legitimate move: increase horizontal buffer somehow.
     * @param hDistance
     * @param amount Positive amount.
     * @param data
     */
    private void hBufRegain(final double hDistance, final double amount, final MovingData data, final MovingConfig cc) {
        /*
         * TODO: Consider different concepts: 
         *          - full resetting with harder conditions.
         *          - maximum regain amount.
         *          - reset or regain only every x blocks h distance.
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
        final long now = System.currentTimeMillis();
        final double yDistAbs = Math.abs(yDistance);
        final double baseSpeed = (thisMove.from.onGround && thisMove.to.onGround) ?
                                  Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player)) + 0.1 
                                : Magic.swimBaseSpeedV(Bridge1_13.isSwimming(player));
        
        // TODO: Later also cover things like a sudden stop.
        // Minimal speed.
        if (yDistAbs <= baseSpeed) {
            return new double[]{baseSpeed, 0.0};
        }

        if (from.isOnGround() && !BlockProperties.isLiquid(from.getTypeIdAbove())
            && (from.isInWaterLogged() || data.isHalfGroundHalfWater)
                ) {
            data.liftOffEnvelope = LiftOffEnvelope.NORMAL;
            final double jump = data.liftOffEnvelope.getMinJumpGain(data.jumpAmplifier);
            tags.add("liq-grd");
            return new double[]{jump, yDistance - jump};
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
        final Double wRes = VerticalLiquidRules.liquidWorkarounds(from, to, baseSpeed, frictDist, lastMove, data);
        if (wRes != null) {
            return new double[]{wRes, 0.0};
        }

        // Try to use velocity for compensation.
        if (data.getOrUseVerticalVelocity(yDistance) != null) {
            return new double[]{yDistance, 0.0};
        }
    
        // TODO: Move to creativefly
        if (Bridge1_13.isRiptiding(player) || (data.timeRiptiding + 1000 > now)) {
            return new double[]{yDistance, 0.0};
        }

        // TODO: Set magic speeds!
        if ((from.getBlockFlags() & BlockProperties.F_BUBBLECOLUMN) != 0) {
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
    private double vDistClimbable(final Player player, final PlayerLocation from, final PlayerLocation to,
                                  final boolean fromOnGround, final boolean toOnGround, 
                                  final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
                                  final double yDistance, final MovingData data) {

        double vDistanceAboveLimit = 0.0;
        long now = System.currentTimeMillis();
        data.sfNoLowJump = true;
        data.clearActiveHorVel();
        // TODO: Might not be able to ignore vertical velocity if moving off climbable (!).

        // TODO: bring in in-medium accounting
        final double jumpHeight = 1.35 + (data.jumpAmplifier > 0 ? (0.6 + data.jumpAmplifier - 1.0) : 0.0);
        // TODO: ladders are ground !
        // TODO: yDistance < 0.0 ?
        final double maxSpeed = yDistance < 0.0 ? Magic.climbSpeedDescend : Magic.climbSpeedAscend;
        if (Math.abs(yDistance) > maxSpeed) {
            if (from.isOnGround(jumpHeight, 0D, 0D, BlockProperties.F_CLIMBABLE)) {
                if (yDistance > data.liftOffEnvelope.getMaxJumpGain(data.jumpAmplifier) + 0.1) {
                    tags.add("climbstep");
                    vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(yDistance) - maxSpeed);
                }
            }
            else if (!(lastMove.from.inLiquid && Math.abs(yDistance) < Magic.swimBaseSpeedV(Bridge1_13.hasIsSwimming()))) {
                tags.add("climbspeed");
                vDistanceAboveLimit = Math.max(vDistanceAboveLimit, Math.abs(yDistance) - maxSpeed);
            }

            if ((Bridge1_13.isRiptiding(player) || data.timeRiptiding + 3000 > now) && vDistanceAboveLimit< 4.0) {
                vDistanceAboveLimit = 0.0;
            }
        }

        if (yDistance > 0.0) {
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
        data.jumpAmplifier = 0; 

        // Ascend: players cannot ascend in webs
        if (yDistance >= 0.0) {
            vAllowedDistance = (toOnGround && yDistance <= 0.5) ? yDistance : thisMove.from.onGround ? 0.1D : 0.0; 
            if (toOnGround && yDistance <= 0.5 && yDistance > 0.0) tags.add("web_step");
            vDistanceAboveLimit = yDistance - vAllowedDistance;
        }
        // Descend
        else {
            if (thisMove.from.resetCond && thisMove.to.resetCond) {
                vAllowedDistance = thisMove.hDistance > 0.018 ? Magic.webSpeedDescendH : Magic.webSpeedDescendDefault;
                vDistanceAboveLimit = yDistance < vAllowedDistance ? Math.abs(yDistance - vAllowedDistance) : 0.0;
            }
        }

        if (vDistanceAboveLimit > 0.0) {
            tags.add(yDistance > 0.0 ? "vweb" : "vwebdesc");
            
        }

        return new double[]{vAllowedDistance, vDistanceAboveLimit};
    }


    /**
     * Berry bush vertical distance checking
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
    private double[] vDistBush(final Player player, final PlayerMoveData thisMove, 
                               final boolean toOnGround, final double hDistanceAboveLimit, final long now, 
                               final MovingData data, final MovingConfig cc, final PlayerLocation from,
                               final boolean fromOnGround) {
        
        /* TODO: add something like this through via version, to be able to adapt limits a bit more dinamically for berry bushes
        * if (ServerIsAtLeast1_14 && ClientIsLowerThan1_14){
        *    vAllowedDistance = LiftOffEnvelope.NORMAL.getMaxJumpGain(data.jumpAmplifier) + 0.005
        *    vDistanceAboveLimit = yDistance - vAllowedDistance;
        *(...)
        */

        final double yDistance = thisMove.yDistance;
        final double jumpGainMargin = 0.005;
        double vAllowedDistance = 0.0;
        double vDistanceAboveLimit = 0.0;
        data.sfNoLowJump = true;

        // Ascend
        if (yDistance >= 0.0) {
            // TODO: Friction ?
            vAllowedDistance = (from.getBlockFlags() & BlockProperties.F_ALLOW_LOWJUMP) != 0 ?
                                // Compatibility with multiprotocol plugins: allow normal jumping
                                LiftOffEnvelope.NORMAL.getMaxJumpGain(data.jumpAmplifier) + jumpGainMargin : (data.liftOffEnvelope.getMinJumpGain(data.jumpAmplifier) * 20 / 21); // Normal 3/4 but....
            vDistanceAboveLimit = yDistance - vAllowedDistance;
        }
        // Descend
        else {
            // Observed: Upon first collision with a berry bush, a lot of lost ground cases will apply (pyramid and edgedesc)
            // Allow the first move when falling from far above.
            vAllowedDistance = (data.sfJumpPhase > 5 && !fromOnGround && toOnGround || thisMove.touchedGroundWorkaround) ? yDistance : Magic.bushSpeedDescend;
            vDistanceAboveLimit = yDistance < vAllowedDistance ? Math.abs(yDistance - vAllowedDistance) : 0.0;
        }

        if (vDistanceAboveLimit > 0.0) {
            tags.add(yDistance > 0.0 ? "vbush" : "vbushdesc");
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
                                    final MovingData data, final MovingConfig cc){

        // Increment violation level.
        if (Double.isInfinite(data.survivalFlyVL)) data.survivalFlyVL = 0;
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
    public final void handleHoverViolation(final Player player, final PlayerLocation loc, final MovingConfig cc, final MovingData data) {
        if (Double.isInfinite(data.survivalFlyVL)) data.survivalFlyVL = 0;
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
     * This is set with PlayerToggleSneak, to be able to distinguish players that are really sneaking from players that are set sneaking by a plugin. 
     * @param player + ")"
     * @param sneaking
     */
    public void setReallySneaking(final Player player, final boolean sneaking) {
        if (sneaking) reallySneaking.add(player.getName());
        else reallySneaking.remove(player.getName());
    }


    /** 
    * Collect the F_STICKY block flag. Clear NoFall's data upon side collision.
    * @param from
    * @param to
    * @param data
    */
    private boolean isCollideWithHB(PlayerLocation from, PlayerLocation to, MovingData data) {

        final boolean isFlagCollected = (from.getBlockFlags() & BlockProperties.F_STICKY) != 0
                                        ||(to.getBlockFlags() & BlockProperties.F_STICKY) != 0;
        // Moving on side block, remove nofall data
        if (isFlagCollected && BlockProperties.collides(to.getBlockCache(), to.getMinX() - 0.1, to.getMinY(), 
                                                        to.getMinZ() - 0.1, to.getMaxX() + 0.1, to.getMaxY(), 
                                                        to.getMaxZ() + 0.1, BlockProperties.F_STICKY)
            ) {
            data.clearNoFallData();
        }
        return isFlagCollected;
    }


    private boolean isLanternUpper(PlayerLocation from) {
        World w = from.getWorld();
        final int x = from.getBlockX();
        final int y = from.getBlockY() + 2;
        final int z = from.getBlockZ();
        if (w.getBlockAt(x, y, z).getType().toString().equals("LANTERN")) return true;
        return false;
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
        final String hopDelay = (data.bunnyhopDelay > 0 ? (" bHopDelay= " + data.bunnyhopDelay)  : "");
        final String hopTick = (data.bunnyhopTick > 0 ? (" bHopTick= " + data.bunnyhopTick) : "");
        builder.append(hopDelay + hopTick);
        builder.append("\n" + " hDist: " + StringUtil.fdec3.format(hDistance) + dHDist + " / Allowed: " +  StringUtil.fdec3.format(hAllowedDistance) + hBuf + lostSprint + hVelUsed + " , vDist: " + StringUtil.fdec3.format(yDistance) + dYDist + " / Allowed: " + StringUtil.fdec3.format(vAllowedDistance) + " , sby=" + (data.hasSetBack() ? (data.getSetBackY() + " (" + StringUtil.fdec3.format(to.getY() - data.getSetBackY()) + " / " + data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier) + ")") : "?"));
        if (lastMove.toIsValid) {
            builder.append(" , fdsq: " + StringUtil.fdec3.format(thisMove.distanceSquared / lastMove.distanceSquared));
        }
        if (thisMove.verVelUsed != null) {
            builder.append(" , vVelUsed: " + thisMove.verVelUsed + " ");
        }
        data.addVerticalVelocity(builder);
        //      if (data.horizontalVelocityCounter > 0 || data.horizontalFreedom >= 0.001) {
        //          builder.append("\n" + player.getName() + " horizontal freedom: " +  StringUtil.fdec3.format(data.horizontalFreedom) + " (counter=" + data.horizontalVelocityCounter +"/used="+data.horizontalVelocityUsed);
        //      }
        data.addHorizontalVelocity(builder);
        if (!resetFrom && !resetTo) {
            if (cc.survivalFlyAccountingV && data.vDistAcc.count() > data.vDistAcc.bucketCapacity()) {
                builder.append("\n" + " vacc: " + data.vDistAcc.toInformalString());
            }
        }
        if (cc.survivalFlyAccountingH && data.combinedMediumHCount > 0) {
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
        //      builder.append(data.stats.getStatsStr(false));
        NCPAPIProvider.getNoCheatPlusAPI().getLogManager().debug(Streams.TRACE_FILE, builder.toString());
    }

    
    private void logPostViolationTags(final Player player) {
        debug(player, "SurvivalFly Post violation handling tag update:\n" + StringUtil.join(tags, "+"));
    }
}