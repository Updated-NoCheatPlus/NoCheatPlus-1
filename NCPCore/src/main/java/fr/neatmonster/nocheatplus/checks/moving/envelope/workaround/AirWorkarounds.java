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
 * Aim of this class is to provide a quick'n'dirty way of handling movements that cannot be predicted through ordinary means (not necessarily elegance), thus resorting to hard-coded magic.<br>
 * A few things to keep in mind:<br>
 * <li> Before adding any workaround, you should attempt to handle the movement in the way the client intends it (or at least to the closest possible estimate that NCP's infrastructure will allow): falling back to a workaround sould be the last resort.</li>
 * <li> Each workaround has to have proper documentation. Emphasis on "why" the workaround is needed in the first place.</li>
 * <li> Our aim is to nerf / limit what cheaters can do, not catching every single kind of cheat implementation. </li>
 * From this premise, a workaround should then have limited room for exploitation: we prefer players not having to deal with false positives, than catching low-level cheats in this instance.
 *    (If possible, do give an example on what kind of exploits might be enabled by the workaround)</li>
 * <li> Avoid adding too many [and nested, more than 2] conditions (like it was prior to the vDistRel rework). </li>
 * <li> To keep a better track / overview of workarounds, do make use of the workaround registry (append the "use" after all conditions. See the doc for it) </li>
 */ 
public class AirWorkarounds {

    /**
     * Several non-predictable moves with gliding
     * 
     * @param data
     * @param player
     * @param from
     * @param fromOnGround
     * @param to
     * @param toOnGround
     * @param isNormalOrPacketSplitMove
     * @return True, if a workaround applies
     */
    private static boolean oddGliding(final MovingData data, final Player player, final PlayerLocation from, boolean fromOnGround, final PlayerLocation to, boolean toOnGround, boolean isNormalOrPacketSplitMove) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        return 
               /*
                * 0: Touch down into ground / webs / water / whatever, allow the movement. NCP will then force-stop gliding.
                */
               !from.isOnGroundOrResetCond() && to.isOnGroundOrResetCond() && lastMove.yDistance < 0.0
               && data.ws.use(WRPT.W_M_SF_TOUCHDOWN)
               /*
                * 0: Same case but with a block change.
                */
               || !fromOnGround && toOnGround && lastMove.yDistance < 0.0
               && data.ws.use(WRPT.W_M_SF_TOUCHDOWN)
               /*
                * 0: With Bukkit skipping moving events... Not sure how further we can confine this one.
                */
               || !isNormalOrPacketSplitMove
               && data.ws.use(WRPT.W_M_SF_INACCURATE_SPLIT_MOVE)
               /*
                * 0: Boosting against a ceiling.
                */
               || data.fireworksBoostDuration > 0 && from.seekHeadObstruction()
               && data.ws.use(WRPT.W_M_SF_HEAD_OBSTRUCTION)
        ;
    }

    /**
     * Several non-predictable moves with levitation
     * 
     * @param data
     * @param player
     * @param from
     * @param fromOnGround
     * @return True, if a workaround applies
     */
    private static boolean oddLevitation(final MovingData data, final Player player, final PlayerLocation from, final boolean fromOnGround, boolean isNormalOrPacketSplitMove, boolean toOnGround) {
        final IPlayerData pData = DataManager.getPlayerData(player);
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        return  

               /*
                * 0: Don't predict if the movement has messed up coordinates.
                */
                !isNormalOrPacketSplitMove && thisMove.yDistance >= 0.0
                && data.ws.use(WRPT.W_M_SF_INACCURATE_SPLIT_MOVE)
               /*
                * 0: Players can still press the space bar in powder snow to boost ascending speed.
                */
                || from.isInPowderSnow() && MathUtil.between(thisMove.yAllowedDistance, thisMove.yDistance, thisMove.yAllowedDistance + data.liftOffEnvelope.getJumpGain(data.jumpAmplifier))
                && thisMove.yDistance > 0.0 && lastMove.yDistance > 0.0 && BridgeMisc.hasLeatherBootsOn(player)
                && data.ws.use(WRPT.W_M_SF_PWDSNW_ASCEND)
               /*
                * 0: The first move is -most of the time- mispredicted due to... Whatever. Micro moves?
                * TODO: test if this is actually due to micro-moves and remove it. We have a proper way of handling split moves now.
                */
                || (
                     // 1: First move that actually leaves the ground
                     fromOnGround && !thisMove.to.onGround
                     && thisMove.yDistance < data.liftOffEnvelope.getJumpGain(Bridge1_9.getLevitationAmplifier(player) - 1, data.nextStuckInBlockVertical) * data.lastFrictionVertical
                     // 1: Don't know what's going on here: when receiving levitation, the client "stalls" server-side for a tick. They'll begin to levitate on the next tick.
                     // https://gyazo.com/9ea2d76301bcfd5bc10dca2c6db77e63
                     || thisMove.hasLevitation && !lastMove.hasLevitation && fromOnGround && toOnGround && thisMove.yDistance == 0.0
                )
                && data.ws.use(WRPT.W_M_SF_FIRST_MOVE_ASCNEDING_FROM_GROUND)
               /*
                * 0: Sbyte overflow. Still not fixed by Mojang (!!)
                * Levitation level over 127 = fall down at a fast or slow rate, depending on the value.
                * Nothing much to add here, this will effectively let players move freely. The Extreme subcheck will however ensure that players cannot perform insane moves.
                * Why would we have to also keep up with Mojang's negligence? Playing cat-and-mice with cheaters is more than enough. 
                */
                || Bridge1_9.getLevitationAmplifier(player) >= 127
                && data.ws.use(WRPT.W_M_SF_SBYTE_OVERFLOW)
               /*
                * 0: Let older clients on newer servers ascend with protocol-hack plugins emulating levitation.
                * Not going to put up with server administrators who want compatibility AND ACCURACY/CHEAT PROTECTION for every client under the rainbow (including 10+ year old ones)
                */
                || pData.getClientVersion().isOlderThan(ClientVersion.V_1_9) && thisMove.yDistance > 0.0 && ServerVersion.compareMinecraftVersion("1.9") >= 0
                && data.ws.use(WRPT.W_M_SF_LEVITATION_1_8_CLIENT)
            ;
    }


    /**
     * Several non-predictable moves.
     * Some workarounds in here may be more 'structural' (read as: not intended to be a temporary solution, but rather a definitive "fix") than others.
     * 
     * @param data
     * @param fromOnGround
     * @param toOnGround
     * @param from
     * @param to
     * @param predictedDistance Will be overridden if this method returns true.
     * @return True, if a non-predictable case applies.
     */
    public static boolean checkPostPredictWorkaround(final MovingData data, final boolean fromOnGround, final boolean toOnGround, final PlayerLocation from, 
                                                     final PlayerLocation to, final double predictedDistance, final Player player, boolean isNormalOrPacketSplitMove) {
    	final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData secondLastMove = data.playerMoves.getSecondPastMove();
        final IPlayerData pData = DataManager.getPlayerData(player);
        final double yAcceleration = thisMove.yDistance - lastMove.yDistance;

        if (Bridge1_9.isGliding(player)) {
            return oddGliding(data, player, from, fromOnGround, to, toOnGround, isNormalOrPacketSplitMove);
        }
        if (!Double.isInfinite(Bridge1_9.getLevitationAmplifier(player))) {
            return oddLevitation(data, player, from, fromOnGround, isNormalOrPacketSplitMove, toOnGround);
        }
        
        return
               /*
                * 0: Allow players preparing to step down a block / simply descending.
                * For the game, this move is fully on ground, thus gravity is not yet applied (hence 0 speed and 0 jump height), but for NCP (which distinguishes moving from/to ground) this is seen as "leaving the ground", so it will try to enforce friction right away.
                * Strictly speaking, this should be confined by && !toOnGround. For the sake of leniency, we'll demand to be on ground with just the from location.
                */
                thisMove.yDistance == 0.0 && thisMove.setBackYDistance == 0.0 && fromOnGround
                && data.ws.use(WRPT.W_M_SF_PREPARE_TO_DESCEND)
               /*
                * 0: Allow moves with extremely little air times [for NCP].
                * Player leaves the ground for such a short period of time that the game never enforces gravity.
                * Happens when sprinting over small, 1-block wide gaps (the game does allow players to do so)
                * See: https://gyazo.com/c772058239ab28a8d976fe5a31959a82
                */
                || !fromOnGround && toOnGround && lastMove.from.onGround && !lastMove.to.onGround && data.sfJumpPhase <= 1
                && lastMove.toIsValid && thisMove.hDistance > 0.18 && lastMove.yDistance == 0.0 && thisMove.yDistance == 0.0
                && data.ws.use(WRPT.W_M_SF_NO_GRAVITY_GAP)
               /*
                * 0: Allow the first move after teleport/set back/respawn on 1.7.10
                * TODO: ... Time to drop support for 1.6 and 1.7 as well?
                *  Please Mojang, release that PvP update already so that we can finally kill off 1.8.
                */
                || PlayerEnvelopes.couldBeSetBackLoop(data)
                && data.ws.use(WRPT.W_M_SF_COULD_BE_SETBACK_LOOP)
                /*
                 * 0: Very specific case appeared on 1.20 and above: on stepping down a bed, the first friction move has speed of -0.047607
                 * instead of the regular (and predicted) gravity slope of -0.0784
                 */
                || pData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20)
                && lastMove.from.onBouncyBlock && !lastMove.from.onSlimeBlock && !fromOnGround && !toOnGround && thisMove.yDistance < 0.0
                && lastMove.yDistance == 0.0 && MathUtil.inRange(-Magic.GRAVITY_ODD, thisMove.yDistance, -Magic.GRAVITY_VACC) && thisMove.hDistance > 0.1
                && data.ws.use(WRPT.W_M_SF_BED_STEP_DOWN)
               /*
                * 0: Allow falling from high above into powder snow.
                * Player is in powder snow (for NCP) but the game has this really odd mechanic where the block shape is smaller if fall distance is > 2.5.
                * FALLING_COLLISION_SHAPE: 0.8999999761581421D (height) in PowderSnowBlock.java.
                * Can't be fixed without some kind of rework to the block-shape retrieval and definition.
                * TODO: This needs to be handled in a more decent way in RichEntityLocation. Will be removed.
                * TODO: Does this actually work?
                */
                || data.noFallFallDistance > 2.5 && thisMove.from.inPowderSnow && !lastMove.from.inPowderSnow && thisMove.yDistance < 0.0 && lastMove.yDistance < 0.0
                && data.ws.use(WRPT.W_M_SF_LANDING_ON_PWDSNW_FALLDIST_25)
               /*
                * 0: Allow the subsequent move of the one above as well, as it will still yield a false positive.
                * TODO: This needs to be handled in a more decent way in RichEntityLocation. Will be removed.
                * TODO: Does this actually work?
                */
                || thisMove.to.inPowderSnow && !secondLastMove.from.inPowderSnow && thisMove.yDistance < 0.0 && lastMove.yDistance < 0.0
                && data.ws.use(WRPT.W_M_SF_LANDING_ON_PWDSNW_FALLDIST_25)
        ;
    }
}