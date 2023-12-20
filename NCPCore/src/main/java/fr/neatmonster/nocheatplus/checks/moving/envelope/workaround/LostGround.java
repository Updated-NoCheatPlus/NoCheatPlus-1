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

import java.util.Collection;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.combined.Improbable;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.player.Passable;
import fr.neatmonster.nocheatplus.compat.MCAccess;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.players.DataManager;
import fr.neatmonster.nocheatplus.utilities.TickTask;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockCache;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.map.BlockProperties;
import fr.neatmonster.nocheatplus.utilities.map.MaterialUtil;
import fr.neatmonster.nocheatplus.utilities.math.MathUtil;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;


/**
 * Determine if the player should have touched the ground with this movement.
 * @See <a href="https://bugs.mojang.com/browse/MC-90024">Mojang's issue tracker</a> 
 * 
 * @author asofold
 *
 */
public class LostGround {

    // TODO: Cleanup signatures...

    /**
     * Check if touching the ground was lost.
     * (Client did not send / server did not put it through / false negatives on NCP's side / "Blip" glitch).
     * 
     * @param player
     * @param from
     * @param to
     * @param hDistance
     * @param yDistance
     * @param sprinting
     * @param lastMove
     * @param data
     * @param cc
     * @param useBlockChangeTracker 
     * @param tags
     * @return True, if the ground collision was lost.
     */
    public static boolean lostGround(final Player player, final PlayerLocation from, final PlayerLocation to, 
                                     final double hDistance, final double yDistance, final boolean sprinting, 
                                     final PlayerMoveData lastMove, final MovingData data, final MovingConfig cc, 
                                     final BlockChangeTracker blockChangeTracker, final Collection<String> tags) {
        if (yDistance > Math.max(cc.sfStepHeight, data.liftOffEnvelope.getJumpGain(data.jumpAmplifier) + 0.174) || from.isSlidingDown()) {
            return false;
        }

        // TODO: Remove this stupid fix (temporary)
        data.snowFix = (from.getBlockFlags() & BlockFlags.F_HEIGHT_8_INC) != 0;
        // Ascending
        if (yDistance >= 0.0 && lastMove.toIsValid) {
            if (lostGroundAscend(player, from, to, hDistance, yDistance, sprinting, lastMove, data, cc, tags)) {
                return true;
            }
        }
        // Descending.
        if (yDistance <= 0.0) {
            if (lostGroundDescend(data, from, to, player, tags, cc)) {
                return true;
            }
        }
    
        // Block change tracker (kept extra for now).
        if (blockChangeTracker != null && lostGroundPastState(player, from, to, data, cc, blockChangeTracker, tags)) {
            return true;
        }
        // Nothing found.
        return false;
    }


    private static boolean lostGroundPastState(final Player player, final PlayerLocation from, final PlayerLocation to, 
                                               final MovingData data, final MovingConfig cc, final BlockChangeTracker blockChangeTracker, 
                                               final Collection<String> tags) {
        // TODO: Heuristics.
        // TODO: full y-move at from-xz (!).
        final int tick = TickTask.getTick();
        if (from.isOnGroundOpportune(cc.yOnGround, 0L, blockChangeTracker, data.blockChangeRef, tick)) {
            // NOTE: Not sure with setBackSafe here (could set back a hundred blocks on parkour).
            return applyLostGround(player, from, false, data.playerMoves.getCurrentMove(), data, "past", tags);
        }
        return false;
    }

    
    /**
     * This is for descending only (yDistance <= 0.0)
     * 
     * @param data
     * @param from
     * @param to
     * @param player
     * @param tags
     * @param cc
     * @return True, if a case applies
     */
    private static boolean lostGroundDescend(final MovingData data, final PlayerLocation from, final PlayerLocation to, final Player player, final Collection<String> tags, final MovingConfig cc) {
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (to.isOnGround() || thisMove.hDistance <= 0.001 || thisMove.hDistance > 1.5) {
            // Demand to not be toOnGround with thisMove and to be within speed range
            return false;
        }
        // 1: Missed the back-to-ground collision after stepping down a block (to). (--- -> lost onGround)
        // Can be noticed by stepping down slopes/pyramid-like structures.
        if (thisMove.yDistance < 0.0 && from.isOnGround(from.getY() - to.getY() + 0.001) && !lastMove.touchedGroundWorkaround) {
            // Test for passability of the entire box, roughly from feet downwards.
            // TODO: Full bounds check (!).
            // NOTE: checking loc should make sense, rather if loc is higher than from?
            final Location ref = from.getLocation();
            ref.setY(to.getY());
            if (Passable.isPassable(from.getLocation(), ref)) {
                // TODO: Needs new model (store detailed on-ground properties).
                // NOTE: This one always comes before the one below. Could confine by that.
                // ^ But can also happen by itself, so maybe don't :)  
                // (No missedGroundCollision = true here!)
                return applyLostGround(player, from, false, thisMove, data, "stepdown-to", tags);
            }
        }
        // 2: Player landed back on ground and is now preparing to step down again, thus leaving ground yet again. 
        // This collision is lost as well (this would be the "from" onGround collision).
        // (lastMove: --- -> lost onGround(stepdownto)  | thisMove: lost onGround(stepdownfrom) -> --- ).
        if (!thisMove.hasSlowfall && MathUtil.between(1, data.sfJumpPhase, 7) && thisMove.setBackYDistance < 0.0 && lastMove.setBackYDistance < 0.0
            && MathUtil.between(Magic.GRAVITY_MAX, thisMove.yDistance - lastMove.yDistance, cc.sfStepHeight) 
            && thisMove.yDistance > lastMove.yDistance && thisMove.hDistance >= 0.1) {
            if (from.isOnGround(0.6, 0.4, 0.0, 0L)) {
                // This is actually a missed ground collision (corroborated by the fact that fall distance is set back to 0 when this case happens.)
                thisMove.missedGroundCollision = true;
                return applyLostGround(player, from, true, thisMove, data, "stepdown-from", tags);
            }
        }
        // 3: Check for jumping up strange blocks like flower pots on top of other blocks.
        if (thisMove.yDistance == 0.0 && lastMove.yDistance > 0.0 && lastMove.yDistance < 0.25 && data.sfJumpPhase <= data.liftOffEnvelope.getMaxJumpPhase(data.jumpAmplifier) 
            && thisMove.setBackYDistance > 1.0 && thisMove.setBackYDistance < data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier)) {
            if (from.isOnGround(0.25, 0.4, 0, 0L) ) {
                return applyLostGround(player, from, false, thisMove, data, "mini-step", tags);
            }
        }
        // 4: Generic ground collision loss when landing onto/over the edge of blocks.
        // Still needed.
        if (thisMove.yDistance < 0.0 && lastMove.yDistance < 0.0 && thisMove.yDistance > lastMove.yDistance 
            && (
                from.isOnGround(0.5, 0.2, 0) 
                || to.isOnGround(0.5, Math.min(0.2, 0.01 + thisMove.hDistance), Math.min(0.1, 0.01 + -thisMove.yDistance))
            )) {
            thisMove.missedGroundCollision = true;
            return applyLostGround(player, from, true, thisMove, data, "edge-desc", tags);
        }
        // Nothing found.
        return false;
    }


    /**
     * This is for ascending only (yDistance >= 0). Needs last move data.
     * 
     * @param player
     * @param from
     * @param to
     * @param hDistance
     * @param yDistance
     * @param sprinting
     * @param data
     * @param cc
     * @param tags
     * @return
     */
    private static boolean lostGroundAscend(final Player player, final PlayerLocation from, final PlayerLocation to, final double hDistance, final double yDistance, 
                                            final boolean sprinting, final PlayerMoveData lastMove, final MovingData data, final MovingConfig cc, final Collection<String> tags) {
        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        /** Jump height measured as the distance traveled from the onGround Y location to current Y location */
        final double jumpHeight = from.getY() - data.getSetBackY();
        /** Meant to represent the acceleration of the player. Positive= actually accelerating, Negative= decelerating */
        final double yAcceleration = thisMove.yDistance - lastMove.yDistance;
        /** Offset between the allowed jump height and actual height: negative= player jumped higher than possible. Positive= a normal jump/descending */
        final double jumpHeightOffSet = data.liftOffEnvelope.getMaxJumpHeight(data.jumpAmplifier) - jumpHeight;


        // 1: Micro lost ground. Observed with jump obstructed by lanterns and respawn.
        // NOTE: hDistance is to confine, need to test
        // Context: The client has a threshold for sending a position packet/update to the server which is 0.03; the client won't inform the server that it has moved if the movement is smaller than this.
        //          (To be precise, on 1.8 you do get an empty position packet. On 1.9+, you don't even get that. Thanks, Mojang)
        // Now, Can 0.03 cases even happen with Bukkit? Bukkit has a higher threshold for Player Move events (0.0625) after all.
        if (hDistance <= 0.03 && from.isOnGround(0.03) 
            && (thisMove.headObstructed && MaterialUtil.LANTERNS.contains(from.getTypeId(from.getBlockX(), Location.locToBlock(from.getY() + 0.1), from.getBlockZ())) || data.joinOrRespawn)) {
            // This is actually a missed ground collision (corroborated by the fact that fall distance is set back to 0 when this case happens.)
            return applyLostGround(player, from, true, thisMove , data, "0.03", tags);
        }
        if (yDistance > cc.sfStepHeight || hDistance > 1.5 || hDistance <= 0.001) { 
            // Preliminary conditions to better confine all cases that will follow.
            // hDistance is somewhat arbitrary. These cases do not happen with extreme horizontal speeds (on both spectrums).
            return false;
        }

        if (jumpHeightOffSet >= 0.0) {           
            // 1: Check for sprint-jumping on fences with trapdoors above (missing trapdoor's edge touch on server-side, player lands directly onto the fence [For NCP])
            // Strictly speaking, this is not a lost ground case, but a generic collision issue (NCP's VS MC's)
            if (jumpHeight > 1.0 && jumpHeight <= 1.5 && jumpHeightOffSet < 0.6 && data.bunnyhopDelay > 0 
                && yDistance > from.getyOnGround() && lastMove.yDistance <= Magic.GRAVITY_MAX && data.jumpAmplifier <= 0.0
                && (yDistance < Magic.GRAVITY_MIN || yDistance <= data.liftOffEnvelope.getJumpGain(0.0) / 1.52)) {
                to.collectBlockFlags();
                // (Doesn't seem to be a problem with carpets)
                if ((to.getBlockFlags() & BlockFlags.F_ATTACHED_LOW2_SNEW) != 0 && (to.getBlockFlags() & BlockFlags.F_HEIGHT150) != 0) {
                    // Missing the trapdoor by a spitting distance...
                    // No need to add another horizontal margin because the AABB already covers the block, just not vertically.
                    if (to.isOnGround(0.007, 0.0, 0.0)) {
                        // Setbacksafe: matter of taste.
                        // With false, in case of a cheating attempt, the player will be setbacked on the ground instead of the trapdoor.
                        // (No missedGroundCollision = true here!)
                        return applyLostGround(player, from, false, thisMove, data, "trap-fence", tags);
                    }
                }
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////
        // "Could step up"" cases (but might move to another direction, potentially).     //
        ////////////////////////////////////////////////////////////////////////////////////
        if (lastMove.yDistance >= 0.0) { 
            // Safeguard conditions / minimum requirements to prevent too easy abuse.
            return false;
        }

        // 1: Generic could step. 
        // See: https://gyazo.com/779f98b7c2467af57dd8116bf0a193fc
        double horizontalMargin = 0.1 + from.getBoxMarginHorizontal();
        double verticalMargin = cc.sfStepHeight + from.getY();
        // Only apply if not recently used a lostground case and ground is within 1 block distance at maximum.
        if (from.isOnGround(1.0) && !lastMove.touchedGroundWorkaround
            && BlockProperties.isOnGroundShuffled(to.getWorld(), to.getBlockCache(), from.getX(), verticalMargin, from.getZ(), to.getX(), to.getY(), to.getZ(), horizontalMargin, to.getyOnGround(), 0.0)) {
            return applyLostGround(player, from, false, thisMove, data, "couldstep", tags);
        }
        if (to.isOnGround()) {
            // Explicitly demand all below cases to "this move not landing back on ground".
            return false;
        }
        
        // 2: Ground miss with this move (client side blocks y move, but allows h move fully/mostly, missing the edge on server side).
        // See: https://gyazo.com/5613ce5ab7bbb88b760c6b6e67fe35f4
        if (checkEdgeCollision(player, from.getBlockCache(), from.getWorld(), from.getX(), from.getY(), from.getZ(), from.getBoxMarginHorizontal(), from.getyOnGround(), lastMove, data, "-asc1", tags, from.getMCAccess())) {
            thisMove.missedGroundCollision = true;
            // (Use covered area to last from.)
            return true;
        }
        // 3: Mostly similar to the one above
        // xzMargin 0.15: equipped end portal frame (observed and supposedly fixed on MC 1.12.2) - might use an even lower tolerance value here, once there is time to testing this.
        // TODO: Confining in x/z direction in general: should detect if collided in that direction (then skip the x/z dist <= last time).
        double horizontalMargin1 = lastMove.yDistance <= -0.23 ? 0.3 : 0.15;
        if (yDistance == 0.0 && lastMove.yDistance <= -0.1515 && hDistance <= lastMove.hDistance * 1.1 && thisMove.multiMoveCount == 0
            && !lastMove.touchedGroundWorkaround
            && checkEdgeCollision(player, to.getBlockCache(), to.getWorld(), to.getX(), to.getY(), to.getZ(), from.getX(), from.getY(), from.getZ(), hDistance, to.getBoxMarginHorizontal(), horizontalMargin1, data, "-asc2", tags, from.getMCAccess())) {
            return true;
        }
        // 4: (Minimal margin.)
        if (from.isOnGround(from.getyOnGround(), 0.0625, 0.0)) {
            return applyLostGround(player, from, false, thisMove, data, "asc3", tags); 
        }
        // Nothing found.
        return false;
    }

    /**
     * Vertical collision with ground on client side, shifting over an edge with
     * the horizontal move. Needs last move data.
     * 
     * @param player
     * @param blockCache
     * @param world
     * @param x1
     *            Target position.
     * @param y1
     * @param z1
     * @param boxMarginHorizontal
     *            Center to edge, at some resolution.
     * @param yOnGround
     * @param data
     * @param tag
     * @return
     */
    private static boolean checkEdgeCollision(final Player player, final BlockCache blockCache, final World world, final double x1, final double y1, 
                                             final double z1, final double boxMarginHorizontal, final double yOnGround, 
                                             final PlayerMoveData lastMove, final MovingData data, final String tag, final Collection<String> tags, 
                                             final MCAccess mcAccess) {
        return checkEdgeCollision(player, blockCache, world, x1, y1, z1, lastMove.from.getX(), lastMove.from.getY(), lastMove.from.getZ(), lastMove.hDistance, boxMarginHorizontal, yOnGround, data, tag, tags, mcAccess);
    }


    /**
     * Vertical collision with ground on client side, shifting over an edge with
     * the horizontal move. Needs last move data.
     * @See <a href="https://gyazo.com/5613ce5ab7bbb88b760c6b6e67fe35f4">
     * This screenshot for a visual representation of what's happening. </a> 
     * 
     * @param player
     * @param blockCache
     * @param world
     * @param x1 
     *           Player's coordinates...
     * @param y1
     * @param z1
     * @param x2 
     *           Player's coordinates...
     * @param y2
     * @param z2
     * @param hDistance2
     * @param boxMarginHorizontal
     *           AABB horizontal width (fullWidth / 2f)
     * @param yOnGround
     * @param data
     * @param tag
     * @param tags
     * @param mcAccess
     * @return
     */
    private static boolean checkEdgeCollision(final Player player, final BlockCache blockCache, final World world, 
                                             final double x1, final double y1, final double z1, double x2, final double y2, double z2, 
                                             final double hDistance2, final double boxMarginHorizontal, final double yOnGround, 
                                             final MovingData data, final String tag, final Collection<String> tags, final MCAccess mcAccess) {

        // First: calculate vector towards last from.
        x2 -= x1;
        z2 -= z1;

        // Second: cap the size of the extra box (at least horizontal).
        double cappingFactor = 1.0;
        if (Math.abs(x2) > hDistance2) {
            cappingFactor = Math.min(cappingFactor, hDistance2 / Math.abs(x2));
        }
        if (Math.abs(z2) > hDistance2) {
            cappingFactor = Math.min(cappingFactor, hDistance2 / Math.abs(z2));
        }

        // TODO: Further / more precise ?
        // Third: calculate end points.
        x2 = cappingFactor * x2 + x1;
        z2 = cappingFactor * z2 + z1;

        // Finally, test for ground.
        // (We don't add another xz-margin here, as the move should cover ground.)
        if (BlockProperties.isOnGroundShuffled(world, blockCache, x1, y1, z1, x2, y1 + (data.snowFix ? 0.125 : 0.0), z2, boxMarginHorizontal + (data.snowFix ? 0.1 : 0.0), yOnGround, 0.0)) {
            // NOTE: data.fromY for set back is not correct, but currently it is more safe (needs instead: maintain a "distance to ground").
            return applyLostGround(player, new Location(world, x2, y2, z2), true, data.playerMoves.getCurrentMove(), data, "edge" + tag, tags, mcAccess);
        } 
        return false;
    }


    /**
     * Apply lost-ground workaround.
     * 
     * @param player
     * @param refLoc (Bukkit Location)
     * @param setBackSafe If to use the given location as set back location.
     * @param thisMove
     * @param data
     * @param tag Added to "lostground_" as tag.
     * @param tags
     * @param mcAccess
     * @return Always true.
     */
    private static boolean applyLostGround(final Player player, final Location refLoc, final boolean setBackSafe, 
                                           final PlayerMoveData thisMove, final MovingData data, final String tag, 
                                           final Collection<String> tags, final MCAccess mcAccess) {
        if (setBackSafe) {
            data.setSetBack(refLoc);
        }
        else {
            // Keep Set back.
        }
        return applyLostGround(player, thisMove, data, tag, tags, mcAccess);
    }


    /**
     * Apply lost-ground workaround.
     * 
     * @param player
     * @param refLoc (PlayerLocation)
     * @param setBackSafe If to use the given location as set back.
     * @param thisMove
     * @param data
     * @param tag Added to "lostground_" as tag.
     * @param tags
     * @return Always true.
     */
    private static boolean applyLostGround(final Player player, final PlayerLocation refLoc, final boolean setBackSafe, 
                                           final PlayerMoveData thisMove, final MovingData data, final String tag, 
                                           final Collection<String> tags) {
        // Set the new setBack and reset the jumpPhase.
        if (setBackSafe) {
            data.setSetBack(refLoc);
        }
        else {
            // Keep Set back.
        }
        return applyLostGround(player, thisMove, data, tag, tags, refLoc.getMCAccess());
    }


    /**
     * Apply lost-ground workaround (data adjustments and tag).
     * 
     * @param player
     * @param thisMove
     * @param data
     * @param tag Added to "lostground_" as tag.
     * @param tags
     * @param mcAccess
     * @return Always true.
     */
    private static boolean applyLostGround(final Player player, final PlayerMoveData thisMove, final MovingData data, final String tag, 
                                           final Collection<String> tags, final MCAccess mcAccess) {
        // Reset the jumpPhase.
        data.sfJumpPhase = 0;
        // Update the jump amplifier because we assume the player to be able to jump here.
        data.jumpAmplifier = MovingUtil.getJumpAmplifier(player, mcAccess);
        // Update speed factors the the speed estimation.
        data.adjustMediumProperties(player.getLocation(), DataManager.getPlayerData(player).getGenericInstance(MovingConfig.class), player, data.playerMoves.getCurrentMove());
        // Tell NoFall that we assume the player to have been on ground somehow.
        thisMove.touchedGround = true;
        thisMove.touchedGroundWorkaround = true;
        tags.add("lostground_" + tag);
        Improbable.update(player, System.currentTimeMillis());
        return true;
    }
}
