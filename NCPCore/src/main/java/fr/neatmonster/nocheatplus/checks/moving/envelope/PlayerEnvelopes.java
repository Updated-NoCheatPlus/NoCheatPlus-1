package fr.neatmonster.nocheatplus.checks.moving.envelope;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;

/**
 * Various auxiliary methods for moving behaviour modeled after the client or otherwise observed on the server-side.
 */
public class PlayerEnvelopes {


    /**
     * A past air -> toOnGround, move with deceleration.
     * 
     * @param fromOnGround
     * @param toOnGround
     * @param thisMove
     * @return If so.
     */
    public static boolean isTouchDownMove(final PlayerMoveData secondLastMove, final PlayerMoveData lastMove) {
        return !lastMove.from.onGround && lastMove.to.onGround && lastMove.yDistance < 0.0 && lastMove.yDistance > secondLastMove.yDistance;
    }

    /**
     * Jump off the top off a block with the ordinary jumping envelope, however
     * from a slightly higher position with the initial gain being lower than
     * typical, but the following move having the y distance as if jumped off
     * with typical gain.
     * 
     * @param yDistance
     * @param maxJumpGain
     * @param thisMove
     * @param lastMove
     * @param data
     * @return
     */
    public static boolean noobJumpsOffTower(final double yDistance, final double maxJumpGain, 
                                            final PlayerMoveData thisMove, final PlayerMoveData lastMove, final MovingData data) {
        final PlayerMoveData secondPastMove = data.playerMoves.getSecondPastMove();
        return (
                data.sfJumpPhase == 1 && lastMove.touchedGroundWorkaround // TODO: Not observed though.
                || data.sfJumpPhase == 2 && Magic.inAir(lastMove)
                && secondPastMove.valid && secondPastMove.touchedGroundWorkaround
                )
                && Magic.inAir(thisMove)
                && lastMove.yDistance < maxJumpGain && lastMove.yDistance > maxJumpGain * 0.67
                && PlayerEnvelopes.isFrictionFalling(yDistance, maxJumpGain, data.lastFrictionVertical, Magic.GRAVITY_SPAN);
    }

    /**
     * Test if this + last 2 moves are within the gliding envelope (elytra), in
     * this case with horizontal speed gain.
     * 
     * @param thisMove
     * @param lastMove
     * @param pastMove1
     *            Is checked for validity in here (needed).
     * @return
     */
    public static boolean glideEnvelopeWithHorizontalGain(final PlayerMoveData thisMove, final PlayerMoveData lastMove, final PlayerMoveData pastMove1) {
        return pastMove1.toIsValid 
                && glideVerticalGainEnvelope(thisMove.yDistance, lastMove.yDistance)
                && glideVerticalGainEnvelope(lastMove.yDistance, pastMove1.yDistance)
                && lastMove.hDistance > pastMove1.hDistance && thisMove.hDistance > lastMove.hDistance
                && Math.abs(lastMove.hDistance - pastMove1.hDistance) < Magic.GLIDE_HORIZONTAL_GAIN_MAX
                && Math.abs(thisMove.hDistance - lastMove.hDistance) < Magic.GLIDE_HORIZONTAL_GAIN_MAX
                ;
    }

    /**
     * Advanced glide phase vertical gain envelope.
     * 
     * @param yDistance
     * @param previousYDistance
     * @return
     */
    public static boolean glideVerticalGainEnvelope(final double yDistance, final double previousYDistance) {
        return  // Sufficient speed of descending.
                yDistance < Magic.GLIDE_DESCEND_PHASE_MIN && previousYDistance < Magic.GLIDE_DESCEND_PHASE_MIN
                // Controlled difference.
                && yDistance - previousYDistance > Magic.GLIDE_DESCEND_GAIN_MAX_NEG 
                && yDistance - previousYDistance < Magic.GLIDE_DESCEND_GAIN_MAX_POS;
    }

    /**
     * Friction envelope testing, with a different kind of leniency (relate
     * off-amount to decreased amount), testing if 'friction' has been accounted
     * for in a sufficient but not necessarily exact way.<br>
     * In the current shape this method is meant for higher speeds rather (needs
     * a twist for low speed comparison).
     * 
     * @param thisMove
     * @param lastMove
     * @param friction
     *            Friction factor to apply.
     * @param minGravity
     *            Amount to subtract from frictDist by default.
     * @param maxOff
     *            Amount yDistance may be off the friction distance.
     * @param decreaseByOff
     *            Factor, how many times the amount being off friction distance
     *            must fit into the decrease from lastMove to thisMove.
     * @return
     */
    public static boolean enoughFrictionEnvelope(final PlayerMoveData thisMove, final PlayerMoveData lastMove, final double friction, 
                                                 final double minGravity, final double maxOff, final double decreaseByOff) {
    
        // TODO: Elaborate... could have one method to test them all?
        final double frictDist = lastMove.yDistance * friction - minGravity;
        final double off = Math.abs(thisMove.yDistance - frictDist);
        return off <= maxOff && Math.abs(thisMove.yDistance - lastMove.yDistance) <= off * decreaseByOff;
    }

    /**
     * A non-vanilla formula for if the player is (well) within in-air falling envelope.
     * 
     * @param yDistance
     * @param lastYDist
     * @param lastFrictionVertical
     * @param extraGravity Extra amount to fall faster.
     * @return
     */
    public static boolean isFrictionFalling(final double yDistance, final double lastYDist, 
                                            final double lastFrictionVertical, final double extraGravity) {
        if (yDistance >= lastYDist) {
            return false;
        }
        final double frictDist = lastYDist * lastFrictionVertical - Magic.GRAVITY_MIN;
        return yDistance <= frictDist + extraGravity && yDistance > frictDist - Magic.GRAVITY_SPAN - extraGravity;
    }
    
    /**
     * Test if the player is constricted in a 1.5 blocks-high area (applies to 1.14 clients and above).
     * We cannot detect if players try to jump in here: on the server side, player is seen as never leaving the ground and without any vertical motion.
     * 
     * @param from
     * @param pData
     * @return If onGround with ground-like blocks above within a margin of 0.001.
     */
    public static boolean isVerticallyConstricted(final PlayerLocation from, final PlayerLocation to, final IPlayerData pData) {
        if (pData.getClientVersion().isOlderThan(ClientVersion.V_1_14)) {
            return false;
        }
        if (pData.getGenericInstance(MovingData.class).playerMoves.getCurrentMove().touchedGroundWorkaround) {
            // Just ensure to not be too exploitable: ensure that lost ground is ruled out.
            return false;
        }
        if (!pData.isSneaking()) {
            return false;
        }
        return from.isHeadObstructed(0.09, false) && from.isOnGround() && to.isOnGround();
    }

    public static boolean isMathematicallyOnGround(final PlayerLocation pLoc) {
        return Math.abs(pLoc.getY() % (1 / 64D)) < Magic.PREDICTION_EPSILON;
    }

    /**
     * Test if this move is a bunnyhop <br>
     * (Aka: sprint-jump. Increases the player's speed up to roughly twice the usual base speed)
     * 
     * @param from
     * @param to
     * @param pData
     * @param fromOnGround
     * @param toOnGround
     * @param player
     * @return True, if isJump() returned true while the player is sprinting and not in a liquid.
     */
    public static boolean isBunnyhop(final PlayerLocation from, final PlayerLocation to, final IPlayerData pData, boolean fromOnGround, boolean toOnGround, final Player player) {
        final MovingData data = pData.getGenericInstance(MovingData.class);
        if (from.isInLiquid()) {
            return false;
        }
        return isJump(from, to, player, fromOnGround, toOnGround) && pData.isSprinting();
    }

    /**
     * NoCheatPlus' definition of a jump.<br>
     * (Minecraft does not offer a direct way to know if players could have jumped)
     * This is mostly intended for vertical motion, not for horizontal. Use PlayerEnvelopes#isBunnyhop() for that.
     * 
     * @param data
     * @param hasLevitation
     * @param jumpGain The jump speed.
     * @param fromOnGround Uses the BCT.
     * @param toOnGround Uses the BCT.
     * @return True if is a jump. 
     */
    public static boolean isJump(final PlayerLocation from, final PlayerLocation to, final Player player, boolean fromOnGround, boolean toOnGround) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final double jumpGain = data.liftOffEnvelope.getJumpGain(data.jumpAmplifier);
        // NoCheatPlus definition of "jumping" is pretty similar to Minecraft's which is moving from ground with the correct speed.
        // Of course, since we have our own onGround handling, we need to take care of all caveats that it entails... (Lost ground, delayed jump etc...)
        if (thisMove.hasLevitation) {
            return false;
        }
        if (thisMove.isRiptiding) {
            return false;
        }
        // How the fuck do you even detect jumping here !??
        // Motion doesn't change
        // Locations do not change
        // Bukkit velocity does not change...
        // Math on ground doesn't f work.
        // ... Do we need to brute force speed with jumping !? ~Lol?
       /*if (isVerticallyConstricted(from, to, pData) 
           && isMathematicallyOnGround(from) && !isMathematicallyOnGround(to)) {
            Bukkit.getServer().broadcastMessage("Trying to jump");
            return true;
        }*/
        return  
                // 0: Jump phase condition... Demand a very low air time.
                data.sfJumpPhase <= 1
                // 0: Ground conditions... Demand players to be in a "leaving ground" state.
                && ( 
                    // 1: Ordinary lift-off.
                    fromOnGround && !toOnGround
                    // 1: With jump being delayed a tick after (Player jumps server-side, but sends a packet with 0 y-dist. On the next tick, a packet containing the jump speed (0.42) is sent, but the player is already fully in air)
                    || lastMove.toIsValid && lastMove.yDistance <= 0.0 && !thisMove.headObstructed
                    && (
                            // 2: The usual case: here we know that the player actually came from ground with the last move
                            // https://gyazo.com/dfab44980c71dc04e62b48c4ffca778e
                            lastMove.from.onGround && !lastMove.to.onGround && !thisMove.touchedGroundWorkaround // Explicitly demand to not be using a lost ground case here.
                            // 2: With "lostground_stepdown-to": the last collision (above) is lost, so the player is seen as being in air for much longer.
                            // TODO: check for abuses.
                            // https://gyazo.com/a5c22069af8ba6a718308bf5b125659a
                            || lastMove.touchedGroundWorkaround && !thisMove.touchedGroundWorkaround
                            // 2: Second past move: 2 consecutive lost ground can happen. See: https://gyazo.com/1fbe521bf436fa2b45c196c2b4dc7ee3
                            || thisMove.touchedGroundWorkaround && !thisMove.to.onGround 
                            && lastMove.touchedGroundWorkaround 
                    ) 
                )
                // 0: Jump motion conditions... This is pretty much the only way we can know if the player has jumped.
                && (
                    // 1: The ordinary case. The player's speed matches the jump speed gain.
                    // This must be the current move, never the last one.
                    MathUtil.almostEqual(thisMove.yDistance, jumpGain, Magic.PREDICTION_EPSILON)
                    // 1: If head is obstructed, jumping cannot be predicted without MC's collision function. So, just cap motion which will be at or lower than ordinary jump gain, but never higher, and never lower than 0.1 (which is the maximum motion with no jump boost and jumping in a 2-blocks high area).
                    // Also, here, jumping with head obstructed uses the much more lenient step correction method (See comment in AirWorkarounds).
                    // This could be much stricter if we had a isHeadObstructed method that raytraces up to ceiling (to know how much free space the playe        
                    || lastMove.toIsValid && thisMove.headObstructed && thisMove.yDistance > 0.0
                    && (
                        // 2: The ordinary case
                        lastMove.yDistance <= 0.0 && MathUtil.inRange(0.1 * data.lastStuckInBlockVertical, thisMove.yDistance, jumpGain - Magic.GRAVITY_SPAN) 
                        // 2: With crawl mode (1.14+)
                        // https://gyazo.com/653af8221425802c31201d8e4577f4f5
                        || BridgeMisc.isVisuallyCrawling(player) && MathUtil.inRange(jumpGain - Magic.GRAVITY_SPAN, thisMove.yDistance, jumpGain)
                    ) 
                )
            ;
    }

    /**
     * NoCheatPlus' definition of "step".<br>
     * (Minecraft does not offer a direct way to know if players could have stepped up a block).
     * Does not take into consideration lost-ground cases.
     * 
     * @param data
     * @param stepHeight The step height (0.5, prior to 1.8, 0.6 from 1.8 and onwards)
     * @param fromOnGround Uses the BCT.
     * @param toOnGround Uses the BCT.
     * @return True if is a step up.
     */
    public static boolean isStep(final IPlayerData pData, boolean fromOnGround, boolean toOnGround) {
        final MovingData data = pData.getGenericInstance(MovingData.class);
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final MovingConfig cc = pData.getGenericInstance(MovingConfig.class);
        if (thisMove.hasLevitation) {
            return false;
        }
        if (thisMove.isRiptiding) {
            return false;
        }
        // NoCheatPlus definition of "stepping" is pretty simple compared to Minecraft's: moving from ground to ground with positive motion (correct motion[=0.6], rather)
        return  
                fromOnGround && toOnGround
                && (
                    // 1: Handle "excessive ground" cases. AKA: cases where the client leaves ground, but for NCP, the distance from ground is so small that it cannot distingush, resulting as if the player never left.
                    // (Thus, in this case, the game correctly applies friction)
                    thisMove.yDistance <= 0.0 && MathUtil.inRange(0.01, Math.abs(thisMove.yDistance), Magic.GRAVITY_MAX * 4.0)
                    // 1: Otherwise, if motion is positive, we must check against the exact stepping motion; if we don't want abuses that is.
                    || thisMove.yDistance > 0.0 && MathUtil.almostEqual(thisMove.yDistance, cc.sfStepHeight, Magic.PREDICTION_EPSILON)
                )
                // 0: ...Or having an extremely little air time from a ground status to a ground status (lastMove: from air/ground OR to air/ground thisMove: toOnGround.
                || lastMove.yDistance >= -Math.max(Magic.GRAVITY_MAX / 2.0, Math.abs(thisMove.yDistance) * 1.3)
                && lastMove.yDistance <= 0.0 && thisMove.yDistance > 0.0
                && lastMove.touchedGround && lastMove.toIsValid && toOnGround
                && MathUtil.inRange(0.001, thisMove.yDistance, cc.sfStepHeight)
            ;
    }

    /**
     * First move after set back / teleport. Originally has been found with
     * PaperSpigot for MC 1.7.10, however it also does occur on Spigot for MC
     * 1.7.10.
     * 
     * @param thisMove
     * @param lastMove
     * @param data
     * @return
     */
    public static boolean couldBeSetBackLoop(final MovingData data) {
        // TODO: Confine to from at block level (offset 0)?
        final double setBackYDistance;
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (data.hasSetBack()) {
            setBackYDistance = thisMove.to.getY() - data.getSetBackY();
        }
        // Skip being all too forgiving here.
        //        else if (thisMove.touchedGround) {
        //            setBackYDistance = 0.0;
        //        }
        else {
            return false;
        }
        return !lastMove.toIsValid && data.sfJumpPhase == 0 && thisMove.multiMoveCount > 0
                && setBackYDistance > 0.0 && setBackYDistance < Magic.PAPER_DIST 
                && thisMove.yDistance > 0.0 && thisMove.yDistance < Magic.PAPER_DIST && Magic.inAir(thisMove);
    }

    /**
     * Pre conditions: A slime block is underneath and the player isn't really
     * sneaking (with negative motion with thisMove). This does not account for pistons pushing (slime) blocks.<br>
     * 
     * @param player
     * @param from
     * @param to
     * @param data
     * @param cc
     * @return
     */
    public static boolean canBounce(final Player player, final PlayerLocation from, final PlayerLocation to, 
                                    final MovingData data, final MovingConfig cc, final IPlayerData pData) {
        
        // Workaround/fix for bed bouncing. getBlockY() would return an int, while a bed's maxY is 0.5625, causing this method to always return false.
        // A better way to do this would to get the maxY through another method, just can't seem to find it :/
        if (player.isSneaking()) {
            return false;
        }
        double blockY = (to.getY() + 0.4375) % 1 == 0 ? to.getY() : to.getBlockY();
        return 
                // 0: Normal envelope (forestall NoFall).
                MovingUtil.getRealisticFallDistance(player, from.getY(), to.getY(), data, pData) > 1.0
                && (
                    // 1: Ordinary.
                    to.getY() - blockY <= Math.max(cc.yOnGround, cc.noFallyOnGround)
                    // 1: With carpet.
                    || BlockProperties.isCarpet(to.getTypeId()) && to.getY() - to.getBlockY() <= 0.9
                ) 
                // 0: Within wobble-distance.
                || to.getY() - blockY < 0.286 && to.getY() - from.getY() > -0.9
                && to.getY() - from.getY() < -Magic.GRAVITY_MIN
                && !to.isOnGround()
                // 0: Wildcard micro bounces
                || to.isOnGround() && !from.isOnGround() && to.getY() - from.getY() < 0.0
                && MovingUtil.getRealisticFallDistance(player, from.getY(), to.getY(), data, pData) <= 1.0
                // 0: Wildcard riptiding. No point in checking for distance constraints here when speed is so high.
                || Bridge1_13.isRiptiding(player)
           ;
    }

}
