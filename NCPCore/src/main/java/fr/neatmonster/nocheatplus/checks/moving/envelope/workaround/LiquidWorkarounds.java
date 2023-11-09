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
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.LiftOffEnvelope;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;

/**
 * Magic workarounds for moving in liquid (SurvivalFly.vDistLiquid).
 * (See AirWorkarounds)
 * 
 * @author asofold
 *
 */
public class LiquidWorkarounds {

    /**
     * 
     * @param from
     * @param to
     * @param baseSpeed
     * @param frictDist
     * @param thisMove
     * @param lastMove
     * @param data
     * @return The allowed distance for reference, in case the move is allowed.
     *         If no workaround applies, null is returned.
     */
    public static Double liquidWorkarounds(final Player player, final PlayerLocation from, final PlayerLocation to, final double baseSpeed, final double frictDist, final PlayerMoveData lastMove, final MovingData data) {
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final double yDistance = thisMove.yDistance;
        final PlayerMoveData pastMove1 = data.playerMoves.getSecondPastMove();
        final Vector downStreamVector = from.getLiquidPushingVector(to.getX() - from.getX(), to.getZ() - from.getZ(), from.isInWater() ? BlockFlags.F_WATER : BlockFlags.F_LAVA);
        final boolean downStream = downStreamVector.getZ() > 0.0 || downStreamVector.getX() > 0.0;

        if (yDistance >= 0.0) {
            // TODO: liftOffEnvelope: refine conditions (general) , should be near water level.
            // TODO: 1.5 high blocks ?
            // TODO: Conditions seem warped.
            if (yDistance <= 0.5) {

                // Decrease more than difference to baseSpeed.
                if (lastMove.toIsValid && yDistance < lastMove.yDistance && lastMove.yDistance - yDistance > Math.max(0.001, yDistance - baseSpeed)) {
                    return yDistance;
                }

                // Jump out water near edge ground
                if (lastMove.yDistance < -0.5 && yDistance > 0.4 && yDistance < frictDist - Magic.GRAVITY_MAX && from.isOnGround(0.6)) {
                    return frictDist - Magic.GRAVITY_MAX;
                }
                
                // Asc by water level
                if (!(data.liftOffEnvelope == LiftOffEnvelope.LIMIT_LIQUID && Double.isInfinite(Bridge1_13.getDolphinGraceAmplifier(from.getPlayer()))) 
                    && (
                        yDistance <= data.liftOffEnvelope.getJumpGain(data.jumpAmplifier) 
                        && !BlockProperties.isLiquid(from.getTypeIdAbove()) 
                        || !to.isInLiquid() // TODO: impossible !?
                        || (thisMove.to.onGround || lastMove.toIsValid && lastMove.yDistance - yDistance >= 0.010 || to.isAboveStairs())
                    )) {

                    double vAllowedDistance = baseSpeed + 0.397; 
                    double vDistanceAboveLimit = yDistance - vAllowedDistance;
                    if (vDistanceAboveLimit <= 0.0) {
                        return vAllowedDistance;
                    }
                }
            }

            if (lastMove.toIsValid) {

                // Launched in liquid by a bubble column with space bar kept pressed.
                if (Magic.recentlyInBubbleStream(20, data) && yDistance > 0.0 && lastMove.yDistance > 0.0
                    && !data.isVelocityJumpPhase() && yDistance < lastMove.yDistance * data.lastNonVanillaFrictionVertical
                    && yDistance < Magic.bubbleStreamAscend) {
                    return yDistance;
                }

                // Lenient on marginal violation if speed decreases by 'enough'.
                // (Observed on 'dirty' phase)
                // Also needed for waterfalls in 1.13+. Hence why it is not confined by isVelocityJumpPhase()
                if (Math.abs(frictDist - yDistance) <= 2.0 * Magic.GRAVITY_MAX
                    && yDistance < lastMove.yDistance - 4.0 * Math.abs(frictDist - yDistance)) {
                    return yDistance;
                }

                // Jumping with velocity into water from below, just slightly more decrease than gravity, twice.
                // (Should be able to do without aw-ww-ww confinement.)
                // (dirty seems to be set/kept reliably)
                // Also needed for waterfalls in 1.13+. Hence why it is not confined by isVelocityJumpPhase()
                if (yDistance > frictDist && yDistance < lastMove.yDistance - Magic.GRAVITY_MAX && data.insideMediumCount <= 1) {
                    return yDistance;
                }
                
                // Cases considering two past moves with moving up.
                if (pastMove1.toIsValid && pastMove1.to.extraPropertiesValid) {

                    // Splash move with space space pressed (this move leaving liquid).
                    if (pastMove1.yDistance > 0.0 && thisMove.yDistance > 0.0
                        && pastMove1.yDistance - Magic.GRAVITY_MAX > lastMove.yDistance 
                        && lastMove.yDistance - Magic.GRAVITY_ODD > thisMove.yDistance
                        && Magic.intoLiquid(lastMove) && Magic.leavingLiquid(thisMove)) {
                        return yDistance;
                    }

                    // Velocity use in lastMove, keep air friction roughly.
                    if (!Magic.resetCond(pastMove1) && lastMove.yDistance - Magic.GRAVITY_MAX > thisMove.yDistance
                        && Magic.intoLiquid(lastMove) && Magic.leavingLiquid(thisMove)) {
                        return yDistance;
                    }
                }
            }
        }
        // Otherwise, only if last move is available.
        else if (lastMove.toIsValid) {
            
            // TODO: Are all these cases really for descending?
            // Falling into water, mid-speed (second move after diving in).
            if (yDistance > -0.9 && yDistance < lastMove.yDistance 
                && Math.abs(yDistance - lastMove.yDistance) <= Magic.GRAVITY_MAX + Magic.GRAVITY_MIN 
                && yDistance - lastMove.yDistance < -Magic.GRAVITY_MIN) {
                //&& !BlockProperties.isLiquid(to.getTypeId(to.getBlockX(), Location.locToBlock(to.getY() + to.getEyeHeight()), to.getBlockZ()))
                return lastMove.yDistance - Magic.GRAVITY_MAX - Magic.GRAVITY_MIN;
            }
            // Increase speed slightly on second in-medium move (dirty flag may have been reset).
            else if (data.insideMediumCount <= 1
                    // (No strong decrease:)
                    && yDistance > lastMove.yDistance - Magic.GRAVITY_MAX
                    && (
                        // Ordinary (some old case).
                        // See: https://github.com/NoCheatPlus/NoCheatPlus/commit/ca7186558967d3370d1c1176929691a44a337a2d
                        lastMove.yDistance < 0.8 && yDistance < lastMove.yDistance - Magic.GRAVITY_SPAN
                        // Check with three moves, rather shortly touching water.
                        || lastMove.yDistance < -0.5 // Arbitrary, actually observed has been < -1.0
                        && pastMove1.toIsValid && pastMove1.to.extraPropertiesValid
                        && Math.abs(pastMove1.yDistance - lastMove.yDistance) < Magic.GRAVITY_MIN
                        && yDistance <= lastMove.yDistance
                        && Magic.inLiquid(lastMove) && Magic.intoLiquid(pastMove1)
                    )) {
                return yDistance;
            }
            // In-water rough near-0-inversion from allowed speed to a negative amount, little more than allowed (magic -0.2 roughly).
            else if (lastMove.yDistance >= Magic.GRAVITY_MAX / 10.0 && lastMove.yDistance <= Magic.GRAVITY_MAX + Magic.GRAVITY_MIN / 2.0
                    && yDistance < 0.0 && yDistance > -2.0 * Magic.GRAVITY_MAX - Magic.GRAVITY_MIN / 2.0
                    && to.isInLiquid() // TODO: Might skip the liquid check, though.
                    && lastMove.from.inLiquid && lastMove.to.extraPropertiesValid && lastMove.to.inLiquid // TODO: in water only?
                    ) {
                return yDistance;
            }
            // Lava rather.
            else if (data.lastNonVanillaFrictionVertical == Magic.FRICTION_MEDIUM_LAVA 
                    && (
                            // Moving downstream.
                            lastMove.yDistance < 0.0 && yDistance > -0.5 && yDistance < lastMove.yDistance 
                            && lastMove.yDistance - yDistance < Magic.GRAVITY_MIN && downStream
                            // Mix of gravity and base speed [careful: relates to water base speed].
                            || lastMove.yDistance < 0.0 && yDistance > -baseSpeed - Magic.GRAVITY_MAX && yDistance < lastMove.yDistance
                            && lastMove.yDistance - yDistance > Magic.GRAVITY_SPAN
                            && Math.abs(lastMove.yDistance + baseSpeed) < 0.25 * baseSpeed
                            // Falling slightly too fast in lava.
                            || data.insideMediumCount == 1 || data.insideMediumCount == 2 
                            && lastMove.yDistance < 0.0 && yDistance < lastMove.yDistance 
                            && yDistance - lastMove.yDistance > -Magic.GRAVITY_MIN && yDistance > -0.65
                            )
                    ) {
                return yDistance;
            }
        }
        // TODO: Also DOWNSTREAM !?
        return null;
    }
}