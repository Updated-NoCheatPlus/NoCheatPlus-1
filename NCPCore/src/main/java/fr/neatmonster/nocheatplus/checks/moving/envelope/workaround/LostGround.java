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
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.Bridge1_9;
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
        if (Bridge1_9.isGliding(player) || Bridge1_13.isRiptiding(player) || from.isResetCond() || from.isSlidingDown() || from.isOnGround() || player.isFlying()) {
            // Cannot happen under these conditions.
            return false;
        }
        if (!Double.isInfinite(Bridge1_9.getLevitationAmplifier(player))) {
            // Ignore levitation
            return false;
        }

        final PlayerMoveData thisMove = data.playerMoves.getCurrentMove();
        // Very specific case with players jumping with head obstructed by lanterns or after respawning
        if (hDistance <= 0.03 && from.isOnGround(0.03) 
            && (thisMove.headObstructed && MaterialUtil.LANTERNS.contains(from.getTypeId(from.getBlockX(), Location.locToBlock(from.getY() + 0.1), from.getBlockZ())) || data.joinOrRespawn)) {
            return applyLostGround(player, from, true, thisMove, data, "0.03", tags);
        }

        if (!MathUtil.inRange(0.1, hDistance, 1.5)) { 
            // Lost ground only happens with enough motion.
            return false;
        }

        // TODO: Remove this stupid fix (temporary)
        data.snowFix = (from.getBlockFlags() & BlockFlags.F_HEIGHT_8_INC) != 0;
        // Test for "couldstep" early: player might step up, but could also move to another direction. Unpredictable outcome.
        // See this screenshot: https://gyazo.com/779f98b7c2467af57dd8116bf0a193fc
        double horizontalMargin = to.getBoxMarginHorizontal() + 0.1;
        double from_Y = from.getY() + cc.sfStepHeight;
        if (MathUtil.inRange(0.0, yDistance, cc.sfStepHeight) && lastMove.yDistance < 0.0) {
            if (from.isOnGround(1.0) && BlockProperties.isOnGroundShuffled(to.getWorld(), to.getBlockCache(), from.getX(), from_Y, from.getZ(), to.getX(), to.getY(), to.getZ(), horizontalMargin, to.getyOnGround(), 0.0)) {
                thisMove.couldStepUp = true;
                return applyLostGround(player, from, false, thisMove, data, "couldstep", tags);
            }
        }

        if (!lastMove.toIsValid) {
            // Prevent too easy abuse.
            return false;
        }

        // Acceleration changed: player might have collided with something.
        if (thisMove.yDistance > lastMove.yDistance && lastMove.yDistance < 0.0) {
            if (to.isOnGround()) {
                // No need for interpolation in this case: the player likely has just landed on the ground, though there may be cases with missed from, but detected to.
                return false;
            }
            // Try interpolating the ground collision from this-from to last-from.
            if (interpolateGround(player, from.getBlockCache(), from.getWorld(), from.getMCAccess(), "_lastMove", tags, data, 
                                 from.getX(), from.getY(), from.getZ(), lastMove, from.getBoxMarginHorizontal(), from.getyOnGround())) {
                return true;
            }
            // Try interpolating the ground collision from this-to to this-from.
            if (interpolateGround(player, to.getBlockCache(), to.getWorld(), to.getMCAccess(), tags, "_thisMove", data, 
                                  to.getX(), to.getY(), to.getZ(), from.getX(), from.getY(), from.getZ(), thisMove.hDistance, to.getBoxMarginHorizontal(), to.getyOnGround())) {
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
     * Vertical collision with ground on client side, shifting over an edge with
     * the horizontal move. Needs last move data.
     * @See <a href="https://gyazo.com/5613ce5ab7bbb88b760c6b6e67fe35f4">
     * This screenshot for a visual representation of what's happening. </a> 
     * 
     * @param player
     * @param blockCache
     * @param world
     * @param mcAccess
     * @param tags
     * @param tag
     * @param data
     * @param thisX 
     *           Player's current coordinates...
     * @param thisY
     * @param thisZ
     * @param lastX 
     *           Player's last coordinates...
     * @param lastY
     * @param lastZ
     * @param lastHDistance
     * @param boxMarginHorizontal
     *           AABB horizontal width at some resolution
     * @param yOnGround
     * @return
     */
    private static boolean interpolateGround(final Player player, final BlockCache blockCache, final World world, 
                                             final MCAccess mcAccess, final Collection<String> tags, final String tag, final MovingData data, final double thisX, final double thisY, 
                                             final double thisZ, double lastX, final double lastY, 
                                             double lastZ, final double lastHDistance, final double boxMarginHorizontal, final double yOnGround) {
        // First: calculate vector towards last from.
        lastX -= thisX;
        lastZ -= thisZ;

        // Second: cap the size of the extra box (at least horizontal).
        double minFactor = 1.0;
        if (Math.abs(lastX) > lastHDistance) {
            minFactor = Math.min(minFactor, lastHDistance / Math.abs(lastX));
        }
        if (Math.abs(lastZ) > lastHDistance) {
            minFactor = Math.min(minFactor, lastHDistance / Math.abs(lastZ));
        }

        // TODO: Further / more precise ?
        // Third: calculate end points.
        lastX = minFactor * lastX + thisX;
        lastZ = minFactor * lastZ + thisZ;

        // Finally, test for ground.
        // (We don't add another xz-margin here, as the move should cover ground.)
        if (BlockProperties.isOnGroundShuffled(world, blockCache, thisX, thisY, thisZ, lastX, thisY + (data.snowFix ? 0.125 : 0.0), lastZ, boxMarginHorizontal + (data.snowFix ? 0.1 : 0.0), yOnGround, 0.0)) {
            // NOTE: data.fromY for set back is not correct, but currently it is more safe (needs instead: maintain a "distance to ground").
            return applyLostGround(player, new Location(world, lastX, lastY, lastZ), true, data.playerMoves.getCurrentMove(), data, "interpolate" + tag, tags, mcAccess);
        } 
        return false;
    }


    /**
     * Vertical collision with ground on client side, shifting over an edge with
     * the horizontal move. Needs last move data.
     * 
     * @param player
     * @param blockCache
     * @param world
     * @param tag
     * @param data
     * @param thisX
     *            Target's current coordinates...
     * @param thisY
     * @param thisZ
     * @param lastMove 
     *            Last move's coordinates...
     * @param boxMarginHorizontal
     *            Center to edge, at some resolution.
     * @param yOnGround
     * @return
     */
    private static boolean interpolateGround(final Player player, final BlockCache blockCache, final World world, final MCAccess mcAccess, final String tag, 
                                             final Collection<String> tags, final MovingData data, final double thisX, 
                                             final double thisY, final double thisZ, final PlayerMoveData lastMove, final double boxMarginHorizontal, 
                                             final double yOnGround) {
        return interpolateGround(player, blockCache, world, mcAccess, tags, tag, data, thisX, thisY, thisZ, lastMove.from.getX(), lastMove.from.getY(), lastMove.from.getZ(), lastMove.hDistance, boxMarginHorizontal, yOnGround);
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
        player.sendMessage("lostground_" + tag);
        if (thisMove.couldStepUp) {
            // Couldstep is a risky workaround, but also very difficult to reproduce at will.
            // Trying to exploit it "too much" will eventually set off Improbable.
            Improbable.check(player, (float)Math.abs(thisMove.yDistance), System.currentTimeMillis(), "couldstep", DataManager.getPlayerData(player));
        }
        return true;
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
}
