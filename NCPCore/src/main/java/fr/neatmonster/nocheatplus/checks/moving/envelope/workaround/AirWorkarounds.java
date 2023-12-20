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
package fr.neatmonster.nocheatplus.checks.moving.envelope.workaround;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.envelope.PlayerEnvelopes;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.workaround.WRPT;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;
import fr.neatmonster.nocheatplus.compat.versions.ClientVersion;
import fr.neatmonster.nocheatplus.compat.versions.ServerVersion;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;


/**
 * Aim of this class is to provide a quick'n'dirty way of handling movements that cannot be predicted through ordinary means, thus resorting to hard-coded magic. <br>
 * A few things to keep in mind:<br>
 *  - Before adding any workaround, you should attempt to handle the movement in the way the client intends it (or at least to the closest possible estimate that NCP's infrastructure will allow): falling back to a workaround sould be the last resort.<br>
 *  - Each workaround has to have proper documentation. Emphasis on "why" the workaround is needed in the first place.  <br>
 *  - Our aim is to nerf / limit what cheaters can do, not catching every single kind of cheat implementation. <br>
 *    From this premise, a workaround should then have limited room for exploitation: we prefer players not having to deal with false positives, than catching low-level cheat types in this instance.
 *    (If possible, do give an example on which kind of exploits might be possible with the intended workaround in place)<br>
 *  - Avoid adding too many [and nested, more than 2] conditions (like it was prior to the vDistRel rework).
 *  - To keep a better track / overview of workarounds, do make use of the workaround registry (append the "use" after all conditions. See the doc for it)
 */ 
public class AirWorkarounds {

    /**
     * Several non-predictable moves with levitation
     * 
     * @param data
     * @param player
     * @param from
     * @param fromOnGround
     * @return True, if a workaround applies
     */
    private static boolean oddLevitation(final MovingData data, final Player player, final PlayerLocation from, final boolean fromOnGround, boolean isNormalOrPacketSplitMove) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        return  
               /*
                * 0: Can't ascend if head is obstructed
                * DO note that this is the "lenient" headObstructed check: players might be considered as colliding if within the given leniency range.
                */
                (thisMove.headObstructed || lastMove.toIsValid && lastMove.headObstructed && lastMove.yDistance >= 0.0) 
                && data.ws.use(WRPT.W_M_SF_HEAD_OBSTRUCTION)
               /*
                * 0: Don't predict if the movement has messed up coordiantes.
                */
                || !isNormalOrPacketSplitMove && thisMove.yDistance > 0.0
                && data.ws.use(WRPT.W_M_SF_INACCURATE_SPLIT_MOVE)
               /*
                * 0: Players can still press the space bar in powder snow to boost ascending speed.
                */
                || from.isInPowderSnow() && MathUtil.between(thisMove.vAllowedDistance, thisMove.yDistance, thisMove.vAllowedDistance + data.liftOffEnvelope.getJumpGain(data.jumpAmplifier))
                && thisMove.yDistance > 0.0 && lastMove.yDistance > 0.0 && BridgeMisc.hasLeatherBootsOn(player)
                && data.ws.use(WRPT.W_M_SF_PWDSNW_ASCEND)
               /*
                * 0: The first move is -most of the time- mispredicted due to... Whatever. Micro moves?
                * TODO: test if this is actually due to micro-moves and remove it. We have a proper way of handling split moves now.
                */
                || fromOnGround && !thisMove.to.onGround 
                && thisMove.yDistance < data.liftOffEnvelope.getJumpGain(Bridge1_9.getLevitationAmplifier(player) - 1, data.nextStuckInBlockVertical) * data.lastFrictionVertical
                && data.ws.use(WRPT.W_M_SF_FIRST_MOVE_ASCNEDING_FROM_GROUND)
               /*
                * 0: Sbyte overflow. Still not fixed by Mojang (!!)
                * Levitation level over 127 = fall down at a fast or slow rate, depending on the value.
                * Nothing much to add here, this will effectively let players move freely. The Extreme subcheck will however ensure that players cannot perform insane moves.
                * Why would we have to keep up with Mojang's negligence? Playing cat-and-mice with cheaters is more than enough. 
                */
                || Bridge1_9.getLevitationAmplifier(player) >= 127
                && data.ws.use(WRPT.W_M_SF_SBYTE_OVERFLOW)
               /*
                * 0: Let older clients on newer servers ascend with protocol-hack plugins emulating levitation.
                * Not going to put up with server adminstrators who want compatibility AND ACCURACY/CHEAT PROTECTION for every client under the rainbow (including 10+ year old ones)
                */
                || pData.getClientVersion().isOlderThan(ClientVersion.V_1_9) && thisMove.yDistance > 0.0 && ServerVersion.compareMinecraftVersion("1.9") >= 0
                && data.ws.use(WRPT.W_M_SF_LEVITATION_1_8_CLIENT)
            ;
    }


    /**
     * Several non-predictable moves. Mostly involving collision.
     * Some workarounds in here may be more 'structural' (read as: not intended to be a temporary solution, but rather a definitive "fix") than others.
     * 
     * @param data
     * @param fromOnGround
     * @param toOnGround
     * @param from
     * @param to
     * @param predictedDistance This will be overridden if this method returns true.
     * @return True, if a non-predictable case applies.
     */
    public static boolean checkPostPredictWorkaround(final MovingData data, final boolean fromOnGround, final boolean toOnGround, final PlayerLocation from, 
                                                     final PlayerLocation to, final double predictedDistance, final Player player, boolean isNormalOrPacketSplitMove) {
    	final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData secondLastMove = data.playerMoves.getSecondPastMove();
        final double yAcceleration = thisMove.yDistance - lastMove.yDistance;
        // Early returns first
        if (data.fireworksBoostDuration > 0 && data.keepfrictiontick < 0 && thisMove.yDistance < 0.0
            // && yDistDiffEx <= 0.0
            && lastMove.toIsValid && thisMove.yDistance - lastMove.yDistance > -0.7) {
            data.keepfrictiontick = 0;
            // Transition from CreativeFly to SurvivalFly having been in a gliding phase.
            // TO be cleaned up...
            return true;
        }
        if (data.keepfrictiontick < 0) {
            if (lastMove.toIsValid) {
                if (thisMove.yDistance < 0.4 && lastMove.yDistance == thisMove.yDistance) {
                    data.keepfrictiontick = 0;
                    data.setFrictionJumpPhase();
                }
            } 
            else data.keepfrictiontick = 0;
            // Transition from CreativeFly to SurvivalFly having been in a gliding phase.
            // TO be cleaned up...
            return true;
        }
        if (thisMove.hasLevitation) {
            return oddLevitation(data, player, from, fromOnGround, isNormalOrPacketSplitMove);
        }
        
        return

               /*
                * 0: Allow touch-down movements (first movement that has contact with ground after any given air phase). 
                * This is similar to head obstruction: the game seemingly "cuts off" speed when calling its collision function (amount cannot be predicted without the function). Speed will be reset on the very next move (from ground to ground).
                * This may allow for 1-block step cheats variants, or other low-level exploits that do make use of the landing acceleration of the player, but it's better than having a ton of workarounds to deal with.
                */
                // TODO: Demand having TWO descending move? Last move as well?
                !fromOnGround && toOnGround && thisMove.yDistance < 0.0 
                && (
                    // 1: Only if the player is actually decelerating (without this, there would be room for some nasty exploits [i.e.: abusing the workaround to ACCELERATE down to the ground faster than normal])
                    thisMove.yDistance > lastMove.yDistance 
                    // 1: With crawl mode (Very minor acceleration after colliding above)
                    // Should note that this does not happen with jump effect.
                    // https://gyazo.com/4808ee11dce1a527bb95100f86be2b08
                    || BridgeMisc.isVisuallyCrawling(player) && thisMove.yDistance < lastMove.yDistance 
                    && MathUtil.inRange(0.0, Math.abs(yAcceleration), Magic.GRAVITY_MAX)
                    // 1: With bounce effect (With weaker bounces, the player tends to slightly accelerate to the ground instead)
                    || data.verticalBounce != null && thisMove.yDistance < lastMove.yDistance
                )
                && data.ws.use(WRPT.W_M_SF_TOUCHDOWN)
               /*
                * 0: Allow players preparing to step down a block / simply descending.
                * For the game, this move is fully on ground, thus gravity is not yet applied (hence 0 speed and 0 jump height), but for NCP (which distinguishes moving from/to ground) this is seen as "leaving the ground", so it will try to enforce friction right away.
                * Strictly speaking, this should be confined by && !toOnGround. For the sake of leniency, we'll demand to be on ground with just the from location.
                */
                || thisMove.yDistance == 0.0 && thisMove.setBackYDistance == 0.0 && fromOnGround
                && data.ws.use(WRPT.W_M_SF_PREPARE_TO_DESCEND)
               /*
                * 0: Allow moves with extremely little air times [for NCP].
                * Player leaves the ground for a single tick, spread between two moves, then immediately lands back. This on ground change is not picked up by the game, so friction never gets applied.
                * [Essentially, NCP on ground's judgement is too precise/strict here]
                * Happens when sprinting over small, 1-block wide gaps (the game does allow players to do so)
                * See: https://gyazo.com/c772058239ab28a8d976fe5a31959a82
                */
                || !fromOnGround && toOnGround && lastMove.from.onGround && !lastMove.to.onGround 
                && lastMove.toIsValid && thisMove.hDistance > 0.2 && lastMove.yDistance == 0.0 && thisMove.yDistance == 0.0 && data.sfJumpPhase <= 1
                && data.ws.use(WRPT.W_M_SF_NO_GRAVITY_GAP)
               /*
                * 0: Wildcard lost-grund: no clean way to handle it without resorting to a ton of (more) hardcoded magic. 
                * Besides, most cases are already defined quite in detail; any room for abuse (and thus for bypasses) should be minimal.
                */
                || thisMove.touchedGroundWorkaround && lastMove.toIsValid
               /*
                * Originally observed with stuck-speed, but can happen with honey blocks as well (Standing still while also not rotating and trying to jump)
                * Caused by: the (fromIndex - toIndex + 1 < 1) condition in the split move mechanic (movinglistener).
                * Should be fixed so that this does not happen.
                */
                || !isNormalOrPacketSplitMove && thisMove.hDistance < 0.00001 && thisMove.yDistance != 0.0 && (from.isOnGround(0.1) || from.isOnGround() || to.isOnGround())
                && data.ws.use(WRPT.W_M_SF_INACCURATE_SPLIT_MOVE)
               /*
                * 0: After lostground_stepdown-to case, the vertical collision will interfere with the regular friction handling
                * See: https://gyazo.com/c8b1959aacb00927a9c6122224c60cef
                * Can be observed when bunnyhopping right before colliding horizontally with a block and landing on the very edge
                */
                || secondLastMove.toIsValid && lastMove.touchedGroundWorkaround && lastMove.yDistance < 0.0 && lastMove.setBackYDistance > 0.0
                && !thisMove.touchedGroundWorkaround && thisMove.yDistance < 0.0 && data.bunnyhopDelay <= 2
                && MathUtil.inRange(0.001, Math.abs(thisMove.yDistance - predictedDistance), 0.05)
                && data.ws.use(WRPT.W_M_SF_POST_LOSTGROUND_CASE)
               /*
                * 0: Allow the first move(s) bumping head with a block above.
                * Here, the game will reset vertical speed to 0, then proceeds to apply gravity. The move we receive on the server is the latter, so speed can never be 0.
                * The collision however will mess with speed as well, so this move is effectively "hidden" and cannot be predicted without reversing in much more detail what the game does.
                * Solution: just be lenient when this happens, somehow. *Jumping* will use the overly-forgiving step-correction leniency method (see PlayerEnvelopes), but to allow the subsequent descending move(s) (or ascending, depending on how high the ceiling is), we demand the player to collide within a fixed distance.
                * 0.2 is the result of testing (which covers the distance from eye position to the top of the player's AABB, roughly)
                * Now, this workaround would be even more effective if we had a "distanceToBlock/Ground()" method that ray-traces the collision distance from the player's box to the block.
                */
                || (from.isHeadObstructed(0.2, false) && thisMove.yDistance != 0.0 || thisMove.headObstructed && Bridge1_13.isRiptiding(player)) // We need to be much more lenient when riptiding.
                && data.ws.use(WRPT.W_M_SF_HEAD_OBSTRUCTION)
               /*
                * 0: Allow the first move after teleport/set back/respawn on 1.7.10
                */
                || PlayerEnvelopes.couldBeSetBackLoop(data)
                && data.ws.use(WRPT.W_M_SF_COULD_BE_SETBACK_LOOP)
               /*
                * 0: Allow falling from high above into powder snow.
                * Player is in powder snow (for NCP) but the game has this really odd mechanic where the block shape is smaller if fall distance is > 2.5.
                * FALLING_COLLISION_SHAPE: 0.8999999761581421D (height) in PowderSnowBlock.java.
                * Can't be fixed without some kind of rework to the block-shape retrieval and definition.
                * TODO: This needs to be handled in a more decent way in RichEntityLocation. Will be removed.
                */
                || data.noFallFallDistance > 2.5 && thisMove.from.inPowderSnow && !lastMove.from.inPowderSnow && thisMove.yDistance < 0.0 && lastMove.yDistance < 0.0
                && data.ws.use(WRPT.W_M_SF_LANDING_ON_PWDSNW_FALLDIST_25)
               /*
                * 0: Allow the subsequent move of the one above as well, as it will still yield a false positive.
                * TODO: This needs to be handled in a more decent way in RichEntityLocation. Will be removed.
                */
                || thisMove.to.inPowderSnow && !secondLastMove.from.inPowderSnow && thisMove.yDistance < 0.0 && lastMove.yDistance < 0.0
                && data.ws.use(WRPT.W_M_SF_LANDING_ON_PWDSNW_FALLDIST_25);
    }
}