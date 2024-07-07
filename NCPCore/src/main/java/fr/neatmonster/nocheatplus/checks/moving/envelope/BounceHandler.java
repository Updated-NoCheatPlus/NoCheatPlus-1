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
package fr.neatmonster.nocheatplus.checks.moving.envelope;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.moving.MovingConfig;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.model.BounceType;
import fr.neatmonster.nocheatplus.checks.moving.model.PlayerMoveData;
import fr.neatmonster.nocheatplus.checks.moving.velocity.SimpleEntry;
import fr.neatmonster.nocheatplus.checks.moving.velocity.VelocityFlags;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.BlockChangeEntry;
import fr.neatmonster.nocheatplus.compat.blocks.changetracker.BlockChangeTracker.Direction;
import fr.neatmonster.nocheatplus.components.debug.IDebugPlayer;
import fr.neatmonster.nocheatplus.players.IPlayerData;
import fr.neatmonster.nocheatplus.utilities.location.PlayerLocation;
import fr.neatmonster.nocheatplus.utilities.map.BlockFlags;
import fr.neatmonster.nocheatplus.utilities.moving.Magic;
import fr.neatmonster.nocheatplus.utilities.moving.MovingUtil;
import fr.neatmonster.nocheatplus.compat.Bridge1_13;
import fr.neatmonster.nocheatplus.compat.BridgeMisc;

/**
 * Auxiliary methods for bounce effect
 * @author asofold 
 * 
 */
public class BounceHandler {

    private static final long FLAGS_VELOCITY_BOUNCE_BLOCK = VelocityFlags.ORIGIN_BLOCK_BOUNCE;

    private static final long FLAGS_VELOCITY_BOUNCE_BLOCK_MOVE_ASCEND = FLAGS_VELOCITY_BOUNCE_BLOCK | VelocityFlags.SPLIT_ABOVE_0_42 | VelocityFlags.SPLIT_RETAIN_ACTCOUNT | VelocityFlags.ORIGIN_BLOCK_MOVE;

    /**
     * Prepare and adjust data to the future bounce effect: precondition is PlayerEnvelopes#canBounce having returned true. <br>
     * (Player has landed on ground with negative motion and will bounce up with the next move)
     * This might be a micro-move onto ground.
     * 
     * @param player
     * @param fromY 
     * @param toY
     * @param bonceType
     * @param tick
     * @param idp
     * @param data
     * @param cc
     * @param pData
     */
    public static void processBounce(final Player player,final double fromY, final double toY, final BounceType bounceType, final int tick, final IDebugPlayer idp,
                                     final MovingData data, final MovingConfig cc, final IPlayerData pData) {
        /** Takes into account micro moves */
    	double fallDistance = MovingUtil.getRealisticFallDistance(player, fromY, toY, data, pData);
        /** The base force of the bounce */
        double baseEffect = getBaseBounceSpeed(player, fallDistance);
        /** The final force of the bounce, capping at a maximum estimate + some gravity effects */
        double finalEffect = Math.min(getMaximumBounceGain(player), baseEffect+Math.min(baseEffect / 10.0, Magic.GRAVITY_MAX)); // Ancient Greek technology with gravity added.
        final PlayerMoveData lastMove = data.playerMoves.getFirstPastMove();
        if (finalEffect > 0.415 && lastMove.toIsValid && !Bridge1_13.isRiptiding(player)) {
            // Riptiding is exempted, because players can strategically time the release of the trident to bounce up even higher than before (since new speed gets added to the current motion).
            // Let vDistRel enforce the correct motion here, so that player don't exploit this to get higher bounces than normally possible (i.e.: bouncing at the maximum bounce gain immediately with a single try)
            /** Extra capping by last y distance(s). Prevents bouncing higher and higher on normal conditions. */
            double lastMinGain = Math.abs(lastMove.yDistance < 0.0 ? Math.min(lastMove.yDistance, toY-fromY) : (toY-fromY)) - Magic.GRAVITY_SPAN;
            if (lastMinGain < finalEffect) {
                // Likely a cheat attempt...
                finalEffect = lastMinGain;
                if (pData.isDebugActive(CheckType.MOVING)) {
                	idp.debug(player, "Cap this bounce effect by recent y-distances.");
                }
            }
        }
        if (bounceType == BounceType.STATIC_PAST_AND_PUSH) {
            /*
             * TODO: Find out if relevant and handle here (still use maximum
             * cap, but not by y-distance.). Could be the push part is only
             * necessary if the player is pushed upwards without prepared
             * bounce.
             */
        }
        if (pData.isDebugActive(CheckType.MOVING)) {
            idp.debug(player, "Set bounce effect (dY=" + fallDistance + " / " + bounceType + "): " + finalEffect); 
        }
        data.noFallSkipAirCheck = true;
        data.verticalBounce = new SimpleEntry(tick, finalEffect, FLAGS_VELOCITY_BOUNCE_BLOCK, 1); // Just bounce for now.
    }
    
    /**
     * Get an estimation of the bounce's speed.
     * This uses the fall-distance, because the actual y-distance of a move can vary wildly if you collide with anything, 
     * including the block itself (In fact, Minecraft hides speed on the server-side when landing on the ground [see desc. of touchdown workaround in AirWorkarounds]).
     * 
     * @param player
     * @param fallDistance Not Y distance.
     * @return The squared root of the given fall distance, divided by a magic number
     */
    public static double getBaseBounceSpeed(final Player player, double fallDistance) {
        if (BridgeMisc.isRipgliding(player)) {
            // Thank you Mojang for letting players perform such ridiculous moves that not even the server can handle!
            return Math.sqrt(fallDistance);
        }
        if (Bridge1_13.isRiptiding(player)) {
            return Math.sqrt(fallDistance) / 1.5;
        }
        return Math.sqrt(fallDistance) / 3.3;
    }
    
    /**
     * Maximum achievable speed for a single bounce (estimated).
     * 
     * @param player
     * @return The maximum speed observed.
     */
    public static double getMaximumBounceGain(final Player player) {
        if (BridgeMisc.isRipgliding(player)) {
            // Completely made up. Assume that players can roughly reach twice the maximum speed they would be able to achieve when just riptiding.
            return Magic.BOUNCE_VERTICAL_MAX_DIST * 3.5;
        }
        if (Bridge1_13.isRiptiding(player)) {
            // Managed to reach speed up to 5.9, roughly, but skilled players can probably time the release of the trident much better to get even higher speeds.
            // (Hence x2.0)
            return Magic.BOUNCE_VERTICAL_MAX_DIST * 2.0;
        }
        return Magic.BOUNCE_VERTICAL_MAX_DIST;
    }


    /**
     * Handle a prepare bounce.
     * 
     * @param player
     * @param from
     * @param to
     * @param lastMove
     * @param tick
     * @param data
     * @return True, if bounce has been used, i.e. to do without fall damage.
     */
    public static boolean onPreparedBounceSupport(final Player player, final Location from, final Location to, 
                                                  final PlayerMoveData thisMove, final PlayerMoveData lastMove, 
                                                  final int tick, final MovingData data) {
        if (to.getY() > from.getY() || to.getY() == from.getY() && data.verticalBounce.value < 0.13) {
            // Apply bounce.
            if (to.getY() == from.getY()) {
                // TODO: Why is this needed?
                // Fake use velocity here.
                // CHEATING: Commented out because it allows walkspeed-horizontal-fly
                // Now, the real question is, why is this method running even when the player is far away from the slime block?
                // Apparently, data.verticalBounce never gets reset when checking (data.verticalBounce != null && BounceUtil.onPreparedBounceSupport(player, from, to, thisMove, lastMove, tick, data))
                // data.prependVerticalVelocity(new SimpleEntry(tick, 0.0, 1));
                // data.getOrUseVerticalVelocity(0.0);
                if (lastMove.toIsValid && lastMove.yDistance < 0.0) {
                    // Renew the bounce effect.
                    data.verticalBounce = new SimpleEntry(tick, data.verticalBounce.value, 1);
                }
            }
            else data.useVerticalBounce(player);
            return true;
        }
        // Nothing to do.
        data.verticalBounce = null;
        return false;
    }


    /**
     * Only for yDistance < 0 + some bounce envelope checked.
     * 
     * @param player
     * @param from
     * @param to
     * @param lastMove
     * @param lastMove
     * @param tick
     * @param data
     * @param cc
     * @return
     */
    public static BounceType checkPastStateBounceDescend(final Player player, final PlayerLocation from, final PlayerLocation to,
                                                         final PlayerMoveData thisMove, final PlayerMoveData lastMove, final int tick, 
                                                         final MovingData data, final MovingConfig cc, BlockChangeTracker blockChangeTracker) {
        // TODO: Might later need to override/adapt just the bounce effect set by the ordinary method.
        final UUID worldId = from.getWorld().getUID();
        // Prepare (normal/extra) bounce.
        // Typical: a slime block has been there.
        final BlockChangeEntry entryBelowAny = blockChangeTracker.getBlockChangeEntryMatchFlags(data.blockChangeRef, tick, worldId, to.getBlockX(), to.getBlockY() - 1, to.getBlockZ(), null, BlockFlags.F_BOUNCE25);
        if (entryBelowAny != null) {
            // TODO: Check preconditions for bouncing here at all (!).
            // Check if the/a block below the feet of the player got pushed into the feet of the player.
            final BlockChangeEntry entryBelowY_POS = entryBelowAny.direction == Direction.Y_POS ? entryBelowAny 
                                                   : blockChangeTracker.getBlockChangeEntryMatchFlags(data.blockChangeRef, tick, worldId, to.getBlockX(), to.getBlockY() - 1, to.getBlockZ(), Direction.Y_POS, BlockFlags.F_BOUNCE25);
            if (entryBelowY_POS != null) {
                // TODO: Can't know if used... data.blockChangeRef.updateSpan(entryBelowY_POS);
                // TODO: So far, doesn't seem to be followed by violations.
                return BounceType.STATIC_PAST_AND_PUSH;
            }
            // A bouncy block had been there in the past (but without push)
            // TODO: Can't know if used... data.blockChangeRef.updateSpan(entryBelowAny);
            return BounceType.STATIC_PAST;
        }
        // No past activity found, return no bounce.
        /*
         * TODO: Can't update span here. If at all, it can be added as side
         * condition for using the bounce effect. Probably not worth it.
         */
        return BounceType.NO_BOUNCE; 
    }
    

    /**
     * Only for yDistance > 0 + some bounce envelope checked.
     * 
     * @param player
     * @param from
     * @param to
     * @param lastMove
     * @param lastMove
     * @param tick
     * @param data
     * @param cc
     * @return
     */
    public static BounceType checkPastStateBounceAscend(final Player player, final PlayerLocation from, final PlayerLocation to,
                                                        final PlayerMoveData thisMove, final PlayerMoveData lastMove, final int tick, final IPlayerData pData, 
                                                        final IDebugPlayer idp, final MovingData data, final MovingConfig cc, BlockChangeTracker blockChangeTracker) {
        // TODO: More preconditions.
        // TODO: Nail down to more precise side conditions for larger jumps, if possible.
        final UUID worldId = from.getWorld().getUID();
        final boolean debug = pData.isDebugActive(CheckType.MOVING);
        // Possibly a "lost use of slime".
        // TODO: Might need to cover push up, after ordinary slime bounce.
        // TODO: Work around 0-dist?
        // TODO: Adjust amount based on side conditions (center push or off center, distance to block top).
        double amount = -1.0;
        final BlockChangeEntry entryBelowY_POS = blockChangeTracker.getBlockChangeEntryMatchFlags(data.blockChangeRef, tick, worldId, from.getBlockX(), from.getBlockY() - 1, from.getBlockZ(), Direction.Y_POS, BlockFlags.F_BOUNCE25);

        if (
                // Center push.
                entryBelowY_POS != null
                // Off center push.
                || thisMove.yDistance < 1.515 && from.matchBlockChangeMatchResultingFlags(blockChangeTracker, data.blockChangeRef, Direction.Y_POS, Math.min(.415, thisMove.yDistance), BlockFlags.F_BOUNCE25)
            ) {

            amount = Math.min(Math.max(0.505, 1.0 + (double) from.getBlockY() - from.getY() + 1.515), 1.915); // Old: 2.525
            if (debug) {
                idp.debug(player, "Direct block push with bounce (" + (entryBelowY_POS == null ? "off_center)." : "center)."));
            }
            if (entryBelowY_POS != null) {
                data.blockChangeRef.updateSpan(entryBelowY_POS);
            }
        }

        // Center push while being on the top height of the pushed block already (or 0.5 above (!)).
        if (
                amount < 0.0
                // TODO: Not sure about y-Distance.
                && lastMove.toIsValid && lastMove.yDistance >= 0.0 && lastMove.yDistance <= 0.505
                && Math.abs(from.getY() - (double) from.getBlockY() - lastMove.yDistance) < 0.205 // from.getY() - (double) from.getBlockY() == lastMove.yDistance
            ) {

            final BlockChangeEntry entry2BelowY_POS = blockChangeTracker.getBlockChangeEntryMatchFlags(data.blockChangeRef, tick, worldId, from.getBlockX(), from.getBlockY() - 2, from.getBlockZ(), Direction.Y_POS, BlockFlags.F_BOUNCE25);
            if (entry2BelowY_POS != null) {
                // TODO: Does off center push exist with this very case?
                amount = Math.min(Math.max(0.505, 1.0 + (double) from.getBlockY() - from.getY() + 1.515),  1.915 - lastMove.yDistance); // TODO: EXACT MAGIC.
                if (debug) {
                    idp.debug(player, "Foot position block push with bounce (" + (entry2BelowY_POS == null ? "off_center)." : "center)."));
                }
                if (entryBelowY_POS != null) {
                    data.blockChangeRef.updateSpan(entry2BelowY_POS);
                }
            }
        }

        // Finally add velocity if set.
        if (amount >= 0.0) {
            /*
             * TODO: USE EXISTING velocity with bounce flag set first, then peek
             * / add. (might while peek -> has bounce flag: remove velocity)
             */
            data.removeLeadingQueuedVerticalVelocityByFlag(VelocityFlags.ORIGIN_BLOCK_BOUNCE);
            /*
             * TODO: Concepts for limiting... max amount based on side
             * conditions such as block height+1.5, max coordinate, max
             * amount per use, ALLOW_ZERO flag/boolean and set in
             * constructor, demand max. 1 zero dist during validity. Bind
             * use to initial xz coordinates... Too precise = better with
             * past move tracking, or a sub-class of SimpleEntry with better
             * access signatures including thisMove.
             */
            /*
             * TODO: Also account for current yDistance here? E.g. Add two
             * entries, split based on current yDistance?
             */
            final SimpleEntry vel = new SimpleEntry(tick, amount, FLAGS_VELOCITY_BOUNCE_BLOCK_MOVE_ASCEND, 4);
            data.verticalBounce = vel;
            data.useVerticalBounce(player);
            data.useVerticalVelocity(thisMove.yDistance);
            //if (thisMove.yDistance > 0.42) {
            //    data.setFrictionJumpPhase();
            //}
            if (debug) {
                idp.debug(player, "checkPastStateBounceAscend: set velocity: " + vel);
            }
            // TODO: Exact type to return.
            return BounceType.STATIC_PAST_AND_PUSH;
        }
        // TODO: There is a special case with 1.0 up on pistons pushing horizontal only (!).
        return BounceType.NO_BOUNCE;
    }
}